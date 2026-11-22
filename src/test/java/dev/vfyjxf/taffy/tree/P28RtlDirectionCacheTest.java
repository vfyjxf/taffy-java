package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.util.MeasureFunc;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * P28: the layout cache must not answer a direction-dependent layout with a result computed for
 * the other direction.
 *
 * <p>{@code LayoutInput} has no direction slot: {@code direction: inherit} is resolved from the
 * ancestors, so changing one node's direction changes the resolved direction of every inheriting
 * descendant without clearing their layout caches. Before this package the cache was keyed on the
 * {@link LayoutInput} alone, so an LTR pass followed by an RTL change reused the LTR entry of
 * every warm descendant: a container's cached output short-circuited the re-layout of its subtree
 * and percent descendants kept their LTR {@code location}/{@code scrollableOverflow}. The fix
 * makes the resolved direction a cache-wide part of the key, validated by the layout algorithms on
 * both the ordinary and the context-free path, without dirtying the subtree.</p>
 */
@DisplayName("P28: resolved direction is part of the layout cache key")
public class P28RtlDirectionCacheTest {

    private static final float EPSILON = 0.001f;

    // === Fixtures ===

    private static TaffySize<AvailableSpace> definite(float width, float height) {
        return new TaffySize<>(AvailableSpace.definite(width), AvailableSpace.definite(height));
    }

