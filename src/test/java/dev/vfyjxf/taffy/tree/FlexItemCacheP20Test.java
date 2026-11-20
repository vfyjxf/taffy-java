package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * P20 tests for the flex item-list cache in {@link FlexboxComputer}.
 *
 * <p>{@code generateFlexItems} used to rebuild every {@link FlexboxComputer}'s item metadata on
 * every compute call. One layout pass lays a flex container out several times - a flex base-size
 * measurement, a grid track-sizing measurement, the final layout - so the pass now caches the list
 * on a thread-local keyed by the tree and its direction epoch and reuses it while the inner size
 * stays the same (or when no child style resolves against that size at all). A later pass reuses
 * the list stored on the container's {@link NodeData} while its own content version, child-list
 * version, every child's content version, the resolved direction and the inner-size basis are
 * unchanged.</p>
 *
 * <p>These tests pin the observable contract: percentage values must resolve against the inner
 * size of the pass that uses them, modal results (auto margins, target and hypothetical sizes,
 * freeze flags) must not leak from one use of a reused list into the next, and a reused list must
 * produce exactly the layouts a freshly built tree produces.</p>
 */
@DisplayName("P20: flex item-list reuse")
public class FlexItemCacheP20Test {

    private static final float EPSILON = 0.001f;

    private static TaffySize<AvailableSpace> layoutSize(float width, float height) {
        return TaffySize.of(AvailableSpace.definite(width), AvailableSpace.definite(height));
    }

