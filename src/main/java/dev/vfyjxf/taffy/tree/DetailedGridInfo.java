package dev.vfyjxf.taffy.tree;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Detailed information recorded by the most recent layout pass of a {@code display: grid}
 * container.
 *
 * <p>This is the first slice of the Java port of upstream taffy's
 * {@code detailed_layout_info} API (GR-18: #899/#1131/#1134/#1136/#1137/#1185/#1280/#1167). It
 * is an additive, immutable value object: reading it never changes a computed layout, and the
 * layout algorithms record it without changing any layout number. A later slice will add the
 * resolved track-list serialization (with the {@code zoom} argument and its #1185 rule for
 * omitting implicit {@code grid-template-areas} line names), the CSR per-line name structure,
 * the grid-area edge table, and absolute-area resolution.</p>
 *
 * <h2>What is exposed</h2>
 * <ul>
 *   <li>{@link #rows()} and {@link #columns()}: the resolved tracks of each axis in logical
 *       order, with each track's physical start/size in the container's border-box coordinate
 *       space, plus the implicit-track counts, the single-line position of an empty axis and the
 *       explicit line-name table ({@link Tracks#lineNames()}).</li>
 *   <li>{@link #items()}: the grid area (1-based full-grid line numbers) of every in-flow,
 *       non-{@code display:none} child, in document order. Out-of-flow children are not part of
 *       grid placement and are therefore absent, matching upstream's
 *       {@code DetailedGridInfo.items}.</li>
 * </ul>
 *
 * <p>All values are unrounded. The grid algorithm records this information before the final
 * layout of the items, so the values describe the same geometry the container's unrounded
 * layout is built from; {@code RoundLayout} (applied afterwards when rounding is enabled) does
 * not mutate them.</p>
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
 * @param rows    resolved row-axis tracks and line names
 * @param columns resolved column-axis tracks and line names
 * @param items   grid areas of the in-flow items, in document order
 */
public record DetailedGridInfo(Tracks rows, Tracks columns, List<GridItemArea> items) {

    public DetailedGridInfo {
        Objects.requireNonNull(rows, "rows");
        Objects.requireNonNull(columns, "columns");
        items = List.copyOf(items);
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
     * <p>{@code lineNames} maps each <em>explicit</em> line name to its 1-based CSS grid line
     * numbers (ascending, duplicates removed). Line numbers use the style convention: line 1 is
     * the first explicit line, and a value before the explicit grid is negative. The table is a
     * basic explicit-name index for this first slice; the CSR per-line structure and the
     * resolved {@code grid-template-rows}/{@code grid-template-columns} serialization are later
     * GR-18 slices. Names generated implicitly by {@code grid-template-areas}
     * ({@code <area>-start}/{@code <area>-end}) are deliberately absent, matching the browser
     * serialization rule of #1185; those edges still resolve for placement through the grid
     * algorithm's separate area table. Explicitly declared names, including repeat()
     * positional name sets and explicitly declared area-edge-looking names, are included.</p>
     *
     * <p>Immutable: the track list, the map and every name list are unmodifiable copies.</p>
     *
     * @param negativeImplicitTracks number of tracks in the implicit grid before the explicit grid
     * @param explicitTracks         number of explicit tracks
     * @param positiveImplicitTracks number of tracks in the implicit grid after the explicit grid
     * @param tracks                 start/size of every track in logical order
     * @param emptyAxisLine          single-line position of an axis without non-collapsed tracks,
     *                               or {@code null} when the axis has a usable track
     * @param lineNames              explicit line names to their 1-based CSS line numbers
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
            TreeMap<String, List<Integer>> copied = new TreeMap<>();
            for (Map.Entry<String, List<Integer>> entry : lineNames.entrySet()) {
                copied.put(Objects.requireNonNull(entry.getKey(), "line name"),
                    List.copyOf(Objects.requireNonNull(entry.getValue(), "lines for " + entry.getKey())));
            }
            lineNames = Collections.unmodifiableMap(copied);
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
            List<Integer> lines = lineNames.get(name);
            return lines != null ? lines : List.of();
        }
    }
}
