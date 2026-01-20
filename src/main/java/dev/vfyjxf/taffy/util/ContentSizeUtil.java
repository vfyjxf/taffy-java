package dev.vfyjxf.taffy.util;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.style.Overflow;

/**
 * Helpers for computing CSS content size contributions.
 *
 * <p>Mirrors Rust's {@code compute_content_size_contribution} logic:
 * a node contributes its border-box size, except that if its overflow is VISIBLE in an axis,
 * then its contribution in that axis is {@code max(size, contentSize)}.
 */
public final class ContentSizeUtil {

    private ContentSizeUtil() {
    }

    private static float sanitize(float v) {
        return Float.isNaN(v) ? 0f : v;
    }

    public static FloatSize max(FloatSize a, FloatSize b) {
        return new FloatSize(
            Math.max(sanitize(a.width), sanitize(b.width)),
            Math.max(sanitize(a.height), sanitize(b.height))
        );
    }

    /**
     * Determine how much width/height a given node contributes to its parent's content size.
     */
    public static FloatSize computeContentSizeContribution(
        FloatPoint location,
        FloatSize size,
        FloatSize contentSize,
        TaffyPoint<Overflow> overflow
    ) {
        float x = sanitize(location.x);
        float y = sanitize(location.y);

        float sizeW = sanitize(size.width);
        float sizeH = sanitize(size.height);

        float contentW = sanitize(contentSize.width);
        float contentH = sanitize(contentSize.height);

        float contributionW = overflow.x == Overflow.VISIBLE ? Math.max(sizeW, contentW) : sizeW;
        float contributionH = overflow.y == Overflow.VISIBLE ? Math.max(sizeH, contentH) : sizeH;

        if (contributionW > 0f && contributionH > 0f) {
            return new FloatSize(x + contributionW, y + contributionH);
        }

        return FloatSize.zero();
    }
}
