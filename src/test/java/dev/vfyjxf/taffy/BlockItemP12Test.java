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

/**
 * P12 pinning tests for a block item's resolved margins and insets.
 *
 * <p><b>Scope.</b> {@code BlockComputer} resolves a child's {@code margin} and {@code inset}
 * twice per layout pass: once for the geometry it lays the box out with and once for the
 * {@link Layout} it reports back to the tree. P12 collapses that work into a single resolution
 * per item, so these tests exercise every value the two resolutions can disagree on: percentage
 * margins against the container's content box (BK-21), auto margins in both the symmetrical and
 * the one-sided form, negative margins, auto block-axis margins, a {@code height: stretch} item
 * that subtracts its non-auto block margins, inherited {@code direction}, relative insets in
 * both physical forms, and the non-auto margins of a floated box.
 *
 * <p>Every expected value here is derived from the CSS rules the surrounding batch implements
 * (and which the generated WPT suites already gate); the tests are written to fail if a margin
 * is resolved twice against different bases or if an auto margin leaks into a reported value.
 * They must pass unchanged both before and after the P12 refactor.</p>
 */
public class BlockItemP12Test {

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

    // === Percentage margins ===

    @Test
    @DisplayName("percentage margins resolve against the container's content box (BK-21)")
    void percentageMarginsResolveAgainstContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(200f, Float.NaN);
        padding(rootStyle, 20f);

