package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P15 tests for the dense node storage that replaced the {@code Long2ObjectOpenHashMap<NodeData>}.
 *
 * <p>The node id counter is monotonic and starts at zero, so an id doubles as an array index: the
 * hot lookups ({@code getStyle}, {@code getNodeData}, {@code getParent}, {@code getChildren}, the
 * layout pass and the rounding pass) become one bounds check plus one array load instead of a hash
 * lookup. These tests pin the observable behavior the map used to provide - removed and fabricated
 * ids still read as absent and still throw the same exceptions, ids keep their meaning across
 * removals, reparenting and {@code clear()} - and they pin the memory policy: the array never grows
 * with the number of nodes ever created, only with the live set, and spilled ids migrate back into
 * the array when the tree grows over them.</p>
 */
@DisplayName("P15: dense node storage (node id as array index)")
public class DenseNodeStorageP15Test {

    private static final float EPSILON = 0.01f;

    private static TaffyStyle leaf(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle flexRow(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static List<NodeId> newLeaves(TaffyTree tree, int count) {
        List<NodeId> nodes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            nodes.add(tree.newLeaf(leaf(10f, 10f)));
        }
        return nodes;
    }

    // ==================== Dense indexing ====================

    @Test
    @DisplayName("node ids index the node array directly")
    void idsIndexTheNodeArrayDirectly() {
        TaffyTree tree = new TaffyTree();
        List<NodeId> nodes = newLeaves(tree, 50);

        for (int i = 0; i < nodes.size(); i++) {
            assertEquals(i, nodes.get(i).getId(), "ids are dense and ascending");
            assertNotNull(tree.getNodeData(nodes.get(i)));
            assertEquals(10f, tree.getStyle(nodes.get(i)).size.width.getValue(), EPSILON);
        }
        assertEquals(50, tree.totalNodeCount());
        assertEquals(0, tree.spilledNodeCount(), "a dense tree never spills");
        assertTrue(tree.nodeSlotCapacity() >= 50, "the array grew to cover every live id");
    }

    @Test
    @DisplayName("a requested initial capacity pre-allocates the array")
    void requestedInitialCapacityPreallocates() {
        TaffyTree tree = new TaffyTree(4096);
        assertTrue(tree.nodeSlotCapacity() >= 4096);

        newLeaves(tree, 100);
        assertEquals(0, tree.spilledNodeCount());
        assertEquals(100, tree.totalNodeCount());

        assertThrows(IllegalArgumentException.class, () -> new TaffyTree(-1));
    }

    // ==================== Removed / unknown ids ====================

    @Test
    @DisplayName("a removed id reads as absent without shifting later ids")
    void removedIdReadsAsAbsent() {
        TaffyTree tree = new TaffyTree();
        List<NodeId> nodes = newLeaves(tree, 5);
        NodeId removed = nodes.get(2);

        tree.remove(removed);

        assertFalse(tree.containsNode(removed));
        assertNull(tree.getNodeData(removed));
        assertNull(tree.getLayout(removed));
        assertNull(tree.getUnroundedLayout(removed));
        assertNull(tree.getParent(removed));
        assertNull(tree.getDetailedGridInfo(removed));
        assertEquals(0, tree.childCount(removed));
        assertTrue(tree.getChildren(removed).isEmpty());
        assertThrows(TaffyException.class, () -> tree.getStyle(removed));
        assertThrows(TaffyException.class, () -> tree.setStyle(removed, leaf(1f, 1f)));
        assertEquals(4, tree.totalNodeCount());

        // The ids above the hole keep their data: a hole is a null slot, not a shift.
        NodeId shifted = nodes.get(3);
        assertEquals(10f, tree.getStyle(shifted).size.width.getValue(), EPSILON);
        assertNotNull(tree.getNodeData(shifted));

        // Ids are never recycled: the next node takes the next counter value, not the hole.
        NodeId fresh = tree.newLeaf(leaf(3f, 3f));
        assertEquals(5, fresh.getId());
        assertNull(tree.getNodeData(removed));
        assertEquals(5, tree.totalNodeCount());
    }

    @Test
    @DisplayName("unknown and out-of-range ids behave exactly like missing entries")
    void unknownAndOutOfRangeIdsReadAsAbsent() {
        TaffyTree tree = new TaffyTree();
        NodeId parent = tree.newLeaf(leaf(100f, 100f));

        for (NodeId unknown : List.of(NodeId.of(-1L), NodeId.of(9_999L), NodeId.of(Long.MAX_VALUE))) {
            assertFalse(tree.containsNode(unknown));
            assertNull(tree.getNodeData(unknown));
            assertNull(tree.getLayout(unknown));
            assertNull(tree.getParent(unknown));
            assertNull(tree.getMeasureFunc(unknown));
            assertNull(tree.getMeasureFunction(unknown));
            assertNull(tree.getNodeContext(unknown));
            assertNull(tree.getDetailedGridInfo(unknown));
            assertEquals(0, tree.childCount(unknown));
            assertTrue(tree.getChildren(unknown).isEmpty());
            assertThrows(TaffyException.class, () -> tree.getStyle(unknown));
            assertThrows(TaffyException.class, () -> tree.setNodeContext(unknown, "x"));
            assertThrows(TaffyException.class, () -> tree.hasNewLayout(unknown));
            assertNull(tree.getCacheEntry(unknown, LayoutInput.hidden()));
        }

        // A fabricated child id is still rejected, and an unknown parent still throws, without
        // creating a slot for the fabricated id.
        assertThrows(TaffyException.class, () -> tree.addChild(parent, NodeId.of(9_999L)));
        assertThrows(TaffyException.class, () -> tree.addChild(NodeId.of(9_999L), parent));
        assertThrows(TaffyException.class, () -> tree.getChildAtIndex(parent, 0));
        assertEquals(1, tree.totalNodeCount());
        assertFalse(tree.containsNode(NodeId.of(9_999L)));
    }

    // ==================== clear / storage release ====================

    @Test
    @DisplayName("clear drops every node and releases the storage")
    void clearReleasesStorage() {
        TaffyTree tree = new TaffyTree();
        List<NodeId> nodes = newLeaves(tree, 5_000);
        assertTrue(tree.nodeSlotCapacity() >= 5_000);

        tree.clear();

        assertEquals(0, tree.totalNodeCount());
        assertTrue(tree.getAllNodes().isEmpty());
        assertEquals(0, tree.spilledNodeCount());
        assertTrue(tree.nodeSlotCapacity() <= 64, "clear resets the array to its initial size");
        for (NodeId node : nodes) {
            assertFalse(tree.containsNode(node));
            assertNull(tree.getLayout(node));
        }

        NodeId afterClear = tree.newLeaf(leaf(7f, 7f));
        assertEquals(5_000, afterClear.getId(), "ids keep counting up across clear");
        assertEquals(1, tree.totalNodeCount());
        assertEquals(7f, tree.getStyle(afterClear).size.width.getValue(), EPSILON);
    }

    @Test
    @DisplayName("removing from the top returns the array memory")
    void topDownRemovalShrinksTheArray() {
        TaffyTree tree = new TaffyTree();
        List<NodeId> nodes = newLeaves(tree, 5_000);
        assertTrue(tree.nodeSlotCapacity() >= 5_000);

        for (int i = nodes.size() - 1; i >= 0; i--) {
            tree.remove(nodes.get(i));
        }

        assertEquals(0, tree.totalNodeCount());
        assertEquals(0, tree.spilledNodeCount());
        assertTrue(tree.nodeSlotCapacity() <= 64, "the empty prefix was shrunk to the floor");
    }

    // ==================== Sparse memory policy ====================

    @Test
    @DisplayName("clear + rebuild does not size storage by the number of created nodes")
    void clearAndRebuildDoesNotBloatStorage() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = leaf(4f, 4f);

        int cycles = 100;
        int perCycle = 500;
        for (int cycle = 0; cycle < cycles; cycle++) {
            List<NodeId> nodes = new ArrayList<>(perCycle);
            for (int i = 0; i < perCycle; i++) {
                nodes.add(tree.newLeaf(style));
            }
            assertEquals(perCycle, tree.totalNodeCount());
            for (NodeId node : nodes) {
                assertSame(style, tree.getStyle(node), "style identities round-trip through storage");
            }
            tree.clear();
        }

        // 50.000 ids were handed out, but a tree never holds more than 500 at a time: the arrays
        // must not grow with the id counter (an id-indexed array sized by the raw id would need
        // 50.000 slots here, and something like 16MB after a minute of frame-by-frame rebuilding).
        List<NodeId> live = newLeaves(tree, perCycle);
        assertEquals(perCycle, tree.totalNodeCount());
        assertTrue(tree.nodeSlotCapacity() <= 64,
            () -> "storage must stay proportional to the live set, was " + tree.nodeSlotCapacity());
        assertTrue(tree.spilledNodeCount() >= perCycle - 1,
            () -> "the sparse id range spilled instead of growing the array, was "
                + tree.spilledNodeCount());
        for (NodeId node : live) {
            assertNotNull(tree.getNodeData(node));
        }
        assertEquals(cycles * perCycle, live.get(0).getId());
    }

