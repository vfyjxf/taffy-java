package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.LayoutComputer;
import dev.vfyjxf.taffy.tree.LayoutOutput;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.SizingMode;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P3 regression tests for block-axis {@code align-content} (BK-14 / upstream #959, `dcdb0ede`).
 *
 * <p>The portable expectations mirror the upstream generated fixtures
 * {@code block_align_content_start/end/center/space_between/space_around/space_evenly/}
 * {@code stretch_single_child/with_abs_child} ({@code tests/xml/block/*__border_box_ltr.xml}).
 * The block-axis in-flow stack is a single alignment subject, so distribution keywords use their
 * single-subject fallbacks and the group is translated by one offset while the container's
 * content size, abs static positions and first baseline follow the shifted content.</p>
 *
 * <p>Not covered, with the reason:</p>
 * <ul>
 *   <li>{@code block_align_content_safe_end_overflow}: the safe-end case is now covered by
 *       {@code BlockP5Test} through {@link AlignContent#SAFE_END} (ST-02, PK-14 helper
 *       wiring); this file keeps the unsafe end/center overflow cases.</li>
 *   <li>{@code block_align_content_nested_independent}: its expectation ({@code y=25} under the
 *       inner 60px container) depends on upstream #1087/#1251 making a non-normal
 *       {@code align-content} block establish an independent formatting context (BK-18, P7).
 *       Java has no independent-FC concept yet, so the child margin collapses through the
 *       inner block and the portable result is {@code y=20}.</li>
 * </ul>
 */
public class BlockP3AlignTest {

    private static final float EPS = 0.01f;

    /** Baseline reported by the measured leaf (its top border-box edge is 0). */
    private static final float BASELINE = 12f;

    // === Helpers ===

    private static TaffyStyle block() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        return style;
    }

    private static TaffyStyle block(float width, float height) {
        TaffyStyle style = block();
        style.size = new TaffySize<>(
            Float.isNaN(width) ? TaffyDimension.AUTO : TaffyDimension.length(width),
            Float.isNaN(height) ? TaffyDimension.AUTO : TaffyDimension.length(height)
        );
        return style;
    }

    private static TaffyStyle inFlow(float width, float height) {
        return block(width, height);
    }

    private static TaffyStyle absolute(float width, float height) {
        TaffyStyle style = block(width, height);
        style.position = TaffyPosition.ABSOLUTE;
        return style;
    }

    private static NodeId container(TaffyTree tree, AlignContent alignContent,
                                    float width, float height, NodeId... children) {
        TaffyStyle style = block(width, height);
        style.alignContent = alignContent;
        style.position = TaffyPosition.RELATIVE;
        return tree.newWithChildren(style, children);
    }

    private static Layout layout(TaffyTree tree, NodeId node) {
        Layout layout = tree.getLayout(node);
        assertNotNull(layout, "node has no layout");
        return layout;
    }

    private static void assertLocation(TaffyTree tree, NodeId node, float x, float y, String message) {
        Layout layout = layout(tree, node);
        assertEquals(x, layout.location().x, EPS, message + " x");
        assertEquals(y, layout.location().y, EPS, message + " y");
    }

    /** Used after a direct {@link LayoutComputer} pass, which writes unrounded layouts only. */
    private static void assertUnroundedLocation(TaffyTree tree, NodeId node,
                                                float x, float y, String message) {
        Layout layout = tree.getUnroundedLayout(node);
        assertNotNull(layout, "node has no unrounded layout");
        assertEquals(x, layout.location().x, EPS, message + " x");
        assertEquals(y, layout.location().y, EPS, message + " y");
    }

    private static void assertSize(TaffyTree tree, NodeId node, float width, float height, String message) {
        Layout layout = layout(tree, node);
        assertEquals(width, layout.size().width, EPS, message + " width");
        assertEquals(height, layout.size().height, EPS, message + " height");
    }

    /** The upstream {@code three 50x20 children in a 100x120 block} fixture shape. */
    private static NodeId threeChildContainer(TaffyTree tree, AlignContent alignContent) {
        NodeId first = tree.newLeaf(inFlow(50f, 20f));
        NodeId second = tree.newLeaf(inFlow(50f, 20f));
        NodeId third = tree.newLeaf(inFlow(50f, 20f));
        return container(tree, alignContent, 100f, 120f, first, second, third);
    }

    private static void assertThreeChildStack(AlignContent alignContent,
                                              float y0, float y1, float y2) {
        TaffyTree tree = new TaffyTree();
        NodeId root = threeChildContainer(tree, alignContent);
        tree.computeLayout(root, TaffySize.maxContent());

        List<NodeId> children = tree.getChildren(root);
        assertEquals(3, children.size(), "fixture child count");
        assertLocation(tree, children.get(0), 0f, y0, alignContent + " child 0");
        assertLocation(tree, children.get(1), 0f, y1, alignContent + " child 1");
        assertLocation(tree, children.get(2), 0f, y2, alignContent + " child 2");
    }

    private static LayoutOutput layoutOutput(TaffyTree tree, NodeId node) {
        LayoutComputer computer = new LayoutComputer(tree, (MeasureFunc) null);
        return computer.performChildLayout(
            node,
            FloatSize.none(),
            FloatSize.none(),
            TaffySize.maxContent(),
            SizingMode.INHERENT_SIZE,
            TaffyLine.FALSE
        );
    }

    private static NodeId measuredLeaf(TaffyTree tree, float width, float height, float baseline) {
        return tree.newLeafWithMeasureFunction(block(), context -> LayoutOutput.fromSizesAndBaselines(
            new FloatSize(width, height),
            FloatSize.zero(),
            new FloatPoint(Float.NaN, baseline)
        ));
    }

    // === Upstream fixtures: positional keywords ===

    @Test
    @DisplayName("BK-14: align-content start packs the in-flow stack at the content-box start")
    void startPacksAtStart() {
        // block_align_content_start__border_box_ltr
        assertThreeChildStack(AlignContent.START, 0f, 20f, 40f);
        assertThreeChildStack(AlignContent.FLEX_START, 0f, 20f, 40f);
    }

    @Test
    @DisplayName("BK-14: align-content end packs the in-flow stack at the content-box end")
    void endPacksAtEnd() {
        // block_align_content_end__border_box_ltr; the block axis has no flex direction, so
        // flex-end is end.
        assertThreeChildStack(AlignContent.END, 60f, 80f, 100f);
        assertThreeChildStack(AlignContent.FLEX_END, 60f, 80f, 100f);

        TaffyTree tree = new TaffyTree();
        NodeId root = threeChildContainer(tree, AlignContent.END);
        tree.computeLayout(root, TaffySize.maxContent());

        // The content size is recomputed from the committed (shifted) child layouts, so the
        // shifted stack still reports its real bottom edge.
        assertEquals(120f, layout(tree, root).contentSize().height, EPS,
            "the container's content size follows the shifted content");
    }

    @Test
    @DisplayName("BK-14: align-content center centres the in-flow stack")
    void centerCentres() {
        // block_align_content_center__border_box_ltr
        assertThreeChildStack(AlignContent.CENTER, 30f, 50f, 70f);
    }

    // === Upstream fixtures: distribution keywords use the single-subject fallback ===

    @Test
    @DisplayName("BK-14: align-content space-between falls back to start for the single block subject")
    void spaceBetweenFallsBackToStart() {
        // block_align_content_space_between__border_box_ltr: 60px of free space is not
        // distributed between children (the whole stack is one subject).
        assertThreeChildStack(AlignContent.SPACE_BETWEEN, 0f, 20f, 40f);
    }

    @Test
    @DisplayName("BK-14: align-content space-around falls back to center for the single block subject")
    void spaceAroundFallsBackToCenter() {
        // block_align_content_space_around__border_box_ltr
        assertThreeChildStack(AlignContent.SPACE_AROUND, 30f, 50f, 70f);
    }

    @Test
    @DisplayName("BK-14: align-content space-evenly falls back to center for the single block subject")
    void spaceEvenlyFallsBackToCenter() {
        // block_align_content_space_evenly__border_box_ltr
        assertThreeChildStack(AlignContent.SPACE_EVENLY, 30f, 50f, 70f);
    }

    @Test
    @DisplayName("BK-14: align-content stretch falls back to start and does not stretch children")
    void stretchFallsBackToStart() {
        // block_align_content_stretch_single_child__border_box_ltr
        TaffyTree tree = new TaffyTree();
        NodeId only = tree.newLeaf(inFlow(50f, 20f));
        NodeId root = container(tree, AlignContent.STRETCH, 100f, 120f, only);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, only, 0f, 0f, "stretch single child");
        assertSize(tree, only, 50f, 20f, "stretch does not resize the child");

        // With several children the fallback is still a group translation, not per-child stretch.
        assertThreeChildStack(AlignContent.STRETCH, 0f, 20f, 40f);
    }

    @Test
    @DisplayName("BK-14: align-content AUTO is a no-op (the default block behaviour is unchanged)")
    void autoIsANoOp() {
        assertThreeChildStack(AlignContent.AUTO, 0f, 20f, 40f);
    }

    // === Absolutely positioned children ===

    @Test
    @DisplayName("BK-14: an abs child with explicit insets ignores the align-content shift")
    void absChildWithInsetsIgnoresShift() {
        // block_align_content_with_abs_child__border_box_ltr
        TaffyTree tree = new TaffyTree();
        NodeId inFlowChild = tree.newLeaf(inFlow(50f, 20f));

        TaffyStyle absStyle = absolute(30f, 30f);
        absStyle.inset = new TaffyRect<>(
            LengthPercentageAuto.length(10f),
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.length(5f),
            LengthPercentageAuto.AUTO
        );
        NodeId absChild = tree.newLeaf(absStyle);

        NodeId root = container(tree, AlignContent.END, 100f, 120f, inFlowChild, absChild);
        tree.computeLayout(root, TaffySize.maxContent());

        assertLocation(tree, inFlowChild, 0f, 100f, "in-flow child is shifted to the end");
        assertLocation(tree, absChild, 10f, 5f, "explicit insets win over the static position");
    }

    @Test
    @DisplayName("BK-14: an abs child without insets follows the shifted static position")
    void absChildWithoutInsetsFollowsStaticPosition() {
        TaffyTree tree = new TaffyTree();
        NodeId inFlowChild = tree.newLeaf(inFlow(50f, 20f));
        NodeId absChild = tree.newLeaf(absolute(30f, 30f));
        NodeId root = container(tree, AlignContent.END, 100f, 120f, inFlowChild, absChild);
        tree.computeLayout(root, TaffySize.maxContent());

        // The static position of the abs child is where it would sit in flow — after the 20px
        // in-flow sibling, i.e. y=20 — plus the group shift (100 for end alignment); the abs
        // child's own auto insets then resolve to it.
        assertLocation(tree, absChild, 0f, 120f, "abs static position moves with align-content");
    }

    // === Empty / only-abs containers, overflow and insets ===

    @Test
    @DisplayName("BK-14: an empty block container is unaffected by align-content")
    void emptyContainerIsUnaffected() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = block(100f, 120f);
        style.alignContent = AlignContent.CENTER;
        NodeId root = tree.newLeaf(style);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree, root, 100f, 120f, "empty container keeps its size");
        assertEquals(0f, layout(tree, root).contentSize().height, EPS,
            "an empty container has no content");
    }

    @Test
    @DisplayName("BK-14: an only-abs container still shifts the static position by the alignment offset")
    void onlyAbsContainerShiftsStaticPosition() {
        TaffyTree tree = new TaffyTree();
        NodeId absChild = tree.newLeaf(absolute(30f, 30f));
        NodeId root = container(tree, AlignContent.CENTER, 100f, 120f, absChild);

        tree.computeLayout(root, TaffySize.maxContent());

        // No in-flow content contributes a baseline, but the abs static position is offset by the
        // free space (120 - 0) / 2.
        assertLocation(tree, absChild, 0f, 60f, "only-abs container static position");
    }

    @Test
    @DisplayName("BK-14: end alignment of overflowing content uses a negative offset")
    void endOverflowUsesNegativeOffset() {
        TaffyTree tree = new TaffyTree();
        NodeId first = tree.newLeaf(inFlow(50f, 80f));
        NodeId second = tree.newLeaf(inFlow(50f, 80f));
        NodeId root = container(tree, AlignContent.END, 100f, 100f, first, second);
        tree.computeLayout(root, TaffySize.maxContent());

        // free space = 100 - 160 = -60; end is unsafe, so the stack overflows above the top edge.
        assertLocation(tree, first, 0f, -60f, "first overflowing child");
        assertLocation(tree, second, 0f, 20f, "second overflowing child");
    }

    @Test
    @DisplayName("BK-14: center alignment of overflowing content centres the negative free space")
    void centerOverflowUsesNegativeOffset() {
        TaffyTree tree = new TaffyTree();
        NodeId first = tree.newLeaf(inFlow(50f, 80f));
        NodeId second = tree.newLeaf(inFlow(50f, 80f));
        NodeId root = container(tree, AlignContent.CENTER, 100f, 100f, first, second);
        tree.computeLayout(root, TaffySize.maxContent());

        // free space = -60; centered -> -30.
        assertLocation(tree, first, 0f, -30f, "first overflowing child");
        assertLocation(tree, second, 0f, 50f, "second overflowing child");
    }

    @Test
    @DisplayName("BK-14: free space is measured against the content box, not the border box")
    void freeSpaceUsesTheContentBox() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(inFlow(50f, 20f));

        TaffyStyle style = block(100f, 120f);
        style.alignContent = AlignContent.CENTER;
        style.padding = new TaffyRect<>(
            LengthPercentage.ZERO,
            LengthPercentage.ZERO,
            LengthPercentage.length(10f),
            LengthPercentage.length(10f)
        );
        NodeId root = tree.newWithChildren(style, child);

        tree.computeLayout(root, TaffySize.maxContent());

        // Inner height 100, content 20 -> free space 80, centre offset 40, plus the top padding.
        assertLocation(tree, child, 0f, 50f, "padded container centre");
    }

    @Test
    @DisplayName("BK-14: a min-height-derived height provides the free space for align-content")
    void minHeightProvidesFreeSpace() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(inFlow(50f, 20f));

        TaffyStyle style = block(100f, Float.NaN);
        style.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(120f));
        style.alignContent = AlignContent.CENTER;
        NodeId root = tree.newWithChildren(style, child);

        tree.computeLayout(root, TaffySize.maxContent());

        assertSize(tree, root, 100f, 120f, "min-height sizes the auto-height container");
        assertLocation(tree, child, 0f, 50f, "content is centred in the min-height box");
    }

    // === Baseline and content-size sync (CR-07 #996) ===

    @Test
    @DisplayName("BK-14/CR-07: the container's first baseline is shifted with the aligned content")
    void firstBaselineIsShifted() {
        TaffyTree tree = new TaffyTree();
        NodeId leaf = measuredLeaf(tree, 20f, 20f, BASELINE);

        TaffyStyle style = block(100f, 120f);
        style.alignContent = AlignContent.END;
        NodeId root = tree.newWithChildren(style, leaf);

        LayoutOutput output = layoutOutput(tree, root);

        // The only in-flow child sits at y=100 (end alignment); its baseline is 12 below its top.
        assertUnroundedLocation(tree, leaf, 0f, 100f, "measured child position");
        assertTrue(output.baselines() != null && output.baselines().hasFirst(),
            "the container reports a first baseline");
        assertEquals(112f, output.baselines().first().y, EPS, "baselines.first.y");
        assertEquals(112f, output.firstBaselines().y, EPS, "legacy firstBaselines.y");
    }

    @Test
    @DisplayName("BK-14/CR-07: an empty in-flow content produces no baseline even with align-content")
    void emptyContentHasNoBaseline() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = block(100f, 120f);
        style.alignContent = AlignContent.CENTER;
        NodeId root = tree.newLeaf(style);

        LayoutOutput output = layoutOutput(tree, root);

        assertTrue(Float.isNaN(output.firstBaselines().y), "no in-flow baseline to propagate");
    }
}
