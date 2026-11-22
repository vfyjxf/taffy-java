package dev.vfyjxf.taffy.style;

/**
 * The layout-affecting parts of the CSS {@code contain} property.
 *
 * <p>Containment limits the ways in which a box's contents can affect layout outside of the box
 * (and vice versa). This port follows upstream taffy #1128 ({@code 4ae4219}), which implements the
 * layout-relevant parts of the {@code layout} and {@code paint} containment types:</p>
 *
 * <ul>
 *   <li>{@link #LAYOUT} (layout containment): the box establishes an independent formatting
 *       context, and is treated as having no baseline for baseline-alignment purposes.
 *       <a href="https://drafts.csswg.org/css-contain-2/#containment-layout">css-contain-2</a></li>
 *   <li>{@link #PAINT} (paint containment): the box establishes an independent formatting context.
 *       Paint containment's other effects (clipping, containing absolutely-positioned descendants,
 *       stacking context) are outside of the layout algorithm's scope.</li>
 * </ul>
 *
 * <p><b>Size containment is not implemented.</b> Upstream #1128 deliberately ships without it (see
 * the upstream CHANGELOG: "The {@code strict}, {@code size} and {@code inline-size} values are not
 * supported as size containment is not implemented"). The {@link #SIZE}, {@link #INLINE_SIZE},
 * {@link #STRICT} and {@link #CONTENT} constants below still exist so that CSS values and future
 * API consumers can be represented, but they have <em>no layout effect</em> in this port:
 * {@link #STRICT} and {@link #SIZE} do not make a box size as if it were empty, and
 * {@link #SIZE}/{@link #INLINE_SIZE} do not establish an independent formatting context on their
 * own. Only {@link #LAYOUT} and {@link #PAINT} drive layout.</p>
 *
 * <p><b>Representation.</b> This is an immutable bit set (not an {@link java.util.EnumSet}): the
 * constants are flyweights that can be shared freely, testing a flag is a single {@code int}
 * mask operation, and combining flags allocates nothing in the layout hot path. The bit values
 * mirror upstream's flag order ({@code SIZE = 1 << 0}, {@code INLINE_SIZE = 1 << 1},
 * {@code LAYOUT = 1 << 2}, {@code PAINT = 1 << 3}) so the port stays easy to diff against
 * {@code src/style/mod.rs}.</p>
 *
 * <p>Instances are immutable; the {@code union} of two values is a new value and never mutates an
 * operand. {@code null} is never returned by any method of this class.</p>
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/contain">MDN - contain</a>
 */
public final class Contain {

    private final int bits;

    private Contain(int bits) {
        this.bits = bits;
    }

    /**
     * No containment (the default).
     */
    public static final Contain NONE = new Contain(0);

    /**
     * Size containment ({@code contain: size}).
     *
     * <p><b>No layout effect:</b> size containment is not implemented in this port, matching
     * upstream taffy #1128.</p>
     *
     * @see <a href="https://drafts.csswg.org/css-contain-2/#containment-size">css-contain-2</a>
     */
    public static final Contain SIZE = new Contain(1 << 0);

    /**
     * Inline-size containment ({@code contain: inline-size}).
     *
     * <p><b>No layout effect:</b> inline-size containment is not implemented in this port, matching
     * upstream taffy #1128.</p>
     *
     * @see <a href="https://drafts.csswg.org/css-contain-2/#containment-inline-size">css-contain-2</a>
     */
    public static final Contain INLINE_SIZE = new Contain(1 << 1);

    /**
     * Layout containment ({@code contain: layout}): the box establishes an independent formatting
     * context and is treated as having no baseline for baseline-alignment purposes.
     *
     * @see <a href="https://drafts.csswg.org/css-contain-2/#containment-layout">css-contain-2</a>
     */
    public static final Contain LAYOUT = new Contain(1 << 2);

    /**
     * Paint containment ({@code contain: paint}): the box establishes an independent formatting
     * context. Unlike {@link #LAYOUT}, paint containment does not suppress the box's baseline.
     *
     * @see <a href="https://drafts.csswg.org/css-contain-2/#containment-paint">css-contain-2</a>
     */
    public static final Contain PAINT = new Contain(1 << 3);

    /**
     * The containment implied by {@code contain: strict} ({@code size layout paint}, ignoring
     * style containment).
     *
     * <p>Only the {@link #LAYOUT} and {@link #PAINT} parts have a layout effect in this port; the
     * {@link #SIZE} part is carried for API/value parity and is ignored by the layout algorithms.</p>
     */
    public static final Contain STRICT = SIZE.union(LAYOUT).union(PAINT);

