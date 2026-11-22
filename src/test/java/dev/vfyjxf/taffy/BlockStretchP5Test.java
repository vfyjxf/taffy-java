package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * P5 regression tests for {@code height: stretch} in block layout.
 *
 * <p>Upstream #1242 ({@code 6a36efbe}): when a block-level box resolves a {@code stretch} height,
 * a block-axis margin that could collapse through the corresponding edge of its containing block
 * is treated as zero; any other margin is subtracted from the stretch size. The containing block's
 * edge "adjoins" the child margin when it has no border or padding on that side, does not
 * establish a new formatting context, and its margins may collapse with its children's
 * (csswg-drafts#11044, csswg-drafts#13260).</p>
 *
 * <p>Ten of the fourteen upstream fixtures are portable here; each expectation below is copied
 * from the browser-computed {@code tests/xml/block/<fixture>__border_box_ltr.xml} oracle:</p>
 *
 * <ul>
 *   <li>{@code block_height_stretch_margins_no_border}</li>
 *   <li>{@code block_height_stretch_margins_border}</li>
 *   <li>{@code block_height_stretch_margins_border_bottom_only}</li>
 *   <li>{@code block_height_stretch_margins_bfc_root}</li>
 *   <li>{@code block_height_stretch_margins_flow_root}</li>
 *   <li>{@code block_height_stretch_margins_flow_root_child}</li>
 *   <li>{@code block_height_stretch_margins_overflow_hidden}</li>
 *   <li>{@code block_height_stretch_margins_padding_top_only}</li>
 *   <li>{@code block_height_stretch_margins_siblings}</li>
 *   <li>{@code block_height_stretch_margins_flex_item_container}</li>
 * </ul>
 *
 * <p>The four float fixtures ({@code float_child}, {@code next_to_float},
 * {@code next_to_float_clear}, {@code next_to_float_flow_root}) are not portable: this port has no
 * float layout engine yet (the FT-01..FT-07 work items), and their expectations depend on float
 * positioning rather than on the stretch resolution alone. They are deliberately not reproduced
 * here.</p>
 *
 * <p>As in {@link BlockP3Test}, a fixture whose root is an absolutely positioned box is
 * reproduced with a block root (the browser harness's outer {@code #test-root} wrapper is not part
 * of the taffy tree); the tree root is a block formatting context root, which is what
 * {@code block_height_stretch_margins_bfc_root} pins.</p>
 */
public class BlockStretchP5Test {

    private static final float EPSILON = 0.001f;

    // === Helpers ===

    private static TaffyStyle block() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        return style;
    }

    private static TaffyStyle box(float width, float height) {
        TaffyStyle style = block();
        style.size = new TaffySize<>(
            Float.isNaN(width) ? TaffyDimension.AUTO : TaffyDimension.length(width),
            Float.isNaN(height) ? TaffyDimension.AUTO : TaffyDimension.length(height)
        );
        return style;
    }

    /** A block-level box with {@code height: stretch} and the given uniform margin. */
    private static TaffyStyle stretchWithMargin(float margin) {
        TaffyStyle style = block();
        style.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.STRETCH);
        style.margin = TaffyRect.all(LengthPercentageAuto.length(margin));
        return style;
    }

    private static Layout layout(TaffyTree tree, NodeId node) {
        Layout layout = tree.getLayout(node);
        assertNotNull(layout, "node has no layout");
        return layout;
    }

    private static void assertBox(TaffyTree tree, NodeId node,
                                  float x, float y, float width, float height, String message) {
        Layout layout = layout(tree, node);
        assertEquals(x, layout.location().x, EPSILON, message + " x");
        assertEquals(y, layout.location().y, EPSILON, message + " y");
        assertEquals(width, layout.size().width, EPSILON, message + " width");
        assertEquals(height, layout.size().height, EPSILON, message + " height");
    }

    // === Upstream #1242 fixtures: which margins adjoin which container edge ===

    /**
     * {@code block_height_stretch_margins_no_border}: the container has neither border nor
     * padding, so both of the child's margins adjoin its edges and are treated as zero. The
     * stretched child therefore fills the 100px content box even though its margins would
     * normally overflow it.
     */
    @Test
    @DisplayName("BK-19: margins adjoin an unadorned container on both edges and are treated as zero")
    void noBorderMarginsAdjoinBothEdges() {
        TaffyTree tree = new TaffyTree();

        NodeId child = tree.newLeaf(stretchWithMargin(10f));
        NodeId container = tree.newWithChildren(box(100f, 100f), child);
        NodeId root = tree.newWithChildren(box(100f, Float.NaN), container);

        tree.computeLayout(root, TaffySize.maxContent());

        assertBox(tree, root, 0f, 0f, 100f, 110f, "root");
        assertBox(tree, container, 0f, 10f, 100f, 100f, "container");
        assertBox(tree, child, 10f, 0f, 80f, 100f, "stretch child");
    }

    /**
     * {@code block_height_stretch_margins_border}: a border on both edges prevents the collapse,
     * so both margins are subtracted from the 94px content box.
     */
    @Test
    @DisplayName("BK-19: a border on both edges makes both margins count")
    void borderOnBothEdgesSubtractsMargins() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = box(100f, 100f);
        rootStyle.border = TaffyRect.all(LengthPercentage.length(3f));
        NodeId child = tree.newLeaf(stretchWithMargin(10f));
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertBox(tree, root, 0f, 0f, 100f, 100f, "root");
        assertBox(tree, child, 13f, 13f, 74f, 74f, "stretch child");
    }

    /**
     * {@code block_height_stretch_margins_border_bottom_only}: only the bottom edge is decorated,
     * so the top margin is treated as zero while the bottom margin is subtracted from the 95px
     * content box.
     */
    @Test
    @DisplayName("BK-19: a bottom border makes only the bottom margin count")
    void borderBottomOnlySubtractsOnlyTheBottomMargin() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle containerStyle = box(100f, 100f);
        containerStyle.border = new TaffyRect<>(
            LengthPercentage.ZERO,
            LengthPercentage.ZERO,
            LengthPercentage.ZERO,
            LengthPercentage.length(5f)
        );
        NodeId child = tree.newLeaf(stretchWithMargin(10f));
        NodeId container = tree.newWithChildren(containerStyle, child);
        NodeId root = tree.newWithChildren(box(100f, Float.NaN), container);

        tree.computeLayout(root, TaffySize.maxContent());

        assertBox(tree, root, 0f, 0f, 100f, 110f, "root");
        assertBox(tree, container, 0f, 10f, 100f, 100f, "container");
        assertBox(tree, child, 10f, 0f, 80f, 85f, "stretch child");
    }

    /**
     * {@code block_height_stretch_margins_bfc_root}: the tree root is a block formatting context
     * root, so its children's margins do not adjoin its edges even though it has no border or
     * padding.
     */
    @Test
    @DisplayName("BK-19: a block formatting context root does not adjoin its children's margins")
    void bfcRootMarginsAreNotAdjoined() {
        TaffyTree tree = new TaffyTree();

        NodeId child = tree.newLeaf(stretchWithMargin(10f));
        NodeId root = tree.newWithChildren(box(100f, 100f), child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertBox(tree, root, 0f, 0f, 100f, 100f, "root");
        assertBox(tree, child, 10f, 10f, 80f, 80f, "stretch child");
    }

    /**
     * {@code block_height_stretch_margins_flow_root}: {@code display: flow-root} establishes a
     * new formatting context, so the child margins do not adjoin the container edges.
     */
    @Test
    @DisplayName("BK-19: a flow-root container does not adjoin its children's margins")
    void flowRootContainerMarginsAreNotAdjoined() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle containerStyle = box(100f, 100f);
        containerStyle.display = TaffyDisplay.FLOW_ROOT;
        NodeId child = tree.newLeaf(stretchWithMargin(10f));
        NodeId container = tree.newWithChildren(containerStyle, child);
        NodeId root = tree.newWithChildren(box(100f, Float.NaN), container);

        tree.computeLayout(root, TaffySize.maxContent());

        assertBox(tree, root, 0f, 0f, 100f, 100f, "root");
        assertBox(tree, container, 0f, 0f, 100f, 100f, "container");
        assertBox(tree, child, 10f, 10f, 80f, 80f, "stretch child");
    }

    /**
     * {@code block_height_stretch_margins_flow_root_child}: a {@code flow-root} *child* is still
     * stretched from its plain block container's content box, and its own margins are judged by
     * the container's edges, not by the fact that the child establishes a new BFC.
     */
    @Test
    @DisplayName("BK-19: a flow-root child still uses the container's edge adjoinment")
    void flowRootChildMarginsStillAdjoinTheContainerEdges() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle childStyle = stretchWithMargin(10f);
        childStyle.display = TaffyDisplay.FLOW_ROOT;
        NodeId child = tree.newLeaf(childStyle);
        NodeId container = tree.newWithChildren(box(100f, 100f), child);
        NodeId root = tree.newWithChildren(box(100f, Float.NaN), container);

        tree.computeLayout(root, TaffySize.maxContent());

        assertBox(tree, root, 0f, 0f, 100f, 110f, "root");
        assertBox(tree, container, 0f, 10f, 100f, 100f, "container");
        assertBox(tree, child, 10f, 0f, 80f, 100f, "stretch child");
    }

    /**
     * {@code block_height_stretch_margins_overflow_hidden}: a scroll container establishes a new
     * formatting context, so neither margin is treated as zero.
     */
    @Test
    @DisplayName("BK-19: an overflow:hidden container does not adjoin its children's margins")
    void scrollContainerMarginsAreNotAdjoined() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle containerStyle = box(100f, 100f);
        containerStyle.overflow = new TaffyPoint<>(Overflow.HIDDEN, Overflow.HIDDEN);
        containerStyle.scrollbarWidth = 15f;
        NodeId child = tree.newLeaf(stretchWithMargin(10f));
        NodeId container = tree.newWithChildren(containerStyle, child);
        NodeId root = tree.newWithChildren(box(100f, Float.NaN), container);

        tree.computeLayout(root, TaffySize.maxContent());

        assertBox(tree, root, 0f, 0f, 100f, 100f, "root");
        assertBox(tree, container, 0f, 0f, 100f, 100f, "container");
        assertBox(tree, child, 10f, 10f, 80f, 80f, "stretch child");
    }

    /**
     * {@code block_height_stretch_margins_padding_top_only}: only the top edge is decorated, so
     * the top margin is subtracted from the 95px content box while the bottom margin is treated
     * as zero.
     */
    @Test
    @DisplayName("BK-19: top padding makes only the top margin count")
    void paddingTopOnlySubtractsOnlyTheTopMargin() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle containerStyle = box(100f, 100f);
        containerStyle.padding = new TaffyRect<>(
            LengthPercentage.ZERO,
            LengthPercentage.ZERO,
            LengthPercentage.length(5f),
            LengthPercentage.ZERO
        );
        NodeId child = tree.newLeaf(stretchWithMargin(10f));
        NodeId container = tree.newWithChildren(containerStyle, child);
        NodeId root = tree.newWithChildren(box(100f, Float.NaN), container);

        tree.computeLayout(root, TaffySize.maxContent());

        assertBox(tree, root, 0f, 0f, 100f, 100f, "root");
        assertBox(tree, container, 0f, 0f, 100f, 100f, "container");
        assertBox(tree, child, 10f, 15f, 80f, 85f, "stretch child");
    }

    /**
     * {@code block_height_stretch_margins_siblings}: the stretch size does not depend on the
     * preceding content; the child fills the container's content box while the zero-height
     * siblings stay where the margin collapse puts them.
     */
    @Test
    @DisplayName("BK-19: a stretched child among zero-height siblings still fills the content box")
    void stretchedSiblingAmongZeroHeightSiblings() {
        TaffyTree tree = new TaffyTree();

        NodeId first = tree.newLeaf(block());

        TaffyStyle scrollStyle = block();
        scrollStyle.overflow = new TaffyPoint<>(Overflow.HIDDEN, Overflow.HIDDEN);
        scrollStyle.scrollbarWidth = 15f;
        NodeId second = tree.newLeaf(scrollStyle);

        NodeId stretched = tree.newLeaf(stretchWithMargin(10f));

        TaffyStyle scrollStyle2 = block();
        scrollStyle2.overflow = new TaffyPoint<>(Overflow.HIDDEN, Overflow.HIDDEN);
        scrollStyle2.scrollbarWidth = 15f;
        NodeId fourth = tree.newLeaf(scrollStyle2);

        NodeId fifth = tree.newLeaf(block());

        NodeId container = tree.newWithChildren(box(100f, 100f), first, second, stretched, fourth, fifth);
        NodeId root = tree.newWithChildren(box(100f, Float.NaN), container);

        tree.computeLayout(root, TaffySize.maxContent());

        assertBox(tree, root, 0f, 0f, 100f, 100f, "root");
        assertBox(tree, container, 0f, 0f, 100f, 100f, "container");
        assertBox(tree, first, 0f, 0f, 100f, 0f, "first sibling");
        assertBox(tree, second, 0f, 0f, 100f, 0f, "second sibling");
        assertBox(tree, stretched, 10f, 10f, 80f, 100f, "stretch child");
        assertBox(tree, fourth, 0f, 120f, 100f, 0f, "fourth sibling");
        assertBox(tree, fifth, 0f, 120f, 100f, 0f, "fifth sibling");
    }

    /**
     * {@code block_height_stretch_margins_flex_item_container}: the block container is a flex
     * item, so it is laid out with non-collapsible vertical margins; its child's margins do not
     * adjoin its edges.
     */
    @Test
    @DisplayName("BK-19: a block container that is a flex item does not adjoin its child's margins")
    void blockContainerAsFlexItemMarginsAreNotAdjoined() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle flexRootStyle = new TaffyStyle();
        flexRootStyle.display = TaffyDisplay.FLEX;
        flexRootStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.AUTO);

        NodeId child = tree.newLeaf(stretchWithMargin(10f));
        NodeId blockItem = tree.newWithChildren(box(100f, 100f), child);
        NodeId root = tree.newWithChildren(flexRootStyle, blockItem);

        tree.computeLayout(root, TaffySize.maxContent());

        assertBox(tree, root, 0f, 0f, 100f, 100f, "root");
        assertBox(tree, blockItem, 0f, 0f, 100f, 100f, "flex item");
        assertBox(tree, child, 10f, 10f, 80f, 80f, "stretch child");
    }

    // === Edge cases around the keyword resolution ===

    /**
     * A {@code stretch} height with no percentage resolution basis (the containing block's height
     * is automatic) resolves as {@code auto}: the child keeps its content height instead of
     * filling anything.
     */
    @Test
    @DisplayName("BK-19: stretch without a definite container height behaves as auto")
    void stretchFallsBackToAutoWithoutPercentageBasis() {
        TaffyTree tree = new TaffyTree();

        NodeId fixed = tree.newLeaf(box(Float.NaN, 30f));
        NodeId stretched = tree.newLeaf(stretchWithMargin(0f));
        NodeId root = tree.newWithChildren(box(100f, Float.NaN), fixed, stretched);

        tree.computeLayout(root, TaffySize.maxContent());

        assertBox(tree, root, 0f, 0f, 100f, 30f, "root");
        assertBox(tree, fixed, 0f, 0f, 100f, 30f, "fixed child");
        assertBox(tree, stretched, 0f, 30f, 100f, 0f, "stretch child falls back to auto");
    }

    /** The resolved stretch height is clamped by {@code min-height}. */
    @Test
    @DisplayName("BK-19: min-height clamps the resolved stretch height")
    void minHeightClampsStretchHeight() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle childStyle = stretchWithMargin(10f);
        childStyle.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(90f));
        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(box(100f, 100f), child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertBox(tree, root, 0f, 0f, 100f, 100f, "root");
        // The stretch size is 100 - 10 - 10 = 80, clamped up by min-height: 90.
        assertBox(tree, child, 10f, 10f, 80f, 90f, "clamped stretch child");
    }

    /** The resolved stretch height is clamped by {@code max-height}. */
    @Test
    @DisplayName("BK-19: max-height clamps the resolved stretch height")
    void maxHeightClampsStretchHeight() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle childStyle = stretchWithMargin(10f);
        childStyle.maxSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(70f));
        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(box(100f, 100f), child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertBox(tree, root, 0f, 0f, 100f, 100f, "root");
        // The stretch size is 100 - 10 - 10 = 80, clamped down by max-height: 70.
        assertBox(tree, child, 10f, 10f, 80f, 70f, "clamped stretch child");
    }

    /**
     * Regression: a percentage height still resolves against a definite containing block. The
     * stretch keyword resolution must not disturb the established BK-08 path.
     */
    @Test
    @DisplayName("BK-19: a percentage height still resolves against the definite container")
    void percentageHeightStillResolvesAgainstDefiniteContainer() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle childStyle = box(Float.NaN, Float.NaN);
        childStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.percent(0.5f));
        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(box(200f, 200f), child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertBox(tree, root, 0f, 0f, 200f, 200f, "root");
        assertBox(tree, child, 0f, 0f, 200f, 100f, "50% child");
    }

    /**
     * A container whose own height is {@code stretch} receives that resolved height as a known
     * dimension, so it becomes the percentage basis of its children (the BK-08 interaction).
     */
    @Test
    @DisplayName("BK-19: a stretched container becomes the percentage basis of its children")
    void stretchedContainerIsThePercentageBasisOfItsChildren() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle containerStyle = block();
        containerStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.STRETCH);
        containerStyle.margin = TaffyRect.all(LengthPercentageAuto.length(10f));

        TaffyStyle childStyle = box(Float.NaN, Float.NaN);
        childStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.percent(0.5f));
        NodeId child = tree.newLeaf(childStyle);
        NodeId container = tree.newWithChildren(containerStyle, child);
        NodeId root = tree.newWithChildren(box(100f, 200f), container);

        tree.computeLayout(root, TaffySize.maxContent());

        assertBox(tree, root, 0f, 0f, 100f, 200f, "root");
        // The root is a BFC root, so both 10px margins are subtracted: 200 - 20 = 180.
        assertBox(tree, container, 10f, 10f, 80f, 180f, "stretched container");
        // 50% of the stretched container's 180px content box.
        assertBox(tree, child, 0f, 0f, 80f, 90f, "percentage child");
    }

    /**
     * Only {@code stretch} is resolved explicitly in the block axis; the other intrinsic sizing
     * keywords are equal to the content size (what {@code auto} already resolves to), so they
     * must not fill the container.
     */
    @Test
    @DisplayName("BK-19: min-content/max-content/fit-content heights behave as auto in block layout")
    void intrinsicKeywordsOtherThanStretchBehaveAsAuto() {
        TaffyTree tree = new TaffyTree();

        NodeId maxContent = tree.newWithChildren(
            sizeWithHeight(TaffyDimension.MAX_CONTENT),
            tree.newLeaf(box(Float.NaN, 40f)));
        NodeId minContent = tree.newWithChildren(
            sizeWithHeight(TaffyDimension.MIN_CONTENT),
            tree.newLeaf(box(Float.NaN, 15f)));
        NodeId fitContent = tree.newWithChildren(
            sizeWithHeight(TaffyDimension.FIT_CONTENT),
            tree.newLeaf(box(Float.NaN, 25f)));

        NodeId root = tree.newWithChildren(box(100f, 100f), maxContent, minContent, fitContent);

        tree.computeLayout(root, TaffySize.maxContent());

        assertBox(tree, root, 0f, 0f, 100f, 100f, "root");
        assertBox(tree, maxContent, 0f, 0f, 100f, 40f, "max-content child");
        assertBox(tree, minContent, 0f, 40f, 100f, 15f, "min-content child");
        assertBox(tree, fitContent, 0f, 55f, 100f, 25f, "fit-content child");
    }

    /**
     * Regression: the historical {@code auto}/{@code px} heights are untouched — an auto-height
     * child still sizes to its content instead of filling the container, and a length height is
     * used verbatim.
     */
    @Test
    @DisplayName("BK-19: auto and px heights are not stretched")
    void autoAndFixedHeightsAreNotStretched() {
        TaffyTree tree = new TaffyTree();

        NodeId fixed = tree.newLeaf(box(Float.NaN, 20f));
        NodeId autoChild = tree.newWithChildren(block(), tree.newLeaf(box(Float.NaN, 30f)));
        NodeId root = tree.newWithChildren(box(100f, 100f), fixed, autoChild);

        tree.computeLayout(root, TaffySize.maxContent());

        assertBox(tree, root, 0f, 0f, 100f, 100f, "root");
        assertBox(tree, fixed, 0f, 0f, 100f, 20f, "px child");
        assertBox(tree, autoChild, 0f, 20f, 100f, 30f, "auto child");
    }

    private static TaffyStyle sizeWithHeight(TaffyDimension height) {
        TaffyStyle style = block();
        style.size = new TaffySize<>(TaffyDimension.AUTO, height);
        return style;
    }
}
