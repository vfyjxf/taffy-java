package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.style.Dimension;
import dev.vfyjxf.taffy.style.Display;
import dev.vfyjxf.taffy.style.GridTemplateComponent;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.Style;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vfyjxf.taffy.geometry.Rect;
import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.tree.GridTestAdapter;
import dev.vfyjxf.taffy.tree.TrackCounts;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Grid explicit sizing tests ported from taffy/src/compute/grid/explicit_grid.rs
 */
public class GridExplicitGridTest {

    private static Style grid(float width, float height, int cols, int rows) {
        Style s = new Style();
        s.display = Display.GRID;
        s.size = new Size<>(Dimension.length(width), Dimension.length(height));
        s.gridTemplateColumns = new ArrayList<>();
        s.gridTemplateRows = new ArrayList<>();
        for (int i = 0; i < cols; i++) s.gridTemplateColumns.add(TrackSizingFunction.fr(1f));
        for (int i = 0; i < rows; i++) s.gridTemplateRows.add(TrackSizingFunction.fr(1f));
        return s;
    }

    private static void assertExplicit(Style style,
                                       float innerWidth,
                                       float innerHeight,
                                       GridTestAdapter.AutoRepeatStrategy strategy,
                                       int expectedAutoColReps,
                                       int expectedColCount,
                                       int expectedAutoRowReps,
                                       int expectedRowCount) {
        GridTestAdapter.ExplicitGridSizeInAxis cols = GridTestAdapter.computeExplicitGridSizeInAxis(
            style,
            innerWidth,
            strategy,
            true
        );
        GridTestAdapter.ExplicitGridSizeInAxis rows = GridTestAdapter.computeExplicitGridSizeInAxis(
            style,
            innerHeight,
            strategy,
            false
        );

        assertEquals(expectedColCount, cols.explicitTrackCount(), "col_count");
        assertEquals(expectedRowCount, rows.explicitTrackCount(), "row_count");
        assertEquals(expectedAutoColReps, cols.autoRepetitions(), "auto_col_reps");
        assertEquals(expectedAutoRowReps, rows.autoRepetitions(), "auto_row_reps");
    }

    @Test
    @DisplayName("explicit_grid_sizing_no_repeats")
    void explicitGridSizingNoRepeats() {
        Style style = grid(600f, 600f, 2, 4);
        float preferredWidth = style.size.width.intoOption();
        float preferredHeight = style.size.height.intoOption();

        assertExplicit(
            style,
            preferredWidth,
            preferredHeight,
            GridTestAdapter.AutoRepeatStrategy.MAX_REPETITIONS_THAT_DO_NOT_OVERFLOW,
            0,
            2,
            0,
            4
        );
    }

    @Test
    @DisplayName("explicit_grid_sizing_auto_fill_exact_fit")
    void explicitGridSizingAutoFillExactFit() {
        Style style = new Style();
        style.display = Display.GRID;
        style.size = new Size<>(Dimension.length(120f), Dimension.length(80f));
        style.gridTemplateColumnsWithRepeat = new ArrayList<>();
        style.gridTemplateRowsWithRepeat = new ArrayList<>();
        style.gridTemplateColumnsWithRepeat.add(
            GridTemplateComponent.autoFill(TrackSizingFunction.fixed(LengthPercentage.length(40f)))
        );
        style.gridTemplateRowsWithRepeat.add(
            GridTemplateComponent.autoFill(TrackSizingFunction.fixed(LengthPercentage.length(20f)))
        );

        Float preferredWidth = style.size.width.intoOption();
        Float preferredHeight = style.size.height.intoOption();

        assertExplicit(
            style,
            preferredWidth,
            preferredHeight,
            GridTestAdapter.AutoRepeatStrategy.MAX_REPETITIONS_THAT_DO_NOT_OVERFLOW,
            3,
            3,
            4,
            4
        );
    }

    @Test
    @DisplayName("explicit_grid_sizing_auto_fill_non_exact_fit")
    void explicitGridSizingAutoFillNonExactFit() {
        Style style = new Style();
        style.display = Display.GRID;
        style.size = new Size<>(Dimension.length(140f), Dimension.length(90f));
        style.gridTemplateColumnsWithRepeat = new ArrayList<>();
        style.gridTemplateRowsWithRepeat = new ArrayList<>();
        style.gridTemplateColumnsWithRepeat.add(
            GridTemplateComponent.autoFill(TrackSizingFunction.fixed(LengthPercentage.length(40f)))
        );
        style.gridTemplateRowsWithRepeat.add(
            GridTemplateComponent.autoFill(TrackSizingFunction.fixed(LengthPercentage.length(20f)))
        );

        Float preferredWidth = style.size.width.intoOption();
        Float preferredHeight = style.size.height.intoOption();

        assertExplicit(
            style,
            preferredWidth,
            preferredHeight,
            GridTestAdapter.AutoRepeatStrategy.MAX_REPETITIONS_THAT_DO_NOT_OVERFLOW,
            3,
            3,
            4,
            4
        );
    }

