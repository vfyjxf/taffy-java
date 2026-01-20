package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.style.GridAutoFlow;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.GridRepetition;
import dev.vfyjxf.taffy.style.GridTemplateComponent;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test-only adapter for exercising grid internals.
 *
 * <p>This class is intentionally located in the {@code dev.vfyjxf.taffy.tree} package so it can
 * call package-private debug hooks in {@link GridComputer} without reflection.
 */
public final class GridTestAdapter {

    private GridTestAdapter() {}

    public static final class PlacementItem {
        public final int index;
        public final int columnStart;
        public final int columnEnd;
        public final int rowStart;
        public final int rowEnd;

        public PlacementItem(int index, int columnStart, int columnEnd, int rowStart, int rowEnd) {
            this.index = index;
            this.columnStart = columnStart;
            this.columnEnd = columnEnd;
            this.rowStart = rowStart;
            this.rowEnd = rowEnd;
        }
    }

    public static final class PlacementResult {
        public final TrackCounts columnCounts;
        public final TrackCounts rowCounts;
        public final List<PlacementItem> items;

        public PlacementResult(TrackCounts columnCounts, TrackCounts rowCounts, List<PlacementItem> items) {
            this.columnCounts = columnCounts;
            this.rowCounts = rowCounts;
            this.items = items;
        }
    }

    public static PlacementResult runPlacement(
            int explicitColumnCount,
            int explicitRowCount,
            List<TaffyStyle> childStyles,
            GridAutoFlow flow) {
        GridComputer.DebugPlacementResult raw = GridComputer.debugRunPlacementForTest(
                explicitColumnCount,
                explicitRowCount,
                childStyles,
                flow);

        List<PlacementItem> items = new ArrayList<>();
        for (GridComputer.DebugPlacedItem item : raw.items()) {
            items.add(new PlacementItem(item.index(), item.columnStart(), item.columnEnd(), item.rowStart(), item.rowEnd()));
        }
        return new PlacementResult(raw.columnCounts(), raw.rowCounts(), Collections.unmodifiableList(items));
    }

    // ---------------------------------------------------------------------
    // Ports of Rust helper algorithms used only by unit tests
    // ---------------------------------------------------------------------

    public enum AutoRepeatStrategy {
        /** Max repetitions that do not overflow the container. */
        MAX_REPETITIONS_THAT_DO_NOT_OVERFLOW,
        /** Min repetitions required to overflow the container (used for min-size constraint tests). */
        MIN_REPETITIONS_THAT_DO_OVERFLOW
    }

    public record ExplicitGridSizeInAxis(int autoRepetitions, int explicitTrackCount) {}

