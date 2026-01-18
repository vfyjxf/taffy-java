package dev.vfyjxf.taffy.style;

/**
 * An abstracted version of the CSS display property where any value other than "none"
 * is represented by "normal".
 * 
 * @see <a href="https://www.w3.org/TR/css-display-3/#box-generation">CSS Display - Box Generation</a>
 */
public enum BoxGenerationMode {
    /** The node generates a box in the regular way */
    NORMAL,
    
    /** The node and its descendants generate no boxes (they are hidden) */
    NONE;

    public static final BoxGenerationMode DEFAULT = NORMAL;
}
