package dev.vfyjxf.taffy.util;

import dev.vfyjxf.taffy.style.Clear;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The float-positioning state of one block formatting context (BFC).
 *
 * <p>This is the Java skeleton of upstream taffy's {@code FloatContext} from
 * {@code src/compute/float.rs} (CSS floats, upstream #866 / {@code 25759da}). It records the
 * floated boxes of a BFC, the non-overlapping vertical {@link BfcRegion regions} that describe
 * how much horizontal space floats take away, and the running intrinsic-width accounting
 * ({@link #sideSum(Direction) side sums} and {@link #widest()}). The pure queries
 * {@link #clearedThreshold(Clear)} and {@link #contentSlot(float, float, float, Clear)} expose
 * the clear/content-slot semantics that block layout will consume later.</p>
 *
 * <p><b>1.2 scope.</b> This class is the additive type/query foundation for the FT-01 batch.
 * It deliberately does <em>not</em> implement upstream's float placement algorithm
 * ({@code place_floated_box}, segment subdivision, {@code find_content_slot} with an
 * {@code after} cursor) or the intrinsic-width calculator wrapper, and {@code BlockComputer}
 * does not reference it yet. Placement and block integration land in the following FT
 * batches; the value types and queries here are expected to stay source-compatible.</p>
 *
 * <p><b>Coordinates.</b> A {@code FloatContext} stores positions in BFC-root coordinates.
 * {@link BlockContext} adds the per-block y-offset and x-inset translation on top.</p>
 *
 * @see <a href="https://www.w3.org/TR/CSS22/visuren.html#floats">CSS 2.2, Floats</a>
 */
public final class FloatContext {

    /**
     * The {@link ContentSlot#segmentId() segment id} used when a content slot is not associated
     * with a {@link BfcRegion}. Upstream represents this as {@code None}.
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
     * and must not overlap.</p>
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
     * {@link #NO_REGION} when the slot is not tied to a {@link BfcRegion}; the height is
     * {@link Float#POSITIVE_INFINITY} because a slot always extends down to the next float
     * boundary (upstream #866 returns an infinite height here).</p>
     *
     * @param segmentId the index of the {@link BfcRegion} the slot starts in, or
     *                  {@link #NO_REGION}
     * @param x         the x position of the start of the slot, in BFC-root coordinates
     * @param y         the y position of the start of the slot, in BFC-root coordinates
     * @param width     the width of the slot
     * @param height    the height of the slot
     */
    public record ContentSlot(int segmentId, float x, float y, float width, float height) {
        /**
         * Returns whether this slot is associated with a {@link BfcRegion}.
         */
        public boolean hasRegion() {
            return segmentId != NO_REGION;
        }
    }

    private float availableWidth;
    private boolean hasFloats;
    private final List<PlacedFloatedBox> leftFloats = new ArrayList<>();
    private final List<PlacedFloatedBox> rightFloats = new ArrayList<>();
    private final List<BfcRegion> regions = new ArrayList<>();
    private final float[] sideSums = new float[2];
    private float widest;

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
     * of any recorded float. This method only records; it does not place the box or modify
     * {@link #regions()}.</p>
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
     * Adds a non-overlapping BFC region. Regions must be appended in ascending {@code startY}
     * order and must not overlap the previous region.
     *
     * @throws IllegalArgumentException if {@code region.startY()} is above the previous
     *                                  region's {@code endY()}
     */
    public void addRegion(BfcRegion region) {
        Objects.requireNonNull(region, "region");
        if (!regions.isEmpty()) {
            BfcRegion previous = regions.get(regions.size() - 1);
            if (region.startY() < previous.endY()) {
                throw new IllegalArgumentException(
                    "regions must be non-overlapping and ordered: " + region + " starts before "
                    + previous + " ends");
            }
        }
        regions.add(region);
    }

    /**
     * Returns an unmodifiable view of the BFC regions, in insertion order.
     */
    public List<BfcRegion> regions() {
        return Collections.unmodifiableList(regions);
    }

    /**
     * Returns the index of the region containing {@code y}, or {@link #NO_REGION}.
     */
    public int regionIndexAt(float y) {
        for (int i = 0; i < regions.size(); i++) {
            if (regions.get(i).containsY(y)) {
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
        return index == NO_REGION ? null : regions.get(index);
    }

    /**
     * Returns the bottom edge of the lowest float on the side(s) cleared by {@code clear}, or
     * {@code null} when {@code clear} is {@link Clear#NONE} or the relevant side has no floats.
     *
     * <p>This is the skeleton form of upstream's {@code cleared_threshold}: the values are
     * derived from the recorded float boxes' {@code y + height} instead of from placement
     * segment indices, which keeps the query independent of the not-yet-ported placement
     * algorithm.</p>
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
     * Searches for a content slot that avoids the recorded floats.
     *
     * <p>When no float is active at or below {@code minY}, the whole BFC width is returned
     * without a region id. Otherwise the start is moved down to the {@code clear} threshold,
     * the first region ending below that start is selected, and the slot spans from the
     * larger of the region's insets and the caller's insets. The height is
     * {@link Float#POSITIVE_INFINITY} (upstream #866).</p>
     *
     * @param minY       the minimum y position, in BFC-root coordinates
     * @param leftInset  the caller's left inset from the BFC edge
     * @param rightInset the caller's right inset from the BFC edge
     * @param clear      the box's {@code clear} value
     * @return the content slot
     */
    public ContentSlot contentSlot(float minY, float leftInset, float rightInset, Clear clear) {
        Objects.requireNonNull(clear, "clear");

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

        int index = firstRegionEndingAfter(startY);
        if (index == NO_REGION) {
            return new ContentSlot(
                NO_REGION, leftInset, startY,
                availableWidth - leftInset - rightInset, Float.POSITIVE_INFINITY);
        }

        BfcRegion region = regions.get(index);
        float x = Math.max(region.leftInset(), leftInset);
        float right = Math.max(region.rightInset(), rightInset);
        return new ContentSlot(
            index, x, Math.max(region.startY(), startY),
            availableWidth - x - right, Float.POSITIVE_INFINITY);
    }

    private int firstRegionEndingAfter(float y) {
        for (int i = 0; i < regions.size(); i++) {
            if (regions.get(i).endY() > y) {
                return i;
            }
        }
        return NO_REGION;
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
}