    /**
     * Port of Rust's compute_explicit_grid_size_in_axis (tests only).
     */
    public static ExplicitGridSizeInAxis computeExplicitGridSizeInAxis(
            TaffyStyle containerStyle,
            float autoFitContainerSize,
            AutoRepeatStrategy autoFitStrategy,
            boolean horizontalAxis) {

        List<GridTemplateComponent> template = getTemplate(containerStyle, horizontalAxis);
        if (template == null) {
            return new ExplicitGridSizeInAxis(0, 0);
        }
        if (template.isEmpty()) {
            return new ExplicitGridSizeInAxis(0, 0);
        }

        // Invalid if any repetition has zero tracks
        for (GridTemplateComponent def : template) {
            if (def != null && def.isRepeat()) {
                GridRepetition rep = def.getRepeat();
                if (rep != null && rep.getTrackCount() == 0) {
                    return new ExplicitGridSizeInAxis(0, 0);
                }
            }
        }

        int nonAutoRepeatingTrackCount = 0;
        int autoRepetitionCount = 0;
        boolean allTrackDefsHaveFixedComponent = true;

        for (GridTemplateComponent def : template) {
            if (def == null) continue;

            if (def.isSingle()) {
                nonAutoRepeatingTrackCount += 1;
                TrackSizingFunction tsf = def.getSingle();
                allTrackDefsHaveFixedComponent &= (tsf != null && tsf.hasFixedComponent());
            } else {
                GridRepetition rep = def.getRepeat();
                if (rep != null && rep.isAutoRepetition()) {
                    autoRepetitionCount += 1;
                } else if (rep != null) {
                    nonAutoRepeatingTrackCount += rep.getCount() * rep.getTrackCount();
                }
                allTrackDefsHaveFixedComponent &= (rep != null && rep.hasFixedComponent());
            }
        }

        boolean templateIsValid = autoRepetitionCount == 0 || (autoRepetitionCount == 1 && allTrackDefsHaveFixedComponent);
        if (!templateIsValid) {
            return new ExplicitGridSizeInAxis(0, 0);
        }

        if (autoRepetitionCount == 0) {
            return new ExplicitGridSizeInAxis(0, nonAutoRepeatingTrackCount);
        }

        // Find the auto repetition
        GridRepetition repetitionDefinition = null;
        for (GridTemplateComponent def : template) {
            if (def != null && def.isRepeat()) {
                GridRepetition rep = def.getRepeat();
                if (rep != null && rep.isAutoRepetition()) {
                    repetitionDefinition = rep;
                    break;
                }
            }
        }
        if (repetitionDefinition == null) {
            // Shouldn't happen if autoRepetitionCount==1, but be defensive.
            return new ExplicitGridSizeInAxis(0, nonAutoRepeatingTrackCount);
        }

        int repetitionTrackCount = repetitionDefinition.getTrackCount();

        float gapSize = horizontalAxis
                ? containerStyle.getGap().width.resolveOrZero(autoFitContainerSize)
                : containerStyle.getGap().height.resolveOrZero(autoFitContainerSize);

        int numRepetitions;
        if (Float.isNaN(autoFitContainerSize)) {
            numRepetitions = 1;
        } else {
            float innerContainerSize = autoFitContainerSize;

            // Space used by non-repeating tracks
            float nonRepeatingUsedSpace = 0f;
            for (GridTemplateComponent def : template) {
                if (def == null) continue;

                if (def.isSingle()) {
                    TrackSizingFunction tsf = def.getSingle();
                    nonRepeatingUsedSpace += trackDefiniteValue(tsf, autoFitContainerSize);
                } else {
                    GridRepetition rep = def.getRepeat();
                    if (rep == null) continue;

                    if (rep.isAutoRepetition()) {
                        // 0
                    } else {
                        float sum = 0f;
                        for (TrackSizingFunction tsf : rep.getTracks()) {
                            sum += trackDefiniteValue(tsf, autoFitContainerSize);
                        }
                        nonRepeatingUsedSpace += sum * rep.getCount();
                    }
                }
            }

            // Space used by a single repetition
            float perRepetitionTrackUsedSpace = 0f;
            for (TrackSizingFunction tsf : repetitionDefinition.getTracks()) {
                perRepetitionTrackUsedSpace += trackDefiniteValue(tsf, autoFitContainerSize);
            }

            float firstRepetitionAndNonRepeatingTracksUsedSpace = nonRepeatingUsedSpace
                    + perRepetitionTrackUsedSpace
                    + Math.max(0, (nonAutoRepeatingTrackCount + repetitionTrackCount) - 1) * gapSize;

            if (firstRepetitionAndNonRepeatingTracksUsedSpace > innerContainerSize) {
                numRepetitions = 1;
            } else {
                float perRepetitionGapUsedSpace = repetitionTrackCount * gapSize;
                float perRepetitionUsedSpace = perRepetitionTrackUsedSpace + perRepetitionGapUsedSpace;

                if (perRepetitionUsedSpace <= 0.0f) {
                    numRepetitions = 1;
                } else {
                    float numRepsThatFit = (innerContainerSize - firstRepetitionAndNonRepeatingTracksUsedSpace)
                            / perRepetitionUsedSpace;

                    int additional;
                    if (autoFitStrategy == AutoRepeatStrategy.MAX_REPETITIONS_THAT_DO_NOT_OVERFLOW) {
                        additional = (int) Math.floor(numRepsThatFit);
                    } else {
                        additional = (int) Math.ceil(numRepsThatFit);
                    }
                    numRepetitions = 1 + Math.max(0, additional);
                }
            }
        }

        int gridTemplateTrackCount = nonAutoRepeatingTrackCount + (repetitionTrackCount * numRepetitions);
        return new ExplicitGridSizeInAxis(numRepetitions, gridTemplateTrackCount);
    }

