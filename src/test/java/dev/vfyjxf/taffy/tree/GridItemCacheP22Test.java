package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.GridAutoFlow;
import dev.vfyjxf.taffy.style.GridPlacement;
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
 * P22 tests for the pass-scoped grid item-list cache in {@link GridComputer}.
 *
 * <p>{@code generateGridItems} used to rebuild every grid container's item metadata on every
 * compute call. One layout pass lays a grid container out several times while it sizes its tracks
 * (the 1x1 chain of the {@code superDeepGrid} benchmark generates its items up to ~45 times), so
 * the pass now caches the list on a thread-local keyed by the tree and its direction epoch and
 * reuses it while the inner size stays the same - or, when neither a child value nor the parsed
 * placement resolves against the inner size, at every inner size. Only style-derived metadata is
 * kept; the per-pass results (auto-positioned start lines, baselines, contribution caches, final
 * sizes/locations/overflow) are reset before a reused list is handed out.</p>
 *
 * <p>The tests pin the observable contract: a reused list must produce exactly the layouts a
 * freshly built tree produces, percentage values must resolve against the inner size of the pass
 * that uses them, and the modal results of one use (auto placement, baseline shims, contribution
 * caches) must not leak into the next.</p>
 */
@DisplayName("P22: grid item-list reuse")
public class GridItemCacheP22Test {

    private static final float EPSILON = 0.001f;

    private static TaffySize<AvailableSpace> layoutSize(float width, float height) {
        return TaffySize.of(AvailableSpace.definite(width), AvailableSpace.definite(height));
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

    private static TaffyStyle percentLeaf(float percent, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.percent(percent), TaffyDimension.length(height));
        return style;
    }

    // --------------------------------------------------------------- rich fixture

    private record GridForm(List<NodeId> nodes, NodeId root, NodeId changedLeaf) {}

    /**
     * A grid form with an {@code fr} track, an {@code auto} track, a {@code minmax(...)} track,
     * a percentage track, an implicit row, spanning and dense auto-placement, an out-of-flow
     * child, a hidden child, percentage padding/size, and a measured leaf. {@code changed} builds
     * the changed leaf directly in its final style so a cache-reusing tree and a fresh tree
     * describe the same inputs.
     */
    private static GridForm buildGridForm(TaffyTree tree, boolean changed) {
        List<NodeId> nodes = new java.util.ArrayList<>();

        NodeId measured = tree.newLeafWithMeasure(
            new TaffyStyle(), (knownDimensions, availableSpace) -> new FloatSize(30f, 20f));
        nodes.add(measured);

        TaffyStyle percentStyle = percentLeaf(changed ? 0.25f : 0.5f, 10f);
        NodeId percent = tree.newLeaf(percentStyle);
        nodes.add(percent);

        TaffyStyle spanStyle = leaf(40f, 12f);
        spanStyle.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.span(2));
        NodeId span = tree.newLeaf(spanStyle);
        nodes.add(span);

        TaffyStyle explicitStyle = leaf(25f, 9f);
        explicitStyle.gridRow = new TaffyLine<>(GridPlacement.line(2), GridPlacement.line(3));
        explicitStyle.gridColumn = new TaffyLine<>(GridPlacement.line(1), GridPlacement.line(2));
        NodeId explicit = tree.newLeaf(explicitStyle);
        nodes.add(explicit);

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

        TaffyStyle innerGridStyle = grid();
        innerGridStyle.gridTemplateColumns = List.of(
            TrackSizingFunction.auto(), TrackSizingFunction.fr(1f));
        NodeId innerFirst = tree.newLeaf(leaf(11f, 7f));
        nodes.add(innerFirst);
        NodeId innerSecond = tree.newLeaf(percentLeaf(0.5f, 6f));
        nodes.add(innerSecond);
        NodeId innerGrid = tree.newWithChildren(innerGridStyle, new NodeId[]{innerFirst, innerSecond});
        nodes.add(innerGrid);

