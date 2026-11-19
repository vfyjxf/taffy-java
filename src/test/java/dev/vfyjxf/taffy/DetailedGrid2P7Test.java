package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffyLine;
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
 * sorted map.</p>
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
