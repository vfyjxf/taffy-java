package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.Line;
import dev.vfyjxf.taffy.geometry.Rect;
import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.Dimension;
import dev.vfyjxf.taffy.style.Display;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.Style;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Realistic UI scenario benchmarks - simulating real-world layouts
 * 
 * Scenarios:
 * 1. Web page layout: header + sidebar + main content + footer
 * 2. Virtual list: scrollable list with many items (like RecyclerView)
 * 3. Dashboard: grid of cards with varied sizes
 * 4. Form layout: labels + inputs in columns
 * 5. Nested cards: card in card patterns (common in modern UI)
 * 6. Chat UI: message bubbles with varied content
 * 7. E-commerce grid: product cards with images and text
 * 8. Responsive layout: same tree computed at multiple sizes
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class RealisticBenchmark {

    private static final long SEED = 12345L;

    // ==================== 1. Web Page Layout ====================
    // Classic layout: header, sidebar, main content area, footer
    // Main content has multiple sections with varied content

    @State(Scope.Thread)
    public static class WebPageState {
        TaffyTree tree;
        NodeId root;

        @Param({"10", "50", "100"})
        int contentSections;

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            root = buildWebPage(tree, contentSections);
        }
    }

    @Benchmark
    public void webPageLayout(WebPageState state, Blackhole bh) {
        // Typical browser viewport
        state.tree.computeLayout(state.root, Size.of(
            AvailableSpace.definite(1920f),
            AvailableSpace.definite(1080f)
        ));
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== 2. Virtual List (RecyclerView-like) ====================
    // Long scrollable list with many items, each item has complex structure

    @State(Scope.Thread)
    public static class VirtualListState {
        TaffyTree tree;
        NodeId root;

        @Param({"100", "500", "1000", "5000"})
        int itemCount;

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            root = buildVirtualList(tree, itemCount);
        }
    }

    @Benchmark
    public void virtualList(VirtualListState state, Blackhole bh) {
        // Mobile viewport width, unconstrained height
        state.tree.computeLayout(state.root, Size.of(
            AvailableSpace.definite(375f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== 3. Dashboard Grid ====================
    // Grid of cards with different sizes (1x1, 2x1, 1x2, 2x2)

    @State(Scope.Thread)
    public static class DashboardState {
        TaffyTree tree;
        NodeId root;

        @Param({"12", "24", "48", "96"})
        int cardCount;

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            root = buildDashboard(tree, cardCount);
        }
    }

    @Benchmark
    public void dashboardGrid(DashboardState state, Blackhole bh) {
        state.tree.computeLayout(state.root, Size.of(
            AvailableSpace.definite(1440f),
            AvailableSpace.definite(900f)
        ));
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== 4. Form Layout ====================
    // Complex form with labels, inputs, validation messages

    @State(Scope.Thread)
    public static class FormState {
        TaffyTree tree;
        NodeId root;

        @Param({"10", "30", "50", "100"})
        int fieldCount;

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            root = buildForm(tree, fieldCount);
        }
    }

    @Benchmark
    public void formLayout(FormState state, Blackhole bh) {
        state.tree.computeLayout(state.root, Size.of(
            AvailableSpace.definite(600f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== 5. Nested Cards ====================
    // Cards containing cards - common in component libraries

    @State(Scope.Thread)
    public static class NestedCardsState {
        TaffyTree tree;
        NodeId root;

        @Param({"3", "4", "5"})
        int depth;

        @Param({"3", "4"})
        int cardsPerLevel;

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            root = buildNestedCards(tree, depth, cardsPerLevel);
        }
    }

    @Benchmark
    public void nestedCards(NestedCardsState state, Blackhole bh) {
        state.tree.computeLayout(state.root, Size.of(
            AvailableSpace.definite(1200f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== 6. Chat UI ====================
    // Message bubbles with varied content lengths

    @State(Scope.Thread)
    public static class ChatUIState {
        TaffyTree tree;
        NodeId root;
        MeasureFunc measureFunc;

        @Param({"50", "200", "500", "1000"})
        int messageCount;

        @Setup(Level.Trial)
        public void setupTrial() {
            measureFunc = new TextMeasureFunc();
        }

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            root = buildChatUI(tree, messageCount, measureFunc);
        }
    }

    @Benchmark
    public void chatUI(ChatUIState state, Blackhole bh) {
        state.tree.computeLayoutWithMeasure(state.root, Size.of(
            AvailableSpace.definite(375f),
            AvailableSpace.maxContent()
        ), null);
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== 7. E-commerce Product Grid ====================
    // Product cards with image, title, price, rating

    @State(Scope.Thread)
    public static class EcommerceState {
        TaffyTree tree;
        NodeId root;
        MeasureFunc measureFunc;

        @Param({"20", "50", "100", "200"})
        int productCount;

        @Setup(Level.Trial)
        public void setupTrial() {
            measureFunc = new TextMeasureFunc();
        }

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            root = buildEcommerceGrid(tree, productCount, measureFunc);
        }
    }

    @Benchmark
    public void ecommerceGrid(EcommerceState state, Blackhole bh) {
        state.tree.computeLayoutWithMeasure(state.root, Size.of(
            AvailableSpace.definite(1200f),
            AvailableSpace.maxContent()
        ), null);
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== 8. Responsive Multi-Size ====================
    // Same tree computed at multiple viewport sizes

    @State(Scope.Thread)
    public static class ResponsiveState {
        TaffyTree tree;
        NodeId root;
        float[][] viewports;

        @Param({"50", "100"})
        int componentCount;

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            root = buildResponsiveLayout(tree, componentCount);
            // Common device breakpoints
            viewports = new float[][] {
                {320f, 568f},   // iPhone SE
                {375f, 667f},   // iPhone 8
                {414f, 896f},   // iPhone 11
                {768f, 1024f},  // iPad
                {1024f, 768f},  // iPad landscape
                {1366f, 768f},  // Laptop
                {1920f, 1080f}  // Desktop
            };
        }
    }

    @Benchmark
    public void responsiveMultiSize(ResponsiveState state, Blackhole bh) {
        for (float[] vp : state.viewports) {
            state.tree.computeLayout(state.root, Size.of(
                AvailableSpace.definite(vp[0]),
                AvailableSpace.definite(vp[1])
            ));
            bh.consume(state.tree.getLayout(state.root));
        }
    }

    // ==================== 9. Data Table ====================
    // Table with many rows and columns, like spreadsheet

    @State(Scope.Thread)
    public static class DataTableState {
        TaffyTree tree;
        NodeId root;

        @Param({"10", "20"})
        int columns;

        @Param({"100", "500", "1000"})
        int rows;

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            root = buildDataTable(tree, rows, columns);
        }
    }

    @Benchmark
    public void dataTable(DataTableState state, Blackhole bh) {
        state.tree.computeLayout(state.root, Size.of(
            AvailableSpace.definite(1400f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== 10. Mobile App Screen ====================
    // Typical mobile app with nav, tab bar, scrollable content

    @State(Scope.Thread)
    public static class MobileAppState {
        TaffyTree tree;
        NodeId root;
        MeasureFunc measureFunc;

        @Param({"20", "50", "100"})
        int contentItems;

        @Setup(Level.Trial)
        public void setupTrial() {
            measureFunc = new TextMeasureFunc();
        }

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            root = buildMobileApp(tree, contentItems, measureFunc);
        }
    }

    @Benchmark
    public void mobileAppScreen(MobileAppState state, Blackhole bh) {
        state.tree.computeLayoutWithMeasure(state.root, Size.of(
            AvailableSpace.definite(375f),
            AvailableSpace.definite(812f)
        ), null);
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== Builder Methods ====================

    private static NodeId buildWebPage(TaffyTree tree, int contentSections) {
        // Root: column flex container
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.flexDirection = FlexDirection.COLUMN;
        rootStyle.size = new Size<>(Dimension.percent(1f), Dimension.percent(1f));

        // Header
        Style headerStyle = new Style();
        headerStyle.display = Display.FLEX;
        headerStyle.flexDirection = FlexDirection.ROW;
        headerStyle.size = new Size<>(Dimension.percent(1f), Dimension.length(60f));
        headerStyle.padding = Rect.all(LengthPercentage.length(16f));
        headerStyle.gap = new Size<>(LengthPercentage.length(16f), LengthPercentage.length(0f));
        
        NodeId logo = tree.newLeaf(createBoxStyle(120f, 40f));
        NodeId[] navItems = new NodeId[5];
        for (int i = 0; i < 5; i++) {
            navItems[i] = tree.newLeaf(createBoxStyle(80f, 40f));
        }
        NodeId nav = tree.newWithChildren(createFlexRowStyle(), navItems);
        NodeId header = tree.newWithChildren(headerStyle, new NodeId[]{logo, nav});

        // Body: sidebar + main
        Style bodyStyle = new Style();
        bodyStyle.display = Display.FLEX;
        bodyStyle.flexDirection = FlexDirection.ROW;
        bodyStyle.flexGrow = 1f;

        // Sidebar
        Style sidebarStyle = new Style();
        sidebarStyle.display = Display.FLEX;
        sidebarStyle.flexDirection = FlexDirection.COLUMN;
        sidebarStyle.size = new Size<>(Dimension.length(250f), Dimension.AUTO);
        sidebarStyle.padding = Rect.all(LengthPercentage.length(16f));
        sidebarStyle.gap = new Size<>(LengthPercentage.length(0f), LengthPercentage.length(8f));
        
        NodeId[] sidebarItems = new NodeId[8];
        for (int i = 0; i < 8; i++) {
            sidebarItems[i] = tree.newLeaf(createBoxStyle(Float.NaN, 40f));
        }
        NodeId sidebar = tree.newWithChildren(sidebarStyle, sidebarItems);

        // Main content
        Style mainStyle = new Style();
        mainStyle.display = Display.FLEX;
        mainStyle.flexDirection = FlexDirection.COLUMN;
        mainStyle.flexGrow = 1f;
        mainStyle.padding = Rect.all(LengthPercentage.length(24f));
        mainStyle.gap = new Size<>(LengthPercentage.length(0f), LengthPercentage.length(24f));

        NodeId[] sections = new NodeId[contentSections];
        Random rng = new Random(SEED);
        for (int i = 0; i < contentSections; i++) {
            sections[i] = buildContentSection(tree, rng);
        }
        NodeId main = tree.newWithChildren(mainStyle, sections);

        NodeId body = tree.newWithChildren(bodyStyle, new NodeId[]{sidebar, main});

        // Footer
        Style footerStyle = new Style();
        footerStyle.display = Display.FLEX;
        footerStyle.flexDirection = FlexDirection.ROW;
        footerStyle.justifyContent = AlignContent.SPACE_BETWEEN;
        footerStyle.size = new Size<>(Dimension.percent(1f), Dimension.length(80f));
        footerStyle.padding = Rect.all(LengthPercentage.length(16f));

        NodeId[] footerCols = new NodeId[4];
        for (int i = 0; i < 4; i++) {
            footerCols[i] = tree.newLeaf(createBoxStyle(200f, Float.NaN));
        }
        NodeId footer = tree.newWithChildren(footerStyle, footerCols);

        return tree.newWithChildren(rootStyle, new NodeId[]{header, body, footer});
    }

    private static NodeId buildContentSection(TaffyTree tree, Random rng) {
        Style sectionStyle = new Style();
        sectionStyle.display = Display.FLEX;
        sectionStyle.flexDirection = FlexDirection.COLUMN;
        sectionStyle.padding = Rect.all(LengthPercentage.length(16f));
        sectionStyle.gap = new Size<>(LengthPercentage.length(0f), LengthPercentage.length(12f));

        // Title
        NodeId title = tree.newLeaf(createBoxStyle(Float.NaN, 32f));
        
        // Content rows
        int rowCount = 2 + rng.nextInt(4);
        NodeId[] rows = new NodeId[rowCount + 1];
        rows[0] = title;
        
        for (int i = 0; i < rowCount; i++) {
            int colCount = 1 + rng.nextInt(4);
            NodeId[] cols = new NodeId[colCount];
            for (int j = 0; j < colCount; j++) {
                cols[j] = tree.newLeaf(createBoxStyle(Float.NaN, 40f + rng.nextFloat() * 60f));
            }
            Style rowStyle = createFlexRowStyle();
            rowStyle.gap = new Size<>(LengthPercentage.length(12f), LengthPercentage.length(0f));
            rows[i + 1] = tree.newWithChildren(rowStyle, cols);
        }

        return tree.newWithChildren(sectionStyle, rows);
    }

    private static NodeId buildVirtualList(TaffyTree tree, int itemCount) {
        Style listStyle = new Style();
        listStyle.display = Display.FLEX;
        listStyle.flexDirection = FlexDirection.COLUMN;
        listStyle.gap = new Size<>(LengthPercentage.length(0f), LengthPercentage.length(8f));
        listStyle.padding = Rect.all(LengthPercentage.length(12f));

        Random rng = new Random(SEED);
        NodeId[] items = new NodeId[itemCount];
        for (int i = 0; i < itemCount; i++) {
            items[i] = buildListItem(tree, rng);
        }

        return tree.newWithChildren(listStyle, items);
    }

    private static NodeId buildListItem(TaffyTree tree, Random rng) {
        // List item: avatar | content | action
        Style itemStyle = new Style();
        itemStyle.display = Display.FLEX;
        itemStyle.flexDirection = FlexDirection.ROW;
        itemStyle.alignItems = AlignItems.CENTER;
        itemStyle.padding = Rect.all(LengthPercentage.length(12f));
        itemStyle.gap = new Size<>(LengthPercentage.length(12f), LengthPercentage.length(0f));

        NodeId avatar = tree.newLeaf(createBoxStyle(48f, 48f));

        // Content: title + subtitle
        Style contentStyle = new Style();
        contentStyle.display = Display.FLEX;
        contentStyle.flexDirection = FlexDirection.COLUMN;
        contentStyle.flexGrow = 1f;
        contentStyle.gap = new Size<>(LengthPercentage.length(0f), LengthPercentage.length(4f));

        NodeId title = tree.newLeaf(createBoxStyle(Float.NaN, 20f));
        NodeId subtitle = tree.newLeaf(createBoxStyle(Float.NaN, 16f));
        NodeId content = tree.newWithChildren(contentStyle, new NodeId[]{title, subtitle});

        NodeId action = tree.newLeaf(createBoxStyle(40f, 40f));

        return tree.newWithChildren(itemStyle, new NodeId[]{avatar, content, action});
    }

    private static NodeId buildDashboard(TaffyTree tree, int cardCount) {
        Style gridStyle = new Style();
        gridStyle.display = Display.GRID;
        gridStyle.padding = Rect.all(LengthPercentage.length(24f));
        gridStyle.gap = new Size<>(LengthPercentage.length(16f), LengthPercentage.length(16f));

        // 6-column grid
        List<TrackSizingFunction> cols = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            cols.add(TrackSizingFunction.fr(1f));
        }
        gridStyle.gridTemplateColumns = cols;
        gridStyle.gridAutoRows = List.of(TrackSizingFunction.minmax(
            TrackSizingFunction.fixed(150f),
            TrackSizingFunction.auto()
        ));

        Random rng = new Random(SEED);
        NodeId[] cards = new NodeId[cardCount];
        for (int i = 0; i < cardCount; i++) {
            cards[i] = buildDashboardCard(tree, rng);
        }

        return tree.newWithChildren(gridStyle, cards);
    }

    private static NodeId buildDashboardCard(TaffyTree tree, Random rng) {
        // Card spans 1-2 columns and 1-2 rows
        Style cardStyle = new Style();
        cardStyle.display = Display.FLEX;
        cardStyle.flexDirection = FlexDirection.COLUMN;
        cardStyle.padding = Rect.all(LengthPercentage.length(16f));
        cardStyle.gap = new Size<>(LengthPercentage.length(0f), LengthPercentage.length(12f));

        // Random spanning
        int colSpan = rng.nextFloat() < 0.3f ? 2 : 1;
        int rowSpan = rng.nextFloat() < 0.2f ? 2 : 1;
        cardStyle.gridColumn = new Line<>(GridPlacement.auto(), GridPlacement.span(colSpan));
        cardStyle.gridRow = new Line<>(GridPlacement.auto(), GridPlacement.span(rowSpan));

        NodeId header = tree.newLeaf(createBoxStyle(Float.NaN, 24f));
        NodeId content = tree.newLeaf(createBoxStyle(Float.NaN, 80f + rng.nextFloat() * 120f));
        NodeId footer = tree.newLeaf(createBoxStyle(Float.NaN, 32f));

        return tree.newWithChildren(cardStyle, new NodeId[]{header, content, footer});
    }

    private static NodeId buildForm(TaffyTree tree, int fieldCount) {
        Style formStyle = new Style();
        formStyle.display = Display.FLEX;
        formStyle.flexDirection = FlexDirection.COLUMN;
        formStyle.padding = Rect.all(LengthPercentage.length(24f));
        formStyle.gap = new Size<>(LengthPercentage.length(0f), LengthPercentage.length(16f));

        Random rng = new Random(SEED);
        NodeId[] fields = new NodeId[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            fields[i] = buildFormField(tree, rng);
        }

        return tree.newWithChildren(formStyle, fields);
    }

    private static NodeId buildFormField(TaffyTree tree, Random rng) {
        // Field: label + input (+ optional help text)
        Style fieldStyle = new Style();
        fieldStyle.display = Display.FLEX;
        fieldStyle.flexDirection = FlexDirection.COLUMN;
        fieldStyle.gap = new Size<>(LengthPercentage.length(0f), LengthPercentage.length(4f));

        NodeId label = tree.newLeaf(createBoxStyle(Float.NaN, 20f));
        NodeId input = tree.newLeaf(createBoxStyle(Float.NaN, 40f));

        if (rng.nextFloat() < 0.3f) {
            NodeId helpText = tree.newLeaf(createBoxStyle(Float.NaN, 16f));
            return tree.newWithChildren(fieldStyle, new NodeId[]{label, input, helpText});
        }

        return tree.newWithChildren(fieldStyle, new NodeId[]{label, input});
    }

    private static NodeId buildNestedCards(TaffyTree tree, int depth, int cardsPerLevel) {
        if (depth == 0) {
            return tree.newLeaf(createBoxStyle(Float.NaN, 40f));
        }

        Style cardStyle = new Style();
        cardStyle.display = Display.FLEX;
        cardStyle.flexDirection = FlexDirection.COLUMN;
        cardStyle.padding = Rect.all(LengthPercentage.length(16f));
        cardStyle.gap = new Size<>(LengthPercentage.length(0f), LengthPercentage.length(12f));

        NodeId header = tree.newLeaf(createBoxStyle(Float.NaN, 24f));
        
        Style contentStyle = new Style();
        contentStyle.display = Display.FLEX;
        contentStyle.flexDirection = FlexDirection.ROW;
        contentStyle.flexWrap = FlexWrap.WRAP;
        contentStyle.gap = new Size<>(LengthPercentage.length(12f), LengthPercentage.length(12f));

        NodeId[] children = new NodeId[cardsPerLevel];
        for (int i = 0; i < cardsPerLevel; i++) {
            children[i] = buildNestedCards(tree, depth - 1, cardsPerLevel);
        }
        NodeId content = tree.newWithChildren(contentStyle, children);

        return tree.newWithChildren(cardStyle, new NodeId[]{header, content});
    }

    private static NodeId buildChatUI(TaffyTree tree, int messageCount, MeasureFunc measureFunc) {
        Style chatStyle = new Style();
        chatStyle.display = Display.FLEX;
        chatStyle.flexDirection = FlexDirection.COLUMN;
        chatStyle.gap = new Size<>(LengthPercentage.length(0f), LengthPercentage.length(8f));
        chatStyle.padding = Rect.all(LengthPercentage.length(12f));

        Random rng = new Random(SEED);
        NodeId[] messages = new NodeId[messageCount];
        for (int i = 0; i < messageCount; i++) {
            messages[i] = buildChatMessage(tree, rng, i % 2 == 0, measureFunc);
        }

        return tree.newWithChildren(chatStyle, messages);
    }

    private static NodeId buildChatMessage(TaffyTree tree, Random rng, boolean isOwn, MeasureFunc measureFunc) {
        Style messageStyle = new Style();
        messageStyle.display = Display.FLEX;
        messageStyle.flexDirection = FlexDirection.ROW;
        messageStyle.justifyContent = isOwn ? AlignContent.FLEX_END : AlignContent.FLEX_START;
        messageStyle.padding = Rect.of(
            LengthPercentage.length(isOwn ? 48f : 0f),
            LengthPercentage.length(isOwn ? 0f : 48f),
            LengthPercentage.length(0f),
            LengthPercentage.length(0f)
        );

        Style bubbleStyle = new Style();
        bubbleStyle.display = Display.FLEX;
        bubbleStyle.flexDirection = FlexDirection.COLUMN;
        bubbleStyle.padding = Rect.all(LengthPercentage.length(12f));
        bubbleStyle.maxSize = new Size<>(Dimension.percent(0.8f), Dimension.AUTO);

        // Text content (measured)
        Style textStyle = new Style();
        textStyle.flexShrink = 1f;
        NodeId text = tree.newLeafWithMeasure(textStyle, measureFunc);

        // Timestamp
        NodeId timestamp = tree.newLeaf(createBoxStyle(60f, 14f));

        NodeId bubble = tree.newWithChildren(bubbleStyle, new NodeId[]{text, timestamp});
        return tree.newWithChildren(messageStyle, new NodeId[]{bubble});
    }

    private static NodeId buildEcommerceGrid(TaffyTree tree, int productCount, MeasureFunc measureFunc) {
        Style gridStyle = new Style();
        gridStyle.display = Display.GRID;
        gridStyle.padding = Rect.all(LengthPercentage.length(24f));
        gridStyle.gap = new Size<>(LengthPercentage.length(24f), LengthPercentage.length(24f));

        // Responsive columns
        List<TrackSizingFunction> cols = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            cols.add(TrackSizingFunction.fr(1f));
        }
        gridStyle.gridTemplateColumns = cols;

        Random rng = new Random(SEED);
        NodeId[] products = new NodeId[productCount];
        for (int i = 0; i < productCount; i++) {
            products[i] = buildProductCard(tree, rng, measureFunc);
        }

        return tree.newWithChildren(gridStyle, products);
    }

    private static NodeId buildProductCard(TaffyTree tree, Random rng, MeasureFunc measureFunc) {
        Style cardStyle = new Style();
        cardStyle.display = Display.FLEX;
        cardStyle.flexDirection = FlexDirection.COLUMN;

        // Image
        NodeId image = tree.newLeaf(createBoxStyle(Float.NaN, 200f));

        // Content
        Style contentStyle = new Style();
        contentStyle.display = Display.FLEX;
        contentStyle.flexDirection = FlexDirection.COLUMN;
        contentStyle.padding = Rect.all(LengthPercentage.length(12f));
        contentStyle.gap = new Size<>(LengthPercentage.length(0f), LengthPercentage.length(8f));

        // Title (measured text)
        NodeId title = tree.newLeafWithMeasure(new Style(), measureFunc);
        
        // Price row
        Style priceRowStyle = createFlexRowStyle();
        priceRowStyle.justifyContent = AlignContent.SPACE_BETWEEN;
        NodeId price = tree.newLeaf(createBoxStyle(80f, 24f));
        NodeId rating = tree.newLeaf(createBoxStyle(60f, 20f));
        NodeId priceRow = tree.newWithChildren(priceRowStyle, new NodeId[]{price, rating});

        // Add to cart button
        NodeId button = tree.newLeaf(createBoxStyle(Float.NaN, 40f));

        NodeId content = tree.newWithChildren(contentStyle, new NodeId[]{title, priceRow, button});

        return tree.newWithChildren(cardStyle, new NodeId[]{image, content});
    }

    private static NodeId buildResponsiveLayout(TaffyTree tree, int componentCount) {
        // Flex container with wrap
        Style containerStyle = new Style();
        containerStyle.display = Display.FLEX;
        containerStyle.flexDirection = FlexDirection.ROW;
        containerStyle.flexWrap = FlexWrap.WRAP;
        containerStyle.gap = new Size<>(LengthPercentage.length(16f), LengthPercentage.length(16f));
        containerStyle.padding = Rect.all(LengthPercentage.length(16f));

        Random rng = new Random(SEED);
        NodeId[] components = new NodeId[componentCount];
        for (int i = 0; i < componentCount; i++) {
            components[i] = buildResponsiveComponent(tree, rng);
        }

        return tree.newWithChildren(containerStyle, components);
    }

    private static NodeId buildResponsiveComponent(TaffyTree tree, Random rng) {
        Style style = new Style();
        style.display = Display.FLEX;
        style.flexDirection = FlexDirection.COLUMN;
        style.flexBasis = Dimension.length(280f);
        style.flexGrow = 1f;
        style.flexShrink = 1f;
        style.minSize = new Size<>(Dimension.length(200f), Dimension.AUTO);
        style.maxSize = new Size<>(Dimension.length(400f), Dimension.AUTO);
        style.padding = Rect.all(LengthPercentage.length(16f));
        style.gap = new Size<>(LengthPercentage.length(0f), LengthPercentage.length(8f));

        int childCount = 2 + rng.nextInt(3);
        NodeId[] children = new NodeId[childCount];
        for (int i = 0; i < childCount; i++) {
            children[i] = tree.newLeaf(createBoxStyle(Float.NaN, 24f + rng.nextFloat() * 40f));
        }

        return tree.newWithChildren(style, children);
    }

    private static NodeId buildDataTable(TaffyTree tree, int rows, int columns) {
        Style tableStyle = new Style();
        tableStyle.display = Display.GRID;

        // Column definitions
        List<TrackSizingFunction> cols = new ArrayList<>();
        for (int i = 0; i < columns; i++) {
            cols.add(TrackSizingFunction.minmax(
                TrackSizingFunction.fixed(80f),
                TrackSizingFunction.fr(1f)
            ));
        }
        tableStyle.gridTemplateColumns = cols;

        // All cells
        int cellCount = rows * columns;
        NodeId[] cells = new NodeId[cellCount];
        for (int i = 0; i < cellCount; i++) {
            Style cellStyle = new Style();
            cellStyle.padding = Rect.all(LengthPercentage.length(8f));
            cellStyle.minSize = new Size<>(Dimension.AUTO, Dimension.length(40f));
            cells[i] = tree.newLeaf(cellStyle);
        }

        return tree.newWithChildren(tableStyle, cells);
    }

    private static NodeId buildMobileApp(TaffyTree tree, int contentItems, MeasureFunc measureFunc) {
        Style appStyle = new Style();
        appStyle.display = Display.FLEX;
        appStyle.flexDirection = FlexDirection.COLUMN;
        appStyle.size = new Size<>(Dimension.percent(1f), Dimension.percent(1f));

        // Navigation bar
        Style navStyle = new Style();
        navStyle.display = Display.FLEX;
        navStyle.flexDirection = FlexDirection.ROW;
        navStyle.justifyContent = AlignContent.SPACE_BETWEEN;
        navStyle.alignItems = AlignItems.CENTER;
        navStyle.size = new Size<>(Dimension.percent(1f), Dimension.length(44f));
        navStyle.padding = Rect.of(
            LengthPercentage.length(16f),
            LengthPercentage.length(16f),
            LengthPercentage.length(8f),
            LengthPercentage.length(8f)
        );

        NodeId backBtn = tree.newLeaf(createBoxStyle(24f, 24f));
        NodeId title = tree.newLeaf(createBoxStyle(Float.NaN, 24f));
        NodeId menuBtn = tree.newLeaf(createBoxStyle(24f, 24f));
        NodeId navBar = tree.newWithChildren(navStyle, new NodeId[]{backBtn, title, menuBtn});

        // Content area
        Style contentStyle = new Style();
        contentStyle.display = Display.FLEX;
        contentStyle.flexDirection = FlexDirection.COLUMN;
        contentStyle.flexGrow = 1f;
        contentStyle.padding = Rect.all(LengthPercentage.length(16f));
        contentStyle.gap = new Size<>(LengthPercentage.length(0f), LengthPercentage.length(16f));

        Random rng = new Random(SEED);
        NodeId[] items = new NodeId[contentItems];
        for (int i = 0; i < contentItems; i++) {
            items[i] = buildMobileContentItem(tree, rng, measureFunc);
        }
        NodeId content = tree.newWithChildren(contentStyle, items);

        // Tab bar
        Style tabBarStyle = new Style();
        tabBarStyle.display = Display.FLEX;
        tabBarStyle.flexDirection = FlexDirection.ROW;
        tabBarStyle.justifyContent = AlignContent.SPACE_AROUND;
        tabBarStyle.alignItems = AlignItems.CENTER;
        tabBarStyle.size = new Size<>(Dimension.percent(1f), Dimension.length(49f));
        tabBarStyle.padding = Rect.of(
            LengthPercentage.length(8f),
            LengthPercentage.length(8f),
            LengthPercentage.length(8f),
            LengthPercentage.length(8f)
        );

        NodeId[] tabs = new NodeId[5];
        for (int i = 0; i < 5; i++) {
            tabs[i] = tree.newLeaf(createBoxStyle(48f, 32f));
        }
        NodeId tabBar = tree.newWithChildren(tabBarStyle, tabs);

        return tree.newWithChildren(appStyle, new NodeId[]{navBar, content, tabBar});
    }

    private static NodeId buildMobileContentItem(TaffyTree tree, Random rng, MeasureFunc measureFunc) {
        Style itemStyle = new Style();
        itemStyle.display = Display.FLEX;
        itemStyle.flexDirection = FlexDirection.COLUMN;
        itemStyle.padding = Rect.all(LengthPercentage.length(12f));
        itemStyle.gap = new Size<>(LengthPercentage.length(0f), LengthPercentage.length(8f));

        NodeId header = tree.newLeaf(createBoxStyle(Float.NaN, 20f));
        NodeId body = tree.newLeafWithMeasure(new Style(), measureFunc);
        
        Style footerStyle = createFlexRowStyle();
        footerStyle.justifyContent = AlignContent.SPACE_BETWEEN;
        NodeId likes = tree.newLeaf(createBoxStyle(60f, 20f));
        NodeId share = tree.newLeaf(createBoxStyle(60f, 20f));
        NodeId footer = tree.newWithChildren(footerStyle, new NodeId[]{likes, share});

        return tree.newWithChildren(itemStyle, new NodeId[]{header, body, footer});
    }

    // ==================== Helper Methods ====================

    private static Style createBoxStyle(float width, float height) {
        Style style = new Style();
        style.size = new Size<>(
            Float.isNaN(width) ? Dimension.AUTO : Dimension.length(width),
            Float.isNaN(height) ? Dimension.AUTO : Dimension.length(height)
        );
        return style;
    }

    private static Style createFlexRowStyle() {
        Style style = new Style();
        style.display = Display.FLEX;
        style.flexDirection = FlexDirection.ROW;
        style.alignItems = AlignItems.CENTER;
        return style;
    }

    // ==================== Measure Function ====================

    private static final class TextMeasureFunc implements MeasureFunc {
        private static final float CHAR_WIDTH = 8f;
        private static final float LINE_HEIGHT = 20f;
        private static final int[] WORD_LENGTHS = {3, 5, 7, 4, 6, 8, 3, 5, 4, 6, 7, 5, 4, 8, 3, 6};

        @Override
        public FloatSize measure(FloatSize knownDimensions, Size<AvailableSpace> availableSpace) {
            if (!Float.isNaN(knownDimensions.width) && !Float.isNaN(knownDimensions.height)) {
                return new FloatSize(knownDimensions.width, knownDimensions.height);
            }

            // Simulate measuring 5-15 words of text
            int wordCount = 5 + (int)(System.nanoTime() % 11);
            float totalWidth = 0f;
            for (int i = 0; i < wordCount; i++) {
                totalWidth += WORD_LENGTHS[i % WORD_LENGTHS.length] * CHAR_WIDTH;
                if (i < wordCount - 1) totalWidth += CHAR_WIDTH; // space
            }

            float maxWidth = Float.MAX_VALUE;
            if (!Float.isNaN(knownDimensions.width)) {
                maxWidth = knownDimensions.width;
            } else {
                AvailableSpace w = availableSpace.width;
                if (w != null && w.isDefinite()) {
                    maxWidth = w.getValue();
                }
            }

            if (totalWidth <= maxWidth) {
                return new FloatSize(
                    Float.isNaN(knownDimensions.width) ? totalWidth : knownDimensions.width,
                    Float.isNaN(knownDimensions.height) ? LINE_HEIGHT : knownDimensions.height
                );
            }

            // Word wrap
            int lines = (int) Math.ceil(totalWidth / maxWidth);
            return new FloatSize(
                Float.isNaN(knownDimensions.width) ? maxWidth : knownDimensions.width,
                Float.isNaN(knownDimensions.height) ? lines * LINE_HEIGHT : knownDimensions.height
            );
        }
    }
}
