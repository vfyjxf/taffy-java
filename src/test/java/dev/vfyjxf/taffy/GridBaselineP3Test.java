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
 *   <li><b>A4</b> {@code 3dbf2e46}: an item whose block size depends on the size of an intrinsic or
 *       flexible row (percentage height) does not participate, avoiding the cyclic dependency
 *       between the baseline shim and the row size. Upstream has no generated fixture for this.</li>
 *   <li><b>A5</b> {@code e77a7a8d} (#1322): {@code item.baseline} stores the border-box baseline,
 *       the top margin is only added when items are aligned with each other, and the container
 *       baseline is taken from the first row's item's final layout
 *       ({@code gridflex_container_baseline_from_baseline_item_with_margin_top},
 *       {@code gridflex_container_baseline_from_non_baseline_item_{start,center,end}}).</li>
 * </ul>
 *
 * <p>Expectations are the values Chrome reported in the upstream XML fixtures
 * ({@code tests/xml/grid/...}, {@code tests/xml/gridflex/...}):
 * {@code ref y=11/21/31} for the non-baseline container-baseline cases, {@code ref y=30} for the
 * top-margin case, {@code ref y=10} for the relative-offset case.</p>
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

    // ==================================================================
    // A4 / 3dbf2e46: cyclic block-size dependency excludes participation
    // ==================================================================

    @Test
    @DisplayName("A4: a percentage block size in an intrinsic row does not participate in baseline alignment")
    void a4CyclicPercentHeightItemDoesNotParticipate() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = grid(200f, 100f);
        rootStyle.alignItems = AlignItems.BASELINE;
        rootStyle.gridTemplateColumns = fixedTracks(100f, 100f);

        // The item's block size depends on the (intrinsic) row it is being baseline-aligned in.
        TaffyStyle itemAStyle = grid(100f, Float.NaN);
        itemAStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.percent(0.5f));
        itemAStyle.alignSelf = AlignItems.BASELINE;
        NodeId itemA = tree.newLeaf(itemAStyle);

        NodeId itemB = tree.newLeaf(gridItem(20f, AlignItems.BASELINE));

        NodeId root = tree.newWithChildren(rootStyle, itemA, itemB);
        tree.computeLayout(root, TaffySize.maxContent());

        // A is excluded (percentage height + intrinsic row), leaving a single participant, so no
        // baseline alignment happens and B stays at the row start instead of being shimmed by 30.
        assertNode(tree, root, 0f, 0f, 200f, 100f, "root");
        assertNode(tree, itemA, 0f, 0f, 100f, 50f, "item A (percentage height, not baselined)");
        assertNode(tree, itemB, 100f, 0f, 100f, 20f, "item B (single participant, no shim)");
    }

    @Test
    @DisplayName("A4: a percentage block size in a fixed row still participates")
    void a4PercentHeightItemParticipatesInFixedRow() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = grid(200f, 100f);
        rootStyle.alignItems = AlignItems.BASELINE;
        rootStyle.gridTemplateColumns = fixedTracks(100f, 100f);
        rootStyle.gridTemplateRows = fixedTracks(100f);

        TaffyStyle itemAStyle = grid(100f, Float.NaN);
        itemAStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.percent(0.5f));
        itemAStyle.alignSelf = AlignItems.BASELINE;
        NodeId itemA = tree.newLeaf(itemAStyle);

        NodeId itemB = tree.newLeaf(gridItem(20f, AlignItems.BASELINE));

        NodeId root = tree.newWithChildren(rootStyle, itemA, itemB);
        tree.computeLayout(root, TaffySize.maxContent());

        // No intrinsic/flexible row is crossed, so the percentage item participates: its baseline
        // is 50 and B is shimmed by 50 - 20 = 30.
        assertNode(tree, root, 0f, 0f, 200f, 100f, "root");
        assertNode(tree, itemA, 0f, 0f, 100f, 50f, "item A (percentage height, baselined)");
        assertNode(tree, itemB, 100f, 30f, 100f, 20f, "item B (shim 30)");
    }

    // ==================================================================
    // A5 / #1322: container baseline from the first item's final layout,
    // item.baseline stores the border-box baseline
    // ==================================================================

    @Test
    @DisplayName("A5: a top margin on the baseline item is counted once (gridflex_container_baseline_from_baseline_item_with_margin_top)")
    void a5ContainerBaselineCountsTopMarginOnce() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle gridStyle = grid(Float.NaN, Float.NaN);
        gridStyle.alignItems = AlignItems.BASELINE;
        gridStyle.gridTemplateColumns = fixedTracks(50f, 50f);

        TaffyStyle item1Style = grid(50f, 30f);
        item1Style.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.length(10f), LengthPercentageAuto.ZERO
        );
        NodeId item1Content = tree.newLeaf(block(20f, 20f));
        NodeId item1 = tree.newWithChildren(item1Style, item1Content);

        NodeId item2 = tree.newLeaf(block(50f, 40f));

        NodeId gridNode = tree.newWithChildren(gridStyle, item1, item2);
        NodeId reference = tree.newLeaf(block(10f, 10f));
        NodeId root = tree.newWithChildren(flexBaseline(200f, 100f), gridNode, reference);

        tree.computeLayout(root, TaffySize.maxContent());

        // The row is 50px (item 1's 30 + 10 margin + 10 shim). The grid baseline is item 1's final
        // y (20) plus its border-box baseline (20) = 40, so the 10px reference item sits at 30.
        assertNode(tree, gridNode, 0f, 0f, 100f, 50f, "grid");
        assertNode(tree, item1, 0f, 20f, 50f, 30f, "baseline item with top margin");
        assertNode(tree, item2, 50f, 0f, 50f, 40f, "second item");
        assertNode(tree, reference, 100f, 30f, 10f, 10f, "reference item");
    }

    @Test
    @DisplayName("A5: container baseline from a non-baseline item's final layout (start)")
    void a5ContainerBaselineFromNonBaselineItemStart() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle gridStyle = grid(100f, 50f);
        gridStyle.alignItems = AlignItems.START;

        TaffyStyle itemStyle = grid(26f, 30f);
        NodeId itemContent = tree.newLeaf(block(26f, 21f));
        NodeId item = tree.newWithChildren(itemStyle, itemContent);

        NodeId gridNode = tree.newWithChildren(gridStyle, item);
        NodeId reference = tree.newLeaf(block(10f, 10f));
        NodeId root = tree.newWithChildren(flexBaseline(120f, 100f), gridNode, reference);

        tree.computeLayout(root, TaffySize.maxContent());

        // The grid's baseline is its first item's border-box baseline (0 + 21) = 21, so the 10px
        // reference item sits at 11 (the pre-fix code used the item's bottom edge, giving 20).
        assertNode(tree, item, 0f, 0f, 26f, 30f, "grid item");
        assertNode(tree, reference, 100f, 11f, 10f, 10f, "reference item");
    }

    @Test
    @DisplayName("A5: container baseline from a non-baseline item's final layout (center)")
    void a5ContainerBaselineFromNonBaselineItemCenter() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle gridStyle = grid(100f, 50f);
        gridStyle.alignItems = AlignItems.CENTER;

        TaffyStyle itemStyle = grid(26f, 30f);
        NodeId itemContent = tree.newLeaf(block(26f, 21f));
        NodeId item = tree.newWithChildren(itemStyle, itemContent);

        NodeId gridNode = tree.newWithChildren(gridStyle, item);
        NodeId reference = tree.newLeaf(block(10f, 10f));
        NodeId root = tree.newWithChildren(flexBaseline(120f, 100f), gridNode, reference);

        tree.computeLayout(root, TaffySize.maxContent());

        // Item at (50 - 30) / 2 = 10, baseline 10 + 21 = 31, reference at 21.
        assertNode(tree, item, 0f, 10f, 26f, 30f, "grid item");
        assertNode(tree, reference, 100f, 21f, 10f, 10f, "reference item");
    }

    @Test
    @DisplayName("A5: container baseline from a non-baseline item's final layout (end)")
    void a5ContainerBaselineFromNonBaselineItemEnd() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle gridStyle = grid(100f, 50f);
        gridStyle.alignItems = AlignItems.END;

        TaffyStyle itemStyle = grid(26f, 30f);
        NodeId itemContent = tree.newLeaf(block(26f, 21f));
        NodeId item = tree.newWithChildren(itemStyle, itemContent);

        NodeId gridNode = tree.newWithChildren(gridStyle, item);
        NodeId reference = tree.newLeaf(block(10f, 10f));
        NodeId root = tree.newWithChildren(flexBaseline(120f, 100f), gridNode, reference);

        tree.computeLayout(root, TaffySize.maxContent());

        // Item at 50 - 30 = 20, baseline 20 + 21 = 41, reference at 31.
        assertNode(tree, item, 0f, 20f, 26f, 30f, "grid item");
        assertNode(tree, reference, 100f, 31f, 10f, 10f, "reference item");
    }


}
