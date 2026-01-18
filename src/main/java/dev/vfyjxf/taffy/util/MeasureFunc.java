package dev.vfyjxf.taffy.util;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.AvailableSpace;

/**
 * Functional interface for measuring leaf nodes (e.g., text content).
 */
@FunctionalInterface
public interface MeasureFunc {

    /**
     * Measures the intrinsic size of a node.
     *
     * @param knownDimensions  Known dimensions constraint (may have null values)
     * @param availableSpace   Available space constraint
     * @return The measured size of the node
     */
    FloatSize measure(
        FloatSize knownDimensions,
        Size<AvailableSpace> availableSpace
    );
}
