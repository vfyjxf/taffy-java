package dev.vfyjxf.taffy.util;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.tree.Baselines;
import dev.vfyjxf.taffy.tree.LayoutOutput;

/**
 * Shared resolution of a child box's first baseline, used by the block, flexbox and grid layout
 * algorithms (BL-06 / CR-07, upstream taffy #1323).
 *
 * <p>Mirrors upstream {@code src/compute/common/alignment.rs::child_first_baseline}, which unified
 * the five copies of the scroll-container clamp (and supplied the sixth that was missing, the flex
 * column direction). The returned baseline is measured from the top edge of the child's border
 * box, exactly like {@link Baselines}. A value of {@link Float#NaN} means "no first baseline".</p>
 *
 * <p><b>Scroll containers.</b> Per the CSSWG resolution
 * <a href="https://github.com/w3c/csswg-drafts/issues/7660">csswg-drafts#7660</a> a scroll
 * container's baseline is determined from its content as if scrolled to the initial position, but
 * is additionally clamped to its own border box: the result is always in {@code [0, height]}. A
 * scroll container that reports no baseline synthesizes one at its border-box bottom edge (i.e.
 * {@code height}); this is the only synthesis upstream performs. A non-scroll child without a
 * baseline contributes nothing ({@link Float#NaN}) — upstream's block algorithm then propagates no
 * baseline, and the flexbox/grid algorithms fall back to the child's border-box height at the call
 * site ({@code .unwrap_or(size.height)}). This port deliberately does not invent a synthesis from
 * an in-flow subtree, because upstream has none.</p>
 *
 * <p>The child's relative offset is intentionally <em>not</em> part of this helper: the callers
 * that need a container-relative baseline pass a location that already excludes any
 * {@code position: relative} inset (upstream #1323, {@code location.y - inset_offset.y}).</p>
 */
public final class BaselineHelper {

    private BaselineHelper() {
    }

    /**
     * Resolves the first baseline of a child, measured from the top of the child's border box.
     *
     * @param childOutput       the child's layout output; {@code null} yields {@link Float#NaN}
     * @param isScrollContainer whether the child is a scroll container in the block axis, i.e.
     *                          {@code overflow.y.isScrollContainer()}
     * @return the border-box-relative first baseline, or {@link Float#NaN} when the child has none
     */
    public static float childFirstBaseline(LayoutOutput childOutput, boolean isScrollContainer) {
        if (childOutput == null) {
            return Float.NaN;
        }

        float baseline = firstBaselineY(childOutput);
        if (!isScrollContainer) {
            return baseline;
        }

        float height = childOutput.size() != null ? childOutput.size().height : Float.NaN;
        // Scroll containers are clamped to their border box; a missing baseline synthesizes at the
        // border-box bottom edge (upstream: first_baseline.unwrap_or(height).min(height).max(0.0)).
        float value = Float.isNaN(baseline) ? height : baseline;
        if (Float.isNaN(value)) {
            // Rust's f32::min ignores NaN operands and f32::max(NaN, 0.0) is 0.0; keep the same
            // result for a degenerate child whose laid-out height is still unknown.
            return 0f;
        }
        return Math.max(0f, Float.isNaN(height) ? value : Math.min(value, height));
    }

    /**
     * Resolves the first baseline of a child in its parent's coordinate space.
     *
     * <p>The supplied location must already exclude the child's relative offset; pass
     * {@code location.y - relativeInsetY} (upstream #1323).</p>
     *
     * @param childOutput       the child's layout output; {@code null} yields {@link Float#NaN}
     * @param childLocationY    the child's border-box top in the parent's coordinate space,
     *                          without any relative offset
     * @param isScrollContainer whether the child is a scroll container in the block axis
     * @return the parent-relative first baseline, or {@link Float#NaN} when the child has none
     */
    public static float childFirstBaseline(LayoutOutput childOutput, float childLocationY,
                                           boolean isScrollContainer) {
        float baseline = childFirstBaseline(childOutput, isScrollContainer);
        return Float.isNaN(baseline) ? Float.NaN : childLocationY + baseline;
    }

    /**
     * Resolves the first baseline of a child in its parent's coordinate space.
     *
     * @param childOutput       the child's layout output; {@code null} yields {@link Float#NaN}
     * @param childLocation     the child's border-box top-left in the parent's coordinate space,
     *                          without any relative offset; {@code null} yields {@link Float#NaN}
     * @param isScrollContainer whether the child is a scroll container in the block axis
     * @return the parent-relative first baseline, or {@link Float#NaN} when the child has none
     * @see #childFirstBaseline(LayoutOutput, float, boolean)
     */
    public static float childFirstBaseline(LayoutOutput childOutput, FloatPoint childLocation,
                                           boolean isScrollContainer) {
        if (childLocation == null) {
            return Float.NaN;
        }
        return childFirstBaseline(childOutput, childLocation.y, isScrollContainer);
    }

    /**
     * Reads the first baseline from the new {@link Baselines} component, falling back to the legacy
     * {@link LayoutOutput#firstBaselines()} point while both components coexist in 1.2.
     */
    private static float firstBaselineY(LayoutOutput childOutput) {
        Baselines baselines = childOutput.baselines();
        if (baselines != null) {
            FloatPoint first = baselines.first();
            if (first != null && !Float.isNaN(first.y)) {
                return first.y;
            }
        }
        FloatPoint legacy = childOutput.firstBaselines();
        return legacy != null ? legacy.y : Float.NaN;
    }
}