    /**
     * The containment implied by {@code contain: content} ({@code layout paint}, ignoring style
     * containment).
     */
    public static final Contain CONTENT = LAYOUT.union(PAINT);

    /**
     * The default containment (no containment).
     */
    public static final Contain DEFAULT = NONE;

    /**
     * Returns whether {@code self} contains all of the containment types in {@code other}.
     *
     * <p>An empty value is contained in every value ({@code contains(NONE)} is always
     * {@code true}).</p>
     *
     * @param other the containment types to test for
     * @return {@code true} if every flag of {@code other} is also set in this value
     */
    public boolean contains(Contain other) {
        return (bits & other.bits) == other.bits;
    }

    /**
     * Returns whether {@code self} contains any of the containment types in {@code other}.
     *
     * @param other the containment types to test for
     * @return {@code true} if at least one flag of {@code other} is also set in this value
     */
    public boolean intersects(Contain other) {
        return (bits & other.bits) != 0;
    }

    /**
     * Returns the union of the containment types in this value and {@code other}.
     *
     * <p>The receiver is never modified; combining two values allocates a new value.</p>
     *
     * @param other the containment types to add
     * @return a value whose flags are the union of both operands
     */
    public Contain union(Contain other) {
        int combined = bits | other.bits;
        if (combined == bits) {
            return this;
        }
        if (combined == other.bits) {
            return other;
        }
        return new Contain(combined);
    }

    /**
     * Returns whether this value contains no containment type.
     *
     * @return {@code true} for {@link #NONE} (and only {@link #NONE})
     */
    public boolean isEmpty() {
        return bits == 0;
    }

    /**
     * Whether this containment causes the box to establish an independent formatting context.
     *
     * <p>Both layout and paint containment do (upstream #1128:
     * {@code Contain::establishes_independent_formatting_context}). Size and inline-size
     * containment do not, because they are not implemented in this port.</p>
     *
     * @return {@code true} if {@code contain: layout} or {@code contain: paint} is present
     */
    public boolean establishesIndependentFormattingContext() {
        return intersects(LAYOUT.union(PAINT));
    }

    /**
     * Whether this containment suppresses the box's baseline for baseline-alignment purposes.
     *
     * <p>Only layout containment does; paint containment establishes an independent formatting
     * context but keeps its baseline (upstream #1128: {@code Contain::suppresses_baseline}).</p>
     *
     * @return {@code true} if {@code contain: layout} is present
     */
    public boolean suppressesBaseline() {
        return contains(LAYOUT);
    }

    /**
     * Whether this containment prevents the box's overflowing content from contributing to an
     * ancestor's scrollable overflow region (layout containment treats such overflow as ink
     * overflow; paint containment clips it).
     *
     * <p>Upstream #1128 applies this gate in
     * {@code compute_scrollable_overflow_contribution}. This port keeps the predicate for API
     * parity and future wiring; the block/leaf scrollable-overflow accumulation does not consult it
     * yet (see the P7 residual notes), so a contained box still propagates its overflow.</p>
     *
     * @return {@code true} if {@code contain: layout} or {@code contain: paint} is present
     */
    public boolean containsScrollableOverflow() {
        return intersects(LAYOUT.union(PAINT));
    }

    /**
     * Returns the raw flag bits of this value.
     *
     * <p>The value is stable and documented for debugging/serialization only; use the named
     * constants and the predicate methods instead of depending on the numeric value.</p>
     *
     * @return the bit mask of the containment types in this value
     */
    public int bits() {
        return bits;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Contain contain && bits == contain.bits);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(bits);
    }

    @Override
    public String toString() {
        if (bits == 0) {
            return "Contain{NONE}";
        }
        StringBuilder builder = new StringBuilder("Contain{");
        appendFlag(builder, SIZE, "SIZE");
        appendFlag(builder, INLINE_SIZE, "INLINE_SIZE");
        appendFlag(builder, LAYOUT, "LAYOUT");
        appendFlag(builder, PAINT, "PAINT");
        return builder.append('}').toString();
    }

    private void appendFlag(StringBuilder builder, Contain flag, String name) {
        if (contains(flag)) {
            if (builder.charAt(builder.length() - 1) != '{') {
                builder.append(' ');
            }
            builder.append(name);
        }
    }
}
