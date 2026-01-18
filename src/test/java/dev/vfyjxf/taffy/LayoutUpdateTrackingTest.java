package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.Rect;
import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stress-tests layout update tracking (hasUnconsumedLayout/acknowledgeLayout)
 * on a deliberately complex tree with multiple update waves.
 */
public class LayoutUpdateTrackingTest {

    private record LayoutPair(Layout unrounded, Layout roundedOrSelected) {
    }

    private static List<NodeId> collectSubtree(TaffyTree tree, NodeId root) {
        List<NodeId> out = new ArrayList<>();
        Deque<NodeId> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            NodeId n = stack.pop();
            out.add(n);
            List<NodeId> children = tree.getChildren(n);
            // reverse for stable-ish order
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
        return out;
    }

    private static Map<NodeId, LayoutPair> snapshotLayouts(TaffyTree tree, List<NodeId> nodes) {
        Map<NodeId, LayoutPair> snap = new LinkedHashMap<>();
        for (NodeId n : nodes) {
            Layout unrounded = tree.getUnroundedLayout(n);
            Layout rounded = tree.getLayout(n);
            // Both APIs should always return non-null for in-tree nodes, but be defensive.
            snap.put(n, new LayoutPair(unrounded, rounded));
        }
        return snap;
    }

    private static Set<NodeId> diffLayouts(Map<NodeId, LayoutPair> before, Map<NodeId, LayoutPair> after) {
        Set<NodeId> changed = new HashSet<>();
        for (Map.Entry<NodeId, LayoutPair> e : before.entrySet()) {
            NodeId node = e.getKey();
            LayoutPair b = e.getValue();
            LayoutPair a = after.get(node);
            if (a == null) {
                // Node disappeared from subtree
                changed.add(node);
                continue;
            }
            // hasUnconsumedLayout is bumped when either unrounded or final layout changes.
            boolean unroundedChanged = !Objects.equals(b.unrounded(), a.unrounded());
            boolean roundedChanged = !Objects.equals(b.roundedOrSelected(), a.roundedOrSelected());
            if (unroundedChanged || roundedChanged) {
                changed.add(node);
            }
        }
        // New nodes in after
        for (NodeId node : after.keySet()) {
            if (!before.containsKey(node)) {
                changed.add(node);
            }
        }
        return changed;
    }

    private static void acknowledgeAll(TaffyTree tree, List<NodeId> nodes) {
        for (NodeId n : nodes) {
            tree.acknowledgeLayout(n);
        }
    }

    private static void assertFlagsMatchDiff(TaffyTree tree, List<NodeId> nodes, Set<NodeId> changed) {
        for (NodeId n : nodes) {
            boolean expected = changed.contains(n);
            assertEquals(expected, tree.hasUnconsumedLayout(n), "Mismatch for node=" + n);
        }
    }

    private static MeasureFunc fixedMeasure(float width, float height) {
        return (knownDims, availableSpace) -> new FloatSize(
            !Float.isNaN(knownDims.width) ? knownDims.width : width,
            !Float.isNaN(knownDims.height) ? knownDims.height : height
        );
    }

