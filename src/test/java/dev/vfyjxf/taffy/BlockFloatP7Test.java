package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.Clear;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
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
 * P7/FT-01 tests for the BlockComputer float integration.
 *
 * <p><b>Scope.</b> These tests drive the public tree/layout API with floated block children and
 * assert the minimal integration delivered in this batch: left/right float placement, in-flow
 * block avoidance through content slots, {@code clear} on floats and in-flow boxes, BFC-root
 * (flow-root / scroll-container) height contribution with bottom padding, and the unchanged
 * no-float path. The float context is local to each block container in this batch; sharing a
 * context across an ancestral BFC is a documented TODO and is not asserted here.</p>
 */
public class BlockFloatP7Test {

    private static final float EPSILON = 0.001f;

    // === Helpers ===

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

    private static TaffyStyle cleared(Clear clear, float height) {
        TaffyStyle style = sized(Float.NaN, height);
        style.clear = clear;
        return style;
    }

    private static void assertSize(Layout layout, float width, float height, String message) {
        assertEquals(width, layout.size().width, EPSILON, message + " width");
        assertEquals(height, layout.size().height, EPSILON, message + " height");
    }

    private static void assertLocation(Layout layout, float x, float y, String message) {
        assertEquals(x, layout.location().x, EPSILON, message + " x");
        assertEquals(y, layout.location().y, EPSILON, message + " y");
    }

    // === Float placement and in-flow avoidance ===

    @Test
    @DisplayName("a left float is placed at x=0 and a later block starts beside it")
    void leftFloatWrapsFollowingBlock() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(200f, Float.NaN);
        NodeId floatNode = tree.newLeaf(floated(TaffyFloat.LEFT, 50f, 20f));
        NodeId child = tree.newLeaf(sized(Float.NaN, 20f));
        NodeId root = tree.newWithChildren(rootStyle, floatNode, child);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout floatLayout = tree.getLayout(floatNode);
        assertLocation(floatLayout, 0f, 0f, "float");
        assertSize(floatLayout, 50f, 20f, "float");

        Layout childLayout = tree.getLayout(child);
        assertLocation(childLayout, 50f, 0f, "child");
        assertSize(childLayout, 150f, 20f, "child");

