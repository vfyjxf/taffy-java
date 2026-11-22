package dev.vfyjxf.taffy.style;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the bounded definite-available-space cache added for allocation pressure (p34).
 *
 * <p>The cache may only change object identity. Type, value, comparisons and every derived
 * operation must keep the semantics of a freshly constructed instance, including the
 * {@code 0.0}/{@code -0.0} and NaN corner cases.</p>
 */
class AvailableSpaceCacheTest {

    @Test
    void repeatedDefiniteValueReturnsTheCachedInstance() {
        AvailableSpace first = AvailableSpace.definite(1234.5f);
        for (int i = 0; i < 10_000; i++) {
            assertSame(first, AvailableSpace.definite(1234.5f), "iteration " + i);
        }
        assertEquals(AvailableSpace.Type.DEFINITE, first.getType());
        assertEquals(1234.5f, first.getValue());
    }

    @Test
    void commonLayoutValuesAreStable() {
        AvailableSpace zero = AvailableSpace.definite(0f);
        assertSame(zero, AvailableSpace.definite(0f));
        AvailableSpace ten = AvailableSpace.definite(10f);
        AvailableSpace twoThousand = AvailableSpace.definite(2000f);
        assertSame(ten, AvailableSpace.definite(10f));
        assertSame(twoThousand, AvailableSpace.definite(2000f));
        assertNotSame(ten, twoThousand);
    }

    @Test
    void signedZeroKeepsItsOwnIdentityAndSign() {
        AvailableSpace positive = AvailableSpace.definite(0f);
        AvailableSpace negative = AvailableSpace.definite(-0f);
        assertNotSame(positive, negative);
        assertEquals(0, Float.floatToIntBits(positive.getValue()));
        assertEquals(Integer.MIN_VALUE, Float.floatToIntBits(negative.getValue()));
        assertSame(positive, AvailableSpace.definite(0f));
        assertSame(negative, AvailableSpace.definite(-0f));
        // Their public value semantics differ exactly like two fresh instances.
        assertNotEquals0(positive, negative);
    }

    private static void assertNotEquals0(AvailableSpace a, AvailableSpace b) {
        assertTrue(!a.equals(b), "0.0 and -0.0 must not compare equal");
    }

    @Test
    void nanIsCachedAndStaysIndefiniteInValueButDefiniteInType() {
        AvailableSpace first = AvailableSpace.definite(Float.NaN);
        AvailableSpace otherNaNPayload = AvailableSpace.definite(Float.intBitsToFloat(0x7fc00001));
        assertSame(first, otherNaNPayload);
        assertTrue(first.isDefinite());
        assertTrue(Float.isNaN(first.getValue()));
        assertEquals(AvailableSpace.definite(Float.NaN), first);
        assertEquals(first.hashCode(), otherNaNPayload.hashCode());
    }

    @Test
    void infinitiesAndExtremesKeepTheirValues() {
        float[] values = {
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.MAX_VALUE,
            -Float.MAX_VALUE,
            Float.MIN_VALUE,
            -Float.MIN_VALUE
        };
        for (float value : values) {
            AvailableSpace first = AvailableSpace.definite(value);
            assertSame(first, AvailableSpace.definite(value), "identity for " + value);
            assertEquals(value, first.getValue(), "value for " + value);
        }
    }

    @Test
    void cacheIsBoundedSoOldEntriesAreEvicted() {
        // Flood the cache with more distinct values than it has slots; a bounded direct-mapped
        // cache must not keep the first value alive by identity (eviction is allowed, retention of
        // every value is not). The cache size itself is pinned to prove the bound is real.
        assertEquals(256, AvailableSpace.DEFINITE_CACHE_SIZE);
        AvailableSpace first = AvailableSpace.definite(1234.5f);
        for (int i = 0; i < 4096; i++) {
            AvailableSpace.definite(10_000f + i);
        }
        assertNotSame(first, AvailableSpace.definite(1234.5f));
    }

    @Test
    void instancesAreNotSharedAcrossThreads() throws Exception {
        AvailableSpace mainInstance = AvailableSpace.definite(777.25f);
        AtomicReference<AvailableSpace> fromOtherThread = new AtomicReference<>();
        Thread thread = new Thread(() -> fromOtherThread.set(AvailableSpace.definite(777.25f)));
        thread.start();
        thread.join();
        // The cache is per-thread, so the other thread keeps its own instance.
        assertNotSame(mainInstance, fromOtherThread.get());
        assertEquals(mainInstance, fromOtherThread.get());
        assertEquals(mainInstance.getValue(), fromOtherThread.get().getValue());
    }

    @Test
    void derivedOperationsStillWorkOnCachedInstances() {
        AvailableSpace space = AvailableSpace.definite(100f);
        assertEquals(90f, space.maybeSub(10f).getValue());
        assertEquals(110f, space.maybeAdd(10f).getValue());
        assertEquals(100f, space.maybeMax(50f).getValue());
        assertEquals(100f, space.maybeMin(500f).getValue());
        assertEquals(Float.NaN, AvailableSpace.minContent().intoOption());
        assertEquals(Float.NaN, AvailableSpace.maxContent().intoOption());
        assertSame(AvailableSpace.definite(100f), space);
    }
}
