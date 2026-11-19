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
import dev.vfyjxf.taffy.style.AlignSelf;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.BoxGenerationMode;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.JustifyContent;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.util.AlignmentHelper;
import dev.vfyjxf.taffy.util.BaselineHelper;
import dev.vfyjxf.taffy.util.ContentSizeUtil;
import dev.vfyjxf.taffy.util.Resolve;
import dev.vfyjxf.taffy.util.ScrollableOverflowUtil;
import dev.vfyjxf.taffy.util.SizingKeyword;
import dev.vfyjxf.taffy.util.TaffyMath;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Float.NaN;
import static java.lang.Float.POSITIVE_INFINITY;
import static java.lang.Float.isNaN;

/**
 * Computes flexbox layout for nodes with display: flex.
 */
public class FlexboxComputer {

    private final LayoutComputer layoutComputer;

    public FlexboxComputer(LayoutComputer layoutComputer) {
        this.layoutComputer = layoutComputer;
    }

    /**
     * Internal data structure for flex items.
     */
    private static class FlexItem {
        NodeId nodeId;
        int order;
        FloatSize size;
        FloatSize minSize;
        FloatSize maxSize;
        /** The item's aspect ratio, or {@code null}; used to transfer min/max sizes on demand. */
        Float aspectRatio;
        AlignSelf alignSelf;
        TaffyPoint<Overflow> overflow;
        float scrollbarWidth;
        float flexShrink;
        float flexGrow;
        float resolvedMinimumMainSize;
        FloatRect inset;
        FloatRect margin;
        TaffyRect<Boolean> marginIsAuto;
        FloatRect padding;
        FloatRect border;
        float flexBasis;
        float innerFlexBasis;
        float violation;
        boolean frozen;
        float contentFlexFraction;
        FloatSize hypotheticalInnerSize;
        FloatSize hypotheticalOuterSize;
        FloatSize targetSize;
        FloatSize outerTargetSize;
        float baseline;
        float offsetMain;
        float offsetCross;
    }

    /**
     * A line of flex items.
     */
    private static class FlexLine {
        List<FlexItem> items;
        float crossSize;
        float offsetCross;
    }

    /**
     * The result of the final layout pass (P4/SO-01/SO-02).
     *
     * @param firstBaseline      the container's first vertical baseline, or {@link Float#NaN} when absent
     * @param scrollableOverflow the container's finalized scrollable overflow rectangle
     */
    private record FinalLayoutResult(float firstBaseline, ScrollableOverflow scrollableOverflow) {
    }

    /**
     * Returns true if the item participates in baseline alignment: it has {@code align-self: baseline}
     * and neither of its cross-axis margins is {@code auto}.
     * See <a href="https://www.w3.org/TR/css-flexbox-1/#baseline-participation">baseline participation</a>.
     */
    private static boolean participatesInBaselineAlignment(FlexItem item, boolean isRow) {
        boolean crossStartAuto = isRow ? item.marginIsAuto.top : item.marginIsAuto.left;
        boolean crossEndAuto = isRow ? item.marginIsAuto.bottom : item.marginIsAuto.right;
        return item.alignSelf == AlignSelf.BASELINE && !crossStartAuto && !crossEndAuto;
    }

    /**
     * Computes flexbox layout for a node.
     */
    public LayoutOutput compute(NodeId node, LayoutInput inputs, TaffyStyle style) {
        TaffyTree tree = layoutComputer.getTree();
        FloatSize knownDimensions = inputs.knownDimensions();
        FloatSize parentSize = inputs.parentSize();
        TaffySize<AvailableSpace> availableSpace = inputs.availableSpace();
        RunMode runMode = inputs.runMode();

        FlexDirection flexDirection = style.getFlexDirection();
        TaffyDirection direction = layoutComputer.resolveDirection(node);
        boolean isRow = flexDirection.isRow();
        FlexWrap flexWrap = style.getFlexWrap();
        boolean isWrap = flexWrap.isMultiLine();
        boolean isWrapReverse = flexWrap.isReverse();
        boolean isBalance = flexWrap.isBalance();
        // FL-21 (#1105): upstream's `line_count` is Some for every multi-line container and
        // None for nowrap. 0 ("not set") behaves like the CSS initial value of 1.
        int lineCount = isWrap ? Math.max(style.getFlexLineCount(), 1) : 0;

        Float aspectRatio = style.getAspectRatio();
        FloatRect padding = Resolve.resolveRectOrZero(style.getPadding(), parentSize.width);
        FloatRect border = Resolve.resolveRectOrZero(style.getBorder(), parentSize.width);
        FloatSize paddingBorderSize = new FloatSize(
            padding.left + padding.right + border.left + border.right,
            padding.top + padding.bottom + border.top + border.bottom
        );

        FloatSize boxSizingAdjustment = style.getBoxSizing() == BoxSizing.CONTENT_BOX
                                        ? paddingBorderSize
                                        : FloatSize.ZERO;

        FloatSize minSize = maybeAdd(maybeApplyAspectRatio(
            Resolve.maybeResolveSize(style.getMinSize(), parentSize), aspectRatio), boxSizingAdjustment);
        FloatSize maxSize = maybeAdd(maybeApplyAspectRatio(
            Resolve.maybeResolveSize(style.getMaxSize(), parentSize), aspectRatio), boxSizingAdjustment);

        FloatSize resolvedStyleSize = Resolve.maybeResolveSize(style.getSize(), parentSize);

        FloatSize clampedStyleSize = inputs.sizingMode() == SizingMode.INHERENT_SIZE
                                     ? maybeClamp(maybeAdd(maybeApplyAspectRatio(resolvedStyleSize, aspectRatio), boxSizingAdjustment), minSize, maxSize)
                                     : new FloatSize(NaN, NaN);

        // Compute styled based known dimensions
        FloatSize minMaxDefiniteSize = new FloatSize(
            (!isNaN(minSize.width) && !isNaN(maxSize.width) && maxSize.width <= minSize.width)
            ? minSize.width : NaN,
            (!isNaN(minSize.height) && !isNaN(maxSize.height) && maxSize.height <= minSize.height)
            ? minSize.height : NaN
        );

        FloatSize styledBasedKnownDimensions = orChain(knownDimensions, minMaxDefiniteSize, clampedStyleSize);
        styledBasedKnownDimensions = maybeMax(styledBasedKnownDimensions, paddingBorderSize);

        // Detect AR-derived dimensions: auto dimension filled by aspect-ratio, not explicitly specified.
        // These should act as preferred/minimum values, not hard constraints — content can exceed them.
        boolean crossIsArDerived = false;
        boolean mainIsArDerived = false;
        if (aspectRatio != null) {
            float styleCross = isRow ? resolvedStyleSize.height : resolvedStyleSize.width;
            float knownCross = isRow ? knownDimensions.height : knownDimensions.width;
            float mmCross = isRow ? minMaxDefiniteSize.height : minMaxDefiniteSize.width;
            float clampedCross = isRow ? clampedStyleSize.height : clampedStyleSize.width;
            if (isNaN(styleCross) && isNaN(knownCross) && isNaN(mmCross) && !isNaN(clampedCross)) {
                crossIsArDerived = true;
            }
            float styleMain = isRow ? resolvedStyleSize.width : resolvedStyleSize.height;
            float knownMainVal = isRow ? knownDimensions.width : knownDimensions.height;
            float mmMain = isRow ? minMaxDefiniteSize.width : minMaxDefiniteSize.height;
            float clampedMain = isRow ? clampedStyleSize.width : clampedStyleSize.height;
            if (isNaN(styleMain) && isNaN(knownMainVal) && isNaN(mmMain) && !isNaN(clampedMain)) {
                mainIsArDerived = true;
            }
        }

        // Short-circuit if size is known
        if (runMode == RunMode.COMPUTE_SIZE &&
            !isNaN(styledBasedKnownDimensions.width) &&
            !isNaN(styledBasedKnownDimensions.height)) {
            return LayoutOutput.fromOuterSize(styledBasedKnownDimensions);
        }

        // Compute scrollbar gutter
        float scrollbarWidth = style.getScrollbarWidth();
        TaffyPoint<Overflow> overflow = style.getOverflow();
        FloatSize scrollbarGutter = new FloatSize(
            overflow.y == Overflow.SCROLL ? scrollbarWidth : 0f,
            overflow.x == Overflow.SCROLL ? scrollbarWidth : 0f
        );

        // P4/SO-01/SO-02: the scrollable overflow rectangle is produced by the final layout pass
        // below. The flags and frames it needs are fixed here.
        // A scroll container clips unreachable overflow in both axes (upstream #1118: Hidden/Scroll
        // in either axis clips both).
        boolean isScrollContainer = ScrollableOverflowUtil.isScrollContainer(overflow);
        boolean isRtl = direction == TaffyDirection.RTL;
        // Upstream #1372 `scroll_origin_reversed`: the flex main-axis reversal and the
        // wrap-reverse cross reversal, transposed for column containers; RTL puts the inline scroll
        // origin on the physical right edge.
        boolean mainReversed = flexDirection.isReverse();
        boolean reversedX = (isRow ? mainReversed : isWrapReverse) ^ isRtl;
        boolean reversedY = isRow ? isWrapReverse : mainReversed;
        // The offset from the border box to the client area (upstream `padding_box_inset`).
        // Scrollbar gutters sit on the inline-end side: the physical right in LTR, left in RTL.
        FloatRect paddingBoxInset = new FloatRect(
            border.left + (isRtl ? scrollbarGutter.width : 0f),
            border.right + (isRtl ? 0f : scrollbarGutter.width),
            border.top,
            border.bottom + scrollbarGutter.height
        );

        // Content box inset - includes padding, border, and scrollbar gutter
        FloatRect contentBoxInset = new FloatRect(
            padding.left + border.left,
            padding.right + border.right + scrollbarGutter.width, // scrollbar on right
            padding.top + border.top,
            padding.bottom + border.bottom + scrollbarGutter.height // scrollbar on bottom
        );
        FloatSize nodeInnerSize = new FloatSize(
            TaffyMath.maybeSub(styledBasedKnownDimensions.width, contentBoxInset.left + contentBoxInset.right),
            TaffyMath.maybeSub(styledBasedKnownDimensions.height, contentBoxInset.top + contentBoxInset.bottom)
        );

        // Gap
        FloatSize gap = new FloatSize(
            style.getGap().width.resolveOrZero(nodeInnerSize.width),
            style.getGap().height.resolveOrZero(nodeInnerSize.height)
        );

        // Determine available space for flex items (transforms outer available space to inner available space)
        TaffySize<AvailableSpace> innerAvailableSpace = determineAvailableSpace(styledBasedKnownDimensions, availableSpace, contentBoxInset);

        // Generate flex items
        List<FlexItem> items = generateFlexItems(node, style, nodeInnerSize, flexDirection, direction);

        // If no flow items, we still need to layout absolute children
        if (items.isEmpty()) {
            FloatSize containerSize = !isNaN(styledBasedKnownDimensions.width) && !isNaN(styledBasedKnownDimensions.height)
                                      ? styledBasedKnownDimensions
                                      : paddingBorderSize;

            if (runMode == RunMode.COMPUTE_SIZE) {
                return LayoutOutput.fromOuterSize(containerSize);
            }

            List<NodeId> children = tree.getChildren(node);
            FloatSize clientSize = computeClientSize(containerSize, border, scrollbarGutter);
            ScrollableOverflowUtil.Container overflowContainer = new ScrollableOverflowUtil.Container(
                isScrollContainer, clientSize, reversedX, reversedY, padding);

            // Still need to layout absolute children even if no flow children.
            // FL-23 (#1072): the default justify-content behaves as flex-start (upstream resolves
            // `normal` to FLEX_START for flex containers), not writing-mode-relative start.
            JustifyContent jc = style.justifyContent != AlignContent.AUTO ? style.getJustifyContent() : JustifyContent.FLEX_START;
            FloatRect absoluteOverflow = layoutAbsoluteChildren(
                node, containerSize, contentBoxInset, flexDirection, style.getAlignItems(), jc,
                isWrapReverse, isRtl, border, scrollbarGutter, paddingBoxInset, overflowContainer);

            // Layout hidden children (display: none)
            for (int order = 0; order < children.size(); order++) {
                NodeId child = children.get(order);
                if (tree.getStyle(child).getBoxGenerationMode() == BoxGenerationMode.NONE) {
                    tree.setUnroundedLayout(child, Layout.withOrder(order));
                    layoutComputer.performChildLayout(
                        child,
                        FloatSize.none(),
                        FloatSize.none(),
                        TaffySize.maxContent(),
                        SizingMode.INHERENT_SIZE,
                        TaffyLine.FALSE
                    );
                }
            }

            FloatSize contentSize = computeContentSizeFromChildren(node, border);

            // P4/SO-01/SO-02: a container with no children at all follows upstream's
            // `compute_empty_container_layout` (only RTL reaches the client width). This branch is
            // normally unreachable because LayoutComputer routes child_count == 0 to the leaf path,
            // but it keeps the empty-container semantics if that routing changes. A container whose
            // children are all out of flow or hidden still runs the regular final-layout unions
            // (RTL client width, scroll-container end padding) and unions the absolute children's
            // contributions on top.
            FloatRect accumulated;
            if (children.isEmpty()) {
                accumulated = isRtl
                              ? new FloatRect(0f, Math.max(0f, clientSize.width), 0f, 0f)
                              : FloatRect.zero();
            } else {
                accumulated = addContainerOverflow(absoluteOverflow, containerSize, padding, border,
                    scrollbarGutter, isRtl, isScrollContainer);
            }
            ScrollableOverflow scrollableOverflow = ScrollableOverflowUtil.finalizeScrollableOverflow(
                accumulated, clientSize, reversedX, reversedY, isScrollContainer);
            FloatPoint noBaseline = new FloatPoint(NaN, NaN);
            return new LayoutOutput(
                containerSize,
                contentSize,
                scrollableOverflow,
                noBaseline,
                Baselines.fromFirst(noBaseline),
                CollapsibleMarginSet.ZERO,
                CollapsibleMarginSet.ZERO,
                false
            );
        }

        // Determine flex base size and hypothetical main size
        float crossGap = isRow ? gap.height : gap.width;
        determineFlexBaseSize(items, nodeInnerSize, innerAvailableSpace, flexDirection, lineCount, crossGap);

        // Collect items into flex lines (use innerAvailableSpace for wrapping, matching Rust)
        float innerMainSize = isRow ? nodeInnerSize.width : nodeInnerSize.height;
        float mainGap = isRow ? gap.width : gap.height;
        List<FlexLine> flexLines = collectIntoFlexLines(
            items, innerAvailableSpace, mainGap, isWrap, isBalance, lineCount, flexDirection,
            minSize, maxSize, innerMainSize);

        // Determine container main size if not already known (needed for flex grow/shrink)
        if (isNaN(innerMainSize)) {
            innerMainSize = determineContainerMainSize(
                flexLines,
                contentBoxInset,
                minSize,
                maxSize,
                mainGap,
                crossGap,
                flexDirection,
                isWrap,
                isBalance,
                lineCount,
                innerAvailableSpace,
                nodeInnerSize
            );

            // Update nodeInnerSize with the determined main size (matches Rust behavior)
            // This is critical for proper percentage margin resolution in child nodes
            if (isRow) {
                nodeInnerSize = new FloatSize(innerMainSize, nodeInnerSize.height);
            } else {
                nodeInnerSize = new FloatSize(nodeInnerSize.width, innerMainSize);
            }

            // Re-resolve percentage gaps now that we know the main size
            // This handles cyclic gap dependencies (gap is % of container, container depends on content + gap)
            LengthPercentage mainGapStyle = isRow ? style.getGap().width : style.getGap().height;
            mainGap = mainGapStyle.resolveOrZero(innerMainSize);
            // Also update the gap Size so subsequent functions use the correct value
            if (isRow) {
                gap = new FloatSize(mainGap, gap.height);
            } else {
                gap = new FloatSize(gap.width, mainGap);
            }
        }

        // Resolve flexible lengths
        for (FlexLine line : flexLines) {
            resolveFlexibleLengths(line, flexDirection, innerMainSize, mainGap);
        }

        // 9.4. Cross Size Determination
        // 7. Determine the hypothetical cross size of each item
        for (FlexLine line : flexLines) {
            determineHypotheticalCrossSize(
                line, flexDirection, innerMainSize, nodeInnerSize, innerAvailableSpace, isWrap, lineCount, crossGap);
        }

        // Calculate children baselines (for baseline alignment support)
        // This needs to be before calculateCrossSize since baseline affects line cross size
        calculateChildrenBaselines(flexLines, nodeInnerSize, innerAvailableSpace, flexDirection);

        // Calculate cross size
        calculateCrossSize(flexLines, nodeInnerSize, innerAvailableSpace, flexDirection, style, minSize, maxSize, contentBoxInset, crossIsArDerived);

        // Distribute remaining space
        distributeRemainingMainSpace(flexLines, style, innerMainSize, mainGap, flexDirection);

        // Calculate container size (use innerMainSize as the known main size when available)
        FloatSize containerSize = calculateContainerSize(
            flexLines, styledBasedKnownDimensions, paddingBorderSize,
            contentBoxInset, gap, minSize, maxSize, flexDirection, innerMainSize,
            crossIsArDerived, mainIsArDerived);

        if (runMode == RunMode.COMPUTE_SIZE) {
            return LayoutOutput.fromOuterSize(containerSize);
        }

        // Calculate total cross size of all lines for align-content
        float totalLineCrossSize = 0;
        for (FlexLine line : flexLines) {
            totalLineCrossSize += line.crossSize;
        }

        // Align flex lines per align-content (this may stretch lines)
        alignFlexLinesPerAlignContent(flexLines, containerSize, contentBoxInset, gap, style, flexDirection, isWrap, isWrapReverse, totalLineCrossSize);

        // Save item main-axis sums before AR derivation (for detecting changes)
        float preArItemsMain = 0;
        for (FlexLine line : flexLines) {
            float lineMain = 0;
            for (FlexItem item : line.items) {
                lineMain += isRow ? item.outerTargetSize.width : item.outerTargetSize.height;
            }
            preArItemsMain = Math.max(preArItemsMain, lineMain);
        }

        // Determine used cross size for stretch items (uses stretched line.crossSize)
        determineUsedCrossSize(flexLines, flexDirection, style);

        // After AR derivation in determineUsedCrossSize, items may have larger main-axis sizes.
        // Compare item sums to detect actual changes (avoids re-adding gap which breaks cyclic gap resolution).
        float knownMainForUpdate = isRow ? styledBasedKnownDimensions.width : styledBasedKnownDimensions.height;
        if (Float.isNaN(knownMainForUpdate)) {
            float postArItemsMain = 0;
            for (FlexLine line : flexLines) {
                float lineMain = 0;
                for (FlexItem item : line.items) {
                    lineMain += isRow ? item.outerTargetSize.width : item.outerTargetSize.height;
                }
                postArItemsMain = Math.max(postArItemsMain, lineMain);
            }
            float increase = postArItemsMain - preArItemsMain;
            if (increase > 0.01f) {
                float originalContainerMain = isRow ? containerSize.width : containerSize.height;
                float updatedContainerMain = originalContainerMain + increase;
                updatedContainerMain = TaffyMath.clamp(updatedContainerMain,
                    isRow ? minSize.width : minSize.height, isRow ? maxSize.width : maxSize.height);
                updatedContainerMain = Math.max(updatedContainerMain, isRow ? paddingBorderSize.width : paddingBorderSize.height);
                if (isRow) {
                    containerSize = new FloatSize(updatedContainerMain, containerSize.height);
                } else {
                    containerSize = new FloatSize(containerSize.width, updatedContainerMain);
                }
            }
        }

        // Align items on cross axis (must be after lines are stretched)
        alignItemsOnCrossAxis(flexLines, flexDirection, isWrapReverse);

        // Perform the final layout pass: it positions every flex item and every out-of-flow
        // child and accumulates their scrollable overflow contributions into the container's
        // rectangle (P4/SO-01/SO-02), then finalizes it.
        FloatSize clientSize = computeClientSize(containerSize, border, scrollbarGutter);
        ScrollableOverflowUtil.Container overflowContainer = new ScrollableOverflowUtil.Container(
            isScrollContainer, clientSize, reversedX, reversedY, padding);
        FinalLayoutResult finalLayout = performFinalLayout(
            flexLines, node, containerSize, contentBoxInset, gap, style, flexDirection,
            isWrapReverse, border, padding, scrollbarGutter, paddingBoxInset, overflowContainer,
            isRtl, isScrollContainer, reversedX, reversedY);

        // Layout hidden children (display: none)
        List<NodeId> children = tree.getChildren(node);
        for (int order = 0; order < children.size(); order++) {
            NodeId child = children.get(order);
            if (tree.getStyle(child).getBoxGenerationMode() == BoxGenerationMode.NONE) {
                tree.setUnroundedLayout(child, Layout.withOrder(order));
                layoutComputer.performChildLayout(
                    child,
                    FloatSize.none(),
                    FloatSize.none(),
                    TaffySize.maxContent(),
                    SizingMode.INHERENT_SIZE,
                    TaffyLine.FALSE
                );
            }
        }

        FloatSize contentSize = computeContentSizeFromChildren(node, border);
        FloatPoint firstBaseline = new FloatPoint(NaN, finalLayout.firstBaseline());
        return new LayoutOutput(
            containerSize,
            contentSize,
            finalLayout.scrollableOverflow(),
            firstBaseline,
            Baselines.fromFirst(firstBaseline),
            CollapsibleMarginSet.ZERO,
            CollapsibleMarginSet.ZERO,
            false
        );
    }

