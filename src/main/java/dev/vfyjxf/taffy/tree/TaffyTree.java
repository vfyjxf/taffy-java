package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.util.MeasureContext;
import dev.vfyjxf.taffy.util.MeasureFunc;
import dev.vfyjxf.taffy.util.MeasureFunction;
import dev.vfyjxf.taffy.util.RoundLayout;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An entire tree of UI nodes. The entry point to Taffy's high-level API.
 * <p>
 * Allows you to build a tree of UI nodes, run Taffy's layout algorithms over that tree,
 * and then access the resultant layout.
 */
public class TaffyTree {

    private static final int DEFAULT_CAPACITY = 16;

    /** Counter for generating unique node IDs */
    private final AtomicLong nodeIdCounter = new AtomicLong(0);

    /** NodeData storage by node ID - using fastutil for faster primitive key access */
    private final Long2ObjectOpenHashMap<NodeData> nodes;

    /**
     * Detailed grid information recorded by the most recent completed grid layout of a node
     * (GR-18 additive API). Kept separately from {@link NodeData} so that the internal node
     * record is untouched; the map is maintained by {@link #setDetailedGridInfo(NodeId,
     * DetailedGridInfo)} from the grid algorithm and pruned by {@link #remove(NodeId)}/
     * {@link #clear()}.
     */
    private final Long2ObjectOpenHashMap<DetailedGridInfo> detailedGridInfos;

    /** Context data (legacy measure functions) storage by node ID */
    private final Long2ObjectOpenHashMap<MeasureFunc> nodeContextData;

    /**
     * Generic per-node measure context (S1, 1.2 additive) storage by node ID.
     *
     * <p>This is the {@link MeasureContext#nodeContext()} slot handed to new-style
     * {@link MeasureFunction} callbacks. It is independent from the measure callback itself:
     * setting a context does not make a node measurable, and a node without a measure function
     * can still carry a context.</p>
     */
    private final Long2ObjectOpenHashMap<Object> nodeMeasureContexts;

    /** New-style measure functions (S1, 1.2 additive) storage by node ID. */
    private final Long2ObjectOpenHashMap<MeasureFunction> nodeMeasureFunctions;

    /** Parent of each node */
    private final Long2ObjectOpenHashMap<NodeId> parents;

    /**
     * Monotonic stamp for the memoized resolved directions stored on {@link NodeData}.
     *
     * <p>A {@link NodeData#getResolvedDirection() cached direction} is trusted only while its
     * stamp equals this value. The counter is bumped when a layout pass starts (a new pass can
     * observe styles mutated in place through {@code getStyle}) and on every style or child-list
     * mutation: direction is inherited, so changing one node's style or moving a subtree under a
     * different parent can change the direction of every node below it, and a counter bump
     * invalidates that whole subtree in O(1) instead of walking it. Starts at 1 so the
     * {@code -1} stamp of a freshly created node is never trusted.</p>
     */
    private long directionEpoch = 1L;

    /**
     * Monotonic counter of completed layout requests, used to keep the Yoga-style "a recompute
     * marks the tree" contract while a request reuses every stored layout.
     *
     * <p>{@link #hasNewLayout(NodeId)} reports a node as having a new layout while this counter is
     * newer than the generation the node was acknowledged at ({@link
     * NodeData#getAcknowledgedLayoutGeneration()}). A real layout pass still writes layouts and
     * marks the written nodes directly; the counter is what lets the tree-clean fast path report
     * every node without walking the subtree to set flags. It is not a dirty flag: it never
     * invalidates a cache or a layout.</p>
     */
    private long layoutGeneration = 0L;

    /**
     * Number of layout passes actually executed by {@link #computeLayoutInternal}. Requests
     * answered by the tree-clean fast path do not count. Package-private: an internal diagnostic
     * for tests and profiling, not part of the public API.
     */
    private long layoutPassCount = 0L;

    /**
     * The request signature of the most recent successful layout pass, or {@code null} when the
     * stored layouts may no longer answer an identical request; see {@link #tryReuseLastLayout}.
     */
    private LayoutMemo lastLayout = null;

    /** Whether to round layout values */
    private boolean useRounding = true;
    
    /** Optional listener for layout change notifications */
    private LayoutChangeListener layoutChangeListener = null;

    /**
     * Creates a new TaffyTree with default capacity.
     */
    public TaffyTree() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Creates a new TaffyTree with the specified initial capacity.
     */
    public TaffyTree(int capacity) {
        this.nodes = new Long2ObjectOpenHashMap<>(capacity);
        this.detailedGridInfos = new Long2ObjectOpenHashMap<>(capacity);
        this.nodeContextData = new Long2ObjectOpenHashMap<>(capacity);
        this.nodeMeasureContexts = new Long2ObjectOpenHashMap<>(capacity);
        this.nodeMeasureFunctions = new Long2ObjectOpenHashMap<>(capacity);
        this.parents = new Long2ObjectOpenHashMap<>(capacity);
    }

    /**
     * The signature of one successful layout request: which root was laid out, into which
     * available space, with which rounding configuration and which default measure callbacks.
     *
     * <p>Together with a warm root layout cache this is what {@link #tryReuseLastLayout} requires
     * before it answers a request without computing anything. The available space is snapshotted
     * per axis (the {@link AvailableSpace} type is immutable), so a caller that mutates the
     * {@link TaffySize} it passed in cannot corrupt the recorded signature.</p>
     */
    private static final class LayoutMemo {
        private final long rootId;
        private final AvailableSpace availableWidth;
        private final AvailableSpace availableHeight;
        private final boolean rounding;
        private final MeasureFunc defaultMeasureFunc;
        private final MeasureFunction defaultMeasureFunction;

        private LayoutMemo(long rootId, AvailableSpace availableWidth, AvailableSpace availableHeight,
                           boolean rounding, MeasureFunc defaultMeasureFunc,
                           MeasureFunction defaultMeasureFunction) {
            this.rootId = rootId;
            this.availableWidth = availableWidth;
            this.availableHeight = availableHeight;
            this.rounding = rounding;
            this.defaultMeasureFunc = defaultMeasureFunc;
            this.defaultMeasureFunction = defaultMeasureFunction;
        }
    }

    // === Configuration ===

    /**
     * Enable rounding of layout values. Rounding is enabled by default.
     */
    public void enableRounding() {
        if (!this.useRounding) {
            this.useRounding = true;
            // Rounding is part of the layout request signature, but the memo is dropped eagerly so
            // that no later change to the comparison can resurrect a stale signature.
            invalidateLayoutReuse();
        }
    }

    /**
     * Disable rounding of layout values.
     */
    public void disableRounding() {
        if (this.useRounding) {
            this.useRounding = false;
            invalidateLayoutReuse();
        }
    }

    /**
     * Returns whether rounding is enabled.
     */
    public boolean roundingEnabled() {
        return useRounding;
    }
    
