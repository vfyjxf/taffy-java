package dev.vfyjxf.taffy.style;

/**
 * Defines how content is distributed along the main axis in flexbox and grid.
 * <p>
 * Note: STRETCH has special meaning in CSS Grid - it expands auto tracks to fill free space.
 * In flexbox, STRETCH is equivalent to FLEX_START for justify-content.
 */
public enum JustifyContent {
    /** Items are packed at the start */
    FLEX_START,
    /** Items are packed at the end */
    FLEX_END,
    /** Items are centered */
    CENTER,
    /** Items are evenly distributed with space between */
    SPACE_BETWEEN,
    /** Items are evenly distributed with space around */
    SPACE_AROUND,
    /** Items are evenly distributed with equal space */
    SPACE_EVENLY,
    /** Items are packed at the start */
    START,
    /** Items are packed at the end */
    END,
    /**
     * Auto tracks are stretched to fill free space.
     * <p>
     * In CSS Grid: auto tracks expand to share remaining space equally.
     * In Flexbox: equivalent to FLEX_START (no effect on main axis distribution).
     */
    STRETCH;
}