    private static float trackDefiniteValue(TrackSizingFunction sizingFunction, Float parentSize) {
        if (sizingFunction == null) {
            // auto / none => treat as 0 for definite sizing computations
            return 0f;
        }
        Float val = sizingFunction.getDefiniteValue(parentSize);
        return val != null ? val : 0f;
    }

    private static List<GridTemplateComponent> getTemplate(TaffyStyle style, boolean horizontalAxis) {
        if (style == null) return null;

        List<GridTemplateComponent> withRepeat = horizontalAxis
                ? style.gridTemplateColumnsWithRepeat
                : style.gridTemplateRowsWithRepeat;

        if (withRepeat != null) {
            return withRepeat;
        }

        List<TrackSizingFunction> plain = horizontalAxis
                ? style.getGridTemplateColumns()
                : style.getGridTemplateRows();

        if (plain == null) {
            return null;
        }

        if (plain.isEmpty()) {
            return Collections.emptyList();
        }

        List<GridTemplateComponent> converted = new ArrayList<>();
        for (TrackSizingFunction tsf : plain) {
            converted.add(GridTemplateComponent.single(tsf));
        }
        return converted;
    }

    // ---- implicit grid helper ports (tests only) ----

    public static final class ChildMinMaxLineSpan {
        public final int minLine;
        public final int maxLine;
        public final int span;

        public ChildMinMaxLineSpan(int minLine, int maxLine, int span) {
            this.minLine = minLine;
            this.maxLine = maxLine;
            this.span = span;
        }
    }

    /**
     * Port of Rust child_min_line_max_line_span. Returns values in OriginZero line coordinates.
     */
    public static ChildMinMaxLineSpan childMinMaxLineSpan(TaffyLine<GridPlacement> line, int explicitTrackCount) {
        if (line == null) {
            return new ChildMinMaxLineSpan(0, 0, 0);
        }

        OriginZeroPlacement oz = intoOriginZeroIgnoringNamed(line, explicitTrackCount);

        int min;
        int max;

        boolean startIsLine = oz.startType == OriginZeroPlacement.Type.LINE;
        boolean endIsLine = oz.endType == OriginZeroPlacement.Type.LINE;

        if (startIsLine && endIsLine) {
            int t1 = oz.startValue;
            int t2 = oz.endValue;
            if (t1 == t2) {
                min = t1;
                max = t1 + 1;
            } else {
                min = Math.min(t1, t2);
                max = Math.max(t1, t2);
            }
        } else if (startIsLine) {
            int t = oz.startValue;
            min = t;
            if (oz.endType == OriginZeroPlacement.Type.SPAN) {
                max = t + oz.endValue;
            } else {
                max = t + 1;
            }
        } else if (endIsLine) {
            int t = oz.endValue;
            if (oz.startType == OriginZeroPlacement.Type.SPAN) {
                min = t - oz.startValue;
            } else {
                min = t;
            }
            max = t;
        } else {
            // Auto|Span with no definite lines: doesn't affect min/max estimate
            min = 0;
            max = 0;
        }

        int span;
        if (!startIsLine && !endIsLine) {
            span = oz.indefiniteSpan();
        } else {
            span = 1;
        }

        return new ChildMinMaxLineSpan(min, max, span);
    }

    public static final class GridSizeEstimate {
        public final TrackCounts columnCounts;
        public final TrackCounts rowCounts;

        public GridSizeEstimate(TrackCounts columnCounts, TrackCounts rowCounts) {
            this.columnCounts = columnCounts;
            this.rowCounts = rowCounts;
        }
    }

