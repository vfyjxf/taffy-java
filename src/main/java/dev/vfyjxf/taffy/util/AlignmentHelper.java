package dev.vfyjxf.taffy.util;

import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AlignSelf;
import dev.vfyjxf.taffy.style.JustifyContent;
import dev.vfyjxf.taffy.style.TaffyDirection;

/**
 * Shared resolution of the CSS Box Alignment keyword/safety model.
 *
 * <p>Upstream taffy represents alignment as a {@code keyword + safety} struct
 * ({@code src/style/alignment.rs}, #952/#1077/#1254/#1255). Java keeps the public
 * {@link AlignItems}, {@link AlignContent}, {@link AlignSelf} and {@link JustifyContent}
 * enums and encodes the overflow-position modifier in the constant name ({@code SAFE_*}
 * prefixes), appended after the legacy constants so old ordinals stay stable (1.2
 * compatibility policy S2/S2b/Q2). This helper is the single place that turns that flat
 * representation into the same decisions as upstream's
 * {@code src/compute/common/alignment.rs}:</p>
 *
 * <ul>
 *   <li>{@link #applyAlignmentFallback(AlignContent, float, int)} resolves the spec
 *       distribution fallbacks ({@code stretch}/{@code space-*} on a single subject or
 *       overflow) and the {@code safe} overflow fallback to {@code start};</li>
 *   <li>{@link #resolveNormal(AlignContent, AlignContent)} separates an explicit
 *       {@code normal} (layout-mode default, {@code stretch} for flex/grid) from the legacy
 *       {@link AlignContent#AUTO} "not specified" sentinel;</li>
 *   <li>{@link #resolveSelfRelative(AlignItems, TaffyDirection, TaffyDirection, boolean)}
 *       resolves {@code self-start}/{@code self-end} against the item's own direction;</li>
 *   <li>{@link #resolveSelfSafety(AlignItems, boolean)} applies the {@code safe} overflow
 *       fallback for self-alignment;</li>
 *   <li>{@link #computeAlignmentOffset(float, int, float, AlignContent, boolean, boolean)}
 *       computes the first/inter-item offsets including the gap, mirroring upstream's
 *       {@code compute_alignment_offset}.</li>
 * </ul>
 *
 * <p><b>Scope.</b> The flexbox, grid and block algorithms wire this helper (P5): the
 * algorithms resolve {@code NORMAL}/{@code SELF_*}/{@code SAFE_*} through it, while
 * {@code AUTO} remains the Java "unset" sentinel. Absolute positioning still bypasses the
 * container fallback for flex (OF-05/P6); grid/block abspos only normalize the new keywords.</p>
 *
 * <p><b>Direction.</b> {@link TaffyDirection#INHERIT} is a Java extension resolved by
 * {@code LayoutComputer} before layout; this helper treats it as LTR so an unresolved
 * direction degrades to the non-flipped result instead of throwing.</p>
 */
public final class AlignmentHelper {

    private AlignmentHelper() {
    }

    // ------------------------------------------------------------------
    // keyword / safety accessors
    // ------------------------------------------------------------------

    /**
     * Returns whether the alignment carries the {@code safe} overflow-position modifier.
     *
     * @param alignment the alignment value; {@code null} yields {@code false}
     * @return {@code true} for the {@code SAFE_*} constants
     */
    public static boolean isSafe(AlignItems alignment) {
        if (alignment == null) {
            return false;
        }
        return switch (alignment) {
            case SAFE_START, SAFE_END, SAFE_FLEX_START, SAFE_FLEX_END, SAFE_CENTER,
                 SAFE_SELF_START, SAFE_SELF_END -> true;
            case AUTO, NORMAL, START, END, FLEX_START, FLEX_END, SELF_START, SELF_END,
                 CENTER, BASELINE, STRETCH -> false;
        };
    }

    /**
     * Returns whether the alignment carries the {@code safe} overflow-position modifier.
     *
     * @param alignment the alignment value; {@code null} yields {@code false}
     * @return {@code true} for the {@code SAFE_*} constants
     */
    public static boolean isSafe(AlignSelf alignment) {
        if (alignment == null) {
            return false;
        }
        return switch (alignment) {
            case SAFE_START, SAFE_END, SAFE_FLEX_START, SAFE_FLEX_END, SAFE_CENTER,
                 SAFE_SELF_START, SAFE_SELF_END -> true;
            case AUTO, NORMAL, START, END, FLEX_START, FLEX_END, SELF_START, SELF_END,
                 CENTER, BASELINE, STRETCH -> false;
        };
    }

