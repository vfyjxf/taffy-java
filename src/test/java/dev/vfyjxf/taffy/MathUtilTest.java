package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.util.MaybeMath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Math utility tests ported from taffy/src/util/math.rs
 * Tests for the MaybeMath utility functions that handle optional float operations.
 */
public class MathUtilTest {

    private static final float EPSILON = 0.001f;

    @Nested
    @DisplayName("Option<Float> with Option<Float>")
    class OptionWithOption {

        @Test
        @DisplayName("test_maybe_min")
        void testMaybeMin() {
            assertEquals(3.0f, MaybeMath.maybeMin(3.0f, 5.0f), EPSILON);
            assertEquals(3.0f, MaybeMath.maybeMin(5.0f, 3.0f), EPSILON);
            assertEquals(3.0f, MaybeMath.maybeMin(3.0f, (Float) null), EPSILON);
            assertNull(MaybeMath.maybeMin((Float) null, 3.0f));
            assertNull(MaybeMath.maybeMin((Float) null, (Float) null));
        }

        @Test
        @DisplayName("test_maybe_max")
        void testMaybeMax() {
            assertEquals(5.0f, MaybeMath.maybeMax(3.0f, 5.0f), EPSILON);
            assertEquals(5.0f, MaybeMath.maybeMax(5.0f, 3.0f), EPSILON);
            assertEquals(3.0f, MaybeMath.maybeMax(3.0f, (Float) null), EPSILON);
            assertNull(MaybeMath.maybeMax((Float) null, 3.0f));
            assertNull(MaybeMath.maybeMax((Float) null, (Float) null));
        }

        @Test
        @DisplayName("test_maybe_add")
        void testMaybeAdd() {
            assertEquals(8.0f, MaybeMath.maybeAdd(3.0f, 5.0f), EPSILON);
            assertEquals(8.0f, MaybeMath.maybeAdd(5.0f, 3.0f), EPSILON);
            assertEquals(3.0f, MaybeMath.maybeAdd(3.0f, (Float) null), EPSILON);
            assertNull(MaybeMath.maybeAdd((Float) null, 3.0f));
            assertNull(MaybeMath.maybeAdd((Float) null, (Float) null));
        }

        @Test
        @DisplayName("test_maybe_sub")
        void testMaybeSub() {
            assertEquals(-2.0f, MaybeMath.maybeSub(3.0f, 5.0f), EPSILON);
            assertEquals(2.0f, MaybeMath.maybeSub(5.0f, 3.0f), EPSILON);
            assertEquals(3.0f, MaybeMath.maybeSub(3.0f, (Float) null), EPSILON);
            assertNull(MaybeMath.maybeSub((Float) null, 3.0f));
            assertNull(MaybeMath.maybeSub((Float) null, (Float) null));
        }
    }

    @Nested
    @DisplayName("Option<Float> with float primitive")
    class OptionWithPrimitive {

        @Test
        @DisplayName("test_maybe_min")
        void testMaybeMin() {
            assertEquals(3.0f, MaybeMath.maybeMinPrimitive(3.0f, 5.0f), EPSILON);
            assertEquals(3.0f, MaybeMath.maybeMinPrimitive(5.0f, 3.0f), EPSILON);
            assertNull(MaybeMath.maybeMinPrimitive(null, 3.0f));
        }

        @Test
        @DisplayName("test_maybe_max")
        void testMaybeMax() {
            assertEquals(5.0f, MaybeMath.maybeMaxPrimitive(3.0f, 5.0f), EPSILON);
            assertEquals(5.0f, MaybeMath.maybeMaxPrimitive(5.0f, 3.0f), EPSILON);
            assertNull(MaybeMath.maybeMaxPrimitive(null, 3.0f));
        }

        @Test
        @DisplayName("test_maybe_add")
        void testMaybeAdd() {
            assertEquals(8.0f, MaybeMath.maybeAddPrimitive(3.0f, 5.0f), EPSILON);
            assertEquals(8.0f, MaybeMath.maybeAddPrimitive(5.0f, 3.0f), EPSILON);
            assertNull(MaybeMath.maybeAddPrimitive(null, 3.0f));
        }

        @Test
        @DisplayName("test_maybe_sub")
        void testMaybeSub() {
            assertEquals(-2.0f, MaybeMath.maybeSubPrimitive(3.0f, 5.0f), EPSILON);
            assertEquals(2.0f, MaybeMath.maybeSubPrimitive(5.0f, 3.0f), EPSILON);
            assertNull(MaybeMath.maybeSubPrimitive(null, 3.0f));
        }
    }

    @Nested
    @DisplayName("float primitive with Option<Float>")
    class PrimitiveWithOption {

        @Test
        @DisplayName("test_maybe_min")
        void testMaybeMin() {
            assertEquals(3.0f, MaybeMath.primitiveMin(3.0f, 5.0f), EPSILON);
            assertEquals(3.0f, MaybeMath.primitiveMin(5.0f, 3.0f), EPSILON);
            assertEquals(3.0f, MaybeMath.primitiveMin(3.0f, null), EPSILON);
        }

        @Test
        @DisplayName("test_maybe_max")
        void testMaybeMax() {
            assertEquals(5.0f, MaybeMath.primitiveMax(3.0f, 5.0f), EPSILON);
            assertEquals(5.0f, MaybeMath.primitiveMax(5.0f, 3.0f), EPSILON);
            assertEquals(3.0f, MaybeMath.primitiveMax(3.0f, null), EPSILON);
        }

        @Test
        @DisplayName("test_maybe_add")
        void testMaybeAdd() {
            assertEquals(8.0f, MaybeMath.primitiveAdd(3.0f, 5.0f), EPSILON);
            assertEquals(8.0f, MaybeMath.primitiveAdd(5.0f, 3.0f), EPSILON);
            assertEquals(3.0f, MaybeMath.primitiveAdd(3.0f, null), EPSILON);
        }

        @Test
        @DisplayName("test_maybe_sub")
        void testMaybeSub() {
            assertEquals(-2.0f, MaybeMath.primitiveSub(3.0f, 5.0f), EPSILON);
            assertEquals(2.0f, MaybeMath.primitiveSub(5.0f, 3.0f), EPSILON);
            assertEquals(3.0f, MaybeMath.primitiveSub(3.0f, null), EPSILON);
        }
    }
}
