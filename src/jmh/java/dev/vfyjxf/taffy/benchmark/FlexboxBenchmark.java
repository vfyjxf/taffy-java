package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.Rect;
import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.style.Dimension;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Style;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Flexbox layout benchmarks - mirrors Rust taffy benches/flexbox.rs
 *
 * Test groups (matching Rust):
 * - yoga 'huge nested': Deep hierarchy with fixed style
 * - Wide tree: 2-level hierarchy with random styles
 * - Deep tree (random size): Deep hierarchy with random dimensions
 * - Deep tree (auto size): Deep hierarchy with auto/flex-grow
 * - super deep: Very deep tree with 3 children per level
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class FlexboxBenchmark {

    private static final long SEED = 12345L;

    // ==================== yoga 'huge nested' ====================
    // Rust: builder.build_deep_hierarchy(*node_count, 10)
    // Style: size = length(10.0), flex_grow = 1.0

    @State(Scope.Thread)
    public static class HugeNestedState {
        TaffyTree tree;
        NodeId root;

        @Param({"1000", "10000", "100000"})
        int nodeCount;

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            Style style = new Style();
            style.size = new Size<>(Dimension.length(10f), Dimension.length(10f));
            style.flexGrow = 1.0f;

            root = buildDeepHierarchy(tree, nodeCount, 10, style);
        }
    }

    @Benchmark
    public void hugeNested(HugeNestedState state, Blackhole bh) {
        state.tree.computeLayout(state.root, Size.maxContent());
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== Wide tree ====================
    // Rust: builder.build_flat_hierarchy(*node_count)
    // Style: RandomStyleGenerator (random dimensions)

    @State(Scope.Thread)
    public static class WideTreeState {
        TaffyTree tree;
        NodeId root;

        @Param({"1000", "10000", "100000"})
        int nodeCount;

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            Random rng = new Random(SEED);
            root = buildFlatHierarchy(tree, nodeCount, rng);
        }
    }

    @Benchmark
    public void wideTree(WideTreeState state, Blackhole bh) {
        state.tree.computeLayout(state.root, Size.maxContent());
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== Deep tree (random size) ====================
    // Rust: builder.build_deep_hierarchy(*node_count, 2)
    // Style: RandomStyleGenerator

    @State(Scope.Thread)
    public static class DeepRandomState {
        TaffyTree tree;
        NodeId root;

        // Rust uses (4000, "12-level"), (10_000, "14-level")
        @Param({"4000", "10000", "100000"})
        int nodeCount;

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            Random rng = new Random(SEED);
            root = buildDeepHierarchyRandom(tree, nodeCount, 2, rng);
        }
    }

    @Benchmark
    public void deepTreeRandomSize(DeepRandomState state, Blackhole bh) {
        state.tree.computeLayout(state.root, Size.maxContent());
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== Deep tree (auto size) ====================
    // Rust: builder.build_deep_hierarchy(*node_count, 2)
    // Style: flex_grow = 1.0, margin = length(10.0)

    @State(Scope.Thread)
    public static class DeepAutoState {
        TaffyTree tree;
        NodeId root;

        @Param({"4000", "10000", "100000"})
        int nodeCount;

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            Style style = new Style();
            style.flexGrow = 1.0f;
            style.margin = Rect.all(LengthPercentageAuto.length(10f));

            root = buildDeepHierarchy(tree, nodeCount, 2, style);
        }
    }

    @Benchmark
    public void deepTreeAutoSize(DeepAutoState state, Blackhole bh) {
        state.tree.computeLayout(state.root, Size.maxContent());
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== super deep ====================
    // Rust: builder.build_super_deep_hierarchy(*depth, 3)
    // Style: flex_direction = Row, flex_grow = 1.0, margin = length(10.0)

    @State(Scope.Thread)
    public static class SuperDeepState {
        TaffyTree tree;
        NodeId root;

        // Rust uses [50, 100, 200] depth with 3 children per level
        @Param({"50", "100", "200"})
        int depth;

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            root = buildSuperDeepHierarchy(tree, depth, 3);
        }
    }

    @Benchmark
    public void superDeep(SuperDeepState state, Blackhole bh) {
        state.tree.computeLayout(state.root, Size.maxContent());
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== Helper Methods ====================

    /**
     * Builds a deep hierarchy with fixed style (matching Rust's build_deep_hierarchy)
     */
    private static NodeId buildDeepHierarchy(TaffyTree tree, int maxNodes, int branchFactor, Style style) {
        NodeId[] children = buildDeepTree(tree, maxNodes, branchFactor, style);

        Style rootStyle = new Style();
        return tree.newWithChildren(rootStyle, children);
    }

    private static NodeId[] buildDeepTree(TaffyTree tree, int maxNodes, int branchFactor, Style style) {
        if (maxNodes <= branchFactor) {
            NodeId[] leaves = new NodeId[maxNodes];
            for (int i = 0; i < maxNodes; i++) {
                leaves[i] = tree.newLeaf(style);
            }
            return leaves;
        }

        NodeId[] children = new NodeId[branchFactor];
        int nodesPerChild = (maxNodes - branchFactor) / branchFactor;
        for (int i = 0; i < branchFactor; i++) {
            NodeId[] subChildren = buildDeepTree(tree, nodesPerChild, branchFactor, style);
            children[i] = tree.newWithChildren(style, subChildren);
        }
        return children;
    }

    /**
     * Builds a deep hierarchy with random styles
     */
    private static NodeId buildDeepHierarchyRandom(TaffyTree tree, int maxNodes, int branchFactor, Random rng) {
        NodeId[] children = buildDeepTreeRandom(tree, maxNodes, branchFactor, rng);

        Style rootStyle = new Style();
        return tree.newWithChildren(rootStyle, children);
    }

    private static NodeId[] buildDeepTreeRandom(TaffyTree tree, int maxNodes, int branchFactor, Random rng) {
        if (maxNodes <= branchFactor) {
            NodeId[] leaves = new NodeId[maxNodes];
            for (int i = 0; i < maxNodes; i++) {
                leaves[i] = tree.newLeaf(randomStyle(rng));
            }
            return leaves;
        }

        NodeId[] children = new NodeId[branchFactor];
        int nodesPerChild = (maxNodes - branchFactor) / branchFactor;
        for (int i = 0; i < branchFactor; i++) {
            NodeId[] subChildren = buildDeepTreeRandom(tree, nodesPerChild, branchFactor, rng);
            children[i] = tree.newWithChildren(randomStyle(rng), subChildren);
        }
        return children;
    }

    /**
     * Builds a flat hierarchy (2-level) with random styles (matching Rust's build_flat_hierarchy)
     */
    private static NodeId buildFlatHierarchy(TaffyTree tree, int targetNodeCount, Random rng) {
        int created = 0;
        java.util.List<NodeId> children = new java.util.ArrayList<>();

        while (created < targetNodeCount) {
            int count = 1 + rng.nextInt(4); // 1-4 children per container
            NodeId[] subChildren = new NodeId[count];
            for (int i = 0; i < count; i++) {
                subChildren[i] = tree.newLeaf(randomStyle(rng));
            }
            children.add(tree.newWithChildren(randomStyle(rng), subChildren));
            created += count + 1;
        }

        Style rootStyle = new Style();
        return tree.newWithChildren(rootStyle, children.toArray(new NodeId[0]));
    }

    /**
     * Builds a super deep hierarchy (matching Rust's build_super_deep_hierarchy)
     * Creates a chain where each level has `nodesPerLevel` children,
     * with one child being a container that continues the chain.
     */
    private static NodeId buildSuperDeepHierarchy(TaffyTree tree, int depth, int nodesPerLevel) {
        Style style = new Style();
        style.flexDirection = FlexDirection.ROW;
        style.flexGrow = 1.0f;
        style.margin = Rect.all(LengthPercentageAuto.length(10f));

        NodeId[] children = new NodeId[0];
        for (int d = 0; d < depth; d++) {
            NodeId nodeWithChildren = tree.newWithChildren(style, children);

            children = new NodeId[nodesPerLevel];
            children[0] = nodeWithChildren;
            for (int i = 1; i < nodesPerLevel; i++) {
                children[i] = tree.newLeaf(style);
            }
        }

        Style rootStyle = new Style();
        return tree.newWithChildren(rootStyle, children);
    }

    /**
     * Creates a random style (matching Rust's RandomStyleGenerator)
     */
    private static Style randomStyle(Random rng) {
        Style style = new Style();
        style.size = new Size<>(randomDimension(rng), randomDimension(rng));
        return style;
    }

    /**
     * Creates a random dimension (matching Rust's random_dimension)
     */
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
}
