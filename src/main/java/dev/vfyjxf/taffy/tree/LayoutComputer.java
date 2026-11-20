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
import dev.vfyjxf.taffy.style.Contain;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.util.BlockContext;
import dev.vfyjxf.taffy.util.MeasureContext;
import dev.vfyjxf.taffy.util.MeasureFunc;
import dev.vfyjxf.taffy.util.MeasureFuncAdapter;
import dev.vfyjxf.taffy.util.MeasureFunction;
import dev.vfyjxf.taffy.util.Resolve;
import dev.vfyjxf.taffy.util.ScrollableOverflowUtil;
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
     *
     * <p>The root's containing block is the <em>initial containing block</em> (ICB): a rectangle
     * the size of the viewport ({@code availableSpace}) anchored at the canvas origin (upstream
     * #1204/#1205). The root's own {@code position}/{@code inset}/{@code margin} styles resolve
     * against it instead of leaving the root pinned at {@code (0, 0)} (OF-07 / ST-06):</p>
     *
     * <ul>
     *   <li>An in-flow (or {@code position: relative}) root's margin box is placed at the ICB
     *       origin: {@code x = margin.left} ({@code availableWidth - size.width - margin.right}
     *       for a definite RTL viewport) and {@code y = margin.top}. A {@code relative} root then
     *       shifts by its insets, resolved against the ICB and in the block's inline direction:
     *       the physical {@code right} inset wins over {@code left} in RTL (ST-06 / upstream
     *       #906).</li>
     *   <li>A {@code position: absolute}/{@code fixed} root whose ICB is definite in both axes is
     *       sized and positioned by the out-of-flow rules (inset stretch, shrink-to-fit, auto
     *       margins) with the ICB as its containing block. An indefinite ICB leaves no rectangle
     *       to resolve against, so such a root is laid out in flow exactly like upstream.</li>
     *   <li>After the root itself, out-of-flow children whose containing block is the ICB (always
     *       {@code fixed}; {@code absolute} when the root is not positioned) are positioned
     *       against the viewport. The 1.2 line has no out-of-flow hoisting (OF-01), so this final
     *       pass covers the root's direct children only: a {@code fixed} box deeper in the tree is
     *       still positioned against its parent's positioning area (residual TS-05). The root's
     *       {@code contentSize}/{@code scrollableOverflow} are not recomputed, because the
     *       block/flex/grid algorithms fold the out-of-flow contributions in.</li>
     * </ul>
     */
    public void computeLayout(NodeId root, TaffySize<AvailableSpace> availableSpace) {
        TaffyStyle style = tree.getStyle(root);
        TaffyPosition position = style.getPosition();
        TaffyDirection direction = resolveDirection(root);

        // The ICB only exists when the viewport is definite in both axes.
        boolean icbIsDefinite = availableSpace.width.isDefinite() && availableSpace.height.isDefinite();
        if (position.isOutOfFlow() && icbIsDefinite) {
            computeOutOfFlowRootLayout(root, direction, availableSpace);
        } else {
            computeInFlowRootLayout(root, direction, availableSpace);
        }

        positionRootLevelOutOfFlowChildren(root, direction, availableSpace);
    }

    /**
     * Lays out a root that participates in the normal flow (or one whose ICB is indefinite) and
     * stores its layout.
     *
     * <p>This is the historical root path; the only placement change is that the root is placed
     * by its margin (and, for {@code position: relative}, by its insets) against the initial
     * containing block instead of being pinned at {@code (0, 0)} (OF-07 / ST-06, upstream
     * #1205/#906).</p>
     */
    private void computeInFlowRootLayout(NodeId root, TaffyDirection direction,
                                         TaffySize<AvailableSpace> availableSpace) {
        FloatSize knownDimensions = new FloatSize(Float.NaN, Float.NaN);

        TaffyStyle style = tree.getStyle(root);

        // A definite available space is the space available to a box's *border box*, and the
        // owner of that space subtracts the box's margins before handing it over (upstream #1234:
        // LayoutInput.available_space's contract). A node never subtracts its own margins again.
        // The root has no parent, so it subtracts its own margins here, for every display type
        // (upstream compute_in_flow_root_layout; block roots previously did it only for the
        // stretched width, and flex/grid roots not at all). Auto margins contribute zero.
        FloatSize parentSizeOptions = availableSpaceToOptionSize(availableSpace);
        FloatRect rootMargin = Resolve.resolveRectLpaOrZero(style.getMargin(), parentSizeOptions.width);
        TaffySize<AvailableSpace> rootAvailableSpace = new TaffySize<>(
            availableSpace.width.maybeSub(rootMargin.left + rootMargin.right).maybeMax(0f),
            availableSpace.height.maybeSub(rootMargin.top + rootMargin.bottom).maybeMax(0f)
        );

        // For block-level nodes (including flow-root, which uses the block layout algorithm),
        // compute known dimensions based on style and available space
        if (isBlockLayout(style.getDisplay())) {
            FloatSize parentSize = parentSizeOptions;

            Float aspectRatio = style.getAspectRatio();
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

            // Block nodes stretch width to available space (already net of the root's margins)
            float availableWidth = rootAvailableSpace.width.isDefinite()
                                   ? rootAvailableSpace.width.getValue()
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

        // Compute layout recursively. The root's own layout algorithm receives the available
        // space net of the root's margins (upstream #1234), while parent_size keeps the raw
        // viewport for percentage resolution, exactly like upstream.
        LayoutOutput output = performChildLayout(
            root,
            knownDimensions,
            parentSizeOptions,
            rootAvailableSpace,
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

        // Set the root layout. The root's margin box is placed at the origin of the initial
        // containing block: x = margin.left (or, for a definite RTL viewport, aligned to the right
        // edge: availableWidth - size.width - margin.right), y = margin.top. Child locations stay
        // parent-relative and are not affected by this. OF-07 / upstream #1205; the RTL right-edge
        // formula follows ST-06 / upstream #906.
        float rootX;
        if (direction.isRtl() && availableSpace.width.isDefinite()) {
            rootX = availableSpace.width.getValue() - output.size().width - margin.right;
        } else {
            rootX = margin.left;
        }
        float rootY = margin.top;

        // A relatively positioned root is offset by its insets, resolved against the ICB. The
        // physical inset consulted first is the block's inline-start side: right in RTL, left in
        // LTR (ST-06 / upstream #906). Static and sticky roots are not offset by their insets.
        if (style.getPosition() == TaffyPosition.RELATIVE) {
            float icbWidth = availableSpace.width.isDefinite() ? availableSpace.width.getValue() : Float.NaN;
            float icbHeight = availableSpace.height.isDefinite() ? availableSpace.height.getValue() : Float.NaN;
            TaffyRect<LengthPercentageAuto> inset = style.getInset();
            float left = inset.left.maybeResolve(icbWidth);
            float right = inset.right.maybeResolve(icbWidth);
            float top = inset.top.maybeResolve(icbHeight);
            float bottom = inset.bottom.maybeResolve(icbHeight);
            if (direction.isRtl()) {
                rootX += Float.isNaN(right) ? (Float.isNaN(left) ? 0f : left) : -right;
            } else {
                rootX += Float.isNaN(left) ? (Float.isNaN(right) ? 0f : -right) : left;
            }
            rootY += Float.isNaN(top) ? (Float.isNaN(bottom) ? 0f : -bottom) : top;
        }

        Layout layout = new Layout(
            0,
            new FloatPoint(rootX, rootY),
            output.size(),
            output.contentSize(),
            output.scrollableOverflow(),
            scrollbarSize,
            border,
            padding,
            margin
        );

        tree.setUnroundedLayout(root, layout);
    }

    /**
     * Lays out a {@code position: absolute}/{@code fixed} root against the initial containing
     * block and stores its layout.
     *
     * <p>Mirrors upstream {@code layout_oof_box} (#1205): the root is sized from its style size
     * and insets (a pair of insets stretches an {@code auto} axis; otherwise the box may shrink to
     * fit its content), auto margins absorb the free space of a fully constrained axis, and the
     * static position places the root's margin box at the ICB origin (at the right edge in an RTL
     * viewport).</p>
     */
    private void computeOutOfFlowRootLayout(NodeId root, TaffyDirection direction,
                                            TaffySize<AvailableSpace> availableSpace) {
        TaffyStyle style = tree.getStyle(root);
        float areaWidth = availableSpace.width.getValue();
        float areaHeight = availableSpace.height.getValue();
        FloatSize areaSize = new FloatSize(areaWidth, areaHeight);
        OutOfFlowSizing sizing = resolveOutOfFlowSizing(style, areaWidth, areaHeight);
        FloatSize knownDimensions = sizing.knownDimensions();

        // The available space handed to the root's layout algorithm is the ICB, clamped exactly
        // like every other out-of-flow box (upstream layout_oof_box).
        TaffySize<AvailableSpace> definiteArea = outOfFlowAvailableSpace(sizing, areaWidth, areaHeight);

        // An axis with neither a style size nor a pair of insets is sized by the root's own layout
        // algorithm: measure it first, then lay the subtree out again with the definite final size
        // (PA-05's two-pass pattern, which is how this port models upstream's definite pass).
        LayoutOutput measuredOutput = performChildLayout(root, knownDimensions, areaSize, definiteArea,
            SizingMode.INHERENT_SIZE, TaffyLine.FALSE);
        FloatSize finalSize = clampOutOfFlowSize(new FloatSize(
            Float.isNaN(knownDimensions.width) ? measuredOutput.size().width : knownDimensions.width,
            Float.isNaN(knownDimensions.height) ? measuredOutput.size().height : knownDimensions.height
        ), sizing);
        LayoutOutput output = performDefiniteAbsoluteChildLayout(
            root, knownDimensions, finalSize, areaSize, definiteArea, SizingMode.INHERENT_SIZE, measuredOutput);

        FloatRect resolvedMargin = resolveOutOfFlowMargins(sizing.margin(),
            sizing.left(), sizing.right(), sizing.top(), sizing.bottom(), areaWidth, areaHeight, finalSize);
        // The root's static position places its margin box at the ICB origin: at the left edge in
        // LTR and at the right edge in RTL (upstream compute_root_layout).
        float staticX = direction.isRtl()
                        ? areaWidth - finalSize.width - resolvedMargin.right
                        : resolvedMargin.left;
        FloatPoint offset = resolveOutOfFlowOffset(direction, areaWidth, areaHeight, finalSize,
            sizing.left(), sizing.right(), sizing.top(), sizing.bottom(), resolvedMargin,
            staticX, resolvedMargin.top);

        FloatSize scrollbarSize = new FloatSize(
            style.getOverflow().y == Overflow.SCROLL ? style.getScrollbarWidth() : 0f,
            style.getOverflow().x == Overflow.SCROLL ? style.getScrollbarWidth() : 0f
        );
        Layout layout = new Layout(
            0,
            offset,
            finalSize,
            output.contentSize(),
            output.scrollableOverflow(),
            scrollbarSize,
            sizing.border(),
            sizing.padding(),
            resolvedMargin
        );
        tree.setUnroundedLayout(root, layout);
    }

    /**
     * Positions the root's direct out-of-flow children against the initial containing block.
     *
     * <p>CSS positions a {@code position: fixed} box against the viewport and a
     * {@code position: absolute} box against the initial containing block when no ancestor is
     * positioned. The 1.2 line has no out-of-flow hoisting (OF-01), so an out-of-flow child is laid
     * out by the algorithm of its immediate parent; this pass redoes the final positioning of the
     * root's direct out-of-flow children against the ICB, which is the observable part of the
     * upstream root positioning pass (#1204/#1205) without hoisting.</p>
     *
     * <p>The positioning area follows upstream #1204: an axis with a definite viewport size uses
     * the viewport (less the root's scrollbar gutter) and is offset by {@code -root.location}; an
     * indefinite axis falls back to the root's padding box. The root's own layout, including its
     * content size and scrollable overflow, is left untouched.</p>
     */
    private void positionRootLevelOutOfFlowChildren(NodeId root, TaffyDirection direction,
                                                    TaffySize<AvailableSpace> availableSpace) {
        TaffyStyle rootStyle = tree.getStyle(root);
        // A hidden root lays out no boxes, and an indefinite viewport has no ICB in either axis.
        if (rootStyle.getBoxGenerationMode() == BoxGenerationMode.NONE) return;
        if (tree.childCount(root) == 0) return;
        if (!availableSpace.width.isDefinite() && !availableSpace.height.isDefinite()) return;

        Layout rootLayout = tree.getUnroundedLayout(root);
        if (rootLayout == null) return;

        boolean rootIsPositioned = rootStyle.getPosition() != TaffyPosition.STATIC;
        FloatSize scrollbar = rootLayout.scrollbarSize();
        FloatRect border = rootLayout.border();

        // The padding box is the fallback positioning area of an indefinite axis.
        float rootPaddingBoxWidth = rootLayout.size().width - border.left - border.right - scrollbar.width;
        float rootPaddingBoxHeight = rootLayout.size().height - border.top - border.bottom - scrollbar.height;

        boolean definiteWidth = availableSpace.width.isDefinite();
        boolean definiteHeight = availableSpace.height.isDefinite();
        float areaWidth = definiteWidth
                          ? Math.max(0f, availableSpace.width.getValue() - scrollbar.width)
                          : rootPaddingBoxWidth;
        float areaHeight = definiteHeight
                           ? Math.max(0f, availableSpace.height.getValue() - scrollbar.height)
                           : rootPaddingBoxHeight;
        float areaX = definiteWidth ? -rootLayout.location().x : border.left;
        float areaY = definiteHeight ? -rootLayout.location().y : border.top;

        for (NodeId child : tree.getChildren(root)) {
            TaffyStyle childStyle = tree.getStyle(child);
            TaffyPosition childPosition = childStyle.getPosition();
            if (!childPosition.isOutOfFlow()) continue;
            if (childStyle.getBoxGenerationMode() == BoxGenerationMode.NONE) continue;
            // `fixed` boxes are never claimed by an ancestor's positioning pass; `absolute` boxes
            // are claimed by the root itself unless the root is statically positioned.
            boolean usesInitialContainingBlock =
                childPosition == TaffyPosition.FIXED
                || (childPosition == TaffyPosition.ABSOLUTE && !rootIsPositioned);
            if (!usesInitialContainingBlock) continue;
            positionAgainstInitialContainingBlock(child, direction, areaWidth, areaHeight, areaX, areaY);
        }
    }

    /**
     * Re-positions one out-of-flow child of the root against the initial containing block.
     *
     * <p>Sizing follows {@code layout_oof_box}: style size, min/max, aspect ratio and box sizing
     * first, then an {@code auto} axis stretched by a pair of insets. A style-defined axis that the
     * ICB resolves differently from the parent's pass re-lays the subtree out (the cache is cleared
     * first, because a cache hit returns the output without storing a new child layout), including
     * PA-05's definite second pass for percentage sizes in the subtree. An axis that only the
     * content can size keeps the parent's measured size: re-measuring it against the wider
     * viewport would need the full hoisting model (residual TS-05).</p>
     */
    private void positionAgainstInitialContainingBlock(NodeId child, TaffyDirection direction,
                                                       float areaWidth, float areaHeight,
                                                       float areaX, float areaY) {
        TaffyStyle style = tree.getStyle(child);
        Layout existing = tree.getUnroundedLayout(child);
        if (existing == null) return;

        FloatSize areaSize = new FloatSize(areaWidth, areaHeight);
        OutOfFlowSizing sizing = resolveOutOfFlowSizing(style, areaWidth, areaHeight);
        FloatSize knownDimensions = sizing.knownDimensions();
        TaffySize<AvailableSpace> availableSpace = outOfFlowAvailableSpace(sizing, areaWidth, areaHeight);

        // Re-lay the subtree out only when the ICB resolves one of its style-defined axes to a
        // different size than the parent's pass did (e.g. a percentage size or an inset-stretched
        // auto axis). An axis that only the content can size keeps the parent's measurement:
        // re-measuring it against the wider viewport needs the full hoisting model (residual TS-05).
        boolean widthIsDefinite = !Float.isNaN(knownDimensions.width);
        boolean heightIsDefinite = !Float.isNaN(knownDimensions.height);
        FloatSize expected = clampOutOfFlowSize(knownDimensions, sizing);
        boolean widthDiffers = widthIsDefinite && expected.width != existing.size().width;
        boolean heightDiffers = heightIsDefinite && expected.height != existing.size().height;

        FloatSize finalSize = existing.size();
        LayoutOutput relaidOutput = null;
        if (widthDiffers || heightDiffers) {
            // The parent's cache entry was computed against the root's padding box; it may return
            // an output whose stored child layout no longer matches, so clear the subtree first.
            clearCacheRecursively(child);
            LayoutOutput measured = performChildLayout(child, knownDimensions, areaSize, availableSpace,
                SizingMode.INHERENT_SIZE, TaffyLine.FALSE);
            FloatSize measuredSize = new FloatSize(
                widthIsDefinite ? knownDimensions.width : measured.size().width,
                heightIsDefinite ? knownDimensions.height : measured.size().height
            );
            finalSize = clampOutOfFlowSize(measuredSize, sizing);
            // PA-05's definite second pass: percentage sizes in the subtree resolve against the
            // final size instead of the indefinite measurement pass.
            relaidOutput = performDefiniteAbsoluteChildLayout(child, knownDimensions, finalSize, areaSize,
                availableSpace, SizingMode.INHERENT_SIZE, measured);
            finalSize = relaidOutput.size();
        }

        FloatRect resolvedMargin = resolveOutOfFlowMargins(sizing.margin(),
            sizing.left(), sizing.right(), sizing.top(), sizing.bottom(), areaWidth, areaHeight, finalSize);
        // A static-position axis keeps where the parent's pass put the box (that position is
        // already relative to the root); its old margin is swapped for the ICB-resolved one.
        float staticX = existing.location().x - existing.margin().left + resolvedMargin.left;
        float staticY = existing.location().y - existing.margin().top + resolvedMargin.top;
        FloatPoint offset = resolveOutOfFlowOffset(direction, areaWidth, areaHeight, finalSize,
            sizing.left(), sizing.right(), sizing.top(), sizing.bottom(), resolvedMargin, staticX, staticY);
        float x = (Float.isNaN(sizing.left()) && Float.isNaN(sizing.right())) ? offset.x : offset.x + areaX;
        float y = (Float.isNaN(sizing.top()) && Float.isNaN(sizing.bottom())) ? offset.y : offset.y + areaY;

        FloatSize contentSize = relaidOutput != null ? relaidOutput.contentSize() : existing.contentSize();
        ScrollableOverflow scrollableOverflow = relaidOutput != null
                                                ? relaidOutput.scrollableOverflow()
                                                : existing.scrollableOverflow();
        FloatSize scrollbarSize = new FloatSize(
            style.getOverflow().y == Overflow.SCROLL ? style.getScrollbarWidth() : 0f,
            style.getOverflow().x == Overflow.SCROLL ? style.getScrollbarWidth() : 0f
        );
        Layout layout = new Layout(
            existing.order(),
            new FloatPoint(x, y),
            finalSize,
            contentSize,
            scrollableOverflow,
            scrollbarSize,
            sizing.border(),
            sizing.padding(),
            resolvedMargin
        );
        tree.setUnroundedLayout(child, layout);
    }

    /**
     * Resolved style inputs shared by the root out-of-flow path and the root-level ICB
     * positioning pass.
     *
     * @param margin            resolved margins; {@code auto} stays NaN until the free space is known
     * @param padding           resolved padding
     * @param border            resolved border
     * @param paddingBorderSize the padding + border sum, the minimum size of the box
     * @param minSize           resolved min size, at least {@code paddingBorderSize}
     * @param maxSize           resolved max size
     * @param knownDimensions   the size derived from the style size and insets; NaN in an axis the
     *                          box's own layout algorithm has to measure
     * @param left              resolved left inset (NaN for {@code auto})
     * @param right             resolved right inset (NaN for {@code auto})
     * @param top               resolved top inset (NaN for {@code auto})
     * @param bottom            resolved bottom inset (NaN for {@code auto})
     */
    private record OutOfFlowSizing(
        FloatRect margin,
        FloatRect padding,
        FloatRect border,
        FloatSize paddingBorderSize,
        FloatSize minSize,
        FloatSize maxSize,
        FloatSize knownDimensions,
        float left,
        float right,
        float top,
        float bottom
    ) {}

    /**
     * Resolves the style inputs of an out-of-flow box against its positioning area (upstream
     * {@code layout_oof_box}): sizes resolve against the area, a pair of insets stretches an
     * {@code auto} axis, and min/max are applied with padding + border as the lower bound.
     */
    private OutOfFlowSizing resolveOutOfFlowSizing(TaffyStyle style, float areaWidth, float areaHeight) {
        FloatSize areaSize = new FloatSize(areaWidth, areaHeight);

        // Margins resolve against the positioning area width and keep `auto` as NaN until the
        // free space is known.
        FloatRect margin = Resolve.maybeResolveRectLpa(style.getMargin(), areaWidth);
        FloatRect padding = Resolve.resolveRectOrZero(style.getPadding(), areaWidth);
        FloatRect border = Resolve.resolveRectOrZero(style.getBorder(), areaWidth);
        FloatSize paddingBorderSize = new FloatSize(
            padding.left + padding.right + border.left + border.right,
            padding.top + padding.bottom + border.top + border.bottom
        );
        FloatSize boxSizingAdjustment = style.getBoxSizing() == BoxSizing.CONTENT_BOX
                                        ? paddingBorderSize
                                        : new FloatSize(0f, 0f);

        float aspectRatio = style.getAspectRatio();
        FloatSize styleSize = Resolve.maybeApplyAspectRatio(
            Resolve.maybeResolveSize(style.getSize(), areaSize), aspectRatio);
        styleSize = maybeAdd(styleSize, boxSizingAdjustment);
        FloatSize minSize = Resolve.maybeApplyAspectRatio(
            Resolve.maybeResolveSize(style.getMinSize(), areaSize), aspectRatio);
        // A box is never smaller than its padding + border (upstream's or/maybe_max pair).
        minSize = maybeMax(paddingBorderSize, maybeAdd(minSize, boxSizingAdjustment));
        FloatSize maxSize = Resolve.maybeApplyAspectRatio(
            Resolve.maybeResolveSize(style.getMaxSize(), areaSize), aspectRatio);
        maxSize = maybeAdd(maxSize, boxSizingAdjustment);
        FloatSize knownDimensions = maybeClamp(styleSize, minSize, maxSize);

        TaffyRect<LengthPercentageAuto> inset = style.getInset();
        float left = inset.left.maybeResolve(areaWidth);
        float right = inset.right.maybeResolve(areaWidth);
        float top = inset.top.maybeResolve(areaHeight);
        float bottom = inset.bottom.maybeResolve(areaHeight);

        // An absolutely positioned replaced element's `auto` size is not stretched between its
        // insets (upstream #1203).
        if (!style.getItemIsReplaced()) {
            if (Float.isNaN(knownDimensions.width) && !Float.isNaN(left) && !Float.isNaN(right)) {
                float fillWidth = TaffyMath.maybeSub(TaffyMath.maybeSub(areaWidth, margin.left), margin.right)
                                  - left - right;
                knownDimensions = maybeClamp(Resolve.maybeApplyAspectRatio(
                    new FloatSize(Math.max(fillWidth, 0f), knownDimensions.height), aspectRatio), minSize, maxSize);
            }
            if (Float.isNaN(knownDimensions.height) && !Float.isNaN(top) && !Float.isNaN(bottom)) {
                float fillHeight = TaffyMath.maybeSub(TaffyMath.maybeSub(areaHeight, margin.top), margin.bottom)
                                   - top - bottom;
                knownDimensions = maybeClamp(Resolve.maybeApplyAspectRatio(
                    new FloatSize(knownDimensions.width, Math.max(fillHeight, 0f)), aspectRatio), minSize, maxSize);
            }
        }

        return new OutOfFlowSizing(margin, padding, border, paddingBorderSize, minSize, maxSize,
            knownDimensions, left, right, top, bottom);
    }

    /**
     * The available space handed to an out-of-flow box's layout algorithm: its positioning area,
     * clamped by the box's own min/max exactly like upstream {@code layout_oof_box}.
     */
    private static TaffySize<AvailableSpace> outOfFlowAvailableSpace(OutOfFlowSizing sizing,
                                                                     float areaWidth, float areaHeight) {
        return new TaffySize<>(
            AvailableSpace.definite(TaffyMath.clamp(areaWidth, sizing.minSize().width, sizing.maxSize().width)),
            AvailableSpace.definite(TaffyMath.clamp(areaHeight, sizing.minSize().height, sizing.maxSize().height))
        );
    }

    /**
     * Clamps a used size by the box's min/max and raises it to its padding + border size.
     */
    private static FloatSize clampOutOfFlowSize(FloatSize size, OutOfFlowSizing sizing) {
        FloatSize clamped = new FloatSize(
            TaffyMath.maybeClamp(size.width, sizing.minSize().width, sizing.maxSize().width),
            TaffyMath.maybeClamp(size.height, sizing.minSize().height, sizing.maxSize().height)
        );
        return new FloatSize(
            Math.max(clamped.width, sizing.paddingBorderSize().width),
            Math.max(clamped.height, sizing.paddingBorderSize().height)
        );
    }

    /**
     * Resolves the margins of an out-of-flow box whose positioning area is known.
     *
     * <p>An {@code auto} margin only absorbs free space in an axis where both insets are set; in
     * the other axis it resolves to zero (upstream #1202). The free space is measured against the
     * positioning area, and only a margin whose own inset is set counts, exactly like upstream
     * {@code layout_oof_box}.</p>
     */
    private static FloatRect resolveOutOfFlowMargins(FloatRect margin, float left, float right,
                                                     float top, float bottom, float areaWidth,
                                                     float areaHeight, FloatSize finalSize) {
        float nonAutoX = (Float.isNaN(left) ? 0f : nanToZero(margin.left))
                         + (Float.isNaN(right) ? 0f : nanToZero(margin.right));
        float nonAutoY = (Float.isNaN(top) ? 0f : nanToZero(margin.top))
                         + (Float.isNaN(bottom) ? 0f : nanToZero(margin.bottom));
        float freeSpaceX = !Float.isNaN(left) && !Float.isNaN(right)
                           ? areaWidth - left - right - finalSize.width - nonAutoX
                           : 0f;
        float freeSpaceY = !Float.isNaN(top) && !Float.isNaN(bottom)
                           ? areaHeight - top - bottom - finalSize.height - nonAutoY
                           : 0f;
        int autoCountX = (Float.isNaN(margin.left) ? 1 : 0) + (Float.isNaN(margin.right) ? 1 : 0);
        int autoCountY = (Float.isNaN(margin.top) ? 1 : 0) + (Float.isNaN(margin.bottom) ? 1 : 0);
        float autoX = autoMarginSize(freeSpaceX, autoCountX);
        float autoY = autoMarginSize(freeSpaceY, autoCountY);
        return new FloatRect(
            Float.isNaN(margin.left) ? autoX : margin.left,
            Float.isNaN(margin.right) ? autoX : margin.right,
            Float.isNaN(margin.top) ? autoY : margin.top,
            Float.isNaN(margin.bottom) ? autoY : margin.bottom
        );
    }

    /**
     * The share of the free space an {@code auto} margin absorbs in one axis (upstream
     * {@code layout_oof_box}): two auto margins split it, but resolve to zero when the box fills
     * the axis (a negative share is allowed for a single auto margin).
     */
    private static float autoMarginSize(float freeSpace, int autoMarginCount) {
        if (autoMarginCount == 0) return 0f;
        if (autoMarginCount == 2 && freeSpace <= 0f) return 0f;
        return freeSpace / autoMarginCount;
    }

    private static float nanToZero(float value) {
        return Float.isNaN(value) ? 0f : value;
    }

    /**
     * Resolves the location of an out-of-flow box inside its positioning area, in the area's own
     * coordinate space (the caller adds the area offset).
     *
     * <p>When both inline insets are set the block's {@code direction} decides which one wins in
     * an over-constrained axis ({@code right} in RTL, {@code left} in LTR); a single inset
     * positions the box on that side; with no inset the box stays at {@code staticX}. In the block
     * axis {@code top} always wins over {@code bottom} (upstream {@code layout_oof_box}, #906).</p>
     */
    private static FloatPoint resolveOutOfFlowOffset(TaffyDirection direction, float areaWidth,
                                                     float areaHeight, FloatSize finalSize,
                                                     float left, float right, float top, float bottom,
                                                     FloatRect resolvedMargin, float staticX,
                                                     float staticY) {
        float x;
        if (!Float.isNaN(left) && !Float.isNaN(right)) {
            x = direction.isRtl()
                ? areaWidth - finalSize.width - right - resolvedMargin.right
                : left + resolvedMargin.left;
        } else if (!Float.isNaN(left)) {
            x = left + resolvedMargin.left;
        } else if (!Float.isNaN(right)) {
            x = areaWidth - finalSize.width - right - resolvedMargin.right;
        } else {
            x = staticX;
        }
        float y;
        if (!Float.isNaN(top)) {
            y = top + resolvedMargin.top;
        } else if (!Float.isNaN(bottom)) {
            y = areaHeight - finalSize.height - bottom - resolvedMargin.bottom;
        } else {
            y = staticY;
        }
        return new FloatPoint(x, y);
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
     * Performs layout for a child node with an inherited block formatting context.
     *
     * <p>This overload is used by {@code BlockComputer} for children that participate in the
     * parent's block formatting context: their floats are placed into the shared
     * {@link BlockContext}, so a nested ordinary block can affect (and be affected by) the
     * ancestor's floats. A {@code null} context means "fresh BFC". The result is not stored in
     * the layout cache when the child is block-laid-out, because the output depends on the
     * mutable float state of the inherited context rather than on {@link LayoutInput} alone.</p>
     *
     * @param blockContext the inherited BFC context, or {@code null} for a fresh BFC
     */
    public LayoutOutput performChildLayout(
        NodeId node,
        FloatSize knownDimensions,
        FloatSize parentSize,
        TaffySize<AvailableSpace> availableSpace,
        SizingMode sizingMode,
        TaffyLine<Boolean> verticalMarginsAreCollapsible,
        RunMode runMode,
        BlockContext blockContext) {

        LayoutInput inputs = new LayoutInput(
            runMode,
            sizingMode,
            RequestedAxis.BOTH,
            knownDimensions,
            parentSize,
            availableSpace,
            verticalMarginsAreCollapsible
        );

        return computeChildLayout(node, inputs, blockContext);
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
        return computeChildLayoutCached(node, inputs);
    }

    private LayoutOutput computeChildLayoutCached(NodeId node, LayoutInput inputs) {
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
     * Computes layout for a child with an inherited block formatting context.
     *
     * <p>Block layout with a non-{@code null} context is context-dependent: the same
     * {@link LayoutInput} can produce different floats depending on the ancestor BFC's current
     * segments, so such results bypass the layout cache. Other display types establish their own
     * formatting context and are handled by the ordinary cached path.</p>
     */
    public LayoutOutput computeChildLayout(NodeId node, LayoutInput inputs, BlockContext blockContext) {
        if (blockContext == null) {
            return computeChildLayoutCached(node, inputs);
        }

        // Handle hidden layout
        if (inputs.runMode() == RunMode.PERFORM_HIDDEN_LAYOUT) {
            return computeHiddenLayout(node);
        }

        TaffyStyle style = tree.getStyle(node);
        if (isBlockLayout(style.getDisplay())) {
            boolean hadFloats = blockContext.hasFloats();
            LayoutOutput output = computeLayoutUncached(node, inputs, blockContext);
            if (hadFloats || blockContext.hasFloats()) {
                // The output depends on the inherited float state, so it must not be returned by
                // an ordinary cache lookup. Still store it under a non-matching key: TaffyTree
                // uses "cache is empty" as its dirty sentinel, and leaving the cache empty would
                // stop a later style change from marking ancestors dirty.
                LayoutInput staleMarker = new LayoutInput(
                    RunMode.PERFORM_LAYOUT,
                    inputs.sizingMode(),
                    inputs.axis(),
                    inputs.knownDimensions(),
                    new FloatSize(-Float.MAX_VALUE, -Float.MAX_VALUE),
                    inputs.availableSpace(),
                    inputs.verticalMarginsAreCollapsible()
                );
                tree.storeCacheEntry(node, staleMarker, output);
            } else {
                tree.storeCacheEntry(node, inputs, output);
            }
            return output;
        }

        return computeChildLayoutCached(node, inputs);
    }

    /**
     * Computes layout without using cache.
     */
    private LayoutOutput computeLayoutUncached(NodeId node, LayoutInput inputs) {
        return computeLayoutUncached(node, inputs, null);
    }

    private LayoutOutput computeLayoutUncached(NodeId node, LayoutInput inputs, BlockContext blockContext) {
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
            // Flow-root lays its children out with the block algorithm too; its difference from
            // BLOCK is that it establishes a new block formatting context (handled by
            // BlockComputer). Upstream #997 (BK-17).
            case BLOCK, FLOW_ROOT -> computeBlockLayout(node, inputs, style, blockContext);
            case FLEX -> computeFlexboxLayout(node, inputs, style);
            case GRID -> computeGridLayout(node, inputs, style);
            default -> computeLeafLayout(node, inputs, style);
        };
    }

    /**
     * Returns whether {@code display} lays its children out with the block layout algorithm.
     *
     * <p>{@link TaffyDisplay#FLOW_ROOT} uses the block layout algorithm and differs from
     * {@link TaffyDisplay#BLOCK} only in establishing a new block formatting context (upstream
     * #997, BK-17). This helper marks every "is this a block-layout box?" decision site in the
     * layout entry point; it is deliberately <em>not</em> {@link TaffyStyle#isBlock()}, which
     * upstream documents as {@code false} for flow-root because it instead answers "does this box
     * participate in its parent's block formatting context?". The one remaining {@code isBlock()}
     * use below (the leaf collapse-through test) wants exactly that second meaning.</p>
     */
    private static boolean isBlockLayout(TaffyDisplay display) {
        return display == TaffyDisplay.BLOCK || display == TaffyDisplay.FLOW_ROOT;
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

            // min-size does NOT apply the aspect ratio either: only the used size of one axis
            // transfers to the other, and a transferred size is derived from the other axis
            // *after* that axis has been clamped by its own min/max (css-sizing-4). A transferred
            // min/max constraint would let e.g. `max-height: 25px` cap an auto inline size of a
            // 300px-tall box with ratio 4 at 25px instead of 100px.
            minSize = maybeAdd(resolvedMinSizeRaw, boxSizingAdjustment);

            // max-size does NOT apply aspect-ratio in leaf layout (per Taffy behavior)
            maxSize = maybeAdd(resolvedMaxSizeRaw, boxSizingAdjustment);

            FloatSize size = Resolve.maybeResolveSize(style.getSize(), parentSize);
            FloatSize styleSize = maybeAdd(size, boxSizingAdjustment);
            // Clamp the specified axes first, then transfer the remaining definite axis through
            // the ratio: `height: 300px; max-height: 25px; aspect-ratio: 4` is 25px tall and
            // therefore 100px wide, not 1200px.
            FloatSize clampedStyleSize = maybeClamp(styleSize, minSize, maxSize);
            if (!Float.isNaN(aspectRatio)) {
                FloatSize contentBox = new FloatSize(
                    clampedStyleSize.width - boxSizingAdjustment.width,
                    clampedStyleSize.height - boxSizingAdjustment.height
                );
                FloatSize transferred = Resolve.maybeApplyAspectRatio(contentBox, aspectRatio);
                clampedStyleSize = maybeClamp(maybeAdd(transferred, boxSizingAdjustment), minSize, maxSize);
            }

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

        // Check if styles prevent being collapsed through (for margin collapse).
        //
        // Layout and paint containment both make the box establish an independent formatting
        // context, so its margins cannot collapse through it (upstream #1128; the same flag also
        // suppresses the empty-box collapse-through probe below). Size/inline-size containment have
        // no layout effect in this port.
        Contain contain = style.getContain();
        boolean hasStylesPreventingBeingCollapsedThrough =
            !style.isBlock() ||
            overflow.x.isScrollContainer() ||
            overflow.y.isScrollContainer() ||
            style.getPosition().isOutOfFlow() ||
            contain.establishesIndependentFormattingContext() ||
            ( !Float.isNaN(style.getAspectRatio())) ||
            padding.top > 0 ||
            padding.bottom > 0 ||
            border.top > 0 ||
            border.bottom > 0 ||
            (!Float.isNaN(nodeSize.height) && nodeSize.height > 0) ||
            (!Float.isNaN(nodeMinSize.height) && nodeMinSize.height > 0);

        // Return early when both dimensions are known only while *measuring* a leaf whose styles
        // already prevent it from being collapsed through. PerformLayout must not take this
        // shortcut: a fixed-size leaf still runs its measure callback so that a MeasureFunction can
        // report its baseline (the final size still comes from the known dimensions), and a
        // ComputeSize pass without collapse-preventing styles must also measure so the main path can
        // report marginsCanCollapseThrough. This mirrors upstream leaf.rs:
        //   if run_mode == RunMode::ComputeSize && has_styles_preventing_being_collapsed_through
        // LEAF-BASELINE / CR-07.
        if (inputs.runMode() == RunMode.COMPUTE_SIZE
            && hasStylesPreventingBeingCollapsedThrough
            && !Float.isNaN(styledBasedKnownDimensions.width)
            && !Float.isNaN(styledBasedKnownDimensions.height)) {
            // Ensure the size is at least padding+border, WITHOUT scrollbar.
            // Scrollbar gutter reduces content area but doesn't inflate explicit size.
            FloatSize size = new FloatSize(
                Math.max(styledBasedKnownDimensions.width, paddingBorderSize.width),
                Math.max(styledBasedKnownDimensions.height, paddingBorderSize.height)
            );
            // hasStylesPreventingBeingCollapsedThrough is true here, so the leaf cannot be
            // collapsed through and there is no need to probe the measure callback.
            return new LayoutOutput(
                size,
                FloatSize.zero(),
                new FloatPoint(Float.NaN, Float.NaN),
                CollapsibleMarginSet.ZERO,
                CollapsibleMarginSet.ZERO,
                false
            );
        }

        // Compute the available space handed to the measure callback (upstream leaf.rs):
        // 1. Start with available_space, which is already the space available to this node's
        //    *border box*: the owner of that space (the parent algorithm, or the root pass) has
        //    subtracted this node's margins, so the leaf must not subtract them again (X2/C17,
        //    upstream #1234). Subtracting here would double-count a parent's deduction.
        // 2. Override with known_dimensions or node_size if present
        // 3. Subtract content_box_inset for definite values
        TaffySize<AvailableSpace> contentAvailableSpace = new TaffySize<>(
            computeLeafAvailableSpace(
                availableSpace.width,
                knownDimensions.width,
                styledBasedKnownDimensions.width,
                contentBoxInsetSize.width,
                minSize.width,
                maxSize.width
            ),
            computeLeafAvailableSpace(
                availableSpace.height,
                knownDimensions.height,
                styledBasedKnownDimensions.height,
                contentBoxInsetSize.height,
                minSize.height,
                maxSize.height
            )
        );

        // Use a measure callback if available. The output size is a content size (same semantics
        // as the legacy MeasureFunc FloatSize); the padding/border/scrollbar inset is added below.
        // Subtract content box inset (incl. scrollbar) from known dimensions to get content area.
        //
        // A scroll container is measured as if no size were known on a full layout pass (upstream
        // leaf.rs `measure_function(match run_mode { RunMode::ComputeSize => known_dimensions, _ => Size::NONE }, ...)`):
        // its scrollable overflow rectangle is derived from the intrinsic content extent at the
        // available space, so echoing the already-known box size back to the callback would erase
        // exactly the overflow the rectangle describes (a 45x45 overflow:scroll text box reported
        // scrollWidth 0). Other leaves keep the S1 contract documented on {@link MeasureContext},
        // where the callback receives the content-box known dimensions so it can echo them.
        // SO-04.
        boolean measureWithoutKnownSize =
            inputs.runMode() == RunMode.PERFORM_LAYOUT
            && ScrollableOverflowUtil.isScrollContainer(overflow);
        FloatSize measureKnownDimensions = measureWithoutKnownSize
            ? new FloatSize(Float.NaN, Float.NaN)
            : new FloatSize(
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

        // A leaf that reaches the main path with both dimensions already known (PerformLayout after
        // the gated early return, or ComputeSize without collapse-preventing styles) keeps those
        // dimensions verbatim: the caller resolved them against its own layout, and the generic
        // clamp/aspect-ratio steps above can re-resolve them (e.g. a ContentSize flex target whose
        // style min/max or aspect ratio must not be applied a second time). The measure callback is
        // still consulted for baselines, content size and the collapse-through probe.
        // LEAF-BASELINE / CR-07.
        if (!Float.isNaN(styledBasedKnownDimensions.width)
            && !Float.isNaN(styledBasedKnownDimensions.height)) {
            size = new FloatSize(
                Math.max(styledBasedKnownDimensions.width, paddingBorderSize.width),
                Math.max(styledBasedKnownDimensions.height, paddingBorderSize.height)
            );
        }

        // Compute marginsCanCollapseThrough: true if no styles prevent it, height is 0, and measured height is 0
        float measuredHeight = Float.isNaN(measuredSize.height) ? 0f : measuredSize.height;
        // When both dimensions are known the main measure callback received the known height
        // (0), which masks content (a line box) inside a zero-height box. Probe once with an
        // unknown height, exactly like the old early-return path did, so margin collapsing stays
        // content-based. This branch is only reached now that the early return is limited to
        // ComputeSize + collapse-preventing styles. LEAF-BASELINE / CR-07.
        if (!hasStylesPreventingBeingCollapsedThrough
            && size.height == 0
            && !Float.isNaN(styledBasedKnownDimensions.width)
            && !Float.isNaN(styledBasedKnownDimensions.height)) {
            FloatSize collapseProbeKnownDimensions = new FloatSize(
                TaffyMath.maybeSub(styledBasedKnownDimensions.width, contentBoxInsetSize.width),
                Float.NaN
            );
            LayoutOutput probeOutput = measureLeaf(
                node, inputs, style, collapseProbeKnownDimensions, availableSpace);
            measuredHeight = probeOutput != null && !Float.isNaN(probeOutput.size().height)
                             ? probeOutput.size().height
                             : 0f;
        }
        boolean canCollapseThrough = !hasStylesPreventingBeingCollapsedThrough
                                     && size.height == 0
                                     && measuredHeight == 0;

        // A leaf includes its own start padding in the content size (the content starts after it),
        // but its end padding only when it is a scroll container: a non-scroll box does not extend
        // its scrollable overflow region with padding that comes after the content. In RTL the
        // inline start/end sides are the physical right/left ones. SO-05 / upstream #1117.
        boolean scrollContainer = ScrollableOverflowUtil.isScrollContainer(overflow);
        boolean isRtl = resolveDirection(node).isRtl();
        float startPadding = isRtl ? padding.right : padding.left;
        float endPadding = isRtl ? padding.left : padding.right;
        float measuredContentWidth = Float.isNaN(measuredSize.width) ? 0f : measuredSize.width;
        float measuredContentHeight = Float.isNaN(measuredSize.height) ? 0f : measuredSize.height;
        FloatSize measuredContentSize = new FloatSize(measuredContentWidth, measuredContentHeight);
        FloatSize contentSize = new FloatSize(
            startPadding + measuredContentWidth + (scrollContainer ? endPadding : 0f),
            padding.top + measuredContentHeight + (scrollContainer ? padding.bottom : 0f)
        );

        // Baselines and scrollable overflow reported by a MeasureFunction are propagated to the
        // final output (S1). A legacy MeasureFunc goes through MeasureFuncAdapter, which leaves
        // both empty, exactly like the pre-1.2 leaf path. The legacy firstBaselines field is kept
        // in sync with the new Baselines.first() value so old and new consumers observe the same
        // baseline.
        Baselines baselines = measuredOutput != null ? measuredOutput.baselines() : Baselines.NONE;
        ScrollableOverflow reportedOverflow = measuredOutput != null
                                                ? measuredOutput.scrollableOverflow()
                                                : ScrollableOverflow.ZERO;
        // An explicitly reported (non-empty) rectangle keeps the S1 measure contract and wins over
        // the computed leaf rectangle; an absent one is derived from the measured content size
        // (SO-01/SO-02 leaf rule: client-frame origin, start padding, end padding only for scroll
        // containers, RTL mirrored).
        ScrollableOverflow scrollableOverflow =
            reportedOverflow != null && !reportedOverflow.equals(ScrollableOverflow.ZERO)
            ? reportedOverflow
            : ScrollableOverflowUtil.leafScrollableOverflow(
                size, border, padding, scrollbarWidth, overflow, measuredContentSize, isRtl);

        return new LayoutOutput(
            size,
            contentSize,
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
        return computeBlockLayout(node, inputs, style, null);
    }

    private LayoutOutput computeBlockLayout(
        NodeId node, LayoutInput inputs, TaffyStyle style, BlockContext blockContext) {
        // Simplified block layout - full implementation in BlockComputer
        return new BlockComputer(this).compute(node, inputs, style, blockContext);
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
     * Compute available space for leaf node measurement following upstream leaf.rs:
     * 1. Start with the outer available space, which is already net of this node's margins
     *    (the parent subtracts a child's margins; a node never subtracts its own - X2/C17,
     *    upstream #1234)
     * 2. Override with known_dimensions or node_size if present
     * 3. Subtract content_box_inset and clamp for definite values
     */
    private AvailableSpace computeLeafAvailableSpace(
        AvailableSpace outerAvailable,
        float knownDimension,
        float nodeSize,
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