    /**
     * Sets a listener to be notified when node layouts change during computation.
     * 
     * <p>This allows users to:
     * <ul>
     *   <li>Collect a dirty set of changed nodes for efficient incremental updates</li>
     *   <li>Define custom "root node" concepts and track changes within subtrees</li>
     *   <li>Implement custom layout change handling logic</li>
     * </ul>
     * 
     * @param listener the listener, or null to remove the current listener
     * @see LayoutChangeListener
     */
    public void setLayoutChangeListener(LayoutChangeListener listener) {
        this.layoutChangeListener = listener;
    }
    
    /**
     * Gets the current layout change listener.
     * @return the current listener, or null if none is set
     */
    public LayoutChangeListener getLayoutChangeListener() {
        return layoutChangeListener;
    }

    // === Node Creation ===

    /**
     * Creates and adds a new unattached leaf node to the tree.
     */
    public NodeId newLeaf(TaffyStyle style) {
        long id = nodeIdCounter.getAndIncrement();
        NodeId nodeId = new NodeId(id);
        
        NodeData data = new NodeData(style);
        // A node that has never been laid out has no layout to report as new; recording the
        // current generation keeps hasNewLayout false until the next layout request.
        data.acknowledgeLayoutGeneration(layoutGeneration);
        nodes.put(id, data);
        parents.put(id, null);
        
        return nodeId;
    }

    /**
     * Creates and adds a new unattached leaf node with a measure function.
     */
    public NodeId newLeafWithMeasure(TaffyStyle style, MeasureFunc measureFunc) {
        long id = nodeIdCounter.getAndIncrement();
        NodeId nodeId = new NodeId(id);
        
        NodeData data = new NodeData(style);
        data.setHasContext(true);
        data.acknowledgeLayoutGeneration(layoutGeneration);
        nodes.put(id, data);
        nodeContextData.put(id, measureFunc);
        parents.put(id, null);
        
        return nodeId;
    }

    /**
     * Creates and adds a new unattached leaf node with a new-style measure function (S1, 1.2
     * additive).
     *
     * <p>This is the {@link MeasureFunction} counterpart of
     * {@link #newLeafWithMeasure(TaffyStyle, MeasureFunc)}: exactly one measure callback is
     * attached to the node, so the new-style slot is populated and the legacy slot stays empty.</p>
     *
     * @param style       the style of the new node
     * @param measureFunc the new-style measure callback, or {@code null} for none
     * @return the id of the created node
     */
    public NodeId newLeafWithMeasureFunction(TaffyStyle style, MeasureFunction measureFunc) {
        long id = nodeIdCounter.getAndIncrement();
        NodeId nodeId = new NodeId(id);

        NodeData data = new NodeData(style);
        data.setHasContext(measureFunc != null);
        data.acknowledgeLayoutGeneration(layoutGeneration);
        nodes.put(id, data);
        if (measureFunc != null) {
            nodeMeasureFunctions.put(id, measureFunc);
        }
        parents.put(id, null);

        return nodeId;
    }

    /**
     * Creates and adds a new node with children.
     */
    public NodeId newWithChildren(TaffyStyle style, NodeId... childNodes) {
        long id = nodeIdCounter.getAndIncrement();
        NodeId nodeId = new NodeId(id);
        
        NodeData data = new NodeData(style);
        data.acknowledgeLayoutGeneration(layoutGeneration);
        nodes.put(id, data);

        List<NodeId> childList = data.getChildren();
        for (NodeId child : childNodes) {
            parents.put(child.getId(), nodeId);
            childList.add(child);
        }

        parents.put(id, null);
        
        return nodeId;
    }

    /**
     * Creates and adds a new node with children from a list.
     */
    public NodeId newWithChildren(TaffyStyle style, List<NodeId> childNodes) {
        return newWithChildren(style, childNodes.toArray(new NodeId[0]));
    }

    // === Tree Manipulation ===

    /**
     * Drops all nodes in the tree.
     */
    public void clear() {
        nodes.clear();
        detailedGridInfos.clear();
        nodeContextData.clear();
        nodeMeasureContexts.clear();
        nodeMeasureFunctions.clear();
        parents.clear();
        invalidateResolvedDirections();
        // The memoized request signature names a root that no longer exists.
        invalidateLayoutReuse();
    }

    /**
     * Remove a specific node from the tree.
     */
    public void remove(NodeId node) {
        long key = node.getId();
        
        // Remove from parent's children list
        NodeId parent = parents.get(key);
        if (parent != null) {
            NodeData parentData = nodes.get(parent.getId());
            if (parentData != null) {
                parentData.getChildren().removeIf(n -> n.equals(node));
            }
            // CR-04 (#1085): the parent's cached layout was computed with this child present, so it
            // must be invalidated exactly like removeChildAtIndex does.
            markDirty(parent);
        }
        
        // Remove parent references from this node's children
        NodeData data = nodes.get(key);
        if (data != null) {
            for (NodeId child : data.getChildren()) {
                parents.put(child.getId(), null);
            }
        }

        parents.remove(key);
        nodes.remove(key);
        detailedGridInfos.remove(key);
        nodeContextData.remove(key);
        nodeMeasureContexts.remove(key);
        nodeMeasureFunctions.remove(key);
        invalidateResolvedDirections();
    }

    // === Context (Measure Function) Management ===

    /**
     * Sets the legacy measure function for a node.
     *
     * <p>Registering a non-null legacy callback replaces a new-style {@link MeasureFunction}
     * registered on the same node through {@link #setMeasureFunction(NodeId, MeasureFunction)}:
     * a node has at most one measure callback. Passing {@code null} only clears the legacy slot
     * and leaves a new-style callback in place.</p>
     */
    public void setMeasureFunc(NodeId node, MeasureFunc measureFunc) {
        long key = node.getId();
        NodeData data = nodes.get(key);
        if (data == null) {
            throw TaffyException.invalidInputNode(node);
        }
        
        if (measureFunc != null) {
            nodeMeasureFunctions.remove(key);
            nodeContextData.put(key, measureFunc);
        } else {
            nodeContextData.remove(key);
        }
        data.setHasContext(measureFunc != null || nodeMeasureFunctions.containsKey(key));
        
        markDirty(node);
    }

    /**
     * Gets the legacy measure function for a node.
     *
     * <p>Returns {@code null} when the node has no legacy callback. In particular, after a
     * successful {@link #setMeasureFunction(NodeId, MeasureFunction)} call the new-style callback
     * replaces the legacy one, so this getter returns {@code null} even though the node is
     * measurable; use {@link #getMeasureFunction(NodeId)} to read the new-style slot.</p>
     */
    public MeasureFunc getMeasureFunc(NodeId node) {
        return nodeContextData.get(node.getId());
    }

