package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.BoxGenerationMode;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.Clear;
import dev.vfyjxf.taffy.style.Contain;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyFloat;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TextAlign;
import dev.vfyjxf.taffy.util.AlignmentHelper;
import dev.vfyjxf.taffy.util.BaselineHelper;
import dev.vfyjxf.taffy.util.BfcSlot;
import dev.vfyjxf.taffy.util.BlockContext;
import dev.vfyjxf.taffy.util.ContentSizeUtil;
import dev.vfyjxf.taffy.util.FloatContext;
import dev.vfyjxf.taffy.util.Resolve;
import dev.vfyjxf.taffy.util.ScrollableOverflowUtil;
import dev.vfyjxf.taffy.util.SizingKeyword;
import dev.vfyjxf.taffy.util.TaffyMath;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Float.NaN;

/**
 * Computes block layout for nodes with display: block.
 */
public class BlockComputer {

    private final LayoutComputer layoutComputer;

    /**
     * A shared all-zero size used for the default {@code border-box} sizing adjustment. Only
     * ever read, never mutated: the adjusted sizes built from it are fresh values.
     */
    private static final FloatSize ZERO_SIZE = new FloatSize(0f, 0f);

    /**
     * A shared all-zero rectangle, used by an item whose padding or border resolves to zero on
     * every side, and for a reported {@code Layout.margin} whose four components are all zero.
     * Only ever read: item geometry is never mutated after {@link #generateItemList}, and every
     * consumer of a reported layout only reads its rectangles (rounding rebuilds them through
     * {@code roundRect} instead of mutating them).
     */
    private static final FloatRect ZERO_RECT = new FloatRect(0f, 0f, 0f, 0f);

    /**
     * A shared fully-unresolved size (both axes {@link Float#NaN}), used by an item whose
     * size/min-size/max-size cannot resolve in any basis ({@code auto} or an intrinsic keyword).
     * Only ever read.
     */
    private static final FloatSize NONE_SIZE = new FloatSize(NaN, NaN);

    /**
     * The pass-scoped item-list cache; see {@link ItemListCache}.
     */
    private static final ThreadLocal<ItemListCache> ITEM_LIST_CACHE =
        ThreadLocal.withInitial(ItemListCache::new);

    /**
     * The maximum number of {@link BlockItem}s one pass may pin in {@link #ITEM_LIST_CACHE}. The
     * item metadata of the containers seen so far is only reclaimed at the next pass, so a large
     * tree needs a bound; beyond it the cache is dropped and rebuilt (correct, just uncached).
     */
    private static final int MAX_CACHED_ITEMS = 1 << 16;

    public BlockComputer(LayoutComputer layoutComputer) {
        this.layoutComputer = layoutComputer;
    }

    /**
     * Internal data structure for block items.
     */
    private static class BlockItem {
        NodeId nodeId;
        int order;
        boolean isTable;
        /**
         * Whether this item is a replaced element or a compressible replaced form control. Such
         * items are exempt from block stretch sizing and cap their min-content contribution
         * (upstream #1002/#1401).
         */
        boolean isCompressibleReplaced;
        /**
         * For compressible replaced items, the cap on their min-content contribution from a
         * percentage {@code width} or {@code max-width} resolved against zero. NaN when the item
         * has no such cap.
         * <a href="https://drafts.csswg.org/css-sizing-3/#replaced-percentage-min-contribution">
         * CSS Sizing 3: replaced percentage min contribution</a>; upstream #1401 (4aca4f49).
         */
        float minContentWidthCap = NaN;
        /**
         * The item's raw style height, kept so that {@code height: stretch} can be resolved
         * against the containing block's content box at layout time. Upstream #1242 (6a36efbe)
         * resolves only {@code stretch} explicitly in the block axis; the other intrinsic sizing
         * keywords are equal to the content size and therefore behave like {@code auto}.
         */
        TaffyDimension heightStyle;
        /**
         * The item's raw style {@code width}, kept so that a sizing keyword
         * ({@code min-content}, {@code max-content}, {@code fit-content}, {@code stretch}) can be
         * resolved in the containing block's layout context. A keyword resolves to
         * {@link Float#NaN} in {@link #size} and therefore needs its own resolution pass.
         * Upstream {@code resolve_sizing_keyword} (#1099/#1103/#1247).
         */
        TaffyDimension widthStyle;
        /**
         * Whether the item's size in this axis was derived from the other axis through
         * {@code aspect-ratio} rather than specified. A derived axis is subject to the automatic
         * minimum size (the content-based floor), while a specified one is not.
         */
        boolean widthFromAspectRatio;
        boolean heightFromAspectRatio;
        /** The item's preferred aspect ratio ({@code NaN} when it has none). */
        float aspectRatio = NaN;
        FloatSize size;
        FloatSize minSize;
        FloatSize maxSize;
        TaffyPoint<Overflow> overflow;
        float scrollbarWidth;
        TaffyPosition position;
        TaffyRect<LengthPercentageAuto> inset;
        TaffyRect<LengthPercentageAuto> margin;
        /**
         * The item's margins as pre-resolved at item-build time when their used value cannot
         * depend on the containing block's content-box width, i.e. a {@code length} (kept as is)
         * or an {@code auto}/intrinsic-keyword side (which resolves to nothing, so
         * {@link Float#NaN}). A {@code percent} or {@code calc()} component does depend on that
         * width and stays unresolved here; the matching {@code margin*Basis} flag says so.
         *
         * <p>Resolving them once per item instead of once per layout pass removes four
         * {@link LengthPercentageAuto#maybeResolve} and two {@link LengthPercentageAuto#isAuto}
         * calls per child from {@link #performFinalLayoutOnChildren}, which the block profile of
         * {@code formPerFrameStyleChange} reported as the largest self-time there (P18). The
         * values are style-derived item metadata, so they are immutable for the lifetime of the
         * item and reset by nothing in {@link #resetPerPassState}.</p>
         */
        float marginLeft = NaN;
        float marginRight = NaN;
        float marginTop = NaN;
        float marginBottom = NaN;
        /**
         * Whether the matching margin side is {@code auto}: the resolved used value is only known
         * after layout (an auto inline-axis margin absorbs the free space).
         */
        boolean marginLeftIsAuto;
        boolean marginRightIsAuto;
        /** Whether the matching margin side still needs the container's inner width at layout time. */
        boolean marginLeftNeedsInnerWidth;
        boolean marginRightNeedsInnerWidth;
        boolean marginTopNeedsInnerWidth;
        boolean marginBottomNeedsInnerWidth;
        /**
         * Whether all four margins were pre-resolved, i.e. the layout-time margin resolution reads
         * the four primitives and never touches the style object. This is the common case (a
         * child with plain lengths or no margins at all).
         */
        boolean marginsPreResolved;
        FloatRect padding;
        FloatRect border;
        FloatSize paddingBorderSum;
        /**
         * The child's static-position point. Only an out-of-flow child needs one (its containing
         * block's inline static-position rectangle plus the hypothetical block position); an
         * in-flow child shares the read-only zero because nothing reads it there. Filled in by
         * {@link #performFinalLayoutOnChildren}.
         */
        FloatPoint staticPosition;
        boolean canBeCollapsedThrough;
        /**
         * The item's raw {@code justify-self} value; {@link AlignItems#AUTO} is the legacy
         * "not specified" sentinel (the container's {@code justify-items} applies instead).
         * Upstream #1250.
         */
        AlignItems justifySelf;
        /**
         * The item's raw {@code align-self} value; {@link AlignItems#AUTO} is the legacy
         * "not specified" sentinel. Block layout has no {@code align-items} fallback for an
         * out-of-flow child, so an unset value behaves as {@code normal}. Upstream #1206.
         */
        AlignItems alignSelf;
        /**
         * Resolved inline-axis self alignment of an out-of-flow child ({@code null} = normal),
         * captured when the child's static position is generated. Upstream #1206.
         */
        AlignItems staticAlignX;
        /** Resolved block-axis self alignment of an out-of-flow child ({@code null} = normal). */
        AlignItems staticAlignY;
        /**
         * The inline-axis static-position rectangle of an out-of-flow child, in this container's
         * border-box frame: it spans the container's content box
         * (css-position-3 static-position rectangle).
         */
        float staticAreaStartX;
        float staticAreaEndX;
        /**
         * The item's resolved direction ({@code INHERIT} already resolved through the parent
         * chain), used to resolve {@code self-start}/{@code self-end} in the inline axis.
         * Upstream #1077.
         */
        TaffyDirection direction;
        /**
         * Whether the item's style {@code width} is {@code auto}. Only such a box resolves its
         * width to {@code fit-content} when {@code justify-self} is neither {@code normal} nor
         * {@code stretch}. Upstream #1250.
         */
        boolean widthIsAuto;
        /**
         * The child's CSS {@code float} value. Floated children are placed out of the normal
         * block flow by {@link #performFinalLayoutOnChildren} and do not advance the in-flow
         * cursor.
         */
        TaffyFloat floatStyle = TaffyFloat.NONE;
        /**
         * The child's display type, used to decide whether it participates in the parent's
         * block formatting context or establishes its own.
         */
        TaffyDisplay display = TaffyDisplay.BLOCK;
        /**
         * The child's CSS contain flags. Layout and paint containment make the child establish an
         * independent formatting context, so its floats stay inside it, external floats are only
         * avoided (never entered) and its margins never collapse through the parent's edges.
         */
        Contain contain = Contain.NONE;
        /**
         * The child's CSS {@code clear} value. Floats pass it to the placement algorithm; normal
         * in-flow boxes use it to move below cleared floats.
         */
        Clear clear = Clear.NONE;
        /**
         * Pending layout of an in-flow child. Held back from {@code setUnroundedLayout} so that a
         * measurement pass ({@link RunMode#COMPUTE_SIZE}) can return before any tree state is
         * written; the final layout commits them in one place. Upstream #971 (BK-01).
         */
        Layout finalLayout;
    }

    /**
     * A container's item list kept for the duration of one layout pass.
     *
     * <p>A {@link BlockComputer} is created per compute call (see {@code LayoutComputer}'s block
     * dispatch), so a pass-scoped cache cannot live on the instance: the pass is the lifetime of
     * one {@link LayoutComputer} and of one direction epoch (the tree bumps it when a pass
     * starts). The cache therefore lives on a thread-local keyed by the owning tree and epoch, so
     * a later pass, another tree or a re-entrant pass discards it instead of observing styles that
     * have since changed.</p>
     *
     * <p>Within a pass a container's item list is a pure function of its child styles, its child
     * order, its inner size and its resolved direction, and none of those can change while the
     * pass runs (styles only change through {@link TaffyTree#setStyle}, which starts a new epoch).
     * Reusing the list is what keeps a measurement pass and the final layout from resolving the
     * same child styles twice; the deferred per-child layout results filled in by the passes are
     * reset before a reused list is handed out.</p>
     */
    private static final class CachedItemList {
        final List<BlockItem> items;
        final float innerWidth;
        final float innerHeight;
        final TaffyDirection direction;
        /**
         * Whether the item metadata was built from child styles that resolve against the
         * container's inner size at all. When they do not (the common case: no percentage or
         * {@code calc()} padding, border, size, min-size or max-size), the same list is valid for
         * every inner size, so a measurement and a final layout at different known dimensions
         * share it.
         */
        final boolean innerSizeIndependent;

        CachedItemList(List<BlockItem> items, FloatSize innerSize, TaffyDirection direction,
                       boolean innerSizeIndependent) {
            this.items = items;
            this.innerWidth = innerSize.width;
            this.innerHeight = innerSize.height;
            this.direction = direction;
            this.innerSizeIndependent = innerSizeIndependent;
        }

        boolean matches(FloatSize innerSize, TaffyDirection containerDirection) {
            if (direction != containerDirection) {
                return false;
            }
            return innerSizeIndependent
                   || (Float.floatToIntBits(innerWidth) == Float.floatToIntBits(innerSize.width)
                       && Float.floatToIntBits(innerHeight) == Float.floatToIntBits(innerSize.height));
        }
    }

    /**
     * The per-pass item-list cache. The owning tree is held weakly: the cached items reference
     * nothing of the tree, so a caller that stops using a tree does not keep it alive through this
     * thread-local. {@link #epoch} is the tree's direction epoch; a different value means the pass
     * is over (or a style/child-list change happened) and the entries are dropped.
     */
    private static final class ItemListCache {
        private WeakReference<TaffyTree> tree;
        private long epoch = Long.MIN_VALUE;
        private final Long2ObjectOpenHashMap<CachedItemList> lists = new Long2ObjectOpenHashMap<>();
        private int cachedItems;

        void store(long nodeId, List<BlockItem> items, FloatSize innerSize,
                   TaffyDirection direction, boolean innerSizeIndependent) {
            CachedItemList previous = lists.put(nodeId,
                new CachedItemList(items, innerSize, direction, innerSizeIndependent));
            if (previous != null) {
                cachedItems -= previous.items.size();
            }
            cachedItems += items.size();
            if (cachedItems > MAX_CACHED_ITEMS) {
                // Bound the memory a single pass may pin: a list built once per container is only
                // worth keeping while the pass is small enough to matter.
                lists.clear();
                cachedItems = 0;
            }
        }
    }

    /**
     * A block container's item list that survives from one layout pass to the next, stored on the
     * container's {@link NodeData}.
     *
     * <p>A {@link BlockItem} list is a pure function of the child styles and their order, the
     * container's resolved direction and - for percentage or {@code calc()} child padding, border,
     * size, min-size or max-size - the container's content-box size. The stamps captured here let
     * a later pass reuse the list without resolving every child style again: the container's own
     * content version, its child-list version and each direct child's content version are
     * compared. A child style change therefore invalidates only its direct parent's list, not the
     * lists of its grandparents, which cannot observe anything below their direct children. The
     * measure-callback and node-context setters announce their change through
     * {@link TaffyTree#markDirty(NodeId)}, which bumps the same content version.</p>
     *
     * <p>Only style-derived metadata is stored; the per-pass results filled in by
     * {@link #performFinalLayoutOnChildren} are reset before a reused list is handed out. The
     * cache is populated only for containers with more than one item, where rebuilding the list is
     * the cost worth avoiding; a single-item container keeps using the pass-scoped
     * {@link CachedItemList} only.</p>
     */
    static final class BlockItemCache {
        private final List<BlockItem> items;
        private final long containerVersion;
        private final long childrenVersion;
        private final long[] childVersions;
        private final TaffyDirection direction;
        private final float innerWidth;
        private final float innerHeight;
        private final boolean innerSizeIndependent;

        private BlockItemCache(
            List<BlockItem> items,
            long containerVersion,
            long childrenVersion,
            long[] childVersions,
            TaffyDirection direction,
            FloatSize innerSize,
            boolean innerSizeIndependent
        ) {
            this.items = items;
            this.containerVersion = containerVersion;
            this.childrenVersion = childrenVersion;
            this.childVersions = childVersions;
            this.direction = direction;
            this.innerWidth = innerSize.width;
            this.innerHeight = innerSize.height;
            this.innerSizeIndependent = innerSizeIndependent;
        }

