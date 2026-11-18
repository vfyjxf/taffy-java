package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.LayoutOutput;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
