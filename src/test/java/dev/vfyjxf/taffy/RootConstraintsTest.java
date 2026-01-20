package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyStyle;
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
        
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.percent(1.0f), TaffyDimension.percent(1.0f));
        NodeId node = tree.newLeaf(style);
        
        tree.computeLayout(node, new TaffySize<>(
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
        
        TaffyStyle style = new TaffyStyle();
        NodeId node = tree.newLeaf(style);
        
        tree.computeLayout(node, new TaffySize<>(
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
        
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(200.0f), TaffyDimension.length(200.0f));
        NodeId node = tree.newLeaf(style);
        
        tree.computeLayout(node, new TaffySize<>(
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
        
        TaffyStyle childStyle = new TaffyStyle();
        NodeId child = tree.newLeaf(childStyle);
        
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(10.0f), TaffyDimension.length(10.0f));
        rootStyle.padding = new TaffyRect<>(
            LengthPercentage.length(10.0f),
            LengthPercentage.length(10.0f),
            LengthPercentage.length(10.0f),
            LengthPercentage.length(10.0f)
        );
        rootStyle.border = new TaffyRect<>(
            LengthPercentage.length(10.0f),
            LengthPercentage.length(10.0f),
            LengthPercentage.length(10.0f),
            LengthPercentage.length(10.0f)
        );
        NodeId root = tree.newWithChildren(rootStyle, child);
        
        tree.computeLayout(root, TaffySize.maxContent());
        
        Layout layout = tree.getLayout(root);
        assertEquals(40.0f, layout.size().width, EPSILON);
        assertEquals(40.0f, layout.size().height, EPSILON);
    }
}
