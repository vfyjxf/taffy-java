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
     * Version of this node's layout-affecting inputs, as observed by a parent's cached block or
     * flex item list.
     *
     * <p>Bumped by {@link #setStyle} and by a <em>direct</em> {@link #markDirty()} call (which is
     * also how the measure-callback and node-context setters invalidate a node, and how an in-place
     * {@code getStyle()} mutation is announced). A parent's {@code BlockComputer} item cache
     * compares this stamp for each of its children, so a changed child invalidates its direct
     * parent's list without requiring the change to be propagated to every ancestor. Dirty-flag
     * propagation through {@link #markDirtyAncestor()} deliberately does not bump it.</p>
     */
    private long contentVersion;

    /**
     * Version of this node's child list. Bumped whenever {@link TaffyTree} adds, inserts, removes
     * or replaces a child, so a cached block or flex item list can reject a stale child order
     * without comparing ids.
     */
    private long childrenVersion;

    /**
     * Cross-pass block item-list cache owned by {@link BlockComputer}, or {@code null} when this
     * node has none (not a block container, a leaf container, or invalidated).
     *
     * <p>Stored on the node rather than in a pass-scoped map so that the list survives from one
     * layout pass to the next. Package-private through the accessors below: this is internal
     * layout state, not part of the public API.</p>
     */
    private BlockComputer.BlockItemCache blockItemCache;

    /**
     * Cross-pass flex item-list cache owned by {@link FlexboxComputer}, or {@code null} when this
     * node has none (not a flex container, or invalidated).
     *
     * <p>Stored on the node for the same reason as {@link #blockItemCache}: the list must survive
     * from one layout pass to the next. Package-private through the accessors below: this is
     * internal layout state, not part of the public API.</p>
     */
    private FlexboxComputer.FlexItemCache flexItemCache;

    /**
     * Cross-pass grid item-list cache owned by {@link GridComputer}, or {@code null} when this
     * node has none (not a grid container, or invalidated).
     *
     * <p>Stored on the node for the same reason as {@link #blockItemCache}: the list must survive
     * from one layout pass to the next. Package-private through the accessors below: this is
     * internal layout state, not part of the public API.</p>
     */
    private GridComputer.GridItemCache gridItemCache;

    /**
     * Returns the mutable child list of this node. Package-private: child links are owned and
     * mutated exclusively by {@link TaffyTree}; layout passes read it without a map lookup.
     */
    List<NodeId> getChildren() {
        return children;
    }

    /** Returns the stamp a parent's cached item list compares for this node. */
    long getContentVersion() {
        return contentVersion;
    }

    /** Returns the version of this node's child list. */
    long getChildrenVersion() {
        return childrenVersion;
    }

    /**
     * Records a child-list mutation and drops the cached item list that described the old list.
     */
    void bumpChildrenVersion() {
        childrenVersion++;
        blockItemCache = null;
        flexItemCache = null;
        gridItemCache = null;
    }

    /** Returns the cross-pass block item cache, or {@code null} when there is none. */
    BlockComputer.BlockItemCache getBlockItemCache() {
        return blockItemCache;
    }

    /** Stores a freshly built cross-pass block item list. */
    void setBlockItemCache(BlockComputer.BlockItemCache cache) {
        this.blockItemCache = cache;
    }

    /** Drops the cross-pass block item list without touching any version stamp. */
    void clearBlockItemCache() {
        this.blockItemCache = null;
    }

    /** Returns the cross-pass flex item cache, or {@code null} when there is none. */
    FlexboxComputer.FlexItemCache getFlexItemCache() {
        return flexItemCache;
    }

    /** Stores a freshly built cross-pass flex item list. */
    void setFlexItemCache(FlexboxComputer.FlexItemCache cache) {
        this.flexItemCache = cache;
    }

    /** Drops the cross-pass flex item list without touching any version stamp. */
    void clearFlexItemCache() {
        this.flexItemCache = null;
    }

    /** Returns the cross-pass grid item cache, or {@code null} when there is none. */
    GridComputer.GridItemCache getGridItemCache() {
        return gridItemCache;
    }

    /** Stores a freshly built cross-pass grid item list. */
    void setGridItemCache(GridComputer.GridItemCache cache) {
        this.gridItemCache = cache;
    }

    /** Drops the cross-pass grid item list without touching any version stamp. */
    void clearGridItemCache() {
        this.gridItemCache = null;
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
        // A replacement style is a new input for every consumer of this node: bump the stamp a
        // parent's cached block item list compares, and drop this node's own list. Ancestors are
        // intentionally not touched; their lists only depend on their direct children's stamps.
        this.contentVersion++;
        this.blockItemCache = null;
        this.flexItemCache = null;
        this.gridItemCache = null;
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
     * Marks the node as dirty (needing layout recalculation) and invalidates the inputs a parent's
     * cached block item list observes.
     *
     * <p>A direct markDirty is also how an in-place {@code getStyle()} mutation, a new measure
     * callback and a new node context are announced, so it bumps {@link #getContentVersion()} and
     * drops this node's own block item cache. Ancestors reached by dirty-flag propagation use
     * {@link #markDirtyAncestor()} instead: their layout caches must be cleared, but their item
     * caches only depend on their own direct children and stay reusable (the changed child is
     * detected by the content-version comparison).</p>
     *
     * @return true if the layout cache was already empty (node was already dirty)
     */
    public boolean markDirty() {
        this.contentVersion++;
        this.blockItemCache = null;
        this.flexItemCache = null;
        this.gridItemCache = null;
        return cache.clear();
    }

    /**
     * Clears this node's layout cache after its own child list changed.
     *
     * <p>The child-list version and the block item cache were already invalidated by
     * {@link #bumpChildrenVersion()}. The content version deliberately stays unchanged: a
     * grandparent's item list only depends on its direct children's styles, not on this node's
     * children, so propagating a new content stamp would needlessly rebuild every ancestor.</p>
     *
     * @return true if the layout cache was already empty (node was already dirty)
     */
    boolean markDirtyForChildListChange() {
        this.blockItemCache = null;
        this.flexItemCache = null;
        this.gridItemCache = null;
        return cache.clear();
    }

    /**
     * Clears this node's layout cache as part of a dirty-flag propagation from a descendant.
     *
     * <p>Unlike {@link #markDirty()}, this neither bumps the content version nor drops the block
     * item cache: the descendant's own content version is what its direct parent compares, and a
     * container's item list does not depend on anything below its direct children.</p>
     *
     * @return true if the layout cache was already empty (node was already dirty)
     */
    boolean markDirtyAncestor() {
        return cache.clear();
    }
}
