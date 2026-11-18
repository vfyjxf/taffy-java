package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for removing a node from the tree (P0-b1: CR-04/#1085).
 *
 * <p>{@link TaffyTree#remove(NodeId)} detaches the node from its parent, but the parent's cached
 * layout was computed with the removed node present. Unless the parent is marked dirty, a later
 * layout pass with the same inputs can hit that stale cache entry.
 */
public class RemoveRelayoutTest {

    private static final float EPSILON = 0.1f;

    @Test
    @DisplayName("remove_marks_the_former_parent_dirty")
    void removeMarksFormerParentDirty() {
        TaffyTree tree = new TaffyTree();
        NodeId parent = tree.newWithChildren(flexStyle(), leaf(tree, 100f, 50f), leaf(tree, 100f, 50f));

        tree.computeLayout(parent, TaffySize.maxContent());
        assertFalse(tree.isDirty(parent), "the parent has a cache entry after layout");

        tree.remove(tree.getChildAtIndex(parent, 1));

        assertTrue(tree.isDirty(parent), "removing a child must invalidate the parent's cache");
    }

    @Test
    @DisplayName("remove_child_relayouts_the_parent")
    void removeChildRelayoutsParent() {
        TaffyTree tree = new TaffyTree();
        NodeId first = leaf(tree, 100f, 50f);
        NodeId second = leaf(tree, 100f, 50f);
        NodeId parent = tree.newWithChildren(flexStyle(), first, second);

        // A max-content row of two 100px children is 200px wide.
        tree.computeLayout(parent, TaffySize.maxContent());
        assertEquals(200f, tree.getLayout(parent).size().width, EPSILON);

        tree.remove(second);
        tree.computeLayout(parent, TaffySize.maxContent());

        Layout parentLayout = tree.getLayout(parent);
        assertEquals(1, tree.childCount(parent));
        assertEquals(100f, parentLayout.size().width, EPSILON,
            "the relayout must not reuse the layout computed with the removed child");
        assertEquals(100f, tree.getLayout(first).size().width, EPSILON);
        assertTrue(tree.getChildren(parent).contains(first));
    }

    @Test
    @DisplayName("remove_nested_node_relayouts_its_ancestors")
    void removeNestedNodeRelayoutsItsAncestors() {
        TaffyTree tree = new TaffyTree();
        NodeId first = leaf(tree, 30f, 10f);
        NodeId second = leaf(tree, 30f, 10f);
        NodeId inner = tree.newWithChildren(flexStyle(), first, second);
        NodeId outer = tree.newWithChildren(flexStyle(), inner);

        tree.computeLayout(outer, TaffySize.maxContent());
        assertEquals(60f, tree.getLayout(outer).size().width, EPSILON);

        tree.remove(second);
        tree.computeLayout(outer, TaffySize.maxContent());

        assertEquals(30f, tree.getLayout(inner).size().width, EPSILON);
        assertEquals(30f, tree.getLayout(outer).size().width, EPSILON,
            "the ancestor's cached layout was computed with the removed node present");
    }

    private static TaffyStyle flexStyle() {
        TaffyStyle style = new TaffyStyle();
        style.flexDirection = dev.vfyjxf.taffy.style.FlexDirection.ROW;
        return style;
    }

    private static NodeId leaf(TaffyTree tree, float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return tree.newLeaf(style);
    }
}
