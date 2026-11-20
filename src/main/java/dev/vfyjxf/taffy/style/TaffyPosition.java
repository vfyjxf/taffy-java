package dev.vfyjxf.taffy.style;

/**
 * The positioning strategy for this item.
 *
 * <p>This controls both how the origin is determined for the position/inset properties,
 * and whether or not the item will be controlled by flexbox's layout algorithm.</p>
 *
 * <p>The legacy {@link #RELATIVE} / {@link #ABSOLUTE} constants keep their historic ordinals
 * ({@code 0} / {@code 1}); the CSS keywords added for upstream parity (#1140) are appended after
 * them so that old serialized ordinals and switch tables stay valid. The Java default remains
 * {@link #RELATIVE} in the 1.2 line (decision D1); a later major version flips the default to
 * {@link #STATIC} to match CSS.</p>
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/position">MDN - position</a>
 */
public enum TaffyPosition {
    /**
     * The offset is computed relative to the final position given by the layout algorithm.
     * Offsets do not affect the position of any other items; they are effectively a correction
     * factor applied at the end.
     */
    RELATIVE,

    /**
     * The item is taken out of normal flow and positioned relative to its closest positioned
     * ancestor, if any. Otherwise, it is placed relative to the origin.
     * No space is created for the item in the page layout, and its size will not be altered.
     */
    ABSOLUTE,

    /**
     * The item is laid out in normal flow. {@code inset} has no effect on statically positioned
     * items, and they do not act as a containing block for absolutely positioned descendants.
     * <p>
     * This is the CSS default and the upstream 2.0 default. It is appended for source parity
     * without changing the Java 1.2 default, which stays {@link #RELATIVE} (decision D1).
     */
    STATIC,

    /**
     * The item is taken out of normal flow and positioned relative to the root (viewport),
     * unless an ancestor overrides containing-block behavior.
     * <p>
     * The 1.2 port has no full out-of-flow hoisting yet (OF-01). A {@code FIXED} root and a
     * {@code FIXED} direct child of a non-positioned root are positioned against the viewport
     * (OF-07); deeper {@code FIXED} boxes still use their parent's positioning area until hoisting
     * lands.
     */
    FIXED,

    /**
     * The item is laid out in normal flow exactly like a statically positioned item: {@code inset}
     * is <em>not</em> applied as an offset. In CSS, sticky insets are scroll thresholds which a
     * layout library has no knowledge of; applying the sticky offset is the responsibility of the
     * caller once it knows the scroll position.
     */
    STICKY;

    /**
     * Returns whether this position takes the item out of normal flow.
     *
     * <p>{@link #ABSOLUTE} and {@link #FIXED} are out of flow; {@link #RELATIVE}, {@link #STATIC}
     * and {@link #STICKY} participate in their parent's flow.</p>
     */
    public boolean isOutOfFlow() {
        return this == ABSOLUTE || this == FIXED;
    }
}
