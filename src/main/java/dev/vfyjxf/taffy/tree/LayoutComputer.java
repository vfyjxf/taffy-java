package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.util.MeasureContext;
import dev.vfyjxf.taffy.util.MeasureFunc;
import dev.vfyjxf.taffy.util.MeasureFuncAdapter;
import dev.vfyjxf.taffy.util.MeasureFunction;
import dev.vfyjxf.taffy.util.Resolve;
import dev.vfyjxf.taffy.util.TaffyMath;

import java.util.List;
import java.util.Objects;

/**
 * Computes layout for a TaffyTree.
 */
public class LayoutComputer {

    private final TaffyTree tree;
    private final MeasureFunc defaultMeasureFunc;
    private final MeasureFunction defaultMeasureFunction;

    /**
     * Creates a layout computer with a legacy default measure function.
     *
     * <p>Kept for compatibility; equivalent to
     * {@code new LayoutComputer(tree, defaultMeasureFunc, null)}.</p>
     */
    public LayoutComputer(TaffyTree tree, MeasureFunc defaultMeasureFunc) {
        this(tree, defaultMeasureFunc, null);
    }

    /**
     * Creates a layout computer with both a legacy and a new-style default measure function
     * (S1, 1.2 additive).
     *
     * <p>Both defaults are fallbacks for leaves without a per-node callback. Per-node callbacks
     * always take precedence over the defaults: the selection order is node
     * {@link MeasureFunction}, node legacy {@link MeasureFunc}, default {@link MeasureFunction},
     * default legacy {@link MeasureFunc}; see {@link #measureLeaf}.</p>
     *
     * @param tree                    the tree being laid out
     * @param defaultMeasureFunc      the legacy default measure callback, or {@code null}
     * @param defaultMeasureFunction  the new-style default measure callback, or {@code null}
     */
    public LayoutComputer(TaffyTree tree, MeasureFunc defaultMeasureFunc,
                          MeasureFunction defaultMeasureFunction) {
        this.tree = tree;
        this.defaultMeasureFunc = defaultMeasureFunc;
        this.defaultMeasureFunction = defaultMeasureFunction;
    }

