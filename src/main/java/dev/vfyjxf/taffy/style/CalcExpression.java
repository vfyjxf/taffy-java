package dev.vfyjxf.taffy.style;

/**
 * Represents a CSS calc() expression.
 * 
 * <p>In CSS, calc() allows mathematical expressions with units. For example:
 * <ul>
 *   <li>{@code calc(100% - 20px)}</li>
 *   <li>{@code calc(50% + 10px)}</li>
 *   <li>{@code calc(100% / 3)}</li>
 * </ul>
 * 
 * <p>This interface allows users to implement their own calc expression evaluation.
 * The taffy layout engine will call {@link #resolve(float)} during layout computation
 * to get the actual pixel value.
 */
@FunctionalInterface
public interface CalcExpression {
    
    /**
     * Resolves this calc expression to a concrete pixel value.
     * 
     * @param basis The basis value in pixels (e.g., parent container size for percentage calculations)
     * @return The resolved value in pixels
     */
    float resolve(float basis);
    
    // === Common calc expression factories ===
    
    /**
     * Creates a calc expression that adds a percentage of the basis to a fixed length.
     * Equivalent to: calc(percent% + length)
     * 
     * @param percent The percentage value (0.0 to 1.0)
     * @param length The fixed length in pixels
     * @return A calc expression
     */
    static CalcExpression percentPlusLength(float percent, float length) {
        return basis -> basis * percent + length;
    }
    
    /**
     * Creates a calc expression that subtracts a fixed length from a percentage of the basis.
     * Equivalent to: calc(percent% - length)
     * 
     * @param percent The percentage value (0.0 to 1.0)
     * @param length The fixed length in pixels to subtract
     * @return A calc expression
     */
    static CalcExpression percentMinusLength(float percent, float length) {
        return basis -> basis * percent - length;
    }
    
    /**
     * Creates a calc expression that adds two percentages.
     * Equivalent to: calc(percent1% + percent2%)
     * 
     * @param percent1 The first percentage value (0.0 to 1.0)
     * @param percent2 The second percentage value (0.0 to 1.0)
     * @return A calc expression
     */
    static CalcExpression addPercents(float percent1, float percent2) {
        return basis -> basis * (percent1 + percent2);
    }
    
    /**
     * Creates a calc expression that divides a percentage by a number.
     * Equivalent to: calc(percent% / divisor)
     * 
     * @param percent The percentage value (0.0 to 1.0)
     * @param divisor The divisor
     * @return A calc expression
     */
    static CalcExpression percentDividedBy(float percent, float divisor) {
        return basis -> (basis * percent) / divisor;
    }
    
    /**
     * Creates a calc expression that multiplies a percentage by a number.
     * Equivalent to: calc(percent% * multiplier)
     * 
     * @param percent The percentage value (0.0 to 1.0)
     * @param multiplier The multiplier
     * @return A calc expression
     */
    static CalcExpression percentMultipliedBy(float percent, float multiplier) {
        return basis -> basis * percent * multiplier;
    }
    
    /**
     * Creates a calc expression for: calc(basis - length)
     * Equivalent to: calc(100% - length)
     * 
     * @param length The fixed length to subtract
     * @return A calc expression
     */
    static CalcExpression fullMinusLength(float length) {
        return basis -> basis - length;
    }
    
    /**
     * Creates a calc expression for: calc(basis + length)
     * Equivalent to: calc(100% + length)
     * 
     * @param length The fixed length to add
     * @return A calc expression
     */
    static CalcExpression fullPlusLength(float length) {
        return basis -> basis + length;
    }
}
