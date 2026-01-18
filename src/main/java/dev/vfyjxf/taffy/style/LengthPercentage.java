package dev.vfyjxf.taffy.style;

import java.util.Objects;

/**
 * A unit of linear measurement that can be either a fixed length or a percentage.
 */
public final class LengthPercentage {

    /**
     * The type of length value
     */
    public enum Type {
        /**
         * An absolute length in some abstract units (pixels, logical pixels, etc.)
         */
        LENGTH,
        /**
         * A percentage length relative to the size of the containing block
         */
        PERCENT
    }

    private final Type type;
    private final float value;

    private LengthPercentage(Type type, float value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Creates an absolute length value
     */
    public static LengthPercentage length(float value) {
        return new LengthPercentage(Type.LENGTH, value);
    }

    /**
     * Creates a percentage length value.
     * Note: percentages are represented as a float in the range [0.0, 1.0] NOT [0.0, 100.0]
     */
    public static LengthPercentage percent(float value) {
        return new LengthPercentage(Type.PERCENT, value);
    }

    /**
     * Zero length
     */
    public static final LengthPercentage ZERO = length(0);

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
     * Resolve this length against a context size
     *
     * @param context The context size (for percentage resolution)
     * @return The resolved length in pixels
     */
    public float resolve(float context) {
        return switch (type) {
            case LENGTH -> value;
            case PERCENT -> context * value;
        };
    }

    /**
     * Resolve this length against a potentially null context size.
     * Returns NaN if this is a percentage and context is null.
     */
    public float maybeResolve(float context) {
        return switch (type) {
            case LENGTH -> value;
            case PERCENT -> Float.isNaN(context) ? Float.NaN : context * value;
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
        LengthPercentage that = (LengthPercentage) o;
        return type == that.type && Float.compare(value, that.value) == 0;
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
        };
    }
}
