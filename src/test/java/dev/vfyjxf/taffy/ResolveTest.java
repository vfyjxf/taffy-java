package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.TaffyDimension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit resolution tests ported from taffy/src/util/resolve.rs
 * Tests for MaybeResolve and ResolveOrZero implementations.
 */
public class ResolveTest {

    private static final float EPSILON = 0.001f;

    // =============== MaybeResolve Dimension Tests ===============
    
    @Nested
    @DisplayName("maybe_resolve_dimension")
    class MaybeResolveDimension {

        /**
         * Dimension::Auto should always return None
         * The parent / context should not affect the outcome.
         */
        @Test
        @DisplayName("resolve_auto")
        void resolveAuto() {
            assertTrue(Float.isNaN(TaffyDimension.AUTO.maybeResolve(Float.NaN)));
            assertTrue(Float.isNaN(TaffyDimension.AUTO.maybeResolve(5.0f)));
            assertTrue(Float.isNaN(TaffyDimension.AUTO.maybeResolve(-5.0f)));
            assertTrue(Float.isNaN(TaffyDimension.AUTO.maybeResolve(0f)));
        }

        /**
         * Dimension::Length should always return Some(f32)
         * where the f32 value is the inner absolute length.
         * The parent / context should not affect the outcome.
         */
        @Test
        @DisplayName("resolve_length")
        void resolveLength() {
            assertEquals(1.0f, TaffyDimension.length(1.0f).maybeResolve(Float.NaN), EPSILON);
            assertEquals(1.0f, TaffyDimension.length(1.0f).maybeResolve(5.0f), EPSILON);
            assertEquals(1.0f, TaffyDimension.length(1.0f).maybeResolve(-5.0f), EPSILON);
            assertEquals(1.0f, TaffyDimension.length(1.0f).maybeResolve(0f), EPSILON);
        }

        /**
         * Dimension::Percent should return None if context is None.
         * Otherwise it should return Some(f32)
         * where the f32 value is the inner value of the percent * context value.
         * The parent / context should affect the outcome.
         */
        @Test
        @DisplayName("resolve_percent")
        void resolvePercent() {
            assertTrue(Float.isNaN(TaffyDimension.percent(1.0f).maybeResolve(Float.NaN)));
            assertEquals(5.0f, TaffyDimension.percent(1.0f).maybeResolve(5.0f), EPSILON);
            assertEquals(-5.0f, TaffyDimension.percent(1.0f).maybeResolve(-5.0f), EPSILON);
            assertEquals(50.0f, TaffyDimension.percent(1.0f).maybeResolve(50.0f), EPSILON);
        }
    }

    // =============== MaybeResolve Size<Dimension> Tests ===============
    
    @Nested
    @DisplayName("maybe_resolve_size_dimension")
    class MaybeResolveSizeDimension {

        /**
         * Size<Dimension::Auto> should always return Size<None>
         * The parent / context should not affect the outcome.
         */
        @Test
        @DisplayName("maybe_resolve_auto")
        void maybeResolveAuto() {
            var autoSize = TaffySize.auto(TaffyDimension::auto);
            
            assertSizeEquals(FloatSize.none(), autoSize.maybeResolve(FloatSize.none()));
            assertSizeEquals(FloatSize.none(), autoSize.maybeResolve(FloatSize.of(5.0f, 5.0f)));
            assertSizeEquals(FloatSize.none(), autoSize.maybeResolve(FloatSize.of(-5.0f, -5.0f)));
            assertSizeEquals(FloatSize.none(), autoSize.maybeResolve(FloatSize.of(0.0f, 0.0f)));
        }

        /**
         * Size<Dimension::Length> should always return a Size<Some(f32)>
         * where the f32 values are the absolute length.
         * The parent / context should not affect the outcome.
         */
        @Test
        @DisplayName("maybe_resolve_length")
        void maybeResolveLength() {
            var lengthSize = TaffySize.of(TaffyDimension.length(5.0f), TaffyDimension.length(5.0f));
            
            assertSizeEquals(FloatSize.of(5.0f, 5.0f), lengthSize.maybeResolve(FloatSize.none()));
            assertSizeEquals(FloatSize.of(5.0f, 5.0f), lengthSize.maybeResolve(FloatSize.of(5.0f, 5.0f)));
            assertSizeEquals(FloatSize.of(5.0f, 5.0f), lengthSize.maybeResolve(FloatSize.of(-5.0f, -5.0f)));
            assertSizeEquals(FloatSize.of(5.0f, 5.0f), lengthSize.maybeResolve(FloatSize.of(0.0f, 0.0f)));
        }

