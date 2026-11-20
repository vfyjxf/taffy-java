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
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.DetailedGridInfo;
import dev.vfyjxf.taffy.tree.GridItemArea;
import dev.vfyjxf.taffy.tree.GridTrackInfo;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P7 GR-18 first slice: the additive {@link DetailedGridInfo} view of a resolved grid container.
 *
 * <p>Upstream taffy exposes the per-track positions of #1131, the per-item 1-based grid areas of
 * #1134 and the explicit line-name table of #1136 behind the {@code detailed_layout_info} feature.
 * This port adds the same information without changing a single layout number: the tests below
 * pin both the new detail values and the exact layout geometry they describe (so a regression in
 * the old numbers shows up next to the new API assertions).</p>
 *
 * <p>Deferred to later GR-18 slices (asserted here only as constants): the resolved
 * {@code grid-template-rows/columns} serialization with the #1280 {@code zoom} argument
 * ({@code zoom = 1} reproduces the pre-#1280 strings), the CSR per-line name structure, the
 * grid-area edge table, the #1185 explicit omission of implicit area names from a serialized
 * track list, and the physical {@code item_grid_area} rectangle of #1137. The line-name table
 * here already omits the implicit {@code <area>-start}/{@code <area>-end} names; it includes
 * only names that are actually declared on a template.</p>
 */
class DetailedGridP7Test {

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
        // These tests assert track geometry, so opt out of the free-space distribution of the
        // default (normal -> stretch) content alignment. START leaves the used tracks at their
        // intrinsic sizes and anchors them at the content-box start edge.
        style.justifyContent = AlignContent.START;
        style.alignContent = AlignContent.START;
        return style;
    }

    private static TaffyStyle sizedGrid(float width, float height) {
        TaffyStyle style = grid();
        style.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
        return style;
    }

    private static TaffyRect<LengthPercentage> all(float value) {
        LengthPercentage length = LengthPercentage.length(value);
        return new TaffyRect<>(length, length, length, length);
    }

    private static TaffyStyle leaf() {
        return new TaffyStyle();
    }

    private static NodeId addItem(TaffyTree tree, NodeId parent) {
        NodeId child = tree.newLeaf(leaf());
        tree.addChild(parent, child);
        return child;
    }

    private static NodeId addItem(TaffyTree tree, NodeId parent, TaffyStyle style) {
        NodeId child = tree.newLeaf(style);
        tree.addChild(parent, child);
        return child;
    }

    private static TaffyLine<GridPlacement> line(int startLine) {
        return new TaffyLine<>(GridPlacement.line(startLine), GridPlacement.auto());
    }

    private static TaffyLine<GridPlacement> lines(int startLine, int endLine) {
        return new TaffyLine<>(GridPlacement.line(startLine), GridPlacement.line(endLine));
    }

    private static DetailedGridInfo infoOf(TaffyTree tree, NodeId node) {
        DetailedGridInfo info = tree.getDetailedGridInfo(node);
        assertNotNull(info, "expected detailed grid info for " + node);
        return info;
    }

    private static void assertTrack(GridTrackInfo track, float start, float size, String message) {
        assertEquals(start, track.start(), EPSILON, message + " start");
        assertEquals(size, track.size(), EPSILON, message + " size");
        assertEquals(start + size, track.end(), EPSILON, message + " end");
    }

    private static void assertArea(GridItemArea area, int rowStart, int rowEnd,
                                   int columnStart, int columnEnd, String message) {
        assertNotNull(area, message);
        assertEquals(rowStart, area.rowStart(), message + " rowStart");
        assertEquals(rowEnd, area.rowEnd(), message + " rowEnd");
        assertEquals(columnStart, area.columnStart(), message + " columnStart");
        assertEquals(columnEnd, area.columnEnd(), message + " columnEnd");
        assertEquals(rowEnd - rowStart, area.rowSpan(), message + " rowSpan");
        assertEquals(columnEnd - columnStart, area.columnSpan(), message + " columnSpan");
    }

    private static void assertRect(Layout layout, float x, float y, float width, float height, String message) {
        assertEquals(x, layout.location().x, EPSILON, message + " x");
        assertEquals(y, layout.location().y, EPSILON, message + " y");
        assertEquals(width, layout.size().width, EPSILON, message + " width");
        assertEquals(height, layout.size().height, EPSILON, message + " height");
    }

    // ------------------------------------------------------------------
    // Explicit tracks
    // ------------------------------------------------------------------

    @Test
    void explicitTracksReportBorderBoxPositionsAndPreserveLayoutNumbers() {
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

        // The old numeric layout is unchanged: content-box origin 2 (border) + 5 (padding) = 7,
        // gap 10 between the tracks, items stretch to their grid areas.
        assertRect(tree.getLayout(root), 0f, 0f, 200f, 74f, "container");
        assertRect(tree.getLayout(a), 7f, 7f, 40f, 30f, "a");
        assertRect(tree.getLayout(b), 57f, 7f, 60f, 30f, "b");
        assertRect(tree.getLayout(c), 7f, 47f, 40f, 20f, "c");
        assertRect(tree.getLayout(d), 57f, 47f, 60f, 20f, "d");

        DetailedGridInfo info = infoOf(tree, root);

        DetailedGridInfo.Tracks columns = info.columns();
        assertEquals(0, columns.negativeImplicitTracks());
        assertEquals(2, columns.explicitTracks());
        assertEquals(0, columns.positiveImplicitTracks());
        assertEquals(2, columns.trackCount());
        assertNull(columns.emptyAxisLine(), "a non-empty axis has no empty-axis line");
        assertTrack(columns.track(0), 7f, 40f, "column 0");
        assertTrack(columns.track(1), 57f, 60f, "column 1");

        DetailedGridInfo.Tracks rows = info.rows();
        assertEquals(0, rows.negativeImplicitTracks());
        assertEquals(2, rows.explicitTracks());
        assertEquals(0, rows.positiveImplicitTracks());
        assertEquals(2, rows.trackCount());
        assertNull(rows.emptyAxisLine(), "a non-empty axis has no empty-axis line");
        assertTrack(rows.track(0), 7f, 30f, "row 0");
        assertTrack(rows.track(1), 47f, 20f, "row 1");

        // Items are in document order and their areas are 1-based full-grid lines.
        assertEquals(4, info.itemCount());
        assertArea(info.itemArea(0), 1, 2, 1, 2, "a");
        assertArea(info.itemArea(1), 1, 2, 2, 3, "b");
        assertArea(info.itemArea(2), 2, 3, 1, 2, "c");
        assertArea(info.itemArea(3), 2, 3, 2, 3, "d");
        assertNull(info.itemArea(4), "out-of-range item index");
    }

    // ------------------------------------------------------------------
    // Implicit tracks
    // ------------------------------------------------------------------

    @Test
    void implicitTracksAreCountedPositionedAndNumberedInTheFullGrid() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(200f, 100f);
        style.gridTemplateColumns = List.of(px(50f));
        style.gridTemplateRows = List.of(px(20f));
        style.gridAutoColumns = List.of(px(25f));
        style.gridAutoRows = List.of(px(15f));

        NodeId auto = tree.newLeaf(leaf());
        TaffyStyle positive = leaf();
        positive.gridColumn = line(3); // OriginZero line 2: the second positive implicit column.
        NodeId positiveItem = tree.newLeaf(positive);
        TaffyStyle negative = leaf();
        negative.gridColumn = line(-3); // OriginZero line -1: the first negative implicit column.
        NodeId negativeItem = tree.newLeaf(negative);
        NodeId root = tree.newWithChildren(style, List.of(auto, positiveItem, negativeItem));

        tree.computeLayout(root, TaffySize.maxContent());

        assertRect(tree.getLayout(auto), 0f, 0f, 25f, 20f, "auto item");
        assertRect(tree.getLayout(positiveItem), 100f, 0f, 25f, 20f, "positive-implicit item");
        assertRect(tree.getLayout(negativeItem), 0f, 20f, 25f, 15f, "negative-implicit item");

        DetailedGridInfo info = infoOf(tree, root);

        DetailedGridInfo.Tracks columns = info.columns();
        assertEquals(1, columns.negativeImplicitTracks());
        assertEquals(1, columns.explicitTracks());
        assertEquals(2, columns.positiveImplicitTracks());
        assertEquals(4, columns.trackCount());
        assertNull(columns.emptyAxisLine(), "columns are not empty");
        assertTrack(columns.track(0), 0f, 25f, "negative implicit column");
        assertTrack(columns.track(1), 25f, 50f, "explicit column");
        assertTrack(columns.track(2), 75f, 25f, "positive implicit column 1");
        assertTrack(columns.track(3), 100f, 25f, "positive implicit column 2");

        DetailedGridInfo.Tracks rows = info.rows();
        assertEquals(0, rows.negativeImplicitTracks());
        assertEquals(1, rows.explicitTracks());
        assertEquals(1, rows.positiveImplicitTracks());
        assertTrack(rows.track(0), 0f, 20f, "explicit row");
        assertTrack(rows.track(1), 20f, 15f, "positive implicit row");

        // Auto-placement starts at the start of the full implicit grid (the first negative
        // implicit track), so the fully auto item occupies the first column line pair. The
        // explicit start is line 2 because one negative implicit column precedes it, and the
        // positive-implicit item starts at the second positive track (line 4).
        assertArea(info.itemArea(0), 1, 2, 1, 2, "auto item");
        assertArea(info.itemArea(1), 1, 2, 4, 5, "positive-implicit item");
        assertArea(info.itemArea(2), 2, 3, 1, 2, "negative-implicit item");
    }

    // ------------------------------------------------------------------
    // Spanning item area
    // ------------------------------------------------------------------

    @Test
    void spanningItemReportsBothLinePairs() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(120f, 100f);
        style.padding = all(5f);
        style.gap = new TaffySize<>(LengthPercentage.length(10f), LengthPercentage.length(10f));
        style.gridTemplateColumns = List.of(px(40f), px(60f));
        style.gridTemplateRows = List.of(px(50f), px(30f));

        TaffyStyle spanStyle = leaf();
        spanStyle.gridColumn = lines(1, 3);
        spanStyle.gridRow = lines(2, 3);
        NodeId span = tree.newLeaf(spanStyle);
        NodeId auto = tree.newLeaf(leaf());
        NodeId root = tree.newWithChildren(style, List.of(span, auto));

        tree.computeLayout(root, TaffySize.maxContent());

        // The spanning item covers both columns of row 2: 40 + 10 + 60 wide, 30 high.
        assertRect(tree.getLayout(span), 5f, 65f, 110f, 30f, "spanning item");
        assertRect(tree.getLayout(auto), 5f, 5f, 40f, 50f, "auto item");

        DetailedGridInfo info = infoOf(tree, root);
        assertEquals(2, info.itemCount());
        assertArea(info.itemArea(0), 2, 3, 1, 3, "spanning item");
        assertArea(info.itemArea(1), 1, 2, 1, 2, "auto item");
        assertTrack(info.rows().track(0), 5f, 50f, "row 0");
        assertTrack(info.rows().track(1), 65f, 30f, "row 1");
    }

    // ------------------------------------------------------------------
    // Empty axis
    // ------------------------------------------------------------------

    @Test
    void fullyCollapsedAxesReportTheSingleLinePositionedByContentAlignment() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(200f, 100f);
        style.padding = all(5f);
        style.border = all(2f);
        style.justifyContent = AlignContent.CENTER;
        style.alignContent = AlignContent.END;
        // Fixed auto-fit tracks with only a hidden child: every expanded track is unoccupied and
        // collapses, so each axis is the single grid line that GR-14 positions by content
        // alignment. The expanded counts (4 columns, 2 rows) are kept in the track list.
        style.gridTemplateColumnsWithRepeat = List.of(GridTemplateComponent.autoFit(px(40f)));
        style.gridTemplateRowsWithRepeat = List.of(GridTemplateComponent.autoFit(px(30f)));

        TaffyStyle hiddenStyle = leaf();
        hiddenStyle.display = TaffyDisplay.NONE;
        NodeId hidden = tree.newLeaf(hiddenStyle);
        NodeId root = tree.newWithChildren(style, hidden);

        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo info = infoOf(tree, root);
        assertEquals(0, info.itemCount(), "display:none children are not grid items");

        DetailedGridInfo.Tracks columns = info.columns();
        assertEquals(4, columns.trackCount());
        assertTrue(columns.hasSingleLine(), "an all-collapsed column axis has a single line");
        for (int i = 0; i < columns.trackCount(); i++) {
            assertEquals(0f, columns.track(i).size(), EPSILON, "collapsed column " + i);
        }
        // Content box: 200 - 2 * (2 border + 5 padding) = 186 wide, origin 7.
        // justify-content: center => 7 + 186 / 2 = 100.
        assertEquals(100f, columns.emptyAxisLine(), EPSILON, "centered column line");

        DetailedGridInfo.Tracks rows = info.rows();
        assertEquals(2, rows.trackCount());
        assertTrue(rows.hasSingleLine(), "an all-collapsed row axis has a single line");
        for (int i = 0; i < rows.trackCount(); i++) {
            assertEquals(0f, rows.track(i).size(), EPSILON, "collapsed row " + i);
        }
        // Content box: 100 - 14 = 86 high, origin 7. align-content: end => 7 + 86 = 93.
        assertEquals(93f, rows.emptyAxisLine(), EPSILON, "end-aligned row line");
    }

    @Test
    void rtlFullyCollapsedAxisLineIsThePhysicalInlineStartEdge() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(200f, 100f);
        style.direction = TaffyDirection.RTL;
        style.padding = all(5f);
        style.border = all(2f);
        style.gridTemplateColumnsWithRepeat = List.of(GridTemplateComponent.autoFit(px(40f)));
        style.gridTemplateRowsWithRepeat = List.of(GridTemplateComponent.autoFit(px(30f)));

        TaffyStyle hiddenStyle = leaf();
        hiddenStyle.display = TaffyDisplay.NONE;
        NodeId hidden = tree.newLeaf(hiddenStyle);
        NodeId root = tree.newWithChildren(style, hidden);
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo info = infoOf(tree, root);
        assertTrue(info.columns().hasSingleLine());
        assertTrue(info.rows().hasSingleLine());
        // The inline axis is reversed: the single line starts at the right content edge
        // (7 + 186 = 193); the block axis is not reversed (7).
        assertEquals(193f, info.columns().emptyAxisLine(), EPSILON, "RTL column line (right edge)");
        assertEquals(7f, info.rows().emptyAxisLine(), EPSILON, "RTL row line (block axis)");
    }

    /**
     * This port's grid-size estimate always materialises at least one implicit track per axis
     * (upstream starts its span estimate at 0, this port at 1). A plain empty axis therefore has
     * one implicit track and normal alignment rather than an {@code emptyAxisLine}; only the
     * all-collapsed auto-fit case reaches the single-line branch. Pinning that behaviour here
     * keeps the difference explicit until the count-estimate residual is addressed.
     */
    @Test
    void plainEmptyAxisMaterialisesOneImplicitTrack() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(100f, 50f);
        TaffyStyle hiddenStyle = leaf();
        hiddenStyle.display = TaffyDisplay.NONE;
        NodeId hidden = tree.newLeaf(hiddenStyle);
        NodeId root = tree.newWithChildren(style, hidden);
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo info = infoOf(tree, root);
        assertEquals(1, info.columns().trackCount());
        assertEquals(1, info.rows().trackCount());
        // With no items the auto column stays at 0; the single auto row absorbs the definite
        // 50px content height (the same numbers the pre-GR-18 layout produced).
        assertEquals(0f, info.columns().track(0).size(), EPSILON);
        assertEquals(50f, info.rows().track(0).size(), EPSILON);
        assertFalse(info.columns().hasSingleLine());
        assertFalse(info.rows().hasSingleLine());
        assertNull(info.columns().emptyAxisLine());
        assertNull(info.rows().emptyAxisLine());
    }

    // ------------------------------------------------------------------
    // RTL tracks
    // ------------------------------------------------------------------

    @Test
    void rtlTracksStayInLogicalOrderButUsePhysicalPositions() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(110f, 60f);
        style.direction = TaffyDirection.RTL;
        style.padding = all(5f);
        style.gridTemplateColumns = List.of(px(40f), px(60f));
        style.gridTemplateRows = List.of(px(50f));

        NodeId item = tree.newLeaf(leaf());
        NodeId root = tree.newWithChildren(style, item);
        tree.computeLayout(root, TaffySize.maxContent());

        // The auto-placed item is in the first logical column, which is physically rightmost.
        assertRect(tree.getLayout(item), 65f, 5f, 40f, 50f, "RTL item");

        DetailedGridInfo info = infoOf(tree, root);
        DetailedGridInfo.Tracks columns = info.columns();
        assertEquals(2, columns.trackCount());
        // Logical order is preserved: index 0 is the first template track (rightmost in RTL).
        assertTrack(columns.track(0), 65f, 40f, "RTL column 0");
        assertTrack(columns.track(1), 5f, 60f, "RTL column 1");
        assertTrack(info.rows().track(0), 5f, 50f, "RTL row 0");
        assertArea(info.itemArea(0), 1, 2, 1, 2, "RTL item");
    }

    // ------------------------------------------------------------------
    // Named lines
    // ------------------------------------------------------------------

    @Test
    void explicitLineNamesAreExposedAndAreaImplicitNamesAreOmitted() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(100f, 50f);
        style.gridTemplateColumns = List.of(px(40f), px(60f));
        style.gridTemplateRows = List.of(px(50f));
        style.gridTemplateColumnNames = List.of(
            new NamedGridLine("main-start", 1),
            new NamedGridLine("main-end", 3));
        style.gridTemplateRowNames = List.of(
            new NamedGridLine("top", 1),
            new NamedGridLine("bottom", 2));
        style.gridTemplateAreas = List.of(new GridTemplateArea("hero", 1, 2, 1, 3));

        TaffyStyle heroStyle = leaf();
        heroStyle.gridColumn = new TaffyLine<>(GridPlacement.namedLine("hero"), GridPlacement.namedLine("hero"));
        heroStyle.gridRow = new TaffyLine<>(GridPlacement.namedLine("hero"), GridPlacement.namedLine("hero"));
        NodeId hero = tree.newLeaf(heroStyle);
        NodeId root = tree.newWithChildren(style, hero);
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo info = infoOf(tree, root);
        assertEquals(List.of(1), info.columns().linesNamed("main-start"));
        assertEquals(List.of(3), info.columns().linesNamed("main-end"));
        assertEquals(List.of(1), info.rows().linesNamed("top"));
        assertEquals(List.of(2), info.rows().linesNamed("bottom"));
        assertEquals(List.of(), info.columns().linesNamed("missing"));

        // #1185: grid-template-areas implicitly creates hero-start/hero-end lines for placement,
        // but those names are not part of the explicit line-name table (they are not declared on
        // the track list; browsers omit them from the resolved track list as well).
        assertFalse(info.columns().lineNames().containsKey("hero-start"));
        assertFalse(info.columns().lineNames().containsKey("hero-end"));
        assertFalse(info.rows().lineNames().containsKey("hero-start"));
        assertFalse(info.rows().lineNames().containsKey("hero-end"));
        assertEquals(List.of("main-end", "main-start"), List.copyOf(info.columns().lineNames().keySet()));

        // ...while placement still resolves the area edges (the area table is untouched).
        assertArea(info.itemArea(0), 1, 2, 1, 3, "hero item");

        // Immutable snapshots: neither the map nor the item list can be mutated.
        assertThrows(UnsupportedOperationException.class,
            () -> info.columns().lineNames().put("x", List.of(1)));
        assertThrows(UnsupportedOperationException.class,
            () -> info.columns().lineNames().get("main-start").add(2));
        assertThrows(UnsupportedOperationException.class,
            () -> info.items().add(new GridItemArea(1, 2, 1, 2)));
    }

    @Test
    void repeatPositionalLineNamesAreExplicitNames() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle style = sizedGrid(100f, 50f);
        style.gridTemplateColumnsWithRepeat = List.of(
            GridTemplateComponent.repeat(GridRepetition.count(2, List.of(px(50f)),
                List.of(List.of("col-start"), List.of("col-end")))));
        style.gridTemplateRows = List.of(px(50f));

        NodeId item = tree.newLeaf(leaf());
        NodeId root = tree.newWithChildren(style, item);
        tree.computeLayout(root, TaffySize.maxContent());

        DetailedGridInfo info = infoOf(tree, root);
        assertEquals(2, info.columns().explicitTracks());
        // repeat(2, [col-start] 50px [col-end]) yields three lines: 1 start, 2 shared
        // between the repetitions, 3 end.
        assertEquals(List.of(1, 2), info.columns().linesNamed("col-start"));
        assertEquals(List.of(2, 3), info.columns().linesNamed("col-end"));
        assertTrack(info.columns().track(0), 0f, 50f, "repeat column 0");
        assertTrack(info.columns().track(1), 50f, 50f, "repeat column 1");
    }

    // ------------------------------------------------------------------
    // Non-grid / unknown / not-laid-out nodes
    // ------------------------------------------------------------------

    @Test
    void nonGridNodesReturnNull() {
        TaffyTree tree = new TaffyTree();
        TaffyStyle flexStyle = new TaffyStyle();
        flexStyle.display = TaffyDisplay.FLEX;
        NodeId child = tree.newLeaf(leaf());
        NodeId root = tree.newWithChildren(flexStyle, child);
        tree.computeLayout(root, TaffySize.maxContent());

        assertNull(tree.getDetailedGridInfo(root), "flex container");
        assertNull(tree.getDetailedGridInfo(child), "leaf");
        assertNull(tree.getDetailedGridInfo(NodeId.of(9_999)), "unknown node");

        // A grid node that is restyled as flex immediately reports null even though the stored
        // snapshot is only replaced by the next grid layout.
        NodeId restyled = tree.newWithChildren(sizedGrid(100f, 50f), tree.newLeaf(leaf()));
        tree.computeLayout(restyled, TaffySize.maxContent());
        assertNotNull(tree.getDetailedGridInfo(restyled), "grid before restyle");
        tree.setStyle(restyled, flexStyle);
        assertNull(tree.getDetailedGridInfo(restyled), "display:flex after restyle");
    }

    @Test
    void gridAreaValueObjectRejectsInvalidLines() {
        assertEquals(1, new GridItemArea(1, 2, 1, 2).rowSpan());
        assertEquals(2, new GridItemArea(1, 2, 1, 3).columnSpan());
        assertThrows(IllegalArgumentException.class, () -> new GridItemArea(0, 1, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> new GridItemArea(2, 1, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> new GridItemArea(1, 2, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> new GridItemArea(1, 2, 2, 2));
    }

    @Test
    void gridNodeWithoutCompletedLayoutOrWithoutChildrenReturnsNull() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(leaf());
        NodeId root = tree.newWithChildren(sizedGrid(100f, 50f), child);

        assertNull(tree.getDetailedGridInfo(root), "before computeLayout");

        tree.computeLayout(root, TaffySize.maxContent());
        assertNotNull(tree.getDetailedGridInfo(root), "after computeLayout");

        // A childless grid is dispatched to the measured-leaf path in this port, so the grid
        // algorithm never records detailed information for it.
        NodeId childless = tree.newLeaf(sizedGrid(100f, 50f));
        tree.computeLayout(childless, TaffySize.maxContent());
        assertNull(tree.getDetailedGridInfo(childless), "childless grid node");
    }

    @Test
    void detailedInfoIsPrunedWithItsNode() {
        TaffyTree tree = new TaffyTree();
        NodeId child = tree.newLeaf(leaf());
        NodeId root = tree.newWithChildren(sizedGrid(100f, 50f), child);
        tree.computeLayout(root, TaffySize.maxContent());
        assertNotNull(tree.getDetailedGridInfo(root));

        tree.remove(root);
        assertNull(tree.getDetailedGridInfo(root), "removed node");
    }
}