    /**
     * Port of Rust compute_grid_size_estimate (tests only).
     */
    public static GridSizeEstimate computeGridSizeEstimate(
            int explicitColumnCount,
            int explicitRowCount,
            List<TaffyStyle> childStyles) {

        int colMin = 0;
        int colMax = 0;
        int colMaxSpan = 0;
        int rowMin = 0;
        int rowMax = 0;
        int rowMaxSpan = 0;

        for (TaffyStyle child : childStyles) {
            ChildMinMaxLineSpan col = childMinMaxLineSpan(child.gridColumn, explicitColumnCount);
            ChildMinMaxLineSpan row = childMinMaxLineSpan(child.gridRow, explicitRowCount);

            colMin = Math.min(colMin, col.minLine);
            colMax = Math.max(colMax, col.maxLine);
            colMaxSpan = Math.max(colMaxSpan, col.span);

            rowMin = Math.min(rowMin, row.minLine);
            rowMax = Math.max(rowMax, row.maxLine);
            rowMaxSpan = Math.max(rowMaxSpan, row.span);
        }

        int negativeImplicitCols = colMin < 0 ? -colMin : 0;
        int positiveImplicitCols = impliedPositiveImplicitTracks(colMax, explicitColumnCount);
        int totCols = negativeImplicitCols + explicitColumnCount + positiveImplicitCols;
        if (totCols < colMaxSpan) {
            positiveImplicitCols = colMaxSpan - explicitColumnCount - negativeImplicitCols;
        }

        int negativeImplicitRows = rowMin < 0 ? -rowMin : 0;
        int positiveImplicitRows = impliedPositiveImplicitTracks(rowMax, explicitRowCount);
        int totRows = negativeImplicitRows + explicitRowCount + positiveImplicitRows;
        if (totRows < rowMaxSpan) {
            positiveImplicitRows = rowMaxSpan - explicitRowCount - negativeImplicitRows;
        }

        TrackCounts cols = new TrackCounts(negativeImplicitCols, explicitColumnCount, positiveImplicitCols);
        TrackCounts rows = new TrackCounts(negativeImplicitRows, explicitRowCount, positiveImplicitRows);
        return new GridSizeEstimate(cols, rows);
    }

    // ---- explicit grid track initialisation helper (tests only) ----

    public enum DebugGridTrackKind {
        TRACK,
        GUTTER
    }

    public static final class DebugGridTrack {
        public final DebugGridTrackKind kind;
        public final TrackSizingFunction min;
        public final TrackSizingFunction max;

        DebugGridTrack(DebugGridTrackKind kind, TrackSizingFunction min, TrackSizingFunction max) {
            this.kind = kind;
            this.min = min;
            this.max = max;
        }
    }

