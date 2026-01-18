package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.Line;
import dev.vfyjxf.taffy.geometry.Point;
import dev.vfyjxf.taffy.geometry.Rect;
import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AlignSelf;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.BoxGenerationMode;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.Dimension;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.JustifyContent;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.Position;
import dev.vfyjxf.taffy.style.Style;
import dev.vfyjxf.taffy.util.Resolve;
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
        AlignSelf alignSelf;
        Point<Overflow> overflow;
        float scrollbarWidth;
        float flexShrink;
        float flexGrow;
        float resolvedMinimumMainSize;
        FloatRect inset;
        FloatRect margin;
        Rect<Boolean> marginIsAuto;
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
     * Computes flexbox layout for a node.
     */
    public LayoutOutput compute(NodeId node, LayoutInput inputs, Style style) {
        TaffyTree tree = layoutComputer.getTree();
        FloatSize knownDimensions = inputs.knownDimensions();
        FloatSize parentSize = inputs.parentSize();
        Size<AvailableSpace> availableSpace = inputs.availableSpace();
        RunMode runMode = inputs.runMode();

        FlexDirection flexDirection = style.getFlexDirection();
        boolean isRow = flexDirection.isRow();
        boolean isWrap = style.getFlexWrap() != FlexWrap.NO_WRAP;
        boolean isWrapReverse = style.getFlexWrap() == FlexWrap.WRAP_REVERSE;

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

        // Short-circuit if size is known
        if (runMode == RunMode.COMPUTE_SIZE &&
            !isNaN(styledBasedKnownDimensions.width) &&
            !isNaN(styledBasedKnownDimensions.height)) {
            return LayoutOutput.fromOuterSize(styledBasedKnownDimensions);
        }

        // Compute scrollbar gutter
        float scrollbarWidth = style.getScrollbarWidth();
        Point<Overflow> overflow = style.getOverflow();
        FloatSize scrollbarGutter = new FloatSize(
            overflow.y == Overflow.SCROLL ? scrollbarWidth : 0f,
            overflow.x == Overflow.SCROLL ? scrollbarWidth : 0f
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
        Size<AvailableSpace> innerAvailableSpace = determineAvailableSpace(styledBasedKnownDimensions, availableSpace, contentBoxInset);

        // Generate flex items
        List<FlexItem> items = generateFlexItems(node, style, nodeInnerSize, flexDirection);

        // If no flow items, we still need to layout absolute children
        if (items.isEmpty()) {
            FloatSize containerSize = !isNaN(styledBasedKnownDimensions.width) && !isNaN(styledBasedKnownDimensions.height)
                                      ? styledBasedKnownDimensions
                                      : paddingBorderSize;

            if (runMode == RunMode.COMPUTE_SIZE) {
                return LayoutOutput.fromOuterSize(containerSize);
            }

            // Still need to layout absolute children even if no flow children
            // Pass null for justifyContent when not set to get correct default (START)
            JustifyContent jc = style.justifyContent != null ? style.getJustifyContent() : null;
            layoutAbsoluteChildren(node, containerSize, contentBoxInset, flexDirection,
                style.getAlignItems(), jc, isWrapReverse, border, scrollbarGutter);

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
                        Size.maxContent(),
                        SizingMode.INHERENT_SIZE,
                        Line.FALSE
                    );
                }
            }

            return LayoutOutput.fromOuterSize(containerSize);
        }

        // Determine flex base size and hypothetical main size
        determineFlexBaseSize(items, nodeInnerSize, innerAvailableSpace, flexDirection);

        // Collect items into flex lines (use innerAvailableSpace for wrapping, matching Rust)
        float innerMainSize = isRow ? nodeInnerSize.width : nodeInnerSize.height;
        float mainGap = isRow ? gap.width : gap.height;
        List<FlexLine> flexLines = collectIntoFlexLines(items, innerAvailableSpace, mainGap, isWrap, flexDirection, minSize, maxSize);

        // Determine container main size if not already known (needed for flex grow/shrink)
        if (isNaN(innerMainSize)) {
            innerMainSize = determineContainerMainSize(
                flexLines,
                contentBoxInset,
                minSize,
                maxSize,
                mainGap,
                flexDirection,
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
            determineHypotheticalCrossSize(line, flexDirection, innerMainSize, nodeInnerSize, innerAvailableSpace);
        }

        // Calculate children baselines (for baseline alignment support)
        // This needs to be before calculateCrossSize since baseline affects line cross size
        calculateChildrenBaselines(flexLines, nodeInnerSize, innerAvailableSpace, flexDirection);

        // Calculate cross size
        calculateCrossSize(flexLines, nodeInnerSize, innerAvailableSpace, flexDirection, style, minSize, maxSize, contentBoxInset);

        // Distribute remaining space
        distributeRemainingMainSpace(flexLines, style, innerMainSize, mainGap, flexDirection);

        // Calculate container size (use innerMainSize as the known main size when available)
        FloatSize containerSize = calculateContainerSize(
            flexLines, styledBasedKnownDimensions, paddingBorderSize,
            contentBoxInset, gap, minSize, maxSize, flexDirection, innerMainSize);

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

        // Determine used cross size for stretch items (uses stretched line.crossSize)
        determineUsedCrossSize(flexLines, flexDirection, style);

        // Align items on cross axis (must be after lines are stretched)
        alignItemsOnCrossAxis(flexLines, flexDirection, isWrapReverse);

        // Perform final layout and get container baseline
        float firstVerticalBaseline = performFinalLayout(flexLines, node, containerSize, contentBoxInset, gap, style, flexDirection, isWrapReverse);

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
                    Size.maxContent(),
                    SizingMode.INHERENT_SIZE,
                    Line.FALSE
                );
            }
        }

        return LayoutOutput.fromSizesAndBaselines(containerSize, containerSize, new FloatPoint(NaN, firstVerticalBaseline));
    }

    private List<FlexItem> generateFlexItems(NodeId node, Style containerStyle, FloatSize nodeInnerSize, FlexDirection flexDirection) {
        TaffyTree tree = layoutComputer.getTree();
        List<FlexItem> items = new ArrayList<>();
        AlignItems defaultAlign = containerStyle.getAlignItems();

        boolean isRow = flexDirection.isRow();

        int order = 0;
        for (NodeId childId : tree.getChildren(node)) {
            Style childStyle = tree.getStyle(childId);
            if (childStyle.getBoxGenerationMode() == BoxGenerationMode.NONE ||
                childStyle.getPosition() == Position.ABSOLUTE) {
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
            item.minSize = maybeAdd(maybeApplyAspectRatio(
                rawResolvedMinSize, aspectRatio), boxSizingAdj);
            item.maxSize = maybeAdd(maybeApplyAspectRatio(
                rawResolvedMaxSize, aspectRatio), boxSizingAdj);

            item.overflow = childStyle.getOverflow();
            item.scrollbarWidth = childStyle.getScrollbarWidth();
            item.flexGrow = childStyle.getFlexGrow();
            item.flexShrink = childStyle.getFlexShrink();

            AlignItems selfAlignItems = childStyle.getAlignSelf();
            if (selfAlignItems != null) {
                item.alignSelf = AlignSelf.fromAlignItems(selfAlignItems);
            } else {
                item.alignSelf = AlignSelf.fromAlignItems(defaultAlign);
            }

            // If cross size is auto in style and the item is stretched on the cross axis, do not let aspect-ratio
            // resolve the cross size here. Stretch should be able to set it.
            if (aspectRatio != null && item.alignSelf == AlignSelf.STRETCH) {
                boolean crossSizeAutoInStyle = isRow ? (isNaN(rawResolvedSize.height)) : (isNaN(rawResolvedSize.width));

                // 1) Avoid locking computed cross size from aspect ratio when stretch should set it.
                boolean crossAutoInStyleWithDefiniteMain = isRow
                                                           ? (isNaN(rawResolvedSize.height) && !isNaN(rawResolvedSize.width))
                                                           : (isNaN(rawResolvedSize.width) && !isNaN(rawResolvedSize.height));

                if (crossAutoInStyleWithDefiniteMain) {
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
            item.marginIsAuto = new Rect<>(
                childStyle.getMargin().left.isAuto(),
                childStyle.getMargin().right.isAuto(),
                childStyle.getMargin().top.isAuto(),
                childStyle.getMargin().bottom.isAuto()
            );

            Rect<LengthPercentageAuto> insetStyle = childStyle.getInset();
            item.inset = new FloatRect(
                insetStyle.left.maybeResolve(nodeInnerSize.width),
                insetStyle.right.maybeResolve(nodeInnerSize.width),
                insetStyle.top.maybeResolve(nodeInnerSize.height),
                insetStyle.bottom.maybeResolve(nodeInnerSize.height)
            );

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
    private Size<AvailableSpace> determineAvailableSpace(
        FloatSize knownDimensions,
        Size<AvailableSpace> outerAvailableSpace,
        FloatRect contentBoxInset) {

        float horizontalInset = contentBoxInset.left + contentBoxInset.right;
        float verticalInset = contentBoxInset.top + contentBoxInset.bottom;

        AvailableSpace width;
        if (!isNaN(knownDimensions.width)) {
            width = AvailableSpace.definite(knownDimensions.width - horizontalInset);
        } else {
            width = outerAvailableSpace.width.maybeSub(horizontalInset);
        }

        AvailableSpace height;
        if (!isNaN(knownDimensions.height)) {
            height = AvailableSpace.definite(knownDimensions.height - verticalInset);
        } else {
            height = outerAvailableSpace.height.maybeSub(verticalInset);
        }

        return new Size<>(width, height);
    }

    private void determineFlexBaseSize(
        List<FlexItem> items,
        FloatSize nodeInnerSize,
        Size<AvailableSpace> availableSpace,
        FlexDirection flexDirection) {

        boolean isRow = flexDirection.isRow();

        for (FlexItem item : items) {
            TaffyTree tree = layoutComputer.getTree();
            Style childStyle = tree.getStyle(item.nodeId);

            // Parent size for child sizing
            float crossAxisParentSize = isRow ? nodeInnerSize.height : nodeInnerSize.width;
            // child_parent_size = Size::from_cross(dir, cross_axis_parent_size) - only cross axis is set
            FloatSize childParentSize = isRow
                                        ? new FloatSize(NaN, crossAxisParentSize)
                                        : new FloatSize(crossAxisParentSize, NaN);

            // Available space for child sizing - get cross axis min/max from item
            float itemMinCross = isRow ? item.minSize.height : item.minSize.width;
            float itemMaxCross = isRow ? item.maxSize.height : item.maxSize.width;

            // Clamp available space by min- and max- size (matching Rust's determine_flex_base_size)
            AvailableSpace crossAxisAvailableSpace;
            AvailableSpace outerCrossAvail = isRow ? availableSpace.height : availableSpace.width;

            if (outerCrossAvail.isDefinite()) {
                float val = outerCrossAvail.getValue();
                float clamped = !isNaN(crossAxisParentSize) ? crossAxisParentSize : val;
                if (!isNaN(itemMinCross)) clamped = Math.max(clamped, itemMinCross);
                if (!isNaN(itemMaxCross)) clamped = Math.min(clamped, itemMaxCross);
                crossAxisAvailableSpace = AvailableSpace.definite(clamped);
            } else if (outerCrossAvail.isMinContent()) {
                crossAxisAvailableSpace = isNaN(itemMinCross) ? AvailableSpace.minContent() : AvailableSpace.definite(itemMinCross);
            } else { // MaxContent
                crossAxisAvailableSpace = isNaN(itemMaxCross) ? AvailableSpace.maxContent() : AvailableSpace.definite(itemMaxCross);
            }

            // Determine flex basis
            Dimension flexBasis = childStyle.getFlexBasis();
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
            float childKnownCross = crossSize;

            if (item.alignSelf == AlignSelf.STRETCH && isNaN(childKnownCross)) {
                boolean crossStartAuto = isRow ? item.marginIsAuto.top : item.marginIsAuto.left;
                boolean crossEndAuto = isRow ? item.marginIsAuto.bottom : item.marginIsAuto.right;
                if (!crossStartAuto && !crossEndAuto) {
                    // Apply cross-axis available space minus margins
                    float crossAvailValue = crossAxisAvailableSpace.intoOption();
                    if (!isNaN(crossAvailValue)) {
                        float marginCross = isRow
                                            ? (item.margin.top + item.margin.bottom)
                                            : (item.margin.left + item.margin.right);
                        childKnownCross = crossAvailValue - marginCross;
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

            float basis;
            float resolvedFlexBasis = flexBasis.isAuto() ? NaN : flexBasis.maybeResolve(isRow ? nodeInnerSize.width : nodeInnerSize.height);
            // Add box_sizing_adjustment to resolved flex-basis (matching Rust's maybe_add behavior)
            if (!isNaN(resolvedFlexBasis)) {
                resolvedFlexBasis = resolvedFlexBasis + boxSizingAdjustmentMain;
            }

            if (!isNaN(resolvedFlexBasis)) {
                // A. If the item has a definite used flex basis, that's the flex base size.
                basis = resolvedFlexBasis;
            } else if (!isNaN(mainSize)) {
                // B/A. If flex basis is auto, and there's a definite main size, use that.
                basis = mainSize;
            } else {
                // E. Otherwise, size the item into the available space using its used flex basis
                //    in place of its main size, treating a value of content as max-content.
                //    The flex base size is the item's resulting main size.

                // Main axis: Map Definite to MaxContent for flex base size calculation
                AvailableSpace mainAvailSpace = isRow
                                                ? (availableSpace.width.isMinContent() ? AvailableSpace.minContent() : AvailableSpace.maxContent())
                                                : (availableSpace.height.isMinContent() ? AvailableSpace.minContent() : AvailableSpace.maxContent());

                // Cross axis: use computed crossAxisAvailableSpace
                Size<AvailableSpace> childAvailSpace = isRow
                                                       ? new Size<>(mainAvailSpace, crossAxisAvailableSpace)
                                                       : new Size<>(crossAxisAvailableSpace, mainAvailSpace);

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
                    new Line<>(false, false)
                );

                basis = isRow ? measuredSize.width : measuredSize.height;
            }

            item.flexBasis = Math.max(basis, paddingBorderMain); // Floor at padding+border
            item.innerFlexBasis = Math.max(0, item.flexBasis - paddingBorderMain);

            // Calculate resolved minimum main size (CSS 4.5. Automatic Minimum Size of Flex Items)
            // https://www.w3.org/TR/css-flexbox-1/#min-size-auto
            float minMain = isRow ? item.minSize.width : item.minSize.height;
            float maxMain = isRow ? item.maxSize.width : item.maxSize.height;
            float styleMainSize = isRow ? item.size.width : item.size.height;

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
                Size<AvailableSpace> minContentAvailSpace = isRow
                                                            ? new Size<>(AvailableSpace.minContent(), crossAxisAvailableSpace)
                                                            : new Size<>(crossAxisAvailableSpace, AvailableSpace.minContent());

                // Use childKnownDimensions (computed above with stretch logic applied)
                // Per Rust flexbox.rs line 805: reuses child_known_dimensions for min-content measurement
                // Use measureChildSize to avoid setting unroundedLayout prematurely
                FloatSize minMeasuredSize = layoutComputer.measureChildSize(
                    item.nodeId,
                    childKnownDimensions,
                    childParentSize,
                    minContentAvailSpace,
                    SizingMode.CONTENT_SIZE,
                    new Line<>(false, false)
                );

                // 4.5: clamp min-content size by specified size and max-size
                float clampedMinContent = isRow ? minMeasuredSize.width : minMeasuredSize.height;
                if (!isNaN(styleMainSize)) {
                    clampedMinContent = Math.min(clampedMinContent, styleMainSize);
                }
                if (!isNaN(maxMain)) {
                    clampedMinContent = Math.min(clampedMinContent, maxMain);
                }

                // Floor at padding+border
                item.resolvedMinimumMainSize = Math.max(clampedMinContent, paddingBorderMain);
            }

            // Hypothetical main size = flex_basis clamped by min/max
            // Note: In taffy, flex_basis, hypothetical_inner_size, min_size, max_size all include padding+border

            // Use resolvedMinimumMainSize if available (includes content-based minimum)
            float hypotheticalInnerMinMain = Math.max(item.resolvedMinimumMainSize, paddingBorderMain);
            if (!isNaN(minMain)) {
                hypotheticalInnerMinMain = Math.max(hypotheticalInnerMinMain, minMain);
            }

            // Clamp flex_basis by min and max
            // Per CSS spec, min takes precedence over max, so apply max first then min
            float hypotheticalInnerMain = item.flexBasis;
            if (!isNaN(maxMain)) {
                hypotheticalInnerMain = Math.min(hypotheticalInnerMain, maxMain);
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
        FlexDirection flexDirection,
        Size<AvailableSpace> availableSpace,
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

            // If multiple lines (wrapping), ensure at least the available main-axis size
            if (lines.size() > 1) {
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
                    float marginMain = isRow
                                       ? item.margin.left + item.margin.right
                                       : item.margin.top + item.margin.bottom;

                    float styleMinMain = isRow ? item.minSize.width : item.minSize.height;
                    float stylePreferredMain = isRow ? item.size.width : item.size.height;
                    float styleMaxMain = isRow ? item.maxSize.width : item.maxSize.height;

                    // The spec seems a bit unclear on this point (my initial reading was that the .maybe_max(style_preferred) should
                    // not be included here), however this matches both Chrome and Firefox as of 9th March 2023.
                    //
                    // Spec: https://www.w3.org/TR/css-flexbox-1/#intrinsic-item-contributions
                    // Spec modification: https://www.w3.org/TR/css-flexbox-1/#change-2016-max-contribution
                    float clampingBasis = !Float.isNaN(stylePreferredMain)
                                          ? Math.max(item.flexBasis, stylePreferredMain)
                                          : item.flexBasis;

                    // flex_basis acts as min when flex_shrink == 0
                    float flexBasisMin = item.flexShrink == 0f ? clampingBasis : NaN;
                    // flex_basis acts as max when flex_grow == 0
                    float flexBasisMax = item.flexGrow == 0f ? clampingBasis : NaN;

                    // Compute effective min: max(style_min, flex_basis_min, resolved_minimum_main_size)
                    float minMainSize = item.resolvedMinimumMainSize;
                    if (!Float.isNaN(styleMinMain)) minMainSize = Math.max(minMainSize, styleMinMain);
                    if (!Float.isNaN(flexBasisMin)) minMainSize = Math.max(minMainSize, flexBasisMin);

                    // Compute effective max: min(style_max, flex_basis_max) or infinity
                    float maxMainSize = POSITIVE_INFINITY;
                    if (!Float.isNaN(styleMaxMain)) maxMainSize = styleMaxMain;
                    if (!Float.isNaN(flexBasisMax)) {
                        maxMainSize = Math.min(maxMainSize, flexBasisMax);
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
                        contentContribution = item.flexBasis + marginMain;
                    } else {
                        // Measure child content size in main axis
                        AvailableSpace outerCrossAvail = isRow ? availableSpace.height : availableSpace.width;
                        float crossAxisParentSize = isRow ? nodeInnerSize.height : nodeInnerSize.width;
                        float itemMinCross = isRow ? item.minSize.height : item.minSize.width;
                        float itemMaxCross = isRow ? item.maxSize.height : item.maxSize.width;

                        AvailableSpace crossAxisAvailableSpace;
                        if (outerCrossAvail.isDefinite()) {
                            float val = outerCrossAvail.getValue();
                            float clampedCross = !Float.isNaN(crossAxisParentSize) ? crossAxisParentSize : val;
                            if (!Float.isNaN(itemMinCross)) clampedCross = Math.max(clampedCross, itemMinCross);
                            if (!Float.isNaN(itemMaxCross)) clampedCross = Math.min(clampedCross, itemMaxCross);
                            crossAxisAvailableSpace = AvailableSpace.definite(clampedCross);
                        } else if (outerCrossAvail.isMinContent()) {
                            crossAxisAvailableSpace = !Float.isNaN(itemMinCross) ? AvailableSpace.definite(itemMinCross) : AvailableSpace.minContent();
                        } else {
                            crossAxisAvailableSpace = !Float.isNaN(itemMaxCross) ? AvailableSpace.definite(itemMaxCross) : AvailableSpace.maxContent();
                        }

                        Size<AvailableSpace> childAvailSpace = isRow
                                                               ? new Size<>(availableSpace.width, crossAxisAvailableSpace)
                                                               : new Size<>(crossAxisAvailableSpace, availableSpace.height);

                        // Known dimensions: clear main axis, handle stretch for cross
                        float knownWidth = item.size.width;
                        float knownHeight = item.size.height;
                        if (isRow) {
                            knownWidth = NaN;
                        } else {
                            knownHeight = NaN;
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
                                    float marginCross = isRow
                                                        ? item.margin.top + item.margin.bottom
                                                        : item.margin.left + item.margin.right;
                                    crossKnown = cross - marginCross;
                                    if (isRow) {
                                        knownHeight = crossKnown;
                                    } else {
                                        knownWidth = crossKnown;
                                    }
                                }
                            }
                        }

                        FloatSize childKnownDimensions = new FloatSize(knownWidth, knownHeight);

                        LayoutOutput measured = layoutComputer.performChildLayout(
                            item.nodeId,
                            childKnownDimensions,
                            nodeInnerSize,
                            childAvailSpace,
                            SizingMode.INHERENT_SIZE,
                            new Line<>(false, false)
                        );

                        float measuredMain = isRow ? measured.size().width : measured.size().height;
                        float contentMainSize = measuredMain + marginMain;

                        // Asymmetric behavior between row and column containers (matches Webkit/Firefox):
                        // - Row containers: use content size clamped by min/max
                        // - Column containers: content size is at least flex-basis
                        if (isRow) {
                            contentContribution = TaffyMath.maybeClamp(contentMainSize, styleMinMain, styleMaxMain);
                        } else {
                            contentContribution = Math.max(contentMainSize, item.flexBasis);
                            contentContribution = TaffyMath.maybeClamp(contentContribution, styleMinMain, styleMaxMain);
                        }
                        contentContribution = Math.max(contentContribution, mainContentBoxInset);
                    }

                    // Calculate content_flex_fraction
                    // This represents the "flex fraction" needed to size the item to its content contribution
                    float diff = contentContribution - item.flexBasis;
                    if (diff > 0.0f) {
                        item.contentFlexFraction = diff / Math.max(1.0f, item.flexGrow);
                    } else if (diff < 0.0f) {
                        float scaledShrinkFactor = Math.max(1.0f, item.flexShrink * item.innerFlexBasis);
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
                for (FlexItem item : line.items) {
                    float flexFraction = item.contentFlexFraction;

                    float flexContribution;
                    if (item.contentFlexFraction > 0.0f) {
                        flexContribution = Math.max(1.0f, item.flexGrow) * flexFraction;
                    } else if (item.contentFlexFraction < 0.0f) {
                        float scaledShrinkFactor = Math.max(1.0f, item.flexShrink) * item.innerFlexBasis;
                        flexContribution = scaledShrinkFactor * flexFraction;
                    } else {
                        flexContribution = 0.0f;
                    }

                    float itemSize = item.flexBasis + flexContribution;

                    // Update item's target sizes (used later in layout)
                    if (isRow) {
                        item.outerTargetSize = new FloatSize(itemSize, item.outerTargetSize != null ? item.outerTargetSize.height : Float.NaN);
                        item.targetSize = new FloatSize(itemSize, item.targetSize != null ? item.targetSize.height : Float.NaN);
                    } else {
                        item.outerTargetSize = new FloatSize(item.outerTargetSize != null ? item.outerTargetSize.width : Float.NaN, itemSize);
                        item.targetSize = new FloatSize(item.targetSize != null ? item.targetSize.width : Float.NaN, itemSize);
                    }

                    itemMainSizeSum += itemSize;
                }

                float gapSum = mainGap * Math.max(0, line.items.size() - 1);
                maxLineSum = Math.max(maxLineSum, itemMainSizeSum + gapSum);
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
        Size<AvailableSpace> availableSpace,
        float mainGap,
        boolean isWrap,
        FlexDirection flexDirection,
        FloatSize minSize,
        FloatSize maxSize) {

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

        AvailableSpace mainAxisAvailableSpace;
        if (!Float.isNaN(mainMaxSize)) {
            // If max-size is set, use it as the constraint
            float innerVal = outerMainAvail.intoOption();
            float constrainedSize = !Float.isNaN(innerVal) ? innerVal : mainMaxSize;
            if (!Float.isNaN(mainMinSize)) constrainedSize = Math.max(constrainedSize, mainMinSize);
            mainAxisAvailableSpace = AvailableSpace.definite(constrainedSize);
        } else {
            mainAxisAvailableSpace = outerMainAvail;
        }

        // If MaxContent, all items go in one line (no wrapping)
        if (mainAxisAvailableSpace.isMaxContent()) {
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
        Size<AvailableSpace> availableSpace) {

        boolean isRow = flexDirection.isRow();

        for (FlexItem item : line.items) {
            float paddingBorderCross = isRow
                                       ? item.padding.top + item.padding.bottom + item.border.top + item.border.bottom
                                       : item.padding.left + item.padding.right + item.border.left + item.border.right;

            // Use inner main size as the known main size for child layout
            float containerMainSize = innerMainSize;

            // Get child's explicit cross size if set
            float itemCross = isRow ? item.size.height : item.size.width;
            float itemMinCross = isRow ? item.minSize.height : item.minSize.width;
            float itemMaxCross = isRow ? item.maxSize.height : item.maxSize.width;

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
                if (!Float.isNaN(itemMinCross)) val = Math.max(val, itemMinCross);
                if (!Float.isNaN(itemMaxCross)) val = Math.min(val, itemMaxCross);
                val = Math.max(val, paddingBorderCross);
                availCross = AvailableSpace.definite(val);
            }

            float childInnerCross;
            if (!Float.isNaN(itemCross)) {
                childInnerCross = itemCross;
            } else {
                // Measure child to get cross size using measureChildSize
                FloatSize knownDims = isRow
                                      ? new FloatSize(item.targetSize.width, itemCross)
                                      : new FloatSize(itemCross, item.targetSize.height);

                Size<AvailableSpace> childAvailSpace = isRow
                                                       ? new Size<>(
                    !Float.isNaN(containerMainSize) ? AvailableSpace.definite(containerMainSize) : availableSpace.width,
                    availCross)
                                                       : new Size<>(
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
                    new Line<>(false, false)
                );

                float measuredCross = isRow ? measuredSize.height : measuredSize.width;
                childInnerCross = TaffyMath.clamp(measuredCross, itemMinCross, itemMaxCross);
                childInnerCross = Math.max(childInnerCross, paddingBorderCross);
            }

            float marginCross = isRow
                                ? item.margin.top + item.margin.bottom
                                : item.margin.left + item.margin.right;
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
        Size<AvailableSpace> availableSpace,
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
                if (item.alignSelf == AlignSelf.BASELINE) {
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
                if (item.alignSelf == AlignSelf.BASELINE) {
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
                if (item.alignSelf != AlignSelf.BASELINE) {
                    continue;
                }

                // Perform child layout to get the baseline
                FloatSize knownDimensions = new FloatSize(
                    item.targetSize.width,
                    item.hypotheticalInnerSize.height
                );

                Size<AvailableSpace> childAvailableSpace = new Size<>(
                    isNaN(nodeInnerSize.width) ? availableSpace.width : AvailableSpace.definite(nodeInnerSize.width),
                    isNaN(nodeInnerSize.height) ? availableSpace.height : AvailableSpace.definite(nodeInnerSize.height)
                );

                LayoutOutput output = layoutComputer.performChildLayout(
                    item.nodeId,
                    knownDimensions,
                    nodeInnerSize,
                    childAvailableSpace,
                    SizingMode.CONTENT_SIZE,
                    new Line<>(false, false)
                );

                // Baseline is from the first_baselines.y of child, or default to height
                float childBaseline = output.firstBaselines() != null ? output.firstBaselines().y : NaN;
                float height = output.size().height;

                // baseline = first_baseline (or height if none) + top margin
                item.baseline = (!Float.isNaN(childBaseline) ? childBaseline : height) + item.margin.top;
            }
        }
    }

    private void calculateCrossSize(
        List<FlexLine> lines,
        FloatSize nodeInnerSize,
        Size<AvailableSpace> availableSpace,
        FlexDirection flexDirection,
        Style containerStyle,
        FloatSize minSize,
        FloatSize maxSize,
        FloatRect contentBoxInset) {

        boolean isRow = flexDirection.isRow();
        float innerCrossSize = isRow ? nodeInnerSize.height : nodeInnerSize.width;

        // Get original cross-axis available space for fallback
        AvailableSpace crossAxisAvailSpace = isRow ? availableSpace.height : availableSpace.width;

        // If the flex container is single-line and has a definite cross size,
        // the cross size of the flex line is the flex container's inner cross size.
        boolean isWrap = containerStyle.getFlexWrap() == FlexWrap.WRAP ||
                         containerStyle.getFlexWrap() == FlexWrap.WRAP_REVERSE;
        if (!isWrap && lines.size() == 1 && !Float.isNaN(innerCrossSize)) {
            // When container has a definite cross size, line cross size equals the container's inner cross size
            // No need to apply min/max here - those constraints were already applied when determining container size
            float lineCrossSize = innerCrossSize;
            lineCrossSize = Math.max(lineCrossSize, 0);

            var first = lines.get(0);
            first.crossSize = lineCrossSize;

            // Still need to calculate each item's cross size for layout
            for (FlexItem item : first.items) {
                float itemCrossSize = isRow ? item.size.height : item.size.width;
                float itemMinCross = isRow ? item.minSize.height : item.minSize.width;
                float itemMaxCross = isRow ? item.maxSize.height : item.maxSize.width;

                float paddingBorderCross = isRow
                                           ? item.padding.top + item.padding.bottom + item.border.top + item.border.bottom
                                           : item.padding.left + item.padding.right + item.border.left + item.border.right;

                float crossSize;
                if (!Float.isNaN(itemCrossSize)) {
                    crossSize = TaffyMath.clamp(itemCrossSize, itemMinCross, itemMaxCross);
                } else {
                    // Measure to get content size
                    float targetMain = isRow ? item.targetSize.width : item.targetSize.height;
                    FloatSize knownDimensions = isRow
                                                ? new FloatSize(targetMain, NaN)
                                                : new FloatSize(NaN, targetMain);

                    Size<AvailableSpace> availSpace = new Size<>(
                        isRow ? AvailableSpace.definite(targetMain) : AvailableSpace.definite(innerCrossSize),
                        isRow ? AvailableSpace.definite(innerCrossSize) : AvailableSpace.definite(targetMain)
                    );

                    // Use measureChildSize to avoid setting unroundedLayout prematurely
                    FloatSize measuredSize = layoutComputer.measureChildSize(
                        item.nodeId,
                        knownDimensions,
                        nodeInnerSize,
                        availSpace,
                        SizingMode.CONTENT_SIZE,
                        new Line<>(false, false)
                    );

                    crossSize = isRow ? measuredSize.height : measuredSize.width;
                    crossSize = TaffyMath.clamp(crossSize, itemMinCross, itemMaxCross);
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
            }
            return;
        }

        for (FlexLine line : lines) {
            float maxCross = 0;

            for (FlexItem item : line.items) {
                // Get item's cross size from resolved style size
                float itemCrossSize = isRow ? item.size.height : item.size.width;
                float itemMinCross = isRow ? item.minSize.height : item.minSize.width;
                float itemMaxCross = isRow ? item.maxSize.height : item.maxSize.width;

                // Apply min/max constraints
                float paddingBorderCross = isRow
                                           ? item.padding.top + item.padding.bottom + item.border.top + item.border.bottom
                                           : item.padding.left + item.padding.right + item.border.left + item.border.right;

                // If item has a definite cross size, use it; otherwise measure
                float crossSize;
                if (!Float.isNaN(itemCrossSize)) {
                    crossSize = TaffyMath.clamp(itemCrossSize, itemMinCross, itemMaxCross);
                } else {
                    // Calculate cross size by actually measuring the item
                    float targetMain = isRow ? item.targetSize.width : item.targetSize.height;
                    FloatSize knownDimensions = isRow
                                                ? new FloatSize(targetMain, NaN)
                                                : new FloatSize(NaN, targetMain);

                    float availCross = innerCrossSize;
                    if (!Float.isNaN(itemMinCross)) {
                        availCross = TaffyMath.maybeMax(availCross, itemMinCross);
                    }
                    if (!Float.isNaN(itemMaxCross)) {
                        availCross = TaffyMath.maybeMin(availCross, itemMaxCross);
                    }
                    availCross = TaffyMath.maybeMax(availCross, paddingBorderCross);

                    Size<AvailableSpace> availSpace = new Size<>(
                        isRow ? AvailableSpace.definite(targetMain)
                              : (!Float.isNaN(availCross) ? AvailableSpace.definite(availCross) : crossAxisAvailSpace),
                        isRow ? (!Float.isNaN(availCross) ? AvailableSpace.definite(availCross) : crossAxisAvailSpace)
                              : AvailableSpace.definite(targetMain)
                    );

                    // Use measureChildSize to avoid setting unroundedLayout prematurely
                    FloatSize measuredSize = layoutComputer.measureChildSize(
                        item.nodeId,
                        knownDimensions,
                        nodeInnerSize,
                        availSpace,
                        SizingMode.CONTENT_SIZE,
                        new Line<>(false, false)
                    );

                    crossSize = isRow ? measuredSize.height : measuredSize.width;
                    crossSize = TaffyMath.clamp(crossSize, itemMinCross, itemMaxCross);
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

                // Store in hypothetical outer size for baseline calculations
                if (isRow) {
                    item.hypotheticalOuterSize = new FloatSize(item.hypotheticalOuterSize.width, crossSize + marginCross);
                } else {
                    item.hypotheticalOuterSize = new FloatSize(crossSize + marginCross, item.hypotheticalOuterSize.height);
                }

                maxCross = Math.max(maxCross, crossSize + marginCross);
            }

            // Calculate max baseline among baseline-aligned items
            float maxBaseline = 0;
            for (FlexItem item : line.items) {
                if (item.alignSelf == AlignSelf.BASELINE) {
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
                boolean crossStartAuto = isRow ? item.marginIsAuto.top : item.marginIsAuto.left;
                boolean crossEndAuto = isRow ? item.marginIsAuto.bottom : item.marginIsAuto.right;
                float itemHypotheticalOuterCross = isRow ? item.hypotheticalOuterSize.height : item.hypotheticalOuterSize.width;

                if (item.alignSelf == AlignSelf.BASELINE && !crossStartAuto && !crossEndAuto) {
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

            // Clamp line.crossSize with container's min/max inner cross size
            line.crossSize = TaffyMath.clamp(line.crossSize, minInnerCross, maxInnerCross);
        }
    }

    private void distributeRemainingMainSpace(
        List<FlexLine> lines,
        Style containerStyle,
        float innerMainSize,
        float mainGap,
        FlexDirection flexDirection) {

        if (Float.isNaN(innerMainSize)) return;

        boolean isRow = flexDirection.isRow();
        boolean isReverse = flexDirection.isReverse();
        JustifyContent justify = containerStyle.getJustifyContent();

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
                // Apply justify-content
                // Use compute_alignment_offset logic from Rust - works for both positive and negative space
                float initialOffset = 0;
                float betweenOffset = 0;
                int itemCount = line.items.size();

                // Apply alignment fallback following CSS spec and Rust implementation
                // https://www.w3.org/TR/css-align-3/ and https://github.com/w3c/csswg-drafts/issues/10154
                JustifyContent effectiveJustify = justify;
                boolean isSafe = false;

                // 1. If there is only a single item or negative free space, fallback distributed alignments
                if (itemCount <= 1 || freeSpace <= 0) {
                    switch (justify) {
                        case SPACE_BETWEEN:
                            effectiveJustify = JustifyContent.FLEX_START;
                            isSafe = true;
                            break;
                        case SPACE_AROUND:
                        case SPACE_EVENLY:
                            effectiveJustify = JustifyContent.CENTER;
                            isSafe = true;
                            break;
                        default:
                            break;
                    }
                }

                // 2. If free space is negative and "safe" alignment, fallback to Start
                if (freeSpace <= 0 && isSafe) {
                    effectiveJustify = JustifyContent.START;
                }

                // Calculate initial offset (for first item)
                switch (effectiveJustify) {
                    case START, SPACE_BETWEEN -> initialOffset = 0;
                    case FLEX_START -> initialOffset = isReverse ? freeSpace : 0;
                    case END -> initialOffset = freeSpace;
                    case FLEX_END -> initialOffset = isReverse ? 0 : freeSpace;
                    case CENTER -> initialOffset = freeSpace / 2;
                    case SPACE_AROUND -> {
                        if (freeSpace >= 0) {
                            initialOffset = (freeSpace / itemCount) / 2;
                        } else {
                            initialOffset = freeSpace / 2;
                        }
                    }
                    case SPACE_EVENLY -> {
                        if (freeSpace >= 0) {
                            initialOffset = freeSpace / (itemCount + 1);
                        } else {
                            initialOffset = freeSpace / 2;
                        }
                    }
                }

                // Calculate between offset (for subsequent items) - only for positive space
                float effectiveFreeSpace = Math.max(freeSpace, 0);
                switch (effectiveJustify) {
                    case SPACE_BETWEEN -> {
                        if (itemCount > 1) {
                            betweenOffset = effectiveFreeSpace / (itemCount - 1);
                        }
                    }
                    case SPACE_AROUND -> {
                        if (itemCount > 0) {
                            betweenOffset = effectiveFreeSpace / itemCount;
                        }
                    }
                    case SPACE_EVENLY -> betweenOffset = effectiveFreeSpace / (itemCount + 1);
                }

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
            for (FlexItem item : line.items) {
                if (item.alignSelf == AlignSelf.BASELINE) {
                    maxBaseline = Math.max(maxBaseline, item.baseline);
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
                item.offsetCross = computeAlignItemsOffset(item, freeSpace, maxBaseline, isRow, isWrapReverse);
            }
        }
    }

    /**
     * Compute the cross-axis offset for a flex item based on align-self.
     * This follows the Rust align_flex_items_along_cross_axis function exactly.
     * Note: Java's AlignSelf maps START->FLEX_START and END->FLEX_END in fromAlignItems,
     * so we don't need separate START/END cases here.
     */
    private float computeAlignItemsOffset(FlexItem item, float freeSpace, float maxBaseline, boolean isRow, boolean isWrapReverse) {
        AlignSelf alignSelf = item.alignSelf;

        switch (alignSelf) {
            case FLEX_START:
                return isWrapReverse ? freeSpace : 0;
            case FLEX_END:
                return isWrapReverse ? 0 : freeSpace;
            case CENTER:
                return freeSpace / 2;
            case BASELINE:
                if (isRow) {
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
        Style style,
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

            // For STRETCH with single line no-wrap, still distribute extra space to line
            AlignContent alignContent = style.getAlignContent();
            if (alignContent == null) {
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
        AlignContent rawAlignContent = style.getAlignContent();
        if (rawAlignContent == null) {
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
        AlignContent alignContent = rawAlignContent;
        boolean isSafe = false;

        if (numLines <= 1 || freeSpace <= 0) {
            switch (alignContent) {
                case STRETCH, SPACE_BETWEEN -> {
                    alignContent = AlignContent.FLEX_START;
                    isSafe = true;
                }
                case SPACE_AROUND, SPACE_EVENLY -> {
                    alignContent = AlignContent.CENTER;
                    isSafe = true;
                }
                default -> {
                    // no-op
                }
            }
        }

        if (freeSpace <= 0 && isSafe) {
            alignContent = AlignContent.START;
        }

        // Compute line offsets following Rust `compute_alignment_offset` semantics.
        // Rust stores offsets as *deltas* between lines and accumulates them during the final layout pass.
        // Java stores `line.offsetCross` as an *absolute* offset from the content-box cross start,
        // so we accumulate into a cursor here.
        float initialOffset;
        switch (alignContent) {
            case START, SPACE_BETWEEN -> initialOffset = 0;
            case FLEX_START -> initialOffset = isWrapReverse ? freeSpace : 0;
            case END -> initialOffset = freeSpace;
            case FLEX_END -> initialOffset = isWrapReverse ? 0 : freeSpace;
            case CENTER -> initialOffset = freeSpace / 2;
            case STRETCH -> initialOffset = 0;
            case SPACE_AROUND -> {
                if (freeSpace >= 0) {
                    initialOffset = (freeSpace / numLines) / 2;
                } else {
                    initialOffset = freeSpace / 2;
                }
            }
            case SPACE_EVENLY -> {
                if (freeSpace >= 0) {
                    initialOffset = freeSpace / (numLines + 1);
                } else {
                    initialOffset = freeSpace / 2;
                }
            }
            default -> initialOffset = 0;
        }

        float effectiveFreeSpace = Math.max(freeSpace, 0);
        float betweenExtra = 0;
        switch (alignContent) {
            case SPACE_BETWEEN -> betweenExtra = effectiveFreeSpace / (numLines - 1);
            case SPACE_AROUND -> betweenExtra = effectiveFreeSpace / numLines;
            case SPACE_EVENLY -> betweenExtra = effectiveFreeSpace / (numLines + 1);
            default -> {
                // no-op
            }
        }

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
    private void determineUsedCrossSize(List<FlexLine> lines, FlexDirection flexDirection, Style containerStyle) {
        boolean isRow = flexDirection.isRow();

        for (FlexLine line : lines) {
            float lineCrossSize = line.crossSize;

            for (FlexItem item : line.items) {
                // Check if item should stretch
                AlignSelf alignSelf = item.alignSelf;
                if (alignSelf == null) {
                    AlignItems alignItems = containerStyle.getAlignItems();
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

                if (shouldStretch && crossSizeIsAuto && marginsAreNotAuto) {
                    // Stretch the item to fill the line's cross size
                    float marginCross = isRow
                                        ? item.margin.top + item.margin.bottom
                                        : item.margin.left + item.margin.right;

                    float stretchedCrossSize = lineCrossSize - marginCross;

                    // Apply min/max constraints
                    float minCross = isRow ? item.minSize.height : item.minSize.width;
                    float maxCross = isRow ? item.maxSize.height : item.maxSize.width;
                    stretchedCrossSize = TaffyMath.clamp(stretchedCrossSize, minCross, maxCross);
                    stretchedCrossSize = Math.max(stretchedCrossSize, 0);

                    // Update target size
                    if (isRow) {
                        item.targetSize = new FloatSize(item.targetSize.width, stretchedCrossSize);
                        item.outerTargetSize = new FloatSize(item.outerTargetSize.width, stretchedCrossSize + marginCross);
                    } else {
                        item.targetSize = new FloatSize(stretchedCrossSize, item.targetSize.height);
                        item.outerTargetSize = new FloatSize(stretchedCrossSize + marginCross, item.outerTargetSize.height);
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
        float innerMainSize) {

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
            // Use known dimension if available
            containerMain = knownMain;
        } else if (!Float.isNaN(innerMainSize)) {
            // Use the computed innerMainSize from determineContainerMainSize directly.
            // Don't take max with contentMain because contentMain may use updated gap values
            // (from re-resolving percentage gaps) while innerMainSize was computed with original gap.
            // This follows Rust behavior where container_size.main is set once and not recalculated.
            containerMain = innerMainSize + mainContentBoxInset;
        } else {
            containerMain = contentMain + mainContentBoxInset;
        }

        float containerCross = styledBasedKnownDimensions != null && !isNaN((isRow ? styledBasedKnownDimensions.height : styledBasedKnownDimensions.width))
                               ? (isRow ? styledBasedKnownDimensions.height : styledBasedKnownDimensions.width)
                               : contentCross + (isRow ? contentBoxInset.top + contentBoxInset.bottom : contentBoxInset.left + contentBoxInset.right);

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
     * Performs final layout of all flex items and returns the container's first vertical baseline.
     */
    private float performFinalLayout(
        List<FlexLine> lines,
        NodeId node,
        FloatSize containerSize,
        FloatRect contentBoxInset,
        FloatSize gap,
        Style containerStyle,
        FlexDirection flexDirection,
        boolean isWrapReverse) {

        TaffyTree tree = layoutComputer.getTree();
        boolean isRow = flexDirection.isRow();
        boolean isReverse = flexDirection.isReverse();
        float mainGap = isRow ? gap.width : gap.height;

        // Content box offset
        float contentBoxCrossOffset = isRow ? contentBoxInset.top : contentBoxInset.left;

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
                // Look for baseline-aligned item or fall back to first item
                for (FlexItem item : firstLine.items) {
                    if (isRow && item.alignSelf == AlignSelf.BASELINE) {
                        firstBaselineItem = item;
                        break;
                    }
                }
                if (firstBaselineItem == null) {
                    firstBaselineItem = firstLine.items.get(0);
                }
            }
        }

        for (FlexLine line : orderedLines) {
            float mainOffset = isRow ? contentBoxInset.left : contentBoxInset.top;
            float crossOffset = contentBoxCrossOffset + line.offsetCross;

            // Handle reverse direction
            List<FlexItem> orderedItems = isReverse ? new ArrayList<>(line.items) : line.items;
            if (isReverse) {
                java.util.Collections.reverse(orderedItems);
            }

            for (FlexItem item : orderedItems) {
                // Perform child layout with target size as known dimensions
                FloatSize knownDimensions = item.targetSize;

                LayoutOutput output = layoutComputer.performChildLayout(
                    item.nodeId,
                    knownDimensions,
                    nodeInnerSize,
                    new Size<>(AvailableSpace.definite(containerSize.width), AvailableSpace.definite(containerSize.height)),
                    SizingMode.CONTENT_SIZE,
                    new Line<>(false, false)
                );

                // Apply offsets
                float x, y;
                if (isRow) {
                    x = mainOffset + item.offsetMain + item.margin.left;
                    y = crossOffset + item.offsetCross + item.margin.top;
                } else {
                    x = crossOffset + item.offsetCross + item.margin.left;
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

                // Update item baseline for container baseline calculation
                // Per CSS spec: baseline = baseline_offset_cross + inner_baseline
                if (isRow) {
                    float baselineOffsetCross = crossOffset + item.offsetCross + item.margin.top;
                    float innerBaseline = output.firstBaselines() != null ? output.firstBaselines().y : NaN;
                    item.baseline = baselineOffsetCross + (!Float.isNaN(innerBaseline) ? innerBaseline : actualSize.height);
                } else {
                    float baselineOffsetMain = mainOffset + item.offsetMain + item.margin.top;
                    float innerBaseline = output.firstBaselines() != null ? output.firstBaselines().y : NaN;
                    item.baseline = baselineOffsetMain + (!Float.isNaN(innerBaseline) ? innerBaseline : actualSize.height);
                }

                // Calculate container's first vertical baseline
                if (item == firstBaselineItem) {
                    firstVerticalBaseline = item.baseline;
                }

                Layout layout = new Layout(
                    item.order,
                    new FloatPoint(x, y),
                    actualSize,
                    output.contentSize() != null ? output.contentSize() : actualSize,
                    scrollbarSize,
                    item.border,
                    item.padding,
                    item.margin
                );

                tree.setUnroundedLayout(item.nodeId, layout);

                mainOffset += (isRow ? item.outerTargetSize.width : item.outerTargetSize.height) + mainGap;
            }
        }

        // Layout absolutely positioned children
        // Need border and scrollbar gutter for absolute layout
        FloatRect border = Resolve.resolveRectOrZero(containerStyle.getBorder(), containerSize.width);
        float scrollbarWidth = containerStyle.getScrollbarWidth();
        Point<Overflow> overflow = containerStyle.getOverflow();
        FloatSize scrollbarGutter = new FloatSize(
            overflow.y == Overflow.SCROLL ? scrollbarWidth : 0f,
            overflow.x == Overflow.SCROLL ? scrollbarWidth : 0f
        );

        // Pass null for justifyContent when not set to get correct default (START)
        JustifyContent jc = containerStyle.justifyContent != null ? containerStyle.getJustifyContent() : null;
        layoutAbsoluteChildren(node, containerSize, contentBoxInset, flexDirection,
            containerStyle.getAlignItems(), jc, isWrapReverse, border, scrollbarGutter);

        return firstVerticalBaseline;
    }

    private void layoutAbsoluteChildren(NodeId node, FloatSize containerSize, FloatRect contentBoxInset,
                                        FlexDirection flexDirection, AlignItems alignItems, JustifyContent justifyContent,
                                        boolean isWrapReverse, FloatRect border, FloatSize scrollbarGutter) {
        TaffyTree tree = layoutComputer.getTree();
        boolean isRow = flexDirection.isRow();

        for (NodeId childId : tree.getChildren(node)) {
            Style childStyle = tree.getStyle(childId);
            if (childStyle.getPosition() != Position.ABSOLUTE) continue;
            if (childStyle.getBoxGenerationMode() == BoxGenerationMode.NONE) continue;

            // Area available for positioning (container size minus border and scrollbar gutter)
            float insetRelativeWidth = containerSize.width - border.left - border.right - scrollbarGutter.width;
            float insetRelativeHeight = containerSize.height - border.top - border.bottom - scrollbarGutter.height;

            Rect<LengthPercentageAuto> insetStyle = childStyle.getInset();
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

            // Fill from insets if not set
            if (isNaN(knownDimensions.width) && !isNaN(left) && !isNaN(right)) {
                float newWidth = TaffyMath.maybeSub(TaffyMath.maybeSub(insetRelativeWidth, marginOption.left), marginOption.right);
                knownDimensions = new FloatSize(
                    Math.max(!isNaN(newWidth) ? newWidth - left - right : 0f, 0f),
                    knownDimensions.height
                );
                knownDimensions = maybeClamp(maybeApplyAspectRatio(knownDimensions, aspectRatio), minSz, maxSz);
            }
            if (isNaN(knownDimensions.height) && !isNaN(top) && !isNaN(bottom)) {
                float newHeight = TaffyMath.maybeSub(TaffyMath.maybeSub(insetRelativeHeight, marginOption.top), marginOption.bottom);
                knownDimensions = new FloatSize(
                    knownDimensions.width,
                    Math.max(!isNaN(newHeight) ? newHeight - top - bottom : 0f, 0f)
                );
                knownDimensions = maybeClamp(maybeApplyAspectRatio(knownDimensions, aspectRatio), minSz, maxSz);
            }

            LayoutOutput output = layoutComputer.performChildLayout(
                childId,
                knownDimensions,
                new FloatSize(insetRelativeWidth, insetRelativeHeight),
                new Size<>(AvailableSpace.definite(containerSize.width), AvailableSpace.definite(containerSize.height)),
                // Absolute positioned nodes need inherent sizing so that min/max constraints and aspect-ratio
                // resolution are applied by leaf/layout algorithms.
                SizingMode.INHERENT_SIZE,
                new Line<>(false, false)
            );

            FloatSize finalSize = maybeClamp(
                new FloatSize(
                    !isNaN(knownDimensions.width) ? knownDimensions.width : output.size().width,
                    !isNaN(knownDimensions.height) ? knownDimensions.height : output.size().height
                ),
                minSz, maxSz
            );

            // Calculate free space and expand auto margins
            FloatSize freeSpace = new FloatSize(
                Math.max(0f, containerSize.width - finalSize.width - margin.left - margin.right),
                Math.max(0f, containerSize.height - finalSize.height - margin.top - margin.bottom)
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
            if (alignSelf == null) alignSelf = alignItems;
            AlignSelf alignSelfEnum = AlignSelf.fromAlignItems(alignSelf);

            // Calculate main-axis offset
            float offsetMain = computeAbsoluteMainOffset(
                startMain, endMain, finalSize, containerSize, border, scrollbarGutter,
                contentBoxInset, resolvedMargin, justifyContent, isRow, isWrapReverse
            );

            // Calculate cross-axis offset
            float offsetCross = computeAbsoluteCrossOffset(
                startCross, endCross, finalSize, containerSize, border, scrollbarGutter,
                contentBoxInset, resolvedMargin, alignSelfEnum, isRow, isWrapReverse
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

            Point<Overflow> overflow = childStyle.getOverflow();
            FloatSize scrollbarSize = new FloatSize(
                overflow.y == Overflow.SCROLL ? childStyle.getScrollbarWidth() : 0f,
                overflow.x == Overflow.SCROLL ? childStyle.getScrollbarWidth() : 0f
            );

            Layout layout = new Layout(
                0,
                new FloatPoint(x, y),
                finalSize,
                output.contentSize(),
                scrollbarSize,
                itemBorder,
                itemPadding,
                resolvedMargin
            );

            tree.setUnroundedLayout(childId, layout);
        }
    }

    private FloatRect resolveMarginToOption(Rect<LengthPercentageAuto> margin, float parentWidth) {
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
                                            JustifyContent justifyContent, boolean isRow, boolean isWrapReverse) {
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
            // Use justify-content to position
            JustifyContent jc = justifyContent != null ? justifyContent : JustifyContent.START;
            return switch (jc) {
                case SPACE_BETWEEN, START -> contentBoxMainStart + marginMainStart;
                case FLEX_START -> {
                    // FLEX_START behaves like START normally, like END when wrap-reverse
                    if (isWrapReverse) yield containerMain - contentBoxMainEnd - finalMain - marginMainEnd;
                    yield contentBoxMainStart + marginMainStart;
                }
                case END -> containerMain - contentBoxMainEnd - finalMain - marginMainEnd;
                case FLEX_END -> {
                    if (isWrapReverse) yield contentBoxMainStart + marginMainStart;
                    yield containerMain - contentBoxMainEnd - finalMain - marginMainEnd;
                }
                case SPACE_EVENLY, SPACE_AROUND, CENTER ->
                    (containerMain + contentBoxMainStart - contentBoxMainEnd - finalMain
                     + marginMainStart - marginMainEnd) / 2.0f;
            };
        }
    }

    private float computeAbsoluteCrossOffset(float startCross, float endCross, FloatSize finalSize,
                                             FloatSize containerSize, FloatRect border, FloatSize scrollbarGutter,
                                             FloatRect contentBoxInset, FloatRect resolvedMargin,
                                             AlignSelf alignSelf, boolean isRow, boolean isWrapReverse) {
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
            // Use align-self to position
            // Stretch does not apply to absolutely positioned items, treat as flex-start
            // AlignSelf enum: AUTO, FLEX_START, FLEX_END, CENTER, BASELINE, STRETCH
            return switch (alignSelf) {
                case BASELINE, STRETCH, FLEX_START -> {
                    if (isWrapReverse) yield containerCross - contentBoxCrossEnd - finalCross - marginCrossEnd;
                    yield contentBoxCrossStart + marginCrossStart;
                }
                case FLEX_END -> {
                    if (isWrapReverse) yield contentBoxCrossStart + marginCrossStart;
                    yield containerCross - contentBoxCrossEnd - finalCross - marginCrossEnd;
                }
                case CENTER -> (containerCross + contentBoxCrossStart - contentBoxCrossEnd - finalCross
                                + marginCrossStart - marginCrossEnd) / 2.0f;
                default -> contentBoxCrossStart + marginCrossStart;
            };
        }
    }

    // Helper methods
    private FloatSize maybeApplyAspectRatio(FloatSize size, Float aspectRatio) {
        if (aspectRatio == null) return size;
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
}
