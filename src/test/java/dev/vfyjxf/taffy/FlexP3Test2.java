package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.LayoutOutput;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P3 flex baseline correctness fixes for reverse containers (FL-11 / FL-13).
 *
 * <p>Each group mirrors the upstream fixture added with the same fix (see
 * {@code Data/taffy-java/analysis/diff-flex-v3.md} B01/B04 and the corresponding
 * {@code test_fixtures/flex/*.html} + {@code tests/xml/flex/*.xml} expectations, read from
 * the upstream clone at {@code 1abd8eea}):</p>
 *
 * <ul>
 *   <li><b>FL-11 (#987, 832262a7)</b> — {@code align_baseline_wrap_reverse}: in a wrap-reverse
 *       container the baseline-aligned group is anchored to the cross-start edge, which is the
 *       bottom of the line, using {@code line_cross - max_baseline_to_bottom_distance - baseline}.</li>
 *   <li><b>FL-13 (#1127, 8b01c6a0)</b> — {@code flex_container_baseline_column_reverse} and
 *       {@code flex_container_baseline_column_reverse_wrap_reverse}: a column container's first
 *       baseline is generated from the visually startmost item, which for column-reverse is the
 *       last item in flex order.</li>
 * </ul>
 *
 * <p>The upstream leaf-baseline gating (LEAF-BASELINE) is deliberately out of scope: the leaves
 * here have no measure function and synthesize a baseline from their border-box height, as the
 * upstream fixtures do.</p>
 */
public class FlexP3Test2 {

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

    private static TaffyStyle block(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    /** A layout output with an intrinsic size and a first baseline (the upstream BaselineContext). */
    private static LayoutOutput measured(float width, float height, float baseline) {
        return LayoutOutput.fromSizesAndBaselines(
            new FloatSize(width, height),
            FloatSize.zero(),
            new FloatPoint(Float.NaN, baseline)
        );
    }

    /** A leaf whose measure function reports a size and a first baseline. */
    private static NodeId measuredLeaf(TaffyTree tree, float width, float height, float baseline) {
        TaffyStyle style = new TaffyStyle();
        return tree.newLeafWithMeasureFunction(style, context -> measured(width, height, baseline));
    }

    private static void assertSize(Layout layout, float width, float height, String message) {
        assertEquals(width, layout.size().width, EPS, message + " (width)");
        assertEquals(height, layout.size().height, EPS, message + " (height)");
    }

    private static void assertLocation(Layout layout, float x, float y, String message) {
        assertEquals(x, layout.location().x, EPS, message + " (x)");
        assertEquals(y, layout.location().y, EPS, message + " (y)");
    }

    // ==================================================================
    // FL-11 (#987 / 832262a7): in a wrap-reverse container the baseline
    // group is aligned to the cross-start edge, which is the bottom of
    // the line. The offset is
    //   line_cross - max_baseline_to_bottom_distance - baseline
    // instead of (non-reversed) max_baseline - baseline.
    //
    // Upstream fixture: align_baseline_wrap_reverse (100x100 wrap-reverse
    // row, align-items baseline, four 20/40/30/10px-tall items). Two lines
    // of 40px and 30px share the 30px of free cross space equally
    // (stretch -> 55/45), and the lines are placed bottom-up.
    // ==================================================================

    @Test
    @DisplayName("FL-11: wrap-reverse baseline alignment anchors the group to the line bottom")
    void fl11_wrapReverseBaselineAlignmentFormula() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.flexWrap = FlexWrap.WRAP_REVERSE;
        rootStyle.alignItems = AlignItems.BASELINE;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(100f));

        NodeId first = tree.newLeaf(withSize(60f, 20f));
        NodeId second = tree.newLeaf(withSize(40f, 40f));
        NodeId third = tree.newLeaf(withSize(60f, 30f));
        NodeId fourth = tree.newLeaf(withSize(40f, 10f));

        NodeId root = tree.newWithChildren(rootStyle, first, second, third, fourth);
        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 100f, 100f, "root size");
        // First line (55px tall, bottom): 55 - 0 - 20 = 35 / 55 - 0 - 40 = 15.
        assertLocation(tree.getLayout(first), 0f, 80f, "first item (bottom line)");
        assertLocation(tree.getLayout(second), 60f, 60f, "second item (baseline sets max)");
        // Second line (45px tall, above the first): 45 - 0 - 30 = 15 / 45 - 0 - 10 = 35.
        assertLocation(tree.getLayout(third), 0f, 15f, "third item (top line)");
        assertLocation(tree.getLayout(fourth), 60f, 35f, "fourth item (top line)");
    }

    @Test
    @DisplayName("FL-11: max_baseline_to_bottom_distance anchors the group to the line bottom")
    void fl11_wrapReverseBaselineUsesMaxBottomDistance() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.flexWrap = FlexWrap.WRAP_REVERSE;
        rootStyle.alignItems = AlignItems.BASELINE;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(100f));

        // Item A: 40px tall with its first baseline 10px from its top -> 30px from the bottom.
        // A block container propagates its measured child's baseline (CR-07).
        NodeId a = tree.newWithChildren(block(40f, 40f), measuredLeaf(tree, 20f, 20f, 10f));
        // Item B: 20px tall with its first baseline 5px from its top -> 15px from the bottom.
        NodeId b = tree.newWithChildren(block(40f, 20f), measuredLeaf(tree, 10f, 10f, 5f));

        NodeId root = tree.newWithChildren(rootStyle, a, b);
        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 100f, 100f, "root size");
        // The line is stretched to the 100px container. max(40 - 10, 20 - 5) = 30, so the aligned
        // group's baseline sits at 100 - 30 = 70: item A at 60, item B at 65.
        assertLocation(tree.getLayout(a), 0f, 60f, "item with the larger baseline-to-bottom distance");
        assertLocation(tree.getLayout(b), 40f, 65f, "item with the smaller baseline-to-bottom distance");
    }

    // ==================================================================
    // FL-11 container-baseline half: the first baseline of a wrap-reverse
    // row container is generated from the startmost line, which is the
    // last line in flex-line order.
    //
    // Upstream fixture: flex_container_baseline_wrap_reverse_multiline
    // (outer 200x200 baseline row, inner 100x120 wrap-reverse baseline
    // row with align-content flex-start and two lines of 40px/30px).
    // ==================================================================

    @Test
    @DisplayName("FL-11: a wrap-reverse container derives its first baseline from the startmost line")
    void fl11_wrapReverseContainerBaselineUsesStartmostLine() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = flexBox();
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.alignItems = AlignItems.BASELINE;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(200f));

        NodeId reference = tree.newLeaf(withSize(50f, 100f));

        TaffyStyle innerStyle = flexBox();
        innerStyle.flexDirection = FlexDirection.ROW;
        innerStyle.flexWrap = FlexWrap.WRAP_REVERSE;
        innerStyle.alignItems = AlignItems.BASELINE;
        innerStyle.alignContent = AlignContent.FLEX_START;
        innerStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(120f));

        NodeId first = tree.newLeaf(withSize(60f, 20f));
        NodeId second = tree.newLeaf(withSize(40f, 40f));
        NodeId third = tree.newLeaf(withSize(60f, 30f));
        NodeId fourth = tree.newLeaf(withSize(40f, 10f));
        NodeId inner = tree.newWithChildren(innerStyle, first, second, third, fourth);

        NodeId root = tree.newWithChildren(rootStyle, reference, inner);
        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree.getLayout(root), 200f, 200f, "root size");
        assertLocation(tree.getLayout(reference), 0f, 0f, "reference item");
        // The startmost line is line 2 (30px tall) at line offset 50: its first baseline item is
        // placed at 50 + 0 and is 30px tall, so the container baseline is 80 and the container is
        // shifted down by 100 - 80 = 20.
        assertLocation(tree.getLayout(inner), 50f, 20f, "inner wrap-reverse container");
        assertLocation(tree.getLayout(first), 0f, 100f, "line 1 first item");
        assertLocation(tree.getLayout(second), 60f, 80f, "line 1 second item");
        assertLocation(tree.getLayout(third), 0f, 50f, "line 2 first item (startmost)");
        assertLocation(tree.getLayout(fourth), 60f, 70f, "line 2 second item");
    }
}
