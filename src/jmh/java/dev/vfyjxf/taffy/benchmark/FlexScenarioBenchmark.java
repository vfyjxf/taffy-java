package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
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
 * Flexbox scenario coverage: direction, wrapping, grow, shrink, baselines and deep nesting.
 *
 * <p>Every benchmark lays out a tree that was built once in {@code Level.Trial} setup. The
 * {@code Level.Invocation} setup clears every node's cache, which is the Java equivalent of
 * upstream taffy's {@code mark_all_dirty}: the tree is reused, but each measured call is a cold
 * layout. Cache hits are covered separately by {@code IncrementalLayoutBenchmark}.</p>
 *
 * <p>Upstream counterparts: {@code benches/flexbox.rs} (deep/wide trees) and the PanGUI
 * {@code wide_no_wrap}, {@code wide_wrapping}, {@code nested_vertical_stack} workloads.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
@Fork(1)
public class FlexScenarioBenchmark {

    // === Row / column, no wrap ===

    @State(Scope.Thread)
    public static class FlatState {
        @Param({"100", "1000", "10000"})
        public int nodeCount;

        @Param({"row", "column"})
        public String axis;

        TaffyTree tree;
        NodeId root;
        NodeId[] nodes;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            FlexDirection direction = "row".equals(axis) ? FlexDirection.ROW : FlexDirection.COLUMN;
            TaffyStyle rootStyle = BenchSupport.sizedFlex(direction, 1000f, 1000f);

            TaffyStyle itemStyle = new TaffyStyle();
            itemStyle.size = new TaffySize<>(TaffyDimension.length(10f), TaffyDimension.length(10f));
            itemStyle.flexGrow = 1f;
            itemStyle.flexShrink = 1f;
            itemStyle.flexBasis = TaffyDimension.ZERO;

