package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.GridAutoFlow;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * CSS Grid coverage: fixed/fr/auto tracks, auto-flow modes, spanning items and nested grids.
 *
 * <p>Upstream counterpart: {@code benches/grid.rs} ({@code grid/wide}, {@code grid/deep},
 * {@code grid/superdeep}). Every benchmark uses a tree built once in {@code Level.Trial} setup and
 * clears all caches in {@code Level.Invocation} setup, so the measured call is a cold layout.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
@Fork(1)
public class GridCoverageBenchmark {

    // === Track sizing: fixed / fr / auto ===

    @State(Scope.Thread)
    public static class TrackState {
        @Param({"fixed", "fr", "auto"})
        public String trackMode;

        @Param({"100", "1000", "10000"})
        public int cellCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            treeCase = buildTrackGrid(trackMode, cellCount);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void trackSizing(TrackState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(12000f),
            AvailableSpace.definite(12000f)
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === Auto-flow: row / column / row-dense / column-dense ===

    @State(Scope.Thread)
    public static class AutoFlowState {
        @Param({"ROW", "COLUMN", "ROW_DENSE", "COLUMN_DENSE"})
        public String flow;

        @Param({"1000"})
        public int itemCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            treeCase = buildAutoFlowGrid(GridAutoFlow.valueOf(flow), itemCount);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void autoFlow(AutoFlowState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.maxContent());
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === Spanning items ===

    @State(Scope.Thread)
    public static class SpanState {
        @Param({"1000", "10000"})
        public int itemCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            treeCase = buildSpanGrid(itemCount);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void spanningItems(SpanState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(12000f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === Nested grids ===

    @State(Scope.Thread)
    public static class NestedState {
        @Param({"2_5", "3_4"})
        public String config;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            String[] parts = config.split("_");
            treeCase = buildDeepGrid(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void nestedGrids(NestedState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(12000f),
            AvailableSpace.definite(12000f)
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // ==================== Builders ====================

    private static BenchSupport.TreeCase buildTrackGrid(String mode, int cellCount) {
        TaffyTree tree = new TaffyTree();
        int side = BenchSupport.side(cellCount);
        TaffyStyle rootStyle = gridStyle();
        rootStyle.gridTemplateColumns = tracks(mode, side);
        rootStyle.gridTemplateRows = tracks(mode, side);

        TaffyStyle leafStyle = BenchSupport.sized(20f, 20f);
        NodeId[] children = new NodeId[cellCount];
        for (int i = 0; i < cellCount; i++) {
            children[i] = tree.newLeaf(leafStyle);
        }
        return new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, children));
    }

    private static List<TrackSizingFunction> tracks(String mode, int count) {
        return switch (mode) {
            case "fixed" -> BenchSupport.fixedTracks(count, 20f);
            case "fr" -> BenchSupport.frTracks(count);
            case "auto" -> BenchSupport.autoTracks(count);
            default -> throw new IllegalArgumentException("Unknown track mode: " + mode);
        };
    }

    private static BenchSupport.TreeCase buildAutoFlowGrid(GridAutoFlow flow, int itemCount) {
        TaffyTree tree = new TaffyTree();
        boolean rowFlow = flow.isRow();
        int fixedTracks = 10;

        TaffyStyle rootStyle = gridStyle();
        rootStyle.gridAutoFlow = flow;
        if (rowFlow) {
            rootStyle.gridTemplateColumns = BenchSupport.fixedTracks(fixedTracks, 20f);
        } else {
            rootStyle.gridTemplateRows = BenchSupport.fixedTracks(fixedTracks, 20f);
        }
        rootStyle.gridAutoRows = BenchSupport.fixedTracks(1, 20f);
        rootStyle.gridAutoColumns = BenchSupport.fixedTracks(1, 20f);

        TaffyStyle leafStyle = BenchSupport.sized(10f, 10f);
        NodeId[] children = new NodeId[itemCount];
        for (int i = 0; i < itemCount; i++) {
            TaffyStyle itemStyle = leafStyle;
            if (i % 2 == 0) {
                TaffyLine<GridPlacement> span = new TaffyLine<>(
                    GridPlacement.line(1),
                    GridPlacement.span(2)
                );
                if (rowFlow) {
                    TaffyStyle style = leafStyle.copy();
                    style.gridRow = span;
                    itemStyle = style;
                } else {
                    TaffyStyle style = leafStyle.copy();
                    style.gridColumn = span;
                    itemStyle = style;
                }
            } else {
                TaffyStyle style = leafStyle.copy();
                if (rowFlow) {
                    style.gridRow = new TaffyLine<>(GridPlacement.line(2), GridPlacement.AUTO_INSTANCE);
                } else {
                    style.gridColumn = new TaffyLine<>(GridPlacement.line(2), GridPlacement.AUTO_INSTANCE);
                }
                itemStyle = style;
            }
            children[i] = tree.newLeaf(itemStyle);
        }
        return new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, children));
    }

    private static BenchSupport.TreeCase buildSpanGrid(int itemCount) {
        TaffyTree tree = new TaffyTree();
        int side = BenchSupport.side(itemCount);
        TaffyStyle rootStyle = gridStyle();
        rootStyle.gridTemplateColumns = BenchSupport.frTracks(side);
        rootStyle.gridAutoRows = BenchSupport.repeatTrack(TrackSizingFunction.minmax(
            TrackSizingFunction.fixed(20f),
            TrackSizingFunction.auto()
        ), 1);

        TaffyStyle leafStyle = BenchSupport.sized(10f, 10f);
        NodeId[] children = new NodeId[itemCount];
        for (int i = 0; i < itemCount; i++) {
            TaffyStyle itemStyle = leafStyle.copy();
            if ((i & 1) == 0) {
                itemStyle.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.span(2));
            } else {
                itemStyle.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.span(2));
            }
            children[i] = tree.newLeaf(itemStyle);
        }
        return new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, children));
    }