        assertSize(tree.getLayout(root), 200f, 20f, "root");
    }

    @Test
    @DisplayName("a right float is placed at the BFC right edge and a later block starts at x=0")
    void rightFloatWrapsFollowingBlock() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(200f, Float.NaN);
        NodeId floatNode = tree.newLeaf(floated(TaffyFloat.RIGHT, 50f, 20f));
        NodeId child = tree.newLeaf(sized(Float.NaN, 20f));
        NodeId root = tree.newWithChildren(rootStyle, floatNode, child);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout floatLayout = tree.getLayout(floatNode);
        assertLocation(floatLayout, 150f, 0f, "float");
        assertSize(floatLayout, 50f, 20f, "float");

        Layout childLayout = tree.getLayout(child);
        assertLocation(childLayout, 0f, 0f, "child");
        assertSize(childLayout, 150f, 20f, "child");
    }

    @Test
    @DisplayName("an auto-width float is shrink-to-fit sized")
    void autoWidthFloatIsShrinkToFit() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle floatStyle = block();
        floatStyle.float_ = TaffyFloat.LEFT;
        floatStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        NodeId inner = tree.newLeaf(sized(30f, 10f));
        NodeId floatNode = tree.newWithChildren(floatStyle, inner);
        NodeId child = tree.newLeaf(sized(Float.NaN, 10f));

        TaffyStyle rootStyle = sized(200f, Float.NaN);
        NodeId root = tree.newWithChildren(rootStyle, floatNode, child);
        tree.computeLayout(root, TaffySize.maxContent());

        Layout floatLayout = tree.getLayout(floatNode);
        assertSize(floatLayout, 30f, 10f, "float");
        assertLocation(floatLayout, 0f, 0f, "float");

        Layout childLayout = tree.getLayout(child);
        assertLocation(childLayout, 30f, 0f, "child");
        assertSize(childLayout, 170f, 10f, "child");
    }

    @Test
    @DisplayName("float margins are part of the float box's space")
    void floatMarginsOffsetPlacementAndAvoidance() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle floatStyle = floated(TaffyFloat.LEFT, 50f, 20f);
        floatStyle.margin = TaffyRect.ltrb(
            LengthPercentageAuto.length(10f),
            LengthPercentageAuto.length(5f),
            LengthPercentageAuto.length(0f),
            LengthPercentageAuto.length(0f)
        );
        NodeId floatNode = tree.newLeaf(floatStyle);
        NodeId child = tree.newLeaf(sized(Float.NaN, 20f));

        TaffyStyle rootStyle = sized(200f, Float.NaN);
        NodeId root = tree.newWithChildren(rootStyle, floatNode, child);
        tree.computeLayout(root, TaffySize.maxContent());

        Layout floatLayout = tree.getLayout(floatNode);
        assertLocation(floatLayout, 10f, 5f, "float border box");
        assertSize(floatLayout, 50f, 20f, "float");

        // The float's margin box is 60 wide, so the content slot starts at x=60.
        Layout childLayout = tree.getLayout(child);
        assertLocation(childLayout, 60f, 0f, "child");
        assertSize(childLayout, 140f, 20f, "child");
    }

    // === Clearance ===

    @Test
    @DisplayName("clear:left pushes a block below a left float")
    void clearLeftPushesBlockBelowFloat() {
        TaffyTree tree = new TaffyTree();

        NodeId floatNode = tree.newLeaf(floated(TaffyFloat.LEFT, 50f, 20f));
        NodeId child = tree.newLeaf(cleared(Clear.LEFT, 10f));
        TaffyStyle rootStyle = sized(200f, Float.NaN);
        NodeId root = tree.newWithChildren(rootStyle, floatNode, child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree.getLayout(floatNode), 0f, 0f, "float");
        Layout childLayout = tree.getLayout(child);
        assertLocation(childLayout, 0f, 20f, "child");
        assertSize(childLayout, 200f, 10f, "child");
        assertSize(tree.getLayout(root), 200f, 30f, "root");
    }

    @Test
    @DisplayName("clear:both pushes a block below the lower of a left and a right float")
    void clearBothUsesLowestFloat() {
        TaffyTree tree = new TaffyTree();

        NodeId leftFloat = tree.newLeaf(floated(TaffyFloat.LEFT, 50f, 20f));
        NodeId rightFloat = tree.newLeaf(floated(TaffyFloat.RIGHT, 60f, 30f));
        NodeId child = tree.newLeaf(cleared(Clear.BOTH, 10f));
        TaffyStyle rootStyle = sized(200f, Float.NaN);
        NodeId root = tree.newWithChildren(rootStyle, leftFloat, rightFloat, child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree.getLayout(leftFloat), 0f, 0f, "left float");
        assertLocation(tree.getLayout(rightFloat), 140f, 0f, "right float");
        Layout childLayout = tree.getLayout(child);
        assertLocation(childLayout, 0f, 30f, "child");
        assertSize(childLayout, 200f, 10f, "child");
    }

    @Test
    @DisplayName("a clear float is placed below the cleared side's float")
    void clearedFloatIsPlacedBelowFloat() {
        TaffyTree tree = new TaffyTree();

        NodeId first = tree.newLeaf(floated(TaffyFloat.LEFT, 50f, 20f));
        TaffyStyle secondStyle = floated(TaffyFloat.LEFT, 40f, 10f);
        secondStyle.clear = Clear.LEFT;
        NodeId second = tree.newLeaf(secondStyle);
        TaffyStyle rootStyle = sized(200f, Float.NaN);
        NodeId root = tree.newWithChildren(rootStyle, first, second);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree.getLayout(first), 0f, 0f, "first");
        assertLocation(tree.getLayout(second), 0f, 20f, "second");
    }

    @Test
    @DisplayName("clear:right pushes a block below a right float")
    void clearRightPushesBlockBelowFloat() {
        TaffyTree tree = new TaffyTree();

        NodeId floatNode = tree.newLeaf(floated(TaffyFloat.RIGHT, 50f, 20f));
        NodeId child = tree.newLeaf(cleared(Clear.RIGHT, 10f));
        TaffyStyle rootStyle = sized(200f, Float.NaN);
        NodeId root = tree.newWithChildren(rootStyle, floatNode, child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree.getLayout(floatNode), 150f, 0f, "float");
        Layout childLayout = tree.getLayout(child);
        assertLocation(childLayout, 0f, 20f, "child");
        assertSize(childLayout, 200f, 10f, "child");
    }

    // === BFC root containment ===

    @Test
    @DisplayName("a flow-root auto height contains its floats and bottom padding")
    void flowRootHeightContainsFloat() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flowRoot();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
        rootStyle.padding = TaffyRect.ltrb(
            LengthPercentage.ZERO,
            LengthPercentage.ZERO,
            LengthPercentage.ZERO,
            LengthPercentage.length(10f)
        );
        NodeId floatNode = tree.newLeaf(floated(TaffyFloat.LEFT, 50f, 30f));
        NodeId root = tree.newWithChildren(rootStyle, floatNode);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree.getLayout(floatNode), 0f, 0f, "float");
        assertSize(tree.getLayout(root), 200f, 40f, "root");
    }

    @Test
    @DisplayName("overflow:hidden establishes a BFC that contains its floats")
    void overflowHiddenContainsFloat() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(200f, Float.NaN);
        rootStyle.overflow = new TaffyPoint<>(Overflow.HIDDEN, Overflow.HIDDEN);
        rootStyle.padding = TaffyRect.ltrb(
            LengthPercentage.ZERO,
            LengthPercentage.ZERO,
            LengthPercentage.ZERO,
            LengthPercentage.length(5f)
        );
        NodeId floatNode = tree.newLeaf(floated(TaffyFloat.LEFT, 50f, 30f));
        NodeId root = tree.newWithChildren(rootStyle, floatNode);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 200f, 35f, "root");
    }

    @Test
    @DisplayName("a plain block does not contain its floats (only a BFC root does)")
    void plainBlockDoesNotContainFloat() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(200f, Float.NaN);
        rootStyle.padding = TaffyRect.ltrb(
            LengthPercentage.ZERO,
            LengthPercentage.ZERO,
            LengthPercentage.ZERO,
            LengthPercentage.length(10f)
        );
        NodeId floatNode = tree.newLeaf(floated(TaffyFloat.LEFT, 50f, 30f));
        NodeId root = tree.newWithChildren(rootStyle, floatNode);

        tree.computeLayout(root, TaffySize.maxContent());

        // Only the bottom padding contributes to the non-BFC block's auto height.
        assertSize(tree.getLayout(root), 200f, 10f, "root");
    }

    @Test
    @DisplayName("flow-root height is the larger of its in-flow content and its floats")
    void flowRootHeightUsesMaxOfContentAndFloats() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flowRoot();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
        NodeId floatNode = tree.newLeaf(floated(TaffyFloat.LEFT, 50f, 20f));
        NodeId child = tree.newLeaf(sized(Float.NaN, 10f));
        NodeId root = tree.newWithChildren(rootStyle, floatNode, child);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout childLayout = tree.getLayout(child);
        assertLocation(childLayout, 50f, 0f, "child");
        assertSize(childLayout, 150f, 10f, "child");
        // The float is 20 tall and the in-flow content is 10 tall.
        assertSize(tree.getLayout(root), 200f, 20f, "root");
    }

    @Test
    @DisplayName("a nested flow-root contains its own floats independently")
    void nestedFlowRootContainsOwnFloat() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle innerStyle = flowRoot();
        innerStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.AUTO);
        NodeId innerFloat = tree.newLeaf(floated(TaffyFloat.LEFT, 40f, 25f));
        NodeId inner = tree.newWithChildren(innerStyle, innerFloat);

        TaffyStyle outerStyle = sized(200f, Float.NaN);
        NodeId outer = tree.newWithChildren(outerStyle, inner);

        tree.computeLayout(outer, TaffySize.maxContent());

        assertSize(tree.getLayout(inner), 100f, 25f, "inner");
        assertLocation(tree.getLayout(innerFloat), 0f, 0f, "inner float");
        assertSize(tree.getLayout(outer), 200f, 25f, "outer");
    }

    // === No-float regression ===

    @Test
    @DisplayName("a block with no floats keeps its original in-flow layout")
    void noFloatPathUnchanged() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(100f, Float.NaN);
        NodeId first = tree.newLeaf(sized(Float.NaN, 10f));
        NodeId second = tree.newLeaf(sized(Float.NaN, 20f));
        NodeId root = tree.newWithChildren(rootStyle, first, second);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree.getLayout(first), 0f, 0f, "first");
        assertSize(tree.getLayout(first), 100f, 10f, "first");
        assertLocation(tree.getLayout(second), 0f, 10f, "second");
        assertSize(tree.getLayout(second), 100f, 20f, "second");
        assertSize(tree.getLayout(root), 100f, 30f, "root");
    }
}