    /**
     * Aggregates the legacy 1.x {@code contentSize} component from the final child layouts.
     *
     * <p><b>P4/SO-03 (#1007):</b> the legacy content-size contribution of a flex child used to be
     * measured from the container's border box, which inflated it by the leading border. It is now
     * measured from the padding-box origin (the scroll origin), so the container's leading border
     * is subtracted from the child location. This is what makes {@link Layout#scrollWidth()} /
     * {@link Layout#scrollHeight()}, whose merged formula adds both borders, report the same range
     * for a border on either edge.</p>
     *
     * <p><b>RTL:</b> upstream #1007 mirrored the contribution into inline-start coordinates at the
     * same time, but the #1372 model that this port's {@link ScrollableOverflow} follows measures
     * scrollable overflow in physical coordinates and carries RTL in its reversed-origin flags.
     * The legacy field is a physical extent here and is not mirrored (2.0 removes it).</p>
     *
     * @param node            the flex container
     * @param containerBorder the container's resolved border
     * @return the legacy content size
     */
    private FloatSize computeContentSizeFromChildren(NodeId node, FloatRect containerBorder) {
        TaffyTree tree = layoutComputer.getTree();
        FloatSize contentSize = FloatSize.zero();

        for (NodeId childId : tree.getChildren(node)) {
            TaffyStyle childStyle = tree.getStyle(childId);
            if (childStyle.getBoxGenerationMode() == BoxGenerationMode.NONE) continue;

            Layout childLayout = tree.getUnroundedLayout(childId);
            if (childLayout == null) continue;

            FloatPoint childLocation = childLayout.location();
            FloatSize childContentSize = childLayout.contentSize() != null ? childLayout.contentSize() : childLayout.size();
            FloatSize contribution = ContentSizeUtil.computeContentSizeContribution(
                new FloatPoint(
                    childLocation.x - containerBorder.left,
                    childLocation.y - containerBorder.top
                ),
                childLayout.size(),
                childContentSize,
                childStyle.getOverflow()
            );
            contentSize = ContentSizeUtil.max(contentSize, contribution);
        }

        return contentSize;
    }

    private List<FlexItem> generateFlexItems(NodeId node, TaffyStyle containerStyle, FloatSize nodeInnerSize,
                                             FlexDirection flexDirection, TaffyDirection containerDirection) {
        TaffyTree tree = layoutComputer.getTree();
        List<FlexItem> items = new ArrayList<>();
        // ST-04 (#1254): explicit `normal` behaves as `stretch` for flex items. The legacy AUTO
        // sentinel stays untouched: it means "not specified" and defers to the caller default.
        AlignItems defaultAlign = AlignmentHelper.resolveNormal(containerStyle.getAlignItems(), AlignItems.STRETCH);

        boolean isRow = flexDirection.isRow();

        int order = 0;
        for (NodeId childId : tree.getChildren(node)) {
            TaffyStyle childStyle = tree.getStyle(childId);
            if (childStyle.getBoxGenerationMode() == BoxGenerationMode.NONE ||
                childStyle.getPosition().isOutOfFlow()) {
                order++;
                continue;
            }

            FlexItem item = new FlexItem();
            item.nodeId = childId;
            item.order = order++;

            Float aspectRatio = childStyle.getAspectRatio();
            FloatRect itemPadding = Resolve.resolveRectOrZero(childStyle.getPadding(), nodeInnerSize.width);
            FloatRect itemBorder = Resolve.resolveRectOrZero(childStyle.getBorder(), nodeInnerSize.width);
            item.padding = itemPadding;
            item.border = itemBorder;

            FloatSize paddingBorderSum = new FloatSize(
                itemPadding.left + itemPadding.right + itemBorder.left + itemBorder.right,
                itemPadding.top + itemPadding.bottom + itemBorder.top + itemBorder.bottom
            );

            FloatSize boxSizingAdj = childStyle.getBoxSizing() == BoxSizing.CONTENT_BOX
                                     ? paddingBorderSum
                                     : FloatSize.ZERO;

            // Keep raw resolved size (pre aspect-ratio) so we can allow cross-axis stretch to win later.
            FloatSize rawResolvedSize = Resolve.maybeResolveSize(childStyle.getSize(), nodeInnerSize);
            FloatSize rawResolvedMinSize = Resolve.maybeResolveSize(childStyle.getMinSize(), nodeInnerSize);
            FloatSize rawResolvedMaxSize = Resolve.maybeResolveSize(childStyle.getMaxSize(), nodeInnerSize);

            item.size = maybeAdd(maybeApplyAspectRatio(
                rawResolvedSize, aspectRatio), boxSizingAdj);
            // FL-19 (#989): do not apply the aspect ratio to min/max sizes here. The transferred
            // sizes are derived on demand at the two clamp sites (flex base size and hypothetical
            // cross size), because the ratio may combine with sizes whose definiteness is only
            // known after other flex sizing steps.
            item.minSize = maybeAdd(rawResolvedMinSize, boxSizingAdj);
            item.maxSize = maybeAdd(rawResolvedMaxSize, boxSizingAdj);
            item.aspectRatio = aspectRatio;

            item.overflow = childStyle.getOverflow();
            item.scrollbarWidth = childStyle.getScrollbarWidth();

            // ST-01/FL-17: a main-axis 'stretch' is resolved to an exact flex base size in
            // determineFlexBaseSize, so it must not also be mapped to flex-grow: 1 here
            // (upstream has no such mapping; it inflated the item when a sibling's negative
            // margin left free space in the line).
            item.flexGrow = childStyle.getFlexGrow();
            item.flexShrink = childStyle.getFlexShrink();

            // ST-04 (#1254): an explicit `align-self: normal` overrides the container's
            // align-items (unlike AUTO, which defers to it), then resolves to STRETCH for flex.
            // ST-03 (#1077): self-start/self-end resolve against the item's own direction; the
            // cross axis is the inline axis only for column containers.
            AlignItems selfAlignItems = childStyle.getAlignSelf();
            AlignItems effectiveAlignItems = (selfAlignItems != null && selfAlignItems != AlignItems.AUTO)
                                             ? selfAlignItems
                                             : defaultAlign;
            effectiveAlignItems = AlignmentHelper.resolveNormal(effectiveAlignItems, AlignItems.STRETCH);
            AlignSelf alignSelf = AlignSelf.fromAlignItems(effectiveAlignItems);
            alignSelf = AlignmentHelper.resolveSelfRelative(
                alignSelf, layoutComputer.resolveDirection(childId), containerDirection, !isRow);
            item.alignSelf = alignSelf;

            // If cross size is auto in style and the item is stretched on the cross axis, do not let aspect-ratio
            // resolve the cross size here. Stretch should be able to set it.
            // For WRAP containers, keep the AR-derived cross so hypothetical cross sizing uses
            // the initial AR value (matching browser behavior where container cross reflects pre-flex-growth AR).
            boolean isWrap = containerStyle.getFlexWrap().isMultiLine();
            if (aspectRatio != null && !Float.isNaN(aspectRatio) && item.alignSelf == AlignSelf.STRETCH) {
                boolean crossSizeAutoInStyle = isRow ? (isNaN(rawResolvedSize.height)) : (isNaN(rawResolvedSize.width));

                // 1) Avoid locking computed cross size from aspect ratio when stretch should set it.
                // Only for no-wrap containers: in wrap mode, the AR-derived cross is needed for container sizing.
                boolean crossAutoInStyleWithDefiniteMain = isRow
                                                           ? (isNaN(rawResolvedSize.height) && !isNaN(rawResolvedSize.width))
                                                           : (isNaN(rawResolvedSize.width) && !isNaN(rawResolvedSize.height));

                if (crossAutoInStyleWithDefiniteMain && !isWrap) {
                    if (isRow) {
                        item.size = new FloatSize(item.size.width, NaN);
                    } else {
                        item.size = new FloatSize(NaN, item.size.height);
                    }
                }

                // 2) Avoid deriving cross-axis min/max constraints from the main-axis min/max via aspect-ratio,
                // because those derived constraints would incorrectly clamp stretch.
                if (crossSizeAutoInStyle) {
                    boolean minCrossDerivedFromMain = isRow
                                                      ? (isNaN(rawResolvedMinSize.height) && !isNaN(rawResolvedMinSize.width))
                                                      : (isNaN(rawResolvedMinSize.width) && !isNaN(rawResolvedMinSize.height));
                    boolean maxCrossDerivedFromMain = isRow
                                                      ? (isNaN(rawResolvedMaxSize.height) && !isNaN(rawResolvedMaxSize.width))
                                                      : (isNaN(rawResolvedMaxSize.width) && !isNaN(rawResolvedMaxSize.height));

                    if (minCrossDerivedFromMain) {
                        if (isRow) {
                            item.minSize = new FloatSize(item.minSize.width, NaN);
                        } else {
                            item.minSize = new FloatSize(NaN, item.minSize.height);
                        }
                    }

                    if (maxCrossDerivedFromMain) {
                        if (isRow) {
                            item.maxSize = new FloatSize(item.maxSize.width, NaN);
                        } else {
                            item.maxSize = new FloatSize(NaN, item.maxSize.height);
                        }
                    }
                }
            }

            item.margin = Resolve.resolveRectLpaOrZero(childStyle.getMargin(), nodeInnerSize.width);
            item.marginIsAuto = new TaffyRect<>(
                childStyle.getMargin().left.isAuto(),
                childStyle.getMargin().right.isAuto(),
                childStyle.getMargin().top.isAuto(),
                childStyle.getMargin().bottom.isAuto()
            );

            // Only `position: relative` applies its inset as a correction offset. Static and
            // sticky boxes are laid out in normal flow without one (a sticky offset depends on
            // the scroll position and is the caller's job), and a fixed box is out of flow and
            // never becomes a flex item. Upstream #1140 stores the equivalent empty inset
            // (`Rect::NONE`) for every non-relative item.
            if (childStyle.getPosition() == TaffyPosition.RELATIVE) {
                TaffyRect<LengthPercentageAuto> insetStyle = childStyle.getInset();
                item.inset = new FloatRect(
                    insetStyle.left.maybeResolve(nodeInnerSize.width),
                    insetStyle.right.maybeResolve(nodeInnerSize.width),
                    insetStyle.top.maybeResolve(nodeInnerSize.height),
                    insetStyle.bottom.maybeResolve(nodeInnerSize.height)
                );
            } else {
                item.inset = new FloatRect(Float.NaN, Float.NaN, Float.NaN, Float.NaN);
            }

            item.hypotheticalInnerSize = new FloatSize(0f, 0f);
            item.hypotheticalOuterSize = new FloatSize(0f, 0f);
            item.targetSize = new FloatSize(0f, 0f);
            item.outerTargetSize = new FloatSize(0f, 0f);
            item.violation = 0;
            item.frozen = false;
            item.baseline = 0;
            item.offsetMain = 0;
            item.offsetCross = 0;

            items.add(item);
        }

        return items;
    }

    /**
     * Determines the available space for flex items.
     * Transforms outer available space to inner available space based on known dimensions.
     * This is equivalent to determine_available_space in Rust.
     */
    private TaffySize<AvailableSpace> determineAvailableSpace(
        FloatSize knownDimensions,
        TaffySize<AvailableSpace> outerAvailableSpace,
        FloatRect contentBoxInset) {

        float horizontalInset = contentBoxInset.left + contentBoxInset.right;
        float verticalInset = contentBoxInset.top + contentBoxInset.bottom;

        // FL-09 (#1178): available space is floored at zero on both the known and the inherited path.
        AvailableSpace width;
        if (!isNaN(knownDimensions.width)) {
            width = AvailableSpace.definite(Math.max(knownDimensions.width - horizontalInset, 0f));
        } else {
            width = outerAvailableSpace.width.maybeSub(horizontalInset).maybeMax(0f);
        }

        AvailableSpace height;
        if (!isNaN(knownDimensions.height)) {
            height = AvailableSpace.definite(Math.max(knownDimensions.height - verticalInset, 0f));
        } else {
            height = outerAvailableSpace.height.maybeSub(verticalInset).maybeMax(0f);
        }

        return new TaffySize<>(width, height);
    }

    private void determineFlexBaseSize(
        List<FlexItem> items,
        FloatSize nodeInnerSize,
        TaffySize<AvailableSpace> availableSpace,
        FlexDirection flexDirection,
        int lineCount,
        float crossGap) {

        boolean isRow = flexDirection.isRow();

        for (FlexItem item : items) {
            TaffyTree tree = layoutComputer.getTree();
            TaffyStyle childStyle = tree.getStyle(item.nodeId);

            // Parent size for child sizing
            float crossAxisParentSize = isRow ? nodeInnerSize.height : nodeInnerSize.width;
            // child_parent_size = Size::from_cross(dir, cross_axis_parent_size) - only cross axis is set
            FloatSize childParentSize = isRow
                                        ? new FloatSize(NaN, crossAxisParentSize)
                                        : new FloatSize(crossAxisParentSize, NaN);

            // FL-19 (#989): sizes transferred through the aspect ratio clamp the cross axis
            // min/max used by the flex base size calculation (https://github.com/w3c/csswg-drafts/issues/10997).
            FloatSize transferredMinSize = maybeApplyAspectRatio(item.minSize, item.aspectRatio);
            FloatSize transferredMaxSize = maybeApplyAspectRatio(item.maxSize, item.aspectRatio);
            float itemMinCross = isRow ? transferredMinSize.height : transferredMinSize.width;
            float itemMaxCross = isRow ? transferredMaxSize.height : transferredMaxSize.width;

            // FL-10 (#1234): definite available space is the child's border-box space, so the
            // parent subtracts the child's margins (auto margins have already resolved to zero).
            float marginCross = isRow
                                ? item.margin.top + item.margin.bottom
                                : item.margin.left + item.margin.right;

            // Clamp available space by min- and max- size (matching Rust's determine_flex_base_size)
            AvailableSpace crossAxisAvailableSpace;
            AvailableSpace outerCrossAvail = isRow ? availableSpace.height : availableSpace.width;

            if (outerCrossAvail.isDefinite()) {
                float val = outerCrossAvail.getValue();
                // FL-21 (#1105): flex-line-count divides definite cross-axis available space
                // between the requested number of lines (after subtracting the cross gaps).
                float clamped = dividedCrossSpace(
                    !isNaN(crossAxisParentSize) ? crossAxisParentSize : val, lineCount, crossGap);
                // FL-10: subtract the item margin before flooring/clamping (upstream order).
                // FL-09: clamp the constraint at zero before applying min/max.
                clamped = Math.max(clamped - marginCross, 0f);
                if (!isNaN(itemMinCross)) clamped = Math.max(clamped, itemMinCross);
                if (!isNaN(itemMaxCross)) clamped = Math.min(clamped, itemMaxCross);
                crossAxisAvailableSpace = AvailableSpace.definite(clamped);
            } else if (outerCrossAvail.isMinContent()) {
                crossAxisAvailableSpace = isNaN(itemMinCross) ? AvailableSpace.minContent() : AvailableSpace.definite(itemMinCross);
            } else { // MaxContent
                crossAxisAvailableSpace = isNaN(itemMaxCross) ? AvailableSpace.maxContent() : AvailableSpace.definite(itemMaxCross);
            }

            // Determine flex basis
            TaffyDimension flexBasis = childStyle.getFlexBasis();
            float mainSize = isRow ? item.size.width : item.size.height;
            float crossSize = isRow ? item.size.height : item.size.width;

            float paddingBorderMain = isRow
                                      ? item.padding.left + item.padding.right + item.border.left + item.border.right
                                      : item.padding.top + item.padding.bottom + item.border.top + item.border.bottom;

            // Compute child_known_dimensions (Rust flexbox.rs lines 677-690)
            // This is used for BOTH flex base size AND min-content calculations
            // When align_self is STRETCH and cross size is not defined, apply cross-axis 
            // available space as known dimension. This allows nested flex containers to 
            // know their width when parent is flex-direction: column.
            // FL-19 (#989): clamp a definite cross size by the transferred cross min/max so that
            // sizes transferred through the aspect ratio are based on the used cross size.
            float childKnownCross = TaffyMath.maybeClamp(crossSize, itemMinCross, itemMaxCross);

            if (item.alignSelf == AlignSelf.STRETCH && isNaN(childKnownCross)) {
                boolean crossStartAuto = isRow ? item.marginIsAuto.top : item.marginIsAuto.left;
                boolean crossEndAuto = isRow ? item.marginIsAuto.bottom : item.marginIsAuto.right;
                if (!crossStartAuto && !crossEndAuto) {
                    // FL-10: the cross available space already excludes the item's margins,
                    // so it must not be subtracted a second time here.
                    float crossAvailValue = crossAxisAvailableSpace.intoOption();
                    if (!isNaN(crossAvailValue)) {
                        // FL-09: a stretched item's known cross size is floored at zero.
                        childKnownCross = Math.max(crossAvailValue, 0f);
                    }
                }
            }

            FloatSize childKnownDimensions = isRow
                                             ? new FloatSize(NaN, childKnownCross)
                                             : new FloatSize(childKnownCross, NaN);

            // Compute box_sizing_adjustment for flex-basis (Rust flexbox.rs lines 693-704)
            // In content-box mode, flex-basis is a content size, so we need to add padding+border
            // to get the outer size that flex layout uses.
            float boxSizingAdjustmentMain = childStyle.getBoxSizing() == BoxSizing.CONTENT_BOX
                                            ? paddingBorderMain
                                            : 0f;

            // ST-01/FL-17: percentages, stretch and fit-content resolve against the container's
            // inner main size. FL-15 replaces this with the explicit known_dimensions_are_definite
            // flag; until then this is exactly the basis the existing percentage resolution used.
            float percentResolutionMainSize = isRow ? nodeInnerSize.width : nodeInnerSize.height;
            float itemMarginMain = isRow
                                  ? item.margin.left + item.margin.right
                                  : item.margin.top + item.margin.bottom;
            // The size the item would take if stretched to fill the container's main axis
            // (upstream main_stretch_size: inner main size minus the item's main margins, floored
            // at zero). Used by 'stretch' and the bare 'fit-content'.
            float mainStretchSize = TaffyMath.maybeMax(
                TaffyMath.maybeSub(percentResolutionMainSize, itemMarginMain), 0f);

            float resolvedFlexBasis = (flexBasis.isAuto() || flexBasis.isContent())
                                      ? NaN
                                      : flexBasis.maybeResolve(percentResolutionMainSize);
            // Add box_sizing_adjustment to resolved flex-basis (matching Rust's maybe_add behavior)
            if (!isNaN(resolvedFlexBasis)) {
                resolvedFlexBasis = resolvedFlexBasis + boxSizingAdjustmentMain;
            }

            // A. If the item has a definite used flex basis, that's the flex base size.
            //
            // ST-01/FL-17 (#1099/#1103/#1247): a flex-basis that is a sizing keyword is used in
            // place of the main size property. 'stretch' resolves to an exact (definite) size
            // while the other keywords determine the available-space constraint the item is
            // measured under. A keyword that cannot be resolved in the current context behaves as
            // 'content', i.e. it is measured under the default constraint below.
            Float exactBasis = null;
            AvailableSpace keywordMainAvailableSpace = null;

            if (!isNaN(resolvedFlexBasis)) {
                exactBasis = resolvedFlexBasis;
            } else if (flexBasis.isContent()) {
                // A flex basis of 'content' indicates an automatic size based on the item's
                // content: the item is measured (ignoring its main size property) under the
                // default constraint below.
            } else if (flexBasis.isSizingKeyword()) {
                SizingKeyword.Resolution resolution = SizingKeyword.resolve(
                    flexBasis, mainStretchSize, percentResolutionMainSize);
                if (resolution != null && resolution.isExact()) {
                    exactBasis = resolution.getExactValue();
                } else if (resolution != null) {
                    keywordMainAvailableSpace = resolution.getMeasureAvailableSpace();
                }
            } else if (!isNaN(mainSize) && flexBasis.isAuto()) {
                // B/A. If flex basis is auto, and there's a definite main size, use that.
                // Only flex-basis: auto may fall back to the main size (#1286): an unresolved
                // percentage (or any other non-auto basis, e.g. content) is treated as content and
                // measured below.
                exactBasis = mainSize;
            } else if (flexBasis.isAuto()) {
                // A main size that is a sizing keyword either resolves to an exact size or
                // determines the available-space constraint the item is measured under
                // (upstream #1099/#1103).
                TaffyDimension mainDim = isRow ? childStyle.getSize().width : childStyle.getSize().height;
                SizingKeyword.Resolution resolution = SizingKeyword.resolve(
                    mainDim, mainStretchSize, percentResolutionMainSize);
                if (resolution != null && resolution.isExact()) {
                    exactBasis = resolution.getExactValue();
                } else if (resolution != null) {
                    keywordMainAvailableSpace = resolution.getMeasureAvailableSpace();
                }
            }

            float basis;
            if (exactBasis != null) {
                basis = exactBasis;
            } else {
                // E. Otherwise, size the item into the available space using its used flex basis
                //    in place of its main size, treating a value of content as max-content.
                //    The flex base size is the item's resulting main size.
                //    A sizing-keyword constraint resolved above replaces the default constraint.
                AvailableSpace mainAvailSpace = keywordMainAvailableSpace;
                if (mainAvailSpace == null) {
                    // Map AvailableSpace::Definite to AvailableSpace::MaxContent
                    boolean mainIsMinContent = isRow
                                               ? availableSpace.width.isMinContent()
                                               : availableSpace.height.isMinContent();
                    mainAvailSpace = mainIsMinContent ? AvailableSpace.minContent() : AvailableSpace.maxContent();
                }

                // Cross axis: use computed crossAxisAvailableSpace
                TaffySize<AvailableSpace> childAvailSpace = isRow
                                                            ? new TaffySize<>(mainAvailSpace, crossAxisAvailableSpace)
                                                            : new TaffySize<>(crossAxisAvailableSpace, mainAvailSpace);

                // Use measureChildSize instead of performChildLayout to avoid setting 
                // unroundedLayout prematurely. The flex base size calculation should only
                // compute sizes, not perform full layout (matching Rust's measure_child_size).
                FloatSize measuredSize = layoutComputer.measureChildSize(
                    item.nodeId,
                    childKnownDimensions,
                    childParentSize,
                    childAvailSpace,
                    // Flex base size must NOT incorporate explicit min-size (min-size is applied later when clamping the
                    // hypothetical size). We keep CONTENT_SIZE here and apply max-size/aspect-ratio behavior in leaf sizing.
                    SizingMode.CONTENT_SIZE,
                    new TaffyLine<>(false, false)
                );

                basis = isRow ? measuredSize.width : measuredSize.height;
            }

            item.flexBasis = Math.max(basis, paddingBorderMain); // Floor at padding+border
            item.innerFlexBasis = Math.max(0, item.flexBasis - paddingBorderMain);

            // Calculate resolved minimum main size (CSS 4.5. Automatic Minimum Size of Flex Items)
            // https://www.w3.org/TR/css-flexbox-1/#min-size-auto
            float minMain = isRow ? item.minSize.width : item.minSize.height;
            // FL-19 (#989): the automatic minimum and the hypothetical main size are clamped by
            // the main size transferred through the aspect ratio, not by the raw max-size.
            float transferredMinMain = isRow ? transferredMinSize.width : transferredMinSize.height;
            float transferredMaxMain = isRow ? transferredMaxSize.width : transferredMaxSize.height;
            // For the "specified size suggestion" in §4.5, use the RAW style value (not AR-derived).
            // item.size may include aspect-ratio derivation from generateFlexItems, which would
            // incorrectly clamp the auto minimum. The specified size should only be from explicit style.
            TaffyDimension mainDimStyle = isRow ? childStyle.getSize().width : childStyle.getSize().height;
            float styleMainSize;
            if (mainDimStyle != null && !mainDimStyle.isAuto()) {
                float resolved = mainDimStyle.maybeResolve(isRow ? nodeInnerSize.width : nodeInnerSize.height);
                styleMainSize = isNaN(resolved) ? NaN : resolved + boxSizingAdjustmentMain;
            } else {
                styleMainSize = NaN;
            }

            // Check if overflow creates automatic min-size (visible overflow = auto min, else 0)
            boolean hasOverflowMain = isRow
                                      ? (item.overflow.x != Overflow.VISIBLE)
                                      : (item.overflow.y != Overflow.VISIBLE);

            if (!isNaN(minMain)) {
                // Explicit min-size takes precedence
                item.resolvedMinimumMainSize = minMain;
            } else if (hasOverflowMain) {
                // Non-visible overflow means automatic min = 0
                item.resolvedMinimumMainSize = 0;
            } else {
                // Compute min-content size for automatic minimum
                // Use crossAxisAvailableSpace which was computed earlier (matching Rust)
                TaffySize<AvailableSpace> minContentAvailSpace = isRow
                                                                 ? new TaffySize<>(AvailableSpace.minContent(), crossAxisAvailableSpace)
                                                                 : new TaffySize<>(crossAxisAvailableSpace, AvailableSpace.minContent());

                // Use childKnownDimensions (computed above with stretch logic applied)
                // Per Rust flexbox.rs line 805: reuses child_known_dimensions for min-content measurement
                // Use measureChildSize to avoid setting unroundedLayout prematurely
                FloatSize minMeasuredSize = layoutComputer.measureChildSize(
                    item.nodeId,
                    childKnownDimensions,
                    childParentSize,
                    minContentAvailSpace,
                    SizingMode.CONTENT_SIZE,
                    new TaffyLine<>(false, false)
                );

                // 4.5: clamp min-content size by specified size and max-size
                float clampedMinContent = isRow ? minMeasuredSize.width : minMeasuredSize.height;
                if (!isNaN(styleMainSize)) {
                    clampedMinContent = Math.min(clampedMinContent, styleMainSize);
                }
                if (!isNaN(transferredMaxMain)) {
                    clampedMinContent = Math.min(clampedMinContent, transferredMaxMain);
                }

                // Floor at padding+border
                item.resolvedMinimumMainSize = Math.max(clampedMinContent, paddingBorderMain);
            }

            // Hypothetical main size = flex_basis clamped by min/max
            // Note: In taffy, flex_basis, hypothetical_inner_size, min_size, max_size all include padding+border

            // Use resolvedMinimumMainSize if available (includes content-based minimum)
            // FL-19 (#989): transferred min/max clamp the hypothetical main size but do not
            // participate in resolving flexible lengths or clamping the final size.
            float hypotheticalInnerMinMain = item.resolvedMinimumMainSize;
            if (!Float.isNaN(transferredMinMain)) {
                hypotheticalInnerMinMain = Math.max(hypotheticalInnerMinMain, transferredMinMain);
            }
            hypotheticalInnerMinMain = Math.max(hypotheticalInnerMinMain, paddingBorderMain);

            // Clamp flex_basis by min and max
            // Per CSS spec, min takes precedence over max, so apply max first then min
            float hypotheticalInnerMain = item.flexBasis;
            if (!isNaN(transferredMaxMain)) {
                hypotheticalInnerMain = Math.min(hypotheticalInnerMain, transferredMaxMain);
            }
            hypotheticalInnerMain = Math.max(hypotheticalInnerMain, hypotheticalInnerMinMain);

            float marginMain = isRow
                               ? item.margin.left + item.margin.right
                               : item.margin.top + item.margin.bottom;
            float hypotheticalOuterMain = hypotheticalInnerMain + marginMain;

            if (isRow) {
                item.hypotheticalInnerSize = new FloatSize(hypotheticalInnerMain, 0f);
                item.hypotheticalOuterSize = new FloatSize(hypotheticalOuterMain, 0f);
            } else {
                item.hypotheticalInnerSize = new FloatSize(0f, hypotheticalInnerMain);
                item.hypotheticalOuterSize = new FloatSize(0f, hypotheticalOuterMain);
            }
        }
    }

