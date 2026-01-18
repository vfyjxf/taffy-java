package dev.vfyjxf.taffy.generated.gridflex;

import dev.vfyjxf.taffy.geometry.*;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.tree.*;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Generated tests for gridflex layout fixtures.
 */
public class GridflexTest {

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
    @DisplayName("gridflex_column_integration__border_box")
    void gridflexColumnIntegrationBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        MeasureFunc node00Measure = ahemTextMeasure("HH", false);
        NodeId node00 = tree.newLeafWithMeasure(node00Style, node00Measure);

        Style node01Style = new Style();
        MeasureFunc node01Measure = ahemTextMeasure("HH", false);
        NodeId node01 = tree.newLeafWithMeasure(node01Style, node01Measure);

        Style node02Style = new Style();
        MeasureFunc node02Measure = ahemTextMeasure("HH", false);
        NodeId node02 = tree.newLeafWithMeasure(node02Style, node02Measure);

        Style node03Style = new Style();
        MeasureFunc node03Measure = ahemTextMeasure("HH", false);
        NodeId node03 = tree.newLeafWithMeasure(node03Style, node03Measure);

        Style node0Style = new Style();
        node0Style.display = Display.GRID;
        node0Style.gridTemplateColumns.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateColumns.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateRows.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateRows.add(TrackSizingFunction.fr(1.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00, node01, node02, node03);

        Style nodeStyle = new Style();
        nodeStyle.flexDirection = FlexDirection.COLUMN;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(40.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(20.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node01Layout = tree.getLayout(node01);
        assertEquals(20.0f, node01Layout.size().width, "width of node01");
        assertEquals(10.0f, node01Layout.size().height, "height of node01");
        assertEquals(20.0f, node01Layout.location().x, "x of node01");
        assertEquals(0.0f, node01Layout.location().y, "y of node01");
        Layout node02Layout = tree.getLayout(node02);
        assertEquals(20.0f, node02Layout.size().width, "width of node02");
        assertEquals(10.0f, node02Layout.size().height, "height of node02");
        assertEquals(0.0f, node02Layout.location().x, "x of node02");
        assertEquals(10.0f, node02Layout.location().y, "y of node02");
        Layout node03Layout = tree.getLayout(node03);
        assertEquals(20.0f, node03Layout.size().width, "width of node03");
        assertEquals(10.0f, node03Layout.size().height, "height of node03");
        assertEquals(20.0f, node03Layout.location().x, "x of node03");
        assertEquals(10.0f, node03Layout.location().y, "y of node03");
    }

    @Test
    @DisplayName("gridflex_column_integration__content_box")
    void gridflexColumnIntegrationContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        MeasureFunc node00Measure = ahemTextMeasure("HH", false);
        NodeId node00 = tree.newLeafWithMeasure(node00Style, node00Measure);

        Style node01Style = new Style();
        node01Style.boxSizing = BoxSizing.CONTENT_BOX;
        MeasureFunc node01Measure = ahemTextMeasure("HH", false);
        NodeId node01 = tree.newLeafWithMeasure(node01Style, node01Measure);

        Style node02Style = new Style();
        node02Style.boxSizing = BoxSizing.CONTENT_BOX;
        MeasureFunc node02Measure = ahemTextMeasure("HH", false);
        NodeId node02 = tree.newLeafWithMeasure(node02Style, node02Measure);

        Style node03Style = new Style();
        node03Style.boxSizing = BoxSizing.CONTENT_BOX;
        MeasureFunc node03Measure = ahemTextMeasure("HH", false);
        NodeId node03 = tree.newLeafWithMeasure(node03Style, node03Measure);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.GRID;
        node0Style.gridTemplateColumns.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateColumns.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateRows.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateRows.add(TrackSizingFunction.fr(1.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00, node01, node02, node03);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.flexDirection = FlexDirection.COLUMN;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(40.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(20.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node01Layout = tree.getLayout(node01);
        assertEquals(20.0f, node01Layout.size().width, "width of node01");
        assertEquals(10.0f, node01Layout.size().height, "height of node01");
        assertEquals(20.0f, node01Layout.location().x, "x of node01");
        assertEquals(0.0f, node01Layout.location().y, "y of node01");
        Layout node02Layout = tree.getLayout(node02);
        assertEquals(20.0f, node02Layout.size().width, "width of node02");
        assertEquals(10.0f, node02Layout.size().height, "height of node02");
        assertEquals(0.0f, node02Layout.location().x, "x of node02");
        assertEquals(10.0f, node02Layout.location().y, "y of node02");
        Layout node03Layout = tree.getLayout(node03);
        assertEquals(20.0f, node03Layout.size().width, "width of node03");
        assertEquals(10.0f, node03Layout.size().height, "height of node03");
        assertEquals(20.0f, node03Layout.location().x, "x of node03");
        assertEquals(10.0f, node03Layout.location().y, "y of node03");
    }

    @Test
    @DisplayName("gridflex_kitchen_sink__border_box")
    void gridflexKitchenSinkBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.size = new Size<>(Dimension.length(20.0f), Dimension.AUTO);
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00100Style = new Style();
        node00100Style.size = new Size<>(Dimension.length(20.0f), Dimension.AUTO);
        NodeId node00100 = tree.newLeaf(node00100Style);

        Style node00101Style = new Style();
        NodeId node00101 = tree.newLeaf(node00101Style);

        Style node00102Style = new Style();
        NodeId node00102 = tree.newLeaf(node00102Style);

        Style node00103Style = new Style();
        NodeId node00103 = tree.newLeaf(node00103Style);

        Style node0010Style = new Style();
        node0010Style.display = Display.GRID;
        node0010Style.gridTemplateColumns.add(TrackSizingFunction.auto());
        node0010Style.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.percent(0.1f)));
        node0010Style.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.percent(0.3f)));
        node0010Style.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.percent(0.1f)));
        NodeId node0010 = tree.newWithChildren(node0010Style, node00100, node00101, node00102, node00103);

        Style node001Style = new Style();
        node001Style.flexGrow = 1.0f;
        node001Style.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node001 = tree.newWithChildren(node001Style, node0010);

        Style node00Style = new Style();
        NodeId node00 = tree.newWithChildren(node00Style, node000, node001);

        Style node01Style = new Style();
        MeasureFunc node01Measure = ahemTextMeasure("HH", false);
        NodeId node01 = tree.newLeafWithMeasure(node01Style, node01Measure);

        Style node02Style = new Style();
        MeasureFunc node02Measure = ahemTextMeasure("HH", false);
        NodeId node02 = tree.newLeafWithMeasure(node02Style, node02Measure);

        Style node03Style = new Style();
        MeasureFunc node03Measure = ahemTextMeasure("HH", false);
        NodeId node03 = tree.newLeafWithMeasure(node03Style, node03Measure);

        Style node0Style = new Style();
        node0Style.display = Display.GRID;
        node0Style.gridTemplateColumns.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateColumns.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateRows.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateRows.add(TrackSizingFunction.fr(1.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00, node01, node02, node03);

        Style nodeStyle = new Style();
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(140.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(140.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(70.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node000Layout = tree.getLayout(node000);
        assertEquals(20.0f, node000Layout.size().width, "width of node000");
        assertEquals(10.0f, node000Layout.size().height, "height of node000");
        assertEquals(0.0f, node000Layout.location().x, "x of node000");
        assertEquals(0.0f, node000Layout.location().y, "y of node000");
        Layout node001Layout = tree.getLayout(node001);
        assertEquals(50.0f, node001Layout.size().width, "width of node001");
        assertEquals(10.0f, node001Layout.size().height, "height of node001");
        assertEquals(20.0f, node001Layout.location().x, "x of node001");
        assertEquals(0.0f, node001Layout.location().y, "y of node001");
        Layout node0010Layout = tree.getLayout(node0010);
        assertEquals(20.0f, node0010Layout.size().width, "width of node0010");
        assertEquals(10.0f, node0010Layout.size().height, "height of node0010");
        assertEquals(0.0f, node0010Layout.location().x, "x of node0010");
        assertEquals(0.0f, node0010Layout.location().y, "y of node0010");
        Layout node00100Layout = tree.getLayout(node00100);
        assertEquals(20.0f, node00100Layout.size().width, "width of node00100");
        assertEquals(3.0f, node00100Layout.size().height, "height of node00100");
        assertEquals(0.0f, node00100Layout.location().x, "x of node00100");
        assertEquals(0.0f, node00100Layout.location().y, "y of node00100");
        Layout node00101Layout = tree.getLayout(node00101);
        assertEquals(2.0f, node00101Layout.size().width, "width of node00101");
        assertEquals(3.0f, node00101Layout.size().height, "height of node00101");
        assertEquals(20.0f, node00101Layout.location().x, "x of node00101");
        assertEquals(0.0f, node00101Layout.location().y, "y of node00101");
        Layout node00102Layout = tree.getLayout(node00102);
        assertEquals(20.0f, node00102Layout.size().width, "width of node00102");
        assertEquals(1.0f, node00102Layout.size().height, "height of node00102");
        assertEquals(0.0f, node00102Layout.location().x, "x of node00102");
        assertEquals(3.0f, node00102Layout.location().y, "y of node00102");
        Layout node00103Layout = tree.getLayout(node00103);
        assertEquals(2.0f, node00103Layout.size().width, "width of node00103");
        assertEquals(1.0f, node00103Layout.size().height, "height of node00103");
        assertEquals(20.0f, node00103Layout.location().x, "x of node00103");
        assertEquals(3.0f, node00103Layout.location().y, "y of node00103");
        Layout node01Layout = tree.getLayout(node01);
        assertEquals(70.0f, node01Layout.size().width, "width of node01");
        assertEquals(10.0f, node01Layout.size().height, "height of node01");
        assertEquals(70.0f, node01Layout.location().x, "x of node01");
        assertEquals(0.0f, node01Layout.location().y, "y of node01");
        Layout node02Layout = tree.getLayout(node02);
        assertEquals(70.0f, node02Layout.size().width, "width of node02");
        assertEquals(10.0f, node02Layout.size().height, "height of node02");
        assertEquals(0.0f, node02Layout.location().x, "x of node02");
        assertEquals(10.0f, node02Layout.location().y, "y of node02");
        Layout node03Layout = tree.getLayout(node03);
        assertEquals(70.0f, node03Layout.size().width, "width of node03");
        assertEquals(10.0f, node03Layout.size().height, "height of node03");
        assertEquals(70.0f, node03Layout.location().x, "x of node03");
        assertEquals(10.0f, node03Layout.location().y, "y of node03");
    }

    @Test
    @DisplayName("gridflex_kitchen_sink__content_box")
    void gridflexKitchenSinkContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node000Style = new Style();
        node000Style.boxSizing = BoxSizing.CONTENT_BOX;
        node000Style.size = new Size<>(Dimension.length(20.0f), Dimension.AUTO);
        NodeId node000 = tree.newLeaf(node000Style);

        Style node00100Style = new Style();
        node00100Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00100Style.size = new Size<>(Dimension.length(20.0f), Dimension.AUTO);
        NodeId node00100 = tree.newLeaf(node00100Style);

        Style node00101Style = new Style();
        node00101Style.boxSizing = BoxSizing.CONTENT_BOX;
        NodeId node00101 = tree.newLeaf(node00101Style);

        Style node00102Style = new Style();
        node00102Style.boxSizing = BoxSizing.CONTENT_BOX;
        NodeId node00102 = tree.newLeaf(node00102Style);

        Style node00103Style = new Style();
        node00103Style.boxSizing = BoxSizing.CONTENT_BOX;
        NodeId node00103 = tree.newLeaf(node00103Style);

        Style node0010Style = new Style();
        node0010Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0010Style.display = Display.GRID;
        node0010Style.gridTemplateColumns.add(TrackSizingFunction.auto());
        node0010Style.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.percent(0.1f)));
        node0010Style.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.percent(0.3f)));
        node0010Style.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.percent(0.1f)));
        NodeId node0010 = tree.newWithChildren(node0010Style, node00100, node00101, node00102, node00103);

        Style node001Style = new Style();
        node001Style.boxSizing = BoxSizing.CONTENT_BOX;
        node001Style.flexGrow = 1.0f;
        node001Style.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node001 = tree.newWithChildren(node001Style, node0010);

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        NodeId node00 = tree.newWithChildren(node00Style, node000, node001);

        Style node01Style = new Style();
        node01Style.boxSizing = BoxSizing.CONTENT_BOX;
        MeasureFunc node01Measure = ahemTextMeasure("HH", false);
        NodeId node01 = tree.newLeafWithMeasure(node01Style, node01Measure);

        Style node02Style = new Style();
        node02Style.boxSizing = BoxSizing.CONTENT_BOX;
        MeasureFunc node02Measure = ahemTextMeasure("HH", false);
        NodeId node02 = tree.newLeafWithMeasure(node02Style, node02Measure);

        Style node03Style = new Style();
        node03Style.boxSizing = BoxSizing.CONTENT_BOX;
        MeasureFunc node03Measure = ahemTextMeasure("HH", false);
        NodeId node03 = tree.newLeafWithMeasure(node03Style, node03Measure);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.GRID;
        node0Style.gridTemplateColumns.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateColumns.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateRows.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateRows.add(TrackSizingFunction.fr(1.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00, node01, node02, node03);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(140.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(140.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(70.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node000Layout = tree.getLayout(node000);
        assertEquals(20.0f, node000Layout.size().width, "width of node000");
        assertEquals(10.0f, node000Layout.size().height, "height of node000");
        assertEquals(0.0f, node000Layout.location().x, "x of node000");
        assertEquals(0.0f, node000Layout.location().y, "y of node000");
        Layout node001Layout = tree.getLayout(node001);
        assertEquals(50.0f, node001Layout.size().width, "width of node001");
        assertEquals(10.0f, node001Layout.size().height, "height of node001");
        assertEquals(20.0f, node001Layout.location().x, "x of node001");
        assertEquals(0.0f, node001Layout.location().y, "y of node001");
        Layout node0010Layout = tree.getLayout(node0010);
        assertEquals(20.0f, node0010Layout.size().width, "width of node0010");
        assertEquals(10.0f, node0010Layout.size().height, "height of node0010");
        assertEquals(0.0f, node0010Layout.location().x, "x of node0010");
        assertEquals(0.0f, node0010Layout.location().y, "y of node0010");
        Layout node00100Layout = tree.getLayout(node00100);
        assertEquals(20.0f, node00100Layout.size().width, "width of node00100");
        assertEquals(3.0f, node00100Layout.size().height, "height of node00100");
        assertEquals(0.0f, node00100Layout.location().x, "x of node00100");
        assertEquals(0.0f, node00100Layout.location().y, "y of node00100");
        Layout node00101Layout = tree.getLayout(node00101);
        assertEquals(2.0f, node00101Layout.size().width, "width of node00101");
        assertEquals(3.0f, node00101Layout.size().height, "height of node00101");
        assertEquals(20.0f, node00101Layout.location().x, "x of node00101");
        assertEquals(0.0f, node00101Layout.location().y, "y of node00101");
        Layout node00102Layout = tree.getLayout(node00102);
        assertEquals(20.0f, node00102Layout.size().width, "width of node00102");
        assertEquals(1.0f, node00102Layout.size().height, "height of node00102");
        assertEquals(0.0f, node00102Layout.location().x, "x of node00102");
        assertEquals(3.0f, node00102Layout.location().y, "y of node00102");
        Layout node00103Layout = tree.getLayout(node00103);
        assertEquals(2.0f, node00103Layout.size().width, "width of node00103");
        assertEquals(1.0f, node00103Layout.size().height, "height of node00103");
        assertEquals(20.0f, node00103Layout.location().x, "x of node00103");
        assertEquals(3.0f, node00103Layout.location().y, "y of node00103");
        Layout node01Layout = tree.getLayout(node01);
        assertEquals(70.0f, node01Layout.size().width, "width of node01");
        assertEquals(10.0f, node01Layout.size().height, "height of node01");
        assertEquals(70.0f, node01Layout.location().x, "x of node01");
        assertEquals(0.0f, node01Layout.location().y, "y of node01");
        Layout node02Layout = tree.getLayout(node02);
        assertEquals(70.0f, node02Layout.size().width, "width of node02");
        assertEquals(10.0f, node02Layout.size().height, "height of node02");
        assertEquals(0.0f, node02Layout.location().x, "x of node02");
        assertEquals(10.0f, node02Layout.location().y, "y of node02");
        Layout node03Layout = tree.getLayout(node03);
        assertEquals(70.0f, node03Layout.size().width, "width of node03");
        assertEquals(10.0f, node03Layout.size().height, "height of node03");
        assertEquals(70.0f, node03Layout.location().x, "x of node03");
        assertEquals(10.0f, node03Layout.location().y, "y of node03");
    }

    @Test
    @DisplayName("gridflex_kitchen_sink_minimise__border_box")
    void gridflexKitchenSinkMinimiseBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node00 = tree.newLeaf(node00Style);

        Style node01Style = new Style();
        NodeId node01 = tree.newLeaf(node01Style);

        Style node0Style = new Style();
        node0Style.display = Display.GRID;
        node0Style.gridTemplateColumns.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateColumns.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(20.0f)));
        NodeId node0 = tree.newWithChildren(node0Style, node00, node01);

        Style nodeStyle = new Style();
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(50.0f, node00Layout.size().width, "width of node00");
        assertEquals(20.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node01Layout = tree.getLayout(node01);
        assertEquals(50.0f, node01Layout.size().width, "width of node01");
        assertEquals(20.0f, node01Layout.size().height, "height of node01");
        assertEquals(50.0f, node01Layout.location().x, "x of node01");
        assertEquals(0.0f, node01Layout.location().y, "y of node01");
    }

    @Test
    @DisplayName("gridflex_kitchen_sink_minimise__content_box")
    void gridflexKitchenSinkMinimiseContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node00 = tree.newLeaf(node00Style);

        Style node01Style = new Style();
        node01Style.boxSizing = BoxSizing.CONTENT_BOX;
        NodeId node01 = tree.newLeaf(node01Style);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.GRID;
        node0Style.gridTemplateColumns.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateColumns.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.length(20.0f)));
        NodeId node0 = tree.newWithChildren(node0Style, node00, node01);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(100.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(50.0f, node00Layout.size().width, "width of node00");
        assertEquals(20.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node01Layout = tree.getLayout(node01);
        assertEquals(50.0f, node01Layout.size().width, "width of node01");
        assertEquals(20.0f, node01Layout.size().height, "height of node01");
        assertEquals(50.0f, node01Layout.location().x, "x of node01");
        assertEquals(0.0f, node01Layout.location().y, "y of node01");
    }

    @Test
    @DisplayName("gridflex_kitchen_sink_minimise2__border_box")
    void gridflexKitchenSinkMinimise2BorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.size = new Size<>(Dimension.length(20.0f), Dimension.length(20.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.display = Display.GRID;
        node0Style.gridTemplateColumns.add(TrackSizingFunction.auto());
        node0Style.gridTemplateRows.add(TrackSizingFunction.auto());
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.flexGrow = 1.0f;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(20.0f, node00Layout.size().width, "width of node00");
        assertEquals(20.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("gridflex_kitchen_sink_minimise2__content_box")
    void gridflexKitchenSinkMinimise2ContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.size = new Size<>(Dimension.length(20.0f), Dimension.length(20.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.GRID;
        node0Style.gridTemplateColumns.add(TrackSizingFunction.auto());
        node0Style.gridTemplateRows.add(TrackSizingFunction.auto());
        NodeId node0 = tree.newWithChildren(node0Style, node00);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.flexGrow = 1.0f;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(20.0f, node00Layout.size().width, "width of node00");
        assertEquals(20.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
    }

    @Test
    @DisplayName("gridflex_kitchen_sink_minimise3__border_box")
    void gridflexKitchenSinkMinimise3BorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.size = new Size<>(Dimension.length(20.0f), Dimension.length(20.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node01Style = new Style();
        NodeId node01 = tree.newLeaf(node01Style);

        Style node02Style = new Style();
        NodeId node02 = tree.newLeaf(node02Style);

        Style node03Style = new Style();
        NodeId node03 = tree.newLeaf(node03Style);

        Style node0Style = new Style();
        node0Style.display = Display.GRID;
        node0Style.gridTemplateColumns.add(TrackSizingFunction.auto());
        node0Style.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.percent(0.1f)));
        node0Style.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.percent(0.3f)));
        node0Style.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.percent(0.1f)));
        NodeId node0 = tree.newWithChildren(node0Style, node00, node01, node02, node03);

        Style nodeStyle = new Style();
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(20.0f, node00Layout.size().width, "width of node00");
        assertEquals(20.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node01Layout = tree.getLayout(node01);
        assertEquals(2.0f, node01Layout.size().width, "width of node01");
        assertEquals(6.0f, node01Layout.size().height, "height of node01");
        assertEquals(20.0f, node01Layout.location().x, "x of node01");
        assertEquals(0.0f, node01Layout.location().y, "y of node01");
        Layout node02Layout = tree.getLayout(node02);
        assertEquals(20.0f, node02Layout.size().width, "width of node02");
        assertEquals(2.0f, node02Layout.size().height, "height of node02");
        assertEquals(0.0f, node02Layout.location().x, "x of node02");
        assertEquals(6.0f, node02Layout.location().y, "y of node02");
        Layout node03Layout = tree.getLayout(node03);
        assertEquals(2.0f, node03Layout.size().width, "width of node03");
        assertEquals(2.0f, node03Layout.size().height, "height of node03");
        assertEquals(20.0f, node03Layout.location().x, "x of node03");
        assertEquals(6.0f, node03Layout.location().y, "y of node03");
    }

    @Test
    @DisplayName("gridflex_kitchen_sink_minimise3__content_box")
    void gridflexKitchenSinkMinimise3ContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        node00Style.size = new Size<>(Dimension.length(20.0f), Dimension.length(20.0f));
        NodeId node00 = tree.newLeaf(node00Style);

        Style node01Style = new Style();
        node01Style.boxSizing = BoxSizing.CONTENT_BOX;
        NodeId node01 = tree.newLeaf(node01Style);

        Style node02Style = new Style();
        node02Style.boxSizing = BoxSizing.CONTENT_BOX;
        NodeId node02 = tree.newLeaf(node02Style);

        Style node03Style = new Style();
        node03Style.boxSizing = BoxSizing.CONTENT_BOX;
        NodeId node03 = tree.newLeaf(node03Style);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.GRID;
        node0Style.gridTemplateColumns.add(TrackSizingFunction.auto());
        node0Style.gridTemplateColumns.add(TrackSizingFunction.fixed(LengthPercentage.percent(0.1f)));
        node0Style.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.percent(0.3f)));
        node0Style.gridTemplateRows.add(TrackSizingFunction.fixed(LengthPercentage.percent(0.1f)));
        NodeId node0 = tree.newWithChildren(node0Style, node00, node01, node02, node03);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        nodeStyle.size = new Size<>(Dimension.length(50.0f), Dimension.AUTO);
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(20.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(20.0f, node00Layout.size().width, "width of node00");
        assertEquals(20.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node01Layout = tree.getLayout(node01);
        assertEquals(2.0f, node01Layout.size().width, "width of node01");
        assertEquals(6.0f, node01Layout.size().height, "height of node01");
        assertEquals(20.0f, node01Layout.location().x, "x of node01");
        assertEquals(0.0f, node01Layout.location().y, "y of node01");
        Layout node02Layout = tree.getLayout(node02);
        assertEquals(20.0f, node02Layout.size().width, "width of node02");
        assertEquals(2.0f, node02Layout.size().height, "height of node02");
        assertEquals(0.0f, node02Layout.location().x, "x of node02");
        assertEquals(6.0f, node02Layout.location().y, "y of node02");
        Layout node03Layout = tree.getLayout(node03);
        assertEquals(2.0f, node03Layout.size().width, "width of node03");
        assertEquals(2.0f, node03Layout.size().height, "height of node03");
        assertEquals(20.0f, node03Layout.location().x, "x of node03");
        assertEquals(6.0f, node03Layout.location().y, "y of node03");
    }

    @Test
    @DisplayName("gridflex_row_integration__border_box")
    void gridflexRowIntegrationBorderBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        MeasureFunc node00Measure = ahemTextMeasure("HH", false);
        NodeId node00 = tree.newLeafWithMeasure(node00Style, node00Measure);

        Style node01Style = new Style();
        MeasureFunc node01Measure = ahemTextMeasure("HH", false);
        NodeId node01 = tree.newLeafWithMeasure(node01Style, node01Measure);

        Style node02Style = new Style();
        MeasureFunc node02Measure = ahemTextMeasure("HH", false);
        NodeId node02 = tree.newLeafWithMeasure(node02Style, node02Measure);

        Style node03Style = new Style();
        MeasureFunc node03Measure = ahemTextMeasure("HH", false);
        NodeId node03 = tree.newLeafWithMeasure(node03Style, node03Measure);

        Style node0Style = new Style();
        node0Style.display = Display.GRID;
        node0Style.gridTemplateColumns.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateColumns.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateRows.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateRows.add(TrackSizingFunction.fr(1.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00, node01, node02, node03);

        Style nodeStyle = new Style();
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(40.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(20.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node01Layout = tree.getLayout(node01);
        assertEquals(20.0f, node01Layout.size().width, "width of node01");
        assertEquals(10.0f, node01Layout.size().height, "height of node01");
        assertEquals(20.0f, node01Layout.location().x, "x of node01");
        assertEquals(0.0f, node01Layout.location().y, "y of node01");
        Layout node02Layout = tree.getLayout(node02);
        assertEquals(20.0f, node02Layout.size().width, "width of node02");
        assertEquals(10.0f, node02Layout.size().height, "height of node02");
        assertEquals(0.0f, node02Layout.location().x, "x of node02");
        assertEquals(10.0f, node02Layout.location().y, "y of node02");
        Layout node03Layout = tree.getLayout(node03);
        assertEquals(20.0f, node03Layout.size().width, "width of node03");
        assertEquals(10.0f, node03Layout.size().height, "height of node03");
        assertEquals(20.0f, node03Layout.location().x, "x of node03");
        assertEquals(10.0f, node03Layout.location().y, "y of node03");
    }

    @Test
    @DisplayName("gridflex_row_integration__content_box")
    void gridflexRowIntegrationContentBox() {
        TaffyTree tree = new TaffyTree();

        Style node00Style = new Style();
        node00Style.boxSizing = BoxSizing.CONTENT_BOX;
        MeasureFunc node00Measure = ahemTextMeasure("HH", false);
        NodeId node00 = tree.newLeafWithMeasure(node00Style, node00Measure);

        Style node01Style = new Style();
        node01Style.boxSizing = BoxSizing.CONTENT_BOX;
        MeasureFunc node01Measure = ahemTextMeasure("HH", false);
        NodeId node01 = tree.newLeafWithMeasure(node01Style, node01Measure);

        Style node02Style = new Style();
        node02Style.boxSizing = BoxSizing.CONTENT_BOX;
        MeasureFunc node02Measure = ahemTextMeasure("HH", false);
        NodeId node02 = tree.newLeafWithMeasure(node02Style, node02Measure);

        Style node03Style = new Style();
        node03Style.boxSizing = BoxSizing.CONTENT_BOX;
        MeasureFunc node03Measure = ahemTextMeasure("HH", false);
        NodeId node03 = tree.newLeafWithMeasure(node03Style, node03Measure);

        Style node0Style = new Style();
        node0Style.boxSizing = BoxSizing.CONTENT_BOX;
        node0Style.display = Display.GRID;
        node0Style.gridTemplateColumns.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateColumns.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateRows.add(TrackSizingFunction.fr(1.0f));
        node0Style.gridTemplateRows.add(TrackSizingFunction.fr(1.0f));
        NodeId node0 = tree.newWithChildren(node0Style, node00, node01, node02, node03);

        Style nodeStyle = new Style();
        nodeStyle.boxSizing = BoxSizing.CONTENT_BOX;
        NodeId node = tree.newWithChildren(nodeStyle, node0);


        tree.computeLayout(node, Size.maxContent());

        Layout nodeLayout = tree.getLayout(node);
        assertEquals(40.0f, nodeLayout.size().width, "width of node");
        assertEquals(20.0f, nodeLayout.size().height, "height of node");
        assertEquals(0.0f, nodeLayout.location().x, "x of node");
        assertEquals(0.0f, nodeLayout.location().y, "y of node");
        Layout node0Layout = tree.getLayout(node0);
        assertEquals(40.0f, node0Layout.size().width, "width of node0");
        assertEquals(20.0f, node0Layout.size().height, "height of node0");
        assertEquals(0.0f, node0Layout.location().x, "x of node0");
        assertEquals(0.0f, node0Layout.location().y, "y of node0");
        Layout node00Layout = tree.getLayout(node00);
        assertEquals(20.0f, node00Layout.size().width, "width of node00");
        assertEquals(10.0f, node00Layout.size().height, "height of node00");
        assertEquals(0.0f, node00Layout.location().x, "x of node00");
        assertEquals(0.0f, node00Layout.location().y, "y of node00");
        Layout node01Layout = tree.getLayout(node01);
        assertEquals(20.0f, node01Layout.size().width, "width of node01");
        assertEquals(10.0f, node01Layout.size().height, "height of node01");
        assertEquals(20.0f, node01Layout.location().x, "x of node01");
        assertEquals(0.0f, node01Layout.location().y, "y of node01");
        Layout node02Layout = tree.getLayout(node02);
        assertEquals(20.0f, node02Layout.size().width, "width of node02");
        assertEquals(10.0f, node02Layout.size().height, "height of node02");
        assertEquals(0.0f, node02Layout.location().x, "x of node02");
        assertEquals(10.0f, node02Layout.location().y, "y of node02");
        Layout node03Layout = tree.getLayout(node03);
        assertEquals(20.0f, node03Layout.size().width, "width of node03");
        assertEquals(10.0f, node03Layout.size().height, "height of node03");
        assertEquals(20.0f, node03Layout.location().x, "x of node03");
        assertEquals(10.0f, node03Layout.location().y, "y of node03");
    }

}
