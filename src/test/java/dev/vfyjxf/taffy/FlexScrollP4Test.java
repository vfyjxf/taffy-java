package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.ScrollableOverflow;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P4 scrollable-overflow tests for the flex algorithm (SO-01/SO-02/SO-03/SO-05).
 *
 * <p>Upstream #1118 replaced {@code content_size} with the CSS scrollable overflow rectangle and
 * #1372 added the packed {@link ScrollableOverflow} type with physical coordinates and reversed
 * scroll origins; #1007, #1114 and #1116 refined the border/padding/scroll-origin rules for flex
 * containers. The flex final-layout pass now accumulates one
 * {@code compute_scrollable_overflow_contribution} rectangle per item in the container's
 * padding-box frame, adds the container's own contribution (RTL client width and, for a scroll
 * container, its own end padding), unions the absolutely positioned children's contributions and
 * finalizes the result against the client area.</p>
 *
 * <p>These tests assert the per-side rectangle and the reversed-origin flags of
 * {@link Layout#scrollableOverflow()} plus the legacy {@code contentSize} component. The legacy
 * component is still the 1.x field: it keeps the old aggregation (with #1007's border-origin
 * correction) and does not include a scroll container's end padding, because
 * {@code scrollWidth()}/{@code scrollHeight()} only switch to the new rectangle in the later
 * integration step (SO-01/SO-03 separation).</p>
 */
public class FlexScrollP4Test {

    private static final float EPSILON = 0.01f;

    private static final TaffyPoint<Overflow> SCROLL =
        new TaffyPoint<>(Overflow.SCROLL, Overflow.SCROLL);
    private static final TaffyPoint<Overflow> VISIBLE =
        new TaffyPoint<>(Overflow.VISIBLE, Overflow.VISIBLE);

    private static TaffyStyle style() {
        return new TaffyStyle();
    }

    private static TaffyStyle sized(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static NodeId leaf(TaffyTree tree, float width, float height) {
        return tree.newLeaf(sized(width, height));
    }

    private static Layout layout(TaffyTree tree, NodeId node) {
        Layout layout = tree.getLayout(node);
        assertNotNull(layout, "layout of " + node + " must exist");
        return layout;
    }

    private static void assertOverflow(
        Layout layout,
        float left,
        float right,
        float top,
        float bottom
    ) {
        ScrollableOverflow overflow = layout.scrollableOverflow();
        assertNotNull(overflow, "scrollable overflow must be present");
        assertEquals(left, overflow.left(), EPSILON, "scrollable overflow left");
        assertEquals(right, overflow.right(), EPSILON, "scrollable overflow right");
        assertEquals(top, overflow.top(), EPSILON, "scrollable overflow top");
        assertEquals(bottom, overflow.bottom(), EPSILON, "scrollable overflow bottom");
        // ScrollableOverflow documents that the rectangle always contains the scroll origin.
        assertTrue(overflow.left() <= 0f, "left must be non-positive: " + overflow);
        assertTrue(overflow.top() <= 0f, "top must be non-positive: " + overflow);
        assertTrue(overflow.right() >= 0f, "right must be non-negative: " + overflow);
        assertTrue(overflow.bottom() >= 0f, "bottom must be non-negative: " + overflow);
    }

    private static void assertOrigin(Layout layout, boolean reversedX, boolean reversedY) {
        ScrollableOverflow overflow = layout.scrollableOverflow();
        assertEquals(reversedX, overflow.reversedX(), "reversed x scroll origin");
        assertEquals(reversedY, overflow.reversedY(), "reversed y scroll origin");
    }

    private static void assertContentSize(Layout layout, float width, float height) {
        assertEquals(width, layout.contentSize().width, EPSILON, "content size width");
        assertEquals(height, layout.contentSize().height, EPSILON, "content size height");
    }

    // === Children of scroll vs non-scroll containers ===

    @Test
    @DisplayName("a_scroll_container_keeps_the_child_content_extent")
    void scrollContainerKeepsChildContentExtent() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle childStyle = sized(200f, 300f);
        childStyle.flexShrink = 0f;
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = sized(100f, 100f);
        rootStyle.overflow = SCROLL;
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout rootLayout = layout(tree, root);
        assertOverflow(rootLayout, 0f, 200f, 0f, 300f);
        assertOrigin(rootLayout, false, false);
        assertContentSize(rootLayout, 200f, 300f);
        // With no padding and no scrollbar the legacy ranges already line up with the rectangle.
        assertEquals(100f, rootLayout.scrollWidth(), EPSILON);
        assertEquals(200f, rootLayout.scrollHeight(), EPSILON);
        assertEquals(0f, layout(tree, child).location().x, EPSILON);
    }

    @Test
    @DisplayName("a_non_scroll_container_propagates_instead_of_clipping")
    void nonScrollContainerPropagatesOverflow() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle childStyle = sized(200f, 300f);
        childStyle.flexShrink = 0f;
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = sized(100f, 100f);
        rootStyle.overflow = VISIBLE;
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout rootLayout = layout(tree, root);
        assertOverflow(rootLayout, 0f, 200f, 0f, 300f);
        assertOrigin(rootLayout, false, false);
        assertContentSize(rootLayout, 200f, 300f);
    }

    // === Border and padding frames ===

    @Test
    @DisplayName("leading_and_trailing_borders_produce_the_same_scroll_range")
    void leadingAndTrailingBordersProduceTheSameScrollRange() {
        // Upstream #1007: the maximum scroll offset is the content beyond the padding box, so a
        // 20px border belongs on either edge. Flex used to cancel this against a content-size bug.
        for (boolean topBorder : new boolean[]{true, false}) {
            TaffyTree tree = new TaffyTree();
            TaffyStyle childStyle = sized(100f, 1000f);
            childStyle.flexShrink = 0f;
            NodeId child = tree.newLeaf(childStyle);

            TaffyStyle rootStyle = sized(300f, 200f);
            rootStyle.overflow = SCROLL;
            rootStyle.border = topBorder
                               ? TaffyRect.ltrb(zero(), len(20f), zero(), zero())
                               : TaffyRect.ltrb(zero(), zero(), zero(), len(20f));
            NodeId root = tree.newWithChildren(rootStyle, child);
            tree.computeLayout(root, TaffySize.maxContent());

            Layout rootLayout = layout(tree, root);
            assertEquals(820f, rootLayout.scrollHeight(), EPSILON,
                "border on " + (topBorder ? "top" : "bottom"));
            assertOverflow(rootLayout, 0f, 100f, 0f, 1000f);
            assertContentSize(rootLayout, 100f, 1000f);
        }

        for (boolean leftBorder : new boolean[]{true, false}) {
            TaffyTree tree = new TaffyTree();
            TaffyStyle childStyle = sized(1000f, 100f);
            childStyle.flexShrink = 0f;
            NodeId child = tree.newLeaf(childStyle);

            TaffyStyle rootStyle = sized(300f, 200f);
            rootStyle.overflow = SCROLL;
            rootStyle.border = leftBorder
                               ? TaffyRect.ltrb(len(20f), zero(), zero(), zero())
                               : TaffyRect.ltrb(zero(), zero(), len(20f), zero());
            NodeId root = tree.newWithChildren(rootStyle, child);
            tree.computeLayout(root, TaffySize.maxContent());

            Layout rootLayout = layout(tree, root);
            assertEquals(720f, rootLayout.scrollWidth(), EPSILON,
                "border on " + (leftBorder ? "left" : "right"));
            assertOverflow(rootLayout, 0f, 1000f, 0f, 100f);
            assertContentSize(rootLayout, 1000f, 100f);
        }
    }

    @Test
    @DisplayName("border_is_not_part_of_the_container_overflow_frame")
    void borderIsNotPartOfTheContainerOverflowFrame() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle childStyle = sized(100f, 1000f);
        childStyle.flexShrink = 0f;
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = sized(300f, 200f);
        rootStyle.overflow = SCROLL;
        rootStyle.border = TaffyRect.all(len(10f));
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout rootLayout = layout(tree, root);
        // The child sits at (10, 10); subtracting the padding-box inset brings the contribution
        // back to the padding-box frame, so the border never appears in the rectangle.
        assertOverflow(rootLayout, 0f, 100f, 0f, 1000f);
        assertContentSize(rootLayout, 100f, 1000f);
        assertEquals(820f, rootLayout.scrollHeight(), EPSILON);
    }

    @Test
    @DisplayName("a_scroll_container_includes_its_own_end_padding")
    void scrollContainerIncludesItsOwnEndPadding() {
        TaffyTree tree = new TaffyTree();
        NodeId child = leaf(tree, 40f, 40f);

        TaffyStyle rootStyle = sized(100f, 100f);
        rootStyle.overflow = SCROLL;
        rootStyle.padding = TaffyRect.ltrb(len(10f), len(5f), len(20f), len(30f));
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout rootLayout = layout(tree, root);
        // Child at (10, 5), 40x40: the reachable region runs to the end of the content plus the
        // container's end-side padding (right 20, bottom 30), not the start-side padding.
        assertOverflow(rootLayout, 0f, 70f, 0f, 75f);
        assertContentSize(rootLayout, 50f, 45f);
        assertEquals(0f, rootLayout.scrollWidth(), EPSILON, "the padded content still fits");
        assertEquals(0f, rootLayout.scrollHeight(), EPSILON);
    }

    @Test
    @DisplayName("a_non_scroll_container_excludes_its_own_end_padding")
    void nonScrollContainerExcludesItsOwnEndPadding() {
        TaffyTree tree = new TaffyTree();
        NodeId child = leaf(tree, 40f, 40f);

        TaffyStyle rootStyle = sized(100f, 100f);
        rootStyle.padding = TaffyRect.ltrb(zero(), len(20f), zero(), len(30f));
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout rootLayout = layout(tree, root);
        // Only the content contributes: 20 (start padding, through the child position) + 40.
        assertOverflow(rootLayout, 0f, 40f, 0f, 60f);
        assertContentSize(rootLayout, 40f, 60f);
    }

    // === Reversed scroll origins ===

    @Test
    @DisplayName("rtl_row_puts_the_scroll_origin_on_the_right")
    void rtlRowPutsTheScrollOriginOnTheRight() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle childStyle = sized(200f, 50f);
        childStyle.flexShrink = 0f;
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = sized(100f, 50f);
        rootStyle.direction = TaffyDirection.RTL;
        rootStyle.overflow = SCROLL;
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        // RTL rows pack from the right, so the 200px item starts 100px before the client area.
        assertEquals(-100f, layout(tree, child).location().x, EPSILON);
        Layout rootLayout = layout(tree, root);
        assertOverflow(rootLayout, -100f, 100f, 0f, 50f);
        assertOrigin(rootLayout, true, false);
        assertContentSize(rootLayout, 100f, 50f);
    }

    @Test
    @DisplayName("rtl_non_scroll_container_reaches_the_client_width")
    void rtlNonScrollContainerReachesTheClientWidth() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle childStyle = sized(200f, 50f);
        childStyle.flexShrink = 0f;
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = sized(100f, 50f);
        rootStyle.direction = TaffyDirection.RTL;
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        Layout rootLayout = layout(tree, root);
        // A non-scroll container keeps the whole client width on a reversed inline axis, but has
        // no reversed scroll origin of its own.
        assertOverflow(rootLayout, -100f, 100f, 0f, 50f);
        assertOrigin(rootLayout, false, false);
    }

    @Test
    @DisplayName("column_reverse_puts_the_scroll_origin_at_the_bottom")
    void columnReversePutsTheScrollOriginAtTheBottom() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle childStyle = sized(50f, 200f);
        childStyle.flexShrink = 0f;
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = sized(50f, 100f);
        rootStyle.flexDirection = FlexDirection.COLUMN_REVERSE;
        rootStyle.overflow = SCROLL;
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        assertEquals(-100f, layout(tree, child).location().y, EPSILON);
        Layout rootLayout = layout(tree, root);
        assertOverflow(rootLayout, 0f, 50f, -100f, 100f);
        assertOrigin(rootLayout, false, true);
        assertContentSize(rootLayout, 50f, 100f);
    }

    @Test
    @DisplayName("wrap_reverse_puts_the_cross_scroll_origin_at_the_bottom")
    void wrapReversePutsTheCrossScrollOriginAtTheBottom() {
        TaffyTree tree = new TaffyTree();
        NodeId first = leaf(tree, 80f, 40f);
        NodeId second = leaf(tree, 80f, 40f);

        TaffyStyle rootStyle = sized(100f, 50f);
        rootStyle.flexWrap = FlexWrap.WRAP_REVERSE;
        rootStyle.overflow = SCROLL;
        NodeId root = tree.newWithChildren(rootStyle, first, second);
        tree.computeLayout(root, TaffySize.maxContent());

        // Lines are packed from the bottom: the first line starts at 10, the second overflows
        // 30px past the top edge.
        assertEquals(10f, layout(tree, first).location().y, EPSILON);
        assertEquals(-30f, layout(tree, second).location().y, EPSILON);
        Layout rootLayout = layout(tree, root);
        assertOverflow(rootLayout, 0f, 80f, -30f, 50f);
        assertOrigin(rootLayout, false, true);
        assertContentSize(rootLayout, 80f, 50f);
    }

    // === Scroll-origin clipping ===

    @Test
    @DisplayName("a_box_wholly_before_the_scroll_origin_is_excluded")
    void boxWhollyBeforeTheScrollOriginIsExcluded() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle childStyle = sized(50f, 50f);
        childStyle.position = TaffyPosition.ABSOLUTE;
        childStyle.inset = TaffyRect.ltrb(
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.length(200f),
            LengthPercentageAuto.AUTO
        );
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = sized(100f, 50f);
        rootStyle.overflow = SCROLL;
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        // right: 200 in a 100px container puts the 50px box at x = -150: entirely unreachable.
        assertEquals(-150f, layout(tree, child).location().x, EPSILON);
        Layout rootLayout = layout(tree, root);
        assertOverflow(rootLayout, 0f, 0f, 0f, 0f);
        assertOrigin(rootLayout, false, false);
    }

    @Test
    @DisplayName("a_box_crossing_the_scroll_origin_contributes_its_reachable_part")
    void boxCrossingTheScrollOriginContributesItsReachablePart() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle childStyle = sized(50f, 50f);
        childStyle.inset = TaffyRect.ltrb(
            LengthPercentageAuto.length(-25f), LengthPercentageAuto.AUTO,
            LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO
        );
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = sized(100f, 50f);
        rootStyle.overflow = SCROLL;
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        // The relative inset shifts the item to x = -25; the rectangle keeps the reachable part
        // and the finalizer clips the unreachable start at the scroll origin.
        assertEquals(-25f, layout(tree, child).location().x, EPSILON);
        Layout rootLayout = layout(tree, root);
        assertOverflow(rootLayout, 0f, 25f, 0f, 50f);
        assertContentSize(rootLayout, 25f, 50f);
    }

    // === Absolutely positioned children ===

    @Test
    @DisplayName("an_absolutely_positioned_child_contributes_its_overflow")
    void absolutelyPositionedChildContributesItsOverflow() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle childStyle = sized(100f, 80f);
        childStyle.position = TaffyPosition.ABSOLUTE;
        childStyle.inset = TaffyRect.ltrb(
            LengthPercentageAuto.length(50f), LengthPercentageAuto.length(20f),
            LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO
        );
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = sized(100f, 100f);
        rootStyle.overflow = SCROLL;
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        Layout childLayout = layout(tree, child);
        assertEquals(50f, childLayout.location().x, EPSILON);
        assertEquals(20f, childLayout.location().y, EPSILON);
        Layout rootLayout = layout(tree, root);
        assertOverflow(rootLayout, 0f, 150f, 0f, 100f);
        assertContentSize(rootLayout, 150f, 100f);
    }

    @Test
    @DisplayName("an_out_of_flow_only_container_uses_its_own_padding")
    void outOfFlowOnlyContainerUsesItsOwnPadding() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle childStyle = sized(20f, 20f);
        childStyle.position = TaffyPosition.ABSOLUTE;
        childStyle.inset = TaffyRect.ltrb(
            LengthPercentageAuto.length(0f), LengthPercentageAuto.length(0f),
            LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO
        );
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = sized(100f, 100f);
        rootStyle.overflow = SCROLL;
        rootStyle.padding = TaffyRect.ltrb(len(10f), len(5f), len(20f), len(30f));
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        // No in-flow items: the container's own end-padding union sets the reachable extent
        // (right = 20, bottom = 30); the 20x20 box fits inside it.
        Layout rootLayout = layout(tree, root);
        assertOverflow(rootLayout, 0f, 20f, 0f, 30f);
        assertContentSize(rootLayout, 20f, 20f);
    }

    @Test
    @DisplayName("a_container_with_no_flow_items_still_uses_the_container_unions")
    void noFlowItemsStillUsesContainerUnions() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle hiddenStyle = style();
        hiddenStyle.display = TaffyDisplay.NONE;
        NodeId hidden = tree.newLeaf(hiddenStyle);

        TaffyStyle rootStyle = sized(100f, 100f);
        rootStyle.overflow = SCROLL;
        rootStyle.padding = TaffyRect.all(len(10f));
        NodeId root = tree.newWithChildren(rootStyle, hidden);
        tree.computeLayout(root, TaffySize.maxContent());

        Layout rootLayout = layout(tree, root);
        assertOverflow(rootLayout, 0f, 10f, 0f, 10f);
        assertOrigin(rootLayout, false, false);
        assertContentSize(rootLayout, 0f, 0f);

        // The same container in RTL also reaches the client width.
        TaffyTree rtlTree = new TaffyTree();
        TaffyStyle rtlHiddenStyle = style();
        rtlHiddenStyle.display = TaffyDisplay.NONE;
        NodeId rtlHidden = rtlTree.newLeaf(rtlHiddenStyle);
        TaffyStyle rtlRootStyle = sized(100f, 100f);
        rtlRootStyle.direction = TaffyDirection.RTL;
        rtlRootStyle.overflow = SCROLL;
        NodeId rtlRoot = rtlTree.newWithChildren(rtlRootStyle, rtlHidden);
        rtlTree.computeLayout(rtlRoot, TaffySize.maxContent());

        Layout rtlLayout = layout(rtlTree, rtlRoot);
        assertOverflow(rtlLayout, 0f, 100f, 0f, 0f);
        assertOrigin(rtlLayout, true, false);
    }

    // === Item layouts carry their own overflow ===

    @Test
    @DisplayName("an_item_layout_carries_the_child_scrollable_overflow")
    void itemLayoutCarriesTheChildScrollableOverflow() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        MeasureFunc measuredContent = (knownDimensions, availableSpace) -> new FloatSize(150f, 60f);
        NodeId child = tree.newLeafWithMeasure(sized(100f, 100f), measuredContent);

        TaffyStyle rootStyle = sized(200f, 200f);
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        // The measured leaf's own rectangle (0..150 x 0..60) used to be dropped by the legacy
        // 8-component Layout constructor.
        Layout childLayout = layout(tree, child);
        assertOverflow(childLayout, 0f, 150f, 0f, 60f);
        assertContentSize(childLayout, 150f, 60f);

        // A visible parent propagates the child's overflow in both axes; its own border box is
        // 100x100, so the height contribution is the larger border box.
        Layout rootLayout = layout(tree, root);
        assertOverflow(rootLayout, 0f, 150f, 0f, 100f);
        assertContentSize(rootLayout, 150f, 100f);
    }

    // === Margins ===

    @Test
    @DisplayName("a_negative_end_margin_cancels_the_end_padding_extension")
    void negativeEndMarginCancelsEndPadding() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle childStyle = sized(50f, 50f);
        childStyle.margin = TaffyRect.ltrb(
            LengthPercentageAuto.length(0f), LengthPercentageAuto.length(0f),
            LengthPercentageAuto.length(-30f), LengthPercentageAuto.length(-30f)
        );
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = sized(100f, 100f);
        rootStyle.overflow = SCROLL;
        rootStyle.padding = TaffyRect.ltrb(zero(), zero(), len(20f), len(20f));
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        // Without the negative margins the end padding would extend the rectangle to 70; the
        // clamped (padding + margin) term cancels it back to the border-box end.
        assertOverflow(layout(tree, root), 0f, 50f, 0f, 50f);
    }

    @Test
    @DisplayName("auto_margins_are_resolved_before_the_contribution_is_computed")
    void autoMarginsAreResolvedBeforeContribution() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle childStyle = sized(20f, 20f);
        childStyle.margin = TaffyRect.ltrb(
            LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0f),
            LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0f)
        );
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = sized(100f, 100f);
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        // The two auto margins absorb the free space (40 each), so the item's border box ends at
        // x = 60 and the contribution is measured from there.
        assertEquals(40f, layout(tree, child).location().x, EPSILON);
        Layout rootLayout = layout(tree, root);
        assertOverflow(rootLayout, 0f, 60f, 0f, 20f);
        assertContentSize(rootLayout, 60f, 20f);
        assertFalse(rootLayout.scrollableOverflow().reversedX());
    }

    // === Helpers ===

    private static LengthPercentage len(float value) {
        return LengthPercentage.length(value);
    }

    private static LengthPercentage zero() {
        return LengthPercentage.length(0f);
    }
}
