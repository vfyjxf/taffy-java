package dev.vfyjxf.taffy.generated.rtl;

import dev.vfyjxf.taffy.geometry.*;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.tree.*;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Generated tests for rtl layout fixtures.
 */
public class RtlTest {

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
    @DisplayName("block_rtl_basic__border_box")
    void blockRtlBasicBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(150.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(200.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(200.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(150.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(150.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(200.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(100.0f, node2Layout.location().x, "x of node2");
        assertEquals(100.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_rtl_basic__content_box")
    void blockRtlBasicContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(150.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(200.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(200.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(150.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(150.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(200.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(100.0f, node2Layout.location().x, "x of node2");
        assertEquals(100.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("block_rtl_border__border_box")
    void blockRtlBorderBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.border = new TaffyRect<>(LengthPercentage.length(5.0f), LengthPercentage.length(10.0f), LengthPercentage.ZERO, LengthPercentage.ZERO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(190.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_rtl_border__content_box")
    void blockRtlBorderContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.border = new TaffyRect<>(LengthPercentage.length(5.0f), LengthPercentage.length(10.0f), LengthPercentage.ZERO, LengthPercentage.ZERO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(315.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(205.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_rtl_margin_auto_center__border_box")
    void blockRtlMarginAutoCenterBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        node0Style.margin = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(100.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_rtl_margin_auto_center__content_box")
    void blockRtlMarginAutoCenterContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        node0Style.margin = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(100.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_rtl_margin_left__border_box")
    void blockRtlMarginLeftBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        node0Style.margin = new TaffyRect<>(LengthPercentageAuto.length(20.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        node1Style.margin = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(200.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(200.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_rtl_margin_left__content_box")
    void blockRtlMarginLeftContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        node0Style.margin = new TaffyRect<>(LengthPercentageAuto.length(20.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        node1Style.margin = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(200.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(200.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_rtl_margin_right__border_box")
    void blockRtlMarginRightBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        node0Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.length(20.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        node1Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(180.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_rtl_margin_right__content_box")
    void blockRtlMarginRightContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        node0Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.length(20.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        node1Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.AUTO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(180.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("block_rtl_nested__border_box")
    void blockRtlNestedBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node00Style = new TaffyStyle();
        node00Style.direction = TaffyDirection.RTL;
        node00Style.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        node00Style.margin = new TaffyRect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newLeaf(node00Style);

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(200.0f), TaffyDimension.length(150.0f));
        node0Style.margin = new TaffyRect<>(LengthPercentageAuto.length(20.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(200.0f, node0Layout.size().width, "width of node0");
        assertEquals(150.0f, node0Layout.size().height, "height of node0");
        assertEquals(100.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(100.0f, node00Layout.size().width, "width of node00");
        assertEquals(50.0f, node00Layout.size().height, "height of node00");
        assertEquals(100.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_rtl_nested__content_box")
    void blockRtlNestedContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node00Style = new TaffyStyle();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.direction = TaffyDirection.RTL;
        node00Style.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        node00Style.margin = new TaffyRect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node00 = tree.newLeaf(node00Style);

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(200.0f), TaffyDimension.length(150.0f));
        node0Style.margin = new TaffyRect<>(LengthPercentageAuto.length(20.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(200.0f, node0Layout.size().width, "width of node0");
        assertEquals(150.0f, node0Layout.size().height, "height of node0");
        assertEquals(100.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(100.0f, node00Layout.size().width, "width of node00");
        assertEquals(50.0f, node00Layout.size().height, "height of node00");
        assertEquals(100.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("block_rtl_padding__border_box")
    void blockRtlPaddingBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.padding = new TaffyRect<>(LengthPercentage.length(10.0f), LengthPercentage.length(20.0f), LengthPercentage.ZERO, LengthPercentage.ZERO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(180.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("block_rtl_padding__content_box")
    void blockRtlPaddingContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.BLOCK;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.padding = new TaffyRect<>(LengthPercentage.length(10.0f), LengthPercentage.length(20.0f), LengthPercentage.ZERO, LengthPercentage.ZERO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(330.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(210.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
    }

    @Test
    @DisplayName("flex_column_reverse_rtl__border_box")
    void flexColumnReverseRtlBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.flexDirection = FlexDirection.COLUMN_REVERSE;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(50.0f, node0Layout.location().x, "x of node0");
        assertEquals(250.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(50.0f, node1Layout.location().x, "x of node1");
        assertEquals(200.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(50.0f, node2Layout.location().x, "x of node2");
        assertEquals(150.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_column_reverse_rtl__content_box")
    void flexColumnReverseRtlContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.flexDirection = FlexDirection.COLUMN_REVERSE;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(50.0f, node0Layout.location().x, "x of node0");
        assertEquals(250.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(50.0f, node1Layout.location().x, "x of node1");
        assertEquals(200.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(50.0f, node2Layout.location().x, "x of node2");
        assertEquals(150.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_column_rtl__border_box")
    void flexColumnRtlBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.flexDirection = FlexDirection.COLUMN;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(50.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(50.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(50.0f, node2Layout.location().x, "x of node2");
        assertEquals(100.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_column_rtl__content_box")
    void flexColumnRtlContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.flexDirection = FlexDirection.COLUMN;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(300.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(300.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(50.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(50.0f, node1Layout.location().x, "x of node1");
        assertEquals(50.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(50.0f, node2Layout.location().x, "x of node2");
        assertEquals(100.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_reverse_rtl__border_box")
    void flexRowReverseRtlBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.flexDirection = FlexDirection.ROW_REVERSE;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
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
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(50.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(100.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_reverse_rtl__content_box")
    void flexRowReverseRtlContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.flexDirection = FlexDirection.ROW_REVERSE;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
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
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(50.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(100.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_align_items__border_box")
    void flexRowRtlAlignItemsBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(30.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(40.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(30.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(35.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(200.0f, node1Layout.location().x, "x of node1");
        assertEquals(25.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(40.0f, node2Layout.size().height, "height of node2");
        assertEquals(150.0f, node2Layout.location().x, "x of node2");
        assertEquals(30.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_align_items__content_box")
    void flexRowRtlAlignItemsContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(30.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(40.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.alignItems = AlignItems.CENTER;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(30.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(35.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(200.0f, node1Layout.location().x, "x of node1");
        assertEquals(25.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(40.0f, node2Layout.size().height, "height of node2");
        assertEquals(150.0f, node2Layout.location().x, "x of node2");
        assertEquals(30.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_align_self__border_box")
    void flexRowRtlAlignSelfBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.alignSelf = AlignItems.FLEX_START;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(30.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.alignSelf = AlignItems.CENTER;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(30.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.alignSelf = AlignItems.FLEX_END;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(30.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(30.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(30.0f, node1Layout.size().height, "height of node1");
        assertEquals(200.0f, node1Layout.location().x, "x of node1");
        assertEquals(35.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(30.0f, node2Layout.size().height, "height of node2");
        assertEquals(150.0f, node2Layout.location().x, "x of node2");
        assertEquals(70.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_align_self__content_box")
    void flexRowRtlAlignSelfContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.alignSelf = AlignItems.FLEX_START;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(30.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.alignSelf = AlignItems.CENTER;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(30.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.alignSelf = AlignItems.FLEX_END;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(30.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(30.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(30.0f, node1Layout.size().height, "height of node1");
        assertEquals(200.0f, node1Layout.location().x, "x of node1");
        assertEquals(35.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(30.0f, node2Layout.size().height, "height of node2");
        assertEquals(150.0f, node2Layout.location().x, "x of node2");
        assertEquals(70.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_basic__border_box")
    void flexRowRtlBasicBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(200.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(150.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_basic__content_box")
    void flexRowRtlBasicContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(200.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(150.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_flex_grow__border_box")
    void flexRowRtlFlexGrowBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.flexGrow = 1.0f;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.flexGrow = 2.0f;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(200.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(150.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(150.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_flex_grow__content_box")
    void flexRowRtlFlexGrowContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.flexGrow = 1.0f;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.flexGrow = 2.0f;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(200.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(150.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(150.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_flex_shrink__border_box")
    void flexRowRtlFlexShrinkBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.flexShrink = 1.0f;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.flexShrink = 0.0f;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.flexShrink = 2.0f;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(33.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(67.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(17.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(17.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_flex_shrink__content_box")
    void flexRowRtlFlexShrinkContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.flexShrink = 1.0f;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.flexShrink = 0.0f;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.flexShrink = 2.0f;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(33.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(67.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(17.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(17.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_gap__border_box")
    void flexRowRtlGapBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        nodeStyle.gap = new TaffySize<>(LengthPercentage.length(10.0f), LengthPercentage.length(10.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(190.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(130.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_gap__content_box")
    void flexRowRtlGapContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        nodeStyle.gap = new TaffySize<>(LengthPercentage.length(10.0f), LengthPercentage.length(10.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(190.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(130.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_justify_content_center__border_box")
    void flexRowRtlJustifyContentCenterBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.justifyContent = AlignContent.CENTER;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
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
        assertEquals(100.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("flex_row_rtl_justify_content_center__content_box")
    void flexRowRtlJustifyContentCenterContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.justifyContent = AlignContent.CENTER;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
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
        assertEquals(100.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("flex_row_rtl_justify_content_end__border_box")
    void flexRowRtlJustifyContentEndBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.justifyContent = AlignContent.FLEX_END;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(50.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("flex_row_rtl_justify_content_end__content_box")
    void flexRowRtlJustifyContentEndContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.justifyContent = AlignContent.FLEX_END;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(50.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("flex_row_rtl_justify_content_start__border_box")
    void flexRowRtlJustifyContentStartBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.justifyContent = AlignContent.FLEX_START;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(200.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("flex_row_rtl_justify_content_start__content_box")
    void flexRowRtlJustifyContentStartContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.justifyContent = AlignContent.FLEX_START;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(200.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("flex_row_rtl_margin__border_box")
    void flexRowRtlMarginBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        node0Style.margin = new TaffyRect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        node1Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        node2Style.margin = new TaffyRect<>(LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(5.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(180.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(125.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_margin__content_box")
    void flexRowRtlMarginContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        node0Style.margin = new TaffyRect<>(LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        node1Style.margin = new TaffyRect<>(LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        node2Style.margin = new TaffyRect<>(LengthPercentageAuto.length(5.0f), LengthPercentageAuto.length(5.0f), LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO);
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(180.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(125.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_padding__border_box")
    void flexRowRtlPaddingBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        nodeStyle.padding = new TaffyRect<>(LengthPercentage.length(10.0f), LengthPercentage.length(20.0f), LengthPercentage.ZERO, LengthPercentage.ZERO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(230.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(180.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("flex_row_rtl_padding__content_box")
    void flexRowRtlPaddingContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        nodeStyle.padding = new TaffyRect<>(LengthPercentage.length(10.0f), LengthPercentage.length(20.0f), LengthPercentage.ZERO, LengthPercentage.ZERO);
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(330.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(260.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(210.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("flex_row_rtl_space_around__border_box")
    void flexRowRtlSpaceAroundBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.justifyContent = AlignContent.SPACE_AROUND;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(225.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(125.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(25.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_space_around__content_box")
    void flexRowRtlSpaceAroundContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.justifyContent = AlignContent.SPACE_AROUND;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(225.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(125.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(25.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_space_between__border_box")
    void flexRowRtlSpaceBetweenBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.justifyContent = AlignContent.SPACE_BETWEEN;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(125.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_space_between__content_box")
    void flexRowRtlSpaceBetweenContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.justifyContent = AlignContent.SPACE_BETWEEN;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(125.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_space_evenly__border_box")
    void flexRowRtlSpaceEvenlyBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.justifyContent = AlignContent.SPACE_EVENLY;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(213.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(125.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(38.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_row_rtl_space_evenly__content_box")
    void flexRowRtlSpaceEvenlyContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.justifyContent = AlignContent.SPACE_EVENLY;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(100.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(213.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(125.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(38.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_wrap_reverse_rtl__border_box")
    void flexWrapReverseRtlBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.flexWrap = FlexWrap.WRAP_REVERSE;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(50.0f, node0Layout.location().x, "x of node0");
        assertEquals(150.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(150.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(50.0f, node2Layout.location().x, "x of node2");
        assertEquals(50.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_wrap_reverse_rtl__content_box")
    void flexWrapReverseRtlContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.flexWrap = FlexWrap.WRAP_REVERSE;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(50.0f, node0Layout.location().x, "x of node0");
        assertEquals(150.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(150.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(50.0f, node2Layout.location().x, "x of node2");
        assertEquals(50.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_wrap_rtl__border_box")
    void flexWrapRtlBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.flexWrap = FlexWrap.WRAP;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(50.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(50.0f, node2Layout.location().x, "x of node2");
        assertEquals(100.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("flex_wrap_rtl__content_box")
    void flexWrapRtlContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.flexWrap = FlexWrap.WRAP;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(200.0f));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(50.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(50.0f, node2Layout.location().x, "x of node2");
        assertEquals(100.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_auto_flow_row__border_box")
    void gridRtlAutoFlowRowBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle node3Style = new TaffyStyle();
        node3Style.direction = TaffyDirection.RTL;
        node3Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node3 = tree.newLeaf(node3Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridAutoFlow = GridAutoFlow.ROW;
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(150.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(50.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(50.0f, node3Layout.size().height, "height of node3");
        assertEquals(250.0f, node3Layout.location().x, "x of node3");
        assertEquals(100.0f, node3Layout.location().y, "y of node3");
    }

    @Test
    @DisplayName("grid_rtl_auto_flow_row__content_box")
    void gridRtlAutoFlowRowContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle node3Style = new TaffyStyle();
        node3Style.boxSizing = BoxSizing.CONTENT_BOX;
        node3Style.direction = TaffyDirection.RTL;
        node3Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node3 = tree.newLeaf(node3Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridAutoFlow = GridAutoFlow.ROW;
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(150.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(50.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(50.0f, node3Layout.size().width, "width of node3");
        assertEquals(50.0f, node3Layout.size().height, "height of node3");
        assertEquals(250.0f, node3Layout.location().x, "x of node3");
        assertEquals(100.0f, node3Layout.location().y, "y of node3");
    }

    @Test
    @DisplayName("grid_rtl_basic__border_box")
    void gridRtlBasicBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle node3Style = new TaffyStyle();
        node3Style.direction = TaffyDirection.RTL;
        NodeId node3 = tree.newLeaf(node3Style);

        TaffyStyle node4Style = new TaffyStyle();
        node4Style.direction = TaffyDirection.RTL;
        NodeId node4 = tree.newLeaf(node4Style);

        TaffyStyle node5Style = new TaffyStyle();
        node5Style.direction = TaffyDirection.RTL;
        NodeId node5 = tree.newLeaf(node5Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3, node4, node5);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(200.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(100.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(100.0f, node3Layout.size().width, "width of node3");
        assertEquals(100.0f, node3Layout.size().height, "height of node3");
        assertEquals(200.0f, node3Layout.location().x, "x of node3");
        assertEquals(100.0f, node3Layout.location().y, "y of node3");
        Layout node4Layout = tree.getLayout(node4);
        assertEquals(100.0f, node4Layout.size().width, "width of node4");
        assertEquals(100.0f, node4Layout.size().height, "height of node4");
        assertEquals(100.0f, node4Layout.location().x, "x of node4");
        assertEquals(100.0f, node4Layout.location().y, "y of node4");
        Layout node5Layout = tree.getLayout(node5);
        assertEquals(100.0f, node5Layout.size().width, "width of node5");
        assertEquals(100.0f, node5Layout.size().height, "height of node5");
        assertEquals(0.0f, node5Layout.location().x, "x of node5");
        assertEquals(100.0f, node5Layout.location().y, "y of node5");
    }

    @Test
    @DisplayName("grid_rtl_basic__content_box")
    void gridRtlBasicContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle node3Style = new TaffyStyle();
        node3Style.boxSizing = BoxSizing.CONTENT_BOX;
        node3Style.direction = TaffyDirection.RTL;
        NodeId node3 = tree.newLeaf(node3Style);

        TaffyStyle node4Style = new TaffyStyle();
        node4Style.boxSizing = BoxSizing.CONTENT_BOX;
        node4Style.direction = TaffyDirection.RTL;
        NodeId node4 = tree.newLeaf(node4Style);

        TaffyStyle node5Style = new TaffyStyle();
        node5Style.boxSizing = BoxSizing.CONTENT_BOX;
        node5Style.direction = TaffyDirection.RTL;
        NodeId node5 = tree.newLeaf(node5Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2, node3, node4, node5);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(200.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(100.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
        Layout node3Layout = tree.getLayout(node3);
        assertEquals(100.0f, node3Layout.size().width, "width of node3");
        assertEquals(100.0f, node3Layout.size().height, "height of node3");
        assertEquals(200.0f, node3Layout.location().x, "x of node3");
        assertEquals(100.0f, node3Layout.location().y, "y of node3");
        Layout node4Layout = tree.getLayout(node4);
        assertEquals(100.0f, node4Layout.size().width, "width of node4");
        assertEquals(100.0f, node4Layout.size().height, "height of node4");
        assertEquals(100.0f, node4Layout.location().x, "x of node4");
        assertEquals(100.0f, node4Layout.location().y, "y of node4");
        Layout node5Layout = tree.getLayout(node5);
        assertEquals(100.0f, node5Layout.size().width, "width of node5");
        assertEquals(100.0f, node5Layout.size().height, "height of node5");
        assertEquals(0.0f, node5Layout.location().x, "x of node5");
        assertEquals(100.0f, node5Layout.location().y, "y of node5");
    }

    @Test
    @DisplayName("grid_rtl_column_gap__border_box")
    void gridRtlColumnGapBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gap = new TaffySize<>(LengthPercentage.length(20.0f), LengthPercentage.ZERO);
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(200.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(80.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(-40.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_column_gap__content_box")
    void gridRtlColumnGapContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gap = new TaffySize<>(LengthPercentage.length(20.0f), LengthPercentage.ZERO);
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(200.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(80.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(-40.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_explicit_placement__border_box")
    void gridRtlExplicitPlacementBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.gridRow = new TaffyLine<>(GridPlacement.line(1), GridPlacement.auto());
        node0Style.gridColumn = new TaffyLine<>(GridPlacement.line(1), GridPlacement.auto());
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.gridRow = new TaffyLine<>(GridPlacement.line(1), GridPlacement.auto());
        node1Style.gridColumn = new TaffyLine<>(GridPlacement.line(3), GridPlacement.auto());
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.gridRow = new TaffyLine<>(GridPlacement.line(2), GridPlacement.auto());
        node2Style.gridColumn = new TaffyLine<>(GridPlacement.line(2), GridPlacement.auto());
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(200.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(100.0f, node2Layout.location().x, "x of node2");
        assertEquals(100.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_explicit_placement__content_box")
    void gridRtlExplicitPlacementContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.gridRow = new TaffyLine<>(GridPlacement.line(1), GridPlacement.auto());
        node0Style.gridColumn = new TaffyLine<>(GridPlacement.line(1), GridPlacement.auto());
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.gridRow = new TaffyLine<>(GridPlacement.line(1), GridPlacement.auto());
        node1Style.gridColumn = new TaffyLine<>(GridPlacement.line(3), GridPlacement.auto());
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.gridRow = new TaffyLine<>(GridPlacement.line(2), GridPlacement.auto());
        node2Style.gridColumn = new TaffyLine<>(GridPlacement.line(2), GridPlacement.auto());
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(200.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(100.0f, node2Layout.location().x, "x of node2");
        assertEquals(100.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_gap__border_box")
    void gridRtlGapBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gap = new TaffySize<>(LengthPercentage.length(10.0f), LengthPercentage.length(10.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(200.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(90.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("grid_rtl_gap__content_box")
    void gridRtlGapContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gap = new TaffySize<>(LengthPercentage.length(10.0f), LengthPercentage.length(10.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(200.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(90.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
    }

    @Test
    @DisplayName("grid_rtl_justify_content_center__border_box")
    void gridRtlJustifyContentCenterBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.justifyContent = AlignContent.CENTER;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(400.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(150.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(50.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_justify_content_center__content_box")
    void gridRtlJustifyContentCenterContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.justifyContent = AlignContent.CENTER;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(400.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(150.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(50.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_justify_content_end__border_box")
    void gridRtlJustifyContentEndBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.justifyContent = AlignContent.END;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(400.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(200.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(100.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_justify_content_end__content_box")
    void gridRtlJustifyContentEndContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.justifyContent = AlignContent.END;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(400.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(200.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(100.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_justify_content_space_around__border_box")
    void gridRtlJustifyContentSpaceAroundBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.justifyContent = AlignContent.SPACE_AROUND;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(400.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(283.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(150.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(17.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_justify_content_space_around__content_box")
    void gridRtlJustifyContentSpaceAroundContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.justifyContent = AlignContent.SPACE_AROUND;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(400.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(283.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(150.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(17.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_justify_content_space_between__border_box")
    void gridRtlJustifyContentSpaceBetweenBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.justifyContent = AlignContent.SPACE_BETWEEN;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(400.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(300.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(150.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_justify_content_space_between__content_box")
    void gridRtlJustifyContentSpaceBetweenContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.justifyContent = AlignContent.SPACE_BETWEEN;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(400.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(300.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(150.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_justify_content_space_evenly__border_box")
    void gridRtlJustifyContentSpaceEvenlyBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.justifyContent = AlignContent.SPACE_EVENLY;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(400.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(275.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(150.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(25.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_justify_content_space_evenly__content_box")
    void gridRtlJustifyContentSpaceEvenlyContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.justifyContent = AlignContent.SPACE_EVENLY;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(400.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(275.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(150.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(25.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_justify_content_start__border_box")
    void gridRtlJustifyContentStartBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.justifyContent = AlignContent.START;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(400.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(300.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(200.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(100.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_justify_content_start__content_box")
    void gridRtlJustifyContentStartContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.justifyContent = AlignContent.START;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(400.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(400.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(300.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(200.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(100.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(100.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_justify_items_center__border_box")
    void gridRtlJustifyItemsCenterBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.justifyItems = AlignItems.CENTER;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(225.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(125.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(25.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_justify_items_center__content_box")
    void gridRtlJustifyItemsCenterContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.justifyItems = AlignItems.CENTER;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(225.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(125.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(25.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_justify_items_end__border_box")
    void gridRtlJustifyItemsEndBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.justifyItems = AlignItems.END;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(200.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(100.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_justify_items_end__content_box")
    void gridRtlJustifyItemsEndContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.justifyItems = AlignItems.END;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(200.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(100.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_justify_items_start__border_box")
    void gridRtlJustifyItemsStartBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.justifyItems = AlignItems.START;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(150.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(50.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_justify_items_start__content_box")
    void gridRtlJustifyItemsStartContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.justifyItems = AlignItems.START;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(150.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(50.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_justify_self__border_box")
    void gridRtlJustifySelfBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.justifySelf = AlignItems.START;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        node1Style.justifySelf = AlignItems.CENTER;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.justifySelf = AlignItems.END;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(125.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_justify_self__content_box")
    void gridRtlJustifySelfContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.justifySelf = AlignItems.START;
        node0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        node1Style.justifySelf = AlignItems.CENTER;
        node1Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.justifySelf = AlignItems.END;
        node2Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(50.0f, node0Layout.size().width, "width of node0");
        assertEquals(50.0f, node0Layout.size().height, "height of node0");
        assertEquals(250.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(50.0f, node1Layout.size().width, "width of node1");
        assertEquals(50.0f, node1Layout.size().height, "height of node1");
        assertEquals(125.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(50.0f, node2Layout.size().width, "width of node2");
        assertEquals(50.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(0.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_span__border_box")
    void gridRtlSpanBorderBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.direction = TaffyDirection.RTL;
        node0Style.gridColumn = new TaffyLine<>(GridPlacement.span(2), GridPlacement.auto());
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.direction = TaffyDirection.RTL;
        node2Style.gridColumn = new TaffyLine<>(GridPlacement.line(2), GridPlacement.span(2));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(200.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(100.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(200.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(100.0f, node2Layout.location().y, "y of node2");
    }

    @Test
    @DisplayName("grid_rtl_span__content_box")
    void gridRtlSpanContentBox() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle node0Style = new TaffyStyle();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.direction = TaffyDirection.RTL;
        node0Style.gridColumn = new TaffyLine<>(GridPlacement.span(2), GridPlacement.auto());
        NodeId node0 = tree.newLeaf(node0Style);

        TaffyStyle node1Style = new TaffyStyle();
        node1Style.boxSizing = BoxSizing.CONTENT_BOX;
        node1Style.direction = TaffyDirection.RTL;
        NodeId node1 = tree.newLeaf(node1Style);

        TaffyStyle node2Style = new TaffyStyle();
        node2Style.boxSizing = BoxSizing.CONTENT_BOX;
        node2Style.direction = TaffyDirection.RTL;
        node2Style.gridColumn = new TaffyLine<>(GridPlacement.line(2), GridPlacement.span(2));
        NodeId node2 = tree.newLeaf(node2Style);

        TaffyStyle nodeStyle = new TaffyStyle();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.direction = TaffyDirection.RTL;
        nodeStyle.display = TaffyDisplay.GRID;
        nodeStyle.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(200.0f));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        nodeStyle.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(100.0f)));
        NodeId node = tree.newWithChildren(nodeStyle, node0, node1, node2);


        tree.computeLayout(node, TaffySize.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(300.0f, nodeLayout.size().width, "width of node");
        assertEquals(200.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(200.0f, node0Layout.size().width, "width of node0");
        assertEquals(100.0f, node0Layout.size().height, "height of node0");
        assertEquals(100.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node1Layout = tree.getLayout(node1);
        assertEquals(100.0f, node1Layout.size().width, "width of node1");
        assertEquals(100.0f, node1Layout.size().height, "height of node1");
        assertEquals(0.0f, node1Layout.location().x, "x of node1");
        assertEquals(0.0f, node1Layout.location().y, "y of node1");
        Layout node2Layout = tree.getLayout(node2);
        assertEquals(200.0f, node2Layout.size().width, "width of node2");
        assertEquals(100.0f, node2Layout.size().height, "height of node2");
        assertEquals(0.0f, node2Layout.location().x, "x of node2");
        assertEquals(100.0f, node2Layout.location().y, "y of node2");
    }

}
