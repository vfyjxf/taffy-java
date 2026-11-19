package dev.vfyjxf.taffy.style;

/**
 * Whether a box is floated, and to which side.
 *
 * <p>This is the Java port of upstream taffy's {@code Float} style type introduced with CSS
 * floats (upstream #866, {@code 25759da}). It is named {@code TaffyFloat} rather than
 * {@code Float} because a top-level {@code dev.vfyjxf.taffy.style.Float} would shadow
 * {@link java.lang.Float} throughout the style package: existing style classes use
 * {@code Float.NaN}/{@code Float.isNaN}, and generated tests that
 * {@code import dev.vfyjxf.taffy.style.*} would see an ambiguous {@code Float}. The
 * {@code Taffy}-prefix convention is already used by {@link TaffyDisplay},
 * {@link TaffyPosition}, {@link TaffyDirection} and {@link TaffyDimension}.</p>
 *
 * <p><b>1.2 scope.</b> This enum and {@link TaffyStyle#float_} are the additive style
 * foundation of the float work package (FT-01). {@code BlockComputer} does not read the field
 * yet; float placement, block formatting-context integration and intrinsic-width contribution
 * land in the following FT batches.</p>
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/float">MDN - float</a>
 */
public enum TaffyFloat {

    /**
     * The box is not floated and stays in the normal flow (CSS {@code float: none}).
     */
    NONE,

    /**
     * The box is floated to the left (CSS {@code float: left}).
     */
    LEFT,

    /**
     * The box is floated to the right (CSS {@code float: right}).
     */
    RIGHT;

    /**
     * Returns whether this value takes the box out of the normal flow and floats it.
     * This is the port of upstream {@code Float::is_floated}.
     */
    public boolean isFloated() {
        return this != NONE;
    }

    /**
     * Returns whether this value floats the box to the left.
     */
    public boolean isLeft() {
        return this == LEFT;
    }

    /**
     * Returns whether this value floats the box to the right.
     */
    public boolean isRight() {
        return this == RIGHT;
    }
}