    @Test
    @DisplayName("spilled ids migrate back into the array when the tree grows over them")
    void spilledIdsMigrateBackOnGrowth() {
        TaffyTree tree = new TaffyTree();
        List<NodeId> first = newLeaves(tree, 2_000);
        for (int i = first.size() - 1; i >= 0; i--) {
            tree.remove(first.get(i));
        }
        assertEquals(0, tree.totalNodeCount());
        assertTrue(tree.nodeSlotCapacity() <= 64);

        // The counter is at 2000 now, so these ids are far outside a small array and start life in
        // the spilled map; once enough of them are alive, growth is admitted again and the spill is
        // pulled back into the array.
        List<NodeId> second = newLeaves(tree, 2_000);

        assertEquals(2_000, tree.totalNodeCount());
        assertEquals(0, tree.spilledNodeCount(),
            "growth covers the spilled ids and migrates them back");
        assertTrue(tree.nodeSlotCapacity() >= second.get(second.size() - 1).getId() + 1,
            "the array covers the highest live id");
        assertTrue(tree.nodeSlotCapacity() <= 8_192, "growth stays bounded by the live set");
        for (NodeId node : second) {
            assertNotNull(tree.getNodeData(node));
            assertEquals(10f, tree.getStyle(node).size.width.getValue(), EPSILON);
        }
        assertEquals(2_000, tree.getAllNodes().size());
    }

