package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ST-05 / upstream #1256 ("split is_replaced and is_compressible_replaced") and #1351 (a form
 * control's percentage max size must not be resolved against zero in the minimum contribution).
 *
 * <p>TaffyStyle keeps the historical {@code itemIsReplaced} flag and adds
 * {@code itemIsCompressibleReplaced} for form controls with an intrinsic size. The derived accessor
 * {@link TaffyStyle#isCompressibleReplaced()} ORs both (a replaced element is implicitly a
 * compressible replaced element), so with the new flag unset the grid path behaves exactly as
 * before. Block/flex wiring of the new flag and the remaining #1351/S19 semantics are out of scope
 * for this split.
 */
public class ReplacedSplitTest {

    private static final float EPSILON = 0.1f;

    /**
     * Returns an intrinsic size unless the parent provides a definite available space in that axis,
     * in which case it clamps to that available space. Same helper as StyleItemFlagsCompatTest.
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

    /**
     * A grid item with intrinsic width 100, padding 5 on both sides and {@code max-width: 50%} in a
     * grid whose inline size is indefinite during track sizing. Returns the width of the
     * single-column (auto-track) grid under a min-content constraint.
     *
     * <p>A compressible replaced item caps its minimum contribution with a definite preferred/max
     * size, resolving indefinite percentages against zero; the cap therefore floors at the
     * padding+border sum (10). A non-compressible item keeps the full content contribution (110).
     */
    private static Layout gridMinContentLayout(boolean replaced, boolean compressibleReplaced) {
        TaffyTree tree = new TaffyTree();

        TaffyStyle itemStyle = new TaffyStyle();
        itemStyle.display = TaffyDisplay.FLEX;
        itemStyle.itemIsReplaced = replaced;
        itemStyle.itemIsCompressibleReplaced = compressibleReplaced;
        itemStyle.padding = TaffyRect.all(LengthPercentage.length(5.0f));
        itemStyle.maxSize = new TaffySize<>(TaffyDimension.from(LengthPercentage.percent(0.5f)), TaffyDimension.AUTO);
        NodeId item = tree.newLeafWithMeasure(itemStyle, intrinsicWithAvailableClamp(100.0f, 10.0f));

        TaffyStyle gridStyle = new TaffyStyle();
        gridStyle.display = TaffyDisplay.GRID;
        gridStyle.gridTemplateColumns = new ArrayList<>();
        gridStyle.gridTemplateColumns.add(TrackSizingFunction.auto());

        NodeId grid = tree.newWithChildren(gridStyle, item);

        // min-content: the auto track must not be grown to its max-content growth limit, so the
        // capped minimum contribution stays observable.
        tree.computeLayout(grid, TaffySize.minContent());

        return tree.getLayout(grid);
    }

    // === Style-level semantics ===

    @Test
    @DisplayName("compressible_replaced_defaults_to_item_is_replaced")
    void compressibleReplacedDefaultsToItemIsReplaced() {
        TaffyStyle plain = new TaffyStyle();
        assertFalse(plain.getItemIsReplaced());
        assertFalse(plain.itemIsCompressibleReplaced);
        assertFalse(plain.isCompressibleReplaced());

        TaffyStyle replaced = new TaffyStyle();
        replaced.itemIsReplaced = true;
        assertTrue(replaced.isCompressibleReplaced());
        assertFalse(replaced.itemIsCompressibleReplaced);
    }

    @Test
    @DisplayName("set_compressible_replaced_does_not_change_item_is_replaced")
    void setCompressibleReplacedDoesNotChangeItemIsReplaced() {
        TaffyStyle formControl = new TaffyStyle();
        formControl.setCompressibleReplaced(true);
        assertTrue(formControl.itemIsCompressibleReplaced);
        assertTrue(formControl.isCompressibleReplaced());
        assertFalse(formControl.getItemIsReplaced());

        TaffyStyle replacedElement = new TaffyStyle();
        replacedElement.itemIsReplaced = true;
        replacedElement.setCompressibleReplaced(false);
        assertTrue(replacedElement.getItemIsReplaced());
        // A replaced element is implicitly compressible replaced (upstream CoreStyle contract).
        assertTrue(replacedElement.isCompressibleReplaced());
    }

    @Test
    @DisplayName("copy_preserves_compressible_replaced")
    void copyPreservesCompressibleReplaced() {
        TaffyStyle formControl = new TaffyStyle();
        formControl.setCompressibleReplaced(true);
        TaffyStyle copy = formControl.copy();
        assertTrue(copy.itemIsCompressibleReplaced);
        assertTrue(copy.isCompressibleReplaced());
        assertFalse(copy.getItemIsReplaced());

        TaffyStyle replacedElement = new TaffyStyle();
        replacedElement.itemIsReplaced = true;
        TaffyStyle replacedCopy = replacedElement.copy();
        assertTrue(replacedCopy.getItemIsReplaced());
        assertTrue(replacedCopy.isCompressibleReplaced());

        // Both flags survive a copy unchanged.
        TaffyStyle both = new TaffyStyle();
        both.itemIsReplaced = true;
        both.setCompressibleReplaced(true);
        TaffyStyle bothCopy = both.copy();
        assertTrue(bothCopy.getItemIsReplaced());
        assertTrue(bothCopy.itemIsCompressibleReplaced);
        assertTrue(bothCopy.isCompressibleReplaced());
    }

    // === Grid path ===

    @Test
    @DisplayName("grid_default_derivation_keeps_capping_replaced_items")
    void gridDefaultDerivationKeepsCappingReplacedItems() {
        Layout grid = gridMinContentLayout(true, false);
        // itemIsReplaced alone (new flag unset) still drives the compressible-replaced cap.
        assertEquals(10.0f, grid.size().width, EPSILON);
    }

    @Test
    @DisplayName("grid_default_derivation_keeps_non_replaced_uncapped")
    void gridDefaultDerivationKeepsNonReplacedUncapped() {
        Layout grid = gridMinContentLayout(false, false);
        assertEquals(110.0f, grid.size().width, EPSILON);
    }

    @Test
    @DisplayName("grid_explicit_compressible_replaced_uses_the_cap")
    void gridExplicitCompressibleReplacedUsesTheCap() {
        // A form control: not replaced, but compressible replaced. Before the split this flag did
        // not exist and the item behaved like a plain (non-replaced) item.
        Layout grid = gridMinContentLayout(false, true);
        assertEquals(10.0f, grid.size().width, EPSILON);
    }

    @Test
    @DisplayName("grid_replaced_and_explicit_compressible_agree")
    void gridReplacedAndExplicitCompressibleAgree() {
        // Both spellings of "compressible replaced" must produce the same track size.
        assertEquals(
            gridMinContentLayout(true, false).size().width,
            gridMinContentLayout(false, true).size().width,
            EPSILON
        );
    }
}
