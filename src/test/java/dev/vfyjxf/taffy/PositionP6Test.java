package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
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
}
