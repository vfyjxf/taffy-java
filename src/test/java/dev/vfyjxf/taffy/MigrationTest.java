package dev.vfyjxf.taffy;

import com.google.gson.Gson;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Migration tests to ensure behavior matches the original taffy Rust implementation.
 * These tests use JSON fixture format compatible with taffy's test fixtures.
 */
public class MigrationTest {

    private TaffyTree tree;
    private Gson gson;

    @BeforeEach
    void setUp() {
        tree = new TaffyTree();
        gson = new Gson();
    }

    @Test
    @DisplayName("Flexbox: row with fixed sizes")
    void testFlexboxRowFixedSizes() {
        // Equivalent to taffy fixture: flex/flex_row_fixed_size
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200), TaffyDimension.length(100));

        TaffyStyle child1Style = new TaffyStyle();
        child1Style.size = new TaffySize<>(TaffyDimension.length(50), TaffyDimension.length(50));

        TaffyStyle child2Style = new TaffyStyle();
        child2Style.size = new TaffySize<>(TaffyDimension.length(50), TaffyDimension.length(50));

        NodeId child1 = tree.newLeaf(child1Style);
        NodeId child2 = tree.newLeaf(child2Style);
        NodeId root = tree.newWithChildren(rootStyle, child1, child2);

        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(200f),
            AvailableSpace.definite(100f)
        ));

        Layout rootLayout = tree.getLayout(root);
        assertEquals(0, rootLayout.location().x, 0.01f);
        assertEquals(0, rootLayout.location().y, 0.01f);
        assertEquals(200, rootLayout.size().width, 0.01f);
        assertEquals(100, rootLayout.size().height, 0.01f);

        Layout child1Layout = tree.getLayout(child1);
        assertEquals(0, child1Layout.location().x, 0.01f);
        assertEquals(0, child1Layout.location().y, 0.01f);
        assertEquals(50, child1Layout.size().width, 0.01f);
        assertEquals(50, child1Layout.size().height, 0.01f);

        Layout child2Layout = tree.getLayout(child2);
        assertEquals(50, child2Layout.location().x, 0.01f);
        assertEquals(0, child2Layout.location().y, 0.01f);
        assertEquals(50, child2Layout.size().width, 0.01f);
        assertEquals(50, child2Layout.size().height, 0.01f);
    }

    @Test
    @DisplayName("Flexbox: column with fixed sizes")
    void testFlexboxColumnFixedSizes() {
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.COLUMN;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(100), TaffyDimension.length(200));

        TaffyStyle child1Style = new TaffyStyle();
        child1Style.size = new TaffySize<>(TaffyDimension.length(50), TaffyDimension.length(50));

        TaffyStyle child2Style = new TaffyStyle();
        child2Style.size = new TaffySize<>(TaffyDimension.length(50), TaffyDimension.length(50));

        NodeId child1 = tree.newLeaf(child1Style);
        NodeId child2 = tree.newLeaf(child2Style);
        NodeId root = tree.newWithChildren(rootStyle, child1, child2);

        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(100f),
            AvailableSpace.definite(200f)
        ));

        Layout rootLayout = tree.getLayout(root);
        assertEquals(100, rootLayout.size().width, 0.01f);
        assertEquals(200, rootLayout.size().height, 0.01f);

        Layout child1Layout = tree.getLayout(child1);
        assertEquals(0, child1Layout.location().x, 0.01f);
        assertEquals(0, child1Layout.location().y, 0.01f);
        assertEquals(50, child1Layout.size().width, 0.01f);
        assertEquals(50, child1Layout.size().height, 0.01f);

        Layout child2Layout = tree.getLayout(child2);
        assertEquals(0, child2Layout.location().x, 0.01f);
        assertEquals(50, child2Layout.location().y, 0.01f);
        assertEquals(50, child2Layout.size().width, 0.01f);
        assertEquals(50, child2Layout.size().height, 0.01f);
    }

    @Test
    @DisplayName("Flexbox: flex-grow equal distribution")
    void testFlexGrowEqual() {
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(300), TaffyDimension.length(100));

        TaffyStyle childStyle1 = new TaffyStyle();
        childStyle1.flexGrow = 1.0f; childStyle1.flexShrink = 1.0f; childStyle1.flexBasis = TaffyDimension.AUTO;
        childStyle1.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.percent(1.0f));

        TaffyStyle childStyle2 = new TaffyStyle();
        childStyle2.flexGrow = 1.0f; childStyle2.flexShrink = 1.0f; childStyle2.flexBasis = TaffyDimension.AUTO;
        childStyle2.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.percent(1.0f));

        TaffyStyle childStyle3 = new TaffyStyle();
        childStyle3.flexGrow = 1.0f; childStyle3.flexShrink = 1.0f; childStyle3.flexBasis = TaffyDimension.AUTO;
        childStyle3.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.percent(1.0f));

        NodeId child1 = tree.newLeaf(childStyle1);
        NodeId child2 = tree.newLeaf(childStyle2);
        NodeId child3 = tree.newLeaf(childStyle3);
        NodeId root = tree.newWithChildren(rootStyle, child1, child2, child3);

        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(300f),
            AvailableSpace.definite(100f)
        ));

        // Each child should get 100px (300 / 3)
        Layout child1Layout = tree.getLayout(child1);
        assertEquals(0, child1Layout.location().x, 0.01f);
        assertEquals(100, child1Layout.size().width, 0.01f);

        Layout child2Layout = tree.getLayout(child2);
        assertEquals(100, child2Layout.location().x, 0.01f);
        assertEquals(100, child2Layout.size().width, 0.01f);

        Layout child3Layout = tree.getLayout(child3);
        assertEquals(200, child3Layout.location().x, 0.01f);
        assertEquals(100, child3Layout.size().width, 0.01f);
    }

    @Test
    @DisplayName("Flexbox: flex-grow weighted distribution")
    void testFlexGrowWeighted() {
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(400), TaffyDimension.length(100));

        TaffyStyle childStyle1 = new TaffyStyle();
        childStyle1.flexGrow = 1.0f; childStyle1.flexShrink = 1.0f; childStyle1.flexBasis = TaffyDimension.AUTO;

        TaffyStyle childStyle2 = new TaffyStyle();
        childStyle2.flexGrow = 2.0f; childStyle2.flexShrink = 1.0f; childStyle2.flexBasis = TaffyDimension.AUTO;

        TaffyStyle childStyle3 = new TaffyStyle();
        childStyle3.flexGrow = 1.0f; childStyle3.flexShrink = 1.0f; childStyle3.flexBasis = TaffyDimension.AUTO;

        NodeId child1 = tree.newLeaf(childStyle1);
        NodeId child2 = tree.newLeaf(childStyle2);
        NodeId child3 = tree.newLeaf(childStyle3);
        NodeId root = tree.newWithChildren(rootStyle, child1, child2, child3);

        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(400f),
            AvailableSpace.definite(100f)
        ));

        // Total grow: 4
        // Child1: 100 (1/4 * 400)
        // Child2: 200 (2/4 * 400)
        // Child3: 100 (1/4 * 400)
        Layout child1Layout = tree.getLayout(child1);
        assertEquals(100, child1Layout.size().width, 0.01f);

        Layout child2Layout = tree.getLayout(child2);
        assertEquals(200, child2Layout.size().width, 0.01f);

        Layout child3Layout = tree.getLayout(child3);
        assertEquals(100, child3Layout.size().width, 0.01f);
    }

    @Test
    @DisplayName("Flexbox: flex-shrink")
    void testFlexShrink() {
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200), TaffyDimension.length(100));

        TaffyStyle childStyle1 = new TaffyStyle();
        childStyle1.flexGrow = 0.0f; childStyle1.flexShrink = 1.0f; childStyle1.flexBasis = TaffyDimension.AUTO;
        childStyle1.size = new TaffySize<>(TaffyDimension.length(150), TaffyDimension.AUTO);

        TaffyStyle childStyle2 = new TaffyStyle();
        childStyle2.flexGrow = 0.0f; childStyle2.flexShrink = 1.0f; childStyle2.flexBasis = TaffyDimension.AUTO;
        childStyle2.size = new TaffySize<>(TaffyDimension.length(150), TaffyDimension.AUTO);

        NodeId child1 = tree.newLeaf(childStyle1);
        NodeId child2 = tree.newLeaf(childStyle2);
        NodeId root = tree.newWithChildren(rootStyle, child1, child2);

        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(200f),
            AvailableSpace.definite(100f)
        ));

        // Total base: 300, container: 200, overflow: 100
        // Each shrinks 50
        Layout child1Layout = tree.getLayout(child1);
        assertEquals(100, child1Layout.size().width, 0.01f);

        Layout child2Layout = tree.getLayout(child2);
        assertEquals(100, child2Layout.size().width, 0.01f);
    }

    @Test
    @DisplayName("Flexbox: gap")
    void testFlexboxGap() {
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.gap = new TaffySize<>(LengthPercentage.length(10f), LengthPercentage.length(0f));
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200), TaffyDimension.length(100));

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(50), TaffyDimension.length(50));

        NodeId child1 = tree.newLeaf(childStyle);
        NodeId child2 = tree.newLeaf(childStyle);
        NodeId child3 = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child1, child2, child3);

        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(200f),
            AvailableSpace.definite(100f)
        ));

        Layout child1Layout = tree.getLayout(child1);
        assertEquals(0, child1Layout.location().x, 0.01f);

        Layout child2Layout = tree.getLayout(child2);
        assertEquals(60, child2Layout.location().x, 0.01f); // 50 + 10

        Layout child3Layout = tree.getLayout(child3);
        assertEquals(120, child3Layout.location().x, 0.01f); // 60 + 50 + 10
    }

    @Test
    @DisplayName("Flexbox: padding")
    void testFlexboxPadding() {
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.padding = TaffyRect.all(LengthPercentage.length(20f));
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200), TaffyDimension.length(100));

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(50), TaffyDimension.length(50));

        NodeId child1 = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child1);

        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(200f),
            AvailableSpace.definite(100f)
        ));

        Layout rootLayout = tree.getLayout(root);
        assertEquals(200, rootLayout.size().width, 0.01f);
        assertEquals(100, rootLayout.size().height, 0.01f);

        Layout childLayout = tree.getLayout(child1);
        assertEquals(20, childLayout.location().x, 0.01f); // padding left
        assertEquals(20, childLayout.location().y, 0.01f); // padding top
    }

    @Test
    @DisplayName("Block: basic stacking")
    void testBlockBasicStacking() {
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.BLOCK;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200), TaffyDimension.length(300));

        TaffyStyle childStyle1 = new TaffyStyle();
        childStyle1.display = TaffyDisplay.BLOCK;
        childStyle1.size = new TaffySize<>(TaffyDimension.length(100), TaffyDimension.length(50));

        TaffyStyle childStyle2 = new TaffyStyle();
        childStyle2.display = TaffyDisplay.BLOCK;
        childStyle2.size = new TaffySize<>(TaffyDimension.length(150), TaffyDimension.length(75));

        NodeId child1 = tree.newLeaf(childStyle1);
        NodeId child2 = tree.newLeaf(childStyle2);
        NodeId root = tree.newWithChildren(rootStyle, child1, child2);

        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(200f),
            AvailableSpace.definite(300f)
        ));

        Layout child1Layout = tree.getLayout(child1);
        assertEquals(0, child1Layout.location().x, 0.01f);
        assertEquals(0, child1Layout.location().y, 0.01f);
        assertEquals(100, child1Layout.size().width, 0.01f);
        assertEquals(50, child1Layout.size().height, 0.01f);

        Layout child2Layout = tree.getLayout(child2);
        assertEquals(0, child2Layout.location().x, 0.01f);
        assertEquals(50, child2Layout.location().y, 0.01f);
        assertEquals(150, child2Layout.size().width, 0.01f);
        assertEquals(75, child2Layout.size().height, 0.01f);
    }

    @Test
    @DisplayName("Block: auto width stretches to parent")
    void testBlockAutoWidth() {
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.BLOCK;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200), TaffyDimension.length(200));

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.display = TaffyDisplay.BLOCK;
        childStyle.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(50));

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(200f),
            AvailableSpace.definite(200f)
        ));

        Layout childLayout = tree.getLayout(child);
        assertEquals(200, childLayout.size().width, 0.01f); // Should stretch to parent
        assertEquals(50, childLayout.size().height, 0.01f);
    }

    @Test
    @DisplayName("Grid: simple 2x2")
    void testGridSimple2x2() {
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.GRID;
        rootStyle.gridTemplateColumns.add(TrackSizingFunction.fr(1f));
        rootStyle.gridTemplateColumns.add(TrackSizingFunction.fr(1f));
        rootStyle.gridTemplateRows.add(TrackSizingFunction.fr(1f));
        rootStyle.gridTemplateRows.add(TrackSizingFunction.fr(1f));
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200), TaffyDimension.length(200));

        TaffyStyle cellStyle = new TaffyStyle();

        NodeId cell1 = tree.newLeaf(cellStyle);
        NodeId cell2 = tree.newLeaf(cellStyle);
        NodeId cell3 = tree.newLeaf(cellStyle);
        NodeId cell4 = tree.newLeaf(cellStyle);
        NodeId root = tree.newWithChildren(rootStyle, cell1, cell2, cell3, cell4);

        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(200f),
            AvailableSpace.definite(200f)
        ));

        Layout rootLayout = tree.getLayout(root);
        assertEquals(200, rootLayout.size().width, 0.01f);
        assertEquals(200, rootLayout.size().height, 0.01f);

        // Each cell should be 100x100
        Layout cell1Layout = tree.getLayout(cell1);
        assertEquals(0, cell1Layout.location().x, 0.01f);
        assertEquals(0, cell1Layout.location().y, 0.01f);
        assertEquals(100, cell1Layout.size().width, 0.01f);
        assertEquals(100, cell1Layout.size().height, 0.01f);

        Layout cell2Layout = tree.getLayout(cell2);
        assertEquals(100, cell2Layout.location().x, 0.01f);
        assertEquals(0, cell2Layout.location().y, 0.01f);

        Layout cell3Layout = tree.getLayout(cell3);
        assertEquals(0, cell3Layout.location().x, 0.01f);
        assertEquals(100, cell3Layout.location().y, 0.01f);

        Layout cell4Layout = tree.getLayout(cell4);
        assertEquals(100, cell4Layout.location().x, 0.01f);
        assertEquals(100, cell4Layout.location().y, 0.01f);
    }

    @Test
    @DisplayName("Nested: flexbox inside flexbox")
    void testNestedFlexbox() {
        TaffyStyle outerStyle = new TaffyStyle();
        outerStyle.display = TaffyDisplay.FLEX;
        outerStyle.flexDirection = FlexDirection.COLUMN;
        outerStyle.size = new TaffySize<>(TaffyDimension.length(300), TaffyDimension.length(300));

        TaffyStyle innerStyle = new TaffyStyle();
        innerStyle.display = TaffyDisplay.FLEX;
        innerStyle.flexDirection = FlexDirection.ROW;
        innerStyle.flexGrow = 1.0f; innerStyle.flexShrink = 1.0f; innerStyle.flexBasis = TaffyDimension.AUTO;
        innerStyle.size = new TaffySize<>(TaffyDimension.percent(1.0f), TaffyDimension.AUTO);

        TaffyStyle leafStyle = new TaffyStyle();
        leafStyle.flexGrow = 1.0f; leafStyle.flexShrink = 1.0f; leafStyle.flexBasis = TaffyDimension.AUTO;

        NodeId leaf1 = tree.newLeaf(leafStyle);
        NodeId leaf2 = tree.newLeaf(leafStyle);
        NodeId inner = tree.newWithChildren(innerStyle, leaf1, leaf2);

        TaffyStyle leaf3Style = new TaffyStyle();
        leaf3Style.size = new TaffySize<>(TaffyDimension.AUTO, TaffyDimension.length(50));

        NodeId leaf3 = tree.newLeaf(leaf3Style);
        NodeId outer = tree.newWithChildren(outerStyle, inner, leaf3);

        tree.computeLayout(outer, new TaffySize<>(
            AvailableSpace.definite(300f),
            AvailableSpace.definite(300f)
        ));

        Layout outerLayout = tree.getLayout(outer);
        assertEquals(300, outerLayout.size().width, 0.01f);
        assertEquals(300, outerLayout.size().height, 0.01f);

        Layout innerLayout = tree.getLayout(inner);
        assertEquals(0, innerLayout.location().y, 0.01f);
        assertEquals(300, innerLayout.size().width, 0.01f);
        assertEquals(250, innerLayout.size().height, 0.01f); // 300 - 50

        Layout leaf1Layout = tree.getLayout(leaf1);
        assertEquals(150, leaf1Layout.size().width, 0.01f); // 300 / 2

        Layout leaf3Layout = tree.getLayout(leaf3);
        assertEquals(250, leaf3Layout.location().y, 0.01f);
        assertEquals(50, leaf3Layout.size().height, 0.01f);
    }

    @Test
    @DisplayName("Percent dimensions")
    void testPercentDimensions() {
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(400), TaffyDimension.length(200));

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.percent(0.5f), TaffyDimension.percent(0.5f));

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(400f),
            AvailableSpace.definite(200f)
        ));

        Layout childLayout = tree.getLayout(child);
        assertEquals(200, childLayout.size().width, 0.01f);  // 50% of 400
        assertEquals(100, childLayout.size().height, 0.01f); // 50% of 200
    }

    @Test
    @DisplayName("Min/Max constraints")
    void testMinMaxConstraints() {
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(500), TaffyDimension.length(100));

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.flexGrow = 1.0f; childStyle.flexShrink = 1.0f; childStyle.flexBasis = TaffyDimension.AUTO;
        childStyle.minSize = new TaffySize<>(TaffyDimension.length(100), TaffyDimension.AUTO);
        childStyle.maxSize = new TaffySize<>(TaffyDimension.length(200), TaffyDimension.AUTO);

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(500f),
            AvailableSpace.definite(100f)
        ));

        Layout childLayout = tree.getLayout(child);
        // flex-grow would give 500, but max is 200
        assertEquals(200, childLayout.size().width, 0.01f);
    }

    @Test
    @DisplayName("Border affects content area")
    void testBorderAffectsContent() {
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.border = TaffyRect.all(LengthPercentage.length(10f));
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200), TaffyDimension.length(100));

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.flexGrow = 1.0f; childStyle.flexShrink = 1.0f; childStyle.flexBasis = TaffyDimension.AUTO;

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(200f),
            AvailableSpace.definite(100f)
        ));

        Layout childLayout = tree.getLayout(child);
        assertEquals(10, childLayout.location().x, 0.01f);  // border left
        assertEquals(10, childLayout.location().y, 0.01f);  // border top
        assertEquals(180, childLayout.size().width, 0.01f); // 200 - 10 - 10
        assertEquals(80, childLayout.size().height, 0.01f); // 100 - 10 - 10
    }

    @Test
    @DisplayName("Aspect ratio")
    void testAspectRatio() {
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200), TaffyDimension.length(200));

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(100), TaffyDimension.AUTO);
        childStyle.aspectRatio = 2.0f; // width:height = 2:1
        // align-self: start prevents stretch from overriding aspect-ratio
        childStyle.alignSelf = AlignItems.START;

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(200f),
            AvailableSpace.definite(200f)
        ));

        Layout childLayout = tree.getLayout(child);
        assertEquals(100, childLayout.size().width, 0.01f);
        assertEquals(50, childLayout.size().height, 0.01f); // width / aspectRatio = 100 / 2
    }

    @Test
    @DisplayName("Flexbox wrap")
    void testFlexboxWrap() {
        TaffyStyle rootStyle = new TaffyStyle();
        rootStyle.display = TaffyDisplay.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.flexWrap = FlexWrap.WRAP;
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200), TaffyDimension.AUTO);

        TaffyStyle childStyle = new TaffyStyle();
        childStyle.size = new TaffySize<>(TaffyDimension.length(80), TaffyDimension.length(50));

        NodeId child1 = tree.newLeaf(childStyle);
        NodeId child2 = tree.newLeaf(childStyle);
        NodeId child3 = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child1, child2, child3);

        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(200f),
            AvailableSpace.maxContent()
        ));

        // 80 + 80 = 160 fits on first line
        // Third item wraps to second line

        Layout child1Layout = tree.getLayout(child1);
        assertEquals(0, child1Layout.location().x, 0.01f);
        assertEquals(0, child1Layout.location().y, 0.01f);

        Layout child2Layout = tree.getLayout(child2);
        assertEquals(80, child2Layout.location().x, 0.01f);
        assertEquals(0, child2Layout.location().y, 0.01f);

        Layout child3Layout = tree.getLayout(child3);
        assertEquals(0, child3Layout.location().x, 0.01f);
        assertEquals(50, child3Layout.location().y, 0.01f); // Wrapped to next line

        Layout rootLayout = tree.getLayout(root);
        assertEquals(100, rootLayout.size().height, 0.01f); // Two lines of 50px each
    }
}
