package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.TaffyDirection;

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
 * <p>The resolved text direction is a further cache-wide part of the key. {@code direction:
 * inherit} is resolved from the ancestors, so it is not a component of the {@link LayoutInput}
 * and an ancestor's direction change cannot dirty this node. The direction-aware lookup and store
 * therefore carry the node's resolved direction, and a store under a different direction drops
 * every existing entry before storing the new one - the same effect as if the direction were
 * part of every key. A lookup without a direction (the public, direction-less API) accepts
 * entries of any direction, and a cache whose entries were all stored without a direction stays
 * usable from every direction (its direction is "unknown").
 *
 * <p>Implementation note: an entry stores an exact packed key of the {@link LayoutInput}'s
 * values, so a lookup compares three {@code long}s in place, allocates no probe object and needs
 * no field-by-field re-validation. The packed key keeps every relevant bit: a known axis stores
 * its {@link Float#floatToIntBits} value behind a "known" bit, an unknown axis stores the
 * available-space type and, for a definite space, its value bits, and the parent size packs both
 * dimensions. {@code floatToIntBits} canonicalizes NaNs, which matches the {@link Float#compare}
 * semantics the cache had when it compared the fields individually (every NaN compares equal,
 * while {@code -0.0f} and {@code 0.0f} differ). The single final layout entry compares the packed
 * key directly; the nine measure entries additionally keep a 32-bit fingerprint, because one
 * {@code int} comparison rejects the eight entries that usually do not match without loading
 * their key words. Entries are reused in place instead of being reallocated on every store,
 * including after {@link #clear()}: clearing releases the cached results but keeps the (now
 * invalid) entry shells, which keeps repeated cold layouts allocation-free.
 */
public final class LayoutCache {

    private static final int CACHE_SIZE = 9;

    /**
     * Cache entry for final layout
     */
    private CacheEntry<LayoutOutput> finalLayoutEntry = null;

    /**
     * Whether {@link #finalLayoutEntry} was produced by a same-BFC block layout that may be reused
     * by another same-BFC pass.
     *
     * <p>A same-BFC block child is normally recomputed on every pass because the inherited
     * {@code BlockContext} makes its output depend on mutable float state. When the inherited
     * context holds no floats <em>and</em> the child's own layout placed none, the output is a pure
     * function of the {@link LayoutInput} and the node's style/subtree, exactly like a BFC-root
     * layout. Such an entry is marked here;
     * {@link #getContextFreeFinalEntry(LayoutInput, TaffyDirection)} is the only lookup that
     * honours the mark, so ordinary {@link #get(LayoutInput)} callers never gain float-dependent
     * reuse. Every other store clears the mark (see {@link #store(LayoutInput, LayoutOutput)} and
     * {@link #clearEntries()}).</p>
     */
    private boolean finalEntryContextFree = false;

    /**
     * The resolved text direction {@link #finalLayoutEntry} was computed with, as a
     * {@link TaffyDirection} ordinal, or {@code -1} when the entry was not stored as context-free.
     *
     * <p>{@code direction: inherit} is resolved from the ancestors and is not part of the
     * {@link LayoutInput} key, so the same input can produce a mirrored layout after an ancestor's
     * direction changes. A context-free entry therefore remembers the direction it was computed
     * with, and a lookup only hits when the node resolves to the same direction.</p>
     */
    private int finalEntryDirection = -1;

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
     * The resolved text direction every valid entry was computed with, or {@code null} when the
     * entries are direction-agnostic (all of them were stored through the public, direction-less
     * API, which cannot know the direction).
     *
     * <p>{@code direction: inherit} is resolved from the ancestors and is not part of the
     * {@link LayoutInput} key, so one set of inputs can produce a mirrored layout on either side
     * of an ancestor's direction change. Storing under a different direction therefore drops the
     * existing entries (see {@link #store(LayoutInput, LayoutOutput, TaffyDirection)}), and a
     * direction-aware lookup misses when the stored direction differs. A {@code null} stored
     * direction is a wildcard so that the public, direction-less API keeps its historical
     * behaviour: entries it stored are returned by every direction-aware lookup, and its lookups
     * return entries of any direction.</p>
     */
    private TaffyDirection cacheDirection = null;

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
     * A single cache entry: the packed cache key of the layout pass (BL-04) plus the cached
     * result.
     *
     * <p>Each axis holds either the known dimension (when one is set) or the available space, so
     * that available space is ignored in a known axis (CR-01). The parent size is always part of
     * the key because it is used to resolve percentages. The key is stored as two per-axis
     * {@code long}s plus one parent-size {@code long} (see {@link #axisKey} and
     * {@link #parentKey}), so comparing an entry with a lookup is three exact integer
     * comparisons: no key bit is lost and there is nothing left to re-validate. An entry is
     * reused in place by the store path.
     *
     * <p>Both axes and both parent dimensions are part of the key. Upstream taffy (#911)
     * deliberately compares only the x-axis parent size for measure entries as a performance
     * trade-off ("not fully correct"). This port keys on both axes, because its
     * {@link LayoutInput} has no {@code known_dimensions_are_definite} component and percentage
     * sizes can depend on the parent height. The extra misses can be narrowed when that
     * component is added.
     */
    private static final class CacheEntry<T> {
        /** Exact key of the horizontal axis, see {@link #axisKey}. */
        private long axisKeyWidth;
        /** Exact key of the vertical axis, see {@link #axisKey}. */
        private long axisKeyHeight;
        /** Exact key of the parent size, see {@link #parentKey}. */
        private long parentKey;
        /**
         * Fast-path fingerprint of the packed key, used only to reject measure entries before
         * the exact comparison. Equal keys always have equal fingerprints, but equal
         * fingerprints are re-validated against the packed key.
         */
        private int keyHash;
        /** Whether the entry currently holds a result; clearing a cache invalidates without deallocating. */
        private boolean valid;
        private T content;

        private void set(long axisKeyWidth, long axisKeyHeight, long parentKey, int keyHash, T content) {
            this.axisKeyWidth = axisKeyWidth;
            this.axisKeyHeight = axisKeyHeight;
            this.parentKey = parentKey;
            this.keyHash = keyHash;
            this.valid = true;
            this.content = content;
        }

        /**
         * Whether this entry holds exactly the given packed key.
         *
         * <p>{@link LayoutCache#axisKey} and {@link LayoutCache#parentKey} encode every value the
         * cache keys on, so a match of the three packed words is a match of the whole key.
         */
        private boolean matches(long axisKeyWidth, long axisKeyHeight, long parentKey) {
            return this.axisKeyWidth == axisKeyWidth
                && this.axisKeyHeight == axisKeyHeight
                && this.parentKey == parentKey;
        }

        private void invalidate() {
            valid = false;
            content = null;
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
        // The public, direction-less lookup accepts entries of every direction: its callers
        // (and the cache-injection API built on it) have no way to name the resolved direction.
        return get(inputs, null);
    }

    /**
     * Try to retrieve a cached result for the given layout inputs and resolved direction.
     *
     * <p>Behaves like {@link #get(LayoutInput)} but additionally requires the entry to have been
     * computed with the same resolved direction, unless either side is unknown ({@code null}).
     * {@code direction: inherit} is resolved from the ancestors and is not part of the
     * {@link LayoutInput} key, so this is the lookup that keeps an ancestor's direction change
     * from reusing a mirrored layout. Package-private: the public API has no direction slot.</p>
     *
     * @param inputs            the inputs of the layout pass
     * @param resolvedDirection the direction the node currently resolves to, or {@code null} to
     *                          accept any stored direction
     * @return the cached output, or {@code null} if there is no usable entry
     */
    LayoutOutput get(LayoutInput inputs, TaffyDirection resolvedDirection) {
        if (isEmpty) return null;

        if (!directionMatches(resolvedDirection)) {
            return null;
        }

        if (mode != CacheMode.from(inputs)) {
            return null;
        }

        return switch (inputs.runMode()) {
            // A context-free entry was computed under an inherited BlockContext. Callers of this
            // generic lookup cannot prove that their own context holds no floats, so the entry is
            // never handed out here: only getContextFreeFinalEntry, which is called from the one
            // place that checks the inherited float state and the resolved direction, may
            // return it.
            case PERFORM_LAYOUT -> finalEntryContextFree ? null : getFinalLayoutEntry(inputs);
            case COMPUTE_SIZE -> getMeasureEntry(inputs);
            case PERFORM_HIDDEN_LAYOUT -> null;
        };
    }

    /**
     * Try to retrieve a final-layout result that a same-BFC block child may reuse.
     *
     * <p>This is the only lookup that honours the context-free mark documented on
     * {@link #finalEntryContextFree}: it returns a result only when the stored final entry was
     * produced by a same-BFC block layout whose inherited context held no floats and which placed
     * no floats itself. A plain {@link #get(LayoutInput)} never consults the mark, so callers that
     * cannot reason about the inherited {@code BlockContext} are unaffected.</p>
     *
     * <p>The resolved direction is compared as well: {@code direction: inherit} is not part of the
     * {@link LayoutInput} key, so an ancestor's direction change would otherwise be invisible.</p>
     *
     * @param inputs    the inputs of the layout pass; must be a {@link RunMode#PERFORM_LAYOUT} request
     * @param direction the direction the node currently resolves to
     * @return the cached output, or {@code null} if there is no reuse-safe entry
     */
    LayoutOutput getContextFreeFinalEntry(LayoutInput inputs, TaffyDirection direction) {
        if (isEmpty || !finalEntryContextFree || finalEntryDirection != direction.ordinal()) {
            return null;
        }
        if (!directionMatches(direction)) {
            return null;
        }
        if (inputs.runMode() != RunMode.PERFORM_LAYOUT) {
            return null;
        }
        if (mode != CacheMode.from(inputs)) {
            return null;
        }
        return getFinalLayoutEntry(inputs);
    }

    /**
     * Whether entries stored under {@link #cacheDirection} may answer a lookup for the given
     * direction.
     *
     * <p>A {@code null} on either side is unknown and therefore compatible: the public API has no
     * direction parameter, so the direction-less store cannot declare one and the direction-less
     * lookup cannot require one. Two concrete directions must be equal.</p>
     */
    private boolean directionMatches(TaffyDirection resolvedDirection) {
        return resolvedDirection == null || cacheDirection == null || cacheDirection == resolvedDirection;
    }

    private LayoutOutput getFinalLayoutEntry(LayoutInput inputs) {
        CacheEntry<LayoutOutput> entry = finalLayoutEntry;
        if (entry == null || !entry.valid) {
            return null;
        }
        // There is exactly one final entry: compare the exact packed key directly. The key is
        // collision-free, so no separate field comparison is needed after a match.
        FloatSize known = inputs.knownDimensions();
        TaffySize<AvailableSpace> availableSpace = inputs.availableSpace();
        return entry.matches(
                axisKey(known.width, availableSpace.width),
                axisKey(known.height, availableSpace.height),
                parentKey(inputs.parentSize()))
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
        long axisKeyWidth = axisKey(known.width, availableSpace.width);
        long axisKeyHeight = axisKey(known.height, availableSpace.height);
        long parentKey = parentKey(parent);
        int keyHash = keyHash(axisKeyWidth, axisKeyHeight, parentKey);

        int last = lastMeasureEntry;
        if (last >= 0) {
            CacheEntry<FloatSize> entry = measureEntries[last];
            if (entry != null && entry.valid && entry.keyHash == keyHash
                && entry.matches(axisKeyWidth, axisKeyHeight, parentKey)) {
                recentlyUsedEntries |= 1 << last;
                return LayoutOutput.fromOuterSize(entry.content);
            }
        }

        for (int i = 0; i < CACHE_SIZE; i++) {
            CacheEntry<FloatSize> entry = measureEntries[i];
            // The fingerprint rejects almost every non-matching slot with one integer
            // comparison, before its key words are touched; a fingerprint match is still
            // fully re-validated against the packed key below.
            if (entry == null || !entry.valid || entry.keyHash != keyHash) {
                continue;
            }
            if (entry.matches(axisKeyWidth, axisKeyHeight, parentKey)) {
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
        // The public store cannot name the direction; see store(LayoutInput, LayoutOutput,
        // TaffyDirection): it keeps the direction the cache already carries.
        store(inputs, layoutOutput, null);
    }

    /**
     * Store a computed result in the cache, tagged with the resolved direction it was computed
     * with.
     *
     * <p>Storing a result computed with a different direction first drops the existing entries:
     * the entries are only valid for one direction because {@code direction: inherit} is not part
     * of the {@link LayoutInput} key. This is the same rejection a direction component in every
     * key would produce, without enlarging the packed key. A {@code null} direction (the public
     * API path) keeps the direction the cache already carries, or leaves it unknown on an empty
     * cache.</p>
     *
     * <p>The rest of the contract is documented on {@link #store(LayoutInput, LayoutOutput)}:
     * a different cache-wide mode drops the entries too, and measure results carrying
     * margin-collapse metadata are never stored.</p>
     *
     * @param inputs            the inputs of the layout pass
     * @param layoutOutput      the computed result
     * @param resolvedDirection the direction the node resolved to during this layout, or
     *                          {@code null} to keep the current cache direction
     */
    void store(LayoutInput inputs, LayoutOutput layoutOutput, TaffyDirection resolvedDirection) {
        if (inputs.runMode() == RunMode.PERFORM_HIDDEN_LAYOUT) {
            return;
        }

        // A direction-less store is the public compatibility path: it cannot declare a direction,
        // so it keeps the one the cache already carries (unknown for a fresh cache). A
        // direction-aware store whose direction differs invalidates every entry.
        TaffyDirection inputDirection = resolvedDirection != null ? resolvedDirection : cacheDirection;
        int inputMode = CacheMode.from(inputs);
        if (mode != inputMode || cacheDirection != inputDirection) {
            clearEntries();
            mode = inputMode;
            cacheDirection = inputDirection;
        }

        FloatSize known = inputs.knownDimensions();
        TaffySize<AvailableSpace> availableSpace = inputs.availableSpace();
        FloatSize parent = inputs.parentSize();
        long axisKeyWidth = axisKey(known.width, availableSpace.width);
        long axisKeyHeight = axisKey(known.height, availableSpace.height);
        long parentKey = parentKey(parent);
        int keyHash = keyHash(axisKeyWidth, axisKeyHeight, parentKey);

        if (inputs.runMode() == RunMode.PERFORM_LAYOUT) {
            isEmpty = false;
            CacheEntry<LayoutOutput> entry = finalLayoutEntry;
            if (entry == null) {
                entry = new CacheEntry<>();
                finalLayoutEntry = entry;
            }
            entry.set(axisKeyWidth, axisKeyHeight, parentKey, keyHash, layoutOutput);
            // An ordinary store never carries the same-BFC reuse permission; the dedicated
            // storeContextFreeFinalEntry below is the only path that grants it.
            finalEntryContextFree = false;
            finalEntryDirection = -1;
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
                && entry.matches(axisKeyWidth, axisKeyHeight, parentKey)) {
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
        entry.set(axisKeyWidth, axisKeyHeight, parentKey, keyHash, content);
        lastMeasureEntry = entryIndex;
        recentlyUsedEntries |= 1 << entryIndex;
        nextMeasureEntry++;
        if (nextMeasureEntry == CACHE_SIZE) {
            nextMeasureEntry = 0;
        }
    }

    /**
     * Stores a final-layout result that a later same-BFC block pass may reuse.
     *
     * <p>The caller must only use this for a {@link RunMode#PERFORM_LAYOUT} result that was
     * computed with a float-free inherited {@code BlockContext} and that placed no floats itself:
     * those are the results whose value is independent of the mutable BFC float state. The entry
     * is stored exactly like {@link #store(LayoutInput, LayoutOutput)} and is additionally marked
     * context-free; only {@link #getContextFreeFinalEntry(LayoutInput, TaffyDirection)} can observe
     * the mark.</p>
     *
     * @param inputs       the inputs of the layout pass
     * @param layoutOutput the computed result
     * @param direction    the direction the node resolved to during this layout
     */
    void storeContextFreeFinalEntry(
        LayoutInput inputs, LayoutOutput layoutOutput, TaffyDirection direction) {

        if (inputs.runMode() != RunMode.PERFORM_LAYOUT) {
            throw new IllegalArgumentException("context-free reuse is only defined for final layout");
        }
        // The context-free entry is a normal cache entry for the cache-wide direction as well;
        // the dedicated mark below adds the float-context permission on top of it.
        store(inputs, layoutOutput, direction);
        finalEntryContextFree = true;
        finalEntryDirection = direction.ordinal();
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

    /** Set in a packed axis key when the axis carries a known dimension. */
    private static final long KNOWN_AXIS_BIT = 1L << 63;
    /** Shift of the available-space type in a packed axis key without a known dimension. */
    private static final int AVAILABLE_SPACE_TYPE_SHIFT = 61;
    private static final long AVAILABLE_SPACE_DEFINITE = 0b00L << AVAILABLE_SPACE_TYPE_SHIFT;
    private static final long AVAILABLE_SPACE_MIN_CONTENT = 0b01L << AVAILABLE_SPACE_TYPE_SHIFT;
    private static final long AVAILABLE_SPACE_MAX_CONTENT = 0b10L << AVAILABLE_SPACE_TYPE_SHIFT;
    /** Stands for a {@code null} available space, which the cache key compares by identity. */
    private static final long AVAILABLE_SPACE_NULL = 0b11L << AVAILABLE_SPACE_TYPE_SHIFT;
    /** Bits 0-31 of a packed axis key carry the dimension or definite-space value bits. */
    private static final long AXIS_VALUE_MASK = 0xFFFFFFFFL;

    /**
     * Packs one axis of the cache key into an exact 64-bit key.
     *
     * <p>A known dimension sets {@link #KNOWN_AXIS_BIT} and stores its
     * {@link Float#floatToIntBits} value; the available space is then ignored (CR-01). Without a
     * known dimension the key stores the available-space type and, for a definite space, its
     * value bits. A {@code null} available space gets a type of its own: the previous
     * field-by-field comparison compared two {@code null}s as equal and every other pair as
     * unequal, which the type code reproduces exactly. {@code floatToIntBits} canonicalizes
     * NaNs, so every NaN still compares equal, while {@code -0.0f} and {@code 0.0f} still
     * differ.
     *
     * <p>Every bit of the compared values is retained, so this is an exact key and not a hash:
     * equal keys mean equal inputs, and no collision can turn a miss into a hit.
     */
    private static long axisKey(float knownDimension, AvailableSpace availableSpace) {
        if (!Float.isNaN(knownDimension)) {
            return KNOWN_AXIS_BIT | (Float.floatToIntBits(knownDimension) & AXIS_VALUE_MASK);
        }
        if (availableSpace == null) {
            return AVAILABLE_SPACE_NULL;
        }
        return switch (availableSpace.getType()) {
            case DEFINITE -> AVAILABLE_SPACE_DEFINITE
                | (Float.floatToIntBits(availableSpace.getValue()) & AXIS_VALUE_MASK);
            case MIN_CONTENT -> AVAILABLE_SPACE_MIN_CONTENT;
            case MAX_CONTENT -> AVAILABLE_SPACE_MAX_CONTENT;
        };
    }

    /** Packs both parent dimensions into one exact 64-bit key. */
    private static long parentKey(FloatSize parent) {
        return ((long) Float.floatToIntBits(parent.width) << 32)
            | (Float.floatToIntBits(parent.height) & 0xFFFFFFFFL);
    }

    /**
     * A fingerprint of the packed key, used to reject non-matching measure entries before the
     * exact comparison.
     *
     * <p>Equal packed keys always produce the same fingerprint; the converse is not true, which
     * is why a matching fingerprint is re-validated against the packed key.
     */
    private static int keyHash(long axisKeyWidth, long axisKeyHeight, long parentKey) {
        long hash = axisKeyWidth * 0x9E3779B97F4A7C15L
            + axisKeyHeight * 0xC2B2AE3D27D4EB4FL
            + parentKey * 0x165667B19E3779F9L;
        hash ^= hash >>> 33;
        hash *= 0xFF51AFD7ED558CCDL;
        hash ^= hash >>> 29;
        return (int) (hash ^ (hash >>> 32));
    }

    /**
     * Fingerprint of a key still given as its raw fields; equivalent to packing it first and
     * hashing the packed words.
     */
    private static int keyHash(FloatSize known, TaffySize<AvailableSpace> availableSpace, FloatSize parent) {
        return keyHash(
            axisKey(known.width, availableSpace.width),
            axisKey(known.height, availableSpace.height),
            parentKey(parent));
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
        finalEntryContextFree = false;
        finalEntryDirection = -1;
        cacheDirection = null;
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
