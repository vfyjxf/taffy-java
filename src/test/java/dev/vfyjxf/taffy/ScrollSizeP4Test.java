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
import dev.vfyjxf.taffy.style.TaffyDisplay;
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
 * P4 scroll-size integration tests (SO-01/SO-03, upstream #1007/#1372).
 *
 * <p>The maximum scroll offset is the reachable content extent less the padding box, so it must
 * not depend on which physical edge a border or the end padding sits on. This suite exercises the
 * integration switch: {@link Layout#scrollWidth()}/{@link Layout#scrollHeight()} now derive the
 * range from the {@link Layout#scrollableOverflow() scrollable-overflow rectangle} carried by
 * block, flex and grid layouts (and by measured leaves), using the reversed-origin flags for RTL
 * and bottom-to-top axes. The legacy {@link Layout#contentSize()} formula remains only for
 * layouts built by hand through the compatibility constructors.</p>
 *
 * <p>Values follow upstream {@code tests/hand_written/scroll_size.rs} and the reversed-origin
 * cases of {@code scrollable_overflow.rs}: a normal axis reports the content extent beyond the
 * padding-box end edge, while a reversed axis reports how far the content end extends past the
 * client area's physical start edge ({@code -left} / {@code -top}).</p>
 */
public class ScrollSizeP4Test {

    private static final float EPSILON = 0.01f;

    private static final float CONTENT = 1000f;
    private static final float CONTAINER = 200f;
    private static final float BORDER = 20f;
    private static final float PADDING = 20f;

    // === Fixture helpers ===

    private static TaffyStyle box(TaffyDisplay display, float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = display;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle scroller(TaffyDisplay display, float width, float height) {
        TaffyStyle style = box(display, width, height);
        style.overflow = new TaffyPoint<>(Overflow.SCROLL, Overflow.SCROLL);
        return style;
    }

    /** A fixed-size child that flex must not shrink away; block and grid ignore the shrink. */
    private static TaffyStyle child(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        style.flexShrink = 0f;
        return style;
    }

    /**
     * Borders on the block axis only: {@code top}/{@code bottom} are the physical edges.
     * The argument order is (top, bottom) because that is what the scroll range depends on.
     */
    private static TaffyStyle borders(TaffyStyle style, float top, float bottom) {
        style.border = new TaffyRect<>(
            LengthPercentage.length(0f),
            LengthPercentage.length(0f),
            LengthPercentage.length(top),
            LengthPercentage.length(bottom)
        );
        return style;
    }

    /** Borders on the inline axis only, in physical (left, right) order. */
    private static TaffyStyle horizontalBorders(TaffyStyle style, float left, float right) {
        style.border = new TaffyRect<>(
            LengthPercentage.length(left),
            LengthPercentage.length(right),
            LengthPercentage.length(0f),
            LengthPercentage.length(0f)
        );
        return style;
    }

    /** Padding on the block axis only, in physical (top, bottom) order. */
    private static TaffyStyle blockPadding(TaffyStyle style, float top, float bottom) {
        style.padding = new TaffyRect<>(
            LengthPercentage.length(0f),
            LengthPercentage.length(0f),
            LengthPercentage.length(top),
            LengthPercentage.length(bottom)
        );
        return style;
    }

    private static TaffyStyle padding(TaffyStyle style, float left, float right, float top, float bottom) {
        style.padding = new TaffyRect<>(
            LengthPercentage.length(left),
            LengthPercentage.length(right),
            LengthPercentage.length(top),
            LengthPercentage.length(bottom)
        );
        return style;
    }

    /** Lays out a single-child scroller and returns the container's layout. */
    private static Layout computeScroller(TaffyDisplay display, TaffyStyle rootStyle) {
        TaffyTree tree = new TaffyTree();
        NodeId content = tree.newLeaf(child(100f, CONTENT));
        NodeId root = tree.newWithChildren(rootStyle, content);
        return compute(tree, root);
    }

    private static Layout compute(TaffyTree tree, NodeId root) {
        tree.disableRounding();
        tree.computeLayout(root, TaffySize.maxContent());
        Layout layout = tree.getLayout(root);
        assertTrue(layout != null, "root layout");
        return layout;
    }

    private static void assertOverflow(ScrollableOverflow overflow,
                                       float left, float right, float top, float bottom,
                                       boolean reversedX, boolean reversedY, String message) {
        assertEquals(left, overflow.left(), EPSILON, message + " left");
        assertEquals(right, overflow.right(), EPSILON, message + " right");
        assertEquals(top, overflow.top(), EPSILON, message + " top");
        assertEquals(bottom, overflow.bottom(), EPSILON, message + " bottom");
        assertEquals(reversedX, overflow.reversedX(), message + " reversedX");
        assertEquals(reversedY, overflow.reversedY(), message + " reversedY");
    }

    private static final TaffyDisplay[] CONTAINER_DISPLAYS =
        {TaffyDisplay.BLOCK, TaffyDisplay.FLEX, TaffyDisplay.GRID};

    // === Normal axis: content beyond the padding box ===

    @Test
    @DisplayName("scroll_height_is_the_content_beyond_the_padding_box_on_any_border_edge")
    void scrollHeightIsContentBeyondPaddingBox() {
        for (TaffyDisplay display : CONTAINER_DISPLAYS) {
            for (float[] edges : new float[][]{{BORDER, 0f}, {0f, BORDER}, {BORDER, BORDER}, {0f, 0f}}) {
                float top = edges[0];
                float bottom = edges[1];
                TaffyTree tree = new TaffyTree();
                NodeId content = tree.newLeaf(child(100f, CONTENT));
                NodeId root = tree.newWithChildren(
                    borders(scroller(display, 300f, CONTAINER), top, bottom), content);

                Layout layout = compute(tree, root);
                String message = display + " border " + top + "/" + bottom;

                // Upstream #1007: content 1000 less the padding box (200 - border top - bottom).
                assertEquals(CONTENT - (CONTAINER - top - bottom), layout.scrollHeight(), EPSILON,
                    message + " scrollHeight");
                // The rectangle excludes the container's own border: the child starts at the
                // client-area origin, so the reachable content end is exactly 1000.
                assertOverflow(layout.scrollableOverflow(), 0f, 100f, 0f, CONTENT,
                    false, false, message);
                assertEquals(0f, layout.scrollWidth(), EPSILON, message + " scrollWidth");
            }
        }
    }

    @Test
    @DisplayName("scroll_height_does_not_depend_on_which_edge_the_border_is_on")
    void scrollHeightIsIndependentOfBorderEdge() {
        for (TaffyDisplay display : CONTAINER_DISPLAYS) {
            Layout top = computeScroller(display, borders(scroller(display, 300f, CONTAINER), BORDER, 0f));
            Layout bottom = computeScroller(display, borders(scroller(display, 300f, CONTAINER), 0f, BORDER));

            assertEquals(top.scrollHeight(), bottom.scrollHeight(), EPSILON,
                display + ": a border on either vertical edge must report the same range");
            assertEquals(CONTENT - (CONTAINER - BORDER), top.scrollHeight(), EPSILON,
                display + " scrollHeight");
        }
    }

    @Test
    @DisplayName("scroll_width_is_the_content_beyond_the_padding_box_on_any_border_edge")
    void scrollWidthIsContentBeyondPaddingBox() {
        for (TaffyDisplay display : CONTAINER_DISPLAYS) {
            for (float[] edges : new float[][]{{BORDER, 0f}, {0f, BORDER}, {BORDER, BORDER}, {0f, 0f}}) {
                float left = edges[0];
                float right = edges[1];
                TaffyTree tree = new TaffyTree();
                NodeId content = tree.newLeaf(child(CONTENT, 100f));
                NodeId root = tree.newWithChildren(
                    horizontalBorders(scroller(display, CONTAINER, 300f), left, right), content);

                Layout layout = compute(tree, root);
                String message = display + " border " + left + "/" + right;

                assertEquals(CONTENT - (CONTAINER - left - right), layout.scrollWidth(), EPSILON,
                    message + " scrollWidth");
                assertOverflow(layout.scrollableOverflow(), 0f, CONTENT, 0f, 100f,
                    false, false, message);
            }
        }
    }

    @Test
    @DisplayName("a_scroll_container_includes_its_own_end_padding_in_the_range")
    void scrollContainerIncludesOwnEndPadding() {
        for (TaffyDisplay display : CONTAINER_DISPLAYS) {
            for (float[] edges : new float[][]{{PADDING, 0f}, {0f, PADDING}, {PADDING, PADDING}, {0f, 0f}}) {
                float top = edges[0];
                float bottom = edges[1];
                TaffyTree tree = new TaffyTree();
                NodeId content = tree.newLeaf(child(100f, CONTENT));
                NodeId root = tree.newWithChildren(
                    blockPadding(scroller(display, 300f, CONTAINER), top, bottom), content);

                Layout layout = compute(tree, root);

                // The child is positioned after the start padding; a scroll container also adds
                // its own end padding to the reachable region (#1114/#1372).
                assertEquals((top + CONTENT + bottom) - CONTAINER, layout.scrollHeight(), EPSILON,
                    display + " padding " + top + "/" + bottom);
                assertOverflow(layout.scrollableOverflow(), 0f, 100f, 0f,
                    top + CONTENT + bottom, false, false, display.toString());
            }
        }
    }

    @Test
    @DisplayName("a_non_scroll_container_excludes_its_own_end_padding_from_the_range")
    void nonScrollContainerExcludesOwnEndPadding() {
        for (TaffyDisplay display : CONTAINER_DISPLAYS) {
            for (float[] edges : new float[][]{{PADDING, 0f}, {0f, PADDING}, {PADDING, PADDING}, {0f, 0f}}) {
                float top = edges[0];
                float bottom = edges[1];
                TaffyTree tree = new TaffyTree();
                NodeId content = tree.newLeaf(child(100f, CONTENT));
                NodeId root = tree.newWithChildren(
                    blockPadding(box(display, 300f, CONTAINER), top, bottom), content);

                Layout layout = compute(tree, root);

                // Without scroll clipping the region ends with the content: only the start padding
                // moves the child, the end padding is not reachable (#1117/#1372).
                assertOverflow(layout.scrollableOverflow(), 0f, 100f, 0f, top + CONTENT,
                    false, false, display.toString());
                assertEquals(top + CONTENT - CONTAINER, layout.scrollHeight(), EPSILON,
                    display + " padding " + top + "/" + bottom);
            }
        }
    }

    @Test
    @DisplayName("a_flex_scroll_container_counts_its_own_end_padding_but_not_the_content_size")
    void flexScrollRangeAddsEndPaddingEvenWhenContentSizeFits() {
        // The legacy contentSize records only the child's inline extent (90), which fits in the
        // 100px border box; the reachable region additionally includes the 20px end padding.
        TaffyTree tree = new TaffyTree();
        NodeId content = tree.newLeaf(child(90f, 90f));
        TaffyStyle rootStyle = padding(scroller(TaffyDisplay.FLEX, 100f, 100f), 0f, 20f, 0f, 20f);
        NodeId root = tree.newWithChildren(rootStyle, content);

        Layout layout = compute(tree, root);

        assertEquals(90f, layout.contentSize().width, EPSILON, "legacy contentSize.width");
        assertEquals(90f, layout.contentSize().height, EPSILON, "legacy contentSize.height");
        assertOverflow(layout.scrollableOverflow(), 0f, 110f, 0f, 110f, false, false, "flex");
        assertEquals(10f, layout.scrollWidth(), EPSILON, "scrollWidth");
        assertEquals(10f, layout.scrollHeight(), EPSILON, "scrollHeight");
    }

    @Test
    @DisplayName("a_scroll_container_whose_content_fits_reports_a_zero_range")
    void contentThatFitsReportsZeroRange() {
        for (TaffyDisplay display : CONTAINER_DISPLAYS) {
            TaffyTree tree = new TaffyTree();
            NodeId content = tree.newLeaf(child(40f, 40f));
            TaffyStyle rootStyle = padding(scroller(display, 100f, 100f), 10f, 20f, 5f, 15f);
            NodeId root = tree.newWithChildren(rootStyle, content);

            Layout layout = compute(tree, root);

            // The padded content end (70, 60) stays inside the 100px client area.
            assertEquals(0f, layout.scrollWidth(), EPSILON, display + " scrollWidth");
            assertEquals(0f, layout.scrollHeight(), EPSILON, display + " scrollHeight");
        }
    }

    // === Reversed origins (RTL and bottom-to-top flex axes) ===

    @Test
    @DisplayName("a_rtl_scroll_container_scrolls_towards_the_physical_left_edge")
    void rtlScrollContainerUsesReversedOrigin() {
        for (TaffyDisplay display : CONTAINER_DISPLAYS) {
            TaffyTree tree = new TaffyTree();
            NodeId content = tree.newLeaf(child(150f, 50f));
            TaffyStyle rootStyle = scroller(display, 100f, 100f);
            rootStyle.direction = TaffyDirection.RTL;
            NodeId root = tree.newWithChildren(rootStyle, content);

            Layout layout = compute(tree, root);
            String message = display + " RTL";

            // The 150px child is packed from the right edge, so it spans -50..100 in the client
            // frame and the reachable range is the 50px past the physical left edge.
            assertOverflow(layout.scrollableOverflow(), -50f, 100f, 0f, 50f, true, false, message);
            assertEquals(50f, layout.scrollWidth(), EPSILON, message + " scrollWidth");
            assertEquals(0f, layout.scrollHeight(), EPSILON, message + " scrollHeight");
        }
    }

    @Test
    @DisplayName("a_rtl_non_scroll_container_has_no_reversed_origin_and_no_range")
    void rtlNonScrollContainerHasNoRange() {
        for (TaffyDisplay display : CONTAINER_DISPLAYS) {
            TaffyTree tree = new TaffyTree();
            NodeId content = tree.newLeaf(child(150f, 50f));
            TaffyStyle rootStyle = box(display, 100f, 100f);
            rootStyle.direction = TaffyDirection.RTL;
            NodeId root = tree.newWithChildren(rootStyle, content);

            Layout layout = compute(tree, root);
            String message = display + " RTL visible";

            // All overflow is kept for propagation, but without a scroll container there is no
            // reversed origin, so the normal-axis formula sees no end-side overflow.
            assertOverflow(layout.scrollableOverflow(), -50f, 100f, 0f, 50f, false, false, message);
            assertEquals(0f, layout.scrollWidth(), EPSILON, message + " scrollWidth");
        }
    }

    @Test
    @DisplayName("a_column_reverse_scroll_container_scrolls_towards_the_top_edge")
    void columnReverseUsesReversedYOrigin() {
        TaffyTree tree = new TaffyTree();
        NodeId content = tree.newLeaf(child(50f, 200f));
        TaffyStyle rootStyle = scroller(TaffyDisplay.FLEX, 50f, 100f);
        rootStyle.flexDirection = dev.vfyjxf.taffy.style.FlexDirection.COLUMN_REVERSE;
        NodeId root = tree.newWithChildren(rootStyle, content);

        Layout layout = compute(tree, root);

        // The item is packed from the bottom, spanning -100..0 vertically in the client frame.
        assertOverflow(layout.scrollableOverflow(), 0f, 50f, -100f, 100f, false, true, "flex");
        assertEquals(100f, layout.scrollHeight(), EPSILON, "scrollHeight");
        assertEquals(0f, layout.scrollWidth(), EPSILON, "scrollWidth");
    }

    @Test
    @DisplayName("wrap_reverse_lines_overflow_above_the_scrollport")
    void wrapReverseLinesOverflowAboveTheScrollport() {
        TaffyTree tree = new TaffyTree();
        NodeId first = tree.newLeaf(child(80f, 40f));
        NodeId second = tree.newLeaf(child(80f, 40f));
        TaffyStyle rootStyle = scroller(TaffyDisplay.FLEX, 100f, 50f);
        rootStyle.flexWrap = dev.vfyjxf.taffy.style.FlexWrap.WRAP_REVERSE;
        NodeId root = tree.newWithChildren(rootStyle, first, second);

        Layout layout = compute(tree, root);

        // Lines are packed from the bottom: the second line overflows 30px past the top edge.
        assertOverflow(layout.scrollableOverflow(), 0f, 80f, -30f, 50f, false, true, "flex");
        assertEquals(30f, layout.scrollHeight(), EPSILON, "scrollHeight");
    }

    @Test
    @DisplayName("a_rtl_flex_scroll_range_ignores_the_physical_content_size")
    void rtlFlexScrollRangeUsesReversedRectangle() {
        // The legacy contentSize is a physical extent here (100 = -100 + 200), so the old formula
        // reports zero; the reversed rectangle reports the true 100px reachable range.
        TaffyTree tree = new TaffyTree();
        NodeId content = tree.newLeaf(child(200f, 50f));
        TaffyStyle rootStyle = scroller(TaffyDisplay.FLEX, 100f, 50f);
        rootStyle.direction = TaffyDirection.RTL;
        NodeId root = tree.newWithChildren(rootStyle, content);

        Layout layout = compute(tree, root);

        assertEquals(100f, layout.contentSize().width, EPSILON, "legacy contentSize.width");
        assertEquals(-100f, layout.scrollableOverflow().left(), EPSILON, "rectangle left");
        assertEquals(100f, layout.scrollWidth(), EPSILON, "scrollWidth must not be stale contentSize");
    }

    // === Leaves ===

    private static TaffyStyle leafStyle(float width, float height, TaffyDirection direction,
                                        TaffyPoint<Overflow> overflow) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        style.direction = direction;
        style.overflow = overflow;
        style.padding = new TaffyRect<>(
            LengthPercentage.length(10f),
            LengthPercentage.length(20f),
            LengthPercentage.length(5f),
            LengthPercentage.length(15f)
        );
        return style;
    }

    private static Layout measuredLeaf(TaffyDirection direction, Overflow overflow) {
        TaffyTree tree = new TaffyTree();
        NodeId node = tree.newLeafWithMeasure(
            leafStyle(100f, 100f, direction, new TaffyPoint<>(overflow, overflow)),
            (knownDimensions, availableSpace) -> new FloatSize(300f, 200f));
        return compute(tree, node);
    }

    @Test
    @DisplayName("a_scroll_container_leaf_includes_its_end_padding_in_the_range")
    void scrollContainerLeafIncludesEndPadding() {
        Layout layout = measuredLeaf(TaffyDirection.LTR, Overflow.SCROLL);

        // LTR leaf content: start padding 10 + 300 + end padding 20 = 330 wide; 5 + 200 + 15 tall.
        assertEquals(330f, layout.scrollableOverflow().right(), EPSILON, "right");
        assertEquals(220f, layout.scrollableOverflow().bottom(), EPSILON, "bottom");
        assertEquals(230f, layout.scrollWidth(), EPSILON, "scrollWidth");
        assertEquals(120f, layout.scrollHeight(), EPSILON, "scrollHeight");
    }

    @Test
    @DisplayName("a_non_scroll_leaf_excludes_its_end_padding_from_the_range")
    void nonScrollLeafExcludesEndPadding() {
        Layout layout = measuredLeaf(TaffyDirection.LTR, Overflow.VISIBLE);

        // A non-scroll leaf ends with its content: 10 + 300 and 5 + 200.
        assertOverflow(layout.scrollableOverflow(), 0f, 310f, 0f, 205f, false, false, "leaf");
        assertEquals(210f, layout.scrollWidth(), EPSILON, "scrollWidth");
        assertEquals(105f, layout.scrollHeight(), EPSILON, "scrollHeight");
    }

    @Test
    @DisplayName("a_rtl_scroll_container_leaf_reports_the_reversed_range")
    void rtlScrollContainerLeafReportsReversedRange() {
        Layout layout = measuredLeaf(TaffyDirection.RTL, Overflow.SCROLL);

        // The mirrored rectangle starts at the client area's right edge and ends 230px to its
        // left, so the reachable range is the same as in LTR.
        assertTrue(layout.scrollableOverflow().reversedX(), "reversed origin");
        assertEquals(100f, layout.scrollableOverflow().right(), EPSILON, "right");
        assertEquals(-230f, layout.scrollableOverflow().left(), EPSILON, "left");
        assertEquals(230f, layout.scrollWidth(), EPSILON, "scrollWidth");
        assertEquals(120f, layout.scrollHeight(), EPSILON, "scrollHeight");
    }

    @Test
    @DisplayName("a_rtl_non_scroll_leaf_has_no_reversed_range")
    void rtlNonScrollLeafHasNoReversedRange() {
        Layout layout = measuredLeaf(TaffyDirection.RTL, Overflow.VISIBLE);

        // All overflow is kept, but without a scroll container the origin stays normal, so the
        // physical right edge is the only thing the normal-axis formula sees.
        assertOverflow(layout.scrollableOverflow(), -220f, 100f, 0f, 205f, false, false, "leaf");
        assertEquals(0f, layout.scrollWidth(), EPSILON, "scrollWidth");
        assertEquals(105f, layout.scrollHeight(), EPSILON, "scrollHeight");
    }

    // === 1.2 compatibility path ===

    @Test
    @DisplayName("hand_built_layouts_keep_the_legacy_content_size_formula")
    void handBuiltLayoutsKeepLegacyFormula() {
        FloatPoint location = FloatPoint.zero();
        FloatSize size = new FloatSize(100f, 100f);
        FloatRect zero = FloatRect.zero();

        // The 1.1.x constructor supplies ScrollableOverflow.ZERO: no rectangle data, so the legacy
        // contentSize formula remains in charge.
        Layout legacy = new Layout(
            0, location, size.copy(), new FloatSize(200f, 50f), FloatSize.zero(),
            zero.copy(), zero.copy(), zero.copy());
        assertEquals(ScrollableOverflow.ZERO, legacy.scrollableOverflow(), "compat placeholder");
        assertEquals(100f, legacy.scrollWidth(), EPSILON, "legacy scrollWidth");
        assertEquals(0f, legacy.scrollHeight(), EPSILON, "legacy scrollHeight");

        // An explicit rectangle wins even when the legacy component disagrees.
        Layout explicit = new Layout(
            0, location, size.copy(), new FloatSize(999f, 999f),
            new ScrollableOverflow(new FloatRect(0f, 150f, 0f, 0f), false, false),
            FloatSize.zero(), zero.copy(), zero.copy(), zero.copy());
        assertEquals(50f, explicit.scrollWidth(), EPSILON, "explicit scrollWidth");
        assertEquals(0f, explicit.scrollHeight(), EPSILON, "explicit scrollHeight");

        // The reversed origin reads the left edge and ignores the border/border-box terms.
        Layout reversed = new Layout(
            0, location, size.copy(), FloatSize.zero(),
            new ScrollableOverflow(new FloatRect(-70f, 100f, -40f, 100f), true, true),
            FloatSize.zero(), new FloatRect(20f, 20f, 20f, 20f), zero.copy(), zero.copy());
        assertEquals(70f, reversed.scrollWidth(), EPSILON, "reversed scrollWidth");
        assertEquals(40f, reversed.scrollHeight(), EPSILON, "reversed scrollHeight");

        // A scrollbar gutter is clipped to the node's own size before it is added (upstream #1372).
        Layout gutter = new Layout(
            0, location, size.copy(), FloatSize.zero(),
            new ScrollableOverflow(new FloatRect(0f, 150f, 0f, 120f), false, false),
            new FloatSize(10f, 200f), zero.copy(), zero.copy(), zero.copy());
        assertEquals(60f, gutter.scrollWidth(), EPSILON, "gutter scrollWidth");
        assertEquals(120f, gutter.scrollHeight(), EPSILON, "scrollbar gutter is clipped to the node size");
    }
}
