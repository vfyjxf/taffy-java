package dev.vfyjxf.taffy.style;

/**
 * Represents a component in a grid-template-rows or grid-template-columns definition.
 * Can be either a single TrackSizingFunction or a repeat() function.
 */
public class GridTemplateComponent {
    
    public enum Type {
        /** A single track sizing function */
        SINGLE,
        /** A repeat() function */
        REPEAT
    }
    
    private final Type type;
    private final TrackSizingFunction single;
    private final GridRepetition repeat;
    
    private GridTemplateComponent(Type type, TrackSizingFunction single, GridRepetition repeat) {
        this.type = type;
        this.single = single;
        this.repeat = repeat;
    }
    
    /**
     * Creates a single track component
     */
    public static GridTemplateComponent single(TrackSizingFunction track) {
        return new GridTemplateComponent(Type.SINGLE, track, null);
    }
    
    /**
     * Creates a repeat component
     */
    public static GridTemplateComponent repeat(GridRepetition repeat) {
        return new GridTemplateComponent(Type.REPEAT, null, repeat);
    }
    
    /**
     * Creates a repeat component with a specific count
     */
    public static GridTemplateComponent repeatCount(int count, TrackSizingFunction... tracks) {
        java.util.List<TrackSizingFunction> trackList = java.util.Arrays.asList(tracks);
        return new GridTemplateComponent(Type.REPEAT, null, GridRepetition.count(count, trackList));
    }
    
    /**
     * Creates an auto-fill repeat component
     */
    public static GridTemplateComponent autoFill(TrackSizingFunction... tracks) {
        java.util.List<TrackSizingFunction> trackList = java.util.Arrays.asList(tracks);
        return new GridTemplateComponent(Type.REPEAT, null, GridRepetition.autoFill(trackList));
    }
    
    /**
     * Creates an auto-fit repeat component
     */
    public static GridTemplateComponent autoFit(TrackSizingFunction... tracks) {
        java.util.List<TrackSizingFunction> trackList = java.util.Arrays.asList(tracks);
        return new GridTemplateComponent(Type.REPEAT, null, GridRepetition.autoFit(trackList));
    }
    
    public Type getType() {
        return type;
    }
    
    public boolean isSingle() {
        return type == Type.SINGLE;
    }
    
    public boolean isRepeat() {
        return type == Type.REPEAT;
    }
    
    public TrackSizingFunction getSingle() {
        return single;
    }
    
    public GridRepetition getRepeat() {
        return repeat;
    }
    
    /**
     * Checks if this is an auto-repetition (auto-fill or auto-fit)
     */
    public boolean isAutoRepetition() {
        return type == Type.REPEAT && repeat != null && repeat.isAutoRepetition();
    }
    
    /**
     * Checks if this component has a fixed size component.
     * For single tracks, delegates to TrackSizingFunction.hasFixedComponent().
     * For repeats, checks if all tracks have fixed components.
     */
    public boolean hasFixedComponent() {
        if (type == Type.SINGLE) {
            return single != null && single.hasFixedComponent();
        } else {
            return repeat != null && repeat.hasFixedComponent();
        }
    }
}
