package dev.vfyjxf.taffy.style;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a repeat() function in grid-template-rows or grid-template-columns.
 * Used to express: repeat(count, track-list) where count can be:
 * - A specific integer (e.g., repeat(3, 1fr))
 * - auto-fill: Fill as many tracks as possible without overflowing
 * - auto-fit: Like auto-fill but collapses empty tracks
 */
public class GridRepetition {
    
    /**
     * The type of repetition count
     */
    public enum RepetitionType {
        /** A specific integer count */
        COUNT,
        /** auto-fill: Fill as many tracks as possible */
        AUTO_FILL,
        /** auto-fit: Like auto-fill but collapses empty tracks */
        AUTO_FIT
    }
    
    private final RepetitionType type;
    private final int count;
    private final List<TrackSizingFunction> tracks;
    
    private GridRepetition(RepetitionType type, int count, List<TrackSizingFunction> tracks) {
        this.type = type;
        this.count = count;
        this.tracks = tracks;
    }
    
    /**
     * Creates a repeat with a specific count
     */
    public static GridRepetition count(int count, List<TrackSizingFunction> tracks) {
        return new GridRepetition(RepetitionType.COUNT, count, new ArrayList<>(tracks));
    }
    
    /**
     * Creates a repeat with a specific count and a single track
     */
    public static GridRepetition count(int count, TrackSizingFunction track) {
        List<TrackSizingFunction> tracks = new ArrayList<>();
        tracks.add(track);
        return new GridRepetition(RepetitionType.COUNT, count, tracks);
    }
    
    /**
     * Creates an auto-fill repeat
     */
    public static GridRepetition autoFill(List<TrackSizingFunction> tracks) {
        return new GridRepetition(RepetitionType.AUTO_FILL, 0, new ArrayList<>(tracks));
    }
    
    /**
     * Creates an auto-fill repeat with a single track
     */
    public static GridRepetition autoFill(TrackSizingFunction track) {
        List<TrackSizingFunction> tracks = new ArrayList<>();
        tracks.add(track);
        return new GridRepetition(RepetitionType.AUTO_FILL, 0, tracks);
    }
    
    /**
     * Creates an auto-fit repeat
     */
    public static GridRepetition autoFit(List<TrackSizingFunction> tracks) {
        return new GridRepetition(RepetitionType.AUTO_FIT, 0, new ArrayList<>(tracks));
    }
    
    /**
     * Creates an auto-fit repeat with a single track
     */
    public static GridRepetition autoFit(TrackSizingFunction track) {
        List<TrackSizingFunction> tracks = new ArrayList<>();
        tracks.add(track);
        return new GridRepetition(RepetitionType.AUTO_FIT, 0, tracks);
    }
    
    public RepetitionType getType() {
        return type;
    }
    
    public int getCount() {
        return count;
    }
    
    public List<TrackSizingFunction> getTracks() {
        return tracks;
    }
    
    public int getTrackCount() {
        return tracks.size();
    }
    
    public boolean isAutoRepetition() {
        return type == RepetitionType.AUTO_FILL || type == RepetitionType.AUTO_FIT;
    }
    
    /**
     * Checks if all tracks in this repetition have a fixed component.
     * Required for auto-fill/auto-fit to work correctly.
     */
    public boolean hasFixedComponent() {
        return tracks.stream().allMatch(TrackSizingFunction::hasFixedComponent);
    }
}
