package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.style.Clear;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.util.BfcSlot;
import dev.vfyjxf.taffy.util.BlockContext;
import dev.vfyjxf.taffy.util.FloatContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P7/FT-01 placement tests for {@link FloatContext#placeFloatedBox}.
 *
 * <p><b>Scope.</b> These tests drive the float context directly; no block layout, no
 * {@code BlockComputer} integration. They cover the CSS 2.2 §9.5.1 placement rules ported from
 * upstream taffy's {@code place_floated_box} (#866), including the follow-up fixes: the first
 * float is not clamped to the BFC top (#1304), zero-width/zero-height floats are recorded in
 * segments (#1056/#1062/#1387), clearance uses zero-sized float bottoms, and floats may
 * overflow the BFC trailing edge when no float constrains them (rules 1/7).</p>
 */
public class FloatPlacementP7Test {

    private static final float EPSILON = 0.001f;

    private static FloatContext context(float width) {
        FloatContext context = new FloatContext();
        context.setWidth(width);
        return context;
    }

    private static FloatContext.FloatPlacement place(
        FloatContext context,
        float width,
        float height,
        float minY,
        FloatContext.Direction direction,
        Clear clear
    ) {
        return context.placeFloatedBox(width, height, minY, 0.0f, 0.0f, direction, clear);
    }

    private static FloatContext.FloatPlacement place(
        FloatContext context,
        float width,
        float height,
        float minY,
        float leftInset,
        float rightInset,
        FloatContext.Direction direction,
        Clear clear
    ) {
        return context.placeFloatedBox(width, height, minY, leftInset, rightInset, direction, clear);
    }

    // === Basic placement ===

    @Test
    @DisplayName("a left float is placed at the BFC left edge")
    void leftFloatAtBfcStart() {
        FloatContext context = context(200.0f);

        FloatContext.FloatPlacement placement =
            place(context, 50.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);

        assertTrue(placement.placed());
        assertEquals(0.0f, placement.x(), EPSILON);
        assertEquals(0.0f, placement.y(), EPSILON);
        assertEquals(1, context.leftFloats().size());
        assertTrue(context.rightFloats().isEmpty());
        assertTrue(context.hasFloats());
        assertEquals(50.0f, context.regionAt(10.0f).leftInset(), EPSILON);
        assertEquals(0.0f, context.regionAt(10.0f).rightInset(), EPSILON);
    }

    @Test
    @DisplayName("a right float is placed at the BFC right edge")
    void rightFloatAtBfcEnd() {
        FloatContext context = context(200.0f);

        FloatContext.FloatPlacement placement =
            place(context, 50.0f, 20.0f, 0.0f, FloatContext.Direction.RIGHT, Clear.NONE);

        assertTrue(placement.placed());
        assertEquals(150.0f, placement.x(), EPSILON);
        assertEquals(0.0f, placement.y(), EPSILON);
        assertEquals(1, context.rightFloats().size());
        assertEquals(50.0f, context.regionAt(10.0f).rightInset(), EPSILON);
    }

    @Test
    @DisplayName("containing-block insets shift both float edges")
    void containingBlockInsetsOffsetFloats() {
        FloatContext leftContext = context(200.0f);
        FloatContext.FloatPlacement left = place(
            leftContext, 50.0f, 20.0f, 0.0f, 10.0f, 20.0f, FloatContext.Direction.LEFT, Clear.NONE);
        assertEquals(10.0f, left.x(), EPSILON);
        assertEquals(60.0f, leftContext.regionAt(10.0f).leftInset(), EPSILON);
        assertEquals(20.0f, leftContext.regionAt(10.0f).rightInset(), EPSILON);

        FloatContext rightContext = context(200.0f);
        FloatContext.FloatPlacement right = place(
            rightContext, 50.0f, 20.0f, 0.0f, 10.0f, 20.0f, FloatContext.Direction.RIGHT, Clear.NONE);
        assertEquals(130.0f, right.x(), EPSILON);
        assertEquals(10.0f, rightContext.regionAt(10.0f).leftInset(), EPSILON);
        assertEquals(70.0f, rightContext.regionAt(10.0f).rightInset(), EPSILON);
    }

    @Test
    @DisplayName("consecutive left floats share a band while they fit")
    void consecutiveLeftFloatsShareBand() {
        FloatContext context = context(200.0f);

        FloatContext.FloatPlacement first =
            place(context, 60.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        FloatContext.FloatPlacement second =
            place(context, 50.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);

        assertEquals(0.0f, first.x(), EPSILON);
        assertEquals(60.0f, second.x(), EPSILON);
        assertEquals(0.0f, second.y(), EPSILON);
        assertEquals(110.0f, context.regionAt(10.0f).leftInset(), EPSILON);
    }

    @Test
    @DisplayName("a float that no longer fits beside earlier floats wraps below them")
    void overflowingFloatWrapsBelow() {
        FloatContext context = context(200.0f);

        place(context, 150.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        FloatContext.FloatPlacement second =
            place(context, 80.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);

        assertEquals(0.0f, second.x(), EPSILON);
        assertEquals(20.0f, second.y(), EPSILON);
    }

    @Test
    @DisplayName("left and right floats do not overlap; the later one wraps below")
    void leftAndRightFloatsDoNotOverlap() {
        FloatContext context = context(200.0f);

        place(context, 120.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        FloatContext.FloatPlacement right =
            place(context, 100.0f, 20.0f, 0.0f, FloatContext.Direction.RIGHT, Clear.NONE);

        assertEquals(100.0f, right.x(), EPSILON);
        assertEquals(20.0f, right.y(), EPSILON);
    }

    @Test
    @DisplayName("a float may overflow the BFC trailing edge when no float constrains it")
    void floatMayOverflowBfcTrailingEdge() {
        FloatContext leftContext = context(200.0f);
        FloatContext.FloatPlacement left =
            place(leftContext, 250.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        assertEquals(0.0f, left.x(), EPSILON);
        assertEquals(250.0f, leftContext.regionAt(10.0f).leftInset(), EPSILON);

        FloatContext rightContext = context(200.0f);
        FloatContext.FloatPlacement right =
            place(rightContext, 250.0f, 20.0f, 0.0f, FloatContext.Direction.RIGHT, Clear.NONE);
        assertEquals(-50.0f, right.x(), EPSILON);
        assertEquals(250.0f, rightContext.regionAt(10.0f).rightInset(), EPSILON);
    }

    // === Clearance ===

    @Test
    @DisplayName("clear:left moves a float below the lowest left float")
    void clearLeftMovesBelowLeftFloat() {
        FloatContext context = context(200.0f);
        place(context, 50.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        place(context, 50.0f, 10.0f, 0.0f, FloatContext.Direction.RIGHT, Clear.NONE);

        FloatContext.FloatPlacement cleared =
            place(context, 40.0f, 10.0f, 0.0f, FloatContext.Direction.LEFT, Clear.LEFT);

        assertEquals(0.0f, cleared.x(), EPSILON);
        assertEquals(20.0f, cleared.y(), EPSILON);
    }

    @Test
    @DisplayName("clear:right moves a float below the lowest right float")
    void clearRightMovesBelowRightFloat() {
        FloatContext context = context(200.0f);
        place(context, 50.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        place(context, 50.0f, 30.0f, 0.0f, FloatContext.Direction.RIGHT, Clear.NONE);

        FloatContext.FloatPlacement cleared =
            place(context, 40.0f, 10.0f, 0.0f, FloatContext.Direction.RIGHT, Clear.RIGHT);

        assertEquals(160.0f, cleared.x(), EPSILON);
        assertEquals(30.0f, cleared.y(), EPSILON);
    }

    @Test
    @DisplayName("clear:both moves a float below the lowest float on either side")
    void clearBothUsesLowestFloatBottom() {
        FloatContext context = context(200.0f);
        place(context, 50.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        place(context, 50.0f, 30.0f, 0.0f, FloatContext.Direction.RIGHT, Clear.NONE);

        FloatContext.FloatPlacement cleared =
            place(context, 40.0f, 10.0f, 0.0f, FloatContext.Direction.LEFT, Clear.BOTH);

        assertEquals(0.0f, cleared.x(), EPSILON);
        assertEquals(30.0f, cleared.y(), EPSILON);
    }

    @Test
    @DisplayName("clear with no floats on the cleared side is a no-op")
    void clearWithoutRelevantFloatsIsNoOp() {
        FloatContext context = context(200.0f);
        place(context, 50.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);

        FloatContext.FloatPlacement right =
            place(context, 30.0f, 10.0f, 0.0f, FloatContext.Direction.RIGHT, Clear.RIGHT);

        assertEquals(170.0f, right.x(), EPSILON);
        assertEquals(0.0f, right.y(), EPSILON);
    }

    // === First float and zero-sized floats ===

    @Test
    @DisplayName("the first float is not clamped to the top of the BFC")
    void firstFloatNotClampedToBfcTop() {
        FloatContext context = context(200.0f);

        FloatContext.FloatPlacement placement =
            place(context, 50.0f, 20.0f, -30.0f, FloatContext.Direction.LEFT, Clear.NONE);

        assertEquals(0.0f, placement.x(), EPSILON);
        assertEquals(-30.0f, placement.y(), EPSILON);
        assertEquals(-30.0f, context.regionAt(-20.0f).startY(), EPSILON);
        assertEquals(50.0f, context.regionAt(-20.0f).leftInset(), EPSILON);

        FloatContext.BfcRegion above = context.regionAt(-40.0f);
        assertNotNull(above);
        assertEquals(Float.NEGATIVE_INFINITY, above.startY());
        assertEquals(-30.0f, above.endY(), EPSILON);
        assertEquals(0.0f, above.leftInset(), EPSILON);
    }

    @Test
    @DisplayName("a zero-width float is recorded and constrains later floats")
    void zeroWidthFloatRecorded() {
        FloatContext context = context(200.0f);

        FloatContext.FloatPlacement zero =
            place(context, 0.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        assertTrue(zero.placed());
        assertEquals(0.0f, zero.x(), EPSILON);
        assertEquals(1, context.leftFloats().size());
        assertEquals(0.0f, context.regionAt(10.0f).leftInset(), EPSILON);

        FloatContext.FloatPlacement next =
            place(context, 20.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        assertEquals(0.0f, next.y(), EPSILON);
        assertEquals(0.0f, next.x(), EPSILON);
    }

    @Test
    @DisplayName("a zero-height float is recorded as a zero-height segment")
    void zeroHeightFloatRecorded() {
        FloatContext context = context(200.0f);

        FloatContext.FloatPlacement zero =
            place(context, 50.0f, 0.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        assertTrue(zero.placed());
        assertEquals(1, context.leftFloats().size());
        assertNull(context.regionAt(0.0f));

        assertEquals(2, context.regions().size());
        assertEquals(0.0f, context.regions().get(1).startY(), EPSILON);
        assertEquals(0.0f, context.regions().get(1).endY(), EPSILON);
        assertEquals(50.0f, context.regions().get(1).leftInset(), EPSILON);
    }

    @Test
    @DisplayName("clearance uses zero-height float bottoms")
    void clearThresholdIncludesZeroSizedFloats() {
        FloatContext context = context(200.0f);

        place(context, 50.0f, 0.0f, 40.0f, FloatContext.Direction.LEFT, Clear.NONE);
        FloatContext.FloatPlacement cleared =
            place(context, 30.0f, 10.0f, 0.0f, FloatContext.Direction.LEFT, Clear.LEFT);

        assertEquals(40.0f, cleared.y(), EPSILON);
        assertEquals(0.0f, cleared.x(), EPSILON);
    }

    // === Validation and recording ===

    @Test
    @DisplayName("placement records one box per side and rejects invalid sizes")
    void placementRecordsAndValidates() {
        FloatContext context = context(200.0f);
        place(context, 10.0f, 10.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        place(context, 20.0f, 10.0f, 0.0f, FloatContext.Direction.RIGHT, Clear.NONE);
        assertEquals(1, context.leftFloats().size());
        assertEquals(1, context.rightFloats().size());

        assertThrows(IllegalArgumentException.class,
                     () -> context.placeFloatedBox(-1.0f, 10.0f, 0.0f, 0.0f, 0.0f,
                                                   FloatContext.Direction.LEFT, Clear.NONE));
        assertThrows(IllegalArgumentException.class,
                     () -> context.placeFloatedBox(10.0f, -1.0f, 0.0f, 0.0f, 0.0f,
                                                   FloatContext.Direction.LEFT, Clear.NONE));
        assertThrows(IllegalArgumentException.class,
                     () -> context.placeFloatedBox(Float.NaN, 10.0f, 0.0f, 0.0f, 0.0f,
                                                   FloatContext.Direction.LEFT, Clear.NONE));
        assertThrows(IllegalArgumentException.class,
                     () -> context.placeFloatedBox(Float.POSITIVE_INFINITY, 10.0f, 0.0f, 0.0f, 0.0f,
                                                   FloatContext.Direction.LEFT, Clear.NONE));
    }

    // === BFC slots ===

    private static BfcSlot findSlot(
        FloatContext context,
        float minY,
        float minHeight,
        float marginLeft,
        float marginRight,
        TaffyDirection direction
    ) {
        return context.findBfcSlot(
            minY, minHeight, 0.0f, 0.0f, marginLeft, marginRight,
            direction, Clear.NONE, FloatContext.NO_REGION);
    }

    @Test
    @DisplayName("without floats a BFC slot uses the margin-inset containing block")
    void bfcSlotWithoutFloats() {
        FloatContext context = context(200.0f);

        BfcSlot slot = context.findBfcSlot(
            10.0f, 0.0f, 10.0f, 20.0f, 5.0f, 10.0f,
            TaffyDirection.LTR, Clear.NONE, FloatContext.NO_REGION);

        assertEquals(BfcSlot.NO_SEGMENT, slot.segmentId());
        assertFalse(slot.hasSegment());
        assertEquals(15.0f, slot.x(), EPSILON);
        assertEquals(10.0f, slot.y(), EPSILON);
        assertEquals(155.0f, slot.borderWidth(), EPSILON);
        assertEquals(155.0f, slot.stretchWidth(), EPSILON);
        assertEquals(Float.POSITIVE_INFINITY, slot.height());
    }

    @Test
    @DisplayName("a leading float absorbs the box's leading margin")
    void bfcSlotAbsorbsLeadingMargin() {
        FloatContext context = context(200.0f);
        place(context, 50.0f, 40.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);

        BfcSlot slot = findSlot(context, 0.0f, 0.0f, 10.0f, 20.0f, TaffyDirection.LTR);

        assertEquals(1, slot.segmentId());
        assertEquals(50.0f, slot.x(), EPSILON);
        assertEquals(0.0f, slot.y(), EPSILON);
        assertEquals(150.0f, slot.borderWidth(), EPSILON);
        assertEquals(130.0f, slot.stretchWidth(), EPSILON);
        assertEquals(Float.POSITIVE_INFINITY, slot.height());
    }

    @Test
    @DisplayName("a trailing float absorbs only the part of the trailing margin beside it")
    void bfcSlotAbsorbsTrailingMargin() {
        FloatContext context = context(200.0f);
        place(context, 50.0f, 40.0f, 0.0f, FloatContext.Direction.RIGHT, Clear.NONE);

        BfcSlot slot = findSlot(context, 0.0f, 0.0f, 10.0f, 20.0f, TaffyDirection.LTR);

        assertEquals(1, slot.segmentId());
        assertEquals(10.0f, slot.x(), EPSILON);
        assertEquals(140.0f, slot.borderWidth(), EPSILON);
        assertEquals(140.0f, slot.stretchWidth(), EPSILON);
    }

    @Test
    @DisplayName("RTL swaps the leading and trailing sides of the slot")
    void bfcSlotRtlSwapsSides() {
        FloatContext context = context(200.0f);
        place(context, 50.0f, 40.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);

        BfcSlot slot = findSlot(context, 0.0f, 0.0f, 10.0f, 20.0f, TaffyDirection.RTL);

        assertEquals(50.0f, slot.x(), EPSILON);
        assertEquals(130.0f, slot.borderWidth(), EPSILON);
        assertEquals(130.0f, slot.stretchWidth(), EPSILON);
    }

    @Test
    @DisplayName("a negative leading margin does not move past a leading float")
    void bfcSlotNegativeLeadingMarginStopsAtFloat() {
        FloatContext floating = context(200.0f);
        place(floating, 50.0f, 40.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        BfcSlot besideFloat = findSlot(floating, 0.0f, 0.0f, -30.0f, 0.0f, TaffyDirection.LTR);
        assertEquals(50.0f, besideFloat.x(), EPSILON);

        FloatContext free = context(200.0f);
        place(free, 50.0f, 40.0f, 0.0f, FloatContext.Direction.RIGHT, Clear.NONE);
        BfcSlot noLeadingFloat = findSlot(free, 0.0f, 0.0f, -30.0f, 0.0f, TaffyDirection.LTR);
        assertEquals(-30.0f, noLeadingFloat.x(), EPSILON);
        assertEquals(180.0f, noLeadingFloat.borderWidth(), EPSILON);
    }

    @Test
    @DisplayName("trailing margins apply to stretch width and only partly to border fit")
    void bfcSlotTrailingMarginRules() {
        FloatContext positive = context(200.0f);
        place(positive, 50.0f, 40.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        BfcSlot positiveSlot = findSlot(positive, 0.0f, 0.0f, 0.0f, 30.0f, TaffyDirection.LTR);
        assertEquals(150.0f, positiveSlot.borderWidth(), EPSILON);
        assertEquals(120.0f, positiveSlot.stretchWidth(), EPSILON);

        FloatContext negative = context(200.0f);
        place(negative, 50.0f, 40.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        BfcSlot negativeSlot = findSlot(negative, 0.0f, 0.0f, 0.0f, -30.0f, TaffyDirection.LTR);
        assertEquals(180.0f, negativeSlot.borderWidth(), EPSILON);
        assertEquals(180.0f, negativeSlot.stretchWidth(), EPSILON);
    }

    @Test
    @DisplayName("BFC slot height stops where a narrower float starts")
    void bfcSlotHeightStopsAtNarrowerFloat() {
        FloatContext context = context(200.0f);
        place(context, 30.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        place(context, 50.0f, 30.0f, 0.0f, FloatContext.Direction.LEFT, Clear.LEFT);
        assertEquals(20.0f, context.leftFloats().get(1).y(), EPSILON);

        BfcSlot narrow = findSlot(context, 0.0f, 0.0f, 0.0f, 0.0f, TaffyDirection.LTR);
        assertEquals(1, narrow.segmentId());
        assertEquals(170.0f, narrow.borderWidth(), EPSILON);
        assertEquals(20.0f, narrow.height(), EPSILON);

        BfcSlot tall = findSlot(context, 0.0f, 30.0f, 0.0f, 0.0f, TaffyDirection.LTR);
        assertEquals(150.0f, tall.borderWidth(), EPSILON);
        assertEquals(Float.POSITIVE_INFINITY, tall.height());
    }

    @Test
    @DisplayName("below all floats a BFC slot falls back to the containing block")
    void bfcSlotBelowAllFloats() {
        FloatContext context = context(200.0f);
        place(context, 50.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);

        BfcSlot slot = findSlot(context, 30.0f, 0.0f, 5.0f, 10.0f, TaffyDirection.LTR);

        assertFalse(slot.hasSegment());
        assertEquals(5.0f, slot.x(), EPSILON);
        assertEquals(30.0f, slot.y(), EPSILON);
        assertEquals(185.0f, slot.borderWidth(), EPSILON);
        assertEquals(185.0f, slot.stretchWidth(), EPSILON);
    }

    @Test
    @DisplayName("findBfcSlot can resume after a segment")
    void bfcSlotAfterSegment() {
        FloatContext context = context(200.0f);
        place(context, 30.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        place(context, 50.0f, 30.0f, 0.0f, FloatContext.Direction.LEFT, Clear.LEFT);

        BfcSlot afterFirst = context.findBfcSlot(
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            TaffyDirection.LTR, Clear.NONE, 1);

        assertEquals(2, afterFirst.segmentId());
        assertEquals(20.0f, afterFirst.y(), EPSILON);
        assertEquals(150.0f, afterFirst.borderWidth(), EPSILON);
    }

    @Test
    @DisplayName("clear:both moves a BFC slot below both float sides")
    void bfcSlotClearBoth() {
        FloatContext context = context(200.0f);
        place(context, 50.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);
        place(context, 50.0f, 30.0f, 0.0f, FloatContext.Direction.RIGHT, Clear.NONE);

        BfcSlot slot = context.findBfcSlot(
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            TaffyDirection.LTR, Clear.BOTH, FloatContext.NO_REGION);

        assertFalse(slot.hasSegment());
        assertEquals(0.0f, slot.x(), EPSILON);
        assertEquals(30.0f, slot.y(), EPSILON);
        assertEquals(200.0f, slot.borderWidth(), EPSILON);
    }

    @Test
    @DisplayName("a zero-width float edge stops a negative leading margin")
    void bfcSlotZeroWidthFloatEdge() {
        FloatContext context = context(200.0f);
        place(context, 0.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);

        BfcSlot slot = findSlot(context, 0.0f, 0.0f, -10.0f, 0.0f, TaffyDirection.LTR);

        assertEquals(1, slot.segmentId());
        assertEquals(0.0f, slot.x(), EPSILON);
        assertEquals(200.0f, slot.borderWidth(), EPSILON);
    }

    @Test
    @DisplayName("a box crossing a zero-height float narrows beside it")
    void bfcSlotCrossesZeroHeightFloat() {
        FloatContext context = context(200.0f);
        place(context, 50.0f, 0.0f, 40.0f, FloatContext.Direction.LEFT, Clear.NONE);

        BfcSlot slot = findSlot(context, -10.0f, 60.0f, 0.0f, 0.0f, TaffyDirection.LTR);

        assertEquals(0, slot.segmentId());
        assertEquals(-10.0f, slot.y(), EPSILON);
        assertEquals(50.0f, slot.x(), EPSILON);
        assertEquals(150.0f, slot.borderWidth(), EPSILON);
        assertEquals(Float.POSITIVE_INFINITY, slot.height());
    }

    @Test
    @DisplayName("findBfcSlot rejects an invalid afterSegment")
    void bfcSlotRejectsInvalidAfterSegment() {
        FloatContext context = context(200.0f);
        assertThrows(IllegalArgumentException.class,
                     () -> context.findBfcSlot(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                                               TaffyDirection.LTR, Clear.NONE, -2));
    }

    // === BlockContext wrappers ===

    private static BlockContext childContext(FloatContext floatContext) {
        BlockContext root = BlockContext.root(floatContext);
        BlockContext child = root.subContext(30.0f, 10.0f, 20.0f);
        child.applyContentBoxInset(5.0f, 5.0f);
        child.setWidth(200.0f);
        return child;
    }

    @Test
    @DisplayName("BlockContext.placeFloatedBox uses content insets and returns local coordinates")
    void blockContextPlacementUsesLocalCoordinates() {
        FloatContext floatContext = new FloatContext();
        BlockContext child = childContext(floatContext);

        FloatContext.FloatPlacement placement =
            child.placeFloatedBox(50.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);

        assertTrue(placement.placed());
        assertEquals(5.0f, placement.x(), EPSILON); // absolute 15 - border inset 10
        assertEquals(0.0f, placement.y(), EPSILON); // absolute 30 - y offset 30
        assertTrue(child.hasFloats());
        assertEquals(65.0f, floatContext.regionAt(35.0f).leftInset(), EPSILON);
        assertEquals(25.0f, floatContext.regionAt(35.0f).rightInset(), EPSILON);
        assertEquals(20.0f, child.clearedThreshold(Clear.LEFT), EPSILON);
        assertEquals(20.0f, child.floatedContentHeightContribution(), EPSILON);
        assertEquals(30.0f, child.floatedContentHeightIncludingBottomInset(10.0f), EPSILON);
    }

    @Test
    @DisplayName("BlockContext.findBfcSlot translates the slot back to local coordinates")
    void blockContextBfcSlotUsesLocalCoordinates() {
        FloatContext floatContext = new FloatContext();
        BlockContext child = childContext(floatContext);
        child.placeFloatedBox(50.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);

        BfcSlot slot = child.findBfcSlot(
            0.0f, 0.0f, 0.0f, 0.0f, TaffyDirection.LTR, Clear.NONE);
        assertEquals(1, slot.segmentId());
        assertEquals(55.0f, slot.x(), EPSILON); // absolute 65 - border inset 10
        assertEquals(0.0f, slot.y(), EPSILON);
        // T24: the containing block's content box is [15, 175) in the BFC (border insets 10/20
        // plus content insets 5/5), and upstream's find_bfc_slot adds the containing-block insets
        // to the margin insets. A positive trailing margin may overflow that edge, so it does not
        // narrow the fit width: 175 - 65 = 110. The stretch width subtracts the trailing margin
        // from the content-box width: 100 - 10 = 90.
        assertEquals(110.0f, slot.borderWidth(), EPSILON);
        assertEquals(110.0f, slot.stretchWidth(), EPSILON);

        BfcSlot margined = child.findBfcSlot(
            0.0f, 0.0f, 10.0f, 20.0f, TaffyDirection.LTR, Clear.NONE);
        assertEquals(55.0f, margined.x(), EPSILON);
        assertEquals(110.0f, margined.borderWidth(), EPSILON);
        assertEquals(90.0f, margined.stretchWidth(), EPSILON);
    }

    @Test
    @DisplayName("nested BlockContexts share the BFC float state")
    void blockContextsShareFloatState() {
        FloatContext floatContext = new FloatContext();
        BlockContext root = BlockContext.root(floatContext);
        BlockContext child = root.subContext(30.0f, 10.0f, 20.0f);
        child.applyContentBoxInset(5.0f, 5.0f);
        child.setWidth(200.0f);
        child.placeFloatedBox(50.0f, 20.0f, 0.0f, FloatContext.Direction.LEFT, Clear.NONE);

        BlockContext sibling = root.subContext(10.0f, 0.0f, 0.0f);
        assertTrue(sibling.hasFloats());
        assertTrue(sibling.hasActiveFloats(0.0f));
        assertEquals(40.0f, sibling.clearedThreshold(Clear.LEFT), EPSILON);

        FloatContext.ContentSlot content = sibling.findContentSlot(30.0f, Clear.NONE);
        assertEquals(30.0f, content.y(), EPSILON);
        assertEquals(65.0f, content.x(), EPSILON);
        assertEquals(110.0f, content.width(), EPSILON);

        BfcSlot bfc = sibling.findBfcSlot(
            30.0f, 0.0f, 0.0f, 0.0f, TaffyDirection.LTR, Clear.NONE);
        assertEquals(30.0f, bfc.y(), EPSILON);
        assertEquals(65.0f, bfc.x(), EPSILON);
        assertEquals(135.0f, bfc.borderWidth(), EPSILON);
    }

    @Test
    @DisplayName("floated content contribution merges from child to parent with bottom inset")
    void floatedContentContributionMerges() {
        BlockContext root = BlockContext.root();
        assertEquals(0.0f, root.floatedContentHeightContribution(), EPSILON);
        assertEquals(7.0f, root.floatedContentHeightIncludingBottomInset(7.0f), EPSILON);

        BlockContext child = root.subContext(20.0f, 0.0f, 0.0f);
        child.setWidth(200.0f);
        child.placeFloatedBox(50.0f, 20.0f, 10.0f, FloatContext.Direction.LEFT, Clear.NONE);
        assertEquals(30.0f, child.floatedContentHeightContribution(), EPSILON);
        assertEquals(35.0f, child.floatedContentHeightIncludingBottomInset(5.0f), EPSILON);

        root.addChildFloatedContentHeightContribution(child.floatedContentHeightContribution());
        assertEquals(30.0f, root.floatedContentHeightContribution(), EPSILON);
        assertEquals(40.0f, root.floatedContentHeightIncludingBottomInset(10.0f), EPSILON);
    }
}
