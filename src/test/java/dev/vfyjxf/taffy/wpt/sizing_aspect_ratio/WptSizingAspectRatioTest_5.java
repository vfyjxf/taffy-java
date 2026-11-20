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
public class WptSizingAspectRatioTest_5 {

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
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-017.html")
    void flex_aspect_ratio_017() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(800.0f), TaffyDimension.length(100.0f));
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
        s2.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(100.0f));
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.aspectRatio = 0.5f;
        s2.itemIsReplaced = true;
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.overflow = new TaffyPoint<>(Overflow.CLIP, Overflow.CLIP);

        TaffyStyle s3 = new TaffyStyle();
        s3.boxSizing = BoxSizing.CONTENT_BOX;
        s3.direction = TaffyDirection.LTR;
        s3.display = TaffyDisplay.BLOCK;
        s3.flexDirection = FlexDirection.ROW;
        s3.flexWrap = FlexWrap.NO_WRAP;
        s3.flexGrow = 0.0f;
        s3.flexShrink = 1.0f;
        s3.flexBasis = TaffyDimension.AUTO;
        s3.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(100.0f));
        s3.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
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
        assertEquals(800.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // img[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(50.000f, l2.size().width, EPSILON, "width");
        assertEquals(100.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

        // div[3]
        Layout l3 = tree.getLayout(n3);
        assertEquals(50.000f, l3.size().width, EPSILON, "width");
        assertEquals(100.000f, l3.size().height, EPSILON, "height");
        assertEquals(50.000f, l3.location().x, EPSILON, "x");
        assertEquals(0.000f, l3.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-018.html")
    void flex_aspect_ratio_018() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
        s1.flexDirection = FlexDirection.COLUMN;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(800.0f), TaffyDimension.length(100.0f));
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
        s2.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.aspectRatio = 4.0f;
        s2.itemIsReplaced = true;
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.overflow = new TaffyPoint<>(Overflow.CLIP, Overflow.CLIP);

        TaffyStyle s3 = new TaffyStyle();
        s3.boxSizing = BoxSizing.CONTENT_BOX;
        s3.direction = TaffyDirection.LTR;
        s3.display = TaffyDisplay.BLOCK;
        s3.flexDirection = FlexDirection.ROW;
        s3.flexWrap = FlexWrap.NO_WRAP;
        s3.flexGrow = 0.0f;
        s3.flexShrink = 1.0f;
        s3.flexBasis = TaffyDimension.AUTO;
        s3.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(75.0f));
        s3.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
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
        assertEquals(800.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // img[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(100.000f, l2.size().width, EPSILON, "width");
        assertEquals(25.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

        // div[3]
        Layout l3 = tree.getLayout(n3);
        assertEquals(100.000f, l3.size().width, EPSILON, "width");
        assertEquals(75.000f, l3.size().height, EPSILON, "height");
        assertEquals(0.000f, l3.location().x, EPSILON, "x");
        assertEquals(25.000f, l3.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-019.html")
    void flex_aspect_ratio_019() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(800.0f), TaffyDimension.length(100.0f));
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
        s2.flexBasis = TaffyDimension.content();
        s2.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(100.0f));
        s2.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.AUTO);
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
        assertEquals(800.000f, l1.size().width, EPSILON, "width");
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
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-020.html")
    void flex_aspect_ratio_020() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
        s1.flexDirection = FlexDirection.COLUMN;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(800.0f), TaffyDimension.length(100.0f));
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
        s2.flexBasis = TaffyDimension.content();
        s2.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(0.0f));
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
        assertEquals(800.000f, l1.size().width, EPSILON, "width");
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
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-021.html")
    void flex_aspect_ratio_021() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(800.0f), TaffyDimension.length(100.0f));
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
        s2.flexBasis = TaffyDimension.content();
        s2.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(100.0f));
        s2.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.AUTO);
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
        assertEquals(800.000f, l1.size().width, EPSILON, "width");
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
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-022.html")
    void flex_aspect_ratio_022() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
        s1.flexDirection = FlexDirection.COLUMN;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(800.0f), TaffyDimension.length(100.0f));
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
        s2.flexBasis = TaffyDimension.content();
        s2.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(0.0f));
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
        assertEquals(800.000f, l1.size().width, EPSILON, "width");
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
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-023.html")
    void flex_aspect_ratio_023() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
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
        s2.flexGrow = 1.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.percent(0.0f);
        s2.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.AUTO);
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(0.0f));
        s2.aspectRatio = 1.0f;
        s2.itemIsReplaced = true;
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.overflow = new TaffyPoint<>(Overflow.CLIP, Overflow.CLIP);

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

        // img[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(100.000f, l2.size().width, EPSILON, "width");
        assertEquals(100.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-025.html")
    void flex_aspect_ratio_025() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(25.0f));
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
        s2.boxSizing = BoxSizing.BORDER_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(25.0f));
        s2.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.AUTO);
        s2.aspectRatio = 8.0f;
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(10.0f), LengthPercentage.length(0.0f), LengthPercentage.length(15.0f), LengthPercentage.length(0.0f));
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
        assertEquals(800.000f, l1.size().width, EPSILON, "width");
        assertEquals(25.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // div[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(200.000f, l2.size().width, EPSILON, "width");
        assertEquals(25.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-026.html")
    void flex_aspect_ratio_026() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(785f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
        s1.float_ = TaffyFloat.LEFT;
        s1.flexDirection = FlexDirection.COLUMN;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(25.0f), TaffyDimension.AUTO);
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
        s2.boxSizing = BoxSizing.BORDER_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.length(25.0f), TaffyDimension.AUTO);
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(0.0f));
        s2.aspectRatio = 0.125f;
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(15.0f), LengthPercentage.length(0.0f), LengthPercentage.length(10.0f), LengthPercentage.length(0.0f));
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
        s3.size = new TaffySize<>(TaffyDimension.length(10.0f), TaffyDimension.length(190.0f));
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

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(25.000f, l1.size().width, EPSILON, "width");
        assertEquals(200.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // div[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(25.000f, l2.size().width, EPSILON, "width");
        assertEquals(200.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

        // div[3]
        Layout l3 = tree.getLayout(n3);
        assertEquals(10.000f, l3.size().width, EPSILON, "width");
        assertEquals(190.000f, l3.size().height, EPSILON, "height");
        assertEquals(15.000f, l3.location().x, EPSILON, "x");
        assertEquals(10.000f, l3.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-027.html")
    void flex_aspect_ratio_027() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
        s1.flexDirection = FlexDirection.COLUMN;
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
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.aspectRatio = 1.0f;
        s2.itemIsReplaced = true;
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.overflow = new TaffyPoint<>(Overflow.CLIP, Overflow.CLIP);

        NodeId n0;
        NodeId n1;
        NodeId n2;

        n2 = tree.newLeafWithMeasure(s2, replacedIntrinsicSize(200.0f, 200.0f));
        n1 = tree.newWithChildren(s1, n2);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // img[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(100.000f, l2.size().width, EPSILON, "width");
        assertEquals(100.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-028.html")
    void flex_aspect_ratio_028() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
        s1.flexDirection = FlexDirection.COLUMN;
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
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.aspectRatio = 1.0f;
        s2.itemIsReplaced = true;
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.overflow = new TaffyPoint<>(Overflow.CLIP, Overflow.CLIP);

        NodeId n0;
        NodeId n1;
        NodeId n2;

        n2 = tree.newLeafWithMeasure(s2, replacedIntrinsicSize(200.0f, 200.0f));
        n1 = tree.newWithChildren(s1, n2);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // img[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(100.000f, l2.size().width, EPSILON, "width");
        assertEquals(100.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-029.html")
    void flex_aspect_ratio_029() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(800.0f), TaffyDimension.length(100.0f));
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
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.maxSize = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        s2.aspectRatio = 1.0f;
        s2.itemIsReplaced = true;
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.overflow = new TaffyPoint<>(Overflow.CLIP, Overflow.CLIP);

        NodeId n0;
        NodeId n1;
        NodeId n2;

        n2 = tree.newLeafWithMeasure(s2, replacedIntrinsicSize(200.0f, 200.0f));
        n1 = tree.newWithChildren(s1, n2);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(800.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // img[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(100.000f, l2.size().width, EPSILON, "width");
        assertEquals(100.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-030.html")
    void flex_aspect_ratio_030() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(800.0f), TaffyDimension.length(100.0f));
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
        s2.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.aspectRatio = 1.0f;
        s2.itemIsReplaced = true;
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.overflow = new TaffyPoint<>(Overflow.CLIP, Overflow.CLIP);

        NodeId n0;
        NodeId n1;
        NodeId n2;

        n2 = tree.newLeaf(s2);
        n1 = tree.newWithChildren(s1, n2);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(800.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // img[2]
        Layout l2 = tree.getLayout(n2);
        assertEquals(100.000f, l2.size().width, EPSILON, "width");
        assertEquals(100.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-031.html")
    void flex_aspect_ratio_031() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(800.0f), TaffyDimension.length(50.0f));
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
        s2.boxSizing = BoxSizing.BORDER_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.aspectRatio = 1.0f;
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(100.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
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
        assertEquals(800.000f, l1.size().width, EPSILON, "width");
        assertEquals(50.000f, l1.size().height, EPSILON, "height");
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
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-032.html")
    void flex_aspect_ratio_032() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
        s1.flexDirection = FlexDirection.COLUMN;
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
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
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
        s3.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.percent(1.0f));
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
        assertEquals(100.000f, l3.size().height, EPSILON, "height");
        assertEquals(0.000f, l3.location().x, EPSILON, "x");
        assertEquals(0.000f, l3.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-033.html")
    void flex_aspect_ratio_033() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
        s1.flexDirection = FlexDirection.COLUMN;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(800.0f), TaffyDimension.length(100.0f));
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
        s2.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
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
        s3.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.percent(1.0f));
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

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(800.000f, l1.size().width, EPSILON, "width");
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

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-034.html")
    void flex_aspect_ratio_034() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
        s1.flexDirection = FlexDirection.COLUMN;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.alignItems = AlignItems.START;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(800.0f), TaffyDimension.length(100.0f));
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
        s2.flexShrink = 0.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.minSize = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(0.0f));
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
        assertEquals(800.000f, l1.size().width, EPSILON, "width");
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
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-035.html")
    void flex_aspect_ratio_035() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
        s1.flexDirection = FlexDirection.COLUMN;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.alignItems = AlignItems.START;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(100.0f));
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
        s2.flexShrink = 0.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.minSize = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(0.0f));
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
        assertEquals(0.000f, l1.size().width, EPSILON, "width");
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
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-036.html")
    void flex_aspect_ratio_036() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
        s1.flexDirection = FlexDirection.COLUMN;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.alignItems = AlignItems.START;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(800.0f), TaffyDimension.length(100.0f));
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
        s2.flexShrink = 0.0f;
        s2.flexBasis = TaffyDimension.content();
        s2.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.minSize = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(0.0f));
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
        assertEquals(800.000f, l1.size().width, EPSILON, "width");
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
    @DisplayName("css/css-sizing/aspect-ratio/flex-aspect-ratio-037.html")
    void flex_aspect_ratio_037() {
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
        s0.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.AUTO);
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
        s1.display = TaffyDisplay.FLEX;
        s1.flexDirection = FlexDirection.COLUMN;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.alignItems = AlignItems.START;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(800.0f), TaffyDimension.length(100.0f));
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
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(0.0f));
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
        s3.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(0.0f));
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

        // div[1]
        Layout l1 = tree.getLayout(n1);
        assertEquals(800.000f, l1.size().width, EPSILON, "width");
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
        assertEquals(0.000f, l3.size().height, EPSILON, "height");
        assertEquals(0.000f, l3.location().x, EPSILON, "x");
        assertEquals(0.000f, l3.location().y, EPSILON, "y");

    }

}
