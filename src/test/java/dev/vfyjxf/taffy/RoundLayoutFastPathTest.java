package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.ScrollableOverflow;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the allocation-free steady state of the rounding pass.
 *
 * <p>Whole-pixel layouts round to themselves, so the pass does not have to replace the stored
 * final layout on every call: when the rounded output is bit-identical to the unrounded layout and
 * the stored layout already holds it, the pass only keeps the bookkeeping (new-layout flag,
 * ancestor dirty-descendant propagation, listener notification). These tests pin that behavior and
 * its boundary: identity of the kept layout, listener arguments and ordering, flags across
 * acknowledge and re-round, and the cases that must keep taking the full rebuild path
 * (fractional values, NaN, signed zeros, the scrollable-overflow compatibility placeholder).
 */
public class RoundLayoutFastPathTest {

    /** A computed (non-placeholder) empty scrollable overflow: identity matters to Layout. */
    private static ScrollableOverflow emptyOverflow() {
        return new ScrollableOverflow(FloatRect.zero(), false, false);
    }

    /** A whole-pixel layout whose components all round to themselves. */
    private static Layout integerLayout(float x, float y, float width, float height) {
        return new Layout(
            0,
            new FloatPoint(x, y),
            new FloatSize(width, height),
            new FloatSize(width, height),
            emptyOverflow(),
            FloatSize.zero(),
            FloatRect.zero(),
            FloatRect.zero(),
            FloatRect.zero()
        );
    }

    /** A tree whose layouts are all whole pixels: root (0,0,w,h), children below each other. */
    private static IntegerTree integerTree(int childCount) {
        TaffyTree tree = new TaffyTree();
        NodeId[] children = new NodeId[childCount];
        for (int i = 0; i < childCount; i++) {
            children[i] = tree.newLeaf(new TaffyStyle());
        }
        NodeId root = tree.newWithChildren(new TaffyStyle(), children);
        for (int i = 0; i < childCount; i++) {
            tree.setUnroundedLayout(children[i], integerLayout(10f, 10f * i, 10f, 10f));
        }
        tree.setUnroundedLayout(root, integerLayout(0f, 0f, 100f, 10f * childCount));
        return new IntegerTree(tree, root, children);
    }

    private static final class IntegerTree {
        final TaffyTree tree;
        final NodeId root;
        final NodeId[] children;

        IntegerTree(TaffyTree tree, NodeId root, NodeId[] children) {
            this.tree = tree;
            this.root = root;
            this.children = children;
        }
    }

    @Test
    @DisplayName("whole-pixel layouts keep the stored layout on the next pass")
    void wholePixelLayoutsKeepTheStoredLayoutOnTheNextPass() {
        IntegerTree fixture = integerTree(2);
        TaffyTree tree = fixture.tree;
        List<NodeId> visited = new ArrayList<>();
        tree.setLayoutChangeListener((node, oldLayout, newLayout) -> visited.add(node));

        tree.roundLayout(fixture.root);

        Layout rootFirst = tree.getLayout(fixture.root);
        Layout firstChildFirst = tree.getLayout(fixture.children[0]);
        Layout secondChildFirst = tree.getLayout(fixture.children[1]);

        // The rounded output is always its own Layout: the unrounded layout is never aliased.
        assertNotSame(tree.getUnroundedLayout(fixture.root), rootFirst, "root is not the unrounded layout");
        assertNotSame(tree.getUnroundedLayout(fixture.children[0]), firstChildFirst,
            "child is not the unrounded layout");
        assertEquals(100f, rootFirst.size().width, 0f, "rounded root width");
        assertEquals(20f, rootFirst.size().height, 0f, "rounded root height");
        assertEquals(0f, firstChildFirst.location().y, 0f, "rounded first child y");
        assertEquals(10f, secondChildFirst.location().y, 0f, "rounded second child y");
        assertEquals(List.of(fixture.root, fixture.children[0], fixture.children[1]), visited,
            "one notification per node in pre-order");

        // Second pass: rounding is a no-op, so the stored layouts must survive untouched.
        tree.roundLayout(fixture.root);

        assertSame(rootFirst, tree.getLayout(fixture.root), "root layout is kept");
        assertSame(firstChildFirst, tree.getLayout(fixture.children[0]), "first child layout is kept");
        assertSame(secondChildFirst, tree.getLayout(fixture.children[1]), "second child layout is kept");
        assertEquals(List.of(fixture.root, fixture.children[0], fixture.children[1],
                fixture.root, fixture.children[0], fixture.children[1]), visited,
            "the no-op pass still notifies every node in pre-order");
    }

