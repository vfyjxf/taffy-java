package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * P13 tests for the pass-scoped block item-list cache in {@link BlockComputer}.
 *
 * <p>{@code generateItemList} used to rebuild every {@link BlockComputer.BlockItem} for every
 * compute call of a block container. One layout pass computes the same container several times
 * (a flex base-size measurement, a min-content measurement, the final layout) with the same child
 * styles, so the pass now caches the list on a thread-local keyed by the tree and its direction
 * epoch and reuses it while the inner size stays the same (or when no child style resolves against
 * that size at all).</p>
 *
 * <p>These tests pin the observable contract: percentage padding/size/min/max must still resolve
 * against the inner size of the pass that uses them, a style or child change between passes must
 * be observed, and the list must never leak the per-pass layout results (deferred child layout,
 * out-of-flow static position, collapse-through) from an earlier call.</p>
 */
@DisplayName("P13: pass-scoped block item-list reuse")
public class BlockItemReuseP13Test {

    private static final float EPSILON = 0.001f;

    private static TaffySize<AvailableSpace> layoutSize(float width) {
        return TaffySize.of(AvailableSpace.definite(width), AvailableSpace.maxContent());
    }

    private static TaffyStyle flexColumn(float width) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.COLUMN;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.AUTO);
        return style;
    }

    /** A block container with one pixel of padding on every side. */
    private static TaffyStyle paddedBlock() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        style.padding = TaffyRect.all(LengthPercentage.length(1f));
        return style;
    }

    private static TaffyStyle sizedLeaf(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    /**
     * A block container used as a flex item is measured at several inner sizes in one pass (flex
     * base size, min-content, final layout). A percentage child width must resolve against the
     * inner size of the pass that lays the child out (298 = 300 - 2px padding), not against an
     * earlier measurement basis.
     */
    @Test
    void percentageChildWidthResolvesAgainstTheFinalInnerSize() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.percent(0.5f), TaffyDimension.length(10f));
        NodeId child = tree.newLeaf(childStyle);
        NodeId block = tree.newWithChildren(paddedBlock(), new NodeId[]{child});
        NodeId root = tree.newWithChildren(flexColumn(300f), new NodeId[]{block});

        tree.computeLayout(root, layoutSize(300f));

        assertEquals(300f, tree.getLayout(block).size().width, EPSILON);
        assertEquals(149f, tree.getLayout(child).size().width, EPSILON);
        assertEquals(10f, tree.getLayout(child).size().height, EPSILON);
    }

    /** The same property through the min/max constraint path. */
    @Test
    void percentageChildMaxWidthResolvesAgainstTheFinalInnerSize() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(1000f), TaffyDimension.length(10f));
        childStyle.maxSize = new TaffySize<>(TaffyDimension.percent(0.5f), TaffyDimension.AUTO);
        NodeId child = tree.newLeaf(childStyle);
        NodeId block = tree.newWithChildren(paddedBlock(), new NodeId[]{child});
        NodeId root = tree.newWithChildren(flexColumn(300f), new NodeId[]{block});

        tree.computeLayout(root, layoutSize(300f));

        assertEquals(149f, tree.getLayout(child).size().width, EPSILON);
    }

    /** A percentage padding resolves against the container's content-box width. */
    @Test
    void percentageChildPaddingResolvesAgainstTheFinalInnerSize() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(50f), TaffyDimension.AUTO);
        childStyle.padding = TaffyRect.all(LengthPercentage.percent(0.1f));
        NodeId child = tree.newLeaf(childStyle);
        NodeId block = tree.newWithChildren(paddedBlock(), new NodeId[]{child});
        NodeId root = tree.newWithChildren(flexColumn(300f), new NodeId[]{block});

        tree.computeLayout(root, layoutSize(300f));

        // 10% of the block's 298px content box on every side, around a zero-height content box.
        assertEquals(29.8f * 2f, tree.getLayout(child).size().height, EPSILON);
    }

    /** Replacing a style between passes must be observed by the next pass. */
    @Test
    void styleChangeBetweenPassesIsObserved() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        NodeId child = tree.newLeaf(percentageLeaf(0.5f, 10f));
        NodeId block = tree.newWithChildren(paddedBlock(), new NodeId[]{child});
        NodeId root = tree.newWithChildren(flexColumn(300f), new NodeId[]{block});

        tree.computeLayout(root, layoutSize(300f));
        assertEquals(149f, tree.getLayout(child).size().width, EPSILON);

        tree.setStyle(child, percentageLeaf(0.25f, 10f));
        tree.computeLayout(root, layoutSize(300f));
        assertEquals(74.5f, tree.getLayout(child).size().width, EPSILON);

        // And back again, so the previously cached basis cannot sneak back in either.
        tree.setStyle(child, percentageLeaf(0.5f, 10f));
        tree.computeLayout(root, layoutSize(300f));
        assertEquals(149f, tree.getLayout(child).size().width, EPSILON);
    }

    /** A style mutated in place and marked dirty must be observed by the next pass. */
    @Test
    void inPlaceStyleMutationWithMarkDirtyIsObserved() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        NodeId child = tree.newLeaf(percentageLeaf(0.5f, 10f));
        NodeId block = tree.newWithChildren(paddedBlock(), new NodeId[]{child});
        NodeId root = tree.newWithChildren(flexColumn(300f), new NodeId[]{block});

        tree.computeLayout(root, layoutSize(300f));
        assertEquals(149f, tree.getLayout(child).size().width, EPSILON);

        tree.getStyle(child).size.width = TaffyDimension.length(60f);
        tree.markDirty(child);
        tree.computeLayout(root, layoutSize(300f));
        assertEquals(60f, tree.getLayout(child).size().width, EPSILON);
    }

    /** Adding and removing a child between passes must be observed by the next pass. */
    @Test
    void childListChangeBetweenPassesIsObserved() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        NodeId first = tree.newLeaf(sizedLeaf(100f, 10f));
        NodeId block = tree.newWithChildren(paddedBlock(), new NodeId[]{first});
        NodeId root = tree.newWithChildren(flexColumn(300f), new NodeId[]{block});

        tree.computeLayout(root, layoutSize(300f));
        assertEquals(12f, tree.getLayout(block).size().height, EPSILON);

        NodeId second = tree.newLeaf(sizedLeaf(100f, 10f));
        tree.addChild(block, second);
        tree.computeLayout(root, layoutSize(300f));
        assertEquals(22f, tree.getLayout(block).size().height, EPSILON);
        assertEquals(11f, tree.getLayout(second).location().y, EPSILON);

        tree.removeChild(block, second);
        tree.computeLayout(root, layoutSize(300f));
        assertEquals(12f, tree.getLayout(block).size().height, EPSILON);
    }

    /** Re-running the same layout must be idempotent: no per-pass state may leak into the next. */
    @Test
    void repeatedLayoutsAreIdentical() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        NodeId[] leaves = new NodeId[4];
        for (int i = 0; i < leaves.length; i++) {
            leaves[i] = tree.newLeaf(sizedLeaf(50f, 10f + i));
        }
        NodeId block = tree.newWithChildren(paddedBlock(), leaves);
        NodeId root = tree.newWithChildren(flexColumn(300f), new NodeId[]{block});

        tree.computeLayout(root, layoutSize(300f));
        String reference = dump(tree, new NodeId[]{root, block, leaves[0], leaves[1], leaves[2], leaves[3]});
        for (int i = 0; i < 3; i++) {
            tree.computeLayout(root, layoutSize(300f));
            assertEquals(reference, dump(tree, new NodeId[]{root, block, leaves[0], leaves[1], leaves[2], leaves[3]}));
        }
    }

    /**
     * An out-of-flow child keeps the static position of the pass that is currently running, even
     * though the container's item list is reused between its measurement and its final layout.
     */
    @Test
    void outOfFlowChildKeepsItsCurrentStaticPosition() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle absolute = new TaffyStyle();
        absolute.position = TaffyPosition.ABSOLUTE;
        absolute.size = new TaffySize<>(TaffyDimension.length(10f), TaffyDimension.length(10f));
        NodeId abspos = tree.newLeaf(absolute);

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(50f), TaffyDimension.length(10f));
        NodeId inFlow = tree.newLeaf(childStyle);
        NodeId block = tree.newWithChildren(paddedBlock(), new NodeId[]{abspos, inFlow});
        NodeId root = tree.newWithChildren(flexColumn(300f), new NodeId[]{block});

        tree.computeLayout(root, layoutSize(300f));

        // All insets are auto: the box sits at the container's content-box origin.
        assertEquals(1f, tree.getLayout(abspos).location().x, EPSILON);
        assertEquals(1f, tree.getLayout(abspos).location().y, EPSILON);
    }

    /**
     * A layout triggered from a measure callback (a re-entrant pass on another tree) must not make
     * the interrupted pass reuse a list built against that other tree's styles.
     */
    @Test
    void reentrantLayoutOnAnotherTreeDoesNotCorruptTheOuterPass() {
        TaffyTree other = new TaffyTree();
        other.disableRounding();
        NodeId otherLeaf = other.newLeaf(percentageLeaf(0.5f, 10f));
        NodeId otherBlock = other.newWithChildren(paddedBlock(), new NodeId[]{otherLeaf});
        NodeId otherRoot = other.newWithChildren(flexColumn(300f), new NodeId[]{otherBlock});
        other.computeLayout(otherRoot, layoutSize(300f));

        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle measuredStyle = new TaffyStyle();
        measuredStyle.size = new TaffySize<>(TaffyDimension.percent(0.5f), TaffyDimension.length(10f));
        NodeId measured = tree.newLeaf(measuredStyle);
        boolean[] laidOut = new boolean[1];
        tree.setMeasureFunc(measured, (knowndims, availableSpace) -> {
            if (!laidOut[0]) {
                laidOut[0] = true;
                other.computeLayout(otherRoot, layoutSize(300f));
            }
            return new FloatSize(149f, 10f);
        });
        NodeId block = tree.newWithChildren(paddedBlock(), new NodeId[]{measured});
        NodeId root = tree.newWithChildren(flexColumn(300f), new NodeId[]{block});

        tree.computeLayout(root, layoutSize(300f));

        assertEquals(149f, tree.getLayout(measured).size().width, EPSILON);
        assertEquals(10f, tree.getLayout(measured).size().height, EPSILON);
        assertEquals(12f, tree.getLayout(block).size().height, EPSILON);
    }

    /**
     * A zero-padding item shares the internal zero rectangle with every other zero-padding item;
     * the reported padding of a padded sibling (and of the shared one) must stay intact across
     * passes, i.e. nothing may write through the shared geometry.
     */
    @Test
    void sharedZeroGeometryIsNotWrittenThrough() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle paddedStyle = sizedLeaf(50f, 10f);
        paddedStyle.padding = TaffyRect.all(LengthPercentage.length(5f));
        paddedStyle.border = TaffyRect.all(LengthPercentage.length(2f));
        NodeId padded = tree.newLeaf(paddedStyle);
        NodeId plain = tree.newLeaf(sizedLeaf(50f, 10f));
        NodeId block = tree.newWithChildren(paddedBlock(), new NodeId[]{padded, plain});
        NodeId root = tree.newWithChildren(flexColumn(300f), new NodeId[]{block});

        for (int pass = 0; pass < 3; pass++) {
            tree.computeLayout(root, layoutSize(300f));
            assertEquals(5f, tree.getLayout(padded).padding().left, EPSILON);
            assertEquals(2f, tree.getLayout(padded).border().left, EPSILON);
            assertEquals(0f, tree.getLayout(plain).padding().left, EPSILON);
            assertEquals(0f, tree.getLayout(plain).border().left, EPSILON);
        }
    }

    /** A measured leaf with an auto size is laid out from its measure callback. */
    @Test
    void measuredLeafIsNotAffectedByTheItemListCache() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        NodeId measured = tree.newLeaf(new TaffyStyle());
        tree.setMeasureFunc(measured, (knownDimensions, availableSpace) -> new FloatSize(149f, 10f));
        NodeId block = tree.newWithChildren(paddedBlock(), new NodeId[]{measured});
        NodeId root = tree.newWithChildren(flexColumn(300f), new NodeId[]{block});

        tree.computeLayout(root, layoutSize(300f));

        // An auto-width block-level child is stretched to the 298px content box; the measured
        // height comes from the callback.
        assertEquals(298f, tree.getLayout(measured).size().width, EPSILON);
        assertEquals(10f, tree.getLayout(measured).size().height, EPSILON);
    }

    private static TaffyStyle percentageLeaf(float widthPercent, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.percent(widthPercent), TaffyDimension.length(height));
        return style;
    }

    private static String dump(TaffyTree tree, NodeId[] nodes) {
        List<String> lines = new ArrayList<>();
        for (NodeId node : nodes) {
            Layout layout = tree.getLayout(node);
            assertNotNull(layout);
            lines.add(node.getId() + ":" + layout.location().x + "," + layout.location().y
                      + " " + layout.size().width + "x" + layout.size().height);
        }
        return String.join(";", lines);
    }
}
