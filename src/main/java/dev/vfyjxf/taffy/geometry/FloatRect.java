package dev.vfyjxf.taffy.geometry;

import dev.vfyjxf.taffy.style.Dimension;
import dev.vfyjxf.taffy.style.FlexDirection;

import java.util.Objects;

public class FloatRect {
    public float left;
    public float right;
    public float top;
    public float bottom;

    public FloatRect(float left, float right, float top, float bottom) {
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    /**
     * Creates a new Rect with all edges set to the same value
     */
    public static FloatRect all(float value) {
        return new FloatRect(value, value, value, value);
    }

    /**
     * Creates a new Rect with left, top, right, bottom edges (CSS order)
     */
    public static FloatRect ltrb(float left, float top, float right, float bottom) {
        return new FloatRect(left, right, top, bottom);
    }

    /**
     * Creates a new Rect with horizontal (left/right) and vertical (top/bottom) values
     */
    public static FloatRect hv(float horizontal, float vertical) {
        return new FloatRect(horizontal, horizontal, vertical, vertical);
    }

    /**
     * Creates a zero Rect for floats
     */
    public static FloatRect zero() {
        return new FloatRect(0f, 0f, 0f, 0f);
    }

    /**
     * Creates a new Rect with left, right, top, bottom values
     */
    public static FloatRect of(float left, float right, float top, float bottom) {
        return new FloatRect(left, right, top, bottom);
    }

    /**
     * Creates a Rect with auto values using the provided supplier
     */
    public static FloatRect auto(FloatSupplier autoSupplier) {
        return new FloatRect(autoSupplier.get(), autoSupplier.get(), autoSupplier.get(), autoSupplier.get());
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

    public float getLeft() {return left;}

    public float getRight() {return right;}

    public float getTop() {return top;}

    public float getBottom() {return bottom;}

    public static final FloatRect ZERO = zero();


    /**
     * Returns the sum of left and right (horizontal axis sum)
     */
    public float horizontalAxisSum() {
        return left + right;
    }

    /**
     * Returns the sum of top and bottom (vertical axis sum)
     */
    public float verticalAxisSum() {
        return top + bottom;
    }

    /**
     * Returns both axis sums as a Size
     */
    public FloatSize sumAxes() {
        return new FloatSize(horizontalAxisSum(), verticalAxisSum());
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
    public float mainStart(FlexDirection direction) {
        if (direction.isRow()) {
            return direction.isReverse() ? right : left;
        } else {
            return direction.isReverse() ? bottom : top;
        }
    }

    /**
     * Get the main end edge (right for row, bottom for column)
     */
    public float mainEnd(FlexDirection direction) {
        if (direction.isRow()) {
            return direction.isReverse() ? left : right;
        } else {
            return direction.isReverse() ? top : bottom;
        }
    }

    /**
     * Get the cross start edge (top for row, left for column)
     */
    public float crossStart(FlexDirection direction) {
        return direction.isRow() ? top : left;
    }

    /**
     * Get the cross end edge (bottom for row, right for column)
     */
    public float crossEnd(FlexDirection direction) {
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
    public FloatLine horizontalComponents() {
        return new FloatLine(left, right);
    }

    /**
     * Returns a Line representing the top and bottom properties
     */
    public FloatLine verticalComponents() {
        return new FloatLine(top, bottom);
    }

    /**
     * Add two rects together (for float types)
     */
    public FloatRect add(FloatRect other) {
        return new FloatRect(
            this.left + other.left,
            this.right + other.right,
            this.top + other.top,
            this.bottom + other.bottom
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FloatRect rect = (FloatRect) o;
        return Float.compare(rect.left, left) == 0 &&
               Float.compare(rect.right, right) == 0 &&
               Float.compare(rect.top, top) == 0 &&
               Float.compare(rect.bottom, bottom) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right, top, bottom);
    }

    @Override
    public String toString() {
        return "FloatRect{left=" + left + ", right=" + right + ", top=" + top + ", bottom=" + bottom + '}';
    }

    /**
     * Copy this Rect
     */
    public FloatRect copy() {
        return new FloatRect(left, right, top, bottom);
    }
}
