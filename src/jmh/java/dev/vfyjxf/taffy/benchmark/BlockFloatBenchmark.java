package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.Clear;
import dev.vfyjxf.taffy.style.Contain;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyFloat;
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

import java.util.concurrent.TimeUnit;

/**
 * Block layout, CSS floats and block formatting context coverage.
 *
 * <p>Scenarios: deep block nesting, margin collapsing chains, left/right floats with in-flow
 * avoidance, {@code clear}, scroll containers, {@code display: flow-root} and
 * {@code contain: layout} BFC roots. This complements the flex/grid suites with the third layout
 * algorithm and exercises the float placement / BFC slot code paths added for upstream
 * alignment.</p>
 *
 * <p>Cold-layout benchmarks clear all caches in invocation setup; the tree itself is built once
 * per trial.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
@Fork(1)
public class BlockFloatBenchmark {

    // === Deep block nesting ===

    @State(Scope.Thread)
    public static class DeepBlockState {
        @Param({"100", "1000"})
        public int depth;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            TaffyTree tree = new TaffyTree();
            NodeId current = tree.newLeaf(BenchSupport.block(10f, 10f));
            for (int i = 0; i < depth; i++) {
                TaffyStyle parentStyle = BenchSupport.block(Float.NaN, Float.NaN);
                BenchSupport.padding(parentStyle, 1f);
                current = tree.newWithChildren(parentStyle, new NodeId[]{current});
            }
            treeCase = new BenchSupport.TreeCase(tree, current);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void deepBlockNesting(DeepBlockState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.maxContent());
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === Margin collapsing chain ===

    @State(Scope.Thread)
    public static class MarginCollapseState {
        @Param({"100", "1000"})
        public int depth;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            TaffyTree tree = new TaffyTree();
            NodeId current = tree.newLeaf(BenchSupport.block(Float.NaN, 10f));
            for (int i = 0; i < depth; i++) {
                TaffyStyle parentStyle = BenchSupport.block(Float.NaN, Float.NaN);
                parentStyle.margin = dev.vfyjxf.taffy.geometry.TaffyRect.all(
                    dev.vfyjxf.taffy.style.LengthPercentageAuto.length(10f));
                current = tree.newWithChildren(parentStyle, new NodeId[]{current});
            }
            TaffyStyle rootStyle = BenchSupport.block(500f, Float.NaN);
            treeCase = new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, new NodeId[]{current}));
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void marginCollapsing(MarginCollapseState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(500f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === Left / right floats with in-flow avoidance ===

    @State(Scope.Thread)
    public static class FloatState {
        @Param({"LEFT", "RIGHT"})
        public String side;

        @Param({"100", "1000"})
        public int itemCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = BenchSupport.block(800f, Float.NaN);
            TaffyFloat floatSide = TaffyFloat.valueOf(side);

            NodeId[] children = new NodeId[itemCount * 2];
            TaffyStyle floatStyle = BenchSupport.floated(floatSide, 40f, 20f);
            TaffyStyle flowStyle = BenchSupport.block(Float.NaN, 15f);
            int index = 0;
            for (int i = 0; i < itemCount; i++) {
                children[index++] = tree.newLeaf(floatStyle);
                children[index++] = tree.newLeaf(flowStyle);
            }
            treeCase = new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, children));
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void floatsWithInFlowBlocks(FloatState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(800f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === clear ===

    @State(Scope.Thread)
    public static class ClearState {
        @Param({"100", "1000"})
        public int itemCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = BenchSupport.block(800f, Float.NaN);

            NodeId[] children = new NodeId[itemCount * 2];
            TaffyStyle leftFloat = BenchSupport.floated(TaffyFloat.LEFT, 40f, 20f);
            TaffyStyle rightFloat = BenchSupport.floated(TaffyFloat.RIGHT, 40f, 20f);
            TaffyStyle clearedStyle = BenchSupport.cleared(Clear.BOTH, Float.NaN, 15f);

            int index = 0;
            for (int i = 0; i < itemCount; i++) {
                children[index++] = tree.newLeaf((i & 1) == 0 ? leftFloat : rightFloat);
                children[index++] = tree.newLeaf(clearedStyle);
            }
            treeCase = new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, children));
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void clearedBlocks(ClearState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(800f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === Scroll containers ===

    @State(Scope.Thread)
    public static class OverflowState {
        @Param({"100", "1000"})
        public int itemCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = BenchSupport.block(300f, 200f);
            BenchSupport.overflow(rootStyle, Overflow.SCROLL, Overflow.SCROLL);
            rootStyle.scrollbarWidth = 10f;

            TaffyStyle childStyle = BenchSupport.block(100f, 40f);
            NodeId[] children = new NodeId[itemCount];
            for (int i = 0; i < itemCount; i++) {
                children[i] = tree.newLeaf(childStyle);
            }
            treeCase = new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, children));
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void overflowScroll(OverflowState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(300f),
            AvailableSpace.definite(200f)
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === flow-root BFC chain ===

    @State(Scope.Thread)
    public static class FlowRootState {
        @Param({"100", "1000"})
        public int depth;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            TaffyTree tree = new TaffyTree();
            NodeId current = tree.newLeaf(BenchSupport.block(10f, 10f));
            TaffyStyle floatStyle = BenchSupport.floated(TaffyFloat.LEFT, 30f, 20f);
            for (int i = 0; i < depth; i++) {
                TaffyStyle parentStyle = BenchSupport.flowRoot(Float.NaN, Float.NaN);
                BenchSupport.padding(parentStyle, 1f);
                current = tree.newWithChildren(parentStyle, new NodeId[]{tree.newLeaf(floatStyle), current});
            }
            TaffyStyle rootStyle = BenchSupport.flowRoot(500f, Float.NaN);
            treeCase = new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, new NodeId[]{current}));
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void flowRootContainment(FlowRootState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(500f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === contain: layout BFC chain ===

    @State(Scope.Thread)
    public static class ContainState {
        @Param({"100", "1000"})
        public int depth;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            TaffyTree tree = new TaffyTree();
            NodeId current = tree.newLeaf(BenchSupport.block(10f, 10f));
            TaffyStyle floatStyle = BenchSupport.floated(TaffyFloat.LEFT, 30f, 20f);
            for (int i = 0; i < depth; i++) {
                TaffyStyle parentStyle = BenchSupport.block(Float.NaN, Float.NaN);
                parentStyle.contain = Contain.LAYOUT;
                BenchSupport.padding(parentStyle, 1f);
                current = tree.newWithChildren(parentStyle, new NodeId[]{tree.newLeaf(floatStyle), current});
            }
            TaffyStyle rootStyle = BenchSupport.block(500f, Float.NaN);
            rootStyle.contain = Contain.LAYOUT;
            treeCase = new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, new NodeId[]{current}));
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void containLayoutBfc(ContainState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(500f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }
}
