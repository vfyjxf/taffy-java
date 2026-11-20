package dev.vfyjxf.taffy.util;

import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;

/**
 * Utility class for resolving CSS dimension values against parent/context sizes.
 */
public final class Resolve {

    private Resolve() {
    }

    /**
     * Resolve a Size of Dimensions against a parent size.
     */
    public static FloatSize resolveSize(TaffySize<TaffyDimension> size, FloatSize parentSize) {
        return new FloatSize(
            size.width.maybeResolve(parentSize.width),
            size.height.maybeResolve(parentSize.height)
        );
    }

    /**
     * Resolve a Size of Dimensions against a parent size, returning zero for unresolvable values.
     */
    public static FloatSize resolveSizeOrZero(TaffySize<TaffyDimension> size, FloatSize parentSize) {
        return new FloatSize(
            size.width.resolveOrZero(parentSize.width),
            size.height.resolveOrZero(parentSize.height)
        );
    }

    /**
     * Resolve a Rect of LengthPercentage against a context width.
     */
    public static FloatRect resolveRectOrZero(TaffyRect<LengthPercentage> rect, float contextWidth) {
        return new FloatRect(
            rect.left.resolveOrZero(contextWidth),
            rect.right.resolveOrZero(contextWidth),
            rect.top.resolveOrZero(contextWidth),
            rect.bottom.resolveOrZero(contextWidth)
        );
    }

    /**
     * Resolve a Rect of LengthPercentageAuto against a context width.
     */
    public static FloatRect resolveRectLpaOrZero(TaffyRect<LengthPercentageAuto> rect, float contextWidth) {
        return new FloatRect(
            rect.left.resolveOrZero(contextWidth),
            rect.right.resolveOrZero(contextWidth),
            rect.top.resolveOrZero(contextWidth),
            rect.bottom.resolveOrZero(contextWidth)
        );
    }

    /**
     * Maybe resolve a Rect of LengthPercentageAuto against a context width.
     * Returns null for auto values, allowing tracking of which margins are auto.
     */
    public static FloatRect maybeResolveRectLpa(TaffyRect<LengthPercentageAuto> rect, float contextWidth) {
        return new FloatRect(
            rect.left.maybeResolve(contextWidth),
            rect.right.maybeResolve(contextWidth),
            rect.top.maybeResolve(contextWidth),
            rect.bottom.maybeResolve(contextWidth)
        );
    }

    /**
     * Maybe resolve a Size of Dimensions with null for unresolvable values.
     */
    public static FloatSize maybeResolveSize(TaffySize<TaffyDimension> size, FloatSize parentSize) {
        return new FloatSize(
            size.width.maybeResolve(parentSize.width),
            size.height.maybeResolve(parentSize.height)
        );
    }

    /**
     * Resolve a Size of LengthPercentage against a parent size.
     */
    public static FloatSize resolveSizeLp(TaffySize<LengthPercentage> size, FloatSize parentSize) {
        return new FloatSize(
            size.width.maybeResolve(parentSize.width),
            size.height.maybeResolve(parentSize.height)
        );
    }

    /**
     * Resolve a Size of LengthPercentage against a size, returning zero for unresolvable.
     */
    public static FloatSize resolveSizeLpOrZero(TaffySize<LengthPercentage> size, FloatSize contextSize) {
        return new FloatSize(
            size.width.resolveOrZero(contextSize.width),
            size.height.resolveOrZero(contextSize.height)
        );
    }

    /**
     * Whether {@code aspectRatio} is a ratio that can transfer a size between axes.
     *
     * <p>A ratio of zero ({@code aspect-ratio: 0/1}) or a non-finite one
     * ({@code aspect-ratio: 1/0}, {@code 0/0} parsed as infinity) carries no usable
     * proportionality: transferring through it would force the other axis to zero or to
     * infinity, while browsers leave the axis to its own sizing rule (a zero-ratio block still
     * fills its containing block, and an infinite ratio behaves like {@code auto}). The generated
     * WPT cases are css/css-sizing/aspect-ratio/zero-or-infinity-002 (zero) and -006..-010
     * (zero/infinite, replaced elements).</p>
     */
    public static boolean isTransferableAspectRatio(float aspectRatio) {
        return !Float.isNaN(aspectRatio) && aspectRatio > 0f && !Float.isInfinite(aspectRatio);
    }