    @Test
    @DisplayName("explicit_grid_sizing_auto_fill_min_size_exact_fit")
    void explicitGridSizingAutoFillMinSizeExactFit() {
        Style style = new Style();
        style.display = Display.GRID;
        style.minSize = new Size<>(Dimension.length(120f), Dimension.length(80f));
        style.gridTemplateColumnsWithRepeat = new ArrayList<>();
        style.gridTemplateRowsWithRepeat = new ArrayList<>();
        style.gridTemplateColumnsWithRepeat.add(
            GridTemplateComponent.autoFill(TrackSizingFunction.fixed(LengthPercentage.length(40f)))
        );
        style.gridTemplateRowsWithRepeat.add(
            GridTemplateComponent.autoFill(TrackSizingFunction.fixed(LengthPercentage.length(20f)))
        );

        // In Rust test this is the inner container size derived from min-size constraints
        Float innerWidth = 120f;
        Float innerHeight = 80f;

        assertExplicit(
            style,
            innerWidth,
            innerHeight,
            GridTestAdapter.AutoRepeatStrategy.MIN_REPETITIONS_THAT_DO_OVERFLOW,
            3,
            3,
            4,
            4
        );
    }

    @Test
    @DisplayName("explicit_grid_sizing_auto_fill_min_size_non_exact_fit")
    void explicitGridSizingAutoFillMinSizeNonExactFit() {
        Style style = new Style();
        style.display = Display.GRID;
        style.minSize = new Size<>(Dimension.length(140f), Dimension.length(90f));
        style.gridTemplateColumnsWithRepeat = new ArrayList<>();
        style.gridTemplateRowsWithRepeat = new ArrayList<>();
        style.gridTemplateColumnsWithRepeat.add(
            GridTemplateComponent.autoFill(TrackSizingFunction.fixed(LengthPercentage.length(40f)))
        );
        style.gridTemplateRowsWithRepeat.add(
            GridTemplateComponent.autoFill(TrackSizingFunction.fixed(LengthPercentage.length(20f)))
        );

        Float innerWidth = 140f;
        Float innerHeight = 90f;

        assertExplicit(
            style,
            innerWidth,
            innerHeight,
            GridTestAdapter.AutoRepeatStrategy.MIN_REPETITIONS_THAT_DO_OVERFLOW,
            4,
            4,
            5,
            5
        );
    }

    @Test
    @DisplayName("explicit_grid_sizing_auto_fill_multiple_repeated_tracks")
    void explicitGridSizingAutoFillMultipleRepeatedTracks() {
        Style style = new Style();
        style.display = Display.GRID;
        style.size = new Size<>(Dimension.length(140f), Dimension.length(100f));
        style.gridTemplateColumnsWithRepeat = new ArrayList<>();
        style.gridTemplateRowsWithRepeat = new ArrayList<>();
        style.gridTemplateColumnsWithRepeat.add(
            GridTemplateComponent.autoFill(
                TrackSizingFunction.fixed(LengthPercentage.length(40f)),
                TrackSizingFunction.fixed(LengthPercentage.length(20f))
            )
        );
        style.gridTemplateRowsWithRepeat.add(
            GridTemplateComponent.autoFill(
                TrackSizingFunction.fixed(LengthPercentage.length(20f)),
                TrackSizingFunction.fixed(LengthPercentage.length(10f))
            )
        );

        Float preferredWidth = style.size.width.intoOption();
        Float preferredHeight = style.size.height.intoOption();

        assertExplicit(
            style,
            preferredWidth,
            preferredHeight,
            GridTestAdapter.AutoRepeatStrategy.MAX_REPETITIONS_THAT_DO_NOT_OVERFLOW,
            2,
            4,
            3,
            6
        );
    }

