package dev.vfyjxf.taffy.style;

import java.util.Objects;

/**
 * Represents a track sizing function for grid layout.
 * Can be fixed, min/max content, flexible (fr), auto, or minmax().
 */
public final class TrackSizingFunction {

    /**
     * The type of track sizing function
     */
    public enum Type {
        /**
         * A fixed length
         */
        FIXED,
        /**
         * min-content sizing
         */
        MIN_CONTENT,
        /**
         * max-content sizing
         */
        MAX_CONTENT,
        /**
         * fit-content(limit) sizing
         */
        FIT_CONTENT,
        /**
         * auto sizing
         */
        AUTO,
        /**
         * Flexible sizing (fr units)
         */
        FLEX,
        /**
         * minmax(min, max)
         */
        MINMAX
    }

    private final Type type;
    private final LengthPercentage lengthValue;
    private final float flexValue;
    private final TrackSizingFunction minFunc;
    private final TrackSizingFunction maxFunc;

    private TrackSizingFunction(Type type, LengthPercentage lengthValue, float flexValue,
                                TrackSizingFunction minFunc, TrackSizingFunction maxFunc) {
        // Per CSS Grid spec, minmax() arguments cannot be another minmax().
        // See: https://www.w3.org/TR/css-grid-1/#valdef-grid-template-columns-minmax
        // minmax( [ <length> | <percentage> | min-content | max-content | auto ],
        //         [ <length> | <percentage> | <flex> | min-content | max-content | auto ] )
        if (type == Type.MINMAX) {
            if (minFunc != null && minFunc.type == Type.MINMAX) {
                throw new IllegalArgumentException("minmax() min argument cannot be another minmax()");
            }
            if (maxFunc != null && maxFunc.type == Type.MINMAX) {
                throw new IllegalArgumentException("minmax() max argument cannot be another minmax()");
            }
        }
        this.type = type;
        this.lengthValue = lengthValue;
        this.flexValue = flexValue;
        this.minFunc = minFunc;
        this.maxFunc = maxFunc;
    }

    /**
     * Creates a fixed track size
     */
    public static TrackSizingFunction fixed(LengthPercentage value) {
        return new TrackSizingFunction(Type.FIXED, value, 0, null, null);
    }

    /**
     * Creates a fixed track size with a length value
     */
    public static TrackSizingFunction fixed(float value) {
        return fixed(LengthPercentage.length(value));
    }

    /**
     * Creates a fixed track size with a percentage value
     */
    public static TrackSizingFunction percent(float value) {
        return fixed(LengthPercentage.percent(value));
    }

    /**
     * Creates a min-content track
     */
    public static TrackSizingFunction minContent() {
        return MIN_CONTENT;
    }

    /**
     * Creates a max-content track
     */
    public static TrackSizingFunction maxContent() {
        return MAX_CONTENT;
    }

    /**
     * Creates a fit-content track
     */
    public static TrackSizingFunction fitContent(LengthPercentage limit) {
        return new TrackSizingFunction(Type.FIT_CONTENT, limit, 0, null, null);
    }

    /**
     * Creates an auto track
     */
    public static TrackSizingFunction auto() {
        return AUTO;
    }

    /**
     * Creates a flexible track (fr unit)
     */
    public static TrackSizingFunction flex(float fr) {
        return new TrackSizingFunction(Type.FLEX, null, fr, null, null);
    }

    /**
     * Creates a flexible track (fr unit) - alias for flex()
     */
    public static TrackSizingFunction fr(float fr) {
        return flex(fr);
    }

    /**
     * Creates a minmax track.
     *
     * @param min the minimum track sizing function
     * @param max the maximum track sizing function
     * @return a new minmax TrackSizingFunction
     * @throws IllegalArgumentException if min or max would create a recursive reference
     */
    public static TrackSizingFunction minmax(TrackSizingFunction min, TrackSizingFunction max) {
        return new TrackSizingFunction(Type.MINMAX, null, 0, min, max);
    }

    /**
     * Singleton instances
     */
    public static final TrackSizingFunction MIN_CONTENT = new TrackSizingFunction(Type.MIN_CONTENT, null, 0, null, null);
    public static final TrackSizingFunction MAX_CONTENT = new TrackSizingFunction(Type.MAX_CONTENT, null, 0, null, null);
    public static final TrackSizingFunction AUTO = new TrackSizingFunction(Type.AUTO, null, 0, null, null);

    /**
     * Returns the type
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the length value (for FIXED and FIT_CONTENT types)
     */
    public LengthPercentage getLengthValue() {
        return lengthValue;
    }

    /**
     * Returns the flex value (for FLEX type)
     */
    public float getFlexValue() {
        return flexValue;
    }

    /**
     * Returns the min function (for MINMAX type)
     */
    public TrackSizingFunction getMinFunc() {
        return minFunc;
    }

    /**
     * Returns the max function (for MINMAX type)
     */
    public TrackSizingFunction getMaxFunc() {
        return maxFunc;
    }

    /**
     * Returns true if this is a flexible track
     */
    public boolean isFlexible() {
        if (type == Type.FLEX) return true;
        if (type == Type.MINMAX) {
            return (maxFunc != null && maxFunc.isFlexible());
        }
        return false;
    }

    /**
     * Returns true if this is a fr track
     */
    public boolean isFr() {
        return type == Type.FLEX;
    }

    /**
     * Returns the fr value (alias for getFlexValue)
     */
    public float getFrValue() {
        return flexValue;
    }

