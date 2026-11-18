package dev.vfyjxf.taffy.style;

import java.util.Objects;

/**
 * A unit of linear measurement representing a CSS dimension value.
 * Can be a fixed length, a percentage, auto, intrinsic sizing keywords, or a calc expression.
 * 
 * <p>Supports CSS intrinsic sizing keywords for Yoga-like compatibility:
 * <ul>
 *   <li>{@link Type#MIN_CONTENT} - The intrinsic minimum width/height of the content</li>
 *   <li>{@link Type#MAX_CONTENT} - The intrinsic preferred width/height of the content</li>
 *   <li>{@link Type#FIT_CONTENT} - Equivalent to min(max-content, max(min-content, stretch))</li>
 *   <li>{@link Type#STRETCH} - Fill the available space in the containing block</li>
 * </ul>
 */
public final class TaffyDimension {
    
    /** The type of dimension value */
    public enum Type {
        /** An absolute length in some abstract units (pixels, logical pixels, etc.) */
        LENGTH,
        /** A percentage length relative to the size of the containing block */
        PERCENT,
        /** The dimension should be automatically computed */
        AUTO,
        /** A calc() expression that will be evaluated at layout time */
        CALC,
        /** 
         * CSS min-content: the intrinsic minimum width/height.
         * The smallest size the box can take without causing overflow.
         */
        MIN_CONTENT,
        /**
         * CSS max-content: the intrinsic preferred width/height.
         * The ideal size for the content without any line breaks or overflow.
         */
        MAX_CONTENT,
        /**
         * CSS fit-content: clamps the content between min-content and max-content.
         * Equivalent to min(max-content, max(min-content, stretch-fit)).
         */
        FIT_CONTENT,
        /**
         * CSS stretch / -webkit-fill-available: fill the available space.
         * The box expands to fill the available space in the containing block.
         */
        STRETCH,
        /**
         * CSS flex-basis: content. Uses the item's content-based automatic size.
         */
        CONTENT
    }

    private final Type type;
    private final float value;
    private final CalcExpression calcExpression;

    private TaffyDimension(Type type, float value) {
        this.type = type;
        this.value = value;
        this.calcExpression = null;
    }
    
    private TaffyDimension(CalcExpression calcExpression) {
        this.type = Type.CALC;
        this.value = 0;
        this.calcExpression = calcExpression;
    }

    /**
     * Creates an absolute length value
     */
    public static TaffyDimension length(float value) {
        return new TaffyDimension(Type.LENGTH, value);
    }

    /**
     * Creates a percentage length value.
     * Note: percentages are represented as a float in the range [0.0, 1.0] NOT [0.0, 100.0]
     */
    public static TaffyDimension percent(float value) {
        return new TaffyDimension(Type.PERCENT, value);
    }

    /**
     * Creates an auto value
     */
    public static TaffyDimension auto() {
        return AUTO;
    }
    
    /**
     * Creates a calc() expression value.
     * The expression will be evaluated during layout computation.
     */
    public static TaffyDimension calc(CalcExpression expression) {
        return new TaffyDimension(expression);
    }
    
    /**
     * Creates a min-content value.
     * The intrinsic minimum width/height of the content.
     */
    public static TaffyDimension minContent() {
        return MIN_CONTENT;
    }
    
    /**
     * Creates a max-content value.
     * The intrinsic preferred width/height of the content.
     */
    public static TaffyDimension maxContent() {
        return MAX_CONTENT;
    }
    
    /**
     * Creates a fit-content value.
     * Equivalent to min(max-content, max(min-content, stretch)).
     */
    public static TaffyDimension fitContent() {
        return FIT_CONTENT;
    }
    
    /**
     * Creates a stretch value.
     * Fill the available space in the containing block.
     */
    public static TaffyDimension stretch() {
        return STRETCH;
    }

    /**
     * Creates a content value (for flex-basis: content).
     */
    public static TaffyDimension content() {
        return CONTENT;
    }

    /** Zero length */
    public static final TaffyDimension ZERO = length(0);
    
    /** Auto value singleton */
    public static final TaffyDimension AUTO = new TaffyDimension(Type.AUTO, 0);
    
    /** Min-content value singleton */
    public static final TaffyDimension MIN_CONTENT = new TaffyDimension(Type.MIN_CONTENT, 0);
    
    /** Max-content value singleton */
    public static final TaffyDimension MAX_CONTENT = new TaffyDimension(Type.MAX_CONTENT, 0);
    
    /** Fit-content value singleton */
    public static final TaffyDimension FIT_CONTENT = new TaffyDimension(Type.FIT_CONTENT, 0);
    
