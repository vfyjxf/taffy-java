package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P3 flex correctness fixes (FL-10, FL-12) from the upstream alignment list.
 *
 * <p>Each group mirrors the upstream fixture added with the same fix (see
 * {@code Data/taffy-java/analysis/diff-flex-v3.md} A11/B03 and the corresponding
 * {@code test_fixtures/flex/*.html} + {@code tests/xml/flex/*.xml} expectations).
 * The upstream root/leaf/grid halves of #1234 belong to the cross-domain available-space
 * contract (X2/C17) and are deliberately not covered here.
 */
public class FlexP3Test {

    private static final float EPS = 0.01f;

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static TaffyStyle flexBox() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        return style;
    }

    private static TaffyStyle withSize(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(
            Float.isNaN(width) ? TaffyDimension.AUTO : TaffyDimension.length(width),
            Float.isNaN(height) ? TaffyDimension.AUTO : TaffyDimension.length(height)
        );
        return style;
    }

    private static TaffyRect<LengthPercentageAuto> margins(float left, float right, float top, float bottom) {
        return new TaffyRect<>(
            LengthPercentageAuto.length(left), LengthPercentageAuto.length(right),
            LengthPercentageAuto.length(top), LengthPercentageAuto.length(bottom));
    }

    private static void assertSize(Layout layout, float width, float height, String message) {
        assertEquals(width, layout.size().width, EPS, message + " (width)");
        assertEquals(height, layout.size().height, EPS, message + " (height)");
    }

    private static void assertLocation(Layout layout, float x, float y, String message) {
        assertEquals(x, layout.location().x, EPS, message + " (x)");
        assertEquals(y, layout.location().y, EPS, message + " (y)");
    }

    private static List<NodeId> fixedRow(TaffyTree tree, int count) {
        List<NodeId> children = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            children.add(tree.newLeaf(withSize(20f, 10f)));
        }
        return children;
    }

    // ==================================================================
    // FL-10 (#1234 / 04965fc): the available space passed to a flex item
    // is the item's border-box space, i.e. the item's own margins have
    // already been subtracted by the parent.
    //
    // Upstream fixture: flex_column_align_start_shrink_to_fit_excludes_margin
    // (the nested flex-wrap child is 80px wide because the 20px margin is
    // excluded from the 100px column container's cross available space).
    // ==================================================================

    @Test
    @DisplayName("FL-10: column cross available space excludes the item's margin")
    void fl10_crossAvailableExcludesItemMargin() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.COLUMN;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.AUTO);

        TaffyStyle innerStyle = flexBox();
        innerStyle.flexWrap = FlexWrap.WRAP;
        innerStyle.alignSelf = AlignItems.FLEX_START;
        innerStyle.margin = margins(20f, 0f, 0f, 0f);
        NodeId inner = tree.newWithChildren(innerStyle, fixedRow(tree, 10));

        NodeId root = tree.newWithChildren(rootStyle, inner);
        tree.computeLayout(root, TaffySize.maxContent());

        // 100 - 20 = 80 available -> four 20px items per line, three lines.
        assertSize(tree.getLayout(root), 100f, 30f, "root size");
        assertSize(tree.getLayout(inner), 80f, 30f, "nested wrap item size");
        assertLocation(tree.getLayout(inner), 20f, 0f, "nested wrap item position");

        List<NodeId> children = tree.getChildren(inner);
        assertLocation(tree.getLayout(children.get(3)), 60f, 0f, "fourth item is on the first line");
        assertLocation(tree.getLayout(children.get(4)), 0f, 10f, "fifth item wraps to the second line");
        assertLocation(tree.getLayout(children.get(9)), 20f, 20f, "last item wraps to the third line");
    }

    @Test
    @DisplayName("FL-10: negative item margin expands the cross available space")
    void fl10_negativeCrossMarginExpandsAvailableSpace() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.COLUMN;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.AUTO);

        TaffyStyle innerStyle = flexBox();
        innerStyle.flexWrap = FlexWrap.WRAP;
        innerStyle.alignSelf = AlignItems.FLEX_START;
        // The parent subtracts the child's margin: 100 - (-50) = 150, floored at zero
        // only after the subtraction.
        innerStyle.margin = margins(-50f, 0f, 0f, 0f);
        NodeId inner = tree.newWithChildren(innerStyle, fixedRow(tree, 10));

        NodeId root = tree.newWithChildren(rootStyle, inner);
        tree.computeLayout(root, TaffySize.maxContent());

        // 150 available -> seven 20px items per line, two lines.
        assertSize(tree.getLayout(root), 100f, 20f, "root size");
        assertSize(tree.getLayout(inner), 150f, 20f, "nested wrap item size");
        assertLocation(tree.getLayout(inner), -50f, 0f, "nested wrap item position");

        List<NodeId> children = tree.getChildren(inner);
        assertLocation(tree.getLayout(children.get(6)), 120f, 0f, "seventh item is on the first line");
        assertLocation(tree.getLayout(children.get(7)), 0f, 10f, "eighth item wraps to the second line");
    }

    @Test
    @DisplayName("FL-10: STRETCH known cross size subtracts the item margin exactly once")
    void fl10_stretchKnownCrossSubtractsMarginOnce() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(100f));

        TaffyStyle innerStyle = flexBox();
        innerStyle.alignSelf = AlignItems.STRETCH;
        innerStyle.margin = margins(0f, 0f, 10f, 10f);
        NodeId leaf = tree.newLeaf(withSize(20f, 20f));
        NodeId inner = tree.newWithChildren(innerStyle, leaf);

        NodeId root = tree.newWithChildren(rootStyle, inner);
        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 100f, 100f, "root size");
        assertSize(tree.getLayout(inner), 20f, 80f, "stretched item height");
        assertLocation(tree.getLayout(inner), 0f, 10f, "stretched item position");
    }

    // ==================================================================
    // FL-12 (#1109 / 3ce6bef): an item participates in baseline alignment
    // only when align-self is baseline AND neither cross-axis margin is
    // auto. Auto-margin items must be excluded from baseline measurement,
    // the line's max baseline, and the container's first baseline.
    //
    // Upstream fixtures: align_baseline_child_auto_margin_cross_start /
    // _cross_end (row 200x100, align-items baseline, three 50px column
    // items of 40/60/50px height, the third with one auto cross margin).
    // ==================================================================

    private static TaffyStyle columnItem(float width, float height) {
        TaffyStyle style = flexBox();
        style.flexDirection = FlexDirection.COLUMN;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    @Test
    @DisplayName("FL-12: auto cross-start margin excludes the item from baseline alignment")
    void fl12_autoCrossStartMarginExcludedFromBaseline() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.alignItems = AlignItems.BASELINE;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(100f));

        NodeId first = tree.newWithChildren(columnItem(50f, 40f), tree.newLeaf(withSize(50f, 20f)));
        NodeId second = tree.newWithChildren(columnItem(50f, 60f), tree.newLeaf(withSize(50f, 30f)));
        TaffyStyle thirdStyle = columnItem(50f, 50f);
        thirdStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.auto(), LengthPercentageAuto.ZERO);
        NodeId third = tree.newWithChildren(thirdStyle, tree.newLeaf(withSize(50f, 45f)));

        NodeId root = tree.newWithChildren(rootStyle, first, second, third);
        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 200f, 100f, "root size");
        // max baseline is the 30px baseline of the second item (the auto-margin item no longer counts).
        assertLocation(tree.getLayout(first), 0f, 10f, "first baseline item");
        assertLocation(tree.getLayout(second), 50f, 0f, "second baseline item");
        // auto margin absorbs the 50px free space in the 100px line.
        assertLocation(tree.getLayout(third), 100f, 50f, "auto cross-start margin item");
    }

    @Test
    @DisplayName("FL-12: auto cross-end margin excludes the item from baseline alignment")
    void fl12_autoCrossEndMarginExcludedFromBaseline() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.alignItems = AlignItems.BASELINE;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(100f));

        NodeId first = tree.newWithChildren(columnItem(50f, 40f), tree.newLeaf(withSize(50f, 20f)));
        NodeId second = tree.newWithChildren(columnItem(50f, 60f), tree.newLeaf(withSize(50f, 30f)));
        TaffyStyle thirdStyle = columnItem(50f, 50f);
        thirdStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO, LengthPercentageAuto.auto());
        NodeId third = tree.newWithChildren(thirdStyle, tree.newLeaf(withSize(50f, 45f)));

        NodeId root = tree.newWithChildren(rootStyle, first, second, third);
        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 200f, 100f, "root size");
        assertLocation(tree.getLayout(first), 0f, 10f, "first baseline item");
        assertLocation(tree.getLayout(second), 50f, 0f, "second baseline item");
        assertLocation(tree.getLayout(third), 100f, 0f, "auto cross-end margin item");
    }

    @Test
    @DisplayName("FL-12: one baseline item plus an auto-margin baseline item is a no-op")
    void fl12_singleParticipantWithAutoMarginItemIsNoOp() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.alignItems = AlignItems.BASELINE;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(100f));

        NodeId first = tree.newWithChildren(columnItem(50f, 40f), tree.newLeaf(withSize(50f, 20f)));
        TaffyStyle secondStyle = columnItem(50f, 50f);
        secondStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
            LengthPercentageAuto.auto(), LengthPercentageAuto.ZERO);
        NodeId second = tree.newWithChildren(secondStyle, tree.newLeaf(withSize(50f, 45f)));

        NodeId root = tree.newWithChildren(rootStyle, first, second);
        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 200f, 100f, "root size");
        // Only one item participates, so baseline alignment is a no-op and the first item stays at the top.
        assertLocation(tree.getLayout(first), 0f, 0f, "single baseline participant");
        assertLocation(tree.getLayout(second), 50f, 50f, "auto cross-start margin item");
    }
}