    /**
     * Returns whether the alignment carries the {@code safe} overflow-position modifier.
     *
     * @param alignment the alignment value; {@code null} yields {@code false}
     * @return {@code true} for the {@code SAFE_*} constants
     */
    public static boolean isSafe(AlignContent alignment) {
        if (alignment == null) {
            return false;
        }
        return switch (alignment) {
            case SAFE_START, SAFE_END, SAFE_FLEX_START, SAFE_FLEX_END, SAFE_CENTER -> true;
            case AUTO, NORMAL, START, END, FLEX_START, FLEX_END, CENTER, STRETCH,
                 SPACE_BETWEEN, SPACE_EVENLY, SPACE_AROUND -> false;
        };
    }

    /**
     * Returns whether the alignment carries the {@code safe} overflow-position modifier.
     *
     * @param alignment the alignment value; {@code null} yields {@code false}
     * @return {@code true} for the {@code SAFE_*} constants
     */
    public static boolean isSafe(JustifyContent alignment) {
        if (alignment == null) {
            return false;
        }
        return switch (alignment) {
            case SAFE_START, SAFE_END, SAFE_FLEX_START, SAFE_FLEX_END, SAFE_CENTER -> true;
            case NORMAL, START, END, FLEX_START, FLEX_END, CENTER, STRETCH,
                 SPACE_BETWEEN, SPACE_EVENLY, SPACE_AROUND -> false;
        };
    }

    /**
     * Returns the position keyword with the {@code safe} modifier removed (upstream
     * {@code AlignItems::keyword}).
     *
     * @param alignment the alignment value; {@code null} is returned as {@code null}
     * @return the corresponding non-safe constant, or the input when it is already non-safe
     */
    public static AlignItems keyword(AlignItems alignment) {
        if (alignment == null) {
            return null;
        }
        return switch (alignment) {
            case SAFE_START -> AlignItems.START;
            case SAFE_END -> AlignItems.END;
            case SAFE_FLEX_START -> AlignItems.FLEX_START;
            case SAFE_FLEX_END -> AlignItems.FLEX_END;
            case SAFE_CENTER -> AlignItems.CENTER;
            case SAFE_SELF_START -> AlignItems.SELF_START;
            case SAFE_SELF_END -> AlignItems.SELF_END;
            default -> alignment;
        };
    }

    /**
     * Returns the position keyword with the {@code safe} modifier removed.
     *
     * @param alignment the alignment value; {@code null} is returned as {@code null}
     * @return the corresponding non-safe constant, or the input when it is already non-safe
     */
    public static AlignSelf keyword(AlignSelf alignment) {
        if (alignment == null) {
            return null;
        }
        return switch (alignment) {
            case SAFE_START -> AlignSelf.START;
            case SAFE_END -> AlignSelf.END;
            case SAFE_FLEX_START -> AlignSelf.FLEX_START;
            case SAFE_FLEX_END -> AlignSelf.FLEX_END;
            case SAFE_CENTER -> AlignSelf.CENTER;
            case SAFE_SELF_START -> AlignSelf.SELF_START;
            case SAFE_SELF_END -> AlignSelf.SELF_END;
            default -> alignment;
        };
    }

    /**
     * Returns the position keyword with the {@code safe} modifier removed.
     *
     * @param alignment the alignment value; {@code null} is returned as {@code null}
     * @return the corresponding non-safe constant, or the input when it is already non-safe
     */
    public static AlignContent keyword(AlignContent alignment) {
        if (alignment == null) {
            return null;
        }
        return switch (alignment) {
            case SAFE_START -> AlignContent.START;
            case SAFE_END -> AlignContent.END;
            case SAFE_FLEX_START -> AlignContent.FLEX_START;
            case SAFE_FLEX_END -> AlignContent.FLEX_END;
            case SAFE_CENTER -> AlignContent.CENTER;
            default -> alignment;
        };
    }

