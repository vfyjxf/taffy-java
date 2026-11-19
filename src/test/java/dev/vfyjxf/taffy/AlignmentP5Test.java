package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AlignSelf;
import dev.vfyjxf.taffy.style.JustifyContent;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.util.AlignmentHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the first P5 style batch: the appended alignment keywords (ST-02 safe / ST-03
 * self-start/self-end / ST-04 normal) and the shared {@link AlignmentHelper}.
 *
 * <p>Scope: this batch only adds the enum surface and the helper. No layout algorithm is wired
 * to the helper yet, so the tests are deliberately helper-level (plus the style-enum conversion
 * that must stay behaviour-equivalent). Layout behaviour is covered by the existing generated
 * suite and by the later P5 batches that connect the helper.</p>
 */
@DisplayName("Alignment P5: keyword/safety model and shared helper")
public class AlignmentP5Test {

    // ------------------------------------------------------------------
    // 1. Enum surface: appended constants keep the legacy ordinals
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("enum append contract (Q2: append only, legacy ordinals stable)")
    class EnumAppendContract {

        @Test
        @DisplayName("AlignItems appends NORMAL/SELF_*/SAFE_* after the legacy 8 constants")
        void alignItemsAppendOrder() {
            assertEquals(18, AlignItems.values().length);
            // Legacy ordinals must not move: binary/serialized compatibility (policy S2/Q2).
            assertEquals(0, AlignItems.AUTO.ordinal());
            assertEquals(1, AlignItems.START.ordinal());
            assertEquals(2, AlignItems.END.ordinal());
            assertEquals(3, AlignItems.FLEX_START.ordinal());
            assertEquals(4, AlignItems.FLEX_END.ordinal());
            assertEquals(5, AlignItems.CENTER.ordinal());
            assertEquals(6, AlignItems.BASELINE.ordinal());
            assertEquals(7, AlignItems.STRETCH.ordinal());
            // New keywords are appended in upstream constant order.
            assertEquals(8, AlignItems.NORMAL.ordinal());
            assertEquals(9, AlignItems.SELF_START.ordinal());
            assertEquals(10, AlignItems.SELF_END.ordinal());
            assertEquals(11, AlignItems.SAFE_START.ordinal());
            assertEquals(12, AlignItems.SAFE_END.ordinal());
            assertEquals(13, AlignItems.SAFE_FLEX_START.ordinal());
            assertEquals(14, AlignItems.SAFE_FLEX_END.ordinal());
            assertEquals(15, AlignItems.SAFE_CENTER.ordinal());
            assertEquals(16, AlignItems.SAFE_SELF_START.ordinal());
            assertEquals(17, AlignItems.SAFE_SELF_END.ordinal());
        }

        @Test
        @DisplayName("AlignSelf appends NORMAL/SELF_*/SAFE_* after the PA-02 START/END pair")
        void alignSelfAppendOrder() {
            assertEquals(18, AlignSelf.values().length);
            assertEquals(0, AlignSelf.AUTO.ordinal());
            assertEquals(1, AlignSelf.FLEX_START.ordinal());
            assertEquals(2, AlignSelf.FLEX_END.ordinal());
            assertEquals(3, AlignSelf.CENTER.ordinal());
            assertEquals(4, AlignSelf.BASELINE.ordinal());
            assertEquals(5, AlignSelf.STRETCH.ordinal());
            assertEquals(6, AlignSelf.START.ordinal());
            assertEquals(7, AlignSelf.END.ordinal());
            assertEquals(8, AlignSelf.NORMAL.ordinal());
            assertEquals(9, AlignSelf.SELF_START.ordinal());
            assertEquals(10, AlignSelf.SELF_END.ordinal());
            assertEquals(11, AlignSelf.SAFE_START.ordinal());
            assertEquals(12, AlignSelf.SAFE_END.ordinal());
            assertEquals(13, AlignSelf.SAFE_FLEX_START.ordinal());
            assertEquals(14, AlignSelf.SAFE_FLEX_END.ordinal());
            assertEquals(15, AlignSelf.SAFE_CENTER.ordinal());
            assertEquals(16, AlignSelf.SAFE_SELF_START.ordinal());
            assertEquals(17, AlignSelf.SAFE_SELF_END.ordinal());
        }

        @Test
        @DisplayName("AlignContent appends NORMAL/SAFE_* after the legacy 10 constants")
        void alignContentAppendOrder() {
            assertEquals(16, AlignContent.values().length);
            assertEquals(0, AlignContent.AUTO.ordinal());
            assertEquals(1, AlignContent.START.ordinal());
            assertEquals(2, AlignContent.END.ordinal());
            assertEquals(3, AlignContent.FLEX_START.ordinal());
            assertEquals(4, AlignContent.FLEX_END.ordinal());
            assertEquals(5, AlignContent.CENTER.ordinal());
            assertEquals(6, AlignContent.STRETCH.ordinal());
            assertEquals(7, AlignContent.SPACE_BETWEEN.ordinal());
            assertEquals(8, AlignContent.SPACE_EVENLY.ordinal());
            assertEquals(9, AlignContent.SPACE_AROUND.ordinal());
            assertEquals(10, AlignContent.NORMAL.ordinal());
            assertEquals(11, AlignContent.SAFE_START.ordinal());
            assertEquals(12, AlignContent.SAFE_END.ordinal());
            assertEquals(13, AlignContent.SAFE_FLEX_START.ordinal());
            assertEquals(14, AlignContent.SAFE_FLEX_END.ordinal());
            assertEquals(15, AlignContent.SAFE_CENTER.ordinal());
        }

