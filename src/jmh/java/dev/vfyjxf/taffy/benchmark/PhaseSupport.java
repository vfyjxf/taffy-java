package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree builders for the phase benchmarks.
 *
 * <p>Every builder is deterministic: the same {@code (display, sizing, count)} triple always
 * produces a structurally identical tree, so a baseline and a candidate checkout measure the
 * same work. Styles are created once per node and never mutated afterwards; a benchmark that
 * mutates styles owns those style instances itself.</p>
 *
 * <p>The two shapes mirror the upstream {@code benches/relayout.rs} builders where possible:</p>
 * <ul>
 *   <li><b>flat</b>: one container with {@code nodeCount - 1} leaf children (upstream's
 *       {@code wide} shape taken to 100k);</li>
 *   <li><b>deep</b>: a chain of {@code depth} containers, each with one continuing child and one
 *       leaf side child (upstream's deep/super-deep chain shape).</li>
 * </ul>
 *
 * <p>Leaf sizing variants split style-resolution work out of the layout algorithm:</p>
 * <ul>
 *   <li>{@code fixed}: 10x10 definite leaf, no intrinsic sizing;</li>
 *   <li>{@code auto}: auto size plus a small padding/border, so every leaf resolves a content
 *       contribution instead of reading a definite size;</li>
 *   <li>{@code percent}: 10% x 10% leaf under a definite 2000x2000 container;</li>
 *   <li>{@code margin}: fixed 10x10 leaf with a 2px margin on every side.</li>
 * </ul>
 */
final class PhaseSupport {

    /** Layout width used by the incremental/flat roots (upstream relayout: 2000). */
    static final float LAYOUT_WIDTH = 2000f;

    /** Definite extent of the synthetic ancestor that makes percentage sizes resolvable. */
    static final float PERCENT_EXTENT = 2000f;

    /** Explicit column count for the flat grid scenario; keeps the implicit grid shallow. */
    static final int FLAT_GRID_COLUMNS = 64;

    static final String FIXED = "fixed";
    static final String AUTO = "auto";
    static final String PERCENT = "percent";
    static final String MARGIN = "margin";

    private PhaseSupport() {
    }

    /** The available space shared by the phase benchmarks (upstream relayout: 2000 x max-content). */
    static TaffySize<AvailableSpace> layoutSpace() {
        return TaffySize.of(AvailableSpace.definite(LAYOUT_WIDTH), AvailableSpace.maxContent());
    }

    /** Available space for a container whose own style size is definite. */
    static TaffySize<AvailableSpace> definiteSpace() {
        return TaffySize.of(AvailableSpace.definite(PERCENT_EXTENT), AvailableSpace.definite(PERCENT_EXTENT));
    }

    static TaffyDisplay displayOf(String display) {
        return switch (display) {
            case "flex" -> TaffyDisplay.FLEX;
            case "block" -> TaffyDisplay.BLOCK;
            case "grid" -> TaffyDisplay.GRID;
            default -> throw new IllegalArgumentException("Unknown display: " + display);
        };
    }

    // === Styles ===

    /** A leaf style for the requested sizing variant. */
    static TaffyStyle leafStyle(String sizing, TaffyDisplay display) {
        TaffyStyle style = new TaffyStyle();
        if (display == TaffyDisplay.BLOCK) {
            // Leaves of a block container participate in its block formatting context.
            style.display = TaffyDisplay.BLOCK;
        }
        switch (sizing) {
            case FIXED -> style.size = new TaffySize<>(
                TaffyDimension.length(10f), TaffyDimension.length(10f));
            case AUTO -> {
                // Auto size with non-zero padding/border: the leaf has to resolve its own content
                // box instead of reading a definite style size.
                style.padding = TaffyRect.all(LengthPercentage.length(2f));
                style.border = TaffyRect.all(LengthPercentage.length(1f));
            }
            case PERCENT -> style.size = new TaffySize<>(
                TaffyDimension.percent(0.1f), TaffyDimension.percent(0.1f));
            case MARGIN -> {
                style.size = new TaffySize<>(TaffyDimension.length(10f), TaffyDimension.length(10f));
                style.margin = TaffyRect.all(LengthPercentageAuto.length(2f));
            }
            default -> throw new IllegalArgumentException("Unknown sizing: " + sizing);
        }
        return style;
    }

    /** A container style for the requested display and chain level. */
    static TaffyStyle containerStyle(TaffyDisplay display, int level, String sizing) {
        TaffyStyle style = new TaffyStyle();
        style.display = display;
        switch (display) {
            case FLEX -> style.flexDirection = (level & 1) == 0 ? FlexDirection.ROW : FlexDirection.COLUMN;
            case GRID -> style.gridTemplateColumns = List.of(TrackSizingFunction.auto());
            default -> {
                // BLOCK (and FLOW_ROOT, unused here) need no extra container style.
            }
        }
        if (PERCENT.equals(sizing)) {
            // Percentage children need a definite containing block at every level, otherwise the
            // percentage resolves against an auto size and the scenario stops exercising
            // percentage resolution.
            style.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.percent(1f));
        }
        return style;
    }

    /** Root style for the flat shape: one container with every leaf as a direct child. */
    static TaffyStyle flatRootStyle(TaffyDisplay display, String sizing) {
        TaffyStyle style = containerStyle(display, 0, sizing);
        if (display == TaffyDisplay.GRID) {
            List<TrackSizingFunction> columns = new ArrayList<>(FLAT_GRID_COLUMNS);
            for (int i = 0; i < FLAT_GRID_COLUMNS; i++) {
                columns.add(TrackSizingFunction.auto());
            }
            style.gridTemplateColumns = columns;
        }
        if (PERCENT.equals(sizing)) {
            style.size = new TaffySize<>(
                TaffyDimension.length(PERCENT_EXTENT), TaffyDimension.length(PERCENT_EXTENT));
        }
        return style;
    }

    /** Root style for the deep shape: a wrapper that makes percentage chains resolvable. */
    static TaffyStyle deepRootStyle(TaffyDisplay display, String sizing) {
        TaffyStyle style = containerStyle(display, 0, sizing);
        if (PERCENT.equals(sizing)) {
            style.size = new TaffySize<>(
                TaffyDimension.length(PERCENT_EXTENT), TaffyDimension.length(PERCENT_EXTENT));
        }
        return style;
    }

    // === Trees ===

    /**
     * Builds a flat tree: a single container with {@code nodeCount - 1} leaf children.
     *
     * @param nodeCount total node count, including the root container
     */
    static NodeId buildFlat(TaffyTree tree, String displayName, int nodeCount, String sizing) {
        TaffyDisplay display = displayOf(displayName);
        TaffyStyle leaf = leafStyle(sizing, display);
        int childCount = Math.max(1, nodeCount - 1);
        NodeId[] children = new NodeId[childCount];
        for (int i = 0; i < childCount; i++) {
            children[i] = tree.newLeaf(leaf);
        }
        return tree.newWithChildren(flatRootStyle(display, sizing), children);
    }

    /**
     * Builds a deep chain: {@code depth} containers, each with a continuing child and one leaf,
     * wrapped in one root. Total node count is {@code 2 * depth + 2}.
     *
     * <p>The loop is iterative on purpose: a 1024-deep tree must not rely on the recursive
     * builder at a Java stack size that is only guaranteed by the JMH task's {@code -Xss16m}.</p>
     */
    static NodeId buildDeep(TaffyTree tree, String displayName, int depth, String sizing) {
        TaffyDisplay display = displayOf(displayName);
        TaffyStyle leaf = leafStyle(sizing, display);
        NodeId continuing = tree.newLeaf(leaf);
        for (int level = depth; level >= 1; level--) {
            NodeId side = tree.newLeaf(leaf);
            continuing = tree.newWithChildren(containerStyle(display, level, sizing), continuing, side);
        }
        return tree.newWithChildren(deepRootStyle(display, sizing), continuing);
    }

    // === Measured leaves (LeafFlowBenchmark / IncrementalPhaseBenchmark) ===

    /** Width of the column flex root used by the measured-leaf scenarios. */
    static final float MEASURE_ROOT_WIDTH = 800f;

    /** Available space for the measured-leaf scenarios. */
    static TaffySize<AvailableSpace> measureSpace() {
        return TaffySize.of(AvailableSpace.definite(MEASURE_ROOT_WIDTH), AvailableSpace.maxContent());
    }

    /** The shared leaf style: full-width, auto-height, so the measure callback is always reached. */
    static TaffyStyle measuredLeafStyle() {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
        return style;
    }

    /** The shared measured-tree root: a column flex box of the given width. */
    static TaffyStyle measuredRootStyle() {
        return BenchSupport.sizedFlex(FlexDirection.COLUMN, MEASURE_ROOT_WIDTH, Float.NaN);
    }

    /**
     * Builds the measured-leaf tree used by the measure scenarios.
     *
     * @param leafCount number of measured leaves; the tree also contains one root container
     */
    static NodeId buildMeasuredTree(TaffyTree tree, int leafCount, MeasureFunc measure) {
        return buildMeasuredTree(tree, leafCount, measure, measuredLeafStyle());
    }

    static NodeId buildMeasuredTree(TaffyTree tree, int leafCount, MeasureFunc measure, TaffyStyle leafStyle) {
        NodeId[] children = new NodeId[leafCount];
        for (int i = 0; i < leafCount; i++) {
            children[i] = tree.newLeafWithMeasure(leafStyle, measure);
        }
        return tree.newWithChildren(measuredRootStyle(), children);
    }

    /**
     * Creates the callback for a {@code fixed|text} measure parameter.
     *
     * <p>{@code fixed} returns a constant 10x10 content size; {@code text} is the deterministic
     * 16-word wrapping callback from {@link BenchSupport}.</p>
     */
    static MeasureFunc measureOf(String kind) {
        return switch (kind) {
            case "fixed" -> new FixedMeasure();
            case "text" -> new BenchSupport.WordWrapMeasure(16);
            default -> throw new IllegalArgumentException("Unknown measure kind: " + kind);
        };
    }

    /** Number of times a callback created by {@link #measureOf(String)} has been invoked. */
    static long measureCallCount(MeasureFunc measure) {
        if (measure instanceof FixedMeasure fixed) {
            return fixed.cells;
        }
        if (measure instanceof BenchSupport.WordWrapMeasure text) {
            return text.callCount();
        }
        return 0L;
    }

    /** Constant-size measure callback: the cheapest realistic leaf callback. */
    static final class FixedMeasure implements MeasureFunc {
        private static final float SIZE = 10f;
        private long cells;

        @Override
        public FloatSize measure(FloatSize knownDimensions, TaffySize<AvailableSpace> availableSpace) {
            cells++;
            float width = Float.isNaN(knownDimensions.width) ? SIZE : knownDimensions.width;
            float height = Float.isNaN(knownDimensions.height) ? SIZE : knownDimensions.height;
            return new FloatSize(width, height);
        }
    }
}
