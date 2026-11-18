package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.LayoutComputer;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.SizingMode;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P1 regression tests for block layout correctness fixes (BK-01..BK-07, BK-10, BK-11, BK-15, BK-21).
 *
 * <p>Each test mirrors the upstream taffy fixture that accompanied the corresponding fix, using
 * the browser-computed expectations from {@code tests/xml/block/**} as the oracle. Fixtures whose
 * root is a {@code display:block} {@code position:relative} box are reproduced with a block root;
 * the outer {@code #test-root} wrapper is only needed by the browser harness.</p>
 */
public class BlockP1Test {

    private static final float EPSILON = 0.001f;

    // === Helpers ===

    private static TaffyStyle blockStyle() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
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

    // === BK-06: percentage padding/border resolve against the containing block width ===

    /**
     * Upstream fixture {@code block_padding_percentage_nonstretch_width} (#1083).
     *
     * <p>A 100px wide block with {@code padding: 10%} inside a 40px wide block: percentage
     * padding resolves against the containing block width (40 -> 4px per side), not the box's own
     * width (which would give 10px per side).</p>
     */
    @Test
    @DisplayName("BK-06: percentage padding resolves against the containing block width")
    void percentagePaddingUsesContainingBlockWidth() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle innerStyle = blockStyle();
        innerStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        NodeId inner = tree.newLeaf(innerStyle);

        TaffyStyle childStyle = blockStyle();
        childStyle.size = size(TaffyDimension.length(100f), TaffyDimension.AUTO);
        childStyle.padding = TaffyRect.all(LengthPercentage.percent(0.1f));
        NodeId child = tree.newWithChildren(childStyle, inner);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(40f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout rootLayout = tree.getLayout(root);
        assertLocation(rootLayout, 0f, 0f, "root");
        assertSize(rootLayout, 40f, 18f, "root");

        Layout childLayout = tree.getLayout(child);
        assertLocation(childLayout, 0f, 0f, "child");
        // height = 4 (top padding) + 10 (inner) + 4 (bottom padding)
        assertSize(childLayout, 100f, 18f, "child");

        Layout innerLayout = tree.getLayout(inner);
        assertLocation(innerLayout, 4f, 4f, "inner");
        // width = 100 - 4 - 4
        assertSize(innerLayout, 92f, 10f, "inner");
    }

    // === BK-21: percentage margins resolve against the containing block's inner width ===

    /**
     * A block child's percentage margins (all four sides) resolve against the containing block's
     * inline size, which is the parent's *content box* width, not its outer width. Here the root
     * is 100 wide with 20px padding, so the containing block width is 60: {@code margin: 10%}
     * resolves to 6, not to 10. Port of the percentage-margin part of upstream #990 (86d93d95);
     * the float/clearance parts of that commit do not apply (Java has no float layout).
     */
    @Test
    @DisplayName("BK-21: percentage margins resolve against the container inner width")
    void percentageMarginsUseContainerInnerWidth() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle childStyle = blockStyle();
        childStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        childStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.percent(0.1f),
            LengthPercentageAuto.ZERO,
            LengthPercentageAuto.percent(0.1f),
            LengthPercentageAuto.ZERO
        );
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(100f), TaffyDimension.AUTO);
        rootStyle.padding = TaffyRect.all(LengthPercentage.length(20f));
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout rootLayout = tree.getLayout(root);
        assertLocation(rootLayout, 0f, 0f, "root");
        // 20 (top padding) + 6 (margin) + 10 (child) + 20 (bottom padding)
        assertSize(rootLayout, 100f, 56f, "root");

        Layout childLayout = tree.getLayout(child);
        // x = 20 (left padding) + 6 (10% of the 60px containing block width)
        assertLocation(childLayout, 26f, 26f, "child");
        // auto width fills the containing block minus the non-auto horizontal margin
        assertSize(childLayout, 54f, 10f, "child");
    }

    // === BK-05: min-height determining the used height stops bottom-margin collapsing ===

    /**
     * Upstream fixture {@code block_margin_y_last_child_collapse_blocked_by_min_height} (#1082).
     *
     * <p>The outer box's used height (40) comes from {@code min-height}, so its last child's
     * 20px bottom margin must not collapse with its own 10px bottom margin; the sibling is placed
     * after the 10px margin (not after the 20px one).</p>
     */
    @Test
    @DisplayName("BK-05: min-height determining the used height blocks bottom-margin collapsing")
    void minHeightDeterminingUsedHeightBlocksBottomMarginCollapse() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle grandchildStyle = blockStyle();
        grandchildStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        grandchildStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.length(20f));
        NodeId grandchild = tree.newLeaf(grandchildStyle);

        TaffyStyle childStyle = blockStyle();
        childStyle.minSize = size(TaffyDimension.AUTO, TaffyDimension.length(40f));
        childStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10f));
        NodeId child = tree.newWithChildren(childStyle, grandchild);

        TaffyStyle siblingStyle = blockStyle();
        siblingStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        NodeId sibling = tree.newLeaf(siblingStyle);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(50f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, child, sibling);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 50f, 60f, "root");
        assertLocation(tree.getLayout(child), 0f, 0f, "child");
        assertSize(tree.getLayout(child), 50f, 40f, "child");
        assertLocation(tree.getLayout(grandchild), 0f, 0f, "grandchild");
        assertSize(tree.getLayout(grandchild), 50f, 10f, "grandchild");
        // 40 (child) + 10 (child's own bottom margin, the collapsed-through 20px one is inside)
        assertLocation(tree.getLayout(sibling), 0f, 50f, "sibling");
        assertSize(tree.getLayout(sibling), 50f, 10f, "sibling");
    }

    /**
     * Upstream fixture {@code block_margin_y_last_child_collapse_not_blocked_by_smaller_min_height}
     * (#1082, control case): when {@code min-height} is smaller than the content height the last
     * child's bottom margin still collapses with the box's own bottom margin.
     */
    @Test
    @DisplayName("BK-05: a min-height smaller than the used height still collapses the bottom margin")
    void smallerMinHeightStillCollapsesBottomMargin() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle grandchildStyle = blockStyle();
        grandchildStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(20f));
        grandchildStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.length(30f));
        NodeId grandchild = tree.newLeaf(grandchildStyle);

        TaffyStyle childStyle = blockStyle();
        childStyle.minSize = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        childStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10f));
        NodeId child = tree.newWithChildren(childStyle, grandchild);

        TaffyStyle siblingStyle = blockStyle();
        siblingStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        NodeId sibling = tree.newLeaf(siblingStyle);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(50f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, child, sibling);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 50f, 60f, "root");
        assertSize(tree.getLayout(child), 50f, 20f, "child");
        assertSize(tree.getLayout(grandchild), 50f, 20f, "grandchild");
        // 20 (child) + 30 (max of the adjoining 30/10 margins)
        assertLocation(tree.getLayout(sibling), 0f, 50f, "sibling");
    }

    /**
     * Upstream fixture {@code block_margin_y_collapse_through_child_blocked_by_parent_min_height}
     * (#1082): the child's only grandchild collapses through, its 20px bottom margin stays inside
     * the min-height sized parent, so the sibling follows at y = 50.
     */
    @Test
    @DisplayName("BK-05: a collapsed-through child margin does not escape a min-height parent")
    void collapsedThroughChildMarginBlockedByParentMinHeight() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle grandchildStyle = blockStyle();
        grandchildStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.length(20f));
        NodeId grandchild = tree.newLeaf(grandchildStyle);

        TaffyStyle childStyle = blockStyle();
        childStyle.minSize = size(TaffyDimension.AUTO, TaffyDimension.length(30f));
        NodeId child = tree.newWithChildren(childStyle, grandchild);

        TaffyStyle siblingStyle = blockStyle();
        siblingStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        NodeId sibling = tree.newLeaf(siblingStyle);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(50f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, child, sibling);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 50f, 60f, "root");
        // 20 is the min-height parent's own top margin set, which is the collapsed-through
        // grandchild's 20px bottom margin
        assertLocation(tree.getLayout(child), 0f, 20f, "child");
        assertSize(tree.getLayout(child), 50f, 30f, "child");
        assertLocation(tree.getLayout(grandchild), 0f, 0f, "grandchild");
        assertSize(tree.getLayout(grandchild), 50f, 0f, "grandchild");
        assertLocation(tree.getLayout(sibling), 0f, 50f, "sibling");
    }

    // === BK-11: a collapsed-through first child's margin is not added to the bottom ===

    /**
     * Upstream fixture {@code block_margin_y_first_child_collapse_through_with_border_bottom}
     * (#1397). The inner box has no height and no top border/padding but a 10px bottom border, so
     * its first (only) child collapses through and its margins are folded into the box's top
     * margin set. They must not be added to the box's bottom as well.
     */
    @Test
    @DisplayName("BK-11: collapsed-through first-child margin is not added to the box's bottom")
    void collapsedThroughFirstChildMarginNotAddedToBottom() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle grandchildStyle = blockStyle();
        grandchildStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(10f), LengthPercentageAuto.length(10f));
        NodeId grandchild = tree.newLeaf(grandchildStyle);

        TaffyStyle childStyle = blockStyle();
        childStyle.border = new TaffyRect<>(
            LengthPercentage.ZERO, LengthPercentage.ZERO,
            LengthPercentage.ZERO, LengthPercentage.length(10f));
        NodeId child = tree.newWithChildren(childStyle, grandchild);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(50f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        // 10 (collapsed-through top margin) + 10 (bottom border)
        assertSize(tree.getLayout(root), 50f, 20f, "root");
        assertLocation(tree.getLayout(child), 0f, 10f, "child");
        assertSize(tree.getLayout(child), 50f, 10f, "child");
        assertLocation(tree.getLayout(grandchild), 0f, 0f, "grandchild");
        assertSize(tree.getLayout(grandchild), 50f, 0f, "grandchild");
    }

    // === BK-10: in-flow children report their own margins ===

    /**
     * Port of upstream {@code tests/hand_written/block_margin.rs}
     * ({@code layout_margin_excludes_collapsed_margins}, #1287): margin collapsing changes where
     * boxes are placed, but {@code Layout.margin} is each box's own margin.
     *
     * <p>The upstream test uses a {@code display: flow-root} root (which this port does not have
     * yet); a block root behaves identically here because the root's own margins are marked
     * non-collapsible by the layout entry point.</p>
     */
    @Test
    @DisplayName("BK-10: Layout.margin reports each in-flow child's own margins")
    void layoutMarginExcludesCollapsedMargins() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle innerStyle = blockStyle();
        innerStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        innerStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(13f), LengthPercentageAuto.length(20f));
        NodeId inner = tree.newLeaf(innerStyle);

        TaffyStyle outerStyle = blockStyle();
        outerStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(8f), LengthPercentageAuto.length(5f));
        NodeId outer = tree.newWithChildren(outerStyle, inner);

        TaffyStyle siblingStyle = blockStyle();
        siblingStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        siblingStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(30f), LengthPercentageAuto.ZERO);
        NodeId sibling = tree.newLeaf(siblingStyle);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(100f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, outer, sibling);

        tree.computeLayout(root, TaffySize.maxContent());

        // The inner box's margins collapse through the outer box's edges
        Layout outerLayout = tree.getLayout(outer);
        assertLocation(outerLayout, 0f, 13f, "outer");
        assertSize(outerLayout, 100f, 10f, "outer");
        assertEquals(8f, outerLayout.margin().top, EPSILON, "outer margin top");
        assertEquals(5f, outerLayout.margin().bottom, EPSILON, "outer margin bottom");

        Layout innerLayout = tree.getLayout(inner);
        assertLocation(innerLayout, 0f, 0f, "inner");
        assertEquals(13f, innerLayout.margin().top, EPSILON, "inner margin top");
        assertEquals(20f, innerLayout.margin().bottom, EPSILON, "inner margin bottom");

        // The sibling is placed after the largest of the adjoining margins (5, 20, 30)
        Layout siblingLayout = tree.getLayout(sibling);
        assertLocation(siblingLayout, 0f, 53f, "sibling");
        assertEquals(30f, siblingLayout.margin().top, EPSILON, "sibling margin top");
    }

    // === BK-01: measurement must not write layout state to the tree ===

    /**
     * Upstream #971: a block container must not {@code set_unrounded_layout} on its in-flow
     * children while it is only being measured ({@code RunMode.COMPUTE_SIZE}). Before the fix the
     * in-flow layouts were written unconditionally, so every intrinsic-size probe overwrote the
     * committed layout with the probe's (content-sized) values.
     */
    @Test
    @DisplayName("BK-01: measuring a block container does not overwrite its children's layouts")
    void measurePassDoesNotWriteChildLayouts() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle leafStyle = blockStyle();
        leafStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        NodeId leaf = tree.newLeaf(leafStyle);

        TaffyStyle containerStyle = blockStyle();
        NodeId container = tree.newWithChildren(containerStyle, leaf);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(200f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, container);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout leafBefore = tree.getUnroundedLayout(leaf);
        Layout containerBefore = tree.getUnroundedLayout(container);
        assertEquals(200f, leafBefore.size().width, EPSILON, "leaf is stretched by the final layout");

        // Measure the container the way an intrinsic-size consumer would: content based width,
        // ComputeSize run mode, no full layout requested.
        LayoutComputer computer = new LayoutComputer(tree, (dev.vfyjxf.taffy.util.MeasureFunc) null);
        FloatSize measured = computer.measureChildSize(
            container,
            new FloatSize(Float.NaN, Float.NaN),
            new FloatSize(200f, Float.NaN),
            new TaffySize<>(AvailableSpace.definite(200f), AvailableSpace.MAX_CONTENT),
            SizingMode.INHERENT_SIZE,
            TaffyLine.FALSE
        );
        assertEquals(0f, measured.width, EPSILON, "content-based measured width of the empty leaf");

        assertEquals(leafBefore, tree.getUnroundedLayout(leaf), "measuring must not write the leaf layout");
        assertEquals(containerBefore, tree.getUnroundedLayout(container),
            "measuring must not write the container layout");
    }

    // === BK-02: a measuring block passes ComputeSize to its in-flow children ===

    /**
     * Upstream #972: a layout algorithm that is itself being measured must pass
     * {@code RunMode::ComputeSize} down to its children. Otherwise the children perform a real
     * layout and commit layout state to the tree during a measurement pass (defeating BK-01), and
     * they skip the margin-collapse reporting a vertical ComputeSize pass exists to provide
     * (BK-03).
     *
     * <p>The probe uses a narrower known width (100 instead of the tree's 200) so the probe's
     * computed child layouts differ from the committed ones: without the run-mode propagation the
     * nested block container writes those narrower layouts during the measurement.</p>
     */
    @Test
    @DisplayName("BK-02: a measured block lays its in-flow children out in ComputeSize mode")
    void measurePassPropagatesRunModeToInFlowChildren() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle leafStyle = blockStyle();
        leafStyle.size = size(TaffyDimension.AUTO, TaffyDimension.length(10f));
        leafStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(10f), LengthPercentageAuto.length(20f));
        NodeId leaf = tree.newLeaf(leafStyle);

        TaffyStyle innerStyle = blockStyle();
        NodeId inner = tree.newWithChildren(innerStyle, leaf);

        TaffyStyle outerStyle = blockStyle();
        outerStyle.padding = TaffyRect.all(LengthPercentage.length(5f));
        NodeId outer = tree.newWithChildren(outerStyle, inner);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(200f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, outer);

        tree.computeLayout(root, TaffySize.maxContent());

        // 5 (padding top) + 10 (margins) + 10 (leaf) + 20 (margins) + 5 (padding bottom)
        assertSize(tree.getLayout(outer), 200f, 50f, "outer");
        assertEquals(190f, tree.getLayout(inner).size().width, EPSILON, "inner is stretched");

        Layout outerBefore = tree.getUnroundedLayout(outer);
        Layout innerBefore = tree.getUnroundedLayout(inner);
        Layout leafBefore = tree.getUnroundedLayout(leaf);
        assertEquals(190f, leafBefore.size().width, EPSILON, "leaf is stretched by the final layout");

        // Measure the outer block with a *narrower* known width, as an intrinsic-size consumer
        // would: the probe's inner layouts would be 90px wide instead of the committed 190px.
        LayoutComputer computer = new LayoutComputer(tree, (dev.vfyjxf.taffy.util.MeasureFunc) null);
        computer.measureChildSize(
            outer,
            new FloatSize(100f, Float.NaN),
            new FloatSize(200f, Float.NaN),
            new TaffySize<>(AvailableSpace.definite(100f), AvailableSpace.MAX_CONTENT),
            SizingMode.INHERENT_SIZE,
            TaffyLine.FALSE
        );

        assertEquals(outerBefore, tree.getUnroundedLayout(outer), "outer layout must not be written");
        assertEquals(innerBefore, tree.getUnroundedLayout(inner), "inner layout must not be written");
        assertEquals(leafBefore, tree.getUnroundedLayout(leaf), "leaf layout must not be written");
    }

    // === BK-07: abspos auto margins resolve against the used (clamped) size ===

    /**
     * Upstream fixture {@code block_absolute_margin_auto_left_and_right_max_width} (#1096): the
     * declared width is 180 but {@code max-width} clamps the used width to 100, so the two auto
     * horizontal margins split the remaining 100px (50 each) instead of collapsing to 0.
     */
    @Test
    @DisplayName("BK-07: abspos auto left/right margins centre a max-width clamped box")
    void absoluteAutoMarginsUseClampedWidth() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.position = TaffyPosition.ABSOLUTE;
        childStyle.size = size(TaffyDimension.length(180f), TaffyDimension.length(40f));
        childStyle.maxSize = size(TaffyDimension.length(100f), TaffyDimension.AUTO);
        childStyle.inset = new TaffyRect<>(
            LengthPercentageAuto.length(0f), LengthPercentageAuto.length(0f),
            LengthPercentageAuto.length(20f), LengthPercentageAuto.AUTO);
        childStyle.margin = TaffyRect.all(LengthPercentageAuto.auto());
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.position = TaffyPosition.RELATIVE;
        rootStyle.size = size(TaffyDimension.length(200f), TaffyDimension.length(100f));
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout childLayout = tree.getLayout(child);
        assertLocation(childLayout, 50f, 20f, "child");
        assertSize(childLayout, 100f, 40f, "child");
    }

    /**
     * Upstream fixture {@code block_absolute_margin_auto_top_and_bottom_max_height} (#1096): same
     * rule on the vertical axis with {@code max-height}.
     */
    @Test
    @DisplayName("BK-07: abspos auto top/bottom margins centre a max-height clamped box")
    void absoluteAutoMarginsUseClampedHeight() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.position = TaffyPosition.ABSOLUTE;
        childStyle.size = size(TaffyDimension.length(100f), TaffyDimension.length(90f));
        childStyle.maxSize = size(TaffyDimension.AUTO, TaffyDimension.length(40f));
        childStyle.inset = new TaffyRect<>(
            LengthPercentageAuto.length(20f), LengthPercentageAuto.AUTO,
            LengthPercentageAuto.length(0f), LengthPercentageAuto.length(0f));
        childStyle.margin = TaffyRect.all(LengthPercentageAuto.auto());
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = blockStyle();
        rootStyle.position = TaffyPosition.RELATIVE;
        rootStyle.size = size(TaffyDimension.length(200f), TaffyDimension.length(100f));
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout childLayout = tree.getLayout(child);
        assertLocation(childLayout, 20f, 30f, "child");
        assertSize(childLayout, 100f, 40f, "child");
    }
}
