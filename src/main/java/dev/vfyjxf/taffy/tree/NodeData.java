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

    /** Monotonic version that increases when this node's layout changes */
    private long layoutVersion;

    /** Last layout version acknowledged by the user */
    private long acknowledgedLayoutVersion;
    
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
        this.layoutVersion = 0L;
        this.acknowledgedLayoutVersion = 0L;
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

    public boolean hasUnconsumedLayout() {
        return layoutVersion != acknowledgedLayoutVersion;
    }

    public void acknowledgeLayout() {
        this.acknowledgedLayoutVersion = this.layoutVersion;
    }

    void markLayoutUpdated(long newLayoutVersion) {
        this.layoutVersion = newLayoutVersion;
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
