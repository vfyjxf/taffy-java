package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.JustifyContent;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * P6 regression tests for the static position of absolutely positioned flex children.
 *
 * <p>FL-22 (upstream #958, {@code becf0e59}): a {@code safe} cross-axis {@code align-self} on an
 * absolutely positioned child falls back to logical {@code start} when the item's used size plus
 * resolved margins overflows the static-position alignment container (the containing block's
 * content box). Without the overflow the modifier is simply dropped.</p>
 *
 * <p>FL-23 (upstream #1072, {@code 5f2d500}): {@code justify-content: start}/{@code end} and
 * {@code align-self: start}/{@code end}/{@code baseline} are resolved writing-mode relative (they
 * flip for RTL but not for {@code row-reverse}/{@code column-reverse} or {@code wrap-reverse}),
 * the default {@code justify-content} and the {@code space-between} fallback behave as
 * {@code flex-start} (flex-relative), and {@code flex-start}/{@code flex-end} stay flex-relative.</p>
 *
 * <p><b>Scope.</b> This port has no out-of-flow hoisting (OF-01), so an absolutely positioned box
 * is laid out by its direct parent. Every test below therefore makes the direct parent the
 * containing block; the upstream "outer containing block" fixtures cannot be reproduced until
 * OF-01 lands. Only the positioning area (content box) and the writing mode of that parent are
 * exercised.</p>
 */
public class FlexAbsposP6Test {

    private static final float EPSILON = 0.001f;

    // === Helpers ===

    private static TaffyStyle flexRow(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.ROW;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle flexColumn(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.COLUMN;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle absoluteSized(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.position = TaffyPosition.ABSOLUTE;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static NodeId absoluteChild(TaffyTree tree, float width, float height, AlignItems alignSelf) {
        TaffyStyle style = absoluteSized(width, height);
        if (alignSelf != null) {
            style.alignSelf = alignSelf;
        }
        return tree.newLeaf(style);
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

    // === FL-22 / #958: safe cross-axis self-alignment ===

    @Test
    @DisplayName("FL-22: safe end on an overflowing abspos item falls back to the start edge")
    void safeEndFallsBackToStartOnOverflow() {
        TaffyTree tree = new TaffyTree();
        NodeId child = absoluteChild(tree, 50f, 200f, AlignItems.SAFE_END);
        NodeId root = tree.newWithChildren(flexRow(100f, 100f), child);

        tree.computeLayout(root, TaffySize.maxContent());

        // 200px item in a 100px content box: safe end is start.
        assertLocation(tree, child, 0f, 0f, "safe end with cross overflow");
    }

    @Test
    @DisplayName("FL-22: safe end without overflow keeps the end edge")
    void safeEndKeepsEndWithoutOverflow() {
        TaffyTree tree = new TaffyTree();
        NodeId child = absoluteChild(tree, 50f, 50f, AlignItems.SAFE_END);
        NodeId root = tree.newWithChildren(flexRow(100f, 100f), child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, child, 0f, 50f, "safe end without cross overflow");
    }

    @Test
    @DisplayName("FL-22: resolved margins count towards the cross-axis overflow")
    void safeEndCountsMarginsWhenDetectingOverflow() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = absoluteSized(50f, 50f);
        style.alignSelf = AlignItems.SAFE_END;
        style.margin = TaffyRect.ltrb(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.length(30f),
            LengthPercentageAuto.ZERO, LengthPercentageAuto.length(30f));
        NodeId child = tree.newLeaf(style);
        NodeId root = tree.newWithChildren(flexRow(100f, 100f), child);

        tree.computeLayout(root, TaffySize.maxContent());

        // 50 + 30 + 30 > 100: safe end falls back to start, then the start margin is applied.
        assertLocation(tree, child, 0f, 30f, "safe end with margins overflowing the content box");
    }

    @Test
    @DisplayName("FL-22: an unsafe end on an overflowing abspos item stays at the end edge")
    void unsafeEndStaysEndOnOverflow() {
        TaffyTree tree = new TaffyTree();
        NodeId child = absoluteChild(tree, 50f, 200f, AlignItems.END);
        NodeId root = tree.newWithChildren(flexRow(100f, 100f), child);

        tree.computeLayout(root, TaffySize.maxContent());

        // Without the safe modifier the item is allowed to overflow the start edge.
        assertLocation(tree, child, 0f, -100f, "unsafe end with cross overflow");
    }

    @Test
    @DisplayName("FL-22: safe center falls back to start on overflow and centres otherwise")
    void safeCenterFallsBackOnlyOnOverflow() {
        TaffyTree overflowing = new TaffyTree();
        NodeId overflowChild = absoluteChild(overflowing, 50f, 200f, AlignItems.SAFE_CENTER);
        NodeId overflowRoot = overflowing.newWithChildren(flexRow(100f, 100f), overflowChild);
        overflowing.computeLayout(overflowRoot, TaffySize.maxContent());
        assertLocation(overflowing, overflowChild, 0f, 0f, "safe center with cross overflow");

        TaffyTree fitting = new TaffyTree();
        NodeId fittingChild = absoluteChild(fitting, 50f, 50f, AlignItems.SAFE_CENTER);
        NodeId fittingRoot = fitting.newWithChildren(flexRow(100f, 100f), fittingChild);
        fitting.computeLayout(fittingRoot, TaffySize.maxContent());
        assertLocation(fitting, fittingChild, 0f, 25f, "safe center without cross overflow");
    }

    @Test
    @DisplayName("FL-22: the cross-axis overflow is measured against the content box")
    void safeEndUsesTheContentBoxAsAlignmentContainer() {
        TaffyTree tree = new TaffyTree();
        NodeId overflowChild = absoluteChild(tree, 50f, 100f, AlignItems.SAFE_END);
        NodeId fittingChild = absoluteChild(tree, 50f, 50f, AlignItems.SAFE_END);

        TaffyStyle rootStyle = flexRow(100f, 100f);
        rootStyle.padding = TaffyRect.all(LengthPercentage.length(10f));
        NodeId root = tree.newWithChildren(rootStyle, overflowChild, fittingChild);

        tree.computeLayout(root, TaffySize.maxContent());

        // Content box = [10, 90] (80px) in both axes. 100px overflows the cross axis -> start at
        // y = 10; 50px fits -> end at y = 40. The main axis is the default flex-start (x = 10).
        assertLocation(tree, overflowChild, 10f, 10f, "safe end overflowing the content box");
        assertLocation(tree, fittingChild, 10f, 40f, "safe end fitting the content box");
    }

    @Test
    @DisplayName("FL-22: safe cross alignment on a column container uses the horizontal axis")
    void safeCrossAlignmentOnColumnContainer() {
        TaffyTree tree = new TaffyTree();
        NodeId overflowChild = absoluteChild(tree, 200f, 20f, AlignItems.SAFE_END);
        NodeId fittingChild = absoluteChild(tree, 50f, 20f, AlignItems.SAFE_END);
        NodeId root = tree.newWithChildren(flexColumn(100f, 100f), overflowChild, fittingChild);

        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, overflowChild, 0f, 0f, "safe end with horizontal overflow");
        assertLocation(tree, fittingChild, 50f, 0f, "safe end without horizontal overflow");
    }
}
