package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.GridAutoFlow;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.GridTemplateComponent;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * P7 grid placement/sizing batch: upstream-equivalent fixtures for
 * {@code alignment-changes.md} GR-07 and GR-13.
 *
 * <ul>
 *   <li><b>GR-07</b> ({@code 50e3af61} #946): the auto-repeat repetition count accounts for every
 *       track contributed by single definitions and fixed-count {@code repeat()}s (not only the
 *       tracks that resolve to a definite size), and an auto-fit repetition at the end of the
 *       template collapses its trailing tracks <em>and</em> the gutters before them.</li>
 *   <li><b>GR-13</b> ({@code c454065e} #1015, {@code ad2a5ee5} #1038, {@code 16d3443e} #1263):
 *       sparse per-track occupancy intervals, collision-jumping auto-placement search, and
 *       per-track sparse placement cursors.</li>
 * </ul>
 *
 * <p>Fixture numbers are taken from the upstream generated XML expectations (Chrome layout
 * oracle) or, where the Java generated-test corpus predates the fixture, from a probe of the
 * current Rust taffy implementation. Note that the Java public API does not expose the resolved
 * {@code grid-template-rows/columns} track lists yet (upstream GR-18 detailed grid info is not
 * ported), so the track geometry is asserted through the resulting container and item rectangles,
 * exactly as the generated tests do.</p>
 */
class GridAutoFitP7Test {

    private static final float EPSILON = 0.01f;

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static TaffyStyle grid() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        return style;
    }

    private static TrackSizingFunction px(float value) {
        return TrackSizingFunction.fixed(LengthPercentage.length(value));
    }

    private static void assertLayout(Layout layout, float x, float y, float width, float height, String label) {
        assertEquals(x, layout.location().x, EPSILON, label + " x");
        assertEquals(y, layout.location().y, EPSILON, label + " y");
        assertEquals(width, layout.size().width, EPSILON, label + " width");
        assertEquals(height, layout.size().height, EPSILON, label + " height");
    }

    private static void assertSize(Layout layout, float width, float height, String label) {
        assertEquals(width, layout.size().width, EPSILON, label + " width");
        assertEquals(height, layout.size().height, EPSILON, label + " height");
    }

    /** An item placed at a definite row and column (both spans 1). */
    private static TaffyStyle placed(int columnLine, int rowLine) {
        TaffyStyle style = new TaffyStyle();
        style.gridColumn = new TaffyLine<>(GridPlacement.line(columnLine), GridPlacement.auto());
        style.gridRow = new TaffyLine<>(GridPlacement.line(rowLine), GridPlacement.auto());
        return style;
    }

    /** An item locked to a row and spanning the passed number of rows (auto column). */
    private static TaffyStyle rowSpan(int rowLine, int span) {
        TaffyStyle style = new TaffyStyle();
        style.gridRow = new TaffyLine<>(GridPlacement.line(rowLine), GridPlacement.span(span));
        return style;
    }

    /** An item locked to a single row (auto column). */
    private static TaffyStyle rowLine(int rowLine) {
        TaffyStyle style = new TaffyStyle();
        style.gridRow = new TaffyLine<>(GridPlacement.line(rowLine), GridPlacement.auto());
        return style;
    }

    // ------------------------------------------------------------------
    // GR-07: auto-repeat count includes every non-auto-repeat track
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // GR-13: sparse occupancy intervals, collision jumping, sparse cursors
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("GR-13: sparse occupancy intervals and placement cursors")
    class SparseOccupancyPlacement {

        @Test
        @DisplayName("grid_auto_placement_sparse_spanning_locked_items")
        void gridAutoPlacementSparseSpanningLockedItems() {
            // Upstream fixture grid_auto_placement_sparse_spanning_locked_items: with sparse packing,
            // items locked to a row are placed after items previously auto-placed with the same start
            // line, ignoring items that span in from other tracks (upstream #1263).
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = grid();
            rootStyle.gridAutoColumns = new ArrayList<>(List.of(px(50f)));
            rootStyle.gridAutoRows = new ArrayList<>(List.of(px(20f)));
            List<NodeId> children = new ArrayList<>();
            children.add(tree.newLeaf(placed(1, 2)));       // row 2, column 1
            children.add(tree.newLeaf(placed(2, 3)));       // row 3, column 2
            children.add(tree.newLeaf(rowSpan(2, 2)));            // row 2 / span 2
            children.add(tree.newLeaf(rowSpan(1, 2)));            // row 1 / span 2
            children.add(tree.newLeaf(rowLine(1)));               // row 1
            children.add(tree.newLeaf(rowLine(3)));               // row 3
            children.add(tree.newLeaf(rowLine(2)));               // row 2
            children.add(tree.newLeaf(new TaffyStyle()));         // fully auto
            NodeId root = tree.newWithChildren(rootStyle, children);

            tree.computeLayout(root, TaffySize.maxContent());

            assertSize(tree.getLayout(root), 200f, 60f, "container");
            assertLayout(tree.getLayout(children.get(0)), 0f, 20f, 50f, 20f, "child 0");
            assertLayout(tree.getLayout(children.get(1)), 50f, 40f, 50f, 20f, "child 1");
            assertLayout(tree.getLayout(children.get(2)), 100f, 20f, 50f, 40f, "child 2");
            assertLayout(tree.getLayout(children.get(3)), 50f, 0f, 50f, 40f, "child 3");
            assertLayout(tree.getLayout(children.get(4)), 100f, 0f, 50f, 20f, "child 4");
            assertLayout(tree.getLayout(children.get(5)), 0f, 40f, 50f, 20f, "child 5");
            assertLayout(tree.getLayout(children.get(6)), 150f, 20f, 50f, 20f, "child 6");
            assertLayout(tree.getLayout(children.get(7)), 0f, 0f, 50f, 20f, "child 7");
        }

        @Test
        @DisplayName("alternating_row_spans_use_sparse_row_cursors")
        void alternatingRowSpansUseSparseRowCursors() {
            // Mirrors upstream's grid-alternating-spans benchmark shape: row-locked items alternate
            // between rows 1..2 and row 2, and sparse placement walks the columns.
            int itemCount = 200;
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = grid();
            rootStyle.gridAutoColumns = new ArrayList<>(List.of(px(1f)));
            rootStyle.gridAutoRows = new ArrayList<>(List.of(px(1f)));
            rootStyle.gridTemplateColumns = new ArrayList<>();
            for (int i = 0; i < itemCount; i++) {
                rootStyle.gridTemplateColumns.add(px(1f));
            }
            List<NodeId> children = new ArrayList<>();
            for (int i = 0; i < itemCount; i++) {
                TaffyStyle itemStyle = new TaffyStyle();
                itemStyle.gridRow = (i % 2 == 0)
                    ? new TaffyLine<>(GridPlacement.line(1), GridPlacement.span(2))
                    : new TaffyLine<>(GridPlacement.line(2), GridPlacement.auto());
                children.add(tree.newLeaf(itemStyle));
            }
            NodeId root = tree.newWithChildren(rootStyle, children);

            tree.computeLayout(root, TaffySize.maxContent());

            assertLayout(tree.getLayout(children.get(0)), 0f, 0f, 1f, 2f, "item 0");
            assertLayout(tree.getLayout(children.get(1)), 1f, 1f, 1f, 1f, "item 1");
            assertLayout(tree.getLayout(children.get(2)), 2f, 0f, 1f, 2f, "item 2");
            assertLayout(tree.getLayout(children.get(3)), 3f, 1f, 1f, 1f, "item 3");
            assertLayout(tree.getLayout(children.get(itemCount - 2)), 198f, 0f, 1f, 2f, "item N-2");
            assertLayout(tree.getLayout(children.get(itemCount - 1)), 199f, 1f, 1f, 1f, "item N-1");
        }

        @Test
        @DisplayName("old_sparse_row_flow_placement_is_unchanged")
        void oldSparseRowFlowPlacementIsUnchanged() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = grid();
            rootStyle.gridTemplateColumns = new ArrayList<>(List.of(px(50f), px(50f)));
            rootStyle.gridTemplateRows = new ArrayList<>(List.of(px(20f), px(20f)));
            rootStyle.gridAutoColumns = new ArrayList<>(List.of(px(50f)));
            rootStyle.gridAutoRows = new ArrayList<>(List.of(px(20f)));
            List<NodeId> children = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                children.add(tree.newLeaf(new TaffyStyle()));
            }
            NodeId root = tree.newWithChildren(rootStyle, children);

            tree.computeLayout(root, TaffySize.maxContent());

            float[][] expected = {
                {0f, 0f}, {50f, 0f}, {0f, 20f}, {50f, 20f},
                {0f, 40f}, {50f, 40f}, {0f, 60f}, {50f, 60f}
            };
            for (int i = 0; i < expected.length; i++) {
                assertLayout(tree.getLayout(children.get(i)), expected[i][0], expected[i][1], 50f, 20f,
                    "item " + i);
            }
        }

        @Test
        @DisplayName("old_dense_column_flow_placement_is_unchanged")
        void oldDenseColumnFlowPlacementIsUnchanged() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = grid();
            rootStyle.gridAutoFlow = GridAutoFlow.COLUMN;
            rootStyle.gridTemplateColumns = new ArrayList<>(List.of(px(50f), px(50f), px(50f), px(50f)));
            rootStyle.gridTemplateRows = new ArrayList<>(List.of(px(20f), px(20f)));
            List<NodeId> children = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                children.add(tree.newLeaf(new TaffyStyle()));
            }
            NodeId root = tree.newWithChildren(rootStyle, children);

            tree.computeLayout(root, TaffySize.maxContent());

            float[][] expected = {
                {0f, 0f}, {0f, 20f}, {50f, 0f}, {50f, 20f},
                {100f, 0f}, {100f, 20f}, {150f, 0f}, {150f, 20f}
            };
            for (int i = 0; i < expected.length; i++) {
                assertLayout(tree.getLayout(children.get(i)), expected[i][0], expected[i][1], 50f, 20f,
                    "item " + i);
            }
        }

        @Test
        @DisplayName("old_dense_placement_in_negative_tracks_is_unchanged")
        void oldDensePlacementInNegativeTracksIsUnchanged() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = grid();
            rootStyle.gridAutoFlow = GridAutoFlow.ROW_DENSE;
            rootStyle.gridTemplateColumns = new ArrayList<>(List.of(px(50f), px(50f)));
            rootStyle.gridTemplateRows = new ArrayList<>(List.of(px(20f), px(20f)));
            rootStyle.gridAutoColumns = new ArrayList<>(List.of(px(50f)));
            rootStyle.gridAutoRows = new ArrayList<>(List.of(px(20f)));
            TaffyStyle firstStyle = new TaffyStyle();
            firstStyle.gridColumn = new TaffyLine<>(GridPlacement.line(-5), GridPlacement.auto());
            firstStyle.gridRow = new TaffyLine<>(GridPlacement.line(1), GridPlacement.auto());
            TaffyStyle secondStyle = new TaffyStyle();
            secondStyle.gridRow = new TaffyLine<>(GridPlacement.line(2), GridPlacement.auto());
            NodeId first = tree.newLeaf(firstStyle);
            NodeId second = tree.newLeaf(secondStyle);
            NodeId third = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(rootStyle, first, second, third);

            tree.computeLayout(root, TaffySize.maxContent());

            assertLayout(tree.getLayout(first), 0f, 0f, 50f, 20f, "first item");
            assertLayout(tree.getLayout(second), 0f, 20f, 50f, 20f, "second item");
            assertLayout(tree.getLayout(third), 50f, 0f, 50f, 20f, "third item");
        }

        @Test
        @DisplayName("small_item_in_max_size_occupied_grid_terminates")
        void smallItemInMaxSizeOccupiedGridTerminates() {
            // Upstream adversarial_styles::small_item_in_max_size_occupied_grid_terminates: a search
            // colliding with the whole grid must jump past the occupied interval instead of scanning
            // (10000 tracks)^2 candidate cells (upstream #1038).
            assertTimeoutPreemptively(java.time.Duration.ofSeconds(10), () -> {
                TaffyTree tree = new TaffyTree();
                TaffyStyle bigStyle = new TaffyStyle();
                bigStyle.gridRow = new TaffyLine<>(GridPlacement.span(10_000), GridPlacement.auto());
                bigStyle.gridColumn = new TaffyLine<>(GridPlacement.span(10_000), GridPlacement.auto());
                TaffyStyle smallStyle = new TaffyStyle();
                smallStyle.gridRow = new TaffyLine<>(GridPlacement.span(10_000), GridPlacement.auto());
                smallStyle.gridColumn = new TaffyLine<>(GridPlacement.span(1), GridPlacement.auto());
                NodeId big = tree.newLeaf(bigStyle);
                NodeId small = tree.newLeaf(smallStyle);
                TaffyStyle rootStyle = grid();
                rootStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(100f));
                NodeId root = tree.newWithChildren(rootStyle, big, small);

                tree.computeLayout(root, TaffySize.maxContent());

                assertSize(tree.getLayout(root), 100f, 100f, "container");
            });
        }

        @Test
        @DisplayName("extreme_placements_and_spans_terminate")
        void extremePlacementsAndSpansTerminate() {
            // Upstream adversarial_styles::extreme_placements_and_spans_terminate: items may span
            // thousands of tracks and be placed thousands of tracks outside the explicit grid.
            assertTimeoutPreemptively(java.time.Duration.ofSeconds(10), () -> {
                TaffyTree tree = new TaffyTree();
                TaffyStyle spanStyle = new TaffyStyle();
                spanStyle.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.span(2_000));
                spanStyle.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.span(2_000));
                TaffyStyle extremeStyle = new TaffyStyle();
                extremeStyle.gridRow = new TaffyLine<>(GridPlacement.line(-2_000), GridPlacement.line(2_000));
                extremeStyle.gridColumn = new TaffyLine<>(GridPlacement.line(-2_000), GridPlacement.line(2_000));
                NodeId span = tree.newLeaf(spanStyle);
                NodeId extreme = tree.newLeaf(extremeStyle);
                TaffyStyle rootStyle = grid();
                rootStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(100f));
                NodeId root = tree.newWithChildren(rootStyle, span, extreme);

                tree.computeLayout(root, TaffySize.maxContent());

                assertSize(tree.getLayout(root), 100f, 100f, "container");
            });
        }
    }

    @Nested
    @DisplayName("GR-07: auto-repeat count and auto-fit trailing collapse")
    class AutoRepeatAndTailCollapse {

        @Test
        @DisplayName("multi_repeat_auto_fill_after_fixed_tracks")
        void multiRepeatAutoFillAfterFixedTracks() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = grid();
            rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(20f));
            rootStyle.gap = TaffySize.all(LengthPercentage.length(10f));
            rootStyle.gridTemplateColumnsWithRepeat = new ArrayList<>(List.of(
                GridTemplateComponent.repeatCount(3, px(20f)),
                GridTemplateComponent.autoFill(px(20f))));
            rootStyle.gridTemplateRows = new ArrayList<>(List.of(px(20f)));
            TaffyStyle childStyle = new TaffyStyle();
            childStyle.gridColumn = new TaffyLine<>(GridPlacement.line(7), GridPlacement.auto());
            NodeId child = tree.newLeaf(childStyle);
            NodeId root = tree.newWithChildren(rootStyle, child);

            tree.computeLayout(root, TaffySize.maxContent());

            // 3 fixed + 4 auto-fill tracks exactly fill 200px with six 10px gutters.
            assertSize(tree.getLayout(root), 200f, 20f, "container");
            assertLayout(tree.getLayout(child), 180f, 0f, 20f, 20f, "child");
        }

        @Test
        @DisplayName("multi_repeat_auto_fill_before_fixed_tracks")
        void multiRepeatAutoFillBeforeFixedTracks() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = grid();
            rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(20f));
            rootStyle.gridTemplateColumnsWithRepeat = new ArrayList<>(List.of(
                GridTemplateComponent.autoFill(px(30f)),
                GridTemplateComponent.repeatCount(2, px(20f))));
            rootStyle.gridTemplateRows = new ArrayList<>(List.of(px(20f)));
            TaffyStyle childStyle = new TaffyStyle();
            childStyle.gridColumn = new TaffyLine<>(GridPlacement.line(6), GridPlacement.auto());
            NodeId child = tree.newLeaf(childStyle);
            NodeId root = tree.newWithChildren(rootStyle, child);

            tree.computeLayout(root, TaffySize.maxContent());

            // 5 auto-fill 30px tracks + 2 fixed 20px tracks = 190px.
            assertSize(tree.getLayout(root), 200f, 20f, "container");
            assertLayout(tree.getLayout(child), 150f, 0f, 20f, 20f, "child");
        }

        @Test
        @DisplayName("auto_repeat_count_accounts_for_non_auto_track_gutters")
        void autoRepeatCountAccountsForNonAutoTrackGutters() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = grid();
            rootStyle.size = new TaffySize<>(TaffyDimension.length(250f), TaffyDimension.length(20f));
            rootStyle.gap = TaffySize.all(LengthPercentage.length(10f));
            rootStyle.gridTemplateColumnsWithRepeat = new ArrayList<>(List.of(
                GridTemplateComponent.repeatCount(2, px(20f)),
                GridTemplateComponent.autoFill(px(30f))));
            rootStyle.gridTemplateRows = new ArrayList<>(List.of(px(20f)));
            TaffyStyle childStyle = new TaffyStyle();
            childStyle.gridColumn = new TaffyLine<>(GridPlacement.line(7), GridPlacement.auto());
            NodeId child = tree.newLeaf(childStyle);
            NodeId root = tree.newWithChildren(rootStyle, child);

            tree.computeLayout(root, TaffySize.maxContent());

            // 2 fixed + 5 auto-fill tracks: 40 + 150 = 190px of tracks and six 10px gutters = 250px.
            assertSize(tree.getLayout(root), 250f, 20f, "container");
            assertLayout(tree.getLayout(child), 220f, 0f, 30f, 20f, "child");
        }

        @Test
        @DisplayName("grid_auto_fit_last_columns_collapse_trailing_tracks_and_gutters")
        void gridAutoFitLastColumnsCollapseTrailingTracksAndGutters() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = grid();
            rootStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(50f));
            rootStyle.gap = TaffySize.all(LengthPercentage.length(10f));
            rootStyle.justifyContent = AlignContent.CENTER;
            rootStyle.gridTemplateColumnsWithRepeat = new ArrayList<>(List.of(
                GridTemplateComponent.single(px(100f)),
                GridTemplateComponent.autoFit(px(50f))));
            rootStyle.gridTemplateRows = new ArrayList<>(List.of(px(50f)));
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(rootStyle, child);

            tree.computeLayout(root, TaffySize.maxContent());

            // 3 empty auto-fit tracks collapse after placement: the used inline size is 100px (the
            // trailing gutter before the collapsed run also collapses), centered in 300px.
            assertSize(tree.getLayout(root), 300f, 50f, "container");
            assertLayout(tree.getLayout(child), 100f, 0f, 100f, 50f, "child");
        }

        @Test
        @DisplayName("grid_auto_fit_last_rows_collapse_trailing_tracks_and_gutters")
        void gridAutoFitLastRowsCollapseTrailingTracksAndGutters() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = grid();
            rootStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(300f));
            rootStyle.gap = TaffySize.all(LengthPercentage.length(10f));
            rootStyle.alignContent = AlignContent.CENTER;
            rootStyle.gridTemplateColumns = new ArrayList<>(List.of(px(100f)));
            rootStyle.gridTemplateRowsWithRepeat = new ArrayList<>(List.of(
                GridTemplateComponent.single(px(50f)),
                GridTemplateComponent.autoFit(px(30f))));
            NodeId child = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(rootStyle, child);

            tree.computeLayout(root, TaffySize.maxContent());

            // The six empty auto-fit rows collapse: the used block size is 50px (no trailing gutter),
            // centered in 300px.
            assertSize(tree.getLayout(root), 100f, 300f, "container");
            assertLayout(tree.getLayout(child), 0f, 125f, 100f, 50f, "child");
        }

        @Test
        @DisplayName("grid_auto_fit_last_collapsed_run_keeps_one_gap_after_previous_track")
        void gridAutoFitLastCollapsedRunKeepsOneGapAfterPreviousTrack() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = grid();
            rootStyle.size = new TaffySize<>(TaffyDimension.length(250f), TaffyDimension.length(50f));
            rootStyle.gap = TaffySize.all(LengthPercentage.length(5f));
            rootStyle.gridTemplateColumnsWithRepeat = new ArrayList<>(List.of(
                GridTemplateComponent.autoFit(px(30f)),
                GridTemplateComponent.single(px(50f))));
            rootStyle.gridTemplateRows = new ArrayList<>(List.of(px(50f)));
            TaffyStyle occupiedStyle = new TaffyStyle();
            occupiedStyle.gridColumn = new TaffyLine<>(GridPlacement.line(2), GridPlacement.auto());
            TaffyStyle trailingStyle = new TaffyStyle();
            trailingStyle.gridColumn = new TaffyLine<>(GridPlacement.line(6), GridPlacement.auto());
            NodeId occupied = tree.newLeaf(occupiedStyle);
            NodeId trailing = tree.newLeaf(trailingStyle);
            NodeId root = tree.newWithChildren(rootStyle, occupied, trailing);

            tree.computeLayout(root, TaffySize.maxContent());

            // Auto-fit repetition tracks 1..4 collapse; the gutter after the occupied first track
            // survives (upstream keeps the gutter that follows a non-collapsed track), so the
            // following 50px track starts at 30 + 5 = 35.
            assertLayout(tree.getLayout(occupied), 0f, 0f, 30f, 50f, "occupied auto-fit track");
            assertLayout(tree.getLayout(trailing), 35f, 0f, 50f, 50f, "track after collapsed run");
        }

        @Test
        @DisplayName("grid_auto_fit_last_with_fixed_count_repeats")
        void gridAutoFitLastWithFixedCountRepeats() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = grid();
            rootStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(50f));
            rootStyle.gap = TaffySize.all(LengthPercentage.length(10f));
            rootStyle.justifyContent = AlignContent.CENTER;
            rootStyle.gridTemplateColumnsWithRepeat = new ArrayList<>(List.of(
                GridTemplateComponent.repeatCount(2, px(50f)),
                GridTemplateComponent.autoFit(px(40f))));
            rootStyle.gridTemplateRows = new ArrayList<>(List.of(px(50f)));
            TaffyStyle firstStyle = new TaffyStyle();
            firstStyle.gridColumn = new TaffyLine<>(GridPlacement.line(1), GridPlacement.auto());
            TaffyStyle secondStyle = new TaffyStyle();
            secondStyle.gridColumn = new TaffyLine<>(GridPlacement.line(2), GridPlacement.auto());
            NodeId first = tree.newLeaf(firstStyle);
            NodeId second = tree.newLeaf(secondStyle);
            NodeId root = tree.newWithChildren(rootStyle, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            // 2 fixed tracks + 3 empty auto-fit tracks: used inline size 50 + 10 + 50 = 110px
            // centered in 300px (offset 95).
            assertLayout(tree.getLayout(first), 95f, 0f, 50f, 50f, "first fixed track");
            assertLayout(tree.getLayout(second), 155f, 0f, 50f, 50f, "second fixed track");
        }

        @Test
        @DisplayName("grid_absolute_auto_fit_overlapping_gutters")
        void gridAbsoluteAutoFitOverlappingGutters() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = grid();
            rootStyle.position = TaffyPosition.RELATIVE;
            rootStyle.size = new TaffySize<>(TaffyDimension.length(250f), TaffyDimension.length(250f));
            rootStyle.gap = TaffySize.all(LengthPercentage.length(5f));
            rootStyle.gridTemplateColumnsWithRepeat = new ArrayList<>(List.of(
                GridTemplateComponent.autoFit(px(30f)),
                GridTemplateComponent.single(px(50f))));
            rootStyle.gridTemplateRowsWithRepeat = new ArrayList<>(List.of(
                GridTemplateComponent.autoFit(px(30f)),
                GridTemplateComponent.single(px(50f))));
            TaffyStyle inFlowStyle = new TaffyStyle();
            inFlowStyle.gridRow = new TaffyLine<>(GridPlacement.line(2), GridPlacement.line(4));
            inFlowStyle.gridColumn = new TaffyLine<>(GridPlacement.line(2), GridPlacement.line(4));
            List<NodeId> children = new ArrayList<>();
            children.add(tree.newLeaf(inFlowStyle));
            int[][] absoluteAreas = {{2, 5, 2, 5}, {2, 6, 2, 6}, {4, 7, 4, 7}, {5, 7, 5, 7}, {4, 5, 4, 5}};
            for (int[] area : absoluteAreas) {
                TaffyStyle absoluteStyle = new TaffyStyle();
                absoluteStyle.position = TaffyPosition.ABSOLUTE;
                absoluteStyle.inset = TaffyRect.all(LengthPercentageAuto.length(0f));
                absoluteStyle.gridRow = new TaffyLine<>(GridPlacement.line(area[0]), GridPlacement.line(area[1]));
                absoluteStyle.gridColumn = new TaffyLine<>(GridPlacement.line(area[2]), GridPlacement.line(area[3]));
                children.add(tree.newLeaf(absoluteStyle));
            }
            NodeId root = tree.newWithChildren(rootStyle, children);

            tree.computeLayout(root, TaffySize.maxContent());

            // Upstream grid_absolute_auto_fit_overlapping_gutters__border_box_ltr
            assertSize(tree.getLayout(root), 250f, 250f, "container");
            assertLayout(tree.getLayout(children.get(0)), 0f, 0f, 65f, 65f, "in-flow item");
            assertLayout(tree.getLayout(children.get(1)), 0f, 0f, 65f, 65f, "absolute item 1");
            assertLayout(tree.getLayout(children.get(2)), 0f, 0f, 65f, 65f, "absolute item 2");
            assertLayout(tree.getLayout(children.get(3)), 70f, 70f, 50f, 50f, "absolute item 3");
            assertLayout(tree.getLayout(children.get(4)), 70f, 70f, 50f, 50f, "absolute item 4");
            assertLayout(tree.getLayout(children.get(5)), 70f, 70f, 0f, 0f, "absolute item 5");
        }

        @Test
        @DisplayName("grid_absolute_collapsed_auto_fit_tracks_rtl")
        void gridAbsoluteCollapsedAutoFitTracksRtl() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = grid();
            rootStyle.position = TaffyPosition.RELATIVE;
            rootStyle.direction = TaffyDirection.RTL;
            rootStyle.boxSizing = BoxSizing.BORDER_BOX;
            rootStyle.size = new TaffySize<>(TaffyDimension.length(530f), TaffyDimension.length(330f));
            rootStyle.padding = TaffyRect.all(LengthPercentage.length(15f));
            rootStyle.gap = TaffySize.all(LengthPercentage.length(10f));
            rootStyle.gridTemplateColumnsWithRepeat = new ArrayList<>(List.of(
                GridTemplateComponent.autoFit(px(100f))));
            rootStyle.gridTemplateRowsWithRepeat = new ArrayList<>(List.of(
                GridTemplateComponent.autoFit(px(100f))));
            TaffyStyle endStyle = new TaffyStyle();
            endStyle.position = TaffyPosition.ABSOLUTE;
            endStyle.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.line(1));
            endStyle.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.line(1));
            TaffyStyle startStyle = new TaffyStyle();
            startStyle.position = TaffyPosition.ABSOLUTE;
            startStyle.gridRow = new TaffyLine<>(GridPlacement.line(-1), GridPlacement.auto());
            startStyle.gridColumn = new TaffyLine<>(GridPlacement.line(-1), GridPlacement.auto());
            NodeId end = tree.newLeaf(endStyle);
            NodeId start = tree.newLeaf(startStyle);
            NodeId root = tree.newWithChildren(rootStyle, end, start);

            tree.computeLayout(root, TaffySize.maxContent());

            // Upstream grid_absolute_collapsed_auto_fit_tracks_rtl__border_box_ltr: every track on
            // both axes collapses, leaving a single grid line aligned within the content box.
            assertSize(tree.getLayout(root), 530f, 330f, "container");
            assertLayout(tree.getLayout(end), 530f, 0f, 0f, 0f, "auto/1 line item");
            assertLayout(tree.getLayout(start), 515f, 15f, 0f, 0f, "-1/auto line item");
        }

        @Test
        @DisplayName("multiple_auto_repetitions_invalidate_template")
        void multipleAutoRepetitionsInvalidateTemplate() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = grid();
            rootStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(100f));
            rootStyle.gridTemplateColumnsWithRepeat = new ArrayList<>(List.of(
                GridTemplateComponent.autoFit(px(30f)),
                GridTemplateComponent.autoFill(px(30f))));
            TaffyStyle itemStyle = new TaffyStyle();
            itemStyle.size = new TaffySize<>(TaffyDimension.length(30f), TaffyDimension.length(20f));
            NodeId first = tree.newLeaf(itemStyle);
            NodeId second = tree.newLeaf(itemStyle.copy());
            NodeId root = tree.newWithChildren(rootStyle, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            // An invalid template (more than one auto-repetition) contributes no explicit tracks,
            // so the two items auto-place into one implicit column and two implicit rows
            // (upstream compute_explicit_grid_size_in_axis returns (0, 0)).
            assertSize(tree.getLayout(root), 100f, 100f, "container");
            assertLayout(tree.getLayout(first), 0f, 0f, 30f, 20f, "first item");
            assertLayout(tree.getLayout(second), 0f, 50f, 30f, 20f, "second item");
        }

        @Test
        @DisplayName("auto_fit_with_fit_content_track_invalidates_template")
        void autoFitWithFitContentTrackInvalidatesTemplate() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle rootStyle = grid();
            rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(100f));
            rootStyle.gridTemplateColumnsWithRepeat = new ArrayList<>(List.of(
                GridTemplateComponent.autoFit(TrackSizingFunction.fitContent(LengthPercentage.length(100f)))));
            TaffyStyle itemStyle = new TaffyStyle();
            itemStyle.size = new TaffySize<>(TaffyDimension.length(30f), TaffyDimension.length(20f));
            NodeId first = tree.newLeaf(itemStyle);
            NodeId second = tree.newLeaf(itemStyle.copy());
            NodeId root = tree.newWithChildren(rootStyle, first, second);

            tree.computeLayout(root, TaffySize.maxContent());

            // fit-content() is not a fixed track sizing function for auto-repeat validity (upstream
            // #946), so the template is discarded and the items use the implicit grid.
            assertSize(tree.getLayout(root), 200f, 100f, "container");
            assertLayout(tree.getLayout(first), 0f, 0f, 30f, 20f, "first item");
            assertLayout(tree.getLayout(second), 0f, 50f, 30f, 20f, "second item");
        }
    }
}