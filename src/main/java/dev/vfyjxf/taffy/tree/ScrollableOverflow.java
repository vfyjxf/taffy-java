package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatRect;

/**
 * The scrollable overflow rectangle of a node, together with the physical directions in which
 * scrolling starts at the end edge.
 *
 * <p>The rectangle is expressed in physical x/y coordinates relative to the scroll origin (the
 * top-left edge of the client area). To obtain border-box coordinates, add
 * {@code border.left}/{@code border.top}. The rectangle contains the origin: {@code left}/{@code top}
 * are non-positive and {@code right}/{@code bottom} are non-negative. A normal axis has range
 * {@code 0..scrollWidth} (or {@code 0..scrollHeight}); a reversed axis has range
 * {@code -scrollWidth..0} (or {@code -scrollHeight..0}).</p>
 *
 * <p><b>1.2 status (T1/BL-03, minimal additive type):</b> this is the smallest useful skeleton of
 * upstream #1118/#1372 (SO-01). The rectangle is carried as a {@link FloatRect} (defensively
 * copied on the way in and out) plus explicit {@code reversedX}/{@code reversedY} flags instead of
 * the upstream packed sign-bit
 * encoding, so {@link #left()}/{@link #right()}/{@link #top()}/{@link #bottom()} already return
 * physical (unpacked) edges. Per-side accessors, {@link #transpose()} and {@link #round()} (which
 * preserves {@code -0.0}) are provided for the future algorithm wiring, but <b>nothing in block,
 * flex or grid produces or consumes this value in 1.2</b>: {@link Layout#scrollableOverflow()}
 * defaults to {@link #ZERO}, and {@code scrollWidth()}/{@code scrollHeight()} still use the legacy
 * {@link Layout#contentSize()} formula. P4 adds the packed/serde semantics, the scroll-origin
 * clipping rules and the algorithm integration; 2.0 removes {@code contentSize}.</p>
 *
 * <p>Instances are defensive value objects: the constructor copies the supplied rectangle and
 * {@link #rect()} returns a copy, so mutating the returned {@link FloatRect} can never change this
 * value, another overflow, or the shared {@link #ZERO} constant. The per-side accessors and
 * {@link #transpose()}/{@link #round()}/{@link #copy()} operate on the internal copy.</p>
 *
 * @param rect      the physical overflow rectangle, containing the origin; copied by the constructor
 * @param reversedX whether the physical x scroll origin is at the right edge
 * @param reversedY whether the physical y scroll origin is at the bottom edge
 */
public record ScrollableOverflow(FloatRect rect, boolean reversedX, boolean reversedY) {

    /**
     * Empty overflow with normal scroll origins.
     *
     * <p>Its rectangle is a private copy, and {@link #rect()} hands out further copies, so this
     * constant cannot be mutated through the public API.</p>
     */
    public static final ScrollableOverflow ZERO = new ScrollableOverflow(FloatRect.zero(), false, false);

    /**
     * Creates a defensive {@code ScrollableOverflow}.
     *
     * @param rect      the physical overflow rectangle; copied, {@code null} becomes a zero rectangle
     * @param reversedX whether the physical x scroll origin is at the right edge
     * @param reversedY whether the physical y scroll origin is at the bottom edge
     */
    public ScrollableOverflow {
        rect = rect == null ? FloatRect.zero() : rect.copy();
    }

    /**
     * The physical overflow rectangle, as a defensive copy.
     *
     * <p>The returned {@link FloatRect} is owned by the caller: mutating it does not change this
     * value or any shared constant.</p>
     *
     * @return a copy of the internal rectangle
     */
    @Override
    public FloatRect rect() {
        return rect.copy();
    }

    /**
     * Creates an empty overflow with normal scroll origins.
     *
     * @return {@link #ZERO}
     */
    public static ScrollableOverflow zero() {
        return ZERO;
    }

    /**
     * The physical left overflow edge (non-positive).
     *
     * @return {@code rect.left}
     */
    public float left() {
        return rect.left;
    }

    /**
     * The physical right overflow edge (non-negative).
     *
     * @return {@code rect.right}
     */
    public float right() {
        return rect.right;
    }

    /**
     * The physical top overflow edge (non-positive).
     *
     * @return {@code rect.top}
     */
    public float top() {
        return rect.top;
    }

    /**
     * The physical bottom overflow edge (non-negative).
     *
     * @return {@code rect.bottom}
     */
    public float bottom() {
        return rect.bottom;
    }

    /**
     * Swaps the physical axes, preserving the reversed-origin directions.
     *
     * @return a new overflow whose left/top come from this top/left and whose flags are swapped
     */
    public ScrollableOverflow transpose() {
        return new ScrollableOverflow(
            new FloatRect(rect.top, rect.bottom, rect.left, rect.right),
            reversedY,
            reversedX
        );
    }

    /**
     * Rounds every edge to the nearest whole pixel, with ties rounding away from zero (matching
     * Rust's {@code f32::round}), while preserving the sign of {@code -0.0}.
     *
     * @return a new overflow with rounded edges and the same reversed-origin flags
     */
    public ScrollableOverflow round() {
        return new ScrollableOverflow(
            new FloatRect(round(rect.left), round(rect.right), round(rect.top), round(rect.bottom)),
            reversedX,
            reversedY
        );
    }

    private static float round(float value) {
        // Return signed zero unchanged so that -0.0 survives rounding (upstream #1372 does the same).
        if (value == 0.0f) {
            return value;
        }
        return value >= 0.0f ? (float) Math.floor(value + 0.5) : (float) Math.ceil(value - 0.5);
    }

    /**
     * Deep-copies the overflow rectangle.
     *
     * @return a new overflow with a copied rectangle and the same flags
     */
    public ScrollableOverflow copy() {
        // The constructor performs the defensive copy; pass the internal rectangle to avoid two copies.
        return new ScrollableOverflow(rect, reversedX, reversedY);
    }

    @Override
    public String toString() {
        return "ScrollableOverflow{rect=" + rect + ", reversedX=" + reversedX + ", reversedY=" + reversedY + '}';
    }
}
