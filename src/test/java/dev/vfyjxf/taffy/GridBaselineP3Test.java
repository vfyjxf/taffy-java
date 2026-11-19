package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.GridAutoFlow;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P3 regression tests for the grid baseline section (CR-07-GRID / diff-grid-v2 A3-A6).
 *
 * <p>Mirrors the upstream fixtures (taffy @1abd8eea, LTR border box) added by the commits this
 * change ports:</p>
 * <ul>
 *   <li><b>A3</b> {@code e85c605b} (#1125): an item with an auto block-axis margin does not
 *       participate in baseline alignment, so it must not be measured and must not raise or take
 *       part in the row's baseline; the container baseline selection also skips it
 *       ({@code grid_align_items_baseline_child_auto_margin},
 *       {@code grid_baseline_child_auto_margin_container_baseline}).</li>
 * </ul>
 */
public class GridBaselineP3Test {

    private static final float EPS = 0.01f;

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static TaffyDimension dim(float value) {
        return Float.isNaN(value) ? TaffyDimension.AUTO : TaffyDimension.length(value);
    }

    /** A grid container with an optional definite size (NaN = auto). */
    private static TaffyStyle grid(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        style.size = new TaffySize<>(dim(width), dim(height));
        return style;
    }

    /** A block box with a definite size, used for plain (baseline-less) content. */
    private static TaffyStyle block(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        style.size = new TaffySize<>(dim(width), dim(height));
        return style;
    }

    /** A grid item with a definite block size and an explicit {@code align-self}. */
    private static TaffyStyle gridItem(float height, AlignItems alignSelf) {
        TaffyStyle style = grid(100f, height);
        style.alignSelf = alignSelf;
        return style;
    }

    /** A row flex container with {@code align-items: baseline}, the harness of the gridflex fixtures. */
    private static TaffyStyle flexBaseline(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.alignItems = AlignItems.BASELINE;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static List<TrackSizingFunction> fixedTracks(float... sizes) {
        List<TrackSizingFunction> tracks = new java.util.ArrayList<>();
        for (float size : sizes) {
            tracks.add(TrackSizingFunction.fixed(size));
        }
        return tracks;
    }

    private static void assertNode(TaffyTree tree, NodeId node, float x, float y, float width, float height,
                                   String label) {
        Layout layout = tree.getLayout(node);
        assertEquals(x, layout.location().x, EPS, label + " (x)");
        assertEquals(y, layout.location().y, EPS, label + " (y)");
        assertEquals(width, layout.size().width, EPS, label + " (width)");
        assertEquals(height, layout.size().height, EPS, label + " (height)");
    }

    // ==================================================================
    // A3 / #1125: baseline participation excludes auto block-axis margins
    // ==================================================================

    @Test
    @DisplayName("A3: a non-participant in the row does not raise the row baseline")
    void a3NonParticipantDoesNotRaiseRowBaseline() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = grid(300f, Float.NaN);
        rootStyle.alignItems = AlignItems.BASELINE;
        rootStyle.gridTemplateColumns = fixedTracks(100f, 100f, 100f);

        NodeId itemA = tree.newLeaf(gridItem(20f, AlignItems.BASELINE));
        NodeId itemB = tree.newLeaf(gridItem(30f, AlignItems.BASELINE));
        // Not baseline-aligned: measured by the pre-A3 code for the whole row, which made its 100px
        // height the row's max baseline and shimmed the participants down.
        NodeId itemC = tree.newLeaf(gridItem(100f, AlignItems.START));

        NodeId root = tree.newWithChildren(rootStyle, itemA, itemB, itemC);
        tree.computeLayout(root, TaffySize.maxContent());

        // Upstream only measures items participating in baseline alignment, so the row's max
        // baseline is max(20, 30) = 30.
        assertNode(tree, root, 0f, 0f, 300f, 100f, "root");
        assertNode(tree, itemA, 0f, 10f, 100f, 20f, "item A (shim 10)");
        assertNode(tree, itemB, 100f, 0f, 100f, 30f, "item B (shim 0)");
        assertNode(tree, itemC, 200f, 0f, 100f, 100f, "item C (not baselined)");
    }

    @Test
    @DisplayName("A3: an item with an auto block-axis margin does not participate in baseline alignment")
    void a3AutoBlockMarginItemDoesNotParticipate() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = grid(300f, Float.NaN);
        rootStyle.alignItems = AlignItems.BASELINE;
        rootStyle.gridTemplateColumns = fixedTracks(100f, 100f, 100f);

        NodeId itemA = tree.newLeaf(gridItem(20f, AlignItems.BASELINE));

        TaffyStyle itemBStyle = gridItem(30f, AlignItems.BASELINE);
        itemBStyle.margin = TaffyRect.all(LengthPercentageAuto.AUTO);
        NodeId itemB = tree.newLeaf(itemBStyle);

        NodeId itemC = tree.newLeaf(gridItem(25f, AlignItems.BASELINE));

        NodeId root = tree.newWithChildren(rootStyle, itemA, itemB, itemC);
        tree.computeLayout(root, TaffySize.maxContent());

        // css-align-3 9.5: the auto-margin item is excluded, so the row's max baseline is
        // max(20, 25) = 25 and B is positioned by its auto margin (no free space in the 30px row).
        assertNode(tree, root, 0f, 0f, 300f, 30f, "root");
        assertNode(tree, itemA, 0f, 5f, 100f, 20f, "item A (shim 5)");
        assertNode(tree, itemB, 100f, 0f, 100f, 30f, "item B (auto margin, not baselined)");
        assertNode(tree, itemC, 200f, 0f, 100f, 25f, "item C (shim 0)");
    }

    @Test
    @DisplayName("A3: the container baseline skips auto-margin items (upstream grid_baseline_child_auto_margin_container_baseline)")
    void a3ContainerBaselineSkipsAutoMarginItem() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = grid(120f, 80f);
        rootStyle.alignItems = AlignItems.BASELINE;
        rootStyle.gridTemplateColumns = fixedTracks(60f, 60f);

        TaffyStyle itemAStyle = grid(50f, 30f);
        NodeId itemA = tree.newLeaf(itemAStyle);

        // Inner grid: auto-flow column, 25px columns, 40px rows.
        TaffyStyle itemBStyle = grid(50f, Float.NaN);
        itemBStyle.gridAutoFlow = GridAutoFlow.COLUMN;
        itemBStyle.gridAutoRows = fixedTracks(40f);
        itemBStyle.gridAutoColumns = fixedTracks(25f);

        NodeId content0 = tree.newLeaf(grid(25f, 20f));
        TaffyStyle content1Style = grid(25f, 10f);
        content1Style.alignSelf = AlignItems.BASELINE;
        content1Style.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO
        );
        NodeId content1 = tree.newLeaf(content1Style);

        NodeId itemB = tree.newWithChildren(itemBStyle, content0, content1);
        NodeId root = tree.newWithChildren(rootStyle, itemA, itemB);
        tree.computeLayout(root, TaffySize.maxContent());

        // The inner grid's baseline comes from its first item (height 20), not from the
        // auto-margin item, so B's border-box baseline is 20 and A (baseline 30) shims B by 10.
        assertNode(tree, itemA, 0f, 0f, 50f, 30f, "item A");
        assertNode(tree, itemB, 60f, 10f, 50f, 40f, "item B (shim 10)");
        assertNode(tree, content0, 0f, 0f, 25f, 20f, "content 0");
        assertNode(tree, content1, 25f, 30f, 25f, 10f, "content 1 (auto margin)");
    }










}
