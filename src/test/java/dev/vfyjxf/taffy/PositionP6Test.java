package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.LayoutOutput;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P6 regression tests for the position model and out-of-flow sizing.
 *
 * <p>Upstream taffy #1140 ({@code 46c4e9c}): {@code Position} gains {@code Static}, {@code Fixed}
 * and {@code Sticky}. Only {@code Relative} applies its inset as a relative offset; {@code Static}
 * and {@code Sticky} are laid out in normal flow with no offset (a sticky offset depends on the
 * scroll position, which a layout library does not know), and {@code Fixed} is out of flow exactly
 * like {@code Absolute} until out-of-flow hoisting (OF-01) exists.</p>
 *
 * <p>Compatibility (decision D1): the constants are appended after the legacy
 * {@code RELATIVE}/{@code ABSOLUTE} so old ordinals stay stable, and the Java 1.2 default remains
 * {@code RELATIVE} rather than flipping to the CSS default {@code STATIC}.</p>
 */
public class PositionP6Test {

    private static final float EPSILON = 0.001f;

    // === Helpers ===

    private static TaffyStyle container(TaffyDisplay display, float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = display;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle sized(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
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

    /** Runs a single-child layout in a container of the given display and returns the child layout. */
    private static Layout layoutSingleChild(TaffyDisplay display, TaffyStyle childStyle, float width, float height) {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(container(display, width, height), child);
        tree.computeLayout(root, TaffySize.maxContent());
        return layout(tree, child);
    }

    // === OF-02 / #1140: enum surface and defaults ===

    /**
     * The CSS keywords are appended after the legacy constants, so old ordinals (and any
     * ordinal-based serialization) keep their meaning.
     */
    @Test
    @DisplayName("OF-02: STATIC/FIXED/STICKY are appended after the legacy ordinals")
    void positionEnumAppendsKeywordsAfterLegacyConstants() {
        assertEquals(0, TaffyPosition.RELATIVE.ordinal(), "RELATIVE ordinal");
        assertEquals(1, TaffyPosition.ABSOLUTE.ordinal(), "ABSOLUTE ordinal");
        assertEquals(2, TaffyPosition.STATIC.ordinal(), "STATIC ordinal");
        assertEquals(3, TaffyPosition.FIXED.ordinal(), "FIXED ordinal");
        assertEquals(4, TaffyPosition.STICKY.ordinal(), "STICKY ordinal");
        assertEquals(5, TaffyPosition.values().length, "constant count");
    }

    /**
     * Decision D1: the 1.2 line keeps {@code RELATIVE} as the default even though CSS (and upstream
     * 2.0) default to {@code STATIC}, so existing trees keep their historical inset behavior.
     */
    @Test
    @DisplayName("OF-02: the 1.2 default position remains RELATIVE (D1)")
    void defaultPositionRemainsRelative() {
        assertEquals(TaffyPosition.RELATIVE, new TaffyStyle().position);
    }

    @Test
    @DisplayName("OF-02: isOutOfFlow is true for ABSOLUTE/FIXED only")
    void isOutOfFlowCoversAbsoluteAndFixed() {
        assertTrue(TaffyPosition.ABSOLUTE.isOutOfFlow(), "ABSOLUTE");
        assertTrue(TaffyPosition.FIXED.isOutOfFlow(), "FIXED");
        assertFalse(TaffyPosition.RELATIVE.isOutOfFlow(), "RELATIVE");
        assertFalse(TaffyPosition.STATIC.isOutOfFlow(), "STATIC");
        assertFalse(TaffyPosition.STICKY.isOutOfFlow(), "STICKY");
    }

    // === OF-02 / #1140: only RELATIVE resolves a relative inset ===

    @Test
    @DisplayName("OF-02: RELATIVE applies its inset in block, flex and grid layout")
    void relativeInsetAppliesInEveryLayoutMode() {
        for (TaffyDisplay display : new TaffyDisplay[]{TaffyDisplay.BLOCK, TaffyDisplay.FLEX, TaffyDisplay.GRID}) {
            TaffyStyle childStyle = sized(10f, 10f);
            childStyle.position = TaffyPosition.RELATIVE;
            childStyle.inset = new TaffyRect<>(px(5f), LengthPercentageAuto.AUTO, px(7f), LengthPercentageAuto.AUTO);

            Layout child = layoutSingleChild(display, childStyle, 100f, 100f);

            assertEquals(5f, child.location().x, EPSILON, display + " relative inset x");
            assertEquals(7f, child.location().y, EPSILON, display + " relative inset y");
        }
    }

    @Test
    @DisplayName("OF-02: STATIC and STICKY are in flow and do not apply their inset")
    void staticAndStickyIgnoreInset() {
        for (TaffyDisplay display : new TaffyDisplay[]{TaffyDisplay.BLOCK, TaffyDisplay.FLEX, TaffyDisplay.GRID}) {
            for (TaffyPosition position : new TaffyPosition[]{TaffyPosition.STATIC, TaffyPosition.STICKY}) {
                TaffyStyle childStyle = sized(10f, 10f);
                childStyle.position = position;
                childStyle.inset = new TaffyRect<>(px(5f), LengthPercentageAuto.AUTO, px(7f), LengthPercentageAuto.AUTO);

                Layout child = layoutSingleChild(display, childStyle, 100f, 100f);

                assertEquals(0f, child.location().x, EPSILON, position + " in " + display + " x");
                assertEquals(0f, child.location().y, EPSILON, position + " in " + display + " y");
            }
        }
    }

    /**
     * A fixed box is out of flow: it is positioned by its insets against the containing block
     * (upstream 1.2 treats {@code Fixed} exactly like {@code Absolute}), not by the static position
     * plus a relative offset.
     */
    @Test
    @DisplayName("OF-02: FIXED is out of flow and positioned by its insets, not by a relative offset")
    void fixedIsOutOfFlowAndUsesAbsposInsets() {
        TaffyTree tree = new TaffyTree();
        NodeId inFlow = tree.newLeaf(sized(20f, 20f));

        TaffyStyle fixedStyle = sized(10f, 10f);
        fixedStyle.position = TaffyPosition.FIXED;
        fixedStyle.inset = new TaffyRect<>(px(30f), LengthPercentageAuto.AUTO, px(40f), LengthPercentageAuto.AUTO);
        NodeId fixed = tree.newLeaf(fixedStyle);

        NodeId root = tree.newWithChildren(container(TaffyDisplay.BLOCK, 100f, 100f), inFlow, fixed);
        tree.computeLayout(root, TaffySize.maxContent());

        // The in-flow sibling occupies the first line; the fixed box does not consume flow space.
        assertLocation(tree, inFlow, 0f, 0f, "in-flow sibling");
        assertLocation(tree, fixed, 30f, 40f, "fixed child");
        assertSize(tree, fixed, 10f, 10f, "fixed child");
    }

    // === OF-03 / #1202: abspos auto margins only absorb space with both insets set ===

    /** Lays out one absolutely positioned child in a 200x200 container of the given display. */
    private static Layout absposChild(TaffyDisplay display, TaffyStyle childStyle) {
        childStyle.position = TaffyPosition.ABSOLUTE;
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(container(display, 200f, 200f), child);
        tree.computeLayout(root, TaffySize.maxContent());
        return layout(tree, child);
    }

    private static TaffyRect<LengthPercentageAuto> margin(
        LengthPercentageAuto left, LengthPercentageAuto right,
        LengthPercentageAuto top, LengthPercentageAuto bottom) {
        return new TaffyRect<>(left, right, top, bottom);
    }

    private static TaffyRect<LengthPercentageAuto> inset(
        LengthPercentageAuto left, LengthPercentageAuto right,
        LengthPercentageAuto top, LengthPercentageAuto bottom) {
        return new TaffyRect<>(left, right, top, bottom);
    }

    /**
     * Upstream #1202: with both inline insets set, two auto margins split the free space left by
     * the used size, inset positions and non-auto margins between them (the pre-existing behavior
     * for the fully specified case must not change).
     */
    @Test
    @DisplayName("OF-03: block abspos auto margins split the free space when both insets are set")
    void blockAbsposAutoMarginsAbsorbWithBothInsets() {
        TaffyStyle childStyle = sized(50f, 50f);
        childStyle.margin = margin(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO,
                                   LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        childStyle.inset = inset(px(10f), px(20f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);

        Layout child = absposChild(TaffyDisplay.BLOCK, childStyle);

        // 200 - 10 - 20 - 50 = 120 free space; 60 per auto margin; x = 10 + 60.
        assertEquals(70f, child.location().x, EPSILON, "block both-inset auto margin x");
        assertEquals(0f, child.location().y, EPSILON, "block both-inset auto margin y");
    }

    /**
     * Upstream #1202: with only the end inset set, the inline axis is over-constrained and the
     * auto margin resolves to zero instead of absorbing the space up to the inset.
     */
    @Test
    @DisplayName("OF-03: block abspos single inset does not inflate the auto margin")
    void blockAbsposSingleInsetDoesNotInflateAutoMargin() {
        TaffyStyle rightOnly = sized(50f, 50f);
        rightOnly.margin = margin(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO,
                                  LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        rightOnly.inset = inset(LengthPercentageAuto.AUTO, px(20f), px(10f), LengthPercentageAuto.AUTO);
        Layout rightLayout = absposChild(TaffyDisplay.BLOCK, rightOnly);
        assertEquals(130f, rightLayout.location().x, EPSILON, "block right-inset auto margin x");
        assertEquals(10f, rightLayout.location().y, EPSILON, "block right-inset auto margin y");

        TaffyStyle bottomOnly = sized(50f, 50f);
        bottomOnly.margin = margin(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
                                   LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO);
        bottomOnly.inset = inset(px(10f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, px(20f));
        Layout bottomLayout = absposChild(TaffyDisplay.BLOCK, bottomOnly);
        assertEquals(10f, bottomLayout.location().x, EPSILON, "block bottom-inset auto margin x");
        assertEquals(130f, bottomLayout.location().y, EPSILON, "block bottom-inset auto margin y");

        // A single *start* inset already resolved to zero free space before #1202; keep it pinned.
        TaffyStyle leftOnly = sized(50f, 50f);
        leftOnly.margin = margin(LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO,
                                 LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        leftOnly.inset = inset(px(10f), LengthPercentageAuto.AUTO, px(10f), LengthPercentageAuto.AUTO);
        Layout leftLayout = absposChild(TaffyDisplay.BLOCK, leftOnly);
        assertEquals(10f, leftLayout.location().x, EPSILON, "block left-inset auto margin x");
        assertEquals(10f, leftLayout.location().y, EPSILON, "block left-inset auto margin y");
    }

    /** The same #1202 rule as implemented by the flexbox absolute-positioning pass. */
    @Test
    @DisplayName("OF-03: flex abspos auto margins split the free space when both insets are set")
    void flexAbsposAutoMarginsAbsorbWithBothInsets() {
        TaffyStyle childStyle = sized(50f, 50f);
        childStyle.margin = margin(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO,
                                   LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        childStyle.inset = inset(px(10f), px(20f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);

        Layout child = absposChild(TaffyDisplay.FLEX, childStyle);

        assertEquals(70f, child.location().x, EPSILON, "flex both-inset auto margin x");
        assertEquals(0f, child.location().y, EPSILON, "flex both-inset auto margin y");
    }

    @Test
    @DisplayName("OF-03: flex abspos single inset does not inflate the auto margin")
    void flexAbsposSingleInsetDoesNotInflateAutoMargin() {
        TaffyStyle rightOnly = sized(50f, 50f);
        rightOnly.margin = margin(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO,
                                  LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        rightOnly.inset = inset(LengthPercentageAuto.AUTO, px(20f), px(10f), LengthPercentageAuto.AUTO);
        Layout rightLayout = absposChild(TaffyDisplay.FLEX, rightOnly);
        assertEquals(130f, rightLayout.location().x, EPSILON, "flex right-inset auto margin x");
        assertEquals(10f, rightLayout.location().y, EPSILON, "flex right-inset auto margin y");

        TaffyStyle leftOnly = sized(50f, 50f);
        leftOnly.margin = margin(LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO,
                                 LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        leftOnly.inset = inset(px(10f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        Layout leftLayout = absposChild(TaffyDisplay.FLEX, leftOnly);
        assertEquals(10f, leftLayout.location().x, EPSILON, "flex left-inset auto margin x");
    }

    /**
     * Regression: absolute positioning of a non-replaced box between two insets still stretches
     * the auto size to fill the positioning area (the OF-04 replaced guard must not change it).
     */
    @Test
    @DisplayName("OF-03: legacy absolute inset stretching still fills the area")
    void absoluteInsetStretchingRegression() {
        TaffyStyle childStyle = new TaffyStyle();
        childStyle.inset = inset(px(10f), px(20f), px(10f), px(20f));

        Layout child = absposChild(TaffyDisplay.BLOCK, childStyle);

        assertEquals(170f, child.size().width, EPSILON, "absolute stretched width");
        assertEquals(170f, child.size().height, EPSILON, "absolute stretched height");
        assertEquals(10f, child.location().x, EPSILON, "absolute stretched x");
        assertEquals(10f, child.location().y, EPSILON, "absolute stretched y");
    }

    // === OF-04 / #1203: abspos replaced elements are not stretched between their insets ===

    /** A replaced leaf whose intrinsic size is 100x50 (an image, upstream's TestNodeContext). */
    private static NodeId replacedLeaf(TaffyTree tree, TaffyStyle style) {
        style.itemIsReplaced = true;
        return tree.newLeafWithMeasureFunction(style, context -> LayoutOutput.fromSizesAndBaselines(
            new FloatSize(100f, 50f),
            FloatSize.zero(),
            new FloatPoint(Float.NaN, Float.NaN)
        ));
    }

    private static Layout replacedAbspos(TaffyStyle childStyle) {
        return replacedAbspos(TaffyDisplay.BLOCK, childStyle, 400f, 300f);
    }

    private static Layout replacedAbspos(TaffyDisplay display, TaffyStyle childStyle, float width, float height) {
        TaffyTree tree = new TaffyTree();
        childStyle.position = TaffyPosition.ABSOLUTE;
        NodeId child = replacedLeaf(tree, childStyle);
        NodeId root = tree.newWithChildren(container(display, width, height), child);
        tree.computeLayout(root, TaffySize.maxContent());
        return layout(tree, child);
    }

    /**
     * An auto-sized absolutely positioned replaced element keeps its intrinsic size instead of
     * filling the space between its insets (CSS2.1 §10.3.8/§10.6.5); its location still follows
     * the start insets.
     */
    @Test
    @DisplayName("OF-04: abspos replaced element keeps its intrinsic 100x50 size between insets")
    void replacedAbsposIsNotStretched() {
        for (TaffyDisplay display : new TaffyDisplay[]{TaffyDisplay.BLOCK, TaffyDisplay.FLEX, TaffyDisplay.GRID}) {
            TaffyStyle childStyle = new TaffyStyle();
            childStyle.inset = inset(px(10f), px(10f), px(10f), px(10f));

            Layout child = replacedAbspos(display, childStyle, 400f, 300f);

            assertEquals(100f, child.size().width, EPSILON, display + " replaced width");
            assertEquals(50f, child.size().height, EPSILON, display + " replaced height");
            assertEquals(10f, child.location().x, EPSILON, display + " replaced x");
            assertEquals(10f, child.location().y, EPSILON, display + " replaced y");
        }
    }

    /**
     * A replaced element with a definite width and an auto height resolves the height from its
     * aspect ratio rather than from {top, bottom} (CSS2.1 §10.6.5).
     */
    @Test
    @DisplayName("OF-04: abspos replaced auto height comes from the aspect ratio, not the insets")
    void replacedAbsposHeightUsesAspectRatio() {
        TaffyStyle childStyle = new TaffyStyle();
        childStyle.aspectRatio = 2f; // width / height, i.e. a 200x100 image
        childStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.AUTO);
        childStyle.inset = inset(px(10f), px(10f), px(10f), px(10f));

        Layout child = replacedAbspos(childStyle);

        assertEquals(200f, child.size().width, EPSILON, "replaced aspect-ratio width");
        assertEquals(100f, child.size().height, EPSILON, "replaced aspect-ratio height");
    }

    /**
     * The same insets on a non-replaced box still stretch it between the insets: the #1203 guard
     * must not change the legacy sizing of ordinary out-of-flow boxes.
     */
    @Test
    @DisplayName("OF-04: non-replaced abspos box is still stretched between its insets")
    void nonReplacedAbsposIsStillStretched() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle childStyle = new TaffyStyle();
        childStyle.position = TaffyPosition.ABSOLUTE;
        childStyle.inset = inset(px(10f), px(10f), px(10f), px(10f));
        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(container(TaffyDisplay.BLOCK, 400f, 300f), child);
        tree.computeLayout(root, TaffySize.maxContent());

        Layout childLayout = layout(tree, child);
        assertEquals(380f, childLayout.size().width, EPSILON, "non-replaced stretched width");
        assertEquals(280f, childLayout.size().height, EPSILON, "non-replaced stretched height");
        assertEquals(10f, childLayout.location().x, EPSILON, "non-replaced x");
        assertEquals(10f, childLayout.location().y, EPSILON, "non-replaced y");
    }

    /**
     * A compressible replaced form control is <em>not</em> a replaced element for this rule
     * (upstream #1256 split the two predicates): its auto size keeps the historical stretch.
     */
    @Test
    @DisplayName("OF-04: compressible-replaced box keeps the legacy stretch behavior")
    void compressibleReplacedIsNotTreatedAsReplaced() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle childStyle = new TaffyStyle();
        childStyle.position = TaffyPosition.ABSOLUTE;
        childStyle.itemIsCompressibleReplaced = true;
        childStyle.inset = inset(px(10f), px(10f), px(10f), px(10f));
        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(container(TaffyDisplay.BLOCK, 400f, 300f), child);
        tree.computeLayout(root, TaffySize.maxContent());

        Layout childLayout = layout(tree, child);
        assertEquals(380f, childLayout.size().width, EPSILON, "compressible-replaced stretched width");
        assertEquals(280f, childLayout.size().height, EPSILON, "compressible-replaced stretched height");
    }
}
