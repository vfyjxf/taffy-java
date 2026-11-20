package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.tree.LayoutCache;
import dev.vfyjxf.taffy.tree.LayoutInput;
import dev.vfyjxf.taffy.tree.LayoutOutput;
import dev.vfyjxf.taffy.tree.RequestedAxis;
import dev.vfyjxf.taffy.tree.RunMode;
import dev.vfyjxf.taffy.tree.SizingMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bit-for-bit cache-key semantics and entry-slot reuse of {@link LayoutCache}.
 *
 * <p>The key tests pin the exact comparisons the cache must use: {@code Float.compare} for
 * dimensions (so {@code -0.0} and {@code 0.0} differ, while every NaN compares equal), value
 * equality for definite available space, and identity for a {@code null} available space. The
 * reuse tests pin the allocation work removal: an entry object is updated and evicted in place,
 * and clearing a cache releases results without throwing the entry shells away.
 */
public class LayoutCacheSemanticsTest {

    private static final TaffySize<AvailableSpace> MAX_CONTENT =
        new TaffySize<>(AvailableSpace.MAX_CONTENT, AvailableSpace.MAX_CONTENT);

    // === Exact key comparison semantics ===

    @Test
    @DisplayName("signed_zero_known_dimension_misses")
    void signedZeroKnownDimensionMisses() {
        LayoutCache cache = new LayoutCache();
        LayoutInput positiveZero = performLayout(new FloatSize(0.0f, Float.NaN), FloatSize.none(), MAX_CONTENT);
        cache.store(positiveZero, output(1f, 1f));

        assertNotNull(cache.get(positiveZero), "the same key must hit");
        assertNull(cache.get(performLayout(new FloatSize(-0.0f, Float.NaN), FloatSize.none(), MAX_CONTENT)),
            "Float.compare(-0.0f, 0.0f) != 0, so the known dimension differs");
    }

    @Test
    @DisplayName("signed_zero_parent_size_misses")
    void signedZeroParentSizeMisses() {
        LayoutCache cache = new LayoutCache();
        LayoutInput positiveZero = performLayout(FloatSize.none(), new FloatSize(0.0f, 0.0f), MAX_CONTENT);
        cache.store(positiveZero, output(1f, 1f));

        assertNull(cache.get(performLayout(FloatSize.none(), new FloatSize(-0.0f, 0.0f), MAX_CONTENT)),
            "Float.compare distinguishes the two zeros in the parent size too");
    }

    @Test
    @DisplayName("nan_parent_size_compares_equal")
    void nanParentSizeComparesEqual() {
        LayoutCache cache = new LayoutCache();
        LayoutInput nanParent = performLayout(FloatSize.none(), new FloatSize(Float.NaN, Float.NaN), MAX_CONTENT);
        cache.store(nanParent, output(1f, 1f));

        assertNotNull(cache.get(performLayout(FloatSize.none(),
            new FloatSize(Float.NaN, Float.NaN), MAX_CONTENT)),
            "Float.compare treats every NaN as equal, so two NaN parents are the same key");
    }

    @Test
    @DisplayName("known_dimension_must_match_nan_ness")
    void knownDimensionMustMatchNaNNess() {
        LayoutCache cache = new LayoutCache();
        LayoutInput widthKnown = computeSize(new FloatSize(100f, Float.NaN), FloatSize.none(), MAX_CONTENT);
        cache.store(widthKnown, output(100f, 50f));

        assertNull(cache.get(computeSize(new FloatSize(100f, 50f), FloatSize.none(), MAX_CONTENT)),
            "a known height on one side only must miss");
        assertNull(cache.get(computeSize(FloatSize.none(), FloatSize.none(), MAX_CONTENT)),
            "an unknown width on one side only must miss");
        assertNotNull(cache.get(widthKnown));
    }

    @Test
    @DisplayName("definite_available_space_uses_value_equality")
    void definiteAvailableSpaceUsesValueEquality() {
        LayoutCache cache = new LayoutCache();
        LayoutInput ten = computeSize(FloatSize.none(), FloatSize.none(),
            new TaffySize<>(AvailableSpace.definite(10f), AvailableSpace.definite(20f)));
        cache.store(ten, output(10f, 20f));

        assertNotNull(cache.get(computeSize(FloatSize.none(), FloatSize.none(),
            new TaffySize<>(AvailableSpace.definite(10f), AvailableSpace.definite(20f)))),
            "distinct definite instances with equal values are value-equal");
        assertNull(cache.get(computeSize(FloatSize.none(), FloatSize.none(),
            new TaffySize<>(AvailableSpace.definite(0.0f), AvailableSpace.definite(20f)))),
            "a different definite value must miss");
    }