    /**
     * Computes layout starting from the root node.
     */
    public void computeLayout(NodeId root, TaffySize<AvailableSpace> availableSpace) {
        FloatSize knownDimensions = new FloatSize(Float.NaN, Float.NaN);

        TaffyStyle style = tree.getStyle(root);

        // For block nodes, compute known dimensions based on style and available space
        if (style.getDisplay() == TaffyDisplay.BLOCK) {
            FloatSize parentSize = availableSpaceToOptionSize(availableSpace);

            Float aspectRatio = style.getAspectRatio();
            FloatRect margin = Resolve.resolveRectLpaOrZero(style.getMargin(), parentSize.width);
            FloatRect padding = Resolve.resolveRectOrZero(style.getPadding(), parentSize.width);
            FloatRect border = Resolve.resolveRectOrZero(style.getBorder(), parentSize.width);

            FloatSize paddingBorderSize = new FloatSize(
                padding.left + padding.right + border.left + border.right,
                padding.top + padding.bottom + border.top + border.bottom
            );

            FloatSize boxSizingAdjustment = style.getBoxSizing() == BoxSizing.CONTENT_BOX
                                            ? paddingBorderSize
                                            : new FloatSize(0f, 0f);

            FloatSize size2 = Resolve.maybeResolveSize(style.getMinSize(), parentSize);
            FloatSize minSize = Resolve.maybeApplyAspectRatio(size2, aspectRatio);
            minSize = maybeAdd(minSize, boxSizingAdjustment);

            FloatSize size1 = Resolve.maybeResolveSize(style.getMaxSize(), parentSize);
            FloatSize maxSize = Resolve.maybeApplyAspectRatio(size1, aspectRatio);
            maxSize = maybeAdd(maxSize, boxSizingAdjustment);

            FloatSize size = Resolve.maybeResolveSize(style.getSize(), parentSize);
            FloatSize styleSize = Resolve.maybeApplyAspectRatio(size, aspectRatio);
            styleSize = maybeAdd(styleSize, boxSizingAdjustment);
            styleSize = maybeClamp(styleSize, minSize, maxSize);

            // Min-max definite size
            FloatSize minMaxDefiniteSize = new FloatSize(
                (!Float.isNaN(minSize.width) && !Float.isNaN(maxSize.width) && maxSize.width <= minSize.width)
                ? minSize.width : Float.NaN,
                (!Float.isNaN(minSize.height) && !Float.isNaN(maxSize.height) && maxSize.height <= minSize.height)
                ? minSize.height : Float.NaN
            );

            // Block nodes stretch width to available space
            float availableWidth = availableSpace.width.isDefinite()
                                   ? TaffyMath.maybeSub(availableSpace.width.getValue(), margin.left + margin.right)
                                   : Float.NaN;
            FloatSize availableSpaceBasedSize = new FloatSize(availableWidth, Float.NaN);

            knownDimensions = orChain(
                knownDimensions,
                minMaxDefiniteSize,
                styleSize,
                availableSpaceBasedSize
            );
            knownDimensions = maybeMax(knownDimensions, paddingBorderSize);
        }

        // Compute layout recursively
        LayoutOutput output = performChildLayout(
            root,
            knownDimensions,
            availableSpaceToOptionSize(availableSpace),
            availableSpace,
            SizingMode.INHERENT_SIZE,
            new TaffyLine<>(false, false)
        );

        // Get style properties for final layout
        float contextWidth = availableSpace.width.isDefinite() ? availableSpace.width.getValue() : Float.NaN;
        FloatRect padding = Resolve.resolveRectOrZero(style.getPadding(), contextWidth);
        FloatRect border = Resolve.resolveRectOrZero(style.getBorder(), contextWidth);
        FloatRect margin = Resolve.resolveRectLpaOrZero(style.getMargin(), contextWidth);

        FloatSize scrollbarSize = new FloatSize(
            style.getOverflow().y == Overflow.SCROLL ? style.getScrollbarWidth() : 0f,
            style.getOverflow().x == Overflow.SCROLL ? style.getScrollbarWidth() : 0f
        );

        // Set the root layout.
        // PA-03: in RTL the root's margin box is aligned to the right edge of the available space:
        //   x = available_width - size.width - margin.right
        // which matches the upstream root RTL branch (taffy compute/mod.rs).
        // Non-RTL, or an indefinite available width, keeps the historical origin (0, 0): the rest of the
        // upstream root placement (margin box at the origin, i.e. x = margin.left / y = margin.top, plus
        // relative inset offsets) belongs to ST-06/P6 and is still pending here.
        // Child locations stay parent-relative and are not affected by this.
        TaffyDirection direction = resolveDirection(root);
        float rootX = 0f;
        if (direction == TaffyDirection.RTL && availableSpace.width.isDefinite()) {
            rootX = availableSpace.width.getValue() - output.size().width - margin.right;
        }

        Layout layout = new Layout(
            0,
            new FloatPoint(rootX, 0f),
            output.size(),
            output.contentSize(),
            scrollbarSize,
            border,
            padding,
            margin
        );

        tree.setUnroundedLayout(root, layout);
    }

    /**
     * Performs layout for a child node (final layout).
     *
     * <p>Equivalent to {@link #performChildLayout(NodeId, FloatSize, FloatSize, TaffySize, SizingMode,
     * TaffyLine, RunMode)} with {@link RunMode#PERFORM_LAYOUT}: the child is laid out and its
     * subtree's layout state is written to the tree. Kept as the compatibility signature for
     * callers that always want a final layout.</p>
     */
    public LayoutOutput performChildLayout(
        NodeId node,
        FloatSize knownDimensions,
        FloatSize parentSize,
        TaffySize<AvailableSpace> availableSpace,
        SizingMode sizingMode,
        TaffyLine<Boolean> verticalMarginsAreCollapsible) {

        return performChildLayout(node, knownDimensions, parentSize, availableSpace, sizingMode,
            verticalMarginsAreCollapsible, RunMode.PERFORM_LAYOUT);
    }

    /**
     * Performs layout for a child node using the parent's {@link RunMode} (BK-02, upstream #972).
     *
     * <p>A layout algorithm that is itself only being measured must hand
     * {@link RunMode#COMPUTE_SIZE} down to its children, otherwise the children perform a real
     * layout and write layout state to the tree during a measurement (defeating BK-01) and skip
     * the margin-collapse reporting that a vertical {@code ComputeSize} pass is expected to
     * return (BK-03). Passing {@link RunMode#PERFORM_LAYOUT} is exactly the behaviour of the
     * legacy overload.</p>
     *
     * @param runMode the parent's run mode, propagated to the child
     */
    public LayoutOutput performChildLayout(
        NodeId node,
        FloatSize knownDimensions,
        FloatSize parentSize,
        TaffySize<AvailableSpace> availableSpace,
        SizingMode sizingMode,
        TaffyLine<Boolean> verticalMarginsAreCollapsible,
        RunMode runMode) {

        // TODO(CR-01): upstream taffy passes the axis that the caller actually needs (block layout
        // even has a single-axis early-return). This port has no enabled RequestedAxis caller yet
        // (FL-25), so every layout pass requests BOTH. LayoutCache already keys on the other
        // inputs; the axis is the remaining unreachable part of CR-01.
        LayoutInput inputs = new LayoutInput(
            runMode,
            sizingMode,
            RequestedAxis.BOTH,
            knownDimensions,
            parentSize,
            availableSpace,
            verticalMarginsAreCollapsible
        );

        return computeChildLayout(node, inputs);
    }

