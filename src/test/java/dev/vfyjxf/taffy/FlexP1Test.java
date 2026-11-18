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

    // ==================================================================
    // FL-03 (#1183 / fc379ea): the intrinsic clamping basis is axis
    // dependent. Rows use max(flex_basis, style_preferred); columns use
    // flex_basis only, so a column item's height cannot inflate the
    // container's max-content size beyond its flex-basis.
    //
    // Upstream fixture: blockflex_min_content_ignores_own_height_as_percentage_basis
    // (the block child's 100% height is not needed for the flex-side fix).
    // ==================================================================

    @Test
    @DisplayName("FL-03: column clamping basis is flex-basis, not max(flex-basis, height)")
    void fl03_columnClampingBasisUsesFlexBasisOnly() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.COLUMN;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.AUTO);

        TaffyStyle itemStyle = new TaffyStyle();
        itemStyle.flexBasis = TaffyDimension.length(100f);
        itemStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(200f));
        NodeId item = tree.newLeaf(itemStyle);
        NodeId root = tree.newWithChildren(rootStyle, item);

        tree.computeLayout(root, TaffySize.maxContent());

        // old: clamping basis = max(100, 200) = 200 -> container height 200
        // new: clamping basis = flex-basis = 100 -> container height 100
        assertSize(tree.getLayout(root), 100f, 100f, "column container max-content size");
        assertSize(tree.getLayout(item), 100f, 100f, "column item size");
    }

    @Test
    @DisplayName("FL-03: row clamping basis keeps max(flex-basis, width)")
    void fl03_rowClampingBasisKeepsMaxWithPreferred() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.ROW;

        TaffyStyle itemStyle = new TaffyStyle();
        itemStyle.flexBasis = TaffyDimension.length(100f);
        itemStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
        NodeId item = tree.newLeaf(itemStyle);
        NodeId root = tree.newWithChildren(rootStyle, item);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 200f, 0f, "row container max-content size");
        assertEquals(100f, tree.getLayout(item).size().width, EPS, "row item keeps its flex-basis target");
    }

    // ==================================================================
    // FL-04 (#1164 / adef6dd): in the intrinsic contribution the inner size
    // is floored/addressed first, then the main-axis margin is added, and
    // only then the style min/max clamp is applied.
    //
    // Upstream fixtures: flex_grow_negative_margin_intrinsic_column and
    // flex_grow_flex_shrink_negative_margin_intrinsic_column. The gridflex
    // fixture of the same upstream commit is cross-domain (grid) and skipped.
    // ==================================================================

    @Test
    @DisplayName("FL-04: negative main-axis margin is applied after the flex-basis floor (column)")
    void fl04_negativeMarginAppliedAfterFloorColumn() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.COLUMN;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.AUTO);

        TaffyStyle itemStyle = withSize(100f, 100f);
        itemStyle.flexGrow = 1f;
        itemStyle.flexShrink = 0f;
        itemStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(-24f), LengthPercentageAuto.ZERO);
        NodeId item = tree.newLeaf(itemStyle);
        NodeId root = tree.newWithChildren(rootStyle, item);

        tree.computeLayout(root, TaffySize.maxContent());

        // old: (100 - 24).max(flex-basis = 100) = 100 -> container 100
        // new: (100.max(100) - 24) = 76 -> container 76
        assertSize(tree.getLayout(root), 300f, 76f, "column container intrinsic main size");
        assertSize(tree.getLayout(item), 100f, 100f, "column item size");
        assertLocation(tree.getLayout(item), 0f, -24f, "column item position");
    }

    @Test
    @DisplayName("FL-04: negative main-axis margin is applied after the flex-basis floor (row)")
    void fl04_negativeMarginAppliedAfterFloorRow() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.ROW;

        TaffyStyle itemStyle = withSize(100f, 100f);
        itemStyle.flexGrow = 1f;
        itemStyle.flexShrink = 0f;
        itemStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.length(-24f), LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId item = tree.newLeaf(itemStyle);
        NodeId root = tree.newWithChildren(rootStyle, item);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 76f, 100f, "row container intrinsic main size");
        assertLocation(tree.getLayout(item), -24f, 0f, "row item position");
    }

    // ==================================================================
    // FL-05 (#1335 / ea07c4c): intrinsic max-content main size with negative
    // margins.
    //   1. the item contribution in the line sum includes the main-axis margin
    //   2. the content flex fraction subtracts the margin (it is already part
    //      of content_contribution)
    //   3. a wrapping row container is floored at its largest single item
    //
    // Upstream fixtures: intrinsic_sizing_main_size_row_negative_margin_*
    // and the last row of taffy_issue_1151_negative_margin.
    // ==================================================================

    @Test
    @DisplayName("FL-05: row max-content contribution applies the item's negative margin once")
    void fl05_rowNegativeMarginPaddingBorder() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.ROW;

        TaffyStyle itemStyle = new TaffyStyle();
        itemStyle.padding = new TaffyRect<>(
            LengthPercentage.length(5f), LengthPercentage.ZERO,
            LengthPercentage.ZERO, LengthPercentage.ZERO);
        itemStyle.border = new TaffyRect<>(
            LengthPercentage.ZERO, LengthPercentage.length(1f),
            LengthPercentage.ZERO, LengthPercentage.ZERO);
        itemStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.length(-2f), LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId item = tree.newLeaf(itemStyle);
        NodeId root = tree.newWithChildren(rootStyle, item);

        tree.computeLayout(root, TaffySize.maxContent());

        // item: flex-basis 6 (5 padding + 1 border), contribution 4, margin -2
        // old line sum: 6 (margin dropped by the second pass) -> container 6
        // new line sum: 6 - 2 = 4 -> container 4
        assertSize(tree.getLayout(root), 4f, 0f, "row container max-content size");
        assertSize(tree.getLayout(item), 6f, 0f, "item size");
        assertLocation(tree.getLayout(item), -2f, 0f, "item position");
    }

    @Test
    @DisplayName("FL-05: multiple negative-margin items contribute their margins to the row max-content size")
    void fl05_rowNegativeMarginMultipleItems() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.ROW;

        NodeId first = tree.newLeaf(withSize(100f, Float.NaN));

        TaffyStyle secondStyle = withSize(0f, Float.NaN);
        secondStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.length(-10f), LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId second = tree.newLeaf(secondStyle);

        TaffyStyle thirdStyle = new TaffyStyle();
        thirdStyle.padding = new TaffyRect<>(
            LengthPercentage.length(5f), LengthPercentage.ZERO,
            LengthPercentage.ZERO, LengthPercentage.ZERO);
        thirdStyle.border = new TaffyRect<>(
            LengthPercentage.ZERO, LengthPercentage.length(1f),
            LengthPercentage.ZERO, LengthPercentage.ZERO);
        thirdStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.length(-10f), LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId third = tree.newLeaf(thirdStyle);

        NodeId root = tree.newWithChildren(rootStyle, first, second, third);
        tree.computeLayout(root, TaffySize.maxContent());

        // upstream fixture: container 86, items 100 / 0 / 6 at x = 0 / 90 / 80
        assertSize(tree.getLayout(root), 86f, 0f, "row container max-content size");
        assertEquals(100f, tree.getLayout(first).size().width, EPS, "first item width");
        assertEquals(0f, tree.getLayout(second).size().width, EPS, "second item width");
        assertEquals(6f, tree.getLayout(third).size().width, EPS, "third item width");
        assertLocation(tree.getLayout(first), 0f, 0f, "first item position");
        assertLocation(tree.getLayout(second), 90f, 0f, "second item position");
        assertLocation(tree.getLayout(third), 80f, 0f, "third item position");
    }

    @Test
    @DisplayName("FL-05: wrapping row max-content size includes negative margins")
    void fl05_rowWrapNegativeMarginMultipleItems() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.flexWrap = FlexWrap.WRAP;

        NodeId first = tree.newLeaf(withSize(100f, Float.NaN));

        TaffyStyle secondStyle = withSize(0f, Float.NaN);
        secondStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.length(-10f), LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId second = tree.newLeaf(secondStyle);
        NodeId third = tree.newLeaf(withSize(50f, Float.NaN));

        NodeId root = tree.newWithChildren(rootStyle, first, second, third);
        tree.computeLayout(root, TaffySize.maxContent());

        // upstream fixture: container 140, items 100 / 0 / 50 at x = 0 / 90 / 90
        assertSize(tree.getLayout(root), 140f, 0f, "wrapping row container max-content size");
        assertLocation(tree.getLayout(first), 0f, 0f, "first item position");
        assertLocation(tree.getLayout(second), 90f, 0f, "second item position");
        assertLocation(tree.getLayout(third), 90f, 0f, "third item position");
    }

    @Test
    @DisplayName("FL-05: wrapping row max-content size is floored at its largest single item")
    void fl05_rowWrapFlooredAtLargestItem() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.flexWrap = FlexWrap.WRAP;

        NodeId first = tree.newLeaf(withSize(100f, Float.NaN));

        TaffyStyle secondStyle = withSize(0f, Float.NaN);
        secondStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.length(-10f), LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId second = tree.newLeaf(secondStyle);

        NodeId root = tree.newWithChildren(rootStyle, first, second);
        tree.computeLayout(root, TaffySize.maxContent());

        // line sum = 100 + (0 - 10) = 90; the floor makes the container 100 (the largest item).
        assertSize(tree.getLayout(root), 100f, 0f, "wrapping row container floors at the largest item");
        assertLocation(tree.getLayout(first), 0f, 0f, "first item position");
        assertLocation(tree.getLayout(second), 90f, 0f, "second item position");
    }

    // ==================================================================
    // FL-06 (#1101 / 2e4dbc5, #1176 / ac2b869): where flex lines wrap.
    //   - max-width caps wrapping when the container's main size is indefinite
    //   - an auto-height column container does not wrap against definite
    //     available space handed down by an ancestor
    //
    // NOTE: Java approximates upstream's has_definite_main_size with
    // !Float.isNaN(knownInnerMainSize) (TODO FL-15/C01).
    // ==================================================================

    @Test
    @DisplayName("FL-06: wrap row uses min(available, max-width) when its width is indefinite")
    void fl06_wrapRowMaxWidthIndefiniteWidth() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.flexWrap = FlexWrap.WRAP;
        rootStyle.maxSize = new TaffySize<>(TaffyDimension.length(20f), TaffyDimension.AUTO);
        rootStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(110f));

        NodeId first = tree.newLeaf(withSize(20f, 10f));
        NodeId second = tree.newLeaf(withSize(20f, 40f));
        NodeId root = tree.newWithChildren(rootStyle, first, second);

        // 200px viewport from the upstream fixture's wrapper div; without FL-06 the items wrap
        // against 200 and stay on one line.
        tree.computeLayout(root, TaffySize.of(AvailableSpace.definite(200f), AvailableSpace.maxContent()));

        assertSize(tree.getLayout(root), 20f, 110f, "wrapped row container size");
        assertSize(tree.getLayout(first), 20f, 10f, "first item size");
        assertLocation(tree.getLayout(first), 0f, 0f, "first item position");
        assertSize(tree.getLayout(second), 20f, 40f, "second item size");
        assertLocation(tree.getLayout(second), 0f, 40f, "second item position");
    }

    @Test
    @DisplayName("FL-06: auto-height column does not wrap against ancestor available space")
    void fl06_wrapColumnAutoHeightDoesNotWrapAgainstAvailableSpace() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.alignItems = AlignItems.FLEX_START;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(400f), TaffyDimension.length(150f));

        TaffyStyle columnStyle = flexBox();
        columnStyle.flexDirection = FlexDirection.COLUMN;
        columnStyle.flexWrap = FlexWrap.WRAP;

        NodeId first = tree.newLeaf(withSize(60f, 100f));
        NodeId second = tree.newLeaf(withSize(60f, 100f));
        NodeId column = tree.newWithChildren(columnStyle, first, second);
        NodeId root = tree.newWithChildren(rootStyle, column);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 400f, 150f, "row container size");
        // without FL-06 the column wraps two 100px items against the ancestor's 150px height
        assertSize(tree.getLayout(column), 60f, 200f, "column container does not wrap");
        assertLocation(tree.getLayout(first), 0f, 0f, "first item position");
        assertLocation(tree.getLayout(second), 0f, 100f, "second item position");
    }

    // ==================================================================
    // FL-07 (#1286 / e4bd9c2): an unresolved percentage flex-basis is a
    // content measurement; only flex-basis: auto may fall back to the main
    // size style.
    //
    // The upstream fixture (blockflex_indefinite_percentage_flex_basis) cannot
    // go red/green in Java: its item content measures to the same 100px as the
    // preferred height, so both the old and the new code produce the same
    // values. The reduction below isolates the basis fallback itself.
    // ==================================================================

    @Test
    @DisplayName("FL-07: unresolved percentage flex-basis measures content instead of using the height")
    void fl07_unresolvedPercentageFlexBasisUsesContent() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.COLUMN;

        TaffyStyle itemStyle = new TaffyStyle();
        itemStyle.flexBasis = TaffyDimension.percent(50f); // container main size is indefinite
        itemStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(100f));
        NodeId item = tree.newLeaf(itemStyle);
        NodeId root = tree.newWithChildren(rootStyle, item);

        tree.computeLayout(root, TaffySize.maxContent());

        // old: basis falls back to the 100px height -> container 100
        // new: basis is the (empty) content size 0 -> container 0
        assertSize(tree.getLayout(root), 0f, 0f, "column container max-content size");
        assertSize(tree.getLayout(item), 0f, 0f, "item used size");
    }

    @Test
    @DisplayName("FL-07: flex-basis auto still falls back to the main size")
    void fl07_autoFlexBasisFallsBackToMainSize() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.COLUMN;

        TaffyStyle itemStyle = new TaffyStyle();
        itemStyle.flexBasis = TaffyDimension.AUTO;
        itemStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(100f));
        NodeId item = tree.newLeaf(itemStyle);
        NodeId root = tree.newWithChildren(rootStyle, item);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 0f, 100f, "column container max-content size");
        assertSize(tree.getLayout(item), 0f, 100f, "item used size");
    }
}