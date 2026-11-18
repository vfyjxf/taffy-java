package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatSize;

/**
 * A struct containing the result of laying a single node, which is returned up to the parent node.
 *
 * @param size                      The size of the node
 * @param contentSize               The size of the content within the node. Legacy 1.x component kept
 *                                  for compatibility; 2.0 removes it in favour of
 *                                  {@link #scrollableOverflow()}.
 * @param scrollableOverflow        The scrollable overflow rectangle of the node (T1/T2 additive
 *                                  component). Always {@link ScrollableOverflow#ZERO} in 1.2: the
 *                                  layout algorithms do not populate it yet (P4/SO-01).
 * @param firstBaselines            The first baseline of the node in each dimension, if any.
 *                                  Legacy 1.x component kept for compatibility; 2.0 removes it in
 *                                  favour of {@link #baselines()}.
 * @param baselines                 The first and last baselines of the node (T1/T2 additive component).
 *                                  The compatibility constructors derive {@code first} from
 *                                  {@code firstBaselines} and leave {@code last} absent; no algorithm
 *                                  computes baselines yet (P3/CR-07).
 * @param topMargin                 Top margin that can be collapsed with. Used for CSS block layout margin collapsing.
 * @param bottomMargin              Bottom margin that can be collapsed with. Used for CSS block layout margin collapsing.
 * @param marginsCanCollapseThrough Whether margins can be collapsed through this node. Used for CSS block layout.
 */
public record LayoutOutput(
    FloatSize size,
    FloatSize contentSize,
    ScrollableOverflow scrollableOverflow,
    FloatPoint firstBaselines,
    Baselines baselines,
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
     * Compatibility constructor matching the 1.1.x output contract.
     *
     * <p>This is an explicit overload (not the canonical constructor) that keeps old source code and
     * binaries working while {@link #scrollableOverflow()} and {@link #baselines()} are additive. It
     * delegates to the canonical constructor with {@link ScrollableOverflow#ZERO} and derives
     * {@code baselines.first} from {@code firstBaselines} ({@code baselines.last} stays absent).
     * 2.0 removes the legacy {@code contentSize}/{@code firstBaselines} components, this overload and
     * the legacy accessors together.</p>
     *
     * @param size                      the size of the node
     * @param contentSize               the size of the content within the node
     * @param firstBaselines            the first baseline of the node in each dimension, if any
     * @param topMargin                 top margin that can be collapsed with
     * @param bottomMargin              bottom margin that can be collapsed with
     * @param marginsCanCollapseThrough whether margins can be collapsed through this node
     */
    public LayoutOutput(
        FloatSize size,
        FloatSize contentSize,
        FloatPoint firstBaselines,
        CollapsibleMarginSet topMargin,
        CollapsibleMarginSet bottomMargin,
        boolean marginsCanCollapseThrough
    ) {
        this(size, contentSize, ScrollableOverflow.ZERO, firstBaselines,
             Baselines.fromFirst(firstBaselines), topMargin, bottomMargin, marginsCanCollapseThrough);
    }

    /**
     * An all-zero LayoutOutput for hidden nodes
     */
    public static final LayoutOutput HIDDEN = new LayoutOutput(
        FloatSize.zero(),
        FloatSize.zero(),
        ScrollableOverflow.ZERO,
        new FloatPoint(Float.NaN, Float.NaN),
        Baselines.NONE,
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
     * Swap the horizontal and vertical axes of the output (inverse of
     * {@link LayoutInput#transpose()}).
     *
     * <p>The geometry is carried through the transposition: {@link #size()},
     * {@link #contentSize()} and {@link #scrollableOverflow()} swap their axes, and the legacy
     * {@link #firstBaselines()} point as well as both {@link #baselines()} points swap
     * {@code x} and {@code y}. The Java baseline type carries both axes, so the baselines are
     * transposed instead of dropped (upstream only computes the block-axis baseline).</p>
     *
     * <p>Collapsible margins and the collapse-through flag are block-axis quantities that have no
     * meaning on the parent's inline axis, so {@link #topMargin()} and {@link #bottomMargin()} are
     * reset to {@link CollapsibleMarginSet#ZERO} and {@link #marginsCanCollapseThrough()} to
     * {@code false}, exactly like upstream taffy #1265. They do not survive a transposition.</p>
     *
     * <p>The receiver is never modified; the returned output is a new record instance.</p>
     *
     * @return a new transposed output
     */
    public LayoutOutput transpose() {
        return new LayoutOutput(
            new FloatSize(size.height, size.width),
            new FloatSize(contentSize.height, contentSize.width),
            scrollableOverflow.transpose(),
            firstBaselines.transpose(),
            new Baselines(baselines.first().transpose(), baselines.last().transpose()),
            CollapsibleMarginSet.ZERO,
            CollapsibleMarginSet.ZERO,
            false
        );
    }

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
            ScrollableOverflow.ZERO,
            firstBaselines,
            Baselines.fromFirst(firstBaselines),
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
