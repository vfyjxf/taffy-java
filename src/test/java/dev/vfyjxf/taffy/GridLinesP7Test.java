package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.GridRepetition;
import dev.vfyjxf.taffy.style.GridTemplateArea;
import dev.vfyjxf.taffy.style.GridTemplateComponent;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.NamedGridLine;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.tree.grid.NamedLineResolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P7 GR-17 + GR-15: {@code repeat()} line names and named-line resolution semantics.
 *
 * <p>Covers two upstream alignment items:</p>
 * <ul>
 *   <li><b>GR-17 / #1035 + N3</b>: a {@code repeat()} carries positional line names (empty or
 *       {@code track_count + 1} sets) that are expanded into named lines for every repetition, and
 *       named-line numbering is clamped into the limited grid (the old numbering could underflow
 *       into nonsensical line numbers).</li>
 *   <li><b>GR-15 / #1258 + #1264 + #1259</b>: the resolver works in OriginZero coordinates with
 *       clamping, keeps grid-area edges in their own table, prefers a named-area edge for a bare
 *       {@code <custom-ident>}, and resolves named lines exactly once before the grid size
 *       estimate (shared by the in-flow and absolutely positioned paths).</li>
 * </ul>
 *
 * <p>The layout assertions reproduce the expectations of the upstream fixtures
 * {@code grid_placement_named_area_edge_priority}, {@code grid_placement_named_span_backwards},
 * {@code grid_placement_named_span_insufficient_lines} and
 * {@code grid_placement_named_line_negative_index} (border-box ltr variants); the resolver-level
 * assertions mirror the unit tests of upstream {@code named.rs}.</p>
 */
class GridLinesP7Test {

    private static final float EPSILON = 0.1f;

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static TrackSizingFunction px(float value) {
        return TrackSizingFunction.fixed(LengthPercentage.length(value));
    }

