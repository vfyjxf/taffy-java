package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P12 tests for the memoized resolved direction used by {@link LayoutComputer#resolveDirection}.
 *
 * <p>{@code direction} is inherited: a node styled {@code INHERIT} takes its nearest ancestor's
 * resolved direction and LTR when no ancestor states one. Layout used to walk the parent chain on
 * every query, and flex/grid/block ask once per child item plus once per measured leaf, which made
 * the walk 23-24% of layout time on wide trees (JProfiler, p11-profile). The pass now resolves
 * top-down and memoizes the answer on {@link NodeData}, stamped with the tree's direction epoch.
 * These tests pin the observable behavior: the layout geometry produced by nested RTL/LTR/INHERIT
 * chains, root RTL placement, re-resolution after a direction change (style object mutated in
 * place or replaced through {@code setStyle}), re-resolution after insert/move/reparent, and the
 * cache invariants themselves (every node resolved in the last pass, repeated queries stable, a
 * new pass re-resolving).</p>
 */
@DisplayName("P12: memoized direction resolution")
public class DirectionCacheP12Test {

    private static final float EPSILON = 0.01f;

    /** A 300x100-ish flex row container of the given size; direction is left at its default. */
    private static TaffyStyle flexRow(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.ROW;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    /** A fixed-size leaf with the default (INHERIT) direction. */
    private static TaffyStyle leaf(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    /**
     * Resolves {@code node} through the public semantic entry point and asserts both the value and
     * the memoization stamp.
     *
     * <p>A node is not necessarily visited by the last pass (a warm layout cache skips whole
     * subtrees), so the stamp is checked after a query instead of assuming the pass touched every
     * node. If a node already carries a value stamped with the current epoch, that value must
     * already be the correct one: a stale cache would show up there.</p>
     */
    private static void assertResolvedDirection(TaffyTree tree, NodeId node, TaffyDirection expected) {
        NodeData before = tree.getNodeData(node);
        assertNotNull(before, "node exists: " + node);
        if (before.getResolvedDirectionEpoch() == tree.currentDirectionEpoch()) {
            assertEquals(expected, before.getResolvedDirection(),
                "a value stamped with the current epoch is the current resolution: " + node);
        }
        assertEquals(expected, new LayoutComputer(tree, (MeasureFunc) null).resolveDirection(node),
            "resolved direction: " + node);
        assertEquals(tree.currentDirectionEpoch(), tree.getNodeData(node).getResolvedDirectionEpoch(),
            "the query memoized its answer for the current epoch: " + node);
    }

    /**
     * Marks every layout cache in the subtree dirty, i.e. forces a cold relayout of it.
     *
     * <p>A direction change that does not dirty a descendant (an in-place style mutation, a
     * re-parent) leaves that descendant's inner layout warm; since P28 the layout cache validates
     * the resolved direction at lookup time, so the geometry is still correct. The tests keep
     * forcing a cold relayout so their assertions do not depend on which entries happen to be
     * warm, exactly like the cold-layout benchmarks do; the direction memo itself is invalidated
     * by the epoch bump alone.</p>
     */
    private static void markSubtreeDirty(TaffyTree tree, NodeId root) {
        NodeData data = tree.getNodeData(root);
        if (data == null) {
            return;
        }
        data.markDirty();
        for (NodeId child : data.getChildren()) {
            markSubtreeDirty(tree, child);
        }
    }

    @Test
    @DisplayName("P12: nested INHERIT chain follows the RTL root, in layout geometry and in the cache")
    void nestedInheritFollowsRtlRoot() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexRow(300f, 100f);
        rootStyle.direction = TaffyDirection.RTL;

        NodeId leaf = tree.newLeaf(leaf(20f, 20f));
        NodeId middle = tree.newLeaf(flexRow(100f, 20f)); // INHERIT
        tree.addChild(middle, leaf);
        NodeId root = tree.newWithChildren(rootStyle, middle);

        tree.computeLayout(root, TaffySize.maxContent());

        // RTL row: the only item sits against the root's right edge.
        assertEquals(200f, tree.getLayout(middle).location().x, EPSILON,
            "INHERIT container follows the RTL root");
        // ... and the leaf sits against that container's right edge (also inherited).
        assertEquals(80f, tree.getLayout(leaf).location().x, EPSILON,
            "INHERIT leaf follows the inherited RTL direction");

        assertResolvedDirection(tree, root, TaffyDirection.RTL);
        assertResolvedDirection(tree, middle, TaffyDirection.RTL);
        assertResolvedDirection(tree, leaf, TaffyDirection.RTL);
    }

    @Test
    @DisplayName("P12: an explicit LTR inside an RTL tree stops inheritance for its subtree")
    void explicitLtrInsideRtlStopsInheritance() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexRow(300f, 100f);
        rootStyle.direction = TaffyDirection.RTL;
        TaffyStyle middleStyle = flexRow(100f, 20f);
        middleStyle.direction = TaffyDirection.LTR;

        NodeId leaf = tree.newLeaf(leaf(20f, 20f));
        NodeId middle = tree.newLeaf(middleStyle);
        tree.addChild(middle, leaf);
        NodeId root = tree.newWithChildren(rootStyle, middle);

        tree.computeLayout(root, TaffySize.maxContent());

        assertEquals(200f, tree.getLayout(middle).location().x, EPSILON,
            "the RTL root still places its item from the right");
        assertEquals(0f, tree.getLayout(leaf).location().x, EPSILON,
            "the leaf inherits the explicit LTR from its parent, not the RTL root");

        assertResolvedDirection(tree, middle, TaffyDirection.LTR);
        assertResolvedDirection(tree, leaf, TaffyDirection.LTR);
    }

    @Test
    @DisplayName("P12: a parent direction change is observed by descendants on the next relayout")
    void parentDirectionChangeReresolvesDescendants() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexRow(300f, 100f);
        rootStyle.direction = TaffyDirection.LTR;

        NodeId leaf = tree.newLeaf(leaf(20f, 20f));
        NodeId middle = tree.newLeaf(flexRow(100f, 20f)); // INHERIT, so a change here matters
        tree.addChild(middle, leaf);
        NodeId root = tree.newWithChildren(rootStyle, middle);

        tree.computeLayout(root, TaffySize.maxContent());
        assertEquals(0f, tree.getLayout(middle).location().x, EPSILON, "LTR root places from the left");
        assertEquals(0f, tree.getLayout(leaf).location().x, EPSILON);
        long firstEpoch = tree.currentDirectionEpoch();

        // 1) In-place style mutation (getStyle returns the live style object): only a new pass can
        //    notice it, which is what the pass-start epoch bump is for.
        tree.getStyle(root).direction = TaffyDirection.RTL;
        markSubtreeDirty(tree, root);
        tree.computeLayout(root, TaffySize.maxContent());
        assertTrue(tree.currentDirectionEpoch() > firstEpoch, "a new pass starts a new epoch");
        assertEquals(200f, tree.getLayout(middle).location().x, EPSILON,
            "in-place root direction change re-resolves the subtree");
        assertEquals(80f, tree.getLayout(leaf).location().x, EPSILON,
            "descendant follows the changed root direction");
        assertResolvedDirection(tree, leaf, TaffyDirection.RTL);

        // 2) Ancestor (not root) direction change through setStyle: the whole subtree below the
        //    changed node must be re-resolved, not just the node itself.
        TaffyStyle middleLtr = flexRow(100f, 20f);
        middleLtr.direction = TaffyDirection.LTR;
        tree.setStyle(middle, middleLtr);
        markSubtreeDirty(tree, middle);
        tree.computeLayout(root, TaffySize.maxContent());
        assertEquals(0f, tree.getLayout(leaf).location().x, EPSILON,
            "changing an ancestor's direction re-resolves its descendants");
        assertResolvedDirection(tree, leaf, TaffyDirection.LTR);

        // 3) setStyle on the root back to LTR: the inherited direction flips again.
        TaffyStyle rootLtr = flexRow(300f, 100f);
        rootLtr.direction = TaffyDirection.LTR;
        tree.setStyle(root, rootLtr);
        markSubtreeDirty(tree, root);
        tree.computeLayout(root, TaffySize.maxContent());
        assertEquals(0f, tree.getLayout(middle).location().x, EPSILON);
        assertEquals(0f, tree.getLayout(leaf).location().x, EPSILON);
    }

    @Test
    @DisplayName("P12: inserting, moving and reparenting a subtree re-resolves its inherited direction")
    void insertMoveAndReparentReresolveSubtree() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexRow(300f, 20f); // INHERIT -> LTR
        TaffyStyle rtlBoxStyle = flexRow(100f, 20f);
        rtlBoxStyle.direction = TaffyDirection.RTL;
        TaffyStyle ltrBoxStyle = flexRow(100f, 20f); // INHERIT -> LTR

        NodeId rtlBox = tree.newLeaf(rtlBoxStyle);
        NodeId ltrBox = tree.newLeaf(ltrBoxStyle);
        NodeId moved = tree.newLeaf(flexRow(100f, 20f)); // INHERIT: direction depends on its parent
        NodeId leaf = tree.newLeaf(leaf(20f, 20f));      // INHERIT
        tree.addChild(moved, leaf);
        NodeId root = tree.newWithChildren(rootStyle, rtlBox, ltrBox);

        // Insert (not append): the subtree starts out under the RTL box.
        tree.insertChildAtIndex(rtlBox, 0, moved);
        tree.computeLayout(root, TaffySize.maxContent());
        assertEquals(80f, tree.getLayout(leaf).location().x, EPSILON,
            "freshly inserted subtree inherits the RTL parent");
        assertResolvedDirection(tree, leaf, TaffyDirection.RTL);

        // Move: remove from the RTL box and add to the LTR box.
        tree.removeChild(rtlBox, moved);
        tree.addChild(ltrBox, moved);
        markSubtreeDirty(tree, moved);
        tree.computeLayout(root, TaffySize.maxContent());
        assertEquals(0f, tree.getLayout(leaf).location().x, EPSILON,
            "moved subtree re-resolves against the LTR parent");
        assertResolvedDirection(tree, leaf, TaffyDirection.LTR);

        // Reparent through setChildren: back under the RTL box.
        tree.setChildren(ltrBox);
        tree.setChildren(rtlBox, moved);
        markSubtreeDirty(tree, moved);
        tree.computeLayout(root, TaffySize.maxContent());
        assertEquals(80f, tree.getLayout(leaf).location().x, EPSILON,
            "reparented subtree re-resolves against the RTL parent");
        assertResolvedDirection(tree, leaf, TaffyDirection.RTL);
    }

    @Test
    @DisplayName("P12: an RTL root is placed against the ICB's right edge and its children inherit it")
    void rootRtlPlacesRootAndChildren() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexRow(100f, 50f);
        rootStyle.direction = TaffyDirection.RTL;
        NodeId child = tree.newLeaf(leaf(50f, 50f));
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(300f),
            AvailableSpace.definite(100f)
        ));

        // x = availableWidth - size.width - margin.right, unchanged by the cache.
        assertEquals(200f, tree.getLayout(root).location().x, EPSILON, "RTL root placement");
        assertEquals(50f, tree.getLayout(child).location().x, EPSILON,
            "the child inherits the root's RTL direction");
        assertResolvedDirection(tree, child, TaffyDirection.RTL);
    }

    @Test
    @DisplayName("P12: the cache resolves a leaf-first query top-down and stays stable across queries")
    void cacheIsResolvedTopDownAndStable() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexRow(300f, 100f);
        rootStyle.direction = TaffyDirection.RTL;

        NodeId deepest = tree.newLeaf(leaf(10f, 10f));
        NodeId middle = tree.newLeaf(flexRow(50f, 20f));
        NodeId top = tree.newLeaf(flexRow(100f, 20f));
        tree.addChild(top, middle);
        tree.addChild(middle, deepest);
        NodeId root = tree.newWithChildren(rootStyle, top);

        // Query the deepest node first, outside a layout pass: the top-down walk must memoize
        // every ancestor it consults, so the later queries are cache hits.
        LayoutComputer computer = new LayoutComputer(tree, (MeasureFunc) null);
        long epochBeforeQuery = tree.currentDirectionEpoch();
        assertEquals(TaffyDirection.RTL, computer.resolveDirection(deepest),
            "leaf-first query resolves the whole chain");
        assertEquals(epochBeforeQuery, tree.currentDirectionEpoch(),
            "resolving directions does not start a pass");
        assertResolvedDirection(tree, root, TaffyDirection.RTL);
        assertResolvedDirection(tree, top, TaffyDirection.RTL);
        assertResolvedDirection(tree, middle, TaffyDirection.RTL);
        assertResolvedDirection(tree, deepest, TaffyDirection.RTL);

        // Repeated queries in the same epoch hit the memoized values and change nothing.
        for (int i = 0; i < 5; i++) {
            assertEquals(TaffyDirection.RTL, computer.resolveDirection(deepest));
            assertEquals(TaffyDirection.RTL, computer.resolveDirection(top));
        }
        assertEquals(epochBeforeQuery, tree.currentDirectionEpoch(), "cache hits do not bump the epoch");

        // A full layout pass re-resolves everything from the same styles and leaves the same
        // direction behind, so repeated layout is idempotent for direction.
        tree.computeLayout(root, TaffySize.maxContent());
        float firstLeafX = tree.getLayout(deepest).location().x;
        float firstTopX = tree.getLayout(top).location().x;
        assertResolvedDirection(tree, deepest, TaffyDirection.RTL);
        for (int i = 0; i < 3; i++) {
            tree.computeLayout(root, TaffySize.maxContent());
            assertEquals(firstTopX, tree.getLayout(top).location().x, EPSILON);
            assertEquals(firstLeafX, tree.getLayout(deepest).location().x, EPSILON);
            assertResolvedDirection(tree, deepest, TaffyDirection.RTL);
        }
    }

    @Test
    @DisplayName("P12: an INHERIT root with no parent resolves to the LTR initial value")
    void inheritRootResolvesToDefault() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = flexRow(100f, 50f); // direction stays INHERIT
        NodeId child = tree.newLeaf(leaf(20f, 20f));
        NodeId root = tree.newWithChildren(rootStyle, child);

        LayoutComputer computer = new LayoutComputer(tree, (MeasureFunc) null);
        assertEquals(TaffyDirection.DEFAULT, computer.resolveDirection(root),
            "no ancestor states a direction: CSS initial value LTR");
        assertEquals(TaffyDirection.LTR, computer.resolveDirection(child));

        tree.computeLayout(root, TaffySize.maxContent());
        assertEquals(0f, tree.getLayout(child).location().x, EPSILON);
        assertResolvedDirection(tree, root, TaffyDirection.LTR);
        assertResolvedDirection(tree, child, TaffyDirection.LTR);
    }
}
