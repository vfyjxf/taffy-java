package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal data structure for storing node data.
 */
public class NodeData {
    
    /** The layout strategy used by this node */
    private TaffyStyle style;

    /**
     * Memoized resolved direction, valid only while {@link #resolvedDirectionEpoch} equals the
     * owning tree's current direction epoch.
     *
     * <p>{@code direction} is an inherited CSS property: a node styled {@code INHERIT} takes the
     * nearest ancestor's resolved direction (LTR when no ancestor states one). Resolving that
     * parent chain on every query is O(depth) id-to-style lookups, and layout algorithms ask for
     * every flex/grid/block child and every measured leaf, so the result is memoized here instead
     * of being recomputed. The tree bumps its direction epoch when a layout pass starts and after
     * every style or child-list change, and a stamp from any other epoch is ignored, so the cache
     * self-invalidates without a descendant walk and nothing is ever written back to the style.
     * Internal layout state only: not exposed through the public API.</p>
     */
    private TaffyDirection resolvedDirection = TaffyDirection.DEFAULT;

    /** The direction epoch {@link #resolvedDirection} was computed for ({@code -1} when never). */
    private long resolvedDirectionEpoch = -1L;

    /**
     * The children of this node, in layout order.
     *
     * <p>Child links live on the node itself instead of in a parallel id-to-list map so that a
     * pass which already holds the {@code NodeData} (layout computation, rounding) can read the
     * child list without a second hash lookup. The list is always allocated (empty for a leaf)
     * and is only mutated by {@link TaffyTree}.</p>
     */
    private final List<NodeId> children = new ArrayList<>();

    /**
     * Returns the mutable child list of this node. Package-private: child links are owned and
     * mutated exclusively by {@link TaffyTree}; layout passes read it without a map lookup.
     */
    List<NodeId> getChildren() {
        return children;
    }
    
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
     * The tree-wide layout generation at which {@link #hasNewLayout} was last acknowledged.
     *
     * <p>A layout request that reuses the stored layouts (the tree-clean fast path in
     * {@code TaffyTree}) writes nothing, so no node would flip its {@link #hasNewLayout} flag;
     * {@code TaffyTree.hasNewLayout(NodeId)} therefore also treats a node as having a new layout
     * while the tree's generation is newer than the generation this node was acknowledged at.
     * That keeps the Yoga-style "a recompute marks the tree" contract in O(1) instead of walking
     * the subtree to re-set the flags. Internal layout state only: not exposed through the public
     * API.</p>
     */
    private long acknowledgedLayoutGeneration;

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

    /**
     * Returns the memoized resolved direction.
     *
     * <p>The value is only meaningful while {@link #getResolvedDirectionEpoch()} equals the
     * owning tree's current direction epoch; {@code TaffyTree} checks that stamp before using it.
     * Package-private: the cache is layout-internal state, not part of the public API.</p>
     */
    TaffyDirection getResolvedDirection() {
        return resolvedDirection;
    }

    /**
     * Returns the direction epoch {@link #getResolvedDirection()} was computed for.
     */
    long getResolvedDirectionEpoch() {
        return resolvedDirectionEpoch;
    }

    /**
     * Memoizes a resolved direction for the given direction epoch.
     */
    void setResolvedDirection(TaffyDirection direction, long epoch) {
        this.resolvedDirection = direction;
        this.resolvedDirectionEpoch = epoch;
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
     * Returns the layout generation at which this node was last acknowledged.
     *
     * <p>Package-private: the generation is compared against the owning tree's current generation
     * by {@code TaffyTree.hasNewLayout(NodeId)}; it is not part of the public API.</p>
     */
    long getAcknowledgedLayoutGeneration() {
        return acknowledgedLayoutGeneration;
    }

    /**
     * Records that this node has been acknowledged for the given layout generation.
     *
     * <p>Package-private: see {@link #getAcknowledgedLayoutGeneration()}.</p>
     */
    void acknowledgeLayoutGeneration(long generation) {
        this.acknowledgedLayoutGeneration = generation;
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
