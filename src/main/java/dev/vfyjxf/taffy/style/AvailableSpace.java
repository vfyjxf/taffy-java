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

    private AvailableSpace(Type type, float value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Creates a definite available space with the given value
     */
    public static AvailableSpace definite(float value) {
        return new AvailableSpace(Type.DEFINITE, value);
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
