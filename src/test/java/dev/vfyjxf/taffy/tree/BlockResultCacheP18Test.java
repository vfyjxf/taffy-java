package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P18 experiment: a block container cannot cache its whole compute result under the key the
 * experiment proposed ({@link LayoutInput} + container/children versions + each direct child's
 * layout version).
 *
 * <p><b>The cache is not implemented and not enabled.</b> These tests are the reproduction
 * deliverable of the rejected experiment: they pin the two observable facts that make the
 * proposed key unusable, so that a later attempt does not have to rediscover them.</p>
 *
 * <ol>
 *   <li>An unchanged layout request is already answered by the tree-clean fast path
 *       ({@code TaffyTree#computeLayoutInternal}/{@code tryReuseLastLayout}), so a container
 *       result cache adds nothing to the steady state.</li>
 *   <li>A container that is still recomputed has had every direct child's stored layout rewritten
 *       by the previous pass (a parent pass commits a {@link Layout} for every in-flow child),
 *       so a key that compares per-child layout versions never matches a retained entry. The
 *       reverse case - the container recomputes while every direct child layout is unchanged -
 *       exists only for same-BFC block children, which bypass the layout cache by design
 *       ({@code LayoutComputer#computeChildLayout(NodeId, LayoutInput, BlockContext)}); the
 *       measurement in the experiment report shows 49 of 50 form panels in that state per frame,
 *       with an unchanged value for every child.</li>
 * </ol>
 *
 * <p>The write counts below are the direct evidence: one label style change, every panel and
 * every panel's rows receive a layout write, while the untouched panels' written values are
 * identical to the previous pass. A version key therefore misses, and a value key would only be
 * sound together with a dirty check - which is exactly the follow-up recorded in the report.</p>
 */
@DisplayName("P18: block container result cache (experiment, not enabled)")
class BlockResultCacheP18Test {

    private static final float EPSILON = 0.001f;
    private static final int PANELS = 4;
    private static final int ROWS = 6;
    private static final int LABELS = 3;
    private static final float WIDTH = 1920f;
    private static final float HEIGHT = 1080f;

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

    private static TaffySize<AvailableSpace> space(float width, float height) {
        return TaffySize.of(AvailableSpace.definite(width), AvailableSpace.definite(height));
    }

    private record Shape(TaffyTree tree, NodeId root, NodeId hoveredLabel,
                         TaffyStyle plainStyle, TaffyStyle alternateStyle) {}

    /** The LDLib2 form shape, shrunk to {@link #PANELS} panels of {@link #ROWS} rows. */
    private static Shape buildForm() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.BLOCK;
        rootStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.percent(1f));
        rootStyle.padding = TaffyRect.all(LengthPercentage.length(8f));

        NodeId[][] panelRows = new NodeId[PANELS][ROWS];
        NodeId hovered = null;
        TaffyStyle plain = null;
        TaffyStyle alternate = null;
        NodeId[] panelNodes = new NodeId[PANELS];
        for (int p = 0; p < PANELS; p++) {
            TaffyStyle panelStyle = new TaffyStyle();
            panelStyle.display = TaffyDisplay.BLOCK;
            panelStyle.padding = TaffyRect.all(LengthPercentage.length(8f));
            panelStyle.border = TaffyRect.all(LengthPercentage.length(2f));
            for (int r = 0; r < ROWS; r++) {
                TaffyStyle rowStyle = new TaffyStyle();
                rowStyle.display = TaffyDisplay.FLEX;
                rowStyle.flexDirection = FlexDirection.ROW;
                rowStyle.gap = new TaffySize<>(LengthPercentage.length(4f), LengthPercentage.length(4f));
                rowStyle.alignItems = AlignItems.CENTER;
                NodeId[] labels = new NodeId[LABELS];
                for (int c = 0; c < LABELS; c++) {
                    TaffyStyle labelStyle = new TaffyStyle();
                    labelStyle.flexGrow = 1f;
                    labelStyle.flexShrink = 1f;
                    labelStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
                    labels[c] = tree.newLeafWithMeasure(
                        labelStyle, glyphMeasure(8 + Math.floorMod((p * ROWS + r) * LABELS + c * 37, 56)));
                    if (p == 0 && r == 0 && c == 1) {
                        hovered = labels[c];
                        plain = labelStyle;
                        alternate = labelStyle.copy();
                        alternate.padding = TaffyRect.all(LengthPercentage.length(2f));
                    }
                }
                panelRows[p][r] = tree.newWithChildren(rowStyle, labels);
            }
            panelNodes[p] = tree.newWithChildren(panelStyle, panelRows[p]);
        }
        NodeId root = tree.newWithChildren(rootStyle, panelNodes);
        return new Shape(tree, root, hovered, plain, alternate);
    }

    /**
     * A single label style change rewrites every panel's and every row's stored layout, even
     * though the untouched panels' values are unchanged.
     *
     * <p>This is the exact state a container result cache would have to exploit - and precisely
     * the state in which the proposed child-layout-version key cannot match, because the
     * previous pass already rewrote those layouts. Note that the untouched panels were never
     * dirty: they are recomputed only because they are same-BFC block children of the root.</p>
     */
    @Test
    void singleLeafStyleChangeRewritesEveryContainerChildLayout() {
        Shape shape = buildForm();
        TaffyTree tree = shape.tree();
        tree.computeLayout(shape.root(), space(WIDTH, HEIGHT));

        NodeId untouchedPanel = tree.getChildren(shape.root()).get(2);
        NodeId untouchedRow = tree.getChildren(untouchedPanel).get(3);
        assertFalse(tree.isDirty(untouchedPanel), "an untouched panel is not dirty");
        Layout untouchedPanelBefore = tree.getLayout(untouchedPanel);
        Layout untouchedRowBefore = tree.getLayout(untouchedRow);

        Writes writes = new Writes();
        tree.setLayoutChangeListener(writes);
        tree.setStyle(shape.hoveredLabel(), shape.alternateStyle());
        assertFalse(tree.isDirty(untouchedPanel), "the toggle does not dirty an untouched panel");
        tree.computeLayout(shape.root(), space(WIDTH, HEIGHT));
        tree.setLayoutChangeListener(null);

        // Every panel is written by the root's commit, and every panel's rows are written by the
        // panel's own commit: the row layouts of untouched panels are rewritten with the same
        // values, which is what a version-keyed result cache would miss.
        for (NodeId panel : tree.getChildren(shape.root())) {
            assertTrue(writes.count(panel) >= 1, "every panel receives a layout write");
            for (NodeId row : tree.getChildren(panel)) {
                assertTrue(writes.count(row) >= 1, "every row receives a layout write");
            }
        }
        // The untouched panel keeps its size; only the shift of the preceding, taller panel moves
        // it down. Everything below it is parent-relative and therefore value-identical.
        assertEquals(untouchedPanelBefore.size(), tree.getLayout(untouchedPanel).size(),
            "the untouched panel's written size is unchanged");
        assertEquals(untouchedPanelBefore.location().y + 4f,
            tree.getLayout(untouchedPanel).location().y, EPSILON,
            "the untouched panel only moves down by the taller first panel");
        assertEquals(untouchedRowBefore, tree.getLayout(untouchedRow),
            "the untouched row's written layout is value-identical");
        assertTrue(writes.allEqualForSubtree(tree, untouchedRow),
            "every write below an untouched row carries the previous values");

        // The change itself is real: the padded label is 4px taller, so its row and panel grow.
        assertEquals(13f, tree.getLayout(shape.hoveredLabel()).size().height, EPSILON);
        assertTrue(writes.count(shape.hoveredLabel()) >= 1, "the changed label is written");
    }

    /**
     * A style write that changes no value (the same style instance is stored again) also rewrites
     * every container's children: a version key cannot distinguish it from a real change.
     */
    @Test
    void noOpStyleWriteStillRewritesEveryContainer() {
        Shape shape = buildForm();
        TaffyTree tree = shape.tree();
        tree.computeLayout(shape.root(), space(WIDTH, HEIGHT));
        long passesBefore = tree.layoutPassCount();

        Writes writes = new Writes();
        tree.setLayoutChangeListener(writes);
        tree.setStyle(shape.hoveredLabel(), shape.plainStyle());
        tree.computeLayout(shape.root(), space(WIDTH, HEIGHT));
        tree.setLayoutChangeListener(null);

        assertEquals(passesBefore + 1, tree.layoutPassCount(), "the write forces a real pass");
        for (NodeId panel : tree.getChildren(shape.root())) {
            assertTrue(writes.count(panel) >= 1, "every panel is rewritten");
            for (NodeId row : tree.getChildren(panel)) {
                assertTrue(writes.count(row) >= 1, "every row is rewritten");
            }
        }
        assertTrue(writes.allEqualForSubtree(tree, shape.root()),
            "a no-op write rewrites the whole tree with unchanged values");
    }

    /**
     * The steady state needs no container result cache: an unchanged request is already answered
     * without a layout pass, and the stored {@link Layout} instances are kept.
     */
    @Test
    void unchangedRequestIsAlreadyAnsweredWithoutAPass() {
        Shape shape = buildForm();
        TaffyTree tree = shape.tree();
        tree.computeLayout(shape.root(), space(WIDTH, HEIGHT));
        Layout rootLayout = tree.getLayout(shape.root());
        Layout panelLayout = tree.getLayout(tree.getChildren(shape.root()).get(1));
        long passes = tree.layoutPassCount();

        tree.computeLayout(shape.root(), space(WIDTH, HEIGHT));

        assertEquals(passes, tree.layoutPassCount(), "no layout pass ran");
        assertSame(rootLayout, tree.getLayout(shape.root()), "the stored root layout is reused");
        assertSame(panelLayout, tree.getLayout(tree.getChildren(shape.root()).get(1)),
            "the stored panel layout is reused");
    }

    /**
     * The resize-with-fixed-children case named in the experiment plan: the panel recomputes
     * because the root's content width changed, every leaf keeps its layout, and the leaves are
     * still rewritten - so the version key misses even though nothing changed in value.
     */
    @Test
    void resizeWithFixedChildrenRewritesUnchangedLeafLayouts() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.BLOCK;
        rootStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.percent(1f));
        rootStyle.padding = TaffyRect.all(LengthPercentage.length(8f));
        TaffyStyle panelStyle = new TaffyStyle();
        panelStyle.display = TaffyDisplay.BLOCK;
        panelStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.AUTO);
        panelStyle.padding = TaffyRect.all(LengthPercentage.length(4f));
        NodeId[] leaves = new NodeId[5];
        for (int i = 0; i < leaves.length; i++) {
            TaffyStyle leafStyle = new TaffyStyle();
            leafStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(10f));
            leaves[i] = tree.newLeaf(leafStyle);
        }
        NodeId panel = tree.newWithChildren(panelStyle, leaves);
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{panel});
        tree.computeLayout(root, space(WIDTH, HEIGHT));
        Layout panelBefore = tree.getLayout(panel);
        Layout leafBefore = tree.getLayout(leaves[0]);

        Writes writes = new Writes();
        tree.setLayoutChangeListener(writes);
        tree.computeLayout(root, space(WIDTH - 320f, HEIGHT));
        tree.setLayoutChangeListener(null);

        assertTrue(writes.count(panel) >= 1, "the panel recomputes on the resize");
        for (NodeId leaf : leaves) {
            assertTrue(writes.count(leaf) >= 1, "every fixed leaf is rewritten");
        }
        assertEquals(panelBefore, tree.getLayout(panel), "the panel's layout is unchanged");
        assertEquals(leafBefore, tree.getLayout(leaves[0]), "the leaf's layout is unchanged");
        assertTrue(writes.allEqualForSubtree(tree, panel),
            "every rewritten layout below the panel carries unchanged values");
    }

    /**
     * The only state in which a sound result-cache key could match - a cleared container with
     * warm ancestors and unchanged children - is unreachable: the ancestors' caches answer the
     * request before the cleared container is visited.
     */
    @Test
    void clearedContainerIsNeverVisitedWhileItsAncestorsStayWarm() {
        Shape shape = buildForm();
        TaffyTree tree = shape.tree();
        tree.computeLayout(shape.root(), space(WIDTH, HEIGHT));
        long passesBefore = tree.layoutPassCount();
        NodeId panel = tree.getChildren(shape.root()).get(1);

        tree.clearCache(panel);
        Writes writes = new Writes();
        tree.setLayoutChangeListener(writes);
        tree.computeLayout(shape.root(), space(WIDTH, HEIGHT));
        tree.setLayoutChangeListener(null);

        assertEquals(passesBefore + 1, tree.layoutPassCount(),
            "the listener disables the tree-clean fast path, so the entry point runs");
        assertEquals(0, writes.count(panel), "the cleared panel is never visited");
        for (NodeId row : tree.getChildren(panel)) {
            assertEquals(0, writes.count(row), "its rows are never visited either");
        }
    }

    /** Records every layout write, with the number of writes per node and the written values. */
    private static final class Writes implements LayoutChangeListener {
        private final Map<Long, Integer> counts = new HashMap<>();
        private final Map<Long, List<Layout[]>> values = new HashMap<>();

        @Override
        public void onLayoutChanged(NodeId node, Layout oldLayout, Layout newLayout) {
            counts.merge(node.getId(), 1, Integer::sum);
            values.computeIfAbsent(node.getId(), key -> new ArrayList<>())
                .add(new Layout[]{oldLayout, newLayout});
        }

        int count(NodeId node) {
            return counts.getOrDefault(node.getId(), 0);
        }

        /** Whether every write to {@code subtreeRoot} and below repeated the previous values. */
        boolean allEqualForSubtree(TaffyTree tree, NodeId subtreeRoot) {
            if (!allEqual(subtreeRoot)) {
                return false;
            }
            for (NodeId child : tree.getChildren(subtreeRoot)) {
                if (!allEqualForSubtree(tree, child)) {
                    return false;
                }
            }
            return true;
        }

        private boolean allEqual(NodeId node) {
            List<Layout[]> writes = values.get(node.getId());
            if (writes == null) {
                return true;
            }
            for (Layout[] write : writes) {
                if (write[0] == null || write[1] == null || !write[0].equals(write[1])) {
                    return false;
                }
            }
            return true;
        }
    }
}
