package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.FloatSupplier;
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
import dev.vfyjxf.taffy.style.GridAutoFlow;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.GridRepetition;
import dev.vfyjxf.taffy.style.GridTemplateComponent;
import dev.vfyjxf.taffy.style.JustifyContent;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.grid.NamedLineResolver;
import dev.vfyjxf.taffy.util.ContentSizeUtil;
import dev.vfyjxf.taffy.util.Resolve;
import dev.vfyjxf.taffy.util.TaffyMath;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Float.NaN;

/**
 * Computes grid layout for nodes with display: grid.
 * <p>
 * This is a simplified implementation of CSS Grid layout.
 * Full grid layout is complex and involves:
 * - Grid template definitions (rows/columns)
 * - Grid auto-flow
 * - Grid placement
 * - Track sizing algorithm
 * - Alignment
 */
public class GridComputer {

    private final LayoutComputer layoutComputer;

    public GridComputer(LayoutComputer layoutComputer) {
        this.layoutComputer = layoutComputer;
    }

    /**
     * Internal data structure for grid items.
     * Grid placement uses OriginZero coordinates where:
     * - null means "auto" (needs auto-placement)
     * - 0 is the start of the explicit grid
     * - Positive values are lines after the explicit grid start
     * - Negative values are lines before the explicit grid start (in negative implicit grid)
     */
    private static class GridItem {
        NodeId nodeId;
        int order;
        FloatSize size;
        FloatSize minSize;
        FloatSize maxSize;
        TaffySize<TaffyDimension> rawSize;      // Original unresolved size (for re-resolving with new grid area)
        TaffySize<TaffyDimension> rawMinSize;   // Original unresolved min-size
        TaffySize<TaffyDimension> rawMaxSize;   // Original unresolved max-size
        BoxSizing boxSizing;          // Box sizing mode for padding/border adjustment
        TaffyPosition position;
        TaffyRect<LengthPercentageAuto> inset;
        TaffyRect<LengthPercentageAuto> rawMargin;  // Original margin style (for special percentage resolution)
        FloatRect margin;
        FloatRect padding;
        FloatRect border;
        TaffyPoint<Overflow> overflow;
        float scrollbarWidth;
        Float aspectRatio;

        // Is this a "compressible replaced element"? (CSS Sizing / Grid min-size-auto behavior)
        // https://drafts.csswg.org/css-sizing-3/#min-content-zero
        boolean isCompressibleReplaced;

        // Grid placement (OriginZero coordinates, null means auto)
        Integer columnStart;
        Integer columnEnd;    // exclusive end index
        Integer rowStart;
        Integer rowEnd;       // exclusive end index
        int columnSpan;   // number of columns to span
        int rowSpan;      // number of rows to span

        // Alignment
        AlignItems alignSelf;
        AlignItems justifySelf;

        // Baseline alignment
        float baseline = NaN;       // The item's first baseline (horizontal)
        float baselineShim;   // Shim for baseline alignment that acts like an extra top margin

        // Computed values
        FloatSize computedSize;
        FloatPoint location;

        // Track crossing flags (for re-run logic)
        boolean crossesIntrinsicColumn;
        boolean crossesFlexibleColumn;
        boolean crossesIntrinsicRow;
        boolean crossesFlexibleRow;

        // Caches for intrinsic size computation. These caches are only valid for a single run of the track-sizing algorithm.
        // Cache for the available_space input to intrinsic sizing computation (for cache invalidation)
        FloatSize availableSpaceCache;
        // Cache for the min-content contribution
        Float minContentContributionWidth;
        Float minContentContributionHeight;
        // Cache for the max-content contribution
        Float maxContentContributionWidth;
        Float maxContentContributionHeight;
        // Cache for the minimum contribution
        Float minimumContributionWidth;
        Float minimumContributionHeight;

        /**
         * Compute the item's resolved margins for size contributions.
         * Horizontal percentage margins always resolve to zero if the container size is indefinite
         * as otherwise this would introduce a cyclic dependency.
         *
         * @param innerNodeWidth The inner width of the node, used for vertical percentage margins
         * @return Size with width being sum of horizontal margins, height being sum of vertical margins
         */
        FloatSize getMarginAxisSumsWithBaselineShims(float innerNodeWidth) {
            // Horizontal percentage margins resolve against 0 (not the container width)
            // This is per CSS Grid spec to avoid cyclic dependencies
            float left = Resolve.resolveLpaOrZero(rawMargin.left, 0f);
            float right = Resolve.resolveLpaOrZero(rawMargin.right, 0f);
            // Vertical percentage margins resolve against inner_node_width
            float top = Resolve.resolveLpaOrZero(rawMargin.top, innerNodeWidth) + baselineShim;
            float bottom = Resolve.resolveLpaOrZero(rawMargin.bottom, innerNodeWidth);
            return new FloatSize(left + right, top + bottom);
        }

        /**
         * Get min content contribution for width, using cache if available.
         */
        float getMinContentContributionWidthCached(
            LayoutComputer layoutComputer,
            FloatSize availableSpace,
            FloatSize innerNodeSize) {
            if (minContentContributionWidth != null) {
                return minContentContributionWidth;
            }
            float contribution = computeMinContentContributionWidth(layoutComputer, availableSpace, innerNodeSize);
            minContentContributionWidth = contribution;
            return contribution;
        }

        /**
         * Get max content contribution for width, using cache if available.
         */
        float getMaxContentContributionWidthCached(
            LayoutComputer layoutComputer,
            FloatSize availableSpace,
            FloatSize innerNodeSize) {
            if (maxContentContributionWidth != null) {
                return maxContentContributionWidth;
            }
            float contribution = computeMaxContentContributionWidth(layoutComputer, availableSpace, innerNodeSize);
            maxContentContributionWidth = contribution;
            return contribution;
        }

        /**
         * Get max content contribution for height, using cache if available.
         */
        float getMaxContentContributionHeightCached(
            LayoutComputer layoutComputer,
            FloatSize availableSpace,
            FloatSize innerNodeSize) {
            if (maxContentContributionHeight != null) {
                return maxContentContributionHeight;
            }
            float contribution = computeMaxContentContributionHeight(layoutComputer, availableSpace, innerNodeSize);
            maxContentContributionHeight = contribution;
            return contribution;
        }

        /**
         * Compute min content contribution for width axis.
         * Uses measureChildSize for efficient sizing without full layout.
         * <p>
         * This method returns the contribution INCLUDING margin, matching the
         * original behavior expected by callers in track sizing.
         */
        private float computeMinContentContributionWidth(
            LayoutComputer layoutComputer,
            FloatSize availableSpace,
            FloatSize innerNodeSize) {
            FloatSize marginAxisSums = getMarginAxisSumsWithBaselineShims(innerNodeSize.width);

            // If explicit width is set, use it
            if (!Float.isNaN(size.width)) {
                return size.width + marginAxisSums.width;
            }

            // Compute known dimensions with stretch alignment and aspect ratio
            FloatSize knownDimensions = computeKnownDimensions(availableSpace, innerNodeSize);

            // Build available space for measurement - match Rust's available_space.map() logic
            // Both axes: Some(size) -> Definite(size), None -> MinContent
            AvailableSpace widthAvail = !Float.isNaN(availableSpace.width)
                                        ? AvailableSpace.definite(availableSpace.width)
                                        : AvailableSpace.minContent();
            AvailableSpace heightAvail = !Float.isNaN(availableSpace.height)
                                         ? AvailableSpace.definite(availableSpace.height)
                                         : AvailableSpace.minContent();

            // Use measureChildSize for efficient sizing (returns just size, no full layout)
            FloatSize measuredSize = layoutComputer.measureChildSize(
                nodeId,
                knownDimensions,
                innerNodeSize,
                new TaffySize<>(widthAvail, heightAvail),
                SizingMode.INHERENT_SIZE,
                TaffyLine.FALSE
            );

            return measuredSize.width + marginAxisSums.width;
        }

        /**
         * Compute max content contribution for width axis.
         * <p>
         * This method returns the contribution INCLUDING margin, matching the
         * original behavior expected by callers in track sizing.
         */
        private float computeMaxContentContributionWidth(
            LayoutComputer layoutComputer,
            FloatSize availableSpace,
            FloatSize innerNodeSize) {
            FloatSize marginAxisSums = getMarginAxisSumsWithBaselineShims(innerNodeSize.width);

            // If explicit width is set, use it
            if (!Float.isNaN(size.width)) {
                return size.width + marginAxisSums.width;
            }

            FloatSize knownDimensions = computeKnownDimensions(availableSpace, innerNodeSize);

            // Build available space for measurement - match Rust's available_space.map() logic
            // Both axes: Some(size) -> Definite(size), None -> MaxContent
            AvailableSpace widthAvail = !Float.isNaN(availableSpace.width)
                                        ? AvailableSpace.definite(availableSpace.width)
                                        : AvailableSpace.maxContent();
            AvailableSpace heightAvail = !Float.isNaN(availableSpace.height)
                                         ? AvailableSpace.definite(availableSpace.height)
                                         : AvailableSpace.maxContent();

            FloatSize measuredSize = layoutComputer.measureChildSize(
                nodeId,
                knownDimensions,
                innerNodeSize,
                new TaffySize<>(widthAvail, heightAvail),
                SizingMode.INHERENT_SIZE,
                TaffyLine.FALSE
            );

            return measuredSize.width + marginAxisSums.width;
        }

        /**
         * Compute max content contribution for height axis.
         * <p>
         * This method returns the contribution INCLUDING margin, matching the
         * original behavior expected by callers in track sizing.
         */
        private float computeMaxContentContributionHeight(
            LayoutComputer layoutComputer,
            FloatSize availableSpace,
            FloatSize innerNodeSize) {
            FloatSize marginAxisSums = getMarginAxisSumsWithBaselineShims(innerNodeSize.width);

            // If explicit height is set, use it
            if (!Float.isNaN(size.height)) {
                return size.height + marginAxisSums.height;
            }

            FloatSize knownDimensions = computeKnownDimensions(availableSpace, innerNodeSize);

            // Build available space for measurement - match Rust's available_space.map() logic
            // Both axes: Some(size) -> Definite(size), None -> MaxContent
            AvailableSpace widthAvail = !Float.isNaN(availableSpace.width)
                                        ? AvailableSpace.definite(availableSpace.width)
                                        : AvailableSpace.maxContent();
            AvailableSpace heightAvail = !Float.isNaN(availableSpace.height)
                                         ? AvailableSpace.definite(availableSpace.height)
                                         : AvailableSpace.maxContent();

            FloatSize measuredSize = layoutComputer.measureChildSize(
                nodeId,
                knownDimensions,
                innerNodeSize,
                new TaffySize<>(widthAvail, heightAvail),
                SizingMode.INHERENT_SIZE,
                TaffyLine.FALSE
            );

            return measuredSize.height + marginAxisSums.height;
        }

        /**
         * Compute known dimensions for sizing, applying stretch alignment and aspect ratio.
         * This is similar to Rust's GridItem::known_dimensions method.
         */
        private FloatSize computeKnownDimensions(FloatSize gridAreaSize, FloatSize innerNodeSize) {
            FloatSize margins = getMarginAxisSumsWithBaselineShims(innerNodeSize.width);

            // Note: item.size, item.minSize, item.maxSize already include boxSizingAdj from generateGridItems.
            // So we should NOT add boxSizingAdj again here.
            // Just use them directly with aspect ratio applied.
            FloatSize inherentSize = maybeApplyAspectRatio(size, aspectRatio);
            // DO NOT add boxSizingAdj - it's already included in `size`!

            FloatSize minSizeResolved = maybeApplyAspectRatio(minSize, aspectRatio);
            // DO NOT add boxSizingAdj - it's already included in `minSize`!

            FloatSize maxSizeResolved = maybeApplyAspectRatio(maxSize, aspectRatio);
            // DO NOT add boxSizingAdj - it's already included in `maxSize`!

            // Grid area minus margins
            FloatSize gridAreaMinusMargins = new FloatSize(
                !Float.isNaN(gridAreaSize.width) ? gridAreaSize.width - margins.width : NaN,
                !Float.isNaN(gridAreaSize.height) ? gridAreaSize.height - margins.height : NaN
            );

            // Apply stretch alignment for width
            float width = inherentSize.width;
            if (Float.isNaN(width)) {
                // Check if we should stretch
                boolean leftMarginAuto = rawMargin != null && rawMargin.left != null && rawMargin.left.isAuto();
                boolean rightMarginAuto = rawMargin != null && rawMargin.right != null && rawMargin.right.isAuto();
                if (!leftMarginAuto && !rightMarginAuto && justifySelf == AlignItems.STRETCH) {
                    width = gridAreaMinusMargins.width;
                }
            }

            // Reapply aspect ratio after width adjustment
            FloatSize sizeWithWidth = maybeApplyAspectRatio(new FloatSize(width, inherentSize.height), aspectRatio);
            width = sizeWithWidth.width;
            float height = sizeWithWidth.height;

            // Apply stretch alignment for height
            // Per CSS Grid spec: If item has aspectRatio and no explicit height, 
            // vertical alignment defaults to START (not STRETCH).
            boolean shouldStretchHeight = (alignSelf == AlignItems.STRETCH);
            if (aspectRatio != null && !Float.isNaN(aspectRatio) && Float.isNaN(inherentSize.height)) {
                // When aspectRatio is set and height is not explicit, default is START not STRETCH
                shouldStretchHeight = false;
            }

            if (Float.isNaN(height) && shouldStretchHeight) {
                boolean topMarginAuto = rawMargin != null && rawMargin.top != null && rawMargin.top.isAuto();
                boolean bottomMarginAuto = rawMargin != null && rawMargin.bottom != null && rawMargin.bottom.isAuto();
                if (!topMarginAuto && !bottomMarginAuto) {
                    height = gridAreaMinusMargins.height;
                }
            }

            // Reapply aspect ratio after height adjustment
            FloatSize finalSize = maybeApplyAspectRatio(new FloatSize(width, height), aspectRatio);

            // Clamp by min/max
            return maybeClamp(finalSize, minSizeResolved, maxSizeResolved);
        }

    }

    private static float capCompressibleReplacedMinimumContributionWidth(GridItem item, float currentWithMargin, float marginWidth) {
        if (item == null || !item.isCompressibleReplaced) return currentWithMargin;

        // Border+padding minimum (outer size can't be smaller than this)
        float paddingBorderWidth = (item.padding != null ? item.padding.left + item.padding.right : 0f)
                                   + (item.border != null ? item.border.left + item.border.right : 0f);

        float preferred = resolveReplacedCapValueWidth(item.rawSize, item.boxSizing, paddingBorderWidth);
        float max = resolveReplacedCapValueWidth(item.rawMaxSize, item.boxSizing, paddingBorderWidth);

        float capped = currentWithMargin;
        if (!Float.isNaN(preferred)) capped = Math.min(capped, preferred + marginWidth);
        if (!Float.isNaN(max)) capped = Math.min(capped, max + marginWidth);

        // Keep consistent with min padding/border invariants
        float minPossible = paddingBorderWidth + marginWidth;
        if (capped < minPossible) capped = minPossible;
        return capped;
    }

    private static float resolveReplacedCapValueWidth(TaffySize<TaffyDimension> rawSize, BoxSizing boxSizing, float paddingBorderWidth) {
        if (rawSize == null || rawSize.width == null) return NaN;
        // Resolve against 0 so that indefinite percentages are treated as definite 0 for capping purposes.
        float v = rawSize.width.maybeResolve(0f);
        if (Float.isNaN(v)) return NaN;
        // If size applies to content-box, convert to border-box by adding padding+border.
        if (boxSizing == BoxSizing.CONTENT_BOX) {
            v += paddingBorderWidth;
        }
        // Ensure not smaller than padding+border.
        return Math.max(v, paddingBorderWidth);
    }

