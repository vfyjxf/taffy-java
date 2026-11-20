package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyStyle;
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

import java.util.concurrent.TimeUnit;

/**
 * Measure-leaf (text-like) coverage, split into the phases a measured layout goes through.
 *
 * <p>Upstream counterparts: {@code benches/relayout.rs} ({@code measure_cold} /
 * {@code measure_warm_children}) and the measure-heavy PanGUI workloads. The tree shape is a
 * column flex root of {@code leafCount = 1k / 10k} measured leaves, each with a percentage width
 * and an auto height, so every leaf reaches the measure callback.</p>
 *
 * <p>Two callback kinds are parameterised:</p>
 * <ul>
 *   <li>{@code fixed}: returns a constant 10x10 content size (cheap callback, isolates the
 *       layout-side measure plumbing);</li>
 *   <li>{@code text}: the deterministic 16-word wrapping callback from {@link BenchSupport}
 *       (line height 16), which does real per-call work like a text leaf.</li>
 * </ul>
 *
 * <p>The measured methods decompose the flow:</p>
 * <ul>
 *   <li>{@code newLeafWithMeasureOnly}: allocate the leaves and attach the callback, no parent,
 *       no layout;</li>
 *   <li>{@code buildTreeOnly}: leaves + root construction, no layout;</li>
 *   <li>{@code buildTreeAndLayout}: tree construction and one layout in the same invocation;</li>
 *   <li>{@code layoutCold}: tree built once in trial setup, all caches cleared per invocation;</li>
 *   <li>{@code layoutWarmChildren}: only the root cache is cleared, measured children answer from
 *       their caches;</li>
 *   <li>{@code measureDirect}: the callback alone, with the same known dimensions the layout
 *       passes;</li>
 *   <li>{@code layoutNoMeasure}: the same tree with fixed-size leaves and no callback, for the
 *       non-measured baseline.</li>
 * </ul>
 *
 * <p>The build methods intentionally construct a fresh tree per invocation. They are the only
 * methods in this suite that do so; the layout methods follow the suite rule of one trial-built
 * tree plus cache invalidation.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
@Fork(1)
public class LeafFlowBenchmark {

    private static final FloatSize NONE = new FloatSize(Float.NaN, Float.NaN);
    private static final TaffySize<AvailableSpace> MAX_CONTENT =
        new TaffySize<>(AvailableSpace.MAX_CONTENT, AvailableSpace.MAX_CONTENT);

    // === Layout of a trial-built measured tree ===

    @State(Scope.Thread)
    public static class LayoutState {
        @Param({"1000", "10000"})
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
    public static class WarmLayoutState {
        @Param({"1000", "10000"})
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
    public void layoutCold(LayoutState state, Blackhole bh) {
        state.tree.computeLayout(state.root, PhaseSupport.measureSpace());
        bh.consume(state.tree.getLayout(state.root));
    }

    @Benchmark
    public void layoutWarmChildren(WarmLayoutState state, Blackhole bh) {
        state.tree.computeLayout(state.root, PhaseSupport.measureSpace());
        bh.consume(state.tree.getLayout(state.root));
    }

    // === Build decomposition (fresh tree per invocation) ===

    @State(Scope.Thread)
    public static class BuildState {
        @Param({"1000", "10000"})
        public int leafCount;

        @Param({"fixed", "text"})
        public String measureKind;

        TaffyStyle leafStyle;
        MeasureFunc measure;

        @Setup(Level.Trial)
        public void setupTrial() {
            measure = PhaseSupport.measureOf(measureKind);
            leafStyle = PhaseSupport.measuredLeafStyle();
        }
    }

    @Benchmark
    public void newLeafWithMeasureOnly(BuildState state, Blackhole bh) {
        TaffyTree tree = new TaffyTree();
        for (int i = 0; i < state.leafCount; i++) {
            tree.newLeafWithMeasure(state.leafStyle, state.measure);
        }
        bh.consume(tree);
    }

    @Benchmark
    public void buildTreeOnly(BuildState state, Blackhole bh) {
        TaffyTree tree = new TaffyTree();
        NodeId root = PhaseSupport.buildMeasuredTree(tree, state.leafCount, state.measure, state.leafStyle);
        bh.consume(root);
        bh.consume(tree);
    }

    @Benchmark
    public void buildTreeAndLayout(BuildState state, Blackhole bh) {
        TaffyTree tree = new TaffyTree();
        NodeId root = PhaseSupport.buildMeasuredTree(tree, state.leafCount, state.measure, state.leafStyle);
        tree.computeLayout(root, PhaseSupport.measureSpace());
        bh.consume(tree.getLayout(root));
    }

    // === Callback alone ===

    @State(Scope.Thread)
    public static class MeasureOnlyState {
        @Param({"1000", "10000"})
        public int leafCount;

        @Param({"fixed", "text"})
        public String measureKind;

        MeasureFunc measure;

        @Setup(Level.Trial)
        public void setupTrial() {
            measure = PhaseSupport.measureOf(measureKind);
        }
    }

    @Benchmark
    public void measureDirect(MeasureOnlyState state, Blackhole bh) {
        for (int i = 0; i < state.leafCount; i++) {
            bh.consume(state.measure.measure(NONE, MAX_CONTENT));
        }
    }

    // === Same layout without a measure callback ===

    @State(Scope.Thread)
    public static class NoMeasureState {
        @Param({"1000", "10000"})
        public int leafCount;

        TaffyTree tree;
        NodeId root;
        NodeId[] nodes;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.disableRounding();
            TaffyStyle leafStyle = new TaffyStyle();
            leafStyle.size = new TaffySize<>(TaffyDimension.length(10f), TaffyDimension.length(10f));
            NodeId[] children = new NodeId[leafCount];
            for (int i = 0; i < leafCount; i++) {
                children[i] = tree.newLeaf(leafStyle);
            }
            root = tree.newWithChildren(
                BenchSupport.sizedFlex(FlexDirection.COLUMN, PhaseSupport.MEASURE_ROOT_WIDTH, Float.NaN),
                children);
            nodes = BenchSupport.collectNodes(tree, root);
            tree.computeLayout(root, PhaseSupport.measureSpace());
        }

        @Setup(Level.Invocation)
        public void clearAllCaches() {
            BenchSupport.clearCaches(tree, nodes);
        }
    }

    @Benchmark
    public void layoutNoMeasure(NoMeasureState state, Blackhole bh) {
        state.tree.computeLayout(state.root, PhaseSupport.measureSpace());
        bh.consume(state.tree.getLayout(state.root));
    }
}
