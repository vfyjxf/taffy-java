package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.GridTemplateComponent;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.DetailedGridInfo;
import dev.vfyjxf.taffy.tree.GridTrackInfo;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P7 grid residuals: the upstream fixtures for the remaining GR-02 track-sizing steps (T7, T14 and
 * the limited min-content contribution) and for the GR-10/GR-14 short-circuits, plus the evidence of
 * which of them this port already reproduces.
 *
 * <ul>
 *   <li><b>GR-02 T7</b> ({@code fca50a0c} #1033): the beyond-limits space of a spanning item's
 *       max-content contribution is only distributed to tracks whose <em>max</em> track sizing
 *       function is max-content/fit-content. The upstream fixture
 *       {@code grid_content_sized_columns_max_content_and_max_content_fixed_and_max_content} is
 *       <b>already reproduced</b>: the {@code minmax(max-content, 20px)} track keeps its 20px
 *       maximum, so this port has no beyond-limits stage that could grow it.</li>
 *   <li><b>GR-02 T14</b> ({@code 3defa72e} #1245): the growth-limit space of a spanning item is
 *       only distributed to infinitely growable tracks (their growth limit is still infinite, or it
 *       was raised by the previous step) and is capped by a {@code fit-content()} argument; only
 *       the space left over is distributed beyond the limits. Implemented in
 *       {@code GridComputer.distributeSpanningItemGrowthLimits} (11.5 steps 4-5) with upstream's
 *       planned-increase/growable bookkeeping, so both upstream fixtures reproduce Chrome:
 *       {@code grid_fit_content_span_2_capped_by_non_growable_fit_content_track} (45px / 20px) and
 *       {@code grid_fit_content_span_3_between_auto_tracks} (30px / 30px / 30px). The step only
 *       runs for a definite axis: the min-/max-content constrained axis keeps this port's existing
 *       base-size model, because upstream's indefinite path also applies the limited min-content
 *       contribution rule (see the next bullet).</li>
 *   <li><b>GR-02 limited min-content</b> ({@code 1a2d05ab} #1022 step 1): under a min-/max-content
 *       constraint a non-scrolling item's min-content contribution is limited by the sum of the
 *       definite max track sizing functions it spans and floored at its minimum contribution. The
 *       upstream XML suite has no fixture where that differs from the minimum contribution this
 *       port already uses (every {@code grid_span_2_*_fit_content_*_indefinite} fixture has a
 *       content-based minimum equal to its min-content contribution, and the only fixtures with a
 *       definite preferred size are scroll containers, which the rule excludes), so the port is
 *       equivalent on every available fixture and no behaviour was invented here.</li>
 *   <li><b>GR-14</b> ({@code 9d9dcff2} #1078): an axis whose tracks are all collapsed still has one
 *       grid line, positioned by content alignment. The row-axis half of the upstream fixture
 *       {@code grid_absolute_collapsed_auto_fit_tracks_rtl} is <b>already reproduced</b> (auto-fit
 *       rows collapse to {@code 0px 0px} and the absolutely positioned child resolves its row line
 *       to the content box's block-start edge), so the residual recorded in {@code p1-residuals.md}
 *       (auto-fit rows not collapsing) no longer holds.</li>
 *   <li><b>GR-10</b> ({@code 9770a07a} #1219): the "only the container's width was requested, skip
 *       row sizing" short-circuit exists in {@code GridComputer.compute} but is unreachable in this
 *       port because every caller passes {@code RequestedAxis.BOTH}; enabling it needs the
 *       RequestedAxis plumbing (CR-01/FL-25), which lives outside this batch's files, so no test is
 *       added here beyond the existing {@code GridP1Test} guard.</li>
 * </ul>
 *
 * <p>The fixtures are transcribed from the upstream generated XML expectations (Chrome layout
 * oracle) into the Java test style used by the other P7 batches; text is measured with the same
 * Ahem-style helper the generated grid tests use.</p>
 */
class GridResidualP7Test {

    private static final float EPSILON = 0.01f;
    private static final float GLYPH = 10f;

    // ------------------------------------------------------------------
    // GR-02 T7: only max-content/fit-content maxima receive beyond-limits space
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GR-02 T7: grid_content_sized_columns_max_content_and_max_content_fixed_and_max_content")
    void beyondLimitsIgnoresFixedMaxTracks() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle grid = grid();
        grid.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.auto());
        grid.gridTemplateColumns = List.of(
            TrackSizingFunction.maxContent(),
            TrackSizingFunction.minmax(TrackSizingFunction.maxContent(), TrackSizingFunction.fixed(20f)),
            TrackSizingFunction.maxContent());
        NodeId five = tree.newLeafWithMeasure(new TaffyStyle(), text("XXXXX"));
        NodeId three = tree.newLeafWithMeasure(new TaffyStyle(), text("XXX"));
        NodeId span = tree.newLeafWithMeasure(new TaffyStyle(), text("XX\u200BXX\u200BXX\u200BXX\u200BXX\u200BXX\u200BXX"));
        place(tree, five, 1, 1, 1, 1);
        place(tree, three, 3, 1, 1, 1);
        place(tree, span, 1, 3, 1, 1);
        NodeId root = tree.newWithChildren(grid, five, three, span);

        tree.computeLayout(root, TaffySize.maxContent());

        // The spanning item's 140px max-content contribution raises the two max-content columns to
        // 70px and 50px; the minmax(max-content, 20px) column stays at its 20px maximum.
        assertColumnSizes(tree, root, 70f, 20f, 50f);
        assertSize(tree.getLayout(root), 300f, 10f, "container");
        assertRect(tree.getLayout(five), 0f, 0f, 70f, 10f, "first max-content item");
        assertRect(tree.getLayout(three), 90f, 0f, 50f, 10f, "third max-content item");
        assertRect(tree.getLayout(span), 0f, 0f, 140f, 10f, "spanning item");
    }

    // ------------------------------------------------------------------
    // GR-02 T14: growth limit space only for infinitely growable tracks, fit-content() capped
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GR-02 T14: grid_fit_content_span_2_capped_by_non_growable_fit_content_track")
    void fitContentSpanTwoCappedByNonGrowableTrack() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle grid = grid();
        grid.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.auto());
        grid.justifyContent = AlignContent.START;
        grid.gap = new TaffySize<>(LengthPercentage.length(5f), LengthPercentage.ZERO);
        grid.gridTemplateColumns = List.of(
            TrackSizingFunction.fitContent(LengthPercentage.length(110f)),
            TrackSizingFunction.fitContent(LengthPercentage.length(40f)));
        NodeId small = tree.newLeafWithMeasure(new TaffyStyle(), text("XX"));
        NodeId span = tree.newLeafWithMeasure(new TaffyStyle(), text("XXX\u200BXXX\u200BX"));
        place(tree, small, 2, 1, 1, 1);
        place(tree, span, 1, 2, 2, 1);
        NodeId root = tree.newWithChildren(grid, small, span);

        tree.computeLayout(root, TaffySize.maxContent());

        // Only the first fit-content() track is still infinitely growable, so the spanning item's
        // 70px max-content contribution raises its growth limit to 45px; the second track keeps the
        // 20px its non-spanning item gave it.
        assertColumnSizes(tree, root, 45f, 20f);
        assertSize(tree.getLayout(root), 100f, 20f, "container");
        assertRect(tree.getLayout(small), 50f, 0f, 20f, 10f, "non-spanning item");
        assertRect(tree.getLayout(span), 0f, 10f, 70f, 10f, "spanning item");
    }

    @Test
    @DisplayName("GR-02 T14: grid_fit_content_span_3_between_auto_tracks")
    void fitContentSpanThreeBetweenAutoTracks() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle grid = grid();
        grid.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.auto());
        grid.justifyContent = AlignContent.START;
        grid.gap = new TaffySize<>(LengthPercentage.length(5f), LengthPercentage.ZERO);
        grid.gridTemplateColumns = List.of(
            TrackSizingFunction.auto(),
            TrackSizingFunction.fitContent(LengthPercentage.length(110f)),
            TrackSizingFunction.auto());
        NodeId wide = tree.newLeafWithMeasure(new TaffyStyle(), text("XX\u200BXX\u200BXX\u200BXX\u200BXX\u200BXX"));
        NodeId narrow = tree.newLeafWithMeasure(new TaffyStyle(), text("XXX\u200BXXX\u200BX"));
        place(tree, wide, 1, 3, 1, 1);
        place(tree, narrow, 1, 3, 2, 1);
        NodeId root = tree.newWithChildren(grid, wide, narrow);

        tree.computeLayout(root, TaffySize.maxContent());

        // The 100px max-content contribution is shared by the three tracks up to the fit-content()
        // argument (110px does not bind), giving 30px each.
        assertColumnSizesAtMost(tree, root, 30f, 30f, 30f);
        assertSize(tree.getLayout(root), 100f, 30f, "container");
        assertRect(tree.getLayout(wide), 0f, 0f, 100f, 20f, "first spanning item");
        assertRect(tree.getLayout(narrow), 0f, 20f, 100f, 10f, "second spanning item");
    }

    // ------------------------------------------------------------------
    // GR-14: all-collapsed axis keeps one aligned grid line (row axis of the upstream fixture)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GR-14: grid_absolute_collapsed_auto_fit_tracks_rtl (row and column axes)")
    void collapsedAutoFitAxisAlignsAbsoluteGridLines() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle grid = grid();
        grid.size = new TaffySize<>(TaffyDimension.length(530f), TaffyDimension.length(330f));
        grid.padding = TaffyRect.all(LengthPercentage.length(15f));
        grid.gap = new TaffySize<>(LengthPercentage.length(10f), LengthPercentage.length(10f));
        grid.direction = TaffyDirection.RTL;
        grid.gridTemplateColumnsWithRepeat = List.of(
            GridTemplateComponent.autoFit(TrackSizingFunction.fixed(100f)));
        grid.gridTemplateRowsWithRepeat = List.of(
            GridTemplateComponent.autoFit(TrackSizingFunction.fixed(100f)));

        TaffyStyle startLineEnd = new TaffyStyle();
        startLineEnd.position = TaffyPosition.ABSOLUTE;
        startLineEnd.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.line(1));
        startLineEnd.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.line(1));

        TaffyStyle endLineStart = new TaffyStyle();
        endLineStart.position = TaffyPosition.ABSOLUTE;
        endLineStart.gridColumn = new TaffyLine<>(GridPlacement.line(-1), GridPlacement.auto());
        endLineStart.gridRow = new TaffyLine<>(GridPlacement.line(-1), GridPlacement.auto());

        NodeId first = tree.newLeaf(startLineEnd);
        NodeId second = tree.newLeaf(endLineStart);
        NodeId root = tree.newWithChildren(grid, first, second);

        tree.computeLayout(root, TaffySize.maxContent());

        // Every auto-fit track is collapsed: the rows resolve to "0px 0px" and the columns to four
        // 0px tracks, and the absolutely positioned children still resolve their lines inside the
        // content box (RTL puts the inline-start edge on the right).
        assertSize(tree.getLayout(root), 530f, 330f, "container");
        assertCollapsedTracks(tree, root);
        assertRect(tree.getLayout(first), 530f, 0f, 0f, 0f, "start-line end child");
        assertRect(tree.getLayout(second), 515f, 15f, 0f, 0f, "end-line start child");
    }

    // ----------------- helpers -----------------

    private static TaffyStyle grid() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        return style;
    }

    private static void place(TaffyTree tree, NodeId node, int columnStart, int columnSpan,
                              int rowStart, int rowSpan) {
        TaffyStyle style = tree.getStyle(node);
        style.gridColumn = new TaffyLine<>(GridPlacement.line(columnStart),
            columnSpan == 1 ? GridPlacement.auto() : GridPlacement.span(columnSpan));
        style.gridRow = new TaffyLine<>(GridPlacement.line(rowStart),
            rowSpan == 1 ? GridPlacement.auto() : GridPlacement.span(rowSpan));
    }

    /** An Ahem-style text measure: 10x10 per glyph, zero-width spaces are wrapping opportunities. */
    private static MeasureFunc text(String value) {
        return (knownDimensions, availableSpace) -> measure(value, knownDimensions, availableSpace);
    }

    private static FloatSize measure(String value, FloatSize knownDimensions,
                                     TaffySize<AvailableSpace> availableSpace) {
        String[] parts = value.split("\u200B", -1);
        int minLength = 0;
        int maxLength = 0;
        for (String part : parts) {
            minLength = Math.max(minLength, part.length());
            maxLength += part.length();
        }

        float inlineSize;
        if (!Float.isNaN(knownDimensions.width)) {
            inlineSize = knownDimensions.width;
        } else if (availableSpace.width.isMinContent()) {
            inlineSize = minLength * GLYPH;
        } else if (availableSpace.width.isMaxContent()) {
            inlineSize = maxLength * GLYPH;
        } else if (availableSpace.width.isDefinite()) {
            inlineSize = Math.min(availableSpace.width.getValue(), maxLength * GLYPH);
        } else {
            inlineSize = maxLength * GLYPH;
        }
        inlineSize = Math.max(inlineSize, minLength * GLYPH);

        float blockSize;
        if (!Float.isNaN(knownDimensions.height)) {
            blockSize = knownDimensions.height;
        } else {
            int perLine = (int) Math.floor(inlineSize / GLYPH);
            int lines = 1;
            int currentLine = 0;
            for (String part : parts) {
                if (currentLine + part.length() > perLine) {
                    if (currentLine > 0) {
                        lines++;
                    }
                    currentLine = part.length();
                } else {
                    currentLine += part.length();
                }
            }
            blockSize = lines * GLYPH;
        }
        return new FloatSize(inlineSize, blockSize);
    }

    private static void assertColumnSizes(TaffyTree tree, NodeId root, float... expected) {
        List<GridTrackInfo> tracks = tree.getDetailedGridInfo(root).columns().tracks();
        assertEquals(expected.length, tracks.size(), "column count");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], tracks.get(i).size(), EPSILON, "column " + i + " size");
        }
    }

    /** The {@code grid_fit_content_span_3_between_auto_tracks} expectations carry Chrome's rounding. */
    private static void assertColumnSizesAtMost(TaffyTree tree, NodeId root, float... expected) {
        List<GridTrackInfo> tracks = tree.getDetailedGridInfo(root).columns().tracks();
        assertEquals(expected.length, tracks.size(), "column count");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], tracks.get(i).size(), 0.5f, "column " + i + " size");
        }
    }

    private static void assertCollapsedTracks(TaffyTree tree, NodeId root) {
        DetailedGridInfo info = tree.getDetailedGridInfo(root);
        assertEquals(2, info.rows().tracks().size(), "row count");
        for (GridTrackInfo row : info.rows().tracks()) {
            assertEquals(0f, row.size(), EPSILON, "collapsed row size");
        }
        assertEquals(4, info.columns().tracks().size(), "column count");
        for (GridTrackInfo column : info.columns().tracks()) {
            assertEquals(0f, column.size(), EPSILON, "collapsed column size");
        }
    }

    private static void assertSize(Layout layout, float width, float height, String message) {
        assertEquals(width, layout.size().width, EPSILON, message + " width");
        assertEquals(height, layout.size().height, EPSILON, message + " height");
    }

    private static void assertRect(Layout layout, float x, float y, float width, float height, String message) {
        FloatPoint location = layout.location();
        FloatSize size = layout.size();
        assertEquals(x, location.x, EPSILON, message + " x");
        assertEquals(y, location.y, EPSILON, message + " y");
        assertEquals(width, size.width, EPSILON, message + " width");
        assertEquals(height, size.height, EPSILON, message + " height");
    }
}
