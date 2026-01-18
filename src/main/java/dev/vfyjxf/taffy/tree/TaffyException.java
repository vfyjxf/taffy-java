package dev.vfyjxf.taffy.tree;

/**
 * Error types for Taffy operations.
 */
public class TaffyException extends RuntimeException {

    private final ErrorType type;
    private final NodeId parentNode;
    private final NodeId childNode;
    private final Integer childIndex;
    private final Integer childCount;

    public enum ErrorType {
        CHILD_INDEX_OUT_OF_BOUNDS,
        INVALID_PARENT_NODE,
        INVALID_CHILD_NODE,
        INVALID_INPUT_NODE
    }

    private TaffyException(ErrorType type, NodeId parentNode, NodeId childNode, Integer childIndex, Integer childCount, String message) {
        super(message);
        this.type = type;
        this.parentNode = parentNode;
        this.childNode = childNode;
        this.childIndex = childIndex;
        this.childCount = childCount;
    }

    public static TaffyException childIndexOutOfBounds(NodeId parent, int childIndex, int childCount) {
        String message = String.format(
            "Index (is %d) should be < child_count (%d) for parent node %s",
            childIndex, childCount, parent
        );
        return new TaffyException(ErrorType.CHILD_INDEX_OUT_OF_BOUNDS, parent, null, childIndex, childCount, message);
    }

    public static TaffyException invalidParentNode(NodeId parent) {
        String message = String.format("Parent Node %s is not in the TaffyTree instance", parent);
        return new TaffyException(ErrorType.INVALID_PARENT_NODE, parent, null, null, null, message);
    }

    public static TaffyException invalidChildNode(NodeId child) {
        String message = String.format("Child Node %s is not in the TaffyTree instance", child);
        return new TaffyException(ErrorType.INVALID_CHILD_NODE, null, child, null, null, message);
    }

    public static TaffyException invalidInputNode(NodeId node) {
        String message = String.format("Supplied Node %s is not in the TaffyTree instance", node);
        return new TaffyException(ErrorType.INVALID_INPUT_NODE, null, null, null, null, message);
    }

    public ErrorType getType() {
        return type;
    }

    public NodeId getParentNode() {
        return parentNode;
    }

    public NodeId getChildNode() {
        return childNode;
    }

    public Integer getChildIndex() {
        return childIndex;
    }

    public Integer getChildCount() {
        return childCount;
    }
}
