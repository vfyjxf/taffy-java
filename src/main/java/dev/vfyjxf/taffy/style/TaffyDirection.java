package dev.vfyjxf.taffy.style;

/**
 * The CSS direction property defines the text direction and inline base direction.
 * 
 * Per CSS spec (https://www.w3.org/TR/css-writing-modes-3/#direction):
 * - Initial value: ltr
 * - Inherited: yes
 * 
 * This means:
 * - Child elements inherit direction from their parent by default
 * - Root elements use the initial value (LTR) if not explicitly set
 * 
 * In RTL (right-to-left) mode:
 * - For flexbox with row direction: items are laid out from right to left
 * - For grid: the inline axis starts from the right
 * - For block layout: text and inline elements flow from right to left
 * 
 * @see <a href="https://www.w3.org/TR/css-writing-modes-3/#direction">CSS Writing Modes - direction</a>
 */
public enum TaffyDirection {
    /** 
     * Inherit direction from parent element (CSS inherited property behavior).
     * If at root with no parent, resolves to the initial value (LTR).
     * This is the default value for Style.direction to match CSS inheritance semantics.
     */
    INHERIT,
    
    /** Left-to-right direction. Items flow from left to right in the inline axis. */
    LTR,
    
    /** Right-to-left direction. Items flow from right to left in the inline axis. */
    RTL;
    
    /** 
     * The CSS initial value for direction (used when INHERIT reaches root).
     * Per CSS spec, the initial value is 'ltr'.
     */
    public static final TaffyDirection DEFAULT = LTR;
    
    /**
     * Returns true if this is the left-to-right direction.
     * 
     * @return true if LTR
     */
    public boolean isLtr() {
        return this == LTR;
    }
    
    /**
     * Returns true if this is the right-to-left direction.
     * 
     * @return true if RTL
     */
    public boolean isRtl() {
        return this == RTL;
    }
    
    /**
     * Returns true if this direction should be inherited from parent.
     * 
     * @return true if INHERIT
     */
    public boolean isInherit() {
        return this == INHERIT;
    }
    
    /**
     * Returns the opposite direction.
     * 
     * @return RTL if this is LTR, LTR if this is RTL
     */
    public TaffyDirection opposite() {
        return this == LTR ? RTL : LTR;
    }
}
