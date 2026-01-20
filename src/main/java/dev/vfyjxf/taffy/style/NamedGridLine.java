package dev.vfyjxf.taffy.style;

import java.util.Objects;

/**
 * Defines a named grid line at a specific index.
 * <p>
 * Corresponds to Rust's NamedGridLine struct.
 * Used in grid_template_column_names and grid_template_row_names.
 */
public final class NamedGridLine {
    private final String name;
    private final int index; // 1-based line index

    public NamedGridLine(String name, int index) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamedGridLine that = (NamedGridLine) o;
        return index == that.index && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, index);
    }

    @Override
    public String toString() {
        return "[" + name + "] at line " + index;
    }
}
