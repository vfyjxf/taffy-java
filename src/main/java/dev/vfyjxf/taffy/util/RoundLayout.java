package dev.vfyjxf.taffy.util;

import dev.vfyjxf.taffy.geometry.FloatLine;
import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Utility class for rounding layout values to pixel boundaries.
 * This prevents anti-aliased "blurry" rendering that can occur with fractional pixel values.
 */
public final class RoundLayout {

    private RoundLayout() {
    }
    
    /**
     * Round to nearest integer, with ties rounding away from zero (matches Rust's f32::round()).
     * This differs from Java's Math.round() which rounds ties towards positive infinity.
     * Example: round(-148.5) returns -149 (Rust) vs -148 (Java Math.round)
     */
    private static float round(float value) {
        // Math.rint rounds to nearest even for ties, we need round away from zero
        // For positive: 0.5 -> 1 (same as Math.round)
        // For negative: -0.5 -> -1 (different from Math.round which gives 0)
        if (value >= 0) {
            return (float) Math.floor(value + 0.5);
        } else {
            return (float) Math.ceil(value - 0.5);
        }
    }

    /**
     * Rounds the calculated layout to whole pixels.
     * Uses iterative BFS to avoid deep recursion stack overflow.
     */
    public static void roundLayout(TaffyTree tree, NodeId nodeId) {
        // Use a stack-based approach instead of recursion for better performance
        // Store: [nodeId, cumulativeX, cumulativeY]
        Deque<Object[]> stack = new ArrayDeque<>();
        stack.push(new Object[]{nodeId, 0f, 0f});
        
        while (!stack.isEmpty()) {
            Object[] item = stack.pop();
            NodeId currentNode = (NodeId) item[0];
            float cumulativeX = (Float) item[1];
            float cumulativeY = (Float) item[2];
            
            Layout layout = tree.getUnroundedLayout(currentNode);
            if (layout == null) continue;

            FloatPoint location = layout.location();
            FloatSize size = layout.size();

            float nodeX = !Float.isNaN(location.x) ? location.x : 0f;
            float nodeY = !Float.isNaN(location.y) ? location.y : 0f;
            float nodeWidth = !Float.isNaN(size.width) ? size.width : 0f;
            float nodeHeight = !Float.isNaN(size.height) ? size.height : 0f;

            float absoluteX = cumulativeX + nodeX;
            float absoluteY = cumulativeY + nodeY;

            // Round the position
            float roundedX = round(nodeX);
            float roundedY = round(nodeY);

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

            // Apply rounding to layout
            Layout roundedLayout = new Layout(
                layout.order(),
                new FloatPoint(roundedX, roundedY),
                new FloatSize(roundedWidth, roundedHeight),
                new FloatSize(roundedContentWidth, roundedContentHeight),
                roundedScrollbarSize,
                roundRect(layout.border()),
                roundRect(layout.padding()),
                roundRect(layout.margin())
            );

            tree.setLayout(currentNode, roundedLayout);

            // Push children to stack
            List<NodeId> childList = tree.getChildren(currentNode);
            // Process in reverse order to maintain original order when popping
            for (int i = childList.size() - 1; i >= 0; i--) {
                stack.push(new Object[]{childList.get(i), absoluteX, absoluteY});
            }
        }
    }

    /**
     * Rounds the layout recursively starting from the given node.
     * @deprecated Use {@link #roundLayout(TaffyTree, NodeId)} instead.
     */
    @Deprecated
    public static void roundLayout(TaffyTree tree, NodeId nodeId, float cumulativeX, float cumulativeY) {
        // Delegate to iterative version
        roundLayoutRecursive(tree, nodeId, cumulativeX, cumulativeY);
    }
    
    private static void roundLayoutRecursive(TaffyTree tree, NodeId nodeId, float cumulativeX, float cumulativeY) {
        Layout layout = tree.getUnroundedLayout(nodeId);
        if (layout == null) return;

        FloatPoint location = layout.location();
        FloatSize size = layout.size();

        float nodeX = !Float.isNaN(location.x) ? location.x : 0f;
        float nodeY = !Float.isNaN(location.y) ? location.y : 0f;
        float nodeWidth = !Float.isNaN(size.width) ? size.width : 0f;
        float nodeHeight = !Float.isNaN(size.height) ? size.height : 0f;

        float absoluteX = cumulativeX + nodeX;
        float absoluteY = cumulativeY + nodeY;

        float roundedX = round(nodeX);
        float roundedY = round(nodeY);
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
            roundedScrollbarSize,
            roundRect(layout.border()),
            roundRect(layout.padding()),
            roundRect(layout.margin())
        );

        tree.setLayout(nodeId, roundedLayout);

        for (NodeId childId : tree.getChildren(nodeId)) {
            roundLayoutRecursive(tree, childId, absoluteX, absoluteY);
        }
    }

    /**
     * Rounds a Rect's values using our round() that matches Rust's behavior.
     */
    private static FloatRect roundRect(FloatRect rect) {
        return new FloatRect(
            !Float.isNaN(rect.left) ? round(rect.left) : 0f,
            !Float.isNaN(rect.right) ? round(rect.right) : 0f,
            !Float.isNaN(rect.top) ? round(rect.top) : 0f,
            !Float.isNaN(rect.bottom) ? round(rect.bottom) : 0f
        );
    }

    /**
     * Rounds a Line's values using our round() that matches Rust's behavior.
     */
    private static FloatLine roundLine(FloatLine line) {
        return new FloatLine(
            !Float.isNaN(line.start) ? round(line.start) : 0f,
            !Float.isNaN(line.end) ? round(line.end) : 0f
        );
    }
}
