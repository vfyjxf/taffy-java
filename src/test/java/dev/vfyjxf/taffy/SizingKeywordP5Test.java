package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.CalcExpression;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.util.SizingKeyword;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P5 ST-01 / FL-17: the sizing-keyword parser ({@link SizingKeyword}) and its wiring into the
 * flexbox algorithm.
 *
 * <p>The parser is the additive route that answers the Exact/Measure question for
 * {@code min-content}, {@code max-content}, {@code fit-content}, {@code fit-content(calc())} and
 * {@code stretch}. {@link TaffyDimension#maybeResolve(float)} keeps its legacy behavior (NaN for
 * every keyword); the flexbox tests pin the new behavior at the three consumption sites:</p>
 * <ul>
 *   <li>the flex-basis keyword (including {@code flex-basis: content});</li>
 *   <li>the main-axis size keyword used when the flex basis is {@code auto};</li>
 *   <li>the cross-axis size keyword used for the hypothetical cross size.</li>
 * </ul>
 */
@DisplayName("Sizing keywords P5 (ST-01 parser + FL-17 flex wiring)")
public class SizingKeywordP5Test {

    private static final float EPSILON = 0.01f;

    // ------------------------------------------------------------------
    // 1. parser matrix
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("SizingKeyword parser")
    class ParserTests {

        @Test
        @DisplayName("min-content / max-content / stretch parse to their keywords")
        void parsesSimpleKeywords() {
            SizingKeyword min = SizingKeyword.parse(TaffyDimension.minContent());
            assertEquals(SizingKeyword.Kind.MIN_CONTENT, min.getKind());
            assertTrue(min.isMinContent());
            assertFalse(min.isMaxContent());
            assertFalse(min.isFitContent());
            assertFalse(min.isStretch());
            assertFalse(min.hasFitContentLimit());

            SizingKeyword max = SizingKeyword.parse(TaffyDimension.maxContent());
            assertEquals(SizingKeyword.Kind.MAX_CONTENT, max.getKind());
            assertTrue(max.isMaxContent());

            SizingKeyword stretch = SizingKeyword.parse(TaffyDimension.stretch());
            assertEquals(SizingKeyword.Kind.STRETCH, stretch.getKind());
            assertTrue(stretch.isStretch());
        }

        @Test
        @DisplayName("bare fit-content parses without a limit")
        void parsesBareFitContent() {
            SizingKeyword keyword = SizingKeyword.parse(TaffyDimension.fitContent());
            assertEquals(SizingKeyword.Kind.FIT_CONTENT, keyword.getKind());
            assertTrue(keyword.isFitContent());
            assertFalse(keyword.hasFitContentLimit());
            assertNull(keyword.getFitContentLimit());
            assertEquals("fit-content", keyword.toString());
        }

        @Test
        @DisplayName("fit-content(length) parses with its limit")
        void parsesFitContentLengthLimit() {
            LengthPercentage limit = LengthPercentage.length(40f);
            SizingKeyword keyword = SizingKeyword.parse(TaffyDimension.fitContent(limit));

            assertEquals(SizingKeyword.Kind.FIT_CONTENT, keyword.getKind());
            assertTrue(keyword.hasFitContentLimit());
            assertEquals(limit, keyword.getFitContentLimit());
            assertEquals("fit-content(40.0px)", keyword.toString());
        }

        @Test
        @DisplayName("fit-content(calc) parses with its limit")
        void parsesFitContentCalcLimit() {
            CalcExpression calc = CalcExpression.percentPlusLength(0.5f, 10f);
            TaffyDimension dimension = TaffyDimension.fitContent(LengthPercentage.calc(calc));
            SizingKeyword keyword = SizingKeyword.parse(dimension);

            assertTrue(keyword.hasFitContentLimit());
            assertEquals(LengthPercentage.calc(calc), keyword.getFitContentLimit());
            assertEquals("fit-content(calc(...))", keyword.toString());
            assertEquals(dimension, TaffyDimension.fitContent(LengthPercentage.calc(calc)));
        }

