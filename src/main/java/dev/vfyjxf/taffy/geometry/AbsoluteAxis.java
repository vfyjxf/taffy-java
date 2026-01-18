package dev.vfyjxf.taffy.geometry;

/**
 * The simple absolute horizontal and vertical axis
 */
public enum AbsoluteAxis {
    HORIZONTAL,
    VERTICAL;

    /**
     * Returns the other variant of the enum
     */
    public AbsoluteAxis other() {
        return this == HORIZONTAL ? VERTICAL : HORIZONTAL;
    }
}
