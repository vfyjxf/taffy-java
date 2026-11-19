package dev.vfyjxf.taffy.tree;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Detailed information recorded by the most recent layout pass of a {@code display: grid}
 * container.
 *
 * <p>This is the Java port of upstream taffy's {@code detailed_layout_info} API (GR-18:
 * #899/#1131/#1134/#1136/#1137/#1185/#1280/#1167). The first slice (commit
 * {@code 5ce0d05}) added the per-track positions, the in-flow item grid areas and the explicit
 * line-name table; this second slice adds the upstream CSR (compressed sparse row) line-name
 * structure, the separately tracked {@code grid-template-areas} edge table, the physical item
 * rectangle of #1134 and the resolved track-list serialization with the #1280 {@code zoom}
 * argument. The whole API is additive and immutable: reading it never changes a computed
 * layout, and the layout algorithms record it without changing any layout number.</p>
 *
 * <h2>What is exposed</h2>
 * <ul>
 *   <li>{@link #rows()} and {@link #columns()}: the resolved tracks of each axis in logical
 *       order, with each track's physical start/size in the container's border-box coordinate
 *       space, plus the implicit-track counts, the single-line position of an empty axis and the
 *       explicit line-name table ({@link Tracks#lineNames()}, {@link Tracks#lineNameTable()}).</li>
 *   <li>{@link #items()}: the grid area (1-based full-grid line numbers) of every in-flow,
 *       non-{@code display:none} child, in document order. Out-of-flow children are not part of
 *       grid placement and are therefore absent, matching upstream's {@code DetailedGridInfo.items};
 *       {@link #itemRectangle(int)} resolves the physical rectangle of that area.</li>
 *   <li>{@link #areas()}: the {@code grid-template-areas} edge table (and the areas synthesised
 *       from explicitly declared {@code <area>-start}/{@code <area>-end} line names), kept
 *       separate from the line-name table.</li>
 *   <li>The resolved {@code grid-template-rows}/{@code grid-template-columns} value (track
 *       sizes and per-line names) with the CSS {@code zoom} divisor, see
 *       {@code Tracks#toTrackListString(float)}.</li>
 * </ul>
 *
 * <p>All values are unrounded. The grid algorithm records this information before the final
 * layout of the items, so the values describe the same geometry the container's unrounded
 * layout is built from; {@code RoundLayout} (applied afterwards when rounding is enabled) does
 * not mutate them.</p>
 *
 * <h2>Line names are stored as CSR</h2>
 * <p>Upstream stores the names of each explicit grid line in a CSR ({@code Vec<name>} plus
 * {@code Vec<offsets>}) and keeps a second map from a name to the lines carrying it for
 * placement. {@link LineNames} has the same two structures: the per-line view ({@link
 * LineNames#line(int)}, {@link LineNames#lineCount()}) and the name-to-lines view ({@link
 * LineNames#linesNamed(String)}, {@link LineNames#nameLineStart(String)}, {@link
 * LineNames#nameLineCount(String)}). It also implements {@code Map<String, List<Integer>>} so
 * that the first-slice accessors ({@link Tracks#lineNames()}, {@link Tracks#linesNamed(String)})
 * keep their exact previous behaviour and return type; the record component of {@link Tracks} is
 * normalized to the CSR representation in its constructor.</p>
 *
 * <p>Only names declared on the track template (including {@code repeat()} positional name sets)
 * are part of the line-name CSR. The implicit {@code <area>-start}/{@code <area>-end} names that
 * {@code grid-template-areas} generates are deliberately <em>not</em> part of it (browsers omit
 * them from the resolved track list, #1185); they remain available for placement through the
 * grid algorithm and are exposed separately through {@link #areas()}. Explicitly declared names
 * that happen to end in {@code -start}/{@code -end} are ordinary explicit names and are included.
 * </p>
 *
 * <p>The per-line order of names that come from different sources is the declaration order of
 * the two Java style inputs (the flat {@code gridTemplateColumnNames}/{@code gridTemplateRowNames}
 * lists first, then the positional {@code repeat()} name sets in expansion order). Unlike
 * upstream, the Java style API is a flattened {@code (name, line)} list and therefore does not
 * preserve the exact interleaving of line-name sets and {@code repeat()} components that the
 * upstream {@code Vec<Vec<String>>} iterator encodes.</p>
 *
 * <h2>Lifecycle and validity</h2>
 * <p>The information belongs to the node and is replaced by the next completed grid layout pass.
 * Style or child changes do not clear it eagerly (matching upstream, where the detailed layout
 * info is a slot on the node); read it after {@code computeLayout} to obtain the current values.
 * {@link TaffyTree#getDetailedGridInfo(NodeId)} returns {@code null} for a node that does not
 * exist, for a node that is not currently {@code display: grid}, and for a grid node that has
 * not been through the grid layout path yet (including a childless grid node, which this port
 * dispatches to the measured-leaf path). The getter returns {@code null} rather than an empty
 * value object so that callers cannot confuse "grid was not laid out" with a real empty grid;
 * this API choice is part of the GR-18 first-slice contract.</p>
 *
 * <p>Immutable: the record defensively copies its lists and maps, and every nested value is
 * immutable as well.</p>
 *
 * @param rows    resolved row-axis tracks, line names and area edges
 * @param columns resolved column-axis tracks, line names and area edges
 * @param items   grid areas of the in-flow items, in document order
 * @param areas   the named grid-area edge table, sorted by area name
 */
public record DetailedGridInfo(Tracks rows, Tracks columns, List<GridItemArea> items,
                               Map<String, GridArea> areas) {

    public DetailedGridInfo {
        Objects.requireNonNull(rows, "rows");
        Objects.requireNonNull(columns, "columns");
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(areas, "areas");
        items = List.copyOf(items);
        TreeMap<String, GridArea> copiedAreas = new TreeMap<>();
        for (Map.Entry<String, GridArea> entry : areas.entrySet()) {
            copiedAreas.put(Objects.requireNonNull(entry.getKey(), "area name"),
                Objects.requireNonNull(entry.getValue(), "area " + entry.getKey()));
        }
        areas = Collections.unmodifiableMap(copiedAreas);
    }

    /**
     * Creates detailed information without a grid-area edge table.
     *
     * @param rows    resolved row-axis tracks
     * @param columns resolved column-axis tracks
     * @param items   grid areas of the in-flow items
     */
    public DetailedGridInfo(Tracks rows, Tracks columns, List<GridItemArea> items) {
        this(rows, columns, items, Map.of());
    }

    /** The number of in-flow items with a recorded grid area. */
    public int itemCount() {
        return items.size();
    }

    /**
     * The grid area of the item at {@code itemIndex}, where {@code 0} is the first in-flow,
     * non-hidden child in document order.
     *
     * @return the area, or {@code null} when {@code itemIndex} is out of range (mirroring
     *         upstream's {@code Option} return)
     */
    public GridItemArea itemArea(int itemIndex) {
        if (itemIndex < 0 || itemIndex >= items.size()) {
            return null;
        }
        return items.get(itemIndex);
    }

    /**
     * The physical rectangle of the grid area occupied by the item at {@code itemIndex}, in the
     * grid container's border-box coordinate space (upstream
     * {@code DetailedGridInfo::item_grid_area}, #1134).
     *
     * <p>The edges resolve to the edges of the tracks bounding the item's area: a start line
     * resolves to the start of the first track that follows it and an end line to the end of the
     * last track that precedes it, so the rectangle excludes any gutter or content-alignment
     * space around the area. Tracks are stored in logical order but their positions are physical,
     * so in an RTL container the first and last bounding tracks can have swapped physical starts;
     * the rectangle takes the minimum physical start and maximum physical end (the same min/max
     * normalisation upstream applies). All values are unrounded, like the track positions.</p>
     *
     * <p>Only in-flow items are recorded (out-of-flow children are not grid items), matching
     * upstream. Resolving an out-of-flow child's area from the grid placement properties is a
     * later GR-18 slice.</p>
     *
     * @param itemIndex index into {@link #items()}; {@code 0} is the first in-flow child
     * @return the rectangle, or {@code null} when {@code itemIndex} is out of range (mirroring
     *         upstream's {@code Option} return)
     * @throws IndexOutOfBoundsException if the recorded area references a line outside the
     *                                   recorded tracks (an inconsistent snapshot)
     */
    public ItemRectangle itemRectangle(int itemIndex) {
        GridItemArea area = itemArea(itemIndex);
        if (area == null) {
            return null;
        }
        GridTrackInfo firstColumn = columns.track(area.columnStart() - 1);
        GridTrackInfo lastColumn = columns.track(area.columnEnd() - 2);
        float left = Math.min(firstColumn.start(), lastColumn.start());
        float right = Math.max(firstColumn.end(), lastColumn.end());
        GridTrackInfo firstRow = rows.track(area.rowStart() - 1);
        GridTrackInfo lastRow = rows.track(area.rowEnd() - 2);
        float top = firstRow.start();
        float bottom = lastRow.end();
        return new ItemRectangle(left, top, right - left, bottom - top);
    }

    /**
     * The resolved tracks and line names of one axis.
     *
     * <p>{@code tracks} lists every track of the full grid (negative implicit, explicit, then
     * positive implicit) in logical order. Each item's start/size is in the grid container's
     * border-box coordinate space; see {@link GridTrackInfo}. A track collapsed by
     * {@code auto-fit} is present with {@code size == 0}, so the list always has
     * {@code negativeImplicitTracks + explicitTracks + positiveImplicitTracks} entries.</p>
     *
     * <p>{@code emptyAxisLine} is {@code null} when the axis has at least one non-collapsed
     * track. When it has none &mdash; every track collapsed by {@code auto-fit}, or no tracks at
     * all in a tree not subject to this port's implicit-track floor &mdash; it is the physical
     * position of the axis's single grid line relative to the container's border box, with
     * content alignment applied. For example {@code justify-content: start} in LTR puts the line
     * at the content-box start edge; in RTL, where {@code start} is the inline start, it is at the
     * content-box right edge. This port's grid-size estimate materialises one zero-size implicit
     * track for an otherwise empty axis, so a plain empty axis reports that track through the
     * normal alignment path; the fully {@code auto-fit}-collapsed axis is the observable
     * single-line case and the one the absolutely positioned path uses (GR-14). Upstream's
     * {@code DetailedGridTracksInfo.empty_axis_line} (#1167) is the zero-track equivalent.</p>
     *
     * <p>{@code lineNames} is the first-slice explicit line-name table, kept for compatibility:
     * it maps each <em>explicit</em> line name to its 1-based CSS grid line numbers (ascending,
     * duplicates removed). It is backed by the CSR {@link #lineNameTable()}; new code should use
     * that accessor directly. Line numbers use the style convention: line 1 is the first explicit
     * line, and a value before the explicit grid is negative. Names generated implicitly by
     * {@code grid-template-areas} are deliberately absent from the line-name table, matching the
     * browser serialization rule of #1185; those edges are in {@link DetailedGridInfo#areas()}
     * instead. Explicitly declared names, including repeat() positional name sets and explicitly
     * declared area-edge-looking names, are included.</p>
     *
     * <p>Immutable: the track list, the CSR table and every name list are unmodifiable.</p>
     *
     * @param negativeImplicitTracks number of tracks in the implicit grid before the explicit grid
     * @param explicitTracks         number of explicit tracks
     * @param positiveImplicitTracks number of tracks in the implicit grid after the explicit grid
     * @param tracks                 start/size of every track in logical order
     * @param emptyAxisLine          single-line position of an axis without non-collapsed tracks,
     *                               or {@code null} when the axis has a usable track
     * @param lineNames              explicit line names to their 1-based CSS line numbers (backed
     *                               by a {@link LineNames} CSR instance)
     */
    public record Tracks(
        int negativeImplicitTracks,
        int explicitTracks,
        int positiveImplicitTracks,
        List<GridTrackInfo> tracks,
        Float emptyAxisLine,
        Map<String, List<Integer>> lineNames
    ) {

        public Tracks {
            if (negativeImplicitTracks < 0 || explicitTracks < 0 || positiveImplicitTracks < 0) {
                throw new IllegalArgumentException(
                    "grid track counts must be non-negative, got negativeImplicit=" + negativeImplicitTracks
                        + ", explicit=" + explicitTracks + ", positiveImplicit=" + positiveImplicitTracks);
            }
            Objects.requireNonNull(tracks, "tracks");
            Objects.requireNonNull(lineNames, "lineNames");
            tracks = List.copyOf(tracks);
            // Normalise the legacy Map component to the CSR representation. This keeps the
            // component type (and therefore the public accessor) unchanged while making the CSR
            // the single stored form (upstream GridLineNames).
            lineNames = LineNames.of(lineNames, explicitTracks);
        }

        /** The total number of tracks in the axis (negative implicit + explicit + positive implicit). */
        public int trackCount() {
            return negativeImplicitTracks + explicitTracks + positiveImplicitTracks;
        }

        /** Whether the axis has no tracks at all (and therefore one grid line). */
        public boolean isEmpty() {
            return trackCount() == 0;
        }

        /**
         * Whether the axis behaves as a single grid line positioned by content alignment: it has
         * no non-collapsed track, so {@link #emptyAxisLine()} is non-null. See the class Javadoc
         * for the difference between a zero-track axis and an all-collapsed {@code auto-fit} axis
         * in this port.
         */
        public boolean hasSingleLine() {
            return emptyAxisLine != null;
        }

        /**
         * The track at {@code index} in logical order.
         *
         * @throws IndexOutOfBoundsException if {@code index} is outside the axis
         */
        public GridTrackInfo track(int index) {
            return tracks.get(index);
        }

        /**
         * The 1-based CSS line numbers carrying {@code name}, in ascending order, or an empty
         * list when the name is not declared on this axis.
         */
        public List<Integer> linesNamed(String name) {
            return lineNameTable().linesNamed(name);
        }

        /**
         * The CSR line-name table of this axis (upstream {@code GridLineNames}).
         *
         * <p>Line indices are relative to the explicit grid: line 0 is the first explicit line.
         * Use {@link #namesForLine(int)} for an index relative to the full grid (including the
         * implicit tracks), matching upstream's {@code names_for_line}.</p>
         */
        public LineNames lineNameTable() {
            return (LineNames) lineNames;
        }

        /**
         * The names of the grid line with the passed 0-indexed line index of the <em>full</em>
         * grid, where line {@code i} bounds the start of track {@code i}. Returns an empty list
         * for an implicit line or an index outside the full grid (upstream
         * {@code DetailedGridTracksInfo.names_for_line}).
         */
        public List<String> namesForLine(int lineIndex) {
            return lineNameTable().line(lineIndex - negativeImplicitTracks);
        }

        /**
         * The name groups of every line of the full grid (including implicit tracks) in line
         * order. Returns an empty list when the axis has no named lines at all (upstream
         * {@code iter_line_names}).
         */
        public List<List<String>> iterLineNames() {
            LineNames table = lineNameTable();
            if (table.isEmpty()) {
                return List.of();
            }
            int lineCount = trackCount() + 1;
            List<List<String>> lines = new ArrayList<>(lineCount);
            for (int i = 0; i < lineCount; i++) {
                lines.add(namesForLine(i));
            }
            return List.copyOf(lines);
        }
    }

    /**
     * A named grid area, expressed as 1-based grid line numbers relative to the explicit grid.
     *
     * <p>This mirrors one entry of upstream's private {@code GridAreasMap} (the area edges
     * {@code grid-template-areas} creates, or that an explicitly declared
     * {@code <area>-start}/{@code <area>-end} line name synthesises). A value of {@code 0} means
     * that edge has not been named: {@code grid-template-areas} always sets all four edges, while
     * an area synthesised from a lone explicit {@code foo-start} line has {@code rowEnd == 0} or
     * {@code columnEnd == 0} until the matching {@code foo-end} name is seen.</p>
     *
     * <p>The values are style-side lines, not full-grid lines: they are the same 1-based,
     * explicit-relative numbers used by {@code grid-template-areas}. See
     * {@link DetailedGridInfo#areas()}.</p>
     *
     * @param rowStart    first row line of the area (0 when unknown)
     * @param columnStart first column line of the area (0 when unknown)
     * @param rowEnd      row line after the area's last row (0 when unknown)
     * @param columnEnd   column line after the area's last column (0 when unknown)
     */
    public record GridArea(int rowStart, int columnStart, int rowEnd, int columnEnd) {
    }

    /**
     * The rectangle of one in-flow item's grid area, relative to the grid container's border box
     * (upstream's {@code (Point<f32>, Size<f32>)} return of {@code item_grid_area}).
     *
     * <p>{@code x}/{@code y} are the physical left/top edges (in LTR and RTL alike, because the
     * source track positions are already physical), and {@code width}/{@code height} are the
     * distance between the outer edges of the bounding tracks. The values are unrounded and in
     * CSS pixels.</p>
     *
     * @param x      physical left edge relative to the container's border box
     * @param y      physical top edge relative to the container's border box
     * @param width  width of the area, including any gutters between spanned tracks
     * @param height height of the area, including any gutters between spanned tracks
     */
    public record ItemRectangle(float x, float y, float width, float height) {

        /** The physical right edge: {@code x + width}. */
        public float right() {
            return x + width;
        }

        /** The physical bottom edge: {@code y + height}. */
        public float bottom() {
            return y + height;
        }
    }

    /**
     * The CSR (compressed sparse row) line-name table of one axis (upstream
     * {@code GridLineNames}).
     *
     * <p>Two compressed views are stored:</p>
     * <ul>
     *   <li><b>line to names</b>: a flat array of names plus {@code offsets}, where line
     *       {@code i}'s names are {@code names[offsets[i] .. offsets[i + 1])}. The offsets are
     *       relative to the explicit grid (line 0 is the first explicit line).</li>
     *   <li><b>name to lines</b>: the names in sorted order plus {@code start}/{@code count}
     *       pairs into a flat array of 1-based CSS line numbers. This is the lookup the resolver
     *       uses and the backing storage for the first-slice {@code Map} accessor.</li>
     * </ul>
     *
     * <p>This class implements {@code Map<String, List<Integer>>} so that
     * {@link Tracks#lineNames()} keeps its previous return type and semantics: an immutable map
     * with sorted keys, whose values are immutable ascending line lists. Mutating methods throw
     * {@link UnsupportedOperationException}. A grid without any named line is represented by an
     * empty table (as upstream does); see {@link #isEmpty()}.</p>
     */
    public static final class LineNames extends AbstractMap<String, List<Integer>> {

        private static final LineNames EMPTY =
            new LineNames(new String[0], new int[0], new String[0], new int[0], new int[0], new int[0]);

        /** Flat name storage, in explicit line order. */
        private final String[] names;

        /** Line CSR offsets into {@link #names}; empty, or {@code lineCount + 1} entries. */
        private final int[] offsets;

        /** The names of this axis in sorted order (keys of the name CSR). */
        private final String[] sortedNames;

        /** Start index of each name's line run in {@link #lineNumbers}. */
        private final int[] nameLineStart;

        /** Number of lines of each name in {@link #lineNumbers}. */
        private final int[] nameLineCount;

        /** Flat 1-based CSS line numbers, grouped by {@link #sortedNames}. */
        private final int[] lineNumbers;

        /** Lazily built, immutable {@code entrySet} view. */
        private transient Set<Entry<String, List<Integer>>> entries;

        private LineNames(String[] names, int[] offsets, String[] sortedNames, int[] nameLineStart,
                          int[] nameLineCount, int[] lineNumbers) {
            this.names = names;
            this.offsets = offsets;
            this.sortedNames = sortedNames;
            this.nameLineStart = nameLineStart;
            this.nameLineCount = nameLineCount;
            this.lineNumbers = lineNumbers;
        }

        /**
         * The empty table: no line names on the axis.
         */
        public static LineNames empty() {
            return EMPTY;
        }

        /**
         * Normalises a first-slice line-name map into the CSR representation.
         *
         * <p>The per-line order is the sorted name order (the old {@code TreeMap} iteration
         * order), because a plain map does not carry declaration order. The grid algorithm passes
         * {@link #fromLineGroups(List, Map)} instead, which preserves the declaration order of
         * the template.</p>
         *
         * @param lineNames      name to 1-based CSS line numbers
         * @param explicitTracks the number of explicit tracks of the axis, used to size the
         *                       per-line view when the map has no name on the trailing line
         */
        public static LineNames of(Map<String, List<Integer>> lineNames, int explicitTracks) {
            Objects.requireNonNull(lineNames, "lineNames");
            if (lineNames instanceof LineNames csr) {
                return csr;
            }
            if (lineNames.isEmpty()) {
                return EMPTY;
            }
            return build(deriveLineGroups(lineNames, explicitTracks), lineNames);
        }

        /**
         * Builds the CSR from the declaration-ordered name groups of each explicit line plus the
         * name-to-lines map retained by the grid algorithm.
         *
         * <p>This is the constructor the grid pass uses: {@code groups} is the upstream
         * {@code Vec<(line, name)>} collapsed into one name list per explicit line (line 0 is the
         * first explicit line), while {@code lineNames} is the placement lookup. The two inputs
         * describe the same declared names; the map additionally carries the area-edge lines the
         * resolver seeds for explicitly declared {@code <area>-start}/{@code <area>-end} names.
         * Those seeded lines belong to the placement table only and are deliberately not added to
         * the per-line serialization view, matching upstream's explicit-line-name pairs.</p>
         *
         * @param groups     per explicit line name groups in declaration order; empty when the
         *                   grid has no named lines
         * @param lineNames  name to 1-based CSS line numbers (the placement lookup)
         */
        public static LineNames fromLineGroups(List<List<String>> groups,
                                               Map<String, List<Integer>> lineNames) {
            Objects.requireNonNull(groups, "groups");
            Objects.requireNonNull(lineNames, "lineNames");
            if (lineNames instanceof LineNames csr) {
                return csr;
            }
            if (lineNames.isEmpty()) {
                return EMPTY;
            }
            return build(groups, lineNames);
        }

        private static LineNames build(List<List<String>> groups, Map<String, List<Integer>> lineNames) {
            TreeMap<String, List<Integer>> sorted = normalize(lineNames);

            int maxLine = 0;
            for (List<Integer> lines : sorted.values()) {
                for (int line : lines) {
                    maxLine = Math.max(maxLine, line);
                }
            }
            int groupLineCount = groups == null ? 0 : groups.size();
            int lineCount = Math.max(groupLineCount, maxLine);

            // Line CSR: keep the declaration order of the groups for the lines they cover; for
            // lines only present in the map, append the names in sorted key order.
            List<List<String>> perLine = new ArrayList<>(lineCount);
            for (int i = 0; i < lineCount; i++) {
                perLine.add(new ArrayList<>());
            }
            int copiedGroups = Math.min(lineCount, groupLineCount);
            for (int i = 0; i < copiedGroups; i++) {
                List<String> group = groups.get(i);
                if (group == null) {
                    continue;
                }
                List<String> target = perLine.get(i);
                for (String name : group) {
                    if (name != null && !name.isEmpty() && !target.contains(name)) {
                        target.add(name);
                    }
                }
            }
            for (Map.Entry<String, List<Integer>> entry : sorted.entrySet()) {
                for (int line : entry.getValue()) {
                    if (line < 1 || line > lineCount || line <= copiedGroups) {
                        continue;
                    }
                    List<String> target = perLine.get(line - 1);
                    if (!target.contains(entry.getKey())) {
                        target.add(entry.getKey());
                    }
                }
            }

            int nameCount = 0;
            for (List<String> line : perLine) {
                nameCount += line.size();
            }
            String[] flatNames = new String[nameCount];
            int[] flatOffsets = new int[lineCount + 1];
            int flatIndex = 0;
            for (int i = 0; i < lineCount; i++) {
                flatOffsets[i] = flatIndex;
                for (String name : perLine.get(i)) {
                    flatNames[flatIndex++] = name;
                }
            }
            flatOffsets[lineCount] = flatIndex;

            // Name CSR: sorted names plus start/count into the flat line-number array.
            String[] sortedNames = sorted.keySet().toArray(new String[0]);
            int[] nameLineStart = new int[sortedNames.length];
            int[] nameLineCount = new int[sortedNames.length];
            int totalLines = 0;
            for (List<Integer> lines : sorted.values()) {
                totalLines += lines.size();
            }
            int[] lineNumbers = new int[totalLines];
            int lineIndex = 0;
            for (int i = 0; i < sortedNames.length; i++) {
                List<Integer> lines = sorted.get(sortedNames[i]);
                nameLineStart[i] = lineIndex;
                nameLineCount[i] = lines.size();
                for (int line : lines) {
                    lineNumbers[lineIndex++] = line;
                }
            }

            return new LineNames(flatNames, flatOffsets, sortedNames, nameLineStart, nameLineCount, lineNumbers);
        }

        /**
         * The number of explicit grid lines represented by the per-line view (0 when the axis has
         * no named lines), upstream {@code GridLineNames::line_count}.
         */
        public int lineCount() {
            return Math.max(0, offsets.length - 1);
        }

        /** Whether the axis has no names at all (upstream {@code GridLineNames::is_empty}). */
        public boolean isEmpty() {
            return names.length == 0 && sortedNames.length == 0;
        }

        /**
         * The names of the explicit line at {@code lineIndex} (0-based), in declaration order.
         * Returns an empty list for an unnamed line or an index outside the explicit grid.
         */
        public List<String> line(int lineIndex) {
            if (lineIndex < 0 || lineIndex >= lineCount()) {
                return List.of();
            }
            int from = offsets[lineIndex];
            int to = offsets[lineIndex + 1];
            return List.of(Arrays.copyOfRange(names, from, to));
        }

        /**
         * Every explicit line's name group in line order, as an immutable list.
         */
        public List<List<String>> lines() {
            List<List<String>> lines = new ArrayList<>(lineCount());
            for (int i = 0; i < lineCount(); i++) {
                lines.add(line(i));
            }
            return List.copyOf(lines);
        }

        /** The number of distinct names of the axis. */
        public int nameCount() {
            return sortedNames.length;
        }

        /**
         * The {@code index}-th name in sorted order.
         *
         * @throws IndexOutOfBoundsException if {@code index} is outside the name table
         */
        public String nameAt(int index) {
            return sortedNames[index];
        }

        /**
         * The 1-based CSS line numbers carrying {@code name}, in ascending order, or an empty
         * list when the name is unknown.
         */
        @Override
        public List<Integer> get(Object name) {
            int index = indexOfName(name);
            if (index < 0) {
                return null;
            }
            return linesNamed(sortedNames[index]);
        }

        /** Whether {@code name} is declared on this axis. */
        @Override
        public boolean containsKey(Object name) {
            return indexOfName(name) >= 0;
        }

        @Override
        public int size() {
            return sortedNames.length;
        }

        /**
         * The line numbers of {@code name}, in ascending order (an empty list for an unknown
         * name).
         */
        public List<Integer> linesNamed(String name) {
            int index = indexOfName(name);
            if (index < 0) {
                return List.of();
            }
            int from = nameLineStart[index];
            int count = nameLineCount[index];
            int[] copy = Arrays.copyOfRange(lineNumbers, from, from + count);
            List<Integer> boxed = new ArrayList<>(copy.length);
            for (int line : copy) {
                boxed.add(line);
            }
            return List.copyOf(boxed);
        }

        /**
         * The start index of {@code name}'s run in the flat line-number array, or {@code -1} when
         * the name is unknown. Together with {@link #nameLineCount(String)} and
         * {@link #lineNumberAt(int)} this exposes the name-to-lines CSR directly.
         */
        public int nameLineStart(String name) {
            int index = indexOfName(name);
            return index < 0 ? -1 : nameLineStart[index];
        }

        /**
         * The number of lines carrying {@code name}, or {@code 0} when the name is unknown.
         */
        public int nameLineCount(String name) {
            int index = indexOfName(name);
            return index < 0 ? 0 : nameLineCount[index];
        }

        /**
         * The 1-based CSS line number stored at {@code flatIndex} of the name CSR.
         *
         * @throws IndexOutOfBoundsException if {@code flatIndex} is outside the flat line array
         */
        public int lineNumberAt(int flatIndex) {
            return lineNumbers[flatIndex];
        }

        @Override
        public Set<Entry<String, List<Integer>>> entrySet() {
            Set<Entry<String, List<Integer>>> cached = entries;
            if (cached == null) {
                List<Entry<String, List<Integer>>> list = new ArrayList<>(sortedNames.length);
                for (String name : sortedNames) {
                    list.add(new SimpleImmutableEntry<>(name, linesNamed(name)));
                }
                cached = Collections.unmodifiableSet(
                    new java.util.LinkedHashSet<>(list));
                entries = cached;
            }
            return cached;
        }

        private int indexOfName(Object name) {
            if (!(name instanceof String string)) {
                return -1;
            }
            int low = 0;
            int high = sortedNames.length - 1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                int comparison = sortedNames[mid].compareTo(string);
                if (comparison < 0) {
                    low = mid + 1;
                } else if (comparison > 0) {
                    high = mid - 1;
                } else {
                    return mid;
                }
            }
            return -1;
        }

        private static TreeMap<String, List<Integer>> normalize(Map<String, List<Integer>> lineNames) {
            TreeMap<String, List<Integer>> sorted = new TreeMap<>();
            for (Map.Entry<String, List<Integer>> entry : lineNames.entrySet()) {
                String name = Objects.requireNonNull(entry.getKey(), "line name");
                Objects.requireNonNull(entry.getValue(), "lines for " + name);
                List<Integer> lines = entry.getValue();
                List<Integer> copy = new ArrayList<>(lines.size());
                for (Integer line : lines) {
                    copy.add(Objects.requireNonNull(line, "line number for " + name));
                }
                copy.sort(Integer::compareTo);
                for (int i = copy.size() - 1; i > 0; i--) {
                    if (copy.get(i).equals(copy.get(i - 1))) {
                        copy.remove(i);
                    }
                }
                sorted.put(name, List.copyOf(copy));
            }
            return sorted;
        }

        private static List<List<String>> deriveLineGroups(Map<String, List<Integer>> lineNames,
                                                           int explicitTracks) {
            TreeMap<String, List<Integer>> sorted = normalize(lineNames);
            int maxLine = 0;
            for (List<Integer> lines : sorted.values()) {
                for (int line : lines) {
                    maxLine = Math.max(maxLine, line);
                }
            }
            int lineCount = Math.max(Math.max(0, explicitTracks) + 1, maxLine);
            List<List<String>> groups = new ArrayList<>(lineCount);
            for (int i = 0; i < lineCount; i++) {
                groups.add(new ArrayList<>());
            }
            for (Map.Entry<String, List<Integer>> entry : sorted.entrySet()) {
                for (int line : entry.getValue()) {
                    if (line >= 1 && line <= lineCount) {
                        groups.get(line - 1).add(entry.getKey());
                    }
                }
            }
            return groups;
        }
    }
}
