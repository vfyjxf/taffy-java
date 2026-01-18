package dev.vfyjxf.taffy.tree;

/**
 * A type representing the id of a single node in a tree of nodes.
 * Internally it is a wrapper around a long value.
 */
public record NodeId(long value) {

    /**
     * Create a new NodeId from a long value
     */
    public static NodeId of(long value) {
        return new NodeId(value);
    }

    /**
     * Get the ID (alias for getValue)
     */
    public long getId() {
        return value;
    }

    /**
     * Convert to int index
     */
    public int toIndex() {
        return (int) value;
    }
}
