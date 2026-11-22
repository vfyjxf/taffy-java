package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P4 scrollable-overflow tests for the grid algorithm (SO-01/SO-02/SO-03).
 *
 * <p>Upstream #1118/#1372 replace {@code content_size} with a physical scrollable overflow
 * rectangle accumulated by each algorithm. This port keeps both components in 1.2
 * ({@link Layout#contentSize()} stays the legacy 1.x value until 2.0), so every case below
 * asserts the new per-side rectangle <em>and</em> the legacy content size, which is exactly the
 * difference the new rectangle fixes.</p>
 *
 * <p>Semantics checked here, mirroring upstream:
 * <ul>
 *   <li>a child's contribution is resolved in the container's padding-box frame, so the container
 *       border is subtracted from the child's location;</li>
 *   <li>a scroll container clips overflow before its scroll origin, keeps a box that crosses the
 *       origin only up to the origin ({@code left = 0} on a normal axis) and includes its own end
 *       padding;</li>
 *   <li>RTL puts the scroll origin at the right edge ({@code reversedX}) and makes the physical
 *       left side the reachable end;</li>
 *   <li>absolutely positioned children contribute through the out-of-flow frame (no container
 *       padding, no margin) and an "empty" grid (no in-flow item) still reports its own padding;</li>
 *   <li>a child container's rectangle propagates only where its own {@code overflow} allows it.</li>
 * </ul>
 */
public class GridScrollP4Test {

    private static final float EPSILON = 0.01f;

    private static final TaffyPoint<Overflow> VISIBLE =
        new TaffyPoint<>(Overflow.VISIBLE, Overflow.VISIBLE);
    private static final TaffyPoint<Overflow> SCROLL =
        new TaffyPoint<>(Overflow.SCROLL, Overflow.SCROLL);
    private static final TaffyPoint<Overflow> HIDDEN =
        new TaffyPoint<>(Overflow.HIDDEN, Overflow.HIDDEN);

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private static TaffyStyle gridStyle(float width, float height, TaffyDirection direction,
                                        TaffyPoint<Overflow> overflow) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        style.direction = direction;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        style.overflow = overflow;
        return style;
    }

    private static TaffyStyle withPadding(TaffyStyle style, float left, float right,
                                          float top, float bottom) {
        style.padding = new TaffyRect<>(
            LengthPercentage.length(left),
            LengthPercentage.length(right),
            LengthPercentage.length(top),
            LengthPercentage.length(bottom)
        );
        return style;
    }

    private static TaffyStyle leafStyle(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle withMarginLeft(TaffyStyle style, float marginLeft) {
        style.margin = new TaffyRect<>(
            LengthPercentageAuto.length(marginLeft),
            LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO
        );
        return style;
    }

    /**
     * An absolutely positioned child. {@code null} insets are {@code auto}, which makes the box
     * stay at the static position.
     */
    private static TaffyStyle absoluteStyle(float width, float height,
                                            Float left, Float right, Float top, Float bottom) {
        TaffyStyle style = leafStyle(width, height);
        style.position = TaffyPosition.ABSOLUTE;
        style.inset = new TaffyRect<>(
            left == null ? LengthPercentageAuto.AUTO : LengthPercentageAuto.length(left),
            right == null ? LengthPercentageAuto.AUTO : LengthPercentageAuto.length(right),
            top == null ? LengthPercentageAuto.AUTO : LengthPercentageAuto.length(top),
            bottom == null ? LengthPercentageAuto.AUTO : LengthPercentageAuto.length(bottom)
        );
        return style;
    }

    /** A two-level grid tree used by the propagation cases. */
    private record NestedGrid(TaffyTree tree, NodeId outer, NodeId inner) {
    }

    private static Layout compute(TaffyTree tree, NodeId root) {
        tree.disableRounding();
        tree.computeLayout(root, TaffySize.maxContent());
        return tree.getLayout(root);
    }

    // ------------------------------------------------------------------
    // Assertions
    // ------------------------------------------------------------------

    private static void assertRect(ScrollableOverflow overflow, float left, float right,
                                   float top, float bottom, String label) {
        assertEquals(left, overflow.left(), EPSILON, label + " left");
        assertEquals(right, overflow.right(), EPSILON, label + " right");
        assertEquals(top, overflow.top(), EPSILON, label + " top");
        assertEquals(bottom, overflow.bottom(), EPSILON, label + " bottom");
    }

    private static void assertContentSize(Layout layout, float width, float height, String label) {
        assertEquals(width, layout.contentSize().width, EPSILON, label + " contentSize.width");
        assertEquals(height, layout.contentSize().height, EPSILON, label + " contentSize.height");
    }

    // ------------------------------------------------------------------
    // In-flow children: non-scroll container
    // ------------------------------------------------------------------

    @Test
    @DisplayName("in_flow_item_overhanging_the_grid_extends_its_scrollable_overflow")
    void inFlowItemOverhangingTheGridExtendsItsScrollableOverflow() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(leafStyle(50f, 150f));
        NodeId root = tree.newWithChildren(
            gridStyle(100f, 100f, TaffyDirection.LTR, VISIBLE), child);

        Layout layout = compute(tree, root);

        // The 150px-tall item sits at the grid origin: its border box is the whole contribution.
        assertRect(layout.scrollableOverflow(), 0f, 50f, 0f, 150f, "grid");
        assertFalse(layout.scrollableOverflow().reversedX());
        assertFalse(layout.scrollableOverflow().reversedY());
        // The legacy component only knows the item's border box, so it records 50x150.
        assertContentSize(layout, 50f, 150f, "grid");
    }

    @Test
    @DisplayName("a_child_overflow_rectangle_propagates_through_a_visible_grid")
    void childOverflowRectanglePropagatesThroughVisibleGrid() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle childStyle = leafStyle(50f, 50f);
        childStyle.overflow = VISIBLE;
        // A fixed 50x50 leaf whose measured content is 150 wide: its border box stays 50, its own
        // scrollable overflow rectangle reaches 150 (upstream leaf.rs / SO-05).
        NodeId child = tree.newLeafWithMeasure(childStyle,
            (knownDimensions, availableSpace) -> new FloatSize(150f, 50f));
        NodeId root = tree.newWithChildren(
            gridStyle(100f, 100f, TaffyDirection.LTR, VISIBLE), child);

        Layout layout = compute(tree, root);
        Layout childLayout = tree.getLayout(child);

        assertEquals(150f, childLayout.scrollableOverflow().right(), EPSILON, "child right");
        assertEquals(50f, childLayout.scrollableOverflow().bottom(), EPSILON, "child bottom");
        // The child propagates on both axes, so the grid's rectangle includes the 150px content.
        assertRect(layout.scrollableOverflow(), 0f, 150f, 0f, 50f, "grid");
        // The legacy content size uses the item's border box (grid 1.x writes contentSize = size),
        // which is exactly the information the new rectangle adds.
        assertContentSize(layout, 50f, 50f, "grid");
    }

    @Test
    @DisplayName("a_child_border_moves_its_own_overflow_into_the_border_box")
    void childBorderMovesItsOwnOverflowIntoTheBorderBox() {
        TaffyTree tree = new TaffyTree();
        // A 50x50 grid child with a 5px border whose own 100px-wide content escapes it. The
        // child's rectangle is relative to its client area, so its left border shifts the
        // contributed end extent by 5 (upstream contribution adds the child's border.left).
        TaffyStyle innerStyle = gridStyle(50f, 50f, TaffyDirection.LTR, VISIBLE);
        innerStyle.border = new TaffyRect<>(
            LengthPercentage.length(5f),
            LengthPercentage.length(5f),
            LengthPercentage.length(5f),
            LengthPercentage.length(5f)
        );
        NodeId grandchild = tree.newLeaf(leafStyle(100f, 20f));
        NodeId child = tree.newWithChildren(innerStyle, grandchild);
        NodeId root = tree.newWithChildren(
            gridStyle(100f, 100f, TaffyDirection.LTR, VISIBLE), child);

        Layout layout = compute(tree, root);

        assertRect(tree.getLayout(child).scrollableOverflow(), 0f, 100f, 0f, 20f, "child");
        // The contribution is the union of the child's border box (50x50) and its content end
        // (100 + border.left): 105 wide, 50 tall.
        assertRect(layout.scrollableOverflow(), 0f, 105f, 0f, 50f, "grid");
        assertContentSize(layout, 50f, 50f, "grid");
    }

    @Test
    @DisplayName("visible_grid_does_not_add_its_own_end_padding")
    void visibleGridDoesNotAddItsOwnEndPadding() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(leafStyle(50f, 50f));
        TaffyStyle rootStyle = withPadding(
            gridStyle(200f, 100f, TaffyDirection.LTR, VISIBLE), 10f, 20f, 5f, 15f);
        NodeId root = tree.newWithChildren(rootStyle, child);

        Layout layout = compute(tree, root);

        // The item's border box starts after the start padding (10, 5) and ends at (60, 55). A box
        // that is not a scroll container does not extend its region by the end padding (20, 15).
        // The accumulator starts at the zero rectangle, so the region always contains the origin
        // (upstream grid accumulates from Rect::ZERO), hence left/top are 0, not 10/5.
        assertRect(layout.scrollableOverflow(), 0f, 60f, 0f, 55f, "grid");
        // Legacy: the item's border-box-frame location plus its border-box size (60, 55).
        assertContentSize(layout, 60f, 55f, "grid");
    }

    // ------------------------------------------------------------------
    // In-flow children: scroll container
    // ------------------------------------------------------------------

    @Test
    @DisplayName("scroll_grid_includes_its_own_end_padding")
    void scrollGridIncludesItsOwnEndPadding() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(leafStyle(50f, 50f));
        TaffyStyle rootStyle = withPadding(
            gridStyle(200f, 100f, TaffyDirection.LTR, SCROLL), 10f, 20f, 5f, 15f);
        NodeId root = tree.newWithChildren(rootStyle, child);

        Layout layout = compute(tree, root);

        // Start padding positions the item (10, 5); the scroll container's own end padding is part
        // of the reachable region, so the rectangle ends at 10 + 50 + 20 and 5 + 50 + 15.
        assertRect(layout.scrollableOverflow(), 0f, 80f, 0f, 70f, "grid");
        assertFalse(layout.scrollableOverflow().reversedX());
        assertContentSize(layout, 60f, 55f, "grid");
    }

    @Test
    @DisplayName("default_rounding_keeps_the_grid_overflow_rectangle")
    void defaultRoundingKeepsTheGridOverflowRectangle() {
        // Rounding is enabled by default: the rectangle computed during the layout pass must
        // survive RoundLayout instead of being reset by a compatibility-constructor rebuild.
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(leafStyle(50.3f, 50.7f));
        TaffyStyle rootStyle = withPadding(
            gridStyle(200f, 100f, TaffyDirection.LTR, SCROLL), 10.4f, 20.6f, 5.2f, 15.8f);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());
        Layout layout = tree.getLayout(root);

        // right = 10.4 + 50.3 + 20.6 = 81.3 -> 81; bottom = 5.2 + 50.7 + 15.8 = 71.7 -> 72.
        assertRect(layout.scrollableOverflow(), 0f, 81f, 0f, 72f, "grid");
    }

    @Test
    @DisplayName("scroll_grid_drops_overflow_wholly_before_the_scroll_origin")
    void scrollGridDropsOverflowWhollyBeforeTheScrollOrigin() {
        TaffyTree tree = new TaffyTree();
        // The negative start margin puts the whole item at x -100..-50, before the LTR origin.
        NodeId child = tree.newLeaf(withMarginLeft(leafStyle(50f, 50f), -100f));
        NodeId root = tree.newWithChildren(
            gridStyle(200f, 200f, TaffyDirection.LTR, HIDDEN), child);

        Layout layout = compute(tree, root);

        assertRect(layout.scrollableOverflow(), 0f, 0f, 0f, 0f, "grid");
        assertFalse(layout.scrollableOverflow().reversedX());
        // The legacy model has no unreachable region: it still records the item's extent, and the
        // max against the zero rectangle floors the negative side at 0.
        assertContentSize(layout, 0f, 50f, "grid");
    }

    @Test
    @DisplayName("scroll_grid_clips_an_item_that_crosses_the_scroll_origin")
    void scrollGridClipsItemCrossingTheScrollOrigin() {
        TaffyTree tree = new TaffyTree();
        // Straddles the origin: x -25..25.
        NodeId child = tree.newLeaf(withMarginLeft(leafStyle(50f, 50f), -25f));
        NodeId root = tree.newWithChildren(
            gridStyle(200f, 200f, TaffyDirection.LTR, HIDDEN), child);

        Layout layout = compute(tree, root);

        // The unreachable part before the origin is dropped, the reachable part is kept.
        assertRect(layout.scrollableOverflow(), 0f, 25f, 0f, 50f, "grid");
        assertContentSize(layout, 25f, 50f, "grid");
    }

    // ------------------------------------------------------------------
    // Empty grids (no in-flow item)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("absolute_only_scroll_grid_reports_its_end_padding")
    void absoluteOnlyScrollGridReportsItsEndPadding() {
        TaffyTree tree = new TaffyTree();
        NodeId absolute = tree.newLeaf(absoluteStyle(5f, 5f, 0f, null, 0f, null));
        TaffyStyle rootStyle = withPadding(
            gridStyle(200f, 200f, TaffyDirection.LTR, SCROLL), 0f, 20f, 0f, 30f);
        NodeId root = tree.newWithChildren(rootStyle, absolute);

        Layout layout = compute(tree, root);

        // With no in-flow item the only in-flow rectangle is the container's own end padding
        // (upstream absolute_only_containers_include_scroll_container_end_padding).
        assertRect(layout.scrollableOverflow(), 0f, 20f, 0f, 30f, "grid");
        assertContentSize(layout, 5f, 5f, "grid");
    }

    @Test
    @DisplayName("absolute_only_visible_grid_does_not_report_its_padding")
    void absoluteOnlyVisibleGridDoesNotReportItsPadding() {
        TaffyTree tree = new TaffyTree();
        NodeId absolute = tree.newLeaf(absoluteStyle(5f, 5f, 0f, null, 0f, null));
        TaffyStyle rootStyle = withPadding(
            gridStyle(200f, 200f, TaffyDirection.LTR, VISIBLE), 0f, 20f, 0f, 30f);
        NodeId root = tree.newWithChildren(rootStyle, absolute);

        Layout layout = compute(tree, root);

        // A box that is not a scroll container only contributes its children, never its padding.
        assertRect(layout.scrollableOverflow(), 0f, 5f, 0f, 5f, "grid");
        assertContentSize(layout, 5f, 5f, "grid");
    }

    @Test
    @DisplayName("absolute_child_wholly_before_the_scroll_origin_contributes_nothing")
    void absoluteChildWhollyBeforeTheScrollOriginContributesNothing() {
        TaffyTree tree = new TaffyTree();
        NodeId absolute = tree.newLeaf(absoluteStyle(50f, 50f, -100f, null, 0f, null));
        NodeId root = tree.newWithChildren(
            gridStyle(200f, 200f, TaffyDirection.LTR, HIDDEN), absolute);

        Layout layout = compute(tree, root);

        assertRect(layout.scrollableOverflow(), 0f, 0f, 0f, 0f, "grid");
        // The legacy content size keeps the out-of-flow box's extent.
        assertContentSize(layout, 0f, 50f, "grid");
    }

    @Test
    @DisplayName("absolute_child_crossing_the_scroll_origin_is_clipped_to_the_reachable_part")
    void absoluteChildCrossingTheScrollOriginIsClipped() {
        TaffyTree tree = new TaffyTree();
        NodeId absolute = tree.newLeaf(absoluteStyle(50f, 50f, -25f, null, 0f, null));
        NodeId root = tree.newWithChildren(
            gridStyle(200f, 200f, TaffyDirection.LTR, HIDDEN), absolute);

        Layout layout = compute(tree, root);

        assertRect(layout.scrollableOverflow(), 0f, 25f, 0f, 50f, "grid");
        assertContentSize(layout, 25f, 50f, "grid");
    }

    // ------------------------------------------------------------------
    // RTL: the scroll origin sits at the physical right edge
    // ------------------------------------------------------------------

    @Test
    @DisplayName("rtl_scroll_grid_keeps_reachable_left_overflow_and_reverses_the_x_origin")
    void rtlScrollGridKeepsReachableLeftOverflow() {
        TaffyTree tree = new TaffyTree();
        // In RTL the inline-end overflow is on the physical left.
        NodeId absolute = tree.newLeaf(absoluteStyle(50f, 50f, -50f, null, 0f, null));
        NodeId root = tree.newWithChildren(
            gridStyle(200f, 200f, TaffyDirection.RTL, SCROLL), absolute);

        Layout layout = compute(tree, root);

        assertTrue(layout.scrollableOverflow().reversedX(), "the rtl scroll origin is the right edge");
        assertFalse(layout.scrollableOverflow().reversedY());
        // The reachable end is the physical left; the reversed end (right) is clamped to the
        // client width (200), matching upstream's end_side_overflow_is_captured_for_rtl test.
        assertRect(layout.scrollableOverflow(), -50f, 200f, 0f, 50f, "grid");
        assertContentSize(layout, 0f, 50f, "grid");
    }

    @Test
    @DisplayName("rtl_absolute_child_past_the_right_scroll_origin_is_dropped")
    void rtlAbsoluteChildPastTheRightScrollOriginIsDropped() {
        TaffyTree tree = new TaffyTree();
        // A negative right inset puts the box past the right (start) edge, x = 200 + 100 - 50.
        NodeId absolute = tree.newLeaf(absoluteStyle(50f, 50f, null, -100f, 0f, null));
        NodeId root = tree.newWithChildren(
            gridStyle(200f, 200f, TaffyDirection.RTL, SCROLL), absolute);

        Layout layout = compute(tree, root);

        assertTrue(layout.scrollableOverflow().reversedX());
        assertEquals(250f, tree.getLayout(absolute).location().x, EPSILON, "static position");
        // Wholly unreachable boxes are excluded, so only the client area's reversed end remains.
        assertEquals(0f, layout.scrollableOverflow().left(), EPSILON, "left");
        assertEquals(200f, layout.scrollableOverflow().right(), EPSILON, "reversed end");
        // The legacy component is not clipped.
        assertContentSize(layout, 300f, 50f, "grid");
    }

    @Test
    @DisplayName("rtl_visible_grid_has_no_reversed_origin_and_still_keeps_the_client_area_end")
    void rtlVisibleGridHasNoReversedOrigin() {
        TaffyTree tree = new TaffyTree();
        NodeId absolute = tree.newLeaf(absoluteStyle(5f, 5f, 0f, null, 0f, null));
        NodeId root = tree.newWithChildren(
            gridStyle(200f, 100f, TaffyDirection.RTL, VISIBLE), absolute);

        Layout layout = compute(tree, root);

        assertFalse(layout.scrollableOverflow().reversedX(),
            "a non-scroll container has no reversed origin");
        // The upstream grid rectangle unions the client area's inline end in RTL even without
        // reachable children, so the rectangle spans the client width.
        assertRect(layout.scrollableOverflow(), 0f, 200f, 0f, 5f, "grid");
        assertContentSize(layout, 5f, 5f, "grid");
    }

    @Test
    @DisplayName("rtl_in_flow_item_sits_at_the_inline_start_and_marks_the_reversed_origin")
    void rtlInFlowItemMarksTheReversedOrigin() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(leafStyle(50f, 50f));
        NodeId root = tree.newWithChildren(
            gridStyle(200f, 100f, TaffyDirection.RTL, HIDDEN), child);

        Layout layout = compute(tree, root);

        // The item is placed at the inline start, which is the physical right edge (x 150..200).
        assertEquals(150f, tree.getLayout(child).location().x, EPSILON, "in-flow static position");
        assertTrue(layout.scrollableOverflow().reversedX());
        assertRect(layout.scrollableOverflow(), 0f, 200f, 0f, 50f, "grid");
        assertContentSize(layout, 200f, 50f, "grid");
    }

    // ------------------------------------------------------------------
    // Nested grid containers
    // ------------------------------------------------------------------

    private static NestedGrid nestedTree(TaffyPoint<Overflow> innerOverflow) {
        TaffyTree tree = new TaffyTree();
        NodeId grandchild = tree.newLeaf(leafStyle(300f, 300f));
        NodeId inner = tree.newWithChildren(
            gridStyle(100f, 100f, TaffyDirection.LTR, innerOverflow), grandchild);
        NodeId outer = tree.newWithChildren(
            gridStyle(200f, 200f, TaffyDirection.LTR, VISIBLE), inner);
        return new NestedGrid(tree, outer, inner);
    }

    @Test
    @DisplayName("a_nested_scroll_grid_clips_the_overflow_it_propagates")
    void nestedScrollGridClipsTheOverflowItPropagates() {
        NestedGrid nested = nestedTree(HIDDEN);
        TaffyTree tree = nested.tree();
        Layout outerLayout = compute(tree, nested.outer());
        Layout innerLayout = tree.getLayout(nested.inner());

        // The inner grid is a scroll container: its 300px content is clipped to the 100px client
        // area, and only its border box reaches the outer grid (upstream #1372).
        assertRect(innerLayout.scrollableOverflow(), 0f, 300f, 0f, 300f, "inner");
        assertRect(outerLayout.scrollableOverflow(), 0f, 100f, 0f, 100f, "outer");
        // Legacy: the grid writes each item's contentSize as its border-box size (1.x behaviour),
        // so the outer grid only sees the inner grid's 100x100 box, not its content.
        assertContentSize(outerLayout, 100f, 100f, "outer");
    }

    @Test
    @DisplayName("a_nested_visible_grid_propagates_its_overflow_rectangle")
    void nestedVisibleGridPropagatesItsOverflowRectangle() {
        NestedGrid nested = nestedTree(VISIBLE);
        TaffyTree tree = nested.tree();
        Layout outerLayout = compute(tree, nested.outer());
        Layout innerLayout = tree.getLayout(nested.inner());

        // Without clipping the inner rectangle escapes: the outer grid includes the 300px content.
        assertRect(innerLayout.scrollableOverflow(), 0f, 300f, 0f, 300f, "inner");
        assertRect(outerLayout.scrollableOverflow(), 0f, 300f, 0f, 300f, "outer");
        assertContentSize(outerLayout, 100f, 100f, "outer");
    }
}
