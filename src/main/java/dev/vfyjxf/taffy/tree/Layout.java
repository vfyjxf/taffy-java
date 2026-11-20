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
 *                           {@link #scrollableOverflow()}. After the P4 integration the scroll-size
 *                           getters only fall back to it for layouts built by hand through the
 *                           compatibility constructors.
 * @param scrollableOverflow The scrollable overflow rectangle of the node (T1/T2 additive component).
 *                           The block, flex and grid algorithms populate it (P4/SO-01/SO-02), the
 *                           measured-leaf path computes it, and {@code RoundLayout} rounds it.
 *                           {@link #scrollWidth()}/{@link #scrollHeight()} derive the reachable
 *                           scroll range from it; {@link ScrollableOverflow#ZERO} doubles as the
 *                           1.2 compatibility placeholder for hand-built layouts. 2.0 removes
 *                           {@code contentSize}.
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
     * <p>This is the magnitude of the reachable scroll range, as in upstream taffy #1372
     * (SO-01/SO-03). When the node carries a {@link #scrollableOverflow() scrollable overflow
     * rectangle} the range is derived from it rather than from the legacy
     * {@link #contentSize()} component:</p>
     * <ul>
     *   <li>a reversed x scroll origin (RTL) reports {@code -overflow.left()}: the content end is
     *       on the physical left, so the reachable range is how far it extends past the client
     *       area's left edge;</li>
     *   <li>a normal origin reports the content extent beyond the padding-box end edge, floored at
     *       zero: {@code max(0, overflow.right() + min(scrollbar.width, size.width) - size.width
     *       + border.left + border.right)}. The reachable extent is the content extent less the
     *       width of the padding box, so it must not depend on which edge a border sits on: both
     *       {@code border.left} and {@code border.right} are added (upstream #1007).</li>
     * </ul>
     *
     * <p><b>1.2 compatibility path:</b> a {@code Layout} built by hand through the compatibility
     * constructors (or {@link #Layout()}) carries the shared {@link ScrollableOverflow#ZERO}
     * placeholder, which this port treats as "no scrollable-overflow data". Such a value keeps the
     * legacy {@code contentSize}-based formula so that 1.x source and binaries observe the same
     * numbers; layouts produced by {@code computeLayout} always carry a freshly built rectangle
     * (possibly an empty one) and use the new model. 2.0 removes {@code contentSize} and this
     * fallback together.</p>
     */
    public float scrollWidth() {
        if (hasComputedScrollableOverflow()) {
            if (scrollableOverflow.reversedX()) {
                return -scrollableOverflow.left();
            }
            return Math.max(0f, scrollableOverflow.right()
                               + Math.min(scrollbarSize.width, size.width)
                               - size.width + border.left + border.right);
        }
        return Math.max(0, contentSize.width + Math.min(scrollbarSize.width, size.width)
                           - size.width + border.left + border.right);
    }

    /**
     * Return the maximum vertical scroll offset of the node.
     *
     * <p>See {@link #scrollWidth()} for the model: a reversed y scroll origin (a bottom-to-top
     * block flow, i.e. {@code column-reverse}/{@code wrap-reverse}) reports
     * {@code -overflow.top()}; a normal origin reports the content extent beyond the padding-box
     * bottom edge, floored at zero, adding both {@code border.top} and {@code border.bottom}
     * (upstream #1007/#1372). The legacy {@code contentSize}-based formula remains only on the
     * 1.2 compatibility path described by {@link #scrollWidth()}; 2.0 removes it.</p>
     */
    public float scrollHeight() {
        if (hasComputedScrollableOverflow()) {
            if (scrollableOverflow.reversedY()) {
                return -scrollableOverflow.top();
            }
            return Math.max(0f, scrollableOverflow.bottom()
                               + Math.min(scrollbarSize.height, size.height)
                               - size.height + border.top + border.bottom);
        }
        return Math.max(0, contentSize.height + Math.min(scrollbarSize.height, size.height)
                           - size.height + border.top + border.bottom);
    }

    /**
     * Whether this layout carries a computed scrollable-overflow rectangle rather than the 1.2
     * compatibility placeholder.
     *
     * <p>The placeholder is the shared {@link ScrollableOverflow#ZERO} constant, and that constant
     * is only ever installed by the compatibility constructors ({@link #Layout()},
     * {@link #Layout(int)}, {@link #withOrder(int)} and the 8-component constructor), so it is
     * recognised by identity. Every rectangle an algorithm produces — the block, flex and grid
     * accumulators, the measured-leaf path and the rounded layout — is a freshly constructed value,
     * even when its edges happen to be zero.</p>
     *
     * <p>An <em>empty</em> computed rectangle is a real result and must be honoured: a scroll
     * container whose content does not overflow has a zero range, so it must not fall back to the
     * legacy {@code contentSize} formula, which still adds the clipped scrollbar gutter and
     * reported a 2px range for a 2px-wide {@code overflow: scroll} box
     * ({@code grid_overflow_scrollbars_overridden_by_available_space}, upstream #1372 reports 0).</p>
     */
    private boolean hasComputedScrollableOverflow() {
        return scrollableOverflow != null && scrollableOverflow != ScrollableOverflow.ZERO;
    }

    // === Getter methods ===

    @Override
    public String toString() {
        return "Layout{order=" + order + ", location=" + location + ", size=" + size + "}";
    }
}
