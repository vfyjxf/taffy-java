package dev.vfyjxf.taffy.geometry;

/**
 * The CSS abstract axis
 * @see <a href="https://www.w3.org/TR/css-writing-modes-3/#abstract-axes">CSS Writing Modes - Abstract axes</a>
 */
public enum AbstractAxis {
    /**
     * The axis in the inline dimension, i.e. the horizontal axis in horizontal writing modes
     * and the vertical axis in vertical writing modes.
     */
    INLINE,
    
    /**
     * The axis in the block dimension, i.e. the vertical axis in horizontal writing modes
     * and the horizontal axis in vertical writing modes.
     */
    BLOCK;

    /**
     * Returns the other variant of the enum
     */
    public AbstractAxis other() {
        return this == INLINE ? BLOCK : INLINE;
    }

    /**
     * Convert an AbstractAxis into an AbsoluteAxis naively assuming that the Inline axis is Horizontal.
     * This is currently always true, but will change if Taffy ever implements the writing_mode property.
     */
    public AbsoluteAxis asAbsNaive() {
        return this == INLINE ? AbsoluteAxis.HORIZONTAL : AbsoluteAxis.VERTICAL;
    }
}
