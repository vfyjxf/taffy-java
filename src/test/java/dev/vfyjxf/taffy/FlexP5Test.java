package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.AlignmentHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Flexbox algorithm wiring for the P5 alignment keywords (ST-02 safe, ST-03 self-start/self-end,
 * ST-04 normal) and regression coverage for the legacy keyword values.
 *
 * <p>The model lives in {@link AlignmentHelper}; this test drives it through the flexbox layout
 * path (flex container + child layouts) and pins the resolution rules the algorithm applies:</p>
 * <ul>
 *   <li>{@code normal} is an explicit keyword: in flex it behaves as {@code stretch} for
 *       align-items/align-self and as {@code flex-start} for justify-content; the legacy
 *       {@code AUTO} sentinel keeps its "not specified" meaning;</li>
 *   <li>{@code self-start}/{@code self-end} resolve against the item's own direction in the inline
 *       axis (the cross axis of a column container) and are never flipped by wrap-reverse;</li>
 *   <li>{@code safe *} falls back to the logical start edge when the subject overflows;</li>
 *   <li>the legacy {@code start}/{@code end}/{@code flex-start}/{@code flex-end} values keep their
 *       pre-existing numeric offsets.</li>
 * </ul>
 */
@DisplayName("Flexbox P5: safe/self/normal alignment wiring")
public class FlexP5Test {

    private static final float EPSILON = 0.01f;

    // ------------------------------------------------------------------
    // 1. explicit normal
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("normal keyword (ST-04 #1254/#1255)")
    class NormalKeyword {

        @Test
        @DisplayName("container align-items: normal stretches a row item like AUTO did")
        void containerAlignItemsNormalStretchesRowItem() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = flexRow(100f, 100f);
            containerStyle.alignItems = AlignItems.NORMAL;
            NodeId container = tree.newLeaf(containerStyle);
            NodeId child = addChild(tree, container, 20f, Float.NaN);

            tree.computeLayout(container, TaffySize.maxContent());

            Layout layout = tree.getLayout(child);
            assertEquals(100f, layout.size().height, EPSILON, "normal behaves as stretch on the cross axis");
            assertEquals(0f, layout.location().y, EPSILON);
        }

        @Test
        @DisplayName("align-self: normal overrides the container align-items: center")
        void itemAlignSelfNormalOverridesContainerCenter() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = flexRow(100f, 100f);
            containerStyle.alignItems = AlignItems.CENTER;
            NodeId container = tree.newLeaf(containerStyle);

            TaffyStyle childStyle = baseChild(20f, Float.NaN);
            childStyle.alignSelf = AlignItems.NORMAL;
            NodeId child = tree.newLeaf(childStyle);
            tree.addChild(container, child);

            tree.computeLayout(container, TaffySize.maxContent());

