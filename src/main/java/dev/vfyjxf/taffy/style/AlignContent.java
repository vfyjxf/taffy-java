package dev.vfyjxf.taffy.style;

/**
 * Sets the distribution of space between and around content items.
 * For Flexbox it controls alignment in the cross axis.
 * For Grid it controls alignment in the block axis.
 * 
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/align-content">MDN - align-content</a>
 */
public enum AlignContent {
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
    
    /** Items are centered around the middle of the axis */
    CENTER,
    
    /** Items are stretched to fill the container */
    STRETCH,
    
    /**
     * The first and last items are aligned flush with the edges of the container (no gap).
     * The gap between items is distributed evenly.
     */
    SPACE_BETWEEN,
    
    /**
     * The gap between the first and last items is exactly THE SAME as the gap between items.
     * The gaps are distributed evenly.
     */
    SPACE_EVENLY,
    
    /**
     * The gap between the first and last items is exactly HALF the gap between items.
     * The gaps are distributed evenly in proportion to these ratios.
     */
    SPACE_AROUND;
}
