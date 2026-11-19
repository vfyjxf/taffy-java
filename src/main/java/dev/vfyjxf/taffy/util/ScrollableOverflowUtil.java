package dev.vfyjxf.taffy.util;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.tree.ScrollableOverflow;

/**
 * Helpers for computing the CSS scrollable overflow rectangle (P4/SO-01/SO-02).
 *
 * <p>These are the shared pieces of upstream taffy #1118/#1372 ({@code compute/leaf.rs},
 * {@code compute/block.rs}, {@code compute/flexbox.rs}, {@code compute/grid}, {@code compute/oof.rs}
 * all use them): a per-child contribution in the parent's padding-box frame, a union accumulator,
 * the output boundary that clips unreachable overflow for scroll containers, and the leaf-local
 * rectangle. Keeping them in one place means the block, flex and grid algorithms cannot drift
 * apart on the hard parts (RTL symmetry, end padding and unreachable-region clipping).</p>
 *
 * <p><b>Coordinates.</b> A {@link ScrollableOverflow} rectangle is expressed in physical x/y
 * relative to the top-left edge of the node's client area (the padding box minus scrollbar
 * gutters). It contains the origin: its left/top edges are non-positive and its right/bottom edges
 * are non-negative. The {@code reversedX}/{@code reversedY} flags say at which physical edge the
 * scroll origin (the initial scroll offset) sits; they are not part of the geometry and RTL does
 * not mirror the coordinates. {@link #computeContribution} works in the parent's padding-box frame
 * instead: {@code location} is the child's border-box origin in that frame, and the returned
 * rectangle can be negative (it is clipped when the accumulator is finalized).</p>
 *
 * <p><b>Containment ({@code contain: layout/paint}) is not modelled yet</b>: this port has no
 * {@code contain} style property, so a box always propagates its overflow as if it were not
 * contained. When containment lands, {@link #computeContribution} must gain the equivalent of
 * upstream's {@code contain.contains_scrollable_overflow()} gate.</p>
 *
 * @see ScrollableOverflow
 */
public final class ScrollableOverflowUtil {

    private ScrollableOverflowUtil() {
    }

    /**
     * The container geometry that a child's scrollable overflow contribution is resolved against
     * (upstream's {@code ScrollableOverflowContainer}).
     *
     * <p>The values are defensively copied in the canonical constructor and returned as copies, so
     * a caller can never mutate the container description after it has been supplied.</p>
     *
     * @param parentIsScrollContainer whether the parent clips unreachable overflow and includes its
     *                                own end padding in the reachable region
     * @param parentSize              the extent of the parent's client area (border box less border
     *                                and scrollbar gutters), not of the border box
     * @param reversedX               whether the parent's physical x scroll origin is at its right edge
     * @param reversedY               whether the parent's physical y scroll origin is at its bottom edge
     * @param padding                 the parent's resolved padding; only the end side of a
     *                                scroll-container parent extends a contribution
     */
    public record Container(
        boolean parentIsScrollContainer,
        FloatSize parentSize,
        boolean reversedX,
        boolean reversedY,
        FloatRect padding
    ) {

        /**
         * Creates a container description, copying the supplied geometry.
         *
         * @param parentIsScrollContainer whether the parent is a scroll container
         * @param parentSize              the parent's client-area extent; {@code null} becomes zero
         * @param reversedX               whether the parent's x scroll origin is at its right edge
         * @param reversedY               whether the parent's y scroll origin is at its bottom edge
         * @param padding                 the parent's resolved padding; {@code null} becomes zero
         */
        public Container {
            parentSize = parentSize == null ? FloatSize.zero() : parentSize.copy();
            padding = padding == null ? FloatRect.zero() : padding.copy();
        }

        /**
         * The parent's client-area extent, as a defensive copy.
         *
         * @return a copy of the client-area size
         */
        @Override
        public FloatSize parentSize() {
            return parentSize.copy();
        }

        /**
         * The parent's resolved padding, as a defensive copy.
         *
         * @return a copy of the padding rectangle
         */
        @Override
        public FloatRect padding() {
            return padding.copy();
        }
    }

    /**
     * Returns whether either axis makes the node a scroll container ({@link Overflow#HIDDEN} or
     * {@link Overflow#SCROLL}).
     *
     * <p>A scroll container clips overflow in <em>both</em> axes and its own end padding counts
     * towards its scrollable overflow region, even when only one axis scrolls.</p>
     *
     * @param overflow the resolved overflow flags of the node
     * @return whether the node is a scroll container
     */
    public static boolean isScrollContainer(TaffyPoint<Overflow> overflow) {
        return overflow.x.isScrollContainer() || overflow.y.isScrollContainer();
    }

