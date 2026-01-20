package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Measures cache-hit relayout cost (computeLayout on an already-laid-out, non-dirty tree).
 *
 * Motivation: many real workloads call layout repeatedly without changing styles.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class RelayoutBenchmark {

    private static final long SEED = 12345L;

    @State(Scope.Thread)
    public static class RelayoutState {
        TaffyTree tree;
        NodeId root;

        // keep default run lightweight
        @Param({"flex_wide_1000", "grid_wide_31"})
        String caseName;

        @Setup(Level.Trial)
        public void setupTrial() {
            if ("flex_wide_1000".equals(caseName)) {
                Object[] built = buildFlexWideTree(1000);
                tree = (TaffyTree) built[0];
                root = (NodeId) built[1];
                tree.computeLayout(root, TaffySize.maxContent());
            } else if ("grid_wide_31".equals(caseName)) {
                Object[] built = buildGridFlatHierarchy(31, 31);
                tree = (TaffyTree) built[0];
                root = (NodeId) built[1];
                tree.computeLayout(root, TaffySize.of(AvailableSpace.definite(12000f), AvailableSpace.definite(12000f)));
            } else {
                throw new IllegalArgumentException("Unknown caseName: " + caseName);
            }
        }
    }

    @Benchmark
    public void relayout(RelayoutState state, Blackhole bh) {
        if ("flex_wide_1000".equals(state.caseName)) {
            state.tree.computeLayout(state.root, TaffySize.maxContent());
        } else {
            state.tree.computeLayout(state.root, TaffySize.of(AvailableSpace.definite(12000f), AvailableSpace.definite(12000f)));
        }
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== Builders ====================

    private static Object[] buildFlexWideTree(int targetNodeCount) {
        TaffyTree tree = new TaffyTree();
        Random rng = new Random(SEED);

        int created = 0;
        List<NodeId> children = new ArrayList<>();

        while (created < targetNodeCount) {
            int count = 1 + rng.nextInt(4); // 1-4 children per container
            NodeId[] subChildren = new NodeId[count];
            for (int i = 0; i < count; i++) {
                subChildren[i] = tree.newLeaf(randomFlexItemStyle(rng));
            }
            children.add(tree.newWithChildren(randomFlexContainerStyle(rng), subChildren));
            created += count + 1;
        }

        TaffyStyle rootStyle = new TaffyStyle();
        NodeId root = tree.newWithChildren(rootStyle, children.toArray(new NodeId[0]));
        return new Object[]{tree, root};
    }

    private static TaffyStyle randomFlexItemStyle(Random rng) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(randomDimension(rng), randomDimension(rng));
        return style;
    }

    private static TaffyStyle randomFlexContainerStyle(Random rng) {
        // Keep it simple (still randomized) so it exercises flex layout.
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = rng.nextBoolean() ? FlexDirection.ROW : FlexDirection.COLUMN;
        style.flexWrap = rng.nextBoolean() ? FlexWrap.WRAP : FlexWrap.NO_WRAP;
        return style;
    }

    private static TaffyDimension randomDimension(Random rng) {
        float rand = rng.nextFloat();
        if (rand < 0.2f) {
            return TaffyDimension.AUTO;
        } else if (rand < 0.8f) {
            return TaffyDimension.length(rng.nextFloat() * 500f);
        } else {
            return TaffyDimension.percent(rng.nextFloat());
        }
    }

    private static NodeId buildRandomLeaf(TaffyTree taffy) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(20.0f), TaffyDimension.length(20.0f));
        return taffy.newLeaf(style);
    }

    private static TrackSizingFunction randomGridTrack(Random rng) {
        float r = rng.nextFloat();
        if (r < 0.1f) {
            return TrackSizingFunction.auto();
        } else if (r < 0.2f) {
            return TrackSizingFunction.minContent();
        } else if (r < 0.3f) {
            return TrackSizingFunction.maxContent();
        } else if (r < 0.5f) {
            return TrackSizingFunction.fr(1.0f);
        } else if (r < 0.6f) {
            return TrackSizingFunction.minmax(TrackSizingFunction.fixed(0.0f), TrackSizingFunction.fr(1.0f));
        } else if (r < 0.8f) {
            return TrackSizingFunction.fixed(40.0f);
        } else {
            return TrackSizingFunction.percent(0.3f);
        }
    }

    private static Object[] buildGridFlatHierarchy(int colCount, int rowCount) {
        TaffyTree taffy = new TaffyTree();
        Random rng = new Random(SEED);

        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;

        List<TrackSizingFunction> cols = new ArrayList<>(colCount);
        List<TrackSizingFunction> rows = new ArrayList<>(rowCount);
        for (int i = 0; i < colCount; i++) {
            cols.add(randomGridTrack(rng));
        }
        for (int i = 0; i < rowCount; i++) {
            rows.add(randomGridTrack(rng));
        }
        style.gridTemplateColumns = cols;
        style.gridTemplateRows = rows;

        int childCount = colCount * rowCount;
        NodeId[] children = new NodeId[childCount];
        for (int i = 0; i < childCount; i++) {
            children[i] = buildRandomLeaf(taffy);
        }

        NodeId root = taffy.newWithChildren(style, children);
        return new Object[]{taffy, root};
    }
}
