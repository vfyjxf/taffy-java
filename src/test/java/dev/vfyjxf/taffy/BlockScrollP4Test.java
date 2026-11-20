package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.ScrollableOverflow;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P4 scrollable-overflow tests for block layout (SO-01/SO-02/SO-03).
 *
 * <p>Every expectation mirrors upstream taffy #1118/#1372 as adapted to this port's block
 * algorithm: a child contributes its border box unioned with the overflow it lets escape, in the
 * container's client-area frame; a scroll container additionally includes its own end padding and
 * drops the region before its scroll origin, while RTL moves that origin to the right edge and is
 * reported through {@link ScrollableOverflow#reversedX()} instead of mirroring the coordinates.</p>
 *
 * <p>The legacy {@code contentSize} component is asserted next to the rectangle: it still feeds
 * {@code scrollWidth()}/{@code scrollHeight()} until the integration switches them to the
 * rectangle, and its contributions are measured from the same client-area frame (upstream #1007).</p>
 */
public class BlockScrollP4Test {

    private static final float EPSILON = 0.001f;

    // === Fixture helpers ===

    private static TaffyStyle block() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        return style;
    }

    private static TaffyStyle block(float width, float height) {
        TaffyStyle style = block();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    /** A block container that clips unreachable overflow and includes its own end padding. */
    private static TaffyStyle scrollContainer(float width, float height) {
        TaffyStyle style = block(width, height);
        style.overflow = new TaffyPoint<>(Overflow.SCROLL, Overflow.SCROLL);
        return style;
    }

    private static TaffyStyle rtl(TaffyStyle style) {
        style.direction = TaffyDirection.RTL;
        return style;
    }

    private static TaffyStyle padding(TaffyStyle style, float value) {
        style.padding = TaffyRect.all(LengthPercentage.length(value));
        return style;
    }

    private static TaffyStyle border(TaffyStyle style, float value) {
        style.border = TaffyRect.all(LengthPercentage.length(value));
        return style;
    }

    private static TaffyStyle marginLeft(TaffyStyle style, float value) {
        style.margin = new TaffyRect<>(
            LengthPercentageAuto.length(value),
            LengthPercentageAuto.length(0f),
            LengthPercentageAuto.length(0f),
            LengthPercentageAuto.length(0f)
        );
        return style;
    }

    private static TaffyStyle absolute(float left, float top, float width, float height) {
        TaffyStyle style = block(width, height);
        style.position = TaffyPosition.ABSOLUTE;
        // TaffyRect's constructor is (left, right, top, bottom).
        style.inset = new TaffyRect<>(
            LengthPercentageAuto.length(left),
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.length(top),
            LengthPercentageAuto.AUTO
        );
        return style;
    }

    private static Layout layout(TaffyTree tree, NodeId root) {
        tree.disableRounding();
        tree.computeLayout(root, TaffySize.maxContent());
        Layout layout = tree.getLayout(root);
        assertTrue(layout != null, "root layout");
        return layout;
    }

    // === Assertions ===

    private static void assertOverflow(Layout layout, float left, float right, float top, float bottom,
                                       boolean reversedX, boolean reversedY, String message) {
        ScrollableOverflow overflow = layout.scrollableOverflow();
        FloatRect rect = overflow.rect();
        assertEquals(left, rect.left, EPSILON, message + " rect.left");
        assertEquals(right, rect.right, EPSILON, message + " rect.right");
        assertEquals(top, rect.top, EPSILON, message + " rect.top");
        assertEquals(bottom, rect.bottom, EPSILON, message + " rect.bottom");
        // The per-side accessors must agree with the rectangle (they are physical edges).
        assertEquals(rect.left, overflow.left(), EPSILON, message + " left()");
        assertEquals(rect.right, overflow.right(), EPSILON, message + " right()");
        assertEquals(rect.top, overflow.top(), EPSILON, message + " top()");
        assertEquals(rect.bottom, overflow.bottom(), EPSILON, message + " bottom()");
        assertEquals(reversedX, overflow.reversedX(), message + " reversedX");
        assertEquals(reversedY, overflow.reversedY(), message + " reversedY");
        assertTrue(rect.left <= 0f, message + " origin containment left");
        assertTrue(rect.top <= 0f, message + " origin containment top");
        assertTrue(rect.right >= 0f, message + " origin containment right");
        assertTrue(rect.bottom >= 0f, message + " origin containment bottom");
    }

    private static void assertContentSize(Layout layout, float width, float height, String message) {
        assertEquals(width, layout.contentSize().width, EPSILON, message + " contentSize.width");
        assertEquals(height, layout.contentSize().height, EPSILON, message + " contentSize.height");
    }

    // === Non-scroll containers ===

    @Test
    @DisplayName("SO-01: a visible block container reports the overflow of its children")
    void visibleContainerReportsChildOverflow() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(block(200f, 50f));
        NodeId root = tree.newWithChildren(block(100f, 100f), child);

        Layout rootLayout = layout(tree, root);

        // The child's border box (200x50 at the origin) is the whole contribution: the leaf is not
        // a scroll container and has no content of its own to overflow.
        assertOverflow(rootLayout, 0f, 200f, 0f, 50f, false, false, "visible container");
        assertContentSize(rootLayout, 200f, 50f, "visible container");
    }

    @Test
    @DisplayName("SO-02: a non-scroll container keeps start-side overflow for propagation")
    void nonScrollContainerKeepsNegativeOverflow() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(marginLeft(block(50f, 50f), -30f));
        NodeId root = tree.newWithChildren(block(100f, 100f), child);

        Layout rootLayout = layout(tree, root);

        // Nothing clips a non-scroll container, so the negative start edge survives: only the
        // origin containment in the finalized rectangle bounds it.
        assertOverflow(rootLayout, -30f, 20f, 0f, 50f, false, false, "non-scroll container");
        // The legacy content size is an extent, not a rectangle, so a contribution that starts
        // before the origin and ends after it still measures from the origin: -30 + 50 = 20.
        assertContentSize(rootLayout, 20f, 50f, "non-scroll container");
    }

    // === Scroll containers ===

    @Test
    @DisplayName("SO-02: a scroll container includes its own end padding (#1051/#1114)")
    void scrollContainerIncludesItsOwnEndPadding() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(block(80f, 20f));
        NodeId root = tree.newWithChildren(padding(scrollContainer(100f, 100f), 10f), child);

        Layout rootLayout = layout(tree, root);

        // The child sits at (10, 10); the scroll container's end padding is reachable, so the
        // rectangle reaches the client area's far edge (100 - 10 = 90 for the child, +10 padding).
        assertOverflow(rootLayout, 0f, 100f, 0f, 40f, false, false, "scroll container end padding");
        assertContentSize(rootLayout, 100f, 40f, "scroll container end padding");

        // The same box without a scroll container does not extend its overflow by its own padding:
        // the child's own extent (10 + 80, 10 + 20) is all of it.
        TaffyTree visibleTree = new TaffyTree();
        NodeId visibleChild = visibleTree.newLeaf(block(80f, 20f));
        NodeId visibleRoot = visibleTree.newWithChildren(padding(block(100f, 100f), 10f), visibleChild);

        Layout visibleLayout = layout(visibleTree, visibleRoot);
        assertOverflow(visibleLayout, 0f, 90f, 0f, 30f, false, false, "visible container end padding");
        assertContentSize(visibleLayout, 90f, 30f, "visible container end padding");
    }

    @Test
    @DisplayName("SO-02: a negative end margin cancels a scroll container's end padding (#1372)")
    void negativeEndMarginCancelsTheEndPadding() {
        // The 10px end padding is the difference between the child's own extent (90) and the
        // client area's far edge (100); a -10px end margin removes exactly that difference.
        TaffyTree tree = new TaffyTree();
        TaffyStyle childStyle = block(80f, 20f);
        childStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.length(0f),
            LengthPercentageAuto.length(-10f),
            LengthPercentageAuto.length(0f),
            LengthPercentageAuto.length(0f)
        );
        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(padding(scrollContainer(100f, 100f), 10f), child);

        Layout rootLayout = layout(tree, root);

        assertOverflow(rootLayout, 0f, 90f, 0f, 40f, false, false, "negative end margin");
    }

    @Test
    @DisplayName("SO-02: negative overflow wholly before the scroll origin is dropped (#1116)")
    void unreachableOverflowBeforeTheScrollOriginIsDropped() {
        // The child spans -80..-30 in the client frame: entirely unreachable by scrolling.
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(marginLeft(block(50f, 50f), -80f));
        NodeId root = tree.newWithChildren(scrollContainer(100f, 100f), child);

        Layout rootLayout = layout(tree, root);

        assertOverflow(rootLayout, 0f, 0f, 0f, 0f, false, false, "unreachable child in a scroller");
        // The legacy content size is a Size, so it cannot express a negative extent: the
        // start-side overflow is floored at zero per axis (upstream's f32_max accumulation),
        // which leaves only the block-axis extent of the child that was dropped from the
        // rectangle.
        assertContentSize(rootLayout, 0f, 50f, "unreachable child in a scroller");

        // The same child in a non-scroll container is kept whole (nothing clips it).
        TaffyTree visibleTree = new TaffyTree();
        NodeId visibleChild = visibleTree.newLeaf(marginLeft(block(50f, 50f), -80f));
        NodeId visibleRoot = visibleTree.newWithChildren(block(100f, 100f), visibleChild);

        assertOverflow(layout(visibleTree, visibleRoot), -80f, 0f, 0f, 50f, false, false,
            "unreachable child in a visible box");
    }

    @Test
    @DisplayName("SO-02: overflow crossing the scroll origin keeps its reachable part and starts at 0")
    void overflowCrossingTheScrollOriginIsClippedAtTheStart() {
        // The child spans -30..20: the part before the origin is unreachable, the rest is kept.
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(marginLeft(block(50f, 50f), -30f));
        NodeId root = tree.newWithChildren(scrollContainer(100f, 100f), child);

        Layout rootLayout = layout(tree, root);

        assertOverflow(rootLayout, 0f, 20f, 0f, 50f, false, false, "crossing the scroll origin");
        // finalizeScrollableOverflow resets a normal axis' start edge to zero.
        assertEquals(0f, rootLayout.scrollableOverflow().left(), EPSILON, "clipped start edge");
        assertFalse(rootLayout.scrollableOverflow().reversedX(), "normal scroll origin");
    }

    @Test
    @DisplayName("SO-02: a scroll container includes its end padding with an empty content box")
    void emptyScrollContainerReportsItsEndPadding() {
        // A container whose only child is display:none has no contributions but is still laid out
        // by this algorithm (a childless node would take the leaf path), so this is the block
        // algorithm's empty-container case.
        TaffyStyle hiddenStyle = block(30f, 30f);
        hiddenStyle.display = TaffyDisplay.NONE;

        // An empty scroll container still has a reachable region: its own end padding.
        TaffyTree tree = new TaffyTree();
        NodeId hidden = tree.newLeaf(hiddenStyle);
        NodeId root = tree.newWithChildren(padding(scrollContainer(100f, 100f), 10f), hidden);

        Layout rootLayout = layout(tree, root);

        assertOverflow(rootLayout, 0f, 10f, 0f, 10f, false, false, "empty scroll container");
        assertContentSize(rootLayout, 10f, 10f, "empty scroll container");

        // The same empty container in RTL: the scroll origin is the right edge of the client area,
        // which is unioned in even with no reachable child content.
        TaffyTree rtlTree = new TaffyTree();
        NodeId rtlHidden = rtlTree.newLeaf(hiddenStyle);
        NodeId rtlRoot = rtlTree.newWithChildren(
            rtl(padding(scrollContainer(100f, 100f), 10f)), rtlHidden);

        assertOverflow(layout(rtlTree, rtlRoot), 0f, 100f, 0f, 10f, true, false,
            "empty RTL scroll container");

        // An empty container that does not scroll has no overflow at all.
        TaffyTree visibleTree = new TaffyTree();
        NodeId visibleHidden = visibleTree.newLeaf(hiddenStyle);
        NodeId visibleRoot = visibleTree.newWithChildren(block(100f, 100f), visibleHidden);
        assertOverflow(layout(visibleTree, visibleRoot), 0f, 0f, 0f, 0f, false, false,
            "empty visible container");
    }

    @Test
    @DisplayName("SO-03: scrollHeight is the content beyond the padding box on any border edge (#1007)")
    void scrollSizeMatchesThePaddingBoxAcrossBorderEdges() {
        // Upstream tests/hand_written/scroll_size.rs, block half: a 300x200 border-box scroller
        // holding 1000px of content must report the same maximum scroll offset whether the 20px
        // border is on the leading or the trailing edge, and its content size must not include
        // the container's own border.
        float content = 1000f;
        float container = 200f;
        float borderWidth = 20f;

        for (float[] edges : new float[][] {
            {borderWidth, 0f}, {0f, borderWidth}, {borderWidth, borderWidth}, {0f, 0f}
        }) {
            float top = edges[0];
            float bottom = edges[1];
            String message = "border " + top + "/" + bottom;

            TaffyTree tree = new TaffyTree();
            NodeId child = tree.newLeaf(block(100f, content));
            TaffyStyle style = block(300f, container);
            style.boxSizing = BoxSizing.BORDER_BOX;
            style.overflow = new TaffyPoint<>(Overflow.SCROLL, Overflow.SCROLL);
            style.border = new TaffyRect<>(
                LengthPercentage.length(0f),
                LengthPercentage.length(0f),
                LengthPercentage.length(top),
                LengthPercentage.length(bottom)
            );
            NodeId root = tree.newWithChildren(style, child);

            Layout rootLayout = layout(tree, root);

            // The reachable extent is the content beyond the padding box: 1000 - (200 - borders).
            assertEquals(content - (container - top - bottom), rootLayout.scrollHeight(), EPSILON,
                message + " scrollHeight");
            // The content size starts at the client area, so the container's own border is not
            // part of it: it is the child's own extent in both axes.
            assertContentSize(rootLayout, 100f, content, message);
            // The child sits at the client-area origin, so the rectangle is the child's extent.
            assertOverflow(rootLayout, 0f, 100f, 0f, content, false, false, message);
        }
    }

    // === RTL / reversed origins ===

    @Test
    @DisplayName("SO-02: an RTL scroll container scrolls towards the left edge (reversed origin)")
    void rtlScrollContainerUsesAReversedOrigin() {
        // The 150px child of a 100px container overflows towards the inline end, which is the
        // physical left in RTL: it spans -50..100 in the client frame.
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(block(150f, 50f));
        NodeId root = tree.newWithChildren(rtl(scrollContainer(100f, 100f)), child);

        Layout rootLayout = layout(tree, root);

        assertOverflow(rootLayout, -50f, 100f, 0f, 50f, true, false, "RTL scroll container");
        assertContentSize(rootLayout, 150f, 50f, "RTL scroll container");

        // The reachable range is the same 50px as in LTR, where the child spans 0..150.
        TaffyTree ltrTree = new TaffyTree();
        NodeId ltrChild = ltrTree.newLeaf(block(150f, 50f));
        NodeId ltrRoot = ltrTree.newWithChildren(scrollContainer(100f, 100f), ltrChild);

        Layout ltrLayout = layout(ltrTree, ltrRoot);
        assertOverflow(ltrLayout, 0f, 150f, 0f, 50f, false, false, "LTR scroll container");
        assertEquals(-rootLayout.scrollableOverflow().left(),
            ltrLayout.scrollableOverflow().right() - 100f, EPSILON,
            "the reachable scroll range does not depend on the writing direction");
    }

    @Test
    @DisplayName("SO-02: an RTL non-scroll container reports physical edges without a reversed origin")
    void rtlVisibleContainerKeepsPhysicalCoordinates() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(block(150f, 50f));
        NodeId root = tree.newWithChildren(rtl(block(100f, 100f)), child);

        Layout rootLayout = layout(tree, root);

        // A non-scroll container keeps all of its overflow and has no reversed origins: the
        // rectangle stays in physical coordinates.
        assertOverflow(rootLayout, -50f, 100f, 0f, 50f, false, false, "RTL visible container");
        assertContentSize(rootLayout, 150f, 50f, "RTL visible container");
    }

    // === Out-of-flow children ===

    @Test
    @DisplayName("SO-01: absolutely positioned children contribute to the scrollable overflow")
    void absoluteChildrenContributeToScrollableOverflow() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(block(10f, 10f));
        NodeId absChild = tree.newLeaf(absolute(50f, 50f, 80f, 40f));
        NodeId root = tree.newWithChildren(block(100f, 100f), child, absChild);

        Layout rootLayout = layout(tree, root);

        // The abs child is positioned against the container's client area: 50..130, 50..90.
        assertOverflow(rootLayout, 0f, 130f, 0f, 90f, false, false, "abs child contribution");
        assertContentSize(rootLayout, 130f, 90f, "abs child contribution");
    }

    @Test
    @DisplayName("SO-01: a scroll container drops an unreachable absolutely positioned child")
    void scrollContainerDropsAnUnreachableAbsoluteChild() {
        TaffyTree tree = new TaffyTree();
        NodeId absChild = tree.newLeaf(absolute(-200f, -200f, 80f, 40f));
        NodeId root = tree.newWithChildren(scrollContainer(100f, 100f), absChild);

        Layout rootLayout = layout(tree, root);

        assertOverflow(rootLayout, 0f, 0f, 0f, 0f, false, false, "unreachable abs child");
        assertContentSize(rootLayout, 0f, 0f, "unreachable abs child");
    }

    // === Nested boxes: child border and propagated overflow ===

    @Test
    @DisplayName("SO-02: a nested child contributes its own overflow, its border is added back")
    void nestedChildOverflowAndBorderAreIncluded() {
        TaffyTree tree = new TaffyTree();
        NodeId grandChild = tree.newLeaf(block(300f, 300f));
        TaffyStyle innerStyle = border(block(100f, 100f), 5f);
        NodeId inner = tree.newWithChildren(innerStyle, grandChild);
        NodeId root = tree.newWithChildren(block(100f, 100f), inner);

        Layout rootLayout = layout(tree, root);
        Layout innerLayout = tree.getLayout(inner);

        // The inner box is not a scroll container, so it keeps the grandchild's overflow; its own
        // rectangle is relative to its client area, which starts after the 5px border.
        assertOverflow(innerLayout, 0f, 300f, 0f, 300f, false, false, "nested inner box");

        // The parent sees the child's client-area rectangle plus the child's border (300 + 5).
        assertOverflow(rootLayout, 0f, 305f, 0f, 305f, false, false, "nested child contribution");
    }

    @Test
    @DisplayName("SO-02: a nested scroll container contributes only its border box")
    void nestedScrollContainerChildContributesOnlyItsBorderBox() {
        TaffyTree tree = new TaffyTree();
        NodeId grandChild = tree.newLeaf(block(300f, 300f));
        NodeId inner = tree.newWithChildren(scrollContainer(100f, 100f), grandChild);
        NodeId root = tree.newWithChildren(block(100f, 100f), inner);

        Layout rootLayout = layout(tree, root);

        // The inner scroll container clips its own overflow, so only its border box escapes.
        assertOverflow(rootLayout, 0f, 100f, 0f, 100f, false, false, "nested scroll container");
        assertContentSize(rootLayout, 100f, 100f, "nested scroll container");

        Layout innerLayout = tree.getLayout(inner);
        assertOverflow(innerLayout, 0f, 300f, 0f, 300f, false, false, "nested scroll container itself");
    }
}