    private static TaffyStyle gridContainer(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyLine<GridPlacement> place(GridPlacement start, GridPlacement end) {
        return new TaffyLine<>(start, end);
    }

    private static TaffyLine<GridPlacement> column(GridPlacement start) {
        return place(start, GridPlacement.auto());
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

    private static void assertRect(TaffyTree tree, NodeId node,
                                   float x, float y, float width, float height) {
        assertEquals(x, tree.getLayout(node).location().x, EPSILON, "x");
        assertEquals(y, tree.getLayout(node).location().y, EPSILON, "y");
        assertEquals(width, tree.getLayout(node).size().width, EPSILON, "width");
        assertEquals(height, tree.getLayout(node).size().height, EPSILON, "height");
    }

    private static NamedLineResolver resolver(TaffyStyle style, int explicitColumns, int explicitRows) {
        NamedLineResolver resolver = new NamedLineResolver(style);
        resolver.setExplicitColumnCount(explicitColumns);
        resolver.setExplicitRowCount(explicitRows);
        return resolver;
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

    private static List<List<String>> nameSets(String... names) {
        List<List<String>> sets = new java.util.ArrayList<>();
        for (String name : names) {
            sets.add(name.isEmpty() ? List.of() : List.of(name));
        }
        return sets;
    }

    // ------------------------------------------------------------------
    // GR-17: repeat() line names
    // ------------------------------------------------------------------

    /**
     * {@code grid-template-columns: 100px repeat(3, [a] 50px [b])}. The repetition's two name sets
     * are positional: {@code [a]} names the line before each repeated track and {@code [b]} the line
     * after it, so the shared boundary line of two repetitions belongs to both {@code b} and
     * {@code a}: {@code a} = lines 2,3,4 and {@code b} = lines 3,4,5.
     */
    @Test
    void repeatLineNamesNameEveryRepetition() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = gridContainer(250f, 100f);
        rootStyle.gridTemplateRows = List.of(px(100f));
        rootStyle.gridTemplateColumnsWithRepeat = List.of(
            GridTemplateComponent.single(px(100f)),
            GridTemplateComponent.repeat(GridRepetition.count(3, List.of(px(50f)),
                nameSets("a", "b"))));
        NodeId root = tree.newLeaf(rootStyle);

        // 1st "a" (line 2) .. 2nd "a" (line 3) => the first repeated track.
        NodeId firstRepeatedTrack = addItem(tree, root,
            place(GridPlacement.namedLine("a", 1), GridPlacement.namedLine("a", 2)),
            place(GridPlacement.line(1), GridPlacement.line(2)));
        // 3rd "b" is line 5, which is the end edge of the explicit grid.
        NodeId afterLastRepeatedTrack = addItem(tree, root,
            column(GridPlacement.namedLine("b", 3)),
            place(GridPlacement.line(1), GridPlacement.line(2)));

        tree.computeLayout(root, TaffySize.maxContent());

        assertRect(tree, firstRepeatedTrack, 100f, 0f, 50f, 100f);
        assertEquals(250f, tree.getLayout(afterLastRepeatedTrack).location().x, EPSILON,
            "the last \"b\" line is the end edge of the 4-track explicit grid");
    }

    /** The positional name sets of a repetition must collapse onto the shared repetition boundary. */
    @Test
    void repeatLineNamesCollapseAtRepetitionBoundaries() {
        TaffyStyle style = new TaffyStyle();
        style.gridTemplateColumnsWithRepeat = List.of(
            GridTemplateComponent.single(px(100f)),
            GridTemplateComponent.repeat(GridRepetition.count(3, List.of(px(50f)),
                nameSets("a", "b"))));
        NamedLineResolver resolver = resolver(style, 4, 1);

        assertEquals(2, resolvedStart(resolver, GridPlacement.namedLine("a", 1), true));
        assertEquals(3, resolvedStart(resolver, GridPlacement.namedLine("a", 2), true));
        assertEquals(4, resolvedStart(resolver, GridPlacement.namedLine("a", 3), true));
        // The 3rd repetition's end line already merged with the following line; the 4th "a" is the
        // second implicit line after the explicit grid (OCR fallback).
        assertEquals(6, resolvedStart(resolver, GridPlacement.namedLine("a", 4), true));
        assertEquals(3, resolvedStart(resolver, GridPlacement.namedLine("b", 1), true));
        assertEquals(4, resolvedStart(resolver, GridPlacement.namedLine("b", 2), true));
        assertEquals(5, resolvedStart(resolver, GridPlacement.namedLine("b", 3), true));
    }

    /**
     * The line names of an {@code auto-fill} repetition are generated once per expanded repetition,
     * using the same repetition count as the track expansion.
     */
    @Test
    void autoFillRepeatLineNamesFollowTheExpandedRepetitionCount() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = gridContainer(250f, 100f);
        rootStyle.gridTemplateRows = List.of(px(100f));
        rootStyle.gridTemplateColumnsWithRepeat = List.of(
            GridTemplateComponent.repeat(GridRepetition.autoFill(List.of(px(50f)),
                nameSets("x", "y"))));
        NodeId root = tree.newLeaf(rootStyle);

        // 250px / 50px = 5 repetitions, so "x" = lines 1..5 and "y" = lines 2..6.
        NodeId fifthX = addItem(tree, root,
            column(GridPlacement.namedLine("x", 5)),
            place(GridPlacement.line(1), GridPlacement.line(2)));
        NodeId fifthY = addItem(tree, root,
            column(GridPlacement.namedLine("y", 5)),
            place(GridPlacement.line(1), GridPlacement.line(2)));

        tree.computeLayout(root, TaffySize.maxContent());

        assertRect(tree, fifthX, 200f, 0f, 50f, 100f);
        assertEquals(250f, tree.getLayout(fifthY).location().x, EPSILON,
            "the 5th \"y\" line is the end edge of the 5-repetition explicit grid");
    }

    /** The auto-repetition line names use the same count as the track expansion (not a fixed 1). */
    @Test
    void autoFillRepeatLineNamesRequireTheExpandedRepetitionCount() {
        TaffyStyle style = new TaffyStyle();
        style.gridTemplateColumnsWithRepeat = List.of(
            GridTemplateComponent.repeat(GridRepetition.autoFill(List.of(px(50f)),
                nameSets("x", "y"))));

        // Without the expanded count the resolver only sees a single repetition.
        NamedLineResolver singleRepetition = resolver(style, 5, 1);
        assertEquals(2, resolvedStart(singleRepetition, GridPlacement.namedLine("y", 1), true));
        assertEquals(7, resolvedStart(singleRepetition, GridPlacement.namedLine("y", 2), true));

        // With 5 expanded repetitions "y" has one line per repetition plus the initial line.
        NamedLineResolver fiveRepetitions = new NamedLineResolver(style, 5, 0);
        fiveRepetitions.setExplicitColumnCount(5);
        fiveRepetitions.setExplicitRowCount(1);
        assertEquals(6, resolvedStart(fiveRepetitions, GridPlacement.namedLine("y", 5), true));
    }

    /** Line names must be carried by a repetition in the row axis too. */
    @Test
    void repeatLineNamesInRowAxis() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = gridContainer(100f, 70f);
        rootStyle.gridTemplateColumns = List.of(px(100f));
        rootStyle.gridTemplateRowsWithRepeat = List.of(
            GridTemplateComponent.single(px(10f)),
            GridTemplateComponent.repeat(GridRepetition.count(2, List.of(px(30f)),
                nameSets("r", "s"))));
        NodeId root = tree.newLeaf(rootStyle);

        // "s" = rows 3,4 and "r" = rows 2,3.
        NodeId secondRepeatedRow = addItem(tree, root,
            place(GridPlacement.line(1), GridPlacement.line(2)),
            place(GridPlacement.namedLine("s", 1), GridPlacement.namedLine("r", 2)));

        tree.computeLayout(root, TaffySize.maxContent());

        assertRect(tree, secondRepeatedRow, 0f, 40f, 100f, 30f);
    }