    @Test
    @DisplayName("explicit_grid_sizing_auto_fill_gap")
    void explicitGridSizingAutoFillGap() {
        Style style = new Style();
        style.display = Display.GRID;
        style.size = new Size<>(Dimension.length(140f), Dimension.length(100f));
        style.gridTemplateColumnsWithRepeat = new ArrayList<>();
        style.gridTemplateRowsWithRepeat = new ArrayList<>();
        style.gridTemplateColumnsWithRepeat.add(
            GridTemplateComponent.autoFill(TrackSizingFunction.fixed(LengthPercentage.length(40f)))
        );
        style.gridTemplateRowsWithRepeat.add(
            GridTemplateComponent.autoFill(TrackSizingFunction.fixed(LengthPercentage.length(20f)))
        );
        style.gap = new Size<>(LengthPercentage.length(20f), LengthPercentage.length(20f));

        Float preferredWidth = style.size.width.intoOption();
        Float preferredHeight = style.size.height.intoOption();

        assertExplicit(
            style,
            preferredWidth,
            preferredHeight,
            GridTestAdapter.AutoRepeatStrategy.MAX_REPETITIONS_THAT_DO_NOT_OVERFLOW,
            2,
            2,
            3,
            3
        );
    }

    @Test
    @DisplayName("explicit_grid_sizing_no_defined_size")
    void explicitGridSizingNoDefinedSize() {
        Style style = new Style();
        style.display = Display.GRID;
        style.gridTemplateColumnsWithRepeat = new ArrayList<>();
        style.gridTemplateRowsWithRepeat = new ArrayList<>();
        style.gridTemplateColumnsWithRepeat.add(
            GridTemplateComponent.autoFill(
                TrackSizingFunction.fixed(LengthPercentage.length(40f)),
                TrackSizingFunction.fixed(LengthPercentage.percent(0.5f)),
                TrackSizingFunction.fixed(LengthPercentage.length(20f))
            )
        );
        style.gridTemplateRowsWithRepeat.add(
            GridTemplateComponent.autoFill(TrackSizingFunction.fixed(LengthPercentage.length(20f)))
        );
        style.gap = new Size<>(LengthPercentage.length(20f), LengthPercentage.length(20f));

        // No preferred size => autoFitContainerSize is null
        assertExplicit(
            style,
            Float.NaN,
            Float.NaN,
            GridTestAdapter.AutoRepeatStrategy.MIN_REPETITIONS_THAT_DO_OVERFLOW,
            1,
            3,
            1,
            1
        );
    }

    @Test
    @DisplayName("explicit_grid_sizing_mix_repeated_and_non_repeated")
    void explicitGridSizingMixRepeatedAndNonRepeated() {
        Style style = new Style();
        style.display = Display.GRID;
        style.size = new Size<>(Dimension.length(140f), Dimension.length(100f));
        style.gridTemplateColumnsWithRepeat = new ArrayList<>();
        style.gridTemplateRowsWithRepeat = new ArrayList<>();
        style.gridTemplateColumnsWithRepeat.add(GridTemplateComponent.single(TrackSizingFunction.fixed(LengthPercentage.length(20f))));
        style.gridTemplateColumnsWithRepeat.add(
            GridTemplateComponent.autoFill(TrackSizingFunction.fixed(LengthPercentage.length(40f)))
        );
        style.gridTemplateRowsWithRepeat.add(GridTemplateComponent.single(TrackSizingFunction.fixed(LengthPercentage.length(40f))));
        style.gridTemplateRowsWithRepeat.add(
            GridTemplateComponent.autoFill(TrackSizingFunction.fixed(LengthPercentage.length(20f)))
        );
        style.gap = new Size<>(LengthPercentage.length(20f), LengthPercentage.length(20f));

        Float preferredWidth = style.size.width.intoOption();
        Float preferredHeight = style.size.height.intoOption();

        assertExplicit(
            style,
            preferredWidth,
            preferredHeight,
            GridTestAdapter.AutoRepeatStrategy.MAX_REPETITIONS_THAT_DO_NOT_OVERFLOW,
            2,
            3,
            1,
            2
        );
    }

    @Test
    @DisplayName("explicit_grid_sizing_mix_with_padding")
    void explicitGridSizingMixWithPadding() {
        Style style = new Style();
        style.display = Display.GRID;
        style.size = new Size<>(Dimension.length(120f), Dimension.length(120f));
        style.padding = new Rect<>(
            LengthPercentage.length(10f),
            LengthPercentage.length(10f),
            LengthPercentage.length(20f),
            LengthPercentage.length(20f)
        );
        style.gridTemplateColumnsWithRepeat = new ArrayList<>();
        style.gridTemplateRowsWithRepeat = new ArrayList<>();
        style.gridTemplateColumnsWithRepeat.add(
            GridTemplateComponent.autoFill(TrackSizingFunction.fixed(LengthPercentage.length(20f)))
        );
        style.gridTemplateRowsWithRepeat.add(
            GridTemplateComponent.autoFill(TrackSizingFunction.fixed(LengthPercentage.length(20f)))
        );

        // Inner sizes (content box) passed explicitly, matching Rust test.
        assertExplicit(
            style,
            100f,
            80f,
            GridTestAdapter.AutoRepeatStrategy.MAX_REPETITIONS_THAT_DO_NOT_OVERFLOW,
            5,
            5,
            4,
            4
        );
    }