    /**
     * Measures a child node's size without performing full layout.
     * Uses COMPUTE_SIZE run mode which doesn't set unrounded layout.
     * This should be used during sizing calculations (like flex base size)
     * where we only need the computed size, not the full layout.
     */
    public FloatSize measureChildSize(
        NodeId node,
        FloatSize knownDimensions,
        FloatSize parentSize,
        TaffySize<AvailableSpace> availableSpace,
        SizingMode sizingMode,
        TaffyLine<Boolean> verticalMarginsAreCollapsible) {

        // TODO(CR-01): the axis is not requested here either; see performChildLayout. When a
        // single-axis caller exists, pass RequestedAxis.from(axis) and let LayoutCache validate it.
        LayoutInput inputs = new LayoutInput(
            RunMode.COMPUTE_SIZE,
            sizingMode,
            RequestedAxis.BOTH,
            knownDimensions,
            parentSize,
            availableSpace,
            verticalMarginsAreCollapsible
        );

        return computeChildLayout(node, inputs).size();
    }

    /**
     * Performs the definite-size (second) layout pass of an absolutely positioned child (PA-05).
     *
     * <p>The first pass (the caller's {@code measuredOutput}) lays the child out with the known
     * dimensions derived from the child's own style. An axis left {@code auto} is then sized from
     * the child's content, and that measured outer size is the child's definite final size.
     * Percentage sizes in the child's subtree can only resolve against a <em>definite</em>
     * containing block, so a second pass lays the child out again with that final size as the
     * known dimensions (both axes definite) and a definite available space. Without it a
     * {@code height: 50%} descendant of a content-sized absolutely positioned box measured 0
     * instead of 50% of the final height.
     *
     * <p>Only the out-of-flow subtree's layout cache is invalidated between the passes: the first
     * pass stored entries computed against an indefinite containing block, and
     * {@link LayoutCache} treats a request whose known size equals the cached computed size as a
     * hit, which would otherwise short-circuit the definite pass. Ancestor caches and dirty flags
     * are untouched, and when both axes of the known dimensions were already definite the first
     * pass is returned unchanged, so boxes with a fully styled size are still laid out once.
     *
     * <p>This is not full out-of-flow hoisting (P6): the box is still laid out and positioned by
     * its containing block's own pass; only the size passed to the child layout is definite.
     *
     * @param node            the absolutely positioned child
     * @param knownDimensions the known dimensions used by the first pass (an axis may be NaN)
     * @param finalSize       the child's definite final outer size derived from the first pass
     * @param parentSize      the parent size used by the first pass
     * @param availableSpace  the available space used by the first pass
     * @param sizingMode      the sizing mode used by the first pass
     * @param measuredOutput  the output of the first pass
     * @return the definite-size pass output, or {@code measuredOutput} when both axes were known
     */
    LayoutOutput performDefiniteAbsoluteChildLayout(
        NodeId node,
        FloatSize knownDimensions,
        FloatSize finalSize,
        FloatSize parentSize,
        TaffySize<AvailableSpace> availableSpace,
        SizingMode sizingMode,
        LayoutOutput measuredOutput) {

        if (!Float.isNaN(knownDimensions.width) && !Float.isNaN(knownDimensions.height)) {
            // Both axes were definite, so the first pass already was the definite-layout pass.
            return measuredOutput;
        }

        clearCacheRecursively(node);

        TaffySize<AvailableSpace> definiteAvailableSpace = new TaffySize<>(
            availableSpace.width.isDefinite() ? availableSpace.width : AvailableSpace.definite(finalSize.width),
            availableSpace.height.isDefinite() ? availableSpace.height : AvailableSpace.definite(finalSize.height)
        );

        return performChildLayout(
            node,
            finalSize,
            parentSize,
            definiteAvailableSpace,
            sizingMode,
            TaffyLine.FALSE
        );
    }

