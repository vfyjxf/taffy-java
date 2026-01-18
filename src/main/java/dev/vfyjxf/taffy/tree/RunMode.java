package dev.vfyjxf.taffy.tree;

/**
 * Whether we are performing a full layout, or we merely need to size the node.
 */
public enum RunMode {
    /** A full layout for this node and all children should be computed */
    PERFORM_LAYOUT,
    
    /** 
     * The layout algorithm should be executed such that an accurate container size for the node can be determined.
     * Layout steps that aren't necessary for determining the container size of the current node can be skipped.
     */
    COMPUTE_SIZE,
    
    /** This node should have a null layout set as it has been hidden (i.e. using Display::None) */
    PERFORM_HIDDEN_LAYOUT
}
