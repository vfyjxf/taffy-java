package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatPoint;

/**
 * The first and last baselines of a node, measured as an offset from the top edge of the node's
 * border box.
 *
 * <p>A baseline is the line on which text sits; see
 * <a href="https://www.w3.org/TR/css-writing-modes-3/#intro-baselines">CSS Writing Modes 3</a>.
 * Each baseline is stored as a four-value {@link FloatPoint} pair, so all four slots are
 * representable: {@code first.x}, {@code first.y}, {@code last.x} and {@code last.y}.
 * {@link Float#NaN} on an axis means "no baseline on that axis". In the horizontal writing mode
 * implemented by the layout algorithms only the {@code y} value is meaningful; the {@code x}
 * value is unused and is normally NaN for both baselines.</p>
 *
 * <p><b>1.2 status (T2/BL-02, additive skeleton):</b> this type is the new baseline contract that
 * lives next to the legacy {@link LayoutOutput#firstBaselines()} component. No layout algorithm
 * populates it yet: {@code first} stays {@link #NONE}-like (NaN) as before and {@code last} has no
 * producer at all (upstream #1107 also leaves it empty). The block/flex/grid baseline propagation
 * (P3/CR-07) and the removal of the legacy component belong to later phases; 2.0 deletes
 * {@code firstBaselines}.</p>
 *
 * @param first the first baseline of the node; NaN coordinates mean "absent"
 * @param last  the last baseline of the node; NaN coordinates mean "absent"
 */
public record Baselines(FloatPoint first, FloatPoint last) {

    /**
     * A {@code Baselines} with neither a first nor a last baseline.
     */
    public static final Baselines NONE = new Baselines(FloatPoint.none(), FloatPoint.none());

    /**
     * Creates a {@code Baselines} from just a first baseline; the last baseline is absent.
     *
     * @param first the first baseline, or a point whose coordinates are NaN for the absent axes
     * @return a {@code Baselines} whose last baseline is absent
     */
    public static Baselines fromFirst(FloatPoint first) {
        return new Baselines(first, FloatPoint.none());
    }

    /**
     * Whether the first baseline is present on at least one axis.
     *
     * @return {@code true} if {@link #first()} is non-null and has at least one non-NaN coordinate
     */
    public boolean hasFirst() {
        return isPresent(first);
    }

    /**
     * Whether the last baseline is present on at least one axis.
     *
     * @return {@code true} if {@link #last()} is non-null and has at least one non-NaN coordinate
     */
    public boolean hasLast() {
        return isPresent(last);
    }

    private static boolean isPresent(FloatPoint point) {
        return point != null && (!Float.isNaN(point.x) || !Float.isNaN(point.y));
    }

    @Override
    public String toString() {
        return "Baselines{first=" + first + ", last=" + last + '}';
    }
}