    /**
     * Clears the layout cache of a node and of every descendant.
     *
     * <p>Used by {@link #performDefiniteAbsoluteChildLayout} to force the definite-size pass to
     * recompute the whole out-of-flow subtree instead of reusing cache entries computed against an
     * indefinite containing block. Only the subtree is cleared: the ancestors' caches stay valid
     * because their inputs did not change, and their dirty flags are not touched.
     */
    private void clearCacheRecursively(NodeId node) {
        tree.clearCache(node);
        for (NodeId child : tree.getChildren(node)) {
            clearCacheRecursively(child);
        }
    }

    /**
     * Computes layout for a child, using cache if available.
     */
    public LayoutOutput computeChildLayout(NodeId node, LayoutInput inputs) {

        // Handle hidden layout
        if (inputs.runMode() == RunMode.PERFORM_HIDDEN_LAYOUT) {
            return computeHiddenLayout(node);
        }

        // Try cache first
        LayoutOutput cached = tree.getCacheEntry(node, inputs);
        if (cached != null) {
            return cached;
        }

        // Compute layout
        LayoutOutput output = computeLayoutUncached(node, inputs);

        // Store in cache
        tree.storeCacheEntry(node, inputs, output);

        return output;
    }

    /**
     * Computes layout without using cache.
     */
    private LayoutOutput computeLayoutUncached(NodeId node, LayoutInput inputs) {
        TaffyStyle style = tree.getStyle(node);
        TaffyDisplay display = style.getDisplay();
        int childCount = tree.childCount(node);

        // Dispatch based on display mode and whether node has children
        if (display == TaffyDisplay.NONE) {
            return computeHiddenLayout(node);
        }

        if (childCount == 0) {
            // Leaf node - use measure function
            return computeLeafLayout(node, inputs, style);
        }

        return switch (display) {
            case BLOCK -> computeBlockLayout(node, inputs, style);
            case FLEX -> computeFlexboxLayout(node, inputs, style);
            case GRID -> computeGridLayout(node, inputs, style);
            default -> computeLeafLayout(node, inputs, style);
        };
    }

    /**
     * Computes layout for a hidden node.
     */
    public LayoutOutput computeHiddenLayout(NodeId node) {
        tree.clearCache(node);
        tree.setUnroundedLayout(node, Layout.withOrder(0));

        // Process children as hidden
        List<NodeId> children = tree.getChildren(node);
        for (NodeId child : children) {
            computeChildLayout(child, LayoutInput.hidden());
        }

        return LayoutOutput.hidden();
    }

