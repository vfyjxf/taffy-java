package dev.vfyjxf.taffy.style;

/**
 * Sets the layout used for the children of this node.
 */
public enum Display {
    /** The children will follow the block layout algorithm */
    BLOCK,
    
    /** The children will follow the flexbox layout algorithm */
    FLEX,
    
    /** The children will follow the CSS Grid layout algorithm */
    GRID,
    
    /** The node is hidden, and its children will also be hidden */
    NONE;

    /** The default Display mode */
    public static final Display DEFAULT = FLEX;
}