    @Test
    @DisplayName("the no-op pass reports the kept layout as both old and new")
    void noOpPassReportsTheKeptLayoutAsOldAndNew() {
        IntegerTree fixture = integerTree(1);
        TaffyTree tree = fixture.tree;
        AtomicBoolean secondPass = new AtomicBoolean(false);
        AtomicInteger noOpNotifications = new AtomicInteger();

        tree.setLayoutChangeListener((node, oldLayout, newLayout) -> {
            if (secondPass.get()) {
                assertSame(oldLayout, newLayout, "no-op pass: old and new are the kept layout");
                assertSame(newLayout, tree.getLayout(node), "the notified layout is the visible layout");
                noOpNotifications.incrementAndGet();
            } else {
                assertNotSame(oldLayout, newLayout, "a rebuild pass reports a replaced layout");
            }
        });

        tree.roundLayout(fixture.root);
        secondPass.set(true);
        tree.roundLayout(fixture.root);

        assertEquals(2, noOpNotifications.get(), "root and its child are both notified on the no-op pass");
    }

    @Test
    @DisplayName("the no-op pass keeps the new-layout flag, dirty propagation and listener count")
    void noOpPassKeepsNewLayoutFlagAndDirtyPropagation() {
        IntegerTree fixture = integerTree(2);
        TaffyTree tree = fixture.tree;
        tree.roundLayout(fixture.root);

        // Acknowledge the whole tree, so every flag starts cleared.
        for (NodeId node : List.of(fixture.root, fixture.children[0], fixture.children[1])) {
            tree.acknowledgeSubtree(node);
        }
        for (NodeId node : List.of(fixture.root, fixture.children[0], fixture.children[1])) {
            assertFalse(tree.hasNewLayout(node), "flag cleared for " + node);
            assertFalse(tree.needsVisit(node), "no pending visit for " + node);
        }

        List<NodeId> visited = new ArrayList<>();
        tree.setLayoutChangeListener((node, oldLayout, newLayout) -> visited.add(node));

        tree.roundLayout(fixture.root);

        assertEquals(List.of(fixture.root, fixture.children[0], fixture.children[1]), visited,
            "the no-op pass notifies every node exactly once");
        for (NodeId node : List.of(fixture.root, fixture.children[0], fixture.children[1])) {
            assertTrue(tree.hasNewLayout(node), "new-layout flag set again for " + node);
        }
        assertTrue(tree.hasDirtyDescendant(fixture.root), "root sees dirty descendants again");
        assertFalse(tree.hasDirtyDescendant(fixture.children[0]), "leaf has no dirty descendant");
        assertFalse(tree.hasDirtyDescendant(fixture.children[1]), "leaf has no dirty descendant");
    }

    @Test
    @DisplayName("fractional layouts still round to whole pixels")
    void fractionalLayoutsStillRound() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(new TaffyStyle());
        NodeId root = tree.newWithChildren(new TaffyStyle(), child);
        tree.setUnroundedLayout(root, integerLayout(0f, 0f, 100f, 100f));
        tree.setUnroundedLayout(child, integerLayout(0.5f, 0.5f, 2.5f, 2.5f));

        tree.roundLayout(root);