    /**
     * Returns the position keyword with the {@code safe} modifier removed.
     *
     * @param alignment the alignment value; {@code null} is returned as {@code null}
     * @return the corresponding non-safe constant, or the input when it is already non-safe
     */
    public static JustifyContent keyword(JustifyContent alignment) {
        if (alignment == null) {
            return null;
        }
        return switch (alignment) {
            case SAFE_START -> JustifyContent.START;
            case SAFE_END -> JustifyContent.END;
            case SAFE_FLEX_START -> JustifyContent.FLEX_START;
            case SAFE_FLEX_END -> JustifyContent.FLEX_END;
            case SAFE_CENTER -> JustifyContent.CENTER;
            default -> alignment;
        };
    }

    /**
     * Returns whether this is the explicit {@code normal} keyword (upstream #1254/#1255).
     * {@link AlignItems#AUTO} is the legacy "not specified" sentinel and is <em>not</em>
     * {@code normal}.
     */
    public static boolean isNormal(AlignItems alignment) {
        return alignment == AlignItems.NORMAL;
    }

    /** Returns whether this is the explicit {@code normal} keyword; {@code AUTO} is not. */
    public static boolean isNormal(AlignSelf alignment) {
        return alignment == AlignSelf.NORMAL;
    }

    /** Returns whether this is the explicit {@code normal} keyword; {@code AUTO} is not. */
    public static boolean isNormal(AlignContent alignment) {
        return alignment == AlignContent.NORMAL;
    }

    /** Returns whether this is the explicit {@code normal} keyword. */
    public static boolean isNormal(JustifyContent alignment) {
        return alignment == JustifyContent.NORMAL;
    }

    /**
     * Returns whether the legacy "not specified" sentinel is set. Java keeps
     * {@link AlignItems#AUTO} so that the public style fields stay non-null and unset can be
     * distinguished from an explicit {@code normal} (upstream #1254 replaced the sentinel with
     * {@code Option}).
     */
    public static boolean isUnset(AlignItems alignment) {
        return alignment == null || alignment == AlignItems.AUTO;
    }

    /** Returns whether the legacy {@link AlignSelf#AUTO} sentinel is set. */
    public static boolean isUnset(AlignSelf alignment) {
        return alignment == null || alignment == AlignSelf.AUTO;
    }

    /** Returns whether the legacy {@link AlignContent#AUTO} sentinel is set. */
    public static boolean isUnset(AlignContent alignment) {
        return alignment == null || alignment == AlignContent.AUTO;
    }

    // ------------------------------------------------------------------
    // normal resolution (explicit normal vs the AUTO sentinel)
    // ------------------------------------------------------------------

    /**
     * Resolves an explicit {@code normal} to the keyword the layout mode uses as its default,
     * leaving every other value untouched.
     *
     * <p>{@code normal} is layout-mode dependent, so the caller supplies the context keyword:
     * flex items/self-alignment and grid items resolve to {@code STRETCH} (grid items may pass
     * {@code START} for the replaced/definite-size cases), block containers resolve content
     * alignment to {@code START}. The legacy {@link AlignItems#AUTO} sentinel is deliberately
     * <em>not</em> replaced: it means "not specified" (inherit / caller default), whereas
     * {@code NORMAL} is an explicit value that overrides such a default.</p>
     *
     * @param alignment     the configured alignment, possibly {@code null}
     * @param normalKeyword the keyword {@code normal} behaves as in this layout context
     * @return the resolved alignment; {@code null} when {@code alignment} is {@code null}
     */
    public static AlignItems resolveNormal(AlignItems alignment, AlignItems normalKeyword) {
        if (alignment == null) {
            return null;
        }
        return alignment == AlignItems.NORMAL ? normalKeyword : alignment;
    }

    /**
     * Resolves an explicit {@code normal} to the keyword the layout mode uses as its default,
     * leaving {@link AlignSelf#AUTO} and every other value untouched. See
     * {@link #resolveNormal(AlignItems, AlignItems)}.
     *
     * @param alignment     the configured alignment, possibly {@code null}
     * @param normalKeyword the keyword {@code normal} behaves as in this layout context
     * @return the resolved alignment; {@code null} when {@code alignment} is {@code null}
     */
    public static AlignSelf resolveNormal(AlignSelf alignment, AlignSelf normalKeyword) {
        if (alignment == null) {
            return null;
        }
        return alignment == AlignSelf.NORMAL ? normalKeyword : alignment;
    }