        @Test
        @DisplayName("non-keyword dimensions and content do not parse")
        void nonKeywordsParseToNull() {
            assertNull(SizingKeyword.parse(null));
            assertNull(SizingKeyword.parse(TaffyDimension.length(10f)));
            assertNull(SizingKeyword.parse(TaffyDimension.percent(0.5f)));
            assertNull(SizingKeyword.parse(TaffyDimension.auto()));
            assertNull(SizingKeyword.parse(TaffyDimension.calc(CalcExpression.fullMinusLength(5f))));
            // content is only valid for flex-basis; it is not a sizing keyword.
            assertNull(SizingKeyword.parse(TaffyDimension.content()));
        }

        @Test
        @DisplayName("fit-content(limit) rejects a null limit")
        void fitContentRejectsNullLimit() {
            assertThrows(NullPointerException.class, () -> TaffyDimension.fitContent((LengthPercentage) null));
            assertThrows(NullPointerException.class, () -> SizingKeyword.fitContent((LengthPercentage) null));
        }

        @Test
        @DisplayName("keyword equality distinguishes fit-content limits")
        void keywordEqualityDistinguishesLimits() {
            SizingKeyword bare = SizingKeyword.fitContent();
            SizingKeyword forty = SizingKeyword.fitContent(LengthPercentage.length(40f));
            SizingKeyword fortyAgain = SizingKeyword.fitContent(LengthPercentage.length(40f));
            SizingKeyword fifty = SizingKeyword.fitContent(LengthPercentage.length(50f));

            assertEquals(bare, SizingKeyword.parse(TaffyDimension.fitContent()));
            assertEquals(forty, fortyAgain);
            assertEquals(forty.hashCode(), fortyAgain.hashCode());
            assertNotEquals(bare, forty);
            assertNotEquals(forty, fifty);
            assertNotEquals(forty, SizingKeyword.minContent());
        }
    }

    // ------------------------------------------------------------------
    // 2. Exact / Measure resolution matrix
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Exact / Measure resolution")
    class ResolutionTests {

        @Test
        @DisplayName("min-content and max-content always request measurement")
        void intrinsicKeywordsRequestMeasurement() {
            SizingKeyword.Resolution min = SizingKeyword.minContent().resolve(Float.NaN, Float.NaN);
            assertEquals(SizingKeyword.Resolution.Type.MEASURE, min.getType());
            assertTrue(min.getMeasureAvailableSpace().isMinContent());

            SizingKeyword.Resolution max = SizingKeyword.maxContent().resolve(Float.NaN, Float.NaN);
            assertTrue(max.getMeasureAvailableSpace().isMaxContent());
        }

        @Test
        @DisplayName("stretch resolves to an exact size when the stretch size is known")
        void stretchResolvesExactly() {
            SizingKeyword.Resolution resolution = SizingKeyword.stretch().resolve(120f, Float.NaN);
            assertEquals(SizingKeyword.Resolution.Type.EXACT, resolution.getType());
            assertTrue(resolution.isExact());
            assertEquals(120f, resolution.getExactValue(), EPSILON);
        }

        @Test
        @DisplayName("stretch without a stretch size is unresolvable (behaves as auto)")
        void stretchWithoutStretchSizeIsUnresolvable() {
            assertNull(SizingKeyword.stretch().resolve(Float.NaN, 300f));
            assertNull(SizingKeyword.resolve(TaffyDimension.stretch(), Float.NaN, 300f));
        }

        @Test
        @DisplayName("bare fit-content measures under the stretch size")
        void bareFitContentMeasuresUnderStretchSize() {
            SizingKeyword.Resolution resolution = SizingKeyword.fitContent().resolve(250f, Float.NaN);
            assertTrue(resolution.isMeasure());
            assertTrue(resolution.getMeasureAvailableSpace().isDefinite());
            assertEquals(250f, resolution.getMeasureAvailableSpace().getValue(), EPSILON);

            assertNull(SizingKeyword.fitContent().resolve(Float.NaN, 300f));
        }

        @Test
        @DisplayName("fit-content(length) measures under the length regardless of basis")
        void fitContentLengthMeasuresUnderLength() {
            SizingKeyword keyword = SizingKeyword.fitContent(LengthPercentage.length(40f));
            for (float basis : new float[]{Float.NaN, 0f, 500f}) {
                SizingKeyword.Resolution resolution = keyword.resolve(Float.NaN, basis);
                assertTrue(resolution.isMeasure());
                assertEquals(40f, resolution.getMeasureAvailableSpace().getValue(), EPSILON);
            }
        }

