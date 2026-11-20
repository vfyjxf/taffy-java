package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.NamedGridLine;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression tests for absolutely positioned grid children placed with named lines.
 * <p>
 * Upstream test: {@code grid_absolute_known_named_lines__content_box_ltr.xml}. The port used to
 * pass the raw {@link GridPlacement}s of absolutely positioned children straight into the absolute
 * grid-area resolution, where named lines and named spans had no branch and silently fell back to
 * the container edges (so the item filled the whole grid). Named lines must be resolved through the
 * {@code NamedLineResolver} before the grid area is computed, exactly like in-flow items.
 */
class GridAbsoluteNamedLinesTest {

    private static final float EPSILON = 0.01f;

    @Test
    void absoluteGridAreaResolvesKnownNamedLines() {
        TaffyTree tree = new TaffyTree();
        NodeId container = newContainer(tree);

        // grid-row: a / c; grid-column: a / c
        NodeId all = newAbsoluteChild(tree,
            namedLine("a"), namedLine("c"),
            namedLine("a"), namedLine("c"));
        // grid-row-end: b; grid-column-start: b (auto resolves to the container edges)
        NodeId partial = newAbsoluteChild(tree,
            namedLine("b"), GridPlacement.auto(),
            GridPlacement.auto(), namedLine("b"));
        // Swapped lines must be normalized: grid-row: c / a; grid-column: c / a
        NodeId reversed = newAbsoluteChild(tree,
            namedLine("c"), namedLine("a"),
            namedLine("c"), namedLine("a"));

        tree.addChild(container, all);
        tree.addChild(container, partial);
        tree.addChild(container, reversed);

        tree.computeLayout(container, TaffySize.maxContent());

        Layout containerLayout = tree.getLayout(container);
        assertEquals(220.0f, containerLayout.size().width, EPSILON, "container border-box width");
        assertEquals(170.0f, containerLayout.size().height, EPSILON, "container border-box height");

        // Column a starts at the left padding edge (10), column c ends at 10 + 40 + 60 = 110.
        // Row a starts at the top padding edge (10), row c ends at 10 + 20 + 50 = 80.
        assertRect(tree.getLayout(all), 10.0f, 10.0f, 100.0f, 70.0f, "a / c");

        // Only one line is known in each axis, so the other edge is the container's border edge.
        assertRect(tree.getLayout(partial), 50.0f, 0.0f, 170.0f, 30.0f, "b / auto");

        assertRect(tree.getLayout(reversed), 10.0f, 10.0f, 100.0f, 70.0f, "c / a reversed");
    }

    @Test
    void absoluteNamedLineWithNumericSpan() {
        TaffyTree tree = new TaffyTree();
        NodeId container = newContainer(tree);

        // grid-row: a / span 1; grid-column: a / span 1
        NodeId child = newAbsoluteChild(tree,
            namedLine("a"), GridPlacement.span(1),
            namedLine("a"), GridPlacement.span(1));

        tree.addChild(container, child);
        tree.computeLayout(container, TaffySize.maxContent());

        // 40px column 1 and 20px row 1, starting at the padding edge.
        assertRect(tree.getLayout(child), 10.0f, 10.0f, 40.0f, 20.0f, "a / span 1");
    }

    @Test
    void absoluteNamedSpanResolvesAgainstNamedLine() {
        TaffyTree tree = new TaffyTree();
        NodeId container = newContainer(tree);

        // grid-row: a / span b; grid-column: a / span b
        NodeId child = newAbsoluteChild(tree,
            namedLine("a"), GridPlacement.namedSpan("b", 1),
            namedLine("a"), GridPlacement.namedSpan("b", 1));

        tree.addChild(container, child);
        tree.computeLayout(container, TaffySize.maxContent());

        // The named span "b" from line a resolves to line b, i.e. the end of the first track.
        assertRect(tree.getLayout(child), 10.0f, 10.0f, 40.0f, 20.0f, "a / span b");
    }

    @Test
    void absoluteNamedLinesWithInFlowSibling() {
        // An in-flow item forces the non-empty grid path (the fixture itself exercises the
        // empty-grid early-return path, where absolutely positioned children are also laid out).
        TaffyTree tree = new TaffyTree();
        NodeId container = newContainer(tree);

        TaffyStyle inFlowStyle = new TaffyStyle();
        inFlowStyle.gridColumn = new TaffyLine<>(GridPlacement.line(1), GridPlacement.line(2));
        inFlowStyle.gridRow = new TaffyLine<>(GridPlacement.line(1), GridPlacement.line(2));
        NodeId inFlow = tree.newLeaf(inFlowStyle);

        NodeId absolute = newAbsoluteChild(tree,
            namedLine("a"), namedLine("c"),
            namedLine("a"), namedLine("c"));

        tree.addChild(container, inFlow);
        tree.addChild(container, absolute);
        tree.computeLayout(container, TaffySize.maxContent());

        assertRect(tree.getLayout(inFlow), 10.0f, 10.0f, 40.0f, 20.0f, "in-flow line 1 / 2");
        assertRect(tree.getLayout(absolute), 10.0f, 10.0f, 100.0f, 70.0f, "in-flow sibling a / c");
    }

    private static NodeId newContainer(TaffyTree tree) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        style.boxSizing = BoxSizing.CONTENT_BOX;
        style.size = new TaffySize<>(TaffyDimension.length(200.0f), TaffyDimension.length(150.0f));
        style.padding = TaffyRect.all(LengthPercentage.length(10.0f));
        style.gridTemplateColumns = Arrays.asList(
            TrackSizingFunction.fixed(LengthPercentage.length(40.0f)),
            TrackSizingFunction.fixed(LengthPercentage.length(60.0f))
        );
        style.gridTemplateRows = Arrays.asList(
            TrackSizingFunction.fixed(LengthPercentage.length(20.0f)),
            TrackSizingFunction.fixed(LengthPercentage.length(50.0f))
        );
        style.gridTemplateColumnNames = Arrays.asList(
            new NamedGridLine("a", 1),
            new NamedGridLine("b", 2),
            new NamedGridLine("c", 3)
        );
        style.gridTemplateRowNames = Arrays.asList(
            new NamedGridLine("a", 1),
            new NamedGridLine("b", 2),
            new NamedGridLine("c", 3)
        );
        return tree.newLeaf(style);
    }

    private static NodeId newAbsoluteChild(
        TaffyTree tree,
        GridPlacement columnStart, GridPlacement columnEnd,
        GridPlacement rowStart, GridPlacement rowEnd) {
        TaffyStyle style = new TaffyStyle();
        style.position = TaffyPosition.ABSOLUTE;
        // Stretch the absolutely positioned item over its resolved grid area.
        style.inset = TaffyRect.all(LengthPercentageAuto.length(0.0f));
        style.gridColumn = new TaffyLine<>(columnStart, columnEnd);
        style.gridRow = new TaffyLine<>(rowStart, rowEnd);
        return tree.newLeaf(style);
    }

    private static GridPlacement namedLine(String name) {
        return GridPlacement.namedLine(name);
    }

    private static void assertRect(Layout layout, float x, float y, float width, float height, String label) {
        assertEquals(x, layout.location().x, EPSILON, label + " x");
        assertEquals(y, layout.location().y, EPSILON, label + " y");
        assertEquals(width, layout.size().width, EPSILON, label + " width");
        assertEquals(height, layout.size().height, EPSILON, label + " height");
    }
}