        @Test
        @DisplayName("JustifyContent appends NORMAL/SAFE_* after the legacy 9 constants")
        void justifyContentAppendOrder() {
            assertEquals(15, JustifyContent.values().length);
            assertEquals(0, JustifyContent.FLEX_START.ordinal());
            assertEquals(1, JustifyContent.FLEX_END.ordinal());
            assertEquals(2, JustifyContent.CENTER.ordinal());
            assertEquals(3, JustifyContent.SPACE_BETWEEN.ordinal());
            assertEquals(4, JustifyContent.SPACE_AROUND.ordinal());
            assertEquals(5, JustifyContent.SPACE_EVENLY.ordinal());
            assertEquals(6, JustifyContent.START.ordinal());
            assertEquals(7, JustifyContent.END.ordinal());
            assertEquals(8, JustifyContent.STRETCH.ordinal());
            assertEquals(9, JustifyContent.NORMAL.ordinal());
            assertEquals(10, JustifyContent.SAFE_START.ordinal());
            assertEquals(11, JustifyContent.SAFE_END.ordinal());
            assertEquals(12, JustifyContent.SAFE_FLEX_START.ordinal());
            assertEquals(13, JustifyContent.SAFE_FLEX_END.ordinal());
            assertEquals(14, JustifyContent.SAFE_CENTER.ordinal());
        }

        @Test
        @DisplayName("AUTO is still the non-null unset sentinel and NORMAL is a distinct explicit value")
        void autoAndNormalAreDistinct() {
            assertNotEquals(AlignItems.AUTO, AlignItems.NORMAL);
            assertNotEquals(AlignSelf.AUTO, AlignSelf.NORMAL);
            assertNotEquals(AlignContent.AUTO, AlignContent.NORMAL);
            assertNotEquals(0, AlignItems.NORMAL.ordinal());
            assertNotEquals(0, AlignContent.NORMAL.ordinal());
        }

        @Test
        @DisplayName("AlignSelf.fromAlignItems/toAlignItems preserve the new keyword and safety")
        void alignSelfConversionsRoundTrip() {
            assertEquals(AlignSelf.NORMAL, AlignSelf.fromAlignItems(AlignItems.NORMAL));
            assertEquals(AlignSelf.SELF_START, AlignSelf.fromAlignItems(AlignItems.SELF_START));
            assertEquals(AlignSelf.SELF_END, AlignSelf.fromAlignItems(AlignItems.SELF_END));
            assertEquals(AlignSelf.SAFE_START, AlignSelf.fromAlignItems(AlignItems.SAFE_START));
            assertEquals(AlignSelf.SAFE_END, AlignSelf.fromAlignItems(AlignItems.SAFE_END));
            assertEquals(AlignSelf.SAFE_FLEX_START, AlignSelf.fromAlignItems(AlignItems.SAFE_FLEX_START));
            assertEquals(AlignSelf.SAFE_FLEX_END, AlignSelf.fromAlignItems(AlignItems.SAFE_FLEX_END));
            assertEquals(AlignSelf.SAFE_CENTER, AlignSelf.fromAlignItems(AlignItems.SAFE_CENTER));
            assertEquals(AlignSelf.SAFE_SELF_START, AlignSelf.fromAlignItems(AlignItems.SAFE_SELF_START));
            assertEquals(AlignSelf.SAFE_SELF_END, AlignSelf.fromAlignItems(AlignItems.SAFE_SELF_END));

            assertEquals(AlignItems.NORMAL, AlignSelf.NORMAL.toAlignItems());
            assertEquals(AlignItems.SELF_START, AlignSelf.SELF_START.toAlignItems());
            assertEquals(AlignItems.SELF_END, AlignSelf.SELF_END.toAlignItems());
            assertEquals(AlignItems.SAFE_START, AlignSelf.SAFE_START.toAlignItems());
            assertEquals(AlignItems.SAFE_END, AlignSelf.SAFE_END.toAlignItems());
            assertEquals(AlignItems.SAFE_FLEX_START, AlignSelf.SAFE_FLEX_START.toAlignItems());
            assertEquals(AlignItems.SAFE_FLEX_END, AlignSelf.SAFE_FLEX_END.toAlignItems());
            assertEquals(AlignItems.SAFE_CENTER, AlignSelf.SAFE_CENTER.toAlignItems());
            assertEquals(AlignItems.SAFE_SELF_START, AlignSelf.SAFE_SELF_START.toAlignItems());
            assertEquals(AlignItems.SAFE_SELF_END, AlignSelf.SAFE_SELF_END.toAlignItems());
        }

        @Test
        @DisplayName("style justify-content conversion preserves NORMAL and SAFE_*")
        void styleJustifyContentConversion() {
            TaffyStyle style = new TaffyStyle();
            style.justifyContent = AlignContent.NORMAL;
            assertEquals(JustifyContent.NORMAL, style.getJustifyContent());
            style.justifyContent = AlignContent.SAFE_START;
            assertEquals(JustifyContent.SAFE_START, style.getJustifyContent());
            style.justifyContent = AlignContent.SAFE_END;
            assertEquals(JustifyContent.SAFE_END, style.getJustifyContent());
            style.justifyContent = AlignContent.SAFE_FLEX_START;
            assertEquals(JustifyContent.SAFE_FLEX_START, style.getJustifyContent());
            style.justifyContent = AlignContent.SAFE_FLEX_END;
            assertEquals(JustifyContent.SAFE_FLEX_END, style.getJustifyContent());
            style.justifyContent = AlignContent.SAFE_CENTER;
            assertEquals(JustifyContent.SAFE_CENTER, style.getJustifyContent());
            // Legacy AUTO behavior is unchanged (flex default flex-start).
            style.justifyContent = AlignContent.AUTO;
            assertEquals(JustifyContent.FLEX_START, style.getJustifyContent());
        }
    }

