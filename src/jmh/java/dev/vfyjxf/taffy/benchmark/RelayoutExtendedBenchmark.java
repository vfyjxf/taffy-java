package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.Dimension;
import dev.vfyjxf.taffy.style.Display;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.Style;
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
 * Extended (slower) relayout benchmarks.
 * Excluded from default `jmh` task; run via Gradle task `jmhExtended`.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class RelayoutExtendedBenchmark {

    private static final long SEED = 12345L;

    @State(Scope.Thread)
    public static class RelayoutState {
        TaffyTree tree;
        NodeId root;

        @Param({"flex_wide_10000", "grid_wide_100", "grid_wide_316"})
        String caseName;

        @Setup(Level.Trial)
        public void setupTrial() {
            if ("flex_wide_10000".equals(caseName)) {
                Object[] built = buildFlexWideTree(10000);
                tree = (TaffyTree) built[0];
                root = (NodeId) built[1];
                tree.computeLayout(root, Size.maxContent());
            } else if ("grid_wide_100".equals(caseName)) {
                Object[] built = buildGridFlatHierarchy(100, 100);
                tree = (TaffyTree) built[0];
                root = (NodeId) built[1];
                tree.computeLayout(root, Size.of(AvailableSpace.definite(12000f), AvailableSpace.definite(12000f)));
            } else if ("grid_wide_316".equals(caseName)) {
                Object[] built = buildGridFlatHierarchy(316, 316);
                tree = (TaffyTree) built[0];
                root = (NodeId) built[1];
                tree.computeLayout(root, Size.of(AvailableSpace.definite(12000f), AvailableSpace.definite(12000f)));
            } else {
                throw new IllegalArgumentException("Unknown caseName: " + caseName);
            }
        }
    }

    @Benchmark
    public void relayout(RelayoutState state, Blackhole bh) {
        if ("flex_wide_10000".equals(state.caseName)) {
            state.tree.computeLayout(state.root, Size.maxContent());
        } else {
            state.tree.computeLayout(state.root, Size.of(AvailableSpace.definite(12000f), AvailableSpace.definite(12000f)));
        }
        bh.consume(state.tree.getLayout(state.root));
    }

    // Same minimal builders as RelayoutBenchmark (kept self-contained)

    private static Object[] buildFlexWideTree(int targetNodeCount) {
        TaffyTree tree = new TaffyTree();
        Random rng = new Random(SEED);

        int created = 0;
        List<NodeId> children = new ArrayList<>();

        while (created < targetNodeCount) {
            int count = 1 + rng.nextInt(4);
            NodeId[] subChildren = new NodeId[count];
            for (int i = 0; i < count; i++) {
                subChildren[i] = tree.newLeaf(randomFlexItemStyle(rng));
            }
            children.add(tree.newWithChildren(randomFlexContainerStyle(rng), subChildren));
            created += count + 1;
        }

        Style rootStyle = new Style();
        NodeId root = tree.newWithChildren(rootStyle, children.toArray(new NodeId[0]));
        return new Object[]{tree, root};
    }

    private static Style randomFlexItemStyle(Random rng) {
        Style style = new Style();
        style.size = new Size<>(randomDimension(rng), randomDimension(rng));
        return style;
    }

    private static Style randomFlexContainerStyle(Random rng) {
        Style style = new Style();
        style.display = Display.FLEX;
        style.flexDirection = rng.nextBoolean() ? FlexDirection.ROW : FlexDirection.COLUMN;
        style.flexWrap = rng.nextBoolean() ? FlexWrap.WRAP : FlexWrap.NO_WRAP;
        return style;
    }

    private static Dimension randomDimension(Random rng) {
        float rand = rng.nextFloat();
        if (rand < 0.2f) {
            return Dimension.AUTO;
        } else if (rand < 0.8f) {
            return Dimension.length(rng.nextFloat() * 500f);
        } else {
            return Dimension.percent(rng.nextFloat());
        }
    }

    private static NodeId buildRandomLeaf(TaffyTree taffy) {
        Style style = new Style();
        style.size = new Size<>(Dimension.length(20.0f), Dimension.length(20.0f));
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

        Style style = new Style();
        style.display = Display.GRID;

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
