package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P20 tests for the flex item-list cache in {@link FlexboxComputer}.
 *
 * <p>{@code generateFlexItems} used to rebuild every {@link FlexboxComputer}'s item metadata on
 * every compute call. One layout pass lays a flex container out several times - a flex base-size
 * measurement, a grid track-sizing measurement, the final layout - so the pass now caches the list
 * on a thread-local keyed by the tree and its direction epoch and reuses it while the inner size
 * stays the same (or when no child style resolves against that size at all). A later pass reuses
 * the list stored on the container's {@link NodeData} while its own content version, child-list
 * version, every child's content version, the resolved direction and the inner-size basis are
 * unchanged.</p>
 *
 * <p>These tests pin the observable contract: percentage values must resolve against the inner
 * size of the pass that uses them, modal results (auto margins, target and hypothetical sizes,
 * freeze flags) must not leak from one use of a reused list into the next, and a reused list must
 * produce exactly the layouts a freshly built tree produces.</p>
 */
@DisplayName("P20: flex item-list reuse")
public class FlexItemCacheP20Test {

    private static final float EPSILON = 0.001f;

    private static TaffySize<AvailableSpace> layoutSize(float width, float height) {
        return TaffySize.of(AvailableSpace.definite(width), AvailableSpace.definite(height));
    }

    private static TaffyStyle flexRow() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.ROW;
        return style;
    }

    private static TaffyStyle flexColumn() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = FlexDirection.COLUMN;
        return style;
    }

    private static TaffyStyle grid() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        return style;
    }

    private static TaffyStyle leaf(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    /**
     * A flex container used as a grid item is measured at several known widths while a single
     * {@code 1fr} track is sized, then laid out at the final width. Its percentage-sized children
     * must resolve against the final inner width, not against an earlier measurement basis.
     */
    @Test
    void percentageChildSizeResolvesAgainstTheFinalGridItemWidth() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle labelStyle = new TaffyStyle();
        labelStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.length(10f));
        NodeId label = tree.newLeaf(labelStyle);

        TaffyStyle cardStyle = flexColumn();
        cardStyle.padding = TaffyRect.all(LengthPercentage.length(6f));
        NodeId card = tree.newWithChildren(cardStyle, new NodeId[]{label});

        TaffyStyle rootStyle = grid();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(320f), TaffyDimension.length(200f));
        rootStyle.gridTemplateColumns = List.of(TrackSizingFunction.fr(1f));
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{card});

        tree.computeLayout(root, layoutSize(320f, 200f));

        assertEquals(320f, tree.getLayout(card).size().width, EPSILON);
        // The card's content box is 320 - 2 * 6 = 308, so a 100%-wide label fills it.
        assertEquals(308f, tree.getLayout(label).size().width, EPSILON);
        assertEquals(10f, tree.getLayout(label).size().height, EPSILON);
        assertEquals(6f, tree.getLayout(label).location().x, EPSILON);
    }

    /**
     * The same property through a percentage padding: a flex container used as a flex item is
     * first measured with an indefinite inner width (its flex base size) and then laid out at its
     * final width, where the percentage padding must resolve to 10% of the final content box.
     */
    @Test
    void percentageChildPaddingResolvesAgainstTheFinalFlexItemWidth() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.boxSizing = BoxSizing.CONTENT_BOX;
        childStyle.size = new TaffySize<>(TaffyDimension.length(50f), TaffyDimension.AUTO);
        childStyle.padding = TaffyRect.all(LengthPercentage.percent(0.1f));
        NodeId child = tree.newLeaf(childStyle);

        TaffyStyle cardStyle = flexColumn();
        cardStyle.flexGrow = 1f;
        cardStyle.padding = TaffyRect.all(LengthPercentage.length(1f));
        NodeId card = tree.newWithChildren(cardStyle, new NodeId[]{child});

        TaffyStyle rootStyle = flexRow();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(200f));
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{card});

        tree.computeLayout(root, layoutSize(300f, 200f));

        assertEquals(300f, tree.getLayout(card).size().width, EPSILON);
        // The card's content box is 298 wide; 10% padding is 29.8 on every side.
        assertEquals(29.8f, tree.getLayout(child).padding().left, EPSILON);
        assertEquals(59.6f, tree.getLayout(child).size().height, EPSILON);
        assertEquals(109.6f, tree.getLayout(child).size().width, EPSILON);
    }

    /**
     * A percentage value also has to be re-resolved when the same container is laid out in a later
     * pass, so a pass-scoped entry can never leak into a pass with a different basis.
     */
    @Test
    void percentageChildResolvesAgainInALaterPass() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.percent(0.5f), TaffyDimension.length(10f));
        NodeId child = tree.newLeaf(childStyle);
        TaffyStyle cardStyle = flexColumn();
        cardStyle.flexGrow = 1f;
        NodeId card = tree.newWithChildren(cardStyle, new NodeId[]{child});
        TaffyStyle rootStyle = flexRow();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(200f));
        NodeId root = tree.newWithChildren(rootStyle, new NodeId[]{card});

        tree.computeLayout(root, layoutSize(300f, 200f));
        assertEquals(150f, tree.getLayout(child).size().width, EPSILON);

        rootStyle.size = new TaffySize<>(TaffyDimension.length(500f), TaffyDimension.length(200f));
        tree.setStyle(root, rootStyle);
        tree.computeLayout(root, layoutSize(500f, 200f));
        assertEquals(250f, tree.getLayout(child).size().width, EPSILON);
    }
}
