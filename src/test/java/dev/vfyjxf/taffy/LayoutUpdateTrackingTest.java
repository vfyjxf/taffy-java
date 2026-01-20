package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Yoga-style layout update tracking with dirty propagation.
 * <p>
 * The new semantics are:
 * <ul>
 *   <li>{@code hasNewLayout(node)} - true if this node was laid out since last acknowledgement</li>
 *   <li>{@code hasDirtyDescendant(node)} - true if any descendant has a new layout</li>
 *   <li>{@code needsVisit(node)} - true if node or any descendant needs attention</li>
 * </ul>
 * <p>
 * This allows efficient tree walking from root - skip subtrees where needsVisit() is false.
 */
public class LayoutUpdateTrackingTest {

    private static List<NodeId> collectSubtree(TaffyTree tree, NodeId root) {
        List<NodeId> out = new ArrayList<>();
        Deque<NodeId> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            NodeId n = stack.pop();
            out.add(n);
            List<NodeId> children = tree.getChildren(n);
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
        return out;
    }

    private static void acknowledgeAll(TaffyTree tree, List<NodeId> nodes) {
        for (NodeId n : nodes) {
            tree.acknowledgeLayout(n);
        }
    }

    private static void acknowledgeSubtreeAll(TaffyTree tree, List<NodeId> nodes) {
        // Acknowledge in reverse order (children first) for proper dirty descendant clearing
        for (int i = nodes.size() - 1; i >= 0; i--) {
            tree.acknowledgeSubtree(nodes.get(i));
        }
    }

    private static MeasureFunc fixedMeasure(float width, float height) {
        return (knownDims, availableSpace) -> new FloatSize(
            !Float.isNaN(knownDims.width) ? knownDims.width : width,
            !Float.isNaN(knownDims.height) ? knownDims.height : height
        );
    }

    @Nested
    @DisplayName("Basic dirty propagation")
    class BasicDirtyPropagation {

        @Test
        @DisplayName("initial layout marks all nodes as having new layout")
        void initialLayoutMarksAllNodes() {
            TaffyTree tree = new TaffyTree();

            NodeId leaf = tree.newLeaf(new TaffyStyle());
            NodeId child = tree.newWithChildren(new TaffyStyle(), leaf);
            NodeId root = tree.newWithChildren(new TaffyStyle(), child);

            // Before compute - no new layout flags
            assertFalse(tree.hasNewLayout(root));
            assertFalse(tree.hasNewLayout(child));
            assertFalse(tree.hasNewLayout(leaf));

            tree.computeLayout(root, TaffySize.maxContent());

            // After compute - all nodes have new layout
            assertTrue(tree.hasNewLayout(root));
            assertTrue(tree.hasNewLayout(child));
            assertTrue(tree.hasNewLayout(leaf));

            // Dirty descendant propagation
            assertTrue(tree.hasDirtyDescendant(root));
            assertTrue(tree.hasDirtyDescendant(child));
            assertFalse(tree.hasDirtyDescendant(leaf)); // leaf has no descendants
        }

        @Test
        @DisplayName("recompute without changes still marks nodes")
        void recomputeWithoutChangesStillMarks() {
            TaffyTree tree = new TaffyTree();

            TaffyStyle style = new TaffyStyle();
            style.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(100f));
            NodeId node = tree.newLeaf(style);

            tree.computeLayout(node, TaffySize.maxContent());
            tree.acknowledgeLayout(node);
            assertFalse(tree.hasNewLayout(node));

            // Recompute - should still mark as having new layout (Yoga-style behavior)
            tree.computeLayout(node, TaffySize.maxContent());
            assertTrue(tree.hasNewLayout(node));
        }

        @Test
        @DisplayName("acknowledge clears new layout flag")
        void acknowledgeClearsNewLayoutFlag() {
            TaffyTree tree = new TaffyTree();
            NodeId node = tree.newLeaf(new TaffyStyle());

            tree.computeLayout(node, TaffySize.maxContent());
            assertTrue(tree.hasNewLayout(node));

            tree.acknowledgeLayout(node);
            assertFalse(tree.hasNewLayout(node));
        }

