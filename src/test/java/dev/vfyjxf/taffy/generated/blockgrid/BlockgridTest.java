package dev.vfyjxf.taffy.generated.blockgrid;

import dev.vfyjxf.taffy.geometry.*;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.tree.*;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Generated tests for blockgrid layout fixtures.
 */
public class BlockgridTest {

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
    @DisplayName("blockgrid_block_in_grid_auto__border_box")
    void blockgridBlockInGridAutoBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.auto());
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(40.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(40.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_block_in_grid_auto__content_box")
    void blockgridBlockInGridAutoContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.auto());
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(40.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(40.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_block_in_grid_fixed_fit_content_larger__border_box")
    void blockgridBlockInGridFixedFitContentLargerBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fitContent(LengthPercentage.length(50.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(40.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(40.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_block_in_grid_fixed_fit_content_larger__content_box")
    void blockgridBlockInGridFixedFitContentLargerContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fitContent(LengthPercentage.length(50.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(40.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(40.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_block_in_grid_fixed_fit_content_middle__border_box")
    void blockgridBlockInGridFixedFitContentMiddleBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fitContent(LengthPercentage.length(30.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(30.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(30.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(30.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_block_in_grid_fixed_fit_content_middle__content_box")
    void blockgridBlockInGridFixedFitContentMiddleContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fitContent(LengthPercentage.length(30.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(30.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(30.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(30.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_block_in_grid_fixed_fit_content_smaller__border_box")
    void blockgridBlockInGridFixedFitContentSmallerBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fitContent(LengthPercentage.length(10.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(20.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(20.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_block_in_grid_fixed_fit_content_smaller__content_box")
    void blockgridBlockInGridFixedFitContentSmallerContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fitContent(LengthPercentage.length(10.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(20.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(20.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_block_in_grid_fixed_larger__border_box")
    void blockgridBlockInGridFixedLargerBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(50.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_block_in_grid_fixed_larger__content_box")
    void blockgridBlockInGridFixedLargerContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(50.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_block_in_grid_fixed_middle__border_box")
    void blockgridBlockInGridFixedMiddleBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(30.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(30.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(30.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(30.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_block_in_grid_fixed_middle__content_box")
    void blockgridBlockInGridFixedMiddleContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(30.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(30.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(30.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(30.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_block_in_grid_fixed_smaller__border_box")
    void blockgridBlockInGridFixedSmallerBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(10.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(10.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(10.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(10.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_block_in_grid_fixed_smaller__content_box")
    void blockgridBlockInGridFixedSmallerContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(10.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(10.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(10.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(10.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_block_in_grid_fr__border_box")
    void blockgridBlockInGridFrBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fr(1.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(40.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(40.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_block_in_grid_fr__content_box")
    void blockgridBlockInGridFrContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fr(1.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(40.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(40.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_block_in_grid_max_content__border_box")
    void blockgridBlockInGridMaxContentBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.minContent());
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(20.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(20.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_block_in_grid_max_content__content_box")
    void blockgridBlockInGridMaxContentContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.minContent());
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(20.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(20.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_block_in_grid_min_content__border_box")
    void blockgridBlockInGridMinContentBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.minContent());
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(20.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(20.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_block_in_grid_min_content__content_box")
    void blockgridBlockInGridMinContentContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.minContent());
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(20.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(20.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_grid_in_block__border_box")
    void blockgridGridInBlockBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node00Style = new TaffyStyle();
        node00Style.direction = TaffyDirection.LTR;
        node00Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.GRID;
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(20.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(200.0f), TaffyDimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(70.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(200.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(50.0f, node00Layout.size().width, "width of node00");
        assertEquals(50.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(20.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_grid_in_block__content_box")
    void blockgridGridInBlockContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node00Style = new TaffyStyle();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.direction = TaffyDirection.LTR;
        node00Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.GRID;
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.BLOCK;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(20.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(200.0f), TaffyDimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(70.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(200.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(50.0f, node00Layout.size().width, "width of node00");
        assertEquals(50.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(20.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("blockgrid_margin_y_collapse_through_blocked_by_grid__border_box")
    void blockgridMarginYCollapseThroughBlockedByGridBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        node0Style.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(10.0f));
        node0Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.GRID;
        node1Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.LTR;
        node2Style.display = TaffyDisplay.BLOCK;
        node2Style.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(10.0f));
        node2Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(40.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(30.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("blockgrid_margin_y_collapse_through_blocked_by_grid__content_box")
    void blockgridMarginYCollapseThroughBlockedByGridContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.BLOCK;
        node0Style.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(10.0f));
        node0Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.LTR;
        node1Style.display = TaffyDisplay.GRID;
        node1Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.LTR;
        node2Style.display = TaffyDisplay.BLOCK;
        node2Style.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(10.0f));
        node2Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(40.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(30.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("blockgrid_margin_y_first_child_collapse_blocked_by_grid__border_box")
    void blockgridMarginYFirstChildCollapseBlockedByGridBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node000Style = new TaffyStyle();
        node000Style.direction = TaffyDirection.LTR;
        node000Style.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        TaffyStyle node00Style = new TaffyStyle();
        node00Style.direction = TaffyDirection.LTR;
        node00Style.display = TaffyDisplay.BLOCK;
        node00Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.GRID;
        node0Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(50.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(10.0f, node00Layout.location().y, "y of node00");
        Layout node000Layout = tree.getLayout(node000);
        assertEquals(50.0f, node000Layout.size().width, "width of node000");
        assertEquals(10.0f, node000Layout.size().height, "height of node000");
        assertEquals(0.0f, node000Layout.location().x, "x of node000");
        assertEquals(0.0f, node000Layout.location().y, "y of node000");
    }

    @Test
    @DisplayName("blockgrid_margin_y_first_child_collapse_blocked_by_grid__content_box")
    void blockgridMarginYFirstChildCollapseBlockedByGridContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node000Style = new TaffyStyle();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.direction = TaffyDirection.LTR;
        node000Style.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        TaffyStyle node00Style = new TaffyStyle();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.direction = TaffyDirection.LTR;
        node00Style.display = TaffyDisplay.BLOCK;
        node00Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.GRID;
        node0Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(50.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(10.0f, node00Layout.location().y, "y of node00");
        Layout node000Layout = tree.getLayout(node000);
        assertEquals(50.0f, node000Layout.size().width, "width of node000");
        assertEquals(10.0f, node000Layout.size().height, "height of node000");
        assertEquals(0.0f, node000Layout.location().x, "x of node000");
        assertEquals(0.0f, node000Layout.location().y, "y of node000");
    }

    @Test
    @DisplayName("blockgrid_margin_y_last_child_collapse_blocked_by_grid__border_box")
    void blockgridMarginYLastChildCollapseBlockedByGridBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node000Style = new TaffyStyle();
        node000Style.direction = TaffyDirection.LTR;
        node000Style.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        TaffyStyle node00Style = new TaffyStyle();
        node00Style.direction = TaffyDirection.LTR;
        node00Style.display = TaffyDisplay.BLOCK;
        node00Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.GRID;
        node0Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(50.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node000Layout = tree.getLayout(node000);
        assertEquals(50.0f, node000Layout.size().width, "width of node000");
        assertEquals(10.0f, node000Layout.size().height, "height of node000");
        assertEquals(0.0f, node000Layout.location().x, "x of node000");
        assertEquals(0.0f, node000Layout.location().y, "y of node000");
    }

    @Test
    @DisplayName("blockgrid_margin_y_last_child_collapse_blocked_by_grid__content_box")
    void blockgridMarginYLastChildCollapseBlockedByGridContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node000Style = new TaffyStyle();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.direction = TaffyDirection.LTR;
        node000Style.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        TaffyStyle node00Style = new TaffyStyle();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.direction = TaffyDirection.LTR;
        node00Style.display = TaffyDisplay.BLOCK;
        node00Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.LTR;
        node0Style.display = TaffyDisplay.GRID;
        node0Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.LTR;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(50.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node000Layout = tree.getLayout(node000);
        assertEquals(50.0f, node000Layout.size().width, "width of node000");
        assertEquals(10.0f, node000Layout.size().height, "height of node000");
        assertEquals(0.0f, node000Layout.location().x, "x of node000");
        assertEquals(0.0f, node000Layout.location().y, "y of node000");
    }

}
