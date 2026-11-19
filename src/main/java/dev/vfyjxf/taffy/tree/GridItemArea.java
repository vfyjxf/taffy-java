package dev.vfyjxf.taffy.tree;

/**
 * The grid area occupied by one in-flow grid item, expressed as 1-based grid line numbers.
 *
 * <p>The four values are the CSS Grid line numbers bounding the item's area. They index the
 * <em>full</em> grid, including the implicit tracks before and after the explicit grid: line 1
 * bounds the start of the first (possibly implicit) track, exactly like the resolved grid line
 * numbers a browser reports for {@code grid-row-start}/{@code grid-column-start}. This differs
 * from the line numbers written in style, where line 1 is the first <em>explicit</em> line: an
 * item placed in the first negative implicit track has {@code rowStart == 1} here even though
 * its style line is negative.</p>
 *
 * <p>{@code rowEnd}/{@code columnEnd} are exclusive in the CSS sense (they are the line after
 * the item's last track), so a non-spanning item has {@code rowSpan() == 1} and a two-column
 * spanning item has {@code columnEnd() - columnStart() == 2}.</p>
 *
 * <p>This is the Java counterpart of upstream taffy's {@code DetailedGridItemsInfo}
 * (#1134), which stores the same four 1-based line numbers. The physical rectangle of the
 * area is resolved on demand by
 * {@link DetailedGridInfo#itemRectangle(int)} (upstream {@code item_grid_area}).</p>
 *
 * @param rowStart    1-based line where the item's rows start
 * @param rowEnd      1-based line where the item's rows end (exclusive)
 * @param columnStart 1-based line where the item's columns start
 * @param columnEnd   1-based line where the item's columns end (exclusive)
 */
public record GridItemArea(int rowStart, int rowEnd, int columnStart, int columnEnd) {

    public GridItemArea {
        if (rowStart < 1 || columnStart < 1) {
            throw new IllegalArgumentException(
                "grid area line numbers are 1-based, got rowStart=" + rowStart + ", columnStart=" + columnStart);
        }
        if (rowEnd <= rowStart || columnEnd <= columnStart) {
            throw new IllegalArgumentException(
                "grid area end lines must follow their start lines, got row " + rowStart + "/" + rowEnd
                    + ", column " + columnStart + "/" + columnEnd);
        }
    }

    /** The number of rows the item spans: {@code rowEnd - rowStart}. */
    public int rowSpan() {
        return rowEnd - rowStart;
    }

    /** The number of columns the item spans: {@code columnEnd - columnStart}. */
    public int columnSpan() {
        return columnEnd - columnStart;
    }
}