    /**
     * Resolves an explicit {@code normal} to the keyword the layout mode uses as its default
     * ({@code STRETCH} for flex/grid, {@code START} for block), leaving
     * {@link AlignContent#AUTO} and every other value untouched.
     *
     * @param alignment     the configured alignment, possibly {@code null}
     * @param normalKeyword the keyword {@code normal} behaves as in this layout context
     * @return the resolved alignment; {@code null} when {@code alignment} is {@code null}
     */
    public static AlignContent resolveNormal(AlignContent alignment, AlignContent normalKeyword) {
        if (alignment == null) {
            return null;
        }
        return alignment == AlignContent.NORMAL ? normalKeyword : alignment;
    }

    /**
     * Resolves an explicit {@code normal} to the keyword the layout mode uses as its default
     * ({@code STRETCH} for flex/grid, {@code START} for block), leaving all other values
     * untouched.
     *
     * @param alignment     the configured alignment, possibly {@code null}
     * @param normalKeyword the keyword {@code normal} behaves as in this layout context
     * @return the resolved alignment; {@code null} when {@code alignment} is {@code null}
     */
    public static JustifyContent resolveNormal(JustifyContent alignment, JustifyContent normalKeyword) {
        if (alignment == null) {
            return null;
        }
        return alignment == JustifyContent.NORMAL ? normalKeyword : alignment;
    }

    // ------------------------------------------------------------------
    // self-start / self-end and safe overflow fallback
    // ------------------------------------------------------------------

    /**
     * Resolves the writing-mode-relative {@code SELF_START}/{@code SELF_END} keywords to
     * {@code START}/{@code END} against the item's own direction (upstream #1077, CSS Box
     * Alignment §5.2).
     *
     * <p>In the inline axis the result flips when the item's direction differs from the
     * container's; in the block axis (horizontal-tb only, which is the only writing mode taffy
     * supports) {@code self-start}/{@code self-end} always resolve to {@code start}/{@code end}.
     * The {@code SAFE_SELF_*} variants resolve to {@code SAFE_START}/{@code SAFE_END} and keep
     * the safety modifier. All other keywords are returned unchanged.</p>
     *
     * @param alignment          the item-level alignment, possibly {@code null}
     * @param itemDirection      the item's resolved direction (INHERIT is treated as LTR)
     * @param containerDirection the container's resolved direction (INHERIT is treated as LTR)
     * @param axisIsInline       {@code true} for the inline (horizontal) axis, {@code false} for
     *                           the block (vertical) axis
     * @return the resolved alignment; {@code null} when {@code alignment} is {@code null}
     */
    public static AlignItems resolveSelfRelative(AlignItems alignment,
                                                 TaffyDirection itemDirection,
                                                 TaffyDirection containerDirection,
                                                 boolean axisIsInline) {
        if (alignment == null) {
            return null;
        }
        boolean flip = axisIsInline && isRtl(itemDirection) != isRtl(containerDirection);
        if (flip) {
            return switch (alignment) {
                case SELF_START -> AlignItems.END;
                case SELF_END -> AlignItems.START;
                case SAFE_SELF_START -> AlignItems.SAFE_END;
                case SAFE_SELF_END -> AlignItems.SAFE_START;
                default -> alignment;
            };
        }
        return switch (alignment) {
            case SELF_START -> AlignItems.START;
            case SELF_END -> AlignItems.END;
            case SAFE_SELF_START -> AlignItems.SAFE_START;
            case SAFE_SELF_END -> AlignItems.SAFE_END;
            default -> alignment;
        };
    }