    @Test
    @DisplayName("layout_update_tracking_on_complex_tree_with_multiple_update_waves")
    void layoutUpdateTrackingOnComplexTreeWithMultipleUpdateWaves() {
        TaffyTree tree = new TaffyTree();
        // Keep default rounding enabled: this is the common external API mode.

        // --- Build a deliberately complex tree ---
        // Leaf A: fixed size
        Style leafAStyle = new Style();
        leafAStyle.size = new Size<>(Dimension.length(40f), Dimension.length(10f));
        NodeId leafA = tree.newLeaf(leafAStyle);

        // Leaf B: measured leaf (size depends on measure func)
        Style leafBStyle = new Style();
        leafBStyle.flexGrow = 1f;
        NodeId leafB = tree.newLeafWithMeasure(leafBStyle, fixedMeasure(25f, 12f));

        // Leaf C: will be toggled display none later
        Style leafCStyle = new Style();
        leafCStyle.size = new Size<>(Dimension.length(15f), Dimension.length(15f));
        NodeId leafC = tree.newLeaf(leafCStyle);

        // Flex row container with wrap
        Style flexRowStyle = new Style();
        flexRowStyle.display = Display.FLEX;
        flexRowStyle.flexDirection = FlexDirection.ROW;
        flexRowStyle.flexWrap = FlexWrap.WRAP;
        flexRowStyle.gap = new Size<>(LengthPercentage.length(3f), LengthPercentage.length(2f));
        flexRowStyle.padding = new Rect<>(
            LengthPercentage.length(1f),
            LengthPercentage.length(1f),
            LengthPercentage.length(1f),
            LengthPercentage.length(1f)
        );
        NodeId flexRow = tree.newWithChildren(flexRowStyle, leafA, leafB, leafC);

        // Nested block container under root
        Style blockStyle = new Style();
        blockStyle.display = Display.BLOCK;
        blockStyle.padding = new Rect<>(
            LengthPercentage.length(2f),
            LengthPercentage.length(4f),
            LengthPercentage.length(6f),
            LengthPercentage.length(8f)
        );
        NodeId block = tree.newWithChildren(blockStyle, flexRow);

        // Root flex column
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.flexDirection = FlexDirection.COLUMN;
        rootStyle.alignItems = AlignItems.STRETCH;
        rootStyle.size = new Size<>(Dimension.length(200f), Dimension.length(120f));
        rootStyle.padding = new Rect<>(
            LengthPercentage.length(5f),
            LengthPercentage.length(5f),
            LengthPercentage.length(5f),
            LengthPercentage.length(5f)
        );
        NodeId root = tree.newWithChildren(rootStyle, block);

        Size<AvailableSpace> available = new Size<>(AvailableSpace.definite(200f), AvailableSpace.definite(120f));

        // Collect nodes once; we will re-collect after structural changes.
        List<NodeId> nodes = collectSubtree(tree, root);

        // --- Wave 0: initial compute should create "unconsumed" layouts across the subtree ---
        Map<NodeId, LayoutPair> before0 = snapshotLayouts(tree, nodes);
        tree.computeLayout(root, available);
        Map<NodeId, LayoutPair> after0 = snapshotLayouts(tree, nodes);
        Set<NodeId> changed0 = diffLayouts(before0, after0);
        assertTrue(changed0.contains(root), "Expected root layout to be updated on first compute");
        assertFlagsMatchDiff(tree, nodes, changed0);

        acknowledgeAll(tree, nodes);
        for (NodeId n : nodes) {
            assertFalse(tree.hasUnconsumedLayout(n), "Expected ack to clear flag for node=" + n);
        }

        // --- Wave 1: recompute without changes should not flip any flags ---
        Map<NodeId, LayoutPair> before1 = snapshotLayouts(tree, nodes);
        tree.computeLayout(root, available);
        Map<NodeId, LayoutPair> after1 = snapshotLayouts(tree, nodes);
        Set<NodeId> changed1 = diffLayouts(before1, after1);
        assertTrue(changed1.isEmpty(), "Expected no layout changes on identical recompute");
        assertFlagsMatchDiff(tree, nodes, changed1);

        acknowledgeAll(tree, nodes);

        // --- Wave 2: change an ancestor padding (should move descendants relative to that ancestor) ---
        Map<NodeId, LayoutPair> before2 = snapshotLayouts(tree, nodes);
        Style newBlockStyle = tree.getStyle(block).copy();
        newBlockStyle.padding = new Rect<>(
            LengthPercentage.length(10f),
            LengthPercentage.length(10f),
            LengthPercentage.length(10f),
            LengthPercentage.length(10f)
        );
        tree.setStyle(block, newBlockStyle);
        tree.computeLayout(root, available);
        Map<NodeId, LayoutPair> after2 = snapshotLayouts(tree, nodes);
        Set<NodeId> changed2 = diffLayouts(before2, after2);
        assertFalse(changed2.isEmpty(), "Expected some layout changes after ancestor padding update");
        assertFlagsMatchDiff(tree, nodes, changed2);

        acknowledgeAll(tree, nodes);

        // --- Wave 3: toggle a leaf to display none (hidden layout propagation) ---
        Map<NodeId, LayoutPair> before3 = snapshotLayouts(tree, nodes);
        Style hidden = tree.getStyle(leafC).copy();
        hidden.display = Display.NONE;
        tree.setStyle(leafC, hidden);
        tree.computeLayout(root, available);
        Map<NodeId, LayoutPair> after3 = snapshotLayouts(tree, nodes);
        Set<NodeId> changed3 = diffLayouts(before3, after3);
        assertTrue(changed3.contains(leafC), "Expected leafC to change layout when hidden");
        assertFlagsMatchDiff(tree, nodes, changed3);

        acknowledgeAll(tree, nodes);

        // --- Wave 4: change flex direction on flexRow to force reflow ---
        Map<NodeId, LayoutPair> before4 = snapshotLayouts(tree, nodes);
        Style newFlexRow = tree.getStyle(flexRow).copy();
        newFlexRow.flexDirection = FlexDirection.ROW_REVERSE;
        tree.setStyle(flexRow, newFlexRow);
        tree.computeLayout(root, available);
        Map<NodeId, LayoutPair> after4 = snapshotLayouts(tree, nodes);
        Set<NodeId> changed4 = diffLayouts(before4, after4);
        assertFalse(changed4.isEmpty(), "Expected reflow changes after flexDirection update");
        assertFlagsMatchDiff(tree, nodes, changed4);

        acknowledgeAll(tree, nodes);

        // --- Wave 5: structural change (add a new measured leaf into flexRow) ---
        NodeId leafD = tree.newLeafWithMeasure(new Style(), fixedMeasure(33f, 7f));
        Map<NodeId, LayoutPair> before5 = snapshotLayouts(tree, nodes);
        tree.addChild(flexRow, leafD);
        tree.computeLayout(root, available);

        // Recollect nodes because subtree grew.
        nodes = collectSubtree(tree, root);
        Map<NodeId, LayoutPair> after5 = snapshotLayouts(tree, nodes);
        // before snapshot didn't include new node; diffLayouts accounts for new nodes.
        Set<NodeId> changed5 = diffLayouts(before5, after5);
        assertTrue(changed5.contains(leafD), "Expected new leafD to have layout and be flagged");
        assertFlagsMatchDiff(tree, nodes, changed5);

        acknowledgeAll(tree, nodes);

        // --- Wave 6: update measure func on leafB (size change should propagate) ---
        Map<NodeId, LayoutPair> before6 = snapshotLayouts(tree, nodes);
        tree.setMeasureFunc(leafB, fixedMeasure(60f, 12f));
        tree.computeLayout(root, available);
        Map<NodeId, LayoutPair> after6 = snapshotLayouts(tree, nodes);
        Set<NodeId> changed6 = diffLayouts(before6, after6);
        // NOTE: Changing measure func does not guarantee a visible layout change.
        // For example, flex sizing can override intrinsic width. We only require that
        // hasUnconsumedLayout matches whether the produced layout actually changed.
        assertFlagsMatchDiff(tree, nodes, changed6);
    }
}
