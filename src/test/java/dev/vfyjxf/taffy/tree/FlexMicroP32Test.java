package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * P32 tests for the flex per-item hot path.
 *
 * <p>The p32 changes touch three properties of a flex item list that are easy to break without a
 * visible failure in a single-pass layout:</p>
 *
 * <ul>
 *   <li>the list is built once and then walked by index by every sizing stage, so a stage may no
 *       longer rely on an iterator's fail-fast behaviour - two stages must still see the same
 *       items in the same order;</li>
 *   <li>style-derived values (padding/border sums, box-sizing adjustment, aspect-ratio
 *       transferred min/max sizes, axis-oriented style dimensions, flex-basis, the 4.5 specified
 *       size suggestion and the scrollbar gutter) are resolved when the item is built and read
 *       from the item afterwards, so a rebuild must happen whenever one of them can change;</li>
 *   <li>the four per-pass sizes of an item (hypothetical and target sizes) are owned by the item
 *       and written in place, so a reused list must be reset to zero before it is handed out
 *       again.</li>
 * </ul>
 *
 * <p>Every scenario therefore runs twice: on a tree whose flex item lists are reused across
 * passes (the warm tree, relaid out repeatedly with {@code invalidateLayoutReuse}) and on a
 * freshly built tree, and every node field of every node is compared after each pass. The
 * assertions on concrete geometry (<em>not</em> just fresh-vs-warm equality) pin the flex sizing
 * results themselves, so a change in how free space, wrapping, auto margins or percentages are
 * resolved cannot pass unnoticed.</p>
 */
@DisplayName("P32: flex per-item hot path")
public class FlexMicroP32Test {

    private static final float EPSILON = 0.001f;

    /** A scenario builds the same node tree into a tree and returns its nodes, root first. */
    private interface Scenario {
        List<NodeId> build(TaffyTree tree);

        default NodeId root(List<NodeId> nodes) {
            return nodes.get(0);
        }
    }

    // =============================================================================================
    // Flex sizing: grow, shrink, wrap, gap
    // =============================================================================================

