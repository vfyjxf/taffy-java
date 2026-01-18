package dev.vfyjxf.taffy.style;

import java.util.Objects;

/**
 * Represents a grid placement for a grid item.
 * Can be auto, a specific line number, or a span.
 */
public final class GridPlacement {
    
    /** The type of grid placement */
    public enum Type {
        /** Grid item is automatically placed */
        AUTO,
        /** Grid item is placed at a specific line */
        LINE,
        /** Grid item spans a number of tracks */
        SPAN
    }

    private final Type type;
    private final int value;
    private final String lineName;

    private GridPlacement(Type type, int value, String lineName) {
        this.type = type;
        this.value = value;
        this.lineName = lineName;
    }

    /**
     * Creates an auto placement
     */
    public static GridPlacement auto() {
        return AUTO_INSTANCE;
    }

    /**
     * Creates a line placement at the specified line number
     * @param line 1-based line number (can be negative to count from end)
     */
    public static GridPlacement line(int line) {
        return new GridPlacement(Type.LINE, line, null);
    }

    /**
     * Creates a named line placement
     */
    public static GridPlacement namedLine(String name) {
        return new GridPlacement(Type.LINE, 0, name);
    }

    /**
     * Creates a span placement
     * @param span Number of tracks to span
     */
    public static GridPlacement span(int span) {
        return new GridPlacement(Type.SPAN, span, null);
    }

    /** Auto placement singleton */
    public static final GridPlacement AUTO_INSTANCE = new GridPlacement(Type.AUTO, 0, null);

    /**
     * Returns the type of this placement
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the line number or span value
     */
    public int getValue() {
        return value;
    }

    /**
     * Returns the named line (may be null)
     */
    public String getLineName() {
        return lineName;
    }

    /**
     * Returns true if this is an auto placement
     */
    public boolean isAuto() {
        return type == Type.AUTO;
    }

    /**
     * Returns true if this is a line placement
     */
    public boolean isLine() {
        return type == Type.LINE;
    }

    /**
     * Returns true if this is a span placement
     */
    public boolean isSpan() {
        return type == Type.SPAN;
    }

    /**
     * Returns true if this placement is definite (not auto)
     */
    public boolean isDefinite() {
        return type != Type.AUTO;
    }

    /**
     * Returns the line number (for LINE type)
     */
    public int getLineNumber() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GridPlacement that = (GridPlacement) o;
        return type == that.type && value == that.value && 
               Objects.equals(lineName, that.lineName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value, lineName);
    }

    @Override
    public String toString() {
        return switch (type) {
            case AUTO -> "auto";
            case LINE -> lineName != null ? lineName : String.valueOf(value);
            case SPAN -> "span " + value;
        };
    }
}
