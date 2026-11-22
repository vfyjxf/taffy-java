package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * P6 regression tests for out-of-flow self-alignment and static positioning.
 *
 * <p>Upstream #1206 ({@code a4dff69e}) gives absolutely positioned boxes their
 * {@code align-self}/{@code justify-self} values: the box is aligned (or stretched) within its
 * inset-modified containing block, {@code justify-self: auto} falls back to the container's
 * {@code justify-items}, and the static-position rectangle is aligned with the same keywords.
 * Upstream #1267 ({@code 5f6ec1d4}) makes {@code safe} static alignment check overflow against the
 * inset-modified containing block and fall back to its start edge.</p>
 *
 * <p><b>Scope.</b> This port has no out-of-flow hoisting (OF-01), so an out-of-flow box is laid
 * out by its direct parent. The tests below therefore always make the direct parent the
 * containing block (a positioned block/grid container, or a flex container); the upstream
 * "outer containing block" fixtures cannot be reproduced until OF-01 lands.</p>
 */
public class AbsposAlignP6Test {

    private static final float EPSILON = 0.001f;

    // === Helpers ===

    private static TaffyStyle block(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle grid(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        // One explicit cell: an auto-placed absolutely positioned child then covers the whole
        // grid area, which is also its containing block.
        style.gridTemplateColumns = List.of(TrackSizingFunction.fixed(LengthPercentage.length(width)));
        style.gridTemplateRows = List.of(TrackSizingFunction.fixed(LengthPercentage.length(height)));
        return style;
    }

    private static TaffyStyle sized(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle absolute(TaffyStyle style) {
        style.position = TaffyPosition.ABSOLUTE;
        return style;
    }

    private static LengthPercentageAuto px(float value) {
        return LengthPercentageAuto.length(value);
    }

    private static Layout layout(TaffyTree tree, NodeId node) {
        Layout layout = tree.getLayout(node);
        assertNotNull(layout, "node has no layout");
        return layout;
    }

    private static void assertLocation(TaffyTree tree, NodeId node, float x, float y, String message) {
        Layout layout = layout(tree, node);
        assertEquals(x, layout.location().x, EPSILON, message + " (x)");
        assertEquals(y, layout.location().y, EPSILON, message + " (y)");
    }

    private static void assertSize(TaffyTree tree, NodeId node, float width, float height, String message) {
        Layout layout = layout(tree, node);
        assertEquals(width, layout.size().width, EPSILON, message + " (width)");
        assertEquals(height, layout.size().height, EPSILON, message + " (height)");
    }

    // === OF-05 / #1206: block abspos self-alignment ===

    @Test
    @DisplayName("OF-05: justify-self aligns an abspos block child in its static-position rectangle")
    void blockJustifySelfInStaticPositionRectangle() {
        TaffyTree tree = new TaffyTree();
        NodeId start = tree.newLeaf(absolute(sized(30f, 30f)));
        tree.getStyle(start).justifySelf = AlignItems.START;
        NodeId center = tree.newLeaf(absolute(sized(30f, 30f)));
        tree.getStyle(center).justifySelf = AlignItems.CENTER;
        NodeId end = tree.newLeaf(absolute(sized(30f, 30f)));
        tree.getStyle(end).justifySelf = AlignItems.END;

        TaffyStyle rootStyle = block(200f, 100f);
        rootStyle.position = TaffyPosition.RELATIVE;
        NodeId root = tree.newWithChildren(rootStyle, start, center, end);
        tree.computeLayout(root, TaffySize.maxContent());

        // The static-position rectangle spans the content box [0, 200]. The block axis static
        // position is a zero-height line at y = 0 (no preceding in-flow content).
        assertLocation(tree, start, 0f, 0f, "justify-self: start");
        assertLocation(tree, center, 85f, 0f, "justify-self: center");
        assertLocation(tree, end, 170f, 0f, "justify-self: end");
    }

    @Test
    @DisplayName("OF-05: justify-self: auto takes the block container's justify-items")
    void blockJustifyItemsFallback() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(absolute(sized(30f, 30f)));

        TaffyStyle rootStyle = block(200f, 100f);
        rootStyle.position = TaffyPosition.RELATIVE;
        rootStyle.justifyItems = AlignItems.END;
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, child, 170f, 0f, "justify-items: end fallback");
    }

    @Test
    @DisplayName("OF-05: block self-alignment flips logical start/end edges in RTL")
    void blockRtlSelfAlignment() {
        TaffyTree tree = new TaffyTree();
        NodeId inherited = tree.newLeaf(absolute(sized(30f, 30f)));
        NodeId explicitStart = tree.newLeaf(absolute(sized(30f, 30f)));
        tree.getStyle(explicitStart).justifySelf = AlignItems.START;

        TaffyStyle rootStyle = block(200f, 100f);
        rootStyle.position = TaffyPosition.RELATIVE;
        rootStyle.direction = TaffyDirection.RTL;
        rootStyle.justifyItems = AlignItems.END;
        NodeId root = tree.newWithChildren(rootStyle, inherited, explicitStart);
        tree.computeLayout(root, TaffySize.maxContent());

        // In RTL the inline start edge is the physical right edge.
        assertLocation(tree, inherited, 0f, 0f, "justify-items: end (RTL) is the physical left");
        assertLocation(tree, explicitStart, 170f, 0f, "justify-self: start (RTL) is the physical right");
    }

