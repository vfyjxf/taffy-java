package dev.vfyjxf.taffy.style;

/**
 * Sets the layout used for the children of this node.
 */
public enum TaffyDisplay {
    /** The children will follow the block layout algorithm */
    BLOCK,
    
    /** The children will follow the flexbox layout algorithm */
    FLEX,
    
    /** The children will follow the CSS Grid layout algorithm */
    GRID,
    
    /** The node is hidden, and its children will also be hidden */
    NONE,

    /**
     * The children will follow the block layout algorithm, and the node establishes a new block
     * formatting context: its own margins do not collapse with its children's, and (once CSS float
     * layout is ported) it will contain its own floats and avoid external ones. This port currently
     * implements the margin-isolation part; float containment/avoidance is part of F1/P7.
     *
     * <p>Appended after {@link #NONE} for compatibility (upstream taffy #997, BK-17). Note that
     * {@link TaffyStyle#isBlock()} deliberately stays {@code false} for this value: upstream
     * documents that {@code CoreStyle::is_block} only answers {@code true} for
     * {@link #BLOCK}, because it means "this box participates in its parent's block formatting
     * context" — which a flow-root box does not.</p>
     */
    FLOW_ROOT;

    /** The default Display mode */
    public static final TaffyDisplay DEFAULT = FLEX;
}
