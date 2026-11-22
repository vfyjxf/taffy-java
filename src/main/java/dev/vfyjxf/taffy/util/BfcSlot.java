package dev.vfyjxf.taffy.util;

/**
 * An empty slot that avoids floats, suitable for laying out a box that establishes an
 * independent formatting context (a BFC root) into.
 *
 * <p>This is the Java port of upstream taffy's {@code BfcSlot} introduced by #991. Unlike
 * {@link FloatContext.ContentSlot}, a BFC slot accounts for the box's own margins: margins are
 * resolved against the containing block's edges (not the float edges) and may therefore overlap
 * floats. The slot exposes both the space available for the border box and the width that an
 * {@code auto} width resolves to ({@link #stretchWidth()}).</p>
 *
 * <p>{@code x}/{@code y} are the top-left corner of the slot in the coordinate space of the
 * {@link FloatContext} that produced it (BFC-root coordinates). {@link BlockContext} translates
 * them back to block-local coordinates.</p>
 *
 * @param segmentId    the index of the segment the slot starts in, or
 *                     {@link FloatContext#NO_REGION}
 * @param x            the x position of the start of the slot (border-box edge)
 * @param y            the y position of the start of the slot
 * @param borderWidth  the space available for the box's border box: the space between float
 *                     edges and the margin-inset containing-block edges. The box fits in the
 *                     slot if its border-box width does not exceed this.
 * @param stretchWidth the width that an auto width resolves to (before applying min/max
 *                     constraints and the negative-margin lower bound). This differs from
 *                     {@link #borderWidth()} in that the trailing margin is subtracted (except
 *                     for the part that overlaps a float).
 * @param height       how far down from the slot's top its widths stay valid: a box taller
 *                     than this may be beside further floats (upstream #1394).
 */
public record BfcSlot(int segmentId, float x, float y, float borderWidth, float stretchWidth, float height) {

    /**
     * The {@link #segmentId()} used when the slot is not associated with a segment.
     */
    public static final int NO_SEGMENT = FloatContext.NO_REGION;

    /**
     * Returns whether this slot is associated with a segment.
     */
    public boolean hasSegment() {
        return segmentId != NO_SEGMENT;
    }
}
