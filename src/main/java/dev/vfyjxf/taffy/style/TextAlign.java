package dev.vfyjxf.taffy.style;

/**
 * Text alignment for block layout.
 */
public enum TextAlign {
    /** Automatic alignment (typically left for LTR, right for RTL) */
    AUTO,
    
    /** Align to the start of the line (left for LTR) */
    START,
    
    /** Align to the end of the line (right for LTR) */
    END,
    
    /** Align to the left */
    LEFT,
    
    /** Align to the right */
    RIGHT,
    
    /** Center alignment */
    CENTER,
    
    /** Justify text by adjusting word spacing */
    JUSTIFY,
    
    /** Justify all lines including the last one */
    JUSTIFY_ALL;
}
