package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.GridRepetition;
import dev.vfyjxf.taffy.style.GridTemplateArea;
import dev.vfyjxf.taffy.style.GridTemplateComponent;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.NamedGridLine;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.DetailedGridInfo;
import dev.vfyjxf.taffy.tree.GridTrackInfo;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.tree.NodeId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P7 GR-18 second slice: the CSR line-name structure and the separate
 * {@code grid-template-areas} edge table of {@link DetailedGridInfo} (upstream #1136 / #1185).
 *
 * <p>The assertions pin the upstream semantics: the per-line CSR keeps only names declared on
 * the track template (including {@code repeat()} positional name sets), the implicit
 * {@code <area>-start}/{@code <area>-end} names of {@code grid-template-areas} live in the
 * separate {@link DetailedGridInfo#areas()} table, and the first-slice
 * {@link DetailedGridInfo.Tracks#lineNames()} accessor keeps returning the same immutable,
 * sorted map. The item rectangles (#1134), the resolved track-list serialization (#1136/#1280)
 * and the empty-axis distinction (#1167) are covered by the later sections of this class.</p>
 */
class DetailedGrid2P7Test {

    private static final float EPSILON = 0.01f;

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static TrackSizingFunction px(float value) {
        return TrackSizingFunction.fixed(LengthPercentage.length(value));
    }

    private static TaffyStyle grid() {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;
        style.justifyContent = AlignContent.START;
        style.alignContent = AlignContent.START;
        return style;
    }

    private static TaffyStyle sizedGrid(float width, float height) {
        TaffyStyle style = grid();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyStyle leaf() {
        return new TaffyStyle();
    }

    private static NodeId addItem(TaffyTree tree, NodeId parent) {
        NodeId child = tree.newLeaf(leaf());
        tree.addChild(parent, child);
        return child;
    }

    private static TaffyLine<GridPlacement> line(int startLine) {
        return new TaffyLine<>(GridPlacement.line(startLine), GridPlacement.auto());
    }

    private static DetailedGridInfo infoOf(TaffyTree tree, NodeId node) {
        DetailedGridInfo info = tree.getDetailedGridInfo(node);
        assertNotNull(info, "expected detailed grid info for " + node);
        return info;
    }

    private static GridTrackInfo track(float start, float size) {
        return new GridTrackInfo(start, size);
    }

    private static TaffyRect<LengthPercentage> all(float value) {
        LengthPercentage length = LengthPercentage.length(value);
        return new TaffyRect<>(length, length, length, length);
    }

    private static void assertRectangle(DetailedGridInfo.ItemRectangle rectangle,
                                        float x, float y, float width, float height, String message) {
        assertNotNull(rectangle, message);
        assertEquals(x, rectangle.x(), EPSILON, message + " x");
        assertEquals(y, rectangle.y(), EPSILON, message + " y");
        assertEquals(width, rectangle.width(), EPSILON, message + " width");
        assertEquals(height, rectangle.height(), EPSILON, message + " height");
    }

    // ------------------------------------------------------------------
    // CSR line names
    // ------------------------------------------------------------------

    @Test
    void csrLineNamesExposePerLineGroupsAndNameSpans() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(100f, 50f);
        style.gridTemplateColumns = List.of(px(40f), px(60f));
        style.gridTemplateRows = List.of(px(50f));
        // Declaration order on line 3 is "c" then "a": the per-line CSR preserves it.
        style.gridTemplateColumnNames = List.of(
            new NamedGridLine("a", 1),
            new NamedGridLine("b", 2),
            new NamedGridLine("c", 3),
            new NamedGridLine("a", 3));
        NodeId root = tree.newWithChildren(style, List.of(tree.newLeaf(leaf())));
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo.Tracks columns = infoOf(tree, root).columns();
        DetailedGridInfo.LineNames names = columns.lineNameTable();

        // Line CSR: one group per explicit line, in declaration order.
        assertEquals(3, names.lineCount(), "one group per explicit line");
        assertEquals(List.of("a"), names.line(0));
        assertEquals(List.of("b"), names.line(1));
        assertEquals(List.of("c", "a"), names.line(2), "same-line declaration order");
        assertEquals(List.of(List.of("a"), List.of("b"), List.of("c", "a")), names.lines());
        assertEquals(List.of(), names.line(-1), "no line before the explicit grid");
        assertEquals(List.of(), names.line(3), "no line after the explicit grid");

        // Name CSR: sorted names and start/count spans into the flat line-number array.
        assertEquals(3, names.nameCount());
        assertEquals(List.of("a", "b", "c"), List.of(names.nameAt(0), names.nameAt(1), names.nameAt(2)));
        assertEquals(0, names.nameLineStart("a"));
        assertEquals(2, names.nameLineCount("a"));
        assertEquals(1, names.lineNumberAt(0));
        assertEquals(3, names.lineNumberAt(1));
        assertEquals(2, names.nameLineStart("b"));
        assertEquals(1, names.nameLineCount("b"));
        assertEquals(2, names.lineNumberAt(2));
        assertEquals(3, names.nameLineStart("c"));
        assertEquals(3, names.lineNumberAt(3));
        assertEquals(-1, names.nameLineStart("missing"));
        assertEquals(0, names.nameLineCount("missing"));
        assertEquals(List.of(1, 3), names.linesNamed("a"));
        assertEquals(List.of(), names.linesNamed("missing"));

        // Old first-slice accessors keep their exact semantics (sorted immutable map).
        assertEquals(List.of(1, 3), columns.linesNamed("a"));
        assertEquals(Map.of("a", List.of(1, 3), "b", List.of(2), "c", List.of(3)), columns.lineNames());
        assertEquals(List.of("a", "b", "c"), List.copyOf(columns.lineNames().keySet()));
        assertEquals(3, columns.lineNames().size());
        assertTrue(columns.lineNames().containsKey("a"));
        assertFalse(columns.lineNames().containsKey("missing"));
        assertNull(columns.lineNames().get("missing"));
        assertFalse(names.isEmpty());

        // Full-grid line accessors and the iteration match upstream names_for_line/iter_line_names.
        assertEquals(List.of("a"), columns.namesForLine(0));
        assertEquals(List.of("b"), columns.namesForLine(1));
        assertEquals(List.of("c", "a"), columns.namesForLine(2));
        assertEquals(List.of(), columns.namesForLine(3));
        assertEquals(List.of(List.of("a"), List.of("b"), List.of("c", "a")), columns.iterLineNames());

        // Immutability of both views.
        assertThrows(UnsupportedOperationException.class, () -> columns.lineNames().put("x", List.of(1)));
        assertThrows(UnsupportedOperationException.class, () -> names.linesNamed("a").add(2));
        assertThrows(UnsupportedOperationException.class, () -> names.line(0).add("x"));
        assertThrows(UnsupportedOperationException.class,
            () -> names.entrySet().iterator().next().setValue(List.of(9)));
    }

    @Test
    void legacyTracksConstructorNormalisesTheMapToCsr() {
        // The first-slice public constructor remains source-compatible: a plain map is
        // normalised to the CSR, and the explicit track count sizes the per-line view so the
        // trailing explicit line exists even when it has no name.
        DetailedGridInfo.Tracks tracks = new DetailedGridInfo.Tracks(
            0, 2, 0,
            List.of(track(0f, 40f), track(40f, 60f)),
            null,
            Map.of("a", List.of(1)));

        assertEquals(List.of(1), tracks.linesNamed("a"));
        assertEquals(List.of(1), tracks.lineNames().get("a"));
        assertEquals(3, tracks.lineNameTable().lineCount(), "explicit tracks + 1");
        assertEquals(List.of("a"), tracks.lineNameTable().line(0));
        assertEquals(List.of(), tracks.lineNameTable().line(1));
        assertEquals(List.of(), tracks.lineNameTable().line(2));
        assertEquals(List.of(List.of("a"), List.of(), List.of()), tracks.lineNameTable().lines());
        assertEquals(40f, tracks.track(0).size(), EPSILON);
        assertEquals(60f, tracks.track(1).size(), EPSILON);
        assertNull(tracks.emptyAxisLine());
        assertThrows(UnsupportedOperationException.class, () -> tracks.tracks().add(track(0f, 1f)));
    }

    @Test
    void emptyCrsIsReportedWhenTheAxisHasNoNamedLines() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(100f, 50f);
        style.gridTemplateColumns = List.of(px(40f), px(60f));
        style.gridTemplateRows = List.of(px(50f));
        NodeId root = tree.newWithChildren(style, List.of(tree.newLeaf(leaf())));
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo.Tracks columns = infoOf(tree, root).columns();
        assertTrue(columns.lineNameTable().isEmpty());
        assertEquals(0, columns.lineNameTable().lineCount());
        assertEquals(0, columns.lineNameTable().nameCount());
        assertEquals(List.of(), columns.iterLineNames(), "no named lines yields no line groups");
        assertEquals(List.of(), columns.lineNames().keySet().stream().toList());
    }

    // ------------------------------------------------------------------
    // Item rectangles
    // ------------------------------------------------------------------

    @Test
    void itemRectangleResolvesTheBorderBoxGeometryOfEachArea() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(200f, 74f);
        style.padding = all(5f);
        style.border = all(2f);
        style.gap = new TaffySize<>(LengthPercentage.length(10f), LengthPercentage.length(10f));
        style.gridTemplateColumns = List.of(px(40f), px(60f));
        style.gridTemplateRows = List.of(px(30f), px(20f));

        NodeId a = tree.newLeaf(leaf());
        NodeId b = tree.newLeaf(leaf());
        NodeId c = tree.newLeaf(leaf());
        NodeId d = tree.newLeaf(leaf());
        NodeId root = tree.newWithChildren(style, List.of(a, b, c, d));
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo info = infoOf(tree, root);
        assertEquals(4, info.itemCount());
        assertRectangle(info.itemRectangle(0), 7f, 7f, 40f, 30f, "a");
        assertRectangle(info.itemRectangle(1), 57f, 7f, 60f, 30f, "b");
        assertRectangle(info.itemRectangle(2), 7f, 47f, 40f, 20f, "c");
        assertRectangle(info.itemRectangle(3), 57f, 47f, 60f, 20f, "d");
        assertNull(info.itemRectangle(4), "out-of-range item index");
        assertNull(info.itemRectangle(-1), "negative item index");

        // The rectangle describes exactly the geometry the item layout was built from.
        assertEquals(tree.getLayout(a).location().x, info.itemRectangle(0).x(), EPSILON);
        assertEquals(tree.getLayout(a).location().y, info.itemRectangle(0).y(), EPSILON);
        assertEquals(tree.getLayout(d).size().width, info.itemRectangle(3).width(), EPSILON);
        assertEquals(tree.getLayout(d).size().height, info.itemRectangle(3).height(), EPSILON);

        DetailedGridInfo.ItemRectangle first = info.itemRectangle(0);
        assertEquals(first.x() + first.width(), first.right(), EPSILON);
        assertEquals(first.y() + first.height(), first.bottom(), EPSILON);
    }

    @Test
    void itemRectangleIncludesGuttersBetweenSpannedTracks() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(120f, 100f);
        style.padding = all(5f);
        style.gap = new TaffySize<>(LengthPercentage.length(10f), LengthPercentage.length(10f));
        style.gridTemplateColumns = List.of(px(40f), px(60f));
        style.gridTemplateRows = List.of(px(50f), px(30f));

        TaffyStyle spanStyle = leaf();
        spanStyle.gridColumn = new TaffyLine<>(GridPlacement.line(1), GridPlacement.line(3));
        spanStyle.gridRow = new TaffyLine<>(GridPlacement.line(2), GridPlacement.line(3));
        NodeId span = tree.newLeaf(spanStyle);
        NodeId auto = tree.newLeaf(leaf());
        NodeId root = tree.newWithChildren(style, List.of(span, auto));
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo info = infoOf(tree, root);
        // Row 2, both columns: 40 + 10 gutter + 60 wide; the rectangle includes the gutter.
        assertRectangle(info.itemRectangle(0), 5f, 65f, 110f, 30f, "spanning item");
        assertRectangle(info.itemRectangle(1), 5f, 5f, 40f, 50f, "auto item");
        assertEquals(110f, tree.getLayout(span).size().width, EPSILON);
    }

    @Test
    void itemRectangleIncludesContentAlignmentOffset() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(200f, 100f);
        style.justifyContent = AlignContent.CENTER;
        style.alignContent = AlignContent.CENTER;
        style.gridTemplateColumns = List.of(px(40f), px(60f));
        style.gridTemplateRows = List.of(px(50f));
        NodeId item = tree.newLeaf(leaf());
        NodeId root = tree.newWithChildren(style, List.of(item));
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo info = infoOf(tree, root);
        // Free inline space 100 -> centered offset 50; free block space 50 -> offset 25.
        assertRectangle(info.itemRectangle(0), 50f, 25f, 40f, 50f, "centered item");
        assertEquals(tree.getLayout(item).location().x, info.itemRectangle(0).x(), EPSILON);
        assertEquals(tree.getLayout(item).location().y, info.itemRectangle(0).y(), EPSILON);
    }

    @Test
    void itemRectangleIsPhysicalInRtl() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(110f, 60f);
        style.direction = dev.vfyjxf.taffy.style.TaffyDirection.RTL;
        style.padding = all(5f);
        style.gridTemplateColumns = List.of(px(40f), px(60f));
        style.gridTemplateRows = List.of(px(50f));

        TaffyStyle spanStyle = leaf();
        spanStyle.gridColumn = new TaffyLine<>(GridPlacement.line(1), GridPlacement.line(3));
        NodeId span = tree.newLeaf(spanStyle);
        NodeId root = tree.newWithChildren(style, List.of(span));
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo info = infoOf(tree, root);
        // Logical track 1 is physically rightmost (65..105), logical track 2 is 5..65. The
        // rectangle takes the minimum physical start and maximum physical end (5..105).
        assertEquals(65f, info.columns().track(0).start(), EPSILON);
        assertEquals(5f, info.columns().track(1).start(), EPSILON);
        assertRectangle(info.itemRectangle(0), 5f, 5f, 100f, 50f, "RTL spanning item");
        assertEquals(5f, tree.getLayout(span).location().x, EPSILON);
        assertEquals(100f, tree.getLayout(span).size().width, EPSILON);
    }

    // ------------------------------------------------------------------
    // repeat() positional names and implicit grid lines
    // ------------------------------------------------------------------

    @Test
    void repeatPositionalNamesAreCsrLineGroups() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(100f, 50f);
        style.gridTemplateColumnsWithRepeat = List.of(
            GridTemplateComponent.repeat(GridRepetition.count(2, List.of(px(50f)),
                List.of(List.of("col-start"), List.of("col-end")))));
        style.gridTemplateColumnNames = List.of(
            new NamedGridLine("outer-start", 1),
            new NamedGridLine("outer-end", 3));
        style.gridTemplateRows = List.of(px(50f));
        NodeId root = tree.newWithChildren(style, List.of(tree.newLeaf(leaf())));
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo.Tracks columns = infoOf(tree, root).columns();
        assertEquals(2, columns.explicitTracks());
        // Flat style names are declared before the repeat() positional sets in this port's style
        // API, so on the collapsed last line "outer-end" precedes "col-end". This ordering is a
        // documented consequence of the flattened (name, line) Java style input.
        assertEquals(List.of("outer-start", "col-start"), columns.lineNameTable().line(0));
        assertEquals(List.of("col-end", "col-start"), columns.lineNameTable().line(1));
        assertEquals(List.of("outer-end", "col-end"), columns.lineNameTable().line(2));
        assertEquals(List.of(1), columns.linesNamed("outer-start"));
        assertEquals(List.of(3), columns.linesNamed("outer-end"));
        assertEquals(List.of(1, 2), columns.linesNamed("col-start"));
        assertEquals(List.of(2, 3), columns.linesNamed("col-end"));
    }

    @Test
    void namesForLineAccountsForImplicitTracks() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(100f, 50f);
        style.gridTemplateColumns = List.of(px(50f));
        style.gridTemplateColumnNames = List.of(
            new NamedGridLine("a", 1),
            new NamedGridLine("b", 2));
        style.gridTemplateRows = List.of(px(50f));

        TaffyStyle itemStyle = leaf();
        itemStyle.gridColumn = line(-3); // one negative implicit column before the explicit track
        NodeId item = tree.newLeaf(itemStyle);
        NodeId root = tree.newWithChildren(style, List.of(item));
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo.Tracks columns = infoOf(tree, root).columns();
        assertEquals(1, columns.negativeImplicitTracks());
        assertEquals(2, columns.lineNameTable().lineCount(), "explicit lines only");
        // Full-grid indices: the leading implicit line is unnamed, then a/b follow.
        assertEquals(List.of(), columns.namesForLine(0));
        assertEquals(List.of("a"), columns.namesForLine(1));
        assertEquals(List.of("b"), columns.namesForLine(2));
        assertEquals(List.of(), columns.namesForLine(3));
        assertEquals(List.of(List.of(), List.of("a"), List.of("b")), columns.iterLineNames());
    }

    // ------------------------------------------------------------------
    // Resolved track-list serialization
    // ------------------------------------------------------------------

    @Test
    void resolvedTrackListSerializesSizesInUpstreamFormat() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(100f, 50f);
        style.gridTemplateColumns = List.of(px(40f), px(60f));
        style.gridTemplateRows = List.of(px(50f));
        NodeId root = tree.newWithChildren(style, List.of(tree.newLeaf(leaf())));
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo info = infoOf(tree, root);
        assertEquals("40px 60px", info.gridTemplateColumns());
        assertEquals("50px", info.gridTemplateRows());
        assertEquals(info.gridTemplateColumns(1f), info.gridTemplateColumns(), "default zoom is 1");
        assertEquals(info.gridTemplateRows(1f), info.gridTemplateRows(), "default zoom is 1");
    }

    @Test
    void zoomDividesSerializedTrackSizes() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(100f, 100f);
        style.gridTemplateColumns = List.of(px(100f));
        style.gridTemplateRows = List.of(px(50f));
        NodeId root = tree.newWithChildren(style, List.of(tree.newLeaf(leaf())));
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo info = infoOf(tree, root);
        assertEquals("50px", info.gridTemplateColumns(2f));
        assertEquals("25px", info.gridTemplateRows(2f));
        // f32 shortest round-trip decimal, like Rust's Display: 100 / 3 = 33.333332.
        assertEquals("33.333332px", info.gridTemplateColumns(3f));

        StringBuilder columns = new StringBuilder("prefix:");
        info.writeGridTemplateColumns(columns, 4f);
        assertEquals("prefix:25px", columns.toString());
        StringBuilder rows = new StringBuilder();
        info.writeGridTemplateRows(rows);
        assertEquals("50px", rows.toString());
        StringBuilder defaultColumns = new StringBuilder();
        info.writeGridTemplateColumns(defaultColumns);
        assertEquals("100px", defaultColumns.toString());
    }

    @Test
    void resolvedTrackListPrintsLineNamesInDeclarationOrder() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(100f, 50f);
        style.gridTemplateColumns = List.of(px(40f), px(60f));
        style.gridTemplateRows = List.of(px(50f));
        style.gridTemplateColumnNames = List.of(
            new NamedGridLine("full-start", 1),
            new NamedGridLine("main-start", 2),
            new NamedGridLine("main-end", 3),
            new NamedGridLine("full-end", 3));
        style.gridTemplateAreas = List.of(new GridTemplateArea("hero", 1, 2, 1, 3));
        NodeId root = tree.newWithChildren(style, List.of(tree.newLeaf(leaf())));
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo info = infoOf(tree, root);
        // Trailing explicit line names are appended after the last track; the implicit
        // grid-template-areas hero-start/hero-end names never appear (#1185).
        assertEquals("[full-start] 40px [main-start] 60px [main-end full-end]",
            info.gridTemplateColumns());
        assertEquals("50px", info.gridTemplateRows());
        assertEquals("[full-start] 20px [main-start] 30px [main-end full-end]",
            info.gridTemplateColumns(2f));
        assertFalse(info.gridTemplateColumns().contains("hero"));
    }

    @Test
    void resolvedTrackListOmitsTrailingNamesBeyondTheExplicitGrid() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(100f, 50f);
        style.gridTemplateColumns = List.of(px(50f));
        style.gridTemplateColumnNames = List.of(
            new NamedGridLine("a", 1),
            new NamedGridLine("b", 2));
        style.gridTemplateRows = List.of(px(50f));
        style.gridAutoColumns = List.of(px(25f));

        TaffyStyle itemStyle = leaf();
        itemStyle.gridColumn = line(3); // one positive implicit column after the explicit track
        NodeId item = tree.newLeaf(itemStyle);
        NodeId root = tree.newWithChildren(style, List.of(item));
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo info = infoOf(tree, root);
        // This port's line-3 placement materialises two positive implicit tracks (pinned by the
        // first-slice test); both use the 25px auto-column size.
        assertEquals(2, info.columns().positiveImplicitTracks());
        // The explicit end line's names are printed on the first implicit track's start line; the
        // line after the last full-grid track is unnamed, so no names follow the final 25px.
        assertEquals("[a] 50px [b] 25px 25px", info.gridTemplateColumns());
    }

    @Test
    void resolvedTrackListIncludesLeadingImplicitTracksAsUnnamed() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(100f, 50f);
        style.gridTemplateColumns = List.of(px(50f));
        style.gridTemplateColumnNames = List.of(
            new NamedGridLine("a", 1),
            new NamedGridLine("b", 2));
        style.gridTemplateRows = List.of(px(50f));
        style.gridAutoColumns = List.of(px(25f));

        TaffyStyle itemStyle = leaf();
        itemStyle.gridColumn = line(-3); // one negative implicit column before the explicit track
        NodeId item = tree.newLeaf(itemStyle);
        NodeId root = tree.newWithChildren(style, List.of(item));
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo info = infoOf(tree, root);
        assertEquals(1, info.columns().negativeImplicitTracks());
        assertEquals("25px [a] 50px [b]", info.gridTemplateColumns());
    }

    @Test
    void repeatPositionalNamesSerializeInDeclarationOrder() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(100f, 50f);
        style.gridTemplateColumnsWithRepeat = List.of(
            GridTemplateComponent.repeat(GridRepetition.count(2, List.of(px(50f)),
                List.of(List.of("col-start"), List.of("col-end")))));
        style.gridTemplateColumnNames = List.of(
            new NamedGridLine("outer-start", 1),
            new NamedGridLine("outer-end", 3));
        style.gridTemplateRows = List.of(px(50f));
        NodeId root = tree.newWithChildren(style, List.of(tree.newLeaf(leaf())));
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo info = infoOf(tree, root);
        // The per-line order follows this port's flattened style inputs (flat names first), which
        // differs from upstream's positional-set interleaving on the collapsed last line only.
        assertEquals("[outer-start col-start] 50px [col-end col-start] 50px [outer-end col-end]",
            info.gridTemplateColumns());
    }

    @Test
    void emptyAxisSerializesAsNoneWhileCollapsedOrMaterialisedAxesSerializeTheirTracks() {
        // A true zero-track axis is not produced by this port's layout (the size estimate
        // materialises one implicit track), but the detailed value object accepts and serializes
        // it exactly like upstream: "none".
        DetailedGridInfo.Tracks empty = new DetailedGridInfo.Tracks(
            0, 0, 0, List.of(), 12f, Map.of());
        assertEquals("none", empty.toTrackListString());
        assertEquals("none", empty.toTrackListString(2f));

        // An all-collapsed auto-fit axis keeps its expanded tracks (all zero-sized), so it does
        // not serialize as "none": the difference from a true zero-track axis stays observable.
        List<GridTrackInfo> collapsed = List.of(track(7f, 0f), track(7f, 0f), track(7f, 0f), track(7f, 0f));
        DetailedGridInfo.Tracks collapsedAxis = new DetailedGridInfo.Tracks(
            0, 4, 0, collapsed, 7f, Map.of());
        assertEquals("0px 0px 0px 0px", collapsedAxis.toTrackListString());

        // The grid path's plain empty axis materialises one implicit track (the residual noted by
        // the first slice): 0px column, 50px row, never "none".
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(100f, 50f);
        TaffyStyle hiddenStyle = leaf();
        hiddenStyle.display = TaffyDisplay.NONE;
        NodeId root = tree.newWithChildren(style, List.of(tree.newLeaf(hiddenStyle)));
        tree.computeLayout(root, TaffySize.maxContent());
        DetailedGridInfo info = infoOf(tree, root);
        assertEquals("0px", info.gridTemplateColumns());
        assertEquals("50px", info.gridTemplateRows());
    }

    @Test
    void toStringIncludesResolvedTracksItemsAndAreas() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(100f, 50f);
        style.gridTemplateColumns = List.of(px(40f), px(60f));
        style.gridTemplateRows = List.of(px(50f));
        style.gridTemplateAreas = List.of(new GridTemplateArea("hero", 1, 2, 1, 2));
        NodeId root = tree.newWithChildren(style, List.of(tree.newLeaf(leaf())));
        tree.computeLayout(root, TaffySize.maxContent());

        String text = infoOf(tree, root).toString();
        assertTrue(text.startsWith("DetailedGridInfo{"), text);
        assertTrue(text.contains("columns=40px 60px"), text);
        assertTrue(text.contains("rows=50px"), text);
        assertTrue(text.contains("items=[GridItemArea["), text);
        assertTrue(text.contains(" -> ItemRectangle["), text);
        assertTrue(text.contains("areas={hero=GridArea["), text);
    }

    // ------------------------------------------------------------------
    // Area edge table
    // ------------------------------------------------------------------

    @Test
    void areaEdgeTableIsSeparateFromTheLineNameCsr() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(100f, 50f);
        style.gridTemplateColumns = List.of(px(40f), px(60f));
        style.gridTemplateRows = List.of(px(50f));
        style.gridTemplateAreas = List.of(new GridTemplateArea("hero", 1, 2, 1, 3));
        // An explicitly declared area-edge-looking name is an ordinary explicit line name.
        style.gridTemplateColumnNames = List.of(new NamedGridLine("hero-start", 2));
        NodeId root = tree.newWithChildren(style, List.of(tree.newLeaf(leaf())));
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo info = infoOf(tree, root);
        DetailedGridInfo.Tracks columns = info.columns();

        // The implicit area edges are not part of the resolved line names (#1185).
        assertFalse(columns.lineNames().containsKey("hero-end"));
        assertEquals(List.of(), columns.lineNameTable().line(2), "line 3 carries no declared name");

        // The explicitly declared hero-start is an ordinary line name at its declared line...
        assertEquals(List.of("hero-start"), columns.lineNameTable().line(1));
        // ...while the placement table still merges the area's edge line (line 1), as upstream
        // upsert_line_name_map does; the serialized line CSR keeps only the explicit occurrence.
        assertEquals(List.of(1, 2), columns.linesNamed("hero-start"));
        assertEquals(List.of(1, 2), columns.lineNames().get("hero-start"));

        // The area table carries the four style-side edges of the named area.
        assertEquals(1, info.areas().size());
        assertEquals(new DetailedGridInfo.GridArea(1, 1, 2, 3), info.areas().get("hero"));
        assertEquals(List.of("hero"), List.copyOf(info.areas().keySet()));
        assertThrows(UnsupportedOperationException.class,
            () -> info.areas().put("x", new DetailedGridInfo.GridArea(1, 1, 2, 2)));
    }

    @Test
    void areaTableIsSortedAndIncludesExplicitlyDeclaredEdges() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(100f, 50f);
        style.gridTemplateColumns = List.of(px(40f), px(60f));
        style.gridTemplateRows = List.of(px(50f));
        style.gridTemplateAreas = List.of(
            new GridTemplateArea("zeta", 1, 2, 1, 3),
            new GridTemplateArea("alpha", 1, 2, 2, 3));
        NodeId root = tree.newWithChildren(style, List.of(tree.newLeaf(leaf())));
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo info = infoOf(tree, root);
        assertEquals(List.of("alpha", "zeta"), List.copyOf(info.areas().keySet()));
        assertEquals(new DetailedGridInfo.GridArea(1, 2, 2, 3), info.areas().get("alpha"));
        assertEquals(new DetailedGridInfo.GridArea(1, 1, 2, 3), info.areas().get("zeta"));
        // Implicit edges stay out of the line-name table on both axes.
        assertFalse(info.columns().lineNames().containsKey("alpha-start"));
        assertFalse(info.rows().lineNames().containsKey("alpha-start"));
        assertFalse(info.columns().lineNames().containsKey("zeta-end"));
    }
}
