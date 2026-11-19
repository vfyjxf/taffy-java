package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.Overflow;
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
    // GR-12: cyclic percentage minima, indefinite percentage min tracks, fr under
    // a block-axis min-content constraint
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("GR-12: percentage minima and block-axis min-content fr expansion")
    class PercentageMinimaAndFr {

        @Test
        @DisplayName("grid_minimum_contribution_cyclic_percent_min_size")
        void cyclicPercentMinSize() {
            assertCyclicPercentMinimum(null);
        }

        @Test
        @DisplayName("grid_minimum_contribution_cyclic_percent_min_and_preferred_size")
        void cyclicPercentMinAndPreferredSize() {
            assertCyclicPercentMinimum(new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.percent(1f)));
        }

        /**
         * The item's cyclic {@code min-width/min-height: 100%} resolve against zero for its minimum
         * contribution (upstream #1257), so the auto tracks keep their 0 minimum and the content
         * (a 100x100 grid item) overflows the 50x50 container instead of stretching it.
         */
        private void assertCyclicPercentMinimum(TaffySize<TaffyDimension> preferredSize) {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.size = new TaffySize<>(TaffyDimension.length(50f), TaffyDimension.length(50f));
            grid.gridTemplateColumns = List.of(TrackSizingFunction.auto());
            grid.gridTemplateRows = List.of(TrackSizingFunction.auto());

            TaffyStyle itemStyle = grid();
            if (preferredSize != null) itemStyle.size = preferredSize;
            itemStyle.minSize = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.percent(1f));
            NodeId inner = tree.newLeaf(sized(100f, 100f));
            NodeId item = tree.newWithChildren(itemStyle, inner);
            NodeId root = tree.newWithChildren(grid, item);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 50f, 50f, "container");
            assertSize(tree.getLayout(item), 50f, 50f, "item");
            assertSize(tree.getLayout(inner), 100f, 100f, "inner");
            assertLocation(tree.getLayout(inner), 0f, 0f, "inner");
        }

        @Test
        @DisplayName("grid_percent_min_track_indefinite_fixed_max_column")
        void percentMinTrackIndefiniteFixedMaxColumn() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.gridTemplateColumns = List.of(
                TrackSizingFunction.minmax(TrackSizingFunction.percent(0.2f), TrackSizingFunction.fixed(20f)));
            NodeId item = tree.newLeafWithMeasure(new TaffyStyle(), ahemTextMeasure("HHH"));
            NodeId root = tree.newWithChildren(grid, item);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 20f, 10f, "container");
            assertSize(tree.getLayout(item), 20f, 10f, "item");
        }

        @Test
        @DisplayName("grid_percent_min_track_indefinite_fixed_max_row")
        void percentMinTrackIndefiniteFixedMaxRow() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fixed(20f));
            grid.gridTemplateRows = List.of(
                TrackSizingFunction.minmax(TrackSizingFunction.percent(1f), TrackSizingFunction.fixed(15f)));
            NodeId item = tree.newLeafWithMeasure(new TaffyStyle(), ahemTextMeasure("HH\u200BHH"));
            NodeId root = tree.newWithChildren(grid, item);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 20f, 15f, "container");
            assertSize(tree.getLayout(item), 20f, 15f, "item");
        }

        @Test
        @DisplayName("indefinite percentage min track behaves as auto for an explicit min size (column)")
        void percentMinTrackExplicitMinColumn() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.gridTemplateColumns = List.of(
                TrackSizingFunction.minmax(TrackSizingFunction.percent(0.2f), TrackSizingFunction.fixed(20f)));
            TaffyStyle itemStyle = new TaffyStyle();
            itemStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(10f));
            itemStyle.minSize = new TaffySize<>(TaffyDimension.length(30f), TaffyDimension.AUTO);
            NodeId item = tree.newLeaf(itemStyle);
            NodeId root = tree.newWithChildren(grid, item);

            tree.computeLayout(root, TaffySize.maxContent());

            // The percentage min track behaves as auto, so the explicit 30px min-width raises the
            // track above the fixed 20px max (the automatic content-based minimum would be clamped).
            assertSize(tree.getLayout(root), 30f, 10f, "container");
            assertSize(tree.getLayout(item), 30f, 10f, "item");
        }

        @Test
        @DisplayName("indefinite percentage min track behaves as auto for an explicit min size (row)")
        void percentMinTrackExplicitMinRow() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fixed(20f));
            grid.gridTemplateRows = List.of(
                TrackSizingFunction.minmax(TrackSizingFunction.percent(1f), TrackSizingFunction.fixed(15f)));
            TaffyStyle itemStyle = new TaffyStyle();
            itemStyle.size = new TaffySize<>(TaffyDimension.length(20f), TaffyDimension.AUTO);
            itemStyle.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(30f));
            NodeId item = tree.newLeaf(itemStyle);
            NodeId root = tree.newWithChildren(grid, item);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 20f, 30f, "container");
            assertSize(tree.getLayout(item), 20f, 30f, "item");
        }

        @Test
        @DisplayName("grid_fr_rows_nested_min_content_block_size")
        void frRowsNestedMinContentBlockSize() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = grid();
            rootStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(200f));
            NodeId innerGrid = frRowsGridWithHiddenOverflow(tree);
            NodeId root = tree.newWithChildren(rootStyle, innerGrid);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 100f, 200f, "container");
            // The nested grid's min-content block size is its max-content block size (upstream #1284),
            // so the auto row and the nested grid grow to 50 + 400 despite the 200px parent.
            assertSize(tree.getLayout(innerGrid), 100f, 450f, "nested grid");
            NodeId second = tree.getChildren(innerGrid).get(1);
            assertLocation(tree.getLayout(second), 0f, 50f, "second item");
            assertSize(tree.getLayout(second), 100f, 400f, "second item");
        }

        @Test
        @DisplayName("grid_fr_rows_flex_item_min_content_block_size")
        void frRowsFlexItemMinContentBlockSize() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle flexStyle = new TaffyStyle();
            flexStyle.display = TaffyDisplay.FLEX;
            flexStyle.flexDirection = FlexDirection.COLUMN;
            flexStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(200f));
            NodeId gridNode = frRowsGridWithHiddenOverflow(tree);
            NodeId root = tree.newWithChildren(flexStyle, gridNode);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 100f, 200f, "flex container");
            assertSize(tree.getLayout(gridNode), 100f, 450f, "grid");
            NodeId second = tree.getChildren(gridNode).get(1);
            assertLocation(tree.getLayout(second), 0f, 50f, "second item");
            assertSize(tree.getLayout(second), 100f, 400f, "second item");
        }

        @Test
        @DisplayName("fr rows expand under a block-axis min-content constraint")
        void frRowsUnderMinContentBlockConstraint() {
            TaffyTree tree = new TaffyTree();
            NodeId gridNode = frRowsGridWithHiddenOverflow(tree);

            tree.computeLayout(gridNode, new TaffySize<>(AvailableSpace.definite(100f), AvailableSpace.minContent()));

            // A min-content constraint in the block axis behaves as max-content for flexible-track
            // expansion (upstream #1284), so the 1fr row takes the 400px content height.
            assertEquals(450f, tree.getLayout(gridNode).size().height, EPSILON, "grid height");
            NodeId second = tree.getChildren(gridNode).get(1);
            assertLocation(tree.getLayout(second), 0f, 50f, "second item");
            assertSize(tree.getLayout(second), 0f, 400f, "second item");
        }

        /**
         * The upstream #1284 fixture: a grid with {@code grid-template-rows: 50px 1fr} whose second
         * item is a hidden-overflow box containing a 400px-tall child.
         */
        private NodeId frRowsGridWithHiddenOverflow(TaffyTree tree) {
            TaffyStyle gridStyle = grid();
            gridStyle.gridTemplateRows = List.of(TrackSizingFunction.fixed(50f), TrackSizingFunction.fr(1f));
            NodeId empty = tree.newLeaf(new TaffyStyle());
            TaffyStyle scrollerStyle = new TaffyStyle();
            scrollerStyle.overflow = new TaffyPoint<>(Overflow.HIDDEN, Overflow.HIDDEN);
            NodeId tall = tree.newLeaf(sized(0f, 400f));
            NodeId scroller = tree.newWithChildren(scrollerStyle, tall);
            return tree.newWithChildren(gridStyle, empty, scroller);
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