    // ------------------------------------------------------------------
    // 2. keyword/safety accessors
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("keyword/safety accessors")
    class KeywordSafetyAccessors {

        @Test
        @DisplayName("isSafe recognises exactly the SAFE_* constants per enum")
        void isSafeMatrix() {
            List.of(AlignItems.SAFE_START, AlignItems.SAFE_END, AlignItems.SAFE_FLEX_START,
                            AlignItems.SAFE_FLEX_END, AlignItems.SAFE_CENTER,
                            AlignItems.SAFE_SELF_START, AlignItems.SAFE_SELF_END)
                    .forEach(value -> assertTrue(AlignmentHelper.isSafe(value), value.name()));
            List.of(AlignItems.AUTO, AlignItems.NORMAL, AlignItems.START, AlignItems.END,
                            AlignItems.FLEX_START, AlignItems.FLEX_END, AlignItems.SELF_START,
                            AlignItems.SELF_END, AlignItems.CENTER, AlignItems.BASELINE,
                            AlignItems.STRETCH)
                    .forEach(value -> assertFalse(AlignmentHelper.isSafe(value), value.name()));

            List.of(AlignSelf.SAFE_START, AlignSelf.SAFE_END, AlignSelf.SAFE_FLEX_START,
                            AlignSelf.SAFE_FLEX_END, AlignSelf.SAFE_CENTER,
                            AlignSelf.SAFE_SELF_START, AlignSelf.SAFE_SELF_END)
                    .forEach(value -> assertTrue(AlignmentHelper.isSafe(value), value.name()));
            assertFalse(AlignmentHelper.isSafe(AlignSelf.AUTO));
            assertFalse(AlignmentHelper.isSafe(AlignSelf.NORMAL));

            List.of(AlignContent.SAFE_START, AlignContent.SAFE_END, AlignContent.SAFE_FLEX_START,
                            AlignContent.SAFE_FLEX_END, AlignContent.SAFE_CENTER)
                    .forEach(value -> assertTrue(AlignmentHelper.isSafe(value), value.name()));
            assertFalse(AlignmentHelper.isSafe(AlignContent.AUTO));
            assertFalse(AlignmentHelper.isSafe(AlignContent.NORMAL));

            List.of(JustifyContent.SAFE_START, JustifyContent.SAFE_END,
                            JustifyContent.SAFE_FLEX_START, JustifyContent.SAFE_FLEX_END,
                            JustifyContent.SAFE_CENTER)
                    .forEach(value -> assertTrue(AlignmentHelper.isSafe(value), value.name()));
            assertFalse(AlignmentHelper.isSafe(JustifyContent.NORMAL));
        }

        @Test
        @DisplayName("keyword() strips the safe modifier and keeps self keywords")
        void keywordStripsSafety() {
            assertEquals(AlignItems.START, AlignmentHelper.keyword(AlignItems.SAFE_START));
            assertEquals(AlignItems.END, AlignmentHelper.keyword(AlignItems.SAFE_END));
            assertEquals(AlignItems.FLEX_START, AlignmentHelper.keyword(AlignItems.SAFE_FLEX_START));
            assertEquals(AlignItems.FLEX_END, AlignmentHelper.keyword(AlignItems.SAFE_FLEX_END));
            assertEquals(AlignItems.CENTER, AlignmentHelper.keyword(AlignItems.SAFE_CENTER));
            assertEquals(AlignItems.SELF_START, AlignmentHelper.keyword(AlignItems.SAFE_SELF_START));
            assertEquals(AlignItems.SELF_END, AlignmentHelper.keyword(AlignItems.SAFE_SELF_END));
            assertEquals(AlignItems.SELF_START, AlignmentHelper.keyword(AlignItems.SELF_START));
            assertEquals(AlignItems.NORMAL, AlignmentHelper.keyword(AlignItems.NORMAL));
            assertEquals(AlignItems.AUTO, AlignmentHelper.keyword(AlignItems.AUTO));

            assertEquals(AlignSelf.SELF_END, AlignmentHelper.keyword(AlignSelf.SAFE_SELF_END));
            assertEquals(AlignSelf.SELF_START, AlignmentHelper.keyword(AlignSelf.SAFE_SELF_START));

            assertEquals(AlignContent.START, AlignmentHelper.keyword(AlignContent.SAFE_START));
            assertEquals(AlignContent.END, AlignmentHelper.keyword(AlignContent.SAFE_END));
            assertEquals(AlignContent.FLEX_START, AlignmentHelper.keyword(AlignContent.SAFE_FLEX_START));
            assertEquals(AlignContent.FLEX_END, AlignmentHelper.keyword(AlignContent.SAFE_FLEX_END));
            assertEquals(AlignContent.CENTER, AlignmentHelper.keyword(AlignContent.SAFE_CENTER));
            assertEquals(AlignContent.NORMAL, AlignmentHelper.keyword(AlignContent.NORMAL));

            assertEquals(JustifyContent.START, AlignmentHelper.keyword(JustifyContent.SAFE_START));
            assertEquals(JustifyContent.END, AlignmentHelper.keyword(JustifyContent.SAFE_END));
            assertEquals(JustifyContent.FLEX_START, AlignmentHelper.keyword(JustifyContent.SAFE_FLEX_START));
            assertEquals(JustifyContent.FLEX_END, AlignmentHelper.keyword(JustifyContent.SAFE_FLEX_END));
            assertEquals(JustifyContent.CENTER, AlignmentHelper.keyword(JustifyContent.SAFE_CENTER));
        }