        TaffyStyle rootStyle = grid();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(400f), TaffyDimension.length(300f));
        rootStyle.gridTemplateColumns = List.of(
            TrackSizingFunction.fr(1f),
            TrackSizingFunction.auto(),
            TrackSizingFunction.minmax(TrackSizingFunction.fixed(40f), TrackSizingFunction.fr(1f)),
            TrackSizingFunction.percent(0.2f)
        );
        rootStyle.gridTemplateRows = List.of(TrackSizingFunction.auto(), TrackSizingFunction.fixed(30f));
        rootStyle.gridAutoFlow = GridAutoFlow.ROW_DENSE;
        rootStyle.gap = new TaffySize<>(LengthPercentage.length(4f), LengthPercentage.length(4f));
        rootStyle.padding = TaffyRect.all(LengthPercentage.length(3f));
        NodeId root = tree.newWithChildren(rootStyle,
            new NodeId[]{measured, percent, span, explicit, absolute, innerGrid});
        nodes.add(root);

        return new GridForm(nodes, root, percent);
    }

    // --------------------------------------------------------------------- tests

    /**
     * A reused item list must produce exactly the layouts a tree built from scratch produces:
     * every {@link Layout} field of every node is compared, not just the root size. The tree is
     * laid out once, resized to a different available space (so the grid container is generated
     * again in a later pass) and compared with a fresh tree laid out at the same final size.
     */
    @Test
    void reusedItemListMatchesFreshlyBuiltTreeFieldByField() {
        TaffyTree cachedTree = new TaffyTree();
        cachedTree.disableRounding();
        GridForm cachedForm = buildGridForm(cachedTree, false);
        cachedTree.computeLayout(cachedForm.root(), layoutSize(400f, 300f));
        cachedTree.computeLayout(cachedForm.root(), layoutSize(360f, 280f));

        TaffyTree freshTree = new TaffyTree();
        freshTree.disableRounding();
        GridForm freshForm = buildGridForm(freshTree, false);
        freshTree.computeLayout(freshForm.root(), layoutSize(360f, 280f));

        assertEquals(cachedForm.nodes().size(), freshForm.nodes().size());
        for (int i = 0; i < cachedForm.nodes().size(); i++) {
            Layout expected = freshTree.getLayout(freshForm.nodes().get(i));
            Layout actual = cachedTree.getLayout(cachedForm.nodes().get(i));
            assertNotNull(expected, "fresh layout " + i);
            assertNotNull(actual, "cached layout " + i);
            assertLayoutEquals(expected, actual, "node " + i);
        }
    }

    /**
     * A grid item whose child uses percentage size and padding is measured at several inner sizes
     * during track sizing. The final layout must resolve those percentages against the grid area
     * of the final track, not against an earlier measurement basis.
     */
    @Test
    void percentageItemValuesResolveAgainstTheFinalGridArea() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle labelStyle = percentLeaf(0.5f, 10f);
        labelStyle.padding = TaffyRect.all(LengthPercentage.percent(0.1f));
        NodeId label = tree.newLeaf(labelStyle);

        TaffyStyle rootStyle = grid();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(320f), TaffyDimension.length(200f));
        rootStyle.gridTemplateColumns = List.of(TrackSizingFunction.fr(1f));
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{label});

        tree.computeLayout(root, layoutSize(320f, 200f));

        Layout labelLayout = tree.getLayout(label);
        // The grid area is the 320px content box (no padding on the container).
        assertEquals(160f, labelLayout.size().width, EPSILON);
        // 10% of the grid area's inline size (320) on each side.
        assertEquals(32f, labelLayout.padding().left, EPSILON);
        assertEquals(32f, labelLayout.padding().right, EPSILON);
    }

    /**
     * Auto placement writes start lines into the items. A reused list must hand those back as
     * "auto" so the dense auto-flow placement of the next pass sees the same input as a fresh
     * item list. The second pass uses a different available width, so the tracks are sized again
     * and every item is positioned again.
     */
    @Test
    void autoPlacementDoesNotLeakBetweenPasses() {
        TaffyTree cachedTree = new TaffyTree();
        cachedTree.disableRounding();
        GridForm cachedForm = buildGridForm(cachedTree, false);
        cachedTree.computeLayout(cachedForm.root(), layoutSize(400f, 300f));
        cachedTree.computeLayout(cachedForm.root(), layoutSize(220f, 140f));

        TaffyTree freshTree = new TaffyTree();
        freshTree.disableRounding();
        GridForm freshForm = buildGridForm(freshTree, false);
        freshTree.computeLayout(freshForm.root(), layoutSize(220f, 140f));

        for (int i = 0; i < cachedForm.nodes().size(); i++) {
            assertLayoutEquals(freshTree.getLayout(freshForm.nodes().get(i)),
                cachedTree.getLayout(cachedForm.nodes().get(i)), "node " + i);
        }
    }

    /**
     * A child style change must be observed on the next pass: the item metadata (and the
     * percentage it resolves) is rebuilt instead of answering from a reused list.
     */
    @Test
    void childStyleChangeIsObserved() {
        TaffyTree cachedTree = new TaffyTree();
        cachedTree.disableRounding();
        GridForm cachedForm = buildGridForm(cachedTree, false);
        cachedTree.computeLayout(cachedForm.root(), layoutSize(400f, 300f));

        cachedTree.setStyle(cachedForm.changedLeaf(), percentLeaf(0.25f, 10f));
        cachedTree.computeLayout(cachedForm.root(), layoutSize(400f, 300f));

        TaffyTree freshTree = new TaffyTree();
        freshTree.disableRounding();
        GridForm freshForm = buildGridForm(freshTree, true);
        freshTree.computeLayout(freshForm.root(), layoutSize(400f, 300f));

        for (int i = 0; i < cachedForm.nodes().size(); i++) {
            assertLayoutEquals(freshTree.getLayout(freshForm.nodes().get(i)),
                cachedTree.getLayout(cachedForm.nodes().get(i)), "node " + i);
        }
        assertEquals(freshTree.getLayout(freshForm.changedLeaf()).size().width,
            cachedTree.getLayout(cachedForm.changedLeaf()).size().width, EPSILON);
    }

    /**
     * Adding and removing a child changes the child list the item positions were built from; a
     * reused list must not survive either mutation.
     */
    @Test
    void addAndRemoveChildAreObserved() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        GridForm form = buildGridForm(tree, false);
        tree.computeLayout(form.root(), layoutSize(400f, 300f));

        NodeId added = tree.newLeaf(leaf(33f, 17f));
        tree.addChild(form.root(), added);
        tree.computeLayout(form.root(), layoutSize(400f, 300f));

        TaffyTree freshTree = new TaffyTree();
        freshTree.disableRounding();
        GridForm freshForm = buildGridForm(freshTree, false);
        NodeId freshAdded = freshTree.newLeaf(leaf(33f, 17f));
        freshTree.addChild(freshForm.root(), freshAdded);
        freshTree.computeLayout(freshForm.root(), layoutSize(400f, 300f));

        assertEquals(freshTree.getLayout(freshAdded).location(),
            tree.getLayout(added).location(), "added child location");
        assertEquals(freshTree.getLayout(freshAdded).size(),
            tree.getLayout(added).size(), "added child size");

        tree.remove(added);
        tree.computeLayout(form.root(), layoutSize(400f, 300f));
        freshTree.computeLayout(freshForm.root(), layoutSize(400f, 300f));
        for (int i = 0; i < form.nodes().size(); i++) {
            assertLayoutEquals(freshTree.getLayout(freshForm.nodes().get(i)),
                tree.getLayout(form.nodes().get(i)), "node " + i);
        }
    }

    /**
     * A measured leaf inside a grid item is measured during track sizing and laid out again in
     * the final pass; the reused item list must not pin the measurement's known dimensions.
     */
    @Test
    void measuredItemIsRemeasuredAtTheFinalSize() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        NodeId measured = tree.newLeafWithMeasure(new TaffyStyle(),
            (knownDimensions, availableSpace) -> new FloatSize(
                Float.isNaN(knownDimensions.width) ? 40f : knownDimensions.width,
                Float.isNaN(knownDimensions.height) ? 10f : knownDimensions.height));

        TaffyStyle rootStyle = grid();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(100f));
        rootStyle.gridTemplateColumns = List.of(TrackSizingFunction.fr(1f));
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{measured});

        tree.computeLayout(root, layoutSize(200f, 100f));

        assertEquals(200f, tree.getLayout(measured).size().width, EPSILON);
        assertEquals(100f, tree.getLayout(measured).size().height, EPSILON);
    }

    // ------------------------------------------------------------- cross-pass cache

    /**
     * A later pass reuses the list stored on the container's {@link NodeData} while its content
     * version, child versions, direction and inner-size basis are unchanged. The auto-sized root
     * is laid out into two different available spaces, so the second pass really runs the grid
     * algorithm instead of the tree-clean fast path.
     */
    @Test
    void itemCacheIsReusedWhenNothingChanged() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId first = tree.newLeaf(leaf(20f, 10f));
        NodeId second = tree.newLeaf(leaf(30f, 10f));
        TaffyStyle rootStyle = grid();
        rootStyle.gridTemplateColumns = List.of(TrackSizingFunction.auto(), TrackSizingFunction.auto());
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{first, second});

        tree.computeLayout(root, layoutSize(400f, 300f));
        GridComputer.GridItemCache firstCache = tree.getNodeData(root).getGridItemCache();
        assertNotNull(firstCache);

        tree.computeLayout(root, layoutSize(200f, 150f));
        assertSame(firstCache, tree.getNodeData(root).getGridItemCache());
        assertEquals(20f, tree.getLayout(first).size().width, EPSILON);
        assertEquals(20f, tree.getLayout(second).location().x, EPSILON);
    }

    /**
     * When a child value resolves against the container's inner size (here a percentage width),
     * a pass with a different inner size must rebuild the list instead of reusing stale resolved
     * metadata, and the percentage must resolve against the new basis.
     */
    @Test
    void percentageMetadataInvalidatesOnInnerSizeChange() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId child = tree.newLeaf(percentLeaf(0.5f, 10f));
        TaffyStyle rootStyle = grid();
        rootStyle.size = new TaffySize<>(TaffyDimension.percent(0.5f), TaffyDimension.length(100f));
        rootStyle.gridTemplateColumns = List.of(TrackSizingFunction.fr(1f));
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{child});

        tree.computeLayout(root, layoutSize(400f, 100f));
        GridComputer.GridItemCache firstCache = tree.getNodeData(root).getGridItemCache();
        assertNotNull(firstCache);
        assertEquals(100f, tree.getLayout(child).size().width, EPSILON);

        tree.computeLayout(root, layoutSize(200f, 100f));
        assertNotSame(firstCache, tree.getNodeData(root).getGridItemCache());
        // Root width 200 * 50% = 100; the 1fr track is 100 wide, so the 50% child is 50 wide.
        assertEquals(50f, tree.getLayout(child).size().width, EPSILON);
    }

    /**
     * An out-of-flow child is not part of the in-flow item list and only shares its index with it,
     * so changing its style must not rebuild the grid's list (an animated badge must not invalidate
     * the whole grid).
     */
    @Test
    void outOfFlowChildStyleChangeKeepsTheCache() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId inFlow = tree.newLeaf(leaf(20f, 10f));
        TaffyStyle badgeStyle = leaf(5f, 5f);
        badgeStyle.position = TaffyPosition.ABSOLUTE;
        NodeId badge = tree.newLeaf(badgeStyle);
        TaffyStyle rootStyle = grid();
        rootStyle.gridTemplateColumns = List.of(TrackSizingFunction.auto());
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{inFlow, badge});

        tree.computeLayout(root, layoutSize(200f, 100f));
        GridComputer.GridItemCache firstCache = tree.getNodeData(root).getGridItemCache();
        assertNotNull(firstCache);

        TaffyStyle biggerBadge = leaf(15f, 6f);
        biggerBadge.position = TaffyPosition.ABSOLUTE;
        tree.setStyle(badge, biggerBadge);
        tree.computeLayout(root, layoutSize(200f, 100f));
        assertSame(firstCache, tree.getNodeData(root).getGridItemCache());
        assertEquals(20f, tree.getLayout(inFlow).size().width, EPSILON);
        assertEquals(15f, tree.getLayout(badge).size().width, EPSILON);
    }

    /**
     * A direct child's style change, a child-list change and a direction change all invalidate the
     * cross-pass list; a plain {@code clearCache} (layout-cache invalidation) does not, matching
     * the flex item-list cache.
     */
    @Test
    void cacheInvalidationMatchesTheItemListContract() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId first = tree.newLeaf(leaf(20f, 10f));
        NodeId second = tree.newLeaf(leaf(30f, 10f));
        TaffyStyle rootStyle = grid();
        rootStyle.gridTemplateColumns = List.of(TrackSizingFunction.auto(), TrackSizingFunction.auto());
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{first, second});
        tree.computeLayout(root, layoutSize(400f, 300f));
        GridComputer.GridItemCache cache = tree.getNodeData(root).getGridItemCache();
        assertNotNull(cache);

        tree.clearCache(root);
        tree.computeLayout(root, layoutSize(400f, 300f));
        assertSame(cache, tree.getNodeData(root).getGridItemCache());

        tree.setStyle(first, leaf(25f, 10f));
        tree.computeLayout(root, layoutSize(400f, 300f));
        GridComputer.GridItemCache afterStyle = tree.getNodeData(root).getGridItemCache();
        assertNotSame(cache, afterStyle);
        assertEquals(25f, tree.getLayout(first).size().width, EPSILON);

        NodeId added = tree.newLeaf(leaf(7f, 7f));
        tree.addChild(root, added);
        tree.computeLayout(root, layoutSize(400f, 300f));
        assertNotSame(afterStyle, tree.getNodeData(root).getGridItemCache());

        GridComputer.GridItemCache afterChildList = tree.getNodeData(root).getGridItemCache();
        TaffyStyle rtl = tree.getStyle(root);
        rtl.direction = TaffyDirection.RTL;
        tree.setStyle(root, rtl);
        tree.computeLayout(root, layoutSize(400f, 300f));
        assertNotSame(afterChildList, tree.getNodeData(root).getGridItemCache());
    }

    // ------------------------------------------------------------------- helpers

    private static void assertLayoutEquals(Layout expected, Layout actual, String label) {
        assertNotNull(expected, label + " expected");
        assertNotNull(actual, label + " actual");
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