    /**
     * Determine the container's main size when it's not explicitly set.
     * This is needed before resolving flexible lengths so that flex grow/shrink can work correctly.
     */
    private float determineContainerMainSize(
        List<FlexLine> lines,
        FloatRect contentBoxInset,
        FloatSize minSize,
        FloatSize maxSize,
        float mainGap,
        float crossGap,
        FlexDirection flexDirection,
        boolean isWrap,
        boolean isBalance,
        int lineCount,
        TaffySize<AvailableSpace> availableSpace,
        FloatSize nodeInnerSize) {

        boolean isRow = flexDirection.isRow();
        float mainContentBoxInset = isRow
                                    ? contentBoxInset.left + contentBoxInset.right
                                    : contentBoxInset.top + contentBoxInset.bottom;

        AvailableSpace mainAvail = isRow ? availableSpace.width : availableSpace.height;

        // Determine outer main size
        float outerMainSize;

        if (mainAvail.isDefinite()) {
            // Definite available space: use longest line length based on flex-basis (matches Rust Definite branch)
            float longestLineLength = 0;
            for (FlexLine line : lines) {
                float lineLength = 0;
                for (FlexItem item : line.items) {
                    // IMPORTANT: Use hypothetical outer size, not raw flex-basis.
                    // The hypothetical size is flex-basis clamped by min/max (and includes padding/border per Taffy).
                    // Using flex-basis here breaks cases where flex-basis is 0 but min-size is definite
                    // (e.g. intrinsic_sizing_main_size_min_size), leading to negative free space and bad offsets.
                    float itemOuterMain = isRow ? item.hypotheticalOuterSize.width : item.hypotheticalOuterSize.height;
                    lineLength += itemOuterMain;
                }
                lineLength += mainGap * (line.items.size() - 1);
                longestLineLength = Math.max(longestLineLength, lineLength);
            }

            outerMainSize = longestLineLength + mainContentBoxInset;

            if (isBalance) {
                // FL-21 (#1105): a balanced container can produce multiple lines that all fit
                // within the available space (via flex-line-count), in which case fit-content
                // sizing uses its max-content size: the longest line when items are balanced
                // across the minimum line count without a size limit. Percentages continue to
                // resolve against the full container size.
                int itemCount = 0;
                for (FlexLine line : lines) {
                    itemCount += line.items.size();
                }
                if (itemCount > 0) {
                    float[] itemLengths = new float[itemCount];
                    int index = 0;
                    for (FlexLine line : lines) {
                        for (FlexItem item : line.items) {
                            itemLengths[index++] = isRow
                                                   ? item.hypotheticalOuterSize.width
                                                   : item.hypotheticalOuterSize.height;
                        }
                    }
                    int[] itemCounts = FlexBalance.balancedLineItemCounts(
                        itemLengths, POSITIVE_INFINITY, mainGap, Math.max(lineCount, 1));
                    float widestLineLength = 0;
                    index = 0;
                    for (int count : itemCounts) {
                        float lineLength = mainGap * Math.max(0, count - 1);
                        for (int i = 0; i < count; i++) {
                            lineLength += itemLengths[index + i];
                        }
                        widestLineLength = Math.max(widestLineLength, lineLength);
                        index += count;
                    }
                    float maxContentSize = widestLineLength + mainContentBoxInset;
                    outerMainSize = Math.max(outerMainSize, Math.min(maxContentSize, mainAvail.getValue()));
                }
            } else if (lines.size() > 1) {
                // If multiple lines (wrapping), ensure at least the available main-axis size
                outerMainSize = Math.max(outerMainSize, mainAvail.getValue());
            }
        } else if (mainAvail.isMinContent()) {
            // Min-content: for wrap, longest-line based size. Our line collection already handles MinContent by placing each
            // item on its own line, which makes this equivalent.
            float longestLineLength = 0;
            for (FlexLine line : lines) {
                float lineLength = 0;
                for (FlexItem item : line.items) {
                    float itemOuterMain = isRow ? item.hypotheticalOuterSize.width : item.hypotheticalOuterSize.height;
                    lineLength += itemOuterMain;
                }
                lineLength += mainGap * (line.items.size() - 1);
                longestLineLength = Math.max(longestLineLength, lineLength);
            }
            outerMainSize = longestLineLength + mainContentBoxInset;
        } else {
            // Max-content: intrinsic sizing using the CSS Flexbox intrinsic item contribution algorithm.
            // See: https://www.w3.org/TR/css-flexbox-1/#intrinsic-item-contributions
            // 
            // This implements the full algorithm including content_flex_fraction calculation
            // which handles scaling based on flex-grow/flex-shrink factors.
            float maxLineSum = 0;

            for (FlexLine line : lines) {
                // Step 1: Calculate content_contribution and content_flex_fraction for each item
                for (FlexItem item : line.items) {
                    TaffyStyle childStyle = layoutComputer.getTree().getStyle(item.nodeId);
                    float marginMain = isRow
                                       ? item.margin.left + item.margin.right
                                       : item.margin.top + item.margin.bottom;

                    float styleMinMain = isRow ? item.minSize.width : item.minSize.height;
                    float stylePreferredMain = isRow ? item.size.width : item.size.height;
                    float styleMaxMain = isRow ? item.maxSize.width : item.maxSize.height;

                    // The spec seems a bit unclear on this point (my initial reading was that the .maybe_max(style_preferred) should
                    // not be included here), however for row containers this matches both Chrome and Firefox as of 9th March 2023.
                    // For column containers the flex base size alone is the clamping basis (as the spec says): a column item's
                    // `height` must not inflate the container's max-content size beyond the item's flex-basis.
                    //
                    // Spec: https://www.w3.org/TR/css-flexbox-1/#intrinsic-item-contributions
                    // Spec modification: https://www.w3.org/TR/css-flexbox-1/#change-2016-max-contribution
                    // Issue: https://github.com/w3c/csswg-drafts/issues/1435
                    // Gentest: padding_border_overrides_size_flex_basis_0.html
                    // Gentest: blockflex_min_content_ignores_own_height_as_percentage_basis.html
                    float clampingBasis = isRow && !Float.isNaN(stylePreferredMain)
                                          ? Math.max(item.flexBasis, stylePreferredMain)
                                          : item.flexBasis;

                    // flex_basis acts as min when flex_shrink == 0
                    float flexBasisMin = item.flexShrink == 0f ? clampingBasis : NaN;
                    // flex_basis acts as max when flex_grow == 0
                    float flexBasisMax = item.flexGrow == 0f ? clampingBasis : NaN;

                    // Compute effective min: max(style_min, flex_basis_min, resolved_minimum_main_size)
                    float minMainSize = item.resolvedMinimumMainSize;
                    if (!Float.isNaN(styleMinMain)) minMainSize = Math.max(minMainSize, styleMinMain);

                    // Compute effective max: min(style_max, flex_basis_max) or infinity
                    float maxMainSize = POSITIVE_INFINITY;
                    if (!Float.isNaN(styleMaxMain)) maxMainSize = styleMaxMain;
                    if (!Float.isNaN(flexBasisMax)) {
                        maxMainSize = Math.min(maxMainSize, flexBasisMax);
                    }

                    if (!Float.isNaN(flexBasisMin)) {
                        float cappedFlexBasisMin = maxMainSize != POSITIVE_INFINITY
                                                   ? Math.min(flexBasisMin, maxMainSize)
                                                   : flexBasisMin;
                        minMainSize = Math.max(minMainSize, cappedFlexBasisMin);
                    }

                    float intrinsicFlexBasis = item.flexBasis;
                    if (maxMainSize != POSITIVE_INFINITY) {
                        intrinsicFlexBasis = Math.min(intrinsicFlexBasis, maxMainSize);
                    }

                    float contentContribution;

                    // Fast path: if clamping values are such that max <= min, avoid measuring content
                    if (!Float.isNaN(stylePreferredMain) && (maxMainSize <= minMainSize || maxMainSize <= stylePreferredMain)) {
                        // pref.min(max).max(min) - clamp preferred by max, then ensure at least min
                        float clamped = Math.min(stylePreferredMain, maxMainSize);
                        clamped = Math.max(clamped, minMainSize);
                        contentContribution = clamped + marginMain;
                    } else if (maxMainSize <= minMainSize) {
                        contentContribution = minMainSize + marginMain;
                    } else if (item.overflow.x != Overflow.VISIBLE || item.overflow.y != Overflow.VISIBLE) {
                        // Scroll container: use flex-basis
                        contentContribution = intrinsicFlexBasis + marginMain;
                    } else if (!Float.isNaN(stylePreferredMain)) {
                        // FL-18 (#1155): if the item has a definite preferred main size then that is
                        // its content contribution (an inherent-size measure of the item would return
                        // it), floored by the item's main-axis padding+border, so measuring the item
                        // can be skipped. Min/max clamping is applied like the measured contributions.
                        float itemPaddingBorderMain = isRow
                                                      ? item.padding.left + item.padding.right
                                                        + item.border.left + item.border.right
                                                      : item.padding.top + item.padding.bottom
                                                        + item.border.top + item.border.bottom;
                        float innerMainSize = Math.max(stylePreferredMain, itemPaddingBorderMain);
                        if (isRow) {
                            contentContribution = TaffyMath.maybeClamp(
                                innerMainSize + marginMain, styleMinMain, styleMaxMain);
                        } else {
                            contentContribution = TaffyMath.maybeClamp(
                                Math.max(innerMainSize, item.flexBasis) + marginMain, styleMinMain, styleMaxMain);
                        }
                    } else {
                        // Measure child content size in main axis
                        AvailableSpace outerCrossAvail = isRow ? availableSpace.height : availableSpace.width;
                        float crossAxisParentSize = isRow ? nodeInnerSize.height : nodeInnerSize.width;
                        float itemMinCross = isRow ? item.minSize.height : item.minSize.width;
                        float itemMaxCross = isRow ? item.maxSize.height : item.maxSize.width;
                        // FL-10 (#1234): the child's margin is excluded from its available space.
                        float marginCross = isRow
                                            ? item.margin.top + item.margin.bottom
                                            : item.margin.left + item.margin.right;

                        AvailableSpace crossAxisAvailableSpace;
                        if (outerCrossAvail.isDefinite()) {
                            float val = outerCrossAvail.getValue();
                            // FL-21 (#1105): flex-line-count divides definite cross-axis available
                            // space between the requested number of lines.
                            float clampedCross = dividedCrossSpace(
                                !Float.isNaN(crossAxisParentSize) ? crossAxisParentSize : val, lineCount, crossGap);
                            // FL-10: subtract the item margin before flooring/clamping.
                            clampedCross = Math.max(clampedCross - marginCross, 0f);
                            if (!Float.isNaN(itemMinCross)) clampedCross = Math.max(clampedCross, itemMinCross);
                            if (!Float.isNaN(itemMaxCross)) clampedCross = Math.min(clampedCross, itemMaxCross);
                            crossAxisAvailableSpace = AvailableSpace.definite(clampedCross);
                        } else if (outerCrossAvail.isMinContent()) {
                            crossAxisAvailableSpace = !Float.isNaN(itemMinCross) ? AvailableSpace.definite(itemMinCross) : AvailableSpace.minContent();
                        } else {
                            crossAxisAvailableSpace = !Float.isNaN(itemMaxCross) ? AvailableSpace.definite(itemMaxCross) : AvailableSpace.maxContent();
                        }

                        TaffySize<AvailableSpace> childAvailSpace = isRow
                                                                    ? new TaffySize<>(availableSpace.width, crossAxisAvailableSpace)
                                                                    : new TaffySize<>(crossAxisAvailableSpace, availableSpace.height);

                        // Known dimensions: clear main axis, handle stretch for cross
                        float knownWidth = item.size.width;
                        float knownHeight = item.size.height;
                        if (isRow) {
                            knownWidth = NaN;
                        } else {
                            knownHeight = NaN;
                        }

                        // FL-20 (#1208): clamp the definite cross size by the cross min/max
                        // sizes (aspect-ratio transferred), as determineFlexBaseSize does: an
                        // item measured wider than its max-width wraps onto fewer lines and
                        // under-reports its height.
                        FloatSize transferredMinSize = maybeApplyAspectRatio(item.minSize, item.aspectRatio);
                        FloatSize transferredMaxSize = maybeApplyAspectRatio(item.maxSize, item.aspectRatio);
                        if (isRow) {
                            knownHeight = TaffyMath.maybeClamp(
                                knownHeight, transferredMinSize.height, transferredMaxSize.height);
                        } else {
                            knownWidth = TaffyMath.maybeClamp(
                                knownWidth, transferredMinSize.width, transferredMaxSize.width);
                        }

                        // Stretch cross size if needed
                        if (item.alignSelf == AlignSelf.STRETCH) {
                            boolean crossMarginsAuto = isRow
                                                       ? (item.marginIsAuto.top || item.marginIsAuto.bottom)
                                                       : (item.marginIsAuto.left || item.marginIsAuto.right);
                            float crossKnown = isRow ? knownHeight : knownWidth;
                            if (!crossMarginsAuto && Float.isNaN(crossKnown)) {
                                float cross = crossAxisAvailableSpace.intoOption();
                                if (!Float.isNaN(cross)) {
                                    // FL-10: the available space already excludes the item's
                                    // margins; do not subtract them a second time.
                                    // FL-09: a stretched item's known cross size is floored at zero.
                                    crossKnown = Math.max(cross, 0f);
                                    if (isRow) {
                                        knownHeight = crossKnown;
                                    } else {
                                        knownWidth = crossKnown;
                                    }
                                }
                            }
                        }

                        FloatSize childKnownDimensions = new FloatSize(knownWidth, knownHeight);

                        // Measure only, as upstream's measure_child_size does: performing the layout here would
                        // overwrite the item's descendants and its final layout cache entry with this measuring pass.
                        // FL-18 (#1155): measure the content size, not the inherent size. The
                        // item's own preferred main size is applied by the fast path above, so this
                        // call must not let the leaf re-apply the style size (it also keeps the
                        // cache entries separate from inherent-size measures).
                        FloatSize measured = layoutComputer.measureChildSize(
                            item.nodeId,
                            childKnownDimensions,
                            nodeInnerSize,
                            childAvailSpace,
                            SizingMode.CONTENT_SIZE,
                            new TaffyLine<>(false, false)
                        );

                        float measuredMain = isRow ? measured.width : measured.height;

                        // FL-18 (#1155): a known cross size is transferred through the item's
                        // aspect ratio and floors the measured content size.
                        Float itemAspectRatio = childStyle.getAspectRatio();
                        float knownCross = isRow ? childKnownDimensions.height : childKnownDimensions.width;
                        if (itemAspectRatio != null && !Float.isNaN(itemAspectRatio) && !Float.isNaN(knownCross)) {
                            float transferredMain = isRow
                                                    ? knownCross * itemAspectRatio
                                                    : knownCross / itemAspectRatio;
                            measuredMain = Math.max(measuredMain, transferredMain);
                        }

                        // FL-04 (#1164): floor the inner contribution first, then add the main-axis margin,
                        // and only then clamp by the style min/max. The margin must not be clamped away.
                        //
                        // Asymmetric behavior between row and column containers (matches Webkit/Firefox):
                        // - Row containers: use content size clamped by min/max
                        // - Column containers: content size is at least flex-basis
                        float innerMainSize = measuredMain;
                        if (isRow) {
                            contentContribution = TaffyMath.maybeClamp(
                                innerMainSize + marginMain, styleMinMain, styleMaxMain);
                        } else {
                            contentContribution = TaffyMath.maybeClamp(
                                Math.max(innerMainSize, item.flexBasis) + marginMain, styleMinMain, styleMaxMain);
                        }
                    }

                    // Calculate content_flex_fraction
                    // This represents the "flex fraction" needed to size the item to its content contribution
                    // FL-05: contentContribution already includes the main-axis margin, so the flex
                    // base size comparison must subtract it again (upstream #1335).
                    float diff = contentContribution - marginMain - intrinsicFlexBasis;
                    if (diff > 0.0f) {
                        item.contentFlexFraction = diff / Math.max(1.0f, item.flexGrow);
                    } else if (diff < 0.0f) {
                        float scaledShrinkFactor = Math.max(1.0f, item.flexShrink) * item.innerFlexBasis;
                        item.contentFlexFraction = diff / scaledShrinkFactor;
                    } else {
                        item.contentFlexFraction = 0.0f;
                    }
                }

                // Step 2: Calculate the final item sizes using flex_contribution
                // Add each item's flex base size to the product of:
                //   - its flex grow factor (or scaled flex shrink factor, if the chosen max-content flex fraction was negative)
                //   - the chosen max-content flex fraction
                float itemMainSizeSum = 0;
                // A wrapping row container is at least as large as its largest item (which matters when
                // another item has a negative margin). This matches Chrome; Firefox does not apply this floor.
                boolean floorByLargestItem = isWrap && isRow;
                float largestItemSize = 0f;
                for (FlexItem item : line.items) {
                    float flexFraction = item.contentFlexFraction;

                    float flexContribution;
                    if (item.contentFlexFraction > 0.0f) {
                        flexContribution = Math.max(1.0f, item.flexGrow) * flexFraction;
                    } else if (item.contentFlexFraction < 0.0f) {
                        float scaledShrinkFactor = Math.max(1.0f, item.flexShrink) * item.innerFlexBasis;
                        flexContribution = scaledShrinkFactor == 0.0f ? 0.0f : scaledShrinkFactor * flexFraction;
                    } else {
                        flexContribution = 0.0f;
                    }

                    float styleMaxMain = isRow ? item.maxSize.width : item.maxSize.height;
                    float maxMainSize = POSITIVE_INFINITY;
                    if (!Float.isNaN(styleMaxMain)) maxMainSize = styleMaxMain;
                    if (item.flexGrow == 0f) {
                        float stylePreferredMain = isRow ? item.size.width : item.size.height;
                        // FL-03: for column containers the clamping basis is the flex base size alone.
                        float clampingBasis = isRow && !Float.isNaN(stylePreferredMain)
                                              ? Math.max(item.flexBasis, stylePreferredMain)
                                              : item.flexBasis;
                        maxMainSize = Math.min(maxMainSize, clampingBasis);
                    }
                    float intrinsicFlexBasis = item.flexBasis;
                    if (maxMainSize != POSITIVE_INFINITY) {
                        intrinsicFlexBasis = Math.min(intrinsicFlexBasis, maxMainSize);
                    }

                    // FL-05: the line sum is an outer contribution, so the item margin belongs in it.
                    // The target sizes are not written back here: the main-axis resolution below owns
                    // them, and writing intrinsic-sizing values into targetSize leaked into final layout.
                    float marginMain = isRow
                                       ? item.margin.left + item.margin.right
                                       : item.margin.top + item.margin.bottom;
                    float itemSize = intrinsicFlexBasis + flexContribution + marginMain;

                    if (floorByLargestItem) {
                        largestItemSize = Math.max(largestItemSize, itemSize);
                    }

                    itemMainSizeSum += itemSize;
                }

                float gapSum = mainGap * Math.max(0, line.items.size() - 1);
                maxLineSum = Math.max(maxLineSum, Math.max(largestItemSize, itemMainSizeSum + gapSum));
            }

            outerMainSize = maxLineSum + mainContentBoxInset;
        }

        // Apply min/max constraints
        float minMain = isRow ? minSize.width : minSize.height;
        float maxMain = isRow ? maxSize.width : maxSize.height;
        outerMainSize = TaffyMath.clamp(outerMainSize, minMain, maxMain);

        // Ensure at least content box inset
        outerMainSize = Math.max(outerMainSize, mainContentBoxInset);

        // Return inner main size
        return Math.max(outerMainSize - mainContentBoxInset, 0f);
    }