        @Test
        @DisplayName("isNormal/isUnset separate explicit normal from the AUTO sentinel")
        void normalVersusUnset() {
            assertTrue(AlignmentHelper.isNormal(AlignItems.NORMAL));
            assertFalse(AlignmentHelper.isNormal(AlignItems.AUTO));
            assertTrue(AlignmentHelper.isUnset(AlignItems.AUTO));
            assertFalse(AlignmentHelper.isUnset(AlignItems.NORMAL));
            assertTrue(AlignmentHelper.isNormal(AlignSelf.NORMAL));
            assertTrue(AlignmentHelper.isUnset(AlignSelf.AUTO));
            assertTrue(AlignmentHelper.isNormal(AlignContent.NORMAL));
            assertTrue(AlignmentHelper.isUnset(AlignContent.AUTO));
            assertTrue(AlignmentHelper.isNormal(JustifyContent.NORMAL));
        }
    }

    // ------------------------------------------------------------------
    // 3. normal resolution by layout context
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("normal resolution by layout context")
    class NormalResolution {

        @Test
        @DisplayName("explicit NORMAL resolves to the context keyword")
        void explicitNormalResolves() {
            assertEquals(AlignItems.STRETCH, AlignmentHelper.resolveNormal(AlignItems.NORMAL, AlignItems.STRETCH));
            assertEquals(AlignItems.START, AlignmentHelper.resolveNormal(AlignItems.NORMAL, AlignItems.START));
            assertEquals(AlignSelf.STRETCH, AlignmentHelper.resolveNormal(AlignSelf.NORMAL, AlignSelf.STRETCH));
            assertEquals(AlignSelf.START, AlignmentHelper.resolveNormal(AlignSelf.NORMAL, AlignSelf.START));
            assertEquals(AlignContent.STRETCH,
                    AlignmentHelper.resolveNormal(AlignContent.NORMAL, AlignContent.STRETCH));
            assertEquals(AlignContent.START,
                    AlignmentHelper.resolveNormal(AlignContent.NORMAL, AlignContent.START));
            assertEquals(JustifyContent.STRETCH,
                    AlignmentHelper.resolveNormal(JustifyContent.NORMAL, JustifyContent.STRETCH));
            assertEquals(JustifyContent.START,
                    AlignmentHelper.resolveNormal(JustifyContent.NORMAL, JustifyContent.START));
        }

        @Test
        @DisplayName("AUTO stays unset and is never treated as an explicit normal override")
        void autoIsNotOverridden() {
            assertEquals(AlignItems.AUTO, AlignmentHelper.resolveNormal(AlignItems.AUTO, AlignItems.STRETCH));
            assertEquals(AlignSelf.AUTO, AlignmentHelper.resolveNormal(AlignSelf.AUTO, AlignSelf.STRETCH));
            assertEquals(AlignContent.AUTO, AlignmentHelper.resolveNormal(AlignContent.AUTO, AlignContent.STRETCH));
        }

        @Test
        @DisplayName("non-normal values pass through unchanged")
        void otherKeywordsPassThrough() {
            assertEquals(AlignItems.START, AlignmentHelper.resolveNormal(AlignItems.START, AlignItems.STRETCH));
            assertEquals(AlignItems.SAFE_END, AlignmentHelper.resolveNormal(AlignItems.SAFE_END, AlignItems.STRETCH));
            assertEquals(AlignContent.SPACE_BETWEEN,
                    AlignmentHelper.resolveNormal(AlignContent.SPACE_BETWEEN, AlignContent.STRETCH));
            assertEquals(JustifyContent.SAFE_CENTER,
                    AlignmentHelper.resolveNormal(JustifyContent.SAFE_CENTER, JustifyContent.STRETCH));
        }
    }

    // ------------------------------------------------------------------
    // 4. self-start/self-end resolution
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("self-start/self-end resolution (upstream #1077)")
    class SelfRelative {

        @Test
        @DisplayName("inline axis flips self keywords when the item direction differs from the container")
        void inlineAxisFlips() {
            assertEquals(AlignItems.START, AlignmentHelper.resolveSelfRelative(
                    AlignItems.SELF_START, TaffyDirection.LTR, TaffyDirection.LTR, true));
            assertEquals(AlignItems.END, AlignmentHelper.resolveSelfRelative(
                    AlignItems.SELF_START, TaffyDirection.LTR, TaffyDirection.RTL, true));
            assertEquals(AlignItems.END, AlignmentHelper.resolveSelfRelative(
                    AlignItems.SELF_END, TaffyDirection.LTR, TaffyDirection.LTR, true));
            assertEquals(AlignItems.START, AlignmentHelper.resolveSelfRelative(
                    AlignItems.SELF_END, TaffyDirection.LTR, TaffyDirection.RTL, true));
            assertEquals(AlignItems.START, AlignmentHelper.resolveSelfRelative(
                    AlignItems.SELF_START, TaffyDirection.RTL, TaffyDirection.RTL, true));
            assertEquals(AlignItems.END, AlignmentHelper.resolveSelfRelative(
                    AlignItems.SELF_START, TaffyDirection.RTL, TaffyDirection.LTR, true));
        }

