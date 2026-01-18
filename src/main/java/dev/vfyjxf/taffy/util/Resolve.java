package dev.vfyjxf.taffy.util;

import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.Rect;
import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.Dimension;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;

/**
 * Utility class for resolving CSS dimension values against parent/context sizes.
 */
public final class Resolve {

    private Resolve() {
    }

    /**
     * Resolve a Size of Dimensions against a parent size.
     */
    public static FloatSize resolveSize(Size<Dimension> size, FloatSize parentSize) {
        return new FloatSize(
            size.width.maybeResolve(parentSize.width),
            size.height.maybeResolve(parentSize.height)
        );
    }

    /**
     * Resolve a Size of Dimensions against a parent size, returning zero for unresolvable values.
     */
    public static FloatSize resolveSizeOrZero(Size<Dimension> size, FloatSize parentSize) {
        return new FloatSize(
            size.width.resolveOrZero(parentSize.width),
            size.height.resolveOrZero(parentSize.height)
        );
    }

    /**
     * Resolve a Rect of LengthPercentage against a context width.
     */
    public static FloatRect resolveRectOrZero(Rect<LengthPercentage> rect, float contextWidth) {
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
    public static FloatRect resolveRectLpaOrZero(Rect<LengthPercentageAuto> rect, float contextWidth) {
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
    public static FloatRect maybeResolveRectLpa(Rect<LengthPercentageAuto> rect, float contextWidth) {
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
    public static FloatSize maybeResolveSize(Size<Dimension> size, FloatSize parentSize) {
        return new FloatSize(
            size.width.maybeResolve(parentSize.width),
            size.height.maybeResolve(parentSize.height)
        );
    }

    /**
     * Resolve a Size of LengthPercentage against a parent size.
     */
    public static FloatSize resolveSizeLp(Size<LengthPercentage> size, FloatSize parentSize) {
        return new FloatSize(
            size.width.maybeResolve(parentSize.width),
            size.height.maybeResolve(parentSize.height)
        );
    }

    /**
     * Resolve a Size of LengthPercentage against a size, returning zero for unresolvable.
     */
    public static FloatSize resolveSizeLpOrZero(Size<LengthPercentage> size, FloatSize contextSize) {
        return new FloatSize(
            size.width.resolveOrZero(contextSize.width),
            size.height.resolveOrZero(contextSize.height)
        );
    }

    /**
     * Apply aspect ratio to a size if one dimension is missing.
     */
    public static FloatSize maybeApplyAspectRatio(FloatSize size, Float aspectRatio) {
        if (aspectRatio == null) return size;
        if (!Float.isNaN(size.width) && Float.isNaN(size.height)) {
            return new FloatSize(size.width, size.width / aspectRatio);
        }
        if (Float.isNaN(size.width) && !Float.isNaN(size.height)) {
            return new FloatSize(size.height * aspectRatio, size.height);
        }
        return size;
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
            TaffyMath.maybeClamp(size.width, min != null ? min.width : null, max != null ? max.width : null),
            TaffyMath.maybeClamp(size.height, min != null ? min.height : null, max != null ? max.height : null)
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
