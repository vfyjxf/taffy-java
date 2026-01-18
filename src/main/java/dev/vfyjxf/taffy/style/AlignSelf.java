package dev.vfyjxf.taffy.style;

/**
 * Controls how an individual flex/grid item is aligned along the cross axis.
 */
public enum AlignSelf {
    /** Use the parent's align-items value */
    AUTO,
    /** Items are aligned at the start of the cross axis */
    FLEX_START,
    /** Items are aligned at the end of the cross axis */
    FLEX_END,
    /** Items are aligned at the center of the cross axis */
    CENTER,
    /** Items are aligned at the baseline */
    BASELINE,
    /** Items are stretched to fill the cross axis */
    STRETCH;

    /**
     * Creates an AlignSelf from an AlignItems value.
     */
    public static AlignSelf fromAlignItems(AlignItems alignItems) {
        if (alignItems == null) return STRETCH;
        return switch (alignItems) {
            case FLEX_START, START -> FLEX_START;
            case FLEX_END, END -> FLEX_END;
            case CENTER -> CENTER;
            case BASELINE -> BASELINE;
            case STRETCH -> STRETCH;
        };
    }

    /**
     * Converts to AlignItems.
     */
    public AlignItems toAlignItems() {
        return switch (this) {
            case FLEX_START -> AlignItems.FLEX_START;
            case FLEX_END -> AlignItems.FLEX_END;
            case CENTER -> AlignItems.CENTER;
            case BASELINE -> AlignItems.BASELINE;
            default -> AlignItems.STRETCH;
        };
    }
}
