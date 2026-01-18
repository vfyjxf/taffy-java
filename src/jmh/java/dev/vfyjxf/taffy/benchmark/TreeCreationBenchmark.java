package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.style.Style;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Tree creation benchmarks - mirrors Rust taffy benches/tree_creation.rs
 *
 * Benchmark groups:
 * - Tree creation: TaffyTree::new vs TaffyTree::with_capacity
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class TreeCreationBenchmark {

    private static final long SEED = 12345L;

    // ==================== Tree creation ====================
    // Measures tree creation time without layout computation

    @State(Scope.Thread)
    public static class TreeCreationState {
        @Param({"1000", "10000", "100000"})
        int nodeCount;
    }

    @Benchmark
    public void treeCreationNew(TreeCreationState state, Blackhole bh) {
        Object[] result = buildFlatHierarchy(state.nodeCount, false);
        bh.consume(result[0]);
        bh.consume(result[1]);
    }

    @Benchmark
    public void treeCreationWithCapacity(TreeCreationState state, Blackhole bh) {
        Object[] result = buildFlatHierarchy(state.nodeCount, true);
        bh.consume(result[0]);
        bh.consume(result[1]);
    }

    // ==================== Helper Methods ====================

    /**
     * Build a random leaf node
     */
    private static NodeId buildRandomLeaf(TaffyTree taffy) {
        return taffy.newLeaf(new Style());
    }

    /**
     * A tree with many children that have shallow depth (matching Rust's build_taffy_flat_hierarchy)
     */
    private static Object[] buildFlatHierarchy(int totalNodeCount, boolean useWithCapacity) {
        TaffyTree taffy = useWithCapacity ? new TaffyTree(totalNodeCount) : new TaffyTree();
        Random rng = new Random(SEED);
        List<NodeId> children = new ArrayList<>();
        int nodeCount = 0;

        while (nodeCount < totalNodeCount) {
            int subChildrenCount = 1 + rng.nextInt(4); // 1..=4
            NodeId[] subChildren = new NodeId[subChildrenCount];
            for (int i = 0; i < subChildrenCount; i++) {
                subChildren[i] = buildRandomLeaf(taffy);
            }
            NodeId node = taffy.newWithChildren(new Style(), subChildren);
            children.add(node);
            nodeCount += 1 + subChildrenCount;
        }

        NodeId root = taffy.newWithChildren(new Style(), children.toArray(new NodeId[0]));
        return new Object[]{taffy, root};
    }
}
