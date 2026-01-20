package dev.vfyjxf.taffy.geometry;

import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;

import java.util.Objects;

public class FloatSize {
    public float width;
    public float height;

    public FloatSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Transitional constructor: allow legacy nullable Float inputs.
     *
     * In the float-specialized geometry model, NaN represents "None".
     */
    public FloatSize(Float width, Float height) {
        this(width != null ? width : Float.NaN, height != null ? height : Float.NaN);
    }

    /**
     * Transitional constructor for mixed nullable/primitive inputs.
     */
    public FloatSize(Float width, float height) {
        this(width != null ? width : Float.NaN, height);
    }

    /**
     * Transitional constructor for mixed primitive/nullable inputs.
     */
    public FloatSize(float width, Float height) {
        this(width, height != null ? height : Float.NaN);
    }

    /**
     * Creates a new Size with both dimensions set to the same value
     */
    public static FloatSize all(float value) {
        return new FloatSize(value, value);
    }

    /**
     * Creates a new Size with specified width and height
     */
    public static FloatSize of(float width, float height) {
        return new FloatSize(width, height);
    }

    /**
     * Creates a zero Size for floats
     */
    public static FloatSize zero() {
        return new FloatSize(0f, 0f);
    }

    /**
     * Creates a Size with both dimensions set to NONE (null)
     */
    public static FloatSize none() {
        return new FloatSize(Float.NaN, Float.NaN);
    }

    /**
     * Creates a Size with auto values using the provided supplier
     */
    public static FloatSize auto(FloatSupplier autoSupplier) {
        return new FloatSize(autoSupplier.get(), autoSupplier.get());
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    /**
     * Creates a Size with both dimensions set to MAX_CONTENT
     */
    public static TaffySize<AvailableSpace> maxContent() {
        return new TaffySize<>(AvailableSpace.MAX_CONTENT, AvailableSpace.MAX_CONTENT);
    }

    /**
     * Creates a Size with both dimensions set to MIN_CONTENT
     */
    public static TaffySize<AvailableSpace> minContent() {
        return new TaffySize<>(AvailableSpace.MIN_CONTENT, AvailableSpace.MIN_CONTENT);
    }

    public static final FloatSize ZERO = zero();
    public static final FloatSize NONE = none();

    /**
     * Get either the width or height depending on the axis
     */
    public float get(AbsoluteAxis axis) {
        return axis == AbsoluteAxis.HORIZONTAL ? width : height;
    }

    /**
     * Set either the width or height depending on the axis
     */
    public void set(AbsoluteAxis axis, float value) {
        if (axis == AbsoluteAxis.HORIZONTAL) {
            width = value;
        } else {
            height = value;
        }
    }

    /**
     * Get the main axis dimension for a given flex direction
     */
    public float main(FlexDirection direction) {
        return direction.isRow() ? width : height;
    }

    /**
     * Get the cross axis dimension for a given flex direction
     */
    public float cross(FlexDirection direction) {
        return direction.isRow() ? height : width;
    }


    /**
     * Copy this Size
     */
    public FloatSize copy() {
        return new FloatSize(width, height);
    }

    public boolean isNone() {
        return Float.isNaN(width) && Float.isNaN(height);
    }

    public boolean isZero() {
        return width == 0f && height == 0f;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FloatSize floatSize = (FloatSize) o;
        return Float.compare(floatSize.width, width) == 0 && Float.compare(floatSize.height, height) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }

    @Override
    public String toString() {
        return "FloatSize{" +
               "width=" + width +
               ", height=" + height +
               '}';
    }
}
