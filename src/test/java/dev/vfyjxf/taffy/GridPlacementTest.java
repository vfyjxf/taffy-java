package dev.vfyjxf.taffy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.GridAutoFlow;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.GridTestAdapter;
import dev.vfyjxf.taffy.tree.TrackCounts;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Grid placement algorithm tests ported from taffy/src/compute/grid/placement.rs
 */
public class GridPlacementTest {

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

    private static void assertPlacement(GridTestAdapter.PlacementResult result, int childIndex,
                                        int colStart, int colEnd, int rowStart, int rowEnd) {
        assertNotNull(result);
        assertTrue(childIndex >= 0 && childIndex < result.items.size(), "childIndex in range");
        GridTestAdapter.PlacementItem item = result.items.get(childIndex);
        assertEquals(colStart, item.columnStart, "child " + childIndex + " columnStart");
        assertEquals(colEnd, item.columnEnd, "child " + childIndex + " columnEnd");
        assertEquals(rowStart, item.rowStart, "child " + childIndex + " rowStart");
        assertEquals(rowEnd, item.rowEnd, "child " + childIndex + " rowEnd");
    }

    @Test
    @DisplayName("test_only_fixed_placement")
    void testOnlyFixedPlacement() {
        GridAutoFlow flow = GridAutoFlow.ROW;
        int explicitCols = 2;
        int explicitRows = 2;
        List<TaffyStyle> children = List.of(
            child(line(1), auto(), line(1), auto()),
            child(line(-4), auto(), line(-3), auto()),
            child(line(-3), auto(), line(-4), auto()),
            child(line(3), span(2), line(5), auto())
        );

        GridTestAdapter.PlacementResult result = GridTestAdapter.runPlacement(explicitCols, explicitRows, children, flow);

        assertPlacement(result, 0, 0, 1, 0, 1);
        assertPlacement(result, 1, -1, 0, 0, 1);
        assertPlacement(result, 2, 0, 1, -1, 0);
        assertPlacement(result, 3, 2, 4, 4, 5);

        assertCounts(result.columnCounts, 1, 2, 2, "column track counts");
        assertCounts(result.rowCounts, 1, 2, 3, "row track counts");
    }

    @Test
    @DisplayName("test_placement_spanning_origin")
    void testPlacementSpanningOrigin() {
        GridAutoFlow flow = GridAutoFlow.ROW;
        int explicitCols = 2;
        int explicitRows = 2;
        List<TaffyStyle> children = List.of(
            child(line(-1), line(-1), line(-1), line(-1)),
            child(line(-1), span(2), line(-1), span(2)),
            child(line(-4), line(-4), line(-4), line(-4)),
            child(line(-4), span(2), line(-4), span(2))
        );

        GridTestAdapter.PlacementResult result = GridTestAdapter.runPlacement(explicitCols, explicitRows, children, flow);

        assertPlacement(result, 0, 2, 3, 2, 3);
        assertPlacement(result, 1, 2, 4, 2, 4);
        assertPlacement(result, 2, -1, 0, -1, 0);
        assertPlacement(result, 3, -1, 1, -1, 1);

        assertCounts(result.columnCounts, 1, 2, 2, "column track counts");
        assertCounts(result.rowCounts, 1, 2, 2, "row track counts");
    }

    @Test
    @DisplayName("test_only_auto_placement_row_flow")
    void testOnlyAutoPlacementRowFlow() {
        GridAutoFlow flow = GridAutoFlow.ROW;
        int explicitCols = 2;
        int explicitRows = 2;
        List<TaffyStyle> children = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            children.add(child(auto(), auto(), auto(), auto()));
        }

        GridTestAdapter.PlacementResult result = GridTestAdapter.runPlacement(explicitCols, explicitRows, children, flow);

        assertPlacement(result, 0, 0, 1, 0, 1);
        assertPlacement(result, 1, 1, 2, 0, 1);
        assertPlacement(result, 2, 0, 1, 1, 2);
        assertPlacement(result, 3, 1, 2, 1, 2);
        assertPlacement(result, 4, 0, 1, 2, 3);
        assertPlacement(result, 5, 1, 2, 2, 3);
        assertPlacement(result, 6, 0, 1, 3, 4);
        assertPlacement(result, 7, 1, 2, 3, 4);

