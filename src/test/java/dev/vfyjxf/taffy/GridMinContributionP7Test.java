package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.GridPlacement;
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
 * P7 grid batch: upstream-equivalent fixtures for
 * {@code alignment-changes.md} GR-02 (minimum contribution) and GR-20 (implicit tracks in the
 * stretch phase).
 *
 * <ul>
 *   <li><b>GR-20</b>: the 11.8 "stretch auto tracks" step and the maximise step must classify an
 *       implicit track by its {@code grid-auto-columns}/{@code grid-auto-rows} sizing function.
 *       Only tracks whose <em>max</em> track sizing function is {@code auto} stretch; an implicit
 *       track given a fixed size by {@code grid-auto-*} must keep that size.</li>
 *   <li><b>GR-02</b> ({@code 1a2d05ab} #1022, {@code fca50a0c} #1033, {@code a25e1f76} #1228,
 *       {@code 654d6d7e} #1278, {@code 9fb6fc28} #1282, {@code 0daade2d} #1351,
 *       {@code e90751fd} #1362): a minimum contribution is the preferred size clamped by the
 *       item's min/max size when the preferred size is definite; otherwise the specified minimum
 *       size (including a cyclic percentage resolved against zero) or, for a scroll container,
 *       zero; the automatic content-based minimum only applies when the item spans an auto-min
 *       track and is not a multi-span item crossing a flexible track, is capped by the spanned
 *       fixed max track sizing functions, and is never smaller than the item's padding + border.</li>
 * </ul>
 *
 * <p>Fixture values follow the upstream generated XML expectations (Chrome layout oracle), ported
 * to the Java test style used by the other P7 batches.</p>
 */
class GridMinContributionP7Test {

    private static final float EPSILON = 0.01f;

    // ------------------------------------------------------------------
    // GR-20: implicit tracks are stretched only when their max sizing function is auto
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("GR-20: implicit tracks use their grid-auto-* sizing function when stretching")
    class ImplicitTrackStretch {

        @Test
        @DisplayName("a fixed implicit column is not stretched")
        void fixedImplicitColumnIsNotStretched() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(50f));
            grid.gridTemplateColumns = List.of(TrackSizingFunction.auto());
            grid.gridAutoColumns = List.of(TrackSizingFunction.fixed(50f));
            grid.gridTemplateRows = List.of(TrackSizingFunction.fixed(50f));
            NodeId explicit = tree.newLeaf(sized(10f, 10f));
            // Placed on grid column line 2, i.e. the single implicit column.
            NodeId implicit = tree.newLeaf(placed(new TaffyStyle(), 2, 1, 1, 1));
            NodeId root = tree.newWithChildren(grid, explicit, implicit);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 200f, 50f, "container");
            // The explicit auto column absorbs all of the free space (200 - 50 = 150); the implicit
            // fixed column keeps its 50px grid-auto-columns size.
            assertLocation(tree.getLayout(explicit), 0f, 0f, "explicit item");
            assertLocation(tree.getLayout(implicit), 150f, 0f, "implicit item");
            assertSize(tree.getLayout(implicit), 50f, 50f, "implicit item");
        }

        @Test
        @DisplayName("two implicit fixed columns keep their grid-auto-columns sizes")
        void fixedImplicitColumnsKeepTheirSizes() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(40f));
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fixed(100f));
            grid.gridAutoColumns = List.of(TrackSizingFunction.fixed(60f));
            grid.gridTemplateRows = List.of(TrackSizingFunction.fixed(40f));
            NodeId first = tree.newLeaf(placed(new TaffyStyle(), 1, 1, 1, 1));
            NodeId second = tree.newLeaf(placed(new TaffyStyle(), 2, 1, 1, 1));
            NodeId third = tree.newLeaf(placed(new TaffyStyle(), 3, 1, 1, 1));
            NodeId root = tree.newWithChildren(grid, first, second, third);

            tree.computeLayout(root, TaffySize.maxContent());

            assertLocation(tree.getLayout(second), 100f, 0f, "first implicit column");
            assertLocation(tree.getLayout(third), 160f, 0f, "second implicit column");
            assertSize(tree.getLayout(second), 60f, 40f, "first implicit column item");
            assertSize(tree.getLayout(third), 60f, 40f, "second implicit column item");
        }

        @Test
        @DisplayName("an auto implicit column still stretches")
        void autoImplicitColumnStillStretches() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(50f));
            grid.gridTemplateColumns = List.of(TrackSizingFunction.auto());
            grid.gridAutoColumns = List.of(TrackSizingFunction.auto());
            grid.gridTemplateRows = List.of(TrackSizingFunction.fixed(50f));
            NodeId first = tree.newLeaf(sized(10f, 10f));
            NodeId second = tree.newLeaf(placed(sized(10f, 10f), 2, 1, 1, 1));
            NodeId root = tree.newWithChildren(grid, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            // Both tracks are auto with a 10px minimum contribution: the 180px of free space is
            // split evenly, so each column becomes 100px wide.
            assertLocation(tree.getLayout(second), 100f, 0f, "implicit item");
        }

        @Test
        @DisplayName("a fixed implicit row is not stretched")
        void fixedImplicitRowIsNotStretched() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(200f));
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fixed(100f));
            grid.gridTemplateRows = List.of(TrackSizingFunction.auto());
            grid.gridAutoRows = List.of(TrackSizingFunction.fixed(50f));
            NodeId first = tree.newLeaf(sized(10f, 10f));
            NodeId second = tree.newLeaf(placed(new TaffyStyle(), 1, 1, 2, 1));
            NodeId root = tree.newWithChildren(grid, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            assertLocation(tree.getLayout(second), 0f, 150f, "implicit row item");
            assertSize(tree.getLayout(second), 100f, 50f, "implicit row item");
        }
    }

    // ------------------------------------------------------------------
    // Helpers
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
}
