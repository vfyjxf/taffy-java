package dev.vfyjxf.taffy.wpt.sizing_aspect_ratio;

import dev.vfyjxf.taffy.geometry.*;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.tree.*;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Generated from WPT.
 *
 * Source: https://github.com/web-platform-tests/wpt
 *
 * NOTE: These tests are generated and live under a dedicated package
 * (`dev.vfyjxf.taffy.wpt.*`) and are intentionally
 * kept separate from taffy fixture-based tests.
 */
public class WptSizingAspectRatioTest_2 {

    private static final float EPSILON = 0.5f;
    private static MeasureFunc replacedIntrinsicSize(float intrinsicWidth, float intrinsicHeight) {
        return (knownDimensions, availableSpace) -> {
            float width = Float.isNaN(knownDimensions.width) ? intrinsicWidth : knownDimensions.width;
            float height = Float.isNaN(knownDimensions.height) ? intrinsicHeight : knownDimensions.height;
            if (!Float.isNaN(knownDimensions.width) && Float.isNaN(knownDimensions.height)
                    && intrinsicWidth > 0f) {
                height = knownDimensions.width * intrinsicHeight / intrinsicWidth;
            } else if (!Float.isNaN(knownDimensions.height) && Float.isNaN(knownDimensions.width)
                    && intrinsicHeight > 0f) {
                width = knownDimensions.height * intrinsicWidth / intrinsicHeight;
            }
            return new FloatSize(width, height);
        };
    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-002.html")
    void block_aspect_ratio_002() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.CONTENT_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(100.0f));
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.aspectRatio = 1.0f;
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;

