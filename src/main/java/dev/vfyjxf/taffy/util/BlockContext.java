package dev.vfyjxf.taffy.util;

import dev.vfyjxf.taffy.style.Clear;

import java.util.Objects;

/**
 * The per-block view of a {@link FloatContext} inside a block formatting context (BFC).
 *
 * <p>This is the Java skeleton of upstream taffy's {@code BlockContext} from
 * {@code src/compute/block.rs} (CSS floats, upstream #866 / {@code 25759da}). A context holds
 * the shared BFC {@link FloatContext}, the block's y-offset from the BFC root, its border-box
 * x-insets and its content-box x-insets. Every query accepts block-local coordinates, adds
 * {@link #yOffset()} / the x-insets to reach BFC-root coordinates, and subtracts them again
 * from values returned to the caller.</p>
 *
 * <p><b>1.2 scope.</b> This class is the additive type/query foundation for the FT-01 batch.
 * It deliberately does <em>not</em> implement upstream's {@code place_floated_box} wrapper and
 * {@code BlockComputer} does not create or use a {@code BlockContext} yet. Float placement,
 * the floated content height contribution and the block integration land in the following FT
 * batches; the query surface here is expected to stay source-compatible.</p>
 *
 * <p><b>Region coordinates.</b> {@link FloatContext} stores BFC regions in BFC-root
 * coordinates. {@link #addRegion(float, float, float, float)} takes block-local coordinates and
 * translates them on insertion; {@link #regionAt(float)} translates a stored region back to
 * block-local coordinates (insets are clamped at zero, because a root region may be less inset
 * than this block's border box).</p>
 */
public final class BlockContext {

    private final FloatContext floatContext;
    private final float yOffset;
    private final float leftInset;
    private final float rightInset;
    private float contentBoxLeftInset;
    private float contentBoxRightInset;
    private final boolean bfcRoot;

    private BlockContext(
        FloatContext floatContext,
        float yOffset,
        float leftInset,
        float rightInset,
        boolean bfcRoot
    ) {
        this.floatContext = floatContext;
        this.yOffset = yOffset;
        this.leftInset = leftInset;
        this.rightInset = rightInset;
        this.contentBoxLeftInset = leftInset;
        this.contentBoxRightInset = rightInset;
        this.bfcRoot = bfcRoot;
    }

    /**
     * Creates the root context of a BFC with zero offsets and insets.
     *
     * @param floatContext the BFC's float context, shared by all sub-contexts
     */
    public static BlockContext root(FloatContext floatContext) {
        return new BlockContext(Objects.requireNonNull(floatContext, "floatContext"),
                                0.0f, 0.0f, 0.0f, true);
    }

    /**
     * Creates the root context of a new BFC with a fresh {@link FloatContext}.
     */
    public static BlockContext root() {
        return root(new FloatContext());
    }

    /**
     * Creates a sub-context for a child block.
     *
     * <p>The child's y-offset is this block's offset plus {@code additionalYOffset}; its
     * border-box x-insets are accumulated from this block's insets. The content-box insets
     * start equal to the border-box insets and are refined by
     * {@link #applyContentBoxInset(float, float)}.</p>
     *
     * @param additionalYOffset the child's top edge relative to this block's top edge
     * @param leftInset         the child's left border-box inset relative to this block
     * @param rightInset        the child's right border-box inset relative to this block
     */
    public BlockContext subContext(float additionalYOffset, float leftInset, float rightInset) {
        return new BlockContext(
            floatContext,
            yOffset + additionalYOffset,
            this.leftInset + leftInset,
            this.rightInset + rightInset,
            false
        );
    }

    /**
     * Returns whether this context is the root of its BFC.
     */
    public boolean isBfcRoot() {
        return bfcRoot;
    }

    /**
     * Returns the y-offset of this block's top edge from the BFC root.
     */
    public float yOffset() {
        return yOffset;
    }

    /**
     * Returns the x-inset of this block's border box from the left edge of the BFC.
     */
    public float leftInset() {
        return leftInset;
    }

    /**
     * Returns the x-inset of this block's border box from the right edge of the BFC.
     */
    public float rightInset() {
        return rightInset;
    }

