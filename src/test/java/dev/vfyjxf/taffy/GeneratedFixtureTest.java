package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.Dimension;
import dev.vfyjxf.taffy.style.Display;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Style;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.geometry.Rect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Generated fixture tests based on taffy's test fixtures.
 * These tests verify that taffy-java produces the same layout results
 * as the original Rust implementation.
 */
public class GeneratedFixtureTest {
    
    private static final float EPSILON = 0.01f;
    
    @Test
    @DisplayName("flex_direction_row")
    void flexDirectionRow() {
        TaffyTree tree = new TaffyTree();
        
        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(10f), Dimension.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);
        
        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.length(10f), Dimension.AUTO);
        NodeId node1 = tree.newLeaf(node1Style);
        
        Style node2Style = new Style();
        node2Style.size = new Size<>(Dimension.length(10f), Dimension.AUTO);
        NodeId node2 = tree.newLeaf(node2Style);
        
        Style rootStyle = new Style();
        rootStyle.size = new Size<>(Dimension.length(100f), Dimension.length(100f));
        NodeId root = tree.newWithChildren(rootStyle, node0, node1, node2);
        
        tree.computeLayout(root, Size.maxContent());
        
        // Root
        Layout rootLayout = tree.getLayout(root);
        assertEquals(100f, rootLayout.size().width, EPSILON);
        assertEquals(100f, rootLayout.size().height, EPSILON);
        assertEquals(0f, rootLayout.location().x, EPSILON);
        assertEquals(0f, rootLayout.location().y, EPSILON);
        
        // Node0
        Layout layout0 = tree.getLayout(node0);
        assertEquals(10f, layout0.size().width, EPSILON);
        assertEquals(100f, layout0.size().height, EPSILON);
        assertEquals(0f, layout0.location().x, EPSILON);
        assertEquals(0f, layout0.location().y, EPSILON);
        
        // Node1
        Layout layout1 = tree.getLayout(node1);
        assertEquals(10f, layout1.size().width, EPSILON);
        assertEquals(100f, layout1.size().height, EPSILON);
        assertEquals(10f, layout1.location().x, EPSILON);
        assertEquals(0f, layout1.location().y, EPSILON);
        
        // Node2
        Layout layout2 = tree.getLayout(node2);
        assertEquals(10f, layout2.size().width, EPSILON);
        assertEquals(100f, layout2.size().height, EPSILON);
        assertEquals(20f, layout2.location().x, EPSILON);
        assertEquals(0f, layout2.location().y, EPSILON);
    }
    
    @Test
    @DisplayName("flex_direction_column")
    void flexDirectionColumn() {
        TaffyTree tree = new TaffyTree();
        
        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.length(10f));
        NodeId node0 = tree.newLeaf(node0Style);
        
        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.length(10f));
        NodeId node1 = tree.newLeaf(node1Style);
        
        Style node2Style = new Style();
        node2Style.size = new Size<>(Dimension.AUTO, Dimension.length(10f));
        NodeId node2 = tree.newLeaf(node2Style);
        
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.flexDirection = FlexDirection.COLUMN;
        rootStyle.size = new Size<>(Dimension.length(100f), Dimension.length(100f));
        NodeId root = tree.newWithChildren(rootStyle, node0, node1, node2);
        
        tree.computeLayout(root, Size.maxContent());
        
        // Root
        Layout rootLayout = tree.getLayout(root);
        assertEquals(100f, rootLayout.size().width, EPSILON);
        assertEquals(100f, rootLayout.size().height, EPSILON);
        
        // Node0 - at top
        Layout layout0 = tree.getLayout(node0);
        assertEquals(100f, layout0.size().width, EPSILON);  // Stretches to full width
        assertEquals(10f, layout0.size().height, EPSILON);
        assertEquals(0f, layout0.location().x, EPSILON);
        assertEquals(0f, layout0.location().y, EPSILON);
        
        // Node1 - below node0
        Layout layout1 = tree.getLayout(node1);
        assertEquals(100f, layout1.size().width, EPSILON);
        assertEquals(10f, layout1.size().height, EPSILON);
        assertEquals(0f, layout1.location().x, EPSILON);
        assertEquals(10f, layout1.location().y, EPSILON);
        
        // Node2 - below node1
        Layout layout2 = tree.getLayout(node2);
        assertEquals(100f, layout2.size().width, EPSILON);
        assertEquals(10f, layout2.size().height, EPSILON);
        assertEquals(0f, layout2.location().x, EPSILON);
        assertEquals(20f, layout2.location().y, EPSILON);
    }
    
    @Test
    @DisplayName("flex_grow_child")
    void flexGrowChild() {
        TaffyTree tree = new TaffyTree();
        
        Style node0Style = new Style();
        node0Style.flexGrow = 1.0f;
        node0Style.flexBasis = Dimension.length(0f);
        NodeId node0 = tree.newLeaf(node0Style);
        
        Style rootStyle = new Style();
        rootStyle.size = new Size<>(Dimension.length(100f), Dimension.length(100f));
        NodeId root = tree.newWithChildren(rootStyle, node0);
        
        tree.computeLayout(root, Size.maxContent());
        
        Layout rootLayout = tree.getLayout(root);
        assertEquals(100f, rootLayout.size().width, EPSILON);
        assertEquals(100f, rootLayout.size().height, EPSILON);
        
        Layout layout0 = tree.getLayout(node0);
        assertEquals(100f, layout0.size().width, EPSILON);
        assertEquals(100f, layout0.size().height, EPSILON);
        assertEquals(0f, layout0.location().x, EPSILON);
        assertEquals(0f, layout0.location().y, EPSILON);
    }
    
    @Test
    @DisplayName("flex_grow_flex_basis_percent_min_max")
    void flexGrowFlexBasisPercentMinMax() {
        TaffyTree tree = new TaffyTree();
        
        Style node0Style = new Style();
        node0Style.flexGrow = 1.0f;
        node0Style.flexShrink = 0.0f;
        node0Style.flexBasis = Dimension.length(0f);
        node0Style.size = new Size<>(Dimension.AUTO, Dimension.AUTO);
        node0Style.minSize = new Size<>(Dimension.percent(0.6f), Dimension.AUTO);
        node0Style.maxSize = new Size<>(Dimension.percent(0.6f), Dimension.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);
        
        Style node1Style = new Style();
        node1Style.flexGrow = 2.0f;
        node1Style.flexShrink = 0.0f;
        node1Style.flexBasis = Dimension.percent(0.5f);
        node1Style.size = new Size<>(Dimension.AUTO, Dimension.AUTO);
        node1Style.maxSize = new Size<>(Dimension.percent(0.2f), Dimension.AUTO);
        NodeId node1 = tree.newLeaf(node1Style);
        
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.size = new Size<>(Dimension.length(120f), Dimension.length(50f));
        NodeId root = tree.newWithChildren(rootStyle, node0, node1);
        
        tree.computeLayout(root, Size.maxContent());
        
        Layout rootLayout = tree.getLayout(root);
        assertEquals(120f, rootLayout.size().width, EPSILON);
        assertEquals(50f, rootLayout.size().height, EPSILON);
        
        // node0: min/max are 60%, so 60% of 120 = 72
        Layout layout0 = tree.getLayout(node0);
        assertEquals(72f, layout0.size().width, EPSILON);
        
        // node1: max is 20%, so 20% of 120 = 24
        Layout layout1 = tree.getLayout(node1);
        assertEquals(24f, layout1.size().width, EPSILON);
    }
    
    @Test
    @DisplayName("align_items_center")
    void alignItemsCenter() {
        TaffyTree tree = new TaffyTree();
        
        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(10f), Dimension.length(10f));
        NodeId node0 = tree.newLeaf(node0Style);
        
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.alignItems = AlignItems.CENTER;
        rootStyle.size = new Size<>(Dimension.length(100f), Dimension.length(100f));
        NodeId root = tree.newWithChildren(rootStyle, node0);
        
        tree.computeLayout(root, Size.maxContent());
        
        Layout rootLayout = tree.getLayout(root);
        assertEquals(100f, rootLayout.size().width, EPSILON);
        assertEquals(100f, rootLayout.size().height, EPSILON);
        
        Layout layout0 = tree.getLayout(node0);
        assertEquals(10f, layout0.size().width, EPSILON);
        assertEquals(10f, layout0.size().height, EPSILON);
        assertEquals(0f, layout0.location().x, EPSILON);
        assertEquals(45f, layout0.location().y, EPSILON); // centered: (100-10)/2 = 45
    }
    
    @Test
    @DisplayName("align_items_flex_end")
    void alignItemsFlexEnd() {
        TaffyTree tree = new TaffyTree();
        
        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(10f), Dimension.length(10f));
        NodeId node0 = tree.newLeaf(node0Style);
        
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.alignItems = AlignItems.FLEX_END;
        rootStyle.size = new Size<>(Dimension.length(100f), Dimension.length(100f));
        NodeId root = tree.newWithChildren(rootStyle, node0);
        
        tree.computeLayout(root, Size.maxContent());
        
        Layout layout0 = tree.getLayout(node0);
        assertEquals(10f, layout0.size().width, EPSILON);
        assertEquals(10f, layout0.size().height, EPSILON);
        assertEquals(0f, layout0.location().x, EPSILON);
        assertEquals(90f, layout0.location().y, EPSILON); // flex-end: 100-10 = 90
    }
    
    @Test
    @DisplayName("justify_content_row_center")
    void justifyContentRowCenter() {
        TaffyTree tree = new TaffyTree();
        
        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(10f), Dimension.length(10f));
        NodeId node0 = tree.newLeaf(node0Style);
        
        Style node1Style = new Style();
        node1Style.size = new Size<>(Dimension.length(10f), Dimension.length(10f));
        NodeId node1 = tree.newLeaf(node1Style);
        
        Style node2Style = new Style();
        node2Style.size = new Size<>(Dimension.length(10f), Dimension.length(10f));
        NodeId node2 = tree.newLeaf(node2Style);
        
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.justifyContent = AlignContent.CENTER;
        rootStyle.size = new Size<>(Dimension.length(100f), Dimension.length(100f));
        NodeId root = tree.newWithChildren(rootStyle, node0, node1, node2);
        
        tree.computeLayout(root, Size.maxContent());
        
        // Total content width = 30, container = 100, offset = (100-30)/2 = 35
        Layout layout0 = tree.getLayout(node0);
        assertEquals(35f, layout0.location().x, EPSILON);
        
        Layout layout1 = tree.getLayout(node1);
        assertEquals(45f, layout1.location().x, EPSILON);
        
        Layout layout2 = tree.getLayout(node2);
        assertEquals(55f, layout2.location().x, EPSILON);
    }
    
    @Test
    @DisplayName("justify_content_row_space_between")
    void justifyContentRowSpaceBetween() {
        TaffyTree tree = new TaffyTree();
        
        Style nodeStyle = new Style();
        nodeStyle.size = new Size<>(Dimension.length(10f), Dimension.length(10f));
        
        NodeId node0 = tree.newLeaf(nodeStyle);
        NodeId node1 = tree.newLeaf(nodeStyle);
        NodeId node2 = tree.newLeaf(nodeStyle);
        
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.justifyContent = AlignContent.SPACE_BETWEEN;
        rootStyle.size = new Size<>(Dimension.length(100f), Dimension.length(100f));
        NodeId root = tree.newWithChildren(rootStyle, node0, node1, node2);
        
        tree.computeLayout(root, Size.maxContent());
        
        // Space between: first at 0, last at end, space distributed evenly
        // Total content = 30, space = 70, gaps = 2
        // gap = 70/2 = 35
        Layout layout0 = tree.getLayout(node0);
        assertEquals(0f, layout0.location().x, EPSILON);
        
        Layout layout1 = tree.getLayout(node1);
        assertEquals(45f, layout1.location().x, EPSILON); // 0 + 10 + 35
        
        Layout layout2 = tree.getLayout(node2);
        assertEquals(90f, layout2.location().x, EPSILON); // 100 - 10
    }
    
    @Test
    @DisplayName("wrap_row")
    void wrapRow() {
        TaffyTree tree = new TaffyTree();
        
        Style nodeStyle = new Style();
        nodeStyle.size = new Size<>(Dimension.length(31f), Dimension.length(30f));
        
        NodeId node0 = tree.newLeaf(nodeStyle);
        NodeId node1 = tree.newLeaf(nodeStyle);
        NodeId node2 = tree.newLeaf(nodeStyle);
        NodeId node3 = tree.newLeaf(nodeStyle);
        
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.flexWrap = FlexWrap.WRAP;
        rootStyle.size = new Size<>(Dimension.length(100f), Dimension.AUTO);
        NodeId root = tree.newWithChildren(rootStyle, node0, node1, node2, node3);
        
        tree.computeLayout(root, Size.maxContent());
        
        Layout rootLayout = tree.getLayout(root);
        assertEquals(100f, rootLayout.size().width, EPSILON);
        assertEquals(60f, rootLayout.size().height, EPSILON); // 2 rows of 30px
        
        // First row
        Layout layout0 = tree.getLayout(node0);
        assertEquals(0f, layout0.location().x, EPSILON);
        assertEquals(0f, layout0.location().y, EPSILON);
        
        Layout layout1 = tree.getLayout(node1);
        assertEquals(31f, layout1.location().x, EPSILON);
        assertEquals(0f, layout1.location().y, EPSILON);
        
        Layout layout2 = tree.getLayout(node2);
        assertEquals(62f, layout2.location().x, EPSILON);
        assertEquals(0f, layout2.location().y, EPSILON);
        
        // Second row
        Layout layout3 = tree.getLayout(node3);
        assertEquals(0f, layout3.location().x, EPSILON);
        assertEquals(30f, layout3.location().y, EPSILON);
    }
    
    @Test
    @DisplayName("padding_no_child")
    void paddingNoChild() {
        TaffyTree tree = new TaffyTree();
        
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.padding = Rect.ltrb(
            LengthPercentage.length(10f),
            LengthPercentage.length(10f),
            LengthPercentage.length(10f),
            LengthPercentage.length(10f)
        );
        NodeId root = tree.newLeaf(rootStyle);
        
        tree.computeLayout(root, Size.maxContent());
        
        Layout layout = tree.getLayout(root);
        assertEquals(20f, layout.size().width, EPSILON);  // padding left + right
        assertEquals(20f, layout.size().height, EPSILON); // padding top + bottom
    }
    
    @Test
    @DisplayName("border_no_child")
    void borderNoChild() {
        TaffyTree tree = new TaffyTree();
        
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.border = Rect.ltrb(
            LengthPercentage.length(10f),
            LengthPercentage.length(10f),
            LengthPercentage.length(10f),
            LengthPercentage.length(10f)
        );
        NodeId root = tree.newLeaf(rootStyle);
        
        tree.computeLayout(root, Size.maxContent());
        
        Layout layout = tree.getLayout(root);
        assertEquals(20f, layout.size().width, EPSILON);
        assertEquals(20f, layout.size().height, EPSILON);
    }
    
    @Test
    @DisplayName("display_none")
    void displayNone() {
        TaffyTree tree = new TaffyTree();
        
        Style node0Style = new Style();
        node0Style.flexGrow = 1.0f;
        NodeId node0 = tree.newLeaf(node0Style);
        
        Style node1Style = new Style();
        node1Style.display = Display.NONE;
        node1Style.flexGrow = 1.0f;
        NodeId node1 = tree.newLeaf(node1Style);
        
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.size = new Size<>(Dimension.length(100f), Dimension.length(100f));
        NodeId root = tree.newWithChildren(rootStyle, node0, node1);
        
        tree.computeLayout(root, Size.maxContent());
        
        // Node0 should take up all space since node1 is hidden
        Layout layout0 = tree.getLayout(node0);
        assertEquals(100f, layout0.size().width, EPSILON);
        assertEquals(100f, layout0.size().height, EPSILON);
        
        // Node1 should be zero size
        Layout layout1 = tree.getLayout(node1);
        assertEquals(0f, layout1.size().width, EPSILON);
        assertEquals(0f, layout1.size().height, EPSILON);
    }
    
    @Test
    @DisplayName("gap_column_gap_flexible")
    void gapColumnGapFlexible() {
        TaffyTree tree = new TaffyTree();
        
        Style nodeStyle = new Style();
        nodeStyle.flexGrow = 1.0f;
        nodeStyle.flexShrink = 1.0f;
        nodeStyle.flexBasis = Dimension.percent(0f);  // Rust uses percent(0f32)
        
        NodeId node0 = tree.newLeaf(nodeStyle);
        NodeId node1 = tree.newLeaf(nodeStyle);
        NodeId node2 = tree.newLeaf(nodeStyle);
        
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.gap = new Size<>(LengthPercentage.length(10f), LengthPercentage.length(20f));
        rootStyle.size = new Size<>(Dimension.length(80f), Dimension.length(100f));
        NodeId root = tree.newWithChildren(rootStyle, node0, node1, node2);
        
        tree.computeLayout(root, Size.maxContent());
        
        // Container width=80, gap=10, 3 items => 2 gaps = 20
        // Available for content = 80 - 20 = 60, each child = 60 / 3 = 20
        Layout rootLayout = tree.getLayout(root);
        assertEquals(80f, rootLayout.size().width, EPSILON);
        assertEquals(100f, rootLayout.size().height, EPSILON);
        
        Layout layout0 = tree.getLayout(node0);
        assertEquals(0f, layout0.location().x, EPSILON);
        assertEquals(20f, layout0.size().width, EPSILON);
        assertEquals(100f, layout0.size().height, EPSILON);
        
        Layout layout1 = tree.getLayout(node1);
        assertEquals(30f, layout1.location().x, EPSILON); // 20 + 10 gap
        assertEquals(20f, layout1.size().width, EPSILON);
        
        Layout layout2 = tree.getLayout(node2);
        assertEquals(60f, layout2.location().x, EPSILON); // 30 + 20 + 10 gap
        assertEquals(20f, layout2.size().width, EPSILON);
    }
    
    @Test
    @DisplayName("percentage_width_height")
    void percentageWidthHeight() {
        TaffyTree tree = new TaffyTree();
        
        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.percent(0.3f), Dimension.percent(0.3f));
        NodeId node0 = tree.newLeaf(node0Style);
        
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.size = new Size<>(Dimension.length(200f), Dimension.length(400f));
        NodeId root = tree.newWithChildren(rootStyle, node0);
        
        tree.computeLayout(root, Size.maxContent());
        
        Layout layout0 = tree.getLayout(node0);
        assertEquals(60f, layout0.size().width, EPSILON);   // 30% of 200
        assertEquals(120f, layout0.size().height, EPSILON); // 30% of 400
    }
    
    @Test
    @DisplayName("margin_and_flex_row")
    void marginAndFlexRow() {
        TaffyTree tree = new TaffyTree();
        
        Style node0Style = new Style();
        node0Style.flexGrow = 1.0f;
        node0Style.margin = Rect.ltrb(
            LengthPercentageAuto.length(10f),
            LengthPercentageAuto.length(10f),
            LengthPercentageAuto.length(10f),
            LengthPercentageAuto.length(10f)
        );
        NodeId node0 = tree.newLeaf(node0Style);
        
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.size = new Size<>(Dimension.length(100f), Dimension.length(100f));
        NodeId root = tree.newWithChildren(rootStyle, node0);
        
        tree.computeLayout(root, Size.maxContent());
        
        Layout layout0 = tree.getLayout(node0);
        assertEquals(80f, layout0.size().width, EPSILON);  // 100 - 10 - 10
        assertEquals(80f, layout0.size().height, EPSILON); // 100 - 10 - 10
        assertEquals(10f, layout0.location().x, EPSILON);
        assertEquals(10f, layout0.location().y, EPSILON);
    }
    
    @Test
    @DisplayName("min_width")
    void minWidth() {
        TaffyTree tree = new TaffyTree();
        
        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(10f), Dimension.AUTO);
        node0Style.minSize = new Size<>(Dimension.length(60f), Dimension.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);
        
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.size = new Size<>(Dimension.length(100f), Dimension.length(100f));
        NodeId root = tree.newWithChildren(rootStyle, node0);
        
        tree.computeLayout(root, Size.maxContent());
        
        Layout layout0 = tree.getLayout(node0);
        assertEquals(60f, layout0.size().width, EPSILON); // min-width enforced
    }
    
    @Test
    @DisplayName("max_width")
    void maxWidth() {
        TaffyTree tree = new TaffyTree();
        
        Style node0Style = new Style();
        node0Style.size = new Size<>(Dimension.length(200f), Dimension.AUTO);
        node0Style.maxSize = new Size<>(Dimension.length(60f), Dimension.AUTO);
        NodeId node0 = tree.newLeaf(node0Style);
        
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.size = new Size<>(Dimension.length(100f), Dimension.length(100f));
        NodeId root = tree.newWithChildren(rootStyle, node0);
        
        tree.computeLayout(root, Size.maxContent());
        
        Layout layout0 = tree.getLayout(node0);
        assertEquals(60f, layout0.size().width, EPSILON); // max-width enforced
    }
}
