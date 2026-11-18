package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.LayoutComputer;
import dev.vfyjxf.taffy.tree.LayoutOutput;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.SizingMode;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P3 regression tests for block layout alignment (BK-08, BK-09, BK-17).
 *
 * <p>Each test mirrors the upstream taffy fixture that accompanied the corresponding fix, using
 * the browser-computed expectations from {@code tests/xml/block/**} as the oracle. As in
 * {@link BlockP1Test}, a fixture whose root is an absolutely positioned box is reproduced with a
 * block root: the outer browser harness wrapper is not part of the taffy tree.</p>
 */
public class BlockP3Test {

    private static final float EPSILON = 0.001f;

    // === Helpers ===

    private static TaffyStyle blockStyle() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        return style;
    }

    private static TaffyStyle flowRootStyle() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLOW_ROOT;
        return style;
    }

    private static TaffySize<TaffyDimension> size(TaffyDimension width, TaffyDimension height) {
        return new TaffySize<>(width, height);
    }

    private static void assertSize(Layout layout, float width, float height, String message) {
        assertEquals(width, layout.size().width, EPSILON, message + " width");
        assertEquals(height, layout.size().height, EPSILON, message + " height");
    }

    private static void assertLocation(Layout layout, float x, float y, String message) {
        assertEquals(x, layout.location().x, EPSILON, message + " x");
        assertEquals(y, layout.location().y, EPSILON, message + " y");
    }

    private static void assertPadding(Layout layout, float left, float right, float top, float bottom,
                                      String message) {
        assertEquals(left, layout.padding().left, EPSILON, message + " padding left");
        assertEquals(right, layout.padding().right, EPSILON, message + " padding right");
        assertEquals(top, layout.padding().top, EPSILON, message + " padding top");
        assertEquals(bottom, layout.padding().bottom, EPSILON, message + " padding bottom");
    }

    // === BK-08: percentage resolution basis (width and height) ===

    /**
     * Upstream fixture {@code block_percentage_height_with_min_height_parent} (#1122): a block
     * whose only definite height input is {@code min-height} does not become the percentage
     * resolution basis of its children. The 50% height child of a {@code min-height: 80px} box with
     * an automatic height resolves as {@code auto}, i.e. to its 20px content height — not to 40.
     */
    @Test
    @DisplayName("BK-08: min-height alone is not the percentage height basis of the children")
    void minHeightIsNotThePercentageHeightBasis() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle grandchildStyle = blockStyle();
        grandchildStyle.size = size(TaffyDimension.length(20f), TaffyDimension.length(20f));
        NodeId grandchild = tree.newLeaf(grandchildStyle);

        TaffyStyle childStyle = blockStyle();
        childStyle.size = size(TaffyDimension.AUTO, TaffyDimension.percent(0.5f));
        NodeId child = tree.newWithChildren(childStyle, grandchild);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(100f), TaffyDimension.AUTO);
        rootStyle.minSize = size(TaffyDimension.AUTO, TaffyDimension.length(80f));
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 100f, 80f, "root");
        assertLocation(tree.getLayout(child), 0f, 0f, "child");
        // height: 50% has no resolution basis (auto height parent), so it computes to auto = 20px
        assertSize(tree.getLayout(child), 100f, 20f, "child");
        assertLocation(tree.getLayout(grandchild), 0f, 0f, "grandchild");
        assertSize(tree.getLayout(grandchild), 20f, 20f, "grandchild");
    }

    /**
     * Upstream fixture {@code block_relative_inset_percentage_definite_height} (#1122): a
     * relatively positioned child's percentage vertical inset resolves against the containing
     * block's height when it is definite. {@code top: 50%} of a 200px tall container is 100px;
     * before the fix it always resolved against a hard-coded zero basis, so the child stayed at 0.
     */
    @Test
    @DisplayName("BK-08: percentage vertical insets resolve against a definite container height")
    void percentageVerticalInsetsUseDefiniteContainerHeight() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle childStyle = blockStyle();
        childStyle.position = TaffyPosition.RELATIVE;
        childStyle.size = size(TaffyDimension.length(20f), TaffyDimension.length(20f));
        childStyle.inset = new TaffyRect<>(
            LengthPercentageAuto.percent(0.1f),
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.percent(0.5f),
            LengthPercentageAuto.AUTO
        );
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(100f), TaffyDimension.length(200f));
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 100f, 200f, "root");
        // left: 10% of the 100px content box, top: 50% of the 200px content box height
        assertLocation(tree.getLayout(child), 10f, 100f, "child");
        assertSize(tree.getLayout(child), 20f, 20f, "child");
    }

    /**
     * Upstream fixture {@code block_relative_inset_percentage_auto_height} (#1122, control case):
     * with an automatic container height the percentage vertical inset has no basis and resolves
     * to zero. The section heading — zero, not 50% of the resolved content height.
     */
    @Test
    @DisplayName("BK-08: percentage vertical insets are ignored with an indefinite container height")
    void percentageVerticalInsetsIgnoredWithIndefiniteContainerHeight() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle childStyle = blockStyle();
        childStyle.position = TaffyPosition.RELATIVE;
        childStyle.size = size(TaffyDimension.length(20f), TaffyDimension.length(20f));
        childStyle.inset = new TaffyRect<>(
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.percent(0.5f),
            LengthPercentageAuto.AUTO
        );
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(100f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 100f, 20f, "root");
        assertLocation(tree.getLayout(child), 0f, 0f, "child");
    }

    /**
     * A block child's percentage padding and border resolve against its containing block's
     * <em>content box</em> width, which is what {@code parent_size.width} on the child layout input
     * is. #866(d) changed that input from the container's outer width to its inner width; resolving
     * against the outer width inflated the padding of a child of a padded box.
     */
    @Test
    @DisplayName("BK-08: child percentage padding resolves against the container content box width")
    void childPercentagePaddingUsesContainerContentBoxWidth() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle childStyle = blockStyle();
        childStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        childStyle.padding = TaffyRect.all(LengthPercentage.percent(0.1f));
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(200f), TaffyDimension.AUTO);
        rootStyle.padding = TaffyRect.all(LengthPercentage.length(20f));
        rootStyle.border = TaffyRect.all(LengthPercentage.length(5f));
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout rootLayout = tree.getLayout(root);
        assertSize(rootLayout, 200f, 80f, "root");
        Layout childLayout = tree.getLayout(child);
        // content box width = 200 - 2 * (20 padding + 5 border) = 150 -> 10% = 15 per edge
        assertLocation(childLayout, 25f, 25f, "child");
        assertSize(childLayout, 150f, 30f, "child");
        assertPadding(childLayout, 15f, 15f, 15f, 15f, "child");
    }

    /**
     * Combination case: a definite-height padded container gives its children a definite
     * percentage height basis (content box height) and percentage vertical insets, while the
     * children's own percentage padding resolves against the container content box width.
     */
    @Test
    @DisplayName("BK-08: percentage padding, height and inset combine on a padded container")
    void percentagePaddingHeightAndInsetCombine() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle childStyle = blockStyle();
        childStyle.position = TaffyPosition.RELATIVE;
        childStyle.size = size(TaffyDimension.AUTO, TaffyDimension.percent(0.5f));
        childStyle.padding = TaffyRect.all(LengthPercentage.percent(0.1f));
        childStyle.inset = new TaffyRect<>(
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.percent(0.1f),
            LengthPercentageAuto.AUTO
        );
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(200f), TaffyDimension.length(200f));
        rootStyle.padding = TaffyRect.all(LengthPercentage.length(20f));
        rootStyle.border = TaffyRect.all(LengthPercentage.length(5f));
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 200f, 200f, "root");
        Layout childLayout = tree.getLayout(child);
        // content box is 150x150: height 50% = 75, padding 10% = 15, top 10% = 15
        assertLocation(childLayout, 25f, 40f, "child");
        assertSize(childLayout, 150f, 75f, "child");
        assertPadding(childLayout, 15f, 15f, 15f, 15f, "child");
    }

    /**
     * {@code min-height} alone must not become the percentage resolution basis for the vertical
     * insets either: the container's 80px min-height is not a definite height, so {@code top: 50%}
     * stays at zero even though the used height ends up 80.
     */
    @Test
    @DisplayName("BK-08: min-height alone is not the percentage inset basis either")
    void minHeightIsNotThePercentageInsetBasis() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle childStyle = blockStyle();
        childStyle.position = TaffyPosition.RELATIVE;
        childStyle.size = size(TaffyDimension.length(20f), TaffyDimension.length(20f));
        childStyle.inset = new TaffyRect<>(
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.percent(0.5f),
            LengthPercentageAuto.AUTO
        );
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(100f), TaffyDimension.AUTO);
        rootStyle.minSize = size(TaffyDimension.AUTO, TaffyDimension.length(80f));
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 100f, 80f, "root");
        assertLocation(tree.getLayout(child), 0f, 0f, "child");
    }

    // === BK-09: margin collapse-through is decided by the used outer height ===

    /**
     * Upstream fixture {@code block_margin_y_collapse_through_height_clamped_by_max_height}
     * (#1188): a box whose declared {@code height} is 10px but whose used height is 0 (clamped by
     * {@code max-height: 0}) still collapses through. Judging this by the computed height blocked
     * the collapse and pushed the following sibling 10px down.
     */
    @Test
    @DisplayName("BK-09: max-height clamping the used height to 0 still collapses through")
    void usedHeightClampedToZeroCollapsesThrough() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle firstStyle = blockStyle();
        firstStyle.position = TaffyPosition.RELATIVE;
        firstStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        firstStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10f));
        NodeId first = tree.newLeaf(firstStyle);

        TaffyStyle emptyStyle = blockStyle();
        emptyStyle.position = TaffyPosition.RELATIVE;
        NodeId empty = tree.newLeaf(emptyStyle);

        TaffyStyle middleStyle = blockStyle();
        middleStyle.position = TaffyPosition.RELATIVE;
        middleStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        middleStyle.maxSize = size(TaffyDimension.AUTO, TaffyDimension.length(0f));
        middleStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(10f), LengthPercentageAuto.length(10f));
        NodeId middle = tree.newWithChildren(middleStyle, empty);

        TaffyStyle lastStyle = blockStyle();
        lastStyle.position = TaffyPosition.RELATIVE;
        lastStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        lastStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(10f), LengthPercentageAuto.ZERO);
        NodeId last = tree.newLeaf(lastStyle);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(50f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, first, middle, last);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 50f, 30f, "root");
        assertLocation(tree.getLayout(first), 0f, 0f, "first");
        assertSize(tree.getLayout(first), 50f, 10f, "first");
        // The collapsed-through box sits on top of the second row and has a used height of zero
        assertLocation(tree.getLayout(middle), 0f, 20f, "middle");
        assertSize(tree.getLayout(middle), 50f, 0f, "middle");
        assertLocation(tree.getLayout(empty), 0f, 0f, "empty");
        assertSize(tree.getLayout(empty), 50f, 0f, "empty");
        // The 10px margins on both sides of the collapsed-through box overlap this sibling's own
        assertLocation(tree.getLayout(last), 0f, 20f, "last");
        assertSize(tree.getLayout(last), 50f, 10f, "last");
    }

    /**
     * Upstream fixture {@code block_margin_y_collapse_through_percentage_min_height_indefinite_parent}
     * (#1188): a percentage {@code min-height} under an auto-height parent has no resolution basis,
     * so the used height stays 0 and the box collapses through.
     */
    @Test
    @DisplayName("BK-09: an unresolvable percentage min-height does not block collapse-through")
    void percentageMinHeightWithIndefiniteParentCollapsesThrough() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle firstStyle = blockStyle();
        firstStyle.position = TaffyPosition.RELATIVE;
        firstStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        firstStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10f));
        NodeId first = tree.newLeaf(firstStyle);

        TaffyStyle emptyStyle = blockStyle();
        emptyStyle.position = TaffyPosition.RELATIVE;
        NodeId empty = tree.newLeaf(emptyStyle);

        TaffyStyle middleStyle = blockStyle();
        middleStyle.position = TaffyPosition.RELATIVE;
        middleStyle.minSize = size(TaffyDimension.AUTO, TaffyDimension.percent(0.1f));
        middleStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(10f), LengthPercentageAuto.length(10f));
        NodeId middle = tree.newWithChildren(middleStyle, empty);

        TaffyStyle lastStyle = blockStyle();
        lastStyle.position = TaffyPosition.RELATIVE;
        lastStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        lastStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(10f), LengthPercentageAuto.ZERO);
        NodeId last = tree.newLeaf(lastStyle);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(50f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, first, middle, last);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 50f, 30f, "root");
        assertLocation(tree.getLayout(middle), 0f, 20f, "middle");
        assertSize(tree.getLayout(middle), 50f, 0f, "middle");
        assertLocation(tree.getLayout(last), 0f, 20f, "last");
    }

    /**
     * Upstream fixture {@code block_margin_y_collapse_through_blocked_by_min_height} (#1188
     * counter-example): {@code min-height: 1px} makes the used height non-zero, so the box does
     * not collapse through and its sibling follows below its 1px box.
     */
    @Test
    @DisplayName("BK-09: a non-zero used height from min-height still blocks collapse-through")
    void minHeightKeepingUsedHeightNonZeroBlocksCollapseThrough() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle firstStyle = blockStyle();
        firstStyle.position = TaffyPosition.RELATIVE;
        firstStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        firstStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10f));
        NodeId first = tree.newLeaf(firstStyle);

        TaffyStyle middleStyle = blockStyle();
        middleStyle.position = TaffyPosition.RELATIVE;
        middleStyle.minSize = size(TaffyDimension.AUTO, TaffyDimension.length(1f));
        middleStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(10f), LengthPercentageAuto.length(10f));
        NodeId middle = tree.newLeaf(middleStyle);

        TaffyStyle lastStyle = blockStyle();
        lastStyle.position = TaffyPosition.RELATIVE;
        lastStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        lastStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(10f), LengthPercentageAuto.ZERO);
        NodeId last = tree.newLeaf(lastStyle);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(50f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, first, middle, last);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 50f, 41f, "root");
        assertLocation(tree.getLayout(middle), 0f, 20f, "middle");
        assertSize(tree.getLayout(middle), 50f, 1f, "middle");
        // 20 + 1 (middle) + max(10, 10) margins
        assertLocation(tree.getLayout(last), 0f, 31f, "last");
        assertSize(tree.getLayout(last), 50f, 10f, "last");
    }

    /**
     * Counter-example from #1188: a zero used height does not collapse the margins through a box
     * that has block-start/end border or padding (only the block edges matter). The box keeps its
     * padding/border height and its margins stay separate from its siblings'.
     */
    @Test
    @DisplayName("BK-09: border or padding keeps a zero-height box from collapsing through")
    void zeroHeightWithBorderOrPaddingDoesNotCollapseThrough() {
        assertZeroHeightBoxWithEdgeInsetDoesNotCollapseThrough(true);
        assertZeroHeightBoxWithEdgeInsetDoesNotCollapseThrough(false);
    }

    private static void assertZeroHeightBoxWithEdgeInsetDoesNotCollapseThrough(boolean useBorder) {
        String kind = useBorder ? "border" : "padding";
        TaffyTree tree = new TaffyTree();

        TaffyStyle firstStyle = blockStyle();
        firstStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        NodeId first = tree.newLeaf(firstStyle);

        TaffyStyle middleStyle = blockStyle();
        middleStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(0f));
        middleStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(10f), LengthPercentageAuto.length(10f));
        if (useBorder) {
            middleStyle.border = new TaffyRect<>(
                LengthPercentage.ZERO, LengthPercentage.ZERO,
                LengthPercentage.ZERO, LengthPercentage.length(10f));
        } else {
            middleStyle.padding = new TaffyRect<>(
                LengthPercentage.ZERO, LengthPercentage.ZERO,
                LengthPercentage.ZERO, LengthPercentage.length(10f));
        }
        NodeId middle = tree.newLeaf(middleStyle);

        TaffyStyle lastStyle = blockStyle();
        lastStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        lastStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(10f), LengthPercentageAuto.ZERO);
        NodeId last = tree.newLeaf(lastStyle);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(50f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, first, middle, last);

        tree.computeLayout(root, TaffySize.maxContent());

        // 10 (first) + 10 (own top margin; the root does not collapse with its children)
        assertLocation(tree.getLayout(middle), 0f, 20f, kind);
        // used height = the 10px block-end inset, because the content height is zero
        assertSize(tree.getLayout(middle), 50f, 10f, kind);
        // 20 + 10 (used height) + max(10, 10) adjoining margins
        assertLocation(tree.getLayout(last), 0f, 40f, kind);
        assertSize(tree.getLayout(root), 50f, 50f, kind + " root");
    }

    /**
     * Control case for #1188: a box whose used height is zero (explicit {@code height: 0}) and
     * which has no border or padding still collapses through, so the margins around it overlap
     * and the following sibling moves up.
     */
    @Test
    @DisplayName("BK-09: a plain zero-height box collapses through")
    void plainZeroHeightBoxCollapsesThrough() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle firstStyle = blockStyle();
        firstStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        firstStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10f));
        NodeId first = tree.newLeaf(firstStyle);

        TaffyStyle middleStyle = blockStyle();
        middleStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(0f));
        middleStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(10f), LengthPercentageAuto.length(10f));
        NodeId middle = tree.newLeaf(middleStyle);

        TaffyStyle lastStyle = blockStyle();
        lastStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        lastStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(10f), LengthPercentageAuto.ZERO);
        NodeId last = tree.newLeaf(lastStyle);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(50f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, first, middle, last);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 50f, 30f, "root");
        assertLocation(tree.getLayout(middle), 0f, 20f, "middle");
        assertSize(tree.getLayout(middle), 50f, 0f, "middle");
        assertLocation(tree.getLayout(last), 0f, 20f, "last");
    }

    // === BK-17: display: flow-root ===

    /**
     * Upstream fixture {@code block_flow_root_margin_non_collapse} (#997): a {@code flow-root}
     * box always establishes a new block formatting context, so its 20px/30px child margins are
     * contained by it instead of collapsing with (and escaping through) its own block edges. The
     * plain block sibling in the same parent shows the old, collapsing behaviour.
     */
    @Test
    @DisplayName("BK-17: FLOW_ROOT contains its children's block margins")
    void flowRootMarginNonCollapse() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle blockGrandchildStyle = blockStyle();
        blockGrandchildStyle.position = TaffyPosition.RELATIVE;
        blockGrandchildStyle.size = size(TaffyDimension.length(40f), TaffyDimension.length(10f));
        blockGrandchildStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(20f), LengthPercentageAuto.length(30f));
        NodeId blockGrandchild = tree.newLeaf(blockGrandchildStyle);

        TaffyStyle blockChildStyle = blockStyle();
        blockChildStyle.position = TaffyPosition.RELATIVE;
        blockChildStyle.size = size(TaffyDimension.length(100f), TaffyDimension.AUTO);
        NodeId blockChild = tree.newWithChildren(blockChildStyle, blockGrandchild);

        TaffyStyle flowRootGrandchildStyle = blockStyle();
        flowRootGrandchildStyle.position = TaffyPosition.RELATIVE;
        flowRootGrandchildStyle.size = size(TaffyDimension.length(40f), TaffyDimension.length(10f));
        flowRootGrandchildStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(20f), LengthPercentageAuto.length(30f));
        NodeId flowRootGrandchild = tree.newLeaf(flowRootGrandchildStyle);

        TaffyStyle flowRootChildStyle = flowRootStyle();
        flowRootChildStyle.position = TaffyPosition.RELATIVE;
        flowRootChildStyle.size = size(TaffyDimension.length(100f), TaffyDimension.AUTO);
        NodeId flowRootChild = tree.newWithChildren(flowRootChildStyle, flowRootGrandchild);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(200f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, blockChild, flowRootChild);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 200f, 120f, "root");
        // The block child's 20px top margin collapses with its child's, so it is placed at y=20
        // and its own box is only 10px tall; the 30px bottom margin escapes below it.
        assertLocation(tree.getLayout(blockChild), 0f, 20f, "block child");
        assertSize(tree.getLayout(blockChild), 100f, 10f, "block child");
        assertLocation(tree.getLayout(blockGrandchild), 0f, 0f, "block grandchild");
        // The flow-root child contains both margins: 20 + 10 + 30 = 60px tall, placed at y=60.
        assertLocation(tree.getLayout(flowRootChild), 0f, 60f, "flow-root child");
        assertSize(tree.getLayout(flowRootChild), 100f, 60f, "flow-root child");
        assertLocation(tree.getLayout(flowRootGrandchild), 0f, 20f, "flow-root grandchild");
        assertSize(tree.getLayout(flowRootGrandchild), 40f, 10f, "flow-root grandchild");
    }

    /**
     * The observable difference between {@code flow-root} and {@code block} without any float
     * involvement: a block parent's top margin collapses with its first child's, a flow-root
     * parent's does not. The child therefore sits at y=0 inside a block parent placed at y=20,
     * while the flow-root parent starts at y=0 and the child at y=20.
     */
    @Test
    @DisplayName("BK-17: FLOW_ROOT and BLOCK differ observably in margin collapsing")
    void flowRootDiffersFromBlockByMarginCollapsing() {
        TaffyTree blockTree = new TaffyTree();
        TaffyStyle blockParentStyle = blockStyle();
        blockParentStyle.size = size(TaffyDimension.length(100f), TaffyDimension.AUTO);
        NodeId blockChild = blockTree.newLeaf(marginedChildStyle());
        NodeId blockParent = blockTree.newWithChildren(blockParentStyle, blockChild);
        NodeId blockRoot = blockTree.newWithChildren(blockRootStyle(), blockParent);
        blockTree.computeLayout(blockRoot, TaffySize.maxContent());

        // The parent's top margin escapes and carries the child's 20px margin: parent at y=20.
        assertLocation(blockTree.getLayout(blockParent), 0f, 20f, "block parent");
        assertSize(blockTree.getLayout(blockParent), 100f, 10f, "block parent");
        assertLocation(blockTree.getLayout(blockChild), 0f, 0f, "block child");

        TaffyTree flowRootTree = new TaffyTree();
        TaffyStyle flowRootParentStyle = flowRootStyle();
        flowRootParentStyle.size = size(TaffyDimension.length(100f), TaffyDimension.AUTO);
        NodeId flowRootChild = flowRootTree.newLeaf(marginedChildStyle());
        NodeId flowRootParent = flowRootTree.newWithChildren(flowRootParentStyle, flowRootChild);
        NodeId flowRootRoot = flowRootTree.newWithChildren(blockRootStyle(), flowRootParent);
        flowRootTree.computeLayout(flowRootRoot, TaffySize.maxContent());

        // The new BFC keeps both margins inside: parent at y=0 and 60px tall, child at y=20.
        assertLocation(flowRootTree.getLayout(flowRootParent), 0f, 0f, "flow-root parent");
        assertSize(flowRootTree.getLayout(flowRootParent), 100f, 60f, "flow-root parent");
        assertLocation(flowRootTree.getLayout(flowRootChild), 0f, 20f, "flow-root child");
        assertSize(flowRootTree.getLayout(flowRootChild), 100f, 10f, "flow-root child");
    }

    private static TaffyStyle marginedChildStyle() {
        TaffyStyle childStyle = blockStyle();
        childStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        childStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(20f), LengthPercentageAuto.length(30f));
        return childStyle;
    }

    /**
     * A zero-height flow-root box does not collapse through (it is not part of its parent's
     * formatting context), while an identical block box with a zero used height does. Both boxes
     * have a zero box height, so only the display mode explains the 10px difference.
     */
    @Test
    @DisplayName("BK-17: a zero-height FLOW_ROOT does not collapse through")
    void flowRootZeroHeightDoesNotCollapseThrough() {
        TaffyTree flowRootTree = new TaffyTree();
        NodeId flowRootFirst = flowRootTree.newLeaf(plainChildStyle());
        NodeId flowRootMiddle = flowRootTree.newLeaf(zeroHeightMiddleStyle(flowRootStyle()));
        NodeId flowRootLast = flowRootTree.newLeaf(siblingStyle());
        NodeId flowRootRoot = flowRootTree.newWithChildren(
            blockRootStyle(), flowRootFirst, flowRootMiddle, flowRootLast);
        flowRootTree.computeLayout(flowRootRoot, TaffySize.maxContent());

        assertLocation(flowRootTree.getLayout(flowRootFirst), 0f, 0f, "flow-root first");
        assertLocation(flowRootTree.getLayout(flowRootMiddle), 0f, 20f, "flow-root middle");
        assertSize(flowRootTree.getLayout(flowRootMiddle), 50f, 0f, "flow-root middle");
        assertLocation(flowRootTree.getLayout(flowRootLast), 0f, 30f, "flow-root last");
        assertEquals(40f, flowRootTree.getLayout(flowRootRoot).size().height, EPSILON,
            "flow-root root height");

        TaffyTree blockTree = new TaffyTree();
        NodeId blockFirst = blockTree.newLeaf(plainChildStyle());
        NodeId blockMiddle = blockTree.newLeaf(zeroHeightMiddleStyle(blockStyle()));
        NodeId blockLast = blockTree.newLeaf(siblingStyle());
        NodeId blockRoot = blockTree.newWithChildren(blockRootStyle(), blockFirst, blockMiddle, blockLast);
        blockTree.computeLayout(blockRoot, TaffySize.maxContent());

        assertLocation(blockTree.getLayout(blockFirst), 0f, 0f, "block first");

        assertLocation(blockTree.getLayout(blockMiddle), 0f, 20f, "block middle");
        assertSize(blockTree.getLayout(blockMiddle), 50f, 0f, "block middle");
        assertLocation(blockTree.getLayout(blockLast), 0f, 20f, "block last");
        assertEquals(30f, blockTree.getLayout(blockRoot).size().height, EPSILON,
            "block root height");
    }

    private static TaffyStyle plainChildStyle() {
        TaffyStyle style = blockStyle();
        style.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        return style;
    }

    private static TaffyStyle zeroHeightMiddleStyle(TaffyStyle displayStyle) {
        displayStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(0f));
        displayStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(10f), LengthPercentageAuto.length(10f));
        return displayStyle;
    }

    private static TaffyStyle siblingStyle() {
        TaffyStyle style = blockStyle();
        style.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        style.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(10f), LengthPercentageAuto.ZERO);
        return style;
    }

    private static TaffyStyle blockRootStyle() {
        TaffyStyle style = blockStyle();
        style.size = size(TaffyDimension.length(50f), TaffyDimension.AUTO);
        return style;
    }

    /**
     * {@code FLOW_ROOT} is laid out with the block layout algorithm: a flow-root root stretches to
     * the available width exactly like a block root, and its in-flow children stack on the block
     * axis. The enum value is additive: it is appended after {@link TaffyDisplay#NONE} and the
     * default display is still {@link TaffyDisplay#FLEX}, while {@code is_block} stays false for
     * flow-root (upstream documents that distinction for {@code CoreStyle::is_block}).
     */
    @Test
    @DisplayName("BK-17: FLOW_ROOT is an additive display value that uses block layout")
    void flowRootIsAdditiveAndUsesBlockLayout() {
        TaffyDisplay[] values = TaffyDisplay.values();
        assertEquals(5, values.length, "one additive enum constant");
        assertEquals(TaffyDisplay.BLOCK, values[0]);
        assertEquals(TaffyDisplay.FLEX, values[1]);
        assertEquals(TaffyDisplay.GRID, values[2]);
        assertEquals(TaffyDisplay.NONE, values[3]);
        assertEquals(TaffyDisplay.FLOW_ROOT, values[4], "FLOW_ROOT is appended after NONE");
        assertEquals(TaffyDisplay.FLEX, TaffyDisplay.DEFAULT);
        assertNotEquals(TaffyDisplay.BLOCK, TaffyDisplay.FLOW_ROOT);
        assertFalse(flowRootStyle().isBlock(), "is_block stays false for flow-root");
        assertTrue(blockStyle().isBlock(), "is_block is still true for block");

        // Both display modes lay the same tree out with the block algorithm: the root stretches
        // to the definite available width and the children stack on the block axis.
        assertBlockLayoutGeometry(twoChildTree(TaffyDisplay.FLOW_ROOT), "flow-root");
        assertBlockLayoutGeometry(twoChildTree(TaffyDisplay.BLOCK), "block");
    }

    /**
     * The tree dump labels a flow-root container {@code FLOW-ROOT} (upstream #997 adds the same
     * label to {@code PrintTree::get_debug_label}).
     */
    @Test
    @DisplayName("BK-17: the tree dump labels a flow-root container FLOW-ROOT")
    void flowRootDebugLabel() throws Exception {
        TaffyTree tree = new TaffyTree();
        TaffyStyle innerStyle = blockStyle();
        NodeId grandchild = tree.newLeaf(new TaffyStyle());
        NodeId inner = tree.newWithChildren(innerStyle, grandchild);
        NodeId root = tree.newWithChildren(flowRootStyle(), inner);
        tree.computeLayout(root, TaffySize.maxContent());

        StringWriter writer = new StringWriter();
        tree.writeTree(writer, root);
        String output = writer.toString();

        assertTrue(output.startsWith("[FLOW-ROOT] "), output);
        assertTrue(output.contains("[BLOCK] "), output);
        assertTrue(output.contains("[LEAF] "), output);
    }

    private record TwoChildTree(TaffyTree tree, NodeId root, NodeId first, NodeId second) {}

    private static TwoChildTree twoChildTree(TaffyDisplay display) {
        TaffyTree tree = new TaffyTree();

        TaffyStyle firstStyle = blockStyle();
        firstStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        NodeId first = tree.newLeaf(firstStyle);

        TaffyStyle secondStyle = blockStyle();
        secondStyle.size = size(TaffyDimension.length(50f), TaffyDimension.length(20f));
        NodeId second = tree.newLeaf(secondStyle);

        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = display;
        NodeId root = tree.newWithChildren(rootStyle, first, second);

        return new TwoChildTree(tree, root, first, second);
    }

    private static void assertBlockLayoutGeometry(TwoChildTree built, String message) {
        built.tree().computeLayout(built.root(), new TaffySize<>(
            AvailableSpace.definite(80f), AvailableSpace.maxContent()));

        assertSize(built.tree().getLayout(built.root()), 80f, 30f, message + " root");
        assertLocation(built.tree().getLayout(built.first()), 0f, 0f, message + " first");
        assertSize(built.tree().getLayout(built.first()), 80f, 10f, message + " first");
        assertLocation(built.tree().getLayout(built.second()), 0f, 10f, message + " second");
        assertSize(built.tree().getLayout(built.second()), 50f, 20f, message + " second");
    }
}