    private List<FlexLine> collectIntoFlexLines(
        List<FlexItem> items,
        TaffySize<AvailableSpace> availableSpace,
        float mainGap,
        boolean isWrap,
        boolean isBalance,
        int lineCount,
        FlexDirection flexDirection,
        FloatSize minSize,
        FloatSize maxSize,
        float knownInnerMainSize) {

        List<FlexLine> lines = new ArrayList<>();
        boolean isRow = flexDirection.isRow();

        if (!isWrap) {
            FlexLine line = new FlexLine();
            line.items = new ArrayList<>(items);
            line.crossSize = 0;
            line.offsetCross = 0;
            lines.add(line);
            return lines;
        }

        // Determine main axis available space for wrapping (matching Rust's collect_flex_lines)
        float mainMaxSize = isRow ? maxSize.width : maxSize.height;
        float mainMinSize = isRow ? minSize.width : minSize.height;
        AvailableSpace outerMainAvail = isRow ? availableSpace.width : availableSpace.height;

        // TODO(FL-15/C01): this approximates upstream's `has_definite_main_size` flag. A known main
        // size derived from the container's own content (e.g. a flex-basis percentage that could not
        // resolve) is indeterminate here; the real flag arrives with the definiteness work.
        boolean hasDefiniteMainSize = !Float.isNaN(knownInnerMainSize);

        AvailableSpace mainAxisAvailableSpace;
        if (isBalance && !hasDefiniteMainSize) {
            // FL-21 (#1105): balanced line breaking requires a definite main size. Otherwise items
            // are balanced without a size limit, matching how the container was sized under a
            // min/max-content constraint (upstream collect_balanced_flex_lines).
            mainAxisAvailableSpace = AvailableSpace.maxContent();
        } else if (!Float.isNaN(mainMaxSize)) {
            // If max-size is set, use it as the constraint
            float innerVal = outerMainAvail.intoOption();
            float constrainedSize = !Float.isNaN(innerVal) ? innerVal : mainMaxSize;
            // FL-06 (#1101): when the container's main size is indefinite it is at most max-size,
            // so max-size (not the available space) is the limit that items wrap against.
            if (!hasDefiniteMainSize) {
                constrainedSize = Math.min(constrainedSize, mainMaxSize);
            }
            if (!Float.isNaN(mainMinSize)) constrainedSize = Math.max(constrainedSize, mainMinSize);
            mainAxisAvailableSpace = AvailableSpace.definite(constrainedSize);
        } else if (!isRow && !hasDefiniteMainSize && outerMainAvail.isDefinite()) {
            // FL-06 (#1176): a column container's automatic main size is content-based, so definite
            // available space handed down by an ancestor does not constrain where its lines wrap.
            // Automatic widths resolve against available space, so rows do wrap against it.
            mainAxisAvailableSpace = AvailableSpace.maxContent();
        } else {
            mainAxisAvailableSpace = outerMainAvail;
        }

        // If MaxContent, all items go in one line (no wrapping). A balanced container instead
        // balances without a size limit, still honouring the requested minimum line count.
        if (mainAxisAvailableSpace.isMaxContent()) {
            if (isBalance) {
                return collectBalancedFlexLines(items, POSITIVE_INFINITY, mainGap, lineCount, flexDirection);
            }
            FlexLine line = new FlexLine();
            line.items = new ArrayList<>(items);
            line.crossSize = 0;
            line.offsetCross = 0;
            lines.add(line);
            return lines;
        }

        // If MinContent, each item gets its own line
        if (mainAxisAvailableSpace.isMinContent()) {
            for (FlexItem item : items) {
                FlexLine line = new FlexLine();
                line.items = new ArrayList<>();
                line.items.add(item);
                line.crossSize = 0;
                line.offsetCross = 0;
                lines.add(line);
            }
            return lines;
        }

        // Definite available space - wrap based on constraint
        float availMainSize = mainAxisAvailableSpace.getValue();
        if (isBalance) {
            return collectBalancedFlexLines(items, availMainSize, mainGap, lineCount, flexDirection);
        }
        float lineMainSize = 0;
        List<FlexItem> currentLine = new ArrayList<>();

        for (FlexItem item : items) {
            float itemOuterMain = isRow
                                  ? item.hypotheticalOuterSize.width
                                  : item.hypotheticalOuterSize.height;

            if (!currentLine.isEmpty()) {
                float newSize = lineMainSize + mainGap + itemOuterMain;
                if (newSize > availMainSize) {
                    FlexLine line = new FlexLine();
                    line.items = currentLine;
                    line.crossSize = 0;
                    line.offsetCross = 0;
                    lines.add(line);
                    currentLine = new ArrayList<>();
                    lineMainSize = 0;
                }
            }

            currentLine.add(item);
            lineMainSize += (currentLine.size() > 1 ? mainGap : 0) + itemOuterMain;
        }

        if (!currentLine.isEmpty()) {
            FlexLine line = new FlexLine();
            line.items = currentLine;
            line.crossSize = 0;
            line.offsetCross = 0;
            lines.add(line);
        }

        return lines;
    }

    /**
     * Collect flex items into balanced flex lines ({@code flex-wrap: balance}), such that the
     * largest line is as small as possible (FL-21, upstream #1105).
     *
     * @param lineLimit     the container's inner main size, or positive infinity when the main
     *                      size is indefinite
     * @param minLineCount  the requested minimum number of lines ({@code flex-line-count})
     */
    private List<FlexLine> collectBalancedFlexLines(
        List<FlexItem> items,
        float lineLimit,
        float mainGap,
        int minLineCount,
        FlexDirection flexDirection) {

        List<FlexLine> lines = new ArrayList<>();
        if (items.isEmpty()) {
            return lines;
        }

        boolean isRow = flexDirection.isRow();
        float[] itemSizes = new float[items.size()];
        for (int index = 0; index < items.size(); index++) {
            FlexItem item = items.get(index);
            itemSizes[index] = isRow ? item.hypotheticalOuterSize.width : item.hypotheticalOuterSize.height;
        }

        int[] itemCounts = FlexBalance.balancedLineItemCounts(itemSizes, lineLimit, mainGap, minLineCount);
        int index = 0;
        for (int count : itemCounts) {
            FlexLine line = new FlexLine();
            line.items = new ArrayList<>(items.subList(index, index + count));
            line.crossSize = 0;
            line.offsetCross = 0;
            lines.add(line);
            index += count;
        }
        return lines;
    }

    /**
     * When a multi-line container requests a minimum number of lines ({@code flex-line-count}),
     * definite cross-axis available space for measuring items is divided between the requested
     * number of lines, after subtracting the cross-axis gaps between them (FL-21, upstream #1105).
     * A line count of 1 (the default) leaves the available space unchanged.
     */
    private static float dividedCrossSpace(float crossAvailableSpace, int lineCount, float crossGap) {
        if (lineCount > 1) {
            return (crossAvailableSpace - (lineCount - 1) * crossGap) / lineCount;
        }
        return crossAvailableSpace;
    }

    private void resolveFlexibleLengths(
        FlexLine line,
        FlexDirection flexDirection,
        float innerMainSize,
        float mainGap) {

        boolean isRow = flexDirection.isRow();
        float totalMainAxisGap = mainGap * (line.items.size() - 1);

        // 1. Determine the used flex factor
        float totalHypotheticalOuterMainSize = 0;
        for (FlexItem item : line.items) {
            totalHypotheticalOuterMainSize += isRow ? item.hypotheticalOuterSize.width : item.hypotheticalOuterSize.height;
        }

        float usedFlexFactor = totalMainAxisGap + totalHypotheticalOuterMainSize;
        float nodeInnerMain = !Float.isNaN(innerMainSize) ? innerMainSize : 0;
        boolean growing = usedFlexFactor < nodeInnerMain;
        boolean shrinking = usedFlexFactor > nodeInnerMain;
        boolean exactlySized = !growing && !shrinking;

        // 2. Size inflexible items - freeze them
        for (FlexItem item : line.items) {
            float innerTargetSize = isRow ? item.hypotheticalInnerSize.width : item.hypotheticalInnerSize.height;

            if (isRow) {
                item.targetSize = new FloatSize(innerTargetSize, item.targetSize.height);
            } else {
                item.targetSize = new FloatSize(item.targetSize.width, innerTargetSize);
            }

            // Freeze if: exactly sized, no flex, or flex basis violates hypothetical size
            if (exactlySized
                || (item.flexGrow == 0.0f && item.flexShrink == 0.0f)
                || (growing && item.flexBasis > (isRow ? item.hypotheticalInnerSize.width : item.hypotheticalInnerSize.height))
                || (shrinking && item.flexBasis < (isRow ? item.hypotheticalInnerSize.width : item.hypotheticalInnerSize.height))) {

                item.frozen = true;
                float marginMain = isRow ? item.margin.left + item.margin.right : item.margin.top + item.margin.bottom;
                float outerTargetSize = innerTargetSize + marginMain;

                if (isRow) {
                    item.outerTargetSize = new FloatSize(outerTargetSize, item.outerTargetSize.height);
                } else {
                    item.outerTargetSize = new FloatSize(item.outerTargetSize.width, outerTargetSize);
                }
            }
        }

        if (exactlySized) {
            return;
        }

        // 3. Calculate initial free space
        float usedSpace = totalMainAxisGap;
        for (FlexItem item : line.items) {
            if (item.frozen) {
                usedSpace += isRow ? item.outerTargetSize.width : item.outerTargetSize.height;
            } else {
                float marginMain = isRow ? item.margin.left + item.margin.right : item.margin.top + item.margin.bottom;
                usedSpace += item.flexBasis + marginMain;
            }
        }
        float initialFreeSpace = nodeInnerMain - usedSpace;

        // 4. Loop
        while (true) {
            // a. Check for flexible items
            boolean allFrozen = true;
            for (FlexItem item : line.items) {
                if (!item.frozen) {
                    allFrozen = false;
                    break;
                }
            }
            if (allFrozen) {
                break;
            }

            // b. Calculate remaining free space
            usedSpace = totalMainAxisGap;
            for (FlexItem item : line.items) {
                if (item.frozen) {
                    usedSpace += isRow ? item.outerTargetSize.width : item.outerTargetSize.height;
                } else {
                    float marginMain = isRow ? item.margin.left + item.margin.right : item.margin.top + item.margin.bottom;
                    usedSpace += item.flexBasis + marginMain;
                }
            }

            float sumFlexGrow = 0;
            float sumFlexShrink = 0;
            for (FlexItem item : line.items) {
                if (!item.frozen) {
                    sumFlexGrow += item.flexGrow;
                    sumFlexShrink += item.flexShrink;
                }
            }

            float freeSpace;
            if (growing && sumFlexGrow < 1.0f) {
                float scaledFreeSpace = initialFreeSpace * sumFlexGrow - totalMainAxisGap;
                float remainingFreeSpace = nodeInnerMain - usedSpace;
                freeSpace = Math.min(scaledFreeSpace, remainingFreeSpace);
            } else if (shrinking && sumFlexShrink < 1.0f) {
                float scaledFreeSpace = initialFreeSpace * sumFlexShrink - totalMainAxisGap;
                float remainingFreeSpace = nodeInnerMain - usedSpace;
                freeSpace = Math.max(scaledFreeSpace, remainingFreeSpace);
            } else {
                freeSpace = nodeInnerMain - usedSpace;
            }

            // c. Distribute free space proportionally
            if (Math.abs(freeSpace) > 0.0001f) {
                if (growing && sumFlexGrow > 0) {
                    for (FlexItem item : line.items) {
                        if (!item.frozen) {
                            float newMain = item.flexBasis + freeSpace * (item.flexGrow / sumFlexGrow);
                            if (isRow) {
                                item.targetSize = new FloatSize(newMain, item.targetSize.height);
                            } else {
                                item.targetSize = new FloatSize(item.targetSize.width, newMain);
                            }
                        }
                    }
                } else if (shrinking && sumFlexShrink > 0) {
                    // Calculate scaled shrink factors
                    float sumScaledShrinkFactor = 0;
                    for (FlexItem item : line.items) {
                        if (!item.frozen) {
                            sumScaledShrinkFactor += item.innerFlexBasis * item.flexShrink;
                        }
                    }

                    if (sumScaledShrinkFactor > 0) {
                        for (FlexItem item : line.items) {
                            if (!item.frozen) {
                                float scaledShrinkFactor = item.innerFlexBasis * item.flexShrink;
                                float newMain = item.flexBasis + freeSpace * (scaledShrinkFactor / sumScaledShrinkFactor);
                                if (isRow) {
                                    item.targetSize = new FloatSize(newMain, item.targetSize.height);
                                } else {
                                    item.targetSize = new FloatSize(item.targetSize.width, newMain);
                                }
                            }
                        }
                    }
                }
            }

            // d. Fix min/max violations
            float totalViolation = 0;
            for (FlexItem item : line.items) {
                if (!item.frozen) {
                    float targetMain = isRow ? item.targetSize.width : item.targetSize.height;
                    float minMain = isRow ? item.minSize.width : item.minSize.height;
                    float maxMain = isRow ? item.maxSize.width : item.maxSize.height;

                    // Use resolvedMinimumMainSize if it's positive, otherwise use minMain
                    float resolvedMin;
                    if (item.resolvedMinimumMainSize > 0) {
                        resolvedMin = item.resolvedMinimumMainSize;
                    } else {
                        resolvedMin = minMain;
                    }

                    // Clamp target size - handle null values
                    // Per CSS spec, min takes precedence over max, so apply max first then min
                    float clamped = targetMain;
                    if (!Float.isNaN(maxMain) && clamped > maxMain) {
                        clamped = maxMain;
                    }
                    if (!Float.isNaN(resolvedMin) && clamped < resolvedMin) {
                        clamped = resolvedMin;
                    }
                    clamped = Math.max(0, clamped);

                    item.violation = clamped - targetMain;

                    if (isRow) {
                        item.targetSize = new FloatSize(clamped, item.targetSize.height);
                        item.outerTargetSize = new FloatSize(clamped + item.margin.left + item.margin.right, item.outerTargetSize.height);
                    } else {
                        item.targetSize = new FloatSize(item.targetSize.width, clamped);
                        item.outerTargetSize = new FloatSize(item.outerTargetSize.width, clamped + item.margin.top + item.margin.bottom);
                    }

                    totalViolation += item.violation;
                }
            }

            // e. Freeze over-flexed items
            for (FlexItem item : line.items) {
                if (!item.frozen) {
                    if (totalViolation > 0) {
                        // Positive total: freeze items with min violations
                        item.frozen = item.violation > 0;
                    } else if (totalViolation < 0) {
                        // Negative total: freeze items with max violations
                        item.frozen = item.violation < 0;
                    } else {
                        // Zero: freeze all
                        item.frozen = true;
                    }
                }
            }
        }
    }

    /**
     * Determine the hypothetical cross size of each item.
     * Step 7 of 9.4. Cross Size Determination in CSS Flexbox spec.
     */
    private void determineHypotheticalCrossSize(
        FlexLine line,
        FlexDirection flexDirection,
        float innerMainSize,
        FloatSize nodeInnerSize,
        TaffySize<AvailableSpace> availableSpace,
        boolean isWrap,
        int lineCount,
        float crossGap) {

        boolean isRow = flexDirection.isRow();

        for (FlexItem item : line.items) {
            float paddingBorderCross = isRow
                                       ? item.padding.top + item.padding.bottom + item.border.top + item.border.bottom
                                       : item.padding.left + item.padding.right + item.border.left + item.border.right;

            // FL-10 (#1234): available space is the child's border-box space, so the item's
            // margins are already subtracted by the parent.
            float marginCross = isRow
                                ? item.margin.top + item.margin.bottom
                                : item.margin.left + item.margin.right;
            float marginMain = isRow
                               ? item.margin.left + item.margin.right
                               : item.margin.top + item.margin.bottom;

            // Use inner main size as the known main size for child layout (minus the item margin)
            float containerMainSize = Math.max(innerMainSize - marginMain, 0f);

            // Get child's explicit cross size if set
            float itemCross = isRow ? item.size.height : item.size.width;
            // FL-19 (#989): sizes transferred through the aspect ratio clamp the hypothetical
            // cross size (https://github.com/w3c/csswg-drafts/issues/10997).
            FloatSize transferredMinSize = maybeApplyAspectRatio(item.minSize, item.aspectRatio);
            FloatSize transferredMaxSize = maybeApplyAspectRatio(item.maxSize, item.aspectRatio);
            float itemMinCross = isRow ? transferredMinSize.height : transferredMinSize.width;
            float itemMaxCross = isRow ? transferredMaxSize.height : transferredMaxSize.width;

            // Clamp cross size
            if (!Float.isNaN(itemCross)) {
                itemCross = TaffyMath.clamp(itemCross, itemMinCross, itemMaxCross);
                itemCross = Math.max(itemCross, paddingBorderCross);
            }

            // Calculate available cross space
            AvailableSpace availCross = isRow ? availableSpace.height : availableSpace.width;
            // Clamp available space by min/max if definite
            if (availCross.isDefinite()) {
                float val = availCross.getValue();
                // FL-21 (#1105): flex-line-count divides definite cross-axis available space
                // between the requested number of lines.
                val = dividedCrossSpace(val, lineCount, crossGap);
                // FL-10: subtract the item margin before flooring/clamping.
                // FL-09: clamp the cross constraint at zero before applying min/max.
                val = Math.max(val - marginCross, 0f);
                if (!Float.isNaN(itemMinCross)) val = Math.max(val, itemMinCross);
                if (!Float.isNaN(itemMaxCross)) val = Math.min(val, itemMaxCross);
                val = Math.max(val, paddingBorderCross);
                availCross = AvailableSpace.definite(val);
            }

            // ST-01/FL-17 (#1099): a cross size that is a sizing keyword (min-content,
            // max-content, fit-content, fit-content(...)) determines the available-space
            // constraint the item is measured under. 'stretch' is not resolved here: it
            // stretches to the flex line like align-self: stretch (determineUsedCrossSize).
            TaffyStyle childStyle = layoutComputer.getTree().getStyle(item.nodeId);
            TaffyDimension crossDim = isRow ? childStyle.getSize().height : childStyle.getSize().width;
            float crossResolutionBasis = isRow ? nodeInnerSize.height : nodeInnerSize.width;
            // FL-21 (#1105): a bare fit-content/stretch cross size is measured against the
            // divided stretch size; percentages continue to resolve against the full size.
            float crossStretchSize = TaffyMath.maybeSub(
                dividedCrossSpace(crossResolutionBasis, lineCount, crossGap), marginCross);
            SizingKeyword.Resolution crossKeyword = SizingKeyword.resolve(
                crossDim, crossStretchSize, crossResolutionBasis);
            if (crossKeyword != null && crossKeyword.isMeasure()) {
                availCross = crossKeyword.getMeasureAvailableSpace();
            }

            float childInnerCross;
            if (!Float.isNaN(itemCross)) {
                childInnerCross = itemCross;
            } else {
                // Measure child to get cross size using measureChildSize
                // For wrap containers with AR items, don't pass the flex-grown main size.
                // This prevents AR from inflating the hypothetical cross based on flex growth,
                // keeping the container cross based on pre-flex-growth AR values (matching browser behavior).
                float knownMain = isWrap ? NaN : (isRow ? item.targetSize.width : item.targetSize.height);
                FloatSize knownDims = isRow
                                      ? new FloatSize(knownMain, itemCross)
                                      : new FloatSize(itemCross, knownMain);

                TaffySize<AvailableSpace> childAvailSpace = isRow
                                                            ? new TaffySize<>(
                    !Float.isNaN(containerMainSize) ? AvailableSpace.definite(containerMainSize) : availableSpace.width,
                    availCross)
                                                            : new TaffySize<>(
                    availCross,
                    !Float.isNaN(containerMainSize) ? AvailableSpace.definite(containerMainSize) : availableSpace.height);

                // Use measureChildSize instead of performChildLayout to avoid setting 
                // unroundedLayout prematurely during hypothetical size calculation
                FloatSize measuredSize = layoutComputer.measureChildSize(
                    item.nodeId,
                    knownDims,
                    nodeInnerSize,
                    childAvailSpace,
                    SizingMode.CONTENT_SIZE,
                    new TaffyLine<>(false, false)
                );

                float measuredCross = isRow ? measuredSize.height : measuredSize.width;
                childInnerCross = TaffyMath.clamp(measuredCross, itemMinCross, itemMaxCross);
                childInnerCross = Math.max(childInnerCross, paddingBorderCross);
            }

            float childOuterCross = childInnerCross + marginCross;

            // Update hypothetical sizes
            if (isRow) {
                item.hypotheticalInnerSize = new FloatSize(item.hypotheticalInnerSize.width, childInnerCross);
                item.hypotheticalOuterSize = new FloatSize(item.hypotheticalOuterSize.width, childOuterCross);
            } else {
                item.hypotheticalInnerSize = new FloatSize(childInnerCross, item.hypotheticalInnerSize.height);
                item.hypotheticalOuterSize = new FloatSize(childOuterCross, item.hypotheticalOuterSize.height);
            }
        }
    }

