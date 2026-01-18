package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.AbsoluteAxis;

/**
 * An axis that layout algorithms can be requested to compute a size for.
 */
public enum RequestedAxis {
    /** The horizontal axis */
    HORIZONTAL,
    
    /** The vertical axis */
    VERTICAL,
    
    /** Both axes */
    BOTH;

    /**
     * Convert from AbsoluteAxis
     */
    public static RequestedAxis from(AbsoluteAxis axis) {
        return axis == AbsoluteAxis.HORIZONTAL ? HORIZONTAL : VERTICAL;
    }

    /**
     * Try to convert to AbsoluteAxis (returns null for BOTH)
     */
    public AbsoluteAxis toAbsoluteAxis() {
        return switch (this) {
            case HORIZONTAL -> AbsoluteAxis.HORIZONTAL;
            case VERTICAL -> AbsoluteAxis.VERTICAL;
            case BOTH -> null;
        };
    }
}
