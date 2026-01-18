package dev.vfyjxf.taffy.geometry;

import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.Dimension;
import dev.vfyjxf.taffy.style.FlexDirection;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A 2D size with width and height.
 *
 * @param <T> The type of the size values
 */
public final class Size<T> {
    public T width;
    public T height;

    public Size(T width, T height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Creates a new Size with both dimensions set to the same value
     */
    public static <T> Size<T> all(T value) {
        return new Size<>(value, value);
    }

    /**
     * Creates a new Size with specified width and height
     */
    public static <T> Size<T> of(T width, T height) {
        return new Size<>(width, height);
    }

    /**
     * Creates a zero Size for floats
     */
    public static Size<Float> zero() {
        return new Size<>(0f, 0f);
    }

    /**
     * Creates a Size with both dimensions set to NONE (null)
     */
    public static <T> Size<T> none() {
        return new Size<>(null, null);
    }

    /**
     * Creates a Size with auto values using the provided supplier
     */
    public static <T> Size<T> auto(Supplier<T> autoSupplier) {
        return new Size<>(autoSupplier.get(), autoSupplier.get());
    }

    public T getWidth() {
        return width;
    }

    public T getHeight() {
        return height;
    }

    /**
     * Creates a Size with both dimensions set to MAX_CONTENT
     */
    public static Size<AvailableSpace> maxContent() {
        return new Size<>(AvailableSpace.MAX_CONTENT, AvailableSpace.MAX_CONTENT);
    }

    /**
     * Creates a Size with both dimensions set to MIN_CONTENT
     */
    public static Size<AvailableSpace> minContent() {
        return new Size<>(AvailableSpace.MIN_CONTENT, AvailableSpace.MIN_CONTENT);
    }

    public static final Size<Float> ZERO = zero();

    /**
     * Map each dimension using the provided function
     */
    public <R> Size<R> map(Function<T, R> mapper) {
        return new Size<>(mapper.apply(width), mapper.apply(height));
    }

    /**
     * Zip two sizes together using the provided function
     */
    public <U, R> Size<R> zipWith(Size<U> other, BiFunction<T, U, R> mapper) {
        return new Size<>(mapper.apply(width, other.width), mapper.apply(height, other.height));
    }

    /**
     * Get either the width or height depending on the axis
     */
    public T get(AbsoluteAxis axis) {
        return axis == AbsoluteAxis.HORIZONTAL ? width : height;
    }

    /**
     * Set either the width or height depending on the axis
     */
    public void set(AbsoluteAxis axis, T value) {
        if (axis == AbsoluteAxis.HORIZONTAL) {
            width = value;
        } else {
            height = value;
        }
    }

    /**
     * Get the main axis dimension for a given flex direction
     */
    public T main(FlexDirection direction) {
        return direction.isRow() ? width : height;
    }

    /**
     * Get the cross axis dimension for a given flex direction
     */
    public T cross(FlexDirection direction) {
        return direction.isRow() ? height : width;
    }

    /**
     * Set the main axis dimension for a given flex direction
     */
    public void setMain(FlexDirection direction, T value) {
        if (direction.isRow()) {
            width = value;
        } else {
            height = value;
        }
    }

    /**
     * Set the cross axis dimension for a given flex direction
     */
    public void setCross(FlexDirection direction, T value) {
        if (direction.isRow()) {
            height = value;
        } else {
            width = value;
        }
    }

    /**
     * Create a Size from main and cross axis values
     */
    public static <T> Size<T> fromMainCross(FlexDirection direction, T main, T cross) {
        if (direction.isRow()) {
            return new Size<>(main, cross);
        } else {
            return new Size<>(cross, main);
        }
    }

    /**
     * Create a Size from cross axis value only (main axis is null)
     */
    public static <T> Size<T> fromCross(FlexDirection direction, T cross) {
        if (direction.isRow()) {
            return new Size<>(null, cross);
        } else {
            return new Size<>(cross, null);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Size<?> size = (Size<?>) o;
        return Objects.equals(width, size.width) && Objects.equals(height, size.height);
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }

    @Override
    public String toString() {
        return "Size{width=" + width + ", height=" + height + '}';
    }

    /**
     * Copy this Size
     */
    public Size<T> copy() {
        return new Size<>(width, height);
    }

    /**
     * Check if both dimensions are null
     */
    public boolean isNone() {
        return width == null && height == null;
    }

    /**
     * Check if both dimensions are not null
     */
    public boolean isBoth() {
        return width != null && height != null;
    }

    /**
     * Resolve a Size<Dimension> against a context FloatSize
     * Returns FloatSize where null means unresolvable
     */
    public FloatSize maybeResolve(FloatSize context) {
        @SuppressWarnings("unchecked")
        Size<Dimension> self = (Size<Dimension>) this;
        return new FloatSize(
            self.width.maybeResolve(context.width),
            self.height.maybeResolve(context.height)
        );
    }
}
