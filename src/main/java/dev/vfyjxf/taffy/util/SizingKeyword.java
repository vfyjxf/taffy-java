package dev.vfyjxf.taffy.util;

import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDimension;

import java.util.Objects;

/**
 * Parsed representation of the CSS sizing keywords that may appear on {@code width},
 * {@code height} and {@code flex-basis}: {@code min-content}, {@code max-content},
 * {@code fit-content}, {@code fit-content(<length-percentage>)} and {@code stretch}.
 *
 * <p>This is the Java counterpart of taffy's {@code compute::common::sizing_keyword} module
 * (#1099, #1103 and #1247). A {@link TaffyDimension} is {@link #parse(TaffyDimension) parsed}
 * into a keyword, and the keyword is then {@link #resolve(float, float) resolved} in a layout
 * context to either an {@link Resolution#exact(float) exact} used size or a
 * {@link Resolution#measure(AvailableSpace) measurement request}:</p>
 * <ul>
 *   <li>{@code min-content} / {@code max-content} always request a measurement under the
 *       corresponding {@link AvailableSpace#minContent() min-content} /
 *       {@link AvailableSpace#maxContent() max-content} constraint;</li>
 *   <li>{@code fit-content(<length>)}, {@code fit-content(<percentage>)} and
 *       {@code fit-content(calc())} request a measurement under a
 *       {@link AvailableSpace#definite(float) definite} constraint derived from their limit;</li>
 *   <li>a bare {@code fit-content} requests a measurement under the definite stretch size;</li>
 *   <li>{@code stretch} resolves to the stretch size exactly, without measuring.</li>
 * </ul>
 *
 * <p>A keyword that cannot be resolved in the current context (a percentage/calc limit or a bare
 * {@code fit-content} without a resolution basis, or {@code stretch} without a stretch size)
 * resolves to {@code null}. Callers must treat such a keyword as {@code auto}/{@code content},
 * which is what upstream does. Non-keyword dimensions and {@link TaffyDimension#CONTENT} also
 * parse to {@code null}: {@code content} is only valid for {@code flex-basis} and is not a
 * sizing keyword (it is resolved by the flexbox algorithm from the item's content).</p>
 *
 * <p>{@link TaffyDimension#maybeResolve(float)} deliberately keeps returning {@code NaN} for every
 * keyword; this parser is the additive route that distinguishes "needs to be measured" from
 * "no value".</p>
 */
public final class SizingKeyword {

    /** The parsed sizing keyword. */
    public enum Kind {
        /** CSS {@code min-content}: size the item under a min-content constraint. */
        MIN_CONTENT,
        /** CSS {@code max-content}: size the item under a max-content constraint. */
        MAX_CONTENT,
        /** CSS {@code fit-content} or {@code fit-content(<length-percentage>)}. */
        FIT_CONTENT,
        /** CSS {@code stretch}: fill the available space in the containing block. */
        STRETCH
    }

    /**
     * How a sizing keyword resolves to a used size.
     *
     * <p>Mirrors upstream's {@code SizingKeywordResolution::{Exact, Measure}}.</p>
     */
    public static final class Resolution {

        /** Whether the resolution is an exact size or a measurement request. */
        public enum Type {
            /** The size resolves to {@link #getExactValue()} without measuring the item. */
            EXACT,
            /** The size is obtained by measuring the item under {@link #getMeasureAvailableSpace()}. */
            MEASURE
        }

        private final Type type;
        private final float exactValue;
        private final AvailableSpace measureAvailableSpace;

        private Resolution(Type type, float exactValue, AvailableSpace measureAvailableSpace) {
            this.type = type;
            this.exactValue = exactValue;
            this.measureAvailableSpace = measureAvailableSpace;
        }

        /**
         * Creates an exact resolution: the used size is known without measuring the item.
         */
        public static Resolution exact(float value) {
            return new Resolution(Type.EXACT, value, null);
        }

