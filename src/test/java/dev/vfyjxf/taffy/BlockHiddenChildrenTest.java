package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression tests for {@code display: none} descendants of block containers.
 *
 * <p>Block layout builds its item list from the children whose
 * {@link dev.vfyjxf.taffy.style.BoxGenerationMode} is not {@code NONE}, so hidden children are
 * never laid out by the normal flow/absolute passes. They must still be reset to a zero layout on
 * every relayout, otherwise a child that is toggled to {@code display: none} keeps the unrounded
 * and final layout values from the last visible pass.</p>
 */
public class BlockHiddenChildrenTest {

    private static final float EPSILON = 0.1f;

    @Test
    @DisplayName("block display:none child layout is cleared on relayout")
    void blockDisplayNoneChildLayoutIsCleared() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle inFlowStyle = blockBox(40f, 40f);
        inFlowStyle.padding = TaffyRect.all(LengthPercentage.length(2f));
        inFlowStyle.border = TaffyRect.all(LengthPercentage.length(1f));
        NodeId inFlow = tree.newLeaf(inFlowStyle);

        TaffyStyle absoluteStyle = blockBox(30f, 30f);
        absoluteStyle.position = TaffyPosition.ABSOLUTE;
        absoluteStyle.padding = TaffyRect.all(LengthPercentage.length(3f));
        NodeId absolute = tree.newLeaf(absoluteStyle);

        NodeId visibleSibling = tree.newLeaf(blockBox(20f, 20f));

        NodeId root = tree.newWithChildren(blockBox(200f, 200f), inFlow, absolute, visibleSibling);

        // First layout: every child is visible and must get a real layout.
        tree.computeLayout(root, TaffySize.maxContent());

        assertEquals(40f, tree.getLayout(inFlow).size().width, EPSILON, "in-flow child width before hiding");
        assertEquals(40f, tree.getLayout(inFlow).size().height, EPSILON, "in-flow child height before hiding");
        assertEquals(30f, tree.getLayout(absolute).size().width, EPSILON, "absolute child width before hiding");
        assertEquals(30f, tree.getLayout(absolute).size().height, EPSILON, "absolute child height before hiding");
        assertEquals(20f, tree.getLayout(visibleSibling).size().width, EPSILON, "visible sibling width");

        // Second layout: toggle both the in-flow and the out-of-flow child to display:none.
        tree.setStyle(inFlow, hidden(inFlowStyle));
        tree.setStyle(absolute, hidden(absoluteStyle));
        tree.computeLayout(root, TaffySize.maxContent());

        assertLayoutZeroed(tree, inFlow, "hidden in-flow child");
        assertLayoutZeroed(tree, absolute, "hidden absolute child");

        // The visible sibling is not hidden and must still be laid out normally.
        assertEquals(20f, tree.getLayout(visibleSibling).size().width, EPSILON, "visible sibling width after hiding");
        assertEquals(20f, tree.getLayout(visibleSibling).size().height, EPSILON, "visible sibling height after hiding");
        assertEquals(0f, tree.getLayout(visibleSibling).location().y, EPSILON, "visible sibling top after hiding");

        // Third layout: restoring display reproduces the original layout values.
        tree.setStyle(inFlow, inFlowStyle);
        tree.setStyle(absolute, absoluteStyle);
        tree.computeLayout(root, TaffySize.maxContent());

