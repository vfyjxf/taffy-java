package dev.vfyjxf.taffy.benchmark;

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

import java.util.concurrent.TimeUnit;

/**
 * Algorithm-phase coverage with no measure callbacks.
 *
 * <p>The class isolates the flexbox, block and grid layout algorithms from measurement, tree
 * construction and incremental caching. Every tree is built once in {@code @Setup(Level.Trial)}
 * and only its layout caches are cleared before each measured invocation, which is the Java
 * counterpart of upstream taffy's {@code mark_all_dirty} + one layout pass.</p>
 *
 * <p>Scenarios:</p>
 * <ul>
 *   <li><b>flat</b>: one container with {@code nodeCount - 1} leaf children
 *       ({@code nodeCount = 1k / 10k / 100k});</li>
 *   <li><b>deep</b>: a chain of {@code depth = 64 / 256 / 1024} containers, each with one
 *       continuing child and one leaf side child;</li>
 *   <li><b>sizing</b>: {@code auto} (auto-sized leaf with padding/border), {@code percent}
 *       (10% x 10% leaf under a definite 2000x2000 container) and {@code margin} (fixed leaf with
 *       a 2px margin). The {@code fixed} variant is the {@code *Fixed} methods themselves.</li>
 * </ul>
 *
 * <p>Rounding is split into separate methods ({@code *Rounded}) instead of a parameter because it
 * is a tree-wide pass that visits every node after the layout algorithm, and a 100k-node tree makes
 * that pass a material part of the measurement. Comparing {@code flatFixed} with
 * {@code flatFixedRounded} therefore prices the rounding pass without changing the layout work.</p>
 *
 * <p>Upstream counterparts: {@code benches/flexbox.rs} (deep/wide), {@code benches/grid.rs}
 * (deep/superdeep) and {@code benches/relayout.rs} (shape breadth). The {@code percent}/{@code auto}
 * variants have no direct upstream benchmark; they exist to separate style resolution from the
 * algorithm itself.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
@Fork(1)
public class PhaseLayoutBenchmark {

    // === Flat shape, fixed-size leaves ===

    @State(Scope.Thread)
    public static class FlatFixedState {
        @Param({"flex", "block", "grid"})
        public String display;

        @Param({"1000", "10000", "100000"})
        public int nodeCount;

        TaffyTree tree;
        NodeId root;
        NodeId[] nodes;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.disableRounding();
            root = PhaseSupport.buildFlat(tree, display, nodeCount, PhaseSupport.FIXED);
            nodes = BenchSupport.collectNodes(tree, root);
            tree.computeLayout(root, PhaseSupport.layoutSpace());
        }

        @Setup(Level.Invocation)
        public void clearCaches() {
            BenchSupport.clearCaches(tree, nodes);
        }
    }

    @State(Scope.Thread)
    public static class FlatFixedRoundedState {
        @Param({"flex", "block", "grid"})
        public String display;

        @Param({"1000", "10000", "100000"})
        public int nodeCount;

        TaffyTree tree;
        NodeId root;
        NodeId[] nodes;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.enableRounding();
            root = PhaseSupport.buildFlat(tree, display, nodeCount, PhaseSupport.FIXED);
            nodes = BenchSupport.collectNodes(tree, root);
            tree.computeLayout(root, PhaseSupport.layoutSpace());
        }

        @Setup(Level.Invocation)
        public void clearCaches() {
            BenchSupport.clearCaches(tree, nodes);
        }
    }

    @Benchmark
    public void flatFixed(FlatFixedState state, Blackhole bh) {
        state.tree.computeLayout(state.root, PhaseSupport.layoutSpace());
        bh.consume(state.tree.getLayout(state.root));
    }

    @Benchmark
    public void flatFixedRounded(FlatFixedRoundedState state, Blackhole bh) {
        state.tree.computeLayout(state.root, PhaseSupport.layoutSpace());
        bh.consume(state.tree.getLayout(state.root));
    }

    // === Flat shape, auto/percent/margin variants (rounding off) ===

    @State(Scope.Thread)
    public static class FlatSizingState {
        @Param({"flex", "block", "grid"})
        public String display;

        @Param({"1000", "10000", "100000"})
        public int nodeCount;

        @Param({"auto", "percent", "margin"})
        public String sizing;

        TaffyTree tree;
        NodeId root;
        NodeId[] nodes;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.disableRounding();
            root = PhaseSupport.buildFlat(tree, display, nodeCount, sizing);
            nodes = BenchSupport.collectNodes(tree, root);
            tree.computeLayout(root, PhaseSupport.layoutSpace());
        }

        @Setup(Level.Invocation)
        public void clearCaches() {
            BenchSupport.clearCaches(tree, nodes);
        }
    }

    @State(Scope.Thread)
    public static class FlatSizingRoundedState {
        @Param({"flex", "block", "grid"})
        public String display;

        @Param({"1000", "10000", "100000"})
        public int nodeCount;

        @Param({"auto", "percent", "margin"})
        public String sizing;

        TaffyTree tree;
        NodeId root;
        NodeId[] nodes;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.enableRounding();
            root = PhaseSupport.buildFlat(tree, display, nodeCount, sizing);
            nodes = BenchSupport.collectNodes(tree, root);
            tree.computeLayout(root, PhaseSupport.layoutSpace());
        }

        @Setup(Level.Invocation)
        public void clearCaches() {
            BenchSupport.clearCaches(tree, nodes);
        }
    }

    @Benchmark
    public void flatSizing(FlatSizingState state, Blackhole bh) {
        state.tree.computeLayout(state.root, PhaseSupport.layoutSpace());
        bh.consume(state.tree.getLayout(state.root));
    }

    @Benchmark
    public void flatSizingRounded(FlatSizingRoundedState state, Blackhole bh) {
        state.tree.computeLayout(state.root, PhaseSupport.layoutSpace());
        bh.consume(state.tree.getLayout(state.root));
    }

    // === Deep chain, fixed-size leaves ===

    @State(Scope.Thread)
    public static class DeepFixedState {
        @Param({"flex", "block", "grid"})
        public String display;

        @Param({"64", "256", "1024"})
        public int depth;

        TaffyTree tree;
        NodeId root;
        NodeId[] nodes;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.disableRounding();
            root = PhaseSupport.buildDeep(tree, display, depth, PhaseSupport.FIXED);
            nodes = BenchSupport.collectNodes(tree, root);
            tree.computeLayout(root, PhaseSupport.layoutSpace());
        }

        @Setup(Level.Invocation)
        public void clearCaches() {
            BenchSupport.clearCaches(tree, nodes);
        }
    }

    @State(Scope.Thread)
    public static class DeepFixedRoundedState {
        @Param({"flex", "block", "grid"})
        public String display;

        @Param({"64", "256", "1024"})
        public int depth;

        TaffyTree tree;
        NodeId root;
        NodeId[] nodes;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.enableRounding();
            root = PhaseSupport.buildDeep(tree, display, depth, PhaseSupport.FIXED);
            nodes = BenchSupport.collectNodes(tree, root);
            tree.computeLayout(root, PhaseSupport.layoutSpace());
        }

        @Setup(Level.Invocation)
        public void clearCaches() {
            BenchSupport.clearCaches(tree, nodes);
        }
    }

    @Benchmark
    public void deepFixed(DeepFixedState state, Blackhole bh) {
        state.tree.computeLayout(state.root, PhaseSupport.layoutSpace());
        bh.consume(state.tree.getLayout(state.root));
    }

    @Benchmark
    public void deepFixedRounded(DeepFixedRoundedState state, Blackhole bh) {
        state.tree.computeLayout(state.root, PhaseSupport.layoutSpace());
        bh.consume(state.tree.getLayout(state.root));
    }

    // === Deep chain, auto/percent/margin variants (rounding off) ===

    @State(Scope.Thread)
    public static class DeepSizingState {
        @Param({"flex", "block", "grid"})
        public String display;

        @Param({"64", "256", "1024"})
        public int depth;

        @Param({"auto", "percent", "margin"})
        public String sizing;

        TaffyTree tree;
        NodeId root;
        NodeId[] nodes;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.disableRounding();
            root = PhaseSupport.buildDeep(tree, display, depth, sizing);
            nodes = BenchSupport.collectNodes(tree, root);
            tree.computeLayout(root, PhaseSupport.layoutSpace());
        }

        @Setup(Level.Invocation)
        public void clearCaches() {
            BenchSupport.clearCaches(tree, nodes);
        }
    }

    @State(Scope.Thread)
    public static class DeepSizingRoundedState {
        @Param({"flex", "block", "grid"})
        public String display;

        @Param({"64", "256", "1024"})
        public int depth;

        @Param({"auto", "percent", "margin"})
        public String sizing;

        TaffyTree tree;
        NodeId root;
        NodeId[] nodes;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.enableRounding();
            root = PhaseSupport.buildDeep(tree, display, depth, sizing);
            nodes = BenchSupport.collectNodes(tree, root);
            tree.computeLayout(root, PhaseSupport.layoutSpace());
        }

        @Setup(Level.Invocation)
        public void clearCaches() {
            BenchSupport.clearCaches(tree, nodes);
        }
    }

    @Benchmark
    public void deepSizing(DeepSizingState state, Blackhole bh) {
        state.tree.computeLayout(state.root, PhaseSupport.layoutSpace());
        bh.consume(state.tree.getLayout(state.root));
    }

    @Benchmark
    public void deepSizingRounded(DeepSizingRoundedState state, Blackhole bh) {
        state.tree.computeLayout(state.root, PhaseSupport.layoutSpace());
        bh.consume(state.tree.getLayout(state.root));
    }
}
