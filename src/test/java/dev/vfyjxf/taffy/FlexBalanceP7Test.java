package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.LayoutOutput;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.RunMode;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P7/FL-21 tests for {@code flex-wrap: balance}/{@code balance-reverse} and
 * {@code flex-line-count} (upstream taffy #1105, {@code 1bade66}).
 *
 * <p>Scope: the two new {@link FlexWrap} values, the {@code TaffyStyle.flexLineCount} field, the
 * balanced line-breaking algorithm (squared line sizes minimized by the divide-and-conquer dynamic
 * program, with the zero-sized-item rule and the first-line-bias tie-break), the
 * {@code balance-reverse} cross-axis flip, and the {@code flex-line-count} division of definite
 * cross-axis available space. Greedy {@code WRAP}/{@code WRAP_REVERSE} line breaking is pinned
 * unchanged.</p>
 *
 * <p>Expected numbers mirror the upstream generated fixtures {@code balance_row},
 * {@code balance_row_line_count}, {@code balance_row_zero_sized_items}, {@code balance_row_percent},
 * {@code balance_column} and {@code balance_wrap_reverse_row}, plus the nested/intrinsic scenarios
 * where balanced line breaking cannot apply because the container's main size is indefinite.</p>
 */
public class FlexBalanceP7Test {

    private static final float EPS = 0.001f;

    // ------------------------------------------------------------------
    // Style / tree helpers
    // ------------------------------------------------------------------

    private static TaffyStyle sized(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(
            Float.isNaN(width) ? TaffyDimension.AUTO : TaffyDimension.length(width),
            Float.isNaN(height) ? TaffyDimension.AUTO : TaffyDimension.length(height)
        );
        return style;
    }

    private static TaffyStyle flexRow(float width, FlexWrap wrap) {
        TaffyStyle style = sized(width, Float.NaN);
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.ROW;
        style.flexWrap = wrap;
        return style;
    }

    private static TaffyStyle flexColumn(float height, FlexWrap wrap) {
        TaffyStyle style = sized(Float.NaN, height);
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.COLUMN;
        style.flexWrap = wrap;
        return style;
    }

    private static Layout layoutOf(TaffyTree tree, NodeId node) {
        return tree.getLayout(node);
    }

    private static void assertSize(Layout layout, float width, float height, String message) {
        assertEquals(width, layout.size().width, EPS, message + " width");
        assertEquals(height, layout.size().height, EPS, message + " height");
    }

    private static void assertLocation(Layout layout, float x, float y, String message) {
        assertEquals(x, layout.location().x, EPS, message + " x");
        assertEquals(y, layout.location().y, EPS, message + " y");
    }

    /** Builds a row flex container of the given width with fixed-size children and lays it out. */
    private static NodeId rowWithChildren(TaffyTree tree, float containerWidth, FlexWrap wrap,
                                          float[] widths, float[] heights) {
        NodeId[] children = new NodeId[widths.length];
        for (int index = 0; index < widths.length; index++) {
            children[index] = tree.newLeaf(sized(widths[index], heights[index]));
        }
        TaffyStyle rootStyle = flexRow(containerWidth, wrap);
        NodeId root = tree.newWithChildren(rootStyle, children);
        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(500), AvailableSpace.definite(500)));
        return root;
    }

    // ==================================================================
    // FlexWrap value type
    // ==================================================================

    @Test
    @DisplayName("FL-21: balance values are appended so the old ordinals do not move")
    void balanceValuesAreAppended() {
        FlexWrap[] values = FlexWrap.values();
        assertEquals(5, values.length, "value count");
        assertEquals(FlexWrap.NO_WRAP, values[0], "ordinal 0");
        assertEquals(FlexWrap.WRAP, values[1], "ordinal 1");
        assertEquals(FlexWrap.WRAP_REVERSE, values[2], "ordinal 2");
        assertEquals(FlexWrap.BALANCE, values[3], "ordinal 3");
        assertEquals(FlexWrap.BALANCE_REVERSE, values[4], "ordinal 4");
        assertEquals(0, FlexWrap.NO_WRAP.ordinal(), "NO_WRAP ordinal");
        assertEquals(1, FlexWrap.WRAP.ordinal(), "WRAP ordinal");
        assertEquals(2, FlexWrap.WRAP_REVERSE.ordinal(), "WRAP_REVERSE ordinal");
    }

    @Test
    @DisplayName("FL-21: wrap classification helpers match upstream is_multi_line/is_reverse/is_balance")
    void wrapClassificationHelpers() {
        assertFalse(FlexWrap.NO_WRAP.isMultiLine(), "NO_WRAP is not multi-line");
        assertTrue(FlexWrap.WRAP.isMultiLine(), "WRAP is multi-line");
        assertTrue(FlexWrap.WRAP_REVERSE.isMultiLine(), "WRAP_REVERSE is multi-line");
        assertTrue(FlexWrap.BALANCE.isMultiLine(), "BALANCE is multi-line");
        assertTrue(FlexWrap.BALANCE_REVERSE.isMultiLine(), "BALANCE_REVERSE is multi-line");

        assertFalse(FlexWrap.NO_WRAP.isReverse(), "NO_WRAP is not reverse");
        assertFalse(FlexWrap.WRAP.isReverse(), "WRAP is not reverse");
        assertTrue(FlexWrap.WRAP_REVERSE.isReverse(), "WRAP_REVERSE is reverse");
        assertFalse(FlexWrap.BALANCE.isReverse(), "BALANCE is not reverse");
        assertTrue(FlexWrap.BALANCE_REVERSE.isReverse(), "BALANCE_REVERSE is reverse");

        assertFalse(FlexWrap.NO_WRAP.isBalance(), "NO_WRAP is not balance");
        assertFalse(FlexWrap.WRAP.isBalance(), "WRAP is not balance");
        assertFalse(FlexWrap.WRAP_REVERSE.isBalance(), "WRAP_REVERSE is not balance");
        assertTrue(FlexWrap.BALANCE.isBalance(), "BALANCE is balance");
        assertTrue(FlexWrap.BALANCE_REVERSE.isBalance(), "BALANCE_REVERSE is balance");
    }

    // ==================================================================
    // Style field
    // ==================================================================

    @Test
    @DisplayName("FL-21: flexLineCount defaults to unset (0) and copy() preserves it")
    void flexLineCountDefaultsAndCopies() {
        TaffyStyle style = new TaffyStyle();
        assertEquals(0, style.flexLineCount, "default field");
        assertEquals(0, style.getFlexLineCount(), "default getter");

        assertSame(style, style.setFlexLineCount(3), "setter returns this");
        assertEquals(3, style.flexLineCount, "field after set");
        assertEquals(3, style.getFlexLineCount(), "getter after set");

        TaffyStyle copy = style.copy();
        assertEquals(3, copy.flexLineCount, "copy preserves flexLineCount");
        copy.flexLineCount = 5;
        assertEquals(3, style.flexLineCount, "copy is independent");
    }

    // ==================================================================
    // Greedy regression (old values must not move)
    // ==================================================================

    @Test
    @DisplayName("FL-21: WRAP and WRAP_REVERSE keep the greedy single-pass line breaking")
    void greedyWrapValuesAreUnchanged() {
        float[] widths = {30, 30, 30, 30};
        float[] heights = {20, 20, 20, 20};

        TaffyTree wrap = new TaffyTree();
        NodeId wrapRoot = rowWithChildren(wrap, 100, FlexWrap.WRAP, widths, heights);
        assertSize(layoutOf(wrap, wrapRoot), 100, 40, "WRAP container");
        assertLocation(layoutOf(wrap, wrap.getChildren(wrapRoot).get(0)), 0, 0, "WRAP item 0");
        assertLocation(layoutOf(wrap, wrap.getChildren(wrapRoot).get(1)), 30, 0, "WRAP item 1");
        assertLocation(layoutOf(wrap, wrap.getChildren(wrapRoot).get(2)), 60, 0, "WRAP item 2");
        assertLocation(layoutOf(wrap, wrap.getChildren(wrapRoot).get(3)), 0, 20, "WRAP item 3");

        TaffyTree reverse = new TaffyTree();
        NodeId reverseRoot = rowWithChildren(reverse, 100, FlexWrap.WRAP_REVERSE, widths, heights);
        assertSize(layoutOf(reverse, reverseRoot), 100, 40, "WRAP_REVERSE container");
        assertLocation(layoutOf(reverse, reverse.getChildren(reverseRoot).get(0)), 0, 20, "WRAP_REVERSE item 0");
        assertLocation(layoutOf(reverse, reverse.getChildren(reverseRoot).get(1)), 30, 20, "WRAP_REVERSE item 1");
        assertLocation(layoutOf(reverse, reverse.getChildren(reverseRoot).get(2)), 60, 20, "WRAP_REVERSE item 2");
        assertLocation(layoutOf(reverse, reverse.getChildren(reverseRoot).get(3)), 0, 0, "WRAP_REVERSE item 3");
    }

    @Test
    @DisplayName("FL-21: an explicit flexLineCount of 1 leaves greedy line breaking unchanged")
    void greedyLineCountOneMatchesUnset() {
        float[] widths = {31, 32, 33, 34};
        float[] heights = {30, 30, 30, 30};

        TaffyTree unset = new TaffyTree();
        NodeId unsetRoot = rowWithChildren(unset, 100, FlexWrap.WRAP, widths, heights);

        TaffyTree explicit = new TaffyTree();
        TaffyStyle rootStyle = flexRow(100, FlexWrap.WRAP);
        rootStyle.flexLineCount = 1;
        NodeId[] children = new NodeId[widths.length];
        for (int index = 0; index < widths.length; index++) {
            children[index] = explicit.newLeaf(sized(widths[index], heights[index]));
        }
        NodeId explicitRoot = explicit.newWithChildren(rootStyle, children);
        explicit.computeLayout(explicitRoot, new TaffySize<>(AvailableSpace.definite(500), AvailableSpace.definite(500)));

        assertEquals(layoutOf(unset, unsetRoot).size().height, layoutOf(explicit, explicitRoot).size().height, EPS,
            "container height");
        for (int index = 0; index < widths.length; index++) {
            Layout expected = layoutOf(unset, unset.getChildren(unsetRoot).get(index));
            Layout actual = layoutOf(explicit, explicit.getChildren(explicitRoot).get(index));
            assertEquals(expected.location().x, actual.location().x, EPS, "item " + index + " x");
            assertEquals(expected.location().y, actual.location().y, EPS, "item " + index + " y");
        }
    }

    // ==================================================================
    // Balanced row breaking
    // ==================================================================

    @Test
    @DisplayName("FL-21: balance_row fixture splits 31/32/33/34 into two balanced lines")
    void balanceRowMatchesUpstreamFixture() {
        float[] widths = {31, 32, 33, 34};
        float[] heights = {30, 30, 30, 30};

        TaffyTree greedy = new TaffyTree();
        NodeId greedyRoot = rowWithChildren(greedy, 100, FlexWrap.WRAP, widths, heights);
        assertLocation(layoutOf(greedy, greedy.getChildren(greedyRoot).get(2)), 63, 0, "greedy item 2 stays on line 1");
        assertLocation(layoutOf(greedy, greedy.getChildren(greedyRoot).get(3)), 0, 30, "greedy item 3 wraps");

        TaffyTree tree = new TaffyTree();
        NodeId root = rowWithChildren(tree, 100, FlexWrap.BALANCE, widths, heights);
        assertSize(layoutOf(tree, root), 100, 60, "balanced container");
        List<NodeId> children = tree.getChildren(root);
        assertLocation(layoutOf(tree, children.get(0)), 0, 0, "balanced item 0");
        assertLocation(layoutOf(tree, children.get(1)), 31, 0, "balanced item 1");
        assertLocation(layoutOf(tree, children.get(2)), 0, 30, "balanced item 2");
        assertLocation(layoutOf(tree, children.get(3)), 33, 30, "balanced item 3");
    }

    @Test
    @DisplayName("FL-21: balance_row line sizes are equalized (63/67 instead of greedy 96/34)")
    void balanceEqualizesLineSizes() {
        float[] widths = {60, 20, 20, 20};
        float[] heights = {10, 40, 10, 10};

        TaffyTree greedy = new TaffyTree();
        NodeId greedyRoot = rowWithChildren(greedy, 100, FlexWrap.WRAP, widths, heights);
        assertLocation(layoutOf(greedy, greedy.getChildren(greedyRoot).get(3)), 0, 40, "greedy puts item 3 on line 2");

        TaffyTree tree = new TaffyTree();
        NodeId root = rowWithChildren(tree, 100, FlexWrap.BALANCE, widths, heights);
        List<NodeId> children = tree.getChildren(root);
        // 60 / 20+20+20 balances to line sizes 60/60 (squared error 7200) instead of 80/40 or 100/20.
        assertLocation(layoutOf(tree, children.get(0)), 0, 0, "balanced item 0 alone on line 1");
        assertLocation(layoutOf(tree, children.get(1)), 0, 10, "balanced item 1 starts line 2");
        assertLocation(layoutOf(tree, children.get(2)), 20, 10, "balanced item 2");
        assertLocation(layoutOf(tree, children.get(3)), 40, 10, "balanced item 3");
        assertSize(layoutOf(tree, root), 100, 50, "balanced container cross size");
    }

    @Test
    @DisplayName("FL-21: balance accounts for the main-axis gap")
    void balanceWithMainAxisGap() {
        float[] widths = {25, 25, 25, 25};
        float[] heights = {20, 20, 20, 20};

        TaffyTree tree = new TaffyTree();
        NodeId[] children = new NodeId[4];
        for (int index = 0; index < 4; index++) {
            children[index] = tree.newLeaf(sized(widths[index], heights[index]));
        }
        TaffyStyle rootStyle = flexRow(100, FlexWrap.BALANCE);
        rootStyle.gap = TaffySize.all(LengthPercentage.length(10));
        NodeId root = tree.newWithChildren(rootStyle, children);
        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(500), AvailableSpace.definite(500)));

        // Greedy fits 3 items (25 + 10 + 25 + 10 + 25 = 95); balance splits 2/2 (60/60).
        assertSize(layoutOf(tree, root), 100, 50, "container");
        assertLocation(layoutOf(tree, children[0]), 0, 0, "item 0");
        assertLocation(layoutOf(tree, children[1]), 35, 0, "item 1");
        assertLocation(layoutOf(tree, children[2]), 0, 30, "item 2");
        assertLocation(layoutOf(tree, children[3]), 35, 30, "item 3");
    }

    @Test
    @DisplayName("FL-21: balance-reverse keeps the balanced partition and flips the cross axis")
    void balanceReverseFlipsCrossAxis() {
        float[] widths = {31, 32, 33, 34};
        float[] heights = {30, 30, 30, 30};

        TaffyTree tree = new TaffyTree();
        NodeId root = rowWithChildren(tree, 100, FlexWrap.BALANCE_REVERSE, widths, heights);
        assertSize(layoutOf(tree, root), 100, 60, "container");
        List<NodeId> children = tree.getChildren(root);
        assertLocation(layoutOf(tree, children.get(0)), 0, 30, "item 0 on the lower line");
        assertLocation(layoutOf(tree, children.get(1)), 31, 30, "item 1 on the lower line");
        assertLocation(layoutOf(tree, children.get(2)), 0, 0, "item 2 on the upper line");
        assertLocation(layoutOf(tree, children.get(3)), 33, 0, "item 3 on the upper line");
    }

    // ==================================================================
    // flex-line-count
    // ==================================================================

    @Test
    @DisplayName("FL-21: balance_row_line_count requests at least three balanced lines")
    void balanceRespectsFlexLineCount() {
        float[] widths = {31, 32, 33, 34};
        float[] heights = {30, 30, 30, 30};

        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = flexRow(100, FlexWrap.BALANCE);
        rootStyle.flexLineCount = 3;
        NodeId[] children = new NodeId[4];
        for (int index = 0; index < 4; index++) {
            children[index] = tree.newLeaf(sized(widths[index], heights[index]));
        }
        NodeId root = tree.newWithChildren(rootStyle, children);
        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(500), AvailableSpace.definite(500)));

        // 3 lines: 63 / 32 / 34 (squared error 6149) beats 31 / 65 / 34 (6342) and 31 / 32 / 67 (6474).
        assertSize(layoutOf(tree, root), 100, 90, "container");
        assertLocation(layoutOf(tree, children[0]), 0, 0, "item 0");
        assertLocation(layoutOf(tree, children[1]), 31, 0, "item 1");
        assertLocation(layoutOf(tree, children[2]), 0, 30, "item 2");
        assertLocation(layoutOf(tree, children[3]), 0, 60, "item 3");
    }

    @Test
    @DisplayName("FL-21: flexLineCount beyond the item count degrades to one item per line")
    void flexLineCountBeyondItemCount() {
        float[] widths = {31, 32, 33, 34};
        float[] heights = {30, 30, 30, 30};

        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = flexRow(100, FlexWrap.BALANCE);
        rootStyle.flexLineCount = 10;
        NodeId[] children = new NodeId[4];
        for (int index = 0; index < 4; index++) {
            children[index] = tree.newLeaf(sized(widths[index], heights[index]));
        }
        NodeId root = tree.newWithChildren(rootStyle, children);
        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(500), AvailableSpace.definite(500)));

        assertSize(layoutOf(tree, root), 100, 120, "container");
        for (int index = 0; index < 4; index++) {
            assertLocation(layoutOf(tree, children[index]), 0, index * 30f, "item " + index);
        }
    }

    @Test
    @DisplayName("FL-21: a zero/negative flexLineCount behaves like the initial value 1")
    void zeroOrNegativeFlexLineCountBehavesLikeOne() {
        float[] widths = {31, 32, 33, 34};
        float[] heights = {30, 30, 30, 30};

        TaffyTree zero = new TaffyTree();
        TaffyStyle zeroStyle = flexRow(100, FlexWrap.BALANCE);
        zeroStyle.flexLineCount = 0;
        NodeId[] zeroChildren = new NodeId[4];
        for (int index = 0; index < 4; index++) {
            zeroChildren[index] = zero.newLeaf(sized(widths[index], heights[index]));
        }
        NodeId zeroRoot = zero.newWithChildren(zeroStyle, zeroChildren);
        zero.computeLayout(zeroRoot, new TaffySize<>(AvailableSpace.definite(500), AvailableSpace.definite(500)));

        TaffyTree negative = new TaffyTree();
        TaffyStyle negativeStyle = flexRow(100, FlexWrap.BALANCE);
        negativeStyle.flexLineCount = -5;
        NodeId[] negativeChildren = new NodeId[4];
        for (int index = 0; index < 4; index++) {
            negativeChildren[index] = negative.newLeaf(sized(widths[index], heights[index]));
        }
        NodeId negativeRoot = negative.newWithChildren(negativeStyle, negativeChildren);
        negative.computeLayout(negativeRoot, new TaffySize<>(AvailableSpace.definite(500), AvailableSpace.definite(500)));

        assertSize(layoutOf(zero, zeroRoot), 100, 60, "zero container");
        assertSize(layoutOf(negative, negativeRoot), 100, 60, "negative container");
        for (int index = 0; index < 4; index++) {
            Layout expected = layoutOf(zero, zero.getChildren(zeroRoot).get(index));
            Layout actual = layoutOf(negative, negative.getChildren(negativeRoot).get(index));
            assertEquals(expected.location().x, actual.location().x, EPS, "item " + index + " x");
            assertEquals(expected.location().y, actual.location().y, EPS, "item " + index + " y");
        }
    }

    @Test
    @DisplayName("FL-21: flex-line-count does not change greedy line breaking")
    void greedyWrapIgnoresFlexLineCountForLineBreaking() {
        float[] widths = {31, 32, 33, 34};
        float[] heights = {30, 30, 30, 30};

        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = flexRow(100, FlexWrap.WRAP);
        rootStyle.flexLineCount = 3;
        NodeId[] children = new NodeId[4];
        for (int index = 0; index < 4; index++) {
            children[index] = tree.newLeaf(sized(widths[index], heights[index]));
        }
        NodeId root = tree.newWithChildren(rootStyle, children);
        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(500), AvailableSpace.definite(500)));

        // Still the greedy 3/1 split; flex-line-count only divides the cross measurement space.
        assertSize(layoutOf(tree, root), 100, 60, "container");
        assertLocation(layoutOf(tree, children[2]), 63, 0, "item 2 stays on line 1");
        assertLocation(layoutOf(tree, children[3]), 0, 30, "item 3 wraps");
    }

    @Test
    @DisplayName("FL-21: flex-line-count has no effect on a nowrap container")
    void flexLineCountHasNoEffectOnNowrap() {
        TaffyTree tree = new TaffyTree();
        NodeId[] children = new NodeId[3];
        for (int index = 0; index < 3; index++) {
            children[index] = tree.newLeaf(sized(20, 20));
        }
        TaffyStyle rootStyle = flexRow(20, FlexWrap.NO_WRAP);
        rootStyle.flexLineCount = 10000;
        NodeId root = tree.newWithChildren(rootStyle, children);
        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(500), AvailableSpace.definite(500)));

        assertSize(layoutOf(tree, root), 20, 20, "container");
        float totalWidth = 0;
        for (NodeId child : children) {
            totalWidth += layoutOf(tree, child).size().width;
        }
        assertEquals(20, totalWidth, EPS, "items still shrink into the single line");
        assertEquals(0, layoutOf(tree, children[0]).location().y, EPS, "no wrapping");
    }

    @Test
    @DisplayName("FL-21: flexLineCount divides definite cross space used to measure items")
    void flexLineCountDividesCrossAvailableSpaceForMeasurement() {
        List<Float> unsetHeights = measureWithLineCount(0, 0);
        assertFalse(unsetHeights.isEmpty(), "measure function must run");
        for (float height : unsetHeights) {
            assertEquals(100, height, EPS, "unset flexLineCount leaves the cross space whole");
        }

        List<Float> dividedHeights = measureWithLineCount(2, 0);
        assertFalse(dividedHeights.isEmpty(), "measure function must run");
        for (float height : dividedHeights) {
            assertEquals(50, height, EPS, "two lines divide the 100px cross space");
        }

        List<Float> gapHeights = measureWithLineCount(2, 10);
        assertFalse(gapHeights.isEmpty(), "measure function must run");
        for (float height : gapHeights) {
            assertEquals(45, height, EPS, "the cross gap is subtracted before dividing");
        }
    }

    /** Runs a 100x100 wrapping row with one measured child and returns its compute-size cross space. */
    private static List<Float> measureWithLineCount(int lineCount, float gap) {
        TaffyTree tree = new TaffyTree();
        List<Float> heights = new ArrayList<>();
        MeasureFunction measure = context -> {
            if (context.input().runMode() == RunMode.COMPUTE_SIZE) {
                heights.add(context.input().availableSpace().height.intoOption());
            }
            return LayoutOutput.fromSizes(new FloatSize(10, 10), FloatSize.zero());
        };
        TaffyStyle childStyle = sized(30, Float.NaN);
        NodeId child = tree.newLeafWithMeasureFunction(childStyle, measure);
        TaffyStyle rootStyle = sized(100, 100);
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.flexWrap = FlexWrap.WRAP;
        rootStyle.flexLineCount = lineCount;
        rootStyle.gap = TaffySize.all(LengthPercentage.length(gap));
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(500), AvailableSpace.definite(500)));
        return heights;
    }

    // ==================================================================
    // Column, zero-sized items and percentages
    // ==================================================================

    @Test
    @DisplayName("FL-21: balance_column fixture balances a column container")
    void balanceColumnMatchesUpstreamFixture() {
        TaffyTree tree = new TaffyTree();
        float[] heights = {31, 32, 33, 34};
        NodeId[] children = new NodeId[4];
        for (int index = 0; index < 4; index++) {
            children[index] = tree.newLeaf(sized(30, heights[index]));
        }
        NodeId root = tree.newWithChildren(flexColumn(100, FlexWrap.BALANCE), children);
        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(500), AvailableSpace.definite(500)));

        // Two lines of two items: line 1 is 31/32 tall, line 2 is 33/34 tall.
        assertSize(layoutOf(tree, root), 60, 100, "container");
        assertLocation(layoutOf(tree, children[0]), 0, 0, "item 0");
        assertLocation(layoutOf(tree, children[1]), 0, 31, "item 1");
        assertLocation(layoutOf(tree, children[2]), 30, 0, "item 2");
        assertLocation(layoutOf(tree, children[3]), 30, 33, "item 3");
    }

    @Test
    @DisplayName("FL-21: balance_row_zero_sized_items glues zero-sized items to the preceding line")
    void balanceZeroSizedItems() {
        TaffyTree tree = new TaffyTree();
        float[] widths = {40, 0, 40, 0, 40};
        float[] heights = {30, 10, 30, 10, 30};
        NodeId[] children = new NodeId[5];
        for (int index = 0; index < 5; index++) {
            children[index] = tree.newLeaf(sized(widths[index], heights[index]));
        }
        NodeId root = tree.newWithChildren(flexRow(100, FlexWrap.BALANCE), children);
        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(500), AvailableSpace.definite(500)));

        // Both zero-sized items stay on the first line: 40/0/40/0 then 40.
        assertSize(layoutOf(tree, root), 100, 60, "container");
        assertLocation(layoutOf(tree, children[0]), 0, 0, "item 0");
        assertLocation(layoutOf(tree, children[1]), 40, 0, "zero item 1 is glued");
        assertLocation(layoutOf(tree, children[2]), 40, 0, "item 2");
        assertLocation(layoutOf(tree, children[3]), 80, 0, "zero item 3 is glued");
        assertLocation(layoutOf(tree, children[4]), 0, 30, "item 4 wraps");
    }

    @Test
    @DisplayName("FL-21: balance_row_percent resolves percentages against the definite main size")
    void balancePercentItems() {
        TaffyTree tree = new TaffyTree();
        NodeId[] children = new NodeId[3];
        for (int index = 0; index < 3; index++) {
            TaffyStyle style = new TaffyStyle();
            style.size = new TaffySize<>(TaffyDimension.percent(0.45f), TaffyDimension.length(30));
            children[index] = tree.newLeaf(style);
        }
        NodeId root = tree.newWithChildren(flexRow(100, FlexWrap.BALANCE), children);
        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(500), AvailableSpace.definite(500)));

        assertSize(layoutOf(tree, root), 100, 60, "container");
        assertSize(layoutOf(tree, children[0]), 45, 30, "item 0");
        assertSize(layoutOf(tree, children[1]), 45, 30, "item 1");
        assertLocation(layoutOf(tree, children[2]), 0, 30, "item 2 wraps to line 2");
    }

    // ==================================================================
    // Scenarios where balanced line breaking cannot apply
    // ==================================================================

    @Test
    @DisplayName("FL-21: an auto-width balance container cannot break lines (indefinite main size)")
    void balanceRequiresDefiniteMainSize() {
        TaffyTree tree = new TaffyTree();
        NodeId[] children = new NodeId[4];
        for (int index = 0; index < 4; index++) {
            children[index] = tree.newLeaf(sized(30, 20));
        }
        NodeId inner = tree.newWithChildren(flexRow(Float.NaN, FlexWrap.BALANCE), children);
        TaffyStyle outerStyle = sized(Float.NaN, Float.NaN);
        outerStyle.display = TaffyDisplay.FLEX;
        outerStyle.flexDirection = FlexDirection.COLUMN;
        outerStyle.alignItems = AlignItems.FLEX_START;
        NodeId root = tree.newWithChildren(outerStyle, inner);
        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(100), AvailableSpace.definite(500)));

        // No size limit: the default minimum line count of 1 keeps all items on one line.
        assertSize(layoutOf(tree, inner), 120, 20, "one balanced line");
        for (int index = 0; index < 4; index++) {
            assertLocation(layoutOf(tree, children[index]), index * 30f, 0, "item " + index);
        }
    }

    @Test
    @DisplayName("FL-21: flexLineCount still applies when the main size is indefinite")
    void flexLineCountAppliesUnderIndefiniteMainSize() {
        TaffyTree tree = new TaffyTree();
        float[] widths = {60, 20, 20, 20};
        NodeId[] children = new NodeId[4];
        for (int index = 0; index < 4; index++) {
            children[index] = tree.newLeaf(sized(widths[index], 20));
        }
        TaffyStyle innerStyle = flexRow(Float.NaN, FlexWrap.BALANCE);
        innerStyle.flexLineCount = 2;
        NodeId inner = tree.newWithChildren(innerStyle, children);
        TaffyStyle outerStyle = sized(Float.NaN, Float.NaN);
        outerStyle.display = TaffyDisplay.FLEX;
        outerStyle.flexDirection = FlexDirection.COLUMN;
        outerStyle.alignItems = AlignItems.FLEX_START;
        NodeId root = tree.newWithChildren(outerStyle, inner);
        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(100), AvailableSpace.definite(500)));

        // Balanced without a size limit into two lines: 60 / 60.
        assertSize(layoutOf(tree, inner), 60, 40, "two balanced lines");
        assertLocation(layoutOf(tree, children[0]), 0, 0, "item 0");
        assertLocation(layoutOf(tree, children[1]), 0, 20, "item 1");
        assertLocation(layoutOf(tree, children[2]), 20, 20, "item 2");
        assertLocation(layoutOf(tree, children[3]), 40, 20, "item 3");
    }

    @Test
    @DisplayName("FL-21: balanced fit-content uses the balanced max-content width, not the available width")
    void balanceFitContentUsesBalancedMaxContent() {
        TaffyTree tree = new TaffyTree();
        float[] widths = {31, 32, 33, 34};
        NodeId[] children = new NodeId[4];
        for (int index = 0; index < 4; index++) {
            children[index] = tree.newLeaf(sized(widths[index], 30));
        }
        TaffyStyle innerStyle = flexRow(Float.NaN, FlexWrap.BALANCE);
        innerStyle.flexLineCount = 2;
        NodeId inner = tree.newWithChildren(innerStyle, children);
        TaffyStyle outerStyle = sized(Float.NaN, Float.NaN);
        outerStyle.display = TaffyDisplay.FLEX;
        outerStyle.flexDirection = FlexDirection.COLUMN;
        outerStyle.alignItems = AlignItems.FLEX_START;
        NodeId root = tree.newWithChildren(outerStyle, inner);
        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(100), AvailableSpace.definite(500)));

        // Two balanced lines 63 / 67: the widest line (67) is the fit-content width, not 100.
        assertSize(layoutOf(tree, inner), 67, 60, "balanced fit-content width");
        assertLocation(layoutOf(tree, children[0]), 0, 0, "item 0");
        assertLocation(layoutOf(tree, children[1]), 31, 0, "item 1");
        assertLocation(layoutOf(tree, children[2]), 0, 30, "item 2");
        assertLocation(layoutOf(tree, children[3]), 33, 30, "item 3");
    }

    @Test
    @DisplayName("FL-21: percentages of an auto-width balance container resolve to zero")
    void percentItemsUnderIndefiniteMainSizeResolveToZero() {
        TaffyTree tree = new TaffyTree();
        NodeId[] children = new NodeId[3];
        for (int index = 0; index < 3; index++) {
            TaffyStyle style = new TaffyStyle();
            style.size = new TaffySize<>(TaffyDimension.percent(0.45f), TaffyDimension.length(30));
            children[index] = tree.newLeaf(style);
        }
        TaffyStyle innerStyle = flexRow(Float.NaN, FlexWrap.BALANCE);
        innerStyle.flexLineCount = 2;
        NodeId inner = tree.newWithChildren(innerStyle, children);
        TaffyStyle outerStyle = sized(Float.NaN, Float.NaN);
        outerStyle.display = TaffyDisplay.FLEX;
        outerStyle.flexDirection = FlexDirection.COLUMN;
        outerStyle.alignItems = AlignItems.FLEX_START;
        NodeId root = tree.newWithChildren(outerStyle, inner);
        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(100), AvailableSpace.definite(500)));

        // There is no percentage basis, so the items are zero-sized; the zero-sized-item rule and
        // the first-line-bias tie-break still split them into 2 / 1 lines.
        assertSize(layoutOf(tree, inner), 0, 60, "container");
        for (NodeId child : children) {
            assertEquals(0, layoutOf(tree, child).size().width, EPS, "unresolved percentage width");
        }
        assertLocation(layoutOf(tree, children[0]), 0, 0, "item 0");
        assertLocation(layoutOf(tree, children[1]), 0, 0, "item 1");
        assertLocation(layoutOf(tree, children[2]), 0, 30, "item 2 on line 2");
    }

    @Test
    @DisplayName("FL-21: nested flex containers balance like leaves")
    void nestedFlexContainersBalanceLikeLeaves() {
        TaffyTree tree = new TaffyTree();
        NodeId[] children = new NodeId[4];
        for (int index = 0; index < 4; index++) {
            TaffyStyle nestedStyle = sized(30, 20);
            nestedStyle.display = TaffyDisplay.FLEX;
            nestedStyle.flexDirection = FlexDirection.ROW;
            NodeId grandChild = tree.newLeaf(new TaffyStyle());
            children[index] = tree.newWithChildren(nestedStyle, grandChild);
        }
        NodeId root = tree.newWithChildren(flexRow(100, FlexWrap.BALANCE), children);
        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(500), AvailableSpace.definite(500)));

        assertSize(layoutOf(tree, root), 100, 40, "container");
        assertLocation(layoutOf(tree, children[0]), 0, 0, "item 0");
        assertLocation(layoutOf(tree, children[1]), 30, 0, "item 1");
        assertLocation(layoutOf(tree, children[2]), 0, 20, "item 2");
        assertLocation(layoutOf(tree, children[3]), 30, 20, "item 3");
    }
}
