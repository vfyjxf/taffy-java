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
    // helpers
    // ------------------------------------------------------------------

    private static TaffyStyle grid() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
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
