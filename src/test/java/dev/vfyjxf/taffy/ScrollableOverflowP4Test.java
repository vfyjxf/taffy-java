package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Baselines;
import dev.vfyjxf.taffy.tree.CollapsibleMarginSet;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.LayoutOutput;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.ScrollableOverflow;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.RoundLayout;
import dev.vfyjxf.taffy.util.ScrollableOverflowUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P4 scrollable-overflow tests for the core/leaf half of the package (SO-01/SO-02/SO-03/SO-05).
 *
 * <p>SO-03: {@code scrollWidth()}/{@code scrollHeight()} report the same maximum scroll offset no
 * matter which edge a border sits on, because the reachable range is the content extent less the
 * padding box (upstream #1007). The legacy {@code contentSize} component is still the input, so
 * those cases construct a {@link Layout} directly.</p>
 *
 * <p>SO-01/SO-02: {@link ScrollableOverflowUtil} mirrors upstream #1118/#1372: a child's
 * contribution is resolved in the parent's padding-box frame, unreachable overflow before the
 * scroll origin is excluded for scroll containers, a scroll container's own end padding is
 * included, and RTL uses the physical right/left edges. The rectangle contains the origin and the
 * reversed flags say where the scroll origin sits.</p>
 */
public class ScrollableOverflowP4Test {

    private static final float EPSILON = 0.01f;

    private static final TaffyPoint<Overflow> VISIBLE =
        new TaffyPoint<>(Overflow.VISIBLE, Overflow.VISIBLE);
    private static final TaffyPoint<Overflow> SCROLL =
        new TaffyPoint<>(Overflow.SCROLL, Overflow.SCROLL);

    private static Layout layoutWithContent(FloatSize size, FloatSize contentSize, FloatRect border) {
        return new Layout(
            0,
            FloatPoint.zero(),
            size,
            contentSize,
            FloatSize.zero(),
            border,
            FloatRect.zero(),
            FloatRect.zero()
        );
    }

    private static ScrollableOverflowUtil.Container scrollContainer(
        FloatSize clientSize, boolean reversedX, FloatRect padding
    ) {
        return new ScrollableOverflowUtil.Container(true, clientSize, reversedX, false, padding);
    }

    private static ScrollableOverflowUtil.Container visibleContainer(FloatSize clientSize) {
        return new ScrollableOverflowUtil.Container(false, clientSize, false, false, FloatRect.zero());
    }

    private static void assertContainsOrigin(ScrollableOverflow overflow) {
        FloatRect rect = overflow.rect();
        assertTrue(rect.left <= 0f, "left must be non-positive: " + rect);
        assertTrue(rect.top <= 0f, "top must be non-positive: " + rect);
        assertTrue(rect.right >= 0f, "right must be non-negative: " + rect);
        assertTrue(rect.bottom >= 0f, "bottom must be non-negative: " + rect);
    }

    // === SO-03: scrollWidth/scrollHeight are symmetric in the border ===

    @Test
    @DisplayName("scroll_width_is_symmetric_in_the_horizontal_border")
    void scrollWidthIsSymmetricInHorizontalBorder() {
        FloatSize size = new FloatSize(100f, 100f);
        FloatSize contentSize = new FloatSize(200f, 200f);

        // content (200) less the padding box: 100 with no border, 80 with a 20px border on either side.
        assertEquals(100f, layoutWithContent(size, contentSize, FloatRect.zero()).scrollWidth(), EPSILON);
        assertEquals(120f,
            layoutWithContent(size, contentSize, new FloatRect(20f, 0f, 0f, 0f)).scrollWidth(), EPSILON);
        assertEquals(120f,
            layoutWithContent(size, contentSize, new FloatRect(0f, 20f, 0f, 0f)).scrollWidth(), EPSILON);
        // Both borders are added, so the same padding box results from either edge.
        assertEquals(140f,
            layoutWithContent(size, contentSize, new FloatRect(20f, 20f, 0f, 0f)).scrollWidth(), EPSILON);
    }

    @Test
    @DisplayName("scroll_height_is_symmetric_in_the_vertical_border")
    void scrollHeightIsSymmetricInVerticalBorder() {
        FloatSize size = new FloatSize(100f, 100f);
        FloatSize contentSize = new FloatSize(200f, 200f);

        assertEquals(100f, layoutWithContent(size, contentSize, FloatRect.zero()).scrollHeight(), EPSILON);
        assertEquals(120f,
            layoutWithContent(size, contentSize, new FloatRect(0f, 0f, 20f, 0f)).scrollHeight(), EPSILON);
        assertEquals(120f,
            layoutWithContent(size, contentSize, new FloatRect(0f, 0f, 0f, 20f)).scrollHeight(), EPSILON);
        assertEquals(140f,
            layoutWithContent(size, contentSize, new FloatRect(0f, 0f, 20f, 20f)).scrollHeight(), EPSILON);
    }

    @Test
    @DisplayName("a_200px_scroller_reports_820_with_a_leading_or_trailing_border")
    void scrollerScrollRangeMatchesForLeadingAndTrailingBorder() {
        // Upstream #1007 example: a 200px border-box scroller holding 1000px of content. The
        // maximum offset is content less the padding box, so a 20px border belongs on either edge.
        FloatSize size = new FloatSize(200f, 200f);
        FloatSize contentSize = new FloatSize(1000f, 1000f);

        Layout topBorder = layoutWithContent(size, contentSize, new FloatRect(0f, 0f, 20f, 0f));
        Layout bottomBorder = layoutWithContent(size, contentSize, new FloatRect(0f, 0f, 0f, 20f));
        assertEquals(820f, topBorder.scrollHeight(), EPSILON);
        assertEquals(820f, bottomBorder.scrollHeight(), EPSILON);
        // The vertical border does not disturb the horizontal range: 1000 - 200.
        assertEquals(800f, topBorder.scrollWidth(), EPSILON);
        assertEquals(800f, bottomBorder.scrollWidth(), EPSILON);
    }

    @Test
    @DisplayName("scroll_ranges_are_floored_at_zero")
    void scrollRangesAreFlooredAtZero() {
        Layout layout = layoutWithContent(
            new FloatSize(100f, 100f),
            new FloatSize(40f, 40f),
            new FloatRect(10f, 10f, 10f, 10f)
        );
        assertEquals(0f, layout.scrollWidth(), EPSILON);
        assertEquals(0f, layout.scrollHeight(), EPSILON);
    }

    // === SO-02: per-child contributions ===

    @Test
    @DisplayName("contribution_is_in_the_parent_padding_box_and_adds_the_child_border")
    void contributionIsInParentPaddingBoxAndAddsChildBorder() {
        // Visible child at (10, 10) whose client-area overflow extends to (20, 30); its own border
        // shifts that rectangle to (25, 35) in its border-box frame, which becomes the contributed
        // end extent because the border box itself is only 10x10.
        FloatRect contribution = ScrollableOverflowUtil.computeContribution(
            new FloatPoint(10f, 10f),
            new FloatSize(10f, 10f),
            new FloatRect(0f, 20f, 0f, 30f),
            new FloatRect(5f, 0f, 5f, 0f),
            VISIBLE,
            visibleContainer(new FloatSize(200f, 200f)),
            FloatRect.zero()
        );

        assertEquals(10f, contribution.left, EPSILON);
        assertEquals(35f, contribution.right, EPSILON);
        assertEquals(10f, contribution.top, EPSILON);
        assertEquals(45f, contribution.bottom, EPSILON);
    }

    @Test
    @DisplayName("a_scroll_container_child_contributes_only_its_border_box")
    void scrollContainerChildContributesOnlyBorderBox() {
        // The child clips both axes: the 100px of reported overflow must not escape.
        FloatRect contribution = ScrollableOverflowUtil.computeContribution(
            new FloatPoint(10f, 10f),
            new FloatSize(10f, 10f),
            new FloatRect(0f, 100f, 0f, 100f),
            FloatRect.zero(),
            SCROLL,
            visibleContainer(new FloatSize(200f, 200f)),
            FloatRect.zero()
        );

        assertEquals(10f, contribution.left, EPSILON);
        assertEquals(20f, contribution.right, EPSILON);
        assertEquals(20f, contribution.bottom, EPSILON);
    }

    @Test
    @DisplayName("clip_in_one_axis_still_propagates_the_other")
    void clipInOneAxisStillPropagatesTheOther() {
        // Clip is not a scroll container: overflow clips its own axis but propagates the other.
        FloatRect contribution = ScrollableOverflowUtil.computeContribution(
            new FloatPoint(0f, 0f),
            new FloatSize(10f, 10f),
            new FloatRect(0f, 100f, 0f, 100f),
            FloatRect.zero(),
            new TaffyPoint<>(Overflow.CLIP, Overflow.VISIBLE),
            visibleContainer(new FloatSize(200f, 200f)),
            FloatRect.zero()
        );

        assertEquals(10f, contribution.right, EPSILON);
        assertEquals(100f, contribution.bottom, EPSILON);
    }

    @Test
    @DisplayName("a_zero_sized_child_contributes_nothing")
    void zeroSizedChildContributesNothing() {
        FloatRect contribution = ScrollableOverflowUtil.computeContribution(
            new FloatPoint(0f, 0f),
            new FloatSize(0f, 0f),
            FloatRect.zero(),
            FloatRect.zero(),
            VISIBLE,
            visibleContainer(new FloatSize(200f, 200f)),
            FloatRect.zero()
        );

        assertEquals(0f, contribution.left);
        assertEquals(0f, contribution.right);
        assertEquals(0f, contribution.top);
        assertEquals(0f, contribution.bottom);
    }

    @Test
    @DisplayName("a_child_wholly_before_the_scroll_origin_is_excluded_from_a_scroll_container")
    void whollyUnreachableChildIsExcludedFromScrollContainer() {
        // Child spans x/y -100..-50: entirely before the LTR scroll origin.
        FloatRect contribution = ScrollableOverflowUtil.computeContribution(
            new FloatPoint(-100f, -100f),
            new FloatSize(50f, 50f),
            FloatRect.zero(),
            FloatRect.zero(),
            VISIBLE,
            scrollContainer(new FloatSize(200f, 200f), false, FloatRect.zero()),
            FloatRect.zero()
        );

        assertEquals(0f, contribution.left);
        assertEquals(0f, contribution.right);
        assertEquals(0f, contribution.bottom);
    }

    @Test
    @DisplayName("a_child_crossing_the_scroll_origin_keeps_only_its_reachable_part")
    void partiallyUnreachableChildIsClippedAtTheOrigin() {
        // Child spans -25..25: partially unreachable, so it contributes its border box.
        FloatRect contribution = ScrollableOverflowUtil.computeContribution(
            new FloatPoint(-25f, -25f),
            new FloatSize(50f, 50f),
            FloatRect.zero(),
            FloatRect.zero(),
            VISIBLE,
            scrollContainer(new FloatSize(200f, 200f), false, FloatRect.zero()),
            FloatRect.zero()
        );
        assertEquals(-25f, contribution.left, EPSILON);
        assertEquals(25f, contribution.right, EPSILON);

        // The algorithm accumulates from the origin and clips at the output boundary.
        ScrollableOverflow overflow = ScrollableOverflowUtil.finalizeScrollableOverflow(
            ScrollableOverflowUtil.union(FloatRect.zero(), contribution),
            new FloatSize(200f, 200f),
            false,
            false,
            true
        );
        assertEquals(0f, overflow.left(), EPSILON);
        assertEquals(25f, overflow.right(), EPSILON);
        assertFalse(overflow.reversedX());
        assertFalse(overflow.reversedY());
    }

    @Test
    @DisplayName("a_scroll_container_parent_includes_its_own_end_padding")
    void scrollContainerParentIncludesEndPadding() {
        FloatRect contribution = ScrollableOverflowUtil.computeContribution(
            new FloatPoint(0f, 0f),
            new FloatSize(50f, 50f),
            FloatRect.zero(),
            FloatRect.zero(),
            VISIBLE,
            scrollContainer(
                new FloatSize(200f, 200f), false,
                new FloatRect(10f, 20f, 30f, 40f)
            ),
            FloatRect.zero()
        );

        // End padding right/bottom extends the end edges; start padding does not move the origin.
        assertEquals(0f, contribution.left, EPSILON);
        assertEquals(70f, contribution.right, EPSILON);
        assertEquals(0f, contribution.top, EPSILON);
        assertEquals(90f, contribution.bottom, EPSILON);
    }

    @Test
    @DisplayName("a_visible_parent_does_not_add_its_own_end_padding")
    void visibleParentDoesNotAddEndPadding() {
        FloatRect contribution = ScrollableOverflowUtil.computeContribution(
            new FloatPoint(0f, 0f),
            new FloatSize(50f, 50f),
            FloatRect.zero(),
            FloatRect.zero(),
            VISIBLE,
            new ScrollableOverflowUtil.Container(
                false, new FloatSize(200f, 200f), false, false,
                new FloatRect(10f, 20f, 30f, 40f)
            ),
            FloatRect.zero()
        );

        assertEquals(50f, contribution.right, EPSILON);
        assertEquals(50f, contribution.bottom, EPSILON);
    }

    @Test
    @DisplayName("a_negative_end_margin_cancels_the_end_padding_but_never_inverts_it")
    void negativeEndMarginCancelsEndPadding() {
        FloatRect cancelling = ScrollableOverflowUtil.computeContribution(
            new FloatPoint(0f, 0f),
            new FloatSize(50f, 50f),
            FloatRect.zero(),
            FloatRect.zero(),
            VISIBLE,
            scrollContainer(
                new FloatSize(200f, 200f), false,
                new FloatRect(0f, 20f, 0f, 20f)
            ),
            new FloatRect(0f, -30f, 0f, -30f)
        );
        // padding 20 + margin -30 -> max(0, -10) = 0, so the end edge stays at the border box edge.
        assertEquals(50f, cancelling.right, EPSILON);
        assertEquals(50f, cancelling.bottom, EPSILON);

        FloatRect shrunk = ScrollableOverflowUtil.computeContribution(
            new FloatPoint(0f, 0f),
            new FloatSize(50f, 50f),
            FloatRect.zero(),
            FloatRect.zero(),
            VISIBLE,
            scrollContainer(
                new FloatSize(200f, 200f), false,
                new FloatRect(0f, 20f, 0f, 20f)
            ),
            new FloatRect(0f, -5f, 0f, -5f)
        );
        assertEquals(65f, shrunk.right, EPSILON);
        assertEquals(65f, shrunk.bottom, EPSILON);
    }

    @Test
    @DisplayName("rtl_scroll_containers_add_end_padding_on_the_left")
    void rtlScrollContainerAddsEndPaddingOnLeft() {
        // Reversed origin at the right edge: the end side is the physical left.
        FloatRect contribution = ScrollableOverflowUtil.computeContribution(
            new FloatPoint(0f, 0f),
            new FloatSize(50f, 50f),
            FloatRect.zero(),
            FloatRect.zero(),
            VISIBLE,
            scrollContainer(
                new FloatSize(200f, 200f), true,
                new FloatRect(20f, 0f, 0f, 0f)
            ),
            FloatRect.zero()
        );

        assertEquals(-20f, contribution.left, EPSILON);
        assertEquals(50f, contribution.right, EPSILON);
    }

    @Test
    @DisplayName("a_child_past_the_rtl_scroll_origin_is_unreachable")
    void childPastRtlScrollOriginIsUnreachable() {
        // In RTL the origin is the right edge: a child starting at x = parent width is wholly past it.
        FloatRect contribution = ScrollableOverflowUtil.computeContribution(
            new FloatPoint(200f, 0f),
            new FloatSize(50f, 50f),
            FloatRect.zero(),
            FloatRect.zero(),
            VISIBLE,
            scrollContainer(new FloatSize(200f, 200f), true, FloatRect.zero()),
            FloatRect.zero()
        );

        assertEquals(0f, contribution.left);
        assertEquals(0f, contribution.right);
    }

    @Test
    @DisplayName("rtl_contributions_are_mirrored_by_the_finalized_rectangle")
    void rtlContributionIsMirroredByFinalizedRectangle() {
        // Child spans x -50..0 at the left of a 200-wide reversed viewport: reachable end overflow.
        FloatRect contribution = ScrollableOverflowUtil.computeContribution(
            new FloatPoint(-50f, 0f),
            new FloatSize(50f, 50f),
            FloatRect.zero(),
            FloatRect.zero(),
            VISIBLE,
            scrollContainer(new FloatSize(200f, 200f), true, FloatRect.zero()),
            FloatRect.zero()
        );
        assertEquals(-50f, contribution.left, EPSILON);
        assertEquals(0f, contribution.right, EPSILON);

        ScrollableOverflow overflow = ScrollableOverflowUtil.finalizeScrollableOverflow(
            ScrollableOverflowUtil.union(FloatRect.zero(), contribution),
            new FloatSize(200f, 200f),
            true,
            false,
            true
        );
        assertTrue(overflow.reversedX(), "the scroll origin is at the right edge");
        assertEquals(-50f, overflow.left(), EPSILON);
        assertEquals(0f, overflow.right(), EPSILON);
    }

    @Test
    @DisplayName("non_scroll_containers_keep_start_side_overflow_and_have_no_reversed_origin")
    void nonScrollContainerKeepsStartSideOverflow() {
        FloatRect contribution = ScrollableOverflowUtil.computeContribution(
            new FloatPoint(-100f, -100f),
            new FloatSize(50f, 50f),
            FloatRect.zero(),
            FloatRect.zero(),
            VISIBLE,
            visibleContainer(new FloatSize(200f, 200f)),
            FloatRect.zero()
        );
        assertEquals(-100f, contribution.left, EPSILON);
        assertEquals(-50f, contribution.right, EPSILON);

        ScrollableOverflow overflow = ScrollableOverflowUtil.finalizeScrollableOverflow(
            ScrollableOverflowUtil.union(FloatRect.zero(), contribution),
            new FloatSize(200f, 200f),
            true,
            true,
            false
        );
        assertEquals(-100f, overflow.left(), EPSILON);
        assertEquals(0f, overflow.right(), EPSILON);
        assertFalse(overflow.reversedX(), "non-scroll containers have no reversed origin");
        assertFalse(overflow.reversedY());
    }

    @Test
    @DisplayName("union_merges_contributions_independently_of_order")
    void unionMergesContributionsIndependentlyOfOrder() {
        FloatRect a = new FloatRect(-10f, 20f, -5f, 30f);
        FloatRect b = new FloatRect(-20f, 10f, 0f, 40f);

        FloatRect merged = ScrollableOverflowUtil.union(a, b);
        assertEquals(-20f, merged.left, EPSILON);
        assertEquals(20f, merged.right, EPSILON);
        assertEquals(-5f, merged.top, EPSILON);
        assertEquals(40f, merged.bottom, EPSILON);

        FloatRect all = ScrollableOverflowUtil.unionAll(List.of(a, b));
        assertEquals(merged.left, all.left, EPSILON);
        assertEquals(merged.right, all.right, EPSILON);
        assertEquals(merged.top, all.top, EPSILON);
        assertEquals(merged.bottom, all.bottom, EPSILON);

        assertEquals(0f, ScrollableOverflowUtil.unionAll(List.of()).left);
        assertEquals(0f, ScrollableOverflowUtil.unionAll(List.of()).right);
    }

    // === SO-01/SO-02: output boundary and leaf rectangle ===

    @Test
    @DisplayName("finalize_clips_the_unreachable_start_of_a_normal_scroll_container")
    void finalizeClipsUnreachableStartOfNormalScrollContainer() {
        ScrollableOverflow overflow = ScrollableOverflowUtil.finalizeScrollableOverflow(
            new FloatRect(-10f, 250f, -5f, 300f),
            new FloatSize(200f, 200f),
            false,
            false,
            true
        );

        assertEquals(0f, overflow.left(), EPSILON);
        assertEquals(250f, overflow.right(), "end overflow is the scrollable part");
        assertEquals(0f, overflow.top(), EPSILON);
        assertEquals(300f, overflow.bottom(), EPSILON);
        assertFalse(overflow.reversedX());
        assertFalse(overflow.reversedY());
    }

    @Test
    @DisplayName("finalize_clamps_the_end_of_a_reversed_scroll_container_to_the_client_extent")
    void finalizeClampsReversedEndToClientExtent() {
        ScrollableOverflow overflow = ScrollableOverflowUtil.finalizeScrollableOverflow(
            new FloatRect(-30f, 250f, -50f, 300f),
            new FloatSize(200f, 200f),
            true,
            true,
            true
        );

        assertEquals(-30f, overflow.left(), "the reachable scroll range is the start side");
        assertEquals(200f, overflow.right(), "the reversed end is clamped to the client width");
        assertEquals(-50f, overflow.top(), EPSILON);
        assertEquals(200f, overflow.bottom(), "the reversed end is clamped to the client height");
        assertTrue(overflow.reversedX());
        assertTrue(overflow.reversedY());
    }

    @Test
    @DisplayName("non_scroll_leaf_does_not_include_its_end_padding")
    void nonScrollLeafDoesNotIncludeEndPadding() {
        // Measured content 50x20, padding 10/20/30/40, LTR, overflow visible.
        ScrollableOverflow overflow = ScrollableOverflowUtil.leafScrollableOverflow(
            new FloatSize(200f, 200f),
            FloatRect.zero(),
            new FloatRect(10f, 20f, 30f, 40f),
            0f,
            VISIBLE,
            new FloatSize(50f, 20f),
            false
        );

        assertEquals(0f, overflow.left(), EPSILON);
        assertEquals(60f, overflow.right(), "start padding 10 + content 50, no end padding");
        assertEquals(0f, overflow.top(), EPSILON);
        assertEquals(50f, overflow.bottom(), "top padding 30 + content 20, no end padding");
        assertFalse(overflow.reversedX());
        assertFalse(overflow.reversedY());
    }

    @Test
    @DisplayName("scroll_container_leaf_includes_its_end_padding")
    void scrollContainerLeafIncludesEndPadding() {
        ScrollableOverflow overflow = ScrollableOverflowUtil.leafScrollableOverflow(
            new FloatSize(200f, 200f),
            FloatRect.zero(),
            new FloatRect(10f, 20f, 30f, 40f),
            0f,
            SCROLL,
            new FloatSize(50f, 20f),
            false
        );

        assertEquals(0f, overflow.left(), EPSILON);
        assertEquals(80f, overflow.right(), "start 10 + content 50 + end 20");
        assertEquals(0f, overflow.top(), EPSILON);
        assertEquals(90f, overflow.bottom(), "top 30 + content 20 + end 40");
    }

    @Test
    @DisplayName("rtl_leaf_scrollable_overflow_is_mirrored_around_the_right_client_edge")
    void rtlLeafScrollableOverflowIsMirrored() {
        // Same geometry as the scroll-container leaf above, but RTL and with content wider than the
        // client area: start padding is the physical right one and the rectangle grows leftwards.
        ScrollableOverflow overflow = ScrollableOverflowUtil.leafScrollableOverflow(
            new FloatSize(200f, 200f),
            FloatRect.zero(),
            new FloatRect(10f, 20f, 30f, 40f),
            0f,
            SCROLL,
            new FloatSize(300f, 20f),
            true
        );

        assertTrue(overflow.reversedX(), "RTL puts the scroll origin at the right edge");
        assertEquals(200f, overflow.right(), "the reversed end is the client width");
        // content end = start(right) 20 + content 300 + end(left) 10 = 330; client width 200.
        assertEquals(-130f, overflow.left(), EPSILON);
        assertEquals(90f, overflow.bottom(), "the block axis is unaffected by RTL");
        assertContainsOrigin(overflow);
    }

    @Test
    @DisplayName("ltr_and_rtl_scroll_leaves_report_the_same_reachable_range")
    void ltrAndRtlScrollLeavesReportTheSameReachableRange() {
        FloatRect padding = new FloatRect(10f, 20f, 0f, 0f);
        FloatSize measured = new FloatSize(300f, 20f);

        ScrollableOverflow ltr = ScrollableOverflowUtil.leafScrollableOverflow(
            new FloatSize(100f, 100f), FloatRect.zero(), padding, 0f, SCROLL, measured, false);
        ScrollableOverflow rtl = ScrollableOverflowUtil.leafScrollableOverflow(
            new FloatSize(100f, 100f), FloatRect.zero(), padding, 0f, SCROLL, measured, true);

        // LTR reachable range = content rectangle right edge - client width.
        assertEquals(330f - 100f, ltr.right() - 100f, EPSILON);
        // RTL reachable range = the start-side extent of the mirrored rectangle.
        assertEquals(330f - 100f, -rtl.left(), EPSILON);
    }

    @Test
    @DisplayName("leaf_overflow_contains_the_origin")
    void leafOverflowContainsTheOrigin() {
        ScrollableOverflow ltr = ScrollableOverflowUtil.leafScrollableOverflow(
            new FloatSize(100f, 100f), FloatRect.zero(), FloatRect.hv(10f, 10f),
            0f, VISIBLE, new FloatSize(50f, 50f), false);
        ScrollableOverflow rtl = ScrollableOverflowUtil.leafScrollableOverflow(
            new FloatSize(100f, 100f), FloatRect.zero(), FloatRect.hv(10f, 10f),
            0f, VISIBLE, new FloatSize(50f, 50f), true);
        ScrollableOverflow scrollLtr = ScrollableOverflowUtil.leafScrollableOverflow(
            new FloatSize(100f, 100f), FloatRect.zero(), FloatRect.hv(10f, 10f),
            0f, SCROLL, new FloatSize(50f, 50f), false);

        assertContainsOrigin(ltr);
        assertContainsOrigin(rtl);
        assertContainsOrigin(scrollLtr);
    }

    @Test
    @DisplayName("rounding_preserves_the_scrollable_overflow_rectangle")
    void roundingPreservesScrollableOverflow() {
        TaffyTree tree = new TaffyTree();
        NodeId node = tree.newLeaf(leafStyle(100f, 100f, TaffyDirection.LTR, SCROLL));
        tree.setUnroundedLayout(node, new Layout(
            0,
            new FloatPoint(0.4f, 0.4f),
            new FloatSize(10.6f, 5.4f),
            new FloatSize(30.4f, 20.6f),
            new ScrollableOverflow(new FloatRect(-0.3f, 30.6f, -0.4f, 12.4f), false, false),
            new FloatSize(1.4f, 0.6f),
            FloatRect.zero(),
            FloatRect.zero(),
            FloatRect.zero()
        ));

        RoundLayout.roundLayout(tree, node);

        ScrollableOverflow rounded = tree.getLayout(node).scrollableOverflow();
        assertEquals(31f, rounded.right(), EPSILON, "30.6 rounds to 31 instead of being dropped");
        assertEquals(12f, rounded.bottom(), EPSILON, "12.4 rounds to 12");
        assertEquals(
            Float.floatToRawIntBits(-0.0f),
            Float.floatToRawIntBits(rounded.left()),
            "a -0.0 start edge must survive rounding with its sign"
        );
        assertEquals(
            Float.floatToRawIntBits(-0.0f),
            Float.floatToRawIntBits(rounded.top()),
            "a -0.0 start edge must survive rounding with its sign"
        );
        assertFalse(rounded.reversedX());
        assertFalse(rounded.reversedY());
    }

    // === SO-01: ScrollableOverflow rectangle semantics ===

    @Test
    @DisplayName("transpose_swaps_the_physical_axes_and_the_reversed_origins")
    void transposeSwapsAxesAndReversedOrigins() {
        ScrollableOverflow overflow = new ScrollableOverflow(
            new FloatRect(-5f, 10f, -3f, 7f), true, false);

        ScrollableOverflow transposed = overflow.transpose();

        assertEquals(-3f, transposed.left(), EPSILON);
        assertEquals(7f, transposed.right(), EPSILON);
        assertEquals(-5f, transposed.top(), EPSILON);
        assertEquals(10f, transposed.bottom(), EPSILON);
        assertFalse(transposed.reversedX());
        assertTrue(transposed.reversedY());
        // The source value is untouched; instances are value objects.
        assertEquals(-5f, overflow.left(), EPSILON);
        assertTrue(overflow.reversedX());
    }

    @Test
    @DisplayName("round_preserves_signed_zero_and_rounds_away_from_zero")
    void roundPreservesSignedZero() {
        ScrollableOverflow rounded = new ScrollableOverflow(
            new FloatRect(-1.4f, 10.6f, -0.4f, 3.5f), false, false
        ).round();

        assertEquals(-1f, rounded.left(), EPSILON);
        assertEquals(11f, rounded.right(), EPSILON);
        assertEquals(4f, rounded.bottom(), EPSILON);
        assertEquals(
            Float.floatToRawIntBits(-0.0f),
            Float.floatToRawIntBits(rounded.top()),
            "a -0.0 edge must survive rounding with its sign"
        );
        assertContainsOrigin(rounded);
    }

    // === SO-05: leaf content size and Layout/LayoutOutput wiring ===

    private static TaffyStyle leafStyle(float width, float height, TaffyDirection direction,
                                        TaffyPoint<Overflow> overflow) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        style.padding = new TaffyRect<>(
            LengthPercentage.length(10f),
            LengthPercentage.length(20f),
            LengthPercentage.length(5f),
            LengthPercentage.length(15f)
        );
        style.direction = direction;
        style.overflow = overflow;
        return style;
    }

    @Test
    @DisplayName("non_scroll_leaf_content_size_excludes_end_padding")
    void nonScrollLeafContentSizeExcludesEndPadding() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId node = tree.newLeafWithMeasure(
            leafStyle(100f, 100f, TaffyDirection.LTR, VISIBLE),
            (knownDimensions, availableSpace) -> new FloatSize(50f, 20f)
        );

        tree.computeLayout(node, TaffySize.maxContent());
        Layout layout = tree.getLayout(node);

        // start padding + measured: 10 + 50 (not 10 + 50 + 20); 5 + 20 (not 5 + 20 + 15).
        assertEquals(60f, layout.contentSize().width, EPSILON);
        assertEquals(25f, layout.contentSize().height, EPSILON);
        assertEquals(60f, layout.scrollableOverflow().right(), EPSILON);
        assertEquals(25f, layout.scrollableOverflow().bottom(), EPSILON);
    }

    @Test
    @DisplayName("scroll_container_leaf_content_size_includes_end_padding")
    void scrollContainerLeafContentSizeIncludesEndPadding() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId node = tree.newLeafWithMeasure(
            leafStyle(100f, 100f, TaffyDirection.LTR, SCROLL),
            (knownDimensions, availableSpace) -> new FloatSize(50f, 20f)
        );

        tree.computeLayout(node, TaffySize.maxContent());
        Layout layout = tree.getLayout(node);

        assertEquals(80f, layout.contentSize().width, EPSILON, "10 + 50 + 20");
        assertEquals(40f, layout.contentSize().height, EPSILON, "5 + 20 + 15");
        assertEquals(80f, layout.scrollableOverflow().right(), EPSILON);
        assertEquals(40f, layout.scrollableOverflow().bottom(), EPSILON);
    }

    @Test
    @DisplayName("rtl_leaf_content_size_swaps_start_and_end_padding")
    void rtlLeafContentSizeSwapsStartAndEndPadding() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId nonScroll = tree.newLeafWithMeasure(
            leafStyle(100f, 100f, TaffyDirection.RTL, VISIBLE),
            (knownDimensions, availableSpace) -> new FloatSize(50f, 20f)
        );
        NodeId scroll = tree.newLeafWithMeasure(
            leafStyle(100f, 100f, TaffyDirection.RTL, SCROLL),
            (knownDimensions, availableSpace) -> new FloatSize(50f, 20f)
        );

        tree.computeLayout(nonScroll, TaffySize.maxContent());
        assertEquals(70f, tree.getLayout(nonScroll).contentSize().width, EPSILON,
            "RTL start padding is the physical right one: 20 + 50");

        tree.computeLayout(scroll, TaffySize.maxContent());
        assertEquals(80f, tree.getLayout(scroll).contentSize().width, EPSILON, "20 + 50 + 10");
    }

    @Test
    @DisplayName("overflowing_leaf_reports_scrollable_overflow_and_scroll_size")
    void overflowingLeafReportsScrollableOverflowAndScrollSize() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId node = tree.newLeafWithMeasure(
            leafStyle(100f, 100f, TaffyDirection.LTR, SCROLL),
            (knownDimensions, availableSpace) -> new FloatSize(300f, 200f)
        );

        tree.computeLayout(node, TaffySize.maxContent());
        Layout layout = tree.getLayout(node);

        assertEquals(100f, layout.size().width, EPSILON);
        assertEquals(100f, layout.size().height, EPSILON);
        assertEquals(330f, layout.contentSize().width, EPSILON, "10 + 300 + 20");
        assertEquals(220f, layout.contentSize().height, EPSILON, "5 + 200 + 15");
        assertEquals(330f, layout.scrollableOverflow().right(), EPSILON);
        assertEquals(220f, layout.scrollableOverflow().bottom(), EPSILON);
        assertEquals(230f, layout.scrollWidth(), EPSILON, "content 330 less the 100px padding box");
        assertEquals(120f, layout.scrollHeight(), EPSILON, "content 220 less the 100px padding box");
    }

    @Test
    @DisplayName("rtl_overflowing_leaf_mirrors_the_overflow_rectangle")
    void rtlOverflowingLeafMirrorsTheOverflowRectangle() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId node = tree.newLeafWithMeasure(
            leafStyle(100f, 100f, TaffyDirection.RTL, SCROLL),
            (knownDimensions, availableSpace) -> new FloatSize(300f, 200f)
        );

        tree.computeLayout(node, TaffySize.maxContent());
        Layout layout = tree.getLayout(node);

        ScrollableOverflow overflow = layout.scrollableOverflow();
        assertTrue(overflow.reversedX(), "RTL puts the scroll origin at the right edge");
        assertEquals(100f, overflow.right(), EPSILON, "the mirrored rectangle ends at the client width");
        assertEquals(-230f, overflow.left(), EPSILON, "client width 100 - content end 330");
        assertEquals(230f, layout.scrollWidth(), EPSILON);
        assertEquals(120f, layout.scrollHeight(), EPSILON);
    }

    @Test
    @DisplayName("measure_reported_overflow_wins_over_the_computed_leaf_rectangle")
    void measuredOverflowWinsOverComputedLeafOverflow() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        ScrollableOverflow reported = new ScrollableOverflow(
            new FloatRect(0f, 60f, 0f, 25f), false, false);
        NodeId node = tree.newLeafWithMeasureFunction(
            leafStyle(100f, 100f, TaffyDirection.LTR, VISIBLE),
            context -> new LayoutOutput(
                new FloatSize(50f, 20f),
                FloatSize.zero(),
                reported,
                new FloatPoint(Float.NaN, Float.NaN),
                Baselines.NONE,
                CollapsibleMarginSet.ZERO,
                CollapsibleMarginSet.ZERO,
                false
            )
        );

        tree.computeLayout(node, TaffySize.maxContent());

        assertEquals(60f, tree.getLayout(node).scrollableOverflow().right(), EPSILON);
        assertEquals(25f, tree.getLayout(node).scrollableOverflow().bottom(), EPSILON);
    }

    @Test
    @DisplayName("default_rounding_keeps_the_leaf_overflow_reachable")
    void defaultRoundingKeepsLeafOverflow() {
        // Rounding is enabled by default: the leaf rectangle computed by LayoutComputer must survive
        // RoundLayout instead of being reset to ZERO by a compatibility-constructor rebuild.
        TaffyTree tree = new TaffyTree();
        NodeId node = tree.newLeafWithMeasure(
            leafStyle(100f, 100f, TaffyDirection.LTR, SCROLL),
            (knownDimensions, availableSpace) -> new FloatSize(300f, 200f)
        );

        tree.computeLayout(node, TaffySize.maxContent());
        Layout layout = tree.getLayout(node);

        assertEquals(330f, layout.scrollableOverflow().right(), EPSILON, "10 + 300 + 20");
        assertEquals(220f, layout.scrollableOverflow().bottom(), EPSILON, "5 + 200 + 15");
        assertEquals(230f, layout.scrollWidth(), EPSILON, "content 330 less the 100px padding box");
    }

    @Test
    @DisplayName("rect_contains_the_origin_for_computed_overflows")
    void computedOverflowsContainTheOrigin() {
        ScrollableOverflow contributionOverflow = ScrollableOverflowUtil.finalizeScrollableOverflow(
            new FloatRect(-40f, 250f, -20f, 300f),
            new FloatSize(200f, 200f),
            true,
            false,
            true
        );
        assertContainsOrigin(contributionOverflow);

        ScrollableOverflow nonScroll = ScrollableOverflowUtil.finalizeScrollableOverflow(
            new FloatRect(-40f, 250f, -20f, 300f),
            new FloatSize(200f, 200f),
            false,
            false,
            false
        );
        assertContainsOrigin(nonScroll);
        assertEquals(-40f, nonScroll.left(), EPSILON);
        assertEquals(250f, nonScroll.right(), EPSILON);
    }
}
