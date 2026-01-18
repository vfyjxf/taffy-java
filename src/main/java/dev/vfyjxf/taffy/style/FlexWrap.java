package dev.vfyjxf.taffy.style;

/**
 * Controls whether flex items are forced onto one line or can wrap onto multiple lines.
 * 
 * @see <a href="https://www.w3.org/TR/css-flexbox-1/#flex-wrap-property">CSS Flexbox - flex-wrap</a>
 */
public enum FlexWrap {
    /** Items will not wrap and stay on a single line */
    NO_WRAP,
    
    /** Items will wrap according to this item's FlexDirection */
    WRAP,
    
    /** Items will wrap in the opposite direction to this item's FlexDirection */
    WRAP_REVERSE;
}