        @Test
        @DisplayName("block axis maps self keywords to plain start/end")
        void blockAxisDoesNotFlip() {
            assertEquals(AlignItems.START, AlignmentHelper.resolveSelfRelative(
                    AlignItems.SELF_START, TaffyDirection.LTR, TaffyDirection.RTL, false));
            assertEquals(AlignItems.END, AlignmentHelper.resolveSelfRelative(
                    AlignItems.SELF_END, TaffyDirection.RTL, TaffyDirection.LTR, false));
        }

        @Test
        @DisplayName("safe self keywords resolve to safe start/end and keep the safety flag")
        void safeSelfKeywordsPreserveSafety() {
            assertEquals(AlignItems.SAFE_START, AlignmentHelper.resolveSelfRelative(
                    AlignItems.SAFE_SELF_START, TaffyDirection.LTR, TaffyDirection.LTR, true));
            assertEquals(AlignItems.SAFE_END, AlignmentHelper.resolveSelfRelative(
                    AlignItems.SAFE_SELF_START, TaffyDirection.LTR, TaffyDirection.RTL, true));
            assertEquals(AlignItems.SAFE_END, AlignmentHelper.resolveSelfRelative(
                    AlignItems.SAFE_SELF_END, TaffyDirection.LTR, TaffyDirection.LTR, true));
            assertEquals(AlignItems.SAFE_START, AlignmentHelper.resolveSelfRelative(
                    AlignItems.SAFE_SELF_END, TaffyDirection.LTR, TaffyDirection.RTL, true));
        }

        @Test
        @DisplayName("non-self keywords are unchanged and INHERIT degrades to LTR")
        void otherKeywordsUnchanged() {
            assertEquals(AlignItems.START, AlignmentHelper.resolveSelfRelative(
                    AlignItems.START, TaffyDirection.LTR, TaffyDirection.RTL, true));
            assertEquals(AlignItems.FLEX_END, AlignmentHelper.resolveSelfRelative(
                    AlignItems.FLEX_END, TaffyDirection.LTR, TaffyDirection.RTL, true));
            assertEquals(AlignItems.NORMAL, AlignmentHelper.resolveSelfRelative(
                    AlignItems.NORMAL, TaffyDirection.LTR, TaffyDirection.RTL, true));
            // Java-only INHERIT has no direction of its own; it degrades to LTR until resolved.
            assertEquals(AlignItems.START, AlignmentHelper.resolveSelfRelative(
                    AlignItems.SELF_START, TaffyDirection.INHERIT, TaffyDirection.LTR, true));
        }

        @Test
        @DisplayName("AlignSelf variant resolves identically")
        void alignSelfVariant() {
            assertEquals(AlignSelf.START, AlignmentHelper.resolveSelfRelative(
                    AlignSelf.SELF_START, TaffyDirection.LTR, TaffyDirection.LTR, true));
            assertEquals(AlignSelf.END, AlignmentHelper.resolveSelfRelative(
                    AlignSelf.SELF_START, TaffyDirection.LTR, TaffyDirection.RTL, true));
            assertEquals(AlignSelf.SAFE_END, AlignmentHelper.resolveSelfRelative(
                    AlignSelf.SAFE_SELF_START, TaffyDirection.LTR, TaffyDirection.RTL, true));
            assertEquals(AlignSelf.SAFE_START, AlignmentHelper.resolveSelfRelative(
                    AlignSelf.SAFE_SELF_END, TaffyDirection.LTR, TaffyDirection.RTL, true));
            assertEquals(AlignSelf.AUTO, AlignmentHelper.resolveSelfRelative(
                    AlignSelf.AUTO, TaffyDirection.LTR, TaffyDirection.RTL, true));
        }
    }

    // ------------------------------------------------------------------
    // 5. safe self fallback
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("safe self-alignment overflow fallback (upstream #952)")
    class SelfSafetyFallback {

        @Test
        @DisplayName("safe values fall back to start only when the subject overflows")
        void safeOverflowFallsBackToStart() {
            assertEquals(AlignItems.START, AlignmentHelper.resolveSelfSafety(AlignItems.SAFE_END, true));
            assertEquals(AlignItems.END, AlignmentHelper.resolveSelfSafety(AlignItems.SAFE_END, false));
            assertEquals(AlignItems.START, AlignmentHelper.resolveSelfSafety(AlignItems.SAFE_CENTER, true));
            assertEquals(AlignItems.CENTER, AlignmentHelper.resolveSelfSafety(AlignItems.SAFE_CENTER, false));
            assertEquals(AlignItems.START, AlignmentHelper.resolveSelfSafety(AlignItems.SAFE_SELF_END, true));
            assertEquals(AlignItems.SELF_END, AlignmentHelper.resolveSelfSafety(AlignItems.SAFE_SELF_END, false));
            assertEquals(AlignItems.START, AlignmentHelper.resolveSelfSafety(AlignItems.SAFE_FLEX_START, true));
        }

        @Test
        @DisplayName("unsafe values are unaffected by overflow")
        void unsafeValuesUnchanged() {
            assertEquals(AlignItems.END, AlignmentHelper.resolveSelfSafety(AlignItems.END, true));
            assertEquals(AlignItems.CENTER, AlignmentHelper.resolveSelfSafety(AlignItems.CENTER, true));
            assertEquals(AlignItems.AUTO, AlignmentHelper.resolveSelfSafety(AlignItems.AUTO, true));
            assertEquals(AlignItems.NORMAL, AlignmentHelper.resolveSelfSafety(AlignItems.NORMAL, true));
        }

