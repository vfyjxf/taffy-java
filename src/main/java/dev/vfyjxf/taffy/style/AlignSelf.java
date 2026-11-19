package dev.vfyjxf.taffy.style;

/**
 * Controls how an individual flex/grid item is aligned along the cross axis.
 */
public enum AlignSelf {
    /** Use the parent's align-items value */
    AUTO,
    /** Items are aligned at the flex-relative start of the cross axis (flips with flex-wrap: wrap-reverse) */
    FLEX_START,
    /** Items are aligned at the flex-relative end of the cross axis (flips with flex-wrap: wrap-reverse) */
    FLEX_END,
    /** Items are aligned at the center of the cross axis */
    CENTER,
    /** Items are aligned at the baseline */
    BASELINE,
    /** Items are stretched to fill the cross axis */
    STRETCH,
    /**
     * Items are aligned at the physical start of the cross axis.
     * <p>
     * Unlike {@link #FLEX_START}, this is not affected by {@code flex-wrap: wrap-reverse};
     * it always resolves to the cross-start edge of the container.
     */
    START,
    /**
     * Items are aligned at the physical end of the cross axis.
     * <p>
     * Unlike {@link #FLEX_END}, this is not affected by {@code flex-wrap: wrap-reverse};
     * it always resolves to the cross-end edge of the container.
     */
    END,

    /**
     * The layout mode's default alignment (upstream #1254): {@code stretch} for flex items and
     * for grid items without a definite size, {@code start} where stretching does not apply.
     * <p>
     * Unlike {@link #AUTO}, which is the legacy "not specified" sentinel, this is an explicit
     * keyword that can override an inherited alignment. Appended after the existing constants so
     * their ordinals stay stable (1.2 compatibility policy S2/Q2).
     */
    NORMAL,

    /**
     * Items are packed toward the start of the axis as determined by the item's own
     * writing mode/direction rather than the container's (upstream #1077).
     * <p>
     * Equivalent to {@link #START} when the item's direction matches the container's and to
     * {@link #END} when they differ in the inline axis. The layout algorithms resolve this through
     * {@code AlignmentHelper.resolveSelfRelative}.
     */
    SELF_START,

    /**
     * Items are packed toward the end of the axis as determined by the item's own writing
     * mode/direction rather than the container's (upstream #1077).
     * <p>
     * Equivalent to {@link #END} when the item's direction matches the container's and to
     * {@link #START} when they differ in the inline axis. Resolution is not wired into the layout
     * algorithms in this batch; use {@code AlignmentHelper.resolveSelfRelative}.
     */
    SELF_END,

    /**
     * Like {@link #START}, but falls back to {@link #START} when the alignment subject overflows
     * the alignment container, so the start edge stays visible (upstream #952, CSS Box Alignment
     * {@code safe start}).
     */
    SAFE_START,

    /**
     * Like {@link #END}, but falls back to {@link #START} when the alignment subject overflows
     * the alignment container (upstream #952, {@code safe end}).
     */
    SAFE_END,

    /**
     * Like {@link #FLEX_START}, but falls back to {@link #START} when the alignment subject
     * overflows the alignment container (upstream #952, {@code safe flex-start}).
     */
    SAFE_FLEX_START,

    /**
     * Like {@link #FLEX_END}, but falls back to {@link #START} when the alignment subject
     * overflows the alignment container (upstream #952, {@code safe flex-end}).
     */
    SAFE_FLEX_END,

    /**
     * Like {@link #CENTER}, but falls back to {@link #START} when the alignment subject overflows
     * the alignment container (upstream #952, {@code safe center}).
     */
    SAFE_CENTER,

    /**
     * Like {@link #SELF_START}, but falls back to {@link #START} when the alignment subject
     * overflows the alignment container (upstream #952, {@code safe self-start}).
     */
    SAFE_SELF_START,

    /**
     * Like {@link #SELF_END}, but falls back to {@link #START} when the alignment subject
     * overflows the alignment container (upstream #952, {@code safe self-end}).
     */
    SAFE_SELF_END;

    /**
     * Creates an AlignSelf from an AlignItems value.
     * <p>
     * {@link AlignItems#START}/{@link AlignItems#END} are kept distinct from the
     * flex-relative {@link AlignItems#FLEX_START}/{@link AlignItems#FLEX_END}:
     * they differ under {@code flex-wrap: wrap-reverse}.
     */
    public static AlignSelf fromAlignItems(AlignItems alignItems) {
        if (alignItems == null || alignItems == AlignItems.AUTO) return STRETCH;
        return switch (alignItems) {
            case START -> START;
            case END -> END;
            case FLEX_START -> FLEX_START;
            case FLEX_END -> FLEX_END;
            case CENTER -> CENTER;
            case BASELINE -> BASELINE;
            case STRETCH -> STRETCH;
            case NORMAL -> NORMAL;
            case SELF_START -> SELF_START;
            case SELF_END -> SELF_END;
            case SAFE_START -> SAFE_START;
            case SAFE_END -> SAFE_END;
            case SAFE_FLEX_START -> SAFE_FLEX_START;
            case SAFE_FLEX_END -> SAFE_FLEX_END;
            case SAFE_CENTER -> SAFE_CENTER;
            case SAFE_SELF_START -> SAFE_SELF_START;
            case SAFE_SELF_END -> SAFE_SELF_END;
            case AUTO -> STRETCH; // defensive (handled above)
        };
    }

    /**
     * Converts to AlignItems.
     */
    public AlignItems toAlignItems() {
        return switch (this) {
            case START -> AlignItems.START;
            case END -> AlignItems.END;
            case FLEX_START -> AlignItems.FLEX_START;
            case FLEX_END -> AlignItems.FLEX_END;
            case CENTER -> AlignItems.CENTER;
            case BASELINE -> AlignItems.BASELINE;
            case NORMAL -> AlignItems.NORMAL;
            case SELF_START -> AlignItems.SELF_START;
            case SELF_END -> AlignItems.SELF_END;
            case SAFE_START -> AlignItems.SAFE_START;
            case SAFE_END -> AlignItems.SAFE_END;
            case SAFE_FLEX_START -> AlignItems.SAFE_FLEX_START;
            case SAFE_FLEX_END -> AlignItems.SAFE_FLEX_END;
            case SAFE_CENTER -> AlignItems.SAFE_CENTER;
            case SAFE_SELF_START -> AlignItems.SAFE_SELF_START;
            case SAFE_SELF_END -> AlignItems.SAFE_SELF_END;
            case STRETCH, AUTO -> AlignItems.STRETCH;
        };
    }
}
