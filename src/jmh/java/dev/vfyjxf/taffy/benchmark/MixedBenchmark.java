package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.lang.Float.NaN;

/**
 * Mixed grid+flex benchmarks - mirrors Rust taffy benches/mixed.rs.
 *
 * Rust uses cosmic_text to measure lorem ipsum text via a measure function.
 * Java port uses a deterministic, allocation-free word-wrapping measure function to approximate text measurement cost.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class MixedBenchmark {

    private static final long SEED = 12345L;

    // Matches Rust mixed.rs
    private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    @State(Scope.Thread)
    public static class MixedState {
        TaffyTree tree;
        NodeId root;

        @Param({"2", "4"})
        int depth;

        @Param({"4", "8"})
        int width;

        private MeasureFunc measureFunc;

        @Setup(Level.Trial)
        public void setupTrial() {
            // Precompute word widths once per thread.
            this.measureFunc = new LoremIpsumMeasure(LOREM_IPSUM);
        }

        @Setup(Level.Invocation)
        public void setupInvocation() {
            tree = new TaffyTree();
            Random rng = new Random(SEED);
            root = buildMixedTree(tree, depth, width, rng, true, measureFunc);
        }
    }

    @Benchmark
    public void mixedFlexGrid(MixedState state, Blackhole bh) {
        // Rust uses Size::MAX_CONTENT
        state.tree.computeLayoutWithMeasure(state.root, TaffySize.maxContent(), null);
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== Tree builder (mirrors Rust build_mixed_tree) ====================

    private static NodeId buildMixedTree(
        TaffyTree taffy,
        int depth,
        int width,
        Random rng,
        boolean isGrid,
        MeasureFunc measureFunc
    ) {
        if (depth == 0) {
            // Rust: new_leaf_with_context(Style::default(), context)
            return taffy.newLeafWithMeasure(new TaffyStyle(), measureFunc);
        }

        List<NodeId> children = new ArrayList<>(width);
        for (int i = 0; i < width; i++) {
            children.add(buildMixedTree(taffy, depth - 1, width, rng, !isGrid, measureFunc));
        }

        TaffyStyle style;
        if (isGrid) {
            int trackCount = (int) Math.ceil(Math.sqrt(width));
            style = randomNxNGridStyle(rng, trackCount);
        } else {
            style = randomFlexStyle(rng);
        }

        return taffy.newWithChildren(style, children);
    }

    // ==================== Random style generators (mirrors Rust) ====================

    private static TrackSizingFunction randomGridTrack(Random rng) {
        float r = rng.nextFloat();
        if (r < 0.1f) {
            return TrackSizingFunction.auto();
        } else if (r < 0.2f) {
            return TrackSizingFunction.minContent();
        } else if (r < 0.3f) {
            return TrackSizingFunction.maxContent();
        } else if (r < 0.5f) {
            return TrackSizingFunction.fr(1.0f);
        } else if (r < 0.6f) {
            return TrackSizingFunction.minmax(TrackSizingFunction.fixed(0.0f), TrackSizingFunction.fr(1.0f));
        } else if (r < 0.8f) {
            return TrackSizingFunction.fixed(40.0f);
        } else {
            return TrackSizingFunction.percent(0.3f);
        }
    }

    private static TaffyStyle randomNxNGridStyle(Random rng, int trackCount) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.GRID;

        List<TrackSizingFunction> cols = new ArrayList<>(trackCount);
        List<TrackSizingFunction> rows = new ArrayList<>(trackCount);
        for (int i = 0; i < trackCount; i++) {
            cols.add(randomGridTrack(rng));
            rows.add(randomGridTrack(rng));
        }
        style.gridTemplateColumns = cols;
        style.gridTemplateRows = rows;

        return style;
    }

    private static TaffyStyle randomFlexStyle(Random rng) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = rng.nextBoolean() ? FlexDirection.ROW : FlexDirection.COLUMN;
        style.flexWrap = rng.nextBoolean() ? FlexWrap.WRAP : FlexWrap.NO_WRAP;
        return style;
    }

    // ==================== Measure function (approximates cosmic_text measurement) ====================

    private static final class LoremIpsumMeasure implements MeasureFunc {
        private static final float CHAR_WIDTH = 7.0f;
        private static final float SPACE_WIDTH = 3.0f;
        private static final float LINE_HEIGHT = 16.0f;

        private final float[] wordWidths;
        private final float maxWordWidth;
        private final float singleLineWidth;

        private LoremIpsumMeasure(String text) {
            // Split once; measure() stays allocation-free.
            String[] words = text.split("\\s+");
            wordWidths = new float[words.length];
            float maxW = 0f;
            float sum = 0f;
            for (int i = 0; i < words.length; i++) {
                float w = words[i].length() * CHAR_WIDTH;
                wordWidths[i] = w;
                maxW = Math.max(maxW, w);
                sum += w;
                if (i != 0) {
                    sum += SPACE_WIDTH;
                }
            }
            maxWordWidth = maxW;
            singleLineWidth = sum;
        }

        @Override
        public FloatSize measure(FloatSize knownDimensions, TaffySize<AvailableSpace> availableSpace) {
            // If both dimensions are known, return them directly.
            if (!Float.isNaN(knownDimensions.width) && !Float.isNaN(knownDimensions.height)) {
                return new FloatSize(knownDimensions.width, knownDimensions.height);
            }

            // Compute width constraint like Rust's mixed.rs (known width wins; otherwise available_space determines constraint).
            float widthConstraint;
            if (!Float.isNaN(knownDimensions.width)) {
                widthConstraint = knownDimensions.width;
            } else {
                AvailableSpace w = availableSpace.width;
                if (w != null && w.isMinContent()) {
                    widthConstraint = 0f;
                } else if (w != null && w.isDefinite()) {
                    widthConstraint = w.getValue();
                } else {
                    // MaxContent / null => unconstrained
                    widthConstraint = NaN;
                }
            }

            float width;
            int lines;

            if (Float.isNaN(widthConstraint)) {
                // Max-content: all in one line.
                width = singleLineWidth;
                lines = 1;
            } else if (widthConstraint <= 0f) {
                // Min-content: wrap at every word.
                width = maxWordWidth;
                lines = wordWidths.length;
            } else {
                // Greedy word wrapping.
                float maxLine = 0f;
                float line = 0f;
                int lineCount = 1;

                for (int i = 0; i < wordWidths.length; i++) {
                    float w = wordWidths[i];
                    float add = (line == 0f) ? w : (SPACE_WIDTH + w);
                    if (line != 0f && (line + add) > widthConstraint) {
                        // new line
                        maxLine = Math.max(maxLine, line);
                        line = w;
                        lineCount++;
                    } else {
                        line += add;
                    }
                }
                maxLine = Math.max(maxLine, line);
                width = maxLine;
                lines = lineCount;
            }

            float height = lines * LINE_HEIGHT;

            // Respect known height if provided (optional).
            if (!Float.isNaN(knownDimensions.height)) {
                height = knownDimensions.height;
            }

            return new FloatSize(width, height);
        }
    }
}
