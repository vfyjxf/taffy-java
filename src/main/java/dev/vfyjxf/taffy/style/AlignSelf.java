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
    NORMAL;

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
            case STRETCH, AUTO -> AlignItems.STRETCH;
        };
    }
}