    /**
     * Resolves {@code SELF_START}/{@code SELF_END} against the item's own direction. See
     * {@link #resolveSelfRelative(AlignItems, TaffyDirection, TaffyDirection, boolean)}.
     *
     * @param alignment          the item-level alignment, possibly {@code null}
     * @param itemDirection      the item's resolved direction (INHERIT is treated as LTR)
     * @param containerDirection the container's resolved direction (INHERIT is treated as LTR)
     * @param axisIsInline       {@code true} for the inline (horizontal) axis
     * @return the resolved alignment; {@code null} when {@code alignment} is {@code null}
     */
    public static AlignSelf resolveSelfRelative(AlignSelf alignment,
                                                TaffyDirection itemDirection,
                                                TaffyDirection containerDirection,
                                                boolean axisIsInline) {
        if (alignment == null) {
            return null;
        }
        boolean flip = axisIsInline && isRtl(itemDirection) != isRtl(containerDirection);
        if (flip) {
            return switch (alignment) {
                case SELF_START -> AlignSelf.END;
                case SELF_END -> AlignSelf.START;
                case SAFE_SELF_START -> AlignSelf.SAFE_END;
                case SAFE_SELF_END -> AlignSelf.SAFE_START;
                default -> alignment;
            };
        }
        return switch (alignment) {
            case SELF_START -> AlignSelf.START;
            case SELF_END -> AlignSelf.END;
            case SAFE_SELF_START -> AlignSelf.SAFE_START;
            case SAFE_SELF_END -> AlignSelf.SAFE_END;
            default -> alignment;
        };
    }

    /**
     * Applies the {@code safe} overflow-position fallback for self-alignment (upstream
     * {@code resolve_self_alignment_safety}, #952).
     *
     * <p>When the alignment subject overflows its alignment container and the value carries the
     * {@code safe} modifier, the result is logical {@code START}; otherwise the safety modifier
     * is dropped and the bare keyword is returned. The caller decides whether the item overflows
     * (upstream uses {@code resolved_size + non_auto_margin.sum() > alignment_container_size}).</p>
     *
     * @param alignment the item-level alignment, possibly {@code null}
     * @param overflows whether the alignment subject overflows the alignment container
     * @return the non-safe keyword, {@code START} for a safe value that overflows, or
     *         {@code null} when {@code alignment} is {@code null}
     */
    public static AlignItems resolveSelfSafety(AlignItems alignment, boolean overflows) {
        if (alignment == null) {
            return null;
        }
        if (overflows && isSafe(alignment)) {
            return AlignItems.START;
        }
        return keyword(alignment);
    }

    /**
     * Applies the {@code safe} overflow-position fallback for self-alignment. See
     * {@link #resolveSelfSafety(AlignItems, boolean)}.
     *
     * @param alignment the item-level alignment, possibly {@code null}
     * @param overflows whether the alignment subject overflows the alignment container
     * @return the non-safe keyword, {@code START} for a safe value that overflows, or
     *         {@code null} when {@code alignment} is {@code null}
     */
    public static AlignSelf resolveSelfSafety(AlignSelf alignment, boolean overflows) {
        if (alignment == null) {
            return null;
        }
        if (overflows && isSafe(alignment)) {
            return AlignSelf.START;
        }
        return keyword(alignment);
    }

    // ------------------------------------------------------------------
    // content alignment fallback and offsets
    // ------------------------------------------------------------------

    /**
     * Resolves the spec-defined fallbacks for content alignment and returns the bare position
     * keyword the alignment math should use (upstream {@code apply_alignment_fallback}, #952).
     *
     * <p>Steps, matching upstream:
     * <ol>
     *   <li>{@code normal} (and the legacy {@link AlignContent#AUTO} sentinel) behaves as
     *       {@code stretch} here; callers that need the block-container behaviour resolve
     *       {@code normal} to {@code start} before calling this method;</li>
     *   <li>with a single item ({@code numItems <= 1}) or non-positive free space, the
     *       distribution keywords use their positional fallbacks: {@code stretch} and
     *       {@code space-between} become {@code flex-start}, while {@code space-around} and
     *       {@code space-evenly} become {@code center}; the {@code space-*} fallbacks gain
     *       implicit {@code safe} semantics, whereas {@code stretch} does not;</li>
     *   <li>a {@code safe} value that overflows ({@code freeSpace <= 0}) falls back to
     *       {@code start}.</li>
     * </ol>
     *
     * @param alignment the configured content alignment, possibly {@code null}
     * @param freeSpace the free space in the alignment axis (negative on overflow)
     * @param numItems  the number of alignment subjects (tracks/items/lines)
     * @return the effective non-safe keyword; {@code null} when {@code alignment} is {@code null}
     */
    public static AlignContent applyAlignmentFallback(AlignContent alignment, float freeSpace, int numItems) {
        if (alignment == null) {
            return null;
        }
        return toAlignContent(applyAlignmentFallback(toJustifyContent(alignment), freeSpace, numItems));
    }

