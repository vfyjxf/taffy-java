package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.Overflow;
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
    // GR-02: minimum contribution completeness
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("GR-02: minimum contribution follows the CSS Grid 6.6 rules")
    class MinimumContribution {

        @Test
        @DisplayName("grid_min_contribution_size_clamped_by_max_size_columns")
        void preferredWidthClampedByMaxWidth() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(100f));
            grid.justifyContent = AlignContent.START;
            grid.gridTemplateColumns = List.of(TrackSizingFunction.auto());
            TaffyStyle clamped = new TaffyStyle();
            clamped.size = new TaffySize<>(TaffyDimension.length(25f), TaffyDimension.AUTO);
            clamped.maxSize = new TaffySize<>(TaffyDimension.length(10f), TaffyDimension.AUTO);
            NodeId first = tree.newLeaf(clamped);
            NodeId second = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            // 25px preferred clamped by the 10px max-width: the auto column (and both items) are 10px.
            assertSize(tree.getLayout(root), 100f, 100f, "container");
            assertSize(tree.getLayout(first), 10f, 50f, "clamped item");
            assertSize(tree.getLayout(second), 10f, 50f, "stretched item");
            assertLocation(tree.getLayout(second), 0f, 50f, "stretched item");
        }

        @Test
        @DisplayName("grid_min_contribution_size_clamped_by_min_size_columns")
        void preferredWidthClampedByMinWidth() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(100f));
            grid.gridTemplateColumns = List.of(TrackSizingFunction.minmax(
                TrackSizingFunction.auto(), TrackSizingFunction.fixed(0f)));
            TaffyStyle clamped = new TaffyStyle();
            clamped.size = new TaffySize<>(TaffyDimension.length(25f), TaffyDimension.AUTO);
            clamped.minSize = new TaffySize<>(TaffyDimension.length(30f), TaffyDimension.AUTO);
            NodeId first = tree.newLeaf(clamped);
            NodeId second = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 100f, 100f, "container");
            assertSize(tree.getLayout(first), 30f, 50f, "clamped item");
            assertSize(tree.getLayout(second), 30f, 50f, "stretched item");
            assertLocation(tree.getLayout(second), 0f, 50f, "stretched item");
        }

        @Test
        @DisplayName("grid_min_contribution_size_clamped_by_max_size_rows")
        void preferredHeightClampedByMaxHeight() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.AUTO);
            grid.gridTemplateRows = List.of(TrackSizingFunction.auto());
            TaffyStyle clamped = new TaffyStyle();
            clamped.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(25f));
            clamped.maxSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(10f));
            NodeId item = tree.newLeaf(clamped);
            NodeId root = tree.newWithChildren(grid, item);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 100f, 10f, "container");
            assertSize(tree.getLayout(item), 100f, 10f, "clamped item");
        }

        @Test
        @DisplayName("multi-span item uses its specified minimum size")
        void multiSpanSpecifiedMinimumSize() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(20f));
            grid.justifyContent = AlignContent.START;
            grid.gridTemplateColumns = List.of(TrackSizingFunction.auto(), TrackSizingFunction.auto());
            grid.gridTemplateRows = List.of(TrackSizingFunction.fixed(10f), TrackSizingFunction.fixed(10f));
            TaffyStyle spanning = new TaffyStyle();
            spanning.size = new TaffySize<>(TaffyDimension.length(25f), TaffyDimension.length(10f));
            spanning.minSize = new TaffySize<>(TaffyDimension.length(30f), TaffyDimension.AUTO);
            NodeId item = tree.newLeaf(placed(spanning, 1, 2, 1, 1));
            NodeId firstColumn = tree.newLeaf(placed(new TaffyStyle(), 1, 1, 2, 1));
            NodeId secondColumn = tree.newLeaf(placed(new TaffyStyle(), 2, 1, 2, 1));
            NodeId root = tree.newWithChildren(grid, item, firstColumn, secondColumn);

            tree.computeLayout(root, TaffySize.maxContent());

            // The 30px specified minimum (larger than the 25px preferred size) is distributed over
            // the two spanned auto columns, 15px each.
            assertSize(tree.getLayout(item), 30f, 10f, "spanning item");
            assertSize(tree.getLayout(firstColumn), 15f, 10f, "first column item");
            assertSize(tree.getLayout(secondColumn), 15f, 10f, "second column item");
            assertLocation(tree.getLayout(secondColumn), 15f, 10f, "second column item");
        }

        @Test
        @DisplayName("automatic minimum is capped by a spanned fixed max track, floored at padding + border")
        void automaticMinimumClampedBySpannedFixedTrack() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(30f));
            grid.justifyContent = AlignContent.START;
            grid.gridTemplateColumns = List.of(TrackSizingFunction.minmax(
                TrackSizingFunction.auto(), TrackSizingFunction.fixed(5f)));
            grid.gridTemplateRows = List.of(TrackSizingFunction.auto());
            TaffyStyle itemStyle = new TaffyStyle();
            itemStyle.padding = TaffyRect.all(LengthPercentage.length(10f));
            NodeId item = tree.newLeafWithMeasure(itemStyle,
                (knownDimensions, availableSpace) -> new FloatSize(100f, 10f));
            NodeId root = tree.newWithChildren(grid, item);

            tree.computeLayout(root, TaffySize.maxContent());

            // min-content contribution is 120px (100px content + 20px padding), but the spanned
            // fixed max track limit (5px) clamps it, with padding + border (20px) as the floor.
            assertSize(tree.getLayout(item), 20f, 30f, "clamped item");
        }

        @Test
        @DisplayName("scroll containers have a zero automatic minimum but keep their padding floor")
        void scrollContainerAutomaticMinimumIsZero() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(20f));
            grid.justifyContent = AlignContent.START;
            grid.gridTemplateColumns = List.of(TrackSizingFunction.minmax(
                TrackSizingFunction.auto(), TrackSizingFunction.fixed(10f)));
            grid.gridTemplateRows = List.of(TrackSizingFunction.fixed(20f));
            TaffyStyle itemStyle = new TaffyStyle();
            itemStyle.overflow = new TaffyPoint<>(Overflow.HIDDEN, Overflow.HIDDEN);
            NodeId item = tree.newLeafWithMeasure(itemStyle,
                (knownDimensions, availableSpace) -> new FloatSize(100f, 10f));
            NodeId root = tree.newWithChildren(grid, item);

            tree.computeLayout(root, TaffySize.maxContent());

            // The scroll container's automatic minimum is zero, so the track is sized by the fixed
            // 10px max; its min-content contribution does not raise the base size.
            assertSize(tree.getLayout(item), 10f, 20f, "scroll container");
        }

        @Test
        @DisplayName("a sizing keyword keeps its content-based growth")
        void sizingKeywordKeepsContentGrowth() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(20f));
            grid.justifyContent = AlignContent.START;
            grid.gridTemplateColumns = List.of(TrackSizingFunction.auto());
            grid.gridTemplateRows = List.of(TrackSizingFunction.fixed(20f));
            TaffyStyle itemStyle = new TaffyStyle();
            itemStyle.size = new TaffySize<>(TaffyDimension.maxContent(), TaffyDimension.length(10f));
            NodeId item = tree.newLeafWithMeasure(itemStyle,
                (knownDimensions, availableSpace) -> {
                    if (availableSpace.width.isMinContent()) return new FloatSize(40f, 10f);
                    return new FloatSize(100f, 10f);
                });
            NodeId root = tree.newWithChildren(grid, item);

            tree.computeLayout(root, TaffySize.maxContent());

            // width:max-content resolves through content measurement: the auto track starts at the
            // 40px min-content contribution and grows to the 100px max-content contribution.
            assertSize(tree.getLayout(item), 100f, 10f, "keyword item");
        }

        @Test
        @DisplayName("compressible replaced elements cap their automatic minimum")
        void compressibleReplacedCap() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = grid();
            grid.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(20f));
            grid.justifyContent = AlignContent.START;
            grid.gridTemplateColumns = List.of(TrackSizingFunction.auto());
            grid.gridTemplateRows = List.of(TrackSizingFunction.fixed(20f));
            TaffyStyle itemStyle = new TaffyStyle();
            itemStyle.itemIsCompressibleReplaced = true;
            itemStyle.size = new TaffySize<>(TaffyDimension.length(30f), TaffyDimension.length(10f));
            itemStyle.alignSelf = dev.vfyjxf.taffy.style.AlignItems.START;
            NodeId item = tree.newLeafWithMeasure(itemStyle,
                (knownDimensions, availableSpace) -> new FloatSize(100f, 10f));
            NodeId root = tree.newWithChildren(grid, item);

            tree.computeLayout(root, TaffySize.maxContent());

            // The content-based minimum (100px) is capped by the definite 30px preferred size.
            assertSize(tree.getLayout(item), 30f, 10f, "compressible replaced item");
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