        /**
         * Size<Dimension::Percent> should return Size<None> if context is Size<None>.
         * Otherwise it should return Size<Some(f32)>
         * where the f32 value is the inner value of the percent * context value.
         * The context should affect the outcome.
         */
        @Test
        @DisplayName("maybe_resolve_percent")
        void maybeResolvePercent() {
            var percentSize = TaffySize.of(TaffyDimension.percent(5.0f), TaffyDimension.percent(5.0f));
            
            assertSizeEquals(FloatSize.none(), percentSize.maybeResolve(FloatSize.none()));
            assertSizeEquals(FloatSize.of(25.0f, 25.0f), percentSize.maybeResolve(FloatSize.of(5.0f, 5.0f)));
            assertSizeEquals(FloatSize.of(-25.0f, -25.0f), percentSize.maybeResolve(FloatSize.of(-5.0f, -5.0f)));
            assertSizeEquals(FloatSize.of(0.0f, 0.0f), percentSize.maybeResolve(FloatSize.of(0.0f, 0.0f)));
        }
    }

    // =============== ResolveOrZero Dimension Tests ===============

    @Nested
    @DisplayName("resolve_or_zero_dimension_to_option_f32")
    class ResolveOrZeroDimensionToOptionF32 {

        @Test
        @DisplayName("resolve_or_zero_auto")
        void resolveOrZeroAuto() {
            assertEquals(0.0f, TaffyDimension.AUTO.resolveOrZero(Float.NaN), EPSILON);
            assertEquals(0.0f, TaffyDimension.AUTO.resolveOrZero(5.0f), EPSILON);
            assertEquals(0.0f, TaffyDimension.AUTO.resolveOrZero(-5.0f), EPSILON);
            assertEquals(0.0f, TaffyDimension.AUTO.resolveOrZero(0.0f), EPSILON);
        }

        @Test
        @DisplayName("resolve_or_zero_length")
        void resolveOrZeroLength() {
            assertEquals(5.0f, TaffyDimension.length(5.0f).resolveOrZero(Float.NaN), EPSILON);
            assertEquals(5.0f, TaffyDimension.length(5.0f).resolveOrZero(5.0f), EPSILON);
            assertEquals(5.0f, TaffyDimension.length(5.0f).resolveOrZero(-5.0f), EPSILON);
            assertEquals(5.0f, TaffyDimension.length(5.0f).resolveOrZero(0.0f), EPSILON);
        }

        @Test
        @DisplayName("resolve_or_zero_percent")
        void resolveOrZeroPercent() {
            assertEquals(0.0f, TaffyDimension.percent(5.0f).resolveOrZero(Float.NaN), EPSILON);
            assertEquals(25.0f, TaffyDimension.percent(5.0f).resolveOrZero(5.0f), EPSILON);
            assertEquals(-25.0f, TaffyDimension.percent(5.0f).resolveOrZero(-5.0f), EPSILON);
            assertEquals(0.0f, TaffyDimension.percent(5.0f).resolveOrZero(0.0f), EPSILON);
        }
    }

    // =============== ResolveOrZero Rect<Dimension> to Rect Tests ===============

    @Nested
    @DisplayName("resolve_or_zero_rect_dimension_to_rect")
    class ResolveOrZeroRectDimensionToRect {

        @Test
        @DisplayName("resolve_or_zero_auto")
        void resolveOrZeroAuto() {
            var autoRect = TaffyRect.auto(TaffyDimension::auto);
            
            assertRectEquals(TaffyRect.zero(), autoRect.resolveOrZero(FloatSize.none()));
            assertRectEquals(TaffyRect.zero(), autoRect.resolveOrZero(FloatSize.of(5.0f, 5.0f)));
            assertRectEquals(TaffyRect.zero(), autoRect.resolveOrZero(FloatSize.of(-5.0f, -5.0f)));
            assertRectEquals(TaffyRect.zero(), autoRect.resolveOrZero(FloatSize.of(0.0f, 0.0f)));
        }

        @Test
        @DisplayName("resolve_or_zero_length")
        void resolveOrZeroLength() {
            var lengthRect = TaffyRect.fromLength(5.0f);
            
            assertRectEquals(TaffyRect.of(5.0f, 5.0f, 5.0f, 5.0f), lengthRect.resolveOrZero(FloatSize.none()));
            assertRectEquals(TaffyRect.of(5.0f, 5.0f, 5.0f, 5.0f), lengthRect.resolveOrZero(FloatSize.of(5.0f, 5.0f)));
            assertRectEquals(TaffyRect.of(5.0f, 5.0f, 5.0f, 5.0f), lengthRect.resolveOrZero(FloatSize.of(-5.0f, -5.0f)));
            assertRectEquals(TaffyRect.of(5.0f, 5.0f, 5.0f, 5.0f), lengthRect.resolveOrZero(FloatSize.of(0.0f, 0.0f)));
        }

