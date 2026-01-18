package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.Rect;
import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.Dimension;
import dev.vfyjxf.taffy.style.Display;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.Style;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended TaffyTree API tests ported from taffy/src/tree/taffy_tree.rs
 */
public class TaffyTreeApiTest {

    // ==================== Capacity Tests ====================

    @Test
    @DisplayName("new_should_allocate_default_capacity")
    void newShouldAllocateDefaultCapacity() {
        TaffyTree tree = new TaffyTree();
        // Just verify tree is created, Java doesn't expose capacity like Rust
        assertNotNull(tree);
    }

    @Test
    @DisplayName("test_with_capacity")
    void testWithCapacity() {
        TaffyTree tree = new TaffyTree(8);
        assertNotNull(tree);
    }

    // ==================== Leaf Node Tests ====================

    @Test
    @DisplayName("test_new_leaf")
    void testNewLeaf() {
        TaffyTree tree = new TaffyTree();
        NodeId node = tree.newLeaf(new Style());
        
        assertNotNull(node);
        assertEquals(0, tree.childCount(node));
    }

    @Test
    @DisplayName("new_leaf_with_context")
    void newLeafWithContext() {
        TaffyTree tree = new TaffyTree();
        
        MeasureFunc measureFunc = (knownDimensions, availableSpace) -> FloatSize.zero();
        NodeId node = tree.newLeafWithMeasure(new Style(), measureFunc);
        
        assertNotNull(node);
        assertEquals(0, tree.childCount(node));
    }

    // ==================== Children Management Tests ====================

    @Test
    @DisplayName("test_new_with_children")
    void testNewWithChildren() {
        TaffyTree tree = new TaffyTree();
        NodeId child0 = tree.newLeaf(new Style());
        NodeId child1 = tree.newLeaf(new Style());
        NodeId node = tree.newWithChildren(new Style(), child0, child1);

        assertEquals(2, tree.childCount(node));
        List<NodeId> children = tree.getChildren(node);
        assertEquals(child0, children.get(0));
        assertEquals(child1, children.get(1));
    }

    @Test
    @DisplayName("add_child")
    void addChild() {
        TaffyTree tree = new TaffyTree();
        NodeId node = tree.newLeaf(new Style());
        assertEquals(0, tree.childCount(node));

        NodeId child0 = tree.newLeaf(new Style());
        tree.addChild(node, child0);
        assertEquals(1, tree.childCount(node));

        NodeId child1 = tree.newLeaf(new Style());
        tree.addChild(node, child1);
        assertEquals(2, tree.childCount(node));
    }

    @Test
    @DisplayName("insert_child_at_index")
    void insertChildAtIndex() {
        TaffyTree tree = new TaffyTree();
        
        NodeId child0 = tree.newLeaf(new Style());
        NodeId child1 = tree.newLeaf(new Style());
        NodeId child2 = tree.newLeaf(new Style());
        
        NodeId node = tree.newLeaf(new Style());
        assertEquals(0, tree.childCount(node));

        tree.insertChildAtIndex(node, 0, child0);
        assertEquals(1, tree.childCount(node));
        assertEquals(child0, tree.getChildren(node).get(0));

        tree.insertChildAtIndex(node, 0, child1);
        assertEquals(2, tree.childCount(node));
        assertEquals(child1, tree.getChildren(node).get(0));
        assertEquals(child0, tree.getChildren(node).get(1));

        tree.insertChildAtIndex(node, 1, child2);
        assertEquals(3, tree.childCount(node));
        assertEquals(child1, tree.getChildren(node).get(0));
        assertEquals(child2, tree.getChildren(node).get(1));
        assertEquals(child0, tree.getChildren(node).get(2));
    }

    @Test
    @DisplayName("set_children")
    void setChildren() {
        TaffyTree tree = new TaffyTree();
        
        NodeId child0 = tree.newLeaf(new Style());
        NodeId child1 = tree.newLeaf(new Style());
        NodeId node = tree.newWithChildren(new Style(), child0, child1);

        assertEquals(2, tree.childCount(node));
        assertEquals(child0, tree.getChildren(node).get(0));
        assertEquals(child1, tree.getChildren(node).get(1));

        NodeId child2 = tree.newLeaf(new Style());
        NodeId child3 = tree.newLeaf(new Style());
        tree.setChildren(node, child2, child3);

        assertEquals(2, tree.childCount(node));
        assertEquals(child2, tree.getChildren(node).get(0));
        assertEquals(child3, tree.getChildren(node).get(1));
    }

