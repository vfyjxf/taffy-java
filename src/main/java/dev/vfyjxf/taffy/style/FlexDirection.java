package dev.vfyjxf.taffy.style;

import dev.vfyjxf.taffy.geometry.AbsoluteAxis;

/**
 * The direction of the flexbox layout main axis.
 * 
 * There are always two perpendicular layout axes: main (or primary) and cross (or secondary).
 * Adding items will cause them to be positioned adjacent to each other along the main axis.
 * 
 * @see <a href="https://www.w3.org/TR/css-flexbox-1/#flex-direction-property">CSS Flexbox - flex-direction</a>
 */
public enum FlexDirection {
    /** Defines +x as the main axis. Items will be added from left to right in a row. */
    ROW,
    
    /** Defines +y as the main axis. Items will be added from top to bottom in a column. */
    COLUMN,
    
    /** Defines -x as the main axis. Items will be added from right to left in a row. */
    ROW_REVERSE,
    
    /** Defines -y as the main axis. Items will be added from bottom to top in a column. */
    COLUMN_REVERSE;

    /**
     * Is the direction Row or RowReverse?
     */
    public boolean isRow() {
        return this == ROW || this == ROW_REVERSE;
    }

    /**
     * Is the direction Column or ColumnReverse?
     */
    public boolean isColumn() {
        return this == COLUMN || this == COLUMN_REVERSE;
    }

    /**
     * Is the direction RowReverse or ColumnReverse?
     */
    public boolean isReverse() {
        return this == ROW_REVERSE || this == COLUMN_REVERSE;
    }

    /**
     * The AbsoluteAxis that corresponds to the main axis
     */
    public AbsoluteAxis mainAxis() {
        return isRow() ? AbsoluteAxis.HORIZONTAL : AbsoluteAxis.VERTICAL;
    }

    /**
     * The AbsoluteAxis that corresponds to the cross axis
     */
    public AbsoluteAxis crossAxis() {
        return isRow() ? AbsoluteAxis.VERTICAL : AbsoluteAxis.HORIZONTAL;
    }
}
