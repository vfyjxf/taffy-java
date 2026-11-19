package dev.vfyjxf.taffy.util;

import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.Clear;
import dev.vfyjxf.taffy.style.TaffyDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The float-positioning state of one block formatting context (BFC).
 *
 * <p>This is the Java port of upstream taffy's {@code FloatContext} from
 * {@code src/compute/float.rs} (CSS floats, upstream #866 / {@code 25759da}). It records the
 * floated boxes of a BFC, the non-overlapping vertical segments that describe how much
 * horizontal space floats take away ({@link #regions()} exposes them as {@link BfcRegion}s),
 * and the running intrinsic-width accounting ({@link #sideSum(Direction) side sums} and
 * {@link #widest()}). {@link #placeFloatedBox(float, float, float, float, float, Direction, Clear)}
 * implements the CSS 2.2 float placement rules, while
 * {@link #clearedThreshold(Clear)} and {@link #contentSlot(float, float, float, Clear)} expose
 * the clear/content-slot queries that block layout will consume later.</p>
 *
 * <p><b>1.2 scope.</b> The placement algorithm and value types are additive foundations for
 * the FT-01 batch. {@code BlockComputer} does not reference them yet: block integration,
 * floated content height contribution and the block formatting-context root behaviour land in
 * the following FT batches. The public API here is expected to stay source-compatible.</p>
 *
 * <p><b>Coordinates.</b> A {@code FloatContext} stores positions in BFC-root coordinates.
 * {@link BlockContext} adds the per-block y-offset and x-inset translation on top.</p>
 *
 * @see <a href="https://www.w3.org/TR/CSS22/visuren.html#floats">CSS 2.2, Floats</a>
 */
public final class FloatContext {

    /**
     * Tolerance used when checking whether a box fits in a horizontal space, to absorb
     * {@code f32} rounding errors (e.g. percentage widths and margins that sum to exactly
     * 100% may otherwise exceed the container width and spuriously wrap).
     */
    public static final float FIT_TOLERANCE = 0.001f;

    /**
     * The {@link ContentSlot#segmentId() slot id} used when a result is not associated with a
     * segment. Upstream represents this as {@code None}.
     */
    public static final int NO_REGION = -1;

    /**
     * The side on which a floated box is placed.
     *
     * <p>The ordinal is the upstream side slot ({@code 0} = left, {@code 1} = right).</p>
     */
    public enum Direction {
        /** A left-floated box. */
        LEFT(0),
        /** A right-floated box. */
        RIGHT(1);

        private final int slot;

        Direction(int slot) {
            this.slot = slot;
        }

        /**
         * Returns the zero-based side slot used by the context's side-sum array
         * ({@code 0} for left, {@code 1} for right).
         */
        public int slot() {
            return slot;
        }

        /**
         * Returns the opposite side.
         */
        public Direction opposite() {
            return this == LEFT ? RIGHT : LEFT;
        }
    }

    /**
     * A floated box recorded in a {@link FloatContext}.
     *
     * <p>The field set mirrors upstream's {@code PlacedFloatedBox} in #866 ({@code width},
     * {@code height}, {@code x_inset}, {@code y}). A caller-defined id was added upstream later
     * (FT-06); this skeleton intentionally starts with the #866 shape.</p>
     *
     * @param width  the border-box width of the box
     * @param height the border-box height of the box
     * @param xInset the horizontal distance from the edge of the container that the box is
     *               floated towards (from the left for a left float, from the right for a right
     *               float)
     * @param y      the vertical distance from the top edge of the BFC container
     */
    public record PlacedFloatedBox(float width, float height, float xInset, float y) {
        /**
         * Validates that the box has a non-negative size.
         */
        public PlacedFloatedBox {
            if (width < 0.0f) {
                throw new IllegalArgumentException("width must be non-negative: " + width);
            }
            if (height < 0.0f) {
                throw new IllegalArgumentException("height must be non-negative: " + height);
            }
        }
    }

    /**
     * A non-overlapping horizontal segment of the BFC container.
     *
     * <p>A region covers the half-open vertical range {@code [startY, endY)}. Floats on each
     * side take away {@code leftInset}/{@code rightInset} from the region's full width, mirroring
     * upstream's private {@code Segment}. Regions are stored in ascending {@code startY} order
     * and must not overlap. The index of a region is its position in {@link #regions()}.</p>
     *
     * <p>Regions returned by {@link #regions()} after float placement may start at
     * {@link Float#NEGATIVE_INFINITY}: this is the empty segment above the first float, which
     * keeps content pulled up by a negative margin above that float (upstream #1304).</p>
     *
     * @param startY     the top of the region, in BFC-root coordinates
     * @param endY       the bottom of the region, in BFC-root coordinates
     * @param leftInset  the horizontal space taken by left floats
     * @param rightInset the horizontal space taken by right floats
     */
    public record BfcRegion(float startY, float endY, float leftInset, float rightInset) {
        /**
         * Validates the vertical range and the non-negative insets.
         */
        public BfcRegion {
            if (startY > endY) {
                throw new IllegalArgumentException(
                    "startY must not be greater than endY: " + startY + " > " + endY);
            }
            if (leftInset < 0.0f) {
                throw new IllegalArgumentException("leftInset must be non-negative: " + leftInset);
            }
            if (rightInset < 0.0f) {
                throw new IllegalArgumentException("rightInset must be non-negative: " + rightInset);
            }
        }

        /**
         * Returns whether {@code y} lies in this half-open region ({@code startY <= y < endY}).
         */
        public boolean containsY(float y) {
            return y >= startY && y < endY;
        }

        /**
         * Returns the vertical height of the region.
         */
        public float height() {
            return endY - startY;
        }

        /**
         * Returns the horizontal space left for content in a BFC of {@code bfcWidth}.
         */
        public float width(float bfcWidth) {
            return bfcWidth - leftInset - rightInset;
        }
    }

    /**
     * An empty slot that avoids floats, suitable for laying out non-floated content into.
     *
     * <p>This mirrors upstream's {@code ContentSlot}. {@code segmentId} is
     * {@link #NO_REGION} when the slot is not tied to a segment; the height is
     * {@link Float#POSITIVE_INFINITY} because a slot always extends down to the next float
     * boundary (upstream #866 returns an infinite height here).</p>
     *
     * @param segmentId the index of the segment the slot starts in, or {@link #NO_REGION}
     * @param x         the x position of the start of the slot, in BFC-root coordinates
     * @param y         the y position of the start of the slot, in BFC-root coordinates
     * @param width     the width of the slot
     * @param height    the height of the slot
     */
    public record ContentSlot(int segmentId, float x, float y, float width, float height) {
        /**
         * Returns whether this slot is associated with a segment.
         */
        public boolean hasRegion() {
            return segmentId != NO_REGION;
        }
    }

    /**
     * The result of placing a floated box.
     *
     * <p>{@code x}/{@code y} are the BFC-root coordinates of the box's border-box top-left
     * corner (for a right float {@code x} is {@code BFC width - x_inset - width}). {@code placed}
     * is {@code true} when the context positioned and recorded the box; valid boxes, including
     * zero-width and zero-height boxes, are always placed (zero-sized floats are recorded as
     * zero-height segments per upstream #1387). The flag keeps the result shape stable for any
     * future caller that filters out boxes before placement.</p>
     *
     * @param x      the x position of the box's top-left corner, in BFC-root coordinates
     * @param y      the y position of the box's top-left corner, in BFC-root coordinates
     * @param placed whether the box was positioned and recorded
     */
    public record FloatPlacement(float x, float y, boolean placed) {
    }

    private float availableWidth;
    private boolean hasFloats;
    private final List<PlacedFloatedBox> leftFloats = new ArrayList<>();
    private final List<PlacedFloatedBox> rightFloats = new ArrayList<>();
    private final List<Segment> segments = new ArrayList<>();
    private final float[] sideSums = new float[2];
    private float widest;
    /**
     * The segment-index range (start inclusive, end exclusive) that the last float on each side
     * was placed in. Used to skip segments that cannot be at or below a cleared float.
     */
    private final int[][] lastPlacedFloats = new int[2][2];
    /**
     * The topmost y position allowed for a new float (CSS2 float rule 5), including the tops of
     * zero-sized floats that occupy no segment. {@code null} before the first placement.
     */
    private Float floatCeiling;

    /**
     * Creates an empty float context with a zero width.
     */
    public FloatContext() {
    }

    /**
     * Sets the width of the BFC. Positions relative to the right edge (right floats and
     * right-floated slots) are resolved against this value.
     */
    public void setWidth(float availableWidth) {
        this.availableWidth = availableWidth;
    }

    /**
     * Returns the width of the BFC.
     */
    public float availableWidth() {
        return availableWidth;
    }

    /**
     * Returns whether any floated box has been recorded.
     */
    public boolean hasFloats() {
        return hasFloats;
    }

    /**
     * Returns whether any recorded float extends to or below {@code minY}.
     */
    public boolean hasActiveFloats(float minY) {
        return hasFloats && maxFloatBottom() > minY;
    }

    /**
     * Returns an unmodifiable view of the left-floated boxes, in record order.
     */
    public List<PlacedFloatedBox> leftFloats() {
        return Collections.unmodifiableList(leftFloats);
    }

    /**
     * Returns an unmodifiable view of the right-floated boxes, in record order.
     */
    public List<PlacedFloatedBox> rightFloats() {
        return Collections.unmodifiableList(rightFloats);
    }

    /**
     * Records a floated box and updates the intrinsic-width accounting.
     *
     * <p>The box is appended to the list for {@code direction}. The side sums follow the
     * upstream intrinsic-width rules: a {@code clear} value resets the side sum(s) it clears
     * before the box's width is added to its own side. {@link #widest()} is the maximum width
     * of any recorded float. This method only records intrinsic-width information; it does not
     * place the box or modify {@link #regions()}. Use
     * {@link #placeFloatedBox(float, float, float, float, float, Direction, Clear)} for
     * layout-affecting placement.</p>
     *
     * @param floatedBox the box to record
     * @param direction  the side the box is floated towards
     * @param clear      the box's {@code clear} value
     * @return {@code floatedBox}
     */
    public PlacedFloatedBox recordFloat(PlacedFloatedBox floatedBox, Direction direction, Clear clear) {
        Objects.requireNonNull(floatedBox, "floatedBox");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(clear, "clear");

        if (clear.clearsLeft()) {
            sideSums[Direction.LEFT.slot()] = 0.0f;
        }
        if (clear.clearsRight()) {
            sideSums[Direction.RIGHT.slot()] = 0.0f;
        }
        sideSums[direction.slot()] += floatedBox.width();
        widest = Math.max(widest, floatedBox.width());
        (direction == Direction.LEFT ? leftFloats : rightFloats).add(floatedBox);
        hasFloats = true;
        return floatedBox;
    }

    /**
     * Convenience overload of {@link #recordFloat(PlacedFloatedBox, Direction, Clear)}.
     */
    public PlacedFloatedBox recordFloat(
        float width, float height, float xInset, float y, Direction direction, Clear clear) {
        return recordFloat(new PlacedFloatedBox(width, height, xInset, y), direction, clear);
    }

    /**
     * Returns the accumulated intrinsic-width sum for one side.
     */
    public float sideSum(Direction direction) {
        return sideSums[direction.slot()];
    }

    /**
     * Returns a copy of the side sums as {@code [left, right]}.
     */
    public float[] sideSums() {
        return new float[] {sideSums[Direction.LEFT.slot()], sideSums[Direction.RIGHT.slot()]};
    }

    /**
     * Returns the width of the widest single recorded float (the min-content contribution of
     * the floats).
     */
    public float widest() {
        return widest;
    }

    /**
     * Positions a floated box in this BFC and records it.
     *
     * <p>The placement follows CSS 2.2 §9.5.1 and upstream taffy's
     * {@code place_floated_box_inner}:</p>
     * <ul>
     *   <li>a float may not be higher than any earlier float ({@code floatCeiling}) or than
     *       the bottom of a float it clears ({@code clear});</li>
     *   <li>the box is placed at the left/right BFC edge (or the containing block's
     *       {@code containingBlockLeftInset}/{@code containingBlockRightInset}) as far up as
     *       it fits horizontally beside existing floats;</li>
     *   <li>a float may overflow the far edge of the BFC when no float constrains it there
     *       (CSS2 rules 1 and 7), so a wide float is not clamped;</li>
     *   <li>the first float in a context is not clamped to {@code y = 0}: a containing block
     *       pulled up by a negative margin may place floats above the BFC top (upstream
     *       #1304);</li>
     *   <li>zero-width and zero-height floats are recorded, as their edges can constrain later
     *       floats and float-avoiding boxes (upstream #1056/#1062/#1387).</li>
     * </ul>
     *
     * @param width                   the border-box width of the float
     * @param height                  the border-box height of the float
     * @param minY                    the minimum y position, in BFC-root coordinates
     * @param containingBlockLeftInset the containing block's left inset from the BFC edge
     * @param containingBlockRightInset the containing block's right inset from the BFC edge
     * @param direction               the side the box is floated towards
     * @param clear                   the box's {@code clear} value
     * @return the position and placement flag
     */
    public FloatPlacement placeFloatedBox(
        float width,
        float height,
        float minY,
        float containingBlockLeftInset,
        float containingBlockRightInset,
        Direction direction,
        Clear clear
    ) {
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(clear, "clear");
        if (!(width >= 0.0f) || Float.isInfinite(width)) {
            throw new IllegalArgumentException("width must be finite and non-negative: " + width);
        }
        if (!(height >= 0.0f) || Float.isInfinite(height)) {
            throw new IllegalArgumentException("height must be finite and non-negative: " + height);
        }

        hasFloats = true;
        PlacedFloatedBox placed = placeFloatedBoxInner(
            width, height, minY, containingBlockLeftInset, containingBlockRightInset, direction, clear);

        int slot = direction.slot();
        (direction == Direction.LEFT ? leftFloats : rightFloats).add(placed);
        floatCeiling = floatCeiling == null ? placed.y() : Math.max(floatCeiling, placed.y());
        float x = direction == Direction.LEFT
                  ? placed.xInset()
                  : availableWidth - placed.xInset() - width;
        return new FloatPlacement(x, placed.y(), true);
    }

    /**
     * Adds a non-overlapping BFC region. Regions must be appended in ascending {@code startY}
     * order and must not overlap the previous region.
     *
     * <p>This is a manual seeding hook for the query API. Float placement creates and updates
     * its own segments; do not mix manual regions with {@link #placeFloatedBox} on the same
     * context. Manually added regions infer the "has a float on this side" flags from whether
     * the corresponding inset is greater than zero.</p>
     *
     * @throws IllegalArgumentException if {@code region.startY()} is above the previous
     *                                  region's {@code endY()}
     */
    public void addRegion(BfcRegion region) {
        Objects.requireNonNull(region, "region");
        if (!segments.isEmpty()) {
            Segment previous = segments.get(segments.size() - 1);
            if (region.startY() < previous.endY) {
                throw new IllegalArgumentException(
                    "regions must be non-overlapping and ordered: " + region + " starts before the previous region ends at "
                    + previous.endY);
            }
        }
        segments.add(new Segment(
            region.startY(),
            region.endY(),
            region.leftInset(),
            region.rightInset(),
            region.leftInset() > 0.0f,
            region.rightInset() > 0.0f
        ));
    }

    /**
     * Returns an unmodifiable view of the BFC regions, in segment order.
     *
     * <p>Regions are rebuilt from the internal segments on each call, so the returned list is a
     * snapshot; indices match {@link #regionIndexAt(float)} and the {@code segmentId} values used
     * by {@link ContentSlot} and {@code BfcSlot}.</p>
     */
    public List<BfcRegion> regions() {
        List<BfcRegion> regions = new ArrayList<>(segments.size());
        for (Segment segment : segments) {
            regions.add(segment.toRegion());
        }
        return Collections.unmodifiableList(regions);
    }

    /**
     * Returns the index of the region containing {@code y}, or {@link #NO_REGION}.
     */
    public int regionIndexAt(float y) {
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i).containsY(y)) {
                return i;
            }
        }
        return NO_REGION;
    }

    /**
     * Returns the region containing {@code y}, or {@code null} if no region contains it.
     */
    public BfcRegion regionAt(float y) {
        int index = regionIndexAt(y);
        return index == NO_REGION ? null : segments.get(index).toRegion();
    }

    /**
     * Returns the bottom edge of the lowest float on the side(s) cleared by {@code clear}, or
     * {@code null} when {@code clear} is {@link Clear#NONE} or the relevant side has no floats.
     *
     * <p>The values are derived from the placed float boxes' {@code y + height}, including
     * zero-sized floats that occupy no segment, matching upstream #1056's {@code clear_bottoms}
     * semantics.</p>
     */
    public Float clearedThreshold(Clear clear) {
        Objects.requireNonNull(clear, "clear");
        return switch (clear) {
            case NONE -> null;
            case LEFT -> maxFloatBottom(leftFloats);
            case RIGHT -> maxFloatBottom(rightFloats);
            case BOTH -> maxNullable(maxFloatBottom(leftFloats), maxFloatBottom(rightFloats));
        };
    }

    /**
     * Searches for a content slot that avoids the placed floats.
     *
     * <p>When no float is active at or below {@code minY}, the whole BFC width is returned
     * without a segment id. Otherwise the start is moved down to the {@code clear} threshold,
     * the first segment ending below that start is selected, and the slot spans from the
     * larger of the segment's insets and the caller's insets. The height is
     * {@link Float#POSITIVE_INFINITY} (upstream #866).</p>
     *
     * @param minY       the minimum y position, in BFC-root coordinates
     * @param leftInset  the caller's left inset from the BFC edge
     * @param rightInset the caller's right inset from the BFC edge
     * @param clear      the box's {@code clear} value
     * @return the content slot
     */
    public ContentSlot contentSlot(float minY, float leftInset, float rightInset, Clear clear) {
        // Compatibility form: a zero-height content slot. The height-aware overload below
        // implements upstream #1386.
        return contentSlot(minY, 0.0f, leftInset, rightInset, clear, NO_REGION);
    }

    /**
     * Searches for a content slot that avoids the floats beside the content's full height.
     *
     * <p>This is the height-aware form added by upstream #1386: the slot's insets are the union
     * of the insets of every segment the content's vertical extent touches, and
     * {@link ContentSlot#height()} is how far down from the slot's top its width stays valid
     * (content taller than that may be beside further floats). The search can be resumed after
     * a segment with {@code afterSegment}.</p>
     *
     * @param minY         the minimum y position, in BFC-root coordinates
     * @param height       the content height to keep clear of floats (0 for the legacy form)
     * @param leftInset    the caller's left inset from the BFC edge
     * @param rightInset   the caller's right inset from the BFC edge
     * @param clear        the box's {@code clear} value
     * @param afterSegment the segment after which the search starts, or {@link #NO_REGION}
     * @return the content slot
     */
    public ContentSlot contentSlot(
        float minY,
        float height,
        float leftInset,
        float rightInset,
        Clear clear,
        int afterSegment
    ) {
        Objects.requireNonNull(clear, "clear");
        if (afterSegment < NO_REGION) {
            throw new IllegalArgumentException("afterSegment must be >= " + NO_REGION + ": " + afterSegment);
        }

        if (!hasActiveFloats(minY)) {
            return new ContentSlot(
                NO_REGION, leftInset, minY,
                availableWidth - leftInset - rightInset, Float.POSITIVE_INFINITY);
        }

        float startY = minY;
        Float clearThreshold = clearedThreshold(clear);
        if (clearThreshold != null) {
            startY = Math.max(startY, clearThreshold);
        }

        int atLeast = afterSegment == NO_REGION ? 0 : afterSegment + 1;
        int cleared = clearedSegment(clear);
        int hwm = Math.max(atLeast, cleared == NO_REGION ? 0 : cleared + 1);

        int index = firstSegmentEndingAfter(hwm, startY);
        if (index >= segments.size()) {
            return new ContentSlot(
                NO_REGION, leftInset, startY,
                availableWidth - leftInset - rightInset, Float.POSITIVE_INFINITY);
        }

        Segment segment = segments.get(index);
        float y = Math.max(segment.startY, startY);
        float endY = y + height;
        float insetLeft = Math.max(segment.leftInset, leftInset);
        float insetRight = Math.max(segment.rightInset, rightInset);
        float slotHeight = Float.POSITIVE_INFINITY;
        for (int i = index + 1; i < segments.size(); i++) {
            Segment lower = segments.get(i);
            if (lower.startY < endY) {
                insetLeft = Math.max(insetLeft, lower.leftInset);
                insetRight = Math.max(insetRight, lower.rightInset);
            } else if (lower.leftInset > insetLeft || lower.rightInset > insetRight) {
                slotHeight = lower.startY - y;
                break;
            }
        }

        return new ContentSlot(
            index, insetLeft, y,
            availableWidth - insetLeft - insetRight, slotHeight);
    }

    /**
     * Searches for a slot suitable for laying out a box that establishes an independent
     * formatting context (whose border box must not overlap floats).
     *
     * <p>This ports upstream {@code find_bfc_slot} (#991, with the #1392 separate-inset
     * subtraction and the #1394 slot-height field). The box's margins are resolved against the
     * containing block's content edges, not the float edges, so they may overlap floats:</p>
     * <ul>
     *   <li>the leading margin (in flow direction) positions the border box's leading edge but
     *       may be partially or fully absorbed by a float on the leading side; a negative
     *       leading margin does not move the border edge past the float or containing-block
     *       edge;</li>
     *   <li>the trailing margin is subtracted from the width an auto width resolves to (except
     *       for the part that overlaps a float on the trailing side). When no float intrudes on
     *       the trailing side the margin applies as usual, so a positive margin may overflow
     *       the containing-block edge and a negative margin widens the border box.</li>
     * </ul>
     *
     * <p>The slot must not overlap floats beside any part of {@code minHeight} below its top
     * edge; {@link BfcSlot#height()} reports how far down the slot's widths stay valid. When no
     * float is active at or below {@code minY}, the slot uses the margin-inset containing-block
     * width and has no segment id.</p>
     *
     * <p>A BFC root's own bottom padding and border are not part of this query: upstream
     * #1384 adds them when the root's auto height is consumed. {@link BlockContext} exposes the
     * corresponding pure helper for the later BlockComputer batch.</p>
     *
     * @param minY                      the minimum y position, in BFC-root coordinates
     * @param minHeight                 a lower bound on the box's border-box height
     * @param containingBlockLeftInset  the containing block's left content-box inset from the BFC edge
     * @param containingBlockRightInset the containing block's right content-box inset from the BFC edge
     * @param marginLeft                the box's left margin (may be negative)
     * @param marginRight               the box's right margin (may be negative)
     * @param direction                 the inline flow direction (LTR/RTL); any other value is
     *                                  treated as LTR
     * @param clear                     the box's {@code clear} value
     * @param afterSegment              the segment after which the search starts, or
     *                                  {@link #NO_REGION}
     * @return the BFC slot, in BFC-root coordinates
     */
    public BfcSlot findBfcSlot(
        float minY,
        float minHeight,
        float containingBlockLeftInset,
        float containingBlockRightInset,
        float marginLeft,
        float marginRight,
        TaffyDirection direction,
        Clear clear,
        int afterSegment
    ) {
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(clear, "clear");
        if (afterSegment < NO_REGION) {
            throw new IllegalArgumentException("afterSegment must be >= " + NO_REGION + ": " + afterSegment);
        }

        float marginLeftInset = containingBlockLeftInset + marginLeft;
        float marginRightInset = containingBlockRightInset + marginRight;
        float noFloatWidth = availableWidth - marginLeftInset - marginRightInset;
        BfcSlot noFloatSlot = new BfcSlot(
            NO_REGION, marginLeftInset, minY,
            noFloatWidth, noFloatWidth, Float.POSITIVE_INFINITY);

        if (!hasActiveFloats(minY)) {
            return noFloatSlot;
        }

        // Clearance clears past the bottom of floats on the relevant side, including zero-sized
        // floats which occupy no segment.
        float effectiveMinY = minY;
        Float clearThreshold = clearedThreshold(clear);
        if (clearThreshold != null) {
            effectiveMinY = Math.max(effectiveMinY, clearThreshold);
        }

        int atLeast = afterSegment == NO_REGION ? 0 : afterSegment + 1;
        int cleared = clearedSegment(clear);
        int hwm = Math.max(atLeast, cleared == NO_REGION ? 0 : cleared + 1);

        int startIndex = firstSegmentEndingAfter(hwm, effectiveMinY);
        if (startIndex >= segments.size()) {
            // Below all floats.
            float y = segments.isEmpty()
                      ? effectiveMinY
                      : Math.max(segments.get(segments.size() - 1).endY, effectiveMinY);
            return new BfcSlot(
                NO_REGION, marginLeftInset, y,
                noFloatWidth, noFloatWidth, Float.POSITIVE_INFINITY);
        }

        Segment segment = segments.get(startIndex);
        float y = Math.max(segment.startY, effectiveMinY);

        // Combine the float insets of every segment beside the box's minimum height, then
        // extend the slot down over any further segments that do not narrow it.
        Segment combined = segment.copy();
        int next = startIndex + 1;
        while (next < segments.size() && segments.get(next).startY < y + minHeight) {
            combined.union(segments.get(next));
            next++;
        }
        float slotHeight = Float.POSITIVE_INFINITY;
        for (int i = next; i < segments.size(); i++) {
            if (segments.get(i).isNarrowerThan(combined)) {
                slotHeight = segments.get(i).startY - y;
                break;
            }
        }

        Direction lead = direction == TaffyDirection.RTL ? Direction.RIGHT : Direction.LEFT;
        Direction trail = lead.opposite();
        boolean hasLeadFloat = lead == Direction.LEFT ? combined.floatLeft : combined.floatRight;
        boolean hasTrailFloat = trail == Direction.LEFT ? combined.floatLeft : combined.floatRight;
        float leadInset = lead == Direction.LEFT ? combined.leftInset : combined.rightInset;
        float trailInset = trail == Direction.LEFT ? combined.leftInset : combined.rightInset;
        float cbLead = lead == Direction.LEFT ? containingBlockLeftInset : containingBlockRightInset;
        float cbTrail = trail == Direction.LEFT ? containingBlockLeftInset : containingBlockRightInset;
        float marginLead = lead == Direction.LEFT ? marginLeft : marginRight;
        float marginTrail = trail == Direction.LEFT ? marginLeft : marginRight;

        float fitLead = hasLeadFloat ? Math.max(leadInset, marginLead) : marginLead;
        float fitTrail = hasTrailFloat ? Math.max(trailInset, cbTrail) : Math.min(marginTrail, cbTrail);
        float stretchLead = fitLead;
        float stretchTrail = hasTrailFloat ? Math.max(trailInset, marginTrail) : marginTrail;

        float fitLeft = lead == Direction.LEFT ? fitLead : fitTrail;
        float fitRight = lead == Direction.LEFT ? fitTrail : fitLead;
        float stretchLeft = lead == Direction.LEFT ? stretchLead : stretchTrail;
        float stretchRight = lead == Direction.LEFT ? stretchTrail : stretchLead;

        return new BfcSlot(
            startIndex,
            fitLeft,
            y,
            availableWidth - fitLeft - fitRight,
            availableWidth - stretchLeft - stretchRight,
            slotHeight
        );
    }

    private int clearedSegment(Clear clear) {
        int leftEnd = lastPlacedFloats[Direction.LEFT.slot()][1];
        int rightEnd = lastPlacedFloats[Direction.RIGHT.slot()][1];
        return switch (clear) {
            case NONE -> NO_REGION;
            case LEFT -> leftEnd > 0 ? leftEnd : NO_REGION;
            case RIGHT -> rightEnd > 0 ? rightEnd : NO_REGION;
            case BOTH -> (leftEnd > 0 || rightEnd > 0) ? Math.max(leftEnd, rightEnd) : NO_REGION;
        };
    }

    // === Placement implementation ===

    private PlacedFloatedBox placeFloatedBoxInner(
        float width,
        float height,
        float minY,
        float containingBlockLeftInset,
        float containingBlockRightInset,
        Direction direction,
        Clear clear
    ) {
        int slot = direction.slot();
        float[] cbInsets = {containingBlockLeftInset, containingBlockRightInset};

        // Floats (including zero-sized floats) that occupy no segment still constrain the
        // position of later boxes: rule 5 (a float may not be higher than the top of any
        // earlier float) and clear (which clears past the bottom of floats on the relevant
        // side, regardless of their width).
        float effectiveMinY = minY;
        if (floatCeiling != null) {
            effectiveMinY = Math.max(effectiveMinY, floatCeiling);
        }
        Float clearThreshold = clearedThreshold(clear);
        if (clearThreshold != null) {
            effectiveMinY = Math.max(effectiveMinY, clearThreshold);
        }

        // Ensure that the float starts at or after the last placed float in either direction
        // (CSS2 rule 5: the outer top of a floating box may not be higher than the outer top
        // of any block or floated box generated by an element earlier in the source document).
        int floatStart = Math.max(lastPlacedFloats[0][0], lastPlacedFloats[1][0]);
        int hwm = switch (clear) {
            case LEFT -> Math.max(floatStart, lastPlacedFloats[0][1] + 1);
            case RIGHT -> Math.max(floatStart, lastPlacedFloats[1][1] + 1);
            case BOTH -> Math.max(floatStart, Math.max(lastPlacedFloats[0][1], lastPlacedFloats[1][1]) + 1);
            case NONE -> floatStart;
        };

        int startIdx = firstSegmentEndingAfter(hwm, effectiveMinY);
        int endIdx = startIdx;
        float startY = effectiveMinY;

        int start = NO_REGION;
        int end = NO_REGION;
        float placedInset = cbInsets[slot];

        outer:
        while (true) {
            if (startIdx >= segments.size()) {
                // No existing segment can accommodate the float: create a new segment below all
                // existing segments.
                start = NO_REGION;
                end = NO_REGION;
                placedInset = cbInsets[slot];
                break;
            }

            Segment startSegment = segments.get(startIdx);
            if (!startSegment.fitsFloatWidth(width, direction, availableWidth, cbInsets)) {
                startIdx++;
                endIdx = Math.max(endIdx, startIdx);
                continue;
            }

            startY = Math.max(startY, startSegment.startY);
            FloatFitter fitter = new FloatFitter(
                availableWidth, startSegment.endY - startY, cbInsets[0], cbInsets[1]);
            fitter.unionInsets(startSegment);

            while (true) {
                if (endIdx >= segments.size()) {
                    // The float extends below all existing segments.
                    start = startIdx;
                    end = NO_REGION;
                    placedInset = fitter.placedInset(direction);
                    break outer;
                }

                Segment endSegment = segments.get(endIdx);
                fitter.unionInsets(endSegment);
                if (!fitter.fitsHorizontally(width, direction)) {
                    startIdx++;
                    endIdx = Math.max(endIdx, startIdx);
                    continue outer;
                }

                if (endIdx != startIdx) {
                    fitter.addHeight(endSegment.endY - endSegment.startY);
                }
                if (!fitter.fitsVertically(height)) {
                    endIdx++;
                    continue;
                }

                start = startIdx;
                end = endIdx;
                placedInset = fitter.placedInset(direction);
                break outer;
            }
        }

        if (start == NO_REGION) {
            // The float is placed after all existing segments. Before the first float the
            // context is float-free all the way up, so an empty segment is inserted above the
            // float; this keeps content positioned above it (e.g. by a negative margin) from
            // being moved down to it. The float itself may have a negative y: its containing
            // block may be pulled up by a negative margin (CSS2 float rule 4, upstream #1304).
            float lastYEnd = segments.isEmpty()
                             ? Float.NEGATIVE_INFINITY
                             : segments.get(segments.size() - 1).endY;
            if (startY > lastYEnd) {
                segments.add(new Segment(lastYEnd, startY, 0.0f, 0.0f, false, false));
            }
            float segmentStartY = Math.max(lastYEnd, startY);

            Segment floatSegment = new Segment(
                segmentStartY, segmentStartY + height, cbInsets[0], cbInsets[1], false, false);
            floatSegment.setInset(direction, cbInsets[slot] + width);
            floatSegment.setHasFloat(direction, true);
            segments.add(floatSegment);

            int index = segments.size() - 1;
            updateLastPlacedFloat(direction, index, index + 1);
            return new PlacedFloatedBox(width, height, cbInsets[slot], segmentStartY);
        }

        int startSegmentIdx = start;
        if (startY != segments.get(startSegmentIdx).startY) {
            subdivideSegment(startSegmentIdx, startY);
            startSegmentIdx++;
            if (end != NO_REGION) {
                end++;
            }
        }

        // A zero-height float takes up no vertical space, but still narrows content and floats
        // whose vertical extent crosses its position, so it gets its own zero-height segment
        // (upstream #1387).
        if (height == 0.0f) {
            Segment segment = segments.get(startSegmentIdx);
            segments.add(startSegmentIdx, new Segment(
                startY, startY, segment.leftInset, segment.rightInset,
                segment.floatLeft, segment.floatRight));
            end = startSegmentIdx;
        }

        int endSegmentIdx;
        if (end == NO_REGION) {
            float lastYEnd = segments.get(segments.size() - 1).endY;
            float endY = startY + height;
            if (endY > lastYEnd) {
                segments.add(new Segment(lastYEnd, endY, cbInsets[0], cbInsets[1], false, false));
            }
            endSegmentIdx = segments.size() - 1;
        } else {
            endSegmentIdx = end;
            float endY = startY + height;
            while (endSegmentIdx > startSegmentIdx && endY <= segments.get(endSegmentIdx).startY) {
                endSegmentIdx--;
            }
            Segment endSegment = segments.get(endSegmentIdx);
            if (endSegment.startY < endY && endY < endSegment.endY) {
                subdivideSegment(endSegmentIdx, endY);
            }
        }

        float placedInsetPlusWidth = placedInset + width;
        for (int i = startSegmentIdx; i <= endSegmentIdx; i++) {
            Segment segment = segments.get(i);
            segment.setInset(direction, placedInsetPlusWidth);
            segment.setHasFloat(direction, true);
        }

        updateLastPlacedFloat(direction, startSegmentIdx, endSegmentIdx + 1);
        return new PlacedFloatedBox(width, height, placedInset, startY);
    }

    private void subdivideSegment(int index, float divideAtY) {
        Segment oldSegment = segments.get(index);
        if (!(divideAtY > oldSegment.startY && divideAtY < oldSegment.endY)) {
            throw new IllegalStateException(
                "cannot subdivide segment " + oldSegment + " at " + divideAtY);
        }
        Segment newSegment = new Segment(
            divideAtY, oldSegment.endY, oldSegment.leftInset, oldSegment.rightInset,
            oldSegment.floatLeft, oldSegment.floatRight);
        oldSegment.endY = divideAtY;
        segments.add(index + 1, newSegment);
    }

    private void updateLastPlacedFloat(Direction direction, int start, int endExclusive) {
        int slot = direction.slot();
        lastPlacedFloats[slot][0] = Math.max(lastPlacedFloats[slot][0], start);
        lastPlacedFloats[slot][1] = Math.max(lastPlacedFloats[slot][1], endExclusive);
    }

    private int firstSegmentEndingAfter(int fromIndex, float y) {
        for (int i = fromIndex; i < segments.size(); i++) {
            if (segments.get(i).endY > y) {
                return i;
            }
        }
        return segments.size();
    }

    private int firstSegmentEndingAfter(float y) {
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i).endY > y) {
                return i;
            }
        }
        return NO_REGION;
    }

    private static boolean floatFitsHorizontally(
        float width,
        Direction direction,
        float bfcWidth,
        float floatLeftInset,
        float floatRightInset,
        float containingBlockLeftInset,
        float containingBlockRightInset
    ) {
        Direction lead = direction;
        Direction trail = direction.opposite();
        float leadInset = lead == Direction.LEFT ? floatLeftInset : floatRightInset;
        float trailInset = trail == Direction.LEFT ? floatLeftInset : floatRightInset;
        float cbLeadInset = lead == Direction.LEFT ? containingBlockLeftInset : containingBlockRightInset;
        float cbTrailInset = trail == Direction.LEFT ? containingBlockLeftInset : containingBlockRightInset;

        float xInset = Math.max(leadInset, cbLeadInset);
        boolean fitsOppositeFloats =
            trailInset == 0.0f || xInset + width <= bfcWidth - trailInset + FIT_TOLERANCE;
        boolean fitsContainingBlock =
            leadInset == 0.0f || xInset + width <= bfcWidth - cbTrailInset + FIT_TOLERANCE;
        return fitsOppositeFloats && fitsContainingBlock;
    }

    private float maxFloatBottom() {
        Float bottom = maxNullable(maxFloatBottom(leftFloats), maxFloatBottom(rightFloats));
        return bottom == null ? 0.0f : bottom;
    }

    private static Float maxFloatBottom(List<PlacedFloatedBox> floats) {
        Float max = null;
        for (PlacedFloatedBox box : floats) {
            float bottom = box.y() + box.height();
            if (max == null || bottom > max) {
                max = bottom;
            }
        }
        return max;
    }

    private static Float maxNullable(Float left, Float right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return Math.max(left, right);
    }

    /**
     * Computes the intrinsic (content-based) width contribution of a set of floats.
     *
     * <p>This ports upstream's {@code FloatIntrinsicWidthCalculator} (#1055/#1150). It is used
     * only while sizing an auto-width block container: under a definite available width the
     * floats' max-content sides are clamped to that width but never below the widest single
     * float; under max-content they accumulate; under min-content only the widest float
     * contributes. {@code clear} resets the cleared side sums before the new float is added.</p>
     *
     * <p>This is linear float accounting only; it does not place boxes. Per the FT-03 scope it
     * feeds {@code BlockComputer.determineContentBasedContainerWidth}; no non-float result
     * changes.</p>
     */
    public static final class IntrinsicWidthCalculator {
        private final AvailableSpace availableWidth;
        private final float[] sideSums = new float[2];
        private float contribution;
        private float widest;

        /**
         * Creates a calculator for the container's available width.
         */
        public IntrinsicWidthCalculator(AvailableSpace availableWidth) {
            this.availableWidth = Objects.requireNonNull(availableWidth, "availableWidth");
        }

        /**
         * Adds one floated box's outer width (border box + horizontal margins).
         */
        public void addFloat(float width, Direction direction, Clear clear) {
            Objects.requireNonNull(direction, "direction");
            Objects.requireNonNull(clear, "clear");
            if (availableWidth.isMinContent()) {
                contribution = Math.max(contribution, width);
            } else {
                // Definite and max-content both accumulate adjacent floats on each side.
                if (clear.clearsLeft()) {
                    sideSums[Direction.LEFT.slot()] = 0.0f;
                }
                if (clear.clearsRight()) {
                    sideSums[Direction.RIGHT.slot()] = 0.0f;
                }
                sideSums[direction.slot()] += width;
                contribution = Math.max(contribution, sideSums[0] + sideSums[1]);
            }
            widest = Math.max(widest, width);
        }

        /**
         * Returns the accumulated side sum for one side.
         */
        public float sideSum(Direction direction) {
            return sideSums[direction.slot()];
        }

        /**
         * Returns the widest single float contributed so far.
         */
        public float widest() {
            return widest;
        }

        /**
         * Returns the computed intrinsic width contribution.
         */
        public float result() {
            if (availableWidth.isDefinite()) {
                return Math.max(Math.min(contribution, availableWidth.getValue()), widest);
            }
            return contribution;
        }
    }

    /**
     * A non-overlapping horizontal segment of the BFC container.
     *
     * <p>This is the internal counterpart of upstream's {@code Segment}. The public
     * {@link BfcRegion} omits the {@code hasFloat} flags because they are only meaningful for
     * the slot searches.</p>
     */
    private static final class Segment {
        private float startY;
        private float endY;
        private float leftInset;
        private float rightInset;
        private boolean floatLeft;
        private boolean floatRight;

        private Segment(
            float startY,
            float endY,
            float leftInset,
            float rightInset,
            boolean hasFloatLeft,
            boolean hasFloatRight
        ) {
            this.startY = startY;
            this.endY = endY;
            this.leftInset = leftInset;
            this.rightInset = rightInset;
            this.floatLeft = hasFloatLeft;
            this.floatRight = hasFloatRight;
        }

        private boolean containsY(float y) {
            return y >= startY && y < endY;
        }

        private Segment copy() {
            return new Segment(startY, endY, leftInset, rightInset, floatLeft, floatRight);
        }

        /**
         * Widens this segment's float insets and occupied sides to cover {@code other}'s.
         */
        private void union(Segment other) {
            leftInset = Math.max(leftInset, other.leftInset);
            rightInset = Math.max(rightInset, other.rightInset);
            floatLeft = floatLeft || other.floatLeft;
            floatRight = floatRight || other.floatRight;
        }

        /**
         * Whether this segment is narrower than {@code other} on either side: a larger inset,
         * or a float on a side where {@code other} has none. The latter counts even if the
         * inset does not grow, because {@link #findBfcSlot} only applies the insets on sides
         * that have a float.
         */
        private boolean isNarrowerThan(Segment other) {
            return leftInset > other.leftInset
                   || rightInset > other.rightInset
                   || (floatLeft && !other.floatLeft)
                   || (floatRight && !other.floatRight);
        }

        private BfcRegion toRegion() {
            return new BfcRegion(startY, endY, leftInset, rightInset);
        }

        private boolean fitsFloatWidth(
            float width,
            Direction direction,
            float bfcWidth,
            float[] containingBlockInsets
        ) {
            return floatFitsHorizontally(
                width, direction, bfcWidth, leftInset, rightInset,
                containingBlockInsets[0], containingBlockInsets[1]);
        }

        private void setInset(Direction direction, float value) {
            if (direction == Direction.LEFT) {
                leftInset = value;
            } else {
                rightInset = value;
            }
        }

        private void setHasFloat(Direction direction, boolean value) {
            if (direction == Direction.LEFT) {
                floatLeft = value;
            } else {
                floatRight = value;
            }
        }

        @Override
        public String toString() {
            return "Segment{" + startY + ".." + endY + ", left=" + leftInset + ", right=" + rightInset
                   + ", floatLeft=" + floatLeft + ", floatRight=" + floatRight + '}';
        }
    }

    /**
     * Helper type for placing a single floated box: given a pinned starting y position, this
     * type helps determine whether there is any x position such that there is sufficient
     * horizontal space for the box across its entire height.
     */
    private static final class FloatFitter {
        private final float bfcWidth;
        private double slotHeight;
        private final float[] floatInsets = new float[2];
        private final float[] cbInsets = new float[2];

        private FloatFitter(float bfcWidth, float slotHeight, float cbLeftInset, float cbRightInset) {
            this.bfcWidth = bfcWidth;
            this.slotHeight = slotHeight;
            this.cbInsets[0] = cbLeftInset;
            this.cbInsets[1] = cbRightInset;
        }

        private void unionInsets(Segment segment) {
            floatInsets[0] = Math.max(floatInsets[0], segment.leftInset);
            floatInsets[1] = Math.max(floatInsets[1], segment.rightInset);
        }

        private float placedInset(Direction direction) {
            return Math.max(floatInsets[direction.slot()], cbInsets[direction.slot()]);
        }

        private boolean fitsHorizontally(float width, Direction direction) {
            return floatFitsHorizontally(
                width, direction, bfcWidth, floatInsets[0], floatInsets[1], cbInsets[0], cbInsets[1]);
        }

        private void addHeight(float height) {
            slotHeight += height;
        }

        private boolean fitsVertically(float height) {
            return slotHeight >= height;
        }
    }
}
