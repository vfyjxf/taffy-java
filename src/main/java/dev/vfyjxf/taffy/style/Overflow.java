package dev.vfyjxf.taffy.style;

/**
 * How children overflowing their container should affect layout.
 * 
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/overflow">MDN - overflow</a>
 */
public enum Overflow {
    /**
     * The automatic minimum size of this node as a flexbox/grid item should be based on the size of its content.
     * Content that overflows this node should contribute to the scroll region of its parent.
     */
    VISIBLE,
    
    /**
     * The automatic minimum size of this node as a flexbox/grid item should be based on the size of its content.
     * Content that overflows this node should not contribute to the scroll region of its parent.
     */
    CLIP,
    
    /**
     * The automatic minimum size of this node as a flexbox/grid item should be 0.
     * Content that overflows this node should not contribute to the scroll region of its parent.
     */
    HIDDEN,
    
    /**
     * The automatic minimum size of this node as a flexbox/grid item should be 0.
     * Additionally, space should be reserved for a scrollbar.
     * Content that overflows this node should not contribute to the scroll region of its parent.
     */
    SCROLL;

    /**
     * Returns true for overflow modes that contain their contents (Hidden, Scroll)
     * or false for overflow modes that allow their contents to spill (Visible, Clip).
     */
    public boolean isScrollContainer() {
        return this == HIDDEN || this == SCROLL;
    }

    /**
     * Returns 0.0 if the overflow mode would cause the automatic minimum size of a Flexbox or CSS Grid item
     * to be 0. Otherwise returns null.
     */
    public Float maybeIntoAutomaticMinSize() {
        return isScrollContainer() ? 0.0f : null;
    }
}
