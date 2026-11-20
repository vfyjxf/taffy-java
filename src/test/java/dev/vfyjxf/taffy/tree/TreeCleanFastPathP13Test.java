package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P13: the tree-clean layout fast path.
 *
 * <p>An identical layout request (same root, same available space per axis, same rounding
 * configuration, same default measure callbacks) on a tree whose last layout is still valid must
 * be answered from the stored layouts: no layout algorithm pass, no rounding pass, no layout
 * object re-allocation.</p>
 *
 * <p>"No pass ran" is proved two ways: the package-private {@code layoutPassCount()} counter and
 * the identity of every node's {@link Layout} instance (a pass re-allocates them). The invalidation
 * tests prove the opposite direction — every input mutation, the rounding toggle, an available
 * space change, an explicit layout write and a directly started layout pass must force a real
 * pass. {@code fastPathMatchesForcedPass} is the differential check: the reused result and a
 * forced cold recompute must serialize to the same tree dump.</p>
 */
class TreeCleanFastPathP13Test {

    private static final float EPSILON = 0.001f;

    // === Builders ===

    private static TaffyStyle sized(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle flexColumn(float width) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.AUTO);
        return style;
    }

    /**
     * A three-level flex tree with fractional sizes and padding, so the rounding pass has real work
     * and the final layouts differ from the unrounded ones.
     */
    private static NodeId fractionalTree(TaffyTree tree) {
        TaffyStyle rootStyle = flexColumn(200.5f);
        NodeId branch1 = tree.newWithChildren(
            flexColumn(90.5f),
            tree.newLeaf(sized(10.5f, 10.5f)),
            tree.newLeaf(sized(20.25f, 20.25f)));
        NodeId branch2 = tree.newWithChildren(
            flexColumn(80.75f),
            tree.newLeaf(sized(30.75f, 30.75f)));
        return tree.newWithChildren(rootStyle, branch1, branch2);
    }

    private static TaffySize<AvailableSpace> request(float width) {
        return TaffySize.of(AvailableSpace.definite(width), AvailableSpace.maxContent());
    }

    private static List<NodeId> collect(TaffyTree tree, NodeId root) {
        List<NodeId> out = new ArrayList<>();
        Deque<NodeId> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            NodeId node = stack.pop();
            out.add(node);
            List<NodeId> children = tree.getChildren(node);
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
        return out;
    }

    private static String dump(TaffyTree tree, NodeId root) {
        StringWriter writer = new StringWriter();
        try {
            tree.writeTree(writer, root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return writer.toString();
    }

    private static MeasureFunc fixedMeasure(float width, float height) {
        return (knownDimensions, availableSpace) -> new FloatSize(
            !Float.isNaN(knownDimensions.width) ? knownDimensions.width : width,
            !Float.isNaN(knownDimensions.height) ? knownDimensions.height : height);
    }

    // === The fast path itself ===

    @Test
    @DisplayName("P13: an unchanged request reuses every stored layout (rounding enabled)")
    void unchangedRequestReusesStoredLayoutsWithRounding() {
        TaffyTree tree = new TaffyTree(); // rounding is enabled by default
        NodeId root = fractionalTree(tree);
        List<NodeId> nodes = collect(tree, root);

        tree.computeLayout(root, request(200.5f));
        assertEquals(1, tree.layoutPassCount(), "the first request runs one pass");

        List<Layout> layoutsBefore = new ArrayList<>();
        for (NodeId node : nodes) {
            layoutsBefore.add(tree.getLayout(node));
        }
        String dumpBefore = dump(tree, root);

        for (int i = 0; i < 5; i++) {
            tree.computeLayout(root, request(200.5f));
        }

        assertEquals(1, tree.layoutPassCount(),
            "five identical requests must not run a layout or rounding pass");
        for (int i = 0; i < nodes.size(); i++) {
            assertSame(layoutsBefore.get(i), tree.getLayout(nodes.get(i)),
                "no layout is re-allocated for " + nodes.get(i) + " (a rounding pass allocates one per node)");
        }
        assertEquals(dumpBefore, dump(tree, root), "the reused layouts are the stored layouts");
    }

    @Test
    @DisplayName("P13: an unchanged request reuses the stored layouts with rounding disabled too")
    void unchangedRequestReusesStoredLayoutsWithoutRounding() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId root = fractionalTree(tree);
        List<NodeId> nodes = collect(tree, root);

        tree.computeLayout(root, request(200.5f));
        List<Layout> layoutsBefore = new ArrayList<>();
        for (NodeId node : nodes) {
            layoutsBefore.add(tree.getLayout(node));
        }

        tree.computeLayout(root, request(200.5f));

        assertEquals(1, tree.layoutPassCount(), "the second request is a fast-path hit");
        for (int i = 0; i < nodes.size(); i++) {
            assertSame(layoutsBefore.get(i), tree.getLayout(nodes.get(i)),
                "the unrounded layout instance is reused for " + nodes.get(i));
        }
    }

    @Test
    @DisplayName("P13: the reused result is identical to a forced cold recompute")
    void fastPathMatchesForcedPass() {
        TaffyTree tree = new TaffyTree();
        NodeId root = fractionalTree(tree);
        List<NodeId> nodes = collect(tree, root);

        tree.computeLayout(root, request(200.5f));
        String coldDump = dump(tree, root);

        // Warm request: answered from the stored layouts.
        tree.computeLayout(root, request(200.5f));
        assertEquals(1, tree.layoutPassCount(), "the warm request is a fast-path hit");
        assertEquals(coldDump, dump(tree, root), "the fast path returns the cold-layout result");

        // Force a cold recompute through the dirty flags.
        for (NodeId node : nodes) {
            tree.markDirty(node);
        }
        tree.computeLayout(root, request(200.5f));
        assertEquals(2, tree.layoutPassCount(), "clearing the caches forces a real pass");
        assertEquals(coldDump, dump(tree, root),
            "the fast path and a real pass produce the same layout tree");
    }

    // === Invalidation: style, children, measure ===

    @Test
    @DisplayName("P13: a style change forces a real pass")
    void styleChangeForcesPass() {
        TaffyTree tree = new TaffyTree();
        NodeId leaf = tree.newLeaf(sized(10.5f, 10.5f));
        NodeId root = tree.newWithChildren(flexColumn(200.5f), leaf);

        tree.computeLayout(root, request(200.5f));
        assertEquals(1, tree.layoutPassCount());

        tree.setStyle(leaf, sized(40.5f, 10.5f));
        tree.computeLayout(root, request(200.5f));

        assertEquals(2, tree.layoutPassCount(), "setStyle must invalidate the fast path");
        assertEquals(Math.round(40.5f), tree.getLayout(leaf).size().width, EPSILON,
            "the changed style is laid out");
    }

    @Test
    @DisplayName("P13: every child-list change forces a real pass")
    void childListChangesForcePass() {
        TaffyTree tree = new TaffyTree();
        NodeId root = tree.newWithChildren(flexColumn(200.5f), tree.newLeaf(sized(10f, 10f)));
        tree.computeLayout(root, request(200.5f));
        long passes = tree.layoutPassCount();

        NodeId added = tree.newLeaf(sized(10f, 10f));
        tree.addChild(root, added);
        tree.computeLayout(root, request(200.5f));
        assertEquals(++passes, tree.layoutPassCount(), "addChild");

        tree.removeChild(root, added);
        tree.computeLayout(root, request(200.5f));
        assertEquals(++passes, tree.layoutPassCount(), "removeChild");

        tree.insertChildAtIndex(root, 0, added);
        tree.computeLayout(root, request(200.5f));
        assertEquals(++passes, tree.layoutPassCount(), "insertChildAtIndex");

        tree.removeChildAtIndex(root, 0);
        tree.computeLayout(root, request(200.5f));
        assertEquals(++passes, tree.layoutPassCount(), "removeChildAtIndex");

        NodeId replacement = tree.newLeaf(sized(12f, 12f));
        tree.replaceChildAtIndex(root, 0, replacement);
        tree.computeLayout(root, request(200.5f));
        assertEquals(++passes, tree.layoutPassCount(), "replaceChildAtIndex");

        tree.setChildren(root, tree.newLeaf(sized(8f, 8f)), tree.newLeaf(sized(9f, 9f)));
        tree.computeLayout(root, request(200.5f));
        assertEquals(++passes, tree.layoutPassCount(), "setChildren");
    }

    @Test
    @DisplayName("P13: measure callback and context changes force a real pass")
    void measureChangesForcePass() {
        TaffyTree tree = new TaffyTree();
        NodeId leaf = tree.newLeafWithMeasure(new TaffyStyle(), fixedMeasure(10f, 10f));
        NodeId root = tree.newWithChildren(flexColumn(200.5f), leaf);

        tree.computeLayout(root, request(200.5f));
        long passes = tree.layoutPassCount();

        tree.setMeasureFunc(leaf, fixedMeasure(20f, 10f));
        tree.computeLayout(root, request(200.5f));
        assertEquals(++passes, tree.layoutPassCount(), "setMeasureFunc");

        tree.setMeasureFunction(leaf, (context) -> LayoutOutput.fromOuterSize(new FloatSize(30f, 10f)));
        tree.computeLayout(root, request(200.5f));
        assertEquals(++passes, tree.layoutPassCount(), "setMeasureFunction");

        tree.setNodeContext(leaf, "new text");
        tree.computeLayout(root, request(200.5f));
        assertEquals(++passes, tree.layoutPassCount(), "setNodeContext");
    }

    // === Invalidation: request inputs ===

    @Test
    @DisplayName("P13: available space and rounding changes force a real pass")
    void requestInputChangesForcePass() {
        TaffyTree tree = new TaffyTree();
        NodeId root = fractionalTree(tree);

        tree.computeLayout(root, request(200.5f));
        assertEquals(1, tree.layoutPassCount());

        tree.computeLayout(root, request(250.5f));
        assertEquals(2, tree.layoutPassCount(), "a different available width");

        tree.computeLayout(root, request(200.5f));
        assertEquals(3, tree.layoutPassCount(), "going back to the previous width is a new request");

        tree.computeLayout(root, TaffySize.of(AvailableSpace.definite(200.5f), AvailableSpace.minContent()));
        assertEquals(4, tree.layoutPassCount(), "a different height constraint type");

        tree.disableRounding();
        tree.computeLayout(root, TaffySize.of(AvailableSpace.definite(200.5f), AvailableSpace.minContent()));
        assertEquals(5, tree.layoutPassCount(), "disabling rounding changes the request");

        tree.enableRounding();
        tree.computeLayout(root, TaffySize.of(AvailableSpace.definite(200.5f), AvailableSpace.minContent()));
        assertEquals(6, tree.layoutPassCount(), "enabling rounding changes it back");
    }

    @Test
    @DisplayName("P13: explicit dirty marking and public cache clearing force a real pass")
    void dirtyAndCacheClearingForcePass() {
        TaffyTree tree = new TaffyTree();
        NodeId root = fractionalTree(tree);

        tree.computeLayout(root, request(200.5f));
        tree.markDirty(root);
        tree.computeLayout(root, request(200.5f));
        assertEquals(2, tree.layoutPassCount(), "markDirty empties the root cache");

        tree.clearCache(root);
        tree.computeLayout(root, request(200.5f));
        assertEquals(3, tree.layoutPassCount(), "clearCache empties the root cache");

        tree.computeLayout(root, request(200.5f));
        assertEquals(3, tree.layoutPassCount(), "the following identical request is a hit again");
    }

    @Test
    @DisplayName("P13: an explicit layout write is not silently skipped")
    void explicitLayoutWriteInvalidates() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId root = fractionalTree(tree);
        tree.computeLayout(root, request(200.5f));

        Layout original = tree.getLayout(root);
        tree.setUnroundedLayout(root, new Layout(0, original.location(), original.size(),
            original.contentSize(), original.scrollableOverflow(), original.scrollbarSize(),
            original.border(), original.padding(), original.margin()));
        tree.computeLayout(root, request(200.5f));

        assertEquals(2, tree.layoutPassCount(), "a written layout forces a real pass");
        assertNotSame(original, tree.getLayout(root), "the pass re-derives the root layout");
    }

    @Test
    @DisplayName("P13: a listener disables the fast path and keeps observing every write")
    void listenerDisablesFastPath() {
        TaffyTree tree = new TaffyTree();
        NodeId root = fractionalTree(tree);
        List<NodeId> nodes = collect(tree, root);

        int[] notifications = {0};
        tree.setLayoutChangeListener((node, oldLayout, newLayout) -> notifications[0]++);

        tree.computeLayout(root, request(200.5f));
        assertEquals(1, tree.layoutPassCount());
        assertEquals(nodes.size(), notifications[0], "the rounding pass reports every node once");

        notifications[0] = 0;
        tree.computeLayout(root, request(200.5f));

        assertEquals(2, tree.layoutPassCount(),
            "the listener contract reports layout writes, so the fast path stays off");
        assertEquals(nodes.size(), notifications[0], "the listener still reports every node");

        // Without a listener the same request is a fast-path hit again.
        tree.setLayoutChangeListener(null);
        tree.computeLayout(root, request(200.5f));
        assertEquals(2, tree.layoutPassCount(), "removing the listener re-enables the fast path");
    }

    // === Semantics that must survive the fast path ===

    @Test
    @DisplayName("P13: a reused request still reports a new layout for the whole tree")
    void reusedRequestStillReportsNewLayout() {
        TaffyTree tree = new TaffyTree();
        NodeId root = fractionalTree(tree);
        List<NodeId> nodes = collect(tree, root);

        tree.computeLayout(root, request(200.5f));
        for (NodeId node : nodes) {
            tree.acknowledgeSubtree(node);
        }
        for (NodeId node : nodes) {
            assertFalse(tree.hasNewLayout(node), "acknowledged node reports nothing: " + node);
            assertFalse(tree.needsVisit(node), "acknowledged node needs no visit: " + node);
        }

        tree.computeLayout(root, request(200.5f));
        assertEquals(1, tree.layoutPassCount(), "this request is a fast-path hit");
        for (NodeId node : nodes) {
            assertTrue(tree.hasNewLayout(node),
                "a completed request still marks the tree without walking it: " + node);
        }
        assertTrue(tree.needsVisit(root), "a walk from the root still sees the request");

        // Acknowledging again clears it, still without a pass.
        for (NodeId node : nodes) {
            tree.acknowledgeSubtree(node);
        }
        for (NodeId node : nodes) {
            assertFalse(tree.hasNewLayout(node), "the second acknowledgement clears the flag: " + node);
        }
        assertEquals(1, tree.layoutPassCount(), "acknowledging never runs a pass");
    }

    @Test
    @DisplayName("P13: an unknown root still throws instead of reusing a stored layout")
    void unknownRootStillThrows() {
        TaffyTree tree = new TaffyTree();
        NodeId root = fractionalTree(tree);
        tree.computeLayout(root, request(200.5f));

        assertThrows(TaffyException.class, () -> tree.computeLayout(NodeId.of(9999L), request(200.5f)));
        assertEquals(1, tree.layoutPassCount(), "the failed request never reached the layout algorithm");
    }

    // === Layout passes started outside computeLayout ===

    @Test
    @DisplayName("P13: a pass started through LayoutComputer invalidates the recorded request")
    void directLayoutComputerPassInvalidates() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId root = fractionalTree(tree);
        tree.computeLayout(root, request(200.5f));
        assertEquals(1, tree.layoutPassCount());

        // A caller can run the algorithm directly: TaffyTree never sees which root or available
        // space that pass used, so the recorded request signature can no longer be trusted.
        new LayoutComputer(tree, null, null).computeLayout(root, request(200.5f));

        tree.computeLayout(root, request(200.5f));
        assertEquals(2, tree.layoutPassCount(), "the recorded request is dropped by the direct pass");

        tree.computeLayout(root, request(200.5f));
        assertEquals(2, tree.layoutPassCount(), "the following identical request is a hit again");
    }

    @Test
    @DisplayName("P13: laying out a sub-root does not poison the parent request")
    void subRootPassDoesNotPoisonParentRequest() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId branch = tree.newWithChildren(flexColumn(90.5f), tree.newLeaf(sized(10f, 10f)));
        NodeId root = tree.newWithChildren(flexColumn(200.5f), branch);

        tree.computeLayout(root, request(200.5f));
        String rootDump = dump(tree, root);

        tree.computeLayout(branch, request(40.5f));
        assertEquals(2, tree.layoutPassCount(), "the sub-root request is a different request");

        tree.computeLayout(root, request(200.5f));
        assertEquals(3, tree.layoutPassCount(),
            "the parent request must recompute after a sub-root pass changed the layouts");
        assertEquals(rootDump, dump(tree, root), "and it restores the parent layout");
    }
}