    /**
     * Sets the new-style measure function for a node (S1, 1.2 additive).
     *
     * <p>The callback receives a {@link MeasureContext} with the content-box layout inputs, the
     * node style, the node id and the generic node context, and returns a
     * {@code LayoutOutput} whose size is treated as the content size. Registering a non-null
     * callback replaces a legacy {@link MeasureFunc} registered on the same node: a node has at
     * most one measure callback. Passing {@code null} only clears the new-style slot and leaves a
     * legacy callback in place.</p>
     *
     * <p>Like {@link #setMeasureFunc(NodeId, MeasureFunc)}, this invalidates the node's cached
     * layout and marks it (and its ancestors) dirty.</p>
     *
     * @param node        the node to set the callback on
     * @param measureFunc the new-style callback, or {@code null} to clear it
     * @throws TaffyException if the node does not exist
     */
    public void setMeasureFunction(NodeId node, MeasureFunction measureFunc) {
        long key = node.getId();
        NodeData data = nodes.get(key);
        if (data == null) {
            throw TaffyException.invalidInputNode(node);
        }

        if (measureFunc != null) {
            nodeContextData.remove(key);
            nodeMeasureFunctions.put(key, measureFunc);
        } else {
            nodeMeasureFunctions.remove(key);
        }
        data.setHasContext(measureFunc != null || nodeContextData.containsKey(key));

        markDirty(node);
    }

    /**
     * Gets the new-style measure function for a node (S1, 1.2 additive).
     *
     * @param node the node to query
     * @return the registered {@link MeasureFunction}, or {@code null} if none is set
     */
    public MeasureFunction getMeasureFunction(NodeId node) {
        return nodeMeasureFunctions.get(node.getId());
    }

    /**
     * Sets the generic measure context for a node (S1, 1.2 additive).
     *
     * <p>The context is opaque to the layout algorithms: it is only passed to a
     * {@link MeasureFunction} through {@link MeasureContext#nodeContext()}, which lets a caller
     * attach text, a resource handle or any other state without closing over it. Setting or
     * clearing the context invalidates the node's cached layout and marks it (and its ancestors)
     * dirty, because the context can change what the measure callback returns.</p>
     *
     * @param node    the node to set the context on
     * @param context the context object, or {@code null} to clear it
     * @throws TaffyException if the node does not exist
     */
    public void setNodeContext(NodeId node, Object context) {
        long key = node.getId();
        if (!nodes.containsKey(key)) {
            throw TaffyException.invalidInputNode(node);
        }

        if (context != null) {
            nodeMeasureContexts.put(key, context);
        } else {
            nodeMeasureContexts.remove(key);
        }

        markDirty(node);
    }

    /**
     * Gets the generic measure context for a node (S1, 1.2 additive).
     *
     * @param node the node to query
     * @return the context set through {@link #setNodeContext(NodeId, Object)}, or {@code null}
     */
    public Object getNodeContext(NodeId node) {
        return nodeMeasureContexts.get(node.getId());
    }

    // === Child Management ===

    /**
     * Adds a child node under the parent.
     */
    public void addChild(NodeId parent, NodeId child) {
        long parentKey = parent.getId();
        long childKey = child.getId();

        NodeData parentData = nodes.get(parentKey);
        if (parentData == null) {
            throw TaffyException.invalidParentNode(parent);
        }
        if (!nodes.containsKey(childKey)) {
            throw TaffyException.invalidChildNode(child);
        }

        parents.put(childKey, parent);
        parentData.getChildren().add(child);
        invalidateResolvedDirections();
        markDirty(parent);
    }

    /**
     * Inserts a child at the given index.
     */
    public void insertChildAtIndex(NodeId parent, int childIndex, NodeId child) {
        long parentKey = parent.getId();
        NodeData parentData = nodes.get(parentKey);
        List<NodeId> parentChildren = parentData != null ? parentData.getChildren() : null;
        
        if (parentChildren == null) {
            throw TaffyException.invalidParentNode(parent);
        }
        
        int childCount = parentChildren.size();
        if (childIndex > childCount) {
            throw TaffyException.childIndexOutOfBounds(parent, childIndex, childCount);
        }
        
        parents.put(child.getId(), parent);
        parentChildren.add(childIndex, child);
        invalidateResolvedDirections();
        markDirty(parent);
    }

    /**
     * Sets the children of a node, replacing existing children.
     */
    public void setChildren(NodeId parent, NodeId... newChildren) {
        long parentKey = parent.getId();
        NodeData parentData = nodes.get(parentKey);
        List<NodeId> parentChildList = parentData != null ? parentData.getChildren() : null;
        
        if (parentChildList == null) {
            throw TaffyException.invalidParentNode(parent);
        }
        
        // Remove parent reference from current children
        for (NodeId child : parentChildList) {
            parents.put(child.getId(), null);
        }
        
        // Set new children
        for (NodeId child : newChildren) {
            // Remove from previous parent if any
            NodeId previousParent = parents.get(child.getId());
            if (previousParent != null) {
                removeChild(previousParent, child);
            }
            parents.put(child.getId(), parent);
        }
        
        parentChildList.clear();
        parentChildList.addAll(Arrays.asList(newChildren));
        invalidateResolvedDirections();
        markDirty(parent);
    }

    /**
     * Removes a child from a parent.
     */
    public void removeChild(NodeId parent, NodeId child) {
        NodeData parentData = nodes.get(parent.getId());
        List<NodeId> parentChildren = parentData != null ? parentData.getChildren() : null;
        if (parentChildren == null) {
            throw TaffyException.invalidParentNode(parent);
        }
        
        int index = -1;
        for (int i = 0; i < parentChildren.size(); i++) {
            if (parentChildren.get(i).equals(child)) {
                index = i;
                break;
            }
        }
        
        if (index >= 0) {
            removeChildAtIndex(parent, index);
        }
    }

    /**
     * Removes the child at the given index.
     */
    public NodeId removeChildAtIndex(NodeId parent, int childIndex) {
        long parentKey = parent.getId();
        NodeData parentData = nodes.get(parentKey);
        List<NodeId> parentChildren = parentData != null ? parentData.getChildren() : null;
        
        if (parentChildren == null) {
            throw TaffyException.invalidParentNode(parent);
        }
        
        int childCount = parentChildren.size();
        if (childIndex >= childCount) {
            throw TaffyException.childIndexOutOfBounds(parent, childIndex, childCount);
        }
        
        NodeId child = parentChildren.remove(childIndex);
        parents.put(child.getId(), null);
        invalidateResolvedDirections();
        markDirty(parent);
        
        return child;
    }

    /**
     * Gets the child at the given index.
     */
    public NodeId getChildAtIndex(NodeId parent, int childIndex) {
        long parentKey = parent.getId();
        NodeData parentData = nodes.get(parentKey);
        List<NodeId> parentChildren = parentData != null ? parentData.getChildren() : null;
        
        if (parentChildren == null) {
            throw TaffyException.invalidParentNode(parent);
        }
        
        int childCount = parentChildren.size();
        if (childIndex >= childCount) {
            throw TaffyException.childIndexOutOfBounds(parent, childIndex, childCount);
        }
        
        return parentChildren.get(childIndex);
    }

