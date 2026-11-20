package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
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
import java.util.concurrent.TimeUnit;

/**
 * Incremental re-layout phases.
 *
 * <p>Upstream counterpart: {@code benches/relayout.rs}. Rounding is disabled everywhere, as
 * upstream does for these benchmarks: the rounding pass visits every node of the tree on every
 * layout and would hide the cache effects being measured.</p>
 *
 * <p>Every tree is a flat column flex root with {@code nodeCount - 1} fixed 10x10 leaves
 * ({@code nodeCount = 1k / 10k / 100k}), built once in {@code @Setup(Level.Trial)} and laid out
 * once so that all caches are warm before the first measured invocation.</p>
 *
 * <p>Scenarios:</p>
 * <ul>
 *   <li>{@code repeatCompute}: identical inputs again; a pure root-cache hit (sentinel, not a
 *       layout throughput number);</li>
 *   <li>{@code resizeRoot}: the available width alternates 2000/2500; the root recomputes and
 *       children whose inputs did not change hit their caches;</li>
 *   <li>{@code styleChangeLeaf}: one leaf's style height toggles 10/20; ancestors recompute;</li>
 *   <li>{@code addRemoveChild}: a detached leaf is attached, the tree is laid out, the leaf is
 *       detached again;</li>
 *   <li>{@code measureCold}: all caches cleared, so every measured leaf is measured again;</li>
 *   <li>{@code measureWarmChildren}: only the root cache is cleared, measured children answer
 *       from their caches;</li>
 *   <li>{@code clearCaches}: only the invalidation walk is timed (no layout), which prices the
 *       {@code clearCache} per-node cost the cold scenarios hide inside setup;</li>
 *   <li>{@code geometryUnchanged} / {@code geometryUnchangedRounded}: the upstream relayout
 *       {@code geometry_unchanged} shape (fanout 10, depth 3, 1111 nodes, containers cycling
 *       flex/block/grid) with the root cache already valid, once with rounding disabled and once
 *       with it enabled. The first times the root cache fast path alone; the second times the
 *       same fast path plus the tree-wide rounding pass, so the pair prices what rounding adds
 *       to a no-op re-layout.</li>
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
@Fork(1)
public class IncrementalPhaseBenchmark {

    private static final float LAYOUT_WIDTH = 2000f;
    private static final float RESIZE_WIDTH = 2500f;

    // === Same inputs again (root cache hit) ===

    @State(Scope.Thread)
    public static class RepeatState {
        @Param({"1000", "10000", "100000"})
        public int nodeCount;

        TaffyTree tree;
        NodeId root;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.disableRounding();
            root = buildFlatFlexTree(tree, nodeCount);
            tree.computeLayout(root, layoutSpace());
        }
    }

    @Benchmark
    public void repeatCompute(RepeatState state, Blackhole bh) {
        state.tree.computeLayout(state.root, layoutSpace());
        bh.consume(state.tree.getLayout(state.root));
    }

    // === Resize the root available space, keep child caches ===

    @State(Scope.Thread)
    public static class ResizeState {
        @Param({"1000", "10000", "100000"})
        public int nodeCount;

        TaffyTree tree;
        NodeId root;
        boolean wide;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.disableRounding();
            root = buildFlatFlexTree(tree, nodeCount);
            tree.computeLayout(root, layoutSpace());
        }
    }

    @Benchmark
    public void resizeRoot(ResizeState state, Blackhole bh) {
        state.wide = !state.wide;
        float width = state.wide ? RESIZE_WIDTH : LAYOUT_WIDTH;
        state.tree.computeLayout(state.root, TaffySize.of(
            AvailableSpace.definite(width), AvailableSpace.maxContent()));
        bh.consume(state.tree.getLayout(state.root));
    }

    // === Change one leaf style, relayout ===

    @State(Scope.Thread)
    public static class StyleChangeState {
        @Param({"1000", "10000", "100000"})
        public int nodeCount;

        TaffyTree tree;
        NodeId root;
        NodeId leaf;
        TaffyStyle shortStyle;
        TaffyStyle tallStyle;
        boolean tall;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.disableRounding();
            shortStyle = leafStyle(10f);
            tallStyle = leafStyle(20f);
            NodeId[] firstChild = new NodeId[1];
            root = buildFlatFlexTree(tree, nodeCount, shortStyle, firstChild);
            leaf = firstChild[0];
            tree.computeLayout(root, layoutSpace());
        }
    }

    @Benchmark
    public void styleChangeLeaf(StyleChangeState state, Blackhole bh) {
        state.tall = !state.tall;
        state.tree.setStyle(state.leaf, state.tall ? state.tallStyle : state.shortStyle);
        state.tree.computeLayout(state.root, layoutSpace());
        bh.consume(state.tree.getLayout(state.root));
    }

    // === Add / remove a child and relayout ===

    @State(Scope.Thread)
    public static class AddRemoveState {
        @Param({"1000", "10000", "100000"})
        public int nodeCount;

        TaffyTree tree;
        NodeId root;
        NodeId detached;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.disableRounding();
            root = buildFlatFlexTree(tree, nodeCount);
            detached = tree.newLeaf(leafStyle(10f));
            tree.computeLayout(root, layoutSpace());
        }
    }

    @Benchmark
    public void addRemoveChild(AddRemoveState state, Blackhole bh) {
        state.tree.addChild(state.root, state.detached);
        state.tree.computeLayout(state.root, layoutSpace());
        bh.consume(state.tree.getLayout(state.root));
        state.tree.removeChild(state.root, state.detached);
    }

    // === Measure cache: cold vs warm children ===

    @State(Scope.Thread)
    public static class MeasureColdState {
        @Param({"1000", "10000", "100000"})
        public int leafCount;

        @Param({"fixed", "text"})
        public String measureKind;

        TaffyTree tree;
        NodeId root;
        NodeId[] nodes;
        MeasureFunc measure;

        @Setup(Level.Trial)
        public void setupTrial() {
            measure = PhaseSupport.measureOf(measureKind);
            tree = new TaffyTree();
            tree.disableRounding();
            root = PhaseSupport.buildMeasuredTree(tree, leafCount, measure);
            nodes = BenchSupport.collectNodes(tree, root);
            tree.computeLayout(root, PhaseSupport.measureSpace());
            if (PhaseSupport.measureCallCount(measure) == 0L) {
                throw new IllegalStateException("measure callback was not invoked during setup");
            }
        }

        @Setup(Level.Invocation)
        public void clearAllCaches() {
            BenchSupport.clearCaches(tree, nodes);
        }
    }

    @State(Scope.Thread)
    public static class MeasureWarmState {
        @Param({"1000", "10000", "100000"})
        public int leafCount;

        @Param({"fixed", "text"})
        public String measureKind;

        TaffyTree tree;
        NodeId root;
        MeasureFunc measure;

        @Setup(Level.Trial)
        public void setupTrial() {
            measure = PhaseSupport.measureOf(measureKind);
            tree = new TaffyTree();
            tree.disableRounding();
            root = PhaseSupport.buildMeasuredTree(tree, leafCount, measure);
            tree.computeLayout(root, PhaseSupport.measureSpace());
            if (PhaseSupport.measureCallCount(measure) == 0L) {
                throw new IllegalStateException("measure callback was not invoked during setup");
            }
        }

        @Setup(Level.Invocation)
        public void clearRootCache() {
            tree.clearCache(root);
        }
    }

    @Benchmark
    public void measureCold(MeasureColdState state, Blackhole bh) {
        state.tree.computeLayout(state.root, PhaseSupport.measureSpace());
        bh.consume(state.tree.getLayout(state.root));
    }

    @Benchmark
    public void measureWarmChildren(MeasureWarmState state, Blackhole bh) {
        state.tree.computeLayout(state.root, PhaseSupport.measureSpace());
        bh.consume(state.tree.getLayout(state.root));
    }

    // === Invalidation walk only ===

    @State(Scope.Thread)
    public static class ClearCacheState {
        @Param({"1000", "10000", "100000"})
        public int nodeCount;

        TaffyTree tree;
        NodeId root;
        NodeId[] nodes;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.disableRounding();
            root = buildFlatFlexTree(tree, nodeCount);
            nodes = BenchSupport.collectNodes(tree, root);
            tree.computeLayout(root, layoutSpace());
        }
    }

    @Benchmark
    public void clearCaches(ClearCacheState state, Blackhole bh) {
        BenchSupport.clearCaches(state.tree, state.nodes);
        bh.consume(state.tree);
    }

    // === Geometry unchanged: root fast path with and without rounding ===

    @State(Scope.Thread)
    public static class GeometryState {
        TaffyTree tree;
        NodeId root;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.disableRounding();
            root = buildGeometryTree(tree);
            tree.computeLayout(root, layoutSpace());
        }
    }

    @State(Scope.Thread)
    public static class GeometryRoundedState {
        TaffyTree tree;
        NodeId root;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.enableRounding();
            root = buildGeometryTree(tree);
            tree.computeLayout(root, layoutSpace());
        }
    }

    @Benchmark
    public void geometryUnchanged(GeometryState state, Blackhole bh) {
        state.tree.computeLayout(state.root, layoutSpace());
        bh.consume(state.tree.getLayout(state.root));
    }

    @Benchmark
    public void geometryUnchangedRounded(GeometryRoundedState state, Blackhole bh) {
        state.tree.computeLayout(state.root, layoutSpace());
        bh.consume(state.tree.getLayout(state.root));
    }

    // === Helpers ===

    private static TaffySize<AvailableSpace> layoutSpace() {
        return TaffySize.of(AvailableSpace.definite(LAYOUT_WIDTH), AvailableSpace.maxContent());
    }

    private static TaffyStyle leafStyle(float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(10f), TaffyDimension.length(height));
        return style;
    }

    private static NodeId buildFlatFlexTree(TaffyTree tree, int nodeCount) {
        return buildFlatFlexTree(tree, nodeCount, leafStyle(10f), null);
    }

    private static NodeId buildFlatFlexTree(TaffyTree tree, int nodeCount, TaffyStyle leafStyle,
                                            NodeId[] firstChild) {
        TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, LAYOUT_WIDTH, Float.NaN);
        int childCount = Math.max(1, nodeCount - 1);
        NodeId[] children = new NodeId[childCount];
        for (int i = 0; i < childCount; i++) {
            children[i] = tree.newLeaf(leafStyle);
        }
        if (firstChild != null) {
            firstChild[0] = children[0];
        }
        return tree.newWithChildren(rootStyle, children);
    }

    /**
     * The upstream relayout {@code geometry_unchanged} shape: fanout 10, depth 3, so
     * {@code 1 + 10 + 100 + 1000 = 1111} nodes, with containers cycling flex/block/grid.
     */
    private static NodeId buildGeometryTree(TaffyTree tree) {
        NodeId root = buildGeometrySubtree(tree, 10, 0, 3);
        if (tree.totalNodeCount() != 1111) {
            throw new IllegalStateException("geometry shape should have 1111 nodes, got "
                + tree.totalNodeCount());
        }
        return root;
    }

    private static NodeId buildGeometrySubtree(TaffyTree tree, int fanout, int depth, int maxDepth) {
        if (depth == maxDepth) {
            return tree.newLeaf(leafStyle(10f));
        }
        NodeId[] children = new NodeId[fanout];
        for (int i = 0; i < fanout; i++) {
            children[i] = buildGeometrySubtree(tree, fanout, depth + 1, maxDepth);
        }
        return tree.newWithChildren(geometryContainerStyle(depth), children);
    }

    private static TaffyStyle geometryContainerStyle(int depth) {
        TaffyStyle style = new TaffyStyle();
        TaffyDisplay display = switch (depth % 3) {
            case 0 -> TaffyDisplay.FLEX;
            case 1 -> TaffyDisplay.BLOCK;
            default -> TaffyDisplay.GRID;
        };
        style.display = display;
        if (display == TaffyDisplay.FLEX) {
            style.flexDirection = (depth & 1) == 0 ? FlexDirection.COLUMN : FlexDirection.ROW;
        }
        if (display == TaffyDisplay.GRID) {
            style.gridTemplateColumns = List.of(
                TrackSizingFunction.auto(), TrackSizingFunction.auto());
        }
        style.padding = TaffyRect.all(LengthPercentage.length(1f));
        return style;
    }
}
