package dev.vfyjxf.taffy.util;

import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.LayoutInput;
import dev.vfyjxf.taffy.tree.NodeId;

/**
 * The arguments passed to a {@link MeasureFunction} when a leaf node is measured.
 *
 * <p>This is the Java equivalent of the upstream (#1091, BL-09/S1) measure callback arguments
 * {@code (LayoutInput, NodeId, Option<&mut Ctx>, &Style)}: everything the callback needs in order
 * to compute a content size is bundled into one object, so the functional interface stays a single
 * abstract method and old-style {@link MeasureFunc} lambdas can be adapted with
 * {@link MeasureFuncAdapter}.</p>
 *
 * <p><b>1.2 status (S1, additive):</b> the context is built by
 * {@code LayoutComputer} at the two leaf measure call sites. The {@code input} is a
 * <em>measure-specific derived input</em>, not the raw {@code LayoutInput} that the surrounding
 * layout pass received: its known dimensions are content-box dimensions (the node's padding,
 * border and scrollbar gutter already subtracted). On the main leaf path its available space is
 * the computed content-box available space; on the early-return collapse-through probe the
 * available space is the layout input's available space and the known height is {@code NaN},
 * matching the legacy probe. Those are exactly the arguments the legacy {@link MeasureFunc} used
 * to receive, so adapting an old callback through {@link MeasureFuncAdapter} forwards identical
 * arguments. {@code style}, {@code nodeId} and {@code nodeContext} are the additional information
 * that the legacy callback could not see.</p>
 *
 * @param input       the measure-specific derived layout inputs (see the class description); the
 *                    known dimensions already exclude the node's padding, border and scrollbar
 *                    gutter
 * @param style       the resolved style of the node being measured (never {@code null})
 * @param nodeId      the id of the node being measured (never {@code null})
 * @param nodeContext the generic per-node context set with
 *                    {@code TaffyTree.setNodeContext(NodeId, Object)}, or {@code null} when none
 *                    was set
 */
public record MeasureContext(
    LayoutInput input,
    TaffyStyle style,
    NodeId nodeId,
    Object nodeContext
) {
}