    @Test
    @DisplayName("definite_available_space_signed_zero_differs")
    void definiteAvailableSpaceSignedZeroDiffers() {
        LayoutCache cache = new LayoutCache();
        LayoutInput positiveZero = computeSize(FloatSize.none(), FloatSize.none(),
            new TaffySize<>(AvailableSpace.definite(0.0f), AvailableSpace.MAX_CONTENT));
        cache.store(positiveZero, output(1f, 1f));

        assertNull(cache.get(computeSize(FloatSize.none(), FloatSize.none(),
            new TaffySize<>(AvailableSpace.definite(-0.0f), AvailableSpace.MAX_CONTENT))),
            "AvailableSpace.equals uses Float.compare, so -0.0f is not 0.0f");
    }

    @Test
    @DisplayName("definite_nan_available_space_compares_equal")
    void definiteNanAvailableSpaceComparesEqual() {
        LayoutCache cache = new LayoutCache();
        LayoutInput nanSpace = computeSize(FloatSize.none(), FloatSize.none(),
            new TaffySize<>(AvailableSpace.definite(Float.NaN), AvailableSpace.MAX_CONTENT));
        cache.store(nanSpace, output(1f, 1f));

        assertNotNull(cache.get(computeSize(FloatSize.none(), FloatSize.none(),
            new TaffySize<>(AvailableSpace.definite(Float.NaN), AvailableSpace.MAX_CONTENT))),
            "AvailableSpace.equals uses Float.compare, which treats NaNs as equal");
    }

    @Test
    @DisplayName("null_available_space_uses_identity")
    void nullAvailableSpaceUsesIdentity() {
        LayoutCache cache = new LayoutCache();
        LayoutInput nullSpace = computeSize(FloatSize.none(), FloatSize.none(),
            new TaffySize<>(null, null));
        cache.store(nullSpace, output(1f, 1f));

        assertNotNull(cache.get(computeSize(FloatSize.none(), FloatSize.none(),
            new TaffySize<>(null, null))),
            "null == null is true, so two null available spaces match");
        assertNull(cache.get(computeSize(FloatSize.none(), FloatSize.none(), MAX_CONTENT)),
            "null does not equal a MAX_CONTENT instance");
    }

    @Test
    @DisplayName("known_axis_ignores_null_available_space")
    void knownAxisIgnoresNullAvailableSpace() {
        LayoutCache cache = new LayoutCache();
        LayoutInput known = computeSize(new FloatSize(50f, Float.NaN), FloatSize.none(),
            new TaffySize<>(null, AvailableSpace.definite(30f)));
        cache.store(known, output(50f, 30f));

        assertNotNull(cache.get(computeSize(new FloatSize(50f, Float.NaN), FloatSize.none(),
            new TaffySize<>(AvailableSpace.MAX_CONTENT, AvailableSpace.definite(30f)))),
            "the known width axis ignores its available space even when it is null");
        assertNull(cache.get(computeSize(new FloatSize(50f, Float.NaN), FloatSize.none(),
            new TaffySize<>(null, AvailableSpace.definite(31f)))),
            "the still-unknown height axis still keys on its available space");
    }

    @Test
    @DisplayName("final_and_measure_entries_do_not_cross_hit")
    void finalAndMeasureEntriesDoNotCrossHit() {
        LayoutCache cache = new LayoutCache();
        LayoutInput finalLayout = performLayout(FloatSize.none(), FloatSize.none(), MAX_CONTENT);
        cache.store(finalLayout, output(10f, 10f));

        assertNull(cache.get(computeSize(FloatSize.none(), FloatSize.none(), MAX_CONTENT)),
            "a measure lookup must not reuse a final layout entry");
        assertNotNull(cache.get(finalLayout), "the final layout entry must still be valid");
    }

    // === Entry slot reuse ===

    @Test
    @DisplayName("measure_store_updates_entry_in_place")
    void measureStoreUpdatesEntryInPlace() {
        LayoutCache cache = new LayoutCache();
        LayoutInput first = measureOfWidth(10f);
        cache.store(first, output(10f, 20f));
        Object entry = measureEntries(cache)[0];

        cache.store(first, output(11f, 21f));

        assertSame(entry, measureEntries(cache)[0], "a matching store must not allocate a new entry");
        LayoutOutput cached = cache.get(first);
        assertNotNull(cached);
        assertEquals(11f, cached.size().width);
        assertEquals(21f, cached.size().height);
    }

    @Test
    @DisplayName("measure_eviction_reuses_the_victim_shell")
    void measureEvictionReusesTheVictimShell() {
        LayoutCache cache = new LayoutCache();
        for (int i = 0; i < 9; i++) {
            cache.store(measureOfWidth(i), output(i, i));
        }
        Object[] before = measureEntries(cache);

        // The clock clears all used bits and then replaces slot 0 first.
        cache.store(measureOfWidth(9), output(9f, 9f));
        Object[] after = measureEntries(cache);

        assertSame(before[0], after[0], "the evicted slot must be reused, not reallocated");
        assertNull(cache.get(measureOfWidth(0)), "the evicted key must miss");
        assertNotNull(cache.get(measureOfWidth(9)), "the replacement key must hit");
    }

