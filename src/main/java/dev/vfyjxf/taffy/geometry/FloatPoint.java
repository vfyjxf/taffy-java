package dev.vfyjxf.taffy.geometry;

import dev.vfyjxf.taffy.style.FlexDirection;

import java.util.Objects;

public class FloatPoint {
    public float x;
    public float y;

    public FloatPoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Creates a new FloatPoint with both coordinates set to the same value
     */
    public static FloatPoint all(float value) {
        return new FloatPoint(value, value);
    }

    /**
     * Creates a zero FloatPoint for floats
     */
    public static FloatPoint zero() {
        return new FloatPoint(0f, 0f);
    }

    /**
     * Creates a FloatPoint with both coordinates set to NONE (null)
     */
    public static FloatPoint none() {
        return new FloatPoint(Float.NaN, Float.NaN);
    }

    public static final FloatPoint ZERO = zero();

    /**
     * Get either the x or y value depending on the axis
     */
    public float get(AbsoluteAxis axis) {
        return axis == AbsoluteAxis.HORIZONTAL ? x : y;
    }

    /**
     * Set either the x or y value depending on the axis
     */
    public void set(AbsoluteAxis axis, float value) {
        if (axis == AbsoluteAxis.HORIZONTAL) {
            x = value;
        } else {
            y = value;
        }
    }

    /**
     * Get the main axis value for a given flex direction
     */
    public float main(FlexDirection direction) {
        return direction.isRow() ? x : y;
    }

    /**
     * Get the cross axis value for a given flex direction
     */
    public float cross(FlexDirection direction) {
        return direction.isRow() ? y : x;
    }

    /**
     * Set the main axis value for a given flex direction
     */
    public void setMain(FlexDirection direction, float value) {
        if (direction.isRow()) {
            x = value;
        } else {
            y = value;
        }
    }

    /**
     * Set the cross axis value for a given flex direction
     */
    public void setCross(FlexDirection direction, float value) {
        if (direction.isRow()) {
            y = value;
        } else {
            x = value;
        }
    }

    /**
     * Transpose the x and y values
     */
    public FloatPoint transpose() {
        return new FloatPoint(y, x);
    }

    /**
     * Create a FloatPoint from main and cross axis values
     */
    public static FloatPoint fromMainCross(FlexDirection direction, float main, float cross) {
        if (direction.isRow()) {
            return new FloatPoint(main, cross);
        } else {
            return new FloatPoint(cross, main);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FloatPoint point = (FloatPoint) o;
        return Float.compare(point.x, x) == 0 && Float.compare(point.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "FloatPoint{x=" + x + ", y=" + y + '}';
    }

    /**
     * Copy this FloatPoint
     */
    public FloatPoint copy() {
        return new FloatPoint(x, y);
    }
}
