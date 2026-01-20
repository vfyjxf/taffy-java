package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Compatibility tests for Style flags that exist in Rust taffy:
 * - itemIsTable: block layout should NOT stretch-fit table children.
 * - itemIsReplaced: grid minimum contribution for compressible replaced elements should be capped by definite preferred/max sizes.
 */
public class StyleItemFlagsCompatTest {

    private static final float EPSILON = 0.1f;

    private static MeasureFunc fixedMeasure(float width, float height) {
        return (knownDimensions, availableSpace) -> {
            float w = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : width;
            float h = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : height;
            return new FloatSize(w, h);
        };
    }

    /**
     * Returns an intrinsic size unless the parent provides a definite available space in that axis,
     * in which case it clamps to that available space.
     */
    private static MeasureFunc intrinsicWithAvailableClamp(float intrinsicWidth, float intrinsicHeight) {
        return (knownDimensions, availableSpace) -> {
            float w;
            if (!Float.isNaN(knownDimensions.width)) {
                w = knownDimensions.width;
            } else if (availableSpace.width.isDefinite()) {
                w = Math.min(intrinsicWidth, availableSpace.width.getValue());
            } else {
                w = intrinsicWidth;
            }

            float h;
            if (!Float.isNaN(knownDimensions.height)) {
                h = knownDimensions.height;
            } else if (availableSpace.height.isDefinite()) {
                h = Math.min(intrinsicHeight, availableSpace.height.getValue());
            } else {
                h = intrinsicHeight;
            }
            return new FloatSize(w, h);
        };
    }

    @Test
    @DisplayName("block_table_child_does_not_get_stretch_fit_width")
    void blockTableChildDoesNotGetStretchFitWidth() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.display = TaffyDisplay.FLEX;
        childStyle.itemIsTable = true;
        NodeId child = tree.newLeafWithMeasure(childStyle, fixedMeasure(20.0f, 10.0f));

        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.BLOCK;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout childLayout = tree.getLayout(child);
        assertEquals(20.0f, childLayout.size().width, EPSILON);
        assertEquals(10.0f, childLayout.size().height, EPSILON);
    }

    @Test
    @DisplayName("block_non_table_child_gets_stretch_fit_width")
    void blockNonTableChildGetsStretchFitWidth() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.display = TaffyDisplay.FLEX;
        childStyle.itemIsTable = false;
        NodeId child = tree.newLeafWithMeasure(childStyle, fixedMeasure(20.0f, 10.0f));

        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.BLOCK;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout childLayout = tree.getLayout(child);
        assertEquals(100.0f, childLayout.size().width, EPSILON);
        assertEquals(10.0f, childLayout.size().height, EPSILON);
    }

    @Test
    @DisplayName("grid_compressible_replaced_minimum_contribution_is_capped_by_max_size")
    void gridCompressibleReplacedMinimumContributionIsCappedByMaxSize() {
        TaffyTree tree = new TaffyTree();

        // Grid item: intrinsic (min-content/max-content) width 100.
        // The max-width is a percentage which is unresolvable during track sizing when the container width is indefinite.
        // Rust applies a special rule for compressible replaced elements and resolves such percentages against 0 for capping.
        TaffyStyle itemStyle = new TaffyStyle();
        itemStyle.display = TaffyDisplay.FLEX;
        itemStyle.itemIsReplaced = true;
        itemStyle.padding = TaffyRect.all(LengthPercentage.length(5.0f));
        itemStyle.maxSize = new TaffySize<>(TaffyDimension.from(LengthPercentage.percent(0.5f)), TaffyDimension.AUTO);
        NodeId item = tree.newLeafWithMeasure(itemStyle, intrinsicWithAvailableClamp(100.0f, 10.0f));

        TaffyStyle gridStyle = new TaffyStyle();
        gridStyle.display = TaffyDisplay.GRID;
        gridStyle.gridTemplateColumns = new ArrayList<>();
        gridStyle.gridTemplateColumns.add(TrackSizingFunction.auto());

        NodeId grid = tree.newWithChildren(gridStyle, item);

        // Use a min-content constraint so auto tracks do NOT get expanded to their growth limit (max-content)
        // in the "maximize tracks" phase. This makes the replaced-element minimum contribution capping observable.
        tree.computeLayout(grid, TaffySize.minContent());

        // With capping, the auto track (and thus the grid) should shrink-wrap to the padding+border sum (10).
        Layout gridLayout = tree.getLayout(grid);
        assertEquals(10.0f, gridLayout.size().width, EPSILON);

        Layout itemLayout = tree.getLayout(item);
        assertEquals(10.0f, itemLayout.size().width, EPSILON);
    }

    @Test
    @DisplayName("grid_non_replaced_item_does_not_apply_compressible_replaced_capping")
    void gridNonReplacedItemDoesNotApplyCompressibleReplacedCapping() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle itemStyle = new TaffyStyle();
        itemStyle.display = TaffyDisplay.FLEX;
        itemStyle.itemIsReplaced = false;
        itemStyle.padding = TaffyRect.all(LengthPercentage.length(5.0f));
        itemStyle.maxSize = new TaffySize<>(TaffyDimension.from(LengthPercentage.percent(0.5f)), TaffyDimension.AUTO);
        NodeId item = tree.newLeafWithMeasure(itemStyle, intrinsicWithAvailableClamp(100.0f, 10.0f));

        TaffyStyle gridStyle = new TaffyStyle();
        gridStyle.display = TaffyDisplay.GRID;
        gridStyle.gridTemplateColumns = new ArrayList<>();
        gridStyle.gridTemplateColumns.add(TrackSizingFunction.auto());

        NodeId grid = tree.newWithChildren(gridStyle, item);

        tree.computeLayout(grid, TaffySize.minContent());

        // Non-replaced items should not use the compressible replaced capping rule.
        // With an indefinite container width, the percent max-size is unresolvable and should not affect track sizing,
        // so the grid will still size itself based on content contributions.
        Layout gridLayout = tree.getLayout(grid);
        assertEquals(110.0f, gridLayout.size().width, EPSILON);

        Layout itemLayout = tree.getLayout(item);
        assertEquals(110.0f, itemLayout.size().width, EPSILON);
    }
}