    private static TaffyStyle flexRow() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.ROW;
        return style;
    }

    private static TaffyStyle flexColumn() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.COLUMN;
        return style;
    }

    private static TaffyStyle grid() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        return style;
    }

    private static TaffyStyle leaf(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    /**
     * A flex container used as a grid item is measured at several known widths while a single
     * {@code 1fr} track is sized, then laid out at the final width. Its percentage-sized children
     * must resolve against the final inner width, not against an earlier measurement basis.
     */
    @Test
    void percentageChildSizeResolvesAgainstTheFinalGridItemWidth() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle labelStyle = new TaffyStyle();
        labelStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.length(10f));
        NodeId label = tree.newLeaf(labelStyle);

        TaffyStyle cardStyle = flexColumn();
        cardStyle.padding = TaffyRect.all(LengthPercentage.length(6f));
        NodeId card = tree.newWithChildren(cardStyle, new NodeId[]{label});

        TaffyStyle rootStyle = grid();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(320f), TaffyDimension.length(200f));
        rootStyle.gridTemplateColumns = List.of(TrackSizingFunction.fr(1f));
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{card});

        tree.computeLayout(root, layoutSize(320f, 200f));

        assertEquals(320f, tree.getLayout(card).size().width, EPSILON);
        // The card's content box is 320 - 2 * 6 = 308, so a 100%-wide label fills it.
        assertEquals(308f, tree.getLayout(label).size().width, EPSILON);
        assertEquals(10f, tree.getLayout(label).size().height, EPSILON);
        assertEquals(6f, tree.getLayout(label).location().x, EPSILON);
    }

    /**
     * The same property through a percentage padding: a flex container used as a flex item is
     * first measured with an indefinite inner width (its flex base size) and then laid out at its
     * final width, where the percentage padding must resolve to 10% of the final content box.
     */
    @Test
    void percentageChildPaddingResolvesAgainstTheFinalFlexItemWidth() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.boxSizing = BoxSizing.CONTENT_BOX;
        childStyle.size = new TaffySize<>(TaffyDimension.length(50f), TaffyDimension.AUTO);
        childStyle.padding = TaffyRect.all(LengthPercentage.percent(0.1f));
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle cardStyle = flexColumn();
        cardStyle.flexGrow = 1f;
        cardStyle.padding = TaffyRect.all(LengthPercentage.length(1f));
        NodeId card = tree.newWithChildren(cardStyle, new NodeId[]{child});

        TaffyStyle rootStyle = flexRow();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(200f));
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{card});

        tree.computeLayout(root, layoutSize(300f, 200f));

        assertEquals(300f, tree.getLayout(card).size().width, EPSILON);
        // The card's content box is 298 wide; 10% padding is 29.8 on every side.
        assertEquals(29.8f, tree.getLayout(child).padding().left, EPSILON);
        assertEquals(59.6f, tree.getLayout(child).size().height, EPSILON);
        assertEquals(109.6f, tree.getLayout(child).size().width, EPSILON);
    }

    /**
     * A percentage value also has to be re-resolved when the same container is laid out in a later
     * pass, so a pass-scoped entry can never leak into a pass with a different basis.
     */
    @Test
    void percentageChildResolvesAgainInALaterPass() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.percent(0.5f), TaffyDimension.length(10f));
        NodeId child = tree.newLeaf(childStyle);
        TaffyStyle cardStyle = flexColumn();
        cardStyle.flexGrow = 1f;
        NodeId card = tree.newWithChildren(cardStyle, new NodeId[]{child});
        TaffyStyle rootStyle = flexRow();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(200f));
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{card});

        tree.computeLayout(root, layoutSize(300f, 200f));
        assertEquals(150f, tree.getLayout(child).size().width, EPSILON);

        rootStyle.size = new TaffySize<>(TaffyDimension.length(500f), TaffyDimension.length(200f));
        tree.setStyle(root, rootStyle);
        tree.computeLayout(root, layoutSize(500f, 200f));
        assertEquals(250f, tree.getLayout(child).size().width, EPSILON);
    }

    // =============================================================================================
    // Cross-pass reuse
    // =============================================================================================

    /** Two passes with unchanged inputs reuse the stored list outright. */
    @Test
    void unchangedPassesReuseTheStoredItemList() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        NodeId first = tree.newLeaf(leaf(40f, 10f));
        NodeId second = tree.newLeaf(leaf(40f, 10f));
        NodeId root = tree.newWithChildren(sized(flexRow(), 200f, 100f), new NodeId[]{first, second});

        tree.computeLayout(root, layoutSize(200f, 100f));
        FlexboxComputer.FlexItemCache firstCache = tree.getNodeData(root).getFlexItemCache();
        assertNotNull(firstCache);

        tree.invalidateLayoutReuse();
        tree.computeLayout(root, layoutSize(200f, 100f));

        assertSame(firstCache, tree.getNodeData(root).getFlexItemCache());
        assertEquals(40f, tree.getLayout(first).size().width, EPSILON);
        assertEquals(40f, tree.getLayout(second).location().x, EPSILON);
    }

    /**
     * A direct child's style change rebuilds only its own parent's list. The grandparent sees the
     * changed child through its parent's content version, which did not change.
     */
    @Test
    void childStyleChangeRebuildsOnlyItsDirectParent() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        NodeId a1 = tree.newLeaf(leaf(20f, 10f));
        NodeId a2 = tree.newLeaf(leaf(20f, 10f));
        NodeId panelA = tree.newWithChildren(flexRow(), new NodeId[]{a1, a2});
        NodeId b1 = tree.newLeaf(leaf(20f, 10f));
        NodeId b2 = tree.newLeaf(leaf(20f, 10f));
        NodeId panelB = tree.newWithChildren(flexRow(), new NodeId[]{b1, b2});
        NodeId root = tree.newWithChildren(sized(flexRow(), 300f, 100f), new NodeId[]{panelA, panelB});

        tree.computeLayout(root, layoutSize(300f, 100f));
        FlexboxComputer.FlexItemCache rootCache = tree.getNodeData(root).getFlexItemCache();
        FlexboxComputer.FlexItemCache panelACache = tree.getNodeData(panelA).getFlexItemCache();
        FlexboxComputer.FlexItemCache panelBCache = tree.getNodeData(panelB).getFlexItemCache();
        assertNotNull(rootCache);
        assertNotNull(panelACache);
        assertNotNull(panelBCache);

        tree.setStyle(a1, leaf(50f, 10f));
        tree.computeLayout(root, layoutSize(300f, 100f));

        assertSame(rootCache, tree.getNodeData(root).getFlexItemCache());
        assertSame(panelBCache, tree.getNodeData(panelB).getFlexItemCache());
        assertNotSame(panelACache, tree.getNodeData(panelA).getFlexItemCache());
        assertEquals(50f, tree.getLayout(a1).size().width, EPSILON);
        assertEquals(50f, tree.getLayout(a2).location().x, EPSILON);
    }

    /** Adding and removing a child rebuilds only that container, not its ancestors or siblings. */
    @Test
    void childListChangeRebuildsOnlyTheChangedContainer() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        NodeId a1 = tree.newLeaf(leaf(20f, 10f));
        NodeId a2 = tree.newLeaf(leaf(20f, 10f));
        NodeId panelA = tree.newWithChildren(flexRow(), new NodeId[]{a1, a2});
        NodeId b1 = tree.newLeaf(leaf(20f, 10f));
        NodeId b2 = tree.newLeaf(leaf(20f, 10f));
        NodeId panelB = tree.newWithChildren(flexRow(), new NodeId[]{b1, b2});
        NodeId root = tree.newWithChildren(sized(flexRow(), 300f, 100f), new NodeId[]{panelA, panelB});

        tree.computeLayout(root, layoutSize(300f, 100f));
        FlexboxComputer.FlexItemCache rootCache = tree.getNodeData(root).getFlexItemCache();
        FlexboxComputer.FlexItemCache panelACache = tree.getNodeData(panelA).getFlexItemCache();
        FlexboxComputer.FlexItemCache panelBCache = tree.getNodeData(panelB).getFlexItemCache();
        assertNotNull(rootCache);

        NodeId a3 = tree.newLeaf(leaf(20f, 10f));
        tree.addChild(panelA, a3);
        tree.computeLayout(root, layoutSize(300f, 100f));

        assertSame(rootCache, tree.getNodeData(root).getFlexItemCache());
        assertSame(panelBCache, tree.getNodeData(panelB).getFlexItemCache());
        assertNotSame(panelACache, tree.getNodeData(panelA).getFlexItemCache());
        assertEquals(40f, tree.getLayout(a3).location().x, EPSILON);

        FlexboxComputer.FlexItemCache grownPanelCache = tree.getNodeData(panelA).getFlexItemCache();
        tree.removeChild(panelA, a3);
        tree.computeLayout(root, layoutSize(300f, 100f));

        assertSame(rootCache, tree.getNodeData(root).getFlexItemCache());
        assertSame(panelBCache, tree.getNodeData(panelB).getFlexItemCache());
        assertNotSame(grownPanelCache, tree.getNodeData(panelA).getFlexItemCache());
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
        NodeId panelA = tree.newWithChildren(flexRow(), new NodeId[]{measured, sibling});
        NodeId b1 = tree.newLeaf(leaf(20f, 10f));
        NodeId b2 = tree.newLeaf(leaf(20f, 10f));
        NodeId panelB = tree.newWithChildren(flexRow(), new NodeId[]{b1, b2});
        NodeId root = tree.newWithChildren(sized(flexRow(), 300f, 100f), new NodeId[]{panelA, panelB});

        tree.computeLayout(root, layoutSize(300f, 100f));
        FlexboxComputer.FlexItemCache rootCache = tree.getNodeData(root).getFlexItemCache();
        FlexboxComputer.FlexItemCache panelACache = tree.getNodeData(panelA).getFlexItemCache();
        FlexboxComputer.FlexItemCache panelBCache = tree.getNodeData(panelB).getFlexItemCache();
        assertNotNull(panelACache);
        assertEquals(10f, tree.getLayout(measured).size().width, EPSILON);

        tree.setMeasureFunction(measured, context -> LayoutOutput.fromOuterSize(new FloatSize(40f, 10f)));
        tree.computeLayout(root, layoutSize(300f, 100f));

        assertSame(rootCache, tree.getNodeData(root).getFlexItemCache());
        assertSame(panelBCache, tree.getNodeData(panelB).getFlexItemCache());
        assertNotSame(panelACache, tree.getNodeData(panelA).getFlexItemCache());
        assertEquals(40f, tree.getLayout(measured).size().width, EPSILON);

        FlexboxComputer.FlexItemCache grownPanelCache = tree.getNodeData(panelA).getFlexItemCache();
        tree.setMeasureFunc(measured, (knownDimensions, availableSpace) -> new FloatSize(20f, 10f));
        tree.computeLayout(root, layoutSize(300f, 100f));

        assertSame(rootCache, tree.getNodeData(root).getFlexItemCache());
        assertNotSame(grownPanelCache, tree.getNodeData(panelA).getFlexItemCache());
        assertEquals(20f, tree.getLayout(measured).size().width, EPSILON);

        // A new context changes what the callback returns even though no style changed.
        FlexboxComputer.FlexItemCache contextPanelCache = tree.getNodeData(panelA).getFlexItemCache();
        tree.setMeasureFunction(measured, context ->
            LayoutOutput.fromOuterSize(new FloatSize(context.nodeContext() == null ? 20f : 30f, 10f)));
        tree.setNodeContext(measured, new Object());
        tree.computeLayout(root, layoutSize(300f, 100f));

        assertNotSame(contextPanelCache, tree.getNodeData(panelA).getFlexItemCache());
        assertEquals(30f, tree.getLayout(measured).size().width, EPSILON);
    }

    /**
     * A direction inherited from an ancestor changes the container's resolved direction and the
     * child's resolved direction. Neither is carried by {@code LayoutInput}, so only the direction
     * comparison in the cache can reject the old list; the item's {@code self-start} alignment
     * proves the rebuilt list is used.
     */
    @Test
    void inheritedDirectionChangeInvalidatesTheItemList() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle childStyle = leaf(10f, 10f);
        childStyle.alignSelf = AlignItems.SELF_START;
        NodeId child = tree.newLeaf(childStyle);
        NodeId extra = tree.newLeaf(leaf(20f, 10f));
        TaffyStyle panelStyle = flexColumn();
        panelStyle.flexGrow = 1f;
        NodeId panel = tree.newWithChildren(panelStyle, new NodeId[]{child, extra});
        TaffyStyle rootStyle = sized(flexRow(), 300f, 100f);
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{panel});

        tree.computeLayout(root, layoutSize(300f, 100f));
        FlexboxComputer.FlexItemCache ltrPanelCache = tree.getNodeData(panel).getFlexItemCache();
        assertNotNull(ltrPanelCache);
        assertEquals(0f, tree.getLayout(child).location().x, EPSILON);

        rootStyle.direction = TaffyDirection.RTL;
        tree.setStyle(root, rootStyle);
        // The layout cache does not carry direction (inherited, and not part of LayoutInput), so a
        // child whose own inputs did not change keeps its cached layout. Clearing just this
        // container's layout makes the pass recompute it under the new inherited direction.
        tree.clearCache(panel);
        tree.computeLayout(root, layoutSize(300f, 100f));

        assertNotSame(ltrPanelCache, tree.getNodeData(panel).getFlexItemCache());
        assertEquals(290f, tree.getLayout(child).location().x, EPSILON);

        // A real pass with no input change reuses the RTL list and keeps the same geometry.
        FlexboxComputer.FlexItemCache rtlPanelCache = tree.getNodeData(panel).getFlexItemCache();
        tree.invalidateLayoutReuse();
        tree.computeLayout(root, layoutSize(300f, 100f));
        assertSame(rtlPanelCache, tree.getNodeData(panel).getFlexItemCache());
        assertEquals(290f, tree.getLayout(child).location().x, EPSILON);
    }

    /** Percentage padding is resolved against the inner size; a new basis rebuilds the list. */
    @Test
    void percentageDependentListRevalidatesWhenTheInnerSizeChanges() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.boxSizing = BoxSizing.CONTENT_BOX;
        childStyle.size = new TaffySize<>(TaffyDimension.length(50f), TaffyDimension.AUTO);
        childStyle.padding = TaffyRect.all(LengthPercentage.percent(0.1f));
        NodeId child = tree.newLeaf(childStyle);
        NodeId extra = tree.newLeaf(leaf(10f, 10f));
        TaffyStyle panelStyle = flexColumn();
        panelStyle.flexGrow = 1f;
        NodeId panel = tree.newWithChildren(panelStyle, new NodeId[]{child, extra});
        TaffyStyle rootStyle = sized(flexRow(), 200f, 200f);
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{panel});

        tree.computeLayout(root, layoutSize(200f, 200f));
        FlexboxComputer.FlexItemCache narrowCache = tree.getNodeData(panel).getFlexItemCache();
        assertNotNull(narrowCache);
        assertEquals(20f, tree.getLayout(child).padding().left, EPSILON);
        assertEquals(40f, tree.getLayout(child).size().height, EPSILON);

        rootStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(200f));
        tree.setStyle(root, rootStyle);
        tree.computeLayout(root, layoutSize(300f, 200f));

        assertNotSame(narrowCache, tree.getNodeData(panel).getFlexItemCache());
        assertEquals(30f, tree.getLayout(child).padding().left, EPSILON);
        assertEquals(60f, tree.getLayout(child).size().height, EPSILON);
    }

    /**
     * A list whose child styles resolve to the same values at every basis is reused even when the
     * container's inner size changes between passes, and the auto margins it assigns are reset.
     */
    @Test
    void innerSizeIndependentListSurvivesAnAvailableSpaceChange() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        NodeId first = tree.newLeaf(leaf(50f, 10f));
        TaffyStyle pushedStyle = leaf(30f, 10f);
        pushedStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.auto(),
            LengthPercentageAuto.length(0f),
            LengthPercentageAuto.length(0f),
            LengthPercentageAuto.length(0f)
        );
        NodeId pushed = tree.newLeaf(pushedStyle);
        TaffyStyle panelStyle = flexRow();
        panelStyle.flexGrow = 1f;
        NodeId panel = tree.newWithChildren(panelStyle, new NodeId[]{first, pushed});
        TaffyStyle rootStyle = sized(flexRow(), 200f, 100f);
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{panel});

        tree.computeLayout(root, layoutSize(200f, 100f));
        FlexboxComputer.FlexItemCache cache = tree.getNodeData(panel).getFlexItemCache();
        assertNotNull(cache);
        assertEquals(170f, tree.getLayout(pushed).location().x, EPSILON);

        rootStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(100f));
        tree.setStyle(root, rootStyle);
        tree.computeLayout(root, layoutSize(300f, 100f));

        assertSame(cache, tree.getNodeData(panel).getFlexItemCache());
        assertEquals(270f, tree.getLayout(pushed).location().x, EPSILON);
    }

    /**
     * A reused ancestor item list must produce exactly the layouts a tree built from scratch
     * produces: every {@link Layout} field of every node is compared, not just the root size. The
     * tree exercises grow, shrink, wrap, gaps, percentages, auto margins, out-of-flow children and
     * a measure callback.
     */
    @Test
    void reusedItemListMatchesFreshlyBuiltTreeFieldByField() {
        TaffyTree cachedTree = new TaffyTree();
        cachedTree.disableRounding();
        RichForm cachedForm = buildRichForm(cachedTree, false);
        NodeId cachedRoot = cachedForm.root();
        cachedTree.computeLayout(cachedRoot, layoutSize(400f, 300f));
        FlexboxComputer.FlexItemCache rootCache = cachedTree.getNodeData(cachedRoot).getFlexItemCache();
        assertNotNull(rootCache);

        cachedTree.setStyle(cachedForm.changedLeaf(), percentLeaf(0.25f));
        cachedTree.computeLayout(cachedRoot, layoutSize(400f, 300f));
        assertSame(rootCache, cachedTree.getNodeData(cachedRoot).getFlexItemCache());

        TaffyTree freshTree = new TaffyTree();
        freshTree.disableRounding();
        RichForm freshForm = buildRichForm(freshTree, true);
        freshTree.computeLayout(freshForm.root(), layoutSize(400f, 300f));

        assertEquals(cachedForm.nodes().size(), freshForm.nodes().size());
        for (int i = 0; i < cachedForm.nodes().size(); i++) {
            Layout expected = freshTree.getLayout(freshForm.nodes().get(i));
            Layout actual = cachedTree.getLayout(cachedForm.nodes().get(i));
            assertNotNull(expected);
            assertNotNull(actual);
            assertLayoutEquals(expected, actual, "node " + i);
        }
    }

    private record RichForm(java.util.List<NodeId> nodes, NodeId root, NodeId changedLeaf) {}

    /**
     * Builds the same rich form in two trees. {@code changed} makes the changed leaf start out in
     * its final style in the fresh tree, so both trees describe the same final inputs.
     */
    private static RichForm buildRichForm(TaffyTree tree, boolean changed) {
        java.util.List<NodeId> nodes = new java.util.ArrayList<>();

        NodeId percent = tree.newLeaf(percentLeaf(changed ? 0.25f : 0.5f));
        nodes.add(percent);
        NodeId shrink = tree.newLeaf(leaf(1000f, 10f));
        nodes.add(shrink);
        NodeId measured = tree.newLeafWithMeasure(
            new TaffyStyle(), (knownDimensions, availableSpace) -> new FloatSize(30f, 20f));
        nodes.add(measured);
        TaffyStyle autoMarginStyle = leaf(20f, 10f);
        autoMarginStyle.margin = TaffyRect.ltrb(
            LengthPercentageAuto.auto(),
            LengthPercentageAuto.length(0f),
            LengthPercentageAuto.length(0f),
            LengthPercentageAuto.length(0f)
        );
        NodeId autoMargin = tree.newLeaf(autoMarginStyle);
        nodes.add(autoMargin);
        TaffyStyle absoluteStyle = new TaffyStyle();
        absoluteStyle.position = TaffyPosition.ABSOLUTE;
        absoluteStyle.size = new TaffySize<>(TaffyDimension.length(9f), TaffyDimension.length(8f));
        absoluteStyle.inset = TaffyRect.ltrb(
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.length(4f),
            LengthPercentageAuto.length(4f),
            LengthPercentageAuto.AUTO
        );
        NodeId absolute = tree.newLeaf(absoluteStyle);
        nodes.add(absolute);
        TaffyStyle panelStyle = flexColumn();
        panelStyle.flexGrow = 1f;
        panelStyle.flexShrink = 1f;
        panelStyle.padding = TaffyRect.all(LengthPercentage.length(3f));
        panelStyle.gap = new TaffySize<>(LengthPercentage.length(2f), LengthPercentage.length(2f));
        NodeId panelA = tree.newWithChildren(panelStyle,
            new NodeId[]{percent, shrink, measured, autoMargin, absolute});
        nodes.add(panelA);

        TaffyStyle wrapStyle = flexRow();
        wrapStyle.flexWrap = FlexWrap.WRAP;
        wrapStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.AUTO);
        wrapStyle.gap = new TaffySize<>(LengthPercentage.length(4f), LengthPercentage.length(4f));
        NodeId wrapA = tree.newLeaf(leaf(40f, 10f));
        nodes.add(wrapA);
        NodeId wrapB = tree.newLeaf(leaf(40f, 10f));
        nodes.add(wrapB);
        NodeId wrapC = tree.newLeaf(leaf(40f, 10f));
        nodes.add(wrapC);
        NodeId panelB = tree.newWithChildren(wrapStyle, new NodeId[]{wrapA, wrapB, wrapC});
        nodes.add(panelB);

        NodeId footer = tree.newLeaf(leaf(60f, 9f));
        nodes.add(footer);

        TaffyStyle rootStyle = flexRow();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(400f), TaffyDimension.length(300f));
        rootStyle.padding = TaffyRect.all(LengthPercentage.length(2f));
        rootStyle.gap = new TaffySize<>(LengthPercentage.length(6f), LengthPercentage.length(6f));
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{panelA, panelB, footer});
        nodes.add(root);
        return new RichForm(nodes, root, percent);
    }

    private static TaffyStyle percentLeaf(float percent) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.percent(percent), TaffyDimension.length(10f));
        return style;
    }

    private static TaffyStyle sized(TaffyStyle style, float width, float height) {
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
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