        Layout rootFirst = tree.getLayout(root);
        Layout childFirst = tree.getLayout(child);
        assertEquals(100f, rootFirst.size().width, 0f, "root width");
        assertEquals(1f, childFirst.location().x, 0f, "round(0.5) ties away from zero");
        assertEquals(1f, childFirst.location().y, 0f, "round(0.5) ties away from zero");
        assertEquals(2f, childFirst.size().width, 0f, "width = round(3.0) - round(0.5)");
        assertEquals(2f, childFirst.size().height, 0f, "height = round(3.0) - round(0.5)");

        // The whole-pixel root is still kept; the fractional child is re-rounded to the same values.
        tree.roundLayout(root);
        assertSame(rootFirst, tree.getLayout(root), "whole-pixel root layout is kept");
        assertEquals(childFirst, tree.getLayout(child), "repeated rounding is idempotent for fractions");
    }

    @Test
    @DisplayName("NaN, negative and overflow components keep their rounding semantics")
    void nanNegativeAndOverflowSemanticsAreUnchanged() {
        TaffyTree tree = new TaffyTree();
        NodeId root = tree.newLeaf(new TaffyStyle());
        tree.setUnroundedLayout(root, new Layout(
            3,
            new FloatPoint(Float.NaN, 1.5f),
            new FloatSize(10.5f, 2.25f),
            new FloatSize(Float.NaN, 3.75f),
            new ScrollableOverflow(FloatRect.of(1.5f, 2.5f, -1.5f, -2.5f), true, false),
            new FloatSize(0.5f, 1.5f),
            new FloatRect(Float.NaN, 1.5f, 2.5f, -1.5f),
            FloatRect.of(0.5f, -0.5f, 2.5f, -2.5f),
            FloatRect.zero()
        ));

        tree.roundLayout(root);

        Layout rounded = tree.getLayout(root);
        assertEquals(3, rounded.order(), "order is carried through");
        assertEquals(0f, rounded.location().x, 0f, "NaN location becomes 0");
        assertEquals(2f, rounded.location().y, 0f, "round(1.5) ties away from zero");
        assertEquals(11f, rounded.size().width, 0f, "width = round(10.5)");
        assertEquals(2f, rounded.size().height, 0f, "height = round(3.75) - round(1.5)");
        assertEquals(0f, rounded.contentSize().width, 0f, "NaN content width becomes 0");
        assertEquals(3f, rounded.contentSize().height, 0f, "content height = 5 - 2");
        assertEquals(1f, rounded.scrollbarSize().width, 0f, "scrollbar width");
        assertEquals(2f, rounded.scrollbarSize().height, 0f, "scrollbar height");

        FloatRect overflow = rounded.scrollableOverflow().rect();
        assertEquals(2f, overflow.left, 0f, "overflow left");
        assertEquals(3f, overflow.right, 0f, "overflow right");
        assertEquals(-2f, overflow.top, 0f, "overflow top");
        assertEquals(-3f, overflow.bottom, 0f, "overflow bottom");
        assertTrue(rounded.scrollableOverflow().reversedX(), "reversedX flag kept");
        assertFalse(rounded.scrollableOverflow().reversedY(), "reversedY flag kept");

        assertEquals(0f, rounded.border().left, 0f, "NaN border edge becomes 0");
        assertEquals(2f, rounded.border().right, 0f, "border right");
        assertEquals(3f, rounded.border().top, 0f, "border top");
        assertEquals(-2f, rounded.border().bottom, 0f, "border bottom");
        assertEquals(1f, rounded.padding().left, 0f, "padding left");
        assertEquals(-1f, rounded.padding().right, 0f, "padding right");
        assertEquals(3f, rounded.padding().top, 0f, "padding top");
        assertEquals(-3f, rounded.padding().bottom, 0f, "padding bottom");

        // Fractional/NaN/transformed components never take the keep path: re-rounding rebuilds the
        // layout and lands on exactly the same values.
        tree.roundLayout(root);
        assertEquals(rounded, tree.getLayout(root), "repeated rounding is idempotent");
    }

    @Test
    @DisplayName("a signed-zero overflow edge is rounded, not kept")
    void signedZeroOverflowEdgeIsRounded() {
        TaffyTree tree = new TaffyTree();
        NodeId root = tree.newLeaf(new TaffyStyle());
        tree.setUnroundedLayout(root, new Layout(
            0,
            new FloatPoint(0f, 0f),
            new FloatSize(10f, 10f),
            new FloatSize(10f, 10f),
            new ScrollableOverflow(new FloatRect(-0.3f, 10f, -0.4f, 10f), false, true),
            FloatSize.zero(),
            FloatRect.zero(),
            FloatRect.zero(),
            FloatRect.zero()
        ));

        tree.roundLayout(root);
        tree.roundLayout(root);

        ScrollableOverflow rounded = tree.getLayout(root).scrollableOverflow();
        assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(rounded.left()),
            "-0.0 start edge survives rounding");
        assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(rounded.top()),
            "-0.0 start edge survives rounding");
        assertTrue(rounded.reversedY(), "reversedY flag kept");
        assertFalse(rounded.reversedX(), "reversedX flag kept");
        assertEquals(10f, rounded.right(), 0f, "right edge");
    }

    @Test
    @DisplayName("the overflow compatibility placeholder is never kept as the final layout")
    void compatibilityPlaceholderOverflowIsNeverKept() {
        TaffyTree tree = new TaffyTree();
        NodeId root = tree.newLeaf(new TaffyStyle());

        // The rounded layout the pass builds always carries a freshly built overflow rectangle; a
        // hand-built 8-arg layout carries the shared ZERO placeholder, whose identity Layout uses
        // to keep the legacy contentSize scroll formula. The values are equal, so only the identity
        // guard stops the placeholder from being kept.
        tree.setUnroundedLayout(root, new Layout(
            0,
            new FloatPoint(0f, 0f),
            new FloatSize(10f, 10f),
            new FloatSize(20f, 20f),
            emptyOverflow(),
            FloatSize.zero(),
            FloatRect.zero(),
            FloatRect.zero(),
            FloatRect.zero()
        ));
        tree.setLayout(root, new Layout(
            0,
            new FloatPoint(0f, 0f),
            new FloatSize(10f, 10f),
            new FloatSize(20f, 20f),
            FloatSize.zero(),
            FloatRect.zero(),
            FloatRect.zero(),
            FloatRect.zero()
        ));
        assertSame(ScrollableOverflow.ZERO, tree.getLayout(root).scrollableOverflow(),
            "the hand-built layout carries the placeholder");

        tree.roundLayout(root);

        Layout rounded = tree.getLayout(root);
        assertNotSame(ScrollableOverflow.ZERO, rounded.scrollableOverflow(),
            "the rounded layout carries a computed overflow, not the placeholder");
        assertEquals(0f, rounded.scrollWidth(), 0f,
            "scroll width uses the computed (empty) overflow instead of the legacy contentSize formula");
        assertEquals(20f, rounded.contentSize().width, 0f, "content size is carried through");

        // Now that the stored layout carries a computed overflow and the values are whole pixels,
        // the next pass is a plain no-op.
        tree.roundLayout(root);
        assertSame(rounded, tree.getLayout(root), "the next pass keeps the rebuilt layout");
    }

    @Test
    @DisplayName("a mixed tree keeps whole-pixel nodes and re-rounds fractional ones")
    void mixedTreeKeepsOnlyWholePixelNodes() {
        TaffyTree tree = new TaffyTree();
        NodeId whole = tree.newLeaf(new TaffyStyle());
        NodeId fractional = tree.newLeaf(new TaffyStyle());
        NodeId root = tree.newWithChildren(new TaffyStyle(), whole, fractional);
        tree.setUnroundedLayout(root, integerLayout(0f, 0f, 100f, 100f));
        tree.setUnroundedLayout(whole, integerLayout(0f, 0f, 10f, 10f));
        tree.setUnroundedLayout(fractional, integerLayout(0.25f, 0.75f, 5.5f, 2.25f));

        tree.roundLayout(root);
        Layout rootFirst = tree.getLayout(root);
        Layout wholeFirst = tree.getLayout(whole);
        Layout fractionalFirst = tree.getLayout(fractional);
        assertEquals(6f, fractionalFirst.size().width, 0f, "fractional width rounds");
        assertEquals(2f, fractionalFirst.size().height, 0f, "fractional height rounds");

        tree.roundLayout(root);
        assertSame(rootFirst, tree.getLayout(root), "whole-pixel root is kept");
        assertSame(wholeFirst, tree.getLayout(whole), "whole-pixel child is kept");
        assertEquals(fractionalFirst, tree.getLayout(fractional), "fractional child re-rounds to the same values");
    }

    @Test
    @DisplayName("a second computeLayout keeps the rounded layouts of whole-pixel geometry")
    void secondComputeLayoutKeepsRoundedLayouts() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle leafStyle = new TaffyStyle();
        leafStyle.size = new TaffySize<>(TaffyDimension.length(10f), TaffyDimension.length(10f));
        NodeId first = tree.newLeaf(leafStyle);
        NodeId second = tree.newLeaf(leafStyle);
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.flexDirection = FlexDirection.COLUMN;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, first, second);

        TaffySize<AvailableSpace> availableSpace = TaffySize.maxContent();
        tree.computeLayout(root, availableSpace);
        Layout rootFirst = tree.getLayout(root);
        Layout firstChildFirst = tree.getLayout(first);
        assertEquals(20f, rootFirst.size().height, 0f, "root stacks the two 10px leaves");

        tree.computeLayout(root, availableSpace);

        assertSame(rootFirst, tree.getLayout(root), "unchanged geometry keeps the rounded root layout");
        assertSame(firstChildFirst, tree.getLayout(first), "unchanged geometry keeps the rounded child layout");
        assertEquals(20f, tree.getLayout(root).size().height, 0f, "values are unchanged");
    }

    @Test
    @DisplayName("a no-op pass allocates far less than the rebuild pass")
    void noOpPassAllocatesLessThanTheRebuildPass() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        Assumptions.assumeTrue(bean instanceof com.sun.management.ThreadMXBean,
            "thread allocation counters need the com.sun.management.ThreadMXBean");
        com.sun.management.ThreadMXBean counters = (com.sun.management.ThreadMXBean) bean;
        Assumptions.assumeTrue(counters.isThreadAllocatedMemorySupported(),
            "thread allocation counters are not supported on this JVM");
        if (!counters.isThreadAllocatedMemoryEnabled()) {
            counters.setThreadAllocatedMemoryEnabled(true);
        }
        Assumptions.assumeTrue(counters.isThreadAllocatedMemoryEnabled(),
            "thread allocation counters are disabled on this JVM");

        // Warm the pass so JIT bookkeeping does not land inside the measured calls.
        IntegerTree warmup = integerTree(200);
        warmup.tree.roundLayout(warmup.root);
        warmup.tree.roundLayout(warmup.root);

        IntegerTree noOpFixture = integerTree(200);
        noOpFixture.tree.roundLayout(noOpFixture.root);
        long noOpBytes = allocatedBytes(() -> noOpFixture.tree.roundLayout(noOpFixture.root));

        IntegerTree rebuildFixture = integerTree(200);
        long rebuildBytes = allocatedBytes(() -> rebuildFixture.tree.roundLayout(rebuildFixture.root));

        // The measured pass still allocates its traversal arrays (a few KB); the rebuild pass adds a
        // Layout plus its geometry for every node. A loose factor keeps this a diagnostic rather
        // than a hard gate on exact byte counts.
        assertTrue(noOpBytes * 3 < rebuildBytes,
            "no-op pass allocated " + noOpBytes + " bytes, rebuild pass allocated " + rebuildBytes + " bytes");
    }

    private static long allocatedBytes(Runnable action) {
        com.sun.management.ThreadMXBean counters =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long threadId = Thread.currentThread().getId();
        long before = counters.getThreadAllocatedBytes(threadId);
        action.run();
        long after = counters.getThreadAllocatedBytes(threadId);
        return after - before;
    }
}