        /**
         * Creates a measurement request: the item must be measured under the given constraint.
         */
        public static Resolution measure(AvailableSpace availableSpace) {
            return new Resolution(Type.MEASURE, Float.NaN, Objects.requireNonNull(availableSpace, "availableSpace"));
        }

        /** Returns the resolution type. */
        public Type getType() {
            return type;
        }

        /** Returns true when the size is known without measuring the item. */
        public boolean isExact() {
            return type == Type.EXACT;
        }

        /** Returns true when the item must be measured under an available-space constraint. */
        public boolean isMeasure() {
            return type == Type.MEASURE;
        }

        /**
         * Returns the exact used size.
         *
         * @throws IllegalStateException if this is a {@link Type#MEASURE} resolution
         */
        public float getExactValue() {
            if (type != Type.EXACT) {
                throw new IllegalStateException("Not an exact resolution: " + type);
            }
            return exactValue;
        }

        /**
         * Returns the constraint the item must be measured under.
         *
         * @throws IllegalStateException if this is an {@link Type#EXACT} resolution
         */
        public AvailableSpace getMeasureAvailableSpace() {
            if (type != Type.MEASURE) {
                throw new IllegalStateException("Not a measurement resolution: " + type);
            }
            return measureAvailableSpace;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Resolution that = (Resolution) o;
            return type == that.type
                    && Float.compare(exactValue, that.exactValue) == 0
                    && Objects.equals(measureAvailableSpace, that.measureAvailableSpace);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, exactValue, measureAvailableSpace);
        }

