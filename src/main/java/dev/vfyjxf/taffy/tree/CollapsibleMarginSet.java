package dev.vfyjxf.taffy.tree;

/**
 * A set of margins that are available for collapsing with for block layout's margin collapsing.
 */
public final class CollapsibleMarginSet {
    /** The largest positive margin */
    private float positive;
    
    /** The smallest negative margin (with largest absolute value) */
    private float negative;

    private CollapsibleMarginSet(float positive, float negative) {
        this.positive = positive;
        this.negative = negative;
    }

    /** A default margin set with no collapsible margins */
    public static final CollapsibleMarginSet ZERO = new CollapsibleMarginSet(0, 0);

    /**
     * Static factory method for zero margin set
     */
    public static CollapsibleMarginSet zero() {
        return new CollapsibleMarginSet(0, 0);
    }

    /**
     * Create a set from a single margin
     */
    public static CollapsibleMarginSet fromMargin(float margin) {
        if (margin >= 0) {
            return new CollapsibleMarginSet(margin, 0);
        } else {
            return new CollapsibleMarginSet(0, margin);
        }
    }

    /**
     * Collapse a single margin with this set
     */
    public CollapsibleMarginSet collapseWithMargin(float margin) {
        if (margin >= 0) {
            positive = Math.max(positive, margin);
        } else {
            negative = Math.min(negative, margin);
        }
        return this;
    }

    /**
     * Collapse another margin set with this set
     */
    public CollapsibleMarginSet collapseWithSet(CollapsibleMarginSet other) {
        positive = Math.max(positive, other.positive);
        negative = Math.min(negative, other.negative);
        return this;
    }

    /**
     * Resolve the resultant margin from this set once all collapsible margins have been collapsed into it
     */
    public float resolve() {
        return positive + negative;
    }

    /**
     * Copy this set
     */
    public CollapsibleMarginSet copy() {
        return new CollapsibleMarginSet(positive, negative);
    }

    @Override
    public String toString() {
        return "CollapsibleMarginSet{positive=" + positive + ", negative=" + negative + "}";
    }
}
