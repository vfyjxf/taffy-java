package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Basic tests for the TaffyTree layout engine.
 */
public class TaffyTreeTest {

    @Test
    void testCreateLeafNode() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(100), TaffyDimension.length(100));

        NodeId node = tree.newLeaf(style);

        assertNotNull(node);
        assertEquals(1, tree.totalNodeCount());
        assertEquals(0, tree.childCount(node));
    }

    @Test
    void testCreateNodeWithChildren() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(50), TaffyDimension.length(50));

        NodeId child1 = tree.newLeaf(childStyle);
        NodeId child2 = tree.newLeaf(childStyle);

        TaffyStyle parentStyle = new TaffyStyle();
        parentStyle.display = TaffyDisplay.FLEX;

        NodeId parent = tree.newWithChildren(parentStyle, child1, child2);

        assertEquals(3, tree.totalNodeCount());
        assertEquals(2, tree.childCount(parent));
        assertEquals(parent, tree.getParent(child1));
        assertEquals(parent, tree.getParent(child2));
    }

    @Test
    void testBasicFlexboxLayout() {
        TaffyTree tree = new TaffyTree();

        // Create children with fixed sizes
        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(100), TaffyDimension.length(100));

        NodeId child1 = tree.newLeaf(childStyle);
        NodeId child2 = tree.newLeaf(childStyle);

        // Create parent with flexbox
        TaffyStyle parentStyle = new TaffyStyle();
        parentStyle.display = TaffyDisplay.FLEX;
        parentStyle.flexDirection = FlexDirection.ROW;
        parentStyle.size = new TaffySize<>(TaffyDimension.length(400), TaffyDimension.length(200));

        NodeId parent = tree.newWithChildren(parentStyle, child1, child2);

        // Compute layout
        tree.computeLayout(parent, new TaffySize<>(AvailableSpace.definite(400f), AvailableSpace.definite(200f)));

        // Verify parent layout
        Layout parentLayout = tree.getLayout(parent);
        assertNotNull(parentLayout);
        assertEquals(400f, parentLayout.size().width, 0.1f);
        assertEquals(200f, parentLayout.size().height, 0.1f);

        // Verify child layouts
        Layout child1Layout = tree.getLayout(child1);
        Layout child2Layout = tree.getLayout(child2);

        assertNotNull(child1Layout);
        assertNotNull(child2Layout);

        // Child 1 should be at x=0
        assertEquals(0f, child1Layout.location().x, 0.1f);

        // Child 2 should be after child 1
        assertTrue(child2Layout.location().x >= child1Layout.size().width);
    }

    @Test
    void testFlexGrow() {
        TaffyTree tree = new TaffyTree();

        // Create children with flex-grow
        TaffyStyle child1Style = new TaffyStyle();
        child1Style.flexGrow = 1.0f; child1Style.flexShrink = 1.0f; child1Style.flexBasis = TaffyDimension.AUTO;

        TaffyStyle child2Style = new TaffyStyle();
        child2Style.flexGrow = 2.0f; child2Style.flexShrink = 1.0f; child2Style.flexBasis = TaffyDimension.AUTO;

        NodeId child1 = tree.newLeaf(child1Style);
        NodeId child2 = tree.newLeaf(child2Style);

        // Create parent with flexbox
        TaffyStyle parentStyle = new TaffyStyle();
        parentStyle.display = TaffyDisplay.FLEX;
        parentStyle.flexDirection = FlexDirection.ROW;
        parentStyle.size = new TaffySize<>(TaffyDimension.length(300), TaffyDimension.length(100));

        NodeId parent = tree.newWithChildren(parentStyle, child1, child2);

        // Compute layout
        tree.computeLayout(parent, new TaffySize<>(AvailableSpace.definite(300f), AvailableSpace.definite(100f)));

        // Child 1 should get 1/3 of the space, child 2 should get 2/3
        Layout child1Layout = tree.getLayout(child1);
        Layout child2Layout = tree.getLayout(child2);

        assertNotNull(child1Layout);
        assertNotNull(child2Layout);

        // Approximate ratio check (allowing for rounding)
        float ratio = child2Layout.size().width / child1Layout.size().width;
        assertTrue(ratio >= 1.8f && ratio <= 2.2f,
            "Child2 should be approximately 2x the width of child1, ratio was: " + ratio);
    }

    @Test
    void testBlockLayout() {
        TaffyTree tree = new TaffyTree();

        // Create children with fixed heights
        TaffyStyle child1Style = new TaffyStyle();
        child1Style.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(50));

        TaffyStyle child2Style = new TaffyStyle();
        child2Style.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(50));

        NodeId child1 = tree.newLeaf(child1Style);
        NodeId child2 = tree.newLeaf(child2Style);

        // Create parent with block layout
        TaffyStyle parentStyle = new TaffyStyle();
        parentStyle.display = TaffyDisplay.BLOCK;
        parentStyle.size = new TaffySize<>(TaffyDimension.length(200), TaffyDimension.AUTO);

        NodeId parent = tree.newWithChildren(parentStyle, child1, child2);

        // Compute layout
        tree.computeLayout(parent, new TaffySize<>(AvailableSpace.definite(200f), AvailableSpace.maxContent()));

        // Verify children are stacked vertically
        Layout child1Layout = tree.getLayout(child1);
        Layout child2Layout = tree.getLayout(child2);

        assertNotNull(child1Layout);
        assertNotNull(child2Layout);

        // Child 2 should be below child 1
        assertTrue(child2Layout.location().y >= child1Layout.location().y + child1Layout.size().height - 1,
            "Child 2 should be below child 1");
    }

    @Test
    void testPaddingAndBorder() {
        TaffyTree tree = new TaffyTree();

        // Create a child
        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(100), TaffyDimension.length(100));

        NodeId child = tree.newLeaf(childStyle);

        // Create parent with padding and border
        TaffyStyle parentStyle = new TaffyStyle();
        parentStyle.display = TaffyDisplay.FLEX;
        parentStyle.padding = TaffyRect.all(LengthPercentage.length(10f));
        parentStyle.border = TaffyRect.all(LengthPercentage.length(5f));

        NodeId parent = tree.newWithChildren(parentStyle, child);

        // Compute layout
        tree.computeLayout(parent, new TaffySize<>(AvailableSpace.maxContent(), AvailableSpace.maxContent()));

        // Child should be offset by padding + border
        Layout childLayout = tree.getLayout(child);
        assertNotNull(childLayout);

        // Child should be at padding + border offset
        assertEquals(15f, childLayout.location().x, 0.1f);
        assertEquals(15f, childLayout.location().y, 0.1f);
    }

    @Test
    void testMeasureFunction() {
        TaffyTree tree = new TaffyTree();

        // Create a leaf with a measure function
        TaffyStyle style = new TaffyStyle();

        NodeId leaf = tree.newLeafWithMeasure(style, (knownDimensions, availableSpace) -> {
            // Return a fixed size based on text measurement simulation
            float width = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : 150f;
            float height = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : 30f;
            return new FloatSize(width, height);
        });

        // Compute layout
        tree.computeLayout(leaf, new TaffySize<>(AvailableSpace.maxContent(), AvailableSpace.maxContent()));

        Layout layout = tree.getLayout(leaf);
        assertNotNull(layout);
        assertEquals(150f, layout.size().width, 0.1f);
        assertEquals(30f, layout.size().height, 0.1f);
    }

    @Test
    void testMarkDirty() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(100), TaffyDimension.length(100));

        NodeId node = tree.newLeaf(style);

        // Initially node should be dirty (no cached layout)
        assertTrue(tree.isDirty(node));

        // Compute layout
        tree.computeLayout(node, new TaffySize<>(AvailableSpace.maxContent(), AvailableSpace.maxContent()));

        // After layout, node should not be dirty
        assertFalse(tree.isDirty(node));

        // Mark dirty
        tree.markDirty(node);

        // Now node should be dirty again
        assertTrue(tree.isDirty(node));
    }

    @Test
    void testRemoveNode() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle style = new TaffyStyle();
        NodeId child = tree.newLeaf(style);
        NodeId parent = tree.newWithChildren(style, child);

        assertEquals(2, tree.totalNodeCount());
        assertEquals(1, tree.childCount(parent));

        // Remove child
        tree.removeChild(parent, child);

        assertEquals(2, tree.totalNodeCount()); // Node still exists, just detached
        assertEquals(0, tree.childCount(parent));
        assertNull(tree.getParent(child));
    }

    @Test
    void testDisplayNone() {
        TaffyTree tree = new TaffyTree();

        // Create a hidden child
        TaffyStyle hiddenStyle = new TaffyStyle();
        hiddenStyle.display = TaffyDisplay.NONE;
        hiddenStyle.size = new TaffySize<>(TaffyDimension.length(100), TaffyDimension.length(100));

        TaffyStyle visibleStyle = new TaffyStyle();
        visibleStyle.size = new TaffySize<>(TaffyDimension.length(100), TaffyDimension.length(100));

        NodeId hidden = tree.newLeaf(hiddenStyle);
        NodeId visible = tree.newLeaf(visibleStyle);

        TaffyStyle parentStyle = new TaffyStyle();
        parentStyle.display = TaffyDisplay.FLEX;

        NodeId parent = tree.newWithChildren(parentStyle, hidden, visible);

        // Compute layout
        tree.computeLayout(parent, new TaffySize<>(AvailableSpace.maxContent(), AvailableSpace.maxContent()));

        // Hidden node should have zero size
        Layout hiddenLayout = tree.getLayout(hidden);
        assertEquals(0f, hiddenLayout.size().width, 0.1f);
        assertEquals(0f, hiddenLayout.size().height, 0.1f);

        // Visible node should have its size
        Layout visibleLayout = tree.getLayout(visible);
        assertEquals(100f, visibleLayout.size().width, 0.1f);
        assertEquals(100f, visibleLayout.size().height, 0.1f);
    }
}