    /**
     * Calculate the baselines of children for baseline alignment.
     * This should be called before calculateCrossSize.
     */
    private void calculateChildrenBaselines(
        List<FlexLine> flexLines,
        FloatSize nodeInnerSize,
        TaffySize<AvailableSpace> availableSpace,
        FlexDirection flexDirection) {

        boolean isRow = flexDirection.isRow();

        // Only compute baselines for flex rows because we only support baseline alignment in the cross axis
        // where that axis is also the inline axis
        if (!isRow) {
            return;
        }

        for (FlexLine line : flexLines) {
            // Check if any items have baseline alignment
            boolean hasBaselineItem = false;
            for (FlexItem item : line.items) {
                if (participatesInBaselineAlignment(item, isRow)) {
                    hasBaselineItem = true;
                    break;
                }
            }
            if (!hasBaselineItem) {
                continue;
            }

            // Count baseline-aligned items
            int baselineCount = 0;
            for (FlexItem item : line.items) {
                if (participatesInBaselineAlignment(item, isRow)) {
                    baselineCount++;
                }
            }

            // If a flex line has one or zero items participating in baseline alignment
            // then baseline alignment is a no-op so we skip
            if (baselineCount <= 1) {
                continue;
            }

            for (FlexItem item : line.items) {
                // Only calculate baselines for children participating in baseline alignment
                if (!participatesInBaselineAlignment(item, isRow)) {
                    continue;
                }

                // Perform child layout to get the baseline
                FloatSize knownDimensions = new FloatSize(
                    item.targetSize.width,
                    item.hypotheticalInnerSize.height
                );

                // FL-10 (#1234): the available space passed to the child excludes its margins.
                float childMarginHorizontal = item.margin.left + item.margin.right;
                float childMarginVertical = item.margin.top + item.margin.bottom;
                AvailableSpace childAvailWidth = isNaN(nodeInnerSize.width)
                                                 ? availableSpace.width
                                                 : AvailableSpace.definite(nodeInnerSize.width);
                AvailableSpace childAvailHeight = isNaN(nodeInnerSize.height)
                                                  ? availableSpace.height
                                                  : AvailableSpace.definite(nodeInnerSize.height);
                TaffySize<AvailableSpace> childAvailableSpace = new TaffySize<>(
                    childAvailWidth.maybeSub(childMarginHorizontal).maybeMax(0f),
                    childAvailHeight.maybeSub(childMarginVertical).maybeMax(0f)
                );

                LayoutOutput output = layoutComputer.performChildLayout(
                    item.nodeId,
                    knownDimensions,
                    nodeInnerSize,
                    childAvailableSpace,
                    SizingMode.CONTENT_SIZE,
                    new TaffyLine<>(false, false)
                );

                // A scroll-container item's baseline is clamped to its border box, and a scroll
                // container without a baseline synthesizes one at its border-box bottom edge
                // (upstream #995, moved into the shared helper by #1323). A non-scroll item with no
                // baseline falls back to its border-box height, as upstream does with
                // child_first_baseline(...).unwrap_or(size.height).
                float height = output.size().height;
                float childBaseline = BaselineHelper.childFirstBaseline(
                    output, item.overflow.y.isScrollContainer());

                // baseline = first_baseline (or height if none) + top margin
                item.baseline = (!Float.isNaN(childBaseline) ? childBaseline : height) + item.margin.top;
            }
        }
    }

    private void calculateCrossSize(
        List<FlexLine> lines,
        FloatSize nodeInnerSize,
        TaffySize<AvailableSpace> availableSpace,
        FlexDirection flexDirection,
        TaffyStyle containerStyle,
        FloatSize minSize,
        FloatSize maxSize,
        FloatRect contentBoxInset,
        boolean crossIsArDerived) {

        boolean isRow = flexDirection.isRow();
        float innerCrossSize = isRow ? nodeInnerSize.height : nodeInnerSize.width;

        // Get original cross-axis available space for fallback
        AvailableSpace crossAxisAvailSpace = isRow ? availableSpace.height : availableSpace.width;

        // If the flex container is single-line and has a definite cross size,
        // the cross size of the flex line is the flex container's inner cross size.
        // Exception: when cross is AR-derived, allow content to grow beyond the AR value.
        boolean isWrap = containerStyle.getFlexWrap().isMultiLine();
        if (!isWrap && lines.size() == 1 && !Float.isNaN(innerCrossSize) && !crossIsArDerived) {
            // When container has a definite cross size, line cross size equals the container's inner cross size
            // No need to apply min/max here - those constraints were already applied when determining container size
            float lineCrossSize = innerCrossSize;
            lineCrossSize = Math.max(lineCrossSize, 0);

            var first = lines.get(0);
            first.crossSize = lineCrossSize;

            // Still need to calculate each item's cross size for layout. FL-19 (#989): clamp by
            // the sizes transferred through the aspect ratio, matching the hypothetical cross size.
            for (FlexItem item : first.items) {
                float itemCrossSize = isRow ? item.size.height : item.size.width;
                FloatSize transferredMinSize = maybeApplyAspectRatio(item.minSize, item.aspectRatio);
                FloatSize transferredMaxSize = maybeApplyAspectRatio(item.maxSize, item.aspectRatio);
                float itemMinCross = isRow ? transferredMinSize.height : transferredMinSize.width;
                float itemMaxCross = isRow ? transferredMaxSize.height : transferredMaxSize.width;

                float paddingBorderCross = isRow
                                           ? item.padding.top + item.padding.bottom + item.border.top + item.border.bottom
                                           : item.padding.left + item.padding.right + item.border.left + item.border.right;

                float marginCross = isRow
                                    ? item.margin.top + item.margin.bottom
                                    : item.margin.left + item.margin.right;
                // FL-10 (#1234): the available space passed to the child excludes its margins.
                float availableCross = Math.max(innerCrossSize - marginCross, 0f);

                float crossSize;
                if (!Float.isNaN(itemCrossSize)) {
                    crossSize = TaffyMath.clamp(itemCrossSize, itemMinCross, itemMaxCross);
                } else {
                    // Measure to get content size
                    float targetMain = isRow ? item.targetSize.width : item.targetSize.height;
                    FloatSize knownDimensions = isRow
                                                ? new FloatSize(targetMain, NaN)
                                                : new FloatSize(NaN, targetMain);

                    // ST-01/FL-17: a sizing keyword on the cross axis replaces the definite
                    // line cross size as the constraint for this measurement. `stretch` is
                    // ignored here (it is staged from the line in determineUsedCrossSize).
                    TaffyStyle itemStyle = layoutComputer.getTree().getStyle(item.nodeId);
                    TaffyDimension crossDim = isRow ? itemStyle.getSize().height : itemStyle.getSize().width;
                    float crossResolutionBasis = isRow ? nodeInnerSize.height : nodeInnerSize.width;
                    float crossStretchSize = TaffyMath.maybeSub(crossResolutionBasis, marginCross);
                    SizingKeyword.Resolution crossKeyword = SizingKeyword.resolve(
                        crossDim, crossStretchSize, crossResolutionBasis);
                    AvailableSpace measuredCrossAvail =
                        crossKeyword != null && crossKeyword.isMeasure()
                        ? crossKeyword.getMeasureAvailableSpace()
                        : AvailableSpace.definite(availableCross);

                    TaffySize<AvailableSpace> availSpace = new TaffySize<>(
                        isRow ? AvailableSpace.definite(targetMain) : measuredCrossAvail,
                        isRow ? measuredCrossAvail : AvailableSpace.definite(targetMain)
                    );

                    // Use measureChildSize to avoid setting unroundedLayout prematurely
                    FloatSize measuredSize = layoutComputer.measureChildSize(
                        item.nodeId,
                        knownDimensions,
                        nodeInnerSize,
                        availSpace,
                        SizingMode.CONTENT_SIZE,
                        new TaffyLine<>(false, false)
                    );

                    crossSize = isRow ? measuredSize.height : measuredSize.width;
                    crossSize = TaffyMath.clamp(crossSize, itemMinCross, itemMaxCross);
                }
                crossSize = Math.max(crossSize, paddingBorderCross);

                if (isRow) {
                    item.targetSize = new FloatSize(item.targetSize.width, crossSize);
                    item.outerTargetSize = new FloatSize(item.outerTargetSize.width, crossSize + marginCross);
                } else {
                    item.targetSize = new FloatSize(crossSize, item.targetSize.height);
                    item.outerTargetSize = new FloatSize(crossSize + marginCross, item.outerTargetSize.height);
                }
            }
            return;
        }

        for (FlexLine line : lines) {
            float maxCross = 0;

            for (FlexItem item : line.items) {
                // Get item's cross size from resolved style size. FL-19 (#989): clamp by the
                // transferred min/max sizes (aspect ratio applied on demand).
                float itemCrossSize = isRow ? item.size.height : item.size.width;
                FloatSize transferredMinSize = maybeApplyAspectRatio(item.minSize, item.aspectRatio);
                FloatSize transferredMaxSize = maybeApplyAspectRatio(item.maxSize, item.aspectRatio);
                float itemMinCross = isRow ? transferredMinSize.height : transferredMinSize.width;
                float itemMaxCross = isRow ? transferredMaxSize.height : transferredMaxSize.width;

                // Apply min/max constraints
                float paddingBorderCross = isRow
                                           ? item.padding.top + item.padding.bottom + item.border.top + item.border.bottom
                                           : item.padding.left + item.padding.right + item.border.left + item.border.right;

                // Use hypothetical cross size from step 7 (matching Rust behavior).
                // Don't re-measure with flex-resolved main to avoid AR inflation
                // that would incorrectly increase line cross size.
                float crossSize;
                if (!Float.isNaN(itemCrossSize)) {
                    crossSize = TaffyMath.clamp(itemCrossSize, itemMinCross, itemMaxCross);
                } else {
                    crossSize = isRow ? item.hypotheticalInnerSize.height : item.hypotheticalInnerSize.width;
                }
                crossSize = Math.max(crossSize, paddingBorderCross);

                float marginCross = isRow
                                    ? item.margin.top + item.margin.bottom
                                    : item.margin.left + item.margin.right;

                if (isRow) {
                    item.targetSize = new FloatSize(item.targetSize.width, crossSize);
                    item.outerTargetSize = new FloatSize(item.outerTargetSize.width, crossSize + marginCross);
                } else {
                    item.targetSize = new FloatSize(crossSize, item.targetSize.height);
                    item.outerTargetSize = new FloatSize(crossSize + marginCross, item.outerTargetSize.height);
                }

                // Use hypothetical outer size from step 7 for line cross calculation (don't overwrite)
                maxCross = Math.max(maxCross, crossSize + marginCross);
            }

            // Calculate max baseline among baseline-aligned items
            float maxBaseline = 0;
            for (FlexItem item : line.items) {
                if (participatesInBaselineAlignment(item, isRow)) {
                    maxBaseline = Math.max(maxBaseline, item.baseline);
                }
            }

            // Calculate line cross size considering baseline alignment
            // Per CSS spec:
            // 1. Collect all baseline-aligned items: Find largest distance from baseline to cross-start and cross-end
            // 2. Among non-baseline items, find largest hypothetical outer cross size
            // 3. Line cross size is max of these
            float lineCrossSize = 0;
            for (FlexItem item : line.items) {
                float itemHypotheticalOuterCross = isRow ? item.hypotheticalOuterSize.height : item.hypotheticalOuterSize.width;

                if (participatesInBaselineAlignment(item, isRow)) {
                    // Baseline-aligned: max_baseline - baseline + hypothetical_outer_cross
                    float contribution = maxBaseline - item.baseline + itemHypotheticalOuterCross;
                    lineCrossSize = Math.max(lineCrossSize, contribution);
                } else {
                    // Non-baseline: just hypothetical outer cross size
                    lineCrossSize = Math.max(lineCrossSize, itemHypotheticalOuterCross);
                }
            }

            line.crossSize = lineCrossSize;
        }

        // If the flex container is single-line, then clamp the line's cross-size 
        // to be within the container's computed min and max cross sizes.
        // Note: This is separate from align-content: stretch which is handled later.
        // isWrap is already defined at the start of this method
        if (!isWrap && lines.size() == 1) {
            // Use the already-resolved min/max sizes (includes box-sizing adjustment)
            float resolvedMinCross = isRow ? minSize.height : minSize.width;
            float resolvedMaxCross = isRow ? maxSize.height : maxSize.width;

            // Use pre-computed contentBoxInset for padding+border
            float containerPaddingBorderCross = isRow
                                                ? contentBoxInset.top + contentBoxInset.bottom
                                                : contentBoxInset.left + contentBoxInset.right;

            // Subtract padding+border from min/max to get inner content area constraints
            // Note: min/max already include box-sizing adjustment, so we just need to remove padding+border
            float minInnerCross = !Float.isNaN(resolvedMinCross) ?
                                  Math.max(0, resolvedMinCross - containerPaddingBorderCross) : NaN;
            float maxInnerCross = !Float.isNaN(resolvedMaxCross) ?
                                  Math.max(0, resolvedMaxCross - containerPaddingBorderCross) : NaN;

            FlexLine line = lines.get(0);

            // When cross is AR-derived, floor with AR value before clamping with min/max
            if (crossIsArDerived && !Float.isNaN(innerCrossSize)) {
                line.crossSize = Math.max(line.crossSize, innerCrossSize);
            }

            // Clamp line.crossSize with container's min/max inner cross size
            line.crossSize = TaffyMath.clamp(line.crossSize, minInnerCross, maxInnerCross);
        }
    }

    private void distributeRemainingMainSpace(
        List<FlexLine> lines,
        TaffyStyle containerStyle,
        float innerMainSize,
        float mainGap,
        FlexDirection flexDirection) {

        if (Float.isNaN(innerMainSize)) return;

        boolean isRow = flexDirection.isRow();
        boolean isReverse = flexDirection.isReverse();

        // Note: For RTL, we DON'T change the offset calculation here.
        // The RTL adjustment happens in performFinalLayout when calculating x coordinates.
        // This keeps offsetMain as "distance from the logical start" which we then
        // convert to screen coordinates in performFinalLayout.

        // ST-04 (#1255): explicit justify-content: normal behaves as flex-start in flex
        // layout. The legacy AUTO sentinel already resolves to flex-start in the getter.
        JustifyContent justify = AlignmentHelper.resolveNormal(
            containerStyle.getJustifyContent(), JustifyContent.FLEX_START);

        for (FlexLine line : lines) {
            float usedSpace = 0;
            int autoMarginCount = 0;

            for (FlexItem item : line.items) {
                usedSpace += isRow ? item.outerTargetSize.width : item.outerTargetSize.height;

                if (isRow) {
                    if (item.marginIsAuto.left) autoMarginCount++;
                    if (item.marginIsAuto.right) autoMarginCount++;
                } else {
                    if (item.marginIsAuto.top) autoMarginCount++;
                    if (item.marginIsAuto.bottom) autoMarginCount++;
                }
            }
            usedSpace += mainGap * (line.items.size() - 1);

            float freeSpace = innerMainSize - usedSpace;

            // Distribute to auto margins first
            if (autoMarginCount > 0 && freeSpace > 0) {
                float autoMarginSize = freeSpace / autoMarginCount;
                for (FlexItem item : line.items) {
                    if (isRow) {
                        // Update offsetMain for left auto margin
                        if (item.marginIsAuto.left) {
                            item.margin = new FloatRect(autoMarginSize, item.margin.right, item.margin.top, item.margin.bottom);
                            item.outerTargetSize = new FloatSize(item.outerTargetSize.width + autoMarginSize, item.outerTargetSize.height);
                        }
                        // Update outerTargetSize for right auto margin (affects next item's position)
                        if (item.marginIsAuto.right) {
                            item.margin = new FloatRect(item.margin.left, autoMarginSize, item.margin.top, item.margin.bottom);
                            item.outerTargetSize = new FloatSize(item.outerTargetSize.width + autoMarginSize, item.outerTargetSize.height);
                        }
                    } else {
                        // Update offsetMain for top auto margin
                        if (item.marginIsAuto.top) {
                            item.margin = new FloatRect(item.margin.left, item.margin.right, autoMarginSize, item.margin.bottom);
                            item.outerTargetSize = new FloatSize(item.outerTargetSize.width, item.outerTargetSize.height + autoMarginSize);
                        }
                        // Update outerTargetSize for bottom auto margin (affects next item's position)
                        if (item.marginIsAuto.bottom) {
                            item.margin = new FloatRect(item.margin.left, item.margin.right, item.margin.top, autoMarginSize);
                            item.outerTargetSize = new FloatSize(item.outerTargetSize.width, item.outerTargetSize.height + autoMarginSize);
                        }
                    }
                }
            } else {
                // Apply justify-content through the shared ST-02 helper
                // (AlignmentHelper.applyAlignmentFallback / computeAlignmentOffset), which mirrors
                // upstream's apply_alignment_fallback and compute_alignment_offset:
                // https://www.w3.org/TR/css-align-3/ and https://github.com/w3c/csswg-drafts/issues/10154
                float initialOffset = 0;
                float betweenOffset = 0;
                int itemCount = line.items.size();

                // FL-08: the distribution fallbacks differ: stretch falls back to plain
                // flex-start (no implicit safe), while space-* gain implicit safe semantics and
                // then fall back to logical start on overflow. A safe modifier falls back to
                // START when the items overflow. Both steps live in the helper.
                JustifyContent effectiveJustify = AlignmentHelper.applyAlignmentFallback(
                    justify, freeSpace, itemCount);

                // Calculate initial offset (for first item). For RTL, we DON'T adjust START/END
                // here; the RTL coordinate transformation happens in performFinalLayout.
                initialOffset = AlignmentHelper.computeAlignmentOffset(
                    freeSpace, itemCount, 0f, effectiveJustify, isReverse, true);

                // Calculate between offset (for subsequent items) - only for positive space.
                // The fixed main-axis gap is added by performFinalLayout, so pass gap = 0: the
                // helper's non-first branch then returns just the distributed extra space.
                betweenOffset = AlignmentHelper.computeAlignmentOffset(
                    freeSpace, itemCount, 0f, effectiveJustify, isReverse, false);

                // Set offsetMain for each item
                // First item gets initialOffset, subsequent items get accumulated betweenOffset
                float accumulatedBetweenOffset = 0;
                for (int i = 0; i < line.items.size(); i++) {
                    FlexItem item = line.items.get(i);
                    if (i == 0) {
                        item.offsetMain = initialOffset;
                    } else {
                        accumulatedBetweenOffset += betweenOffset;
                        item.offsetMain = initialOffset + accumulatedBetweenOffset;
                    }
                }
            }
        }
    }

    private void alignItemsOnCrossAxis(List<FlexLine> lines, FlexDirection flexDirection, boolean isWrapReverse) {
        boolean isRow = flexDirection.isRow();

        for (FlexLine line : lines) {
            // Calculate max baseline for this line (needed for baseline alignment)
            float maxBaseline = 0;
            // FL-11 (#987): the distance from the baseline-aligned group's baseline to the
            // cross-end edge of the item ("max_baseline_to_bottom_distance" upstream). In a
            // wrap-reverse container the cross axis is flipped, so this distance is what anchors
            // the group to the cross-start edge (visually the bottom of the line).
            float maxBaselineToBottomDistance = 0;
            for (FlexItem item : line.items) {
                if (participatesInBaselineAlignment(item, isRow)) {
                    maxBaseline = Math.max(maxBaseline, item.baseline);
                    float itemCross = isRow ? item.outerTargetSize.height : item.outerTargetSize.width;
                    maxBaselineToBottomDistance = Math.max(maxBaselineToBottomDistance, itemCross - item.baseline);
                }
            }

            for (FlexItem item : line.items) {
                float itemCross = isRow ? item.outerTargetSize.height : item.outerTargetSize.width;
                float freeSpace = line.crossSize - itemCross;

                // 13. Resolve cross-axis auto margins
                // If a flex item has auto cross-axis margins, distribute free space to those margins
                boolean crossStartAuto = isRow ? item.marginIsAuto.top : item.marginIsAuto.left;
                boolean crossEndAuto = isRow ? item.marginIsAuto.bottom : item.marginIsAuto.right;

                if (crossStartAuto && crossEndAuto) {
                    // Both margins are auto: split free space equally
                    float autoMargin = freeSpace / 2.0f;
                    if (isRow) {
                        item.margin = new FloatRect(item.margin.left, item.margin.right, autoMargin, autoMargin);
                    } else {
                        item.margin = new FloatRect(autoMargin, autoMargin, item.margin.top, item.margin.bottom);
                    }
                    item.offsetCross = 0;
                    continue;
                } else if (crossStartAuto) {
                    // Only start margin is auto: give all free space to start margin
                    if (isRow) {
                        item.margin = new FloatRect(item.margin.left, item.margin.right, freeSpace, item.margin.bottom);
                    } else {
                        item.margin = new FloatRect(freeSpace, item.margin.right, item.margin.top, item.margin.bottom);
                    }
                    item.offsetCross = 0;
                    continue;
                } else if (crossEndAuto) {
                    // Only end margin is auto: give all free space to end margin
                    if (isRow) {
                        item.margin = new FloatRect(item.margin.left, item.margin.right, item.margin.top, freeSpace);
                    } else {
                        item.margin = new FloatRect(item.margin.left, freeSpace, item.margin.top, item.margin.bottom);
                    }
                    item.offsetCross = 0;
                    continue;
                }

                // 14. Align all flex items along the cross-axis (no auto margins)
                // Use the align_flex_items_along_cross_axis logic from Rust
                item.offsetCross = computeAlignItemsOffset(
                    item, freeSpace, maxBaseline, maxBaselineToBottomDistance, isRow, isWrapReverse);
            }
        }
    }