    @Test
    @DisplayName("remove_child")
    void removeChild() {
        TaffyTree tree = new TaffyTree();
        NodeId child0 = tree.newLeaf(new Style());
        NodeId child1 = tree.newLeaf(new Style());
        NodeId node = tree.newWithChildren(new Style(), child0, child1);

        assertEquals(2, tree.childCount(node));

        tree.removeChild(node, child0);
        assertEquals(1, tree.childCount(node));
        assertEquals(child1, tree.getChildren(node).get(0));

        tree.removeChild(node, child1);
        assertEquals(0, tree.childCount(node));
    }

    @Test
    @DisplayName("remove_child_at_index")
    void removeChildAtIndex() {
        TaffyTree tree = new TaffyTree();
        NodeId child0 = tree.newLeaf(new Style());
        NodeId child1 = tree.newLeaf(new Style());
        NodeId node = tree.newWithChildren(new Style(), child0, child1);

        assertEquals(2, tree.childCount(node));

        tree.removeChildAtIndex(node, 0);
        assertEquals(1, tree.childCount(node));
        assertEquals(child1, tree.getChildren(node).get(0));

        tree.removeChildAtIndex(node, 0);
        assertEquals(0, tree.childCount(node));
    }

    @Test
    @DisplayName("replace_child_at_index")
    void replaceChildAtIndex() {
        TaffyTree tree = new TaffyTree();
        
        NodeId child0 = tree.newLeaf(new Style());
        NodeId child1 = tree.newLeaf(new Style());
        
        NodeId node = tree.newWithChildren(new Style(), child0);
        assertEquals(1, tree.childCount(node));
        assertEquals(child0, tree.getChildren(node).get(0));

        tree.replaceChildAtIndex(node, 0, child1);
        assertEquals(1, tree.childCount(node));
        assertEquals(child1, tree.getChildren(node).get(0));
    }

    @Test
    @DisplayName("test_child_at_index")
    void testChildAtIndex() {
        TaffyTree tree = new TaffyTree();
        NodeId child0 = tree.newLeaf(new Style());
        NodeId child1 = tree.newLeaf(new Style());
        NodeId child2 = tree.newLeaf(new Style());
        NodeId node = tree.newWithChildren(new Style(), child0, child1, child2);

        assertEquals(child0, tree.getChildAtIndex(node, 0));
        assertEquals(child1, tree.getChildAtIndex(node, 1));
        assertEquals(child2, tree.getChildAtIndex(node, 2));
    }

    @Test
    @DisplayName("test_child_count")
    void testChildCount() {
        TaffyTree tree = new TaffyTree();
        NodeId child0 = tree.newLeaf(new Style());
        NodeId child1 = tree.newLeaf(new Style());
        NodeId node = tree.newWithChildren(new Style(), child0, child1);

        assertEquals(2, tree.childCount(node));
        assertEquals(0, tree.childCount(child0));
        assertEquals(0, tree.childCount(child1));
    }

    @Test
    @DisplayName("test_children")
    void testChildren() {
        TaffyTree tree = new TaffyTree();
        NodeId child0 = tree.newLeaf(new Style());
        NodeId child1 = tree.newLeaf(new Style());
        NodeId node = tree.newWithChildren(new Style(), child0, child1);

        List<NodeId> children = tree.getChildren(node);
        assertEquals(2, children.size());
        assertEquals(child0, children.get(0));
        assertEquals(child1, children.get(1));

        assertTrue(tree.getChildren(child0).isEmpty());
    }

    // ==================== Remove Node Tests ====================

    @Test
    @DisplayName("remove_node_should_remove")
    void removeNodeShouldRemove() {
        TaffyTree tree = new TaffyTree();
        NodeId node = tree.newLeaf(new Style());
        
        tree.remove(node);
        // Node should be removed from tree
        assertFalse(tree.containsNode(node));
    }

