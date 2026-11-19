package dev.vfyjxf.taffy.style;

/**
 * Gives a box "clearance", which moves it below floated boxes that precede it in the tree.
 *
 * <p>This is the Java port of upstream taffy's {@code Clear} style type from the CSS float
 * work (upstream #866, {@code 25759da}).</p>
 *
 * <p><b>1.2 scope.</b> This enum and {@link TaffyStyle#clear} are the additive style
 * foundation of the float work package (FT-01). {@code BlockComputer} does not read the field
 * yet; clearance and block formatting-context integration land in the following FT batches.</p>
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/clear">MDN - clear</a>
 */
public enum Clear {

    /**
     * The box does not clear floated boxes (CSS {@code clear: none}).
     */
    NONE,

    /**
     * The box clears left-floated boxes (CSS {@code clear: left}).
     */
    LEFT,

    /**
     * The box clears right-floated boxes (CSS {@code clear: right}).
     */
    RIGHT,

    /**
     * The box clears boxes floated in either direction (CSS {@code clear: both}).
     */
    BOTH;

    /**
     * Returns whether this value clears left-floated boxes.
     */
    public boolean clearsLeft() {
        return this == LEFT || this == BOTH;
    }

    /**
     * Returns whether this value clears right-floated boxes.
     */
    public boolean clearsRight() {
        return this == RIGHT || this == BOTH;
    }
}
