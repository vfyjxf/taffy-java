package dev.vfyjxf.taffy.geometry;

public class FloatLine {
    public float start;
    public float end;

    public FloatLine(float start, float end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Creates a Line with both values set to the same value
     */
    public static FloatLine all(float value) {
        return new FloatLine(value, value);
    }

    /**
     * Sum of start and end (for numeric types)
     */
    public float sum() {
        return start + end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FloatLine line = (FloatLine) o;
        return Float.compare(line.start, start) == 0 && Float.compare(line.end, end) == 0;
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits(start) ^ Float.floatToIntBits(end);
    }

    @Override
    public String toString() {
        return "Line{start=" + start + ", end=" + end + '}';
    }

    /**
     * Copy this Line
     */
    public FloatLine copy() {
        return new FloatLine(start, end);
    }
}