    @Test
    @DisplayName("remove_node_should_detach_hierarchy")
    void removeNodeShouldDetachHierarchy() {
        TaffyTree tree = new TaffyTree();
        
        // Build a linear tree layout: <0> <- <1> <- <2>
        NodeId node2 = tree.newLeaf(new Style());
        NodeId node1 = tree.newWithChildren(new Style(), node2);
        NodeId node0 = tree.newWithChildren(new Style(), node1);

        // Both node0 and node1 should have 1 child node
        assertEquals(1, tree.getChildren(node0).size());
        assertEquals(node1, tree.getChildren(node0).get(0));
        assertEquals(1, tree.getChildren(node1).size());
        assertEquals(node2, tree.getChildren(node1).get(0));

        // Disconnect the tree: <0> <2>
        tree.remove(node1);

        // Both remaining nodes should have no child nodes
        assertTrue(tree.getChildren(node0).isEmpty());
        assertTrue(tree.getChildren(node2).isEmpty());
    }

    @Test
    @DisplayName("remove_last_node")
    void removeLastNode() {
        TaffyTree tree = new TaffyTree();
        
        NodeId parent = tree.newLeaf(new Style());
        NodeId child = tree.newLeaf(new Style());
        tree.addChild(parent, child);

        tree.remove(child);
        tree.remove(parent);
        
        // Both nodes should be removed
        assertFalse(tree.containsNode(child));
        assertFalse(tree.containsNode(parent));
    }

    @Test
    @DisplayName("remove_child_updates_parents")
    void removeChildUpdatesParents() {
        TaffyTree tree = new TaffyTree();
        
        NodeId parent = tree.newLeaf(new Style());
        NodeId child = tree.newLeaf(new Style());
        
        tree.addChild(parent, child);
        tree.remove(parent);

        // Once the parent is removed this shouldn't cause issues
        tree.setChildren(child);
        assertEquals(0, tree.childCount(child));
    }

    // ==================== Style Tests ====================

    @Test
    @DisplayName("test_set_style")
    void testSetStyle() {
        TaffyTree tree = new TaffyTree();
        
        NodeId node = tree.newLeaf(new Style());
        assertEquals(Display.FLEX, tree.getStyle(node).display);

        Style newStyle = new Style();
        newStyle.display = Display.NONE;
        tree.setStyle(node, newStyle);
        assertEquals(Display.NONE, tree.getStyle(node).display);
    }

    @Test
    @DisplayName("test_style")
    void testStyle() {
        TaffyTree tree = new TaffyTree();
        
        Style style = new Style();
        style.display = Display.NONE;
        style.flexDirection = FlexDirection.ROW_REVERSE;
        
        NodeId node = tree.newLeaf(style);
        
        Style retrievedStyle = tree.getStyle(node);
        assertNotNull(retrievedStyle);
        assertEquals(Display.NONE, retrievedStyle.display);
        assertEquals(FlexDirection.ROW_REVERSE, retrievedStyle.flexDirection);
    }

    // ==================== Layout Tests ====================

    @Test
    @DisplayName("test_layout")
    void testLayout() {
        TaffyTree tree = new TaffyTree();
        NodeId node = tree.newLeaf(new Style());
        
        Layout layout = tree.getLayout(node);
        assertNotNull(layout);
    }

    @Test
    @DisplayName("has_unconsumed_layout_tracks_layout_changes")
    void hasUnconsumedLayoutTracksLayoutChanges() {
        TaffyTree tree = new TaffyTree();

        Style style = new Style();
        style.size = new Size<>(Dimension.length(100f), Dimension.length(100f));
        NodeId node = tree.newLeaf(style);

        // Initial state: no computed layout to consume
        assertFalse(tree.hasUnconsumedLayout(node));

        tree.computeLayout(node, Size.maxContent());
        assertTrue(tree.hasUnconsumedLayout(node));

        tree.acknowledgeLayout(node);
        assertFalse(tree.hasUnconsumedLayout(node));

        // Recomputing without changes should not flip the flag back to true
        tree.computeLayout(node, Size.maxContent());
        assertFalse(tree.hasUnconsumedLayout(node));

        // Changing style should produce a layout change
        Style newStyle = new Style();
        newStyle.size = new Size<>(Dimension.length(200f), Dimension.length(200f));
        tree.setStyle(node, newStyle);

        tree.computeLayout(node, Size.maxContent());
        assertTrue(tree.hasUnconsumedLayout(node));
    }

