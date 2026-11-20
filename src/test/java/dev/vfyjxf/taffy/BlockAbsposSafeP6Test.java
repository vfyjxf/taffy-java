package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
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
 * P6 regression tests for {@code safe} static alignment of absolutely positioned block children.
 *
 * <p>Upstream #1267 ({@code 5f6ec1d4}) changes only the "both insets auto" branch of
 * {@code OofAxis::position} in {@code src/compute/oof.rs}: the static-position rectangle and the
 * alignment keyword first derive the inset-modified containing block (IMCB), and an explicit
 * {@code safe} alignment whose margin box overflows that IMCB falls back to the containing block's
 * physical start edge. The {@code default} (auto-safety) modifier deliberately does <em>not</em>
 * fall back here; it keeps aligning within the static-position rectangle exactly as before, so
 * only an explicit {@code safe} value can change the result.</p>
 *
 * <p><b>IMCB derivation</b> (upstream {@code inset_modified_containing_block}, css-position-3
 * "resolving insets"). With both insets {@code auto}, the physical alignment edge chosen by the
 * resolved self-alignment selects the line, relative to the containing block:
 * {@code start -> [static.start, cbSize]}, {@code end -> [0, static.end]}, and
 * {@code center ->} the line centered on the static rectangle's center and bounded by the nearer
 * containing-block edge. An over-constrained result collapses towards the logical end edge. This
 * is why a {@code safe end} overflow falls back to offset 0 (the containing block start), not to
 * the static-position rectangle's start.</p>
 *
 * <p><b>Scope.</b> This port has no out-of-flow hoisting (OF-01), so an out-of-flow box is laid
 * out by its direct parent, and the tests below always use that parent's padding box as the
 * containing block. The upstream block fixture
 * ({@code block_absolute_safe_static_outer_cb_tentative}) uses a static parent inside a positioned
 * ancestor and therefore cannot be reproduced until OF-01 lands; {@link #directParentIsContainingBlock()}
 * pins the current boundary on purpose.</p>
 */
public class BlockAbsposSafeP6Test {

    private static final float EPSILON = 0.001f;

    // === Helpers ===

    private static TaffyStyle block(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle absolute(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.position = TaffyPosition.ABSOLUTE;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle inFlowHeight(float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        style.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(height));
        return style;
    }

    private static void padding(TaffyStyle style, float left, float right) {
        style.padding = TaffyRect.ltrb(
            LengthPercentage.length(left),
            LengthPercentage.ZERO,
            LengthPercentage.length(right),
            LengthPercentage.ZERO
        );
    }

    private static void margin(TaffyStyle style, float left, float right) {
        style.margin = TaffyRect.ltrb(
            LengthPercentageAuto.length(left),
            LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(right),
            LengthPercentageAuto.ZERO
        );
    }

    private static TaffyStyle positioned(TaffyStyle style) {
        style.position = TaffyPosition.RELATIVE;
        return style;
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

    // === OF-06 / #1267: safe static alignment for both-auto insets ===

    @Test
    @DisplayName("OF-06: safe end falls back to the physical start when the margin box overflows")
    void safeStaticEndFallsBackOnOverflow() {
        TaffyTree tree = new TaffyTree();
        // Both insets auto, explicit 'safe end', margin box (240) wider than the 200px IMCB.
        NodeId safeOverflow = tree.newLeaf(absolute(240f, 20f));
        tree.getStyle(safeOverflow).justifySelf = AlignItems.SAFE_END;
        // The same box with the default (unsafe) 'end' keeps the static-position alignment.
        NodeId defaultOverflow = tree.newLeaf(absolute(240f, 20f));
        tree.getStyle(defaultOverflow).justifySelf = AlignItems.END;
        // A safe value that fits still aligns to its requested edge.
        NodeId safeFits = tree.newLeaf(absolute(80f, 20f));
        tree.getStyle(safeFits).justifySelf = AlignItems.SAFE_END;

        NodeId root = tree.newWithChildren(positioned(block(200f, 100f)), safeOverflow, defaultOverflow, safeFits);
        tree.computeLayout(root, TaffySize.maxContent());

        // Static rectangle = [0, 200]; 'end' derived IMCB = [0, 200] (size 200).
        assertLocation(tree, safeOverflow, 0f, 0f, "safe end falls back to the IMCB start");
        assertLocation(tree, defaultOverflow, -40f, 0f, "default end does not fall back");
        assertLocation(tree, safeFits, 120f, 0f, "safe end that fits aligns to the static end edge");
    }

    @Test
    @DisplayName("OF-06: safe static alignment checks the derived IMCB, not the static rectangle")
    void safeStaticUsesDerivedImcb() {
        TaffyTree tree = new TaffyTree();
        // Content box = [20, 160] inside the 200px padding box (the containing block).
        NodeId safeEndOverflow = tree.newLeaf(absolute(170f, 20f));
        tree.getStyle(safeEndOverflow).justifySelf = AlignItems.SAFE_END;
        NodeId defaultEndOverflow = tree.newLeaf(absolute(170f, 20f));
        tree.getStyle(defaultEndOverflow).justifySelf = AlignItems.END;
        NodeId safeEndFits = tree.newLeaf(absolute(150f, 20f));
        tree.getStyle(safeEndFits).justifySelf = AlignItems.SAFE_END;
        NodeId safeCenterFits = tree.newLeaf(absolute(150f, 20f));
        tree.getStyle(safeCenterFits).justifySelf = AlignItems.SAFE_CENTER;
        NodeId safeCenterOverflow = tree.newLeaf(absolute(190f, 20f));
        tree.getStyle(safeCenterOverflow).justifySelf = AlignItems.SAFE_CENTER;

        TaffyStyle rootStyle = positioned(block(200f, 100f));
        padding(rootStyle, 20f, 40f);
        NodeId root = tree.newWithChildren(
            rootStyle, safeEndOverflow, defaultEndOverflow, safeEndFits, safeCenterFits, safeCenterOverflow);
        tree.computeLayout(root, TaffySize.maxContent());

        // 'end' derived IMCB = [0, static.end] = [0, 160] (size 160): the fallback is the
        // containing block's physical start (0), not the static rectangle's start (20).
        assertLocation(tree, safeEndOverflow, 0f, 0f, "safe end overflow falls back to the CB start");
        assertLocation(tree, defaultEndOverflow, -10f, 0f, "default end keeps the static-rect end");
        assertLocation(tree, safeEndFits, 10f, 0f, "safe end that fits uses the static rectangle");
        // 'center' derived IMCB = [0, 180] (center 90, nearer to the start edge).
        assertLocation(tree, safeCenterFits, 15f, 0f, "safe center that fits uses the static rectangle");
        assertLocation(tree, safeCenterOverflow, 0f, 0f, "safe center overflow falls back to the CB start");
    }

    @Test
    @DisplayName("OF-06: a center-derived IMCB is bounded by the nearer containing-block edge")
    void safeStaticCenterImcbHugsNearerEdge() {
        TaffyTree tree = new TaffyTree();
        // Content box = [140, 160]; its center (150) is nearer to the containing block's end (200),
        // so the center-derived IMCB is [100, 200] (size 100).
        NodeId safeOverflow = tree.newLeaf(absolute(120f, 20f));
        tree.getStyle(safeOverflow).justifySelf = AlignItems.SAFE_CENTER;
        NodeId defaultCenter = tree.newLeaf(absolute(120f, 20f));
        tree.getStyle(defaultCenter).justifySelf = AlignItems.CENTER;
        NodeId safeFits = tree.newLeaf(absolute(80f, 20f));
        tree.getStyle(safeFits).justifySelf = AlignItems.SAFE_CENTER;

        TaffyStyle rootStyle = positioned(block(200f, 100f));
        padding(rootStyle, 140f, 40f);
        NodeId root = tree.newWithChildren(rootStyle, safeOverflow, defaultCenter, safeFits);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, safeOverflow, 100f, 0f, "safe center falls back to the IMCB start");
        assertLocation(tree, defaultCenter, 90f, 0f, "default center keeps the static-rect center");
        assertLocation(tree, safeFits, 110f, 0f, "safe center that fits uses the static rectangle");
    }

    @Test
    @DisplayName("OF-06: the overflow check uses the margin box, including its margins")
    void safeStaticMarginBoxIncludesMargins() {
        TaffyTree tree = new TaffyTree();
        // Content 150 + 30 + 30 margins = a 210px margin box against the 200px IMCB.
        NodeId safeEnd = tree.newLeaf(absolute(150f, 20f));
        tree.getStyle(safeEnd).justifySelf = AlignItems.SAFE_END;
        margin(tree.getStyle(safeEnd), 30f, 30f);
        NodeId defaultEnd = tree.newLeaf(absolute(150f, 20f));
        tree.getStyle(defaultEnd).justifySelf = AlignItems.END;
        margin(tree.getStyle(defaultEnd), 30f, 30f);
        NodeId safeCenter = tree.newLeaf(absolute(150f, 20f));
        tree.getStyle(safeCenter).justifySelf = AlignItems.SAFE_CENTER;
        margin(tree.getStyle(safeCenter), 30f, 30f);
        NodeId defaultCenter = tree.newLeaf(absolute(150f, 20f));
        tree.getStyle(defaultCenter).justifySelf = AlignItems.CENTER;
        margin(tree.getStyle(defaultCenter), 30f, 30f);

        NodeId root = tree.newWithChildren(
            positioned(block(200f, 100f)), safeEnd, defaultEnd, safeCenter, defaultCenter);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, safeEnd, 30f, 0f, "safe end falls back to IMCB start + margin");
        assertLocation(tree, defaultEnd, 20f, 0f, "default end aligns the overflowing margin box");
        assertLocation(tree, safeCenter, 30f, 0f, "safe center overflow includes the margins");
        assertLocation(tree, defaultCenter, 25f, 0f, "default center aligns the overflowing margin box");
    }

    @Test
    @DisplayName("OF-06: in RTL a safe fallback uses the containing block's physical start edge")
    void safeStaticRtlFallbackUsesPhysicalStart() {
        TaffyTree tree = new TaffyTree();
        NodeId safeOverflow = tree.newLeaf(absolute(240f, 20f));
        tree.getStyle(safeOverflow).justifySelf = AlignItems.SAFE_END;
        NodeId defaultOverflow = tree.newLeaf(absolute(240f, 20f));
        tree.getStyle(defaultOverflow).justifySelf = AlignItems.END;
        NodeId safeFits = tree.newLeaf(absolute(80f, 20f));
        tree.getStyle(safeFits).justifySelf = AlignItems.SAFE_END;

        TaffyStyle rootStyle = positioned(block(200f, 100f));
        rootStyle.direction = TaffyDirection.RTL;
        NodeId root = tree.newWithChildren(rootStyle, safeOverflow, defaultOverflow, safeFits);
        tree.computeLayout(root, TaffySize.maxContent());

        // RTL: logical end is the physical left edge, so the fallback anchors the box at the
        // physical right edge (200 - 240 = -40); the unsafe 'end' stays at the static left edge.
        assertLocation(tree, safeOverflow, -40f, 0f, "safe end falls back to the physical start");
        assertLocation(tree, defaultOverflow, 0f, 0f, "default end keeps the physical end edge");
        assertLocation(tree, safeFits, 0f, 0f, "safe end that fits stays at the physical end edge");
    }

    @Test
    @DisplayName("OF-06: explicit safe self-start/end resolves against the item's direction first")
    void safeSelfAlignmentResolvesAgainstItemDirection() {
        TaffyTree tree = new TaffyTree();
        // The item is LTR inside an RTL containing block: 'self-start' is the physical left edge,
        // i.e. the containing block's end, so 'safe self-start' falls back to the physical right.
        NodeId safeOverflow = tree.newLeaf(absolute(240f, 20f));
        explicitSelfAlignment(tree, safeOverflow, AlignItems.SAFE_SELF_START);
        NodeId defaultOverflow = tree.newLeaf(absolute(240f, 20f));
        explicitSelfAlignment(tree, defaultOverflow, AlignItems.SELF_START);
        NodeId safeFits = tree.newLeaf(absolute(80f, 20f));
        explicitSelfAlignment(tree, safeFits, AlignItems.SAFE_SELF_START);

        TaffyStyle rootStyle = positioned(block(200f, 100f));
        rootStyle.direction = TaffyDirection.RTL;
        NodeId root = tree.newWithChildren(rootStyle, safeOverflow, defaultOverflow, safeFits);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, safeOverflow, -40f, 0f, "safe self-start falls back to the CB start");
        assertLocation(tree, defaultOverflow, 0f, 0f, "unsafe self-start keeps the resolved edge");
        assertLocation(tree, safeFits, 0f, 0f, "safe self-start that fits stays on the resolved edge");
    }

    private static void explicitSelfAlignment(TaffyTree tree, NodeId node, AlignItems justifySelf) {
        TaffyStyle style = tree.getStyle(node);
        style.justifySelf = justifySelf;
        style.direction = TaffyDirection.LTR;
    }

    @Test
    @DisplayName("OF-06: safe align-self falls back within the block-axis IMCB")
    void safeStaticAlignSelfInBlockAxis() {
        TaffyTree tree = new TaffyTree();
        // An in-flow child pushes the static position of the following out-of-flow boxes to y=30.
        NodeId inFlow = tree.newLeaf(inFlowHeight(30f));
        NodeId safeCenterOverflow = tree.newLeaf(absolute(20f, 120f));
        tree.getStyle(safeCenterOverflow).alignSelf = AlignItems.SAFE_CENTER;
        NodeId defaultCenterOverflow = tree.newLeaf(absolute(20f, 120f));
        tree.getStyle(defaultCenterOverflow).alignSelf = AlignItems.CENTER;
        NodeId safeCenterFits = tree.newLeaf(absolute(20f, 40f));
        tree.getStyle(safeCenterFits).alignSelf = AlignItems.SAFE_CENTER;
        NodeId safeEndOverflow = tree.newLeaf(absolute(20f, 40f));
        tree.getStyle(safeEndOverflow).alignSelf = AlignItems.SAFE_END;
        NodeId defaultEndOverflow = tree.newLeaf(absolute(20f, 40f));
        tree.getStyle(defaultEndOverflow).alignSelf = AlignItems.END;

        NodeId root = tree.newWithChildren(
            positioned(block(200f, 100f)),
            inFlow, safeCenterOverflow, defaultCenterOverflow, safeCenterFits, safeEndOverflow, defaultEndOverflow);
        tree.computeLayout(root, TaffySize.maxContent());

        // Static line y=30; 'center' derived IMCB = [0, 60] (center 30, nearer to the start).
        assertLocation(tree, safeCenterOverflow, 0f, 0f, "safe center overflow falls back to y=0");
        assertLocation(tree, defaultCenterOverflow, 0f, -30f, "default center keeps the static line");
        assertLocation(tree, safeCenterFits, 0f, 10f, "safe center that fits uses the static line");
        // 'end' derived IMCB = [0, 30]: a 40px box overflows it.
        assertLocation(tree, safeEndOverflow, 0f, 0f, "safe end overflow falls back to y=0");
        assertLocation(tree, defaultEndOverflow, 0f, -10f, "default end keeps the static line");
    }

    // === OF-05 regression: the new branch must not move the non-safe numbers ===

    @Test
    @DisplayName("OF-06: OF-05 static and both-inset alignment numbers are unchanged")
    void of05NumbersUnchanged() {
        TaffyTree tree = new TaffyTree();
        NodeId start = tree.newLeaf(absolute(30f, 30f));
        tree.getStyle(start).justifySelf = AlignItems.START;
        NodeId center = tree.newLeaf(absolute(30f, 30f));
        tree.getStyle(center).justifySelf = AlignItems.CENTER;
        NodeId end = tree.newLeaf(absolute(30f, 30f));
        tree.getStyle(end).justifySelf = AlignItems.END;

        NodeId bothEnd = tree.newLeaf(absolute(20f, 20f));
        bothInsets(tree, bothEnd, AlignItems.END);
        NodeId bothCenter = tree.newLeaf(absolute(20f, 20f));
        bothInsets(tree, bothCenter, AlignItems.CENTER);
        NodeId bothSafeOverflow = tree.newLeaf(absolute(200f, 20f));
        bothInsets(tree, bothSafeOverflow, AlignItems.SAFE_CENTER);

        NodeId root = tree.newWithChildren(
            positioned(block(200f, 100f)), start, center, end, bothEnd, bothCenter, bothSafeOverflow);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, start, 0f, 0f, "justify-self: start");
        assertLocation(tree, center, 85f, 0f, "justify-self: center");
        assertLocation(tree, end, 170f, 0f, "justify-self: end");
        // IMCB = [20, 160] (width 140) for the both-inset children.
        assertLocation(tree, bothEnd, 140f, 0f, "both-inset end");
        assertLocation(tree, bothCenter, 80f, 0f, "both-inset center");
        assertLocation(tree, bothSafeOverflow, 20f, 0f, "both-inset safe center overflow");
    }

    private static void bothInsets(TaffyTree tree, NodeId node, AlignItems justifySelf) {
        TaffyStyle style = tree.getStyle(node);
        style.justifySelf = justifySelf;
        style.inset = TaffyRect.ltrb(
            LengthPercentageAuto.length(20f),
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.length(40f),
            LengthPercentageAuto.AUTO
        );
    }

    // === No OF-01 hoisting: the direct parent is the containing block ===

    @Test
    @DisplayName("OF-06: without OOF hoisting the direct parent supplies the IMCB")
    void directParentIsContainingBlock() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(absolute(150f, 20f));
        tree.getStyle(child).justifySelf = AlignItems.SAFE_END;

        // A static direct parent inside a positioned ancestor. Until OF-01 lands the child is laid
        // out by (and its IMCB is measured against) the direct parent, not the 300px outer
        // containing block: the static rectangle is [0, 100] and the 'end' derived IMCB is
        // [0, 100], so the 150px margin box overflows and falls back to x=0.
        TaffyStyle innerStyle = block(100f, 100f);
        innerStyle.position = TaffyPosition.STATIC;
        margin(innerStyle, 40f, 0f);
        NodeId inner = tree.newWithChildren(innerStyle, child);
        TaffyStyle outerStyle = positioned(block(300f, 300f));
        NodeId outer = tree.newWithChildren(outerStyle, inner);
        tree.computeLayout(outer, TaffySize.maxContent());

        assertLocation(tree, inner, 40f, 0f, "static parent offset inside the outer ancestor");
        assertLocation(tree, child, 0f, 0f, "direct parent is the containing block");
        assertSize(tree, child, 150f, 20f, "offending margin box keeps its size");
    }
}