    /**
     * Computes layout for a leaf node (node without children or with measure function).
     */
    private LayoutOutput computeLeafLayout(NodeId node, LayoutInput inputs, TaffyStyle style) {
        FloatSize knownDimensions = inputs.knownDimensions();
        FloatSize parentSize = inputs.parentSize();
        TaffySize<AvailableSpace> availableSpace = inputs.availableSpace();

        // Resolve style sizes
        Float aspectRatio = style.getAspectRatio();
        FloatRect margin = Resolve.resolveRectLpaOrZero(style.getMargin(), parentSize.width);
        FloatRect padding = Resolve.resolveRectOrZero(style.getPadding(), parentSize.width);
        FloatRect border = Resolve.resolveRectOrZero(style.getBorder(), parentSize.width);

        // Padding + border only (without scrollbar gutter)
        FloatSize paddingBorderSize = new FloatSize(
            padding.left + padding.right + border.left + border.right,
            padding.top + padding.bottom + border.top + border.bottom
        );

        // Scrollbar gutters are reserved when overflow is scroll
        // Axes are transposed: vertical scroll needs horizontal space for scrollbar
        float scrollbarWidth = style.getScrollbarWidth();
        TaffyPoint<Overflow> overflow = style.getOverflow();
        float scrollbarGutterX = overflow.y == Overflow.SCROLL ? scrollbarWidth : 0f;
        float scrollbarGutterY = overflow.x == Overflow.SCROLL ? scrollbarWidth : 0f;

        // Content box inset = padding + border + scrollbar gutter
        FloatSize contentBoxInsetSize = new FloatSize(
            paddingBorderSize.width + scrollbarGutterX,
            paddingBorderSize.height + scrollbarGutterY
        );

        // box_sizing_adjustment only includes padding+border, NOT scrollbar gutter
        // Per Rust leaf.rs: box_sizing_adjustment = if content_box { pb_sum } else { ZERO }
        // Scrollbar gutter affects content_box_inset (internal space) but not the external size adjustment
        FloatSize boxSizingAdjustment = style.getBoxSizing() == BoxSizing.CONTENT_BOX
                                        ? paddingBorderSize
                                        : new FloatSize(0f, 0f);
        // For ContentSize mode, we ignore min-size and explicit size; max-size still clamps content contributions.
        // These should only be applied when computing inherent size (except max-size clamping).
        SizingMode sizingMode = inputs.sizingMode();

        FloatSize minSize;
        FloatSize maxSize;
        FloatSize styledBasedKnownDimensions;
        FloatSize nodeSize; // Track for canBeCollapsedThrough computation
        FloatSize nodeMinSize; // Track for canBeCollapsedThrough computation

        // Keep raw resolved min/max (before aspect-ratio expansion) so we can implement "fill_*" aspect-ratio behavior
        // when only one axis is specified.
        FloatSize resolvedMinSizeRaw;
        FloatSize resolvedMaxSizeRaw = Resolve.maybeResolveSize(style.getMaxSize(), parentSize);

        if (sizingMode == SizingMode.CONTENT_SIZE) {
            // In ContentSize mode, we ignore min-size and explicit size; callers provide knownDimensions when needed.
            // Max-size still clamps intrinsic contributions to avoid over-inflation.
            minSize = new FloatSize(Float.NaN, Float.NaN);
            maxSize = maybeAdd(resolvedMaxSizeRaw, boxSizingAdjustment);

            // Ignore style "size" here; callers using CONTENT_SIZE typically provide knownDimensions when needed.
            styledBasedKnownDimensions = knownDimensions;
            nodeSize = knownDimensions;
            nodeMinSize = new FloatSize(Float.NaN, Float.NaN);
        } else {
            resolvedMinSizeRaw = Resolve.maybeResolveSize(style.getMinSize(), parentSize);

            minSize = Resolve.maybeApplyAspectRatio(resolvedMinSizeRaw, aspectRatio);
            minSize = maybeAdd(minSize, boxSizingAdjustment);

            // max-size does NOT apply aspect-ratio in leaf layout (per Taffy behavior)
            maxSize = maybeAdd(resolvedMaxSizeRaw, boxSizingAdjustment);

            FloatSize size = Resolve.maybeResolveSize(style.getSize(), parentSize);
            FloatSize styleSize = Resolve.maybeApplyAspectRatio(size, aspectRatio);
            styleSize = maybeAdd(styleSize, boxSizingAdjustment);
            FloatSize clampedStyleSize = maybeClamp(styleSize, minSize, maxSize);

            // Merge known dimensions with style size
            styledBasedKnownDimensions = new FloatSize(
                Float.isNaN(knownDimensions.width) ? clampedStyleSize.width : knownDimensions.width,
                Float.isNaN(knownDimensions.height) ? clampedStyleSize.height : knownDimensions.height
            );
            nodeSize = new FloatSize(
                Float.isNaN(knownDimensions.width) ? styleSize.width : knownDimensions.width,
                Float.isNaN(knownDimensions.height) ? styleSize.height : knownDimensions.height
            );
            nodeMinSize = minSize;
        }

        // Check if styles prevent being collapsed through (for margin collapse)
        boolean hasStylesPreventingBeingCollapsedThrough =
            !style.isBlock() ||
            overflow.x.isScrollContainer() ||
            overflow.y.isScrollContainer() ||
            style.getPosition() == TaffyPosition.ABSOLUTE ||
            ( !Float.isNaN(style.getAspectRatio())) ||
            padding.top > 0 ||
            padding.bottom > 0 ||
            border.top > 0 ||
            border.bottom > 0 ||
            (!Float.isNaN(nodeSize.height) && nodeSize.height > 0) ||
            (!Float.isNaN(nodeMinSize.height) && nodeMinSize.height > 0);

        // If both dimensions are known, return early (but ensure size is at least padding+border, WITHOUT scrollbar)
        // Scrollbar gutter reduces content area but doesn't inflate explicit size
        if (!Float.isNaN(styledBasedKnownDimensions.width) && !Float.isNaN(styledBasedKnownDimensions.height)) {
            FloatSize size = new FloatSize(
                Math.max(styledBasedKnownDimensions.width, paddingBorderSize.width),
                Math.max(styledBasedKnownDimensions.height, paddingBorderSize.height)
            );
            // For early return, we need to compute marginsCanCollapseThrough
            // If styles already prevent collapse through, return false immediately
            // Otherwise, if size.height == 0, we need to check if node has content (e.g., text)
            // by calling the measure function
            boolean canCollapseThrough;
            if (hasStylesPreventingBeingCollapsedThrough) {
                canCollapseThrough = false;
            } else if (size.height == 0) {
                // Call measure with null height to get actual content height
                // This determines if there's a "line box" that prevents collapse through.
                // This early-return path goes through the same measure selection order as the main
                // leaf path, but only the measured content height is consumed: the returned
                // baselines/scrollable overflow are not attached to the style-sized output.
                FloatSize measureKnownDimensions = new FloatSize(
                    TaffyMath.maybeSub(styledBasedKnownDimensions.width, contentBoxInsetSize.width),
                    Float.NaN  // Pass null height to get actual content height
                );
                LayoutOutput measuredOutput = measureLeaf(
                    node, inputs, style, measureKnownDimensions, availableSpace);
                if (measuredOutput != null) {
                    float measuredHeight = Float.isNaN(measuredOutput.size().height)
                                          ? 0f
                                          : measuredOutput.size().height;
                    canCollapseThrough = measuredHeight == 0;
                } else {
                    canCollapseThrough = true;
                }
            } else {
                canCollapseThrough = false;
            }
            return new LayoutOutput(
                size,
                FloatSize.zero(),
                new FloatPoint(Float.NaN, Float.NaN),
                CollapsibleMarginSet.ZERO,
                CollapsibleMarginSet.ZERO,
                canCollapseThrough
            );
        }

        // Compute available content space following Rust's logic:
        // 1. Start with available_space
        // 2. Subtract margin (leaf's own margin reduces its available space)
        // 3. Override with known_dimensions or node_size if present
        // 4. Subtract content_box_inset for definite values
        float marginHorizontal = margin.left + margin.right;
        float marginVertical = margin.top + margin.bottom;

        TaffySize<AvailableSpace> contentAvailableSpace = new TaffySize<>(
            computeLeafAvailableSpace(
                availableSpace.width,
                knownDimensions.width,
                styledBasedKnownDimensions.width,
                marginHorizontal,
                contentBoxInsetSize.width,
                minSize.width,
                maxSize.width
            ),
            computeLeafAvailableSpace(
                availableSpace.height,
                knownDimensions.height,
                styledBasedKnownDimensions.height,
                marginVertical,
                contentBoxInsetSize.height,
                minSize.height,
                maxSize.height
            )
        );

        // Use a measure callback if available. The output size is a content size (same semantics
        // as the legacy MeasureFunc FloatSize); the padding/border/scrollbar inset is added below.
        // Subtract content box inset (incl. scrollbar) from known dimensions to get content area
        FloatSize measureKnownDimensions = new FloatSize(
            TaffyMath.maybeSub(styledBasedKnownDimensions.width, contentBoxInsetSize.width),
            TaffyMath.maybeSub(styledBasedKnownDimensions.height, contentBoxInsetSize.height)
        );
        LayoutOutput measuredOutput = measureLeaf(
            node, inputs, style, measureKnownDimensions, contentAvailableSpace);
        FloatSize measuredSize = measuredOutput != null
                                 ? measuredOutput.size()
                                 : new FloatSize(0f, 0f);

        // Compute final size
        // For measured content, add contentBoxInsetSize (incl. scrollbar) to content size
        // Follow Rust/Taffy logic: known_dimensions.or(node_size).unwrap_or(measured + inset).clamp(min, max)
        float width = Float.isNaN(styledBasedKnownDimensions.width) ?
                      (Float.isNaN(measuredSize.width) ? contentBoxInsetSize.width : measuredSize.width + contentBoxInsetSize.width) :
                      styledBasedKnownDimensions.width;
        float height = Float.isNaN(styledBasedKnownDimensions.height) ?
                       (Float.isNaN(measuredSize.height) ? contentBoxInsetSize.height : measuredSize.height + contentBoxInsetSize.height) :
                       styledBasedKnownDimensions.height;

        // Apply min/max constraints (clamp)
        width = TaffyMath.clamp(width, minSize.width, maxSize.width);
        height = TaffyMath.clamp(height, minSize.height, maxSize.height);

        // Apply aspect-ratio enforcement.
        // When one dimension is known (from knownDimensions or style) and the other is auto/measured,
        // derive the auto dimension from the known one via aspect ratio.
        if (aspectRatio != null && !Float.isNaN(aspectRatio)) {
            // Derive height from width (existing behavior)
            float aspectHeight = width / aspectRatio;
            height = Math.max(height, aspectHeight);

            // Derive width from height when height is known but width was auto/measured
            // This fixes aspect-ratio in flex row items where cross (height) is known
            if (!Float.isNaN(styledBasedKnownDimensions.height) && Float.isNaN(styledBasedKnownDimensions.width)) {
                float aspectWidth = height * aspectRatio;
                width = Math.max(width, aspectWidth);
            }
        }

        // Ensure size is at least padding + border (WITHOUT scrollbar)
        // maxSize can constrain node to be smaller than scrollbar gutter
        width = Math.max(width, paddingBorderSize.width);
        height = Math.max(height, paddingBorderSize.height);

        FloatSize size = new FloatSize(width, height);

        // Compute marginsCanCollapseThrough: true if no styles prevent it, height is 0, and measured height is 0
        float measuredHeight = Float.isNaN(measuredSize.height) ? 0f : measuredSize.height;
        boolean canCollapseThrough = !hasStylesPreventingBeingCollapsedThrough
                                     && size.height == 0
                                     && measuredHeight == 0;

        // Baselines and scrollable overflow reported by a MeasureFunction are propagated to the
        // final output (S1). A legacy MeasureFunc goes through MeasureFuncAdapter, which leaves
        // both empty, exactly like the pre-1.2 leaf path. The legacy firstBaselines field is kept
        // in sync with the new Baselines.first() value so old and new consumers observe the same
        // baseline.
        Baselines baselines = measuredOutput != null ? measuredOutput.baselines() : Baselines.NONE;
        ScrollableOverflow scrollableOverflow = measuredOutput != null
                                                ? measuredOutput.scrollableOverflow()
                                                : ScrollableOverflow.ZERO;

        return new LayoutOutput(
            size,
            new FloatSize(
                (Float.isNaN(measuredSize.width) ? 0f : measuredSize.width) + padding.left + padding.right,
                (Float.isNaN(measuredSize.height) ? 0f : measuredSize.height) + padding.top + padding.bottom
            ),
            scrollableOverflow,
            baselines.first(),
            baselines,
            CollapsibleMarginSet.ZERO,
            CollapsibleMarginSet.ZERO,
            canCollapseThrough
        );
    }

