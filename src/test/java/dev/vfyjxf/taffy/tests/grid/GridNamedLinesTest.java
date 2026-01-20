package dev.vfyjxf.taffy.tests.grid;

import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.GridTemplateArea;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.NamedGridLine;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Grid named lines and areas support.
 * Verifies the NamedLineResolver correctly resolves named line placements.
 */
class GridNamedLinesTest {

    @Test
    void testGridPlacementNamedLineTypes() {
        // Test that GridPlacement properly supports named line types
        GridPlacement namedLine = GridPlacement.namedLine("header");
        assertTrue(namedLine.isNamedLine());
        assertEquals("header", namedLine.getLineName());
        assertEquals(1, namedLine.getNthIndex());

        GridPlacement namedLineNth = GridPlacement.namedLine("sidebar", 2);
        assertTrue(namedLineNth.isNamedLine());
        assertEquals("sidebar", namedLineNth.getLineName());
        assertEquals(2, namedLineNth.getNthIndex());

        GridPlacement namedSpan = GridPlacement.namedSpan("content", 2);
        assertTrue(namedSpan.isNamedSpan());
        assertEquals("content", namedSpan.getLineName());
        assertEquals(2, namedSpan.getValue());
    }

    @Test
    void testGridTemplateAreaCreation() {
        // Test GridTemplateArea creation
        GridTemplateArea area = new GridTemplateArea("header", 1, 2, 1, 4);
        assertEquals("header", area.getName());
        assertEquals(1, area.getRowStart());
        assertEquals(2, area.getRowEnd());
        assertEquals(1, area.getColumnStart());
        assertEquals(4, area.getColumnEnd());
    }

    @Test
    void testNamedGridLineCreation() {
        // Test NamedGridLine creation
        NamedGridLine line = new NamedGridLine("sidebar-start", 2);
        assertEquals("sidebar-start", line.getName());
        assertEquals(2, line.getIndex());
    }

    @Test
    void testGridWithExplicitNamedLines() {
        // Test a grid with explicitly named column lines
        // grid-template-columns: [first] 100px [middle] 100px [last]
        TaffyTree tree = new TaffyTree();

        TaffyStyle containerStyle = new TaffyStyle();
        containerStyle.display = TaffyDisplay.GRID;
        containerStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(200f));
        containerStyle.gridTemplateColumns = Arrays.asList(
            TrackSizingFunction.fixed(LengthPercentage.length(100f)),
            TrackSizingFunction.fixed(LengthPercentage.length(100f))
        );
        // Add named lines
        containerStyle.gridTemplateColumnNames = Arrays.asList(
            new NamedGridLine("first", 1),
            new NamedGridLine("middle", 2),
            new NamedGridLine("last", 3)
        );
        containerStyle.gridTemplateRows = Arrays.asList(
            TrackSizingFunction.fixed(LengthPercentage.length(100f))
        );

        NodeId root = tree.newLeaf(containerStyle);

