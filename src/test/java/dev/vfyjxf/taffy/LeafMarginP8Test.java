package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.GridTemplateComponent;
import dev.vfyjxf.taffy.style.LengthPercentage;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P8/X2-C17 tests for the leaf available-space contract (upstream taffy #1234, {@code 04965fc5}).
 *
 * <p>Upstream's contract, stated in {@code LayoutInput}'s docs and in {@code compute_leaf_layout}:
 * a definite available space is the space available to a node's <em>border box</em>, the parent
 * subtracts the node's margins before passing it, and the node itself (leaf included) never
 * subtracts its own margins again. The root is the only box without a parent, so the root pass
 * subtracts the root's own margins, for every display type.</p>
 *
 * <p>Java used to subtract the leaf's own margins inside {@code LayoutComputer} while the block
 * algorithm already subtracted a child's horizontal margins and the flex algorithm (FL-10) already
 * subtracted a flex item's margins on both axes. The measured available space was therefore
 * deducting those margins twice. This suite pins the single deduction for each of the four calling
 * sources (block, flex, grid and root), the unchanged numbers for margin-free content, and the
 * known residual on the grid intrinsic-contribution path.</p>
 *
 * <p><b>Out of scope:</b> out-of-flow boxes ({@code position: absolute}/{@code fixed}) are laid out
 * by the out-of-flow helpers, which still hand over the positioning area itself as available space.
 * Upstream's {@code layout_oof_box} uses the stretch-fit size (the inset-modified containing block
 * minus the non-auto margins) instead, so an out-of-flow measure leaf now observes the area without
 * the margin deduction the removed leaf code used to supply. That belongs to the out-of-flow work
 * (OF-01/OF-05) and is deliberately not compensated here.</p>
 */
public class LeafMarginP8Test {

    private static final float EPS = 0.001f;

    /** A 200x100 definite viewport: the containing block every scenario below starts from. */
    private static final float VIEWPORT_WIDTH = 200f;
    private static final float VIEWPORT_HEIGHT = 100f;

    // ------------------------------------------------------------------
    // Style helpers
    // ------------------------------------------------------------------

