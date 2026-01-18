package dev.vfyjxf.taffy.util;

/**
 * Utility class for mathematical operations that handle nullable Float values.
 * Ported from taffy/src/util/math.rs
 */
public final class MaybeMath {

    private MaybeMath() {
        // Utility class, not instantiable
    }

    // ==================== Option<Float> with Option<Float> ====================

    /**
     * Returns the minimum of two nullable Float values.
     * If lhs is null, returns null. If rhs is null, returns lhs.
     */
    public static Float maybeMin(Float lhs, Float rhs) {
        if (lhs == null) {
            return null;
        }
        if (rhs == null) {
            return lhs;
        }
        return Math.min(lhs, rhs);
    }

    /**
     * Returns the maximum of two nullable Float values.
     * If lhs is null, returns null. If rhs is null, returns lhs.
     */
    public static Float maybeMax(Float lhs, Float rhs) {
        if (lhs == null) {
            return null;
        }
        if (rhs == null) {
            return lhs;
        }
        return Math.max(lhs, rhs);
    }

    /**
     * Adds two nullable Float values.
     * If lhs is null, returns null. If rhs is null, returns lhs.
     */
    public static Float maybeAdd(Float lhs, Float rhs) {
        if (lhs == null) {
            return null;
        }
        if (rhs == null) {
            return lhs;
        }
        return lhs + rhs;
    }

    /**
     * Subtracts rhs from lhs for nullable Float values.
     * If lhs is null, returns null. If rhs is null, returns lhs.
     */
    public static Float maybeSub(Float lhs, Float rhs) {
        if (lhs == null) {
            return null;
        }
        if (rhs == null) {
            return lhs;
        }
        return lhs - rhs;
    }

    // ==================== Option<Float> with float primitive ====================

    /**
     * Returns the minimum of a nullable Float and a primitive float.
     * If lhs is null, returns null.
     */
    public static Float maybeMinPrimitive(Float lhs, float rhs) {
        if (lhs == null) {
            return null;
        }
        return Math.min(lhs, rhs);
    }

    /**
     * Returns the maximum of a nullable Float and a primitive float.
     * If lhs is null, returns null.
     */
    public static Float maybeMaxPrimitive(Float lhs, float rhs) {
        if (lhs == null) {
            return null;
        }
        return Math.max(lhs, rhs);
    }

    /**
     * Adds a nullable Float and a primitive float.
     * If lhs is null, returns null.
     */
    public static Float maybeAddPrimitive(Float lhs, float rhs) {
        if (lhs == null) {
            return null;
        }
        return lhs + rhs;
    }

    /**
     * Subtracts a primitive float from a nullable Float.
     * If lhs is null, returns null.
     */
    public static Float maybeSubPrimitive(Float lhs, float rhs) {
        if (lhs == null) {
            return null;
        }
        return lhs - rhs;
    }

    // ==================== float primitive with Option<Float> ====================

    /**
     * Returns the minimum of a primitive float and a nullable Float.
     * If rhs is null, returns lhs.
     */
    public static float primitiveMin(float lhs, Float rhs) {
        if (rhs == null) {
            return lhs;
        }
        return Math.min(lhs, rhs);
    }

    /**
     * Returns the maximum of a primitive float and a nullable Float.
     * If rhs is null, returns lhs.
     */
    public static float primitiveMax(float lhs, Float rhs) {
        return TaffyMath.f32Max(lhs, rhs);
    }

    /**
     * Adds a primitive float and a nullable Float.
     * If rhs is null, returns lhs.
     */
    public static float primitiveAdd(float lhs, Float rhs) {
        if (rhs == null) {
            return lhs;
        }
        return lhs + rhs;
    }

    /**
     * Subtracts a nullable Float from a primitive float.
     * If rhs is null, returns lhs.
     */
    public static float primitiveSub(float lhs, Float rhs) {
        if (rhs == null) {
            return lhs;
        }
        return lhs - rhs;
    }
}
