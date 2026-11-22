package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.style.CalcExpression;
import dev.vfyjxf.taffy.style.TaffyDimension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the additive S5 (1.2) sizing-keyword helper on {@link TaffyDimension}.
 *
 * <p>Scope: the helper is opt-in only. {@code maybeResolve} must keep returning {@code NaN}
 * for every intrinsic keyword (default LEGACY_AUTO behavior); wiring the helper into the
 * layout algorithms is deferred to the 1.3 opt-in and the 2.0 default flip.
 */
@DisplayName("Sizing Keyword Helper Tests (S5 P0-b)")
public class SizingKeywordHelperTest {

    private static final float MIN_CONTENT_SIZE = 11f;
    private static final float MAX_CONTENT_SIZE = 22f;
    private static final float FIT_CONTENT_SIZE = 33f;

    private static TaffyDimension sample(TaffyDimension.Type type) {
        return switch (type) {
            case LENGTH -> TaffyDimension.length(10f);
            case PERCENT -> TaffyDimension.percent(0.5f);
            case AUTO -> TaffyDimension.auto();
            case CALC -> TaffyDimension.calc(CalcExpression.percentPlusLength(0.5f, 10f));
            case MIN_CONTENT -> TaffyDimension.minContent();
            case MAX_CONTENT -> TaffyDimension.maxContent();
            case FIT_CONTENT -> TaffyDimension.fitContent();
            case STRETCH -> TaffyDimension.stretch();
            case CONTENT -> TaffyDimension.content();
        };
    }

    @Nested
    @DisplayName("resolveIntrinsicKeyword mapping")
    class ResolveMappingTests {

        @Test
        @DisplayName("min-content selects the min-content size")
        void minContentMapsToMinContentSize() {
            assertEquals(MIN_CONTENT_SIZE, TaffyDimension.minContent()
                    .resolveIntrinsicKeyword(MIN_CONTENT_SIZE, MAX_CONTENT_SIZE, FIT_CONTENT_SIZE));
        }

        @Test
        @DisplayName("max-content selects the max-content size")
        void maxContentMapsToMaxContentSize() {
            assertEquals(MAX_CONTENT_SIZE, TaffyDimension.maxContent()
                    .resolveIntrinsicKeyword(MIN_CONTENT_SIZE, MAX_CONTENT_SIZE, FIT_CONTENT_SIZE));
        }

        @Test
        @DisplayName("fit-content selects the caller-supplied fit-content size")
        void fitContentMapsToFitContentSize() {
            assertEquals(FIT_CONTENT_SIZE, TaffyDimension.fitContent()
                    .resolveIntrinsicKeyword(MIN_CONTENT_SIZE, MAX_CONTENT_SIZE, FIT_CONTENT_SIZE));
        }

        @Test
        @DisplayName("each keyword returns exactly one of the three arguments")
        void mappingsDoNotMixArguments() {
            // Negative/distinct values make an accidental argument swap obvious.
            assertEquals(-1f, TaffyDimension.minContent().resolveIntrinsicKeyword(-1f, -2f, -3f));
            assertEquals(-2f, TaffyDimension.maxContent().resolveIntrinsicKeyword(-1f, -2f, -3f));
            assertEquals(-3f, TaffyDimension.fitContent().resolveIntrinsicKeyword(-1f, -2f, -3f));
        }

        @Test
        @DisplayName("arguments are not evaluated for unused sizes")
        void untakenArgumentsAreIgnored() {
            assertEquals(MIN_CONTENT_SIZE, TaffyDimension.minContent()
                    .resolveIntrinsicKeyword(MIN_CONTENT_SIZE, Float.NaN, Float.NaN));
            assertEquals(MAX_CONTENT_SIZE, TaffyDimension.maxContent()
                    .resolveIntrinsicKeyword(Float.NaN, MAX_CONTENT_SIZE, Float.NaN));
            assertEquals(FIT_CONTENT_SIZE, TaffyDimension.fitContent()
                    .resolveIntrinsicKeyword(Float.NaN, Float.NaN, FIT_CONTENT_SIZE));
        }
    }

    @Nested
    @DisplayName("resolveIntrinsicKeyword unsupported types")
    class ResolveUnsupportedTests {

