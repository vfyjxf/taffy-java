package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;

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
 *
 * <p>Implementation note: an entry stores the key fields themselves, so a lookup compares the
 * {@link LayoutInput}'s values in place and never allocates a probe object. Measure lookups
 * first compare a 32-bit key fingerprint and only run the full per-axis comparison when the
 * fingerprints match; the fingerprint is a pure fast-path filter and a matching fingerprint is
 * always re-validated, so collisions cannot turn a miss into a hit. Entries are reused in place
 * instead of being reallocated on every store, including after {@link #clear()}: clearing
 * releases the cached results but keeps the (now invalid) entry shells, which keeps repeated
 * cold layouts allocation-free.
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
     * The number of valid entries in {@link #measureEntries}. Once the cache has filled up this
     * stays at {@link #CACHE_SIZE}; {@link #clearEntries()} resets it. Used to skip the measure
     * scan entirely when no measure result has been stored yet.
     */
    private int measureEntryCount = 0;

    /**
     * Index of the measure entry that satisfied the previous lookup, or {@code -1}. Many layout
     * passes repeat the same measurement (a stretch pass followed by a layout pass, or a style
     * change that keeps the child's available space), so checking this slot before scanning all
     * nine usually turns the scan into one comparison. Clock eviction still sees the access via
     * {@link #recentlyUsedEntries}.
     */
    private int lastMeasureEntry = -1;

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
     * The layout inputs that affect a node's layout but are not part of the cache key,
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
     * A single cache entry: the cache key of the layout pass (BL-04) plus the cached result.
     *
     * <p>Each axis holds either the known dimension (when one is set) or the available space, so
     * that available space is ignored in a known axis (CR-01). The parent size is always part of
     * the key because it is used to resolve percentages. The fields are stored directly (instead
     * of in a separate key object) and an entry is reused in place by the store path.
     */
    private static final class CacheEntry<T> {
        private float knownWidth;
        private float knownHeight;
        private AvailableSpace availableSpaceWidth;
        private AvailableSpace availableSpaceHeight;
        private float parentWidth;
        private float parentHeight;
        /**
         * Fast-path fingerprint of the key fields; equal keys always have equal fingerprints,
         * but equal fingerprints are re-validated with {@link #matches}.
         */
        private int keyHash;
        /** Whether the entry currently holds a result; clearing a cache invalidates without deallocating. */
        private boolean valid;
        private T content;

        private void set(FloatSize known, TaffySize<AvailableSpace> availableSpace, FloatSize parent,
                         int keyHash, T content) {
            this.knownWidth = known.width;
            this.knownHeight = known.height;
            this.availableSpaceWidth = availableSpace.width;
            this.availableSpaceHeight = availableSpace.height;
            this.parentWidth = parent.width;
            this.parentHeight = parent.height;
            this.keyHash = keyHash;
            this.valid = true;
            this.content = content;
        }

        /**
         * Whether this entry can be used for the given {@code PERFORM_LAYOUT} input.
         *
         * <p>Measure entries use the same comparison. Upstream taffy (#911) deliberately compares
         * only the x-axis parent size for measure entries as a performance trade-off ("not fully
         * correct"). This port keys on both axes, because its {@link LayoutInput} has no
         * {@code known_dimensions_are_definite} component and percentage sizes can depend on the
         * parent height. The extra misses can be narrowed when that component is added.
         */
        private boolean matches(FloatSize known, TaffySize<AvailableSpace> availableSpace, FloatSize parent) {
            return matchesAxis(knownWidth, availableSpaceWidth, known.width, availableSpace.width)
                && matchesAxis(knownHeight, availableSpaceHeight, known.height, availableSpace.height)
                && Float.compare(parentWidth, parent.width) == 0
                && Float.compare(parentHeight, parent.height) == 0;
        }

        private void invalidate() {
            valid = false;
            content = null;
        }

        /**
         * Compares one axis exactly as the cache key did before entries stored the fields
         * directly. {@code Float.compare} is deliberately kept: it distinguishes {@code -0.0}
         * from {@code 0.0} and treats every NaN as equal.
         */
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

        return switch (inputs.runMode()) {
            case PERFORM_LAYOUT -> getFinalLayoutEntry(inputs);
            case COMPUTE_SIZE -> getMeasureEntry(inputs);
            case PERFORM_HIDDEN_LAYOUT -> null;
        };
    }

    private LayoutOutput getFinalLayoutEntry(LayoutInput inputs) {
        CacheEntry<LayoutOutput> entry = finalLayoutEntry;
        if (entry == null || !entry.valid) {
            return null;
        }
        return entry.matches(inputs.knownDimensions(), inputs.availableSpace(), inputs.parentSize())
            ? entry.content
            : null;
    }

    private LayoutOutput getMeasureEntry(LayoutInput inputs) {
        if (measureEntryCount == 0) {
            return null;
        }

        FloatSize known = inputs.knownDimensions();
        TaffySize<AvailableSpace> availableSpace = inputs.availableSpace();
        FloatSize parent = inputs.parentSize();
        int keyHash = keyHash(known, availableSpace, parent);

        int last = lastMeasureEntry;
        if (last >= 0) {
            CacheEntry<FloatSize> entry = measureEntries[last];
            if (entry != null && entry.valid && entry.keyHash == keyHash
                && entry.matches(known, availableSpace, parent)) {
                recentlyUsedEntries |= 1 << last;
                return LayoutOutput.fromOuterSize(entry.content);
            }
        }

        for (int i = 0; i < CACHE_SIZE; i++) {
            CacheEntry<FloatSize> entry = measureEntries[i];
            // The fingerprint rejects almost every non-matching slot without touching its key
            // fields; a fingerprint match is still fully re-validated below.
            if (entry == null || !entry.valid || entry.keyHash != keyHash) {
                continue;
            }
            if (entry.matches(known, availableSpace, parent)) {
                // Mark the entry as used so that the clock does not evict a hot entry.
                recentlyUsedEntries |= 1 << i;
                lastMeasureEntry = i;
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

        FloatSize known = inputs.knownDimensions();
        TaffySize<AvailableSpace> availableSpace = inputs.availableSpace();
        FloatSize parent = inputs.parentSize();
        int keyHash = keyHash(known, availableSpace, parent);

        if (inputs.runMode() == RunMode.PERFORM_LAYOUT) {
            isEmpty = false;
            CacheEntry<LayoutOutput> entry = finalLayoutEntry;
            if (entry == null) {
                entry = new CacheEntry<>();
                finalLayoutEntry = entry;
            }
            entry.set(known, availableSpace, parent, keyHash, layoutOutput);
            return;
        }

        // RunMode.COMPUTE_SIZE
        if (carriesMarginCollapseMetadata(inputs, layoutOutput)) {
            return;
        }

        isEmpty = false;
        FloatSize content = layoutOutput.size();

        // Update a matching entry in place if there is one.
        for (int i = 0; i < CACHE_SIZE; i++) {
            CacheEntry<FloatSize> entry = measureEntries[i];
            if (entry != null && entry.valid && entry.keyHash == keyHash
                && entry.matches(known, availableSpace, parent)) {
                entry.content = content;
                recentlyUsedEntries |= 1 << i;
                lastMeasureEntry = i;
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
        CacheEntry<FloatSize> entry = measureEntries[entryIndex];
        if (entry == null) {
            // First use of this slot: allocate the reusable shell once, never on updates.
            entry = new CacheEntry<>();
            measureEntries[entryIndex] = entry;
            measureEntryCount++;
        } else if (!entry.valid) {
            // A shell retained across clear(): reusing it keeps cold layouts allocation-free.
            measureEntryCount++;
        }
        entry.set(known, availableSpace, parent, keyHash, content);
        lastMeasureEntry = entryIndex;
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
     * A fingerprint of the key fields used to reject non-matching measure entries before the
     * full comparison.
     *
     * <p>It hashes exactly the values {@link CacheEntry#matches} compares: in a known axis the
     * known dimension, otherwise the available space's type and value; plus both parent
     * dimensions. {@code floatToIntBits} canonicalizes NaNs, matching {@code Float.compare}'s
     * treatment of every NaN as equal. Two equal keys therefore always produce the same
     * fingerprint; the converse is not true, which is why a matching fingerprint is re-validated.
     */
    private static int keyHash(FloatSize known, TaffySize<AvailableSpace> availableSpace, FloatSize parent) {
        int hash = 0x9E3779B9;
        hash = 31 * hash + axisHash(known.width, availableSpace.width);
        hash = 31 * hash + axisHash(known.height, availableSpace.height);
        hash = 31 * hash + Float.floatToIntBits(parent.width);
        hash = 31 * hash + Float.floatToIntBits(parent.height);
        return hash;
    }

    private static int axisHash(float knownDimension, AvailableSpace availableSpace) {
        if (!Float.isNaN(knownDimension)) {
            return Float.floatToIntBits(knownDimension);
        }
        if (availableSpace == null) {
            return 0x7f4a7c15;
        }
        return switch (availableSpace.getType()) {
            case DEFINITE -> Float.floatToIntBits(availableSpace.getValue()) ^ 0x165667b1;
            case MIN_CONTENT -> 0x27d4eb2f;
            case MAX_CONTENT -> 0x165667c5;
        };
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
        if (finalLayoutEntry != null) {
            finalLayoutEntry.invalidate();
        }
        for (int i = 0; i < CACHE_SIZE; i++) {
            CacheEntry<FloatSize> entry = measureEntries[i];
            if (entry != null) {
                entry.invalidate();
            }
        }
        recentlyUsedEntries = 0;
        nextMeasureEntry = 0;
        measureEntryCount = 0;
        lastMeasureEntry = -1;
        isEmpty = true;
    }

    /**
     * Check if the cache is empty
     */
    public boolean isEmpty() {
        return isEmpty;
    }
}