        @Test
        @DisplayName("fit-content(percent) resolves against the percentage basis")
        void fitContentPercentResolvesAgainstBasis() {
            SizingKeyword keyword = SizingKeyword.fitContent(LengthPercentage.percent(0.5f));

            SizingKeyword.Resolution resolution = keyword.resolve(Float.NaN, 200f);
            assertTrue(resolution.isMeasure());
            assertEquals(100f, resolution.getMeasureAvailableSpace().getValue(), EPSILON);

            assertNull(keyword.resolve(Float.NaN, Float.NaN),
                    "an unresolvable percentage limit behaves as auto/content");
        }

        @Test
        @DisplayName("fit-content(calc) resolves against the percentage basis")
        void fitContentCalcResolvesAgainstBasis() {
            CalcExpression calc = CalcExpression.percentPlusLength(0.5f, 10f);
            SizingKeyword keyword = SizingKeyword.fitContent(LengthPercentage.calc(calc));

            SizingKeyword.Resolution resolution = keyword.resolve(Float.NaN, 100f);
            assertTrue(resolution.isMeasure());
            assertEquals(60f, resolution.getMeasureAvailableSpace().getValue(), EPSILON);

            assertNull(keyword.resolve(Float.NaN, Float.NaN),
                    "an unresolvable calc limit behaves as auto/content");
        }

        @Test
        @DisplayName("SizingKeyword.resolve parses and resolves in one step")
        void resolveConvenienceParsesFirst() {
            SizingKeyword.Resolution resolution = SizingKeyword.resolve(
                    TaffyDimension.maxContent(), 200f, 500f);
            assertTrue(resolution.getMeasureAvailableSpace().isMaxContent());

            assertNull(SizingKeyword.resolve(TaffyDimension.length(10f), 200f, 500f));
            assertNull(SizingKeyword.resolve(TaffyDimension.content(), 200f, 500f));
            assertNull(SizingKeyword.resolve(null, 200f, 500f));
        }

        @Test
        @DisplayName("resolution accessors enforce their Exact/Measure kind")
        void resolutionAccessorsEnforceKind() {
            SizingKeyword.Resolution exact = SizingKeyword.Resolution.exact(30f);
            SizingKeyword.Resolution measure = SizingKeyword.Resolution.measure(AvailableSpace.minContent());

            assertEquals(30f, exact.getExactValue(), EPSILON);
            assertTrue(measure.getMeasureAvailableSpace().isMinContent());
            assertThrows(IllegalStateException.class, exact::getMeasureAvailableSpace);
            assertThrows(IllegalStateException.class, measure::getExactValue);
            assertThrows(NullPointerException.class, () -> SizingKeyword.Resolution.measure(null));
        }

        @Test
        @DisplayName("resolution equality and rendering")
        void resolutionEqualityAndRendering() {
            assertEquals(SizingKeyword.Resolution.exact(30f), SizingKeyword.Resolution.exact(30f));
            assertEquals(SizingKeyword.Resolution.exact(30f).hashCode(),
                    SizingKeyword.Resolution.exact(30f).hashCode());
            assertNotEquals(SizingKeyword.Resolution.exact(30f), SizingKeyword.Resolution.exact(31f));
            assertEquals("Exact(30.0)",
                    SizingKeyword.Resolution.exact(30f).toString());
            assertEquals("Measure(MinContent)",
                    SizingKeyword.Resolution.measure(AvailableSpace.minContent()).toString());
        }
    }

    // ------------------------------------------------------------------
    // 3. TaffyDimension additive API and legacy behavior
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("TaffyDimension sizing-keyword API")
    class DimensionApiTests {

        @Test
        @DisplayName("fit-content(limit) exposes its limit additively")
        void fitContentWithLimitExposesLimit() {
            LengthPercentage limit = LengthPercentage.percent(0.4f);
            TaffyDimension dimension = TaffyDimension.fitContent(limit);

            assertEquals(TaffyDimension.Type.FIT_CONTENT, dimension.getType());
            assertTrue(dimension.hasFitContentLimit());
            assertEquals(limit, dimension.getFitContentLimit());
            assertTrue(dimension.isFitContent());
            assertTrue(dimension.isSizingKeyword());
            assertTrue(dimension.requiresMeasurement());
            assertEquals("fit-content(40.0%)", dimension.toString());

            assertFalse(TaffyDimension.fitContent().hasFitContentLimit());
            assertNull(TaffyDimension.fitContent().getFitContentLimit());
            assertEquals(TaffyDimension.fitContent(), TaffyDimension.FIT_CONTENT);
        }