    /**
     * Computes the rectangle that one child contributes to its parent's scrollable overflow
     * rectangle (upstream's {@code compute_scrollable_overflow_contribution}).
     *
     * <p>The child's own scrollable overflow rectangle is relative to the child's client area, so
     * the child's border is added first to move it into the child's border-box frame. A
     * non-scroll-container child propagates its overflow on an axis only when that axis is
     * {@link Overflow#VISIBLE}; {@link Overflow#CLIP} clips its own axis without establishing a
     * scroll container, while any scroll container clips both axes and contributes only its border
     * box. The result is the union of the child's border box and the overflow it lets escape.</p>
     *
     * <p>For a scroll-container parent, a box positioned wholly in the unreachable region before
     * the scroll origin contributes nothing, and the parent's own end padding is included (the
     * start padding is already part of the child positions). In RTL the end padding is on the left;
     * a negative end margin shrinks the padding extension but never inverts it. Boxes that only
     * partially cross the scroll origin still contribute their reachable part; the unreachable part
     * is clipped by {@link #finalizeScrollableOverflow}.</p>
     *
     * <p>{@code location} and the result are in the parent's padding-box frame. {@code margin} is
     * the child's resolved margin: block layout passes only its non-positive components
     * ({@code margin.min(0)}), which is what lets a negative end margin cancel the end padding;
     * auto margins arrive as {@link Float#NaN} in this port and are treated as zero.</p>
     *
     * @param location               the child's border-box origin in the parent's padding-box frame
     * @param size                   the child's border-box size
     * @param scrollableOverflowRect the child's own scrollable overflow rectangle (relative to the
     *                               child's client area; use {@link ScrollableOverflow#rect()})
     * @param border                 the child's resolved border
     * @param overflow               the child's resolved overflow flags
     * @param container              the parent container description
     * @param margin                 the child's resolved margin (non-positive for block layout)
     * @return the contribution rectangle in the parent's padding-box frame, or a zero rectangle
     *         when the child contributes nothing
     */
    public static FloatRect computeContribution(
        FloatPoint location,
        FloatSize size,
        FloatRect scrollableOverflowRect,
        FloatRect border,
        TaffyPoint<Overflow> overflow,
        Container container,
        FloatRect margin
    ) {
        FloatRect childOverflow = scrollableOverflowRect == null ? FloatRect.zero() : scrollableOverflowRect;
        FloatRect childBorder = border == null ? FloatRect.zero() : border;
        FloatRect childMargin = margin == null ? FloatRect.zero() : margin;

        float overflowLeft = sanitize(childOverflow.left);
        float overflowRight = sanitize(childOverflow.right);
        float overflowTop = sanitize(childOverflow.top);
        float overflowBottom = sanitize(childOverflow.bottom);

        // Child overflow is relative to its inner border edge: shift it into border-box coordinates.
        float childLeft = overflowLeft + sanitize(childBorder.left);
        float childRight = overflowRight + sanitize(childBorder.left);
        float childTop = overflowTop + sanitize(childBorder.top);
        float childBottom = overflowBottom + sanitize(childBorder.top);

        boolean scrollContainer = isScrollContainer(overflow);
        boolean propagatesX = !scrollContainer && overflow.x == Overflow.VISIBLE;
        boolean propagatesY = !scrollContainer && overflow.y == Overflow.VISIBLE;

        float width = sanitize(size.width);
        float height = sanitize(size.height);

        // A child with no positive extent contributes nothing (upstream returns Rect::ZERO).
        float endExtentWidth = propagatesX ? Math.max(width, childRight) : width;
        float endExtentHeight = propagatesY ? Math.max(height, childBottom) : height;
        if (endExtentWidth <= 0f || endExtentHeight <= 0f) {
            return FloatRect.zero();
        }

        float startExtentX = propagatesX ? Math.min(0f, childLeft) : 0f;
        float startExtentY = propagatesY ? Math.min(0f, childTop) : 0f;

        float x = sanitize(location.x);
        float y = sanitize(location.y);
        FloatRect contribution = new FloatRect(
            x + startExtentX,
            x + endExtentWidth,
            y + startExtentY,
            y + endExtentHeight
        );

        if (!container.parentIsScrollContainer()) {
            return contribution;
        }

        FloatSize parentSize = container.parentSize();
        FloatRect padding = container.padding();
        float paddingLeft = sanitize(padding.left);
        float paddingRight = sanitize(padding.right);
        float paddingTop = sanitize(padding.top);
        float paddingBottom = sanitize(padding.bottom);
        float marginLeft = sanitize(childMargin.left);
        float marginRight = sanitize(childMargin.right);
        float marginTop = sanitize(childMargin.top);
        float marginBottom = sanitize(childMargin.bottom);

        // Boxes wholly in the unreachable region before/after the scroll origin are excluded
        // (https://www.w3.org/TR/css-overflow-3/#scrollable).
        boolean whollyUnreachable =
            (container.reversedX()
                 ? contribution.left >= sanitize(parentSize.width)
                 : contribution.right <= 0f)
            || (container.reversedY()
                    ? contribution.top >= sanitize(parentSize.height)
                    : contribution.bottom <= 0f);
        if (whollyUnreachable) {
            return FloatRect.zero();
        }

        // Only a scroll container includes its own end padding in the reachable region; RTL scrolls
        // towards the left, and a negative end margin can cancel the padding extension.
        if (container.reversedX()) {
            contribution.left = Math.min(contribution.left, x - Math.max(0f, paddingLeft + marginLeft));
        } else {
            contribution.right =
                Math.max(contribution.right, x + width + Math.max(0f, paddingRight + marginRight));
        }
        if (container.reversedY()) {
            contribution.top = Math.min(contribution.top, y - Math.max(0f, paddingTop + marginTop));
        } else {
            contribution.bottom =
                Math.max(contribution.bottom, y + height + Math.max(0f, paddingBottom + marginBottom));
        }
        return contribution;
    }

