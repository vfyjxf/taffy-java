package dev.vfyjxf.taffy.style;

import java.util.Objects;

import static java.lang.Float.NaN;

/**
 * Represents the available space for layout in a single axis.
 * 
 * This is used to communicate sizing constraints from parent to child during layout.
 */
public final class AvailableSpace {
    
    /** The type of available space constraint */
    public enum Type {
        /** The amount of space available is the specified number of pixels */
        DEFINITE,
        /** The amount of space available is indefinite and the node should be laid out under a min-content constraint */
        MIN_CONTENT,
        /** The amount of space available is indefinite and the node should be laid out under a max-content constraint */
        MAX_CONTENT
    }

    private final Type type;
    private final float value;

    /**
     * Number of slots in the per-thread direct-mapped cache of definite available spaces.
     *
     * <p>Layout hands the same handful of definite values (0, container extents, child sizes)
     * down through hundreds of thousands of nodes, so allocating a fresh instance for every
     * constraint made {@link #definite(float)} a top allocation site. The cache is deliberately
     * tiny and bounded: it is a fixed-size array pair per thread, never grows, and cannot retain
     * anything beyond the last {@value #DEFINITE_CACHE_SIZE} distinct values that happened to
     * land in each slot.</p>
     */
    static final int DEFINITE_CACHE_SIZE = 256;

    private static final int DEFINITE_CACHE_MASK = DEFINITE_CACHE_SIZE - 1;

    /**
     * Per-thread, bounded direct-mapped cache for {@link #definite(float)}.
     *
     * <p>Values are keyed by {@link Float#floatToIntBits(float)}: {@code -0.0} and {@code 0.0}
     * keep distinct keys and all NaN payloads collapse to the canonical NaN. A hit returns the
     * cached immutable instance; a miss allocates one new instance and replaces the slot's
     * previous entry. {@link AvailableSpace} is immutable and has no identity-based behaviour, so
     * sharing instances does not change the public contract.</p>
     */
    private static final ThreadLocal<DefiniteCache> DEFINITE_CACHE =
        ThreadLocal.withInitial(DefiniteCache::new);

    private static final class DefiniteCache {
        private final int[] keys = new int[DEFINITE_CACHE_SIZE];
        private final AvailableSpace[] values = new AvailableSpace[DEFINITE_CACHE_SIZE];

        private AvailableSpace get(float value) {
            int bits = Float.floatToIntBits(value);
            // Full 32-bit avalanche (murmur3 fmix32): `bits * constant` alone leaves the slot
            // index dependent only on the low mantissa bits, so plain layout values such as
            // 0.0/-0.0 or 10.0/2000.0 would collide in one slot and throw each other out.
            int hash = bits;
            hash ^= hash >>> 16;
            hash *= 0x85EBCA6B;
            hash ^= hash >>> 13;
            hash *= 0xC2B2AE35;
            hash ^= hash >>> 16;
            int index = hash & DEFINITE_CACHE_MASK;
            AvailableSpace cached = values[index];
            if (cached != null && keys[index] == bits) {
                return cached;
            }
            AvailableSpace created = new AvailableSpace(Type.DEFINITE, value);
            keys[index] = bits;
            values[index] = created;
            return created;
        }
    }

    private AvailableSpace(Type type, float value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Creates a definite available space with the given value.
     *
     * <p>Definite available spaces are immutable, so this method may return a cached instance
     * for a value that was handed out before on the same thread (see {@link #DEFINITE_CACHE}).
     * Callers must not rely on object identity between two results.</p>
     */
    public static AvailableSpace definite(float value) {
        return DEFINITE_CACHE.get().get(value);
    }

    /**
     * Returns a min-content available space
     */
    public static AvailableSpace minContent() {
        return MIN_CONTENT;
    }

    /**
     * Returns a max-content available space
     */
    public static AvailableSpace maxContent() {
        return MAX_CONTENT;
    }

    /** Singleton instance for MIN_CONTENT */
    public static final AvailableSpace MIN_CONTENT = new AvailableSpace(Type.MIN_CONTENT, 0);
    
    /** Singleton instance for MAX_CONTENT */
    public static final AvailableSpace MAX_CONTENT = new AvailableSpace(Type.MAX_CONTENT, 0);
    
    /** Zero definite space */
    public static final AvailableSpace ZERO = definite(0);

    /**
     * Returns the type of this available space
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the value if this is a definite space, otherwise 0
     */
    public float getValue() {
        return value;
    }

    /**
     * Returns true if this is a definite space
     */
    public boolean isDefinite() {
        return type == Type.DEFINITE;
    }

    /**
     * Returns true if this is a min-content constraint
     */
    public boolean isMinContent() {
        return type == Type.MIN_CONTENT;
    }

    /**
     * Returns true if this is a max-content constraint
     */
    public boolean isMaxContent() {
        return type == Type.MAX_CONTENT;
    }

    /**
     * Returns the value as an Option (Float or NaN for indefinite)
     */
    public float intoOption() {
        return type == Type.DEFINITE ? value : NaN;
    }

    /**
     * Applies the provided function to the value if definite, otherwise returns self
     */
    public AvailableSpace mapDefiniteValue(java.util.function.Function<Float, Float> f) {
        if (type == Type.DEFINITE) {
            return definite(f.apply(value));
        }
        return this;
    }

    /**
     * Subtract from definite value
     */
    public AvailableSpace maybeSub(float amount) {
        if (type == Type.DEFINITE) {
            return definite(value - amount);
        }
        return this;
    }

    /**
     * Add to definite value
     */
    public AvailableSpace maybeAdd(float amount) {
        if (type == Type.DEFINITE) {
            return definite(value + amount);
        }
        return this;
    }

    /**
     * Take maximum with definite value
     */
    public AvailableSpace maybeMax(float amount) {
        if (type == Type.DEFINITE) {
            return definite(Math.max(value, amount));
        }
        return this;
    }

    /**
     * Take minimum with definite value
     */
    public AvailableSpace maybeMin(float amount) {
        if (type == Type.DEFINITE) {
            return definite(Math.min(value, amount));
        }
        return this;
    }

    /**
     * Return value or default for indefinite
     */
    public float unwrapOr(float defaultValue) {
        return type == Type.DEFINITE ? value : defaultValue;
    }

    /**
     * Check if this AvailableSpace is "roughly equal" to another.
     * Used for cache matching - two available spaces are roughly equal if:
     * - Both are the same type (Definite, MinContent, MaxContent)
     * - For Definite types, the values are within f32::EPSILON of each other
     */
    public boolean isRoughlyEqual(AvailableSpace other) {
        if (this == other) return true;
        if (other == null) return false;
        
        // Same type with same value
        if (this.type == other.type) {
            if (this.type == Type.DEFINITE) {
                return Math.abs(this.value - other.value) < 0.0001f; // f32::EPSILON equivalent
            }
            return true; // Both MinContent or both MaxContent
        }
        
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AvailableSpace that = (AvailableSpace) o;
        return type == that.type && (type != Type.DEFINITE || Float.compare(value, that.value) == 0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        return switch (type) {
            case DEFINITE -> "Definite(" + value + ")";
            case MIN_CONTENT -> "MinContent";
            case MAX_CONTENT -> "MaxContent";
        };
    }
}
