package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Pins the package-private scratch-copy used by the flex overflow accounting (p34).
 *
 * <p>The public defensive copy in {@link ScrollableOverflow#rect()} must keep its contract; the
 * scratch variant may only overwrite the caller-owned rectangle and must never expose or mutate
 * the internal one.</p>
 */
class ScrollableOverflowScratchCopyTest {

    @Test
    void scratchCopyOverwritesOnlyTheTarget() {
        ScrollableOverflow overflow = new ScrollableOverflow(new FloatRect(1f, 2f, 3f, 4f), true, false);
        FloatRect target = new FloatRect(-1f, -1f, -1f, -1f);
        overflow.copyRectInto(target);
        assertEquals(1f, target.left);
        assertEquals(2f, target.right);
        assertEquals(3f, target.top);
        assertEquals(4f, target.bottom);
        // The flags and the internal rectangle are untouched.
        assertEquals(true, overflow.reversedX());
        assertEquals(false, overflow.reversedY());
        assertEquals(1f, overflow.left());
        assertEquals(2f, overflow.right());
        assertEquals(3f, overflow.top());
        assertEquals(4f, overflow.bottom());
    }

    @Test
    void mutatingTheScratchDoesNotChangeTheOverflowOrItsDefensiveCopy() {
        ScrollableOverflow overflow = new ScrollableOverflow(new FloatRect(1f, 2f, 3f, 4f), false, true);
        FloatRect target = new FloatRect(0f, 0f, 0f, 0f);
        overflow.copyRectInto(target);
        target.left = 99f;
        assertEquals(1f, overflow.left());
        FloatRect defensive = overflow.rect();
        assertNotSame(target, defensive);
        assertEquals(1f, defensive.left);
        defensive.left = 123f;
        assertEquals(1f, overflow.left());
    }

    @Test
    void computedEmptyValueIsDistinctFromTheCompatibilityPlaceholder() {
        // ZERO means "no overflow data" in Layout; the interned computed-empty value must be a
        // different instance that happens to compare equal, so algorithms can share it safely.
        assertNotSame(ScrollableOverflow.ZERO, ScrollableOverflow.EMPTY_COMPUTED);
        assertEquals(ScrollableOverflow.ZERO, ScrollableOverflow.EMPTY_COMPUTED);
        assertEquals(0f, ScrollableOverflow.EMPTY_COMPUTED.left());
        assertEquals(0f, ScrollableOverflow.EMPTY_COMPUTED.right());
        assertEquals(0f, ScrollableOverflow.EMPTY_COMPUTED.top());
        assertEquals(0f, ScrollableOverflow.EMPTY_COMPUTED.bottom());
    }

    @Test
    void fixedLeafInstallsAComputedEmptyOverflowNotThePlaceholder() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle containerStyle = new TaffyStyle();
        containerStyle.display = TaffyDisplay.FLEX;
        TaffyStyle leafStyle = new TaffyStyle();
        leafStyle.size = new TaffySize<>(TaffyDimension.length(10f), TaffyDimension.length(10f));
        NodeId leaf = tree.newLeaf(leafStyle);
        NodeId root = tree.newWithChildren(containerStyle, List.of(leaf));
        tree.computeLayout(root, TaffySize.of(
            dev.vfyjxf.taffy.style.AvailableSpace.definite(100f),
            dev.vfyjxf.taffy.style.AvailableSpace.maxContent()));

        Layout leafLayout = tree.getUnroundedLayout(leaf);
        assertNotSame(ScrollableOverflow.ZERO, leafLayout.scrollableOverflow(),
            "an algorithm-produced empty rectangle must not be the 1.x placeholder");
        assertEquals(ScrollableOverflow.EMPTY_COMPUTED, leafLayout.scrollableOverflow());
        assertEquals(0f, leafLayout.scrollWidth());
        assertEquals(0f, leafLayout.scrollHeight());
    }

    @Test
    void zeroOverflowCopiesZeros() {
        ScrollableOverflow overflow = ScrollableOverflow.zero();
        FloatRect target = new FloatRect(5f, 6f, 7f, 8f);
        overflow.copyRectInto(target);
        assertEquals(0f, target.left);
        assertEquals(0f, target.right);
        assertEquals(0f, target.top);
        assertEquals(0f, target.bottom);
        assertEquals(0f, ScrollableOverflow.ZERO.left());
    }
}