    private static TaffyStyle flexRow(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.ROW;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle gridBox(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        style.gridTemplateColumns = List.of(TrackSizingFunction.fr(1f));
        return style;
    }

    private static TaffyStyle blockBox(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle percentLeaf(float percent, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.percent(percent), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle rtl(TaffyStyle style) {
        TaffyStyle copy = copy(style);
        copy.direction = TaffyDirection.RTL;
        return copy;
    }

    private static TaffyStyle ltr(TaffyStyle style) {
        TaffyStyle copy = copy(style);
        copy.direction = TaffyDirection.LTR;
        return copy;
    }

    private static TaffyStyle copy(TaffyStyle style) {
        TaffyStyle copy = style.copy();
        return copy != null ? copy : style;
    }

    private record Form(NodeId root, NodeId middle, NodeId leaf) {}

    /** Root (given direction) -> INHERIT flex row -> percent leaf. */
    private static Form buildFlex(TaffyTree tree, TaffyDirection direction) {
        TaffyStyle rootStyle = flexRow(300f, 100f);
        rootStyle.direction = direction;
        NodeId leaf = tree.newLeaf(percentLeaf(0.5f, 10f));
        NodeId middle = tree.newLeaf(flexRow(100f, 20f));
        tree.addChild(middle, leaf);
        NodeId root = tree.newWithChildren(rootStyle, middle);
        return new Form(root, middle, leaf);
    }

    /** Root (given direction) -> INHERIT block box -> INHERIT flex row -> percent leaf. */
    private static Form buildBlockWithFlexInner(TaffyTree tree, TaffyDirection direction) {
        TaffyStyle rootStyle = blockBox(300f, 100f);
        rootStyle.direction = direction;
        NodeId leaf = tree.newLeaf(percentLeaf(0.5f, 10f));
        NodeId inner = tree.newLeaf(flexRow(100f, 20f));
        tree.addChild(inner, leaf);
        TaffyStyle middleStyle = blockBox(100f, 20f);
        // A block container that always recomputes is placed between the root and the flex child,
        // so the stale entry is the flex child's ordinary final-layout entry.
        middleStyle.overflow = new TaffyPoint<>(Overflow.HIDDEN, Overflow.HIDDEN);
        NodeId middle = tree.newWithChildren(middleStyle, inner);
        NodeId root = tree.newWithChildren(rootStyle, middle);
        return new Form(root, inner, leaf);
    }

    /** Root flex row (given direction) -> INHERIT block box -> percent leaf. */
    private static Form buildFlexWithBlockChild(TaffyTree tree, TaffyDirection direction) {
        TaffyStyle rootStyle = flexRow(300f, 100f);
        rootStyle.direction = direction;
        NodeId leaf = tree.newLeaf(percentLeaf(0.5f, 10f));
        NodeId middle = tree.newWithChildren(blockBox(100f, 20f), leaf);
        NodeId root = tree.newWithChildren(rootStyle, middle);
        return new Form(root, middle, leaf);
    }

    /** Root (given direction) -> INHERIT grid -> percent leaf. */
    private static Form buildGrid(TaffyTree tree, TaffyDirection direction) {
        TaffyStyle rootStyle = gridBox(300f, 100f);
        rootStyle.direction = direction;
        NodeId leaf = tree.newLeaf(percentLeaf(0.5f, 10f));
        NodeId middle = tree.newLeaf(gridBox(100f, 20f));
        tree.addChild(middle, leaf);
        NodeId root = tree.newWithChildren(rootStyle, middle);
        return new Form(root, middle, leaf);
    }

    /** Root flex row (given direction) -> explicit-direction flex row -> percent leaf. */
    private static Form buildFlexWithExplicitMiddle(TaffyTree tree, TaffyDirection rootDirection,
                                                    TaffyDirection middleDirection) {
        TaffyStyle rootStyle = flexRow(300f, 100f);
        rootStyle.direction = rootDirection;
        NodeId leaf = tree.newLeaf(percentLeaf(0.5f, 10f));
        TaffyStyle middleStyle = flexRow(100f, 20f);
        middleStyle.direction = middleDirection;
        NodeId middle = tree.newWithChildren(middleStyle, leaf);
        NodeId root = tree.newWithChildren(rootStyle, middle);
        return new Form(root, middle, leaf);
    }

    // === End-to-end regressions ===

    @Test
    @DisplayName("LTR flex pass, then RTL: inherited percent leaf matches a fresh RTL tree")
    void flexLtrToRtlMatchesFreshTree() {
        TaffyTree cachedTree = new TaffyTree();
        cachedTree.disableRounding();
        Form cachedForm = buildFlex(cachedTree, TaffyDirection.LTR);
        cachedTree.computeLayout(cachedForm.root(), definite(300f, 100f));

        cachedTree.setStyle(cachedForm.root(), rtl(flexRow(300f, 100f)));
        cachedTree.computeLayout(cachedForm.root(), definite(300f, 100f));

        TaffyTree freshTree = new TaffyTree();
        freshTree.disableRounding();
        Form freshForm = buildFlex(freshTree, TaffyDirection.RTL);
        freshTree.computeLayout(freshForm.root(), definite(300f, 100f));

        assertTreeMatches(cachedTree, cachedForm, freshTree, freshForm);
        // The mirrored position is the observable regression: the leaf sits against the right
        // edge of its 100-wide RTL middle container.
        assertEquals(50f, cachedTree.getLayout(cachedForm.leaf()).location().x, EPSILON,
            "the percent leaf follows the inherited RTL direction");
        assertEquals(200f, cachedTree.getLayout(cachedForm.middle()).location().x, EPSILON,
            "the middle container is positioned from the RTL root's right edge");

        // The second compute of the same tree must be stable: the first pass has already replaced
        // the LTR entries, so repeating the request may reuse them.
        cachedTree.computeLayout(cachedForm.root(), definite(300f, 100f));
        assertTreeMatches(cachedTree, cachedForm, freshTree, freshForm);
    }

    @Test
    @DisplayName("RTL flex pass, then LTR: inherited percent leaf matches a fresh LTR tree")
    void flexRtlToLtrMatchesFreshTree() {
        TaffyTree cachedTree = new TaffyTree();
        cachedTree.disableRounding();
        Form cachedForm = buildFlex(cachedTree, TaffyDirection.RTL);
        cachedTree.computeLayout(cachedForm.root(), definite(300f, 100f));
        assertEquals(50f, cachedTree.getLayout(cachedForm.leaf()).location().x, EPSILON);

        cachedTree.setStyle(cachedForm.root(), ltr(flexRow(300f, 100f)));
        cachedTree.computeLayout(cachedForm.root(), definite(300f, 100f));

        TaffyTree freshTree = new TaffyTree();
        freshTree.disableRounding();
        Form freshForm = buildFlex(freshTree, TaffyDirection.LTR);
        freshTree.computeLayout(freshForm.root(), definite(300f, 100f));

        assertTreeMatches(cachedTree, cachedForm, freshTree, freshForm);
        assertEquals(0f, cachedTree.getLayout(cachedForm.leaf()).location().x, EPSILON,
            "the percent leaf follows the inherited LTR direction");
    }

    @Test
    @DisplayName("block root with an inheriting flex child matches a fresh tree after LTR -> RTL")
    void blockRootWithFlexChildMatchesFreshTree() {
        TaffyTree cachedTree = new TaffyTree();
        cachedTree.disableRounding();
        Form cachedForm = buildBlockWithFlexInner(cachedTree, TaffyDirection.LTR);
        cachedTree.computeLayout(cachedForm.root(), definite(300f, 100f));

        cachedTree.setStyle(cachedForm.root(), rtl(blockBox(300f, 100f)));
        cachedTree.computeLayout(cachedForm.root(), definite(300f, 100f));

        TaffyTree freshTree = new TaffyTree();
        freshTree.disableRounding();
        Form freshForm = buildBlockWithFlexInner(freshTree, TaffyDirection.RTL);
        freshTree.computeLayout(freshForm.root(), definite(300f, 100f));

        assertTreeMatches(cachedTree, cachedForm, freshTree, freshForm);
        assertEquals(50f, cachedTree.getLayout(cachedForm.leaf()).location().x, EPSILON,
            "the percent leaf follows the direction inherited through the block container");
    }

    @Test
    @DisplayName("a block container as a flex item re-lays out its percent child after LTR -> RTL")
    void flexRootWithBlockChildMatchesFreshTree() {
        TaffyTree cachedTree = new TaffyTree();
        cachedTree.disableRounding();
        Form cachedForm = buildFlexWithBlockChild(cachedTree, TaffyDirection.LTR);
        cachedTree.computeLayout(cachedForm.root(), definite(300f, 100f));

        cachedTree.setStyle(cachedForm.root(), rtl(flexRow(300f, 100f)));
        cachedTree.computeLayout(cachedForm.root(), definite(300f, 100f));

        TaffyTree freshTree = new TaffyTree();
        freshTree.disableRounding();
        Form freshForm = buildFlexWithBlockChild(freshTree, TaffyDirection.RTL);
        freshTree.computeLayout(freshForm.root(), definite(300f, 100f));

        assertTreeMatches(cachedTree, cachedForm, freshTree, freshForm);
        assertEquals(50f, cachedTree.getLayout(cachedForm.leaf()).location().x, EPSILON,
            "the percent leaf is right-aligned by the resumed block layout");
    }

    @Test
    @DisplayName("the P22 grid shape matches a fresh tree after LTR -> RTL")
    void gridLtrToRtlMatchesFreshTree() {
        TaffyTree cachedTree = new TaffyTree();
        cachedTree.disableRounding();
        Form cachedForm = buildGrid(cachedTree, TaffyDirection.LTR);
        cachedTree.computeLayout(cachedForm.root(), definite(300f, 100f));

        cachedTree.setStyle(cachedForm.root(), rtl(gridBox(300f, 100f)));
        cachedTree.computeLayout(cachedForm.root(), definite(300f, 100f));

        TaffyTree freshTree = new TaffyTree();
        freshTree.disableRounding();
        Form freshForm = buildGrid(freshTree, TaffyDirection.RTL);
        freshTree.computeLayout(freshForm.root(), definite(300f, 100f));

        assertTreeMatches(cachedTree, cachedForm, freshTree, freshForm);
        assertEquals(50f, cachedTree.getLayout(cachedForm.leaf()).location().x, EPSILON,
            "the percent leaf follows the inherited RTL direction in the grid");
    }

    @Test
    @DisplayName("a direction change does not dirty the subtree's content versions")
    void directionChangeDoesNotDirtyTheSubtree() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        Form form = buildFlex(tree, TaffyDirection.LTR);
        tree.computeLayout(form.root(), definite(300f, 100f));

        long middleVersion = tree.getNodeData(form.middle()).getContentVersion();
        long leafVersion = tree.getNodeData(form.leaf()).getContentVersion();

        tree.setStyle(form.root(), rtl(flexRow(300f, 100f)));

        assertEquals(middleVersion, tree.getNodeData(form.middle()).getContentVersion(),
            "the fix must not bump a descendant's content version");
        assertEquals(leafVersion, tree.getNodeData(form.leaf()).getContentVersion(),
            "the fix must not bump a descendant's content version");
        assertFalse(tree.isDirty(form.leaf()),
            "the descendant cache stays warm; the direction is validated at lookup time");

        tree.computeLayout(form.root(), definite(300f, 100f));

        TaffyTree freshTree = new TaffyTree();
        freshTree.disableRounding();
        Form freshForm = buildFlex(freshTree, TaffyDirection.RTL);
        freshTree.computeLayout(freshForm.root(), definite(300f, 100f));
        assertTreeMatches(tree, form, freshTree, freshForm);
    }

    @Test
    @DisplayName("an explicit-direction subtree keeps its own direction while the root flips")
    void explicitDirectionSubtreeKeepsItsDirection() {
        TaffyTree cachedTree = new TaffyTree();
        cachedTree.disableRounding();
        Form cachedForm = buildFlexWithExplicitMiddle(cachedTree, TaffyDirection.RTL,
            TaffyDirection.LTR);
        cachedTree.computeLayout(cachedForm.root(), definite(300f, 100f));
        assertEquals(0f, cachedTree.getLayout(cachedForm.leaf()).location().x, EPSILON,
            "the explicit LTR middle stops the RTL inheritance");

        cachedTree.setStyle(cachedForm.root(), ltr(flexRow(300f, 100f)));
        cachedTree.computeLayout(cachedForm.root(), definite(300f, 100f));

        TaffyTree freshTree = new TaffyTree();
        freshTree.disableRounding();
        Form freshForm = buildFlexWithExplicitMiddle(freshTree, TaffyDirection.LTR,
            TaffyDirection.LTR);
        freshTree.computeLayout(freshForm.root(), definite(300f, 100f));

        assertTreeMatches(cachedTree, cachedForm, freshTree, freshForm);
        assertEquals(0f, cachedTree.getLayout(cachedForm.leaf()).location().x, EPSILON,
            "the explicit-direction subtree is unaffected by the root's direction change");
        assertEquals(0f, cachedTree.getLayout(cachedForm.middle()).location().x, EPSILON,
            "the newly LTR root repositions the middle container");
    }

    @Test
    @DisplayName("an in-place direction mutation is observed by a direct layout pass")
    void inPlaceDirectionMutationIsObserved() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        Form form = buildFlex(tree, TaffyDirection.LTR);
        tree.computeLayout(form.root(), definite(300f, 100f));

        // No setStyle and no markDirty: only the pass-start direction epoch bump can notice this.
        // A pass started through LayoutComputer bypasses the tree-clean fast path, so the
        // direction-aware cache lookup is what has to reject the LTR entries.
        tree.getStyle(form.root()).direction = TaffyDirection.RTL;
        new LayoutComputer(tree, (MeasureFunc) null).computeLayout(form.root(), definite(300f, 100f));

        TaffyTree freshTree = new TaffyTree();
        freshTree.disableRounding();
        Form freshForm = buildFlex(freshTree, TaffyDirection.RTL);
        freshTree.computeLayout(freshForm.root(), definite(300f, 100f));

        assertTreeMatches(tree, form, freshTree, freshForm);
    }

    // === Cache contract ===

    private static LayoutInput finalInput() {
        return new LayoutInput(
            RunMode.PERFORM_LAYOUT,
            SizingMode.INHERENT_SIZE,
            RequestedAxis.BOTH,
            new FloatSize(100f, Float.NaN),
            new FloatSize(300f, 100f),
            definite(300f, 100f),
            TaffyLine.FALSE
        );
    }

    private static LayoutInput measureInput(float knownWidth) {
        return new LayoutInput(
            RunMode.COMPUTE_SIZE,
            SizingMode.INHERENT_SIZE,
            RequestedAxis.BOTH,
            new FloatSize(knownWidth, Float.NaN),
            new FloatSize(300f, 100f),
            definite(300f, 100f),
            TaffyLine.FALSE
        );
    }

    private static LayoutOutput output(float width, float height) {
        return LayoutOutput.fromOuterSize(new FloatSize(width, height));
    }

    @Test
    @DisplayName("a direction-aware lookup rejects an entry from the other direction")
    void directionAwareLookupRequiresTheSameDirection() {
        LayoutCache cache = new LayoutCache();
        LayoutInput inputs = finalInput();
        LayoutOutput stored = output(10f, 20f);

        cache.store(inputs, stored, TaffyDirection.LTR);

        assertSame(stored, cache.get(inputs, TaffyDirection.LTR));
        assertNull(cache.get(inputs, TaffyDirection.RTL),
            "the same inputs in the other direction must not reuse the mirrored layout");
        // The public direction-less lookup keeps its historical, direction-agnostic behaviour.
        assertSame(stored, cache.get(inputs));
    }

    @Test
    @DisplayName("storing under another direction drops both final and measure entries")
    void storeUnderAnotherDirectionEvictsTheEntries() {
        LayoutCache cache = new LayoutCache();
        LayoutInput finalInputs = finalInput();
        LayoutInput measureInputs = measureInput(50f);
        LayoutOutput ltrFinal = output(10f, 20f);
        LayoutOutput ltrMeasure = output(50f, 20f);

        cache.store(finalInputs, ltrFinal, TaffyDirection.LTR);
        cache.store(measureInputs, ltrMeasure, TaffyDirection.LTR);

        assertNull(cache.get(finalInputs, TaffyDirection.RTL));
        assertNull(cache.get(measureInputs, TaffyDirection.RTL));

        LayoutOutput rtlFinal = output(30f, 40f);
        cache.store(finalInputs, rtlFinal, TaffyDirection.RTL);
        assertSame(rtlFinal, cache.get(finalInputs, TaffyDirection.RTL));
        assertNull(cache.get(finalInputs, TaffyDirection.LTR),
            "the old direction's final entry was dropped");
        assertNull(cache.get(measureInputs, TaffyDirection.LTR),
            "the old direction's measure entries were dropped too");

        cache.store(measureInputs, output(75f, 20f), TaffyDirection.RTL);
        assertNull(cache.get(measureInputs, TaffyDirection.LTR));
        assertNotNull(cache.get(measureInputs, TaffyDirection.RTL));
    }

    @Test
    @DisplayName("the public direction-less API stays direction-agnostic")
    void publicStoreAndLookupStayDirectionAgnostic() {
        LayoutCache cache = new LayoutCache();
        LayoutInput inputs = finalInput();
        LayoutOutput stored = output(10f, 20f);

        cache.store(inputs, stored);

        assertSame(stored, cache.get(inputs), "the public lookup keeps working");
        assertSame(stored, cache.get(inputs, TaffyDirection.LTR),
            "an entry stored without a direction is unknown, not LTR");
        assertSame(stored, cache.get(inputs, TaffyDirection.RTL),
            "an entry stored without a direction is unknown, not RTL");

        LayoutOutput directional = output(30f, 40f);
        cache.store(inputs, directional, TaffyDirection.LTR);
        assertSame(directional, cache.get(inputs, TaffyDirection.LTR));
        assertNull(cache.get(inputs, TaffyDirection.RTL));
        assertSame(directional, cache.get(inputs),
            "the public lookup still answers after a direction-aware store");
    }

    @Test
    @DisplayName("clearing the cache forgets the cached direction")
    void clearForgetsTheCachedDirection() {
        LayoutCache cache = new LayoutCache();
        LayoutInput inputs = finalInput();
        cache.store(inputs, output(10f, 20f), TaffyDirection.LTR);
        cache.clear();

        LayoutOutput unknown = output(30f, 40f);
        cache.store(inputs, unknown);
        assertSame(unknown, cache.get(inputs, TaffyDirection.RTL),
            "a cleared cache starts over with no direction state");
    }

    @Test
    @DisplayName("the context-free entry is gated by the same direction")
    void contextFreeEntryHonoursTheCacheDirection() {
        LayoutCache cache = new LayoutCache();
        LayoutInput inputs = finalInput();
        LayoutOutput stored = output(10f, 20f);

        cache.storeContextFreeFinalEntry(inputs, stored, TaffyDirection.LTR);

        assertNull(cache.getContextFreeFinalEntry(inputs, TaffyDirection.RTL));
        assertSame(stored, cache.getContextFreeFinalEntry(inputs, TaffyDirection.LTR));

        LayoutOutput rtl = output(30f, 40f);
        cache.store(inputs, rtl, TaffyDirection.RTL);
        assertNull(cache.getContextFreeFinalEntry(inputs, TaffyDirection.LTR),
            "the ordinary RTL store dropped the LTR entries, mark included");
        assertNull(cache.getContextFreeFinalEntry(inputs, TaffyDirection.RTL),
            "an ordinary store never grants context-free reuse");
    }

    // === Helpers ===

    private static void assertTreeMatches(TaffyTree cachedTree, Form cachedForm,
                                          TaffyTree freshTree, Form freshForm) {
        assertLayoutEquals("root", freshTree.getLayout(freshForm.root()),
            cachedTree.getLayout(cachedForm.root()));
        assertLayoutEquals("middle", freshTree.getLayout(freshForm.middle()),
            cachedTree.getLayout(cachedForm.middle()));
        assertLayoutEquals("leaf", freshTree.getLayout(freshForm.leaf()),
            cachedTree.getLayout(cachedForm.leaf()));
    }

    private static void assertLayoutEquals(String label, Layout expected, Layout actual) {
        assertNotNull(expected, label + " expected");
        assertNotNull(actual, label + " actual");
        assertEquals(expected.order(), actual.order(), label + " order");
        assertEquals(expected.location(), actual.location(), label + " location");
        assertEquals(expected.size(), actual.size(), label + " size");
        assertEquals(expected.contentSize(), actual.contentSize(), label + " contentSize");
        assertEquals(expected.scrollableOverflow(), actual.scrollableOverflow(),
            label + " scrollableOverflow");
        assertEquals(expected.scrollbarSize(), actual.scrollbarSize(), label + " scrollbarSize");
        assertEquals(expected.border(), actual.border(), label + " border");
        assertEquals(expected.padding(), actual.padding(), label + " padding");
        assertEquals(expected.margin(), actual.margin(), label + " margin");
    }
}