    /**
     * Replaces the child at the given index with a new child.
     */
    public NodeId replaceChildAtIndex(NodeId parent, int childIndex, NodeId newChild) {
        long parentKey = parent.getId();
        NodeData parentData = nodes.get(parentKey);
        List<NodeId> parentChildren = parentData != null ? parentData.getChildren() : null;
        
        if (parentChildren == null) {
            throw TaffyException.invalidParentNode(parent);
        }
        
        int childCount = parentChildren.size();
        if (childIndex >= childCount) {
            throw TaffyException.childIndexOutOfBounds(parent, childIndex, childCount);
        }
        
        parents.put(newChild.getId(), parent);
        NodeId oldChild = parentChildren.set(childIndex, newChild);
        parents.put(oldChild.getId(), null);
        invalidateResolvedDirections();
        markDirty(parent);
        
        return oldChild;
    }

    // === Getters ===

    /**
     * Returns the number of children of a node.
     */
    public int childCount(NodeId parent) {
        NodeData data = nodes.get(parent.getId());
        return data != null ? data.getChildren().size() : 0;
    }

    /**
     * Returns an unmodifiable list of children.
     */
    public List<NodeId> getChildren(NodeId parent) {
        NodeData data = nodes.get(parent.getId());
        return data != null ? data.getChildren() : Collections.emptyList();
    }

    /**
     * Returns the children list directly without creating a wrapper.
     * Only for internal use where the caller guarantees not to modify the list.
     */
    List<NodeId> getChildrenInternal(NodeId parent) {
        NodeData data = nodes.get(parent.getId());
        return data != null ? data.getChildren() : null;
    }

    /**
     * Returns the total number of nodes in the tree.
     */
    public int totalNodeCount() {
        return nodes.size();
    }

    /**
     * Returns the parent of a node.
     */
    public NodeId getParent(NodeId child) {
        return parents.get(child.getId());
    }

    /** Returns the epoch that stamps currently valid {@link NodeData} resolved directions. */
    long currentDirectionEpoch() {
        return directionEpoch;
    }

    /**
     * Invalidates every memoized resolved direction by starting a new direction epoch.
     *
     * <p>Called when a layout pass starts and after any style or child-list change, because a
     * direction is inherited: one changed style or one re-parented subtree may change the
     * resolved direction of arbitrarily many descendants. Starting a new epoch invalidates all
     * of them at once (O(1)); the layout pass then resolves each node at most once, top-down.</p>
     */
    void invalidateResolvedDirections() {
        directionEpoch++;
    }

    // === Style Management ===

    /**
     * Sets the style of a node.
     */
    public void setStyle(NodeId node, TaffyStyle style) {
        NodeData data = nodes.get(node.getId());
        if (data == null) {
            throw TaffyException.invalidInputNode(node);
        }
        data.setStyle(style);
        // A new direction may be inherited by the whole subtree; one epoch bump invalidates it.
        invalidateResolvedDirections();
        markDirty(node);
    }

    /**
     * Gets the style of a node.
     */
    public TaffyStyle getStyle(NodeId node) {
        NodeData data = nodes.get(node.getId());
        if (data == null) {
            throw TaffyException.invalidInputNode(node);
        }
        return data.getStyle();
    }

    // === Layout Access ===

    /**
     * Returns the layout of a node.
     */
    public Layout getLayout(NodeId node) {
        NodeData data = nodes.get(node.getId());
        if (data == null) {
            return null;
        }
        return useRounding ? data.getFinalLayout() : data.getUnroundedLayout();
    }

    /**
     * Returns the unrounded layout of a node.
     */
    public Layout getUnroundedLayout(NodeId node) {
        NodeData data = nodes.get(node.getId());
        return data != null ? data.getUnroundedLayout() : null;
    }

    /**
     * Returns the detailed grid information recorded by the most recent completed layout pass of
     * a grid container (GR-18 additive API).
     *
     * <p>The returned value is an immutable snapshot: it describes the tracks and in-flow item
     * areas the grid algorithm resolved for this node, in the grid container's border-box
     * coordinate space, using unrounded values. Reading it never changes layout and it is never
     * mutated by later layout passes (a later pass replaces the stored value instead). It also
     * exposes the CSR per-line names ({@link DetailedGridInfo.Tracks#lineNameTable()}), the
     * separate {@code grid-template-areas} edge table ({@link DetailedGridInfo#areas()}), the
     * physical item rectangles ({@link DetailedGridInfo#itemRectangle(int)}) and the resolved
     * {@code grid-template-rows}/{@code grid-template-columns} serialization with its CSS
     * {@code zoom} argument ({@link DetailedGridInfo#gridTemplateRows(float)}).</p>
     *
     * <p>Returns {@code null} when the node does not exist, when its current display is not
     * {@link TaffyDisplay#GRID}, and when a grid node has not gone through the grid layout path
     * yet. Notably, this port dispatches a childless node to the measured-leaf path, so a grid
     * container with no children has no detailed information. A non-grid node returns
     * {@code null} rather than an empty value object: there is no axis or item information to
     * report, and callers must be able to distinguish "not a grid" from "a grid that has not been
     * laid out" (the Javadoc of {@link DetailedGridInfo} documents this first-slice contract).</p>
     *
     * <p>The information is not cleared eagerly by style or child changes; it is replaced by the
     * next completed grid layout. Call this after {@code computeLayout}/{@code
     * computeLayoutWithMeasure} to obtain current values.</p>
     *
     * @param node the node to query
     * @return the detailed grid info, or {@code null} as described above
     */
    public DetailedGridInfo getDetailedGridInfo(NodeId node) {
        NodeData data = nodes.get(node.getId());
        if (data == null) {
            return null;
        }
        if (data.getStyle().getDisplay() != TaffyDisplay.GRID) {
            return null;
        }
        return detailedGridInfos.get(node.getId());
    }

    /**
     * Records the detailed grid information of a node. Layout-algorithm internal: the grid
     * algorithm calls this after resolving tracks and placements; callers of the public API only
     * read the value through {@link #getDetailedGridInfo(NodeId)}.
     *
     * <p>A {@code null} value clears the entry, and an unknown node is ignored. The map is pruned
     * by {@link #remove(NodeId)} and {@link #clear()} as well.</p>
     */
    void setDetailedGridInfo(NodeId node, DetailedGridInfo detailedGridInfo) {
        long key = node.getId();
        if (!nodes.containsKey(key)) {
            return;
        }
        if (detailedGridInfo == null) {
            detailedGridInfos.remove(key);
        } else {
            detailedGridInfos.put(key, detailedGridInfo);
        }
    }

    // === Layout Change Tracking (Yoga-style dirty propagation) ===

