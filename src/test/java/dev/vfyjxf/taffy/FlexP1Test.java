package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1 flex correctness fixes (FL-01..FL-09) from the upstream alignment list.
 *
 * <p>Each group of tests mirrors the upstream fixture that was added together with the fix
 * (see {@code Data/taffy-java/analysis/diff-flex-v3.md} section A and the corresponding
 * {@code test_fixtures/flex/*.html} + {@code tests/xml/flex/*.xml} expectations). Where the
 * upstream fixture cannot be reduced to a Java red/green test (cross-domain grid/baseline
 * dependencies or Java measurement semantics), the reduction is documented on the test.
 */
public class FlexP1Test {

    private static final float EPS = 0.01f;

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static TaffyStyle flexBox() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        return style;
    }

    private static TaffyStyle withSize(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(
            Float.isNaN(width) ? TaffyDimension.AUTO : TaffyDimension.length(width),
            Float.isNaN(height) ? TaffyDimension.AUTO : TaffyDimension.length(height)
        );
        return style;
    }

    private static void assertSize(Layout layout, float width, float height, String message) {
        assertEquals(width, layout.size().width, EPS, message + " (width)");
        assertEquals(height, layout.size().height, EPS, message + " (height)");
    }

    private static void assertLocation(Layout layout, float x, float y, String message) {
        assertEquals(x, layout.location().x, EPS, message + " (x)");
        assertEquals(y, layout.location().y, EPS, message + " (y)");
    }

    // ==================================================================
    // FL-01 (#1018 / cef75b6): intrinsic main size must not be floored by
    // the container's padding/border.
    //
    // Upstream fixtures: flex_grow_padding_auto_size_row / _column.
    // ==================================================================

    @Test
    @DisplayName("FL-01: row max-content size ignores container padding as an item floor")
    void fl01_rowMaxContentNotFlooredByPadding() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.padding = TaffyRect.all(LengthPercentage.length(100f));

        TaffyStyle growingStyle = withSize(10f, 50f);
        growingStyle.flexGrow = 1f;
        NodeId growing = tree.newLeaf(growingStyle);
        NodeId fixed = tree.newLeaf(withSize(10f, 50f));

        NodeId root = tree.newWithChildren(rootStyle, growing, fixed);
        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 220f, 250f, "row container max-content size");
        assertSize(tree.getLayout(growing), 10f, 50f, "growing item size");
        assertSize(tree.getLayout(fixed), 10f, 50f, "fixed item size");
        assertLocation(tree.getLayout(growing), 100f, 100f, "growing item position");
        assertLocation(tree.getLayout(fixed), 110f, 100f, "fixed item position");
    }

    @Test
    @DisplayName("FL-01: column max-content size ignores container padding as an item floor")
    void fl01_columnMaxContentNotFlooredByPadding() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.COLUMN;
        rootStyle.padding = TaffyRect.all(LengthPercentage.length(100f));

        TaffyStyle growingStyle = withSize(50f, 10f);
        growingStyle.flexGrow = 1f;
        NodeId growing = tree.newLeaf(growingStyle);
        NodeId fixed = tree.newLeaf(withSize(50f, 10f));

        NodeId root = tree.newWithChildren(rootStyle, growing, fixed);
        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 250f, 220f, "column container max-content size");
        assertSize(tree.getLayout(growing), 50f, 10f, "growing item size");
        assertSize(tree.getLayout(fixed), 50f, 10f, "fixed item size");
        assertLocation(tree.getLayout(growing), 100f, 100f, "growing item position");
        assertLocation(tree.getLayout(fixed), 100f, 110f, "fixed item position");
    }

    // ==================================================================
    // FL-02 (#1152 / 7d31807): the scaled shrink factor used by intrinsic
    // contributions must be max(1, flex_shrink) * inner_flex_basis, and the
    // second pass must not multiply a zero shrink factor by an infinite
    // flex fraction.
    //
    // Upstream fixture: taffy_issue_1151_negative_margin. Here the factor
    // mismatch is isolated with an explicit flex-basis and flex-shrink < 1.
    // ==================================================================

    @Test
    @DisplayName("FL-02: scaled shrink factor is max(1, shrink) * inner basis")
    void fl02_scaledShrinkFactorUsesMaxOneTimesInnerBasis() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.ROW;

        TaffyStyle itemStyle = new TaffyStyle();
        itemStyle.flexBasis = TaffyDimension.length(100f);
        itemStyle.flexShrink = 0.5f;
        MeasureFunc measure = (known, available) -> new FloatSize(
            Float.isNaN(known.width) ? 40f : known.width,
            Float.isNaN(known.height) ? 10f : known.height
        );
        NodeId item = tree.newLeafWithMeasure(itemStyle, measure);
        NodeId root = tree.newWithChildren(rootStyle, item);

        tree.computeLayout(root, TaffySize.maxContent());

        // content contribution 40, flex-basis 100, diff -60
        // new: fraction = -60 / (max(1, 0.5) * 100) = -0.6 -> size = 40
        // old: fraction = -60 / max(1, 0.5 * 100) = -1.2 -> size = -20 -> floored to 0
        Layout rootLayout = tree.getLayout(root);
        assertFalse(Float.isNaN(rootLayout.size().width), "container width must not be NaN");
        assertEquals(40f, rootLayout.size().width, EPS,
            "scaled shrink factor must be max(1, shrink) * inner basis");
    }

    @Test
    @DisplayName("FL-02: zero scaled shrink factor contributes zero instead of NaN")
    void fl02_zeroScaledShrinkFactorContributesZero() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.ROW;

        // inner flex basis is 0 (flex-basis == padding), and the scroll-container
        // contribution path makes diff < 0, so the first pass stores -Infinity.
        TaffyStyle itemStyle = new TaffyStyle();
        itemStyle.flexBasis = TaffyDimension.length(20f);
        itemStyle.flexShrink = 1f;
        itemStyle.maxSize = new TaffySize<>(TaffyDimension.length(10f), TaffyDimension.AUTO);
        itemStyle.padding = new TaffyRect<>(
            LengthPercentage.length(20f), LengthPercentage.ZERO,
            LengthPercentage.ZERO, LengthPercentage.ZERO);
        itemStyle.overflow = new dev.vfyjxf.taffy.geometry.TaffyPoint<>(
            dev.vfyjxf.taffy.style.Overflow.SCROLL, dev.vfyjxf.taffy.style.Overflow.SCROLL);
        NodeId item = tree.newLeaf(itemStyle);
        NodeId root = tree.newWithChildren(rootStyle, item);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout rootLayout = tree.getLayout(root);
        assertFalse(Float.isNaN(rootLayout.size().width), "zero scaled shrink factor must not produce NaN");
        assertEquals(10f, rootLayout.size().width, EPS,
            "content contribution clamps to max-width instead of NaN");
    }
}