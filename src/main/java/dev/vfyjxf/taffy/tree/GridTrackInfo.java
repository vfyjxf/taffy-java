package dev.vfyjxf.taffy.tree;

/**
 * One resolved track of a {@link DetailedGridInfo} axis.
 *
 * <p>Coordinates are CSS pixels in the grid container's <em>border box</em> coordinate space
 * (the border-box origin is {@code (0, 0)}). {@code start} is the physical left edge (horizontal
 * axis) or top edge (vertical axis) and {@code size} is the used track size, so
 * {@link #end()} is always {@code start + size}. The position includes the container's border
 * and padding, the {@code gap} property and content alignment
 * ({@code align-content}/{@code justify-content}). The values are the unrounded values produced
 * by the layout pass: the same numbers {@code RoundLayout} rounds when rounding is enabled.</p>
 *
 * <p>Tracks are listed in logical order (the order of the template and the implicit tracks
 * around it) regardless of {@code direction}. In an RTL container the first logical column
 * track is physically the rightmost one, so its {@code start} is larger than the second track's
 * {@code start}; every track still satisfies {@code start <= end()}.</p>
 *
 * <p>This is the Java counterpart of one entry of upstream taffy's
 * {@code DetailedGridTracksInfo.positions} ({@code Line<f32>}, #1131). A collapsed
 * {@code auto-fit} track is present with {@code size == 0}.</p>
 *
 * @param start physical start (left/top) edge, relative to the container's border box
 * @param size  used size of the track along the axis
 */
public record GridTrackInfo(float start, float size) {

    /**
     * The physical end (right/bottom) edge of the track: {@code start + size}.
     */
    public float end() {
        return start + size;
    }
}
