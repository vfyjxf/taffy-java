package dev.vfyjxf.taffy.util;

/**
 * Utility class for math operations.
 */
public final class TaffyMath {

    private TaffyMath() {
    }

    /**
     * Returns the maximum of two float values.
     */
    public static float max(float a, float b) {
        return Math.max(a, b);
    }

    /**
     * Returns the minimum of two float values.
     */
    public static float min(float a, float b) {
        return Math.min(a, b);
    }

    /**
     * Clamp a value between min and max.
     * Per CSS spec, if min > max, min takes precedence.
     */
    public static float clamp(float value, float min, float max) {
        float result = value;
        // Apply max first, then min, so min takes precedence if min > max
        if (!Float.isNaN(max)) {
            result = Math.min(result, max);
        }
        if (!Float.isNaN(min)) {
            result = Math.max(result, min);
        }
        return result;
    }

    /**
     * Maybe clamp - clamps only if the constraint is present.
     */
    public static float maybeClamp(float value, float min, float max) {
        if (Float.isNaN(value)) return Float.NaN;
        return clamp(value, min, max);
    }

    /**
     * Returns the absolute value.
     */
    public static float abs(float value) {
        return Math.abs(value);
    }

    /**
     * Returns the floor of a value.
     */
    public static float floor(float value) {
        return (float) Math.floor(value);
    }

    /**
     * Returns the ceiling of a value.
     */
    public static float ceil(float value) {
        return (float) Math.ceil(value);
    }

    /**
     * Rounds to the nearest integer.
     */
    public static float round(float value) {
        return Math.round(value);
    }

    /**
     * Check if a float is NaN.
     */
    public static boolean isNaN(float value) {
        return Float.isNaN(value);
    }

    /**
     * Check if a float is defined (not NaN and not infinite).
     */
    public static boolean isDefined(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    /**
     * Maybe add - returns null if first value is null, otherwise adds the values.
     * Follows Rust. maybe_add(Option) semantics:
     * - (Some(l), Some(r)) => Some(l + r)
     * - (Some(l), None) => Some(l)
     * - (None, Some(r)) => None
     * - (None, None) => None
     */
    public static float maybeAdd(float a, float b) {
        if (Float.isNaN(a)) return Float.NaN;
        if (Float.isNaN(b)) return a;
        return a + b;
    }

    /**
     * Maybe subtract - subtracts only if both values are present.
     */
    public static float maybeSub(float a, float b) {
        if (Float.isNaN(a)) return Float.NaN;
        if (Float.isNaN(b)) return a;
        return a - b;
    }

    /**
     * Maybe multiply - multiplies only if both values are present.
     */
    public static float maybeMul(float a, float b) {
        if (Float.isNaN(a) || Float.isNaN(b)) return Float.NaN;
        return a * b;
    }

    /**
     * Maybe divide - divides only if both values are present and divisor is not zero.
     */
    public static float maybeDiv(float a, float b) {
        if (Float.isNaN(a) || Float.isNaN(b) || b == 0) return Float.NaN;
        return a / b;
    }

    /**
     * Maybe max - returns the max of two optional values.
     * Returns NaN if first value is NaN (preserving the "undefined" semantic).
     * Follows Rust's Option.maybe_max(Option) semantics:
     * - (Some(l), Some(r)) => Some(max(l, r))
     * - (Some(l), None) => Some(l)
     * - (None, Some(r)) => None
     * - (None, None) => None
     */
    public static float maybeMax(float a, float b) {
        if (Float.isNaN(a)) return Float.NaN;
        if (Float.isNaN(b)) return a;  // (Some(l), None) => Some(l)
        return Math.max(a, b);
    }

    /**
     * Maybe min - returns the min of two optional values.
     * Returns NaN if first value is NaN (preserving the "undefined" semantic).
     * Follows Rust's Option.maybe_min(Option) semantics:
     * - (Some(l), Some(r)) => Some(min(l, r))
     * - (Some(l), None) => Some(l)
     * - (None, Some(r)) => None
     * - (None, None) => None
     */
    public static float maybeMin(float a, float b) {
        if (Float.isNaN(a)) return Float.NaN;
        if (Float.isNaN(b)) return a;  // (Some(l), None) => Some(l)
        return Math.min(a, b);
    }

    /**
     * Returns the value or a default if null.
     */
    public static float orElse(Float value, float defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Max of float and optional Float (returns float if optional is null).
     */
    public static float f32Max(float a, Float b) {
        if (b == null) return a;
        return Math.max(a, b);
    }

    /**
     * Apply aspect ratio to get width from height.
     */
    public static Float applyAspectRatioWidth(Float height, Float aspectRatio) {
        if (height == null || aspectRatio == null) return null;
        return height * aspectRatio;
    }

    /**
     * Apply aspect ratio to get height from width.
     */
    public static Float applyAspectRatioHeight(Float width, Float aspectRatio) {
        if (width == null || aspectRatio == null) return null;
        return width / aspectRatio;
    }
}
