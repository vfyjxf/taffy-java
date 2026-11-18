package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.tree.Baselines;
import dev.vfyjxf.taffy.tree.CollapsibleMarginSet;
import dev.vfyjxf.taffy.tree.LayoutInput;
import dev.vfyjxf.taffy.tree.LayoutOutput;
import dev.vfyjxf.taffy.tree.RequestedAxis;
import dev.vfyjxf.taffy.tree.RunMode;
import dev.vfyjxf.taffy.tree.ScrollableOverflow;
import dev.vfyjxf.taffy.tree.SizingMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the vertical-writing-mode transpose helpers added by CR-06 (upstream taffy #1265):
 * {@link TaffySize#transpose()}, {@link TaffyRect#transpose()},
 * {@link LayoutInput#transpose()} and {@link LayoutOutput#transpose()}.
 *
 * <p>The margins/collapse flag of {@code LayoutOutput} and the
 * {@code verticalMarginsAreCollapsible} flag of {@code LayoutInput} follow the upstream reset
 * semantics and therefore do not survive a transposition; all geometry does.</p>
 */
public class TransposeTest {

    // ==================== TaffySize ====================

    @Test
    @DisplayName("size_transpose_swaps_width_and_height")
    void sizeTransposeSwapsWidthAndHeight() {
        TaffySize<Float> size = TaffySize.of(10f, 20f);

        TaffySize<Float> transposed = size.transpose();

        assertEquals(20f, transposed.width);
        assertEquals(10f, transposed.height);
        // Receiver untouched.
        assertEquals(10f, size.width);
        assertEquals(20f, size.height);
    }

    @Test
    @DisplayName("size_transpose_twice_restores_original")
    void sizeTransposeTwiceRestoresOriginal() {
        TaffySize<Float> size = TaffySize.of(10f, 20f);

        assertEquals(size, size.transpose().transpose());
    }

    @Test
    @DisplayName("size_transpose_preserves_none_and_nan")
    void sizeTransposePreservesNoneAndNaN() {
        TaffySize<Float> none = TaffySize.none();
        TaffySize<Float> transposedNone = none.transpose();
        assertNull(transposedNone.width);
        assertNull(transposedNone.height);
        assertTrue(transposedNone.isNone());

        TaffySize<Float> partial = TaffySize.of(5f, null);
        assertNull(partial.transpose().width);
        assertEquals(5f, partial.transpose().height);

        TaffySize<Float> nan = TaffySize.of(Float.NaN, 7f);
        assertEquals(7f, nan.transpose().width);
        assertTrue(Float.isNaN(nan.transpose().height));
    }

    // ==================== TaffyRect ====================

    @Test
    @DisplayName("rect_transpose_swaps_horizontal_and_vertical_sides")
    void rectTransposeSwapsHorizontalAndVerticalSides() {
        TaffyRect<Float> rect = new TaffyRect<>(1f, 2f, 3f, 4f);

        TaffyRect<Float> transposed = rect.transpose();

        assertEquals(3f, transposed.left);
        assertEquals(4f, transposed.right);
        assertEquals(1f, transposed.top);
        assertEquals(2f, transposed.bottom);
        // Receiver untouched.
        assertEquals(1f, rect.left);
        assertEquals(2f, rect.right);
        assertEquals(3f, rect.top);
        assertEquals(4f, rect.bottom);
    }

    @Test
    @DisplayName("rect_transpose_twice_restores_original")
    void rectTransposeTwiceRestoresOriginal() {
        TaffyRect<Float> rect = new TaffyRect<>(1f, 2f, 3f, 4f);

        assertEquals(rect, rect.transpose().transpose());
    }

    @Test
    @DisplayName("rect_transpose_preserves_nan")
    void rectTransposePreservesNaN() {
        TaffyRect<Float> rect = new TaffyRect<>(Float.NaN, 2f, 3f, Float.NaN);

        TaffyRect<Float> transposed = rect.transpose();

        assertEquals(3f, transposed.left);
        assertTrue(Float.isNaN(transposed.right));
        assertTrue(Float.isNaN(transposed.top));
        assertEquals(2f, transposed.bottom);
    }

    // ==================== LayoutInput ====================

    @Test
    @DisplayName("layout_input_transpose_swaps_per_axis_fields")
    void layoutInputTransposeSwapsPerAxisFields() {
        LayoutInput input = layoutInput(RequestedAxis.HORIZONTAL, true);

        LayoutInput transposed = input.transpose();

        assertEquals(RunMode.PERFORM_LAYOUT, transposed.runMode());
        assertEquals(SizingMode.INHERENT_SIZE, transposed.sizingMode());
        assertEquals(RequestedAxis.VERTICAL, transposed.axis());

        assertEquals(20f, transposed.knownDimensions().width);
        assertEquals(10f, transposed.knownDimensions().height);
        assertEquals(40f, transposed.parentSize().width);
        assertEquals(30f, transposed.parentSize().height);
        assertEquals(AvailableSpace.definite(200f), transposed.availableSpace().width);
        assertEquals(AvailableSpace.definite(100f), transposed.availableSpace().height);

        // Receiver untouched.
        assertEquals(RequestedAxis.HORIZONTAL, input.axis());
        assertEquals(10f, input.knownDimensions().width);
        assertEquals(20f, input.knownDimensions().height);
    }

    @Test
    @DisplayName("layout_input_transpose_transposes_each_axis_value")
    void layoutInputTransposeTransposesEachAxisValue() {
        assertEquals(RequestedAxis.VERTICAL, layoutInput(RequestedAxis.HORIZONTAL, false).transpose().axis());
        assertEquals(RequestedAxis.HORIZONTAL, layoutInput(RequestedAxis.VERTICAL, false).transpose().axis());
        assertEquals(RequestedAxis.BOTH, layoutInput(RequestedAxis.BOTH, false).transpose().axis());
        assertNull(layoutInput(null, false).transpose().axis());
    }

    @Test
    @DisplayName("layout_input_transpose_resets_vertical_margins_are_collapsible")
    void layoutInputTransposeResetsVerticalMarginsAreCollapsible() {
        LayoutInput input = layoutInput(RequestedAxis.BOTH, true);

        LayoutInput transposed = input.transpose();

        assertEquals(TaffyLine.FALSE, transposed.verticalMarginsAreCollapsible());
        // The flag is block-axis state and is deliberately not recovered by a second transpose.
        assertEquals(TaffyLine.FALSE, transposed.transpose().verticalMarginsAreCollapsible());
        // Receiver untouched.
        assertEquals(TaffyLine.TRUE, input.verticalMarginsAreCollapsible());
    }

    @Test
    @DisplayName("layout_input_transpose_twice_restores_geometry_and_axis")
    void layoutInputTransposeTwiceRestoresGeometryAndAxis() {
        LayoutInput input = layoutInput(RequestedAxis.VERTICAL, false);

        assertEquals(input, input.transpose().transpose());
    }

    @Test
    @DisplayName("layout_input_transpose_preserves_nan_dimensions")
    void layoutInputTransposePreservesNaNDimensions() {
        LayoutInput input = new LayoutInput(
            RunMode.COMPUTE_SIZE,
            SizingMode.CONTENT_SIZE,
            RequestedAxis.BOTH,
            FloatSize.none(),
            FloatSize.none(),
            new TaffySize<>(AvailableSpace.MIN_CONTENT, AvailableSpace.MAX_CONTENT),
            TaffyLine.FALSE
        );

        LayoutInput transposed = input.transpose();

        assertTrue(Float.isNaN(transposed.knownDimensions().width));
        assertTrue(Float.isNaN(transposed.knownDimensions().height));
        assertTrue(Float.isNaN(transposed.parentSize().width));
        assertTrue(Float.isNaN(transposed.parentSize().height));
        assertEquals(AvailableSpace.MAX_CONTENT, transposed.availableSpace().width);
        assertEquals(AvailableSpace.MIN_CONTENT, transposed.availableSpace().height);
    }

    // ==================== LayoutOutput ====================

    @Test
    @DisplayName("layout_output_transpose_swaps_geometry_and_baselines")
    void layoutOutputTransposeSwapsGeometryAndBaselines() {
        LayoutOutput output = sampleOutput();

        LayoutOutput transposed = output.transpose();

        assertEquals(new FloatSize(20f, 10f), transposed.size());
        assertEquals(new FloatSize(40f, 30f), transposed.contentSize());
        assertEquals(new FloatRect(-3f, 4f, -1f, 2f), transposed.scrollableOverflow().rect());
        assertTrue(transposed.scrollableOverflow().reversedX());
        assertFalse(transposed.scrollableOverflow().reversedY());
        assertEquals(new FloatPoint(12f, 11f), transposed.firstBaselines());
        assertEquals(new FloatPoint(14f, 13f), transposed.baselines().first());
        assertEquals(new FloatPoint(16f, 15f), transposed.baselines().last());

        // Receiver untouched.
        assertEquals(new FloatSize(10f, 20f), output.size());
        assertEquals(new FloatSize(30f, 40f), output.contentSize());
        assertEquals(new FloatPoint(11f, 12f), output.firstBaselines());
    }

    @Test
    @DisplayName("layout_output_transpose_resets_collapsible_margins")
    void layoutOutputTransposeResetsCollapsibleMargins() {
        LayoutOutput output = sampleOutput();

        LayoutOutput transposed = output.transpose();

        assertSame(CollapsibleMarginSet.ZERO, transposed.topMargin());
        assertSame(CollapsibleMarginSet.ZERO, transposed.bottomMargin());
        assertEquals(0f, transposed.topMargin().resolve());
        assertEquals(0f, transposed.bottomMargin().resolve());
        assertFalse(transposed.marginsCanCollapseThrough());

        // Receiver untouched.
        assertEquals(5f, output.topMargin().resolve());
        assertEquals(-6f, output.bottomMargin().resolve());
        assertTrue(output.marginsCanCollapseThrough());
    }

    @Test
    @DisplayName("layout_output_transpose_twice_restores_geometry_but_not_margins")
    void layoutOutputTransposeTwiceRestoresGeometryButNotMargins() {
        LayoutOutput output = sampleOutput();

        LayoutOutput twice = output.transpose().transpose();

        assertEquals(output.size(), twice.size());
        assertEquals(output.contentSize(), twice.contentSize());
        assertEquals(output.scrollableOverflow().rect(), twice.scrollableOverflow().rect());
        assertEquals(output.scrollableOverflow().reversedX(), twice.scrollableOverflow().reversedX());
        assertEquals(output.scrollableOverflow().reversedY(), twice.scrollableOverflow().reversedY());
        assertEquals(output.firstBaselines(), twice.firstBaselines());
        assertEquals(output.baselines(), twice.baselines());

        // Reset semantics are not reversible (upstream taffy #1265).
        assertSame(CollapsibleMarginSet.ZERO, twice.topMargin());
        assertSame(CollapsibleMarginSet.ZERO, twice.bottomMargin());
        assertFalse(twice.marginsCanCollapseThrough());
    }

    @Test
    @DisplayName("layout_output_transpose_preserves_absent_baselines")
    void layoutOutputTransposePreservesAbsentBaselines() {
        LayoutOutput transposed = LayoutOutput.HIDDEN.transpose();

        assertTrue(Float.isNaN(transposed.firstBaselines().x));
        assertTrue(Float.isNaN(transposed.firstBaselines().y));
        assertFalse(transposed.baselines().hasFirst());
        assertFalse(transposed.baselines().hasLast());
        assertEquals(FloatSize.zero(), transposed.size());
        assertEquals(FloatSize.zero(), transposed.contentSize());
    }

    // ==================== Helpers ====================

    private static LayoutInput layoutInput(RequestedAxis axis, boolean verticalMarginsAreCollapsible) {
        return new LayoutInput(
            RunMode.PERFORM_LAYOUT,
            SizingMode.INHERENT_SIZE,
            axis,
            new FloatSize(10f, 20f),
            new FloatSize(30f, 40f),
            new TaffySize<>(AvailableSpace.definite(100f), AvailableSpace.definite(200f)),
            verticalMarginsAreCollapsible ? TaffyLine.TRUE : TaffyLine.FALSE
        );
    }

    private static LayoutOutput sampleOutput() {
        return new LayoutOutput(
            new FloatSize(10f, 20f),
            new FloatSize(30f, 40f),
            new ScrollableOverflow(new FloatRect(-1f, 2f, -3f, 4f), false, true),
            new FloatPoint(11f, 12f),
            new Baselines(new FloatPoint(13f, 14f), new FloatPoint(15f, 16f)),
            CollapsibleMarginSet.fromMargin(5f),
            CollapsibleMarginSet.fromMargin(-6f),
            true
        );
    }
}
