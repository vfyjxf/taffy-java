package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyFloat;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * P17 tests for the cross-pass block item-list cache in {@link BlockComputer}.
 *
 * <p>The P13 cache only lived for one layout pass. LDLib2's per-frame form update changes a single
 * label's style and recomputes the form root, so a pass-scoped cache still rebuilds every block
 * container's item list from scratch. The list is now stored on the container's {@link NodeData}
 * and reused while the container's own content version, its child-list version, every direct
 * child's content version, the resolved direction and the inner-size basis are unchanged.</p>
 *
 * <p>These tests pin the observable contract: a child style change rebuilds only its direct
 * parent's list (not its grandparents'), child-list and measure/direction/percentage changes are
 * observed, and a reused list produces exactly the layouts a freshly built tree produces.</p>
 */
@DisplayName("P17: cross-pass block item-list cache")
public class BlockItemCrossPassP17Test {

    private static final float EPSILON = 0.001f;

    private static TaffySize<AvailableSpace> layoutSize(float width) {
        return TaffySize.of(AvailableSpace.definite(width), AvailableSpace.maxContent());
    }

    private static TaffyStyle block() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        return style;
    }

    private static TaffyStyle sizedBlock(float width) {
        TaffyStyle style = block();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.AUTO);
        return style;
    }

    private static TaffyStyle leaf(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle percentagePaddingLeaf(float width, float height) {
        TaffyStyle style = leaf(width, height);
        style.boxSizing = BoxSizing.CONTENT_BOX;
        style.padding = TaffyRect.all(LengthPercentage.percent(0.1f));
        return style;
    }

    private static BlockComputer.BlockItemCache itemCache(TaffyTree tree, NodeId node) {
        return tree.getNodeData(node).getBlockItemCache();
    }

    /**
     * The LDLib2 shape: changing one label's style must rebuild its direct panel's item list but
     * let the form root reuse the item lists it holds for every panel.
     */
    @Test
    void styleChangeOnOneLeafReusesAncestorItemLists() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        NodeId leafA1 = tree.newLeaf(leaf(20f, 10f));
        NodeId leafA2 = tree.newLeaf(leaf(20f, 10f));
        NodeId panelA = tree.newWithChildren(block(), new NodeId[]{leafA1, leafA2});
        NodeId leafB1 = tree.newLeaf(leaf(20f, 10f));
        NodeId leafB2 = tree.newLeaf(leaf(20f, 10f));
        NodeId panelB = tree.newWithChildren(block(), new NodeId[]{leafB1, leafB2});
        NodeId root = tree.newWithChildren(block(), new NodeId[]{panelA, panelB});

        tree.computeLayout(root, layoutSize(300f));
        BlockComputer.BlockItemCache rootCache = itemCache(tree, root);
        BlockComputer.BlockItemCache panelACache = itemCache(tree, panelA);
        BlockComputer.BlockItemCache panelBCache = itemCache(tree, panelB);
        assertNotNull(rootCache);
        assertNotNull(panelACache);
        assertNotNull(panelBCache);

        tree.setStyle(leafA1, leaf(40f, 10f));
        tree.computeLayout(root, layoutSize(300f));

        // The root only sees panel styles, which did not change; the untouched sibling panel is
        // equally unaffected. The changed leaf's direct panel has to rebuild.
        assertSame(rootCache, itemCache(tree, root));
        assertSame(panelBCache, itemCache(tree, panelB));
        assertNotSame(panelACache, itemCache(tree, panelA));
        assertEquals(40f, tree.getLayout(leafA1).size().width, EPSILON);
    }

    /** Adding and removing a child rebuilds only that container, not its ancestors or siblings. */
    @Test
    void childListChangeRebuildsOnlyTheChangedContainer() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        NodeId leafA1 = tree.newLeaf(leaf(20f, 10f));
        NodeId leafA2 = tree.newLeaf(leaf(20f, 10f));
        NodeId panelA = tree.newWithChildren(block(), new NodeId[]{leafA1, leafA2});
        NodeId leafB1 = tree.newLeaf(leaf(20f, 10f));
        NodeId leafB2 = tree.newLeaf(leaf(20f, 10f));
        NodeId panelB = tree.newWithChildren(block(), new NodeId[]{leafB1, leafB2});
        NodeId root = tree.newWithChildren(block(), new NodeId[]{panelA, panelB});

        tree.computeLayout(root, layoutSize(300f));
        BlockComputer.BlockItemCache rootCache = itemCache(tree, root);
        BlockComputer.BlockItemCache panelACache = itemCache(tree, panelA);
        BlockComputer.BlockItemCache panelBCache = itemCache(tree, panelB);
        assertNotNull(rootCache);
        assertNotNull(panelACache);
        assertNotNull(panelBCache);

        NodeId leafA3 = tree.newLeaf(leaf(20f, 10f));
        tree.addChild(panelA, leafA3);
        tree.computeLayout(root, layoutSize(300f));

        assertSame(rootCache, itemCache(tree, root));
        assertSame(panelBCache, itemCache(tree, panelB));
        assertNotSame(panelACache, itemCache(tree, panelA));
        assertEquals(30f, tree.getLayout(panelA).size().height, EPSILON);

        BlockComputer.BlockItemCache grownPanelCache = itemCache(tree, panelA);
        tree.removeChild(panelA, leafA3);
        tree.computeLayout(root, layoutSize(300f));

        assertSame(rootCache, itemCache(tree, root));
        assertSame(panelBCache, itemCache(tree, panelB));
        assertNotSame(grownPanelCache, itemCache(tree, panelA));
        assertEquals(20f, tree.getLayout(panelA).size().height, EPSILON);
    }

    /** The first step keeps single-item containers out of the per-node cache. */
    @Test
    void singleItemContainerKeepsNoCrossPassCache() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        NodeId first = tree.newLeaf(leaf(20f, 10f));
        NodeId second = tree.newLeaf(leaf(20f, 10f));
        NodeId root = tree.newWithChildren(block(), new NodeId[]{first, second});

        tree.computeLayout(root, layoutSize(300f));
        assertNotNull(itemCache(tree, root));

        tree.removeChild(root, second);
        tree.computeLayout(root, layoutSize(300f));

        assertNull(itemCache(tree, root));
        assertEquals(10f, tree.getLayout(root).size().height, EPSILON);
    }

    /**
     * Replacing a measure callback (new-style or legacy) and changing the node context must be
     * observed; only the measured leaf's direct parent rebuilds.
     */
    @Test
    void measureCallbackAndContextChangesAreObserved() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        NodeId measured = tree.newLeaf(new TaffyStyle());
        tree.setMeasureFunction(measured, context -> LayoutOutput.fromOuterSize(new FloatSize(10f, 10f)));
        NodeId sibling = tree.newLeaf(leaf(20f, 10f));
        NodeId panelA = tree.newWithChildren(block(), new NodeId[]{measured, sibling});
        NodeId leafB1 = tree.newLeaf(leaf(20f, 10f));
        NodeId leafB2 = tree.newLeaf(leaf(20f, 10f));
        NodeId panelB = tree.newWithChildren(block(), new NodeId[]{leafB1, leafB2});
        NodeId root = tree.newWithChildren(block(), new NodeId[]{panelA, panelB});

        tree.computeLayout(root, layoutSize(300f));
        BlockComputer.BlockItemCache rootCache = itemCache(tree, root);
        BlockComputer.BlockItemCache panelACache = itemCache(tree, panelA);
        BlockComputer.BlockItemCache panelBCache = itemCache(tree, panelB);
        assertNotNull(rootCache);
        assertEquals(10f, tree.getLayout(measured).size().height, EPSILON);

        tree.setMeasureFunction(measured, context -> LayoutOutput.fromOuterSize(new FloatSize(10f, 40f)));
        tree.computeLayout(root, layoutSize(300f));

        assertSame(rootCache, itemCache(tree, root));
        assertSame(panelBCache, itemCache(tree, panelB));
        assertNotSame(panelACache, itemCache(tree, panelA));
        assertEquals(40f, tree.getLayout(measured).size().height, EPSILON);

        BlockComputer.BlockItemCache grownPanelCache = itemCache(tree, panelA);
        tree.setMeasureFunc(measured, (knownDimensions, availableSpace) -> new FloatSize(10f, 20f));
        tree.computeLayout(root, layoutSize(300f));

        assertSame(rootCache, itemCache(tree, root));
        assertNotSame(grownPanelCache, itemCache(tree, panelA));
        assertEquals(20f, tree.getLayout(measured).size().height, EPSILON);

        // A new context changes what the callback returns even though no style changed.
        BlockComputer.BlockItemCache contextPanelCache = itemCache(tree, panelA);
        tree.setMeasureFunction(measured, context ->
            LayoutOutput.fromOuterSize(new FloatSize(10f, context.nodeContext() == null ? 20f : 30f)));
        tree.setNodeContext(measured, new Object());
        tree.computeLayout(root, layoutSize(300f));

        assertNotSame(contextPanelCache, itemCache(tree, panelA));
        assertEquals(30f, tree.getLayout(measured).size().height, EPSILON);
    }

    /**
     * A direction inherited from an ancestor changes the container's resolved direction. The
     * container's own style did not change, so only the direction comparison in the cache can
     * reject the old list; the out-of-flow static position proves the rebuilt items are used.
     */
    @Test
    void directionChangeInvalidatesContainerItemList() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle absoluteStyle = new TaffyStyle();
        absoluteStyle.position = TaffyPosition.ABSOLUTE;
        absoluteStyle.size = new TaffySize<>(TaffyDimension.length(10f), TaffyDimension.length(10f));
        absoluteStyle.justifySelf = AlignItems.SELF_START;
        NodeId absolute = tree.newLeaf(absoluteStyle);
        NodeId inFlow = tree.newLeaf(leaf(20f, 10f));
        NodeId panel = tree.newWithChildren(block(), new NodeId[]{absolute, inFlow});
        NodeId root = tree.newWithChildren(sizedBlock(300f), new NodeId[]{panel});

        tree.computeLayout(root, layoutSize(300f));
        BlockComputer.BlockItemCache ltrPanelCache = itemCache(tree, panel);
        assertNotNull(ltrPanelCache);
        assertEquals(0f, tree.getLayout(absolute).location().x, EPSILON);

        TaffyStyle rtlRoot = sizedBlock(300f);
        rtlRoot.direction = TaffyDirection.RTL;
        tree.setStyle(root, rtlRoot);
        tree.computeLayout(root, layoutSize(300f));

        assertNotSame(ltrPanelCache, itemCache(tree, panel));
        assertEquals(290f, tree.getLayout(absolute).location().x, EPSILON);

        // A real pass with no input change must reuse the RTL list and keep the same geometry.
        BlockComputer.BlockItemCache rtlPanelCache = itemCache(tree, panel);
        tree.invalidateLayoutReuse();
        tree.computeLayout(root, layoutSize(300f));
        assertSame(rtlPanelCache, itemCache(tree, panel));
        assertEquals(290f, tree.getLayout(absolute).location().x, EPSILON);
    }

    /** Percentage padding is resolved against the inner size; a new basis rebuilds the list. */
    @Test
    void percentagePaddingRevalidatesWhenInnerSizeChanges() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        NodeId first = tree.newLeaf(percentagePaddingLeaf(20f, 10f));
        NodeId second = tree.newLeaf(percentagePaddingLeaf(20f, 10f));
        NodeId root = tree.newWithChildren(block(), new NodeId[]{first, second});

        tree.computeLayout(root, layoutSize(200f));
        BlockComputer.BlockItemCache narrowCache = itemCache(tree, root);
        assertNotNull(narrowCache);
        assertEquals(50f, tree.getLayout(first).size().height, EPSILON);
        assertEquals(20f, tree.getLayout(first).padding().left, EPSILON);

        // Same basis: a real pass reuses the list and still reports the same values.
        tree.invalidateLayoutReuse();
        tree.computeLayout(root, layoutSize(200f));
        assertSame(narrowCache, itemCache(tree, root));
        assertEquals(50f, tree.getLayout(first).size().height, EPSILON);

        // Wider containing block: the cached list is invalid, and the percentage padding must
        // resolve against the new 300px inner width.
        tree.computeLayout(root, layoutSize(300f));
        assertNotSame(narrowCache, itemCache(tree, root));
        assertEquals(70f, tree.getLayout(first).size().height, EPSILON);
        assertEquals(30f, tree.getLayout(first).padding().left, EPSILON);

        // And back, so a previously cached basis cannot sneak back in.
        tree.computeLayout(root, layoutSize(200f));
        assertEquals(50f, tree.getLayout(first).size().height, EPSILON);
    }

    /**
     * A reused ancestor item list must produce exactly the layouts a tree built from scratch
     * produces: every {@link Layout} field of every node is compared, not just the root size.
     */
    @Test
    void reusedAncestorListMatchesFreshlyBuiltTreeFieldByField() {
        TaffyTree cachedTree = new TaffyTree();
        cachedTree.disableRounding();
        Form cachedForm = buildForm(cachedTree, false);
        NodeId cachedRoot = cachedForm.root();
        cachedTree.computeLayout(cachedRoot, layoutSize(320f));
        BlockComputer.BlockItemCache rootCache = itemCache(cachedTree, cachedRoot);
        assertNotNull(rootCache);

        // Change one deep label after a completed pass, then recompute: every ancestor above the
        // label's panel reuses its item list.
        cachedTree.setStyle(cachedForm.changedLeaf(), leaf(37f, 13f));
        cachedTree.computeLayout(cachedRoot, layoutSize(320f));
        assertSame(rootCache, itemCache(cachedTree, cachedRoot));

        TaffyTree freshTree = new TaffyTree();
        freshTree.disableRounding();
        Form freshForm = buildForm(freshTree, true);
        freshTree.computeLayout(freshForm.root(), layoutSize(320f));

        assertEquals(cachedForm.nodes().size(), freshForm.nodes().size());
        for (int i = 0; i < cachedForm.nodes().size(); i++) {
            Layout expected = freshTree.getLayout(freshForm.nodes().get(i));
            Layout actual = cachedTree.getLayout(cachedForm.nodes().get(i));
            assertNotNull(expected);
            assertNotNull(actual);
            assertLayoutEquals(expected, actual, "node " + i);
        }
    }

    private record Form(List<NodeId> nodes, NodeId root, NodeId changedLeaf) {}

    /**
     * Builds the same small form in both trees. {@code changed} makes the leaf that the cached
     * tree mutates after its first pass start out in its final style in the fresh tree.
     */
    private static Form buildForm(TaffyTree tree, boolean changed) {
        List<NodeId> nodes = new ArrayList<>();

        NodeId header = tree.newLeaf(leaf(100f, 12f));
        nodes.add(header);
        NodeId body = tree.newWithChildren(block(), new NodeId[]{header});
        nodes.add(body);

        NodeId labelA = tree.newLeaf(changed ? leaf(37f, 13f) : leaf(20f, 10f));
        nodes.add(labelA);
        TaffyStyle padded = leaf(45f, 11f);
        padded.padding = TaffyRect.ltrb(
            LengthPercentage.length(3f),
            LengthPercentage.length(4f),
            LengthPercentage.length(5f),
            LengthPercentage.length(6f)
        );
        padded.border = TaffyRect.all(LengthPercentage.length(1f));
        NodeId labelB = tree.newLeaf(padded);
        nodes.add(labelB);
        NodeId panelA = tree.newWithChildren(block(), new NodeId[]{labelA, labelB});
        nodes.add(panelA);

        TaffyStyle absoluteStyle = new TaffyStyle();
        absoluteStyle.position = TaffyPosition.ABSOLUTE;
        absoluteStyle.size = new TaffySize<>(TaffyDimension.length(9f), TaffyDimension.length(8f));
        absoluteStyle.justifySelf = AlignItems.SELF_END;
        NodeId absolute = tree.newLeaf(absoluteStyle);
        nodes.add(absolute);
        TaffyStyle floatedStyle = leaf(15f, 6f);
        floatedStyle.float_ = TaffyFloat.LEFT;
        NodeId floated = tree.newLeaf(floatedStyle);
        nodes.add(floated);
        NodeId panelB = tree.newWithChildren(block(), new NodeId[]{absolute, floated});
        nodes.add(panelB);

        NodeId footer = tree.newLeaf(leaf(60f, 9f));
        nodes.add(footer);

        NodeId root = tree.newWithChildren(rootStyle(), new NodeId[]{body, panelA, panelB, footer});
        nodes.add(root);
        return new Form(nodes, root, labelA);
    }

    private static TaffyStyle rootStyle() {
        TaffyStyle style = sizedBlock(320f);
        style.padding = TaffyRect.all(LengthPercentage.length(2f));
        return style;
    }

    private static void assertLayoutEquals(Layout expected, Layout actual, String label) {
        assertEquals(expected.order(), actual.order(), label + " order");
        assertEquals(expected.location(), actual.location(), label + " location");
        assertEquals(expected.size(), actual.size(), label + " size");
        assertEquals(expected.contentSize(), actual.contentSize(), label + " contentSize");
        assertEquals(expected.scrollableOverflow(), actual.scrollableOverflow(), label + " scrollableOverflow");
        assertEquals(expected.scrollbarSize(), actual.scrollbarSize(), label + " scrollbarSize");
        assertEquals(expected.border(), actual.border(), label + " border");
        assertEquals(expected.padding(), actual.padding(), label + " padding");
        assertEquals(expected.margin(), actual.margin(), label + " margin");
    }
}
