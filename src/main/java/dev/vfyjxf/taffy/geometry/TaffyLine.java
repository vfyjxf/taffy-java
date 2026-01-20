package dev.vfyjxf.taffy.geometry;

import java.util.Objects;
import java.util.function.Function;

/**
 * A line with start and end values, used for representing spans or pairs of values.
 * @param <T> The type of the line values
 */
public final class TaffyLine<T> {
    public T start;
    public T end;

    public TaffyLine(T start, T end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Creates a Line with both values set to the same value
     */
    public static <T> TaffyLine<T> all(T value) {
        return new TaffyLine<>(value, value);
    }

    /**
     * Line with both values set to false
     */
    public static final TaffyLine<Boolean> FALSE = new TaffyLine<>(false, false);

    /**
     * Line with both values set to true
     */
    public static final TaffyLine<Boolean> TRUE = new TaffyLine<>(true, true);

    /**
     * Map each value using the provided function
     */
    public <R> TaffyLine<R> map(Function<T, R> mapper) {
        return new TaffyLine<>(mapper.apply(start), mapper.apply(end));
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
        TaffyLine<?> line = (TaffyLine<?>) o;
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
    public TaffyLine<T> copy() {
        return new TaffyLine<>(start, end);
    }
}
