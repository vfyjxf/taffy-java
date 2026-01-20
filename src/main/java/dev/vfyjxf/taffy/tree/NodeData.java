package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.style.TaffyStyle;

/**
 * Internal data structure for storing node data.
 */
public class NodeData {
    
    /** The layout strategy used by this node */
    private TaffyStyle style;
    
    /** The always unrounded results of the layout computation */
    private Layout unroundedLayout;
    
    /** The final results of the layout computation (may be rounded) */
    private Layout finalLayout;
    
    /** Whether the node has context data (measure function) */
    private boolean hasContext;

    /** 
     * Whether this node has a new layout that hasn't been acknowledged.
     * Similar to Yoga's hasNewLayout - set after layout computation, cleared by user.
     */
    private boolean hasNewLayout;

    /**
     * Whether any descendant of this node has a new layout.
     * This allows walking the tree from root and early-returning when no dirty descendants exist.
     */
    private boolean hasDirtyDescendant;
    
    /** The cached results of layout computation */
    private final LayoutCache cache;
    
    /**
     * Creates new NodeData with the given style.
     */
    public NodeData(TaffyStyle style) {
        this.style = style != null ? style : new TaffyStyle();
        this.unroundedLayout = new Layout();
        this.finalLayout = new Layout();
        this.hasContext = false;
        this.cache = new LayoutCache();
        this.hasNewLayout = false;
        this.hasDirtyDescendant = false;
    }
    
    public TaffyStyle getStyle() {
        return style;
    }
    
    public void setStyle(TaffyStyle style) {
        this.style = style;
    }
    
    public Layout getUnroundedLayout() {
        return unroundedLayout;
    }
    
    public void setUnroundedLayout(Layout layout) {
        this.unroundedLayout = layout;
    }
    
    public Layout getFinalLayout() {
        return finalLayout;
    }
    
    public void setFinalLayout(Layout layout) {
        this.finalLayout = layout;
    }
    
    public boolean hasContext() {
        return hasContext;
    }
    
    public void setHasContext(boolean hasContext) {
        this.hasContext = hasContext;
    }
    
    public LayoutCache getCache() {
        return cache;
    }

    /**
     * Returns true if this node has a new layout that hasn't been acknowledged.
     */
    public boolean hasNewLayout() {
        return hasNewLayout;
    }

    /**
     * Returns true if any descendant of this node has a new layout.
     */
    public boolean hasDirtyDescendant() {
        return hasDirtyDescendant;
    }

    /**
     * Returns true if this node or any descendant has a new layout.
     * Convenience method for walking the tree.
     */
    public boolean needsVisit() {
        return hasNewLayout || hasDirtyDescendant;
    }

    /**
     * Marks that this node has a new layout.
     */
    void markNewLayout() {
        this.hasNewLayout = true;
    }

    /**
     * Marks that a descendant has a new layout.
     * @return true if the flag was already set (no need to propagate further)
     */
    boolean markDirtyDescendant() {
        if (hasDirtyDescendant) {
            return true; // already set, no need to propagate
        }
        hasDirtyDescendant = true;
        return false;
    }

    /**
     * Clears the new layout flag for this node.
     */
    public void acknowledgeLayout() {
        this.hasNewLayout = false;
    }

    /**
     * Clears the dirty descendant flag for this node.
     * Should be called when all descendants have been acknowledged.
     */
    public void clearDirtyDescendant() {
        this.hasDirtyDescendant = false;
    }
    
    /**
     * Marks the node as dirty (needing layout recalculation).
     * 
     * @return true if the cache was already empty (node was already dirty)
     */
    public boolean markDirty() {
        return cache.clear();
    }
}