    /**
     * Computes grid layout for a node.
     */
    public LayoutOutput compute(NodeId node, LayoutInput inputs, TaffyStyle style) {
        TaffyTree tree = layoutComputer.getTree();
        FloatSize knownDimensions = inputs.knownDimensions();
        FloatSize parentSize = inputs.parentSize();
        TaffySize<AvailableSpace> availableSpace = inputs.availableSpace();
        RunMode runMode = inputs.runMode();

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
        FloatSize clampedStyleSize = inputs.sizingMode() == SizingMode.INHERENT_SIZE
                                     ? maybeClamp(maybeAdd(maybeApplyAspectRatio(
                Resolve.maybeResolveSize(style.getSize(), parentSize), aspectRatio), boxSizingAdjustment),
            minSize, maxSize)
                                     : new FloatSize(NaN, NaN);

        // Compute styled based known dimensions
        FloatSize minMaxDefiniteSize = new FloatSize(
            (!Float.isNaN(minSize.width) && !Float.isNaN(maxSize.width) && maxSize.width <= minSize.width)
            ? minSize.width : NaN,
            (!Float.isNaN(minSize.height) && !Float.isNaN(maxSize.height) && maxSize.height <= minSize.height)
            ? minSize.height : NaN
        );

        FloatSize styledBasedKnownDimensions = orChain(knownDimensions, minMaxDefiniteSize, clampedStyleSize);
        styledBasedKnownDimensions = maybeMax(styledBasedKnownDimensions, paddingBorderSize);

        // Short-circuit if size is known
        if (runMode == RunMode.COMPUTE_SIZE &&
            !Float.isNaN(styledBasedKnownDimensions.width) &&
            !Float.isNaN(styledBasedKnownDimensions.height)) {
            return LayoutOutput.fromOuterSize(styledBasedKnownDimensions);
        }

        // Scrollbar gutters are reserved when the `overflow` property is set to `Overflow::Scroll`.
        // However, the axes are switched (transposed) because a node that scrolls vertically needs
        // *horizontal* space to be reserved for a scrollbar
        TaffyPoint<Overflow> overflow = style.getOverflow();
        float scrollbarWidth = style.getScrollbarWidth();
        float scrollbarGutterX = overflow.y == Overflow.SCROLL ? scrollbarWidth : 0f;  // vertical scroll needs horizontal space
        float scrollbarGutterY = overflow.x == Overflow.SCROLL ? scrollbarWidth : 0f;  // horizontal scroll needs vertical space

        // Content box inset (padding + border + scrollbar gutter)
        FloatRect contentBoxInset = new FloatRect(
            padding.left + border.left,
            padding.right + border.right + scrollbarGutterX,
            padding.top + border.top,
            padding.bottom + border.bottom + scrollbarGutterY
        );
        FloatSize nodeInnerSize = new FloatSize(
            TaffyMath.maybeSub(styledBasedKnownDimensions.width, contentBoxInset.left + contentBoxInset.right),
            TaffyMath.maybeSub(styledBasedKnownDimensions.height, contentBoxInset.top + contentBoxInset.bottom)
        );

        // Compute available grid space for track sizing
        // This is the space available to the grid when the container size is not yet known
        // Unlike nodeInnerSize (which is definite only when style-based size is set),
        // availableGridSpace can be definite even when the container is auto-sized
        // but has a definite available space from its parent.
        // 
        // Rust: constrained_available_space = known_dimensions
        //         .or(preferred_size)
        //         .map(|size| size.map(AvailableSpace::Definite))
        //         .unwrap_or(available_space)
        //         .maybe_clamp(min_size, max_size)
        //         .maybe_max(padding_border_size);
        float contentBoxInsetWidth = contentBoxInset.left + contentBoxInset.right;
        float contentBoxInsetHeight = contentBoxInset.top + contentBoxInset.bottom;

        // First, compute constrained available space (before subtracting content box inset)
        // This clamps the available space by min/max size constraints
        TaffySize<AvailableSpace> constrainedAvailableSpace;
        if (!Float.isNaN(styledBasedKnownDimensions.width) && !Float.isNaN(styledBasedKnownDimensions.height)) {
            // Both dimensions known - use them directly
            constrainedAvailableSpace = new TaffySize<>(
                AvailableSpace.definite(styledBasedKnownDimensions.width),
                AvailableSpace.definite(styledBasedKnownDimensions.height)
            );
        } else {
            // Use available space from parent, then clamp by min/max size
            AvailableSpace constrainedWidth = !Float.isNaN(styledBasedKnownDimensions.width)
                                              ? AvailableSpace.definite(styledBasedKnownDimensions.width)
                                              : clampAvailableSpace(availableSpace.width, minSize.width, maxSize.width, paddingBorderSize.width);
            AvailableSpace constrainedHeight = !Float.isNaN(styledBasedKnownDimensions.height)
                                               ? AvailableSpace.definite(styledBasedKnownDimensions.height)
                                               : clampAvailableSpace(availableSpace.height, minSize.height, maxSize.height, paddingBorderSize.height);
            constrainedAvailableSpace = new TaffySize<>(constrainedWidth, constrainedHeight);
        }

        // Then compute available grid space by subtracting content box inset
        TaffySize<AvailableSpace> availableGridSpace = new TaffySize<>(
            constrainedAvailableSpace.width.mapDefiniteValue(v -> v - contentBoxInsetWidth),
            constrainedAvailableSpace.height.mapDefiniteValue(v -> v - contentBoxInsetHeight)
        );

        // Gap
        FloatSize gap = new FloatSize(
            style.getGap().width.resolveOrZero(nodeInnerSize.width),
            style.getGap().height.resolveOrZero(nodeInnerSize.height)
        );

        // Expand auto-fill/auto-fit in grid templates
        List<TrackSizingFunction> expandedColumns = getExpandedTemplateColumns(style, nodeInnerSize.width, gap.width);
        List<TrackSizingFunction> expandedRows = getExpandedTemplateRows(style, nodeInnerSize.height, gap.height);

        // Generate grid items
        List<GridItem> items = generateGridItems(node, style, nodeInnerSize);

        // Determine grid dimensions based on template or content
        TrackCounts colCounts = computeColumnCounts(items, expandedColumns);
        TrackCounts rowCounts = computeRowCounts(items, expandedRows);

        // Auto-place items that don't have explicit positions
        // Note: This may expand colCounts and rowCounts as needed
        autoPlaceItems(items, style.getGridAutoFlow(), colCounts, rowCounts);

        // Get the final track counts after auto-placement (may have been expanded)
        int numColumns = colCounts.len();
        int numRows = rowCounts.len();

        // Determine if items cross flexible or intrinsic tracks (for re-run optimization)
        determineIfItemCrossesFlexibleOrIntrinsicTracks(
            items, expandedColumns, style.getGridAutoColumns(),
            expandedRows, style.getGridAutoRows(),
            numColumns, numRows, colCounts, rowCounts);

        // Calculate track sizes even if no items - based on template
        FloatList columnSizes = calculateColumnSizes(style, nodeInnerSize, availableGridSpace, availableSpace, gap, numColumns, numRows, items, colCounts, expandedColumns);

        // CSS Grid: Collapse empty auto-fit columns
        // Per CSS Grid spec, auto-fit tracks that don't contain any items should be collapsed (size = 0)
        collapseEmptyAutoFitColumns(columnSizes, style, items, nodeInnerSize, gap.width, colCounts);

        // Per CSS Grid spec 11.8: Update inner_node_size.width if it was indefinite
        // This is needed for resolving percentage values in row sizing and for re-running column sizing
        float initialColumnSum = 0f;
        for (float colSize : columnSizes) {
            initialColumnSum += colSize;
        }
        // Also exclude gaps adjacent to collapsed (0-size) tracks
        int nonCollapsedColumns = 0;
        for (float colSize : columnSizes) {
            if (colSize > 0) {
                nonCollapsedColumns++;
            }
        }
        initialColumnSum += gap.width * Math.max(0, nonCollapsedColumns - 1);

        // Store whether parent was indefinite before updating
        boolean parentWidthIndefinite = !availableGridSpace.width.isDefinite();

        // If inner node width was indefinite, update it with the column sum
        if (Float.isNaN(nodeInnerSize.width)) {
            nodeInnerSize = new FloatSize(initialColumnSum, nodeInnerSize.height);

            // Per CSS Grid spec step 7: Resolve percentage track base sizes
            // In the case of an indefinitely sized container, percentage gap resolves to zero during initial sizing
            // and needs to be re-resolved based on the content-sized content box of the container
            // Re-compute gap if it uses percentages (for use in positioning, NOT for container sizing)
            if (style.getGap().width.isPercent()) {
                float newGapWidth = style.getGap().width.resolveOrZero(nodeInnerSize.width);
                gap = new FloatSize(newGapWidth, gap.height);
                // Note: initialColumnSum is NOT recalculated - container size uses the initial value
                // The new gap is used for child positioning
            }
        }

        // CSS Grid spec Step 7: Re-resolve percentage column base sizes
        // Percentage tracks that were treated as auto during initial sizing need to be clamped to their resolved sizes
        // This is CRITICAL for minmax(auto, percent) where the track was sized to content, but needs to be clamped to percent
        if (parentWidthIndefinite) {
            // Resolve percentage column sizing functions against the now-known container width
            for (int i = 0; i < columnSizes.size() && i < expandedColumns.size(); i++) {
                TrackSizingFunction track = expandedColumns.get(i);
                if (track != null && track.isMinmax()) {
                    // Get min and max sizing functions
                    TrackSizingFunction minF = track.getMinFunc();
                    TrackSizingFunction maxF = track.getMaxFunc();

                    // Resolve percentage values from min/max
                    float resolvedMin = NaN;
                    float resolvedMax = NaN;

                    if (minF != null && minF.isFixed() && minF.getFixedValue() != null && minF.getFixedValue().isPercent()) {
                        resolvedMin = minF.getFixedValue().resolveOrZero(nodeInnerSize.width);
                    }
                    if (maxF != null && maxF.isFixed() && maxF.getFixedValue() != null && maxF.getFixedValue().isPercent()) {
                        resolvedMax = maxF.getFixedValue().resolveOrZero(nodeInnerSize.width);
                    }

                    // Clamp base_size to [min, max] as in Rust's maybe_clamp
                    if (!Float.isNaN(resolvedMin) || !Float.isNaN(resolvedMax)) {
                        float newSize = columnSizes.getFloat(i);
                        if (!Float.isNaN(resolvedMin)) {
                            newSize = Math.max(newSize, resolvedMin);
                        }
                        if (!Float.isNaN(resolvedMax)) {
                            newSize = Math.min(newSize, resolvedMax);
                        }
                        columnSizes.set(i, newSize);
                    }
                } else if (track != null && track.isFixed() && track.getFixedValue() != null && track.getFixedValue().isPercent()) {
                    // Simple fixed percentage track
                    float resolvedSize = track.getFixedValue().resolveOrZero(nodeInnerSize.width);
                    float currentSize = columnSizes.getFloat(i);
                    // Clamp to the resolved percentage value
                    float newSize = Math.max(resolvedSize, Math.min(currentSize, resolvedSize));
                    columnSizes.set(i, newSize);
                }
            }
        }

        // Clear item available_space_cache after initial column sizing (Rust: items.iter_mut().for_each(|item| item.available_space_cache = None))
        for (GridItem item : items) {
            item.availableSpaceCache = null;
        }

        // Resolve baseline-aligned item baselines before row sizing (CSS Grid 11.5.1)
        resolveItemBaselines(items, nodeInnerSize);

        FloatList rowSizes = calculateRowSizes(style, nodeInnerSize, availableGridSpace, gap, numRows, items, columnSizes, colCounts, rowCounts, expandedRows);

        // Calculate initial row sum (like Rust's initial_row_sum)
        float initialRowSum = 0f;
        for (float rowSize : rowSizes) {
            initialRowSum += rowSize;
        }
        initialRowSum += gap.height * Math.max(0, rowSizes.size() - 1);

        // Per CSS Grid spec 11.8: Update inner_node_size.height if it was indefinite
        boolean parentHeightIndefinite = !availableGridSpace.height.isDefinite();
        if (Float.isNaN(nodeInnerSize.height)) {
            nodeInnerSize = new FloatSize(nodeInnerSize.width, initialRowSum);

            // Per CSS Grid spec step 7: Resolve percentage row base sizes and gap
            // In the case of an indefinitely sized container, percentage gap resolves to zero during initial sizing
            if (style.getGap().height.isPercent()) {
                float newGapHeight = style.getGap().height.resolveOrZero(nodeInnerSize.height);
                gap = new FloatSize(gap.width, newGapHeight);
            }
        }

        // CSS Grid spec step 7: Re-resolve percentage row base sizes
        // Percentage tracks that were treated as auto during initial sizing need to be clamped to their resolved sizes
        if (parentHeightIndefinite) {
            // Check if any row has percentage sizing that needs re-resolution
            for (int i = 0; i < rowSizes.size() && i < expandedRows.size(); i++) {
                TrackSizingFunction track = expandedRows.get(i);
                if (track != null && track.isFixed() && track.getFixedValue() != null && track.getFixedValue().isPercent()) {
                    float resolvedMin = track.getFixedValue().resolveOrZero(nodeInnerSize.height);
                    float resolvedMax = track.getFixedValue().resolveOrZero(nodeInnerSize.height);
                    float currentSize = rowSizes.getFloat(i);
                    // Clamp base_size to [min, max] as in Rust's maybe_clamp
                    float newSize = Math.max(resolvedMin, Math.min(currentSize, resolvedMax));
                    rowSizes.set(i, newSize);
                }
            }
        }

        // === Rust-style Re-run Logic (CSS Grid spec 11.8) ===
        // Column sizing must be re-run (once) if:
        //   - The grid container's width was initially indefinite and there are any columns with percentage track sizing functions
        //   - Any grid item crossing an intrinsically sized track's min content contribution width has changed

        boolean rerunColumnSizing;
        boolean hasPercentageColumn = expandedColumns.stream().anyMatch(TrackSizingFunction::usesPercentage);
        boolean hasPercentageGutter = style.getGap().width.isPercent();
        rerunColumnSizing = parentWidthIndefinite && (hasPercentageColumn || hasPercentageGutter);

        final FloatSize finalNodeInnerSize = nodeInnerSize;
        final FloatList finalRowSizes = rowSizes;
        final FloatSize finalGap = gap;

        if (!rerunColumnSizing) {
            // Check if any item crossing an intrinsic column has changed its min_content_contribution
            // This mirrors Rust's approach: compute new contribution and compare with cached value
            // CRITICAL: Only trigger re-run if the cached value EXISTS and has changed.
            // If cache is empty, the item wasn't used in initial track sizing, so skip it.
            boolean minContentContributionChanged = false;
            for (GridItem item : items) {
                if (!item.crossesIntrinsicColumn) continue;

                // If no cached contribution from initial track sizing, skip this item
                // This is critical for performance - avoids triggering recursive measureChildSize
                if (item.minContentContributionWidth == null) {
                    continue;
                }

                // Compute available_space based on known row sizes (like Rust's item.available_space())
                float itemHeight = computeItemAvailableHeight(item, finalRowSizes, finalGap);
                FloatSize itemAvailSpace = new FloatSize(NaN, itemHeight);

                // Check if available space actually changed
                boolean availSpaceChanged = item.availableSpaceCache == null ||
                                            !floatEquals(item.availableSpaceCache.height, itemHeight);

                if (!availSpaceChanged) {
                    // Available space didn't change, contribution won't change either
                    continue;
                }

                // Compute new min_content_contribution (non-cached version)
                FloatSize marginAxisSums = item.getMarginAxisSumsWithBaselineShims(finalNodeInnerSize.width);
                float newMinContentContribution;
                if (!Float.isNaN(item.size.width)) {
                    newMinContentContribution = item.size.width + marginAxisSums.width;
                } else {
                    FloatSize itemKnownDims = item.computeKnownDimensions(itemAvailSpace, finalNodeInnerSize);
                    FloatSize measuredSize = layoutComputer.measureChildSize(
                        item.nodeId,
                        itemKnownDims,
                        finalNodeInnerSize,
                        new TaffySize<>(AvailableSpace.minContent(),
                            !Float.isNaN(itemHeight) ? AvailableSpace.definite(itemHeight) : AvailableSpace.minContent()),
                        SizingMode.INHERENT_SIZE,
                        TaffyLine.FALSE
                    );
                    newMinContentContribution = measuredSize.width + marginAxisSums.width;
                }

                // Compare with cached value
                boolean hasChanged = Math.abs(newMinContentContribution - item.minContentContributionWidth) > 0.001f;

                // Update cache
                item.availableSpaceCache = itemAvailSpace;
                item.minContentContributionWidth = newMinContentContribution;
                item.maxContentContributionWidth = null;
                item.minimumContributionWidth = null;

                if (hasChanged) {
                    minContentContributionChanged = true;
                }
            }
            rerunColumnSizing = minContentContributionChanged;
        } else {
            // Clear intrinsic width caches since we're definitely re-running due to percentage tracks
            for (GridItem item : items) {
                item.availableSpaceCache = null;
                item.minContentContributionWidth = null;
                item.maxContentContributionWidth = null;
                item.minimumContributionWidth = null;
            }
        }

        if (rerunColumnSizing) {
            // Re-run column sizing with known row sizes
            columnSizes = calculateColumnSizesWithRowSizes(style, nodeInnerSize, availableGridSpace, availableSpace, gap,
                numColumns, numRows, items, colCounts, expandedColumns, finalRowSizes);
            // Re-apply auto-fit collapse after re-run
            collapseEmptyAutoFitColumns(columnSizes, style, items, nodeInnerSize, gap.width, colCounts);

            // Row sizing must be re-run (once) if:
            //   - The grid container's height was initially indefinite and there are any rows with percentage track sizing functions
            //   - Any grid item crossing an intrinsically sized track's min content contribution height has changed

            boolean rerunRowSizing;
            boolean hasPercentageRow = expandedRows.stream().anyMatch(TrackSizingFunction::usesPercentage);
            boolean hasPercentageRowGutter = style.getGap().height.isPercent();
            rerunRowSizing = parentHeightIndefinite && (hasPercentageRow || hasPercentageRowGutter);

            final FloatList finalColumnSizes = columnSizes;

            if (!rerunRowSizing) {
                // Check if any item crossing an intrinsic row has changed its min_content_contribution
                // CRITICAL: Only trigger re-run if cached value EXISTS and has changed
                boolean minContentContributionChanged = false;
                for (GridItem item : items) {
                    if (!item.crossesIntrinsicRow) continue;

                    // If no cached contribution from initial track sizing, skip this item
                    if (item.minContentContributionHeight == null) {
                        continue;
                    }

                    // Compute available_space based on known column sizes
                    float itemWidth = computeItemAvailableWidth(item, finalColumnSizes, finalGap);
                    FloatSize itemAvailSpace = new FloatSize(itemWidth, NaN);

                    // Check if available space actually changed
                    boolean availSpaceChanged = item.availableSpaceCache == null ||
                                                !floatEquals(item.availableSpaceCache.width, itemWidth);

                    if (!availSpaceChanged) {
                        continue;
                    }

                    // Compute new min_content_contribution for height
                    FloatSize marginAxisSums = item.getMarginAxisSumsWithBaselineShims(finalNodeInnerSize.width);
                    float newMinContentContribution;
                    if (!Float.isNaN(item.size.height)) {
                        newMinContentContribution = item.size.height + marginAxisSums.height;
                    } else {
                        FloatSize itemKnownDims = item.computeKnownDimensions(itemAvailSpace, finalNodeInnerSize);
                        FloatSize measuredSize = layoutComputer.measureChildSize(
                            item.nodeId,
                            itemKnownDims,
                            finalNodeInnerSize,
                            new TaffySize<>(!Float.isNaN(itemWidth) ? AvailableSpace.definite(itemWidth) : AvailableSpace.minContent(),
                                AvailableSpace.minContent()),
                            SizingMode.INHERENT_SIZE,
                            TaffyLine.FALSE
                        );
                        newMinContentContribution = measuredSize.height + marginAxisSums.height;
                    }

                    // Compare with cached value
                    boolean hasChanged = Math.abs(newMinContentContribution - item.minContentContributionHeight) > 0.001f;

                    // Update cache
                    item.availableSpaceCache = itemAvailSpace;
                    item.minContentContributionHeight = newMinContentContribution;
                    item.maxContentContributionHeight = null;
                    item.minimumContributionHeight = null;

                    if (hasChanged) {
                        minContentContributionChanged = true;
                    }
                }
                rerunRowSizing = minContentContributionChanged;
            } else {
                // Clear intrinsic height caches
                for (GridItem item : items) {
                    item.availableSpaceCache = null;
                    item.minContentContributionHeight = null;
                    item.maxContentContributionHeight = null;
                    item.minimumContributionHeight = null;
                }
            }

            if (rerunRowSizing) {
                // Re-run row sizing with known column sizes
                rowSizes = calculateRowSizes(style, nodeInnerSize, availableGridSpace, gap, numRows,
                    items, columnSizes, colCounts, rowCounts, expandedRows);
            }
        }

        // Calculate container size based on tracks
        // IMPORTANT: Use initialColumnSum for container size calculation (per CSS Grid spec)
        // This ensures that max-content sizing returns the initial (unrestricted) size,
        // while columnSizes (potentially re-run) is used for child positioning
        float contentWidth = initialColumnSum;

        // IMPORTANT: Use initialRowSum for container size calculation (per CSS Grid spec)
        float contentHeight = initialRowSum;

        // If no items, return container size based on template
        if (items.isEmpty()) {
            float containerWidth = !Float.isNaN(styledBasedKnownDimensions.width)
                                   ? styledBasedKnownDimensions.width
                                   : contentWidth + contentBoxInset.left + contentBoxInset.right;
            float containerHeight = !Float.isNaN(styledBasedKnownDimensions.height)
                                    ? styledBasedKnownDimensions.height
                                    : contentHeight + contentBoxInset.top + contentBoxInset.bottom;

            containerWidth = TaffyMath.clamp(containerWidth, minSize.width, maxSize.width);
            containerHeight = TaffyMath.clamp(containerHeight, minSize.height, maxSize.height);
            containerWidth = Math.max(containerWidth, paddingBorderSize.width);
            containerHeight = Math.max(containerHeight, paddingBorderSize.height);

            FloatSize containerSize = new FloatSize(containerWidth, containerHeight);

            // Calculate track offsets for absolute positioned children
            // Track offsets start from padding+border edge (content box)
            FloatList colOffsets = calculateTrackOffsets(columnSizes, gap.width, padding.left + border.left);
            FloatList rowOffsets = calculateTrackOffsets(rowSizes, gap.height, padding.top + border.top);

            // Get direction for RTL support (resolve INHERIT)
            boolean isRtlEmpty = layoutComputer.resolveDirection(node) == TaffyDirection.RTL;

            // Layout absolutely positioned children even if no regular items
            layoutAbsoluteChildren(node, containerSize, border, scrollbarGutterX, scrollbarGutterY, colCounts, rowCounts, colOffsets, rowOffsets, isRtlEmpty);

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

            FloatSize contentSize = computeContentSizeFromChildren(node);
            return LayoutOutput.fromSizesAndBaselines(containerSize, contentSize, new FloatPoint(NaN, NaN));
        }

        // Calculate track offsets for absolute positioned children
        // Track offsets start from padding+border edge (content box)
        FloatList columnOffsets = calculateTrackOffsets(columnSizes, gap.width, padding.left + border.left);
        FloatList rowOffsets = calculateTrackOffsets(rowSizes, gap.height, padding.top + border.top);

        // Get direction for RTL/LTR support (resolve INHERIT)
        TaffyDirection direction = layoutComputer.resolveDirection(node);
        boolean isRtl = direction == TaffyDirection.RTL;

        // Place items and calculate final positions
        placeItems(items, columnSizes, rowSizes, gap, contentBoxInset, nodeInnerSize, style, colCounts, rowCounts, isRtl);

        float containerWidth = !Float.isNaN(styledBasedKnownDimensions.width)
                               ? styledBasedKnownDimensions.width
                               : contentWidth + contentBoxInset.left + contentBoxInset.right;
        float containerHeight = !Float.isNaN(styledBasedKnownDimensions.height)
                                ? styledBasedKnownDimensions.height
                                : contentHeight + contentBoxInset.top + contentBoxInset.bottom;

        // Apply min/max
        containerWidth = TaffyMath.clamp(containerWidth, minSize.width, maxSize.width);
        containerHeight = TaffyMath.clamp(containerHeight, minSize.height, maxSize.height);

        // Ensure at least padding/border size
        containerWidth = Math.max(containerWidth, paddingBorderSize.width);
        containerHeight = Math.max(containerHeight, paddingBorderSize.height);

        FloatSize containerSize = new FloatSize(containerWidth, containerHeight);

        if (runMode == RunMode.COMPUTE_SIZE) {
            return LayoutOutput.fromOuterSize(containerSize);
        }

        // Perform final layout
        performFinalLayout(items);

        // Layout absolutely positioned children
        layoutAbsoluteChildren(node, containerSize, border, scrollbarGutterX, scrollbarGutterY, colCounts, rowCounts, columnOffsets, rowOffsets, isRtl);

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

        // Calculate container baseline
        float containerBaseline = calculateContainerBaseline(items);

        FloatSize contentSize = computeContentSizeFromChildren(node);

        return LayoutOutput.fromSizesAndBaselines(
            containerSize,
            contentSize,
            new FloatPoint(NaN, containerBaseline)
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

    /**
     * Calculate the first baseline of the grid container.
     * This is based on the first baseline-aligned item in the first row, or the first item if none are baseline-aligned.
     */
    private float calculateContainerBaseline(List<GridItem> items) {
        if (items.isEmpty()) {
            return NaN;
        }

        // Sort items by row start position
        List<GridItem> sortedItems = new ArrayList<>(items);
        sortedItems.sort((a, b) -> {
            int aRowStart = a.rowStart != null ? a.rowStart : 0;
            int bRowStart = b.rowStart != null ? b.rowStart : 0;
            return Integer.compare(aRowStart, bRowStart);
        });

        // Get the first row index
        var firstItem = sortedItems.get(0);
        int firstRow = firstItem.rowStart != null ? firstItem.rowStart : 0;

        // Find all items in the first row
        List<GridItem> firstRowItems = new ArrayList<>();
        for (GridItem item : sortedItems) {
            int rowStart = item.rowStart != null ? item.rowStart : 0;
            if (rowStart != firstRow) break;
            firstRowItems.add(item);
        }

        // Check if any items in this row are baseline aligned
        GridItem baselineItem = null;
        for (GridItem item : firstRowItems) {
            if (item.alignSelf == AlignItems.BASELINE) {
                baselineItem = item;
                break;
            }
        }

        // Use first baseline-aligned item, or first item if none
        GridItem chosenItem = (baselineItem != null) ? baselineItem : firstRowItems.get(0);

        // Calculate baseline: y_position + baseline (or height if baseline is null)
        float yPosition = (chosenItem.location != null) ? chosenItem.location.y : 0f;
        float height = (chosenItem.computedSize != null && !Float.isNaN(chosenItem.computedSize.height))
                       ? chosenItem.computedSize.height : 0f;
        float baseline = (!Float.isNaN(chosenItem.baseline)) ? chosenItem.baseline : height;

        return yPosition + baseline;
    }

    /**
     * Calculate track offsets from track sizes and gap.
     * Each offset is the distance from the start of the grid (border edge) to the start of that track.
     * Collapsed tracks (size = 0) do not contribute gaps between them and adjacent tracks.
     */
    private FloatList calculateTrackOffsets(FloatList trackSizes, float gap, float borderStart) {
        FloatList offsets = new FloatArrayList();
        float currentOffset = borderStart;
        boolean previousWasNonCollapsed = false;

        for (int i = 0; i < trackSizes.size(); i++) {
            float trackSize = trackSizes.getFloat(i);
            boolean isCollapsed = (trackSize == 0);

            // Add gap before this track if:
            // - This track is not collapsed AND
            // - The previous track was non-collapsed
            if (!isCollapsed && previousWasNonCollapsed) {
                currentOffset += gap;
            }

            offsets.add(currentOffset);
            currentOffset += trackSize;

            if (!isCollapsed) {
                previousWasNonCollapsed = true;
            }
        }
        // Add final offset (after all tracks)
        offsets.add(currentOffset);
        return offsets;
    }

    private List<GridItem> generateGridItems(NodeId node, TaffyStyle containerStyle, FloatSize nodeInnerSize) {
        TaffyTree tree = layoutComputer.getTree();
        List<GridItem> items = new ArrayList<>();

        // Get explicit track counts for negative line number resolution
        List<TrackSizingFunction> templateCols = containerStyle.getGridTemplateColumns();
        List<TrackSizingFunction> templateRows = containerStyle.getGridTemplateRows();
        int explicitColCount = (templateCols != null) ? templateCols.size() : 0;
        int explicitRowCount = (templateRows != null) ? templateRows.size() : 0;

        // Create NamedLineResolver for resolving named grid lines
        NamedLineResolver namedLineResolver = new NamedLineResolver(containerStyle);
        namedLineResolver.setExplicitColumnCount(explicitColCount);
        namedLineResolver.setExplicitRowCount(explicitRowCount);

        int order = 0;
        for (NodeId childId : tree.getChildren(node)) {
            TaffyStyle childStyle = tree.getStyle(childId);
            if (childStyle.getBoxGenerationMode() == BoxGenerationMode.NONE) {
                order++;
                continue;
            }

            GridItem item = new GridItem();
            item.nodeId = childId;
            item.order = order++;
            item.position = childStyle.getPosition();

            Float aspectRatio = childStyle.getAspectRatio();
            FloatRect itemPadding = Resolve.resolveRectOrZero(childStyle.getPadding(), nodeInnerSize.width);
            FloatRect itemBorder = Resolve.resolveRectOrZero(childStyle.getBorder(), nodeInnerSize.width);
            item.padding = itemPadding;
            item.border = itemBorder;
            item.aspectRatio = aspectRatio;

            // Store raw (unresolved) size styles for later re-resolution with known grid area
            item.rawSize = childStyle.getSize();
            item.rawMinSize = childStyle.getMinSize();
            item.rawMaxSize = childStyle.getMaxSize();
            item.boxSizing = childStyle.getBoxSizing();

            FloatSize paddingBorderSum = new FloatSize(
                itemPadding.left + itemPadding.right + itemBorder.left + itemBorder.right,
                itemPadding.top + itemPadding.bottom + itemBorder.top + itemBorder.bottom
            );

            FloatSize boxSizingAdj = childStyle.getBoxSizing() == BoxSizing.CONTENT_BOX
                                     ? paddingBorderSum
                                     : FloatSize.ZERO;

            item.size = maybeAdd(maybeApplyAspectRatio(
                Resolve.maybeResolveSize(childStyle.getSize(), nodeInnerSize), aspectRatio), boxSizingAdj);
            // minSize must be at least paddingBorderSum (CSS spec: size cannot be smaller than padding+border)
            FloatSize resolvedMinSize = maybeAdd(maybeApplyAspectRatio(
                Resolve.maybeResolveSize(childStyle.getMinSize(), nodeInnerSize), aspectRatio), boxSizingAdj);
            // Ensure minSize is at least paddingBorderSum, even if minSize was not explicitly set
            item.minSize = new FloatSize(
                Math.max(!Float.isNaN(resolvedMinSize.width) ? resolvedMinSize.width : 0f, paddingBorderSum.width),
                Math.max(!Float.isNaN(resolvedMinSize.height) ? resolvedMinSize.height : 0f, paddingBorderSum.height)
            );
            item.maxSize = maybeAdd(maybeApplyAspectRatio(
                Resolve.maybeResolveSize(childStyle.getMaxSize(), nodeInnerSize), aspectRatio), boxSizingAdj);

            item.rawMargin = childStyle.getMargin();
            item.margin = Resolve.resolveRectLpaOrZero(childStyle.getMargin(), nodeInnerSize.width);
            item.inset = childStyle.getInset();
            item.overflow = childStyle.getOverflow();
            item.scrollbarWidth = childStyle.getScrollbarWidth();
            item.isCompressibleReplaced = childStyle.getItemIsReplaced();

            // Alignment for track sizing: use child's preference, or parent's default, or STRETCH
            // This merged value is used during track sizing (computeItemKnownDimensions).
            // Final placement (placeItems) re-reads raw values to apply aspectRatio rules.
            AlignItems parentAlignItems = containerStyle.getAlignItems();
            AlignItems parentJustifyItems = containerStyle.justifyItems;
            AlignItems resolvedAlignItems = (parentAlignItems != null && parentAlignItems != AlignItems.AUTO)
                                            ? parentAlignItems
                                            : AlignItems.STRETCH;
            AlignItems resolvedJustifyItems = (parentJustifyItems != null && parentJustifyItems != AlignItems.AUTO)
                                              ? parentJustifyItems
                                              : AlignItems.STRETCH;

            AlignItems rawAlignSelf = childStyle.getAlignSelf();
            AlignItems rawJustifySelf = childStyle.justifySelf;
            item.alignSelf = (rawAlignSelf != null && rawAlignSelf != AlignItems.AUTO) ? rawAlignSelf : resolvedAlignItems;
            item.justifySelf = (rawJustifySelf != null && rawJustifySelf != AlignItems.AUTO) ? rawJustifySelf : resolvedJustifyItems;

            // Grid placement - properly handle LINE, SPAN, NAMED_LINE, NAMED_SPAN, and AUTO
            GridPlacement colStart = childStyle.getGridColumnStart();
            GridPlacement colEnd = childStyle.getGridColumnEnd();
            GridPlacement rowStart = childStyle.getGridRowStart();
            GridPlacement rowEnd = childStyle.getGridRowEnd();

            // Resolve named lines to numeric lines first
            TaffyLine<GridPlacement> resolvedCol = namedLineResolver.resolveColumnNames(
                new TaffyLine<>(colStart, colEnd));
            TaffyLine<GridPlacement> resolvedRow = namedLineResolver.resolveRowNames(
                new TaffyLine<>(rowStart, rowEnd));

            // Parse column placement with explicit track count for negative line resolution
            GridPlacementResult colPlacement = parseGridPlacement(resolvedCol.start, resolvedCol.end, explicitColCount);
            item.columnStart = colPlacement.startIndex;
            item.columnEnd = colPlacement.endIndex;
            item.columnSpan = colPlacement.span;

            // Parse row placement with explicit track count for negative line resolution
            GridPlacementResult rowPlacement = parseGridPlacement(resolvedRow.start, resolvedRow.end, explicitRowCount);
            item.rowStart = rowPlacement.startIndex;
            item.rowEnd = rowPlacement.endIndex;
            item.rowSpan = rowPlacement.span;

            item.computedSize = new FloatSize(0f, 0f);
            item.location = new FloatPoint(0f, 0f);

            if (item.position != TaffyPosition.ABSOLUTE) {
                items.add(item);
            }
        }

        return items;
    }

    /**
     * Result of parsing grid placement.
     *
     * @param startIndex null means auto
     * @param endIndex   null means auto
     */
    private record GridPlacementResult(Integer startIndex, Integer endIndex, int span) {}

    /**
     * Parse grid placement from start and end values.
     * Returns GridPlacementResult where:
     * - startIndex is OriginZero coordinate (null means auto, can be negative for implicit grid)
     * - endIndex is OriginZero coordinate exclusive (null means auto)
     * - span is the number of tracks
     *
     * @param explicitTrackCount The number of explicit tracks for negative line number resolution
     */
    private GridPlacementResult parseGridPlacement(GridPlacement start, GridPlacement end, int explicitTrackCount) {
        Integer startIndex = null;
        Integer endIndex = null;
        int span = 1;

        if (start.isLine()) {
            startIndex = resolveGridLine(start.getLineNumber(), explicitTrackCount);
        }
        if (end.isLine()) {
            endIndex = resolveGridLine(end.getLineNumber(), explicitTrackCount);
        }
        if (start.isSpan()) {
            span = start.getValue();
        }
        if (end.isSpan()) {
            span = end.getValue();
        }

        // If we have start and end, compute span
        if (startIndex != null && endIndex != null) {
            span = endIndex - startIndex;
            if (span < 1) span = 1;
        }
        // If we have start and span but no end
        else if (startIndex != null && end.isSpan()) {
            endIndex = startIndex + span;
        }
        // If we have end and span but no start
        else if (endIndex != null && start.isSpan()) {
            startIndex = endIndex - span;
        }

        return new GridPlacementResult(startIndex, endIndex, span);
    }

    /**
     * Resolve a CSS grid line number to an OriginZero line index.
     * CSS grid line numbers are 1-based:
     * - Positive values count from the start (1 = first line)
     * - Negative values count from the end (-1 = last line)
     * - 0 is invalid and treated as auto (null)
     * <p>
     * OriginZero coordinates:
     * - 0 is the start of the explicit grid
     * - Positive values are lines after the explicit grid start
     * - Negative values are lines before the explicit grid start (negative implicit grid)
     *
     * @param lineNumber         The CSS grid line number (1-based, can be negative)
     * @param explicitTrackCount The number of explicit tracks
     * @return OriginZero line index, or null for auto
     */
    private Integer resolveGridLine(int lineNumber, int explicitTrackCount) {
        if (lineNumber == 0) {
            // Grid line 0 is invalid, treat as auto
            return null;
        } else if (lineNumber > 0) {
            // Positive: line 1 = origin-zero 0, line 2 = origin-zero 1, etc.
            return lineNumber - 1;
        } else {
            // Negative: line -1 = last line (explicitTrackCount), line -2 = second to last, etc.
            // explicitLineCount = explicitTrackCount + 1
            // line -1 -> explicitTrackCount + 1 + (-1) = explicitTrackCount
            // line -2 -> explicitTrackCount + 1 + (-2) = explicitTrackCount - 1
            int explicitLineCount = explicitTrackCount + 1;
            return explicitLineCount + lineNumber;
        }
    }

    /**
     * Expand grid template with auto-fill/auto-fit into a list of TrackSizingFunctions.
     * This resolves repeat(auto-fill, ...) and repeat(auto-fit, ...) based on container size.
     *
     * @param template      The list of GridTemplateComponent (may include auto-fill/auto-fit)
     * @param containerSize The definite container size in this axis, or null if indefinite
     * @param gap           The gap between tracks
     * @return A list of TrackSizingFunctions with auto-repetitions expanded
     */
    private List<TrackSizingFunction> expandAutoRepetition(List<GridTemplateComponent> template,
                                                           float containerSize, float gap) {
        if (template == null || template.isEmpty()) {
            return new ArrayList<>();
        }

        // Check if there's an auto-repetition
        GridRepetition autoRepeat = null;
        int autoRepeatIndex = -1;
        for (int i = 0; i < template.size(); i++) {
            GridTemplateComponent comp = template.get(i);
            if (comp.isAutoRepetition()) {
                autoRepeat = comp.getRepeat();
                autoRepeatIndex = i;
                break;
            }
        }

        if (autoRepeat == null) {
            // No auto-repetition, just expand normally
            List<TrackSizingFunction> result = new ArrayList<>();
            for (GridTemplateComponent comp : template) {
                if (comp.isSingle()) {
                    result.add(comp.getSingle());
                } else if (comp.isRepeat()) {
                    GridRepetition repeat = comp.getRepeat();
                    int count = repeat.getCount();
                    for (int i = 0; i < count; i++) {
                        result.addAll(repeat.getTracks());
                    }
                }
            }
            return result;
        }

        // Has auto-repetition - calculate how many times to repeat
        int numRepetitions;
        if (Float.isNaN(containerSize) || containerSize <= 0) {
            // No definite container size, use 1 repetition
            numRepetitions = 1;
        } else {
            // Calculate space used by non-repeating tracks
            float nonRepeatingUsedSpace = 0f;
            int nonRepeatingTrackCount = 0;
            for (int i = 0; i < template.size(); i++) {
                if (i == autoRepeatIndex) continue;
                GridTemplateComponent comp = template.get(i);
                if (comp.isSingle()) {
                    float val = comp.getSingle().getDefiniteValue(containerSize);
                    if (!Float.isNaN(val)) {
                        nonRepeatingUsedSpace += val;
                        nonRepeatingTrackCount++;
                    }
                } else if (comp.isRepeat()) {
                    GridRepetition repeat = comp.getRepeat();
                    for (int j = 0; j < repeat.getCount(); j++) {
                        for (TrackSizingFunction track : repeat.getTracks()) {
                            float val = track.getDefiniteValue(containerSize);
                            if (!Float.isNaN(val)) {
                                nonRepeatingUsedSpace += val;
                                nonRepeatingTrackCount++;
                            }
                        }
                    }
                }
            }

            // Calculate space per repetition
            float perRepetitionSpace = 0f;
            int tracksPerRepetition = autoRepeat.getTrackCount();
            for (TrackSizingFunction track : autoRepeat.getTracks()) {
                float val = track.getDefiniteValue(containerSize);
                if (!Float.isNaN(val)) {
                    perRepetitionSpace += val;
                }
            }

            // First repetition includes gaps for non-repeating tracks
            int firstRepNonRepeatingTracks = nonRepeatingTrackCount + tracksPerRepetition;
            float firstRepUsedSpace = nonRepeatingUsedSpace + perRepetitionSpace
                                      + Math.max(0, firstRepNonRepeatingTracks - 1) * gap;

            if (firstRepUsedSpace > containerSize) {
                numRepetitions = 1;  // At least 1 repetition
            } else {
                // Calculate how many more can fit
                float perRepGap = tracksPerRepetition * gap;
                float perRepTotal = perRepetitionSpace + perRepGap;
                if (perRepTotal <= 0) {
                    numRepetitions = 1;
                } else {
                    float remainingSpace = containerSize - firstRepUsedSpace;
                    int extraReps = (int) Math.floor(remainingSpace / perRepTotal);
                    numRepetitions = 1 + extraReps;
                }
            }
        }

        // Build result with expanded repetitions
        List<TrackSizingFunction> result = new ArrayList<>();
        for (int i = 0; i < template.size(); i++) {
            GridTemplateComponent comp = template.get(i);
            if (i == autoRepeatIndex) {
                // Expand auto-repetition
                for (int j = 0; j < numRepetitions; j++) {
                    result.addAll(autoRepeat.getTracks());
                }
            } else if (comp.isSingle()) {
                result.add(comp.getSingle());
            } else if (comp.isRepeat()) {
                GridRepetition repeat = comp.getRepeat();
                for (int j = 0; j < repeat.getCount(); j++) {
                    result.addAll(repeat.getTracks());
                }
            }
        }
        return result;
    }

    /**
     * Information about auto-fit tracks in a grid template.
     * Used to identify which tracks should be collapsed if empty.
     *
     * @param startIndex          Index of the first auto-fit track in the expanded template
     * @param trackCount          Number of auto-fit tracks
     * @param tracksPerRepetition Number of tracks per repetition (for multi-track auto-fit)
     */
    private record AutoFitInfo(int startIndex, int trackCount, int tracksPerRepetition) {}

    /**
     * Check if a grid template uses auto-fit and return information about it.
     *
     * @return AutoFitInfo if auto-fit is used, null otherwise
     */
    private AutoFitInfo getAutoFitInfo(List<GridTemplateComponent> template, float containerSize, float gap) {
        if (template == null || template.isEmpty()) {
            return null;
        }

        // Find auto-fit component
        GridRepetition autoFit = null;
        int autoFitIndex = -1;
        int tracksBefore = 0;

        for (int i = 0; i < template.size(); i++) {
            GridTemplateComponent comp = template.get(i);
            if (comp.isAutoRepetition() && comp.getRepeat().getType() == GridRepetition.RepetitionType.AUTO_FIT) {
                autoFit = comp.getRepeat();
                autoFitIndex = i;
                break;
            }
            // Count tracks before auto-fit
            if (comp.isSingle()) {
                tracksBefore++;
            } else if (comp.isRepeat()) {
                GridRepetition repeat = comp.getRepeat();
                tracksBefore += repeat.getCount() * repeat.getTrackCount();
            }
        }

        if (autoFit == null) {
            return null;
        }

        // Calculate number of repetitions (same logic as expandAutoRepetition)
        int numRepetitions;
        if (Float.isNaN(containerSize) || containerSize <= 0) {
            numRepetitions = 1;
        } else {
            // Calculate space per repetition
            float perRepetitionSpace = 0f;
            int tracksPerRepetition = autoFit.getTrackCount();
            for (TrackSizingFunction track : autoFit.getTracks()) {
                float val = track.getDefiniteValue(containerSize);
                if (!Float.isNaN(val)) {
                    perRepetitionSpace += val;
                }
            }

            // Calculate non-repeating used space
            float nonRepeatingUsedSpace = 0f;
            int nonRepeatingTrackCount = 0;
            for (int i = 0; i < template.size(); i++) {
                if (i == autoFitIndex) continue;
                GridTemplateComponent comp = template.get(i);
                if (comp.isSingle()) {
                    float val = comp.getSingle().getDefiniteValue(containerSize);
                    if (!Float.isNaN(val)) {
                        nonRepeatingUsedSpace += val;
                        nonRepeatingTrackCount++;
                    }
                } else if (comp.isRepeat()) {
                    GridRepetition repeat = comp.getRepeat();
                    for (int j = 0; j < repeat.getCount(); j++) {
                        for (TrackSizingFunction track : repeat.getTracks()) {
                            float val = track.getDefiniteValue(containerSize);
                            if (!Float.isNaN(val)) {
                                nonRepeatingUsedSpace += val;
                                nonRepeatingTrackCount++;
                            }
                        }
                    }
                }
            }

            int firstRepNonRepeatingTracks = nonRepeatingTrackCount + tracksPerRepetition;
            float firstRepUsedSpace = nonRepeatingUsedSpace + perRepetitionSpace
                                      + Math.max(0, firstRepNonRepeatingTracks - 1) * gap;

            if (firstRepUsedSpace > containerSize) {
                numRepetitions = 1;
            } else {
                float perRepGap = tracksPerRepetition * gap;
                float perRepTotal = perRepetitionSpace + perRepGap;
                if (perRepTotal <= 0) {
                    numRepetitions = 1;
                } else {
                    float remainingSpace = containerSize - firstRepUsedSpace;
                    int extraReps = (int) Math.floor(remainingSpace / perRepTotal);
                    numRepetitions = 1 + extraReps;
                }
            }
        }

        int totalAutoFitTracks = numRepetitions * autoFit.getTrackCount();
        return new AutoFitInfo(tracksBefore, totalAutoFitTracks, autoFit.getTrackCount());
    }

    /**
     * Collapse empty auto-fit tracks by setting their size to 0.
     * Per CSS Grid spec, auto-fit tracks that don't contain any items should be collapsed.
     *
     * @param columnSizes   The calculated column sizes to modify
     * @param style         The container style (to check for auto-fit)
     * @param items         The grid items
     * @param nodeInnerSize The container inner size
     * @param gap           The gap between tracks
     * @param colCounts     Track counts information
     */
    private void collapseEmptyAutoFitColumns(FloatList columnSizes, TaffyStyle style, List<GridItem> items,
                                             FloatSize nodeInnerSize, float gap, TrackCounts colCounts) {
        if (style.gridTemplateColumnsWithRepeat == null || style.gridTemplateColumnsWithRepeat.isEmpty()) {
            return;
        }

        AutoFitInfo autoFitInfo = getAutoFitInfo(style.gridTemplateColumnsWithRepeat, nodeInnerSize.width, gap);
        if (autoFitInfo == null) {
            return;
        }

        // Build a set of columns that contain items
        boolean[] columnOccupied = new boolean[columnSizes.size()];
        for (GridItem item : items) {
            if (item.position == TaffyPosition.ABSOLUTE) {
                continue;
            }
            int colStart = getItemColumnWithCounts(item, 0, columnSizes.size(), colCounts);
            int colEnd = colStart + item.columnSpan;
            for (int col = colStart; col < colEnd && col < columnSizes.size(); col++) {
                columnOccupied[col] = true;
            }
        }

        // Collapse unoccupied auto-fit columns
        // The auto-fit tracks are in the explicit grid region
        int explicitStart = colCounts.negativeImplicit;
        int autoFitStart = explicitStart + autoFitInfo.startIndex;
        int autoFitEnd = autoFitStart + autoFitInfo.trackCount;

        for (int col = autoFitStart; col < autoFitEnd && col < columnSizes.size(); col++) {
            if (!columnOccupied[col]) {
                columnSizes.set(col, 0f);
            }
        }
    }

    /**
     * Get the expanded grid template columns, handling auto-fill/auto-fit if present.
     */
    private List<TrackSizingFunction> getExpandedTemplateColumns(TaffyStyle style, float containerWidth, float gap) {
        if (style.gridTemplateColumnsWithRepeat != null && !style.gridTemplateColumnsWithRepeat.isEmpty()) {
            return expandAutoRepetition(style.gridTemplateColumnsWithRepeat, containerWidth, gap);
        }
        return style.getGridTemplateColumns();
    }

    /**
     * Get the expanded grid template rows, handling auto-fill/auto-fit if present.
     */
    private List<TrackSizingFunction> getExpandedTemplateRows(TaffyStyle style, float containerHeight, float gap) {
        if (style.gridTemplateRowsWithRepeat != null && !style.gridTemplateRowsWithRepeat.isEmpty()) {
            return expandAutoRepetition(style.gridTemplateRowsWithRepeat, containerHeight, gap);
        }
        return style.getGridTemplateRows();
    }

    /**
     * Compute the number of columns (and negative implicit columns) in the grid.
     * Returns TrackCounts with negativeImplicit, explicit, and positiveImplicit track counts.
     */
    private TrackCounts computeColumnCounts(List<GridItem> items, List<TrackSizingFunction> expandedCols) {
        // Check explicit columns from expanded template (after auto-fill/auto-fit expansion)
        int explicitCols = (expandedCols != null && !expandedCols.isEmpty()) ? expandedCols.size() : 0;

        // Scan items for min (negative implicit) and max (positive implicit) positions
        int minCol = 0;  // Minimum OriginZero position (can be negative)
        int maxCol = explicitCols;  // Maximum OriginZero position (end of explicit grid or beyond)
        int maxSpan = 1;  // Maximum span for auto-placed items

        for (GridItem item : items) {
            if (item.columnStart != null) {
                // Has definite start position
                minCol = Math.min(minCol, item.columnStart);
                int itemEnd = item.columnStart + item.columnSpan;
                maxCol = Math.max(maxCol, itemEnd);
            } else if (item.columnEnd != null) {
                // Has definite end position but auto start
                int itemStart = item.columnEnd - item.columnSpan;
                minCol = Math.min(minCol, itemStart);
                maxCol = Math.max(maxCol, item.columnEnd);
            } else {
                // Fully auto - track max span for later
                maxSpan = Math.max(maxSpan, item.columnSpan);
            }
        }

        // Compute negative implicit tracks (for items positioned before line 0)
        int negativeImplicit = minCol < 0 ? -minCol : 0;

        // Compute positive implicit tracks (for items positioned after explicit grid)
        int positiveImplicit = maxCol > explicitCols ? maxCol - explicitCols : 0;

        // Note: We do NOT estimate extra tracks for auto-placed items here.
        // The auto-placement algorithm will dynamically expand the grid as needed.
        // This matches Rust's compute_grid_size_estimate behavior.

        // Ensure we have enough tracks for the max span of any indefinitely placed item
        int totalTracks = negativeImplicit + explicitCols + positiveImplicit;
        if (totalTracks < maxSpan) {
            positiveImplicit = maxSpan - negativeImplicit - explicitCols;
        }

        return new TrackCounts(negativeImplicit, explicitCols, positiveImplicit);
    }

    /**
     * Compute the number of rows (and negative implicit rows) in the grid.
     * Returns TrackCounts with negativeImplicit, explicit, and positiveImplicit track counts.
     * <p>
     * Note: This only estimates based on explicitly positioned items and their spans.
     * Auto-placed items will cause dynamic expansion during the auto-placement algorithm.
     * This matches Rust's compute_grid_size_estimate behavior.
     */
    private TrackCounts computeRowCounts(List<GridItem> items, List<TrackSizingFunction> expandedRows) {
        // Check explicit rows from expanded template (after auto-fill/auto-fit expansion)
        int explicitRows = (expandedRows != null && !expandedRows.isEmpty()) ? expandedRows.size() : 0;

        // Scan items for min (negative implicit) and max (positive implicit) positions
        int minRow = 0;
        int maxRow = explicitRows;
        int maxSpan = 1;

        for (GridItem item : items) {
            if (item.rowStart != null) {
                minRow = Math.min(minRow, item.rowStart);
                int itemEnd = item.rowStart + item.rowSpan;
                maxRow = Math.max(maxRow, itemEnd);
            } else if (item.rowEnd != null) {
                int itemStart = item.rowEnd - item.rowSpan;
                minRow = Math.min(minRow, itemStart);
                maxRow = Math.max(maxRow, item.rowEnd);
            } else {
                // Fully auto - track max span for later
                maxSpan = Math.max(maxSpan, item.rowSpan);
            }
        }

        // Compute negative and positive implicit tracks
        int negativeImplicit = minRow < 0 ? -minRow : 0;
        int positiveImplicit = maxRow > explicitRows ? maxRow - explicitRows : 0;

        // Note: We do NOT estimate extra tracks for auto-placed items here.
        // The auto-placement algorithm will dynamically expand the grid as needed.
        // This matches Rust's compute_grid_size_estimate behavior.

        // Ensure we have enough tracks for the max span of any indefinitely placed item
        int totalTracks = negativeImplicit + explicitRows + positiveImplicit;
        if (totalTracks < maxSpan) {
            positiveImplicit = maxSpan - negativeImplicit - explicitRows;
        }

        return new TrackCounts(negativeImplicit, explicitRows, positiveImplicit);
    }

    /**
     * Auto-place items that don't have explicit column/row positions.
     * Supports both sparse (default) and dense packing algorithms.
     * CSS Grid Specification: 8.5. Grid Item Placement Algorithm
     * <p>
     * For ROW flow:
     * - primary axis = column (horizontal, items flow across columns first)
     * - secondary axis = row (vertical)
     * <p>
     * For COLUMN flow:
     * - primary axis = row (vertical, items flow down rows first)
     * - secondary axis = column (horizontal)
     * <p>
     * Processing order:
     * 1. Items with definite positions in both axes
     * 2. Items with definite position only in SECONDARY axis (row for row flow, column for column flow)
     * 3. Items with secondary axis NOT definite (including those with only primary definite, and fully auto)
     * <p>
     * The occupancy matrix uses 0-based indices where 0 is the first track in the implicit grid.
     * We use TrackCounts to convert between OriginZero coordinates and matrix indices.
     * <p>
     * This implementation uses a CellOccupancyMatrix that can dynamically expand as needed.
     */
    private void autoPlaceItems(List<GridItem> items, GridAutoFlow autoFlow,
                                TrackCounts colCounts, TrackCounts rowCounts) {
        boolean isDense = autoFlow != null && autoFlow.isDense();
        boolean isRowFlow = autoFlow == null || autoFlow.isRow();

        // Create a dynamically expanding occupancy matrix
        CellOccupancyMatrix matrix = new CellOccupancyMatrix(colCounts, rowCounts);

        // Step 1: Place items with definite positions in both axes
        for (GridItem item : items) {
            if (item.columnStart != null && item.rowStart != null) {
                matrix.markArea(item.columnStart, item.rowStart, item.columnSpan, item.rowSpan);
            }
        }

        // Step 2: Place items with definite SECONDARY axis position only
        // For row flow: secondary=row -> items with definite row but auto column
        // For column flow: secondary=column -> items with definite column but auto row
        for (GridItem item : items) {
            if (item.columnStart != null && item.rowStart != null) {
                continue; // Already placed in step 1
            }

            int colSpan = item.columnSpan;
            int rowSpan = item.rowSpan;

            if (isRowFlow) {
                // For row flow: secondary axis = row
                // Handle items with definite row but auto column
                if (item.rowStart != null) {
                    int rowStartOz = item.rowStart;
                    int[] result = matrix.findEmptyAreaInRow(rowStartOz, colSpan, rowSpan);
                    if (result != null) {
                        item.columnStart = result[0];
                        matrix.markArea(item.columnStart, rowStartOz, colSpan, rowSpan);
                    } else {
                        // Expand grid and place at end
                        item.columnStart = matrix.colCounts.positiveImplicitEndLine();
                        matrix.markArea(item.columnStart, rowStartOz, colSpan, rowSpan);
                    }
                }
            } else {
                // For column flow: secondary axis = column
                // Handle items with definite column but auto row
                if (item.columnStart != null) {
                    int colStartOz = item.columnStart;
                    int[] result = matrix.findEmptyAreaInColumn(colStartOz, colSpan, rowSpan);
                    if (result != null) {
                        item.rowStart = result[0];
                        matrix.markArea(colStartOz, item.rowStart, colSpan, rowSpan);
                    } else {
                        // Expand grid and place at end
                        item.rowStart = matrix.rowCounts.positiveImplicitEndLine();
                        matrix.markArea(colStartOz, item.rowStart, colSpan, rowSpan);
                    }
                }
            }
        }

        // Step 3: Place items with secondary axis NOT definite (in DOM order)
        // This includes items with only primary axis definite, and fully auto items
        // For row flow: secondary=row, so process items where row is NOT definite
        // For column flow: secondary=column, so process items where column is NOT definite
        int autoPrimaryIdx = 0;  // Current position in primary axis
        int autoSecondaryIdx = 0;  // Current position in secondary axis

        for (GridItem item : items) {
            if (item.columnStart != null && item.rowStart != null) {
                // Already positioned
                continue;
            }

            // Check if secondary axis is already definite (already handled in step 2)
            if (isRowFlow && item.rowStart != null) {
                continue; // Row is definite, was handled in step 2
            }
            if (!isRowFlow && item.columnStart != null) {
                continue; // Column is definite, was handled in step 2
            }

            int colSpan = item.columnSpan;
            int rowSpan = item.rowSpan;

            // For row flow: primary axis = column
            // For column flow: primary axis = row
            if (isRowFlow) {
                // Check if primary (column) is definite
                if (item.columnStart != null) {
                    // Primary (column) definite, secondary (row) auto
                    int colStartOz = item.columnStart;
                    int[] result = matrix.findEmptyAreaInColumn(colStartOz, colSpan, rowSpan);
                    if (result != null) {
                        item.rowStart = result[0];
                        matrix.markArea(colStartOz, item.rowStart, colSpan, rowSpan);
                    } else {
                        // Expand grid and place at end
                        item.rowStart = matrix.rowCounts.positiveImplicitEndLine();
                        matrix.markArea(colStartOz, item.rowStart, colSpan, rowSpan);
                    }
                } else {
                    // Fully auto - search row by row, column by column
                    // For row flow: iterate columns first (primary), then rows (secondary)
                    int startRow = isDense ? 0 : autoSecondaryIdx;
                    int startCol = isDense ? 0 : autoPrimaryIdx;

                    int[] result = matrix.findEmptyArea(isRowFlow, startCol, startRow, colSpan, rowSpan, isDense);
                    if (result != null) {
                        item.columnStart = result[0];
                        item.rowStart = result[1];
                        matrix.markArea(item.columnStart, item.rowStart, colSpan, rowSpan);
                        if (!isDense) {
                            // Update cursor - for row flow, advance along columns then rows
                            autoPrimaryIdx = result[0] + colSpan;
                            autoSecondaryIdx = result[1];
                            if (autoPrimaryIdx >= matrix.colCounts.len()) {
                                autoPrimaryIdx = 0;
                                autoSecondaryIdx++;
                            }
                        }
                    } else {
                        // Place at new row at end of grid
                        int newRowStart = matrix.rowCounts.positiveImplicitEndLine();
                        item.columnStart = 0;
                        item.rowStart = newRowStart;
                        matrix.markArea(item.columnStart, item.rowStart, colSpan, rowSpan);
                        if (!isDense) {
                            autoPrimaryIdx = colSpan;
                            autoSecondaryIdx = newRowStart;
                            if (autoPrimaryIdx >= matrix.colCounts.len()) {
                                autoPrimaryIdx = 0;
                                autoSecondaryIdx++;
                            }
                        }
                    }
                }
            } else {
                // Column flow: primary axis = row
                if (item.rowStart != null) {
                    // Primary (row) definite, secondary (column) auto
                    int rowStartOz = item.rowStart;
                    int[] result = matrix.findEmptyAreaInRow(rowStartOz, colSpan, rowSpan);
                    if (result != null) {
                        item.columnStart = result[0];
                        matrix.markArea(item.columnStart, rowStartOz, colSpan, rowSpan);
                    } else {
                        // Expand grid and place at end
                        item.columnStart = matrix.colCounts.positiveImplicitEndLine();
                        matrix.markArea(item.columnStart, rowStartOz, colSpan, rowSpan);
                    }
                } else {
                    // Fully auto - for column flow: iterate rows first (primary), then columns (secondary)
                    int startCol = isDense ? 0 : autoSecondaryIdx;
                    int startRow = isDense ? 0 : autoPrimaryIdx;

                    int[] result = matrix.findEmptyArea(isRowFlow, startCol, startRow, colSpan, rowSpan, isDense);
                    if (result != null) {
                        item.columnStart = result[0];
                        item.rowStart = result[1];
                        matrix.markArea(item.columnStart, item.rowStart, colSpan, rowSpan);
                        if (!isDense) {
                            // Update cursor - for column flow, advance along rows then columns
                            autoPrimaryIdx = result[1] + rowSpan;
                            autoSecondaryIdx = result[0];
                            if (autoPrimaryIdx >= matrix.rowCounts.len()) {
                                autoPrimaryIdx = 0;
                                autoSecondaryIdx++;
                            }
                        }
                    } else {
                        // Place at new column at end of grid
                        int newColStart = matrix.colCounts.positiveImplicitEndLine();
                        item.columnStart = newColStart;
                        item.rowStart = 0;
                        matrix.markArea(item.columnStart, item.rowStart, colSpan, rowSpan);
                        if (!isDense) {
                            autoPrimaryIdx = rowSpan;
                            autoSecondaryIdx = newColStart;
                            if (autoPrimaryIdx >= matrix.rowCounts.len()) {
                                autoPrimaryIdx = 0;
                                autoSecondaryIdx++;
                            }
                        }
                    }
                }
            }
        }

        // Update the TrackCounts with final values after placement
        colCounts.update(matrix.colCounts);
        rowCounts.update(matrix.rowCounts);
    }

    /**
     * A dynamically-expanding cell occupancy matrix for grid auto-placement.
     * This mirrors Rust's CellOccupancyMatrix behavior.
     */
    private static class CellOccupancyMatrix {
        TrackCounts colCounts;
        TrackCounts rowCounts;
        boolean[][] cells;  // [row][col] in matrix coordinates

        CellOccupancyMatrix(TrackCounts colCounts, TrackCounts rowCounts) {
            this.colCounts = new TrackCounts(colCounts);
            this.rowCounts = new TrackCounts(rowCounts);
            this.cells = new boolean[rowCounts.len()][colCounts.len()];
        }

        /**
         * Convert OriginZero column to matrix index.
         */
        private int colToIndex(int ozCol) {
            return colCounts.ozLineToNextTrack(ozCol);
        }

        /**
         * Convert OriginZero row to matrix index.
         */
        private int rowToIndex(int ozRow) {
            return rowCounts.ozLineToNextTrack(ozRow);
        }

        /**
         * Convert matrix column index to OriginZero.
         */
        private int indexToCol(int idx) {
            return colCounts.trackToOzLine(idx);
        }

        /**
         * Convert matrix row index to OriginZero.
         */
        private int indexToRow(int idx) {
            return rowCounts.trackToOzLine(idx);
        }

        /**
         * Expand the matrix to fit the given OriginZero range.
         */
        private void expandToFit(int ozColStart, int ozRowStart, int colSpan, int rowSpan) {
            int ozColEnd = ozColStart + colSpan;
            int ozRowEnd = ozRowStart + rowSpan;

            // Calculate required expansion
            int reqNegCols = Math.max(-ozColStart - colCounts.negativeImplicit, 0);
            int reqPosCols = Math.max(ozColEnd - (colCounts.explicit + colCounts.positiveImplicit), 0);
            int reqNegRows = Math.max(-ozRowStart - rowCounts.negativeImplicit, 0);
            int reqPosRows = Math.max(ozRowEnd - (rowCounts.explicit + rowCounts.positiveImplicit), 0);

            if (reqNegCols == 0 && reqPosCols == 0 && reqNegRows == 0 && reqPosRows == 0) {
                return;  // No expansion needed
            }

            int oldNumCols = colCounts.len();
            int oldNumRows = rowCounts.len();
            int newNumCols = oldNumCols + reqNegCols + reqPosCols;
            int newNumRows = oldNumRows + reqNegRows + reqPosRows;

            boolean[][] newCells = new boolean[newNumRows][newNumCols];

            // Copy old cells to new matrix, offset by negative expansions
            for (int r = 0; r < oldNumRows; r++) {
                if (oldNumCols >= 0)
                    System.arraycopy(cells[r], 0, newCells[r + reqNegRows], reqNegCols, oldNumCols);
            }

            cells = newCells;
            colCounts.negativeImplicit += reqNegCols;
            colCounts.positiveImplicit += reqPosCols;
            rowCounts.negativeImplicit += reqNegRows;
            rowCounts.positiveImplicit += reqPosRows;
        }

        /**
         * Mark an area as occupied, expanding the matrix if needed.
         */
        void markArea(int ozColStart, int ozRowStart, int colSpan, int rowSpan) {
            expandToFit(ozColStart, ozRowStart, colSpan, rowSpan);

            int colIdx = colToIndex(ozColStart);
            int rowIdx = rowToIndex(ozRowStart);

            for (int r = rowIdx; r < rowIdx + rowSpan; r++) {
                for (int c = colIdx; c < colIdx + colSpan; c++) {
                    cells[r][c] = true;
                }
            }
        }

        /**
         * Check if an area is available (all cells unoccupied).
         */
        private boolean isAreaAvailable(int colIdx, int rowIdx, int colSpan, int rowSpan) {
            int numCols = colCounts.len();
            int numRows = rowCounts.len();

            if (colIdx < 0 || rowIdx < 0 || colIdx + colSpan > numCols || rowIdx + rowSpan > numRows) {
                return false;
            }

            for (int r = rowIdx; r < rowIdx + rowSpan; r++) {
                for (int c = colIdx; c < colIdx + colSpan; c++) {
                    if (cells[r][c]) return false;
                }
            }
            return true;
        }

        /**
         * Find an empty area in a specific row (for items with definite row).
         * Returns [ozColStart] or null if not found within current grid.
         */
        int[] findEmptyAreaInRow(int ozRowStart, int colSpan, int rowSpan) {
            int rowIdx = rowToIndex(ozRowStart);
            int numCols = colCounts.len();

            for (int c = 0; c <= numCols - colSpan; c++) {
                if (isAreaAvailable(c, rowIdx, colSpan, rowSpan)) {
                    return new int[]{indexToCol(c)};
                }
            }
            return null;
        }

        /**
         * Find an empty area in a specific column (for items with definite column).
         * Returns [ozRowStart] or null if not found within current grid.
         */
        int[] findEmptyAreaInColumn(int ozColStart, int colSpan, int rowSpan) {
            int colIdx = colToIndex(ozColStart);
            int numRows = rowCounts.len();

            for (int r = 0; r <= numRows - rowSpan; r++) {
                if (isAreaAvailable(colIdx, r, colSpan, rowSpan)) {
                    return new int[]{indexToRow(r)};
                }
            }
            return null;
        }

        /**
         * Find an empty area for auto-placement.
         * Returns [ozColStart, ozRowStart] or null if not found within current grid.
         */
        int[] findEmptyArea(boolean isRowFlow, int startCol, int startRow, int colSpan, int rowSpan, boolean isDense) {
            int numCols = colCounts.len();
            int numRows = rowCounts.len();

            if (isRowFlow) {
                // Row flow: iterate rows (secondary), then columns (primary)
                int rowStart = isDense ? 0 : startRow;
                for (int r = rowStart; r <= numRows - rowSpan; r++) {
                    int colStart = (r == startRow && !isDense) ? startCol : 0;
                    for (int c = colStart; c <= numCols - colSpan; c++) {
                        if (isAreaAvailable(c, r, colSpan, rowSpan)) {
                            return new int[]{indexToCol(c), indexToRow(r)};
                        }
                    }
                }
            } else {
                // Column flow: iterate columns (secondary), then rows (primary)
                int colStart = isDense ? 0 : startCol;
                for (int c = colStart; c <= numCols - colSpan; c++) {
                    int rowStart = (c == startCol && !isDense) ? startRow : 0;
                    for (int r = rowStart; r <= numRows - rowSpan; r++) {
                        if (isAreaAvailable(c, r, colSpan, rowSpan)) {
                            return new int[]{indexToCol(c), indexToRow(r)};
                        }
                    }
                }
            }
            return null;
        }
    }

    private FloatList calculateColumnSizes(
        TaffyStyle style,
        FloatSize nodeInnerSize,
        TaffySize<AvailableSpace> availableGridSpace,
        TaffySize<AvailableSpace> originalAvailableSpace,
        FloatSize gap,
        int numColumns,
        int numRows,
        List<GridItem> items,
        TrackCounts colCounts,
        List<TrackSizingFunction> expandedCols) {
        return calculateColumnSizesWithRowSizes(style, nodeInnerSize, availableGridSpace, originalAvailableSpace, gap,
            numColumns, numRows, items, colCounts, expandedCols, null);
    }

    private FloatList calculateColumnSizesWithRowSizes(
        TaffyStyle style,
        FloatSize nodeInnerSize,
        TaffySize<AvailableSpace> availableGridSpace,
        TaffySize<AvailableSpace> originalAvailableSpace,
        FloatSize gap,
        int numColumns,
        int numRows,
        List<GridItem> items,
        TrackCounts colCounts,
        List<TrackSizingFunction> expandedCols,
        FloatList knownRowSizes) {

        FloatList sizes = new FloatArrayList();
        FloatList growthLimits = new FloatArrayList();// Track max size for each column (growth limit)
        List<TrackSizingFunction> templateCols = expandedCols;
        List<TrackSizingFunction> templateRows = style.getGridTemplateRows();
        List<TrackSizingFunction> autoRows = style.getGridAutoRows();

        // Use available grid space when node inner size is not definite
        // This allows the grid to be constrained by parent's available space
        AvailableSpace gridWidthSpace = availableGridSpace.width;
        float availableGridWidth = gridWidthSpace.intoOption();

        float availableWidth = !Float.isNaN(nodeInnerSize.width)
                               ? nodeInnerSize.width - gap.width * (numColumns - 1)
                               : (!Float.isNaN(availableGridWidth)
                                  ? availableGridWidth - gap.width * (numColumns - 1)
                                  : 0);

        float totalFr = 0f;  // Sum of all fr values
        float usedSpace = 0;
        IntList frTrackIndices = new IntArrayList();  // Track which indices have fr
        FloatList frValues = new FloatArrayList();  // Store fr values

        // Get auto columns for implicit tracks
        List<TrackSizingFunction> autoColumns = style.getGridAutoColumns();
        int autoColCount = (autoColumns != null && !autoColumns.isEmpty()) ? autoColumns.size() : 0;
        int explicitColCount = (templateCols != null) ? templateCols.size() : 0;

        // Initialize growth limits list
        for (int i = 0; i < numColumns; i++) {
            growthLimits.add(Float.MAX_VALUE);
        }

        // First pass: resolve fixed sizes and count fr units
        // Use colCounts to determine which tracks are explicit vs implicit
        int negativeImplicitCount = colCounts.negativeImplicit;
        int explicitStart = negativeImplicitCount;
        int explicitEnd = explicitStart + explicitColCount;

        for (int i = 0; i < numColumns; i++) {
            // Determine the track sizing function for this column
            TrackSizingFunction track;
            if (i >= explicitStart && i < explicitEnd && templateCols != null) {
                // Explicit track from grid-template-columns
                int templateIndex = i - explicitStart;
                track = templateCols.get(templateIndex);
            } else if (autoColCount > 0) {
                // Implicit track from grid-auto-columns (cycles through)
                // For negative implicit tracks (i < explicitStart): cycle in reverse order
                // For positive implicit tracks (i >= explicitEnd): cycle in forward order
                int implicitIndex;
                if (i < explicitStart) {
                    // Negative implicit: count backwards from explicit start
                    // The first negative implicit track (closest to explicit) should be auto[autoColCount-1]
                    // Going further back cycles in reverse
                    int distanceFromExplicit = explicitStart - i;
                    implicitIndex = (autoColCount - (distanceFromExplicit % autoColCount)) % autoColCount;
                } else {
                    // Positive implicit: cycle forward from index 0
                    int distanceFromExplicit = i - explicitEnd;
                    implicitIndex = distanceFromExplicit % autoColCount;
                }
                track = autoColumns.get(implicitIndex);
            } else {
                // No auto columns defined, default to auto
                track = null;
            }

            if (track != null && track.isFr()) {
                totalFr += track.getFrValue();  // Accumulate fr values
                frTrackIndices.add(i);
                frValues.add(track.getFrValue());
                sizes.add(0f); // Will be resolved later
            } else if (track == null || track.isAuto() || track.isMinContent() || track.isMaxContent()) {
                // Auto or content-based track - will be resolved later
                sizes.add(NaN);
            } else if (track.isMinmax()) {
                // For minmax, check if max is flexible
                TrackSizingFunction minF = track.getMinFunc();
                TrackSizingFunction maxF = track.getMaxFunc();
                if (maxF != null && maxF.isFr()) {
                    totalFr += maxF.getFrValue();
                    frTrackIndices.add(i);
                    frValues.add(maxF.getFrValue());
                    sizes.add(0f);
                } else if (minF != null && minF.isFixed() && maxF != null && maxF.isFixed()) {
                    // minmax(fixed, fixed) - start at min, can grow to max
                    float minSize = minF.getFixedValue().resolveOrZero(nodeInnerSize.width);
                    float maxSize = maxF.getFixedValue().resolveOrZero(nodeInnerSize.width);
                    sizes.add(minSize);
                    usedSpace += minSize;
                    // Set growth limit to max size
                    growthLimits.set(i, maxSize);
                } else if (maxF != null && maxF.isFixed()) {
                    // minmax(auto/content, fixed) - try to resolve max
                    float maybeSize = maxF.getFixedValue().maybeResolve(nodeInnerSize.width);
                    if (!Float.isNaN(maybeSize)) {
                        // Check if min is intrinsic (auto, min-content, max-content)
                        // For intrinsic min, we need to calculate the contribution first
                        if (minF != null && (minF.isMinContent() || minF.isMaxContent())) {
                            // Need to calculate content-based size later
                            // For now, treat as auto and calculate content contribution
                            sizes.add(NaN);
                            // Set growth limit to max value to cap growth
                            growthLimits.set(i, maybeSize);
                        } else {
                            // minF is auto or null - use max as base size
                            sizes.add(maybeSize);
                            usedSpace += maybeSize;
                            // Set growth limit to same as base to prevent further growth
                            growthLimits.set(i, maybeSize);
                        }
                    } else {
                        // max is not resolvable (e.g., percent with no parent size)
                        // Treat as auto
                        sizes.add(NaN);
                    }
                } else {
                    // Auto-like minmax - treat as auto
                    sizes.add(NaN);
                }
            } else if (track.isFitContent()) {
                // fit-content tracks are like auto but with a max limit
                sizes.add(NaN);
            } else {
                // Fixed track (length or percentage)
                // If percentage and container width is indefinite, treat as auto per CSS Grid spec
                if (track.isFixed() && track.getFixedValue() != null &&
                    track.getFixedValue().isPercent() && Float.isNaN(nodeInnerSize.width)) {
                    // Percentage track with indefinite container - treat as auto
                    sizes.add(NaN);
                } else {
                    float size = resolveTrackSize(track, nodeInnerSize.width);
                    sizes.add(size);
                    usedSpace += size;
                }
            }
        }

        // Handle fr tracks (including 0fr which behave like auto for base size)
        if (!frTrackIndices.isEmpty()) {
            // Step 1: Calculate base sizes for fr tracks based on item content (fr = minmax(auto, Nfr))
            // For items in fr tracks, their minimum/min-content contribution sets the track base size
            FloatList frBaseSizes = new FloatArrayList();
            for (int idx = 0; idx < frTrackIndices.size(); idx++) {
                frBaseSizes.add(0f);
            }

            // Calculate base sizes from span=1 items in fr tracks
            for (int itemIdx = 0; itemIdx < items.size(); itemIdx++) {
                GridItem item = items.get(itemIdx);
                int col = getItemColumnWithCounts(item, itemIdx, numColumns, colCounts);
                int span = item.columnSpan;

                if (span == 1 && frTrackIndices.contains(col)) {
                    int frIdx = frTrackIndices.indexOf(col);
                    // Horizontal percentage margins resolve to 0 in track sizing to avoid cyclic dependency
                    FloatSize marginAxisSums = item.getMarginAxisSumsWithBaselineShims(nodeInnerSize.width);
                    float minContribution;
                    if (!Float.isNaN(item.size.width)) {
                        minContribution = item.size.width + marginAxisSums.width;
                    } else {
                        // Estimate row height for aspect-ratio resolution
                        float estimatedRowHeight = estimateItemRowHeightWithKnownSizes(item, templateRows, autoRows, numRows, nodeInnerSize.height, gap, knownRowSizes);

                        // Use cached contribution - key optimization!
                        // Note: getMinContentContributionWidthCached already includes margin, so don't add again
                        FloatSize availSpace = new FloatSize(NaN, estimatedRowHeight);
                        item.availableSpaceCache = availSpace;
                        minContribution = item.getMinContentContributionWidthCached(layoutComputer, availSpace, nodeInnerSize);
                    }
                    frBaseSizes.set(frIdx, Math.max(frBaseSizes.getFloat(frIdx), minContribution));
                }
            }

            // Set initial fr track sizes to their base sizes
            for (int idx = 0; idx < frTrackIndices.size(); idx++) {
                int i = frTrackIndices.getInt(idx);
                sizes.set(i, frBaseSizes.getFloat(idx));
            }

            // 11.6 Maximise Tracks: Before expanding flexible tracks, grow non-flex tracks to their growth limits
            // This ensures tracks like minmax(20px, 40px) are maximized before fr tracks get remaining space
            if (!Float.isNaN(nodeInnerSize.width)) {
                float currentUsed = 0f;
                for (int i = 0; i < sizes.size(); i++) {
                    float s = sizes.getFloat(i);
                    if (!Float.isNaN(s)) currentUsed += s;
                }
                float freeSpace = availableWidth - currentUsed;
                if (freeSpace > 0) {
                    // Find non-flex tracks that can grow (have finite growth limits > base size)
                    IntList growableTracks = new IntArrayList();
                    for (int i = 0; i < sizes.size(); i++) {
                        if (!frTrackIndices.contains(i)) {
                            float limit = growthLimits.getFloat(i);
                            float size = sizes.getFloat(i);
                            if (!Float.isNaN(limit) && limit != Float.MAX_VALUE && !Float.isNaN(size) && limit > size) {
                                growableTracks.add(i);
                            }
                        }
                    }
                    // Distribute free space to growable tracks up to their limits
                    while (freeSpace > 0.001f && !growableTracks.isEmpty()) {
                        float sharePerTrack = freeSpace / growableTracks.size();
                        float distributed = 0f;
                        IntList stillGrowable = new IntArrayList();
                        for (int i = 0; i < growableTracks.size(); i++) {
                            int idx = growableTracks.getInt(i);
                            float size = sizes.getFloat(idx);
                            float limit = growthLimits.getFloat(idx);
                            float room = limit - size;
                            float increase = Math.min(sharePerTrack, room);
                            sizes.set(idx, size + increase);
                            distributed += increase;
                            if (size + increase < limit - 0.001f) {
                                stillGrowable.add(idx);
                            }
                        }
                        freeSpace -= distributed;
                        growableTracks = stillGrowable;
                        if (distributed < 0.001f) break;
                    }
                    // Update usedSpace after maximise step
                    usedSpace = 0f;
                    for (int i = 0; i < sizes.size(); i++) {
                        if (!frTrackIndices.contains(i)) {
                            float s = sizes.getFloat(i);
                            if (!Float.isNaN(s)) usedSpace += s;
                        }
                    }
                }
            }

            // Only distribute flex space if there are actual fr values > 0
            if (totalFr > 0 && !Float.isNaN(nodeInnerSize.width)) {
                // Definite container - use find_size_of_fr algorithm
                // Available space for fr tracks = total available - fixed track sizes
                float spaceForFrTracks = availableWidth - usedSpace;

                if (spaceForFrTracks > 0) {
                    // Find the size of an fr unit using the algorithm from CSS Grid spec
                    float flexFraction = findSizeOfFrForDefinite(frBaseSizes, frValues, spaceForFrTracks);

                    // Apply: base_size = max(base_size, flex_factor * flex_fraction)
                    for (int idx = 0; idx < frTrackIndices.size(); idx++) {
                        int i = frTrackIndices.getInt(idx);
                        float baseSize = frBaseSizes.getFloat(idx);
                        float frValue = frValues.getFloat(idx);
                        float flexSize = frValue * flexFraction;
                        sizes.set(i, Math.max(baseSize, flexSize));
                    }
                }
            } else if (totalFr > 0) {
                // Indefinite container (MaxContent) - two-phase process

                // Phase 1: Distribute spanning items' contributions to fr tracks (Step 4 of resolve_intrinsic_track_sizes)
                // Items that span flex tracks have their contribution distributed proportionally by flex factor
                for (int itemIdx = 0; itemIdx < items.size(); itemIdx++) {
                    GridItem item = items.get(itemIdx);
                    int col = getItemColumnWithCounts(item, itemIdx, numColumns, colCounts);
                    int span = item.columnSpan;

                    // Check if this item crosses any fr tracks
                    IntList frTracksInSpan = new IntArrayList();
                    float spanFrSum = 0f;
                    for (int c = col; c < col + span && c < numColumns; c++) {
                        if (frTrackIndices.contains(c)) {
                            frTracksInSpan.add(c);
                            int idx = frTrackIndices.indexOf(c);
                            spanFrSum += frValues.getFloat(idx);
                        }
                    }

                    if (!frTracksInSpan.isEmpty()) {
                        // Horizontal percentage margins resolve to 0 in track sizing to avoid cyclic dependency
                        FloatSize marginAxisSums = item.getMarginAxisSumsWithBaselineShims(nodeInnerSize.width);
                        // Get item's contribution (max-content for indefinite sizing)
                        float itemContribution;
                        if (!Float.isNaN(item.size.width)) {
                            itemContribution = item.size.width + marginAxisSums.width;
                        } else {
                            // Estimate the height available for this item based on row tracks
                            float estimatedRowHeight = estimateItemRowHeightWithKnownSizes(item, templateRows, autoRows, numRows, nodeInnerSize.height, gap, knownRowSizes);

                            // Use cached contribution - key optimization!
                            // Note: getMaxContentContributionWidthCached already includes margin
                            FloatSize availSpace = new FloatSize(NaN, estimatedRowHeight);
                            item.availableSpaceCache = availSpace;
                            itemContribution = item.getMaxContentContributionWidthCached(layoutComputer, availSpace, nodeInnerSize);
                        }

                        // Subtract existing track sizes (non-fr tracks in span)
                        // If track size is null (not yet computed), estimate using min-content of span=1 items
                        for (int c = col; c < col + span && c < numColumns; c++) {
                            if (!frTrackIndices.contains(c)) {
                                float trackSize = sizes.getFloat(c);
                                if (!Float.isNaN(trackSize)) {
                                    itemContribution -= trackSize;
                                } else {
                                    // Track not yet sized - estimate min-content from span=1 items in this track
                                    float trackMinContent = estimateTrackMinContent(c, items, numColumns, nodeInnerSize, colCounts);
                                    itemContribution -= trackMinContent;
                                }
                            }
                        }
                        // Subtract gaps
                        if (span > 1) {
                            itemContribution -= gap.width * (span - 1);
                        }

                        if (itemContribution > 0 && spanFrSum > 0) {
                            // Distribute contribution to fr tracks proportionally by flex factor
                            for (int i = 0; i < frTracksInSpan.size(); i++) {
                                int c = frTracksInSpan.getInt(i);
                                int idx = frTrackIndices.indexOf(c);
                                float frValue = frValues.getFloat(idx);
                                float share = itemContribution * (frValue / spanFrSum);
                                frBaseSizes.set(idx, Math.max(frBaseSizes.getFloat(idx), share));
                            }
                        }
                    }
                }

                // Update fr track sizes to their new base sizes
                for (int idx = 0; idx < frTrackIndices.size(); idx++) {
                    int i = frTrackIndices.getInt(idx);
                    sizes.set(i, frBaseSizes.getFloat(idx));
                }

                // Phase 2: Calculate flex_fraction based on track base sizes
                // For each fr track: if flex_factor > 1, use base_size/flex_factor; else use base_size
                float flexFraction = 0f;
                for (int idx = 0; idx < frTrackIndices.size(); idx++) {
                    float baseSize = frBaseSizes.getFloat(idx);
                    float frValue = frValues.getFloat(idx);
                    float trackFr;
                    if (frValue > 1.0f) {
                        trackFr = baseSize / frValue;
                    } else {
                        trackFr = baseSize;
                    }
                    flexFraction = Math.max(flexFraction, trackFr);
                }

                // Phase 3: Apply flex_fraction - each track gets max(base_size, flex_factor * flex_fraction)
                for (int idx = 0; idx < frTrackIndices.size(); idx++) {
                    int i = frTrackIndices.getInt(idx);
                    float baseSize = frBaseSizes.getFloat(idx);
                    float frValue = frValues.getFloat(idx);
                    sizes.set(i, Math.max(baseSize, frValue * flexFraction));
                }
            }
        }

        // Step 1: Handle auto/content-sized tracks (min-content, max-content, auto)
        // First, calculate content-based sizes for span=1 items
        for (int i = 0; i < sizes.size(); i++) {
            if (Float.isNaN(sizes.getFloat(i))) {
                TrackSizingFunction track = (templateCols != null && i < templateCols.size())
                                            ? templateCols.get(i) : null;

                // Calculate max-content size for this track from span=1 items
                float maxContentSize = 0f;     // For base_size - respects minSize
                float pureMaxContentSize = 0f; // For growth_limit - pure content max-content, not minSize
                float minContentSize = 0f;

                for (int itemIdx = 0; itemIdx < items.size(); itemIdx++) {
                    GridItem item = items.get(itemIdx);
                    int col = getItemColumnWithCounts(item, itemIdx, numColumns, colCounts);
                    int span = item.columnSpan;

                    // Only consider items that occupy this track with span of 1
                    if (col == i && span == 1) {
                        // Horizontal percentage margins resolve to 0 in track sizing to avoid cyclic dependency
                        FloatSize marginAxisSums = item.getMarginAxisSumsWithBaselineShims(nodeInnerSize.width);
                        // Calculate minimum size from padding+border (size cannot be less than this)
                        float itemPaddingBorderWidth = (item.padding != null ? item.padding.left + item.padding.right : 0f)
                                                       + (item.border != null ? item.border.left + item.border.right : 0f);

                        if (!Float.isNaN(item.size.width)) {
                            // Size must be at least padding+border
                            float effectiveWidth = Math.max(item.size.width, itemPaddingBorderWidth);
                            float itemWidth = effectiveWidth + marginAxisSums.width;
                            maxContentSize = Math.max(maxContentSize, itemWidth);
                            pureMaxContentSize = Math.max(pureMaxContentSize, itemWidth);  // Explicit size is pure max-content
                            minContentSize = Math.max(minContentSize, itemWidth);
                        } else {
                            // Check if item has overflow: hidden/scroll/auto - affects min-content contribution
                            boolean hasOverflow = item.overflow != null &&
                                                  (item.overflow.x == Overflow.HIDDEN || item.overflow.x == Overflow.SCROLL ||
                                                   item.overflow.x == Overflow.CLIP);

                            // Get item's min-width constraint (for auto track sizing)
                            float itemMinWidthConstraint = (item.minSize != null && !Float.isNaN(item.minSize.width))
                                                           ? item.minSize.width + marginAxisSums.width
                                                           : 0f;

                            // Estimate the height available for this item based on row tracks
                            // This is crucial for flex children with column direction
                            float estimatedRowHeight = estimateItemRowHeightWithKnownSizes(item, templateRows, autoRows, numRows, nodeInnerSize.height, gap, knownRowSizes);

                            // Use cached contribution - key optimization!
                            FloatSize availSpace = new FloatSize(NaN, estimatedRowHeight);
                            item.availableSpaceCache = availSpace;

                            // Measure max-content using cache
                            // Note: cached methods already include margin
                            float itemMaxWidth = item.getMaxContentContributionWidthCached(layoutComputer, availSpace, nodeInnerSize);
                            // NOTE: max-content contribution does NOT include min-size for growth_limit purposes!
                            // min-size affects minimum_contribution (base_size), not max_content_contribution (growth_limit)
                            // We track pure max-content separately for growth_limit
                            pureMaxContentSize = Math.max(pureMaxContentSize, itemMaxWidth);  // Without min-size

                            // For base_size: max-content respects min-size
                            itemMaxWidth = Math.max(itemMaxWidth, itemMinWidthConstraint);
                            maxContentSize = Math.max(maxContentSize, itemMaxWidth);

                            // For min-content: if overflow is not visible, min-content contribution is 0 (or min-size)
                            float itemMinWidth;
                            if (hasOverflow) {
                                // Use minSize if set, otherwise 0
                                itemMinWidth = itemMinWidthConstraint;
                            } else {
                                // Measure min-content using cache
                                float measuredMinWidth = item.getMinContentContributionWidthCached(layoutComputer, availSpace, nodeInnerSize);
                                // min-content contribution should respect min-size constraint
                                itemMinWidth = Math.max(measuredMinWidth, itemMinWidthConstraint);
                            }

                            // Compressible replaced element capping (Rust: is_compressible_replaced)
                            // If preferred/max sizes are definite (including % resolved against 0), cap the content-based minimum.
                            itemMinWidth = capCompressibleReplacedMinimumContributionWidth(item, itemMinWidth, marginAxisSums.width);

                            minContentSize = Math.max(minContentSize, itemMinWidth);
                        }
                    }
                }

                // Apply the appropriate size based on track type
                float size;
                if (track != null && track.isMaxContent()) {
                    size = maxContentSize;
                } else if (track != null && track.isMinContent()) {
                    size = minContentSize;
                } else if (track != null && track.isFitContent()) {
                    // fit-content(limit) = min(max-content, max(min-content, limit))
                    // If the limit is a percentage that can't be resolved (indefinite container),
                    // fit-content behaves as max-content (unlimited growth)
                    float limit = NaN;
                    LengthPercentage fitContentArg = track.getFitContentArgument();
                    if (fitContentArg != null) {
                        if (fitContentArg.isPercent()) {
                            // For percent values, use maybeResolve - returns null if container is indefinite
                            // Check for null or NaN width (indefinite container)
                            float resolveContext = (Float.isNaN(nodeInnerSize.width)) ? NaN : nodeInnerSize.width;
                            limit = fitContentArg.maybeResolve(resolveContext);
                        } else {
                            // For absolute lengths, always resolves
                            limit = fitContentArg.resolveOrZero(nodeInnerSize.width);
                        }
                    }
                    // The growth limit is clamped by fit-content argument
                    // If limit is null (unresolvable percent), use max-content
                    if (!Float.isNaN(limit)) {
                        size = Math.min(maxContentSize, Math.max(minContentSize, limit));
                    } else {
                        size = maxContentSize;
                    }
                } else if (track != null && track.isMinmax()) {
                    // For minmax, the growth limit is determined by the max function
                    // But the base size is determined by the min function
                    TrackSizingFunction minFunc = track.getMinFunc();
                    TrackSizingFunction maxFunc = track.getMaxFunc();

                    // First, determine the min contribution (base size)
                    float minContribution = 0f;
                    if (minFunc != null && minFunc.isMinContent()) {
                        minContribution = minContentSize;
                    } else if (minFunc != null && minFunc.isMaxContent()) {
                        minContribution = maxContentSize;
                    } else if (minFunc != null && minFunc.isAuto()) {
                        minContribution = minContentSize;  // auto uses min-content as base
                    } else if (minFunc != null && minFunc.isFixed()) {
                        float fixedSize = minFunc.getFixedValue().maybeResolve(nodeInnerSize.width);
                        minContribution = !Float.isNaN(fixedSize) ? fixedSize : minContentSize;
                    }

                    // Then, determine the max limit (growth limit)
                    float maxLimit = NaN;  // null means "not resolvable, use min"
                    if (maxFunc != null && maxFunc.isMinContent()) {
                        maxLimit = minContentSize;
                    } else if (maxFunc != null && maxFunc.isMaxContent()) {
                        maxLimit = maxContentSize;
                    } else if (maxFunc != null && maxFunc.isAuto()) {
                        maxLimit = maxContentSize;
                    } else if (maxFunc != null && maxFunc.isFitContent()) {
                        // fit-content inside minmax: if percent is unresolvable, behave as max-content
                        float limit = NaN;
                        LengthPercentage fitContentArg = maxFunc.getFitContentArgument();
                        if (fitContentArg != null) {
                            if (fitContentArg.isPercent()) {
                                float resolveContext = (Float.isNaN(nodeInnerSize.width)) ? NaN : nodeInnerSize.width;
                                limit = fitContentArg.maybeResolve(resolveContext);
                            } else {
                                limit = fitContentArg.resolveOrZero(nodeInnerSize.width);
                            }
                        }
                        if (!Float.isNaN(limit)) {
                            maxLimit = Math.min(maxContentSize, Math.max(minContentSize, limit));
                        } else {
                            // Unresolvable percent - behave as max-content
                            maxLimit = maxContentSize;
                        }
                    } else if (maxFunc != null && maxFunc.isFixed()) {
                        // Use maybeResolve - returns null if percent is unresolvable
                        maxLimit = maxFunc.getFixedValue().maybeResolve(nodeInnerSize.width);  // Keep as null if unresolvable
                    }

                    // Per CSS Grid spec:
                    // - If max is resolvable: size = max(minContribution, maxLimit)
                    // - If max is NOT resolvable (e.g., percent in indefinite container):
                    //   growth_limit = infinity, so track behaves like auto with no upper limit.
                    //   Use max-content size for max-content sizing mode.
                    if (!Float.isNaN(maxLimit)) {
                        size = Math.max(minContribution, maxLimit);
                    } else {
                        // Max is not resolvable - growth_limit = infinity
                        // Per Rust: track behaves as auto with infinite growth limit
                        // For max-content sizing: use max-content size (maxContentSize)
                        // For min-content sizing: use min contribution
                        // Since we're in max-content sizing mode (indefinite container), use max-content
                        size = maxContentSize;
                        growthLimits.set(i, Float.MAX_VALUE);  // Explicitly set growth limit to infinity
                    }
                } else {
                    // Auto track: base size is min-content
                    // Growth limit depends on whether container has a definite size:
                    // 
                    // Per Rust resolve_intrinsic_track_sizes: for auto tracks, after processing span=1 items,
                    // growth_limit is updated from INFINITY to max_content_contribution.
                    // 
                    // However, when container has a DEFINITE size (nodeInnerSize.width != null):
                    // - The maximise_tracks step distributes space up to growth_limit
                    // - Then stretch_auto_tracks distributes remaining space equally to auto tracks
                    // - For stretch to work, growth_limit needs to be effectively unlimited
                    // 
                    // When container is auto-sized (nodeInnerSize.width == null):
                    // - stretch_auto_tracks uses axis_available_space_for_expansion = MaxContent (not Definite)
                    // - This means free_space = 0 in stretch_auto_tracks, so no stretching occurs
                    // - For maximise_tracks: growth_limit = max_content_contribution
                    // - So maximise_tracks can only grow tracks up to their max-content size
                    //
                    // When container has definite size (nodeInnerSize.width != null):
                    // - maximise_tracks STILL uses growth_limit (max-content) as the limit
                    // - Then stretch_auto_tracks (only when justify-content: stretch) distributes
                    //   remaining space equally among auto tracks, ignoring growth_limit
                    // 
                    // Per Rust: growth_limit is set to max_content_contribution after resolve_intrinsic_track_sizes
                    // The key is that maximise_tracks respects growth_limit, but stretch_auto_tracks does not.
                    if (!Float.isNaN(nodeInnerSize.width)) {
                        // Container has definite size: base = min-content
                        // growth_limit = max-content (for maximise_tracks limit)
                        // stretch_auto_tracks will distribute additional space if justify-content: stretch
                        size = minContentSize;
                        // Use max-content as growth limit for maximise_tracks phase
                        // pureMaxContentSize is the max-content without min-size
                        if (pureMaxContentSize > 0) {
                            growthLimits.set(i, pureMaxContentSize);
                        }
                        //else {
                            // No span=1 content, keep MAX_VALUE for span>1 distribution
                            // (already initialized to MAX_VALUE)
                        //}
                    } else if (!Float.isNaN(availableGridWidth)) {
                        // Container is auto-sized but has definite available space:
                        // Base = min-content, growth_limit = max-content (no stretch beyond max-content)
                        // This matches Rust's behavior where growth_limit is updated to max_content_contribution
                        // after resolve_intrinsic_track_sizes for span=1 items
                        size = minContentSize;
                        growthLimits.set(i, pureMaxContentSize > 0 ? pureMaxContentSize : minContentSize);
                    } else {
                        // Indefinite available space
                        // Per CSS Grid spec and Rust taffy: when grid is being sized under min-content
                        // or max-content constraint, AUTO tracks should use min-content contribution
                        // for their base size (not max-content).
                        // See Rust track_sizing.rs lines 588-607: AUTO_TAG case checks for MinContent/MaxContent
                        boolean isIntrinsicSizing = gridWidthSpace.isMinContent() || gridWidthSpace.isMaxContent();
                        if (isIntrinsicSizing) {
                            // MinContent or MaxContent sizing: use min-content for base size
                            size = minContentSize;
                            // Only set growth_limit if there are span=1 items with content
                            // Otherwise keep MAX_VALUE so span>1 items can distribute space to this track
                            if (pureMaxContentSize > 0) {
                                growthLimits.set(i, pureMaxContentSize);
                            }
                            // If pureMaxContentSize == 0 (no span=1 content), keep MAX_VALUE
                        } else {
                            // Truly indefinite: use max-content for base size
                            size = maxContentSize;
                            // Keep growth_limit at MAX_VALUE (already initialized) so span>1 items can
                            // distribute space to this track. The actual limit comes from span>1 distribution.
                        }
                    }
                }

                sizes.set(i, size);
                usedSpace += size;
            }
        }

        // Step 2: Handle spanning items (span > 1)
        // Multi-step distribution per CSS Grid spec (matching Rust taffy/Chrome/Firefox behavior)
        // Step 2.1: For intrinsic minimums - distribute min-content to all intrinsic tracks
        // Step 2.2: For content-based minimums - distribute min-content to min-content/max-content tracks
        // Step 2.3: For max-content minimums (MaxContent sizing) - distribute max-content, prioritizing max-content tracks

        boolean isMaxContentSizing = (availableGridSpace.width != null && availableGridSpace.width.isMaxContent());

        for (int itemIdx = 0; itemIdx < items.size(); itemIdx++) {
            GridItem item = items.get(itemIdx);
            int col = getItemColumnWithCounts(item, itemIdx, numColumns, colCounts);
            int span = item.columnSpan;

            if (span > 1 && col >= 0 && col + span <= numColumns) {
                // Check if item is a scroll container (overflow: hidden, scroll, clip)
                boolean isScrollContainer = item.overflow != null &&
                                            (item.overflow.x == Overflow.HIDDEN || item.overflow.x == Overflow.SCROLL ||
                                             item.overflow.x == Overflow.CLIP);

                // Estimate the height available for this item based on row tracks
                float estimatedRowHeight = estimateItemRowHeightWithKnownSizes(item, templateRows, autoRows, numRows, nodeInnerSize.height, gap, knownRowSizes);

                // Use cached contribution - key optimization!
                FloatSize availSpace = new FloatSize(NaN, estimatedRowHeight);
                item.availableSpaceCache = availSpace;

                // Horizontal percentage margins resolve to 0 in track sizing to avoid cyclic dependency
                FloatSize marginAxisSums = item.getMarginAxisSumsWithBaselineShims(nodeInnerSize.width);

                // Calculate item's contributions using cache
                // Note: cached methods already include margin
                float itemMaxContent;
                float itemMinContent;
                if (!Float.isNaN(item.size.width)) {
                    itemMaxContent = item.size.width + marginAxisSums.width;
                    itemMinContent = itemMaxContent;
                } else {
                    itemMaxContent = item.getMaxContentContributionWidthCached(layoutComputer, availSpace, nodeInnerSize);
                    itemMinContent = item.getMinContentContributionWidthCached(layoutComputer, availSpace, nodeInnerSize);
                }

                // For scroll containers, minimum contribution is 0 (unless min-size is set)
                float minimumContribution = isScrollContainer ? 0f : itemMinContent;

                // Compressible replaced element capping (Rust: is_compressible_replaced)
                minimumContribution = capCompressibleReplacedMinimumContributionWidth(item, minimumContribution, marginAxisSums.width);

                // Identify track types
                IntList intrinsicTracks = new IntArrayList();  // All intrinsic min tracks (auto, min-content, max-content)
                IntList minOrMaxContentTracks = new IntArrayList();  // min-content or max-content tracks
                IntList maxContentTracks = new IntArrayList();  // max-content tracks only
                IntList autoTracks = new IntArrayList();  // auto tracks (including implicit auto and fr)
                IntList flexTracks = new IntArrayList();  // fr tracks
                Int2FloatMap fitContentLimits = new Int2FloatOpenHashMap();  // fit-content track limits
                fitContentLimits.defaultReturnValue(NaN);
                boolean crossesFlexTrack = false;  // Whether this item crosses any fr track

                for (int c = col; c < col + span; c++) {
                    TrackSizingFunction track = (templateCols != null && c < templateCols.size())
                                                ? templateCols.get(c) : null;

                    // Get the effective min and max track sizing functions
                    // For minmax(A, B), minFunc = A, maxFunc = B
                    // For other types, they act as both min and max
                    TrackSizingFunction minFunc = (track != null && track.isMinmax()) ? track.getMinFunc() : track;
                    TrackSizingFunction maxFunc = (track != null && track.isMinmax()) ? track.getMaxFunc() : track;

                    // Track classification is based on the *min track sizing function* (and a few quirks that depend on max)
                    // See Rust: `GridTrack::{min_track_sizing_function,max_track_sizing_function}` usage.
                    boolean minIsAuto = track == null ||
                                        (minFunc != null && minFunc.isAuto()) ||
                                        (track != null && (track.isFr() || track.isFitContent()));
                    boolean minIsMinContent = (minFunc != null && minFunc.isMinContent());
                    boolean minIsMaxContent = (minFunc != null && minFunc.isMaxContent());
                    boolean minIsIntrinsic = track == null || minIsAuto || minIsMinContent || minIsMaxContent;
                    boolean maxIsMinContent = (maxFunc != null && maxFunc.isMinContent());

                    // Intrinsic min tracks: tracks where min_track_sizing_function is intrinsic
                    if (minIsIntrinsic) {
                        intrinsicTracks.add(c);
                    }
                    // min-content or max-content min tracks (not just the whole track, but min function)
                    if (minIsMinContent || minIsMaxContent) {
                        minOrMaxContentTracks.add(c);
                    }
                    // max-content min tracks only
                    if (minIsMaxContent) {
                        maxContentTracks.add(c);
                    }

                    // auto min tracks (including implicit auto and fr min functions)
                    // Rust quirk (Chrome compat): exclude minmax(auto, min-content) from the "auto" set in max-content sizing.
                    if (minIsAuto && !maxIsMinContent) {
                        autoTracks.add(c);
                    }

                    // fit-content tracks - need to track their limits (fit-content can appear as the max in minmax())
                    if (maxFunc != null && maxFunc.isFitContent()) {
                        LengthPercentage limitArg = maxFunc.getFitContentArgument();
                        float limit;
                        if (limitArg == null) {
                            limit = Float.MAX_VALUE;
                        } else if (limitArg.isPercent()) {
                            // For percent values, only resolve if we have a definite container size
                            // If container size is indefinite (null or NaN), percent fit-content acts as unlimited (INFINITY in Rust)
                            float resolveContext = (Float.isNaN(nodeInnerSize.width)) ? NaN : nodeInnerSize.width;
                            float resolved = limitArg.maybeResolve(resolveContext);
                            limit = (!Float.isNaN(resolved)) ? resolved : Float.MAX_VALUE;
                        } else {
                            // For absolute lengths, resolve directly
                            limit = limitArg.resolveOrZero(nodeInnerSize.width);
                        }
                        fitContentLimits.put(c, limit);
                    }

                    // Flexible tracks - for flex-only distribution (a track is flexible if its MAX sizing function is flexible)
                    if (maxFunc != null && maxFunc.isFlexible()) {
                        flexTracks.add(c);
                        crossesFlexTrack = true;
                    }
                }

                // Helper function to calculate current spanned size
                FloatSupplier getCurrentSpannedSize = () -> {
                    float total = 0f;
                    int gapsInSpan = span - 1;
                    for (int c = col; c < col + span; c++) {
                        total += sizes.getFloat(c);
                    }
                    return total + gapsInSpan * gap.width;
                };

                // CSS Grid spec: If the item spans a track with a flexible sizing function,
                // its contribution is distributed only to the flexible tracks.
                // This is because flexible tracks will grow to fill available space in expand_flexible_tracks.

                if (crossesFlexTrack) {
                    // Item crosses a flex track - distribute only to flex tracks
                    // Step 2.1, 2.2, 2.3 all distribute to flex tracks only
                    if (!flexTracks.isEmpty()) {
                        float currentSpannedSize = getCurrentSpannedSize.get();
                        float extraNeeded = itemMaxContent - currentSpannedSize;
                        if (extraNeeded > 0) {
                            float extraPerTrack = extraNeeded / flexTracks.size();
                            for (int c : flexTracks) {
                                sizes.set(c, sizes.getFloat(c) + extraPerTrack);
                            }
                        }
                    }
                } else {
                    // Item does not cross flex track - normal distribution

                    // Step 2.1: For intrinsic minimums
                    // Distribute minimum contribution to all intrinsic tracks
                    // For scroll containers, minimum contribution is 0
                    // For non-scroll containers, it's the min-content contribution
                    // Per CSS Grid spec: respect growth_limit when distributing space
                    if (!intrinsicTracks.isEmpty()) {
                        float currentSpannedSize = getCurrentSpannedSize.get();
                        float extraNeeded = minimumContribution - currentSpannedSize;
                        if (extraNeeded > 0) {
                            // For scroll containers, use fit_content_limited_growth_limit
                            if (isScrollContainer) {
                                distributeSpaceToTracks(extraNeeded, intrinsicTracks, sizes, fitContentLimits);
                            } else {
                                // Distribute space respecting growth_limit
                                distributeSpaceWithGrowthLimit(extraNeeded, intrinsicTracks, sizes, growthLimits);
                            }
                        }
                    }

                    // Step 2.2: For content-based minimums
                    // Continue to increase base size of min-content or max-content tracks
                    // Use min-content contribution (not minimum contribution)
                    // Per CSS Grid spec: respect growth_limit when distributing space
                    if (!minOrMaxContentTracks.isEmpty()) {
                        float currentSpannedSize = getCurrentSpannedSize.get();
                        float extraNeeded = itemMinContent - currentSpannedSize;
                        if (extraNeeded > 0) {
                            // For scroll containers, use fit_content_limited_growth_limit
                            if (isScrollContainer) {
                                distributeSpaceToTracks(extraNeeded, minOrMaxContentTracks, sizes, fitContentLimits);
                            } else {
                                // Distribute space respecting growth_limit
                                distributeSpaceWithGrowthLimit(extraNeeded, minOrMaxContentTracks, sizes, growthLimits);
                            }
                        }
                    }

                    // Step 2.3a: For max-content minimums (only when MaxContent sizing)
                    // If any max-content min tracks exist, distribute only to them
                    // Otherwise, distribute to auto min tracks (using fit_content_limited_growth_limit)
                    // Note: Per CSS Grid spec, if auto tracks hit their limits, the remaining space is NOT redistributed
                    if (isMaxContentSizing) {
                        float currentSpannedSize = getCurrentSpannedSize.get();
                        float extraNeeded = itemMaxContent - currentSpannedSize;
                        if (extraNeeded > 0) {
                            // Check if any max-content min tracks exist
                            if (!maxContentTracks.isEmpty()) {
                                // Distribute to max-content tracks respecting growth_limit
                                distributeSpaceWithGrowthLimit(extraNeeded, maxContentTracks, sizes, growthLimits);
                            } else if (!autoTracks.isEmpty()) {
                                // Distribute to auto tracks with fit_content_limited_growth_limit
                                distributeSpaceToTracks(extraNeeded, autoTracks, sizes, fitContentLimits);
                                // Note: remaining space is NOT redistributed to other tracks
                            }
                        }
                    }

                    // Step 2.3b: In all cases, continue to increase the base size of tracks with a 
                    // min track sizing function of max-content by distributing extra space as needed 
                    // to account for these items' max-content contributions.
                    // This runs regardless of whether we're in max-content sizing mode.
                    if (!maxContentTracks.isEmpty()) {
                        float currentSpannedSize = getCurrentSpannedSize.get();
                        float extraNeeded = itemMaxContent - currentSpannedSize;
                        if (extraNeeded > 0) {
                            // Distribute space respecting growth_limit
                            distributeSpaceWithGrowthLimit(extraNeeded, maxContentTracks, sizes, growthLimits);
                        }
                    }
                }
            }
        }

        // Step 4: After span>1 distribution, update growth_limits for auto tracks
        // Per CSS Grid spec: "If at this point any track's growth limit is now less than its base size,
        // increase its growth limit to match its base size."
        // This ensures growth_limit >= base_size, which is crucial for maximise_tracks to work correctly.
        // For auto tracks, also set growth_limit = base_size so that maximise_tracks doesn't over-allocate
        // (stretch_auto_tracks will handle additional stretch if justify-content is stretch)
        for (int i = 0; i < sizes.size(); i++) {
            float currentSize = sizes.getFloat(i);
            float currentLimit = growthLimits.getFloat(i);

            if (!Float.isNaN(currentSize) && !Float.isNaN(currentLimit)) {
                // If growth_limit < base_size, increase it
                if (currentLimit < currentSize) {
                    growthLimits.set(i, currentSize);
                }
                // For auto tracks with infinite growth_limit, cap it to current base_size
                // This prevents maximise_tracks from over-allocating space that should go to stretch_auto_tracks
                if (currentLimit == Float.MAX_VALUE) {
                    // Check if this is an auto track
                    TrackSizingFunction track = null;
                    int expStart = colCounts.negativeImplicit;
                    int expEnd = expStart + colCounts.explicit;
                    if (i >= expStart && i < expEnd && templateCols != null) {
                        int templateIndex = i - expStart;
                        if (templateIndex < templateCols.size()) {
                            track = templateCols.get(templateIndex);
                        }
                    }
                    // Auto track (explicit auto or implicit) should have growth_limit = base_size
                    if (track == null || track.isAuto()) {
                        growthLimits.set(i, currentSize);
                    }
                }
            }
        }

        // 11.6 Maximise Tracks
        // Per CSS Grid spec: When free space is infinite (MaxContent sizing),
        // set all tracks to their growth limit directly.
        if (availableGridSpace.width.isMaxContent()) {
            for (int i = 0; i < sizes.size(); i++) {
                float limit = growthLimits.getFloat(i);
                float currentSize = sizes.getFloat(i);
                // Set base_size to growth_limit (or stay at current if growth_limit is infinite)
                if (!Float.isNaN(limit) && limit != Float.MAX_VALUE && !Float.isNaN(currentSize)) {
                    sizes.set(i, Math.max(currentSize, limit));
                } else if (Float.isNaN(currentSize)) {
                    // null size means intrinsic track not yet resolved
                    sizes.set(i, 0f);
                }
            }
        }

        // If container has definite available space, handle overflow/underflow for auto tracks
        // This includes when nodeInnerSize is null but availableGridSpace is definite
        float effectiveAvailableWidth = !Float.isNaN(nodeInnerSize.width)
                                        ? nodeInnerSize.width - gap.width * (numColumns - 1)
                                        : (!Float.isNaN(availableGridWidth)
                                           ? availableGridWidth - gap.width * (numColumns - 1)
                                           : -1);  // -1 means indefinite

        if (effectiveAvailableWidth >= 0) {
            // Before distributing free space, initialize null-sized auto tracks to 0
            // This allows them to participate in space distribution
            for (int i = 0; i < sizes.size(); i++) {
                if (Float.isNaN(sizes.getFloat(i))) {
                    // This is an auto track (implicit or explicit auto) - initialize to 0
                    sizes.set(i, 0f);
                }
            }

            // Recalculate total after initializing null tracks
            float adjustedTotalTrackSize = 0;
            for (float s : sizes) {
                adjustedTotalTrackSize += (!Float.isNaN(s) ? s : 0);
            }

            float freeSpace = effectiveAvailableWidth - adjustedTotalTrackSize;

            // CRITICAL FIX: Per CSS Grid spec and Rust Taffy's compute_free_space behavior:
            // When the ORIGINAL availableSpace is MinContent (before clamping by maxSize),
            // free space should be 0 - tracks should not be expanded even if availableGridSpace
            // was clamped to Definite(maxSize). This ensures the grid computes its minimum content
            // size correctly rather than filling up to maxSize.
            // This check only applies when nodeInnerSize is indefinite (grid has no fixed size).
            if (originalAvailableSpace.width.isMinContent() && Float.isNaN(nodeInnerSize.width)) {
                freeSpace = 0f;
            }

            // Only grow, don't shrink below min-content
            // Auto tracks' min size is their min-content contribution, which is their base size
            // Per CSS Grid spec, when free space is negative, auto tracks don't shrink below min-content
            //
            // TWO PHASES per Rust:
            // 1. maximise_tracks: distribute space up to growth_limit (for all containers with available space)
            // 2. stretch_auto_tracks: additional stretch for auto tracks (only when container has DEFINITE SIZE)
            if (freeSpace > 0) {
                // Find tracks that can grow (auto, minmax with room to grow)
                IntList growableTrackIndices = new IntArrayList();
                int growExplicitStart = colCounts.negativeImplicit;
                int growExplicitEnd = growExplicitStart + colCounts.explicit;

                for (int i = 0; i < sizes.size(); i++) {
                    // Determine track type using colCounts
                    TrackSizingFunction track;
                    if (i >= growExplicitStart && i < growExplicitEnd) {
                        // Explicit track
                        int templateIndex = i - growExplicitStart;
                        track = (templateCols != null && templateIndex < templateCols.size())
                                ? templateCols.get(templateIndex) : null;
                    } else {
                        // Implicit track (negative or positive)
                        track = null;
                    }
                    float currentSize = sizes.getFloat(i);
                    float limit = growthLimits.getFloat(i);

                    // Track can grow if:
                    // 1. It's an auto track (explicit or implicit)
                    // 2. It's a minmax track with room to grow
                    boolean canGrow = false;
                    if (track != null && track.isAuto()) {
                        canGrow = true;
                    } else if (track == null) {
                        canGrow = true;  // Implicit auto track
                    } else if (track != null && track.isMinmax()) {
                        // minmax track - check if it has a growth limit set and room to grow
                        if (!Float.isNaN(currentSize) && !Float.isNaN(limit) && currentSize < limit) {
                            canGrow = true;
                        }
                    }

                    if (canGrow) {
                        growableTrackIndices.add(i);
                    }
                }

                if (!growableTrackIndices.isEmpty()) {
                    // PHASE 1: maximise_tracks - distribute space up to growth_limit
                    // This respects the fit_content_limited_growth_limit
                    float remainingSpace = freeSpace;
                    while (remainingSpace > 0.001f) {
                        // Find tracks that can still grow (below their growth limit)
                        IntList growableTracks = new IntArrayList();
                        for (int j = 0; j < growableTrackIndices.size(); j++) {
                            int i = growableTrackIndices.getInt(j);
                            float currentSize = sizes.getFloat(i);
                            float limit = growthLimits.getFloat(i);
                            if (!Float.isNaN(currentSize) && !Float.isNaN(limit) && currentSize < limit - 0.001f) {
                                growableTracks.add(i);
                            }
                        }

                        if (growableTracks.isEmpty()) {
                            break;
                        }

                        // Calculate how much to add to each track
                        float extra = remainingSpace / growableTracks.size();
                        float actuallyDistributed = 0f;

                        for (int j = 0; j < growableTracks.size(); j++) {
                            int i = growableTracks.getInt(j);
                            float currentSize = sizes.getFloat(i);
                            float limit = growthLimits.getFloat(i);
                            if (!Float.isNaN(currentSize) && !Float.isNaN(limit)) {
                                float maxGrow = limit - currentSize;
                                float toAdd = Math.min(extra, maxGrow);
                                sizes.set(i, currentSize + toAdd);
                                actuallyDistributed += toAdd;
                            }
                        }

                        remainingSpace -= actuallyDistributed;

                        // Break if we didn't distribute anything (to avoid infinite loop)
                        if (actuallyDistributed < 0.001f) {
                            break;
                        }
                    }

                    // PHASE 2: stretch_auto_tracks - only when:
                    // 1. Container has definite size or min-size
                    // 2. justifyContent is STRETCH (default for Grid)
                    // Per CSS Grid spec, stretch_auto_tracks only applies when justify-content/align-content is stretch
                    // 
                    // Key difference from PHASE 1: stretch_auto_tracks recalculates free space from scratch
                    // using axis_available_space_for_expansion (which is inner_node_size when definite).
                    // This is NOT the remaining space from PHASE 1 - it's a fresh calculation.
                    AlignContent justifyContent = style.justifyContent;  // AUTO/null means default = STRETCH for Grid
                    boolean shouldStretch = (justifyContent == null || justifyContent == AlignContent.AUTO || justifyContent == AlignContent.STRETCH);
                    if (!Float.isNaN(nodeInnerSize.width) && shouldStretch) {
                        // Recalculate free space for stretch (not using remainingSpace from PHASE 1)
                        float stretchUsedSpace = 0;
                        for (float s : sizes) {
                            stretchUsedSpace += (!Float.isNaN(s) ? s : 0);
                        }
                        float stretchFreeSpace = effectiveAvailableWidth - stretchUsedSpace;

                        if (stretchFreeSpace > 0.001f) {
                            // Find auto tracks only (not minmax tracks)
                            List<Integer> autoTracks = new ArrayList<>();
                            for (int i : growableTrackIndices) {
                                TrackSizingFunction track;
                                if (i >= growExplicitStart && i < growExplicitEnd) {
                                    int templateIndex = i - growExplicitStart;
                                    track = (templateCols != null && templateIndex < templateCols.size())
                                            ? templateCols.get(templateIndex) : null;
                                } else {
                                    track = null;
                                }

                                if (track == null || track.isAuto()) {
                                    autoTracks.add(i);
                                }
                            }

                            if (!autoTracks.isEmpty()) {
                                float extraPerTrack = stretchFreeSpace / autoTracks.size();
                                for (int i : autoTracks) {
                                    float currentSize = sizes.getFloat(i);
                                    if (!Float.isNaN(currentSize)) {
                                        sizes.set(i, currentSize + extraPerTrack);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // When freeSpace < 0, tracks stay at their base size (min-content for auto)
            // This means the grid can overflow its available space
        } else {
            // When available space is indefinite (effectiveAvailableWidth < 0),
            // Per CSS Grid spec and Rust's compute_free_space():
            // - MinContent: free_space = 0 -> do NOT expand tracks
            // - MaxContent: free_space = INFINITY -> expand tracks to growth_limit
            // - Truly indefinite (no constraint): treat like MaxContent
            //
            // Only expand to growth_limit when:
            // 1. MaxContent sizing mode (infinite free space)
            // 2. Truly indefinite (no MinContent/MaxContent constraint)
            // MinContent should NOT expand since free_space = 0
            boolean shouldExpandToGrowthLimit = gridWidthSpace.isMaxContent() ||
                                                (!gridWidthSpace.isMinContent() && !gridWidthSpace.isMaxContent() && !gridWidthSpace.isDefinite());
            if (shouldExpandToGrowthLimit) {
                for (int i = 0; i < sizes.size(); i++) {
                    float limit = growthLimits.getFloat(i);
                    float currentSize = sizes.getFloat(i);
                    if (!Float.isNaN(limit) && limit != Float.MAX_VALUE && !Float.isNaN(currentSize) && limit > currentSize) {
                        sizes.set(i, limit);
                    }
                }
            }
            // For MinContent: do nothing, keep base_size as-is
        }

        // Final pass: ensure no null values remain
        for (int i = 0; i < sizes.size(); i++) {
            if (Float.isNaN(sizes.getFloat(i))) {
                sizes.set(i, 0f);
            }
        }

        return sizes;
    }

    /**
     * Find the size of an fr unit for a definite container.
     * Uses the algorithm from CSS Grid spec 11.7.1.
     * If the product of the hypothetical fr size and any flexible track's flex factor
     * is less than the track's base size, restart treating that track as inflexible.
     * <p>
     * Special case: If the sum of flex factors is less than 1, multiply the free space
     * by that sum - this means tracks won't use all available space.
     */
    private float findSizeOfFrForDefinite(FloatList baseSizes, FloatList frValues, float spaceToFill) {
        if (spaceToFill <= 0 || frValues.isEmpty()) return 0f;

        // Use a copy to track which tracks are still flexible
        boolean[] isFlexible = new boolean[frValues.size()];
        for (int i = 0; i < frValues.size(); i++) {
            isFlexible[i] = true;
        }

        float flexFraction;
        boolean changed;

        // Iterate until no tracks need to be made inflexible
        do {
            changed = false;

            // Calculate hypothetical fr size from flexible tracks only
            float flexibleFrSum = 0f;
            float inflexibleSpaceUsed = 0f;

            for (int i = 0; i < frValues.size(); i++) {
                if (isFlexible[i]) {
                    flexibleFrSum += frValues.getFloat(i);
                } else {
                    // Use base size for inflexible tracks
                    inflexibleSpaceUsed += baseSizes.getFloat(i);
                }
            }

            if (flexibleFrSum <= 0) {
                return 0f;
            }

            float leftoverSpace = spaceToFill - inflexibleSpaceUsed;

            // CSS Grid spec: If the sum of flex factors is less than 1, multiply the free space
            // by that sum and use that as the hypothetical free space
            if (flexibleFrSum < 1) {
                // When fr sum < 1, each fr track gets fr * leftoverSpace, not filling all space
                flexFraction = leftoverSpace;  // 1fr = leftoverSpace in this case
            } else {
                // Normal case: divide space by fr sum
                flexFraction = leftoverSpace / flexibleFrSum;
            }

            // Check if any flexible track's hypothetical size is less than its base size
            for (int i = 0; i < frValues.size(); i++) {
                if (isFlexible[i]) {
                    float hypotheticalSize = frValues.getFloat(i) * flexFraction;
                    if (hypotheticalSize < baseSizes.getFloat(i)) {
                        // Mark as inflexible and restart
                        isFlexible[i] = false;
                        changed = true;
                    }
                }
            }
        } while (changed);

        return Math.max(0, flexFraction);
    }

    /**
     * Determine (in each axis) whether the item crosses any flexible or intrinsic tracks.
     * This is used by the re-run logic to decide whether sizing needs to be recalculated.
     */
    private void determineIfItemCrossesFlexibleOrIntrinsicTracks(
        List<GridItem> items,
        List<TrackSizingFunction> templateColumns,
        List<TrackSizingFunction> autoColumns,
        List<TrackSizingFunction> templateRows,
        List<TrackSizingFunction> autoRows,
        int numColumns,
        int numRows,
        TrackCounts colCounts,
        TrackCounts rowCounts) {

        for (int idx = 0; idx < items.size(); idx++) {
            GridItem item = items.get(idx);
            int col = getItemColumnWithCounts(item, idx, numColumns, colCounts);
            int row = getItemRowWithCounts(item, idx, numRows, rowCounts);

            // Check columns
            item.crossesFlexibleColumn = false;
            item.crossesIntrinsicColumn = false;
            for (int c = col; c < col + item.columnSpan && c < numColumns; c++) {
                TrackSizingFunction track = getTrackSizingFunctionForIndex(templateColumns, autoColumns, c, colCounts);
                if (track != null) {
                    if (track.isFr()) {
                        item.crossesFlexibleColumn = true;
                    }
                    if (track.hasIntrinsicSizingFunction()) {
                        item.crossesIntrinsicColumn = true;
                    }
                }
            }

            // Check rows
            item.crossesFlexibleRow = false;
            item.crossesIntrinsicRow = false;
            for (int r = row; r < row + item.rowSpan && r < numRows; r++) {
                TrackSizingFunction track = getTrackSizingFunctionForIndex(templateRows, autoRows, r, rowCounts);
                if (track != null) {
                    if (track.isFr()) {
                        item.crossesFlexibleRow = true;
                    }
                    if (track.hasIntrinsicSizingFunction()) {
                        item.crossesIntrinsicRow = true;
                    }
                }
            }
        }
    }

    /**
     * Get the track sizing function for a given track index.
     */
    private TrackSizingFunction getTrackSizingFunctionForIndex(
        List<TrackSizingFunction> template,
        List<TrackSizingFunction> auto,
        int trackIndex,
        TrackCounts counts) {
        // The track index is in the explicit grid range if it's >= negativeImplicit and < negativeImplicit + explicit
        int explicitStart = counts.negativeImplicit;
        int explicitEnd = explicitStart + counts.explicit;

        if (trackIndex >= explicitStart && trackIndex < explicitEnd) {
            // Explicit track
            int templateIndex = trackIndex - explicitStart;
            if (template != null && templateIndex < template.size()) {
                return template.get(templateIndex);
            }
        }

        // Implicit track - use auto sizing
        if (auto != null && !auto.isEmpty()) {
            return auto.get(0);
        }

        // Default to auto
        return TrackSizingFunction.auto();
    }

    /**
     * 11.5.1 Shim baseline-aligned items so their intrinsic size contributions reflect their baseline alignment.
     * <p>
     * This method computes the baseline and baseline_shim for each item that participates in baseline alignment.
     * Items are grouped by row, and within each row, items with align_self == BASELINE get their baselines computed.
     * The baseline_shim acts as an extra top margin during track sizing and final positioning.
     */
    private void resolveItemBaselines(List<GridItem> items, FloatSize innerNodeSize) {
        // Check if there are any baseline-aligned items
        boolean hasBaselineItem = false;
        for (GridItem item : items) {
            if (item.alignSelf == AlignItems.BASELINE) {
                hasBaselineItem = true;
                break;
            }
        }
        if (!hasBaselineItem) {
            return;
        }

        // Sort items by row start position
        List<GridItem> sortedItems = new ArrayList<>(items);
        sortedItems.sort((a, b) -> {
            int aRowStart = a.rowStart != null ? a.rowStart : 0;
            int bRowStart = b.rowStart != null ? b.rowStart : 0;
            return Integer.compare(aRowStart, bRowStart);
        });

        // Iterate over rows
        int idx = 0;
        while (idx < sortedItems.size()) {
            int currentRow = sortedItems.get(idx).rowStart != null ? sortedItems.get(idx).rowStart : 0;

            // Find all items in the current row
            List<GridItem> rowItems = new ArrayList<>();
            while (idx < sortedItems.size()) {
                int rowStart = sortedItems.get(idx).rowStart != null ? sortedItems.get(idx).rowStart : 0;
                if (rowStart != currentRow) break;
                rowItems.add(sortedItems.get(idx));
                idx++;
            }

            // Count how many items in this row are baseline aligned
            int baselineItemCount = 0;
            for (GridItem item : rowItems) {
                if (item.alignSelf == AlignItems.BASELINE) {
                    baselineItemCount++;
                }
            }

            // If a row has one or zero items participating in baseline alignment then skip
            if (baselineItemCount <= 1) {
                continue;
            }

            // Compute the baselines of all items in the row (not just baseline-aligned ones)
            // This is needed to calculate the max baseline for the row
            for (GridItem item : rowItems) {
                // Measure the item to get its baseline
                LayoutOutput output = layoutComputer.performChildLayout(
                    item.nodeId,
                    new FloatSize(NaN, NaN),
                    innerNodeSize,
                    new TaffySize<>(AvailableSpace.minContent(), AvailableSpace.minContent()),
                    SizingMode.INHERENT_SIZE,
                    new TaffyLine<>(false, false)
                );

                float baselineValue = output.firstBaselines() != null ? output.firstBaselines().y : NaN;
                float height = output.size().height;
                float marginTop = item.margin != null ? item.margin.top : 0f;

                // baseline = first_baseline.y or height, plus margin.top
                item.baseline = (!Float.isNaN(baselineValue) ? baselineValue : height) + marginTop;
            }

            // Compute the max baseline of all items in the row
            float rowMaxBaseline = 0f;
            for (GridItem item : rowItems) {
                if (!Float.isNaN(item.baseline)) {
                    rowMaxBaseline = Math.max(rowMaxBaseline, item.baseline);
                }
            }

            // Compute the baseline shim for each item in the row
            for (GridItem item : rowItems) {
                if (!Float.isNaN(item.baseline)) {
                    item.baselineShim = rowMaxBaseline - item.baseline;
                }
            }
        }
    }

    private FloatList calculateRowSizes(
        TaffyStyle style,
        FloatSize nodeInnerSize,
        TaffySize<AvailableSpace> availableGridSpace,
        FloatSize gap,
        int numRows,
        List<GridItem> items,
        FloatList columnSizes,
        TrackCounts colCounts,
        TrackCounts rowCounts,
        List<TrackSizingFunction> expandedRows) {

        FloatList sizes = new FloatArrayList();

        // Pre-compute item row indices ONCE (optimization)
        int numColumns = columnSizes.size();
        int[] itemRows = new int[items.size()];
        for (int i = 0; i < items.size(); i++) {
            itemRows[i] = getItemRowWithCounts(items.get(i), i, numColumns, rowCounts);
        }

        // Build row-to-items index map for O(1) lookup instead of O(items) scanning per row
        // This reduces calculateAutoRowHeight from O(items*rows) to O(items+rows)
        IntList[] itemIndicesByRow = new IntList[numRows];
        for (int i = 0; i < numRows; i++) {
            itemIndicesByRow[i] = new IntArrayList();
        }
        for (int i = 0; i < items.size(); i++) {
            int row = itemRows[i];
            if (row >= 0 && row < numRows) {
                itemIndicesByRow[row].add(i);
            }
        }

        // Use available grid space when node inner size is not definite
        AvailableSpace gridHeightSpace = availableGridSpace.height;
        float availableGridHeight = gridHeightSpace.intoOption();

        float availableHeight = !Float.isNaN(nodeInnerSize.height)
                                ? nodeInnerSize.height - gap.height * (numRows - 1)
                                : (!Float.isNaN(availableGridHeight)
                                   ? availableGridHeight - gap.height * (numRows - 1)
                                   : 0);

        float totalFr = 0f;
        float usedSpace = 0;
        int autoCount = 0;

        // Get auto rows for implicit tracks
        List<TrackSizingFunction> autoRows = style.getGridAutoRows();
        int autoRowCount = (autoRows != null && !autoRows.isEmpty()) ? autoRows.size() : 0;
        int explicitRowCount = (expandedRows != null) ? expandedRows.size() : 0;

        // Use rowCounts to determine which tracks are explicit vs implicit
        int negativeImplicitCount = rowCounts.negativeImplicit;
        int explicitStart = negativeImplicitCount;
        int explicitEnd = explicitStart + explicitRowCount;

        // First pass: resolve fixed sizes and count fr units
        for (int i = 0; i < numRows; i++) {
            // Determine the track sizing function for this row
            TrackSizingFunction track;
            if (i >= explicitStart && i < explicitEnd && expandedRows != null) {
                // Explicit track from grid-template-rows
                int templateIndex = i - explicitStart;
                track = expandedRows.get(templateIndex);
            } else if (autoRowCount > 0) {
                // Implicit track from grid-auto-rows (cycles through)
                // For negative implicit tracks (i < explicitStart): cycle in reverse order
                // For positive implicit tracks (i >= explicitEnd): cycle in forward order
                int implicitIndex;
                if (i < explicitStart) {
                    // Negative implicit: count backwards from explicit start
                    int distanceFromExplicit = explicitStart - i;
                    implicitIndex = (autoRowCount - (distanceFromExplicit % autoRowCount)) % autoRowCount;
                } else {
                    // Positive implicit: cycle forward from index 0
                    int distanceFromExplicit = i - explicitEnd;
                    implicitIndex = distanceFromExplicit % autoRowCount;
                }
                track = autoRows.get(implicitIndex);
            } else {
                // No auto rows defined, default to auto
                track = null;
            }

            if (track != null && track.isFr()) {
                totalFr += track.getFrValue();
                sizes.add(NaN);
            } else if (track == null || track.isAuto() || track.isMinContent() || track.isMaxContent()) {
                // Auto or content-based track - will be resolved later
                autoCount++;
                sizes.add(NaN);
            } else if (track.isMinmax()) {
                // For minmax, check if max is flexible
                TrackSizingFunction maxF = track.getMaxFunc();
                if (maxF != null && maxF.isFr()) {
                    totalFr += maxF.getFrValue();
                    sizes.add(NaN);
                } else if (maxF != null && maxF.isFixed()) {
                    // Use the max size as the track size for now (simplified)
                    float size = maxF.getFixedValue().resolveOrZero(nodeInnerSize.height);
                    sizes.add(size);
                    usedSpace += size;
                } else {
                    // Auto-like minmax - treat as auto
                    autoCount++;
                    sizes.add(NaN);
                }
            } else if (track.isFitContent()) {
                // fit-content tracks are like auto but with a max limit
                autoCount++;
                sizes.add(NaN);
            } else {
                // Fixed track (length or percentage)
                // If percentage and container height is indefinite, treat as auto per CSS Grid spec
                if (track.isFixed() && track.getFixedValue() != null &&
                    track.getFixedValue().isPercent() && Float.isNaN(nodeInnerSize.height)) {
                    // Percentage track with indefinite container - treat as auto
                    autoCount++;
                    sizes.add(NaN);
                } else {
                    float size = resolveTrackSize(track, nodeInnerSize.height);
                    sizes.add(size);
                    usedSpace += size;
                }
            }
        }

        // Second pass: handle fr and auto tracks
        // Per CSS Grid spec, fr units are equivalent to minmax(auto, Nfr)
        // This means the track's minimum size is determined by its content (auto),
        // and then it may grow larger based on the fr distribution.


        if (totalFr > 0) {
            if (!Float.isNaN(nodeInnerSize.height)) {
                // Definite container - first calculate content-based sizes for fr tracks
                // (implementing the "auto" part of minmax(auto, Nfr))
                FloatList contentBasedSizes = new FloatArrayList();
                for (int i = 0; i < sizes.size(); i++) {
                    if (Float.isNaN(sizes.getFloat(i)) && expandedRows != null && i < expandedRows.size() && expandedRows.get(i).isFr()) {
                        float contentHeight = calculateAutoRowHeight(items, columnSizes, nodeInnerSize, colCounts, gap.width, itemIndicesByRow[i]);
                        contentBasedSizes.add(contentHeight);
                    } else {
                        contentBasedSizes.add(NaN);
                    }
                }

                // Calculate fr sizes using an iterative approach similar to Rust's find_size_of_fr
                // This handles the case where some tracks' content exceeds their fr-based allocation
                float availableForFr = availableHeight - usedSpace;
                float currentTotalFr = totalFr;
                float usedByInflexible = 0f;
                List<Boolean> isInflexible = new ArrayList<>();
                for (int i = 0; i < sizes.size(); i++) {
                    isInflexible.add(false);
                }

                // Iterative algorithm: tracks with content > fr*fraction become inflexible
                boolean changed = true;
                while (changed) {
                    changed = false;
                    float remainingForFr = availableForFr - usedByInflexible;
                    float flexFraction = currentTotalFr > 0 ? remainingForFr / currentTotalFr : 0f;

                    for (int i = 0; i < sizes.size(); i++) {
                        if (Float.isNaN(sizes.getFloat(i)) && expandedRows != null && i < expandedRows.size() && expandedRows.get(i).isFr()) {
                            if (!isInflexible.get(i)) {
                                float frValue = expandedRows.get(i).getFrValue();
                                float frBasedSize = frValue * flexFraction;
                                float contentSize = contentBasedSizes.getFloat(i);

                                // If content exceeds fr-based size, this track becomes inflexible
                                if (!Float.isNaN(contentSize) && contentSize > frBasedSize) {
                                    isInflexible.set(i, true);
                                    usedByInflexible += contentSize;
                                    currentTotalFr -= frValue;
                                    changed = true;
                                }
                            }
                        }
                    }
                }

                // Now set final sizes
                float remainingForFr = availableForFr - usedByInflexible;
                float flexFraction = currentTotalFr > 0 ? remainingForFr / currentTotalFr : 0f;

                for (int i = 0; i < sizes.size(); i++) {
                    if (Float.isNaN(sizes.getFloat(i))) {
                        if (expandedRows != null && i < expandedRows.size() && expandedRows.get(i).isFr()) {
                            if (isInflexible.get(i)) {
                                // Use content size for inflexible tracks
                                sizes.set(i, contentBasedSizes.getFloat(i));
                            } else {
                                // Use fr-based size for flexible tracks
                                float frValue = expandedRows.get(i).getFrValue();
                                float frBasedSize = frValue * flexFraction;
                                float contentSize = contentBasedSizes.getFloat(i);
                                // Final size = max(content, fr-based) per CSS Grid spec
                                sizes.set(i, !Float.isNaN(contentSize) ? Math.max(contentSize, frBasedSize) : frBasedSize);
                            }
                        } else {
                            float maxRowHeight = calculateAutoRowHeight(items, columnSizes, nodeInnerSize, colCounts, gap.width, itemIndicesByRow[i]);
                            sizes.set(i, maxRowHeight);
                        }
                    }
                }
            } else {
                // Indefinite container (MaxContent) - fr tracks without content get 0
                // Calculate flex fraction based on item content (similar to columns)
                float flexFraction = 0f;

                // Pre-compute fr track info ONCE outside the item loop (optimization)
                boolean[] isFrRow = new boolean[numRows];
                float[] frRowValue = new float[numRows];
                for (int r = 0; r < numRows; r++) {
                    if (expandedRows != null && r < expandedRows.size() && expandedRows.get(r).isFr()) {
                        isFrRow[r] = true;
                        frRowValue[r] = expandedRows.get(r).getFrValue();
                    }
                }

                // Check items that cross flexible rows
                for (int itemIdx = 0; itemIdx < items.size(); itemIdx++) {
                    GridItem item = items.get(itemIdx);
                    int row = getItemRowWithCounts(item, itemIdx, columnSizes.size(), rowCounts);
                    int span = item.rowSpan;

                    // Check if this item crosses any fr tracks (O(1) lookup now)
                    boolean crossesFr = false;
                    float itemFrSum = 0f;

                    for (int r = row; r < row + span && r < numRows; r++) {
                        if (isFrRow[r]) {
                            crossesFr = true;
                            itemFrSum += frRowValue[r];
                        }
                    }

                    if (crossesFr && !Float.isNaN(item.size.height)) {
                        // Item has explicit height
                        // Calculate flex fraction needed
                        float usedByNonFr = 0f;
                        for (int r = row; r < row + span && r < numRows; r++) {
                            if (!isFrRow[r]) {
                                float value = sizes.getFloat(r);
                                usedByNonFr += !Float.isNaN(value) ? value : 0f;
                            }
                        }
                        float leftover = item.size.height - usedByNonFr;
                        if (leftover > 0 && itemFrSum > 0) {
                            float itemFr = leftover / itemFrSum;
                            flexFraction = Math.max(flexFraction, itemFr);
                        }
                    } else if (crossesFr) {
                        // Measure content using cache
                        int col = getItemColumnWithCounts(item, itemIdx, columnSizes.size(), colCounts);
                        float colWidth = col < columnSizes.size() ? columnSizes.getFloat(col) : 0f;

                        // Use cached contribution - key optimization!
                        // Note: cached methods already include margin
                        FloatSize availSpace = new FloatSize(colWidth, NaN);
                        item.availableSpaceCache = availSpace;
                        float contentHeight = item.getMaxContentContributionHeightCached(layoutComputer, availSpace, nodeInnerSize);

                        float usedByNonFr = 0f;
                        for (int r = row; r < row + span && r < numRows; r++) {
                            if (!isFrRow[r]) {
                                float value = sizes.getFloat(r);
                                usedByNonFr += !Float.isNaN(value) ? value : 0f;
                            }
                        }
                        float leftover = contentHeight - usedByNonFr;
                        if (leftover > 0 && itemFrSum > 0) {
                            float itemFr = leftover / itemFrSum;
                            flexFraction = Math.max(flexFraction, itemFr);
                        }
                    }
                }

                // Apply flex fraction to fr tracks
                for (int i = 0; i < sizes.size(); i++) {
                    if (Float.isNaN(sizes.getFloat(i))) {
                        if (expandedRows != null && i < expandedRows.size() && expandedRows.get(i).isFr()) {
                            sizes.set(i, expandedRows.get(i).getFrValue() * flexFraction);
                        } else {
                            // Auto row - size based on content
                            float maxRowHeight = calculateAutoRowHeight(items, columnSizes, nodeInnerSize, colCounts, gap.width, itemIndicesByRow[i]);
                            sizes.set(i, maxRowHeight);
                        }
                    }
                }
            }
        } else if (autoCount > 0 && !Float.isNaN(nodeInnerSize.height)) {
            // No fr units but have auto tracks with definite container size
            // 11.8 Stretch auto Tracks: expand auto tracks to fill remaining space

            // Step 1: Calculate content size for all auto tracks
            FloatList autoContentSizes = new FloatArrayList();
            float autoContentTotal = 0f;
            for (int i = 0; i < sizes.size(); i++) {
                if (Float.isNaN(sizes.getFloat(i))) {
                    float maxRowHeight = calculateAutoRowHeight(items, columnSizes, nodeInnerSize, colCounts, gap.width, itemIndicesByRow[i]);
                    autoContentSizes.add(maxRowHeight);
                    autoContentTotal += maxRowHeight;
                } else {
                    autoContentSizes.add(0f); // Placeholder for non-auto tracks
                }
            }

            // Step 2: Calculate actual remaining space after content sizes
            float totalUsed = usedSpace + autoContentTotal;
            float freeSpace = availableHeight - totalUsed;

            // Step 3: Distribute free space equally among auto tracks (only if positive)
            float extraPerAutoTrack = freeSpace > 0 ? freeSpace / autoCount : 0f;

            // Step 4: Set auto track sizes = content size + extra space
            for (int i = 0; i < sizes.size(); i++) {
                if (Float.isNaN(sizes.getFloat(i))) {
                    float contentSize = autoContentSizes.getFloat(i);
                    sizes.set(i, contentSize + extraPerAutoTrack);
                }
            }
        } else if (autoCount > 0) {
            // No fr units and indefinite container - calculate based on content
            for (int i = 0; i < sizes.size(); i++) {
                if (Float.isNaN(sizes.getFloat(i))) {
                    float maxRowHeight = calculateAutoRowHeight(items, columnSizes, nodeInnerSize, colCounts, gap.width, itemIndicesByRow[i]);
                    sizes.set(i, maxRowHeight);
                }
            }
        }

        // Final pass: ensure no null values remain (fallback for edge cases)
        for (int i = 0; i < sizes.size(); i++) {
            if (Float.isNaN(sizes.getFloat(i))) {
                // Calculate auto row height as fallback
                float maxRowHeight = calculateAutoRowHeight(items, columnSizes, nodeInnerSize, colCounts, gap.width, itemIndicesByRow[i]);
                sizes.set(i, maxRowHeight); // Use actual content height, can be 0 for empty rows
            }
        }

        return sizes;
    }

    private float calculateAutoRowHeight(List<GridItem> items, FloatList columnSizes, FloatSize nodeInnerSize, TrackCounts colCounts, float columnGap, IntList rowItemIndices) {
        float maxRowHeight = 0;

        // Only iterate over items that belong to this row (O(items_in_row) instead of O(all_items))
        for (int idx = 0; idx < rowItemIndices.size(); idx++) {
            int i = rowItemIndices.getInt(idx);
            GridItem item = items.get(i);
            float height;

                // Calculate minimum size from padding+border (size cannot be less than this)
                float itemPaddingBorderHeight = (item.padding != null ? item.padding.top + item.padding.bottom : 0f)
                                                + (item.border != null ? item.border.top + item.border.bottom : 0f);

                // Get margin axis sums using the Rust logic:
                // - Horizontal percentage margins resolve to 0 to avoid cyclic dependency
                // - Vertical percentage margins resolve against inner_node_width
                FloatSize marginAxisSums = item.getMarginAxisSumsWithBaselineShims(nodeInnerSize.width);

                // Get grid area size for this item (track size, not including margins)
                int col = getItemColumnWithCounts(item, i, columnSizes.size(), colCounts);
                float gridAreaWidth = 0f;
                int endCol = Math.min(col + item.columnSpan, columnSizes.size());
                for (int c = col; c < endCol; c++) {
                    gridAreaWidth += columnSizes.getFloat(c);
                }
                // Add gaps between spanned columns
                int numGaps = Math.max(0, endCol - col - 1);
                gridAreaWidth += numGaps * columnGap;

                FloatSize gridAreaSize = new FloatSize(gridAreaWidth, NaN);

                // Compute known dimensions using the proper method that respects item's percentage sizes
                FloatSize itemKnownDimensions = computeItemKnownDimensions(item, gridAreaSize, nodeInnerSize);

                // If item has explicit height (from its size or computed from aspect ratio)
                if (!Float.isNaN(itemKnownDimensions.height)) {
                    float effectiveHeight = Math.max(itemKnownDimensions.height, itemPaddingBorderHeight);
                    height = effectiveHeight + marginAxisSums.height;
                } else {
                    // Check if item is a scroll container (overflow: scroll, hidden, or clip)
                    // Per CSS Grid spec, scroll containers have automatic minimum size of 0
                    boolean isScrollContainer = item.overflow != null &&
                                                (item.overflow.y == Overflow.HIDDEN || item.overflow.y == Overflow.SCROLL ||
                                                 item.overflow.y == Overflow.CLIP);

                    // For scroll containers, we use automatic minimum size of 0 ONLY when
                    // the grid container has a definite height (the scroll container will be stretched).
                    // When sizing under max-content constraint (nodeInnerSize.height == null),
                    // we need to measure the actual content.
                    if (isScrollContainer && !Float.isNaN(nodeInnerSize.height)) {
                        // Automatic minimum size for scroll containers is 0
                        // (will be stretched to fill available space later)
                        height = marginAxisSums.height;
                    } else {
                        // Otherwise measure the content using cache
                        // IMPORTANT: Pass gridAreaWidth (grid area size), not itemWidth (which is already minus margins)
                        // The computeKnownDimensions inside will subtract margins again if needed

                        // Use cached contribution - key optimization!
                        // Note: cached methods already include margin
                        FloatSize availSpace = new FloatSize(gridAreaWidth, NaN);
                        item.availableSpaceCache = availSpace;
                        height = item.getMaxContentContributionHeightCached(layoutComputer, availSpace, nodeInnerSize);
                    }
                }

                maxRowHeight = Math.max(maxRowHeight, height);
        }

        return maxRowHeight;
    }

    private void placeItems(
        List<GridItem> items,
        FloatList columnSizes,
        FloatList rowSizes,
        FloatSize gap,
        FloatRect contentBoxInset,
        FloatSize nodeInnerSize,
        TaffyStyle containerStyle,
        TrackCounts colCounts,
        TrackCounts rowCounts,
        boolean isRtl) {

        int numColumns = columnSizes.size();
        int numRows = rowSizes.size();

        // Count non-collapsed tracks (size > 0) for alignment calculations
        // Collapsed tracks (from auto-fit) don't participate in content alignment
        int nonCollapsedColumns = 0;
        for (float colSize : columnSizes) {
            if (colSize > 0) nonCollapsedColumns++;
        }
        int nonCollapsedRows = 0;
        for (float rowSize : rowSizes) {
            if (rowSize > 0) nonCollapsedRows++;
        }

        // Default alignment from container
        AlignItems defaultAlignItems = containerStyle.getAlignItems();
        AlignItems defaultJustifyItems = containerStyle.justifyItems;

        // Calculate total track sizes (only non-collapsed tracks contribute to used space)
        float totalColumnSize = 0;
        for (float colSize : columnSizes) {
            totalColumnSize += colSize;
        }
        // Only count gaps between non-collapsed tracks
        totalColumnSize += gap.width * Math.max(0, nonCollapsedColumns - 1);

        float totalRowSize = 0;
        for (float rowSize : rowSizes) {
            totalRowSize += rowSize;
        }
        totalRowSize += gap.height * Math.max(0, nonCollapsedRows - 1);

        // Calculate free space for content alignment (can be negative)
        float innerWidth = !Float.isNaN(nodeInnerSize.width) ? nodeInnerSize.width : totalColumnSize;
        float innerHeight = !Float.isNaN(nodeInnerSize.height) ? nodeInnerSize.height : totalRowSize;
        float freeSpaceWidth = innerWidth - totalColumnSize;
        float freeSpaceHeight = innerHeight - totalRowSize;

        // Calculate content alignment offsets (use non-collapsed track counts)
        float contentOffsetX = calculateContentAlignmentOffset(containerStyle.getJustifyContent(), freeSpaceWidth, nonCollapsedColumns);
        float contentOffsetY = calculateContentAlignmentOffset(containerStyle.getAlignContent(), freeSpaceHeight, nonCollapsedRows);

        // In RTL, 'start' and 'end' for justify-content are flipped along the inline axis.
        // For RTL, we need to adjust contentOffsetX:
        // - START/FLEX_START: items start from right edge, contentOffsetX = 0 (no initial offset from right)
        // - END/FLEX_END: items end at left edge, contentOffsetX = freeSpaceWidth (offset from right)
        // Note: The RTL coordinate transformation in the trackX calculation handles the mirroring.
        if (isRtl) {
            JustifyContent justifyContent = containerStyle.getJustifyContent();
            if (justifyContent == null || justifyContent == JustifyContent.START || justifyContent == JustifyContent.FLEX_START) {
                // For RTL START: items align to inline-start (right edge), no offset
                contentOffsetX = 0;
            } else if (justifyContent == JustifyContent.END || justifyContent == JustifyContent.FLEX_END) {
                // For RTL END: items align to inline-end (left edge), offset by free space
                contentOffsetX = freeSpaceWidth;
            }
            // CENTER, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY remain symmetric
        }

        // Calculate gap adjustments for space-between, space-around, space-evenly (use non-collapsed track counts)
        float adjustedGapX = calculateAdjustedGap(containerStyle.getJustifyContent(), freeSpaceWidth, nonCollapsedColumns, gap.width);
        float adjustedGapY = calculateAdjustedGap(containerStyle.getAlignContent(), freeSpaceHeight, nonCollapsedRows, gap.height);

        for (int i = 0; i < items.size(); i++) {
            GridItem item = items.get(i);

            int col = getItemColumnWithCounts(item, i, numColumns, colCounts);
            int row = getItemRowWithCounts(item, i, numColumns, rowCounts);

            // Calculate span - number of tracks item covers
            int colSpan = item.columnSpan;
            int rowSpan = item.rowSpan;

            // Clamp to available tracks
            if (col + colSpan > numColumns) colSpan = numColumns - col;
            if (row + rowSpan > numRows) rowSpan = numRows - row;
            if (colSpan < 1) colSpan = 1;
            if (rowSpan < 1) rowSpan = 1;

            // Track size (available space for item) - sum of spanned tracks plus gaps between them
            // Only add gaps between non-collapsed tracks
            float trackWidth = 0;
            boolean firstNonCollapsedInSpan = true;
            for (int c = 0; c < colSpan; c++) {
                if (col + c < numColumns) {
                    float colSize = columnSizes.getFloat(col + c);
                    if (colSize > 0) {
                        if (!firstNonCollapsedInSpan) {
                            trackWidth += adjustedGapX;
                        }
                        trackWidth += colSize;
                        firstNonCollapsedInSpan = false;
                    }
                }
            }

            // Get track position with content alignment offset
            // Only add gap after non-collapsed tracks
            // For RTL, column indices are logical (counting from inline-start), so we mirror the x-coordinate.
            float trackX;
            if (isRtl) {
                // Offset of this track area from inline-start (which is the right edge in RTL)
                float startOffset = 0;
                boolean prevColNonCollapsed = false;
                for (int c = 0; c < col; c++) {
                    float colSize = columnSizes.getFloat(c);
                    boolean isNonCollapsed = colSize > 0;
                    if (isNonCollapsed && prevColNonCollapsed) {
                        startOffset += adjustedGapX;
                    }
                    startOffset += colSize;
                    if (isNonCollapsed) {
                        prevColNonCollapsed = true;
                    }
                }
                if (col < numColumns && columnSizes.getFloat(col) > 0 && prevColNonCollapsed) {
                    startOffset += adjustedGapX;
                }

                // Mirror into left-origin coordinates
                // For RTL, contentOffsetX represents offset from inline-end (left edge),
                // so we subtract it from the right side calculation
                trackX = contentBoxInset.left + (innerWidth - contentOffsetX - startOffset - trackWidth);
            } else {
                // LTR: Calculate from left edge (original logic)
                trackX = contentBoxInset.left + contentOffsetX;
                boolean prevColNonCollapsed = false;
                for (int c = 0; c < col; c++) {
                    float colSize = columnSizes.getFloat(c);
                    boolean isNonCollapsed = colSize > 0;
                    // Add gap before this track if both previous and current are non-collapsed
                    if (isNonCollapsed && prevColNonCollapsed) {
                        trackX += adjustedGapX;
                    }
                    trackX += colSize;
                    if (isNonCollapsed) {
                        prevColNonCollapsed = true;
                    }
                }
                // Add initial offset if the item's column is non-collapsed and there were previous non-collapsed columns
                if (col < numColumns && columnSizes.getFloat(col) > 0 && prevColNonCollapsed) {
                    trackX += adjustedGapX;
                }
            }

            float trackY = contentBoxInset.top + contentOffsetY;
            boolean prevRowNonCollapsed = false;
            for (int r = 0; r < row; r++) {
                float rowSize = rowSizes.getFloat(r);
                boolean isNonCollapsed = rowSize > 0;
                if (isNonCollapsed && prevRowNonCollapsed) {
                    trackY += adjustedGapY;
                }
                trackY += rowSize;
                if (isNonCollapsed) {
                    prevRowNonCollapsed = true;
                }
            }
            if (row < numRows && rowSizes.getFloat(row) > 0 && prevRowNonCollapsed) {
                trackY += adjustedGapY;
            }

            float trackHeight = 0;
            firstNonCollapsedInSpan = true;
            for (int r = 0; r < rowSpan; r++) {
                if (row + r < numRows) {
                    float rowSize = rowSizes.getFloat(row + r);
                    if (rowSize > 0) {
                        if (!firstNonCollapsedInSpan) {
                            trackHeight += adjustedGapY;
                        }
                        trackHeight += rowSize;
                        firstNonCollapsedInSpan = false;
                    }
                }
            }

            // Re-resolve margin with track width (percentage margins are relative to grid area width)
            TaffyTree tree = layoutComputer.getTree();
            TaffyStyle childStyle = tree.getStyle(item.nodeId);
            TaffyRect<LengthPercentageAuto> marginStyle = childStyle.getMargin();
            FloatRect margin = Resolve.resolveRectLpaOrZero(marginStyle, trackWidth);

            // Check if margins are auto (affects stretch behavior)
            boolean hasHorizontalAutoMargin = marginStyle.left.isAuto() || marginStyle.right.isAuto();
            boolean hasVerticalAutoMargin = marginStyle.top.isAuto() || marginStyle.bottom.isAuto();

            // Determine alignment following CSS Grid spec (alignment.rs):
            // Re-read raw alignment from style (not the merged value from item)
            // 1. Child's explicit alignment (alignSelf/justifySelf)
            // 2. Container's alignment (alignItems/justifyItems) - passed as defaultAlignItems/defaultJustifyItems
            // 3. If both are null, apply special rules based on aspectRatio/explicit size
            AlignItems childAlignSelf = childStyle.getAlignSelf();
            AlignItems childJustifySelf = childStyle.justifySelf;

            AlignItems alignY = childAlignSelf;
            if (alignY == null || alignY == AlignItems.AUTO) {
                alignY = defaultAlignItems;
            }
            if (alignY == null || alignY == AlignItems.AUTO) {
                // Per CSS Grid spec: If height is set OR aspect-ratio is set, default is START, otherwise STRETCH
                alignY = (!Float.isNaN(item.size.height) || (item.aspectRatio != null && !Float.isNaN(item.aspectRatio))) ? AlignItems.START : AlignItems.STRETCH;
            }

            AlignItems alignX = childJustifySelf;
            if (alignX == null || alignX == AlignItems.AUTO) {
                alignX = defaultJustifyItems;
            }
            if (alignX == null || alignX == AlignItems.AUTO) {
                // Per CSS Grid spec: If width is set, default is START, otherwise STRETCH
                alignX = (!Float.isNaN(item.size.width)) ? AlignItems.START : AlignItems.STRETCH;
            }

            // Per CSS Grid spec: auto margins prevent stretch
            // When margin is auto, the item should use its content size and auto margins absorb free space
            boolean stretchWidth = alignX == AlignItems.STRETCH && !hasHorizontalAutoMargin;
            boolean stretchHeight = alignY == AlignItems.STRETCH && !hasVerticalAutoMargin;

            // Available space after margin
            float availableWidth = trackWidth - margin.left - margin.right;
            float availableHeight = trackHeight - margin.top - margin.bottom;

            // Calculate minimum size from padding+border (size cannot be less than this)
            float itemPaddingBorderWidth = (item.padding != null ? item.padding.left + item.padding.right : 0f)
                                           + (item.border != null ? item.border.left + item.border.right : 0f);
            float itemPaddingBorderHeight = (item.padding != null ? item.padding.top + item.padding.bottom : 0f)
                                            + (item.border != null ? item.border.top + item.border.bottom : 0f);

            // Calculate size based on alignment
            float width, height;

            if (!Float.isNaN(item.size.width)) {
                // Explicit size (but must be at least padding+border)
                width = Math.max(item.size.width, itemPaddingBorderWidth);
            } else if (stretchWidth) {
                // Stretch to fill
                width = availableWidth;
            } else {
                // Content-based size (START, CENTER, END, or has auto margin) - measure child
                LayoutOutput output = layoutComputer.performChildLayout(
                    item.nodeId,
                    new FloatSize(NaN, NaN),
                    nodeInnerSize,
                    new TaffySize<>(AvailableSpace.definite(availableWidth), AvailableSpace.definite(availableHeight)),
                    SizingMode.INHERENT_SIZE,
                    new TaffyLine<>(false, false)
                );
                width = output.size().width;
            }

            // Reapply aspect ratio after width is determined
            // If aspect_ratio is set and height is not explicitly set, calculate height from width
            float heightFromAspectRatio = NaN;
            if (item.aspectRatio != null && !Float.isNaN(item.aspectRatio) && Float.isNaN(item.size.height)) {
                heightFromAspectRatio = width / item.aspectRatio;
            }

            if (!Float.isNaN(item.size.height)) {
                // Explicit size (but must be at least padding+border)
                height = Math.max(item.size.height, itemPaddingBorderHeight);
            } else if (!Float.isNaN(heightFromAspectRatio)) {
                // Height from aspect ratio
                height = heightFromAspectRatio;
            } else if (stretchHeight) {
                // Stretch to fill
                height = availableHeight;
            } else {
                // Content-based size (START, CENTER, END, or has auto margin) - measure child with known width
                LayoutOutput output = layoutComputer.performChildLayout(
                    item.nodeId,
                    new FloatSize(width, NaN),
                    nodeInnerSize,
                    new TaffySize<>(AvailableSpace.definite(width), AvailableSpace.definite(availableHeight)),
                    SizingMode.INHERENT_SIZE,
                    new TaffyLine<>(false, false)
                );
                height = output.size().height;
            }

            // Reapply aspect ratio after height is determined (for cases where width is not set but height is)
            if (item.aspectRatio != null && !Float.isNaN(item.aspectRatio) && Float.isNaN(item.size.width) && !Float.isNaN(item.size.height)) {
                width = height * item.aspectRatio;
            }

            // Apply min/max constraints
            width = TaffyMath.clamp(width, item.minSize.width, item.maxSize.width);
            height = TaffyMath.clamp(height, item.minSize.height, item.maxSize.height);

            // Calculate auto margin values - auto margins absorb free space
            float freeSpaceX = availableWidth - width;
            float freeSpaceY = availableHeight - height;
            int horizontalAutoMarginCount = (marginStyle.left.isAuto() ? 1 : 0) + (marginStyle.right.isAuto() ? 1 : 0);
            int verticalAutoMarginCount = (marginStyle.top.isAuto() ? 1 : 0) + (marginStyle.bottom.isAuto() ? 1 : 0);
            float horizontalAutoMarginSize = horizontalAutoMarginCount > 0 ? Math.max(0, freeSpaceX) / horizontalAutoMarginCount : 0;
            float verticalAutoMarginSize = verticalAutoMarginCount > 0 ? Math.max(0, freeSpaceY) / verticalAutoMarginCount : 0;

            // Resolve final margins (auto margins get the computed value)
            float marginLeft = marginStyle.left.isAuto() ? horizontalAutoMarginSize : margin.left;
            float marginRight = marginStyle.right.isAuto() ? horizontalAutoMarginSize : margin.right;
            float marginTop = marginStyle.top.isAuto() ? verticalAutoMarginSize : margin.top;
            float marginBottom = marginStyle.bottom.isAuto() ? verticalAutoMarginSize : margin.bottom;

            // Calculate position based on alignment
            float x = trackX + marginLeft;
            float y = trackY + marginTop;

            // Adjust x based on justify (horizontal alignment) - only if no auto margins
            if (!hasHorizontalAutoMargin) {
                // In RTL, START/END are swapped: START = right (inline-start), END = left (inline-end)
                // Since trackX is already the left edge of the track in screen coordinates,
                // we need to adjust the alignment behavior for RTL:
                // - START/FLEX_START in RTL: item aligns to right edge of track = x += freeSpaceX
                // - END/FLEX_END in RTL: item aligns to left edge of track = x += 0
                // - CENTER: x += freeSpaceX / 2 (symmetric)
                if (isRtl) {
                    switch (alignX) {
                        case CENTER:
                            x += freeSpaceX / 2;
                            break;
                        case START:
                        case FLEX_START:
                            // In RTL, START is the right edge of the track
                            x += freeSpaceX;
                            break;
                        case END:
                        case FLEX_END:
                        case STRETCH:
                        default:
                            // In RTL, END is the left edge of the track (where trackX points)
                            // x stays at start (trackX + marginLeft)
                            break;
                    }
                } else {
                    // LTR: original behavior
                    switch (alignX) {
                        case CENTER:
                            x += freeSpaceX / 2;
                            break;
                        case END:
                        case FLEX_END:
                            x += freeSpaceX;
                            break;
                        case START:
                        case FLEX_START:
                        case STRETCH:
                        default:
                            // x stays at start
                            break;
                    }
                }
            }

            // Adjust y based on align (vertical alignment) - only if no auto margins
            if (!hasVerticalAutoMargin) {
                switch (alignY) {
                    case CENTER:
                        y += freeSpaceY / 2;
                        break;
                    case END:
                    case FLEX_END:
                        y += freeSpaceY;
                        break;
                    case BASELINE:
                        // For baseline alignment, add the baseline shim which acts as extra top margin
                        y += item.baselineShim;
                        break;
                    case START:
                    case FLEX_START:
                    case STRETCH:
                    default:
                        // y stays at start
                        break;
                }
            }

            item.location = new FloatPoint(x, y);

            // Apply relative position inset offset
            // For position: relative, inset values offset the item from its natural position
            if (item.position == TaffyPosition.RELATIVE && item.inset != null) {
                // Horizontal inset: use left, or -right if left is not set
                float insetLeft = item.inset.left.maybeResolve(trackWidth);
                float insetRight = item.inset.right.maybeResolve(trackWidth);
                float insetX = 0f;
                if (!Float.isNaN(insetLeft)) {
                    insetX = insetLeft;
                } else if (!Float.isNaN(insetRight)) {
                    insetX = -insetRight;
                }

                // Vertical inset: use top, or -bottom if top is not set
                float insetTop = item.inset.top.maybeResolve(trackHeight);
                float insetBottom = item.inset.bottom.maybeResolve(trackHeight);
                float insetY = 0f;
                if (!Float.isNaN(insetTop)) {
                    insetY = insetTop;
                } else if (!Float.isNaN(insetBottom)) {
                    insetY = -insetBottom;
                }

                item.location = new FloatPoint(x + insetX, y + insetY);
            }

            item.computedSize = new FloatSize(width, height);
            // Update item margin with resolved values for final layout
            item.margin = new FloatRect(marginLeft, marginRight, marginTop, marginBottom);

            // Perform final child layout with known dimensions to trigger recursive layout
            layoutComputer.performChildLayout(
                item.nodeId,
                new FloatSize(width, height),  // known dimensions
                new FloatSize(trackWidth, trackHeight),  // parent inner size
                new TaffySize<>(AvailableSpace.definite(width), AvailableSpace.definite(height)),
                SizingMode.INHERENT_SIZE,
                new TaffyLine<>(false, false)
            );
        }
    }

    private void performFinalLayout(List<GridItem> items) {
        TaffyTree tree = layoutComputer.getTree();

        for (GridItem item : items) {
            FloatSize scrollbarSize = new FloatSize(
                item.overflow.y == Overflow.SCROLL ? item.scrollbarWidth : 0f,
                item.overflow.x == Overflow.SCROLL ? item.scrollbarWidth : 0f
            );

            Layout layout = new Layout(
                item.order,
                item.location,
                item.computedSize,
                item.computedSize,
                scrollbarSize,
                item.border,
                item.padding,
                item.margin
            );

            tree.setUnroundedLayout(item.nodeId, layout);
        }
    }

    private void layoutAbsoluteChildren(
        NodeId node,
        FloatSize containerSize,
        FloatRect border,
        float scrollbarGutterX,
        float scrollbarGutterY,
        TrackCounts colCounts,
        TrackCounts rowCounts,
        FloatList columnOffsets,
        FloatList rowOffsets,
        boolean isRtl) {
        TaffyTree tree = layoutComputer.getTree();

        for (NodeId childId : tree.getChildren(node)) {
            TaffyStyle childStyle = tree.getStyle(childId);
            if (childStyle.getPosition() != TaffyPosition.ABSOLUTE) continue;
            if (childStyle.getBoxGenerationMode() == BoxGenerationMode.NONE) continue;

            // Resolve grid placement for absolute positioned items
            TaffyLine<GridPlacement> gridCol = childStyle.gridColumn;
            TaffyLine<GridPlacement> gridRow = childStyle.gridRow;

            // Calculate grid area based on grid placement
            FloatRect gridArea = resolveAbsoluteGridArea(
                gridCol, gridRow,
                colCounts, rowCounts,
                columnOffsets, rowOffsets,
                containerSize, border,
                scrollbarGutterX, scrollbarGutterY
            );

            float areaWidth = gridArea.right - gridArea.left;
            float areaHeight = gridArea.bottom - gridArea.top;

            TaffyRect<LengthPercentageAuto> insetStyle = childStyle.getInset();
            float left = insetStyle.left.maybeResolve(areaWidth);
            float right = insetStyle.right.maybeResolve(areaWidth);
            float top = insetStyle.top.maybeResolve(areaHeight);
            float bottom = insetStyle.bottom.maybeResolve(areaHeight);

            FloatRect margin = Resolve.resolveRectLpaOrZero(childStyle.getMargin(), areaWidth);
            FloatRect itemPadding = Resolve.resolveRectOrZero(childStyle.getPadding(), areaWidth);
            FloatRect itemBorder = Resolve.resolveRectOrZero(childStyle.getBorder(), areaWidth);

            FloatSize paddingBorderSum = new FloatSize(
                itemPadding.left + itemPadding.right + itemBorder.left + itemBorder.right,
                itemPadding.top + itemPadding.bottom + itemBorder.top + itemBorder.bottom
            );

            Float aspectRatio = childStyle.getAspectRatio();
            FloatSize boxSizingAdj = childStyle.getBoxSizing() == BoxSizing.CONTENT_BOX
                                     ? paddingBorderSum
                                     : FloatSize.ZERO;

            FloatSize styleSize = maybeAdd(maybeApplyAspectRatio(
                Resolve.maybeResolveSize(childStyle.getSize(), new FloatSize(areaWidth, areaHeight)),
                aspectRatio), boxSizingAdj);
            FloatSize minSz = maybeMax(maybeAdd(maybeApplyAspectRatio(
                Resolve.maybeResolveSize(childStyle.getMinSize(), new FloatSize(areaWidth, areaHeight)),
                aspectRatio), boxSizingAdj), paddingBorderSum);
            FloatSize maxSz = maybeAdd(maybeApplyAspectRatio(
                Resolve.maybeResolveSize(childStyle.getMaxSize(), new FloatSize(areaWidth, areaHeight)),
                aspectRatio), boxSizingAdj);

            FloatSize knownDimensions = maybeClamp(styleSize, minSz, maxSz);

            // Size from inset (when both left/right or top/bottom are set)
            // Per CSS spec, width from inset is applied first
            if (Float.isNaN(knownDimensions.width) && !Float.isNaN(left) && !Float.isNaN(right)) {
                knownDimensions = new FloatSize(
                    Math.max(0, areaWidth - left - right - margin.left - margin.right),
                    knownDimensions.height
                );
            }

            // Reapply aspect ratio after width is determined from inset
            // If aspect_ratio is set, and we now have width but no height, calculate height from width
            if (aspectRatio != null && !Float.isNaN(aspectRatio) && !Float.isNaN(knownDimensions.width) && Float.isNaN(knownDimensions.height)) {
                knownDimensions = new FloatSize(
                    knownDimensions.width,
                    knownDimensions.width / aspectRatio
                );
            }

            // Only apply height from inset if height is still not determined (after aspect_ratio)
            if (Float.isNaN(knownDimensions.height) && !Float.isNaN(top) && !Float.isNaN(bottom)) {
                knownDimensions = new FloatSize(
                    knownDimensions.width,
                    Math.max(0, areaHeight - top - bottom - margin.top - margin.bottom)
                );
            }

            // Reapply aspect ratio after height is determined from inset (for width calculation)
            if (aspectRatio != null && !Float.isNaN(aspectRatio) && !Float.isNaN(knownDimensions.height) && Float.isNaN(knownDimensions.width)) {
                knownDimensions = new FloatSize(
                    knownDimensions.height * aspectRatio,
                    knownDimensions.height
                );
            }

            LayoutOutput output = layoutComputer.performChildLayout(
                childId,
                knownDimensions,
                new FloatSize(areaWidth, areaHeight),
                new TaffySize<>(AvailableSpace.definite(areaWidth), AvailableSpace.definite(areaHeight)),
                SizingMode.INHERENT_SIZE,
                new TaffyLine<>(false, false)
            );

            FloatSize finalSize = maybeClamp(
                new FloatSize(
                    !Float.isNaN(knownDimensions.width) ? knownDimensions.width : output.size().width,
                    !Float.isNaN(knownDimensions.height) ? knownDimensions.height : output.size().height
                ),
                minSz, maxSz
            );

            // Get alignment styles
            AlignItems justifySelf = childStyle.justifySelf;
            AlignItems alignSelf = childStyle.getAlignSelf();

            // For RTL, we need to flip the grid area horizontally within the container
            // grid-column: 1 in RTL means the rightmost column
            FloatRect effectiveGridArea = gridArea;
            if (isRtl) {
                // In RTL, flip the left/right of the grid area
                // gridArea.left becomes containerSize.width - gridArea.right
                // gridArea.right becomes containerSize.width - gridArea.left
                float newLeft = containerSize.width - gridArea.right;
                float newRight = containerSize.width - gridArea.left;
                effectiveGridArea = new FloatRect(newLeft, newRight, gridArea.top, gridArea.bottom);
            }
            float effectiveAreaWidth = effectiveGridArea.right - effectiveGridArea.left;

            // Compute offset in horizontal axis
            float xInArea;
            if (!Float.isNaN(left)) {
                xInArea = left + margin.left;
            } else if (!Float.isNaN(right)) {
                xInArea = effectiveAreaWidth - right - finalSize.width - margin.right;
            } else {
                // Use alignment-based offset
                float freeSpaceX = effectiveAreaWidth - finalSize.width - margin.left - margin.right;
                if (justifySelf == AlignItems.END || justifySelf == AlignItems.FLEX_END) {
                    xInArea = freeSpaceX + margin.left;
                } else if (justifySelf == AlignItems.CENTER) {
                    xInArea = freeSpaceX / 2 + margin.left;
                } else {
                    // START, FLEX_START, STRETCH, BASELINE - all start at margin.left
                    xInArea = margin.left;
                }
            }

            // Compute offset in vertical axis
            float yInArea;
            if (!Float.isNaN(top)) {
                yInArea = top + margin.top;
            } else if (!Float.isNaN(bottom)) {
                yInArea = areaHeight - bottom - finalSize.height - margin.bottom;
            } else {
                // Use alignment-based offset
                float freeSpaceY = areaHeight - finalSize.height - margin.top - margin.bottom;
                if (alignSelf == AlignItems.END || alignSelf == AlignItems.FLEX_END) {
                    yInArea = freeSpaceY + margin.top;
                } else if (alignSelf == AlignItems.CENTER) {
                    yInArea = freeSpaceY / 2 + margin.top;
                } else {
                    // START, FLEX_START, STRETCH, BASELINE - all start at margin.top
                    yInArea = margin.top;
                }
            }

            // Convert to container coordinates
            float x = effectiveGridArea.left + xInArea;
            float y = gridArea.top + yInArea;

            TaffyPoint<Overflow> overflow = childStyle.getOverflow();
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
                margin
            );

            tree.setUnroundedLayout(childId, layout);
        }
    }

    /**
     * Resolve the grid area for an absolutely positioned grid item.
     * Per CSS Grid spec, if a placement is auto or refers to a non-existent line,
     * the item is positioned against the grid container's padding edge.
     */
    private FloatRect resolveAbsoluteGridArea(
        TaffyLine<GridPlacement> gridCol,
        TaffyLine<GridPlacement> gridRow,
        TrackCounts colCounts,
        TrackCounts rowCounts,
        FloatList columnOffsets,
        FloatList rowOffsets,
        FloatSize containerSize,
        FloatRect border,
        float scrollbarGutterX,
        float scrollbarGutterY) {

        // Resolve column placement
        Integer colStart = resolveAbsoluteGridLine(gridCol.start, gridCol.end, colCounts, true);
        Integer colEnd = resolveAbsoluteGridLine(gridCol.end, gridCol.start, colCounts, false);

        // Resolve row placement
        Integer rowStart = resolveAbsoluteGridLine(gridRow.start, gridRow.end, rowCounts, true);
        Integer rowEnd = resolveAbsoluteGridLine(gridRow.end, gridRow.start, rowCounts, false);

        // Get offset values, falling back to border edges
        float left = colStart != null && colStart >= 0 && colStart < columnOffsets.size()
                     ? columnOffsets.getFloat(colStart)
                     : border.left;
        float right = colEnd != null && colEnd >= 0 && colEnd < columnOffsets.size()
                      ? columnOffsets.getFloat(colEnd)
                      : containerSize.width - border.right - scrollbarGutterX;
        float top = rowStart != null && rowStart >= 0 && rowStart < rowOffsets.size()
                    ? rowOffsets.getFloat(rowStart)
                    : border.top;
        float bottom = rowEnd != null && rowEnd >= 0 && rowEnd < rowOffsets.size()
                       ? rowOffsets.getFloat(rowEnd)
                       : containerSize.height - border.bottom - scrollbarGutterY;

        return new FloatRect(left, right, top, bottom);
    }

    /**
     * Resolve a grid line for absolute positioning.
     * Returns the track index, or null if the line should default to the container edge.
     */
    private Integer resolveAbsoluteGridLine(
        GridPlacement placement,
        GridPlacement otherPlacement,
        TrackCounts trackCounts,
        boolean isStart) {

        if (placement == null || placement.isAuto()) {
            // Auto placement - use container edge
            // But if other is a line and this is start, we could compute from span
            // For now, just return null (use container edge)
            return null;
        }

        if (placement.isLine()) {
            int lineNum = placement.getValue();
            // Convert to origin-zero (CSS uses 1-based lines)
            int originZero;
            if (lineNum > 0) {
                originZero = lineNum - 1;
            } else if (lineNum < 0) {
                // Negative line: -1 is last line = explicitTrackCount
                originZero = trackCounts.explicit + 1 + lineNum;
            } else {
                return null; // line(0) is invalid
            }

            // If the other placement is also a line, resolve ordering and the "equal lines" quirk.
            if (otherPlacement != null && otherPlacement.isLine()) {
                int otherLineNum = otherPlacement.getValue();
                if (otherLineNum == 0) {
                    return null;
                }
                int otherOriginZero = otherLineNum > 0
                                     ? otherLineNum - 1
                                     : trackCounts.explicit + 1 + otherLineNum;

                // Per Rust resolve_absolutely_positioned_grid_tracks:
                // - if equal: end = start + 1
                // - else: start=min, end=max
                if (originZero == otherOriginZero) {
                    if (!isStart) {
                        originZero = originZero + 1;
                    }
                } else {
                    originZero = isStart ? Math.min(originZero, otherOriginZero) : Math.max(originZero, otherOriginZero);
                }
            }

            // Check bounds
            int minOz = -trackCounts.negativeImplicit;
            int maxOz = trackCounts.explicit + trackCounts.positiveImplicit;
            if (originZero < minOz || originZero > maxOz) {
                return null; // Out of bounds, treat as auto for absolutely positioned items
            }

            // Map OriginZero line to index in our offsets vector (which includes implicit tracks)
            return originZero + trackCounts.negativeImplicit;
        }

        if (placement.isSpan()) {
            // Span combined with a line on the other side
            if (otherPlacement != null && otherPlacement.isLine()) {
                int otherLine = otherPlacement.getValue();
                if (otherLine == 0) {
                    return null;
                }
                int otherOriginZero = otherLine > 0 ? otherLine - 1 : trackCounts.explicit + 1 + otherLine;
                int span = placement.getValue();

                if (isStart) {
                    int originZero = otherOriginZero - span;
                    int minOz = -trackCounts.negativeImplicit;
                    int maxOz = trackCounts.explicit + trackCounts.positiveImplicit;
                    if (originZero < minOz || originZero > maxOz) return null;
                    return originZero + trackCounts.negativeImplicit;
                } else {
                    int originZero = otherOriginZero + span;
                    int minOz = -trackCounts.negativeImplicit;
                    int maxOz = trackCounts.explicit + trackCounts.positiveImplicit;
                    if (originZero < minOz || originZero > maxOz) return null;
                    return originZero + trackCounts.negativeImplicit;
                }
            }
            // Span without a definite line on the other side - use container edge
            return null;
        }

        return null;
    }

    /**
     * Helper method to distribute space to tracks while respecting fit-content limits.
     * Returns the remaining space that couldn't be distributed.
     */
    private float distributeSpaceToTracks(
        float spaceToDistribute,
        IntList targetTracks,
        FloatList sizes,
        Int2FloatMap fitContentLimits) {

        float remaining = spaceToDistribute;

        while (remaining > 0.001f) {
            // Find tracks that can still grow (not at fit-content limit)
            IntList growableTracks = new IntArrayList();
            for (int c : targetTracks) {
                float limit = fitContentLimits.get(c);
                if (Float.isNaN(limit) || sizes.getFloat(c) < limit - 0.001f) {
                    growableTracks.add(c);
                }
            }

            if (growableTracks.isEmpty()) {
                break;  // All tracks at limit
            }

            float extraPerTrack = remaining / growableTracks.size();
            float distributed = 0f;

            for (int c : growableTracks) {
                float limit = fitContentLimits.get(c);
                float currentSize = sizes.getFloat(c);
                float increase = extraPerTrack;

                if (!Float.isNaN(limit)) {
                    // Limit growth for fit-content tracks
                    float maxIncrease = limit - currentSize;
                    increase = Math.min(increase, Math.max(0f, maxIncrease));
                }

                sizes.set(c, currentSize + increase);
                distributed += increase;
            }

            remaining -= distributed;

            // Prevent infinite loop if we can't distribute anything
            if (distributed < 0.001f) {
                break;
            }
        }

        return remaining;
    }

    /**
     * Distribute space to tracks while respecting growth limits.
     * This follows the CSS Grid algorithm where space is distributed only to tracks
     * that haven't reached their growth limit yet.
     * Returns the remaining space that couldn't be distributed.
     */
    private float distributeSpaceWithGrowthLimit(
        float spaceToDistribute,
        IntList targetTracks,
        FloatList sizes,
        FloatList growthLimits) {

        float remaining = spaceToDistribute;

        while (remaining > 0.001f) {
            // Find tracks that can still grow (not at growth limit)
            IntList growableTracks = new IntArrayList();
            for (int c : targetTracks) {
                float currentSize = Float.isNaN(sizes.getFloat(c)) ? 0f : sizes.getFloat(c);
                float limit = Float.isNaN(growthLimits.getFloat(c)) ? Float.MAX_VALUE : growthLimits.getFloat(c);

                // Track can grow if it hasn't reached its limit
                if (currentSize < limit - 0.001f) {
                    growableTracks.add(c);
                }
            }

            if (growableTracks.isEmpty()) {
                break;  // All tracks at limit
            }

            float extraPerTrack = remaining / growableTracks.size();
            float distributed = 0f;

            for (int c : growableTracks) {
                float currentSize = !Float.isNaN(sizes.getFloat(c)) ? sizes.getFloat(c) : 0f;
                float limit = !Float.isNaN(growthLimits.getFloat(c)) ? growthLimits.getFloat(c) : Float.MAX_VALUE;

                // Calculate maximum possible increase
                float maxIncrease = limit - currentSize;
                float increase = Math.min(extraPerTrack, Math.max(0f, maxIncrease));

                sizes.set(c, currentSize + increase);
                distributed += increase;
            }

            remaining -= distributed;

            // Prevent infinite loop if we can't distribute anything
            if (distributed < 0.001f) {
                break;
            }
        }

        return remaining;
    }

    /**
     * Estimate the min-content contribution of a track based on span=1 items.
     * This is used when we need to know a track's size before it has been formally computed.
     */
    private float estimateTrackMinContent(int trackIndex, List<GridItem> items, int numColumns, FloatSize nodeInnerSize, TrackCounts colCounts) {
        float minContent = 0f;

        for (int itemIdx = 0; itemIdx < items.size(); itemIdx++) {
            GridItem item = items.get(itemIdx);
            int col = getItemColumnWithCounts(item, itemIdx, numColumns, colCounts);
            int span = item.columnSpan;

            // Only consider span=1 items in this track
            if (col == trackIndex && span == 1) {
                // Horizontal percentage margins resolve to 0 in track sizing to avoid cyclic dependency
                FloatSize marginAxisSums = item.getMarginAxisSumsWithBaselineShims(nodeInnerSize.width);
                float itemMinWidth;

                // Check item's min-size constraint first
                float itemMinWidthConstraint = item.minSize != null && !Float.isNaN(item.minSize.width)
                                               ? item.minSize.width + marginAxisSums.width
                                               : 0f;

                if (!Float.isNaN(item.size.width)) {
                    itemMinWidth = item.size.width + marginAxisSums.width;
                } else {
                    // Measure min-content
                    LayoutOutput minOutput = layoutComputer.performChildLayout(
                        item.nodeId,
                        new FloatSize(NaN, NaN),
                        nodeInnerSize,
                        new TaffySize<>(AvailableSpace.minContent(), AvailableSpace.maxContent()),
                        SizingMode.INHERENT_SIZE,
                        new TaffyLine<>(false, false)
                    );
                    itemMinWidth = minOutput.size().width + marginAxisSums.width;
                }

                // Apply min-size constraint
                itemMinWidth = Math.max(itemMinWidth, itemMinWidthConstraint);
                minContent = Math.max(minContent, itemMinWidth);
            }
        }

        return minContent;
    }

    // Helper methods

    /**
     * Get the matrix index for an item's column, using TrackCounts for coordinate conversion.
     */
    private int getItemColumnWithCounts(GridItem item, int index, int numColumns, TrackCounts colCounts) {
        if (item.columnStart != null) {
            return colCounts.ozLineToNextTrack(item.columnStart);
        }
        return index % numColumns;
    }

    /**
     * Get the matrix index for an item's row, using TrackCounts for coordinate conversion.
     */
    private int getItemRowWithCounts(GridItem item, int index, int numColumns, TrackCounts rowCounts) {
        if (item.rowStart != null) {
            return rowCounts.ozLineToNextTrack(item.rowStart);
        }
        return index / numColumns;
    }

    private float resolveTrackSize(TrackSizingFunction track, float availableSize) {
        if (track.isMinContent() || track.isMaxContent()) {
            return 0; // Will be resolved based on content
        }
        if (track.isAuto()) {
            return 0; // Will be resolved based on content
        }
        if (track.isFixed()) {
            LengthPercentage lp = track.getFixedValue();
            if (lp != null) {
                return lp.resolveOrZero(availableSize);
            }
        }
        return 0;
    }

    private static FloatSize maybeApplyAspectRatio(FloatSize size, Float aspectRatio) {
        if (aspectRatio == null || Float.isNaN(aspectRatio)) return size;
        if (!Float.isNaN(size.width) && Float.isNaN(size.height)) {
            return new FloatSize(size.width, size.width / aspectRatio);
        }
        if (Float.isNaN(size.width) && !Float.isNaN(size.height)) {
            return new FloatSize(size.height * aspectRatio, size.height);
        }
        return size;
    }

    private static FloatSize maybeAdd(FloatSize size, FloatSize addition) {
        return new FloatSize(
            TaffyMath.maybeAdd(size.width, addition.width),
            TaffyMath.maybeAdd(size.height, addition.height)
        );
    }

    private static FloatSize maybeClamp(FloatSize size, FloatSize min, FloatSize max) {
        return new FloatSize(
            TaffyMath.maybeClamp(size.width, min.width, max.width),
            TaffyMath.maybeClamp(size.height, min.height, max.height)
        );
    }

    /**
     * Clamp an AvailableSpace value by min and max size constraints.
     * Matches Rust's maybe_clamp(min_size, max_size).maybe_max(padding_border_size) behavior.
     * <p>
     * If the available space is definite, it gets clamped. If it's min/max-content,
     * we check if max_size is set - if so, we convert to definite(max_size) since
     * that constrains the intrinsic sizing.
     * <p>
     * Note: This differs from Rust's maybe_clamp which preserves MinContent/MaxContent.
     * However, Rust passes min_size and max_size separately to track_sizing_algorithm,
     * where they are used for flexible track expansion and stretch. Our Java implementation
     * uses a simpler approach where availableGridSpace includes the max_size constraint.
     */
    private static AvailableSpace clampAvailableSpace(
        AvailableSpace availSpace,
        float minSize,
        float maxSize,
        float paddingBorderSize) {
        if (availSpace.isDefinite()) {
            // Definite available space - clamp it
            float value = availSpace.getValue();
            if (!Float.isNaN(minSize)) {
                value = Math.max(value, minSize);
            }
            if (!Float.isNaN(maxSize)) {
                value = Math.min(value, maxSize);
            }
            // Apply padding_border_size minimum
            if (!Float.isNaN(paddingBorderSize)) {
                value = Math.max(value, paddingBorderSize);
            }
            return AvailableSpace.definite(value);
        } else {
            // Min/Max-content sizing
            // If max-size is set, convert to definite to constrain intrinsic sizing
            if (!Float.isNaN(maxSize)) {
                float value = maxSize;
                // Apply padding_border_size minimum
                if (!Float.isNaN(paddingBorderSize)) {
                    value = Math.max(value, paddingBorderSize);
                }
                return AvailableSpace.definite(value);
            }
            // Otherwise keep as min/max-content
            return availSpace;
        }
    }

    private static FloatSize maybeMax(FloatSize size, FloatSize min) {
        return new FloatSize(
            TaffyMath.maybeMax(size.width, min.width),
            TaffyMath.maybeMax(size.height, min.height)
        );
    }


    private static FloatSize orChain(FloatSize... sizes) {
        float width = NaN;
        float height = NaN;
        for (FloatSize size : sizes) {
            if (Float.isNaN(width) && !Float.isNaN(size.width)) width = size.width;
            if (Float.isNaN(height) && !Float.isNaN(size.height)) height = size.height;
            if (!Float.isNaN(width) && !Float.isNaN(height)) break;
        }
        return new FloatSize(width, height);
    }

    /**
     * Compare two nullable Float values for equality with tolerance.
     */
    private static boolean floatEquals(float a, float b) {
        if (Float.isNaN(a) && Float.isNaN(b)) return true;
        if (Float.isNaN(a) || Float.isNaN(b)) return false;
        return Math.abs(a - b) < 0.001f;
    }

    /**
     * Calculate the initial offset for content alignment (align-content/justify-content)
     */
    private float calculateContentAlignmentOffset(AlignContent alignment, float freeSpace, int numTracks) {
        if (alignment == null || numTracks <= 0) {
            return 0;
        }

        // For negative space, only CENTER and END can have non-zero offset
        return switch (alignment) {
            case CENTER -> freeSpace / 2;  // Works for both positive and negative space
            case END, FLEX_END -> freeSpace;  // Works for both positive and negative space
            case SPACE_AROUND -> {
                // Only apply when positive space
                if (freeSpace > 0) {
                    yield freeSpace / (numTracks * 2);
                }
                yield 0;
            }
            case SPACE_EVENLY -> {
                if (freeSpace > 0) {
                    yield freeSpace / (numTracks + 1);
                }
                yield 0;
            }
            default -> 0;
        };
    }

    /**
     * Calculate the initial offset for justify-content
     */
    private float calculateContentAlignmentOffset(JustifyContent alignment, float freeSpace, int numTracks) {
        if (alignment == null || numTracks <= 0) {
            return 0;
        }

        return switch (alignment) {
            case CENTER -> freeSpace / 2;
            case END, FLEX_END -> freeSpace;
            case SPACE_AROUND -> {
                if (freeSpace > 0) {
                    yield freeSpace / (numTracks * 2);
                }
                yield 0;
            }
            case SPACE_EVENLY -> {
                if (freeSpace > 0) {
                    yield freeSpace / (numTracks + 1);
                }
                yield 0;
            }
            // STRETCH, START, FLEX_START, SPACE_BETWEEN: offset is 0 (content starts at the beginning)
            // Per CSS Grid spec: stretch distributes space to tracks, not to gutters
            // SPACE_BETWEEN also has offset 0 (first item at start, last at end)
            case STRETCH, START, FLEX_START, SPACE_BETWEEN -> 0;
        };
    }

    /**
     * Calculate adjusted gap for space-between, space-around, space-evenly
     */
    private float calculateAdjustedGap(AlignContent alignment, float freeSpace, int numTracks, float originalGap) {
        if (alignment == null || freeSpace <= 0 || numTracks <= 1) {
            return originalGap;
        }

        return switch (alignment) {
            case SPACE_BETWEEN ->
                // All free space goes between tracks
                originalGap + freeSpace / (numTracks - 1);
            case SPACE_AROUND ->
                // Each track gets freeSpace/(numTracks*2) on each side
                // Gap between = original + 2 * (freeSpace / (numTracks*2)) = original + freeSpace/numTracks
                originalGap + freeSpace / numTracks;
            case SPACE_EVENLY ->
                // Equal space between all
                originalGap + freeSpace / (numTracks + 1);
            default -> originalGap;
        };
    }

    /**
     * Calculate adjusted gap for justify-content
     */
    private float calculateAdjustedGap(JustifyContent alignment, float freeSpace, int numTracks, float originalGap) {
        if (alignment == null || freeSpace <= 0 || numTracks <= 1) {
            return originalGap;
        }

        return switch (alignment) {
            case SPACE_BETWEEN -> originalGap + freeSpace / (numTracks - 1);
            case SPACE_AROUND -> originalGap + freeSpace / numTracks;
            case SPACE_EVENLY -> originalGap + freeSpace / (numTracks + 1);
            // STRETCH, START, END, etc: no gap adjustment (space is distributed to tracks, not gutters)
            case STRETCH, START, END, FLEX_START, FLEX_END, CENTER -> originalGap;
        };
    }

    /**
     * Get the definite value of a track sizing function.
     * Returns the fixed size if track is a fixed length or resolvable percentage.
     * Returns null for intrinsic sizing functions (auto, min-content, max-content, fr).
     */
    private float getTrackDefiniteValue(TrackSizingFunction track, float parentSize) {
        if (track == null) {
            return NaN;
        }
        if (track.isFixed()) {
            return track.getFixedValue().maybeResolve(parentSize);
        }
        if (track.isMinmax()) {
            // For minmax, use the max function's definite value if available
            TrackSizingFunction maxFunc = track.getMaxFunc();
            if (maxFunc != null && maxFunc.isFixed()) {
                return maxFunc.getFixedValue().maybeResolve(parentSize);
            }
        }
        // For auto, min-content, max-content, fr - return null
        return NaN;
    }

    /**
     * Estimate the height available for an item based on the row tracks it spans.
     * If knownRowSizes is provided, uses the actual track sizes instead of template definitions.
     * This is used for re-running column sizing after row sizing is complete.
     */
    private float estimateItemRowHeightWithKnownSizes(GridItem item, List<TrackSizingFunction> templateRows,
                                                      List<TrackSizingFunction> autoRows, int numRows, float parentHeight, FloatSize gap,
                                                      FloatList knownRowSizes) {
        int row = item.rowStart;
        int rowSpan = item.rowSpan;

        if (row < 0) {
            return NaN;  // Item not yet placed
        }

        // If we have known row sizes, use them directly
        if (knownRowSizes != null && !knownRowSizes.isEmpty()) {
            float totalHeight = 0f;
            for (int r = row; r < row + rowSpan && r < knownRowSizes.size(); r++) {
                totalHeight += knownRowSizes.getFloat(r);
            }
            // Add gaps between rows
            if (rowSpan > 1 && gap != null && !Float.isNaN(gap.height)) {
                totalHeight += gap.height * (rowSpan - 1);
            }
            return totalHeight;
        }

        int autoRowCount = (autoRows != null && !autoRows.isEmpty()) ? autoRows.size() : 0;
        int explicitRowCount = (templateRows != null) ? templateRows.size() : 0;

        float totalHeight = 0f;
        boolean allDefinite = true;

        for (int r = row; r < row + rowSpan && r < numRows; r++) {
            TrackSizingFunction track;
            if (r < explicitRowCount && templateRows != null) {
                track = templateRows.get(r);
            } else if (autoRowCount > 0) {
                int implicitIndex = r - explicitRowCount;
                track = autoRows.get(implicitIndex % autoRowCount);
            } else {
                track = null;  // Default to auto
            }

            float definiteValue = getTrackDefiniteValue(track, parentHeight);
            if (!Float.isNaN(definiteValue)) {
                totalHeight += definiteValue;
            } else {
                allDefinite = false;
                break;
            }
        }

        if (allDefinite) {
            // Add gaps between rows
            if (rowSpan > 1 && gap != null) {
                totalHeight += gap.height * (rowSpan - 1);
            }
            return totalHeight;
        }
        return NaN;
    }

    /**
     * Compute the available width for an item based on known column sizes.
     * Similar to how Rust computes available space for items during re-run detection.
     */
    private float computeItemAvailableWidth(GridItem item, FloatList columnSizes, FloatSize gap) {
        if (item.columnStart == null || item.columnStart < 0) {
            return NaN;  // Item not yet placed
        }

        int column = item.columnStart;
        int columnSpan = item.columnSpan;

        if (columnSizes == null || columnSizes.isEmpty()) {
            return NaN;
        }

        float totalWidth = 0f;
        for (int c = column; c < column + columnSpan && c < columnSizes.size(); c++) {
            totalWidth += columnSizes.getFloat(c);
        }
        // Add gaps between columns
        if (columnSpan > 1 && gap.width > 0) {
            totalWidth += gap.width * (columnSpan - 1);
        }
        return totalWidth;
    }

    /**
     * Compute the available height for an item based on known row sizes.
     * Similar to how Rust computes available space for items during re-run detection.
     */
    private float computeItemAvailableHeight(GridItem item, FloatList rowSizes, FloatSize gap) {
        if (item.rowStart == null || item.rowStart < 0) {
            return NaN;  // Item not yet placed
        }

        int row = item.rowStart;
        int rowSpan = item.rowSpan;

        if (rowSizes == null || rowSizes.isEmpty()) {
            return NaN;
        }

        float totalHeight = 0f;
        for (int r = row; r < row + rowSpan && r < rowSizes.size(); r++) {
            totalHeight += rowSizes.getFloat(r);
        }
        // Add gaps between rows
        if (rowSpan > 1 && gap.height > 0) {
            totalHeight += gap.height * (rowSpan - 1);
        }
        return totalHeight;
    }

    /**
     * Computes the known dimensions for an item based on its style, the grid area size, and aspect ratio.
     * This mirrors Rust's GridItem::known_dimensions() method.
     * <p>
     * When measuring an item's intrinsic size contribution:
     * 1. Resolve percentage sizes against the estimated grid area size
     * 2. Apply aspect ratio to derive unknown dimensions from known ones
     * 3. Apply stretch alignment if applicable
     * <p>
     * This is critical for items with percent heights and aspect-ratios, where the height
     * might resolve to a definite value based on the grid area, and then the width can be
     * derived from the aspect ratio.
     */
    private FloatSize computeItemKnownDimensions(
        GridItem item,
        FloatSize gridAreaSize,
        FloatSize innerNodeSize) {
        // Horizontal percentage margins resolve to 0 in track sizing to avoid cyclic dependency
        FloatSize marginAxisSums = item.getMarginAxisSumsWithBaselineShims(innerNodeSize.width);
        float marginsWidth = marginAxisSums.width;
        float marginsHeight = marginAxisSums.height;

        // Resolve padding and border against grid area size
        float paddingBorderWidth = (item.padding != null ? item.padding.left + item.padding.right : 0f)
                                   + (item.border != null ? item.border.left + item.border.right : 0f);
        float paddingBorderHeight = (item.padding != null ? item.padding.top + item.padding.bottom : 0f)
                                    + (item.border != null ? item.border.top + item.border.bottom : 0f);
        FloatSize paddingBorderSize = new FloatSize(paddingBorderWidth, paddingBorderHeight);

        // Box sizing adjustment
        FloatSize boxSizingAdj = (item.boxSizing == BoxSizing.CONTENT_BOX)
                                 ? paddingBorderSize
                                 : FloatSize.ZERO;

        // Resolve inherent size against grid area (not the initial nodeInnerSize)
        // This is critical: percent sizes like height: 100% must be resolved against the grid area,
        // not the container size that was used during item creation.
        FloatSize rawResolved = Resolve.maybeResolveSize(item.rawSize, gridAreaSize);
        FloatSize inherentSize = maybeAdd(maybeApplyAspectRatio(rawResolved, item.aspectRatio), boxSizingAdj);

        float width = inherentSize.width;
        float height = inherentSize.height;

        // If size is still null, check if we can derive from aspect ratio
        Float aspectRatio = item.aspectRatio;

        // Apply aspect ratio (already done in maybeApplyAspectRatio above, but do again in case stretch applies)
        if (aspectRatio != null && !Float.isNaN(aspectRatio) && aspectRatio > 0) {
            if (!Float.isNaN(width) && Float.isNaN(height)) {
                // width is known, derive height
                height = width / aspectRatio;
            } else if (!Float.isNaN(height) && Float.isNaN(width)) {
                // height is known, derive width
                width = height * aspectRatio;
            }
        }

        // Apply stretch alignment if size is still null
        float gridAreaMinusMarginsWidth = (!Float.isNaN(gridAreaSize.width))
                                          ? gridAreaSize.width - marginsWidth : NaN;
        float gridAreaMinusMarginsHeight = (!Float.isNaN(gridAreaSize.height))
                                           ? gridAreaSize.height - marginsHeight : NaN;

        if (Float.isNaN(width) && item.justifySelf == AlignItems.STRETCH) {
            // Check if margins are not auto (auto margins prevent stretch)
            // In our implementation, margins are already resolved to 0 if they were auto
            width = gridAreaMinusMarginsWidth;
            // Reapply aspect ratio
            if (aspectRatio != null && !Float.isNaN(aspectRatio) && aspectRatio > 0 && !Float.isNaN(width) && Float.isNaN(height)) {
                height = width / aspectRatio;
            }
        }

        // Per CSS Grid spec: If item has aspectRatio and no explicit height, 
        // vertical alignment defaults to START (not STRETCH).
        // This prevents the aspectRatio from incorrectly calculating width from stretched height.
        // See: https://www.w3.org/TR/css-grid-1/#grid-item-sizing
        boolean shouldStretchHeight = item.alignSelf == AlignItems.STRETCH;
        if (aspectRatio != null && !Float.isNaN(aspectRatio) && Float.isNaN(inherentSize.height)) {
            // When aspectRatio is set and height is not explicit, default is START not STRETCH
            shouldStretchHeight = false;
        }

        if (Float.isNaN(height) && shouldStretchHeight) {
            height = gridAreaMinusMarginsHeight;
            // Reapply aspect ratio
            if (aspectRatio != null && !Float.isNaN(aspectRatio) && aspectRatio > 0 && !Float.isNaN(height) && Float.isNaN(width)) {
                width = height * aspectRatio;
            }
        }

        // Clamp by min/max size
        if (!Float.isNaN(width) && item.minSize != null && !Float.isNaN(item.minSize.width)) {
            width = Math.max(width, item.minSize.width);
        }
        if (!Float.isNaN(width) && item.maxSize != null && !Float.isNaN(item.maxSize.width)) {
            width = Math.min(width, item.maxSize.width);
        }
        if (!Float.isNaN(height) && item.minSize != null && !Float.isNaN(item.minSize.height)) {
            height = Math.max(height, item.minSize.height);
        }
        if (!Float.isNaN(height) && item.maxSize != null && !Float.isNaN(item.maxSize.height)) {
            height = Math.min(height, item.maxSize.height);
        }

        return new FloatSize(width, height);
    }

    // ---------------------------------------------------------------------
    // Test-only hooks (package-private)
    //
    // These helpers expose a narrow, structured view of grid placement so that
    // unit tests can assert correctness without relying on reflection.
    // They are intentionally package-private and should not be used by
    // production code.
    // ---------------------------------------------------------------------

    record DebugPlacedItem(int index, int columnStart, int columnEnd, int rowStart, int rowEnd) {}

    record DebugPlacementResult(TrackCounts columnCounts, TrackCounts rowCounts, List<DebugPlacedItem> items) {}

    static DebugPlacementResult debugRunPlacementForTest(
        int explicitColumnCount,
        int explicitRowCount,
        List<TaffyStyle> childStyles,
        GridAutoFlow flow) {
        GridComputer gc = new GridComputer(null);

        // Minimal container style needed by count/placement logic
        TaffyStyle containerStyle = new TaffyStyle();
        containerStyle.display = TaffyDisplay.GRID;
        containerStyle.gridAutoFlow = (flow != null) ? flow : GridAutoFlow.ROW;

        // Build internal items from the provided child styles
        List<GridItem> items = new ArrayList<>();
        for (int i = 0; i < childStyles.size(); i++) {
            TaffyStyle child = childStyles.get(i);

            GridItem item = new GridItem();
            item.order = i;
            item.position = TaffyPosition.RELATIVE;

            GridPlacementResult colPlacement = gc.parseGridPlacement(
                child.getGridColumnStart(),
                child.getGridColumnEnd(),
                explicitColumnCount
            );
            item.columnStart = colPlacement.startIndex;
            item.columnEnd = colPlacement.endIndex;
            item.columnSpan = colPlacement.span;

            GridPlacementResult rowPlacement = gc.parseGridPlacement(
                child.getGridRowStart(),
                child.getGridRowEnd(),
                explicitRowCount
            );
            item.rowStart = rowPlacement.startIndex;
            item.rowEnd = rowPlacement.endIndex;
            item.rowSpan = rowPlacement.span;

            items.add(item);
        }

        // Provide explicit track counts to the count computation.
        // Only the list sizes are relevant here.
        List<TrackSizingFunction> expandedCols = new ArrayList<>();
        for (int i = 0; i < explicitColumnCount; i++) {
            expandedCols.add(TrackSizingFunction.auto());
        }
        List<TrackSizingFunction> expandedRows = new ArrayList<>();
        for (int i = 0; i < explicitRowCount; i++) {
            expandedRows.add(TrackSizingFunction.auto());
        }

        TrackCounts colCounts = gc.computeColumnCounts(items, expandedCols);
        TrackCounts rowCounts = gc.computeRowCounts(items, expandedRows);

        gc.autoPlaceItems(items, containerStyle.gridAutoFlow, colCounts, rowCounts);

        List<DebugPlacedItem> placed = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            GridItem item = items.get(i);

            int colStart;
            if (item.columnStart != null) {
                colStart = item.columnStart;
            } else if (item.columnEnd != null) {
                colStart = item.columnEnd - item.columnSpan;
            } else {
                colStart = colCounts.implicitStartLine();
            }

            int rowStart;
            if (item.rowStart != null) {
                rowStart = item.rowStart;
            } else if (item.rowEnd != null) {
                rowStart = item.rowEnd - item.rowSpan;
            } else {
                rowStart = rowCounts.implicitStartLine();
            }

            int colEnd = colStart + Math.max(1, item.columnSpan);
            int rowEnd = rowStart + Math.max(1, item.rowSpan);

            placed.add(new DebugPlacedItem(i, colStart, colEnd, rowStart, rowEnd));
        }

        return new DebugPlacementResult(colCounts, rowCounts, placed);
    }
}