    /**
     * Merges two contribution rectangles into the smallest rectangle containing both.
     *
     * <p>Algorithms accumulate from {@link FloatRect#zero()}, so the result always contains the
     * origin and {@link #finalizeScrollableOverflow} can make the documented
     * {@link ScrollableOverflow} guarantees without further sanitising.</p>
     *
     * @param a a contribution rectangle
     * @param b another contribution rectangle
     * @return the union of both rectangles as a new rectangle
     */
    public static FloatRect union(FloatRect a, FloatRect b) {
        return new FloatRect(
            Math.min(a.left, b.left),
            Math.max(a.right, b.right),
            Math.min(a.top, b.top),
            Math.max(a.bottom, b.bottom)
        );
    }

    /**
     * Merges an arbitrary number of contribution rectangles (order independent).
     *
     * @param contributions the contribution rectangles to merge; may be empty
     * @return the union of every rectangle, or a zero rectangle when the input is empty
     */
    public static FloatRect unionAll(Iterable<FloatRect> contributions) {
        FloatRect result = FloatRect.zero();
        for (FloatRect contribution : contributions) {
            result = union(result, contribution);
        }
        return result;
    }

    /**
     * Turns an algorithm's accumulated contribution rectangle into the node's
     * {@link ScrollableOverflow} (upstream's {@code finalize_scrollable_overflow}).
     *
     * <p>All geometry stays unpacked until this boundary. For a scroll container the unreachable
     * region is clipped: on a normal axis the start is reset to zero, and on a reversed axis the
     * end is clamped to the client extent (the initial offset sits at that edge, so the region
     * before it cannot be scrolled to). The reversed flags are kept and describe the physical
     * scroll-origin edges. A non-scroll container keeps all of its overflow, for propagation and
     * hit testing, and has no reversed origins.</p>
     *
     * <p>The rectangle is unioned with the origin first, so the returned value contains
     * {@code (0, 0)} as {@link ScrollableOverflow} documents even when the caller's accumulator did
     * not start from zero.</p>
     *
     * @param accumulatedRect the union of the children's contributions
     * @param clientSize      the node's client-area extent (border box less border and scrollbar
     *                        gutters); negative or NaN extents are treated as zero
     * @param reversedX       whether the physical x scroll origin is at the node's right edge
     * @param reversedY       whether the physical y scroll origin is at the node's bottom edge
     * @param isScrollContainer whether the node is a scroll container
     * @return the finalized scrollable overflow
     */
    public static ScrollableOverflow finalizeScrollableOverflow(
        FloatRect accumulatedRect,
        FloatSize clientSize,
        boolean reversedX,
        boolean reversedY,
        boolean isScrollContainer
    ) {
        FloatRect rect = accumulatedRect == null ? FloatRect.zero() : accumulatedRect.copy();
        // Guarantee the documented origin containment regardless of the accumulator's start value.
        rect.left = Math.min(rect.left, 0f);
        rect.right = Math.max(rect.right, 0f);
        rect.top = Math.min(rect.top, 0f);
        rect.bottom = Math.max(rect.bottom, 0f);

        float width = Math.max(0f, sanitize(clientSize == null ? 0f : clientSize.width));
        float height = Math.max(0f, sanitize(clientSize == null ? 0f : clientSize.height));

        if (isScrollContainer) {
            if (reversedX) {
                rect.right = Math.min(rect.right, width);
            } else {
                rect.left = 0f;
            }
            if (reversedY) {
                rect.bottom = Math.min(rect.bottom, height);
            } else {
                rect.top = 0f;
            }
        } else {
            reversedX = false;
            reversedY = false;
        }

        return new ScrollableOverflow(rect, reversedX, reversedY);
    }

