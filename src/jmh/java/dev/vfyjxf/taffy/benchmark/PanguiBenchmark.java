package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;
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
 * Benchmarks translated from PanGUI's layout benchmark templates
 * ({@code upstream/taffy/benches/pangui/*.txt}).
 *
 * <p>The templates are a small declarative DSL. Rather than parsing the {@code .txt} files at
 * runtime, each workload is built directly with the Java style API. The mapping follows upstream's
 * {@code pangui.rs} translator:</p>
 * <ul>
 *   <li>{@code Horizontal}/{@code Vertical} become {@code FlexDirection.ROW}/{@code COLUMN}.</li>
 *   <li>{@code Width/Height(Pixels)} become length dimensions; {@code Percentage} becomes a
 *       percentage; {@code Fit} becomes {@code auto}.</li>
 *   <li>{@code Expand(w)} on the parent's main axis becomes {@code flex-grow: w, flex-shrink: 1,
 *       flex-basis: 0}; on the cross axis it becomes {@code align-items: stretch} (the default).</li>
 *   <li>{@code Wrap(Auto)} becomes {@code FlexWrap.WRAP}.</li>
 *   <li>{@code Ratio(r)} becomes {@code aspect-ratio: 1/r} when applied to height.</li>
 * </ul>
 *
 * <p>Roots whose template height is {@code Fit} are laid out against
 * {@code (definite width, max-content height)}, matching the root-size rule in {@code pangui.rs}.
 * The 100,000-node variants and the ~100k-node {@code fit_nesting} workload live in
 * {@link PanguiExtendedBenchmark} so the default suite stays short.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
@Fork(1)
public class PanguiBenchmark {

    // === wide_no_wrap_simple_few / many (many lives in PanguiExtendedBenchmark) ===

    @State(Scope.Thread)
    public static class WideNoWrapState {
        @Param({"1000", "10000"})
        public int nodeCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            treeCase = wideNoWrap(nodeCount);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void wideNoWrapSimple(WideNoWrapState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(100f),
            AvailableSpace.definite(100f)
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === wide_wrapping ===

    @State(Scope.Thread)
    public static class WideWrappingState {
        @Param({"10000"})
        public int nodeCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            treeCase = wideWrapping(nodeCount);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void wideWrapping(WideWrappingState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(100f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === nested_vertical_stack ===

    @State(Scope.Thread)
    public static class NestedVerticalStackState {
        @Param({"10000"})
        public int nodeCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            treeCase = nestedVerticalStack(nodeCount);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void nestedVerticalStack(NestedVerticalStackState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(200f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === flex_expand_equal_weights ===

    @State(Scope.Thread)
    public static class EqualWeightsState {
        @Param({"15000"})
        public int nodeCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            treeCase = flexExpandEqualWeights(nodeCount);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void flexExpandEqualWeights(EqualWeightsState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(10000f),
            AvailableSpace.definite(100f)
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === flex_expand_weights ===

    @State(Scope.Thread)
    public static class WeightedExpandState {
        @Param({"5000"})
        public int groupCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            treeCase = flexExpandWeights(groupCount);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void flexExpandWeights(WeightedExpandState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(10000f),
            AvailableSpace.definite(100f)
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === percentage_and_ratio ===

    @State(Scope.Thread)
    public static class PercentageRatioState {
        @Param({"10000"})
        public int nodeCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            treeCase = percentageAndRatio(nodeCount);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void percentageAndRatio(PercentageRatioState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(1000f),
            AvailableSpace.definite(1000f)
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === expand_with_max_constraint ===

    @State(Scope.Thread)
    public static class MaxConstraintState {
        @Param({"1000"})
        public int groupCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            treeCase = expandWithConstraint(groupCount, 40f, Float.NaN);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void expandWithMaxConstraint(MaxConstraintState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(100f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === expand_with_min_constraint ===

    @State(Scope.Thread)
    public static class MinConstraintState {
        @Param({"1000"})
        public int groupCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            treeCase = expandWithConstraint(groupCount, Float.NaN, 60f);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void expandWithMinConstraint(MinConstraintState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(100f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === perpendicular_expand_with_wrap ===

    @State(Scope.Thread)
    public static class PerpendicularWrapState {
        @Param({"1000"})
        public int repeatCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            treeCase = perpendicularExpandWithWrap(repeatCount);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void perpendicularExpandWithWrap(PerpendicularWrapState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(100f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === pixels_with_min_expand_constraint ===

    @State(Scope.Thread)
    public static class PixelsMinExpandState {
        @Param({"10000"})
        public int groupCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            treeCase = pixelsWithMinExpand(groupCount);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void pixelsWithMinExpandConstraint(PixelsMinExpandState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(100f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === padding_and_margin ===

    @State(Scope.Thread)
    public static class PaddingMarginState {
        @Param({"100"})
        public int nodeCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            treeCase = paddingAndMargin(nodeCount);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void paddingAndMargin(PaddingMarginState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(1000f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // ==================== Workload builders ====================

    private static BenchSupport.TreeCase wideNoWrap(int nodeCount) {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.ROW, 100f, 100f);
        TaffyStyle leafStyle = BenchSupport.sized(10f, 10f);
        NodeId[] children = new NodeId[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            children[i] = tree.newLeaf(leafStyle);
        }
        return new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, children));
    }

    private static BenchSupport.TreeCase wideWrapping(int nodeCount) {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.ROW, 100f, Float.NaN);
        rootStyle.flexWrap = FlexWrap.WRAP;
        TaffyStyle leafStyle = BenchSupport.sized(10f, 10f);
        NodeId[] children = new NodeId[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            children[i] = tree.newLeaf(leafStyle);
        }
        return new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, children));
    }

    private static BenchSupport.TreeCase nestedVerticalStack(int nodeCount) {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, 200f, Float.NaN);
        BenchSupport.padding(rootStyle, 10f);
        BenchSupport.gap(rootStyle, 5f);
        TaffyStyle leafStyle = new TaffyStyle();
        leafStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.length(1f));
        NodeId[] children = new NodeId[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            children[i] = tree.newLeaf(leafStyle);
        }
        return new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, children));
    }

    private static BenchSupport.TreeCase flexExpandEqualWeights(int nodeCount) {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.ROW, 10000f, 100f);
        TaffyStyle leafStyle = new TaffyStyle();
        leafStyle.flexGrow = 1f;
        leafStyle.flexShrink = 1f;
        leafStyle.flexBasis = TaffyDimension.ZERO;
        NodeId[] children = new NodeId[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            children[i] = tree.newLeaf(leafStyle);
        }
        return new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, children));
    }

    private static BenchSupport.TreeCase flexExpandWeights(int groupCount) {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.ROW, 10000f, 100f);

        TaffyStyle groupStyle = BenchSupport.sizedFlex(FlexDirection.ROW, 100f, 100f);
        TaffyStyle firstStyle = new TaffyStyle();
        firstStyle.flexGrow = 1f;
        firstStyle.flexShrink = 1f;
        firstStyle.flexBasis = TaffyDimension.ZERO;
        TaffyStyle secondStyle = new TaffyStyle();
        secondStyle.flexGrow = 2f;
        secondStyle.flexShrink = 1f;
        secondStyle.flexBasis = TaffyDimension.ZERO;

        NodeId[] groups = new NodeId[groupCount];
        for (int i = 0; i < groupCount; i++) {
            NodeId first = tree.newLeaf(firstStyle);
            NodeId second = tree.newLeaf(secondStyle);
            groups[i] = tree.newWithChildren(groupStyle, new NodeId[]{first, second});
        }
        return new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, groups));
    }

    private static BenchSupport.TreeCase percentageAndRatio(int nodeCount) {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, 1000f, 1000f);
        TaffyStyle leafStyle = new TaffyStyle();
        leafStyle.size = new TaffySize<>(TaffyDimension.percent(0.5f), TaffyDimension.AUTO);
        leafStyle.aspectRatio = 2.0f;
        NodeId[] children = new NodeId[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            children[i] = tree.newLeaf(leafStyle);
        }
        return new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, children));
    }

    private static BenchSupport.TreeCase expandWithConstraint(int groupCount, float maxWidth, float minWidth) {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.ROW, 100f, Float.NaN);
        TaffyStyle groupStyle = BenchSupport.sizedFlex(FlexDirection.ROW, 100f, Float.NaN);

        TaffyStyle firstStyle = new TaffyStyle();
        firstStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(10f));
        firstStyle.flexGrow = 1f;
        firstStyle.flexShrink = 1f;
        firstStyle.flexBasis = TaffyDimension.ZERO;

        TaffyStyle secondStyle = new TaffyStyle();
        secondStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(10f));
        secondStyle.flexGrow = 1f;
        secondStyle.flexShrink = 1f;
        secondStyle.flexBasis = TaffyDimension.ZERO;
        if (!Float.isNaN(maxWidth)) {
            secondStyle.maxSize = new TaffySize<>(TaffyDimension.length(maxWidth), TaffyDimension.AUTO);
        }
        if (!Float.isNaN(minWidth)) {
            secondStyle.minSize = new TaffySize<>(TaffyDimension.length(minWidth), TaffyDimension.AUTO);
        }

        NodeId[] groups = new NodeId[groupCount];
        for (int i = 0; i < groupCount; i++) {
            NodeId first = tree.newLeaf(firstStyle);
            NodeId second = tree.newLeaf(secondStyle);
            groups[i] = tree.newWithChildren(groupStyle, new NodeId[]{first, second});
        }
        return new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, groups));
    }

    private static BenchSupport.TreeCase perpendicularExpandWithWrap(int repeatCount) {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.ROW, 100f, Float.NaN);
        rootStyle.flexWrap = FlexWrap.WRAP;
        rootStyle.alignItems = AlignItems.STRETCH;

        TaffyStyle tallStyle = new TaffyStyle();
        tallStyle.size = new TaffySize<>(TaffyDimension.length(1f), TaffyDimension.AUTO);

        NodeId[] children = new NodeId[repeatCount * 12];
        int index = 0;
        for (int i = 0; i < repeatCount; i++) {
            for (int j = 0; j < 6; j++) {
                float size = 10f * (j + 1);
                children[index++] = tree.newLeaf(BenchSupport.sized(size, size));
                children[index++] = tree.newLeaf(tallStyle);
            }
        }
        return new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, children));
    }

    private static BenchSupport.TreeCase pixelsWithMinExpand(int groupCount) {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.ROW, 100f, Float.NaN);
        TaffyStyle groupStyle = BenchSupport.sizedFlex(FlexDirection.ROW, 100f, Float.NaN);

        TaffyStyle leafStyle = new TaffyStyle();
        leafStyle.size = new TaffySize<>(TaffyDimension.length(10f), TaffyDimension.length(10f));
        leafStyle.flexGrow = 1f;
        leafStyle.flexShrink = 0f;
        leafStyle.flexBasis = TaffyDimension.AUTO;

        NodeId[] groups = new NodeId[groupCount];
        for (int i = 0; i < groupCount; i++) {
            NodeId first = tree.newLeaf(leafStyle);
            NodeId second = tree.newLeaf(leafStyle);
            groups[i] = tree.newWithChildren(groupStyle, new NodeId[]{first, second});
        }
        return new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, groups));
    }

    private static BenchSupport.TreeCase paddingAndMargin(int nodeCount) {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, 1000f, Float.NaN);
        BenchSupport.padding(rootStyle, 20f);
        BenchSupport.gap(rootStyle, 10f);

        TaffyStyle leafStyle = new TaffyStyle();
        leafStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.length(20f));
        leafStyle.margin = dev.vfyjxf.taffy.geometry.TaffyRect.all(
            dev.vfyjxf.taffy.style.LengthPercentageAuto.length(2f));

        NodeId[] children = new NodeId[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            children[i] = tree.newLeaf(leafStyle);
        }
        return new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, children));
    }
}
