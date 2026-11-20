package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
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
import java.util.concurrent.TimeUnit;

/**
 * Incremental layout and cache coverage.
 *
 * <p>Upstream counterpart: {@code benches/relayout.rs}. Each benchmark isolates one incremental
 * scenario:</p>
 * <ul>
 *   <li>{@code coldLayout}: every node's cache is cleared, so the whole tree is laid out;</li>
 *   <li>{@code styleChangeRelayout}: one leaf's style changes; ancestors are recomputed and
 *       untouched siblings are answered from their caches;</li>
 *   <li>{@code geometryUnchanged}: layout is requested again with identical inputs, which is a
 *       pure root-cache hit;</li>
 *   <li>{@code resizeRelayout}: the available width alternates, so the root recomputes while
 *       nodes whose inputs did not change hit their caches;</li>
 *   <li>{@code addRemoveAndRelayout}: a child is attached, the tree is laid out, and the child is
 *       detached again;</li>
 *   <li>{@code measureCold}/{@code measureWarmChildren}: a tree of measured leaves with cold
 *       caches versus valid child caches.</li>
 * </ul>
 *
 * <p>Rounding is disabled for the incremental benchmarks, mirroring upstream's relayout benches:
 * the rounding pass visits every node on every layout and would otherwise dominate the cache
 * measurements.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
@Fork(1)
public class IncrementalLayoutBenchmark {

    private static final float LAYOUT_WIDTH = 2000f;

    // === Cold layout, one layout algorithm at a time ===

    @State(Scope.Thread)
    public static class ColdState {
        @Param({"flex", "block", "grid", "mixed"})
        public String display;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            TaffyTree tree = new TaffyTree();
            TaffyDisplay target = "mixed".equals(display)
                                  ? null
                                  : TaffyDisplay.valueOf(display.toUpperCase());
            NodeId[] firstLeaf = new NodeId[1];
            NodeId root = buildSubtree(tree, 10, 0, 3, target, firstLeaf);
            treeCase = new BenchSupport.TreeCase(tree, root);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void coldLayout(ColdState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, layoutSize());
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === Style change on one leaf ===

    @State(Scope.Thread)
    public static class StyleChangeState {
        @Param({"wide", "balanced", "deep"})
        public String shape;

        TaffyTree tree;
        NodeId root;
        NodeId leaf;
        TaffyStyle shortStyle;
        TaffyStyle tallStyle;
        boolean tall;

        @Setup(Level.Trial)
        public void setupTrial() {
            int fanout;
            int depth;
            switch (shape) {
                case "wide" -> {
                    fanout = 100;
                    depth = 2;
                }
                case "balanced" -> {
                    fanout = 10;
                    depth = 4;
                }
                case "deep" -> {
                    fanout = 2;
                    depth = 12;
                }
                default -> throw new IllegalArgumentException("Unknown shape: " + shape);
            }

            tree = new TaffyTree();
            tree.disableRounding();
            NodeId[] firstLeaf = new NodeId[1];
            root = buildSubtree(tree, fanout, 0, depth, null, firstLeaf);
            leaf = firstLeaf[0];
            shortStyle = leafStyle(10f);
            tallStyle = leafStyle(20f);
            tree.computeLayout(root, layoutSize());
        }
    }

    @Benchmark
    public void styleChangeRelayout(StyleChangeState state, Blackhole bh) {
        state.tall = !state.tall;
        state.tree.setStyle(state.leaf, state.tall ? state.tallStyle : state.shortStyle);
        state.tree.computeLayout(state.root, layoutSize());
        bh.consume(state.tree.getLayout(state.root));
    }

    // === Geometry unchanged (pure cache hit) ===

    @State(Scope.Thread)
    public static class GeometryState {
        TaffyTree tree;
        NodeId root;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.disableRounding();
            NodeId[] firstLeaf = new NodeId[1];
            root = buildSubtree(tree, 10, 0, 3, null, firstLeaf);
            tree.computeLayout(root, layoutSize());
        }
    }

    @Benchmark
    public void geometryUnchanged(GeometryState state, Blackhole bh) {
        state.tree.computeLayout(state.root, layoutSize());
        bh.consume(state.tree.getLayout(state.root));
    }

    // === Resize relayout ===

    @State(Scope.Thread)
    public static class ResizeState {
        TaffyTree tree;
        NodeId root;
        boolean wide;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.disableRounding();
            NodeId[] firstLeaf = new NodeId[1];
            root = buildSubtree(tree, 10, 0, 3, null, firstLeaf);
            tree.computeLayout(root, layoutSize());
        }
    }

    @Benchmark
    public void resizeRelayout(ResizeState state, Blackhole bh) {
        state.wide = !state.wide;
        float width = state.wide ? 2500f : 2000f;
        state.tree.computeLayout(state.root, TaffySize.of(
            AvailableSpace.definite(width),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.tree.getLayout(state.root));
    }

    // === Add / remove a child and relayout ===

    @State(Scope.Thread)
    public static class AddRemoveState {
        TaffyTree tree;
        NodeId root;
        NodeId detached;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.disableRounding();
            TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, LAYOUT_WIDTH, Float.NaN);
            TaffyStyle childStyle = BenchSupport.sized(20f, 10f);
            NodeId[] children = new NodeId[100];
            for (int i = 0; i < children.length; i++) {
                children[i] = tree.newLeaf(childStyle);
            }
            root = tree.newWithChildren(rootStyle, children);
            detached = tree.newLeaf(childStyle);
            tree.computeLayout(root, layoutSize());
        }
    }

    @Benchmark
    public void addRemoveAndRelayout(AddRemoveState state, Blackhole bh) {
        state.tree.addChild(state.root, state.detached);
        state.tree.computeLayout(state.root, layoutSize());
        bh.consume(state.tree.getLayout(state.root));
        state.tree.removeChild(state.root, state.detached);
    }

    // === Measure cache: cold vs valid child caches ===

    @State(Scope.Thread)
    public static class MeasureState {
        @Param({"200"})
        public int leafCount;

        TaffyTree tree;
        NodeId root;
        NodeId[] nodes;
        BenchSupport.WordWrapMeasure measure;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.disableRounding();
            measure = new BenchSupport.WordWrapMeasure(16);
            TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, 800f, Float.NaN);
            NodeId[] children = new NodeId[leafCount];
            TaffyStyle leafStyle = new TaffyStyle();
            leafStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
            for (int i = 0; i < leafCount; i++) {
                children[i] = tree.newLeafWithMeasure(leafStyle, measure);
            }
            root = tree.newWithChildren(rootStyle, children);
            nodes = BenchSupport.collectNodes(tree, root);

            tree.computeLayout(root, measureSize());
            if (measure.callCount() == 0L) {
                throw new IllegalStateException("measure callback was not invoked during setup");
            }
        }

        @Setup(Level.Invocation)
        public void clearAllCaches() {
            BenchSupport.clearCaches(tree, nodes);
        }
    }

    @Benchmark
    public void measureCold(MeasureState state, Blackhole bh) {
        state.tree.computeLayout(state.root, measureSize());
        bh.consume(state.tree.getLayout(state.root));
    }

    @Benchmark
    public void measureWarmChildren(MeasureState state, Blackhole bh) {
        state.tree.clearCache(state.root);
        state.tree.computeLayout(state.root, measureSize());
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== Builders ====================

    private static TaffySize<AvailableSpace> layoutSize() {
        return TaffySize.of(AvailableSpace.definite(LAYOUT_WIDTH), AvailableSpace.maxContent());
    }

    private static TaffySize<AvailableSpace> measureSize() {
        return TaffySize.of(AvailableSpace.definite(800f), AvailableSpace.maxContent());
    }

    private static TaffyStyle leafStyle(float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(10f), TaffyDimension.length(height));
        return style;
    }

    /**
     * Builds a recursive container tree with {@code fanout} children per level and fixed-size
     * leaves at {@code maxDepth}. A {@code null} display cycles flex/block/grid by depth, which is
     * upstream relayout's {@code mixed} configuration.
     */
    private static NodeId buildSubtree(
        TaffyTree tree,
        int fanout,
        int depth,
        int maxDepth,
        TaffyDisplay display,
        NodeId[] firstLeaf
    ) {
        if (depth == maxDepth) {
            NodeId leaf = tree.newLeaf(leafStyle(10f));
            if (firstLeaf[0] == null) {
                firstLeaf[0] = leaf;
            }
            return leaf;
        }

        NodeId[] children = new NodeId[fanout];
        for (int i = 0; i < fanout; i++) {
            children[i] = buildSubtree(tree, fanout, depth + 1, maxDepth, display, firstLeaf);
        }

        TaffyDisplay actual = display != null
                              ? display
                              : new TaffyDisplay[]{TaffyDisplay.FLEX, TaffyDisplay.BLOCK, TaffyDisplay.GRID}[depth % 3];
        return tree.newWithChildren(containerStyle(actual, depth), children);
    }

    private static TaffyStyle containerStyle(TaffyDisplay display, int depth) {
        TaffyStyle style = new TaffyStyle();
        style.display = display;
        if (display == TaffyDisplay.FLEX || display == TaffyDisplay.FLOW_ROOT) {
            style.flexDirection = (depth & 1) == 0 ? FlexDirection.COLUMN : FlexDirection.ROW;
        }
        if (display == TaffyDisplay.GRID) {
            style.gridTemplateColumns = List.of(TrackSizingFunction.auto(), TrackSizingFunction.auto());
        }
        style.padding = dev.vfyjxf.taffy.geometry.TaffyRect.all(
            dev.vfyjxf.taffy.style.LengthPercentage.length(1f));
        return style;
    }
}
