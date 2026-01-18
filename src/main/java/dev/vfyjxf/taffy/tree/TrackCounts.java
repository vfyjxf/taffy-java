package dev.vfyjxf.taffy.tree;

/**
 * Stores the number of tracks in a given dimension.
 * Stores separately the number of tracks in the implicit and explicit grids.
 * 
 * Taffy uses two coordinate systems to refer to grid lines (the gaps/gutters between rows/columns):
 * 
 * "CSS Grid Line" coordinates are those used in grid-row/grid-column in the CSS grid spec:
 *   - 0 is not a valid index
 *   - The line at left hand (or top) edge of the explicit grid is line 1
 *     (and counts up from there)
 *   - The line at the right hand (or bottom) edge of the explicit grid in -1
 *     (and counts down from there)
 * 
 * "OriginZero" coordinates are a normalized form:
 *   - The line at left hand (or top) edge of the explicit grid is line 0
 *   - The next line to the right (or down) is 1, and so on
 *   - The next line to the left (or up) is -1, and so on
 */
public class TrackCounts {
    /** The number of tracks in the implicit grid before the explicit grid */
    public int negativeImplicit;
    
    /** The number of tracks in the explicit grid */
    public int explicit;
    
    /** The number of tracks in the implicit grid after the explicit grid */
    public int positiveImplicit;
    
    public TrackCounts(int negativeImplicit, int explicit, int positiveImplicit) {
        this.negativeImplicit = negativeImplicit;
        this.explicit = explicit;
        this.positiveImplicit = positiveImplicit;
    }
    
    /** Copy constructor */
    public TrackCounts(TrackCounts other) {
        this.negativeImplicit = other.negativeImplicit;
        this.explicit = other.explicit;
        this.positiveImplicit = other.positiveImplicit;
    }
    
    /** Count the total number of tracks in the axis */
    public int len() {
        return negativeImplicit + explicit + positiveImplicit;
    }
    
    /** The OriginZeroLine representing the start of the implicit grid */
    public int implicitStartLine() {
        return -negativeImplicit;
    }
    
    /** The OriginZeroLine representing the end of the implicit grid */
    public int implicitEndLine() {
        return explicit + positiveImplicit;
    }
    
    /** The OriginZeroLine representing the end of the positive implicit grid */
    public int positiveImplicitEndLine() {
        return explicit + positiveImplicit;
    }
    
    /**
     * Converts a grid line in OriginZero coordinates into the track immediately
     * following that grid line as an index into the CellOccupancyMatrix.
     */
    public int ozLineToNextTrack(int ozLine) {
        return ozLine + negativeImplicit;
    }
    
    /**
     * Converts a track index in the CellOccupancyMatrix into the grid line immediately
     * preceding that track in OriginZero coordinates.
     */
    public int trackToOzLine(int trackIndex) {
        return trackIndex - negativeImplicit;
    }
    
    /**
     * Update this TrackCounts from another TrackCounts.
     */
    public void update(TrackCounts other) {
        this.negativeImplicit = other.negativeImplicit;
        this.explicit = other.explicit;
        this.positiveImplicit = other.positiveImplicit;
    }
    
    @Override
    public String toString() {
        return "TrackCounts{negativeImplicit=" + negativeImplicit + 
               ", explicit=" + explicit + 
               ", positiveImplicit=" + positiveImplicit + "}";
    }
}