    @Test
    @DisplayName("test_mark_dirty")
    void testMarkDirty() {
        TaffyTree tree = new TaffyTree();
        NodeId child0 = tree.newLeaf(new Style());
        NodeId child1 = tree.newLeaf(new Style());
        NodeId node = tree.newWithChildren(new Style(), child0, child1);

        tree.computeLayout(node, Size.maxContent());

        assertFalse(tree.isDirty(child0));
        assertFalse(tree.isDirty(child1));
        assertFalse(tree.isDirty(node));

        tree.markDirty(node);
        assertFalse(tree.isDirty(child0));
        assertFalse(tree.isDirty(child1));
        assertTrue(tree.isDirty(node));

        tree.computeLayout(node, Size.maxContent());
        tree.markDirty(child0);
        assertTrue(tree.isDirty(child0));
        assertFalse(tree.isDirty(child1));
        assertTrue(tree.isDirty(node));
    }

    @Test
    @DisplayName("compute_layout_should_produce_valid_result")
    void computeLayoutShouldProduceValidResult() {
        TaffyTree tree = new TaffyTree();
        
        Style style = new Style();
        style.size = new Size<>(Dimension.length(10f), Dimension.length(10f));
        NodeId node = tree.newLeaf(style);
        
        tree.computeLayout(node, new Size<>(
            AvailableSpace.definite(100f),
            AvailableSpace.definite(100f)
        ));
        
        Layout layout = tree.getLayout(node);
        assertNotNull(layout);
    }

    @Test
    @DisplayName("make_sure_layout_location_is_top_left")
    void makeSureLayoutLocationIsTopLeft() {
        TaffyTree tree = new TaffyTree();
        
        Style childStyle = new Style();
        childStyle.size = new Size<>(Dimension.percent(1f), Dimension.percent(1f));
        NodeId child = tree.newLeaf(childStyle);
        
        Style rootStyle = new Style();
        rootStyle.size = new Size<>(Dimension.length(100f), Dimension.length(100f));
        rootStyle.padding = new Rect<>(
            LengthPercentage.length(10f),
            LengthPercentage.length(20f),
            LengthPercentage.length(30f),
            LengthPercentage.length(40f)
        );
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, Size.maxContent());

        // If Layout::location represents top-left coord, 'node' location
        // must be (due applied 'root' padding): {x: 10, y: 30}.
        Layout layout = tree.getLayout(child);
        assertEquals(10f, layout.location().x, 0.1f);
        assertEquals(30f, layout.location().y, 0.1f);
    }

    @Test
    @DisplayName("set_children_reparents")
    void setChildrenReparents() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(new Style());
        NodeId oldParent = tree.newWithChildren(new Style(), child);

        NodeId newParent = tree.newLeaf(new Style());
        tree.setChildren(newParent, child);

        assertTrue(tree.getChildren(oldParent).isEmpty());
        assertEquals(1, tree.getChildren(newParent).size());
        assertEquals(child, tree.getChildren(newParent).get(0));
    }

    // ==================== Measure Function Tests ====================

    @Test
    @DisplayName("set_measure")
    void setMeasure() {
        TaffyTree tree = new TaffyTree();
        
        MeasureFunc measureFunc = (knownDimensions, availableSpace) -> {
            float w = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : 200f;
            float h = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : 200f;
            return new FloatSize(w, h);
        };
        
        NodeId node = tree.newLeafWithMeasure(new Style(), measureFunc);
        tree.computeLayout(node, Size.maxContent());
        assertEquals(200f, tree.getLayout(node).size().width, 0.1f);

        // Update measure function
        MeasureFunc newMeasureFunc = (knownDimensions, availableSpace) -> {
            float w = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : 100f;
            float h = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : 100f;
            return new FloatSize(w, h);
        };
        
        tree.setMeasureFunc(node, newMeasureFunc);
        tree.computeLayout(node, Size.maxContent());
        assertEquals(100f, tree.getLayout(node).size().width, 0.1f);
    }

    @Test
    @DisplayName("set_measure_of_previously_unmeasured_node")
    void setMeasureOfPreviouslyUnmeasuredNode() {
        TaffyTree tree = new TaffyTree();
        
        NodeId node = tree.newLeaf(new Style());
        tree.computeLayout(node, Size.maxContent());
        assertEquals(0f, tree.getLayout(node).size().width, 0.1f);

        MeasureFunc measureFunc = (knownDimensions, availableSpace) -> {
            float w = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : 100f;
            float h = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : 100f;
            return new FloatSize(w, h);
        };
        
        tree.setMeasureFunc(node, measureFunc);
        tree.computeLayout(node, Size.maxContent());
        assertEquals(100f, tree.getLayout(node).size().width, 0.1f);
    }
}