    /**
     * Compute the cross-axis offset for a flex item based on align-self.
     * This follows the Rust align_flex_items_along_cross_axis function exactly.
     * <p>
     * {@code START}/{@code END} are physical (they never flip with wrap-reverse), while
     * {@code FLEX_START}/{@code FLEX_END} are flex-relative (they flip). See PA-02.
     * Column containers use the same wrap-reverse flip for the flex-relative values here;
     * their RTL cross-axis mirroring is applied by {@link #performFinalLayout}, which stores the
     * logical cross offset and computes the physical x from the right edge.
     * <p>
     * ST-03 (#1077): {@code SELF_START}/{@code SELF_END} have already been resolved to
     * {@code START}/{@code END} against the item's own direction in {@link #generateFlexItems}.
     * ST-02 (#952): a {@code SAFE_*} value whose item overflows the line falls back to logical
     * {@code START}; a value that fits keeps its requested position with the modifier dropped.
     * <p>
     * {@code maxBaselineToBottomDistance} is the largest distance between a baseline-participating
     * item's baseline and its outer cross-end edge; it anchors the baseline group to the cross-start
     * edge of a wrap-reverse line (upstream #987, FL-11).
     */
    private float computeAlignItemsOffset(
        FlexItem item,
        float freeSpace,
        float maxBaseline,
        float maxBaselineToBottomDistance,
        boolean isRow,
        boolean isWrapReverse) {
        // ST-02 (#952): resolve the safe overflow-position modifier once; the switch below then
        // only sees bare keywords (safe start/end/flex-*/center collapse to START on overflow).
        AlignSelf alignSelf = AlignmentHelper.resolveSelfSafety(item.alignSelf, freeSpace < 0f);

        switch (alignSelf) {
            case START:
                return 0;
            case END:
                return freeSpace;
            case FLEX_START:
                return isWrapReverse ? freeSpace : 0;
            case FLEX_END:
                return isWrapReverse ? 0 : freeSpace;
            case CENTER:
                return freeSpace / 2;
            case BASELINE:
                if (isRow) {
                    if (isWrapReverse) {
                        // FL-11 (#987): in a wrap-reverse container the cross axis is flipped, so the
                        // baseline-aligned group of items is aligned to the cross-start edge, which is
                        // the bottom of the line: line_cross - max_baseline_to_bottom_distance - baseline.
                        float lineCrossSize = freeSpace + item.outerTargetSize.height;
                        return lineCrossSize - maxBaselineToBottomDistance - item.baseline;
                    }
                    return maxBaseline - item.baseline;
                } else {
                    // Until we support vertical writing modes, baseline alignment only makes sense if
                    // the direction is row, so we treat it as flex-start alignment in columns.
                    return isWrapReverse ? freeSpace : 0;
                }
            case STRETCH:
            default:
                // For stretch, if item can't be stretched (has fixed size), 
                // position depends on wrap_reverse
                return isWrapReverse ? freeSpace : 0;
        }
    }

    /**
     * Align all flex lines per align-content.
     * This distributes remaining cross-axis space among lines.
     */
    private void alignFlexLinesPerAlignContent(
        List<FlexLine> lines,
        FloatSize containerSize,
        FloatRect contentBoxInset,
        FloatSize gap,
        TaffyStyle style,
        FlexDirection flexDirection,
        boolean isWrap,
        boolean isWrapReverse,
        float totalLineCrossSize) {

        if (lines.isEmpty()) return;

        // For single-line containers (no wrap), align-content has no effect
        // All items will be on line with offsetCross = 0
        if (!isWrap) {
            // For single line no-wrap, just set offset to 0 and let align-items handle cross positioning
            for (FlexLine line : lines) {
                line.offsetCross = 0;
            }

            // For STRETCH with single line no-wrap, still distribute extra space to line.
            // ST-04 (#1255): explicit normal behaves as stretch for flex containers; AUTO keeps
            // its legacy "not specified" default of stretch as well.
            AlignContent alignContent = AlignmentHelper.resolveNormal(
                style.getAlignContent(), AlignContent.STRETCH);
            if (alignContent == null || alignContent == AlignContent.AUTO) {
                alignContent = AlignContent.STRETCH;
            }
            if (alignContent == AlignContent.STRETCH && lines.size() == 1) {
                boolean isRow = flexDirection.isRow();
                float innerContainerCross = isRow
                                            ? containerSize.height - contentBoxInset.top - contentBoxInset.bottom
                                            : containerSize.width - contentBoxInset.left - contentBoxInset.right;
                var first = lines.get(0);
                float freeSpace = innerContainerCross - first.crossSize;
                if (freeSpace > 0) {
                    first.crossSize += freeSpace;
                }
            }
            return;
        }

        boolean isRow = flexDirection.isRow();
        int numLines = lines.size();
        float crossGap = isRow ? gap.height : gap.width;
        float totalCrossAxisGap = crossGap * (numLines - 1);
        float innerContainerCross = isRow
                                    ? containerSize.height - contentBoxInset.top - contentBoxInset.bottom
                                    : containerSize.width - contentBoxInset.left - contentBoxInset.right;
        float freeSpace = innerContainerCross - totalLineCrossSize - totalCrossAxisGap;

        // The raw align-content value controls whether we stretch line cross sizes.
        // This matches Rust where line stretching is handled by `handle_align_content_stretch`
        // and line offsets are computed later using a potentially-fallback alignment mode.
        // ST-04 (#1255): explicit normal behaves as stretch for flex containers.
        AlignContent rawAlignContent = AlignmentHelper.resolveNormal(
            style.getAlignContent(), AlignContent.STRETCH);
        if (rawAlignContent == null || rawAlignContent == AlignContent.AUTO) {
            rawAlignContent = AlignContent.STRETCH;
        }

        // Handle `align-content: stretch` by increasing each line's cross size equally.
        // This consumes the free space so subsequent offset alignment sees freeSpace == 0.
        if (rawAlignContent == AlignContent.STRETCH && freeSpace > 0) {
            float extraPerLine = freeSpace / numLines;
            for (FlexLine line : lines) {
                line.crossSize += extraPerLine;
            }
            freeSpace = 0;
        }

        // Apply alignment fallback per Rust `apply_alignment_fallback`
        // https://www.w3.org/TR/css-align-3/ and https://github.com/w3c/csswg-drafts/issues/10154
        // FL-08 (#1363): stretch falls back to plain flex-start (no implicit safe), while the
        // space-* keywords gain implicit safe semantics; ST-02 (#952) then falls a safe value back
        // to logical start on overflow. All three steps live in the shared helper.
        AlignContent alignContent = AlignmentHelper.applyAlignmentFallback(
            rawAlignContent, freeSpace, numLines);

        // Compute line offsets following Rust `compute_alignment_offset` semantics.
        // Rust stores offsets as *deltas* between lines and accumulates them during the final layout pass.
        // Java stores `line.offsetCross` as an *absolute* offset from the content-box cross start,
        // so we accumulate into a cursor here.
        // The cross-axis equivalent of justify-content: layoutIsFlexReversed is wrap-reverse.
        // The fixed cross gap is added separately below, so pass gap = 0.
        float initialOffset = AlignmentHelper.computeAlignmentOffset(
            freeSpace, numLines, 0f, alignContent, isWrapReverse, true);
        float betweenExtra = AlignmentHelper.computeAlignmentOffset(
            freeSpace, numLines, 0f, alignContent, isWrapReverse, false);

        // Apply offsets to lines in the same order that `performFinalLayout` will consume them.
        // When wrap-reverse is enabled, lines are laid out in reverse order.
        List<FlexLine> orderedLines = isWrapReverse ? new ArrayList<>(lines) : lines;
        if (isWrapReverse) {
            java.util.Collections.reverse(orderedLines);
        }

        float cursor = 0;
        for (int i = 0; i < orderedLines.size(); i++) {
            FlexLine line = orderedLines.get(i);
            float delta = (i == 0) ? initialOffset : (crossGap + betweenExtra);
            cursor += delta;
            line.offsetCross = cursor;
            cursor += line.crossSize;
        }
    }

