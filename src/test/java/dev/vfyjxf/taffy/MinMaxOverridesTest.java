package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.Dimension;
import dev.vfyjxf.taffy.style.Style;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Min/Max overrides tests ported from taffy/tests/min_max_overrides.rs
 * These tests verify that min/max size constraints interact correctly with regular size.
 */
public class MinMaxOverridesTest {

    private static final float EPSILON = 0.1f;

    @Test
    @DisplayName("min_overrides_max")
    void minOverridesMax() {
        TaffyTree tree = new TaffyTree();
        
        Style childStyle = new Style();
        childStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        childStyle.minSize = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        childStyle.maxSize = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        NodeId child = tree.newLeaf(childStyle);
        
        tree.computeLayout(child, new Size<>(
            AvailableSpace.definite(100.0f),
            AvailableSpace.definite(100.0f)
        ));
        
        Layout layout = tree.getLayout(child);
        // min_size should override max_size when min > max
        assertEquals(100.0f, layout.size().width, EPSILON);
        assertEquals(100.0f, layout.size().height, EPSILON);
    }

    @Test
    @DisplayName("max_overrides_size")
    void maxOverridesSize() {
        TaffyTree tree = new TaffyTree();
        
        Style childStyle = new Style();
        childStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        childStyle.maxSize = new Size<>(Dimension.length(10.0f), Dimension.length(10.0f));
        NodeId child = tree.newLeaf(childStyle);
        
        tree.computeLayout(child, new Size<>(
            AvailableSpace.definite(100.0f),
            AvailableSpace.definite(100.0f)
        ));
        
        Layout layout = tree.getLayout(child);
        // max_size should clamp the final size
        assertEquals(10.0f, layout.size().width, EPSILON);
        assertEquals(10.0f, layout.size().height, EPSILON);
    }

    @Test
    @DisplayName("min_overrides_size")
    void minOverridesSize() {
        TaffyTree tree = new TaffyTree();
        
        Style childStyle = new Style();
        childStyle.size = new Size<>(Dimension.length(50.0f), Dimension.length(50.0f));
        childStyle.minSize = new Size<>(Dimension.length(100.0f), Dimension.length(100.0f));
        NodeId child = tree.newLeaf(childStyle);
        
        tree.computeLayout(child, new Size<>(
            AvailableSpace.definite(100.0f),
            AvailableSpace.definite(100.0f)
        ));
        
        Layout layout = tree.getLayout(child);
        // min_size should expand the final size
        assertEquals(100.0f, layout.size().width, EPSILON);
        assertEquals(100.0f, layout.size().height, EPSILON);
    }
}
