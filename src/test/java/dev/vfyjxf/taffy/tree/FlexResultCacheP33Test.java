package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyFloat;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P33: flex container result cache.
 *
 * <p>A flex container with unchanged inputs and unchanged direct children answers a repeated
 * {@link RunMode#PERFORM_LAYOUT} request from a cached {@link LayoutOutput} instead of re-running
 * the item sizing stages. The cache is version-keyed (container/child content versions, child
 * layout versions, resolved direction, inner size and the packed layout input), so it survives a
 * plain {@link TaffyTree#clearCache} exactly like the flex/grid item-list caches while every
 * mutation that can change the result invalidates it.</p>
 *
 * <p>The observable of a hit is that the pass writes no child layout: the container's direct
 * children keep the exact same unrounded {@link Layout} instances (a miss rebuilds them in
 * {@code performFinalLayout}). A miss additionally replaces or drops the cache entry.</p>
 *
 * <p>The entry also requires every direct child's {@link LayoutCache} to be warm. Clearing only
 * the container therefore hits (the warm-relayout convention), while clearing every node - the
 * cold-layout convention - misses and performs the full algorithm, so a cold layout stays cold.</p>
 */
@DisplayName("P33: flex container result cache")
class FlexResultCacheP33Test {

    private static final float EPSILON = 1e-4f;
    private static final TaffySize<AvailableSpace> SPACE =
        TaffySize.of(AvailableSpace.definite(300f), AvailableSpace.definite(120f));

    // === Helpers ===

    private static TaffyStyle flexRow() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.ROW;
        style.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(120f));
        return style;
    }

    private static TaffyStyle flexColumn() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.COLUMN;
        return style;
    }

    private static TaffyStyle leaf(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static MeasureFunc fixedMeasure(float width, float height) {
        return (knownDimensions, availableSpace) -> new FloatSize(width, height);
    }

    private static FlexboxComputer.FlexResultCache cacheOf(TaffyTree tree, NodeId node) {
        return tree.getNodeData(node).getFlexResultCache();
    }

    private static LayoutOutput size(float width, float height) {
        return LayoutOutput.fromOuterSize(new FloatSize(width, height));
    }

    // === Basic hit / miss contract ===

    @Test
    @DisplayName("a warm relayout after clearCache(root) hits and writes no child layout")
    void warmRelayoutAfterRootCacheClearHits() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        AtomicInteger calls = new AtomicInteger();
        NodeId measured = tree.newLeafWithMeasure(new TaffyStyle(),
            (knownDimensions, availableSpace) -> {
                calls.incrementAndGet();
                return new FloatSize(40f, 10f);
            });
        NodeId sibling = tree.newLeaf(leaf(20f, 10f));
        NodeId root = tree.newWithChildren(flexRow(), measured, sibling);

        tree.computeLayout(root, SPACE);
        FlexboxComputer.FlexResultCache cache = cacheOf(tree, root);
        assertNotNull(cache, "a final flex layout stores a result entry");
        Layout measuredLayout = tree.getUnroundedLayout(measured);
        Layout siblingLayout = tree.getUnroundedLayout(sibling);
        long passes = tree.layoutPassCount();
        calls.set(0);

        // measureWarm convention: only the root's layout cache is cleared.
        tree.clearCache(root);
        tree.computeLayout(root, SPACE);

        assertEquals(passes + 1, tree.layoutPassCount(), "clearCache still forces a real pass");
        assertSame(cache, cacheOf(tree, root), "the result entry itself is kept");
        assertSame(measuredLayout, tree.getUnroundedLayout(measured),
            "a hit does not rewrite the measured child layout");
        assertSame(siblingLayout, tree.getUnroundedLayout(sibling),
            "a hit does not rewrite the sibling layout");
        assertEquals(0, calls.get(), "a hit runs no child measure callback");
        assertEquals(300f, tree.getLayout(root).size().width, EPSILON);
        assertEquals(0f, tree.getLayout(measured).location().x, EPSILON);
        assertEquals(40f, tree.getLayout(sibling).location().x, EPSILON);
    }

    @Test
    @DisplayName("clearCache does not drop the result entry (item-list-cache contract)")
    void clearCacheKeepsTheEntry() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId child = tree.newLeaf(leaf(10f, 10f));
        NodeId root = tree.newWithChildren(flexRow(), child);

        tree.computeLayout(root, SPACE);
        FlexboxComputer.FlexResultCache cache = cacheOf(tree, root);
        assertNotNull(cache);

        tree.clearCache(root);
        assertSame(cache, cacheOf(tree, root), "clearCache only invalidates the layout cache");
        assertTrue(tree.getNodeData(root).getCache().isEmpty(),
            "the layout cache itself was emptied");
    }

    @Test
    @DisplayName("clearing the children's caches keeps the pass cold: no result hit")
    void coldChildCachesMiss() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        AtomicInteger calls = new AtomicInteger();
        NodeId measured = tree.newLeafWithMeasure(new TaffyStyle(),
            (knownDimensions, availableSpace) -> {
                calls.incrementAndGet();
                return new FloatSize(40f, 10f);
            });
        NodeId sibling = tree.newLeaf(leaf(20f, 10f));
        NodeId root = tree.newWithChildren(flexRow(), measured, sibling);
        tree.computeLayout(root, SPACE);
        Layout measuredBefore = tree.getUnroundedLayout(measured);
        Layout siblingBefore = tree.getUnroundedLayout(sibling);
        calls.set(0);

        // The cold-benchmark convention: every node's layout cache is cleared.
        tree.clearCache(measured);
        tree.clearCache(sibling);
        tree.clearCache(root);
        tree.computeLayout(root, SPACE);

        assertNotSame(measuredBefore, tree.getUnroundedLayout(measured),
            "a cold subtree is recomputed, not answered from the ancestor result cache");
        assertNotSame(siblingBefore, tree.getUnroundedLayout(sibling));
        assertTrue(calls.get() > 0, "the measured child is measured again");
        assertEquals(300f, tree.getLayout(root).size().width, EPSILON);
    }

    @Test
    @DisplayName("a direct child style change invalidates the entry and rebuilds the children")
    void childStyleChangeInvalidates() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId child = tree.newLeaf(leaf(10f, 10f));
        NodeId root = tree.newWithChildren(flexRow(), child);
        tree.computeLayout(root, SPACE);
        Layout before = tree.getUnroundedLayout(child);
        assertNotNull(cacheOf(tree, root));

        tree.setStyle(child, leaf(70f, 10f));
        assertNull(cacheOf(tree, root), "setStyle + dirty propagation drops the ancestor result");
        tree.computeLayout(root, SPACE);

        assertNotNull(cacheOf(tree, root));
        assertNotSame(before, tree.getUnroundedLayout(child));
        assertEquals(70f, tree.getLayout(child).size().width, EPSILON);
    }

    @Test
    @DisplayName("a deep descendant style change invalidates the ancestor container")
    void deepDescendantChangeInvalidates() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId deepLeaf = tree.newLeaf(leaf(10f, 10f));
        NodeId inner = tree.newWithChildren(flexColumn(), deepLeaf);
        NodeId root = tree.newWithChildren(flexRow(), inner);
        tree.computeLayout(root, SPACE);
        Layout innerBefore = tree.getUnroundedLayout(inner);
        Layout deepBefore = tree.getUnroundedLayout(deepLeaf);
        assertNotNull(cacheOf(tree, root));
        assertNotNull(cacheOf(tree, inner));

        tree.setStyle(deepLeaf, leaf(80f, 10f));
        assertNull(cacheOf(tree, root), "dirty propagation reaches the flex root");
        assertNull(cacheOf(tree, inner));
        tree.computeLayout(root, SPACE);

        assertNotSame(innerBefore, tree.getUnroundedLayout(inner));
        assertNotSame(deepBefore, tree.getUnroundedLayout(deepLeaf));
        assertEquals(80f, tree.getLayout(deepLeaf).size().width, EPSILON);
        assertEquals(80f, tree.getLayout(inner).size().width, EPSILON);
    }

    @Test
    @DisplayName("adding and removing a child invalidates the entry")
    void childListChangeInvalidates() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId first = tree.newLeaf(leaf(10f, 10f));
        NodeId root = tree.newWithChildren(flexRow(), first);
        tree.computeLayout(root, SPACE);
        assertNotNull(cacheOf(tree, root));

        NodeId added = tree.newLeaf(leaf(30f, 10f));
        tree.addChild(root, added);
        assertNull(cacheOf(tree, root));
        tree.computeLayout(root, SPACE);

        assertEquals(30f, tree.getLayout(added).size().width, EPSILON);
        assertEquals(0f, tree.getLayout(first).location().x, EPSILON);
        assertEquals(10f, tree.getLayout(added).location().x, EPSILON);

        tree.removeChild(root, added);
        assertNull(cacheOf(tree, root));
        tree.computeLayout(root, SPACE);
        assertEquals(0f, tree.getLayout(first).location().x, EPSILON);
    }

    @Test
    @DisplayName("a different available space misses and recomputes")
    void resizeMisses() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle percentChild = new TaffyStyle();
        percentChild.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
        NodeId child = tree.newLeaf(percentChild);
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, SPACE);
        Layout before = tree.getUnroundedLayout(child);
        assertEquals(300f, tree.getLayout(child).size().width, EPSILON);

        TaffySize<AvailableSpace> wider =
            TaffySize.of(AvailableSpace.definite(500f), AvailableSpace.definite(120f));
        tree.computeLayout(root, wider);

        assertNotSame(before, tree.getUnroundedLayout(child), "the resized pass recomputes");
        assertEquals(500f, tree.getLayout(child).size().width, EPSILON);
        assertNotNull(cacheOf(tree, root));
    }

    @Test
    @DisplayName("a direction change misses and produces the mirrored layout")
    void directionChangeMisses() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId child = tree.newLeaf(leaf(10f, 10f));
        TaffyStyle rootStyle = flexRow();
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, SPACE);
        assertEquals(0f, tree.getLayout(child).location().x, EPSILON);

        TaffyStyle rtl = flexRow();
        rtl.direction = TaffyDirection.RTL;
        tree.setStyle(root, rtl);
        tree.computeLayout(root, SPACE);

        assertEquals(290f, tree.getLayout(child).location().x, EPSILON);
    }

    @Test
    @DisplayName("a measure callback change invalidates the entry")
    void measureChangeInvalidates() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId measured = tree.newLeafWithMeasure(new TaffyStyle(), fixedMeasure(10f, 10f));
        NodeId root = tree.newWithChildren(flexRow(), measured);
        tree.computeLayout(root, SPACE);
        assertEquals(10f, tree.getLayout(measured).size().width, EPSILON);
        assertNotNull(cacheOf(tree, root));

        tree.setMeasureFunc(measured, fixedMeasure(55f, 10f));
        assertNull(cacheOf(tree, root));
        tree.computeLayout(root, SPACE);
        assertEquals(55f, tree.getLayout(measured).size().width, EPSILON);
    }

    @Test
    @DisplayName("a node-context change invalidates the entry")
    void contextChangeInvalidates() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId measured = tree.newLeafWithMeasure(new TaffyStyle(), (knownDimensions, availableSpace) ->
            new FloatSize(10f, 10f));
        NodeId root = tree.newWithChildren(flexRow(), measured);
        tree.computeLayout(root, SPACE);
        assertNotNull(cacheOf(tree, root));

        tree.setNodeContext(measured, new Object());
        assertNull(cacheOf(tree, root));
    }

    @Test
    @DisplayName("an explicit child layout write is detected by the layout version stamp")
    void explicitChildLayoutWriteMisses() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId child = tree.newLeaf(leaf(10f, 10f));
        NodeId root = tree.newWithChildren(flexRow(), child);
        tree.computeLayout(root, SPACE);
        Layout real = tree.getLayout(child);
        assertNotNull(cacheOf(tree, root));

        // Write a bogus unrounded layout directly; clearCache(root) drops the layout cache but the
        // result entry survives and must be rejected by the child's layout-version stamp.
        tree.setUnroundedLayout(child, new Layout(0, new FloatPoint(0f, 0f),
            new FloatSize(999f, 999f), FloatSize.zero(), ScrollableOverflow.ZERO,
            FloatSize.zero(), FloatRect.zero(), FloatRect.zero(), FloatRect.zero()));
        tree.clearCache(root);
        tree.computeLayout(root, SPACE);

        assertEquals(real.size().width, tree.getLayout(child).size().width, EPSILON);
        assertEquals(real.size().height, tree.getLayout(child).size().height, EPSILON);
    }

    @Test
    @DisplayName("a listener disables the cache: a listener pass rewrites children")
    void listenerDisablesTheCache() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        AtomicInteger calls = new AtomicInteger();
        NodeId child = tree.newLeafWithMeasure(new TaffyStyle(),
            (knownDimensions, availableSpace) -> {
                calls.incrementAndGet();
                return new FloatSize(10f, 10f);
            });
        NodeId root = tree.newWithChildren(flexRow(), child);
        tree.computeLayout(root, SPACE);
        assertNotNull(cacheOf(tree, root));
        Layout before = tree.getUnroundedLayout(child);

        AtomicInteger notifications = new AtomicInteger();
        tree.setLayoutChangeListener((node, oldLayout, newLayout) -> notifications.incrementAndGet());
        tree.clearCache(root);
        tree.computeLayout(root, SPACE);

        assertTrue(notifications.get() > 0, "the listener still observes every layout write");
        assertNotSame(before, tree.getUnroundedLayout(child),
            "a listener pass never answers from the result cache");
        tree.setLayoutChangeListener(null);
    }

    @Test
    @DisplayName("measure-only requests neither use nor overwrite the entry")
    void measureOnlyRequestsDoNotTouchTheCache() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId child = tree.newLeaf(leaf(10f, 10f));
        NodeId root = tree.newWithChildren(flexRow(), child);
        tree.computeLayout(root, SPACE);
        FlexboxComputer.FlexResultCache cache = cacheOf(tree, root);
        Layout before = tree.getUnroundedLayout(child);

        LayoutComputer computer = new LayoutComputer(tree, null);
        FloatSize measured = computer.measureChildSize(root, FloatSize.none(), FloatSize.none(),
            TaffySize.maxContent(), SizingMode.INHERENT_SIZE, TaffyLine.FALSE);

        assertEquals(300f, measured.width, EPSILON);
        assertSame(cache, cacheOf(tree, root), "a COMPUTE_SIZE request is not cached");
        assertSame(before, tree.getUnroundedLayout(child));
    }

    @Test
    @DisplayName("a hit keeps hasNewLayout / needsVisit reporting through the layout generation")
    void hitKeepsNewLayoutReporting() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId child = tree.newLeaf(leaf(10f, 10f));
        NodeId root = tree.newWithChildren(flexRow(), child);
        tree.computeLayout(root, SPACE);
        tree.acknowledgeLayout(root);
        tree.acknowledgeLayout(child);
        assertFalse(tree.hasNewLayout(child));

        tree.clearCache(root);
        tree.computeLayout(root, SPACE);

        assertTrue(tree.hasNewLayout(child), "the completed request still marks descendants");
        assertTrue(tree.needsVisit(root));
    }

    @Test
    @DisplayName("a warm relayout of a container with an absolute child hits")
    void absoluteChildWarmRelayoutHits() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId flow = tree.newLeaf(leaf(40f, 10f));
        TaffyStyle badgeStyle = leaf(24f, 12f);
        badgeStyle.position = TaffyPosition.ABSOLUTE;
        badgeStyle.inset = TaffyRect.ltrb(LengthPercentageAuto.AUTO,
            LengthPercentageAuto.length(4f), LengthPercentageAuto.length(4f),
            LengthPercentageAuto.AUTO);
        NodeId badge = tree.newLeaf(badgeStyle);
        NodeId root = tree.newWithChildren(flexRow(), flow, badge);
        tree.computeLayout(root, SPACE);
        Layout flowLayout = tree.getUnroundedLayout(flow);
        Layout badgeLayout = tree.getUnroundedLayout(badge);
        FlexboxComputer.FlexResultCache cache = cacheOf(tree, root);
        assertNotNull(cache);

        tree.clearCache(root);
        tree.computeLayout(root, SPACE);

        assertSame(cache, cacheOf(tree, root));
        assertSame(flowLayout, tree.getUnroundedLayout(flow));
        assertSame(badgeLayout, tree.getUnroundedLayout(badge),
            "the out-of-flow child layout is part of the cached result too");
    }

    @Test
    @DisplayName("a hidden child's assigned order is part of the cached result")
    void hiddenChildWarmRelayoutHits() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        NodeId flow = tree.newLeaf(leaf(40f, 10f));
        TaffyStyle hiddenStyle = leaf(5f, 5f);
        hiddenStyle.display = TaffyDisplay.NONE;
        NodeId hidden = tree.newLeaf(hiddenStyle);
        NodeId root = tree.newWithChildren(flexRow(), flow, hidden);
        tree.computeLayout(root, SPACE);
        Layout flowLayout = tree.getUnroundedLayout(flow);
        Layout hiddenLayout = tree.getUnroundedLayout(hidden);
        FlexboxComputer.FlexResultCache cache = cacheOf(tree, root);
        assertNotNull(cache);
        assertFalse(tree.getNodeData(hidden).getCache().isEmpty(),
            "the hidden pass clears the child cache but the ordinary store re-fills it");

        tree.clearCache(root);
        tree.computeLayout(root, SPACE);

        assertSame(cache, cacheOf(tree, root));
        assertSame(flowLayout, tree.getUnroundedLayout(flow));
        assertSame(hiddenLayout, tree.getUnroundedLayout(hidden));
        assertEquals(0f, tree.getLayout(hidden).size().width, EPSILON);
        assertEquals(1, tree.getLayout(hidden).order());
    }

    @Test
    @DisplayName("a float inside a flex item does not defeat the result cache")
    void floatInsideFlexItemStaysCorrect() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle floatedStyle = leaf(20f, 10f);
        floatedStyle.float_ = TaffyFloat.LEFT;
        NodeId floated = tree.newLeaf(floatedStyle);
        NodeId text = tree.newLeaf(leaf(40f, 10f));
        NodeId item = tree.newWithChildren(new TaffyStyle(), floated, text);
        NodeId root = tree.newWithChildren(flexRow(), item);
        tree.computeLayout(root, SPACE);
        float xBefore = tree.getLayout(text).location().x;
        assertNotNull(cacheOf(tree, root));

        // Unchanged pass: hit, no child rewrite.
        Layout floatedLayout = tree.getUnroundedLayout(floated);
        tree.clearCache(root);
        tree.computeLayout(root, SPACE);
        assertSame(floatedLayout, tree.getUnroundedLayout(floated));

        // Changing the float side changes the item's subtree -> the whole chain misses.
        TaffyStyle right = leaf(20f, 10f);
        right.float_ = TaffyFloat.RIGHT;
        tree.setStyle(floated, right);
        assertNull(cacheOf(tree, root));
        tree.computeLayout(root, SPACE);
        assertNotSame(xBefore, tree.getLayout(text).location().x);
    }

    // === Fresh vs incremental field-by-field ===

    private static final class Ids {
        NodeId root;
        NodeId measured;
        NodeId card;
        NodeId label;
        NodeId badge;
        NodeId hidden;
        NodeId floated;
        NodeId flowSibling;
        NodeId detached;
    }

    private static Ids build(TaffyTree tree) {
        Ids ids = new Ids();
        ids.measured = tree.newLeafWithMeasure(new TaffyStyle(), fixedMeasure(40f, 10f));

        TaffyStyle badgeStyle = leaf(24f, 12f);
        badgeStyle.position = TaffyPosition.ABSOLUTE;
        badgeStyle.inset = TaffyRect.ltrb(LengthPercentageAuto.length(4f),
            LengthPercentageAuto.length(4f), LengthPercentageAuto.length(4f),
            LengthPercentageAuto.AUTO);
        ids.badge = tree.newLeaf(badgeStyle);

        TaffyStyle labelStyle = new TaffyStyle();
        labelStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
        ids.label = tree.newLeafWithMeasure(labelStyle, fixedMeasure(30f, 9f));

        TaffyStyle hiddenStyle = leaf(5f, 5f);
        hiddenStyle.display = TaffyDisplay.NONE;
        ids.hidden = tree.newLeaf(hiddenStyle);

        TaffyStyle cardStyle = flexColumn();
        cardStyle.padding = TaffyRect.all(LengthPercentage.length(4f));
        cardStyle.gap = TaffySize.of(LengthPercentage.length(3f), LengthPercentage.length(3f));
        ids.card = tree.newWithChildren(cardStyle, ids.label, ids.badge, ids.hidden);

        TaffyStyle floatedStyle = leaf(20f, 10f);
        floatedStyle.float_ = TaffyFloat.LEFT;
        ids.floated = tree.newLeaf(floatedStyle);
        ids.flowSibling = tree.newLeaf(leaf(40f, 10f));

        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(120f));
        rootStyle.padding = TaffyRect.all(LengthPercentage.length(6f));
        rootStyle.gap = TaffySize.of(LengthPercentage.length(5f), LengthPercentage.length(5f));
        ids.root = tree.newWithChildren(rootStyle, ids.measured, ids.card, ids.floated, ids.flowSibling);

        ids.detached = tree.newWithChildren(flexRow(), tree.newLeaf(leaf(33f, 11f)));
        return ids;
    }

    private record Fixture(TaffyTree tree, Ids ids) {
    }

    /** Builds a fresh tree with the full scenario and lays it out at {@code space}. */
    private static Fixture freshAt(boolean rounding, TaffySize<AvailableSpace> space) {
        TaffyTree fresh = new TaffyTree();
        if (!rounding) {
            fresh.disableRounding();
        }
        Ids freshIds = build(fresh);
        fresh.computeLayout(freshIds.root, space);
        return new Fixture(fresh, freshIds);
    }

    @Test
    @DisplayName("a hit reproduces every fresh layout field, rounding on and off")
    void hitReproducesEveryFreshField() {
        for (boolean rounding : new boolean[]{false, true}) {
            TaffyTree tree = new TaffyTree();
            if (!rounding) {
                tree.disableRounding();
            }
            Ids ids = build(tree);
            tree.computeLayout(ids.root, SPACE);

            tree.clearCache(ids.root);
            tree.computeLayout(ids.root, SPACE);

            Fixture fixture = freshAt(rounding, SPACE);
            List<String> problems = new ArrayList<>();
            compareSubtree(problems, "root", tree, ids.root, fixture.tree(), fixture.ids().root);
            assertTrue(problems.isEmpty(), () -> "hit diverged from a fresh layout:\n"
                + String.join("\n", problems));
        }
    }

    @Test
    @DisplayName("every mutation leaves the incremental layouts equal to a fresh tree")
    void mutationSequenceMatchesFreshTree() {
        List<Mutation> mutations = new ArrayList<>();
        mutations.add(new Mutation("clearCache(root)+hit", (tree, ids) -> tree.clearCache(ids.root), true));
        mutations.add(new Mutation("child style", (tree, ids) ->
            tree.setStyle(ids.measured, new TaffyStyle())));
        mutations.add(new Mutation("measure callback", (tree, ids) ->
            tree.setMeasureFunc(ids.measured, fixedMeasure(55f, 10f))));
        mutations.add(new Mutation("in-place markDirty", (tree, ids) ->
            tree.markDirty(ids.flowSibling)));
        mutations.add(new Mutation("node context", (tree, ids) ->
            tree.setNodeContext(ids.measured, new Object())));
        mutations.add(new Mutation("direction", (tree, ids) -> {
            TaffyStyle rtl = tree.getStyle(ids.root);
            rtl.direction = TaffyDirection.RTL;
            tree.setStyle(ids.root, rtl);
        }));
        mutations.add(new Mutation("add child", (tree, ids) ->
            tree.addChild(ids.root, ids.detached)));
        mutations.add(new Mutation("remove child", (tree, ids) ->
            tree.removeChild(ids.root, ids.detached)));
        mutations.add(new Mutation("float side", (tree, ids) -> {
            TaffyStyle right = leaf(20f, 10f);
            right.float_ = TaffyFloat.RIGHT;
            tree.setStyle(ids.floated, right);
        }));
        mutations.add(new Mutation("hidden to visible", (tree, ids) ->
            tree.setStyle(ids.hidden, leaf(7f, 7f))));
        mutations.add(new Mutation("badge size", (tree, ids) -> {
            TaffyStyle bigger = leaf(30f, 12f);
            bigger.position = TaffyPosition.ABSOLUTE;
            bigger.inset = TaffyRect.ltrb(LengthPercentageAuto.length(4f),
                LengthPercentageAuto.length(4f), LengthPercentageAuto.length(4f),
                LengthPercentageAuto.AUTO);
            tree.setStyle(ids.badge, bigger);
        }));
        mutations.add(new Mutation("overflow scroll", (tree, ids) -> {
            TaffyStyle scroll = tree.getStyle(ids.card);
            scroll.overflow = new dev.vfyjxf.taffy.geometry.TaffyPoint<>(Overflow.SCROLL, Overflow.SCROLL);
            tree.setStyle(ids.card, scroll);
        }));

        for (boolean rounding : new boolean[]{false, true}) {
            TaffyTree tree = new TaffyTree();
            if (!rounding) {
                tree.disableRounding();
            }
            Ids ids = build(tree);
            tree.computeLayout(ids.root, SPACE);

            for (int step = 0; step < mutations.size(); step++) {
                Mutation mutation = mutations.get(step);

                // Replay the same mutations on a fresh tree, then lay it out once.
                TaffyTree fresh = new TaffyTree();
                if (!rounding) {
                    fresh.disableRounding();
                }
                Ids freshIds = build(fresh);
                for (int replay = 0; replay <= step; replay++) {
                    Mutation earlier = mutations.get(replay);
                    if (!earlier.isLayoutRequest()) {
                        earlier.apply(fresh, freshIds);
                    }
                }
                fresh.computeLayout(freshIds.root, SPACE);

                if (!mutation.isLayoutRequest()) {
                    mutation.apply(tree, ids);
                    tree.computeLayout(ids.root, SPACE);
                } else {
                    tree.computeLayout(ids.root, SPACE);
                }

                List<String> problems = new ArrayList<>();
                compareSubtree(problems, "rounding=" + rounding + " step=" + step
                    + " [" + mutation.name() + "]", tree, ids.root, fresh, freshIds.root);
                assertTrue(problems.isEmpty(), () -> "incremental diverged from fresh:\n"
                    + String.join("\n", problems));
            }
        }
    }

    private record Mutation(String name, java.util.function.BiConsumer<TaffyTree, Ids> action,
                            boolean isLayoutRequest) {
        Mutation(String name, java.util.function.BiConsumer<TaffyTree, Ids> action) {
            this(name, action, false);
        }

        void apply(TaffyTree tree, Ids ids) {
            action.accept(tree, ids);
        }
    }

    private static void compareSubtree(List<String> problems, String path, TaffyTree tree, NodeId node,
                                       TaffyTree fresh, NodeId freshNode) {
        if (node == null || freshNode == null) {
            problems.add(path + ": missing node (" + node + " vs " + freshNode + ")");
            return;
        }
        compareLayout(problems, path + " final", fresh.getLayout(freshNode), tree.getLayout(node));
        compareLayout(problems, path + " unrounded",
            fresh.getUnroundedLayout(freshNode), tree.getUnroundedLayout(node));
        List<NodeId> children = tree.getChildren(node);
        List<NodeId> freshChildren = fresh.getChildren(freshNode);
        if (children.size() != freshChildren.size()) {
            problems.add(path + " childCount: fresh=" + freshChildren.size()
                + " incremental=" + children.size());
            return;
        }
        for (int i = 0; i < children.size(); i++) {
            compareSubtree(problems, path + "/" + i, tree, children.get(i), fresh, freshChildren.get(i));
        }
    }

    private static void compareLayout(List<String> problems, String where, Layout expected, Layout actual) {
        if (expected == null || actual == null) {
            if (expected != actual) {
                problems.add(where + ": fresh=" + expected + " incremental=" + actual);
            }
            return;
        }
        checkInt(problems, where, "order", expected.order(), actual.order());
        checkFloat(problems, where, "location.x", expected.location().x, actual.location().x);
        checkFloat(problems, where, "location.y", expected.location().y, actual.location().y);
        checkFloat(problems, where, "size.width", expected.size().width, actual.size().width);
        checkFloat(problems, where, "size.height", expected.size().height, actual.size().height);
        checkFloat(problems, where, "contentSize.width",
            expected.contentSize().width, actual.contentSize().width);
        checkFloat(problems, where, "contentSize.height",
            expected.contentSize().height, actual.contentSize().height);

        ScrollableOverflow expectedOverflow = expected.scrollableOverflow();
        ScrollableOverflow actualOverflow = actual.scrollableOverflow();
        checkFloat(problems, where, "overflow.left", expectedOverflow.left(), actualOverflow.left());
        checkFloat(problems, where, "overflow.right", expectedOverflow.right(), actualOverflow.right());
        checkFloat(problems, where, "overflow.top", expectedOverflow.top(), actualOverflow.top());
        checkFloat(problems, where, "overflow.bottom", expectedOverflow.bottom(), actualOverflow.bottom());
        checkBoolean(problems, where, "overflow.reversedX",
            expectedOverflow.reversedX(), actualOverflow.reversedX());
        checkBoolean(problems, where, "overflow.reversedY",
            expectedOverflow.reversedY(), actualOverflow.reversedY());

        checkFloat(problems, where, "scrollbar.width",
            expected.scrollbarSize().width, actual.scrollbarSize().width);
        checkFloat(problems, where, "scrollbar.height",
            expected.scrollbarSize().height, actual.scrollbarSize().height);
        compareRect(problems, where, "border", expected.border(), actual.border());
        compareRect(problems, where, "padding", expected.padding(), actual.padding());
        compareRect(problems, where, "margin", expected.margin(), actual.margin());
    }

    private static void compareRect(List<String> problems, String where, String field,
                                    FloatRect expected, FloatRect actual) {
        checkFloat(problems, where, field + ".left", expected.left, actual.left);
        checkFloat(problems, where, field + ".right", expected.right, actual.right);
        checkFloat(problems, where, field + ".top", expected.top, actual.top);
        checkFloat(problems, where, field + ".bottom", expected.bottom, actual.bottom);
    }

    private static void checkFloat(List<String> problems, String where, String field,
                                   float expected, float actual) {
        if (Float.compare(expected, actual) != 0) {
            problems.add(where + " " + field + ": fresh=" + expected + " incremental=" + actual);
        }
    }

    private static void checkInt(List<String> problems, String where, String field,
                                 int expected, int actual) {
        if (expected != actual) {
            problems.add(where + " " + field + ": fresh=" + expected + " incremental=" + actual);
        }
    }

    private static void checkBoolean(List<String> problems, String where, String field,
                                     boolean expected, boolean actual) {
        if (expected != actual) {
            problems.add(where + " " + field + ": fresh=" + expected + " incremental=" + actual);
        }
    }
}
