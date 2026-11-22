package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.LengthPercentage;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    // ------------------------------------------------------------------
    // GR-08 (#1227): per-axis track-sizing re-run caches
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GR-08: the column re-run resolves contributions against the known row sizes")
    void rerunColumnSizingUsesKnownRowSizes() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = gridStyle();
        rootStyle.gridTemplateColumns = List.of(TrackSizingFunction.auto());
        rootStyle.gridTemplateRows = List.of(TrackSizingFunction.fixed(100.0f));

        // height: 100% is indefinite while the columns are being sized, and only resolves once the
        // rows are known. The column re-run must then recompute this item's width contribution.
        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.auto(), TaffyDimension.percent(1.0f));
        childStyle.aspectRatio = 2.0f;
        childStyle.justifySelf = dev.vfyjxf.taffy.style.AlignItems.START;
        NodeId child = tree.newLeaf(childStyle);

        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        // Residual: the container's content-sized width still uses the initial (pre-re-run) column sum
        // (upstream updates the container size when the intrinsic contribution changed -
        // grid/mod.rs "intrinsic_column_contribution_changed" block, tracked separately).
        assertRect(tree.getLayout(child), 0.0f, 0.0f, 200.0f, 100.0f, "re-run sized child");
    }

    @Test
    @DisplayName("GR-08: the per-axis contribution caches stay valid across repeated layout runs")
    void perAxisContributionCachesAreStableAcrossRuns() {
        TaffyTree tree = new TaffyTree();
        java.util.concurrent.atomic.AtomicInteger measureCount = new java.util.concurrent.atomic.AtomicInteger();

        TaffyStyle rootStyle = gridStyle();
        // A percentage column forces the column re-run, which is what exercises the per-axis
        // contribution input caches.
        rootStyle.gridTemplateColumns = List.of(
            TrackSizingFunction.percent(0.5f), TrackSizingFunction.fixed(100.0f));
        rootStyle.gridTemplateRows = List.of(TrackSizingFunction.auto());

        TaffyStyle itemStyle = new TaffyStyle();
        itemStyle.gridColumn = new TaffyLine<>(GridPlacement.line(2), GridPlacement.line(3));
        itemStyle.gridRow = new TaffyLine<>(GridPlacement.line(1), GridPlacement.line(2));
        NodeId item = tree.newLeafWithMeasure(itemStyle, (knownDimensions, availableSpace) -> {
            measureCount.incrementAndGet();
            float width = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : 30.0f;
            float height = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : 20.0f;
            return new dev.vfyjxf.taffy.geometry.FloatSize(width, height);
        });

        NodeId root = tree.newWithChildren(rootStyle, item);
        tree.computeLayout(root, TaffySize.maxContent());

        assertEquals(100.0f, tree.getLayout(item).size().width, EPSILON, "item width");
        assertEquals(20.0f, tree.getLayout(item).size().height, EPSILON, "item height");

        int firstRunMeasures = measureCount.get();
        assertEquals(100.0f, tree.getLayout(root).size().width, EPSILON, "container width");
        assertEquals(20.0f, tree.getLayout(root).size().height, EPSILON, "container height");

        // Re-running the layout with unchanged styles must not change the result and must not keep
        // growing the number of measurements (the caches are per-run, not per-tree).
        tree.markDirty(root);
        tree.computeLayout(root, TaffySize.maxContent());
        assertEquals(100.0f, tree.getLayout(item).size().width, EPSILON, "item width (second run)");
        assertEquals(20.0f, tree.getLayout(item).size().height, EPSILON, "item height (second run)");
        assertTrue(measureCount.get() - firstRunMeasures <= firstRunMeasures + 4,
            "second layout run measured " + (measureCount.get() - firstRunMeasures) + " times, first run "
            + firstRunMeasures);
    }

    // ------------------------------------------------------------------
    // GR-10 (#1219): skip row sizing when only the container's width is requested
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GR-10: a grid measured for its size still reports its full outer size")
    void computeSizeStillReturnsFullOuterSize() {
        // The horizontal-axis short-circuit added for GR-10 is unreachable in this port because every
        // caller requests RequestedAxis.BOTH (CR-01 todo), so this test guards the behaviour the
        // short-circuit must preserve: a grid measured in COMPUTE_SIZE mode during its parent's track
        // sizing still reports a complete border-box size, and its own rows are still laid out.
        TaffyTree tree = new TaffyTree();

        TaffyStyle outerStyle = gridStyle();
        outerStyle.gridTemplateColumns = List.of(TrackSizingFunction.auto());
        outerStyle.gridTemplateRows = List.of(TrackSizingFunction.auto());

        TaffyStyle innerStyle = gridStyle();
        innerStyle.gridTemplateColumns = List.of(TrackSizingFunction.fixed(40.0f));
        innerStyle.gridTemplateRows = List.of(TrackSizingFunction.fixed(30.0f));

        TaffyStyle leafStyle = new TaffyStyle();
        leafStyle.size = new TaffySize<>(TaffyDimension.length(10.0f), TaffyDimension.length(20.0f));

        NodeId leaf = tree.newLeaf(leafStyle);
        NodeId inner = tree.newWithChildren(innerStyle, leaf);
        NodeId outer = tree.newWithChildren(outerStyle, inner);

        tree.computeLayout(outer, TaffySize.maxContent());

        assertEquals(40.0f, tree.getLayout(inner).size().width, EPSILON, "inner grid width");
        assertEquals(30.0f, tree.getLayout(inner).size().height, EPSILON, "inner grid height");
        assertEquals(40.0f, tree.getLayout(outer).size().width, EPSILON, "outer grid width");
        assertEquals(30.0f, tree.getLayout(outer).size().height, EPSILON, "outer grid height");
        assertEquals(20.0f, tree.getLayout(leaf).size().height, EPSILON, "inner grid item height");
    }

    // ------------------------------------------------------------------
    // GR-14 (#1078) + OF-08 (#1071/#1073/#1266): absolute grid areas resolve to track edges
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GR-14: an empty grid positions its single line by content alignment")
    void emptyGridLineIsPositionedByContentAlignment() {
        // Upstream grid_absolute_collapsed_auto_fit_tracks_rtl uses this behaviour for an all-collapsed
        // grid: the grid still contains one line per axis, positioned by justify-content/align-content.
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = gridStyle();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200.0f), TaffyDimension.length(100.0f));
        rootStyle.justifyContent = dev.vfyjxf.taffy.style.AlignContent.CENTER;
        rootStyle.alignContent = dev.vfyjxf.taffy.style.AlignContent.END;

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.position = TaffyPosition.ABSOLUTE;
        childStyle.size = new TaffySize<>(TaffyDimension.length(10.0f), TaffyDimension.length(10.0f));
        childStyle.gridColumn = new TaffyLine<>(GridPlacement.line(1), GridPlacement.auto());
        childStyle.gridRow = new TaffyLine<>(GridPlacement.line(1), GridPlacement.auto());
        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        // The column axis has no tracks at all: justify-content: center puts its single line at 200 / 2.
        // Residual: this port always materialises one implicit row (maxSpan = 1 in computeRowCounts)
        // which stretches to the 100px content box, so the block axis never reaches the empty-axis
        // branch; auto-fit rows are not collapsed either (see the GR-14 report).
        assertRect(tree.getLayout(child), 100.0f, 0.0f, 10.0f, 10.0f, "empty grid abspos child");
    }

    @Test
    @DisplayName("GR-14: collapsed auto-fit columns align absolute grid lines (LTR and RTL)")
    void collapsedAutoFitColumnsAreAligned() {
        assertCollapsedAutoFitLine(true, dev.vfyjxf.taffy.style.AlignContent.CENTER, 250.0f);
        // In RTL the inline-start edge is the right content edge.
        assertCollapsedAutoFitLine(false, dev.vfyjxf.taffy.style.AlignContent.START, 500.0f);
        assertCollapsedAutoFitLine(false, dev.vfyjxf.taffy.style.AlignContent.END, 0.0f);
    }

    private static void assertCollapsedAutoFitLine(boolean ltr, dev.vfyjxf.taffy.style.AlignContent justifyContent,
                                                   float expectedX) {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = gridStyle();
        rootStyle.direction = ltr ? dev.vfyjxf.taffy.style.TaffyDirection.LTR
                                  : dev.vfyjxf.taffy.style.TaffyDirection.RTL;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(500.0f), TaffyDimension.length(100.0f));
        rootStyle.justifyContent = justifyContent;
        rootStyle.gridTemplateColumnsWithRepeat = List.of(
            GridTemplateComponent.autoFit(TrackSizingFunction.fixed(100.0f)));
        rootStyle.gridTemplateRows = List.of(TrackSizingFunction.fixed(100.0f));

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.position = TaffyPosition.ABSOLUTE;
        childStyle.gridColumn = new TaffyLine<>(GridPlacement.line(1), GridPlacement.auto());
        childStyle.gridRow = new TaffyLine<>(GridPlacement.line(1), GridPlacement.auto());
        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        // Every auto-fit track is collapsed, so the grid is a single line positioned by content
        // alignment; the abspos child resolves its start edge to it.
        assertEquals(expectedX, tree.getLayout(child).location().x, EPSILON,
            "collapsed auto-fit grid line x (ltr=" + ltr + ", justify-content=" + justifyContent + ")");
    }

    @Test
    @DisplayName("OF-08: abspos area spans resolve to track edges across gaps")
    void absoluteGridAreaSpansAcrossGaps() {
        // Upstream fixture grid_absolute_gaps_spans.
        TaffyTree tree = new TaffyTree();
        NodeId root = newGapsContainer(tree, dev.vfyjxf.taffy.style.TaffyDirection.LTR);

        NodeId child = newAbsoluteGapsChild(tree, GridPlacement.line(2), GridPlacement.line(4),
            GridPlacement.line(1), GridPlacement.line(3));

        tree.addChild(root, child);
        tree.computeLayout(root, TaffySize.maxContent());

        // First spanned column starts at border 5 + padding 15 + track 40 + gap 50 = 110.
        assertRect(tree.getLayout(child), 110.0f, 20.0f, 0.0f, 0.0f, "gaps span child");
    }

    @Test
    @DisplayName("OF-08: a line used as an end edge resolves to the end of the preceding track")
    void absoluteGridAreaEndLines() {
        // Upstream fixtures grid_absolute_gaps_end_lines / _rtl.
        TaffyTree ltrTree = new TaffyTree();
        NodeId ltrRoot = newGapsContainer(ltrTree, dev.vfyjxf.taffy.style.TaffyDirection.LTR);
        NodeId ltrChild = newAbsoluteGapsChild(ltrTree, GridPlacement.auto(), GridPlacement.line(2),
            GridPlacement.auto(), GridPlacement.line(2));
        ltrTree.addChild(ltrRoot, ltrChild);
        ltrTree.computeLayout(ltrRoot, TaffySize.maxContent());
        assertRect(ltrTree.getLayout(ltrChild), 5.0f, 5.0f, 0.0f, 0.0f, "LTR end lines child");

        TaffyTree rtlTree = new TaffyTree();
        NodeId rtlRoot = newGapsContainer(rtlTree, dev.vfyjxf.taffy.style.TaffyDirection.RTL);
        NodeId rtlChild = newAbsoluteGapsChild(rtlTree, GridPlacement.auto(), GridPlacement.line(2),
            GridPlacement.auto(), GridPlacement.line(2));
        rtlTree.addChild(rtlRoot, rtlChild);
        rtlTree.computeLayout(rtlRoot, TaffySize.maxContent());
        assertRect(rtlTree.getLayout(rtlChild), 435.0f, 5.0f, 0.0f, 0.0f, "RTL end lines child");
    }

    @Test
    @DisplayName("OF-08: content alignment free space is excluded from the abspos area (center)")
    void absoluteGridAreaExcludesContentAlignmentCenter() {
        // Upstream fixture grid_absolute_content_alignment_center: 100+50 tracks in a 400px content
        // box, centered -> the first track starts at (400 - 150) / 2 = 125; rows (200 - 100) / 2 = 50.
        assertAbsoluteContentAlignment(
            dev.vfyjxf.taffy.style.TaffyDirection.LTR,
            dev.vfyjxf.taffy.style.AlignContent.CENTER,
            dev.vfyjxf.taffy.style.AlignContent.CENTER,
            GridPlacement.line(1), GridPlacement.line(2), GridPlacement.line(1), GridPlacement.line(2),
            125.0f, 50.0f, 100.0f, 70.0f);
    }

    @Test
    @DisplayName("OF-08: content alignment free space is excluded from the abspos area (space-between)")
    void absoluteGridAreaExcludesContentAlignmentSpaceBetween() {
        // Upstream fixture grid_absolute_content_alignment_space_between: first track at 0, the gap
        // between the two columns absorbs the free space (250), so column 2 starts at 100 + 250 = 350.
        assertAbsoluteContentAlignment(
            dev.vfyjxf.taffy.style.TaffyDirection.LTR,
            dev.vfyjxf.taffy.style.AlignContent.SPACE_BETWEEN,
            dev.vfyjxf.taffy.style.AlignContent.SPACE_BETWEEN,
            GridPlacement.line(2), GridPlacement.line(3), GridPlacement.line(2), GridPlacement.line(3),
            350.0f, 170.0f, 50.0f, 30.0f);
    }

    @Test
    @DisplayName("OF-08: content alignment free space is excluded from the abspos area (end + RTL)")
    void absoluteGridAreaExcludesContentAlignmentEndRtl() {
        // Upstream fixture grid_absolute_content_alignment_end_rtl: in RTL justify-content: end packs to
        // the left, so the 100px first column starts at 400 - 250 - 100 = 50; rows end -> y = 100.
        assertAbsoluteContentAlignment(
            dev.vfyjxf.taffy.style.TaffyDirection.RTL,
            dev.vfyjxf.taffy.style.AlignContent.END,
            dev.vfyjxf.taffy.style.AlignContent.END,
            GridPlacement.line(1), GridPlacement.line(2), GridPlacement.line(1), GridPlacement.line(2),
            50.0f, 100.0f, 100.0f, 70.0f);
    }

    private static void assertAbsoluteContentAlignment(
        dev.vfyjxf.taffy.style.TaffyDirection direction,
        dev.vfyjxf.taffy.style.AlignContent justifyContent,
        dev.vfyjxf.taffy.style.AlignContent alignContent,
        GridPlacement columnStart, GridPlacement columnEnd,
        GridPlacement rowStart, GridPlacement rowEnd,
        float expectedX, float expectedY, float expectedWidth, float expectedHeight) {

        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = gridStyle();
        rootStyle.direction = direction;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(400.0f), TaffyDimension.length(200.0f));
        rootStyle.justifyContent = justifyContent;
        rootStyle.alignContent = alignContent;
        rootStyle.gridTemplateColumns = List.of(
            TrackSizingFunction.fixed(100.0f), TrackSizingFunction.fixed(50.0f));
        rootStyle.gridTemplateRows = List.of(
            TrackSizingFunction.fixed(70.0f), TrackSizingFunction.fixed(30.0f));

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.position = TaffyPosition.ABSOLUTE;
        childStyle.size = new TaffySize<>(TaffyDimension.percent(1.0f), TaffyDimension.percent(1.0f));
        childStyle.gridColumn = new TaffyLine<>(columnStart, columnEnd);
        childStyle.gridRow = new TaffyLine<>(rowStart, rowEnd);
        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertRect(tree.getLayout(child), expectedX, expectedY, expectedWidth, expectedHeight,
            "abspos child under content alignment " + justifyContent);
    }

    private static NodeId newGapsContainer(TaffyTree tree, dev.vfyjxf.taffy.style.TaffyDirection direction) {
        TaffyStyle style = gridStyle();
        style.direction = direction;
        style.size = new TaffySize<>(TaffyDimension.length(440.0f), TaffyDimension.length(240.0f));
        style.border = dev.vfyjxf.taffy.geometry.TaffyRect.all(LengthPercentage.length(5.0f));
        style.padding = dev.vfyjxf.taffy.geometry.TaffyRect.all(LengthPercentage.length(15.0f));
        style.gap = new TaffySize<>(LengthPercentage.length(50.0f), LengthPercentage.length(25.0f));
        style.gridTemplateColumns = List.of(
            TrackSizingFunction.fixed(40.0f), TrackSizingFunction.fixed(40.0f),
            TrackSizingFunction.fixed(40.0f), TrackSizingFunction.fixed(40.0f));
        style.gridTemplateRows = List.of(
            TrackSizingFunction.fixed(40.0f), TrackSizingFunction.fixed(40.0f));
        return tree.newLeaf(style);
    }

    private static NodeId newAbsoluteGapsChild(TaffyTree tree,
                                               GridPlacement columnStart, GridPlacement columnEnd,
                                               GridPlacement rowStart, GridPlacement rowEnd) {
        TaffyStyle style = new TaffyStyle();
        style.position = TaffyPosition.ABSOLUTE;
        style.gridColumn = new TaffyLine<>(columnStart, columnEnd);
        style.gridRow = new TaffyLine<>(rowStart, rowEnd);
        return tree.newLeaf(style);
    }

    // ------------------------------------------------------------------
    // GR-20 (#1036/#1037): bounded loops and defensive clamps for pathological styles
    // ------------------------------------------------------------------

    @Test
    @org.junit.jupiter.api.Timeout(10)
    @DisplayName("GR-20: non-finite track inputs terminate instead of looping forever")
    void nonFiniteTrackInputsTerminate() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = gridStyle();
        rootStyle.gridTemplateColumns = List.of(
            TrackSizingFunction.fr(1.0f), TrackSizingFunction.fr(1.0f));
        // Non-finite intrinsic inputs: a fixed track with a NaN length and a NaN item size.
        rootStyle.gridTemplateRows = List.of(TrackSizingFunction.fixed(Float.NaN));

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(Float.NaN), TaffyDimension.length(Float.NaN));
        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, new TaffySize<>(
            dev.vfyjxf.taffy.style.AvailableSpace.definite(Float.POSITIVE_INFINITY),
            dev.vfyjxf.taffy.style.AvailableSpace.definite(Float.POSITIVE_INFINITY)));

        // The fr sizing loops are bounded (upstream #1036), so this returns; the exact sizes are not
        // meaningful for non-finite inputs, only termination is (enforced by @Timeout).
        assertRect(tree.getLayout(child), 0.0f, 0.0f, 0.0f, 0.0f, "non-finite child");
    }

    @Test
    @org.junit.jupiter.api.Timeout(10)
    @DisplayName("GR-20: a span larger than the estimated grid terminates")
    void spanLargerThanGridTerminates() {
        // Upstream #1037 fixed an infinite loop when an auto-placed item's span exceeded the estimated
        // grid size. This port clamps spans and bounds the occupancy matrix (GR-05), so the placement
        // must terminate; the item is clamped into the limited grid.
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = gridStyle();
        rootStyle.gridTemplateColumns = List.of(TrackSizingFunction.fixed(10.0f));
        rootStyle.gridAutoColumns = List.of(TrackSizingFunction.fixed(10.0f));
        rootStyle.gridAutoRows = List.of(TrackSizingFunction.fixed(10.0f));

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(10.0f), TaffyDimension.length(10.0f));
        childStyle.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.span(10_000));
        childStyle.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.span(10_000));
        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertEquals(0.0f, tree.getLayout(child).location().x, EPSILON, "clamped child x");
        assertEquals(0.0f, tree.getLayout(child).location().y, EPSILON, "clamped child y");
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