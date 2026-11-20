package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.Clear;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyFloat;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared builders and helpers for the benchmark suite.
 *
 * <p>The helpers here are deliberately small and allocation-light: the benchmark classes own the
 * scenarios, while this class owns only the repetitive tree plumbing. All builders are
 * deterministic so that a baseline and an aligned run create structurally identical trees.</p>
 */
final class BenchSupport {

    static final long SEED = 12345L;

    private BenchSupport() {
    }

    // === Tree walking / invalidation ===

    /**
     * Collects every node reachable from {@code root} with an iterative breadth-first walk.
     * Deep trees (depth 1000+) must not use recursion here: the helper runs inside JMH setup,
     * where a stack overflow would abort the benchmark rather than the measurement.
     */
    static NodeId[] collectNodes(TaffyTree tree, NodeId root) {
        List<NodeId> ordered = new ArrayList<>();
        ArrayDeque<NodeId> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            NodeId node = queue.poll();
            ordered.add(node);
            for (NodeId child : tree.getChildren(node)) {
                queue.add(child);
            }
        }
        return ordered.toArray(new NodeId[0]);
    }

    /**
     * Clears the layout cache of every node so that the next {@code computeLayout} is a cold
     * layout. This mirrors upstream taffy's {@code mark_all_dirty} benchmark helper: the tree is
     * built once and only the layout pass is timed.
     */
    static void clearCaches(TaffyTree tree, NodeId[] nodes) {
        for (NodeId node : nodes) {
            tree.clearCache(node);
        }
    }

    // === Dimensions and boxes ===

    static TaffyDimension len(float value) {
        return TaffyDimension.length(value);
    }

    static TaffyDimension pct(float value) {
        return TaffyDimension.percent(value);
    }

    /** {@code size: width x height}; either value may be {@link Float#NaN} for {@code auto}. */
    static TaffyStyle sized(float width, float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(
            Float.isNaN(width) ? TaffyDimension.AUTO : TaffyDimension.length(width),
            Float.isNaN(height) ? TaffyDimension.AUTO : TaffyDimension.length(height)
        );
        return style;
    }

    static TaffyStyle flex(FlexDirection direction) {
        TaffyStyle style = new TaffyStyle();
        style.display = TaffyDisplay.FLEX;
        style.flexDirection = direction;
        return style;
    }

    static TaffyStyle flexRow() {
        return flex(FlexDirection.ROW);
    }

    static TaffyStyle flexColumn() {
        return flex(FlexDirection.COLUMN);
    }

    static TaffyStyle sizedFlex(FlexDirection direction, float width, float height) {
        TaffyStyle style = flex(direction);
        style.size = new TaffySize<>(
            Float.isNaN(width) ? TaffyDimension.AUTO : TaffyDimension.length(width),
            Float.isNaN(height) ? TaffyDimension.AUTO : TaffyDimension.length(height)
        );
        return style;
    }

    static TaffyStyle block(float width, float height) {
        TaffyStyle style = sized(width, height);
        style.display = TaffyDisplay.BLOCK;
        return style;
    }

    static TaffyStyle flowRoot(float width, float height) {
        TaffyStyle style = sized(width, height);
        style.display = TaffyDisplay.FLOW_ROOT;
        return style;
    }

    static TaffyStyle floated(TaffyFloat side, float width, float height) {
        TaffyStyle style = block(width, height);
        style.float_ = side;
        return style;
    }

    static TaffyStyle cleared(Clear clear, float width, float height) {
        TaffyStyle style = block(width, height);
        style.clear = clear;
        return style;
    }

    static void padding(TaffyStyle style, float value) {
        style.padding = TaffyRect.all(LengthPercentage.length(value));
    }

    static void margin(TaffyStyle style, float value) {
        style.margin = TaffyRect.all(LengthPercentageAuto.length(value));
    }

    static void gap(TaffyStyle style, float value) {
        style.gap = new TaffySize<>(LengthPercentage.length(value), LengthPercentage.length(value));
    }

    static void overflow(TaffyStyle style, Overflow x, Overflow y) {
        style.overflow = new dev.vfyjxf.taffy.geometry.TaffyPoint<>(x, y);
    }

    // === Grid tracks ===

    static List<TrackSizingFunction> fixedTracks(int count, float size) {
        List<TrackSizingFunction> tracks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            tracks.add(TrackSizingFunction.fixed(size));
        }
        return tracks;
    }

    static List<TrackSizingFunction> frTracks(int count) {
        List<TrackSizingFunction> tracks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            tracks.add(TrackSizingFunction.fr(1f));
        }
        return tracks;
    }

    static List<TrackSizingFunction> autoTracks(int count) {
        List<TrackSizingFunction> tracks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            tracks.add(TrackSizingFunction.auto());
        }
        return tracks;
    }

    static List<TrackSizingFunction> repeatTrack(TrackSizingFunction track, int count) {
        List<TrackSizingFunction> tracks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            tracks.add(track);
        }
        return tracks;
    }

    /** Smallest square side that holds {@code cells} cells. */
    static int side(int cells) {
        return Math.max(1, (int) Math.ceil(Math.sqrt(cells)));
    }

    // === Measure functions ===

    /**
     * Deterministic word-wrapping measure function used by the measure-heavy benchmarks.
     *
     * <p>The per-word widths are precomputed, so {@link #measure} is allocation-free apart from
     * the returned {@link FloatSize}; this keeps the benchmark focused on the layout engine's
     * measure path rather than on string handling.</p>
     */
    static final class WordWrapMeasure implements MeasureFunc {
        private static final float SPACE_WIDTH = 3.0f;
        private static final float LINE_HEIGHT = 16.0f;

        private final float[] wordWidths;
        private final float maxWordWidth;
        private final float singleLineWidth;

        private long calls;

        WordWrapMeasure(int wordCount) {
            this.wordWidths = new float[wordCount];
            float max = 0f;
            float sum = 0f;
            for (int i = 0; i < wordCount; i++) {
                float width = 8f + ((i * 37 + 11) % 80);
                wordWidths[i] = width;
                max = Math.max(max, width);
                sum += width;
                if (i != 0) {
                    sum += SPACE_WIDTH;
                }
            }
            this.maxWordWidth = max;
            this.singleLineWidth = sum;
        }

        long callCount() {
            return calls;
        }

        @Override
        public FloatSize measure(FloatSize knownDimensions, TaffySize<AvailableSpace> availableSpace) {
            calls++;

            if (!Float.isNaN(knownDimensions.width) && !Float.isNaN(knownDimensions.height)) {
                return new FloatSize(knownDimensions.width, knownDimensions.height);
            }

            float widthConstraint;
            if (!Float.isNaN(knownDimensions.width)) {
                widthConstraint = knownDimensions.width;
            } else {
                AvailableSpace width = availableSpace.width;
                if (width != null && width.isMinContent()) {
                    widthConstraint = 0f;
                } else if (width != null && width.isDefinite()) {
                    widthConstraint = width.getValue();
                } else {
                    widthConstraint = Float.NaN;
                }
            }

            float width;
            int lines;
            if (Float.isNaN(widthConstraint)) {
                width = singleLineWidth;
                lines = 1;
            } else if (widthConstraint <= 0f) {
                width = maxWordWidth;
                lines = wordWidths.length;
            } else {
                float maxLine = 0f;
                float line = 0f;
                int lineCount = 1;
                for (float word : wordWidths) {
                    float add = (line == 0f) ? word : (SPACE_WIDTH + word);
                    if (line != 0f && (line + add) > widthConstraint) {
                        maxLine = Math.max(maxLine, line);
                        line = word;
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
            if (!Float.isNaN(knownDimensions.height)) {
                height = knownDimensions.height;
            }
            return new FloatSize(Float.isNaN(knownDimensions.width) ? width : knownDimensions.width, height);
        }
    }

    /** A tree plus its root and a pre-computed node list for cold-layout invalidation. */
    static final class TreeCase {
        final TaffyTree tree;
        final NodeId root;
        final NodeId[] nodes;

        TreeCase(TaffyTree tree, NodeId root) {
            this.tree = tree;
            this.root = root;
            this.nodes = collectNodes(tree, root);
        }

        void invalidateCaches() {
            clearCaches(tree, nodes);
        }
    }
}
