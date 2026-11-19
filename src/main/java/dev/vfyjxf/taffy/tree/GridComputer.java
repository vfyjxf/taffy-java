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
import dev.vfyjxf.taffy.util.AlignmentHelper;
import dev.vfyjxf.taffy.util.BaselineHelper;
import dev.vfyjxf.taffy.util.ContentSizeUtil;
import dev.vfyjxf.taffy.util.Resolve;
import dev.vfyjxf.taffy.util.ScrollableOverflowUtil;
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

    /**
     * The maximum number of tracks in each direction from the start of the explicit grid (line 0 in
     * OriginZero coordinates), and the maximum span of a single grid item.
     * <p>
     * Per <a href="https://www.w3.org/TR/css-grid-1/#overlarge-grids">CSS Grid over-large grids</a>
     * grids are clamped to this many tracks in each axis. Upstream taffy enforces the same limit in
     * {@code src/compute/grid/types/coordinates.rs} (GR-05 / #986) because it stores grid lines and
     * spans in {@code i16}/{@code u16}; this port uses {@code int} so the limit is defensive rather
     * than an overflow requirement, but it must still be enforced so that pathological styles cannot
     * allocate or iterate over an unbounded number of tracks, lines or cells.
     */
    static final int MAX_GRID_TRACKS = 10_000;

    /**
     * Sentinel returned by the sparse occupancy matrix when an area is entirely free (no collision
     * jump) and when no sparse placement cursor exists for a track. Real values are bounded grid
     * lines, so {@link Integer#MIN_VALUE} cannot collide with a valid result.
     */
    private static final int NO_COLLISION = Integer.MIN_VALUE;
    private static final int NO_CURSOR = Integer.MIN_VALUE;

    /** The lowest valid grid line in OriginZero coordinates. */
    static final int MIN_OZ_LINE = -MAX_GRID_TRACKS;

    /** The highest valid grid line in OriginZero coordinates. */
    static final int MAX_OZ_LINE = MAX_GRID_TRACKS;

    private final LayoutComputer layoutComputer;

    public GridComputer(LayoutComputer layoutComputer) {
        this.layoutComputer = layoutComputer;
    }

    /**
     * Clamp an OriginZero grid line into the limited grid ({@code [-MAX_GRID_TRACKS, MAX_GRID_TRACKS]}).
     */
    private static int clampOzLine(long line) {
        if (line < MIN_OZ_LINE) return MIN_OZ_LINE;
        if (line > MAX_OZ_LINE) return MAX_OZ_LINE;
        return (int) line;
    }

    /**
     * Clamp a grid item span into the limited grid. A span is at least one track (a zero or negative
     * span is invalid and is treated as 1, matching upstream's {@code Span(span).clamp(1, MAX_GRID_TRACKS)}).
     */
    private static int clampGridSpan(int span) {
        if (span < 1) return 1;
        return Math.min(span, MAX_GRID_TRACKS);
    }

    /**
     * Saturating addition used when accumulating track counts or line numbers, so that pathological
     * style values cannot overflow the accumulator.
     */
    private static int saturatingAdd(int a, int b) {
        long sum = (long) a + (long) b;
        if (sum > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (sum < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) sum;
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
        TaffyRect<LengthPercentage> rawPadding;   // Original padding style (resolved against the grid area)
        TaffyRect<LengthPercentage> rawBorder;    // Original border style (resolved against the grid area)
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
        // The item's first baseline (horizontal), measured from the top of its border box
        // (upstream #1322). Set from the item's intrinsic layout when resolving baseline alignment
        // during track sizing, then from its final layout.
        float baseline = NaN;
        float baselineShim;   // Shim for baseline alignment that acts like an extra top margin

        // The item's final y position in the container's border-box coordinate space, excluding any
        // relative offset (which does not affect the container's baseline; upstream #1323).
        // Set by the final positioning pass, read by calculateContainerBaseline.
        float yPosition;

        // Computed values
        FloatSize computedSize;
        FloatPoint location;

        // The item's own scrollable overflow rectangle, as reported by its final layout output
        // (upstream grid/alignment.rs stores the child output's rectangle in the item's Layout).
        // Recorded by placeItems and written to the item's Layout by performFinalLayout; the
        // container's own rectangle accumulates the contribution in placeItems.
        ScrollableOverflow scrollableOverflow = ScrollableOverflow.ZERO;

        // Track crossing flags (for re-run logic)
        boolean crossesIntrinsicColumn;
        boolean crossesFlexibleColumn;
        boolean crossesIntrinsicRow;
        boolean crossesFlexibleRow;

        // Caches for intrinsic size computation. These caches are only valid for a single run of the track-sizing algorithm.
        // Caches for the available_space input used to compute the intrinsic contributions, one per axis
        // (GR-08 / upstream #1227). They are recorded when a contribution is actually computed and are
        // compared by the track-sizing re-run checks to decide whether a contribution can have changed.
        // A single shared field was wrong: the block-axis measurement clobbered the inline-axis input
        // (writing a size whose width is NaN), so the row re-run check always saw "input changed" and
        // re-measured every item.
        FloatSize inlineContributionInputCache;
        FloatSize blockContributionInputCache;
        // Cache for the known_dimensions input to intrinsic sizing computation, together with the
        // grid area it was computed for. The min- and max-content contributions of an item are both
        // computed from the same known_dimensions, so sharing the result halves that work
        // (GR-09 / upstream #1226). Must be cleared whenever an input changes (baseline_shim, grid
        // area size); see clearKnownDimensionsCache().
        FloatSize knownDimensionsCacheGridArea;
        FloatSize knownDimensionsCache;
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
         * Whether the item has an auto margin in the block axis (upstream GridItem::has_auto_block_margin).
         * Per css-align-3 §9.5 a box with an auto margin in the relevant axis does not participate
         * in baseline alignment.
         */
        boolean hasAutoBlockMargin() {
            return rawMargin != null && (rawMargin.top.isAuto() || rawMargin.bottom.isAuto());
        }

        /**
         * Whether the item's block size depends on the size of its row(s), creating a cyclic
         * dependency with baseline alignment (which itself affects row sizing). Mirrors upstream
         * GridItem::has_cyclic_block_size_dependency: {@code size.height.uses_percentage()} is true
         * for percentage values and for calc() expressions (the compact-length percentage bit is
         * set for every calc tag), and the item must cross an intrinsic or flexible row.
         * Per css-grid-1 §12.5 such items do not participate and use their fallback alignment.
         */
        boolean hasCyclicBlockSizeDependency() {
            if (rawSize == null || rawSize.height == null) return false;
            boolean usesPercentage = rawSize.height.isPercent() || rawSize.height.isCalc();
            return usesPercentage && (crossesIntrinsicRow || crossesFlexibleRow);
        }

        /**
         * Returns true if the item participates in baseline alignment: it has {@code align-self: baseline},
         * neither of its block-axis margins is auto, and its block size does not depend on the row
         * size (upstream #1125 / 3dbf2e46, css-align-3 §9.5 and css-grid-1 §12.5).
         */
        boolean participatesInBaselineAlignment() {
            return alignSelf == AlignItems.BASELINE
                && !hasAutoBlockMargin()
                && !hasCyclicBlockSizeDependency();
        }

        /**
         * The item's baseline as measured from the top of its margin box. Baseline alignment aligns
         * items by this value; {@link #baseline} itself is border-box relative (upstream #1322).
         *
         * @param innerNodeWidth the container's inner width, used to resolve a percentage top margin
         */
        float marginBoxBaseline(float innerNodeWidth) {
            float top = rawMargin != null ? Resolve.resolveLpaOrZero(rawMargin.top, innerNodeWidth) : 0f;
            return (Float.isNaN(baseline) ? 0f : baseline) + top;
        }

        /**
         * Compute the item's resolved margins for size contributions.
         * Horizontal percentage margins always resolve to zero if the container size is indefinite
         * as otherwise this would introduce a cyclic dependency.
         *
         * @param innerNodeWidth The inner width of the node, used for vertical percentage margins
         * @return Size with width being sum of horizontal margins, height being sum of vertical margins
         */
        FloatSize getMarginAxisSumsWithBaselineShims(float percentageBasis) {
            // Horizontal percentage margins resolve against 0 (not the containing block width)
            // This is per CSS Grid spec to avoid cyclic dependencies
            float left = Resolve.resolveLpaOrZero(rawMargin.left, 0f);
            float right = Resolve.resolveLpaOrZero(rawMargin.right, 0f);
            // Vertical percentage margins resolve against the inline size of the containing block,
            // which is the grid area's inline size during intrinsic sizing (upstream #960).
            float top = Resolve.resolveLpaOrZero(rawMargin.top, percentageBasis) + baselineShim;
            float bottom = Resolve.resolveLpaOrZero(rawMargin.bottom, percentageBasis);
            return new FloatSize(left + right, top + bottom);
        }

        /**
         * The item's padding resolved against the grid area. Percentage padding and border resolve
         * against the inline size of the containing block, which for a grid item under intrinsic
         * measurement is the grid area's inline size when it is definite (upstream #960).
         *
         * @param gridAreaSize the grid area size, either dimension NaN when indefinite
         */
        FloatRect resolvePadding(FloatSize gridAreaSize) {
            return rawPadding != null ? Resolve.resolveRectOrZero(rawPadding, gridAreaSize.width) : FloatRect.zero();
        }

        /** The item's border resolved against the grid area's inline size (upstream #960). */
        FloatRect resolveBorder(FloatSize gridAreaSize) {
            return rawBorder != null ? Resolve.resolveRectOrZero(rawBorder, gridAreaSize.width) : FloatRect.zero();
        }

        /** The border-box padding + border sums resolved against the grid area. */
        FloatSize paddingBorderSize(FloatSize gridAreaSize) {
            FloatRect padding = resolvePadding(gridAreaSize);
            FloatRect border = resolveBorder(gridAreaSize);
            return new FloatSize(
                padding.left + padding.right + border.left + border.right,
                padding.top + padding.bottom + border.top + border.bottom
            );
        }

        /** Box-sizing adjustment: content-box styles add their padding + border to the used size. */
        private FloatSize boxSizingAdjustment(FloatSize gridAreaSize) {
            return boxSizing == BoxSizing.CONTENT_BOX ? paddingBorderSize(gridAreaSize) : FloatSize.ZERO;
        }

        /**
         * The item's preferred size resolved against the grid area (upstream alignment.rs). A
         * percentage resolves against the grid area when that dimension is definite; otherwise it
         * stays unresolved. The box-sizing adjustment is added for content-box items.
         */
        FloatSize resolvePreferredSize(FloatSize gridAreaSize) {
            return maybeAdd(maybeApplyAspectRatio(Resolve.maybeResolveSize(rawSize, gridAreaSize), aspectRatio),
                boxSizingAdjustment(gridAreaSize));
        }

        /** The item's specified min size resolved against the grid area, floored at padding + border. */
        FloatSize resolveMinSize(FloatSize gridAreaSize) {
            FloatSize min = maybeAdd(maybeApplyAspectRatio(Resolve.maybeResolveSize(rawMinSize, gridAreaSize), aspectRatio),
                boxSizingAdjustment(gridAreaSize));
            FloatSize paddingBorder = paddingBorderSize(gridAreaSize);
            return new FloatSize(
                Math.max(!Float.isNaN(min.width) ? min.width : 0f, paddingBorder.width),
                Math.max(!Float.isNaN(min.height) ? min.height : 0f, paddingBorder.height)
            );
        }

        /**
         * The item's specified min size resolved against the grid area, treating an indefinite
         * grid-area dimension as zero (upstream #1257). A cyclic percentage min size therefore
         * contributes padding + border (normally zero) instead of falling through to the
         * content-based automatic minimum size.
         */
        FloatSize resolveMinSizePercentAsZero(FloatSize gridAreaSize) {
            FloatSize basis = new FloatSize(
                !Float.isNaN(gridAreaSize.width) ? gridAreaSize.width : 0f,
                !Float.isNaN(gridAreaSize.height) ? gridAreaSize.height : 0f
            );
            FloatSize min = maybeAdd(maybeApplyAspectRatio(Resolve.maybeResolveSize(rawMinSize, basis), aspectRatio),
                boxSizingAdjustment(gridAreaSize));
            FloatSize paddingBorder = paddingBorderSize(gridAreaSize);
            return new FloatSize(
                Math.max(!Float.isNaN(min.width) ? min.width : 0f, paddingBorder.width),
                Math.max(!Float.isNaN(min.height) ? min.height : 0f, paddingBorder.height)
            );
        }

        /** The item's specified max size resolved against the grid area. */
        FloatSize resolveMaxSize(FloatSize gridAreaSize) {
            return maybeAdd(maybeApplyAspectRatio(Resolve.maybeResolveSize(rawMaxSize, gridAreaSize), aspectRatio),
                boxSizingAdjustment(gridAreaSize));
        }

        /** The item's margins resolved against the grid area's inline size. */
        FloatRect resolveMargin(FloatSize gridAreaSize) {
            return rawMargin != null ? Resolve.resolveRectLpaOrZero(rawMargin, gridAreaSize.width) : FloatRect.zero();
        }


        /**
         * Get min content contribution for width, using cache if available.
         */
        float getMinContentContributionWidthCached(LayoutComputer layoutComputer, FloatSize gridAreaSize) {
            if (minContentContributionWidth != null) {
                return minContentContributionWidth;
            }
            float contribution = computeMinContentContributionWidth(layoutComputer, gridAreaSize);
            minContentContributionWidth = contribution;
            // Record the input this contribution was computed for (only on a cache miss, so the
            // recorded input always matches the cached contribution).
            inlineContributionInputCache = gridAreaSize;
            return contribution;
        }

        /**
         * Get max content contribution for width, using cache if available.
         */
        float getMaxContentContributionWidthCached(LayoutComputer layoutComputer, FloatSize gridAreaSize) {
            if (maxContentContributionWidth != null) {
                return maxContentContributionWidth;
            }
            float contribution = computeMaxContentContributionWidth(layoutComputer, gridAreaSize);
            maxContentContributionWidth = contribution;
            inlineContributionInputCache = gridAreaSize;
            return contribution;
        }

        /**
         * Get max content contribution for height, using cache if available.
         */
        float getMaxContentContributionHeightCached(LayoutComputer layoutComputer, FloatSize gridAreaSize) {
            if (maxContentContributionHeight != null) {
                return maxContentContributionHeight;
            }
            float contribution = computeMaxContentContributionHeight(layoutComputer, gridAreaSize);
            maxContentContributionHeight = contribution;
            blockContributionInputCache = gridAreaSize;
            return contribution;
        }

        /**
         * Compute min content contribution for width axis.
         * Uses measureChildSize for efficient sizing without full layout.
         * <p>
         * This method returns the contribution INCLUDING margin, matching the
         * original behavior expected by callers in track sizing.
         */
        private float computeMinContentContributionWidth(LayoutComputer layoutComputer, FloatSize gridAreaSize) {
            FloatSize marginAxisSums = getMarginAxisSumsWithBaselineShims(gridAreaSize.width);

            // If the preferred width resolves against the grid area, use it
            FloatSize inherentSize = resolvePreferredSize(gridAreaSize);
            if (!Float.isNaN(inherentSize.width)) {
                return inherentSize.width + marginAxisSums.width;
            }

            // Compute known dimensions with stretch alignment and aspect ratio
            FloatSize knownDimensions = computeKnownDimensionsCached(gridAreaSize);

            // Build available space for measurement - match Rust's available_space.map() logic.
            // Upstream calls this with grid_area_size.with(axis, None), so the sized axis is
            // unconstrained (min-content here) and the other axis is definite when the grid area is.
            AvailableSpace widthAvail = AvailableSpace.minContent();
            AvailableSpace heightAvail = !Float.isNaN(gridAreaSize.height)
                                         ? AvailableSpace.definite(gridAreaSize.height)
                                         : AvailableSpace.minContent();

            // Use measureChildSize for efficient sizing (returns just size, no full layout).
            // The grid area is the item's containing block during intrinsic measurement (#960).
            FloatSize measuredSize = layoutComputer.measureChildSize(
                nodeId,
                knownDimensions,
                gridAreaSize,
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
        private float computeMaxContentContributionWidth(LayoutComputer layoutComputer, FloatSize gridAreaSize) {
            FloatSize marginAxisSums = getMarginAxisSumsWithBaselineShims(gridAreaSize.width);

            // If the preferred width resolves against the grid area, use it
            FloatSize inherentSize = resolvePreferredSize(gridAreaSize);
            if (!Float.isNaN(inherentSize.width)) {
                return inherentSize.width + marginAxisSums.width;
            }

            FloatSize knownDimensions = computeKnownDimensionsCached(gridAreaSize);

            // Build available space for measurement - match Rust's available_space.map() logic.
            // See the min-content path: the sized axis is unconstrained (max-content here).
            AvailableSpace widthAvail = AvailableSpace.maxContent();
            AvailableSpace heightAvail = !Float.isNaN(gridAreaSize.height)
                                         ? AvailableSpace.definite(gridAreaSize.height)
                                         : AvailableSpace.maxContent();

            FloatSize measuredSize = layoutComputer.measureChildSize(
                nodeId,
                knownDimensions,
                gridAreaSize,
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
        private float computeMaxContentContributionHeight(LayoutComputer layoutComputer, FloatSize gridAreaSize) {
            FloatSize marginAxisSums = getMarginAxisSumsWithBaselineShims(gridAreaSize.width);

            // If the preferred height resolves against the grid area, use it
            FloatSize inherentSize = resolvePreferredSize(gridAreaSize);
            if (!Float.isNaN(inherentSize.height)) {
                return inherentSize.height + marginAxisSums.height;
            }

            FloatSize knownDimensions = computeKnownDimensionsCached(gridAreaSize);

            // Build available space for measurement - match Rust's available_space.map() logic.
            // See the inline-axis paths: the sized axis is unconstrained (max-content here) and
            // the grid area's other dimension is definite when available.
            AvailableSpace widthAvail = !Float.isNaN(gridAreaSize.width)
                                        ? AvailableSpace.definite(gridAreaSize.width)
                                        : AvailableSpace.maxContent();
            AvailableSpace heightAvail = AvailableSpace.maxContent();

            FloatSize measuredSize = layoutComputer.measureChildSize(
                nodeId,
                knownDimensions,
                gridAreaSize,
                new TaffySize<>(widthAvail, heightAvail),
                SizingMode.INHERENT_SIZE,
                TaffyLine.FALSE
            );

            return measuredSize.height + marginAxisSums.height;
        }

        /**
         * Clear the cached known dimensions. Called explicitly wherever an input of
         * {@link #computeKnownDimensions(FloatSize)} changes (GR-08/GR-09 explicit invalidation),
         * matching upstream #1226's {@code known_dimensions_cache = None} sites.
         */
        void clearKnownDimensionsCache() {
            knownDimensionsCacheGridArea = null;
            knownDimensionsCache = null;
        }

        /**
         * Get the known dimensions for the given grid area size, using the cache when it was computed
         * for exactly the same inputs. This mirrors upstream's {@code known_dimensions_cached}
         * (GR-09 / #1226): the min-content and max-content contributions of an item are both computed
         * from the same known dimensions, so the second contribution can reuse the first one's result.
         */
        private FloatSize computeKnownDimensionsCached(FloatSize gridAreaSize) {
            if (knownDimensionsCache != null && floatSizeEquals(knownDimensionsCacheGridArea, gridAreaSize)) {
                return knownDimensionsCache;
            }
            FloatSize knownDimensions = computeKnownDimensions(gridAreaSize);
            knownDimensionsCacheGridArea = gridAreaSize;
            knownDimensionsCache = knownDimensions;
            return knownDimensions;
        }

        /**
         * Compute known dimensions for sizing, applying stretch alignment and aspect ratio.
         * This is similar to Rust's GridItem::known_dimensions method.
         */
        private FloatSize computeKnownDimensions(FloatSize gridAreaSize) {
            FloatSize margins = getMarginAxisSumsWithBaselineShims(gridAreaSize.width);

            // Resolve the preferred/min/max sizes against the grid area (upstream #960). A
            // percentage stays unresolved when the corresponding grid-area dimension is indefinite.
            FloatSize inherentSize = resolvePreferredSize(gridAreaSize);
            FloatSize minSizeResolved = resolveMinSize(gridAreaSize);
            FloatSize maxSizeResolved = resolveMaxSize(gridAreaSize);

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
                // TODO(S19): upstream #1281 treats `normal` alignment as "stretch unless replaced";
                // Java normalizes AUTO to STRETCH before this point (S10), so replaced items still
                // stretch here.
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

    private static float capCompressibleReplacedMinimumContributionWidth(GridItem item, float currentWithMargin,
                                                                          float marginWidth, FloatSize gridAreaSize) {
        if (item == null || !item.isCompressibleReplaced) return currentWithMargin;

        // Border+padding minimum (outer size can't be smaller than this), resolved against the grid area
        float paddingBorderWidth = item.paddingBorderSize(gridAreaSize).width;

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

        // The track sizing algorithm operates on the grid container's content box, so the min/max
        // sizes (which are border-box sizes) have to be converted to content-box sizes before being
        // passed to it (GR-03 / upstream #1023). Note that minSize/maxSize already include the
        // box-sizing adjustment (padding + border for content-box styles), so subtracting the content
        // box inset here does not double count it.
        FloatSize innerMinSize = new FloatSize(
            TaffyMath.maybeSub(minSize.width, contentBoxInsetWidth),
            TaffyMath.maybeSub(minSize.height, contentBoxInsetHeight)
        );
        FloatSize innerMaxSize = new FloatSize(
            TaffyMath.maybeSub(maxSize.width, contentBoxInsetWidth),
            TaffyMath.maybeSub(maxSize.height, contentBoxInsetHeight)
        );

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

        // Expand auto-fill/auto-fit in grid templates. The auto-fit container sizes are captured here
        // (before the first track-sizing pass can update `nodeInnerSize`) so that the auto-repeat
        // counts used by the expanded templates and by the auto-fit collapse pass are identical
        // (GR-07 / upstream #946).
        float autoFitContainerWidth = nodeInnerSize.width;
        float autoFitContainerHeight = nodeInnerSize.height;
        List<TrackSizingFunction> expandedColumns = getExpandedTemplateColumns(style, autoFitContainerWidth, gap.width);
        List<TrackSizingFunction> expandedRows = getExpandedTemplateRows(style, autoFitContainerHeight, gap.height);

        // Resolve named lines exactly once, before the grid size estimate (GR-15 / #1259), and share
        // the resolver between the in-flow placement pass and the absolutely positioned children.
        int columnAutoRepetitions = autoRepetitionCount(style.gridTemplateColumnsWithRepeat,
            autoFitContainerWidth, gap.width);
        int rowAutoRepetitions = autoRepetitionCount(style.gridTemplateRowsWithRepeat,
            autoFitContainerHeight, gap.height);
        NamedLineResolver namedLineResolver = createNamedLineResolver(style, expandedColumns, expandedRows,
            columnAutoRepetitions, rowAutoRepetitions);

        // Generate grid items
        List<GridItem> items = generateGridItems(node, style, nodeInnerSize, namedLineResolver);

        // Determine grid dimensions based on template or content
        TrackCounts colCounts = computeColumnCounts(items, expandedColumns,
            style.getGridTemplateAreaColumnCount());
        TrackCounts rowCounts = computeRowCounts(items, expandedRows,
            style.getGridTemplateAreaRowCount());

        // Auto-place items that don't have explicit positions
        // Note: This may expand colCounts and rowCounts as needed
        autoPlaceItems(items, style.getGridAutoFlow(), colCounts, rowCounts);

        // Get the final track counts after auto-placement (may have been expanded)
        int numColumns = colCounts.len();
        int numRows = rowCounts.len();

        // The per-index track sizing functions (explicit template + cycling grid-auto-*), shared by
        // the grid-area-size estimates below and by the track sizing passes.
        List<TrackSizingFunction> resolvedColumnTracks = resolveTrackSizingList(
            expandedColumns, style.getGridAutoColumns(), colCounts, numColumns);
        List<TrackSizingFunction> resolvedRowTracks = resolveTrackSizingList(
            expandedRows, style.getGridAutoRows(), rowCounts, numRows);

        // Determine if items cross flexible or intrinsic tracks (for re-run optimization)
        determineIfItemCrossesFlexibleOrIntrinsicTracks(
            items, expandedColumns, style.getGridAutoColumns(),
            expandedRows, style.getGridAutoRows(),
            numColumns, numRows, colCounts, rowCounts);

        // CSS Grid: auto-fit tracks that contain no in-flow item are collapsed (size 0, no gutter).
        // The occupancy of a track cannot change during track sizing, so the collapse decision is
        // made once and re-applied to the sizes after each sizing pass (GR-07 / upstream #946).
        AutoFitCollapse columnAutoFit = computeAutoFitCollapse(numColumns, style.gridTemplateColumnsWithRepeat,
            items, autoFitContainerWidth, gap.width, colCounts, true);
        AutoFitCollapse rowAutoFit = computeAutoFitCollapse(numRows, style.gridTemplateRowsWithRepeat,
            items, autoFitContainerHeight, gap.height, rowCounts, false);

        // Calculate track sizes even if no items - based on template
        FloatList columnSizes = calculateColumnSizes(style, nodeInnerSize, innerMinSize.width, innerMaxSize.width,
            availableGridSpace, availableSpace, gap, numColumns, numRows, items, colCounts, expandedColumns);
        applyAutoFitCollapse(columnSizes, columnAutoFit.collapsed());
        AxisGeometry columnGeometry = AxisGeometry.of(columnAutoFit.collapsed(), columnAutoFit.autoFitIsLast());

        // Per CSS Grid spec 11.8: Update inner_node_size.width if it was indefinite
        // This is needed for resolving percentage values in row sizing and for re-running column sizing
        float initialColumnSum = columnGeometry.usedSize(columnSizes, gap.width);

        // Whether the grid container's own size is indefinite, which is what decides whether
        // percentage tracks/gaps have to be re-run (GR-11 / upstream #1243). This is deliberately
        // not the available-space definiteness: a shrink-to-fit (e.g. inline-grid) container can have
        // a definite available space while its own size is still content-based.
        boolean parentWidthIndefinite = Float.isNaN(styledBasedKnownDimensions.width);
        boolean parentHeightIndefinite = Float.isNaN(styledBasedKnownDimensions.height);

        // If inner node width was indefinite, update it with the column sum. Row sizing resolves
        // percentage heights against the content-sized container width (upstream #960).
        if (Float.isNaN(nodeInnerSize.width)) {
            nodeInnerSize = new FloatSize(initialColumnSum, nodeInnerSize.height);
        }

        // If only the container's width has been requested then the rows do not need to be sized at all,
        // which avoids measuring the height of every item (and recursively of everything below them).
        // Column sizing is not re-run when only computing the container's size, so the returned size is
        // unaffected by skipping the block-axis work.
        //
        // TODO(GR-10): unreachable in this port today: LayoutComputer always passes
        // RequestedAxis.BOTH (see the CR-01 TODOs in LayoutComputer.performChildLayout and
        // LayoutComputer.measureChildSize), so no caller ever asks for the horizontal axis alone.
        // Enable this together with the RequestedAxis plumbing (CR-01 / FL-25). Upstream #1219
        // (9770a07) has the same short-circuit.
        if (runMode == RunMode.COMPUTE_SIZE && inputs.axis() == RequestedAxis.HORIZONTAL) {
            float width = Math.max(
                TaffyMath.clamp(initialColumnSum + contentBoxInset.left + contentBoxInset.right,
                    minSize.width, maxSize.width),
                paddingBorderSize.width);
            return LayoutOutput.fromOuterSize(new FloatSize(width, 0f));
        }

        // Clear the inline-axis contribution input cache after initial column sizing (Rust:
        // items.iter_mut().for_each(|item| item.grid_area_size_cache = None)): the container's inner
        // width is now definite, so any input recorded during column sizing is stale.
        for (GridItem item : items) {
            item.inlineContributionInputCache = null;
            item.blockContributionInputCache = null;
            item.clearKnownDimensionsCache();
        }

        // Resolve baseline-aligned item baselines before row sizing (CSS Grid 11.5.1)
        resolveItemBaselines(items, nodeInnerSize);

        FloatList rowSizes = calculateRowSizes(style, nodeInnerSize, innerMinSize.height, innerMaxSize.height,
            availableGridSpace, gap, numRows, items, columnSizes, colCounts, rowCounts, expandedRows);
        applyAutoFitCollapse(rowSizes, rowAutoFit.collapsed());
        AxisGeometry rowGeometry = AxisGeometry.of(rowAutoFit.collapsed(), rowAutoFit.autoFitIsLast());

        // Calculate initial row sum (like Rust's initial_row_sum)
        float initialRowSum = rowGeometry.usedSize(rowSizes, gap.height);

        // The container's size is now determined, so percentages resolve against its content box
        // when track sizing is re-run (GR-11 / upstream #1243). Unlike the first pass, the content
        // box is the min/max-clamped container content box. This replaces the old "step 7" clamp of
        // percentage track base sizes: its results were always discarded by the re-run that follows
        // (upstream #1244).
        float containerBorderWidth = !Float.isNaN(styledBasedKnownDimensions.width)
                                     ? styledBasedKnownDimensions.width
                                     : initialColumnSum + contentBoxInsetWidth;
        float containerBorderHeight = !Float.isNaN(styledBasedKnownDimensions.height)
                                      ? styledBasedKnownDimensions.height
                                      : initialRowSum + contentBoxInsetHeight;
        containerBorderWidth = Math.max(TaffyMath.clamp(containerBorderWidth, minSize.width, maxSize.width),
            paddingBorderSize.width);
        containerBorderHeight = Math.max(TaffyMath.clamp(containerBorderHeight, minSize.height, maxSize.height),
            paddingBorderSize.height);
        nodeInnerSize = new FloatSize(
            Math.max(0f, containerBorderWidth - contentBoxInsetWidth),
            Math.max(0f, containerBorderHeight - contentBoxInsetHeight));
        if (style.getGap().width.isPercent()) {
            gap = new FloatSize(style.getGap().width.resolveOrZero(nodeInnerSize.width), gap.height);
        }
        if (style.getGap().height.isPercent()) {
            gap = new FloatSize(gap.width, style.getGap().height.resolveOrZero(nodeInnerSize.height));
        }

        // === Rust-style Re-run Logic (CSS Grid spec 11.8) ===
        // Column sizing must be re-run (once) if:
        //   - The grid container's width was initially indefinite and there are any columns with percentage track sizing functions
        //   - Any grid item crossing an intrinsically sized track's min content contribution width has changed

        boolean rerunColumnSizing;
        boolean hasPercentageColumn = expandedColumns.stream().anyMatch(TrackSizingFunction::usesPercentage);
        boolean hasPercentageGutter = style.getGap().width.isPercent();
        boolean hasPercentageRow = expandedRows.stream().anyMatch(TrackSizingFunction::usesPercentage);
        boolean hasPercentageRowGutter = style.getGap().height.isPercent();
        rerunColumnSizing = parentWidthIndefinite && (hasPercentageColumn || hasPercentageGutter);

        final FloatSize finalNodeInnerSize = nodeInnerSize;
        final FloatList finalRowSizes = rowSizes;
        final FloatSize finalGap = gap;

        // Upstream #960: the intrinsic contributions that changed during the re-run checks force the
        // container's size to be recomputed from the final track sizes (the first pass sized the
        // tracks against an indefinite container, so its sums are stale). The flags are only set by
        // the corresponding check actually running and finding a different min-content contribution.
        boolean intrinsicColumnContributionChanged = false;
        boolean intrinsicRowContributionChanged = false;

        if (!rerunColumnSizing) {
            // Check if any item crossing an intrinsic column has changed its min_content_contribution
            // This mirrors Rust's approach: compute new contribution and compare with cached value
            // CRITICAL: Only trigger re-run if the cached value EXISTS and has changed.
            // If cache is empty, the item wasn't used in initial track sizing, so skip it.
            boolean minContentContributionChanged = false;
            for (int itemIdx = 0; itemIdx < items.size(); itemIdx++) {
                GridItem item = items.get(itemIdx);
                if (!item.crossesIntrinsicColumn) continue;

                // If no cached contribution from initial track sizing, skip this item
                // This is critical for performance - avoids triggering recursive measureChildSize
                if (item.minContentContributionWidth == null) {
                    continue;
                }

                // Recompute the grid area from the initial column sizes and the initial row sizes,
                // like Rust's item.grid_area_size() (upstream #960).
                int col = getItemColumnWithCounts(item, itemIdx, numColumns, colCounts);
                float areaWidth = definiteGridAreaExtent(columnSizes, resolvedColumnTracks, col,
                    item.columnSpan, finalNodeInnerSize.width);
                float itemHeight = computeItemAvailableHeight(item, finalRowSizes, finalGap);
                FloatSize itemGridAreaSize = new FloatSize(areaWidth, itemHeight);

                // Check if the grid-area input used for the cached inline-axis contribution changed
                // (per-axis cache, GR-08 / #1227)
                boolean inputChanged = item.inlineContributionInputCache == null ||
                                       !floatSizeEquals(item.inlineContributionInputCache, itemGridAreaSize);

                if (!inputChanged) {
                    // The grid area didn't change, so the contribution cannot have changed either
                    continue;
                }

                // Compute new min_content_contribution (non-cached version)
                FloatSize marginAxisSums = item.getMarginAxisSumsWithBaselineShims(itemGridAreaSize.width);
                float newMinContentContribution;
                FloatSize preferredSize = item.resolvePreferredSize(itemGridAreaSize);
                if (!Float.isNaN(preferredSize.width)) {
                    newMinContentContribution = preferredSize.width + marginAxisSums.width;
                } else {
                    FloatSize itemKnownDims = item.computeKnownDimensions(itemGridAreaSize);
                    FloatSize measuredSize = layoutComputer.measureChildSize(
                        item.nodeId,
                        itemKnownDims,
                        itemGridAreaSize,
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
                item.inlineContributionInputCache = itemGridAreaSize;
                item.minContentContributionWidth = newMinContentContribution;
                item.maxContentContributionWidth = null;
                item.minimumContributionWidth = null;

                if (hasChanged) {
                    minContentContributionChanged = true;
                }
            }
            intrinsicColumnContributionChanged = minContentContributionChanged;
            rerunColumnSizing = minContentContributionChanged;
        } else {
            // Clear intrinsic width caches since we're definitely re-running due to percentage tracks
            for (GridItem item : items) {
                item.inlineContributionInputCache = null;
                item.blockContributionInputCache = null;
                item.minContentContributionWidth = null;
                item.maxContentContributionWidth = null;
                item.minimumContributionWidth = null;
                item.clearKnownDimensionsCache();
            }
        }

        // Row sizing must be re-run (once), independently of column sizing (GR-11 / upstream #1244):
        //   - The grid container's height is indefinite and there are percentage rows/gaps
        //   - Column sizing was re-run and an item crossing an intrinsic row changed its contribution
        boolean rerunRowSizing = parentHeightIndefinite && (hasPercentageRow || hasPercentageRowGutter);

        if (rerunRowSizing) {
            // Clear the intrinsic height caches: the percentage-row re-run re-measures every item
            // (upstream #1244; the caches are cleared before the column re-run so that the column
            // re-run's contribution check is skipped).
            for (GridItem item : items) {
                item.inlineContributionInputCache = null;
                item.blockContributionInputCache = null;
                item.minContentContributionHeight = null;
                item.maxContentContributionHeight = null;
                item.minimumContributionHeight = null;
                item.clearKnownDimensionsCache();
            }
        }

        if (rerunColumnSizing) {
            // Re-run column sizing with known row sizes
            columnSizes = calculateColumnSizesWithRowSizes(style, nodeInnerSize, innerMinSize.width, innerMaxSize.width,
                availableGridSpace, availableSpace, gap, numColumns, numRows, items, colCounts, expandedColumns, finalRowSizes);
            // Re-apply auto-fit collapse after the re-run. The collapse flags (and therefore the
            // axis geometry) do not change, only the sizes of the non-collapsed tracks can.
            applyAutoFitCollapse(columnSizes, columnAutoFit.collapsed());

            // The column widths may have changed, so the items' intrinsic height contributions need
            // to be recomputed. This is skipped when row sizing is being re-run anyway, because its
            // caches have already been cleared above.
            final FloatList finalColumnSizes = columnSizes;

            if (!rerunRowSizing) {
                // Check if any item crossing an intrinsic row has changed its min_content_contribution
                // CRITICAL: Only trigger re-run if cached value EXISTS and has changed
                boolean minContentContributionChanged = false;
                for (int itemIdx = 0; itemIdx < items.size(); itemIdx++) {
                    GridItem item = items.get(itemIdx);
                    if (!item.crossesIntrinsicRow) continue;

                    // If no cached contribution from initial track sizing, skip this item
                    if (item.minContentContributionHeight == null) {
                        continue;
                    }

                    // Recompute the grid area from the known column sizes and the initial row sizes
                    // (upstream #960's item.grid_area_size()).
                    int row = getItemRowWithCounts(item, itemIdx, numColumns, rowCounts);
                    float areaHeight = definiteGridAreaExtent(rowSizes, resolvedRowTracks, row,
                        item.rowSpan, finalNodeInnerSize.height);
                    float itemWidth = computeItemAvailableWidth(item, finalColumnSizes, finalGap);
                    FloatSize itemGridAreaSize = new FloatSize(itemWidth, areaHeight);

                    // Check if the grid-area input used for the cached block-axis contribution changed
                    // (per-axis cache, GR-08 / #1227)
                    boolean inputChanged = item.blockContributionInputCache == null ||
                                           !floatSizeEquals(item.blockContributionInputCache, itemGridAreaSize);

                    if (!inputChanged) {
                        continue;
                    }

                    // Compute new min_content_contribution for height
                    FloatSize marginAxisSums = item.getMarginAxisSumsWithBaselineShims(itemGridAreaSize.width);
                    float newMinContentContribution;
                    FloatSize preferredSize = item.resolvePreferredSize(itemGridAreaSize);
                    if (!Float.isNaN(preferredSize.height)) {
                        newMinContentContribution = preferredSize.height + marginAxisSums.height;
                    } else {
                        FloatSize itemKnownDims = item.computeKnownDimensions(itemGridAreaSize);
                        FloatSize measuredSize = layoutComputer.measureChildSize(
                            item.nodeId,
                            itemKnownDims,
                            itemGridAreaSize,
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
                    item.blockContributionInputCache = itemGridAreaSize;
                    item.minContentContributionHeight = newMinContentContribution;
                    item.maxContentContributionHeight = null;
                    item.minimumContributionHeight = null;

                    if (hasChanged) {
                        minContentContributionChanged = true;
                    }
                }
                intrinsicRowContributionChanged = minContentContributionChanged;
                rerunRowSizing = minContentContributionChanged;
            }
        }

        if (rerunRowSizing) {
            // Re-run row sizing with known column sizes
            rowSizes = calculateRowSizes(style, nodeInnerSize, innerMinSize.height, innerMaxSize.height,
                availableGridSpace, gap, numRows, items, columnSizes, colCounts, rowCounts, expandedRows);
            // Re-apply the auto-fit collapse after the re-run (GR-07 / upstream #946)
            applyAutoFitCollapse(rowSizes, rowAutoFit.collapsed());
        }

        // Calculate container size based on tracks.
        // By default the initial (first-pass) sums determine the container's content size, which is
        // what CSS Grid 11.8 specifies for percentage-track resolution steps. Upstream #960 adds one
        // exception: when an intrinsic-axis contribution changed during the re-run check and that
        // axis has no percentage tracks, the container's size is recomputed from the final track
        // sizes, because the first pass had sized those tracks against an indefinite container.
        float contentWidth = initialColumnSum;
        float contentHeight = initialRowSum;

        if (intrinsicColumnContributionChanged && !hasPercentageColumn) {
            contentWidth = columnGeometry.usedSize(columnSizes, gap.width);
            // Keep the downstream alignment/positioning pass in sync with the recomputed container.
            float recomputedBorderWidth = !Float.isNaN(styledBasedKnownDimensions.width)
                                          ? styledBasedKnownDimensions.width
                                          : contentWidth + contentBoxInsetWidth;
            recomputedBorderWidth = Math.max(TaffyMath.clamp(recomputedBorderWidth, minSize.width, maxSize.width),
                paddingBorderSize.width);
            nodeInnerSize = new FloatSize(Math.max(0f, recomputedBorderWidth - contentBoxInsetWidth),
                nodeInnerSize.height);
        }

        if (intrinsicRowContributionChanged && !hasPercentageRow) {
            contentHeight = rowGeometry.usedSize(rowSizes, gap.height);
            float recomputedBorderHeight = !Float.isNaN(styledBasedKnownDimensions.height)
                                           ? styledBasedKnownDimensions.height
                                           : contentHeight + contentBoxInsetHeight;
            recomputedBorderHeight = Math.max(TaffyMath.clamp(recomputedBorderHeight, minSize.height, maxSize.height),
                paddingBorderSize.height);
            nodeInnerSize = new FloatSize(nodeInnerSize.width,
                Math.max(0f, recomputedBorderHeight - contentBoxInsetHeight));
        }

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

            // Resolve the grid lines used by absolutely positioned children (OF-08/GR-14). The lines are
            // logical (from the content box's inline-start) and include the content alignment offset.
            ContentAlignment alignment = computeContentAlignment(style, columnSizes, rowSizes, gap, nodeInnerSize,
                columnGeometry, rowGeometry);

            // Get direction for RTL support (resolve INHERIT)
            boolean isRtlEmpty = layoutComputer.resolveDirection(node) == TaffyDirection.RTL;

            GridAxisLines columnLines = new GridAxisLines(columnSizes, columnGeometry, alignment.gapX(),
                alignment.offsetX(), contentBoxInset.left, alignment.innerWidth(), isRtlEmpty);
            GridAxisLines rowLines = new GridAxisLines(rowSizes, rowGeometry, alignment.gapY(),
                alignment.offsetY(), contentBoxInset.top, alignment.innerHeight(), false);

            // Layout absolutely positioned children even if no regular items. Their overflow is
            // accumulated in the container's padding-box frame and merged below (upstream oof.rs).
            FloatRect absoluteOverflowRect = layoutAbsoluteChildren(node, containerSize, border, scrollbarGutterX,
                scrollbarGutterY, colCounts, rowCounts, columnLines, rowLines, isRtlEmpty, namedLineResolver);

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

            // The container itself contributes its own padding to the scrollable overflow region
            // (upstream #1051/#1114, grid/mod.rs). With no in-flow item the in-flow accumulator is
            // empty, so this union is the whole in-flow part of the rectangle.
            boolean isEmptyScrollContainer = ScrollableOverflowUtil.isScrollContainer(overflow);
            FloatSize emptyClientSize = clientSize(containerSize, border, scrollbarGutterX, scrollbarGutterY);
            ScrollableOverflow emptyOverflow = ScrollableOverflowUtil.finalizeScrollableOverflow(
                ScrollableOverflowUtil.union(
                    containerPaddingOverflowRect(emptyClientSize, padding, isRtlEmpty, isEmptyScrollContainer),
                    absoluteOverflowRect),
                emptyClientSize,
                isRtlEmpty,
                false,
                isEmptyScrollContainer);
            return layoutOutput(containerSize, contentSize, emptyOverflow, new FloatPoint(NaN, NaN));
        }

        // Resolve the grid lines used by absolutely positioned children (OF-08/GR-14). The lines are
        // logical (from the content box's inline-start) and include the content alignment offset.
        ContentAlignment alignment = computeContentAlignment(style, columnSizes, rowSizes, gap, nodeInnerSize,
            columnGeometry, rowGeometry);

        // Get direction for RTL/LTR support (resolve INHERIT)
        TaffyDirection direction = layoutComputer.resolveDirection(node);
        boolean isRtl = direction == TaffyDirection.RTL;
        // The inline axis is reversed in RTL: lines are measured from the right content edge.
        GridAxisLines columnLines = new GridAxisLines(columnSizes, columnGeometry, alignment.gapX(),
            alignment.offsetX(), contentBoxInset.left, alignment.innerWidth(), isRtl);
        GridAxisLines rowLines = new GridAxisLines(rowSizes, rowGeometry, alignment.gapY(),
            alignment.offsetY(), contentBoxInset.top, alignment.innerHeight(), false);

        // Container size. Computed before the item positioning pass because the scrollable overflow
        // container description (client area) depends on the final container size.
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

        // The scrollable overflow of the container is accumulated in its padding-box frame: item
        // locations are shifted by the padding-box origin (the border edge; this port keeps the RTL
        // scrollbar gutter on the right, so the origin is the left border edge in both directions).
        boolean isScrollContainer = ScrollableOverflowUtil.isScrollContainer(overflow);
        FloatSize clientSize = clientSize(containerSize, border, scrollbarGutterX, scrollbarGutterY);
        FloatPoint paddingBoxOrigin = new FloatPoint(border.left, border.top);
        ScrollableOverflowUtil.Container inFlowOverflowContainer = new ScrollableOverflowUtil.Container(
            isScrollContainer, clientSize, isRtl, false, padding);

        // Place items and calculate final positions, accumulating their overflow contributions
        FloatRect itemOverflowRect = placeItems(items, columnSizes, rowSizes, gap, contentBoxInset, nodeInnerSize,
            style, colCounts, rowCounts, columnGeometry, rowGeometry, isRtl, paddingBoxOrigin, inFlowOverflowContainer);

        if (runMode == RunMode.COMPUTE_SIZE) {
            return LayoutOutput.fromOuterSize(containerSize);
        }

        // Perform final layout
        performFinalLayout(items);

        // Layout absolutely positioned children, accumulating their overflow contributions
        FloatRect absoluteOverflowRect = layoutAbsoluteChildren(node, containerSize, border, scrollbarGutterX,
            scrollbarGutterY, colCounts, rowCounts, columnLines, rowLines, isRtl, namedLineResolver);

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

        // The container's own end padding joins the in-flow rectangle (upstream #1051/#1114) and
        // the absolutely positioned children contribute their own rectangles (upstream oof.rs).
        // A single finalize is equivalent to upstream's finalize-then-union-then-finalize pass:
        // union and the per-edge clipping commute.
        ScrollableOverflow scrollableOverflow = ScrollableOverflowUtil.finalizeScrollableOverflow(
            ScrollableOverflowUtil.union(
                ScrollableOverflowUtil.union(
                    itemOverflowRect,
                    containerPaddingOverflowRect(clientSize, padding, isRtl, isScrollContainer)),
                absoluteOverflowRect),
            clientSize,
            isRtl,
            false,
            isScrollContainer);

        return layoutOutput(containerSize, contentSize, scrollableOverflow, new FloatPoint(NaN, containerBaseline));
    }

    /**
     * The container's client area, in the container's border-box coordinate space: the border box
     * less its border and the scrollbar gutters (upstream's {@code container_border_box - border -
     * scrollbar_gutter}). The extent is floored at zero.
     */
    private static FloatSize clientSize(FloatSize containerSize, FloatRect border,
                                       float scrollbarGutterX, float scrollbarGutterY) {
        return new FloatSize(
            Math.max(0f, containerSize.width - border.left - border.right - scrollbarGutterX),
            Math.max(0f, containerSize.height - border.top - border.bottom - scrollbarGutterY)
        );
    }

    /**
     * The rectangle the grid container itself contributes to its scrollable overflow region.
     *
     * <p>This is upstream {@code grid/mod.rs}'s {@code is_scroll_container} union (SO-02): a scroll
     * container's own end padding is part of its reachable region (bottom/right in LTR, the client
     * width on the reversed inline axis in RTL), while a container that is not a scroll container
     * only reaches its client area's inline end in RTL (its client area is the region the content
     * is aligned in). Unioning these rectangles with the children's contributions means a negative
     * end margin can never shrink the region below the container's own padding.</p>
     *
     * @param clientSize        the container's client-area extent
     * @param padding           the container's resolved padding
     * @param rtl               whether the container's inline axis is reversed
     * @param isScrollContainer whether either axis is a scroll container
     * @return the container's own contribution in its padding-box frame
     */
    private static FloatRect containerPaddingOverflowRect(FloatSize clientSize, FloatRect padding,
                                                          boolean rtl, boolean isScrollContainer) {
        if (!isScrollContainer) {
            return rtl ? new FloatRect(0f, clientSize.width, 0f, 0f) : FloatRect.zero();
        }
        return rtl
               ? new FloatRect(Math.min(clientSize.width - padding.left, 0f), clientSize.width, 0f, padding.bottom)
               : new FloatRect(0f, padding.right, 0f, padding.bottom);
    }

    /**
     * Builds a container's {@link LayoutOutput} with its scrollable overflow rectangle, keeping the
     * legacy {@code contentSize}/{@code firstBaselines} semantics of
     * {@link LayoutOutput#fromSizesAndBaselines}. The canonical record constructor is used so the
     * computed rectangle reaches the public {@link Layout} instead of being replaced by
     * {@link ScrollableOverflow#ZERO} (SO-01).
     */
    private static LayoutOutput layoutOutput(FloatSize size, FloatSize contentSize,
                                            ScrollableOverflow scrollableOverflow, FloatPoint firstBaselines) {
        return new LayoutOutput(
            size,
            contentSize,
            scrollableOverflow,
            firstBaselines,
            Baselines.fromFirst(firstBaselines),
            CollapsibleMarginSet.ZERO,
            CollapsibleMarginSet.ZERO,
            false
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
     * <p>
     * The baseline is taken from the final layout of the first item of the first row (upstream
     * #1322): during track sizing an item's baseline is only measured from its intrinsic layout,
     * and an item that does not participate in baseline alignment has none at all, so using the
     * measured value made the container report the item's bottom edge instead of its real first
     * baseline.
     * <p>
     * Only items participating in baseline alignment can provide the container's baseline
     * (upstream #1125 / 3dbf2e46): if the row's first item does not participate, the row's first
     * participating item is used, and the row's first item is the fallback. The item's relative
     * offset is excluded ({@link GridItem#yPosition}, upstream #1323), and a scroll container's
     * baseline is clamped to its border box by the final-layout pass (#1108).
     */
    private float calculateContainerBaseline(List<GridItem> items) {
        if (items.isEmpty()) {
            return NaN;
        }

        // The first row is the one with the lowest row start index
        int firstRow = Integer.MAX_VALUE;
        for (GridItem item : items) {
            firstRow = Math.min(firstRow, item.rowStart != null ? item.rowStart : 0);
        }

        // The row's first item (in document order) and its first baseline participant
        GridItem firstItemInRow = null;
        GridItem chosenItem = null;
        for (GridItem item : items) {
            int rowStart = item.rowStart != null ? item.rowStart : 0;
            if (rowStart != firstRow) continue;
            if (firstItemInRow == null) {
                firstItemInRow = item;
            }
            if (chosenItem == null && item.participatesInBaselineAlignment()) {
                chosenItem = item;
            }
        }
        if (chosenItem == null) {
            chosenItem = firstItemInRow;
        }

        // Calculate baseline: y_position (without relative offset) + border-box baseline
        // (or the border-box height when the item reports none)
        float height = (chosenItem.computedSize != null && !Float.isNaN(chosenItem.computedSize.height))
                       ? chosenItem.computedSize.height : 0f;
        float baseline = (!Float.isNaN(chosenItem.baseline)) ? chosenItem.baseline : height;

        return chosenItem.yPosition + baseline;
    }

    /**
     * Content-alignment geometry for both axes, used to position absolutely positioned children (OF-08)
     * and to position the single grid line of an empty axis (GR-14).
     * <p>
     * {@code offsetX}/{@code offsetY} are the offsets of the first non-collapsed track from the content
     * box's inline-start edge, {@code gapX}/{@code gapY} are the gaps used between non-collapsed tracks
     * (content alignment distributes free space into the gaps for the space-* keywords), and
     * {@code innerWidth}/{@code innerHeight} are the definite content box sizes (or the used track size
     * when the container is indefinite in that axis).
     */
    private record ContentAlignment(
        float offsetX, float gapX, float innerWidth,
        float offsetY, float gapY, float innerHeight) {}

    /**
     * Compute the content alignment (justify-content/align-content) geometry used by the absolute
     * positioning path. This mirrors the alignment computation performed by {@link #placeItems} so that
     * absolutely positioned children resolve their grid lines to the same (content-aligned) track edges.
     */
    private ContentAlignment computeContentAlignment(
        TaffyStyle style,
        FloatList columnSizes,
        FloatList rowSizes,
        FloatSize gap,
        FloatSize nodeInnerSize,
        AxisGeometry columnGeometry,
        AxisGeometry rowGeometry) {

        int nonCollapsedColumns = columnGeometry.nonCollapsedTrackCount();
        float totalColumnSize = columnGeometry.usedSize(columnSizes, gap.width);

        int nonCollapsedRows = rowGeometry.nonCollapsedTrackCount();
        float totalRowSize = rowGeometry.usedSize(rowSizes, gap.height);

        float innerWidth = !Float.isNaN(nodeInnerSize.width) ? nodeInnerSize.width : totalColumnSize;
        float innerHeight = !Float.isNaN(nodeInnerSize.height) ? nodeInnerSize.height : totalRowSize;
        float freeSpaceWidth = innerWidth - totalColumnSize;
        float freeSpaceHeight = innerHeight - totalRowSize;

        JustifyContent justifyContent = style.getJustifyContent();
        AlignContent alignContent = style.getAlignContent();

        // An axis without any non-collapsed track still contains a single grid line, which content
        // alignment positions within the container (GR-14 / upstream #1078).
        float offsetX = nonCollapsedColumns == 0
                        ? calculateEmptyAxisAlignmentOffset(justifyContent, freeSpaceWidth)
                        : calculateContentAlignmentOffset(justifyContent, freeSpaceWidth, nonCollapsedColumns);
        float offsetY = nonCollapsedRows == 0
                        ? calculateEmptyAxisAlignmentOffset(alignContent, freeSpaceHeight)
                        : calculateContentAlignmentOffset(alignContent, freeSpaceHeight, nonCollapsedRows);

        float gapX = calculateAdjustedGap(justifyContent, freeSpaceWidth, nonCollapsedColumns, gap.width);
        float gapY = calculateAdjustedGap(alignContent, freeSpaceHeight, nonCollapsedRows, gap.height);

        return new ContentAlignment(offsetX, gapX, innerWidth, offsetY, gapY, innerHeight);
    }

    /**
     * The offset of the (single) grid line of an axis without any non-collapsed track. The distribution
     * keywords fall back to their single-item fallbacks (space-* behave like center, stretch and
     * space-between behave like start) and a {@code safe} keyword falls back to start on overflow,
     * matching upstream's {@code apply_alignment_fallback} + {@code compute_alignment_offset} for an
     * empty grid (GR-14 / #1078, ST-02 / #952).
     */
    private static float calculateEmptyAxisAlignmentOffset(AlignContent alignment, float freeSpace) {
        if (alignment == null) return 0;
        AlignContent effective = AlignmentHelper.applyAlignmentFallback(
            resolveGridContentAlignment(alignment), freeSpace, 0);
        return AlignmentHelper.computeAlignmentOffset(freeSpace, 0, 0f, effective, false, true);
    }

    private static float calculateEmptyAxisAlignmentOffset(JustifyContent alignment, float freeSpace) {
        if (alignment == null) return 0;
        JustifyContent effective = AlignmentHelper.applyAlignmentFallback(
            resolveGridContentAlignment(alignment), freeSpace, 0);
        return AlignmentHelper.computeAlignmentOffset(freeSpace, 0, 0f, effective, false, true);
    }

    /**
     * The grid line positions of one axis, used to resolve absolutely positioned children (OF-08).
     * <p>
     * Positions are logical: measured from the content box's inline-start edge (the left content edge for
     * LTR, the right content edge for RTL) and growing towards the inline-end, with the content alignment
     * offset and the space-* gap distribution already applied. {@link #physical(float)} converts a logical
     * position into container coordinates.
     * <p>
     * Grid lines used by absolutely positioned children resolve to the edges of the tracks adjacent to the
     * line rather than to the raw gutter offset (upstream #1071):
     * <ul>
     *   <li>as a start edge, a line resolves to the start of the first non-collapsed track that follows it;</li>
     *   <li>as an end edge, a line resolves to the end of the last non-collapsed track that precedes it.</li>
     * </ul>
     * so that free space distributed by align-content/justify-content is not included in the grid area, and
     * gutters adjacent to collapsed tracks (which overlap) are not counted twice (upstream #1266).
     */
    private static final class GridAxisLines {
        private final float[] starts;   // logical start position of each track; the last entry is the end of the grid
        private final float[] ends;     // logical end position of each track
        private final boolean[] nonCollapsed;
        private final float origin;
        private final float innerSize;
        private final boolean reversed;

        GridAxisLines(FloatList trackSizes, AxisGeometry geometry, float adjustedGap, float firstTrackOffset,
                      float origin, float innerSize, boolean reversed) {
            int trackCount = trackSizes.size();
            this.starts = new float[trackCount + 1];
            this.ends = new float[trackCount];
            this.nonCollapsed = new boolean[trackCount];
            this.origin = origin;
            this.innerSize = innerSize;
            this.reversed = reversed;

            float position = firstTrackOffset;
            for (int i = 0; i < trackCount; i++) {
                nonCollapsed[i] = !geometry.isCollapsed(i);
                // A gutter survives unless the track it follows is collapsed (GR-07 / upstream #946);
                // {@link AxisGeometry} already accounts for the trailing gutter and the auto-fit at
                // the end of the template collapsing its preceding gutter.
                if (i > 0 && geometry.hasGapAfter(i - 1)) {
                    position += adjustedGap;
                }
                starts[i] = position;
                position += trackSizes.getFloat(i);
                ends[i] = position;
            }
            starts[trackCount] = position;
        }

        /** Convert a logical position (from the inline-start edge) into container coordinates. */
        float physical(float logical) {
            return reversed ? origin + innerSize - logical : origin + logical;
        }

        /** The physical position of a grid line used as a start edge. */
        float startEdge(int lineIndex) {
            int from = Math.max(0, lineIndex);
            for (int i = from; i < nonCollapsed.length; i++) {
                if (nonCollapsed[i]) return physical(starts[i]);
            }
            return physical(starts[starts.length - 1]);
        }

        /** The physical position of a grid line used as an end edge. */
        float endEdge(int lineIndex) {
            for (int i = Math.min(lineIndex, nonCollapsed.length) - 1; i >= 0; i--) {
                if (nonCollapsed[i]) return physical(ends[i]);
            }
            // No track precedes this line: resolve to the start of the first track that follows it.
            for (int i = Math.max(0, lineIndex); i < nonCollapsed.length; i++) {
                if (nonCollapsed[i]) return physical(starts[i]);
            }
            return physical(starts[starts.length - 1]);
        }
    }

    /**
     * Create a {@link NamedLineResolver} for a grid container.
     * <p>
     * The explicit track counts are derived from the <em>expanded</em> templates (so that a
     * {@code repeat()} contributes its expanded track count, GR-17 / #1035) combined with the
     * {@code grid-template-areas} template size (which defaults to the extents of the named areas
     * but may be larger because of unnamed "." cells, GR-16 / upstream #1024), and clamped into the
     * limited grid. The same resolver
     * instance is used by the in-flow placement pass and by the absolutely positioned children, so
     * that named-line lookup and fallback numbering are identical on both paths and named lines are
     * resolved exactly once before the grid size estimate (GR-15 / #1259).
     *
     * @param containerStyle      the grid container's style
     * @param expandedColumns     the column template with auto-repetitions expanded
     * @param expandedRows        the row template with auto-repetitions expanded
     * @param columnAutoRepetitions the number of times the column auto-repetition is expanded
     * @param rowAutoRepetitions    the number of times the row auto-repetition is expanded
     */
    private NamedLineResolver createNamedLineResolver(TaffyStyle containerStyle,
                                                      List<TrackSizingFunction> expandedColumns,
                                                      List<TrackSizingFunction> expandedRows,
                                                      int columnAutoRepetitions,
                                                      int rowAutoRepetitions) {
        NamedLineResolver namedLineResolver =
            new NamedLineResolver(containerStyle, columnAutoRepetitions, rowAutoRepetitions);
        // The area template size is taken from the style rather than derived from the named areas:
        // unnamed (".") cells can make the template larger than the extents of the named areas and
        // they still contribute explicit tracks (GR-16 / upstream #1024).
        namedLineResolver.setExplicitColumnCount(
            explicitTrackCount(expandedColumns, containerStyle.getGridTemplateAreaColumnCount()));
        namedLineResolver.setExplicitRowCount(
            explicitTrackCount(expandedRows, containerStyle.getGridTemplateAreaRowCount()));
        return namedLineResolver;
    }

    /**
     * The number of explicit tracks in one axis: the expanded template track count, extended to the
     * {@code grid-template-areas} template size and clamped into the limited grid (upstream
     * {@code explicit_col_count = max(template_count, area_count).min(MAX_GRID_TRACKS)}).
     */
    private static int explicitTrackCount(List<TrackSizingFunction> expandedTemplate, int areaTrackCount) {
        int templateCount = (expandedTemplate != null) ? Math.min(expandedTemplate.size(), MAX_GRID_TRACKS) : 0;
        return Math.max(templateCount, Math.min(areaTrackCount, MAX_GRID_TRACKS));
    }

    private List<GridItem> generateGridItems(NodeId node, TaffyStyle containerStyle, FloatSize nodeInnerSize,
                                             NamedLineResolver namedLineResolver) {
        TaffyTree tree = layoutComputer.getTree();
        List<GridItem> items = new ArrayList<>();

        // Explicit track counts for negative line number resolution. They come from the resolver so
        // that in-flow items, absolutely positioned items and named-line fallbacks all number their
        // lines against the same (expanded template + grid areas) explicit grid.
        int explicitColCount = namedLineResolver.getExplicitColumnCount();
        int explicitRowCount = namedLineResolver.getExplicitRowCount();

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
            // Keep the raw styles: percentage padding/border/size/min/max/margin resolve against the
            // grid area during intrinsic sizing and placement (GR-06 / upstream #960).
            item.rawPadding = childStyle.getPadding();
            item.rawBorder = childStyle.getBorder();
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
            // TODO(T17/T4): upstream #1351 resolves the max-size basis of the minimum contribution
            // against zero only for replaced elements; the cap below still applies to every
            // compressible replaced item. Wiring that split needs the "definite preferred size
            // clamped by min/max" branch from #1022/#1282, which the minimum contribution does not
            // have yet.
            item.isCompressibleReplaced = childStyle.isCompressibleReplaced();

            // Alignment for track sizing: use child's preference, or parent's default, or STRETCH.
            // This merged value is used during track sizing (GridItem#computeKnownDimensions), where
            // upstream's GridItem::is_stretch_aligned treats both 'stretch' and (non-replaced)
            // 'normal' as stretching (#1254). Final placement (placeItems) re-reads the raw values to
            // apply the grid item 'normal' rules and self-relative resolution.
            AlignItems parentAlignItems = containerStyle.getAlignItems();
            AlignItems parentJustifyItems = containerStyle.justifyItems;
            AlignItems resolvedAlignItems = resolveTrackSizingAlignment(parentAlignItems);
            AlignItems resolvedJustifyItems = resolveTrackSizingAlignment(parentJustifyItems);

            AlignItems rawAlignSelf = childStyle.getAlignSelf();
            AlignItems rawJustifySelf = childStyle.justifySelf;
            item.alignSelf = (rawAlignSelf != null && rawAlignSelf != AlignItems.AUTO)
                             ? resolveTrackSizingAlignment(rawAlignSelf)
                             : resolvedAlignItems;
            item.justifySelf = (rawJustifySelf != null && rawJustifySelf != AlignItems.AUTO)
                               ? resolveTrackSizingAlignment(rawJustifySelf)
                               : resolvedJustifyItems;

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

            if (!item.position.isOutOfFlow()) {
                items.add(item);
            }
        }

        return items;
    }

    /**
     * Resolve the alignment used for <em>track sizing</em> (upstream {@code GridItem::new} plus
     * {@code is_stretch_aligned}, #1254).
     *
     * <p>An unset ({@link AlignItems#AUTO}) or explicit {@link AlignItems#NORMAL} alignment stretches
     * during track sizing, matching upstream (which treats {@code normal} as {@code stretch} here
     * regardless of the item's preferred size). Other keywords are preserved, in particular
     * {@code baseline} and {@code self-start}/{@code self-end}.</p>
     *
     * @param alignment the item-level alignment, or the container's align-items/justify-items
     * @return the alignment to use during track sizing
     */
    private static AlignItems resolveTrackSizingAlignment(AlignItems alignment) {
        if (alignment == null || alignment == AlignItems.AUTO || alignment == AlignItems.NORMAL) {
            return AlignItems.STRETCH;
        }
        return alignment;
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
            span = clampGridSpan(start.getValue());
        }
        if (end.isSpan()) {
            span = clampGridSpan(end.getValue());
        }

        // If we have start and end, compute span
        if (startIndex != null && endIndex != null) {
            span = clampGridSpan(endIndex - startIndex);
        }
        // If we have start and span but no end
        else if (startIndex != null && end.isSpan()) {
            endIndex = clampOzLine((long) startIndex + span);
        }
        // If we have end and span but no start
        else if (endIndex != null && start.isSpan()) {
            startIndex = clampOzLine((long) endIndex - span);
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
            // Clamped into the limited grid (GR-05 / #986).
            return clampOzLine((long) lineNumber - 1);
        } else {
            // Negative: line -1 = last line (explicitTrackCount), line -2 = second to last, etc.
            // explicitLineCount = explicitTrackCount + 1
            // line -1 -> explicitTrackCount + 1 + (-1) = explicitTrackCount
            // line -2 -> explicitTrackCount + 1 + (-2) = explicitTrackCount - 1
            // Clamped into the limited grid (GR-05 / #986).
            long explicitLineCount = (long) explicitTrackCount + 1;
            return clampOzLine(explicitLineCount + lineNumber);
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

        // Summarize the template's repetitions (GR-07 / upstream #946). An invalid template (a
        // repetition with no tracks, more than one auto-repetition, or an auto-repetition combined
        // with a non-fixed track) contributes no explicit tracks at all.
        TemplateSummary summary = summarizeTemplate(template);
        if (!summary.isValid()) {
            return new ArrayList<>();
        }

        GridRepetition autoRepeat = summary.firstAutoRepetitionIndex() >= 0
                                    ? template.get(summary.firstAutoRepetitionIndex()).getRepeat() : null;
        int autoRepeatIndex = summary.firstAutoRepetitionIndex();

        if (autoRepeat == null) {
            // No auto-repetition, just expand normally. The expanded template is truncated at the
            // track limit so that a large explicit repetition count cannot create an unbounded
            // number of tracks (GR-05 / #986 "clamp auto-repeat tracks at their insertion point").
            List<TrackSizingFunction> result = new ArrayList<>();
            for (GridTemplateComponent comp : template) {
                if (comp.isSingle()) {
                    if (result.size() >= MAX_GRID_TRACKS) break;
                    result.add(comp.getSingle());
                } else if (comp.isRepeat()) {
                    GridRepetition repeat = comp.getRepeat();
                    int count = repeat.getCount();
                    for (int i = 0; i < count; i++) {
                        for (TrackSizingFunction track : repeat.getTracks()) {
                            if (result.size() >= MAX_GRID_TRACKS) return result;
                            result.add(track);
                        }
                    }
                }
            }
            return result;
        }

        // Has auto-repetition - calculate how many times to repeat
        int numRepetitions = computeAutoRepetitionCount(template, autoRepeatIndex, autoRepeat,
            containerSize, gap, summary);

        // Build result with expanded repetitions, truncating the expanded template at the track limit.
        List<TrackSizingFunction> result = new ArrayList<>();
        for (int i = 0; i < template.size(); i++) {
            GridTemplateComponent comp = template.get(i);
            if (i == autoRepeatIndex) {
                // Expand auto-repetition
                for (int j = 0; j < numRepetitions; j++) {
                    for (TrackSizingFunction track : autoRepeat.getTracks()) {
                        if (result.size() >= MAX_GRID_TRACKS) return result;
                        result.add(track);
                    }
                }
            } else if (comp.isSingle()) {
                if (result.size() >= MAX_GRID_TRACKS) return result;
                result.add(comp.getSingle());
            } else if (comp.isRepeat()) {
                GridRepetition repeat = comp.getRepeat();
                for (int j = 0; j < repeat.getCount(); j++) {
                    for (TrackSizingFunction track : repeat.getTracks()) {
                        if (result.size() >= MAX_GRID_TRACKS) return result;
                        result.add(track);
                    }
                }
            }
        }
        return result;
    }

    /**
     * The track-count facts about a {@code grid-template-*} definition that upstream's
     * {@code compute_explicit_grid_size_in_axis} derives in a single pass (GR-07 / upstream #946):
     * the number of tracks contributed by every single-track definition and every fixed-count
     * {@code repeat()}, the number of auto-repetitions, and the validity of the template.
     * <p>
     * Per CSS Grid, an auto-fill/auto-fit repetition may appear at most once, and the template may
     * not combine it with tracks that have no fixed sizing component; a repetition with no tracks
     * makes the whole definition invalid. An invalid template contributes no explicit tracks
     * (upstream returns {@code (0, 0)} and {@code initialize_grid_tracks} then skips the template).
     *
     * @param nonAutoRepeatingTrackCount      tracks from single definitions and fixed-count repeats
     * @param autoRepetitionCount             number of auto-fill/auto-fit repetitions
     * @param firstAutoRepetitionIndex        index of the first auto-repetition, or -1
     * @param hasZeroTrackRepetition          whether any repetition declares no tracks
     * @param allTrackDefsHaveFixedComponent  whether every track has a fixed sizing component
     */
    private record TemplateSummary(int nonAutoRepeatingTrackCount, int autoRepetitionCount,
                                   int firstAutoRepetitionIndex, boolean hasZeroTrackRepetition,
                                   boolean allTrackDefsHaveFixedComponent) {
        boolean isValid() {
            if (hasZeroTrackRepetition) return false;
            if (autoRepetitionCount == 0) return true;
            return autoRepetitionCount == 1 && allTrackDefsHaveFixedComponent;
        }
    }

    /**
     * Summarize the repetitions of a grid template, mirroring upstream
     * {@code compute_explicit_grid_size_in_axis} (GR-07 / upstream #946).
     */
    private static TemplateSummary summarizeTemplate(List<GridTemplateComponent> template) {
        int nonAutoRepeatingTrackCount = 0;
        int autoRepetitionCount = 0;
        int firstAutoRepetitionIndex = -1;
        boolean hasZeroTrackRepetition = false;
        boolean allTrackDefsHaveFixedComponent = true;

        for (int i = 0; i < template.size(); i++) {
            GridTemplateComponent comp = template.get(i);
            if (comp == null) continue;
            if (comp.isSingle()) {
                nonAutoRepeatingTrackCount = saturatingAdd(nonAutoRepeatingTrackCount, 1);
                allTrackDefsHaveFixedComponent &= hasUpstreamFixedComponent(comp.getSingle());
            } else if (comp.isRepeat()) {
                GridRepetition repeat = comp.getRepeat();
                if (repeat == null || repeat.getTrackCount() == 0) {
                    hasZeroTrackRepetition = true;
                    allTrackDefsHaveFixedComponent = false;
                    continue;
                }
                if (repeat.isAutoRepetition()) {
                    if (firstAutoRepetitionIndex < 0) firstAutoRepetitionIndex = i;
                    autoRepetitionCount = saturatingAdd(autoRepetitionCount, 1);
                } else {
                    nonAutoRepeatingTrackCount = saturatingAdd(nonAutoRepeatingTrackCount,
                        (int) Math.min(Integer.MAX_VALUE, (long) repeat.getCount() * repeat.getTrackCount()));
                }
                for (TrackSizingFunction track : repeat.getTracks()) {
                    allTrackDefsHaveFixedComponent &= hasUpstreamFixedComponent(track);
                }
            }
        }

        return new TemplateSummary(Math.min(nonAutoRepeatingTrackCount, MAX_GRID_TRACKS),
            autoRepetitionCount, firstAutoRepetitionIndex, hasZeroTrackRepetition,
            allTrackDefsHaveFixedComponent);
    }

    /**
     * Whether a track sizing function has a fixed component, using upstream's definition
     * ({@code TrackSizingFunction::has_fixed_component}: the min or max sizing function is a
     * length/percentage). Note that {@code fit-content()} does <em>not</em> count: upstream treats a
     * template that combines an auto-repetition with a {@code fit-content()} track as invalid,
     * whereas {@link TrackSizingFunction#hasFixedComponent()} (a legacy public helper) counts the
     * fit-content() limit as fixed. Keeping the stricter rule here avoids changing that public
     * helper while matching upstream's template validation (GR-07 / #946).
     */
    private static boolean hasUpstreamFixedComponent(TrackSizingFunction track) {
        if (track == null) {
            return false;
        }
        if (track.isFixed()) {
            return track.getFixedValue() != null;
        }
        if (track.isMinmax()) {
            return hasUpstreamFixedComponent(track.getMinFunc())
                   || hasUpstreamFixedComponent(track.getMaxFunc());
        }
        return false;
    }

    /**
     * A track sizing function's definite value against a parent size, or 0 when it has none
     * (upstream {@code track_definite_value} in {@code compute_explicit_grid_size_in_axis}).
     */
    private static float definiteTrackValue(TrackSizingFunction track, float parentSize) {
        if (track == null) return 0f;
        float value = track.getDefiniteValue(parentSize);
        return Float.isNaN(value) ? 0f : value;
    }

    /**
     * The number of times the auto-repetition ({@code auto-fill}/{@code auto-fit}) at
     * {@code autoRepeatIndex} is expanded in the axis with the passed definite container size and
     * gap.
     * <p>
     * The result is the largest number of repetitions that does not overflow the container (or 1
     * when the container size is indefinite), clamped into the limited grid at the repetition's
     * insertion point so that the tracks before it are preserved and the tracks after it are
     * truncated in source order (https://www.w3.org/TR/css-grid-1/#overlarge-grids, GR-05 / #986).
     * <p>
     * The named-line resolver (GR-17 / #1035) uses the very same count so that the line names of
     * the tracks generated by the repetition are numbered identically to the expanded template.
     *
     * @param template        the grid template containing the auto-repetition
     * @param autoRepeatIndex the index of the auto-repetition component in {@code template}
     * @param autoRepeat      the auto-repetition component
     * @param containerSize   the definite container size in this axis
     * @param gap             the gap between tracks in this axis
     * @param summary         the template's repetition summary (upstream #946)
     */
    private int computeAutoRepetitionCount(List<GridTemplateComponent> template, int autoRepeatIndex,
                                           GridRepetition autoRepeat, float containerSize, float gap,
                                           TemplateSummary summary) {
        int numRepetitions;
        if (Float.isNaN(containerSize) || containerSize <= 0) {
            // No definite container size, use 1 repetition
            numRepetitions = 1;
        } else {
            // Calculate space used by the non-auto-repeating tracks. Every non-auto track counts
            // towards the number of gutters even when it has no definite value (upstream #946: the
            // gutter count is non_auto_repeating_track_count, not the count of definite tracks).
            float nonRepeatingUsedSpace = 0f;
            for (int i = 0; i < template.size(); i++) {
                if (i == autoRepeatIndex) continue;
                GridTemplateComponent comp = template.get(i);
                if (comp.isSingle()) {
                    nonRepeatingUsedSpace += definiteTrackValue(comp.getSingle(), containerSize);
                } else if (comp.isRepeat()) {
                    GridRepetition repeat = comp.getRepeat();
                    if (repeat == null || repeat.isAutoRepetition()) continue;
                    float perTrackSum = 0f;
                    for (TrackSizingFunction track : repeat.getTracks()) {
                        perTrackSum += definiteTrackValue(track, containerSize);
                    }
                    nonRepeatingUsedSpace += perTrackSum * repeat.getCount();
                }
            }
            int nonRepeatingTrackCount = summary.nonAutoRepeatingTrackCount();

            // Calculate space per repetition
            float perRepetitionSpace = 0f;
            int tracksPerRepetition = autoRepeat.getTrackCount();
            for (TrackSizingFunction track : autoRepeat.getTracks()) {
                perRepetitionSpace += definiteTrackValue(track, containerSize);
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
                    // Clamp to the track limit before the float->int cast so that a huge (or
                    // infinite) container size cannot saturate to a negative value.
                    numRepetitions = 1 + Math.min(MAX_GRID_TRACKS, (int) Math.floor(remainingSpace / perRepTotal));
                }
            }
        }

        // Clamp the number of repetitions based on the auto-repeat's insertion point so that tracks
        // before it are preserved and tracks after it are truncated in source order
        // (https://www.w3.org/TR/css-grid-1/#overlarge-grids).
        int insertionPoint = autoRepeatInsertionPoint(template, autoRepeatIndex);
        return clampAutoRepetitionCount(insertionPoint, autoRepeat.getTrackCount(), numRepetitions);
    }

    /**
     * The number of times the auto-repetition of a template is expanded, or 0 when the template
     * has no {@code auto-fill}/{@code auto-fit} repetition or is invalid (GR-07 / upstream #946).
     */
    private int autoRepetitionCount(List<GridTemplateComponent> template, float containerSize, float gap) {
        if (template == null || template.isEmpty()) {
            return 0;
        }
        TemplateSummary summary = summarizeTemplate(template);
        if (!summary.isValid() || summary.firstAutoRepetitionIndex() < 0) {
            return 0;
        }
        int index = summary.firstAutoRepetitionIndex();
        return computeAutoRepetitionCount(template, index, template.get(index).getRepeat(),
            containerSize, gap, summary);
    }

    /**
     * The number of tracks declared by the template before the auto-repetition at {@code autoRepeatIndex}.
     * Used to clamp the auto-repetition into the limited grid while preserving source order (GR-05 / #986).
     */
    private static int autoRepeatInsertionPoint(List<GridTemplateComponent> template, int autoRepeatIndex) {
        int insertionPoint = 0;
        for (int i = 0; i < template.size() && i < autoRepeatIndex; i++) {
            GridTemplateComponent comp = template.get(i);
            if (comp.isSingle()) {
                insertionPoint = saturatingAdd(insertionPoint, 1);
            } else if (comp.isRepeat() && !comp.getRepeat().isAutoRepetition()) {
                GridRepetition repeat = comp.getRepeat();
                insertionPoint = saturatingAdd(insertionPoint,
                    (int) Math.min(Integer.MAX_VALUE, (long) repeat.getCount() * repeat.getTrackCount()));
            }
        }
        return Math.min(insertionPoint, MAX_GRID_TRACKS);
    }

    /**
     * Clamp an auto-repetition count into the limited grid: the tracks that follow the auto-repetition
     * are truncated in source order, so the number of repetitions is capped by the number of tracks
     * that remain after the insertion point (GR-05 / #986).
     *
     * @param insertionPoint       number of tracks declared before the auto-repetition
     * @param tracksPerRepetition  number of tracks in a single repetition
     * @param repetitions          the (content-sized) number of repetitions that fit in the container
     * @return the number of repetitions to emit (0 when there is no room for any track)
     */
    private static int clampAutoRepetitionCount(int insertionPoint, int tracksPerRepetition, int repetitions) {
        if (tracksPerRepetition <= 0) {
            // A repetition with no tracks emits nothing; keep a single repetition so the template is
            // still considered expanded rather than vanishing silently.
            return Math.max(1, repetitions);
        }
        int remainingTracks = MAX_GRID_TRACKS - insertionPoint;
        if (remainingTracks <= 0) {
            return 0;
        }
        int maxRepetitions = (int) (((long) remainingTracks + tracksPerRepetition - 1) / tracksPerRepetition);
        return Math.max(1, Math.min(repetitions, maxRepetitions));
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
        TemplateSummary summary = summarizeTemplate(template);
        if (!summary.isValid() || summary.firstAutoRepetitionIndex() < 0) {
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
                tracksBefore = saturatingAdd(tracksBefore, 1);
            } else if (comp.isRepeat()) {
                GridRepetition repeat = comp.getRepeat();
                tracksBefore = saturatingAdd(tracksBefore,
                    (int) Math.min(Integer.MAX_VALUE, (long) repeat.getCount() * repeat.getTrackCount()));
            }
        }
        tracksBefore = Math.min(tracksBefore, MAX_GRID_TRACKS);

        if (autoFit == null) {
            return null;
        }

        // Calculate number of repetitions with the same helper that expands the template, so that
        // the collapsed range stays aligned with the expanded template (GR-05 / #986).
        int numRepetitions = computeAutoRepetitionCount(template, autoFitIndex, autoFit, containerSize, gap, summary);

        int totalAutoFitTracks = (int) Math.min((long) numRepetitions * autoFit.getTrackCount(),
            Math.max(0, MAX_GRID_TRACKS - tracksBefore));
        return new AutoFitInfo(tracksBefore, totalAutoFitTracks, autoFit.getTrackCount());
    }

    /**
     * The auto-fit collapse state of one grid axis (GR-07 / upstream #946).
     *
     * @param collapsed     per-track flags: true for auto-fit tracks containing no in-flow item
     * @param autoFitIsLast whether the auto-fit repetition reaches the end of the track list
     */
    private record AutoFitCollapse(boolean[] collapsed, boolean autoFitIsLast) {}

    /**
     * Determine which auto-fit tracks of one axis are empty and therefore collapsed.
     * <p>
     * Per CSS Grid, every auto-fit track that contains no in-flow item is collapsed. Upstream
     * decides this from the occupancy matrix when it initializes the track list; this port decides
     * it from the placed items, which is equivalent because the in-flow items are exactly the
     * items whose grid areas were marked in the matrix.
     *
     * @param trackCount           The number of tracks in the axis (including implicit tracks)
     * @param template             The axis' template (to detect auto-fit), or null
     * @param items                The in-flow grid items
     * @param autoFitContainerSize The container size the template's auto-repeat was expanded against
     * @param gap                  The gap between tracks in this axis
     * @param trackCounts          Track counts information for this axis
     * @param horizontalAxis       true for columns, false for rows
     */
    private AutoFitCollapse computeAutoFitCollapse(int trackCount, List<GridTemplateComponent> template,
                                                   List<GridItem> items, float autoFitContainerSize, float gap,
                                                   TrackCounts trackCounts, boolean horizontalAxis) {
        boolean[] collapsed = new boolean[Math.max(0, trackCount)];
        if (template == null || template.isEmpty() || trackCount <= 0) {
            return new AutoFitCollapse(collapsed, false);
        }

        AutoFitInfo autoFitInfo = getAutoFitInfo(template, autoFitContainerSize, gap);
        if (autoFitInfo == null) {
            return new AutoFitCollapse(collapsed, false);
        }

        boolean[] trackOccupied = new boolean[trackCount];
        for (GridItem item : items) {
            if (item.position.isOutOfFlow()) {
                continue;
            }
            int start = horizontalAxis
                        ? getItemColumnWithCounts(item, 0, trackCount, trackCounts)
                        : getItemRowWithCounts(item, 0, trackCount, trackCounts);
            int span = horizontalAxis ? item.columnSpan : item.rowSpan;
            for (int track = Math.max(0, start); track < start + span && track < trackCount; track++) {
                trackOccupied[track] = true;
            }
        }

        // Collapse unoccupied auto-fit tracks. The auto-fit tracks are in the explicit grid region.
        int autoFitStart = trackCounts.negativeImplicit + autoFitInfo.startIndex;
        int autoFitEnd = autoFitStart + autoFitInfo.trackCount;
        for (int track = Math.max(0, autoFitStart); track < autoFitEnd && track < trackCount; track++) {
            if (!trackOccupied[track]) {
                collapsed[track] = true;
            }
        }

        // Upstream's is_last check: the auto-fit repetition is the last component of the track list.
        return new AutoFitCollapse(collapsed, autoFitEnd >= trackCount);
    }

    /** Set the size of every collapsed track to 0 (their content was already excluded). */
    private static void applyAutoFitCollapse(FloatList trackSizes, boolean[] collapsed) {
        int count = Math.min(trackSizes.size(), collapsed.length);
        for (int i = 0; i < count; i++) {
            if (collapsed[i]) {
                trackSizes.set(i, 0f);
            }
        }
    }

    /**
     * Collapsed-aware geometry of one grid axis (GR-07 / upstream #946).
     * <p>
     * Upstream models gutters as tracks and collapses the gutter that follows a collapsed track.
     * The gutter that follows a non-collapsed track therefore survives even when every track after
     * it is collapsed; only the trailing gutter of the axis and - when the auto-fit repetition is
     * the last component of the track list - the gutter after the last non-collapsed track are
     * collapsed as well (upstream #946's backward pass). {@code gapAfter[i]} records the surviving
     * gutters, which makes a collapsed run in the middle of the template keep exactly one gap
     * before it and a collapsed run at the end of the template keep none.
     */
    private static final class AxisGeometry {
        private final boolean[] collapsed;
        private final boolean[] gapAfter;
        private final int nonCollapsedTrackCount;

        private AxisGeometry(boolean[] collapsed, boolean[] gapAfter, int nonCollapsedTrackCount) {
            this.collapsed = collapsed;
            this.gapAfter = gapAfter;
            this.nonCollapsedTrackCount = nonCollapsedTrackCount;
        }

        static AxisGeometry of(boolean[] collapsed, boolean autoFitIsLast) {
            int trackCount = collapsed.length;
            boolean[] gapAfter = new boolean[Math.max(0, trackCount - 1)];
            int nonCollapsedTrackCount = 0;
            int lastNonCollapsedTrack = -1;
            for (int i = 0; i < trackCount; i++) {
                if (!collapsed[i]) {
                    nonCollapsedTrackCount++;
                    lastNonCollapsedTrack = i;
                }
            }
            for (int i = 0; i < gapAfter.length; i++) {
                gapAfter[i] = !collapsed[i];
            }
            if (autoFitIsLast && lastNonCollapsedTrack >= 0 && lastNonCollapsedTrack < trackCount - 1) {
                gapAfter[lastNonCollapsedTrack] = false;
            }
            return new AxisGeometry(collapsed, gapAfter, nonCollapsedTrackCount);
        }

        boolean isCollapsed(int trackIndex) {
            return trackIndex >= 0 && trackIndex < collapsed.length && collapsed[trackIndex];
        }

        boolean hasGapAfter(int trackIndex) {
            return trackIndex >= 0 && trackIndex < gapAfter.length && gapAfter[trackIndex];
        }

        int nonCollapsedTrackCount() {
            return nonCollapsedTrackCount;
        }

        /** Size used by the tracks and their surviving gutters (the axis' used size). */
        float usedSize(FloatList trackSizes, float gap) {
            float usedSize = 0f;
            int trackCount = trackSizes.size();
            for (int i = 0; i < trackCount; i++) {
                usedSize += trackSizes.getFloat(i);
                if (i < gapAfter.length && gapAfter[i]) {
                    usedSize += gap;
                }
            }
            return usedSize;
        }

        /** Start offset of one track relative to the start of the first track. */
        float trackStart(FloatList trackSizes, int trackIndex, float adjustedGap) {
            int end = Math.min(trackIndex, trackSizes.size());
            float offset = 0f;
            for (int i = 0; i < end; i++) {
                offset += trackSizes.getFloat(i);
                if (hasGapAfter(i)) {
                    offset += adjustedGap;
                }
            }
            return offset;
        }

        /** Extent of a span of tracks, including the surviving gutters between them. */
        float trackSpanSize(FloatList trackSizes, int start, int span, float adjustedGap) {
            int end = Math.min(start + span, trackSizes.size());
            float size = 0f;
            for (int i = Math.max(0, start); i < end; i++) {
                size += trackSizes.getFloat(i);
                if (i + 1 < end && hasGapAfter(i)) {
                    size += adjustedGap;
                }
            }
            return size;
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
    private TrackCounts computeColumnCounts(List<GridItem> items, List<TrackSizingFunction> expandedCols,
                                           int areaColumnCount) {
        // Check explicit columns from the expanded template (after auto-fill/auto-fit expansion) and
        // from the grid-template-areas template size: the area template can be larger than the
        // extents of the named areas because of unnamed (".") cells, and those cells still
        // contribute explicit tracks (GR-16 / upstream #1024, which computes
        // explicit_col_count = max(template_count, area_column_count)).
        // Clamped into the limited grid (GR-05 / #986).
        int templateCols = (expandedCols != null && !expandedCols.isEmpty())
                           ? Math.min(expandedCols.size(), MAX_GRID_TRACKS) : 0;
        int explicitCols = Math.max(templateCols,
            Math.min(Math.max(0, areaColumnCount), MAX_GRID_TRACKS));

        // Scan items for min (negative implicit) and max (positive implicit) positions
        int minCol = 0;  // Minimum OriginZero position (can be negative)
        int maxCol = explicitCols;  // Maximum OriginZero position (end of explicit grid or beyond)
        int maxSpan = 1;  // Maximum span for auto-placed items

        for (GridItem item : items) {
            if (item.columnStart != null) {
                // Has definite start position
                minCol = Math.min(minCol, item.columnStart);
                int itemEnd = clampOzLine((long) item.columnStart + item.columnSpan);
                maxCol = Math.max(maxCol, itemEnd);
            } else if (item.columnEnd != null) {
                // Has definite end position but auto start
                int itemStart = clampOzLine((long) item.columnEnd - item.columnSpan);
                minCol = Math.min(minCol, itemStart);
                maxCol = Math.max(maxCol, item.columnEnd);
            } else {
                // Fully auto - track max span for later
                maxSpan = Math.max(maxSpan, clampGridSpan(item.columnSpan));
            }
        }

        // Clamp the item lines into the limited grid before deriving the implicit track counts.
        minCol = Math.max(MIN_OZ_LINE, minCol);
        maxCol = Math.min(MAX_OZ_LINE, maxCol);

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
            positiveImplicit = Math.max(0, maxSpan - negativeImplicit - explicitCols);
        }

        // Clamp the total track count so that the grid remains within the limited grid
        // (implicit lines stay within [MIN_OZ_LINE, MAX_OZ_LINE]).
        positiveImplicit = Math.min(positiveImplicit, Math.max(0, MAX_OZ_LINE - explicitCols));

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
    private TrackCounts computeRowCounts(List<GridItem> items, List<TrackSizingFunction> expandedRows,
                                         int areaRowCount) {
        // Check explicit rows from the expanded template (after auto-fill/auto-fit expansion) and
        // from the grid-template-areas template size (GR-16 / upstream #1024).
        // Clamped into the limited grid (GR-05 / #986).
        int templateRows = (expandedRows != null && !expandedRows.isEmpty())
                           ? Math.min(expandedRows.size(), MAX_GRID_TRACKS) : 0;
        int explicitRows = Math.max(templateRows,
            Math.min(Math.max(0, areaRowCount), MAX_GRID_TRACKS));

        // Scan items for min (negative implicit) and max (positive implicit) positions
        int minRow = 0;
        int maxRow = explicitRows;
        int maxSpan = 1;

        for (GridItem item : items) {
            if (item.rowStart != null) {
                minRow = Math.min(minRow, item.rowStart);
                int itemEnd = clampOzLine((long) item.rowStart + item.rowSpan);
                maxRow = Math.max(maxRow, itemEnd);
            } else if (item.rowEnd != null) {
                int itemStart = clampOzLine((long) item.rowEnd - item.rowSpan);
                minRow = Math.min(minRow, itemStart);
                maxRow = Math.max(maxRow, item.rowEnd);
            } else {
                // Fully auto - track max span for later
                maxSpan = Math.max(maxSpan, clampGridSpan(item.rowSpan));
            }
        }

        // Clamp the item lines into the limited grid before deriving the implicit track counts.
        minRow = Math.max(MIN_OZ_LINE, minRow);
        maxRow = Math.min(MAX_OZ_LINE, maxRow);

        // Compute negative and positive implicit tracks
        int negativeImplicit = minRow < 0 ? -minRow : 0;
        int positiveImplicit = maxRow > explicitRows ? maxRow - explicitRows : 0;

        // Note: We do NOT estimate extra tracks for auto-placed items here.
        // The auto-placement algorithm will dynamically expand the grid as needed.
        // This matches Rust's compute_grid_size_estimate behavior.

        // Ensure we have enough tracks for the max span of any indefinitely placed item
        int totalTracks = negativeImplicit + explicitRows + positiveImplicit;
        if (totalTracks < maxSpan) {
            positiveImplicit = Math.max(0, maxSpan - negativeImplicit - explicitRows);
        }

        // Clamp the total track count so that the grid remains within the limited grid
        // (implicit lines stay within [MIN_OZ_LINE, MAX_OZ_LINE]).
        positiveImplicit = Math.min(positiveImplicit, Math.max(0, MAX_OZ_LINE - explicitRows));

        return new TrackCounts(negativeImplicit, explicitRows, positiveImplicit);
    }

    /**
     * Auto-place items that don't have explicit column/row positions.
     * Supports both sparse (default) and dense packing algorithms.
     * CSS Grid Specification: 8.5. Grid Item Placement Algorithm
     * <p>
     * This mirrors upstream taffy's placement pass (GR-13): the occupancy matrix is a sparse
     * per-track interval list, a collision makes the search jump past the whole colliding interval
     * (upstream #1038), and sparse packing continues after the last auto-placed item that starts in
     * the same secondary-axis track (upstream #1263) instead of restarting from the grid start.
     * <p>
     * For ROW flow the primary axis is the column axis (items flow across columns first); for
     * COLUMN flow the primary axis is the row axis.
     */
    private void autoPlaceItems(List<GridItem> items, GridAutoFlow autoFlow,
                                TrackCounts colCounts, TrackCounts rowCounts) {
        boolean isDense = autoFlow != null && autoFlow.isDense();
        boolean isRowFlow = autoFlow == null || autoFlow.isRow();

        CellOccupancyMatrix matrix = new CellOccupancyMatrix(colCounts, rowCounts);

        // Step 1: Place items with definite positions in both axes. These are definitely placed and
        // do not advance any sparse placement cursor (upstream CellOccupancyState::DefinitelyPlaced).
        for (GridItem item : items) {
            if (item.columnStart != null && item.rowStart != null) {
                matrix.markArea(item.columnStart, item.rowStart, item.columnSpan, item.rowSpan,
                    isRowFlow, false);
            }
        }

        // Step 2: Place the remaining items with a definite secondary-axis position. The primary
        // search starts at the sparse cursor of that secondary track (or at the start of the
        // implicit grid for dense packing) and jumps over colliding intervals.
        for (GridItem item : items) {
            if (item.columnStart != null && item.rowStart != null) {
                continue; // Already placed in step 1
            }
            if (isRowFlow) {
                if (item.rowStart == null) {
                    continue; // The secondary (row) axis is not definite
                }
                int secondaryStart = item.rowStart;
                int secondaryEnd = clampOzLine((long) secondaryStart + item.rowSpan);
                int primaryStart = isDense
                                   ? matrix.implicitStartLine(true)
                                   : matrix.autoPlacementCursor(true, secondaryStart);
                if (primaryStart == NO_CURSOR) {
                    primaryStart = matrix.implicitStartLine(true);
                }
                while (true) {
                    int primaryEnd = clampOzLine((long) primaryStart + item.columnSpan);
                    int jump = matrix.lineAreaCollisionJump(true, primaryStart, primaryEnd,
                        secondaryStart, secondaryEnd);
                    if (jump == NO_COLLISION) {
                        break;
                    }
                    primaryStart = jump;
                }
                item.columnStart = clampOzLine(primaryStart);
                matrix.markArea(item.columnStart, secondaryStart, item.columnSpan, item.rowSpan, true, true);
            } else {
                if (item.columnStart == null) {
                    continue; // The secondary (column) axis is not definite
                }
                int secondaryStart = item.columnStart;
                int secondaryEnd = clampOzLine((long) secondaryStart + item.columnSpan);
                int primaryStart = isDense
                                   ? matrix.implicitStartLine(false)
                                   : matrix.autoPlacementCursor(false, secondaryStart);
                if (primaryStart == NO_CURSOR) {
                    primaryStart = matrix.implicitStartLine(false);
                }
                while (true) {
                    int primaryEnd = clampOzLine((long) primaryStart + item.rowSpan);
                    int jump = matrix.lineAreaCollisionJump(false, primaryStart, primaryEnd,
                        secondaryStart, secondaryEnd);
                    if (jump == NO_COLLISION) {
                        break;
                    }
                    primaryStart = jump;
                }
                item.rowStart = clampOzLine(primaryStart);
                matrix.markArea(item.columnStart, item.rowStart, item.columnSpan, item.rowSpan, false, true);
            }
        }

        // Step 3/4: Position the remaining items (secondary axis indefinite) in document order.
        int primaryStartLine = matrix.implicitStartLine(isRowFlow);
        int secondaryStartLine = matrix.implicitStartLine(!isRowFlow);
        int primaryIdx = primaryStartLine;
        int secondaryIdx = secondaryStartLine;

        for (GridItem item : items) {
            boolean secondaryDefinite = isRowFlow ? item.rowStart != null : item.columnStart != null;
            if (secondaryDefinite) {
                continue; // Already placed in steps 1-2
            }

            int primarySpanCount = isRowFlow ? item.columnSpan : item.rowSpan;
            int secondarySpanCount = isRowFlow ? item.rowSpan : item.columnSpan;
            boolean primaryDefinite = isRowFlow ? item.columnStart != null : item.rowStart != null;

            int placedPrimaryStart;
            int placedSecondaryStart;

            if (primaryDefinite) {
                // The primary axis position is definite, so the search walks the secondary axis.
                placedPrimaryStart = isRowFlow ? item.columnStart : item.rowStart;
                if (!isDense && placedPrimaryStart < primaryIdx) {
                    // Sparse packing: an item before the current primary position moves on to the
                    // next secondary track (upstream #1263).
                    secondaryIdx = saturatingAdd(secondaryIdx, 1);
                }
                while (true) {
                    int secondaryEnd = clampOzLine((long) secondaryIdx + secondarySpanCount);
                    int primaryEnd = clampOzLine((long) placedPrimaryStart + primarySpanCount);
                    // The search's primary axis is the item's secondary axis, and the item's
                    // definite primary span is the search's secondary span.
                    int jump = matrix.lineAreaCollisionJump(!isRowFlow,
                        secondaryIdx, secondaryEnd, placedPrimaryStart, primaryEnd);
                    if (jump == NO_COLLISION) {
                        break;
                    }
                    secondaryIdx = jump;
                }
                placedSecondaryStart = clampOzLine(secondaryIdx);
            } else {
                // Both axes indefinite: walk the primary axis, wrapping to the next secondary track
                // once the primary span leaves the implicit grid.
                boolean spansAllPrimaryTracks = primarySpanCount >= matrix.primaryTrackCount(isRowFlow);
                while (true) {
                    int primaryEnd = clampOzLine((long) primaryIdx + primarySpanCount);
                    int secondaryEnd = clampOzLine((long) secondaryIdx + secondarySpanCount);
                    if (primaryEnd > matrix.primaryImplicitEndLine(isRowFlow)) {
                        if (primaryIdx == primaryStartLine) {
                            // The span cannot fit even at the grid start (the limited-grid clamping
                            // truncated the grid): place it here and let track clamping truncate
                            // the placement.
                            break;
                        }
                        secondaryIdx = saturatingAdd(secondaryIdx, 1);
                        primaryIdx = primaryStartLine;
                        continue;
                    }
                    if (spansAllPrimaryTracks) {
                        // An item spanning every primary track only fits in a stripe of entirely
                        // empty tracks, so jump past every non-empty secondary track.
                        int jump = matrix.occupiedTrackJump(!isRowFlow, secondaryIdx, secondaryEnd);
                        if (jump != NO_COLLISION) {
                            secondaryIdx = jump;
                            primaryIdx = primaryStartLine;
                            continue;
                        }
                    }
                    int jump = matrix.lineAreaCollisionJump(isRowFlow, primaryIdx, primaryEnd,
                        secondaryIdx, secondaryEnd);
                    if (jump != NO_COLLISION) {
                        primaryIdx = jump;
                        continue;
                    }
                    break;
                }
                placedPrimaryStart = clampOzLine(primaryIdx);
                placedSecondaryStart = clampOzLine(secondaryIdx);
            }

            if (isRowFlow) {
                item.columnStart = placedPrimaryStart;
                item.rowStart = placedSecondaryStart;
                matrix.markArea(item.columnStart, item.rowStart, item.columnSpan, item.rowSpan, true, true);
            } else {
                item.rowStart = placedPrimaryStart;
                item.columnStart = placedSecondaryStart;
                matrix.markArea(item.columnStart, item.rowStart, item.columnSpan, item.rowSpan, false, true);
            }

            // Upstream: dense packing resets the search to the grid start; sparse packing continues
            // after the placed item (the next item starts at the primary end in the same track).
            if (isDense) {
                primaryIdx = primaryStartLine;
                secondaryIdx = secondaryStartLine;
            } else {
                primaryIdx = clampOzLine((long) placedPrimaryStart + primarySpanCount);
                secondaryIdx = placedSecondaryStart;
            }
        }

        // Update the TrackCounts with final values after placement
        colCounts.update(matrix.colCounts);
        rowCounts.update(matrix.rowCounts);
    }

    /**
     * A run of occupied cells within a single track of the cell occupancy matrix (upstream
     * {@code OccupiedInterval}). The range is in OriginZero line coordinates along the track: for a
     * row track it spans column lines and for a column track it spans row lines. The intervals of
     * one track are sorted and disjoint, and touching intervals are merged so that a run of
     * adjacent occupied cells is stored as one interval (upstream #1015/#1263).
     */
    private static final class TrackIntervals {
        private static final int[] EMPTY = new int[0];

        /** Pairs of (start, end) OriginZero lines, sorted and disjoint. */
        private int[] ranges = EMPTY;
        private int intervalCount;
        /** Furthest end line of an auto-placed item starting in this track, or {@link #NO_CURSOR}. */
        private int autoPlacementCursor = NO_CURSOR;

        boolean isEmpty() {
            return intervalCount == 0;
        }

        /** Add the union of the passed range to the sorted, disjoint interval list. */
        void paint(int start, int end) {
            if (start >= end) {
                return;
            }
            if (intervalCount == 0) {
                push(start, end);
                return;
            }
            // Fast path: the range lies entirely after all existing intervals. This is the common
            // case because placement queries and paints mostly advance forwards.
            int lastEnd = ranges[2 * intervalCount - 1];
            if (lastEnd <= start) {
                if (lastEnd == start) {
                    ranges[2 * intervalCount - 1] = end;
                } else {
                    push(start, end);
                }
                return;
            }

            // Rare path: rebuild the list, merging every overlapping or touching interval.
            int[] result = new int[ranges.length + 2];
            int resultCount = 0;
            int index = 0;
            while (index < intervalCount && ranges[2 * index + 1] < start) {
                result[2 * resultCount] = ranges[2 * index];
                result[2 * resultCount + 1] = ranges[2 * index + 1];
                resultCount++;
                index++;
            }
            int mergedStart = start;
            int mergedEnd = end;
            while (index < intervalCount && ranges[2 * index] <= mergedEnd) {
                mergedStart = Math.min(mergedStart, ranges[2 * index]);
                mergedEnd = Math.max(mergedEnd, ranges[2 * index + 1]);
                index++;
            }
            result[2 * resultCount] = mergedStart;
            result[2 * resultCount + 1] = mergedEnd;
            resultCount++;
            while (index < intervalCount) {
                result[2 * resultCount] = ranges[2 * index];
                result[2 * resultCount + 1] = ranges[2 * index + 1];
                resultCount++;
                index++;
            }
            ranges = result;
            intervalCount = resultCount;
        }

        private void push(int start, int end) {
            if (2 * (intervalCount + 1) > ranges.length) {
                int[] grown = new int[Math.max(4, ranges.length * 2)];
                System.arraycopy(ranges, 0, grown, 0, 2 * intervalCount);
                ranges = grown;
            }
            ranges[2 * intervalCount] = start;
            ranges[2 * intervalCount + 1] = end;
            intervalCount++;
        }

        /**
         * The extremal occupied cell of the interval a forward search along the track collides with
         * last: the end of the last overlapping interval (upstream #1038 {@code collision_extent}).
         * Returns {@link #NO_COLLISION} when the range is entirely unoccupied.
         */
        int collisionExtent(int rangeStart, int rangeEnd) {
            for (int index = intervalCount - 1; index >= 0; index--) {
                int intervalEnd = ranges[2 * index + 1];
                if (intervalEnd <= rangeStart) {
                    break;
                }
                if (ranges[2 * index] < rangeEnd) {
                    return intervalEnd - 1;
                }
            }
            return NO_COLLISION;
        }

        void advanceAutoPlacementCursor(int primaryEnd) {
            autoPlacementCursor = Math.max(autoPlacementCursor, primaryEnd);
        }
    }

    /**
     * A dynamically-expanding cell occupancy matrix for grid auto-placement.
     * <p>
     * Occupancy is stored sparsely as per-track interval lists in both orientations (a row track
     * keeps the occupied column intervals and a column track the occupied row intervals), so memory
     * usage is proportional to the number of placed items rather than to the number of grid cells
     * (upstream #1015). Extending the grid only grows the per-track reference arrays; interval
     * contents are stored in OriginZero coordinates and therefore do not need to be copied.
     * <p>
     * Search helpers mirror upstream: {@link #lineAreaCollisionJump} returns the first primary-axis
     * position past all colliding intervals (upstream #1038) and {@link #occupiedTrackJump} skips
     * non-empty secondary tracks for items spanning the whole primary axis. Sparse placement
     * cursors are stored per secondary track, separately from the occupancy intervals (upstream
     * #1263).
     */
    private static class CellOccupancyMatrix {
        TrackCounts colCounts;
        TrackCounts rowCounts;
        /** For each row track: the occupied intervals within that row (in column coordinates). */
        TrackIntervals[] rowIntervals;
        /** For each column track: the occupied intervals within that column (in row coordinates). */
        TrackIntervals[] columnIntervals;

        CellOccupancyMatrix(TrackCounts colCounts, TrackCounts rowCounts) {
            this.colCounts = new TrackCounts(colCounts);
            this.rowCounts = new TrackCounts(rowCounts);
            this.rowIntervals = new TrackIntervals[this.rowCounts.len()];
            this.columnIntervals = new TrackIntervals[this.colCounts.len()];
        }

        private TrackIntervals rowTrack(int index) {
            TrackIntervals intervals = rowIntervals[index];
            if (intervals == null) {
                intervals = new TrackIntervals();
                rowIntervals[index] = intervals;
            }
            return intervals;
        }

        private TrackIntervals columnTrack(int index) {
            TrackIntervals intervals = columnIntervals[index];
            if (intervals == null) {
                intervals = new TrackIntervals();
                columnIntervals[index] = intervals;
            }
            return intervals;
        }

        /**
         * Expand the track lists (potentially in all four directions) so that the passed area fits.
         * The expansion is clamped into the limited grid (GR-05 / #986) so that pathological
         * placements cannot allocate an unbounded number of tracks.
         */
        private void expandToFit(int ozColStart, int ozRowStart, int colSpan, int rowSpan) {
            long ozColEnd = (long) ozColStart + colSpan;
            long ozRowEnd = (long) ozRowStart + rowSpan;

            int reqNegCols = (int) Math.max(-(long) ozColStart - colCounts.negativeImplicit, 0);
            int reqPosCols = (int) Math.max(ozColEnd - (colCounts.explicit + colCounts.positiveImplicit), 0);
            int reqNegRows = (int) Math.max(-(long) ozRowStart - rowCounts.negativeImplicit, 0);
            int reqPosRows = (int) Math.max(ozRowEnd - (rowCounts.explicit + rowCounts.positiveImplicit), 0);

            reqNegCols = Math.min(reqNegCols, Math.max(0, MAX_GRID_TRACKS - colCounts.negativeImplicit));
            reqNegRows = Math.min(reqNegRows, Math.max(0, MAX_GRID_TRACKS - rowCounts.negativeImplicit));
            reqPosCols = Math.min(reqPosCols,
                Math.max(0, MAX_OZ_LINE - (colCounts.explicit + colCounts.positiveImplicit)));
            reqPosRows = Math.min(reqPosRows,
                Math.max(0, MAX_OZ_LINE - (rowCounts.explicit + rowCounts.positiveImplicit)));

            if (reqNegCols == 0 && reqPosCols == 0 && reqNegRows == 0 && reqPosRows == 0) {
                return; // No expansion needed
            }

            rowIntervals = expandTrackList(rowIntervals, reqNegRows, reqPosRows);
            columnIntervals = expandTrackList(columnIntervals, reqNegCols, reqPosCols);
            colCounts.negativeImplicit += reqNegCols;
            colCounts.positiveImplicit += reqPosCols;
            rowCounts.negativeImplicit += reqNegRows;
            rowCounts.positiveImplicit += reqPosRows;
        }

        private static TrackIntervals[] expandTrackList(TrackIntervals[] tracks, int negative, int positive) {
            if (negative == 0 && positive == 0) {
                return tracks;
            }
            TrackIntervals[] expanded = new TrackIntervals[tracks.length + negative + positive];
            System.arraycopy(tracks, 0, expanded, negative, tracks.length);
            return expanded;
        }

        /**
         * Mark an area as occupied, expanding the track lists when needed. For auto-placed items
         * the sparse cursor of the starting secondary track advances to the item's primary end
         * (upstream #1263); definitely placed items do not advance any cursor.
         */
        void markArea(int ozColStart, int ozRowStart, int colSpan, int rowSpan,
                      boolean isRowFlow, boolean autoPlaced) {
            expandToFit(ozColStart, ozRowStart, colSpan, rowSpan);

            // Clamp the marked range to the tracks that actually exist so that a placement clamped
            // into the limited grid cannot write outside the track arrays.
            int colStartIndex = Math.max(0, colCounts.ozLineToNextTrack(ozColStart));
            int rowStartIndex = Math.max(0, rowCounts.ozLineToNextTrack(ozRowStart));
            int colEndIndex = Math.min(columnIntervals.length,
                (int) Math.min(Integer.MAX_VALUE, (long) colCounts.ozLineToNextTrack(ozColStart) + colSpan));
            int rowEndIndex = Math.min(rowIntervals.length,
                (int) Math.min(Integer.MAX_VALUE, (long) rowCounts.ozLineToNextTrack(ozRowStart) + rowSpan));
            if (colStartIndex >= colEndIndex || rowStartIndex >= rowEndIndex) {
                return;
            }

            int colOzStart = colCounts.trackToOzLine(colStartIndex);
            int colOzEnd = colCounts.trackToOzLine(colEndIndex - 1) + 1;
            int rowOzStart = rowCounts.trackToOzLine(rowStartIndex);
            int rowOzEnd = rowCounts.trackToOzLine(rowEndIndex - 1) + 1;

            if (autoPlaced) {
                if (isRowFlow) {
                    int end = clampOzLine((long) ozColStart + colSpan);
                    rowTrack(rowStartIndex).advanceAutoPlacementCursor(end);
                } else {
                    int end = clampOzLine((long) ozRowStart + rowSpan);
                    columnTrack(colStartIndex).advanceAutoPlacementCursor(end);
                }
            }

            for (int row = rowStartIndex; row < rowEndIndex; row++) {
                rowTrack(row).paint(colOzStart, colOzEnd);
            }
            for (int column = colStartIndex; column < colEndIndex; column++) {
                columnTrack(column).paint(rowOzStart, rowOzEnd);
            }
        }

        /** The start line of the implicit grid along the flow's primary axis. */
        int implicitStartLine(boolean isRowFlow) {
            return (isRowFlow ? colCounts : rowCounts).implicitStartLine();
        }

        /** The end line of the implicit grid along the flow's primary axis. */
        int primaryImplicitEndLine(boolean isRowFlow) {
            return (isRowFlow ? colCounts : rowCounts).implicitEndLine();
        }

        /** The number of tracks along the flow's primary axis. */
        int primaryTrackCount(boolean isRowFlow) {
            return (isRowFlow ? colCounts : rowCounts).len();
        }

        /**
         * The sparse placement cursor of the secondary-axis track starting at the passed line, or
         * {@link #NO_CURSOR} when no auto-placed item starts there (upstream #1263
         * {@code auto_placement_cursor}).
         */
        int autoPlacementCursor(boolean isRowFlow, int secondaryStart) {
            TrackIntervals[] tracks = isRowFlow ? rowIntervals : columnIntervals;
            TrackCounts counts = isRowFlow ? rowCounts : colCounts;
            int index = counts.ozLineToNextTrack(secondaryStart);
            if (index < 0 || index >= tracks.length || tracks[index] == null) {
                return NO_CURSOR;
            }
            return tracks[index].autoPlacementCursor;
        }

        /**
         * Check an area for occupied cells and return the next primary-axis search position that is
         * not guaranteed to collide, or {@link #NO_COLLISION} when the area is entirely free
         * (upstream #1038 {@code line_area_collision_jump}). The jump is past the end of the last
         * colliding interval rather than just past the queried area, which is what keeps the search
         * linear in the number of occupied intervals instead of quadratic in the track count.
         */
        int lineAreaCollisionJump(boolean isRowFlow, int primaryStart, int primaryEnd,
                                  int secondaryStart, int secondaryEnd) {
            TrackIntervals[] tracks = isRowFlow ? rowIntervals : columnIntervals;
            TrackCounts counts = isRowFlow ? rowCounts : colCounts;
            int start = Math.max(0, counts.ozLineToNextTrack(secondaryStart));
            int end = Math.min(tracks.length, counts.ozLineToNextTrack(secondaryEnd));
            int extent = NO_COLLISION;
            for (int index = start; index < end; index++) {
                TrackIntervals intervals = tracks[index];
                if (intervals == null) {
                    continue;
                }
                int cell = intervals.collisionExtent(primaryStart, primaryEnd);
                if (cell == NO_COLLISION) {
                    continue;
                }
                extent = (extent == NO_COLLISION) ? cell : Math.max(extent, cell);
            }
            return extent == NO_COLLISION ? NO_COLLISION : extent + 1;
        }

        /**
         * The next position past every non-empty track in the passed span, or {@link #NO_COLLISION}
         * when all of them are empty (upstream #1038 {@code occupied_track_jump}). Items spanning
         * every track of one axis can only be placed in a stripe of entirely empty tracks on the
         * other axis.
         *
         * @param axisIsHorizontal true to inspect the column tracks, false to inspect the row tracks
         */
        int occupiedTrackJump(boolean axisIsHorizontal, int secondaryStart, int secondaryEnd) {
            TrackIntervals[] tracks = axisIsHorizontal ? columnIntervals : rowIntervals;
            TrackCounts counts = axisIsHorizontal ? colCounts : rowCounts;
            int start = Math.max(0, counts.ozLineToNextTrack(secondaryStart));
            int end = Math.min(tracks.length, counts.ozLineToNextTrack(secondaryEnd));
            for (int index = end - 1; index >= start; index--) {
                TrackIntervals intervals = tracks[index];
                if (intervals != null && !intervals.isEmpty()) {
                    return counts.trackToOzLine(index) + 1;
                }
            }
            return NO_COLLISION;
        }
    }

    private FloatList calculateColumnSizes(
        TaffyStyle style,
        FloatSize nodeInnerSize,
        float innerMinWidth,
        float innerMaxWidth,
        TaffySize<AvailableSpace> availableGridSpace,
        TaffySize<AvailableSpace> originalAvailableSpace,
        FloatSize gap,
        int numColumns,
        int numRows,
        List<GridItem> items,
        TrackCounts colCounts,
        List<TrackSizingFunction> expandedCols) {
        return calculateColumnSizesWithRowSizes(style, nodeInnerSize, innerMinWidth, innerMaxWidth,
            availableGridSpace, originalAvailableSpace, gap,
            numColumns, numRows, items, colCounts, expandedCols, null);
    }

    private FloatList calculateColumnSizesWithRowSizes(
        TaffyStyle style,
        FloatSize nodeInnerSize,
        float innerMinWidth,
        float innerMaxWidth,
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

        float usedSpace = 0;
        IntList frTrackIndices = new IntArrayList();  // Track which indices have fr
        FloatList frValues = new FloatArrayList();  // Store fr values

        // Get auto columns for implicit tracks
        List<TrackSizingFunction> autoColumns = style.getGridAutoColumns();
        int autoColCount = (autoColumns != null && !autoColumns.isEmpty()) ? autoColumns.size() : 0;
        // Per-index track sizing functions, used for the grid-area-size estimates (GR-06 / #960).
        List<TrackSizingFunction> resolvedColumnTracks =
            resolveTrackSizingList(expandedCols, autoColumns, colCounts, numColumns);
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
                    frTrackIndices.add(i);
                    frValues.add(maxF.getFrValue());
                    // Initialise the base size from the min track sizing function (CSS Grid 11.4):
                    // a fixed min is its definite value, an intrinsic min starts at zero and is
                    // filled in by the intrinsic/contingent contribution passes.
                    float frBase = (minF != null && minF.isFixed())
                                   ? Math.max(0f, minF.getFixedValue().resolveOrZero(nodeInnerSize.width)) : 0f;
                    sizes.add(frBase);
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

        // GR-12 / upstream #1261: when the container's width is indefinite, an indefinite percentage
        // min track sizing function behaves as auto. Its base size is the item's minimum contribution:
        // the automatic content-based minimum is clamped by the fixed max track size, but an explicit
        // min-size or a definite preferred size larger than the fixed maximum raises the base above it.
        for (int i = 0; i < numColumns; i++) {
            if (!trackMinIsIndefinitePercent(resolvedColumnTracks.get(i), nodeInnerSize.width)) continue;
            float maxLimit = trackDefiniteValue(trackMaxFunction(resolvedColumnTracks.get(i)), nodeInnerSize.width);
            if (Float.isNaN(maxLimit) || Float.isNaN(sizes.getFloat(i))) continue;
            float base = sizes.getFloat(i);
            for (int itemIdx = 0; itemIdx < items.size(); itemIdx++) {
                GridItem item = items.get(itemIdx);
                int col = getItemColumnWithCounts(item, itemIdx, numColumns, colCounts);
                if (col != i || item.columnSpan != 1) continue;
                float estimatedRowHeight = estimateItemRowHeightWithKnownSizes(item, templateRows, autoRows,
                    numRows, nodeInnerSize.height, gap, knownRowSizes);
                FloatSize itemGridAreaSize = new FloatSize(NaN, estimatedRowHeight);
                base = Math.max(base, explicitMinimumContributionExtent(item, true, itemGridAreaSize));
            }
            sizes.set(i, base);
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
                // The aggregate minimum contribution (CSS Grid 6.6): used as the auto track base
                // under a definite available space, where a cyclic percentage min-size contributes
                // zero and scroll containers contribute no content size (GR-12 / upstream #1257).
                float minimumContributionSize = 0f;

                for (int itemIdx = 0; itemIdx < items.size(); itemIdx++) {
                    GridItem item = items.get(itemIdx);
                    int col = getItemColumnWithCounts(item, itemIdx, numColumns, colCounts);
                    int span = item.columnSpan;

                    // Only consider items that occupy this track with span of 1
                    if (col == i && span == 1) {
                        // Estimate the height available for this item based on row tracks. This is the
                        // item's grid area: during intrinsic sizing its percentages resolve against the
                        // grid area, not the grid container (GR-06 / upstream #960).
                        float estimatedRowHeight = estimateItemRowHeightWithKnownSizes(item, templateRows, autoRows, numRows, nodeInnerSize.height, gap, knownRowSizes);
                        FloatSize itemGridAreaSize = new FloatSize(
                            definiteGridAreaExtent(sizes, resolvedColumnTracks, i, 1, nodeInnerSize.width),
                            estimatedRowHeight);

                        // Horizontal percentage margins resolve to 0 in track sizing to avoid cyclic dependency
                        FloatSize marginAxisSums = item.getMarginAxisSumsWithBaselineShims(itemGridAreaSize.width);
                        // Calculate minimum size from padding+border (size cannot be less than this)
                        float itemPaddingBorderWidth = item.paddingBorderSize(itemGridAreaSize).width;

                        FloatSize preferredSize = item.resolvePreferredSize(itemGridAreaSize);
                        if (!Float.isNaN(preferredSize.width)) {
                            // Size must be at least padding+border
                            float effectiveWidth = Math.max(preferredSize.width, itemPaddingBorderWidth);
                            float itemWidth = effectiveWidth + marginAxisSums.width;
                            maxContentSize = Math.max(maxContentSize, itemWidth);
                            pureMaxContentSize = Math.max(pureMaxContentSize, itemWidth);  // Explicit size is pure max-content
                            minContentSize = Math.max(minContentSize, itemWidth);
                            minimumContributionSize = Math.max(minimumContributionSize, itemWidth);
                        } else {
                            // Check if item has overflow: hidden/scroll/auto - affects min-content contribution
                            boolean hasOverflow = item.overflow != null &&
                                                  (item.overflow.x == Overflow.HIDDEN || item.overflow.x == Overflow.SCROLL ||
                                                   item.overflow.x == Overflow.CLIP);

                            // Get item's min-width constraint (for auto track sizing)
                            FloatSize resolvedMinSize = item.resolveMinSize(itemGridAreaSize);
                            float itemMinWidthConstraint = !Float.isNaN(resolvedMinSize.width)
                                                           ? resolvedMinSize.width + marginAxisSums.width
                                                           : 0f;

                            // Measure max-content using cache
                            // Note: cached methods already include margin
                            float itemMaxWidth = item.getMaxContentContributionWidthCached(layoutComputer, itemGridAreaSize);
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
                                float measuredMinWidth = item.getMinContentContributionWidthCached(layoutComputer, itemGridAreaSize);
                                // min-content contribution should respect min-size constraint
                                itemMinWidth = Math.max(measuredMinWidth, itemMinWidthConstraint);
                            }

                            // Compressible replaced element capping (Rust: is_compressible_replaced)
                            // If preferred/max sizes are definite (including % resolved against 0), cap the content-based minimum.
                            itemMinWidth = capCompressibleReplacedMinimumContributionWidth(item, itemMinWidth, marginAxisSums.width, itemGridAreaSize);

                            minContentSize = Math.max(minContentSize, itemMinWidth);

                            // The minimum contribution differs from the min-content contribution for
                            // scroll containers and for a cyclic percentage min-size (which resolves
                            // against zero, #1257).
                            float measuredMinForMinimum = hasOverflow
                                ? 0f
                                : item.getMinContentContributionWidthCached(layoutComputer, itemGridAreaSize);
                            float minimumContribution = minimumContributionWidth(item, measuredMinForMinimum,
                                marginAxisSums.width, hasOverflow, itemGridAreaSize);
                            minimumContribution = capCompressibleReplacedMinimumContributionWidth(item,
                                minimumContribution, marginAxisSums.width, itemGridAreaSize);
                            minimumContributionSize = Math.max(minimumContributionSize, minimumContribution);
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
                        // Container has definite size: the base size is the item's minimum
                        // contribution (upstream AUTO_TAG with a definite available space) — equal to
                        // the min-content contribution except for scroll containers, definite
                        // min-sizes and cyclic percentage minima (#1257).
                        // growth_limit = max-content (for maximise_tracks limit)
                        // stretch_auto_tracks will distribute additional space if justify-content: stretch
                        size = minimumContributionSize;
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
                        // Container is auto-sized but has definite available space: the base size is
                        // the item's minimum contribution and the growth limit its max-content
                        // contribution (upstream AUTO_TAG with a definite available space).
                        size = minimumContributionSize;
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

                // Estimate the height available for this item based on row tracks and build the item's
                // grid area: percentages resolve against it during intrinsic sizing (GR-06 / #960).
                float estimatedRowHeight = estimateItemRowHeightWithKnownSizes(item, templateRows, autoRows, numRows, nodeInnerSize.height, gap, knownRowSizes);
                FloatSize itemGridAreaSize = new FloatSize(
                    definiteGridAreaExtent(sizes, resolvedColumnTracks, col, span, nodeInnerSize.width),
                    estimatedRowHeight);

                // Horizontal percentage margins resolve to 0 in track sizing to avoid cyclic dependency
                FloatSize marginAxisSums = item.getMarginAxisSumsWithBaselineShims(itemGridAreaSize.width);

                // Calculate item's contributions using cache
                // Note: cached methods already include margin
                float itemMaxContent;
                float itemMinContent;
                FloatSize preferredSize = item.resolvePreferredSize(itemGridAreaSize);
                if (!Float.isNaN(preferredSize.width)) {
                    itemMaxContent = preferredSize.width + marginAxisSums.width;
                    itemMinContent = itemMaxContent;
                } else {
                    itemMaxContent = item.getMaxContentContributionWidthCached(layoutComputer, itemGridAreaSize);
                    itemMinContent = item.getMinContentContributionWidthCached(layoutComputer, itemGridAreaSize);
                }

                // For scroll containers, minimum contribution is 0 (unless min-size is set)
                float minimumContribution = isScrollContainer ? 0f : itemMinContent;

                // Compressible replaced element capping (Rust: is_compressible_replaced)
                minimumContribution = capCompressibleReplacedMinimumContributionWidth(item, minimumContribution, marginAxisSums.width, itemGridAreaSize);

                // Identify track types
                IntList intrinsicTracks = new IntArrayList();  // All intrinsic min tracks (auto, min-content, max-content)
                IntList minOrMaxContentTracks = new IntArrayList();  // min-content or max-content tracks
                IntList maxContentTracks = new IntArrayList();  // max-content tracks only
                IntList autoTracks = new IntArrayList();  // auto tracks (including implicit auto and fr)
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

                // Items that cross a flexible track are handled after the intrinsic sizing of the
                // non-flexible tracks by resolveFlexibleTrackContributions (GR-01 / upstream #1019,
                // #1084): their contributions are distributed only to the affected flexible tracks
                // and in proportion to the affected flex factors.
                if (!crossesFlexTrack) {
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

        // 11.5 Resolve Intrinsic Track Sizes, flexible tracks (GR-01 / upstream #1019, #1084).
        // Items that cross a flexible track are processed last, after the non-flexible tracks have
        // received their intrinsic sizes, and their contributions are handed only to the affected
        // flexible tracks (in proportion to the affected flex factor sum, or equally when that sum is
        // zero), scaled by min(crossed flex factor sum, 1) and anchored at the spanned inflexible
        // track sizes with the item's minimum contribution as a floor.
        List<FlexContribution> flexItemContributions = new ArrayList<>();
        boolean intrinsicWidthConstraint = gridWidthSpace.isMinContent() || gridWidthSpace.isMaxContent();
        for (int itemIdx = 0; itemIdx < items.size(); itemIdx++) {
            GridItem item = items.get(itemIdx);
            if (!item.crossesFlexibleColumn) continue;

            int col = getItemColumnWithCounts(item, itemIdx, numColumns, colCounts);
            int span = item.columnSpan;
            // The item's grid area: percentages resolve against it during intrinsic sizing (#960).
            float estimatedRowHeight = estimateItemRowHeightWithKnownSizes(
                item, templateRows, autoRows, numRows, nodeInnerSize.height, gap, knownRowSizes);
            FloatSize itemGridAreaSize = new FloatSize(
                definiteGridAreaExtent(sizes, resolvedColumnTracks, col, span, nodeInnerSize.width),
                estimatedRowHeight);
            FloatSize marginAxisSums = item.getMarginAxisSumsWithBaselineShims(itemGridAreaSize.width);

            float itemMinContent;
            float itemMaxContent;
            FloatSize preferredSize = item.resolvePreferredSize(itemGridAreaSize);
            if (!Float.isNaN(preferredSize.width)) {
                itemMinContent = preferredSize.width + marginAxisSums.width;
                itemMaxContent = itemMinContent;
            } else {
                itemMaxContent = item.getMaxContentContributionWidthCached(layoutComputer, itemGridAreaSize);
                itemMinContent = item.getMinContentContributionWidthCached(layoutComputer, itemGridAreaSize);
            }
            boolean isScrollContainer = item.overflow != null
                && (item.overflow.x == Overflow.HIDDEN || item.overflow.x == Overflow.SCROLL || item.overflow.x == Overflow.CLIP);
            float minimumContribution = minimumContributionWidth(item, itemMinContent, marginAxisSums.width,
                isScrollContainer, itemGridAreaSize);
            boolean crossesIntrinsic = false;
            for (int c = col; c < col + span && c < numColumns; c++) {
                if (trackMinIsIntrinsic(resolvedColumnTracks.get(c))) {
                    crossesIntrinsic = true;
                    break;
                }
            }
            flexItemContributions.add(new FlexContribution(col, span, crossesIntrinsic, minimumContribution,
                itemMinContent, itemMaxContent));
        }
        resolveFlexibleTrackContributions(sizes, resolvedColumnTracks, gap.width, flexItemContributions,
            intrinsicWidthConstraint, availableGridSpace.width.isMaxContent());

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

        // 11.7 Expand Flexible Tracks (GR-01): size the flexible tracks from the resolved base sizes.
        // Under a definite inner size the tracks fill the container; under an indefinite inner size
        // the used flex fraction is the maximum of the per-track base/flex term and the result of
        // finding the size of an fr for each flexible-track-crossing item's max-content contribution.
        // The content-box min/max correction below then redoes this step against the container's
        // style min/max size (GR-03 / #1023).
        float expansionAvailableWidth = Float.isNaN(nodeInnerSize.width)
                                        ? NaN
                                        : nodeInnerSize.width - gap.width * Math.max(0, numColumns - 1);
        expandFlexibleTracks(sizes, gap.width, frTrackIndices, frValues, flexItemContributions,
            expansionAvailableWidth, gridWidthSpace);

        // 11.7 Expand Flexible Tracks: min/max correction (GR-03 / upstream #1023).
        // The flexible track expansion above only sees the container's content. When the container's
        // inner size is indefinite but it has a definite min- or max-size, the expansion has to be
        // redone against that inner size when the hypothetical grid size falls outside it. The inner
        // min/max sizes are content-box values; the gaps are not part of the track list here, so they
        // are removed from the space to fill.
        if (Float.isNaN(nodeInnerSize.width)) {
            applyFlexibleTrackMinMaxCorrection(sizes, frTrackIndices, frValues,
                TaffyMath.maybeSub(innerMinWidth, gap.width * Math.max(0, numColumns - 1)),
                TaffyMath.maybeSub(innerMaxWidth, gap.width * Math.max(0, numColumns - 1)));
        }

        // 11.8. Stretch auto Tracks
        // Expands tracks whose max track sizing function is auto, dividing the remaining positive,
        // definite free space equally among them. Per upstream stretch_auto_tracks, when the
        // container's inner size is indefinite the inner min-size is used instead, and a min-content
        // constraint leaves no free space (GR-03 / #1023).
        //
        // AUTO/null means the legacy grid default = STRETCH; an explicit NORMAL behaves as STRETCH
        // for grid containers too (upstream #1255 / grid_align_content).
        AlignContent justifyContent = style.justifyContent;
        boolean shouldStretch = (justifyContent == null || justifyContent == AlignContent.AUTO
                                 || justifyContent == AlignContent.NORMAL
                                 || justifyContent == AlignContent.STRETCH);
        float stretchAvailableSpace = NaN;
        if (!Float.isNaN(nodeInnerSize.width)) {
            stretchAvailableSpace = effectiveAvailableWidth;
        } else if (!Float.isNaN(innerMinWidth) && !gridWidthSpace.isMinContent()) {
            stretchAvailableSpace = TaffyMath.maybeSub(innerMinWidth, gap.width * Math.max(0, numColumns - 1));
        }
        if (shouldStretch && !Float.isNaN(stretchAvailableSpace)) {
            // Recalculate free space for stretch (this is not the remaining space from the maximise
            // step: stretch is only limited by the container's inner size / min-size).
            float stretchUsedSpace = 0;
            for (float s : sizes) {
                stretchUsedSpace += (!Float.isNaN(s) ? s : 0);
            }
            float stretchFreeSpace = stretchAvailableSpace - stretchUsedSpace;
            if (stretchFreeSpace > 0.001f) {
                int stretchExplicitStart = colCounts.negativeImplicit;
                int stretchExplicitEnd = stretchExplicitStart + colCounts.explicit;
                // Only tracks with an auto max track sizing function are stretched (not minmax tracks
                // and not flexible tracks).
                List<Integer> autoTracks = new ArrayList<>();
                for (int i = 0; i < sizes.size(); i++) {
                    TrackSizingFunction track;
                    if (i >= stretchExplicitStart && i < stretchExplicitEnd) {
                        int templateIndex = i - stretchExplicitStart;
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
                        sizes.set(i, sizes.getFloat(i) + extraPerTrack);
                    }
                }
            }
        }

        // Final pass: ensure no null values remain
        for (int i = 0; i < sizes.size(); i++) {
            if (Float.isNaN(sizes.getFloat(i))) {
                sizes.set(i, 0f);
            }
        }

        return sizes;
    }


    // === GR-01: flexible (fr) track intrinsic contributions (upstream #1019, #1084) ===

    /**
     * One grid item's intrinsic size contributions in a single axis, as consumed by the flexible
     * track contribution pass.
     *
     * @param start            first track index spanned by the item
     * @param span             number of tracks spanned by the item
     * @param crossesIntrinsic whether the item spans at least one track with an intrinsic min track
     *                         sizing function (upstream {@code crosses_intrinsic_track})
     * @param minimum          minimum contribution (outer size, including margins)
     * @param minContent       min-content contribution (outer size, including margins)
     * @param maxContent       max-content contribution (outer size, including margins)
     */
    private record FlexContribution(int start, int span, boolean crossesIntrinsic,
                                    float minimum, float minContent, float maxContent) {}

    /** Whether the track's max track sizing function is flexible (upstream {@code is_flexible}). */
    private static boolean isFlexibleTrack(TrackSizingFunction track) {
        return track != null && (track.isFr()
                                 || (track.isMinmax() && track.getMaxFunc() != null && track.getMaxFunc().isFr()));
    }

    /** The track's flex factor (0 for non-flexible tracks). */
    private static float flexFactorOf(TrackSizingFunction track) {
        if (track == null) return 0f;
        if (track.isFr()) return track.getFrValue();
        if (track.isMinmax() && track.getMaxFunc() != null && track.getMaxFunc().isFr()) {
            return track.getMaxFunc().getFrValue();
        }
        return 0f;
    }

    private static TrackSizingFunction trackMinFunction(TrackSizingFunction track) {
        if (track == null) return null;
        return track.isMinmax() ? track.getMinFunc() : track;
    }

    private static TrackSizingFunction trackMaxFunction(TrackSizingFunction track) {
        if (track == null) return null;
        return track.isMinmax() ? track.getMaxFunc() : track;
    }

    /**
     * Whether the track's min track sizing function is intrinsic (auto, min-content, max-content, or
     * the auto min implied by a flexible/fit-content track). Mirrors the classification used by the
     * intrinsic sizing code above.
     */
    private static boolean trackMinIsIntrinsic(TrackSizingFunction track) {
        TrackSizingFunction min = trackMinFunction(track);
        if (track == null || min == null) return true;
        return min.isAuto() || min.isMinContent() || min.isMaxContent() || min.isFr() || min.isFitContent();
    }

    private static boolean trackMinIsMinOrMaxContent(TrackSizingFunction track) {
        TrackSizingFunction min = trackMinFunction(track);
        return min != null && (min.isMinContent() || min.isMaxContent());
    }

    private static boolean trackMinIsMaxContent(TrackSizingFunction track) {
        TrackSizingFunction min = trackMinFunction(track);
        return min != null && min.isMaxContent();
    }

    private static boolean trackMinIsAuto(TrackSizingFunction track) {
        if (track == null) return true;
        TrackSizingFunction min = trackMinFunction(track);
        if (min == null) return true;
        return min.isAuto() || track.isFr() || track.isFitContent();
    }

    /**
     * Whether the track's min sizing function behaves as {@code auto} in the given axis
     * (upstream #1261 {@code MinTrackSizingFunction::behaves_as_auto}): an {@code auto} min, or an
     * indefinite percentage min (percentage tracks whose container size is not yet known).
     */
    private static boolean trackMinBehavesAsAuto(TrackSizingFunction track, float containerAxisSize) {
        if (track == null) return true;
        TrackSizingFunction min = trackMinFunction(track);
        if (min == null || min.isAuto() || track.isFr() || track.isFitContent()) return true;
        return Float.isNaN(containerAxisSize)
               && min.isFixed() && min.getFixedValue() != null && min.getFixedValue().isPercent();
    }

    /** Whether the track's min sizing function is a percentage that has to behave as auto (#1261). */
    private static boolean trackMinIsIndefinitePercent(TrackSizingFunction track, float containerAxisSize) {
        if (track == null || !Float.isNaN(containerAxisSize)) return false;
        TrackSizingFunction min = trackMinFunction(track);
        return min != null && min.isFixed() && min.getFixedValue() != null && min.getFixedValue().isPercent();
    }

    private static boolean trackMaxIsMinContent(TrackSizingFunction track) {
        TrackSizingFunction max = trackMaxFunction(track);
        return max != null && max.isMinContent();
    }

    /** Whether the track's max track sizing function is auto (implicit tracks behave as auto). */
    private static boolean trackMaxIsAuto(TrackSizingFunction track) {
        if (track == null) return true;
        TrackSizingFunction max = trackMaxFunction(track);
        return max == null || max.isAuto();
    }

    /**
     * Resolve the track sizing function of every track in one axis (explicit tracks from the
     * template, implicit tracks from grid-auto-*), using the same cycling rules as the initial
     * sizing pass.
     */
    private static List<TrackSizingFunction> resolveTrackSizingList(
        List<TrackSizingFunction> template,
        List<TrackSizingFunction> autoTracks,
        TrackCounts counts,
        int numTracks) {
        int autoCount = autoTracks != null ? autoTracks.size() : 0;
        int explicitStart = counts.negativeImplicit;
        int templateSize = template != null ? template.size() : 0;
        // The track template ends before the explicit grid does when grid-template-areas creates
        // explicit tracks beyond it (unnamed "." cells or areas extending past the template). Those
        // tracks are sized by grid-auto-* cycling from index 0, forming one contiguous run with the
        // positive implicit grid (upstream initialize_grid_tracks' grid_area_tracks).
        int templateEnd = explicitStart + templateSize;
        List<TrackSizingFunction> tracks = new ArrayList<>(Math.max(1, numTracks));
        for (int i = 0; i < numTracks; i++) {
            TrackSizingFunction track;
            if (i >= explicitStart && i < templateEnd) {
                // Explicit track from the track template
                track = template.get(i - explicitStart);
            } else if (autoCount > 0) {
                int implicitIndex;
                if (i < explicitStart) {
                    int distanceFromExplicit = explicitStart - i;
                    implicitIndex = (autoCount - (distanceFromExplicit % autoCount)) % autoCount;
                } else {
                    int distanceFromTemplate = i - templateEnd;
                    implicitIndex = distanceFromTemplate % autoCount;
                }
                track = autoTracks.get(implicitIndex);
            } else {
                track = null;
            }
            tracks.add(track);
        }
        return tracks;
    }

    /**
     * The item's minimum contribution in the inline axis (CSS Grid 6.6): the min-content contribution
     * for the automatic minimum size, the specified min-width when it is definite, and only the
     * margins for scroll containers (whose automatic minimum size is zero).
     */
    private static float minimumContributionWidth(GridItem item, float minContentContribution,
                                                  float marginWidth, boolean isScrollContainer,
                                                  FloatSize gridAreaSize) {
        // A definite preferred width resolved against the grid area short-circuits to the min-content
        // contribution, which was itself computed from that preferred size.
        if (!Float.isNaN(item.resolvePreferredSize(gridAreaSize).width)) return minContentContribution;
        // The specified min-size is floored at padding+border even for an auto min-size, so only an
        // explicitly specified min-width replaces the content-based automatic minimum. An indefinite
        // grid-area dimension makes a percentage resolve against zero (upstream #1257), which is what
        // stops a cyclic percentage minimum from falling through to the content-based minimum.
        FloatSize minSize = item.resolveMinSizePercentAsZero(gridAreaSize);
        if (hasExplicitMinSize(item.rawMinSize != null ? item.rawMinSize.width : null, minSize.width)) {
            return Math.max(minSize.width + marginWidth, 0f);
        }
        if (isScrollContainer) return Math.max(marginWidth, 0f);
        return minContentContribution;
    }

    /**
     * The part of an item's minimum contribution that can exceed a fixed track maximum: a definite
     * preferred size or an explicitly specified min size (including an indefinite percentage min
     * size, which resolves against zero). The content-based automatic minimum is clamped by the
     * fixed limit and therefore cannot exceed it (CSS Grid 6.6; upstream #1261).
     */
    private static float explicitMinimumContributionExtent(GridItem item, boolean inlineAxis,
                                                           FloatSize gridAreaSize) {
        FloatSize preferred = item.resolvePreferredSize(gridAreaSize);
        float extent = inlineAxis ? preferred.width : preferred.height;
        if (Float.isNaN(extent)) {
            FloatSize min = item.resolveMinSizePercentAsZero(gridAreaSize);
            TaffyDimension rawMin = item.rawMinSize == null
                                    ? null
                                    : (inlineAxis ? item.rawMinSize.width : item.rawMinSize.height);
            float resolvedMin = inlineAxis ? min.width : min.height;
            if (hasExplicitMinSize(rawMin, resolvedMin)) {
                extent = resolvedMin;
            }
        }
        if (Float.isNaN(extent)) return 0f;
        FloatSize margins = item.getMarginAxisSumsWithBaselineShims(gridAreaSize.width);
        return extent + (inlineAxis ? margins.width : margins.height);
    }

    /** Whether the style's min size in one axis is explicitly specified and resolved to a definite value. */
    private static boolean hasExplicitMinSize(TaffyDimension rawMinSize, float resolvedMinSize) {
        return rawMinSize != null && !rawMinSize.isAuto() && !Float.isNaN(resolvedMinSize);
    }

    /**
     * The item's minimum contribution in the block axis. The Java port measures the block-axis
     * min-content contribution as the max-content height (the same value for every item it supports:
     * a block-axis min-content contribution is the content height under a min-content constraint,
     * which equals the max-content height unless a percentage height or an aspect ratio is involved).
     */
    private static float minimumContributionHeight(GridItem item, float contentContribution,
                                                   float marginHeight, boolean isScrollContainer,
                                                   FloatSize gridAreaSize) {
        if (!Float.isNaN(item.resolvePreferredSize(gridAreaSize).height)) return contentContribution;
        FloatSize minSize = item.resolveMinSizePercentAsZero(gridAreaSize);
        if (hasExplicitMinSize(item.rawMinSize != null ? item.rawMinSize.height : null, minSize.height)) {
            return Math.max(minSize.height + marginHeight, 0f);
        }
        if (isScrollContainer) return Math.max(marginHeight, 0f);
        return contentContribution;
    }

    /**
     * CSS Grid 11.5 "Resolve Intrinsic Track Sizes" for items that cross a flexible track
     * (upstream #1019, #1084).
     *
     * <p>Flexible tracks receive intrinsic contributions only from items that cross them, and only
     * the affected flexible tracks (the ones matching the current step's affected predicate) take
     * part. Each step computes every item's own extra space against the base sizes at the start of
     * the step, then applies the per-track maximum of those per-item increases; this matches
     * upstream's per-item item-incurred increase / planned increase model and makes the result
     * independent of item order.</p>
     *
     * <p>When the item's contribution is content-derived, it is scaled by
     * {@code min(crossed flex factor sum, 1)} with the spanned inflexible track sizes as the anchor
     * (CSS Grid 2, 12.5; #1084). Step 1 additionally floors the result at the item's minimum
     * contribution.</p>
     *
     * @param intrinsicConstraint whether the grid container is being sized under a min-/max-content
     *                            constraint (the scaled minimum-contribution rule only applies then)
     * @param maxContentConstraint whether the available space is a max-content constraint (step 3
     *                             only runs then)
     */
    private void resolveFlexibleTrackContributions(
        FloatList sizes,
        List<TrackSizingFunction> tracks,
        float gap,
        List<FlexContribution> items,
        boolean intrinsicConstraint,
        boolean maxContentConstraint) {
        applyFlexibleContributionStep(1, sizes, tracks, gap, items, intrinsicConstraint);
        applyFlexibleContributionStep(2, sizes, tracks, gap, items, intrinsicConstraint);
        if (maxContentConstraint) {
            applyFlexibleContributionStep(3, sizes, tracks, gap, items, intrinsicConstraint);
        }
        applyFlexibleContributionStep(4, sizes, tracks, gap, items, intrinsicConstraint);
    }

    private void applyFlexibleContributionStep(
        int step,
        FloatList sizes,
        List<TrackSizingFunction> tracks,
        float gap,
        List<FlexContribution> items,
        boolean intrinsicConstraint) {
        float[] plannedIncrease = new float[sizes.size()];
        for (FlexContribution item : items) {
            if (step == 1 && !item.crossesIntrinsic()) continue;

            float space;
            if (step == 1) {
                if (intrinsicConstraint) {
                    // Under a min-/max-content constraint: scale the content-derived contribution by
                    // min(crossed flex factor sum, 1) around the spanned inflexible tracks, and floor
                    // it at the item's minimum contribution (#1084).
                    float limitedMinContent = Math.max(item.minContent(), item.minimum());
                    float inflexibleSizes = 0f;
                    float crossedFlexFactorSum = 0f;
                    for (int t = item.start(); t < item.start() + item.span(); t++) {
                        if (isFlexibleTrack(tracks.get(t))) {
                            crossedFlexFactorSum += flexFactorOf(tracks.get(t));
                        } else {
                            inflexibleSizes += sizes.getFloat(t);
                        }
                    }
                    float scale = Math.min(crossedFlexFactorSum, 1f);
                    float excess = Math.max(limitedMinContent - inflexibleSizes, 0f);
                    space = Math.max(item.minimum(), inflexibleSizes + excess * scale);
                } else {
                    space = item.minimum();
                }
            } else if (step == 2) {
                space = item.minContent();
            } else if (step == 3) {
                // Max-content minimums under a max-content constraint: the same scaling, anchored at
                // the spanned inflexible tracks rather than at the current base sizes so that this
                // pass does not compound with the scaling already applied to the minimum contribution.
                float inflexibleSizes = 0f;
                float crossedFlexFactorSum = 0f;
                for (int t = item.start(); t < item.start() + item.span(); t++) {
                    if (isFlexibleTrack(tracks.get(t))) {
                        crossedFlexFactorSum += flexFactorOf(tracks.get(t));
                    } else {
                        inflexibleSizes += sizes.getFloat(t);
                    }
                }
                float scale = Math.min(crossedFlexFactorSum, 1f);
                space = inflexibleSizes + Math.max(item.maxContent() - inflexibleSizes, 0f) * scale;
            } else {
                space = item.maxContent();
            }

            if (space <= 0f) continue;
            distributeFlexibleContribution(step, space, item.start(), item.span(), sizes, tracks, gap, plannedIncrease);
        }

        // Apply the step's planned increase (the maximum per-track item-incurred increase).
        for (int t = 0; t < sizes.size(); t++) {
            if (plannedIncrease[t] > 0f) {
                sizes.set(t, sizes.getFloat(t) + plannedIncrease[t]);
            }
        }
    }

    /**
     * Distribute one item's contribution to the affected flexible tracks it spans (upstream
     * {@code distribute_item_space_to_base_size} with {@code is_flex = true}).
     *
     * <p>The free space is the item's contribution minus the current sizes of all tracks it spans
     * (including the gaps between them). It is then shared between the affected flexible tracks in
     * proportion to their flex factors, or equally when the sum of those factors is zero (#1019).
     * Tracks with a zero flex factor therefore only receive space when every affected track has a
     * zero flex factor.</p>
     */
    private void distributeFlexibleContribution(
        int step,
        float space,
        int start,
        int span,
        FloatList sizes,
        List<TrackSizingFunction> tracks,
        float gap,
        float[] plannedIncrease) {
        float spannedSizes = 0f;
        for (int t = start; t < start + span; t++) {
            spannedSizes += sizes.getFloat(t);
        }
        if (span > 1) {
            spannedSizes += gap * (span - 1);
        }
        float extraSpace = Math.max(space - spannedSizes, 0f);
        if (extraSpace <= 0f) return;

        boolean anyMaxContentMin = false;
        for (int t = start; t < start + span; t++) {
            if (trackMinIsMaxContent(tracks.get(t))) {
                anyMaxContentMin = true;
                break;
            }
        }

        IntList affected = new IntArrayList();
        for (int t = start; t < start + span; t++) {
            TrackSizingFunction track = tracks.get(t);
            if (!isFlexibleTrack(track)) continue;
            boolean isAffected = switch (step) {
                case 1 -> trackMinIsIntrinsic(track);
                case 2 -> trackMinIsMinOrMaxContent(track);
                case 3 -> anyMaxContentMin ? trackMinIsMaxContent(track)
                                           : (trackMinIsAuto(track) && !trackMaxIsMinContent(track));
                default -> trackMinIsMaxContent(track);
            };
            if (isAffected) {
                affected.add(t);
            }
        }
        if (affected.isEmpty()) return;

        // The flex factor sum is computed over the affected tracks only (browser behaviour; #1019).
        float flexFactorSum = 0f;
        for (int i = 0; i < affected.size(); i++) {
            flexFactorSum += flexFactorOf(tracks.get(affected.getInt(i)));
        }
        for (int i = 0; i < affected.size(); i++) {
            int t = affected.getInt(i);
            float share = flexFactorSum > 0f
                          ? extraSpace * flexFactorOf(tracks.get(t)) / flexFactorSum
                          : extraSpace / affected.size();
            if (share > plannedIncrease[t]) {
                plannedIncrease[t] = share;
            }
        }
    }

    /**
     * CSS Grid 11.7 "Expand Flexible Tracks" (upstream {@code expand_flexible_tracks}).
     *
     * @param availableTracksSpace the inner size minus the gaps between tracks, or {@code NaN} when
     *                             the container's inner size is indefinite
     * @param availableSpace       the available space for the axis, used to distinguish a
     *                             min-content constraint (used flex fraction zero) from a
     *                             max-content/indefinite one
     */
    private void expandFlexibleTracks(
        FloatList sizes,
        float gap,
        IntList frTrackIndices,
        FloatList frValues,
        List<FlexContribution> items,
        float availableTracksSpace,
        AvailableSpace availableSpace) {
        if (frTrackIndices.isEmpty()) return;
        float flexFraction = 0f;
        if (!Float.isNaN(availableTracksSpace)) {
            // Definite free space: the used flex fraction fills the available space, but only when
            // the base sizes already fit (otherwise the free space is zero).
            float usedSpace = 0f;
            for (int i = 0; i < sizes.size(); i++) {
                usedSpace += sizes.getFloat(i);
            }
            if (availableTracksSpace - usedSpace <= 0f) return;
            flexFraction = findSizeOfFr(sizes, 0, sizes.size(), frTrackIndices, frValues, availableTracksSpace);
        } else if (availableSpace.isMinContent()) {
            // Under a min-content constraint the used flex fraction is zero.
            return;
        } else {
            // Indefinite free space (max-content constraint): the used flex fraction is the maximum of
            //   - for each flexible track: base_size / flex_factor when the flex factor is > 1, else
            //     the base size;
            //   - for each item crossing a flexible track: find_size_of_fr over the item's spanned
            //     tracks with the item's max-content contribution as the space to fill.
            for (int frIndex = 0; frIndex < frTrackIndices.size(); frIndex++) {
                int i = frTrackIndices.getInt(frIndex);
                float baseSize = Math.max(0f, sizes.getFloat(i));
                float fr = frValues.getFloat(frIndex);
                flexFraction = Math.max(flexFraction, fr > 1f ? baseSize / fr : baseSize);
            }
            for (FlexContribution item : items) {
                int end = item.start() + item.span();
                float spaceToFill = item.maxContent() - gap * Math.max(0, item.span() - 1);
                flexFraction = Math.max(flexFraction,
                    findSizeOfFr(sizes, item.start(), end, frTrackIndices, frValues, spaceToFill));
            }
        }

        for (int frIndex = 0; frIndex < frTrackIndices.size(); frIndex++) {
            int i = frTrackIndices.getInt(frIndex);
            sizes.set(i, Math.max(sizes.getFloat(i), frValues.getFloat(frIndex) * flexFraction));
        }
    }

    /**
     * The track sizing algorithm's "find the size of an fr" (CSS Grid 11.7.1) over a range of the
     * track list. The base sizes of the non-flexible tracks in the range are part of the space that
     * has to be filled, which matters both for flexible track expansion and for the min/max
     * correction of that step ({@code expand_flexible_tracks} / {@code find_size_of_fr} upstream).
     *
     * @param sizes          the current base size of every track in the axis
     * @param frTrackIndices the indices of the tracks whose max track sizing function is flexible
     * @param frValues       the flex factor of each flexible track (same order as {@code frTrackIndices})
     * @param spaceToFill    the space the tracks should fill
     * @return the used flex fraction
     */
    private float findSizeOfFr(
        FloatList sizes,
        int start,
        int end,
        IntList frTrackIndices,
        FloatList frValues,
        float spaceToFill) {
        if (spaceToFill <= 0f || frTrackIndices.isEmpty()) return 0f;

        // The algorithm restarts treating at least one more flexible track as inflexible each time,
        // so a valid hypothetical fr size is found within (number of flexible tracks + 1) iterations.
        // The bound also guarantees termination for non-finite inputs (GR-20 / upstream #1036).
        float hypotheticalFrSize = Float.POSITIVE_INFINITY;
        int maxIterations = frTrackIndices.size() + 1;
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            float usedSpace = 0f;
            float naiveFlexFactorSum = 0f;
            for (int i = start; i < end; i++) {
                float baseSize = sizes.getFloat(i);
                if (Float.isNaN(baseSize)) baseSize = 0f;
                int frIndex = frTrackIndices.indexOf(i);
                if (frIndex >= 0 && frValues.getFloat(frIndex) * hypotheticalFrSize >= baseSize) {
                    naiveFlexFactorSum += frValues.getFloat(frIndex);
                } else {
                    usedSpace += baseSize;
                }
            }
            float leftoverSpace = spaceToFill - usedSpace;
            float flexFactor = Math.max(naiveFlexFactorSum, 1f);
            float previousHypotheticalFrSize = hypotheticalFrSize;
            hypotheticalFrSize = leftoverSpace / flexFactor;

            boolean valid = true;
            for (int frIndex = 0; frIndex < frTrackIndices.size(); frIndex++) {
                int i = frTrackIndices.getInt(frIndex);
                float baseSize = sizes.getFloat(i);
                if (Float.isNaN(baseSize)) baseSize = 0f;
                float flexFactorValue = frValues.getFloat(frIndex);
                // A flexible track is only still flexible if its share at the new hypothetical fr
                // size covers its base size, unless it already failed to do so in the previous pass.
                if (!(flexFactorValue * hypotheticalFrSize >= baseSize
                      || flexFactorValue * previousHypotheticalFrSize < baseSize)) {
                    valid = false;
                    break;
                }
            }
            if (valid) break;
        }
        return Math.max(0f, hypotheticalFrSize);
    }

    /**
     * Redo the flexible track expansion against a definite content-box min/max size (CSS Grid 11.7
     * "expand flexible tracks" min/max correction; upstream #1023). Only called when the container's
     * inner size is indefinite but its style min/max size is definite.
     *
     * @param tracksAvailableMin the content-box min size (in tracks + gaps space), or NaN
     * @param tracksAvailableMax the content-box max size (in tracks + gaps space), or NaN
     */
    private void applyFlexibleTrackMinMaxCorrection(
        FloatList sizes,
        IntList frTrackIndices,
        FloatList frValues,
        float tracksAvailableMin,
        float tracksAvailableMax) {
        if (frTrackIndices.isEmpty()) return;
        if (Float.isNaN(tracksAvailableMin) && Float.isNaN(tracksAvailableMax)) return;

        float hypotheticalGridSize = 0f;
        for (int i = 0; i < sizes.size(); i++) {
            float size = sizes.getFloat(i);
            hypotheticalGridSize += Float.isNaN(size) ? 0f : size;
        }

        float flexFraction;
        // Note: min size takes precedence over max size (upstream comment).
        if (!Float.isNaN(tracksAvailableMin) && hypotheticalGridSize < tracksAvailableMin) {
            flexFraction = findSizeOfFr(sizes, 0, sizes.size(), frTrackIndices, frValues, tracksAvailableMin);
        } else if (!Float.isNaN(tracksAvailableMax) && hypotheticalGridSize > tracksAvailableMax) {
            flexFraction = findSizeOfFr(sizes, 0, sizes.size(), frTrackIndices, frValues, tracksAvailableMax);
        } else {
            return;
        }

        for (int frIndex = 0; frIndex < frTrackIndices.size(); frIndex++) {
            int i = frTrackIndices.getInt(frIndex);
            float baseSize = sizes.getFloat(i);
            if (Float.isNaN(baseSize)) baseSize = 0f;
            sizes.set(i, Math.max(baseSize, frValues.getFloat(frIndex) * flexFraction));
        }
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
                    // minmax(<anything>, 1fr) is a flexible track too (its max function is flexible)
                    if (isFlexibleTrack(track)) {
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
                    if (isFlexibleTrack(track)) {
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
            // An explicit track created by grid-template-areas beyond the track template is sized
            // by grid-auto-*, cycling from index 0 (GR-16 / upstream #1024).
            if (auto != null && !auto.isEmpty()) {
                int templateSize = template != null ? template.size() : 0;
                return auto.get(Math.max(0, templateIndex - templateSize) % auto.size());
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
     * This method computes the baseline and baseline_shim for each item that participates in baseline
     * alignment (see {@link GridItem#participatesInBaselineAlignment()}): it has
     * {@code align-self: baseline}, no auto block-axis margin (#1125) and no cyclic block-size
     * dependency on its row (3dbf2e46). Items that do not participate are neither measured nor
     * shimmed, and do not contribute their baseline to the row (upstream only measures the
     * participants). Items are grouped by row; a row with one or zero participants is a no-op.
     * <p>
     * The stored baseline is relative to the top of the item's border box (upstream #1322); the top
     * margin is added only when items are aligned with each other (via
     * {@link GridItem#marginBoxBaseline(float)}). The baseline_shim acts as an extra top margin
     * during track sizing and final positioning.
     */
    private void resolveItemBaselines(List<GridItem> items, FloatSize innerNodeSize) {
        // Determine if the grid has any item participating in baseline alignment
        boolean hasBaselineItem = false;
        for (GridItem item : items) {
            if (item.participatesInBaselineAlignment()) {
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

            // Count how many items in this row participate in baseline alignment
            int baselineItemCount = 0;
            for (GridItem item : rowItems) {
                if (item.participatesInBaselineAlignment()) {
                    baselineItemCount++;
                }
            }

            // If a row has one or zero items participating in baseline alignment then skip
            if (baselineItemCount <= 1) {
                continue;
            }

            // Compute the baselines of the items in the row participating in baseline alignment
            for (GridItem item : rowItems) {
                if (!item.participatesInBaselineAlignment()) {
                    continue;
                }

                // Measure the item to get its baseline
                LayoutOutput output = layoutComputer.performChildLayout(
                    item.nodeId,
                    new FloatSize(NaN, NaN),
                    innerNodeSize,
                    new TaffySize<>(AvailableSpace.minContent(), AvailableSpace.minContent()),
                    SizingMode.INHERENT_SIZE,
                    new TaffyLine<>(false, false)
                );

                // Scroll-container items' baselines are clamped to their border box, and a
                // scroll container without a baseline synthesizes one at its border-box bottom
                // edge (upstream #1108, shared helper from #1323). A non-scroll item with no
                // baseline falls back to its border-box height at the call site, exactly like
                // upstream's child_first_baseline(...).unwrap_or(height).
                float height = output.size().height;
                float baselineValue = BaselineHelper.childFirstBaseline(
                    output, item.overflow.y.isScrollContainer());

                // The baseline is stored relative to the top of the item's border box; the top
                // margin is added only where items are aligned with each other (upstream #1322).
                item.baseline = !Float.isNaN(baselineValue) ? baselineValue : height;
            }

            // Items are aligned by the distance from the top of their margin box to their baseline
            float rowMaxBaseline = -Float.MAX_VALUE;
            for (GridItem item : rowItems) {
                if (item.participatesInBaselineAlignment()) {
                    rowMaxBaseline = Math.max(rowMaxBaseline, item.marginBoxBaseline(innerNodeSize.width));
                }
            }

            // Compute the baseline shim for each item in the row participating in baseline alignment
            for (GridItem item : rowItems) {
                if (item.participatesInBaselineAlignment()) {
                    item.baselineShim = rowMaxBaseline - item.marginBoxBaseline(innerNodeSize.width);
                    // The baseline shim is part of the item's margin sums, which feed into its known
                    // dimensions, so any cached known dimensions are stale after this point
                    // (upstream #1226 clears known_dimensions_cache here for the same reason).
                    item.clearKnownDimensionsCache();
                }
            }
        }
    }

    private FloatList calculateRowSizes(
        TaffyStyle style,
        FloatSize nodeInnerSize,
        float innerMinHeight,
        float innerMaxHeight,
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

        float usedSpace = 0;
        int autoCount = 0;
        // Flexible track indices/values (needed for the min/max correction of flexible expansion)
        IntList frTrackIndices = new IntArrayList();
        FloatList frValues = new FloatArrayList();

        // Get auto rows for implicit tracks
        List<TrackSizingFunction> autoRows = style.getGridAutoRows();
        int autoRowCount = (autoRows != null && !autoRows.isEmpty()) ? autoRows.size() : 0;
        int explicitRowCount = (expandedRows != null) ? expandedRows.size() : 0;

        // Use rowCounts to determine which tracks are explicit vs implicit
        int negativeImplicitCount = rowCounts.negativeImplicit;
        int explicitStart = negativeImplicitCount;
        int explicitEnd = explicitStart + explicitRowCount;

        // Per-index track sizing functions, used for the grid-area-size estimates (GR-06 / #960).
        List<TrackSizingFunction> resolvedRowTracks = resolveTrackSizingList(expandedRows, autoRows, rowCounts, numRows);

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
                frTrackIndices.add(i);
                frValues.add(track.getFrValue());
                sizes.add(NaN);
            } else if (track == null || track.isAuto() || track.isMinContent() || track.isMaxContent()) {
                // Auto or content-based track - will be resolved later
                autoCount++;
                sizes.add(NaN);
            } else if (track.isMinmax()) {
                // For minmax, check if max is flexible
                TrackSizingFunction minF = track.getMinFunc();
                TrackSizingFunction maxF = track.getMaxFunc();
                if (maxF != null && maxF.isFr()) {
                    frTrackIndices.add(i);
                    frValues.add(maxF.getFrValue());
                    float frBase = (minF != null && minF.isFixed())
                                   ? Math.max(0f, minF.getFixedValue().resolveOrZero(nodeInnerSize.height)) : 0f;
                    sizes.add(frBase);
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

        // GR-12 / upstream #1261: when the container's height is indefinite, an indefinite
        // percentage min track sizing function behaves as auto. See the inline-axis pass for the
        // rationale (explicit min sizes and definite preferred sizes can exceed the fixed maximum).
        for (int i = 0; i < numRows; i++) {
            if (!trackMinIsIndefinitePercent(resolvedRowTracks.get(i), nodeInnerSize.height)) continue;
            float maxLimit = trackDefiniteValue(trackMaxFunction(resolvedRowTracks.get(i)), nodeInnerSize.height);
            if (Float.isNaN(maxLimit) || Float.isNaN(sizes.getFloat(i))) continue;
            float base = sizes.getFloat(i);
            for (int itemIdx = 0; itemIdx < items.size(); itemIdx++) {
                GridItem item = items.get(itemIdx);
                int row = getItemRowWithCounts(item, itemIdx, columnSizes.size(), rowCounts);
                if (row != i || item.rowSpan != 1) continue;
                float itemWidth = computeItemAvailableWidth(item, columnSizes, gap);
                FloatSize itemGridAreaSize = new FloatSize(itemWidth, NaN);
                base = Math.max(base, explicitMinimumContributionExtent(item, false, itemGridAreaSize));
            }
            sizes.set(i, base);
        }

        // Second pass: handle fr and auto tracks
        // Per CSS Grid spec, fr units are equivalent to minmax(auto, Nfr)
        // This means the track's minimum size is determined by its content (auto),
        // and then it may grow larger based on the fr distribution.


        // Resolve the non-flexible tracks' base sizes. Fixed rows were resolved in the first pass;
        // auto/content-sized rows get their content height. An auto-min row in a container with a
        // definite height uses the items' minimum contributions instead (GR-12 / upstream #1257),
        // which the 11.8 stretch step then grows to the available space.
        boolean definiteRowContainerHeight = !Float.isNaN(nodeInnerSize.height);
        for (int i = 0; i < sizes.size(); i++) {
            if (Float.isNaN(sizes.getFloat(i)) && !frTrackIndices.contains(i)) {
                boolean autoMinRow = trackMinBehavesAsAuto(resolvedRowTracks.get(i), nodeInnerSize.height);
                sizes.set(i, definiteRowContainerHeight && autoMinRow
                    ? calculateAutoRowMinimumContribution(items, columnSizes, nodeInnerSize, colCounts, gap.width,
                        itemIndicesByRow[i], sizes, resolvedRowTracks)
                    : calculateAutoRowHeight(items, columnSizes, nodeInnerSize, colCounts, gap.width,
                        itemIndicesByRow[i], sizes, resolvedRowTracks));
            }
        }
        // Flexible rows start at a zero base size until the flexible contribution pass gives them one.
        for (int frIndex = 0; frIndex < frTrackIndices.size(); frIndex++) {
            int i = frTrackIndices.getInt(frIndex);
            if (Float.isNaN(sizes.getFloat(i))) {
                sizes.set(i, 0f);
            }
        }

        // 11.5 Resolve Intrinsic Track Sizes, flexible rows (GR-01 / upstream #1019, #1084). See the
        // inline-axis path for the rationale of the scaled, affected-only, minimum-floored
        // distribution.
        List<FlexContribution> flexItemContributions = new ArrayList<>();
        boolean intrinsicHeightConstraint = gridHeightSpace.isMinContent() || gridHeightSpace.isMaxContent();
        for (int itemIdx = 0; itemIdx < items.size(); itemIdx++) {
            GridItem item = items.get(itemIdx);
            if (!item.crossesFlexibleRow) continue;

            int row = getItemRowWithCounts(item, itemIdx, columnSizes.size(), rowCounts);
            int span = item.rowSpan;

            // The item's block-axis content contribution: the same content-height estimate the rest
            // of the row sizing uses (it accounts for the item's grid area width, margins, padding
            // and explicit height).
            IntList singleItemRow = new IntArrayList();
            singleItemRow.add(itemIdx);
            float itemContent = calculateAutoRowHeight(
                items, columnSizes, nodeInnerSize, colCounts, gap.width, singleItemRow, sizes, resolvedRowTracks);

            // The item's grid area: the inline size comes from the known column sizes, the block
            // size from the current row sizes (indefinite unless all spanned rows are definite).
            float itemWidth = computeItemAvailableWidth(item, columnSizes, gap);
            FloatSize itemGridAreaSize = new FloatSize(itemWidth,
                definiteGridAreaExtent(sizes, resolvedRowTracks, row, span, nodeInnerSize.height));
            FloatSize marginAxisSums = item.getMarginAxisSumsWithBaselineShims(itemGridAreaSize.width);
            boolean isScrollContainer = item.overflow != null
                && (item.overflow.y == Overflow.HIDDEN || item.overflow.y == Overflow.SCROLL || item.overflow.y == Overflow.CLIP);
            float minimumContribution = minimumContributionHeight(item, itemContent, marginAxisSums.height,
                isScrollContainer, itemGridAreaSize);
            boolean crossesIntrinsic = false;
            for (int r = row; r < row + span && r < numRows; r++) {
                if (trackMinIsIntrinsic(resolvedRowTracks.get(r))) {
                    crossesIntrinsic = true;
                    break;
                }
            }
            flexItemContributions.add(new FlexContribution(row, span, crossesIntrinsic, minimumContribution,
                itemContent, itemContent));
        }
        resolveFlexibleTrackContributions(sizes, resolvedRowTracks, gap.height, flexItemContributions,
            intrinsicHeightConstraint, availableGridSpace.height.isMaxContent());

        // 11.7 Expand Flexible Tracks (GR-01): see the inline-axis path.
        float expansionAvailableHeight = Float.isNaN(nodeInnerSize.height)
                                         ? NaN
                                         : nodeInnerSize.height - gap.height * Math.max(0, numRows - 1);
        // A min-content constraint in the block axis is treated as a max-content constraint when
        // expanding flexible tracks: the min-content block size of a grid container is its
        // max-content block size (GR-12 / upstream #1284). The inline axis keeps the min-content
        // behaviour.
        AvailableSpace rowExpansionSpace = gridHeightSpace.isMinContent()
                                           ? AvailableSpace.maxContent()
                                           : gridHeightSpace;
        expandFlexibleTracks(sizes, gap.height, frTrackIndices, frValues, flexItemContributions,
            expansionAvailableHeight, rowExpansionSpace);

        // 11.7 Expand Flexible Tracks: min/max correction (GR-03 / upstream #1023). See the column
        // path for the rationale; the inner min/max sizes are content-box values and the gaps are not
        // part of the track list here.
        if (Float.isNaN(nodeInnerSize.height)) {
            applyFlexibleTrackMinMaxCorrection(sizes, frTrackIndices, frValues,
                TaffyMath.maybeSub(innerMinHeight, gap.height * Math.max(0, numRows - 1)),
                TaffyMath.maybeSub(innerMaxHeight, gap.height * Math.max(0, numRows - 1)));
        }

        // 11.8 Stretch auto Tracks: auto rows absorb the remaining positive, definite free space.
        // When the container's inner size is indefinite the content-box min-size provides the space
        // to stretch into (GR-03 / #1023); a min-content constraint leaves no free space.
        float stretchSpace = NaN;
        if (!Float.isNaN(nodeInnerSize.height)) {
            stretchSpace = availableHeight;
        } else if (!Float.isNaN(innerMinHeight) && !gridHeightSpace.isMinContent()) {
            stretchSpace = TaffyMath.maybeSub(innerMinHeight, gap.height * Math.max(0, numRows - 1));
        }
        if (!Float.isNaN(stretchSpace)) {
            int autoTrackCount = 0;
            for (int i = 0; i < sizes.size(); i++) {
                TrackSizingFunction track = resolvedRowTracks.get(i);
                if (!isFlexibleTrack(track) && trackMaxIsAuto(track)) {
                    autoTrackCount++;
                }
            }
            float stretchUsedSpace = 0f;
            for (int i = 0; i < sizes.size(); i++) {
                stretchUsedSpace += sizes.getFloat(i);
            }
            float stretchFreeSpace = stretchSpace - stretchUsedSpace;
            if (autoTrackCount > 0 && stretchFreeSpace > 0.001f) {
                float extraPerTrack = stretchFreeSpace / autoTrackCount;
                for (int i = 0; i < sizes.size(); i++) {
                    TrackSizingFunction track = resolvedRowTracks.get(i);
                    if (!isFlexibleTrack(track) && trackMaxIsAuto(track)) {
                        sizes.set(i, sizes.getFloat(i) + extraPerTrack);
                    }
                }
            }
        }

        // Final pass: ensure no null values remain (fallback for edge cases)
        for (int i = 0; i < sizes.size(); i++) {
            if (Float.isNaN(sizes.getFloat(i))) {
                // Calculate auto row height as fallback
                float maxRowHeight = calculateAutoRowHeight(items, columnSizes, nodeInnerSize, colCounts, gap.width,
                    itemIndicesByRow[i], sizes, resolvedRowTracks);
                sizes.set(i, maxRowHeight); // Use actual content height, can be 0 for empty rows
            }
        }

        return sizes;
    }

    private float calculateAutoRowHeight(List<GridItem> items, FloatList columnSizes, FloatSize nodeInnerSize,
                                         TrackCounts colCounts, float columnGap, IntList rowItemIndices,
                                         FloatList rowTrackSizes, List<TrackSizingFunction> rowTracks) {
        float maxRowHeight = 0;

        // Only iterate over items that belong to this row (O(items_in_row) instead of O(all_items))
        for (int idx = 0; idx < rowItemIndices.size(); idx++) {
            int i = rowItemIndices.getInt(idx);
            GridItem item = items.get(i);
            float height;

                // Get grid area size for this item (the grid area is the containing block for its
                // percentage sizes, margins and padding; upstream #960).
                int col = getItemColumnWithCounts(item, i, columnSizes.size(), colCounts);
                float gridAreaWidth = 0f;
                int endCol = Math.min(col + item.columnSpan, columnSizes.size());
                for (int c = col; c < endCol; c++) {
                    gridAreaWidth += columnSizes.getFloat(c);
                }
                // Add gaps between spanned columns
                int numGaps = Math.max(0, endCol - col - 1);
                gridAreaWidth += numGaps * columnGap;

                int row = getItemRowWithCounts(item, i, columnSizes.size(), colCounts);
                float gridAreaHeight = definiteGridAreaExtent(rowTrackSizes, rowTracks, row, item.rowSpan,
                    nodeInnerSize.height);
                FloatSize gridAreaSize = new FloatSize(gridAreaWidth, gridAreaHeight);

                // Calculate minimum size from padding+border (size cannot be less than this)
                float itemPaddingBorderHeight = item.paddingBorderSize(gridAreaSize).height;

                // Get margin axis sums using the Rust logic:
                // - Horizontal percentage margins resolve to 0 to avoid cyclic dependency
                // - Vertical percentage margins resolve against the grid area's inline size
                FloatSize marginAxisSums = item.getMarginAxisSumsWithBaselineShims(gridAreaSize.width);

                // Compute known dimensions using the proper method that respects item's percentage sizes
                FloatSize itemKnownDimensions = item.computeKnownDimensions(gridAreaSize);

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
                        height = item.getMaxContentContributionHeightCached(layoutComputer, gridAreaSize);
                    }
                }

                maxRowHeight = Math.max(maxRowHeight, height);
        }

        return maxRowHeight;
    }

    /**
     * The aggregate minimum contribution of the items starting in one row (CSS Grid 6.6), used as
     * the base size of an auto-min row whose container has a definite height (GR-12 / upstream
     * #1257): a scroll container contributes no content size, a cyclic percentage min-size resolves
     * against zero, and an explicit min-size or definite preferred size replaces the content-based
     * automatic minimum.
     */
    private float calculateAutoRowMinimumContribution(List<GridItem> items, FloatList columnSizes,
                                                      FloatSize nodeInnerSize, TrackCounts colCounts, float columnGap,
                                                      IntList rowItemIndices, FloatList rowTrackSizes,
                                                      List<TrackSizingFunction> rowTracks) {
        float maxMinimum = 0f;
        for (int idx = 0; idx < rowItemIndices.size(); idx++) {
            int i = rowItemIndices.getInt(idx);
            GridItem item = items.get(i);

            int col = getItemColumnWithCounts(item, i, columnSizes.size(), colCounts);
            float gridAreaWidth = 0f;
            int endCol = Math.min(col + item.columnSpan, columnSizes.size());
            for (int c = col; c < endCol; c++) {
                gridAreaWidth += columnSizes.getFloat(c);
            }
            gridAreaWidth += Math.max(0, endCol - col - 1) * columnGap;

            int row = getItemRowWithCounts(item, i, columnSizes.size(), colCounts);
            float gridAreaHeight = definiteGridAreaExtent(rowTrackSizes, rowTracks, row, item.rowSpan,
                nodeInnerSize.height);
            FloatSize gridAreaSize = new FloatSize(gridAreaWidth, gridAreaHeight);

            FloatSize marginAxisSums = item.getMarginAxisSumsWithBaselineShims(gridAreaSize.width);
            boolean isScrollContainer = item.overflow != null
                && (item.overflow.y == Overflow.HIDDEN || item.overflow.y == Overflow.SCROLL
                    || item.overflow.y == Overflow.CLIP);

            FloatSize preferredSize = item.resolvePreferredSize(gridAreaSize);
            float contentContribution;
            if (!Float.isNaN(preferredSize.height)) {
                contentContribution = Math.max(preferredSize.height, item.paddingBorderSize(gridAreaSize).height)
                                      + marginAxisSums.height;
            } else {
                contentContribution = item.getMaxContentContributionHeightCached(layoutComputer, gridAreaSize);
            }

            maxMinimum = Math.max(maxMinimum, minimumContributionHeight(item, contentContribution,
                marginAxisSums.height, isScrollContainer, gridAreaSize));
        }
        return maxMinimum;
    }

    /**
     * Merges and resolves the self-alignment of one grid item (upstream
     * {@code align_and_position_item}, #1077/#1254).
     *
     * <p>An unset ({@link AlignItems#AUTO}) item-level value inherits the container's
     * align-items/justify-items; an explicit item value overrides it. The merged value is then
     * resolved against the item's own direction ({@code self-start}/{@code self-end}, #1077), and a
     * {@code normal} keyword (explicit or inherited) uses the layout-mode default: a grid item with a
     * definite size in the axis (or a preferred aspect ratio in the block axis) is start-aligned,
     * everything else stretches (#1254). The legacy unset sentinel keeps the same contextual default
     * as before this batch, so legacy values are unaffected.</p>
     *
     * @param itemAlignment      the item's align-self/justify-self
     * @param containerAlignment the container's align-items/justify-items
     * @param axisIsInline       true for the inline (column) axis, false for the block (row) axis
     * @param itemDirection      the item's resolved direction
     * @param containerDirection the container's resolved direction
     * @param hasDefiniteSize    whether the item has a definite size in this axis
     * @param hasAspectRatio     whether the item has a preferred aspect ratio (block axis only)
     * @return the resolved alignment, never null
     */
    private static AlignItems resolveGridItemAlignment(
        AlignItems itemAlignment,
        AlignItems containerAlignment,
        boolean axisIsInline,
        TaffyDirection itemDirection,
        TaffyDirection containerDirection,
        boolean hasDefiniteSize,
        boolean hasAspectRatio) {

        AlignItems merged = AlignmentHelper.isUnset(itemAlignment) ? containerAlignment : itemAlignment;
        merged = AlignmentHelper.resolveSelfRelative(merged, itemDirection, containerDirection, axisIsInline);
        if (AlignmentHelper.isUnset(merged) || AlignmentHelper.isNormal(merged)) {
            boolean startAligned = axisIsInline ? hasDefiniteSize : hasDefiniteSize || hasAspectRatio;
            return startAligned ? AlignItems.START : AlignItems.STRETCH;
        }
        return merged;
    }

    /**
     * Sizes, aligns and positions the in-flow items, performing their final layout.
     *
     * <p>Every item's scrollable overflow is turned into a contribution in the container's
     * padding-box frame (upstream {@code compute_scrollable_overflow_contribution}, SO-01/SO-02):
     * the item's own rectangle comes from its final layout output, its border moves it from the
     * client area to the border box, and unreachable overflow is dropped when the container is a
     * scroll container. The contributions are unioned into the returned rectangle, which the
     * container finalizes together with its own padding and the out-of-flow children's
     * rectangles.</p>
     *
     * @param paddingBoxOrigin  the padding-box origin in the container's border-box frame
     * @param overflowContainer the container description used to resolve the contributions
     * @return the union of the items' overflow contributions
     */
    private FloatRect placeItems(
        List<GridItem> items,
        FloatList columnSizes,
        FloatList rowSizes,
        FloatSize gap,
        FloatRect contentBoxInset,
        FloatSize nodeInnerSize,
        TaffyStyle containerStyle,
        TrackCounts colCounts,
        TrackCounts rowCounts,
        AxisGeometry columnGeometry,
        AxisGeometry rowGeometry,
        boolean isRtl,
        FloatPoint paddingBoxOrigin,
        ScrollableOverflowUtil.Container overflowContainer) {

        FloatRect itemOverflowRect = FloatRect.zero();

        int numColumns = columnSizes.size();
        int numRows = rowSizes.size();

        // Count collapsed tracks for alignment calculations (GR-07 / upstream #946)
        int nonCollapsedColumns = columnGeometry.nonCollapsedTrackCount();
        int nonCollapsedRows = rowGeometry.nonCollapsedTrackCount();

        // Default alignment from container
        AlignItems defaultAlignItems = containerStyle.getAlignItems();
        AlignItems defaultJustifyItems = containerStyle.justifyItems;

        // Used track sizes (collapsed tracks contribute neither a size nor a surviving gutter)
        float totalColumnSize = columnGeometry.usedSize(columnSizes, gap.width);
        float totalRowSize = rowGeometry.usedSize(rowSizes, gap.height);

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

            // Clamp into the grid so that a placement clamped by the limited grid (GR-05) cannot
            // index past the track arrays.
            col = Math.max(0, Math.min(col, numColumns - 1));
            row = Math.max(0, Math.min(row, numRows - 1));

            // Calculate span - number of tracks item covers
            int colSpan = item.columnSpan;
            int rowSpan = item.rowSpan;

            // Clamp to available tracks
            if (col + colSpan > numColumns) colSpan = numColumns - col;
            if (row + rowSpan > numRows) rowSpan = numRows - row;
            if (colSpan < 1) colSpan = 1;
            if (rowSpan < 1) rowSpan = 1;

            // Track size (available space for item): sum of the spanned tracks plus the surviving
            // gutters between them. Items are never placed in a collapsed track (their occupancy is
            // what keeps a track from collapsing), so only the gutters inside the span are counted.
            float trackWidth = columnGeometry.trackSpanSize(columnSizes, col, colSpan, adjustedGapX);
            float trackHeight = rowGeometry.trackSpanSize(rowSizes, row, rowSpan, adjustedGapY);

            // Track position with content alignment offset. For RTL, column indices are logical
            // (counting from inline-start), so the x-coordinate is mirrored.
            float trackY = contentBoxInset.top + contentOffsetY
                           + rowGeometry.trackStart(rowSizes, row, adjustedGapY);
            float trackX;
            if (isRtl) {
                trackX = contentBoxInset.left + (innerWidth - contentOffsetX
                         - columnGeometry.trackStart(columnSizes, col, adjustedGapX) - trackWidth);
            } else {
                trackX = contentBoxInset.left + contentOffsetX
                         + columnGeometry.trackStart(columnSizes, col, adjustedGapX);
            }

            // The grid area is the item's positioning rectangle and, for a grid item, also its
            // containing block. They are carried as two separate values: the alignment/positioning
            // code below uses the grid area, while percentage-dependent styles and the child's
            // parent size use the containing block (GR-06 / upstream #1271).
            TaffyTree tree = layoutComputer.getTree();
            TaffyStyle childStyle = tree.getStyle(item.nodeId);
            FloatSize gridAreaSize = new FloatSize(trackWidth, trackHeight);
            FloatSize containingBlockSize = gridAreaSize;

            // Re-resolve the item's percentage styles against the containing block (upstream #960):
            // preferred/min/max size, padding, border and margins are no longer the container-relative
            // values computed when the items were generated. For a grid item the containing block is
            // the grid area, whether that area's size came from a definite container or from content.
            TaffyRect<LengthPercentageAuto> marginStyle = childStyle.getMargin();
            FloatRect margin = item.resolveMargin(containingBlockSize);
            FloatRect itemPadding = item.resolvePadding(containingBlockSize);
            FloatRect itemBorder = item.resolveBorder(containingBlockSize);
            item.padding = itemPadding;
            item.border = itemBorder;
            FloatSize itemPaddingBorderSize = item.paddingBorderSize(containingBlockSize);
            FloatSize preferredSize = item.resolvePreferredSize(containingBlockSize);
            FloatSize resolvedMinSize = item.resolveMinSize(containingBlockSize);
            FloatSize resolvedMaxSize = item.resolveMaxSize(containingBlockSize);

            // Check if margins are auto (affects stretch behavior)
            boolean hasHorizontalAutoMargin = marginStyle.left.isAuto() || marginStyle.right.isAuto();
            boolean hasVerticalAutoMargin = marginStyle.top.isAuto() || marginStyle.bottom.isAuto();

            // Determine alignment following CSS Grid spec (alignment.rs) and the upstream
            // #1077/#1254/#952 model:
            // 1. The item's explicit align-self/justify-self wins; unset (AUTO) inherits the
            //    container's align-items/justify-items (defaultAlignItems/defaultJustifyItems).
            // 2. self-start/self-end resolve against the item's own direction: the inline axis flips
            //    when the item's direction differs from the container's, the block axis never flips
            //    in horizontal-tb.
            // 3. An explicit (or inherited) 'normal' is layout-mode dependent: START when the item has
            //    a definite size in the axis (or a preferred aspect ratio in the block axis), STRETCH
            //    otherwise. The legacy unset sentinel keeps exactly the same contextual default.
            TaffyDirection containerDirection = isRtl ? TaffyDirection.RTL : TaffyDirection.LTR;
            TaffyDirection itemDirection = layoutComputer.resolveDirection(item.nodeId);

            AlignItems alignY = resolveGridItemAlignment(
                childStyle.getAlignSelf(),
                defaultAlignItems,
                false,
                itemDirection,
                containerDirection,
                !Float.isNaN(preferredSize.height),
                item.aspectRatio != null && !Float.isNaN(item.aspectRatio));
            AlignItems alignX = resolveGridItemAlignment(
                childStyle.justifySelf,
                defaultJustifyItems,
                true,
                itemDirection,
                containerDirection,
                !Float.isNaN(preferredSize.width),
                false);

            // Per CSS Grid spec: auto margins prevent stretch
            // When margin is auto, the item should use its content size and auto margins absorb free space
            boolean stretchWidth = alignX == AlignItems.STRETCH && !hasHorizontalAutoMargin;
            boolean stretchHeight = alignY == AlignItems.STRETCH && !hasVerticalAutoMargin;

            // Available space after margin
            float availableWidth = trackWidth - margin.left - margin.right;
            float availableHeight = trackHeight - margin.top - margin.bottom;

            // Calculate minimum size from padding+border (size cannot be less than this)
            float itemPaddingBorderWidth = itemPaddingBorderSize.width;
            float itemPaddingBorderHeight = itemPaddingBorderSize.height;

            // Calculate size based on alignment
            float width, height;

            if (!Float.isNaN(preferredSize.width)) {
                // Preferred size resolved against the grid area (but at least padding+border)
                width = Math.max(preferredSize.width, itemPaddingBorderWidth);
            } else if (stretchWidth) {
                // Stretch to fill
                width = availableWidth;
            } else {
                // Content-based size (START, CENTER, END, or has auto margin) - measure child. The
                // containing block is passed as the child's parent size so that its own percentage
                // styles resolve against the grid area rather than the grid container (#1271).
                LayoutOutput output = layoutComputer.performChildLayout(
                    item.nodeId,
                    new FloatSize(NaN, NaN),
                    containingBlockSize,
                    new TaffySize<>(AvailableSpace.definite(availableWidth), AvailableSpace.definite(availableHeight)),
                    SizingMode.INHERENT_SIZE,
                    new TaffyLine<>(false, false)
                );
                width = output.size().width;
            }

            // Reapply aspect ratio after width is determined
            // If aspect_ratio is set and height is not explicitly set, calculate height from width
            float heightFromAspectRatio = NaN;
            if (item.aspectRatio != null && !Float.isNaN(item.aspectRatio) && Float.isNaN(preferredSize.height)) {
                heightFromAspectRatio = width / item.aspectRatio;
            }

            if (!Float.isNaN(preferredSize.height)) {
                // Preferred size resolved against the grid area (but at least padding+border)
                height = Math.max(preferredSize.height, itemPaddingBorderHeight);
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
                    containingBlockSize,
                    new TaffySize<>(AvailableSpace.definite(width), AvailableSpace.definite(availableHeight)),
                    SizingMode.INHERENT_SIZE,
                    new TaffyLine<>(false, false)
                );
                height = output.size().height;
            }

            // Reapply aspect ratio after height is determined (for cases where width is not set but height is)
            if (item.aspectRatio != null && !Float.isNaN(item.aspectRatio) && Float.isNaN(preferredSize.width) && !Float.isNaN(preferredSize.height)) {
                width = height * item.aspectRatio;
            }

            // Apply min/max constraints (resolved against the grid area)
            width = TaffyMath.clamp(width, resolvedMinSize.width, resolvedMaxSize.width);
            height = TaffyMath.clamp(height, resolvedMinSize.height, resolvedMaxSize.height);

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

            // A 'safe' overflow-position keyword falls back to logical start when the item's margin box
            // overflows its grid area (upstream #952, CSS Box Alignment 4.3). Auto margins absorb the
            // free space and therefore do not count; the baseline shim acts like an extra top margin.
            // Non-safe keywords are only stripped of the safety modifier, so legacy geometry is
            // unchanged.
            float nonAutoMarginX = (marginStyle.left.isAuto() ? 0f : marginLeft)
                                   + (marginStyle.right.isAuto() ? 0f : marginRight);
            float nonAutoMarginY = (marginStyle.top.isAuto() ? 0f : marginTop)
                                   + (marginStyle.bottom.isAuto() ? 0f : marginBottom)
                                   + item.baselineShim;
            alignX = AlignmentHelper.resolveSelfSafety(alignX, width + nonAutoMarginX > trackWidth);
            alignY = AlignmentHelper.resolveSelfSafety(alignY, height + nonAutoMarginY > trackHeight);

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

            // The item's final y position without any relative offset: a relative offset moves the
            // box but does not change the container's baseline (upstream #1323).
            item.yPosition = y;
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
            LayoutOutput finalOutput = layoutComputer.performChildLayout(
                item.nodeId,
                new FloatSize(width, height),  // known dimensions
                containingBlockSize,  // the containing block for percentage styles (#1271)
                new TaffySize<>(AvailableSpace.definite(width), AvailableSpace.definite(height)),
                SizingMode.INHERENT_SIZE,
                new TaffyLine<>(false, false)
            );

            // The container's baseline is taken from the item's final layout rather than from the
            // measurement done during track sizing (upstream #1322). Scroll containers are clamped
            // to their border box by the shared helper (upstream #1108/#1323); an item without a
            // baseline leaves NaN here and the container baseline falls back to its height.
            item.baseline = BaselineHelper.childFirstBaseline(
                finalOutput, item.overflow.y.isScrollContainer());

            // Accumulate the item's scrollable overflow contribution. The item's rectangle is
            // expressed relative to its own client area, its border moves it into the border-box
            // frame, and shifting by the padding-box origin puts it in the container's
            // padding-box frame (upstream grid/alignment.rs). A relative inset offset moves the
            // box, so the final (stored) location is the one that counts.
            item.scrollableOverflow = finalOutput.scrollableOverflow();
            itemOverflowRect = ScrollableOverflowUtil.union(
                itemOverflowRect,
                ScrollableOverflowUtil.computeContribution(
                    new FloatPoint(
                        item.location.x - paddingBoxOrigin.x,
                        item.location.y - paddingBoxOrigin.y),
                    item.computedSize,
                    item.scrollableOverflow.rect(),
                    item.border,
                    item.overflow,
                    overflowContainer,
                    item.margin
                )
            );
        }

        return itemOverflowRect;
    }

    private void performFinalLayout(List<GridItem> items) {
        TaffyTree tree = layoutComputer.getTree();

        for (GridItem item : items) {
            FloatSize scrollbarSize = new FloatSize(
                item.overflow.y == Overflow.SCROLL ? item.scrollbarWidth : 0f,
                item.overflow.x == Overflow.SCROLL ? item.scrollbarWidth : 0f
            );

            // Canonical Layout: the child reports its own scrollable overflow rectangle (SO-01),
            // so the parent must carry it through instead of dropping it in a compatibility
            // constructor. The legacy contentSize component keeps its 1.x value (the item's
            // border-box size) until 2.0 removes it.
            Layout layout = new Layout(
                item.order,
                item.location,
                item.computedSize,
                item.computedSize,
                item.scrollableOverflow,
                scrollbarSize,
                item.border,
                item.padding,
                item.margin
            );

            tree.setUnroundedLayout(item.nodeId, layout);
        }
    }

    /**
     * Sizes, aligns and positions the absolutely positioned children, performing their final
     * layout and accumulating their scrollable overflow contributions.
     *
     * <p>Mirrors upstream's out-of-flow positioning pass ({@code compute/oof.rs}): an absolute box
     * contributes relative to the absolute positioning area's padding-box origin and extends the
     * region neither by the container's own padding (the container description carries zero
     * padding) nor by its own margin. In RTL the client area's inline end is part of the region
     * even when no candidate survives, so the accumulator starts at the client width.</p>
     *
     * @return the union of the absolutely positioned children's overflow contributions, in the
     *         container's padding-box frame
     */
    private FloatRect layoutAbsoluteChildren(
        NodeId node,
        FloatSize containerSize,
        FloatRect border,
        float scrollbarGutterX,
        float scrollbarGutterY,
        TrackCounts colCounts,
        TrackCounts rowCounts,
        GridAxisLines columnLines,
        GridAxisLines rowLines,
        boolean isRtl,
        NamedLineResolver namedLineResolver) {
        TaffyTree tree = layoutComputer.getTree();

        boolean isScrollContainer = ScrollableOverflowUtil.isScrollContainer(tree.getStyle(node).getOverflow());
        FloatSize absoluteClientSize = clientSize(containerSize, border, scrollbarGutterX, scrollbarGutterY);
        FloatPoint paddingBoxOrigin = new FloatPoint(border.left, border.top);
        // Upstream oof.rs passes an empty padding rectangle: end padding is the in-flow container's
        // own concern and must not be added a second time for out-of-flow boxes.
        ScrollableOverflowUtil.Container overflowContainer = new ScrollableOverflowUtil.Container(
            isScrollContainer, absoluteClientSize, isRtl, false, FloatRect.zero());
        FloatRect absoluteOverflowRect = isRtl
                                        ? new FloatRect(0f, absoluteClientSize.width, 0f, 0f)
                                        : FloatRect.zero();

        for (NodeId childId : tree.getChildren(node)) {
            TaffyStyle childStyle = tree.getStyle(childId);
            if (!childStyle.getPosition().isOutOfFlow()) continue;
            if (childStyle.getBoxGenerationMode() == BoxGenerationMode.NONE) continue;

            // Resolve grid placement for absolute positioned items.
            // Named lines/spans must be resolved to numeric lines before the absolute grid area is
            // computed, otherwise NAMED_LINE/NAMED_SPAN placements fall back to the container edges
            // and the absolutely positioned item fills the whole grid. The resolver is the same
            // instance that the in-flow path built before the grid size estimate (GR-15 / #1259).
            TaffyLine<GridPlacement> gridCol = childStyle.gridColumn;
            TaffyLine<GridPlacement> gridRow = childStyle.gridRow;

            // Resolve named lines/spans to numeric lines, preserving auto/line/span semantics
            if (hasNamedGridPlacement(gridCol)) {
                gridCol = namedLineResolver.resolveColumnNames(gridCol);
            }
            if (hasNamedGridPlacement(gridRow)) {
                gridRow = namedLineResolver.resolveRowNames(gridRow);
            }

            // Calculate grid area based on grid placement (physical container coordinates)
            FloatRect gridArea = resolveAbsoluteGridArea(
                gridCol, gridRow,
                colCounts, rowCounts,
                columnLines, rowLines,
                containerSize, border,
                scrollbarGutterX, scrollbarGutterY,
                isRtl
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

            // An `auto` size of an absolutely positioned *replaced* element resolves to its
            // intrinsic size instead of being stretched between its insets (upstream #1203).
            boolean isReplaced = childStyle.getItemIsReplaced();

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

            // Size from inset (when both left/right or top/bottom are set).
            // Per CSS spec, width from inset is applied first. A replaced element is not
            // stretched between its insets: its auto size resolves to its intrinsic size
            // instead (upstream #1203).
            if (!isReplaced && Float.isNaN(knownDimensions.width) && !Float.isNaN(left) && !Float.isNaN(right)) {
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
            if (!isReplaced && Float.isNaN(knownDimensions.height) && !Float.isNaN(top) && !Float.isNaN(bottom)) {
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

            // Resolve the item's self-alignment keywords for the out-of-flow path (upstream
            // #1206). An unset (AUTO) self value takes the container's justify-items/align-items
            // fallback, self-start/self-end resolve against the item's own direction, and an
            // explicit 'normal' behaves as start for an out-of-flow box. This is the OF-05 part
            // of the out-of-flow positioning model.
            TaffyDirection absposItemDirection = layoutComputer.resolveDirection(childId);
            TaffyDirection absposContainerDirection = isRtl ? TaffyDirection.RTL : TaffyDirection.LTR;
            TaffyStyle containerStyle = tree.getStyle(node);
            AlignItems justifySelf = childStyle.justifySelf;
            if (justifySelf == null || justifySelf == AlignItems.AUTO) {
                justifySelf = containerStyle.justifyItems;
            }
            if (justifySelf == AlignItems.AUTO) {
                justifySelf = null;
            }
            justifySelf = AlignmentHelper.resolveNormal(
                AlignmentHelper.resolveSelfRelative(justifySelf, absposItemDirection, absposContainerDirection, true),
                AlignItems.START);
            AlignItems alignSelf = childStyle.getAlignSelf();
            if (alignSelf == null || alignSelf == AlignItems.AUTO) {
                alignSelf = containerStyle.getAlignItems();
            }
            if (alignSelf == AlignItems.AUTO) {
                alignSelf = null;
            }
            alignSelf = AlignmentHelper.resolveNormal(
                AlignmentHelper.resolveSelfRelative(alignSelf, absposItemDirection, absposContainerDirection, false),
                AlignItems.START);

            // The grid area is already resolved to physical container coordinates (the logical start/end
            // lines were mirrored for RTL in resolveAbsoluteGridArea), so only the self-alignment keywords
            // need to be interpreted relative to the writing direction.
            float effectiveAreaWidth = gridArea.right - gridArea.left;
            TaffyPoint<Overflow> containerOverflow = containerStyle.getOverflow();

            // Resolve the item's position inside the grid area. The grid area is the containing
            // block of an absolutely positioned grid child, so its IMCB (and, with both insets
            // auto, its static-position rectangle) is the area itself. Upstream #1206 (a4dff69e).
            float xInArea = absoluteAxisOffset(
                effectiveAreaWidth, left, right, finalSize.width, margin.left, margin.right,
                justifySelf, AlignmentHelper.isSafe(justifySelf), isRtl,
                containerOverflow.x.isScrollContainer()
            );
            float yInArea = absoluteAxisOffset(
                areaHeight, top, bottom, finalSize.height, margin.top, margin.bottom,
                alignSelf, AlignmentHelper.isSafe(alignSelf), false,
                containerOverflow.y.isScrollContainer()
            );

            // Convert to container coordinates
            float x = gridArea.left + xInArea;
            float y = gridArea.top + yInArea;

            TaffyPoint<Overflow> overflow = childStyle.getOverflow();
            FloatSize scrollbarSize = new FloatSize(
                overflow.y == Overflow.SCROLL ? childStyle.getScrollbarWidth() : 0f,
                overflow.x == Overflow.SCROLL ? childStyle.getScrollbarWidth() : 0f
            );

            // Canonical Layout: the absolutely positioned child reports its own scrollable
            // overflow rectangle, which the container accumulates below (SO-01). The legacy
            // contentSize component keeps its 1.x value.
            Layout layout = new Layout(
                0,
                new FloatPoint(x, y),
                finalSize,
                output.contentSize(),
                output.scrollableOverflow(),
                scrollbarSize,
                itemBorder,
                itemPadding,
                margin
            );

            tree.setUnroundedLayout(childId, layout);

            // Out-of-flow contributions are measured from the absolute positioning area's
            // padding-box origin and never extend the region by the container's own padding
            // (upstream oof.rs).
            absoluteOverflowRect = ScrollableOverflowUtil.union(
                absoluteOverflowRect,
                ScrollableOverflowUtil.computeContribution(
                    new FloatPoint(x - paddingBoxOrigin.x, y - paddingBoxOrigin.y),
                    finalSize,
                    output.scrollableOverflow().rect(),
                    itemBorder,
                    overflow,
                    overflowContainer,
                    FloatRect.zero()
                )
            );
        }

        return absoluteOverflowRect;
    }

    /**
     * Whether a grid placement line contains a named line or named span that must be resolved via
     * {@link NamedLineResolver} before the placement can be interpreted. Plain auto/line/span
     * placements are left untouched and keep their existing semantics.
     */
    private static boolean hasNamedGridPlacement(TaffyLine<GridPlacement> placement) {
        if (placement == null) return false;
        GridPlacement start = placement.start;
        GridPlacement end = placement.end;
        return (start != null && (start.isNamedLine() || start.isNamedSpan()))
               || (end != null && (end.isNamedLine() || end.isNamedSpan()));
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
        GridAxisLines columnLines,
        GridAxisLines rowLines,
        FloatSize containerSize,
        FloatRect border,
        float scrollbarGutterX,
        float scrollbarGutterY,
        boolean isRtl) {

        // Resolve column placement
        Integer colStart = resolveAbsoluteGridLine(gridCol.start, gridCol.end, colCounts, true);
        Integer colEnd = resolveAbsoluteGridLine(gridCol.end, gridCol.start, colCounts, false);

        // Resolve row placement
        Integer rowStart = resolveAbsoluteGridLine(gridRow.start, gridRow.end, rowCounts, true);
        Integer rowEnd = resolveAbsoluteGridLine(gridRow.end, gridRow.start, rowCounts, false);

        // A grid line used as an edge resolves to the edge of the adjacent track rather than to the raw
        // gutter offset, so that content alignment free space is excluded from the grid area
        // (upstream #1071/#1266). In RTL the item's physical left edge derives from its logical end line
        // and its physical right edge from its logical start line (upstream's RTL branch).
        float left;
        float right;
        if (isRtl) {
            left = colEnd != null ? columnLines.endEdge(colEnd) : border.left + scrollbarGutterX;
            right = colStart != null ? columnLines.startEdge(colStart) : containerSize.width - border.right;
        } else {
            left = colStart != null ? columnLines.startEdge(colStart) : border.left;
            right = colEnd != null ? columnLines.endEdge(colEnd) : containerSize.width - border.right - scrollbarGutterX;
        }

        float top = rowStart != null ? rowLines.startEdge(rowStart) : border.top;
        float bottom = rowEnd != null ? rowLines.endEdge(rowEnd) : containerSize.height - border.bottom - scrollbarGutterY;

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
    /**
     * Distribute space to tracks with a fit-content() cap but no growth-limit cap, as upstream's
     * {@code fit_content_limited_growth_limit} does for the tracks this variant is used for.
     * Returns the remaining space that couldn't be distributed.
     */
    private float distributeSpaceToTracks(
        float spaceToDistribute,
        IntList targetTracks,
        FloatList sizes,
        Int2FloatMap fitContentLimits) {
        FloatList limits = new FloatArrayList(Math.max(1, sizes.size()));
        for (int i = 0; i < sizes.size(); i++) {
            limits.add(NaN);
        }
        for (var entry : fitContentLimits.int2FloatEntrySet()) {
            limits.set(entry.getIntKey(), entry.getFloatValue());
        }
        return distributeSpaceUpToLimits(spaceToDistribute, targetTracks, sizes, limits);
    }

    /**
     * Distribute space to tracks while respecting their growth limits.
     * This follows the CSS Grid algorithm where space is distributed only to tracks that haven't
     * reached their growth limit yet. Returns the remaining space that couldn't be distributed.
     */
    private float distributeSpaceWithGrowthLimit(
        float spaceToDistribute,
        IntList targetTracks,
        FloatList sizes,
        FloatList growthLimits) {
        return distributeSpaceUpToLimits(spaceToDistribute, targetTracks, sizes, growthLimits);
    }

    /**
     * The shared "distribute space up to limits" loop (CSS Grid 11.5.1; upstream
     * {@code distribute_space_up_to_limits}): space is spread evenly over all still-growable affected
     * tracks and never past a track's limit. A {@code NaN} limit means "no limit".
     *
     * <p>GR-04 / upstream #1001 ({@code 51dfca63}): the upstream version keeps a per-track
     * <em>item-incurred increase</em> separate from the affected property, and its initial version
     * forgot to subtract that increase when computing the per-iteration headroom, so a track could
     * grow past its limit (and the acceptance check accepted it because it also ignored the
     * increase). This port updates the affected size in place on every iteration, so the headroom
     * computed from the current size always reflects the increases already distributed for the
     * current item. The invariant therefore holds by construction; both call-site families
     * (growth-limit-limited and fit-content-limited) share this single implementation so the rule
     * only exists once.</p>
     *
     * @param limits per-track limit, indexed by absolute track index ({@code NaN} = no limit)
     * @return the space that could not be distributed
     */
    private float distributeSpaceUpToLimits(
        float spaceToDistribute,
        IntList targetTracks,
        FloatList sizes,
        FloatList limits) {

        float remaining = spaceToDistribute;

        while (remaining > 0.001f) {
            // Find tracks that can still grow (not at their limit). The current size already
            // includes every increase incurred so far, so this is the item-incurred-increase-aware
            // headroom test from upstream #1001.
            IntList growableTracks = new IntArrayList();
            for (int c : targetTracks) {
                float currentSize = Float.isNaN(sizes.getFloat(c)) ? 0f : sizes.getFloat(c);
                float limit = limits == null || Float.isNaN(limits.getFloat(c)) ? Float.MAX_VALUE : limits.getFloat(c);
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
                float currentSize = Float.isNaN(sizes.getFloat(c)) ? 0f : sizes.getFloat(c);
                float limit = limits == null || Float.isNaN(limits.getFloat(c)) ? Float.MAX_VALUE : limits.getFloat(c);

                // Clamp the per-track increase to the remaining headroom: never past the limit
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

    // === Out-of-flow self-alignment (OF-05, upstream #1206) ===

    /** The physical edge a resolved self-alignment selects in the absolutely positioned model. */
    private enum OofEdge {
        START, CENTER, END
    }

    /**
     * The physical edge selected by a self-alignment value. {@code reversed} is true when the
     * logical start edge of the containing block is the physical end edge (the inline axis of an
     * RTL containing block). An unset value behaves as start (upstream
     * {@code OofAlignment::from_style} derives normal from an absent alignment).
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

    /** Positions a box of {@code size} within {@code [start, end]} on the given physical edge. */
    private static float alignInLine(float start, float end, float size, OofEdge edge) {
        return switch (edge) {
            case START -> start;
            case CENTER -> (start + end - size) / 2f;
            case END -> end - size;
        };
    }

    /** Whether an unset self alignment (or an explicit {@code normal}) behaves as the default. */
    private static boolean isNormalAlignment(AlignItems alignment) {
        AlignItems keyword = AlignmentHelper.keyword(alignment);
        return keyword == null || keyword == AlignItems.NORMAL;
    }

    /**
     * Resolves the position of one axis of an absolutely positioned grid child within its grid
     * area (upstream {@code oof.rs} {@code OofAxis}, reduced to the direct-parent model).
     *
     * <p>The grid area is the containing block of an absolutely positioned grid child, so the
     * inset-modified containing block is resolved against the area and, when both insets are
     * {@code auto}, the static-position rectangle is the whole area. The returned offset is
     * relative to the area's start edge.</p>
     *
     * @param cbSize      the grid area's size in this axis
     * @param insetStart  the resolved start inset ({@code NaN} = auto)
     * @param insetEnd    the resolved end inset ({@code NaN} = auto)
     * @param size        the box's border-box size in this axis
     * @param marginStart the resolved start margin
     * @param marginEnd   the resolved end margin
     * @param alignment   the resolved self alignment of the axis ({@code null} = normal)
     * @param safe        whether the alignment carries the {@code safe} modifier
     * @param reversed    whether the logical start edge is the physical end edge (RTL inline)
     * @param scrollAxis  whether the containing block scrolls in this axis
     * @return the offset of the box's border-box start from the area's start edge
     */
    private static float absoluteAxisOffset(float cbSize, float insetStart, float insetEnd,
                                            float size, float marginStart, float marginEnd,
                                            AlignItems alignment, boolean safe, boolean reversed,
                                            boolean scrollAxis) {
        boolean hasStartInset = !Float.isNaN(insetStart);
        boolean hasEndInset = !Float.isNaN(insetEnd);
        float marginBox = size + marginStart + marginEnd;

        if (!hasStartInset && !hasEndInset) {
            // Both insets auto: static alignment within the grid area. Because the area is the
            // whole static-position rectangle, its IMCB is the area as well. A `safe` value
            // whose margin box overflows the area falls back to the logical start edge.
            OofEdge keyword = alignmentEdge(alignment, reversed);
            if (safe && marginBox > cbSize) {
                keyword = startEdge(reversed);
            }
            return alignInLine(0f, cbSize, marginBox, keyword) + marginStart;
        }

        float imcbStart;
        float imcbEnd;
        if (hasStartInset && hasEndInset) {
            imcbStart = insetStart;
            // Over-constrained insets: the weaker (end) inset is reduced so that the IMCB is
            // empty rather than negative (css-position-3 resolving-insets).
            imcbEnd = Math.max(insetStart, cbSize - insetEnd);
        } else if (hasStartInset) {
            imcbStart = insetStart;
            imcbEnd = cbSize;
        } else {
            imcbStart = 0f;
            imcbEnd = cbSize - insetEnd;
        }

        if (!hasStartInset || !hasEndInset) {
            // A single inset anchors the margin box to the stronger inset's edge.
            return hasStartInset
                   ? imcbStart + marginStart
                   : imcbEnd - size - marginEnd;
        }

        float imcbSize = imcbEnd - imcbStart;
        OofEdge edge = alignmentEdge(alignment, reversed);
        if (safe) {
            OofEdge effective = marginBox > imcbSize ? startEdge(reversed) : edge;
            return alignInLine(imcbStart, imcbEnd, marginBox, effective) + marginStart;
        }
        if (marginBox <= imcbSize || isNormalAlignment(alignment)) {
            // 'normal' is start-aligned (as specified) even when the box overflows.
            return alignInLine(imcbStart, imcbEnd, marginBox, edge) + marginStart;
        }
        // Default overflow alignment for absolutely positioned boxes: keep the box as close to the
        // requested alignment as possible without overflowing the overflow-limit rectangle.
        // https://www.w3.org/TR/css-align-3/#auto-safety-position
        float limitStart = Math.min(imcbStart, 0f);
        float limitEnd = Math.max(imcbEnd, cbSize);
        if (scrollAxis) {
            if (reversed) {
                limitStart = Float.NEGATIVE_INFINITY;
            } else {
                limitEnd = Float.POSITIVE_INFINITY;
            }
        }
        if (marginBox <= limitEnd - limitStart) {
            float start = alignInLine(imcbStart, imcbEnd, marginBox, edge);
            return Math.max(Math.min(start, limitEnd - marginBox), limitStart) + marginStart;
        }
        return alignInLine(limitStart, limitEnd, marginBox, startEdge(reversed)) + marginStart;
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
    /**
     * Compare two sizes exactly, treating {@code NaN} as equal to {@code NaN}. Used for cache keys,
     * where a value must be identical rather than approximately equal.
     */
    private static boolean floatSizeEquals(FloatSize a, FloatSize b) {
        if (a == null || b == null) return a == b;
        return floatEqualsExactly(a.width, b.width) && floatEqualsExactly(a.height, b.height);
    }

    private static boolean floatEqualsExactly(float a, float b) {
        if (Float.isNaN(a) || Float.isNaN(b)) return Float.isNaN(a) && Float.isNaN(b);
        return a == b;
    }

    private static boolean floatEquals(float a, float b) {
        if (Float.isNaN(a) && Float.isNaN(b)) return true;
        if (Float.isNaN(a) || Float.isNaN(b)) return false;
        return Math.abs(a - b) < 0.001f;
    }

    /**
     * Resolve an explicit {@code normal} for grid content alignment to the grid default
     * ({@code stretch}, upstream {@code grid_align_content} / #1255). The legacy
     * {@link AlignContent#AUTO} sentinel is left untouched: it means "not specified" and keeps the
     * pre-P5 grid default (auto tracks stretch, zero alignment offset).
     */
    private static AlignContent resolveGridContentAlignment(AlignContent alignment) {
        return AlignmentHelper.resolveNormal(alignment, AlignContent.STRETCH);
    }

    /** Resolves an explicit {@code normal} for grid content alignment to {@code stretch}. */
    private static JustifyContent resolveGridContentAlignment(JustifyContent alignment) {
        return AlignmentHelper.resolveNormal(alignment, JustifyContent.STRETCH);
    }

    /**
     * Calculate the initial offset for content alignment (align-content/justify-content).
     *
     * <p>The keyword and its safety modifier are resolved by the shared {@link AlignmentHelper}
     * (upstream {@code apply_alignment_fallback} + {@code compute_alignment_offset}): explicit
     * {@code normal} behaves as {@code stretch} for grid containers, the distribution keywords use
     * their single-subject fallbacks, and a {@code safe} keyword falls back to logical start when
     * the tracks overflow (ST-02 / #952, ST-04 / #1255). Legacy keyword geometry is unchanged.</p>
     */
    private float calculateContentAlignmentOffset(AlignContent alignment, float freeSpace, int numTracks) {
        if (alignment == null || numTracks <= 0) {
            // An axis without non-collapsed tracks has a single grid line; see
            // calculateEmptyAxisAlignmentOffset, which the absolute-positioning path uses.
            return 0;
        }
        AlignContent effective = AlignmentHelper.applyAlignmentFallback(
            resolveGridContentAlignment(alignment), freeSpace, numTracks);
        return AlignmentHelper.computeAlignmentOffset(freeSpace, numTracks, 0f, effective, false, true);
    }

    /**
     * Calculate the initial offset for justify-content. See
     * {@link #calculateContentAlignmentOffset(AlignContent, float, int)} for the resolution rules.
     */
    private float calculateContentAlignmentOffset(JustifyContent alignment, float freeSpace, int numTracks) {
        if (alignment == null || numTracks <= 0) {
            return 0;
        }
        JustifyContent effective = AlignmentHelper.applyAlignmentFallback(
            resolveGridContentAlignment(alignment), freeSpace, numTracks);
        return AlignmentHelper.computeAlignmentOffset(freeSpace, numTracks, 0f, effective, false, true);
    }

    /**
     * Calculate adjusted gap for space-between, space-around, space-evenly.
     *
     * <p>The inter-subject delta is computed by the shared
     * {@link AlignmentHelper#computeAlignmentOffset(float, int, float, AlignContent, boolean, boolean)},
     * after the keyword/safety fallbacks (explicit {@code normal} is {@code stretch}, and an
     * overflowing {@code safe} distribution keyword has already fallen back to its positional
     * keyword). The guard preserves the historical behaviour: only positive free space between two
     * or more tracks is distributed.</p>
     */
    private float calculateAdjustedGap(AlignContent alignment, float freeSpace, int numTracks, float originalGap) {
        if (alignment == null || freeSpace <= 0 || numTracks <= 1) {
            return originalGap;
        }
        AlignContent effective = AlignmentHelper.applyAlignmentFallback(
            resolveGridContentAlignment(alignment), freeSpace, numTracks);
        return AlignmentHelper.computeAlignmentOffset(freeSpace, numTracks, originalGap, effective, false, false);
    }

    /**
     * Calculate adjusted gap for justify-content. See
     * {@link #calculateAdjustedGap(AlignContent, float, int, float)}.
     */
    private float calculateAdjustedGap(JustifyContent alignment, float freeSpace, int numTracks, float originalGap) {
        if (alignment == null || freeSpace <= 0 || numTracks <= 1) {
            return originalGap;
        }
        JustifyContent effective = AlignmentHelper.applyAlignmentFallback(
            resolveGridContentAlignment(alignment), freeSpace, numTracks);
        return AlignmentHelper.computeAlignmentOffset(freeSpace, numTracks, originalGap, effective, false, false);
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
     * The definite value of a single track sizing function, mirroring upstream
     * {@code TrackSizingFunction::definite_value}: only a fixed length — or a percentage that
     * resolves against a definite parent size — has a definite value.
     */
    private static float trackDefiniteValue(TrackSizingFunction function, float parentSize) {
        if (function == null || !function.isFixed() || function.getFixedValue() == null) return NaN;
        return function.getFixedValue().maybeResolve(parentSize);
    }

    /**
     * The extent of an item's grid area along one axis, mirroring upstream #960
     * {@code GridItem::grid_area_size}: the area is definite only when every spanned track's min
     * and max track sizing functions have the same definite value. Gutters are not included in
     * this intrinsic-size estimate (upstream sums the spanned tracks' base sizes).
     */
    private static float definiteGridAreaExtent(FloatList trackSizes, List<TrackSizingFunction> tracks,
                                                int start, int span, float parentSize) {
        if (tracks == null || trackSizes == null || start < 0 || span < 1) return NaN;
        float total = 0f;
        for (int i = start; i < start + span; i++) {
            if (i >= tracks.size() || i >= trackSizes.size()) return NaN;
            TrackSizingFunction track = tracks.get(i);
            float min = trackDefiniteValue(trackMinFunction(track), parentSize);
            float max = trackDefiniteValue(trackMaxFunction(track), parentSize);
            if (Float.isNaN(min) || Float.isNaN(max) || !floatEqualsExactly(min, max)) return NaN;
            float base = trackSizes.getFloat(i);
            total += Float.isNaN(base) ? min : base;
        }
        return total;
    }

    /**
     * Build an item's grid area size: {@code inlineAxis} selects which dimension is the sized-axis
     * extent, the other dimension comes from the opposite axis' size estimate (upstream #960's
     * {@code get_track_size_estimate}). Either dimension may be {@code NaN} (indefinite).
     */
    private static FloatSize gridAreaSize(boolean inlineAxis, float sizedAxisExtent, float otherAxisExtent) {
        return inlineAxis
               ? new FloatSize(sizedAxisExtent, otherAxisExtent)
               : new FloatSize(otherAxisExtent, sizedAxisExtent);
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

        TrackCounts colCounts = gc.computeColumnCounts(items, expandedCols, explicitColumnCount);
        TrackCounts rowCounts = gc.computeRowCounts(items, expandedRows, explicitRowCount);

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
