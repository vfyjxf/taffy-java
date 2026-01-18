package dev.vfyjxf.taffy.generated.block;

import dev.vfyjxf.taffy.geometry.*;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.tree.*;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Generated tests for block layout fixtures.
 */
public class BlockTest {

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
    @DisplayName("absolute_correct_cross_child_size_with_percentage__border_box")
    void absoluteCorrectCrossChildSizeWithPercentageBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.size = new Size<>(Dimension.length(200.0f), Dimension.length(10.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node01Style = new Style();
        node01Style.size = new Size<>(Dimension.percent(1.0f), Dimension.length(10.0f));
        NodeId node01 = tree.newLeaf(node01Style);

        Style node020Style = new Style();
        node020Style.size = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        NodeId node020 = tree.newLeaf(node020Style);

        Style node02Style = new Style();
        node02Style.size = new Size<>(Dimension.percent(1.0f), Dimension.length(10.0f));
        NodeId node02 = tree.newWithChildren(node02Style, node020);

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.flexDirection = FlexDirection.COLUMN;
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(50.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(40.0f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newWithChildren(node0Style, node00, node01, node02);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(300.0f), Dimension.length(110.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(110.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(200.0f, node0Layout.size().width, "width of node0");
        assertEquals(30.0f, node0Layout.size().height, "height of node0");
        assertEquals(50.0f, node0Layout.location().x, "x of node0");
        assertEquals(40.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(200.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node01Layout = tree.getLayout(node01);
        assertEquals(200.0f, node01Layout.size().width, "width of node01");
        assertEquals(10.0f, node01Layout.size().height, "height of node01");
        assertEquals(0.0f, node01Layout.location().x, "x of node01");
        assertEquals(10.0f, node01Layout.location().y, "y of node01");
        Layout node02Layout = tree.getLayout(node02);
        assertEquals(200.0f, node02Layout.size().width, "width of node02");
        assertEquals(10.0f, node02Layout.size().height, "height of node02");
        assertEquals(0.0f, node02Layout.location().x, "x of node02");
        assertEquals(20.0f, node02Layout.location().y, "y of node02");
        Layout node020Layout = tree.getLayout(node020);
        assertEquals(10.0f, node020Layout.size().width, "width of node020");
        assertEquals(10.0f, node020Layout.size().height, "height of node020");
        assertEquals(0.0f, node020Layout.location().x, "x of node020");
        assertEquals(0.0f, node020Layout.location().y, "y of node020");
    }

    @Test
    @DisplayName("absolute_correct_cross_child_size_with_percentage__content_box")
    void absoluteCorrectCrossChildSizeWithPercentageContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.size = new Size<>(Dimension.length(200.0f), Dimension.length(10.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node01Style = new Style();
        node01Style.boxSizing = BoxSizing.CONTENT_BOX;
        node01Style.size = new Size<>(Dimension.percent(1.0f), Dimension.length(10.0f));
        NodeId node01 = tree.newLeaf(node01Style);

        Style node020Style = new Style();
        node020Style.boxSizing = BoxSizing.CONTENT_BOX;
        node020Style.size = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        NodeId node020 = tree.newLeaf(node020Style);

        Style node02Style = new Style();
        node02Style.boxSizing = BoxSizing.CONTENT_BOX;
        node02Style.size = new Size<>(Dimension.percent(1.0f), Dimension.length(10.0f));
        NodeId node02 = tree.newWithChildren(node02Style, node020);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.flexDirection = FlexDirection.COLUMN;
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(50.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(40.0f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newWithChildren(node0Style, node00, node01, node02);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(300.0f), Dimension.length(110.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(110.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(200.0f, node0Layout.size().width, "width of node0");
        assertEquals(30.0f, node0Layout.size().height, "height of node0");
        assertEquals(50.0f, node0Layout.location().x, "x of node0");
        assertEquals(40.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(200.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node01Layout = tree.getLayout(node01);
        assertEquals(200.0f, node01Layout.size().width, "width of node01");
        assertEquals(10.0f, node01Layout.size().height, "height of node01");
        assertEquals(0.0f, node01Layout.location().x, "x of node01");
        assertEquals(10.0f, node01Layout.location().y, "y of node01");
        Layout node02Layout = tree.getLayout(node02);
        assertEquals(200.0f, node02Layout.size().width, "width of node02");
        assertEquals(10.0f, node02Layout.size().height, "height of node02");
        assertEquals(0.0f, node02Layout.location().x, "x of node02");
        assertEquals(20.0f, node02Layout.location().y, "y of node02");
        Layout node020Layout = tree.getLayout(node020);
        assertEquals(10.0f, node020Layout.size().width, "width of node020");
        assertEquals(10.0f, node020Layout.size().height, "height of node020");
        assertEquals(0.0f, node020Layout.location().x, "x of node020");
        assertEquals(0.0f, node020Layout.location().y, "y of node020");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_aspect_ratio_overrides_height_of_full_inset__border_box")
    void blockAbsoluteAspectRatioAspectRatioOverridesHeightOfFullInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.aspectRatio = 3.0f;
        node0Style.inset = new Rect<>(LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.percent(0.05f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(360.0f, node0Layout.size().width, "width of node0");
        assertEquals(120.0f, node0Layout.size().height, "height of node0");
        assertEquals(20.0f, node0Layout.location().x, "x of node0");
        assertEquals(15.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_aspect_ratio_overrides_height_of_full_inset__content_box")
    void blockAbsoluteAspectRatioAspectRatioOverridesHeightOfFullInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.aspectRatio = 3.0f;
        node0Style.inset = new Rect<>(LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.percent(0.05f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(360.0f, node0Layout.size().width, "width of node0");
        assertEquals(120.0f, node0Layout.size().height, "height of node0");
        assertEquals(20.0f, node0Layout.location().x, "x of node0");
        assertEquals(15.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_fill_height__border_box")
    void blockAbsoluteAspectRatioFillHeightBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.percent(0.5f), Dimension.AUTO);
        node0Style.aspectRatio = 3.0f;
        node0Style.inset = new Rect<>(LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(200.0f, node0Layout.size().width, "width of node0");
        assertEquals(67.0f, node0Layout.size().height, "height of node0");
        assertEquals(20.0f, node0Layout.location().x, "x of node0");
        assertEquals(15.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_fill_height__content_box")
    void blockAbsoluteAspectRatioFillHeightContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.percent(0.5f), Dimension.AUTO);
        node0Style.aspectRatio = 3.0f;
        node0Style.inset = new Rect<>(LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(200.0f, node0Layout.size().width, "width of node0");
        assertEquals(67.0f, node0Layout.size().height, "height of node0");
        assertEquals(20.0f, node0Layout.location().x, "x of node0");
        assertEquals(15.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_fill_height_from_inset__border_box")
    void blockAbsoluteAspectRatioFillHeightFromInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.aspectRatio = 3.0f;
        node0Style.inset = new Rect<>(LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(320.0f, node0Layout.size().width, "width of node0");
        assertEquals(107.0f, node0Layout.size().height, "height of node0");
        assertEquals(40.0f, node0Layout.location().x, "x of node0");
        assertEquals(15.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_fill_height_from_inset__content_box")
    void blockAbsoluteAspectRatioFillHeightFromInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.aspectRatio = 3.0f;
        node0Style.inset = new Rect<>(LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(320.0f, node0Layout.size().width, "width of node0");
        assertEquals(107.0f, node0Layout.size().height, "height of node0");
        assertEquals(40.0f, node0Layout.location().x, "x of node0");
        assertEquals(15.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_fill_max_height__border_box")
    void blockAbsoluteAspectRatioFillMaxHeightBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.maxSize = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        node0Style.aspectRatio = 3.0f;
        MeasureFunc node0Measure = ahemTextMeasure("HHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(17.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_fill_max_height__content_box")
    void blockAbsoluteAspectRatioFillMaxHeightContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.maxSize = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        node0Style.aspectRatio = 3.0f;
        MeasureFunc node0Measure = ahemTextMeasure("HHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(17.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_fill_max_width__border_box")
    void blockAbsoluteAspectRatioFillMaxWidthBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.maxSize = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node0Style.aspectRatio = 0.5f;
        MeasureFunc node0Measure = ahemTextMeasure("HHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(25.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_fill_max_width__content_box")
    void blockAbsoluteAspectRatioFillMaxWidthContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.maxSize = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node0Style.aspectRatio = 0.5f;
        MeasureFunc node0Measure = ahemTextMeasure("HHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH\u200BHHHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(25.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_fill_min_height__border_box")
    void blockAbsoluteAspectRatioFillMinHeightBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.minSize = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        node0Style.aspectRatio = 3.0f;
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(17.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_fill_min_height__content_box")
    void blockAbsoluteAspectRatioFillMinHeightContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.minSize = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        node0Style.aspectRatio = 3.0f;
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(17.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_fill_min_width__border_box")
    void blockAbsoluteAspectRatioFillMinWidthBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.minSize = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node0Style.aspectRatio = 0.5f;
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(25.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_fill_min_width__content_box")
    void blockAbsoluteAspectRatioFillMinWidthContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.minSize = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node0Style.aspectRatio = 0.5f;
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(25.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_fill_width__border_box")
    void blockAbsoluteAspectRatioFillWidthBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.percent(0.2f));
        node0Style.aspectRatio = 3.0f;
        node0Style.inset = new Rect<>(LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(180.0f, node0Layout.size().width, "width of node0");
        assertEquals(60.0f, node0Layout.size().height, "height of node0");
        assertEquals(20.0f, node0Layout.location().x, "x of node0");
        assertEquals(15.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_fill_width__content_box")
    void blockAbsoluteAspectRatioFillWidthContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.percent(0.2f));
        node0Style.aspectRatio = 3.0f;
        node0Style.inset = new Rect<>(LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(180.0f, node0Layout.size().width, "width of node0");
        assertEquals(60.0f, node0Layout.size().height, "height of node0");
        assertEquals(20.0f, node0Layout.location().x, "x of node0");
        assertEquals(15.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_fill_width_from_inset__border_box")
    void blockAbsoluteAspectRatioFillWidthFromInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.aspectRatio = 3.0f;
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.3f), LengthPercentageAuto.percent(0.5f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(180.0f, node0Layout.size().width, "width of node0");
        assertEquals(60.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(90.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_fill_width_from_inset__content_box")
    void blockAbsoluteAspectRatioFillWidthFromInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.aspectRatio = 3.0f;
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.3f), LengthPercentageAuto.percent(0.5f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(180.0f, node0Layout.size().width, "width of node0");
        assertEquals(60.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(90.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_height_overrides_inset__border_box")
    void blockAbsoluteAspectRatioHeightOverridesInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.percent(0.1f));
        node0Style.aspectRatio = 3.0f;
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.3f), LengthPercentageAuto.percent(0.5f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(90.0f, node0Layout.size().width, "width of node0");
        assertEquals(30.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(90.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_height_overrides_inset__content_box")
    void blockAbsoluteAspectRatioHeightOverridesInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.percent(0.1f));
        node0Style.aspectRatio = 3.0f;
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.3f), LengthPercentageAuto.percent(0.5f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(90.0f, node0Layout.size().width, "width of node0");
        assertEquals(30.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(90.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_width_overrides_inset__border_box")
    void blockAbsoluteAspectRatioWidthOverridesInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.percent(0.4f), Dimension.AUTO);
        node0Style.aspectRatio = 3.0f;
        node0Style.inset = new Rect<>(LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(160.0f, node0Layout.size().width, "width of node0");
        assertEquals(53.0f, node0Layout.size().height, "height of node0");
        assertEquals(40.0f, node0Layout.location().x, "x of node0");
        assertEquals(15.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_aspect_ratio_width_overrides_inset__content_box")
    void blockAbsoluteAspectRatioWidthOverridesInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.percent(0.4f), Dimension.AUTO);
        node0Style.aspectRatio = 3.0f;
        node0Style.inset = new Rect<>(LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(400.0f), Dimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(160.0f, node0Layout.size().width, "width of node0");
        assertEquals(53.0f, node0Layout.size().height, "height of node0");
        assertEquals(40.0f, node0Layout.location().x, "x of node0");
        assertEquals(15.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_child_with_margin_x__border_box")
    void blockAbsoluteChildWithMarginXBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(9.0f), Dimension.length(9.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.length(7.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.position = Position.ABSOLUTE;
        node1Style.size = new Size<>(Dimension.length(9.0f), Dimension.length(9.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.length(7.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.position = Position.ABSOLUTE;
        node2Style.size = new Size<>(Dimension.length(9.0f), Dimension.length(9.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(5.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(20.0f), Dimension.length(37.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(20.0f, nodeLayout.size().width, "width of node");
        assertEquals(37.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(9.0f, node0Layout.size().width, "width of node0");
        assertEquals(9.0f, node0Layout.size().height, "height of node0");
        assertEquals(7.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(9.0f, node1Layout.size().width, "width of node1");
        assertEquals(9.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(9.0f, node2Layout.size().width, "width of node2");
        assertEquals(9.0f, node2Layout.size().height, "height of node2");
        assertEquals(10.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_absolute_child_with_margin_x__content_box")
    void blockAbsoluteChildWithMarginXContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(9.0f), Dimension.length(9.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.length(7.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.position = Position.ABSOLUTE;
        node1Style.size = new Size<>(Dimension.length(9.0f), Dimension.length(9.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.length(7.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.position = Position.ABSOLUTE;
        node2Style.size = new Size<>(Dimension.length(9.0f), Dimension.length(9.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(5.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(20.0f), Dimension.length(37.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(20.0f, nodeLayout.size().width, "width of node");
        assertEquals(37.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(9.0f, node0Layout.size().width, "width of node0");
        assertEquals(9.0f, node0Layout.size().height, "height of node0");
        assertEquals(7.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(9.0f, node1Layout.size().width, "width of node1");
        assertEquals(9.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(9.0f, node2Layout.size().width, "width of node2");
        assertEquals(9.0f, node2Layout.size().height, "height of node2");
        assertEquals(10.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_absolute_child_with_margin_y__border_box")
    void blockAbsoluteChildWithMarginYBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(9.0f), Dimension.length(9.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(7.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.position = Position.ABSOLUTE;
        node1Style.size = new Size<>(Dimension.length(9.0f), Dimension.length(9.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(7.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.position = Position.ABSOLUTE;
        node2Style.size = new Size<>(Dimension.length(9.0f), Dimension.length(9.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(5.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(20.0f), Dimension.length(37.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(20.0f, nodeLayout.size().width, "width of node");
        assertEquals(37.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(9.0f, node0Layout.size().width, "width of node0");
        assertEquals(9.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(7.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(9.0f, node1Layout.size().width, "width of node1");
        assertEquals(9.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(9.0f, node2Layout.size().width, "width of node2");
        assertEquals(9.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(10.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_absolute_child_with_margin_y__content_box")
    void blockAbsoluteChildWithMarginYContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(9.0f), Dimension.length(9.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(7.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.position = Position.ABSOLUTE;
        node1Style.size = new Size<>(Dimension.length(9.0f), Dimension.length(9.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(7.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.position = Position.ABSOLUTE;
        node2Style.size = new Size<>(Dimension.length(9.0f), Dimension.length(9.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(5.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(20.0f), Dimension.length(37.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(20.0f, nodeLayout.size().width, "width of node");
        assertEquals(37.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(9.0f, node0Layout.size().width, "width of node0");
        assertEquals(9.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(7.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(9.0f, node1Layout.size().width, "width of node1");
        assertEquals(9.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(9.0f, node2Layout.size().width, "width of node2");
        assertEquals(9.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(10.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_absolute_child_with_max_height__border_box")
    void blockAbsoluteChildWithMaxHeightBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(150.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.maxSize = new Size<>(Dimension.AUTO, Dimension.length(100.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.length(20.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(80.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(100.0f, node00Layout.size().width, "width of node00");
        assertEquals(150.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_absolute_child_with_max_height__content_box")
    void blockAbsoluteChildWithMaxHeightContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(150.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.maxSize = new Size<>(Dimension.AUTO, Dimension.length(100.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.length(20.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(80.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(100.0f, node00Layout.size().width, "width of node00");
        assertEquals(150.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_absolute_layout_child_order__border_box")
    void blockAbsoluteLayoutChildOrderBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(60.0f), Dimension.length(40.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.position = Position.ABSOLUTE;
        node1Style.size = new Size<>(Dimension.length(60.0f), Dimension.length(40.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.length(60.0f), Dimension.length(40.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.justifyContent = AlignContent.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(110.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(110.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(60.0f, node0Layout.size().width, "width of node0");
        assertEquals(40.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(60.0f, node1Layout.size().width, "width of node1");
        assertEquals(40.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(40.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(60.0f, node2Layout.size().width, "width of node2");
        assertEquals(40.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(40.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_absolute_layout_child_order__content_box")
    void blockAbsoluteLayoutChildOrderContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(60.0f), Dimension.length(40.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.position = Position.ABSOLUTE;
        node1Style.size = new Size<>(Dimension.length(60.0f), Dimension.length(40.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.length(60.0f), Dimension.length(40.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.justifyContent = AlignContent.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(110.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(110.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(60.0f, node0Layout.size().width, "width of node0");
        assertEquals(40.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(60.0f, node1Layout.size().width, "width of node1");
        assertEquals(40.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(40.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(60.0f, node2Layout.size().width, "width of node2");
        assertEquals(40.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(40.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_absolute_layout_no_size__border_box")
    void blockAbsoluteLayoutNoSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(0.0f, node0Layout.size().width, "width of node0");
        assertEquals(0.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_layout_no_size__content_box")
    void blockAbsoluteLayoutNoSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(0.0f, node0Layout.size().width, "width of node0");
        assertEquals(0.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_layout_percentage_bottom_based_on_parent_height__border_box")
    void blockAbsoluteLayoutPercentageBottomBasedOnParentHeightBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.5f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.position = Position.ABSOLUTE;
        node1Style.size = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        node1Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.5f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.position = Position.ABSOLUTE;
        node2Style.size = new Size<>(Dimension.length(10.0f), Dimension.AUTO);
        node2Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(10.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(100.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(10.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(90.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(10.0f, node2Layout.size().width, "width of node2");
        assertEquals(160.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(20.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_absolute_layout_percentage_bottom_based_on_parent_height__content_box")
    void blockAbsoluteLayoutPercentageBottomBasedOnParentHeightContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.5f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.position = Position.ABSOLUTE;
        node1Style.size = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        node1Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.5f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.position = Position.ABSOLUTE;
        node2Style.size = new Size<>(Dimension.length(10.0f), Dimension.AUTO);
        node2Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(10.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(100.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(10.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(90.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(10.0f, node2Layout.size().width, "width of node2");
        assertEquals(160.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(20.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_absolute_layout_percentage_height__border_box")
    void blockAbsoluteLayoutPercentageHeightBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(10.0f), Dimension.percent(0.5f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(10.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_layout_percentage_height__content_box")
    void blockAbsoluteLayoutPercentageHeightContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(10.0f), Dimension.percent(0.5f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(10.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_layout_row_width_height_end_bottom__border_box")
    void blockAbsoluteLayoutRowWidthHeightEndBottomBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(10.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(80.0f, node0Layout.location().x, "x of node0");
        assertEquals(80.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_layout_row_width_height_end_bottom__content_box")
    void blockAbsoluteLayoutRowWidthHeightEndBottomContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(10.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(80.0f, node0Layout.location().x, "x of node0");
        assertEquals(80.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_layout_start_top_end_bottom__border_box")
    void blockAbsoluteLayoutStartTopEndBottomBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(80.0f, node0Layout.size().width, "width of node0");
        assertEquals(80.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_layout_start_top_end_bottom__content_box")
    void blockAbsoluteLayoutStartTopEndBottomContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(80.0f, node0Layout.size().width, "width of node0");
        assertEquals(80.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_layout_width_height_end_bottom__border_box")
    void blockAbsoluteLayoutWidthHeightEndBottomBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(10.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(80.0f, node0Layout.location().x, "x of node0");
        assertEquals(80.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_layout_width_height_end_bottom__content_box")
    void blockAbsoluteLayoutWidthHeightEndBottomContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(10.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(80.0f, node0Layout.location().x, "x of node0");
        assertEquals(80.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_layout_width_height_start_top__border_box")
    void blockAbsoluteLayoutWidthHeightStartTopBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(10.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_layout_width_height_start_top__content_box")
    void blockAbsoluteLayoutWidthHeightStartTopContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(10.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_layout_width_height_start_top_end_bottom__border_box")
    void blockAbsoluteLayoutWidthHeightStartTopEndBottomBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(10.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_layout_width_height_start_top_end_bottom__content_box")
    void blockAbsoluteLayoutWidthHeightStartTopEndBottomContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(10.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_layout_within_border__border_box")
    void blockAbsoluteLayoutWithinBorderBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.position = Position.ABSOLUTE;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node1Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.position = Position.ABSOLUTE;
        node2Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        node2Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style node3Style = new Style();
        node3Style.display = Display.BLOCK;
        node3Style.position = Position.ABSOLUTE;
        node3Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node3Style.margin = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        node3Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f));
        NodeId node3 = tree.newLeaf(node3Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        nodeStyle.padding = new Rect<>(LengthPercentage.length(10.0f), LengthPercentage.length(10.0f), LengthPercentage.length(10.0f), LengthPercentage.length(10.0f));
        nodeStyle.border = new Rect<>(LengthPercentage.length(10.0f), LengthPercentage.length(10.0f), LengthPercentage.length(10.0f), LengthPercentage.length(10.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(40.0f, node1Layout.location().x, "x of node1");
        assertEquals(40.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(20.0f, node2Layout.location().x, "x of node2");
        assertEquals(20.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(50.0f, node3Layout.size().height, "height of node3");
        assertEquals(30.0f, node3Layout.location().x, "x of node3");
        assertEquals(30.0f, node3Layout.location().y, "y of node3");
    }

    @Test
    @DisplayName("block_absolute_layout_within_border__content_box")
    void blockAbsoluteLayoutWithinBorderContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.position = Position.ABSOLUTE;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node1Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.position = Position.ABSOLUTE;
        node2Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        node2Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style node3Style = new Style();
        node3Style.boxSizing = BoxSizing.CONTENT_BOX;
        node3Style.display = Display.BLOCK;
        node3Style.position = Position.ABSOLUTE;
        node3Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node3Style.margin = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        node3Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f));
        NodeId node3 = tree.newLeaf(node3Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        nodeStyle.padding = new Rect<>(LengthPercentage.length(10.0f), LengthPercentage.length(10.0f), LengthPercentage.length(10.0f), LengthPercentage.length(10.0f));
        nodeStyle.border = new Rect<>(LengthPercentage.length(10.0f), LengthPercentage.length(10.0f), LengthPercentage.length(10.0f), LengthPercentage.length(10.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(140.0f, nodeLayout.size().width, "width of node");
        assertEquals(140.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(80.0f, node1Layout.location().x, "x of node1");
        assertEquals(80.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(20.0f, node2Layout.location().x, "x of node2");
        assertEquals(20.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(50.0f, node3Layout.size().height, "height of node3");
        assertEquals(70.0f, node3Layout.location().x, "x of node3");
        assertEquals(70.0f, node3Layout.location().y, "y of node3");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_bottom_and_top_with_inset__border_box")
    void blockAbsoluteMarginAutoBottomAndTopWithInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(70.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_bottom_and_top_with_inset__content_box")
    void blockAbsoluteMarginAutoBottomAndTopWithInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(70.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_bottom_and_top_without_inset__border_box")
    void blockAbsoluteMarginAutoBottomAndTopWithoutInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_bottom_and_top_without_inset__content_box")
    void blockAbsoluteMarginAutoBottomAndTopWithoutInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_bottom_with_inset__border_box")
    void blockAbsoluteMarginAutoBottomWithInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_bottom_with_inset__content_box")
    void blockAbsoluteMarginAutoBottomWithInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_bottom_without_inset__border_box")
    void blockAbsoluteMarginAutoBottomWithoutInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_bottom_without_inset__content_box")
    void blockAbsoluteMarginAutoBottomWithoutInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_and_right_with_inset__border_box")
    void blockAbsoluteMarginAutoLeftAndRightWithInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(70.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_and_right_with_inset__content_box")
    void blockAbsoluteMarginAutoLeftAndRightWithInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(70.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_and_right_without_inset__border_box")
    void blockAbsoluteMarginAutoLeftAndRightWithoutInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_and_right_without_inset__content_box")
    void blockAbsoluteMarginAutoLeftAndRightWithoutInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_child_bigger_than_parent_with_inset__border_box")
    void blockAbsoluteMarginAutoLeftChildBiggerThanParentWithInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(72.0f), Dimension.length(72.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(52.0f), Dimension.length(52.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(52.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(72.0f, node0Layout.size().width, "width of node0");
        assertEquals(72.0f, node0Layout.size().height, "height of node0");
        assertEquals(-40.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_child_bigger_than_parent_with_inset__content_box")
    void blockAbsoluteMarginAutoLeftChildBiggerThanParentWithInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(72.0f), Dimension.length(72.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(52.0f), Dimension.length(52.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(52.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(72.0f, node0Layout.size().width, "width of node0");
        assertEquals(72.0f, node0Layout.size().height, "height of node0");
        assertEquals(-40.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_child_bigger_than_parent_without_inset__border_box")
    void blockAbsoluteMarginAutoLeftChildBiggerThanParentWithoutInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(72.0f), Dimension.length(72.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(52.0f), Dimension.length(52.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(52.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(72.0f, node0Layout.size().width, "width of node0");
        assertEquals(72.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_child_bigger_than_parent_without_inset__content_box")
    void blockAbsoluteMarginAutoLeftChildBiggerThanParentWithoutInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(72.0f), Dimension.length(72.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(52.0f), Dimension.length(52.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(52.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(72.0f, node0Layout.size().width, "width of node0");
        assertEquals(72.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_fix_right_child_bigger_than_parent_with_inset__border_box")
    void blockAbsoluteMarginAutoLeftFixRightChildBiggerThanParentWithInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(72.0f), Dimension.length(72.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(52.0f), Dimension.length(52.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(52.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(72.0f, node0Layout.size().width, "width of node0");
        assertEquals(72.0f, node0Layout.size().height, "height of node0");
        assertEquals(-50.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_fix_right_child_bigger_than_parent_with_inset__content_box")
    void blockAbsoluteMarginAutoLeftFixRightChildBiggerThanParentWithInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(72.0f), Dimension.length(72.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(52.0f), Dimension.length(52.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(52.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(72.0f, node0Layout.size().width, "width of node0");
        assertEquals(72.0f, node0Layout.size().height, "height of node0");
        assertEquals(-50.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_fix_right_child_bigger_than_parent_without_inset__border_box")
    void blockAbsoluteMarginAutoLeftFixRightChildBiggerThanParentWithoutInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(72.0f), Dimension.length(72.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(52.0f), Dimension.length(52.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(52.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(72.0f, node0Layout.size().width, "width of node0");
        assertEquals(72.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_fix_right_child_bigger_than_parent_without_inset__content_box")
    void blockAbsoluteMarginAutoLeftFixRightChildBiggerThanParentWithoutInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(72.0f), Dimension.length(72.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(52.0f), Dimension.length(52.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(52.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(72.0f, node0Layout.size().width, "width of node0");
        assertEquals(72.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_right_child_bigger_than_parent_with_inset__border_box")
    void blockAbsoluteMarginAutoLeftRightChildBiggerThanParentWithInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(72.0f), Dimension.length(72.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(52.0f), Dimension.length(52.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(52.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(72.0f, node0Layout.size().width, "width of node0");
        assertEquals(72.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_right_child_bigger_than_parent_with_inset__content_box")
    void blockAbsoluteMarginAutoLeftRightChildBiggerThanParentWithInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(72.0f), Dimension.length(72.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(52.0f), Dimension.length(52.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(52.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(72.0f, node0Layout.size().width, "width of node0");
        assertEquals(72.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_right_child_bigger_than_parent_without_inset__border_box")
    void blockAbsoluteMarginAutoLeftRightChildBiggerThanParentWithoutInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(72.0f), Dimension.length(72.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(52.0f), Dimension.length(52.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(52.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(72.0f, node0Layout.size().width, "width of node0");
        assertEquals(72.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_right_child_bigger_than_parent_without_inset__content_box")
    void blockAbsoluteMarginAutoLeftRightChildBiggerThanParentWithoutInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(72.0f), Dimension.length(72.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(52.0f), Dimension.length(52.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(52.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(72.0f, node0Layout.size().width, "width of node0");
        assertEquals(72.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_with_inset__border_box")
    void blockAbsoluteMarginAutoLeftWithInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(130.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_with_inset__content_box")
    void blockAbsoluteMarginAutoLeftWithInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(130.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_without_inset__border_box")
    void blockAbsoluteMarginAutoLeftWithoutInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_left_without_inset__content_box")
    void blockAbsoluteMarginAutoLeftWithoutInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_multiple_children_with_inset__border_box")
    void blockAbsoluteMarginAutoMultipleChildrenWithInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.position = Position.ABSOLUTE;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        node1Style.inset = new Rect<>(LengthPercentageAuto.length(20.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(20.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_multiple_children_with_inset__content_box")
    void blockAbsoluteMarginAutoMultipleChildrenWithInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.position = Position.ABSOLUTE;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        node1Style.inset = new Rect<>(LengthPercentageAuto.length(20.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(20.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_multiple_children_without_inset__border_box")
    void blockAbsoluteMarginAutoMultipleChildrenWithoutInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.position = Position.ABSOLUTE;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_multiple_children_without_inset__content_box")
    void blockAbsoluteMarginAutoMultipleChildrenWithoutInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.position = Position.ABSOLUTE;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_right_with_inset__border_box")
    void blockAbsoluteMarginAutoRightWithInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_right_with_inset__content_box")
    void blockAbsoluteMarginAutoRightWithInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_right_without_inset__border_box")
    void blockAbsoluteMarginAutoRightWithoutInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_right_without_inset__content_box")
    void blockAbsoluteMarginAutoRightWithoutInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_top_with_inset__border_box")
    void blockAbsoluteMarginAutoTopWithInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.length(20.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(140.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_top_with_inset__content_box")
    void blockAbsoluteMarginAutoTopWithInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO);
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.length(20.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(140.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_top_without_inset__border_box")
    void blockAbsoluteMarginAutoTopWithoutInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_auto_top_without_inset__content_box")
    void blockAbsoluteMarginAutoTopWithoutInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_bottom_left_with_inset__border_box")
    void blockAbsoluteMarginBottomLeftWithInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(20.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(10.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(30.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_bottom_left_with_inset__content_box")
    void blockAbsoluteMarginBottomLeftWithInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(20.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(20.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(10.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(30.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_bottom_left_without_inset__border_box")
    void blockAbsoluteMarginBottomLeftWithoutInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(10.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_margin_bottom_left_without_inset__content_box")
    void blockAbsoluteMarginBottomLeftWithoutInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(10.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_minmax_bottom_right_max__border_box")
    void blockAbsoluteMinmaxBottomRightMaxBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        node0Style.maxSize = new Size<>(Dimension.length(40.0f), Dimension.length(30.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(30.0f, node0Layout.size().height, "height of node0");
        assertEquals(50.0f, node0Layout.location().x, "x of node0");
        assertEquals(60.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_minmax_bottom_right_max__content_box")
    void blockAbsoluteMinmaxBottomRightMaxContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        node0Style.maxSize = new Size<>(Dimension.length(40.0f), Dimension.length(30.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(30.0f, node0Layout.size().height, "height of node0");
        assertEquals(50.0f, node0Layout.location().x, "x of node0");
        assertEquals(60.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_minmax_bottom_right_min_max__border_box")
    void blockAbsoluteMinmaxBottomRightMinMaxBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.minSize = new Size<>(Dimension.length(50.0f), Dimension.length(60.0f));
        node0Style.maxSize = new Size<>(Dimension.length(40.0f), Dimension.length(30.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(60.0f, node0Layout.size().height, "height of node0");
        assertEquals(40.0f, node0Layout.location().x, "x of node0");
        assertEquals(30.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_minmax_bottom_right_min_max__content_box")
    void blockAbsoluteMinmaxBottomRightMinMaxContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.minSize = new Size<>(Dimension.length(50.0f), Dimension.length(60.0f));
        node0Style.maxSize = new Size<>(Dimension.length(40.0f), Dimension.length(30.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(60.0f, node0Layout.size().height, "height of node0");
        assertEquals(40.0f, node0Layout.location().x, "x of node0");
        assertEquals(30.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_minmax_bottom_right_min_max_preferred__border_box")
    void blockAbsoluteMinmaxBottomRightMinMaxPreferredBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        node0Style.minSize = new Size<>(Dimension.length(50.0f), Dimension.length(60.0f));
        node0Style.maxSize = new Size<>(Dimension.length(40.0f), Dimension.length(30.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(60.0f, node0Layout.size().height, "height of node0");
        assertEquals(40.0f, node0Layout.location().x, "x of node0");
        assertEquals(30.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_minmax_bottom_right_min_max_preferred__content_box")
    void blockAbsoluteMinmaxBottomRightMinMaxPreferredContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        node0Style.minSize = new Size<>(Dimension.length(50.0f), Dimension.length(60.0f));
        node0Style.maxSize = new Size<>(Dimension.length(40.0f), Dimension.length(30.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(60.0f, node0Layout.size().height, "height of node0");
        assertEquals(40.0f, node0Layout.location().x, "x of node0");
        assertEquals(30.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_minmax_top_left_bottom_right_max__border_box")
    void blockAbsoluteMinmaxTopLeftBottomRightMaxBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.maxSize = new Size<>(Dimension.length(40.0f), Dimension.length(30.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(30.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_minmax_top_left_bottom_right_max__content_box")
    void blockAbsoluteMinmaxTopLeftBottomRightMaxContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.maxSize = new Size<>(Dimension.length(40.0f), Dimension.length(30.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(30.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_minmax_top_left_bottom_right_min_max__border_box")
    void blockAbsoluteMinmaxTopLeftBottomRightMinMaxBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.minSize = new Size<>(Dimension.length(50.0f), Dimension.length(60.0f));
        node0Style.maxSize = new Size<>(Dimension.length(40.0f), Dimension.length(30.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(60.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_minmax_top_left_bottom_right_min_max__content_box")
    void blockAbsoluteMinmaxTopLeftBottomRightMinMaxContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.minSize = new Size<>(Dimension.length(50.0f), Dimension.length(60.0f));
        node0Style.maxSize = new Size<>(Dimension.length(40.0f), Dimension.length(30.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(60.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_no_styles__border_box")
    void blockAbsoluteNoStylesBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.position = Position.ABSOLUTE;
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

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
        assertEquals(0.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_absolute_no_styles__content_box")
    void blockAbsoluteNoStylesContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.position = Position.ABSOLUTE;
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

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
        assertEquals(0.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_absolute_padding_border_overrides_max_size__border_box")
    void blockAbsolutePaddingBorderOverridesMaxSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.maxSize = new Size<>(Dimension.length(12.0f), Dimension.length(12.0f));
        node0Style.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        node0Style.border = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(0.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(22.0f, node0Layout.size().width, "width of node0");
        assertEquals(14.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_padding_border_overrides_max_size__content_box")
    void blockAbsolutePaddingBorderOverridesMaxSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.maxSize = new Size<>(Dimension.length(12.0f), Dimension.length(12.0f));
        node0Style.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        node0Style.border = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(0.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(22.0f, node0Layout.size().width, "width of node0");
        assertEquals(14.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_padding_border_overrides_size__border_box")
    void blockAbsolutePaddingBorderOverridesSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(12.0f), Dimension.length(12.0f));
        node0Style.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        node0Style.border = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(0.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(22.0f, node0Layout.size().width, "width of node0");
        assertEquals(14.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_padding_border_overrides_size__content_box")
    void blockAbsolutePaddingBorderOverridesSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(12.0f), Dimension.length(12.0f));
        node0Style.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        node0Style.border = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(0.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(34.0f, node0Layout.size().width, "width of node0");
        assertEquals(26.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_absolute_resolved_insets__border_box")
    void blockAbsoluteResolvedInsetsBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.position = Position.ABSOLUTE;
        node00Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node00 = tree.newLeaf(node00Style);

        Style node01Style = new Style();
        node01Style.position = Position.ABSOLUTE;
        node01Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO);
        NodeId node01 = tree.newLeaf(node01Style);

        Style node02Style = new Style();
        node02Style.position = Position.ABSOLUTE;
        node02Style.inset = new Rect<>(LengthPercentageAuto.percent(1.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(1.0f), LengthPercentageAuto.AUTO);
        NodeId node02 = tree.newLeaf(node02Style);

        Style node03Style = new Style();
        node03Style.position = Position.ABSOLUTE;
        node03Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(1.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(1.0f));
        NodeId node03 = tree.newLeaf(node03Style);

        Style node04Style = new Style();
        node04Style.position = Position.ABSOLUTE;
        node04Style.inset = new Rect<>(LengthPercentageAuto.length(30.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(30.0f), LengthPercentageAuto.AUTO);
        NodeId node04 = tree.newLeaf(node04Style);

        Style node05Style = new Style();
        node05Style.position = Position.ABSOLUTE;
        node05Style.size = new Size<>(Dimension.percent(1.0f), Dimension.percent(1.0f));
        node05Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO);
        NodeId node05 = tree.newLeaf(node05Style);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        node0Style.padding = new Rect<>(LengthPercentage.length(15.0f), LengthPercentage.length(15.0f), LengthPercentage.length(15.0f), LengthPercentage.length(15.0f));
        node0Style.border = new Rect<>(LengthPercentage.length(20.0f), LengthPercentage.length(20.0f), LengthPercentage.length(20.0f), LengthPercentage.length(20.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00, node01, node02, node03, node04, node05);

        Style node10Style = new Style();
        node10Style.position = Position.ABSOLUTE;
        node10Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node10 = tree.newLeaf(node10Style);

        Style node11Style = new Style();
        node11Style.position = Position.ABSOLUTE;
        node11Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO);
        NodeId node11 = tree.newLeaf(node11Style);

        Style node12Style = new Style();
        node12Style.position = Position.ABSOLUTE;
        node12Style.inset = new Rect<>(LengthPercentageAuto.percent(1.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(1.0f), LengthPercentageAuto.AUTO);
        NodeId node12 = tree.newLeaf(node12Style);

        Style node13Style = new Style();
        node13Style.position = Position.ABSOLUTE;
        node13Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(1.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(1.0f));
        NodeId node13 = tree.newLeaf(node13Style);

        Style node14Style = new Style();
        node14Style.position = Position.ABSOLUTE;
        node14Style.inset = new Rect<>(LengthPercentageAuto.length(30.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(30.0f), LengthPercentageAuto.AUTO);
        NodeId node14 = tree.newLeaf(node14Style);

        Style node15Style = new Style();
        node15Style.position = Position.ABSOLUTE;
        node15Style.size = new Size<>(Dimension.percent(1.0f), Dimension.percent(1.0f));
        node15Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO);
        NodeId node15 = tree.newLeaf(node15Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        node1Style.padding = new Rect<>(LengthPercentage.length(15.0f), LengthPercentage.length(15.0f), LengthPercentage.length(15.0f), LengthPercentage.length(15.0f));
        node1Style.border = new Rect<>(LengthPercentage.length(20.0f), LengthPercentage.length(20.0f), LengthPercentage.length(20.0f), LengthPercentage.length(20.0f));
        node1Style.overflow = new Point<>(Overflow.SCROLL, Overflow.SCROLL);
        node1Style.scrollbarWidth = 15.0f;
        NodeId node1 = tree.newWithChildren(node1Style, node10, node11, node12, node13, node14, node15);

        Style node20Style = new Style();
        node20Style.display = Display.BLOCK;
        node20Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node20 = tree.newLeaf(node20Style);

        Style node21Style = new Style();
        node21Style.position = Position.ABSOLUTE;
        node21Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node21 = tree.newLeaf(node21Style);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        node2Style.padding = new Rect<>(LengthPercentage.length(15.0f), LengthPercentage.length(15.0f), LengthPercentage.length(15.0f), LengthPercentage.length(15.0f));
        node2Style.border = new Rect<>(LengthPercentage.length(20.0f), LengthPercentage.length(20.0f), LengthPercentage.length(20.0f), LengthPercentage.length(20.0f));
        NodeId node2 = tree.newWithChildren(node2Style, node20, node21);

        Style nodeStyle = new Style();
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(600.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(200.0f, node0Layout.size().width, "width of node0");
        assertEquals(200.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(0.0f, node00Layout.size().width, "width of node00");
        assertEquals(0.0f, node00Layout.size().height, "height of node00");
        assertEquals(35.0f, node00Layout.location().x, "x of node00");
        assertEquals(35.0f, node00Layout.location().y, "y of node00");
        Layout node01Layout = tree.getLayout(node01);
        assertEquals(0.0f, node01Layout.size().width, "width of node01");
        assertEquals(0.0f, node01Layout.size().height, "height of node01");
        assertEquals(20.0f, node01Layout.location().x, "x of node01");
        assertEquals(20.0f, node01Layout.location().y, "y of node01");
        Layout node02Layout = tree.getLayout(node02);
        assertEquals(0.0f, node02Layout.size().width, "width of node02");
        assertEquals(0.0f, node02Layout.size().height, "height of node02");
        assertEquals(180.0f, node02Layout.location().x, "x of node02");
        assertEquals(180.0f, node02Layout.location().y, "y of node02");
        Layout node03Layout = tree.getLayout(node03);
        assertEquals(0.0f, node03Layout.size().width, "width of node03");
        assertEquals(0.0f, node03Layout.size().height, "height of node03");
        assertEquals(20.0f, node03Layout.location().x, "x of node03");
        assertEquals(20.0f, node03Layout.location().y, "y of node03");
        Layout node04Layout = tree.getLayout(node04);
        assertEquals(0.0f, node04Layout.size().width, "width of node04");
        assertEquals(0.0f, node04Layout.size().height, "height of node04");
        assertEquals(50.0f, node04Layout.location().x, "x of node04");
        assertEquals(50.0f, node04Layout.location().y, "y of node04");
        Layout node05Layout = tree.getLayout(node05);
        assertEquals(160.0f, node05Layout.size().width, "width of node05");
        assertEquals(160.0f, node05Layout.size().height, "height of node05");
        assertEquals(20.0f, node05Layout.location().x, "x of node05");
        assertEquals(20.0f, node05Layout.location().y, "y of node05");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(200.0f, node1Layout.size().width, "width of node1");
        assertEquals(200.0f, node1Layout.size().height, "height of node1");
        assertEquals(200.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(0.0f, node10Layout.size().width, "width of node10");
        assertEquals(0.0f, node10Layout.size().height, "height of node10");
        assertEquals(35.0f, node10Layout.location().x, "x of node10");
        assertEquals(35.0f, node10Layout.location().y, "y of node10");
        Layout node11Layout = tree.getLayout(node11);
        assertEquals(0.0f, node11Layout.size().width, "width of node11");
        assertEquals(0.0f, node11Layout.size().height, "height of node11");
        assertEquals(20.0f, node11Layout.location().x, "x of node11");
        assertEquals(20.0f, node11Layout.location().y, "y of node11");
        Layout node12Layout = tree.getLayout(node12);
        assertEquals(0.0f, node12Layout.size().width, "width of node12");
        assertEquals(0.0f, node12Layout.size().height, "height of node12");
        assertEquals(165.0f, node12Layout.location().x, "x of node12");
        assertEquals(165.0f, node12Layout.location().y, "y of node12");
        Layout node13Layout = tree.getLayout(node13);
        assertEquals(0.0f, node13Layout.size().width, "width of node13");
        assertEquals(0.0f, node13Layout.size().height, "height of node13");
        assertEquals(20.0f, node13Layout.location().x, "x of node13");
        assertEquals(20.0f, node13Layout.location().y, "y of node13");
        Layout node14Layout = tree.getLayout(node14);
        assertEquals(0.0f, node14Layout.size().width, "width of node14");
        assertEquals(0.0f, node14Layout.size().height, "height of node14");
        assertEquals(50.0f, node14Layout.location().x, "x of node14");
        assertEquals(50.0f, node14Layout.location().y, "y of node14");
        Layout node15Layout = tree.getLayout(node15);
        assertEquals(145.0f, node15Layout.size().width, "width of node15");
        assertEquals(145.0f, node15Layout.size().height, "height of node15");
        assertEquals(20.0f, node15Layout.location().x, "x of node15");
        assertEquals(20.0f, node15Layout.location().y, "y of node15");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(200.0f, node2Layout.size().width, "width of node2");
        assertEquals(200.0f, node2Layout.size().height, "height of node2");
        assertEquals(400.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
        Layout node20Layout = tree.getLayout(node20);
        assertEquals(50.0f, node20Layout.size().width, "width of node20");
        assertEquals(10.0f, node20Layout.size().height, "height of node20");
        assertEquals(35.0f, node20Layout.location().x, "x of node20");
        assertEquals(35.0f, node20Layout.location().y, "y of node20");
        Layout node21Layout = tree.getLayout(node21);
        assertEquals(0.0f, node21Layout.size().width, "width of node21");
        assertEquals(0.0f, node21Layout.size().height, "height of node21");
        assertEquals(35.0f, node21Layout.location().x, "x of node21");
        assertEquals(45.0f, node21Layout.location().y, "y of node21");
    }

    @Test
    @DisplayName("block_absolute_resolved_insets__content_box")
    void blockAbsoluteResolvedInsetsContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.position = Position.ABSOLUTE;
        node00Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node00 = tree.newLeaf(node00Style);

        Style node01Style = new Style();
        node01Style.boxSizing = BoxSizing.CONTENT_BOX;
        node01Style.position = Position.ABSOLUTE;
        node01Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO);
        NodeId node01 = tree.newLeaf(node01Style);

        Style node02Style = new Style();
        node02Style.boxSizing = BoxSizing.CONTENT_BOX;
        node02Style.position = Position.ABSOLUTE;
        node02Style.inset = new Rect<>(LengthPercentageAuto.percent(1.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(1.0f), LengthPercentageAuto.AUTO);
        NodeId node02 = tree.newLeaf(node02Style);

        Style node03Style = new Style();
        node03Style.boxSizing = BoxSizing.CONTENT_BOX;
        node03Style.position = Position.ABSOLUTE;
        node03Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(1.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(1.0f));
        NodeId node03 = tree.newLeaf(node03Style);

        Style node04Style = new Style();
        node04Style.boxSizing = BoxSizing.CONTENT_BOX;
        node04Style.position = Position.ABSOLUTE;
        node04Style.inset = new Rect<>(LengthPercentageAuto.length(30.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(30.0f), LengthPercentageAuto.AUTO);
        NodeId node04 = tree.newLeaf(node04Style);

        Style node05Style = new Style();
        node05Style.boxSizing = BoxSizing.CONTENT_BOX;
        node05Style.position = Position.ABSOLUTE;
        node05Style.size = new Size<>(Dimension.percent(1.0f), Dimension.percent(1.0f));
        node05Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO);
        NodeId node05 = tree.newLeaf(node05Style);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        node0Style.padding = new Rect<>(LengthPercentage.length(15.0f), LengthPercentage.length(15.0f), LengthPercentage.length(15.0f), LengthPercentage.length(15.0f));
        node0Style.border = new Rect<>(LengthPercentage.length(20.0f), LengthPercentage.length(20.0f), LengthPercentage.length(20.0f), LengthPercentage.length(20.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00, node01, node02, node03, node04, node05);

        Style node10Style = new Style();
        node10Style.boxSizing = BoxSizing.CONTENT_BOX;
        node10Style.position = Position.ABSOLUTE;
        node10Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node10 = tree.newLeaf(node10Style);

        Style node11Style = new Style();
        node11Style.boxSizing = BoxSizing.CONTENT_BOX;
        node11Style.position = Position.ABSOLUTE;
        node11Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO);
        NodeId node11 = tree.newLeaf(node11Style);

        Style node12Style = new Style();
        node12Style.boxSizing = BoxSizing.CONTENT_BOX;
        node12Style.position = Position.ABSOLUTE;
        node12Style.inset = new Rect<>(LengthPercentageAuto.percent(1.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(1.0f), LengthPercentageAuto.AUTO);
        NodeId node12 = tree.newLeaf(node12Style);

        Style node13Style = new Style();
        node13Style.boxSizing = BoxSizing.CONTENT_BOX;
        node13Style.position = Position.ABSOLUTE;
        node13Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(1.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(1.0f));
        NodeId node13 = tree.newLeaf(node13Style);

        Style node14Style = new Style();
        node14Style.boxSizing = BoxSizing.CONTENT_BOX;
        node14Style.position = Position.ABSOLUTE;
        node14Style.inset = new Rect<>(LengthPercentageAuto.length(30.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(30.0f), LengthPercentageAuto.AUTO);
        NodeId node14 = tree.newLeaf(node14Style);

        Style node15Style = new Style();
        node15Style.boxSizing = BoxSizing.CONTENT_BOX;
        node15Style.position = Position.ABSOLUTE;
        node15Style.size = new Size<>(Dimension.percent(1.0f), Dimension.percent(1.0f));
        node15Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(0.0f), LengthPercentageAuto.AUTO);
        NodeId node15 = tree.newLeaf(node15Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        node1Style.padding = new Rect<>(LengthPercentage.length(15.0f), LengthPercentage.length(15.0f), LengthPercentage.length(15.0f), LengthPercentage.length(15.0f));
        node1Style.border = new Rect<>(LengthPercentage.length(20.0f), LengthPercentage.length(20.0f), LengthPercentage.length(20.0f), LengthPercentage.length(20.0f));
        node1Style.overflow = new Point<>(Overflow.SCROLL, Overflow.SCROLL);
        node1Style.scrollbarWidth = 15.0f;
        NodeId node1 = tree.newWithChildren(node1Style, node10, node11, node12, node13, node14, node15);

        Style node20Style = new Style();
        node20Style.boxSizing = BoxSizing.CONTENT_BOX;
        node20Style.display = Display.BLOCK;
        node20Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node20 = tree.newLeaf(node20Style);

        Style node21Style = new Style();
        node21Style.boxSizing = BoxSizing.CONTENT_BOX;
        node21Style.position = Position.ABSOLUTE;
        node21Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node21 = tree.newLeaf(node21Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        node2Style.padding = new Rect<>(LengthPercentage.length(15.0f), LengthPercentage.length(15.0f), LengthPercentage.length(15.0f), LengthPercentage.length(15.0f));
        node2Style.border = new Rect<>(LengthPercentage.length(20.0f), LengthPercentage.length(20.0f), LengthPercentage.length(20.0f), LengthPercentage.length(20.0f));
        NodeId node2 = tree.newWithChildren(node2Style, node20, node21);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(810.0f, nodeLayout.size().width, "width of node");
        assertEquals(270.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(270.0f, node0Layout.size().width, "width of node0");
        assertEquals(270.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(0.0f, node00Layout.size().width, "width of node00");
        assertEquals(0.0f, node00Layout.size().height, "height of node00");
        assertEquals(35.0f, node00Layout.location().x, "x of node00");
        assertEquals(35.0f, node00Layout.location().y, "y of node00");
        Layout node01Layout = tree.getLayout(node01);
        assertEquals(0.0f, node01Layout.size().width, "width of node01");
        assertEquals(0.0f, node01Layout.size().height, "height of node01");
        assertEquals(20.0f, node01Layout.location().x, "x of node01");
        assertEquals(20.0f, node01Layout.location().y, "y of node01");
        Layout node02Layout = tree.getLayout(node02);
        assertEquals(0.0f, node02Layout.size().width, "width of node02");
        assertEquals(0.0f, node02Layout.size().height, "height of node02");
        assertEquals(250.0f, node02Layout.location().x, "x of node02");
        assertEquals(250.0f, node02Layout.location().y, "y of node02");
        Layout node03Layout = tree.getLayout(node03);
        assertEquals(0.0f, node03Layout.size().width, "width of node03");
        assertEquals(0.0f, node03Layout.size().height, "height of node03");
        assertEquals(20.0f, node03Layout.location().x, "x of node03");
        assertEquals(20.0f, node03Layout.location().y, "y of node03");
        Layout node04Layout = tree.getLayout(node04);
        assertEquals(0.0f, node04Layout.size().width, "width of node04");
        assertEquals(0.0f, node04Layout.size().height, "height of node04");
        assertEquals(50.0f, node04Layout.location().x, "x of node04");
        assertEquals(50.0f, node04Layout.location().y, "y of node04");
        Layout node05Layout = tree.getLayout(node05);
        assertEquals(230.0f, node05Layout.size().width, "width of node05");
        assertEquals(230.0f, node05Layout.size().height, "height of node05");
        assertEquals(20.0f, node05Layout.location().x, "x of node05");
        assertEquals(20.0f, node05Layout.location().y, "y of node05");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(270.0f, node1Layout.size().width, "width of node1");
        assertEquals(270.0f, node1Layout.size().height, "height of node1");
        assertEquals(270.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(0.0f, node10Layout.size().width, "width of node10");
        assertEquals(0.0f, node10Layout.size().height, "height of node10");
        assertEquals(35.0f, node10Layout.location().x, "x of node10");
        assertEquals(35.0f, node10Layout.location().y, "y of node10");
        Layout node11Layout = tree.getLayout(node11);
        assertEquals(0.0f, node11Layout.size().width, "width of node11");
        assertEquals(0.0f, node11Layout.size().height, "height of node11");
        assertEquals(20.0f, node11Layout.location().x, "x of node11");
        assertEquals(20.0f, node11Layout.location().y, "y of node11");
        Layout node12Layout = tree.getLayout(node12);
        assertEquals(0.0f, node12Layout.size().width, "width of node12");
        assertEquals(0.0f, node12Layout.size().height, "height of node12");
        assertEquals(235.0f, node12Layout.location().x, "x of node12");
        assertEquals(235.0f, node12Layout.location().y, "y of node12");
        Layout node13Layout = tree.getLayout(node13);
        assertEquals(0.0f, node13Layout.size().width, "width of node13");
        assertEquals(0.0f, node13Layout.size().height, "height of node13");
        assertEquals(20.0f, node13Layout.location().x, "x of node13");
        assertEquals(20.0f, node13Layout.location().y, "y of node13");
        Layout node14Layout = tree.getLayout(node14);
        assertEquals(0.0f, node14Layout.size().width, "width of node14");
        assertEquals(0.0f, node14Layout.size().height, "height of node14");
        assertEquals(50.0f, node14Layout.location().x, "x of node14");
        assertEquals(50.0f, node14Layout.location().y, "y of node14");
        Layout node15Layout = tree.getLayout(node15);
        assertEquals(215.0f, node15Layout.size().width, "width of node15");
        assertEquals(215.0f, node15Layout.size().height, "height of node15");
        assertEquals(20.0f, node15Layout.location().x, "x of node15");
        assertEquals(20.0f, node15Layout.location().y, "y of node15");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(270.0f, node2Layout.size().width, "width of node2");
        assertEquals(270.0f, node2Layout.size().height, "height of node2");
        assertEquals(540.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
        Layout node20Layout = tree.getLayout(node20);
        assertEquals(50.0f, node20Layout.size().width, "width of node20");
        assertEquals(10.0f, node20Layout.size().height, "height of node20");
        assertEquals(35.0f, node20Layout.location().x, "x of node20");
        assertEquals(35.0f, node20Layout.location().y, "y of node20");
        Layout node21Layout = tree.getLayout(node21);
        assertEquals(0.0f, node21Layout.size().width, "width of node21");
        assertEquals(0.0f, node21Layout.size().height, "height of node21");
        assertEquals(35.0f, node21Layout.location().x, "x of node21");
        assertEquals(45.0f, node21Layout.location().y, "y of node21");
    }

    @Test
    @DisplayName("block_align_baseline_child__border_box")
    void blockAlignBaselineChildBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(20.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style nodeStyle = new Style();
        nodeStyle.alignItems = AlignItems.BASELINE;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(20.0f, node1Layout.size().height, "height of node1");
        assertEquals(50.0f, node1Layout.location().x, "x of node1");
        assertEquals(30.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(0.0f, node10Layout.location().y, "y of node10");
    }

    @Test
    @DisplayName("block_align_baseline_child__content_box")
    void blockAlignBaselineChildContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.boxSizing = BoxSizing.CONTENT_BOX;
        node10Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(20.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.alignItems = AlignItems.BASELINE;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(20.0f, node1Layout.size().height, "height of node1");
        assertEquals(50.0f, node1Layout.location().x, "x of node1");
        assertEquals(30.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(0.0f, node10Layout.location().y, "y of node10");
    }

    @Test
    @DisplayName("block_align_baseline_child_margin__border_box")
    void blockAlignBaselineChildMarginBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(5.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        node10Style.margin = new Rect<>(LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(5.0f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(20.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style nodeStyle = new Style();
        nodeStyle.alignItems = AlignItems.BASELINE;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(5.0f, node0Layout.location().x, "x of node0");
        assertEquals(5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(20.0f, node1Layout.size().height, "height of node1");
        assertEquals(50.0f, node1Layout.location().x, "x of node1");
        assertEquals(35.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(5.0f, node10Layout.location().x, "x of node10");
        assertEquals(5.0f, node10Layout.location().y, "y of node10");
    }

    @Test
    @DisplayName("block_align_baseline_child_margin__content_box")
    void blockAlignBaselineChildMarginContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(5.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.boxSizing = BoxSizing.CONTENT_BOX;
        node10Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        node10Style.margin = new Rect<>(LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(5.0f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(20.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.alignItems = AlignItems.BASELINE;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(5.0f, node0Layout.location().x, "x of node0");
        assertEquals(5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(20.0f, node1Layout.size().height, "height of node1");
        assertEquals(50.0f, node1Layout.location().x, "x of node1");
        assertEquals(35.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(5.0f, node10Layout.location().x, "x of node10");
        assertEquals(5.0f, node10Layout.location().y, "y of node10");
    }

    @Test
    @DisplayName("block_align_baseline_child_margin_percent__border_box")
    void blockAlignBaselineChildMarginPercentBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.percent(0.05f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.display = Display.BLOCK;
        node10Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        node10Style.margin = new Rect<>(LengthPercentageAuto.percent(0.01f), LengthPercentageAuto.percent(0.01f), LengthPercentageAuto.percent(0.01f), LengthPercentageAuto.percent(0.01f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(20.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style nodeStyle = new Style();
        nodeStyle.alignItems = AlignItems.BASELINE;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(5.0f, node0Layout.location().x, "x of node0");
        assertEquals(5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(20.0f, node1Layout.size().height, "height of node1");
        assertEquals(50.0f, node1Layout.location().x, "x of node1");
        assertEquals(35.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(1.0f, node10Layout.location().x, "x of node10");
        assertEquals(1.0f, node10Layout.location().y, "y of node10");
    }

    @Test
    @DisplayName("block_align_baseline_child_margin_percent__content_box")
    void blockAlignBaselineChildMarginPercentContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.percent(0.05f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.boxSizing = BoxSizing.CONTENT_BOX;
        node10Style.display = Display.BLOCK;
        node10Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        node10Style.margin = new Rect<>(LengthPercentageAuto.percent(0.01f), LengthPercentageAuto.percent(0.01f), LengthPercentageAuto.percent(0.01f), LengthPercentageAuto.percent(0.01f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(20.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.alignItems = AlignItems.BASELINE;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(5.0f, node0Layout.location().x, "x of node0");
        assertEquals(5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(20.0f, node1Layout.size().height, "height of node1");
        assertEquals(50.0f, node1Layout.location().x, "x of node1");
        assertEquals(35.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(1.0f, node10Layout.location().x, "x of node10");
        assertEquals(1.0f, node10Layout.location().y, "y of node10");
    }

    @Test
    @DisplayName("block_align_baseline_child_padding__border_box")
    void blockAlignBaselineChildPaddingBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.display = Display.BLOCK;
        node10Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(20.0f));
        node1Style.padding = new Rect<>(LengthPercentage.length(5.0f), LengthPercentage.length(5.0f), LengthPercentage.length(5.0f), LengthPercentage.length(5.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style nodeStyle = new Style();
        nodeStyle.alignItems = AlignItems.BASELINE;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        nodeStyle.padding = new Rect<>(LengthPercentage.length(5.0f), LengthPercentage.length(5.0f), LengthPercentage.length(5.0f), LengthPercentage.length(5.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(5.0f, node0Layout.location().x, "x of node0");
        assertEquals(5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(20.0f, node1Layout.size().height, "height of node1");
        assertEquals(45.0f, node1Layout.location().x, "x of node1");
        assertEquals(35.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(5.0f, node10Layout.location().x, "x of node10");
        assertEquals(5.0f, node10Layout.location().y, "y of node10");
    }

    @Test
    @DisplayName("block_align_baseline_child_padding__content_box")
    void blockAlignBaselineChildPaddingContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.boxSizing = BoxSizing.CONTENT_BOX;
        node10Style.display = Display.BLOCK;
        node10Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(20.0f));
        node1Style.padding = new Rect<>(LengthPercentage.length(5.0f), LengthPercentage.length(5.0f), LengthPercentage.length(5.0f), LengthPercentage.length(5.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.alignItems = AlignItems.BASELINE;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        nodeStyle.padding = new Rect<>(LengthPercentage.length(5.0f), LengthPercentage.length(5.0f), LengthPercentage.length(5.0f), LengthPercentage.length(5.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(110.0f, nodeLayout.size().width, "width of node");
        assertEquals(110.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(5.0f, node0Layout.location().x, "x of node0");
        assertEquals(5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(60.0f, node1Layout.size().width, "width of node1");
        assertEquals(30.0f, node1Layout.size().height, "height of node1");
        assertEquals(45.0f, node1Layout.location().x, "x of node1");
        assertEquals(25.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(5.0f, node10Layout.location().x, "x of node10");
        assertEquals(5.0f, node10Layout.location().y, "y of node10");
    }

    @Test
    @DisplayName("block_align_baseline_child_top__border_box")
    void blockAlignBaselineChildTopBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.display = Display.BLOCK;
        node10Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(20.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style nodeStyle = new Style();
        nodeStyle.alignItems = AlignItems.BASELINE;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(20.0f, node1Layout.size().height, "height of node1");
        assertEquals(50.0f, node1Layout.location().x, "x of node1");
        assertEquals(30.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(0.0f, node10Layout.location().y, "y of node10");
    }

    @Test
    @DisplayName("block_align_baseline_child_top__content_box")
    void blockAlignBaselineChildTopContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.boxSizing = BoxSizing.CONTENT_BOX;
        node10Style.display = Display.BLOCK;
        node10Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(20.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.alignItems = AlignItems.BASELINE;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(20.0f, node1Layout.size().height, "height of node1");
        assertEquals(50.0f, node1Layout.location().x, "x of node1");
        assertEquals(30.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(0.0f, node10Layout.location().y, "y of node10");
    }

    @Test
    @DisplayName("block_align_baseline_child_top2__border_box")
    void blockAlignBaselineChildTop2BorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.display = Display.BLOCK;
        node10Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(20.0f));
        node1Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.length(5.0f), LengthPercentageAuto.AUTO);
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style nodeStyle = new Style();
        nodeStyle.alignItems = AlignItems.BASELINE;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(20.0f, node1Layout.size().height, "height of node1");
        assertEquals(50.0f, node1Layout.location().x, "x of node1");
        assertEquals(35.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(0.0f, node10Layout.location().y, "y of node10");
    }

    @Test
    @DisplayName("block_align_baseline_child_top2__content_box")
    void blockAlignBaselineChildTop2ContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.boxSizing = BoxSizing.CONTENT_BOX;
        node10Style.display = Display.BLOCK;
        node10Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(20.0f));
        node1Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.length(5.0f), LengthPercentageAuto.AUTO);
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.alignItems = AlignItems.BASELINE;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(20.0f, node1Layout.size().height, "height of node1");
        assertEquals(50.0f, node1Layout.location().x, "x of node1");
        assertEquals(35.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(0.0f, node10Layout.location().y, "y of node10");
    }

    @Test
    @DisplayName("block_align_baseline_double_nested_child__border_box")
    void blockAlignBaselineDoubleNestedChildBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(20.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style node10Style = new Style();
        node10Style.display = Display.BLOCK;
        node10Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(15.0f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(20.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style nodeStyle = new Style();
        nodeStyle.alignItems = AlignItems.BASELINE;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(50.0f, node00Layout.size().width, "width of node00");
        assertEquals(20.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(20.0f, node1Layout.size().height, "height of node1");
        assertEquals(50.0f, node1Layout.location().x, "x of node1");
        assertEquals(30.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(15.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(0.0f, node10Layout.location().y, "y of node10");
    }

    @Test
    @DisplayName("block_align_baseline_double_nested_child__content_box")
    void blockAlignBaselineDoubleNestedChildContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(20.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style node10Style = new Style();
        node10Style.boxSizing = BoxSizing.CONTENT_BOX;
        node10Style.display = Display.BLOCK;
        node10Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(15.0f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(20.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.alignItems = AlignItems.BASELINE;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(50.0f, node00Layout.size().width, "width of node00");
        assertEquals(20.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(20.0f, node1Layout.size().height, "height of node1");
        assertEquals(50.0f, node1Layout.location().x, "x of node1");
        assertEquals(30.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(15.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(0.0f, node10Layout.location().y, "y of node10");
    }

    @Test
    @DisplayName("block_aspect_ratio_fill_height__border_box")
    void blockAspectRatioFillHeightBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(40.0f), Dimension.AUTO);
        node0Style.aspectRatio = 2.0f;
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_aspect_ratio_fill_height__content_box")
    void blockAspectRatioFillHeightContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(40.0f), Dimension.AUTO);
        node0Style.aspectRatio = 2.0f;
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_aspect_ratio_fill_max_height__border_box")
    void blockAspectRatioFillMaxHeightBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.maxSize = new Size<>(Dimension.length(40.0f), Dimension.AUTO);
        node0Style.aspectRatio = 2.0f;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH\u200BHH\u200BHH\u200BHH\u200BHH\u200BHH\u200BHH\u200BHH\u200BHH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(60.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_aspect_ratio_fill_max_height__content_box")
    void blockAspectRatioFillMaxHeightContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.maxSize = new Size<>(Dimension.length(40.0f), Dimension.AUTO);
        node0Style.aspectRatio = 2.0f;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH\u200BHH\u200BHH\u200BHH\u200BHH\u200BHH\u200BHH\u200BHH\u200BHH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(60.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_aspect_ratio_fill_max_width__border_box")
    void blockAspectRatioFillMaxWidthBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.maxSize = new Size<>(Dimension.AUTO, Dimension.length(20.0f));
        node0Style.aspectRatio = 2.0f;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH\u200BHH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_aspect_ratio_fill_max_width__content_box")
    void blockAspectRatioFillMaxWidthContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.maxSize = new Size<>(Dimension.AUTO, Dimension.length(20.0f));
        node0Style.aspectRatio = 2.0f;
        MeasureFunc node0Measure = ahemTextMeasure("HH\u200BHH\u200BHH\u200BHH", false);
        NodeId node0 = tree.newLeafWithMeasure(node0Style, node0Measure);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_aspect_ratio_fill_min_height__border_box")
    void blockAspectRatioFillMinHeightBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.minSize = new Size<>(Dimension.length(40.0f), Dimension.AUTO);
        node0Style.aspectRatio = 2.0f;
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_aspect_ratio_fill_min_height__content_box")
    void blockAspectRatioFillMinHeightContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.minSize = new Size<>(Dimension.length(40.0f), Dimension.AUTO);
        node0Style.aspectRatio = 2.0f;
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_aspect_ratio_fill_min_width__border_box")
    void blockAspectRatioFillMinWidthBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.minSize = new Size<>(Dimension.AUTO, Dimension.length(40.0f));
        node0Style.aspectRatio = 2.0f;
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_aspect_ratio_fill_min_width__content_box")
    void blockAspectRatioFillMinWidthContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.minSize = new Size<>(Dimension.AUTO, Dimension.length(40.0f));
        node0Style.aspectRatio = 2.0f;
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_aspect_ratio_fill_width__border_box")
    void blockAspectRatioFillWidthBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(40.0f));
        node0Style.aspectRatio = 2.0f;
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(80.0f, node0Layout.size().width, "width of node0");
        assertEquals(40.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_aspect_ratio_fill_width__content_box")
    void blockAspectRatioFillWidthContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(40.0f));
        node0Style.aspectRatio = 2.0f;
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(80.0f, node0Layout.size().width, "width of node0");
        assertEquals(40.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_basic__border_box")
    void blockBasicBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_basic__content_box")
    void blockBasicContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_border_fixed_size__border_box")
    void blockBorderFixedSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        nodeStyle.border = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(50.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(38.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(8.0f, node0Layout.location().x, "x of node0");
        assertEquals(2.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(38.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(8.0f, node1Layout.location().x, "x of node1");
        assertEquals(12.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_border_fixed_size__content_box")
    void blockBorderFixedSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        nodeStyle.border = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(62.0f, nodeLayout.size().width, "width of node");
        assertEquals(58.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(8.0f, node0Layout.location().x, "x of node0");
        assertEquals(2.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(8.0f, node1Layout.location().x, "x of node1");
        assertEquals(12.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_border_intrinsic_size__border_box")
    void blockBorderIntrinsicSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.border = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(62.0f, nodeLayout.size().width, "width of node");
        assertEquals(28.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(8.0f, node0Layout.location().x, "x of node0");
        assertEquals(2.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(8.0f, node1Layout.location().x, "x of node1");
        assertEquals(12.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_border_intrinsic_size__content_box")
    void blockBorderIntrinsicSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.border = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(62.0f, nodeLayout.size().width, "width of node");
        assertEquals(28.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(8.0f, node0Layout.location().x, "x of node0");
        assertEquals(2.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(8.0f, node1Layout.location().x, "x of node1");
        assertEquals(12.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_border_percentage_fixed_size__border_box")
    void blockBorderPercentageFixedSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(50.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(50.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_border_percentage_fixed_size__content_box")
    void blockBorderPercentageFixedSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(50.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(50.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_border_percentage_intrinsic_size__border_box")
    void blockBorderPercentageIntrinsicSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(0.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(0.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(0.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_border_percentage_intrinsic_size__content_box")
    void blockBorderPercentageIntrinsicSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(0.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(0.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(0.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_display_none__border_box")
    void blockDisplayNoneBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.NONE;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(0.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(10.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_display_none__content_box")
    void blockDisplayNoneContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.NONE;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(0.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(10.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_display_none_with_child__border_box")
    void blockDisplayNoneWithChildBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.size = new Size<>(Dimension.length(20.0f), Dimension.length(10.0f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node1Style = new Style();
        node1Style.display = Display.NONE;
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style node2Style = new Style();
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(0.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(0.0f, node10Layout.size().width, "width of node10");
        assertEquals(0.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(0.0f, node10Layout.location().y, "y of node10");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(10.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_display_none_with_child__content_box")
    void blockDisplayNoneWithChildContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.boxSizing = BoxSizing.CONTENT_BOX;
        node10Style.size = new Size<>(Dimension.length(20.0f), Dimension.length(10.0f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.NONE;
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(0.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(0.0f, node10Layout.size().width, "width of node10");
        assertEquals(0.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(0.0f, node10Layout.location().y, "y of node10");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(10.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_display_none_with_inset__border_box")
    void blockDisplayNoneWithInsetBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.NONE;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(0.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_display_none_with_inset__content_box")
    void blockDisplayNoneWithInsetContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.NONE;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.AUTO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(0.0f, node1Layout.size().width, "width of node1");
        assertEquals(0.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_display_none_with_margin__border_box")
    void blockDisplayNoneWithMarginBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.NONE;
        node0Style.size = new Size<>(Dimension.length(20.0f), Dimension.length(20.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(0.0f, node0Layout.size().width, "width of node0");
        assertEquals(0.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_display_none_with_margin__content_box")
    void blockDisplayNoneWithMarginContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.NONE;
        node0Style.size = new Size<>(Dimension.length(20.0f), Dimension.length(20.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(0.0f, node0Layout.size().width, "width of node0");
        assertEquals(0.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_display_none_with_position_absolute__border_box")
    void blockDisplayNoneWithPositionAbsoluteBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.NONE;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(0.0f, node0Layout.size().width, "width of node0");
        assertEquals(0.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_display_none_with_position_absolute__content_box")
    void blockDisplayNoneWithPositionAbsoluteContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.NONE;
        node0Style.position = Position.ABSOLUTE;
        node0Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(0.0f, node0Layout.size().width, "width of node0");
        assertEquals(0.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_inset_fixed__border_box")
    void blockInsetFixedBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(2.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(4.0f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(6.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(8.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.inset = new Rect<>(LengthPercentageAuto.length(2.0f), LengthPercentageAuto.length(6.0f), LengthPercentageAuto.length(4.0f), LengthPercentageAuto.length(8.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(2.0f, node0Layout.location().x, "x of node0");
        assertEquals(4.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(-6.0f, node1Layout.location().x, "x of node1");
        assertEquals(2.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(2.0f, node2Layout.location().x, "x of node2");
        assertEquals(24.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_inset_fixed__content_box")
    void blockInsetFixedContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(2.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(4.0f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(6.0f), LengthPercentageAuto.AUTO, LengthPercentageAuto.length(8.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.inset = new Rect<>(LengthPercentageAuto.length(2.0f), LengthPercentageAuto.length(6.0f), LengthPercentageAuto.length(4.0f), LengthPercentageAuto.length(8.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(2.0f, node0Layout.location().x, "x of node0");
        assertEquals(4.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(-6.0f, node1Layout.location().x, "x of node1");
        assertEquals(2.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(2.0f, node2Layout.location().x, "x of node2");
        assertEquals(24.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_inset_percentage__border_box")
    void blockInsetPercentageBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.percent(0.02f), LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.04f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.06f), LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.08f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.inset = new Rect<>(LengthPercentageAuto.percent(0.02f), LengthPercentageAuto.percent(0.06f), LengthPercentageAuto.percent(0.04f), LengthPercentageAuto.percent(0.08f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(1.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(-3.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(1.0f, node2Layout.location().x, "x of node2");
        assertEquals(20.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_inset_percentage__content_box")
    void blockInsetPercentageContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.inset = new Rect<>(LengthPercentageAuto.percent(0.02f), LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.04f), LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.inset = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.06f), LengthPercentageAuto.AUTO, LengthPercentageAuto.percent(0.08f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.inset = new Rect<>(LengthPercentageAuto.percent(0.02f), LengthPercentageAuto.percent(0.06f), LengthPercentageAuto.percent(0.04f), LengthPercentageAuto.percent(0.08f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(1.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(-3.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(1.0f, node2Layout.location().x, "x of node2");
        assertEquals(20.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_intrinsic_width__border_box")
    void blockIntrinsicWidthBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_intrinsic_width__content_box")
    void blockIntrinsicWidthContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_item_max_width__border_box")
    void blockItemMaxWidthBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node0Style.maxSize = new Size<>(Dimension.length(100.0f), Dimension.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node1Style.maxSize = new Size<>(Dimension.length(300.0f), Dimension.AUTO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(200.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_item_max_width__content_box")
    void blockItemMaxWidthContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node0Style.maxSize = new Size<>(Dimension.length(100.0f), Dimension.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node1Style.maxSize = new Size<>(Dimension.length(300.0f), Dimension.AUTO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(200.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_item_min_width_overrides_max_width__border_box")
    void blockItemMinWidthOverridesMaxWidthBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node0Style.minSize = new Size<>(Dimension.length(200.0f), Dimension.AUTO);
        node0Style.maxSize = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(200.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_item_min_width_overrides_max_width__content_box")
    void blockItemMinWidthOverridesMaxWidthContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node0Style.minSize = new Size<>(Dimension.length(200.0f), Dimension.AUTO);
        node0Style.maxSize = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(200.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_item_text_align_center__border_box")
    void blockItemTextAlignCenterBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node0Style.maxSize = new Size<>(Dimension.length(100.0f), Dimension.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node1Style.maxSize = new Size<>(Dimension.length(300.0f), Dimension.AUTO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.textAlign = TextAlign.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(50.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(200.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_item_text_align_center__content_box")
    void blockItemTextAlignCenterContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node0Style.maxSize = new Size<>(Dimension.length(100.0f), Dimension.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node1Style.maxSize = new Size<>(Dimension.length(300.0f), Dimension.AUTO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.textAlign = TextAlign.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(50.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(200.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_item_text_align_left__border_box")
    void blockItemTextAlignLeftBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node0Style.maxSize = new Size<>(Dimension.length(100.0f), Dimension.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node1Style.maxSize = new Size<>(Dimension.length(300.0f), Dimension.AUTO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.textAlign = TextAlign.LEFT;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(200.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_item_text_align_left__content_box")
    void blockItemTextAlignLeftContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node0Style.maxSize = new Size<>(Dimension.length(100.0f), Dimension.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node1Style.maxSize = new Size<>(Dimension.length(300.0f), Dimension.AUTO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.textAlign = TextAlign.LEFT;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(200.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_item_text_align_right__border_box")
    void blockItemTextAlignRightBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node0Style.maxSize = new Size<>(Dimension.length(100.0f), Dimension.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node1Style.maxSize = new Size<>(Dimension.length(300.0f), Dimension.AUTO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.textAlign = TextAlign.RIGHT;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(100.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(200.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_item_text_align_right__content_box")
    void blockItemTextAlignRightContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node0Style.maxSize = new Size<>(Dimension.length(100.0f), Dimension.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node1Style.maxSize = new Size<>(Dimension.length(300.0f), Dimension.AUTO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.textAlign = TextAlign.RIGHT;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(100.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(200.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_auto_bottom__border_box")
    void blockMarginAutoBottomBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_auto_bottom__content_box")
    void blockMarginAutoBottomContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_auto_bottom_and_top__border_box")
    void blockMarginAutoBottomAndTopBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_auto_bottom_and_top__content_box")
    void blockMarginAutoBottomAndTopContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_auto_left__border_box")
    void blockMarginAutoLeftBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(150.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_auto_left__content_box")
    void blockMarginAutoLeftContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(150.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_auto_left_and_right__border_box")
    void blockMarginAutoLeftAndRightBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(75.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_auto_left_and_right__content_box")
    void blockMarginAutoLeftAndRightContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(75.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_auto_left_and_right_with_auto_width__border_box")
    void blockMarginAutoLeftAndRightWithAutoWidthBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node0Style.maxSize = new Size<>(Dimension.length(100.0f), Dimension.AUTO);
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(50.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_margin_auto_left_and_right_with_auto_width__content_box")
    void blockMarginAutoLeftAndRightWithAutoWidthContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(50.0f));
        node0Style.maxSize = new Size<>(Dimension.length(100.0f), Dimension.AUTO);
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(50.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_margin_auto_left_child_bigger_than_parent__border_box")
    void blockMarginAutoLeftChildBiggerThanParentBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(72.0f), Dimension.length(72.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(52.0f), Dimension.length(52.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(52.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(72.0f, node0Layout.size().width, "width of node0");
        assertEquals(72.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_margin_auto_left_child_bigger_than_parent__content_box")
    void blockMarginAutoLeftChildBiggerThanParentContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(72.0f), Dimension.length(72.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(52.0f), Dimension.length(52.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(52.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(72.0f, node0Layout.size().width, "width of node0");
        assertEquals(72.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_margin_auto_left_fix_right_child_bigger_than_parent__border_box")
    void blockMarginAutoLeftFixRightChildBiggerThanParentBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(72.0f), Dimension.length(72.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(52.0f), Dimension.length(52.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(52.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(72.0f, node0Layout.size().width, "width of node0");
        assertEquals(72.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_margin_auto_left_fix_right_child_bigger_than_parent__content_box")
    void blockMarginAutoLeftFixRightChildBiggerThanParentContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(72.0f), Dimension.length(72.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(52.0f), Dimension.length(52.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(52.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(72.0f, node0Layout.size().width, "width of node0");
        assertEquals(72.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_margin_auto_left_right_child_bigger_than_parent__border_box")
    void blockMarginAutoLeftRightChildBiggerThanParentBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(72.0f), Dimension.length(72.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(52.0f), Dimension.length(52.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(52.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(72.0f, node0Layout.size().width, "width of node0");
        assertEquals(72.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_margin_auto_left_right_child_bigger_than_parent__content_box")
    void blockMarginAutoLeftRightChildBiggerThanParentContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(72.0f), Dimension.length(72.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(52.0f), Dimension.length(52.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(52.0f, nodeLayout.size().width, "width of node");
        assertEquals(52.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(72.0f, node0Layout.size().width, "width of node0");
        assertEquals(72.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_margin_auto_multiple_children__border_box")
    void blockMarginAutoMultipleChildrenBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(100.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_auto_multiple_children__content_box")
    void blockMarginAutoMultipleChildrenContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(100.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_auto_right__border_box")
    void blockMarginAutoRightBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_auto_right__content_box")
    void blockMarginAutoRightContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_auto_top__border_box")
    void blockMarginAutoTopBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_auto_top__content_box")
    void blockMarginAutoTopContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(200.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_fixed_auto_bottom__border_box")
    void blockMarginXFixedAutoBottomBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(20.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_fixed_auto_bottom__content_box")
    void blockMarginXFixedAutoBottomContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(20.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_fixed_auto_left__border_box")
    void blockMarginXFixedAutoLeftBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(20.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(30.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_fixed_auto_left__content_box")
    void blockMarginXFixedAutoLeftContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(20.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(30.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_fixed_auto_left_and_right__border_box")
    void blockMarginXFixedAutoLeftAndRightBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(20.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(15.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_fixed_auto_left_and_right__content_box")
    void blockMarginXFixedAutoLeftAndRightContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(20.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(15.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_fixed_auto_right__border_box")
    void blockMarginXFixedAutoRightBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(20.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_fixed_auto_right__content_box")
    void blockMarginXFixedAutoRightContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(20.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_fixed_auto_top__border_box")
    void blockMarginXFixedAutoTopBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(20.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_fixed_auto_top__content_box")
    void blockMarginXFixedAutoTopContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(20.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_fixed_size_negative__border_box")
    void blockMarginXFixedSizeNegativeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(65.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(-10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_fixed_size_negative__content_box")
    void blockMarginXFixedSizeNegativeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(65.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(-10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_fixed_size_positive__border_box")
    void blockMarginXFixedSizePositiveBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(5.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(35.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_fixed_size_positive__content_box")
    void blockMarginXFixedSizePositiveContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(5.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(35.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_intrinsic_size_negative__border_box")
    void blockMarginXIntrinsicSizeNegativeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(0.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(15.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(-10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(0.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_intrinsic_size_negative__content_box")
    void blockMarginXIntrinsicSizeNegativeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(0.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(15.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(-10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(0.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_intrinsic_size_positive__border_box")
    void blockMarginXIntrinsicSizePositiveBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(5.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(15.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(0.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(15.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_intrinsic_size_positive__content_box")
    void blockMarginXIntrinsicSizePositiveContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(5.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(15.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(0.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(15.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_percentage_fixed_size_negative__border_box")
    void blockMarginXPercentageFixedSizeNegativeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.percent(-0.2f), LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(65.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(-10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_percentage_fixed_size_negative__content_box")
    void blockMarginXPercentageFixedSizeNegativeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.percent(-0.2f), LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(65.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(-10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_percentage_fixed_size_positive__border_box")
    void blockMarginXPercentageFixedSizePositiveBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.percent(0.2f), LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(35.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_percentage_fixed_size_positive__content_box")
    void blockMarginXPercentageFixedSizePositiveContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.percent(0.2f), LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(35.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(10.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_percentage_intrinsic_size_other_negative__border_box")
    void blockMarginXPercentageIntrinsicSizeOtherNegativeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.percent(-0.2f), LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(130.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(-20.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_percentage_intrinsic_size_other_negative__content_box")
    void blockMarginXPercentageIntrinsicSizeOtherNegativeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.percent(-0.2f), LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(130.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(-20.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_percentage_intrinsic_size_other_positive__border_box")
    void blockMarginXPercentageIntrinsicSizeOtherPositiveBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.percent(0.2f), LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(70.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(20.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_percentage_intrinsic_size_other_positive__content_box")
    void blockMarginXPercentageIntrinsicSizeOtherPositiveContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.percent(0.2f), LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(70.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(20.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_x_percentage_intrinsic_size_self_negative__border_box")
    void blockMarginXPercentageIntrinsicSizeSelfNegativeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.percent(-0.2f), LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(-20.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_margin_x_percentage_intrinsic_size_self_negative__content_box")
    void blockMarginXPercentageIntrinsicSizeSelfNegativeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.percent(-0.2f), LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(-20.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_margin_x_percentage_intrinsic_size_self_positive__border_box")
    void blockMarginXPercentageIntrinsicSizeSelfPositiveBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.percent(0.2f), LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(20.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_margin_x_percentage_intrinsic_size_self_positive__content_box")
    void blockMarginXPercentageIntrinsicSizeSelfPositiveContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(100.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.percent(0.2f), LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(20.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_margin_y_collapse_complex__border_box")
    void blockMarginYCollapseComplexBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node10Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.length(-5.0f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node11Style = new Style();
        node11Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node11Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(7.0f), LengthPercentageAuto.length(3.0f));
        NodeId node11 = tree.newLeaf(node11Style);

        Style node12Style = new Style();
        node12Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node12Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-6.0f), LengthPercentageAuto.length(9.0f));
        NodeId node12 = tree.newLeaf(node12Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10, node11, node12);

        Style node2Style = new Style();
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.length(-5.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style node3Style = new Style();
        node3Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node3Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node3 = tree.newLeaf(node3Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-10.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(-10.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(0.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(-5.0f, node10Layout.location().y, "y of node10");
        Layout node11Layout = tree.getLayout(node11);
        assertEquals(0.0f, node11Layout.size().width, "width of node11");
        assertEquals(10.0f, node11Layout.size().height, "height of node11");
        assertEquals(0.0f, node11Layout.location().x, "x of node11");
        assertEquals(7.0f, node11Layout.location().y, "y of node11");
        Layout node12Layout = tree.getLayout(node12);
        assertEquals(0.0f, node12Layout.size().width, "width of node12");
        assertEquals(10.0f, node12Layout.size().height, "height of node12");
        assertEquals(0.0f, node12Layout.location().x, "x of node12");
        assertEquals(-6.0f, node12Layout.location().y, "y of node12");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(-10.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(10.0f, node3Layout.size().height, "height of node3");
        assertEquals(0.0f, node3Layout.location().x, "x of node3");
        assertEquals(-10.0f, node3Layout.location().y, "y of node3");
    }

    @Test
    @DisplayName("block_margin_y_collapse_complex__content_box")
    void blockMarginYCollapseComplexContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.boxSizing = BoxSizing.CONTENT_BOX;
        node10Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node10Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.length(-5.0f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node11Style = new Style();
        node11Style.boxSizing = BoxSizing.CONTENT_BOX;
        node11Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node11Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(7.0f), LengthPercentageAuto.length(3.0f));
        NodeId node11 = tree.newLeaf(node11Style);

        Style node12Style = new Style();
        node12Style.boxSizing = BoxSizing.CONTENT_BOX;
        node12Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node12Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-6.0f), LengthPercentageAuto.length(9.0f));
        NodeId node12 = tree.newLeaf(node12Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10, node11, node12);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.length(-5.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style node3Style = new Style();
        node3Style.boxSizing = BoxSizing.CONTENT_BOX;
        node3Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node3Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node3 = tree.newLeaf(node3Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-10.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(-10.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(0.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(-5.0f, node10Layout.location().y, "y of node10");
        Layout node11Layout = tree.getLayout(node11);
        assertEquals(0.0f, node11Layout.size().width, "width of node11");
        assertEquals(10.0f, node11Layout.size().height, "height of node11");
        assertEquals(0.0f, node11Layout.location().x, "x of node11");
        assertEquals(7.0f, node11Layout.location().y, "y of node11");
        Layout node12Layout = tree.getLayout(node12);
        assertEquals(0.0f, node12Layout.size().width, "width of node12");
        assertEquals(10.0f, node12Layout.size().height, "height of node12");
        assertEquals(0.0f, node12Layout.location().x, "x of node12");
        assertEquals(-6.0f, node12Layout.location().y, "y of node12");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(-10.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(10.0f, node3Layout.size().height, "height of node3");
        assertEquals(0.0f, node3Layout.location().x, "x of node3");
        assertEquals(-10.0f, node3Layout.location().y, "y of node3");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_blocked_by_aspect_ratio__border_box")
    void blockMarginYCollapseThroughBlockedByAspectRatioBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.aspectRatio = 2.0f;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(65.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(25.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(55.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_blocked_by_aspect_ratio__content_box")
    void blockMarginYCollapseThroughBlockedByAspectRatioContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.aspectRatio = 2.0f;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(65.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(25.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(55.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_blocked_by_border_bottom__border_box")
    void blockMarginYCollapseThroughBlockedByBorderBottomBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
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
        assertEquals(20.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_blocked_by_border_bottom__content_box")
    void blockMarginYCollapseThroughBlockedByBorderBottomContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
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
        assertEquals(20.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_blocked_by_border_top__border_box")
    void blockMarginYCollapseThroughBlockedByBorderTopBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
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
        assertEquals(20.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_blocked_by_border_top__content_box")
    void blockMarginYCollapseThroughBlockedByBorderTopContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
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
        assertEquals(20.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_blocked_by_height__border_box")
    void blockMarginYCollapseThroughBlockedByHeightBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(1.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(41.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(1.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(31.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_blocked_by_height__content_box")
    void blockMarginYCollapseThroughBlockedByHeightContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(1.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(41.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(1.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(31.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_blocked_by_line_box__border_box")
    void blockMarginYCollapseThroughBlockedByLineBoxBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        MeasureFunc node1Measure = ahemTextMeasure("HH", false);
        NodeId node1 = tree.newLeafWithMeasure(node1Style, node1Measure);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(50.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(40.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_blocked_by_line_box__content_box")
    void blockMarginYCollapseThroughBlockedByLineBoxContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        MeasureFunc node1Measure = ahemTextMeasure("HH", false);
        NodeId node1 = tree.newLeafWithMeasure(node1Style, node1Measure);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(50.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(40.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_blocked_by_line_box_with_height_zero__border_box")
    void blockMarginYCollapseThroughBlockedByLineBoxWithHeightZeroBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(0.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        MeasureFunc node1Measure = ahemTextMeasure("HH", false);
        NodeId node1 = tree.newLeafWithMeasure(node1Style, node1Measure);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

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
    @DisplayName("block_margin_y_collapse_through_blocked_by_line_box_with_height_zero__content_box")
    void blockMarginYCollapseThroughBlockedByLineBoxWithHeightZeroContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(0.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        MeasureFunc node1Measure = ahemTextMeasure("HH", false);
        NodeId node1 = tree.newLeafWithMeasure(node1Style, node1Measure);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

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
    @DisplayName("block_margin_y_collapse_through_blocked_by_line_box_with_max_height_zero__border_box")
    void blockMarginYCollapseThroughBlockedByLineBoxWithMaxHeightZeroBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.maxSize = new Size<>(Dimension.AUTO, Dimension.length(0.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        MeasureFunc node1Measure = ahemTextMeasure("HH", false);
        NodeId node1 = tree.newLeafWithMeasure(node1Style, node1Measure);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

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
    @DisplayName("block_margin_y_collapse_through_blocked_by_line_box_with_max_height_zero__content_box")
    void blockMarginYCollapseThroughBlockedByLineBoxWithMaxHeightZeroContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.maxSize = new Size<>(Dimension.AUTO, Dimension.length(0.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        MeasureFunc node1Measure = ahemTextMeasure("HH", false);
        NodeId node1 = tree.newLeafWithMeasure(node1Style, node1Measure);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

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
    @DisplayName("block_margin_y_collapse_through_blocked_by_min_height__border_box")
    void blockMarginYCollapseThroughBlockedByMinHeightBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.minSize = new Size<>(Dimension.AUTO, Dimension.length(1.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(41.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(1.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(31.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_blocked_by_min_height__content_box")
    void blockMarginYCollapseThroughBlockedByMinHeightContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.minSize = new Size<>(Dimension.AUTO, Dimension.length(1.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(41.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(1.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(31.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_blocked_by_overflow_x_hidden__border_box")
    void blockMarginYCollapseThroughBlockedByOverflowXHiddenBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        node1Style.overflow = new Point<>(Overflow.HIDDEN, Overflow.VISIBLE);
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

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
    @DisplayName("block_margin_y_collapse_through_blocked_by_overflow_x_hidden__content_box")
    void blockMarginYCollapseThroughBlockedByOverflowXHiddenContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        node1Style.overflow = new Point<>(Overflow.HIDDEN, Overflow.VISIBLE);
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

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
    @DisplayName("block_margin_y_collapse_through_blocked_by_overflow_x_scroll__border_box")
    void blockMarginYCollapseThroughBlockedByOverflowXScrollBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        node1Style.overflow = new Point<>(Overflow.SCROLL, Overflow.VISIBLE);
        node1Style.scrollbarWidth = 15.0f;
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(55.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(15.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(45.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_blocked_by_overflow_x_scroll__content_box")
    void blockMarginYCollapseThroughBlockedByOverflowXScrollContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        node1Style.overflow = new Point<>(Overflow.SCROLL, Overflow.VISIBLE);
        node1Style.scrollbarWidth = 15.0f;
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(55.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(15.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(45.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_blocked_by_overflow_y_hidden__border_box")
    void blockMarginYCollapseThroughBlockedByOverflowYHiddenBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        node1Style.overflow = new Point<>(Overflow.VISIBLE, Overflow.HIDDEN);
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

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
    @DisplayName("block_margin_y_collapse_through_blocked_by_overflow_y_hidden__content_box")
    void blockMarginYCollapseThroughBlockedByOverflowYHiddenContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        node1Style.overflow = new Point<>(Overflow.VISIBLE, Overflow.HIDDEN);
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

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
    @DisplayName("block_margin_y_collapse_through_blocked_by_overflow_y_scroll__border_box")
    void blockMarginYCollapseThroughBlockedByOverflowYScrollBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        node1Style.overflow = new Point<>(Overflow.VISIBLE, Overflow.SCROLL);
        node1Style.scrollbarWidth = 15.0f;
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

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
    @DisplayName("block_margin_y_collapse_through_blocked_by_overflow_y_scroll__content_box")
    void blockMarginYCollapseThroughBlockedByOverflowYScrollContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        node1Style.overflow = new Point<>(Overflow.VISIBLE, Overflow.SCROLL);
        node1Style.scrollbarWidth = 15.0f;
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

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
    @DisplayName("block_margin_y_collapse_through_blocked_by_padding_bottom__border_box")
    void blockMarginYCollapseThroughBlockedByPaddingBottomBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        node1Style.padding = new Rect<>(LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.length(1.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(41.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(1.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(31.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_blocked_by_padding_bottom__content_box")
    void blockMarginYCollapseThroughBlockedByPaddingBottomContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        node1Style.padding = new Rect<>(LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.length(1.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(41.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(1.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(31.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_blocked_by_padding_top__border_box")
    void blockMarginYCollapseThroughBlockedByPaddingTopBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        node1Style.padding = new Rect<>(LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.length(1.0f), LengthPercentage.ZERO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(41.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(1.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(31.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_blocked_by_padding_top__content_box")
    void blockMarginYCollapseThroughBlockedByPaddingTopContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        node1Style.padding = new Rect<>(LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.length(1.0f), LengthPercentage.ZERO);
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(41.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(1.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(31.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_negative__border_box")
    void blockMarginYCollapseThroughNegativeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-7.0f), LengthPercentageAuto.length(-3.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-2.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(13.0f, nodeLayout.size().height, "height of node");
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
        assertEquals(3.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(3.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_negative__content_box")
    void blockMarginYCollapseThroughNegativeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-7.0f), LengthPercentageAuto.length(-3.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-2.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(13.0f, nodeLayout.size().height, "height of node");
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
        assertEquals(3.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(3.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_positive__border_box")
    void blockMarginYCollapseThroughPositiveBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
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
        assertEquals(20.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_positive__content_box")
    void blockMarginYCollapseThroughPositiveContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
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
        assertEquals(20.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_positive_and_negative__border_box")
    void blockMarginYCollapseThroughPositiveAndNegativeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(-4.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(7.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(17.0f, nodeLayout.size().height, "height of node");
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
        assertEquals(5.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(7.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_positive_and_negative__content_box")
    void blockMarginYCollapseThroughPositiveAndNegativeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(-4.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(7.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(17.0f, nodeLayout.size().height, "height of node");
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
        assertEquals(5.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(7.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_with_absolute_child__border_box")
    void blockMarginYCollapseThroughWithAbsoluteChildBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.position = Position.ABSOLUTE;
        MeasureFunc node10Measure = ahemTextMeasure("HH", false);
        NodeId node10 = tree.newLeafWithMeasure(node10Style, node10Measure);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
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
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(20.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(0.0f, node10Layout.location().y, "y of node10");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(20.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_collapse_through_with_absolute_child__content_box")
    void blockMarginYCollapseThroughWithAbsoluteChildContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.boxSizing = BoxSizing.CONTENT_BOX;
        node10Style.position = Position.ABSOLUTE;
        MeasureFunc node10Measure = ahemTextMeasure("HH", false);
        NodeId node10 = tree.newLeafWithMeasure(node10Style, node10Measure);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
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
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(20.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(0.0f, node10Layout.location().y, "y of node10");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(20.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_margin_y_first_child_collapse_blocked_by_border_top__border_box")
    void blockMarginYFirstChildCollapseBlockedByBorderTopBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
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
    @DisplayName("block_margin_y_first_child_collapse_blocked_by_border_top__content_box")
    void blockMarginYFirstChildCollapseBlockedByBorderTopContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
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
    @DisplayName("block_margin_y_first_child_collapse_blocked_by_overflow_x_hidden__border_box")
    void blockMarginYFirstChildCollapseBlockedByOverflowXHiddenBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        node0Style.overflow = new Point<>(Overflow.HIDDEN, Overflow.VISIBLE);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

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
    @DisplayName("block_margin_y_first_child_collapse_blocked_by_overflow_x_hidden__content_box")
    void blockMarginYFirstChildCollapseBlockedByOverflowXHiddenContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        node0Style.overflow = new Point<>(Overflow.HIDDEN, Overflow.VISIBLE);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

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
    @DisplayName("block_margin_y_first_child_collapse_blocked_by_overflow_x_scroll__border_box")
    void blockMarginYFirstChildCollapseBlockedByOverflowXScrollBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        node0Style.overflow = new Point<>(Overflow.SCROLL, Overflow.VISIBLE);
        node0Style.scrollbarWidth = 15.0f;
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(45.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(35.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_first_child_collapse_blocked_by_overflow_x_scroll__content_box")
    void blockMarginYFirstChildCollapseBlockedByOverflowXScrollContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        node0Style.overflow = new Point<>(Overflow.SCROLL, Overflow.VISIBLE);
        node0Style.scrollbarWidth = 15.0f;
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(45.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(35.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_first_child_collapse_blocked_by_overflow_y_hidden__border_box")
    void blockMarginYFirstChildCollapseBlockedByOverflowYHiddenBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        node0Style.overflow = new Point<>(Overflow.VISIBLE, Overflow.HIDDEN);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

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
    @DisplayName("block_margin_y_first_child_collapse_blocked_by_overflow_y_hidden__content_box")
    void blockMarginYFirstChildCollapseBlockedByOverflowYHiddenContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        node0Style.overflow = new Point<>(Overflow.VISIBLE, Overflow.HIDDEN);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

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
    @DisplayName("block_margin_y_first_child_collapse_blocked_by_overflow_y_scroll__border_box")
    void blockMarginYFirstChildCollapseBlockedByOverflowYScrollBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        node0Style.overflow = new Point<>(Overflow.VISIBLE, Overflow.SCROLL);
        node0Style.scrollbarWidth = 15.0f;
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

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
        assertEquals(35.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(10.0f, node00Layout.location().y, "y of node00");
        Layout node000Layout = tree.getLayout(node000);
        assertEquals(35.0f, node000Layout.size().width, "width of node000");
        assertEquals(10.0f, node000Layout.size().height, "height of node000");
        assertEquals(0.0f, node000Layout.location().x, "x of node000");
        assertEquals(0.0f, node000Layout.location().y, "y of node000");
    }

    @Test
    @DisplayName("block_margin_y_first_child_collapse_blocked_by_overflow_y_scroll__content_box")
    void blockMarginYFirstChildCollapseBlockedByOverflowYScrollContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        node0Style.overflow = new Point<>(Overflow.VISIBLE, Overflow.SCROLL);
        node0Style.scrollbarWidth = 15.0f;
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

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
        assertEquals(35.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(10.0f, node00Layout.location().y, "y of node00");
        Layout node000Layout = tree.getLayout(node000);
        assertEquals(35.0f, node000Layout.size().width, "width of node000");
        assertEquals(10.0f, node000Layout.size().height, "height of node000");
        assertEquals(0.0f, node000Layout.location().x, "x of node000");
        assertEquals(0.0f, node000Layout.location().y, "y of node000");
    }

    @Test
    @DisplayName("block_margin_y_first_child_collapse_blocked_by_padding_top__border_box")
    void blockMarginYFirstChildCollapseBlockedByPaddingTopBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        node0Style.padding = new Rect<>(LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.length(1.0f), LengthPercentage.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(31.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(21.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(50.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(11.0f, node00Layout.location().y, "y of node00");
        Layout node000Layout = tree.getLayout(node000);
        assertEquals(50.0f, node000Layout.size().width, "width of node000");
        assertEquals(10.0f, node000Layout.size().height, "height of node000");
        assertEquals(0.0f, node000Layout.location().x, "x of node000");
        assertEquals(0.0f, node000Layout.location().y, "y of node000");
    }

    @Test
    @DisplayName("block_margin_y_first_child_collapse_blocked_by_padding_top__content_box")
    void blockMarginYFirstChildCollapseBlockedByPaddingTopContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        node0Style.padding = new Rect<>(LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.length(1.0f), LengthPercentage.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(31.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(21.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(50.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(11.0f, node00Layout.location().y, "y of node00");
        Layout node000Layout = tree.getLayout(node000);
        assertEquals(50.0f, node000Layout.size().width, "width of node000");
        assertEquals(10.0f, node000Layout.size().height, "height of node000");
        assertEquals(0.0f, node000Layout.location().x, "x of node000");
        assertEquals(0.0f, node000Layout.location().y, "y of node000");
    }

    @Test
    @DisplayName("block_margin_y_first_child_collapse_negative_equal__border_box")
    void blockMarginYFirstChildCollapseNegativeEqualBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-10.0f, node0Layout.location().y, "y of node0");
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
    @DisplayName("block_margin_y_first_child_collapse_negative_equal__content_box")
    void blockMarginYFirstChildCollapseNegativeEqualContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-10.0f, node0Layout.location().y, "y of node0");
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
    @DisplayName("block_margin_y_first_child_collapse_negative_parent_larger__border_box")
    void blockMarginYFirstChildCollapseNegativeParentLargerBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-10.0f, node0Layout.location().y, "y of node0");
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
    @DisplayName("block_margin_y_first_child_collapse_negative_parent_larger__content_box")
    void blockMarginYFirstChildCollapseNegativeParentLargerContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-10.0f, node0Layout.location().y, "y of node0");
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
    @DisplayName("block_margin_y_first_child_collapse_negative_parent_smaller__border_box")
    void blockMarginYFirstChildCollapseNegativeParentSmallerBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-10.0f, node0Layout.location().y, "y of node0");
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
    @DisplayName("block_margin_y_first_child_collapse_negative_parent_smaller__content_box")
    void blockMarginYFirstChildCollapseNegativeParentSmallerContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-10.0f, node0Layout.location().y, "y of node0");
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
    @DisplayName("block_margin_y_first_child_collapse_not_blocked_by_border_bottom__border_box")
    void blockMarginYFirstChildCollapseNotBlockedByBorderBottomBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
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
    @DisplayName("block_margin_y_first_child_collapse_not_blocked_by_border_bottom__content_box")
    void blockMarginYFirstChildCollapseNotBlockedByBorderBottomContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
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
    @DisplayName("block_margin_y_first_child_collapse_not_blocked_by_padding_bottom__border_box")
    void blockMarginYFirstChildCollapseNotBlockedByPaddingBottomBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        node0Style.padding = new Rect<>(LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.length(1.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(21.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(11.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
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
    @DisplayName("block_margin_y_first_child_collapse_not_blocked_by_padding_bottom__content_box")
    void blockMarginYFirstChildCollapseNotBlockedByPaddingBottomContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        node0Style.padding = new Rect<>(LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.length(1.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(21.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(11.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
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
    @DisplayName("block_margin_y_first_child_collapse_positive_and_negative__border_box")
    void blockMarginYFirstChildCollapsePositiveAndNegativeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style node100Style = new Style();
        node100Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node100 = tree.newLeaf(node100Style);

        Style node10Style = new Style();
        node10Style.display = Display.BLOCK;
        node10Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f), LengthPercentageAuto.ZERO);
        NodeId node10 = tree.newWithChildren(node10Style, node100);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style node200Style = new Style();
        node200Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node200 = tree.newLeaf(node200Style);

        Style node20Style = new Style();
        node20Style.display = Display.BLOCK;
        node20Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node20 = tree.newWithChildren(node20Style, node200);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newWithChildren(node2Style, node20);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(5.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(0.0f, node10Layout.location().y, "y of node10");
        Layout node100Layout = tree.getLayout(node100);
        assertEquals(50.0f, node100Layout.size().width, "width of node100");
        assertEquals(10.0f, node100Layout.size().height, "height of node100");
        assertEquals(0.0f, node100Layout.location().x, "x of node100");
        assertEquals(0.0f, node100Layout.location().y, "y of node100");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(20.0f, node2Layout.location().y, "y of node2");
        Layout node20Layout = tree.getLayout(node20);
        assertEquals(50.0f, node20Layout.size().width, "width of node20");
        assertEquals(10.0f, node20Layout.size().height, "height of node20");
        assertEquals(0.0f, node20Layout.location().x, "x of node20");
        assertEquals(0.0f, node20Layout.location().y, "y of node20");
        Layout node200Layout = tree.getLayout(node200);
        assertEquals(50.0f, node200Layout.size().width, "width of node200");
        assertEquals(10.0f, node200Layout.size().height, "height of node200");
        assertEquals(0.0f, node200Layout.location().x, "x of node200");
        assertEquals(0.0f, node200Layout.location().y, "y of node200");
    }

    @Test
    @DisplayName("block_margin_y_first_child_collapse_positive_and_negative__content_box")
    void blockMarginYFirstChildCollapsePositiveAndNegativeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style node100Style = new Style();
        node100Style.boxSizing = BoxSizing.CONTENT_BOX;
        node100Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node100 = tree.newLeaf(node100Style);

        Style node10Style = new Style();
        node10Style.boxSizing = BoxSizing.CONTENT_BOX;
        node10Style.display = Display.BLOCK;
        node10Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f), LengthPercentageAuto.ZERO);
        NodeId node10 = tree.newWithChildren(node10Style, node100);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style node200Style = new Style();
        node200Style.boxSizing = BoxSizing.CONTENT_BOX;
        node200Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node200 = tree.newLeaf(node200Style);

        Style node20Style = new Style();
        node20Style.boxSizing = BoxSizing.CONTENT_BOX;
        node20Style.display = Display.BLOCK;
        node20Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node20 = tree.newWithChildren(node20Style, node200);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newWithChildren(node2Style, node20);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(5.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(0.0f, node10Layout.location().y, "y of node10");
        Layout node100Layout = tree.getLayout(node100);
        assertEquals(50.0f, node100Layout.size().width, "width of node100");
        assertEquals(10.0f, node100Layout.size().height, "height of node100");
        assertEquals(0.0f, node100Layout.location().x, "x of node100");
        assertEquals(0.0f, node100Layout.location().y, "y of node100");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(20.0f, node2Layout.location().y, "y of node2");
        Layout node20Layout = tree.getLayout(node20);
        assertEquals(50.0f, node20Layout.size().width, "width of node20");
        assertEquals(10.0f, node20Layout.size().height, "height of node20");
        assertEquals(0.0f, node20Layout.location().x, "x of node20");
        assertEquals(0.0f, node20Layout.location().y, "y of node20");
        Layout node200Layout = tree.getLayout(node200);
        assertEquals(50.0f, node200Layout.size().width, "width of node200");
        assertEquals(10.0f, node200Layout.size().height, "height of node200");
        assertEquals(0.0f, node200Layout.location().x, "x of node200");
        assertEquals(0.0f, node200Layout.location().y, "y of node200");
    }

    @Test
    @DisplayName("block_margin_y_first_child_collapse_positive_equal__border_box")
    void blockMarginYFirstChildCollapsePositiveEqualBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
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
    @DisplayName("block_margin_y_first_child_collapse_positive_equal__content_box")
    void blockMarginYFirstChildCollapsePositiveEqualContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
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
    @DisplayName("block_margin_y_first_child_collapse_positive_parent_larger__border_box")
    void blockMarginYFirstChildCollapsePositiveParentLargerBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
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
    @DisplayName("block_margin_y_first_child_collapse_positive_parent_larger__content_box")
    void blockMarginYFirstChildCollapsePositiveParentLargerContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
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
    @DisplayName("block_margin_y_first_child_collapse_positive_parent_smaller__border_box")
    void blockMarginYFirstChildCollapsePositiveParentSmallerBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
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
    @DisplayName("block_margin_y_first_child_collapse_positive_parent_smaller__content_box")
    void blockMarginYFirstChildCollapsePositiveParentSmallerContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
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
    @DisplayName("block_margin_y_first_granchild_collapse_positive_and_negative__border_box")
    void blockMarginYFirstGranchildCollapsePositiveAndNegativeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0000Style = new Style();
        node0000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0000 = tree.newLeaf(node0000Style);

        Style node000Style = new Style();
        node000Style.display = Display.BLOCK;
        node000Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node000 = tree.newWithChildren(node000Style, node0000);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style node1000Style = new Style();
        node1000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1000 = tree.newLeaf(node1000Style);

        Style node100Style = new Style();
        node100Style.display = Display.BLOCK;
        node100Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-2.0f), LengthPercentageAuto.ZERO);
        NodeId node100 = tree.newWithChildren(node100Style, node1000);

        Style node10Style = new Style();
        node10Style.display = Display.BLOCK;
        node10Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f), LengthPercentageAuto.ZERO);
        NodeId node10 = tree.newWithChildren(node10Style, node100);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style node2000Style = new Style();
        node2000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node2000 = tree.newLeaf(node2000Style);

        Style node200Style = new Style();
        node200Style.display = Display.BLOCK;
        node200Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(15.0f), LengthPercentageAuto.ZERO);
        NodeId node200 = tree.newWithChildren(node200Style, node2000);

        Style node20Style = new Style();
        node20Style.display = Display.BLOCK;
        node20Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f), LengthPercentageAuto.ZERO);
        NodeId node20 = tree.newWithChildren(node20Style, node200);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newWithChildren(node2Style, node20);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
        Layout node0000Layout = tree.getLayout(node0000);
        assertEquals(50.0f, node0000Layout.size().width, "width of node0000");
        assertEquals(10.0f, node0000Layout.size().height, "height of node0000");
        assertEquals(0.0f, node0000Layout.location().x, "x of node0000");
        assertEquals(0.0f, node0000Layout.location().y, "y of node0000");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(5.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(0.0f, node10Layout.location().y, "y of node10");
        Layout node100Layout = tree.getLayout(node100);
        assertEquals(50.0f, node100Layout.size().width, "width of node100");
        assertEquals(10.0f, node100Layout.size().height, "height of node100");
        assertEquals(0.0f, node100Layout.location().x, "x of node100");
        assertEquals(0.0f, node100Layout.location().y, "y of node100");
        Layout node1000Layout = tree.getLayout(node1000);
        assertEquals(50.0f, node1000Layout.size().width, "width of node1000");
        assertEquals(10.0f, node1000Layout.size().height, "height of node1000");
        assertEquals(0.0f, node1000Layout.location().x, "x of node1000");
        assertEquals(0.0f, node1000Layout.location().y, "y of node1000");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(20.0f, node2Layout.location().y, "y of node2");
        Layout node20Layout = tree.getLayout(node20);
        assertEquals(50.0f, node20Layout.size().width, "width of node20");
        assertEquals(10.0f, node20Layout.size().height, "height of node20");
        assertEquals(0.0f, node20Layout.location().x, "x of node20");
        assertEquals(0.0f, node20Layout.location().y, "y of node20");
        Layout node200Layout = tree.getLayout(node200);
        assertEquals(50.0f, node200Layout.size().width, "width of node200");
        assertEquals(10.0f, node200Layout.size().height, "height of node200");
        assertEquals(0.0f, node200Layout.location().x, "x of node200");
        assertEquals(0.0f, node200Layout.location().y, "y of node200");
        Layout node2000Layout = tree.getLayout(node2000);
        assertEquals(50.0f, node2000Layout.size().width, "width of node2000");
        assertEquals(10.0f, node2000Layout.size().height, "height of node2000");
        assertEquals(0.0f, node2000Layout.location().x, "x of node2000");
        assertEquals(0.0f, node2000Layout.location().y, "y of node2000");
    }

    @Test
    @DisplayName("block_margin_y_first_granchild_collapse_positive_and_negative__content_box")
    void blockMarginYFirstGranchildCollapsePositiveAndNegativeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0000Style = new Style();
        node0000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0000 = tree.newLeaf(node0000Style);

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.display = Display.BLOCK;
        node000Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node000 = tree.newWithChildren(node000Style, node0000);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style node1000Style = new Style();
        node1000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1000 = tree.newLeaf(node1000Style);

        Style node100Style = new Style();
        node100Style.boxSizing = BoxSizing.CONTENT_BOX;
        node100Style.display = Display.BLOCK;
        node100Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-2.0f), LengthPercentageAuto.ZERO);
        NodeId node100 = tree.newWithChildren(node100Style, node1000);

        Style node10Style = new Style();
        node10Style.boxSizing = BoxSizing.CONTENT_BOX;
        node10Style.display = Display.BLOCK;
        node10Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f), LengthPercentageAuto.ZERO);
        NodeId node10 = tree.newWithChildren(node10Style, node100);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style node2000Style = new Style();
        node2000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node2000 = tree.newLeaf(node2000Style);

        Style node200Style = new Style();
        node200Style.boxSizing = BoxSizing.CONTENT_BOX;
        node200Style.display = Display.BLOCK;
        node200Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(15.0f), LengthPercentageAuto.ZERO);
        NodeId node200 = tree.newWithChildren(node200Style, node2000);

        Style node20Style = new Style();
        node20Style.boxSizing = BoxSizing.CONTENT_BOX;
        node20Style.display = Display.BLOCK;
        node20Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f), LengthPercentageAuto.ZERO);
        NodeId node20 = tree.newWithChildren(node20Style, node200);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newWithChildren(node2Style, node20);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
        Layout node0000Layout = tree.getLayout(node0000);
        assertEquals(50.0f, node0000Layout.size().width, "width of node0000");
        assertEquals(10.0f, node0000Layout.size().height, "height of node0000");
        assertEquals(0.0f, node0000Layout.location().x, "x of node0000");
        assertEquals(0.0f, node0000Layout.location().y, "y of node0000");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(5.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(0.0f, node10Layout.location().y, "y of node10");
        Layout node100Layout = tree.getLayout(node100);
        assertEquals(50.0f, node100Layout.size().width, "width of node100");
        assertEquals(10.0f, node100Layout.size().height, "height of node100");
        assertEquals(0.0f, node100Layout.location().x, "x of node100");
        assertEquals(0.0f, node100Layout.location().y, "y of node100");
        Layout node1000Layout = tree.getLayout(node1000);
        assertEquals(50.0f, node1000Layout.size().width, "width of node1000");
        assertEquals(10.0f, node1000Layout.size().height, "height of node1000");
        assertEquals(0.0f, node1000Layout.location().x, "x of node1000");
        assertEquals(0.0f, node1000Layout.location().y, "y of node1000");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(20.0f, node2Layout.location().y, "y of node2");
        Layout node20Layout = tree.getLayout(node20);
        assertEquals(50.0f, node20Layout.size().width, "width of node20");
        assertEquals(10.0f, node20Layout.size().height, "height of node20");
        assertEquals(0.0f, node20Layout.location().x, "x of node20");
        assertEquals(0.0f, node20Layout.location().y, "y of node20");
        Layout node200Layout = tree.getLayout(node200);
        assertEquals(50.0f, node200Layout.size().width, "width of node200");
        assertEquals(10.0f, node200Layout.size().height, "height of node200");
        assertEquals(0.0f, node200Layout.location().x, "x of node200");
        assertEquals(0.0f, node200Layout.location().y, "y of node200");
        Layout node2000Layout = tree.getLayout(node2000);
        assertEquals(50.0f, node2000Layout.size().width, "width of node2000");
        assertEquals(10.0f, node2000Layout.size().height, "height of node2000");
        assertEquals(0.0f, node2000Layout.location().x, "x of node2000");
        assertEquals(0.0f, node2000Layout.location().y, "y of node2000");
    }

    @Test
    @DisplayName("block_margin_y_first_granchild_collapse_positive_equal__border_box")
    void blockMarginYFirstGranchildCollapsePositiveEqualBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0000Style = new Style();
        node0000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0000 = tree.newLeaf(node0000Style);

        Style node000Style = new Style();
        node000Style.display = Display.BLOCK;
        node000Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node000 = tree.newWithChildren(node000Style, node0000);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
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
        Layout node0000Layout = tree.getLayout(node0000);
        assertEquals(50.0f, node0000Layout.size().width, "width of node0000");
        assertEquals(10.0f, node0000Layout.size().height, "height of node0000");
        assertEquals(0.0f, node0000Layout.location().x, "x of node0000");
        assertEquals(0.0f, node0000Layout.location().y, "y of node0000");
    }

    @Test
    @DisplayName("block_margin_y_first_granchild_collapse_positive_equal__content_box")
    void blockMarginYFirstGranchildCollapsePositiveEqualContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0000Style = new Style();
        node0000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0000 = tree.newLeaf(node0000Style);

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.display = Display.BLOCK;
        node000Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node000 = tree.newWithChildren(node000Style, node0000);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
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
        Layout node0000Layout = tree.getLayout(node0000);
        assertEquals(50.0f, node0000Layout.size().width, "width of node0000");
        assertEquals(10.0f, node0000Layout.size().height, "height of node0000");
        assertEquals(0.0f, node0000Layout.location().x, "x of node0000");
        assertEquals(0.0f, node0000Layout.location().y, "y of node0000");
    }

    @Test
    @DisplayName("block_margin_y_last_child_collapse_blocked_by_border_bottom__border_box")
    void blockMarginYLastChildCollapseBlockedByBorderBottomBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_child_collapse_blocked_by_border_bottom__content_box")
    void blockMarginYLastChildCollapseBlockedByBorderBottomContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_child_collapse_blocked_by_overflow_x_hidden__border_box")
    void blockMarginYLastChildCollapseBlockedByOverflowXHiddenBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        node0Style.overflow = new Point<>(Overflow.HIDDEN, Overflow.VISIBLE);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

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
    @DisplayName("block_margin_y_last_child_collapse_blocked_by_overflow_x_hidden__content_box")
    void blockMarginYLastChildCollapseBlockedByOverflowXHiddenContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        node0Style.overflow = new Point<>(Overflow.HIDDEN, Overflow.VISIBLE);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

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
    @DisplayName("block_margin_y_last_child_collapse_blocked_by_overflow_x_scroll__border_box")
    void blockMarginYLastChildCollapseBlockedByOverflowXScrollBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        node0Style.overflow = new Point<>(Overflow.SCROLL, Overflow.VISIBLE);
        node0Style.scrollbarWidth = 15.0f;
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(45.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(35.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_child_collapse_blocked_by_overflow_x_scroll__content_box")
    void blockMarginYLastChildCollapseBlockedByOverflowXScrollContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        node0Style.overflow = new Point<>(Overflow.SCROLL, Overflow.VISIBLE);
        node0Style.scrollbarWidth = 15.0f;
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(45.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(35.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_child_collapse_blocked_by_overflow_y_hidden__border_box")
    void blockMarginYLastChildCollapseBlockedByOverflowYHiddenBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        node0Style.overflow = new Point<>(Overflow.VISIBLE, Overflow.HIDDEN);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

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
    @DisplayName("block_margin_y_last_child_collapse_blocked_by_overflow_y_hidden__content_box")
    void blockMarginYLastChildCollapseBlockedByOverflowYHiddenContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        node0Style.overflow = new Point<>(Overflow.VISIBLE, Overflow.HIDDEN);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

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
    @DisplayName("block_margin_y_last_child_collapse_blocked_by_overflow_y_scroll__border_box")
    void blockMarginYLastChildCollapseBlockedByOverflowYScrollBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        node0Style.overflow = new Point<>(Overflow.VISIBLE, Overflow.SCROLL);
        node0Style.scrollbarWidth = 15.0f;
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

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
        assertEquals(35.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node000Layout = tree.getLayout(node000);
        assertEquals(35.0f, node000Layout.size().width, "width of node000");
        assertEquals(10.0f, node000Layout.size().height, "height of node000");
        assertEquals(0.0f, node000Layout.location().x, "x of node000");
        assertEquals(0.0f, node000Layout.location().y, "y of node000");
    }

    @Test
    @DisplayName("block_margin_y_last_child_collapse_blocked_by_overflow_y_scroll__content_box")
    void blockMarginYLastChildCollapseBlockedByOverflowYScrollContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        node0Style.overflow = new Point<>(Overflow.VISIBLE, Overflow.SCROLL);
        node0Style.scrollbarWidth = 15.0f;
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

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
        assertEquals(35.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node000Layout = tree.getLayout(node000);
        assertEquals(35.0f, node000Layout.size().width, "width of node000");
        assertEquals(10.0f, node000Layout.size().height, "height of node000");
        assertEquals(0.0f, node000Layout.location().x, "x of node000");
        assertEquals(0.0f, node000Layout.location().y, "y of node000");
    }

    @Test
    @DisplayName("block_margin_y_last_child_collapse_blocked_by_padding_bottom__border_box")
    void blockMarginYLastChildCollapseBlockedByPaddingBottomBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        node0Style.padding = new Rect<>(LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.length(1.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(31.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(21.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_child_collapse_blocked_by_padding_bottom__content_box")
    void blockMarginYLastChildCollapseBlockedByPaddingBottomContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        node0Style.padding = new Rect<>(LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.length(1.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(31.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(21.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_child_collapse_negative_equal__border_box")
    void blockMarginYLastChildCollapseNegativeEqualBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_child_collapse_negative_equal__content_box")
    void blockMarginYLastChildCollapseNegativeEqualContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_child_collapse_negative_parent_larger__border_box")
    void blockMarginYLastChildCollapseNegativeParentLargerBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_child_collapse_negative_parent_larger__content_box")
    void blockMarginYLastChildCollapseNegativeParentLargerContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_child_collapse_negative_parent_smaller__border_box")
    void blockMarginYLastChildCollapseNegativeParentSmallerBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_child_collapse_negative_parent_smaller__content_box")
    void blockMarginYLastChildCollapseNegativeParentSmallerContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_child_collapse_not_blocked_by_border_top__border_box")
    void blockMarginYLastChildCollapseNotBlockedByBorderTopBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_child_collapse_not_blocked_by_border_top__content_box")
    void blockMarginYLastChildCollapseNotBlockedByBorderTopContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_child_collapse_not_blocked_by_padding_top__border_box")
    void blockMarginYLastChildCollapseNotBlockedByPaddingTopBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        node0Style.padding = new Rect<>(LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.length(1.0f), LengthPercentage.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(21.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(11.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(50.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(1.0f, node00Layout.location().y, "y of node00");
        Layout node000Layout = tree.getLayout(node000);
        assertEquals(50.0f, node000Layout.size().width, "width of node000");
        assertEquals(10.0f, node000Layout.size().height, "height of node000");
        assertEquals(0.0f, node000Layout.location().x, "x of node000");
        assertEquals(0.0f, node000Layout.location().y, "y of node000");
    }

    @Test
    @DisplayName("block_margin_y_last_child_collapse_not_blocked_by_padding_top__content_box")
    void blockMarginYLastChildCollapseNotBlockedByPaddingTopContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        node0Style.padding = new Rect<>(LengthPercentage.ZERO, LengthPercentage.ZERO, LengthPercentage.length(1.0f), LengthPercentage.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(21.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(11.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(50.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(1.0f, node00Layout.location().y, "y of node00");
        Layout node000Layout = tree.getLayout(node000);
        assertEquals(50.0f, node000Layout.size().width, "width of node000");
        assertEquals(10.0f, node000Layout.size().height, "height of node000");
        assertEquals(0.0f, node000Layout.location().x, "x of node000");
        assertEquals(0.0f, node000Layout.location().y, "y of node000");
    }

    @Test
    @DisplayName("block_margin_y_last_child_collapse_positive_and_negative__border_box")
    void blockMarginYLastChildCollapsePositiveAndNegativeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style node100Style = new Style();
        node100Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node100 = tree.newLeaf(node100Style);

        Style node10Style = new Style();
        node10Style.display = Display.BLOCK;
        node10Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f));
        NodeId node10 = tree.newWithChildren(node10Style, node100);

        Style node1Style = new Style();
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style node200Style = new Style();
        node200Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node200 = tree.newLeaf(node200Style);

        Style node20Style = new Style();
        node20Style.display = Display.BLOCK;
        node20Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node20 = tree.newWithChildren(node20Style, node200);

        Style node2Style = new Style();
        node2Style.display = Display.BLOCK;
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f));
        NodeId node2 = tree.newWithChildren(node2Style, node20);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(0.0f, node10Layout.location().y, "y of node10");
        Layout node100Layout = tree.getLayout(node100);
        assertEquals(50.0f, node100Layout.size().width, "width of node100");
        assertEquals(10.0f, node100Layout.size().height, "height of node100");
        assertEquals(0.0f, node100Layout.location().x, "x of node100");
        assertEquals(0.0f, node100Layout.location().y, "y of node100");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(15.0f, node2Layout.location().y, "y of node2");
        Layout node20Layout = tree.getLayout(node20);
        assertEquals(50.0f, node20Layout.size().width, "width of node20");
        assertEquals(10.0f, node20Layout.size().height, "height of node20");
        assertEquals(0.0f, node20Layout.location().x, "x of node20");
        assertEquals(0.0f, node20Layout.location().y, "y of node20");
        Layout node200Layout = tree.getLayout(node200);
        assertEquals(50.0f, node200Layout.size().width, "width of node200");
        assertEquals(10.0f, node200Layout.size().height, "height of node200");
        assertEquals(0.0f, node200Layout.location().x, "x of node200");
        assertEquals(0.0f, node200Layout.location().y, "y of node200");
    }

    @Test
    @DisplayName("block_margin_y_last_child_collapse_positive_and_negative__content_box")
    void blockMarginYLastChildCollapsePositiveAndNegativeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style node100Style = new Style();
        node100Style.boxSizing = BoxSizing.CONTENT_BOX;
        node100Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node100 = tree.newLeaf(node100Style);

        Style node10Style = new Style();
        node10Style.boxSizing = BoxSizing.CONTENT_BOX;
        node10Style.display = Display.BLOCK;
        node10Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f));
        NodeId node10 = tree.newWithChildren(node10Style, node100);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.display = Display.BLOCK;
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10);

        Style node200Style = new Style();
        node200Style.boxSizing = BoxSizing.CONTENT_BOX;
        node200Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node200 = tree.newLeaf(node200Style);

        Style node20Style = new Style();
        node20Style.boxSizing = BoxSizing.CONTENT_BOX;
        node20Style.display = Display.BLOCK;
        node20Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node20 = tree.newWithChildren(node20Style, node200);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.display = Display.BLOCK;
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f));
        NodeId node2 = tree.newWithChildren(node2Style, node20);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(10.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(50.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(0.0f, node10Layout.location().y, "y of node10");
        Layout node100Layout = tree.getLayout(node100);
        assertEquals(50.0f, node100Layout.size().width, "width of node100");
        assertEquals(10.0f, node100Layout.size().height, "height of node100");
        assertEquals(0.0f, node100Layout.location().x, "x of node100");
        assertEquals(0.0f, node100Layout.location().y, "y of node100");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(15.0f, node2Layout.location().y, "y of node2");
        Layout node20Layout = tree.getLayout(node20);
        assertEquals(50.0f, node20Layout.size().width, "width of node20");
        assertEquals(10.0f, node20Layout.size().height, "height of node20");
        assertEquals(0.0f, node20Layout.location().x, "x of node20");
        assertEquals(0.0f, node20Layout.location().y, "y of node20");
        Layout node200Layout = tree.getLayout(node200);
        assertEquals(50.0f, node200Layout.size().width, "width of node200");
        assertEquals(10.0f, node200Layout.size().height, "height of node200");
        assertEquals(0.0f, node200Layout.location().x, "x of node200");
        assertEquals(0.0f, node200Layout.location().y, "y of node200");
    }

    @Test
    @DisplayName("block_margin_y_last_child_collapse_positive_equal__border_box")
    void blockMarginYLastChildCollapsePositiveEqualBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_child_collapse_positive_equal__content_box")
    void blockMarginYLastChildCollapsePositiveEqualContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_child_collapse_positive_parent_larger__border_box")
    void blockMarginYLastChildCollapsePositiveParentLargerBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_child_collapse_positive_parent_larger__content_box")
    void blockMarginYLastChildCollapsePositiveParentLargerContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_child_collapse_positive_parent_smaller__border_box")
    void blockMarginYLastChildCollapsePositiveParentSmallerBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_child_collapse_positive_parent_smaller__content_box")
    void blockMarginYLastChildCollapsePositiveParentSmallerContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
    @DisplayName("block_margin_y_last_granchild_collapse_positive_equal__border_box")
    void blockMarginYLastGranchildCollapsePositiveEqualBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0000Style = new Style();
        node0000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0000 = tree.newLeaf(node0000Style);

        Style node000Style = new Style();
        node000Style.display = Display.BLOCK;
        node000Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node000 = tree.newWithChildren(node000Style, node0000);

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
        Layout node0000Layout = tree.getLayout(node0000);
        assertEquals(50.0f, node0000Layout.size().width, "width of node0000");
        assertEquals(10.0f, node0000Layout.size().height, "height of node0000");
        assertEquals(0.0f, node0000Layout.location().x, "x of node0000");
        assertEquals(0.0f, node0000Layout.location().y, "y of node0000");
    }

    @Test
    @DisplayName("block_margin_y_last_granchild_collapse_positive_equal__content_box")
    void blockMarginYLastGranchildCollapsePositiveEqualContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0000Style = new Style();
        node0000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0000Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0000 = tree.newLeaf(node0000Style);

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.display = Display.BLOCK;
        node000Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node000 = tree.newWithChildren(node000Style, node0000);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newWithChildren(node00Style, node000);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
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
        Layout node0000Layout = tree.getLayout(node0000);
        assertEquals(50.0f, node0000Layout.size().width, "width of node0000");
        assertEquals(10.0f, node0000Layout.size().height, "height of node0000");
        assertEquals(0.0f, node0000Layout.location().x, "x of node0000");
        assertEquals(0.0f, node0000Layout.location().y, "y of node0000");
    }

    @Test
    @DisplayName("block_margin_y_sibling_collapse_negative__border_box")
    void blockMarginYSiblingCollapseNegativeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.length(-5.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style node3Style = new Style();
        node3Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node3Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node3 = tree.newLeaf(node3Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-10.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(-10.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(-10.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(10.0f, node3Layout.size().height, "height of node3");
        assertEquals(0.0f, node3Layout.location().x, "x of node3");
        assertEquals(-10.0f, node3Layout.location().y, "y of node3");
    }

    @Test
    @DisplayName("block_margin_y_sibling_collapse_negative__content_box")
    void blockMarginYSiblingCollapseNegativeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.length(-5.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style node3Style = new Style();
        node3Style.boxSizing = BoxSizing.CONTENT_BOX;
        node3Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node3Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node3 = tree.newLeaf(node3Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-10.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(-10.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(-10.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(10.0f, node3Layout.size().height, "height of node3");
        assertEquals(0.0f, node3Layout.location().x, "x of node3");
        assertEquals(-10.0f, node3Layout.location().y, "y of node3");
    }

    @Test
    @DisplayName("block_margin_y_sibling_collapse_negative_percentage__border_box")
    void blockMarginYSiblingCollapseNegativePercentageBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.percent(-0.1f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.percent(-0.1f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(-0.05f), LengthPercentageAuto.percent(-0.05f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style node3Style = new Style();
        node3Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node3Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.percent(-0.1f));
        NodeId node3 = tree.newLeaf(node3Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(15.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(5.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(10.0f, node3Layout.size().height, "height of node3");
        assertEquals(0.0f, node3Layout.location().x, "x of node3");
        assertEquals(10.0f, node3Layout.location().y, "y of node3");
    }

    @Test
    @DisplayName("block_margin_y_sibling_collapse_negative_percentage__content_box")
    void blockMarginYSiblingCollapseNegativePercentageContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.percent(-0.1f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.percent(-0.1f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(-0.05f), LengthPercentageAuto.percent(-0.05f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style node3Style = new Style();
        node3Style.boxSizing = BoxSizing.CONTENT_BOX;
        node3Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node3Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.percent(-0.1f));
        NodeId node3 = tree.newLeaf(node3Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(15.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(5.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(10.0f, node3Layout.size().height, "height of node3");
        assertEquals(0.0f, node3Layout.location().x, "x of node3");
        assertEquals(10.0f, node3Layout.location().y, "y of node3");
    }

    @Test
    @DisplayName("block_margin_y_sibling_collapse_positive__border_box")
    void blockMarginYSiblingCollapsePositiveBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(5.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style node3Style = new Style();
        node3Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node3Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node3 = tree.newLeaf(node3Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(90.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(30.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(50.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(10.0f, node3Layout.size().height, "height of node3");
        assertEquals(0.0f, node3Layout.location().x, "x of node3");
        assertEquals(70.0f, node3Layout.location().y, "y of node3");
    }

    @Test
    @DisplayName("block_margin_y_sibling_collapse_positive__content_box")
    void blockMarginYSiblingCollapsePositiveContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(5.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style node3Style = new Style();
        node3Style.boxSizing = BoxSizing.CONTENT_BOX;
        node3Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node3Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node3 = tree.newLeaf(node3Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(90.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(30.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(50.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(10.0f, node3Layout.size().height, "height of node3");
        assertEquals(0.0f, node3Layout.location().x, "x of node3");
        assertEquals(70.0f, node3Layout.location().y, "y of node3");
    }

    @Test
    @DisplayName("block_margin_y_sibling_collapse_positive_and_negative__border_box")
    void blockMarginYSiblingCollapsePositiveAndNegativeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.length(5.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style node3Style = new Style();
        node3Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node3Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node3 = tree.newLeaf(node3Style);

        Style node4Style = new Style();
        node4Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node4Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node4 = tree.newLeaf(node4Style);

        Style node5Style = new Style();
        node5Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node5Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(-5.0f));
        NodeId node5 = tree.newLeaf(node5Style);

        Style node6Style = new Style();
        node6Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node6Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node6 = tree.newLeaf(node6Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3, node4, node5, node6);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(90.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(35.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(10.0f, node3Layout.size().height, "height of node3");
        assertEquals(0.0f, node3Layout.location().x, "x of node3");
        assertEquals(40.0f, node3Layout.location().y, "y of node3");
        Layout node4Layout = tree.getLayout(node4);
        assertEquals(50.0f, node4Layout.size().width, "width of node4");
        assertEquals(10.0f, node4Layout.size().height, "height of node4");
        assertEquals(0.0f, node4Layout.location().x, "x of node4");
        assertEquals(50.0f, node4Layout.location().y, "y of node4");
        Layout node5Layout = tree.getLayout(node5);
        assertEquals(50.0f, node5Layout.size().width, "width of node5");
        assertEquals(10.0f, node5Layout.size().height, "height of node5");
        assertEquals(0.0f, node5Layout.location().x, "x of node5");
        assertEquals(55.0f, node5Layout.location().y, "y of node5");
        Layout node6Layout = tree.getLayout(node6);
        assertEquals(50.0f, node6Layout.size().width, "width of node6");
        assertEquals(10.0f, node6Layout.size().height, "height of node6");
        assertEquals(0.0f, node6Layout.location().x, "x of node6");
        assertEquals(70.0f, node6Layout.location().y, "y of node6");
    }

    @Test
    @DisplayName("block_margin_y_sibling_collapse_positive_and_negative__content_box")
    void blockMarginYSiblingCollapsePositiveAndNegativeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.length(5.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style node3Style = new Style();
        node3Style.boxSizing = BoxSizing.CONTENT_BOX;
        node3Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node3Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node3 = tree.newLeaf(node3Style);

        Style node4Style = new Style();
        node4Style.boxSizing = BoxSizing.CONTENT_BOX;
        node4Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node4Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node4 = tree.newLeaf(node4Style);

        Style node5Style = new Style();
        node5Style.boxSizing = BoxSizing.CONTENT_BOX;
        node5Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node5Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(-5.0f));
        NodeId node5 = tree.newLeaf(node5Style);

        Style node6Style = new Style();
        node6Style.boxSizing = BoxSizing.CONTENT_BOX;
        node6Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node6Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node6 = tree.newLeaf(node6Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3, node4, node5, node6);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(90.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(35.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(10.0f, node3Layout.size().height, "height of node3");
        assertEquals(0.0f, node3Layout.location().x, "x of node3");
        assertEquals(40.0f, node3Layout.location().y, "y of node3");
        Layout node4Layout = tree.getLayout(node4);
        assertEquals(50.0f, node4Layout.size().width, "width of node4");
        assertEquals(10.0f, node4Layout.size().height, "height of node4");
        assertEquals(0.0f, node4Layout.location().x, "x of node4");
        assertEquals(50.0f, node4Layout.location().y, "y of node4");
        Layout node5Layout = tree.getLayout(node5);
        assertEquals(50.0f, node5Layout.size().width, "width of node5");
        assertEquals(10.0f, node5Layout.size().height, "height of node5");
        assertEquals(0.0f, node5Layout.location().x, "x of node5");
        assertEquals(55.0f, node5Layout.location().y, "y of node5");
        Layout node6Layout = tree.getLayout(node6);
        assertEquals(50.0f, node6Layout.size().width, "width of node6");
        assertEquals(10.0f, node6Layout.size().height, "height of node6");
        assertEquals(0.0f, node6Layout.location().x, "x of node6");
        assertEquals(70.0f, node6Layout.location().y, "y of node6");
    }

    @Test
    @DisplayName("block_margin_y_sibling_collapse_positive_and_negative_percentage__border_box")
    void blockMarginYSiblingCollapsePositiveAndNegativePercentageBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.percent(0.1f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(-0.05f), LengthPercentageAuto.percent(0.05f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style node3Style = new Style();
        node3Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node3Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.percent(-0.1f));
        NodeId node3 = tree.newLeaf(node3Style);

        Style node4Style = new Style();
        node4Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node4Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(-0.1f));
        NodeId node4 = tree.newLeaf(node4Style);

        Style node5Style = new Style();
        node5Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node5Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.percent(-0.05f));
        NodeId node5 = tree.newLeaf(node5Style);

        Style node6Style = new Style();
        node6Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node6Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f));
        NodeId node6 = tree.newLeaf(node6Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3, node4, node5, node6);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(80.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(15.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(28.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(10.0f, node3Layout.size().height, "height of node3");
        assertEquals(0.0f, node3Layout.location().x, "x of node3");
        assertEquals(35.0f, node3Layout.location().y, "y of node3");
        Layout node4Layout = tree.getLayout(node4);
        assertEquals(50.0f, node4Layout.size().width, "width of node4");
        assertEquals(10.0f, node4Layout.size().height, "height of node4");
        assertEquals(0.0f, node4Layout.location().x, "x of node4");
        assertEquals(45.0f, node4Layout.location().y, "y of node4");
        Layout node5Layout = tree.getLayout(node5);
        assertEquals(50.0f, node5Layout.size().width, "width of node5");
        assertEquals(10.0f, node5Layout.size().height, "height of node5");
        assertEquals(0.0f, node5Layout.location().x, "x of node5");
        assertEquals(53.0f, node5Layout.location().y, "y of node5");
        Layout node6Layout = tree.getLayout(node6);
        assertEquals(50.0f, node6Layout.size().width, "width of node6");
        assertEquals(10.0f, node6Layout.size().height, "height of node6");
        assertEquals(0.0f, node6Layout.location().x, "x of node6");
        assertEquals(65.0f, node6Layout.location().y, "y of node6");
    }

    @Test
    @DisplayName("block_margin_y_sibling_collapse_positive_and_negative_percentage__content_box")
    void blockMarginYSiblingCollapsePositiveAndNegativePercentageContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.percent(0.1f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(-0.05f), LengthPercentageAuto.percent(0.05f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style node3Style = new Style();
        node3Style.boxSizing = BoxSizing.CONTENT_BOX;
        node3Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node3Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.percent(-0.1f));
        NodeId node3 = tree.newLeaf(node3Style);

        Style node4Style = new Style();
        node4Style.boxSizing = BoxSizing.CONTENT_BOX;
        node4Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node4Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(-0.1f));
        NodeId node4 = tree.newLeaf(node4Style);

        Style node5Style = new Style();
        node5Style.boxSizing = BoxSizing.CONTENT_BOX;
        node5Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node5Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.percent(-0.05f));
        NodeId node5 = tree.newLeaf(node5Style);

        Style node6Style = new Style();
        node6Style.boxSizing = BoxSizing.CONTENT_BOX;
        node6Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node6Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f));
        NodeId node6 = tree.newLeaf(node6Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3, node4, node5, node6);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(80.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(15.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(28.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(10.0f, node3Layout.size().height, "height of node3");
        assertEquals(0.0f, node3Layout.location().x, "x of node3");
        assertEquals(35.0f, node3Layout.location().y, "y of node3");
        Layout node4Layout = tree.getLayout(node4);
        assertEquals(50.0f, node4Layout.size().width, "width of node4");
        assertEquals(10.0f, node4Layout.size().height, "height of node4");
        assertEquals(0.0f, node4Layout.location().x, "x of node4");
        assertEquals(45.0f, node4Layout.location().y, "y of node4");
        Layout node5Layout = tree.getLayout(node5);
        assertEquals(50.0f, node5Layout.size().width, "width of node5");
        assertEquals(10.0f, node5Layout.size().height, "height of node5");
        assertEquals(0.0f, node5Layout.location().x, "x of node5");
        assertEquals(53.0f, node5Layout.location().y, "y of node5");
        Layout node6Layout = tree.getLayout(node6);
        assertEquals(50.0f, node6Layout.size().width, "width of node6");
        assertEquals(10.0f, node6Layout.size().height, "height of node6");
        assertEquals(0.0f, node6Layout.location().x, "x of node6");
        assertEquals(65.0f, node6Layout.location().y, "y of node6");
    }

    @Test
    @DisplayName("block_margin_y_sibling_collapse_positive_percentage__border_box")
    void blockMarginYSiblingCollapsePositivePercentageBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.percent(0.05f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style node3Style = new Style();
        node3Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node3Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f));
        NodeId node3 = tree.newLeaf(node3Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(65.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(35.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(10.0f, node3Layout.size().height, "height of node3");
        assertEquals(0.0f, node3Layout.location().x, "x of node3");
        assertEquals(50.0f, node3Layout.location().y, "y of node3");
    }

    @Test
    @DisplayName("block_margin_y_sibling_collapse_positive_percentage__content_box")
    void blockMarginYSiblingCollapsePositivePercentageContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.05f), LengthPercentageAuto.percent(0.05f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style node3Style = new Style();
        node3Style.boxSizing = BoxSizing.CONTENT_BOX;
        node3Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node3Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f));
        NodeId node3 = tree.newLeaf(node3Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(65.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(35.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(10.0f, node3Layout.size().height, "height of node3");
        assertEquals(0.0f, node3Layout.location().x, "x of node3");
        assertEquals(50.0f, node3Layout.location().y, "y of node3");
    }

    @Test
    @DisplayName("block_margin_y_simple_negative__border_box")
    void blockMarginYSimpleNegativeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-10.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(-10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_y_simple_negative__content_box")
    void blockMarginYSimpleNegativeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-10.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(-10.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_y_simple_negative_percentage_other__border_box")
    void blockMarginYSimpleNegativePercentageOtherBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.percent(-0.1f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_y_simple_negative_percentage_other__content_box")
    void blockMarginYSimpleNegativePercentageOtherContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.percent(-0.1f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_y_simple_negative_percentage_self__border_box")
    void blockMarginYSimpleNegativePercentageSelfBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.percent(-0.1f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_y_simple_negative_percentage_self__content_box")
    void blockMarginYSimpleNegativePercentageSelfContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(-0.1f), LengthPercentageAuto.percent(-0.1f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_y_simple_positive__border_box")
    void blockMarginYSimplePositiveBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(40.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(30.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_y_simple_positive__content_box")
    void blockMarginYSimplePositiveContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(40.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(30.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_y_simple_positive_percentage_other__border_box")
    void blockMarginYSimplePositivePercentageOtherBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_y_simple_positive_percentage_other__content_box")
    void blockMarginYSimplePositivePercentageOtherContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_y_simple_positive_percentage_self__border_box")
    void blockMarginYSimplePositivePercentageSelfBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_y_simple_positive_percentage_self__content_box")
    void blockMarginYSimplePositivePercentageSelfContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.percent(0.1f), LengthPercentageAuto.percent(0.1f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(5.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(20.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_margin_y_total_collapse__border_box")
    void blockMarginYTotalCollapseBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node01Style = new Style();
        node01Style.display = Display.BLOCK;
        node01Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node01 = tree.newLeaf(node01Style);

        Style node02Style = new Style();
        node02Style.display = Display.BLOCK;
        node02Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node02Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node02 = tree.newLeaf(node02Style);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00, node01, node02);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(50.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node01Layout = tree.getLayout(node01);
        assertEquals(50.0f, node01Layout.size().width, "width of node01");
        assertEquals(0.0f, node01Layout.size().height, "height of node01");
        assertEquals(0.0f, node01Layout.location().x, "x of node01");
        assertEquals(20.0f, node01Layout.location().y, "y of node01");
        Layout node02Layout = tree.getLayout(node02);
        assertEquals(50.0f, node02Layout.size().width, "width of node02");
        assertEquals(10.0f, node02Layout.size().height, "height of node02");
        assertEquals(0.0f, node02Layout.location().x, "x of node02");
        assertEquals(20.0f, node02Layout.location().y, "y of node02");
    }

    @Test
    @DisplayName("block_margin_y_total_collapse__content_box")
    void blockMarginYTotalCollapseContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node00Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node01Style = new Style();
        node01Style.boxSizing = BoxSizing.CONTENT_BOX;
        node01Style.display = Display.BLOCK;
        node01Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node01 = tree.newLeaf(node01Style);

        Style node02Style = new Style();
        node02Style.boxSizing = BoxSizing.CONTENT_BOX;
        node02Style.display = Display.BLOCK;
        node02Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node02Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO);
        NodeId node02 = tree.newLeaf(node02Style);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.length(10.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00, node01, node02);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(30.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(10.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(50.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node01Layout = tree.getLayout(node01);
        assertEquals(50.0f, node01Layout.size().width, "width of node01");
        assertEquals(0.0f, node01Layout.size().height, "height of node01");
        assertEquals(0.0f, node01Layout.location().x, "x of node01");
        assertEquals(20.0f, node01Layout.location().y, "y of node01");
        Layout node02Layout = tree.getLayout(node02);
        assertEquals(50.0f, node02Layout.size().width, "width of node02");
        assertEquals(10.0f, node02Layout.size().height, "height of node02");
        assertEquals(0.0f, node02Layout.location().x, "x of node02");
        assertEquals(20.0f, node02Layout.location().y, "y of node02");
    }

    @Test
    @DisplayName("block_margin_y_total_collapse_complex__border_box")
    void blockMarginYTotalCollapseComplexBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node10Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.length(-5.0f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node11Style = new Style();
        node11Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node11Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(7.0f), LengthPercentageAuto.length(3.0f));
        NodeId node11 = tree.newLeaf(node11Style);

        Style node12Style = new Style();
        node12Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node12Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-6.0f), LengthPercentageAuto.length(9.0f));
        NodeId node12 = tree.newLeaf(node12Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10, node11, node12);

        Style node2Style = new Style();
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.length(-5.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style node3Style = new Style();
        node3Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node3Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node3 = tree.newLeaf(node3Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-10.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(-10.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(0.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(-5.0f, node10Layout.location().y, "y of node10");
        Layout node11Layout = tree.getLayout(node11);
        assertEquals(0.0f, node11Layout.size().width, "width of node11");
        assertEquals(10.0f, node11Layout.size().height, "height of node11");
        assertEquals(0.0f, node11Layout.location().x, "x of node11");
        assertEquals(7.0f, node11Layout.location().y, "y of node11");
        Layout node12Layout = tree.getLayout(node12);
        assertEquals(0.0f, node12Layout.size().width, "width of node12");
        assertEquals(10.0f, node12Layout.size().height, "height of node12");
        assertEquals(0.0f, node12Layout.location().x, "x of node12");
        assertEquals(-6.0f, node12Layout.location().y, "y of node12");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(-10.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(10.0f, node3Layout.size().height, "height of node3");
        assertEquals(0.0f, node3Layout.location().x, "x of node3");
        assertEquals(-10.0f, node3Layout.location().y, "y of node3");
    }

    @Test
    @DisplayName("block_margin_y_total_collapse_complex__content_box")
    void blockMarginYTotalCollapseComplexContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node0Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node10Style = new Style();
        node10Style.boxSizing = BoxSizing.CONTENT_BOX;
        node10Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node10Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.length(-5.0f));
        NodeId node10 = tree.newLeaf(node10Style);

        Style node11Style = new Style();
        node11Style.boxSizing = BoxSizing.CONTENT_BOX;
        node11Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node11Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(7.0f), LengthPercentageAuto.length(3.0f));
        NodeId node11 = tree.newLeaf(node11Style);

        Style node12Style = new Style();
        node12Style.boxSizing = BoxSizing.CONTENT_BOX;
        node12Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node12Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-6.0f), LengthPercentageAuto.length(9.0f));
        NodeId node12 = tree.newLeaf(node12Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node1Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node1 = tree.newWithChildren(node1Style, node10, node11, node12);

        Style node2Style = new Style();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node2Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-5.0f), LengthPercentageAuto.length(-5.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        Style node3Style = new Style();
        node3Style.boxSizing = BoxSizing.CONTENT_BOX;
        node3Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        node3Style.margin = new Rect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.length(-10.0f), LengthPercentageAuto.length(-10.0f));
        NodeId node3 = tree.newLeaf(node3Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(0.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(-10.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(-10.0f, node1Layout.location().y, "y of node1");
        Layout node10Layout = tree.getLayout(node10);
        assertEquals(0.0f, node10Layout.size().width, "width of node10");
        assertEquals(10.0f, node10Layout.size().height, "height of node10");
        assertEquals(0.0f, node10Layout.location().x, "x of node10");
        assertEquals(-5.0f, node10Layout.location().y, "y of node10");
        Layout node11Layout = tree.getLayout(node11);
        assertEquals(0.0f, node11Layout.size().width, "width of node11");
        assertEquals(10.0f, node11Layout.size().height, "height of node11");
        assertEquals(0.0f, node11Layout.location().x, "x of node11");
        assertEquals(7.0f, node11Layout.location().y, "y of node11");
        Layout node12Layout = tree.getLayout(node12);
        assertEquals(0.0f, node12Layout.size().width, "width of node12");
        assertEquals(10.0f, node12Layout.size().height, "height of node12");
        assertEquals(0.0f, node12Layout.location().x, "x of node12");
        assertEquals(-6.0f, node12Layout.location().y, "y of node12");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(10.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(-10.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(10.0f, node3Layout.size().height, "height of node3");
        assertEquals(0.0f, node3Layout.location().x, "x of node3");
        assertEquals(-10.0f, node3Layout.location().y, "y of node3");
    }

    @Test
    @DisplayName("block_overflow_scrollbars_overridden_by_available_space__border_box")
    void blockOverflowScrollbarsOverriddenByAvailableSpaceBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.position = Position.ABSOLUTE;
        node00Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.overflow = new Point<>(Overflow.SCROLL, Overflow.SCROLL);
        node0Style.scrollbarWidth = 15.0f;
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
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
        assertEquals(15.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(0.0f, node00Layout.size().width, "width of node00");
        assertEquals(0.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_overflow_scrollbars_overridden_by_available_space__content_box")
    void blockOverflowScrollbarsOverriddenByAvailableSpaceContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.position = Position.ABSOLUTE;
        node00Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.overflow = new Point<>(Overflow.SCROLL, Overflow.SCROLL);
        node0Style.scrollbarWidth = 15.0f;
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
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
        assertEquals(15.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(0.0f, node00Layout.size().width, "width of node00");
        assertEquals(0.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_overflow_scrollbars_overridden_by_max_size__border_box")
    void blockOverflowScrollbarsOverriddenByMaxSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.maxSize = new Size<>(Dimension.length(2.0f), Dimension.length(4.0f));
        nodeStyle.overflow = new Point<>(Overflow.SCROLL, Overflow.SCROLL);
        nodeStyle.scrollbarWidth = 15.0f;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(2.0f, nodeLayout.size().width, "width of node");
        assertEquals(4.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(0.0f, node0Layout.size().width, "width of node0");
        assertEquals(0.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_overflow_scrollbars_overridden_by_max_size__content_box")
    void blockOverflowScrollbarsOverriddenByMaxSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.maxSize = new Size<>(Dimension.length(2.0f), Dimension.length(4.0f));
        nodeStyle.overflow = new Point<>(Overflow.SCROLL, Overflow.SCROLL);
        nodeStyle.scrollbarWidth = 15.0f;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(2.0f, nodeLayout.size().width, "width of node");
        assertEquals(4.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(0.0f, node0Layout.size().width, "width of node0");
        assertEquals(0.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_overflow_scrollbars_overridden_by_size__border_box")
    void blockOverflowScrollbarsOverriddenBySizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(2.0f), Dimension.length(4.0f));
        nodeStyle.overflow = new Point<>(Overflow.SCROLL, Overflow.SCROLL);
        nodeStyle.scrollbarWidth = 15.0f;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(2.0f, nodeLayout.size().width, "width of node");
        assertEquals(4.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(0.0f, node0Layout.size().width, "width of node0");
        assertEquals(0.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_overflow_scrollbars_overridden_by_size__content_box")
    void blockOverflowScrollbarsOverriddenBySizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(2.0f), Dimension.length(4.0f));
        nodeStyle.overflow = new Point<>(Overflow.SCROLL, Overflow.SCROLL);
        nodeStyle.scrollbarWidth = 15.0f;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(2.0f, nodeLayout.size().width, "width of node");
        assertEquals(4.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(0.0f, node0Layout.size().width, "width of node0");
        assertEquals(0.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_overflow_scrollbars_take_up_space_both_axis__border_box")
    void blockOverflowScrollbarsTakeUpSpaceBothAxisBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        nodeStyle.overflow = new Point<>(Overflow.SCROLL, Overflow.SCROLL);
        nodeStyle.scrollbarWidth = 15.0f;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(50.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(35.0f, node0Layout.size().width, "width of node0");
        assertEquals(35.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_overflow_scrollbars_take_up_space_both_axis__content_box")
    void blockOverflowScrollbarsTakeUpSpaceBothAxisContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        nodeStyle.overflow = new Point<>(Overflow.SCROLL, Overflow.SCROLL);
        nodeStyle.scrollbarWidth = 15.0f;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(50.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(35.0f, node0Layout.size().width, "width of node0");
        assertEquals(35.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_overflow_scrollbars_take_up_space_cross_axis__border_box")
    void blockOverflowScrollbarsTakeUpSpaceCrossAxisBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        nodeStyle.overflow = new Point<>(Overflow.VISIBLE, Overflow.SCROLL);
        nodeStyle.scrollbarWidth = 15.0f;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(50.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(35.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_overflow_scrollbars_take_up_space_cross_axis__content_box")
    void blockOverflowScrollbarsTakeUpSpaceCrossAxisContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        nodeStyle.overflow = new Point<>(Overflow.VISIBLE, Overflow.SCROLL);
        nodeStyle.scrollbarWidth = 15.0f;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(50.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(35.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_overflow_scrollbars_take_up_space_main_axis__border_box")
    void blockOverflowScrollbarsTakeUpSpaceMainAxisBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.position = Position.ABSOLUTE;
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        nodeStyle.overflow = new Point<>(Overflow.SCROLL, Overflow.VISIBLE);
        nodeStyle.scrollbarWidth = 15.0f;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(50.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(35.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_overflow_scrollbars_take_up_space_main_axis__content_box")
    void blockOverflowScrollbarsTakeUpSpaceMainAxisContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.position = Position.ABSOLUTE;
        node0Style.inset = new Rect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        nodeStyle.overflow = new Point<>(Overflow.SCROLL, Overflow.VISIBLE);
        nodeStyle.scrollbarWidth = 15.0f;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(50.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(35.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_padding_border_fixed_size__border_box")
    void blockPaddingBorderFixedSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        nodeStyle.padding = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        nodeStyle.border = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(50.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(28.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(15.0f, node0Layout.location().x, "x of node0");
        assertEquals(3.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(28.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(15.0f, node1Layout.location().x, "x of node1");
        assertEquals(13.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_padding_border_fixed_size__content_box")
    void blockPaddingBorderFixedSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        nodeStyle.padding = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        nodeStyle.border = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(72.0f, nodeLayout.size().width, "width of node");
        assertEquals(64.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(15.0f, node0Layout.location().x, "x of node0");
        assertEquals(3.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(15.0f, node1Layout.location().x, "x of node1");
        assertEquals(13.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_padding_border_intrinsic_size__border_box")
    void blockPaddingBorderIntrinsicSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.padding = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        nodeStyle.border = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(72.0f, nodeLayout.size().width, "width of node");
        assertEquals(34.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(15.0f, node0Layout.location().x, "x of node0");
        assertEquals(3.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(15.0f, node1Layout.location().x, "x of node1");
        assertEquals(13.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_padding_border_intrinsic_size__content_box")
    void blockPaddingBorderIntrinsicSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.padding = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        nodeStyle.border = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(72.0f, nodeLayout.size().width, "width of node");
        assertEquals(34.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(15.0f, node0Layout.location().x, "x of node0");
        assertEquals(3.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(15.0f, node1Layout.location().x, "x of node1");
        assertEquals(13.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_padding_border_overrides_max_size__border_box")
    void blockPaddingBorderOverridesMaxSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.maxSize = new Size<>(Dimension.length(12.0f), Dimension.length(12.0f));
        node0Style.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        node0Style.border = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(22.0f, nodeLayout.size().width, "width of node");
        assertEquals(14.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(22.0f, node0Layout.size().width, "width of node0");
        assertEquals(14.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(0.0f, node00Layout.size().width, "width of node00");
        assertEquals(0.0f, node00Layout.size().height, "height of node00");
        assertEquals(15.0f, node00Layout.location().x, "x of node00");
        assertEquals(3.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_padding_border_overrides_max_size__content_box")
    void blockPaddingBorderOverridesMaxSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.maxSize = new Size<>(Dimension.length(12.0f), Dimension.length(12.0f));
        node0Style.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        node0Style.border = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(22.0f, nodeLayout.size().width, "width of node");
        assertEquals(14.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(22.0f, node0Layout.size().width, "width of node0");
        assertEquals(14.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(0.0f, node00Layout.size().width, "width of node00");
        assertEquals(0.0f, node00Layout.size().height, "height of node00");
        assertEquals(15.0f, node00Layout.location().x, "x of node00");
        assertEquals(3.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_padding_border_overrides_min_size__border_box")
    void blockPaddingBorderOverridesMinSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.minSize = new Size<>(Dimension.length(0.0f), Dimension.length(0.0f));
        node0Style.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        node0Style.border = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(22.0f, nodeLayout.size().width, "width of node");
        assertEquals(14.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(22.0f, node0Layout.size().width, "width of node0");
        assertEquals(14.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(0.0f, node00Layout.size().width, "width of node00");
        assertEquals(0.0f, node00Layout.size().height, "height of node00");
        assertEquals(15.0f, node00Layout.location().x, "x of node00");
        assertEquals(3.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_padding_border_overrides_min_size__content_box")
    void blockPaddingBorderOverridesMinSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.minSize = new Size<>(Dimension.length(0.0f), Dimension.length(0.0f));
        node0Style.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        node0Style.border = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(22.0f, nodeLayout.size().width, "width of node");
        assertEquals(14.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(22.0f, node0Layout.size().width, "width of node0");
        assertEquals(14.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(0.0f, node00Layout.size().width, "width of node00");
        assertEquals(0.0f, node00Layout.size().height, "height of node00");
        assertEquals(15.0f, node00Layout.location().x, "x of node00");
        assertEquals(3.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_padding_border_overrides_size__border_box")
    void blockPaddingBorderOverridesSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(12.0f), Dimension.length(12.0f));
        node0Style.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        node0Style.border = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(22.0f, nodeLayout.size().width, "width of node");
        assertEquals(14.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(22.0f, node0Layout.size().width, "width of node0");
        assertEquals(14.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(0.0f, node00Layout.size().width, "width of node00");
        assertEquals(0.0f, node00Layout.size().height, "height of node00");
        assertEquals(15.0f, node00Layout.location().x, "x of node00");
        assertEquals(3.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_padding_border_overrides_size__content_box")
    void blockPaddingBorderOverridesSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.size = new Size<>(Dimension.length(12.0f), Dimension.length(12.0f));
        node0Style.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        node0Style.border = new Rect<>(LengthPercentage.length(7.0f), LengthPercentage.length(3.0f), LengthPercentage.length(1.0f), LengthPercentage.length(5.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(34.0f, nodeLayout.size().width, "width of node");
        assertEquals(26.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(34.0f, node0Layout.size().width, "width of node0");
        assertEquals(26.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(12.0f, node00Layout.size().width, "width of node00");
        assertEquals(0.0f, node00Layout.size().height, "height of node00");
        assertEquals(15.0f, node00Layout.location().x, "x of node00");
        assertEquals(3.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_padding_border_percentage_fixed_size__border_box")
    void blockPaddingBorderPercentageFixedSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.padding = new Rect<>(LengthPercentage.percent(0.04f), LengthPercentage.percent(0.02f), LengthPercentage.percent(0.01f), LengthPercentage.percent(0.03f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(50.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(12.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(47.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(2.0f, node00Layout.location().x, "x of node00");
        assertEquals(1.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_padding_border_percentage_fixed_size__content_box")
    void blockPaddingBorderPercentageFixedSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.padding = new Rect<>(LengthPercentage.percent(0.04f), LengthPercentage.percent(0.02f), LengthPercentage.percent(0.01f), LengthPercentage.percent(0.03f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(50.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(12.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(47.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(2.0f, node00Layout.location().x, "x of node00");
        assertEquals(1.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_padding_border_percentage_intrinsic_size__border_box")
    void blockPaddingBorderPercentageIntrinsicSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.padding = new Rect<>(LengthPercentage.percent(0.04f), LengthPercentage.percent(0.02f), LengthPercentage.percent(0.01f), LengthPercentage.percent(0.03f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(0.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(0.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(0.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_padding_border_percentage_intrinsic_size__content_box")
    void blockPaddingBorderPercentageIntrinsicSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.padding = new Rect<>(LengthPercentage.percent(0.04f), LengthPercentage.percent(0.02f), LengthPercentage.percent(0.01f), LengthPercentage.percent(0.03f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(0.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(0.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(0.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_padding_fixed_size__border_box")
    void blockPaddingFixedSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        nodeStyle.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(50.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(38.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(8.0f, node0Layout.location().x, "x of node0");
        assertEquals(2.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(38.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(8.0f, node1Layout.location().x, "x of node1");
        assertEquals(12.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_padding_fixed_size__content_box")
    void blockPaddingFixedSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        nodeStyle.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(62.0f, nodeLayout.size().width, "width of node");
        assertEquals(58.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(8.0f, node0Layout.location().x, "x of node0");
        assertEquals(2.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(8.0f, node1Layout.location().x, "x of node1");
        assertEquals(12.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_padding_intrinsic_size__border_box")
    void blockPaddingIntrinsicSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(62.0f, nodeLayout.size().width, "width of node");
        assertEquals(28.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(8.0f, node0Layout.location().x, "x of node0");
        assertEquals(2.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(8.0f, node1Layout.location().x, "x of node1");
        assertEquals(12.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_padding_intrinsic_size__content_box")
    void blockPaddingIntrinsicSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.size = new Size<>(Dimension.length(50.0f), Dimension.length(10.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        Style node1Style = new Style();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.padding = new Rect<>(LengthPercentage.length(8.0f), LengthPercentage.length(4.0f), LengthPercentage.length(2.0f), LengthPercentage.length(6.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(62.0f, nodeLayout.size().width, "width of node");
        assertEquals(28.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(8.0f, node0Layout.location().x, "x of node0");
        assertEquals(2.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(10.0f, node1Layout.size().height, "height of node1");
        assertEquals(8.0f, node1Layout.location().x, "x of node1");
        assertEquals(12.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_padding_percentage_fixed_size__border_box")
    void blockPaddingPercentageFixedSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.padding = new Rect<>(LengthPercentage.percent(0.04f), LengthPercentage.percent(0.02f), LengthPercentage.percent(0.01f), LengthPercentage.percent(0.03f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(50.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(12.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(47.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(2.0f, node00Layout.location().x, "x of node00");
        assertEquals(1.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_padding_percentage_fixed_size__content_box")
    void blockPaddingPercentageFixedSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.padding = new Rect<>(LengthPercentage.percent(0.04f), LengthPercentage.percent(0.02f), LengthPercentage.percent(0.01f), LengthPercentage.percent(0.03f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(50.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(12.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(47.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(2.0f, node00Layout.location().x, "x of node00");
        assertEquals(1.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_padding_percentage_intrinsic_size__border_box")
    void blockPaddingPercentageIntrinsicSizeBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.display = Display.BLOCK;
        node00Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.display = Display.BLOCK;
        node0Style.padding = new Rect<>(LengthPercentage.percent(0.04f), LengthPercentage.percent(0.02f), LengthPercentage.percent(0.01f), LengthPercentage.percent(0.03f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(0.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(0.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(0.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_padding_percentage_intrinsic_size__content_box")
    void blockPaddingPercentageIntrinsicSizeContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.display = Display.BLOCK;
        node00Style.size = new Size<>(Dimension.AUTO, Dimension.length(10.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.BLOCK;
        node0Style.padding = new Rect<>(LengthPercentage.percent(0.04f), LengthPercentage.percent(0.02f), LengthPercentage.percent(0.01f), LengthPercentage.percent(0.03f));
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.display = Display.BLOCK;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(0.0f, nodeLayout.size().width, "width of node");
        assertEquals(10.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(0.0f, node0Layout.size().width, "width of node0");
        assertEquals(10.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(0.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
    }

}