    /** Nested {@code tracks x tracks} grids, {@code levels} deep (upstream grid/deep). */
    private static BenchSupport.TreeCase buildDeepGrid(int tracks, int levels) {
        TaffyTree tree = new TaffyTree();
        Random rng = new Random(BenchSupport.SEED);
        NodeId[] rootChildren = buildDeepGridLevel(tree, tracks, levels, rng);
        TaffyStyle rootStyle = new TaffyStyle();
        return new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, rootChildren));
    }

    private static NodeId[] buildDeepGridLevel(TaffyTree tree, int tracks, int levels, Random rng) {
        int childCount = tracks * tracks;
        if (levels == 1) {
            NodeId[] leaves = new NodeId[childCount];
            TaffyStyle leafStyle = BenchSupport.sized(20f, 20f);
            for (int i = 0; i < childCount; i++) {
                leaves[i] = tree.newLeaf(leafStyle);
            }
            return leaves;
        }

        NodeId[] children = new NodeId[childCount];
        for (int i = 0; i < childCount; i++) {
            NodeId[] subChildren = buildDeepGridLevel(tree, tracks, levels - 1, rng);
            TaffyStyle style = randomGridStyle(rng, tracks);
            children[i] = tree.newWithChildren(style, subChildren);
        }
        return children;
    }

    private static TaffyStyle gridStyle() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        return style;
    }

    private static TaffyStyle randomGridStyle(Random rng, int trackCount) {
        TaffyStyle style = gridStyle();
        style.gridTemplateColumns = randomTracks(rng, trackCount);
        style.gridTemplateRows = randomTracks(rng, trackCount);
        return style;
    }

    private static List<TrackSizingFunction> randomTracks(Random rng, int count) {
        List<TrackSizingFunction> tracks = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            tracks.add(randomTrack(rng));
        }
        return tracks;
    }

    private static TrackSizingFunction randomTrack(Random rng) {
        float value = rng.nextFloat();
        if (value < 0.1f) {
            return TrackSizingFunction.auto();
        } else if (value < 0.2f) {
            return TrackSizingFunction.minContent();
        } else if (value < 0.3f) {
            return TrackSizingFunction.maxContent();
        } else if (value < 0.5f) {
            return TrackSizingFunction.fr(1f);
        } else if (value < 0.6f) {
            return TrackSizingFunction.minmax(TrackSizingFunction.fixed(0f), TrackSizingFunction.fr(1f));
        } else if (value < 0.8f) {
            return TrackSizingFunction.fixed(40f);
        } else {
            return TrackSizingFunction.percent(0.3f);
        }
    }
}
