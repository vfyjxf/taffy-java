package dev.vfyjxf.taffy.generated.leaf;

import dev.vfyjxf.taffy.geometry.*;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.tree.*;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Generated tests for leaf layout fixtures.
 */
public class LeafTest {

    private static final float EPSILON = 0.1f;

    private static MeasureFunc ahemTextMeasure(String text, boolean vertical) {
        final String trimmed = text == null ? "" : text.trim();
        return (knownDimensions, availableSpace) -> {
            if (!Float.isNaN(knownDimensions.width) && !Float.isNaN(knownDimensions.height)) {
                return new FloatSize(knownDimensions.width, knownDimensions.height);
            }

            final char ZWS = '\u200B';
            final float H_WIDTH = 10.0f;
            final float H_HEIGHT = 10.0f;

            String[] parts = trimmed.isEmpty() ? new String[0] : trimmed.split(String.valueOf(ZWS), -1);
            if (parts.length == 0) {
                float w = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : 0.0f;
                float h = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : 0.0f;
                return new FloatSize(w, h);
            }

            int minLineLength = 0;
            int maxLineLength = 0;
            for (String p : parts) {
                int len = p.length();
                if (len > minLineLength) minLineLength = len;
                maxLineLength += len;
            }

            float knownInline = vertical ? knownDimensions.height : knownDimensions.width;
            float knownBlock = vertical ? knownDimensions.width : knownDimensions.height;
            AvailableSpace availInline = vertical ? availableSpace.height : availableSpace.width;

            float inlineSize;
            if (!Float.isNaN(knownInline)) {
                inlineSize = knownInline;
            } else if (availInline != null && availInline.isMinContent()) {
                inlineSize = minLineLength * H_WIDTH;
            } else if (availInline != null && availInline.isMaxContent()) {
                inlineSize = maxLineLength * H_WIDTH;
            } else if (availInline != null && availInline.isDefinite()) {
                inlineSize = Math.min(availInline.getValue(), maxLineLength * H_WIDTH);
            } else {
                inlineSize = maxLineLength * H_WIDTH;
            }
            inlineSize = Math.max(inlineSize, minLineLength * H_WIDTH);

            float blockSize;
            if (!Float.isNaN(knownBlock)) {
                blockSize = knownBlock;
            } else {
                int inlineLineLength = (int) Math.floor(inlineSize / H_WIDTH);
                int lineCount = 1;
                int currentLineLength = 0;
                for (String p : parts) {
                    int len = p.length();
                    if (currentLineLength + len > inlineLineLength) {
                        if (currentLineLength > 0) {
                            lineCount += 1;
                        }
                        currentLineLength = len;
                    } else {
                        currentLineLength += len;
                    }
                }
                blockSize = lineCount * H_HEIGHT;
            }

            FloatSize computed = vertical
                ? new FloatSize(blockSize, inlineSize)
                : new FloatSize(inlineSize, blockSize);

            float outW = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : computed.width;
            float outH = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : computed.height;
            return new FloatSize(outW, outH);
        };
    }

