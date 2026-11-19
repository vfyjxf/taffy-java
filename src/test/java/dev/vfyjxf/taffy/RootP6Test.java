package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P6 regression tests for the root node as the initial containing block (ICB) and for the
 * direction semantics of the root.
 *
 * <p>Upstream taffy #1204 and #1205 changed {@code compute_root_layout} so that the root is
 * placed inside a viewport-sized initial containing block anchored at the canvas origin instead of
 * always being pinned at {@code (0, 0)}:</p>
 *
 * <ul>
 *   <li>an in-flow root's margin box sits at {@code (margin.left, margin.top)} (or at the right
 *       edge in a definite RTL viewport), and a {@code position: relative} root is then offset by
 *       its insets;</li>
 *   <li>a {@code position: absolute}/{@code fixed} root is sized and positioned by the
 *       out-of-flow rules against the ICB (inset stretch, shrink-to-fit, auto margins);</li>
 *   <li>an out-of-flow child whose containing block is the ICB is positioned against the
 *       viewport: always for {@code fixed}, and for {@code absolute} when the root is not
 *       positioned. The 1.2 line has no out-of-flow hoisting (OF-01), so only the root's direct
 *       children are covered here.</li>
 * </ul>
 *
 * <p>The root placement cases are ported from upstream
 * {@code tests/hand_written/initial_containing_block.rs} (#1205) and from the generated
 * {@code tests/xml/block/block_root_*.xml} fixtures added by #1204/#1205, including their RTL
 * variants. The Java 1.2 default position is {@code RELATIVE} (decision D1), so cases whose
 * upstream fixture relies on the CSS default {@code STATIC} set it explicitly.</p>
 */
@DisplayName("P6 root viewport ICB and RTL root (OF-07 / ST-06)")
class RootP6Test {

    private static final float EPSILON = 0.01f;

    private static final TaffyPosition[] OUT_OF_FLOW_POSITIONS =
        new TaffyPosition[]{TaffyPosition.ABSOLUTE, TaffyPosition.FIXED};

    private static final TaffyDirection[] DIRECTIONS =
        new TaffyDirection[]{TaffyDirection.LTR, TaffyDirection.RTL};

    // === Helpers ===

    private static TaffySize<AvailableSpace> viewport(float width, float height) {
        return new TaffySize<>(AvailableSpace.definite(width), AvailableSpace.definite(height));
    }

    /** Computes layout with rounding disabled and returns the tree for assertions. */
    private static TaffyTree compute(TaffyTree tree, NodeId root, float viewportWidth, float viewportHeight) {
        tree.disableRounding();
        tree.computeLayout(root, viewport(viewportWidth, viewportHeight));
        return tree;
    }

    private static TaffyStyle blockStyle() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        return style;
    }

    private static TaffyStyle sized(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle sizedBlock(float width, float height) {
        TaffyStyle style = sized(width, height);
        style.display = TaffyDisplay.BLOCK;
        return style;
    }

    private static LengthPercentageAuto px(float value) {
        return LengthPercentageAuto.length(value);
    }

    private static TaffyRect<LengthPercentageAuto> margin(float left, float right, float top, float bottom) {
        return new TaffyRect<>(px(left), px(right), px(top), px(bottom));
    }

    private static TaffyRect<LengthPercentageAuto> inset(LengthPercentageAuto left, LengthPercentageAuto right,
                                                         LengthPercentageAuto top, LengthPercentageAuto bottom) {
        return new TaffyRect<>(left, right, top, bottom);
    }

    private static TaffyRect<LengthPercentage> edges(float value) {
        return TaffyRect.all(LengthPercentage.length(value));
    }

    private static void assertNode(TaffyTree tree, NodeId node, float x, float y, float width, float height,
                                   String message) {
        Layout layout = tree.getLayout(node);
        assertEquals(x, layout.location().x, EPSILON, message + " (x)");
        assertEquals(y, layout.location().y, EPSILON, message + " (y)");
        assertEquals(width, layout.size().width, EPSILON, message + " (width)");
        assertEquals(height, layout.size().height, EPSILON, message + " (height)");
    }

    // === OF-07 / #1205: the root is placed in the initial containing block ===

    /**
     * Ported from upstream {@code root_is_placed_at_its_margin} (#1205): the root's margin box is
     * placed at the origin of the initial containing block and a block-level root still stretches
     * to the available width less its horizontal margins.
     */
    @Test
    @DisplayName("OF-07: an in-flow root's margin box is placed at the ICB origin")
    void inFlowRootIsPlacedAtItsMargin() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = blockStyle();
        rootStyle.margin = margin(10f, 10f, 20f, 20f);
        rootStyle.border = edges(5f);
        NodeId root = tree.newLeaf(rootStyle);

        compute(tree, root, 800f, 600f);

        Layout layout = tree.getLayout(root);
        assertEquals(10f, layout.location().x, EPSILON, "root x is margin.left");
        assertEquals(20f, layout.location().y, EPSILON, "root y is margin.top");
        assertEquals(780f, layout.size().width, EPSILON, "root stretches to the ICB width less its margins");
    }

    /**
     * Ported from upstream {@code inset_zero_fills_viewport} (#1205). The child's location is
     * relative to the root's border box, which sits at {@code (10, 20)} in the viewport, so a
     * viewport-filling box is at {@code (-10, -20)} in its parent's frame.
     */
    @Test
    @DisplayName("OF-07: a zero-inset absolute/fixed child fills the viewport-sized ICB")
    void zeroInsetOutOfFlowChildFillsViewport() {
        for (TaffyPosition position : OUT_OF_FLOW_POSITIONS) {
            TaffyTree tree = new TaffyTree();

            TaffyStyle rootStyle = blockStyle();
            // The upstream fixture relies on the CSS default `static`; the Java 1.2 default is
            // `relative` (decision D1), under which the root would itself be the child's
            // containing block.
            rootStyle.position = TaffyPosition.STATIC;
            rootStyle.margin = margin(10f, 10f, 20f, 20f);
            rootStyle.border = edges(5f);

            TaffyStyle oofStyle = new TaffyStyle();
            oofStyle.position = position;
            oofStyle.inset = inset(px(0f), px(0f), px(0f), px(0f));
            NodeId oof = tree.newLeaf(oofStyle);
            NodeId root = tree.newWithChildren(rootStyle, oof);

            compute(tree, root, 800f, 600f);

            assertNode(tree, root, 10f, 20f, 780f, 10f, position + " root");
            assertNode(tree, oof, -10f, -20f, 800f, 600f, position + " fills the viewport");
        }
    }

    /**
     * Ported from upstream {@code bottom_right_insets_resolve_against_viewport} (#1205): insets
     * resolve against the viewport, and the area offset is the negated root location.
     */
    @Test
    @DisplayName("OF-07: bottom/right insets of an ICB-positioned child resolve against the viewport")
    void bottomRightInsetsResolveAgainstViewport() {
        for (TaffyPosition position : OUT_OF_FLOW_POSITIONS) {
            TaffyTree tree = new TaffyTree();

            TaffyStyle rootStyle = blockStyle();
            rootStyle.position = TaffyPosition.STATIC;
            rootStyle.margin = margin(10f, 10f, 20f, 20f);
            rootStyle.border = edges(5f);

            TaffyStyle oofStyle = sized(50f, 50f);
            oofStyle.position = position;
            oofStyle.inset = inset(LengthPercentageAuto.AUTO, px(0f), LengthPercentageAuto.AUTO, px(0f));
            NodeId oof = tree.newLeaf(oofStyle);
            NodeId root = tree.newWithChildren(rootStyle, oof);

            compute(tree, root, 800f, 600f);

            assertNode(tree, oof, 800f - 50f - 10f, 600f - 50f - 20f, 50f, 50f,
                position + " resolved against the ICB");
        }
    }

    /**
     * Ported from upstream {@code static_position_is_unaffected} (#1205): an out-of-flow box
     * with no insets keeps the static position its containing block's algorithm gave it.
     */
    @Test
    @DisplayName("OF-07: an out-of-flow child without insets keeps its static position")
    void staticPositionIsUnaffected() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = blockStyle();
        rootStyle.position = TaffyPosition.STATIC;
        rootStyle.margin = margin(10f, 10f, 20f, 20f);
        rootStyle.border = edges(5f);

        TaffyStyle oofStyle = sized(50f, 50f);
        oofStyle.position = TaffyPosition.ABSOLUTE;
        NodeId oof = tree.newLeaf(oofStyle);
        NodeId root = tree.newWithChildren(rootStyle, oof);

        compute(tree, root, 800f, 600f);

        // The root's padding box starts at its border (5, 5).
        assertNode(tree, oof, 5f, 5f, 50f, 50f, "static position");
    }

    /**
     * Ported from upstream {@code fixed_root_is_positioned_by_its_insets} (#1205): a
     * {@code position: fixed} root is positioned like an absolutely positioned root against the
     * ICB (fixed cannot be covered by the Chrome-oracle fixtures).
     */
    @Test
    @DisplayName("OF-07: an absolute/fixed root is positioned by its insets against the viewport")
    void outOfFlowRootIsPositionedByItsInsets() {
        for (TaffyPosition position : OUT_OF_FLOW_POSITIONS) {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = sizedBlock(100f, 50f);
            rootStyle.position = position;
            rootStyle.inset = inset(LengthPercentageAuto.AUTO, px(10f),
                LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.AUTO);
            NodeId root = tree.newLeaf(rootStyle);

            compute(tree, root, 800f, 600f);

            // 800 - 100 - 10 = 690; 10% of 600 = 60.
            assertNode(tree, root, 690f, 60f, 100f, 50f, position + " root");
        }
    }

    // === OF-07 / #1204: a viewport-sized ICB for the root's out-of-flow children ===

    /**
     * Ported from the generated {@code block_root_absolute_inset_all__*_ltr/rtl} fixtures
     * (#1205): a pair of insets stretches the auto-sized root across the viewport, and its in-flow
     * child fills the resulting content box.
     */
    @Test
    @DisplayName("OF-07: an absolute root with four insets stretches across the viewport")
    void absoluteRootWithFourInsetsStretchesAcrossViewport() {
        for (TaffyDirection direction : DIRECTIONS) {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = blockStyle();
            rootStyle.direction = direction;
            rootStyle.position = TaffyPosition.ABSOLUTE;
            rootStyle.inset = inset(px(50f), px(30f), px(20f), px(40f));

            TaffyStyle childStyle = blockStyle();
            childStyle.position = TaffyPosition.RELATIVE;
            childStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(10f));
            NodeId child = tree.newLeaf(childStyle);
            NodeId root = tree.newWithChildren(rootStyle, child);

            compute(tree, root, 400f, 300f);

            // 400 - 50 - 30 = 320; 300 - 20 - 40 = 240 (both directions agree because the
            // stretched size leaves no free space to distribute).
            assertNode(tree, root, 50f, 20f, 320f, 240f, direction + " root");
            assertNode(tree, child, 0f, 0f, 320f, 10f, direction + " in-flow child");
        }
    }

    /**
     * Ported from the generated {@code block_root_absolute_inset_left_top__*_ltr/rtl} fixtures
     * (#1205): the root's border box is placed at the inset position (the border is not added to
     * the location) and the block-axis percentage inset resolves against the viewport height.
     */
    @Test
    @DisplayName("OF-07: an absolute root with left/top insets keeps its border box at that position")
    void absoluteRootWithLeftTopInsets() {
        for (TaffyDirection direction : DIRECTIONS) {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = sizedBlock(100f, 50f);
            rootStyle.direction = direction;
            rootStyle.position = TaffyPosition.ABSOLUTE;
            rootStyle.border = edges(5f);
            rootStyle.inset = inset(px(30f), LengthPercentageAuto.AUTO,
                LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.AUTO);

            TaffyStyle childStyle = blockStyle();
            childStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(10f));
            NodeId child = tree.newLeaf(childStyle);
            NodeId root = tree.newWithChildren(rootStyle, child);

            compute(tree, root, 400f, 300f);

            // left = 30, top = 10% of 300 = 30.
            assertNode(tree, root, 30f, 30f, 100f, 50f, direction + " root");
            assertNode(tree, child, 5f, 5f, 90f, 10f, direction + " child inside the border");
        }
    }

    /**
     * Ported from the generated {@code block_root_absolute_inset_right_bottom__*_ltr/rtl}
     * fixtures (#1205): the right/bottom insets are measured from the viewport edges and the
     * root's margins are subtracted from that edge.
     */
    @Test
    @DisplayName("OF-07: an absolute root with right/bottom insets resolves them against the viewport")
    void absoluteRootWithRightBottomInsets() {
        for (TaffyDirection direction : DIRECTIONS) {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = sizedBlock(100f, 50f);
            rootStyle.direction = direction;
            rootStyle.position = TaffyPosition.ABSOLUTE;
            rootStyle.margin = margin(10f, 10f, 5f, 5f);
            rootStyle.inset = inset(LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.1f),
                LengthPercentageAuto.AUTO, px(20f));

            TaffyStyle childStyle = blockStyle();
            childStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(10f));
            NodeId child = tree.newLeaf(childStyle);
            NodeId root = tree.newWithChildren(rootStyle, child);

            compute(tree, root, 400f, 300f);

            // x = 400 - 100 - 10% * 400 - margin.right(10) = 250;
            // y = 300 - 50 - 20 - margin.bottom(5) = 225.
            assertNode(tree, root, 250f, 225f, 100f, 50f, direction + " root");
            assertNode(tree, child, 0f, 0f, 100f, 10f, direction + " in-flow child");
        }
    }

    /**
     * Ported from the generated {@code block_root_absolute_shrink_to_fit__*_ltr} fixture
     * (#1205): an absolutely positioned root with no size and no insets shrinks to fit its
     * content (plus its padding) instead of stretching to the viewport, while its static position
     * is the ICB origin.
     */
    @Test
    @DisplayName("OF-07: an auto-sized absolute root shrinks to fit in LTR")
    void absoluteRootShrinksToFitInLtr() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = blockStyle();
        rootStyle.direction = TaffyDirection.LTR;
        rootStyle.position = TaffyPosition.ABSOLUTE;
        rootStyle.padding = edges(5f);

        NodeId child = tree.newLeaf(sizedBlock(60f, 10f));
        NodeId root = tree.newWithChildren(rootStyle, child);

        compute(tree, root, 400f, 300f);

        // content 60x10 + padding 5 on each side.
        assertNode(tree, root, 0f, 0f, 70f, 20f, "LTR shrink-to-fit root");
        assertNode(tree, child, 5f, 5f, 60f, 10f, "LTR child");
    }

    /**
     * Ported from the generated {@code block_root_absolute_shrink_to_fit__*_rtl} fixture
     * (#1205): the same shrink-to-fit root is right-aligned in the ICB in RTL.
     */
    @Test
    @DisplayName("OF-07: an auto-sized absolute RTL root shrinks to fit at the right edge")
    void absoluteRootShrinksToFitInRtl() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = blockStyle();
        rootStyle.direction = TaffyDirection.RTL;
        rootStyle.position = TaffyPosition.ABSOLUTE;
        rootStyle.padding = edges(5f);

        NodeId child = tree.newLeaf(sizedBlock(60f, 10f));
        NodeId root = tree.newWithChildren(rootStyle, child);

        compute(tree, root, 400f, 300f);

        // 400 - 70 = 330.
        assertNode(tree, root, 330f, 0f, 70f, 20f, "RTL shrink-to-fit root");
        assertNode(tree, child, 5f, 5f, 60f, 10f, "RTL child");
    }

    /**
     * At viewport size the ICB is the root's containing block, so an auto-sized root with all
     * insets zero is exactly the viewport and its in-flow children lay out in that box.
     */
    @Test
    @DisplayName("OF-07: an absolute root with zero insets is equivalent to the viewport")
    void zeroInsetAbsoluteRootIsTheViewport() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = blockStyle();
        rootStyle.position = TaffyPosition.ABSOLUTE;
        rootStyle.inset = inset(px(0f), px(0f), px(0f), px(0f));

        NodeId child = tree.newLeaf(sizedBlock(400f, 10f));
        NodeId root = tree.newWithChildren(rootStyle, child);

        compute(tree, root, 400f, 300f);

        assertNode(tree, root, 0f, 0f, 400f, 300f, "zero-inset root fills the viewport");
        assertNode(tree, child, 0f, 0f, 400f, 10f, "in-flow child of the viewport-sized root");
    }

    /**
     * Ported from the generated {@code block_root_relative_inset__*_ltr} fixture (#1205): a
     * relatively positioned root starts at its margin box origin and is then offset by its insets,
     * keeping its in-flow and out-of-flow children in the root's frame.
     */
    @Test
    @DisplayName("OF-07: an LTR relative root is offset by its insets from its margin origin")
    void relativeRootOffsetByInsetsInLtr() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = sizedBlock(100f, 50f);
        rootStyle.direction = TaffyDirection.LTR;
        rootStyle.position = TaffyPosition.RELATIVE;
        rootStyle.margin = margin(10f, 10f, 10f, 10f);
        rootStyle.border = edges(5f);
        rootStyle.inset = inset(px(25f), LengthPercentageAuto.AUTO,
            LengthPercentageAuto.AUTO, px(15f));

        TaffyStyle flowStyle = blockStyle();
        flowStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(10f));
        NodeId flow = tree.newLeaf(flowStyle);
        TaffyStyle absoluteStyle = sized(20f, 20f);
        absoluteStyle.position = TaffyPosition.ABSOLUTE;
        absoluteStyle.inset = inset(LengthPercentageAuto.AUTO, px(0f),
            LengthPercentageAuto.AUTO, px(10f));
        NodeId absolute = tree.newLeaf(absoluteStyle);
        NodeId root = tree.newWithChildren(rootStyle, flow, absolute);

        compute(tree, root, 400f, 300f);

        // x = margin.left(10) + left(25) = 35; y = margin.top(10) - bottom(15) = -5.
        assertNode(tree, root, 35f, -5f, 100f, 50f, "LTR relative root");
        assertNode(tree, flow, 5f, 5f, 90f, 10f, "LTR in-flow child");
        // The root is positioned, so it is the absolutely positioned child's containing block.
        assertNode(tree, absolute, 75f, 15f, 20f, 20f, "LTR absolute child");
    }

    /**
     * Ported from the generated {@code block_root_relative_inset__*_rtl} fixture (#1205): the RTL
     * root starts at the right edge of the ICB and its relative inset still resolves from the
     * physical left side (which is set, while right is auto).
     */
    @Test
    @DisplayName("OF-07: an RTL relative root is right-aligned and offset by its insets")
    void relativeRootOffsetByInsetsInRtl() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = sizedBlock(100f, 50f);
        rootStyle.direction = TaffyDirection.RTL;
        rootStyle.position = TaffyPosition.RELATIVE;
        rootStyle.margin = margin(10f, 10f, 10f, 10f);
        rootStyle.border = edges(5f);
        rootStyle.inset = inset(px(25f), LengthPercentageAuto.AUTO,
            LengthPercentageAuto.AUTO, px(15f));

        TaffyStyle flowStyle = blockStyle();
        flowStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(10f));
        NodeId flow = tree.newLeaf(flowStyle);
        TaffyStyle absoluteStyle = sized(20f, 20f);
        absoluteStyle.position = TaffyPosition.ABSOLUTE;
        absoluteStyle.inset = inset(LengthPercentageAuto.AUTO, px(0f),
            LengthPercentageAuto.AUTO, px(10f));
        NodeId absolute = tree.newLeaf(absoluteStyle);
        NodeId root = tree.newWithChildren(rootStyle, flow, absolute);

        compute(tree, root, 400f, 300f);

        // x = 400 - 100 - margin.right(10) + left(25) = 315; y = -5 as in LTR.
        assertNode(tree, root, 315f, -5f, 100f, 50f, "RTL relative root");
        assertNode(tree, flow, 5f, 5f, 90f, 10f, "RTL in-flow child");
        assertNode(tree, absolute, 75f, 15f, 20f, 20f, "RTL absolute child");
    }

    /**
     * The root-level ICB pass must be stable: a cache hit on the root skips the layout
     * algorithms, so the pass recomputes positions from the styles and the root layout alone. A
     * second layout with the same viewport and a third one with a resized viewport are checked
     * here.
     */
    @Test
    @DisplayName("OF-07: the root-level ICB pass is stable across repeated layouts")
    void rootLevelIcbPassIsStable() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = blockStyle();
        rootStyle.position = TaffyPosition.STATIC;
        rootStyle.margin = margin(10f, 10f, 20f, 20f);
        rootStyle.border = edges(5f);

        TaffyStyle oofStyle = new TaffyStyle();
        oofStyle.position = TaffyPosition.FIXED;
        oofStyle.inset = inset(px(0f), px(0f), px(0f), px(0f));
        NodeId oof = tree.newLeaf(oofStyle);
        NodeId root = tree.newWithChildren(rootStyle, oof);

        compute(tree, root, 800f, 600f);
        assertNode(tree, oof, -10f, -20f, 800f, 600f, "first layout");

        // Same inputs: the root's layout comes from the cache, the pass must not drift.
        tree.computeLayout(root, viewport(800f, 600f));
        assertNode(tree, oof, -10f, -20f, 800f, 600f, "repeated layout");

        // Resized viewport: the ICB is the new viewport and the root's margin box moves with it.
        tree.computeLayout(root, viewport(1000f, 400f));
        assertNode(tree, root, 10f, 20f, 980f, 10f, "resized root");
        assertNode(tree, oof, -10f, -20f, 1000f, 400f, "resized ICB");
    }

    // === OF-07: the root's margin box and non-relative positions ===

    /**
     * Regression for the pre-P6 numbers (PA-03): without margins a definite-width LTR root keeps
     * the historical origin.
     */
    @Test
    @DisplayName("OF-07: legacy LTR root numbers are unchanged")
    void legacyLtrRootNumbersAreUnchanged() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = sizedBlock(300f, 100f);
        rootStyle.direction = TaffyDirection.LTR;
        NodeId root = tree.newLeaf(rootStyle);

        compute(tree, root, 500f, 400f);

        assertNode(tree, root, 0f, 0f, 300f, 100f, "LTR root");
    }

    @Test
    @DisplayName("OF-07: an LTR root's margin box is placed at (margin.left, margin.top)")
    void ltrRootUsesLeftAndTopMargins() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = sizedBlock(100f, 50f);
        rootStyle.direction = TaffyDirection.LTR;
        rootStyle.margin = margin(7f, 9f, 11f, 13f);
        NodeId root = tree.newLeaf(rootStyle);

        compute(tree, root, 300f, 150f);

        assertNode(tree, root, 7f, 11f, 100f, 50f, "LTR root");
    }

    /**
     * Only {@code position: relative} applies its insets as an offset; a statically positioned
     * root is placed by its margin alone.
     */
    @Test
    @DisplayName("OF-07: a static root ignores its insets")
    void staticRootIgnoresInsets() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = sizedBlock(100f, 50f);
        rootStyle.direction = TaffyDirection.LTR;
        rootStyle.position = TaffyPosition.STATIC;
        rootStyle.margin = margin(10f, 10f, 10f, 10f);
        rootStyle.inset = inset(px(25f), LengthPercentageAuto.AUTO,
            LengthPercentageAuto.AUTO, px(15f));
        NodeId root = tree.newLeaf(rootStyle);

        compute(tree, root, 400f, 300f);

        assertNode(tree, root, 10f, 10f, 100f, 50f, "static root");
    }


    // === Containing-block boundary: fixed vs absolute under a positioned root ===

    /**
     * A {@code fixed} child is never claimed by an ancestor's positioning pass, so it uses the
     * ICB even when the root is positioned; an {@code absolute} child of the same positioned root
     * is positioned by the root itself (CSS: the root is its containing block).
     */
    @Test
    @DisplayName("OF-07: fixed uses the viewport while absolute uses a positioned root")
    void fixedChildOfPositionedRootUsesViewport() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = sizedBlock(100f, 50f);
        rootStyle.position = TaffyPosition.RELATIVE;
        rootStyle.margin = margin(10f, 0f, 20f, 0f);

        TaffyStyle fixedStyle = new TaffyStyle();
        fixedStyle.position = TaffyPosition.FIXED;
        fixedStyle.inset = inset(px(0f), px(0f), px(0f), px(0f));
        NodeId fixed = tree.newLeaf(fixedStyle);

        TaffyStyle absoluteStyle = new TaffyStyle();
        absoluteStyle.position = TaffyPosition.ABSOLUTE;
        absoluteStyle.inset = inset(px(0f), px(0f), px(0f), px(0f));
        NodeId absolute = tree.newLeaf(absoluteStyle);

        NodeId root = tree.newWithChildren(rootStyle, fixed, absolute);

        compute(tree, root, 400f, 300f);

        assertNode(tree, root, 10f, 20f, 100f, 50f, "positioned root");
        // The ICB is at the canvas origin, which is (-10, -20) in the root's frame.
        assertNode(tree, fixed, -10f, -20f, 400f, 300f, "fixed child of the positioned root");
        // The root's padding box is 100x50 at the root's own origin.
        assertNode(tree, absolute, 0f, 0f, 100f, 50f, "absolute child of the positioned root");
    }
}