    /** Stretch value singleton */
    public static final TaffyDimension STRETCH = new TaffyDimension(Type.STRETCH, 0);
    
    /** Content value singleton (for flex-basis: content) */
    public static final TaffyDimension CONTENT = new TaffyDimension(Type.CONTENT, 0);

    /**
     * Convert from LengthPercentage
     */
    public static TaffyDimension from(LengthPercentage lp) {
        return switch (lp.getType()) {
            case LENGTH -> length(lp.getValue());
            case PERCENT -> percent(lp.getValue());
            case CALC -> calc(lp.getCalcExpression());
        };
    }

    /**
     * Convert from LengthPercentageAuto
     */
    public static TaffyDimension from(LengthPercentageAuto lpa) {
        return switch (lpa.getType()) {
            case LENGTH -> length(lpa.getValue());
            case PERCENT -> percent(lpa.getValue());
            case AUTO -> AUTO;
            case CALC -> calc(lpa.getCalcExpression());
            case MIN_CONTENT -> MIN_CONTENT;
            case MAX_CONTENT -> MAX_CONTENT;
            case FIT_CONTENT -> FIT_CONTENT;
            case STRETCH -> STRETCH;
        };
    }

    /**
     * Returns the type of this dimension
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the raw value
     */
    public float getValue() {
        return value;
    }
    
    /**
     * Returns the calc expression, or null if not a calc type
     */
    public CalcExpression getCalcExpression() {
        return calcExpression;
    }

    /**
     * Returns true if this is an absolute length
     */
    public boolean isLength() {
        return type == Type.LENGTH;
    }

    /**
     * Returns true if this is a percentage
     */
    public boolean isPercent() {
        return type == Type.PERCENT;
    }

    /**
     * Returns true if this is auto
     */
    public boolean isAuto() {
        return type == Type.AUTO;
    }
    
    /**
     * Returns true if this is a calc expression
     */
    public boolean isCalc() {
        return type == Type.CALC;
    }
    
    /**
     * Returns true if this is min-content
     */
    public boolean isMinContent() {
        return type == Type.MIN_CONTENT;
    }
    
    /**
     * Returns true if this is max-content
     */
    public boolean isMaxContent() {
        return type == Type.MAX_CONTENT;
    }
    
    /**
     * Returns true if this is fit-content
     */
    public boolean isFitContent() {
        return type == Type.FIT_CONTENT;
    }
    
    /**
     * Returns true if this is stretch
     */
    public boolean isStretch() {
        return type == Type.STRETCH;
    }
    
    /**
     * Returns true if this is content (flex-basis: content)
     */
    public boolean isContent() {
        return type == Type.CONTENT;
    }
    
    /**
     * Returns true if this is an intrinsic sizing keyword (min-content, max-content, fit-content, stretch)
     */
    public boolean isIntrinsic() {
        return type == Type.MIN_CONTENT || type == Type.MAX_CONTENT || 
               type == Type.FIT_CONTENT || type == Type.STRETCH || type == Type.CONTENT;
    }

    /**
     * Returns true if this dimension is one of the four intrinsic sizing keywords whose value is
     * obtained from content measurement: {@link Type#MIN_CONTENT}, {@link Type#MAX_CONTENT},
     * {@link Type#FIT_CONTENT} and {@link Type#STRETCH}.
     *
     * <p>This is the set modelled by taffy's {@code SizingKeyword} and the set handled by
     * {@link #resolveIntrinsicKeyword(float, float, float)}. {@link Type#CONTENT} is deliberately
     * excluded because it is not a sizing keyword: it only exists as {@code flex-basis: content}
     * and is resolved by the flexbox algorithm from the item's content. Note that the pre-existing
     * {@link #isIntrinsic()} remains broader and still returns true for {@link Type#CONTENT}; use
     * this method when selecting the sizing-keyword code path.
     *
     * @return true for {@code min-content}, {@code max-content}, {@code fit-content} and
     *         {@code stretch}; false for every other type, including {@code content}
     */
    public boolean isIntrinsicKeyword() {
        return type == Type.MIN_CONTENT || type == Type.MAX_CONTENT ||
               type == Type.FIT_CONTENT || type == Type.STRETCH;
    }

