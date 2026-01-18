package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatSize;

/**
 * A struct containing the result of laying a single node, which is returned up to the parent node.
 *
 * @param size                      The size of the node
 * @param contentSize               The size of the content within the node
 * @param firstBaselines            The first baseline of the node in each dimension, if any
 * @param topMargin                 Top margin that can be collapsed with. Used for CSS block layout margin collapsing.
 * @param bottomMargin              Bottom margin that can be collapsed with. Used for CSS block layout margin collapsing.
 * @param marginsCanCollapseThrough Whether margins can be collapsed through this node. Used for CSS block layout.
 */
public record LayoutOutput(
    FloatSize size,
    FloatSize contentSize,
    FloatPoint firstBaselines,
    CollapsibleMarginSet topMargin,
    CollapsibleMarginSet bottomMargin,
    boolean marginsCanCollapseThrough
) {

    /**
     * Create a new LayoutOutput
     */
    public LayoutOutput {
    }

    /**
     * An all-zero LayoutOutput for hidden nodes
     */
    public static final LayoutOutput HIDDEN = new LayoutOutput(
        FloatSize.zero(),
        FloatSize.zero(),
        new FloatPoint(Float.NaN, Float.NaN),
        CollapsibleMarginSet.ZERO,
        CollapsibleMarginSet.ZERO,
        false
    );

    /**
     * Static factory method for hidden layout output
     */
    public static LayoutOutput hidden() {
        return HIDDEN;
    }

    // === Getters ===

    /**
     * A blank layout output
     */
    public static final LayoutOutput DEFAULT = HIDDEN;

    /**
     * Constructor to create a LayoutOutput from just the size and baselines
     */
    public static LayoutOutput fromSizesAndBaselines(
        FloatSize size,
        FloatSize contentSize,
        FloatPoint firstBaselines
    ) {
        return new LayoutOutput(
            size,
            contentSize,
            firstBaselines,
            CollapsibleMarginSet.ZERO,
            CollapsibleMarginSet.ZERO,
            false
        );
    }

    /**
     * Construct a LayoutOutput from just the container and content sizes
     */
    public static LayoutOutput fromSizes(FloatSize size, FloatSize contentSize) {
        return fromSizesAndBaselines(size, contentSize, new FloatPoint(Float.NaN, Float.NaN));
    }

    /**
     * Construct a LayoutOutput from just the container's size
     */
    public static LayoutOutput fromOuterSize(FloatSize size) {
        return fromSizes(size, FloatSize.zero());
    }

    @Override
    public String toString() {
        return "LayoutOutput{size=" + size + ", contentSize=" + contentSize + "}";
    }
}