        n1 = tree.newLeaf(s1);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-004.html")
    void block_aspect_ratio_004() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.BORDER_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.aspectRatio = 2.0f;
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(50.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;

        n1 = tree.newLeaf(s1);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(50.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-005.html")
    void block_aspect_ratio_005() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.CONTENT_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.AUTO);
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.aspectRatio = 1.0f;
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(50.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;

        n1 = tree.newLeaf(s1);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(50.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-006.html")
    void block_aspect_ratio_006() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.BORDER_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(50.0f));
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.aspectRatio = 2.0f;
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(25.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;

        n1 = tree.newLeaf(s1);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(50.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-007.html")
    void block_aspect_ratio_007() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.CONTENT_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(25.0f));
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.aspectRatio = 4.0f;
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(25.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;

        n1 = tree.newLeaf(s1);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(50.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-008.html")
    void block_aspect_ratio_008() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.CONTENT_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s2.aspectRatio = 1.0f;
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;
        NodeId n2;

        n2 = tree.newLeaf(s2);
        n1 = tree.newWithChildren(s1, n2);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // div[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(100.000f, l2.size().width, EPSILON, "width");
        assertEquals(100.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-009.html")
    void block_aspect_ratio_009() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.CONTENT_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.aspectRatio = 2.0f;
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        s2.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;
        NodeId n2;

        n2 = tree.newLeaf(s2);
        n1 = tree.newWithChildren(s1, n2);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // div[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(100.000f, l2.size().width, EPSILON, "width");
        assertEquals(100.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-010.html")
    void block_aspect_ratio_010() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.CONTENT_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.aspectRatio = 1.0f;
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.overflow = new TaffyPoint<>(Overflow.HIDDEN, Overflow.HIDDEN);

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        s2.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s3 = new TaffyStyle();
        s3.boxSizing = BoxSizing.CONTENT_BOX;
        s3.direction = TaffyDirection.LTR;
        s3.display = TaffyDisplay.BLOCK;
        s3.flexDirection = FlexDirection.ROW;
        s3.flexWrap = FlexWrap.NO_WRAP;
        s3.flexGrow = 0.0f;
        s3.flexShrink = 1.0f;
        s3.flexBasis = TaffyDimension.AUTO;
        s3.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(500.0f));
        s3.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s3.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s3.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s3.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s3.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s3.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s3.gridAutoFlow = GridAutoFlow.ROW;
        s3.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s3.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;
        NodeId n2;
        NodeId n3;

        n2 = tree.newLeaf(s2);
        n3 = tree.newLeaf(s3);
        n1 = tree.newWithChildren(s1, n2, n3);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // div[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(100.000f, l2.size().width, EPSILON, "width");
        assertEquals(100.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

        // div[3]
        Layout l3 = tree.getLayout(n3);
        assertEquals(100.000f, l3.size().width, EPSILON, "width");
        assertEquals(500.000f, l3.size().height, EPSILON, "height");
        assertEquals(0.000f, l3.location().x, EPSILON, "x");
        assertEquals(100.000f, l3.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-011.html")
    void block_aspect_ratio_011() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.CONTENT_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.aspectRatio = 1.0f;
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.overflow = new TaffyPoint<>(Overflow.HIDDEN, Overflow.HIDDEN);

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.length(85.0f), TaffyDimension.length(100.0f));
        s2.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s3 = new TaffyStyle();
        s3.boxSizing = BoxSizing.CONTENT_BOX;
        s3.direction = TaffyDirection.LTR;
        s3.display = TaffyDisplay.BLOCK;
        s3.flexDirection = FlexDirection.ROW;
        s3.flexWrap = FlexWrap.NO_WRAP;
        s3.flexGrow = 0.0f;
        s3.flexShrink = 1.0f;
        s3.flexBasis = TaffyDimension.AUTO;
        s3.size = new TaffySize<>(TaffyDimension.length(85.0f), TaffyDimension.length(500.0f));
        s3.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s3.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s3.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s3.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s3.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s3.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s3.gridAutoFlow = GridAutoFlow.ROW;
        s3.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s3.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;
        NodeId n2;
        NodeId n3;

        n2 = tree.newLeaf(s2);
        n3 = tree.newLeaf(s3);
        n1 = tree.newWithChildren(s1, n2, n3);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // div[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(85.000f, l2.size().width, EPSILON, "width");
        assertEquals(100.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

        // div[3]
        Layout l3 = tree.getLayout(n3);
        assertEquals(85.000f, l3.size().width, EPSILON, "width");
        assertEquals(500.000f, l3.size().height, EPSILON, "height");
        assertEquals(0.000f, l3.location().x, EPSILON, "x");
        assertEquals(100.000f, l3.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-012.html")
    void block_aspect_ratio_012() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.CONTENT_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.aspectRatio = 1.0f;
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.overflow = new TaffyPoint<>(Overflow.SCROLL, Overflow.SCROLL);

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.length(85.0f), TaffyDimension.length(100.0f));
        s2.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s3 = new TaffyStyle();
        s3.boxSizing = BoxSizing.CONTENT_BOX;
        s3.direction = TaffyDirection.LTR;
        s3.display = TaffyDisplay.BLOCK;
        s3.flexDirection = FlexDirection.ROW;
        s3.flexWrap = FlexWrap.NO_WRAP;
        s3.flexGrow = 0.0f;
        s3.flexShrink = 1.0f;
        s3.flexBasis = TaffyDimension.AUTO;
        s3.size = new TaffySize<>(TaffyDimension.length(85.0f), TaffyDimension.length(500.0f));
        s3.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s3.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s3.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s3.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s3.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s3.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s3.gridAutoFlow = GridAutoFlow.ROW;
        s3.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s3.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;
        NodeId n2;
        NodeId n3;

        n2 = tree.newLeaf(s2);
        n3 = tree.newLeaf(s3);
        n1 = tree.newWithChildren(s1, n2, n3);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // div[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(85.000f, l2.size().width, EPSILON, "width");
        assertEquals(100.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

        // div[3]
        Layout l3 = tree.getLayout(n3);
        assertEquals(85.000f, l3.size().width, EPSILON, "width");
        assertEquals(500.000f, l3.size().height, EPSILON, "height");
        assertEquals(0.000f, l3.location().x, EPSILON, "x");
        assertEquals(100.000f, l3.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-013.html")
    void block_aspect_ratio_013() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.CONTENT_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.aspectRatio = 1.0f;
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        s2.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;
        NodeId n2;

        n2 = tree.newLeaf(s2);
        n1 = tree.newWithChildren(s1, n2);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // div[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(100.000f, l2.size().width, EPSILON, "width");
        assertEquals(50.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-015.html")
    void block_aspect_ratio_015() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.CONTENT_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(100.0f));
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.aspectRatio = 0.5f;
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(0.0f));
        s2.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;
        NodeId n2;

        n2 = tree.newLeaf(s2);
        n1 = tree.newWithChildren(s1, n2);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // div[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(100.000f, l2.size().width, EPSILON, "width");
        assertEquals(0.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-016.html")
    void block_aspect_ratio_016() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.CONTENT_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(100.0f));
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.aspectRatio = 1.0f;
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(0.0f));
        s2.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;
        NodeId n2;

        n2 = tree.newLeaf(s2);
        n1 = tree.newWithChildren(s1, n2);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // div[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(50.000f, l2.size().width, EPSILON, "width");
        assertEquals(0.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-019.html")
    void block_aspect_ratio_019() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.CONTENT_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.float_ = TaffyFloat.LEFT;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.aspectRatio = 1.0f;
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(0.0f));
        s2.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;
        NodeId n2;

        n2 = tree.newLeaf(s2);
        n1 = tree.newWithChildren(s1, n2);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // div[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(100.000f, l2.size().width, EPSILON, "width");
        assertEquals(0.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-021.html")
    void block_aspect_ratio_021() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.CONTENT_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.maxContent(), TaffyDimension.AUTO);
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.maxSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(100.0f));
        s1.aspectRatio = 1.0f;
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.length(200.0f), TaffyDimension.length(0.0f));
        s2.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;
        NodeId n2;

        n2 = tree.newLeaf(s2);
        n1 = tree.newWithChildren(s1, n2);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // div[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(200.000f, l2.size().width, EPSILON, "width");
        assertEquals(0.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-022.html")
    void block_aspect_ratio_022() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.CONTENT_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(200.0f));
        s1.maxSize = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        s1.aspectRatio = 1.0f;
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.length(0.0f));
        s2.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s3 = new TaffyStyle();
        s3.boxSizing = BoxSizing.CONTENT_BOX;
        s3.direction = TaffyDirection.LTR;
        s3.display = TaffyDisplay.BLOCK;
        s3.flexDirection = FlexDirection.ROW;
        s3.flexWrap = FlexWrap.NO_WRAP;
        s3.flexGrow = 0.0f;
        s3.flexShrink = 1.0f;
        s3.flexBasis = TaffyDimension.AUTO;
        s3.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        s3.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s3.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s3.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s3.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s3.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s3.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s3.gridAutoFlow = GridAutoFlow.ROW;
        s3.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s3.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;
        NodeId n2;
        NodeId n3;

        n2 = tree.newLeaf(s2);
        n3 = tree.newLeaf(s3);
        n1 = tree.newWithChildren(s1, n2, n3);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(200.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // div[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(300.000f, l2.size().width, EPSILON, "width");
        assertEquals(0.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

        // div[3]
        Layout l3 = tree.getLayout(n3);
        assertEquals(100.000f, l3.size().width, EPSILON, "width");
        assertEquals(100.000f, l3.size().height, EPSILON, "height");
        assertEquals(0.000f, l3.location().x, EPSILON, "x");
        assertEquals(0.000f, l3.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-023.html")
    void block_aspect_ratio_023() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.CONTENT_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s1.minSize = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(0.0f));
        s1.maxSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(40.0f));
        s1.aspectRatio = 1.0f;
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(0.0f));
        s2.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s3 = new TaffyStyle();
        s3.boxSizing = BoxSizing.CONTENT_BOX;
        s3.direction = TaffyDirection.LTR;
        s3.display = TaffyDisplay.BLOCK;
        s3.flexDirection = FlexDirection.ROW;
        s3.flexWrap = FlexWrap.NO_WRAP;
        s3.flexGrow = 0.0f;
        s3.flexShrink = 1.0f;
        s3.flexBasis = TaffyDimension.AUTO;
        s3.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        s3.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s3.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s3.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s3.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s3.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s3.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s3.gridAutoFlow = GridAutoFlow.ROW;
        s3.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s3.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;
        NodeId n2;
        NodeId n3;

        n2 = tree.newLeaf(s2);
        n3 = tree.newLeaf(s3);
        n1 = tree.newWithChildren(s1, n2, n3);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(40.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // div[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(50.000f, l2.size().width, EPSILON, "width");
        assertEquals(0.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

        // div[3]
        Layout l3 = tree.getLayout(n3);
        assertEquals(100.000f, l3.size().width, EPSILON, "width");
        assertEquals(100.000f, l3.size().height, EPSILON, "height");
        assertEquals(0.000f, l3.location().x, EPSILON, "x");
        assertEquals(0.000f, l3.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-024.html")
    void block_aspect_ratio_024() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.CONTENT_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.minContent(), TaffyDimension.length(100.0f));
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.percent(1.0f));
        s2.aspectRatio = 1.0f;
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s3 = new TaffyStyle();
        s3.boxSizing = BoxSizing.CONTENT_BOX;
        s3.direction = TaffyDirection.LTR;
        s3.display = TaffyDisplay.BLOCK;
        s3.flexDirection = FlexDirection.ROW;
        s3.flexWrap = FlexWrap.NO_WRAP;
        s3.flexGrow = 0.0f;
        s3.flexShrink = 1.0f;
        s3.flexBasis = TaffyDimension.AUTO;
        s3.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(0.0f));
        s3.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s3.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s3.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s3.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s3.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s3.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s3.gridAutoFlow = GridAutoFlow.ROW;
        s3.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s3.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;
        NodeId n2;
        NodeId n3;

        n3 = tree.newLeaf(s3);
        n2 = tree.newWithChildren(s2, n3);
        n1 = tree.newWithChildren(s1, n2);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div#target
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // div[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(100.000f, l2.size().width, EPSILON, "width");
        assertEquals(100.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

        // div[3]
        Layout l3 = tree.getLayout(n3);
        assertEquals(50.000f, l3.size().width, EPSILON, "width");
        assertEquals(0.000f, l3.size().height, EPSILON, "height");
        assertEquals(0.000f, l3.location().x, EPSILON, "x");
        assertEquals(0.000f, l3.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-025.html")
    void block_aspect_ratio_025() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.CONTENT_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.float_ = TaffyFloat.LEFT;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.percent(1.0f));
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(100.0f));
        s1.aspectRatio = 1.0f;
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;

        n1 = tree.newLeaf(s1);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/block-aspect-ratio-028.html")
    void block_aspect_ratio_028() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();

        TaffyStyle s0 = new TaffyStyle();
        s0.boxSizing = BoxSizing.CONTENT_BOX;
        s0.direction = TaffyDirection.LTR;
        s0.display = TaffyDisplay.BLOCK;
        s0.flexDirection = FlexDirection.ROW;
        s0.flexWrap = FlexWrap.NO_WRAP;
        s0.flexGrow = 0.0f;
        s0.flexShrink = 1.0f;
        s0.flexBasis = TaffyDimension.AUTO;
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(457f));
        s0.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s0.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s0.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s0.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s0.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s0.gridAutoFlow = GridAutoFlow.ROW;
        s0.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s0.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s1 = new TaffyStyle();
        s1.boxSizing = BoxSizing.CONTENT_BOX;
        s1.direction = TaffyDirection.LTR;
        s1.display = TaffyDisplay.BLOCK;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.minContent(), TaffyDimension.length(100.0f));
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.percent(1.0f));
        s2.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s3 = new TaffyStyle();
        s3.boxSizing = BoxSizing.CONTENT_BOX;
        s3.direction = TaffyDirection.LTR;
        s3.display = TaffyDisplay.BLOCK;
        s3.flexDirection = FlexDirection.ROW;
        s3.flexWrap = FlexWrap.NO_WRAP;
        s3.flexGrow = 0.0f;
        s3.flexShrink = 1.0f;
        s3.flexBasis = TaffyDimension.AUTO;
        s3.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s3.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.percent(1.0f));
        s3.aspectRatio = 1.0f;
        s3.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s3.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s3.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s3.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s3.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s3.gridAutoFlow = GridAutoFlow.ROW;
        s3.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s3.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;
        NodeId n2;
        NodeId n3;

        n3 = tree.newLeaf(s3);
        n2 = tree.newWithChildren(s2, n3);
        n1 = tree.newWithChildren(s1, n2);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div#target
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // div[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(100.000f, l2.size().width, EPSILON, "width");
        assertEquals(100.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

        // div[3]
        Layout l3 = tree.getLayout(n3);
        assertEquals(100.000f, l3.size().width, EPSILON, "width");
        assertEquals(100.000f, l3.size().height, EPSILON, "height");
        assertEquals(0.000f, l3.location().x, EPSILON, "x");
        assertEquals(0.000f, l3.location().y, EPSILON, "y");

    }

}
