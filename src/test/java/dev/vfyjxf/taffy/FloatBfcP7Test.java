package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.Clear;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyFloat;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.FloatContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P7/FT-01 part 4 + FT-03 tests for cross-layer BFC context sharing, BfcSlot margin semantics,
 * RTL float avoidance, relative insets and float intrinsic width.
 *
 * <p>Scope: ordinary nested blocks share the ancestor block's {@link FloatContext}; flow-root and
 * scroll containers establish an independent one. The tests also pin the auto-width float
 * contribution (side sums / definite clamp / widest float). No Flex/Grid integration is touched.</p>
 */
public class FloatBfcP7Test {

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

    @Test
    @DisplayName("a float inside a nested ordinary block is avoided by later outer siblings")
    void nestedBlockFloatIsVisibleToOuterSibling() {
        TaffyTree tree = new TaffyTree();

        NodeId floatNode = tree.newLeaf(floated(TaffyFloat.LEFT, 50f, 20f));
        TaffyStyle nestedStyle = sized(Float.NaN, Float.NaN);
        NodeId nested = tree.newWithChildren(nestedStyle, floatNode);
        NodeId beside = tree.newLeaf(sized(Float.NaN, 10f));
        NodeId cleared = tree.newLeaf(cleared(Clear.LEFT, 10f));

        TaffyStyle rootStyle = flowRoot();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, nested, beside, cleared);

        tree.computeLayout(root, TaffySize.maxContent());

        // The nested block is not a BFC root: it has no in-flow height of its own.
        assertSize(tree.getLayout(nested), 200f, 0f, "nested");
        assertLocation(tree.getLayout(floatNode), 0f, 0f, "float");

        Layout besideLayout = tree.getLayout(beside);
        assertLocation(besideLayout, 50f, 0f, "beside");
        assertSize(besideLayout, 150f, 10f, "beside");

        Layout clearedLayout = tree.getLayout(cleared);
        assertLocation(clearedLayout, 0f, 20f, "cleared");
        assertSize(clearedLayout, 200f, 10f, "cleared");

