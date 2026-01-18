package dev.vfyjxf.taffy.geometry;

import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.BiFunction;

/**
 * A 2D point with x and y coordinates.
 * @param <T> The type of the coordinate values
 */
public final class Point<T> {
    public T x;
    public T y;

    public Point(T x, T y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Creates a new Point with both coordinates set to the same value
     */
    public static <T> Point<T> all(T value) {
        return new Point<>(value, value);
    }

    /**
     * Creates a zero Point for floats
     */
    public static Point<Float> zero() {
        return new Point<>(0f, 0f);
    }

    /**
     * Creates a Point with both coordinates set to NONE (null)
     */
    public static Point<Float> none() {
        return new Point<>(null, null);
    }

    public static final Point<Float> ZERO = zero();

    /**
     * Map each coordinate using the provided function
     */
    public <R> Point<R> map(Function<T, R> mapper) {
        return new Point<>(mapper.apply(x), mapper.apply(y));
    }

    /**
     * Zip two points together using the provided function
     */
    public <U, R> Point<R> zipWith(Point<U> other, BiFunction<T, U, R> mapper) {
        return new Point<>(mapper.apply(x, other.x), mapper.apply(y, other.y));
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
    public Point<T> transpose() {
        return new Point<>(y, x);
    }

    /**
     * Create a Point from main and cross axis values
     */
    public static <T> Point<T> fromMainCross(FlexDirection direction, T main, T cross) {
        if (direction.isRow()) {
            return new Point<>(main, cross);
        } else {
            return new Point<>(cross, main);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point<?> point = (Point<?>) o;
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
    public Point<T> copy() {
        return new Point<>(x, y);
    }
}