    /** {@code lineNames} is empty or exactly {@code track_count + 1} sets long (API contract). */
    @Test
    void repeatLineNamesApiContract() {
        assertThrows(IllegalArgumentException.class, () ->
            GridRepetition.count(3, List.of(px(50f)), nameSets("a")));
        assertThrows(IllegalArgumentException.class, () ->
            GridRepetition.autoFill(List.of(px(50f), px(50f)), nameSets("a", "b")));
        assertThrows(IllegalArgumentException.class, () ->
            GridRepetition.autoFit(List.of(px(50f)), List.of(List.of("a"), List.of("b"), List.of("c"))));

        GridRepetition unnamed = GridRepetition.count(2, List.of(px(10f), px(20f)));
        assertFalse(unnamed.hasLineNames());
        assertTrue(unnamed.getLineNames().isEmpty());

        GridRepetition named = GridRepetition.count(2, List.of(px(10f), px(20f)),
            nameSets("p", "q", "r"));
        assertTrue(named.hasLineNames());
        assertEquals(3, named.getLineNames().size());
        assertEquals(List.of("q"), named.getLineNames().get(1));

        GridRepetition withNames = unnamed.withLineNames(nameSets("s", "", ""));
        assertTrue(withNames.hasLineNames());
        assertEquals(3, withNames.getLineNames().size());
        assertFalse(withNames.withLineNames(List.of()).hasLineNames());
        assertEquals(unnamed, unnamed.withLineNames(List.of()));
        assertFalse(unnamed.equals(named));
    }

    // ------------------------------------------------------------------
    // GR-15: named line / named area edge resolution
    // ------------------------------------------------------------------

