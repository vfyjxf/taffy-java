package dev.vfyjxf.taffy.geometry;

import dev.vfyjxf.taffy.style.Dimension;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.BiFunction;

/**
 * An axis-aligned rectangle with left, right, top, and bottom edges.
 * This can represent either a bounding box or padding/margin/border values.
 * @param <T> The type of the edge values
 */
public final class Rect<T> {
    public T left;
    public T right;
    public T top;
    public T bottom;

    public Rect(T left, T right, T top, T bottom) {
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    /**
     * Creates a new Rect with all edges set to the same value
     */
    public static <T> Rect<T> all(T value) {
        return new Rect<>(value, value, value, value);
    }

    /**
     * Creates a new Rect with left, top, right, bottom edges (CSS order)
     */
    public static <T> Rect<T> ltrb(T left, T top, T right, T bottom) {
        return new Rect<>(left, right, top, bottom);
    }

    /**
     * Creates a new Rect with horizontal (left/right) and vertical (top/bottom) values
     */
    public static <T> Rect<T> hv(T horizontal, T vertical) {
        return new Rect<>(horizontal, horizontal, vertical, vertical);
    }

    /**
     * Creates a zero Rect for floats
     */
    public static Rect<Float> zero() {
        return new Rect<>(0f, 0f, 0f, 0f);
    }

    /**
     * Creates a new Rect with left, right, top, bottom values
     */
    public static <T> Rect<T> of(T left, T right, T top, T bottom) {
        return new Rect<>(left, right, top, bottom);
    }

    /**
     * Creates a Rect with auto values using the provided supplier
     */
    public static <T> Rect<T> auto(Supplier<T> autoSupplier) {
        return new Rect<>(autoSupplier.get(), autoSupplier.get(), autoSupplier.get(), autoSupplier.get());
    }

    /**
     * Creates a Rect<Dimension> with all edges set to a fixed length
     */
    public static Rect<Dimension> fromLength(float value) {
        return new Rect<>(
            Dimension.length(value),
            Dimension.length(value),
            Dimension.length(value),
            Dimension.length(value)
        );
    }

    /**
     * Creates a Rect<Dimension> with all edges set to a percentage
     */
    public static Rect<Dimension> fromPercent(float value) {
        return new Rect<>(
            Dimension.percent(value),
            Dimension.percent(value),
            Dimension.percent(value),
            Dimension.percent(value)
        );
    }

    public T getLeft() { return left; }
    public T getRight() { return right; }
    public T getTop() { return top; }
    public T getBottom() { return bottom; }

    public static final Rect<Float> ZERO = zero();

    /**
     * Map each edge using the provided function
     */
    public <R> Rect<R> map(Function<T, R> mapper) {
        return new Rect<>(
            mapper.apply(left),
            mapper.apply(right),
            mapper.apply(top),
            mapper.apply(bottom)
        );
    }

    /**
     * Zip with a Size, applying mapper to left/right with width and top/bottom with height
     */
    public <U, R> Rect<R> zipSize(Size<U> size, BiFunction<T, U, R> mapper) {
        return new Rect<>(
            mapper.apply(left, size.width),
            mapper.apply(right, size.width),
            mapper.apply(top, size.height),
            mapper.apply(bottom, size.height)
        );
    }

    /**
     * Returns the sum of left and right (horizontal axis sum)
     */
    public float horizontalAxisSum() {
        return ((Number) left).floatValue() + ((Number) right).floatValue();
    }

    /**
     * Returns the sum of top and bottom (vertical axis sum)
     */
    public float verticalAxisSum() {
        return ((Number) top).floatValue() + ((Number) bottom).floatValue();
    }

    /**
     * Returns both axis sums as a Size
     */
    public Size<Float> sumAxes() {
        return new Size<>(horizontalAxisSum(), verticalAxisSum());
    }

    /**
     * Returns the sum of the main axis edges for the given flex direction
     */
    public float mainAxisSum(FlexDirection direction) {
        return direction.isRow() ? horizontalAxisSum() : verticalAxisSum();
    }

    /**
     * Returns the sum of the cross axis edges for the given flex direction
     */
    public float crossAxisSum(FlexDirection direction) {
        return direction.isRow() ? verticalAxisSum() : horizontalAxisSum();
    }

    /**
     * Get the main start edge (left for row, top for column)
     */
    public T mainStart(FlexDirection direction) {
        if (direction.isRow()) {
            return direction.isReverse() ? right : left;
        } else {
            return direction.isReverse() ? bottom : top;
        }
    }

    /**
     * Get the main end edge (right for row, bottom for column)
     */
    public T mainEnd(FlexDirection direction) {
        if (direction.isRow()) {
            return direction.isReverse() ? left : right;
        } else {
            return direction.isReverse() ? top : bottom;
        }
    }

    /**
     * Get the cross start edge (top for row, left for column)
     */
    public T crossStart(FlexDirection direction) {
        return direction.isRow() ? top : left;
    }

    /**
     * Get the cross end edge (bottom for row, right for column)
     */
    public T crossEnd(FlexDirection direction) {
        return direction.isRow() ? bottom : right;
    }

    /**
     * Get the sum of edges along a grid axis
     */
    public float gridAxisSum(AbsoluteAxis axis) {
        return axis == AbsoluteAxis.HORIZONTAL ? horizontalAxisSum() : verticalAxisSum();
    }

    /**
     * Returns a Line representing the left and right properties
     */
    public Line<T> horizontalComponents() {
        return new Line<>(left, right);
    }

    /**
     * Returns a Line representing the top and bottom properties
     */
    public Line<T> verticalComponents() {
        return new Line<>(top, bottom);
    }

    /**
     * Add two rects together (for float types)
     */
    public Rect<Float> add(Rect<Float> other) {
        @SuppressWarnings("unchecked")
        Rect<Float> self = (Rect<Float>) this;
        return new Rect<>(
            self.left + other.left,
            self.right + other.right,
            self.top + other.top,
            self.bottom + other.bottom
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rect<?> rect = (Rect<?>) o;
        return Objects.equals(left, rect.left) &&
               Objects.equals(right, rect.right) &&
               Objects.equals(top, rect.top) &&
               Objects.equals(bottom, rect.bottom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right, top, bottom);
    }

    @Override
    public String toString() {
        return "Rect{left=" + left + ", right=" + right + ", top=" + top + ", bottom=" + bottom + '}';
    }

    /**
     * Copy this Rect
     */
    public Rect<T> copy() {
        return new Rect<>(left, right, top, bottom);
    }

    /**
     * Resolve a Rect<Dimension> against a context Size<Float>
     * Returns Rect<Float> with resolved values (0 for unresolvable)
     */
    public Rect<Float> resolveOrZero(Size<Float> context) {
        @SuppressWarnings("unchecked")
        Rect<Dimension> self = (Rect<Dimension>) this;

        // NOTE: older API uses nullable Float; treat null as "None" => NaN
        float cw = context.width == null ? Float.NaN : context.width;
        float ch = context.height == null ? Float.NaN : context.height;
        return new Rect<>(
            self.left.resolveOrZero(cw),
            self.right.resolveOrZero(cw),
            self.top.resolveOrZero(ch),
            self.bottom.resolveOrZero(ch)
        );
    }

    /**
     * Resolve a Rect<Dimension> against a context FloatSize (NaN means unresolvable)
     * Returns Rect<Float> with resolved values (0 for unresolvable)
     */
    public Rect<Float> resolveOrZero(FloatSize context) {
        @SuppressWarnings("unchecked")
        Rect<Dimension> self = (Rect<Dimension>) this;
        return new Rect<>(
            self.left.resolveOrZero(context.width),
            self.right.resolveOrZero(context.width),
            self.top.resolveOrZero(context.height),
            self.bottom.resolveOrZero(context.height)
        );
    }

    /**
     * Resolve a Rect<Dimension> against a single optional context value (used for all edges)
     * Returns Rect<Float> with resolved values (0 for unresolvable)
     */
    public Rect<Float> resolveOrZero(float context) {
        @SuppressWarnings("unchecked")
        Rect<Dimension> self = (Rect<Dimension>) this;
        return new Rect<>(
            self.left.resolveOrZero(context),
            self.right.resolveOrZero(context),
            self.top.resolveOrZero(context),
            self.bottom.resolveOrZero(context)
        );
    }

    /**
     * Backwards-compatible overload accepting nullable Float.
     * Null is treated as "None" => NaN.
     */
    public Rect<Float> resolveOrZero(Float context) {
        return resolveOrZero(context == null ? Float.NaN : context.floatValue());
    }
}
