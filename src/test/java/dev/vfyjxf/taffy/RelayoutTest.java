package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Relayout stability tests ported from taffy/tests/relayout.rs
 * These tests verify that repeated layout computations produce stable, consistent results.
 */
public class RelayoutTest {

    private static final float EPSILON = 0.1f;

    @Test
    @DisplayName("relayout")
    void relayout() {
        TaffyTree tree = new TaffyTree();
        
        TaffyStyle node1Style = new TaffyStyle();
        node1Style.size = new TaffySize<>(TaffyDimension.length(8.0f), TaffyDimension.length(80.0f));
        NodeId node1 = tree.newLeaf(node1Style);
        
        TaffyStyle node0Style = new TaffyStyle();
        node0Style.alignSelf = AlignItems.CENTER;
        node0Style.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.AUTO);
        NodeId node0 = tree.newWithChildren(node0Style, node1);
        
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.size = new TaffySize<>(TaffyDimension.percent(1.0f), TaffyDimension.percent(1.0f));
        NodeId root = tree.newWithChildren(rootStyle, node0);
        
        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(100.0f),
            AvailableSpace.definite(100.0f)
        ));
        
        float initialX = tree.getLayout(root).location().x;
        float initialY = tree.getLayout(root).location().y;
        float initial0X = tree.getLayout(node0).location().x;
        float initial0Y = tree.getLayout(node0).location().y;
        float initial1X = tree.getLayout(node1).location().x;
        float initial1Y = tree.getLayout(node1).location().y;
        
        // Re-compute layout multiple times and verify stability
        for (int i = 1; i < 10; i++) {
            tree.computeLayout(root, new TaffySize<>(
                AvailableSpace.definite(100.0f),
                AvailableSpace.definite(100.0f)
            ));
            
            assertEquals(initialX, tree.getLayout(root).location().x, EPSILON, "root x after iteration " + i);
            assertEquals(initialY, tree.getLayout(root).location().y, EPSILON, "root y after iteration " + i);
            assertEquals(initial0X, tree.getLayout(node0).location().x, EPSILON, "node0 x after iteration " + i);
            assertEquals(initial0Y, tree.getLayout(node0).location().y, EPSILON, "node0 y after iteration " + i);
            assertEquals(initial1X, tree.getLayout(node1).location().x, EPSILON, "node1 x after iteration " + i);
            assertEquals(initial1Y, tree.getLayout(node1).location().y, EPSILON, "node1 y after iteration " + i);
        }
    }

    @Test
    @DisplayName("toggle_root_display_none")
    void toggleRootDisplayNone() {
        TaffyTree tree = new TaffyTree();
        
        TaffyStyle hiddenStyle = new TaffyStyle();
        hiddenStyle.display = TaffyDisplay.NONE;
        hiddenStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        
        TaffyStyle flexStyle = new TaffyStyle();
        flexStyle.display = TaffyDisplay.FLEX;
        flexStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        
        // Setup
        NodeId node = tree.newLeaf(hiddenStyle);
        
        // Layout 1 (None)
        tree.computeLayout(node, TaffySize.maxContent());
        Layout layout = tree.getLayout(node);
        assertEquals(0.0f, layout.location().x, EPSILON);
        assertEquals(0.0f, layout.location().y, EPSILON);
        assertEquals(0.0f, layout.size().width, EPSILON);
        assertEquals(0.0f, layout.size().height, EPSILON);
        
        // Layout 2 (Flex)
        tree.setStyle(node, flexStyle);
        tree.computeLayout(node, TaffySize.maxContent());
        layout = tree.getLayout(node);
        assertEquals(0.0f, layout.location().x, EPSILON);
        assertEquals(0.0f, layout.location().y, EPSILON);
        assertEquals(100.0f, layout.size().width, EPSILON);
        assertEquals(100.0f, layout.size().height, EPSILON);
        
        // Layout 3 (None)
        tree.setStyle(node, hiddenStyle);
        tree.computeLayout(node, TaffySize.maxContent());
        layout = tree.getLayout(node);
        assertEquals(0.0f, layout.location().x, EPSILON);
        assertEquals(0.0f, layout.location().y, EPSILON);
        assertEquals(0.0f, layout.size().width, EPSILON);
        assertEquals(0.0f, layout.size().height, EPSILON);
    }

    @Test
    @DisplayName("toggle_root_display_none_with_children")
    void toggleRootDisplayNoneWithChildren() {
        TaffyTree tree = new TaffyTree();
        
        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(800.0f), TaffyDimension.length(100.0f));
        NodeId child = tree.newLeaf(childStyle);
        
        TaffyStyle parentStyle = new TaffyStyle();
        parentStyle.size = new TaffySize<>(TaffyDimension.length(800.0f), TaffyDimension.length(100.0f));
        NodeId parent = tree.newWithChildren(parentStyle, child);
        
        TaffyStyle rootStyle = new TaffyStyle();
        NodeId root = tree.newWithChildren(rootStyle, parent);
        
        tree.computeLayout(root, TaffySize.maxContent());
        assertEquals(800.0f, tree.getLayout(child).size().width, EPSILON);
        assertEquals(100.0f, tree.getLayout(child).size().height, EPSILON);
        
        TaffyStyle hiddenStyle = new TaffyStyle();
        hiddenStyle.display = TaffyDisplay.NONE;
        tree.setStyle(root, hiddenStyle);
        tree.computeLayout(root, TaffySize.maxContent());
        assertEquals(0.0f, tree.getLayout(child).size().width, EPSILON);
        assertEquals(0.0f, tree.getLayout(child).size().height, EPSILON);
        
        tree.setStyle(root, new TaffyStyle());
        tree.computeLayout(root, TaffySize.maxContent());
        assertEquals(800.0f, tree.getLayout(parent).size().width, EPSILON);
        assertEquals(100.0f, tree.getLayout(parent).size().height, EPSILON);
        assertEquals(800.0f, tree.getLayout(child).size().width, EPSILON);
        assertEquals(100.0f, tree.getLayout(child).size().height, EPSILON);
    }

    @Test
    @DisplayName("toggle_flex_child_display_none")
    void toggleFlexChildDisplayNone() {
        TaffyTree tree = new TaffyTree();
        
        TaffyStyle hiddenStyle = new TaffyStyle();
        hiddenStyle.display = TaffyDisplay.NONE;
        hiddenStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        
        TaffyStyle flexStyle = new TaffyStyle();
        flexStyle.display = TaffyDisplay.FLEX;
        flexStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        
        // Setup
        NodeId node = tree.newLeaf(hiddenStyle);
        NodeId root = tree.newWithChildren(flexStyle, node);
        
        System.out.println("=== Layout 1 (None) ===");
        System.out.println("node style display: " + tree.getStyle(node).display);
        // Layout 1 (None)
        tree.computeLayout(root, TaffySize.maxContent());
        Layout layout = tree.getLayout(node);
        System.out.println("node layout: " + layout.size());
        assertEquals(0.0f, layout.location().x, EPSILON);
        assertEquals(0.0f, layout.location().y, EPSILON);
        assertEquals(0.0f, layout.size().width, EPSILON);
        assertEquals(0.0f, layout.size().height, EPSILON);
        
        System.out.println("=== Layout 2 (Flex) ===");
        System.out.println("Before setStyle - isDirty(node): " + tree.isDirty(node));
        System.out.println("Before setStyle - isDirty(root): " + tree.isDirty(root));
        // Layout 2 (Flex)
        tree.setStyle(node, flexStyle);
        System.out.println("After setStyle - node style display: " + tree.getStyle(node).display);
        System.out.println("After setStyle - isDirty(node): " + tree.isDirty(node));
        System.out.println("After setStyle - isDirty(root): " + tree.isDirty(root));
        tree.computeLayout(root, TaffySize.maxContent());
        layout = tree.getLayout(node);
        System.out.println("node layout: " + layout.size());
        assertEquals(0.0f, layout.location().x, EPSILON);
        assertEquals(0.0f, layout.location().y, EPSILON);
        assertEquals(100.0f, layout.size().width, EPSILON);
        assertEquals(100.0f, layout.size().height, EPSILON);
        
        // Layout 3 (None)
        tree.setStyle(node, hiddenStyle);
        tree.computeLayout(root, TaffySize.maxContent());
        layout = tree.getLayout(node);
        assertEquals(0.0f, layout.location().x, EPSILON);
        assertEquals(0.0f, layout.location().y, EPSILON);
        assertEquals(0.0f, layout.size().width, EPSILON);
        assertEquals(0.0f, layout.size().height, EPSILON);
    }

    @Test
    @DisplayName("toggle_flex_container_display_none")
    void toggleFlexContainerDisplayNone() {
        TaffyTree tree = new TaffyTree();
        
        TaffyStyle hiddenStyle = new TaffyStyle();
        hiddenStyle.display = TaffyDisplay.NONE;
        hiddenStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        
        TaffyStyle flexStyle = new TaffyStyle();
        flexStyle.display = TaffyDisplay.FLEX;
        flexStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        
        // Setup
        NodeId node = tree.newLeaf(hiddenStyle);
        NodeId root = tree.newWithChildren(hiddenStyle, node);
        
        // Layout 1 (None)
        tree.computeLayout(root, TaffySize.maxContent());
        Layout layout = tree.getLayout(root);
        assertEquals(0.0f, layout.location().x, EPSILON);
        assertEquals(0.0f, layout.location().y, EPSILON);
        assertEquals(0.0f, layout.size().width, EPSILON);
        assertEquals(0.0f, layout.size().height, EPSILON);
        
        // Layout 2 (Flex)
        tree.setStyle(root, flexStyle);
        tree.computeLayout(root, TaffySize.maxContent());
        layout = tree.getLayout(root);
        assertEquals(0.0f, layout.location().x, EPSILON);
        assertEquals(0.0f, layout.location().y, EPSILON);
        assertEquals(100.0f, layout.size().width, EPSILON);
        assertEquals(100.0f, layout.size().height, EPSILON);
        
        // Layout 3 (None)
        tree.setStyle(root, hiddenStyle);
        tree.computeLayout(root, TaffySize.maxContent());
        layout = tree.getLayout(root);
        assertEquals(0.0f, layout.location().x, EPSILON);
        assertEquals(0.0f, layout.location().y, EPSILON);
        assertEquals(0.0f, layout.size().width, EPSILON);
        assertEquals(0.0f, layout.size().height, EPSILON);
    }

    @Test
    @DisplayName("toggle_grid_child_display_none")
    void toggleGridChildDisplayNone() {
        TaffyTree tree = new TaffyTree();
        
        TaffyStyle hiddenStyle = new TaffyStyle();
        hiddenStyle.display = TaffyDisplay.NONE;
        hiddenStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        
        TaffyStyle gridStyle = new TaffyStyle();
        gridStyle.display = TaffyDisplay.GRID;
        gridStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        
        // Setup
        NodeId node = tree.newLeaf(hiddenStyle);
        NodeId root = tree.newWithChildren(gridStyle, node);
        
        // Layout 1 (None)
        tree.computeLayout(root, TaffySize.maxContent());
        Layout layout = tree.getLayout(node);
        assertEquals(0.0f, layout.location().x, EPSILON);
        assertEquals(0.0f, layout.location().y, EPSILON);
        assertEquals(0.0f, layout.size().width, EPSILON);
        assertEquals(0.0f, layout.size().height, EPSILON);
        
        // Layout 2 (Grid)
        tree.setStyle(node, gridStyle);
        tree.computeLayout(root, TaffySize.maxContent());
        layout = tree.getLayout(node);
        assertEquals(0.0f, layout.location().x, EPSILON);
        assertEquals(0.0f, layout.location().y, EPSILON);
        assertEquals(100.0f, layout.size().width, EPSILON);
        assertEquals(100.0f, layout.size().height, EPSILON);
        
        // Layout 3 (None)
        tree.setStyle(node, hiddenStyle);
        tree.computeLayout(root, TaffySize.maxContent());
        layout = tree.getLayout(node);
        assertEquals(0.0f, layout.location().x, EPSILON);
        assertEquals(0.0f, layout.location().y, EPSILON);
        assertEquals(0.0f, layout.size().width, EPSILON);
        assertEquals(0.0f, layout.size().height, EPSILON);
    }

    @Test
    @DisplayName("toggle_grid_container_display_none")
    void toggleGridContainerDisplayNone() {
        TaffyTree tree = new TaffyTree();
        
        TaffyStyle hiddenStyle = new TaffyStyle();
        hiddenStyle.display = TaffyDisplay.NONE;
        hiddenStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        
        TaffyStyle gridStyle = new TaffyStyle();
        gridStyle.display = TaffyDisplay.GRID;
        gridStyle.size = new TaffySize<>(TaffyDimension.length(100.0f), TaffyDimension.length(100.0f));
        
        // Setup
        NodeId node = tree.newLeaf(hiddenStyle);
        NodeId root = tree.newWithChildren(hiddenStyle, node);
        
        // Layout 1 (None)
        tree.computeLayout(root, TaffySize.maxContent());
        Layout layout = tree.getLayout(root);
        assertEquals(0.0f, layout.location().x, EPSILON);
        assertEquals(0.0f, layout.location().y, EPSILON);
        assertEquals(0.0f, layout.size().width, EPSILON);
        assertEquals(0.0f, layout.size().height, EPSILON);
        
        // Layout 2 (Grid)
        tree.setStyle(root, gridStyle);
        tree.computeLayout(root, TaffySize.maxContent());
        layout = tree.getLayout(root);
        assertEquals(0.0f, layout.location().x, EPSILON);
        assertEquals(0.0f, layout.location().y, EPSILON);
        assertEquals(100.0f, layout.size().width, EPSILON);
        assertEquals(100.0f, layout.size().height, EPSILON);
        
        // Layout 3 (None)
        tree.setStyle(root, hiddenStyle);
        tree.computeLayout(root, TaffySize.maxContent());
        layout = tree.getLayout(root);
        assertEquals(0.0f, layout.location().x, EPSILON);
        assertEquals(0.0f, layout.location().y, EPSILON);
        assertEquals(0.0f, layout.size().width, EPSILON);
        assertEquals(0.0f, layout.size().height, EPSILON);
    }

    @Test
    @DisplayName("relayout_is_stable_with_rounding")
    // @Disabled("Rounding edge case: expected 301 but got 300 - needs investigation of RoundLayout algorithm")
    void relayoutIsStableWithRounding() {
        TaffyTree tree = new TaffyTree();
        tree.enableRounding();
        
        // <div style="width: 1920px; height: 1080px">
        //     <div style="width: 100%; left: 1.5px">
        //         <div style="width: 150px; justify-content: end">
        //             <div style="min-width: 300px" />
        //         </div>
        //     </div>
        // </div>
        
        TaffyStyle innerStyle = new TaffyStyle();
        innerStyle.minSize = new TaffySize<>(TaffyDimension.length(300.0f), TaffyDimension.AUTO);
        NodeId inner = tree.newLeaf(innerStyle);
        
        TaffyStyle wrapperStyle = new TaffyStyle();
        wrapperStyle.size = new TaffySize<>(TaffyDimension.length(150.0f), TaffyDimension.AUTO);
        wrapperStyle.justifyContent = AlignContent.END;
        NodeId wrapper = tree.newWithChildren(wrapperStyle, inner);
        
        TaffyStyle outerStyle = new TaffyStyle();
        outerStyle.size = new TaffySize<>(TaffyDimension.percent(1.0f), TaffyDimension.AUTO);
        outerStyle.inset = new TaffyRect<>(
            LengthPercentageAuto.length(1.5f),
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.AUTO,
            LengthPercentageAuto.AUTO
        );
        NodeId outer = tree.newWithChildren(outerStyle, wrapper);
        
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(1920.0f), TaffyDimension.length(1080.0f));
        NodeId root = tree.newWithChildren(rootStyle, outer);
        
        for (int i = 0; i < 5; i++) {
            tree.markDirty(root);
            tree.computeLayout(root, TaffySize.maxContent());
            
            Layout rootLayout = tree.getLayout(root);
            assertEquals(0.0f, rootLayout.location().x, EPSILON);
            assertEquals(0.0f, rootLayout.location().y, EPSILON);
            assertEquals(1920.0f, rootLayout.size().width, EPSILON);
            assertEquals(1080.0f, rootLayout.size().height, EPSILON);
            
            Layout outerLayout = tree.getLayout(outer);
            assertEquals(2.0f, outerLayout.location().x, EPSILON);
            assertEquals(0.0f, outerLayout.location().y, EPSILON);
            assertEquals(1920.0f, outerLayout.size().width, EPSILON);
            assertEquals(1080.0f, outerLayout.size().height, EPSILON);
            
            Layout wrapperLayout = tree.getLayout(wrapper);
            assertEquals(0.0f, wrapperLayout.location().x, EPSILON);
            assertEquals(0.0f, wrapperLayout.location().y, EPSILON);
            assertEquals(150.0f, wrapperLayout.size().width, EPSILON);
            assertEquals(1080.0f, wrapperLayout.size().height, EPSILON);
            
            Layout innerLayout = tree.getLayout(inner);
            assertEquals(-150.0f, innerLayout.location().x, EPSILON);
            assertEquals(0.0f, innerLayout.location().y, EPSILON);
            assertEquals(301.0f, innerLayout.size().width, EPSILON);
            assertEquals(1080.0f, innerLayout.size().height, EPSILON);
        }
    }
}
