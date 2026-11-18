package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PA-03: the root node of an RTL tree is placed against the right edge of the available space:
 * {@code x = availableWidth - size.width - margin.right} (upstream taffy {@code compute/mod.rs} root RTL
 * branch). Child locations stay parent-relative, so this only moves the root.
 *
 * <p>Scope note: the rest of the upstream root placement (margin box at the origin, i.e.
 * {@code x = margin.left} / {@code y = margin.top} for LTR, plus relative inset offsets) belongs to
 * ST-06/P6 and is not implemented here. Non-RTL roots and RTL roots without a definite available width
 * therefore keep the historical origin {@code (0, 0)}.
 */
@DisplayName("RTL root location (PA-03)")
class RtlRootLocationTest {

    private static final float EPSILON = 0.01f;

    /**
     * A 300x100 flex row root with explicit direction, plus one 50x50 child.
     */
    private static TaffyTree newRootWithChild(TaffyDirection direction, TaffyStyle[] outRootStyle, NodeId[] outRoot,
                                              NodeId[] outChild) {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        // Always set the direction explicitly: RTL behaviour must not depend on INHERIT resolution.
        rootStyle.direction = direction;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(100f));

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(50f), TaffyDimension.length(50f));

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child);

        outRootStyle[0] = rootStyle;
        outRoot[0] = root;
        outChild[0] = child;
        return tree;
    }

    @Test
    @DisplayName("RTL root with definite available width sits at the right edge")
    void rtlRootIsAlignedToRightEdgeOfAvailableSpace() {
        TaffyStyle[] rootStyle = new TaffyStyle[1];
        NodeId[] root = new NodeId[1];
        NodeId[] child = new NodeId[1];
        TaffyTree tree = newRootWithChild(TaffyDirection.RTL, rootStyle, root, child);

        tree.computeLayout(root[0], new TaffySize<>(
            AvailableSpace.definite(500f),
            AvailableSpace.definite(400f)
        ));

        Layout rootLayout = tree.getLayout(root[0]);
        assertEquals(300f, rootLayout.size().width, EPSILON, "root width");
        assertEquals(100f, rootLayout.size().height, EPSILON, "root height");
        // x = availableWidth - size.width - margin.right = 500 - 300 - 0
        assertEquals(200f, rootLayout.location().x, EPSILON, "RTL root x should be flush with the right edge");
        assertEquals(0f, rootLayout.location().y, EPSILON, "RTL root y should stay at the origin");

        // Children are still parent-relative: 300 - 50 inside the root, not 500 - 50 in the ICB.
        Layout childLayout = tree.getLayout(child[0]);
        assertEquals(250f, childLayout.location().x, EPSILON, "child x stays parent-relative (root width - child width)");
        assertEquals(0f, childLayout.location().y, EPSILON, "child y stays parent-relative");
    }

    @Test
    @DisplayName("RTL root subtracts its right margin from the right edge")
    void rtlRootAccountsForRightMargin() {
        TaffyStyle[] rootStyle = new TaffyStyle[1];
        NodeId[] root = new NodeId[1];
        NodeId[] child = new NodeId[1];
        TaffyTree tree = newRootWithChild(TaffyDirection.RTL, rootStyle, root, child);
        rootStyle[0].margin = TaffyRect.ltrb(
            LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(20f),
            LengthPercentageAuto.ZERO
        );

        tree.computeLayout(root[0], new TaffySize<>(
            AvailableSpace.definite(500f),
            AvailableSpace.definite(400f)
        ));

        Layout rootLayout = tree.getLayout(root[0]);
        // x = availableWidth - size.width - margin.right = 500 - 300 - 20
        assertEquals(180f, rootLayout.location().x, EPSILON, "RTL root x should account for margin.right");
        assertEquals(0f, rootLayout.location().y, EPSILON, "RTL root y should stay at the origin");
    }

    @Test
    @DisplayName("RTL root with indefinite available width falls back to the origin")
    void rtlRootWithoutDefiniteAvailableWidthStaysAtOrigin() {
        TaffyStyle[] rootStyle = new TaffyStyle[1];
        NodeId[] root = new NodeId[1];
        NodeId[] child = new NodeId[1];
        TaffyTree tree = newRootWithChild(TaffyDirection.RTL, rootStyle, root, child);

        // No definite available width -> nothing to align against; the historical origin is kept
        // (pending ST-06/P6 root placement work).
        tree.computeLayout(root[0], TaffySize.maxContent());

        Layout rootLayout = tree.getLayout(root[0]);
        assertEquals(0f, rootLayout.location().x, EPSILON, "RTL root without available width stays at x=0");
        assertEquals(0f, rootLayout.location().y, EPSILON, "RTL root without available width stays at y=0");
    }

    @Test
    @DisplayName("LTR root keeps the historical origin")
    void ltrRootStaysAtOriginWithDefiniteAvailableSpace() {
        TaffyStyle[] rootStyle = new TaffyStyle[1];
        NodeId[] root = new NodeId[1];
        NodeId[] child = new NodeId[1];
        TaffyTree tree = newRootWithChild(TaffyDirection.LTR, rootStyle, root, child);

        tree.computeLayout(root[0], new TaffySize<>(
            AvailableSpace.definite(500f),
            AvailableSpace.definite(400f)
        ));

        Layout rootLayout = tree.getLayout(root[0]);
        assertEquals(0f, rootLayout.location().x, EPSILON, "LTR root stays at x=0 (upstream margin-box placement is P6)");
        assertEquals(0f, rootLayout.location().y, EPSILON, "LTR root stays at y=0 (upstream margin-box placement is P6)");
    }

    @Test
    @DisplayName("Root without an explicit direction keeps the historical origin")
    void rootWithInheritedDirectionStaysAtOrigin() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        // direction is left at its default INHERIT: a root has no parent, so it resolves to LTR.
        rootStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(100f));
        NodeId root = tree.newLeaf(rootStyle);

        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(500f),
            AvailableSpace.definite(400f)
        ));

        Layout rootLayout = tree.getLayout(root);
        assertEquals(0f, rootLayout.location().x, EPSILON, "INHERIT root resolves to LTR and stays at x=0");
        assertEquals(0f, rootLayout.location().y, EPSILON, "INHERIT root resolves to LTR and stays at y=0");
    }

    @Test
    @DisplayName("RTL root wider than the available space follows the upstream formula (no clamp)")
    void rtlRootWiderThanAvailableSpaceFollowsUpstreamFormula() {
        TaffyStyle[] rootStyle = new TaffyStyle[1];
        NodeId[] root = new NodeId[1];
        NodeId[] child = new NodeId[1];
        TaffyTree tree = newRootWithChild(TaffyDirection.RTL, rootStyle, root, child);
        rootStyle[0].size = new TaffySize<>(TaffyDimension.length(600f), TaffyDimension.length(100f));

        tree.computeLayout(root[0], new TaffySize<>(
            AvailableSpace.definite(500f),
            AvailableSpace.definite(400f)
        ));

        Layout rootLayout = tree.getLayout(root[0]);
        // Upstream does not clamp the root position: 500 - 600 = -100.
        assertEquals(-100f, rootLayout.location().x, EPSILON, "RTL root wider than the ICB follows the formula unclamped");
    }
}
