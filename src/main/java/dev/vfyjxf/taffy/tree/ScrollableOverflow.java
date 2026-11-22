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
 * <p><b>1.2 status (T1/BL-03, P4):</b> this is the additive upstream #1118/#1372 (SO-01) type. The
 * rectangle is carried as a {@link FloatRect} (defensively copied on the way in and out) plus
 * explicit {@code reversedX}/{@code reversedY} flags instead of the upstream packed sign-bit
 * encoding, so {@link #left()}/{@link #right()}/{@link #top()}/{@link #bottom()} already return
 * physical (unpacked) edges. Leaf, block, flex and grid layouts populate it, {@code RoundLayout}
 * rounds it, and {@code scrollWidth()}/{@code scrollHeight()} derive the reachable range from it
 * (reversed origins use {@code -left}/{@code -top}). The legacy {@link Layout#contentSize()}
 * formula survives only as the hand-built-layout compatibility path and is removed in 2.0.</p>
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
     * The shared value of a <em>computed</em>, empty scrollable-overflow rectangle.
     *
     * <p>This is not {@link #ZERO}: {@link Layout} treats {@code ZERO} as the 1.x "no overflow
     * data" placeholder and only {@code ZERO} is excluded by its identity check, so a real
     * algorithm result must be a different instance. Algorithms that produce the empty rectangle
     * on most nodes can return this interned value instead of allocating a fresh record, a
     * rectangle copy and a client-size pair per node. Instances are immutable value objects (the
     * constructor and {@link #rect()} defensively copy), so sharing it is invisible except for
     * identity, which never distinguishes computed rectangles from each other.</p>
     */
    static final ScrollableOverflow EMPTY_COMPUTED = new ScrollableOverflow(FloatRect.zero(), false, false);

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
     * Copies the internal rectangle into a caller-owned scratch rectangle.
     *
     * <p>Layout algorithms that read the overflow of every child allocate a defensive copy through
     * {@link #rect()} just to read four floats, and throw it away immediately. This package-private
     * back door lets them copy into one scratch rectangle per container instead. It is not part of
     * the public API: the caller must not let {@code target} escape and must treat it as
     * overwritten.</p>
     *
     * @param target the caller-owned rectangle to overwrite
     */
    void copyRectInto(FloatRect target) {
        target.left = rect.left;
        target.right = rect.right;
        target.top = rect.top;
        target.bottom = rect.bottom;
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
