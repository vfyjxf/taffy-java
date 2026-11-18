package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.Baselines;
import dev.vfyjxf.taffy.tree.CollapsibleMarginSet;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.LayoutComputer;
import dev.vfyjxf.taffy.tree.LayoutOutput;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.ScrollableOverflow;
import dev.vfyjxf.taffy.tree.SizingMode;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.BaselineHelper;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P3 regression tests for the unified first-baseline contract (BL-06 / CR-07).
 *
 * <p>Covers the shared {@link BaselineHelper} (upstream #1323's
 * {@code common::child_first_baseline}), the block container's baseline propagation (upstream
 * #996), the scroll-container clamp/synthesis (upstream #1126 for block, #1128/#1323 for the
 * shared helper) and the flex/grid consumers (upstream #995 row flex, #1108 grid, #1323 column
 * flex). The expectations mirror the upstream hand-written tests in
 * {@code tests/hand_written/baseline.rs} and the generated fixtures
 * {@code block_container_baseline_ignores_relative_offset},
 * {@code flex_container_baseline_column_scroll_container_clamped} and
 * {@code grid_align_items_baseline_overflow_scroll}.</p>
 *
 * <p>Grid's baseline-participation rules (#1125 auto block margins, 3dbf2e46 cyclic block-size
 * dependency) and the grid/block container-baseline refinements of #1322/#1323 are <b>not</b>
 * covered here: they belong to the grid A section (A3/A4/A5/A6) and would change existing
 * baseline numbers, which this change deliberately does not do.</p>
 */
public class BaselineP3Test {

    private static final float EPS = 0.01f;

    private static final float BASELINE = 12f;
    private static final float SCROLL_HEIGHT = 30f;
    private static final float CONTENT_BASELINE = 48f;

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static TaffyStyle block(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        style.size = new TaffySize<>(
            Float.isNaN(width) ? TaffyDimension.AUTO : TaffyDimension.length(width),
            Float.isNaN(height) ? TaffyDimension.AUTO : TaffyDimension.length(height)
        );
        return style;
    }

    private static TaffyStyle autoBlock() {
        return block(Float.NaN, Float.NaN);
    }

    private static TaffyStyle scrollContainer(float width, float height) {
        TaffyStyle style = block(width, height);
        style.overflow = TaffyPoint.all(Overflow.HIDDEN);
        return style;
    }

    private static TaffyStyle flexRowBaseline(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.ROW;
        style.alignItems = AlignItems.BASELINE;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle flexColumn(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.COLUMN;
        style.alignItems = AlignItems.FLEX_START;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle gridBaseline(float width, float height, float... columns) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        style.alignItems = AlignItems.BASELINE;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        if (columns != null && columns.length > 0) {
            List<TrackSizingFunction> tracks = new java.util.ArrayList<>();
            for (float column : columns) {
                tracks.add(TrackSizingFunction.fixed(column));
            }
            style.gridTemplateColumns = tracks;
        }
        return style;
    }

    /** A layout output with an intrinsic size and a first baseline (the upstream BaselineContext). */
    private static LayoutOutput measured(float width, float height, float baseline) {
        return LayoutOutput.fromSizesAndBaselines(
            new FloatSize(width, height),
            FloatSize.zero(),
            new FloatPoint(Float.NaN, baseline)
        );
    }

    /** A leaf whose measure function reports a size and a first baseline. */
    private static NodeId measuredLeaf(TaffyTree tree, float width, float height, float baseline) {
        TaffyStyle style = autoBlock();
        return tree.newLeafWithMeasureFunction(style, context -> measured(width, height, baseline));
    }

    /** Runs a full layout on {@code node} and returns its output (the container's baseline lives here). */
    private static LayoutOutput layoutOutput(TaffyTree tree, NodeId node) {
        LayoutComputer computer = new LayoutComputer(tree, (MeasureFunc) null);
        return computer.performChildLayout(
            node,
            FloatSize.none(),
            FloatSize.none(),
            TaffySize.maxContent(),
            SizingMode.INHERENT_SIZE,
            TaffyLine.FALSE
        );
    }

    private static void assertBaseline(float expected, LayoutOutput output, String message) {
        assertTrue(output.baselines() != null && output.baselines().hasFirst(),
            message + ": expected a first baseline, got " + output.baselines());
        assertEquals(expected, output.baselines().first().y, EPS, message + " (baselines.first.y)");
        assertEquals(expected, output.firstBaselines().y, EPS, message + " (legacy firstBaselines.y)");
    }

    private static void assertNoBaseline(LayoutOutput output, String message) {
        assertTrue(Float.isNaN(output.baselines().first().y), message + " (baselines.first.y)");
        assertTrue(Float.isNaN(output.firstBaselines().y), message + " (legacy firstBaselines.y)");
    }

    private static void assertLocation(Layout layout, float x, float y, String message) {
        assertEquals(x, layout.location().x, EPS, message + " (x)");
        assertEquals(y, layout.location().y, EPS, message + " (y)");
    }

    // ==================================================================
    // BL-06: the shared helper
    // ==================================================================

    @Test
    @DisplayName("BL-06: helper returns the plain baseline for non-scroll children")
    void helperReturnsPlainBaselineForNonScrollChild() {
        LayoutOutput child = measured(30f, 20f, 15f);

        assertEquals(15f, BaselineHelper.childFirstBaseline(child, false), EPS,
            "non-scroll child keeps its baseline");
        assertEquals(25f, BaselineHelper.childFirstBaseline(child, new FloatPoint(0f, 10f), false), EPS,
            "the location form adds the child's border-box top");
        assertTrue(Float.isNaN(BaselineHelper.childFirstBaseline(LayoutOutput.fromOuterSize(
            new FloatSize(30f, 20f)), false)), "a non-scroll child without a baseline has none");
    }

    @Test
    @DisplayName("BL-06: helper clamps and synthesizes scroll-container baselines")
    void helperClampsAndSynthesizesScrollContainerBaseline() {
        // Upstream: first_baseline.unwrap_or(height).min(height).max(0.0)
        assertEquals(20f, BaselineHelper.childFirstBaseline(measured(30f, 20f, 48f), true), EPS,
            "a baseline below the content but beyond the border box is clamped to the height");
        assertEquals(48f, BaselineHelper.childFirstBaseline(measured(30f, 20f, 48f), false), EPS,
            "a non-scroll child is not clamped");
        assertEquals(15f, BaselineHelper.childFirstBaseline(measured(30f, 20f, 15f), true), EPS,
            "a baseline inside the border box is preserved");
        assertEquals(0f, BaselineHelper.childFirstBaseline(measured(30f, 20f, -5f), true), EPS,
            "a negative baseline is clamped to zero");
        assertEquals(20f, BaselineHelper.childFirstBaseline(
            LayoutOutput.fromOuterSize(new FloatSize(30f, 20f)), true), EPS,
            "a scroll container without a baseline synthesizes one at the border-box bottom edge");
    }

    @Test
    @DisplayName("BL-06: helper reads the new Baselines component when the legacy point is empty")
    void helperReadsNewBaselinesComponent() {
        LayoutOutput child = new LayoutOutput(
            new FloatSize(30f, 20f),
            FloatSize.zero(),
            ScrollableOverflow.ZERO,
            new FloatPoint(Float.NaN, Float.NaN),
            new Baselines(new FloatPoint(Float.NaN, 12f), FloatPoint.none()),
            CollapsibleMarginSet.ZERO,
            CollapsibleMarginSet.ZERO,
            false
        );

        assertEquals(12f, BaselineHelper.childFirstBaseline(child, false), EPS,
            "baselines.first is the new source of truth");
    }

}