        @Test
        @DisplayName("fit-content limits participate in equality and hash codes")
        void fitContentLimitEquality() {
            TaffyDimension forty = TaffyDimension.fitContent(LengthPercentage.length(40f));
            TaffyDimension fortyAgain = TaffyDimension.fitContent(LengthPercentage.length(40f));
            TaffyDimension fifty = TaffyDimension.fitContent(LengthPercentage.length(50f));

            assertEquals(forty, fortyAgain);
            assertEquals(forty.hashCode(), fortyAgain.hashCode());
            assertNotEquals(forty, fifty);
            assertNotEquals(forty, TaffyDimension.fitContent());
            assertNotEquals(forty, TaffyDimension.length(40f));
        }

        @Test
        @DisplayName("isSizingKeyword matches the upstream keyword set")
        void isSizingKeywordMatchesUpstreamSet() {
            assertTrue(TaffyDimension.minContent().isSizingKeyword());
            assertTrue(TaffyDimension.maxContent().isSizingKeyword());
            assertTrue(TaffyDimension.fitContent().isSizingKeyword());
            assertTrue(TaffyDimension.fitContent(LengthPercentage.length(10f)).isSizingKeyword());
            assertTrue(TaffyDimension.stretch().isSizingKeyword());
            assertFalse(TaffyDimension.content().isSizingKeyword());
            assertFalse(TaffyDimension.length(10f).isSizingKeyword());
            assertFalse(TaffyDimension.auto().isSizingKeyword());
        }

        @Test
        @DisplayName("requiresMeasurement is true only for measured keywords")
        void requiresMeasurementClassification() {
            assertTrue(TaffyDimension.minContent().requiresMeasurement());
            assertTrue(TaffyDimension.maxContent().requiresMeasurement());
            assertTrue(TaffyDimension.fitContent().requiresMeasurement());
            assertTrue(TaffyDimension.fitContent(LengthPercentage.length(10f)).requiresMeasurement());
            assertFalse(TaffyDimension.stretch().requiresMeasurement());
            assertFalse(TaffyDimension.content().requiresMeasurement());
            assertFalse(TaffyDimension.length(10f).requiresMeasurement());
            assertFalse(TaffyDimension.percent(0.5f).requiresMeasurement());
            assertFalse(TaffyDimension.auto().requiresMeasurement());
        }

        @Test
        @DisplayName("legacy maybeResolve keeps returning NaN for every keyword")
        void legacyMaybeResolveKeepsNaN() {
            assertTrue(Float.isNaN(TaffyDimension.minContent().maybeResolve(100f)));
            assertTrue(Float.isNaN(TaffyDimension.maxContent().maybeResolve(100f)));
            assertTrue(Float.isNaN(TaffyDimension.fitContent().maybeResolve(100f)));
            assertTrue(Float.isNaN(
                    TaffyDimension.fitContent(LengthPercentage.length(40f)).maybeResolve(100f)));
            assertTrue(Float.isNaN(TaffyDimension.stretch().maybeResolve(100f)));
            assertTrue(Float.isNaN(TaffyDimension.content().maybeResolve(100f)));
            assertEquals(0f, TaffyDimension.fitContent(LengthPercentage.length(40f)).resolveOrZero(100f));
        }

        @Test
        @DisplayName("legacy concrete values are unchanged (100% and px)")
        void legacyConcreteValuesUnchanged() {
            assertEquals(100f, TaffyDimension.percent(1f).maybeResolve(100f), EPSILON);
            assertEquals(50f, TaffyDimension.percent(0.5f).maybeResolve(100f), EPSILON);
            assertEquals(10f, TaffyDimension.length(10f).maybeResolve(Float.NaN), EPSILON);
            assertTrue(Float.isNaN(TaffyDimension.auto().maybeResolve(100f)));
            assertSame(TaffyDimension.fitContent(), TaffyDimension.fitContent());
        }
    }
}
