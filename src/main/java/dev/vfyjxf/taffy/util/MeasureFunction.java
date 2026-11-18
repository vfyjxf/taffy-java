package dev.vfyjxf.taffy.util;

import dev.vfyjxf.taffy.tree.LayoutOutput;

/**
 * Functional interface for measuring leaf nodes (e.g. text content).
 *
 * <p>This is the new 1.2 measure contract (S1, alignment item BL-09): unlike the legacy
 * {@link MeasureFunc}, a callback receives the full {@link MeasureContext} (layout inputs, style,
 * node id and generic node context) and returns a {@link LayoutOutput}, so it can report
 * baselines and scrollable overflow in addition to the measured size.</p>
 *
 * <p><b>1.2 status (S1, additive):</b> the interface is new in 1.2 and lives next to the legacy
 * {@link MeasureFunc}. Old callbacks keep working through {@link MeasureFuncAdapter}; a node can
 * carry one callback of either kind and both kinds are consulted by the leaf measure path. 2.0
 * removes the legacy interface.</p>
 *
 * <p><b>Return-value contract:</b> {@link LayoutOutput#size()} is interpreted as the node's
 * <em>content</em> size, exactly as the legacy {@link MeasureFunc} returned a content-box size.
 * The caller adds the leaf's padding, border and scrollbar gutter to obtain the final outer size,
 * and propagates the returned {@link LayoutOutput#baselines()} and
 * {@link LayoutOutput#scrollableOverflow()} to the final layout output unchanged. The remaining
 * components ({@code contentSize}, margins, {@code marginsCanCollapseThrough}) are ignored: the
 * leaf path recomputes them.</p>
 *
 * @see MeasureContext
 * @see MeasureFuncAdapter
 */
@FunctionalInterface
public interface MeasureFunction {

    /**
     * Measures the intrinsic content size of a node.
     *
     * @param context the measure arguments: content-box layout inputs, style, node id and node
     *                context
     * @return the measured content size plus optional baselines/scrollable overflow; must not be
     *         {@code null} (the layout call sites reject a null result with a
     *         {@link NullPointerException})
     */
    LayoutOutput measure(MeasureContext context);
}