    @Test
    @DisplayName("OF-05: with both insets set the block child aligns within its IMCB")
    void blockBothInsetsAlignsWithinImcb() {
        TaffyTree tree = new TaffyTree();
        NodeId end = tree.newLeaf(absolute(sized(20f, 20f)));
        endStyle(tree, end, AlignItems.END);
        NodeId center = tree.newLeaf(absolute(sized(20f, 20f)));
        endStyle(tree, center, AlignItems.CENTER);

        TaffyStyle rootStyle = block(200f, 100f);
        rootStyle.position = TaffyPosition.RELATIVE;
        NodeId root = tree.newWithChildren(rootStyle, end, center);
        tree.computeLayout(root, TaffySize.maxContent());

        // IMCB = [left 20, 200 - right 40] = [20, 160] (width 140).
        assertLocation(tree, end, 140f, 0f, "justify-self: end inside the IMCB");
        assertLocation(tree, center, 80f, 0f, "justify-self: center inside the IMCB");
    }

    private static void endStyle(TaffyTree tree, NodeId node, AlignItems alignment) {
        TaffyStyle style = tree.getStyle(node);
        style.justifySelf = alignment;
        style.inset = TaffyRect.ltrb(px(20f), LengthPercentageAuto.AUTO, px(40f), LengthPercentageAuto.AUTO);
    }

    @Test
    @DisplayName("OF-05: align-self aligns a block child within its block-axis IMCB")
    void blockAlignSelfWithinImcb() {
        TaffyTree tree = new TaffyTree();
        NodeId end = tree.newLeaf(absolute(sized(20f, 20f)));
        TaffyStyle endStyle = tree.getStyle(end);
        endStyle.alignSelf = AlignItems.END;
        endStyle.inset = TaffyRect.ltrb(LengthPercentageAuto.AUTO, px(10f), LengthPercentageAuto.AUTO, px(10f));
        NodeId center = tree.newLeaf(absolute(sized(20f, 20f)));
        TaffyStyle centerStyle = tree.getStyle(center);
        centerStyle.alignSelf = AlignItems.CENTER;
        centerStyle.inset = TaffyRect.ltrb(LengthPercentageAuto.AUTO, px(10f), LengthPercentageAuto.AUTO, px(10f));

        TaffyStyle rootStyle = block(200f, 100f);
        rootStyle.position = TaffyPosition.RELATIVE;
        NodeId root = tree.newWithChildren(rootStyle, end, center);
        tree.computeLayout(root, TaffySize.maxContent());

        // IMCB = [top 10, 100 - bottom 10] = [10, 90] (height 80).
        assertLocation(tree, end, 0f, 70f, "align-self: end inside the IMCB");
        assertLocation(tree, center, 0f, 40f, "align-self: center inside the IMCB");
    }

    @Test
    @DisplayName("OF-05: align-self: stretch stretches an auto block size between both insets")
    void blockStretchSelf() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(absolute(new TaffyStyle()));
        tree.getStyle(child).size = new TaffySize<>(TaffyDimension.length(20f), TaffyDimension.AUTO);
        TaffyStyle childStyle = tree.getStyle(child);
        childStyle.alignSelf = AlignItems.STRETCH;
        childStyle.inset = TaffyRect.ltrb(LengthPercentageAuto.AUTO, px(10f), LengthPercentageAuto.AUTO, px(10f));

