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
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TextAlign;
import dev.vfyjxf.taffy.util.AlignmentHelper;
import dev.vfyjxf.taffy.util.BaselineHelper;
import dev.vfyjxf.taffy.util.ContentSizeUtil;
import dev.vfyjxf.taffy.util.Resolve;
import dev.vfyjxf.taffy.util.ScrollableOverflowUtil;
import dev.vfyjxf.taffy.util.TaffyMath;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Float.NaN;

/**
 * Computes block layout for nodes with display: block.
 */
public class BlockComputer {

    private final LayoutComputer layoutComputer;

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
        FloatSize size;
        FloatSize minSize;
        FloatSize maxSize;
        TaffyPoint<Overflow> overflow;
        float scrollbarWidth;
        TaffyPosition position;
        TaffyRect<LengthPercentageAuto> inset;
        TaffyRect<LengthPercentageAuto> margin;
        FloatRect padding;
        FloatRect border;
        FloatSize paddingBorderSum;
        FloatSize computedSize;
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
         * Pending layout of an in-flow child. Held back from {@code setUnroundedLayout} so that a
         * measurement pass ({@link RunMode#COMPUTE_SIZE}) can return before any tree state is
         * written; the final layout commits them in one place. Upstream #971 (BK-01).
         */
        Layout finalLayout;
    }

    /**
     * Result of performing final layout on in-flow children, including margin collapse info.
     */
    private record InFlowLayoutResult(
        float contentHeight, CollapsibleMarginSet firstChildTopMarginSet,
        CollapsibleMarginSet lastChildBottomMarginSet,
        boolean allChildrenCanBeCollapsedThrough,
        float firstBaseline
    ) {}

    /**
     * Computes block layout for a node.
     */
    public LayoutOutput compute(NodeId node, LayoutInput inputs, TaffyStyle style) {
        TaffyTree tree = layoutComputer.getTree();
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

        FloatSize paddingBorderSize = new FloatSize(
            padding.left + padding.right + border.left + border.right,
            padding.top + padding.bottom + border.top + border.bottom
        );

        FloatSize boxSizingAdjustment = style.getBoxSizing() == BoxSizing.CONTENT_BOX
                                        ? paddingBorderSize
                                        : new FloatSize(0f, 0f);

        FloatSize size2 = Resolve.maybeResolveSize(style.getSize(), parentSize);
        FloatSize sizeStyle = Resolve.maybeApplyAspectRatio(size2, aspectRatio);
        sizeStyle = maybeAdd(sizeStyle, boxSizingAdjustment);

        FloatSize size1 = Resolve.maybeResolveSize(style.getMinSize(), parentSize);
        FloatSize minSize = Resolve.maybeApplyAspectRatio(size1, aspectRatio);
        minSize = maybeAdd(minSize, boxSizingAdjustment);

        FloatSize size = Resolve.maybeResolveSize(style.getMaxSize(), parentSize);
        FloatSize maxSize = Resolve.maybeApplyAspectRatio(size, aspectRatio);
        maxSize = maybeAdd(maxSize, boxSizingAdjustment);

        // A `display: flow-root` box always establishes a new block formatting context. The part
        // of that behaviour this port can express is margin isolation: the box's own margins no
        // longer adjoin its children's, so nothing collapses with or through its block edges.
        // (Containing floats and avoiding a sibling's floats are the other two effects of the
        // new BFC; this port has no float layout yet — see the float work package F1/P7.)
        // BK-17 / upstream #997 (10004cea).
        boolean establishesNewBlockFormattingContext =
            style.getDisplay() == TaffyDisplay.FLOW_ROOT;

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

        // css-sizing-4: a definite width plus an `aspect-ratio` makes the height definite. This
        // must be resolved *before* the content box size and the item list are derived, otherwise
        // the children are laid out against an unknown height (so their percentage heights resolve
        // to nothing and a descendant grid's auto rows collapse), and the container's own height is
        // only corrected afterwards.
        //
        // Only the height is derived: the width of a block-level box comes from the parent's
        // stretch/known width or from the style, and re-deriving a width from a style height would
        // override a flex item's flex-resolved main size (the Java generated flex aspect-ratio
        // fixtures pin that browser behaviour). The derived height is clamped by min/max and by the
        // padding+border sum, exactly like the intrinsic-height path below.
        // BK-15(a) / upstream #965 (8609e92f).
        if (runMode == RunMode.PERFORM_LAYOUT && !Float.isNaN(aspectRatio)
            && Float.isNaN(styledBasedKnownDimensions.height)
            && !Float.isNaN(styledBasedKnownDimensions.width)) {
            // Aspect ratio applies to the content box: convert the border-box width into the
            // content box before dividing, then convert the derived content height back.
            float arDerivedHeight = (styledBasedKnownDimensions.width - boxSizingAdjustment.width) / aspectRatio
                                    + boxSizingAdjustment.height;
            float arHeight = TaffyMath.clamp(arDerivedHeight, minSize.height, maxSize.height);
            arHeight = Math.max(arHeight, !Float.isNaN(paddingBorderSize.height) ? paddingBorderSize.height : 0);
            styledBasedKnownDimensions = new FloatSize(styledBasedKnownDimensions.width, arHeight);
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

        // Generate item list
        List<BlockItem> items = generateItemList(node, containerContentBoxSize);

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

        TaffyDirection direction = layoutComputer.resolveDirection(node);

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
            childrenMarginsAdjoinOwnEdges
        );

        float containerOuterHeight = styledBasedKnownDimensions.height;
        if (Float.isNaN(containerOuterHeight)) {
            // This branch is only reached when the height is auto. In a final layout an
            // aspect-ratio derived height was already resolved above (so the height is definite
            // and this branch is skipped); during a measurement the aspect ratio is instead
            // folded into the content-based height, which is the legacy (content-inclusive)
            // measurement that flex/grid intrinsic sizing has been verified against.
            float contentHeight = layoutResult.contentHeight;
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
                for (BlockItem item : items) {
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
        for (BlockItem item : items) {
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
        FloatSize inFlowContentSize = FloatSize.zero();
        for (BlockItem item : items) {
            Layout childLayout = item.finalLayout;
            // Out-of-flow children were accumulated by the absolute pass above.
            if (childLayout == null) continue;

            FloatRect childMargin = childLayout.margin();
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
                    new FloatRect(
                        nonPositive(childMargin.left),
                        nonPositive(childMargin.right),
                        nonPositive(childMargin.top),
                        nonPositive(childMargin.bottom)
                    )
                )
            );

            inFlowContentSize = ContentSizeUtil.max(inFlowContentSize,
                contentSizeContribution(childLayout, item.overflow, rtl, containerOuterWidth,
                    resolvedBorder, scrollbarGutter));
        }

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

    private List<BlockItem> generateItemList(NodeId node, FloatSize nodeInnerSize) {
        TaffyTree tree = layoutComputer.getTree();
        List<BlockItem> items = new ArrayList<>();

        int order = 0;
        for (NodeId childId : tree.getChildren(node)) {
            TaffyStyle childStyle = tree.getStyle(childId);
            if (childStyle.getBoxGenerationMode() == BoxGenerationMode.NONE) {
                order++;
                continue;
            }

            BlockItem item = new BlockItem();
            item.nodeId = childId;
            item.order = order++;

            Float aspectRatio = childStyle.getAspectRatio();
            FloatRect itemPadding = Resolve.resolveRectOrZero(childStyle.getPadding(), nodeInnerSize.width);
            FloatRect itemBorder = Resolve.resolveRectOrZero(childStyle.getBorder(), nodeInnerSize.width);
            item.padding = itemPadding;
            item.border = itemBorder;
            item.paddingBorderSum = new FloatSize(
                itemPadding.left + itemPadding.right + itemBorder.left + itemBorder.right,
                itemPadding.top + itemPadding.bottom + itemBorder.top + itemBorder.bottom
            );

            FloatSize boxSizingAdj = childStyle.getBoxSizing() == BoxSizing.CONTENT_BOX
                                     ? item.paddingBorderSum
                                     : new FloatSize(0f, 0f);

            FloatSize size2 = Resolve.maybeResolveSize(childStyle.getSize(), nodeInnerSize);
            item.size = maybeAdd(Resolve.maybeApplyAspectRatio(size2, aspectRatio), boxSizingAdj);
            FloatSize size1 = Resolve.maybeResolveSize(childStyle.getMinSize(), nodeInnerSize);
            item.minSize = maybeAdd(Resolve.maybeApplyAspectRatio(size1, aspectRatio), boxSizingAdj);
            FloatSize size = Resolve.maybeResolveSize(childStyle.getMaxSize(), nodeInnerSize);
            item.maxSize = maybeAdd(Resolve.maybeApplyAspectRatio(size, aspectRatio), boxSizingAdj);

            item.overflow = childStyle.getOverflow();
            item.scrollbarWidth = childStyle.getScrollbarWidth();
            item.position = childStyle.getPosition();
            item.inset = childStyle.getInset();
            item.margin = childStyle.getMargin();
            item.heightStyle = childStyle.getSize().height;
            item.computedSize = new FloatSize(0f, 0f);
            item.staticPosition = new FloatPoint(0f, 0f);
            item.canBeCollapsedThrough = false;
            item.isTable = childStyle.getItemIsTable();
            item.isCompressibleReplaced = childStyle.isCompressibleReplaced();
            item.justifySelf = childStyle.justifySelf;
            item.alignSelf = childStyle.getAlignSelf();
            item.direction = layoutComputer.resolveDirection(childId);
            item.widthIsAuto = childStyle.getSize().width.isAuto();
            // For compressible replaced items a percentage width/max-width resolves against a
            // zero-width containing block in the min-content contribution, so the item contributes
            // at most this cap. Upstream #1401 (4aca4f49).
            if (item.isCompressibleReplaced) {
                FloatSize resolvedWidth = Resolve.maybeResolveSize(
                    childStyle.getSize(), new FloatSize(0f, NaN));
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

        return items;
    }

    private float determineContentBasedContainerWidth(List<BlockItem> items, AvailableSpace availableWidth) {
        float maxChildWidth = 0f;

        for (BlockItem item : items) {
            if (item.position.isOutOfFlow()) continue;

            FloatSize knownDimensions = maybeClamp(item.size, item.minSize, item.maxSize);

            float width;
            if (!Float.isNaN(knownDimensions.width)) {
                width = knownDimensions.width;
            } else {
                float marginSum = resolveMarginSumOrZero(item.margin, availableWidth.isDefinite() ? availableWidth.getValue() : NaN);
                AvailableSpace adjustedAvailable = subtractFromAvailable(availableWidth, marginSum);

                LayoutOutput output = layoutComputer.performChildLayout(
                    item.nodeId,
                    knownDimensions,
                    new FloatSize(NaN, NaN),
                    new TaffySize<>(adjustedAvailable, AvailableSpace.minContent()),
                    SizingMode.INHERENT_SIZE,
                    new TaffyLine<>(true, true)
                );
                float measuredWidth = output.size().width;
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
            }
            width = Math.max(width, item.paddingBorderSum.width);
            maxChildWidth = Math.max(maxChildWidth, width);
        }

        return maxChildWidth;
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
        TaffyLine<Boolean> childrenMarginsAdjoinOwnEdges) {

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

        for (BlockItem item : items) {
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
            FloatRect itemMarginOpt = resolveMarginOptional(item.margin, containerInnerWidth);
            FloatRect itemNonAutoMargin = new FloatRect(
                Float.isNaN(itemMarginOpt.left) ? 0f : itemMarginOpt.left,
                Float.isNaN(itemMarginOpt.right) ? 0f : itemMarginOpt.right,
                Float.isNaN(itemMarginOpt.top) ? 0f : itemMarginOpt.top,
                Float.isNaN(itemMarginOpt.bottom) ? 0f : itemMarginOpt.bottom
            );
            float itemNonAutoXMarginSum = itemNonAutoMargin.left + itemNonAutoMargin.right;

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
            // Unless stretched, tables resolve their own width (CSS2 section 10.3.3): an
            // auto-width table is sized by its content, not by the containing block. A stretch
            // justify-self stretches such a box through the regular stretch path instead
            // (upstream #1250).
            if (item.isTable && !isStretchJustifySelf) {
                knownDimensions = new FloatSize(NaN, NaN);
            } else {
                float width;
                if (!Float.isNaN(item.size.width)) {
                    width = item.size.width;
                } else if (nonStretchJustifySelf != null && item.widthIsAuto) {
                    // "The automatic width of a block-level box whose justify-self is neither
                    // normal nor stretch is fit-content rather than stretch": measure the item
                    // under the available width, which is upstream's Dimension::fit_content()
                    // sizing-keyword resolution (upstream #1250).
                    float fitContentAvailable = Math.max(0f, containerInnerWidth - itemNonAutoXMarginSum);
                    FloatSize measured = layoutComputer.measureChildSize(
                        item.nodeId,
                        new FloatSize(NaN, NaN),
                        parentSize,
                        new TaffySize<>(
                            AvailableSpace.definite(fitContentAvailable),
                            AvailableSpace.maxContent()
                        ),
                        SizingMode.INHERENT_SIZE,
                        new TaffyLine<>(true, true)
                    );
                    width = Float.isNaN(measured.width) ? fitContentAvailable : measured.width;
                } else {
                    width = containerInnerWidth - itemNonAutoXMarginSum;
                }
                width = TaffyMath.clamp(width, item.minSize.width, item.maxSize.width);
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
                    itemNonAutoMargin,
                    childrenMarginsAdjoinOwnEdges
                );
                float resolvedHeight =
                    !Float.isNaN(item.size.height) ? item.size.height : keywordHeight;
                knownDimensions = maybeClamp(
                    new FloatSize(width, resolvedHeight),
                    item.minSize,
                    item.maxSize
                );
            }

            // The parent's run mode is propagated: measuring this container must not make its
            // children write layout state, and a vertical ComputeSize pass needs the children's
            // margin-collapse outputs. BK-02 / upstream #972.
            LayoutOutput itemOutput = layoutComputer.performChildLayout(
                item.nodeId,
                knownDimensions,
                parentSize,
                new TaffySize<>(
                    subtractFromAvailable(availableSpace.width, itemNonAutoXMarginSum),
                    availableSpace.height
                ),
                SizingMode.INHERENT_SIZE,
                new TaffyLine<>(true, true),
                runMode
            );

            FloatSize finalSize = itemOutput.size();

            // Get margin collapse info from child layout
            CollapsibleMarginSet topMarginSet = itemOutput.topMargin().copy()
                                                          .collapseWithMargin(!Float.isNaN(itemMarginOpt.top) ? itemMarginOpt.top : 0f);
            CollapsibleMarginSet bottomMarginSet = itemOutput.bottomMargin().copy()
                                                             .collapseWithMargin(!Float.isNaN(itemMarginOpt.bottom) ? itemMarginOpt.bottom : 0f);

            // Expand auto margins
            float freeXSpace = Math.max(0, containerInnerWidth - finalSize.width - itemNonAutoXMarginSum);
            int autoMarginCount = (item.margin.left.isAuto() ? 1 : 0) + (item.margin.right.isAuto() ? 1 : 0);
            float xAxisAutoMarginSize = autoMarginCount > 0 ? freeXSpace / autoMarginCount : 0;

            // Layout.margin reports the child's *own* margins. The collapsed sets above describe
            // where the box is placed relative to its siblings (a parent/child margin interaction),
            // not the box's own margin; reporting them leaked the parent's collapsed margin into
            // the child's margin. Auto margins report 0 here: the resolved auto size is only
            // consumed by the positioning maths. BK-10 / upstream #1287.
            FloatRect resolvedMargin = new FloatRect(
                Float.isNaN(itemMarginOpt.left) ? xAxisAutoMarginSize : itemMarginOpt.left,
                Float.isNaN(itemMarginOpt.right) ? xAxisAutoMarginSize : itemMarginOpt.right,
                Float.isNaN(itemMarginOpt.top) ? 0f : itemMarginOpt.top,
                Float.isNaN(itemMarginOpt.bottom) ? 0f : itemMarginOpt.bottom
            );

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
                float insetLeft = item.inset.left.maybeResolve(containerInnerWidth);
                float insetRight = item.inset.right.maybeResolve(containerInnerWidth);
                float insetTop = item.inset.top.maybeResolve(childPercentageResolutionHeight);
                float insetBottom = item.inset.bottom.maybeResolve(childPercentageResolutionHeight);
                insetOffsetX = !Float.isNaN(insetLeft) ? insetLeft : (!Float.isNaN(insetRight) ? -insetRight : 0f);
                insetOffsetY = !Float.isNaN(insetTop) ? insetTop : (!Float.isNaN(insetBottom) ? -insetBottom : 0f);
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

            item.computedSize = finalSize;
            item.canBeCollapsedThrough = itemOutput.marginsCanCollapseThrough();

            // Update static position for RTL
            float staticX = isRtl
                ? (containerOuterWidth - contentBoxInset.right)
                : contentBoxInset.left;
            item.staticPosition = new FloatPoint(
                staticX,
                committedYOffset + activeCollapsibleMarginSet.resolve()
            );

            float y = committedYOffset + insetOffsetY + yMarginOffset;

            // Calculate the x position. A non-stretch justify-self aligns the item's margin box
            // within its alignment container and takes precedence over legacy text-align, but
            // auto margins take precedence over both: they have already absorbed the free space,
            // and the historical auto-margin placement below is the result the spec expects for
            // that case (upstream #1250).
            float itemOuterWidth = finalSize.width + resolvedMargin.left + resolvedMargin.right;
            float freeSpace = containerInnerWidth - itemOuterWidth;
            boolean hasAutoXMargin = item.margin.left.isAuto() || item.margin.right.isAuto();
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

            FloatSize scrollbarSize = new FloatSize(
                item.overflow.y == Overflow.SCROLL ? item.scrollbarWidth : 0f,
                item.overflow.x == Overflow.SCROLL ? item.scrollbarWidth : 0f
            );

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
            firstBaseline
        );
    }

    /**
     * Resolve margins returning null for auto margins.
     */
    private FloatRect resolveMarginOptional(TaffyRect<LengthPercentageAuto> margin, float contextWidth) {
        return new FloatRect(
            margin.left.isAuto() ? NaN : margin.left.maybeResolve(contextWidth),
            margin.right.isAuto() ? NaN : margin.right.maybeResolve(contextWidth),
            margin.top.isAuto() ? NaN : margin.top.maybeResolve(contextWidth),
            margin.bottom.isAuto() ? NaN : margin.bottom.maybeResolve(contextWidth)
        );
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

        for (BlockItem item : items) {
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

            // Compute size from style
            FloatSize size2 = Resolve.maybeResolveSize(childStyle.getSize(), new FloatSize(areaWidth, areaHeight));
            FloatSize styleSize = maybeAdd(Resolve.maybeApplyAspectRatio(size2, aspectRatio), boxSizingAdj);
            FloatSize size1 = Resolve.maybeResolveSize(childStyle.getMinSize(), new FloatSize(areaWidth, areaHeight));
            FloatSize minSz = maybeAdd(Resolve.maybeApplyAspectRatio(size1, aspectRatio), boxSizingAdj);
            minSz = maybeMax(minSz, paddingBorderSum);
            FloatSize size = Resolve.maybeResolveSize(childStyle.getMaxSize(), new FloatSize(areaWidth, areaHeight));
            FloatSize maxSz = maybeAdd(Resolve.maybeApplyAspectRatio(size, aspectRatio), boxSizingAdj);

            FloatSize knownDimensions = maybeClamp(styleSize, minSz, maxSz);

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

            FloatSize scrollbarSize = new FloatSize(
                item.overflow.y == Overflow.SCROLL ? item.scrollbarWidth : 0f,
                item.overflow.x == Overflow.SCROLL ? item.scrollbarWidth : 0f
            );

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
     * @param itemMargin the item's resolved non-auto margins
     * @param marginsTreatedAsZero which block-axis margins adjoin the containing block's edges
     * @return the stretch height, or {@link Float#NaN} when the height is not a resolvable
     *         {@code stretch} keyword in this context
     */
    private static float resolveStretchHeight(
        TaffyDimension heightStyle,
        float containerPercentageResolutionHeight,
        FloatRect itemMargin,
        TaffyLine<Boolean> marginsTreatedAsZero) {
        if (heightStyle == null || !heightStyle.isStretch()) return NaN;
        if (Float.isNaN(containerPercentageResolutionHeight)) return NaN;
        float height = containerPercentageResolutionHeight;
        if (!marginsTreatedAsZero.start) height -= itemMargin.top;
        if (!marginsTreatedAsZero.end) height -= itemMargin.bottom;
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

    private float resolveMarginSumOrZero(TaffyRect<LengthPercentageAuto> margin, float contextWidth) {
        float left = margin.left.isAuto() ? 0f : margin.left.resolveOrZero(contextWidth);
        float right = margin.right.isAuto() ? 0f : margin.right.resolveOrZero(contextWidth);
        return left + right;
    }
}
