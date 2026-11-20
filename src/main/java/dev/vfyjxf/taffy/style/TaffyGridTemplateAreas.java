package dev.vfyjxf.taffy.style;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The value of the CSS {@code grid-template-areas} property: the named grid areas together with the
 * overall size of the area template in tracks.
 * <p>
 * A {@code grid-template-areas} value can be larger than the extents of its named areas because of
 * unnamed ({@code .}) cells, and those cells still contribute to the size of the explicit grid: for
 * example {@code "a ."} is a one row by two column template even though only one cell is named, and
 * {@code ". . ."} alone is a one row by three column template with no named area at all. The
 * template size therefore cannot be derived from the named areas alone; this type carries it
 * explicitly.
 * <p>
 * <p><b>Name.</b> The {@code Taffy} prefix follows the same collision-driven convention as
 * {@link TaffyFloat}, {@link TaffyDimension} and {@link TaffyPosition}: a top-level
 * {@code GridTemplateAreas} would be ambiguous with consumer types under
 * {@code import dev.vfyjxf.taffy.style.*} (the LDLib2 regression found exactly that), just as
 * {@code Float} would shadow {@link java.lang.Float}.
 *
 * <p>This is the additive counterpart of upstream taffy's {@code grid-template-areas} struct
 * (upstream #1024 / GR-16). {@link TaffyStyle#gridTemplateAreas} keeps its historical
 * {@code List<GridTemplateArea>} type for source and binary compatibility, so the template size is
 * carried by two extra fields on the style ({@link TaffyStyle#gridTemplateAreaRowCount} and
 * {@link TaffyStyle#gridTemplateAreaColumnCount}, whose default {@link #DERIVE} sentinel derives the
 * size from the named areas exactly as before). Use {@link #applyTo(TaffyStyle)} (or
 * {@link TaffyStyle#setGridTemplateAreaTemplate}) to set the areas and the size together, and
 * {@link #from(TaffyStyle)} to read the effective value back from a style.
 *
 * @see <a href="https://www.w3.org/TR/css-grid-1/#grid-template-areas-property">CSS Grid 1:
 *      grid-template-areas</a>
 */
public final class TaffyGridTemplateAreas {

    /**
     * Sentinel for {@link TaffyStyle#gridTemplateAreaRowCount} and
     * {@link TaffyStyle#gridTemplateAreaColumnCount} meaning "derive the template size from the
     * extents of the named areas" (the pre-GR-16 behavior, and the default).
     */
    public static final int DERIVE = -1;

    private final List<GridTemplateArea> areas;
    private final int rowCount;
    private final int columnCount;

    /**
     * Creates a template value.
     *
     * @param areas       the named grid areas; {@code null} is treated as an empty list. The list is
     *                    copied, so later changes to it do not affect this value.
     * @param rowCount    the number of rows of the area template (at least 0)
     * @param columnCount the number of columns of the area template (at least 0)
     * @throws IllegalArgumentException if a count is negative or an area is {@code null}
     */
    public TaffyGridTemplateAreas(List<GridTemplateArea> areas, int rowCount, int columnCount) {
        if (rowCount < 0) {
            throw new IllegalArgumentException("grid-template-areas row count must not be negative: " + rowCount);
        }
        if (columnCount < 0) {
            throw new IllegalArgumentException(
                "grid-template-areas column count must not be negative: " + columnCount);
        }
        List<GridTemplateArea> copy = new ArrayList<>();
        if (areas != null) {
            for (GridTemplateArea area : areas) {
                if (area == null) {
                    throw new IllegalArgumentException("grid-template-areas must not contain null areas");
                }
                copy.add(area);
            }
        }
        this.areas = List.copyOf(copy);
        this.rowCount = rowCount;
        this.columnCount = columnCount;
    }

    /** A template with no named area and no explicit track (the CSS initial value). */
    public static TaffyGridTemplateAreas empty() {
        return new TaffyGridTemplateAreas(List.of(), 0, 0);
    }

    /**
     * Creates a template from named areas whose size is derived from their extents (upstream's
     * default {@code grid_template_area_row_count}/{@code column_count} implementation).
     *
     * @param areas the named grid areas; {@code null} is treated as an empty list
     */
    public static TaffyGridTemplateAreas fromAreas(List<GridTemplateArea> areas) {
        return new TaffyGridTemplateAreas(areas, derivedRowCount(areas), derivedColumnCount(areas));
    }

    /**
     * The effective template value of a style: its named areas plus the template size, where the
     * size is the explicitly configured one or, when unset, the extents of the named areas.
     *
     * @param style the style to read; must not be {@code null}
     */
    public static TaffyGridTemplateAreas from(TaffyStyle style) {
        Objects.requireNonNull(style, "style cannot be null");
        return new TaffyGridTemplateAreas(style.gridTemplateAreas,
            style.getGridTemplateAreaRowCount(), style.getGridTemplateAreaColumnCount());
    }

    /**
     * Writes this template to a style: sets the style's named areas and its explicit template size.
     *
     * @param style the style to write; must not be {@code null}
     * @return the passed style, for chaining
     */
    public TaffyStyle applyTo(TaffyStyle style) {
        Objects.requireNonNull(style, "style cannot be null");
        style.gridTemplateAreas = new ArrayList<>(areas);
        style.gridTemplateAreaRowCount = rowCount;
        style.gridTemplateAreaColumnCount = columnCount;
        return style;
    }

    /**
     * The number of rows covered by the extents of the named areas, i.e. the value upstream's
     * default {@code grid_template_area_row_count} derives: the largest {@code row_end - 1} of any
     * area (a non-positive {@code row_end} contributes zero).
     *
     * @param areas the named grid areas; {@code null} is treated as an empty list
     */
    public static int derivedRowCount(List<GridTemplateArea> areas) {
        int count = 0;
        if (areas != null) {
            for (GridTemplateArea area : areas) {
                if (area != null) {
                    count = Math.max(count, Math.max(1, area.getRowEnd()) - 1);
                }
            }
        }
        return count;
    }

    /**
     * The number of columns covered by the extents of the named areas, i.e. the value upstream's
     * default {@code grid_template_area_column_count} derives: the largest {@code column_end - 1} of
     * any area (a non-positive {@code column_end} contributes zero).
     *
     * @param areas the named grid areas; {@code null} is treated as an empty list
     */
    public static int derivedColumnCount(List<GridTemplateArea> areas) {
        int count = 0;
        if (areas != null) {
            for (GridTemplateArea area : areas) {
                if (area != null) {
                    count = Math.max(count, Math.max(1, area.getColumnEnd()) - 1);
                }
            }
        }
        return count;
    }

    /**
     * The named grid areas. The returned list is immutable; the individual areas are immutable too.
     */
    public List<GridTemplateArea> getAreas() {
        return areas;
    }

    /**
     * The number of rows of the area template. May be greater than the extent of the named areas
     * because of unnamed ({@code .}) cells.
     */
    public int getRowCount() {
        return rowCount;
    }

    /**
     * The number of columns of the area template. May be greater than the extent of the named areas
     * because of unnamed ({@code .}) cells.
     */
    public int getColumnCount() {
        return columnCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaffyGridTemplateAreas that = (TaffyGridTemplateAreas) o;
        return rowCount == that.rowCount && columnCount == that.columnCount && areas.equals(that.areas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(areas, rowCount, columnCount);
    }

    @Override
    public String toString() {
        return "TaffyGridTemplateAreas{areas=" + areas + ", rows=" + rowCount + ", columns=" + columnCount + "}";
    }
}