    /**
     * Determine the used cross size of each flex item.
     * If a flex item has align-self: stretch, its computed cross size property is auto,
     * and neither of its cross-axis margins are auto, the used outer cross size is the
     * used cross size of its flex line, clamped according to the item's used min and max cross sizes.
     */
    private void determineUsedCrossSize(List<FlexLine> lines, FlexDirection flexDirection, TaffyStyle containerStyle) {
        TaffyTree tree = layoutComputer.getTree();
        boolean isRow = flexDirection.isRow();

        for (FlexLine line : lines) {
            float lineCrossSize = line.crossSize;

            for (FlexItem item : line.items) {
                // Check if item should stretch. Items are generated with their align-self already
                // resolved (AUTO inherited, NORMAL/SELF_* resolved), so this fallback only guards
                // callers that construct items differently.
                AlignSelf alignSelf = item.alignSelf;
                if (alignSelf == null || alignSelf == AlignSelf.AUTO) {
                    AlignItems alignItems = AlignmentHelper.resolveNormal(
                        containerStyle.getAlignItems(), AlignItems.STRETCH);
                    alignSelf = (alignItems != null) ? AlignSelf.fromAlignItems(alignItems) : AlignSelf.STRETCH;
                }

                boolean shouldStretch = alignSelf == AlignSelf.STRETCH;

                // Check if cross size is auto (null in our case)
                float itemCrossSize = isRow ? item.size.height : item.size.width;
                boolean crossSizeIsAuto = Float.isNaN(itemCrossSize);

                // Check if cross margins are not auto - stretch only applies if neither cross margin is auto
                boolean crossStartAuto = isRow ? item.marginIsAuto.top : item.marginIsAuto.left;
                boolean crossEndAuto = isRow ? item.marginIsAuto.bottom : item.marginIsAuto.right;
                boolean marginsAreNotAuto = !crossStartAuto && !crossEndAuto;

                // ST-01 (#1099): a cross size of 'stretch' stretches to the flex line like
                // align-self: stretch, but regardless of the alignment style (upstream's
                // is_stretched predicate). align-self: stretch keeps its auto-size probe.
                TaffyStyle itemStyle = tree.getStyle(item.nodeId);
                TaffyDimension crossDimStyle = isRow ? itemStyle.getSize().height : itemStyle.getSize().width;
                boolean crossIsStretch = crossDimStyle != null && crossDimStyle.isStretch();
                boolean stretchesCross = marginsAreNotAuto
                        && (crossIsStretch || (shouldStretch && crossSizeIsAuto));

                if (stretchesCross) {
                    // Stretch the item to fill the line's cross size
                    float marginCross = isRow
                                        ? item.margin.top + item.margin.bottom
                                        : item.margin.left + item.margin.right;

                    // FL-09: floor the stretched size at zero before applying min/max (upstream order).
                    float stretchedCrossSize = Math.max(lineCrossSize - marginCross, 0f);

                    // Apply min/max constraints
                    float minCross = isRow ? item.minSize.height : item.minSize.width;
                    float maxCross = isRow ? item.maxSize.height : item.maxSize.width;
                    stretchedCrossSize = TaffyMath.clamp(stretchedCrossSize, minCross, maxCross);

                    // Update target size
                    if (isRow) {
                        item.targetSize = new FloatSize(item.targetSize.width, stretchedCrossSize);
                        item.outerTargetSize = new FloatSize(item.outerTargetSize.width, stretchedCrossSize + marginCross);
                    } else {
                        item.targetSize = new FloatSize(stretchedCrossSize, item.targetSize.height);
                        item.outerTargetSize = new FloatSize(stretchedCrossSize + marginCross, item.outerTargetSize.height);
                    }

                    // If the item has an aspect ratio and its main-axis size was auto in the style,
                    // derive the main-axis size from the stretched cross size via aspect ratio.
                    Float aspectRatio = itemStyle.getAspectRatio();
                    if (aspectRatio != null && !Float.isNaN(aspectRatio)) {
                        TaffyDimension mainDimStyle = isRow ? itemStyle.getSize().width : itemStyle.getSize().height;
                        boolean mainSizeIsAuto = mainDimStyle == null || mainDimStyle.isAuto();
                        if (mainSizeIsAuto) {
                            float derivedMain = isRow
                                ? stretchedCrossSize * aspectRatio
                                : stretchedCrossSize / aspectRatio;

                            // Clamp by main-axis min/max and content-based minimum
                            float minMain = isRow ? item.minSize.width : item.minSize.height;
                            float maxMain = isRow ? item.maxSize.width : item.maxSize.height;
                            derivedMain = TaffyMath.clamp(derivedMain, minMain, maxMain);
                            derivedMain = Math.max(derivedMain, item.resolvedMinimumMainSize);

                            // Only update when AR-derived main is LARGER than the current target main.
                            // This preserves flex-grown/shrunk values (spec: step 11 doesn't affect main).
                            float currentMain = isRow ? item.targetSize.width : item.targetSize.height;
                            if (derivedMain > currentMain) {
                                float marginMain = isRow
                                    ? item.margin.left + item.margin.right
                                    : item.margin.top + item.margin.bottom;

                                if (isRow) {
                                    item.targetSize = new FloatSize(derivedMain, item.targetSize.height);
                                    item.outerTargetSize = new FloatSize(derivedMain + marginMain, item.outerTargetSize.height);
                                } else {
                                    item.targetSize = new FloatSize(item.targetSize.width, derivedMain);
                                    item.outerTargetSize = new FloatSize(item.outerTargetSize.width, derivedMain + marginMain);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private FloatSize calculateContainerSize(
        List<FlexLine> lines,
        FloatSize styledBasedKnownDimensions,
        FloatSize paddingBorderSize,
        FloatRect contentBoxInset,
        FloatSize gap,
        FloatSize minSize,
        FloatSize maxSize,
        FlexDirection flexDirection,
        float innerMainSize,
        boolean crossIsArDerived,
        boolean mainIsArDerived) {

        boolean isRow = flexDirection.isRow();

        // Calculate main size from items
        float contentMain = 0;
        for (FlexLine line : lines) {
            float lineMain = 0;
            for (FlexItem item : line.items) {
                lineMain += isRow ? item.outerTargetSize.width : item.outerTargetSize.height;
            }
            lineMain += (isRow ? gap.width : gap.height) * (line.items.size() - 1);
            contentMain = Math.max(contentMain, lineMain);
        }

        // Calculate cross size
        float contentCross = 0;
        for (FlexLine line : lines) {
            contentCross += line.crossSize;
        }
        contentCross += (isRow ? gap.height : gap.width) * (lines.size() - 1);

        // Determine main container size
        float knownMain = styledBasedKnownDimensions != null
                          ? (isRow ? styledBasedKnownDimensions.width : styledBasedKnownDimensions.height)
                          : NaN;
        float mainContentBoxInset = isRow
                                    ? contentBoxInset.left + contentBoxInset.right
                                    : contentBoxInset.top + contentBoxInset.bottom;

        float containerMain;
        if (!Float.isNaN(knownMain)) {
            if (mainIsArDerived) {
                // AR-derived main: content can exceed the AR value
                containerMain = Math.max(knownMain, contentMain + mainContentBoxInset);
            } else {
                // Use known dimension if available
                containerMain = knownMain;
            }
        } else if (!Float.isNaN(innerMainSize)) {
            // Use the computed innerMainSize from determineContainerMainSize directly.
            // Don't take max with contentMain because contentMain may use updated gap values
            // (from re-resolving percentage gaps) while innerMainSize was computed with original gap.
            // This follows Rust behavior where container_size.main is set once and not recalculated.
            containerMain = innerMainSize + mainContentBoxInset;
        } else {
            containerMain = contentMain + mainContentBoxInset;
        }

        float crossContentBoxInset = isRow
                                     ? contentBoxInset.top + contentBoxInset.bottom
                                     : contentBoxInset.left + contentBoxInset.right;
        float knownCross = (styledBasedKnownDimensions != null)
                           ? (isRow ? styledBasedKnownDimensions.height : styledBasedKnownDimensions.width)
                           : NaN;
        float containerCross;
        if (!Float.isNaN(knownCross)) {
            if (crossIsArDerived) {
                // AR-derived cross: content can exceed the AR value
                containerCross = Math.max(knownCross, contentCross + crossContentBoxInset);
            } else {
                containerCross = knownCross;
            }
        } else {
            containerCross = contentCross + crossContentBoxInset;
        }

        // Apply min/max
        containerMain = TaffyMath.clamp(containerMain, isRow ? minSize.width : minSize.height, isRow ? maxSize.width : maxSize.height);
        containerCross = TaffyMath.clamp(containerCross, isRow ? minSize.height : minSize.width, isRow ? maxSize.height : maxSize.width);

        // Ensure at least padding/border size
        containerMain = Math.max(containerMain, isRow ? paddingBorderSize.width : paddingBorderSize.height);
        containerCross = Math.max(containerCross, isRow ? paddingBorderSize.height : paddingBorderSize.width);

        return isRow
               ? new FloatSize(containerMain, containerCross)
               : new FloatSize(containerCross, containerMain);
    }

    /**
     * Performs the final layout pass of all flex items.
     *
     * <p>Besides positioning the items and the out-of-flow children, this pass accumulates their
     * scrollable overflow contributions in the container's padding-box frame (upstream #1372
     * {@code final_layout_pass}) and finalizes the container's {@link ScrollableOverflow}
     * (P4/SO-01/SO-02).</p>
     *
     * @return the container's first vertical baseline and its finalized scrollable overflow
     */
    private FinalLayoutResult performFinalLayout(
        List<FlexLine> lines,
        NodeId node,
        FloatSize containerSize,
        FloatRect contentBoxInset,
        FloatSize gap,
        TaffyStyle containerStyle,
        FlexDirection flexDirection,
        boolean isWrapReverse,
        FloatRect border,
        FloatRect padding,
        FloatSize scrollbarGutter,
        FloatRect paddingBoxInset,
        ScrollableOverflowUtil.Container overflowContainer,
        boolean isRtl,
        boolean isScrollContainer,
        boolean reversedX,
        boolean reversedY) {

        TaffyTree tree = layoutComputer.getTree();
        boolean isRow = flexDirection.isRow();
        boolean isReverse = flexDirection.isReverse();

        float mainGap = isRow ? gap.width : gap.height;

        // Union of the children's contributions, in this container's padding-box frame.
        FloatRect overflowRect = FloatRect.zero();

        // Content box offset
        // For RTL column layouts, cross axis starts from the right edge
        float contentBoxCrossOffset = isRow ? contentBoxInset.top : contentBoxInset.left;

        // Calculate inner cross size for RTL column layout
        float innerCrossSize = isRow
                               ? containerSize.height - contentBoxInset.top - contentBoxInset.bottom
                               : containerSize.width - contentBoxInset.left - contentBoxInset.right;

        // Calculate node inner size for child layout
        FloatSize nodeInnerSize = new FloatSize(
            containerSize.width - contentBoxInset.left - contentBoxInset.right,
            containerSize.height - contentBoxInset.top - contentBoxInset.bottom
        );

        // For calculating container's first baseline
        float firstVerticalBaseline = NaN;
        FlexItem firstBaselineItem = null;

        // Handle wrap reverse
        List<FlexLine> orderedLines = isWrapReverse ? new ArrayList<>(lines) : lines;
        if (isWrapReverse) {
            java.util.Collections.reverse(orderedLines);
        }

        // Find the first item for baseline calculation
        if (!orderedLines.isEmpty()) {
            FlexLine firstLine = orderedLines.get(0);
            if (!firstLine.items.isEmpty()) {
                if (isRow) {
                    // Row containers generate their first baseline from the first item that
                    // participates in baseline alignment, falling back to the first item.
                    for (FlexItem item : firstLine.items) {
                        if (participatesInBaselineAlignment(item, isRow)) {
                            firstBaselineItem = item;
                            break;
                        }
                    }
                    if (firstBaselineItem == null) {
                        firstBaselineItem = firstLine.items.get(0);
                    }
                } else {
                    // FL-13 (#1127): column containers generate their first baseline from the
                    // visually startmost item, which for column-reverse is the last item in flex
                    // order (the items are still in flex order here).
                    firstBaselineItem = isReverse
                                        ? firstLine.items.get(firstLine.items.size() - 1)
                                        : firstLine.items.get(0);
                }
            }
        }

        for (FlexLine line : orderedLines) {
            float crossOffset = contentBoxCrossOffset + line.offsetCross;

            // Handle reverse direction
            // For RTL row layouts, we DON'T reverse items here - the RTL coordinate transformation
            // handles the right-to-left positioning. We only reverse for actual flex-direction: *-reverse.
            List<FlexItem> orderedItems = isReverse ? new ArrayList<>(line.items) : line.items;
            if (isReverse) {
                java.util.Collections.reverse(orderedItems);
            }

            // Calculate starting position for main axis
            float mainAxisSize = isRow ? containerSize.width : containerSize.height;
            // mainOffset accumulates position from the start edge
            // For RTL row, it accumulates from the right edge (contentBoxInset.right)
            // For LTR row, it accumulates from the left edge (contentBoxInset.left)
            float mainOffset;
            if (isRow) {
                mainOffset = isRtl ? contentBoxInset.right : contentBoxInset.left;
            } else {
                mainOffset = contentBoxInset.top;
            }

            for (FlexItem item : orderedItems) {
                // Perform child layout with target size as known dimensions
                FloatSize knownDimensions = item.targetSize;

                // For AR items with auto cross in style where flex resolution changed the main size,
                // re-derive the cross from the flex-resolved main via AR. This allows the child to
                // overflow the container when the flex-grown main produces a larger AR-derived cross.
                TaffyStyle childStyle = tree.getStyle(item.nodeId);
                Float arForFinal = childStyle.getAspectRatio();
                if (arForFinal != null && !Float.isNaN(arForFinal)) {
                    TaffyDimension crossDimStyle = isRow ? childStyle.getSize().height : childStyle.getSize().width;
                    boolean crossAutoInStyle = crossDimStyle == null || crossDimStyle.isAuto();
                    // Only apply when flex resolution changed the main size (target != hypothetical)
                    float hypMain = isRow ? item.hypotheticalInnerSize.width : item.hypotheticalInnerSize.height;
                    float targetMain = isRow ? item.targetSize.width : item.targetSize.height;
                    boolean mainChangedByFlex = Math.abs(targetMain - hypMain) > 0.01f;
                    if (crossAutoInStyle && mainChangedByFlex) {
                        float mainSize = isRow ? knownDimensions.width : knownDimensions.height;
                        float arDerivedCross = isRow ? mainSize / arForFinal : mainSize * arForFinal;
                        // Use the AR-derived cross if it's larger than the stretched/target cross
                        float currentCross = isRow ? knownDimensions.height : knownDimensions.width;
                        if (arDerivedCross > currentCross) {
                            knownDimensions = isRow
                                ? new FloatSize(knownDimensions.width, arDerivedCross)
                                : new FloatSize(arDerivedCross, knownDimensions.height);
                        }
                    }
                }

                // FL-10 (#1234): the available space passed to the child excludes its margins.
                float itemMarginHorizontal = item.margin.left + item.margin.right;
                float itemMarginVertical = item.margin.top + item.margin.bottom;
                TaffySize<AvailableSpace> childAvailableSpace = new TaffySize<>(
                    AvailableSpace.definite(Math.max(containerSize.width - itemMarginHorizontal, 0f)),
                    AvailableSpace.definite(Math.max(containerSize.height - itemMarginVertical, 0f))
                );

                LayoutOutput output = layoutComputer.performChildLayout(
                    item.nodeId,
                    knownDimensions,
                    nodeInnerSize,
                    childAvailableSpace,
                    SizingMode.CONTENT_SIZE,
                    new TaffyLine<>(false, false)
                );

                // Apply offsets
                float x, y;
                if (isRow) {
                    if (isRtl) {
                        // RTL row: x is calculated from the right edge
                        // mainOffset accumulates the position from the right edge
                        float itemMainSize = item.targetSize.width;
                        // Calculate: right edge - mainOffset - item.offsetMain - margin.right - itemSize
                        x = mainAxisSize - mainOffset - item.offsetMain - item.margin.right - itemMainSize;
                    } else {
                        // LTR row: x is calculated from the left edge (normal)
                        x = mainOffset + item.offsetMain + item.margin.left;
                    }
                    y = crossOffset + item.offsetCross + item.margin.top;
                } else {
                    if (isRtl) {
                        // RTL column layout: cross axis starts from right edge
                        // x = right edge - cross offset - item cross end position + margin.left
                        float itemCrossSize = item.targetSize.width;
                        x = contentBoxInset.left + innerCrossSize - item.offsetCross - itemCrossSize - item.margin.right;
                    } else {
                        x = crossOffset + item.offsetCross + item.margin.left;
                    }
                    y = mainOffset + item.offsetMain + item.margin.top;
                }

                // Handle relative positioning
                if (!isNaN(item.inset.left)) x += item.inset.left;
                else if (!isNaN(item.inset.right)) x -= item.inset.right;
                if (!isNaN(item.inset.top)) y += item.inset.top;
                else if (!isNaN(item.inset.bottom)) y -= item.inset.bottom;

                FloatSize scrollbarSize = new FloatSize(
                    item.overflow.y == Overflow.SCROLL ? item.scrollbarWidth : 0f,
                    item.overflow.x == Overflow.SCROLL ? item.scrollbarWidth : 0f
                );

                // Use the actual output size from child layout
                FloatSize actualSize = output.size();

                // Update item baseline for container baseline calculation.
                // Per CSS spec: baseline = baseline_offset + inner_baseline. The inner baseline is
                // clamped to the item's border box when the item is a scroll container; this applies
                // to row and column containers alike (upstream #995 covered row containers, #1323
                // added the column direction that used to be skipped here).
                float innerBaseline = BaselineHelper.childFirstBaseline(
                    output, item.overflow.y.isScrollContainer());
                if (Float.isNaN(innerBaseline)) {
                    innerBaseline = actualSize.height;
                }
                if (isRow) {
                    float baselineOffsetCross = crossOffset + item.offsetCross + item.margin.top;
                    item.baseline = baselineOffsetCross + innerBaseline;
                } else {
                    float baselineOffsetMain = mainOffset + item.offsetMain + item.margin.top;
                    item.baseline = baselineOffsetMain + innerBaseline;
                }

                // Calculate container's first vertical baseline
                if (item == firstBaselineItem) {
                    firstVerticalBaseline = item.baseline;
                }

                // Canonical 9-component constructor: the item's own scrollable overflow is carried
                // on its layout instead of being dropped by the legacy 8-component overload.
                ScrollableOverflow childOverflow =
                    output.scrollableOverflow() != null ? output.scrollableOverflow() : ScrollableOverflow.ZERO;
                Layout layout = new Layout(
                    item.order,
                    new FloatPoint(x, y),
                    actualSize,
                    output.contentSize() != null ? output.contentSize() : actualSize,
                    childOverflow,
                    scrollbarSize,
                    item.border,
                    item.padding,
                    item.margin
                );

                tree.setUnroundedLayout(item.nodeId, layout);

                // P4/SO-01/SO-02: accumulate the item's contribution in this container's
                // padding-box frame. `location - paddingBoxInset` is #1007's border correction in
                // the physical #1372 model (scrollbar gutter included, no RTL mirroring). The full
                // resolved margin is passed: auto margins have already been assigned by the
                // free-space distribution and only the end side extends a scroll container's
                // padding, which the shared helper clamps at zero.
                FloatRect contribution = ScrollableOverflowUtil.computeContribution(
                    new FloatPoint(x - paddingBoxInset.left, y - paddingBoxInset.top),
                    actualSize,
                    childOverflow.rect(),
                    item.border,
                    item.overflow,
                    overflowContainer,
                    item.margin
                );
                overflowRect = ScrollableOverflowUtil.union(overflowRect, contribution);

                float marginMain = isRow
                    ? item.margin.left + item.margin.right
                    : item.margin.top + item.margin.bottom;
                mainOffset += (isRow ? actualSize.width : actualSize.height) + marginMain + mainGap;
            }
        }

        // P4/SO-01/SO-02: the container's own contribution (RTL client width and, for a scroll
        // container, its own end padding) is a union on top of the in-flow items, matching
        // upstream #1372's final_layout_pass.
        FloatRect accumulated = addContainerOverflow(overflowRect, containerSize, padding, border,
            scrollbarGutter, isRtl, isScrollContainer);

        // Layout absolutely positioned children and union their contributions. An absolutely
        // positioned box does not extend the container's padding (upstream oof.rs passes a zero
        // margin and a zero-padding container), so only its own border box/overflow propagates.
        // FL-23 (#1072): the default justify-content behaves as flex-start (upstream resolves
        // `normal` to FLEX_START for flex containers), not writing-mode-relative start.
        JustifyContent jc = containerStyle.justifyContent != AlignContent.AUTO ? containerStyle.getJustifyContent() : JustifyContent.FLEX_START;
        FloatRect absoluteOverflow = layoutAbsoluteChildren(
            node, containerSize, contentBoxInset, flexDirection, containerStyle.getAlignItems(), jc,
            isWrapReverse, isRtl, border, scrollbarGutter, paddingBoxInset, overflowContainer);
        accumulated = ScrollableOverflowUtil.union(accumulated, absoluteOverflow);

        ScrollableOverflow scrollableOverflow = ScrollableOverflowUtil.finalizeScrollableOverflow(
            accumulated,
            computeClientSize(containerSize, border, scrollbarGutter),
            reversedX,
            reversedY,
            isScrollContainer
        );
        return new FinalLayoutResult(firstVerticalBaseline, scrollableOverflow);
    }

    private FloatRect layoutAbsoluteChildren(NodeId node, FloatSize containerSize, FloatRect contentBoxInset,
                                             FlexDirection flexDirection, AlignItems alignItems, JustifyContent justifyContent,
                                             boolean isWrapReverse, boolean isRtl, FloatRect border, FloatSize scrollbarGutter,
                                             FloatRect paddingBoxInset,
                                             ScrollableOverflowUtil.Container overflowContainer) {
        TaffyTree tree = layoutComputer.getTree();
        boolean isRow = flexDirection.isRow();
        // FL-23 (#1072): an axis is writing-mode relative only when it is horizontal. The flex
        // start edge is reversed by a reversed flex-direction and by RTL on the main axis, and by
        // wrap-reverse and by RTL on a horizontal cross axis. `start`/`end` themselves are never
        // affected by the flex reversal flags.
        boolean mainIsRtl = isRow && isRtl;
        boolean crossIsRtl = !isRow && isRtl;
        boolean mainAxisFlexStartReversed = flexDirection.isReverse() ^ mainIsRtl;
        boolean crossAxisFlexStartReversed = isWrapReverse ^ crossIsRtl;
        FloatRect overflowRect = FloatRect.zero();

        // Out-of-flow boxes do not extend their containing block's padding (upstream oof.rs uses
        // a zero-padding container), so a separate container description is used for their
        // contributions. The clipping rules and the physical frame are the same.
        ScrollableOverflowUtil.Container absoluteContainer = new ScrollableOverflowUtil.Container(
            overflowContainer.parentIsScrollContainer(),
            overflowContainer.parentSize(),
            overflowContainer.reversedX(),
            overflowContainer.reversedY(),
            FloatRect.zero()
        );

        for (NodeId childId : tree.getChildren(node)) {
            TaffyStyle childStyle = tree.getStyle(childId);
            if (!childStyle.getPosition().isOutOfFlow()) continue;
            if (childStyle.getBoxGenerationMode() == BoxGenerationMode.NONE) continue;

            // Area available for positioning (container size minus border and scrollbar gutter)
            float insetRelativeWidth = containerSize.width - border.left - border.right - scrollbarGutter.width;
            float insetRelativeHeight = containerSize.height - border.top - border.bottom - scrollbarGutter.height;

            TaffyRect<LengthPercentageAuto> insetStyle = childStyle.getInset();
            float left = insetStyle.left.maybeResolve(insetRelativeWidth);
            float right = insetStyle.right.maybeResolve(insetRelativeWidth);
            float top = insetStyle.top.maybeResolve(insetRelativeHeight);
            float bottom = insetStyle.bottom.maybeResolve(insetRelativeHeight);

            // Margins resolve against inset-relative width
            FloatRect marginOption = resolveMarginToOption(childStyle.getMargin(), insetRelativeWidth);
            FloatRect margin = new FloatRect(
                isNaN(marginOption.left) ? 0f : marginOption.left,
                isNaN(marginOption.right) ? 0f : marginOption.right,
                isNaN(marginOption.top) ? 0f : marginOption.top,
                isNaN(marginOption.bottom) ? 0f : marginOption.bottom
            );

            // An `auto` size of an absolutely positioned *replaced* element resolves to its
            // intrinsic size instead of being stretched between its insets (upstream #1203).
            boolean isReplaced = childStyle.getItemIsReplaced();

            // Compute size
            Float aspectRatio = childStyle.getAspectRatio();
            FloatRect itemPadding = Resolve.resolveRectOrZero(childStyle.getPadding(), insetRelativeWidth);
            FloatRect itemBorder = Resolve.resolveRectOrZero(childStyle.getBorder(), insetRelativeWidth);
            FloatSize paddingBorderSum = new FloatSize(
                itemPadding.left + itemPadding.right + itemBorder.left + itemBorder.right,
                itemPadding.top + itemPadding.bottom + itemBorder.top + itemBorder.bottom
            );

            FloatSize boxSizingAdj = childStyle.getBoxSizing() == BoxSizing.CONTENT_BOX
                                     ? paddingBorderSum
                                     : FloatSize.ZERO;

            FloatSize styleSize = maybeAdd(maybeApplyAspectRatio(
                Resolve.maybeResolveSize(childStyle.getSize(), new FloatSize(insetRelativeWidth, insetRelativeHeight)),
                aspectRatio), boxSizingAdj);
            // NOTE: padding/border must always be a lower bound for the used size.
            // TaffyMath.maybeMax preserves "undefined" when the *left* operand is null, so we must
            // put paddingBorderSum on the left to ensure it applies even when min-size is not set.
            FloatSize minSz = maybeMax(paddingBorderSum, maybeAdd(maybeApplyAspectRatio(
                Resolve.maybeResolveSize(childStyle.getMinSize(), new FloatSize(insetRelativeWidth, insetRelativeHeight)),
                aspectRatio), boxSizingAdj));
            FloatSize maxSz = maybeAdd(maybeApplyAspectRatio(
                Resolve.maybeResolveSize(childStyle.getMaxSize(), new FloatSize(insetRelativeWidth, insetRelativeHeight)),
                aspectRatio), boxSizingAdj);

            FloatSize knownDimensions = maybeClamp(styleSize, minSz, maxSz);

            // Fill from insets if not set. A replaced element is not stretched: its auto size
            // resolves to its intrinsic size instead (upstream #1203).
            if (!isReplaced && isNaN(knownDimensions.width) && !isNaN(left) && !isNaN(right)) {
                float newWidth = TaffyMath.maybeSub(TaffyMath.maybeSub(insetRelativeWidth, marginOption.left), marginOption.right);
                knownDimensions = new FloatSize(
                    Math.max(!isNaN(newWidth) ? newWidth - left - right : 0f, 0f),
                    knownDimensions.height
                );
                knownDimensions = maybeClamp(maybeApplyAspectRatio(knownDimensions, aspectRatio), minSz, maxSz);
            }
            if (!isReplaced && isNaN(knownDimensions.height) && !isNaN(top) && !isNaN(bottom)) {
                float newHeight = TaffyMath.maybeSub(TaffyMath.maybeSub(insetRelativeHeight, marginOption.top), marginOption.bottom);
                knownDimensions = new FloatSize(
                    knownDimensions.width,
                    Math.max(!isNaN(newHeight) ? newHeight - top - bottom : 0f, 0f)
                );
                knownDimensions = maybeClamp(maybeApplyAspectRatio(knownDimensions, aspectRatio), minSz, maxSz);
            }

            TaffySize<AvailableSpace> availableSpace = new TaffySize<>(
                AvailableSpace.definite(containerSize.width),
                AvailableSpace.definite(containerSize.height)
            );

            LayoutOutput measuredOutput = layoutComputer.performChildLayout(
                childId,
                knownDimensions,
                new FloatSize(insetRelativeWidth, insetRelativeHeight),
                availableSpace,
                // Absolute positioned nodes need inherent sizing so that min/max constraints and aspect-ratio
                // resolution are applied by leaf/layout algorithms.
                SizingMode.INHERENT_SIZE,
                new TaffyLine<>(false, false)
            );

            FloatSize finalSize = maybeClamp(
                new FloatSize(
                    !isNaN(knownDimensions.width) ? knownDimensions.width : measuredOutput.size().width,
                    !isNaN(knownDimensions.height) ? knownDimensions.height : measuredOutput.size().height
                ),
                minSz, maxSz
            );

            // PA-05: when an axis had to be sized from content, the measured output was produced
            // against an indefinite containing block. Re-run the child layout with the definite
            // final size so percentage sizes in its subtree resolve against it; the returned
            // output supplies the content size (and the descendant layouts) used below, while the
            // final size, auto margins and offsets keep using `finalSize`.
            LayoutOutput output = layoutComputer.performDefiniteAbsoluteChildLayout(
                childId,
                knownDimensions,
                finalSize,
                new FloatSize(insetRelativeWidth, insetRelativeHeight),
                availableSpace,
                SizingMode.INHERENT_SIZE,
                measuredOutput
            );

            // Auto margins only absorb free space in an axis where *both* insets are non-auto;
            // with one (or no) inset the axis is over-constrained and they resolve to 0.
            // The free space is measured against the positioning area (the container's border box
            // less border and scrollbar gutter), and a margin only counts when its own inset is
            // set. Upstream #1202 (3307c7e0).
            float nonAutoXMarginSum = (isNaN(left) ? 0f : margin.left) + (isNaN(right) ? 0f : margin.right);
            float nonAutoYMarginSum = (isNaN(top) ? 0f : margin.top) + (isNaN(bottom) ? 0f : margin.bottom);
            FloatSize freeSpace = new FloatSize(
                !isNaN(left) && !isNaN(right)
                ? Math.max(0f, insetRelativeWidth - left - right - finalSize.width - nonAutoXMarginSum)
                : 0f,
                !isNaN(top) && !isNaN(bottom)
                ? Math.max(0f, insetRelativeHeight - top - bottom - finalSize.height - nonAutoYMarginSum)
                : 0f
            );

            // Resolve auto margins
            float autoMarginWidth = 0f;
            int autoMarginCountX = (Float.isNaN(marginOption.left) ? 1 : 0) + (Float.isNaN(marginOption.right) ? 1 : 0);
            if (autoMarginCountX > 0) {
                autoMarginWidth = freeSpace.width / autoMarginCountX;
            }

            float autoMarginHeight = 0f;
            int autoMarginCountY = (Float.isNaN(marginOption.top) ? 1 : 0) + (Float.isNaN(marginOption.bottom) ? 1 : 0);
            if (autoMarginCountY > 0) {
                autoMarginHeight = freeSpace.height / autoMarginCountY;
            }

            FloatRect resolvedMargin = new FloatRect(
                !Float.isNaN(marginOption.left) ? marginOption.left : autoMarginWidth,
                !Float.isNaN(marginOption.right) ? marginOption.right : autoMarginWidth,
                !Float.isNaN(marginOption.top) ? marginOption.top : autoMarginHeight,
                !Float.isNaN(marginOption.bottom) ? marginOption.bottom : autoMarginHeight
            );

            // Determine flex-relative insets
            float startMain, endMain, startCross, endCross;
            if (isRow) {
                startMain = left;
                endMain = right;
                startCross = top;
                endCross = bottom;
            } else {
                startMain = top;
                endMain = bottom;
                startCross = left;
                endCross = right;
            }

            // Get child's align-self or fall back to container's align-items
            AlignItems alignSelf = childStyle.getAlignSelf();
            if (alignSelf == null || alignSelf == AlignItems.AUTO) alignSelf = alignItems;
            AlignSelf alignSelfEnum = AlignSelf.fromAlignItems(alignSelf);

            // Calculate main-axis offset. FL-23 (#1072): the static position follows the flex
            // main axis (reversed flex-direction, RTL for row containers) while the explicit
            // `start`/`end` keywords follow the writing mode only.
            float offsetMain = computeAbsoluteMainOffset(
                startMain, endMain, finalSize, containerSize, border, scrollbarGutter,
                contentBoxInset, resolvedMargin, justifyContent, isRow,
                mainIsRtl, mainAxisFlexStartReversed
            );

            // FL-22 (#958): a `safe` cross-axis self-alignment falls back to logical start when the
            // item's outer cross size (used size plus resolved margins) overflows the static-position
            // alignment container, which is the container's content box.
            float crossContainerSize = isRow ? containerSize.height : containerSize.width;
            float crossContentBoxInsetSum = isRow
                                            ? contentBoxInset.top + contentBoxInset.bottom
                                            : contentBoxInset.left + contentBoxInset.right;
            float crossOuterSize = (isRow ? finalSize.height : finalSize.width)
                                   + (isRow ? resolvedMargin.top + resolvedMargin.bottom
                                            : resolvedMargin.left + resolvedMargin.right);
            boolean crossOverflows = crossOuterSize > crossContainerSize - crossContentBoxInsetSum;

            // Calculate cross-axis offset. FL-23 (#1072): the flex-relative keywords follow the
            // cross axis (wrap-reverse, RTL for column containers) while `start`/`end`/`baseline`
            // follow the writing mode only.
            float offsetCross = computeAbsoluteCrossOffset(
                startCross, endCross, finalSize, containerSize, border, scrollbarGutter,
                contentBoxInset, resolvedMargin, alignSelfEnum, isRow,
                crossIsRtl, crossAxisFlexStartReversed, crossOverflows
            );

            // Convert to x, y
            float x, y;
            if (isRow) {
                x = offsetMain;
                y = offsetCross;
            } else {
                x = offsetCross;
                y = offsetMain;
            }

            TaffyPoint<Overflow> overflow = childStyle.getOverflow();
            FloatSize scrollbarSize = new FloatSize(
                overflow.y == Overflow.SCROLL ? childStyle.getScrollbarWidth() : 0f,
                overflow.x == Overflow.SCROLL ? childStyle.getScrollbarWidth() : 0f
            );

            ScrollableOverflow childOverflow =
                output.scrollableOverflow() != null ? output.scrollableOverflow() : ScrollableOverflow.ZERO;
            Layout layout = new Layout(
                0,
                new FloatPoint(x, y),
                finalSize,
                output.contentSize(),
                childOverflow,
                scrollbarSize,
                itemBorder,
                itemPadding,
                resolvedMargin
            );

            tree.setUnroundedLayout(childId, layout);

            // P4/SO-01/SO-02: absolute children contribute from the padding-box frame as well. The
            // shared helper receives a zero margin (upstream oof.rs passes Rect::ZERO), so their
            // margins never extend or shrink the container's reachable region.
            FloatRect contribution = ScrollableOverflowUtil.computeContribution(
                new FloatPoint(x - paddingBoxInset.left, y - paddingBoxInset.top),
                finalSize,
                childOverflow.rect(),
                itemBorder,
                overflow,
                absoluteContainer,
                FloatRect.zero()
            );
            overflowRect = ScrollableOverflowUtil.union(overflowRect, contribution);
        }

        return overflowRect;
    }

    /**
     * The client-area extent of a node: its border box less its border and scrollbar gutters
     * (upstream #1372's {@code finalize_scrollable_overflow} extent argument).
     *
     * <p>Scrollbar gutters are transposed in this port ({@code scrollbarGutter.width} is the space
     * reserved by a vertical scrollbar), so each component is subtracted from the matching
     * physical axis.</p>
     *
     * @param containerSize   the border-box size
     * @param border          the resolved border
     * @param scrollbarGutter the reserved scrollbar space
     * @return the client-area size (possibly negative for degenerate boxes; the finalizer clamps)
     */
    private static FloatSize computeClientSize(FloatSize containerSize, FloatRect border, FloatSize scrollbarGutter) {
        return new FloatSize(
            containerSize.width - border.horizontalAxisSum() - scrollbarGutter.width,
            containerSize.height - border.verticalAxisSum() - scrollbarGutter.height
        );
    }

    /**
     * Adds a container's own scrollable overflow contribution to its accumulated rectangle
     * (upstream #1372 {@code final_layout_pass}).
     *
     * <p>In RTL the physical x scroll origin is the right edge, so the rectangle always reaches the
     * client width: the reachable region of a reversed axis is the negative extent measured from
     * that origin. A scroll container additionally contributes its own end-side padding, which
     * matters when no child reaches it (for example a container whose children are all out of
     * flow). The union never shrinks an accumulator that already extends further, so a container
     * whose content overflows keeps the larger child-driven extent.</p>
     *
     * @param accumulated       the union of the children's contributions, in the padding-box frame
     * @param containerSize     the node's border-box size
     * @param padding           the node's resolved padding
     * @param border            the node's resolved border
     * @param scrollbarGutter   the reserved scrollbar space
     * @param isRtl             whether the node's resolved direction is right-to-left
     * @param isScrollContainer whether the node clips unreachable overflow
     * @return the rectangle including the container's own contribution
     */
    private static FloatRect addContainerOverflow(
        FloatRect accumulated,
        FloatSize containerSize,
        FloatRect padding,
        FloatRect border,
        FloatSize scrollbarGutter,
        boolean isRtl,
        boolean isScrollContainer) {
        float clientWidth = Math.max(0f, containerSize.width - border.horizontalAxisSum() - scrollbarGutter.width);
        FloatRect result = accumulated == null ? FloatRect.zero() : accumulated;
        if (isRtl) {
            result = ScrollableOverflowUtil.union(result, new FloatRect(0f, clientWidth, 0f, 0f));
        }
        if (isScrollContainer) {
            // The physical left padding is the inline-end padding in RTL; in LTR the same end
            // padding starts at the physical right edge. The start-side padding is already part of
            // the child positions and must not be added a second time.
            result = ScrollableOverflowUtil.union(result, new FloatRect(
                isRtl ? Math.min(clientWidth - padding.left, 0f) : 0f,
                isRtl ? clientWidth : padding.right,
                0f,
                padding.bottom
            ));
        }
        return result;
    }

    private FloatRect resolveMarginToOption(TaffyRect<LengthPercentageAuto> margin, float parentWidth) {
        return new FloatRect(
            margin.left.maybeResolve(parentWidth),
            margin.right.maybeResolve(parentWidth),
            margin.top.maybeResolve(parentWidth),
            margin.bottom.maybeResolve(parentWidth)
        );
    }

    private float computeAbsoluteMainOffset(float startMain, float endMain, FloatSize finalSize,
                                            FloatSize containerSize, FloatRect border, FloatSize scrollbarGutter,
                                            FloatRect contentBoxInset, FloatRect resolvedMargin,
                                            JustifyContent justifyContent, boolean isRow,
                                            boolean mainIsRtl, boolean mainAxisFlexStartReversed) {
        float containerMain = isRow ? containerSize.width : containerSize.height;
        float finalMain = isRow ? finalSize.width : finalSize.height;
        float borderMainStart = isRow ? border.left : border.top;
        float borderMainEnd = isRow ? border.right : border.bottom;
        float scrollbarMain = isRow ? scrollbarGutter.width : scrollbarGutter.height;
        float contentBoxMainStart = isRow ? contentBoxInset.left : contentBoxInset.top;
        float contentBoxMainEnd = isRow ? contentBoxInset.right : contentBoxInset.bottom;
        float marginMainStart = isRow ? resolvedMargin.left : resolvedMargin.top;
        float marginMainEnd = isRow ? resolvedMargin.right : resolvedMargin.bottom;

        if (!Float.isNaN(startMain)) {
            return startMain + borderMainStart + marginMainStart;
        } else if (!Float.isNaN(endMain)) {
            return containerMain - borderMainEnd - scrollbarMain - finalMain - endMain - marginMainEnd;
        } else {
            // FL-23 (#1072) static position. `start`/`end` are writing-mode relative: they flip
            // for RTL but not for reversed flex directions. `flex-start`/`flex-end`, the default
            // `normal` and the `space-between`/`stretch` fallbacks are flex-relative.
            float startPosition = contentBoxMainStart + marginMainStart;
            float endPosition = containerMain - contentBoxMainEnd - finalMain - marginMainEnd;
            JustifyContent configured = justifyContent != null ? justifyContent : JustifyContent.FLEX_START;
            JustifyContent keyword = AlignmentHelper.keyword(configured);
            if (keyword == JustifyContent.START || keyword == JustifyContent.END) {
                boolean startSide = keyword == JustifyContent.START ? !mainIsRtl : mainIsRtl;
                return startSide ? startPosition : endPosition;
            }
            if (keyword == JustifyContent.FLEX_END) {
                return mainAxisFlexStartReversed ? startPosition : endPosition;
            }
            if (keyword == JustifyContent.CENTER
                || keyword == JustifyContent.SPACE_AROUND
                || keyword == JustifyContent.SPACE_EVENLY) {
                return (containerMain + contentBoxMainStart - contentBoxMainEnd - finalMain
                        + marginMainStart - marginMainEnd) / 2.0f;
            }
            // FLEX_START / NORMAL / STRETCH / SPACE_BETWEEN: flex-relative start. Stretch has no
            // effect on the main axis of a flex container and space-between falls back to it.
            return mainAxisFlexStartReversed ? endPosition : startPosition;
        }
    }

    private float computeAbsoluteCrossOffset(float startCross, float endCross, FloatSize finalSize,
                                             FloatSize containerSize, FloatRect border, FloatSize scrollbarGutter,
                                             FloatRect contentBoxInset, FloatRect resolvedMargin,
                                             AlignSelf alignSelf, boolean isRow,
                                             boolean crossIsRtl, boolean crossAxisFlexStartReversed,
                                             boolean overflows) {
        float containerCross = isRow ? containerSize.height : containerSize.width;
        float finalCross = isRow ? finalSize.height : finalSize.width;
        float borderCrossStart = isRow ? border.top : border.left;
        float borderCrossEnd = isRow ? border.bottom : border.right;
        float scrollbarCross = isRow ? scrollbarGutter.height : scrollbarGutter.width;
        float contentBoxCrossStart = isRow ? contentBoxInset.top : contentBoxInset.left;
        float contentBoxCrossEnd = isRow ? contentBoxInset.bottom : contentBoxInset.right;
        float marginCrossStart = isRow ? resolvedMargin.top : resolvedMargin.left;
        float marginCrossEnd = isRow ? resolvedMargin.bottom : resolvedMargin.right;

        if (!Float.isNaN(startCross)) {
            return startCross + borderCrossStart + marginCrossStart;
        } else if (!Float.isNaN(endCross)) {
            return containerCross - borderCrossEnd - scrollbarCross - finalCross - endCross - marginCrossEnd;
        } else {
            // Use align-self to position. Stretch does not apply to absolutely positioned items.
            //
            // FL-22 (#958): resolve the `safe` overflow-position modifier first. A safe keyword
            // whose item overflows the content box falls back to logical START; otherwise the
            // modifier is dropped and the bare keyword is used.
            //
            // FL-23 (#1072): `start`/`end` (and `baseline`, whose static-position fallback is
            // `start`) are writing-mode relative: they flip for RTL but not for `wrap-reverse`.
            // `flex-start`/`flex-end` and the `stretch`/`normal` fallback are flex-relative.
            AlignSelf crossKeyword = AlignmentHelper.resolveSelfSafety(alignSelf, overflows);
            if (crossKeyword == AlignSelf.NORMAL) {
                // `normal` behaves as `stretch` for flex items (upstream #1254); stretch does not
                // apply to absolutely positioned items, hence the flex-relative start below.
                crossKeyword = AlignSelf.STRETCH;
            }
            float startPosition = contentBoxCrossStart + marginCrossStart;
            float endPosition = containerCross - contentBoxCrossEnd - finalCross - marginCrossEnd;
            if (crossKeyword == AlignSelf.START || crossKeyword == AlignSelf.END
                || crossKeyword == AlignSelf.BASELINE) {
                boolean startSide = crossKeyword == AlignSelf.END ? crossIsRtl : !crossIsRtl;
                return startSide ? startPosition : endPosition;
            }
            if (crossKeyword == AlignSelf.FLEX_END) {
                return crossAxisFlexStartReversed ? startPosition : endPosition;
            }
            if (crossKeyword == AlignSelf.CENTER) {
                return (containerCross + contentBoxCrossStart - contentBoxCrossEnd - finalCross
                        + marginCrossStart - marginCrossEnd) / 2.0f;
            }
            if (crossKeyword == AlignSelf.SELF_START || crossKeyword == AlignSelf.SELF_END) {
                // Self-relative resolution against the item's own writing mode is not wired into
                // the absolute positioning path in this batch; keep the legacy start placement.
                return startPosition;
            }
            // AUTO / STRETCH / FLEX_START: flex-relative start.
            return crossAxisFlexStartReversed ? endPosition : startPosition;
        }
    }

    // Helper methods
    private FloatSize maybeApplyAspectRatio(FloatSize size, Float aspectRatio) {
        if (aspectRatio == null || Float.isNaN(aspectRatio)) return size;
        if (!Float.isNaN(size.width) && Float.isNaN(size.height)) {
            return new FloatSize(size.width, size.width / aspectRatio);
        }
        if (Float.isNaN(size.width) && !Float.isNaN(size.height)) {
            return new FloatSize(size.height * aspectRatio, size.height);
        }
        return size;
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
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < sizes.length; i++) {
            FloatSize size = sizes[i];
            if (Float.isNaN(width) && !Float.isNaN(size.width)) width = size.width;
            if (Float.isNaN(height) && !Float.isNaN(size.height)) height = size.height;
            if (!Float.isNaN(width) && !Float.isNaN(height)) break;
        }
        return new FloatSize(width, height);
    }

    /**
     * Balanced line breaking for {@code flex-wrap: balance} (FL-21, upstream #1105).
     *
     * <p>Implements the balancing algorithm from the CSS Flexbox Level 2 draft
     * (<a href="https://drafts.csswg.org/css-flexbox-2/#balancing">CSS Flexbox 2 - balancing</a>).
     * Items are divided into exactly lineCount contiguous sequences (lines), where lineCount is the
     * number of lines that greedy line breaking would produce (with item sizes floored at zero),
     * raised to the minimum flex line count ({@code flex-line-count}, clamped to the number of
     * items), such that:</p>
     * <ul>
     *   <li>every line holds at least one item;</li>
     *   <li>no line's size exceeds the container's inner main size, unless the line holds a single
     *       (overflowing) item;</li>
     *   <li>a zero-sized item is assigned to the end of the preceding line rather than the
     *       beginning of a line, unless no valid division can glue it there;</li>
     *   <li>the sum of the squared line sizes is minimized;</li>
     *   <li>ties are broken by assigning the most items to the first line, then the most items to
     *       the second line, and so on.</li>
     * </ul>
     *
     * <p>Because every division has the same line count, total item size, and total gap size,
     * minimizing the sum of squared errors is equivalent to minimizing the sum of squared line
     * sizes, which is what is actually computed. All arithmetic uses {@code double}: finite
     * {@code float} sizes convert exactly, and equally-balanced divisions compare equal.</p>
     *
     * <p>The minimization is a dynamic program over (line count, first item of suffix) accelerated
     * with the divide-and-conquer optimization, running in
     * {@code O(lineCount * itemCount * log(itemCount))} time using
     * {@code O(lineCount * itemCount)} transient memory.</p>
     */
    private static final class FlexBalance {

        /** Score assigned to divisions that violate the line size constraint. */
        private static final double INFEASIBLE = Double.POSITIVE_INFINITY;

        private FlexBalance() {
        }

        /**
         * Convert an item size in pixels to an {@code f64}, flooring negative sizes at zero. (An
         * infinite size is clamped to the largest finite {@code f32} so that sums stay meaningful.)
         */
        private static double toSize(float value) {
            return Math.min(Math.max(value, 0.0), Float.MAX_VALUE);
        }

        /** Line sizing shared by the scoring and readback phases. */
        private static final class LineSizes {
            /**
             * Per item, the prefix sum of the item sizes through it (each item also contributes
             * one trailing gap, so the size of the line holding items start..=end is
             * prefixSums[end] - prefixSums[start - 1] - gapBetweenItems).
             */
            final double[] prefixSums;
            /** Per item, whether the item's (floored) size is zero. */
            final boolean[] zeroItem;
            /** The size of the gap between adjacent items on a line. */
            final double gapBetweenItems;
            /**
             * The container's inner main size, which lines may not exceed (unless they hold a
             * single item).
             */
            final double limit;

            LineSizes(double[] prefixSums, boolean[] zeroItem, double gapBetweenItems, double limit) {
                this.prefixSums = prefixSums;
                this.zeroItem = zeroItem;
                this.gapBetweenItems = gapBetweenItems;
                this.limit = limit;
            }

            int itemCount() {
                return prefixSums.length;
            }

            boolean isZeroItem(int index) {
                return zeroItem[index];
            }

            /** The size of the line holding items start..=end. */
            double lineSize(int start, int end) {
                double startSum = start == 0 ? 0.0 : prefixSums[start - 1];
                return prefixSums[end] - startSum - gapBetweenItems;
            }

            /**
             * The squared size of the line holding items start..=end, or {@link #INFEASIBLE} for a
             * line of more than one item exceeding the limit.
             */
            double lineCost(int start, int end) {
                double size = lineSize(start, end);
                if (end > start && size > limit) {
                    return INFEASIBLE;
                }
                return size * size;
            }

            /**
             * For every suffix of the items, the number of lines that greedy line breaking
             * produces for it: each line collects consecutive items until the next item no longer
             * fits, and if even the first item of a line doesn't fit, that line takes just the one
             * (overflowing) item. (The entry for the empty suffix is zero.)
             */
            int[] suffixGreedyLineCounts() {
                int itemCount = itemCount();
                int[] counts = new int[itemCount + 1];
                // The (exclusive) end of the greedy first line of the suffix, which only moves down
                // as the suffix grows leftwards since lines starting earlier are larger.
                int lineEnd = itemCount;
                for (int start = itemCount - 1; start >= 0; start--) {
                    while (lineEnd > start + 1 && lineSize(start, lineEnd - 1) > limit) {
                        lineEnd--;
                    }
                    counts[start] = 1 + counts[lineEnd];
                }
                return counts;
            }

            /**
             * For every start, the largest end such that the line holding items start..=end does
             * not exceed the limit (or start itself if even that one item overflows).
             */
            int[] fitEnds() {
                int itemCount = itemCount();
                int[] fitEnds = new int[itemCount];
                // Lines starting later are smaller, so the fit end only moves up
                int fitEnd = 0;
                for (int start = 0; start < itemCount; start++) {
                    if (fitEnd < start) {
                        fitEnd = start;
                    }
                    while (fitEnd + 1 < itemCount && lineSize(start, fitEnd + 1) <= limit) {
                        fitEnd++;
                    }
                    fitEnds[start] = fitEnd;
                }
                return fitEnds;
            }
        }

        /**
         * One row of the balancing dynamic program: divisions of item suffixes into exactly
         * {@code lines} lines.
         */
        private static final class Row {
            /** The line sizing for the items being divided. */
            final LineSizes sizes;
            /** See {@link LineSizes#fitEnds()}. */
            final int[] fitEnds;
            /**
             * The previous row: prev[start] is the minimum score of any valid division of items
             * start.. into exactly {@code lines - 1} lines ({@link #INFEASIBLE} if there is none).
             */
            final double[] prev;
            /**
             * The line ends that precede a non-zero item, in ascending order and capped to
             * {@code maxEnd}. Line ends preceding a <em>zero</em> item are constrained by the
             * zero-sized-item rule and handled separately.
             */
            final int[] nonzeroEnds;
            /** The largest possible end of the first line. */
            final int maxEnd;

            Row(LineSizes sizes, int[] fitEnds, double[] prev, int[] nonzeroEnds, int maxEnd) {
                this.sizes = sizes;
                this.fitEnds = fitEnds;
                this.prev = prev;
                this.nonzeroEnds = nonzeroEnds;
                this.maxEnd = maxEnd;
            }
        }

        /**
         * Compute cur[start] and opts[start] for start in startLo..=startHi, where cur[start] is
         * the minimum over valid ends end of
         * row.sizes.lineCost(start, end) + row.prev[end + 1] and opts[start] is the largest end
         * achieving that minimum.
         *
         * <p>Uses the divide-and-conquer dynamic programming optimization: over the ends preceding
         * non-zero items, the cost function satisfies the concave quadrangle inequality, so the
         * largest minimizing end is non-decreasing in start. The ends constrained by the
         * zero-sized-item rule are not monotone this way, so they are excluded from the scan and
         * merged in separately.</p>
         */
        private static void fillRow(
            Row row,
            double[] cur,
            int[] opts,
            int optsOffset,
            int startLo,
            int startHi,
            int colLo,
            int colHi) {

            if (startLo > startHi) {
                return;
            }
            int start = startLo + (startHi - startLo) / 2;

            // Minimize over the in-range ends preceding a non-zero item, starting from start (the
            // line must hold at least one item)
            int firstCol = colLo + lowerBound(row.nonzeroEnds, colLo, colHi, start);
            double minCost = INFEASIBLE;
            int minCol = -1;
            for (int col = firstCol; col < colHi; col++) {
                int end = row.nonzeroEnds[col];
                double lineCost = row.sizes.lineCost(start, end);
                if (lineCost == INFEASIBLE) {
                    // Every longer line also exceeds the limit
                    break;
                }
                double cost = lineCost + row.prev[end + 1];
                // The <= comparison keeps the *largest* minimizing end, assigning the most items to
                // the earliest lines as the tie-break requires
                if (cost <= minCost) {
                    minCost = cost;
                    minCol = col;
                }
            }

            // Merge the single end preceding a zero item that the zero-sized-item rule allows
            int minEnd = minCol >= 0 ? row.nonzeroEnds[minCol] : -1;
            int zeroEnd = Math.min(row.fitEnds[start], row.maxEnd);
            if (row.sizes.isZeroItem(zeroEnd + 1)) {
                double cost = row.sizes.lineCost(start, zeroEnd) + row.prev[zeroEnd + 1];
                if (cost < minCost || (cost == minCost && (minEnd < 0 || minEnd < zeroEnd))) {
                    minCost = cost;
                    minEnd = zeroEnd;
                }
            }

            // Infeasible states are excluded from the range by the caller, and every feasible
            // state has a valid transition
            cur[start] = minCost;
            opts[optsOffset + start] = minEnd >= 0 ? minEnd : start;

            // The zero-rule end is excluded from the narrowing: only the non-zero ends' minima are
            // monotone
            if (start > startLo) {
                int narrowedColHi = minCol >= 0 ? minCol + 1 : colHi;
                fillRow(row, cur, opts, optsOffset, startLo, start - 1, colLo, narrowedColHi);
            }
            if (start < startHi) {
                int narrowedColLo = minCol >= 0 ? minCol : firstCol;
                fillRow(row, cur, opts, optsOffset, start + 1, startHi, narrowedColLo, colHi);
            }
        }

        /**
         * Determine the number of items on each line that balances items across lines, such that
         * the largest line is as small as possible, with a minimum of {@code minLineCount} lines
         * (or one line per item if there are fewer items).
         *
         * @param itemSizes       the outer hypothetical main size of each item
         * @param lineLimit       the container's inner main size, or positive infinity
         * @param gapBetweenItems the main-axis gap between adjacent items on a line
         * @param minLineCount    the requested minimum number of lines ({@code flex-line-count})
         * @return per-line item counts; all non-zero and summing to the number of items
         */
        static int[] balancedLineItemCounts(
            float[] itemSizes,
            float lineLimit,
            float gapBetweenItems,
            int minLineCount) {

            int itemCount = itemSizes.length;
            if (itemCount == 0) {
                return new int[0];
            }
            double gap = toSize(gapBetweenItems);
            double limit = lineLimit;

            double[] prefixSums = new double[itemCount];
            boolean[] zeroItem = new boolean[itemCount];
            double sum = 0.0;
            for (int index = 0; index < itemCount; index++) {
                double size = toSize(itemSizes[index]);
                sum += size + gap;
                prefixSums[index] = sum;
                zeroItem[index] = size == 0.0;
            }
            LineSizes sizes = new LineSizes(prefixSums, zeroItem, gap, limit);

            // suffixGreedy[start] is the number of lines greedy line breaking produces for items
            // start.., which is also the *fewest* lines the suffix can validly be divided into
            int[] suffixGreedy = sizes.suffixGreedyLineCounts();
            int lineCount = Math.max(suffixGreedy[0], Math.min(Math.max(minLineCount, 1), itemCount));

            if (lineCount == itemCount) {
                // One item per line is the only division (this covers flex-line-count of at least
                // the item count as well as every item overflowing a line of its own)
                int[] itemCounts = new int[itemCount];
                java.util.Arrays.fill(itemCounts, 1);
                return itemCounts;
            }

            int[] fitEnds = sizes.fitEnds();
            int[] nonzeroEnds = new int[Math.max(0, itemCount - 1)];
            int nonzeroCount = 0;
            for (int end = 0; end < itemCount - 1; end++) {
                if (!sizes.isZeroItem(end + 1)) {
                    nonzeroEnds[nonzeroCount++] = end;
                }
            }
            if (nonzeroCount != nonzeroEnds.length) {
                nonzeroEnds = java.util.Arrays.copyOf(nonzeroEnds, nonzeroCount);
            }

            // prev[start] is the minimum total score of any valid division of items start.. into
            // exactly lines - 1 lines (INFEASIBLE if there is none), and cur is the row being
            // computed for lines lines.
            double[] prev = new double[itemCount];
            for (int start = 0; start < itemCount; start++) {
                prev[start] = sizes.lineCost(start, itemCount - 1);
            }
            double[] cur = new double[itemCount];
            java.util.Arrays.fill(cur, INFEASIBLE);
            int[] opts = new int[(lineCount - 1) * itemCount];
            for (int lines = 2; lines <= lineCount; lines++) {
                // The remaining lines - 1 lines need one item each, bounding this line's end
                int maxEnd = itemCount - lines;
                // Starts whose suffix doesn't fit in lines lines even with greedy breaking are
                // infeasible; they form a prefix (a longer suffix never needs fewer lines), and
                // excluding them keeps every state fillRow solves feasible
                int firstStart = 0;
                while (firstStart < maxEnd && suffixGreedy[firstStart] > lines) {
                    cur[firstStart] = INFEASIBLE;
                    firstStart++;
                }
                int colHi = countAtMost(nonzeroEnds, maxEnd);
                Row row = new Row(sizes, fitEnds, prev, nonzeroEnds, maxEnd);
                fillRow(row, cur, opts, (lines - 2) * itemCount, firstStart, maxEnd, 0, colHi);
                double[] swap = prev;
                prev = cur;
                cur = swap;
            }

            // Read the division back out front to back
            int[] itemCounts = new int[lineCount];
            int start = 0;
            for (int lines = lineCount; lines >= 2; lines--) {
                int end = opts[(lines - 2) * itemCount + start];
                itemCounts[lineCount - lines] = end - start + 1;
                start = end + 1;
            }
            itemCounts[lineCount - 1] = itemCount - start;
            return itemCounts;
        }

        /** The first index in values[lo..hi) whose value is at least {@code key}. */
        private static int lowerBound(int[] values, int lo, int hi, int key) {
            int low = lo;
            int high = hi;
            while (low < high) {
                int mid = (low + high) >>> 1;
                if (values[mid] < key) {
                    low = mid + 1;
                } else {
                    high = mid;
                }
            }
            return low - lo;
        }

        /** The number of values in the ascending array that are at most {@code maxEnd}. */
        private static int countAtMost(int[] values, int maxEnd) {
            int low = 0;
            int high = values.length;
            while (low < high) {
                int mid = (low + high) >>> 1;
                if (values[mid] <= maxEnd) {
                    low = mid + 1;
                } else {
                    high = mid;
                }
            }
            return low;
        }

    }
}
