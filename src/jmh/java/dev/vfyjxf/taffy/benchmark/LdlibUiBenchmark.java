package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
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
 * Mixed / realistic UI benchmarks modelled on an LDLib-style Minecraft UI tree
 * (panels, labels, buttons and scroll areas).
 *
 * <p>LDLib lays out widget trees that mix flex panels with measured text labels and fixed-size
 * controls. These benchmarks capture the four dominant shapes of such a tree:</p>
 * <ul>
 *   <li>a panel tree with header, sidebar and a long list of mixed panels;</li>
 *   <li>many measured text leaves (measure-heavy layout);</li>
 *   <li>percentage sizes combined with aspect ratios;</li>
 *   <li>a grid of flex cards, each containing measured labels and a scroll area.</li>
 * </ul>
 *
 * <p>Upstream mixed.rs uses cosmic_text; the Java suite uses
 * {@link BenchSupport.WordWrapMeasure}, a deterministic allocation-light word-wrapping measure
 * callback that keeps the benchmark focused on the layout engine's measure path.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
@Fork(1)
public class LdlibUiBenchmark {

    private static final float ROOT_WIDTH = 1600f;
    private static final float ROOT_HEIGHT = 900f;

    // === Panel tree ===

    @State(Scope.Thread)
    public static class PanelTreeState {
        @Param({"10", "100", "1000"})
        public int panelCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            treeCase = buildPanelTree(panelCount);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void panelTree(PanelTreeState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(ROOT_WIDTH),
            AvailableSpace.definite(ROOT_HEIGHT)
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === Measure-heavy leaves ===

    @State(Scope.Thread)
    public static class MeasureHeavyState {
        @Param({"100", "1000", "10000"})
        public int leafCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            MeasureFunc measure = new BenchSupport.WordWrapMeasure(24);
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, 800f, Float.NaN);
            BenchSupport.gap(rootStyle, 2f);

            NodeId[] children = new NodeId[leafCount];
            for (int i = 0; i < leafCount; i++) {
                TaffyStyle leafStyle = new TaffyStyle();
                leafStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
                children[i] = tree.newLeafWithMeasure(leafStyle, measure);
            }
            treeCase = new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, children));
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void measureHeavyLeaves(MeasureHeavyState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(800f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === Percentage + aspect ratio ===

    @State(Scope.Thread)
    public static class PercentAspectState {
        @Param({"100", "1000", "10000"})
        public int leafCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, 1920f, 1080f);

            NodeId[] children = new NodeId[leafCount];
            for (int i = 0; i < leafCount; i++) {
                TaffyStyle leafStyle = new TaffyStyle();
                leafStyle.size = new TaffySize<>(TaffyDimension.percent(0.25f), TaffyDimension.AUTO);
                leafStyle.aspectRatio = 1.5f;
                children[i] = tree.newLeaf(leafStyle);
            }
            treeCase = new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, children));
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void percentAndAspectRatio(PercentAspectState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(1920f),
            AvailableSpace.definite(1080f)
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === Grid of flex cards ===

    @State(Scope.Thread)
    public static class GridFlexState {
        @Param({"24", "96", "384"})
        public int cardCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            treeCase = buildGridFlexCards(cardCount);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void gridFlexCards(GridFlexState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(ROOT_WIDTH),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === Scroll panel list ===

    @State(Scope.Thread)
    public static class ScrollPanelsState {
        @Param({"10", "100"})
        public int panelCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            treeCase = buildScrollPanels(panelCount);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void scrollPanels(ScrollPanelsState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(1280f),
            AvailableSpace.definite(720f)
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // ==================== Builders ====================

    private static BenchSupport.TreeCase buildPanelTree(int panelCount) {
        TaffyTree tree = new TaffyTree();
        MeasureFunc measure = new BenchSupport.WordWrapMeasure(18);

        TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, ROOT_WIDTH, ROOT_HEIGHT);
        BenchSupport.padding(rootStyle, 8f);
        BenchSupport.gap(rootStyle, 8f);

        NodeId header = buildHeader(tree, measure);
        NodeId body = buildBody(tree, measure, panelCount);
        return new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, new NodeId[]{header, body}));
    }

    private static NodeId buildHeader(TaffyTree tree, MeasureFunc measure) {
        TaffyStyle headerStyle = BenchSupport.sizedFlex(FlexDirection.ROW, Float.NaN, 56f);
        BenchSupport.padding(headerStyle, 8f);
        BenchSupport.gap(headerStyle, 8f);
        headerStyle.alignItems = AlignItems.CENTER;

        NodeId title = tree.newLeafWithMeasure(labelStyle(1f), measure);
        NodeId[] children = new NodeId[4];
        children[0] = title;
        for (int i = 1; i < 4; i++) {
            children[i] = tree.newLeaf(buttonStyle());
        }
        return tree.newWithChildren(headerStyle, children);
    }

    private static NodeId buildBody(TaffyTree tree, MeasureFunc measure, int panelCount) {
        TaffyStyle bodyStyle = BenchSupport.sizedFlex(FlexDirection.ROW, Float.NaN, Float.NaN);
        bodyStyle.flexGrow = 1f;
        BenchSupport.gap(bodyStyle, 8f);

        NodeId sidebar = buildSidebar(tree, measure);
        NodeId main = buildMainPanel(tree, measure, panelCount);
        return tree.newWithChildren(bodyStyle, new NodeId[]{sidebar, main});
    }

    private static NodeId buildSidebar(TaffyTree tree, MeasureFunc measure) {
        TaffyStyle sidebarStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, 240f, Float.NaN);
        BenchSupport.padding(sidebarStyle, 8f);
        BenchSupport.gap(sidebarStyle, 6f);

        NodeId[] children = new NodeId[12];
        for (int i = 0; i < children.length; i++) {
            if ((i & 1) == 0) {
                children[i] = tree.newLeafWithMeasure(labelStyle(1f), measure);
            } else {
                children[i] = tree.newLeaf(buttonStyle());
            }
        }
        return tree.newWithChildren(sidebarStyle, children);
    }

    private static NodeId buildMainPanel(TaffyTree tree, MeasureFunc measure, int panelCount) {
        TaffyStyle mainStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, Float.NaN, Float.NaN);
        mainStyle.flexGrow = 1f;
        mainStyle.padding = dev.vfyjxf.taffy.geometry.TaffyRect.all(
            dev.vfyjxf.taffy.style.LengthPercentage.length(8f));
        BenchSupport.gap(mainStyle, 8f);

        NodeId[] panels = new NodeId[panelCount];
        for (int i = 0; i < panelCount; i++) {
            panels[i] = buildPanel(tree, measure, i);
        }
        return tree.newWithChildren(mainStyle, panels);
    }

