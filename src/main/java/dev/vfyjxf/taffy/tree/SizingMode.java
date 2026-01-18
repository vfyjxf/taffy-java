package dev.vfyjxf.taffy.tree;

/**
 * Whether styles should be taken into account when computing size.
 */
public enum SizingMode {
    /** Only content contributions should be taken into account */
    CONTENT_SIZE,
    
    /** Inherent size styles should be taken into account in addition to content contributions */
    INHERENT_SIZE
}
