package dev.vfyjxf.taffy.style;

import java.util.Objects;

/**
 * A unit of linear measurement representing a CSS dimension value.
 * Can be a fixed length, a percentage, or auto.
 */
public final class Dimension {
    
    /** The type of dimension value */
    public enum Type {
        /** An absolute length in some abstract units (pixels, logical pixels, etc.) */
        LENGTH,
        /** A percentage length relative to the size of the containing block */
        PERCENT,
        /** The dimension should be automatically computed */
        AUTO
    }

    private final Type type;
    private final float value;

    private Dimension(Type type, float value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Creates an absolute length value
     */
    public static Dimension length(float value) {
        return new Dimension(Type.LENGTH, value);
    }

    /**
     * Creates a percentage length value.
     * Note: percentages are represented as a float in the range [0.0, 1.0] NOT [0.0, 100.0]
     */
    public static Dimension percent(float value) {
        return new Dimension(Type.PERCENT, value);
    }

    /**
     * Creates an auto value
     */
    public static Dimension auto() {
        return AUTO;
    }

    /** Zero length */
    public static final Dimension ZERO = length(0);
    
    /** Auto value singleton */
    public static final Dimension AUTO = new Dimension(Type.AUTO, 0);

    /**
     * Convert from LengthPercentage
     */
    public static Dimension from(LengthPercentage lp) {
        return switch (lp.getType()) {
            case LENGTH -> length(lp.getValue());
            case PERCENT -> percent(lp.getValue());
        };
    }

    /**
     * Convert from LengthPercentageAuto
     */
    public static Dimension from(LengthPercentageAuto lpa) {
        return switch (lpa.getType()) {
            case LENGTH -> length(lpa.getValue());
            case PERCENT -> percent(lpa.getValue());
            case AUTO -> AUTO;
        };
    }

    /**
     * Returns the type of this dimension
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the raw value
     */
    public float getValue() {
        return value;
    }

    /**
     * Returns true if this is an absolute length
     */
    public boolean isLength() {
        return type == Type.LENGTH;
    }

    /**
     * Returns true if this is a percentage
     */
    public boolean isPercent() {
        return type == Type.PERCENT;
    }

    /**
     * Returns true if this is auto
     */
    public boolean isAuto() {
        return type == Type.AUTO;
    }

    /**
     * Returns the length value as an option (null if not a length)
     */
    public float intoOption() {
        return type == Type.LENGTH ? value : Float.NaN;
    }

    /**
     * Resolve this dimension against a context size.
     * Returns NaN for auto values.
     */
    public float maybeResolve(float context) {
        return switch (type) {
            case LENGTH -> value;
            case PERCENT -> Float.isNaN(context) ? Float.NaN : context * value;
            case AUTO -> Float.NaN;
        };
    }

    /**
     * Resolve this dimension or return zero if unresolvable
     */
    public float resolveOrZero(float context) {
        float resolved = maybeResolve(context);
        return Float.isNaN(resolved) ? 0f : resolved;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dimension that = (Dimension) o;
        return type == that.type && (type == Type.AUTO || Float.compare(value, that.value) == 0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        return switch (type) {
            case LENGTH -> value + "px";
            case PERCENT -> (value * 100) + "%";
            case AUTO -> "auto";
        };
    }
}