        // Child placed at named line "first" to "middle"
        TaffyStyle childStyle = new TaffyStyle();
        childStyle.gridColumn = new TaffyLine<>(
            GridPlacement.namedLine("first"),
            GridPlacement.namedLine("middle")
        );
        NodeId child = tree.newLeaf(childStyle);
        tree.addChild(root, child);

        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(300f), AvailableSpace.definite(200f)));

        // Named line "first" is line 1 (origin-zero: 0)
        // Named line "middle" is line 2 (origin-zero: 1)
        // So child should span column 0 only (one column)
        float childX = tree.getLayout(child).location().x;
        float childWidth = tree.getLayout(child).contentSize().width;
        
        assertEquals(0f, childX, 0.1f, "Child should start at x=0");
        assertEquals(100f, childWidth, 0.1f, "Child should be 100px wide (one column)");
    }

    @Test
    void testGridWithTemplateAreas() {
        // Test grid-template-areas creating implicit named lines
        // grid-template-areas:
        //   "header header header"
        //   "sidebar content content"
        //   "footer footer footer"
        TaffyTree tree = new TaffyTree();

        TaffyStyle containerStyle = new TaffyStyle();
        containerStyle.display = TaffyDisplay.GRID;
        containerStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(300f));
        
        // 3 columns x 3 rows, 100px each
        containerStyle.gridTemplateColumns = Arrays.asList(
            TrackSizingFunction.fixed(LengthPercentage.length(100f)),
            TrackSizingFunction.fixed(LengthPercentage.length(100f)),
            TrackSizingFunction.fixed(LengthPercentage.length(100f))
        );
        containerStyle.gridTemplateRows = Arrays.asList(
            TrackSizingFunction.fixed(LengthPercentage.length(100f)),
            TrackSizingFunction.fixed(LengthPercentage.length(100f)),
            TrackSizingFunction.fixed(LengthPercentage.length(100f))
        );
        
        // Define areas
        containerStyle.gridTemplateAreas = Arrays.asList(
            new GridTemplateArea("header", 1, 2, 1, 4),    // row 1, cols 1-3
            new GridTemplateArea("sidebar", 2, 3, 1, 2),  // row 2, col 1
            new GridTemplateArea("content", 2, 3, 2, 4),  // row 2, cols 2-3
            new GridTemplateArea("footer", 3, 4, 1, 4)    // row 3, cols 1-3
        );

        NodeId root = tree.newLeaf(containerStyle);

        // Child placed in "header" area using implicit "header-start" and "header-end" lines
        TaffyStyle headerStyle = new TaffyStyle();
        headerStyle.gridColumn = new TaffyLine<>(
            GridPlacement.namedLine("header-start"),
            GridPlacement.namedLine("header-end")
        );
        headerStyle.gridRow = new TaffyLine<>(
            GridPlacement.namedLine("header-start"),
            GridPlacement.namedLine("header-end")
        );
        NodeId header = tree.newLeaf(headerStyle);
        tree.addChild(root, header);

        // Child in sidebar area
        TaffyStyle sidebarStyle = new TaffyStyle();
        sidebarStyle.gridColumn = new TaffyLine<>(
            GridPlacement.namedLine("sidebar-start"),
            GridPlacement.namedLine("sidebar-end")
        );
        sidebarStyle.gridRow = new TaffyLine<>(
            GridPlacement.namedLine("sidebar-start"),
            GridPlacement.namedLine("sidebar-end")
        );
        NodeId sidebar = tree.newLeaf(sidebarStyle);
        tree.addChild(root, sidebar);

        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(300f), AvailableSpace.definite(300f)));

        // Header should be at row 1, spanning all 3 columns
        assertEquals(0f, tree.getLayout(header).location().x, 0.1f);
        assertEquals(0f, tree.getLayout(header).location().y, 0.1f);
        assertEquals(300f, tree.getLayout(header).contentSize().width, 0.1f, "Header spans 3 columns");
        assertEquals(100f, tree.getLayout(header).contentSize().height, 0.1f, "Header is 1 row");

        // Sidebar should be at row 2, col 1
        assertEquals(0f, tree.getLayout(sidebar).location().x, 0.1f);
        assertEquals(100f, tree.getLayout(sidebar).location().y, 0.1f, "Sidebar starts at row 2");
        assertEquals(100f, tree.getLayout(sidebar).contentSize().width, 0.1f, "Sidebar is 1 column");
        assertEquals(100f, tree.getLayout(sidebar).contentSize().height, 0.1f, "Sidebar is 1 row");
    }

    @Test
    void testNamedLineWithNthOccurrence() {
        // Test using nth occurrence of a named line
        // grid-template-columns: [col] 100px [col] 100px [col]
        // Line named "col" appears at lines 1, 2, and 3
        TaffyTree tree = new TaffyTree();

        TaffyStyle containerStyle = new TaffyStyle();
        containerStyle.display = TaffyDisplay.GRID;
        containerStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(100f));
        containerStyle.gridTemplateColumns = Arrays.asList(
            TrackSizingFunction.fixed(LengthPercentage.length(100f)),
            TrackSizingFunction.fixed(LengthPercentage.length(100f))
        );
        // Name "col" at lines 1, 2, 3
        containerStyle.gridTemplateColumnNames = Arrays.asList(
            new NamedGridLine("col", 1),
            new NamedGridLine("col", 2),
            new NamedGridLine("col", 3)
        );
        containerStyle.gridTemplateRows = Arrays.asList(
            TrackSizingFunction.fixed(LengthPercentage.length(100f))
        );

        NodeId root = tree.newLeaf(containerStyle);

        // Child from 2nd "col" to 3rd "col" (second column only)
        TaffyStyle childStyle = new TaffyStyle();
        childStyle.gridColumn = new TaffyLine<>(
            GridPlacement.namedLine("col", 2),  // 2nd occurrence
            GridPlacement.namedLine("col", 3)   // 3rd occurrence
        );
        NodeId child = tree.newLeaf(childStyle);
        tree.addChild(root, child);

        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(300f), AvailableSpace.definite(100f)));

        // 2nd "col" is line 2 (origin-zero: 1)
        // 3rd "col" is line 3 (origin-zero: 2)
        // So child occupies column index 1 (second column)
        assertEquals(100f, tree.getLayout(child).location().x, 0.1f, "Child at 2nd column");
        assertEquals(100f, tree.getLayout(child).contentSize().width, 0.1f);
    }

    @Test
    void testFallbackForNonExistentNamedLine() {
        // When a named line doesn't exist, grid falls back to implicit grid
        TaffyTree tree = new TaffyTree();

        TaffyStyle containerStyle = new TaffyStyle();
        containerStyle.display = TaffyDisplay.GRID;
        containerStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(100f));
        containerStyle.gridTemplateColumns = Arrays.asList(
            TrackSizingFunction.fixed(LengthPercentage.length(100f)),
            TrackSizingFunction.fixed(LengthPercentage.length(100f))
        );
        containerStyle.gridTemplateRows = Arrays.asList(
            TrackSizingFunction.fixed(LengthPercentage.length(100f))
        );

        NodeId root = tree.newLeaf(containerStyle);

        // Reference a non-existent named line "nonexistent"
        // Per CSS spec, this should fall back to the first implicit line
        TaffyStyle childStyle = new TaffyStyle();
        childStyle.gridColumn = new TaffyLine<>(
            GridPlacement.namedLine("nonexistent"),
            GridPlacement.span(1)
        );
        NodeId child = tree.newLeaf(childStyle);
        tree.addChild(root, child);

        // This should not throw and should place the item somewhere
        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(200f), AvailableSpace.definite(100f)));

        // The item should be placed (in implicit grid area beyond explicit tracks)
        var layout = tree.getLayout(child);
        assertNotNull(layout);
    }

    @Test
    void testMixedNamedAndNumericPlacement() {
        // Mix named lines with numeric line references
        TaffyTree tree = new TaffyTree();

        TaffyStyle containerStyle = new TaffyStyle();
        containerStyle.display = TaffyDisplay.GRID;
        containerStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(100f));
        containerStyle.gridTemplateColumns = Arrays.asList(
            TrackSizingFunction.fixed(LengthPercentage.length(100f)),
            TrackSizingFunction.fixed(LengthPercentage.length(100f)),
            TrackSizingFunction.fixed(LengthPercentage.length(100f))
        );
        containerStyle.gridTemplateColumnNames = Arrays.asList(
            new NamedGridLine("start", 1),
            new NamedGridLine("end", 4)
        );
        containerStyle.gridTemplateRows = Arrays.asList(
            TrackSizingFunction.fixed(LengthPercentage.length(100f))
        );

        NodeId root = tree.newLeaf(containerStyle);

        // Start at named "start", end at numeric line 3
        TaffyStyle childStyle = new TaffyStyle();
        childStyle.gridColumn = new TaffyLine<>(
            GridPlacement.namedLine("start"),
            GridPlacement.line(3)
        );
        NodeId child = tree.newLeaf(childStyle);
        tree.addChild(root, child);

        tree.computeLayout(root, new TaffySize<>(AvailableSpace.definite(300f), AvailableSpace.definite(100f)));

        // "start" is line 1, numeric 3 is line 3
        // spans columns 0 and 1 (2 columns, 200px)
        assertEquals(0f, tree.getLayout(child).location().x, 0.1f);
        assertEquals(200f, tree.getLayout(child).contentSize().width, 0.1f);
    }
}
