package dev.vfyjxf.taffy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.GridTestAdapter;
import dev.vfyjxf.taffy.tree.TrackCounts;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Grid implicit sizing tests ported from taffy/src/compute/grid/implicit_grid.rs
 */
public class GridImplicitGridTest {

    private static GridPlacement auto() {
        return GridPlacement.auto();
    }

    private static GridPlacement line(int n) {
        return GridPlacement.line(n);
    }

    private static GridPlacement span(int n) {
        return GridPlacement.span(n);
    }

    private static TaffyStyle child(GridPlacement colStart, GridPlacement colEnd, GridPlacement rowStart, GridPlacement rowEnd) {
        TaffyStyle s = new TaffyStyle();
        s.display = TaffyDisplay.GRID;
        s.gridColumn = new TaffyLine<>(colStart, colEnd);
        s.gridRow = new TaffyLine<>(rowStart, rowEnd);
        return s;
    }

    private static void assertCounts(TrackCounts actual, int neg, int explicit, int pos, String label) {
        assertNotNull(actual, label);
        assertEquals(neg, actual.negativeImplicit, label + ": negativeImplicit");
        assertEquals(explicit, actual.explicit, label + ": explicit");
        assertEquals(pos, actual.positiveImplicit, label + ": positiveImplicit");
    }

    @Test
    @DisplayName("child_min_max_line_auto")
    void childMinMaxLineAuto() {
        TaffyLine<GridPlacement> gridLine = new TaffyLine<>(line(5), span(6));
        GridTestAdapter.ChildMinMaxLineSpan result = GridTestAdapter.childMinMaxLineSpan(gridLine, 6);
        assertEquals(4, result.minLine);
        assertEquals(10, result.maxLine);
        assertEquals(1, result.span);
    }

    @Test
    @DisplayName("child_min_max_line_negative_track")
    void childMinMaxLineNegativeTrack() {
        TaffyLine<GridPlacement> gridLine = new TaffyLine<>(line(-5), span(3));
        GridTestAdapter.ChildMinMaxLineSpan result = GridTestAdapter.childMinMaxLineSpan(gridLine, 6);
        assertEquals(2, result.minLine);
        assertEquals(5, result.maxLine);
        assertEquals(1, result.span);
    }

    @Test
    @DisplayName("explicit_grid_sizing_with_children")
    void explicitGridSizingWithChildren() {
        int explicitColCount = 6;
        int explicitRowCount = 8;

        List<TaffyStyle> children = List.of(
            child(line(1), span(2), line(2), auto()),
            child(line(-4), auto(), line(-2), auto())
        );

        GridTestAdapter.GridSizeEstimate estimate = GridTestAdapter.computeGridSizeEstimate(explicitColCount, explicitRowCount, children);

        assertCounts(estimate.columnCounts, 0, explicitColCount, 0, "inline");
        assertCounts(estimate.rowCounts, 0, explicitRowCount, 0, "block");
    }

    @Test
    @DisplayName("negative_implicit_grid_sizing")
    void negativeImplicitGridSizing() {
        int explicitColCount = 4;
        int explicitRowCount = 4;

        List<TaffyStyle> children = List.of(
            child(line(-6), span(2), line(-8), auto()),
            child(line(4), auto(), line(3), auto())
        );

        GridTestAdapter.GridSizeEstimate estimate = GridTestAdapter.computeGridSizeEstimate(explicitColCount, explicitRowCount, children);

        assertCounts(estimate.columnCounts, 1, explicitColCount, 0, "inline");
        assertCounts(estimate.rowCounts, 3, explicitRowCount, 0, "block");
    }
}