        TaffyStyle childStyle = sized(Float.NaN, 10f);
        childStyle.margin = TaffyRect.all(LengthPercentageAuto.percent(0.10f));

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{child});
        tree.computeLayout(root, definite(200f));

        Layout childLayout = tree.getLayout(child);
        // 10% of the content-box width (200 - 2 * 20 = 160) is 16, on every side.
        assertMargin(childLayout, 16f, 16f, 16f, 16f, "percentage margin");
        // The auto width is stretched inside the margins: 160 - 16 - 16 = 128.
        assertSize(childLayout, 128f, 10f, "percentage margin box");
        assertLocation(childLayout, 36f, 36f, "percentage margin position");
        // The container's auto height is the content height: top padding + margins + child +
        // bottom margin + bottom padding = 20 + 16 + 10 + 16 + 20.
        assertSize(tree.getLayout(root), 200f, 82f, "container height with positive margins");
    }

    // === Auto margins ===

    @Test
    @DisplayName("symmetric auto margins absorb the free space and report their used size")
    void symmetricAutoMarginsAbsorbFreeSpace() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(300f, Float.NaN);
        TaffyStyle childStyle = sized(100f, 10f);
        childStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{child});
        tree.computeLayout(root, definite(300f));

        Layout childLayout = tree.getLayout(child);
        assertMargin(childLayout, 100f, 100f, 0f, 0f, "symmetric auto margins");
        assertLocation(childLayout, 100f, 0f, "symmetric auto margins position");
        assertSize(tree.getLayout(root), 300f, 10f, "container height without block margins");
    }

    @Test
    @DisplayName("a single auto margin absorbs all of the free space")
    void oneSidedAutoMarginAbsorbsFreeSpace() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(300f, Float.NaN);
        TaffyStyle childStyle = sized(100f, 10f);
        childStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10f),
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{child});
        tree.computeLayout(root, definite(300f));

        Layout childLayout = tree.getLayout(child);
        assertMargin(childLayout, 190f, 10f, 0f, 0f, "one-sided auto margin");
        assertLocation(childLayout, 190f, 0f, "one-sided auto margin position");
    }

    @Test
    @DisplayName("auto block-axis margins resolve to zero in normal flow")
    void autoBlockAxisMarginsResolveToZero() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(200f, 100f);
        TaffyStyle childStyle = sized(100f, 10f);
        childStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{child});
        tree.computeLayout(root, definite(200f));

        Layout childLayout = tree.getLayout(child);
        assertMargin(childLayout, 0f, 0f, 0f, 0f, "auto block-axis margins");
        assertLocation(childLayout, 0f, 0f, "auto block-axis margin position");
    }

    // === Negative margins ===

    @Test
    @DisplayName("negative margins are reported unchanged and move the box outwards")
    void negativeMarginsAreReportedUnchanged() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(200f, Float.NaN);
        TaffyStyle childStyle = sized(100f, 10f);
        childStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.length(-20f), LengthPercentageAuto.length(-10f),
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{child});
        tree.computeLayout(root, definite(200f));

        Layout childLayout = tree.getLayout(child);
        assertMargin(childLayout, -20f, -10f, 0f, 0f, "negative margins");
        assertLocation(childLayout, -20f, 0f, "negative margin position");
    }

    // === height: stretch ===

    @Test
    @DisplayName("height: stretch subtracts the item's non-auto block margins")
    void stretchHeightSubtractsNonAutoMargins() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(200f, 100f);
        padding(rootStyle, 1f);

        TaffyStyle childStyle = sized(100f, Float.NaN);
        childStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.stretch());
        childStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(10f), LengthPercentageAuto.length(20f));

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{child});
        tree.computeLayout(root, definite(200f));

        Layout childLayout = tree.getLayout(child);
        // The container's content-box height (100 - 2) less the two block margins.
        assertSize(childLayout, 100f, 68f, "stretch height");
        assertLocation(childLayout, 1f, 11f, "stretch height position");
        assertMargin(childLayout, 0f, 0f, 10f, 20f, "stretch height margins");
    }

    // === direction inheritance ===

    @Test
    @DisplayName("an item inherits the container's resolved RTL direction through a nested block")
    void nestedItemsInheritResolvedDirection() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(300f, Float.NaN);
        rootStyle.direction = TaffyDirection.RTL;

        // Both the intermediate container and the leaf leave direction at its INHERIT default.
        TaffyStyle middleStyle = block();
        TaffyStyle leafStyle = sized(100f, 10f);

        NodeId leaf = tree.newLeaf(leafStyle);
        NodeId middle = tree.newWithChildren(middleStyle, new NodeId[]{leaf});
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{middle});
        tree.computeLayout(root, definite(300f));

        assertSize(tree.getLayout(middle), 300f, 10f, "inherited RTL container");
        assertLocation(tree.getLayout(middle), 0f, 0f, "inherited RTL container position");
        // RTL: default alignment is the right edge, so the 100px leaf starts at 300 - 100.
        assertLocation(tree.getLayout(leaf), 200f, 0f, "inherited RTL leaf position");
    }

    // === Relative insets ===

    @Test
    @DisplayName("relative insets offset the box in both physical forms")
    void relativeInsetsOffsetTheBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(200f, Float.NaN);

        TaffyStyle leftTopStyle = sized(100f, 10f);
        leftTopStyle.position = TaffyPosition.RELATIVE;
        leftTopStyle.inset = new TaffyRect<>(
            LengthPercentageAuto.length(10f), LengthPercentageAuto.AUTO,
            LengthPercentageAuto.length(4f), LengthPercentageAuto.AUTO);

        TaffyStyle rightBottomStyle = sized(100f, 10f);
        rightBottomStyle.position = TaffyPosition.RELATIVE;
        rightBottomStyle.inset = new TaffyRect<>(
            LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10f),
            LengthPercentageAuto.AUTO, LengthPercentageAuto.length(3f));

        NodeId leftTop = tree.newLeaf(leftTopStyle);
        NodeId rightBottom = tree.newLeaf(rightBottomStyle);
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{leftTop, rightBottom});
        tree.computeLayout(root, definite(200f));

        // A non-auto start inset wins over the end inset and offsets towards the end edge.
        assertLocation(tree.getLayout(leftTop), 10f, 4f, "left/top relative inset");
        // With only the end inset set the box moves the other way; the block offset stacks on
        // top of the first child's height.
        assertLocation(tree.getLayout(rightBottom), -10f, 7f, "right/bottom relative inset");
    }

    // === Floats ===

    @Test
    @DisplayName("a floated box is placed with its non-auto margins and the next block avoids it")
    void floatedBoxUsesNonAutoMargins() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = sized(300f, 100f);

        TaffyStyle floatStyle = sized(50f, 20f);
        floatStyle.float_ = TaffyFloat.LEFT;
        margin(floatStyle, 10f);

        TaffyStyle flowStyle = sized(100f, 10f);

        NodeId floatNode = tree.newLeaf(floatStyle);
        NodeId flowNode = tree.newLeaf(flowStyle);
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{floatNode, flowNode});
        tree.computeLayout(root, definite(300f));

        Layout floatLayout = tree.getLayout(floatNode);
        assertMargin(floatLayout, 10f, 10f, 10f, 10f, "floated box margins");
        assertLocation(floatLayout, 10f, 10f, "floated box position");

        // The float's 50px border box plus its 20px of horizontal margins leaves a slot that
        // starts at x = 70; the following in-flow block is placed at its top edge.
        Layout flowLayout = tree.getLayout(flowNode);
        assertLocation(flowLayout, 70f, 0f, "in-flow block beside the float");
    }
}
