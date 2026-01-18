package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.Dimension;
import dev.vfyjxf.taffy.style.Style;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rounding tests ported from taffy/tests/rounding.rs
 * These tests verify that layout rounding doesn't create visual gaps.
 */
public class RoundingTest {

    private static final float EPSILON = 0.001f;

    @Test
    @DisplayName("rounding_doesnt_leave_gaps")
    void roundingDoesntLeaveGaps() {
        TaffyTree tree = new TaffyTree();
        
        Size<Dimension> squareSize = new Size<>(Dimension.length(100.3f), Dimension.length(100.3f));
        
        Style childAStyle = new Style();
        childAStyle.size = squareSize;
        NodeId childA = tree.newLeaf(childAStyle);
        
        Style childBStyle = new Style();
        childBStyle.size = squareSize;
        NodeId childB = tree.newLeaf(childBStyle);
        
        Style rootStyle = new Style();
        rootStyle.size = new Size<>(Dimension.length(963.3333f), Dimension.length(1000.0f));
        rootStyle.justifyContent = AlignContent.CENTER;
        NodeId root = tree.newWithChildren(rootStyle, childA, childB);
        
        tree.computeLayout(root, Size.maxContent());
        
        Layout layoutA = tree.getLayout(childA);
        Layout layoutB = tree.getLayout(childB);
        
        // Child A's right edge should meet child B's left edge exactly (no gap)
        float childARight = layoutA.location().x + layoutA.size().width;
        float childBLeft = layoutB.location().x;
        
        assertEquals(childARight, childBLeft, EPSILON, 
            "Gap detected between siblings: childA right edge (" + childARight + 
            ") should equal childB left edge (" + childBLeft + ")");
    }
}