    /**
     * Returns true if this is a min-content track
     */
    public boolean isMinContent() {
        return type == Type.MIN_CONTENT;
    }

    /**
     * Returns true if this is a max-content track
     */
    public boolean isMaxContent() {
        return type == Type.MAX_CONTENT;
    }

    /**
     * Returns true if this is an auto track
     */
    public boolean isAuto() {
        return type == Type.AUTO;
    }

    /**
     * Returns true if this is a fixed size track
     */
    public boolean isFixed() {
        return type == Type.FIXED;
    }

    /**
     * Returns true if this is a minmax track
     */
    public boolean isMinmax() {
        return type == Type.MINMAX;
    }

    /**
     * Returns true if this is a fit-content track
     */
    public boolean isFitContent() {
        return type == Type.FIT_CONTENT;
    }

    /**
     * Returns the fit-content argument (the limit)
     */
    public LengthPercentage getFitContentArgument() {
        if (type == Type.FIT_CONTENT) {
            return lengthValue;
        }
        return null;
    }

    /**
     * Returns the fixed value
     */
    public LengthPercentage getFixedValue() {
        return lengthValue;
    }

    /**
     * Returns true if this is an intrinsic track (min-content, max-content, auto, fit-content)
     */
    public boolean isIntrinsic() {
        return type == Type.MIN_CONTENT || type == Type.MAX_CONTENT ||
               type == Type.AUTO || type == Type.FIT_CONTENT;
    }

    /**
     * Returns true if this track has an intrinsic sizing function (min-content, max-content, auto, or fit-content).
     * For minmax() tracks, checks if either min or max is intrinsic.
     * This is used to determine which items need their min-content contribution tracked for re-run detection.
     */
    public boolean hasIntrinsicSizingFunction() {
        return switch (type) {
            case MIN_CONTENT, MAX_CONTENT, AUTO, FIT_CONTENT -> true;
            case MINMAX -> (minFunc != null && minFunc.hasIntrinsicSizingFunction()) ||
                           (maxFunc != null && maxFunc.hasIntrinsicSizingFunction());
            default -> false;
        };
    }

    /**
     * Determine whether at least one of the components ("min" and "max") are fixed sizing function.
     * Required for auto-fill/auto-fit validation.
     * A track has a fixed component if it's a fixed length/percentage or a minmax where either min or max is fixed.
     */
    public boolean hasFixedComponent() {
        return switch (type) {
            case FIXED -> lengthValue != null;
            case MINMAX -> (minFunc != null && minFunc.hasFixedComponent()) ||
                           (maxFunc != null && maxFunc.hasFixedComponent());
            case FIT_CONTENT -> lengthValue != null;  // fit-content has a fixed limit
            default -> false;  // MIN_CONTENT, MAX_CONTENT, AUTO, FLEX are not fixed
        };
    }

    /**
     * Get the definite value of this track sizing function, if it has one.
     * Used for calculating auto-fill/auto-fit repetition counts.
     */
    public float getDefiniteValue(float parentSize) {
        return switch (type) {
            case FIXED -> {
                if (lengthValue == null) yield Float.NaN;
                yield lengthValue.maybeResolve(parentSize);
            }
            case MINMAX -> {
                // Use max if definite, otherwise min
                float maxVal = maxFunc != null ? maxFunc.getDefiniteValue(parentSize) : Float.NaN;
                float minVal = minFunc != null ? minFunc.getDefiniteValue(parentSize) : Float.NaN;
                if (!Float.isNaN(maxVal)) {
                    yield !Float.isNaN(minVal) ? Math.max(maxVal, minVal) : maxVal;
                }
                yield minVal;
            }
            case FIT_CONTENT -> lengthValue != null ? lengthValue.maybeResolve(parentSize) : Float.NaN;
            default -> Float.NaN;
        };
    }

    /**
     * Returns true if this track sizing function depends on the parent size (uses percentage values).
     * This is used to determine if column/row sizing needs to be re-run after initial sizing.
     */
    public boolean usesPercentage() {
        return switch (type) {
            case FIXED, FIT_CONTENT -> lengthValue != null && lengthValue.isPercent();
            case MINMAX -> (minFunc != null && minFunc.usesPercentage()) ||
                           (maxFunc != null && maxFunc.usesPercentage());
            default -> false;
        };
    }

    @Override
    public String toString() {
        return switch (type) {
            case FIXED -> lengthValue.toString();
            case MIN_CONTENT -> "min-content";
            case MAX_CONTENT -> "max-content";
            case FIT_CONTENT -> "fit-content(" + lengthValue + ")";
            case AUTO -> "auto";
            case FLEX -> flexValue + "fr";
            case MINMAX -> "minmax(" + minFunc + ", " + maxFunc + ")";
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrackSizingFunction that = (TrackSizingFunction) o;
        if (type != that.type) return false;
        if (Float.compare(that.flexValue, flexValue) != 0) return false;
        if (!Objects.equals(lengthValue, that.lengthValue)) return false;
        if (!Objects.equals(minFunc, that.minFunc)) return false;
        return Objects.equals(maxFunc, that.maxFunc);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (lengthValue != null ? lengthValue.hashCode() : 0);
        result = 31 * result + (flexValue != 0.0f ? Float.floatToIntBits(flexValue) : 0);
        result = 31 * result + (minFunc != null ? minFunc.hashCode() : 0);
        result = 31 * result + (maxFunc != null ? maxFunc.hashCode() : 0);
        return result;
    }
}
