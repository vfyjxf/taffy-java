package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.TaffyDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P19: the context-free final-layout mark of {@link LayoutCache}.
 *
 * <p>A same-BFC block child can reuse its final layout only while the inherited
 * {@code BlockContext} holds no floats. Its entry is therefore marked, and the mark is only
 * visible to {@link LayoutCache#getContextFreeFinalEntry(LayoutInput, TaffyDirection)}. These
 * tests pin the exclusivity of that lookup: the generic {@link LayoutCache#get(LayoutInput)} must
 * never return a marked entry, because its callers cannot prove that their own context is
 * float-free.</p>
 */
@DisplayName("P19: context-free final layout cache entry")
class LayoutCacheContextFreeP19Test {

    private static final TaffyDirection LTR = TaffyDirection.LTR;
    private static final TaffyDirection RTL = TaffyDirection.RTL;

    private static LayoutInput finalLayoutInput() {
        return new LayoutInput(
            RunMode.PERFORM_LAYOUT,
            SizingMode.INHERENT_SIZE,
            RequestedAxis.BOTH,
            new FloatSize(300f, Float.NaN),
            new FloatSize(800f, 600f),
            TaffySize.of(AvailableSpace.definite(800f), AvailableSpace.definite(600f)),
            TaffyLine.FALSE
        );
    }

    private static LayoutInput measureInput() {
        return new LayoutInput(
            RunMode.COMPUTE_SIZE,
            SizingMode.INHERENT_SIZE,
            RequestedAxis.BOTH,
            new FloatSize(Float.NaN, Float.NaN),
            new FloatSize(800f, 600f),
            TaffySize.of(AvailableSpace.definite(800f), AvailableSpace.definite(600f)),
            TaffyLine.FALSE
        );
    }

    private static LayoutOutput output(float width, float height) {
        return LayoutOutput.fromOuterSize(new FloatSize(width, height));
    }

    @Test
    @DisplayName("an ordinary store is never context-free")
    void ordinaryStoreIsNeverContextFree() {
        LayoutCache cache = new LayoutCache();
        LayoutInput inputs = finalLayoutInput();
        LayoutOutput stored = output(10f, 20f);

        cache.store(inputs, stored);

        assertSame(stored, cache.get(inputs), "the generic lookup returns an ordinary entry");
        assertNull(cache.getContextFreeFinalEntry(inputs, LTR),
            "an ordinary store must not grant same-BFC reuse");
    }

    @Test
    @DisplayName("a context-free entry is only visible to the dedicated lookup")
    void contextFreeEntryIsHiddenFromTheGenericLookup() {
        LayoutCache cache = new LayoutCache();
        LayoutInput inputs = finalLayoutInput();
        LayoutOutput stored = output(30f, 40f);

        cache.storeContextFreeFinalEntry(inputs, stored, LTR);

        assertFalse(cache.isEmpty(), "the entry still marks the node as clean");
        assertNull(cache.get(inputs),
            "the generic lookup must not hand out a result computed under an inherited context");
        assertSame(stored, cache.getContextFreeFinalEntry(inputs, LTR),
            "the dedicated lookup returns the marked entry");
    }

    @Test
    @DisplayName("a later ordinary store clears the mark")
    void ordinaryStoreClearsTheMark() {
        LayoutCache cache = new LayoutCache();
        LayoutInput inputs = finalLayoutInput();

        cache.storeContextFreeFinalEntry(inputs, output(30f, 40f), LTR);
        LayoutOutput ordinary = output(50f, 60f);
        cache.store(inputs, ordinary);

        assertSame(ordinary, cache.get(inputs), "the ordinary entry is visible again");
        assertNull(cache.getContextFreeFinalEntry(inputs, LTR),
            "a later ordinary store revokes the reuse permission");
    }

    @Test
    @DisplayName("clearing the cache clears the mark")
    void clearClearsTheMark() {
        LayoutCache cache = new LayoutCache();
        LayoutInput inputs = finalLayoutInput();

        cache.storeContextFreeFinalEntry(inputs, output(30f, 40f), LTR);
        cache.clear();
        assertNull(cache.getContextFreeFinalEntry(inputs, LTR), "a cleared cache has no entry to reuse");

        LayoutOutput ordinary = output(70f, 80f);
        cache.store(inputs, ordinary);
        assertNull(cache.getContextFreeFinalEntry(inputs, LTR),
            "clearing the mark is not undone by the next ordinary store");
    }

    @Test
    @DisplayName("a measure store leaves the final entry and its mark alone")
    void measureStoreKeepsTheMark() {
        LayoutCache cache = new LayoutCache();
        LayoutInput finalInputs = finalLayoutInput();
        LayoutOutput finalOutput = output(30f, 40f);

        cache.storeContextFreeFinalEntry(finalInputs, finalOutput, LTR);
        cache.store(measureInput(), output(12f, 24f));

        assertSame(finalOutput, cache.getContextFreeFinalEntry(finalInputs, LTR),
            "storing a measure result does not touch the final entry");
    }

    @Test
    @DisplayName("the dedicated lookup only answers final-layout requests")
    void dedicatedLookupOnlyAnswersFinalLayoutRequests() {
        LayoutCache cache = new LayoutCache();
        cache.storeContextFreeFinalEntry(finalLayoutInput(), output(30f, 40f), LTR);

        assertNull(cache.getContextFreeFinalEntry(measureInput(), LTR),
            "a measure request never reuses a final-layout entry");
    }

    @Test
    @DisplayName("a cache-wide mode mismatch rejects the marked entry")
    void modeMismatchRejectsTheMarkedEntry() {
        LayoutCache cache = new LayoutCache();
        cache.storeContextFreeFinalEntry(finalLayoutInput(), output(30f, 40f), LTR);
        LayoutInput otherMode = new LayoutInput(
            RunMode.PERFORM_LAYOUT,
            SizingMode.CONTENT_SIZE,
            RequestedAxis.BOTH,
            new FloatSize(300f, Float.NaN),
            new FloatSize(800f, 600f),
            TaffySize.of(AvailableSpace.definite(800f), AvailableSpace.definite(600f)),
            TaffyLine.FALSE
        );

        assertNull(cache.getContextFreeFinalEntry(otherMode, LTR),
            "a different sizing mode is a different cache mode");
    }

    /**
     * {@code direction: inherit} is resolved from the ancestors and is not part of the
     * {@link LayoutInput} key, so an entry computed left-to-right must not answer a right-to-left
     * lookup of the same inputs.
     */
    @Test
    @DisplayName("a resolved-direction change rejects the marked entry")
    void directionChangeRejectsTheMarkedEntry() {
        LayoutCache cache = new LayoutCache();
        LayoutInput inputs = finalLayoutInput();
        LayoutOutput stored = output(30f, 40f);

        cache.storeContextFreeFinalEntry(inputs, stored, LTR);

        assertNull(cache.getContextFreeFinalEntry(inputs, RTL),
            "the same inputs in the other direction must not reuse the mirrored layout");
        assertSame(stored, cache.getContextFreeFinalEntry(inputs, LTR),
            "the direction it was computed with still hits");
    }

    @Test
    @DisplayName("only final layout can be stored as context-free")
    void onlyFinalLayoutCanBeStoredAsContextFree() {
        LayoutCache cache = new LayoutCache();
        assertThrows(IllegalArgumentException.class,
            () -> cache.storeContextFreeFinalEntry(measureInput(), output(10f, 10f), LTR));
        assertTrue(cache.isEmpty(), "the rejected store left no entry behind");
    }
}
