package dev.vfyjxf.taffy.style;

/**
 * The positioning strategy for this item.
 * 
 * This controls both how the origin is determined for the position/inset properties,
 * and whether or not the item will be controlled by flexbox's layout algorithm.
 * 
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/position">MDN - position</a>
 */
public enum TaffyPosition {
    /**
     * The offset is computed relative to the final position given by the layout algorithm.
     * Offsets do not affect the position of any other items; they are effectively a correction
     * factor applied at the end.
     */
    RELATIVE,
    
    /**
     * The offset is computed relative to this item's closest positioned ancestor, if any.
     * Otherwise, it is placed relative to the origin.
     * No space is created for the item in the page layout, and its size will not be altered.
     */
    ABSOLUTE;
}
