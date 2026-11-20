package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.GridTemplateArea;
import dev.vfyjxf.taffy.style.TaffyGridTemplateAreas;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.tree.grid.NamedLineResolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P7 GR-16: {@code grid-template-areas} as a sized template, unnamed ({@code .}) cells, and areas
 * extending past the track template.
 *
 * <p>Upstream taffy #1024 ({@code 9d1c5ca}) replaced {@code Style::grid_template_areas:
 * Vec<GridTemplateArea>} with {@code Option<TaffyGridTemplateAreas>}, where the new struct bundles the
 * named areas with the overall size of the area template ({@code row_count}/{@code column_count}).
 * The size cannot be derived from the named areas because unnamed ({@code .}) cells still create
 * explicit tracks (e.g. {@code "a ."} is a one row by two column template), and the resolver now
 * takes the size from the style instead of deriving it from the area extents. The size feeds the
 * explicit grid ({@code explicit_count = max(template_count, area_count)}), and tracks created by
 * areas beyond the track template are sized by {@code grid-auto-*}.</p>
 *
 * <p>The Java API keeps {@link TaffyStyle#gridTemplateAreas} as a {@code List<GridTemplateArea>}
 * for source/binary compatibility (JCC), so the template size is additive:
 * {@link TaffyGridTemplateAreas} is the value type and
 * {@link TaffyStyle#gridTemplateAreaRowCount}/{@link TaffyStyle#gridTemplateAreaColumnCount} carry
 * the size (default {@link TaffyGridTemplateAreas#DERIVE} = derive from the named areas, the pre-GR-16
 * behavior). The tests below pin the new behavior, and compare it with the legacy derived-size
 * behavior where the two differ (old/new is noted in each such test).</p>
 */
class GridAreasP7Test {

    private static final float EPSILON = 0.1f;

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static TrackSizingFunction px(float value) {
        return TrackSizingFunction.fixed(LengthPercentage.length(value));
    }

    private static TaffyStyle gridContainer() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        // Track sizes are asserted in these tests, so opt out of the default stretch distribution.
        style.justifyContent = AlignContent.START;
        style.alignContent = AlignContent.START;
        return style;
    }

    private static TaffyLine<GridPlacement> place(GridPlacement start, GridPlacement end) {
        return new TaffyLine<>(start, end);
    }

    private static TaffyLine<GridPlacement> autoLine() {
        return place(GridPlacement.auto(), GridPlacement.auto());
    }

    private static NodeId addItem(TaffyTree tree, NodeId parent,
                                  TaffyLine<GridPlacement> column,
                                  TaffyLine<GridPlacement> row) {
        TaffyStyle style = new TaffyStyle();
        style.gridColumn = column;
        style.gridRow = row;
        NodeId child = tree.newLeaf(style);
        tree.addChild(parent, child);
        return child;
    }

    private static NodeId addAreaItem(TaffyTree tree, NodeId parent, String areaName) {
        return addItem(tree, parent,
            place(GridPlacement.namedLine(areaName), GridPlacement.namedLine(areaName)),
            place(GridPlacement.namedLine(areaName), GridPlacement.namedLine(areaName)));
    }

    private static void assertRect(TaffyTree tree, NodeId node,
                                   float x, float y, float width, float height) {
        assertEquals(x, tree.getLayout(node).location().x, EPSILON, "x");
        assertEquals(y, tree.getLayout(node).location().y, EPSILON, "y");
        assertEquals(width, tree.getLayout(node).size().width, EPSILON, "width");
        assertEquals(height, tree.getLayout(node).size().height, EPSILON, "height");
    }

    private static int resolvedStart(NamedLineResolver resolver, GridPlacement placement, boolean columnAxis) {
        TaffyLine<GridPlacement> resolved = columnAxis
            ? resolver.resolveColumnNames(place(placement, GridPlacement.auto()))
            : resolver.resolveRowNames(place(placement, GridPlacement.auto()));
        assertTrue(resolved.start.isLine(), "expected a resolved line but got " + resolved.start);
        return resolved.start.getLineNumber();
    }

    private static int resolvedEnd(NamedLineResolver resolver, GridPlacement placement, boolean columnAxis) {
        TaffyLine<GridPlacement> resolved = columnAxis
            ? resolver.resolveColumnNames(place(GridPlacement.auto(), placement))
            : resolver.resolveRowNames(place(GridPlacement.auto(), placement));
        assertTrue(resolved.end.isLine(), "expected a resolved line but got " + resolved.end);
        return resolved.end.getLineNumber();
    }

    // ------------------------------------------------------------------
    // The value type
    // ------------------------------------------------------------------

    /**
     * The template value carries its size independently of the extents of the named areas, which is
     * exactly what makes unnamed ({@code .}) cells representable.
     */
    @Test
    void templateCarriesItsSizeIndependentlyOfTheNamedAreas() {
        GridTemplateArea area = new GridTemplateArea("a", 1, 2, 1, 2);
        TaffyGridTemplateAreas template = new TaffyGridTemplateAreas(List.of(area), 1, 2);

        assertEquals(List.of(area), template.getAreas());
        assertEquals(1, template.getRowCount());
        assertEquals(2, template.getColumnCount());
        assertEquals(new TaffyGridTemplateAreas(List.of(area), 1, 2), template);
        assertEquals(new TaffyGridTemplateAreas(List.of(area), 1, 2).hashCode(), template.hashCode());
        assertNotEquals(template, new TaffyGridTemplateAreas(List.of(area), 1, 3));
        assertNotEquals(template, new TaffyGridTemplateAreas(List.of(area), 2, 2));
        assertTrue(template.toString().contains("rows=1"), template.toString());
        assertTrue(template.toString().contains("columns=2"), template.toString());

        // Deriving from the extents is the legacy behavior, and is what an unset size uses.
        assertEquals(1, TaffyGridTemplateAreas.derivedRowCount(List.of(area)));
        assertEquals(1, TaffyGridTemplateAreas.derivedColumnCount(List.of(area)));
        assertEquals(new TaffyGridTemplateAreas(List.of(area), 1, 1), TaffyGridTemplateAreas.fromAreas(List.of(area)));
        // A non-positive area line contributes zero (upstream's row_end.max(1) - 1).
        assertEquals(0, TaffyGridTemplateAreas.derivedRowCount(List.of(new GridTemplateArea("z", 1, 0, 1, 0))));

        // "." cells: a template can have a size with no named area at all.
        TaffyGridTemplateAreas dots = new TaffyGridTemplateAreas(List.of(), 2, 3);
        assertEquals(2, dots.getRowCount());
        assertEquals(3, dots.getColumnCount());
        assertEquals(TaffyGridTemplateAreas.empty(), TaffyGridTemplateAreas.fromAreas(null));
        assertEquals(new TaffyGridTemplateAreas(null, 0, 0), TaffyGridTemplateAreas.empty());

        // The areas list is copied defensively and cannot be mutated through the value.
        List<GridTemplateArea> areas = new ArrayList<>(List.of(area));
        TaffyGridTemplateAreas copied = new TaffyGridTemplateAreas(areas, 1, 1);
        areas.add(new GridTemplateArea("b", 2, 3, 1, 2));
        assertEquals(1, copied.getAreas().size());
        assertThrows(UnsupportedOperationException.class, () -> copied.getAreas().add(area));

        assertThrows(IllegalArgumentException.class, () -> new TaffyGridTemplateAreas(List.of(), -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new TaffyGridTemplateAreas(List.of(), 1, -1));
        assertThrows(IllegalArgumentException.class,
            () -> new TaffyGridTemplateAreas(Arrays.asList((GridTemplateArea) null), 1, 1));
    }

    /**
     * The style keeps the legacy {@code List<GridTemplateArea>} field, and the additive template
     * size defaults to deriving from the named areas (pre-GR-16 numbers) until it is set.
     */
    @Test
    void styleDerivesTheTemplateSizeUntilItIsSet() {
        TaffyStyle style = new TaffyStyle();
        assertEquals(TaffyGridTemplateAreas.DERIVE, style.gridTemplateAreaRowCount);
        assertEquals(TaffyGridTemplateAreas.DERIVE, style.gridTemplateAreaColumnCount);
        assertEquals(0, style.getGridTemplateAreaRowCount());
        assertEquals(0, style.getGridTemplateAreaColumnCount());

        // Legacy path: areas set directly on the list, no template size set.
        style.gridTemplateAreas = List.of(
            new GridTemplateArea("wide", 1, 2, 3, 5),
            new GridTemplateArea("tall", 4, 6, 1, 2));
        assertEquals(5, style.getGridTemplateAreaRowCount());
        assertEquals(4, style.getGridTemplateAreaColumnCount());
        assertEquals(TaffyGridTemplateAreas.fromAreas(style.gridTemplateAreas), style.getGridTemplateAreaTemplate());

        // Additive path: the template size is explicit and wins over the extents.
        TaffyGridTemplateAreas template = new TaffyGridTemplateAreas(style.gridTemplateAreas, 7, 6);
        style.setGridTemplateAreaTemplate(template);
        assertEquals(7, style.getGridTemplateAreaRowCount());
        assertEquals(6, style.getGridTemplateAreaColumnCount());
        assertEquals(template, style.getGridTemplateAreaTemplate());
        assertEquals(template.getAreas(), style.gridTemplateAreas);

        // copy() keeps the size (and the areas).
        TaffyStyle copy = style.copy();
        assertEquals(7, copy.getGridTemplateAreaRowCount());
        assertEquals(6, copy.getGridTemplateAreaColumnCount());
        assertEquals(template.getAreas(), copy.gridTemplateAreas);
        assertEquals(template, copy.getGridTemplateAreaTemplate());

        // 0 is a real template size (an empty template), distinct from the DERIVE sentinel.
        style.setGridTemplateAreaTemplate(new TaffyGridTemplateAreas(style.gridTemplateAreas, 0, 0));
        assertEquals(0, style.gridTemplateAreaRowCount);
        assertEquals(0, style.getGridTemplateAreaRowCount());
        assertEquals(0, style.getGridTemplateAreaColumnCount());

        // null clears the template back to the initial value.
        style.setGridTemplateAreaTemplate(null);
        assertEquals(0, style.getGridTemplateAreaRowCount());
        assertEquals(0, style.getGridTemplateAreaColumnCount());
        assertTrue(style.gridTemplateAreas.isEmpty());
        assertEquals(TaffyGridTemplateAreas.empty(), style.getGridTemplateAreaTemplate());
    }

    /**
     * {@link NamedLineResolver} keeps deriving the area counts from the named areas (its public
     * behavior is unchanged); the grid computer takes the effective size from the style instead
     * (GR-16 / upstream #1024, where the resolver reads the size from the style).
     */
    @Test
    void resolverStillExposesTheDerivedAreaCounts() {
        TaffyStyle style = new TaffyStyle();
        style.gridTemplateAreas = List.of(
            new GridTemplateArea("wide", 1, 2, 3, 5),
            new GridTemplateArea("tall", 4, 6, 1, 2));

        NamedLineResolver resolver = new NamedLineResolver(style);
        assertEquals(4, resolver.getAreaColumnCount());
        assertEquals(5, resolver.getAreaRowCount());

        // The effective (style) size is the one the grid computer uses for the explicit grid.
        assertEquals(4, style.getGridTemplateAreaColumnCount());
        assertEquals(5, style.getGridTemplateAreaRowCount());

        style.setGridTemplateAreaTemplate(new TaffyGridTemplateAreas(style.gridTemplateAreas, 7, 8));
        assertEquals(8, style.getGridTemplateAreaColumnCount());
        assertEquals(7, style.getGridTemplateAreaRowCount());
    }

    /**
     * The area lines of a pathologically large template saturate at the limited grid instead of
     * overflowing (upstream {@code area_lines_saturate_when_converted_to_grid_lines}, which was
     * updated by #1024 to carry {@code row_count}/{@code column_count} on the template value).
     */
    @Test
    void areaLinesSaturateWhenConvertedToGridLines() {
        TaffyStyle style = new TaffyStyle();
        style.gridTemplateAreas = List.of(new GridTemplateArea("area", 1, 2, 65_535, 65_535));
        style.setGridTemplateAreaTemplate(new TaffyGridTemplateAreas(style.gridTemplateAreas, 1, 65_535));
        assertEquals(65_535, style.getGridTemplateAreaColumnCount());

        NamedLineResolver resolver = new NamedLineResolver(style);
        // OriginZero MAX_OZ_LINE (10000) converted back to CSS coordinates (explicit count 0).
        assertEquals(10_001, resolvedStart(resolver, GridPlacement.namedLine("area-start", 1), true));
        assertEquals(10_001, resolvedStart(resolver, GridPlacement.namedLine("area"), true));
    }

    // ------------------------------------------------------------------
    // Unnamed (".") cells
    // ------------------------------------------------------------------

    /**
     * {@code grid-template-areas: "a ."}: the unnamed second cell is still an explicit column, so an
     * auto-placed item flows into it instead of wrapping to the next row.
     * <p>Old behavior (derived size = the extents of "a" = 1 column): the second item wraps to
     * row 2. New behavior (template size 1x2): it is placed in column 2 of row 1.</p>
     */
    @Test
    void unnamedCellsExtendTheExplicitColumns() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = gridContainer();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(100f));
        rootStyle.gap = new TaffySize<>(LengthPercentage.length(10f), LengthPercentage.length(0f));
        rootStyle.gridAutoColumns = List.of(px(20f));
        rootStyle.gridAutoRows = List.of(px(10f));
        rootStyle.setGridTemplateAreaTemplate(new TaffyGridTemplateAreas(
            List.of(new GridTemplateArea("a", 1, 2, 1, 2)), 1, 2));
        NodeId root = tree.newLeaf(rootStyle);

        NodeId inArea = addAreaItem(tree, root, "a");
        NodeId auto = addItem(tree, root, autoLine(), autoLine());

        tree.computeLayout(root, TaffySize.maxContent());

        assertRect(tree, inArea, 0f, 0f, 20f, 10f);
        assertRect(tree, auto, 30f, 0f, 20f, 10f);

        // Old behavior: with the size derived from the named area there is only one explicit column.
        TaffyTree legacyTree = new TaffyTree();
        TaffyStyle legacyStyle = gridContainer();
        legacyStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(100f));
        legacyStyle.gap = new TaffySize<>(LengthPercentage.length(10f), LengthPercentage.length(0f));
        legacyStyle.gridAutoColumns = List.of(px(20f));
        legacyStyle.gridAutoRows = List.of(px(10f));
        legacyStyle.gridTemplateAreas = List.of(new GridTemplateArea("a", 1, 2, 1, 2));
        NodeId legacyRoot = legacyTree.newLeaf(legacyStyle);
        addAreaItem(legacyTree, legacyRoot, "a");
        NodeId legacyAuto = addItem(legacyTree, legacyRoot, autoLine(), autoLine());
        legacyTree.computeLayout(legacyRoot, TaffySize.maxContent());
        assertRect(legacyTree, legacyAuto, 0f, 10f, 20f, 10f);
    }

    /**
     * {@code grid-template-areas: "a" "."}: the unnamed second row is an explicit row, so the
     * container's content height includes it even when no item is placed there, and the negative
     * line {@code -1} is its last line.
     * <p>Old behavior (derived size = 1 row): the container is one row tall and {@code -1} is row 1.</p>
     */
    @Test
    void unnamedCellsExtendTheExplicitRows() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = gridContainer();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.auto());
        rootStyle.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(10f));
        rootStyle.gridAutoColumns = List.of(px(10f));
        rootStyle.gridAutoRows = List.of(px(20f));
        rootStyle.setGridTemplateAreaTemplate(new TaffyGridTemplateAreas(
            List.of(new GridTemplateArea("a", 1, 2, 1, 2)), 2, 1));
        NodeId root = tree.newLeaf(rootStyle);

        NodeId inArea = addAreaItem(tree, root, "a");
        // grid-row: -2 (the second to last line of the explicit grid) => the second explicit row.
        NodeId lastLine = addItem(tree, root,
            place(GridPlacement.line(1), GridPlacement.line(1)),
            place(GridPlacement.line(-2), GridPlacement.auto()));

        tree.computeLayout(root, TaffySize.maxContent());

        assertRect(tree, inArea, 0f, 0f, 10f, 20f);
        assertRect(tree, lastLine, 0f, 30f, 10f, 20f);
        assertEquals(50f, tree.getLayout(root).size().height, EPSILON, "two explicit rows and one gap");

        // Old behavior: with the size derived from the named area there is only one explicit row.
        TaffyTree legacyTree = new TaffyTree();
        TaffyStyle legacyStyle = gridContainer();
        legacyStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.auto());
        legacyStyle.gap = new TaffySize<>(LengthPercentage.length(0f), LengthPercentage.length(10f));
        legacyStyle.gridAutoColumns = List.of(px(10f));
        legacyStyle.gridAutoRows = List.of(px(20f));
        legacyStyle.gridTemplateAreas = List.of(new GridTemplateArea("a", 1, 2, 1, 2));
        NodeId legacyRoot = legacyTree.newLeaf(legacyStyle);
        addAreaItem(legacyTree, legacyRoot, "a");
        NodeId legacyLastLine = addItem(legacyTree, legacyRoot,
            place(GridPlacement.line(1), GridPlacement.line(1)),
            place(GridPlacement.line(-2), GridPlacement.auto()));
        legacyTree.computeLayout(legacyRoot, TaffySize.maxContent());
        // One explicit row, so line -2 is its first line.
        assertRect(legacyTree, legacyLastLine, 0f, 0f, 10f, 20f);
        assertEquals(20f, legacyTree.getLayout(legacyRoot).size().height, EPSILON);
    }

    /**
     * A template made only of unnamed cells ({@code ". . ." / ". . ."}) is still a 2x3 explicit
     * grid: auto-placed items flow through its columns and wrap at its column count.
     */
    @Test
    void templateSizeWithoutNamedAreasCreatesExplicitTracks() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = gridContainer();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.auto());
        rootStyle.gridAutoColumns = List.of(px(20f));
        rootStyle.gridAutoRows = List.of(px(10f));
        rootStyle.setGridTemplateAreaTemplate(new TaffyGridTemplateAreas(List.of(), 2, 3));
        NodeId root = tree.newLeaf(rootStyle);

        NodeId first = addItem(tree, root, autoLine(), autoLine());
        NodeId second = addItem(tree, root, autoLine(), autoLine());
        NodeId third = addItem(tree, root, autoLine(), autoLine());
        NodeId fourth = addItem(tree, root, autoLine(), autoLine());

        tree.computeLayout(root, TaffySize.maxContent());

        assertRect(tree, first, 0f, 0f, 20f, 10f);
        assertRect(tree, second, 20f, 0f, 20f, 10f);
        assertRect(tree, third, 40f, 0f, 20f, 10f);
        assertRect(tree, fourth, 0f, 10f, 20f, 10f);
        assertEquals(20f, tree.getLayout(root).size().height, EPSILON, "two explicit rows");
    }

    /**
     * The explicit template size is authoritative: upstream keeps it on the style rather than
     * deriving it from the areas, so an explicitly smaller size is honored even when an area's
     * edges lie outside the explicit grid (upstream's
     * {@code area_lines_saturate_when_converted_to_grid_lines} sets {@code row_count: 1} for an area
     * with {@code row_end: 2}).
     * <p>Old behavior (derived size): the unoccupied second row would still be created.</p>
     */
    @Test
    void explicitTemplateSizeWinsOverTheAreaExtents() {
        TaffyStyle style = gridContainer();
        style.gridTemplateAreas = List.of(new GridTemplateArea("a", 1, 3, 1, 2));
        style.setGridTemplateAreaTemplate(new TaffyGridTemplateAreas(style.gridTemplateAreas, 1, 1));
        assertEquals(1, style.getGridTemplateAreaRowCount(), "the explicit size, not the area extents (2)");

        TaffyTree tree = new TaffyTree();
        style.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.auto());
        style.gridAutoRows = List.of(px(20f));
        NodeId root = tree.newLeaf(style);
        addItem(tree, root, autoLine(), autoLine());
        tree.computeLayout(root, TaffySize.maxContent());
        assertEquals(20f, tree.getLayout(root).size().height, EPSILON,
            "only the one explicit row exists; the area's extra row edge is implicit");

        // Old behavior: the derived size would create both rows.
        TaffyTree legacyTree = new TaffyTree();
        TaffyStyle legacyStyle = gridContainer();
        legacyStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.auto());
        legacyStyle.gridAutoRows = List.of(px(20f));
        legacyStyle.gridTemplateAreas = List.of(new GridTemplateArea("a", 1, 3, 1, 2));
        NodeId legacyRoot = legacyTree.newLeaf(legacyStyle);
        addItem(legacyTree, legacyRoot, autoLine(), autoLine());
        legacyTree.computeLayout(legacyRoot, TaffySize.maxContent());
        assertEquals(40f, legacyTree.getLayout(legacyRoot).size().height, EPSILON);
    }

    // ------------------------------------------------------------------
    // Areas extending past the track template
    // ------------------------------------------------------------------

    /**
     * An area whose extent is past the track template still extends the explicit grid (upstream
     * {@code explicit_col_count = max(template_count, area_column_count)}), which the Java port
     * previously applied only to named-line numbering, not to placement.
     * <p>Old behavior: the explicit grid stayed at the 2 template columns, so the second auto-placed
     * item wrapped to row 2. New behavior: the grid has 4 explicit columns and both items fit in
     * row 1.</p>
     */
    @Test
    void areaBeyondTheTrackTemplateExtendsTheExplicitGrid() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = gridContainer();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(100f));
        rootStyle.gridTemplateColumns = List.of(px(20f), px(20f));
        rootStyle.gridTemplateRows = List.of(px(10f));
        rootStyle.gridAutoColumns = List.of(px(10f));
        rootStyle.gridAutoRows = List.of(px(10f));
        // grid-template-areas: a two column area spanning columns 3..4, i.e. past the 2 track template.
        rootStyle.gridTemplateAreas = List.of(new GridTemplateArea("wide", 1, 2, 3, 5));
        NodeId root = tree.newLeaf(rootStyle);

        NodeId first = addItem(tree, root, autoLine(), autoLine());
        NodeId second = addItem(tree, root, autoLine(), autoLine());
        // grid-column: -2 => the second to last line of the (area-extended) explicit grid, i.e.
        // column 4. (Line -1 is the end line, i.e. past the explicit grid.)
        NodeId lastLine = addItem(tree, root,
            place(GridPlacement.line(-2), GridPlacement.auto()), autoLine());

        tree.computeLayout(root, TaffySize.maxContent());

        assertRect(tree, first, 0f, 0f, 20f, 10f);
        assertRect(tree, second, 20f, 0f, 20f, 10f);
        assertRect(tree, lastLine, 50f, 0f, 10f, 10f);
    }

    /**
     * The named-line resolver and the explicit grid agree on the area-extended explicit count:
     * area edges resolve to their own lines and negative line numbers count from the end of the
     * extended explicit grid.
     */
    @Test
    void areaBeyondTheTrackTemplateAlsoExtendsTheNamedLineGrid() {
        TaffyStyle style = gridContainer();
        style.gridTemplateColumns = List.of(px(20f), px(20f));
        style.gridTemplateAreas = List.of(new GridTemplateArea("wide", 1, 2, 3, 5));
        NamedLineResolver resolver = new NamedLineResolver(style);
        resolver.setExplicitColumnCount(4);
        resolver.setExplicitRowCount(1);

        assertEquals(3, resolvedStart(resolver, GridPlacement.namedLine("wide"), true));
        assertEquals(5, resolvedEnd(resolver, GridPlacement.namedLine("wide"), true));
        // The explicit grid now has 4 columns (2 template + 2 from the area template).
        assertEquals(4, resolver.getExplicitColumnCount());
    }

    // ------------------------------------------------------------------
    // Area geometry
    // ------------------------------------------------------------------

    /** An area spanning multiple rows and columns resolves to both of its edges, gaps included. */
    @Test
    void areaSpansCrossRowsAndColumns() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = gridContainer();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(100f));
        rootStyle.gap = new TaffySize<>(LengthPercentage.length(5f), LengthPercentage.length(5f));
        rootStyle.gridTemplateColumns = List.of(px(20f), px(30f), px(40f));
        rootStyle.gridTemplateRows = List.of(px(10f), px(15f), px(20f));
        // "big" spans rows 1..2 and columns 1..2.
        rootStyle.gridTemplateAreas = List.of(new GridTemplateArea("big", 1, 3, 1, 3));
        NodeId root = tree.newLeaf(rootStyle);

        NodeId item = addAreaItem(tree, root, "big");
        tree.computeLayout(root, TaffySize.maxContent());

        assertRect(tree, item, 0f, 0f, 55f, 30f);
        // The template size is still derived from the area extents (2x2), unchanged by GR-16.
        assertEquals(2, rootStyle.getGridTemplateAreaRowCount());
        assertEquals(2, rootStyle.getGridTemplateAreaColumnCount());
    }

    /**
     * Two areas with the same name: upstream inserts areas into a name-keyed map, so the last
     * definition wins for the area's edges, while the template extents (and therefore the derived
     * template size) still cover both definitions.
     */
    @Test
    void duplicateAreaNamesUseTheLastDefinition() {
        TaffyStyle style = gridContainer();
        style.gridTemplateAreas = List.of(
            new GridTemplateArea("a", 1, 2, 1, 2),
            new GridTemplateArea("a", 1, 2, 3, 5));
        NamedLineResolver resolver = new NamedLineResolver(style);
        resolver.setExplicitColumnCount(4);
        resolver.setExplicitRowCount(1);

        assertEquals(3, resolvedStart(resolver, GridPlacement.namedLine("a"), true));
        assertEquals(5, resolvedEnd(resolver, GridPlacement.namedLine("a"), true));
        assertEquals(4, style.getGridTemplateAreaColumnCount());
        assertEquals(1, style.getGridTemplateAreaRowCount());

        TaffyTree tree = new TaffyTree();
        style.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(10f));
        style.gridTemplateColumns = List.of(px(10f), px(10f), px(10f), px(10f));
        style.gridTemplateRows = List.of(px(10f));
        NodeId root = tree.newLeaf(style);

        NodeId first = addAreaItem(tree, root, "a");
        NodeId second = addAreaItem(tree, root, "a");
        tree.computeLayout(root, TaffySize.maxContent());

        // Both items resolve to the last definition: columns 3..4.
        assertRect(tree, first, 20f, 0f, 20f, 10f);
        assertRect(tree, second, 20f, 0f, 20f, 10f);
    }

    /**
     * Legacy regression: {@code grid-template-areas} within the track template keeps the derived
     * template size and the historical layout numbers when no explicit size is set.
     */
    @Test
    void legacyAreaListKeepsTheDerivedTemplateSizeAndLayout() {
        TaffyStyle rootStyle = gridContainer();
        rootStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(300f));
        rootStyle.gridTemplateColumns = List.of(px(100f), px(100f), px(100f));
        rootStyle.gridTemplateRows = List.of(px(100f), px(100f), px(100f));
        rootStyle.gridTemplateAreas = List.of(
            new GridTemplateArea("header", 1, 2, 1, 4),
            new GridTemplateArea("sidebar", 2, 3, 1, 2),
            new GridTemplateArea("content", 2, 3, 2, 4),
            new GridTemplateArea("footer", 3, 4, 1, 4));

        // Derived size = extents, and no explicit size is set.
        assertEquals(TaffyGridTemplateAreas.DERIVE, rootStyle.gridTemplateAreaRowCount);
        assertEquals(3, rootStyle.getGridTemplateAreaRowCount());
        assertEquals(3, rootStyle.getGridTemplateAreaColumnCount());

        TaffyTree tree = new TaffyTree();
        NodeId root = tree.newLeaf(rootStyle);
        NodeId header = addAreaItem(tree, root, "header");
        NodeId sidebar = addAreaItem(tree, root, "sidebar");
        NodeId content = addAreaItem(tree, root, "content");
        NodeId footer = addAreaItem(tree, root, "footer");

        tree.computeLayout(root, new TaffySize<>(
            AvailableSpace.definite(300f), AvailableSpace.definite(300f)));

        assertRect(tree, header, 0f, 0f, 300f, 100f);
        assertRect(tree, sidebar, 0f, 100f, 100f, 100f);
        assertRect(tree, content, 100f, 100f, 200f, 100f);
        assertRect(tree, footer, 0f, 200f, 300f, 100f);
    }
}