        assertEquals(40f, tree.getLayout(inFlow).size().width, EPSILON, "in-flow child width after restoring");
        assertEquals(40f, tree.getLayout(inFlow).size().height, EPSILON, "in-flow child height after restoring");
        assertEquals(30f, tree.getLayout(absolute).size().width, EPSILON, "absolute child width after restoring");
        assertEquals(30f, tree.getLayout(absolute).size().height, EPSILON, "absolute child height after restoring");
    }

    @Test
    @DisplayName("hiding a block parent zeroes its out-of-flow child layout")
    void hidingBlockParentClearsOutOfFlowChildLayout() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle outOfFlowStyle = new TaffyStyle();
        outOfFlowStyle.position = TaffyPosition.ABSOLUTE;
        outOfFlowStyle.size = new TaffySize<>(TaffyDimension.length(30f), TaffyDimension.length(30f));
        NodeId outOfFlow = tree.newLeaf(outOfFlowStyle);

        TaffyStyle parentStyle = blockBox(50f, 50f);
        NodeId parent = tree.newWithChildren(parentStyle, outOfFlow);

        NodeId root = tree.newWithChildren(blockBox(200f, 200f), parent);

        tree.computeLayout(root, TaffySize.maxContent());
        assertEquals(50f, tree.getLayout(parent).size().width, EPSILON, "parent width before hiding");
        assertEquals(30f, tree.getLayout(outOfFlow).size().width, EPSILON, "out-of-flow width before hiding");
        assertEquals(30f, tree.getLayout(outOfFlow).size().height, EPSILON, "out-of-flow height before hiding");

        tree.setStyle(parent, hidden(parentStyle));
        tree.computeLayout(root, TaffySize.maxContent());

        assertLayoutZeroed(tree, parent, "hidden block parent");
        assertLayoutZeroed(tree, outOfFlow, "out-of-flow child of a hidden block parent");
    }

    @Test
    @DisplayName("hidden block subtree recursively clears nested visible descendants")
    void hiddenBlockSubtreeRecursivelyClearsDescendants() {
        TaffyTree tree = new TaffyTree();

        NodeId leaf = tree.newLeaf(blockBox(5f, 5f));
        NodeId nested = tree.newWithChildren(blockBox(50f, 20f), leaf);

        TaffyStyle absoluteStyle = new TaffyStyle();
        absoluteStyle.position = TaffyPosition.ABSOLUTE;
        absoluteStyle.size = new TaffySize<>(TaffyDimension.length(7f), TaffyDimension.length(7f));
        NodeId absolute = tree.newLeaf(absoluteStyle);

        TaffyStyle panelStyle = blockBox(100f, 100f);
        NodeId panel = tree.newWithChildren(panelStyle, nested, absolute);

        NodeId root = tree.newWithChildren(blockBox(200f, 200f), panel);

        tree.computeLayout(root, TaffySize.maxContent());
        assertEquals(100f, tree.getLayout(panel).size().width, EPSILON, "panel width before hiding");
        assertEquals(50f, tree.getLayout(nested).size().width, EPSILON, "nested block width before hiding");
        assertEquals(5f, tree.getLayout(leaf).size().width, EPSILON, "leaf width before hiding");
        assertEquals(7f, tree.getLayout(absolute).size().width, EPSILON, "absolute width before hiding");

        tree.setStyle(panel, hidden(panelStyle));
        tree.computeLayout(root, TaffySize.maxContent());

        assertLayoutZeroed(tree, panel, "hidden block panel");
        assertLayoutZeroed(tree, nested, "in-flow block grandchild of a hidden panel");
        assertLayoutZeroed(tree, leaf, "block great-grandchild of a hidden panel");
        assertLayoutZeroed(tree, absolute, "out-of-flow grandchild of a hidden panel");
    }

    @Test
    @DisplayName("initially hidden block child is laid out once display is restored")
    void initiallyHiddenBlockChildIsLaidOutWhenShown() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle childStyle = blockBox(40f, 40f);
        NodeId child = tree.newLeaf(hidden(childStyle));
        NodeId root = tree.newWithChildren(blockBox(200f, 200f), child);

        tree.computeLayout(root, TaffySize.maxContent());
        assertLayoutZeroed(tree, child, "initially hidden block child");

        tree.setStyle(child, childStyle);
        tree.computeLayout(root, TaffySize.maxContent());

        assertEquals(40f, tree.getLayout(child).size().width, EPSILON, "child width after restoring display");
        assertEquals(40f, tree.getLayout(child).size().height, EPSILON, "child height after restoring display");
    }

    private static TaffyStyle blockBox(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle hidden(TaffyStyle visibleStyle) {
        TaffyStyle hiddenStyle = visibleStyle.copy();
        hiddenStyle.display = TaffyDisplay.NONE;
        return hiddenStyle;
    }

    private static void assertLayoutZeroed(TaffyTree tree, NodeId node, String description) {
        Layout unrounded = tree.getUnroundedLayout(node);
        assertNotNull(unrounded, description + ": unrounded layout must exist");
        assertZeroLayout(unrounded, description + " [unrounded]");

        Layout finalLayout = tree.getLayout(node);
        assertNotNull(finalLayout, description + ": final layout must exist");
        assertZeroLayout(finalLayout, description + " [final]");
    }

    private static void assertZeroLayout(Layout layout, String description) {
        assertEquals(0f, layout.location().x, EPSILON, description + " location.x");
        assertEquals(0f, layout.location().y, EPSILON, description + " location.y");
        assertEquals(0f, layout.size().width, EPSILON, description + " size.width");
        assertEquals(0f, layout.size().height, EPSILON, description + " size.height");
        assertEquals(0f, layout.contentSize().width, EPSILON, description + " contentSize.width");
        assertEquals(0f, layout.contentSize().height, EPSILON, description + " contentSize.height");
        assertEquals(0f, layout.padding().left, EPSILON, description + " padding.left");
        assertEquals(0f, layout.border().left, EPSILON, description + " border.left");
        assertEquals(0f, layout.margin().left, EPSILON, description + " margin.left");
    }
}
