package dev.vfyjxf.taffy.util;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.tree.LayoutOutput;

import java.util.Objects;

/**
 * Adapts a legacy {@link MeasureFunc} to the new {@link MeasureFunction} contract.
 *
 * <p>The adapter forwards the content-box {@link MeasureContext#input()} arguments that the legacy
 * callback used to receive — {@code input.knownDimensions()} and {@code input.availableSpace()} —
 * and wraps the returned content size with {@link LayoutOutput#fromOuterSize(FloatSize)}. That
 * factory leaves the baselines absent and the scrollable overflow zero, which is exactly the
 * behaviour every legacy measure function had: the old leaf path never propagated baselines from a
 * measure callback and hard-coded them to NaN.</p>
 *
 * <p><b>1.2 status (S1, additive):</b> the adapter is how the two leaf measure call sites keep
 * supporting old lambdas while routing every call through the new callback shape. It is used
 * internally by {@code LayoutComputer}; callers can also use it to mix old callbacks into the new
 * API.</p>
 *
 * @see MeasureFunction
 * @see MeasureFunc
 */
public final class MeasureFuncAdapter implements MeasureFunction {

    private final MeasureFunc measureFunc;

    /**
     * Creates an adapter around a legacy measure function.
     *
     * @param measureFunc the legacy callback to adapt, must not be {@code null}
     * @throws NullPointerException if {@code measureFunc} is {@code null}
     */
    public MeasureFuncAdapter(MeasureFunc measureFunc) {
        this.measureFunc = Objects.requireNonNull(measureFunc, "measureFunc");
    }

    /**
     * The adapted legacy measure function.
     *
     * @return the callback supplied to the constructor
     */
    public MeasureFunc measureFunc() {
        return measureFunc;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to the adapted {@link MeasureFunc} with the content-box known dimensions and
     * available space taken from the context input.</p>
     */
    @Override
    public LayoutOutput measure(MeasureContext context) {
        Objects.requireNonNull(context, "context");
        FloatSize measuredSize = measureFunc.measure(
            context.input().knownDimensions(),
            context.input().availableSpace()
        );
        return LayoutOutput.fromOuterSize(measuredSize);
    }

    @Override
    public String toString() {
        return "MeasureFuncAdapter{" + measureFunc + '}';
    }
}
