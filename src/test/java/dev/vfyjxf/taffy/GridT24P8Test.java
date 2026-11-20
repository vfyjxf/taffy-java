package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P8/T24 tests for the grid intrinsic-contribution available space (GR-06, upstream #1234
 * {@code 04965fc5}).
 *
 * <p>Upstream's margin convention is uniform: a definite available space is the space available to
 * a node's <em>border box</em>, so the parent subtracts the child's margins before handing it over
 * (see {@code LeafMarginP8Test} for the leaf half of X2/C17). The grid intrinsic-contribution
 * measurements are the item's parent too, but they used to measure against the raw grid area:
 * {@code GridItem}'s min/max-content contributions and the two re-run contribution checks passed
 * the grid area's definite axis straight to {@code measureChildSize}. Upstream's
 * {@code GridItem::available_space_minus_margins} subtracts the item's margins there as well, per
 * axis, floored at zero, with the margins resolved against the width of the grid area and
 * {@code auto} margins counting as zero.</p>
 *
 * <p>This suite pins:</p>
 * <ul>
 *   <li>an auto and a flexible (fr) column item's measure callback sees the grid area minus its
 *       margins on the definite axis;</li>
 *   <li>the margin-free values are unchanged (200 / 100 for a 200x100 grid);</li>
 *   <li>the axes are subtracted independently and a margin larger than the area is floored at
 *       zero;</li>
 *   <li>percentage margins resolve against the grid area's inline size and {@code auto} margins
 *       count as zero;</li>
 *   <li>the block and flex parent paths keep their existing single deduction.</li>
 * </ul>
 */
public class GridT24P8Test {

    private static final float EPS = 0.001f;

    /** The definite grid size every scenario starts from. */
    private static final float GRID_WIDTH = 200f;
    private static final float GRID_HEIGHT = 100f;

    // ------------------------------------------------------------------
    // Style helpers
    // ------------------------------------------------------------------