        @Test
        @DisplayName("resolve_or_zero_percent")
        void resolveOrZeroPercent() {
            var percentRect = TaffyRect.fromPercent(5.0f);
            
            assertRectEquals(TaffyRect.zero(), percentRect.resolveOrZero(FloatSize.none()));
            assertRectEquals(TaffyRect.of(25.0f, 25.0f, 25.0f, 25.0f), percentRect.resolveOrZero(FloatSize.of(5.0f, 5.0f)));
            assertRectEquals(TaffyRect.of(-25.0f, -25.0f, -25.0f, -25.0f), percentRect.resolveOrZero(FloatSize.of(-5.0f, -5.0f)));
            assertRectEquals(TaffyRect.zero(), percentRect.resolveOrZero(FloatSize.of(0.0f, 0.0f)));
        }
    }

    // =============== ResolveOrZero Rect<Dimension> to Rect<f32> via Option Tests ===============

    @Nested
    @DisplayName("resolve_or_zero_rect_dimension_to_rect_f32_via_option")
    class ResolveOrZeroRectDimensionToRectF32ViaOption {

        @Test
        @DisplayName("resolve_or_zero_auto")
        void resolveOrZeroAuto() {
            var autoRect = TaffyRect.auto(TaffyDimension::auto);
            
            assertRectEquals(TaffyRect.zero(), autoRect.resolveOrZero(Float.NaN));
            assertRectEquals(TaffyRect.zero(), autoRect.resolveOrZero(5.0f));
            assertRectEquals(TaffyRect.zero(), autoRect.resolveOrZero(-5.0f));
            assertRectEquals(TaffyRect.zero(), autoRect.resolveOrZero(0.0f));
        }

        @Test
        @DisplayName("resolve_or_zero_length")
        void resolveOrZeroLength() {
            var lengthRect = TaffyRect.fromLength(5.0f);
            
            assertRectEquals(TaffyRect.of(5.0f, 5.0f, 5.0f, 5.0f), lengthRect.resolveOrZero(Float.NaN));
            assertRectEquals(TaffyRect.of(5.0f, 5.0f, 5.0f, 5.0f), lengthRect.resolveOrZero(5.0f));
            assertRectEquals(TaffyRect.of(5.0f, 5.0f, 5.0f, 5.0f), lengthRect.resolveOrZero(-5.0f));
            assertRectEquals(TaffyRect.of(5.0f, 5.0f, 5.0f, 5.0f), lengthRect.resolveOrZero(0.0f));
        }

        @Test
        @DisplayName("resolve_or_zero_percent")
        void resolveOrZeroPercent() {
            var percentRect = TaffyRect.fromPercent(5.0f);
            
            assertRectEquals(TaffyRect.zero(), percentRect.resolveOrZero(Float.NaN));
            assertRectEquals(TaffyRect.of(25.0f, 25.0f, 25.0f, 25.0f), percentRect.resolveOrZero(5.0f));
            assertRectEquals(TaffyRect.of(-25.0f, -25.0f, -25.0f, -25.0f), percentRect.resolveOrZero(-5.0f));
            assertRectEquals(TaffyRect.zero(), percentRect.resolveOrZero(0.0f));
        }
    }

    // =============== Helper Methods ===============

    private void assertSizeEquals(FloatSize expected, FloatSize actual) {
        if (Float.isNaN(expected.getWidth())) {
            assertTrue(Float.isNaN(actual.getWidth()), "Expected width to be NaN (None)");
        } else {
            assertEquals(expected.getWidth(), actual.getWidth(), EPSILON, "Width mismatch");
        }
        if (Float.isNaN(expected.getHeight())) {
            assertTrue(Float.isNaN(actual.getHeight()), "Expected height to be NaN (None)");
        } else {
            assertEquals(expected.getHeight(), actual.getHeight(), EPSILON, "Height mismatch");
        }
    }

    private void assertRectEquals(TaffyRect<Float> expected, TaffyRect<Float> actual) {
        assertEquals(expected.getLeft(), actual.getLeft(), EPSILON, "Left mismatch");
        assertEquals(expected.getRight(), actual.getRight(), EPSILON, "Right mismatch");
        assertEquals(expected.getTop(), actual.getTop(), EPSILON, "Top mismatch");
        assertEquals(expected.getBottom(), actual.getBottom(), EPSILON, "Bottom mismatch");
    }
}