            Layout layout = tree.getLayout(child);
            assertEquals(100f, layout.size().height, EPSILON,
                "explicit normal must override the container align-items, unlike the AUTO sentinel");
            assertEquals(0f, layout.location().y, EPSILON);
        }

        @Test
        @DisplayName("container align-items: normal stretches a column item's width")
        void containerAlignItemsNormalStretchesColumnItem() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = new TaffyStyle();
            containerStyle.display = TaffyDisplay.FLEX;
            containerStyle.flexDirection = FlexDirection.COLUMN;
            containerStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(100f));
            containerStyle.alignItems = AlignItems.NORMAL;
            NodeId container = tree.newLeaf(containerStyle);
            NodeId child = addChild(tree, container, Float.NaN, 20f);

            tree.computeLayout(container, TaffySize.maxContent());

            Layout layout = tree.getLayout(child);
            assertEquals(100f, layout.size().width, EPSILON, "column cross axis is the width");
            assertEquals(0f, layout.location().x, EPSILON);
        }

        @Test
        @DisplayName("justify-content: normal behaves as flex-start, not start, in row-reverse")
        void justifyContentNormalIsFlexStart() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = flexRow(100f, 100f);
            containerStyle.flexDirection = FlexDirection.ROW_REVERSE;
            containerStyle.justifyContent = AlignContent.NORMAL;
            NodeId container = tree.newLeaf(containerStyle);
            NodeId child = addChild(tree, container, 20f, 20f);

            tree.computeLayout(container, TaffySize.maxContent());

            // FLEX_START in a row-reverse container packs against the right edge (offset 80).
            assertEquals(80f, tree.getLayout(child).location().x, EPSILON,
                "normal must map to flex-start (flex-relative), not to start");
        }

        @Test
        @DisplayName("align-content: normal stretches flex lines like AUTO did")
        void alignContentNormalStretchesLines() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = flexRow(100f, 100f);
            containerStyle.flexWrap = FlexWrap.WRAP;
            containerStyle.alignItems = AlignItems.START;
            containerStyle.alignContent = AlignContent.NORMAL;
            NodeId container = tree.newLeaf(containerStyle);
            NodeId first = addChild(tree, container, 50f, 20f);
            addChild(tree, container, 50f, 20f);
            NodeId third = addChild(tree, container, 50f, 20f);
            addChild(tree, container, 50f, 20f);

            tree.computeLayout(container, TaffySize.maxContent());

            // Two lines of 20px stretch to 50px each: the second line starts at y = 50.
            assertEquals(0f, tree.getLayout(first).location().y, EPSILON);
            assertEquals(50f, tree.getLayout(third).location().y, EPSILON);
        }
    }

    // ------------------------------------------------------------------
    // 2. self-start / self-end
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("self-start/self-end (ST-03 #1077)")
    class SelfRelative {

        @Test
        @DisplayName("row cross axis is the block axis: self-* ignores the item direction")
        void rowSelfKeywordsIgnoreDirection() {
            assertEquals(0f, rowSelfY(AlignItems.SELF_START, TaffyDirection.RTL), EPSILON);
            assertEquals(80f, rowSelfY(AlignItems.SELF_END, TaffyDirection.RTL), EPSILON);
            assertEquals(0f, rowSelfY(AlignItems.SELF_START, TaffyDirection.LTR), EPSILON);
            assertEquals(80f, rowSelfY(AlignItems.SELF_END, TaffyDirection.LTR), EPSILON);
        }

        @Test
        @DisplayName("self-* are not flipped by wrap-reverse (unlike flex-start/flex-end)")
        void rowSelfKeywordsIgnoreWrapReverse() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = flexRow(100f, 100f);
            containerStyle.flexWrap = FlexWrap.WRAP_REVERSE;
            containerStyle.alignItems = AlignItems.SELF_START;
            NodeId container = tree.newLeaf(containerStyle);
            NodeId start = addChild(tree, container, 20f, 20f);

            tree.computeLayout(container, TaffySize.maxContent());
            assertEquals(0f, tree.getLayout(start).location().y, EPSILON,
                "self-start is physical start here; wrap-reverse must not flip it");

            TaffyTree tree2 = new TaffyTree();
            TaffyStyle containerStyle2 = flexRow(100f, 100f);
            containerStyle2.flexWrap = FlexWrap.WRAP_REVERSE;
            containerStyle2.alignItems = AlignItems.SELF_END;
            NodeId container2 = tree2.newLeaf(containerStyle2);
            NodeId end = addChild(tree2, container2, 20f, 20f);

            tree2.computeLayout(container2, TaffySize.maxContent());
            assertEquals(80f, tree2.getLayout(end).location().y, EPSILON);
        }

        @Test
        @DisplayName("column cross axis is the inline axis: self-start follows the item direction")
        void columnSelfStartFollowsItemDirection() {
            // LTR container: an LTR item's own inline start is the left edge, an RTL item's is right.
            assertEquals(0f, columnChildX(TaffyDirection.LTR, TaffyDirection.LTR, AlignItems.SELF_START), EPSILON);
            assertEquals(80f, columnChildX(TaffyDirection.LTR, TaffyDirection.RTL, AlignItems.SELF_START), EPSILON);
            assertEquals(80f, columnChildX(TaffyDirection.LTR, TaffyDirection.LTR, AlignItems.SELF_END), EPSILON);
            assertEquals(0f, columnChildX(TaffyDirection.LTR, TaffyDirection.RTL, AlignItems.SELF_END), EPSILON);
        }

        @Test
        @DisplayName("column self-start follows the container direction too (RTL mirrors the cross axis)")
        void columnSelfStartFollowsContainerDirection() {
            // RTL container: the logical start edge is physically the right edge. An RTL item's own
            // start matches it (80); an LTR item's own start is the left edge (0).
            assertEquals(80f, columnChildX(TaffyDirection.RTL, TaffyDirection.RTL, AlignItems.SELF_START), EPSILON);
            assertEquals(0f, columnChildX(TaffyDirection.RTL, TaffyDirection.LTR, AlignItems.SELF_START), EPSILON);
            assertEquals(0f, columnChildX(TaffyDirection.RTL, TaffyDirection.RTL, AlignItems.SELF_END), EPSILON);
            assertEquals(80f, columnChildX(TaffyDirection.RTL, TaffyDirection.LTR, AlignItems.SELF_END), EPSILON);
        }
    }

    // ------------------------------------------------------------------
    // 3. safe overflow fallback
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("safe overflow-position fallback (ST-02 #952)")
    class SafeOverflow {

        @Test
        @DisplayName("safe center on an overflowing cross-axis item falls back to start")
        void safeCenterFallsBackToStartOnOverflow() {
            float safe = crossY(100f, 50f, 150f, AlignItems.SAFE_CENTER);
            float unsafe = crossY(100f, 50f, 150f, AlignItems.CENTER);
            assertEquals(0f, safe, EPSILON, "safe alignment anchors the overflowing item at start");
            assertEquals(-25f, unsafe, EPSILON, "plain center keeps the negative (overflowing) offset");
        }

        @Test
        @DisplayName("safe end on an overflowing cross-axis item falls back to start")
        void safeEndFallsBackToStartOnOverflow() {
            assertEquals(0f, crossY(100f, 50f, 150f, AlignItems.SAFE_END), EPSILON);
            assertEquals(-50f, crossY(100f, 50f, 150f, AlignItems.END), EPSILON);
        }

        @Test
        @DisplayName("safe alignment that fits keeps its requested position")
        void safeAlignmentWithoutOverflowIsUnchanged() {
            assertEquals(25f, crossY(100f, 50f, 50f, AlignItems.SAFE_CENTER), EPSILON);
            assertEquals(50f, crossY(100f, 50f, 50f, AlignItems.SAFE_END), EPSILON);
        }

        @Test
        @DisplayName("safe self-start resolves against the item direction before the overflow fallback")
        void safeSelfStartUsesItemDirectionThenStart() {
            // RTL container + LTR item: self-start resolves to end; overflowing it falls back to
            // the logical start edge (the right edge), so x = 100 - 150 = -50. Plain self-start
            // stays at the end edge (x = 0).
            assertEquals(-50f, columnChildX(TaffyDirection.RTL, TaffyDirection.LTR,
                AlignItems.SAFE_SELF_START, 150f), EPSILON);
            assertEquals(0f, columnChildX(TaffyDirection.RTL, TaffyDirection.LTR,
                AlignItems.SELF_START, 150f), EPSILON);
        }

        @Test
        @DisplayName("justify-content: safe center falls back to start when the items overflow")
        void safeJustifyCenterFallsBackToStartOnOverflow() {
            assertEquals(0f, justifyX(AlignContent.SAFE_CENTER, false, 0), EPSILON,
                "first overflowing item is anchored at the main-axis start");
            assertEquals(80f, justifyX(AlignContent.SAFE_CENTER, false, 1), EPSILON);
            assertEquals(-30f, justifyX(AlignContent.CENTER, false, 0), EPSILON,
                "plain center keeps the negative offset");
            assertEquals(50f, justifyX(AlignContent.CENTER, false, 1), EPSILON);
        }

        @Test
        @DisplayName("justify-content: safe flex-start falls back to physical start in row-reverse")
        void safeJustifyFlexStartFallsBackToStartInRowReverse() {
            // Row-reverse: flex-start would be the right edge (offset 80); safe start is the
            // physical left edge (offset 0).
            // Two overflowing 80px items are laid out in reverse order in a row-reverse
            // container, so the first flex item starts one item width after the initial offset.
            assertEquals(20f, justifyX(AlignContent.FLEX_START, true, 0), EPSILON);
            assertEquals(80f, justifyX(AlignContent.SAFE_FLEX_START, true, 0), EPSILON);
        }

        @Test
        @DisplayName("align-content: safe end falls back to start when the lines overflow")
        void safeAlignContentEndFallsBackToStartOnOverflow() {
            TwoItems safe = wrappedTwoLines(AlignContent.SAFE_END);
            TwoItems plainEnd = wrappedTwoLines(AlignContent.END);

            assertEquals(0f, safe.tree.getLayout(safe.first).location().y, EPSILON,
                "safe end anchors the first overflowing line at start");
            assertEquals(-60f, plainEnd.tree.getLayout(plainEnd.first).location().y, EPSILON,
                "plain end keeps the negative offset");
        }

        @Test
        @DisplayName("align-content: safe end equals start under wrap-reverse")
        void safeAlignContentUnderWrapReverseMatchesStart() {
            TwoItems safe = wrappedTwoLines(AlignContent.SAFE_END, FlexWrap.WRAP_REVERSE);
            TwoItems start = wrappedTwoLines(AlignContent.START, FlexWrap.WRAP_REVERSE);

            assertEquals(start.tree.getLayout(start.first).location().y,
                safe.tree.getLayout(safe.first).location().y, EPSILON,
                "safe end falls back to the same start edge as START in a wrap-reverse container");
        }
    }

    // ------------------------------------------------------------------
    // 4. legacy keyword regression (numeric values must not change)
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("legacy keyword regression")
    class LegacyRegression {

        @Test
        @DisplayName("AUTO keeps its not-specified meaning (stretch + flex-start)")
        void autoSentinelIsUnchanged() {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = flexRow(100f, 100f);
            NodeId container = tree.newLeaf(containerStyle);
            NodeId child = addChild(tree, container, 20f, Float.NaN);

            tree.computeLayout(container, TaffySize.maxContent());

            Layout layout = tree.getLayout(child);
            assertEquals(100f, layout.size().height, EPSILON, "flex align-items default is stretch");
            assertEquals(0f, layout.location().x, EPSILON, "flex justify-content default is flex-start");
        }

        // The fixture has two non-shrinking 80px items in a 100px container: free space is -60.
        // Distribution keywords degrade to their positional fallback on overflow. In row-reverse
        // the two items are laid out in reverse order, so the first flex item starts one item
        // width after the initial offset.
        static Stream<Arguments> legacyJustifyCases() {
            return Stream.of(
                Arguments.of(AlignContent.FLEX_START, false, 0f),
                Arguments.of(AlignContent.FLEX_END, false, -60f),
                Arguments.of(AlignContent.CENTER, false, -30f),
                Arguments.of(AlignContent.START, false, 0f),
                Arguments.of(AlignContent.END, false, -60f),
                Arguments.of(AlignContent.SPACE_BETWEEN, false, 0f),
                Arguments.of(AlignContent.SPACE_AROUND, false, 0f),
                Arguments.of(AlignContent.SPACE_EVENLY, false, 0f),
                Arguments.of(AlignContent.STRETCH, false, 0f),
                Arguments.of(AlignContent.FLEX_START, true, 20f),
                Arguments.of(AlignContent.FLEX_END, true, 80f),
                Arguments.of(AlignContent.CENTER, true, 50f),
                Arguments.of(AlignContent.START, true, 80f),
                Arguments.of(AlignContent.END, true, 20f),
                Arguments.of(AlignContent.SPACE_BETWEEN, true, 80f),
                Arguments.of(AlignContent.SPACE_AROUND, true, 80f),
                Arguments.of(AlignContent.SPACE_EVENLY, true, 80f),
                Arguments.of(AlignContent.STRETCH, true, 20f)
            );
        }

        @ParameterizedTest(name = "justify-content {0}, row-reverse={1} -> x={2}")
        @MethodSource("legacyJustifyCases")
        @DisplayName("legacy justify-content values keep their numeric offsets")
        void legacyJustifyValuesAreStable(AlignContent justify, boolean rowReverse, float expectedX) {
            assertEquals(expectedX, justifyX(justify, rowReverse, 0), EPSILON);
        }

        static Stream<Arguments> legacyAlignItemsCases() {
            return Stream.of(
                Arguments.of(FlexWrap.WRAP, AlignItems.START, 0f),
                Arguments.of(FlexWrap.WRAP, AlignItems.END, 80f),
                Arguments.of(FlexWrap.WRAP, AlignItems.FLEX_START, 0f),
                Arguments.of(FlexWrap.WRAP, AlignItems.FLEX_END, 80f),
                Arguments.of(FlexWrap.WRAP_REVERSE, AlignItems.START, 0f),
                Arguments.of(FlexWrap.WRAP_REVERSE, AlignItems.END, 80f),
                Arguments.of(FlexWrap.WRAP_REVERSE, AlignItems.FLEX_START, 80f),
                Arguments.of(FlexWrap.WRAP_REVERSE, AlignItems.FLEX_END, 0f)
            );
        }

        @ParameterizedTest(name = "wrap={0}, align-items={1} -> y={2}")
        @MethodSource("legacyAlignItemsCases")
        @DisplayName("legacy align-items start/end vs flex-start/flex-end are stable")
        void legacyAlignItemsValuesAreStable(FlexWrap wrap, AlignItems alignItems, float expectedY) {
            TaffyTree tree = new TaffyTree();
            TaffyStyle containerStyle = flexRow(100f, 100f);
            containerStyle.flexWrap = wrap;
            containerStyle.alignItems = alignItems;
            NodeId container = tree.newLeaf(containerStyle);
            NodeId child = addChild(tree, container, 20f, 20f);

            tree.computeLayout(container, TaffySize.maxContent());

            assertEquals(expectedY, tree.getLayout(child).location().y, EPSILON);
        }

        @Test
        @DisplayName("legacy align-content end keeps its negative overflow offset")
        void legacyAlignContentEndIsStable() {
            TwoItems end = wrappedTwoLines(AlignContent.END);
            assertEquals(-60f, end.tree.getLayout(end.first).location().y, EPSILON);
        }
    }

    // ------------------------------------------------------------------
    // fixtures / helpers
    // ------------------------------------------------------------------

    private record TwoItems(TaffyTree tree, NodeId first, NodeId second) {
    }

    private static TaffyStyle flexRow(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.ROW;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle baseChild(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(
            Float.isNaN(width) ? TaffyDimension.AUTO : TaffyDimension.length(width),
            Float.isNaN(height) ? TaffyDimension.AUTO : TaffyDimension.length(height));
        return style;
    }

    private static NodeId addChild(TaffyTree tree, NodeId parent, float width, float height) {
        NodeId child = tree.newLeaf(baseChild(width, height));
        tree.addChild(parent, child);
        return child;
    }

    private static float rowSelfY(AlignItems alignSelf, TaffyDirection itemDirection) {
        TaffyTree tree = new TaffyTree();
        TaffyStyle containerStyle = flexRow(100f, 100f);
        containerStyle.direction = TaffyDirection.RTL;
        containerStyle.alignItems = AlignItems.CENTER;
        NodeId container = tree.newLeaf(containerStyle);

        TaffyStyle childStyle = baseChild(20f, 20f);
        childStyle.direction = itemDirection;
        childStyle.alignSelf = alignSelf;
        NodeId child = tree.newLeaf(childStyle);
        tree.addChild(container, child);

        tree.computeLayout(container, TaffySize.maxContent());
        return tree.getLayout(child).location().y;
    }

    private static float columnChildX(TaffyDirection containerDirection,
                                      TaffyDirection itemDirection,
                                      AlignItems alignSelf) {
        return columnChildX(containerDirection, itemDirection, alignSelf, 20f);
    }

    private static float columnChildX(TaffyDirection containerDirection,
                                      TaffyDirection itemDirection,
                                      AlignItems alignSelf,
                                      float childWidth) {
        TaffyTree tree = new TaffyTree();
        TaffyStyle containerStyle = new TaffyStyle();
        containerStyle.display = TaffyDisplay.FLEX;
        containerStyle.flexDirection = FlexDirection.COLUMN;
        containerStyle.direction = containerDirection;
        containerStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(100f));
        containerStyle.alignItems = AlignItems.CENTER;
        NodeId container = tree.newLeaf(containerStyle);

        TaffyStyle childStyle = baseChild(childWidth, 20f);
        childStyle.direction = itemDirection;
        childStyle.alignSelf = alignSelf;
        NodeId child = tree.newLeaf(childStyle);
        tree.addChild(container, child);

        tree.computeLayout(container, TaffySize.maxContent());
        return tree.getLayout(child).location().x;
    }

    /**
     * Lays out a row flex container of {@code containerHeight} with one child of a definite cross
     * size (height) and returns the child's unrounded y offset.
     */
    private static float crossY(float containerHeight, float width, float height, AlignItems alignSelf) {
        TaffyTree tree = new TaffyTree();
        TaffyStyle containerStyle = flexRow(100f, containerHeight);
        NodeId container = tree.newLeaf(containerStyle);
        TaffyStyle childStyle = baseChild(width, height);
        childStyle.alignSelf = alignSelf;
        NodeId child = tree.newLeaf(childStyle);
        tree.addChild(container, child);

        tree.computeLayout(container, TaffySize.maxContent());
        return tree.getLayout(child).location().y;
    }

    /**
     * Two 80px-wide, non-shrinking items in a 100px-wide row flex container: the items overflow the
     * main axis by 60px.
     */
    private static float justifyX(AlignContent justify, boolean rowReverse, int childIndex) {
        TaffyTree tree = new TaffyTree();
        TaffyStyle containerStyle = flexRow(100f, 100f);
        if (rowReverse) {
            containerStyle.flexDirection = FlexDirection.ROW_REVERSE;
        }
        containerStyle.justifyContent = justify;
        NodeId container = tree.newLeaf(containerStyle);

        NodeId first = null;
        NodeId second = null;
        for (int i = 0; i < 2; i++) {
            TaffyStyle childStyle = baseChild(80f, 50f);
            childStyle.flexShrink = 0f;
            NodeId node = tree.newLeaf(childStyle);
            tree.addChild(container, node);
            if (i == 0) {
                first = node;
            } else {
                second = node;
            }
        }

        tree.computeLayout(container, TaffySize.maxContent());
        return tree.getLayout(childIndex == 0 ? first : second).location().x;
    }

    private static TwoItems wrappedTwoLines(AlignContent alignContent) {
        return wrappedTwoLines(alignContent, FlexWrap.WRAP);
    }

    /**
     * Two 60x80 items in a 100x100 wrapping row container: each item gets its own line, so the
     * lines overflow the cross axis by 60px.
     */
    private static TwoItems wrappedTwoLines(AlignContent alignContent, FlexWrap wrap) {
        TaffyTree tree = new TaffyTree();
        TaffyStyle containerStyle = flexRow(100f, 100f);
        containerStyle.flexWrap = wrap;
        containerStyle.alignItems = AlignItems.START;
        containerStyle.alignContent = alignContent;
        NodeId container = tree.newLeaf(containerStyle);
        NodeId first = addChild(tree, container, 60f, 80f);
        NodeId second = addChild(tree, container, 60f, 80f);

        tree.computeLayout(container, TaffySize.maxContent());
        return new TwoItems(tree, first, second);
    }
}
