package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Measure function tests ported from taffy/tests/measure.rs
 * These tests verify that leaf nodes with custom measure functions work correctly.
 */
public class MeasureTest {

    private static final float EPSILON = 0.1f;

    // Fixed size measure function: returns 100x100
    private static MeasureFunc fixedMeasure(float width, float height) {
        return (knownDimensions, availableSpace) -> {
            float w = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : width;
            float h = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : height;
            return new FloatSize(w, h);
        };
    }

    // Aspect ratio measure function: width is fixed, height = width * ratio
    private static MeasureFunc aspectRatioMeasure(float baseWidth, float heightRatio) {
        return (knownDimensions, availableSpace) -> {
            float w = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : baseWidth;
            float h = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : (w * heightRatio);
            return new FloatSize(w, h);
        };
    }

    @Test
    @DisplayName("measure_root")
    void measureRoot() {
        TaffyTree tree = new TaffyTree();
        NodeId node = tree.newLeafWithMeasure(new TaffyStyle(), fixedMeasure(100.0f, 100.0f));
        
        tree.computeLayout(node, TaffySize.maxContent());
        
        Layout layout = tree.getLayout(node);
        assertEquals(100.0f, layout.size().width, EPSILON);
        assertEquals(100.0f, layout.size().height, EPSILON);
    }

    @Test
    @DisplayName("measure_child")
    void measureChild() {
        TaffyTree tree = new TaffyTree();
        
        NodeId child = tree.newLeafWithMeasure(new TaffyStyle(), fixedMeasure(100.0f, 100.0f));
        
        TaffyStyle parentStyle = new TaffyStyle();
        NodeId node = tree.newWithChildren(parentStyle, child);
        
        tree.computeLayout(node, TaffySize.maxContent());
        
        Layout nodeLayout = tree.getLayout(node);
        assertEquals(100.0f, nodeLayout.size().width, EPSILON);
        assertEquals(100.0f, nodeLayout.size().height, EPSILON);
        
        Layout childLayout = tree.getLayout(child);
        assertEquals(100.0f, childLayout.size().width, EPSILON);
        assertEquals(100.0f, childLayout.size().height, EPSILON);
    }

    @Test
    @DisplayName("measure_child_constraint")
    void measureChildConstraint() {
        TaffyTree tree = new TaffyTree();
        
        NodeId child = tree.newLeafWithMeasure(new TaffyStyle(), fixedMeasure(100.0f, 100.0f));
        
        TaffyStyle parentStyle = new TaffyStyle();
        parentStyle.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.AUTO);
        NodeId node = tree.newWithChildren(parentStyle, child);
        
        tree.computeLayout(node, TaffySize.maxContent());
        
        // Parent
        Layout nodeLayout = tree.getLayout(node);
        assertEquals(50.0f, nodeLayout.size().width, EPSILON);
        assertEquals(100.0f, nodeLayout.size().height, EPSILON);
        