    private static TaffyStyle block() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        return style;
    }

    private static TaffyStyle display(TaffyDisplay display) {
        TaffyStyle style = new TaffyStyle();
        style.display = display;
        return style;
    }

    private static TaffyStyle sized(TaffyStyle style, float width, float height) {
        style.size = new TaffySize<>(
            Float.isNaN(width) ? TaffyDimension.AUTO : TaffyDimension.length(width),
            Float.isNaN(height) ? TaffyDimension.AUTO : TaffyDimension.length(height)
        );
        return style;
    }

    private static TaffyStyle allMargins(TaffyStyle style, float margin) {
        style.margin = TaffyRect.all(LengthPercentageAuto.length(margin));
        return style;
    }

    private static TaffyStyle flexRow(float width, float height) {
        return sized(display(TaffyDisplay.FLEX), width, height);
    }

    private static TaffyStyle grid(float width, float height) {
        TaffyStyle style = sized(display(TaffyDisplay.GRID), width, height);
        style.gridTemplateColumns = new ArrayList<>(List.of(TrackSizingFunction.fr(1)));
        return style;
    }

    // ------------------------------------------------------------------
    // Layout helpers
    // ------------------------------------------------------------------

    private static void layout(TaffyTree tree, NodeId root) {
        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(VIEWPORT_WIDTH),
            AvailableSpace.definite(VIEWPORT_HEIGHT)
        ));
    }

    private static void assertSize(Layout layout, float width, float height, String message) {
        assertEquals(width, layout.size().width, EPS, message + " width");
        assertEquals(height, layout.size().height, EPS, message + " height");
    }

    private static void assertLocation(Layout layout, float x, float y, String message) {
        assertEquals(x, layout.location().x, EPS, message + " x");
        assertEquals(y, layout.location().y, EPS, message + " y");
    }

    /** One record of the available space a measure callback observed. */
    private static List<TaffySize<AvailableSpace>> capturing() {
        return new ArrayList<>();
    }

    /** The definite values a measure callback observed on one axis of the available space. */
    private static List<Float> definiteValues(List<TaffySize<AvailableSpace>> captures, boolean width) {
        List<Float> values = new ArrayList<>();
        for (TaffySize<AvailableSpace> capture : captures) {
            AvailableSpace space = width ? capture.width : capture.height;
            if (space.isDefinite()) {
                values.add(space.getValue());
            }
        }
        return values;
    }

    private static void assertAllDefinite(List<Float> values, float expected, String message) {
        assertFalse(values.isEmpty(), message + ": the measure callback never saw a definite value");
        for (float value : values) {
            assertEquals(expected, value, EPS, message);
        }
    }

    // ------------------------------------------------------------------
    // Root calling source
    // ------------------------------------------------------------------

    /**
     * A leaf root with margins. The root pass subtracts the root's own margins, so the measured
     * available space is the viewport net of the margins; the leaf no longer subtracts them a
     * second time. The numbers are identical to the pre-fix ones (the leaf used to do the single
     * subtraction the root did not), which is exactly what this test protects: moving the
     * subtraction to the root must not change a leaf root's content size.
     */
    @Test
    @DisplayName("leaf root: available space is the viewport net of its own margin, exactly once")
    void leafRootSubtractsItsOwnMarginExactlyOnce() {
        List<TaffySize<AvailableSpace>> captures = capturing();
        TaffyTree tree = new TaffyTree();
        NodeId root = tree.newLeafWithMeasure(allMargins(block(), 10f), (knownDimensions, availableSpace) -> {
            captures.add(availableSpace);
            float height = availableSpace.height.isDefinite() ? availableSpace.height.getValue() : 5f;
            return new FloatSize(25f, height);
        });

        layout(tree, root);

        Layout layout = tree.getLayout(root);
        // The block root stretches to the available width (200 - 2 * 10) and is measured in the
        // block axis with 100 - 2 * 10 of content space.
        assertSize(layout, 180f, 80f, "leaf root with 10px margins");
        assertLocation(layout, 10f, 10f, "leaf root with 10px margins");
        assertEquals(1, captures.size(), "the leaf measure callback must run once");
        assertEquals(180f, captures.get(0).width.getValue(), EPS, "available width");
        assertEquals(80f, captures.get(0).height.getValue(), EPS, "available height");
    }

    /** A margin-free leaf root keeps its old values: the viewport reaches the measure callback. */
    @Test
    @DisplayName("leaf root without margin: available space is the whole viewport")
    void leafRootWithoutMarginKeepsValues() {
        List<TaffySize<AvailableSpace>> captures = capturing();
        TaffyTree tree = new TaffyTree();
        NodeId root = tree.newLeafWithMeasure(block(), (knownDimensions, availableSpace) -> {
            captures.add(availableSpace);
            float height = availableSpace.height.isDefinite() ? availableSpace.height.getValue() : 5f;
            return new FloatSize(25f, height);
        });

        layout(tree, root);

        Layout layout = tree.getLayout(root);
        assertSize(layout, 200f, 100f, "margin-free leaf root");
        assertLocation(layout, 0f, 0f, "margin-free leaf root");
        assertEquals(1, captures.size(), "the leaf measure callback must run once");
        assertEquals(200f, captures.get(0).width.getValue(), EPS, "available width");
        assertEquals(100f, captures.get(0).height.getValue(), EPS, "available height");
    }

    /**
     * A leaf root's margins and padding are each deducted exactly once: the available space handed
     * to the measure callback is the root's <em>content box</em> (border box minus its own padding;
     * the margins were removed before the layout algorithm ran).
     */
    @Test
    @DisplayName("leaf root: margin and padding are each deducted once from the measured space")
    void leafRootDeductsMarginAndPaddingOnce() {
        List<TaffySize<AvailableSpace>> captures = capturing();
        TaffyStyle style = allMargins(block(), 10f);
        style.padding = TaffyRect.all(LengthPercentage.length(5f));
        TaffyTree tree = new TaffyTree();
        NodeId root = tree.newLeafWithMeasure(style, (knownDimensions, availableSpace) -> {
            captures.add(availableSpace);
            float height = availableSpace.height.isDefinite() ? availableSpace.height.getValue() : 5f;
            float width = availableSpace.width.isDefinite() ? availableSpace.width.getValue() : 25f;
            return new FloatSize(width, height);
        });

        layout(tree, root);

        Layout layout = tree.getLayout(root);
        // The measured content (170 x 70) plus the 5px padding on each side.
        assertSize(layout, 180f, 80f, "leaf root with 10px margins and 5px padding");
        assertEquals(1, captures.size(), "the leaf measure callback must run once");
        // 200 - 2 * 10 margins - 2 * 5 padding, and 100 - 2 * 10 margins - 2 * 5 padding.
        assertEquals(170f, captures.get(0).width.getValue(), EPS, "available content width");
        assertEquals(70f, captures.get(0).height.getValue(), EPS, "available content height");
    }

    // ------------------------------------------------------------------
    // Block calling source
    // ------------------------------------------------------------------

    /**
     * The block algorithm subtracts a child's horizontal margins when it hands over the available
     * space ({@code container_inner_width - item_non_auto_x_margin_sum}). The child's measure
     * callback must therefore see 200 - 2 * 10 = 180, not the 160 the removed leaf deduction
     * produced.
     */
    @Test
    @DisplayName("block parent: a leaf child's margin is subtracted exactly once")
    void blockParentSubtractsChildMarginOnce() {
        List<TaffySize<AvailableSpace>> captures = capturing();
        TaffyStyle childStyle = allMargins(block(), 10f);
        // A non-stretch justify-self sizes the auto width as fit-content by measuring the child
        // under the available width, so the measured value is the child's used width.
        childStyle.justifySelf = AlignItems.CENTER;
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeafWithMeasure(childStyle, (knownDimensions, availableSpace) -> {
            captures.add(availableSpace);
            float width = availableSpace.width.isDefinite() ? availableSpace.width.getValue() : 30f;
            return new FloatSize(width, 10f);
        });
        NodeId root = tree.newWithChildren(sized(block(), 200f, 100f), child);

        layout(tree, root);

        assertSize(tree.getLayout(child), 180f, 10f, "block child with 10px margins");
        assertLocation(tree.getLayout(child), 10f, 10f, "block child with 10px margins");
        assertAllDefinite(definiteValues(captures, true), 180f,
            "block parent already subtracts the child's margins; the leaf must not do it again");
    }

    /** Without margins the block parent still hands the whole inner width to the child. */
    @Test
    @DisplayName("block parent without margin: available width is unchanged")
    void blockParentWithoutMarginKeepsValues() {
        List<TaffySize<AvailableSpace>> captures = capturing();
        TaffyStyle childStyle = block();
        childStyle.justifySelf = AlignItems.CENTER;
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeafWithMeasure(childStyle, (knownDimensions, availableSpace) -> {
            captures.add(availableSpace);
            float width = availableSpace.width.isDefinite() ? availableSpace.width.getValue() : 30f;
            return new FloatSize(width, 10f);
        });
        NodeId root = tree.newWithChildren(sized(block(), 200f, 100f), child);

        layout(tree, root);

        assertSize(tree.getLayout(child), 200f, 10f, "margin-free block child");
        assertAllDefinite(definiteValues(captures, true), 200f, "margin-free block parent available width");
    }

    // ------------------------------------------------------------------
    // Flex calling source
    // ------------------------------------------------------------------

    /**
     * The flex algorithm subtracts a flex item's margins from the cross-axis available space
     * (FL-10 / upstream #1234). With {@code align-self: start} the item's cross size stays
     * content-based, so the measure callback observes that space directly: 100 - 2 * 10 = 80
     * instead of the former 60.
     */
    @Test
    @DisplayName("flex parent: a leaf item's margin is subtracted exactly once")
    void flexParentSubtractsChildMarginOnce() {
        List<TaffySize<AvailableSpace>> captures = capturing();
        TaffyStyle childStyle = allMargins(block(), 10f);
        childStyle.alignSelf = AlignItems.START;
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeafWithMeasure(childStyle, (knownDimensions, availableSpace) -> {
            captures.add(availableSpace);
            float height = availableSpace.height.isDefinite() ? availableSpace.height.getValue() : 10f;
            return new FloatSize(30f, height);
        });
        TaffyStyle rootStyle = flexRow(200f, 100f);
        rootStyle.alignItems = AlignItems.START;
        NodeId root = tree.newWithChildren(rootStyle, child);

        layout(tree, root);

        assertSize(tree.getLayout(child), 30f, 80f, "flex item with 10px margins");
        assertLocation(tree.getLayout(child), 10f, 10f, "flex item with 10px margins");
        assertAllDefinite(definiteValues(captures, false), 80f,
            "flex parent already subtracts the item's margins; the leaf must not do it again");
    }

    /** Without margins the flex parent still hands the whole cross size to the item. */
    @Test
    @DisplayName("flex parent without margin: available cross space is unchanged")
    void flexParentWithoutMarginKeepsValues() {
        List<TaffySize<AvailableSpace>> captures = capturing();
        TaffyStyle childStyle = block();
        childStyle.alignSelf = AlignItems.START;
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeafWithMeasure(childStyle, (knownDimensions, availableSpace) -> {
            captures.add(availableSpace);
            float height = availableSpace.height.isDefinite() ? availableSpace.height.getValue() : 10f;
            return new FloatSize(30f, height);
        });
        TaffyStyle rootStyle = flexRow(200f, 100f);
        rootStyle.alignItems = AlignItems.START;
        NodeId root = tree.newWithChildren(rootStyle, child);

        layout(tree, root);

        assertSize(tree.getLayout(child), 30f, 100f, "margin-free flex item");
        assertAllDefinite(definiteValues(captures, false), 100f, "margin-free flex parent available cross space");
    }

    // ------------------------------------------------------------------
    // Grid calling source
    // ------------------------------------------------------------------

    /**
     * The grid algorithm subtracts an item's margins from the available space it measures a
     * content-sized item with (upstream {@code available_space_minus_margins}). The measured
     * width is 200 - 2 * 10 = 180 instead of 160, and no measuring pass may see the
     * double-deducted 160.
     */
    @Test
    @DisplayName("grid parent: a leaf item's margin is subtracted exactly once")
    void gridParentSubtractsChildMarginOnce() {
        List<TaffySize<AvailableSpace>> captures = capturing();
        TaffyStyle childStyle = allMargins(block(), 10f);
        // Center the item so its width is content-sized (stretch would impose a known width).
        childStyle.justifySelf = AlignItems.CENTER;
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeafWithMeasure(childStyle, (knownDimensions, availableSpace) -> {
            captures.add(availableSpace);
            float width = availableSpace.width.isDefinite() ? availableSpace.width.getValue() : 30f;
            return new FloatSize(width, 10f);
        });
        NodeId root = tree.newWithChildren(grid(200f, 100f), child);

        layout(tree, root);

        assertSize(tree.getLayout(child), 180f, 80f, "grid item with 10px margins");
        assertLocation(tree.getLayout(child), 10f, 10f, "grid item with 10px margins");
        List<Float> widths = definiteValues(captures, true);
        assertTrue(widths.contains(180f), "the content-sized measurement must see the grid area net of margins");
        assertFalse(widths.contains(160f), "no grid measuring pass may subtract the item's margins twice");
    }

    /** Without margins the grid parent still hands the whole grid area to the item. */
    @Test
    @DisplayName("grid parent without margin: available width is unchanged")
    void gridParentWithoutMarginKeepsValues() {
        List<TaffySize<AvailableSpace>> captures = capturing();
        TaffyStyle childStyle = block();
        childStyle.justifySelf = AlignItems.CENTER;
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeafWithMeasure(childStyle, (knownDimensions, availableSpace) -> {
            captures.add(availableSpace);
            float width = availableSpace.width.isDefinite() ? availableSpace.width.getValue() : 30f;
            return new FloatSize(width, 10f);
        });
        NodeId root = tree.newWithChildren(grid(200f, 100f), child);

        layout(tree, root);

        assertSize(tree.getLayout(child), 200f, 100f, "margin-free grid item");
        List<Float> widths = definiteValues(captures, true);
        assertTrue(widths.contains(200f), "margin-free grid parent available width");
        assertFalse(widths.contains(180f), "no pass may deduct a margin the item does not have");
    }

    /**
     * T24 is fixed: the grid <em>intrinsic contribution</em> paths now subtract the item's margins
     * from the definite available space too ({@code available_space_minus_margins}, upstream
     * #1234 / diff-grid T24), exactly like the final position pass. The item's 10px margins turn
     * the 200px grid area into 180px of available space for the measured leaf; before T24 the
     * intrinsic-contribution measurement still saw the whole 200px track.
     */
    @Test
    @DisplayName("T24 fixed: the grid intrinsic-contribution pass sees the grid area net of margins")
    void gridIntrinsicContributionSeesTheGridAreaMinusMargins() {
        List<TaffySize<AvailableSpace>> captures = capturing();
        TaffyStyle childStyle = allMargins(block(), 10f);
        childStyle.justifySelf = AlignItems.CENTER;
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeafWithMeasure(childStyle, (knownDimensions, availableSpace) -> {
            captures.add(availableSpace);
            float width = availableSpace.width.isDefinite() ? availableSpace.width.getValue() : 30f;
            return new FloatSize(width, 10f);
        });
        NodeId root = tree.newWithChildren(grid(200f, 100f), child);

        layout(tree, root);

        // 200 - 2 * 10 = 180: the intrinsic-contribution measurement is net of the item's margins.
        assertTrue(definiteValues(captures, true).contains(180f),
            "grid intrinsic contribution must measure under the grid area minus margins (T24)");
        assertFalse(definiteValues(captures, true).contains(200f),
            "no grid intrinsic measurement may see the undeducted grid area (T24)");
    }

    // ------------------------------------------------------------------
    // Root margin for every display type
    // ------------------------------------------------------------------

    /**
     * Before X2 the root subtracted its own margins only on the block path (for the stretched
     * width). A wrapping flex root is the observable case: with a 10px margin the available width
     * is 180, so two 95px items no longer fit on one line and the root becomes 180x40 instead of
     * the former 190x20.
     */
    @Test
    @DisplayName("flex root: its own margin narrows the available space before line breaking")
    void flexRootSubtractsItsOwnMargin() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = allMargins(display(TaffyDisplay.FLEX), 10f);
        rootStyle.flexWrap = FlexWrap.WRAP;
        NodeId first = tree.newLeaf(sized(block(), 95f, 20f));
        NodeId second = tree.newLeaf(sized(block(), 95f, 20f));
        NodeId root = tree.newWithChildren(rootStyle, first, second);

        layout(tree, root);

        // 2 * 95 = 190 > 180, so the second item wraps under the first.
        assertSize(tree.getLayout(root), 180f, 40f, "wrapping flex root with 10px margins");
        assertLocation(tree.getLayout(root), 10f, 10f, "root margin box position");
        assertLocation(tree.getLayout(second), 0f, 20f, "second item after wrapping");
    }

    /**
     * A grid root with {@code repeat(auto-fill, minmax(100px, 1fr))}: the available width decides
     * how many repetitions fit. With a 10px root margin the available width is 180 (one track),
     * against 200 (two tracks) before X2 subtracted the root's margin on grid roots.
     */
    @Test
    @DisplayName("grid root: its own margin narrows the available space before repetition")
    void gridRootSubtractsItsOwnMargin() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = allMargins(display(TaffyDisplay.GRID), 10f);
        rootStyle.gridTemplateColumnsWithRepeat = new ArrayList<>(List.of(
            GridTemplateComponent.autoFill(TrackSizingFunction.minmax(
                TrackSizingFunction.fixed(100f), TrackSizingFunction.fr(1f)))));
        NodeId root = tree.newWithChildren(
            rootStyle,
            tree.newLeaf(sized(block(), 50f, 20f)),
            tree.newLeaf(sized(block(), 50f, 20f)),
            tree.newLeaf(sized(block(), 50f, 20f))
        );

        layout(tree, root);

        assertSize(tree.getLayout(root), 180f, 60f, "grid root with 10px margins");
        assertLocation(tree.getLayout(root), 10f, 10f, "root margin box position");
    }

    /** A block root keeps its historical block-axis numbers (it already subtracted its margin). */
    @Test
    @DisplayName("block root: its own margin keeps the historical stretched width")
    void blockRootKeepsHistoricalValues() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(sized(block(), 50f, 20f));
        NodeId root = tree.newWithChildren(allMargins(block(), 10f), child);

        layout(tree, root);

        assertSize(tree.getLayout(root), 180f, 20f, "block root with 10px margins");
        assertLocation(tree.getLayout(root), 10f, 10f, "block root margin box position");
    }

    // ------------------------------------------------------------------
    // Known dimensions keep precedence
    // ------------------------------------------------------------------

    /**
     * A style-defined size is a known dimension: the measure callback receives it as the available
     * space (upstream {@code available_space.maybe_set(known_dimensions)}) and margins are
     * irrelevant on that axis. This is why ordinary sized leaves never changed with X2; only the
     * measured, auto-sized ones did.
     */
    @Test
    @DisplayName("known dimensions win over available space: a sized leaf ignores its margin")
    void knownDimensionsTakePrecedenceOverMargin() {
        List<TaffySize<AvailableSpace>> captures = capturing();
        TaffyStyle childStyle = allMargins(sized(block(), 100f, Float.NaN), 10f);
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeafWithMeasure(childStyle, (knownDimensions, availableSpace) -> {
            captures.add(availableSpace);
            return new FloatSize(20f, 10f);
        });
        NodeId root = tree.newWithChildren(sized(block(), 200f, 100f), child);

        layout(tree, root);

        assertSize(tree.getLayout(child), 100f, 10f, "sized block child with 10px margins");
        assertEquals(1, captures.size(), "the leaf measure callback must run once");
        assertEquals(100f, captures.get(0).width.getValue(), EPS,
            "a known width is the available space on that axis, with no margin subtraction");
    }
}
