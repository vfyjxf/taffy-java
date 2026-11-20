package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
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
 * Complex direction, out-of-flow positioning and grid+flex mixed coverage.
 *
 * <p>Extends {@code DirectionRtlBenchmark} with alternating RTL/LTR subtrees, absolute positioned
 * children with every inset combination, and trees that nest all three layout algorithms.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
@Fork(1)
public class AdvancedLayoutBenchmark {

    // === Alternating RTL / LTR nesting ===

    @State(Scope.Thread)
    public static class RtlNestedState {
        @Param({"100", "500"})
        public int depth;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            TaffyTree tree = new TaffyTree();
            NodeId root = buildRtlChain(tree, depth);
            TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.ROW, 1000f, Float.NaN);
            rootStyle.direction = TaffyDirection.RTL;
            treeCase = new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, new NodeId[]{root}));
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void alternatingRtlNesting(RtlNestedState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(1000f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === RTL grid with per-item direction ===

    @State(Scope.Thread)
    public static class RtlGridState {
        @Param({"1000"})
        public int itemCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = new TaffyStyle();
            rootStyle.display = TaffyDisplay.GRID;
            rootStyle.direction = TaffyDirection.RTL;
            rootStyle.size = new TaffySize<>(TaffyDimension.length(1000f), TaffyDimension.length(1000f));
            rootStyle.gridTemplateColumns = BenchSupport.frTracks(10);
            rootStyle.gridAutoRows = BenchSupport.fixedTracks(1, 20f);

            NodeId[] children = new NodeId[itemCount];
            for (int i = 0; i < itemCount; i++) {
                TaffyStyle itemStyle = BenchSupport.sized(10f, 10f);
                itemStyle.direction = (i & 1) == 0 ? TaffyDirection.RTL : TaffyDirection.LTR;
                children[i] = tree.newLeaf(itemStyle);
            }
            treeCase = new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, children));
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void rtlGridWithMixedItemDirections(RtlGridState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(1000f),
            AvailableSpace.definite(1000f)
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === Absolute positioning with varied insets ===

    @State(Scope.Thread)
    public static class AbsoluteState {
        @Param({"100", "1000", "10000"})
        public int childCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = BenchSupport.block(1000f, 1000f);
            rootStyle.position = TaffyPosition.RELATIVE;

            TaffyStyle flowStyle = BenchSupport.block(10f, 10f);
            NodeId[] children = new NodeId[childCount];
            for (int i = 0; i < childCount; i++) {
                if ((i & 1) == 0) {
                    children[i] = tree.newLeaf(absoluteStyle(i));
                } else {
                    children[i] = tree.newLeaf(flowStyle);
                }
            }
            treeCase = new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, children));
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void absolutePositioning(AbsoluteState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(1000f),
            AvailableSpace.definite(1000f)
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === Grid of flex cells with a nested grid ===

    @State(Scope.Thread)
    public static class GridFlexMixedState {
        @Param({"100", "1000"})
        public int cellCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = new TaffyStyle();
            rootStyle.display = TaffyDisplay.GRID;
            rootStyle.size = new TaffySize<>(TaffyDimension.length(1600f), TaffyDimension.AUTO);
            rootStyle.gridTemplateColumns = BenchSupport.frTracks(5);
            rootStyle.gridAutoRows = List.of(TrackSizingFunction.minmax(
                TrackSizingFunction.fixed(100f),
                TrackSizingFunction.auto()
            ));
            BenchSupport.gap(rootStyle, 10f);
            BenchSupport.padding(rootStyle, 10f);

            NodeId[] cells = new NodeId[cellCount];
            for (int i = 0; i < cellCount; i++) {
                cells[i] = buildFlexCell(tree, i);
            }
            treeCase = new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, cells));
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void gridFlexMixed(GridFlexMixedState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(1600f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === Fully mixed nested containers ===

    @State(Scope.Thread)
    public static class MixedNestedState {
        @Param({"3", "5"})
        public int depth;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            TaffyTree tree = new TaffyTree();
            NodeId root = buildMixedTree(tree, depth, 0);
            treeCase = new BenchSupport.TreeCase(tree, root);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void mixedNestedContainers(MixedNestedState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(2000f),
            AvailableSpace.definite(2000f)
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // ==================== Builders ====================

    private static NodeId buildRtlChain(TaffyTree tree, int depth) {
        if (depth == 0) {
            return tree.newLeaf(BenchSupport.sized(20f, 20f));
        }
        TaffyStyle containerStyle = BenchSupport.sizedFlex(
            (depth & 1) == 0 ? FlexDirection.ROW : FlexDirection.COLUMN,
            Float.NaN,
            Float.NaN
        );
        containerStyle.direction = (depth & 1) == 0 ? TaffyDirection.RTL : TaffyDirection.LTR;
        BenchSupport.padding(containerStyle, 1f);
        NodeId leaf = tree.newLeaf(BenchSupport.sized(20f, 20f));
        NodeId nested = buildRtlChain(tree, depth - 1);
        return tree.newWithChildren(containerStyle, new NodeId[]{leaf, nested});
    }

    private static TaffyStyle absoluteStyle(int index) {
        TaffyStyle style = new TaffyStyle();
        style.position = TaffyPosition.ABSOLUTE;
        style.size = new TaffySize<>(TaffyDimension.length(20f), TaffyDimension.length(20f));
        style.inset = switch (index & 3) {
            case 0 -> TaffyRect.of(
                LengthPercentageAuto.length(10f), LengthPercentageAuto.AUTO,
                LengthPercentageAuto.length(10f), LengthPercentageAuto.AUTO);
            case 1 -> TaffyRect.of(
                LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.1f),
                LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.1f));
            case 2 -> TaffyRect.of(
                LengthPercentageAuto.percent(0.25f), LengthPercentageAuto.percent(0.25f),
                LengthPercentageAuto.length(30f), LengthPercentageAuto.length(30f));
            default -> TaffyRect.all(LengthPercentageAuto.AUTO);
        };
        return style;
    }

    private static NodeId buildFlexCell(TaffyTree tree, int index) {
        TaffyStyle cellStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, Float.NaN, Float.NaN);
        BenchSupport.padding(cellStyle, 4f);
        BenchSupport.gap(cellStyle, 2f);

        NodeId title = tree.newLeaf(BenchSupport.sized(20f, 12f));
        NodeId first = tree.newLeaf(BenchSupport.sized(20f, 10f));
        NodeId second = tree.newLeaf(BenchSupport.sized(20f, 10f));

        TaffyStyle nestedStyle = new TaffyStyle();
        nestedStyle.display = TaffyDisplay.GRID;
        nestedStyle.gridTemplateColumns = BenchSupport.frTracks(2);
        nestedStyle.gridAutoRows = BenchSupport.fixedTracks(1, 10f);
        NodeId[] nestedChildren = new NodeId[4];
        for (int i = 0; i < nestedChildren.length; i++) {
            nestedChildren[i] = tree.newLeaf(BenchSupport.sized(5f, 5f));
        }
        NodeId nested = tree.newWithChildren(nestedStyle, nestedChildren);
        return tree.newWithChildren(cellStyle, new NodeId[]{title, first, second, nested});
    }

    private static NodeId buildMixedTree(TaffyTree tree, int depth, int level) {
        if (depth == 0) {
            return tree.newLeaf(BenchSupport.sized(10f, 10f));
        }
        TaffyDisplay display = switch (level % 3) {
            case 0 -> TaffyDisplay.FLEX;
            case 1 -> TaffyDisplay.GRID;
            default -> TaffyDisplay.BLOCK;
        };
        TaffyStyle containerStyle = new TaffyStyle();
        containerStyle.display = display;
        if (display == TaffyDisplay.FLEX) {
            containerStyle.flexDirection = (level & 1) == 0 ? FlexDirection.ROW : FlexDirection.COLUMN;
        }
        if (display == TaffyDisplay.GRID) {
            containerStyle.gridTemplateColumns = BenchSupport.frTracks(2);
        }
        BenchSupport.padding(containerStyle, 1f);

        NodeId[] children = new NodeId[3];
        for (int i = 0; i < children.length; i++) {
            children[i] = buildMixedTree(tree, depth - 1, level + 1);
        }
        return tree.newWithChildren(containerStyle, children);
    }
}
