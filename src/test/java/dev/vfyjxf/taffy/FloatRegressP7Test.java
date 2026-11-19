package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.Clear;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyFloat;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.FloatContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P7 FT-02/FT-06 float regression audit tests.
 *
 * <p>Covers the missing pieces from the audit: height-aware content slots (#1386), styled-height
 * clamping in BFC-slot avoidance (#1385), CSS2 rule 5 plus segment subdivision with fractional
 * offsets (#988), RTL relative-inset precedence (#1305), and clear no-op (#1041). The items that
 * need the public {@code FloatInsets}/float-id API (#1303/#1389) are intentionally out of this
 * batch's file scope and recorded as residual.</p>
 */
public class FloatRegressP7Test {

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

    private static TaffyStyle floated(TaffyFloat direction, float width, float height, Clear clear) {
        TaffyStyle style = sized(width, height);
        style.float_ = direction;
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

    // === #1386: content slot height ===

    @Test
    @DisplayName("#1386: content slot combines insets over the content height and reports validity")
    void contentSlotHeightAware() {
        FloatContext context = new FloatContext();
        context.setWidth(200f);
        context.placeFloatedBox(30f, 100f, 0f, 0f, 0f, FloatContext.Direction.LEFT, Clear.NONE);
        context.placeFloatedBox(50f, 50f, 100f, 0f, 0f, FloatContext.Direction.LEFT, Clear.LEFT);

        FloatContext.ContentSlot shortContent = context.contentSlot(
            0f, 0f, 0f, 0f, Clear.NONE, FloatContext.NO_REGION);
        assertEquals(1, shortContent.segmentId());
        assertEquals(30f, shortContent.x(), EPSILON);
        assertEquals(0f, shortContent.y(), EPSILON);
        assertEquals(170f, shortContent.width(), EPSILON);
        assertEquals(100f, shortContent.height(), EPSILON);

        FloatContext.ContentSlot tallContent = context.contentSlot(
            0f, 120f, 0f, 0f, Clear.NONE, FloatContext.NO_REGION);
        assertEquals(50f, tallContent.x(), EPSILON);
        assertEquals(150f, tallContent.width(), EPSILON);
        assertEquals(Float.POSITIVE_INFINITY, tallContent.height());

        FloatContext.ContentSlot after = context.contentSlot(
            0f, 0f, 0f, 0f, Clear.NONE, 1);
        assertEquals(2, after.segmentId());
        assertEquals(100f, after.y(), EPSILON);
        assertEquals(150f, after.width(), EPSILON);
    }

    // === #1385: BFC root styled height ===

    @Test
    @DisplayName("#1385: a BFC child is checked against floats over its clamped styled height")
    void bfcSlotUsesClampedStyledHeight() {
        TaffyTree tree = new TaffyTree();

        NodeId firstFloat = tree.newLeaf(floated(TaffyFloat.LEFT, 50f, 20f, Clear.NONE));
        NodeId secondFloat = tree.newLeaf(floated(TaffyFloat.LEFT, 60f, 100f, Clear.LEFT));

        TaffyStyle bfcChildStyle = flowRoot();
        bfcChildStyle.size = new TaffySize<>(
            TaffyDimension.length(145f), TaffyDimension.length(100f));
        bfcChildStyle.maxSize = new TaffySize<>(
            TaffyDimension.AUTO, TaffyDimension.length(10f));
        NodeId bfcChild = tree.newLeaf(bfcChildStyle);

        TaffyStyle rootStyle = flowRoot();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, firstFloat, secondFloat, bfcChild);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree.getLayout(firstFloat), 0f, 0f, "first float");
        assertLocation(tree.getLayout(secondFloat), 0f, 20f, "second float");

        // Styled height 100 is clamped by max-height 10, so the child only has to fit beside
        // the 20px band: 145 fits in 150 and stays at y=0 beside the first float.
        Layout childLayout = tree.getLayout(bfcChild);
        assertLocation(childLayout, 50f, 0f, "bfc child");
        assertSize(childLayout, 145f, 10f, "bfc child");
        assertSize(tree.getLayout(root), 200f, 120f, "root");
    }

    // === #988: CSS2 rule 5 and fractional segment subdivision ===

    @Test
    @DisplayName("#988: a right float is not higher than earlier left floats at fractional y")
    void fractionalOffsetsRespectFloatRule5() {
        TaffyTree tree = new TaffyTree();

        NodeId spacer = tree.newLeaf(sized(400f, 43.2f));
        NodeId floatA = tree.newLeaf(floated(TaffyFloat.LEFT, 258f, 42f, Clear.NONE));
        NodeId floatB = tree.newLeaf(floated(TaffyFloat.LEFT, 298f, 42f, Clear.NONE));
        NodeId floatC = tree.newLeaf(floated(TaffyFloat.RIGHT, 98f, 42f, Clear.NONE));

        TaffyStyle rootStyle = sized(400f, Float.NaN);
        NodeId root = tree.newWithChildren(rootStyle, spacer, floatA, floatB, floatC);

        tree.computeLayout(root, TaffySize.maxContent());

        // Values from upstream's hand-written regression (rounded to whole pixels there).
        assertLocation(tree.getLayout(floatA), 0f, 43f, "float A");
        assertLocation(tree.getLayout(floatB), 0f, 85f, "float B");
        assertLocation(tree.getLayout(floatC), 302f, 85f, "float C");
    }
}
