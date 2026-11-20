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
 * 100,000-node tree lifecycle variants. Excluded from the default {@code jmh} task by the
 * {@code .*ExtendedBenchmark.*} exclude pattern; run with {@code ./gradlew jmhExtended}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
@Fork(1)
public class TreeLifecycleExtendedBenchmark {

    private static final TaffyStyle LEAF_STYLE = new TaffyStyle();

    @State(Scope.Thread)
    public static class NewLeafState {
        @Param({"100000"})
        public int nodeCount;
    }

    @Benchmark
    public void newLeaf100k(NewLeafState state, Blackhole bh) {
        TaffyTree tree = new TaffyTree();
        for (int i = 0; i < state.nodeCount; i++) {
            tree.newLeaf(LEAF_STYLE);
        }
        bh.consume(tree);
    }

    @State(Scope.Thread)
    public static class NewWithChildrenState {
        @Param({"20000"})
        public int containers;
    }

    @Benchmark
    public void newWithChildren100k(NewWithChildrenState state, Blackhole bh) {
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

    @State(Scope.Thread)
    public static class RemoveState {
        @Param({"20000"})
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
            tree.newWithChildren(LEAF_STYLE, roots);
        }
    }

    @Benchmark
    public void removeSubtrees100k(RemoveState state, Blackhole bh) {
        for (NodeId child : state.roots) {
            state.tree.remove(child);
        }
        bh.consume(state.tree.totalNodeCount());
    }

    @State(Scope.Thread)
    public static class ClearState {
        @Param({"20000"})
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
    public void clear100k(ClearState state, Blackhole bh) {
        state.tree.clear();
        bh.consume(state.tree.totalNodeCount());
    }
}
