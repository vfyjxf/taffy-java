package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.GridTemplateComponent;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P1 low-cost correctness fixes for the grid algorithm.
 * <p>
 * Each test maps to an upstream change listed in {@code alignment-changes.md} §5.2 (GR-05, GR-08,
 * GR-09, GR-10, GR-14, GR-20) or §5.7 (OF-08), and is written against the upstream fixture or the
 * documented CSS Grid semantics for that change.
 */
class GridP1Test {

    private static final float EPSILON = 0.01f;

    // ------------------------------------------------------------------
    // GR-05 (#986): clamp grids at MAX_GRID_TRACKS tracks in each dimension
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GR-05: a fixed repeat count is truncated at the track limit")
    void fixedRepeatCountIsClamped() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = gridStyle();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(1_000_000.0f), TaffyDimension.auto());
        rootStyle.gridTemplateColumnsWithRepeat = List.of(
            GridTemplateComponent.repeatCount(20_000, TrackSizingFunction.fixed(10.0f)));

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.position = TaffyPosition.ABSOLUTE;
        childStyle.gridColumn = new TaffyLine<>(GridPlacement.line(-1), GridPlacement.auto());
        NodeId child = tree.newLeaf(childStyle);

        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        // 20_000 * 10px would be 200_000px; the grid is clamped to 10_000 tracks (100_000px), which
        // is the position of the last explicit line.
        assertEquals(100_000.0f, tree.getLayout(child).location().x, EPSILON,
            "abspos child at the last clamped explicit line");
    }

    @Test
    @DisplayName("GR-05: auto-fill repetitions are clamped at the track limit")
    void autoFillRepetitionIsClamped() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = gridStyle();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(1_000_000.0f), TaffyDimension.auto());
        rootStyle.gridTemplateColumnsWithRepeat = List.of(
            GridTemplateComponent.autoFill(TrackSizingFunction.fixed(10.0f)));

        // Absolutely positioned child resolved against the last explicit grid line. Its x position is
        // the end of the (clamped) track list, so it exposes the expanded track count.
        TaffyStyle childStyle = new TaffyStyle();
        childStyle.position = TaffyPosition.ABSOLUTE;
        childStyle.gridColumn = new TaffyLine<>(GridPlacement.line(-1), GridPlacement.auto());
        NodeId child = tree.newLeaf(childStyle);

        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        // The container is 1_000_000px wide, so auto-fill would generate 100_000 tracks of 10px.
        // Clamped to 10_000 tracks, the last line sits at 100_000px.
        assertEquals(100_000.0f, tree.getLayout(child).location().x, EPSILON,
            "abspos child at the last clamped auto-fill line");
    }

    @Test
    @DisplayName("GR-05: negative line numbers are clamped into the limited grid")
    void negativeLineNumbersAreClamped() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = gridStyle();
        rootStyle.gridTemplateColumns = List.of(
            TrackSizingFunction.fixed(10.0f),
            TrackSizingFunction.fixed(10.0f));
        rootStyle.gridAutoColumns = List.of(TrackSizingFunction.fixed(10.0f));

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(10.0f), TaffyDimension.length(10.0f));
        childStyle.gridColumn = new TaffyLine<>(GridPlacement.line(-100_000), GridPlacement.auto());
        NodeId child = tree.newLeaf(childStyle);

        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        // The start line is clamped to -10_000, so the grid has 10_000 implicit + 2 explicit tracks
        // of 10px each (100_020px) instead of 99_997 implicit tracks.
        assertEquals(100_020.0f, tree.getLayout(root).size().width, EPSILON,
            "content-sized width with a clamped negative line number");
        assertEquals(0.0f, tree.getLayout(child).location().x, EPSILON,
            "child placed in the first implicit track");
    }

    @Test
    @DisplayName("GR-05: huge spans are clamped at the track limit")
    void hugeSpansAreClamped() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = gridStyle();
        rootStyle.gridTemplateColumns = List.of(TrackSizingFunction.fixed(10.0f));
        rootStyle.gridAutoColumns = List.of(TrackSizingFunction.fixed(10.0f));

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(10.0f), TaffyDimension.length(10.0f));
        childStyle.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.span(100_000));
        NodeId child = tree.newLeaf(childStyle);

        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        // The span is clamped to 10_000 tracks of 10px each.
        assertEquals(100_000.0f, tree.getLayout(root).size().width, EPSILON,
            "content-sized width with a clamped column span");
        assertEquals(0.0f, tree.getLayout(child).location().x, EPSILON, "child x");
        assertEquals(10.0f, tree.getLayout(child).size().width, EPSILON, "child width");
    }

    private static TaffyStyle gridStyle() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        return style;
    }
}