    /**
     * Resolves the spec-defined fallbacks for content alignment. See
     * {@link #applyAlignmentFallback(AlignContent, float, int)}.
     *
     * @param alignment the configured content alignment, possibly {@code null}
     * @param freeSpace the free space in the alignment axis (negative on overflow)
     * @param numItems  the number of alignment subjects (tracks/items/lines)
     * @return the effective non-safe keyword; {@code null} when {@code alignment} is {@code null}
     */
    public static JustifyContent applyAlignmentFallback(JustifyContent alignment, float freeSpace, int numItems) {
        if (alignment == null) {
            return null;
        }
        JustifyContent keyword = keyword(alignment);
        if (keyword == JustifyContent.NORMAL) {
            keyword = JustifyContent.STRETCH;
        }
        boolean isSafe = isSafe(alignment);

        // 1. Single-subject / overflow fallbacks for the distribution keywords.
        if (numItems <= 1 || freeSpace <= 0f) {
            switch (keyword) {
                case STRETCH -> keyword = JustifyContent.FLEX_START;
                case SPACE_BETWEEN -> {
                    keyword = JustifyContent.FLEX_START;
                    isSafe = true;
                }
                case SPACE_AROUND, SPACE_EVENLY -> {
                    keyword = JustifyContent.CENTER;
                    isSafe = true;
                }
                default -> {
                    // no-op
                }
            }
        }

        // 2. Safe alignment falls back to the start edge when the subject overflows.
        if (freeSpace <= 0f && isSafe) {
            keyword = JustifyContent.START;
        }
        return keyword;
    }

    /**
     * Computes the alignment offset of one content-alignment subject, mirroring upstream
     * {@code compute_alignment_offset} (shared by flexbox, grid and block; #952).
     *
     * <p>For the first subject the returned value is the offset from the alignment container's
     * start edge (which is negative for overflowing centring/end alignment). For subsequent
     * subjects the value is the delta from the previous subject, i.e. the original gap plus any
     * distributed space ({@code space-between}/{@code space-around}/{@code space-evenly}).</p>
     *
     * <p>Pass an alignment that has already been through
     * {@link #applyAlignmentFallback(AlignContent, float, int)}; the {@code safe} modifier is
     * ignored here (it is resolved by the fallback step) and {@code normal} behaves as
     * {@code stretch}.</p>
     *
     * @param freeSpace            the free space in the alignment axis
     * @param numItems             the number of alignment subjects
     * @param gap                  the fixed gap between subjects
     * @param alignment            the effective content alignment, possibly {@code null}
     * @param layoutIsFlexReversed whether the flex layout direction is reversed
     * @param isFirst              {@code true} for the first subject, {@code false} for the rest
     * @return the offset for the first subject, or the inter-subject delta otherwise
     */
    public static float computeAlignmentOffset(float freeSpace,
                                               int numItems,
                                               float gap,
                                               AlignContent alignment,
                                               boolean layoutIsFlexReversed,
                                               boolean isFirst) {
        if (alignment == null) {
            return 0f;
        }
        return computeAlignmentOffset(freeSpace, numItems, gap, toJustifyContent(alignment),
                layoutIsFlexReversed, isFirst);
    }

