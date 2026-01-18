package dev.vfyjxf.taffy.style;

/**
 * Controls how items get placed into the grid for auto-placed items.
 */
public enum GridAutoFlow {
    /** Items are placed by filling each row in turn, adding new rows as necessary */
    ROW,
    
    /** Items are placed by filling each column in turn, adding new columns as necessary */
    COLUMN,
    
    /** Like ROW but also attempts to fill in holes earlier in the grid */
    ROW_DENSE,
    
    /** Like COLUMN but also attempts to fill in holes earlier in the grid */
    COLUMN_DENSE;

    /**
     * Returns true if the flow is row-based
     */
    public boolean isRow() {
        return this == ROW || this == ROW_DENSE;
    }

    /**
     * Returns true if the flow is column-based
     */
    public boolean isColumn() {
        return this == COLUMN || this == COLUMN_DENSE;
    }

    /**
     * Returns true if the flow uses dense packing
     */
    public boolean isDense() {
        return this == ROW_DENSE || this == COLUMN_DENSE;
    }
}
