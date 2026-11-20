package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Aspect-ratio sizing rules pinned by the WPT {@code css/css-sizing/aspect-ratio} fixes.
 *
 * <p>These are hand-written unit tests for the sizing behaviour that the WPT family exercises:
 * the css-sizing-4 transfer of a clamped size between axes, the automatic minimum size of
 * aspect-ratio boxes, the block- and grid-axis stretch interaction, the flex base size taken from
 * a cross size pinned by a min-size, and the zero/non-finite ratio guard. They are kept separate
 * from the generated WPT tests, which cover the full page geometry.</p>
 */
public class AspectRatioSizingTest {

    private static final float EPSILON = 0.01f;

    private static TaffyStyle block() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        return style;
    }

    private static TaffyStyle sized(float width, float height) {
        TaffyStyle style = block();
        style.size = new TaffySize<>(
            Float.isNaN(width) ? TaffyDimension.AUTO : TaffyDimension.length(width),
            Float.isNaN(height) ? TaffyDimension.AUTO : TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle root() {
        TaffyStyle style = block();
        style.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        return style;
    }

    private static Layout layout(TaffyTree tree, NodeId node) {
        return tree.getLayout(node);
    }

    @Test
    @DisplayName("block: an inline size transferred from the block size is floored by the min-content size")
    void blockInlineAutomaticMinimum() {
        // height: 100px; aspect-ratio: 1/2 with a 100px-wide child -> the transferred 50px inline
        // size is floored by the 100px min-content contribution (WPT block-aspect-ratio-015).
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle item = sized(Float.NaN, 100f);
        item.aspectRatio = 0.5f;
        NodeId child = tree.newLeaf(sized(100f, Float.NaN));
        NodeId node = tree.newWithChildren(item, child);
        NodeId rootNode = tree.newWithChildren(root(), node);

        tree.computeLayout(rootNode, TaffySize.maxContent());

        Layout l = layout(tree, node);
        assertEquals(100f, l.size().width, EPSILON, "width");
        assertEquals(100f, l.size().height, EPSILON, "height");
    }

    @Test
    @DisplayName("block: a block size transferred from the inline size is floored by the content height")
    void blockBlockAutomaticMinimum() {
        // width: 100px; aspect-ratio: 2/1 with 100px of content -> the transferred 50px height is
        // floored by the 100px content height (WPT block-aspect-ratio-009).
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle item = sized(100f, Float.NaN);
        item.aspectRatio = 2f;
        NodeId child = tree.newLeaf(sized(Float.NaN, 100f));
        NodeId node = tree.newWithChildren(item, child);
        NodeId rootNode = tree.newWithChildren(root(), node);

        tree.computeLayout(rootNode, TaffySize.maxContent());

        Layout l = layout(tree, node);
        assertEquals(100f, l.size().width, EPSILON, "width");
        assertEquals(100f, l.size().height, EPSILON, "height");
    }

    @Test
    @DisplayName("block: the transfer uses the clamped size (max-width caps the height)")
    void blockTransferFromClampedSize() {
        // width: 300px; max-width: 100px; aspect-ratio: 2/1 -> 100x50, not 100x150
        // (WPT block-aspect-ratio-033).
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle item = sized(300f, Float.NaN);
        item.maxSize = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.AUTO);
        item.aspectRatio = 2f;
        NodeId node = tree.newLeaf(item);
        NodeId rootNode = tree.newWithChildren(root(), node);

        tree.computeLayout(rootNode, TaffySize.maxContent());

        Layout l = layout(tree, node);
        assertEquals(100f, l.size().width, EPSILON, "width");
        assertEquals(50f, l.size().height, EPSILON, "height");
    }

    @Test
    @DisplayName("block: cross-axis max constraints are not transferred")
    void blockMaxSizeDoesNotTransfer() {
        // height: 100px; max-width: 100px; aspect-ratio: 2/1 with 300px of content: the height
        // stays the specified 100px, a transferred max-height of 50px must not cap it
        // (WPT block-aspect-ratio-038).
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle item = sized(Float.NaN, 100f);
        item.maxSize = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.AUTO);
        item.aspectRatio = 2f;
        NodeId child = tree.newLeaf(sized(200f, Float.NaN));
        NodeId node = tree.newWithChildren(item, child);
        NodeId rootNode = tree.newWithChildren(root(), node);

        tree.computeLayout(rootNode, TaffySize.maxContent());

        Layout l = layout(tree, node);
        assertEquals(100f, l.size().width, EPSILON, "width");
        assertEquals(100f, l.size().height, EPSILON, "height");
    }

    @Test
    @DisplayName("block: a zero aspect ratio does not collapse the inline size")
    void blockZeroAspectRatioIsIgnored() {
        // aspect-ratio: 0/1 with height: 100px in a 100px-wide container: the zero ratio carries
        // no transfer, so the block still fills its containing block (WPT zero-or-infinity-002).
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle container = block();
        container.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(100f));
        TaffyStyle item = sized(Float.NaN, 100f);
        item.aspectRatio = 0f;
        NodeId node = tree.newLeaf(item);
        NodeId containerNode = tree.newWithChildren(container, node);
        NodeId rootNode = tree.newWithChildren(root(), containerNode);

        tree.computeLayout(rootNode, TaffySize.maxContent());

        Layout l = layout(tree, node);
        assertEquals(100f, l.size().width, EPSILON, "width");
        assertEquals(100f, l.size().height, EPSILON, "height");
    }

    @Test
    @DisplayName("sizing keyword: min-content on a block resolves through the child's ratio transfer")
    void blockMinContentKeyword() {
        // width: min-content; height: 100px with a height:100px; aspect-ratio:1 child -> the
        // min-content size is the child's transferred 100px inline size
        // (WPT intrinsic-size-001).
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle item = sized(Float.NaN, 100f);
        item.size = new TaffySize<>(TaffyDimension.minContent(), TaffyDimension.length(100f));
        TaffyStyle childStyle = sized(Float.NaN, 100f);
        childStyle.aspectRatio = 1f;
        NodeId child = tree.newLeaf(childStyle);
        NodeId node = tree.newWithChildren(item, child);
        NodeId rootNode = tree.newWithChildren(root(), node);

        tree.computeLayout(rootNode, TaffySize.maxContent());

        assertEquals(100f, layout(tree, node).size().width, EPSILON, "width");
    }

    @Test
    @DisplayName("grid: an explicit stretch wins over the aspect ratio in its axis")
    void gridExplicitStretchOverridesRatio() {
        // 100px x 100px grid area, item aspect-ratio: 2/1 stretched in both axes -> 100x100
        // (WPT grid-aspect-ratio-032).
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle grid = new TaffyStyle();
        grid.display = TaffyDisplay.GRID;
        grid.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(100f));
        grid.gridTemplateRows = java.util.List.of(
            dev.vfyjxf.taffy.style.TrackSizingFunction.fixed(100f));
        grid.gridTemplateColumns = java.util.List.of(
            dev.vfyjxf.taffy.style.TrackSizingFunction.fixed(100f));
        TaffyStyle item = block();
        item.aspectRatio = 2f;
        item.alignSelf = AlignItems.STRETCH;
        item.justifySelf = AlignItems.STRETCH;
        NodeId node = tree.newLeaf(item);
        NodeId gridNode = tree.newWithChildren(grid, node);
        NodeId rootNode = tree.newWithChildren(root(), gridNode);

        tree.computeLayout(rootNode, TaffySize.maxContent());

        Layout l = layout(tree, node);
        assertEquals(100f, l.size().width, EPSILON, "width");
        assertEquals(100f, l.size().height, EPSILON, "height");
    }

    @Test
    @DisplayName("grid: a stretched block size transfers through the ratio to the inline size")
    void gridTransferredInlineSizeFromStretchedBlock() {
        // 200px-wide / 100px-tall grid area, item aspect-ratio: 1/1 with align-self: stretch:
        // the block size stretches to 100px and the auto inline size transfers to 100px
        // (WPT grid-aspect-ratio-028).
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle grid = new TaffyStyle();
        grid.display = TaffyDisplay.GRID;
        grid.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(100f));
        grid.gridTemplateRows = java.util.List.of(
            dev.vfyjxf.taffy.style.TrackSizingFunction.fixed(100f));
        grid.gridTemplateColumns = java.util.List.of(
            dev.vfyjxf.taffy.style.TrackSizingFunction.fixed(200f));
        TaffyStyle item = block();
        item.aspectRatio = 1f;
        item.alignSelf = AlignItems.STRETCH;
        NodeId node = tree.newLeaf(item);
        NodeId gridNode = tree.newWithChildren(grid, node);
        NodeId rootNode = tree.newWithChildren(root(), gridNode);

        tree.computeLayout(rootNode, TaffySize.maxContent());

        Layout l = layout(tree, node);
        assertEquals(100f, l.size().width, EPSILON, "width");
        assertEquals(100f, l.size().height, EPSILON, "height");
    }

    @Test
    @DisplayName("flex: a cross size pinned by min-width gives the base size through the ratio")
    void flexBaseSizeFromPinnedCrossSize() {
        // column flex item with only min-width: 100px and aspect-ratio: 1/1: the cross size is
        // 100px, so the flex base size (height) is 100px (WPT flex-aspect-ratio-034).
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle flex = new TaffyStyle();
        flex.display = TaffyDisplay.FLEX;
        flex.flexDirection = FlexDirection.COLUMN;
        flex.alignItems = AlignItems.FLEX_START;
        flex.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(100f));
        TaffyStyle item = block();
        item.aspectRatio = 1f;
        item.minSize = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(0f));
        NodeId node = tree.newLeaf(item);
        NodeId flexNode = tree.newWithChildren(flex, node);
        NodeId rootNode = tree.newWithChildren(root(), flexNode);

        tree.computeLayout(rootNode, TaffySize.maxContent());

        Layout l = layout(tree, node);
        assertEquals(100f, l.size().width, EPSILON, "width");
        assertEquals(100f, l.size().height, EPSILON, "height");
    }

}