        TaffyStyle rootStyle = block(200f, 100f);
        rootStyle.position = TaffyPosition.RELATIVE;
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree, child, 20f, 80f, "stretch-fit block size");
        assertLocation(tree, child, 0f, 10f, "stretched child is inset from the containing block");
    }

    @Test
    @DisplayName("OF-05: a non-stretch self alignment keeps an auto block size fit-content")
    void blockNonStretchSelfKeepsFitContentSize() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(absolute(new TaffyStyle()));
        tree.getStyle(child).size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(20f));
        TaffyStyle childStyle = tree.getStyle(child);
        childStyle.justifySelf = AlignItems.CENTER;
        childStyle.inset = TaffyRect.ltrb(px(10f), LengthPercentageAuto.AUTO, px(10f), LengthPercentageAuto.AUTO);

        TaffyStyle rootStyle = block(200f, 100f);
        rootStyle.position = TaffyPosition.RELATIVE;
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        // `normal`/`stretch` is the only alignment whose auto size is the stretch-fit size; a
        // centered empty box is fit-content (0 wide) and centered in the 180px IMCB.
        assertSize(tree, child, 0f, 20f, "fit-content width");
        assertLocation(tree, child, 100f, 0f, "centered fit-content box");
    }

    // === OF-05 / #1206: grid abspos self-alignment ===

    @Test
    @DisplayName("OF-05: grid abspos takes the container's justify-items/align-items fallback")
    void gridItemsFallback() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(absolute(sized(20f, 20f)));

        TaffyStyle rootStyle = grid(100f, 100f);
        rootStyle.position = TaffyPosition.RELATIVE;
        rootStyle.justifyItems = AlignItems.END;
        rootStyle.alignItems = AlignItems.CENTER;
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        // The auto-placed abspos child covers the whole grid area: justify-items: end is x = 80,
        // align-items: center is y = (100 - 20) / 2 = 40.
        assertLocation(tree, child, 80f, 40f, "grid container fallback");
    }

    @Test
    @DisplayName("OF-05: grid abspos explicit self alignment wins over the container fallback")
    void gridExplicitSelfAlignment() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(absolute(sized(20f, 20f)));
        TaffyStyle childStyle = tree.getStyle(child);
        childStyle.justifySelf = AlignItems.CENTER;
        childStyle.alignSelf = AlignItems.END;

        TaffyStyle rootStyle = grid(100f, 100f);
        rootStyle.position = TaffyPosition.RELATIVE;
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, child, 40f, 80f, "explicit self alignment");
    }

    @Test
    @DisplayName("OF-05: grid abspos with both insets aligns within the IMCB")
    void gridBothInsetsAlignsWithinImcb() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(absolute(sized(20f, 20f)));
        TaffyStyle childStyle = tree.getStyle(child);
        childStyle.justifySelf = AlignItems.END;
        childStyle.alignSelf = AlignItems.CENTER;
        childStyle.inset = TaffyRect.ltrb(px(10f), px(10f), px(10f), px(10f));

        TaffyStyle rootStyle = grid(100f, 100f);
        rootStyle.position = TaffyPosition.RELATIVE;
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        // IMCB = [10, 90] in both axes.
        assertLocation(tree, child, 70f, 40f, "grid IMCB alignment");
    }

    @Test
    @DisplayName("OF-05: grid abspos self alignment flips logical edges in RTL")
    void gridRtlSelfAlignment() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(absolute(sized(20f, 20f)));
        tree.getStyle(child).justifySelf = AlignItems.START;

        TaffyStyle rootStyle = grid(100f, 100f);
        rootStyle.position = TaffyPosition.RELATIVE;
        rootStyle.direction = TaffyDirection.RTL;
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, child, 80f, 0f, "justify-self: start in RTL is the physical right");
    }

    @Test
    @DisplayName("OF-05: grid abspos stretch stays start-aligned (stretch does not apply to abspos)")
    void gridStretchStaysStartAligned() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(absolute(sized(20f, 20f)));
        tree.getStyle(child).alignSelf = AlignItems.STRETCH;

        TaffyStyle rootStyle = grid(100f, 100f);
        rootStyle.position = TaffyPosition.RELATIVE;
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, child, 0f, 0f, "stretch behaves as start");
    }

    @Test
    @DisplayName("OF-05: grid safe alignment inside an IMCB falls back to its start edge on overflow")
    void gridSafeInsideImcb() {
        TaffyTree fits = new TaffyTree();
        NodeId fitsChild = fits.newLeaf(absolute(sized(20f, 20f)));
        TaffyStyle fitsStyle = fits.getStyle(fitsChild);
        fitsStyle.justifySelf = AlignItems.SAFE_CENTER;
        fitsStyle.inset = TaffyRect.ltrb(px(10f), LengthPercentageAuto.AUTO, px(10f), LengthPercentageAuto.AUTO);
        TaffyStyle fitsRootStyle = grid(100f, 100f);
        fitsRootStyle.position = TaffyPosition.RELATIVE;
        NodeId fitsRoot = fits.newWithChildren(fitsRootStyle, fitsChild);
        fits.computeLayout(fitsRoot, TaffySize.maxContent());
        assertLocation(fits, fitsChild, 40f, 0f, "safe center fits the IMCB");

        TaffyTree overflows = new TaffyTree();
        NodeId overflowChild = overflows.newLeaf(absolute(sized(200f, 20f)));
        TaffyStyle overflowStyle = overflows.getStyle(overflowChild);
        overflowStyle.justifySelf = AlignItems.SAFE_CENTER;
        overflowStyle.inset = TaffyRect.ltrb(px(10f), LengthPercentageAuto.AUTO, px(10f), LengthPercentageAuto.AUTO);
        TaffyStyle overflowRootStyle = grid(100f, 100f);
        overflowRootStyle.position = TaffyPosition.RELATIVE;
        NodeId overflowRoot = overflows.newWithChildren(overflowRootStyle, overflowChild);
        overflows.computeLayout(overflowRoot, TaffySize.maxContent());
        assertLocation(overflows, overflowChild, 10f, 0f, "safe center falls back to the IMCB start");
    }
}