    /**
     * Free space goes to the growing item, and the growing item keeps its size when the same list
     * is walked again.
     */
    @Test
    void flexGrowDistributesFreeSpace() {
        Scenario scenario = tree -> {
            List<NodeId> nodes = new ArrayList<>();
            NodeId a = tree.newLeaf(leaf(50f, 10f));
            TaffyStyle growStyle = leaf(50f, 10f);
            growStyle.flexGrow = 1f;
            NodeId b = tree.newLeaf(growStyle);
            TaffyStyle rootStyle = sized(row(), 300f, 100f);
            NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{a, b});
            nodes.add(root);
            nodes.add(a);
            nodes.add(b);
            return nodes;
        };
        assertWarmMatchesFresh("grow", scenario, 300f, 100f, 3);

        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        List<NodeId> nodes = scenario.build(tree);
        tree.computeLayout(nodes.get(0), definite(300f, 100f));
        assertEquals(50f, tree.getLayout(nodes.get(1)).size().width, EPSILON);
        assertEquals(250f, tree.getLayout(nodes.get(2)).size().width, EPSILON);
        assertEquals(50f, tree.getLayout(nodes.get(2)).location().x, EPSILON);
    }

    /**
     * Shrinking distributes the negative free space by scaled shrink factor, the min-size clamp
     * freezes the offending item and the remaining item absorbs the rest - the violation branch
     * writes the item's target and outer target sizes in place (P32).
     */
    @Test
    void flexShrinkClampsToMinSizeAndRefreezes() {
        Scenario scenario = tree -> {
            List<NodeId> nodes = new ArrayList<>();
            TaffyStyle aStyle = leaf(200f, 10f);
            aStyle.minSize = TaffySize.of(TaffyDimension.length(150f), TaffyDimension.AUTO);
            NodeId a = tree.newLeaf(aStyle);
            NodeId b = tree.newLeaf(leaf(300f, 10f));
            NodeId root = tree.newWithChildren(sized(row(), 300f, 100f), new NodeId[]{a, b});
            nodes.add(root);
            nodes.add(a);
            nodes.add(b);
            return nodes;
        };
        assertWarmMatchesFresh("shrink", scenario, 300f, 100f, 3);

        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        List<NodeId> nodes = scenario.build(tree);
        tree.computeLayout(nodes.get(0), definite(300f, 100f));
        assertEquals(150f, tree.getLayout(nodes.get(1)).size().width, EPSILON);
        assertEquals(150f, tree.getLayout(nodes.get(2)).size().width, EPSILON);
        assertEquals(150f, tree.getLayout(nodes.get(2)).location().x, EPSILON);
        assertEquals(300f, tree.getLayout(nodes.get(0)).size().width, EPSILON);
    }

    /**
     * A padded item shrinks by its inner (content-box) flex basis, so the main-axis padding+border
     * sum resolved at build time must be the one the shrink distribution uses - not the cross-axis
     * sum, which is a different number for asymmetric padding.
     */
    @Test
    void paddedItemShrinksByItsInnerFlexBasis() {
        Scenario scenario = tree -> {
            List<NodeId> nodes = new ArrayList<>();
            NodeId a = tree.newLeaf(leaf(100f, 10f));
            TaffyStyle paddedStyle = leaf(100f, 10f);
            paddedStyle.padding = TaffyRect.of(
                LengthPercentage.length(40f),
                LengthPercentage.length(40f),
                LengthPercentage.length(0f),
                LengthPercentage.length(0f)
            );
            NodeId b = tree.newLeaf(paddedStyle);
            NodeId root = tree.newWithChildren(sized(row(), 100f, 100f), new NodeId[]{a, b});
            nodes.add(root);
            nodes.add(a);
            nodes.add(b);
            return nodes;
        };
        assertWarmMatchesFresh("padded-shrink", scenario, 100f, 100f, 3);

        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        List<NodeId> nodes = scenario.build(tree);
        tree.computeLayout(nodes.get(0), definite(100f, 100f));
        // Scaled shrink factors 100 (a) and 100 - 80 = 20 (b), 100px of overflow:
        // a = 100 - 100 * 100/120, b = 100 - 100 * 20/120.
        assertEquals(16.6667f, tree.getLayout(nodes.get(1)).size().width, 0.001f);
        assertEquals(83.3333f, tree.getLayout(nodes.get(2)).size().width, 0.001f);
        assertEquals(16.6667f, tree.getLayout(nodes.get(2)).location().x, 0.001f);
    }

    /** Wrapping with a gap: two lines, the cross gap between them, the container sized by content. */
    @Test
    void flexWrapWithGapProducesTwoLines() {
        Scenario scenario = tree -> {
            List<NodeId> nodes = new ArrayList<>();
            NodeId a = tree.newLeaf(leaf(40f, 10f));
            NodeId b = tree.newLeaf(leaf(40f, 10f));
            NodeId c = tree.newLeaf(leaf(40f, 10f));
            TaffyStyle rootStyle = row();
            rootStyle.flexWrap = FlexWrap.WRAP;
            rootStyle.size = TaffySize.of(TaffyDimension.length(100f), TaffyDimension.AUTO);
            rootStyle.gap = TaffySize.of(LengthPercentage.length(4f), LengthPercentage.length(4f));
            NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{a, b, c});
            nodes.add(root);
            nodes.add(a);
            nodes.add(b);
            nodes.add(c);
            return nodes;
        };
        assertWarmMatchesFresh("wrap", scenario, 100f, 100f, 3);

        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        List<NodeId> nodes = scenario.build(tree);
        tree.computeLayout(nodes.get(0), definite(100f, 100f));
        assertEquals(0f, tree.getLayout(nodes.get(1)).location().x, EPSILON);
        assertEquals(44f, tree.getLayout(nodes.get(2)).location().x, EPSILON);
        // 84px of line 1, a 4px cross gap, then the third item on line 2.
        assertEquals(0f, tree.getLayout(nodes.get(3)).location().x, EPSILON);
        assertEquals(14f, tree.getLayout(nodes.get(3)).location().y, EPSILON);
        assertEquals(24f, tree.getLayout(nodes.get(0)).size().height, EPSILON);
    }

    // =============================================================================================
    // Percentage basis and auto margins
    // =============================================================================================

    /**
     * A percentage child resolves against the container's content box, and it resolves again when
     * the same flex item list is rebuilt for a wider basis (the cached style values are part of
     * the rebuild).
     */
    @Test
    void percentageChildResolvesAgainstTheContentBoxAndRebuilds() {
        Scenario scenario = tree -> {
            List<NodeId> nodes = new ArrayList<>();
            TaffyStyle labelStyle = new TaffyStyle();
            labelStyle.size = TaffySize.of(TaffyDimension.percent(0.5f), TaffyDimension.length(10f));
            NodeId label = tree.newLeaf(labelStyle);
            TaffyStyle panelStyle = column();
            panelStyle.flexGrow = 1f;
            panelStyle.padding = TaffyRect.all(LengthPercentage.length(10f));
            NodeId panel = tree.newWithChildren(panelStyle, new NodeId[]{label});
            NodeId root = tree.newWithChildren(sized(row(), 200f, 100f), new NodeId[]{panel});
            nodes.add(root);
            nodes.add(panel);
            nodes.add(label);
            return nodes;
        };
        assertWarmMatchesFresh("percent", scenario, 200f, 100f, 3);

        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        List<NodeId> nodes = scenario.build(tree);
        tree.computeLayout(nodes.get(0), definite(200f, 100f));
        assertEquals(90f, tree.getLayout(nodes.get(2)).size().width, EPSILON);

        // Rebuild the same list for a different inner-size basis: 300 - 2 * 10 = 280, so 50% = 140.
        TaffyStyle widerRoot = sized(row(), 300f, 100f);
        tree.setStyle(nodes.get(0), widerRoot);
        tree.computeLayout(nodes.get(0), definite(300f, 100f));
        assertEquals(140f, tree.getLayout(nodes.get(2)).size().width, EPSILON);

        TaffyTree fresh = new TaffyTree();
        fresh.disableRounding();
        List<NodeId> freshNodes = scenario.build(fresh);
        fresh.setStyle(freshNodes.get(0), sized(row(), 300f, 100f));
        fresh.computeLayout(freshNodes.get(0), definite(300f, 100f));
        assertSameLayout(fresh, freshNodes, tree, nodes, "percent after resize");
    }

    /**
     * An auto margin consumes the free space of the line, and a second pass recomputes it from the
     * resolved base margin instead of accumulating the assignment of the previous pass.
     */
    @Test
    void autoMarginIsRecomputedForEveryPass() {
        Scenario scenario = tree -> {
            List<NodeId> nodes = new ArrayList<>();
            NodeId a = tree.newLeaf(leaf(50f, 10f));
            TaffyStyle bStyle = leaf(30f, 10f);
            bStyle.margin = TaffyRect.of(
                LengthPercentageAuto.auto(),
                LengthPercentageAuto.length(0f),
                LengthPercentageAuto.length(0f),
                LengthPercentageAuto.length(0f)
            );
            NodeId b = tree.newLeaf(bStyle);
            NodeId root = tree.newWithChildren(sized(row(), 200f, 100f), new NodeId[]{a, b});
            nodes.add(root);
            nodes.add(a);
            nodes.add(b);
            return nodes;
        };
        assertWarmMatchesFresh("auto-margin", scenario, 200f, 100f, 4);

        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        List<NodeId> nodes = scenario.build(tree);
        tree.computeLayout(nodes.get(0), definite(200f, 100f));
        assertEquals(170f, tree.getLayout(nodes.get(2)).location().x, EPSILON);
        assertEquals(30f, tree.getLayout(nodes.get(2)).size().width, EPSILON);

        tree.invalidateLayoutReuse();
        tree.computeLayout(nodes.get(0), definite(200f, 100f));
        assertEquals(170f, tree.getLayout(nodes.get(2)).location().x, EPSILON);

        // A wider container moves the auto margin further; the previous assignment must not leak.
        tree.setStyle(nodes.get(0), sized(row(), 300f, 100f));
        tree.computeLayout(nodes.get(0), definite(300f, 100f));
        assertEquals(270f, tree.getLayout(nodes.get(2)).location().x, EPSILON);
    }

    // =============================================================================================
    // Out-of-flow children, scrollbar gutter, measure callback
    // =============================================================================================

    /**
     * An absolutely positioned child is not a flex item; it is positioned from its insets. A
     * scroll-container item reports the scrollbar gutter resolved from its own overflow style
     * (cached on the item by P32), and a measured child keeps the size its callback reports.
     */
    @Test
    void absoluteChildScrollbarGutterAndMeasuredChild() {
        Scenario scenario = tree -> {
            List<NodeId> nodes = new ArrayList<>();
            NodeId flow = tree.newLeaf(leaf(50f, 10f));
            TaffyStyle absoluteStyle = new TaffyStyle();
            absoluteStyle.position = TaffyPosition.ABSOLUTE;
            absoluteStyle.size = TaffySize.of(TaffyDimension.length(40f), TaffyDimension.length(20f));
            absoluteStyle.inset = TaffyRect.of(
                LengthPercentageAuto.length(10f),
                LengthPercentageAuto.AUTO,
                LengthPercentageAuto.length(5f),
                LengthPercentageAuto.AUTO
            );
            NodeId absolute = tree.newLeaf(absoluteStyle);
            TaffyStyle scrollerStyle = leaf(60f, 10f);
            scrollerStyle.overflow = new TaffyPoint<>(Overflow.SCROLL, Overflow.SCROLL);
            scrollerStyle.scrollbarWidth = 5f;
            NodeId scroller = tree.newLeaf(scrollerStyle);
            TaffyStyle measuredStyle = new TaffyStyle();
            // Not stretched: the callback's 12px height is the item's used cross size.
            measuredStyle.alignSelf = AlignItems.FLEX_START;
            NodeId measured = tree.newLeafWithMeasure(measuredStyle, (knownDimensions, availableSpace) -> {
                float width = knownDimensions == null ? Float.NaN : knownDimensions.width;
                float height = knownDimensions == null ? Float.NaN : knownDimensions.height;
                return new FloatSize(Float.isNaN(width) ? 28f : width, Float.isNaN(height) ? 12f : height);
            });
            TaffyStyle rootStyle = row();
            rootStyle.size = TaffySize.of(TaffyDimension.length(300f), TaffyDimension.length(100f));
            NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{flow, scroller, measured, absolute});
            nodes.add(root);
            nodes.add(flow);
            nodes.add(scroller);
            nodes.add(measured);
            nodes.add(absolute);
            return nodes;
        };
        assertWarmMatchesFresh("abspos-scroll-measure", scenario, 300f, 100f, 3);

        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        List<NodeId> nodes = scenario.build(tree);
        tree.computeLayout(nodes.get(0), definite(300f, 100f));
        // Only the three in-flow children take flex main-axis space.
        assertEquals(0f, tree.getLayout(nodes.get(1)).location().x, EPSILON);
        assertEquals(50f, tree.getLayout(nodes.get(2)).location().x, EPSILON);
        assertEquals(110f, tree.getLayout(nodes.get(3)).location().x, EPSILON);
        assertEquals(28f, tree.getLayout(nodes.get(3)).size().width, EPSILON);
        assertEquals(12f, tree.getLayout(nodes.get(3)).size().height, EPSILON);
        // The scroll container's gutter is resolved from its own overflow style.
        assertEquals(5f, tree.getLayout(nodes.get(2)).scrollbarSize().width, EPSILON);
        assertEquals(5f, tree.getLayout(nodes.get(2)).scrollbarSize().height, EPSILON);
        assertEquals(0f, tree.getLayout(nodes.get(3)).scrollbarSize().width, EPSILON);
        // The absolute child is positioned by its insets, not by the flex flow.
        assertEquals(10f, tree.getLayout(nodes.get(4)).location().x, EPSILON);
        assertEquals(5f, tree.getLayout(nodes.get(4)).location().y, EPSILON);
        assertEquals(40f, tree.getLayout(nodes.get(4)).size().width, EPSILON);
        assertEquals(20f, tree.getLayout(nodes.get(4)).size().height, EPSILON);
    }

    /** A measured child shrinks like any other flex item, and a repeated pass reproduces it. */
    @Test
    void measuredChildShrinksConsistentlyAcrossPasses() {
        Scenario scenario = tree -> {
            List<NodeId> nodes = new ArrayList<>();
            NodeId text = tree.newLeafWithMeasure(new TaffyStyle(), (knownDimensions, availableSpace) -> {
                float knownWidth = knownDimensions == null ? Float.NaN : knownDimensions.width;
                float knownHeight = knownDimensions == null ? Float.NaN : knownDimensions.height;
                if (!Float.isNaN(knownWidth) && !Float.isNaN(knownHeight)) {
                    return new FloatSize(knownWidth, knownHeight);
                }
                float natural = 4 * 7f;
                float width = natural;
                if (!Float.isNaN(knownWidth)) {
                    width = knownWidth;
                } else if (availableSpace != null && availableSpace.width != null) {
                    if (availableSpace.width.isDefinite()) {
                        width = Math.min(natural, availableSpace.width.getValue());
                    } else if (availableSpace.width.isMinContent()) {
                        width = 0f;
                    }
                }
                return new FloatSize(width, Float.isNaN(knownHeight) ? 12f : knownHeight);
            });
            NodeId fixed = tree.newLeaf(leaf(20f, 10f));
            NodeId root = tree.newWithChildren(sized(row(), 300f, 100f), new NodeId[]{text, fixed});
            nodes.add(root);
            nodes.add(text);
            nodes.add(fixed);
            return nodes;
        };
        assertWarmMatchesFresh("measure", scenario, 300f, 100f, 3);
        assertWarmMatchesFresh("measure-narrow", scenario, 40f, 100f, 3);

        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        List<NodeId> nodes = scenario.build(tree);
        tree.computeLayout(nodes.get(0), definite(300f, 100f));
        assertEquals(28f, tree.getLayout(nodes.get(1)).size().width, EPSILON);
        // The row has no gap, so the fixed child starts right after the measured one.
        assertEquals(28f, tree.getLayout(nodes.get(2)).location().x, EPSILON);
    }

    // =============================================================================================
    // Direction, aspect ratio and multi-pass nesting
    // =============================================================================================

    /** RTL rows mirror the main axis and column-reverse flips it; both are stable across passes. */
    @Test
    void directionAndReversedColumnAreStableAcrossPasses() {
        Scenario rtl = tree -> {
            List<NodeId> nodes = new ArrayList<>();
            NodeId a = tree.newLeaf(leaf(50f, 10f));
            NodeId b = tree.newLeaf(leaf(60f, 10f));
            TaffyStyle rootStyle = sized(row(), 200f, 100f);
            rootStyle.direction = TaffyDirection.RTL;
            NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{a, b});
            nodes.add(root);
            nodes.add(a);
            nodes.add(b);
            return nodes;
        };
        assertWarmMatchesFresh("rtl-row", rtl, 200f, 100f, 3);

        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        List<NodeId> rtlNodes = rtl.build(tree);
        tree.computeLayout(rtlNodes.get(0), definite(200f, 100f));
        assertEquals(150f, tree.getLayout(rtlNodes.get(1)).location().x, EPSILON);
        assertEquals(90f, tree.getLayout(rtlNodes.get(2)).location().x, EPSILON);

        Scenario columnReverse = tree2 -> {
            List<NodeId> nodes = new ArrayList<>();
            NodeId a = tree2.newLeaf(leaf(10f, 50f));
            NodeId b = tree2.newLeaf(leaf(10f, 60f));
            TaffyStyle rootStyle = column();
            rootStyle.flexDirection = FlexDirection.COLUMN_REVERSE;
            rootStyle.size = TaffySize.of(TaffyDimension.length(100f), TaffyDimension.length(200f));
            NodeId root = tree2.newWithChildren(rootStyle, new NodeId[]{a, b});
            nodes.add(root);
            nodes.add(a);
            nodes.add(b);
            return nodes;
        };
        assertWarmMatchesFresh("column-reverse", columnReverse, 100f, 200f, 3);

        TaffyTree reverseTree = new TaffyTree();
        reverseTree.disableRounding();
        List<NodeId> reverseNodes = columnReverse.build(reverseTree);
        reverseTree.computeLayout(reverseNodes.get(0), definite(100f, 200f));
        // main-start is the bottom edge for column-reverse: the 90px of free space sits above
        // the items, and the last flex-order item (b, 60px) comes first from the bottom edge.
        assertEquals(90f, reverseTree.getLayout(reverseNodes.get(2)).location().y, EPSILON);
        assertEquals(150f, reverseTree.getLayout(reverseNodes.get(1)).location().y, EPSILON);
    }

    /** An aspect-ratio-derived cross size is transferred at build time and survives a relayout. */
    @Test
    void aspectRatioDerivedCrossSizeSurvivesRelayout() {
        Scenario scenario = tree -> {
            List<NodeId> nodes = new ArrayList<>();
            TaffyStyle ratioStyle = new TaffyStyle();
            ratioStyle.size = TaffySize.of(TaffyDimension.length(40f), TaffyDimension.AUTO);
            ratioStyle.aspectRatio = 2f;
            ratioStyle.alignSelf = AlignItems.FLEX_START;
            NodeId ratio = tree.newLeaf(ratioStyle);
            NodeId plain = tree.newLeaf(leaf(30f, 10f));
            NodeId root = tree.newWithChildren(sized(row(), 300f, 100f), new NodeId[]{ratio, plain});
            nodes.add(root);
            nodes.add(ratio);
            nodes.add(plain);
            return nodes;
        };
        assertWarmMatchesFresh("aspect-ratio", scenario, 300f, 100f, 3);

        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        List<NodeId> nodes = scenario.build(tree);
        tree.computeLayout(nodes.get(0), definite(300f, 100f));
        assertEquals(40f, tree.getLayout(nodes.get(1)).size().width, EPSILON);
        assertEquals(20f, tree.getLayout(nodes.get(1)).size().height, EPSILON);
        assertEquals(40f, tree.getLayout(nodes.get(2)).location().x, EPSILON);
    }

    /**
     * A flex container that is a grid item is laid out at several known widths while the {@code
     * 1fr} track is sized, then at its final width - within a single layout pass. Its item list is
     * therefore built, reset and reused inside that pass, and the percentage child must still
     * resolve against the final inner width.
     */
    @Test
    void flexContainerMeasuredByGridMatchesFreshTree() {
        Scenario scenario = tree -> {
            List<NodeId> nodes = new ArrayList<>();
            TaffyStyle labelStyle = new TaffyStyle();
            labelStyle.size = TaffySize.of(TaffyDimension.percent(1f), TaffyDimension.length(10f));
            NodeId label = tree.newLeaf(labelStyle);
            TaffyStyle cardStyle = column();
            cardStyle.padding = TaffyRect.all(LengthPercentage.length(6f));
            NodeId card = tree.newWithChildren(cardStyle, new NodeId[]{label});
            TaffyStyle rootStyle = new TaffyStyle();
            rootStyle.display = TaffyDisplay.GRID;
            rootStyle.size = TaffySize.of(TaffyDimension.length(320f), TaffyDimension.length(200f));
            rootStyle.gridTemplateColumns = List.of(TrackSizingFunction.fr(1f));
            NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{card});
            nodes.add(root);
            nodes.add(card);
            nodes.add(label);
            return nodes;
        };
        assertWarmMatchesFresh("grid-item", scenario, 320f, 200f, 2);

        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        List<NodeId> nodes = scenario.build(tree);
        tree.computeLayout(nodes.get(0), definite(320f, 200f));
        assertEquals(320f, tree.getLayout(nodes.get(1)).size().width, EPSILON);
        assertEquals(308f, tree.getLayout(nodes.get(2)).size().width, EPSILON);
    }

    // =============================================================================================
    // Whole-form reuse across passes
    // =============================================================================================

    /**
     * A form that exercises grow, shrink, wrapping, gaps, percentages, auto margins, out-of-flow
     * children, a measure callback, an aspect ratio, a scroll-container item and RTL inside nested
     * flex containers. It is laid out three times on the same tree - reusing the item lists built
     * by the previous pass - and every node is compared with a freshly built tree after each pass,
     * at two different available spaces.
     */
    @Test
    void richFormReusesItemListsAndMatchesAFreshTree() {
        TaffyTree warm = new TaffyTree();
        warm.disableRounding();
        List<NodeId> warmNodes = buildRichForm(warm);
        FlexboxComputer.FlexItemCache firstCache = null;

        float[][] spaces = {{400f, 300f}, {320f, 260f}, {400f, 300f}};
        for (int pass = 0; pass < spaces.length; pass++) {
            float width = spaces[pass][0];
            float height = spaces[pass][1];
            if (pass > 0) {
                warm.invalidateLayoutReuse();
            }
            warm.computeLayout(warmNodes.get(0), definite(width, height));
            if (pass == 0) {
                firstCache = warm.getNodeData(warmNodes.get(0)).getFlexItemCache();
            }

            TaffyTree fresh = new TaffyTree();
            fresh.disableRounding();
            List<NodeId> freshNodes = buildRichForm(fresh);
            fresh.computeLayout(freshNodes.get(0), definite(width, height));
            assertSameLayout(fresh, freshNodes, warm, warmNodes, "rich form pass " + (pass + 1));
        }

        // The container's list really is the one built by the first pass (a rebuilt list would be a
        // new cache object), so the passes above did reuse it.
        assertNotNull(firstCache);
        assertSame(firstCache, warm.getNodeData(warmNodes.get(0)).getFlexItemCache());
    }

    /**
     * Builds the rich form; the returned list is in a fixed order and every id is laid out.
     */
    private static List<NodeId> buildRichForm(TaffyTree tree) {
        List<NodeId> nodes = new ArrayList<>();

        TaffyStyle percentStyle = new TaffyStyle();
        percentStyle.size = TaffySize.of(TaffyDimension.percent(0.5f), TaffyDimension.length(10f));
        NodeId percent = tree.newLeaf(percentStyle);
        nodes.add(percent);

        TaffyStyle shrinkStyle = leaf(1000f, 10f);
        shrinkStyle.minSize = TaffySize.of(TaffyDimension.length(120f), TaffyDimension.AUTO);
        NodeId shrink = tree.newLeaf(shrinkStyle);
        nodes.add(shrink);

        NodeId measured = tree.newLeafWithMeasure(new TaffyStyle(), (knownDimensions, availableSpace) -> {
            float width = knownDimensions == null ? Float.NaN : knownDimensions.width;
            float height = knownDimensions == null ? Float.NaN : knownDimensions.height;
            return new FloatSize(Float.isNaN(width) ? 30f : width, Float.isNaN(height) ? 20f : height);
        });
        nodes.add(measured);

        TaffyStyle autoMarginStyle = leaf(20f, 10f);
        autoMarginStyle.margin = TaffyRect.of(
            LengthPercentageAuto.auto(),
            LengthPercentageAuto.length(0f),
            LengthPercentageAuto.length(0f),
            LengthPercentageAuto.length(0f)
        );
        NodeId autoMargin = tree.newLeaf(autoMarginStyle);
        nodes.add(autoMargin);

        TaffyStyle absoluteStyle = new TaffyStyle();
        absoluteStyle.position = TaffyPosition.ABSOLUTE;
        absoluteStyle.size = TaffySize.of(TaffyDimension.length(9f), TaffyDimension.length(8f));
        absoluteStyle.inset = TaffyRect.of(
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.length(4f),
            LengthPercentageAuto.length(4f),
            LengthPercentageAuto.AUTO
        );
        NodeId absolute = tree.newLeaf(absoluteStyle);
        nodes.add(absolute);

        TaffyStyle ratioStyle = new TaffyStyle();
        ratioStyle.size = TaffySize.of(TaffyDimension.AUTO, TaffyDimension.length(14f));
        ratioStyle.aspectRatio = 1.5f;
        NodeId ratio = tree.newLeaf(ratioStyle);
        nodes.add(ratio);

        TaffyStyle panelStyle = column();
        panelStyle.flexGrow = 1f;
        panelStyle.flexShrink = 1f;
        panelStyle.padding = TaffyRect.all(LengthPercentage.length(3f));
        panelStyle.gap = TaffySize.of(LengthPercentage.length(2f), LengthPercentage.length(2f));
        NodeId panelA = tree.newWithChildren(panelStyle,
            new NodeId[]{percent, shrink, measured, autoMargin, absolute, ratio});
        nodes.add(panelA);

        TaffyStyle wrapStyle = row();
        wrapStyle.flexWrap = FlexWrap.WRAP;
        wrapStyle.size = TaffySize.of(TaffyDimension.length(100f), TaffyDimension.AUTO);
        wrapStyle.gap = TaffySize.of(LengthPercentage.length(4f), LengthPercentage.length(4f));
        NodeId wrapA = tree.newLeaf(leaf(40f, 10f));
        nodes.add(wrapA);
        NodeId wrapB = tree.newLeaf(leaf(40f, 10f));
        nodes.add(wrapB);
        NodeId wrapC = tree.newLeaf(leaf(40f, 10f));
        nodes.add(wrapC);
        NodeId panelB = tree.newWithChildren(wrapStyle, new NodeId[]{wrapA, wrapB, wrapC});
        nodes.add(panelB);

        TaffyStyle scrollerStyle = leaf(70f, 10f);
        scrollerStyle.overflow = new TaffyPoint<>(Overflow.SCROLL, Overflow.HIDDEN);
        scrollerStyle.scrollbarWidth = 4f;
        NodeId scroller = tree.newLeaf(scrollerStyle);
        nodes.add(scroller);

        TaffyStyle rtlStyle = row();
        rtlStyle.direction = TaffyDirection.RTL;
        rtlStyle.size = TaffySize.of(TaffyDimension.length(80f), TaffyDimension.AUTO);
        NodeId rtlA = tree.newLeaf(leaf(20f, 9f));
        nodes.add(rtlA);
        NodeId rtlB = tree.newLeaf(leaf(30f, 9f));
        nodes.add(rtlB);
        NodeId rtlPanel = tree.newWithChildren(rtlStyle, new NodeId[]{rtlA, rtlB});
        nodes.add(rtlPanel);

        NodeId footer = tree.newLeaf(leaf(60f, 9f));
        nodes.add(footer);

        TaffyStyle rootStyle = row();
        rootStyle.size = TaffySize.of(TaffyDimension.length(400f), TaffyDimension.length(300f));
        rootStyle.padding = TaffyRect.all(LengthPercentage.length(2f));
        rootStyle.gap = TaffySize.of(LengthPercentage.length(6f), LengthPercentage.length(6f));
        NodeId root = tree.newWithChildren(rootStyle,
            new NodeId[]{panelA, panelB, scroller, rtlPanel, footer});
        nodes.add(root);
        // Root first, so that "nodes.get(0)" is the layout root in every scenario.
        List<NodeId> ordered = new ArrayList<>(nodes.size());
        ordered.add(root);
        nodes.remove(nodes.size() - 1);
        ordered.addAll(nodes);
        return ordered;
    }

    // =============================================================================================
    // Harness
    // =============================================================================================

    /**
     * Lays {@code scenario} out on a warm tree {@code passes} times (invalidating the layout reuse
     * between passes so the flex item lists are exercised again) and compares every node, rounded
     * and unrounded, with a tree built from scratch for the same pass.
     */
    private static void assertWarmMatchesFresh(String label, Scenario scenario, float width, float height,
                                               int passes) {
        TaffyTree warm = new TaffyTree();
        warm.disableRounding();
        List<NodeId> warmNodes = scenario.build(warm);
        TaffySize<AvailableSpace> space = definite(width, height);

        for (int pass = 1; pass <= passes; pass++) {
            if (pass > 1) {
                warm.invalidateLayoutReuse();
            }
            warm.computeLayout(warmNodes.get(0), space);

            TaffyTree fresh = new TaffyTree();
            fresh.disableRounding();
            List<NodeId> freshNodes = scenario.build(fresh);
            fresh.computeLayout(freshNodes.get(0), space);

            assertSameLayout(fresh, freshNodes, warm, warmNodes, label + " pass " + pass);
        }
    }

    private static void assertSameLayout(TaffyTree expectedTree, List<NodeId> expectedNodes,
                                         TaffyTree actualTree, List<NodeId> actualNodes, String label) {
        assertEquals(expectedNodes.size(), actualNodes.size(), label + " node count");
        for (int i = 0; i < expectedNodes.size(); i++) {
            String where = label + " node " + i;
            Layout expected = expectedTree.getLayout(expectedNodes.get(i));
            Layout actual = actualTree.getLayout(expectedNodes.get(i));
            assertNotNull(expected, where + " expected layout");
            assertNotNull(actual, where + " actual layout");
            assertLayoutEquals(expected, actual, where + " final");

            Layout expectedUnrounded = expectedTree.getUnroundedLayout(expectedNodes.get(i));
            Layout actualUnrounded = actualTree.getUnroundedLayout(expectedNodes.get(i));
            assertNotNull(expectedUnrounded, where + " expected unrounded layout");
            assertNotNull(actualUnrounded, where + " actual unrounded layout");
            assertLayoutEquals(expectedUnrounded, actualUnrounded, where + " unrounded");
        }
    }

    private static void assertLayoutEquals(Layout expected, Layout actual, String label) {
        assertEquals(expected.order(), actual.order(), label + " order");
        assertPointEquals(expected.location(), actual.location(), label + " location");
        assertSizeEquals(expected.size(), actual.size(), label + " size");
        assertSizeEquals(expected.contentSize(), actual.contentSize(), label + " contentSize");
        assertEquals(expected.scrollableOverflow().left(), actual.scrollableOverflow().left(), label + " overflow.left");
        assertEquals(expected.scrollableOverflow().right(), actual.scrollableOverflow().right(), label + " overflow.right");
        assertEquals(expected.scrollableOverflow().top(), actual.scrollableOverflow().top(), label + " overflow.top");
        assertEquals(expected.scrollableOverflow().bottom(), actual.scrollableOverflow().bottom(), label + " overflow.bottom");
        assertEquals(expected.scrollableOverflow().reversedX(), actual.scrollableOverflow().reversedX(),
            label + " overflow.reversedX");
        assertEquals(expected.scrollableOverflow().reversedY(), actual.scrollableOverflow().reversedY(),
            label + " overflow.reversedY");
        assertSizeEquals(expected.scrollbarSize(), actual.scrollbarSize(), label + " scrollbarSize");
        assertRectEquals(expected.border(), actual.border(), label + " border");
        assertRectEquals(expected.padding(), actual.padding(), label + " padding");
        assertRectEquals(expected.margin(), actual.margin(), label + " margin");
    }

    private static void assertPointEquals(FloatPoint expected, FloatPoint actual, String label) {
        assertEquals(expected.x, actual.x, label + ".x");
        assertEquals(expected.y, actual.y, label + ".y");
    }

    private static void assertSizeEquals(FloatSize expected, FloatSize actual, String label) {
        assertEquals(expected.width, actual.width, label + ".width");
        assertEquals(expected.height, actual.height, label + ".height");
    }

    private static void assertRectEquals(FloatRect expected, FloatRect actual, String label) {
        assertEquals(expected.left, actual.left, label + ".left");
        assertEquals(expected.right, actual.right, label + ".right");
        assertEquals(expected.top, actual.top, label + ".top");
        assertEquals(expected.bottom, actual.bottom, label + ".bottom");
    }

    // =============================================================================================
    // Styles
    // =============================================================================================

    private static TaffySize<AvailableSpace> definite(float width, float height) {
        return TaffySize.of(AvailableSpace.definite(width), AvailableSpace.definite(height));
    }

    private static TaffyStyle row() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.ROW;
        return style;
    }

    private static TaffyStyle column() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.COLUMN;
        return style;
    }

    private static TaffyStyle leaf(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = TaffySize.of(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle sized(TaffyStyle style, float width, float height) {
        style.size = TaffySize.of(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }
}