    @Test
    @DisplayName("leaf_overflow_scrollbars_affect_available_space_x_axis__border_box")
    void leafOverflowScrollbarsAffectAvailableSpaceXAxisBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.size = new Size<>(Dimension.length(45.0f), Dimension.length(45.0f));
        nodeStyle.overflow = new Point<>(Overflow.SCROLL, Overflow.VISIBLE);
        nodeStyle.scrollbarWidth = 15.0f;
        MeasureFunc nodeMeasure = ahemTextMeasure("HHHHHHHHHHHHHHHHHHHHH", false);
        NodeId node = tree.newLeafWithMeasure(nodeStyle, nodeMeasure);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(45.0f, nodeLayout.size().width, "width of node");
        assertEquals(45.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_overflow_scrollbars_affect_available_space_x_axis__content_box")
    void leafOverflowScrollbarsAffectAvailableSpaceXAxisContentBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.size = new Size<>(Dimension.length(45.0f), Dimension.length(45.0f));
        nodeStyle.overflow = new Point<>(Overflow.SCROLL, Overflow.VISIBLE);
        nodeStyle.scrollbarWidth = 15.0f;
        MeasureFunc nodeMeasure = ahemTextMeasure("HHHHHHHHHHHHHHHHHHHHH", false);
        NodeId node = tree.newLeafWithMeasure(nodeStyle, nodeMeasure);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(45.0f, nodeLayout.size().width, "width of node");
        assertEquals(45.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_overflow_scrollbars_affect_available_space_y_axis__border_box")
    void leafOverflowScrollbarsAffectAvailableSpaceYAxisBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.size = new Size<>(Dimension.length(45.0f), Dimension.length(45.0f));
        nodeStyle.overflow = new Point<>(Overflow.VISIBLE, Overflow.SCROLL);
        nodeStyle.scrollbarWidth = 15.0f;
        MeasureFunc nodeMeasure = ahemTextMeasure("HHHHHHHHHHHHHHHHHHHHH", false);
        NodeId node = tree.newLeafWithMeasure(nodeStyle, nodeMeasure);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(45.0f, nodeLayout.size().width, "width of node");
        assertEquals(45.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_overflow_scrollbars_affect_available_space_y_axis__content_box")
    void leafOverflowScrollbarsAffectAvailableSpaceYAxisContentBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.size = new Size<>(Dimension.length(45.0f), Dimension.length(45.0f));
        nodeStyle.overflow = new Point<>(Overflow.VISIBLE, Overflow.SCROLL);
        nodeStyle.scrollbarWidth = 15.0f;
        MeasureFunc nodeMeasure = ahemTextMeasure("HHHHHHHHHHHHHHHHHHHHH", false);
        NodeId node = tree.newLeafWithMeasure(nodeStyle, nodeMeasure);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(45.0f, nodeLayout.size().width, "width of node");
        assertEquals(45.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_overflow_scrollbars_overridden_by_available_space__border_box")
    void leafOverflowScrollbarsOverriddenByAvailableSpaceBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.flexGrow = 1.0f;
        node0Style.overflow = new Point<>(Overflow.SCROLL, Overflow.SCROLL);
        node0Style.scrollbarWidth = 15.0f;
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.size = new Size<>(Dimension.length(2.0f), Dimension.length(4.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(2.0f, nodeLayout.size().width, "width of node");
        assertEquals(4.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(2.0f, node0Layout.size().width, "width of node0");
        assertEquals(4.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("leaf_overflow_scrollbars_overridden_by_available_space__content_box")
    void leafOverflowScrollbarsOverriddenByAvailableSpaceContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.flexGrow = 1.0f;
        node0Style.overflow = new Point<>(Overflow.SCROLL, Overflow.SCROLL);
        node0Style.scrollbarWidth = 15.0f;
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.size = new Size<>(Dimension.length(2.0f), Dimension.length(4.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(2.0f, nodeLayout.size().width, "width of node");
        assertEquals(4.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(2.0f, node0Layout.size().width, "width of node0");
        assertEquals(4.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("leaf_overflow_scrollbars_overridden_by_max_size__border_box")
    void leafOverflowScrollbarsOverriddenByMaxSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.maxSize = new Size<>(Dimension.length(2.0f), Dimension.length(4.0f));
        nodeStyle.overflow = new Point<>(Overflow.SCROLL, Overflow.SCROLL);
        nodeStyle.scrollbarWidth = 15.0f;
        NodeId node = tree.newLeaf(nodeStyle);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(2.0f, nodeLayout.size().width, "width of node");
        assertEquals(4.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_overflow_scrollbars_overridden_by_max_size__content_box")
    void leafOverflowScrollbarsOverriddenByMaxSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.maxSize = new Size<>(Dimension.length(2.0f), Dimension.length(4.0f));
        nodeStyle.overflow = new Point<>(Overflow.SCROLL, Overflow.SCROLL);
        nodeStyle.scrollbarWidth = 15.0f;
        NodeId node = tree.newLeaf(nodeStyle);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(2.0f, nodeLayout.size().width, "width of node");
        assertEquals(4.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_overflow_scrollbars_overridden_by_size__border_box")
    void leafOverflowScrollbarsOverriddenBySizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.size = new Size<>(Dimension.length(2.0f), Dimension.length(4.0f));
        nodeStyle.overflow = new Point<>(Overflow.SCROLL, Overflow.SCROLL);
        nodeStyle.scrollbarWidth = 15.0f;
        NodeId node = tree.newLeaf(nodeStyle);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(2.0f, nodeLayout.size().width, "width of node");
        assertEquals(4.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_overflow_scrollbars_overridden_by_size__content_box")
    void leafOverflowScrollbarsOverriddenBySizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.size = new Size<>(Dimension.length(2.0f), Dimension.length(4.0f));
        nodeStyle.overflow = new Point<>(Overflow.SCROLL, Overflow.SCROLL);
        nodeStyle.scrollbarWidth = 15.0f;
        NodeId node = tree.newLeaf(nodeStyle);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(2.0f, nodeLayout.size().width, "width of node");
        assertEquals(4.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_overflow_scrollbars_take_up_space_both_axis__border_box")
    void leafOverflowScrollbarsTakeUpSpaceBothAxisBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.overflow = new Point<>(Overflow.SCROLL, Overflow.SCROLL);
        nodeStyle.scrollbarWidth = 15.0f;
        MeasureFunc nodeMeasure = ahemTextMeasure("HH", false);
        NodeId node = tree.newLeafWithMeasure(nodeStyle, nodeMeasure);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(35.0f, nodeLayout.size().width, "width of node");
        assertEquals(25.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_overflow_scrollbars_take_up_space_both_axis__content_box")
    void leafOverflowScrollbarsTakeUpSpaceBothAxisContentBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.overflow = new Point<>(Overflow.SCROLL, Overflow.SCROLL);
        nodeStyle.scrollbarWidth = 15.0f;
        MeasureFunc nodeMeasure = ahemTextMeasure("HH", false);
        NodeId node = tree.newLeafWithMeasure(nodeStyle, nodeMeasure);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(35.0f, nodeLayout.size().width, "width of node");
        assertEquals(25.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_overflow_scrollbars_take_up_space_x_axis__border_box")
    void leafOverflowScrollbarsTakeUpSpaceXAxisBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.overflow = new Point<>(Overflow.SCROLL, Overflow.VISIBLE);
        nodeStyle.scrollbarWidth = 15.0f;
        MeasureFunc nodeMeasure = ahemTextMeasure("HH", false);
        NodeId node = tree.newLeafWithMeasure(nodeStyle, nodeMeasure);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(20.0f, nodeLayout.size().width, "width of node");
        assertEquals(25.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_overflow_scrollbars_take_up_space_x_axis__content_box")
    void leafOverflowScrollbarsTakeUpSpaceXAxisContentBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.overflow = new Point<>(Overflow.SCROLL, Overflow.VISIBLE);
        nodeStyle.scrollbarWidth = 15.0f;
        MeasureFunc nodeMeasure = ahemTextMeasure("HH", false);
        NodeId node = tree.newLeafWithMeasure(nodeStyle, nodeMeasure);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(20.0f, nodeLayout.size().width, "width of node");
        assertEquals(25.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_overflow_scrollbars_take_up_space_y_axis__border_box")
    void leafOverflowScrollbarsTakeUpSpaceYAxisBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.overflow = new Point<>(Overflow.VISIBLE, Overflow.SCROLL);
        nodeStyle.scrollbarWidth = 15.0f;
        MeasureFunc nodeMeasure = ahemTextMeasure("HH", false);
        NodeId node = tree.newLeafWithMeasure(nodeStyle, nodeMeasure);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(35.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_overflow_scrollbars_take_up_space_y_axis__content_box")
    void leafOverflowScrollbarsTakeUpSpaceYAxisContentBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.overflow = new Point<>(Overflow.VISIBLE, Overflow.SCROLL);
        nodeStyle.scrollbarWidth = 15.0f;
        MeasureFunc nodeMeasure = ahemTextMeasure("HH", false);
        NodeId node = tree.newLeafWithMeasure(nodeStyle, nodeMeasure);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(35.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_padding_border_overrides_max_size__border_box")
    void leafPaddingBorderOverridesMaxSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.maxSize = new Size<>(Dimension.length(12.0f), Dimension.length(12.0f));
        nodeStyle.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        nodeStyle.border = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        NodeId node = tree.newLeaf(nodeStyle);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(22.0f, nodeLayout.size().width, "width of node");
        assertEquals(14.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_padding_border_overrides_max_size__content_box")
    void leafPaddingBorderOverridesMaxSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.maxSize = new Size<>(Dimension.length(12.0f), Dimension.length(12.0f));
        nodeStyle.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        nodeStyle.border = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        NodeId node = tree.newLeaf(nodeStyle);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(22.0f, nodeLayout.size().width, "width of node");
        assertEquals(14.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_padding_border_overrides_min_size__border_box")
    void leafPaddingBorderOverridesMinSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.minSize = new Size<>(Dimension.length(0.0f), Dimension.length(0.0f));
        nodeStyle.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        nodeStyle.border = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        NodeId node = tree.newLeaf(nodeStyle);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(22.0f, nodeLayout.size().width, "width of node");
        assertEquals(14.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_padding_border_overrides_min_size__content_box")
    void leafPaddingBorderOverridesMinSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.minSize = new Size<>(Dimension.length(0.0f), Dimension.length(0.0f));
        nodeStyle.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        nodeStyle.border = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        NodeId node = tree.newLeaf(nodeStyle);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(22.0f, nodeLayout.size().width, "width of node");
        assertEquals(14.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_padding_border_overrides_size__border_box")
    void leafPaddingBorderOverridesSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.size = new Size<>(Dimension.length(12.0f), Dimension.length(12.0f));
        nodeStyle.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        nodeStyle.border = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        NodeId node = tree.newLeaf(nodeStyle);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(22.0f, nodeLayout.size().width, "width of node");
        assertEquals(14.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_padding_border_overrides_size__content_box")
    void leafPaddingBorderOverridesSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.size = new Size<>(Dimension.length(12.0f), Dimension.length(12.0f));
        nodeStyle.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        nodeStyle.border = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        NodeId node = tree.newLeaf(nodeStyle);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(34.0f, nodeLayout.size().width, "width of node");
        assertEquals(26.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_with_content_and_border__border_box")
    void leafWithContentAndBorderBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.border = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        MeasureFunc nodeMeasure = ahemTextMeasure("HHHH", false);
        NodeId node = tree.newLeafWithMeasure(nodeStyle, nodeMeasure);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(18.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_with_content_and_border__content_box")
    void leafWithContentAndBorderContentBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.border = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        MeasureFunc nodeMeasure = ahemTextMeasure("HHHH", false);
        NodeId node = tree.newLeafWithMeasure(nodeStyle, nodeMeasure);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(18.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_with_content_and_padding__border_box")
    void leafWithContentAndPaddingBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        MeasureFunc nodeMeasure = ahemTextMeasure("HHHH", false);
        NodeId node = tree.newLeafWithMeasure(nodeStyle, nodeMeasure);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(18.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_with_content_and_padding__content_box")
    void leafWithContentAndPaddingContentBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        MeasureFunc nodeMeasure = ahemTextMeasure("HHHH", false);
        NodeId node = tree.newLeafWithMeasure(nodeStyle, nodeMeasure);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(18.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_with_content_and_padding_border__border_box")
    void leafWithContentAndPaddingBorderBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        nodeStyle.border = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        MeasureFunc nodeMeasure = ahemTextMeasure("HHHH", false);
        NodeId node = tree.newLeafWithMeasure(nodeStyle, nodeMeasure);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(62.0f, nodeLayout.size().width, "width of node");
        assertEquals(24.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

    @Test
    @DisplayName("leaf_with_content_and_padding_border__content_box")
    void leafWithContentAndPaddingBorderContentBox() {
        TaffyTree tree = new TaffyTree();

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        nodeStyle.border = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        MeasureFunc nodeMeasure = ahemTextMeasure("HHHH", false);
        NodeId node = tree.newLeafWithMeasure(nodeStyle, nodeMeasure);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(62.0f, nodeLayout.size().width, "width of node");
        assertEquals(24.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
    }

}
