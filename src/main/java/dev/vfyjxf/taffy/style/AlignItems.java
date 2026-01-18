package dev.vfyjxf.taffy.style;

/**
 * Used to control how child nodes are aligned.
 * For Flexbox it controls alignment in the cross axis.
 * For Grid it controls alignment in the block axis.
 * 
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/align-items">MDN - align-items</a>
 */
public enum AlignItems {
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
    STRETCH;
}
