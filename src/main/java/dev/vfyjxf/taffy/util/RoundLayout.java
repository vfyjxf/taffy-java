package dev.vfyjxf.taffy.util;

import dev.vfyjxf.taffy.geometry.FloatLine;
import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.ScrollableOverflow;
import dev.vfyjxf.taffy.tree.TaffyTree;

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
     *
     * <p>Delegates to {@link TaffyTree#roundLayout(NodeId)}, which owns the iterative
     * (stack-based, non-recursive) pass and reads each node's unrounded layout and child list from
     * a single {@code NodeData} lookup. Kept as the stable utility entry point; behavior is
     * unchanged.</p>
     */
    public static void roundLayout(TaffyTree tree, NodeId nodeId) {
        tree.roundLayout(nodeId);
    }

    /**
     * Rounds the layout recursively starting from the given node, treating the supplied
     * {@code cumulativeX}/{@code cumulativeY} as the node's parent offset.
     *
     * <p>Compatibility-only overload: its semantics differ from {@link #roundLayout(TaffyTree,
     * NodeId)} when the initial offset is non-zero, so it keeps the original recursive
     * implementation instead of delegating to the optimized pass.</p>
     *
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

        // Cumulative-coordinate rounding (see roundLayout: BK-13 / upstream #1189)
        float roundedX = round(absoluteX) - round(cumulativeX);
        float roundedY = round(absoluteY) - round(cumulativeY);
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

        tree.setLayout(nodeId, roundedLayout);

        for (NodeId childId : tree.getChildren(nodeId)) {
            roundLayoutRecursive(tree, childId, absoluteX, absoluteY);
        }
    }

    /**
     * Rounds each scrollable-overflow edge, keeping the reversed-origin flags.
     *
     * <p>The rectangle is carried through rounding instead of being reset: a rebuilt {@link Layout}
     * that used the compatibility constructor would silently drop the overflow computed by the leaf
     * path (SO-01). {@link ScrollableOverflow#round()} rounds ties away from zero like the rest of
     * this class and preserves the sign of {@code -0.0} edges.</p>
     */
    private static ScrollableOverflow roundOverflow(ScrollableOverflow overflow) {
        return overflow == null ? ScrollableOverflow.ZERO : overflow.round();
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
