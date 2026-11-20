package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P7 grid track-sizing batch: upstream-equivalent fixtures for
 * {@code alignment-changes.md} GR-01 / GR-03 / GR-04.
 *
 * <p>The fixtures are hand-ported from the Rust taffy fixtures and their generated
 * expectations (Chrome layout oracle) because the committed generated test tree is pinned to an
 * earlier fixture snapshot:</p>
 *
 * <ul>
 *   <li><b>GR-03</b> ({@code 67d6bd6b}, #1023): the grid container's min/max sizes are border-box
 *       values and must be converted to content-box before entering the track sizing algorithm
 *       ({@code grid_min_width_with_padding_border}, {@code grid_fr_tracks_min_height_with_padding_border},
 *       and the {@code box_sizing} variants).</li>
 *   <li><b>GR-04</b> ({@code 51dfca63}, #1001): space distribution must not grow a track past its
 *       growth limit, accounting for the increase already incurred for the current item
 *       ({@code grid_definite_size_auto_tracks_asymmetric_max_content}).</li>
 *   <li><b>GR-01</b> ({@code 110b1a03} #1019, {@code d2db537f} #1084): flex track intrinsic
 *       contribution distribution uses the affected-only flex factor sum, scales the content
 *       excess by {@code min(sum, 1)} anchored at the spanned inflexible tracks, and floors the
 *       result at the item's minimum contribution ({@code grid_fr_content_proportion_*},
 *       {@code grid_flex_track_intrinsic_sizes_*}).</li>
 * </ul>
 *
 * <p>Text content is emulated with the generated-test Ahem measure function (10px per character,
 * zero-width spaces mark wrapping opportunities), which is how the generated grid tests model
 * {@code <text>} fixtures.</p>
 */
class GridTrackP7Test {

    private static final float EPSILON = 0.01f;

    // ------------------------------------------------------------------
    // GR-03: container min/max converted to content-box before track sizing
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("GR-03: container min/max is content-box for track sizing")
    class ContainerMinMaxContentBox {

        @Test
        @DisplayName("grid_min_width_with_padding_border__content_box")
        void minWidthWithPaddingBorderContentBox() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = paddedBorderGrid();
            grid.boxSizing = BoxSizing.CONTENT_BOX;
            grid.minSize = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, child);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 246f, 44f, "container");
            assertLocation(tree.getLayout(child), 23f, 22f, "child");
            assertSize(tree.getLayout(child), 200f, 0f, "child");
        }

        @Test
        @DisplayName("grid_min_width_with_padding_border__border_box")
        void minWidthWithPaddingBorderBorderBox() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = paddedBorderGrid();
            grid.minSize = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, child);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 200f, 44f, "container");
            assertLocation(tree.getLayout(child), 23f, 22f, "child");
            assertSize(tree.getLayout(child), 154f, 0f, "child");
        }

        @Test
        @DisplayName("grid_min_width_with_padding_border_box_sizing__border_box")
        void minWidthWithPaddingBorderExplicitBorderBoxSizing() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = paddedBorderGrid();
            grid.boxSizing = BoxSizing.BORDER_BOX;
            grid.minSize = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, child);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 200f, 44f, "container");
            assertSize(tree.getLayout(child), 154f, 0f, "child");
        }

        @Test
        @DisplayName("grid_min_height_with_padding_border__content_box")
        void minHeightWithPaddingBorderContentBox() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = paddedBorderGrid();
            grid.boxSizing = BoxSizing.CONTENT_BOX;
            grid.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(100f));
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, child);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 46f, 144f, "container");
            assertLocation(tree.getLayout(child), 23f, 22f, "child");
            assertSize(tree.getLayout(child), 0f, 100f, "child");
        }

        @Test
        @DisplayName("grid_min_height_with_padding_border_box_sizing__content_box")
        void minHeightWithPaddingBorderExplicitBorderBoxSizing() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = paddedBorderGrid();
            grid.boxSizing = BoxSizing.BORDER_BOX;
            grid.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(100f));
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, child);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 46f, 100f, "container");
            assertSize(tree.getLayout(child), 0f, 56f, "child");
        }

        @Test
        @DisplayName("grid_fr_tracks_min_width_with_padding_border__content_box")
        void frTracksMinWidthWithPaddingBorderContentBox() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = paddedBorderGrid();
            grid.boxSizing = BoxSizing.CONTENT_BOX;
            grid.minSize = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fr(1f));
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, child);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 246f, 44f, "container");
            assertLocation(tree.getLayout(child), 23f, 22f, "child");
            assertSize(tree.getLayout(child), 200f, 0f, "child");
        }

        @Test
        @DisplayName("grid_fr_tracks_min_width_with_padding_border__border_box")
        void frTracksMinWidthWithPaddingBorderBorderBox() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = paddedBorderGrid();
            grid.minSize = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fr(1f));
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, child);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 200f, 44f, "container");
            assertSize(tree.getLayout(child), 154f, 0f, "child");
        }

        @Test
        @DisplayName("grid_fr_tracks_min_height_with_padding_border__content_box")
        void frTracksMinHeightWithPaddingBorderContentBox() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = paddedBorderGrid();
            grid.boxSizing = BoxSizing.CONTENT_BOX;
            grid.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(100f));
            grid.gridTemplateRows = List.of(TrackSizingFunction.fr(1f));
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, child);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 46f, 144f, "container");
            assertLocation(tree.getLayout(child), 23f, 22f, "child");
            assertSize(tree.getLayout(child), 0f, 100f, "child");
        }

        @Test
        @DisplayName("grid_fr_tracks_min_height_with_padding_border__border_box")
        void frTracksMinHeightWithPaddingBorderBorderBox() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = paddedBorderGrid();
            grid.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(100f));
            grid.gridTemplateRows = List.of(TrackSizingFunction.fr(1f));
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(grid, child);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 46f, 100f, "container");
            assertSize(tree.getLayout(child), 0f, 56f, "child");
        }
    }

    // ------------------------------------------------------------------
    // GR-04: space distribution does not grow tracks past their growth limit
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("GR-04: distribution respects growth limits")
    class DistributionGrowthLimits {

        @Test
        @DisplayName("grid_definite_size_auto_tracks_asymmetric_max_content")
        void definiteSizeAutoTracksAsymmetricMaxContent() {
            TaffyTree tree = new TaffyTree();

            TaffyStyle firstStyle = new TaffyStyle();
            MeasureFunc firstMeasure = ahemTextMeasure("XX\u200BXX", false);
            NodeId first = tree.newLeafWithMeasure(firstStyle, firstMeasure);

            TaffyStyle secondStyle = new TaffyStyle();
            MeasureFunc secondMeasure = ahemTextMeasure("XX\u200BXX\u200BXX\u200BXX", false);
            NodeId second = tree.newLeafWithMeasure(secondStyle, secondMeasure);

            TaffyStyle grid = new TaffyStyle();
            grid.display = TaffyDisplay.GRID;
            grid.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.AUTO);
            grid.gridTemplateColumns = List.of(TrackSizingFunction.auto(), TrackSizingFunction.auto());
            NodeId root = tree.newWithChildren(grid, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            // Chrome 151: tracks resolve to [40, 60] inside the 100px container.
            assertSize(tree.getLayout(root), 100f, 20f, "container");
            assertLocation(tree.getLayout(first), 0f, 0f, "first item");
            assertSize(tree.getLayout(first), 40f, 20f, "first item");
            assertLocation(tree.getLayout(second), 40f, 0f, "second item");
            assertSize(tree.getLayout(second), 60f, 20f, "second item");
        }

        @Test
        @DisplayName("grid_definite_size_auto_tracks_asymmetric_max_content_issue_1000")
        void definiteSizeAutoTracksAsymmetricMaxContentIssue1000() {
            TaffyTree tree = new TaffyTree();

            TaffyStyle firstStyle = new TaffyStyle();
            MeasureFunc firstMeasure = ahemTextMeasure(
                "XXXXXX\u200BXXXXXX\u200BXXXXXX\u200BXXXXXX\u200BXXXXXX\u200BXXXXXX\u200BXXXX", false);
            NodeId first = tree.newLeafWithMeasure(firstStyle, firstMeasure);

            TaffyStyle secondStyle = new TaffyStyle();
            secondStyle.maxSize = new TaffySize<>(TaffyDimension.length(674f), TaffyDimension.AUTO);
            MeasureFunc secondMeasure = ahemTextMeasure(
                "XXXXXX\u200BXXXXXX\u200BXXXXXX\u200BXXXXXX\u200BXXXXXX\u200BXXXXXX\u200BXXXXXX"
                    + "\u200BXXXXXX\u200BXXXXXX\u200BXXXXXX\u200BXXXXXX\u200BXX", false);
            NodeId second = tree.newLeafWithMeasure(secondStyle, secondMeasure);

            TaffyStyle grid = new TaffyStyle();
            grid.display = TaffyDisplay.GRID;
            grid.size = new TaffySize<>(TaffyDimension.length(928f), TaffyDimension.AUTO);
            grid.gridTemplateColumns = List.of(TrackSizingFunction.auto(), TrackSizingFunction.auto());
            NodeId root = tree.newWithChildren(grid, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            // Issue #1000 on Chrome 151: the 928px grid with min-contents 60 and max-contents 400/674
            // resolves to [400, 528]. The second track must not receive more than its growth limit.
            assertSize(tree.getLayout(root), 928f, 20f, "container");
            assertLocation(tree.getLayout(first), 0f, 0f, "first item");
            assertSize(tree.getLayout(first), 400f, 20f, "first item");
            assertLocation(tree.getLayout(second), 400f, 0f, "second item");
            assertSize(tree.getLayout(second), 528f, 20f, "second item");
        }
    }

    // ------------------------------------------------------------------
    // GR-01: flexible track intrinsic contribution distribution
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("GR-01: flex track intrinsic sizes")
    class FlexTrackIntrinsicSizes {

        @Test
        @DisplayName("grid_flex_track_intrinsic_sizes_001_span1_0fr")
        void span1ZeroFr() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = borderedGrid(50f, 50f);
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fr(0f));
            grid.gridTemplateRows = List.of(TrackSizingFunction.fr(0f));

            NodeId sized = tree.newLeaf(placed(sized(100f, 100f), 1, 1, 1, 1));
            NodeId empty = tree.newLeaf(placed(new TaffyStyle(), 1, 1, 1, 1));
            NodeId root = tree.newWithChildren(grid, sized, empty);

            tree.computeLayout(root, TaffySize.maxContent());

            // A definite-size item's minimum contribution (100) sizes the 0fr track.
            assertSize(tree.getLayout(root), 56f, 56f, "container");
            assertLocation(tree.getLayout(sized), 3f, 3f, "sized item");
            assertSize(tree.getLayout(sized), 100f, 100f, "sized item");
            assertLocation(tree.getLayout(empty), 3f, 3f, "empty item");
            assertSize(tree.getLayout(empty), 100f, 100f, "empty item");
        }

        @Test
        @DisplayName("grid_flex_track_intrinsic_sizes_001_span1_1fr")
        void span1OneFr() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = borderedGrid(50f, 50f);
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fr(1f));
            grid.gridTemplateRows = List.of(TrackSizingFunction.fr(1f));

            NodeId sized = tree.newLeaf(placed(sized(100f, 100f), 1, 1, 1, 1));
            NodeId empty = tree.newLeaf(placed(new TaffyStyle(), 1, 1, 1, 1));
            NodeId root = tree.newWithChildren(grid, sized, empty);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 56f, 56f, "container");
            assertLocation(tree.getLayout(sized), 3f, 3f, "sized item");
            assertSize(tree.getLayout(sized), 100f, 100f, "sized item");
            assertSize(tree.getLayout(empty), 100f, 100f, "empty item");
        }

        @Test
        @DisplayName("grid_flex_track_intrinsic_sizes_001_span2_0fr_1fr")
        void span2ZeroFrOneFr() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = borderedGrid(50f, 50f);
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fr(0f), TrackSizingFunction.fr(1f));
            grid.gridTemplateRows = List.of(TrackSizingFunction.fr(0f), TrackSizingFunction.fr(1f));

            NodeId spanning = tree.newLeaf(placed(sized(100f, 100f), 1, 2, 1, 2));
            NodeId first = tree.newLeaf(placed(new TaffyStyle(), 1, 1, 1, 1));
            NodeId second = tree.newLeaf(placed(new TaffyStyle(), 2, 1, 2, 1));
            NodeId root = tree.newWithChildren(grid, spanning, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            // The contribution goes only to the flexible tracks, in proportion to their flex factors:
            // the 0fr track gets nothing and the 1fr track gets the whole (unscaled, sum == 1) space.
            assertSize(tree.getLayout(root), 56f, 56f, "container");
            assertSize(tree.getLayout(spanning), 100f, 100f, "spanning item");
            assertLocation(tree.getLayout(spanning), 3f, 3f, "spanning item");
            assertSize(tree.getLayout(first), 0f, 0f, "first item");
            assertLocation(tree.getLayout(first), 3f, 3f, "first item");
            assertSize(tree.getLayout(second), 100f, 100f, "second item");
            assertLocation(tree.getLayout(second), 3f, 3f, "second item");
        }

        @Test
        @DisplayName("grid_flex_track_intrinsic_sizes_001_span2_1fr_3fr")
        void span2OneFrThreeFr() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = borderedGrid(50f, 50f);
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fr(1f), TrackSizingFunction.fr(3f));
            grid.gridTemplateRows = List.of(TrackSizingFunction.fr(1f), TrackSizingFunction.fr(3f));

            NodeId spanning = tree.newLeaf(placed(sized(100f, 100f), 1, 2, 1, 2));
            NodeId first = tree.newLeaf(placed(new TaffyStyle(), 1, 1, 1, 1));
            NodeId second = tree.newLeaf(placed(new TaffyStyle(), 2, 1, 2, 1));
            NodeId root = tree.newWithChildren(grid, spanning, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            // 1:3 split of the 100px contribution: [25, 75].
            assertSize(tree.getLayout(root), 56f, 56f, "container");
            assertSize(tree.getLayout(first), 25f, 25f, "first item");
            assertLocation(tree.getLayout(first), 3f, 3f, "first item");
            assertSize(tree.getLayout(second), 75f, 75f, "second item");
            assertLocation(tree.getLayout(second), 28f, 28f, "second item");
        }

        @Test
        @DisplayName("grid_flex_track_intrinsic_sizes_001_span2_0fr_auto")
        void span2ZeroFrAuto() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = borderedGrid(50f, 50f);
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fr(0f), TrackSizingFunction.auto());
            grid.gridTemplateRows = List.of(TrackSizingFunction.fr(0f), TrackSizingFunction.auto());

            NodeId spanning = tree.newLeaf(placed(sized(100f, 100f), 1, 2, 1, 2));
            NodeId first = tree.newLeaf(placed(new TaffyStyle(), 1, 1, 1, 1));
            NodeId second = tree.newLeaf(placed(new TaffyStyle(), 2, 1, 2, 1));
            NodeId root = tree.newWithChildren(grid, spanning, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            // A single affected flexible track takes the whole contribution even at 0fr (equal
            // distribution fallback), so the 0fr track gets 100px and the auto track stays empty.
            assertSize(tree.getLayout(root), 56f, 56f, "container");
            assertSize(tree.getLayout(spanning), 100f, 100f, "spanning item");
            assertLocation(tree.getLayout(spanning), 3f, 3f, "spanning item");
            assertSize(tree.getLayout(first), 100f, 100f, "first item");
            assertSize(tree.getLayout(second), 0f, 0f, "second item");
            assertLocation(tree.getLayout(second), 103f, 103f, "second item");
        }

        @Test
        @DisplayName("grid_flex_track_intrinsic_sizes_001_span2_0fr_0fr")
        void span2ZeroFrZeroFr() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = borderedGrid(50f, 50f);
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fr(0f), TrackSizingFunction.fr(0f));
            grid.gridTemplateRows = List.of(TrackSizingFunction.fr(0f), TrackSizingFunction.fr(0f));

            NodeId spanning = tree.newLeaf(placed(sized(100f, 100f), 1, 2, 1, 2));
            NodeId first = tree.newLeaf(placed(new TaffyStyle(), 1, 1, 1, 1));
            NodeId second = tree.newLeaf(placed(new TaffyStyle(), 2, 1, 2, 1));
            NodeId root = tree.newWithChildren(grid, spanning, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            // Both flex factor sums are zero, so the 100px definite contribution is split equally.
            assertSize(tree.getLayout(root), 56f, 56f, "container");
            assertSize(tree.getLayout(spanning), 100f, 100f, "spanning item");
            assertSize(tree.getLayout(first), 50f, 50f, "first item");
            assertLocation(tree.getLayout(first), 3f, 3f, "first item");
            assertSize(tree.getLayout(second), 50f, 50f, "second item");
            assertLocation(tree.getLayout(second), 53f, 53f, "second item");
        }

        @Test
        @DisplayName("flexible track expansion happens before auto track stretch")
        void expansionBeforeAutoStretch() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = new TaffyStyle();
            grid.display = TaffyDisplay.GRID;
            grid.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(10f));
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fr(1f), TrackSizingFunction.auto());

            NodeId flexible = tree.newLeaf(placed(sized(10f, 10f), 1, 1, 1, 1));
            NodeId auto = tree.newLeaf(placed(sized(10f, 10f), 2, 1, 1, 1));
            NodeId root = tree.newWithChildren(grid, flexible, auto);

            tree.computeLayout(root, TaffySize.maxContent());

            // Upstream order is maximise -> expand flexible tracks -> stretch auto tracks. The auto
            // track keeps its 10px content size and the flexible track absorbs the remaining 90px;
            // stretching first would put the 90px into the auto track and place the second item at x=0.
            assertSize(tree.getLayout(root), 100f, 10f, "container");
            assertLocation(tree.getLayout(flexible), 0f, 0f, "flexible item");
            assertLocation(tree.getLayout(auto), 90f, 0f, "auto item");
            assertSize(tree.getLayout(auto), 10f, 10f, "auto item");
        }

        @Test
        @DisplayName("grid_flex_track_intrinsic_sizes_003_automin_span1_0fr")
        void autominSpan1ZeroFr() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = borderedGrid(60f, 60f);
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fr(0f));
            grid.gridTemplateRows = List.of(TrackSizingFunction.fr(0f));

            NodeId inner = tree.newLeaf(sized(100f, 100f));
            NodeId item = tree.newWithChildren(placed(new TaffyStyle(), 1, 1, 1, 1), inner);
            NodeId empty = tree.newLeaf(placed(new TaffyStyle(), 1, 1, 1, 1));
            NodeId root = tree.newWithChildren(grid, item, empty);

            tree.computeLayout(root, TaffySize.maxContent());

            // Automatic minimum size: the 0fr track receives the item's min-content contribution.
            assertSize(tree.getLayout(root), 66f, 66f, "container");
            assertSize(tree.getLayout(item), 100f, 100f, "item");
            assertLocation(tree.getLayout(item), 3f, 3f, "item");
            assertSize(tree.getLayout(inner), 100f, 100f, "inner");
            assertSize(tree.getLayout(empty), 100f, 100f, "empty item");
        }

        @Test
        @DisplayName("grid_flex_track_intrinsic_sizes_003_fixedmin_span1_1fr")
        void fixedminSpan1OneFr() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = borderedGrid(60f, 60f);
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fr(1f));
            grid.gridTemplateRows = List.of(TrackSizingFunction.fr(1f));

            NodeId inner = tree.newLeaf(sized(100f, 100f));
            TaffyStyle itemStyle = placed(new TaffyStyle(), 1, 1, 1, 1);
            itemStyle.minSize = new TaffySize<>(TaffyDimension.length(50f), TaffyDimension.length(50f));
            NodeId item = tree.newWithChildren(itemStyle, inner);
            NodeId empty = tree.newLeaf(placed(new TaffyStyle(), 1, 1, 1, 1));
            NodeId root = tree.newWithChildren(grid, item, empty);

            tree.computeLayout(root, TaffySize.maxContent());

            // The specified 50px minimum sizes the base and the definite 60px space expands the 1fr track.
            assertSize(tree.getLayout(root), 66f, 66f, "container");
            assertSize(tree.getLayout(item), 60f, 60f, "item");
            assertLocation(tree.getLayout(item), 3f, 3f, "item");
            assertSize(tree.getLayout(inner), 60f, 100f, "inner");
            assertSize(tree.getLayout(empty), 60f, 60f, "empty item");
        }

        @Test
        @DisplayName("grid_flex_track_intrinsic_sizes_001_span2_1fr_minmax_25px_1fr")
        void span2OneFrMinmax25OneFr() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle grid = borderedGrid(50f, 50f);
            grid.gridTemplateColumns = List.of(TrackSizingFunction.fr(1f),
                TrackSizingFunction.minmax(TrackSizingFunction.fixed(25f), TrackSizingFunction.fr(1f)));
            grid.gridTemplateRows = List.of(TrackSizingFunction.fr(1f),
                TrackSizingFunction.minmax(TrackSizingFunction.fixed(25f), TrackSizingFunction.fr(1f)));

            NodeId spanning = tree.newLeaf(placed(sized(100f, 100f), 1, 2, 1, 2));
            NodeId first = tree.newLeaf(placed(new TaffyStyle(), 1, 1, 1, 1));
            NodeId second = tree.newLeaf(placed(new TaffyStyle(), 2, 1, 2, 1));
            NodeId root = tree.newWithChildren(grid, spanning, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            // Both tracks are flexible (min fixed 25px on the second); the 100px contribution splits
            // 1:1 and the fixed 25px minimum then floors the second track, giving [75, 25].
            assertSize(tree.getLayout(root), 56f, 56f, "container");
            assertSize(tree.getLayout(first), 75f, 75f, "first item");
            assertLocation(tree.getLayout(first), 3f, 3f, "first item");
            assertSize(tree.getLayout(second), 25f, 25f, "second item");
            assertLocation(tree.getLayout(second), 78f, 78f, "second item");
        }
    }

    @Nested
    @DisplayName("GR-01: fr content proportion (0fr collapse)")
    class FrContentProportion {

        @Test
        @DisplayName("grid_fr_content_proportion_sub_1_sum")
        void subOneSum() {
            TaffyTree tree = new TaffyTree();
            NodeId root = flexParentWithGrid(tree, 20f, 150f, TrackSizingFunction.fr(0.5f), 0f);
            NodeId grid = tree.getChildren(root).get(0);
            NodeId item = tree.getChildren(grid).get(0);

            tree.computeLayout(root, TaffySize.maxContent());

            // Max-content measure pass: a 0.5fr row with a min-height:0 item scales the 100px content
            // contribution by min(0.5, 1) -> 50px. The definite layout pass then resolves the same
            // 0.5fr row inside the 50px inner height to 25px.
            assertEquals(50f, tree.getLayout(grid).size().height, EPSILON, "grid height");
            assertEquals(25f, tree.getLayout(item).size().height, EPSILON, "row height");
        }

        @Test
        @DisplayName("grid_fr_content_proportion_sub_1_sum_minimum_below_share")
        void subOneSumMinimumBelowShare() {
            TaffyTree tree = new TaffyTree();
            NodeId root = flexParentWithGrid(tree, 20f, 150f, TrackSizingFunction.fr(0.5f), 40f);
            NodeId grid = tree.getChildren(root).get(0);
            NodeId item = tree.getChildren(grid).get(0);

            tree.computeLayout(root, TaffySize.maxContent());

            // The item's minimum contribution (40) is the floor: the row is 40px in the definite pass.
            assertEquals(50f, tree.getLayout(grid).size().height, EPSILON, "grid height");
            assertEquals(40f, tree.getLayout(item).size().height, EPSILON, "row height");
        }

        @Test
        @DisplayName("grid_fr_content_proportion_sub_1_sum_minimum_above_share")
        void subOneSumMinimumAboveShare() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle gridStyle = new TaffyStyle();
            gridStyle.display = TaffyDisplay.GRID;
            gridStyle.gridTemplateRows = List.of(TrackSizingFunction.fr(0.5f));
            gridStyle.gridTemplateColumns = List.of(TrackSizingFunction.fixed(20f));
            NodeId item = itemWithMinHeight(tree, 80f);
            NodeId root = tree.newWithChildren(gridStyle, item);

            tree.computeLayout(root, TaffySize.maxContent());

            // Scaled contribution (50) is below the minimum contribution (80), so the row is 80px.
            assertSize(tree.getLayout(root), 20f, 80f, "container");
            assertSize(tree.getLayout(item), 20f, 80f, "item");
        }

        @Test
        @DisplayName("grid_fr_content_proportion_sum_above_1")
        void sumAboveOne() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle gridStyle = new TaffyStyle();
            gridStyle.display = TaffyDisplay.GRID;
            gridStyle.gridTemplateRows = List.of(TrackSizingFunction.fr(2f));
            gridStyle.gridTemplateColumns = List.of(TrackSizingFunction.fixed(20f));
            NodeId item = itemWithMinHeight(tree, 0f);
            NodeId root = tree.newWithChildren(gridStyle, item);

            tree.computeLayout(root, TaffySize.maxContent());

            // Flex factor sum above one is not scaled: the 2fr row takes the full 100px contribution.
            assertSize(tree.getLayout(root), 20f, 100f, "container");
            assertSize(tree.getLayout(item), 20f, 100f, "item");
        }

        @Test
        @DisplayName("grid_fr_content_proportion_zero_fr_collapse")
        void zeroFrCollapse() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle gridStyle = new TaffyStyle();
            gridStyle.display = TaffyDisplay.GRID;
            gridStyle.gridTemplateRows = List.of(TrackSizingFunction.fr(0f));
            gridStyle.gridTemplateColumns = List.of(TrackSizingFunction.fixed(20f));
            NodeId item = itemWithMinHeight(tree, 0f);
            NodeId root = tree.newWithChildren(gridStyle, item);

            tree.computeLayout(root, TaffySize.maxContent());

            // A 0fr track with a min-height:0 item collapses to zero (the collapse-animation pattern).
            assertSize(tree.getLayout(root), 20f, 0f, "container");
            assertSize(tree.getLayout(item), 20f, 0f, "item");
        }

        @Test
        @DisplayName("grid_fr_minimum_zero_sum_with_other_flex_track")
        void minimumZeroSumWithOtherFlexTrack() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle gridStyle = new TaffyStyle();
            gridStyle.display = TaffyDisplay.GRID;
            gridStyle.gridTemplateRows = List.of(TrackSizingFunction.fr(0f), TrackSizingFunction.fr(1f));
            gridStyle.gridTemplateColumns = List.of(TrackSizingFunction.fixed(20f));
            NodeId first = tree.newLeaf(placed(heightSized(100f), 1, 1, 1, 1));
            NodeId root = tree.newWithChildren(gridStyle, first);

            tree.computeLayout(root, TaffySize.maxContent());

            // The used flex fraction (100px) is applied to every flexible track: [100, 100].
            assertSize(tree.getLayout(root), 20f, 200f, "container");
            assertSize(tree.getLayout(first), 20f, 100f, "first item");
        }

        @Test
        @DisplayName("grid_fr_minimum_zero_sum_with_other_flex_track_definite")
        void minimumZeroSumWithOtherFlexTrackDefinite() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle gridStyle = new TaffyStyle();
            gridStyle.display = TaffyDisplay.GRID;
            gridStyle.gridTemplateRows = List.of(TrackSizingFunction.fr(0f), TrackSizingFunction.fr(1f));
            gridStyle.gridTemplateColumns = List.of(TrackSizingFunction.fixed(20f));
            gridStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(500f));
            NodeId first = tree.newLeaf(placed(heightSized(100f), 1, 1, 1, 1));
            NodeId second = tree.newLeaf(placed(new TaffyStyle(), 1, 1, 2, 1));
            NodeId root = tree.newWithChildren(gridStyle, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            // Definite 500px: the 0fr row keeps its 100px base, the 1fr row absorbs the remaining 400.
            assertSize(tree.getLayout(root), 20f, 500f, "container");
            assertSize(tree.getLayout(first), 20f, 100f, "first item");
            assertLocation(tree.getLayout(second), 0f, 100f, "second item");
            assertSize(tree.getLayout(second), 20f, 400f, "second item");
        }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static TaffyStyle sized(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle heightSized(float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(height));
        return style;
    }

    /** A content-box grid of the given content size with a 3px border (the flex-track fixtures). */
    private static TaffyStyle borderedGrid(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        style.boxSizing = BoxSizing.CONTENT_BOX;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        style.border = TaffyRect.all(LengthPercentage.length(3f));
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
     * The upstream {@code grid_fr_content_proportion_*} parent: a definite-size flex parent with
     * {@code align-items: start} and a 20px-wide grid child whose only row is {@code rowTrack} and
     * whose only item (with the given min-height) wraps a 20x100 box.
     *
     * @return the flex parent node (the grid is its first child, the item the grid's first child)
     */
    private static NodeId flexParentWithGrid(TaffyTree tree, float width, float height,
                                             TrackSizingFunction rowTrack, float itemMinHeight) {
        TaffyStyle parentStyle = new TaffyStyle();
        parentStyle.display = TaffyDisplay.FLEX;
        parentStyle.alignItems = dev.vfyjxf.taffy.style.AlignItems.START;
        parentStyle.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));

        TaffyStyle gridStyle = new TaffyStyle();
        gridStyle.display = TaffyDisplay.GRID;
        gridStyle.gridTemplateRows = List.of(rowTrack);
        gridStyle.gridTemplateColumns = List.of(TrackSizingFunction.fixed(20f));

        NodeId item = itemWithMinHeight(tree, itemMinHeight);
        NodeId grid = tree.newWithChildren(gridStyle, item);
        return tree.newWithChildren(parentStyle, grid);
    }

    /** A grid item with the given min-height wrapping a 20x100 box. */
    private static NodeId itemWithMinHeight(TaffyTree tree, float minHeight) {
        NodeId inner = tree.newLeaf(sized(20f, 100f));
        TaffyStyle style = new TaffyStyle();
        style.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(minHeight));
        return tree.newWithChildren(style, inner);
    }

    /** The padding/border box of the upstream {@code grid_*_with_padding_border} fixtures. */
    private static TaffyStyle paddedBorderGrid() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        style.padding = new TaffyRect<>(
            LengthPercentage.length(3f), LengthPercentage.length(13f),
            LengthPercentage.length(17f), LengthPercentage.length(7f));
        style.border = new TaffyRect<>(
            LengthPercentage.length(20f), LengthPercentage.length(10f),
            LengthPercentage.length(5f), LengthPercentage.length(15f));
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

    /**
     * Ahem-style text measure used by the generated grid tests: 10x10 per character, zero-width
     * spaces are wrapping opportunities (min-content = longest segment, max-content = one line).
     */
    private static MeasureFunc ahemTextMeasure(String text, boolean vertical) {
        final String trimmed = text == null ? "" : text.trim();
        return (knownDimensions, availableSpace) -> {
            if (!Float.isNaN(knownDimensions.width) && !Float.isNaN(knownDimensions.height)) {
                return new dev.vfyjxf.taffy.geometry.FloatSize(knownDimensions.width, knownDimensions.height);
            }

            final char ZWS = '\u200B';
            final float H_WIDTH = 10.0f;
            final float H_HEIGHT = 10.0f;

            String[] parts = trimmed.isEmpty() ? new String[0] : trimmed.split(String.valueOf(ZWS), -1);
            if (parts.length == 0) {
                float w = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : 0.0f;
                float h = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : 0.0f;
                return new dev.vfyjxf.taffy.geometry.FloatSize(w, h);
            }

            int minLineLength = 0;
            int maxLineLength = 0;
            for (String p : parts) {
                int len = p.length();
                if (len > minLineLength) minLineLength = len;
                maxLineLength += len;
            }

            float knownInline = vertical ? knownDimensions.height : knownDimensions.width;
            float knownBlock = vertical ? knownDimensions.width : knownDimensions.height;
            AvailableSpace availInline = vertical ? availableSpace.height : availableSpace.width;

            float inlineSize;
            if (!Float.isNaN(knownInline)) {
                inlineSize = knownInline;
            } else if (availInline != null && availInline.isMinContent()) {
                inlineSize = minLineLength * H_WIDTH;
            } else if (availInline != null && availInline.isMaxContent()) {
                inlineSize = maxLineLength * H_WIDTH;
            } else if (availInline != null && availInline.isDefinite()) {
                inlineSize = Math.min(availInline.getValue(), maxLineLength * H_WIDTH);
            } else {
                inlineSize = maxLineLength * H_WIDTH;
            }
            inlineSize = Math.max(inlineSize, minLineLength * H_WIDTH);

            float blockSize;
            if (!Float.isNaN(knownBlock)) {
                blockSize = knownBlock;
            } else {
                int inlineLineLength = (int) Math.floor(inlineSize / H_WIDTH);
                int lineCount = 1;
                int currentLineLength = 0;
                for (String p : parts) {
                    int len = p.length();
                    if (currentLineLength + len > inlineLineLength) {
                        if (currentLineLength > 0) {
                            lineCount += 1;
                        }
                        currentLineLength = len;
                    } else {
                        currentLineLength += len;
                    }
                }
                blockSize = lineCount * H_HEIGHT;
            }

            dev.vfyjxf.taffy.geometry.FloatSize computed = vertical
                ? new dev.vfyjxf.taffy.geometry.FloatSize(blockSize, inlineSize)
                : new dev.vfyjxf.taffy.geometry.FloatSize(inlineSize, blockSize);

            float outW = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : computed.width;
            float outH = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : computed.height;
            return new dev.vfyjxf.taffy.geometry.FloatSize(outW, outH);
        };
    }
}
