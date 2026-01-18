package dev.vfyjxf.taffy.style;

import java.util.Objects;

/**
 * A unit of linear measurement that can be a fixed length, a percentage, or auto.
 */
public final class LengthPercentageAuto {
    
    /** The type of length value */
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

    private LengthPercentageAuto(Type type, float value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Creates an absolute length value
     */
    public static LengthPercentageAuto length(float value) {
        return new LengthPercentageAuto(Type.LENGTH, value);
    }

    /**
     * Creates a percentage length value.
     * Note: percentages are represented as a float in the range [0.0, 1.0] NOT [0.0, 100.0]
     */
    public static LengthPercentageAuto percent(float value) {
        return new LengthPercentageAuto(Type.PERCENT, value);
    }

    /**
     * Creates an auto value
     */
    public static LengthPercentageAuto auto() {
        return AUTO;
    }

    /** Zero length */
    public static final LengthPercentageAuto ZERO = length(0);
    
    /** Auto value singleton */
    public static final LengthPercentageAuto AUTO = new LengthPercentageAuto(Type.AUTO, 0);

    /**
     * Convert from LengthPercentage
     */
    public static LengthPercentageAuto from(LengthPercentage lp) {
        return switch (lp.getType()) {
            case LENGTH -> length(lp.getValue());
            case PERCENT -> percent(lp.getValue());
        };
    }

    /**
     * Returns the type of this length
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
     * Resolve this length against a context size.
     * Returns NaN for auto values.
     */
    public float resolveToOption(float context) {
        return switch (type) {
            case LENGTH -> value;
            case PERCENT -> context * value;
            case AUTO -> Float.NaN;
        };
    }

    /**
     * Resolve this length against a potentially null context size.
     * Returns null if this is auto or if this is a percentage and context is null.
     */
    public float maybeResolve(float context) {
        return switch (type) {
            case LENGTH -> value;
            case PERCENT -> Float.isNaN(context) ? Float.NaN : context * value;
            case AUTO -> Float.NaN;
        };
    }

    /**
     * Resolve this length or return zero if unresolvable
     */
    public float resolveOrZero(float context) {
        float resolved = maybeResolve(context);
        return Float.isNaN(resolved) ? 0f : resolved;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LengthPercentageAuto that = (LengthPercentageAuto) o;
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