        /**
         * Whether this cached list still describes the container's current children and layout
         * basis. Child ids and order are covered by the child-list version; the per-child content
         * versions cover style replacement and direct {@code markDirty} (in-place style mutation,
         * measure-callback and node-context changes).
         */
        private boolean matches(NodeData container, List<NodeId> children, TaffyTree tree,
                                FloatSize innerSize, TaffyDirection containerDirection) {
            if (containerVersion != container.getContentVersion()
                || childrenVersion != container.getChildrenVersion()
                || direction != containerDirection
                || childVersions.length != children.size()) {
                return false;
            }
            if (!innerSizeIndependent
                && (Float.floatToIntBits(innerWidth) != Float.floatToIntBits(innerSize.width)
                    || Float.floatToIntBits(innerHeight) != Float.floatToIntBits(innerSize.height))) {
                return false;
            }
            for (int i = 0; i < childVersions.length; i++) {
                NodeData child = tree.getNodeData(children.get(i));
                if (child == null || childVersions[i] != child.getContentVersion()) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Result of performing final layout on in-flow children, including margin collapse info.
     */
    private record InFlowLayoutResult(
        float contentHeight, CollapsibleMarginSet firstChildTopMarginSet,
        CollapsibleMarginSet lastChildBottomMarginSet,
        boolean allChildrenCanBeCollapsedThrough,
        float firstBaseline,
        float floatContentContribution
    ) {}

    /**
     * Computes block layout for a node with a fresh block formatting context.
     */
    public LayoutOutput compute(NodeId node, LayoutInput inputs, TaffyStyle style) {
        return compute(node, inputs, style, null);
    }

    /**
     * Computes block layout for a node.
     *
     * @param inheritedContext the block formatting context inherited from the parent block, or
     *                         {@code null} when this node starts a new BFC. Flow-root and scroll
     *                         containers always start a new BFC regardless of this value.
     */
    public LayoutOutput compute(
        NodeId node, LayoutInput inputs, TaffyStyle style, BlockContext inheritedContext) {
        TaffyTree tree = layoutComputer.getTree();
        // The tree root is the initial formatting context root (CSS2 §9.4.1): its own floats
        // contribute to its auto height. This is deliberately separate from the BFC/margin
        // collapse decision below so ordinary blocks keep their existing behavior.
        boolean isTreeRoot = tree.getParent(node) == null;
        // A box laid out as a float establishes its own BFC even when its display is BLOCK. The
        // flag is passed through BlockContext by the parent's float branch.
        boolean laidOutAsFloat = inheritedContext != null && inheritedContext.isFloatBfcRoot();
        FloatSize knownDimensions = inputs.knownDimensions();
        FloatSize parentSize = inputs.parentSize();
        TaffySize<AvailableSpace> availableSpace = inputs.availableSpace();
        RunMode runMode = inputs.runMode();
        TaffyLine<Boolean> verticalMarginsAreCollapsible = inputs.verticalMarginsAreCollapsible();

        Float aspectRatio = style.getAspectRatio();
        FloatRect padding = Resolve.resolveRectOrZero(style.getPadding(), parentSize.width);
        FloatRect border = Resolve.resolveRectOrZero(style.getBorder(), parentSize.width);

        // Scrollbar gutter calculation - axes are transposed
        TaffyPoint<Overflow> overflow = style.getOverflow();
        float scrollbarWidth = style.getScrollbarWidth();
        float scrollbarGutterRight = overflow.y == Overflow.SCROLL ? scrollbarWidth : 0f;
        float scrollbarGutterBottom = overflow.x == Overflow.SCROLL ? scrollbarWidth : 0f;
        FloatRect scrollbarGutter = new FloatRect(0f, scrollbarGutterRight, 0f, scrollbarGutterBottom);

        // Layout and paint containment: both make this box establish an independent formatting
        // context (the same layout effect as display: flow-root), while layout containment
        // additionally suppresses the box's baseline. Size/inline-size containment have no layout
        // effect (upstream #1128 does not implement size containment). Upstream #1128.
        Contain contain = style.getContain();

        FloatSize paddingBorderSize = new FloatSize(
            padding.left + padding.right + border.left + border.right,
            padding.top + padding.bottom + border.top + border.bottom
        );

        FloatSize boxSizingAdjustment = style.getBoxSizing() == BoxSizing.CONTENT_BOX
                                        ? paddingBorderSize
                                        : new FloatSize(0f, 0f);

        // The style size is resolved without the aspect ratio: a transferred size must be derived
        // from the *clamped* size of the other axis (a `width: 300px; max-width: 100px` box with
        // ratio 2 is 100px wide and therefore 50px tall, not the 150px its specified width would
        // transfer). The transfer happens below, after min/max clamping.
        FloatSize size2 = Resolve.maybeResolveSize(style.getSize(), parentSize);
        FloatSize sizeStyle = maybeAdd(size2, boxSizingAdjustment);

        // min/max constraints are NOT transferred through the aspect ratio: only the used size of
        // one axis transfers to the other. A transferred cross-axis constraint would let, for
        // example, `max-width: 100px` cap a content-sized height (Chrome keeps the height at the
        // content size), which is neither the css-sizing-4 rule nor what any browser does.
        FloatSize size1 = Resolve.maybeResolveSize(style.getMinSize(), parentSize);
        FloatSize minSize = maybeAdd(size1, boxSizingAdjustment);

        FloatSize size = Resolve.maybeResolveSize(style.getMaxSize(), parentSize);
        FloatSize maxSize = maybeAdd(size, boxSizingAdjustment);

        // A `display: flow-root` box always establishes a new block formatting context, and a
        // scroll container (including `overflow: hidden`) does too. The parts of that behaviour
        // this port now expresses are margin isolation and containing the container's own floats
        // (its auto height grows to the floats' bottom edge, upstream #1384). Avoiding a sibling's
        // floats still uses this container's own local context only; sharing one context across a
        // nested formatting context needs the layout-computer entry point to carry it (TODO).
        // BK-17 / upstream #997 (10004cea); float containment / #866 / #1384.
        boolean establishesNewBlockFormattingContext =
            style.getDisplay() == TaffyDisplay.FLOW_ROOT ||
            style.getOverflow().x.isScrollContainer() ||
            style.getOverflow().y.isScrollContainer() ||
            contain.establishesIndependentFormattingContext() ||
            laidOutAsFloat;
        // Only a BFC root (or the initial tree-root formatting context) contains its floats in
        // its auto height; a plain nested block keeps ignoring them.
        boolean containsFloats = establishesNewBlockFormattingContext || isTreeRoot;

        // Determine margin collapsing behaviour.
        //
        // `childrenMarginsAdjoinOwnEdges` answers "could a child's margin collapse through this
        // box's block-axis edge?": the box does not establish a new BFC and has neither border
        // nor padding on that side. `ownMarginsCollapseWithChildren` adds the CSS2 §8.3.1 height
        // condition on the end edge (a box with a non-auto height does not collapse its bottom
        // margin with its last child's).
        //
        // The split matters for `height: stretch`: a child margin that adjoins the containing
        // block's edge is treated as zero when resolving the stretch size, while a margin that
        // cannot collapse through that edge is subtracted from it. Upstream #1242 (6a36efbe).
        boolean childrenMarginsAdjoinOwnEdgesStart =
            verticalMarginsAreCollapsible.start &&
            !establishesNewBlockFormattingContext &&
            !style.getOverflow().x.isScrollContainer() &&
            !style.getOverflow().y.isScrollContainer() &&
            !style.getPosition().isOutOfFlow() &&
            padding.top == 0 &&
            border.top == 0;

        boolean childrenMarginsAdjoinOwnEdgesEnd =
            verticalMarginsAreCollapsible.end &&
            !establishesNewBlockFormattingContext &&
            !style.getOverflow().x.isScrollContainer() &&
            !style.getOverflow().y.isScrollContainer() &&
            !style.getPosition().isOutOfFlow() &&
            padding.bottom == 0 &&
            border.bottom == 0;

        TaffyLine<Boolean> childrenMarginsAdjoinOwnEdges =
            new TaffyLine<>(childrenMarginsAdjoinOwnEdgesStart, childrenMarginsAdjoinOwnEdgesEnd);

        TaffyLine<Boolean> ownMarginsCollapseWithChildren = new TaffyLine<>(
            childrenMarginsAdjoinOwnEdgesStart,
            childrenMarginsAdjoinOwnEdgesEnd && Float.isNaN(sizeStyle.height)
        );

        // `!style.isBlock()` is the "does this box participate in its parent's block formatting
        // context?" test, not "is this block layout?": flow-root is laid out by this class but is
        // NOT part of the parent's BFC, so its own margins never collapse through it (upstream
        // #997 keeps is_block false for flow-root for exactly this reason). BK-17.
        boolean hasStylesPreventingBeingCollapsedThrough =
            !style.isBlock() ||
            style.getOverflow().x.isScrollContainer() ||
            style.getOverflow().y.isScrollContainer() ||
            style.getPosition().isOutOfFlow() ||
            (!Float.isNaN(style.getAspectRatio())) ||
            padding.top > 0 ||
            padding.bottom > 0 ||
            border.top > 0 ||
            border.bottom > 0;

        FloatSize clampedStyleSize = inputs.sizingMode() == SizingMode.INHERENT_SIZE
                                     ? maybeClamp(sizeStyle, minSize, maxSize)
                                     : new FloatSize(NaN, NaN);

        // If both min and max are set and max <= min, use min
        FloatSize minMaxDefiniteSize = new FloatSize(
            (!Float.isNaN(minSize.width) && !Float.isNaN(maxSize.width) && maxSize.width <= minSize.width)
            ? minSize.width : NaN,
            (!Float.isNaN(minSize.height) && !Float.isNaN(maxSize.height) && maxSize.height <= minSize.height)
            ? minSize.height : NaN
        );

        FloatSize styledBasedKnownDimensions = orChain(
            knownDimensions,
            minMaxDefiniteSize,
            clampedStyleSize
        );
        styledBasedKnownDimensions = maybeMax(styledBasedKnownDimensions, paddingBorderSize);

        // Short-circuit if size is known and we're only computing size
        if (runMode == RunMode.COMPUTE_SIZE &&
            !Float.isNaN(styledBasedKnownDimensions.width) &&
            !Float.isNaN(styledBasedKnownDimensions.height)) {
            return LayoutOutput.fromOuterSize(styledBasedKnownDimensions);
        }

        // css-sizing-4 automatic minimum size: an aspect-ratio box is floored in an axis by its
        // content-based minimum size (its min-content contribution) when its min-size is auto,
        // and the floor is zero for a scroll container (css-sizing-3 "overflow: non-visible" rule;
        // WPT block-aspect-ratio-010..012, 018). The generated style model cannot distinguish
        // "min-width: auto" from the 0px a block box computes to, so a zero min-size on an
        // aspect-ratio box is treated as auto, which is the used value Chrome computes.
        boolean aspectRatioAutoMinimumSize =
            !Float.isNaN(aspectRatio)
            && !overflow.x.isScrollContainer()
            && !overflow.y.isScrollContainer();

        // css-sizing-4: a definite size in one axis transfers through `aspect-ratio` to make the
        // other definite. This must be resolved *before* the content box size and the item list
        // are derived, otherwise the children are laid out against an unknown height (so their
        // percentage heights resolve to nothing and a descendant grid's auto rows collapse) and
        // the container's own size is only corrected afterwards.
        //
        // Upstream #965 (8609e92f) applies this transfer to the incoming known dimensions with no
        // RunMode special case: it assumes a parent fills an axis only for a real constraint. This
        // port deliberately keeps the `RunMode.PERFORM_LAYOUT` gate as a documented deviation
        // because BL-01/FL-15 (`knownDimensionsAreDefinite`, upstream #1003) is not ported yet:
        // Java's flex measurement hands a definite cross size to the block child during
        // COMPUTE_SIZE, and an ungated transfer then derives the main size (50 for a 100px cross
        // size at ratio 2) instead of leaving the content-based size the browser's automatic
        // minimum needs. Without the gate 14 generated WPT fixtures regress (flex_aspect_ratio
        // 002/004/038/049/050/051/052, border-box and content-box each). The gate can be removed
        // once FL-15/BL-01 can distinguish a definite known size from a measurement probe; the
        // gated-height-only test in BlockResidualP8Test pins the current split.
        //
        // Within a final layout only a newly-filled axis is adopted; a known axis is left exactly
        // as the parent resolved it (re-clamping it could undo padding/border overrides). The
        // ratio applies to the content box: convert the known border-box axis into the content
        // box before transferring, convert the derived axis back, then clamp it by min/max and
        // floor it at the padding+border sum (like the intrinsic-height path below).
        // BK-15(a).
        // Which axis (if any) this box's used size was newly derived for through the ratio. A
        // derived axis is not a specified size: the automatic minimum size above can still floor
        // it (inline) or grow it (block), and a transferred axis must not be clamped by a
        // transferred constraint.
        boolean widthDerivedByAspectRatio = false;
        boolean heightDerivedByAspectRatio = false;
        if (runMode == RunMode.PERFORM_LAYOUT && Resolve.isTransferableAspectRatio(aspectRatio)) {
            FloatSize derived = styledBasedKnownDimensions;
            if (!Float.isNaN(derived.width) && Float.isNaN(derived.height)) {
                float arDerivedHeight = (derived.width - boxSizingAdjustment.width) / aspectRatio
                                        + boxSizingAdjustment.height;
                derived = new FloatSize(derived.width, arDerivedHeight);
                heightDerivedByAspectRatio = true;
            } else if (Float.isNaN(derived.width) && !Float.isNaN(derived.height)) {
                float arDerivedWidth = (derived.height - boxSizingAdjustment.height) * aspectRatio
                                       + boxSizingAdjustment.width;
                derived = new FloatSize(arDerivedWidth, derived.height);
                widthDerivedByAspectRatio = true;
            }
            derived = maybeMax(maybeClamp(derived, minSize, maxSize), paddingBorderSize);
            styledBasedKnownDimensions = new FloatSize(
                !Float.isNaN(styledBasedKnownDimensions.width) ? styledBasedKnownDimensions.width : derived.width,
                !Float.isNaN(styledBasedKnownDimensions.height) ? styledBasedKnownDimensions.height : derived.height
            );
        }

        // Compute container content box size (inner size for resolving child percentages)
        float contentBoxInsetWidth = padding.left + padding.right + border.left + border.right
                                     + scrollbarGutter.left + scrollbarGutter.right;
        float contentBoxInsetHeight = padding.top + padding.bottom + border.top + border.bottom
                                      + scrollbarGutter.top + scrollbarGutter.bottom;
        FloatSize containerContentBoxSize = new FloatSize(
            !Float.isNaN(styledBasedKnownDimensions.width) ? styledBasedKnownDimensions.width - contentBoxInsetWidth : NaN,
            !Float.isNaN(styledBasedKnownDimensions.height) ? styledBasedKnownDimensions.height - contentBoxInsetHeight : NaN
        );

        // The container's own resolved direction. A child inherits it unless the child's style
        // sets a direction of its own, so the item list needs this value to resolve the (default)
        // inherited case without walking the parent chain once per child.
        TaffyDirection direction = layoutComputer.resolveDirection(node);

        // Generate item list
        List<BlockItem> items = generateItemList(node, containerContentBoxSize, direction);

        // Compute container width
        float containerOuterWidth = styledBasedKnownDimensions.width;
        if (Float.isNaN(containerOuterWidth)) {
            // Content box inset includes scrollbar gutter (matches Rust's content_box_inset)
            float contentBoxInsetH = padding.left + padding.right + border.left + border.right
                                     + scrollbarGutter.left + scrollbarGutter.right;
            AvailableSpace availableWidth = subtractFromAvailable(
                availableSpace.width, contentBoxInsetH);
            float intrinsicWidth = determineContentBasedContainerWidth(items, availableWidth)
                                   + contentBoxInsetH;
            containerOuterWidth = TaffyMath.clamp(intrinsicWidth, minSize.width, maxSize.width);
            containerOuterWidth = Math.max(containerOuterWidth, !Float.isNaN(paddingBorderSize.width) ? paddingBorderSize.width : 0);
        }

        // css-sizing-4 automatic minimum size in the inline axis: when the inline size was
        // transferred from a definite block size, the box is still floored by its content-based
        // minimum size (its min-content contribution), unless its own min-width overrides that.
        // Block-aspect-ratio-015/056/057: a `height: 100px; aspect-ratio: 1/2` box holding a
        // 100px-wide child is 100px wide, not the transferred 50px.
        // The automatic minimum only applies when min-width does not specify a larger minimum; a
        // content-box adjustment makes a zero specified min-width resolve to the padding+border
        // sum, so compare against that floor rather than against zero.
        if (widthDerivedByAspectRatio
            && aspectRatioAutoMinimumSize
            && (Float.isNaN(minSize.width) || minSize.width <= paddingBorderSize.width)) {
            float minContentWidth = determineContentBasedContainerWidth(items, AvailableSpace.minContent())
                                    + contentBoxInsetWidth;
            containerOuterWidth = Math.max(containerOuterWidth, minContentWidth);
            containerOuterWidth = Math.max(
                TaffyMath.clamp(containerOuterWidth, minSize.width, maxSize.width),
                paddingBorderSize.width);
        }

        // Short-circuit if computing size and both dimensions known
        if (runMode == RunMode.COMPUTE_SIZE && !Float.isNaN(styledBasedKnownDimensions.height)) {
            return LayoutOutput.fromOuterSize(new FloatSize(containerOuterWidth, styledBasedKnownDimensions.height));
        }

        // The percentage resolution height is this container's height basis for its children:
        // their percentage heights and percentage vertical insets only resolve when it is
        // definite. Per CSS 2 §10.5 an indefinite height makes those percentages behave as
        // `auto`, so `min-height` alone is not a basis — when the style height is auto the
        // answer stays "no basis" even if min-height is set (upstream #1122 removed the
        // unconditional `.or(min_height)` fallback; `maybeMax` has the same Option semantics:
        // (None, Some) => None). Under ContentSize the container's own height/min-height are
        // ignored, so they cannot be a percentage basis either.
        // BK-08 / upstream #1122 (f1a03062).
        float containerPercentageResolutionHeight;
        if (!Float.isNaN(knownDimensions.height)) {
            // A height resolved by the parent (e.g. a definitively sized containing block) is
            // the basis, matching upstream's known_dimensions_are_definite gate.
            containerPercentageResolutionHeight = knownDimensions.height;
        } else if (inputs.sizingMode() == SizingMode.INHERENT_SIZE) {
            containerPercentageResolutionHeight = TaffyMath.maybeMax(sizeStyle.height, minSize.height);
        } else {
            containerPercentageResolutionHeight = NaN;
        }

        // Perform final layout on children
        //
        // Percentage padding and borders resolve against the *containing block's* width
        // (parentSize.width), not the box's own width. The two only differ when the box has a
        // non-stretch width. Fall back to the box's own width when the parent size is unknown
        // (e.g. at the root of the layout tree). BK-06 / upstream #1083.
        float percentageResolutionWidth =
            !Float.isNaN(parentSize.width) ? parentSize.width : containerOuterWidth;
        FloatRect resolvedPadding = Resolve.resolveRectOrZero(style.getPadding(), percentageResolutionWidth);
        FloatRect resolvedBorder = Resolve.resolveRectOrZero(style.getBorder(), percentageResolutionWidth);
        FloatRect contentBoxInset = new FloatRect(
            resolvedPadding.left + resolvedBorder.left + scrollbarGutter.left,
            resolvedPadding.right + resolvedBorder.right + scrollbarGutter.right,
            resolvedPadding.top + resolvedBorder.top + scrollbarGutter.top,
            resolvedPadding.bottom + resolvedBorder.bottom + scrollbarGutter.bottom
        );

        // Select or create the block formatting context. A nested ordinary block reuses the
        // parent's BFC (so its floats are visible to later ancestors' siblings); flow-root and
        // scroll containers always start a fresh one. The root context's width is the BFC's
        // border-box width; content-box insets make float placement respect the root's padding.
        BlockContext blockContext;
        if (inheritedContext == null || establishesNewBlockFormattingContext) {
            blockContext = BlockContext.root(new FloatContext());
            blockContext.setWidth(containerOuterWidth);
            blockContext.applyContentBoxInset(contentBoxInset.left, contentBoxInset.right);
        } else {
            blockContext = inheritedContext;
            // The inherited context already carries this block's border-box position; add the
            // block's own padding/border so content-box queries use the right edges.
            blockContext.applyContentBoxInset(contentBoxInset.left, contentBoxInset.right);
        }

        InFlowLayoutResult layoutResult = performFinalLayoutOnChildren(
            items,
            runMode,
            containerOuterWidth,
            containerPercentageResolutionHeight,
            contentBoxInset,
            style.getTextAlign(),
            style.justifyItems,
            direction,
            ownMarginsCollapseWithChildren,
            childrenMarginsAdjoinOwnEdges,
            blockContext
        );

        float containerOuterHeight = styledBasedKnownDimensions.height;
        if (!Float.isNaN(containerOuterHeight)
            && heightDerivedByAspectRatio
            && aspectRatioAutoMinimumSize
            && (Float.isNaN(minSize.height) || minSize.height <= paddingBorderSize.height)) {
            // css-sizing-4 automatic minimum size in the block axis: a block size transferred
            // from a definite inline size is floored by the content height, so a box with
            // `width: 100px; aspect-ratio: 2/1` holding 100px of content is 100px tall rather
            // than the transferred 50px (WPT block-aspect-ratio-009, 042).
            float contentHeightGrown = Math.max(containerOuterHeight, layoutResult.contentHeight);
            contentHeightGrown = TaffyMath.clamp(contentHeightGrown, minSize.height, maxSize.height);
            containerOuterHeight = Math.max(
                contentHeightGrown, !Float.isNaN(paddingBorderSize.height) ? paddingBorderSize.height : 0);
        }
        if (Float.isNaN(containerOuterHeight)) {
            // This branch is only reached when the height is auto. In a final layout an
            // aspect-ratio derived height was already resolved above (so the height is definite
            // and this branch is skipped); during a measurement the aspect ratio is instead
            // folded into the content-based height, which is the legacy (content-inclusive)
            // measurement that flex/grid intrinsic sizing has been verified against.
            float contentHeight = layoutResult.contentHeight;
            // A BFC root contains its floats: the content height extends to the bottom margin
            // edge of any floated descendant, and the root's bottom padding and border sit below
            // that (upstream #1384, f6b87ac4). This runs before min/max clamping, matching the
            // upstream order.
            if (containsFloats && layoutResult.floatContentContribution > 0.0f) {
                contentHeight = Math.max(
                    contentHeight,
                    layoutResult.floatContentContribution + contentBoxInset.bottom
                );
            }
            if (!Float.isNaN(aspectRatio) && !Float.isNaN(containerOuterWidth)) {
                float arDerivedHeight = (containerOuterWidth - boxSizingAdjustment.width) / aspectRatio
                                        + boxSizingAdjustment.height;
                contentHeight = Math.max(contentHeight, arDerivedHeight);
            }
            containerOuterHeight = TaffyMath.clamp(contentHeight, minSize.height, maxSize.height);
            containerOuterHeight = Math.max(containerOuterHeight, !Float.isNaN(paddingBorderSize.height) ? paddingBorderSize.height : 0);
        }

        FloatSize finalOuterSize = new FloatSize(containerOuterWidth, containerOuterHeight);

        // CSS2 §8.3.1: the bottom margin of a block with `height: auto` collapses with its last
        // in-flow child's bottom margin only if the box's `min-height` is less than the box's
        // used height. When `min-height` determines the used height, the last child's bottom
        // margin no longer adjoins the box's bottom edge, so it stays inside the box instead of
        // collapsing with the box's own bottom margin. (`max-height` has no such effect.)
        // BK-05 / upstream #1082.
        boolean heightConstrainedByMinHeight =
            !Float.isNaN(minSize.height) && minSize.height > 0f && minSize.height >= containerOuterHeight;
        boolean ownBottomMarginCollapsesWithChildren =
            ownMarginsCollapseWithChildren.end && !heightConstrainedByMinHeight;

        // CSS Box Alignment: `align-content` distributes the block-axis in-flow content of a
        // block container within its content box. The whole in-flow stack is a *single*
        // alignment subject, so the distribution keywords (`space-*`, `stretch`) use their
        // single-subject fallbacks and the group is translated by one offset — there is no
        // inter-child gap. Child layouts were deferred into `item.finalLayout` (BK-01) so this
        // shift can happen before they are committed to the tree; out-of-flow children's static
        // positions move with the in-flow content (they are only consumed when that axis has no
        // inset), and the container's first baseline is offset with the content it was measured
        // from (upstream #996).
        //
        // BK-14 / upstream #959 (dcdb0ede); static-position shift from #1206 (a4dff69e) and
        // baseline shift from #996 (b9bfa1d1).
        float firstBaseline = layoutResult.firstBaseline;
        AlignContent alignContent = style.getAlignContent();
        // AUTO is the legacy "not specified" sentinel and keeps the historical no-op; an explicit
        // NORMAL is a first-class value and resolves to START in block layout (upstream #1255).
        if (!AlignmentHelper.isUnset(alignContent)) {
            float resolvedContentBoxInsetHeight = contentBoxInset.top + contentBoxInset.bottom;
            float containerInnerHeight = containerOuterHeight - resolvedContentBoxInsetHeight;
            float inflowContentHeight = layoutResult.contentHeight - resolvedContentBoxInsetHeight;
            float freeSpace = containerInnerHeight - inflowContentHeight;
            float groupOffset = blockAlignContentOffset(alignContent, freeSpace);

            if (groupOffset != 0f) {
                // Index access: the item list is an ArrayList, so the hot loops avoid an
                // iterator allocation and its per-element concurrent-modification check (P18).
                for (int itemIndex = 0, itemCount = items.size(); itemIndex < itemCount; itemIndex++) {
                    BlockItem item = items.get(itemIndex);
                    if (item.finalLayout != null) {
                        Layout shifted = item.finalLayout.copy();
                        shifted.location().y += groupOffset;
                        item.finalLayout = shifted;
                    }
                    if (item.position.isOutOfFlow()) {
                        item.staticPosition.y += groupOffset;
                    }
                }
                if (!Float.isNaN(firstBaseline)) {
                    firstBaseline += groupOffset;
                }
            }
        }

        // Determine whether this node can be collapsed through.
        //
        // CSS2 §8.3.1: a box's top and bottom margins collapse through it only if its *used*
        // height is zero. `height`/`min-height`/`max-height` are therefore judged by the height
        // they resolve to — `height: 1px; max-height: 0` collapses, `min-height: 1%` under an
        // auto-height parent collapses — rather than by their computed values, and a zero used
        // height with block-start/end border or padding still does not collapse (those are in
        // hasStylesPreventingBeingCollapsedThrough).
        // BK-09 / upstream #1188 (c17b313c).
        boolean canBeCollapsedThrough = !hasStylesPreventingBeingCollapsedThrough &&
                                        finalOuterSize.height == 0 &&
                                        layoutResult.allChildrenCanBeCollapsedThrough;

        // Compute output margin sets
        CollapsibleMarginSet topMargin;
        if (ownMarginsCollapseWithChildren.start) {
            topMargin = layoutResult.firstChildTopMarginSet;
        } else {
            float marginTop = Resolve.resolveLpaOrZero(style.getMargin().top, parentSize.width);
            topMargin = CollapsibleMarginSet.fromMargin(marginTop);
        }

        CollapsibleMarginSet bottomMargin;
        if (ownBottomMarginCollapsesWithChildren) {
            bottomMargin = layoutResult.lastChildBottomMarginSet;
        } else {
            float marginBottom = Resolve.resolveLpaOrZero(style.getMargin().bottom, parentSize.width);
            bottomMargin = CollapsibleMarginSet.fromMargin(marginBottom);
        }

        // Short-circuit if computing size.
        //
        // The margin-collapse related outputs are part of the result: a parent block container
        // relies on its children's top/bottom margin sets to compute its own intrinsic height, and
        // the collapse-through flag drives its own collapsing. Content size is not known yet (and
        // is only needed by a full layout), so it stays ZERO.
        // BK-03 / upstream #976 (b9067e74).
        if (runMode == RunMode.COMPUTE_SIZE) {
            return new LayoutOutput(
                finalOuterSize,
                FloatSize.zero(),
                new FloatPoint(NaN, NaN),
                topMargin,
                bottomMargin,
                canBeCollapsedThrough
            );
        }

        // Commit the deferred in-flow layouts. This must stay *after* the ComputeSize return
        // above: measuring a node must never write layout state to the tree.
        // Upstream #971 (BK-01).
        // Index access: see the note on the align-content shift loop above (P18).
        for (int itemIndex = 0, itemCount = items.size(); itemIndex < itemCount; itemIndex++) {
            BlockItem item = items.get(itemIndex);
            if (item.finalLayout != null) {
                tree.setUnroundedLayout(item.nodeId, item.finalLayout);
            }
        }

        // === Scrollable overflow geometry (P4/SO-01/SO-02) ===
        //
        // Every scrollable-overflow rectangle is expressed in the node's *client-area* frame:
        // the border box less its border and scrollbar gutters, i.e. the padding box without the
        // gutters the scrollbars reserve (upstream #1118/#1372). It is never mirrored for RTL: the
        // reversed scroll origin is carried by the {@link ScrollableOverflow} flags (upstream
        // #1372's "accumulate scrollable overflow in physical coordinates").
        boolean rtl = direction.isRtl();
        boolean scrollContainer = ScrollableOverflowUtil.isScrollContainer(overflow);
        FloatSize clientSize = new FloatSize(
            Math.max(0f, finalOuterSize.width - resolvedBorder.horizontalAxisSum()
                           - scrollbarGutter.horizontalAxisSum()),
            Math.max(0f, finalOuterSize.height - resolvedBorder.verticalAxisSum()
                           - scrollbarGutter.verticalAxisSum())
        );

        // Layout absolutely positioned children
        // Absolute position inset is border + scrollbar gutter (not padding), per CSS spec
        FloatRect absolutePositionInset = new FloatRect(
            resolvedBorder.left + scrollbarGutter.left,
            resolvedBorder.right + scrollbarGutter.right,
            resolvedBorder.top + scrollbarGutter.top,
            resolvedBorder.bottom + scrollbarGutter.bottom
        );
        // Out-of-flow children contribute against their own positioning area, which is the client
        // area itself, so the container description carries no padding of its own (upstream's oof
        // pass describes its container with a zero padding rectangle and a zero margin).
        // SO-01 / upstream #1118.
        OutOfFlowOverflow outOfFlowOverflow = performAbsoluteLayoutOnChildren(
            items,
            finalOuterSize,
            absolutePositionInset,
            overflow,
            new ScrollableOverflowUtil.Container(scrollContainer, clientSize, rtl, false, FloatRect.zero())
        );

        // Layout hidden children. Hidden children are filtered out of `items` by
        // generateItemList, so walk the actual child list (in tree order) to find them.
        // Only display:none children are laid out here: their stale unrounded/final layout
        // from an earlier visible pass must be reset to a zeroed layout.
        // performChildLayout is used (instead of computeChildLayout with LayoutInput.hidden):
        // on a NONE child it dispatches to computeHiddenLayout with the same semantics, but it
        // also stores the result in the layout cache. The HIDDEN shortcut skips that store, and
        // an empty cache makes TaffyTree.markDirty stop propagating to ancestors, so a later
        // display restore would not trigger a relayout of the child.
        List<NodeId> children = tree.getChildren(node);
        for (int order = 0; order < children.size(); order++) {
            NodeId childId = children.get(order);
            TaffyStyle childStyle = tree.getStyle(childId);
            if (childStyle.getBoxGenerationMode() == BoxGenerationMode.NONE) {
                tree.setUnroundedLayout(childId, Layout.withOrder(order));
                layoutComputer.performChildLayout(
                    childId,
                    FloatSize.none(),
                    FloatSize.none(),
                    TaffySize.maxContent(),
                    SizingMode.INHERENT_SIZE,
                    TaffyLine.FALSE
                );
            }
        }

        // === Accumulate the in-flow children's scrollable overflow (P4/SO-01/SO-02) ===
        //
        // This runs after the `align-content` shift and after the out-of-flow pass, so every
        // contribution uses the child's final location. Upstream #1372 recomputes the accumulator
        // from scratch after an `align-content` shift for exactly that reason; accumulating once,
        // here, is equivalent and keeps a single accumulation point.
        //
        // A child's own scrollable overflow rectangle is relative to its client area, so the
        // child's border is added inside `computeContribution`; `location` below is the child's
        // border-box origin in this node's client-area frame (hence the border and gutter
        // subtraction). Block layout passes only the non-positive part of the child's resolved
        // margin, which is what lets a negative end margin cancel the container's end padding.
        ScrollableOverflowUtil.Container inFlowOverflowContainer = new ScrollableOverflowUtil.Container(
            scrollContainer, clientSize, rtl, false, resolvedPadding);

        FloatRect accumulatedOverflow = ScrollableOverflowUtil.union(
            FloatRect.zero(), outOfFlowOverflow.scrollableOverflow());

        // The legacy 1.2 content size is still reported by this record component, and is what
        // scrollWidth()/scrollHeight() consume until the P4 integration switches them to the
        // rectangle. It follows upstream's pre-#1118 block rules, but with its contributions
        // measured from the client area (#1007): the reachable content extent then excludes the
        // leading border, which is exactly the term the symmetric scroll-size formula adds back.
        // Upstream #1007. 2.0 removes the component entirely.
        // The running maximum is kept as two primitives: ContentSizeUtil.max would allocate one
        // FloatSize per in-flow child, and the legacy content size is only needed as a value at
        // the end of this loop. A NaN contribution counts as zero, exactly as ContentSizeUtil
        // sanitises it. P18.
        float inFlowContentWidth = 0f;
        float inFlowContentHeight = 0f;
        for (int itemIndex = 0, itemCount = items.size(); itemIndex < itemCount; itemIndex++) {
            BlockItem item = items.get(itemIndex);
            Layout childLayout = item.finalLayout;
            // Out-of-flow children were accumulated by the absolute pass above.
            if (childLayout == null) continue;

            FloatRect childMargin = childLayout.margin();
            // Block layout hands only the non-positive part of the child's margin to the overflow
            // contribution, and a box without negative margins contributes the shared zero
            // rectangle instead of a fresh one (the consumer only reads it). P18.
            float overflowMarginLeft = nonPositive(childMargin.left);
            float overflowMarginRight = nonPositive(childMargin.right);
            float overflowMarginTop = nonPositive(childMargin.top);
            float overflowMarginBottom = nonPositive(childMargin.bottom);
            accumulatedOverflow = ScrollableOverflowUtil.union(
                accumulatedOverflow,
                ScrollableOverflowUtil.computeContribution(
                    new FloatPoint(
                        childLayout.location().x - resolvedBorder.left - scrollbarGutter.left,
                        childLayout.location().y - resolvedBorder.top - scrollbarGutter.top
                    ),
                    childLayout.size(),
                    childLayout.scrollableOverflow().rect(),
                    childLayout.border(),
                    item.overflow,
                    inFlowOverflowContainer,
                    overflowMarginLeft == 0f && overflowMarginRight == 0f
                        && overflowMarginTop == 0f && overflowMarginBottom == 0f
                    ? ZERO_RECT
                    : new FloatRect(
                        overflowMarginLeft, overflowMarginRight,
                        overflowMarginTop, overflowMarginBottom)
                )
            );

            FloatSize contribution = contentSizeContribution(
                childLayout, item.overflow, rtl, containerOuterWidth, resolvedBorder, scrollbarGutter);
            float contributionWidth = Float.isNaN(contribution.width) ? 0f : contribution.width;
            float contributionHeight = Float.isNaN(contribution.height) ? 0f : contribution.height;
            if (contributionWidth > inFlowContentWidth) inFlowContentWidth = contributionWidth;
            if (contributionHeight > inFlowContentHeight) inFlowContentHeight = contributionHeight;
        }
        FloatSize inFlowContentSize = new FloatSize(inFlowContentWidth, inFlowContentHeight);

        // A scroll container's own end padding is part of its reachable region: the start padding
        // is already covered by the child positions, the end padding is not. A non-scroll box does
        // not extend its overflow region with padding that comes after its content.
        // SO-02 / upstream #1051 and #1114.
        if (scrollContainer) {
            inFlowContentSize = new FloatSize(
                inFlowContentSize.width + (rtl ? resolvedPadding.left : resolvedPadding.right),
                inFlowContentSize.height + resolvedPadding.bottom
            );
        }
        FloatSize contentSize = ContentSizeUtil.max(inFlowContentSize, outOfFlowOverflow.contentSize());

        // The container's own extent is part of its scrollable overflow region too: in RTL the
        // client area's right edge is the scroll origin, and a scroll container's own end padding
        // plus its (otherwise empty) content area are reachable. An empty scroll container must
        // therefore report its padding, not a zero rectangle.
        // SO-02 / upstream #1051, #1114 and #1372.
        if (rtl) {
            accumulatedOverflow = ScrollableOverflowUtil.union(
                accumulatedOverflow, new FloatRect(0f, clientSize.width, 0f, 0f));
        }
        if (scrollContainer) {
            accumulatedOverflow = ScrollableOverflowUtil.union(accumulatedOverflow, new FloatRect(
                rtl ? Math.min(clientSize.width - resolvedPadding.left, 0f) : 0f,
                rtl ? clientSize.width : resolvedPadding.right,
                0f,
                resolvedPadding.bottom
            ));
        }
        // Only a scroll container clips the unreachable region and keeps its reversed origins; a
        // non-scroll box keeps all of its overflow for propagation and hit testing.
        ScrollableOverflow scrollableOverflow = ScrollableOverflowUtil.finalizeScrollableOverflow(
            accumulatedOverflow, clientSize, rtl, false, scrollContainer);

        // Layout containment suppresses the box's baseline for baseline-alignment purposes: the
        // box is treated as having no baseline, so a baseline-aligning parent (or any baseline-
        // consuming ancestor) synthesizes one from its border box instead of using its content's.
        // Upstream #1128 (Contain::suppresses_baseline). Paint containment does NOT suppress it.
        if (contain.suppressesBaseline()) {
            firstBaseline = NaN;
        }

        // The final layout output carries the container's first baseline. The ComputeSize returns
        // above deliberately stay baseline-less: upstream returns LayoutOutput::from_outer_size
        // there too (block.rs:482/488/651/656), and only a full layout produces baselines.
        // Upstream #996.
        FloatPoint firstBaselines = new FloatPoint(NaN, firstBaseline);
        return new LayoutOutput(
            finalOuterSize,
            contentSize,
            scrollableOverflow,
            firstBaselines,
            Baselines.fromFirst(firstBaselines),
            topMargin,
            bottomMargin,
            canBeCollapsedThrough
        );
    }

    /**
     * The legacy 1.2 content-size contribution of one laid-out in-flow child.
     *
     * <p>The contribution is measured from the container's client area: in LTR the child's
     * border-box origin minus the container's leading border and scrollbar gutter, in RTL
     * mirrored from the container's right border edge (upstream's pre-#1118 block rule). The
     * start-point correction is the block half of upstream #1007: {@code scrollWidth()}
     * re-adds both borders, so a contribution measured from the border box counted the leading
     * border twice and left the scroll range one border-width short.</p>
     *
     * <p>This is the legacy component only; {@link Layout#scrollableOverflow()} is the model that
     * replaces it (SO-01). The reversed RTL scroll origin is not expressible through a
     * {@link FloatSize}, which is why the RTL value stays an approximation until the scroll-size
     * getters switch to the rectangle (P4 integration).</p>
     */
    private static FloatSize contentSizeContribution(
        Layout childLayout,
        TaffyPoint<Overflow> childOverflow,
        boolean rtl,
        float containerOuterWidth,
        FloatRect border,
        FloatRect gutter
    ) {
        FloatPoint location = childLayout.location();
        FloatSize childSize = childLayout.size();
        FloatPoint contributionLocation = rtl
            ? new FloatPoint(
                containerOuterWidth - (location.x + childSize.width) - border.right,
                location.y - border.top - gutter.top
            )
            : new FloatPoint(
                location.x - border.left - gutter.left,
                location.y - border.top - gutter.top
            );
        FloatSize childContentSize = childLayout.contentSize() != null
                                    ? childLayout.contentSize()
                                    : childSize;
        return ContentSizeUtil.computeContentSizeContribution(
            contributionLocation, childSize, childContentSize, childOverflow);
    }

    /**
     * The non-positive part of one resolved margin component, or zero for NaN.
     *
     * <p>Block layout hands only {@code margin.min(0)} to the scrollable-overflow contribution
     * (upstream #1372): a positive end margin must not extend the reachable region, while a
     * negative end margin cancels the container's end padding extension.</p>
     */
    private static float nonPositive(float margin) {
        return Float.isNaN(margin) ? 0f : Math.min(0f, margin);
    }

    /**
     * The scrollable-overflow rectangle and the legacy content size contributed by a container's
     * out-of-flow children (P4/SO-01/SO-02).
     *
     * @param scrollableOverflow the union of the out-of-flow contributions, in the container's
     *                           client-area frame
     * @param contentSize        the legacy content-size extent of the same children
     */
    private record OutOfFlowOverflow(FloatRect scrollableOverflow, FloatSize contentSize) {}

    private List<BlockItem> generateItemList(
        NodeId node, FloatSize nodeInnerSize, TaffyDirection containerDirection) {
        TaffyTree tree = layoutComputer.getTree();
        ItemListCache cache = itemListCache(tree);
        CachedItemList cached = cache.lists.get(node.getId());
        if (cached != null && cached.matches(nodeInnerSize, containerDirection)) {
            resetPerPassState(cached.items);
            return cached.items;
        }

        List<NodeId> children = tree.getChildren(node);
        NodeData containerData = tree.getNodeData(node);
        if (containerData != null) {
            BlockItemCache persistent = containerData.getBlockItemCache();
            if (persistent != null
                && persistent.matches(containerData, children, tree, nodeInnerSize, containerDirection)) {
                // A list built by an earlier pass whose child styles, child list, direction and
                // inner-size basis are all unchanged. Its style-derived metadata is still current;
                // only the per-pass layout results have to be cleared.
                List<BlockItem> reused = persistent.items;
                resetPerPassState(reused);
                cache.store(node.getId(), reused, nodeInnerSize, containerDirection,
                    persistent.innerSizeIndependent);
                return reused;
            }
        }

        // Whether the metadata resolved below depends on the container's inner size. When it does
        // not, the list is valid for every inner size of this pass (a measurement at one known
        // size and the final layout at another), which is the common case.
        boolean innerSizeIndependent = true;
        List<BlockItem> items = new ArrayList<>(children.size());
        long[] childVersions = new long[children.size()];

        int order = 0;
        for (int childIndex = 0; childIndex < children.size(); childIndex++) {
            NodeId childId = children.get(childIndex);
            NodeData childData = tree.getNodeData(childId);
            childVersions[childIndex] = childData == null ? 0L : childData.getContentVersion();
            TaffyStyle childStyle = tree.getStyle(childId);
            if (childStyle.getBoxGenerationMode() == BoxGenerationMode.NONE) {
                order++;
                continue;
            }

            if (innerSizeIndependent && hasInnerSizeDependentValues(childStyle)) {
                innerSizeIndependent = false;
            }

            BlockItem item = new BlockItem();
            item.nodeId = childId;
            item.order = order++;

            Float aspectRatio = childStyle.getAspectRatio();
            TaffySize<TaffyDimension> childSize = childStyle.getSize();
            FloatRect itemPadding = resolveZeroOrRect(childStyle.getPadding(), nodeInnerSize.width);
            FloatRect itemBorder = resolveZeroOrRect(childStyle.getBorder(), nodeInnerSize.width);
            item.padding = itemPadding;
            item.border = itemBorder;
            item.paddingBorderSum = paddingBorderSum(itemPadding, itemBorder);

            FloatSize boxSizingAdj = childStyle.getBoxSizing() == BoxSizing.CONTENT_BOX
                                     ? item.paddingBorderSum
                                     : ZERO_SIZE;

            FloatSize size2 = resolveSizeOrNone(childSize, nodeInnerSize);
            // The item's min/max constraints are not transferred through its aspect ratio; see
            // the note in compute() (only the used size transfers between axes).
            FloatSize size1 = resolveSizeOrNone(childStyle.getMinSize(), nodeInnerSize);
            item.minSize = addBoxSizingAdjustment(size1, boxSizingAdj);
            FloatSize size = resolveSizeOrNone(childStyle.getMaxSize(), nodeInnerSize);
            item.maxSize = addBoxSizingAdjustment(size, boxSizingAdj);
            // Clamp the item's specified size in its own axis by its own min/max first, then
            // transfer the remaining definite axis through the ratio (css-sizing-4). Deriving
            // before clamping would transfer the unclamped size of the other axis.
            FloatSize clampedSize = clampIfConstrained(size2, size1, size);
            item.size = addBoxSizingAdjustment(
                Resolve.maybeApplyAspectRatio(clampedSize, aspectRatio), boxSizingAdj);
            item.widthFromAspectRatio =
                Float.isNaN(clampedSize.width) && !Float.isNaN(item.size.width);
            item.heightFromAspectRatio =
                Float.isNaN(clampedSize.height) && !Float.isNaN(item.size.height);

            item.overflow = childStyle.getOverflow();
            item.scrollbarWidth = childStyle.getScrollbarWidth();
            item.position = childStyle.getPosition();
            item.inset = childStyle.getInset();
            TaffyRect<LengthPercentageAuto> itemMargin = childStyle.getMargin();
            item.margin = itemMargin;
            // Classify the four margin sides once, here, instead of resolving them on every layout
            // pass: only a percent or calc() component needs the containing block's width (a
            // container-specific basis), while a length is its own value and auto/intrinsic
            // components resolve to nothing. The layout-time resolution in
            // performFinalLayoutOnChildren then reads the primitives alone whenever no side needs
            // the basis. P18.
            item.marginLeftNeedsInnerWidth = marginNeedsInnerWidth(itemMargin.left);
            item.marginRightNeedsInnerWidth = marginNeedsInnerWidth(itemMargin.right);
            item.marginTopNeedsInnerWidth = marginNeedsInnerWidth(itemMargin.top);
            item.marginBottomNeedsInnerWidth = marginNeedsInnerWidth(itemMargin.bottom);
            item.marginLeft = item.marginLeftNeedsInnerWidth ? NaN : staticMarginValue(itemMargin.left);
            item.marginRight = item.marginRightNeedsInnerWidth ? NaN : staticMarginValue(itemMargin.right);
            item.marginTop = item.marginTopNeedsInnerWidth ? NaN : staticMarginValue(itemMargin.top);
            item.marginBottom = item.marginBottomNeedsInnerWidth ? NaN : staticMarginValue(itemMargin.bottom);
            item.marginLeftIsAuto = itemMargin.left.isAuto();
            item.marginRightIsAuto = itemMargin.right.isAuto();
            item.marginsPreResolved = !item.marginLeftNeedsInnerWidth
                                      && !item.marginRightNeedsInnerWidth
                                      && !item.marginTopNeedsInnerWidth
                                      && !item.marginBottomNeedsInnerWidth;
            item.heightStyle = childSize.height;
            item.widthStyle = childSize.width;
            item.aspectRatio = aspectRatio;
            item.staticPosition = FloatPoint.ZERO;
            item.canBeCollapsedThrough = false;
            item.isTable = childStyle.getItemIsTable();
            item.isCompressibleReplaced = childStyle.isCompressibleReplaced();
            item.justifySelf = childStyle.justifySelf;
            item.alignSelf = childStyle.getAlignSelf();
            item.floatStyle = childStyle.getFloat();
            item.display = childStyle.getDisplay();
            item.contain = childStyle.getContain();
            item.clear = childStyle.getClear();
            // Equivalent to {@code layoutComputer.resolveDirection(childId)}: an inherited
            // direction resolves to the nearest ancestor's explicit value, which is exactly the
            // container's own resolved direction, and the container's resolution already walked
            // up to the root when it was inherited as well. Only a child that names its own
            // direction bypasses that.
            TaffyDirection childDirection = childStyle.getDirection();
            item.direction = childDirection.isInherit() ? containerDirection : childDirection;
            item.widthIsAuto = childSize.width.isAuto();
            // For compressible replaced items a percentage width/max-width resolves against a
            // zero-width containing block in the min-content contribution, so the item contributes
            // at most this cap. Upstream #1401 (4aca4f49).
            if (item.isCompressibleReplaced) {
                FloatSize resolvedWidth = Resolve.maybeResolveSize(
                    childSize, new FloatSize(0f, NaN));
                FloatSize resolvedMaxWidth = Resolve.maybeResolveSize(
                    childStyle.getMaxSize(), new FloatSize(0f, NaN));
                float width = resolvedWidth.width;
                float maxWidth = resolvedMaxWidth.width;
                float cap = Float.isNaN(width)
                            ? maxWidth
                            : (Float.isNaN(maxWidth) ? width : Math.min(width, maxWidth));
                item.minContentWidthCap = Float.isNaN(cap) ? NaN : cap + boxSizingAdj.width;
            }

            items.add(item);
        }

        cache.store(node.getId(), items, nodeInnerSize, containerDirection, innerSizeIndependent);
        if (containerData != null) {
            if (items.size() > 1) {
                containerData.setBlockItemCache(new BlockItemCache(
                    items,
                    containerData.getContentVersion(),
                    containerData.getChildrenVersion(),
                    childVersions,
                    containerDirection,
                    nodeInnerSize,
                    innerSizeIndependent
                ));
            } else {
                // Single-item containers are not worth a per-node long-lived list; drop any stale
                // one left by an earlier, larger child list.
                containerData.clearBlockItemCache();
            }
        }
        return items;
    }

    /**
     * Clears the per-pass results a layout pass fills into an item list before the list is handed
     * out again. The deferred child layout, the out-of-flow static position and its alignment
     * fields, and the collapse-through flag are results of {@link #performFinalLayoutOnChildren},
     * not item metadata, so a list reused by a later call must arrive in the same state as a
     * freshly built one.
     */
    private static void resetPerPassState(List<BlockItem> items) {
        for (int i = 0, size = items.size(); i < size; i++) {
            BlockItem item = items.get(i);
            item.finalLayout = null;
            item.staticPosition = FloatPoint.ZERO;
            item.canBeCollapsedThrough = false;
            item.staticAlignX = null;
            item.staticAlignY = null;
            item.staticAreaStartX = 0f;
            item.staticAreaEndX = 0f;
        }
    }

    /**
     * Returns the pass-scoped item-list cache for {@code tree}, discarding the entries of a
     * previous pass (a different tree or a new direction epoch).
     */
    private static ItemListCache itemListCache(TaffyTree tree) {
        ItemListCache cache = ITEM_LIST_CACHE.get();
        long epoch = tree.currentDirectionEpoch();
        TaffyTree cachedTree = cache.tree == null ? null : cache.tree.get();
        if (cachedTree != tree || cache.epoch != epoch) {
            cache.lists.clear();
            cache.cachedItems = 0;
            cache.tree = new WeakReference<>(tree);
            cache.epoch = epoch;
        }
        return cache;
    }

    /**
     * Whether any child-style value this class resolves against the container's inner size can
     * actually depend on it.
     *
     * <p>{@link #generateItemList} resolves the child's padding and border against the container's
     * content-box width and its size/min-size/max-size against the content-box size; a
     * {@code length} ignores that basis, whereas a percentage or a {@code calc()} expression (which
     * may embed one) does not. Style height/width keywords and {@code auto} resolve to
     * {@link Float#NaN} regardless of the basis, so they are basis-independent too. When this
     * method returns {@code false} the item list may be shared across the different known
     * dimensions a single pass lays the container out with.</p>
     */
    private static boolean hasInnerSizeDependentValues(TaffyStyle style) {
        return rectDependsOnInnerSize(style.getPadding())
               || rectDependsOnInnerSize(style.getBorder())
               || sizeDependsOnInnerSize(style.getSize())
               || sizeDependsOnInnerSize(style.getMinSize())
               || sizeDependsOnInnerSize(style.getMaxSize());
    }

    private static boolean rectDependsOnInnerSize(TaffyRect<LengthPercentage> rect) {
        return rect.left.isPercent() || rect.left.isCalc()
               || rect.right.isPercent() || rect.right.isCalc()
               || rect.top.isPercent() || rect.top.isCalc()
               || rect.bottom.isPercent() || rect.bottom.isCalc();
    }

    private static boolean sizeDependsOnInnerSize(TaffySize<TaffyDimension> size) {
        return size.width.isPercent() || size.width.isCalc()
               || size.height.isPercent() || size.height.isCalc();
    }

    private float determineContentBasedContainerWidth(List<BlockItem> items, AvailableSpace availableWidth) {
        float maxChildWidth = 0f;
        // Floats contribute to an auto-width block's intrinsic width through their side sums;
        // non-floated children keep the existing max-child-width path. FT-03 / upstream #1055.
        FloatContext.IntrinsicWidthCalculator floatCalculator =
            new FloatContext.IntrinsicWidthCalculator(availableWidth);

        for (int itemIndex = 0, itemCount = items.size(); itemIndex < itemCount; itemIndex++) {
            BlockItem item = items.get(itemIndex);
            if (item.position.isOutOfFlow()) continue;

            FloatSize knownDimensions = maybeClamp(item.size, item.minSize, item.maxSize);
            float marginSum = resolveInlineMarginSum(
                item, availableWidth.isDefinite() ? availableWidth.getValue() : NaN);

            // An inline size derived from the block size through a preferred aspect ratio is not a
            // specified width: the item still contributes its content, floored by the transferred
            // size (css-sizing-4's automatic minimum size). A 100px-tall, ratio-1/2 box holding a
            // 100px-wide child therefore contributes 100px, not the transferred 50px
            // (WPT block-aspect-ratio-056/057).
            float transferredWidth = NaN;
            if (!Float.isNaN(knownDimensions.width) && item.widthFromAspectRatio) {
                transferredWidth = knownDimensions.width;
                knownDimensions = new FloatSize(NaN, knownDimensions.height);
            }
            // A definite block-axis min-size transfers through the ratio to floor an auto inline
            // size (css-sizing-4's transferred size suggestion): a ratio-1/1 box whose min-height
            // resolves to 100px contributes at least 100px of inline size even though its content
            // is narrower (WPT block-aspect-ratio-024).
            if (item.widthIsAuto && !Float.isNaN(item.aspectRatio) && !Float.isNaN(item.minSize.height)) {
                float transferredMinWidth = item.minSize.height * item.aspectRatio;
                // NaN means "no transferred constraint yet", so it must not win a max().
                transferredWidth = Float.isNaN(transferredWidth)
                                   ? transferredMinWidth
                                   : Math.max(transferredWidth, transferredMinWidth);
            }
            boolean hasKnownWidth = !Float.isNaN(knownDimensions.width);

            float width;
            if (hasKnownWidth) {
                width = knownDimensions.width;
                if (!Float.isNaN(transferredWidth)) {
                    width = Math.max(width, transferredWidth);
                }
            } else {
                AvailableSpace adjustedAvailable = subtractFromAvailable(availableWidth, marginSum);

                // An item whose width is a sizing keyword is measured under the constraint the
                // keyword asks for (min-content / max-content / fit-content / stretch) instead of
                // under the containing block's own available width. Upstream taffy resolves these
                // keywords separately from the plain style size (#1099/#1103/#1247).
                AvailableSpace itemAvailable = adjustedAvailable;
                SizingKeyword.Resolution keyword = SizingKeyword.resolve(item.widthStyle, NaN, NaN);
                if (keyword != null) {
                    itemAvailable = keyword.isMeasure()
                        ? keyword.getMeasureAvailableSpace()
                        : AvailableSpace.definite(keyword.getExactValue());
                }

                // Measurement, not layout: upstream determines the content-based container
                // width with measure_child_size (upstream block.rs). Running a PERFORM_LAYOUT
                // probe here writes the child's (and its descendants') layouts with this probe's
                // parent size and available space instead of the real pass, and a later final
                // cache hit can leave those probe layouts behind (seed 6: n21 percentage height
                // resolved against the probe's height).
                FloatSize measuredSize = layoutComputer.measureChildSize(
                    item.nodeId,
                    knownDimensions,
                    new FloatSize(NaN, NaN),
                    new TaffySize<>(itemAvailable, AvailableSpace.minContent()),
                    SizingMode.INHERENT_SIZE,
                    TaffyLine.TRUE
                );
                float measuredWidth = measuredSize.width;
                // The cap is the item's min-content contribution, so its fit-content contribution
                // is min(max-content, max(cap, available)). Upstream #1401 (4aca4f49).
                float cap = item.minContentWidthCap;
                if (!Float.isNaN(cap)) {
                    if (adjustedAvailable.isMinContent()) {
                        measuredWidth = Math.min(measuredWidth, cap);
                    } else if (adjustedAvailable.isDefinite()) {
                        measuredWidth = Math.min(measuredWidth, Math.max(cap, adjustedAvailable.getValue()));
                    }
                    if (!Float.isNaN(item.minSize.width)) {
                        measuredWidth = Math.max(measuredWidth, item.minSize.width);
                    }
                }
                width = measuredWidth + marginSum;
                if (!Float.isNaN(transferredWidth)) {
                    width = Math.max(width, transferredWidth + marginSum);
                }
            }
            width = Math.max(width, item.paddingBorderSum.width);

            if (item.floatStyle.isFloated()) {
                // A measured width already includes the horizontal margins; a known width does not.
                float contributionWidth = width + (hasKnownWidth ? marginSum : 0.0f);
                FloatContext.Direction direction = item.floatStyle == TaffyFloat.LEFT
                                                    ? FloatContext.Direction.LEFT
                                                    : FloatContext.Direction.RIGHT;
                floatCalculator.addFloat(contributionWidth, direction, item.clear);
            } else {
                maxChildWidth = Math.max(maxChildWidth, width);
            }
        }

        return Math.max(maxChildWidth, floatCalculator.result());
    }

    private InFlowLayoutResult performFinalLayoutOnChildren(
        List<BlockItem> items,
        RunMode runMode,
        float containerOuterWidth,
        float containerPercentageResolutionHeight,
        FloatRect contentBoxInset,
        TextAlign textAlign,
        AlignItems justifyItems,
        TaffyDirection direction,
        TaffyLine<Boolean> ownMarginsCollapseWithChildren,
        TaffyLine<Boolean> childrenMarginsAdjoinOwnEdges,
        BlockContext blockContext) {

        TaffyTree tree = layoutComputer.getTree();
        // The containing block of an in-flow child is this container's *content box*: its
        // percentage widths resolve against the inner width, and its percentage heights and
        // vertical insets against the inner height. The height basis is only passed on when it
        // is definite; a percentage basis is a content box, so the resolved vertical content box
        // inset is subtracted from the border box basis. NaN (indefinite) stays NaN.
        // BK-08 / upstream #866 (25759dad).
        float containerInnerWidth = containerOuterWidth - contentBoxInset.left - contentBoxInset.right;
        float childPercentageResolutionHeight = TaffyMath.maybeSub(
            containerPercentageResolutionHeight, contentBoxInset.top + contentBoxInset.bottom);
        FloatSize parentSize = new FloatSize(containerInnerWidth, childPercentageResolutionHeight);
        // Vertical available space in block flow is indefinite, which this port represents as
        // MaxContent. Passing MinContent made every descendant grid believe it was being sized
        // under a min-content constraint, in which the track maximisation step has zero free
        // space, so auto rows containing only scroll-container items collapsed to zero height.
        // BK-15(b) / upstream #965.
        TaffySize<AvailableSpace> availableSpace = new TaffySize<>(
            AvailableSpace.definite(containerInnerWidth),
            AvailableSpace.maxContent()
        );

        // The available space handed to an in-flow item is the container's own, narrowed by the
        // item's horizontal margin sum. The TaffySize wrapper therefore only differs by
        // that sum, so it is rebuilt only when the sum changes -- in a container whose items share
        // their horizontal margins (the overwhelmingly common case) once per pass. Everything
        // that consumes it (the layout cache included) reads the value rather than retaining or
        // mutating the wrapper.
        TaffySize<AvailableSpace> itemAvailableSpace = null;
        float itemAvailableSpaceMarginSum = NaN;

        float committedYOffset = contentBoxInset.top;
        float yOffsetForAbsolute = contentBoxInset.top;
        CollapsibleMarginSet firstChildTopMarginSet = CollapsibleMarginSet.zero();
        CollapsibleMarginSet activeCollapsibleMarginSet = CollapsibleMarginSet.zero();
        boolean isCollapsingWithFirstMarginSet = true;
        boolean allChildrenCanBeCollapsedThrough = true;
        // The container's first baseline: the first baseline of its first in-flow child that has
        // one. NaN until such a child is laid out (upstream #996, refined by #1323).
        float firstBaseline = NaN;

        // Check RTL once at the start
        boolean isRtl = direction != null && direction.isRtl();

        // Index access on the ArrayList item list: this is the per-child hot loop of block
        // layout, so it avoids the iterator allocation and its per-element concurrent-modification
        // check. P18.
        for (int itemIndex = 0, itemCount = items.size(); itemIndex < itemCount; itemIndex++) {
            BlockItem item = items.get(itemIndex);
            if (item.position.isOutOfFlow()) {
                // The static-position rectangle of an out-of-flow child spans the container's
                // content box in the inline axis, and is a zero-height line at the child's
                // hypothetical position in the block axis (css-position-3 staticpos-rect). The
                // child's own self-alignment properties decide which edge of the inline rectangle
                // it uses; 'justify-self: auto' takes the container's 'justify-items', while block
                // layout has no block-axis container fallback for 'align-self'.
                // Upstream #1206 (a4dff69e).
                float areaStart = contentBoxInset.left;
                float areaEnd = containerOuterWidth - contentBoxInset.right;
                AlignItems justifySelf = item.justifySelf != null && item.justifySelf != AlignItems.AUTO
                                         ? item.justifySelf
                                         : justifyItems;
                if (justifySelf == AlignItems.AUTO) {
                    justifySelf = null;
                }
                item.staticAlignX = AlignmentHelper.resolveSelfRelative(justifySelf, item.direction, direction, true);
                AlignItems alignSelf = item.alignSelf != null && item.alignSelf != AlignItems.AUTO
                                       ? item.alignSelf
                                       : null;
                item.staticAlignY = AlignmentHelper.resolveSelfRelative(alignSelf, item.direction, direction, false);
                item.staticAreaStartX = areaStart;
                item.staticAreaEndX = areaEnd;
                item.staticPosition = new FloatPoint(
                    staticAlignmentEdge(areaStart, areaEnd, item.staticAlignX, isRtl),
                    yOffsetForAbsolute
                );
                continue;
            }

            // Resolve margins.
            //
            // Percentage (auto-margin-free) margins resolve against the containing block's
            // inline size, i.e. this container's *content box* width, not its outer width.
            // BK-21 / upstream #990 (86d93d95).
            //
            // The classification and all basis-independent values were captured when the item was
            // built (see #generateItemList), so a child with plain lengths, auto margins or no
            // margins at all reads four primitives here and never touches the style object; only a
            // percent or calc() side resolves against the content-box width. An auto (or
            // unresolvable) component stays {@link Float#NaN} until its used value is known.
            // P18.
            float marginLeft;
            float marginRight;
            float marginTop;
            float marginBottom;
            if (item.marginsPreResolved) {
                marginLeft = item.marginLeft;
                marginRight = item.marginRight;
                marginTop = item.marginTop;
                marginBottom = item.marginBottom;
            } else {
                TaffyRect<LengthPercentageAuto> marginStyle = item.margin;
                marginLeft = item.marginLeftNeedsInnerWidth
                             ? marginStyle.left.maybeResolve(containerInnerWidth) : item.marginLeft;
                marginRight = item.marginRightNeedsInnerWidth
                              ? marginStyle.right.maybeResolve(containerInnerWidth) : item.marginRight;
                marginTop = item.marginTopNeedsInnerWidth
                            ? marginStyle.top.maybeResolve(containerInnerWidth) : item.marginTop;
                marginBottom = item.marginBottomNeedsInnerWidth
                               ? marginStyle.bottom.maybeResolve(containerInnerWidth) : item.marginBottom;
            }
            boolean autoMarginLeft = item.marginLeftIsAuto;
            boolean autoMarginRight = item.marginRightIsAuto;
            // Placement uses the non-auto margins only: an auto margin contributes nothing to the
            // box's outer size until it is resolved to its used value after layout.
            float nonAutoMarginLeft = Float.isNaN(marginLeft) ? 0f : marginLeft;
            float nonAutoMarginRight = Float.isNaN(marginRight) ? 0f : marginRight;
            float nonAutoMarginTop = Float.isNaN(marginTop) ? 0f : marginTop;
            float nonAutoMarginBottom = Float.isNaN(marginBottom) ? 0f : marginBottom;
            float itemNonAutoXMarginSum = nonAutoMarginLeft + nonAutoMarginRight;

            // === Floated child (CSS 2.2 §9.5.1) ===
            //
            // A float is laid out at its shrink-to-fit size, removed from the normal block flow
            // and placed through the container's FloatContext. It does not advance the in-flow
            // cursor; later in-flow blocks find a content slot that avoids it. The container's
            // auto height only grows for a BFC root (flow-root / scroll container), matching
            // upstream's #1384 rule.
            if (item.floatStyle.isFloated()) {
                float availableWidth = Math.max(0.0f, containerInnerWidth - itemNonAutoXMarginSum);
                float floatWidth = item.size.width;
                if (Float.isNaN(floatWidth)) {
                    // width:auto on a float is shrink-to-fit: use the max-content measurement,
                    // clamped by the available width.
                    FloatSize measured = layoutComputer.measureChildSize(
                        item.nodeId,
                        new FloatSize(NaN, item.size.height),
                        parentSize,
                        new TaffySize<>(AvailableSpace.maxContent(), AvailableSpace.maxContent()),
                        SizingMode.INHERENT_SIZE,
                        TaffyLine.FALSE
                    );
                    floatWidth = Float.isNaN(measured.width)
                                 ? availableWidth
                                 : Math.min(measured.width, availableWidth);
                }
                floatWidth = TaffyMath.clamp(floatWidth, item.minSize.width, item.maxSize.width);
                // A definite block-axis min-size transfers through the ratio to floor the
                // shrink-to-fit inline size (block-aspect-ratio-025: an empty ratio-1 float
                // with min-height: 100px is 100px wide).
                if (Float.isNaN(item.size.width) && !Float.isNaN(item.aspectRatio)
                        && !Float.isNaN(item.minSize.height)) {
                    floatWidth = TaffyMath.clamp(
                        Math.max(floatWidth, item.minSize.height * item.aspectRatio),
                        item.minSize.width, item.maxSize.width);
                }

                LayoutOutput itemOutput = layoutComputer.performChildLayout(
                    item.nodeId,
                    new FloatSize(floatWidth, item.size.height),
                    parentSize,
                    new TaffySize<>(AvailableSpace.definite(availableWidth), AvailableSpace.maxContent()),
                    SizingMode.INHERENT_SIZE,
                    // A float establishes a new block formatting context: its margins never
                    // collapse with its children's or its siblings'.
                    TaffyLine.FALSE,
                    runMode,
                    // Marks this immediate child layout as a float so its own floats are
                    // contained by its height; the context also bypasses the normal cache.
                    BlockContext.rootAsFloat()
                );
                FloatSize finalSize = itemOutput.size();
                // Negative margins can make the margin box smaller than the border box; the
                // placement context requires a finite non-negative size.
                float marginBoxWidth = Math.max(0.0f, finalSize.width + itemNonAutoXMarginSum);
                float marginBoxHeight = Math.max(
                    0.0f, finalSize.height + nonAutoMarginTop + nonAutoMarginBottom);
                float minY = committedYOffset + activeCollapsibleMarginSet.resolve();
                FloatContext.Direction floatDirection = item.floatStyle == TaffyFloat.LEFT
                                                        ? FloatContext.Direction.LEFT
                                                        : FloatContext.Direction.RIGHT;
                FloatContext.FloatPlacement placement = blockContext.placeFloatedBox(
                    marginBoxWidth,
                    marginBoxHeight,
                    minY,
                    floatDirection,
                    item.clear
                );

                // The context returns the margin-box position relative to this block's border
                // box; the Layout reports the border box. A relative inset is applied afterwards
                // and does not affect the placement of any other box (CSS 2.2 9.4.3).
                float x = placement.x() + nonAutoMarginLeft;
                float y = placement.y() + nonAutoMarginTop;
                if (item.position == TaffyPosition.RELATIVE) {
                    x += relativeInsetX(item.inset, containerInnerWidth, isRtl);
                    y += relativeInsetY(item.inset, childPercentageResolutionHeight);
                }

                FloatSize scrollbarSize = scrollbarSize(item);
                item.finalLayout = new Layout(
                    item.order,
                    new FloatPoint(x, y),
                    finalSize,
                    itemOutput.contentSize(),
                    itemOutput.scrollableOverflow(),
                    scrollbarSize,
                    item.border,
                    item.padding,
                    new FloatRect(
                        nonAutoMarginLeft, nonAutoMarginRight,
                        nonAutoMarginTop, nonAutoMarginBottom)
                );
                continue;
            }

            // Avoid the container's floats for a non-floated block. findBfcSlot applies the
            // box's lead/trail margins (including negative margins) and keeps retrying after the
            // returned segment until the box's desired border width fits. With no active float
            // this is skipped, keeping the non-float path unchanged.
            float floatSlotX = 0.0f;
            float floatSlotY = NaN;
            float floatSlotWidth = containerInnerWidth;
            float floatAvailableWidth = containerInnerWidth;
            boolean avoidsFloats = false;
            boolean pushedBelowFloat = false;
            float minYForSlot = committedYOffset + activeCollapsibleMarginSet.resolve();
            if (blockContext.hasActiveFloats(minYForSlot)) {
                // Only the styled (minimum) height is known before the box is laid out. Clamp it
                // by min/max and fall back to min-height, matching upstream #1385; otherwise a
                // style height larger than max-height would check the box against floats it can
                // never reach.
                float clampedStyleHeight = maybeClamp(
                    item.size, item.minSize, item.maxSize).height;
                float minHeight = !Float.isNaN(clampedStyleHeight) ? clampedStyleHeight : 0.0f;
                if (!Float.isNaN(item.minSize.height)) {
                    minHeight = Math.max(minHeight, item.minSize.height);
                }
                minHeight = Math.max(minHeight, item.paddingBorderSum.height);
                float minAutoWidth = Math.max(0.0f, -itemNonAutoXMarginSum);

                int afterSegment = FloatContext.NO_REGION;
                BfcSlot slot;
                while (true) {
                    slot = blockContext.findBfcSlot(
                        minYForSlot,
                        minHeight,
                        nonAutoMarginLeft,
                        nonAutoMarginRight,
                        direction,
                        item.clear,
                        afterSegment
                    );
                    float desiredWidth = !Float.isNaN(item.size.width)
                                         ? item.size.width
                                         : Math.max(slot.stretchWidth(), minAutoWidth);
                    desiredWidth = TaffyMath.clamp(desiredWidth, item.minSize.width, item.maxSize.width);
                    // A box whose block size is derived from its inline size through an aspect
                    // ratio must fit vertically in the slot too: otherwise a 100px-tall
                    // ratio-1 box would be placed in a 50px-tall float slot and overlap the
                    // float that starts below it (WPT floats-aspect-ratio-001). Retry with the
                    // next segment, where the strip is narrower and the derived height smaller.
                    float desiredHeight = !Float.isNaN(item.size.height)
                                          ? item.size.height
                                          : (!Float.isNaN(item.aspectRatio) && !Float.isNaN(desiredWidth)
                                             ? desiredWidth / item.aspectRatio : NaN);
                    if (!slot.hasSegment()
                        || (desiredWidth <= slot.borderWidth() + FloatContext.FIT_TOLERANCE
                            && (Float.isNaN(desiredHeight)
                                || desiredHeight <= slot.height() + FloatContext.FIT_TOLERANCE))) {
                        break;
                    }
                    if (slot.segmentId() == FloatContext.NO_REGION) {
                        break;
                    }
                    afterSegment = slot.segmentId();
                }

                floatSlotX = slot.x();
                floatSlotY = slot.y();
                floatSlotWidth = slot.borderWidth();
                floatAvailableWidth = Math.max(0.0f, slot.stretchWidth());
                avoidsFloats = true;
                pushedBelowFloat = slot.y() > minYForSlot;
            }

            // Resolve this item's inline-axis self alignment (css-align-3 section 6.1.1, upstream
            // #1250). Only the legacy AUTO sentinel means "not specified": it falls back to the
            // container's justify-items, while an explicit NORMAL suppresses that fallback and
            // keeps the default block layout rules. The combined value is then resolved against
            // the item's own direction, so self-start/self-end flip in the inline axis when the
            // item's direction differs from the container's (upstream #1077).
            AlignItems justifySelf = item.justifySelf != null && item.justifySelf != AlignItems.AUTO
                                     ? item.justifySelf
                                     : justifyItems;
            if (justifySelf == AlignItems.AUTO) {
                justifySelf = null;
            }
            justifySelf = AlignmentHelper.resolveSelfRelative(justifySelf, item.direction, direction, true);
            boolean isStretchJustifySelf = justifySelf == AlignItems.STRETCH;
            // The resolved justify-self if it is neither normal nor stretch: only such a value
            // sizes an auto width as fit-content and aligns the margin box itself.
            AlignItems nonStretchJustifySelf =
                justifySelf == null || isStretchJustifySelf || AlignmentHelper.isNormal(justifySelf)
                ? null
                : justifySelf;

            FloatSize knownDimensions;
            // Unless stretched, tables and compressible replaced elements resolve their own
            // width instead of being stretch-sized (CSS2 section 10.3.3): an auto-width table is
            // sized by its content, and a replaced element's auto width is its intrinsic size,
            // not the containing block's width. Upstream passes Size::NONE for both so the
            // child's own layout resolves its style/intrinsic size; a stretch justify-self
            // stretches such a box through the regular stretch path instead (upstream #1250).
            // BK-04 / upstream #1002 (945de0dd); ST-05's isCompressibleReplaced().
            if ((item.isTable || item.isCompressibleReplaced) && !isStretchJustifySelf) {
                knownDimensions = new FloatSize(NaN, NaN);
            } else {
                // The width this item would take if it were stretched to fill its containing
                // block's content box (less the block margins that stay outside it).
                float stretchWidth = Math.max(0f, avoidsFloats
                                                  ? floatAvailableWidth
                                                  : containerInnerWidth - itemNonAutoXMarginSum);
                // "The automatic width of a block-level box whose justify-self is neither normal
                // nor stretch is fit-content rather than stretch": express that as the
                // fit-content sizing keyword, which resolves to a measurement under the stretch
                // width (upstream #1250). An explicit sizing keyword on the item's own width
                // (min-content, max-content, fit-content, fit-content(...), stretch) resolves
                // either to an exact width or to a measurement under its own constraint
                // (upstream #1099/#1103/#1247).
                TaffyDimension widthStyle = nonStretchJustifySelf != null && item.widthIsAuto
                                            ? TaffyDimension.fitContent()
                                            : item.widthStyle;
                SizingKeyword.Resolution keyword = SizingKeyword.resolve(
                    widthStyle, stretchWidth, containerInnerWidth);
                float keywordWidth = NaN;
                if (keyword != null) {
                    if (keyword.isExact()) {
                        keywordWidth = keyword.getExactValue();
                    } else {
                        FloatSize measured = layoutComputer.measureChildSize(
                            item.nodeId,
                            new FloatSize(NaN, NaN),
                            parentSize,
                            new TaffySize<>(keyword.getMeasureAvailableSpace(), AvailableSpace.maxContent()),
                            SizingMode.INHERENT_SIZE,
                            TaffyLine.TRUE
                        );
                        keywordWidth = measured.width;
                    }
                }
                float width = !Float.isNaN(item.size.width)
                              ? item.size.width
                              : (!Float.isNaN(keywordWidth) ? keywordWidth : stretchWidth);
                // A definite block-axis min/max transfers through the ratio to constrain an auto
                // or intrinsic inline size (css-sizing-4 transferred constraints): a box with
                // `max-height: 100px; aspect-ratio: 1` and a max-content inline size is 100px
                // wide, and `max-height: 40px; min-width: 100px` with the same ratio is 100px
                // wide (the transferred cap lowers the stretch width to 40px, the explicit
                // min-width then raises it back to 100px). Chrome never transfers the inline
                // min/max into the block axis, so the reverse is deliberately not done.
                if (Float.isNaN(item.size.width) && !Float.isNaN(item.aspectRatio)) {
                    if (!Float.isNaN(item.maxSize.height)) {
                        width = Math.min(width, item.maxSize.height * item.aspectRatio);
                    }
                    if (!Float.isNaN(item.minSize.height)) {
                        width = Math.max(width, item.minSize.height * item.aspectRatio);
                    }
                }
                width = TaffyMath.clamp(width, item.minSize.width, item.maxSize.width);
                // css-sizing-4 automatic minimum size: an inline size transferred from the block
                // size is floored by the item's min-content contribution unless its own min-width
                // overrides the automatic minimum. (WPT block-aspect-ratio-015/056/057.)
                if (item.widthFromAspectRatio
                    && !item.overflow.x.isScrollContainer()
                    && !item.overflow.y.isScrollContainer()
                    && (Float.isNaN(item.minSize.width)
                        || item.minSize.width <= item.paddingBorderSum.width)) {
                    FloatSize minContent = layoutComputer.measureChildSize(
                        item.nodeId,
                        new FloatSize(NaN, NaN),
                        parentSize,
                        new TaffySize<>(AvailableSpace.minContent(), AvailableSpace.maxContent()),
                        SizingMode.INHERENT_SIZE,
                        TaffyLine.TRUE
                    );
                    if (!Float.isNaN(minContent.width)) {
                        width = Math.max(width, minContent.width);
                        width = Math.max(
                            TaffyMath.clamp(width, item.minSize.width, item.maxSize.width),
                            item.paddingBorderSum.width);
                    }
                }
                // A `height: stretch` item fills this container's content box, minus the block
                // margins that cannot collapse through the corresponding edge of the containing
                // block. A resolved style height (including one derived through `aspect-ratio`)
                // wins, as upstream resolves `height.or(keyword_height)`. The remaining intrinsic
                // sizing keywords are equal to the content size in the block axis, i.e. they
                // behave like `auto`, so only `stretch` is handled here.
                // Upstream #1242 (6a36efbe).
                float keywordHeight = resolveStretchHeight(
                    item.heightStyle,
                    childPercentageResolutionHeight,
                    nonAutoMarginTop,
                    nonAutoMarginBottom,
                    childrenMarginsAdjoinOwnEdges
                );
                // A block size derived from the inline size through the ratio is not passed down
                // as a given: the item derives it itself during its final layout, where it can
                // still be floored by the content height (its block-axis automatic minimum size).
                // Handing the transferred height down would make it look specified and freeze it
                // below the content (WPT block-aspect-ratio-009/017/034/035/042).
                float resolvedHeight =
                    item.heightFromAspectRatio
                        ? keywordHeight
                        : (!Float.isNaN(item.size.height) ? item.size.height : keywordHeight);
                knownDimensions = maybeClamp(
                    new FloatSize(width, resolvedHeight),
                    item.minSize,
                    item.maxSize
                );
            }

            // A plain block child participates in this BFC: give it a sub-context whose top is
            // the estimated border-box y and whose x-insets position its border box inside the
            // parent's content box. flow-root/scroll/table and other display types establish
            // their own formatting context and are laid out without a context.
            // A child that establishes its own formatting context (layout/paint containment) is
            // deliberately excluded: it contains its own floats and only avoids the parent's, so
            // sharing the parent's FloatContext would leak floats across the boundary. Upstream
            // #1128's is_in_same_bfc gate.
            BlockContext childBlockContext = null;
            if (item.display == TaffyDisplay.BLOCK
                && !item.isTable
                && !item.overflow.x.isScrollContainer()
                && !item.overflow.y.isScrollContainer()
                && !item.contain.establishesIndependentFormattingContext()) {
                // A table or replaced child has no known width (it resolves its own size), so the
                // containing block used for float avoidance is the stretch width it would get if
                // it were stretch-sized instead of a zero width, matching upstream #1002's
                // `known_dimensions.width.unwrap_or(stretch_width)`.
                float childWidth = knownDimensions.width;
                if (Float.isNaN(childWidth)) {
                    childWidth = avoidsFloats
                                 ? floatAvailableWidth
                                 : containerInnerWidth - itemNonAutoXMarginSum;
                    childWidth = Math.max(childWidth, item.paddingBorderSum.width);
                }
                float childLeftInset = contentBoxInset.left + nonAutoMarginLeft;
                float childRightInset = containerOuterWidth - childLeftInset - childWidth;
                float estimatedY = committedYOffset + activeCollapsibleMarginSet.resolve();
                childBlockContext = blockContext.subContext(estimatedY, childLeftInset, childRightInset);
            }

            // The parent's run mode is propagated: measuring this container must not make its
            // children write layout state, and a vertical ComputeSize pass needs the children's
            // margin-collapse outputs. BK-02 / upstream #972.
            if (itemAvailableSpace == null || itemNonAutoXMarginSum != itemAvailableSpaceMarginSum) {
                itemAvailableSpace = new TaffySize<>(
                    subtractFromAvailable(availableSpace.width, itemNonAutoXMarginSum),
                    availableSpace.height
                );
                itemAvailableSpaceMarginSum = itemNonAutoXMarginSum;
            }
            LayoutOutput itemOutput = childBlockContext != null
                ? layoutComputer.performChildLayout(
                    item.nodeId,
                    knownDimensions,
                    parentSize,
                    itemAvailableSpace,
                    SizingMode.INHERENT_SIZE,
                    TaffyLine.TRUE,
                    runMode,
                    childBlockContext
                )
                : layoutComputer.performChildLayout(
                    item.nodeId,
                    knownDimensions,
                    parentSize,
                    itemAvailableSpace,
                    SizingMode.INHERENT_SIZE,
                    TaffyLine.TRUE,
                    runMode
                );

            FloatSize finalSize = itemOutput.size();

            // Get margin collapse info from child layout
            CollapsibleMarginSet topMarginSet = itemOutput.topMargin().copy()
                                                          .collapseWithMargin(nonAutoMarginTop);
            CollapsibleMarginSet bottomMarginSet = itemOutput.bottomMargin().copy()
                                                             .collapseWithMargin(nonAutoMarginBottom);

            // Expand auto margins
            float freeXSpace = avoidsFloats
                               ? Math.max(0, floatAvailableWidth - finalSize.width)
                               : Math.max(0, containerInnerWidth - finalSize.width - itemNonAutoXMarginSum);
            int autoMarginCount = (autoMarginLeft ? 1 : 0) + (autoMarginRight ? 1 : 0);
            float xAxisAutoMarginSize = autoMarginCount > 0 ? freeXSpace / autoMarginCount : 0;

            // Layout.margin reports the child's *own* margins. The collapsed sets above describe
            // where the box is placed relative to its siblings (a parent/child margin interaction),
            // not the box's own margin; reporting them leaked the parent's collapsed margin into
            // the child's margin. Auto margins report 0 here: the resolved auto size is only
            // consumed by the positioning maths. BK-10 / upstream #1287.
            float resolvedMarginLeft = Float.isNaN(marginLeft) ? xAxisAutoMarginSize : marginLeft;
            float resolvedMarginRight = Float.isNaN(marginRight) ? xAxisAutoMarginSize : marginRight;
            // A box whose four reported margin components are all zero shares the read-only zero
            // rectangle with the other zero geometry it already receives (padding/border), instead
            // of allocating one per child. P18.
            FloatRect resolvedMargin = resolvedMarginLeft == 0f && resolvedMarginRight == 0f
                                       && nonAutoMarginTop == 0f && nonAutoMarginBottom == 0f
                                       ? ZERO_RECT
                                       : new FloatRect(resolvedMarginLeft, resolvedMarginRight,
                                                       nonAutoMarginTop, nonAutoMarginBottom);

            // Resolve item inset. Only `position: relative` applies its inset as a correction
            // offset. Static and sticky boxes are laid out in normal flow without a relative
            // offset (a sticky offset depends on the scroll position and is the caller's job),
            // and fixed is out of flow, so it is positioned by the absolute pass instead.
            // Upstream #1140.
            // Horizontal insets resolve against the containing block width; vertical insets
            // against its percentage resolution height (not a hard-coded zero basis, which made
            // e.g. `top: 50%` a no-op). BK-08 / upstream #1122 (f1a03062).
            float insetOffsetX = 0f;
            float insetOffsetY = 0f;
            if (item.position == TaffyPosition.RELATIVE) {
                insetOffsetX = relativeInsetX(item.inset, containerInnerWidth, isRtl);
                insetOffsetY = relativeInsetY(item.inset, childPercentageResolutionHeight);
            }

            // Compute y margin offset with margin collapse.
            //
            // This must use the *collapsed* top margin set (the child's collapsed-through margins
            // combined with the child's own top margin), not Layout.margin, which reports the
            // child's own margin only (BK-10). Upstream computes this from `top_margin_set`.
            float yMarginOffset;
            if (isCollapsingWithFirstMarginSet && ownMarginsCollapseWithChildren.start) {
                yMarginOffset = 0f;
            } else {
                yMarginOffset = activeCollapsibleMarginSet.copy()
                                                          .collapseWithSet(topMarginSet).resolve();
            }

            item.canBeCollapsedThrough = itemOutput.marginsCanCollapseThrough();

            // An in-flow item's static position is never read (only an out-of-flow child needs
            // one, and that is filled in above), so it is not recomputed here.
            float y = committedYOffset + insetOffsetY + yMarginOffset;
            if (avoidsFloats) {
                // A box beside floats starts at the content slot's top edge. Its top margin may
                // overlap the float, matching CSS's float-avoidance rules; margin collapsing
                // around a pushed-down box is simplified in this batch (TODO).
                y = floatSlotY + insetOffsetY;
            }

            // Calculate the x position. A non-stretch justify-self aligns the item's margin box
            // within its alignment container and takes precedence over legacy text-align, but
            // auto margins take precedence over both: they have already absorbed the free space,
            // and the historical auto-margin placement below is the result the spec expects for
            // that case (upstream #1250).
            float itemOuterWidth = finalSize.width + resolvedMargin.left + resolvedMargin.right;
            float freeSpace = containerInnerWidth - itemOuterWidth;
            boolean hasAutoXMargin = autoMarginLeft || autoMarginRight;
            float x;

            if (nonStretchJustifySelf != null && !hasAutoXMargin) {
                // A safe value falls back to start when the margin box overflows the containing
                // block; otherwise the safety modifier is dropped (upstream #952).
                AlignItems keyword = AlignmentHelper.resolveSelfSafety(nonStretchJustifySelf, freeSpace < 0f);
                float offset = switch (keyword) {
                    // Centring is direction-independent...
                    case CENTER -> freeSpace / 2f;
                    // ...whereas start/end are logical edges: in RTL start is the right edge.
                    case END, FLEX_END, SELF_END -> isRtl ? 0f : freeSpace;
                    default -> isRtl ? freeSpace : 0f;
                };
                x = contentBoxInset.left + resolvedMargin.left + insetOffsetX + offset;
            } else if (isRtl) {
                // RTL: Default alignment is to the right (START in RTL)
                // Calculate x so item aligns to right edge by default
                x = contentBoxInset.left + freeSpace + resolvedMargin.left + insetOffsetX;

                // Apply text alignment adjustments for RTL. Text-align only applies when no
                // non-stretch justify-self took over the inline-axis alignment.
                if (nonStretchJustifySelf == null && itemOuterWidth < containerInnerWidth) {
                    switch (textAlign) {
                        case LEFT:
                        case END:
                            // Align to left (end in RTL) - subtract freeSpace from the right-aligned position
                            x = contentBoxInset.left + resolvedMargin.left + insetOffsetX;
                            break;
                        case CENTER:
                            // Center alignment
                            x = contentBoxInset.left + freeSpace / 2 + resolvedMargin.left + insetOffsetX;
                            break;
                        default:
                            // START, RIGHT, or default - stay right-aligned (already calculated above)
                            break;
                    }
                }
            } else {
                // LTR: Default alignment is to the left
                x = contentBoxInset.left + insetOffsetX + resolvedMargin.left;

                // Apply text alignment adjustments for LTR. Text-align only applies when no
                // non-stretch justify-self took over the inline-axis alignment.
                if (nonStretchJustifySelf == null && itemOuterWidth < containerInnerWidth) {
                    switch (textAlign) {
                        case RIGHT:
                        case END:
                            x += freeSpace;
                            break;
                        case CENTER:
                            x += freeSpace / 2;
                            break;
                        default:
                            break;
                    }
                }
            }

            if (avoidsFloats) {
                // BfcSlot already accounts for the box's non-auto margins; only resolved auto
                // margins and the relative inset are added here. RTL mirrors the slot's left
                // border edge around the border-box width.
                float extraLeft = resolvedMargin.left - nonAutoMarginLeft;
                float extraRight = resolvedMargin.right - nonAutoMarginRight;
                x = isRtl
                    ? floatSlotX + floatSlotWidth - finalSize.width - extraRight + insetOffsetX
                    : floatSlotX + extraLeft + insetOffsetX;
            }

            if (childBlockContext != null && childBlockContext.hasFloatedContentContribution()) {
                // Floats placed by a same-BFC child are relative to its border top; merge them
                // into this block in this block's local coordinate space. A child with no floats
                // must not contribute its y position.
                blockContext.addChildFloatedContentHeightContribution(
                    y - insetOffsetY + childBlockContext.floatedContentHeightContribution());
            }

            // A block container's first baseline is the first baseline of its first in-flow child
            // that has one. The child's relative offset does not affect it: y includes
            // insetOffsetY, so the baseline location subtracts it again (upstream #996 added the
            // propagation, #1323 removed the relative offset). Scroll-container children are
            // clamped to their border box, and synthesize a baseline at the border-box bottom edge
            // when they report none (upstream #1126 / #1323 BaselineHelper).
            if (Float.isNaN(firstBaseline)) {
                firstBaseline = BaselineHelper.childFirstBaseline(
                    itemOutput, y - insetOffsetY, item.overflow.y.isScrollContainer());
            }

            FloatSize scrollbarSize = scrollbarSize(item);

            // The child's own scrollable overflow is carried into its layout so that this
            // container can fold it into its own contribution (SO-01): the value is relative to
            // the child's client area and is moved into border-box coordinates when the container
            // accumulates it.
            Layout layout = new Layout(
                item.order,
                new FloatPoint(x, y),
                finalSize,
                itemOutput.contentSize(),
                itemOutput.scrollableOverflow(),
                scrollbarSize,
                item.border,
                item.padding,
                resolvedMargin
            );

            // Defer the tree write until the caller has been confirmed to want a final layout
            // (see the commit loop in compute). Upstream #971 (BK-01).
            item.finalLayout = layout;

            // A box that had to move below a float no longer has a top margin adjoining the
            // container's top strut (the simplified form of upstream #1046's forced clearance).
            if (pushedBelowFloat) {
                isCollapsingWithFirstMarginSet = false;
            }

            // Update first_child_top_margin_set
            if (isCollapsingWithFirstMarginSet) {
                if (item.canBeCollapsedThrough) {
                    firstChildTopMarginSet
                        .collapseWithSet(topMarginSet)
                        .collapseWithSet(bottomMarginSet);
                } else {
                    firstChildTopMarginSet.collapseWithSet(topMarginSet);
                    isCollapsingWithFirstMarginSet = false;
                }
            }

            // Update active_collapsible_margin_set
            if (item.canBeCollapsedThrough) {
                activeCollapsibleMarginSet
                    .collapseWithSet(topMarginSet)
                    .collapseWithSet(bottomMarginSet);
                yOffsetForAbsolute = committedYOffset + finalSize.height + yMarginOffset;
            } else {
                committedYOffset += finalSize.height + yMarginOffset;
                activeCollapsibleMarginSet = bottomMarginSet;
                yOffsetForAbsolute = committedYOffset + activeCollapsibleMarginSet.resolve();
                allChildrenCanBeCollapsedThrough = false;
            }
            if (avoidsFloats) {
                // A slot-pushed box must still advance the cursor; otherwise the next sibling
                // would be placed above it. Margin collapsing around such a box is simplified.
                committedYOffset = Math.max(committedYOffset, floatSlotY + finalSize.height);
                yOffsetForAbsolute = Math.max(
                    yOffsetForAbsolute,
                    floatSlotY + finalSize.height + activeCollapsibleMarginSet.resolve()
                );
            }
        }

        CollapsibleMarginSet lastChildBottomMarginSet = activeCollapsibleMarginSet;
        // When the first child's margins collapse through this box and this box has no top
        // border/padding, they were already folded into the box's own top margin set, so they
        // must not also be added to its bottom (they are not adjoining the box's bottom edge).
        // BK-11 / upstream #1397.
        boolean firstChildMarginSetCollapsedThrough =
            isCollapsingWithFirstMarginSet && ownMarginsCollapseWithChildren.start;
        float bottomYMarginOffset = (ownMarginsCollapseWithChildren.end || firstChildMarginSetCollapsedThrough)
                                    ? 0f
                                    : lastChildBottomMarginSet.resolve();

        committedYOffset += contentBoxInset.bottom + bottomYMarginOffset;
        float contentHeight = Math.max(0f, committedYOffset);

        return new InFlowLayoutResult(
            contentHeight,
            firstChildTopMarginSet,
            lastChildBottomMarginSet,
            allChildrenCanBeCollapsedThrough,
            firstBaseline,
            blockContext.floatedContentHeightContribution()
        );
    }

    /**
     * Resolves the inline-axis offset of a relatively positioned box (CSS 2.2 9.4.3).
     *
     * <p>Percentages that cannot be resolved behave as auto. When both insets are non-auto,
     * {@code left} wins in LTR and {@code right} wins in RTL (upstream #1305's shared resolver).</p>
     */
    private static float relativeInsetX(
        TaffyRect<LengthPercentageAuto> inset, float basis, boolean rtl) {
        float left = inset.left.maybeResolve(basis);
        float right = inset.right.maybeResolve(basis);
        if (rtl) {
            return !Float.isNaN(right) ? -right : (!Float.isNaN(left) ? left : 0f);
        }
        return !Float.isNaN(left) ? left : (!Float.isNaN(right) ? -right : 0f);
    }

    /**
     * Resolves the block-axis offset of a relatively positioned box: {@code top} wins over
     * {@code bottom} when both are non-auto.
     */
    private static float relativeInsetY(
        TaffyRect<LengthPercentageAuto> inset, float basis) {
        float top = inset.top.maybeResolve(basis);
        float bottom = inset.bottom.maybeResolve(basis);
        return !Float.isNaN(top) ? top : (!Float.isNaN(bottom) ? -bottom : 0f);
    }

    /**
     * Lays out a container's absolutely positioned children and accumulates the scrollable
     * overflow they contribute to it (P4/SO-01/SO-02, upstream #1118).
     *
     * <p>An out-of-flow child is positioned against the container's positioning area, which is
     * its client area (border box less border and scrollbar gutters), so its contribution
     * location is the child's border-box origin in that frame. The container description
     * therefore carries no padding of its own and the child's margin is not part of the reachable
     * region (upstream's out-of-flow pass describes both as zero).</p>
     *
     * @param items        the container's block items
     * @param areaSize     the container's final border-box size
     * @param areaInset    the inset of the positioning area within the border box
     * @param overflowContainer the container description used for the contributions
     * @return the union of the children's overflow contributions and their legacy content-size
     *         extent, both measured in the containing-block frame
     */
    private OutOfFlowOverflow performAbsoluteLayoutOnChildren(
        List<BlockItem> items,
        FloatSize areaSize,
        FloatRect areaInset,
        TaffyPoint<Overflow> containerOverflow,
        ScrollableOverflowUtil.Container overflowContainer) {

        TaffyTree tree = layoutComputer.getTree();
        float areaWidth = areaSize.width - areaInset.left - areaInset.right;
        float areaHeight = areaSize.height - areaInset.top - areaInset.bottom;

        FloatRect accumulatedOverflow = FloatRect.zero();
        FloatSize contentSize = FloatSize.zero();

        for (int itemIndex = 0, itemCount = items.size(); itemIndex < itemCount; itemIndex++) {
            BlockItem item = items.get(itemIndex);
            if (!item.position.isOutOfFlow()) continue;

            TaffyStyle childStyle = tree.getStyle(item.nodeId);
            if (childStyle.getBoxGenerationMode() == BoxGenerationMode.NONE) continue;

            // An `auto` size of an absolutely positioned *replaced* element resolves to its
            // intrinsic size instead of being stretched between its insets
            // (https://www.w3.org/TR/CSS22/visudet.html#abs-replaced-width and
            // #abs-replaced-height). Upstream #1203 (b52be36a); #1256 later clarified that this
            // uses the replaced flag (not the broader compressible-replaced one), matching the
            // ST-05 split.
            boolean isReplaced = childStyle.getItemIsReplaced();

            Float aspectRatio = childStyle.getAspectRatio();

            // Get margin style - need to track which are auto
            TaffyRect<LengthPercentageAuto> marginStyle = childStyle.getMargin();
            FloatRect marginOpt = Resolve.maybeResolveRectLpa(marginStyle, areaWidth);
            FloatRect itemPadding = Resolve.resolveRectOrZero(childStyle.getPadding(), areaWidth);
            FloatRect itemBorder = Resolve.resolveRectOrZero(childStyle.getBorder(), areaWidth);
            FloatSize paddingBorderSum = new FloatSize(
                itemPadding.left + itemPadding.right + itemBorder.left + itemBorder.right,
                itemPadding.top + itemPadding.bottom + itemBorder.top + itemBorder.bottom
            );

            FloatSize boxSizingAdj = childStyle.getBoxSizing() == BoxSizing.CONTENT_BOX
                                     ? paddingBorderSum
                                     : new FloatSize(0f, 0f);

            // Resolve inset
            TaffyRect<LengthPercentageAuto> insetStyle = childStyle.getInset();
            float left = insetStyle.left.maybeResolve(areaWidth);
            float right = insetStyle.right.maybeResolve(areaWidth);
            float top = insetStyle.top.maybeResolve(areaHeight);
            float bottom = insetStyle.bottom.maybeResolve(areaHeight);

            // Compute size from style. The aspect ratio is applied after the specified size has
            // been clamped in its own axis, and min/max are not transferred across axes (see the
            // note in compute()).
            FloatSize size2 = Resolve.maybeResolveSize(childStyle.getSize(), new FloatSize(areaWidth, areaHeight));
            FloatSize styleSize = maybeAdd(size2, boxSizingAdj);
            FloatSize size1 = Resolve.maybeResolveSize(childStyle.getMinSize(), new FloatSize(areaWidth, areaHeight));
            FloatSize minSz = maybeAdd(Resolve.maybeApplyAspectRatio(size1, aspectRatio), boxSizingAdj);
            minSz = maybeMax(minSz, paddingBorderSum);
            FloatSize size = Resolve.maybeResolveSize(childStyle.getMaxSize(), new FloatSize(areaWidth, areaHeight));
            FloatSize maxSz = maybeAdd(Resolve.maybeApplyAspectRatio(size, aspectRatio), boxSizingAdj);

            FloatSize knownDimensions = maybeClamp(styleSize, minSz, maxSz);
            // A missing axis is transferred through the ratio from the other axis' used size. When
            // the other axis is not specified either, a definite max constraint is used as the
            // basis: an absolutely positioned box with `max-height: 100%` and no size transfers
            // that bound into its inline size (WPT abspos-008/014).
            boolean widthFromAspectRatio = false;
            boolean heightFromAspectRatio = false;
            if (aspectRatio != null && !Float.isNaN(aspectRatio)) {
                // A percentage max constraint also acts as a definite used size when no size is
                // specified: an absolutely positioned box with `max-height: 100%` and no height
                // transfers that bound into its inline size (WPT abspos-008/014). A length max
                // constraint is only a cap and does not transfer (the generated upstream
                // block_absolute_aspect_ratio_fill_max_* fixtures pin that behaviour).
                float basisWidth = knownDimensions.width;
                float basisHeight = knownDimensions.height;
                if (Float.isNaN(basisHeight) && childStyle.getMaxSize().height.isPercent()) {
                    basisHeight = maxSz.height;
                }
                if (Float.isNaN(basisWidth) && childStyle.getMaxSize().width.isPercent()) {
                    basisWidth = maxSz.width;
                }
                FloatSize contentBasis = new FloatSize(basisWidth - boxSizingAdj.width,
                    basisHeight - boxSizingAdj.height);
                FloatSize transferred = maybeAdd(
                    Resolve.maybeApplyAspectRatio(contentBasis, aspectRatio), boxSizingAdj);
                FloatSize next = new FloatSize(
                    !Float.isNaN(knownDimensions.width) ? knownDimensions.width : transferred.width,
                    !Float.isNaN(knownDimensions.height) ? knownDimensions.height : transferred.height
                );
                widthFromAspectRatio = Float.isNaN(knownDimensions.width) && !Float.isNaN(next.width);
                heightFromAspectRatio = Float.isNaN(knownDimensions.height) && !Float.isNaN(next.height);
                knownDimensions = maybeMax(maybeClamp(next, minSz, maxSz), paddingBorderSum);
            }

            // For calculating width from inset, use non-auto margins only
            float nonAutoMarginLeft = Float.isNaN(marginOpt.left) ? 0f : marginOpt.left;
            float nonAutoMarginRight = Float.isNaN(marginOpt.right) ? 0f : marginOpt.right;
            float nonAutoMarginTop = Float.isNaN(marginOpt.top) ? 0f : marginOpt.top;
            float nonAutoMarginBottom = Float.isNaN(marginOpt.bottom) ? 0f : marginOpt.bottom;

            // An auto size is the stretch-fit size only when both insets are set and the
            // self-alignment in that axis stretches: explicit 'stretch', or 'normal' for a
            // non-replaced box (https://www.w3.org/TR/css-position-3/#abspos-auto-size). A
            // replaced element's auto size stays its intrinsic size (upstream #1203).
            // Upstream #1206 (a4dff69e).
            if (isStretchSized(item.staticAlignX, left, right, isReplaced)
                && Float.isNaN(knownDimensions.width)) {
                float newWidth = areaWidth - nonAutoMarginLeft - nonAutoMarginRight - left - right;
                knownDimensions = new FloatSize(Math.max(newWidth, 0f), knownDimensions.height);
                knownDimensions = maybeClamp(Resolve.maybeApplyAspectRatio(knownDimensions, aspectRatio), minSz, maxSz);
            }

            if (isStretchSized(item.staticAlignY, top, bottom, isReplaced)
                && Float.isNaN(knownDimensions.height)) {
                float newHeight = areaHeight - nonAutoMarginTop - nonAutoMarginBottom - top - bottom;
                knownDimensions = new FloatSize(knownDimensions.width, Math.max(newHeight, 0f));
                knownDimensions = maybeClamp(Resolve.maybeApplyAspectRatio(knownDimensions, aspectRatio), minSz, maxSz);
            }

            TaffySize<AvailableSpace> availableSpace = new TaffySize<>(
                AvailableSpace.definite(TaffyMath.clamp(areaWidth, minSz.width, maxSz.width)),
                AvailableSpace.definite(TaffyMath.clamp(areaHeight, minSz.height, maxSz.height))
            );

            LayoutOutput measuredOutput = layoutComputer.performChildLayout(
                item.nodeId,
                knownDimensions,
                new FloatSize(areaWidth, areaHeight),
                availableSpace,
                SizingMode.CONTENT_SIZE,
                new TaffyLine<>(false, false)
            );

            // css-sizing-4 automatic minimum size, out-of-flow flavour: an axis transferred
            // through the ratio is floored by the content-based minimum (min-content inline, the
            // content height in the block axis) unless an explicit min-size overrides it.
            // WPT abspos-012/013 need the content floors, abspos-008/014 the transferred bound.
            boolean autoMinimumSizeApplies =
                !childStyle.getOverflow().x.isScrollContainer()
                && !childStyle.getOverflow().y.isScrollContainer();
            // "auto" is the initial min-* value and enables the floor; an explicit
            // min-width/min-height (even 0) disables it. Resolving to NaN cannot tell the two
            // apart, so the style's own axis value is what decides (WPT abspos-017/018).
            boolean autoMinWidth = childStyle.getMinSize().width.isAuto();
            boolean autoMinHeight = childStyle.getMinSize().height.isAuto();
            FloatSize knownWithFloor = knownDimensions;
            if (widthFromAspectRatio
                && autoMinimumSizeApplies
                && autoMinWidth) {
                FloatSize minContent = layoutComputer.measureChildSize(
                    item.nodeId,
                    new FloatSize(NaN, NaN),
                    new FloatSize(areaWidth, areaHeight),
                    new TaffySize<>(AvailableSpace.minContent(), AvailableSpace.maxContent()),
                    SizingMode.INHERENT_SIZE,
                    new TaffyLine<>(false, false)
                );
                if (!Float.isNaN(minContent.width)) {
                    knownWithFloor = new FloatSize(
                        Math.max(Math.max(knownWithFloor.width, minContent.width), paddingBorderSum.width),
                        knownWithFloor.height);
                }
            }
            if (heightFromAspectRatio
                && autoMinimumSizeApplies
                && autoMinHeight) {
                FloatSize contentHeightProbe = layoutComputer.measureChildSize(
                    item.nodeId,
                    new FloatSize(!Float.isNaN(knownWithFloor.width) ? knownWithFloor.width : NaN, NaN),
                    new FloatSize(areaWidth, areaHeight),
                    new TaffySize<>(
                        !Float.isNaN(knownWithFloor.width)
                            ? AvailableSpace.definite(knownWithFloor.width)
                            : AvailableSpace.maxContent(),
                        AvailableSpace.maxContent()),
                    SizingMode.INHERENT_SIZE,
                    new TaffyLine<>(false, false)
                );
                if (!Float.isNaN(contentHeightProbe.height)) {
                    knownWithFloor = new FloatSize(knownWithFloor.width,
                        Math.max(Math.max(knownWithFloor.height, contentHeightProbe.height),
                            paddingBorderSum.height));
                }
            }
            knownDimensions = maybeClamp(knownWithFloor, minSz, maxSz);
            knownDimensions = new FloatSize(Math.max(knownDimensions.width, paddingBorderSum.width),
                Math.max(knownDimensions.height, paddingBorderSum.height));

            FloatSize finalSize = maybeClamp(
                new FloatSize(
                    Float.isNaN(knownDimensions.width) ? measuredOutput.size().width : knownDimensions.width,
                    Float.isNaN(knownDimensions.height) ? measuredOutput.size().height : knownDimensions.height
                ),
                minSz, maxSz
            );

            // Ensure final size is at least padding + border
            finalSize = new FloatSize(
                Math.max(finalSize.width, paddingBorderSum.width),
                Math.max(finalSize.height, paddingBorderSum.height)
            );

            // PA-05: when an axis had to be sized from content, the measured output was produced
            // against an indefinite containing block. Re-run the child layout with the definite
            // final size so percentage sizes in its subtree resolve against it; the returned
            // output supplies the content size (and the descendant layouts) used below, while the
            // final size, auto margins and location keep using `finalSize`.
            LayoutOutput output = layoutComputer.performDefiniteAbsoluteChildLayout(
                item.nodeId,
                knownDimensions,
                finalSize,
                new FloatSize(areaWidth, areaHeight),
                availableSpace,
                SizingMode.CONTENT_SIZE,
                measuredOutput
            );

            // Calculate non-auto margin (only count if inset is set on that side)
            FloatRect nonAutoMargin = new FloatRect(
                Float.isNaN(left) ? 0f : nonAutoMarginLeft,
                Float.isNaN(right) ? 0f : nonAutoMarginRight,
                Float.isNaN(top) ? 0f : nonAutoMarginTop,
                Float.isNaN(bottom) ? 0f : nonAutoMarginBottom
            );

            // Auto margins only absorb free space in an axis where *both* insets are non-auto.
            // With one (or no) inset the axis is over-constrained, so an auto margin resolves to
            // zero instead of inflating towards the one set inset. The free space is measured
            // against the positioning area, and a margin only counts when its own inset is set
            // (nonAutoMargin). Upstream #1202 (3307c7e0).
            float freeSpaceX = !Float.isNaN(left) && !Float.isNaN(right)
                               ? areaWidth - left - right - finalSize.width
                                 - nonAutoMargin.left - nonAutoMargin.right
                               : 0f;
            float freeSpaceY = !Float.isNaN(top) && !Float.isNaN(bottom)
                               ? areaHeight - top - bottom - finalSize.height
                                 - nonAutoMargin.top - nonAutoMargin.bottom
                               : 0f;

            // Calculate auto margin size against the free space left by the *used* (clamped) size.
            // BK-07 / upstream #1096.
            float autoMarginSizeX = calcAutoMarginSize(marginOpt.left, marginOpt.right, freeSpaceX);

            float autoMarginSizeY = calcAutoMarginSize(marginOpt.top, marginOpt.bottom, freeSpaceY);

            FloatRect autoMargin = new FloatRect(
                Float.isNaN(marginOpt.left) ? autoMarginSizeX : 0f,
                Float.isNaN(marginOpt.right) ? autoMarginSizeX : 0f,
                Float.isNaN(marginOpt.top) ? autoMarginSizeY : 0f,
                Float.isNaN(marginOpt.bottom) ? autoMarginSizeY : 0f
            );

            FloatRect resolvedMargin = new FloatRect(
                Float.isNaN(marginOpt.left) ? autoMargin.left : marginOpt.left,
                Float.isNaN(marginOpt.right) ? autoMargin.right : marginOpt.right,
                Float.isNaN(marginOpt.top) ? autoMargin.top : marginOpt.top,
                Float.isNaN(marginOpt.bottom) ? autoMargin.bottom : marginOpt.bottom
            );

            // Position the margin box within the inset-modified containing block
            // (https://www.w3.org/TR/css-position-3/#abspos-alignment). With both insets auto the
            // box is aligned within its static-position rectangle, and the 'safe' overflow-position
            // keyword can fall back to the IMCB start edge (OF-06 / upstream #1267).
            boolean rtl = overflowContainer.reversedX();
            float staticY = item.staticPosition.y - areaInset.top;
            float x = areaInset.left + absoluteAxisOffset(new AbsoluteAxis(
                areaWidth,
                left, right,
                finalSize.width,
                resolvedMargin.left, resolvedMargin.right,
                Float.isNaN(marginOpt.left), Float.isNaN(marginOpt.right),
                item.staticAlignX,
                AlignmentHelper.isSafe(item.staticAlignX),
                item.staticAreaStartX - areaInset.left,
                item.staticAreaEndX - areaInset.left,
                rtl,
                containerOverflow.x.isScrollContainer()
            ));
            float y = areaInset.top + absoluteAxisOffset(new AbsoluteAxis(
                areaHeight,
                top, bottom,
                finalSize.height,
                resolvedMargin.top, resolvedMargin.bottom,
                Float.isNaN(marginOpt.top), Float.isNaN(marginOpt.bottom),
                item.staticAlignY,
                AlignmentHelper.isSafe(item.staticAlignY),
                staticY, staticY,
                false,
                containerOverflow.y.isScrollContainer()
            ));

            FloatSize scrollbarSize = scrollbarSize(item);

            Layout layout = new Layout(
                item.order,
                new FloatPoint(x, y),
                finalSize,
                output.contentSize(),
                output.scrollableOverflow(),
                scrollbarSize,
                item.border,
                item.padding,
                resolvedMargin
            );

            tree.setUnroundedLayout(item.nodeId, layout);

            // The child's contribution is expressed in the container's client-area frame.
            accumulatedOverflow = ScrollableOverflowUtil.union(
                accumulatedOverflow,
                ScrollableOverflowUtil.computeContribution(
                    new FloatPoint(x - areaInset.left, y - areaInset.top),
                    finalSize,
                    layout.scrollableOverflow().rect(),
                    item.border,
                    item.overflow,
                    overflowContainer,
                    FloatRect.zero()
                )
            );

            // The legacy content size keeps upstream's pre-#1118 out-of-flow rule: measured from
            // the positioning area (client area), mirrored from its right edge in RTL.
            FloatPoint contributionLocation = overflowContainer.reversedX()
                ? new FloatPoint(
                    areaWidth - (x - areaInset.left) - finalSize.width,
                    y - areaInset.top
                )
                : new FloatPoint(x - areaInset.left, y - areaInset.top);
            contentSize = ContentSizeUtil.max(contentSize,
                ContentSizeUtil.computeContentSizeContribution(
                    contributionLocation,
                    finalSize,
                    output.contentSize() != null ? output.contentSize() : finalSize,
                    item.overflow
                ));
        }

        return new OutOfFlowOverflow(accumulatedOverflow, contentSize);
    }

    /**
     * Resolves the auto margin size for one axis of an absolutely positioned box.
     *
     * <p>Only the free space left by the box's <em>used</em> (clamped) size matters: when both
     * margins are auto and the box already fills or overflows the space, they resolve to 0;
     * otherwise the free space is split evenly (a negative value is possible and allowed when the
     * box is larger than the containing block). Judging this by the box's declared size got
     * centring wrong whenever {@code max-width}/{@code max-height} clamped it.
     * BK-07 / upstream #1096.</p>
     */
    private static float calcAutoMarginSize(float marginOpt, float marginOpt1, float freeSpace) {
        float autoMarginSizeX;
        {
            int autoMarginCountX = (Float.isNaN(marginOpt) ? 1 : 0) + (Float.isNaN(marginOpt1) ? 1 : 0);
            // If both margins are auto and the box fills the free space, set auto margins to 0
            if (autoMarginCountX == 2 && freeSpace <= 0f) {
                autoMarginSizeX = 0f;
            } else if (autoMarginCountX > 0) {
                // Allow negative margins when child is larger than parent
                autoMarginSizeX = freeSpace / autoMarginCountX;
            } else {
                autoMarginSizeX = 0f;
            }
        }
        return autoMarginSizeX;
    }

    /**
     * Resolves the block-axis offset that align-content applies to a block container's in-flow
     * content.
     *
     * <p>The whole in-flow stack is a single alignment subject, so the distribution keywords use
     * their single-subject fallbacks before the offset is computed (css-align-3 §5.3), and the
     * keyword/offset math is exactly the shared model in {@link AlignmentHelper} (upstream
     * #959 / #952):</p>
     *
     * <ul>
     *   <li>an explicit {@code NORMAL} resolves to {@code START} in block layout (it is the
     *       layout-mode default for a block container), while the legacy
     *       {@link AlignContent#AUTO} sentinel is filtered out by the caller and never reaches
     *       this method;</li>
     *   <li>{@code stretch} and {@code space-between} fall back to {@code flex-start} (offset 0)
     *       for the single subject, and {@code space-around}/{@code space-evenly} fall back to
     *       {@code center} when there is free space;</li>
     *   <li>a {@code SAFE_*} value that overflows (non-positive free space) falls back to
     *       {@code START}, so it packs at the start edge instead of receiving a negative offset,
     *       whereas the unsafe positional keywords stay unsafe and may overflow;</li>
     *   <li>the block axis has no flex direction, so {@code flex-start}/{@code flex-end} coincide
     *       with start/end (layoutIsFlexReversed is false).</li>
     * </ul>
     */
    private static float blockAlignContentOffset(AlignContent alignContent, float freeSpace) {
        // normal is layout-mode dependent: a block container aligns its content to start (unlike
        // flex/grid, where it behaves as stretch).
        AlignContent resolved = AlignmentHelper.resolveNormal(alignContent, AlignContent.START);
        AlignContent keyword = AlignmentHelper.applyAlignmentFallback(resolved, freeSpace, 1);
        return AlignmentHelper.computeAlignmentOffset(freeSpace, 1, 0f, keyword, false, true);
    }

    /**
     * Resolves the block-axis {@code stretch} sizing keyword for an item's height.
     *
     * <p>The stretch size is the containing block's content-box height (the percentage
     * resolution height already adjusted for the container's content-box inset, BK-08), minus
     * the item's block-axis margins. A margin is treated as zero when it could collapse through
     * the corresponding edge of the containing block, i.e. when that edge is flagged in
     * {@code marginsTreatedAsZero}: the containing block has no border or padding on that side,
     * does not establish a new BFC and its margins may collapse with its children's.</p>
     *
     * <p>Every other sizing keyword ({@code min-content}, {@code max-content}, {@code fit-content})
     * is equal to the content size in the block axis, which is what an {@code auto} height already
     * resolves to, so this returns {@code NaN} for them (the caller then leaves the height
     * indefinite). The same applies when the containing block's content-box height is not known.
     * Upstream #1242 (6a36efbe), CSS Align / w3c/csswg-drafts#11044.</p>
     *
     * @param heightStyle the item's raw style height
     * @param containerPercentageResolutionHeight the containing block's content-box height, or
     *        {@link Float#NaN} when it is indefinite
     * @param marginTop the item's resolved non-auto top margin
     * @param marginBottom the item's resolved non-auto bottom margin
     * @param marginsTreatedAsZero which block-axis margins adjoin the containing block's edges
     * @return the stretch height, or {@link Float#NaN} when the height is not a resolvable
     *         {@code stretch} keyword in this context
     */
    private static float resolveStretchHeight(
        TaffyDimension heightStyle,
        float containerPercentageResolutionHeight,
        float marginTop,
        float marginBottom,
        TaffyLine<Boolean> marginsTreatedAsZero) {
        if (heightStyle == null || !heightStyle.isStretch()) return NaN;
        if (Float.isNaN(containerPercentageResolutionHeight)) return NaN;
        float height = containerPercentageResolutionHeight;
        if (!marginsTreatedAsZero.start) height -= marginTop;
        if (!marginsTreatedAsZero.end) height -= marginBottom;
        return height;
    }

    // === Out-of-flow self-alignment (OF-05 / OF-06, upstream #1206 and #1267) ===

    /** The physical edge a resolved self-alignment selects in the absolutely positioned model. */
    private enum OofEdge {
        START, CENTER, END
    }

    /** An inset-modified containing block (or static-position rectangle) in one axis. */
    private record OofInterval(float start, float end) {
    }

    /**
     * One axis of the absolute positioning model (upstream {@code oof.rs} {@code OofAxis}),
     * reduced to what the direct-parent port needs.
     *
     * <p>All coordinates are physical (left-to-right / top-to-bottom) and relative to the start
     * of the containing block's inset-resolution area. A {@link Float#NaN} inset or margin is
     * {@code auto}.</p>
     *
     * @param cbSize         the size of the containing block in this axis
     * @param insetStart     the resolved start inset ({@code NaN} = auto)
     * @param insetEnd       the resolved end inset ({@code NaN} = auto)
     * @param size           the box's border-box size in this axis
     * @param marginStart    the resolved start margin (auto margins already absorbed)
     * @param marginEnd      the resolved end margin
     * @param autoStartMargin whether the style margin on the start side was auto
     * @param autoEndMargin  whether the style margin on the end side was auto
     * @param alignment      the resolved self alignment of the axis ({@code null} = normal)
     * @param safe           whether the alignment carries the {@code safe} modifier
     * @param staticStart    the static-position rectangle start, in the area frame
     * @param staticEnd      the static-position rectangle end, in the area frame
     * @param reversed       whether the logical start edge is the physical end edge (RTL inline)
     * @param scrollAxis     whether the containing block scrolls in this axis
     */
    private record AbsoluteAxis(
        float cbSize,
        float insetStart,
        float insetEnd,
        float size,
        float marginStart,
        float marginEnd,
        boolean autoStartMargin,
        boolean autoEndMargin,
        AlignItems alignment,
        boolean safe,
        float staticStart,
        float staticEnd,
        boolean reversed,
        boolean scrollAxis
    ) {
        boolean hasStartInset() {
            return !Float.isNaN(insetStart);
        }

        boolean hasEndInset() {
            return !Float.isNaN(insetEnd);
        }

        boolean bothInsets() {
            return hasStartInset() && hasEndInset();
        }
    }

    /**
     * The static-position anchor of a zero-sized box aligned within an inline static-position
     * rectangle, mirroring upstream {@code AxisStaticPosition::from_alignment}: {@code stretch},
     * {@code baseline} and {@code normal} behave as start for static positioning purposes, and
     * start/end swap physical edges when the logical start edge is the physical end edge.
     */
    private static float staticAlignmentEdge(float areaStart, float areaEnd, AlignItems alignment, boolean axisIsRtl) {
        return switch (alignmentEdge(alignment, axisIsRtl)) {
            case START -> areaStart;
            case CENTER -> (areaStart + areaEnd) / 2f;
            case END -> areaEnd;
        };
    }

    /**
     * The physical edge selected by a self-alignment value. {@code reversed} is true when the
     * logical start edge of the containing block is the physical end edge (the inline axis of an
     * RTL containing block). An unset value behaves as start, which is what upstream
     * {@code OofAlignment::from_style} derives from an absent alignment.
     */
    private static OofEdge alignmentEdge(AlignItems alignment, boolean reversed) {
        AlignItems keyword = AlignmentHelper.keyword(alignment);
        if (keyword == null) {
            keyword = AlignItems.START;
        }
        return switch (keyword) {
            case CENTER -> OofEdge.CENTER;
            case END, FLEX_END -> reversed ? OofEdge.START : OofEdge.END;
            // start / flex-start / baseline / stretch / normal
            default -> reversed ? OofEdge.END : OofEdge.START;
        };
    }

    /** The physical edge corresponding to the logical start edge of the containing block. */
    private static OofEdge startEdge(boolean reversed) {
        return reversed ? OofEdge.END : OofEdge.START;
    }

    /** Positions a box of {@code size} within {@code area} on the given physical edge. */
    private static float alignInLine(OofInterval area, float size, OofEdge edge) {
        return switch (edge) {
            case START -> area.start();
            case CENTER -> (area.start() + area.end() - size) / 2f;
            case END -> area.end() - size;
        };
    }

    /**
     * Whether an auto size in this axis is the stretch-fit size rather than the fit-content size
     * (upstream {@code OofAxis::is_stretch_sized}): only a box inset-constrained in the axis can
     * stretch, and only when it asks to ({@code stretch}) or is a non-replaced box whose alignment
     * is {@code normal}. Table boxes are not distinguished in this port.
     */
    private static boolean isStretchSized(AlignItems alignment, float insetStart, float insetEnd, boolean isReplaced) {
        if (Float.isNaN(insetStart) || Float.isNaN(insetEnd)) {
            return false;
        }
        AlignItems keyword = AlignmentHelper.keyword(alignment);
        if (keyword == AlignItems.STRETCH) {
            return true;
        }
        return (keyword == null || keyword == AlignItems.NORMAL) && !isReplaced;
    }

    /** Whether an unset self alignment (or an explicit {@code normal}) behaves as the default. */
    private static boolean isNormalAlignment(AlignItems alignment) {
        AlignItems keyword = AlignmentHelper.keyword(alignment);
        return keyword == null || keyword == AlignItems.NORMAL;
    }

    /**
     * Computes the border-box start of one axis of an absolutely positioned box
     * (<a href="https://www.w3.org/TR/css-position-3/#abspos-alignment">css-position-3 abspos</a>),
     * including auto-margin absorption and the {@code safe} / default overflow-position rules.
     *
     * @param axis the resolved inputs of the axis
     * @return the offset of the box's border-box start from the start of the inset-resolution area
     */
    private static float absoluteAxisOffset(AbsoluteAxis axis) {
        float marginBox = axis.size() + axis.marginStart() + axis.marginEnd();
        if (!axis.hasStartInset() && !axis.hasEndInset()) {
            // Both insets auto: static alignment within the static-position rectangle. The IMCB is
            // derived from that rectangle and the physical edge the alignment selects
            // (css-position-3 resolving-insets), and an explicit 'safe' alignment whose margin box
            // overflows it falls back to the containing block's physical start edge. The default
            // (auto-safety) modifier deliberately does not fall back here: it keeps static
            // alignment unchanged. Upstream #1206 (a4dff69e), refined by #1267 (5f6ec1d4).
            OofEdge keyword = alignmentEdge(axis.alignment(), axis.reversed());
            OofInterval staticArea = new OofInterval(axis.staticStart(), axis.staticEnd());
            OofInterval imcb = staticPositionImcb(staticArea, axis.cbSize(), keyword, axis.reversed());
            if (axis.safe() && marginBox > imcb.end() - imcb.start()) {
                return alignInLine(imcb, marginBox, startEdge(axis.reversed())) + axis.marginStart();
            }
            return alignInLine(staticArea, marginBox, keyword) + axis.marginStart();
        }

        OofInterval imcb;
        if (axis.bothInsets()) {
            float start = axis.insetStart();
            float end = axis.cbSize() - axis.insetEnd();
            // Over-constrained insets: the weaker (end) inset is reduced so that the IMCB is
            // empty rather than negative (css-position-3 resolving-insets).
            imcb = new OofInterval(start, Math.max(start, end));
        } else if (axis.hasStartInset()) {
            imcb = new OofInterval(axis.insetStart(), axis.cbSize());
        } else {
            imcb = new OofInterval(0f, axis.cbSize() - axis.insetEnd());
        }

        if (!axis.bothInsets()) {
            // A single inset anchors the margin box to the stronger inset's edge.
            return axis.hasStartInset()
                   ? imcb.start() + axis.marginStart()
                   : imcb.end() - axis.size() - axis.marginEnd();
        }
        if (axis.autoStartMargin() || axis.autoEndMargin()) {
            // Auto margins have already absorbed the free space (only with both insets set).
            return imcb.start() + axis.marginStart();
        }

        float imcbSize = imcb.end() - imcb.start();
        OofEdge edge = alignmentEdge(axis.alignment(), axis.reversed());
        if (axis.safe()) {
            OofEdge effective = marginBox > imcbSize ? startEdge(axis.reversed()) : edge;
            return alignInLine(imcb, marginBox, effective) + axis.marginStart();
        }
        if (marginBox <= imcbSize || isNormalAlignment(axis.alignment())) {
            // 'normal' is start-aligned (as specified) even when the box overflows.
            return alignInLine(imcb, marginBox, edge) + axis.marginStart();
        }
        // Default overflow alignment for absolutely positioned boxes: keep the box as close to the
        // requested alignment as possible without overflowing the overflow-limit rectangle (the
        // union of the IMCB and the containing block, unbounded on the scrollable side of a scroll
        // container). https://www.w3.org/TR/css-align-3/#auto-safety-position
        float limitStart = Math.min(imcb.start(), 0f);
        float limitEnd = Math.max(imcb.end(), axis.cbSize());
        if (axis.scrollAxis()) {
            if (axis.reversed()) {
                limitStart = Float.NEGATIVE_INFINITY;
            } else {
                limitEnd = Float.POSITIVE_INFINITY;
            }
        }
        if (marginBox <= limitEnd - limitStart) {
            float start = alignInLine(imcb, marginBox, edge);
            return Math.max(Math.min(start, limitEnd - marginBox), limitStart) + axis.marginStart();
        }
        return alignInLine(new OofInterval(limitStart, limitEnd), marginBox, startEdge(axis.reversed()))
               + axis.marginStart();
    }

    /**
     * The inset-modified containing block of an axis whose insets are both {@code auto},
     * mirroring upstream {@code OofAxis::inset_modified_containing_block} for the static-position
     * case (css-position-3 <em>resolving insets</em>).
     *
     * <p>The static-position rectangle and the physical edge the resolved self-alignment selects
     * derive the line: a start alignment takes the static rectangle's start and the containing
     * block's end, an end alignment the containing block's start and the static rectangle's end,
     * and a center alignment the line centered on the static rectangle's center and bounded by the
     * nearer containing-block edge. An over-constrained result collapses towards the containing
     * block's logical end edge, like the generic rule.</p>
     *
     * @param staticArea the static-position rectangle in the containing-block frame
     * @param cbSize     the containing block size in this axis
     * @param keyword    the physical edge selected by the resolved alignment
     * @param reversed   whether the logical start edge is the physical end edge (RTL inline axis)
     * @return the derived IMCB in the containing-block frame
     */
    private static OofInterval staticPositionImcb(
        OofInterval staticArea, float cbSize, OofEdge keyword, boolean reversed) {
        OofInterval imcb = switch (keyword) {
            case START -> new OofInterval(staticArea.start(), cbSize);
            case END -> new OofInterval(0f, staticArea.end());
            case CENTER -> {
                float center = (staticArea.start() + staticArea.end()) / 2f;
                float startDistance = center;
                float endDistance = cbSize - center;
                yield Math.abs(startDistance) <= Math.abs(endDistance)
                      ? new OofInterval(0f, 2f * startDistance)
                      : new OofInterval(cbSize - 2f * endDistance, cbSize);
            }
        };
        if (imcb.end() < imcb.start()) {
            // Over-constrained: the weaker inset is the logical end edge (the physical end edge in
            // LTR, the physical start edge in RTL), which is reduced until the IMCB is empty.
            return startEdge(reversed) == OofEdge.START
                   ? new OofInterval(imcb.start(), imcb.start())
                   : new OofInterval(imcb.end(), imcb.end());
        }
        return imcb;
    }

    // Helper methods

    /**
     * Whether a margin component's used value depends on the containing block's content-box width.
     *
     * <p>Only a {@code percent} component scales with that width and only a {@code calc()}
     * expression can embed one, so those two stay unresolved until layout. A {@code length} is its
     * own value and {@code auto}/intrinsic keywords resolve to nothing, both independent of the
     * basis. {@code CalcExpression} has no basis-dependence query (it only exposes
     * {@code resolve(float)}), so every {@code calc()} is conservatively treated as dependent.</p>
     */
    private static boolean marginNeedsInnerWidth(LengthPercentageAuto margin) {
        return margin.isPercent() || margin.isCalc();
    }

    /**
     * The basis-independent value of a margin component: the length itself, or {@link Float#NaN}
     * for {@code auto} and the intrinsic sizing keywords, which are exactly the values
     * {@link LengthPercentageAuto#maybeResolve(float)} returns for any basis.
     */
    private static float staticMarginValue(LengthPercentageAuto margin) {
        return margin.isLength() ? margin.getValue() : NaN;
    }

    /**
     * Resolves a padding/border rectangle against the container's content-box width, returning the
     * shared {@link #ZERO_RECT} when every side is a zero length.
     *
     * <p>A style without padding or border stores four shared zero lengths, so this is the common
     * case and avoids one rectangle per item per side set.</p>
     */
    private static FloatRect resolveZeroOrRect(TaffyRect<LengthPercentage> rect, float contextWidth) {
        if (isZeroLength(rect.left) && isZeroLength(rect.right)
            && isZeroLength(rect.top) && isZeroLength(rect.bottom)) {
            return ZERO_RECT;
        }
        return Resolve.resolveRectOrZero(rect, contextWidth);
    }

    private static boolean isZeroLength(LengthPercentage value) {
        return value == LengthPercentage.ZERO || (value.isLength() && value.getValue() == 0f);
    }

    /**
     * The item's scrollbar size, or the shared {@link #ZERO_SIZE} when neither axis reserves a
     * gutter.
     *
     * <p>Most boxes do not scroll, so the common case avoids one {@link FloatSize} per reported
     * {@link Layout}. The constant is the same read-only one this class already reports as a
     * zero padding/border sum. P18.</p>
     */
    private static FloatSize scrollbarSize(BlockItem item) {
        float width = item.overflow.y == Overflow.SCROLL ? item.scrollbarWidth : 0f;
        float height = item.overflow.x == Overflow.SCROLL ? item.scrollbarWidth : 0f;
        return width == 0f && height == 0f ? ZERO_SIZE : new FloatSize(width, height);
    }

    /**
     * Sums an item's resolved padding and border, returning the shared {@link #ZERO_SIZE} when the
     * result is zero on both axes (the common case, and the result is only ever read).
     */
    private static FloatSize paddingBorderSum(FloatRect padding, FloatRect border) {
        float width = padding.left + padding.right + border.left + border.right;
        float height = padding.top + padding.bottom + border.top + border.bottom;
        return width == 0f && height == 0f ? ZERO_SIZE : new FloatSize(width, height);
    }

    /**
     * Resolves an item's size/min-size/max-size against the container's content box, returning the
     * shared {@link #NONE_SIZE} when neither axis can resolve in any basis.
     *
     * <p>{@code auto} and the intrinsic sizing keywords resolve to {@link Float#NaN} regardless of
     * the basis ({@link TaffyDimension#maybeResolve}), which is the common case for a block child
     * without an explicit size constraint; a percentage or {@code calc()} expression does depend on
     * the basis and always allocates.</p>
     */
    private static FloatSize resolveSizeOrNone(TaffySize<TaffyDimension> size, FloatSize parentSize) {
        if (resolvesToNothing(size.width) && resolvesToNothing(size.height)) {
            return NONE_SIZE;
        }
        return Resolve.maybeResolveSize(size, parentSize);
    }

    private static boolean resolvesToNothing(TaffyDimension dimension) {
        return switch (dimension.getType()) {
            case AUTO, MIN_CONTENT, MAX_CONTENT, FIT_CONTENT, STRETCH, CONTENT -> true;
            case LENGTH, PERCENT, CALC -> false;
        };
    }

    /**
     * Adds a box-sizing adjustment to a resolved size.
     *
     * <p>With the default {@code border-box} sizing the adjustment is the shared all-zero size,
     * and the result is numerically identical to the input, so the input is returned as-is. The
     * sizes handed around by this class are never mutated after construction, which makes that
     * aliasing safe and saves one allocation per size per item.</p>
     */
    private static FloatSize addBoxSizingAdjustment(FloatSize size, FloatSize adjustment) {
        if (adjustment.width == 0f && adjustment.height == 0f) {
            return size;
        }
        return new FloatSize(
            TaffyMath.maybeAdd(size.width, adjustment.width),
            TaffyMath.maybeAdd(size.height, adjustment.height)
        );
    }

    /**
     * Clamps a size by its optional min/max constraints, returning the input unchanged when both
     * constraints are absent (the common case for a style without {@code min-*}/{@code max-*}).
     */
    private static FloatSize clampIfConstrained(FloatSize size, FloatSize min, FloatSize max) {
        if (Float.isNaN(min.width) && Float.isNaN(min.height)
            && Float.isNaN(max.width) && Float.isNaN(max.height)) {
            return size;
        }
        return new FloatSize(
            TaffyMath.maybeClamp(size.width, min.width, max.width),
            TaffyMath.maybeClamp(size.height, min.height, max.height)
        );
    }

    private FloatSize maybeAdd(FloatSize size, FloatSize addition) {
        return new FloatSize(
            TaffyMath.maybeAdd(size.width, addition.width),
            TaffyMath.maybeAdd(size.height, addition.height)
        );
    }

    private FloatSize maybeClamp(FloatSize size, FloatSize min, FloatSize max) {
        return new FloatSize(
            TaffyMath.maybeClamp(size.width, min.width, max.width),
            TaffyMath.maybeClamp(size.height, min.height, max.height)
        );
    }

    private FloatSize maybeMax(FloatSize size, FloatSize min) {
        return new FloatSize(
            TaffyMath.maybeMax(size.width, min.width),
            TaffyMath.maybeMax(size.height, min.height)
        );
    }

    private FloatSize orChain(FloatSize... sizes) {
        float width = NaN;
        float height = NaN;
        for (FloatSize size : sizes) {
            if (Float.isNaN(width) && !Float.isNaN(size.width)) width = size.width;
            if (Float.isNaN(height) && !Float.isNaN(size.height)) height = size.height;
            if (!Float.isNaN(width) && !Float.isNaN(height)) break;
        }
        return new FloatSize(width, height);
    }

    private AvailableSpace subtractFromAvailable(AvailableSpace available, float value) {
        if (available.isDefinite()) {
            return AvailableSpace.definite(Math.max(0, available.getValue() - value));
        }
        return available;
    }

    /**
     * Resolves an item's inline-axis margin sum against {@code contextWidth}, returning zero for
     * every component that resolves to nothing.
     *
     * <p>Reads the pre-resolved primitives for a side whose value cannot depend on a basis (a
     * length, {@code auto} or an intrinsic keyword); only a percent or calc() side resolves here.
     * The result equals the {@code resolveOrZero} sum of the style values, with {@code auto}
     * counting as zero. P18.</p>
     */
    private static float resolveInlineMarginSum(BlockItem item, float contextWidth) {
        float left = item.marginLeftNeedsInnerWidth
                     ? item.margin.left.resolveOrZero(contextWidth)
                     : (Float.isNaN(item.marginLeft) ? 0f : item.marginLeft);
        float right = item.marginRightNeedsInnerWidth
                      ? item.margin.right.resolveOrZero(contextWidth)
                      : (Float.isNaN(item.marginRight) ? 0f : item.marginRight);
        return left + right;
    }
}