        // Root flow-root contains the float (20) and the cleared sibling's bottom (30).
        assertSize(tree.getLayout(root), 200f, 30f, "root");
    }

    @Test
    @DisplayName("a nested flow-root isolates its floats from the outer BFC")
    void nestedFlowRootIsolatesFloats() {
        TaffyTree tree = new TaffyTree();

        NodeId innerFloat = tree.newLeaf(floated(TaffyFloat.LEFT, 30f, 15f));
        TaffyStyle innerStyle = flowRoot();
        innerStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.AUTO);
        NodeId inner = tree.newWithChildren(innerStyle, innerFloat);

        NodeId sibling = tree.newLeaf(sized(Float.NaN, 10f));
        TaffyStyle outerStyle = sized(200f, Float.NaN);
        NodeId outer = tree.newWithChildren(outerStyle, inner, sibling);

        tree.computeLayout(outer, TaffySize.maxContent());

        assertSize(tree.getLayout(inner), 100f, 15f, "inner");
        Layout siblingLayout = tree.getLayout(sibling);
        assertLocation(siblingLayout, 0f, 15f, "sibling");
        assertSize(siblingLayout, 200f, 10f, "sibling");
        assertSize(tree.getLayout(outer), 200f, 25f, "outer");
    }

    @Test
    @DisplayName("RTL mirrors the float-avoiding block's x position")
    void rtlFloatAvoidanceMirrorsX() {
        TaffyTree tree = new TaffyTree();

        NodeId floatNode = tree.newLeaf(floated(TaffyFloat.RIGHT, 50f, 20f));
        NodeId child = tree.newLeaf(sized(Float.NaN, 10f));

        TaffyStyle rootStyle = sized(200f, Float.NaN);
        rootStyle.direction = TaffyDirection.RTL;
        NodeId root = tree.newWithChildren(rootStyle, floatNode, child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree.getLayout(floatNode), 150f, 0f, "float");
        Layout childLayout = tree.getLayout(child);
        assertLocation(childLayout, 0f, 0f, "child");
        assertSize(childLayout, 150f, 10f, "child");
    }

    @Test
    @DisplayName("a floated box's relative inset moves its Layout but not float avoidance")
    void relativeInsetOnFloat() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle floatStyle = floated(TaffyFloat.LEFT, 50f, 20f);
        floatStyle.position = TaffyPosition.RELATIVE;
        floatStyle.inset = TaffyRect.ltrb(
            LengthPercentageAuto.length(10f),
            LengthPercentageAuto.length(5f),
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.AUTO
        );
        NodeId floatNode = tree.newLeaf(floatStyle);
        NodeId child = tree.newLeaf(sized(Float.NaN, 10f));

        TaffyStyle rootStyle = sized(200f, Float.NaN);
        NodeId root = tree.newWithChildren(rootStyle, floatNode, child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree.getLayout(floatNode), 10f, 5f, "float");
        Layout childLayout = tree.getLayout(child);
        assertLocation(childLayout, 50f, 0f, "child");
        assertSize(childLayout, 150f, 10f, "child");
    }

    @Test
    @DisplayName("BfcSlot applies lead/trail margins beside a float")
    void bfcSlotAppliesMarginsBesideFloat() {
        TaffyTree tree = new TaffyTree();

        NodeId floatNode = tree.newLeaf(floated(TaffyFloat.LEFT, 50f, 20f));
        TaffyStyle childStyle = sized(Float.NaN, 10f);
        childStyle.margin = TaffyRect.ltrb(
            LengthPercentageAuto.length(10f),
            LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(20f),
            LengthPercentageAuto.ZERO
        );
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle rootStyle = flowRoot();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, floatNode, child);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout childLayout = tree.getLayout(child);
        // The leading margin is absorbed by the float edge; the trailing margin reduces the
        // auto width from the slot's border width (150) to 130.
        assertLocation(childLayout, 50f, 0f, "child");
        assertSize(childLayout, 130f, 10f, "child");
        assertEquals(10f, childLayout.margin().left, EPSILON, "margin.left");
        assertEquals(20f, childLayout.margin().right, EPSILON, "margin.right");
    }

    @Test
    @DisplayName("an auto-width block's max-content width includes float side sums")
    void maxContentWidthIncludesFloatSideSums() {
        TaffyTree tree = new TaffyTree();

        NodeId left = tree.newLeaf(floated(TaffyFloat.LEFT, 80f, 10f));
        NodeId right = tree.newLeaf(floated(TaffyFloat.RIGHT, 70f, 10f));
        TaffyStyle rootStyle = sized(Float.NaN, Float.NaN);
        NodeId root = tree.newWithChildren(rootStyle, left, right);

        tree.computeLayout(root, TaffySize.maxContent());

        // 80 + 70 = 150 for the width. The tree root is the initial formatting context, so its
        // auto height now contains the 10px floats.
        assertSize(tree.getLayout(root), 150f, 10f, "root");
    }

    @Test
    @DisplayName("the intrinsic calculator clears sides and clamps a definite width")
    void intrinsicCalculatorSemantics() {
        FloatContext.IntrinsicWidthCalculator maxContent =
            new FloatContext.IntrinsicWidthCalculator(AvailableSpace.maxContent());
        maxContent.addFloat(80f, FloatContext.Direction.LEFT, Clear.NONE);
        maxContent.addFloat(70f, FloatContext.Direction.RIGHT, Clear.NONE);
        assertEquals(80f, maxContent.sideSum(FloatContext.Direction.LEFT), EPSILON);
        assertEquals(70f, maxContent.sideSum(FloatContext.Direction.RIGHT), EPSILON);
        assertEquals(80f, maxContent.widest(), EPSILON);
        assertEquals(150f, maxContent.result(), EPSILON);

        FloatContext.IntrinsicWidthCalculator cleared =
            new FloatContext.IntrinsicWidthCalculator(AvailableSpace.maxContent());
        cleared.addFloat(80f, FloatContext.Direction.LEFT, Clear.NONE);
        cleared.addFloat(70f, FloatContext.Direction.LEFT, Clear.LEFT);
        assertEquals(70f, cleared.sideSum(FloatContext.Direction.LEFT), EPSILON);
        assertEquals(80f, cleared.result(), EPSILON);

        FloatContext.IntrinsicWidthCalculator definite =
            new FloatContext.IntrinsicWidthCalculator(AvailableSpace.definite(100f));
        definite.addFloat(80f, FloatContext.Direction.LEFT, Clear.NONE);
        definite.addFloat(70f, FloatContext.Direction.LEFT, Clear.NONE);
        assertEquals(100f, definite.result(), EPSILON);

        FloatContext.IntrinsicWidthCalculator minContent =
            new FloatContext.IntrinsicWidthCalculator(AvailableSpace.minContent());
        minContent.addFloat(80f, FloatContext.Direction.LEFT, Clear.NONE);
        minContent.addFloat(70f, FloatContext.Direction.RIGHT, Clear.NONE);
        assertEquals(80f, minContent.result(), EPSILON);
    }
}
