package dev.vfyjxf.taffy.style;

/**
 * Controls whether flex items are forced onto one line or can wrap onto multiple lines.
 *
 * <p>The {@link #BALANCE} and {@link #BALANCE_REVERSE} values implement the CSS Flexbox Level 2
 * {@code balance} keywords: items wrap, and are divided into lines such that the largest line is
 * as small as possible (see {@link #isBalance()}). The minimum number of lines is controlled by
 * {@link TaffyStyle#flexLineCount}.</p>
 *
 * @see <a href="https://www.w3.org/TR/css-flexbox-1/#flex-wrap-property">CSS Flexbox - flex-wrap</a>
 * @see <a href="https://drafts.csswg.org/css-flexbox-2/#balance-values">CSS Flexbox 2 - balance values</a>
 */
public enum FlexWrap {
    /** Items will not wrap and stay on a single line */
    NO_WRAP,

    /** Items will wrap according to this item's FlexDirection */
    WRAP,

    /** Items will wrap in the opposite direction to this item's FlexDirection */
    WRAP_REVERSE,

    /**
     * Items will wrap according to this item's FlexDirection, and be balanced across lines such
     * that the largest line is as small as possible.
     */
    BALANCE,

    /**
     * Items will wrap in the opposite direction to this item's FlexDirection, and be balanced
     * across lines such that the largest line is as small as possible.
     */
    BALANCE_REVERSE;

    /**
     * Returns whether this value enables wrapping (any value other than {@link #NO_WRAP}).
     */
    public boolean isMultiLine() {
        return this != NO_WRAP;
    }

    /**
     * Returns whether lines stack in the opposite direction to the cross axis
     * ({@link #WRAP_REVERSE} and {@link #BALANCE_REVERSE}).
     */
    public boolean isReverse() {
        return this == WRAP_REVERSE || this == BALANCE_REVERSE;
    }

    /**
     * Returns whether items are balanced across lines ({@link #BALANCE} and
     * {@link #BALANCE_REVERSE}).
     */
    public boolean isBalance() {
        return this == BALANCE || this == BALANCE_REVERSE;
    }
}