    /**
     * Computes a leaf node's scrollable overflow rectangle from its measured content size
     * (upstream's {@code compute/leaf.rs}).
     *
     * <p>The content starts after the node's start padding; the node's own end padding is part of
     * the scrollable overflow region only when the node is a scroll container (SO-05, #1117), and
     * in RTL the start/end sides are the physical right/left ones. In RTL the rectangle spans from
     * the client area's right edge back to the content end; on a scroll container it is clipped by
     * {@link #finalizeScrollableOverflow} the same way as a container's accumulated overflow.</p>
     *
     * <p>Scrollbar gutters are transposed exactly like the rest of the leaf path: a vertical
     * scrollbar ({@code overflow.y == SCROLL}) reserves horizontal space, and vice versa.</p>
     *
     * @param size                the leaf's final border-box size
     * @param border              the leaf's resolved border
     * @param padding             the leaf's resolved padding
     * @param scrollbarWidth      the scrollbar width from the style
     * @param overflow            the leaf's resolved overflow flags
     * @param measuredContentSize the content-box size reported by the measure callback (NaN values
     *                            are treated as zero)
     * @param rtl                 whether the leaf's resolved direction is right-to-left
     * @return the leaf's scrollable overflow
     */
    public static ScrollableOverflow leafScrollableOverflow(
        FloatSize size,
        FloatRect border,
        FloatRect padding,
        float scrollbarWidth,
        TaffyPoint<Overflow> overflow,
        FloatSize measuredContentSize,
        boolean rtl
    ) {
        FloatRect leafBorder = border == null ? FloatRect.zero() : border;
        FloatRect leafPadding = padding == null ? FloatRect.zero() : padding;
        FloatSize measured = measuredContentSize == null ? FloatSize.zero() : measuredContentSize;

        boolean scrollContainer = isScrollContainer(overflow);
        float gutterX = overflow.y == Overflow.SCROLL ? scrollbarWidth : 0f;
        float gutterY = overflow.x == Overflow.SCROLL ? scrollbarWidth : 0f;

        float boxWidth = sanitize(size.width);
        float boxHeight = sanitize(size.height);
        float clientWidth = Math.max(0f, boxWidth - leafBorder.horizontalAxisSum() - gutterX);
        float contentWidth = Math.max(0f, sanitize(measured.width));
        float contentHeight = Math.max(0f, sanitize(measured.height));

        float startPadding = rtl ? leafPadding.right : leafPadding.left;
        float endPadding = rtl ? leafPadding.left : leafPadding.right;
        float contentEnd = startPadding + contentWidth + (scrollContainer ? endPadding : 0f);

        // The bottom edge is physical in both directions (the block axis is top-to-bottom).
        float contentBottom = leafPadding.top + contentHeight + (scrollContainer ? leafPadding.bottom : 0f);

        float rectLeft = rtl ? Math.min(clientWidth - contentEnd, 0f) : 0f;
        float rectRight = rtl ? clientWidth : contentEnd;

        FloatSize clientSize = new FloatSize(
            boxWidth - leafBorder.horizontalAxisSum() - gutterX,
            boxHeight - leafBorder.verticalAxisSum() - gutterY
        );
        return finalizeScrollableOverflow(
            new FloatRect(rectLeft, rectRight, 0f, contentBottom),
            clientSize,
            rtl,
            false,
            scrollContainer
        );
    }

    private static float sanitize(float value) {
        return Float.isNaN(value) ? 0f : value;
    }
}
