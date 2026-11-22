package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.Clear;
import dev.vfyjxf.taffy.style.LengthPercentage;
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
 * P7 FT-01 architecture-residual tests: initial formatting-context root containment, floats that
 * are themselves BFC roots, and the nested-width residual.
 *
 * <p>The root is identified by {@code getParent(node) == null} and only affects auto-height
 * containment; nested ordinary blocks keep ignoring floats. A float is marked through
 * {@code BlockContext.rootAsFloat}, so floats inside a float are contained by the outer float's
 * height. Nested blocks still share the root BFC width (upstream TODO); the residual test pins
 * that behavior.</p>
 */
public class FloatArchP7Test {

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

    private static void paddingBottom(TaffyStyle style, float bottom) {
        style.padding = TaffyRect.ltrb(
            LengthPercentage.ZERO,
            LengthPercentage.ZERO,
            LengthPercentage.ZERO,
            LengthPercentage.length(bottom)
        );
    }

    private static void assertSize(Layout layout, float width, float height, String message) {
        assertEquals(width, layout.size().width, EPSILON, message + " width");
        assertEquals(height, layout.size().height, EPSILON, message + " height");
    }

    private static void assertLocation(Layout layout, float x, float y, String message) {
        assertEquals(x, layout.location().x, EPSILON, message + " x");
        assertEquals(y, layout.location().y, EPSILON, message + " y");
    }

    // === Initial formatting-context root ===

    @Test
    @DisplayName("a display:block tree root contains its floats in its auto height")
    void blockRootContainsFloats() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(200f, Float.NaN);
        paddingBottom(rootStyle, 10f);
        NodeId floatNode = tree.newLeaf(floated(TaffyFloat.LEFT, 50f, 30f));
        NodeId root = tree.newWithChildren(rootStyle, floatNode);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree.getLayout(floatNode), 0f, 0f, "float");
        // float bottom 30 + bottom padding 10.
        assertSize(tree.getLayout(root), 200f, 40f, "root");
    }

    @Test
    @DisplayName("a non-root plain block still does not contain its floats")
    void nestedPlainBlockDoesNotContainFloats() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle innerStyle = sized(200f, Float.NaN);
        paddingBottom(innerStyle, 10f);
        NodeId floatNode = tree.newLeaf(floated(TaffyFloat.LEFT, 50f, 30f));
        NodeId inner = tree.newWithChildren(innerStyle, floatNode);

        TaffyStyle rootStyle = flowRoot();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, inner);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(inner), 200f, 10f, "inner");
        assertSize(tree.getLayout(root), 200f, 30f, "root");
    }

    @Test
    @DisplayName("a root with no floats is unchanged")
    void rootWithoutFloatsUnchanged() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(200f, Float.NaN);
        NodeId child = tree.newLeaf(sized(Float.NaN, 10f));
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree.getLayout(child), 0f, 0f, "child");
        assertSize(tree.getLayout(root), 200f, 10f, "root");
    }

    // === Float as a BFC root ===

    @Test
    @DisplayName("a float containing another float grows to contain it")
    void floatContainingFloatContainsIt() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle outerStyle = floated(TaffyFloat.LEFT, 100f, Float.NaN);
        NodeId innerFloat = tree.newLeaf(floated(TaffyFloat.LEFT, 50f, 20f));
        NodeId outerFloat = tree.newWithChildren(outerStyle, innerFloat);

        TaffyStyle rootStyle = flowRoot();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, outerFloat);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree.getLayout(innerFloat), 0f, 0f, "inner float");
        Layout outerLayout = tree.getLayout(outerFloat);
        assertLocation(outerLayout, 0f, 0f, "outer float");
        // The outer float is itself a BFC root: its auto height contains the inner float.
        assertSize(outerLayout, 100f, 20f, "outer float");
        assertSize(tree.getLayout(root), 200f, 20f, "root");
    }

    // === Nested-width residual ===

    @Test
    @DisplayName("nested block floats use the nested content edge, not the raw BFC width (residual)")
    void nestedBlockFloatUsesNestedContentEdge() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle innerStyle = sized(100f, Float.NaN);
        NodeId rightFloat = tree.newLeaf(floated(TaffyFloat.RIGHT, 50f, 20f));
        NodeId inner = tree.newWithChildren(innerStyle, rightFloat);

        TaffyStyle rootStyle = flowRoot();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, inner);

        tree.computeLayout(root, TaffySize.maxContent());

        // The shared FloatContext keeps the root width (200), but the child BlockContext's
        // right inset is 200 - 100, so the float's content edge is the nested block's right edge:
        // 200 - 100 - 50 = 50. Nested widths are already honored through insets; the remaining
        // upstream TODO is really about per-block intrinsic/available width, not this position.
        assertLocation(tree.getLayout(rightFloat), 50f, 0f, "right float");
        assertSize(tree.getLayout(root), 200f, 20f, "root");
    }
}