    private static NodeId buildPanel(TaffyTree tree, MeasureFunc measure, int index) {
        TaffyStyle panelStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, Float.NaN, Float.NaN);
        panelStyle.padding = dev.vfyjxf.taffy.geometry.TaffyRect.all(
            dev.vfyjxf.taffy.style.LengthPercentage.length(8f));
        BenchSupport.gap(panelStyle, 6f);

        TaffyStyle rowStyle = BenchSupport.sizedFlex(FlexDirection.ROW, Float.NaN, Float.NaN);
        rowStyle.alignItems = AlignItems.CENTER;
        BenchSupport.gap(rowStyle, 6f);

        NodeId title = tree.newLeafWithMeasure(labelStyle(1f), measure);
        NodeId action = tree.newLeaf(buttonStyle());
        NodeId header = tree.newWithChildren(rowStyle, new NodeId[]{title, action});

        NodeId scroll = buildScrollArea(tree, 4 + (index % 4));
        return tree.newWithChildren(panelStyle, new NodeId[]{header, scroll});
    }

    private static NodeId buildScrollArea(TaffyTree tree, int rows) {
        TaffyStyle scrollStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, Float.NaN, 80f);
        BenchSupport.overflow(scrollStyle, Overflow.SCROLL, Overflow.SCROLL);
        scrollStyle.scrollbarWidth = 8f;
        scrollStyle.padding = dev.vfyjxf.taffy.geometry.TaffyRect.all(
            dev.vfyjxf.taffy.style.LengthPercentage.length(4f));
        BenchSupport.gap(scrollStyle, 2f);

        NodeId[] children = new NodeId[rows];
        TaffyStyle rowStyle = new TaffyStyle();
        rowStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.length(12f));
        for (int i = 0; i < rows; i++) {
            children[i] = tree.newLeaf(rowStyle);
        }
        return tree.newWithChildren(scrollStyle, children);
    }

    private static BenchSupport.TreeCase buildGridFlexCards(int cardCount) {
        TaffyTree tree = new TaffyTree();
        MeasureFunc measure = new BenchSupport.WordWrapMeasure(10);

        TaffyStyle gridStyle = new TaffyStyle();
        gridStyle.display = dev.vfyjxf.taffy.style.TaffyDisplay.GRID;
        gridStyle.size = new TaffySize<>(TaffyDimension.length(ROOT_WIDTH), TaffyDimension.AUTO);
        gridStyle.gridTemplateColumns = BenchSupport.frTracks(4);
        gridStyle.gridAutoRows = List.of(TrackSizingFunction.minmax(
            TrackSizingFunction.fixed(120f),
            TrackSizingFunction.auto()
        ));
        BenchSupport.padding(gridStyle, 16f);
        BenchSupport.gap(gridStyle, 12f);
        gridStyle.justifyContent = AlignContent.START;

        NodeId[] cards = new NodeId[cardCount];
        for (int i = 0; i < cardCount; i++) {
            cards[i] = buildCard(tree, measure);
        }
        return new BenchSupport.TreeCase(tree, tree.newWithChildren(gridStyle, cards));
    }

    private static NodeId buildCard(TaffyTree tree, MeasureFunc measure) {
        TaffyStyle cardStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, Float.NaN, Float.NaN);
        BenchSupport.padding(cardStyle, 8f);
        BenchSupport.gap(cardStyle, 6f);

        NodeId title = tree.newLeafWithMeasure(labelStyle(1f), measure);

        TaffyStyle rowStyle = BenchSupport.sizedFlex(FlexDirection.ROW, Float.NaN, Float.NaN);
        rowStyle.alignItems = AlignItems.CENTER;
        BenchSupport.gap(rowStyle, 6f);
        NodeId price = tree.newLeafWithMeasure(labelStyle(1f), measure);
        NodeId rating = tree.newLeaf(BenchSupport.sized(60f, 20f));
        NodeId details = tree.newWithChildren(rowStyle, new NodeId[]{price, rating});

        NodeId buttonRow = buildButtonRow(tree);
        NodeId scroll = buildScrollArea(tree, 3);
        return tree.newWithChildren(cardStyle, new NodeId[]{title, details, buttonRow, scroll});
    }

    private static NodeId buildButtonRow(TaffyTree tree) {
        TaffyStyle rowStyle = BenchSupport.sizedFlex(FlexDirection.ROW, Float.NaN, Float.NaN);
        rowStyle.flexWrap = FlexWrap.WRAP;
        BenchSupport.gap(rowStyle, 6f);
        NodeId[] buttons = new NodeId[3];
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = tree.newLeaf(buttonStyle());
        }
        return tree.newWithChildren(rowStyle, buttons);
    }

    private static BenchSupport.TreeCase buildScrollPanels(int panelCount) {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, 1280f, 720f);
        BenchSupport.padding(rootStyle, 8f);
        BenchSupport.gap(rootStyle, 4f);

        NodeId[] panels = new NodeId[panelCount];
        for (int i = 0; i < panelCount; i++) {
            TaffyStyle panelStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, Float.NaN, 120f);
            BenchSupport.overflow(panelStyle, Overflow.SCROLL, Overflow.SCROLL);
            panelStyle.scrollbarWidth = 8f;
            panelStyle.padding = dev.vfyjxf.taffy.geometry.TaffyRect.all(
                dev.vfyjxf.taffy.style.LengthPercentage.length(4f));

            NodeId[] rows = new NodeId[20];
            TaffyStyle rowStyle = BenchSupport.sized(100f, 10f);
            for (int j = 0; j < rows.length; j++) {
                rows[j] = tree.newLeaf(rowStyle);
            }
            panels[i] = tree.newWithChildren(panelStyle, rows);
        }
        return new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, panels));
    }

    private static TaffyStyle labelStyle(float grow) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        style.flexGrow = grow;
        style.flexShrink = 1f;
        return style;
    }

    private static TaffyStyle buttonStyle() {
        return BenchSupport.sized(80f, 28f);
    }
}