    /**
     * Port of Rust initialize_grid_tracks (tests only). Produces a sequence of tracks and gutters.
     */
    public static List<DebugGridTrack> initializeGridTracks(
            TrackCounts counts,
            TaffyStyle containerStyle,
            boolean horizontalAxis,
            java.util.function.IntPredicate trackHasItems) {

        List<DebugGridTrack> tracks = new ArrayList<>();

        List<GridTemplateComponent> trackTemplate = getTemplate(containerStyle, horizontalAxis);
        List<TrackSizingFunction> autoTracks = horizontalAxis
                ? containerStyle.getGridAutoColumns()
                : containerStyle.getGridAutoRows();
        LengthPercentage gap = horizontalAxis
                ? containerStyle.getGap().width
                : containerStyle.getGap().height;

        // Initial gutter
        tracks.add(gutter(gap));

        int autoTrackCount = autoTracks != null ? autoTracks.size() : 0;

        // Negative implicit tracks
        if (counts.negativeImplicit > 0) {
            if (autoTrackCount == 0) {
                createImplicitTracks(tracks, counts.negativeImplicit, null, 0, gap);
            } else {
                int mod = counts.negativeImplicit % autoTrackCount;
                int offset = autoTrackCount - mod;
                if (offset == autoTrackCount) offset = 0;
                createImplicitTracks(tracks, counts.negativeImplicit, autoTracks, offset, gap);
            }
        }

        int currentTrackIndex = counts.negativeImplicit;

        // Explicit tracks
        if (counts.explicit > 0 && trackTemplate != null) {
            for (int defIndex = 0; defIndex < trackTemplate.size(); defIndex++) {
                GridTemplateComponent def = trackTemplate.get(defIndex);
                if (def == null) continue;

                if (def.isSingle()) {
                    TrackSizingFunction tsf = def.getSingle();
                    tracks.add(track(minSizingFunction(tsf), maxSizingFunction(tsf)));
                    tracks.add(gutter(gap));
                    currentTrackIndex += 1;
                } else {
                    GridRepetition rep = def.getRepeat();
                    if (rep == null) continue;

                    if (rep.getType() == GridRepetition.RepetitionType.COUNT) {
                        int count = rep.getCount();
                        List<TrackSizingFunction> repTracks = rep.getTracks();
                        int repTrackCount = repTracks.size();
                        int total = repTrackCount * count;
                        for (int i = 0; i < total; i++) {
                            TrackSizingFunction tsf = repTracks.get(i % repTrackCount);
                            tracks.add(track(minSizingFunction(tsf), maxSizingFunction(tsf)));
                            tracks.add(gutter(gap));
                            currentTrackIndex += 1;
                        }
                    } else {
                        // Auto-fit/auto-fill: explicit track count includes the expanded auto-repeated tracks.
                        // Rust computes: auto_repeated_track_count = counts.explicit - (track_template.len() - 1)
                        // (where track_template.len includes the auto-repeat component).
                        int autoRepeatedTrackCount = counts.explicit - (trackTemplate.size() - 1);
                        List<TrackSizingFunction> repTracks = rep.getTracks();
                        int repTrackCount = repTracks.size();
                        for (int i = 0; i < autoRepeatedTrackCount; i++) {
                            TrackSizingFunction tsf = repTracks.get(i % repTrackCount);
                            DebugGridTrack t = track(minSizingFunction(tsf), maxSizingFunction(tsf));
                            DebugGridTrack g = gutter(gap);

                            if (rep.getType() == GridRepetition.RepetitionType.AUTO_FIT) {
                                // Auto-fit tracks that don't contain items are collapsed
                                if (trackHasItems != null && !trackHasItems.test(currentTrackIndex)) {
                                    t = collapsedTrack();
                                    g = collapsedGutter();
                                }
                            }

                            tracks.add(t);
                            tracks.add(g);
                            currentTrackIndex += 1;
                        }
                    }
                }
            }
        }

        int gridAreaTracks = (counts.negativeImplicit + counts.explicit) - currentTrackIndex;
        if (gridAreaTracks < 0) gridAreaTracks = 0;

        // Positive implicit tracks
        int positiveCount = counts.positiveImplicit + gridAreaTracks;
        if (positiveCount > 0) {
            if (autoTrackCount == 0) {
                createImplicitTracks(tracks, positiveCount, null, 0, gap);
            } else {
                createImplicitTracks(tracks, positiveCount, autoTracks, 0, gap);
            }
        }

        // Collapse first and last grid lines
        if (!tracks.isEmpty()) {
            tracks.set(0, collapsedGutter());
            tracks.set(tracks.size() - 1, collapsedGutter());
        }

        return tracks;
    }

    private static void createImplicitTracks(
            List<DebugGridTrack> out,
            int count,
            List<TrackSizingFunction> autoTracks,
            int startOffset,
            LengthPercentage gap) {
        for (int i = 0; i < count; i++) {
            TrackSizingFunction def;
            if (autoTracks == null || autoTracks.isEmpty()) {
                def = TrackSizingFunction.auto();
            } else {
                def = autoTracks.get((startOffset + i) % autoTracks.size());
            }
            out.add(track(minSizingFunction(def), maxSizingFunction(def)));
            out.add(gutter(gap));
        }
    }

    private static DebugGridTrack gutter(LengthPercentage gap) {
        TrackSizingFunction v = TrackSizingFunction.fixed(gap != null ? gap : LengthPercentage.ZERO);
        return new DebugGridTrack(DebugGridTrackKind.GUTTER, v, v);
    }

    private static DebugGridTrack track(TrackSizingFunction min, TrackSizingFunction max) {
        return new DebugGridTrack(DebugGridTrackKind.TRACK, min, max);
    }

    private static DebugGridTrack collapsedGutter() {
        TrackSizingFunction z = TrackSizingFunction.fixed(LengthPercentage.ZERO);
        return new DebugGridTrack(DebugGridTrackKind.GUTTER, z, z);
    }