    /**
     * Apply aspect ratio to a size if one dimension is missing.
     */
    public static FloatSize maybeApplyAspectRatio(FloatSize size, float aspectRatio) {
        if (!isTransferableAspectRatio(aspectRatio)) return size;
        if (!Float.isNaN(size.width) && Float.isNaN(size.height)) {
            return new FloatSize(size.width, size.width / aspectRatio);
        }
        if (Float.isNaN(size.width) && !Float.isNaN(size.height)) {
            return new FloatSize(size.height * aspectRatio, size.height);
        }
        return size;
    }

    /**
     * Transfer min/max sizes between the axes through a preferred aspect ratio.
     *
     * <p>This is the {@code min-width}/{@code min-height}/{@code max-width}/{@code max-height}
     * counterpart of {@link #maybeApplyAspectRatio(FloatSize, float)}, but the transfer only flows
     * into an axis whose <em>preferred</em> size is {@code auto} (unresolved, {@code NaN}). A box
     * that already has a definite preferred size in an axis derives its other axis from that size
     * (css-sizing-4 §5.1: the transferred size suggestion sizes the axis that depends on the
     * ratio), so a min/max constraint on the derived axis must not flow back and resize the
     * definite one. Chrome transfers a {@code max-height} into {@code max-width} only when the
     * inline size is auto; with a definite inline size the {@code max-height} only clamps the
     * ratio-derived block size (WPT grid-aspect-ratio-022/023/024/027).</p>
     *
     * @param value         the resolved min (or max) size pair; unresolved axes are {@code NaN}
     * @param aspectRatio   the preferred aspect ratio
     * @param preferredSize the box's preferred size, resolved through the same basis as
     *                      {@code value}; an {@code auto} axis is {@code NaN}
     * @return the transferred min/max size pair
     */
    public static FloatSize maybeApplyAspectRatioToMinMax(FloatSize value, float aspectRatio, FloatSize preferredSize) {
        if (!isTransferableAspectRatio(aspectRatio)) return value;
        float width = value.width;
        float height = value.height;
        // Both directions read the original values so that a single call never transfers twice.
        float transferredWidth = Float.isNaN(value.width) && Float.isNaN(preferredSize.width) && !Float.isNaN(value.height)
                                 ? value.height * aspectRatio
                                 : value.width;
        float transferredHeight = Float.isNaN(value.height) && Float.isNaN(preferredSize.height) && !Float.isNaN(value.width)
                                  ? value.width / aspectRatio
                                  : value.height;
        return new FloatSize(transferredWidth, transferredHeight);
    }

    /**
     * Maybe add to Size if values are non-null.
     */
    public static FloatSize maybeAddSize(FloatSize size, FloatSize addition) {
        return new FloatSize(
            TaffyMath.maybeAdd(size.width, addition.width),
            TaffyMath.maybeAdd(size.height, addition.height)
        );
    }

    /**
     * Maybe subtract from Size if values are non-null.
     */
    public static FloatSize maybeSubSize(FloatSize size, FloatSize subtraction) {
        return new FloatSize(
            TaffyMath.maybeSub(size.width, subtraction.width),
            TaffyMath.maybeSub(size.height, subtraction.height)
        );
    }

    /**
     * Maybe clamp Size between min and max.
     */
    public static FloatSize maybeClampSize(FloatSize size, FloatSize min, FloatSize max) {
        return new FloatSize(
            TaffyMath.maybeClamp(size.width, min != null ? min.width : Float.NaN, max != null ? max.width : Float.NaN),
            TaffyMath.maybeClamp(size.height, min != null ? min.height : Float.NaN, max != null ? max.height : Float.NaN)
        );
    }

    /**
     * Maybe max Size with a minimum size.
     */
    public static FloatSize maybeMaxSize(FloatSize size, FloatSize min) {
        return new FloatSize(
            TaffyMath.maybeMax(size.width, min.width),
            TaffyMath.maybeMax(size.height, min.height)
        );
    }

    /**
     * Resolve a LengthPercentageAuto value against context width, returning zero for auto or unresolvable.
     */
    public static float resolveLpaOrZero(LengthPercentageAuto lpa, float contextWidth) {
        if (lpa.isAuto()) return 0f;
        float resolved = lpa.maybeResolve(contextWidth);
        return Float.isNaN(resolved) ? 0f : resolved;
    }

    /**
     * Get Size or zero for null values.
     */
    public static FloatSize sizeOrZero(FloatSize size) {
        return new FloatSize(
            Float.isNaN(size.width) ? 0f : size.width,
            Float.isNaN(size.height) ? 0f : size.height
        );
    }
}
