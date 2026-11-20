package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P8 residual regression tests for block layout (BK-04 and BK-15).
 *
 * <p>BK-04 / upstream #1002: block layout stretch-sizes in-flow children to the containing
 * block's width, but tables and compressible replaced elements are exempt — with
 * {@code width: auto} they resolve their own size (CSS2 section 10.3.3), and a replaced
 * element's auto width is its intrinsic size. The style-level split between
 * {@code itemIsReplaced} and {@code itemIsCompressibleReplaced} landed in ST-05; this class pins
 * the block-layout wiring of the derived {@link TaffyStyle#isCompressibleReplaced()}.</p>
 *
 * <p>The upstream fixtures exercised here are {@code tests/hand_written/block_replaced.rs}
 * (#1002) and {@code tests/xml/block/block_aspect_ratio_stretch_width*}. In-flow children keep
 * the legacy stretch behavior for every non-table, non-replaced box, which the regression cases
 * assert value-for-value.</p>
 */
public class BlockResidualP8Test {

    private static final float EPSILON = 0.001f;

    // === Helpers ===

    private static TaffyStyle blockStyle() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.BLOCK;
        return style;
    }

    private static TaffySize<TaffyDimension> size(TaffyDimension width, TaffyDimension height) {
        return new TaffySize<>(width, height);
    }

    /**
     * A fixed intrinsic size, like upstream's badge test context (142x20). The measure callback
     * ignores the supplied constraints so that a wrongly stretch-sized parent is observable: a
     * child laid out with a known 600px width reports 600 instead of the intrinsic 142.
     */
    private static MeasureFunc intrinsic(float width, float height) {
        return (knownDimensions, availableSpace) -> new FloatSize(width, height);
    }

    private static void assertSize(Layout layout, float width, float height, String message) {
        assertEquals(width, layout.size().width, EPSILON, message + " width");
        assertEquals(height, layout.size().height, EPSILON, message + " height");
    }

    /** Lays a single child out in a block container of the given width and returns the child. */
    private static Layout layoutChildInBlock(TaffyStyle childStyle, float containerWidth) {
        TaffyTree tree = new TaffyTree();
        NodeId child = childStyle.itemIsReplaced || childStyle.itemIsCompressibleReplaced || childStyle.itemIsTable
                       ? tree.newLeafWithMeasure(childStyle, intrinsic(142f, 20f))
                       : tree.newLeaf(childStyle);
        TaffyStyle rootStyle = blockStyle();
        rootStyle.size = size(TaffyDimension.length(containerWidth), TaffyDimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());
        return tree.getLayout(child);
    }

    // === BK-04: tables and compressible replaced elements are not stretch-sized ===

    /**
     * Upstream {@code block_replaced::auto_width_uses_intrinsic_size} (#1002): a block-level
     * replaced element with {@code width: auto} uses its intrinsic width (142px), not the 600px
     * of its containing block.
     */
    @Test
    @DisplayName("BK-04: a replaced child's auto width is its intrinsic size, not the stretch width")
    void replacedAutoWidthUsesIntrinsicSize() {
        TaffyStyle childStyle = blockStyle();
        childStyle.itemIsReplaced = true;

        Layout child = layoutChildInBlock(childStyle, 600f);

        assertSize(child, 142f, 20f, "replaced child");
        assertEquals(0f, child.location().x, EPSILON, "replaced child x");
    }

    /**
     * The new ST-05 flag is the other spelling of "compressible replaced": a form control with
     * an intrinsic size (not {@code itemIsReplaced}) is exempt from stretch sizing too. Before
     * BK-04 the flag only fed the min-content cap in BK-12; block stretch ignored it.
     */
    @Test
    @DisplayName("BK-04: a compressible replaced form control is not stretch-sized either")
    void compressibleReplacedAutoWidthUsesIntrinsicSize() {
        TaffyStyle childStyle = blockStyle();
        childStyle.itemIsCompressibleReplaced = true;

        Layout child = layoutChildInBlock(childStyle, 600f);

        assertSize(child, 142f, 20f, "compressible replaced child");
    }

    /**
     * Old-value regression: the exemption is limited to tables and compressible replaced boxes.
     * An ordinary block-level leaf with the same intrinsic size is still stretch-sized to the
     * containing block's width.
     */
    @Test
    @DisplayName("BK-04: a non-replaced child keeps the legacy stretch width")
    void nonReplacedChildIsStillStretchSized() {
        TaffyStyle childStyle = blockStyle();
        // No intrinsic callback for a plain leaf; the block's auto content height is zero. The
        // observable behavior is that the child's border box fills the 600px containing block.
        Layout child = layoutChildInBlock(childStyle, 600f);

        assertSize(child, 600f, 0f, "plain block child");
    }

    /**
     * Old-value regression: tables were already exempt before this change (they are decided by
     * {@code itemIsTable}), so the table path must keep resolving its own width.
     */
    @Test
    @DisplayName("BK-04: a table child still resolves its own width")
    void tableChildStillResolvesOwnWidth() {
        TaffyStyle childStyle = blockStyle();
        childStyle.itemIsTable = true;

        Layout child = layoutChildInBlock(childStyle, 600f);

        assertSize(child, 142f, 20f, "table child");
    }

    /**
     * Upstream {@code block_replaced::explicit_width_is_used} (#1002): the exemption only changes
     * how {@code width: auto} is resolved; an explicit style width still wins.
     */
    @Test
    @DisplayName("BK-04: a replaced child's explicit width and height are used")
    void replacedExplicitWidthIsUsed() {
        TaffyStyle childStyle = blockStyle();
        childStyle.itemIsReplaced = true;
        childStyle.size = size(TaffyDimension.length(300f), TaffyDimension.length(50f));

        Layout child = layoutChildInBlock(childStyle, 600f);

        assertSize(child, 300f, 50f, "replaced child with explicit size");
    }

    /**
     * Upstream {@code block_replaced::max_width_clamps_intrinsic_size} (#1002): after the auto
     * width resolves to the intrinsic size, the resolved min/max sizes clamp it as usual.
     */
    @Test
    @DisplayName("BK-04: max-width clamps the intrinsic width")
    void maxWidthClampsReplacedIntrinsicSize() {
        TaffyStyle childStyle = blockStyle();
        childStyle.itemIsReplaced = true;
        childStyle.maxSize = size(TaffyDimension.percent(1f), TaffyDimension.AUTO);

        Layout child = layoutChildInBlock(childStyle, 100f);

        assertEquals(100f, child.size().width, EPSILON, "replaced child width clamped by 100% max-width");
    }

    /**
     * Upstream {@code block_replaced::auto_margins_center_replaced_child} (#1002): the intrinsic
     * width leaves horizontal free space, which the auto margins absorb.
     */
    @Test
    @DisplayName("BK-04: auto margins center a replaced child")
    void autoMarginsCenterReplacedChild() {
        TaffyStyle childStyle = blockStyle();
        childStyle.itemIsReplaced = true;
        childStyle.margin = new TaffyRect<>(
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.ZERO,
            LengthPercentageAuto.ZERO
        );

        Layout child = layoutChildInBlock(childStyle, 600f);

        assertSize(child, 142f, 20f, "centered replaced child");
        assertEquals((600f - 142f) / 2f, child.location().x, EPSILON, "centered replaced child x");
    }

    /**
     * The exemption is keyed on the resolved {@code justify-self}: an explicit
     * {@code justify-self: stretch} opts a table or replaced box back into stretch sizing
     * (upstream #1250's stretch exception, kept by #1002).
     */
    @Test
    @DisplayName("BK-04: justify-self stretch still stretches a replaced child")
    void justifySelfStretchStretchesReplacedChild() {
        TaffyStyle childStyle = blockStyle();
        childStyle.itemIsReplaced = true;
        childStyle.justifySelf = AlignItems.STRETCH;

        Layout child = layoutChildInBlock(childStyle, 600f);

        assertEquals(600f, child.size().width, EPSILON, "stretch-justified replaced child width");
    }
}
