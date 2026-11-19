package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.style.Clear;
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
}