    @Test
    @DisplayName("clear_releases_content_but_keeps_entry_shells")
    void clearReleasesContentButKeepsEntryShells() throws Exception {
        LayoutCache cache = new LayoutCache();
        LayoutInput measure = measureOfWidth(10f);
        LayoutInput finalLayout = performLayout(FloatSize.none(), FloatSize.none(), MAX_CONTENT);
        cache.store(measure, output(10f, 10f));
        cache.store(finalLayout, output(20f, 20f));

        Object measureEntry = measureEntries(cache)[0];
        Object finalEntry = finalLayoutEntry(cache);
        assertNotNull(contentOf(measureEntry));
        assertNotNull(contentOf(finalEntry));

        cache.clear();

        assertTrue(cache.isEmpty());
        assertNull(cache.get(measure), "cleared entries must not be returned");
        assertNull(cache.get(finalLayout), "cleared final layout must not be returned");
        assertNull(contentOf(measureEntry), "clearing must release the cached measure result");
        assertNull(contentOf(finalEntry), "clearing must release the cached layout output");

        cache.store(measure, output(10f, 10f));
        cache.store(finalLayout, output(20f, 20f));
        assertSame(measureEntry, measureEntries(cache)[0], "a post-clear store must reuse the measure shell");
        assertSame(finalEntry, finalLayoutEntry(cache), "a post-clear store must reuse the final layout shell");
    }

    @Test
    @DisplayName("store_after_mode_change_reuses_entry_shells")
    void storeAfterModeChangeReusesEntryShells() {
        LayoutCache cache = new LayoutCache();
        LayoutInput inherent = measureOfWidth(10f);
        cache.store(inherent, output(10f, 10f));
        Object inherentEntry = measureEntries(cache)[0];

        LayoutInput contentSized = new LayoutInput(
            RunMode.COMPUTE_SIZE,
            SizingMode.CONTENT_SIZE,
            RequestedAxis.BOTH,
            new FloatSize(10f, Float.NaN),
            FloatSize.none(),
            MAX_CONTENT,
            TaffyLine.FALSE
        );
        cache.store(contentSized, output(10f, 10f));

        assertNull(cache.get(inherent), "the old mode's entries must be evicted");
        assertSame(inherentEntry, measureEntries(cache)[0], "the new mode reuses the cleared slot");
        assertNotNull(cache.get(contentSized));
    }

    // === The hash fast path must never turn a miss into a hit ===

    @Test
    @DisplayName("hash_collision_is_revalidated")
    void hashCollisionIsRevalidated() throws Exception {
        LayoutCache cache = new LayoutCache();
        LayoutInput stored = measureOfWidth(10f);
        LayoutInput different = measureOfWidth(20f);
        cache.store(stored, output(10f, 10f));

        Object entry = measureEntries(cache)[0];
        setField(entry, "keyHash", keyHashOf(different));

        assertNull(cache.get(different),
            "a matching fingerprint must still be checked against the actual key fields");
    }

    // === Helpers ===

    private static LayoutInput performLayout(FloatSize knownDimensions, FloatSize parentSize,
                                             TaffySize<AvailableSpace> availableSpace) {
        return input(RunMode.PERFORM_LAYOUT, knownDimensions, parentSize, availableSpace);
    }

    private static LayoutInput computeSize(FloatSize knownDimensions, FloatSize parentSize,
                                           TaffySize<AvailableSpace> availableSpace) {
        return input(RunMode.COMPUTE_SIZE, knownDimensions, parentSize, availableSpace);
    }

    private static LayoutInput input(RunMode runMode, FloatSize knownDimensions, FloatSize parentSize,
                                     TaffySize<AvailableSpace> availableSpace) {
        return new LayoutInput(
            runMode,
            SizingMode.INHERENT_SIZE,
            RequestedAxis.BOTH,
            knownDimensions,
            parentSize,
            availableSpace,
            TaffyLine.FALSE
        );
    }

    private static LayoutInput measureOfWidth(float width) {
        return computeSize(new FloatSize(width, Float.NaN), FloatSize.none(), MAX_CONTENT);
    }

    private static LayoutOutput output(float width, float height) {
        return LayoutOutput.fromSizes(new FloatSize(width, height), FloatSize.zero());
    }

    private static Object[] measureEntries(LayoutCache cache) {
        try {
            Field field = LayoutCache.class.getDeclaredField("measureEntries");
            field.setAccessible(true);
            return (Object[]) field.get(cache);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static Object finalLayoutEntry(LayoutCache cache) {
        try {
            Field field = LayoutCache.class.getDeclaredField("finalLayoutEntry");
            field.setAccessible(true);
            return field.get(cache);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static Object contentOf(Object entry) throws Exception {
        return getField(entry, "content");
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static int keyHashOf(LayoutInput input) throws Exception {
        Method method = LayoutCache.class.getDeclaredMethod(
            "keyHash", FloatSize.class, TaffySize.class, FloatSize.class);
        method.setAccessible(true);
        return (int) method.invoke(null, input.knownDimensions(), input.availableSpace(), input.parentSize());
    }
}
