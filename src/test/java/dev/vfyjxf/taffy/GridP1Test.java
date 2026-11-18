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

    // ------------------------------------------------------------------
    // GR-09 (#1226): cache known dimensions between min- and max-content contributions
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GR-09: intrinsic columns size aspect-ratio items from their known dimensions")
    void knownDimensionsCacheKeepsAspectRatioSizing() {
        // The second item's width is derived from its definite height through its aspect ratio, so its
        // column contribution is computed from the known dimensions of the item (GR-09).
        // Note: a percentage height here would additionally require the container size to be updated
        // after the intrinsic-contribution re-run (upstream grid/mod.rs "intrinsic_column_contribution_changed"
        // block), which this port does not implement yet; see GridP1Test report.
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = gridStyle();
        rootStyle.justifyContent = dev.vfyjxf.taffy.style.AlignContent.START;
        rootStyle.gridTemplateColumns = List.of(
            TrackSizingFunction.auto(), TrackSizingFunction.auto());
        rootStyle.gridTemplateRows = List.of(TrackSizingFunction.auto());

        TaffyStyle firstStyle = new TaffyStyle();
        firstStyle.size = new TaffySize<>(TaffyDimension.length(10.0f), TaffyDimension.length(100.0f));
        firstStyle.gridRow = new TaffyLine<>(GridPlacement.line(1), GridPlacement.line(2));
        firstStyle.gridColumn = new TaffyLine<>(GridPlacement.line(1), GridPlacement.line(2));
        NodeId first = tree.newLeaf(firstStyle);

        TaffyStyle secondStyle = new TaffyStyle();
        secondStyle.size = new TaffySize<>(TaffyDimension.auto(), TaffyDimension.length(100.0f));
        secondStyle.aspectRatio = 2.0f;
        secondStyle.justifySelf = dev.vfyjxf.taffy.style.AlignItems.START;
        secondStyle.gridRow = new TaffyLine<>(GridPlacement.line(1), GridPlacement.line(2));
        secondStyle.gridColumn = new TaffyLine<>(GridPlacement.line(2), GridPlacement.line(3));
        NodeId second = tree.newLeaf(secondStyle);

        NodeId root = tree.newWithChildren(rootStyle, first, second);
        tree.computeLayout(root, TaffySize.maxContent());

        Layout rootLayout = tree.getLayout(root);
        assertEquals(210.0f, rootLayout.size().width, EPSILON, "container width");
        assertEquals(100.0f, rootLayout.size().height, EPSILON, "container height");
        assertRect(tree.getLayout(first), 0.0f, 0.0f, 10.0f, 100.0f, "first item");
        // The 100px height with aspect-ratio: 2 gives a 200px width.
        assertRect(tree.getLayout(second), 10.0f, 0.0f, 200.0f, 100.0f, "second item");
    }

    @Test
    @DisplayName("GR-09: intrinsic caches do not leak between layout runs")
    void knownDimensionsCacheIsPerRun() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = gridStyle();
        rootStyle.gridTemplateColumns = List.of(TrackSizingFunction.auto());
        rootStyle.gridTemplateRows = List.of(TrackSizingFunction.auto());

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(30.0f), TaffyDimension.length(40.0f));
        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());
        assertEquals(30.0f, tree.getLayout(root).size().width, EPSILON, "first run width");
        assertEquals(40.0f, tree.getLayout(root).size().height, EPSILON, "first run height");

        childStyle.size = new TaffySize<>(TaffyDimension.length(70.0f), TaffyDimension.length(80.0f));
        tree.setStyle(child, childStyle);
        tree.computeLayout(root, TaffySize.maxContent());

        assertEquals(70.0f, tree.getLayout(root).size().width, EPSILON, "second run width");
        assertEquals(80.0f, tree.getLayout(root).size().height, EPSILON, "second run height");
        assertRect(tree.getLayout(child), 0.0f, 0.0f, 70.0f, 80.0f, "second run child");
    }

    private static void assertRect(Layout layout, float x, float y, float width, float height, String label) {
        assertEquals(x, layout.location().x, EPSILON, label + " x");
        assertEquals(y, layout.location().y, EPSILON, label + " y");
        assertEquals(width, layout.size().width, EPSILON, label + " width");
        assertEquals(height, layout.size().height, EPSILON, label + " height");
    }

    private static TaffyStyle gridStyle() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        return style;
    }
}