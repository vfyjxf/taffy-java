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
    STRETCH,

    /**
     * The layout mode's default alignment (upstream #1255): {@code stretch} for flex and grid
     * containers, {@code start} for block containers.
     * <p>
     * Unlike {@link #AUTO}, which is the legacy "not specified" sentinel, this is an explicit
     * keyword that can override an inherited alignment. Appended after the existing constants so
     * their ordinals stay stable (1.2 compatibility policy S2/Q2).
     */
    NORMAL,

    /**
     * Like {@link #START}, but falls back to {@link #START} when the content overflows the
     * alignment container (upstream #952, {@code safe start}).
     */
    SAFE_START,

    /**
     * Like {@link #END}, but falls back to {@link #START} when the content overflows the
     * alignment container (upstream #952, {@code safe end}).
     */
    SAFE_END,

    /**
     * Like {@link #FLEX_START}, but falls back to {@link #START} when the content overflows the
     * alignment container (upstream #952, {@code safe flex-start}).
     */
    SAFE_FLEX_START,

    /**
     * Like {@link #FLEX_END}, but falls back to {@link #START} when the content overflows the
     * alignment container (upstream #952, {@code safe flex-end}).
     */
    SAFE_FLEX_END,

    /**
     * Like {@link #CENTER}, but falls back to {@link #START} when the content overflows the
     * alignment container (upstream #952, {@code safe center}).
     */
    SAFE_CENTER;
}