    // ==================== Structural behavior ====================

    @Test
    @DisplayName("totalNodeCount and getAllNodes track the live ids")
    void nodeCountAndGetAllNodesTrackLiveIds() {
        TaffyTree tree = new TaffyTree();
        List<NodeId> nodes = newLeaves(tree, 10);
        Set<NodeId> expected = new HashSet<>(nodes);
        assertEquals(expected, tree.getAllNodes());
        assertEquals(10, tree.totalNodeCount());

        tree.remove(nodes.get(0));
        tree.remove(nodes.get(4));
        tree.remove(nodes.get(9));
        expected.remove(nodes.get(0));
        expected.remove(nodes.get(4));
        expected.remove(nodes.get(9));

        assertEquals(expected, tree.getAllNodes());
        assertEquals(7, tree.totalNodeCount());

        tree.clear();
        assertTrue(tree.getAllNodes().isEmpty());
        assertEquals(0, tree.totalNodeCount());
    }

    @Test
    @DisplayName("child links keep working across the array-backed storage")
    void childLinksSurviveStorageRewrite() {
        TaffyTree tree = new TaffyTree();
        NodeId parent = tree.newLeaf(flexRow(100f, 100f));
        NodeId a = tree.newLeaf(leaf(30f, 10f));
        NodeId b = tree.newLeaf(leaf(30f, 10f));
        NodeId c = tree.newLeaf(leaf(30f, 10f));
        tree.setChildren(parent, a, b, c);

        assertEquals(List.of(a, b, c), tree.getChildren(parent));
        assertSame(parent, tree.getParent(a));
        assertEquals(3, tree.childCount(parent));

        NodeId d = tree.newLeaf(leaf(10f, 10f));
        tree.insertChildAtIndex(parent, 1, d);
        assertEquals(List.of(a, d, b, c), tree.getChildren(parent));

        NodeId e = tree.newLeaf(leaf(10f, 10f));
        NodeId replaced = tree.replaceChildAtIndex(parent, 3, e);
        assertSame(c, replaced);
        assertEquals(List.of(a, d, b, e), tree.getChildren(parent));
        assertSame(parent, tree.getParent(e));
        assertNull(tree.getParent(c), "the replaced child is detached but still a node");
        assertTrue(tree.containsNode(c));

        NodeId removed = tree.removeChildAtIndex(parent, 0);
        assertSame(a, removed);
        assertNull(tree.getParent(a));
        assertEquals(List.of(d, b, e), tree.getChildren(parent));
        assertEquals(6, tree.totalNodeCount(), "detached nodes still count as nodes");
    }

