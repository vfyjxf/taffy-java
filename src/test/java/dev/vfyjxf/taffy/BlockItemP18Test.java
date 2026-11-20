package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyFloat;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P18 pinning tests for the block item margin classification.
 *
 * <p><b>Scope.</b> {@code BlockComputer} now classifies each child's four margins once, when the
 * (cached) {@code BlockItem} list is built: a {@code length} keeps its value, an {@code auto} or
 * intrinsic-keyword component is stored as {@link Float#NaN}, and only a {@code percent} or
 * {@code calc()} component is left for layout-time resolution against the container's content-box
 * width. {@code performFinalLayoutOnChildren} therefore reads four primitives per child and never
 * asks the style object again unless a {@code percent}/{@code calc()} side actually needs it.
 *
 * <p>These tests pin the observable contract that refactor must preserve: fixed margins never pick
 * up the container width, percentage margins always resolve against the current pass's content box
 * (including a second pass at a different width), auto margins still absorb the inline free space,
 * and the float, out-of-flow, RTL and margin-collapse consumers see the same values as before.
 * They must pass unchanged against the pre-P18 implementation.</p>
 */
public class BlockItemP18Test {

    private static final float EPSILON = 0.001f;

    // === Builders ===

    private static TaffyStyle block() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        return style;
    }

    private static TaffyStyle sized(float width, float height) {
        TaffyStyle style = block();
        style.size = new TaffySize<>(
            Float.isNaN(width) ? TaffyDimension.AUTO : TaffyDimension.length(width),
            Float.isNaN(height) ? TaffyDimension.AUTO : TaffyDimension.length(height)
        );
        return style;
    }

    private static void padding(TaffyStyle style, float value) {
        style.padding = TaffyRect.all(LengthPercentage.length(value));
    }

    private static void margin(TaffyStyle style, float value) {
        style.margin = TaffyRect.all(LengthPercentageAuto.length(value));
    }

    private static TaffySize<AvailableSpace> definite(float width) {
        return TaffySize.of(AvailableSpace.definite(width), AvailableSpace.maxContent());
    }

    // === Assertions ===

    private static void assertLocation(Layout layout, float x, float y, String message) {
        assertEquals(x, layout.location().x, EPSILON, message + " x");
        assertEquals(y, layout.location().y, EPSILON, message + " y");
    }

    private static void assertSize(Layout layout, float width, float height, String message) {
        assertEquals(width, layout.size().width, EPSILON, message + " width");
        assertEquals(height, layout.size().height, EPSILON, message + " height");
    }

    private static void assertMargin(
        Layout layout, float left, float right, float top, float bottom, String message) {
        assertEquals(left, layout.margin().left, EPSILON, message + " left margin");
        assertEquals(right, layout.margin().right, EPSILON, message + " right margin");
        assertEquals(top, layout.margin().top, EPSILON, message + " top margin");
        assertEquals(bottom, layout.margin().bottom, EPSILON, message + " bottom margin");
    }

    // === Fixed margins: pre-resolved, basis-independent ===

    @Test
    @DisplayName("a fixed margin keeps its value when the container is laid out at a new width")
    void fixedMarginsDoNotDependOnTheContainerWidth() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(200f, Float.NaN);
        TaffyStyle childStyle = sized(Float.NaN, 10f);
        margin(childStyle, 10f);

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{child});
        tree.computeLayout(root, definite(200f));

        Layout first = tree.getLayout(child);
        assertMargin(first, 10f, 10f, 10f, 10f, "fixed margin (200 wide)");
        assertSize(first, 180f, 10f, "fixed margin box (200 wide)");
        assertLocation(first, 10f, 10f, "fixed margin position (200 wide)");

        // Second pass: the item list is rebuilt against a wider container, but a length margin is
        // still exactly its own value (it is pre-resolved at item-build time and must not be
        // re-derived from the new content-box width).
        TaffyStyle widerRootStyle = sized(300f, Float.NaN);
        tree.setStyle(root, widerRootStyle);
        tree.computeLayout(root, definite(300f));

        Layout second = tree.getLayout(child);
        assertMargin(second, 10f, 10f, 10f, 10f, "fixed margin (300 wide)");
        assertSize(second, 280f, 10f, "fixed margin box (300 wide)");
        assertLocation(second, 10f, 10f, "fixed margin position (300 wide)");
    }

    // === Percentage margins: still resolved per pass against the content box ===

    @Test
    @DisplayName("a percentage margin resolves against each pass's content-box width")
    void percentageMarginsResolveAgainstTheCurrentContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(200f, Float.NaN);
        padding(rootStyle, 20f);

        TaffyStyle childStyle = sized(Float.NaN, 10f);
        childStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.percent(0.10f), LengthPercentageAuto.percent(0.10f),
            LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.percent(0.05f));

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{child});
        tree.computeLayout(root, definite(200f));

        // 10% / 5% of the 160px content box: 16 horizontally, 8 vertically.
        Layout first = tree.getLayout(child);
        assertMargin(first, 16f, 16f, 8f, 8f, "percentage margin (content box 160)");
        assertSize(first, 128f, 10f, "percentage margin box (content box 160)");
        assertLocation(first, 36f, 28f, "percentage margin position (content box 160)");

        // Same child, container 240 wide: the content box is 200 and the percentages must follow.
        TaffyStyle widerRootStyle = sized(240f, Float.NaN);
        padding(widerRootStyle, 20f);
        tree.setStyle(root, widerRootStyle);
        tree.computeLayout(root, definite(240f));

        Layout second = tree.getLayout(child);
        assertMargin(second, 20f, 20f, 10f, 10f, "percentage margin (content box 200)");
        assertSize(second, 160f, 10f, "percentage margin box (content box 200)");
        assertLocation(second, 40f, 30f, "percentage margin position (content box 200)");
    }

    @Test
    @DisplayName("percentage and fixed margins coexist on one item")
    void percentageAndFixedMarginsCoexist() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(200f, Float.NaN);
        TaffyStyle childStyle = sized(100f, 10f);
        childStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.percent(0.10f), LengthPercentageAuto.length(5f),
            LengthPercentageAuto.length(4f), LengthPercentageAuto.AUTO);

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{child});
        tree.computeLayout(root, definite(200f));

        Layout layout = tree.getLayout(child);
        // 10% of 200 = 20 on the left, a plain 5 on the right, 4 on top, auto resolves to zero.
        assertMargin(layout, 20f, 5f, 4f, 0f, "mixed margins");
        assertLocation(layout, 20f, 4f, "mixed margin position");
    }

    // === Auto margins ===

    @Test
    @DisplayName("auto margins still absorb the inline free space")
    void autoMarginsAbsorbFreeSpace() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(300f, Float.NaN);
        TaffyStyle childStyle = sized(100f, 10f);
        childStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{child});
        tree.computeLayout(root, definite(300f));

        Layout layout = tree.getLayout(child);
        assertMargin(layout, 100f, 100f, 0f, 0f, "symmetric auto margins");
        assertLocation(layout, 100f, 0f, "symmetric auto margin position");
    }

    @Test
    @DisplayName("one auto margin absorbs the free space left by a fixed and a percentage margin")
    void autoMarginNextToFixedAndPercentageMargins() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(300f, Float.NaN);
        TaffyStyle childStyle = sized(100f, 10f);
        childStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.10f),
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{child});
        tree.computeLayout(root, definite(300f));

        Layout layout = tree.getLayout(child);
        // The percentage side resolves at layout time (30), the auto side takes the remainder.
        assertMargin(layout, 170f, 30f, 0f, 0f, "auto margin beside a percentage margin");
        assertLocation(layout, 170f, 0f, "auto margin beside a percentage margin position");
    }

    // === RTL ===

    @Test
    @DisplayName("RTL resolves percentage margins against the content box and mirrors the box")
    void rtlPercentageMarginsResolveAgainstTheContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(200f, Float.NaN);
        rootStyle.direction = TaffyDirection.RTL;
        padding(rootStyle, 20f);

        TaffyStyle percentageStyle = sized(100f, 10f);
        percentageStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.percent(0.10f), LengthPercentageAuto.percent(0.10f),
            LengthPercentageAuto.percent(0.10f), LengthPercentageAuto.percent(0.10f));

        TaffyStyle fixedStyle = sized(100f, 10f);
        margin(fixedStyle, 10f);

        NodeId percentageChild = tree.newLeaf(percentageStyle);
        NodeId fixedChild = tree.newLeaf(fixedStyle);
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{percentageChild, fixedChild});
        tree.computeLayout(root, definite(200f));

        // Content box 160: 10% is 16 per side. RTL right-aligns: 20 + (160 - 100 - 32) + 16 = 64.
        Layout percentageLayout = tree.getLayout(percentageChild);
        assertMargin(percentageLayout, 16f, 16f, 16f, 16f, "RTL percentage margin");
        assertLocation(percentageLayout, 64f, 36f, "RTL percentage margin position");

        // A fixed margin under the same RTL rules: 20 + (160 - 100 - 20) + 10 = 70.
        Layout fixedLayout = tree.getLayout(fixedChild);
        assertMargin(fixedLayout, 10f, 10f, 10f, 10f, "RTL fixed margin");
        assertLocation(fixedLayout, 70f, 62f, "RTL fixed margin position");
    }

    // === Floats ===

    @Test
    @DisplayName("a floated box keeps its fixed margins and its percentage margins")
    void floatedBoxUsesPreResolvedAndPercentageMargins() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(300f, 100f);

        TaffyStyle fixedFloatStyle = sized(50f, 20f);
        fixedFloatStyle.float_ = TaffyFloat.LEFT;
        margin(fixedFloatStyle, 10f);

        TaffyStyle percentageFloatStyle = sized(50f, 20f);
        percentageFloatStyle.float_ = TaffyFloat.LEFT;
        percentageFloatStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.percent(0.10f), LengthPercentageAuto.percent(0.10f),
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);

        NodeId fixedFloat = tree.newLeaf(fixedFloatStyle);
        NodeId percentageFloat = tree.newLeaf(percentageFloatStyle);
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{fixedFloat, percentageFloat});
        tree.computeLayout(root, definite(300f));

        // The fixed float is placed with its 10px margin box at the content-box origin.
        Layout fixedLayout = tree.getLayout(fixedFloat);
        assertMargin(fixedLayout, 10f, 10f, 10f, 10f, "floated fixed margins");
        assertLocation(fixedLayout, 10f, 10f, "floated fixed margin position");

        // The percentage float's horizontal margins resolve against the container content box
        // (30 each): its margin box starts after the first float's margin box (70) and the border
        // box is pushed right by its own left margin.
        Layout percentageLayout = tree.getLayout(percentageFloat);
        assertMargin(percentageLayout, 30f, 30f, 0f, 0f, "floated percentage margins");
        assertLocation(percentageLayout, 100f, 0f, "floated percentage margin position");
    }

    // === Out-of-flow children ===

    @Test
    @DisplayName("an absolutely positioned child resolves its percentage margin per pass")
    void absolutelyPositionedChildResolvesPercentageMarginsPerPass() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(200f, 100f);
        TaffyStyle childStyle = sized(20f, 10f);
        childStyle.position = TaffyPosition.ABSOLUTE;
        childStyle.inset = new TaffyRect<>(
            LengthPercentageAuto.length(0f), LengthPercentageAuto.AUTO,
            LengthPercentageAuto.length(0f), LengthPercentageAuto.AUTO);
        childStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.percent(0.10f), LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);

        NodeId child = tree.newLeaf(childStyle);
        NodeId container = tree.newWithChildren(rootStyle, new NodeId[]{child});
        tree.computeLayout(container, definite(200f));

        // 10% of the 200px positioning area: the used left margin is 20.
        Layout first = tree.getLayout(child);
        assertMargin(first, 20f, 0f, 0f, 0f, "abspos percentage margin");
        assertLocation(first, 20f, 0f, "abspos percentage margin position");

        TaffyStyle widerRootStyle = sized(400f, 100f);
        tree.setStyle(container, widerRootStyle);
        tree.computeLayout(container, definite(400f));

        Layout second = tree.getLayout(child);
        assertMargin(second, 40f, 0f, 0f, 0f, "abspos percentage margin (400 wide)");
        assertLocation(second, 40f, 0f, "abspos percentage margin position (400 wide)");
    }

    // === Margin collapse ===

    @Test
    @DisplayName("pre-resolved margins still collapse through a nested block's edges")
    void childMarginsCollapseThroughTheContainerEdges() {
        TaffyTree tree = new TaffyTree();

        // The root has no border/padding, so the middle block's collapsed margins pass through to
        // it; the middle block is the container whose item margins are classified.
        TaffyStyle rootStyle = sized(200f, Float.NaN);
        TaffyStyle middleStyle = block();
        TaffyStyle firstStyle = sized(Float.NaN, 10f);
        margin(firstStyle, 10f);
        TaffyStyle secondStyle = sized(Float.NaN, 10f);
        margin(secondStyle, 20f);

        NodeId first = tree.newLeaf(firstStyle);
        NodeId second = tree.newLeaf(secondStyle);
        NodeId middle = tree.newWithChildren(middleStyle, new NodeId[]{first, second});
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{middle});
        tree.computeLayout(root, definite(200f));

        // The first child's 10px top margin collapses through the middle block's top edge, so the
        // block itself starts 10px down (its own collapsed top margin) while the child sits at its
        // content-box origin.
        Layout middleLayout = tree.getLayout(middle);
        assertSize(middleLayout, 200f, 40f, "collapse container size");
        assertLocation(middleLayout, 0f, 10f, "collapse container position");
        Layout firstLayout = tree.getLayout(first);
        assertMargin(firstLayout, 10f, 10f, 10f, 10f, "collapsed first child margins");
        assertLocation(firstLayout, 10f, 0f, "collapsed first child position");
        assertSize(firstLayout, 180f, 10f, "collapsed first child size");

        // Between the siblings the larger (20) margin wins: 10 (first height) + 20.
        Layout secondLayout = tree.getLayout(second);
        assertMargin(secondLayout, 20f, 20f, 20f, 20f, "collapsed sibling margins");
        assertLocation(secondLayout, 20f, 30f, "collapsed sibling position");
        assertSize(secondLayout, 160f, 10f, "collapsed sibling size");

        // The last child's 20px bottom margin collapses through the middle block's bottom edge and
        // out of the root: 10 + 40 + 20.
        assertEquals(70f, tree.getLayout(root).size().height, EPSILON, "collapsed root height");
    }
}