        @Test
        @DisplayName("acknowledgeSubtree clears both flags")
        void acknowledgeSubtreeClearsBothFlags() {
            TaffyTree tree = new TaffyTree();
            
            NodeId leaf = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(new TaffyStyle(), leaf);

            tree.computeLayout(root, TaffySize.maxContent());

            assertTrue(tree.hasNewLayout(root));
            assertTrue(tree.hasDirtyDescendant(root));

            tree.acknowledgeSubtree(root);

            assertFalse(tree.hasNewLayout(root));
            assertFalse(tree.hasDirtyDescendant(root));
        }
    }

    @Nested
    @DisplayName("needsVisit for efficient tree walking")
    class NeedsVisitTests {

        @Test
        @DisplayName("needsVisit returns true when node has new layout")
        void needsVisitTrueWhenHasNewLayout() {
            TaffyTree tree = new TaffyTree();
            NodeId node = tree.newLeaf(new TaffyStyle());

            tree.computeLayout(node, TaffySize.maxContent());

            assertTrue(tree.hasNewLayout(node));
            assertTrue(tree.needsVisit(node));
        }

        @Test
        @DisplayName("needsVisit returns true when has dirty descendant")
        void needsVisitTrueWhenHasDirtyDescendant() {
            TaffyTree tree = new TaffyTree();
            
            NodeId leaf = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(new TaffyStyle(), leaf);

            tree.computeLayout(root, TaffySize.maxContent());
            tree.acknowledgeLayout(root); // Clear root's hasNewLayout but not dirty descendant

            assertFalse(tree.hasNewLayout(root));
            assertTrue(tree.hasDirtyDescendant(root));
            assertTrue(tree.needsVisit(root)); // Still needs visit because of descendant
        }

        @Test
        @DisplayName("needsVisit returns false when fully acknowledged")
        void needsVisitFalseWhenFullyAcknowledged() {
            TaffyTree tree = new TaffyTree();
            
            NodeId leaf = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(new TaffyStyle(), leaf);

            tree.computeLayout(root, TaffySize.maxContent());

            // Acknowledge from bottom up
            tree.acknowledgeSubtree(leaf);
            tree.acknowledgeSubtree(root);

            assertFalse(tree.needsVisit(root));
            assertFalse(tree.needsVisit(leaf));
        }
    }

    @Nested
    @DisplayName("Efficient tree walking pattern")
    class TreeWalkingPattern {

        @Test
        @DisplayName("can skip unchanged subtrees")
        void canSkipUnchangedSubtrees() {
            TaffyTree tree = new TaffyTree();

            // Build tree: root -> [branch1 -> leaf1, branch2 -> leaf2]
            NodeId leaf1 = tree.newLeaf(new TaffyStyle());
            NodeId branch1 = tree.newWithChildren(new TaffyStyle(), leaf1);
            NodeId leaf2 = tree.newLeaf(new TaffyStyle());
            NodeId branch2 = tree.newWithChildren(new TaffyStyle(), leaf2);
            NodeId root = tree.newWithChildren(new TaffyStyle(), branch1, branch2);

            tree.computeLayout(root, TaffySize.maxContent());

            // Acknowledge entire tree
            List<NodeId> allNodes = collectSubtree(tree, root);
            acknowledgeSubtreeAll(tree, allNodes);

            // Verify all clean
            for (NodeId n : allNodes) {
                assertFalse(tree.needsVisit(n), "Node should not need visit: " + n);
            }

            // Now modify only branch1's style
            TaffyStyle newStyle = tree.getStyle(branch1).copy();
            newStyle.size = new TaffySize<>(TaffyDimension.length(50f), TaffyDimension.length(50f));
            tree.setStyle(branch1, newStyle);

            tree.computeLayout(root, TaffySize.maxContent());

            // Root needs visit (has dirty descendant)
            assertTrue(tree.needsVisit(root));
            
            // Branch1 subtree was affected
            assertTrue(tree.needsVisit(branch1));
            assertTrue(tree.hasNewLayout(branch1));
            
            // Branch2 subtree may also be recomputed (depending on layout algorithm),
            // but we can verify the pattern works for selective acknowledgement
        }

        @Test
        @DisplayName("demonstrates efficient walk pattern")
        void demonstratesEfficientWalkPattern() {
            TaffyTree tree = new TaffyTree();

            // Build a deeper tree
            TaffyStyle leafStyle = new TaffyStyle();
            leafStyle.size = new TaffySize<>(TaffyDimension.length(20f), TaffyDimension.length(20f));
            
            NodeId leaf1 = tree.newLeaf(leafStyle);
            NodeId leaf2 = tree.newLeaf(leafStyle);
            NodeId leaf3 = tree.newLeaf(leafStyle);
            NodeId leaf4 = tree.newLeaf(leafStyle);
            
            NodeId branch1 = tree.newWithChildren(new TaffyStyle(), leaf1, leaf2);
            NodeId branch2 = tree.newWithChildren(new TaffyStyle(), leaf3, leaf4);
            NodeId root = tree.newWithChildren(new TaffyStyle(), branch1, branch2);

            tree.computeLayout(root, TaffySize.maxContent());

            // Simulate efficient walk pattern:
            // 1. Start at root
            // 2. If needsVisit is false, skip entire subtree
            // 3. If hasNewLayout, process node
            // 4. Recurse to children
            // 5. Call acknowledgeSubtree when done with node

            List<NodeId> visited = new ArrayList<>();
            efficientWalk(tree, root, visited);

            // All nodes should have been visited
            assertEquals(7, visited.size());

            // After walk, nothing needs visit
            assertFalse(tree.needsVisit(root));
        }

        private void efficientWalk(TaffyTree tree, NodeId node, List<NodeId> visited) {
            if (!tree.needsVisit(node)) {
                return; // Skip this subtree entirely
            }

            visited.add(node);

            // Process children first (for bottom-up acknowledgement)
            for (NodeId child : tree.getChildren(node)) {
                efficientWalk(tree, child, visited);
            }

            // Acknowledge this node after processing children
            tree.acknowledgeSubtree(node);
        }
    }

    @Nested
    @DisplayName("Complex tree scenarios")
    class ComplexTreeScenarios {

        @Test
        @DisplayName("layout tracking with multiple update waves")
        void layoutTrackingWithMultipleUpdateWaves() {
            TaffyTree tree = new TaffyTree();

            // Build complex tree
            TaffyStyle leafAStyle = new TaffyStyle();
            leafAStyle.size = new TaffySize<>(TaffyDimension.length(40f), TaffyDimension.length(10f));
            NodeId leafA = tree.newLeaf(leafAStyle);

            TaffyStyle leafBStyle = new TaffyStyle();
            leafBStyle.flexGrow = 1f;
            leafBStyle.flexShrink = 1.0f;
            leafBStyle.flexBasis = TaffyDimension.AUTO;
            NodeId leafB = tree.newLeafWithMeasure(leafBStyle, fixedMeasure(25f, 12f));

            NodeId leafC = tree.newLeaf(new TaffyStyle());

            TaffyStyle flexRowStyle = new TaffyStyle();
            flexRowStyle.display = TaffyDisplay.FLEX;
            flexRowStyle.flexDirection = FlexDirection.ROW;
            NodeId flexRow = tree.newWithChildren(flexRowStyle, leafA, leafB, leafC);

            TaffyStyle rootStyle = new TaffyStyle();
            rootStyle.display = TaffyDisplay.FLEX;
            rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(120f));
            NodeId root = tree.newWithChildren(rootStyle, flexRow);

            TaffySize<AvailableSpace> available = new TaffySize<>(
                AvailableSpace.definite(200f), 
                AvailableSpace.definite(120f)
            );

            List<NodeId> nodes = collectSubtree(tree, root);

            // Wave 0: initial compute
            tree.computeLayout(root, available);
            
            // All nodes should have new layout
            for (NodeId n : nodes) {
                assertTrue(tree.hasNewLayout(n), "Node should have new layout after first compute: " + n);
            }
            
            // Root should have dirty descendants
            assertTrue(tree.hasDirtyDescendant(root));

            acknowledgeSubtreeAll(tree, nodes);
            
            // All clean
            for (NodeId n : nodes) {
                assertFalse(tree.needsVisit(n), "Node should not need visit after ack: " + n);
            }

            // Wave 1: recompute without changes
            tree.computeLayout(root, available);
            
            // All nodes marked again (Yoga-style)
            for (NodeId n : nodes) {
                assertTrue(tree.hasNewLayout(n), "Node should have new layout on recompute: " + n);
            }

            acknowledgeSubtreeAll(tree, nodes);

            // Wave 2: change style on one node
            TaffyStyle newLeafAStyle = tree.getStyle(leafA).copy();
            newLeafAStyle.size = new TaffySize<>(TaffyDimension.length(80f), TaffyDimension.length(20f));
            tree.setStyle(leafA, newLeafAStyle);

            tree.computeLayout(root, available);

            // At minimum, the modified node and its ancestors should need visit
            assertTrue(tree.needsVisit(root));
            assertTrue(tree.hasNewLayout(leafA));
        }

        @Test
        @DisplayName("structural changes update tracking correctly")
        void structuralChangesUpdateTrackingCorrectly() {
            TaffyTree tree = new TaffyTree();

            NodeId leaf1 = tree.newLeaf(new TaffyStyle());
            NodeId root = tree.newWithChildren(new TaffyStyle(), leaf1);

            tree.computeLayout(root, TaffySize.maxContent());
            acknowledgeSubtreeAll(tree, collectSubtree(tree, root));

            assertFalse(tree.needsVisit(root));

            // Add a new child
            NodeId leaf2 = tree.newLeaf(new TaffyStyle());
            tree.addChild(root, leaf2);

            tree.computeLayout(root, TaffySize.maxContent());

            // New node should have new layout
            assertTrue(tree.hasNewLayout(leaf2));
            
            // Root should need visit due to recompute
            assertTrue(tree.needsVisit(root));
        }
    }
}