        @Test
        @DisplayName("stretch throws UnsupportedOperationException with a documented reason")
        void stretchThrows() {
            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                    () -> TaffyDimension.stretch()
                            .resolveIntrinsicKeyword(MIN_CONTENT_SIZE, MAX_CONTENT_SIZE, FIT_CONTENT_SIZE));
            assertTrue(ex.getMessage().contains("stretch"), ex.getMessage());
            assertTrue(ex.getMessage().contains("available space"), ex.getMessage());
            assertTrue(ex.getMessage().contains("1.3"), ex.getMessage());
        }

        @Test
        @DisplayName("content throws UnsupportedOperationException, not NaN or auto")
        void contentThrows() {
            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                    () -> TaffyDimension.content()
                            .resolveIntrinsicKeyword(MIN_CONTENT_SIZE, MAX_CONTENT_SIZE, FIT_CONTENT_SIZE));
            assertTrue(ex.getMessage().contains("content"), ex.getMessage());
            assertTrue(ex.getMessage().contains("flex-basis"), ex.getMessage());
        }

        @Test
        @DisplayName("LENGTH, PERCENT, AUTO and CALC are rejected with IllegalStateException")
        void nonKeywordsThrowIllegalState() {
            TaffyDimension[] nonKeywords = {
                    TaffyDimension.length(10f),
                    TaffyDimension.percent(0.5f),
                    TaffyDimension.auto(),
                    TaffyDimension.calc(CalcExpression.percentPlusLength(0.5f, 10f))
            };
            for (TaffyDimension dimension : nonKeywords) {
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                        () -> dimension.resolveIntrinsicKeyword(MIN_CONTENT_SIZE, MAX_CONTENT_SIZE, FIT_CONTENT_SIZE),
                        "expected " + dimension.getType() + " to be rejected");
                assertTrue(ex.getMessage().contains(dimension.getType().name()), ex.getMessage());
                assertFalse(ex.getMessage().isBlank());
            }
        }
    }

    @Nested
    @DisplayName("isIntrinsicKeyword classification")
    class IsIntrinsicKeywordTests {

        @Test
        @DisplayName("the four sizing keywords are intrinsic keywords")
        void fourKeywordsReturnTrue() {
            assertTrue(TaffyDimension.minContent().isIntrinsicKeyword());
            assertTrue(TaffyDimension.maxContent().isIntrinsicKeyword());
            assertTrue(TaffyDimension.fitContent().isIntrinsicKeyword());
            assertTrue(TaffyDimension.stretch().isIntrinsicKeyword());
        }

        @Test
        @DisplayName("LENGTH, PERCENT and AUTO are not intrinsic keywords")
        void lengthPercentAutoReturnFalse() {
            assertFalse(TaffyDimension.length(10f).isIntrinsicKeyword());
            assertFalse(TaffyDimension.percent(0.5f).isIntrinsicKeyword());
            assertFalse(TaffyDimension.auto().isIntrinsicKeyword());
        }

        @Test
        @DisplayName("CALC is not an intrinsic keyword")
        void calcReturnsFalse() {
            assertFalse(TaffyDimension.calc(CalcExpression.percentPlusLength(0.5f, 10f))
                    .isIntrinsicKeyword());
        }

        @Test
        @DisplayName("CONTENT is not a sizing keyword but stays isIntrinsic()")
        void contentIsNotASizingKeyword() {
            assertFalse(TaffyDimension.content().isIntrinsicKeyword(),
                    "content is a flex-basis keyword, not a sizing keyword");
            assertTrue(TaffyDimension.content().isIntrinsic(),
                    "the pre-existing broader isIntrinsic() contract must not change");
        }

        @Test
        @DisplayName("classification partitions every TaffyDimension.Type value")
        void classificationCoversAllTypes() {
            int intrinsicKeywordCount = 0;
            for (TaffyDimension.Type type : TaffyDimension.Type.values()) {
                boolean expected = type == TaffyDimension.Type.MIN_CONTENT
                        || type == TaffyDimension.Type.MAX_CONTENT
                        || type == TaffyDimension.Type.FIT_CONTENT
                        || type == TaffyDimension.Type.STRETCH;
                assertEquals(expected, sample(type).isIntrinsicKeyword(), "unexpected result for " + type);
                if (expected) {
                    intrinsicKeywordCount++;
                }
            }
            assertEquals(4, intrinsicKeywordCount);
        }

        @Test
        @DisplayName("boundary values: zero length, zero percent and auto are not keywords")
        void zeroAndAutoAreNotKeywords() {
            assertFalse(TaffyDimension.ZERO.isIntrinsicKeyword());
            assertFalse(TaffyDimension.length(0f).isIntrinsicKeyword());
            assertFalse(TaffyDimension.percent(0f).isIntrinsicKeyword());
            assertFalse(TaffyDimension.AUTO.isIntrinsicKeyword());
        }

        @Test
        @DisplayName("the keyword singletons themselves classify as keywords")
        void singletonFieldsReturnTrue() {
            assertTrue(TaffyDimension.MIN_CONTENT.isIntrinsicKeyword());
            assertTrue(TaffyDimension.MAX_CONTENT.isIntrinsicKeyword());
            assertTrue(TaffyDimension.FIT_CONTENT.isIntrinsicKeyword());
            assertTrue(TaffyDimension.STRETCH.isIntrinsicKeyword());
        }
    }

    @Nested
    @DisplayName("legacy resolution regression (default LEGACY_AUTO)")
    class LegacyResolutionTests {

        @Test
        @DisplayName("maybeResolve still returns NaN for every intrinsic keyword")
        void maybeResolveStillNaN() {
            assertTrue(Float.isNaN(TaffyDimension.minContent().maybeResolve(100f)));
            assertTrue(Float.isNaN(TaffyDimension.maxContent().maybeResolve(100f)));
            assertTrue(Float.isNaN(TaffyDimension.fitContent().maybeResolve(100f)));
            assertTrue(Float.isNaN(TaffyDimension.stretch().maybeResolve(100f)));
        }

        @Test
        @DisplayName("maybeResolve still returns NaN for content")
        void contentMaybeResolveStillNaN() {
            assertTrue(Float.isNaN(TaffyDimension.content().maybeResolve(100f)));
        }

        @Test
        @DisplayName("length and percent still resolve normally")
        void normalTypesStillResolve() {
            assertEquals(10f, TaffyDimension.length(10f).maybeResolve(100f));
            assertEquals(50f, TaffyDimension.percent(0.5f).maybeResolve(100f));
            assertTrue(Float.isNaN(TaffyDimension.auto().maybeResolve(100f)));
        }

        @Test
        @DisplayName("resolveOrZero is still zero for intrinsic keywords")
        void resolveOrZeroStillZero() {
            assertEquals(0f, TaffyDimension.minContent().resolveOrZero(100f));
            assertEquals(0f, TaffyDimension.maxContent().resolveOrZero(100f));
            assertEquals(0f, TaffyDimension.fitContent().resolveOrZero(100f));
            assertEquals(0f, TaffyDimension.stretch().resolveOrZero(100f));
        }

        @Test
        @DisplayName("calling the helper does not change later legacy resolution")
        void helperHasNoSideEffectsOnLegacyResolution() {
            TaffyDimension min = TaffyDimension.minContent();
            TaffyDimension max = TaffyDimension.maxContent();
            TaffyDimension fit = TaffyDimension.fitContent();

            assertEquals(MIN_CONTENT_SIZE,
                    min.resolveIntrinsicKeyword(MIN_CONTENT_SIZE, MAX_CONTENT_SIZE, FIT_CONTENT_SIZE));
            assertEquals(MAX_CONTENT_SIZE,
                    max.resolveIntrinsicKeyword(MIN_CONTENT_SIZE, MAX_CONTENT_SIZE, FIT_CONTENT_SIZE));
            assertEquals(FIT_CONTENT_SIZE,
                    fit.resolveIntrinsicKeyword(MIN_CONTENT_SIZE, MAX_CONTENT_SIZE, FIT_CONTENT_SIZE));

            assertTrue(Float.isNaN(min.maybeResolve(100f)));
            assertTrue(Float.isNaN(max.maybeResolve(100f)));
            assertTrue(Float.isNaN(fit.maybeResolve(100f)));
            assertEquals(0f, min.resolveOrZero(100f));
            assertEquals(0f, max.resolveOrZero(100f));
            assertEquals(0f, fit.resolveOrZero(100f));
        }
    }
}