    @Test
    @DisplayName("parent links round-trip through the spill map")
    void parentLinksRoundTripThroughSpillMap() {
        TaffyTree tree = new TaffyTree();
        List<NodeId> filler = newLeaves(tree, 2_000);
        for (int i = filler.size() - 1; i >= 0; i--) {
            tree.remove(filler.get(i));
        }
        assertEquals(0, tree.totalNodeCount());

        // Both ids are far above the array the removals left behind, so the parent link is spilled.
        NodeId parent = tree.newLeaf(leaf(50f, 50f));
        NodeId child = tree.newLeaf(leaf(10f, 10f));
        assertTrue(tree.spilledNodeCount() >= 2);
        tree.addChild(parent, child);

        assertSame(parent, tree.getParent(child), "getParent returns the recorded NodeId instance");
        assertEquals(List.of(child), tree.getChildren(parent));

        tree.removeChild(parent, child);
        assertNull(tree.getParent(child));

        tree.addChild(parent, child);
        assertSame(parent, tree.getParent(child));
        tree.remove(parent);
        assertFalse(tree.containsNode(parent));
        assertNull(tree.getParent(child), "removing the parent detaches its children");
        assertEquals(1, tree.totalNodeCount());
    }

    @Test
    @DisplayName("spilled parent links migrate back into the array on growth")
    void spilledParentLinksMigrateOnGrowth() {
        TaffyTree tree = new TaffyTree();
        List<NodeId> filler = newLeaves(tree, 2_000);
        for (int i = filler.size() - 1; i >= 0; i--) {
            tree.remove(filler.get(i));
        }

        NodeId parent = tree.newLeaf(leaf(50f, 50f));
        NodeId child = tree.newLeaf(leaf(10f, 10f));
        tree.addChild(parent, child);
        assertTrue(tree.spilledNodeCount() > 0, "the ids start life spilled");

        // Around id 2286 the growing live set makes the array cheap again, so growth resumes and
        // pulls the spilled node data and the spilled parent link back into the arrays.
        newLeaves(tree, 400);
        assertTrue(tree.nodeSlotCapacity() > parent.getId(), "growth covers the spilled ids");
        assertEquals(0, tree.spilledNodeCount(), "spilled parent links migrate with their nodes");
        assertSame(parent, tree.getParent(child), "the migrated link survives the move");
        assertEquals(402, tree.totalNodeCount());
    }