    /**
     * Equivalent of {@code grid_placement_named_area_edge_priority}: a bare custom-ident matches the
     * {@code -start}/{@code -end} edge of an implicitly named area before a line with that exact
     * name, while an explicit integer index only matches lines with that exact name.
     */
    @Test
    void bareIdentPrefersNamedAreaEdge() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = gridContainer(350f, 140f);
        rootStyle.gridTemplateColumns = List.of(px(50f), px(100f), px(200f));
        rootStyle.gridTemplateRows = List.of(px(20f), px(40f), px(80f));
        rootStyle.gridTemplateColumnNames = List.of(
            new NamedGridLine("a", 1),
            new NamedGridLine("a-start", 2),
            new NamedGridLine("a-end", 3),
            new NamedGridLine("a", 4));
        rootStyle.gridTemplateRowNames = List.of(
            new NamedGridLine("a", 1),
            new NamedGridLine("a-start", 2),
            new NamedGridLine("a-end", 3),
            new NamedGridLine("a", 4));
        NodeId root = tree.newLeaf(rootStyle);

        // grid-column: a; grid-row: a  => the area edges (lines 2 and 3), NOT the lines named "a".
        NodeId bare = addItem(tree, root,
            place(GridPlacement.namedLine("a"), GridPlacement.namedLine("a")),
            place(GridPlacement.namedLine("a"), GridPlacement.namedLine("a")));
        // grid-column: 1 a / 2 a; grid-row: 1 a / 2 a => the first and second lines named "a".
        NodeId indexed = addItem(tree, root,
            place(GridPlacement.namedLine("a", 1), GridPlacement.namedLine("a", 2)),
            place(GridPlacement.namedLine("a", 1), GridPlacement.namedLine("a", 2)));
        // grid-column: a / span a; grid-row: span a / a => area start edge to the next "a" line.
        NodeId spans = addItem(tree, root,
            place(GridPlacement.namedLine("a"), GridPlacement.namedSpan("a", 1)),
            place(GridPlacement.namedSpan("a", 1), GridPlacement.namedLine("a")));

        tree.computeLayout(root, TaffySize.maxContent());

