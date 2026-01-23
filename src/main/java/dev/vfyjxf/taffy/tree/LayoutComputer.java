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
import dev.vfyjxf.taffy.util.MeasureFunc;
import dev.vfyjxf.taffy.util.Resolve;
import dev.vfyjxf.taffy.util.TaffyMath;

import java.util.List;

/**
 * Computes layout for a TaffyTree.
 */
public class LayoutComputer {

    private final TaffyTree tree;
    private final MeasureFunc defaultMeasureFunc;

    public LayoutComputer(TaffyTree tree, MeasureFunc defaultMeasureFunc) {
        this.tree = tree;
        this.defaultMeasureFunc = defaultMeasureFunc;
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

        // Set the root layout
        Layout layout = new Layout(
            0,
            new FloatPoint(0f, 0f),
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
     * Performs layout for a child node.
     */
    public LayoutOutput performChildLayout(
        NodeId node,
        FloatSize knownDimensions,
        FloatSize parentSize,
        TaffySize<AvailableSpace> availableSpace,
        SizingMode sizingMode,
        TaffyLine<Boolean> verticalMarginsAreCollapsible) {

        LayoutInput inputs = new LayoutInput(
            RunMode.PERFORM_LAYOUT,
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
     * Computes layout for a child, using cache if available.
     */
    public LayoutOutput computeChildLayout(NodeId node, LayoutInput inputs) {

        // Handle hidden layout
        if (inputs.runMode() == RunMode.PERFORM_HIDDEN_LAYOUT) {
            return computeHiddenLayout(node);
        }

        // Try cache first
        LayoutOutput cached = tree.getCacheEntry(
            node,
            inputs.knownDimensions(),
            inputs.availableSpace(),
            inputs.runMode()
        );
        if (cached != null) {
            return cached;
        }

        // Compute layout
        LayoutOutput output = computeLayoutUncached(node, inputs);

        // Store in cache
        tree.storeCacheEntry(
            node,
            inputs.knownDimensions(),
            inputs.availableSpace(),
            inputs.runMode(),
            output
        );

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

        // For ContentSize mode, we ignore min/max sizes (like taffy does)
        // These should only be applied when computing inherent size
        SizingMode sizingMode = inputs.sizingMode();

        FloatSize minSize;
        FloatSize maxSize;
        FloatSize styledBasedKnownDimensions;
        FloatSize nodeSize; // Track for canBeCollapsedThrough computation
        FloatSize nodeMinSize; // Track for canBeCollapsedThrough computation

        // Keep raw resolved min/max (before aspect-ratio expansion) so we can implement "fill_*" aspect-ratio behavior
        // when only one axis is specified.
        FloatSize resolvedMinSizeRaw;
        FloatSize resolvedMaxSizeRaw;

        if (sizingMode == SizingMode.CONTENT_SIZE) {
            // In ContentSize mode, we ignore all size styles (min/max/size).
            // Callers using CONTENT_SIZE provide knownDimensions when needed.
            // Per Rust leaf.rs: node_size = known_dimensions, node_min_size = Size::NONE, node_max_size = Size::NONE
            minSize = new FloatSize(Float.NaN, Float.NaN);
            maxSize = new FloatSize(Float.NaN, Float.NaN);

            // Ignore style "size" here; callers using CONTENT_SIZE typically provide knownDimensions when needed.
            styledBasedKnownDimensions = knownDimensions;
            nodeSize = knownDimensions;
            nodeMinSize = new FloatSize(Float.NaN, Float.NaN);
        } else {
            resolvedMinSizeRaw = Resolve.maybeResolveSize(style.getMinSize(), parentSize);
            resolvedMaxSizeRaw = Resolve.maybeResolveSize(style.getMaxSize(), parentSize);

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
                // Check if node has a measure function that would return content
                MeasureFunc measureFunc = tree.getMeasureFunc(node);
                if (measureFunc == null) {
                    measureFunc = defaultMeasureFunc;
                }
                if (measureFunc != null) {
                    // Call measure with null height to get actual content height
                    // This determines if there's a "line box" that prevents collapse through
                    FloatSize measureKnownDimensions = new FloatSize(
                        TaffyMath.maybeSub(styledBasedKnownDimensions.width, contentBoxInsetSize.width),
                        Float.NaN  // Pass null height to get actual content height
                    );
                    FloatSize measuredSize = measureFunc.measure(measureKnownDimensions, availableSpace);
                    float measuredHeight = Float.isNaN(measuredSize.height) ? 0f : measuredSize.height;
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

        // Use measure function if available
        MeasureFunc measureFunc = tree.getMeasureFunc(node);
        if (measureFunc == null) {
            measureFunc = defaultMeasureFunc;
        }

        FloatSize measuredSize;
        if (measureFunc != null) {
            // Subtract content box inset (incl. scrollbar) from known dimensions to get content area
            FloatSize measureKnownDimensions = new FloatSize(
                TaffyMath.maybeSub(styledBasedKnownDimensions.width, contentBoxInsetSize.width),
                TaffyMath.maybeSub(styledBasedKnownDimensions.height, contentBoxInsetSize.height)
            );
            measuredSize = measureFunc.measure(measureKnownDimensions, contentAvailableSpace);
        } else {
            measuredSize = new FloatSize(0f, 0f);
        }

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

        // Apply aspect-ratio: height = max(clamped_height, width / aspect_ratio)
        // This matches Rust: height: f32_max(clamped_size.height, aspect_ratio.map(|ratio| clamped_size.width / ratio).unwrap_or(0.0))
        if (aspectRatio != null && !Float.isNaN(aspectRatio)) {
            float aspectHeight = width / aspectRatio;
            height = Math.max(height, aspectHeight);
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

        return new LayoutOutput(
            size,
            new FloatSize(
                (Float.isNaN(measuredSize.width) ? 0f : measuredSize.width) + padding.left + padding.right,
                (Float.isNaN(measuredSize.height) ? 0f : measuredSize.height) + padding.top + padding.bottom
            ),
            new FloatPoint(Float.NaN, Float.NaN),
            CollapsibleMarginSet.ZERO,
            CollapsibleMarginSet.ZERO,
            canCollapseThrough
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