        assertCounts(result.columnCounts, 0, 2, 0, "column track counts");
        assertCounts(result.rowCounts, 0, 2, 2, "row track counts");
    }

    @Test
    @DisplayName("test_only_auto_placement_column_flow")
    void testOnlyAutoPlacementColumnFlow() {
        GridAutoFlow flow = GridAutoFlow.COLUMN;
        int explicitCols = 2;
        int explicitRows = 2;
        List<TaffyStyle> children = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            children.add(child(auto(), auto(), auto(), auto()));
        }

        GridTestAdapter.PlacementResult result = GridTestAdapter.runPlacement(explicitCols, explicitRows, children, flow);

        assertPlacement(result, 0, 0, 1, 0, 1);
        assertPlacement(result, 1, 0, 1, 1, 2);
        assertPlacement(result, 2, 1, 2, 0, 1);
        assertPlacement(result, 3, 1, 2, 1, 2);
        assertPlacement(result, 4, 2, 3, 0, 1);
        assertPlacement(result, 5, 2, 3, 1, 2);
        assertPlacement(result, 6, 3, 4, 0, 1);
        assertPlacement(result, 7, 3, 4, 1, 2);

        assertCounts(result.columnCounts, 0, 2, 2, "column track counts");
        assertCounts(result.rowCounts, 0, 2, 0, "row track counts");
    }

    @Test
    @DisplayName("test_oversized_item")
    void testOversizedItem() {
        GridAutoFlow flow = GridAutoFlow.ROW;
        int explicitCols = 2;
        int explicitRows = 2;
        List<TaffyStyle> children = List.of(
            child(span(5), auto(), auto(), auto())
        );

        GridTestAdapter.PlacementResult result = GridTestAdapter.runPlacement(explicitCols, explicitRows, children, flow);

        assertPlacement(result, 0, 0, 5, 0, 1);
        assertCounts(result.columnCounts, 0, 2, 3, "column track counts");
        assertCounts(result.rowCounts, 0, 2, 0, "row track counts");
    }

    @Test
    @DisplayName("test_fixed_in_secondary_axis")
    void testFixedInSecondaryAxis() {
        GridAutoFlow flow = GridAutoFlow.ROW;
        int explicitCols = 2;
        int explicitRows = 2;
        List<TaffyStyle> children = List.of(
            child(span(2), auto(), line(1), auto()),
            child(auto(), auto(), line(2), auto()),
            child(auto(), auto(), line(1), auto()),
            child(auto(), auto(), line(4), auto())
        );

        GridTestAdapter.PlacementResult result = GridTestAdapter.runPlacement(explicitCols, explicitRows, children, flow);

        assertPlacement(result, 0, 0, 2, 0, 1);
        assertPlacement(result, 1, 0, 1, 1, 2);
        assertPlacement(result, 2, 2, 3, 0, 1);
        assertPlacement(result, 3, 0, 1, 3, 4);

        assertCounts(result.columnCounts, 0, 2, 1, "column track counts");
        assertCounts(result.rowCounts, 0, 2, 2, "row track counts");
    }

    @Test
    @DisplayName("test_definite_in_secondary_axis_with_fully_definite_negative")
    void testDefiniteInSecondaryAxisWithFullyDefiniteNegative() {
        GridAutoFlow flow = GridAutoFlow.ROW;
        int explicitCols = 2;
        int explicitRows = 2;
        List<TaffyStyle> children = List.of(
            child(auto(), auto(), line(2), auto()),
            child(line(-4), auto(), line(2), auto()),
            child(auto(), auto(), line(1), auto())
        );

        GridTestAdapter.PlacementResult result = GridTestAdapter.runPlacement(explicitCols, explicitRows, children, flow);

        assertPlacement(result, 0, 0, 1, 1, 2);
        assertPlacement(result, 1, -1, 0, 1, 2);
        assertPlacement(result, 2, -1, 0, 0, 1);

        assertCounts(result.columnCounts, 1, 2, 0, "column track counts");
        assertCounts(result.rowCounts, 0, 2, 0, "row track counts");
    }

    @Test
    @DisplayName("test_dense_packing_algorithm")
    void testDensePackingAlgorithm() {
        GridAutoFlow flow = GridAutoFlow.ROW_DENSE;
        int explicitCols = 4;
        int explicitRows = 4;
        List<TaffyStyle> children = List.of(
            child(line(2), auto(), line(1), auto()),
            child(span(2), auto(), auto(), auto()),
            child(auto(), auto(), auto(), auto())
        );

        GridTestAdapter.PlacementResult result = GridTestAdapter.runPlacement(explicitCols, explicitRows, children, flow);

        assertPlacement(result, 0, 1, 2, 0, 1);
        assertPlacement(result, 1, 2, 4, 0, 1);
        assertPlacement(result, 2, 0, 1, 0, 1);

        assertCounts(result.columnCounts, 0, 4, 0, "column track counts");
        assertCounts(result.rowCounts, 0, 4, 0, "row track counts");
    }

    @Test
    @DisplayName("test_sparse_packing_algorithm")
    void testSparsePackingAlgorithm() {
        GridAutoFlow flow = GridAutoFlow.ROW;
        int explicitCols = 4;
        int explicitRows = 4;
        List<TaffyStyle> children = List.of(
            child(auto(), span(3), auto(), auto()),
            child(auto(), span(3), auto(), auto()),
            child(auto(), span(1), auto(), auto())
        );

        GridTestAdapter.PlacementResult result = GridTestAdapter.runPlacement(explicitCols, explicitRows, children, flow);

        assertPlacement(result, 0, 0, 3, 0, 1);
        assertPlacement(result, 1, 0, 3, 1, 2);
        assertPlacement(result, 2, 3, 4, 1, 2);

        assertCounts(result.columnCounts, 0, 4, 0, "column track counts");
        assertCounts(result.rowCounts, 0, 4, 0, "row track counts");
    }

    @Test
    @DisplayName("test_auto_placement_in_negative_tracks")
    void testAutoPlacementInNegativeTracks() {
        GridAutoFlow flow = GridAutoFlow.ROW_DENSE;
        int explicitCols = 2;
        int explicitRows = 2;
        List<TaffyStyle> children = List.of(
            child(line(-5), auto(), line(1), auto()),
            child(auto(), auto(), line(2), auto()),
            child(auto(), auto(), auto(), auto())
        );

        GridTestAdapter.PlacementResult result = GridTestAdapter.runPlacement(explicitCols, explicitRows, children, flow);

        assertPlacement(result, 0, -2, -1, 0, 1);
        assertPlacement(result, 1, -2, -1, 1, 2);
        assertPlacement(result, 2, -1, 0, 0, 1);

        assertCounts(result.columnCounts, 2, 2, 0, "column track counts");
        assertCounts(result.rowCounts, 0, 2, 0, "row track counts");
    }
}
