package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.AvailableSpace;

import java.util.Arrays;

/**
 * A cache for storing the results of layout computation.
 * Uses a multi-slot caching strategy based on known dimensions and available space constraints.
 */
public final class Cache {

    private static final int CACHE_SIZE = 9;

    /**
     * Cache entry for final layout
     */
    private CacheEntry<LayoutOutput> finalLayoutEntry = null;

    /**
     * Cache entries for size measurements
     */
    @SuppressWarnings("unchecked")
    private final CacheEntry<FloatSize>[] measureEntries = new CacheEntry[CACHE_SIZE];

    /**
     * Tracks if all cache entries are empty
     */
    private boolean isEmpty = true;

    /**
     * A single cache entry
     */
    private static class CacheEntry<T> {
        FloatSize knownDimensions;
        Size<AvailableSpace> availableSpace;
        T content;

        CacheEntry(FloatSize knownDimensions, Size<AvailableSpace> availableSpace, T content) {
            this.knownDimensions = knownDimensions;
            this.availableSpace = availableSpace;
            this.content = content;
        }

        boolean matches(FloatSize knownDimensions, Size<AvailableSpace> availableSpace) {
            return sizesEqual(this.knownDimensions, knownDimensions) &&
                   this.availableSpace.equals(availableSpace);
        }

        private static boolean sizesEqual(FloatSize a, FloatSize b) {
            return floatsEqual(a.width, b.width) && floatsEqual(a.height, b.height);
        }

        private static boolean floatsEqual(Float a, Float b) {
            if (a == null && b == null) return true;
            if (a == null || b == null) return false;
            return Float.compare(a, b) == 0;
        }
    }

    /**
     * Create a new empty cache
     */
    public Cache() {
    }

    /**
     * Compute the cache slot to use for the given known dimensions and available space.
     */
    private static int computeCacheSlot(FloatSize knownDimensions, Size<AvailableSpace> availableSpace) {
        boolean hasKnownWidth = !Float.isNaN(knownDimensions.width);
        boolean hasKnownHeight = !Float.isNaN(knownDimensions.height);

        // Slot 0: Both known_dimensions were set
        if (hasKnownWidth && hasKnownHeight) {
            return 0;
        }

        // Slot 1-2: width but not height known_dimension was set
        if (hasKnownWidth) {
            return 1 + (availableSpace.height.isMinContent() ? 1 : 0);
        }

        // Slot 3-4: height but not width known_dimension was set
        if (hasKnownHeight) {
            return 3 + (availableSpace.width.isMinContent() ? 1 : 0);
        }

        // Slots 5-8: Neither known_dimensions were set
        boolean widthIsMinContent = availableSpace.width.isMinContent();
        boolean heightIsMinContent = availableSpace.height.isMinContent();

        if (!widthIsMinContent && !heightIsMinContent) return 5;
        if (!widthIsMinContent) return 6;
        if (!heightIsMinContent) return 7;
        return 8;
    }

    /**
     * Try to retrieve a cached result from the cache.
     * Uses smart matching logic from Rust taffy:
     * - Matches if known_dimensions match OR if known_dimensions equals the cached computed size
     * - Matches if known_dimensions is set OR if available_space is roughly equal
     */
    public LayoutOutput get(
        FloatSize knownDimensions,
        Size<AvailableSpace> availableSpace,
        RunMode runMode
    ) {
        if (isEmpty) return null;

        // Pre-compute known dimension checks once
        float kdWidth = knownDimensions.width;
        float kdHeight = knownDimensions.height;
        boolean hasKnownWidth = !Float.isNaN(kdWidth);
        boolean hasKnownHeight = !Float.isNaN(kdHeight);

        if (runMode == RunMode.PERFORM_LAYOUT) {
            if (finalLayoutEntry != null && matchesEntry(finalLayoutEntry, kdWidth, kdHeight, hasKnownWidth, hasKnownHeight, availableSpace)) {
                return finalLayoutEntry.content;
            }
            return null;
        }

        // For ComputeSize, check all measure cache entries
        for (int i = 0; i < CACHE_SIZE; i++) {
            CacheEntry<FloatSize> entry = measureEntries[i];
            if (entry != null) {
                FloatSize cachedSize = entry.content;
                if (matchesSizeEntry(entry, cachedSize, kdWidth, kdHeight, hasKnownWidth, hasKnownHeight, availableSpace)) {
                    return LayoutOutput.fromOuterSize(cachedSize);
                }
            }
        }

        // Also check final layout entry for ComputeSize
        if (finalLayoutEntry != null) {
            FloatSize cachedSize = finalLayoutEntry.content.size();
            if (matchesSizeEntry(finalLayoutEntry, cachedSize, kdWidth, kdHeight, hasKnownWidth, hasKnownHeight, availableSpace)) {
                return LayoutOutput.fromOuterSize(cachedSize);
            }
        }

        return null;
    }
    
    private static boolean matchesEntry(CacheEntry<LayoutOutput> entry, float kdWidth, float kdHeight,
                                        boolean hasKnownWidth, boolean hasKnownHeight, Size<AvailableSpace> availableSpace) {
        FloatSize cachedSize = entry.content.size();
        return (floatEquals(kdWidth, entry.knownDimensions.width) || floatEquals(kdWidth, cachedSize.width))
            && (floatEquals(kdHeight, entry.knownDimensions.height) || floatEquals(kdHeight, cachedSize.height))
            && (hasKnownWidth || entry.availableSpace.width.isRoughlyEqual(availableSpace.width))
            && (hasKnownHeight || entry.availableSpace.height.isRoughlyEqual(availableSpace.height));
    }
    
    private static <T> boolean matchesSizeEntry(CacheEntry<T> entry, FloatSize cachedSize, float kdWidth, float kdHeight,
                                                boolean hasKnownWidth, boolean hasKnownHeight, Size<AvailableSpace> availableSpace) {
        return (floatEquals(kdWidth, entry.knownDimensions.width) || floatEquals(kdWidth, cachedSize.width))
            && (floatEquals(kdHeight, entry.knownDimensions.height) || floatEquals(kdHeight, cachedSize.height))
            && (hasKnownWidth || entry.availableSpace.width.isRoughlyEqual(availableSpace.width))
            && (hasKnownHeight || entry.availableSpace.height.isRoughlyEqual(availableSpace.height));
    }

    /**
     * Helper to compare Float values (handles NaN)
     */
    private static boolean floatEquals(float a, float b) {
        return Float.compare(a, b) == 0;
    }

    /**
     * Store a computed result in the cache
     */
    public void store(
        FloatSize knownDimensions,
        Size<AvailableSpace> availableSpace,
        RunMode runMode,
        LayoutOutput layoutOutput
    ) {
        isEmpty = false;

        if (runMode == RunMode.PERFORM_LAYOUT) {
            finalLayoutEntry = new CacheEntry<>(knownDimensions, availableSpace, layoutOutput);
        } else {
            int slot = computeCacheSlot(knownDimensions, availableSpace);
            measureEntries[slot] = new CacheEntry<>(knownDimensions, availableSpace, layoutOutput.size());
        }
    }

    /**
     * Clear all cache entries
     *
     * @return true if the cache was already empty, false if it had entries that were cleared
     */
    public boolean clear() {
        boolean wasAlreadyEmpty = isEmpty;
        finalLayoutEntry = null;
        Arrays.fill(measureEntries, null);
        isEmpty = true;
        return wasAlreadyEmpty;
    }

    /**
     * Check if the cache is empty
     */
    public boolean isEmpty() {
        return isEmpty;
    }
}
