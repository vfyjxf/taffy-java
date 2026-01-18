package dev.vfyjxf.taffy.geometry;

import java.util.Objects;
import java.util.function.Function;

/**
 * A line with start and end values, used for representing spans or pairs of values.
 * @param <T> The type of the line values
 */
public final class Line<T> {
    public T start;
    public T end;

    public Line(T start, T end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Creates a Line with both values set to the same value
     */
    public static <T> Line<T> all(T value) {
        return new Line<>(value, value);
    }

    /**
     * Line with both values set to false
     */
    public static final Line<Boolean> FALSE = new Line<>(false, false);

    /**
     * Line with both values set to true
     */
    public static final Line<Boolean> TRUE = new Line<>(true, true);

    /**
     * Map each value using the provided function
     */
    public <R> Line<R> map(Function<T, R> mapper) {
        return new Line<>(mapper.apply(start), mapper.apply(end));
    }

    /**
     * Sum of start and end (for numeric types)
     */
    public float sum() {
        return ((Number) start).floatValue() + ((Number) end).floatValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Line<?> line = (Line<?>) o;
        return Objects.equals(start, line.start) && Objects.equals(end, line.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return "Line{start=" + start + ", end=" + end + '}';
    }

    /**
     * Copy this Line
     */
    public Line<T> copy() {
        return new Line<>(start, end);
    }
}
