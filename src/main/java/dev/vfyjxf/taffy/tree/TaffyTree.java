package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.util.MeasureFunc;
import dev.vfyjxf.taffy.util.RoundLayout;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

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

    /** Context data (measure functions) storage by node ID */
    private final Long2ObjectOpenHashMap<MeasureFunc> nodeContextData;

    /** Children of each node */
    private final Long2ObjectOpenHashMap<List<NodeId>> children;

    /** Parent of each node */
    private final Long2ObjectOpenHashMap<NodeId> parents;

    /** Whether to round layout values */
    private boolean useRounding = true;

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
        this.nodeContextData = new Long2ObjectOpenHashMap<>(capacity);
        this.children = new Long2ObjectOpenHashMap<>(capacity);
        this.parents = new Long2ObjectOpenHashMap<>(capacity);
    }

    // === Configuration ===

    /**
     * Enable rounding of layout values. Rounding is enabled by default.
     */
    public void enableRounding() {
        this.useRounding = true;
    }

    /**
     * Disable rounding of layout values.
     */
    public void disableRounding() {
        this.useRounding = false;
    }

    /**
     * Returns whether rounding is enabled.
     */
    public boolean roundingEnabled() {
        return useRounding;
    }

    // === Node Creation ===

    /**
     * Creates and adds a new unattached leaf node to the tree.
     */
    public NodeId newLeaf(TaffyStyle style) {
        long id = nodeIdCounter.getAndIncrement();
        NodeId nodeId = new NodeId(id);
        
        nodes.put(id, new NodeData(style));
        children.put(id, new ArrayList<>());
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
        nodes.put(id, data);
        nodeContextData.put(id, measureFunc);
        children.put(id, new ArrayList<>());
        parents.put(id, null);
        
        return nodeId;
    }

    /**
     * Creates and adds a new node with children.
     */
    public NodeId newWithChildren(TaffyStyle style, NodeId... childNodes) {
        long id = nodeIdCounter.getAndIncrement();
        NodeId nodeId = new NodeId(id);
        
        nodes.put(id, new NodeData(style));
        
        List<NodeId> childList = new ArrayList<>(childNodes.length);
        for (NodeId child : childNodes) {
            parents.put(child.getId(), nodeId);
            childList.add(child);
        }
        
        children.put(id, childList);
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
        nodeContextData.clear();
        children.clear();
        parents.clear();
    }

    /**
     * Remove a specific node from the tree.
     */
    public void remove(NodeId node) {
        long key = node.getId();
        
        // Remove from parent's children list
        NodeId parent = parents.get(key);
        if (parent != null) {
            List<NodeId> parentChildren = children.get(parent.getId());
            if (parentChildren != null) {
                parentChildren.removeIf(n -> n.equals(node));
            }
        }
        
        // Remove parent references from this node's children
        List<NodeId> nodeChildren = children.get(key);
        if (nodeChildren != null) {
            for (NodeId child : nodeChildren) {
                parents.put(child.getId(), null);
            }
        }
        
        children.remove(key);
        parents.remove(key);
        nodes.remove(key);
        nodeContextData.remove(key);
    }

    // === Context (Measure Function) Management ===

    /**
     * Sets the measure function for a node.
     */
    public void setMeasureFunc(NodeId node, MeasureFunc measureFunc) {
        long key = node.getId();
        NodeData data = nodes.get(key);
        if (data == null) {
            throw TaffyException.invalidInputNode(node);
        }
        
        if (measureFunc != null) {
            data.setHasContext(true);
            nodeContextData.put(key, measureFunc);
        } else {
            data.setHasContext(false);
            nodeContextData.remove(key);
        }
        
        markDirty(node);
    }

    /**
     * Gets the measure function for a node.
     */
    public MeasureFunc getMeasureFunc(NodeId node) {
        return nodeContextData.get(node.getId());
    }

    // === Child Management ===

    /**
     * Adds a child node under the parent.
     */
    public void addChild(NodeId parent, NodeId child) {
        long parentKey = parent.getId();
        long childKey = child.getId();
        
        if (!nodes.containsKey(parentKey)) {
            throw TaffyException.invalidParentNode(parent);
        }
        if (!nodes.containsKey(childKey)) {
            throw TaffyException.invalidChildNode(child);
        }
        
        parents.put(childKey, parent);
        children.get(parentKey).add(child);
        markDirty(parent);
    }

    /**
     * Inserts a child at the given index.
     */
    public void insertChildAtIndex(NodeId parent, int childIndex, NodeId child) {
        long parentKey = parent.getId();
        List<NodeId> parentChildren = children.get(parentKey);
        
        if (parentChildren == null) {
            throw TaffyException.invalidParentNode(parent);
        }
        
        int childCount = parentChildren.size();
        if (childIndex > childCount) {
            throw TaffyException.childIndexOutOfBounds(parent, childIndex, childCount);
        }
        
        parents.put(child.getId(), parent);
        parentChildren.add(childIndex, child);
        markDirty(parent);
    }

    /**
     * Sets the children of a node, replacing existing children.
     */
    public void setChildren(NodeId parent, NodeId... newChildren) {
        long parentKey = parent.getId();
        List<NodeId> parentChildList = children.get(parentKey);
        
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
        markDirty(parent);
    }

    /**
     * Removes a child from a parent.
     */
    public void removeChild(NodeId parent, NodeId child) {
        List<NodeId> parentChildren = children.get(parent.getId());
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
        List<NodeId> parentChildren = children.get(parentKey);
        
        if (parentChildren == null) {
            throw TaffyException.invalidParentNode(parent);
        }
        
        int childCount = parentChildren.size();
        if (childIndex >= childCount) {
            throw TaffyException.childIndexOutOfBounds(parent, childIndex, childCount);
        }
        
        NodeId child = parentChildren.remove(childIndex);
        parents.put(child.getId(), null);
        markDirty(parent);
        
        return child;
    }

    /**
     * Gets the child at the given index.
     */
    public NodeId getChildAtIndex(NodeId parent, int childIndex) {
        long parentKey = parent.getId();
        List<NodeId> parentChildren = children.get(parentKey);
        
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
        List<NodeId> parentChildren = children.get(parentKey);
        
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
        markDirty(parent);
        
        return oldChild;
    }

    // === Getters ===

    /**
     * Returns the number of children of a node.
     */
    public int childCount(NodeId parent) {
        List<NodeId> parentChildren = children.get(parent.getId());
        return parentChildren != null ? parentChildren.size() : 0;
    }

    /**
     * Returns an unmodifiable list of children.
     */
    public List<NodeId> getChildren(NodeId parent) {
        List<NodeId> parentChildren = children.get(parent.getId());
        return parentChildren != null ? parentChildren : Collections.emptyList();
    }

    /**
     * Returns the children list directly without creating a wrapper.
     * Only for internal use where the caller guarantees not to modify the list.
     */
    List<NodeId> getChildrenInternal(NodeId parent) {
        return children.get(parent.getId());
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
        return data.hasNewLayout();
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
        return data.needsVisit();
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
        data.clearDirtyDescendant();
    }

    /**
     * Marks a node as having a new layout and propagates dirty flag up to ancestors.
     */
    private void markNodeLayoutUpdated(NodeId node) {
        NodeData data = nodes.get(node.getId());
        if (data == null) return;
        
        data.markNewLayout();
        
        // Propagate dirty descendant flag up to ancestors
        NodeId parent = parents.get(node.getId());
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
            data.setFinalLayout(layout);
            // When rounding is enabled, mark after setting final layout
            if (useRounding) {
                markNodeLayoutUpdated(node);
            }
        }
    }

    /**
     * Sets the unrounded layout of a node.
     */
    public void setUnroundedLayout(NodeId node, Layout layout) {
        NodeData data = nodes.get(node.getId());
        if (data != null) {
            data.setUnroundedLayout(layout);
            // When rounding is disabled, mark after setting unrounded layout
            if (!useRounding) {
                markNodeLayoutUpdated(node);
            }
        }
    }

    // === Cache Management ===

    /**
     * Gets the cache entry for a node.
     */
    public LayoutOutput getCacheEntry(NodeId node, FloatSize knownDimensions,
                                      TaffySize<AvailableSpace> availableSpace, RunMode runMode) {
        NodeData data = nodes.get(node.getId());
        if (data == null) return null;
        return data.getCache().get(knownDimensions, availableSpace, runMode);
    }

    /**
     * Stores a cache entry for a node.
     */
    public void storeCacheEntry(NodeId node, FloatSize knownDimensions,
                                TaffySize<AvailableSpace> availableSpace, RunMode runMode,
                                LayoutOutput output) {
        NodeData data = nodes.get(node.getId());
        if (data != null) {
            data.getCache().store(knownDimensions, availableSpace, runMode, output);
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
        computeLayoutWithMeasure(rootNode, availableSpace, null);
    }

    /**
     * Computes the layout with a custom measure function for all nodes.
     */
    public void computeLayoutWithMeasure(NodeId rootNode, TaffySize<AvailableSpace> availableSpace,
                                          MeasureFunc defaultMeasureFunc) {
        // This will be implemented by the compute module
        // For now, delegate to the LayoutComputer
        LayoutComputer computer = new LayoutComputer(this, defaultMeasureFunc);
        computer.computeLayout(rootNode, availableSpace);
        
        // Round layouts if enabled
        if (useRounding) {
            RoundLayout.roundLayout(this, rootNode);
        }
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
     * Prints a debug representation of the tree.
     */
    public void printTree(NodeId root) {
        printTreeRecursive(root, 0);
    }

    private void printTreeRecursive(NodeId node, int depth) {
        NodeData data = nodes.get(node.getId());
        if (data == null) return;
        
        String indent = "  ".repeat(depth);
        Layout layout = getLayout(node);
        
        System.out.printf("%s[%s] %s%n", indent, getDebugLabel(node), 
            layout != null ? layout.toString() : "no layout");
        
        for (NodeId child : getChildren(node)) {
            printTreeRecursive(child, depth + 1);
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
