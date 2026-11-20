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
public class WptSizingAspectRatioTest_8 {

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
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-025.html")
    void grid_aspect_ratio_025() {
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
        s1.display = TaffyDisplay.GRID;
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
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(100.0f));
        s1.gridTemplateRows.add(TrackSizingFunction.fixed(100.0f));
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
        s2.size = new TaffySize<>(TaffyDimension.percent(0.5f), TaffyDimension.length(100.0f));
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
        assertEquals(100.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-026.html")
    void grid_aspect_ratio_026() {
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
        s1.display = TaffyDisplay.GRID;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.minContent());
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(100.0f));
        s1.gridTemplateRows.add(TrackSizingFunction.fixed(100.0f));
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
        s2.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.percent(0.5f));
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
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-027.html")
    void grid_aspect_ratio_027() {
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
        s1.display = TaffyDisplay.GRID;
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
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(0.0f));
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(100.0f));
        s1.gridTemplateRows.add(TrackSizingFunction.fixed(100.0f));
        s1.gridAutoFlow = GridAutoFlow.COLUMN;
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
        s2.aspectRatio = 2.0f;
        s2.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s2.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s2.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s2.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.overflow = new TaffyPoint<>(Overflow.HIDDEN, Overflow.HIDDEN);

        TaffyStyle s3 = new TaffyStyle();
        s3.boxSizing = BoxSizing.CONTENT_BOX;
        s3.direction = TaffyDirection.LTR;
        s3.display = TaffyDisplay.BLOCK;
        s3.flexDirection = FlexDirection.ROW;
        s3.flexWrap = FlexWrap.NO_WRAP;
        s3.flexGrow = 0.0f;
        s3.flexShrink = 1.0f;
        s3.flexBasis = TaffyDimension.AUTO;
        s3.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(100.0f));
        s3.minSize = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        s3.aspectRatio = 0.5f;
        s3.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s3.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s3.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s3.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s3.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s3.gridAutoFlow = GridAutoFlow.ROW;
        s3.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s3.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s3.overflow = new TaffyPoint<>(Overflow.HIDDEN, Overflow.HIDDEN);

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
        assertEquals(200.000f, l2.size().width, EPSILON, "width");
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
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-028.html")
    void grid_aspect_ratio_028() {
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
        s1.display = TaffyDisplay.GRID;
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
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(200.0f));
        s1.gridTemplateRows.add(TrackSizingFunction.fixed(100.0f));
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.alignSelf = AlignItems.STRETCH;
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
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-029.html")
    void grid_aspect_ratio_029() {
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
        s1.display = TaffyDisplay.GRID;
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
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(50.0f));
        s1.gridTemplateRows.add(TrackSizingFunction.fixed(100.0f));
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.alignSelf = AlignItems.STRETCH;
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
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-030.html")
    void grid_aspect_ratio_030() {
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
        s1.display = TaffyDisplay.GRID;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(800.0f), TaffyDimension.length(200.0f));
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(100.0f));
        s1.gridTemplateRows.add(TrackSizingFunction.fixed(200.0f));
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.justifySelf = AlignItems.STRETCH;
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
        assertEquals(200.000f, l1.size().height, EPSILON, "height");
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
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-031.html")
    void grid_aspect_ratio_031() {
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
        s1.display = TaffyDisplay.GRID;
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
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(100.0f));
        s1.gridTemplateRows.add(TrackSizingFunction.fixed(50.0f));
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.justifySelf = AlignItems.STRETCH;
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
        assertEquals(100.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-032.html")
    void grid_aspect_ratio_032() {
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
        s1.display = TaffyDisplay.GRID;
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
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(100.0f));
        s1.gridTemplateRows.add(TrackSizingFunction.fixed(100.0f));
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.alignSelf = AlignItems.STRETCH;
        s2.justifySelf = AlignItems.STRETCH;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.aspectRatio = 2.0f;
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
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-033.html")
    void grid_aspect_ratio_033() {
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
        s1.display = TaffyDisplay.GRID;
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
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(100.0f));
        s1.gridTemplateRows.add(TrackSizingFunction.fixed(100.0f));
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.alignSelf = AlignItems.STRETCH;
        s2.justifySelf = AlignItems.STRETCH;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.aspectRatio = 0.5f;
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
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-034.html")
    void grid_aspect_ratio_034() {
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
        s1.display = TaffyDisplay.GRID;
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
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(100.0f));
        s1.gridTemplateRows.add(TrackSizingFunction.fixed(100.0f));
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.alignSelf = AlignItems.STRETCH;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.percent(1.0f), TaffyDimension.AUTO);
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.aspectRatio = 2.0f;
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
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-035.html")
    void grid_aspect_ratio_035() {
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
        s1.display = TaffyDisplay.GRID;
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
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(100.0f));
        s1.gridTemplateRows.add(TrackSizingFunction.fixed(100.0f));
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.alignSelf = AlignItems.STRETCH;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.percent(1.0f), TaffyDimension.AUTO);
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.aspectRatio = 0.5f;
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
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-036.html")
    void grid_aspect_ratio_036() {
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
        s1.display = TaffyDisplay.GRID;
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
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(100.0f));
        s1.gridTemplateRows.add(TrackSizingFunction.fixed(100.0f));
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.justifySelf = AlignItems.STRETCH;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.percent(1.0f));
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.aspectRatio = 2.0f;
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
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-037.html")
    void grid_aspect_ratio_037() {
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
        s1.display = TaffyDisplay.GRID;
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
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(100.0f));
        s1.gridTemplateRows.add(TrackSizingFunction.fixed(100.0f));
        s1.gridAutoFlow = GridAutoFlow.ROW;
        s1.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s1.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s2 = new TaffyStyle();
        s2.boxSizing = BoxSizing.CONTENT_BOX;
        s2.direction = TaffyDirection.LTR;
        s2.display = TaffyDisplay.BLOCK;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.justifySelf = AlignItems.STRETCH;
        s2.flexGrow = 0.0f;
        s2.flexShrink = 1.0f;
        s2.flexBasis = TaffyDimension.AUTO;
        s2.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.percent(1.0f));
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s2.aspectRatio = 0.5f;
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
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-038.html")
    void grid_aspect_ratio_038() {
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
        s1.display = TaffyDisplay.GRID;
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
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(100.0f));
        s1.gridTemplateRows.add(TrackSizingFunction.fixed(50.0f));
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
        assertEquals(100.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-039.html")
    void grid_aspect_ratio_039() {
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
        s1.display = TaffyDisplay.GRID;
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
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(100.0f));
        s1.gridTemplateRows.add(TrackSizingFunction.fixed(100.0f));
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
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
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

        // div#container
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // div#item
        Layout l2 = tree.getLayout(n2);
        assertEquals(100.000f, l2.size().width, EPSILON, "width");
        assertEquals(100.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-040.html")
    void grid_aspect_ratio_040() {
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
        s1.display = TaffyDisplay.GRID;
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
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(100.0f));
        s1.gridTemplateRows.add(TrackSizingFunction.fixed(100.0f));
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

        // div#item
        Layout l2 = tree.getLayout(n2);
        assertEquals(100.000f, l2.size().width, EPSILON, "width");
        assertEquals(100.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-041.html")
    void grid_aspect_ratio_041() {
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
        s1.display = TaffyDisplay.GRID;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.justifyItems = AlignItems.CENTER;
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
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(100.0f));
        s1.gridTemplateRows.add(TrackSizingFunction.fixed(100.0f));
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
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
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
        s3.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.percent(1.0f));
        s3.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
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

        // div[3]
        Layout l3 = tree.getLayout(n3);
        assertEquals(100.000f, l3.size().width, EPSILON, "width");
        assertEquals(100.000f, l3.size().height, EPSILON, "height");
        assertEquals(0.000f, l3.location().x, EPSILON, "x");
        assertEquals(0.000f, l3.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-042.html")
    void grid_aspect_ratio_042() {
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
        s1.display = TaffyDisplay.GRID;
        s1.flexDirection = FlexDirection.ROW;
        s1.flexWrap = FlexWrap.NO_WRAP;
        s1.flexGrow = 0.0f;
        s1.flexShrink = 1.0f;
        s1.flexBasis = TaffyDimension.AUTO;
        s1.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        s1.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s1.maxSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(100.0f));
        s1.aspectRatio = 2.0f;
        s1.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s1.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s1.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s1.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(100.0f));
        s1.gridTemplateRows.add(TrackSizingFunction.fixed(200.0f));
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
        s2.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(200.0f));
        s2.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
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

        // div#container
        Layout l1 = tree.getLayout(n1);
        assertEquals(100.000f, l1.size().width, EPSILON, "width");
        assertEquals(100.000f, l1.size().height, EPSILON, "height");
        assertEquals(0.000f, l1.location().x, EPSILON, "x");
        assertEquals(0.000f, l1.location().y, EPSILON, "y");

        // div#item
        Layout l2 = tree.getLayout(n2);
        assertEquals(100.000f, l2.size().width, EPSILON, "width");
        assertEquals(200.000f, l2.size().height, EPSILON, "height");
        assertEquals(0.000f, l2.location().x, EPSILON, "x");
        assertEquals(0.000f, l2.location().y, EPSILON, "y");

    }

    @Test
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-043.html")
    void grid_aspect_ratio_043() {
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
        s1.display = TaffyDisplay.GRID;
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
        s1.gridTemplateColumns.add(TrackSizingFunction.fixed(100.0f));
        s1.gridTemplateRows.add(TrackSizingFunction.fixed(100.0f));
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
        s2.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.percent(1.0f));
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

        NodeId n0;
        NodeId n1;
        NodeId n2;

        n2 = tree.newLeaf(s2);
        n1 = tree.newWithChildren(s1, n2);
        n0 = tree.newWithChildren(s0, n1);

        tree.computeLayout(n0, TaffySize.maxContent());

        // div#grid
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
    @DisplayName("css/css-sizing/aspect-ratio/grid-aspect-ratio-align-items-center.html")
    void grid_aspect_ratio_align_items_center() {
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
        s2.display = TaffyDisplay.GRID;
        s2.position = TaffyPosition.RELATIVE;
        s2.flexDirection = FlexDirection.ROW;
        s2.flexWrap = FlexWrap.NO_WRAP;
        s2.alignItems = AlignItems.CENTER;
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
        s2.gridTemplateColumns.add(TrackSizingFunction.fixed(100.0f));
        s2.gridTemplateRows.add(TrackSizingFunction.fixed(100.0f));
        s2.gridAutoFlow = GridAutoFlow.ROW;
        s2.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s2.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s3 = new TaffyStyle();
        s3.boxSizing = BoxSizing.CONTENT_BOX;
        s3.direction = TaffyDirection.LTR;
        s3.display = TaffyDisplay.BLOCK;
        s3.position = TaffyPosition.ABSOLUTE;
        s3.flexDirection = FlexDirection.ROW;
        s3.flexWrap = FlexWrap.NO_WRAP;
        s3.alignSelf = AlignItems.FLEX_START;
        s3.flexGrow = 0.0f;
        s3.flexShrink = 1.0f;
        s3.flexBasis = TaffyDimension.AUTO;
        s3.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(25.0f));
        s3.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s3.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s3.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s3.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s3.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s3.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s3.gridAutoFlow = GridAutoFlow.ROW;
        s3.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s3.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s4 = new TaffyStyle();
        s4.boxSizing = BoxSizing.CONTENT_BOX;
        s4.direction = TaffyDirection.LTR;
        s4.display = TaffyDisplay.BLOCK;
        s4.position = TaffyPosition.ABSOLUTE;
        s4.flexDirection = FlexDirection.ROW;
        s4.flexWrap = FlexWrap.NO_WRAP;
        s4.alignSelf = AlignItems.FLEX_START;
        s4.flexGrow = 0.0f;
        s4.flexShrink = 1.0f;
        s4.flexBasis = TaffyDimension.AUTO;
        s4.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(25.0f));
        s4.minSize = new TaffySize<>(TaffyDimension.length(0.0f), TaffyDimension.length(0.0f));
        s4.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s4.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s4.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s4.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s4.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.length(75.0f), LengthPercentageAuto.AUTO);
        s4.gridAutoFlow = GridAutoFlow.ROW;
        s4.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s4.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        TaffyStyle s5 = new TaffyStyle();
        s5.boxSizing = BoxSizing.CONTENT_BOX;
        s5.direction = TaffyDirection.LTR;
        s5.display = TaffyDisplay.BLOCK;
        s5.flexDirection = FlexDirection.ROW;
        s5.flexWrap = FlexWrap.NO_WRAP;
        s5.flexGrow = 0.0f;
        s5.flexShrink = 1.0f;
        s5.flexBasis = TaffyDimension.AUTO;
        s5.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(50.0f));
        s5.minSize = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        s5.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(0f));
        s5.margin = new TaffyRect<>(LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f), LengthPercentageAuto.length(0.0f));
        s5.padding = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s5.border = new TaffyRect<>(LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f), LengthPercentage.length(0.0f));
        s5.inset = new TaffyRect<>(LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO, LengthPercentageAuto.AUTO);
        s5.gridAutoFlow = GridAutoFlow.ROW;
        s5.gridRow = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());
        s5.gridColumn = new TaffyLine<>(GridPlacement.auto(), GridPlacement.auto());

        NodeId n0;
        NodeId n1;
        NodeId n2;
        NodeId n3;
        NodeId n4;
        NodeId n5;

        n3 = tree.newLeaf(s3);
        n4 = tree.newLeaf(s4);
        n5 = tree.newLeaf(s5);
        n2 = tree.newWithChildren(s2, n3, n4, n5);
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
        assertEquals(25.000f, l3.size().height, EPSILON, "height");
        assertEquals(0.000f, l3.location().x, EPSILON, "x");
        assertEquals(0.000f, l3.location().y, EPSILON, "y");

        // div[4]
        Layout l4 = tree.getLayout(n4);
        assertEquals(100.000f, l4.size().width, EPSILON, "width");
        assertEquals(25.000f, l4.size().height, EPSILON, "height");
        assertEquals(0.000f, l4.location().x, EPSILON, "x");
        assertEquals(75.000f, l4.location().y, EPSILON, "y");

        // div[5]
        Layout l5 = tree.getLayout(n5);
        assertEquals(100.000f, l5.size().width, EPSILON, "width");
        assertEquals(50.000f, l5.size().height, EPSILON, "height");
        assertEquals(0.000f, l5.location().x, EPSILON, "x");
        assertEquals(25.000f, l5.location().y, EPSILON, "y");

    }

}