    /**
     * Returns true if this node has a new layout that hasn't been acknowledged.
     * <p>
     * Similar to Yoga's hasNewLayout - set after layout computation, cleared by acknowledgeLayout().
     * Unlike the previous version-based approach, this is set regardless of whether the layout
     * actually changed, allowing users to walk the tree efficiently from root.
     * 
     * @deprecated Use {@link #hasNewLayout(NodeId)} instead for clearer naming.
     */
    @Deprecated
    public boolean hasUnconsumedLayout(NodeId node) {
        return hasNewLayout(node);
    }

    /**
     * Returns true if this node has a new layout that hasn't been acknowledged.
     * <p>
     * Similar to Yoga's hasNewLayout - set after layout computation, cleared by acknowledgeLayout().
     */
    public boolean hasNewLayout(NodeId node) {
        NodeData data = nodes.get(node.getId());
        if (data == null) {
            throw TaffyException.invalidInputNode(node);
        }
        // A node is "new" when its layout was written since it was acknowledged, or when a layout
        // request completed after that acknowledgement. The second term is what keeps the
        // Yoga-style "a recompute marks the tree" behavior for the tree-clean fast path, which
        // reuses every stored layout and therefore writes no per-node flag.
        return data.hasNewLayout() || layoutGeneration > data.getAcknowledgedLayoutGeneration();
    }

    /**
     * Returns true if any descendant of this node has a new layout.
     * <p>
     * This allows efficient tree walking from root - you can skip entire subtrees
     * where no layout changes occurred.
     */
    public boolean hasDirtyDescendant(NodeId node) {
        NodeData data = nodes.get(node.getId());
        if (data == null) {
            throw TaffyException.invalidInputNode(node);
        }
        return data.hasDirtyDescendant();
    }

    /**
     * Returns true if this node or any of its descendants has a new layout.
     * <p>
     * Convenience method for tree walking - returns true if you need to visit
     * this node or any of its children.
     */
    public boolean needsVisit(NodeId node) {
        NodeData data = nodes.get(node.getId());
        if (data == null) {
            throw TaffyException.invalidInputNode(node);
        }
        // See hasNewLayout: a completed request marks the whole tree for the walker even when it
        // reused every stored layout and therefore wrote no per-node flag.
        return data.hasNewLayout() || data.hasDirtyDescendant()
            || layoutGeneration > data.getAcknowledgedLayoutGeneration();
    }

    /**
     * Marks the current layout as consumed/acknowledged for this node.
     */
    public void acknowledgeLayout(NodeId node) {
        NodeData data = nodes.get(node.getId());
        if (data == null) {
            throw TaffyException.invalidInputNode(node);
        }
        data.acknowledgeLayout();
        data.acknowledgeLayoutGeneration(layoutGeneration);
    }

    /**
     * Acknowledges layout for this node and clears dirty descendant flag.
     * <p>
     * Call this after you have processed this node AND all its descendants.
     * This is useful for bottom-up acknowledgement during tree traversal.
     */
    public void acknowledgeSubtree(NodeId node) {
        NodeData data = nodes.get(node.getId());
        if (data == null) {
            throw TaffyException.invalidInputNode(node);
        }
        data.acknowledgeLayout();
        data.acknowledgeLayoutGeneration(layoutGeneration);
        data.clearDirtyDescendant();
    }

    /**
     * Marks a node as having a new layout and propagates dirty flag up to ancestors.
     * Also notifies the layout change listener if one is set.
     *
     * <p>The caller supplies the {@link NodeData} it already fetched, so the layout-writing paths
     * ({@link #setLayout}, {@link #setUnroundedLayout}, {@link #roundLayout}) look a node up once
     * instead of twice.</p>
     */
    private void markNodeLayoutUpdated(NodeData data, long nodeKey, Layout oldLayout, Layout newLayout) {
        data.markNewLayout();

        // Notify the layout change listener
        if (layoutChangeListener != null) {
            layoutChangeListener.onLayoutChanged(NodeId.of(nodeKey), oldLayout, newLayout);
        }

        // Propagate dirty descendant flag up to ancestors
        NodeId parent = parents.get(nodeKey);
        while (parent != null) {
            NodeData parentData = nodes.get(parent.getId());
            if (parentData == null) break;
            
            // If already marked, all ancestors are already marked too
            if (parentData.markDirtyDescendant()) {
                break;
            }
            parent = parents.get(parent.getId());
        }
    }

    /**
     * Sets the final (rounded) layout of a node.
     */
    public void setLayout(NodeId node, Layout layout) {
        NodeData data = nodes.get(node.getId());
        if (data != null) {
            Layout oldLayout = data.getFinalLayout();
            data.setFinalLayout(layout);
            // When rounding is enabled, mark after setting final layout
            if (useRounding) {
                markNodeLayoutUpdated(data, node.getId(), oldLayout, layout);
            }
            // An explicit layout write is invisible to the dirty flags (it does not clear any
            // cache), so a reused layout request must not skip the write's effect: a rounding pass
            // would re-derive this node's final layout from its unrounded layout.
            invalidateLayoutReuse();
        }
    }

    /**
     * Sets the unrounded layout of a node.
     */
    public void setUnroundedLayout(NodeId node, Layout layout) {
        NodeData data = nodes.get(node.getId());
        if (data != null) {
            Layout oldLayout = data.getUnroundedLayout();
            data.setUnroundedLayout(layout);
            // When rounding is disabled, mark after setting unrounded layout
            if (!useRounding) {
                markNodeLayoutUpdated(data, node.getId(), oldLayout, layout);
            }
            // See setLayout: an explicitly written layout is invisible to the dirty flags, while a
            // rounding pass would re-derive the final layout from it.
            invalidateLayoutReuse();
        }
    }

    // === Cache Management ===

    /**
     * Gets the cache entry for a node for the given layout inputs (BL-04).
     *
     * @param node   the node
     * @param inputs the inputs of the layout pass
     * @return the cached output, or {@code null} if there is no usable entry
     */
    public LayoutOutput getCacheEntry(NodeId node, LayoutInput inputs) {
        NodeData data = nodes.get(node.getId());
        if (data == null) return null;
        return data.getCache().get(inputs);
    }

    /**
     * Stores a cache entry for a node for the given layout inputs (BL-04).
     *
     * @param node   the node
     * @param inputs the inputs of the layout pass
     * @param output the computed result
     */
    public void storeCacheEntry(NodeId node, LayoutInput inputs, LayoutOutput output) {
        NodeData data = nodes.get(node.getId());
        if (data != null) {
            data.getCache().store(inputs, output);
            // An injected entry is not a dirty-flag mutation, so the request signature recorded by
            // the last pass can no longer be trusted to describe the stored layouts.
            invalidateLayoutReuse();
        }
    }