    /**
     * Invokes the measure callback that applies to a leaf node, if any.
     *
     * <p><b>Selection order (S1):</b> per-node callbacks always beat the tree-level defaults, so a
     * default measure function can never override a callback the node itself registered.</p>
     * <ol>
     *   <li>the node's new-style {@link MeasureFunction};</li>
     *   <li>the node's legacy {@link MeasureFunc}, wrapped in {@link MeasureFuncAdapter};</li>
     *   <li>the computer's default new-style {@link MeasureFunction};</li>
     *   <li>the computer's default legacy {@link MeasureFunc}, wrapped in {@link MeasureFuncAdapter}.</li>
     * </ol>
     *
     * <p>The legacy {@code MeasureFunc} and {@code defaultMeasureFunc} parameters are adapted at
     * the call site, so both kinds of callback flow through one code path. A node has at most one
     * callback configured at a time ({@code TaffyTree} enforces the mutual exclusion), but the
     * order above is still well defined if both slots were somehow populated.</p>
     *
     * <p>The {@link MeasureContext#input()} is a measure-specific derived input, not the raw
     * {@code LayoutInput} of the surrounding pass. On the main leaf path {@code knownDimensions}
     * and {@code availableSpace} are content-box values; on the early-return collapse-through probe
     * the available space is the layout input's available space and the height stays {@code NaN},
     * exactly as the pre-1.2 code called a legacy measure function. In both cases the arguments
     * forwarded by {@link MeasureFuncAdapter} are identical to the ones the legacy path used.</p>
     *
     * @param node            the leaf node being measured
     * @param inputs          the layout inputs of the surrounding layout pass
     * @param style           the resolved style of the node
     * @param knownDimensions content-box known dimensions for the callback
     * @param availableSpace  available space for the callback (see above)
     * @return the callback output, or {@code null} when no measure callback applies
     */
    private LayoutOutput measureLeaf(NodeId node, LayoutInput inputs, TaffyStyle style,
                                     FloatSize knownDimensions,
                                     TaffySize<AvailableSpace> availableSpace) {
        // Per-node callbacks always take precedence over the tree-level defaults.
        MeasureFunction measureFunction = tree.getMeasureFunction(node);
        if (measureFunction == null) {
            MeasureFunc nodeMeasureFunc = tree.getMeasureFunc(node);
            if (nodeMeasureFunc != null) {
                measureFunction = new MeasureFuncAdapter(nodeMeasureFunc);
            }
        }
        if (measureFunction == null) {
            measureFunction = defaultMeasureFunction;
        }
        if (measureFunction == null && defaultMeasureFunc != null) {
            measureFunction = new MeasureFuncAdapter(defaultMeasureFunc);
        }
        if (measureFunction == null) {
            return null;
        }

        LayoutInput measureInput = inputs
            .withKnownDimensions(knownDimensions)
            .withAvailableSpace(availableSpace);
        MeasureContext measureContext = new MeasureContext(
            measureInput,
            style,
            node,
            tree.getNodeContext(node)
        );
        return Objects.requireNonNull(
            measureFunction.measure(measureContext),
            "MeasureFunction returned null"
        );
    }