            NodeId[] children = new NodeId[nodeCount];
            for (int i = 0; i < nodeCount; i++) {
                children[i] = tree.newLeaf(itemStyle);
            }
            root = tree.newWithChildren(rootStyle, children);
            nodes = BenchSupport.collectNodes(tree, root);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            BenchSupport.clearCaches(tree, nodes);
        }
    }

    @Benchmark
    public void flatRowOrColumn(FlatState state, Blackhole bh) {
        state.tree.computeLayout(state.root, TaffySize.of(
            AvailableSpace.definite(1000f),
            AvailableSpace.definite(1000f)
        ));
        bh.consume(state.tree.getLayout(state.root));
    }

    // === Wrapping row ===

    @State(Scope.Thread)
    public static class WrapState {
        @Param({"100", "1000", "10000"})
        public int nodeCount;

        TaffyTree tree;
        NodeId root;
        NodeId[] nodes;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.ROW, 1000f, Float.NaN);
            rootStyle.flexWrap = FlexWrap.WRAP;

            TaffyStyle itemStyle = new TaffyStyle();
            itemStyle.size = new TaffySize<>(TaffyDimension.length(50f), TaffyDimension.length(20f));

            NodeId[] children = new NodeId[nodeCount];
            for (int i = 0; i < nodeCount; i++) {
                children[i] = tree.newLeaf(itemStyle);
            }
            root = tree.newWithChildren(rootStyle, children);
            nodes = BenchSupport.collectNodes(tree, root);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            BenchSupport.clearCaches(tree, nodes);
        }
    }

    @Benchmark
    public void wrappingRow(WrapState state, Blackhole bh) {
        state.tree.computeLayout(state.root, TaffySize.of(
            AvailableSpace.definite(1000f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.tree.getLayout(state.root));
    }

    // === Grow ===

    @State(Scope.Thread)
    public static class GrowState {
        @Param({"100", "1000", "10000"})
        public int nodeCount;

        TaffyTree tree;
        NodeId root;
        NodeId[] nodes;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.ROW, 4000f, 100f);

            NodeId[] children = new NodeId[nodeCount];
            for (int i = 0; i < nodeCount; i++) {
                TaffyStyle itemStyle = new TaffyStyle();
                itemStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(20f));
                itemStyle.flexGrow = 1f + (i % 5);
                itemStyle.flexShrink = 0f;
                itemStyle.flexBasis = TaffyDimension.ZERO;
                children[i] = tree.newLeaf(itemStyle);
            }
            root = tree.newWithChildren(rootStyle, children);
            nodes = BenchSupport.collectNodes(tree, root);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            BenchSupport.clearCaches(tree, nodes);
        }
    }

    @Benchmark
    public void growItems(GrowState state, Blackhole bh) {
        state.tree.computeLayout(state.root, TaffySize.of(
            AvailableSpace.definite(4000f),
            AvailableSpace.definite(100f)
        ));
        bh.consume(state.tree.getLayout(state.root));
    }

    // === Shrink ===

    @State(Scope.Thread)
    public static class ShrinkState {
        @Param({"100", "1000", "10000"})
        public int nodeCount;

        TaffyTree tree;
        NodeId root;
        NodeId[] nodes;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.ROW, 1000f, 100f);

            TaffyStyle itemStyle = new TaffyStyle();
            itemStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(20f));
            itemStyle.flexShrink = 1f;
            itemStyle.flexBasis = TaffyDimension.AUTO;

            NodeId[] children = new NodeId[nodeCount];
            for (int i = 0; i < nodeCount; i++) {
                children[i] = tree.newLeaf(itemStyle);
            }
            root = tree.newWithChildren(rootStyle, children);
            nodes = BenchSupport.collectNodes(tree, root);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            BenchSupport.clearCaches(tree, nodes);
        }
    }

    @Benchmark
    public void shrinkItems(ShrinkState state, Blackhole bh) {
        state.tree.computeLayout(state.root, TaffySize.of(
            AvailableSpace.definite(1000f),
            AvailableSpace.definite(100f)
        ));
        bh.consume(state.tree.getLayout(state.root));
    }

    // === Baseline alignment ===

    @State(Scope.Thread)
    public static class BaselineState {
        @Param({"100", "1000"})
        public int nodeCount;

        TaffyTree tree;
        NodeId root;
        NodeId[] nodes;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.ROW, 1000f, Float.NaN);
            rootStyle.alignItems = AlignItems.BASELINE;

            NodeId[] children = new NodeId[nodeCount];
            for (int i = 0; i < nodeCount; i++) {
                TaffyStyle columnStyle = new TaffyStyle();
                columnStyle.display = TaffyDisplay.FLEX;
                columnStyle.flexDirection = FlexDirection.COLUMN;

                TaffyStyle titleStyle = new TaffyStyle();
                titleStyle.size = new TaffySize<>(TaffyDimension.length(20f), TaffyDimension.length(10f + (i % 40)));
                TaffyStyle bodyStyle = new TaffyStyle();
                bodyStyle.size = new TaffySize<>(TaffyDimension.length(20f), TaffyDimension.length(5f + (i % 10)));

                NodeId title = tree.newLeaf(titleStyle);
                NodeId body = tree.newLeaf(bodyStyle);
                children[i] = tree.newWithChildren(columnStyle, new NodeId[]{title, body});
            }
            root = tree.newWithChildren(rootStyle, children);
            nodes = BenchSupport.collectNodes(tree, root);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            BenchSupport.clearCaches(tree, nodes);
        }
    }

    @Benchmark
    public void baselineAlignedRow(BaselineState state, Blackhole bh) {
        state.tree.computeLayout(state.root, TaffySize.of(
            AvailableSpace.definite(1000f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.tree.getLayout(state.root));
    }

    // === Deep nesting ===

    @State(Scope.Thread)
    public static class DeepState {
        @Param({"100", "1000"})
        public int depth;

        TaffyTree tree;
        NodeId root;
        NodeId[] nodes;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            TaffyStyle leafStyle = new TaffyStyle();
            leafStyle.size = new TaffySize<>(TaffyDimension.length(10f), TaffyDimension.length(10f));

            NodeId current = tree.newLeaf(leafStyle);
            for (int i = 0; i < depth; i++) {
                TaffyStyle parentStyle = new TaffyStyle();
                parentStyle.display = TaffyDisplay.FLEX;
                parentStyle.flexDirection = FlexDirection.COLUMN;
                parentStyle.padding = dev.vfyjxf.taffy.geometry.TaffyRect.all(
                    dev.vfyjxf.taffy.style.LengthPercentage.length(1f));
                current = tree.newWithChildren(parentStyle, new NodeId[]{current});
            }
            root = current;
            nodes = BenchSupport.collectNodes(tree, root);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            BenchSupport.clearCaches(tree, nodes);
        }
    }

    @Benchmark
    public void deepNestedColumn(DeepState state, Blackhole bh) {
        state.tree.computeLayout(state.root, TaffySize.maxContent());
        bh.consume(state.tree.getLayout(state.root));
    }
}
