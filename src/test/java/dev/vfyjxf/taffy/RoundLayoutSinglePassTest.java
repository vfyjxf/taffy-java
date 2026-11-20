package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.ScrollableOverflow;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.RoundLayout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P11 tests for the single-pass rounding implementation.
 *
 * <p>{@code RoundLayout.roundLayout} used to walk the tree through the public
 * {@code getUnroundedLayout}/{@code setLayout}/{@code getChildren} accessors, which meant several
 * hash lookups per node and an {@code Object[]}/boxed-{@code Float} stack frame per edge. The pass
 * now lives in {@link TaffyTree#roundLayout(NodeId)} and reads the unrounded layout and the child
 * list from one {@code NodeData} fetch. These tests pin the observable behavior: the exact rounded
 * numbers (including NaN handling and cumulative coordinates), the pre-order listener
 * notification, the dirty-descendant propagation, and the skip rule for unknown nodes / nodes
 * without an unrounded layout.</p>
 */
public class RoundLayoutSinglePassTest {

    private static Layout layout(float x, float y, float width, float height) {
        return new Layout(
            0,
            new FloatPoint(x, y),
            new FloatSize(width, height),
            new FloatSize(width, height),
            ScrollableOverflow.ZERO,
            FloatSize.zero(),
            FloatRect.zero(),
            FloatRect.zero(),
            FloatRect.zero()
        );
    }

    @Test
    @DisplayName("P11: tree.roundLayout rounds cumulative positions and rounded-edge sizes")
    void roundsCumulativePositionsThroughTreeEntryPoint() {
        TaffyTree tree = new TaffyTree();

        NodeId grandchild = tree.newLeaf(new TaffyStyle());
        NodeId parent = tree.newWithChildren(new TaffyStyle(), grandchild);
        NodeId root = tree.newWithChildren(new TaffyStyle(), parent);

        tree.setUnroundedLayout(root, layout(0f, 0f, 10f, 10f));
        tree.setUnroundedLayout(parent, layout(0.5f, 0.5f, 2.5f, 2.5f));
        tree.setUnroundedLayout(grandchild, layout(0.5f, 0.5f, 1f, 1f));

        tree.roundLayout(root);

        Layout rootLayout = tree.getLayout(root);
        assertEquals(0f, rootLayout.location().x, 0f, "root x");
        assertEquals(0f, rootLayout.location().y, 0f, "root y");
        assertEquals(10f, rootLayout.size().width, 0f, "root width");
        assertEquals(10f, rootLayout.size().height, 0f, "root height");

        Layout parentLayout = tree.getLayout(parent);
        assertEquals(1f, parentLayout.location().x, 0f, "parent x");
        assertEquals(1f, parentLayout.location().y, 0f, "parent y");
        assertEquals(2f, parentLayout.size().width, 0f, "parent width");
        assertEquals(2f, parentLayout.size().height, 0f, "parent height");

        Layout grandchildLayout = tree.getLayout(grandchild);
        assertEquals(0f, grandchildLayout.location().x, 0f, "grandchild x");
        assertEquals(0f, grandchildLayout.location().y, 0f, "grandchild y");

        // The unrounded layouts are only read, never replaced.
        assertEquals(0.5f, tree.getUnroundedLayout(grandchild).location().x, 0f, "unrounded kept");
    }

    @Test
    @DisplayName("P11: tree.roundLayout keeps NaN/negative/overflow/border/padding semantics")
    void roundsNaNNegativeAndAuxiliaryComponents() {
        TaffyTree tree = new TaffyTree();
        NodeId root = tree.newLeaf(new TaffyStyle());

        tree.setUnroundedLayout(root, new Layout(
            3,
            new FloatPoint(Float.NaN, 1.5f),
            new FloatSize(10.5f, 2.25f),
            new FloatSize(Float.NaN, 3.75f),
            new ScrollableOverflow(FloatRect.of(1.5f, 2.5f, -1.5f, -2.5f), true, false),
            new FloatSize(0.5f, 1.5f),
            new FloatRect(Float.NaN, 1.5f, 2.5f, -1.5f),
            FloatRect.of(0.5f, -0.5f, 2.5f, -2.5f),
            FloatRect.zero()
        ));

        tree.roundLayout(root);

        Layout rounded = tree.getLayout(root);
        assertEquals(3, rounded.order(), "order is carried through");
        assertEquals(0f, rounded.location().x, 0f, "NaN location becomes 0");
        assertEquals(2f, rounded.location().y, 0f, "round(1.5) ties away from zero");
        assertEquals(11f, rounded.size().width, 0f, "width = round(10.5)");
        assertEquals(2f, rounded.size().height, 0f, "height = round(3.75) - round(1.5)");
        assertEquals(0f, rounded.contentSize().width, 0f, "NaN content width becomes 0");
        assertEquals(3f, rounded.contentSize().height, 0f, "content height = 5 - 2");

        assertEquals(1f, rounded.scrollbarSize().width, 0f, "scrollbar width");
        assertEquals(2f, rounded.scrollbarSize().height, 0f, "scrollbar height");

        FloatRect overflow = rounded.scrollableOverflow().rect();
        assertEquals(2f, overflow.left, 0f, "overflow left");
        assertEquals(3f, overflow.right, 0f, "overflow right");
        assertEquals(-2f, overflow.top, 0f, "overflow top");
        assertEquals(-3f, overflow.bottom, 0f, "overflow bottom");
        assertTrue(rounded.scrollableOverflow().reversedX(), "reversedX flag kept");
        assertFalse(rounded.scrollableOverflow().reversedY(), "reversedY flag kept");

        assertEquals(0f, rounded.border().left, 0f, "NaN border edge becomes 0");
        assertEquals(2f, rounded.border().right, 0f, "border right");
        assertEquals(3f, rounded.border().top, 0f, "border top");
        assertEquals(-2f, rounded.border().bottom, 0f, "border bottom");

        assertEquals(1f, rounded.padding().left, 0f, "padding left");
        assertEquals(-1f, rounded.padding().right, 0f, "padding right");
        assertEquals(3f, rounded.padding().top, 0f, "padding top");
        assertEquals(-3f, rounded.padding().bottom, 0f, "padding bottom");
    }

    @Test
    @DisplayName("P11: rounding notifies the listener in pre-order and marks ancestors dirty")
    void notifiesListenerInPreOrder() {
        TaffyTree tree = new TaffyTree();
        NodeId a1 = tree.newLeaf(new TaffyStyle());
        NodeId a2 = tree.newLeaf(new TaffyStyle());
        NodeId a = tree.newWithChildren(new TaffyStyle(), a1, a2);
        NodeId b1 = tree.newLeaf(new TaffyStyle());
        NodeId b = tree.newWithChildren(new TaffyStyle(), b1);
        NodeId root = tree.newWithChildren(new TaffyStyle(), a, b);

        for (NodeId node : List.of(root, a, a1, a2, b, b1)) {
            tree.setUnroundedLayout(node, layout(0.5f, 0.5f, 1.5f, 1.5f));
        }

        List<NodeId> visited = new ArrayList<>();
        tree.setLayoutChangeListener((node, oldLayout, newLayout) -> visited.add(node));

        tree.roundLayout(root);

        assertEquals(List.of(root, a, a1, a2, b, b1), visited,
            "pre-order, first child first (same order as the old reverse-push stack loop)");
        for (NodeId node : List.of(root, a, a1, a2, b, b1)) {
            assertTrue(tree.hasNewLayout(node), "new-layout flag set for " + node);
        }
        assertTrue(tree.hasDirtyDescendant(root), "root saw dirty descendants");
        assertTrue(tree.hasDirtyDescendant(a), "a saw dirty descendants");
        assertTrue(tree.hasDirtyDescendant(b), "b saw dirty descendants");
        assertFalse(tree.hasDirtyDescendant(a1), "leaf has no dirty descendant");
    }

    @Test
    @DisplayName("P11: unknown roots and nodes without an unrounded layout skip their subtree")
    void skipsUnknownAndNullLayoutNodes() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(new TaffyStyle());
        NodeId root = tree.newWithChildren(new TaffyStyle(), child);
        tree.setUnroundedLayout(root, null);
        tree.setUnroundedLayout(child, layout(0.5f, 0.5f, 2.5f, 2.5f));

        List<NodeId> visited = new ArrayList<>();
        tree.setLayoutChangeListener((node, oldLayout, newLayout) -> visited.add(node));

        tree.roundLayout(root);
        tree.roundLayout(NodeId.of(9999L));

        assertTrue(visited.isEmpty(), "no notification when the subtree is skipped");
        assertEquals(0f, tree.getLayout(child).size().width, 0f, "child keeps its initial layout");

        tree.setUnroundedLayout(root, layout(0.5f, 0.5f, 1.5f, 1.5f));
        tree.roundLayout(root);
        assertEquals(List.of(root, child), visited, "known nodes are visited again");
        assertNotNull(tree.getLayout(child));
        // round(abs 1.0) - round(parent abs 0.5) == 1 - 1 == 0
        assertEquals(0f, tree.getLayout(child).location().x, 0f, "child is rounded now");
    }

    @Test
    @DisplayName("P11: the utility entry point delegates to the tree entry point")
    void utilityEntryPointMatchesTreeEntryPoint() {
        TaffyTree delegated = buildFractionalTree();
        TaffyTree direct = buildFractionalTree();

        RoundLayout.roundLayout(delegated, ROOT);
        direct.roundLayout(ROOT);

        for (long id = 0; id < NODE_COUNT; id++) {
            assertEquals(direct.getLayout(NodeId.of(id)), delegated.getLayout(NodeId.of(id)),
                "layout of node " + id);
        }
    }

    /** The tree built by {@link #buildFractionalTree()}: root id 4, container id 2. */
    private static final NodeId ROOT = NodeId.of(4L);
    private static final int NODE_COUNT = 5;

    private static TaffyTree buildFractionalTree() {
        TaffyTree tree = new TaffyTree();
        NodeId c1 = tree.newLeaf(new TaffyStyle());            // 0
        NodeId c2 = tree.newLeaf(new TaffyStyle());            // 1
        NodeId container = tree.newWithChildren(new TaffyStyle(), c1, c2); // 2
        NodeId wrapper = tree.newWithChildren(new TaffyStyle(), container); // 3
        tree.newWithChildren(new TaffyStyle(), wrapper);       // 4 (root)
        tree.setUnroundedLayout(NodeId.of(0L), layout(0.25f, 0.75f, 5.5f, 2.25f));
        tree.setUnroundedLayout(NodeId.of(1L), layout(0.5f, 0.5f, 1.5f, 1.5f));
        tree.setUnroundedLayout(NodeId.of(2L), layout(0.5f, 0.5f, 2.5f, 2.5f));
        tree.setUnroundedLayout(NodeId.of(3L), layout(0f, 0f, 20f, 20f));
        tree.setUnroundedLayout(NodeId.of(4L), layout(0f, 0f, 10f, 10f));
        return tree;
    }

    @Test
    @DisplayName("P11: computeLayout and an explicit roundLayout pass agree")
    void computeLayoutMatchesExplicitRoundingPass() {
        TaffyTree automatic = buildLayoutTree();
        TaffyTree explicit = buildLayoutTree();

        automatic.computeLayout(LAYOUT_ROOT, TaffySize.maxContent());

        explicit.disableRounding();
        explicit.computeLayout(LAYOUT_ROOT, TaffySize.maxContent());
        explicit.enableRounding();
        explicit.roundLayout(LAYOUT_ROOT);

        for (long id = 0; id < LAYOUT_NODE_COUNT; id++) {
            assertEquals(explicit.getLayout(NodeId.of(id)), automatic.getLayout(NodeId.of(id)),
                "layout of node " + id);
        }
    }

    /** The tree built by {@link #buildLayoutTree()}: root id 4 with four leaves. */
    private static final NodeId LAYOUT_ROOT = NodeId.of(4L);
    private static final int LAYOUT_NODE_COUNT = 5;

    private static TaffyTree buildLayoutTree() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle leaf = new TaffyStyle();
        leaf.size = new TaffySize<>(TaffyDimension.length(13.9375f), TaffyDimension.length(13.9375f));
        NodeId[] leaves = new NodeId[4];
        for (int i = 0; i < leaves.length; i++) {
            leaves[i] = tree.newLeaf(leaf); // ids 0..3
        }
        TaffyStyle column = new TaffyStyle();
        column.size = new TaffySize<>(TaffyDimension.length(13.9375f), TaffyDimension.length(13.9375f));
        tree.newWithChildren(column, leaves); // id 4 (root)
        return tree;
    }
}
