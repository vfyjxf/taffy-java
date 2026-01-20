package dev.vfyjxf.taffy.style;

import java.util.Objects;

/**
 * Defines a named grid area with start/end lines in both axes.
 * <p>
 * Corresponds to Rust's GridTemplateArea struct:
 * - name: The area name
 * - row_start, row_end: Row line indices (1-based)
 * - column_start, column_end: Column line indices (1-based)
 */
public final class GridTemplateArea {
    private final String name;
    private final int rowStart;
    private final int rowEnd;
    private final int columnStart;
    private final int columnEnd;

    public GridTemplateArea(String name, int rowStart, int rowEnd, int columnStart, int columnEnd) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.rowStart = rowStart;
        this.rowEnd = rowEnd;
        this.columnStart = columnStart;
        this.columnEnd = columnEnd;
    }

    public String getName() {
        return name;
    }

    public int getRowStart() {
        return rowStart;
    }

    public int getRowEnd() {
        return rowEnd;
    }

    public int getColumnStart() {
        return columnStart;
    }

    public int getColumnEnd() {
        return columnEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GridTemplateArea that = (GridTemplateArea) o;
        return rowStart == that.rowStart && rowEnd == that.rowEnd &&
               columnStart == that.columnStart && columnEnd == that.columnEnd &&
               name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, rowStart, rowEnd, columnStart, columnEnd);
    }

    @Override
    public String toString() {
        return "GridTemplateArea{name='" + name + "', rows=" + rowStart + "-" + rowEnd +
               ", cols=" + columnStart + "-" + columnEnd + "}";
    }
}
