package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.Rect;
import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.Dimension;
import dev.vfyjxf.taffy.style.Display;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.Style;
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
        Style style = new Style();
        style.size = new Size<>(Dimension.length(100), Dimension.length(100));

        NodeId node = tree.newLeaf(style);

        assertNotNull(node);
        assertEquals(1, tree.totalNodeCount());
        assertEquals(0, tree.childCount(node));
    }

    @Test
    void testCreateNodeWithChildren() {
        TaffyTree tree = new TaffyTree();

        Style childStyle = new Style();
        childStyle.size = new Size<>(Dimension.length(50), Dimension.length(50));

        NodeId child1 = tree.newLeaf(childStyle);
        NodeId child2 = tree.newLeaf(childStyle);

        Style parentStyle = new Style();
        parentStyle.display = Display.FLEX;

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
        Style childStyle = new Style();
        childStyle.size = new Size<>(Dimension.length(100), Dimension.length(100));

        NodeId child1 = tree.newLeaf(childStyle);
        NodeId child2 = tree.newLeaf(childStyle);

        // Create parent with flexbox
        Style parentStyle = new Style();
        parentStyle.display = Display.FLEX;
        parentStyle.flexDirection = FlexDirection.ROW;
        parentStyle.size = new Size<>(Dimension.length(400), Dimension.length(200));

        NodeId parent = tree.newWithChildren(parentStyle, child1, child2);

        // Compute layout
        tree.computeLayout(parent, new Size<>(AvailableSpace.definite(400f), AvailableSpace.definite(200f)));

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
        Style child1Style = new Style();
        child1Style.flexGrow = 1.0f;

        Style child2Style = new Style();
        child2Style.flexGrow = 2.0f;

        NodeId child1 = tree.newLeaf(child1Style);
        NodeId child2 = tree.newLeaf(child2Style);

        // Create parent with flexbox
        Style parentStyle = new Style();
        parentStyle.display = Display.FLEX;
        parentStyle.flexDirection = FlexDirection.ROW;
        parentStyle.size = new Size<>(Dimension.length(300), Dimension.length(100));

        NodeId parent = tree.newWithChildren(parentStyle, child1, child2);

        // Compute layout
        tree.computeLayout(parent, new Size<>(AvailableSpace.definite(300f), AvailableSpace.definite(100f)));

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
        Style child1Style = new Style();
        child1Style.size = new Size<>(Dimension.AUTO, Dimension.length(50));

        Style child2Style = new Style();
        child2Style.size = new Size<>(Dimension.AUTO, Dimension.length(50));

        NodeId child1 = tree.newLeaf(child1Style);
        NodeId child2 = tree.newLeaf(child2Style);

        // Create parent with block layout
        Style parentStyle = new Style();
        parentStyle.display = Display.BLOCK;
        parentStyle.size = new Size<>(Dimension.length(200), Dimension.AUTO);

        NodeId parent = tree.newWithChildren(parentStyle, child1, child2);

        // Compute layout
        tree.computeLayout(parent, new Size<>(AvailableSpace.definite(200f), AvailableSpace.maxContent()));

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
        Style childStyle = new Style();
        childStyle.size = new Size<>(Dimension.length(100), Dimension.length(100));

        NodeId child = tree.newLeaf(childStyle);

        // Create parent with padding and border
        Style parentStyle = new Style();
        parentStyle.display = Display.FLEX;
        parentStyle.padding = Rect.all(LengthPercentage.length(10f));
        parentStyle.border = Rect.all(LengthPercentage.length(5f));

        NodeId parent = tree.newWithChildren(parentStyle, child);

        // Compute layout
        tree.computeLayout(parent, new Size<>(AvailableSpace.maxContent(), AvailableSpace.maxContent()));

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
        Style style = new Style();

        NodeId leaf = tree.newLeafWithMeasure(style, (knownDimensions, availableSpace) -> {
            // Return a fixed size based on text measurement simulation
            float width = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : 150f;
            float height = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : 30f;
            return new FloatSize(width, height);
        });

        // Compute layout
        tree.computeLayout(leaf, new Size<>(AvailableSpace.maxContent(), AvailableSpace.maxContent()));

        Layout layout = tree.getLayout(leaf);
        assertNotNull(layout);
        assertEquals(150f, layout.size().width, 0.1f);
        assertEquals(30f, layout.size().height, 0.1f);
    }

    @Test
    void testMarkDirty() {
        TaffyTree tree = new TaffyTree();

        Style style = new Style();
        style.size = new Size<>(Dimension.length(100), Dimension.length(100));

        NodeId node = tree.newLeaf(style);

        // Initially node should be dirty (no cached layout)
        assertTrue(tree.isDirty(node));

        // Compute layout
        tree.computeLayout(node, new Size<>(AvailableSpace.maxContent(), AvailableSpace.maxContent()));

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

        Style style = new Style();
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
        Style hiddenStyle = new Style();
        hiddenStyle.display = Display.NONE;
        hiddenStyle.size = new Size<>(Dimension.length(100), Dimension.length(100));

        Style visibleStyle = new Style();
        visibleStyle.size = new Size<>(Dimension.length(100), Dimension.length(100));

        NodeId hidden = tree.newLeaf(hiddenStyle);
        NodeId visible = tree.newLeaf(visibleStyle);

        Style parentStyle = new Style();
        parentStyle.display = Display.FLEX;

        NodeId parent = tree.newWithChildren(parentStyle, hidden, visible);

        // Compute layout
        tree.computeLayout(parent, new Size<>(AvailableSpace.maxContent(), AvailableSpace.maxContent()));

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
