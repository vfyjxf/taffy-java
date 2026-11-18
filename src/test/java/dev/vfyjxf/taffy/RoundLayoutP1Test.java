package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.RoundLayout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P1 regression tests for {@link RoundLayout} (BK-13 / upstream #1189).
 *
 * <p>Node positions are rounded in cumulative (absolute) coordinates and then converted back to
 * a parent-relative offset ({@code round(abs) - round(parent_abs)}). Rounding the relative
 * offset directly (the pre-P1 behaviour) disagrees with the parent's own rounded edges whenever
 * the parent sits at a fractional offset, which produces 1px gaps or overlaps between siblings
 * and between a parent and its children.</p>
 */
public class RoundLayoutP1Test {

    private static Layout layout(float x, float y, float width, float height) {
        return new Layout(
            0,
            new FloatPoint(x, y),
            new FloatSize(width, height),
            new FloatSize(width, height),
            FloatSize.zero(),
            FloatRect.zero(),
            FloatRect.zero(),
            FloatRect.zero()
        );
    }

    /**
     * The grandchild sits at a fractional offset inside a parent that is itself at a fractional
     * offset. Rounding the relative offsets independently yields 1 for both, so the grandchild
     * starts 2px to the right of the origin while its parent's rounded left edge is at 1px: a
     * 1px gap. Cumulative rounding puts the grandchild at 0 relative to its parent, i.e. exactly
     * on the parent's rounded left edge.
     */
    @Test
    @DisplayName("BK-13: positions round in cumulative coordinates, not per-node relative offsets")
    void roundsPositionsInCumulativeCoordinates() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = new TaffyStyle();
        NodeId grandchild = tree.newLeaf(new TaffyStyle());
        NodeId parent = tree.newWithChildren(new TaffyStyle(), grandchild);
        NodeId root = tree.newWithChildren(rootStyle, parent);

        tree.setUnroundedLayout(root, layout(0f, 0f, 10f, 10f));
        tree.setUnroundedLayout(parent, layout(0.5f, 0.5f, 2.5f, 2.5f));
        tree.setUnroundedLayout(grandchild, layout(0.5f, 0.5f, 1f, 1f));

        RoundLayout.roundLayout(tree, root);

        Layout parentLayout = tree.getLayout(parent);
        // round(0.5) - round(0) == 1; size stays round(0.5 + 2.5) - round(0.5) == 3 - 1 == 2
        assertEquals(1f, parentLayout.location().x, 0f, "parent x");
        assertEquals(1f, parentLayout.location().y, 0f, "parent y");
        assertEquals(2f, parentLayout.size().width, 0f, "parent width");

        Layout grandchildLayout = tree.getLayout(grandchild);
        // round(0.5 + 0.5) - round(0.5) == 1 - 1 == 0 (the pre-P1 code returned round(0.5) == 1)
        assertEquals(0f, grandchildLayout.location().x, 0f, "grandchild x");
        assertEquals(0f, grandchildLayout.location().y, 0f, "grandchild y");

        // In absolute coordinates the grandchild starts exactly on its parent's rounded left
        // edge (root sits at x = 0, so the parent's absolute x is its own relative x).
        float absoluteParentX = parentLayout.location().x;
        float absoluteGrandchildX = absoluteParentX + grandchildLayout.location().x;
        assertEquals(absoluteParentX, absoluteGrandchildX, 0f,
            "grandchild must start on the parent's rounded edge");
    }

    /**
     * Upstream fixture {@code rounding_fractial_input_8} (added by #1189): a flex container with
     * fractional padding holding ten 13.9375px boxes. The rounded children must tile exactly
     * (each child starts where the previous one ended) instead of drifting by a pixel.
     */
    @Test
    @DisplayName("BK-13: rounding_fractial_input_8 tiles fractional children without gaps")
    void roundingFractialInput8() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(200f));
        // TaffyRect order is (left, right, top, bottom)
        rootStyle.padding = new TaffyRect<>(
            LengthPercentage.length(20.625f),
            LengthPercentage.length(0f),
            LengthPercentage.length(51.25f),
            LengthPercentage.length(0f)
        );

        TaffyStyle columnStyle = new TaffyStyle();
        columnStyle.flexDirection = FlexDirection.COLUMN;

        NodeId[] children = new NodeId[10];
        for (int i = 0; i < children.length; i++) {
            TaffyStyle childStyle = new TaffyStyle();
            childStyle.size = new TaffySize<>(TaffyDimension.length(13.9375f), TaffyDimension.length(13.9375f));
            children[i] = tree.newLeaf(childStyle);
        }
        NodeId column = tree.newWithChildren(columnStyle, children);
        NodeId root = tree.newWithChildren(rootStyle, column);

        tree.computeLayout(root, TaffySize.maxContent());

        Layout rootLayout = tree.getLayout(root);
        assertEquals(0f, rootLayout.location().x, 0f, "root x");
        assertEquals(0f, rootLayout.location().y, 0f, "root y");
        assertEquals(100f, rootLayout.size().width, 0f, "root width");
        assertEquals(200f, rootLayout.size().height, 0f, "root height");

        Layout columnLayout = tree.getLayout(column);
        assertEquals(21f, columnLayout.location().x, 0f, "column x");
        assertEquals(51f, columnLayout.location().y, 0f, "column y");
        assertEquals(14f, columnLayout.size().width, 0f, "column width");
        assertEquals(149f, columnLayout.size().height, 0f, "column height");

        // Children are positioned relative to the column, so tiling is checked in the column's
        // own coordinate space: each child must start exactly where the previous one ended.
        float previousEnd = 0f;
        for (int i = 0; i < children.length; i++) {
            Layout childLayout = tree.getLayout(children[i]);
            assertEquals(0f, childLayout.location().x, 0f, "child " + i + " x");
            assertEquals(14f * i, childLayout.location().y, 0f, "child " + i + " y");
            assertEquals(14f, childLayout.size().width, 0f, "child " + i + " width");
            assertEquals(14f, childLayout.size().height, 0f, "child " + i + " height");

            assertEquals(previousEnd, childLayout.location().y, 0f,
                "child " + i + " must start where the previous child ended (no 1px gap)");
            previousEnd = childLayout.location().y + childLayout.size().height;
        }
    }
}
