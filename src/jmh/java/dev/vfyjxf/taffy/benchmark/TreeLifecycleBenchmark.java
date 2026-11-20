package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.style.TaffyStyle;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tree lifecycle throughput: {@code newLeaf}, {@code newWithChildren}, subtree removal and
 * {@code clear}.
 *
 * <p>Upstream counterpart: {@code benches/tree_creation.rs}. Creation benchmarks build the whole
 * flat hierarchy inside the measured method; destruction benchmarks build the hierarchy in
 * {@code Level.Invocation} setup (outside the measurement) and then only time the removal or
 * {@code clear} call.</p>
 *
 * <p>The default suite uses 1,000 and 10,000 nodes. The 100,000-node variants live in
 * {@link TreeLifecycleExtendedBenchmark}, which is excluded from the default {@code jmh} task.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
@Fork(1)
public class TreeLifecycleBenchmark {

    private static final TaffyStyle LEAF_STYLE = new TaffyStyle();

    // === newLeaf throughput ===

    @State(Scope.Thread)
    public static class NewLeafState {
        @Param({"1000", "10000"})
        public int nodeCount;
    }

    @Benchmark
    public void newLeaf(NewLeafState state, Blackhole bh) {
        TaffyTree tree = new TaffyTree();
        for (int i = 0; i < state.nodeCount; i++) {
            tree.newLeaf(LEAF_STYLE);
        }
        bh.consume(tree);
    }

    // === newWithChildren throughput ===

    @State(Scope.Thread)
    public static class NewWithChildrenState {
        /** Each container adds itself plus four leaves, so node count is {@code 5 * containers}. */
        @Param({"200", "2000"})
        public int containers;
    }

    @Benchmark
    public void newWithChildren(NewWithChildrenState state, Blackhole bh) {
        TaffyTree tree = new TaffyTree();
        List<NodeId> roots = new ArrayList<>(state.containers);
        for (int i = 0; i < state.containers; i++) {
            NodeId[] leaves = new NodeId[4];
            for (int j = 0; j < leaves.length; j++) {
                leaves[j] = tree.newLeaf(LEAF_STYLE);
            }
            roots.add(tree.newWithChildren(LEAF_STYLE, leaves));
        }
        bh.consume(tree.newWithChildren(LEAF_STYLE, roots));
        bh.consume(tree);
    }

    // === Subtree removal throughput ===

    @State(Scope.Thread)
    public static class RemoveState {
        @Param({"200", "2000"})
        public int containers;

        TaffyTree tree;
        NodeId[] roots;

        @Setup(Level.Invocation)
        public void setupInvocation() {
            tree = new TaffyTree();
            roots = new NodeId[containers];
            for (int i = 0; i < containers; i++) {
                NodeId[] leaves = new NodeId[4];
                for (int j = 0; j < leaves.length; j++) {
                    leaves[j] = tree.newLeaf(LEAF_STYLE);
                }
                roots[i] = tree.newWithChildren(LEAF_STYLE, leaves);
            }
            NodeId root = tree.newWithChildren(LEAF_STYLE, roots);
            // Keep the root alive but let the benchmark remove the subtrees below it.
            this.root = root;
        }

        NodeId root;
    }

    @Benchmark
    public void removeSubtrees(RemoveState state, Blackhole bh) {
        for (NodeId child : state.roots) {
            state.tree.remove(child);
        }
        bh.consume(state.tree.totalNodeCount());
    }

    // === clear throughput ===

    @State(Scope.Thread)
    public static class ClearState {
        @Param({"200", "2000"})
        public int containers;

        TaffyTree tree;

        @Setup(Level.Invocation)
        public void setupInvocation() {
            tree = new TaffyTree();
            NodeId[] roots = new NodeId[containers];
            for (int i = 0; i < containers; i++) {
                NodeId[] leaves = new NodeId[4];
                for (int j = 0; j < leaves.length; j++) {
                    leaves[j] = tree.newLeaf(LEAF_STYLE);
                }
                roots[i] = tree.newWithChildren(LEAF_STYLE, leaves);
            }
            tree.newWithChildren(LEAF_STYLE, roots);
        }
    }

    @Benchmark
    public void clear(TreeLifecycleBenchmark.ClearState state, Blackhole bh) {
        state.tree.clear();
        bh.consume(state.tree.totalNodeCount());
    }
}
