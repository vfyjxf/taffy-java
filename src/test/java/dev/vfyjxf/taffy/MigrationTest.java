package dev.vfyjxf.taffy;

import com.google.gson.Gson;
import dev.vfyjxf.taffy.geometry.Rect;
import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.Dimension;
import dev.vfyjxf.taffy.style.Display;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.Style;
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
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.size = new Size<>(Dimension.length(200), Dimension.length(100));

        Style child1Style = new Style();
        child1Style.size = new Size<>(Dimension.length(50), Dimension.length(50));

        Style child2Style = new Style();
        child2Style.size = new Size<>(Dimension.length(50), Dimension.length(50));

        NodeId child1 = tree.newLeaf(child1Style);
        NodeId child2 = tree.newLeaf(child2Style);
        NodeId root = tree.newWithChildren(rootStyle, child1, child2);

        tree.computeLayout(root, new Size<>(
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
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.flexDirection = FlexDirection.COLUMN;
        rootStyle.size = new Size<>(Dimension.length(100), Dimension.length(200));

        Style child1Style = new Style();
        child1Style.size = new Size<>(Dimension.length(50), Dimension.length(50));

        Style child2Style = new Style();
        child2Style.size = new Size<>(Dimension.length(50), Dimension.length(50));

        NodeId child1 = tree.newLeaf(child1Style);
        NodeId child2 = tree.newLeaf(child2Style);
        NodeId root = tree.newWithChildren(rootStyle, child1, child2);

        tree.computeLayout(root, new Size<>(
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
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.size = new Size<>(Dimension.length(300), Dimension.length(100));

        Style childStyle1 = new Style();
        childStyle1.flexGrow = 1.0f;
        childStyle1.size = new Size<>(Dimension.AUTO, Dimension.percent(1.0f));

        Style childStyle2 = new Style();
        childStyle2.flexGrow = 1.0f;
        childStyle2.size = new Size<>(Dimension.AUTO, Dimension.percent(1.0f));

        Style childStyle3 = new Style();
        childStyle3.flexGrow = 1.0f;
        childStyle3.size = new Size<>(Dimension.AUTO, Dimension.percent(1.0f));

        NodeId child1 = tree.newLeaf(childStyle1);
        NodeId child2 = tree.newLeaf(childStyle2);
        NodeId child3 = tree.newLeaf(childStyle3);
        NodeId root = tree.newWithChildren(rootStyle, child1, child2, child3);

        tree.computeLayout(root, new Size<>(
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
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.size = new Size<>(Dimension.length(400), Dimension.length(100));

        Style childStyle1 = new Style();
        childStyle1.flexGrow = 1.0f;

        Style childStyle2 = new Style();
        childStyle2.flexGrow = 2.0f;

        Style childStyle3 = new Style();
        childStyle3.flexGrow = 1.0f;

        NodeId child1 = tree.newLeaf(childStyle1);
        NodeId child2 = tree.newLeaf(childStyle2);
        NodeId child3 = tree.newLeaf(childStyle3);
        NodeId root = tree.newWithChildren(rootStyle, child1, child2, child3);

        tree.computeLayout(root, new Size<>(
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
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.size = new Size<>(Dimension.length(200), Dimension.length(100));

        Style childStyle1 = new Style();
        childStyle1.flexShrink = 1.0f;
        childStyle1.size = new Size<>(Dimension.length(150), Dimension.AUTO);

        Style childStyle2 = new Style();
        childStyle2.flexShrink = 1.0f;
        childStyle2.size = new Size<>(Dimension.length(150), Dimension.AUTO);

        NodeId child1 = tree.newLeaf(childStyle1);
        NodeId child2 = tree.newLeaf(childStyle2);
        NodeId root = tree.newWithChildren(rootStyle, child1, child2);

        tree.computeLayout(root, new Size<>(
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
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.gap = new Size<>(LengthPercentage.length(10f), LengthPercentage.length(0f));
        rootStyle.size = new Size<>(Dimension.length(200), Dimension.length(100));

        Style childStyle = new Style();
        childStyle.size = new Size<>(Dimension.length(50), Dimension.length(50));

        NodeId child1 = tree.newLeaf(childStyle);
        NodeId child2 = tree.newLeaf(childStyle);
        NodeId child3 = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child1, child2, child3);

        tree.computeLayout(root, new Size<>(
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
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.padding = Rect.all(LengthPercentage.length(20f));
        rootStyle.size = new Size<>(Dimension.length(200), Dimension.length(100));

        Style childStyle = new Style();
        childStyle.size = new Size<>(Dimension.length(50), Dimension.length(50));

        NodeId child1 = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child1);

        tree.computeLayout(root, new Size<>(
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
        Style rootStyle = new Style();
        rootStyle.display = Display.BLOCK;
        rootStyle.size = new Size<>(Dimension.length(200), Dimension.length(300));

        Style childStyle1 = new Style();
        childStyle1.display = Display.BLOCK;
        childStyle1.size = new Size<>(Dimension.length(100), Dimension.length(50));

        Style childStyle2 = new Style();
        childStyle2.display = Display.BLOCK;
        childStyle2.size = new Size<>(Dimension.length(150), Dimension.length(75));

        NodeId child1 = tree.newLeaf(childStyle1);
        NodeId child2 = tree.newLeaf(childStyle2);
        NodeId root = tree.newWithChildren(rootStyle, child1, child2);

        tree.computeLayout(root, new Size<>(
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
        Style rootStyle = new Style();
        rootStyle.display = Display.BLOCK;
        rootStyle.size = new Size<>(Dimension.length(200), Dimension.length(200));

        Style childStyle = new Style();
        childStyle.display = Display.BLOCK;
        childStyle.size = new Size<>(Dimension.AUTO, Dimension.length(50));

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, new Size<>(
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
        Style rootStyle = new Style();
        rootStyle.display = Display.GRID;
        rootStyle.gridTemplateColumns.add(TrackSizingFunction.fr(1f));
        rootStyle.gridTemplateColumns.add(TrackSizingFunction.fr(1f));
        rootStyle.gridTemplateRows.add(TrackSizingFunction.fr(1f));
        rootStyle.gridTemplateRows.add(TrackSizingFunction.fr(1f));
        rootStyle.size = new Size<>(Dimension.length(200), Dimension.length(200));

        Style cellStyle = new Style();

        NodeId cell1 = tree.newLeaf(cellStyle);
        NodeId cell2 = tree.newLeaf(cellStyle);
        NodeId cell3 = tree.newLeaf(cellStyle);
        NodeId cell4 = tree.newLeaf(cellStyle);
        NodeId root = tree.newWithChildren(rootStyle, cell1, cell2, cell3, cell4);

        tree.computeLayout(root, new Size<>(
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
        Style outerStyle = new Style();
        outerStyle.display = Display.FLEX;
        outerStyle.flexDirection = FlexDirection.COLUMN;
        outerStyle.size = new Size<>(Dimension.length(300), Dimension.length(300));

        Style innerStyle = new Style();
        innerStyle.display = Display.FLEX;
        innerStyle.flexDirection = FlexDirection.ROW;
        innerStyle.flexGrow = 1.0f;
        innerStyle.size = new Size<>(Dimension.percent(1.0f), Dimension.AUTO);

        Style leafStyle = new Style();
        leafStyle.flexGrow = 1.0f;

        NodeId leaf1 = tree.newLeaf(leafStyle);
        NodeId leaf2 = tree.newLeaf(leafStyle);
        NodeId inner = tree.newWithChildren(innerStyle, leaf1, leaf2);

        Style leaf3Style = new Style();
        leaf3Style.size = new Size<>(Dimension.AUTO, Dimension.length(50));

        NodeId leaf3 = tree.newLeaf(leaf3Style);
        NodeId outer = tree.newWithChildren(outerStyle, inner, leaf3);

        tree.computeLayout(outer, new Size<>(
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
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.size = new Size<>(Dimension.length(400), Dimension.length(200));

        Style childStyle = new Style();
        childStyle.size = new Size<>(Dimension.percent(0.5f), Dimension.percent(0.5f));

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, new Size<>(
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
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.size = new Size<>(Dimension.length(500), Dimension.length(100));

        Style childStyle = new Style();
        childStyle.flexGrow = 1.0f;
        childStyle.minSize = new Size<>(Dimension.length(100), Dimension.AUTO);
        childStyle.maxSize = new Size<>(Dimension.length(200), Dimension.AUTO);

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, new Size<>(
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
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.border = Rect.all(LengthPercentage.length(10f));
        rootStyle.size = new Size<>(Dimension.length(200), Dimension.length(100));

        Style childStyle = new Style();
        childStyle.flexGrow = 1.0f;

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, new Size<>(
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
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.size = new Size<>(Dimension.length(200), Dimension.length(200));

        Style childStyle = new Style();
        childStyle.size = new Size<>(Dimension.length(100), Dimension.AUTO);
        childStyle.aspectRatio = 2.0f; // width:height = 2:1
        // align-self: start prevents stretch from overriding aspect-ratio
        childStyle.alignSelf = AlignItems.START;

        NodeId child = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child);

        tree.computeLayout(root, new Size<>(
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
        Style rootStyle = new Style();
        rootStyle.display = Display.FLEX;
        rootStyle.flexDirection = FlexDirection.ROW;
        rootStyle.flexWrap = FlexWrap.WRAP;
        rootStyle.size = new Size<>(Dimension.length(200), Dimension.AUTO);

        Style childStyle = new Style();
        childStyle.size = new Size<>(Dimension.length(80), Dimension.length(50));

        NodeId child1 = tree.newLeaf(childStyle);
        NodeId child2 = tree.newLeaf(childStyle);
        NodeId child3 = tree.newLeaf(childStyle);
        NodeId root = tree.newWithChildren(rootStyle, child1, child2, child3);

        tree.computeLayout(root, new Size<>(
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
