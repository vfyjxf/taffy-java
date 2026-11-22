package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P27 regression: the {@link Layout#order()} of a {@code display: none} child.
 *
 * <p>A hidden child is filtered out of the parent's item list, so every block, flex and grid
 * container walks its child list once more and writes {@code Layout.withOrder(index)} for each
 * hidden child before laying it out with a zero layout. That value must survive the hidden layout
 * pass. Before the fix the hidden pass re-wrote order {@code 0} on the first (uncached) visit, so
 * the same tree reported order {@code 0} on its first layout, {@code 0} on an identical repeat
 * (the whole layout is reused) but the child's <em>index</em> as soon as a later pass forced the
 * container to run again (a resize), while a freshly built tree reported {@code 0} again. The
 * tests below pin all of those cases to the container-assigned index.</p>
 *
 * <p>Inside a hidden subtree the upstream behaviour is kept: every descendant of a
 * {@code display: none} node is itself laid out hidden and reports order {@code 0} (upstream
 * {@code compute_hidden_layout} writes {@code Layout::with_order(0)}); that is intentionally not
 * the child's index because the subtree never participates in rendering. Both rounding modes are
 * covered: rounding on reads the final layout, rounding off reads the unrounded layout.</p>
 */
public class DisplayNoneOrderP27Test {

    private static final float EPSILON = 0.0f;
    private static final TaffyDisplay[] CONTAINERS = {
        TaffyDisplay.BLOCK, TaffyDisplay.FLEX, TaffyDisplay.GRID
    };

    @Test
    @DisplayName("block: display:none children keep their indices on every pass")
    void blockHiddenChildOrderIsStable() {
        assertHiddenChildOrderIsStable(TaffyDisplay.BLOCK);
    }

    @Test
    @DisplayName("flex: display:none children keep their indices on every pass")
    void flexHiddenChildOrderIsStable() {
        assertHiddenChildOrderIsStable(TaffyDisplay.FLEX);
    }

    @Test
    @DisplayName("grid: display:none children keep their indices on every pass")
    void gridHiddenChildOrderIsStable() {
        assertHiddenChildOrderIsStable(TaffyDisplay.GRID);
    }

    /**
     * Covers the three observations that used to disagree: the first layout, an identical repeat
     * (the layout-reuse fast path returns the memoized root layout), and a later pass at a
     * different available space (the container runs again while the hidden child's cache entry is
     * still valid). Every observation must equal the child index, and a freshly built tree laid
     * out at either space must agree.
     *
     * <p>Two hidden children are used so a fix that always writes one value cannot pass by
     * accident, and the second child's index (2) cannot be confused with the zero default.</p>
     */
    private static void assertHiddenChildOrderIsStable(TaffyDisplay display) {
        assertHiddenChildOrderIsStable(display, true);
        assertHiddenChildOrderIsStable(display, false);
    }

    private static void assertHiddenChildOrderIsStable(TaffyDisplay display, boolean rounding) {
        String label = display + (rounding ? " rounding-on" : " rounding-off");

        TaffyTree tree = newTree(rounding);
        NodeId root = tree.newWithChildren(container(display),
            tree.newLeaf(visibleBox(10f, 10f)),
            tree.newLeaf(hiddenBox()),
            tree.newLeaf(hiddenBox()),
            tree.newLeaf(visibleBox(12f, 12f)));
        NodeId firstHidden = tree.getChildren(root).get(1);
        NodeId secondHidden = tree.getChildren(root).get(2);

        // First layout: the uncached hidden pass used to overwrite the index with 0.
        tree.computeLayout(root, layoutSize(100f, 100f));
        assertOrders(label + " first layout", tree, firstHidden, secondHidden, 1, 2);
        assertEquals(0f, tree.getLayout(firstHidden).size().width, EPSILON, label + " first pass hidden width");

        // Identical repeat: the root layout is reused, nothing is written again.
        tree.computeLayout(root, layoutSize(100f, 100f));
        assertOrders(label + " identical repeat", tree, firstHidden, secondHidden, 1, 2);

        // Different available space: the container runs again and its hidden-child loop writes
        // the index; the hidden child's own cache entry still hits, so only a stable index can
        // keep this pass equal to the first one.
        tree.computeLayout(root, layoutSize(140f, 120f));
        assertOrders(label + " resized layout", tree, firstHidden, secondHidden, 1, 2);

        // A freshly built tree at either available space must report the same values.
        assertFreshTreeOrders(label + " fresh at 100x100", display, rounding, 100f, 100f, 1, 2);
        assertFreshTreeOrders(label + " fresh at 140x120", display, rounding, 140f, 120f, 1, 2);
    }

    private static void assertFreshTreeOrders(String label, TaffyDisplay display, boolean rounding,
                                              float width, float height,
                                              int expectedFirst, int expectedSecond) {
        TaffyTree fresh = newTree(rounding);
        NodeId root = fresh.newWithChildren(container(display),
            fresh.newLeaf(visibleBox(10f, 10f)),
            fresh.newLeaf(hiddenBox()),
            fresh.newLeaf(hiddenBox()),
            fresh.newLeaf(visibleBox(12f, 12f)));
        NodeId firstHidden = fresh.getChildren(root).get(1);
        NodeId secondHidden = fresh.getChildren(root).get(2);
        fresh.computeLayout(root, layoutSize(width, height));
        assertOrders(label, fresh, firstHidden, secondHidden, expectedFirst, expectedSecond);
    }

    private static void assertOrders(String label, TaffyTree tree, NodeId firstHidden, NodeId secondHidden,
                                     int expectedFirst, int expectedSecond) {
        assertEquals(expectedFirst, tree.getLayout(firstHidden).order(), label + " first hidden child order");
        assertEquals(expectedSecond, tree.getLayout(secondHidden).order(), label + " second hidden child order");
        assertEquals(expectedFirst, tree.getUnroundedLayout(firstHidden).order(), label + " first hidden unrounded order");
        assertEquals(expectedSecond, tree.getUnroundedLayout(secondHidden).order(), label + " second hidden unrounded order");
    }

    @Test
    @DisplayName("hidden subtree descendants keep order 0 on every pass")
    void hiddenSubtreeDescendantsKeepZeroOrder() {
        TaffyTree tree = new TaffyTree();
        NodeId grandChildA = tree.newLeaf(visibleBox(4f, 4f));
        NodeId grandChildB = tree.newLeaf(visibleBox(5f, 5f));
        NodeId hiddenParent = tree.newWithChildren(hiddenBox(), grandChildA, grandChildB);
        NodeId root = tree.newWithChildren(container(TaffyDisplay.BLOCK),
            tree.newLeaf(visibleBox(10f, 10f)),
            hiddenParent);

        tree.computeLayout(root, layoutSize(100f, 100f));
        assertEquals(1, tree.getLayout(hiddenParent).order(), "hidden child index on the first pass");
        assertEquals(0, tree.getLayout(grandChildA).order(), "hidden grandchild A order on the first pass");
        assertEquals(0, tree.getLayout(grandChildB).order(), "hidden grandchild B order on the first pass");

        tree.computeLayout(root, layoutSize(140f, 120f));
        assertEquals(1, tree.getLayout(hiddenParent).order(), "hidden child index on the resized pass");
        assertEquals(0, tree.getLayout(grandChildA).order(), "hidden grandchild A order on the resized pass");
        assertEquals(0, tree.getLayout(grandChildB).order(), "hidden grandchild B order on the resized pass");
    }

    @Test
    @DisplayName("toggling display keeps the hidden child index and zeroes its descendants")
    void togglingHiddenResetsDescendantOrder() {
        TaffyTree tree = new TaffyTree();
        NodeId childA = tree.newLeaf(visibleBox(4f, 4f));
        NodeId childB = tree.newLeaf(visibleBox(5f, 5f));
        TaffyStyle panelStyle = visibleBox(60f, 60f);
        NodeId panel = tree.newWithChildren(panelStyle, childA, childB);
        NodeId root = tree.newWithChildren(container(TaffyDisplay.BLOCK),
            tree.newLeaf(visibleBox(10f, 10f)),
            panel);

        tree.computeLayout(root, layoutSize(100f, 100f));
        assertEquals(1, tree.getLayout(childB).order(), "visible child B index before hiding");

        tree.setStyle(panel, hiddenBox());
        tree.computeLayout(root, layoutSize(100f, 100f));
        assertEquals(1, tree.getLayout(panel).order(), "hidden panel index after hiding");
        assertEquals(0, tree.getLayout(childA).order(), "hidden descendant A order");
        assertEquals(0, tree.getLayout(childB).order(), "hidden descendant B order");
    }

    @Test
    @DisplayName("every container matches a fresh tree field by field after a resize")
    void resizedTreeMatchesFreshTreeForHiddenOrder() {
        for (TaffyDisplay display : CONTAINERS) {
            for (boolean rounding : new boolean[]{true, false}) {
                String label = display + (rounding ? " rounding-on" : " rounding-off");

                TaffyTree reused = newTree(rounding);
                NodeId root = reused.newWithChildren(container(display),
                    reused.newLeaf(visibleBox(10f, 10f)),
                    reused.newLeaf(hiddenBox()),
                    reused.newLeaf(hiddenBox()),
                    reused.newLeaf(visibleBox(12f, 12f)));
                reused.computeLayout(root, layoutSize(100f, 100f));
                reused.computeLayout(root, layoutSize(140f, 120f));

                TaffyTree fresh = newTree(rounding);
                NodeId freshRoot = fresh.newWithChildren(container(display),
                    fresh.newLeaf(visibleBox(10f, 10f)),
                    fresh.newLeaf(hiddenBox()),
                    fresh.newLeaf(hiddenBox()),
                    fresh.newLeaf(visibleBox(12f, 12f)));
                fresh.computeLayout(freshRoot, layoutSize(140f, 120f));

                assertEquals(fresh.getChildren(freshRoot).size(), reused.getChildren(root).size());
                for (int i = 0; i < fresh.getChildren(freshRoot).size(); i++) {
                    NodeId freshChild = fresh.getChildren(freshRoot).get(i);
                    NodeId reusedChild = reused.getChildren(root).get(i);
                    assertEquals(fresh.getLayout(freshChild).order(), reused.getLayout(reusedChild).order(),
                        label + " child " + i + " order");
                }
                assertEquals(fresh.getLayout(freshRoot).order(), reused.getLayout(root).order(),
                    label + " root order");
            }
        }
    }

    private static TaffyTree newTree(boolean rounding) {
        TaffyTree tree = new TaffyTree();
        if (!rounding) {
            tree.disableRounding();
        }
        return tree;
    }

    private static TaffySize<AvailableSpace> layoutSize(float width, float height) {
        return TaffySize.of(AvailableSpace.definite(width), AvailableSpace.definite(height));
    }

    private static TaffyStyle container(TaffyDisplay display) {
        TaffyStyle style = new TaffyStyle();
        style.display = display;
        style.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(100f));
        return style;
    }

    private static TaffyStyle visibleBox(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle hiddenBox() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.NONE;
        return style;
    }
}
