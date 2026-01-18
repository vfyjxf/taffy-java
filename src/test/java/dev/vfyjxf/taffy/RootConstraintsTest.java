package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.Rect;
import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.Dimension;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.Style;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Root constraints tests ported from taffy/tests/root_constraints.rs
 * These tests verify that root node constraints are applied correctly.
 */
public class RootConstraintsTest {

    private static final float EPSILON = 0.1f;

    @Test
    @DisplayName("root_with_percentage_size")
    void rootWithPercentageSize() {
        TaffyTree tree = new TaffyTree();
        
        Style style = new Style();
        style.size = new Size<>(Dimension.percent(1.0f), Dimension.percent(1.0f));
        NodeId node = tree.newLeaf(style);
        
        tree.computeLayout(node, new Size<>(
            AvailableSpace.definite(100.0f),
            AvailableSpace.definite(200.0f)
        ));
        
        Layout layout = tree.getLayout(node);
        assertEquals(100.0f, layout.size().width, EPSILON);
        assertEquals(200.0f, layout.size().height, EPSILON);
    }

    @Test
    @DisplayName("root_with_no_size")
    void rootWithNoSize() {
        TaffyTree tree = new TaffyTree();
        
        Style style = new Style();
        NodeId node = tree.newLeaf(style);
        
        tree.computeLayout(node, new Size<>(
            AvailableSpace.definite(100.0f),
            AvailableSpace.definite(100.0f)
        ));
        
        Layout layout = tree.getLayout(node);
        assertEquals(0.0f, layout.size().width, EPSILON);
        assertEquals(0.0f, layout.size().height, EPSILON);
    }

    @Test
    @DisplayName("root_with_larger_size")
    void rootWithLargerSize() {
        TaffyTree tree = new TaffyTree();
        
        Style style = new Style();
        style.size = new Size<>(Dimension.length(200.0f), Dimension.length(200.0f));
        NodeId node = tree.newLeaf(style);
        
        tree.computeLayout(node, new Size<>(
            AvailableSpace.definite(100.0f),
            AvailableSpace.definite(100.0f)
        ));
        
        Layout layout = tree.getLayout(node);
        assertEquals(200.0f, layout.size().width, EPSILON);
        assertEquals(200.0f, layout.size().height, EPSILON);
    }

    @Test
    @DisplayName("root_padding_and_border_larger_than_definite_size")
    void rootPaddingAndBorderLargerThanDefiniteSize() {
        TaffyTree tree = new TaffyTree();
        
        Style childStyle = new Style();
        NodeId child = tree.newLeaf(childStyle);
        
        Style rootStyle = new Style();
        rootStyle.size = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        rootStyle.padding = new Rect<>(
            LengthPercentage.length(10.0f),
            LengthPercentage.length(10.0f),
            LengthPercentage.length(10.0f),
            LengthPercentage.length(10.0f)
        );
        rootStyle.border = new Rect<>(
            LengthPercentage.length(10.0f),
            LengthPercentage.length(10.0f),
            LengthPercentage.length(10.0f),
            LengthPercentage.length(10.0f)
        );
        NodeId root = tree.newWithChildren(rootStyle, child);
        
        tree.computeLayout(root, Size.maxContent());
        
        Layout layout = tree.getLayout(root);
        assertEquals(40.0f, layout.size().width, EPSILON);
        assertEquals(40.0f, layout.size().height, EPSILON);
    }
}
