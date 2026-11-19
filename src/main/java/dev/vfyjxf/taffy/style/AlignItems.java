package dev.vfyjxf.taffy.style;

/**
 * Used to control how child nodes are aligned.
 * For Flexbox it controls alignment in the cross axis.
 * For Grid it controls alignment in the block axis.
 * 
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/align-items">MDN - align-items</a>
 */
public enum AlignItems {
    /**
     * Unspecified/auto value used as a non-null sentinel.
     * <p>
     * The effective behavior depends on the algorithm context (e.g. defaulting to STRETCH).
     */
    AUTO,

    /** Items are packed toward the start of the axis */
    START,
    
    /** Items are packed toward the end of the axis */
    END,
    
    /**
     * Items are packed towards the flex-relative start of the axis.
     * For flex containers with flex_direction RowReverse or ColumnReverse this is equivalent to End.
     * In all other cases it is equivalent to Start.
     */
    FLEX_START,
    
    /**
     * Items are packed towards the flex-relative end of the axis.
     * For flex containers with flex_direction RowReverse or ColumnReverse this is equivalent to Start.
     * In all other cases it is equivalent to End.
     */
    FLEX_END,
    
    /** Items are packed along the center of the cross axis */
    CENTER,
    
    /** Items are aligned such as their baselines align */
    BASELINE,
    
    /** Stretch to fill the container */
    STRETCH,

    /**
     * The layout mode's default alignment (upstream #1254): {@code stretch} for flex items and
     * for grid items without a definite size, {@code start} where stretching does not apply.
     * <p>
     * Unlike {@link #AUTO}, which is the legacy "not specified" sentinel, this is an explicit
     * keyword that can override an inherited alignment. Appended after the existing constants so
     * their ordinals stay stable (1.2 compatibility policy S2/Q2).
     */
    NORMAL;
}