    /**
     * Gets the cache entry for a node.
     *
     * @deprecated use {@link #getCacheEntry(NodeId, LayoutInput)} instead. Kept until 2.0 (Q3).
     */
    @Deprecated
    public LayoutOutput getCacheEntry(NodeId node, FloatSize knownDimensions,
                                      TaffySize<AvailableSpace> availableSpace, RunMode runMode) {
        NodeData data = nodes.get(node.getId());
        if (data == null) return null;
        return data.getCache().get(knownDimensions, availableSpace, runMode);
    }

    /**
     * Stores a cache entry for a node.
     *
     * @deprecated use {@link #storeCacheEntry(NodeId, LayoutInput, LayoutOutput)} instead. Kept
     * until 2.0 (Q3).
     */
    @Deprecated
    public void storeCacheEntry(NodeId node, FloatSize knownDimensions,
                                TaffySize<AvailableSpace> availableSpace, RunMode runMode,
                                LayoutOutput output) {
        NodeData data = nodes.get(node.getId());
        if (data != null) {
            data.getCache().store(knownDimensions, availableSpace, runMode, output);
            // See the LayoutInput overload: an injected entry bypasses the dirty flags.
            invalidateLayoutReuse();
        }
    }

    /**
     * Clears the cache for a node.
     */
    public void clearCache(NodeId node) {
        NodeData data = nodes.get(node.getId());
        if (data != null) {
            data.getCache().clear();
        }
    }

    // === Dirty State ===

    /**
     * Marks the node and its ancestors as needing layout recalculation.
     */
    public void markDirty(NodeId node) {
        markDirtyRecursive(node);
    }

    private void markDirtyRecursive(NodeId node) {
        NodeData data = nodes.get(node.getId());
        if (data == null) return;
        
        boolean wasAlreadyDirty = data.markDirty();
        if (!wasAlreadyDirty) {
            NodeId parent = parents.get(node.getId());
            if (parent != null) {
                markDirtyRecursive(parent);
            }
        }
    }

    /**
     * Returns whether a node needs layout recalculation.
     */
    public boolean isDirty(NodeId node) {
        NodeData data = nodes.get(node.getId());
        return data == null || data.getCache().isEmpty();
    }

    // === Layout Computation ===

    /**
     * Computes the layout for the tree starting from the given root node.
     */
    public void computeLayout(NodeId rootNode, TaffySize<AvailableSpace> availableSpace) {
        // Route through the legacy entry point: subclasses overriding computeLayoutWithMeasure keep
        // observing computeLayout even though the new MeasureFunction entry point exists.
        computeLayoutWithMeasure(rootNode, availableSpace, null);
    }

    /**
     * Computes the layout with a default new-style measure function (S1, 1.2 additive).
     *
     * <p>The default is consulted for every leaf node that has no per-node measure callback of
     * its own; see {@code LayoutComputer} for the full precedence. This is the new-terminology
     * counterpart of {@link #computeLayoutWithMeasure(NodeId, TaffySize, MeasureFunc)}; both entry
     * points share the same layout computation and rounding. It is deliberately a separate method
     * name (not an overload of {@code computeLayout}/{@code computeLayoutWithMeasure}) so that
     * existing source passing a bare {@code null} as the third argument keeps compiling against a
     * single applicable signature. {@link #computeLayout(NodeId, TaffySize)} keeps routing through
     * {@link #computeLayoutWithMeasure(NodeId, TaffySize, MeasureFunc)} so that legacy subclass
     * overrides of that method are still honoured.</p>
     *
     * @param rootNode               the root of the tree to lay out
     * @param availableSpace         the space to lay the tree out into
     * @param defaultMeasureFunction the fallback new-style measure callback, or {@code null}
     */
    public void computeLayoutWithMeasureFunction(NodeId rootNode,
                                                 TaffySize<AvailableSpace> availableSpace,
                                                 MeasureFunction defaultMeasureFunction) {
        computeLayoutInternal(rootNode, availableSpace, null, defaultMeasureFunction);
    }

    /**
     * Computes the layout with a custom legacy measure function for all nodes.
     */
    public void computeLayoutWithMeasure(NodeId rootNode, TaffySize<AvailableSpace> availableSpace,
                                          MeasureFunc defaultMeasureFunc) {
        computeLayoutInternal(rootNode, availableSpace, defaultMeasureFunc, null);
    }

    /**
     * Shared implementation of the layout entry points.
     *
     * <p>An identical request on a tree whose last layout is still valid is answered without
     * computing anything: see {@link #tryReuseLastLayout}. Every other request runs the layout
     * algorithm and, when rounding is enabled, the rounding pass, and then records its signature
     * for the next request.</p>
     */
    private void computeLayoutInternal(NodeId rootNode, TaffySize<AvailableSpace> availableSpace,
                                       MeasureFunc defaultMeasureFunc,
                                       MeasureFunction defaultMeasureFunction) {
        // A completed request makes every not-yet-acknowledged node report a new layout, including
        // the nodes a fast-path hit does not touch.
        layoutGeneration++;

        if (tryReuseLastLayout(rootNode, availableSpace, defaultMeasureFunc, defaultMeasureFunction)) {
            return;
        }

        LayoutComputer computer = new LayoutComputer(this, defaultMeasureFunc, defaultMeasureFunction);
        computer.computeLayout(rootNode, availableSpace);

        // Round layouts if enabled
        if (useRounding) {
            roundLayout(rootNode);
        }
        layoutPassCount++;

        lastLayout = new LayoutMemo(
            rootNode.getId(),
            availableSpace.width,
            availableSpace.height,
            useRounding,
            defaultMeasureFunc,
            defaultMeasureFunction
        );
    }

