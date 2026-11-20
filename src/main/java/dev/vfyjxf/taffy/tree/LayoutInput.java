package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;

/**
 * A struct containing the input constraints/hints for laying out a node, which are passed in by the parent.
 *
 * @param runMode                       Whether we only need to know the Node's size, or whether we need to perform a full layout
 * @param sizingMode                    Whether a Node's style sizes should be taken into account or ignored
 * @param axis                          Which axis we need the size of
 * @param knownDimensions               Known dimensions represent dimensions (width/height) which should be taken as fixed when performing layout.
 * @param parentSize                    Parent size dimensions are intended to be used for percentage resolution.
 * @param availableSpace                Available space represents an amount of space to layout into, and is used as a soft constraint for wrapping.
 * @param verticalMarginsAreCollapsible Specific to CSS Block layout. Used for correctly computing margin collapsing.
 */
public record LayoutInput(
    RunMode runMode,
    SizingMode sizingMode,
    RequestedAxis axis,
    FloatSize knownDimensions,
    FloatSize parentSize,
    TaffySize<AvailableSpace> availableSpace,
    TaffyLine<Boolean> verticalMarginsAreCollapsible
) {

    /**
     * Create a LayoutInput for hidden layout
     */
    public static final LayoutInput HIDDEN = new LayoutInput(
        RunMode.PERFORM_HIDDEN_LAYOUT,
        SizingMode.INHERENT_SIZE,
        RequestedAxis.BOTH,
        FloatSize.none(),
        FloatSize.none(),
        new TaffySize<>(AvailableSpace.MAX_CONTENT, AvailableSpace.MAX_CONTENT),
        TaffyLine.FALSE
    );

    /**
     * Static factory method for hidden layout input
     */
    public static LayoutInput hidden() {
        return HIDDEN;
    }

    /**
     * Swap the horizontal and vertical axes of every per-axis input.
     *
     * <p>Lets a tree lay out a node in a vertical writing mode: transpose the inputs, run the
     * (horizontal top-to-bottom) layout algorithm, then transpose the {@link LayoutOutput} back.
     * {@code runMode} and {@code sizingMode} are axis-independent and are preserved;
     * {@code knownDimensions}, {@code parentSize} and {@code availableSpace} swap their axes, and
     * {@code axis} is transposed ({@link RequestedAxis#BOTH} stays {@code BOTH}).</p>
     *
     * <p>{@code verticalMarginsAreCollapsible} is reset to {@link TaffyLine#FALSE}: vertical
     * margins cannot collapse across a change of block axis. This matches upstream taffy #1265
     * ({@code LayoutInput::transpose}), so transposing twice does <b>not</b> restore that
     * field.</p>
     *
     * <p>The receiver is never modified. The axis transposition is handled by this method
     * ({@link RequestedAxis} itself is unchanged in 1.2); the semantics match the upstream
     * {@code RequestedAxis::transpose}.</p>
     *
     * @return a new transposed input
     */
    public LayoutInput transpose() {
        return new LayoutInput(
            runMode,
            sizingMode,
            transposeAxis(axis),
            new FloatSize(knownDimensions.height, knownDimensions.width),
            new FloatSize(parentSize.height, parentSize.width),
            availableSpace.transpose(),
            TaffyLine.FALSE
        );
    }

    private static RequestedAxis transposeAxis(RequestedAxis axis) {
        if (axis == null) {
            return null;
        }
        return switch (axis) {
            case HORIZONTAL -> RequestedAxis.VERTICAL;
            case VERTICAL -> RequestedAxis.HORIZONTAL;
            case BOTH -> RequestedAxis.BOTH;
        };
    }

    // === Getters ===

    /**
     * Create a copy with modified knownDimensions
     */
    public LayoutInput withKnownDimensions(FloatSize knownDimensions) {
        return new LayoutInput(
            this.runMode,
            this.sizingMode,
            this.axis,
            knownDimensions,
            this.parentSize,
            this.availableSpace,
            this.verticalMarginsAreCollapsible
        );
    }

    /**
     * Create a copy with modified availableSpace
     */
    public LayoutInput withAvailableSpace(TaffySize<AvailableSpace> availableSpace) {
        return new LayoutInput(
            this.runMode,
            this.sizingMode,
            this.axis,
            this.knownDimensions,
            this.parentSize,
            availableSpace,
            this.verticalMarginsAreCollapsible
        );
    }

    @Override
    public String toString() {
        return "LayoutInput{runMode=" + runMode + ", knownDimensions=" + knownDimensions +
               ", availableSpace=" + availableSpace + "}";
    }
}