    @Test
    @DisplayName("layout runs over the array-backed storage with unchanged geometry")
    void layoutGeometryIsUnchanged() {
        TaffyTree tree = new TaffyTree();
        NodeId parent = tree.newLeaf(flexRow(100f, 100f));
        NodeId first = tree.newLeaf(leaf(30f, 10f));
        NodeId second = tree.newLeaf(leaf(30f, 10f));
        NodeId third = tree.newLeaf(leaf(30f, 10f));
        tree.setChildren(parent, first, second, third);

        tree.computeLayout(parent, TaffySize.maxContent());

        assertEquals(0f, tree.getLayout(first).location().x, EPSILON);
        assertEquals(30f, tree.getLayout(second).location().x, EPSILON);
        assertEquals(60f, tree.getLayout(third).location().x, EPSILON);
        assertEquals(100f, tree.getLayout(parent).size().width, EPSILON);

        // Removing a node in the middle then laying out again still works from the array.
        tree.remove(second);
        tree.computeLayout(parent, TaffySize.maxContent());
        assertEquals(2, tree.childCount(parent));
        assertEquals(30f, tree.getLayout(third).location().x, EPSILON);
        assertEquals(3, tree.totalNodeCount());
    }

    @Test
    @DisplayName("a node id that fits no slot is stored, read and dropped through the spill map")
    void spillMapRoundTripsSingleNode() {
        TaffyTree tree = new TaffyTree();
        List<NodeId> nodes = newLeaves(tree, 2_000);
        for (int i = nodes.size() - 1; i >= 0; i--) {
            tree.remove(nodes.get(i));
        }
        assertEquals(0, tree.totalNodeCount());

        // Id 2000 is far above the 64-slot array the removals left behind, so it spills.
        NodeId spilled = tree.newLeaf(leaf(12f, 12f));
        assertEquals(2_000, spilled.getId());
        assertTrue(tree.spilledNodeCount() >= 1);
        assertNotNull(tree.getNodeData(spilled));
        assertEquals(12f, tree.getStyle(spilled).size.width.getValue(), EPSILON);

        tree.remove(spilled);
        assertEquals(0, tree.totalNodeCount());
        assertEquals(0, tree.spilledNodeCount(), "dropping the last spilled node frees the map");
        assertFalse(tree.containsNode(spilled));
    }

    @Test
    @DisplayName("maxContent layout of a spilled node and an array node agree")
    void spilledAndArrayNodesLayOutIdentically() {
        TaffyTree dense = new TaffyTree();
        NodeId denseRoot = dense.newLeaf(flexRow(60f, 20f));
        NodeId denseChild = dense.newLeaf(leaf(30f, 20f));
        dense.setChildren(denseRoot, denseChild);
        dense.computeLayout(denseRoot, new TaffySize<>(AvailableSpace.maxContent(), AvailableSpace.maxContent()));

        TaffyTree sparse = new TaffyTree();
        List<NodeId> filler = newLeaves(sparse, 2_000);
        for (int i = filler.size() - 1; i >= 0; i--) {
            sparse.remove(filler.get(i));
        }
        NodeId sparseRoot = sparse.newLeaf(flexRow(60f, 20f));
        NodeId sparseChild = sparse.newLeaf(leaf(30f, 20f));
        sparse.setChildren(sparseRoot, sparseChild);
        assertTrue(sparse.spilledNodeCount() > 0, "the sparse tree stores these nodes in the spill map");
        sparse.computeLayout(sparseRoot, new TaffySize<>(AvailableSpace.maxContent(), AvailableSpace.maxContent()));

        assertEquals(dense.getLayout(denseRoot).size().width, sparse.getLayout(sparseRoot).size().width, EPSILON);
        assertEquals(dense.getLayout(denseRoot).size().height, sparse.getLayout(sparseRoot).size().height, EPSILON);
        assertEquals(dense.getLayout(denseChild).size().width, sparse.getLayout(sparseChild).size().width, EPSILON);
        assertEquals(dense.getLayout(denseChild).location().x, sparse.getLayout(sparseChild).location().x, EPSILON);
    }
}