    /**
     * Whether an identical layout request is already answered by the stored layouts, in which case
     * {@link #computeLayoutInternal} returns without running the layout algorithm or the rounding
     * pass.
     *
     * <p>This is the tree-clean fast path. It exists because the layout cache already answers an
     * unchanged request for the root, but only after the whole root entry point has run and only
     * for the algorithm: the rounding pass still walks every node and re-derives (and re-allocates)
     * every final layout on every request, even when nothing can have changed. Returning before
     * both passes keeps the result identical, since a request that reaches this method would have
     * been a pure root-cache hit followed by a rounding pass over unchanged values.</p>
     *
     * <p>The conditions are:</p>
     * <ul>
     *   <li>the last successful request had the same root, the same available space (per axis, by
     *       type and value), the same rounding configuration and the same default measure
     *       callbacks (by identity — a different default callback may measure differently);</li>
     *   <li>the root's layout cache is not empty. "Cache is empty" is this tree's dirty sentinel
     *       ({@link #isDirty}): every style, child-list or measure-callback change clears the cache
     *       of the changed node and of all of its ancestors, so a warm root cache means no input
     *       of this subtree changed since the pass that filled it. A mutation outside the laid-out
     *       subtree leaves the root cache warm and correctly does not force a recompute, exactly
     *       like the root-cache hit it replaces. A hidden root ({@code display: none}) stores no
     *       cache entry by design, so its requests always take the normal path;</li>
     *   <li>no {@link LayoutChangeListener} is installed. The listener contract reports every
     *       layout write, so with a listener the request takes the normal path and the listener
     *       keeps observing the same notifications it always did;</li>
     *   <li>no explicit layout or cache write happened since the pass (those do not clear a cache,
     *       so they invalidate the memo directly; see {@link #invalidateLayoutReuse}).</li>
     * </ul>
     *
     * <p>{@code hasNewLayout}/{@code needsVisit} keep reporting the request for every node: the
     * tree-level generation is bumped by the caller, so flag queries do not need a subtree walk.</p>
     */
    private boolean tryReuseLastLayout(NodeId rootNode, TaffySize<AvailableSpace> availableSpace,
                                       MeasureFunc defaultMeasureFunc,
                                       MeasureFunction defaultMeasureFunction) {
        LayoutMemo memo = lastLayout;
        if (memo == null || layoutChangeListener != null || rootNode == null || availableSpace == null) {
            return false;
        }
        if (memo.rootId != rootNode.getId()
            || memo.rounding != useRounding
            || memo.defaultMeasureFunc != defaultMeasureFunc
            || memo.defaultMeasureFunction != defaultMeasureFunction) {
            return false;
        }
        if (!sameAvailableSpace(memo.availableWidth, availableSpace.width)
            || !sameAvailableSpace(memo.availableHeight, availableSpace.height)) {
            return false;
        }

        NodeData rootData = nodes.get(memo.rootId);
        return rootData != null && !rootData.getCache().isEmpty();
    }

    /**
     * Compares one axis of an available space with a recorded one. {@link AvailableSpace} instances
     * are immutable, and the min/max-content constraints are shared singletons, so this is a
     * reference compare in the common case.
     */
    private static boolean sameAvailableSpace(AvailableSpace recorded, AvailableSpace requested) {
        if (recorded == requested) {
            return true;
        }
        if (recorded == null || requested == null) {
            return false;
        }
        return recorded.equals(requested);
    }

    /**
     * Drops the recorded layout request signature, so the next {@link #computeLayout(NodeId,
     * TaffySize)} runs a real layout pass.
     *
     * <p>Called by the writes that bypass the dirty flags (an explicit layout or cache write, a
     * rounding toggle, {@link #clear()}) and by {@link LayoutComputer#computeLayout} for any layout
     * pass, including one a caller started directly through a {@link LayoutComputer}.</p>
     */
    void invalidateLayoutReuse() {
        lastLayout = null;
    }

    /**
     * Number of layout passes actually executed, i.e. requests that were not answered by the
     * tree-clean fast path. Package-private: an internal diagnostic used by tests and profiling,
     * not part of the public API.
     */
    long layoutPassCount() {
        return layoutPassCount;
    }

    // === Layout Rounding ===

    /**
     * Rounds the computed layout of a subtree to whole pixels.
     *
     * <p>This is the rounding pass of {@link #computeLayout(NodeId, TaffySize)} exposed as an
     * additive 1.2 entry point; {@link RoundLayout#roundLayout(TaffyTree, NodeId)} delegates here.
     * The pass iterates depth-first with an explicit primitive stack (no recursion) and pushes
     * each node's children in reverse order, so nodes are visited in pre-order exactly like the
     * original utility implementation.</p>
     *
     * <p>Every visited node is read and written through a single {@link NodeData} lookup: the
     * unrounded layout is rounded in cumulative (absolute) coordinates and stored as the final
     * layout, with the same side effects as {@link #setLayout(NodeId, Layout)} (new-layout flag,
     * ancestor dirty-descendant propagation and {@link LayoutChangeListener} notification; the
     * last two only while rounding is enabled). Positions are rounded in absolute coordinates and
     * converted back to parent-relative offsets, and sizes use the rounded absolute edges, which
     * is what keeps fractional children on their parent's rounded edges (BK-13 / upstream #1189).
     * Unknown nodes and nodes without an unrounded layout are skipped together with their
     * subtree.</p>
     *
     * @param node the root of the subtree to round
     */
    public void roundLayout(NodeId node) {
        // The stack stores the node id plus the cumulative (absolute) offset of the node, which is
        // all the traversal state needed. Primitive arrays keep the per-edge cost to two float
        // writes instead of an Object[] / Float boxing allocation.
        int initialCapacity = Math.max(16, nodes.size());
        long[] nodeIds = new long[initialCapacity];
        float[] cumulativeXs = new float[initialCapacity];
        float[] cumulativeYs = new float[initialCapacity];
        nodeIds[0] = node.getId();
        int stackSize = 1;

        while (stackSize > 0) {
            stackSize--;
            long nodeKey = nodeIds[stackSize];
            float cumulativeX = cumulativeXs[stackSize];
            float cumulativeY = cumulativeYs[stackSize];

            NodeData data = nodes.get(nodeKey);
            if (data == null) continue;
            Layout layout = data.getUnroundedLayout();
            if (layout == null) continue;

            FloatPoint location = layout.location();
            FloatSize size = layout.size();

            float nodeX = !Float.isNaN(location.x) ? location.x : 0f;
            float nodeY = !Float.isNaN(location.y) ? location.y : 0f;
            float nodeWidth = !Float.isNaN(size.width) ? size.width : 0f;
            float nodeHeight = !Float.isNaN(size.height) ? size.height : 0f;

            float absoluteX = cumulativeX + nodeX;
            float absoluteY = cumulativeY + nodeY;

            // Round the position in cumulative (absolute) coordinates, then convert back to a
            // parent-relative offset: round(abs) - round(parent_abs). Rounding the relative
            // offset directly made children of a parent at a fractional offset overlap or leave
            // 1px gaps (BK-13 / upstream #1189).
            float roundedX = round(absoluteX) - round(cumulativeX);
            float roundedY = round(absoluteY) - round(cumulativeY);

            // Round size based on accumulated absolute position
            float roundedWidth = round(absoluteX + nodeWidth) - round(absoluteX);
            float roundedHeight = round(absoluteY + nodeHeight) - round(absoluteY);

            FloatSize contentSize = layout.contentSize();
            float contentWidth = (contentSize != null && !Float.isNaN(contentSize.width)) ? contentSize.width : 0f;
            float contentHeight = (contentSize != null && !Float.isNaN(contentSize.height)) ? contentSize.height : 0f;
            float roundedContentWidth = round(absoluteX + contentWidth) - round(absoluteX);
            float roundedContentHeight = round(absoluteY + contentHeight) - round(absoluteY);

            FloatSize scrollbarSize = layout.scrollbarSize();
            float scrollbarWidth = (scrollbarSize != null && !Float.isNaN(scrollbarSize.width)) ? scrollbarSize.width : 0f;
            float scrollbarHeight = (scrollbarSize != null && !Float.isNaN(scrollbarSize.height)) ? scrollbarSize.height : 0f;
            FloatSize roundedScrollbarSize = new FloatSize(round(scrollbarWidth), round(scrollbarHeight));

            Layout roundedLayout = new Layout(
                layout.order(),
                new FloatPoint(roundedX, roundedY),
                new FloatSize(roundedWidth, roundedHeight),
                new FloatSize(roundedContentWidth, roundedContentHeight),
                roundOverflow(layout.scrollableOverflow()),
                roundedScrollbarSize,
                roundRect(layout.border()),
                roundRect(layout.padding()),
                roundRect(layout.margin())
            );

            Layout oldLayout = data.getFinalLayout();
            data.setFinalLayout(roundedLayout);
            if (useRounding) {
                markNodeLayoutUpdated(data, nodeKey, oldLayout, roundedLayout);
            }

            List<NodeId> childList = data.getChildren();
            int childCount = childList.size();
            int requiredCapacity = stackSize + childCount;
            if (requiredCapacity > nodeIds.length) {
                int newCapacity = Math.max(requiredCapacity, nodeIds.length * 2);
                nodeIds = Arrays.copyOf(nodeIds, newCapacity);
                cumulativeXs = Arrays.copyOf(cumulativeXs, newCapacity);
                cumulativeYs = Arrays.copyOf(cumulativeYs, newCapacity);
            }
            // Push in reverse so the first child is popped first (pre-order, same as the old
            // reverse-push loop over the child list).
            for (int i = childCount - 1; i >= 0; i--) {
                nodeIds[stackSize] = childList.get(i).getId();
                cumulativeXs[stackSize] = absoluteX;
                cumulativeYs[stackSize] = absoluteY;
                stackSize++;
            }
        }
    }