        @Test
        @DisplayName("AlignSelf variant follows the same fallback")
        void alignSelfVariant() {
            assertEquals(AlignSelf.START, AlignmentHelper.resolveSelfSafety(AlignSelf.SAFE_END, true));
            assertEquals(AlignSelf.END, AlignmentHelper.resolveSelfSafety(AlignSelf.SAFE_END, false));
            assertEquals(AlignSelf.START, AlignmentHelper.resolveSelfSafety(AlignSelf.SAFE_SELF_START, true));
            assertEquals(AlignSelf.SELF_START, AlignmentHelper.resolveSelfSafety(AlignSelf.SAFE_SELF_START, false));
        }
    }

    // ------------------------------------------------------------------
    // 6. content-alignment fallback
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("content-alignment fallback (upstream apply_alignment_fallback)")
    class ContentFallback {

        @Test
        @DisplayName("safe content alignment falls back to start on overflow")
        void safeContentFallsBackToStart() {
            assertEquals(AlignContent.START, AlignmentHelper.applyAlignmentFallback(AlignContent.SAFE_END, -10f, 3));
            assertEquals(AlignContent.END, AlignmentHelper.applyAlignmentFallback(AlignContent.SAFE_END, 10f, 3));
            assertEquals(AlignContent.START, AlignmentHelper.applyAlignmentFallback(AlignContent.SAFE_CENTER, -10f, 3));
            assertEquals(AlignContent.CENTER, AlignmentHelper.applyAlignmentFallback(AlignContent.SAFE_CENTER, 10f, 3));
            assertEquals(AlignContent.START,
                    AlignmentHelper.applyAlignmentFallback(AlignContent.SAFE_FLEX_START, -10f, 3));
            assertEquals(AlignContent.START, AlignmentHelper.applyAlignmentFallback(AlignContent.SAFE_START, -10f, 3));
            assertEquals(JustifyContent.START,
                    AlignmentHelper.applyAlignmentFallback(JustifyContent.SAFE_END, -10f, 3));
        }

        @Test
        @DisplayName("normal and the legacy AUTO sentinel behave as stretch in flex/grid")
        void normalAndAutoBehaveAsStretch() {
            assertEquals(AlignContent.STRETCH, AlignmentHelper.applyAlignmentFallback(AlignContent.NORMAL, 10f, 3));
            assertEquals(AlignContent.STRETCH, AlignmentHelper.applyAlignmentFallback(AlignContent.AUTO, 10f, 3));
            assertEquals(JustifyContent.STRETCH,
                    AlignmentHelper.applyAlignmentFallback(JustifyContent.NORMAL, 10f, 3));
        }

        @Test
        @DisplayName("distribution keywords use their positional fallback on a single subject")
        void singleSubjectFallbacks() {
            assertEquals(AlignContent.FLEX_START,
                    AlignmentHelper.applyAlignmentFallback(AlignContent.STRETCH, 10f, 1));
            // stretch falls back to flex-start without implicit safe, so overflow does not flip it.
            assertEquals(AlignContent.FLEX_START,
                    AlignmentHelper.applyAlignmentFallback(AlignContent.STRETCH, -10f, 1));
            assertEquals(AlignContent.FLEX_START,
                    AlignmentHelper.applyAlignmentFallback(AlignContent.SPACE_BETWEEN, 10f, 1));
            assertEquals(AlignContent.START,
                    AlignmentHelper.applyAlignmentFallback(AlignContent.SPACE_BETWEEN, -10f, 1));
            assertEquals(AlignContent.CENTER,
                    AlignmentHelper.applyAlignmentFallback(AlignContent.SPACE_AROUND, 10f, 1));
            assertEquals(AlignContent.START,
                    AlignmentHelper.applyAlignmentFallback(AlignContent.SPACE_AROUND, -10f, 1));
            assertEquals(AlignContent.CENTER,
                    AlignmentHelper.applyAlignmentFallback(AlignContent.SPACE_EVENLY, 10f, 1));
        }

        @Test
        @DisplayName("stretch fallback is not implicitly safe; space-* fallbacks are")
        void stretchIsNotImplicitlySafe() {
            // A non-safe value keeps the flex-start fallback even on overflow.
            assertEquals(AlignContent.FLEX_START,
                    AlignmentHelper.applyAlignmentFallback(AlignContent.STRETCH, -10f, 3));
            // The space-* fallbacks gain implicit safe semantics via the fallback flag.
            assertEquals(AlignContent.START,
                    AlignmentHelper.applyAlignmentFallback(AlignContent.SPACE_BETWEEN, -10f, 3));
            assertEquals(AlignContent.START,
                    AlignmentHelper.applyAlignmentFallback(AlignContent.SPACE_AROUND, -10f, 3));
            assertEquals(AlignContent.START,
                    AlignmentHelper.applyAlignmentFallback(AlignContent.SPACE_EVENLY, 0f, 3));
        }

        @Test
        @DisplayName("overflowing non-safe alignments keep their keyword")
        void unsafeKeywordsUnaffected() {
            assertEquals(AlignContent.END, AlignmentHelper.applyAlignmentFallback(AlignContent.END, -10f, 3));
            assertEquals(AlignContent.CENTER, AlignmentHelper.applyAlignmentFallback(AlignContent.CENTER, -10f, 3));
            assertEquals(JustifyContent.CENTER,
                    AlignmentHelper.applyAlignmentFallback(JustifyContent.CENTER, -10f, 3));
        }

