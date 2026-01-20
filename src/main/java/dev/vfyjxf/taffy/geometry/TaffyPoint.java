package dev.vfyjxf.taffy.geometry;

import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.BiFunction;

/**
 * A 2D point with x and y coordinates.
 * @param <T> The type of the coordinate values
 */
public final class TaffyPoint<T> {
    public T x;
    public T y;

    public TaffyPoint(T x, T y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Creates a new Point with both coordinates set to the same value
     */
    public static <T> TaffyPoint<T> all(T value) {
        return new TaffyPoint<>(value, value);
    }

    /**
     * Creates a zero Point for floats
     */
    public static TaffyPoint<Float> zero() {
        return new TaffyPoint<>(0f, 0f);
    }

    /**
     * Creates a Point with both coordinates set to NONE (null)
     */
    public static TaffyPoint<Float> none() {
        return new TaffyPoint<>(null, null);
    }

    public static final TaffyPoint<Float> ZERO = zero();

    /**
     * Map each coordinate using the provided function
     */
    public <R> TaffyPoint<R> map(Function<T, R> mapper) {
        return new TaffyPoint<>(mapper.apply(x), mapper.apply(y));
    }

    /**
     * Zip two points together using the provided function
     */
    public <U, R> TaffyPoint<R> zipWith(TaffyPoint<U> other, BiFunction<T, U, R> mapper) {
        return new TaffyPoint<>(mapper.apply(x, other.x), mapper.apply(y, other.y));
    }

    /**
     * Get either the x or y value depending on the axis
     */
    public T get(AbsoluteAxis axis) {
        return axis == AbsoluteAxis.HORIZONTAL ? x : y;
    }

    /**
     * Set either the x or y value depending on the axis
     */
    public void set(AbsoluteAxis axis, T value) {
        if (axis == AbsoluteAxis.HORIZONTAL) {
            x = value;
        } else {
            y = value;
        }
    }

    /**
     * Get the main axis value for a given flex direction
     */
    public T main(FlexDirection direction) {
        return direction.isRow() ? x : y;
    }

    /**
     * Get the cross axis value for a given flex direction
     */
    public T cross(FlexDirection direction) {
        return direction.isRow() ? y : x;
    }

    /**
     * Set the main axis value for a given flex direction
     */
    public void setMain(FlexDirection direction, T value) {
        if (direction.isRow()) {
            x = value;
        } else {
            y = value;
        }
    }

    /**
     * Set the cross axis value for a given flex direction
     */
    public void setCross(FlexDirection direction, T value) {
        if (direction.isRow()) {
            y = value;
        } else {
            x = value;
        }
    }

    /**
     * Transpose the x and y values
     */
    public TaffyPoint<T> transpose() {
        return new TaffyPoint<>(y, x);
    }

    /**
     * Create a Point from main and cross axis values
     */
    public static <T> TaffyPoint<T> fromMainCross(FlexDirection direction, T main, T cross) {
        if (direction.isRow()) {
            return new TaffyPoint<>(main, cross);
        } else {
            return new TaffyPoint<>(cross, main);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaffyPoint<?> point = (TaffyPoint<?>) o;
        return Objects.equals(x, point.x) && Objects.equals(y, point.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Point{x=" + x + ", y=" + y + '}';
    }

    /**
     * Copy this Point
     */
    public TaffyPoint<T> copy() {
        return new TaffyPoint<>(x, y);
    }
}