    /**
     * Resolves a content-based intrinsic sizing keyword against already measured intrinsic sizes.
     *
     * <ul>
     *   <li>{@link Type#MIN_CONTENT} returns {@code minContent}</li>
     *   <li>{@link Type#MAX_CONTENT} returns {@code maxContent}</li>
     *   <li>{@link Type#FIT_CONTENT} returns {@code fitContent}</li>
     * </ul>
     *
     * <p>The fit-content size is supplied by the caller because CSS defines it as
     * {@code min(max-content, max(min-content, stretch-fit))}; the stretch-fit part depends on
     * the available space and cannot be derived from {@code minContent} and {@code maxContent}
     * alone.
     *
     * <p>{@link Type#STRETCH} and {@link Type#CONTENT} are content-dependent but cannot be
     * resolved by this helper: stretch resolves against the available space, and content is a
     * {@code flex-basis} keyword handled by the flexbox algorithm. Both throw
     * {@link UnsupportedOperationException} rather than returning {@link Float#NaN}, so callers
     * cannot silently treat them as {@code auto}. The opt-in sizing-keyword resolution that will
     * give them a real resolver is deferred to 1.3 (decision S5).
     *
     * <p>This method is additive and does not change the legacy behavior: {@link #maybeResolve(float)}
     * still returns {@code NaN} for all intrinsic keywords until the opt-in mode lands.
     *
     * @param minContent the measured min-content size, in layout units
     * @param maxContent the measured max-content size, in layout units
     * @param fitContent the measured fit-content size (already clamped by the caller), in layout units
     * @return the size selected by this dimension's keyword
     * @throws UnsupportedOperationException if this dimension is {@link Type#STRETCH} or
     *         {@link Type#CONTENT}, whose resolution needs information not available here
     * @throws IllegalStateException if this dimension is not an intrinsic sizing keyword, i.e.
     *         {@link #isIntrinsicKeyword()} is false
     */
    public float resolveIntrinsicKeyword(float minContent, float maxContent, float fitContent) {
        return switch (type) {
            case MIN_CONTENT -> minContent;
            case MAX_CONTENT -> maxContent;
            case FIT_CONTENT -> fitContent;
            case STRETCH, CONTENT -> throw new UnsupportedOperationException(
                    "Cannot resolve '" + this + "' from measured intrinsic sizes: stretch resolves "
                    + "against the available space and 'content' is a flex-basis keyword; "
                    + "opt-in sizing-keyword resolution is deferred until 1.3 (decision S5)");
            case LENGTH, PERCENT, AUTO, CALC -> throw new IllegalStateException(
                    "Not an intrinsic sizing keyword: " + type + " (isIntrinsicKeyword() == false)");
        };
    }

    /**
     * Returns the length value as an option (NaN if not a length)
     */
    public float intoOption() {
        return type == Type.LENGTH ? value : Float.NaN;
    }

    /**
     * Resolve this dimension against a context size.
     * Returns NaN for auto and intrinsic sizing keywords.
     * Note: Intrinsic sizing keywords (min-content, max-content, fit-content, stretch)
     * cannot be resolved to a simple float value - they require the layout algorithm
     * to compute their actual value based on content.
     */
    public float maybeResolve(float context) {
        return switch (type) {
            case LENGTH -> value;
            case PERCENT -> Float.isNaN(context) ? Float.NaN : context * value;
            case AUTO, MIN_CONTENT, MAX_CONTENT, FIT_CONTENT, STRETCH, CONTENT -> Float.NaN;
            case CALC -> Float.isNaN(context) ? Float.NaN : (calcExpression != null ? calcExpression.resolve(context) : 0f);
        };
    }

    /**
     * Resolve this dimension or return zero if unresolvable
     */
    public float resolveOrZero(float context) {
        float resolved = maybeResolve(context);
        return Float.isNaN(resolved) ? 0f : resolved;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaffyDimension that = (TaffyDimension) o;
        if (type != that.type) return false;
        // For singleton types, type equality is sufficient
        if (type == Type.AUTO || type == Type.MIN_CONTENT || type == Type.MAX_CONTENT || 
            type == Type.FIT_CONTENT || type == Type.STRETCH || type == Type.CONTENT) return true;
        if (type == Type.CALC) return Objects.equals(calcExpression, that.calcExpression);
        return Float.compare(value, that.value) == 0;
    }

    @Override
    public int hashCode() {
        if (type == Type.CALC) {
            return Objects.hash(type, calcExpression);
        }
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        return switch (type) {
            case LENGTH -> value + "px";
            case PERCENT -> (value * 100) + "%";
            case AUTO -> "auto";
            case CALC -> "calc(...)";
            case MIN_CONTENT -> "min-content";
            case MAX_CONTENT -> "max-content";
            case FIT_CONTENT -> "fit-content";
            case STRETCH -> "stretch";
            case CONTENT -> "content";
        };
    }
}
