package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;

/**
 * The final result of a layout algorithm for a single node.
 *
 * @param order              The relative ordering of the node. Nodes with a higher order should be rendered on top.
 * @param location           The top-left corner of the node
 * @param size               The width and height of the node
 * @param contentSize        The width and height of the content inside the node.
 *                           This may be larger than the size of the node in the case of overflowing content.
 *                           Legacy 1.x component kept for compatibility; 2.0 removes it in favour of
 *                           {@link #scrollableOverflow()}.
 * @param scrollableOverflow The scrollable overflow rectangle of the node (T1/T2 additive component).
 *                           The leaf path computes it in P4 (SO-01/SO-02) and {@code RoundLayout}
 *                           rounds it; a block, flex or grid layout built by an algorithm still
 *                           carries {@link ScrollableOverflow#ZERO} until that algorithm's SO-01
 *                           wiring lands. {@link #scrollWidth()}/{@link #scrollHeight()} still derive
 *                           from {@code contentSize} until then; 2.0 removes {@code contentSize}.
 * @param scrollbarSize      The size of the scrollbars in each dimension. If there is no scrollbar then the size will be zero.
 * @param border             The size of the borders of the node
 * @param padding            The size of the padding of the node
 * @param margin             The size of the margin of the node
 */
public record Layout(
    int order,
    FloatPoint location,
    FloatSize size,
    FloatSize contentSize,
    ScrollableOverflow scrollableOverflow,
    FloatSize scrollbarSize,
    FloatRect border,
    FloatRect padding,
    FloatRect margin
) {

    /**
     * Creates a new zero-Layout
     */
    public Layout() {
        this(0, FloatPoint.zero(), FloatSize.zero(), FloatSize.zero(), ScrollableOverflow.ZERO,
             FloatSize.zero(), FloatRect.zero(), FloatRect.zero(), FloatRect.zero());
    }

    /**
     * Creates a new zero-Layout with the supplied order value
     */
    public Layout(int order) {
        this(order, FloatPoint.zero(), FloatSize.zero(), FloatSize.zero(), ScrollableOverflow.ZERO,
             FloatSize.zero(), FloatRect.zero(), FloatRect.zero(), FloatRect.zero());
    }

    /**
     * Compatibility constructor matching the 1.1.x layout contract.
     *
     * <p>This is an explicit overload (not the canonical constructor) that exists so that old
     * source code and binaries keep compiling and linking while the new
     * {@link ScrollableOverflow} component is additive. It delegates to the canonical constructor
     * with {@link ScrollableOverflow#ZERO}. 2.0 removes the legacy {@code contentSize} component,
     * this overload and the legacy accessor together.</p>
     *
     * @param order         the relative ordering of the node
     * @param location      the top-left corner of the node
     * @param size          the width and height of the node
     * @param contentSize   the width and height of the content inside the node
     * @param scrollbarSize the size of the scrollbars in each dimension
     * @param border        the size of the borders of the node
     * @param padding       the size of the padding of the node
     * @param margin        the size of the margin of the node
     */
    public Layout(
        int order,
        FloatPoint location,
        FloatSize size,
        FloatSize contentSize,
        FloatSize scrollbarSize,
        FloatRect border,
        FloatRect padding,
        FloatRect margin
    ) {
        this(order, location, size, contentSize, ScrollableOverflow.ZERO,
             scrollbarSize, border, padding, margin);
    }

    /**
     * Creates a new Layout with the given order
     */
    public static Layout withOrder(int order) {
        return new Layout(order);
    }

    /**
     * Copy constructor
     */
    public Layout copy() {
        return new Layout(
            order,
            location.copy(),
            size.copy(),
            contentSize.copy(),
            scrollableOverflow.copy(),
            scrollbarSize.copy(),
            border.copy(),
            padding.copy(),
            margin.copy()
        );
    }

    /**
     * Get the width of the node's content box
     */
    public float contentBoxWidth() {
        return size.width - padding.left - padding.right - border.left - border.right;
    }

    /**
     * Get the height of the node's content box
     */
    public float contentBoxHeight() {
        return size.height - padding.top - padding.bottom - border.top - border.bottom;
    }

    /**
     * Get the size of the node's content box
     */
    public FloatSize contentBoxSize() {
        return new FloatSize(contentBoxWidth(), contentBoxHeight());
    }

    /**
     * Get x offset of the node's content box relative to its parent's border box
     */
    public float contentBoxX() {
        return location.x + border.left + padding.left;
    }

    /**
     * Get y offset of the node's content box relative to its parent's border box
     */
    public float contentBoxY() {
        return location.y + border.top + padding.top;
    }

    /**
     * Return the maximum horizontal scroll offset of the node.
     *
     * <p>The reachable extent of the content is the content extent less the width of the padding
     * box, floored at zero, so it must not depend on which edge a border sits on: both
     * {@code border.left} and {@code border.right} are added (P4/SO-03, upstream #1007).</p>
     *
     * <p><b>1.2 status:</b> still the legacy {@link #contentSize()} formula. The scrollable-overflow
     * rectangle already existing in {@link #scrollableOverflow()} replaces it once the block, flex
     * and grid algorithms populate that rectangle (SO-01); the reversed scroll origins are only
     * expressible through that rectangle, so the two models are not mixed here.</p>
     */
    public float scrollWidth() {
        return Math.max(0, contentSize.width + Math.min(scrollbarSize.width, size.width)
                           - size.width + border.left + border.right);
    }

    /**
     * Return the maximum vertical scroll offset of the node.
     *
     * <p>The reachable extent of the content is the content extent less the height of the padding
     * box, floored at zero, so it must not depend on which edge a border sits on: both
     * {@code border.top} and {@code border.bottom} are added (P4/SO-03, upstream #1007).</p>
     *
     * <p><b>1.2 status:</b> still the legacy {@link #contentSize()} formula; see
     * {@link #scrollWidth()} for the pending scrollable-overflow switch.</p>
     */
    public float scrollHeight() {
        return Math.max(0, contentSize.height + Math.min(scrollbarSize.height, size.height)
                           - size.height + border.top + border.bottom);
    }

    // === Getter methods ===

    @Override
    public String toString() {
        return "Layout{order=" + order + ", location=" + location + ", size=" + size + "}";
    }
}
