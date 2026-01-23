package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.BoxGenerationMode;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TextAlign;
import dev.vfyjxf.taffy.util.ContentSizeUtil;
import dev.vfyjxf.taffy.util.Resolve;
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
    }

    /**
     * Result of performing final layout on in-flow children, including margin collapse info.
     */
    private record InFlowLayoutResult(
        float contentHeight, CollapsibleMarginSet firstChildTopMarginSet,
        CollapsibleMarginSet lastChildBottomMarginSet,
        boolean allChildrenCanBeCollapsedThrough
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

        // Determine margin collapsing behaviour
        boolean ownMarginsCollapseWithChildrenStart =
            verticalMarginsAreCollapsible.start &&
            !style.getOverflow().x.isScrollContainer() &&
            !style.getOverflow().y.isScrollContainer() &&
            style.getPosition() == TaffyPosition.RELATIVE &&
            padding.top == 0 &&
            border.top == 0;

        boolean ownMarginsCollapseWithChildrenEnd =
            verticalMarginsAreCollapsible.end &&
            !style.getOverflow().x.isScrollContainer() &&
            !style.getOverflow().y.isScrollContainer() &&
            style.getPosition() == TaffyPosition.RELATIVE &&
            padding.bottom == 0 &&
            border.bottom == 0 &&
            Float.isNaN(sizeStyle.height);

        TaffyLine<Boolean> ownMarginsCollapseWithChildren =
            new TaffyLine<>(ownMarginsCollapseWithChildrenStart, ownMarginsCollapseWithChildrenEnd);

        boolean hasStylesPreventingBeingCollapsedThrough =
            !style.isBlock() ||
            style.getOverflow().x.isScrollContainer() ||
            style.getOverflow().y.isScrollContainer() ||
            style.getPosition() == TaffyPosition.ABSOLUTE ||
            (!Float.isNaN(style.getAspectRatio())) ||
            padding.top > 0 ||
            padding.bottom > 0 ||
            border.top > 0 ||
            border.bottom > 0 ||
            (!Float.isNaN(sizeStyle.height) && sizeStyle.height > 0) ||
            (!Float.isNaN(minSize.height) && minSize.height > 0);

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

        // Perform final layout on children
        FloatRect resolvedPadding = Resolve.resolveRectOrZero(style.getPadding(), containerOuterWidth);
        FloatRect resolvedBorder = Resolve.resolveRectOrZero(style.getBorder(), containerOuterWidth);
        FloatRect contentBoxInset = new FloatRect(
            resolvedPadding.left + resolvedBorder.left + scrollbarGutter.left,
            resolvedPadding.right + resolvedBorder.right + scrollbarGutter.right,
            resolvedPadding.top + resolvedBorder.top + scrollbarGutter.top,
            resolvedPadding.bottom + resolvedBorder.bottom + scrollbarGutter.bottom
        );

        InFlowLayoutResult layoutResult = performFinalLayoutOnChildren(
            items,
            containerOuterWidth,
            contentBoxInset,
            style.getTextAlign(),
            layoutComputer.resolveDirection(node),
            ownMarginsCollapseWithChildren
        );

        float containerOuterHeight = styledBasedKnownDimensions.height;
        if (Float.isNaN(containerOuterHeight)) {
            containerOuterHeight = TaffyMath.clamp(layoutResult.contentHeight, minSize.height, maxSize.height);
            containerOuterHeight = Math.max(containerOuterHeight, !Float.isNaN(paddingBorderSize.height) ? paddingBorderSize.height : 0);
        }

        FloatSize finalOuterSize = new FloatSize(containerOuterWidth, containerOuterHeight);

        if (runMode == RunMode.COMPUTE_SIZE) {
            return LayoutOutput.fromOuterSize(finalOuterSize);
        }

        // Layout absolutely positioned children
        // Absolute position inset is border + scrollbar gutter (not padding), per CSS spec
        FloatRect absolutePositionInset = new FloatRect(
            resolvedBorder.left + scrollbarGutter.left,
            resolvedBorder.right + scrollbarGutter.right,
            resolvedBorder.top + scrollbarGutter.top,
            resolvedBorder.bottom + scrollbarGutter.bottom
        );
        performAbsoluteLayoutOnChildren(items, finalOuterSize, absolutePositionInset);

        // Layout hidden children
        for (BlockItem item : items) {
            TaffyStyle childStyle = tree.getStyle(item.nodeId);
            if (childStyle.getBoxGenerationMode() == BoxGenerationMode.NONE) {
                tree.setUnroundedLayout(item.nodeId, Layout.withOrder(item.order));
                layoutComputer.computeChildLayout(item.nodeId, LayoutInput.hidden());
            }
        }

        FloatSize contentSize = computeContentSizeFromChildren(node);

        // Determine whether this node can be collapsed through
        boolean canBeCollapsedThrough = !hasStylesPreventingBeingCollapsedThrough &&
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
        if (ownMarginsCollapseWithChildren.end) {
            bottomMargin = layoutResult.lastChildBottomMarginSet;
        } else {
            float marginBottom = Resolve.resolveLpaOrZero(style.getMargin().bottom, parentSize.width);
            bottomMargin = CollapsibleMarginSet.fromMargin(marginBottom);
        }

        return new LayoutOutput(
            finalOuterSize,
            contentSize,
            new FloatPoint(NaN, NaN),
            topMargin,
            bottomMargin,
            canBeCollapsedThrough
        );
    }

    private FloatSize computeContentSizeFromChildren(NodeId node) {
        TaffyTree tree = layoutComputer.getTree();
        FloatSize contentSize = FloatSize.zero();

        for (NodeId childId : tree.getChildren(node)) {
            TaffyStyle childStyle = tree.getStyle(childId);
            if (childStyle.getBoxGenerationMode() == BoxGenerationMode.NONE) continue;

            Layout childLayout = tree.getUnroundedLayout(childId);
            if (childLayout == null) continue;

            FloatSize childContentSize = childLayout.contentSize() != null ? childLayout.contentSize() : childLayout.size();
            FloatSize contribution = ContentSizeUtil.computeContentSizeContribution(
                childLayout.location(),
                childLayout.size(),
                childContentSize,
                childStyle.getOverflow()
            );
            contentSize = ContentSizeUtil.max(contentSize, contribution);
        }

        return contentSize;
    }

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
            item.computedSize = new FloatSize(0f, 0f);
            item.staticPosition = new FloatPoint(0f, 0f);
            item.canBeCollapsedThrough = false;
            item.isTable = childStyle.getItemIsTable();

            items.add(item);
        }

        return items;
    }

    private float determineContentBasedContainerWidth(List<BlockItem> items, AvailableSpace availableWidth) {
        float maxChildWidth = 0f;

        for (BlockItem item : items) {
            if (item.position == TaffyPosition.ABSOLUTE) continue;

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
                width = output.size().width + marginSum;
            }
            width = Math.max(width, item.paddingBorderSum.width);
            maxChildWidth = Math.max(maxChildWidth, width);
        }

        return maxChildWidth;
    }

    private InFlowLayoutResult performFinalLayoutOnChildren(
        List<BlockItem> items,
        float containerOuterWidth,
        FloatRect contentBoxInset,
        TextAlign textAlign,
        TaffyDirection direction,
        TaffyLine<Boolean> ownMarginsCollapseWithChildren) {

        TaffyTree tree = layoutComputer.getTree();
        float containerInnerWidth = containerOuterWidth - contentBoxInset.left - contentBoxInset.right;
        FloatSize parentSize = new FloatSize(containerOuterWidth, NaN);
        TaffySize<AvailableSpace> availableSpace = new TaffySize<>(
            AvailableSpace.definite(containerInnerWidth),
            AvailableSpace.minContent()
        );

        float committedYOffset = contentBoxInset.top;
        float yOffsetForAbsolute = contentBoxInset.top;
        CollapsibleMarginSet firstChildTopMarginSet = CollapsibleMarginSet.zero();
        CollapsibleMarginSet activeCollapsibleMarginSet = CollapsibleMarginSet.zero();
        boolean isCollapsingWithFirstMarginSet = true;
        boolean allChildrenCanBeCollapsedThrough = true;

        // Check RTL once at the start
        boolean isRtl = direction != null && direction.isRtl();

        for (BlockItem item : items) {
            if (item.position == TaffyPosition.ABSOLUTE) {
                // In RTL, static position starts from right
                float staticX = isRtl
                    ? (containerOuterWidth - contentBoxInset.right)
                    : contentBoxInset.left;
                item.staticPosition = new FloatPoint(staticX, yOffsetForAbsolute);
                continue;
            }

            // Resolve margins
            FloatRect itemMarginOpt = resolveMarginOptional(item.margin, containerOuterWidth);
            FloatRect itemNonAutoMargin = new FloatRect(
                Float.isNaN(itemMarginOpt.left) ? 0f : itemMarginOpt.left,
                Float.isNaN(itemMarginOpt.right) ? 0f : itemMarginOpt.right,
                Float.isNaN(itemMarginOpt.top) ? 0f : itemMarginOpt.top,
                Float.isNaN(itemMarginOpt.bottom) ? 0f : itemMarginOpt.bottom
            );
            float itemNonAutoXMarginSum = itemNonAutoMargin.left + itemNonAutoMargin.right;

            FloatSize knownDimensions;
            if (item.isTable) {
                knownDimensions = new FloatSize(NaN, NaN);
            } else {
                float width = Float.isNaN(item.size.width) ? containerInnerWidth - itemNonAutoXMarginSum : item.size.width;
                width = TaffyMath.clamp(width, item.minSize.width, item.maxSize.width);
                knownDimensions = maybeClamp(
                    new FloatSize(width, item.size.height),
                    item.minSize,
                    item.maxSize
                );
            }

            LayoutOutput itemOutput = layoutComputer.performChildLayout(
                item.nodeId,
                knownDimensions,
                parentSize,
                new TaffySize<>(
                    subtractFromAvailable(availableSpace.width, itemNonAutoXMarginSum),
                    availableSpace.height
                ),
                SizingMode.INHERENT_SIZE,
                new TaffyLine<>(true, true)
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

            FloatRect resolvedMargin = new FloatRect(
                Float.isNaN(itemMarginOpt.left) ? xAxisAutoMarginSize : itemMarginOpt.left,
                Float.isNaN(itemMarginOpt.right) ? xAxisAutoMarginSize : itemMarginOpt.right,
                topMarginSet.resolve(),
                bottomMarginSet.resolve()
            );

            // Resolve item inset
            float insetLeft = item.inset.left.maybeResolve(containerInnerWidth);
            float insetRight = item.inset.right.maybeResolve(containerInnerWidth);
            float insetTop = item.inset.top.maybeResolve(0f);
            float insetBottom = item.inset.bottom.maybeResolve(0f);
            float insetOffsetX = !Float.isNaN(insetLeft) ? insetLeft : (!Float.isNaN(insetRight) ? -insetRight : 0f);
            float insetOffsetY = !Float.isNaN(insetTop) ? insetTop : (!Float.isNaN(insetBottom) ? -insetBottom : 0f);

            // Compute y margin offset with margin collapse
            float yMarginOffset;
            if (isCollapsingWithFirstMarginSet && ownMarginsCollapseWithChildren.start) {
                yMarginOffset = 0f;
            } else {
                yMarginOffset = activeCollapsibleMarginSet.copy()
                                                          .collapseWithMargin(resolvedMargin.top).resolve();
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

            // Calculate x position based on direction
            float itemOuterWidth = finalSize.width + resolvedMargin.left + resolvedMargin.right;
            float freeSpace = containerInnerWidth - itemOuterWidth;
            float x;

            if (isRtl) {
                // RTL: Default alignment is to the right (START in RTL)
                // Calculate x so item aligns to right edge by default
                x = contentBoxInset.left + freeSpace + resolvedMargin.left + insetOffsetX;

                // Apply text alignment adjustments for RTL
                if (itemOuterWidth < containerInnerWidth) {
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

                // Apply text alignment adjustments for LTR
                if (itemOuterWidth < containerInnerWidth) {
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

            FloatSize scrollbarSize = new FloatSize(
                item.overflow.y == Overflow.SCROLL ? item.scrollbarWidth : 0f,
                item.overflow.x == Overflow.SCROLL ? item.scrollbarWidth : 0f
            );

            Layout layout = new Layout(
                item.order,
                new FloatPoint(x, y),
                finalSize,
                itemOutput.contentSize(),
                scrollbarSize,
                item.border,
                item.padding,
                resolvedMargin
            );

            tree.setUnroundedLayout(item.nodeId, layout);

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
        float bottomYMarginOffset = ownMarginsCollapseWithChildren.end
                                    ? 0f
                                    : lastChildBottomMarginSet.resolve();

        committedYOffset += contentBoxInset.bottom + bottomYMarginOffset;
        float contentHeight = Math.max(0f, committedYOffset);

        return new InFlowLayoutResult(
            contentHeight,
            firstChildTopMarginSet,
            lastChildBottomMarginSet,
            allChildrenCanBeCollapsedThrough
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

    private void performAbsoluteLayoutOnChildren(
        List<BlockItem> items,
        FloatSize areaSize,
        FloatRect areaInset) {

        TaffyTree tree = layoutComputer.getTree();
        float areaWidth = areaSize.width - areaInset.left - areaInset.right;
        float areaHeight = areaSize.height - areaInset.top - areaInset.bottom;

        for (BlockItem item : items) {
            if (item.position != TaffyPosition.ABSOLUTE) continue;

            TaffyStyle childStyle = tree.getStyle(item.nodeId);
            if (childStyle.getBoxGenerationMode() == BoxGenerationMode.NONE) continue;

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

            // Fill in width from left/right if not set
            if (Float.isNaN(knownDimensions.width) && !Float.isNaN(left) && !Float.isNaN(right)) {
                float newWidth = areaWidth - nonAutoMarginLeft - nonAutoMarginRight - left - right;
                knownDimensions = new FloatSize(Math.max(newWidth, 0f), knownDimensions.height);
                knownDimensions = maybeClamp(Resolve.maybeApplyAspectRatio(knownDimensions, aspectRatio), minSz, maxSz);
            }

            // Fill in height from top/bottom if not set
            if (Float.isNaN(knownDimensions.height) && !Float.isNaN(top) && !Float.isNaN(bottom)) {
                float newHeight = areaHeight - nonAutoMarginTop - nonAutoMarginBottom - top - bottom;
                knownDimensions = new FloatSize(knownDimensions.width, Math.max(newHeight, 0f));
                knownDimensions = maybeClamp(Resolve.maybeApplyAspectRatio(knownDimensions, aspectRatio), minSz, maxSz);
            }

            LayoutOutput output = layoutComputer.performChildLayout(
                item.nodeId,
                knownDimensions,
                new FloatSize(areaWidth, areaHeight),
                new TaffySize<>(
                    AvailableSpace.definite(TaffyMath.clamp(areaWidth, minSz.width, maxSz.width)),
                    AvailableSpace.definite(TaffyMath.clamp(areaHeight, minSz.height, maxSz.height))
                ),
                SizingMode.CONTENT_SIZE,
                new TaffyLine<>(false, false)
            );

            FloatSize finalSize = maybeClamp(
                new FloatSize(
                    Float.isNaN(knownDimensions.width) ? output.size().width : knownDimensions.width,
                    Float.isNaN(knownDimensions.height) ? output.size().height : knownDimensions.height
                ),
                minSz, maxSz
            );

            // Ensure final size is at least padding + border
            finalSize = new FloatSize(
                Math.max(finalSize.width, paddingBorderSum.width),
                Math.max(finalSize.height, paddingBorderSum.height)
            );

            // Calculate non-auto margin (only count if inset is set on that side)
            FloatRect nonAutoMargin = new FloatRect(
                Float.isNaN(left) ? 0f : nonAutoMarginLeft,
                Float.isNaN(right) ? 0f : nonAutoMarginRight,
                Float.isNaN(top) ? 0f : nonAutoMarginTop,
                Float.isNaN(bottom) ? 0f : nonAutoMarginBottom
            );

            // Calculate auto margin space
            // Auto margins for absolutely positioned elements only resolve if inset is set
            float absoluteAutoMarginSpaceX = Float.isNaN(right) ? finalSize.width : areaWidth - right - (!Float.isNaN(left) ? left : 0f);
            float absoluteAutoMarginSpaceY = Float.isNaN(bottom) ? finalSize.height : areaHeight - bottom - (!Float.isNaN(top) ? top : 0f);

            float freeSpaceX = absoluteAutoMarginSpaceX - finalSize.width
                               - nonAutoMargin.left - nonAutoMargin.right;
            float freeSpaceY = absoluteAutoMarginSpaceY - finalSize.height
                               - nonAutoMargin.top - nonAutoMargin.bottom;

            // Calculate auto margin size
            float autoMarginSizeX = calcAutoMarginSize(marginOpt.left, marginOpt.right, styleSize.width, freeSpaceX);

            float autoMarginSizeY = calcAutoMarginSize(marginOpt.top, marginOpt.bottom, styleSize.height, freeSpaceY);

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

            // Position the item using resolved margins
            float x;
            if (!Float.isNaN(left)) {
                x = areaInset.left + left + resolvedMargin.left;
            } else if (!Float.isNaN(right)) {
                x = areaInset.left + areaWidth - finalSize.width - right - resolvedMargin.right;
            } else {
                x = item.staticPosition.x + resolvedMargin.left;
            }

            float y;
            if (!Float.isNaN(top)) {
                y = areaInset.top + top + resolvedMargin.top;
            } else if (!Float.isNaN(bottom)) {
                y = areaInset.top + areaHeight - finalSize.height - bottom - resolvedMargin.bottom;
            } else {
                y = item.staticPosition.y + resolvedMargin.top;
            }

            FloatSize scrollbarSize = new FloatSize(
                item.overflow.y == Overflow.SCROLL ? item.scrollbarWidth : 0f,
                item.overflow.x == Overflow.SCROLL ? item.scrollbarWidth : 0f
            );

            Layout layout = new Layout(
                item.order,
                new FloatPoint(x, y),
                finalSize,
                output.contentSize(),
                scrollbarSize,
                item.border,
                item.padding,
                resolvedMargin
            );

            tree.setUnroundedLayout(item.nodeId, layout);
        }
    }

    private static float calcAutoMarginSize(float marginOpt, float marginOpt1, float styleSize, float freeSpace) {
        float autoMarginSizeX;
        {
            int autoMarginCountX = (Float.isNaN(marginOpt) ? 1 : 0) + (Float.isNaN(marginOpt1) ? 1 : 0);
            // If both margins are auto and size is >= free space, set auto margins to 0
            if (autoMarginCountX == 2 && (Float.isNaN(styleSize) || styleSize >= freeSpace)) {
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
