package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyFloat;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P8 FT-01 deep tests: the per-block available width that float placement and float avoidance
 * use inside a nested block.
 *
 * <p>The shared {@code FloatContext} stores one BFC width, and a nested block's content edges
 * reach it through the {@code BlockContext} insets. A float's border position is therefore
 * already block-local (the right float of a 100-wide block inside a 200-wide BFC lands at 50).
 * The remaining per-block width question is the <em>slot</em> a float-avoiding box gets: it must
 * be bounded by both the floats beside it and the containing block's own content box, never by
 * the raw BFC width. Upstream taffy expresses that by combining the containing-block insets with
 * the box margins inside {@code find_bfc_slot} ({@code src/compute/float.rs}); these tests pin
 * the resulting geometry at the block level.</p>
 *
 * <p>Every scenario is a block formatting context rooted at a 200-wide flow-root, so the
 * expected numbers follow CSS 2.2 §9.5 float avoidance with a definite containing block.</p>
 */
public class FloatWidthP8Test {

    private static final float EPSILON = 0.001f;

    private static TaffyStyle block() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        return style;
    }

    private static TaffyStyle flowRoot() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLOW_ROOT;
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

    private static TaffyStyle floated(TaffyFloat direction, float width, float height) {
        TaffyStyle style = sized(width, height);
        style.float_ = direction;
        return style;
    }

    private static TaffyStyle offsetBlock(float leftMargin, float width) {
        TaffyStyle style = sized(width, Float.NaN);
        style.margin = TaffyRect.ltrb(
            LengthPercentageAuto.length(leftMargin),
            LengthPercentageAuto.ZERO,
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.AUTO
        );
        return style;
    }

    private static void horizontalPadding(TaffyStyle style, float padding) {
        style.padding = TaffyRect.hv(LengthPercentage.length(padding), LengthPercentage.ZERO);
    }

    private static void assertSize(Layout layout, float width, float height, String message) {
        assertEquals(width, layout.size().width, EPSILON, message + " width");
        assertEquals(height, layout.size().height, EPSILON, message + " height");
    }

    private static void assertLocation(Layout layout, float x, float y, String message) {
        assertEquals(x, layout.location().x, EPSILON, message + " x");
        assertEquals(y, layout.location().y, EPSILON, message + " y");
    }

    // === Required scenario: root 200 / inner 100, right float ===

    @Test
    @DisplayName("a right float inside a 100-wide nested block uses the nested content edge")
    void rightFloatInNarrowNestedBlockUsesNestedContentEdge() {
        TaffyTree tree = new TaffyTree();

        NodeId rightFloat = tree.newLeaf(floated(TaffyFloat.RIGHT, 50f, 20f));
        NodeId beside = tree.newLeaf(sized(Float.NaN, 10f));
        NodeId inner = tree.newWithChildren(sized(100f, Float.NaN), rightFloat, beside);

        TaffyStyle rootStyle = flowRoot();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, inner);

        tree.computeLayout(root, TaffySize.maxContent());

        // The nested block's right content edge is 100 from the BFC left and 100 from its right,
        // so the 50-wide right float occupies [50, 100) of the nested block.
        assertLocation(tree.getLayout(inner), 0f, 0f, "inner");
        assertSize(tree.getLayout(inner), 100f, 10f, "inner");
        assertLocation(tree.getLayout(rightFloat), 50f, 0f, "right float");
        // The block beside it spans the remaining nested content width, not the BFC width.
        Layout besideLayout = tree.getLayout(beside);
        assertLocation(besideLayout, 0f, 0f, "beside");
        assertSize(besideLayout, 50f, 10f, "beside");
        // The BFC root still contains the 20-high float.
        assertSize(tree.getLayout(root), 200f, 20f, "root");
    }

    // === The nested block is offset, so insets must bound the slot ===

    @Test
    @DisplayName("an outer leading float left of a nested block does not narrow its content")
    void outerLeadingFloatDoesNotNarrowNestedContent() {
        TaffyTree tree = new TaffyTree();

        NodeId floatNode = tree.newLeaf(floated(TaffyFloat.LEFT, 20f, 20f));
        NodeId child = tree.newLeaf(sized(Float.NaN, 10f));
        NodeId inner = tree.newWithChildren(offsetBlock(50f, 100f), child);

        TaffyStyle rootStyle = flowRoot();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, floatNode, inner);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree.getLayout(floatNode), 0f, 0f, "float");
        assertLocation(tree.getLayout(inner), 50f, 0f, "inner");
        assertSize(tree.getLayout(inner), 100f, 10f, "inner");

        // The 20-wide float reaches x=20; the nested block's content box only starts at x=50, so
        // the child keeps the full 100-wide nested content box at the block-local x=0.
        Layout childLayout = tree.getLayout(child);
        assertLocation(childLayout, 0f, 0f, "child");
        assertSize(childLayout, 100f, 10f, "child");
        assertSize(tree.getLayout(root), 200f, 20f, "root");
    }

    @Test
    @DisplayName("an outer trailing float right of the nested block neither pushes nor narrows it")
    void outerTrailingFloatDoesNotNarrowNestedContent() {
        TaffyTree tree = new TaffyTree();

        NodeId floatNode = tree.newLeaf(floated(TaffyFloat.RIGHT, 20f, 20f));
        NodeId child = tree.newLeaf(sized(Float.NaN, 10f));
        NodeId inner = tree.newWithChildren(offsetBlock(50f, 100f), child);

        TaffyStyle rootStyle = flowRoot();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, floatNode, inner);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree.getLayout(floatNode), 180f, 0f, "float");
        assertLocation(tree.getLayout(inner), 50f, 0f, "inner");
        assertSize(tree.getLayout(inner), 100f, 10f, "inner");

        // The float occupies [180, 200); the nested content box ends at x=150, so the child is
        // not pushed below it and keeps the nested width at the block-local x=0.
        Layout childLayout = tree.getLayout(child);
        assertLocation(childLayout, 0f, 0f, "child");
        assertSize(childLayout, 100f, 10f, "child");
        assertSize(tree.getLayout(root), 200f, 20f, "root");
    }

    // === Nested BFC roots ===

    @Test
    @DisplayName("a nested flow-root sizes its floats from its own width, not the outer BFC width")
    void nestedFlowRootUsesItsOwnWidthForFloats() {
        TaffyTree tree = new TaffyTree();

        NodeId rightFloat = tree.newLeaf(floated(TaffyFloat.RIGHT, 50f, 20f));
        NodeId beside = tree.newLeaf(sized(Float.NaN, 10f));
        TaffyStyle innerStyle = flowRoot();
        innerStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.AUTO);
        NodeId inner = tree.newWithChildren(innerStyle, rightFloat, beside);

        TaffyStyle rootStyle = flowRoot();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, inner);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree.getLayout(inner), 0f, 0f, "inner");
        assertSize(tree.getLayout(inner), 100f, 20f, "inner");
        // The nested BFC is 100 wide, so the right float sits at 100 - 50.
        assertLocation(tree.getLayout(rightFloat), 50f, 0f, "right float");
        Layout besideLayout = tree.getLayout(beside);
        assertLocation(besideLayout, 0f, 0f, "beside");
        assertSize(besideLayout, 50f, 10f, "beside");
        assertSize(tree.getLayout(root), 200f, 20f, "root");
    }

    @Test
    @DisplayName("a flow-root child of an offset nested block avoids the outer float with its local width")
    void flowRootChildOfOffsetBlockUsesNestedContentWidth() {
        TaffyTree tree = new TaffyTree();

        NodeId floatNode = tree.newLeaf(floated(TaffyFloat.LEFT, 20f, 20f));
        NodeId bfcChild = tree.newLeaf(flowRootSized10());
        NodeId inner = tree.newWithChildren(offsetBlock(50f, 100f), bfcChild);

        TaffyStyle rootStyle = flowRoot();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, floatNode, inner);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree.getLayout(floatNode), 0f, 0f, "float");
        assertLocation(tree.getLayout(inner), 50f, 0f, "inner");
        assertSize(tree.getLayout(inner), 100f, 10f, "inner");

        // A block that establishes its own formatting context only avoids floats; its border box
        // stays inside the nested containing block instead of taking the BFC width.
        Layout bfcLayout = tree.getLayout(bfcChild);
        assertLocation(bfcLayout, 0f, 0f, "flow-root child");
        assertSize(bfcLayout, 100f, 10f, "flow-root child");
        assertSize(tree.getLayout(root), 200f, 20f, "root");
    }

    private static TaffyStyle flowRootSized10() {
        TaffyStyle style = flowRoot();
        style.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(10f));
        return style;
    }

    // === Regressions ===

    @Test
    @DisplayName("nested blocks without floats keep the stretch width")
    void nestedBlocksWithoutFloatsKeepTheStretchWidth() {
        TaffyTree tree = new TaffyTree();

        NodeId child = tree.newLeaf(sized(Float.NaN, 10f));
        NodeId inner = tree.newWithChildren(offsetBlock(50f, 100f), child);

        TaffyStyle rootStyle = flowRoot();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, inner);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree.getLayout(inner), 50f, 0f, "inner");
        assertSize(tree.getLayout(inner), 100f, 10f, "inner");
        Layout childLayout = tree.getLayout(child);
        assertLocation(childLayout, 0f, 0f, "child");
        assertSize(childLayout, 100f, 10f, "child");
        assertSize(tree.getLayout(root), 200f, 10f, "root");
    }

    @Test
    @DisplayName("root-level float avoidance is unchanged by the nested-block width fix")
    void rootLevelFloatAvoidanceIsUnchanged() {
        TaffyTree tree = new TaffyTree();

        NodeId floatNode = tree.newLeaf(floated(TaffyFloat.LEFT, 50f, 20f));
        NodeId child = tree.newLeaf(sized(Float.NaN, 10f));

        TaffyStyle rootStyle = flowRoot();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, floatNode, child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree.getLayout(floatNode), 0f, 0f, "float");
        Layout childLayout = tree.getLayout(child);
        assertLocation(childLayout, 50f, 0f, "child");
        assertSize(childLayout, 150f, 10f, "child");
        assertSize(tree.getLayout(root), 200f, 20f, "root");
    }

    @Test
    @DisplayName("a padded BFC root keeps the trailing padding in its float-avoiding slots")
    void paddedRootSlotKeepsTheTrailingPadding() {
        TaffyTree tree = new TaffyTree();

        NodeId floatNode = tree.newLeaf(floated(TaffyFloat.LEFT, 20f, 20f));
        NodeId child = tree.newLeaf(sized(Float.NaN, 10f));

        TaffyStyle rootStyle = flowRoot();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
        horizontalPadding(rootStyle, 10f);
        NodeId root = tree.newWithChildren(rootStyle, floatNode, child);

        tree.computeLayout(root, TaffySize.maxContent());

        // Content box [10, 190); the float occupies [10, 30), so the child fills [30, 190) = 160.
        assertLocation(tree.getLayout(floatNode), 10f, 0f, "float");
        Layout childLayout = tree.getLayout(child);
        assertLocation(childLayout, 30f, 0f, "child");
        assertSize(childLayout, 160f, 10f, "child");
        assertSize(tree.getLayout(root), 200f, 20f, "root");
    }
}