    /**
     * Computes layout for a block container.
     */
    private LayoutOutput computeBlockLayout(NodeId node, LayoutInput inputs, TaffyStyle style) {
        // Simplified block layout - full implementation in BlockComputer
        return new BlockComputer(this).compute(node, inputs, style);
    }

    /**
     * Computes layout for a flexbox container.
     */
    private LayoutOutput computeFlexboxLayout(NodeId node, LayoutInput inputs, TaffyStyle style) {
        // Simplified flexbox layout - full implementation in FlexboxComputer
        return new FlexboxComputer(this).compute(node, inputs, style);
    }

    /**
     * Computes layout for a grid container.
     */
    private LayoutOutput computeGridLayout(NodeId node, LayoutInput inputs, TaffyStyle style) {
        // Simplified grid layout - full implementation in GridComputer
        return new GridComputer(this).compute(node, inputs, style);
    }

    // === Helper methods ===

    public TaffyTree getTree() {
        return tree;
    }

    private FloatSize availableSpaceToOptionSize(TaffySize<AvailableSpace> availableSpace) {
        // For FloatSize we use NaN to represent "None". AvailableSpace.intoOption() already
        // returns NaN for MIN_CONTENT/MAX_CONTENT.
        return new FloatSize(
            availableSpace.width.isDefinite() ? availableSpace.width.getValue() : Float.NaN,
            availableSpace.height.isDefinite() ? availableSpace.height.getValue() : Float.NaN
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
        float width = Float.NaN;
        float height = Float.NaN;

        for (FloatSize size : sizes) {
            if (Float.isNaN(width) && !Float.isNaN(size.width)) {
                width = size.width;
            }
            if (Float.isNaN(height) && !Float.isNaN(size.height)) {
                height = size.height;
            }
            if (!Float.isNaN(width) && !Float.isNaN(height)) {
                break;
            }
        }

        return new FloatSize(width, height);
    }

    /**
     * Compute available space for leaf node measurement following Rust's logic:
     * 1. Start with outer available space
     * 2. Subtract margin (leaf's own margin reduces its available space)
     * 3. Override with known_dimensions or node_size if present
     * 4. Subtract content_box_inset and clamp for definite values
     */
    private AvailableSpace computeLeafAvailableSpace(
        AvailableSpace outerAvailable,
        float knownDimension,
        float nodeSize,
        float margin,
        float contentBoxInset,
        float minSize,
        float maxSize) {
        
        // Start with available space, possibly from known dimensions
        AvailableSpace result;
        if (!Float.isNaN(knownDimension)) {
            result = AvailableSpace.definite(knownDimension);
        } else {
            result = outerAvailable;
        }
        
        // Subtract margin
        result = result.maybeSub(margin);
        
        // Override with known dimension or node size if present
        if (!Float.isNaN(knownDimension)) {
            result = AvailableSpace.definite(knownDimension);
        } else if (!Float.isNaN(nodeSize)) {
            result = AvailableSpace.definite(nodeSize);
        }
        
        // For definite values, clamp and subtract content box inset
        if (result.isDefinite()) {
            float val = result.getValue();
            val = TaffyMath.clamp(val, minSize, maxSize);
            val = Math.max(0, val - contentBoxInset);
            result = AvailableSpace.definite(val);
        }
        
        return result;
    }
    
    /**
     * Resolves the direction for a node, handling INHERIT by looking up the parent chain.
     * If direction is INHERIT, it walks up the parent chain to find a non-INHERIT direction.
     * If no parent has a concrete direction, defaults to LTR.
     * 
     * @param node The node to resolve direction for
     * @return The resolved direction (LTR or RTL, never INHERIT)
     */
    public TaffyDirection resolveDirection(NodeId node) {
        TaffyStyle style = tree.getStyle(node);
        TaffyDirection direction = style.getDirection();
        
        if (!direction.isInherit()) {
            return direction;
        }
        
        // Walk up the parent chain to find a non-INHERIT direction
        NodeId parent = tree.getParent(node);
        while (parent != null) {
            TaffyStyle parentStyle = tree.getStyle(parent);
            TaffyDirection parentDirection = parentStyle.getDirection();
            if (!parentDirection.isInherit()) {
                return parentDirection;
            }
            parent = tree.getParent(parent);
        }
        
        // No parent with concrete direction found, use default
        return TaffyDirection.DEFAULT;
    }
}