    /**
     * Returns the x-inset of this block's content box from the left edge of the BFC.
     */
    public float contentBoxLeftInset() {
        return contentBoxLeftInset;
    }

    /**
     * Returns the x-inset of this block's content box from the right edge of the BFC.
     */
    public float contentBoxRightInset() {
        return contentBoxRightInset;
    }

    /**
     * Sets the width of the shared BFC.
     *
     * <p>Sub-contexts should use {@link #subContext(float, float, float)} for geometry rather
     * than calling this method.</p>
     */
    public void setWidth(float availableWidth) {
        floatContext.setWidth(availableWidth);
    }

    /**
     * Sets the x-axis content-box insets of this block. These are the difference between the
     * border box and the content box (padding + border + scrollbar gutter), added to this
     * block's border-box insets to remain relative to the BFC root.
     */
    public void applyContentBoxInset(float left, float right) {
        this.contentBoxLeftInset = leftInset + left;
        this.contentBoxRightInset = rightInset + right;
    }

    /**
     * Returns whether the BFC contains any floats.
     */
    public boolean hasFloats() {
        return floatContext.hasFloats();
    }

    /**
     * Returns whether the BFC contains any floats that extend to or below the block-local
     * {@code minY}.
     */
    public boolean hasActiveFloats(float minY) {
        return floatContext.hasActiveFloats(minY + yOffset);
    }

    /**
     * Adds a BFC region using block-local coordinates and insets.
     *
     * <p>The region is translated to BFC-root coordinates before it is stored in the shared
     * {@link FloatContext}: {@code startY}/{@code endY} are offset by {@link #yOffset()}, and
     * the x-insets are offset by this block's border-box insets. Regions must be appended in
     * ascending order and must not overlap.</p>
     *
     * @param startY     the top of the region, relative to this block
     * @param endY       the bottom of the region, relative to this block
     * @param leftInset  the left float inset, relative to this block's border box
     * @param rightInset the right float inset, relative to this block's border box
     */
    public void addRegion(float startY, float endY, float leftInset, float rightInset) {
        floatContext.addRegion(new FloatContext.BfcRegion(
            startY + yOffset,
            endY + yOffset,
            this.leftInset + leftInset,
            this.rightInset + rightInset
        ));
    }

    /**
     * Returns the BFC region containing the block-local {@code y}, or {@code null}.
     *
     * <p>The returned region uses block-local coordinates. Its insets are clamped at zero
     * because a BFC-root region may take away less space than this block's border box; in that
     * case the region does not constrain this block on that side.</p>
     */
    public FloatContext.BfcRegion regionAt(float y) {
        FloatContext.BfcRegion region = floatContext.regionAt(y + yOffset);
        if (region == null) {
            return null;
        }
        return new FloatContext.BfcRegion(
            region.startY() - yOffset,
            region.endY() - yOffset,
            Math.max(0.0f, region.leftInset() - leftInset),
            Math.max(0.0f, region.rightInset() - rightInset)
        );
    }

    /**
     * Returns the bottom of the lowest float cleared by {@code clear}, in block-local
     * coordinates, or {@code null}.
     */
    public Float clearedThreshold(Clear clear) {
        Float threshold = floatContext.clearedThreshold(clear);
        return threshold == null ? null : threshold - yOffset;
    }

    /**
     * Searches for a content slot that avoids the BFC's floats, in block-local coordinates.
     *
     * <p>This delegates to {@link FloatContext#contentSlot(float, float, float, Clear)} with
     * the block's content-box insets and translates the result back to block-local
     * coordinates.</p>
     *
     * @param minY  the minimum y position, relative to this block
     * @param clear the box's {@code clear} value
     * @return the content slot, in block-local coordinates
     */
    public FloatContext.ContentSlot findContentSlot(float minY, Clear clear) {
        FloatContext.ContentSlot slot = floatContext.contentSlot(
            minY + yOffset,
            contentBoxLeftInset,
            contentBoxRightInset,
            clear
        );
        return new FloatContext.ContentSlot(
            slot.segmentId(),
            slot.x() - leftInset,
            slot.y() - yOffset,
            slot.width(),
            slot.height()
        );
    }
}
