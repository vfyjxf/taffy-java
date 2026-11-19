package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P7 grid track-sizing batch 2: upstream-equivalent fixtures for
 * {@code alignment-changes.md} GR-06 / GR-11 / GR-12.
 *
 * <ul>
 *   <li><b>GR-06</b> ({@code b353bc07} #960, {@code 2454be27} #1271): during intrinsic sizing an
 *       item's percentage size/min/max/padding/border/margin resolve against its <em>grid area</em>,
 *       not the grid container; the containing-block size is passed separately from the grid area
 *       rectangle.</li>
 *   <li><b>GR-11</b> ({@code d145df5e} #1243, {@code 66b33640} #1244): row sizing is re-run
 *       independently of column sizing, and percentage tracks are re-run whenever the container's
 *       own size (not the available space) is indefinite.</li>
 *   <li><b>GR-12</b> ({@code 4142c9d8} #1257, {@code 8409f3f8} #1261, {@code e2fbe564} #1284):
 *       cyclic percentage minimum contributions resolve against zero, an indefinite percentage
 *       min track behaves as auto, and flexible tracks expand under a block-axis min-content
 *       constraint.</li>
 * </ul>
 *
 * <p>Fixture numbers are taken from the upstream generated XML expectations (Chrome layout
 * oracle). Text content is emulated with the generated-test Ahem measure function (10px per
 * character, zero-width spaces mark wrapping opportunities).</p>
 */
class GridSizing2P7Test {

    private static final float EPSILON = 0.01f;

    // ------------------------------------------------------------------
    // GR-06: item percentages resolve against the grid area
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("GR-06: intrinsic item percentages resolve against the grid area")
    class PercentAgainstGridArea {

        @Test
        @DisplayName("grid_percent_items_100_percent_2_col_auto")
        void percentWidthTwoAutoColumns() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
            grid.gridTemplateColumns = List.of(TrackSizingFunction.auto(), TrackSizingFunction.auto());
            grid.gridTemplateRows = List.of(TrackSizingFunction.fixed(50f), TrackSizingFunction.fixed(50f));
            NodeId first = tree.newLeaf(percentSized(100f, 50f));
            NodeId second = tree.newLeaf(placed(percentSized(100f, 50f), 2, 1, 2, 1));
            NodeId root = tree.newWithChildren(grid, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 200f, 100f, "container");
            assertSize(tree.getLayout(first), 100f, 50f, "first item");
            assertLocation(tree.getLayout(first), 0f, 0f, "first item");
            assertSize(tree.getLayout(second), 100f, 50f, "second item");
            assertLocation(tree.getLayout(second), 100f, 50f, "second item");
        }

        @Test
        @DisplayName("grid_percent_items_100_percent_2_col_fr")
        void percentWidthTwoFlexColumns() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fr(1f), TrackSizingFunction.fr(1f));
            grid.gridTemplateRows = List.of(TrackSizingFunction.fixed(50f), TrackSizingFunction.fixed(50f));
            NodeId first = tree.newLeaf(percentSized(100f, 50f));
            NodeId second = tree.newLeaf(placed(percentSized(100f, 50f), 2, 1, 2, 1));
            NodeId root = tree.newWithChildren(grid, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 200f, 100f, "container");
            assertSize(tree.getLayout(first), 100f, 50f, "first item");
            assertLocation(tree.getLayout(first), 0f, 0f, "first item");
            assertSize(tree.getLayout(second), 100f, 50f, "second item");
            assertLocation(tree.getLayout(second), 100f, 50f, "second item");
        }

        @Test
        @DisplayName("percentage height resolves against the grid area height")
        void percentHeightAgainstRowArea() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(200f));
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fixed(100f));
            grid.gridTemplateRows = List.of(TrackSizingFunction.fixed(80f), TrackSizingFunction.fixed(120f));
            TaffyStyle itemStyle = new TaffyStyle();
            itemStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.percent(1f));
            NodeId item = tree.newLeaf(itemStyle);
            NodeId root = tree.newWithChildren(grid, item);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 100f, 200f, "container");
            // The grid area of the first row is 80px tall; 100% of the row is 80px, not 200px.
            assertSize(tree.getLayout(item), 100f, 80f, "item");
        }

        @Test
        @DisplayName("percentage padding, border and margin resolve against the grid area")
        void percentPaddingBorderMarginAgainstColumnArea() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fixed(80f), TrackSizingFunction.fixed(120f));
            grid.gridTemplateRows = List.of(TrackSizingFunction.fixed(50f));
            TaffyStyle itemStyle = new TaffyStyle();
            itemStyle.size = new TaffySize<>(TaffyDimension.length(10f), TaffyDimension.length(10f));
            itemStyle.margin = TaffyRect.all(LengthPercentageAuto.percent(0.1f));
            itemStyle.padding = TaffyRect.all(LengthPercentage.percent(0.1f));
            itemStyle.border = TaffyRect.all(LengthPercentage.percent(0.1f));
            itemStyle.justifySelf = AlignItems.START;
            itemStyle.alignSelf = AlignItems.START;
            NodeId item = tree.newLeaf(itemStyle);
            NodeId root = tree.newWithChildren(grid, item);

            tree.computeLayout(root, TaffySize.maxContent());

            // The item's grid area is the 80px first column: 10% is 8px per side. The 10px preferred
            // border-box size is floored at padding + border (32px).
            assertSize(tree.getLayout(item), 32f, 32f, "item");
            assertLocation(tree.getLayout(item), 8f, 8f, "item");
            assertRect(tree.getLayout(item).padding(), 8f, 8f, 8f, 8f, "item padding");
        }
    }

    // ------------------------------------------------------------------
    // GR-11: percentage track re-run uses the container size
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("GR-11: percentage tracks re-run when the container size is indefinite")
    class PercentageRerun {

        @Test
        @DisplayName("fit_content_percentage_indefinite_container_max_content_available_space")
        void fitContentPercentColumnMaxContentAvailableSpace() {
            assertFitContentPercentColumn(TaffySize.maxContent());
        }

        @Test
        @DisplayName("fit_content_percentage_indefinite_container_definite_available_space")
        void fitContentPercentColumnDefiniteAvailableSpace() {
            assertFitContentPercentColumn(new TaffySize<>(AvailableSpace.definite(200f), AvailableSpace.maxContent()));
        }

        /**
         * A shrink-to-fit grid with a {@code fit-content(50%)} column. The first pass treats the
         * percentage as unresolvable and sizes the column to the text's max-content width (60px);
         * the container becomes 60px wide and the re-run resolves the fit-content limit against that
         * content box (30px), which clamps the item. The container keeps its first-pass width.
         */
        private void assertFitContentPercentColumn(TaffySize<AvailableSpace> availableSpace) {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fitContent(LengthPercentage.percent(0.5f)));
            grid.gridTemplateRows = List.of(TrackSizingFunction.fixed(40f));
            NodeId item = tree.newLeafWithMeasure(new TaffyStyle(),
                ahemTextMeasure("HH\u200BHH\u200BHH"));
            NodeId root = tree.newWithChildren(grid, item);

            tree.computeLayout(root, availableSpace);

            assertEquals(60f, tree.getLayout(root).size().width, EPSILON, "container width");
            assertEquals(30f, tree.getLayout(item).size().width, EPSILON, "item width");
        }

        @Test
        @DisplayName("percentage_rows_indefinite_container_max_content_available_space")
        void percentRowsShrinkwrapMaxContentAvailableSpace() {
            assertPercentRowsShrinkwrap(TaffySize.maxContent());
        }

        @Test
        @DisplayName("percentage_rows_indefinite_container_definite_available_space")
        void percentRowsShrinkwrapDefiniteAvailableSpace() {
            assertPercentRowsShrinkwrap(new TaffySize<>(AvailableSpace.definite(200f), AvailableSpace.definite(500f)));
        }

        /**
         * Mirrors WPT {@code grid-percent-rows-filled-shrinkwrap-001}: an auto-height grid with
         * rows {@code auto 20% auto}. The percentage row is treated as auto in the first pass, the
         * content box becomes 100px tall, and the row re-run resolves the percentage against it
         * (20px) while the auto rows absorb the remaining space (40px each).
         */
        private void assertPercentRowsShrinkwrap(TaffySize<AvailableSpace> availableSpace) {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.gridTemplateColumns = List.of(TrackSizingFunction.auto());
            grid.gridTemplateRows = List.of(TrackSizingFunction.auto(),
                TrackSizingFunction.percent(0.2f), TrackSizingFunction.auto());
            NodeId first = tree.newLeaf(sized(20f, 20f));
            NodeId second = tree.newLeaf(sized(20f, 60f));
            NodeId third = tree.newLeaf(sized(20f, 20f));
            NodeId root = tree.newWithChildren(grid, first, second, third);

            tree.computeLayout(root, availableSpace);

            assertEquals(100f, tree.getLayout(root).size().height, EPSILON, "container height");
            assertEquals(0f, tree.getLayout(first).location().y, EPSILON, "first item y");
            assertEquals(40f, tree.getLayout(second).location().y, EPSILON, "second item y");
            assertEquals(60f, tree.getLayout(third).location().y, EPSILON, "third item y");
        }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static TaffyStyle grid() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        return style;
    }

    private static TaffyStyle sized(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle percentSized(float widthPercent, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.percent(widthPercent / 100f), TaffyDimension.length(height));
        return style;
    }

    /** Sets an explicit grid position (1-based start line plus span) on an item style. */
    private static TaffyStyle placed(TaffyStyle style, int columnStart, int columnSpan, int rowStart, int rowSpan) {
        style.gridColumn = new TaffyLine<>(GridPlacement.line(columnStart),
            columnSpan == 1 ? GridPlacement.auto() : GridPlacement.span(columnSpan));
        style.gridRow = new TaffyLine<>(GridPlacement.line(rowStart),
            rowSpan == 1 ? GridPlacement.auto() : GridPlacement.span(rowSpan));
        return style;
    }

    /**
     * Ahem-style text measure used by the generated grid tests: 10x10 per character, zero-width
     * spaces are wrapping opportunities (min-content = longest segment, max-content = one line).
     * Horizontal writing mode only (the GR-11 fixtures do not need the vertical variant).
     */
    private static dev.vfyjxf.taffy.util.MeasureFunc ahemTextMeasure(String text) {
        final String trimmed = text == null ? "" : text.trim();
        return (knownDimensions, availableSpace) -> {
            if (!Float.isNaN(knownDimensions.width) && !Float.isNaN(knownDimensions.height)) {
                return new dev.vfyjxf.taffy.geometry.FloatSize(knownDimensions.width, knownDimensions.height);
            }
            final char ZWS = '\u200B';
            final float GLYPH_WIDTH = 10.0f;
            final float GLYPH_HEIGHT = 10.0f;

            String[] parts = trimmed.isEmpty() ? new String[0] : trimmed.split(String.valueOf(ZWS), -1);
            if (parts.length == 0) {
                return new dev.vfyjxf.taffy.geometry.FloatSize(0f, 0f);
            }

            int minLineLength = 0;
            int maxLineLength = 0;
            for (String part : parts) {
                minLineLength = Math.max(minLineLength, part.length());
                maxLineLength += part.length();
            }

            float inlineSize;
            if (!Float.isNaN(knownDimensions.width)) {
                inlineSize = knownDimensions.width;
            } else if (availableSpace.width.isMinContent()) {
                inlineSize = minLineLength * GLYPH_WIDTH;
            } else if (availableSpace.width.isMaxContent()) {
                inlineSize = maxLineLength * GLYPH_WIDTH;
            } else if (availableSpace.width.isDefinite()) {
                inlineSize = Math.min(availableSpace.width.getValue(), maxLineLength * GLYPH_WIDTH);
            } else {
                inlineSize = maxLineLength * GLYPH_WIDTH;
            }
            inlineSize = Math.max(inlineSize, minLineLength * GLYPH_WIDTH);

            float blockSize;
            if (!Float.isNaN(knownDimensions.height)) {
                blockSize = knownDimensions.height;
            } else {
                int inlineLineLength = (int) Math.floor(inlineSize / GLYPH_WIDTH);
                int lineCount = 1;
                int currentLineLength = 0;
                for (String part : parts) {
                    int len = part.length();
                    if (currentLineLength + len > inlineLineLength) {
                        if (currentLineLength > 0) lineCount += 1;
                        currentLineLength = len;
                    } else {
                        currentLineLength += len;
                    }
                }
                blockSize = lineCount * GLYPH_HEIGHT;
            }

            return new dev.vfyjxf.taffy.geometry.FloatSize(
                !Float.isNaN(knownDimensions.width) ? knownDimensions.width : inlineSize,
                !Float.isNaN(knownDimensions.height) ? knownDimensions.height : blockSize);
        };
    }

    private static void assertSize(Layout layout, float width, float height, String message) {
        assertEquals(width, layout.size().width, EPSILON, message + " width");
        assertEquals(height, layout.size().height, EPSILON, message + " height");
    }

    private static void assertLocation(Layout layout, float x, float y, String message) {
        assertEquals(x, layout.location().x, EPSILON, message + " x");
        assertEquals(y, layout.location().y, EPSILON, message + " y");
    }

    private static void assertRect(dev.vfyjxf.taffy.geometry.FloatRect rect,
                                   float left, float right, float top, float bottom, String message) {
        assertEquals(left, rect.left, EPSILON, message + " left");
        assertEquals(right, rect.right, EPSILON, message + " right");
        assertEquals(top, rect.top, EPSILON, message + " top");
        assertEquals(bottom, rect.bottom, EPSILON, message + " bottom");
    }
}