    @Test
    @DisplayName("test_initialize_grid_tracks")
    void testInitializeGridTracks() {
        Style style = new Style();
        style.display = Display.GRID;
        style.gap = new Size<>(LengthPercentage.length(20f), LengthPercentage.length(20f));
        style.gridTemplateColumns = List.of(
            TrackSizingFunction.fixed(LengthPercentage.length(100f)),
            TrackSizingFunction.minmax(
                TrackSizingFunction.fixed(LengthPercentage.length(100f)),
                TrackSizingFunction.fr(2f)
            ),
            TrackSizingFunction.fr(1f)
        );
        style.gridAutoColumns = List.of(
            TrackSizingFunction.auto(),
            TrackSizingFunction.fixed(LengthPercentage.length(100f))
        );

        TrackCounts counts = new TrackCounts(3, style.gridTemplateColumns.size(), 3);
        List<GridTestAdapter.DebugGridTrack> tracks = GridTestAdapter.initializeGridTracks(counts, style, true, idx -> false);

        assertEquals(19, tracks.size(), "Number of tracks doesn't match");

        // Helper to assert by string to avoid relying on TrackSizingFunction object identity
        for (int i = 0; i < tracks.size(); i++) {
            assertNotNull(tracks.get(i), "track " + i);
        }

        class Expected {
            final GridTestAdapter.DebugGridTrackKind kind;
            final String min;
            final String max;

            Expected(GridTestAdapter.DebugGridTrackKind kind, String min, String max) {
                this.kind = kind;
                this.min = min;
                this.max = max;
            }
        }

        List<Expected> expected = List.of(
            // Gutter (collapsed)
            new Expected(GridTestAdapter.DebugGridTrackKind.GUTTER, "0.0px", "0.0px"),
            // Negative implicit tracks (100, auto, 100)
            new Expected(GridTestAdapter.DebugGridTrackKind.TRACK, "100.0px", "100.0px"),
            new Expected(GridTestAdapter.DebugGridTrackKind.GUTTER, "20.0px", "20.0px"),
            new Expected(GridTestAdapter.DebugGridTrackKind.TRACK, "auto", "auto"),
            new Expected(GridTestAdapter.DebugGridTrackKind.GUTTER, "20.0px", "20.0px"),
            new Expected(GridTestAdapter.DebugGridTrackKind.TRACK, "100.0px", "100.0px"),
            new Expected(GridTestAdapter.DebugGridTrackKind.GUTTER, "20.0px", "20.0px"),
            // Explicit tracks
            new Expected(GridTestAdapter.DebugGridTrackKind.TRACK, "100.0px", "100.0px"),
            new Expected(GridTestAdapter.DebugGridTrackKind.GUTTER, "20.0px", "20.0px"),
            new Expected(GridTestAdapter.DebugGridTrackKind.TRACK, "100.0px", "2.0fr"),
            new Expected(GridTestAdapter.DebugGridTrackKind.GUTTER, "20.0px", "20.0px"),
            new Expected(GridTestAdapter.DebugGridTrackKind.TRACK, "auto", "1.0fr"),
            new Expected(GridTestAdapter.DebugGridTrackKind.GUTTER, "20.0px", "20.0px"),
            // Positive implicit tracks (auto, 100, auto)
            new Expected(GridTestAdapter.DebugGridTrackKind.TRACK, "auto", "auto"),
            new Expected(GridTestAdapter.DebugGridTrackKind.GUTTER, "20.0px", "20.0px"),
            new Expected(GridTestAdapter.DebugGridTrackKind.TRACK, "100.0px", "100.0px"),
            new Expected(GridTestAdapter.DebugGridTrackKind.GUTTER, "20.0px", "20.0px"),
            new Expected(GridTestAdapter.DebugGridTrackKind.TRACK, "auto", "auto"),
            // Last gutter (collapsed)
            new Expected(GridTestAdapter.DebugGridTrackKind.GUTTER, "0.0px", "0.0px")
        );

        assertEquals(expected.size(), tracks.size(), "Expected list length");

        for (int i = 0; i < expected.size(); i++) {
            GridTestAdapter.DebugGridTrack actual = tracks.get(i);
            Expected exp = expected.get(i);
            assertEquals(exp.kind, actual.kind, "Track " + i + " kind");
            assertEquals(exp.min, actual.min.toString(), "Track " + i + " min");
            assertEquals(exp.max, actual.max.toString(), "Track " + i + " max");
        }
    }
}
