package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.Layout;
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
 * LDLib2-shaped UI benchmarks.
 *
 * <p>Unlike {@link LdlibUiBenchmark}, which uses a generic panel/label/button tree, every tree in
 * this class is derived from an actual LDLib2 widget path, and every operation mirrors one step of
 * the per-frame layout loop:</p>
 *
 * <ul>
 *   <li>{@code ModularUI.calculateStyleAndLayout} (ModularUI.java:616-650) runs
 *       {@code styleEngine.calculateStyle()} until the style engine is clean, calls
 *       {@code taffyTree.computeLayout(root, layoutWidth, layoutHeight)} while the taffy root is
 *       dirty, and then dispatches {@code onLayoutChanged} to the nodes with new layout.</li>
 *   <li>{@code ModularUI} constructs its tree with {@code taffyTree.disableRounding()} (ModularUI.java:211),
 *       so every tree in this class disables rounding as well.</li>
 *   <li>{@code UIElement.markTaffyStyleDirty} (UIElement.java:342) and
 *       {@code UIElement.onStyleChanged} (UIElement.java:1173) are what a hover or animation
 *       touches: one element's style changes, the ancestor chain is marked dirty, and the next
 *       {@code computeLayout} recomputes only the dirty path.</li>
 *   <li>{@code TextElement.recompute} (TextElement.java:200-224) measures the formatted text and
 *       writes the measured width/height back into the layout style ({@code adaptiveWidth} /
 *       {@code adaptiveHeight}), i.e. LDLib2 labels are measured leaves whose second layout pass
 *       re-runs the tree. {@link GlyphTextMeasure} is the deterministic Java stand-in for that
 *       measurement: a fixed glyph advance, a fixed line height and at most two lines.</li>
 * </ul>
 *
 * <p>The four widget shapes are the ones that dominate real LDLib2 screens:</p>
 * <ol>
 *   <li><b>form</b>: a block page of panels, each panel a block box with padding and a border that
 *       stacks {@code flex row}s of measured labels;</li>
 *   <li><b>dashboard</b>: a {@code display: grid} card wall
 *       (the same display as {@code LayoutPanel}, LayoutPanel.java:26-28) whose cards are flex
 *       columns with measured labels and an absolutely positioned badge
 *       (the pattern of the tooltip in WindowTooltipBoundsScenario.java:161-168);</li>
 *   <li><b>scroll</b>: the {@code ScrollerView} structure (ScrollerView.java:155-190): a clipped
 *       viewport with {@code overflow: scroll} wrapped three levels deep around a 5,000-row
 *       content list;</li>
 *   <li><b>inventory</b>: the {@code InventorySlots} 9-column slot rows (InventorySlots.java:28-45)
 *       with 18x18 {@code ItemSlot}s (ItemSlot.java:172-173), a hover ring and an absolutely
 *       positioned tooltip that follows the cursor.</li>
 * </ol>
 *
 * <p>Operations are the ones LDLib2 performs per frame: a cold layout (style clean, taffy dirty),
 * a single-leaf style change (hover/animation), a viewport resize ({@code setScreenSize}),
 * a child insertion/removal (list virtualization), and the measured-text cold/warm pair.</p>
 *
 * <p>Determinism: text lengths and glyph metrics are fixed integers and floats, there is no clock,
 * no randomness and no string formatting inside the measured methods, so a baseline and a candidate
 * checkout always build structurally identical trees and request identical layouts.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 1, time = 1)
@Fork(1)
public class Ldlib2UiBenchmark {

    // === Deterministic text metrics, matching the defaults of a 9px LDLib2 Label ===

    /** Glyph advance used by {@link GlyphTextMeasure}. */
    static final float GLYPH_WIDTH = 7f;

    /** Line height used by {@link GlyphTextMeasure}; {@code Label} defaults to a 9px height (Label.java:30). */
    static final float LINE_HEIGHT = 9f;

    /** Maximum number of text lines a label renders before it truncates. */
    static final int MAX_TEXT_LINES = 2;

    /** Form shape: 100 rows per panel, 3 measured labels per row (the documented form shape). */
    static final int FORM_ROWS_PER_PANEL = 100;
    static final int FORM_LABELS_PER_ROW = 3;
    static final int FORM_NODES_PER_PANEL = 1 + FORM_ROWS_PER_PANEL * (1 + FORM_LABELS_PER_ROW);

    /** Form viewport: a 1920x1080 Minecraft window at GUI scale 1, resized to 1600 in the resize op. */
    static final float FORM_WIDTH = 1920f;
    static final float FORM_HEIGHT = 1080f;
    static final float FORM_RESIZED_WIDTH = 1600f;

    /** Dashboard viewport, resized to 1024 in the resize op. */
    static final float DASHBOARD_WIDTH = 1280f;
    static final float DASHBOARD_HEIGHT = 720f;
    static final float DASHBOARD_RESIZED_WIDTH = 1024f;

    /** Scroll viewport, resized to 1100 in the resize op. */
    static final float SCROLL_WIDTH = 900f;
    static final float SCROLL_HEIGHT = 600f;
    static final float SCROLL_RESIZED_WIDTH = 1100f;

    /** Inventory viewport, resized to 480 in the resize op. */
    static final float INVENTORY_WIDTH = 400f;
    static final float INVENTORY_HEIGHT = 400f;
    static final float INVENTORY_RESIZED_WIDTH = 480f;

    /** Number of nodes whose layout output a benchmark reads back (upstream reads the root). */
    static final int LAYOUT_SAMPLES = 96;

    // ===================================================================================== helpers

    static TaffySize<AvailableSpace> space(float width, float height) {
        return TaffySize.of(AvailableSpace.definite(width), AvailableSpace.definite(height));
    }