        @Test
        @DisplayName("All JustifyContent fallbacks match the AlignContent overload")
        void justifyContentMatchesAlignContent() {
            for (float freeSpace : new float[]{-10f, 0f, 10f, 50f}) {
                for (int numItems : new int[]{0, 1, 2, 3}) {
                    for (AlignContent alignContent : AlignContent.values()) {
                        AlignContent expected = AlignmentHelper.applyAlignmentFallback(alignContent, freeSpace, numItems);
                        JustifyContent equivalent = AlignmentHelper.applyAlignmentFallback(
                                mapToJustify(alignContent), freeSpace, numItems);
                        assertEquals(expected.name(), equivalent.name(),
                                alignContent + " free=" + freeSpace + " items=" + numItems);
                    }
                }
            }
        }

        private JustifyContent mapToJustify(AlignContent alignment) {
            return switch (alignment) {
                case AUTO, NORMAL -> JustifyContent.NORMAL;
                case START -> JustifyContent.START;
                case END -> JustifyContent.END;
                case FLEX_START -> JustifyContent.FLEX_START;
                case FLEX_END -> JustifyContent.FLEX_END;
                case CENTER -> JustifyContent.CENTER;
                case STRETCH -> JustifyContent.STRETCH;
                case SPACE_BETWEEN -> JustifyContent.SPACE_BETWEEN;
                case SPACE_EVENLY -> JustifyContent.SPACE_EVENLY;
                case SPACE_AROUND -> JustifyContent.SPACE_AROUND;
                case SAFE_START -> JustifyContent.SAFE_START;
                case SAFE_END -> JustifyContent.SAFE_END;
                case SAFE_FLEX_START -> JustifyContent.SAFE_FLEX_START;
                case SAFE_FLEX_END -> JustifyContent.SAFE_FLEX_END;
                case SAFE_CENTER -> JustifyContent.SAFE_CENTER;
            };
        }
    }

    // ------------------------------------------------------------------
    // 7. alignment offset calculation
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("alignment offset calculation (upstream compute_alignment_offset)")
    class OffsetCalculation {

        @Test
        @DisplayName("first-subject offsets match the keyword geometry")
        void firstSubjectOffsets() {
            assertEquals(0f, offset(100f, 3, 0f, AlignContent.START, false, true));
            assertEquals(0f, offset(100f, 3, 0f, AlignContent.FLEX_START, false, true));
            assertEquals(100f, offset(100f, 3, 0f, AlignContent.FLEX_START, true, true));
            assertEquals(100f, offset(100f, 3, 0f, AlignContent.END, false, true));
            assertEquals(0f, offset(100f, 3, 0f, AlignContent.FLEX_END, true, true));
            assertEquals(100f, offset(100f, 3, 0f, AlignContent.FLEX_END, false, true));
            assertEquals(50f, offset(100f, 3, 0f, AlignContent.CENTER, false, true));
            assertEquals(0f, offset(100f, 3, 0f, AlignContent.STRETCH, false, true));
            assertEquals(0f, offset(100f, 3, 0f, AlignContent.SPACE_BETWEEN, false, true));
            assertEquals(0f, offset(100f, 3, 0f, AlignContent.NORMAL, false, true));
            assertEquals(0f, offset(100f, 3, 0f, AlignContent.AUTO, false, true));
        }

        @Test
        @DisplayName("space-* first offsets use n; negative free space splits in half")
        void firstSpaceOffsets() {
            assertEquals(15f, offset(90f, 3, 0f, AlignContent.SPACE_AROUND, false, true), 1e-6f);
            assertEquals(-5f, offset(-10f, 3, 0f, AlignContent.SPACE_AROUND, false, true), 1e-6f);
            assertEquals(20f, offset(80f, 3, 0f, AlignContent.SPACE_EVENLY, false, true), 1e-6f);
            assertEquals(-5f, offset(-10f, 3, 0f, AlignContent.SPACE_EVENLY, false, true), 1e-6f);
            // numItems == 0 is degenerate; the helper must not divide by zero.
            assertEquals(0f, offset(0f, 0, 0f, AlignContent.SPACE_AROUND, false, true), 1e-6f);
            assertEquals(0f, offset(0f, 0, 0f, AlignContent.SPACE_EVENLY, false, true), 1e-6f);
        }

        @Test
        @DisplayName("subsequent-subject deltas include the gap and distributed space")
        void subsequentOffsetsIncludeGap() {
            assertEquals(10f, offset(90f, 3, 10f, AlignContent.START, false, false), 1e-6f);
            assertEquals(55f, offset(90f, 3, 10f, AlignContent.SPACE_BETWEEN, false, false), 1e-6f);
            assertEquals(40f, offset(90f, 3, 10f, AlignContent.SPACE_AROUND, false, false), 1e-6f);
            assertEquals(32.5f, offset(90f, 3, 10f, AlignContent.SPACE_EVENLY, false, false), 1e-6f);
            // Negative free space is clamped to zero for the distributed part, gap still applies.
            assertEquals(10f, offset(-10f, 3, 10f, AlignContent.SPACE_BETWEEN, false, false), 1e-6f);
            assertEquals(10f, offset(-10f, 3, 10f, AlignContent.CENTER, false, false), 1e-6f);
            // Degenerate item counts never produce a division by zero.
            assertEquals(10f, offset(90f, 1, 10f, AlignContent.SPACE_BETWEEN, false, false), 1e-6f);
            assertEquals(10f, offset(90f, 0, 10f, AlignContent.SPACE_AROUND, false, false), 1e-6f);
        }