        assertRect(tree, bare, 50f, 20f, 100f, 40f);
        assertRect(tree, indexed, 0f, 0f, 350f, 140f);
        assertRect(tree, spans, 50f, 0f, 300f, 60f);
    }

    /** Area edges are also addressable through their explicit {@code <area>-start/-end} names. */
    @Test
    void areaEdgeIsTheFirstLineWithTheEdgeName() {
        TaffyStyle style = new TaffyStyle();
        style.gridTemplateAreas = List.of(new GridTemplateArea("area", 1, 2, 2, 4));
        NamedLineResolver resolver = resolver(style, 4, 1);

        // A bare ident resolves to the area edge.
        assertEquals(2, resolvedStart(resolver, GridPlacement.namedLine("area"), true));
        assertEquals(4, resolvedEnd(resolver, GridPlacement.namedLine("area"), true));

        // The area edge is also the line named "area-start" / "area-end".
        assertEquals(2, resolvedStart(resolver, GridPlacement.namedLine("area-start", 1), true));
        assertEquals(4, resolvedStart(resolver, GridPlacement.namedLine("area-end", 1), true));
        assertEquals(4, resolvedStart(resolver, GridPlacement.namedLine("area-end", -1), true));
        // There is only one such line, so the second occurrence is the first implicit line after the
        // explicit grid.
        assertEquals(6, resolvedStart(resolver, GridPlacement.namedLine("area-start", 2), true));

        // Rows: the same area spans row lines 1 and 2.
        assertEquals(1, resolvedStart(resolver, GridPlacement.namedLine("area"), false));
        assertEquals(2, resolvedEnd(resolver, GridPlacement.namedLine("area"), false));
    }

    /** Explicitly named {@code <area>-start/-end} lines are merged with the area's edge lines. */
    @Test
    void areaEdgesMergeWithExplicitlyNamedLines() {
        TaffyStyle style = new TaffyStyle();
        style.gridTemplateAreas = List.of(new GridTemplateArea("area", 1, 2, 2, 4));
        style.gridTemplateColumnNames = List.of(
            new NamedGridLine("area-start", 1),
            new NamedGridLine("area-start", 3),
            new NamedGridLine("area-end", 4),
            new NamedGridLine("area-end", 5));
        NamedLineResolver resolver = resolver(style, 4, 1);

        // The merged line list of "area-start" is {1, 2 (area edge), 3}.
        assertEquals(1, resolvedStart(resolver, GridPlacement.namedLine("area-start", 1), true));
        assertEquals(2, resolvedStart(resolver, GridPlacement.namedLine("area-start", 2), true));
        assertEquals(3, resolvedStart(resolver, GridPlacement.namedLine("area-start", 3), true));
        assertEquals(3, resolvedStart(resolver, GridPlacement.namedLine("area-start", -1), true));
        assertEquals(6, resolvedStart(resolver, GridPlacement.namedLine("area-start", 4), true));
        assertEquals(4, resolvedStart(resolver, GridPlacement.namedLine("area-end", 1), true));
        assertEquals(5, resolvedStart(resolver, GridPlacement.namedLine("area-end", 2), true));
        assertEquals(6, resolvedStart(resolver, GridPlacement.namedLine("area-end", 3), true));

        // A bare ident matches the first line of the edge, which is now line 1 / line 4.
        assertEquals(1, resolvedStart(resolver, GridPlacement.namedLine("area"), true));
        assertEquals(4, resolvedEnd(resolver, GridPlacement.namedLine("area"), true));

        // A named span anchored on a definite line counts the merged lines forwards.
        TaffyLine<GridPlacement> span = resolver.resolveColumnNames(
            place(GridPlacement.line(1), GridPlacement.namedSpan("area-start", 2)));
        assertEquals(3, span.end.getLineNumber());
    }

    /** Area edges are only matched by a bare {@code <custom-ident>} (and their explicit names). */
    @Test
    void areaEdgesAreOnlyMatchedByBareIdents() {
        TaffyStyle style = new TaffyStyle();
        style.gridTemplateAreas = List.of(new GridTemplateArea("area", 1, 2, 2, 4));
        NamedLineResolver resolver = resolver(style, 4, 1);

        // There are no lines named "area", so the indexed form falls back to implicit lines after
        // the explicit grid.
        assertEquals(6, resolvedStart(resolver, GridPlacement.namedLine("area", 1), true));
        assertEquals(7, resolvedStart(resolver, GridPlacement.namedLine("area", 2), true));

        // A named span also only counts lines with that exact name.
        TaffyLine<GridPlacement> span = resolver.resolveColumnNames(
            place(GridPlacement.line(1), GridPlacement.namedSpan("area", 1)));
        assertEquals(6, span.end.getLineNumber());
    }

    /**
     * Equivalent of {@code grid_placement_named_span_backwards}: a named span whose end line is
     * definite counts the named lines backwards from that line.
     */
    @Test
    void namedSpanCountsBackwardsFromTheEndLine() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = gridContainer(350f, 140f);
        rootStyle.gridTemplateColumns = List.of(px(50f), px(100f), px(200f));
        rootStyle.gridTemplateRows = List.of(px(20f), px(40f), px(80f));
        rootStyle.gridTemplateColumnNames = List.of(
            new NamedGridLine("col", 1),
            new NamedGridLine("col", 2),
            new NamedGridLine("mid", 2),
            new NamedGridLine("col", 3),
            new NamedGridLine("col", 4));
        rootStyle.gridTemplateRowNames = List.of(
            new NamedGridLine("row", 1),
            new NamedGridLine("row", 2),
            new NamedGridLine("mid", 2),
            new NamedGridLine("row", 3),
            new NamedGridLine("row", 4));
        NodeId root = tree.newLeaf(rootStyle);

        NodeId first = addItem(tree, root,
            place(GridPlacement.namedSpan("col", 1), GridPlacement.line(3)),
            place(GridPlacement.namedSpan("row", 1), GridPlacement.line(4)));
        NodeId second = addItem(tree, root,
            place(GridPlacement.namedSpan("col", 2), GridPlacement.line(3)),
            place(GridPlacement.namedSpan("row", 2), GridPlacement.line(4)));
        NodeId third = addItem(tree, root,
            place(GridPlacement.namedSpan("col", 3), GridPlacement.line(4)),
            place(GridPlacement.namedSpan("row", 3), GridPlacement.line(4)));
        NodeId fourth = addItem(tree, root,
            place(GridPlacement.namedSpan("mid", 1), GridPlacement.line(-1)),
            place(GridPlacement.namedSpan("mid", 1), GridPlacement.line(-1)));

        tree.computeLayout(root, TaffySize.maxContent());

        assertRect(tree, first, 50f, 60f, 100f, 80f);
        assertRect(tree, second, 0f, 20f, 150f, 120f);
        assertRect(tree, third, 0f, 0f, 350f, 140f);
        assertRect(tree, fourth, 50f, 20f, 300f, 120f);
    }

    /**
     * Equivalent of {@code grid_placement_named_span_insufficient_lines}: when there are fewer
     * matching named lines than the span count, the remaining implicit lines in the direction of the
     * span are counted.
     */
    @Test
    void namedSpanInsufficientLinesContinueIntoImplicitLines() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = gridContainer(370f, 200f);
        rootStyle.gridTemplateColumns = List.of(px(50f), px(100f), px(200f));
        rootStyle.gridTemplateRows = List.of(px(20f), px(40f), px(80f));
        rootStyle.gridAutoColumns = List.of(px(5f));
        rootStyle.gridAutoRows = List.of(px(10f));
        rootStyle.gridTemplateColumnNames = List.of(
            new NamedGridLine("col", 1),
            new NamedGridLine("col", 2),
            new NamedGridLine("mid", 2),
            new NamedGridLine("col", 3),
            new NamedGridLine("col", 4));
        rootStyle.gridTemplateRowNames = List.of(
            new NamedGridLine("row", 1),
            new NamedGridLine("row", 2),
            new NamedGridLine("mid", 2),
            new NamedGridLine("row", 3),
            new NamedGridLine("row", 4));
        NodeId root = tree.newLeaf(rootStyle);

        NodeId first = addItem(tree, root,
            place(GridPlacement.namedSpan("mid", 2), GridPlacement.line(4)),
            place(GridPlacement.namedSpan("mid", 3), GridPlacement.line(4)));
        NodeId second = addItem(tree, root,
            place(GridPlacement.line(1), GridPlacement.namedSpan("mid", 2)),
            place(GridPlacement.line(1), GridPlacement.namedSpan("mid", 3)));
        NodeId third = addItem(tree, root,
            place(GridPlacement.namedSpan("foo", 1), GridPlacement.line(2)),
            place(GridPlacement.namedSpan("foo", 2), GridPlacement.line(2)));
        NodeId fourth = addItem(tree, root,
            place(GridPlacement.line(3), GridPlacement.namedSpan("foo", 1)),
            place(GridPlacement.line(3), GridPlacement.namedSpan("foo", 2)));
        NodeId fifth = addItem(tree, root,
            place(GridPlacement.namedLine("foo"), GridPlacement.namedSpan("foo", 2)),
            place(GridPlacement.namedLine("foo"), GridPlacement.namedSpan("mid", 3)));

        tree.computeLayout(root, TaffySize.maxContent());

        assertRect(tree, first, 0f, 0f, 355f, 160f);
        assertRect(tree, second, 5f, 20f, 355f, 160f);
        assertRect(tree, third, 0f, 0f, 55f, 40f);
        assertRect(tree, fourth, 155f, 80f, 205f, 100f);
        assertRect(tree, fifth, 360f, 170f, 10f, 30f);
    }

    /**
     * Equivalent of {@code grid_placement_named_line_negative_index}: negative indices count
     * backwards from the end of the explicit grid and continue into the implicit lines before it.
     */
    @Test
    void negativeNamedIndexCountsBackwardsFromTheExplicitGrid() {
        TaffyStyle style = new TaffyStyle();
        style.gridTemplateColumns = List.of(px(50f), px(100f), px(200f));
        style.gridTemplateRows = List.of(px(20f), px(40f), px(80f));
        style.gridTemplateColumnNames = List.of(
            new NamedGridLine("col", 1),
            new NamedGridLine("col", 2),
            new NamedGridLine("mid", 2),
            new NamedGridLine("col", 3),
            new NamedGridLine("col", 4));
        style.gridTemplateRowNames = List.of(
            new NamedGridLine("row", 1),
            new NamedGridLine("row", 2),
            new NamedGridLine("mid", 2),
            new NamedGridLine("row", 3),
            new NamedGridLine("row", 4));
        NamedLineResolver resolver = resolver(style, 3, 3);

        assertEquals(4, resolvedStart(resolver, GridPlacement.namedLine("col", -1), true));
        assertEquals(3, resolvedStart(resolver, GridPlacement.namedLine("row", -2), false));
        assertEquals(2, resolvedStart(resolver, GridPlacement.namedLine("mid", -1), true));
        // Only one line is named "mid": the second occurrence counted backwards continues into the
        // implicit lines before the explicit grid (CSS line -5), where the old resolver numbered the
        // implicit lines from the wrong origin and produced -1.
        assertEquals(-5, resolvedStart(resolver, GridPlacement.namedLine("mid", -2), true));
        assertEquals(-5, resolvedStart(resolver, GridPlacement.namedLine("col", -5), true));
        assertEquals(-5, resolvedStart(resolver, GridPlacement.namedLine("foo", -1), true));
        assertEquals(-6, resolvedStart(resolver, GridPlacement.namedLine("mid", -3), false));
        // An explicit end line pairs with a backwards named span exactly like upstream.
        TaffyLine<GridPlacement> pair = resolver.resolveColumnNames(
            place(GridPlacement.namedLine("mid", -2), GridPlacement.namedLine("foo", -1)));
        assertEquals(-5, pair.start.getLineNumber());
        assertEquals(-5, pair.end.getLineNumber());
    }

    /**
     * Named-line numbering is clamped into the limited grid (GR-17 / #1035): extremely large or
     * small indices must not underflow/overflow the line arithmetic.
     */
    @Test
    void namedLineNumbersAreClampedToTheLimitedGrid() {
        TaffyStyle style = new TaffyStyle();
        NamedLineResolver resolver = resolver(style, 10_000, 10_000);

        // OriginZero MAX_OZ_LINE (10000) converted back to CSS coordinates with 10000 explicit
        // tracks: 10000 + 1.
        assertEquals(10_001, resolvedStart(resolver, GridPlacement.namedLine("missing", Integer.MAX_VALUE), true));
        // OriginZero MIN_OZ_LINE (-10000) converted back to CSS coordinates: -10000 - 10001.
        assertEquals(-20_001, resolvedStart(resolver, GridPlacement.namedLine("missing", Integer.MIN_VALUE), true));

        TaffyLine<GridPlacement> span = resolver.resolveColumnNames(
            place(GridPlacement.line(1), GridPlacement.namedSpan("missing", Integer.MAX_VALUE)));
        assertEquals(10_001, span.end.getLineNumber());
    }

    // ------------------------------------------------------------------
    // GR-15: the explicit grid / areas and the absolutely positioned path
    // ------------------------------------------------------------------

    /**
     * {@code grid-template-areas} contributes the extents of the explicit grid (upstream
     * {@code explicit_count = max(template_count, area_count)}), and the area edges resolve against
     * the extended explicit grid.
     */
    @Test
    void areaExtentsExtendTheExplicitGrid() {
        TaffyStyle style = new TaffyStyle();
        style.gridTemplateAreas = List.of(
            new GridTemplateArea("wide", 1, 2, 3, 5),
            new GridTemplateArea("tall", 4, 6, 1, 2));
        NamedLineResolver resolver = new NamedLineResolver(style);

        assertEquals(4, resolver.getAreaColumnCount());
        assertEquals(5, resolver.getAreaRowCount());

        resolver.setExplicitColumnCount(Math.max(2, resolver.getAreaColumnCount()));
        resolver.setExplicitRowCount(Math.max(2, resolver.getAreaRowCount()));
        assertEquals(3, resolvedStart(resolver, GridPlacement.namedLine("wide"), true));
        assertEquals(5, resolvedEnd(resolver, GridPlacement.namedLine("wide"), true));
        assertEquals(4, resolvedStart(resolver, GridPlacement.namedLine("tall"), false));
        assertEquals(6, resolvedEnd(resolver, GridPlacement.namedLine("tall"), false));
        // Numeric lines are resolved by the grid computer against the same extended explicit count;
        // the resolver leaves them untouched.
        assertEquals(-1, resolvedStart(resolver, GridPlacement.line(-1), true));
    }

    /**
     * Absolutely positioned grid children are resolved through the same resolver instance that the
     * in-flow path builds before the grid size estimate (GR-15 / #1259): named lines must not fall
     * back to the container edges.
     */
    @Test
    void absoluteChildrenUseTheOnceResolvedNamedLines() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = gridContainer(200f, 150f);
        rootStyle.position = TaffyPosition.RELATIVE;
        rootStyle.padding = TaffyRect.all(LengthPercentage.length(10f));
        rootStyle.gridTemplateColumns = List.of(px(40f), px(60f));
        rootStyle.gridTemplateRows = List.of(px(20f), px(50f));
        rootStyle.gridTemplateColumnNames = List.of(
            new NamedGridLine("a", 1),
            new NamedGridLine("b", 2),
            new NamedGridLine("c", 3));
        rootStyle.gridTemplateRowNames = List.of(
            new NamedGridLine("a", 1),
            new NamedGridLine("b", 2),
            new NamedGridLine("c", 3));
        NodeId root = tree.newLeaf(rootStyle);

        NodeId full = addAbsoluteItem(tree, root,
            place(GridPlacement.namedLine("a"), GridPlacement.namedLine("c")),
            place(GridPlacement.namedLine("a"), GridPlacement.namedLine("c")));
        NodeId firstColumn = addAbsoluteItem(tree, root,
            place(GridPlacement.namedLine("a"), GridPlacement.namedLine("b")),
            place(GridPlacement.namedLine("a"), GridPlacement.namedLine("b")));

        tree.computeLayout(root, TaffySize.maxContent());

        // Named lines resolve to the grid lines, so the items cover exactly their grid area (the
        // padding box starts at 10,10 and the two tracks are 100 x 70).
        assertRect(tree, full, 10f, 10f, 100f, 70f);
        assertRect(tree, firstColumn, 10f, 10f, 40f, 20f);
    }

    private static NodeId addAbsoluteItem(TaffyTree tree, NodeId parent,
                                          TaffyLine<GridPlacement> column,
                                          TaffyLine<GridPlacement> row) {
        TaffyStyle style = new TaffyStyle();
        style.position = TaffyPosition.ABSOLUTE;
        style.inset = TaffyRect.all(LengthPercentageAuto.length(0f));
        style.gridColumn = column;
        style.gridRow = row;
        NodeId child = tree.newLeaf(style);
        tree.addChild(parent, child);
        return child;
    }
}