        // Child
        Layout childLayout = tree.getLayout(child);
        assertEquals(100.0f, childLayout.size().width, EPSILON);
        assertEquals(100.0f, childLayout.size().height, EPSILON);
    }

    @Test
    @DisplayName("measure_child_constraint_padding_parent")
    void measureChildConstraintPaddingParent() {
        TaffyTree tree = new TaffyTree();
        
        NodeId child = tree.newLeafWithMeasure(new TaffyStyle(), fixedMeasure(100.0f, 100.0f));
        
        TaffyStyle parentStyle = new TaffyStyle();
        parentStyle.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.AUTO);
        parentStyle.padding = new TaffyRect<>(
            LengthPercentage.length(10.0f),
            LengthPercentage.length(10.0f),
            LengthPercentage.length(10.0f),
            LengthPercentage.length(10.0f)
        );
        NodeId node = tree.newWithChildren(parentStyle, child);
        
        tree.computeLayout(node, TaffySize.maxContent());
        
        // Parent
        Layout nodeLayout = tree.getLayout(node);
        assertEquals(0.0f, nodeLayout.location().x, EPSILON);
        assertEquals(0.0f, nodeLayout.location().y, EPSILON);
        assertEquals(50.0f, nodeLayout.size().width, EPSILON);
        assertEquals(120.0f, nodeLayout.size().height, EPSILON);
        
        // Child
        Layout childLayout = tree.getLayout(child);
        assertEquals(10.0f, childLayout.location().x, EPSILON);
        assertEquals(10.0f, childLayout.location().y, EPSILON);
        assertEquals(100.0f, childLayout.size().width, EPSILON);
        assertEquals(100.0f, childLayout.size().height, EPSILON);
    }

    @Test
    @DisplayName("measure_child_with_flex_grow")
    void measureChildWithFlexGrow() {
        TaffyTree tree = new TaffyTree();
        
        TaffyStyle child0Style = new TaffyStyle();
        child0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId child0 = tree.newLeaf(child0Style);
        
        TaffyStyle child1Style = new TaffyStyle();
        child1Style.flexGrow = 1.0f; child1Style.flexShrink = 1.0f; child1Style.flexBasis = TaffyDimension.AUTO;
        NodeId child1 = tree.newLeafWithMeasure(child1Style, fixedMeasure(50.0f, 50.0f));
        
        TaffyStyle parentStyle = new TaffyStyle();
        parentStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        NodeId node = tree.newWithChildren(parentStyle, child0, child1);
        
        tree.computeLayout(node, TaffySize.maxContent());
        
        Layout child1Layout = tree.getLayout(child1);
        assertEquals(50.0f, child1Layout.size().width, EPSILON);
        assertEquals(50.0f, child1Layout.size().height, EPSILON);
    }

    @Test
    @DisplayName("measure_child_with_flex_shrink")
    void measureChildWithFlexShrink() {
        TaffyTree tree = new TaffyTree();
        
        TaffyStyle child0Style = new TaffyStyle();
        child0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        child0Style.flexGrow = 0.0f; child0Style.flexShrink = 0.0f; child0Style.flexBasis = TaffyDimension.AUTO;
        NodeId child0 = tree.newLeaf(child0Style);
        
        TaffyStyle child1Style = new TaffyStyle();
        NodeId child1 = tree.newLeafWithMeasure(child1Style, fixedMeasure(100.0f, 50.0f));
        
        TaffyStyle parentStyle = new TaffyStyle();
        parentStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        NodeId node = tree.newWithChildren(parentStyle, child0, child1);
        
        tree.computeLayout(node, TaffySize.maxContent());
        
        Layout child1Layout = tree.getLayout(child1);
        assertEquals(100.0f, child1Layout.size().width, EPSILON);
        assertEquals(50.0f, child1Layout.size().height, EPSILON);
    }

    @Test
    @DisplayName("remeasure_child_after_growing")
    void remeasureChildAfterGrowing() {
        TaffyTree tree = new TaffyTree();
        
        TaffyStyle child0Style = new TaffyStyle();
        child0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        NodeId child0 = tree.newLeaf(child0Style);
        
        TaffyStyle child1Style = new TaffyStyle();
        child1Style.flexGrow = 1.0f; child1Style.flexShrink = 1.0f; child1Style.flexBasis = TaffyDimension.AUTO;
        // Aspect ratio measure: width=10, height=width*2
        NodeId child1 = tree.newLeafWithMeasure(child1Style, aspectRatioMeasure(10.0f, 2.0f));
        
        TaffyStyle parentStyle = new TaffyStyle();
        parentStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        parentStyle.alignItems = AlignItems.START;
        NodeId node = tree.newWithChildren(parentStyle, child0, child1);
        
        tree.computeLayout(node, TaffySize.maxContent());
        
        Layout child1Layout = tree.getLayout(child1);
        assertEquals(50.0f, child1Layout.size().width, EPSILON);
        assertEquals(100.0f, child1Layout.size().height, EPSILON);
    }

    @Test
    @DisplayName("remeasure_child_after_shrinking")
    void remeasureChildAfterShrinking() {
        TaffyTree tree = new TaffyTree();
        
        TaffyStyle child0Style = new TaffyStyle();
        child0Style.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.length(50.0f));
        child0Style.flexGrow = 0.0f; child0Style.flexShrink = 0.0f; child0Style.flexBasis = TaffyDimension.AUTO;
        NodeId child0 = tree.newLeaf(child0Style);
        
        TaffyStyle child1Style = new TaffyStyle();
        // Aspect ratio measure: width=100, height=width*2
        NodeId child1 = tree.newLeafWithMeasure(child1Style, aspectRatioMeasure(100.0f, 2.0f));
        
        TaffyStyle parentStyle = new TaffyStyle();
        parentStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.AUTO);
        parentStyle.alignItems = AlignItems.START;
        NodeId node = tree.newWithChildren(parentStyle, child0, child1);
        
        tree.computeLayout(node, TaffySize.maxContent());
        
        Layout child1Layout = tree.getLayout(child1);
        assertEquals(100.0f, child1Layout.size().width, EPSILON);
        assertEquals(200.0f, child1Layout.size().height, EPSILON);
    }

    @Test
    @DisplayName("remeasure_child_after_stretching")
    void remeasureChildAfterStretching() {
        TaffyTree tree = new TaffyTree();
        
        // Custom measure: uses known height if provided, else 50; width = height
        MeasureFunc stretchMeasure = (knownDimensions, availableSpace) -> {
            float h = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : 50.0f;
            float w = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : h;
            return new FloatSize(w, h);
        };
        
        TaffyStyle childStyle = new TaffyStyle();
        NodeId child = tree.newLeafWithMeasure(childStyle, stretchMeasure);
        
        TaffyStyle parentStyle = new TaffyStyle();
        parentStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(parentStyle, child);
        
        tree.computeLayout(node, TaffySize.maxContent());
        
        Layout childLayout = tree.getLayout(child);
        assertEquals(100.0f, childLayout.size().width, EPSILON);
        assertEquals(100.0f, childLayout.size().height, EPSILON);
    }

    @Test
    @DisplayName("width_overrides_measure")
    void widthOverridesMeasure() {
        TaffyTree tree = new TaffyTree();
        
        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(50.0f), TaffyDimension.AUTO);
        NodeId child = tree.newLeafWithMeasure(childStyle, fixedMeasure(100.0f, 100.0f));
        
        TaffyStyle parentStyle = new TaffyStyle();
        NodeId node = tree.newWithChildren(parentStyle, child);
        
        tree.computeLayout(node, TaffySize.maxContent());
        
        Layout childLayout = tree.getLayout(child);
        assertEquals(50.0f, childLayout.size().width, EPSILON);
        assertEquals(100.0f, childLayout.size().height, EPSILON);
    }

    @Test
    @DisplayName("height_overrides_measure")
    void heightOverridesMeasure() {
        TaffyTree tree = new TaffyTree();
        
        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(50.0f));
        NodeId child = tree.newLeafWithMeasure(childStyle, fixedMeasure(100.0f, 100.0f));
        
        TaffyStyle parentStyle = new TaffyStyle();
        NodeId node = tree.newWithChildren(parentStyle, child);
        
        tree.computeLayout(node, TaffySize.maxContent());
        
        Layout childLayout = tree.getLayout(child);
        assertEquals(100.0f, childLayout.size().width, EPSILON);
        assertEquals(50.0f, childLayout.size().height, EPSILON);
    }

    @Test
    @DisplayName("flex_basis_overrides_measure")
    void flexBasisOverridesMeasure() {
        TaffyTree tree = new TaffyTree();
        
        TaffyStyle child0Style = new TaffyStyle();
        child0Style.flexGrow = 1.0f; child0Style.flexShrink = 1.0f; child0Style.flexBasis = TaffyDimension.length(50.0f);
        NodeId child0 = tree.newLeaf(child0Style);
        
        TaffyStyle child1Style = new TaffyStyle();
        child1Style.flexGrow = 1.0f; child1Style.flexShrink = 1.0f; child1Style.flexBasis = TaffyDimension.length(50.0f);
        NodeId child1 = tree.newLeafWithMeasure(child1Style, fixedMeasure(100.0f, 100.0f));
        
        TaffyStyle parentStyle = new TaffyStyle();
        parentStyle.size = new TaffySize<>(TaffyDimension.length(200.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(parentStyle, child0, child1);
        
        tree.computeLayout(node, TaffySize.maxContent());
        
        Layout child0Layout = tree.getLayout(child0);
        assertEquals(100.0f, child0Layout.size().width, EPSILON);
        assertEquals(100.0f, child0Layout.size().height, EPSILON);
        
        Layout child1Layout = tree.getLayout(child1);
        assertEquals(100.0f, child1Layout.size().width, EPSILON);
        assertEquals(100.0f, child1Layout.size().height, EPSILON);
    }

    @Test
    @DisplayName("stretch_overrides_measure")
    void stretchOverridesMeasure() {
        TaffyTree tree = new TaffyTree();
        
        TaffyStyle childStyle = new TaffyStyle();
        NodeId child = tree.newLeafWithMeasure(childStyle, fixedMeasure(50.0f, 50.0f));
        
        TaffyStyle parentStyle = new TaffyStyle();
        parentStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(parentStyle, child);
        
        tree.computeLayout(node, TaffySize.maxContent());
        
        Layout childLayout = tree.getLayout(child);
        assertEquals(50.0f, childLayout.size().width, EPSILON);
        assertEquals(100.0f, childLayout.size().height, EPSILON);
    }

    @Test
    @DisplayName("measure_absolute_child")
    void measureAbsoluteChild() {
        TaffyTree tree = new TaffyTree();
        
        TaffyStyle childStyle = new TaffyStyle();
        childStyle.position = TaffyPosition.ABSOLUTE;
        NodeId child = tree.newLeafWithMeasure(childStyle, fixedMeasure(50.0f, 50.0f));
        
        TaffyStyle parentStyle = new TaffyStyle();
        parentStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(parentStyle, child);
        
        tree.computeLayout(node, TaffySize.maxContent());
        
        Layout childLayout = tree.getLayout(child);
        assertEquals(50.0f, childLayout.size().width, EPSILON);
        assertEquals(50.0f, childLayout.size().height, EPSILON);
    }

    @Test
    @DisplayName("ignore_invalid_measure")
    void ignoreInvalidMeasure() {
        TaffyTree tree = new TaffyTree();
        
        // Child without measure function but with flex_grow
        TaffyStyle childStyle = new TaffyStyle();
        childStyle.flexGrow = 1.0f; childStyle.flexShrink = 1.0f; childStyle.flexBasis = TaffyDimension.AUTO;
        NodeId child = tree.newLeaf(childStyle);
        
        TaffyStyle parentStyle = new TaffyStyle();
        parentStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        NodeId node = tree.newWithChildren(parentStyle, child);
        
        tree.computeLayout(node, TaffySize.maxContent());
        
        Layout childLayout = tree.getLayout(child);
        assertEquals(100.0f, childLayout.size().width, EPSILON);
        assertEquals(100.0f, childLayout.size().height, EPSILON);
    }
}
