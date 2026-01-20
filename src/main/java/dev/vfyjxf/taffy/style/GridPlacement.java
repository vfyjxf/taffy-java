package dev.vfyjxf.taffy.style;

import java.util.Objects;

/**
 * Represents a grid placement for a grid item.
 * Can be auto, a specific line number, a named line, a span, or a named span.
 * <p>
 * Matches Rust's GridPlacement enum with variants:
 * - Auto
 * - Line(GridLine)
 * - NamedLine(S, i16)
 * - Span(u16)
 * - NamedSpan(S, u16)
 */
public final class GridPlacement {
    
    /** The type of grid placement */
    public enum Type {
        /** Grid item is automatically placed */
        AUTO,
        /** Grid item is placed at a specific line number */
        LINE,
        /**
         * Grid item is placed at a named line.
         * The nthIndex specifies which occurrence of the named line to use (1-based).
         * Positive values count from the start, negative from the end.
         */
        NAMED_LINE,
        /** Grid item spans a number of tracks */
        SPAN,
        /**
         * Grid item spans until the nth line named by lineName.
         * If there are less than n lines with that name, all implicit lines will be counted.
         */
        NAMED_SPAN
    }

    private final Type type;
    private final int value;
    private final String lineName;
    private final int nthIndex;  // For NAMED_LINE: which occurrence (positive=from start, negative=from end)

    private GridPlacement(Type type, int value, String lineName, int nthIndex) {
        this.type = type;
        this.value = value;
        this.lineName = lineName;
        this.nthIndex = nthIndex;
    }

    /**
     * Creates an auto placement
     */
    public static GridPlacement auto() {
        return AUTO_INSTANCE;
    }

    /**
     * Creates a line placement at the specified line number.
     * @param line 1-based line number (can be negative to count from end).
     *             Line 0 is invalid and treated as auto per CSS spec.
     */
    public static GridPlacement line(int line) {
        return new GridPlacement(Type.LINE, line, null, 0);
    }

    /**
     * Creates a named line placement at the first occurrence of the named line.
     * Equivalent to namedLine(name, 1).
     * @param name The name of the grid line
     */
    public static GridPlacement namedLine(String name) {
        return namedLine(name, 1);
    }

    /**
     * Creates a named line placement at the nth occurrence of the named line.
     * @param name The name of the grid line
     * @param nthIndex Which occurrence to use (1-based). Positive counts from start,
     *                 negative counts from end. 0 is treated as 1.
     */
    public static GridPlacement namedLine(String name, int nthIndex) {
        if (nthIndex == 0) nthIndex = 1;
        return new GridPlacement(Type.NAMED_LINE, 0, name, nthIndex);
    }

    /**
     * Creates a span placement spanning the specified number of tracks.
     * @param span Number of tracks to span
     */
    public static GridPlacement span(int span) {
        return new GridPlacement(Type.SPAN, span, null, 0);
    }

    /**
     * Creates a named span placement spanning until the nth line with the given name.
     * @param name The name of the line to span to
     * @param count The number of lines with this name to span (1-based)
     */
    public static GridPlacement namedSpan(String name, int count) {
        if (count < 1) count = 1;
        return new GridPlacement(Type.NAMED_SPAN, count, name, 0);
    }

    /** Auto placement singleton */
    public static final GridPlacement AUTO_INSTANCE = new GridPlacement(Type.AUTO, 0, null, 0);

    /**
     * Returns the type of this placement
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the line number (for LINE type) or span value (for SPAN/NAMED_SPAN types).
     */
    public int getValue() {
        return value;
    }

    /**
     * Returns the named line (for NAMED_LINE or NAMED_SPAN types, may be null otherwise)
     */
    public String getLineName() {
        return lineName;
    }

    /**
     * Returns the nth index for NAMED_LINE type.
     * Positive values count from start, negative from end.
     */
    public int getNthIndex() {
        return nthIndex;
    }

    /**
     * Returns true if this is an auto placement
     */
    public boolean isAuto() {
        return type == Type.AUTO;
    }

    /**
     * Returns true if this is a line placement (numeric line index)
     */
    public boolean isLine() {
        return type == Type.LINE;
    }

    /**
     * Returns true if this is a named line placement
     */
    public boolean isNamedLine() {
        return type == Type.NAMED_LINE;
    }

    /**
     * Returns true if this is a span placement (numeric span)
     */
    public boolean isSpan() {
        return type == Type.SPAN;
    }

    /**
     * Returns true if this is a named span placement
     */
    public boolean isNamedSpan() {
        return type == Type.NAMED_SPAN;
    }

    /**
     * Returns true if this placement is definite (not auto).
     * Note: Named placements are considered definite.
     * Note: LINE(0) is technically invalid and treated as auto per CSS spec,
     *       but isDefinite() still returns true for it.
     */
    public boolean isDefinite() {
        return type != Type.AUTO;
    }

    /**
     * Returns the line number (for LINE type).
     */
    public int getLineNumber() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GridPlacement that = (GridPlacement) o;
        return type == that.type && value == that.value && nthIndex == that.nthIndex &&
               Objects.equals(lineName, that.lineName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value, lineName, nthIndex);
    }

    @Override
    public String toString() {
        return switch (type) {
            case AUTO -> "auto";
            case LINE -> String.valueOf(value);
            case NAMED_LINE -> nthIndex == 1 ? lineName : lineName + " " + nthIndex;
            case SPAN -> "span " + value;
            case NAMED_SPAN -> "span " + lineName + " " + value;
        };
    }
}