        @Override
        public String toString() {
            return type == Type.EXACT
                    ? "Exact(" + exactValue + ")"
                    : "Measure(" + measureAvailableSpace + ")";
        }
    }

    private static final SizingKeyword MIN_CONTENT_KEYWORD = new SizingKeyword(Kind.MIN_CONTENT, null);
    private static final SizingKeyword MAX_CONTENT_KEYWORD = new SizingKeyword(Kind.MAX_CONTENT, null);
    private static final SizingKeyword FIT_CONTENT_KEYWORD = new SizingKeyword(Kind.FIT_CONTENT, null);
    private static final SizingKeyword STRETCH_KEYWORD = new SizingKeyword(Kind.STRETCH, null);

    private final Kind kind;
    private final LengthPercentage fitContentLimit;

    private SizingKeyword(Kind kind, LengthPercentage fitContentLimit) {
        this.kind = kind;
        this.fitContentLimit = fitContentLimit;
    }

    /** Returns the {@code min-content} keyword. */
    public static SizingKeyword minContent() {
        return MIN_CONTENT_KEYWORD;
    }

    /** Returns the {@code max-content} keyword. */
    public static SizingKeyword maxContent() {
        return MAX_CONTENT_KEYWORD;
    }

    /** Returns the bare {@code fit-content} keyword. */
    public static SizingKeyword fitContent() {
        return FIT_CONTENT_KEYWORD;
    }

    /**
     * Returns the {@code fit-content(<length-percentage>)} keyword.
     *
     * @param limit the fit-content limit; must not be {@code null}
     */
    public static SizingKeyword fitContent(LengthPercentage limit) {
        return new SizingKeyword(Kind.FIT_CONTENT, Objects.requireNonNull(limit, "limit"));
    }

    /** Returns the {@code stretch} keyword. */
    public static SizingKeyword stretch() {
        return STRETCH_KEYWORD;
    }

    /**
     * Parses a dimension as a sizing keyword.
     *
     * @param dimension the dimension to parse; may be {@code null}
     * @return the parsed keyword, or {@code null} when the dimension is not a sizing keyword
     *         (including {@link TaffyDimension#CONTENT}, which is not a sizing keyword)
     */
    public static SizingKeyword parse(TaffyDimension dimension) {
        if (dimension == null) return null;
        return switch (dimension.getType()) {
            case MIN_CONTENT -> MIN_CONTENT_KEYWORD;
            case MAX_CONTENT -> MAX_CONTENT_KEYWORD;
            case FIT_CONTENT -> dimension.hasFitContentLimit()
                    ? fitContent(dimension.getFitContentLimit())
                    : FIT_CONTENT_KEYWORD;
            case STRETCH -> STRETCH_KEYWORD;
            case LENGTH, PERCENT, AUTO, CALC, CONTENT -> null;
        };
    }

    /**
     * Parses and resolves a dimension in one step.
     *
     * @see #resolve(float, float)
     */
    public static Resolution resolve(TaffyDimension dimension, float stretchSize, float percentResolutionBasis) {
        SizingKeyword keyword = parse(dimension);
        return keyword == null ? null : keyword.resolve(stretchSize, percentResolutionBasis);
    }

    /** Returns the keyword kind. */
    public Kind getKind() {
        return kind;
    }

    /**
     * Returns the {@code fit-content()} limit, or {@code null} for the bare {@code fit-content}
     * keyword and for every other keyword.
     */
    public LengthPercentage getFitContentLimit() {
        return fitContentLimit;
    }

    /** Returns true when this is {@code fit-content(<length-percentage>)} with a limit. */
    public boolean hasFitContentLimit() {
        return fitContentLimit != null;
    }

    /** Returns true for {@link Kind#MIN_CONTENT}. */
    public boolean isMinContent() {
        return kind == Kind.MIN_CONTENT;
    }

    /** Returns true for {@link Kind#MAX_CONTENT}. */
    public boolean isMaxContent() {
        return kind == Kind.MAX_CONTENT;
    }

    /** Returns true for {@link Kind#FIT_CONTENT} (with or without a limit). */
    public boolean isFitContent() {
        return kind == Kind.FIT_CONTENT;
    }

    /** Returns true for {@link Kind#STRETCH}. */
    public boolean isStretch() {
        return kind == Kind.STRETCH;
    }

    /**
     * Resolves this keyword in a layout context.
     *
     * @param stretchSize             the size this dimension would take if stretched to fill the
     *                                available space (available space minus margins), or
     *                                {@link Float#NaN} when it is not known; used by
     *                                {@code stretch} and the bare {@code fit-content}
     * @param percentResolutionBasis  the size a percentage (or calc percentage) in the
     *                                {@code fit-content()} limit resolves against, or
     *                                {@link Float#NaN} when it is not known
     * @return the resolution, or {@code null} when the keyword cannot be resolved in this context
     *         (the caller must then treat it as {@code auto}/{@code content})
     */
    public Resolution resolve(float stretchSize, float percentResolutionBasis) {
        return switch (kind) {
            case MIN_CONTENT -> Resolution.measure(AvailableSpace.minContent());
            case MAX_CONTENT -> Resolution.measure(AvailableSpace.maxContent());
            case STRETCH -> Float.isNaN(stretchSize) ? null : Resolution.exact(stretchSize);
            case FIT_CONTENT -> {
                if (fitContentLimit == null) {
                    yield Float.isNaN(stretchSize)
                            ? null
                            : Resolution.measure(AvailableSpace.definite(stretchSize));
                }
                // fit-content(<length>) is context independent; fit-content(<percentage>) and
                // fit-content(calc()) need the percentage resolution basis (upstream #1247).
                float limit = fitContentLimit.maybeResolve(percentResolutionBasis);
                yield Float.isNaN(limit) ? null : Resolution.measure(AvailableSpace.definite(limit));
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SizingKeyword that = (SizingKeyword) o;
        return kind == that.kind && Objects.equals(fitContentLimit, that.fitContentLimit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, fitContentLimit);
    }

    @Override
    public String toString() {
        return switch (kind) {
            case MIN_CONTENT -> "min-content";
            case MAX_CONTENT -> "max-content";
            case FIT_CONTENT -> fitContentLimit == null ? "fit-content" : "fit-content(" + fitContentLimit + ")";
            case STRETCH -> "stretch";
        };
    }
}