    private static TaffyStyle grid(TrackSizingFunction column) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        style.size = new TaffySize<>(TaffyDimension.length(GRID_WIDTH), TaffyDimension.length(GRID_HEIGHT));
        style.gridTemplateColumns = new ArrayList<>(List.of(column));
        return style;
    }

    private static TaffyStyle gridWithFixedRow(TrackSizingFunction column, float rowHeight) {
        TaffyStyle style = grid(column);
        style.gridTemplateRows = new ArrayList<>(List.of(TrackSizingFunction.fixed(rowHeight)));
        return style;
    }

    private static TaffyStyle gridWithFixedColumn(float columnWidth, TrackSizingFunction rowFunction) {
        TaffyStyle style = grid(rowFunction);
        style.gridTemplateColumns = new ArrayList<>(List.of(TrackSizingFunction.fixed(columnWidth)));
        return style;
    }

    private static TaffyStyle block(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle flexRow(float width, float height) {
        TaffyStyle style = block(width, height);
        style.display = TaffyDisplay.FLEX;
        style.alignItems = AlignItems.START;
        return style;
    }

    private static TaffyStyle blockLeaf() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        return style;
    }

    private static TaffyStyle margins(float left, float right, float top, float bottom) {
        TaffyStyle style = blockLeaf();
        style.margin = new TaffyRect<>(
            LengthPercentageAuto.length(left), LengthPercentageAuto.length(right),
            LengthPercentageAuto.length(top), LengthPercentageAuto.length(bottom));
        return style;
    }

    private static TaffyStyle allMargins(float margin) {
        return margins(margin, margin, margin, margin);
    }

    private static TaffyStyle percentageMargins(float left, float right, float top, float bottom) {
        TaffyStyle style = blockLeaf();
        style.margin = new TaffyRect<>(
            LengthPercentageAuto.percent(left), LengthPercentageAuto.percent(right),
            LengthPercentageAuto.percent(top), LengthPercentageAuto.percent(bottom));
        return style;
    }

    private static TaffyStyle autoMargins() {
        TaffyStyle style = blockLeaf();
        style.margin = TaffyRect.all(LengthPercentageAuto.auto());
        return style;
    }

    // ------------------------------------------------------------------
    // Layout helpers
    // ------------------------------------------------------------------

    /** One measured leaf plus the available space every measure pass observed. */
    private static final class Measured {
        private final TaffyTree tree = new TaffyTree();
        private final TaffyStyle parentStyle;
        private final List<TaffySize<AvailableSpace>> captures = new ArrayList<>();

        Measured(TaffyStyle parentStyle) {
            this.parentStyle = parentStyle;
        }

        /** A leaf that reports the available space as its content size, so the capture is visible. */
        Layout layoutWithContentSizedLeaf(TaffyStyle child) {
            return layout(child, (knownDimensions, availableSpace) -> {
                captures.add(availableSpace);
                float width = availableSpace.width.isDefinite() ? availableSpace.width.getValue() : 30f;
                float height = availableSpace.height.isDefinite() ? availableSpace.height.getValue() : 10f;
                return new FloatSize(width, height);
            });
        }

        /** A leaf with a fixed content size, so auto margins show up in the final position. */
        Layout layoutWithFixedLeaf(TaffyStyle child, float width, float height) {
            return layout(child, (knownDimensions, availableSpace) -> {
                captures.add(availableSpace);
                return new FloatSize(width, height);
            });
        }

        private Layout layout(TaffyStyle child, MeasureFunc measureFunc) {
            NodeId leaf = tree.newLeafWithMeasure(child, measureFunc);
            NodeId root = tree.newWithChildren(parentStyle, leaf);
            // The viewport is larger than the grid so that a grid item is never confused with the
            // root's own available space.
            tree.computeLayout(root, new TaffySize<>(
                AvailableSpace.definite(GRID_WIDTH * 2f),
                AvailableSpace.definite(GRID_HEIGHT * 3f)
            ));
            return tree.getLayout(leaf);
        }

        /** The definite values a measure callback observed on one axis. */
        List<Float> definiteValues(boolean width) {
            List<Float> values = new ArrayList<>();
            for (TaffySize<AvailableSpace> capture : captures) {
                AvailableSpace space = width ? capture.width : capture.height;
                if (space.isDefinite()) {
                    values.add(space.getValue());
                }
            }
            return values;
        }

        void assertAllDefinite(boolean width, float expected, String message) {
            List<Float> values = definiteValues(width);
            assertFalse(values.isEmpty(), message + ": the measure callback never saw a definite value");
            for (float value : values) {
                assertEquals(expected, value, EPS, message);
            }
        }

        void assertNoDefinite(boolean width, float unexpected, String message) {
            List<Float> values = definiteValues(width);
            assertFalse(values.contains(unexpected), message);
        }

        /**
         * The first definite value observed on an axis. Track sizing runs before the placement pass,
         * so the first definite value is the one the intrinsic contribution was measured with (the
         * placement pass may later override the axis with the item's known size).
         */
        float firstDefinite(boolean width, String message) {
            List<Float> values = definiteValues(width);
            assertFalse(values.isEmpty(), message + ": the measure callback never saw a definite value");
            return values.get(0);
        }
    }

    private static Measured measure(TaffyStyle parentStyle) {
        return new Measured(parentStyle);
    }

    private static void assertSize(Layout layout, float width, float height, String message) {
        assertEquals(width, layout.size().width, EPS, message + " width");
        assertEquals(height, layout.size().height, EPS, message + " height");
    }

    private static void assertLocation(Layout layout, float x, float y, String message) {
        assertEquals(x, layout.location().x, EPS, message + " x");
        assertEquals(y, layout.location().y, EPS, message + " y");
    }

    // ------------------------------------------------------------------
    // Intrinsic contributions: auto and fixed columns
    // ------------------------------------------------------------------

    /**
     * A leaf with 10px margins in an auto-sized column. The block-axis max-content contribution is
     * measured under the grid area's definite width, which must be 200 - 2 * 10 = 180; the
     * inline-axis min-content contribution is measured under the definite height of 100 - 2 * 10 =
     * 80. The final margin box fills the 200x100 grid area exactly once, so the measurement and the
     * contribution accounting do not subtract the margins twice.
     */
    @Test
    @DisplayName("auto column: intrinsic measure sees the grid area minus the item's margins")
    void autoColumnIntrinsicMeasureSubtractsMargins() {
        TaffyStyle child = allMargins(10f);
        // Keep the item content-sized on both axes: a stretched axis becomes a known dimension
        // (area - margins), which the leaf measure path then prefers over the available space and
        // would mask what the contribution actually hands over.
        child.justifySelf = AlignItems.CENTER;
        child.alignSelf = AlignItems.START;
        Measured measured = measure(grid(TrackSizingFunction.auto()));
        Layout layout = measured.layoutWithContentSizedLeaf(child);

        assertSize(layout, 180f, 80f, "auto-column item with 10px margins");
        assertLocation(layout, 10f, 10f, "auto-column item with 10px margins");
        measured.assertAllDefinite(true, 180f, "block-axis contribution available width");
        measured.assertAllDefinite(false, 80f, "inline-axis contribution available height");
        measured.assertNoDefinite(true, 200f, "the undeducted grid-area width must never reach the measure callback");
        measured.assertNoDefinite(false, 100f, "the undeducted grid-area height must never reach the measure callback");
    }

    /** The same numbers for a flexible (fr) column, whose track is definite when measured. */
    @Test
    @DisplayName("fr column: intrinsic measure sees the grid area minus the item's margins")
    void frColumnIntrinsicMeasureSubtractsMargins() {
        TaffyStyle child = allMargins(10f);
        child.justifySelf = AlignItems.CENTER;
        child.alignSelf = AlignItems.START;
        Measured measured = measure(grid(TrackSizingFunction.fr(1)));
        Layout layout = measured.layoutWithContentSizedLeaf(child);

        assertSize(layout, 180f, 80f, "fr-column item with 10px margins");
        assertLocation(layout, 10f, 10f, "fr-column item with 10px margins");
        measured.assertAllDefinite(true, 180f, "block-axis contribution available width");
        measured.assertAllDefinite(false, 80f, "inline-axis contribution available height");
    }

    /**
     * Without margins the whole grid area reaches the measure callback, exactly as before T24:
     * auto and fr columns both see 200 / 100.
     */
    @Test
    @DisplayName("auto and fr columns without margins: the measured available space is unchanged")
    void marginFreeColumnsKeepTheirOldValues() {
        TaffyStyle child = blockLeaf();
        child.justifySelf = AlignItems.CENTER;
        child.alignSelf = AlignItems.START;
        Measured auto = measure(grid(TrackSizingFunction.auto()));
        Layout autoLayout = auto.layoutWithContentSizedLeaf(child);
        assertSize(autoLayout, 200f, 100f, "margin-free auto-column item");
        auto.assertAllDefinite(true, 200f, "margin-free auto-column available width");
        auto.assertAllDefinite(false, 100f, "margin-free auto-column available height");

        Measured fr = measure(grid(TrackSizingFunction.fr(1)));
        Layout frLayout = fr.layoutWithContentSizedLeaf(child);
        assertSize(frLayout, 200f, 100f, "margin-free fr-column item");
        fr.assertAllDefinite(true, 200f, "margin-free fr-column available width");
        fr.assertAllDefinite(false, 100f, "margin-free fr-column available height");
    }

    /**
     * The two axes subtract their own margins only: with left 5 / right 15 / top 20 / bottom 40 the
     * available space is 200 - (5 + 15) = 180 wide and 100 - (20 + 40) = 40 high. The item is
     * placed at the left/top margins, which confirms the same values drive the final position.
     */
    @Test
    @DisplayName("margins are subtracted per axis, not summed")
    void marginsAreSubtractedPerAxis() {
        TaffyStyle child = margins(5f, 15f, 20f, 40f);
        child.justifySelf = AlignItems.CENTER;
        child.alignSelf = AlignItems.START;
        Measured measured = measure(grid(TrackSizingFunction.auto()));
        Layout layout = measured.layoutWithContentSizedLeaf(child);

        assertSize(layout, 180f, 40f, "asymmetric-margin item");
        assertLocation(layout, 5f, 20f, "asymmetric-margin item");
        measured.assertAllDefinite(true, 180f, "inline margins only narrow the width");
        measured.assertAllDefinite(false, 40f, "block margins only narrow the height");
    }

    /** A fixed row gives the inline-axis contribution a definite block axis: 50 - 20 = 30. */
    @Test
    @DisplayName("fixed row: inline-axis contribution sees the row minus the item's vertical margins")
    void fixedRowIntrinsicMeasureSubtractsMargins() {
        TaffyStyle child = allMargins(10f);
        child.justifySelf = AlignItems.CENTER;
        child.alignSelf = AlignItems.START;
        Measured measured = measure(gridWithFixedRow(TrackSizingFunction.fr(1), 50f));
        Layout layout = measured.layoutWithContentSizedLeaf(child);

        assertSize(layout, 180f, 30f, "fixed-row item with 10px margins");
        assertLocation(layout, 10f, 10f, "fixed-row item with 10px margins");
        measured.assertAllDefinite(true, 180f, "block-axis contribution available width");
        measured.assertAllDefinite(false, 30f, "inline-axis contribution available height");
    }

    /**
     * A margin larger than the axis is floored at zero instead of handing a negative available
     * space to the measure callback: a 100px fixed column with 60px side margins and a 50px fixed
     * row with 40px block margins both measure under 0.
     */
    @Test
    @DisplayName("margins larger than the area are floored at zero on each axis")
    void oversizedMarginsAreFlooredAtZero() {
        TaffyStyle wide = margins(60f, 60f, 10f, 10f);
        wide.justifySelf = AlignItems.CENTER;
        wide.alignSelf = AlignItems.START;
        Measured horizontal = measure(gridWithFixedColumn(100f, TrackSizingFunction.auto()));
        Layout wideLayout = horizontal.layoutWithContentSizedLeaf(wide);

        assertSize(wideLayout, 0f, 80f, "over-wide margins");
        horizontal.assertAllDefinite(true, 0f, "100 - 2 * 60 is floored at zero");
        horizontal.assertNoDefinite(true, 100f, "the undeducted column width must never reach the measure callback");

        TaffyStyle tall = margins(10f, 10f, 40f, 40f);
        tall.justifySelf = AlignItems.CENTER;
        tall.alignSelf = AlignItems.START;
        Measured vertical = measure(gridWithFixedRow(TrackSizingFunction.fr(1), 50f));
        Layout tallLayout = vertical.layoutWithContentSizedLeaf(tall);

        assertSize(tallLayout, 180f, 0f, "over-tall margins");
        vertical.assertAllDefinite(false, 0f, "50 - 2 * 40 is floored at zero");
        vertical.assertNoDefinite(false, 50f, "the undeducted row height must never reach the measure callback");
    }

    // ------------------------------------------------------------------
    // Margin resolution
    // ------------------------------------------------------------------

    /**
     * Percentage margins resolve against the width of the grid area in the intrinsic measurement
     * too (20px each for 10% of 200), matching the final position pass: 200 - 2 * 20 = 160 wide and
     * 100 - 2 * 20 = 60 high.
     */
    @Test
    @DisplayName("percentage margins resolve against the grid area's inline size")
    void percentageMarginsResolveAgainstTheGridAreaWidth() {
        TaffyStyle child = percentageMargins(0.1f, 0.1f, 0.1f, 0.1f);
        child.justifySelf = AlignItems.CENTER;
        child.alignSelf = AlignItems.START;
        Measured measured = measure(grid(TrackSizingFunction.fr(1)));
        Layout layout = measured.layoutWithContentSizedLeaf(child);

        assertSize(layout, 160f, 60f, "10% margin item");
        assertLocation(layout, 20f, 20f, "10% margin item");
        measured.assertAllDefinite(true, 160f, "percentage inline margins");
        measured.assertAllDefinite(false, 60f, "percentage block margins");
    }

    /**
     * An {@code auto} margin counts as zero for the intrinsic measurement (upstream's
     * {@code resolve_or_zero}), so the whole grid area is available and the auto margins only
     * absorb the free space in the final position pass.
     */
    @Test
    @DisplayName("auto margins count as zero for the intrinsic measurement")
    void autoMarginsCountAsZero() {
        Measured measured = measure(grid(TrackSizingFunction.fr(1)));
        Layout layout = measured.layoutWithFixedLeaf(autoMargins(), 30f, 10f);

        assertSize(layout, 30f, 10f, "auto-margin item");
        assertLocation(layout, 85f, 45f, "auto-margin item is centered");
        assertEquals(200f, measured.firstDefinite(true, "auto-margin item"),
            EPS, "auto margins must not narrow the intrinsic contribution's available width");
        assertEquals(100f, measured.firstDefinite(false, "auto-margin item"),
            EPS, "auto margins must not narrow the intrinsic contribution's available height");
    }

    // ------------------------------------------------------------------
    // Other parent paths must not regress
    // ------------------------------------------------------------------

    /** The block parent already subtracts its child's margins once; T24 must not change that. */
    @Test
    @DisplayName("block parent: the child's available space is unchanged")
    void blockParentDoesNotRegress() {
        TaffyStyle child = allMargins(10f);
        child.justifySelf = AlignItems.CENTER;
        Measured measured = measure(block(GRID_WIDTH, GRID_HEIGHT));
        Layout layout = measured.layoutWithContentSizedLeaf(child);

        assertSize(layout, 180f, 10f, "block child with 10px margins");
        assertLocation(layout, 10f, 10f, "block child with 10px margins");
        measured.assertAllDefinite(true, 180f, "block parent still subtracts the child's margins once");
    }

    /** The flex parent already subtracts its item's cross-axis margins once; T24 keeps that path. */
    @Test
    @DisplayName("flex parent: the item's available space is unchanged")
    void flexParentDoesNotRegress() {
        TaffyStyle child = allMargins(10f);
        Measured measured = measure(flexRow(GRID_WIDTH, GRID_HEIGHT));
        Layout layout = measured.layoutWithContentSizedLeaf(child);

        assertSize(layout, 30f, 80f, "flex item with 10px margins");
        assertLocation(layout, 10f, 10f, "flex item with 10px margins");
        measured.assertAllDefinite(false, 80f, "flex parent still subtracts the item's margins once");
    }
}