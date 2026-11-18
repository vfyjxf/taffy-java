package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;

import java.util.Arrays;

/**
 * A cache for storing the results of layout computation.
 *
 * <p>The cache is keyed on the {@link LayoutInput} of the layout pass (BL-04): the known
 * dimensions, the available space, the parent size and the {@link RunMode}, plus a cache-wide
 * mode made of the {@link SizingMode} and the vertical margin collapsibility (CR-03). In an axis
 * with a known dimension only that dimension is part of the key, so a change of the available
 * space in a known axis does not miss the cache (CR-01). The requested {@link RequestedAxis} is
 * not part of the key yet because this port always requests {@link RequestedAxis#BOTH}.
 *
 * <p>Measure entries only keep the outer size of the result, and a cache hit reconstructs a
 * {@link LayoutOutput} with {@link LayoutOutput#fromOuterSize(FloatSize)}. That reconstruction
 * drops the margin-collapse metadata, so measure results that carry such metadata (or that were
 * computed while vertical margins were collapsible) are not cached (CR-02).
 */
public final class LayoutCache {

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
     * Tracks which measure entries have been used since the eviction cursor last passed them.
     * Bit {@code i} corresponds to {@link #measureEntries}{@code [i]}. Implements the
     * second-chance (clock) eviction policy from upstream taffy (#1010).
     */
    private int recentlyUsedEntries = 0;

    /**
     * The next measure entry to consider replacing
     */
    private int nextMeasureEntry = 0;

    /**
     * Tracks if all cache entries are empty
     */
    private boolean isEmpty = true;

    /**
     * The {@link SizingMode} and vertical margin collapsibility that all of the cache's entries
     * were computed with. Results are only retrieved for inputs that have the same mode, and
     * storing a result computed with a different mode drops the existing entries.
     */
    private int mode = CacheMode.INITIAL;

    /**
     * The layout inputs that affect a node's layout but are not part of the {@link CacheKey},
     * packed into an int (CR-03).
     */
    private static final class CacheMode {
        /** Set if the sizing mode is {@link SizingMode#INHERENT_SIZE}. */
        private static final int INHERENT_SIZE_BIT = 0b001;
        /** Set if the node's top margin is collapsible. */
        private static final int COLLAPSIBLE_START_MARGIN_BIT = 0b010;
        /** Set if the node's bottom margin is collapsible. */
        private static final int COLLAPSIBLE_END_MARGIN_BIT = 0b100;
        /** The mode of a cache that has never been stored to; it has no entries, so the value is irrelevant. */
        private static final int INITIAL = 0;

        private static int from(LayoutInput input) {
            int mode = input.sizingMode() == SizingMode.INHERENT_SIZE ? INHERENT_SIZE_BIT : 0;
            TaffyLine<Boolean> collapsible = input.verticalMarginsAreCollapsible();
            if (collapsible != null) {
                if (Boolean.TRUE.equals(collapsible.start)) {
                    mode |= COLLAPSIBLE_START_MARGIN_BIT;
                }
                if (Boolean.TRUE.equals(collapsible.end)) {
                    mode |= COLLAPSIBLE_END_MARGIN_BIT;
                }
            }
            return mode;
        }
    }

    /**
     * The cache key of a layout pass (BL-04). Each axis holds either the known dimension (when
     * one is set) or the available space, so that available space is ignored in a known axis
     * (CR-01). The parent size is always part of the key because it is used to resolve
     * percentages.
     */
    private static final class CacheKey {
        private final float knownWidth;
        private final float knownHeight;
        private final AvailableSpace availableSpaceWidth;
        private final AvailableSpace availableSpaceHeight;
        private final float parentWidth;
        private final float parentHeight;

        private CacheKey(LayoutInput input) {
            FloatSize known = input.knownDimensions();
            TaffySize<AvailableSpace> availableSpace = input.availableSpace();
            FloatSize parent = input.parentSize();
            this.knownWidth = known.width;
            this.knownHeight = known.height;
            this.availableSpaceWidth = availableSpace.width;
            this.availableSpaceHeight = availableSpace.height;
            this.parentWidth = parent.width;
            this.parentHeight = parent.height;
        }

        private static CacheKey of(LayoutInput input) {
            return new CacheKey(input);
        }

        /**
         * Whether this key can be used for the given {@code PERFORM_LAYOUT} input.
         */
        private boolean matchesForFinalLayout(CacheKey other) {
            return matchesAxis(knownWidth, availableSpaceWidth, other.knownWidth, other.availableSpaceWidth)
                && matchesAxis(knownHeight, availableSpaceHeight, other.knownHeight, other.availableSpaceHeight)
                && Float.compare(parentWidth, other.parentWidth) == 0
                && Float.compare(parentHeight, other.parentHeight) == 0;
        }

        /**
         * Whether this key can be used for the given {@code COMPUTE_SIZE} input.
         *
         * <p>Upstream taffy (#911) deliberately compares only the x-axis parent size for measure
         * entries as a performance trade-off ("not fully correct"). This port keys on both axes,
         * because its {@link LayoutInput} has no {@code known_dimensions_are_definite} component
         * and percentage sizes can depend on the parent height. The extra misses can be narrowed
         * when {@code known_dimensions_are_definite} is added.
         */
        private boolean matchesForMeasure(CacheKey other) {
            return matchesForFinalLayout(other);
        }

        private static boolean matchesAxis(float knownDimension, AvailableSpace availableSpace,
                                           float otherKnownDimension, AvailableSpace otherAvailableSpace) {
            boolean hasKnownDimension = !Float.isNaN(knownDimension);
            boolean otherHasKnownDimension = !Float.isNaN(otherKnownDimension);
            if (hasKnownDimension || otherHasKnownDimension) {
                // A known dimension replaces the available space in the key: the available space
                // may change without causing a miss, but a known dimension on only one side means
                // the two inputs differ.
                return hasKnownDimension == otherHasKnownDimension
                    && Float.compare(knownDimension, otherKnownDimension) == 0;
            }
            if (availableSpace == null || otherAvailableSpace == null) {
                return availableSpace == otherAvailableSpace;
            }
            return availableSpace.equals(otherAvailableSpace);
        }
    }

    /**
     * A single cache entry
     */
    private static class CacheEntry<T> {
        private final CacheKey key;
        private T content;

        private CacheEntry(CacheKey key, T content) {
            this.key = key;
            this.content = content;
        }
    }

    /**
     * Create a new empty cache
     */
    public LayoutCache() {
    }

    /**
     * Try to retrieve a cached result for the given layout inputs.
     *
     * <p>The cache-wide mode ({@link SizingMode} and vertical margin collapsibility) must match
     * the mode that stored the entries, otherwise the lookup misses (CR-03). In an axis with a
     * known dimension the available space is not compared (CR-01).
     *
     * @param inputs the inputs of the layout pass
     * @return the cached output, or {@code null} if there is no usable entry
     */
    public LayoutOutput get(LayoutInput inputs) {
        if (isEmpty) return null;

        if (mode != CacheMode.from(inputs)) {
            return null;
        }

        CacheKey key = CacheKey.of(inputs);

        return switch (inputs.runMode()) {
            case PERFORM_LAYOUT ->
                finalLayoutEntry != null && finalLayoutEntry.key.matchesForFinalLayout(key)
                    ? finalLayoutEntry.content
                    : null;
            case COMPUTE_SIZE -> getMeasureEntry(key);
            case PERFORM_HIDDEN_LAYOUT -> null;
        };
    }

    private LayoutOutput getMeasureEntry(CacheKey key) {
        // TODO(CR-02): validate the RequestedAxis of the lookup against the axis the entry was
        // computed for (upstream #1010). Unreachable while every caller requests BOTH (FL-25).
        for (int i = 0; i < CACHE_SIZE; i++) {
            CacheEntry<FloatSize> entry = measureEntries[i];
            if (entry != null && entry.key.matchesForMeasure(key)) {
                // Mark the entry as used so that the clock does not evict a hot entry.
                recentlyUsedEntries |= 1 << i;
                return LayoutOutput.fromOuterSize(entry.content);
            }
        }
        return null;
    }

    /**
     * Store a computed result in the cache.
     *
     * <p>Storing a result computed with a different cache-wide mode first drops the existing
     * entries (CR-03). Measure results that carry margin-collapse metadata are never stored
     * (CR-02), because a cache hit reconstructs them from their outer size only.
     *
     * @param inputs       the inputs of the layout pass
     * @param layoutOutput the computed result
     */
    public void store(LayoutInput inputs, LayoutOutput layoutOutput) {
        if (inputs.runMode() == RunMode.PERFORM_HIDDEN_LAYOUT) {
            return;
        }

        int inputMode = CacheMode.from(inputs);
        if (mode != inputMode) {
            clearEntries();
            mode = inputMode;
        }

        CacheKey key = CacheKey.of(inputs);

        if (inputs.runMode() == RunMode.PERFORM_LAYOUT) {
            isEmpty = false;
            finalLayoutEntry = new CacheEntry<>(key, layoutOutput);
            return;
        }

        // RunMode.COMPUTE_SIZE
        if (carriesMarginCollapseMetadata(inputs, layoutOutput)) {
            return;
        }

        isEmpty = false;

        // Update a matching entry in place if there is one.
        for (int i = 0; i < CACHE_SIZE; i++) {
            CacheEntry<FloatSize> entry = measureEntries[i];
            if (entry != null && entry.key.matchesForMeasure(key)) {
                entry.content = layoutOutput.size();
                recentlyUsedEntries |= 1 << i;
                return;
            }
        }

        // Second-chance (clock) eviction: skip entries that have been used since the cursor last
        // passed them, clearing their used flag; replace the first entry that has not been used.
        while ((recentlyUsedEntries & (1 << nextMeasureEntry)) != 0) {
            recentlyUsedEntries &= ~(1 << nextMeasureEntry);
            nextMeasureEntry++;
            if (nextMeasureEntry == CACHE_SIZE) {
                nextMeasureEntry = 0;
            }
        }

        int entryIndex = nextMeasureEntry;
        measureEntries[entryIndex] = new CacheEntry<>(key, layoutOutput.size());
        recentlyUsedEntries |= 1 << entryIndex;
        nextMeasureEntry++;
        if (nextMeasureEntry == CACHE_SIZE) {
            nextMeasureEntry = 0;
        }
    }

    /**
     * Whether a measure result must not be cached because a cache hit could not reproduce it.
     *
     * <p>A measure cache entry only stores the outer size and reconstructs
     * {@link LayoutOutput#fromOuterSize(FloatSize)}, which resets {@code topMargin},
     * {@code bottomMargin} and {@code marginsCanCollapseThrough} (CR-02). Results carrying any of
     * those values are therefore not cached.
     *
     * <p>This port cannot read the values of a {@link CollapsibleMarginSet} (it has no value
     * equality), so any set other than the shared {@link CollapsibleMarginSet#ZERO} instance is
     * treated as metadata. TODO(CR-02): give CollapsibleMarginSet value equality and compare
     * against {@link CollapsibleMarginSet#ZERO} instead; that file is outside this task's scope.
     * Additionally the block layout drops its margin sets before a
     * {@code COMPUTE_SIZE} result leaves {@link BlockComputer}, so a measure computed while the
     * vertical margins are collapsible is refused as well; that is the reachable case here.
     */
    private static boolean carriesMarginCollapseMetadata(LayoutInput inputs, LayoutOutput layoutOutput) {
        if (layoutOutput.marginsCanCollapseThrough()) {
            return true;
        }
        if (layoutOutput.topMargin() != CollapsibleMarginSet.ZERO
            || layoutOutput.bottomMargin() != CollapsibleMarginSet.ZERO) {
            return true;
        }
        TaffyLine<Boolean> collapsible = inputs.verticalMarginsAreCollapsible();
        return collapsible != null && (Boolean.TRUE.equals(collapsible.start) || Boolean.TRUE.equals(collapsible.end));
    }

    /**
     * Try to retrieve a cached result from the cache.
     *
     * @deprecated use {@link #get(LayoutInput)} instead. Kept until 2.0 (Q3) and delegating with
     * a legacy input that has no parent size, an inherent sizing mode and non-collapsible margins.
     */
    @Deprecated
    public LayoutOutput get(
        FloatSize knownDimensions,
        TaffySize<AvailableSpace> availableSpace,
        RunMode runMode
    ) {
        return get(legacyInputs(knownDimensions, availableSpace, runMode));
    }

    /**
     * Store a computed result in the cache.
     *
     * @deprecated use {@link #store(LayoutInput, LayoutOutput)} instead. Kept until 2.0 (Q3) and
     * delegating with a legacy input that has no parent size, an inherent sizing mode and
     * non-collapsible margins.
     */
    @Deprecated
    public void store(
        FloatSize knownDimensions,
        TaffySize<AvailableSpace> availableSpace,
        RunMode runMode,
        LayoutOutput layoutOutput
    ) {
        store(legacyInputs(knownDimensions, availableSpace, runMode), layoutOutput);
    }

    private static LayoutInput legacyInputs(
        FloatSize knownDimensions,
        TaffySize<AvailableSpace> availableSpace,
        RunMode runMode
    ) {
        return new LayoutInput(
            runMode,
            SizingMode.INHERENT_SIZE,
            RequestedAxis.BOTH,
            knownDimensions,
            FloatSize.none(),
            availableSpace,
            TaffyLine.FALSE
        );
    }

    /**
     * Clear all cache entries
     *
     * @return true if the cache was already empty, false if it had entries that were cleared
     */
    public boolean clear() {
        boolean wasAlreadyEmpty = isEmpty;
        clearEntries();
        return wasAlreadyEmpty;
    }

    private void clearEntries() {
        finalLayoutEntry = null;
        Arrays.fill(measureEntries, null);
        recentlyUsedEntries = 0;
        nextMeasureEntry = 0;
        isEmpty = true;
    }

    /**
     * Check if the cache is empty
     */
    public boolean isEmpty() {
        return isEmpty;
    }
}