    /**
     * Round to nearest integer, with ties rounding away from zero (matches Rust's f32::round()).
     * This differs from Java's Math.round() which rounds ties towards positive infinity.
     */
    private static float round(float value) {
        if (value >= 0) {
            return (float) Math.floor(value + 0.5);
        } else {
            return (float) Math.ceil(value - 0.5);
        }
    }

    /** Rounds each scrollable-overflow edge, keeping the reversed-origin flags (SO-01). */
    private static ScrollableOverflow roundOverflow(ScrollableOverflow overflow) {
        return overflow == null ? ScrollableOverflow.ZERO : overflow.round();
    }

    /** Rounds a rect with {@link #round(float)}, mapping NaN edges to zero. */
    private static FloatRect roundRect(FloatRect rect) {
        return new FloatRect(
            !Float.isNaN(rect.left) ? round(rect.left) : 0f,
            !Float.isNaN(rect.right) ? round(rect.right) : 0f,
            !Float.isNaN(rect.top) ? round(rect.top) : 0f,
            !Float.isNaN(rect.bottom) ? round(rect.bottom) : 0f
        );
    }

    // === Utility Methods ===

    /**
     * Returns the node data for internal use.
     */
    NodeData getNodeData(NodeId node) {
        return nodes.get(node.getId());
    }

    /**
     * Checks if a node exists in the tree.
     */
    public boolean containsNode(NodeId node) {
        return nodes.containsKey(node.getId());
    }

    /**
     * Returns all node IDs in the tree.
     */
    public Set<NodeId> getAllNodes() {
        Set<NodeId> result = new HashSet<>();
        for (Long id : nodes.keySet()) {
            result.add(new NodeId(id));
        }
        return result;
    }

    /**
     * Writes a debug representation of the computed layout of a subtree to the given writer.
     *
     * <p>The output is depth-first, one line per node, with two spaces of indentation per level.
     * Each line carries the node's debug label followed by every {@link Layout} field:
     * {@code order}, {@code location}, {@code size}, {@code contentSize},
     * {@code scrollableOverflow}, {@code scrollbarSize}, {@code border}, {@code padding} and
     * {@code margin}. A node without a computed layout is written as {@code no layout}. An unknown
     * root node (or a node that does not exist) writes nothing at all.</p>
     *
     * <p>The writer is neither flushed nor closed: the caller owns it. The tree is not modified.</p>
     *
     * @param writer the destination to write to
     * @param root   the root of the subtree to write
     * @throws IOException          if the writer fails
     * @throws NullPointerException if {@code writer} is {@code null}
     */
    public void writeTree(Writer writer, NodeId root) throws IOException {
        Objects.requireNonNull(writer, "writer");
        writeTreeRecursive(writer, root, 0);
    }

    private void writeTreeRecursive(Writer writer, NodeId node, int depth) throws IOException {
        NodeData data = nodes.get(node.getId());
        if (data == null) return;

        StringBuilder line = new StringBuilder("  ".repeat(depth));
        line.append('[').append(getDebugLabel(node)).append("] ");

        Layout layout = getLayout(node);
        if (layout == null) {
            line.append("no layout");
        } else {
            appendLayout(line, layout);
        }
        line.append(System.lineSeparator());

        writer.write(line.toString());

        for (NodeId child : getChildren(node)) {
            writeTreeRecursive(writer, child, depth + 1);
        }
    }

    private static void appendLayout(StringBuilder line, Layout layout) {
        line.append("order=").append(layout.order())
            .append(" location=").append(layout.location())
            .append(" size=").append(layout.size())
            .append(" contentSize=").append(layout.contentSize())
            .append(" scrollableOverflow=").append(layout.scrollableOverflow())
            .append(" scrollbarSize=").append(layout.scrollbarSize())
            .append(" border=").append(layout.border())
            .append(" padding=").append(layout.padding())
            .append(" margin=").append(layout.margin());
    }

    /**
     * Prints a debug representation of the tree to {@link System#out}.
     *
     * <p>Delegates to {@link #writeTree(Writer, NodeId)} through a buffered writer and flushes it,
     * so the output is identical to writing to any other writer. {@code System.out} is never
     * closed and its signature is unchanged.</p>
     *
     * @param root the root of the subtree to print
     */
    public void printTree(NodeId root) {
        Writer out = new BufferedWriter(new OutputStreamWriter(System.out));
        try {
            writeTree(out, root);
            out.flush();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write tree debug representation to System.out", e);
        }
    }

    private String getDebugLabel(NodeId node) {
        NodeData data = nodes.get(node.getId());
        if (data == null) return "UNKNOWN";
        
        int numChildren = childCount(node);
        TaffyDisplay display = data.getStyle().getDisplay();
        
        if (display == TaffyDisplay.NONE) return "NONE";
        if (numChildren == 0) return "LEAF";
        
        switch (display) {
            case BLOCK: return "BLOCK";
            case FLOW_ROOT: return "FLOW-ROOT";
            case FLEX:
                FlexDirection dir = data.getStyle().getFlexDirection();
                if (dir == FlexDirection.ROW ||
                    dir == FlexDirection.ROW_REVERSE) {
                    return "FLEX ROW";
                }
                return "FLEX COL";
            case GRID: return "GRID";
            default: return display.toString();
        }
    }
}
