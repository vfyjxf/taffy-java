package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TextAlign;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * P5 regression tests for CSS Box Alignment in block layout.
 *
 * <p>Two work items are covered:</p>
 *
 * <ul>
 *   <li><b>BK-16</b> (upstream #1250, {@code 0a52917}): an in-flow block-level child honours
 *       {@code justify-self}, falling back to the container's {@code justify-items}. A value that
 *       is neither {@code normal} nor {@code stretch} sizes an auto width as fit-content and
 *       aligns the child's margin box within the containing block; auto margins take precedence
 *       over both {@code justify-self} and {@code text-align}; {@code safe} alignment falls back
 *       to start on overflow; {@code stretch} stretches an auto-width table. Out-of-flow children
 *       are deliberately untouched (the static-position part is OF-05).</li>
 *   <li><b>BK-14</b> wiring: the block {@code align-content} offset now goes through the shared
 *       {@code AlignmentHelper} instead of the local switch, so the explicit {@code normal}
 *       keyword resolves to {@code START} for a block container and a {@code SAFE_*} value that
 *       overflows falls back to {@code START} (negative free space becomes a zero offset).</li>
 * </ul>
 *
 * <p>The expectations mirror the upstream generated fixtures
 * {@code tests/xml/block/block_justify_self_*__border_box_ltr.xml} and their {@code _rtl}
 * variants (browser-computed oracle, upstream 1abd8eea), plus the block {@code align-content}
 * safe-overflow behaviour. Each fixture is reproduced with a block root; the browser harness's
 * outer {@code #test-root} wrapper is not needed here.</p>
 *
 * <p>The task brief's direction matrix mentions column and wrap-reverse: block layout has no
 * flex axes, so those do not apply. The inline-axis cases that do apply are covered by an RTL
 * container and by a child whose own direction differs from the container's (the
 * {@code self-start}/{@code self-end} cross-direction flip).</p>
 *
 * <p>Deliberate nuance: for block {@code align-content}, {@code normal} is the layout-mode
 * default {@code START}; for an in-flow block-level {@code justify-self}, upstream #1250/#1254
 * keep {@code normal} as "the default block layout rules" (an auto width still stretches and
 * {@code text-align} still applies). An explicit {@code normal} nevertheless suppresses the
 * container {@code justify-items} fallback, which is what distinguishes it from the legacy
 * {@link AlignItems#AUTO} sentinel; the fixture
 * {@code block_justify_self_text_align_precedence} pins that behaviour.</p>
 */
public class BlockP5Test {

    private static final float EPS = 0.01f;

    // === Helpers ===

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

    private static TaffyStyle containerStyle(float width, float height) {
        TaffyStyle style = sized(width, height);
        style.position = TaffyPosition.RELATIVE;
        return style;
    }

    private static NodeId child(TaffyTree tree, float width, float height, AlignItems justifySelf) {
        TaffyStyle style = sized(width, height);
        style.justifySelf = justifySelf;
        return tree.newLeaf(style);
    }

    private static Layout layout(TaffyTree tree, NodeId node) {
        Layout layout = tree.getLayout(node);
        assertNotNull(layout, "node has no layout");
        return layout;
    }

    private static void assertLocation(TaffyTree tree, NodeId node, float x, float y, String message) {
        Layout layout = layout(tree, node);
        assertEquals(x, layout.location().x, EPS, message + " x");
        assertEquals(y, layout.location().y, EPS, message + " y");
    }

    private static void assertSize(TaffyTree tree, NodeId node, float width, float height, String message) {
        Layout layout = layout(tree, node);
        assertEquals(width, layout.size().width, EPS, message + " width");
        assertEquals(height, layout.size().height, EPS, message + " height");
    }

    /** Three 50x20 in-flow children in a 200x100 block container, as in the justify fixtures. */
    private static NodeId[] threeItems(TaffyTree tree, AlignItems... justifySelf) {
        NodeId[] nodes = new NodeId[justifySelf.length];
        for (int i = 0; i < justifySelf.length; i++) {
            nodes[i] = child(tree, 50f, 20f, justifySelf[i]);
        }
        return nodes;
    }

    // === BK-16: positional justify-self keywords ===

    @Test
    @DisplayName("BK-16: justify-self center/end/start place the margin box in an LTR container")
    void justifySelfPlacesLtr() {
        // block_justify_self_center__border_box_ltr
        TaffyTree tree = new TaffyTree();
        NodeId[] items = threeItems(tree, AlignItems.CENTER, AlignItems.END, AlignItems.START);
        NodeId root = tree.newWithChildren(containerStyle(200f, 100f), items);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, items[0], 75f, 0f, "center");
        assertLocation(tree, items[1], 150f, 20f, "end");
        assertLocation(tree, items[2], 0f, 40f, "start");
    }

    @Test
    @DisplayName("BK-16: the same keywords mirror in an RTL container")
    void justifySelfPlacesRtl() {
        // block_justify_self_center__border_box_rtl
        TaffyTree tree = new TaffyTree();
        NodeId[] items = threeItems(tree, AlignItems.CENTER, AlignItems.END, AlignItems.START);
        TaffyStyle style = containerStyle(200f, 100f);
        style.direction = TaffyDirection.RTL;
        NodeId root = tree.newWithChildren(style, items);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, items[0], 75f, 0f, "center");
        assertLocation(tree, items[1], 0f, 20f, "end");
        assertLocation(tree, items[2], 150f, 40f, "start");
    }

    @Test
    @DisplayName("BK-16: justify-items is the fallback for an AUTO justify-self")
    void justifyItemsIsTheAutoFallback() {
        // block_justify_items_end__border_box_ltr
        TaffyTree tree = new TaffyTree();
        NodeId[] items = threeItems(tree, AlignItems.AUTO, AlignItems.CENTER, AlignItems.AUTO);
        TaffyStyle style = containerStyle(200f, 100f);
        style.justifyItems = AlignItems.END;
        NodeId root = tree.newWithChildren(style, items);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, items[0], 150f, 0f, "auto inherits justify-items: end");
        assertLocation(tree, items[1], 75f, 20f, "explicit center wins");
        assertLocation(tree, items[2], 150f, 40f, "auto inherits justify-items: end");
    }

    @Test
    @DisplayName("BK-16: justify-items fallback mirrors for an RTL container")
    void justifyItemsIsTheAutoFallbackRtl() {
        // block_justify_items_end__border_box_rtl
        TaffyTree tree = new TaffyTree();
        NodeId[] items = threeItems(tree, AlignItems.AUTO, AlignItems.CENTER, AlignItems.AUTO);
        TaffyStyle style = containerStyle(200f, 100f);
        style.justifyItems = AlignItems.END;
        style.direction = TaffyDirection.RTL;
        NodeId root = tree.newWithChildren(style, items);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, items[0], 0f, 0f, "auto inherits justify-items: end (RTL)");
        assertLocation(tree, items[1], 75f, 20f, "explicit center wins");
        assertLocation(tree, items[2], 0f, 40f, "auto inherits justify-items: end (RTL)");
    }

    // === BK-16: self-start / self-end and the item's own direction ===

    @Test
    @DisplayName("BK-16: self-start/self-end resolve against the item's own direction")
    void selfKeywordsResolveAgainstItemDirection() {
        // block_justify_self_self_end__border_box_ltr: in an LTR container, self-end is the
        // physical end for an LTR item; an RTL item's self-start is also the physical end.
        TaffyTree tree = new TaffyTree();
        NodeId ltrSelfEnd = child(tree, 50f, 20f, AlignItems.SELF_END);
        TaffyStyle rtlStyle = sized(50f, 20f);
        rtlStyle.justifySelf = AlignItems.SELF_START;
        rtlStyle.direction = TaffyDirection.RTL;
        NodeId rtlSelfStart = tree.newLeaf(rtlStyle);
        NodeId root = tree.newWithChildren(containerStyle(200f, 100f), ltrSelfEnd, rtlSelfStart);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, ltrSelfEnd, 150f, 0f, "LTR item self-end");
        assertLocation(tree, rtlSelfStart, 150f, 20f, "RTL item self-start flips to end");
    }

    @Test
    @DisplayName("BK-16: self-start/self-end resolve against the item's own direction in RTL")
    void selfKeywordsResolveAgainstItemDirectionRtl() {
        // block_justify_self_self_end__border_box_rtl
        TaffyTree tree = new TaffyTree();
        NodeId selfEnd = child(tree, 50f, 20f, AlignItems.SELF_END);
        NodeId selfStart = child(tree, 50f, 20f, AlignItems.SELF_START);
        TaffyStyle style = containerStyle(200f, 100f);
        style.direction = TaffyDirection.RTL;
        NodeId root = tree.newWithChildren(style, selfEnd, selfStart);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, selfEnd, 0f, 0f, "RTL item self-end");
        assertLocation(tree, selfStart, 150f, 20f, "RTL item self-start");
    }

    // === BK-16: safe overflow fallback ===

    @Test
    @DisplayName("BK-16: safe justify-self falls back to start on overflow in LTR")
    void safeJustifySelfFallsBackToStart() {
        // block_justify_self_safe_center_overflow__border_box_ltr: 150-wide items in 100px.
        TaffyTree tree = new TaffyTree();
        NodeId safeCenter = child(tree, 150f, 20f, AlignItems.SAFE_CENTER);
        NodeId center = child(tree, 150f, 20f, AlignItems.CENTER);
        NodeId safeEnd = child(tree, 150f, 20f, AlignItems.SAFE_END);
        NodeId end = child(tree, 150f, 20f, AlignItems.END);
        NodeId root = tree.newWithChildren(containerStyle(100f, 100f), safeCenter, center, safeEnd, end);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, safeCenter, 0f, 0f, "safe center overflows -> start");
        assertLocation(tree, center, -25f, 20f, "unsafe center keeps the negative offset");
        assertLocation(tree, safeEnd, 0f, 40f, "safe end overflows -> start");
        assertLocation(tree, end, -50f, 60f, "unsafe end keeps the negative offset");
    }

    @Test
    @DisplayName("BK-16: safe justify-self falls back to start on overflow in RTL")
    void safeJustifySelfFallsBackToStartRtl() {
        // block_justify_self_safe_center_overflow__border_box_rtl: RTL start is the right edge.
        TaffyTree tree = new TaffyTree();
        NodeId safeCenter = child(tree, 150f, 20f, AlignItems.SAFE_CENTER);
        NodeId center = child(tree, 150f, 20f, AlignItems.CENTER);
        NodeId safeEnd = child(tree, 150f, 20f, AlignItems.SAFE_END);
        NodeId end = child(tree, 150f, 20f, AlignItems.END);
        TaffyStyle style = containerStyle(100f, 100f);
        style.direction = TaffyDirection.RTL;
        NodeId root = tree.newWithChildren(style, safeCenter, center, safeEnd, end);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, safeCenter, -50f, 0f, "safe center overflows -> start (right edge)");
        assertLocation(tree, center, -25f, 20f, "unsafe center keeps the negative offset");
        assertLocation(tree, safeEnd, -50f, 40f, "safe end overflows -> start (right edge)");
        assertLocation(tree, end, 0f, 60f, "unsafe end keeps the negative offset");
    }

    // === BK-16: text-align precedence and the legacy path ===

    @Test
    @DisplayName("BK-16: justify-self takes precedence over text-align, which still applies without it")
    void justifySelfPrecedesTextAlign() {
        // block_justify_self_text_align_precedence__border_box_ltr
        TaffyTree tree = new TaffyTree();
        NodeId start = child(tree, 50f, 20f, AlignItems.START);
        NodeId center = child(tree, 50f, 20f, AlignItems.CENTER);
        NodeId auto = child(tree, 50f, 20f, AlignItems.AUTO);
        TaffyStyle style = containerStyle(200f, 100f);
        style.textAlign = TextAlign.RIGHT;
        NodeId root = tree.newWithChildren(style, start, center, auto);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, start, 0f, 0f, "justify-self start beats text-align");
        assertLocation(tree, center, 75f, 20f, "justify-self center beats text-align");
        assertLocation(tree, auto, 150f, 40f, "no justify-self -> legacy text-align");
    }

    @Test
    @DisplayName("BK-16: an RTL container keeps the legacy text-align path without justify-self")
    void textAlignStillAppliesInRtl() {
        // block_justify_self_text_align_precedence__border_box_rtl
        TaffyTree tree = new TaffyTree();
        NodeId start = child(tree, 50f, 20f, AlignItems.START);
        NodeId center = child(tree, 50f, 20f, AlignItems.CENTER);
        NodeId auto = child(tree, 50f, 20f, AlignItems.AUTO);
        TaffyStyle style = containerStyle(200f, 100f);
        style.textAlign = TextAlign.RIGHT;
        style.direction = TaffyDirection.RTL;
        NodeId root = tree.newWithChildren(style, start, center, auto);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, start, 150f, 0f, "justify-self start beats text-align (RTL)");
        assertLocation(tree, center, 75f, 20f, "justify-self center beats text-align (RTL)");
        assertLocation(tree, auto, 150f, 40f, "no justify-self -> legacy text-align (RTL)");
    }

    @Test
    @DisplayName("P5: block items without justify-self keep the legacy x placement")
    void legacyXPlacementUnchanged() {
        // LTR: default is the content-box start; RTL: the content-box end.
        TaffyTree tree = new TaffyTree();
        NodeId ltrItem = child(tree, 50f, 20f, AlignItems.AUTO);
        NodeId root = tree.newWithChildren(containerStyle(200f, 100f), ltrItem);
        tree.computeLayout(root, TaffySize.maxContent());
        assertLocation(tree, ltrItem, 0f, 0f, "LTR legacy start");

        TaffyTree rtlTree = new TaffyTree();
        NodeId rtlItem = child(rtlTree, 50f, 20f, AlignItems.AUTO);
        TaffyStyle rtlStyle = containerStyle(200f, 100f);
        rtlStyle.direction = TaffyDirection.RTL;
        NodeId rtlRoot = rtlTree.newWithChildren(rtlStyle, rtlItem);
        rtlTree.computeLayout(rtlRoot, TaffySize.maxContent());
        assertLocation(rtlTree, rtlItem, 150f, 0f, "RTL legacy end");
    }

    // === BK-16: margins ===

    @Test
    @DisplayName("BK-16: justify-self aligns the margin box, not the border box")
    void justifySelfAlignsTheMarginBox() {
        // block_justify_self_with_margins__border_box_ltr
        TaffyTree tree = new TaffyTree();
        TaffyStyle centerStyle = sized(50f, 20f);
        centerStyle.justifySelf = AlignItems.CENTER;
        centerStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.length(10f), LengthPercentageAuto.length(30f),
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId center = tree.newLeaf(centerStyle);

        TaffyStyle endStyle = sized(50f, 20f);
        endStyle.justifySelf = AlignItems.END;
        endStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10f),
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId end = tree.newLeaf(endStyle);

        TaffyStyle startStyle = sized(50f, 20f);
        startStyle.justifySelf = AlignItems.START;
        startStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.length(10f), LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId start = tree.newLeaf(startStyle);

        NodeId root = tree.newWithChildren(containerStyle(200f, 100f), center, end, start);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, center, 65f, 0f, "center with 10/30 margins");
        assertLocation(tree, end, 140f, 20f, "end with 10px right margin");
        assertLocation(tree, start, 10f, 40f, "start with 10px left margin");
    }

    @Test
    @DisplayName("BK-16: justify-self aligns the margin box in RTL")
    void justifySelfAlignsTheMarginBoxRtl() {
        // block_justify_self_with_margins__border_box_rtl
        TaffyTree tree = new TaffyTree();
        TaffyStyle centerStyle = sized(50f, 20f);
        centerStyle.justifySelf = AlignItems.CENTER;
        centerStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.length(10f), LengthPercentageAuto.length(30f),
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId center = tree.newLeaf(centerStyle);

        TaffyStyle endStyle = sized(50f, 20f);
        endStyle.justifySelf = AlignItems.END;
        endStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10f),
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId end = tree.newLeaf(endStyle);

        TaffyStyle startStyle = sized(50f, 20f);
        startStyle.justifySelf = AlignItems.START;
        startStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.length(10f), LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId start = tree.newLeaf(startStyle);

        TaffyStyle style = containerStyle(200f, 100f);
        style.direction = TaffyDirection.RTL;
        NodeId root = tree.newWithChildren(style, center, end, start);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, center, 65f, 0f, "center with 10/30 margins (RTL)");
        assertLocation(tree, end, 0f, 20f, "end with 10px right margin (RTL)");
        assertLocation(tree, start, 150f, 40f, "start with 10px left margin (RTL)");
    }

    // === BK-16: auto margins win ===

    @Test
    @DisplayName("BK-16: auto margins take precedence over justify-self")
    void autoMarginsTakePrecedenceOverJustifySelf() {
        // block_justify_self_auto_margins_precedence__border_box_ltr (and the RTL fixture has the
        // same physical x values).
        TaffyTree tree = new TaffyTree();
        TaffyStyle endStyle = sized(50f, 20f);
        endStyle.justifySelf = AlignItems.END;
        endStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId end = tree.newLeaf(endStyle);

        TaffyStyle startStyle = sized(50f, 20f);
        startStyle.justifySelf = AlignItems.START;
        startStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId start = tree.newLeaf(startStyle);

        TaffyStyle bothStyle = sized(50f, 20f);
        bothStyle.justifySelf = AlignItems.START;
        bothStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId both = tree.newLeaf(bothStyle);

        NodeId root = tree.newWithChildren(containerStyle(200f, 100f), end, start, both);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, end, 0f, 0f, "right auto margin wins over end");
        assertLocation(tree, start, 150f, 20f, "left auto margin wins over start");
        assertLocation(tree, both, 75f, 40f, "two auto margins centre");
    }

    // === BK-16: fit-content auto width ===

    @Test
    @DisplayName("BK-16: a non-stretch justify-self sizes an auto width as fit-content")
    void nonStretchJustifySelfUsesFitContentWidth() {
        // block_justify_self_fit_content_width__border_box_ltr
        TaffyTree tree = new TaffyTree();

        TaffyStyle centerStyle = sized(Float.NaN, 20f);
        centerStyle.justifySelf = AlignItems.CENTER;
        NodeId centerInner = tree.newLeaf(sized(60f, 20f));
        NodeId center = tree.newWithChildren(centerStyle, centerInner);

        TaffyStyle endStyle = sized(Float.NaN, 20f);
        endStyle.justifySelf = AlignItems.END;
        endStyle.padding = TaffyRect.all(LengthPercentage.length(5f));
        NodeId endInner = tree.newLeaf(sized(60f, 10f));
        NodeId end = tree.newWithChildren(endStyle, endInner);

        TaffyStyle stretchStyle = sized(Float.NaN, 20f);
        stretchStyle.justifySelf = AlignItems.STRETCH;
        NodeId stretchInner = tree.newLeaf(sized(60f, 20f));
        NodeId stretch = tree.newWithChildren(stretchStyle, stretchInner);

        NodeId root = tree.newWithChildren(containerStyle(200f, 100f), center, end, stretch);
        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree, center, 60f, 20f, "fit-content center");
        assertLocation(tree, center, 70f, 0f, "fit-content center location");
        assertLocation(tree, centerInner, 0f, 0f, "fit-content center child");

        assertSize(tree, end, 70f, 20f, "fit-content end with padding");
        assertLocation(tree, end, 130f, 20f, "fit-content end location");
        assertLocation(tree, endInner, 5f, 5f, "fit-content end child");

        assertSize(tree, stretch, 200f, 20f, "stretch keeps the auto width stretch");
        assertLocation(tree, stretch, 0f, 40f, "stretch location");
        assertLocation(tree, stretchInner, 0f, 0f, "stretch child");
    }

    @Test
    @DisplayName("BK-16: fit-content auto width mirrors in an RTL container")
    void nonStretchJustifySelfUsesFitContentWidthRtl() {
        // block_justify_self_fit_content_width__border_box_rtl
        TaffyTree tree = new TaffyTree();

        TaffyStyle centerStyle = sized(Float.NaN, 20f);
        centerStyle.justifySelf = AlignItems.CENTER;
        NodeId centerInner = tree.newLeaf(sized(60f, 20f));
        NodeId center = tree.newWithChildren(centerStyle, centerInner);

        TaffyStyle endStyle = sized(Float.NaN, 20f);
        endStyle.justifySelf = AlignItems.END;
        endStyle.padding = TaffyRect.all(LengthPercentage.length(5f));
        NodeId endInner = tree.newLeaf(sized(60f, 10f));
        NodeId end = tree.newWithChildren(endStyle, endInner);

        TaffyStyle stretchStyle = sized(Float.NaN, 20f);
        stretchStyle.justifySelf = AlignItems.STRETCH;
        NodeId stretchInner = tree.newLeaf(sized(60f, 20f));
        NodeId stretch = tree.newWithChildren(stretchStyle, stretchInner);

        TaffyStyle style = containerStyle(200f, 100f);
        style.direction = TaffyDirection.RTL;
        NodeId root = tree.newWithChildren(style, center, end, stretch);
        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree, center, 60f, 20f, "fit-content center (RTL)");
        assertLocation(tree, center, 70f, 0f, "fit-content center location (RTL)");
        assertSize(tree, end, 70f, 20f, "fit-content end with padding (RTL)");
        assertLocation(tree, end, 0f, 20f, "fit-content end location (RTL)");
        assertSize(tree, stretch, 200f, 20f, "stretch keeps the auto width stretch (RTL)");
        assertLocation(tree, stretch, 0f, 40f, "stretch location (RTL)");
        assertLocation(tree, stretchInner, 140f, 0f, "stretch child (RTL legacy end)");
    }

    // === BK-16: explicit NORMAL versus the AUTO sentinel ===

    @Test
    @DisplayName("BK-16: an explicit NORMAL suppresses the container justify-items fallback")
    void explicitNormalOverridesJustifyItems() {
        TaffyTree tree = new TaffyTree();
        NodeId normal = child(tree, 50f, 20f, AlignItems.NORMAL);
        NodeId auto = child(tree, 50f, 20f, AlignItems.AUTO);
        TaffyStyle style = containerStyle(200f, 100f);
        style.justifyItems = AlignItems.END;
        NodeId root = tree.newWithChildren(style, normal, auto);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, normal, 0f, 0f, "explicit normal ignores justify-items: end");
        assertLocation(tree, auto, 150f, 20f, "auto inherits justify-items: end");
    }

    @Test
    @DisplayName("BK-16: NORMAL keeps the default block rules, so text-align still applies")
    void justifySelfNormalKeepsDefaultBlockRules() {
        // This is the deliberate upstream #1250/#1254 nuance: normal is not mapped to start for
        // an in-flow block-level justify-self (block_justify_self_text_align_precedence).
        TaffyTree tree = new TaffyTree();
        NodeId normal = child(tree, 50f, 20f, AlignItems.NORMAL);
        NodeId start = child(tree, 50f, 20f, AlignItems.START);
        TaffyStyle style = containerStyle(200f, 100f);
        style.justifyItems = AlignItems.END;
        style.textAlign = TextAlign.RIGHT;
        NodeId root = tree.newWithChildren(style, normal, start);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, normal, 150f, 0f, "normal keeps the default block rules (text-align)");
        assertLocation(tree, start, 0f, 20f, "start beats text-align");
    }

    // === BK-16: tables ===

    @Test
    @DisplayName("BK-16: justify-self stretch stretches an auto-width table")
    void stretchJustifySelfStretchesTable() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle tableStyle = sized(Float.NaN, 20f);
        tableStyle.itemIsTable = true;
        tableStyle.justifySelf = AlignItems.STRETCH;
        NodeId table = tree.newLeaf(tableStyle);
        NodeId root = tree.newWithChildren(containerStyle(200f, 100f), table);
        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree, table, 200f, 20f, "stretch applies the regular stretch width to a table");
        assertLocation(tree, table, 0f, 0f, "stretched table location");
    }

    // === BK-16: out-of-flow children are untouched ===

    @Test
    @DisplayName("BK-16: absolutely positioned children are not affected by justify-self")
    void absoluteChildrenIgnoreJustifySelf() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle absStyle = sized(30f, 30f);
        absStyle.position = TaffyPosition.ABSOLUTE;
        absStyle.justifySelf = AlignItems.END;
        NodeId absolute = tree.newLeaf(absStyle);
        NodeId root = tree.newWithChildren(containerStyle(200f, 100f), absolute);
        tree.computeLayout(root, TaffySize.maxContent());

        // The static position remains the content-box start; the OOF alignment is OF-05.
        assertLocation(tree, absolute, 0f, 0f, "out-of-flow child keeps its legacy static position");
    }

    // === BK-14: align-content through AlignmentHelper ===

    private static void assertStack(AlignContent alignContent, float y0, float y1, float y2) {
        TaffyTree tree = new TaffyTree();
        NodeId first = tree.newLeaf(sized(50f, 20f));
        NodeId second = tree.newLeaf(sized(50f, 20f));
        NodeId third = tree.newLeaf(sized(50f, 20f));
        TaffyStyle style = containerStyle(100f, 120f);
        style.alignContent = alignContent;
        NodeId root = tree.newWithChildren(style, first, second, third);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, first, 0f, y0, alignContent + " child 0");
        assertLocation(tree, second, 0f, y1, alignContent + " child 1");
        assertLocation(tree, third, 0f, y2, alignContent + " child 2");
    }

    @Test
    @DisplayName("BK-14: explicit NORMAL align-content resolves to START in block layout")
    void normalAlignContentResolvesToStart() {
        assertStack(AlignContent.NORMAL, 0f, 20f, 40f);
    }

    @Test
    @DisplayName("BK-14: legacy align-content keywords keep their pre-helper offsets")
    void legacyAlignContentOffsetsUnchanged() {
        // 100x120 container, three 20px children: free space = 60.
        assertStack(AlignContent.START, 0f, 20f, 40f);
        assertStack(AlignContent.FLEX_START, 0f, 20f, 40f);
        assertStack(AlignContent.END, 60f, 80f, 100f);
        assertStack(AlignContent.FLEX_END, 60f, 80f, 100f);
        assertStack(AlignContent.CENTER, 30f, 50f, 70f);
        assertStack(AlignContent.STRETCH, 0f, 20f, 40f);
        assertStack(AlignContent.SPACE_BETWEEN, 0f, 20f, 40f);
        assertStack(AlignContent.SPACE_AROUND, 30f, 50f, 70f);
        assertStack(AlignContent.SPACE_EVENLY, 30f, 50f, 70f);
    }

    @Test
    @DisplayName("BK-14: a SAFE_* align-content value falls back to start on overflow")
    void safeAlignContentFallsBackToStartOnOverflow() {
        // Two 80px children in a 100px-high container: free space = -60.
        TaffyTree safeEndTree = new TaffyTree();
        NodeId safeEndFirst = safeEndTree.newLeaf(sized(50f, 80f));
        NodeId safeEndSecond = safeEndTree.newLeaf(sized(50f, 80f));
        TaffyStyle safeEndStyle = containerStyle(100f, 100f);
        safeEndStyle.alignContent = AlignContent.SAFE_END;
        NodeId safeEndRoot = safeEndTree.newWithChildren(safeEndStyle, safeEndFirst, safeEndSecond);
        safeEndTree.computeLayout(safeEndRoot, TaffySize.maxContent());
        assertLocation(safeEndTree, safeEndFirst, 0f, 0f, "safe end overflow -> start");
        assertLocation(safeEndTree, safeEndSecond, 0f, 80f, "safe end overflow -> start, second child");

        TaffyTree safeCenterTree = new TaffyTree();
        NodeId safeCenterFirst = safeCenterTree.newLeaf(sized(50f, 80f));
        NodeId safeCenterSecond = safeCenterTree.newLeaf(sized(50f, 80f));
        TaffyStyle safeCenterStyle = containerStyle(100f, 100f);
        safeCenterStyle.alignContent = AlignContent.SAFE_CENTER;
        NodeId safeCenterRoot = safeCenterTree.newWithChildren(safeCenterStyle, safeCenterFirst, safeCenterSecond);
        safeCenterTree.computeLayout(safeCenterRoot, TaffySize.maxContent());
        assertLocation(safeCenterTree, safeCenterFirst, 0f, 0f, "safe center overflow -> start");
        assertLocation(safeCenterTree, safeCenterSecond, 0f, 80f, "safe center overflow -> start, second child");

        // The unsafe keywords keep overflowing with a negative offset.
        TaffyTree endTree = new TaffyTree();
        NodeId endFirst = endTree.newLeaf(sized(50f, 80f));
        NodeId endSecond = endTree.newLeaf(sized(50f, 80f));
        TaffyStyle endStyle = containerStyle(100f, 100f);
        endStyle.alignContent = AlignContent.END;
        NodeId endRoot = endTree.newWithChildren(endStyle, endFirst, endSecond);
        endTree.computeLayout(endRoot, TaffySize.maxContent());
        assertLocation(endTree, endFirst, 0f, -60f, "unsafe end keeps the negative offset");
        assertLocation(endTree, endSecond, 0f, 20f, "unsafe end second child");

        TaffyTree centerTree = new TaffyTree();
        NodeId centerFirst = centerTree.newLeaf(sized(50f, 80f));
        NodeId centerSecond = centerTree.newLeaf(sized(50f, 80f));
        TaffyStyle centerStyle = containerStyle(100f, 100f);
        centerStyle.alignContent = AlignContent.CENTER;
        NodeId centerRoot = centerTree.newWithChildren(centerStyle, centerFirst, centerSecond);
        centerTree.computeLayout(centerRoot, TaffySize.maxContent());
        assertLocation(centerTree, centerFirst, 0f, -30f, "unsafe center keeps the negative offset");
        assertLocation(centerTree, centerSecond, 0f, 50f, "unsafe center second child");
    }

    @Test
    @DisplayName("BK-14: a SAFE_* align-content value keeps its keyword when there is free space")
    void safeAlignContentKeepsKeywordWithoutOverflow() {
        TaffyTree safeEndTree = new TaffyTree();
        NodeId safeEndChild = safeEndTree.newLeaf(sized(50f, 20f));
        TaffyStyle safeEndStyle = containerStyle(100f, 120f);
        safeEndStyle.alignContent = AlignContent.SAFE_END;
        NodeId safeEndRoot = safeEndTree.newWithChildren(safeEndStyle, safeEndChild);
        safeEndTree.computeLayout(safeEndRoot, TaffySize.maxContent());
        assertLocation(safeEndTree, safeEndChild, 0f, 100f, "safe end with free space is end");

        TaffyTree safeCenterTree = new TaffyTree();
        NodeId safeCenterChild = safeCenterTree.newLeaf(sized(50f, 20f));
        TaffyStyle safeCenterStyle = containerStyle(100f, 120f);
        safeCenterStyle.alignContent = AlignContent.SAFE_CENTER;
        NodeId safeCenterRoot = safeCenterTree.newWithChildren(safeCenterStyle, safeCenterChild);
        safeCenterTree.computeLayout(safeCenterRoot, TaffySize.maxContent());
        assertLocation(safeCenterTree, safeCenterChild, 0f, 50f, "safe center with free space is center");

        TaffyTree safeStartTree = new TaffyTree();
        NodeId safeStartChild = safeStartTree.newLeaf(sized(50f, 20f));
        TaffyStyle safeStartStyle = containerStyle(100f, 120f);
        safeStartStyle.alignContent = AlignContent.SAFE_START;
        NodeId safeStartRoot = safeStartTree.newWithChildren(safeStartStyle, safeStartChild);
        safeStartTree.computeLayout(safeStartRoot, TaffySize.maxContent());
        assertLocation(safeStartTree, safeStartChild, 0f, 0f, "safe start is start");
    }

    @Test
    @DisplayName("BK-14: align-content AUTO remains a no-op")
    void alignContentAutoIsANoOp() {
        assertStack(AlignContent.AUTO, 0f, 20f, 40f);
    }
}