        @Test
        @DisplayName("safe keywords compute the same offset as their non-safe keyword")
        void safeKeywordOffsetsMatchNonSafe() {
            assertEquals(offset(100f, 3, 0f, AlignContent.CENTER, false, true),
                    offset(100f, 3, 0f, AlignContent.SAFE_CENTER, false, true));
            assertEquals(offset(100f, 3, 0f, AlignContent.END, false, true),
                    offset(100f, 3, 0f, AlignContent.SAFE_END, false, true));
            assertEquals(offset(100f, 3, 0f, AlignContent.FLEX_START, true, true),
                    offset(100f, 3, 0f, AlignContent.SAFE_FLEX_START, true, true));
        }

        @Test
        @DisplayName("numItems=1 fallback composes with the offset computation")
        void singleItemFallbackComposition() {
            AlignContent effective = AlignmentHelper.applyAlignmentFallback(AlignContent.SPACE_BETWEEN, 50f, 1);
            assertEquals(AlignContent.FLEX_START, effective);
            assertEquals(0f, offset(50f, 1, 0f, effective, false, true));
            // flex-start is flex-relative: it packs at the end when the layout is reversed.
            assertEquals(50f, offset(50f, 1, 0f, effective, true, true));

            AlignContent around = AlignmentHelper.applyAlignmentFallback(AlignContent.SPACE_AROUND, 50f, 1);
            assertEquals(AlignContent.CENTER, around);
            assertEquals(25f, offset(50f, 1, 0f, around, false, true), 1e-6f);

            AlignContent normal = AlignmentHelper.applyAlignmentFallback(AlignContent.NORMAL, 50f, 1);
            assertEquals(AlignContent.FLEX_START, normal);
            assertEquals(0f, offset(50f, 1, 0f, normal, false, true));
        }

        @Test
        @DisplayName("JustifyContent overload agrees with the AlignContent overload")
        void justifyContentOverloadAgrees() {
            assertEquals(55f, AlignmentHelper.computeAlignmentOffset(
                    90f, 3, 10f, JustifyContent.SPACE_BETWEEN, false, false), 1e-6f);
            assertEquals(50f, AlignmentHelper.computeAlignmentOffset(
                    100f, 3, 0f, JustifyContent.CENTER, false, true), 1e-6f);
            assertEquals(0f, AlignmentHelper.computeAlignmentOffset(
                    100f, 3, 0f, JustifyContent.NORMAL, false, true), 1e-6f);
        }

        private static float offset(float freeSpace, int numItems, float gap, AlignContent alignment,
                                    boolean reversed, boolean first) {
            return AlignmentHelper.computeAlignmentOffset(freeSpace, numItems, gap, alignment, reversed, first);
        }
    }

    // ------------------------------------------------------------------
    // 8. explicit normal override at the helper/style level
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("explicit normal override (helper level)")
    class ExplicitNormalOverride {

        @Test
        @DisplayName("default style AUTO stays unset; an explicit NORMAL overrides the context default")
        void styleAutoVersusExplicitNormal() {
            TaffyStyle style = new TaffyStyle();
            assertEquals(AlignItems.AUTO, style.alignItems);
            assertTrue(AlignmentHelper.isUnset(style.alignItems));
            assertEquals(AlignItems.AUTO,
                    AlignmentHelper.resolveNormal(style.alignItems, AlignItems.STRETCH));

            style.alignItems = AlignItems.NORMAL;
            assertFalse(AlignmentHelper.isUnset(style.alignItems));
            assertTrue(AlignmentHelper.isNormal(style.alignItems));
            assertEquals(AlignItems.STRETCH,
                    AlignmentHelper.resolveNormal(style.alignItems, AlignItems.STRETCH));
            assertEquals(AlignItems.START,
                    AlignmentHelper.resolveNormal(style.alignItems, AlignItems.START));
        }

        @Test
        @DisplayName("content normal resolves to stretch in flex/grid and start in block")
        void contentNormalByContext() {
            // flex/grid containers pass STRETCH as the context keyword.
            assertEquals(AlignContent.STRETCH,
                    AlignmentHelper.resolveNormal(AlignContent.NORMAL, AlignContent.STRETCH));
            // block containers pass START as the context keyword.
            assertEquals(AlignContent.START,
                    AlignmentHelper.resolveNormal(AlignContent.NORMAL, AlignContent.START));
            // AUTO (legacy unset) is not an explicit override and is left for the caller.
            assertEquals(AlignContent.AUTO,
                    AlignmentHelper.resolveNormal(AlignContent.AUTO, AlignContent.STRETCH));
            assertEquals(AlignContent.AUTO,
                    AlignmentHelper.resolveNormal(AlignContent.AUTO, AlignContent.START));
        }

        @Test
        @DisplayName("align-self inherits the explicit normal keyword through fromAlignItems")
        void alignSelfNormalInheritance() {
            assertEquals(AlignSelf.NORMAL, AlignSelf.fromAlignItems(AlignItems.NORMAL));
            assertEquals(AlignSelf.STRETCH,
                    AlignmentHelper.resolveNormal(AlignSelf.fromAlignItems(AlignItems.NORMAL), AlignSelf.STRETCH));
            // fromAlignItems keeps the legacy defensive mapping AUTO -> STRETCH.
            assertEquals(AlignSelf.STRETCH, AlignSelf.fromAlignItems(AlignItems.AUTO));
            // The AUTO sentinel itself stays unset when resolved directly.
            assertEquals(AlignSelf.AUTO,
                    AlignmentHelper.resolveNormal(AlignSelf.AUTO, AlignSelf.STRETCH));
        }
    }
}
