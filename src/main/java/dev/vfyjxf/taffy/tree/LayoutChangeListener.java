package dev.vfyjxf.taffy.tree;

/**
 * Listener interface for receiving layout change notifications.
 * 
 * <p>This allows users to:
 * <ul>
 *   <li>Define their own "root node" concept beyond the tree's root</li>
 *   <li>Collect a dirty set for efficient incremental layout updates</li>
 *   <li>Implement custom layout change handling logic</li>
 * </ul>
 * 
 * <h2>Example: Collecting Dirty Nodes</h2>
 * <pre>{@code
 * Set<NodeId> dirtySet = new HashSet<>();
 * tree.setLayoutChangeListener((node, layout) -> {
 *     dirtySet.add(node);
 * });
 * 
 * tree.computeLayout(root, availableSpace);
 * 
 * // Now dirtySet contains all nodes whose layout changed
 * for (NodeId node : dirtySet) {
 *     // Process changed nodes
 * }
 * dirtySet.clear();
 * }</pre>
 * 
 * <h2>Example: Custom Root Tracking</h2>
 * <pre>{@code
 * NodeId myRoot = ...;
 * AtomicBoolean rootChanged = new AtomicBoolean(false);
 * 
 * tree.setLayoutChangeListener((node, layout) -> {
 *     if (isDescendantOf(node, myRoot)) {
 *         rootChanged.set(true);
 *     }
 * });
 * }</pre>
 */
@FunctionalInterface
public interface LayoutChangeListener {
    
    /**
     * Called when a node's layout has been updated.
     * 
     * <p>This is called during layout computation when the final layout
     * of a node is set. The listener is called synchronously during
     * {@link TaffyTree#computeLayout}.
     * 
     * <p><b>Note:</b> The layout passed may be the rounded or unrounded
     * layout depending on the tree's rounding configuration.
     * 
     * @param node the node whose layout changed
     * @param layout the new layout of the node (may be null if layout was cleared)
     */
    void onLayoutChanged(NodeId node, Layout layout);
}