    /**
     * Computes the alignment offset of one content-alignment subject. See
     * {@link #computeAlignmentOffset(float, int, float, AlignContent, boolean, boolean)}.
     *
     * @param freeSpace            the free space in the alignment axis
     * @param numItems             the number of alignment subjects
     * @param gap                  the fixed gap between subjects
     * @param alignment            the effective content alignment, possibly {@code null}
     * @param layoutIsFlexReversed whether the flex layout direction is reversed
     * @param isFirst              {@code true} for the first subject, {@code false} for the rest
     * @return the offset for the first subject, or the inter-subject delta otherwise
     */
    public static float computeAlignmentOffset(float freeSpace,
                                               int numItems,
                                               float gap,
                                               JustifyContent alignment,
                                               boolean layoutIsFlexReversed,
                                               boolean isFirst) {
        if (alignment == null) {
            return 0f;
        }
        JustifyContent keyword = keyword(alignment);
        if (keyword == JustifyContent.NORMAL) {
            keyword = JustifyContent.STRETCH;
        }

        if (isFirst) {
            return switch (keyword) {
                case START, SAFE_START, STRETCH, SPACE_BETWEEN -> 0f;
                case FLEX_START, SAFE_FLEX_START -> layoutIsFlexReversed ? freeSpace : 0f;
                case END, SAFE_END -> freeSpace;
                case FLEX_END, SAFE_FLEX_END -> layoutIsFlexReversed ? 0f : freeSpace;
                case CENTER, SAFE_CENTER -> freeSpace / 2f;
                case SPACE_AROUND -> {
                    if (freeSpace >= 0f) {
                        yield (freeSpace / Math.max(numItems, 1)) / 2f;
                    }
                    yield freeSpace / 2f;
                }
                case SPACE_EVENLY -> {
                    if (freeSpace >= 0f) {
                        yield freeSpace / (Math.max(numItems, 0) + 1);
                    }
                    yield freeSpace / 2f;
                }
                case NORMAL -> 0f;
            };
        }

        float positiveFreeSpace = Math.max(freeSpace, 0f);
        float distributed = switch (keyword) {
            case SPACE_BETWEEN -> numItems > 1 ? positiveFreeSpace / (numItems - 1) : 0f;
            case SPACE_AROUND -> numItems > 0 ? positiveFreeSpace / numItems : 0f;
            case SPACE_EVENLY -> positiveFreeSpace / (Math.max(numItems, 0) + 1);
            default -> 0f;
        };
        return gap + distributed;
    }

    // ------------------------------------------------------------------
    // internal conversions between the two content-alignment enums
    // ------------------------------------------------------------------

    private static boolean isRtl(TaffyDirection direction) {
        return direction == TaffyDirection.RTL;
    }

    /**
     * Converts between the two content-alignment enums. {@link AlignContent#AUTO} has no
     * separate keyword and maps to {@code NORMAL}; the fallback resolves {@code NORMAL} to
     * {@code STRETCH} before converting back, so the sentinel never leaks into a result.
     */
    private static JustifyContent toJustifyContent(AlignContent alignment) {
        return switch (alignment) {
            case AUTO, NORMAL -> JustifyContent.NORMAL;
            case START -> JustifyContent.START;
            case END -> JustifyContent.END;
            case FLEX_START -> JustifyContent.FLEX_START;
            case FLEX_END -> JustifyContent.FLEX_END;
            case CENTER -> JustifyContent.CENTER;
            case STRETCH -> JustifyContent.STRETCH;
            case SPACE_BETWEEN -> JustifyContent.SPACE_BETWEEN;
            case SPACE_EVENLY -> JustifyContent.SPACE_EVENLY;
            case SPACE_AROUND -> JustifyContent.SPACE_AROUND;
            case SAFE_START -> JustifyContent.SAFE_START;
            case SAFE_END -> JustifyContent.SAFE_END;
            case SAFE_FLEX_START -> JustifyContent.SAFE_FLEX_START;
            case SAFE_FLEX_END -> JustifyContent.SAFE_FLEX_END;
            case SAFE_CENTER -> JustifyContent.SAFE_CENTER;
        };
    }

    /** Converts a content-alignment keyword back to {@link AlignContent}. */
    private static AlignContent toAlignContent(JustifyContent alignment) {
        return switch (alignment) {
            case NORMAL -> AlignContent.NORMAL;
            case START -> AlignContent.START;
            case END -> AlignContent.END;
            case FLEX_START -> AlignContent.FLEX_START;
            case FLEX_END -> AlignContent.FLEX_END;
            case CENTER -> AlignContent.CENTER;
            case STRETCH -> AlignContent.STRETCH;
            case SPACE_BETWEEN -> AlignContent.SPACE_BETWEEN;
            case SPACE_EVENLY -> AlignContent.SPACE_EVENLY;
            case SPACE_AROUND -> AlignContent.SPACE_AROUND;
            case SAFE_START -> AlignContent.SAFE_START;
            case SAFE_END -> AlignContent.SAFE_END;
            case SAFE_FLEX_START -> AlignContent.SAFE_FLEX_START;
            case SAFE_FLEX_END -> AlignContent.SAFE_FLEX_END;
            case SAFE_CENTER -> AlignContent.SAFE_CENTER;
        };
    }
}
