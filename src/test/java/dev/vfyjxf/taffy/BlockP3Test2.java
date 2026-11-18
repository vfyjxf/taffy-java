package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.LayoutComputer;
import dev.vfyjxf.taffy.tree.LayoutOutput;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.SizingMode;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P3 block follow-up tests: the replaced-child min-content contribution cap (BK-12 / upstream
 * #1401) and the leaf measure-baseline path (LEAF-BASELINE / upstream leaf.rs gating).
 */
public class BlockP3Test2 {

    private static final float EPS = 0.01f;

    /** Intrinsic content size reported by the replaced/form-control measure callbacks. */
    private static final float INTRINSIC = 8f;

    private static TaffyStyle block() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        return style;
    }

    /** A form control: compressible replaced, so it is not stretch-sized in block flow. */
    private static TaffyStyle formControl() {
        TaffyStyle style = block();
        style.itemIsCompressibleReplaced = true;
        return style;
    }

    private static TaffyStyle paddedFormControl() {
        TaffyStyle style = formControl();
        style.padding = new TaffyRect<>(
            LengthPercentage.length(1f),
            LengthPercentage.length(1f),
            LengthPercentage.ZERO,
            LengthPercentage.ZERO
        );
        return style;
    }

    private static NodeId measuredLeaf(TaffyTree tree, TaffyStyle style) {
        return tree.newLeafWithMeasureFunction(style,
            context -> LayoutOutput.fromOuterSize(new FloatSize(INTRINSIC, INTRINSIC)));
    }

    // ==================================================================
    // BK-12 / upstream #1401: percentage width and max-width have a zero
    // min-content contribution for compressible replaced children
    // ==================================================================

    @Test
    @DisplayName("BK-12: a form control's percentage max-width caps its min-content contribution at zero")
    void percentageMaxWidthHasZeroMinContentContribution() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle imageStyle = paddedFormControl();
        imageStyle.maxSize = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
        NodeId image = measuredLeaf(tree, imageStyle);
        NodeId root = tree.newWithChildren(block(), image);

        tree.computeLayout(root, TaffySize.minContent());

        // Upstream block_replaced::percentage_width_min_content_size_of_container_is_zero:
        // the container is only as wide as the item's 1px + 1px padding.
        assertEquals(2f, tree.getLayout(root).size().width, EPS,
            "the cap makes the percentage max-width contribute zero to the min-content width");
        assertEquals(2f, tree.getLayout(image).size().width, EPS,
            "the form control keeps its padding-box minimum");
    }

    @Test
    @DisplayName("BK-12: a form control's percentage width has a zero min-content contribution")
    void percentageWidthHasZeroMinContentContribution() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle imageStyle = formControl();
        imageStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
        NodeId image = measuredLeaf(tree, imageStyle);
        NodeId root = tree.newWithChildren(block(), image);

        tree.computeLayout(root, TaffySize.minContent());

        assertEquals(0f, tree.getLayout(root).size().width, EPS,
            "a percentage width resolved against zero is the min-content contribution");
    }

    @Test
    @DisplayName("BK-12: max-content still uses the intrinsic size for a percentage max-width")
    void percentageMaxWidthMaxContentContributionIsIntrinsic() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle imageStyle = formControl();
        imageStyle.maxSize = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
        NodeId image = measuredLeaf(tree, imageStyle);
        NodeId root = tree.newWithChildren(block(), image);

        tree.computeLayout(root, TaffySize.maxContent());

        assertEquals(INTRINSIC, tree.getLayout(root).size().width, EPS,
            "the cap only applies to the min-content/fit-content contribution, not to max-content");
    }

    @Test
    @DisplayName("BK-12: a non-replaced child's percentage max-width is not capped")
    void nonReplacedPercentageMaxWidthIsNotCapped() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle plainStyle = block();
        plainStyle.maxSize = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
        NodeId plain = measuredLeaf(tree, plainStyle);
        NodeId root = tree.newWithChildren(block(), plain);

        tree.computeLayout(root, TaffySize.minContent());

        assertEquals(INTRINSIC, tree.getLayout(root).size().width, EPS,
            "only compressible replaced children get the min-content width cap");
    }

    // ==================================================================
    // LEAF-BASELINE: PerformLayout must measure fixed-size leaves so a
    // MeasureFunction can report its baseline (upstream leaf.rs gating)
    // ==================================================================

    private static final float LEAF_SIZE = 20f;
    private static final float MEASURE_BASELINE = 12f;

    private static TaffyStyle fixedLeafStyle() {
        TaffyStyle style = block();
        style.size = new TaffySize<>(
            TaffyDimension.length(LEAF_SIZE),
            TaffyDimension.length(LEAF_SIZE)
        );
        return style;
    }

    private static LayoutOutput measuredBaselineLeaf(float baseline) {
        return LayoutOutput.fromSizesAndBaselines(
            new FloatSize(LEAF_SIZE, LEAF_SIZE),
            FloatSize.zero(),
            new FloatPoint(Float.NaN, baseline)
        );
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

    @Test
    @DisplayName("LEAF-BASELINE: a fixed-size leaf's MeasureFunction baseline reaches the parent output")
    void fixedSizeLeafReportsMeasureBaselineInPerformLayout() {
        TaffyTree tree = new TaffyTree();
        NodeId leaf = tree.newLeafWithMeasureFunction(fixedLeafStyle(),
            context -> measuredBaselineLeaf(MEASURE_BASELINE));
        NodeId root = tree.newWithChildren(block(), leaf);

        LayoutOutput output = layoutOutput(tree, root);

        assertBaseline(MEASURE_BASELINE, output,
            "PerformLayout must measure the fixed-size leaf instead of short-circuiting");
        assertEquals(LEAF_SIZE, tree.getUnroundedLayout(leaf).size().width, EPS,
            "the known 20px width is still the final size");
        assertEquals(LEAF_SIZE, tree.getUnroundedLayout(leaf).size().height, EPS,
            "the known 20px height is still the final size");
    }

    @Test
    @DisplayName("LEAF-BASELINE: a fixed-size leaf without a measure callback keeps reporting no baseline")
    void fixedSizeLeafWithoutMeasureKeepsNoBaseline() {
        TaffyTree tree = new TaffyTree();
        NodeId leaf = tree.newLeaf(fixedLeafStyle());
        NodeId root = tree.newWithChildren(block(), leaf);

        LayoutOutput output = layoutOutput(tree, root);

        assertNoBaseline(output, "a plain block leaf has no measure callback and therefore no baseline");
        assertEquals(LEAF_SIZE, tree.getUnroundedLayout(leaf).size().width, EPS);
        assertEquals(LEAF_SIZE, tree.getUnroundedLayout(leaf).size().height, EPS);
    }

    @Test
    @DisplayName("LEAF-BASELINE: ComputeSize skips the measure callback for a known-size leaf")
    void computeSizeSkipsMeasureForKnownSizeLeaf() {
        TaffyTree tree = new TaffyTree();
        AtomicInteger measureCalls = new AtomicInteger();
        NodeId leaf = tree.newLeafWithMeasureFunction(fixedLeafStyle(), context -> {
            measureCalls.incrementAndGet();
            return measuredBaselineLeaf(MEASURE_BASELINE);
        });
        LayoutComputer computer = new LayoutComputer(tree, (MeasureFunc) null);

        FloatSize size = computer.measureChildSize(
            leaf,
            FloatSize.none(),
            FloatSize.none(),
            TaffySize.maxContent(),
            SizingMode.INHERENT_SIZE,
            TaffyLine.FALSE
        );

        assertEquals(0, measureCalls.get(),
            "ComputeSize with both dimensions known and collapse-preventing styles returns early");
        assertEquals(LEAF_SIZE, size.width, EPS);
        assertEquals(LEAF_SIZE, size.height, EPS);
    }
}
