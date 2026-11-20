package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.Clear;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyFloat;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.util.BlockContext;
import dev.vfyjxf.taffy.util.FloatContext;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P19: a same-BFC block child reuses its final layout while its layout cannot depend on floats.
 *
 * <p>{@code BlockComputer} lays every in-flow {@code display: block} child out with an inherited
 * {@link BlockContext}, because that child shares the parent's block formatting context: its
 * floats are visible to later siblings and its own position avoids the parent's floats. The same
 * {@link LayoutInput} can therefore produce different results, which is why the child used to be
 * recomputed on every pass. The one case that is still a pure function of the input is a child
 * that inherits no floats and places none: with an empty float context the inherited position and
 * content-box insets are never consulted, so the result can be cached and reused
 * ({@code LayoutComputer#computeChildLayout(NodeId, LayoutInput, BlockContext)}).</p>
 *
 * <p>These tests pin both halves of that contract: the LDLib2-shaped form reuses 49 of its 50
 * panels (the rows of a reused panel receive no layout write at all, because a reused panel's own
 * commit does not run), and every float/context interaction - an inherited float, a float inside
 * the child, a box that establishes its own formatting context - falls back to the previous
 * always-recompute behaviour with identical values.</p>
 */
@DisplayName("P19: same-BFC block child layout reuse")
class SameBfcBlockLayoutCacheP19Test {

    private static final float EPSILON = 0.001f;
    private static final float WIDTH = 1920f;
    private static final float HEIGHT = 1080f;

    // === Fixtures ===

    /** A form-shaped tree and the panels the root commits, in order. */
    private record Form(TaffyTree tree, NodeId root, List<NodeId> panels) {

        NodeId panel(int index) {
            return panels.get(index);
        }

        NodeId row(int panel, int row) {
            return tree.getChildren(panel(panel)).get(row);
        }

        NodeId label(int panel, int row, int column) {
            return tree.getChildren(row(panel, row)).get(column);
        }

        List<NodeId> rows(int panel) {
            return tree.getChildren(panel(panel));
        }

        List<NodeId> labels(int panel) {
            List<NodeId> labels = new ArrayList<>();
            for (NodeId row : rows(panel)) {
                labels.addAll(tree.getChildren(row));
            }
            return labels;
        }
    }

    private static TaffySize<AvailableSpace> space(float width, float height) {
        return TaffySize.of(AvailableSpace.definite(width), AvailableSpace.definite(height));
    }

    /** A deterministic text measure: 6px per glyph, 9px line height, two lines maximum. */
    private static MeasureFunc glyphMeasure(int chars) {
        return (knownDimensions, availableSpace) -> {
            if (!Float.isNaN(knownDimensions.width) && !Float.isNaN(knownDimensions.height)) {
                return knownDimensions;
            }
            float constraint;
            if (!Float.isNaN(knownDimensions.width)) {
                constraint = knownDimensions.width;
            } else if (availableSpace.width == null) {
                constraint = Float.NaN;
            } else if (availableSpace.width.isMinContent()) {
                constraint = 0f;
            } else if (availableSpace.width.isDefinite()) {
                constraint = availableSpace.width.getValue();
            } else {
                constraint = Float.NaN;
            }
            float natural = chars * 6f;
            float width;
            int lines;
            if (Float.isNaN(constraint)) {
                width = natural;
                lines = 1;
            } else {
                width = Math.min(natural, constraint);
                lines = Math.min(2, Math.max(1, (int) Math.ceil(natural / Math.max(1f, constraint))));
            }
            return new FloatSize(
                Float.isNaN(knownDimensions.width) ? width : knownDimensions.width,
                Float.isNaN(knownDimensions.height) ? lines * 9f : knownDimensions.height);
        };
    }

    private static TaffyStyle blockPanelStyle() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        style.padding = TaffyRect.all(LengthPercentage.length(8f));
        style.border = TaffyRect.all(LengthPercentage.length(2f));
        return style;
    }

    private static TaffyStyle flexRowStyle() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.ROW;
        style.gap = new TaffySize<>(LengthPercentage.length(4f), LengthPercentage.length(4f));
        style.alignItems = AlignItems.CENTER;
        return style;
    }

    private static TaffyStyle labelStyle() {
        TaffyStyle style = new TaffyStyle();
        style.flexGrow = 1f;
        style.flexShrink = 1f;
        return style;
    }

    private static NodeId appendRow(TaffyTree tree, NodeId panel, int labelCount, int glyphSeed) {
        NodeId[] labels = new NodeId[labelCount];
        for (int c = 0; c < labelCount; c++) {
            labels[c] = tree.newLeafWithMeasure(labelStyle(), glyphMeasure(8 + Math.floorMod(glyphSeed + c * 37, 56)));
        }
        NodeId row = tree.newWithChildren(flexRowStyle(), labels);
        tree.addChild(panel, row);
        return row;
    }

    /** The LDLib2 form shape: a block root with block panels of flex rows of measured labels. */
    private static Form buildForm(int panelCount, int rowsPerPanel, int labelsPerRow) {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.BLOCK;
        rootStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.percent(1f));
        rootStyle.padding = TaffyRect.all(LengthPercentage.length(8f));

        List<NodeId> panels = new ArrayList<>(panelCount);
        for (int p = 0; p < panelCount; p++) {
            NodeId[] rows = new NodeId[rowsPerPanel];
            for (int r = 0; r < rowsPerPanel; r++) {
                NodeId[] labels = new NodeId[labelsPerRow];
                for (int c = 0; c < labelsPerRow; c++) {
                    labels[c] = tree.newLeafWithMeasure(
                        labelStyle(),
                        glyphMeasure(8 + Math.floorMod((p * rowsPerPanel + r) * labelsPerRow + c * 37, 56)));
                }
                rows[r] = tree.newWithChildren(flexRowStyle(), labels);
            }
            panels.add(tree.newWithChildren(blockPanelStyle(), rows));
        }
        NodeId root = tree.newWithChildren(rootStyle, panels.toArray(new NodeId[0]));
        return new Form(tree, root, panels);
    }

    /** Grows a label by 4px, which grows its row, its panel and every panel below it. */
    private static void makeTaller(TaffyTree tree, NodeId label) {
        TaffyStyle style = tree.getStyle(label).copy();
        style.padding = TaffyRect.all(LengthPercentage.length(2f));
        tree.setStyle(label, style);
    }

    private static NodeId floatedLeaf(TaffyTree tree, TaffyFloat direction, float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.float_ = direction;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return tree.newLeaf(style);
    }

    // === Assertions ===

    /** Every layout below {@code subtreeRoot} (not its own, which the parent may move) is unchanged. */
    private static void assertChildrenUnchanged(TaffyTree tree, NodeId subtreeRoot, Map<Long, Layout> before) {
        for (NodeId child : tree.getChildren(subtreeRoot)) {
            assertEquals(before.get(child.getId()), tree.getUnroundedLayout(child),
                "the stored layout of node " + child.getId() + " is unchanged");
            assertChildrenUnchanged(tree, child, before);
        }
    }

    private static Map<Long, Layout> snapshot(TaffyTree tree, NodeId root) {
        Map<Long, Layout> layouts = new HashMap<>();
        collectLayouts(tree, root, layouts);
        return layouts;
    }

    private static void collectLayouts(TaffyTree tree, NodeId node, Map<Long, Layout> layouts) {
        layouts.put(node.getId(), tree.getUnroundedLayout(node));
        for (NodeId child : tree.getChildren(node)) {
            collectLayouts(tree, child, layouts);
        }
    }

    /** Compares two trees structurally (same shape) layout by layout. */
    private static void assertSameLayouts(TaffyTree expected, NodeId expectedNode,
                                          TaffyTree actual, NodeId actualNode) {
        assertEquals(expected.getUnroundedLayout(expectedNode), actual.getUnroundedLayout(actualNode),
            "the unrounded layout of the node at this position is unchanged");
        List<NodeId> expectedChildren = expected.getChildren(expectedNode);
        List<NodeId> actualChildren = actual.getChildren(actualNode);
        assertEquals(expectedChildren.size(), actualChildren.size(),
            "the node at this position has the same number of children");
        for (int i = 0; i < expectedChildren.size(); i++) {
            assertSameLayouts(expected, expectedChildren.get(i), actual, actualChildren.get(i));
        }
    }

    private static void assertOutputEquals(LayoutOutput expected, LayoutOutput actual, String message) {
        assertEquals(expected.size(), actual.size(), message + ": size");
        assertEquals(expected.contentSize(), actual.contentSize(), message + ": contentSize");
        assertEquals(expected.scrollableOverflow(), actual.scrollableOverflow(),
            message + ": scrollableOverflow");
        assertEquals(expected.firstBaselines(), actual.firstBaselines(), message + ": firstBaselines");
        assertEquals(expected.baselines(), actual.baselines(), message + ": baselines");
        assertEquals(expected.topMargin().resolve(), actual.topMargin().resolve(), EPSILON,
            message + ": topMargin");
        assertEquals(expected.bottomMargin().resolve(), actual.bottomMargin().resolve(), EPSILON,
            message + ": bottomMargin");
        assertEquals(expected.marginsCanCollapseThrough(), actual.marginsCanCollapseThrough(),
            message + ": marginsCanCollapseThrough");
    }

    /** Records every layout write, with the number of writes per node. */
    private static final class Writes implements LayoutChangeListener {
        private final Map<Long, Integer> counts = new HashMap<>();

        @Override
        public void onLayoutChanged(NodeId node, Layout oldLayout, Layout newLayout) {
            counts.merge(node.getId(), 1, Integer::sum);
        }

        int count(NodeId node) {
            return counts.getOrDefault(node.getId(), 0);
        }

        void reset() {
            counts.clear();
        }
    }

    // === The reuse itself ===

    /**
     * The headline case: one label change rewrites the changed panel, while every other panel is
     * only committed by the root - no row and no label below it is written at all.
     */
    @Test
    @DisplayName("49 of 50 unchanged panels are reused: their rows receive no layout write")
    void unchangedPanelsAreReused() {
        Form form = buildForm(50, 2, 3);
        TaffyTree tree = form.tree();
        tree.computeLayout(form.root(), space(WIDTH, HEIGHT));

        Map<Long, Layout> before = snapshot(tree, form.root());
        float firstPanelHeight = tree.getUnroundedLayout(form.panel(0)).size().height;

        Writes writes = new Writes();
        tree.setLayoutChangeListener(writes);
        makeTaller(tree, form.label(0, 0, 1));
        tree.computeLayout(form.root(), space(WIDTH, HEIGHT));
        tree.setLayoutChangeListener(null);

        // The changed panel is laid out again: its rows and labels are written.
        for (NodeId row : form.rows(0)) {
            assertTrue(writes.count(row) >= 1, "the changed panel's rows are laid out again");
        }
        assertTrue(writes.count(form.label(0, 0, 1)) >= 1, "the changed label is laid out again");

        // Every other panel is committed by the root but not laid out again.
        for (int p = 1; p < form.panels().size(); p++) {
            NodeId panel = form.panel(p);
            assertTrue(writes.count(panel) >= 1, "the root still commits the panel's own layout");
            for (NodeId row : form.rows(p)) {
                assertEquals(0, writes.count(row),
                    "panel " + p + ": a reused panel does not lay its rows out again");
                for (NodeId label : tree.getChildren(row)) {
                    assertEquals(0, writes.count(label),
                        "panel " + p + ": a reused panel does not lay its labels out again");
                }
            }
            assertFalse(tree.isDirty(panel), "a reused panel stays clean");
            assertChildrenUnchanged(tree, panel, before);
        }

        // The values are the ones a full pass would have produced: the unchanged panels keep
        // their size and move down by exactly the height the changed panel gained.
        float growth = tree.getUnroundedLayout(form.panel(0)).size().height - firstPanelHeight;
        assertTrue(growth > 0f, "the changed panel grew");
        for (int p = 1; p < form.panels().size(); p++) {
            NodeId panel = form.panel(p);
            assertEquals(before.get(panel.getId()).size(), tree.getUnroundedLayout(panel).size(),
                "panel " + p + " keeps its size");
            assertEquals(before.get(panel.getId()).location().y + growth,
                tree.getUnroundedLayout(panel).location().y, EPSILON,
                "panel " + p + " moves down by the changed panel's growth");
        }
    }

    /**
     * The same form with the change inside a later panel: that panel and every panel after it are
     * laid out again (the cache key is per panel, and the later panels moved), while the earlier
     * ones are reused.
     */
    @Test
    @DisplayName("a changed panel is recomputed, the panels before it are reused")
    void changedPanelIsRecomputed() {
        Form form = buildForm(10, 2, 3);
        TaffyTree tree = form.tree();
        tree.computeLayout(form.root(), space(WIDTH, HEIGHT));

        Writes writes = new Writes();
        tree.setLayoutChangeListener(writes);
        makeTaller(tree, form.label(5, 1, 2));
        tree.computeLayout(form.root(), space(WIDTH, HEIGHT));
        tree.setLayoutChangeListener(null);

        for (NodeId row : form.rows(5)) {
            assertTrue(writes.count(row) >= 1, "the changed panel's rows are laid out again");
        }
        for (int p = 0; p < 5; p++) {
            for (NodeId row : form.rows(p)) {
                assertEquals(0, writes.count(row), "panel " + p + " is before the change and is reused");
            }
        }
    }

    /**
     * A reused panel must leave the tree in exactly the state a cold pass produces. The second
     * tree is built identically and has the same style change applied before its only pass.
     */
    @Test
    @DisplayName("a reused panel produces the same layout as a cold pass")
    void reuseMatchesAColdPass() {
        Form warm = buildForm(8, 3, 4);
        warm.tree().computeLayout(warm.root(), space(WIDTH, HEIGHT));
        Writes writes = new Writes();
        warm.tree().setLayoutChangeListener(writes);
        makeTaller(warm.tree(), warm.label(0, 0, 1));
        warm.tree().computeLayout(warm.root(), space(WIDTH, HEIGHT));
        warm.tree().setLayoutChangeListener(null);
        for (int p = 1; p < warm.panels().size(); p++) {
            for (NodeId row : warm.rows(p)) {
                assertEquals(0, writes.count(row), "panel " + p + " was reused");
            }
        }

        Form cold = buildForm(8, 3, 4);
        makeTaller(cold.tree(), cold.label(0, 0, 1));
        cold.tree().computeLayout(cold.root(), space(WIDTH, HEIGHT));

        assertSameLayouts(cold.tree(), cold.root(), warm.tree(), warm.root());
    }

    /** Changing the available space changes the key, so nothing is reused - and values still match. */
    @Test
    @DisplayName("an available-space change misses every panel and still matches a cold pass")
    void availableSpaceChangeMissesEveryPanel() {
        Form form = buildForm(6, 2, 3);
        TaffyTree tree = form.tree();
        tree.computeLayout(form.root(), space(WIDTH, HEIGHT));

        Writes writes = new Writes();
        tree.setLayoutChangeListener(writes);
        tree.computeLayout(form.root(), space(WIDTH - 200f, HEIGHT));
        tree.setLayoutChangeListener(null);

        for (int p = 0; p < form.panels().size(); p++) {
            for (NodeId row : form.rows(p)) {
                assertTrue(writes.count(row) >= 1,
                    "panel " + p + " is laid out again because its width changed");
            }
        }

        Form cold = buildForm(6, 2, 3);
        cold.tree().computeLayout(cold.root(), space(WIDTH - 200f, HEIGHT));
        assertSameLayouts(cold.tree(), cold.root(), tree, form.root());
    }

    /** A child list change clears the affected panel only. */
    @Test
    @DisplayName("a row added to one panel recomputes that panel only")
    void addedRowRecomputesOnlyThatPanel() {
        Form form = buildForm(6, 2, 3);
        TaffyTree tree = form.tree();
        tree.computeLayout(form.root(), space(WIDTH, HEIGHT));

        Writes writes = new Writes();
        tree.setLayoutChangeListener(writes);
        NodeId added = appendRow(tree, form.panel(2), 3, 11);
        tree.computeLayout(form.root(), space(WIDTH, HEIGHT));
        tree.setLayoutChangeListener(null);

        assertTrue(writes.count(added) >= 1, "the added row is laid out");
        assertTrue(writes.count(form.row(2, 0)) >= 1, "the changed panel is laid out again");
        for (int p : new int[]{0, 1, 3, 4, 5}) {
            for (NodeId row : form.rows(p)) {
                assertEquals(0, writes.count(row), "panel " + p + " is reused");
            }
        }

        Form cold = buildForm(6, 2, 3);
        appendRow(cold.tree(), cold.panel(2), 3, 11);
        cold.tree().computeLayout(cold.root(), space(WIDTH, HEIGHT));
        assertSameLayouts(cold.tree(), cold.root(), tree, form.root());
    }

    /** Removing a row clears the affected panel only. */
    @Test
    @DisplayName("a row removed from one panel recomputes that panel only")
    void removedRowRecomputesOnlyThatPanel() {
        Form form = buildForm(6, 3, 3);
        TaffyTree tree = form.tree();
        tree.computeLayout(form.root(), space(WIDTH, HEIGHT));

        Writes writes = new Writes();
        tree.setLayoutChangeListener(writes);
        tree.removeChild(form.panel(2), form.row(2, 1));
        tree.computeLayout(form.root(), space(WIDTH, HEIGHT));
        tree.setLayoutChangeListener(null);

        assertTrue(writes.count(form.row(2, 0)) >= 1, "the changed panel is laid out again");
        for (int p : new int[]{0, 1, 3, 4, 5}) {
            for (NodeId row : form.rows(p)) {
                assertEquals(0, writes.count(row), "panel " + p + " is reused");
            }
        }

        Form cold = buildForm(6, 3, 3);
        cold.tree().removeChild(cold.panel(2), cold.row(2, 1));
        cold.tree().computeLayout(cold.root(), space(WIDTH, HEIGHT));
        assertSameLayouts(cold.tree(), cold.root(), tree, form.root());
    }

    // === Float and formatting-context safety ===

    /** A float preceding the panels makes every panel's position float-dependent: no reuse. */
    @Test
    @DisplayName("an inherited float disables the reuse and keeps the values identical")
    void inheritedFloatDisablesReuse() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.BLOCK;
        rootStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.percent(1f));
        rootStyle.padding = TaffyRect.all(LengthPercentage.length(8f));

        NodeId floated = floatedLeaf(tree, TaffyFloat.LEFT, 300f, 200f);
        List<NodeId> panels = new ArrayList<>();
        for (int p = 0; p < 4; p++) {
            NodeId[] rows = new NodeId[2];
            for (int r = 0; r < 2; r++) {
                NodeId[] labels = new NodeId[3];
                for (int c = 0; c < 3; c++) {
                    labels[c] = tree.newLeafWithMeasure(labelStyle(), glyphMeasure(8 + p * 5 + r * 3 + c));
                }
                rows[r] = tree.newWithChildren(flexRowStyle(), labels);
            }
            panels.add(tree.newWithChildren(blockPanelStyle(), rows));
        }
        List<NodeId> children = new ArrayList<>();
        children.add(floated);
        children.addAll(panels);
        NodeId root = tree.newWithChildren(rootStyle, children.toArray(new NodeId[0]));
        tree.computeLayout(root, space(WIDTH, HEIGHT));

        Writes writes = new Writes();
        tree.setLayoutChangeListener(writes);
        makeTaller(tree, tree.getChildren(tree.getChildren(panels.get(0)).get(0)).get(1));
        tree.computeLayout(root, space(WIDTH, HEIGHT));
        tree.setLayoutChangeListener(null);

        for (int p = 1; p < panels.size(); p++) {
            for (NodeId row : tree.getChildren(panels.get(p))) {
                assertTrue(writes.count(row) >= 1,
                    "panel " + p + " is laid out again because the BFC holds a float");
            }
        }
        assertTrue(writes.count(floated) >= 1, "the float is placed again as well");

        // The same shape laid out once is the reference.
        TaffyTree cold = new TaffyTree();
        cold.disableRounding();
        NodeId coldFloated = floatedLeaf(cold, TaffyFloat.LEFT, 300f, 200f);
        List<NodeId> coldPanels = new ArrayList<>();
        for (int p = 0; p < 4; p++) {
            NodeId[] rows = new NodeId[2];
            for (int r = 0; r < 2; r++) {
                NodeId[] labels = new NodeId[3];
                for (int c = 0; c < 3; c++) {
                    labels[c] = cold.newLeafWithMeasure(labelStyle(), glyphMeasure(8 + p * 5 + r * 3 + c));
                }
                rows[r] = cold.newWithChildren(flexRowStyle(), labels);
            }
            coldPanels.add(cold.newWithChildren(blockPanelStyle(), rows));
        }
        makeTaller(cold, cold.getChildren(cold.getChildren(coldPanels.get(0)).get(0)).get(1));
        List<NodeId> coldChildren = new ArrayList<>();
        coldChildren.add(coldFloated);
        coldChildren.addAll(coldPanels);
        NodeId coldRoot = cold.newWithChildren(rootStyle, coldChildren.toArray(new NodeId[0]));
        cold.computeLayout(coldRoot, space(WIDTH, HEIGHT));
        assertSameLayouts(cold, coldRoot, tree, root);
    }

    /** A float placed inside one panel must not let that panel be reused, while others still are. */
    @Test
    @DisplayName("a panel that places its own float is recomputed, its float-free siblings are reused")
    void childFloatDisablesReuseForThatPanelOnly() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.BLOCK;
        rootStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.percent(1f));

        // panel 0: plain rows (the panel whose label changes)
        NodeId plainRow0 = appendRow(tree, tree.newWithChildren(blockPanelStyle(), new NodeId[0]), 3, 1);
        NodeId plainPanel = tree.getParent(plainRow0);
        // panel 1: plain rows, must be reused
        NodeId plainPanel1 = tree.newWithChildren(blockPanelStyle(), new NodeId[0]);
        NodeId plainRow1 = appendRow(tree, plainPanel1, 3, 2);
        // panel 2: contains a float, must never be reused
        NodeId floatPanel = tree.newWithChildren(blockPanelStyle(), new NodeId[0]);
        appendRow(tree, floatPanel, 3, 3);
        NodeId innerFloat = floatedLeaf(tree, TaffyFloat.LEFT, 40f, 15f);
        tree.addChild(floatPanel, innerFloat);

        NodeId root = tree.newWithChildren(rootStyle, plainPanel, plainPanel1, floatPanel);
        tree.computeLayout(root, space(WIDTH, HEIGHT));

        Writes writes = new Writes();
        tree.setLayoutChangeListener(writes);
        makeTaller(tree, tree.getChildren(plainRow0).get(0));
        tree.computeLayout(root, space(WIDTH, HEIGHT));
        tree.setLayoutChangeListener(null);

        assertTrue(writes.count(plainRow0) >= 1, "the changed panel is laid out again");
        assertEquals(0, writes.count(plainRow1),
            "a float-free sibling before the floating panel is reused");
        assertTrue(writes.count(tree.getChildren(floatPanel).get(0)) >= 1,
            "a panel whose subtree places a float is laid out again");
        assertTrue(writes.count(innerFloat) >= 1, "the inner float is placed again");

        // Reference: the same shape laid out once.
        TaffyTree cold = new TaffyTree();
        cold.disableRounding();
        NodeId coldPanel0 = cold.newWithChildren(blockPanelStyle(), new NodeId[0]);
        NodeId coldRow0 = appendRow(cold, coldPanel0, 3, 1);
        NodeId coldPanel1 = cold.newWithChildren(blockPanelStyle(), new NodeId[0]);
        NodeId coldRow1 = appendRow(cold, coldPanel1, 3, 2);
        NodeId coldFloatPanel = cold.newWithChildren(blockPanelStyle(), new NodeId[0]);
        appendRow(cold, coldFloatPanel, 3, 3);
        NodeId coldInnerFloat = floatedLeaf(cold, TaffyFloat.LEFT, 40f, 15f);
        cold.addChild(coldFloatPanel, coldInnerFloat);
        makeTaller(cold, cold.getChildren(coldRow0).get(0));
        NodeId coldRoot = cold.newWithChildren(rootStyle, coldPanel0, coldPanel1, coldFloatPanel);
        assertEquals(3, cold.getChildren(coldRoot).size());
        cold.computeLayout(coldRoot, space(WIDTH, HEIGHT));
        assertSameLayouts(cold, coldRoot, tree, root);
    }

    /** A flow-root child establishes its own BFC; it must not take the context-free path. */
    @Test
    @DisplayName("a flow-root child is not marked as context-free")
    void flowRootChildIsNotMarked() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle panelStyle = blockPanelStyle();
        panelStyle.display = TaffyDisplay.FLOW_ROOT;
        panelStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.AUTO);
        NodeId leaf = tree.newLeaf(fixedLeafStyle(100f, 40f));
        NodeId panel = tree.newWithChildren(panelStyle, leaf);
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.BLOCK;
        tree.newWithChildren(rootStyle, panel);

        LayoutComputer computer = new LayoutComputer(tree, null);
        LayoutInput inputs = childInputs();
        BlockContext context = parentContext().subContext(0f, 0f, 500f);

        computer.computeChildLayout(panel, inputs, context);

        assertNull(tree.getContextFreeCacheEntry(panel, inputs, TaffyDirection.LTR),
            "a flow-root child is laid out as its own BFC and is not reused through the inherited context");
        assertNotNull(tree.getCacheEntry(panel, inputs),
            "it keeps the previous always-recompute-then-store behaviour");
    }

    /** A measurement is never stored as a reusable final layout. */
    @Test
    @DisplayName("a COMPUTE_SIZE request is not marked as context-free")
    void measureRequestIsNotMarked() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle panelStyle = blockPanelStyle();
        panelStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.AUTO);
        NodeId panel = tree.newWithChildren(panelStyle, tree.newLeaf(fixedLeafStyle(100f, 40f)));
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.BLOCK;
        tree.newWithChildren(rootStyle, panel);

        LayoutComputer computer = new LayoutComputer(tree, null);
        LayoutInput measure = new LayoutInput(
            RunMode.COMPUTE_SIZE,
            SizingMode.INHERENT_SIZE,
            RequestedAxis.BOTH,
            new FloatSize(Float.NaN, Float.NaN),
            new FloatSize(800f, 600f),
            space(800f, 600f),
            TaffyLine.TRUE);

        computer.computeChildLayout(panel, measure, parentContext().subContext(0f, 0f, 500f));

        assertNull(tree.getContextFreeCacheEntry(panel, measure, TaffyDirection.LTR),
            "a measure result is never reused as a final layout");
    }

    /** An inherited context that already holds a float rejects the lookup. */
    @Test
    @DisplayName("an inherited float context rejects the reuse on the direct entry point")
    void inheritedFloatContextRejectsTheLookup() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle panelStyle = blockPanelStyle();
        panelStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.AUTO);
        NodeId panel = tree.newWithChildren(panelStyle, tree.newLeaf(fixedLeafStyle(100f, 40f)));
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.BLOCK;
        tree.newWithChildren(rootStyle, panel);

        LayoutComputer computer = new LayoutComputer(tree, null);
        LayoutInput inputs = childInputs();
        BlockContext parent = parentContext();
        parent.placeFloatedBox(50f, 20f, 0f, FloatContext.Direction.LEFT, Clear.NONE);
        assertTrue(parent.hasFloats(), "the inherited context holds a float");

        Writes first = new Writes();
        tree.setLayoutChangeListener(first);
        computer.computeChildLayout(panel, inputs, parent.subContext(0f, 0f, 500f));
        tree.setLayoutChangeListener(null);
        assertTrue(first.count(tree.getChildren(panel).get(0)) >= 1, "the first call lays the child out");

        assertNull(tree.getContextFreeCacheEntry(panel, inputs, TaffyDirection.LTR),
            "a result computed beside an inherited float is not reusable");

        Writes second = new Writes();
        tree.setLayoutChangeListener(second);
        computer.computeChildLayout(panel, inputs, parent.subContext(0f, 0f, 500f));
        tree.setLayoutChangeListener(null);
        assertTrue(second.count(tree.getChildren(panel).get(0)) >= 1,
            "the next call is recomputed instead of being served from the cache");
    }

    /**
     * {@code direction: inherit} is resolved from the ancestors and is not part of the
     * {@link LayoutInput} key: an ancestor's direction change does not dirty the child, so the
     * context-free entry must remember the direction it was computed with.
     */
    @Test
    @DisplayName("an ancestor direction change invalidates the reused panels")
    void ancestorDirectionChangeInvalidatesReuse() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.BLOCK;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.AUTO);

        TaffyStyle absoluteStyle = new TaffyStyle();
        absoluteStyle.position = TaffyPosition.ABSOLUTE;
        absoluteStyle.size = new TaffySize<>(TaffyDimension.length(10f), TaffyDimension.length(10f));
        absoluteStyle.justifySelf = AlignItems.SELF_START;
        NodeId absolute = tree.newLeaf(absoluteStyle);
        NodeId row = appendRow(tree, tree.newWithChildren(blockPanelStyle(), new NodeId[]{absolute}), 3, 5);
        NodeId panel = tree.getParent(row);
        // A second panel takes the change in the "direction is stable" pass, so the first one
        // stays clean and can be reused.
        NodeId otherRow = appendRow(tree, tree.newWithChildren(blockPanelStyle(), new NodeId[0]), 3, 9);
        NodeId otherPanel = tree.getParent(otherRow);
        NodeId root = tree.newWithChildren(rootStyle, panel, otherPanel);

        tree.computeLayout(root, space(300f, 600f));
        assertEquals(10f, tree.getUnroundedLayout(absolute).location().x, EPSILON,
            "self-start resolves to the content-box left edge in LTR (8px padding + 2px border)");

        // The first panel is reused while the direction stays the same.
        Writes stableWrites = new Writes();
        tree.setLayoutChangeListener(stableWrites);
        makeTaller(tree, tree.getChildren(otherRow).get(0));
        tree.computeLayout(root, space(300f, 600f));
        tree.setLayoutChangeListener(null);
        assertEquals(0, stableWrites.count(row), "the panel is reused while the direction is stable");

        TaffyStyle rtl = tree.getStyle(root).copy();
        rtl.direction = TaffyDirection.RTL;
        tree.setStyle(root, rtl);
        Writes rtlWrites = new Writes();
        tree.setLayoutChangeListener(rtlWrites);
        tree.computeLayout(root, space(300f, 600f));
        tree.setLayoutChangeListener(null);

        assertTrue(rtlWrites.count(row) >= 1,
            "the panel is laid out again because its inherited direction changed");

        TaffyTree cold = new TaffyTree();
        cold.disableRounding();
        NodeId coldAbsolute = cold.newLeaf(absoluteStyle);
        NodeId coldRow = appendRow(cold, cold.newWithChildren(blockPanelStyle(), new NodeId[]{coldAbsolute}), 3, 5);
        NodeId coldOtherRow = appendRow(cold, cold.newWithChildren(blockPanelStyle(), new NodeId[0]), 3, 9);
        makeTaller(cold, cold.getChildren(coldOtherRow).get(0));
        TaffyStyle coldRtl = rootStyle.copy();
        coldRtl.direction = TaffyDirection.RTL;
        NodeId coldRoot = cold.newWithChildren(coldRtl, cold.getParent(coldRow), cold.getParent(coldOtherRow));
        cold.computeLayout(coldRoot, space(300f, 600f));

        assertTrue(cold.getUnroundedLayout(coldAbsolute).location().x > 10f,
            "the RTL reference places the out-of-flow box away from the left content edge");
        assertEquals(cold.getUnroundedLayout(coldAbsolute).location().x,
            tree.getUnroundedLayout(absolute).location().x, EPSILON,
            "the recomputed panel positions the out-of-flow box for the new direction");
    }

    // === Context and state invariants ===

    /**
     * A hit and a miss must leave the parent's {@link BlockContext} - and the shared
     * {@link FloatContext} - exactly as they were: the child layout gets its own sub-context.
     */
    @Test
    @DisplayName("a hit and a miss leave the inherited block context unchanged")
    void hitAndMissLeaveTheInheritedContextUnchanged() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle panelStyle = blockPanelStyle();
        panelStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.AUTO);
        NodeId leaf = tree.newLeaf(fixedLeafStyle(100f, 40f));
        NodeId panel = tree.newWithChildren(panelStyle, leaf);
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.BLOCK;
        tree.newWithChildren(rootStyle, panel);

        LayoutComputer computer = new LayoutComputer(tree, null);
        LayoutInput inputs = childInputs();
        BlockContext parent = parentContext();
        ContextFields parentBefore = ContextFields.of(parent);

        Writes missWrites = new Writes();
        tree.setLayoutChangeListener(missWrites);
        BlockContext missContext = parent.subContext(7f, 5f, 495f);
        LayoutOutput miss = computer.computeChildLayout(panel, inputs, missContext);
        tree.setLayoutChangeListener(null);

        assertTrue(missWrites.count(leaf) >= 1, "the first call is a miss and lays the child out");
        assertEquals(parentBefore, ContextFields.of(parent),
            "a miss leaves the parent context unchanged");
        assertEquals(7f, missContext.yOffset(), EPSILON, "the sub-context keeps its offset");
        assertEquals(5f, missContext.leftInset(), EPSILON, "the sub-context keeps its left inset");
        assertEquals(495f, missContext.rightInset(), EPSILON, "the sub-context keeps its right inset");
        assertFalse(missContext.hasFloats(), "the child placed no floats");

        Writes hitWrites = new Writes();
        tree.setLayoutChangeListener(hitWrites);
        BlockContext hitContext = parent.subContext(123f, 5f, 495f);
        LayoutOutput hit = computer.computeChildLayout(panel, inputs, hitContext);
        tree.setLayoutChangeListener(null);

        assertEquals(0, hitWrites.count(leaf), "a hit does not lay the child out again");
        assertOutputEquals(miss, hit, "the hit returns exactly the miss's output");
        assertEquals(parentBefore, ContextFields.of(parent),
            "a hit leaves the parent context unchanged");
        assertEquals(123f, hitContext.yOffset(), EPSILON);
        assertEquals(5f, hitContext.leftInset(), EPSILON);
        assertEquals(495f, hitContext.rightInset(), EPSILON);
        // The child's own content-box refinement (padding 5 on both sides) is part of the layout
        // that a hit reproduces, not of the caller's context: a miss refined the sub-context it
        // was given, a hit does not touch the one it was given. Nothing reads either value after
        // the child's layout, so the two entries are interchangeable.
        assertEquals(15f, missContext.contentBoxLeftInset(), EPSILON,
            "a miss refines the child's own sub-context content box (5 inset + 8 padding + 2 border)");
        assertEquals(5f, hitContext.contentBoxLeftInset(), EPSILON,
            "a hit does not refine the passed sub-context");
        assertFalse(hitContext.hasFloats());
        assertFalse(hitContext.hasFloatedContentContribution());
    }

    /** Margin collapsing, abspos children and percentage sizes keep working through the reuse. */
    @Test
    @DisplayName("margin collapsing, abspos and percentage children survive the reuse")
    void marginCollapseAbsPosAndPercentageSurviveReuse() {
        Form form = buildForm(4, 2, 3);
        TaffyTree tree = form.tree();
        // Collapsing vertical margins between panels.
        for (NodeId panel : form.panels()) {
            TaffyStyle style = tree.getStyle(panel).copy();
            style.margin = new TaffyRect<>(
                LengthPercentageAuto.length(10f), LengthPercentageAuto.length(0f),
                LengthPercentageAuto.length(10f), LengthPercentageAuto.length(0f));
            tree.setStyle(panel, style);
        }
        // A positioned panel with an absolutely positioned child, and a percentage-sized child.
        TaffyStyle positioned = tree.getStyle(form.panel(1)).copy();
        positioned.position = TaffyPosition.RELATIVE;
        tree.setStyle(form.panel(1), positioned);
        TaffyStyle absolute = new TaffyStyle();
        absolute.position = TaffyPosition.ABSOLUTE;
        absolute.size = new TaffySize<>(TaffyDimension.length(60f), TaffyDimension.length(20f));
        absolute.inset = new TaffyRect<>(
            LengthPercentageAuto.length(4f), LengthPercentageAuto.AUTO,
            LengthPercentageAuto.length(6f), LengthPercentageAuto.AUTO);
        NodeId absoluteChild = tree.newLeaf(absolute);
        tree.addChild(form.panel(1), absoluteChild);

        TaffyStyle percentagePanel = tree.getStyle(form.panel(2)).copy();
        percentagePanel.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(200f));
        tree.setStyle(form.panel(2), percentagePanel);
        TaffyStyle percentageChild = fixedLeafStyle(0f, 0f);
        percentageChild.size = new TaffySize<>(TaffyDimension.percent(0.5f), TaffyDimension.percent(0.5f));
        NodeId percentageNode = tree.newLeaf(percentageChild);
        tree.addChild(form.panel(2), percentageNode);
        tree.computeLayout(form.root(), space(WIDTH, HEIGHT));

        Writes writes = new Writes();
        tree.setLayoutChangeListener(writes);
        makeTaller(tree, form.label(0, 0, 1));
        tree.computeLayout(form.root(), space(WIDTH, HEIGHT));
        tree.setLayoutChangeListener(null);

        for (int p = 1; p < form.panels().size(); p++) {
            for (NodeId row : form.rows(p)) {
                assertEquals(0, writes.count(row), "panel " + p + " is reused");
            }
        }
        assertEquals(0, writes.count(absoluteChild), "the reused panel does not relayout its abspos child");
        assertEquals(0, writes.count(percentageNode), "the reused panel does not relayout its percentage child");
        assertTrue(tree.getUnroundedLayout(percentageNode).size().width > 0f,
            "the percentage child is stretched/percentage-sized");
        assertEquals(90f, tree.getUnroundedLayout(percentageNode).size().height, EPSILON,
            "height: 50% resolves against the 180px content box of the 200px panel");

        // Reference: the same shape laid out once.
        Form cold = buildForm(4, 2, 3);
        TaffyTree coldTree = cold.tree();
        for (NodeId panel : cold.panels()) {
            TaffyStyle style = coldTree.getStyle(panel).copy();
            style.margin = new TaffyRect<>(
                LengthPercentageAuto.length(10f), LengthPercentageAuto.length(0f),
                LengthPercentageAuto.length(10f), LengthPercentageAuto.length(0f));
            coldTree.setStyle(panel, style);
        }
        TaffyStyle coldPositioned = coldTree.getStyle(cold.panel(1)).copy();
        coldPositioned.position = TaffyPosition.RELATIVE;
        coldTree.setStyle(cold.panel(1), coldPositioned);
        coldTree.addChild(cold.panel(1), coldTree.newLeaf(absolute));
        TaffyStyle coldPercentagePanel = coldTree.getStyle(cold.panel(2)).copy();
        coldPercentagePanel.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(200f));
        coldTree.setStyle(cold.panel(2), coldPercentagePanel);
        coldTree.addChild(cold.panel(2), coldTree.newLeaf(percentageChild));
        makeTaller(coldTree, cold.label(0, 0, 1));
        coldTree.computeLayout(cold.root(), space(WIDTH, HEIGHT));

        assertSameLayouts(coldTree, cold.root(), tree, form.root());
    }

    // === Helpers for the direct LayoutComputer entry point ===

    private static TaffyStyle fixedLeafStyle(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static LayoutInput childInputs() {
        return new LayoutInput(
            RunMode.PERFORM_LAYOUT,
            SizingMode.INHERENT_SIZE,
            RequestedAxis.BOTH,
            new FloatSize(300f, Float.NaN),
            new FloatSize(800f, 600f),
            space(800f, 600f),
            TaffyLine.TRUE);
    }

    private static BlockContext parentContext() {
        BlockContext context = BlockContext.root(new FloatContext());
        context.setWidth(800f);
        context.applyContentBoxInset(0f, 0f);
        return context;
    }

    /** The mutable state of a {@link BlockContext} that a child layout must not change. */
    private record ContextFields(
        float yOffset,
        float leftInset,
        float rightInset,
        float contentBoxLeftInset,
        float contentBoxRightInset,
        boolean hasFloats,
        boolean hasFloatedContentContribution,
        float floatedContentHeightContribution) {

        static ContextFields of(BlockContext context) {
            return new ContextFields(
                context.yOffset(),
                context.leftInset(),
                context.rightInset(),
                context.contentBoxLeftInset(),
                context.contentBoxRightInset(),
                context.hasFloats(),
                context.hasFloatedContentContribution(),
                context.floatedContentHeightContribution());
        }
    }
}