    static TaffyStyle definitePercentBox(float padding) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.percent(1f));
        style.padding = TaffyRect.all(LengthPercentage.length(padding));
        return style;
    }

    static void border(TaffyStyle style, float width) {
        style.border = TaffyRect.all(LengthPercentage.length(width));
    }

    static TaffyRect<LengthPercentageAuto> inset(float left, float top) {
        return TaffyRect.ltrb(
            LengthPercentageAuto.length(left),
            LengthPercentageAuto.length(top),
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.AUTO
        );
    }

    /** Deterministic text length for label {@code index}: 8..63 characters. */
    static int textLength(int index) {
        return 8 + Math.floorMod(index * 37, 56);
    }

    /**
     * Deterministic glyph-advance measurement standing in for {@code TextElement.recompute}
     * (TextElement.java:200-224) plus {@code Label}'s 9px line height.
     *
     * <p>The rules are deliberately the simplest ones that still make text measurement depend on
     * the width the layout algorithm hands down:</p>
     * <ul>
     *   <li>one glyph is {@link #GLYPH_WIDTH} px wide and one line is {@link #LINE_HEIGHT} px tall;</li>
     *   <li>text wraps at the available width and truncates after {@link #MAX_TEXT_LINES} lines;</li>
     *   <li>a {@code max-content} request returns the single-line width, a {@code min-content}
     *       request returns one glyph;</li>
     *   <li>a fully known size is returned unchanged, exactly like a fixed-size widget.</li>
     * </ul>
     *
     * <p>No clock, no randomness and no string work: only integer/float arithmetic on the
     * precomputed character count.</p>
     */
    static final class GlyphTextMeasure implements MeasureFunc {
        final int chars;
        long calls;

        GlyphTextMeasure(int chars) {
            this.chars = Math.max(1, chars);
        }

        long callCount() {
            return calls;
        }

        void resetCalls() {
            calls = 0L;
        }

        @Override
        public FloatSize measure(FloatSize knownDimensions, TaffySize<AvailableSpace> availableSpace) {
            calls++;

            if (!Float.isNaN(knownDimensions.width) && !Float.isNaN(knownDimensions.height)) {
                return new FloatSize(knownDimensions.width, knownDimensions.height);
            }

            float widthConstraint;
            if (!Float.isNaN(knownDimensions.width)) {
                widthConstraint = knownDimensions.width;
            } else {
                AvailableSpace width = availableSpace.width;
                if (width == null) {
                    widthConstraint = Float.NaN;
                } else if (width.isMinContent()) {
                    widthConstraint = 0f;
                } else if (width.isDefinite()) {
                    widthConstraint = width.getValue();
                } else {
                    widthConstraint = Float.NaN;
                }
            }

            float naturalWidth = chars * GLYPH_WIDTH;
            float width;
            int lines;
            if (Float.isNaN(widthConstraint)) {
                width = naturalWidth;
                lines = 1;
            } else if (widthConstraint <= 0f) {
                width = GLYPH_WIDTH;
                lines = Math.min(MAX_TEXT_LINES, chars);
            } else {
                int charsPerLine = Math.max(1, (int) Math.floor(widthConstraint / GLYPH_WIDTH));
                int wrappedLines = (chars + charsPerLine - 1) / charsPerLine;
                lines = Math.min(MAX_TEXT_LINES, Math.max(1, wrappedLines));
                width = Math.min(naturalWidth, charsPerLine * GLYPH_WIDTH);
            }

            float height = lines * LINE_HEIGHT;
            if (!Float.isNaN(knownDimensions.height)) {
                height = knownDimensions.height;
            }
            return new FloatSize(Float.isNaN(knownDimensions.width) ? width : knownDimensions.width, height);
        }
    }

    /**
     * A trial-built tree plus a pre-computed node list and a bounded sample used to read the
     * layout output back so that {@code computeLayout} cannot be optimised away.
     */
    static class BaseShape {
        final TaffyTree tree;
        final NodeId root;
        final NodeId[] nodes;
        final NodeId[] sample;

        BaseShape(TaffyTree tree, NodeId root) {
            this.tree = tree;
            this.root = root;
            this.nodes = BenchSupport.collectNodes(tree, root);

            int stride = Math.max(1, this.nodes.length / LAYOUT_SAMPLES);
            int count = (this.nodes.length + stride - 1) / stride;
            this.sample = new NodeId[count];
            for (int i = 0, s = 0; i < this.nodes.length; i += stride) {
                this.sample[s++] = this.nodes[i];
            }
        }

        int nodeCount() {
            return nodes.length;
        }

        /** Mirrors upstream's {@code mark_all_dirty}: every node's cache is cleared. */
        void clearAllCaches() {
            BenchSupport.clearCaches(tree, nodes);
        }

        /**
         * Reads the sampled layout boxes and folds them into one float. A real UI consumes every
         * node's layout (to render it); sampling 96 nodes keeps that read-back cheap while still
         * depending on the full layout result.
         */
        float checksum() {
            float sum = 0f;
            for (NodeId node : sample) {
                Layout layout = tree.getLayout(node);
                if (layout == null) {
                    continue;
                }
                sum += layout.location().x * 1.25f
                       + layout.location().y * 1.5f
                       + layout.size().width * 0.75f
                       + layout.size().height;
            }
            return sum;
        }
    }

    // ========================================================================================= form

    /**
     * Form page: block root, block panels with padding/border, flex rows of measured labels.
     *
     * <p>LDLib2 counterpart: a config/menu page whose root element stacks
     * {@code UIElement}s that themselves use {@code display: flex} rows of {@code Label}s. Panels
     * are block boxes so that their auto width stretches to the page content width, which is the
     * same result a {@code width: 100%} flex panel gets after style resolution, but exercises the
     * block algorithm's content sizing instead.</p>
     */
    static final class FormShape extends BaseShape {
        final int panels;
        final NodeId hoveredLabel;
        final TaffyStyle plainStyle;
        final TaffyStyle alternateStyle;
        final NodeId hostPanel;
        final NodeId detachedRow;

        FormShape(
            TaffyTree tree,
            NodeId root,
            int panels,
            NodeId hoveredLabel,
            TaffyStyle plainStyle,
            TaffyStyle alternateStyle,
            NodeId hostPanel,
            NodeId detachedRow
        ) {
            super(tree, root);
            this.panels = panels;
            this.hoveredLabel = hoveredLabel;
            this.plainStyle = plainStyle;
            this.alternateStyle = alternateStyle;
            this.hostPanel = hostPanel;
            this.detachedRow = detachedRow;
        }
    }

    static FormShape buildForm(int targetNodes) {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        int panels = Math.max(1, (targetNodes + FORM_NODES_PER_PANEL - 1) / FORM_NODES_PER_PANEL);

        TaffyStyle rootStyle = definitePercentBox(8f);
        rootStyle.display = TaffyDisplay.BLOCK;

        NodeId[] panelNodes = new NodeId[panels];
        NodeId hoveredLabel = null;
        TaffyStyle plainStyle = null;
        TaffyStyle alternateStyle = null;

        for (int p = 0; p < panels; p++) {
            TaffyStyle panelStyle = new TaffyStyle();
            panelStyle.display = TaffyDisplay.BLOCK;
            panelStyle.padding = TaffyRect.all(LengthPercentage.length(8f));
            panelStyle.border = TaffyRect.all(LengthPercentage.length(2f));

            NodeId[] rows = new NodeId[FORM_ROWS_PER_PANEL];
            for (int r = 0; r < FORM_ROWS_PER_PANEL; r++) {
                TaffyStyle rowStyle = new TaffyStyle();
                rowStyle.display = TaffyDisplay.FLEX;
                rowStyle.flexDirection = FlexDirection.ROW;
                rowStyle.gap = new TaffySize<>(LengthPercentage.length(4f), LengthPercentage.length(4f));
                rowStyle.alignItems = AlignItems.CENTER;

                NodeId[] labels = new NodeId[FORM_LABELS_PER_ROW];
                for (int c = 0; c < FORM_LABELS_PER_ROW; c++) {
                    TaffyStyle labelStyle = new TaffyStyle();
                    labelStyle.flexGrow = 1f;
                    labelStyle.flexShrink = 1f;
                    labelStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
                    int chars = textLength(p * FORM_ROWS_PER_PANEL + r * FORM_LABELS_PER_ROW + c);
                    labels[c] = tree.newLeafWithMeasure(labelStyle, new GlyphTextMeasure(chars));
                    if (p == 0 && r == 0 && c == 1) {
                        hoveredLabel = labels[c];
                        plainStyle = labelStyle;
                        alternateStyle = labelStyle.copy();
                        alternateStyle.padding = TaffyRect.all(LengthPercentage.length(2f));
                    }
                }
                rows[r] = tree.newWithChildren(rowStyle, labels);
            }
            panelNodes[p] = tree.newWithChildren(panelStyle, rows);
        }

        NodeId root = tree.newWithChildren(rootStyle, panelNodes);
        NodeId hostPanel = panelNodes[0];
        NodeId detachedRow = buildFormRow(tree, 3);

        return new FormShape(tree, root, panels, hoveredLabel, plainStyle, alternateStyle, hostPanel, detachedRow);
    }

    static NodeId buildFormRow(TaffyTree tree, int firstIndex) {
        TaffyStyle rowStyle = new TaffyStyle();
        rowStyle.display = TaffyDisplay.FLEX;
        rowStyle.flexDirection = FlexDirection.ROW;
        rowStyle.gap = new TaffySize<>(LengthPercentage.length(4f), LengthPercentage.length(4f));
        rowStyle.alignItems = AlignItems.CENTER;

        NodeId[] labels = new NodeId[FORM_LABELS_PER_ROW];
        for (int c = 0; c < FORM_LABELS_PER_ROW; c++) {
            TaffyStyle labelStyle = new TaffyStyle();
            labelStyle.flexGrow = 1f;
            labelStyle.flexShrink = 1f;
            labelStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
            labels[c] = tree.newLeafWithMeasure(
                labelStyle,
                new GlyphTextMeasure(textLength(firstIndex + c))
            );
        }
        return tree.newWithChildren(rowStyle, labels);
    }

    @State(Scope.Thread)
    public static class FormColdState {
        @Param({"1000", "5000", "20000"})
        public int targetNodes;

        FormShape shape;

        @Setup(Level.Trial)
        public void setupTrial() {
            shape = buildForm(targetNodes);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            shape.clearAllCaches();
        }
    }

    /**
     * Cold form layout.
     *
     * <p>LDLib2: the first frame of a screen, or a frame after the style engine invalidated every
     * node, reaches {@code ModularUI.calculateStyleAndLayout} with a clean style engine and a dirty
     * taffy root (ModularUI.java:616-630), so the whole tree is laid out in one
     * {@code computeLayout}. Rounding is off, as in {@code ModularUI} (ModularUI.java:211).</p>
     */
    @Benchmark
    public void formColdLayout(FormColdState state, Blackhole bh) {
        state.shape.tree.computeLayout(state.shape.root, space(FORM_WIDTH, FORM_HEIGHT));
        bh.consume(state.shape.checksum());
    }

    @State(Scope.Thread)
    public static class FormIncrementalState {
        @Param({"1000", "5000", "20000"})
        public int targetNodes;

        FormShape shape;
        boolean alternate;
        boolean wide;

        @Setup(Level.Trial)
        public void setupTrial() {
            shape = buildForm(targetNodes);
            shape.tree.setStyle(shape.hoveredLabel, shape.plainStyle);
            shape.tree.computeLayout(shape.root, space(FORM_WIDTH, FORM_HEIGHT));
        }
    }

    /**
     * One hovered label changes its style, then the frame is laid out.
     *
     * <p>LDLib2: a hover/animation writes one style value on the hovered element;
     * {@code UIElement.onStyleChanged} (UIElement.java:1173) leads to
     * {@code markTaffyStyleDirty} (UIElement.java:342), the ancestor chain becomes dirty, and the
     * {@code calculateStyleAndLayout} loop (ModularUI.java:618-650) lays the tree out again. The
     * other 20k nodes must answer from their caches.</p>
     */
    @Benchmark
    public void formPerFrameStyleChange(FormIncrementalState state, Blackhole bh) {
        state.alternate = !state.alternate;
        state.shape.tree.setStyle(
            state.shape.hoveredLabel,
            state.alternate ? state.shape.alternateStyle : state.shape.plainStyle
        );
        state.shape.tree.computeLayout(state.shape.root, space(FORM_WIDTH, FORM_HEIGHT));
        bh.consume(state.shape.checksum());
    }

    /**
     * The window is resized and the page reflows.
     *
     * <p>LDLib2: {@code ModularUI.setScreenSize}-style resize recomputes the root available space
     * from the root element's percent style and calls {@code calculateStyleAndLayout} again
     * (ModularUI.java:561-595). Percent-sized subtrees re-resolve; fixed nodes keep their caches.</p>
     */
    @Benchmark
    public void formResizeRoot(FormIncrementalState state, Blackhole bh) {
        state.wide = !state.wide;
        float width = state.wide ? FORM_WIDTH : FORM_RESIZED_WIDTH;
        state.shape.tree.computeLayout(state.shape.root, space(width, FORM_HEIGHT));
        bh.consume(state.shape.checksum());
    }

    /**
     * A row is inserted into and removed from a panel, with one layout in between.
     *
     * <p>LDLib2: list-style widgets attach a freshly built element row to a panel
     * ({@code UIElement.addChild} mirrors the child list into {@code TaffyTree.addChild},
     * UIElement.java:867-911), let the frame layout, and detach it when it scrolls away.</p>
     */
    @Benchmark
    public void formAddRemoveChild(FormIncrementalState state, Blackhole bh) {
        state.shape.tree.addChild(state.shape.hostPanel, state.shape.detachedRow);
        state.shape.tree.computeLayout(state.shape.root, space(FORM_WIDTH, FORM_HEIGHT));
        bh.consume(state.shape.checksum());
        state.shape.tree.removeChild(state.shape.hostPanel, state.shape.detachedRow);
    }

    // ==================================================================================== dashboard

    /**
     * Dashboard card wall: {@code display: grid} with equal 1fr columns, flex-column cards with
     * measured labels, and an absolutely positioned badge per card.
     *
     * <p>LDLib2 counterpart: a {@code LayoutPanel}-style grid (LayoutPanel.java:26-28 uses
     * {@code display: GRID} with a fixed/1fr track list) filled with card elements, each carrying
     * an out-of-flow badge exactly like the absolutely positioned tooltip target in
     * WindowTooltipBoundsScenario.java:161-168. Out-of-flow children must not contribute to track
     * sizing, which is the grid behaviour this shape stresses.</p>
     */
    static final class DashboardShape extends BaseShape {
        final int side;
        final NodeId badge;
        final TaffyStyle plainStyle;
        final TaffyStyle alternateStyle;
        final NodeId hostGrid;
        final NodeId detachedCard;

        DashboardShape(
            TaffyTree tree,
            NodeId root,
            int side,
            NodeId badge,
            TaffyStyle plainStyle,
            TaffyStyle alternateStyle,
            NodeId hostGrid,
            NodeId detachedCard
        ) {
            super(tree, root);
            this.side = side;
            this.badge = badge;
            this.plainStyle = plainStyle;
            this.alternateStyle = alternateStyle;
            this.hostGrid = hostGrid;
            this.detachedCard = detachedCard;
        }
    }

    static DashboardShape buildDashboard(int side) {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle rootStyle = definitePercentBox(8f);
        rootStyle.display = TaffyDisplay.GRID;
        rootStyle.gridTemplateColumns = BenchSupport.frTracks(side);
        rootStyle.gridAutoRows = List.of(TrackSizingFunction.minmax(
            TrackSizingFunction.fixed(56f),
            TrackSizingFunction.auto()
        ));
        rootStyle.gap = new TaffySize<>(LengthPercentage.length(6f), LengthPercentage.length(6f));

        int cards = side * side;
        NodeId[] cardNodes = new NodeId[cards];
        NodeId firstBadge = null;

        for (int i = 0; i < cards; i++) {
            NodeId title = tree.newLeafWithMeasure(
                cardLabelStyle(),
                new GlyphTextMeasure(14 + (i % 11))
            );
            NodeId subtitle = tree.newLeafWithMeasure(
                cardLabelStyle(),
                new GlyphTextMeasure(26 + (i % 17))
            );
            NodeId badge = tree.newLeaf(badgeStyle(24f));
            if (i == 0) {
                firstBadge = badge;
            }
            cardNodes[i] = tree.newWithChildren(cardStyle(), new NodeId[]{title, subtitle, badge});
        }

        // The badge of the first card is the per-frame animated leaf. Both styles come from the
        // same factory so only the badge width differs between them.
        TaffyStyle plainStyle = badgeStyle(24f);
        TaffyStyle alternateStyle = badgeStyle(30f);
        if (firstBadge != null) {
            tree.setStyle(firstBadge, plainStyle);
        }

        NodeId root = tree.newWithChildren(rootStyle, cardNodes);
        NodeId detachedCard = buildDetachedCard(tree);

        return new DashboardShape(tree, root, side, firstBadge, plainStyle, alternateStyle, root, detachedCard);
    }

    private static TaffyStyle cardLabelStyle() {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
        return style;
    }

    private static TaffyStyle cardStyle() {
        TaffyStyle cardStyle = new TaffyStyle();
        cardStyle.display = TaffyDisplay.FLEX;
        cardStyle.flexDirection = FlexDirection.COLUMN;
        cardStyle.padding = TaffyRect.all(LengthPercentage.length(6f));
        cardStyle.gap = new TaffySize<>(LengthPercentage.length(4f), LengthPercentage.length(4f));
        cardStyle.alignItems = AlignItems.STRETCH;
        return cardStyle;
    }

    private static TaffyStyle badgeStyle(float width) {
        TaffyStyle style = new TaffyStyle();
        style.position = TaffyPosition.ABSOLUTE;
        style.inset = TaffyRect.ltrb(
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.length(4f),
            LengthPercentageAuto.length(4f),
            LengthPercentageAuto.AUTO
        );
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(12f));
        return style;
    }

    private static NodeId buildDetachedCard(TaffyTree tree) {
        TaffyStyle titleStyle = new TaffyStyle();
        titleStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
        NodeId title = tree.newLeafWithMeasure(titleStyle, new GlyphTextMeasure(18));

        TaffyStyle subtitleStyle = new TaffyStyle();
        subtitleStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
        NodeId subtitle = tree.newLeafWithMeasure(subtitleStyle, new GlyphTextMeasure(34));

        NodeId badge = tree.newLeaf(badgeStyle(24f));
        return tree.newWithChildren(cardStyle(), new NodeId[]{title, subtitle, badge});
    }

    @State(Scope.Thread)
    public static class DashboardColdState {
        @Param({"8", "16"})
        public int side;

        DashboardShape shape;

        @Setup(Level.Trial)
        public void setupTrial() {
            shape = buildDashboard(side);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            shape.clearAllCaches();
        }
    }

    /**
     * Cold dashboard layout.
     *
     * <p>LDLib2: the first frame of a grid screen — {@code LayoutPanel}-style
     * {@code display: GRID} track sizing (LayoutPanel.java:26-28) plus out-of-flow badges that must
     * stay out of track sizing (WindowTooltipBoundsScenario.java:161-168).</p>
     */
    @Benchmark
    public void dashboardColdLayout(DashboardColdState state, Blackhole bh) {
        state.shape.tree.computeLayout(
            state.shape.root,
            space(DASHBOARD_WIDTH, DASHBOARD_HEIGHT)
        );
        bh.consume(state.shape.checksum());
    }

    @State(Scope.Thread)
    public static class DashboardIncrementalState {
        @Param({"8", "16"})
        public int side;

        DashboardShape shape;
        boolean alternate;
        boolean wide;

        @Setup(Level.Trial)
        public void setupTrial() {
            shape = buildDashboard(side);
            shape.tree.setStyle(shape.badge, shape.plainStyle);
            shape.tree.computeLayout(shape.root, space(DASHBOARD_WIDTH, DASHBOARD_HEIGHT));
        }
    }

    /**
     * An animated badge on one card changes width, then the frame is laid out.
     *
     * <p>LDLib2: a hover/status animation on one element updates its style
     * ({@code UIElement.onStyleChanged} → {@code markTaffyStyleDirty}, UIElement.java:1173/342).
     * The badge is out of flow, so no grid track changes — this isolates the dirty-ancestor walk
     * plus the cache checks that a per-frame animation costs when geometry barely moves.</p>
     */
    @Benchmark
    public void dashboardPerFrameStyleChange(DashboardIncrementalState state, Blackhole bh) {
        state.alternate = !state.alternate;
        state.shape.tree.setStyle(
            state.shape.badge,
            state.alternate ? state.shape.alternateStyle : state.shape.plainStyle
        );
        state.shape.tree.computeLayout(state.shape.root, space(DASHBOARD_WIDTH, DASHBOARD_HEIGHT));
        bh.consume(state.shape.checksum());
    }

    /**
     * The dashboard viewport is resized, so every {@code 1fr} track is re-sized.
     *
     * <p>LDLib2: a window resize calls back into {@code calculateStyleAndLayout}
     * (ModularUI.java:561-595); {@code fr} tracks have to be re-resolved against the new width.</p>
     */
    @Benchmark
    public void dashboardResizeRoot(DashboardIncrementalState state, Blackhole bh) {
        state.wide = !state.wide;
        float width = state.wide ? DASHBOARD_WIDTH : DASHBOARD_RESIZED_WIDTH;
        state.shape.tree.computeLayout(state.shape.root, space(width, DASHBOARD_HEIGHT));
        bh.consume(state.shape.checksum());
    }

    /**
     * A card is added to / removed from the grid and one layout runs.
     *
     * <p>LDLib2: an item grid that attaches a newly created card element
     * ({@code UIElement.addChild}, UIElement.java:867-911). The added card becomes a new grid item,
     * so implicit row sizing is recomputed for the affected row.</p>
     */
    @Benchmark
    public void dashboardAddRemoveChild(DashboardIncrementalState state, Blackhole bh) {
        state.shape.tree.addChild(state.shape.hostGrid, state.shape.detachedCard);
        state.shape.tree.computeLayout(state.shape.root, space(DASHBOARD_WIDTH, DASHBOARD_HEIGHT));
        bh.consume(state.shape.checksum());
        state.shape.tree.removeChild(state.shape.hostGrid, state.shape.detachedCard);
    }

    // ======================================================================================= scroll

    /**
     * Nested scroll viewports with a 5,000-row content list.
     *
     * <p>LDLib2 counterpart: {@code ScrollerView} (ScrollerView.java:155-190) builds
     * {@code verticalContainer → viewPort (overflow hidden, padding 5) → wrapper → viewContainer}
     * and adds scrollbars as siblings; {@code updateScrollers} (ScrollerView.java:273-330) then
     * reads the content/port sizes and rewrites scrollbar styles. This shape nests that structure
     * three levels deep, which is what a "tree list inside a tab inside a panel" screen does, and
     * puts 5,000 fixed-height rows in the innermost view container.</p>
     */
    static final class ScrollShape extends BaseShape {
        final int rows;
        final NodeId viewContainer;
        final TaffyStyle plainStyle;
        final TaffyStyle alternateStyle;
        final NodeId hostView;
        final NodeId detachedRow;

        ScrollShape(
            TaffyTree tree,
            NodeId root,
            int rows,
            NodeId viewContainer,
            TaffyStyle plainStyle,
            TaffyStyle alternateStyle,
            NodeId hostView,
            NodeId detachedRow
        ) {
            super(tree, root);
            this.rows = rows;
            this.viewContainer = viewContainer;
            this.plainStyle = plainStyle;
            this.alternateStyle = alternateStyle;
            this.hostView = hostView;
            this.detachedRow = detachedRow;
        }
    }

    static ScrollShape buildScroll(int rows) {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle rootStyle = definitePercentBox(5f);
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.COLUMN;
        BenchSupport.overflow(rootStyle, Overflow.SCROLL, Overflow.SCROLL);
        rootStyle.scrollbarWidth = 8f;

        // The outer view container; ScrollerView applies the scroll offset to exactly this node
        // (ScrollerView.java:201-210: viewContainer.layout(layout -> layout.top(-value * range))).
        TaffyStyle outerViewStyle = new TaffyStyle();
        outerViewStyle.display = TaffyDisplay.FLEX;
        outerViewStyle.flexDirection = FlexDirection.COLUMN;
        outerViewStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
        outerViewStyle.gap = new TaffySize<>(LengthPercentage.length(4f), LengthPercentage.length(4f));

        TaffyStyle plainStyle = outerViewStyle.copy();
        TaffyStyle alternateStyle = outerViewStyle.copy();
        alternateStyle.inset = TaffyRect.ltrb(
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.length(-120f),
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.AUTO
        );

        TaffyStyle innerViewStyle = new TaffyStyle();
        innerViewStyle.display = TaffyDisplay.FLEX;
        innerViewStyle.flexDirection = FlexDirection.COLUMN;
        innerViewStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);

        NodeId[] rowNodes = new NodeId[rows];
        TaffyStyle rowStyle = new TaffyStyle();
        rowStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.length(12f));
        for (int i = 0; i < rows; i++) {
            rowNodes[i] = tree.newLeaf(rowStyle);
        }
        NodeId innerView = tree.newWithChildren(innerViewStyle, rowNodes);

        NodeId innerScroll = tree.newWithChildren(scrollContainerStyle(240f, 6f, 4f), new NodeId[]{innerView});

        TaffyStyle midViewStyle = new TaffyStyle();
        midViewStyle.display = TaffyDisplay.FLEX;
        midViewStyle.flexDirection = FlexDirection.COLUMN;
        midViewStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
        midViewStyle.gap = new TaffySize<>(LengthPercentage.length(2f), LengthPercentage.length(2f));
        NodeId midView = tree.newWithChildren(midViewStyle, new NodeId[]{innerScroll});

        NodeId midScroll = tree.newWithChildren(scrollContainerStyle(380f, 6f, 4f), new NodeId[]{midView});

        NodeId outerView = tree.newWithChildren(outerViewStyle, new NodeId[]{midScroll});
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{outerView});

        NodeId detachedRow = buildScrollRow(tree);

        return new ScrollShape(
            tree,
            root,
            rows,
            outerView,
            plainStyle,
            alternateStyle,
            innerView,
            detachedRow
        );
    }

    private static TaffyStyle scrollContainerStyle(float height, float scrollbarWidth, float padding) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.COLUMN;
        style.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.length(height));
        style.padding = TaffyRect.all(LengthPercentage.length(padding));
        BenchSupport.overflow(style, Overflow.SCROLL, Overflow.SCROLL);
        style.scrollbarWidth = scrollbarWidth;
        return style;
    }

    private static NodeId buildScrollRow(TaffyTree tree) {
        TaffyStyle rowStyle = new TaffyStyle();
        rowStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.length(12f));
        return tree.newLeaf(rowStyle);
    }

    @State(Scope.Thread)
    public static class ScrollColdState {
        @Param({"5000"})
        public int contentRows;

        ScrollShape shape;

        @Setup(Level.Trial)
        public void setupTrial() {
            shape = buildScroll(contentRows);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            shape.clearAllCaches();
        }
    }

    /**
     * Cold nested-scroll layout.
     *
     * <p>LDLib2: the first frame of a {@code ScrollerView} screen — three clipped viewports with
     * {@code overflow: scroll} (ScrollerView.java:173) and 5,000 rows inside the innermost
     * container. This is where scrollable-overflow computation, scrollbar reservation and the
     * nesting of scroll containers show up.</p>
     */
    @Benchmark
    public void scrollColdLayout(ScrollColdState state, Blackhole bh) {
        state.shape.tree.computeLayout(state.shape.root, space(SCROLL_WIDTH, SCROLL_HEIGHT));
        bh.consume(state.shape.checksum());
    }

    @State(Scope.Thread)
    public static class ScrollIncrementalState {
        @Param({"5000"})
        public int contentRows;

        ScrollShape shape;
        boolean alternate;
        boolean wide;

        @Setup(Level.Trial)
        public void setupTrial() {
            shape = buildScroll(contentRows);
            shape.tree.setStyle(shape.viewContainer, shape.plainStyle);
            shape.tree.computeLayout(shape.root, space(SCROLL_WIDTH, SCROLL_HEIGHT));
        }
    }

    /**
     * A wheel/scroller event moves the view container, then the frame is laid out.
     *
     * <p>LDLib2: {@code ScrollerView.onVerticalScroll} writes
     * {@code viewContainer.layout(layout -> layout.top(-value * range))}
     * (ScrollerView.java:201-210). The style change is a relative inset, so the node moves without
     * reflowing its siblings — the common per-frame cost of scrolling.</p>
     */
    @Benchmark
    public void scrollPerFrameStyleChange(ScrollIncrementalState state, Blackhole bh) {
        state.alternate = !state.alternate;
        state.shape.tree.setStyle(
            state.shape.viewContainer,
            state.alternate ? state.shape.alternateStyle : state.shape.plainStyle
        );
        state.shape.tree.computeLayout(state.shape.root, space(SCROLL_WIDTH, SCROLL_HEIGHT));
        bh.consume(state.shape.checksum());
    }

    /**
     * The scroll viewport is resized.
     *
     * <p>LDLib2: a window resize re-runs {@code calculateStyleAndLayout} (ModularUI.java:561-595);
     * the viewport is percent-sized, so the scroll containers and the percent-width rows inside
     * them re-resolve while the fixed-height rows keep their subtree caches.</p>
     */
    @Benchmark
    public void scrollResizeRoot(ScrollIncrementalState state, Blackhole bh) {
        state.wide = !state.wide;
        float width = state.wide ? SCROLL_WIDTH : SCROLL_RESIZED_WIDTH;
        state.shape.tree.computeLayout(state.shape.root, space(width, SCROLL_HEIGHT));
        bh.consume(state.shape.checksum());
    }

    /**
     * One row is appended to / removed from the 5,000-row list, with one layout in between.
     *
     * <p>LDLib2: list virtualization appends a row element to the view container
     * ({@code UIElement.addChild} → {@code TaffyTree.addChild}, UIElement.java:867-911) and removes
     * it when it leaves the window.</p>
     */
    @Benchmark
    public void scrollAddRemoveChild(ScrollIncrementalState state, Blackhole bh) {
        state.shape.tree.addChild(state.shape.hostView, state.shape.detachedRow);
        state.shape.tree.computeLayout(state.shape.root, space(SCROLL_WIDTH, SCROLL_HEIGHT));
        bh.consume(state.shape.checksum());
        state.shape.tree.removeChild(state.shape.hostView, state.shape.detachedRow);
    }

    // ==================================================================================== inventory

    /**
     * Inventory screen: a 9x6 slot grid with hover highlight and an absolutely positioned tooltip.
     *
     * <p>LDLib2 counterpart: {@code InventorySlots} (InventorySlots.java:28-45) builds one
     * {@code Row} per 9-slot line and adds a hotbar row; each {@code ItemSlot} is a fixed 18x18
     * element (ItemSlot.java:172-173). The tooltip is the out-of-flow overlay pattern from
     * WindowTooltipBoundsScenario.java:161-168: {@code position: absolute}, fixed inset, fixed
     * size, never contributing to the slot row.</p>
     */
    static final class InventoryShape extends BaseShape {
        final int rowCount;
        final int columns;
        final NodeId hoveredSlot;
        final TaffyStyle plainStyle;
        final TaffyStyle alternateStyle;
        final NodeId tooltip;
        final TaffyStyle tooltipPlainStyle;
        final TaffyStyle tooltipAlternateStyle;
        final NodeId hostRow;
        final NodeId detachedSlot;

        InventoryShape(
            TaffyTree tree,
            NodeId root,
            int rowCount,
            int columns,
            NodeId hoveredSlot,
            TaffyStyle plainStyle,
            TaffyStyle alternateStyle,
            NodeId tooltip,
            TaffyStyle tooltipPlainStyle,
            TaffyStyle tooltipAlternateStyle,
            NodeId hostRow,
            NodeId detachedSlot
        ) {
            super(tree, root);
            this.rowCount = rowCount;
            this.columns = columns;
            this.hoveredSlot = hoveredSlot;
            this.plainStyle = plainStyle;
            this.alternateStyle = alternateStyle;
            this.tooltip = tooltip;
            this.tooltipPlainStyle = tooltipPlainStyle;
            this.tooltipAlternateStyle = tooltipAlternateStyle;
            this.hostRow = hostRow;
            this.detachedSlot = detachedSlot;
        }
    }

    static final int INVENTORY_ROWS = 6;
    static final int INVENTORY_COLUMNS = 9;
    static final int INVENTORY_HOVER_ROW = 2;
    static final int INVENTORY_HOVER_COLUMN = 4;

    static InventoryShape buildInventory() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle rootStyle = definitePercentBox(12f);
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.COLUMN;
        rootStyle.gap = new TaffySize<>(LengthPercentage.length(4f), LengthPercentage.length(4f));

        NodeId[] rows = new NodeId[INVENTORY_ROWS];
        NodeId hoveredSlot = null;
        TaffyStyle plainStyle = null;
        TaffyStyle alternateStyle = null;

        for (int r = 0; r < INVENTORY_ROWS; r++) {
            TaffyStyle rowStyle = new TaffyStyle();
            rowStyle.display = TaffyDisplay.FLEX;
            rowStyle.flexDirection = FlexDirection.ROW;
            rowStyle.justifyContent = dev.vfyjxf.taffy.style.AlignContent.CENTER;
            rowStyle.gap = new TaffySize<>(LengthPercentage.length(2f), LengthPercentage.length(2f));

            NodeId[] slots = new NodeId[INVENTORY_COLUMNS];
            for (int c = 0; c < INVENTORY_COLUMNS; c++) {
                TaffyStyle slotStyle = slotStyle(1f);
                slots[c] = tree.newLeaf(slotStyle);
                if (r == INVENTORY_HOVER_ROW && c == INVENTORY_HOVER_COLUMN) {
                    hoveredSlot = slots[c];
                    plainStyle = slotStyle;
                    alternateStyle = slotStyle(3f);
                }
            }
            rows[r] = tree.newWithChildren(rowStyle, slots);
        }

        NodeId tooltip = buildTooltip(tree);
        TaffyStyle tooltipPlainStyle = tooltipStyle(230f, 70f);
        TaffyStyle tooltipAlternateStyle = tooltipStyle(234f, 74f);
        tree.setStyle(tooltip, tooltipPlainStyle);

        NodeId root = tree.newWithChildren(rootStyle, concat(rows, tooltip));
        NodeId hostRow = rows[INVENTORY_ROWS - 1];
        NodeId detachedSlot = tree.newLeaf(slotStyle(1f));

        return new InventoryShape(
            tree,
            root,
            INVENTORY_ROWS,
            INVENTORY_COLUMNS,
            hoveredSlot,
            plainStyle,
            alternateStyle,
            tooltip,
            tooltipPlainStyle,
            tooltipAlternateStyle,
            hostRow,
            detachedSlot
        );
    }

    private static TaffyStyle slotStyle(float borderWidth) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(18f), TaffyDimension.length(18f));
        style.border = TaffyRect.all(LengthPercentage.length(borderWidth));
        return style;
    }

    private static NodeId buildTooltip(TaffyTree tree) {
        TaffyStyle lineStyle = new TaffyStyle();
        lineStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
        NodeId lineOne = tree.newLeafWithMeasure(lineStyle, new GlyphTextMeasure(21));

        TaffyStyle lineTwoStyle = new TaffyStyle();
        lineTwoStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
        NodeId lineTwo = tree.newLeafWithMeasure(lineTwoStyle, new GlyphTextMeasure(30));

        return tree.newWithChildren(tooltipStyle(230f, 70f), new NodeId[]{lineOne, lineTwo});
    }

    private static TaffyStyle tooltipStyle(float left, float top) {
        TaffyStyle style = new TaffyStyle();
        style.position = TaffyPosition.ABSOLUTE;
        style.inset = inset(left, top);
        style.size = new TaffySize<>(TaffyDimension.length(140f), TaffyDimension.length(48f));
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.COLUMN;
        style.padding = TaffyRect.all(LengthPercentage.length(4f));
        style.gap = new TaffySize<>(LengthPercentage.length(2f), LengthPercentage.length(2f));
        return style;
    }

    private static NodeId[] concat(NodeId[] rows, NodeId extra) {
        NodeId[] all = new NodeId[rows.length + 1];
        System.arraycopy(rows, 0, all, 0, rows.length);
        all[rows.length] = extra;
        return all;
    }

    @State(Scope.Thread)
    public static class InventoryColdState {
        InventoryShape shape;

        @Setup(Level.Trial)
        public void setupTrial() {
            shape = buildInventory();
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            shape.clearAllCaches();
        }
    }

    /**
     * Cold inventory layout.
     *
     * <p>LDLib2: the first frame of a container screen — six 9-slot rows
     * (InventorySlots.java:28-45) of fixed 18x18 slots (ItemSlot.java:172-173) plus the
     * out-of-flow tooltip overlay (WindowTooltipBoundsScenario.java:161-168).</p>
     */
    @Benchmark
    public void inventoryColdLayout(InventoryColdState state, Blackhole bh) {
        state.shape.tree.computeLayout(state.shape.root, space(INVENTORY_WIDTH, INVENTORY_HEIGHT));
        bh.consume(state.shape.checksum());
    }

    @State(Scope.Thread)
    public static class InventoryIncrementalState {
        InventoryShape shape;
        boolean alternate;
        boolean tooltipMoved;
        boolean wide;

        @Setup(Level.Trial)
        public void setupTrial() {
            shape = buildInventory();
            shape.tree.setStyle(shape.hoveredSlot, shape.plainStyle);
            shape.tree.setStyle(shape.tooltip, shape.tooltipPlainStyle);
            shape.tree.computeLayout(shape.root, space(INVENTORY_WIDTH, INVENTORY_HEIGHT));
        }
    }

    /**
     * The hovered slot's highlight style changes, then the frame is laid out.
     *
     * <p>LDLib2: a slot hover updates that element's style
     * ({@code UIElement.onStyleChanged} → {@code markTaffyStyleDirty}, UIElement.java:1173/342).
     * The slot keeps its 18x18 border box, so the row geometry does not move; this isolates the
     * dirty-ancestor walk and the cache checks that a hover costs in {@code calculateStyleAndLayout}
     * (ModularUI.java:618-650).</p>
     */
    @Benchmark
    public void inventoryPerFrameStyleChange(InventoryIncrementalState state, Blackhole bh) {
        state.alternate = !state.alternate;
        state.shape.tree.setStyle(
            state.shape.hoveredSlot,
            state.alternate ? state.shape.alternateStyle : state.shape.plainStyle
        );
        state.shape.tree.computeLayout(state.shape.root, space(INVENTORY_WIDTH, INVENTORY_HEIGHT));
        bh.consume(state.shape.checksum());
    }

    /**
     * The tooltip follows the cursor: only its absolute inset changes, then the frame is laid out.
     *
     * <p>LDLib2: the tooltip overlay is positioned with
     * {@code layout.positionType(ABSOLUTE).leftPercent(...).topPercent(...)}
     * (WindowTooltipBoundsScenario.java:161-168); moving it is a style write on one out-of-flow
     * node, which must not disturb the slot rows.</p>
     */
    @Benchmark
    public void inventoryTooltipFollow(InventoryIncrementalState state, Blackhole bh) {
        state.tooltipMoved = !state.tooltipMoved;
        state.shape.tree.setStyle(
            state.shape.tooltip,
            state.tooltipMoved ? state.shape.tooltipAlternateStyle : state.shape.tooltipPlainStyle
        );
        state.shape.tree.computeLayout(state.shape.root, space(INVENTORY_WIDTH, INVENTORY_HEIGHT));
        bh.consume(state.shape.checksum());
    }

    /**
     * The inventory viewport is resized.
     *
     * <p>LDLib2: a window resize re-runs {@code calculateStyleAndLayout} (ModularUI.java:561-595);
     * the percent-sized root and the centered slot rows re-resolve against the new width while the
     * fixed slots keep their caches.</p>
     */
    @Benchmark
    public void inventoryResizeRoot(InventoryIncrementalState state, Blackhole bh) {
        state.wide = !state.wide;
        float width = state.wide ? INVENTORY_WIDTH : INVENTORY_RESIZED_WIDTH;
        state.shape.tree.computeLayout(state.shape.root, space(width, INVENTORY_HEIGHT));
        bh.consume(state.shape.checksum());
    }

    /**
     * A slot appears/disappears in the hotbar row and one layout runs.
     *
     * <p>LDLib2: a slot whose item changes is still the same element, but a dynamically filled
     * row ({@code UIElement.addChild} → {@code TaffyTree.addChild}, UIElement.java:867-911) is
     * the layout-side cost this models.</p>
     */
    @Benchmark
    public void inventoryAddRemoveChild(InventoryIncrementalState state, Blackhole bh) {
        state.shape.tree.addChild(state.shape.hostRow, state.shape.detachedSlot);
        state.shape.tree.computeLayout(state.shape.root, space(INVENTORY_WIDTH, INVENTORY_HEIGHT));
        bh.consume(state.shape.checksum());
        state.shape.tree.removeChild(state.shape.hostRow, state.shape.detachedSlot);
    }

    // =================================================================== measured text (cold/warm)

    /**
     * A measured-label tree: the layout-side model of a text-heavy LDLib2 screen.
     *
     * <p>LDLib2 counterpart: every {@code Label}/{@code TextElement} runs
     * {@code recompute()} (TextElement.java:200-224), measures the formatted lines and writes the
     * measured width/height back into the layout style via {@code adaptiveWidth}/{@code adaptiveHeight}.
     * This tree gives the layout engine the measured-leaf shape without the text pipeline:</p>
     * <ul>
     *   <li>column flex root of {@code labelCount} percent-width leaves, {@code auto} height;</li>
     *   <li>each leaf carries a {@link GlyphTextMeasure} with a deterministic character count;</li>
     *   <li>a cold layout reaches the measure callback once per leaf; a layout whose leaf caches
     *       are still valid answers from the cache.</li>
     * </ul>
     */
    static final class MeasureShape extends BaseShape {
        final GlyphTextMeasure[] measures;

        MeasureShape(TaffyTree tree, NodeId root, GlyphTextMeasure[] measures) {
            super(tree, root);
            this.measures = measures;
        }

        long measureCalls() {
            long calls = 0L;
            for (GlyphTextMeasure measure : measures) {
                calls += measure.callCount();
            }
            return calls;
        }

        void resetMeasureCalls() {
            for (GlyphTextMeasure measure : measures) {
                measure.resetCalls();
            }
        }
    }

    static MeasureShape buildMeasureTree(int labelCount) {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.COLUMN;
        rootStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);

        GlyphTextMeasure[] measures = new GlyphTextMeasure[labelCount];
        NodeId[] labels = new NodeId[labelCount];
        for (int i = 0; i < labelCount; i++) {
            TaffyStyle labelStyle = new TaffyStyle();
            labelStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
            GlyphTextMeasure measure = new GlyphTextMeasure(textLength(i));
            measures[i] = measure;
            labels[i] = tree.newLeafWithMeasure(labelStyle, measure);
        }

        NodeId root = tree.newWithChildren(rootStyle, labels);
        return new MeasureShape(tree, root, measures);
    }

    static TaffySize<AvailableSpace> measureSpace() {
        return TaffySize.of(AvailableSpace.definite(900f), AvailableSpace.maxContent());
    }

    @State(Scope.Thread)
    public static class MeasureColdState {
        @Param({"2000"})
        public int labelCount;

        MeasureShape shape;

        @Setup(Level.Trial)
        public void setupTrial() {
            shape = buildMeasureTree(labelCount);
            shape.tree.computeLayout(shape.root, measureSpace());
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            shape.clearAllCaches();
        }
    }

    /**
     * Measured text, cold: every label's cache is cleared, so the layout algorithm must call the
     * measure callback for every leaf ({@code TextElement.recompute} equivalent,
     * TextElement.java:200-224).
     */
    @Benchmark
    public void measureCold(MeasureColdState state, Blackhole bh) {
        state.shape.tree.computeLayout(state.shape.root, measureSpace());
        bh.consume(state.shape.checksum());
    }

    /**
     * Measured text, warm: only the root cache is cleared, so the root's algorithm runs but the
     * measured leaves answer from their caches. This is the "same frame requested twice" case
     * inside LDLib2's dirty loop (ModularUI.java:618-650), where the second pass must not re-measure
     * every label.
     */
    @State(Scope.Thread)
    public static class MeasureWarmState {
        @Param({"2000"})
        public int labelCount;

        MeasureShape shape;

        @Setup(Level.Trial)
        public void setupTrial() {
            shape = buildMeasureTree(labelCount);
            shape.tree.computeLayout(shape.root, measureSpace());
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            shape.tree.clearCache(shape.root);
        }
    }

    @Benchmark
    public void measureWarm(MeasureWarmState state, Blackhole bh) {
        state.shape.tree.computeLayout(state.shape.root, measureSpace());
        bh.consume(state.shape.checksum());
    }
}
