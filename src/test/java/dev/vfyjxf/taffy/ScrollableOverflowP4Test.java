package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.tree.Layout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P4 scrollable-overflow tests for the core/leaf half of the package (SO-01/SO-02/SO-03/SO-05).
 *
 * <p>SO-03: {@code scrollWidth()}/{@code scrollHeight()} report the same maximum scroll offset no
 * matter which edge a border sits on, because the reachable range is the content extent less the
 * padding box (upstream #1007). The legacy {@code contentSize} component is still the input, so
 * these cases construct a {@link Layout} directly and freeze both formulas' outcome.</p>
 */
public class ScrollableOverflowP4Test {

    private static final float EPSILON = 0.01f;

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
}
