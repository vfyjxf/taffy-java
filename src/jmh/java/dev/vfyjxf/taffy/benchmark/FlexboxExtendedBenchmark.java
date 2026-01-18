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
 * Extended flexbox benchmarks to increase coverage (larger inputs).
 * Excluded from default `jmh`; run via Gradle task `jmhExtended`.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class FlexboxExtendedBenchmark {

    private static final long SEED = 12345L;

    // ==================== Wide tree (100_000) ====================

    @State(Scope.Thread)
    public static class WideTreeLargeState {
        TaffyTree tree;
        NodeId root;

        @Param({"100000"})
        int nodeCount;

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            Random rng = new Random(SEED);
            root = buildFlatHierarchy(tree, nodeCount, rng);
        }
    }

    @Benchmark
    public void wideTreeLarge(WideTreeLargeState state, Blackhole bh) {
        state.tree.computeLayout(state.root, Size.maxContent());
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== Deep tree (random size, 100_000) ====================

    @State(Scope.Thread)
    public static class DeepRandomLargeState {
        TaffyTree tree;
        NodeId root;

        @Param({"100000"})
        int nodeCount;

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            Random rng = new Random(SEED);
            root = buildDeepHierarchyRandom(tree, nodeCount, 2, rng);
        }
    }

    @Benchmark
    public void deepTreeRandomSizeLarge(DeepRandomLargeState state, Blackhole bh) {
        state.tree.computeLayout(state.root, Size.maxContent());
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== Super deep (depth=1000) ====================

    @State(Scope.Thread)
    public static class SuperDeep1000State {
        TaffyTree tree;
        NodeId root;

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            root = buildSuperDeepHierarchy(tree, 1000, 3);
        }
    }

    @Benchmark
    public void superDeep1000(SuperDeep1000State state, Blackhole bh) {
        state.tree.computeLayout(state.root, Size.maxContent());
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== Helper Methods (copied/minimized from FlexboxBenchmark) ====================

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

    private static NodeId buildFlatHierarchy(TaffyTree tree, int targetNodeCount, Random rng) {
        int created = 0;
        java.util.List<NodeId> children = new java.util.ArrayList<>();

        while (created < targetNodeCount) {
            int count = 1 + rng.nextInt(4);
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

    private static Style randomStyle(Random rng) {
        Style style = new Style();
        style.size = new Size<>(randomDimension(rng), randomDimension(rng));
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
}