    private static DebugGridTrack collapsedTrack() {
        TrackSizingFunction z = TrackSizingFunction.fixed(LengthPercentage.ZERO);
        return new DebugGridTrack(DebugGridTrackKind.TRACK, z, z);
    }

    private static TrackSizingFunction minSizingFunction(TrackSizingFunction f) {
        if (f == null) return TrackSizingFunction.auto();
        if (f.isMinmax()) {
            TrackSizingFunction min = f.getMinFunc();
            return min != null ? min : TrackSizingFunction.auto();
        }
        if (f.isFr()) {
            return TrackSizingFunction.auto();
        }
        return f;
    }

    private static TrackSizingFunction maxSizingFunction(TrackSizingFunction f) {
        if (f == null) return TrackSizingFunction.auto();
        if (f.isMinmax()) {
            TrackSizingFunction max = f.getMaxFunc();
            return max != null ? max : TrackSizingFunction.auto();
        }
        return f;
    }

    private static int impliedPositiveImplicitTracks(int maxLine, int explicitTrackCount) {
        // maxLine is an origin-zero line, explicitTrackCount is tracks. explicit end line is explicitTrackCount.
        if (maxLine > explicitTrackCount) {
            return maxLine - explicitTrackCount;
        }
        return 0;
    }

    // ---- OriginZero placement conversion used by test-only sizing estimate ----

    private static final class OriginZeroPlacement {
        enum Type { AUTO, LINE, SPAN }

        final Type startType;
        final int startValue;
        final Type endType;
        final int endValue;

        OriginZeroPlacement(Type startType, int startValue, Type endType, int endValue) {
            this.startType = startType;
            this.startValue = startValue;
            this.endType = endType;
            this.endValue = endValue;
        }

        int indefiniteSpan() {
            int startSpan = (startType == Type.SPAN) ? startValue : 1;
            int endSpan = (endType == Type.SPAN) ? endValue : 1;
            // Rust rule: if two spans, remove end span; if only span for named line, becomes span 1.
            // Our GridPlacement has no named span distinction, so match the basic behavior.
            return startType == Type.SPAN ? startSpan : endSpan;
        }
    }

    private static OriginZeroPlacement intoOriginZeroIgnoringNamed(TaffyLine<GridPlacement> line, int explicitTrackCount) {
        GridPlacement start = line.start != null ? line.start : GridPlacement.auto();
        GridPlacement end = line.end != null ? line.end : GridPlacement.auto();

        OriginZeroPlacement.Type startType;
        int startValue;
        if (start.isLine()) {
            Integer resolved = resolveGridLineToOriginZero(start.getLineNumber(), explicitTrackCount);
            if (resolved == null) {
                startType = OriginZeroPlacement.Type.AUTO;
                startValue = 0;
            } else {
                startType = OriginZeroPlacement.Type.LINE;
                startValue = resolved;
            }
        } else if (start.isSpan()) {
            startType = OriginZeroPlacement.Type.SPAN;
            startValue = start.getValue();
        } else {
            startType = OriginZeroPlacement.Type.AUTO;
            startValue = 0;
        }

        OriginZeroPlacement.Type endType;
        int endValue;
        if (end.isLine()) {
            Integer resolved = resolveGridLineToOriginZero(end.getLineNumber(), explicitTrackCount);
            if (resolved == null) {
                endType = OriginZeroPlacement.Type.AUTO;
                endValue = 0;
            } else {
                endType = OriginZeroPlacement.Type.LINE;
                endValue = resolved;
            }
        } else if (end.isSpan()) {
            endType = OriginZeroPlacement.Type.SPAN;
            endValue = end.getValue();
        } else {
            endType = OriginZeroPlacement.Type.AUTO;
            endValue = 0;
        }

        return new OriginZeroPlacement(startType, startValue, endType, endValue);
    }

    private static Integer resolveGridLineToOriginZero(int lineNumber, int explicitTrackCount) {
        if (lineNumber == 0) {
            return null;
        } else if (lineNumber > 0) {
            return lineNumber - 1;
        } else {
            int explicitLineCount = explicitTrackCount + 1;
            return explicitLineCount + lineNumber;
        }
    }
}
