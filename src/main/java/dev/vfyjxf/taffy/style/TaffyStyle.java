package dev.vfyjxf.taffy.style;

import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import java.util.ArrayList;
import java.util.List;

/**
 * A typed representation of CSS style properties.
 * This is the primary input to layout computations.
 */
public final class TaffyStyle {
    
    // === Display and Box Model ===
    
    /** What layout strategy should be used? */
    public TaffyDisplay display = TaffyDisplay.DEFAULT;
    
    /** 
     * The text direction (LTR or RTL) for this element.
     * Default is INHERIT which inherits from parent, or LTR if root.
     * @see <a href="https://www.w3.org/TR/css-writing-modes-3/#direction">CSS direction property</a>
     */
    public TaffyDirection direction = TaffyDirection.INHERIT;
    
    /** Whether a child is display:table or not */
    public boolean itemIsTable = false;
    
    /** Is it a replaced element like an image or form field? */
    public boolean itemIsReplaced = false;
    
    /** Should size styles apply to the content box or the border box */
    public BoxSizing boxSizing = BoxSizing.BORDER_BOX;
    
    // === Overflow Properties ===
    
    /** How children overflowing their container should affect layout */
    public TaffyPoint<Overflow> overflow = new TaffyPoint<>(Overflow.VISIBLE, Overflow.VISIBLE);
    
    /** How much space should be reserved for scrollbars */
    public float scrollbarWidth = 0;
    
    // === Position Properties ===
    
    /** What should the position value use as a base offset? */
    public TaffyPosition position = TaffyPosition.RELATIVE;
    
    /** How should the position of this element be tweaked relative to the layout defined? */
    public TaffyRect<LengthPercentageAuto> inset = TaffyRect.all(LengthPercentageAuto.AUTO);
    
    // === Size Properties ===
    
    /** Sets the initial size of the item */
    public TaffySize<TaffyDimension> size = TaffySize.all(TaffyDimension.AUTO);
    
    /** Controls the minimum size of the item */
    public TaffySize<TaffyDimension> minSize = TaffySize.all(TaffyDimension.AUTO);
    
    /** Controls the maximum size of the item */
    public TaffySize<TaffyDimension> maxSize = TaffySize.all(TaffyDimension.AUTO);
    
    /** Sets the preferred aspect ratio for the item (width / height) */
    public Float aspectRatio = null;
    
    // === Spacing Properties ===
    
    /** How large should the margin be on each side? */
    public TaffyRect<LengthPercentageAuto> margin = TaffyRect.all(LengthPercentageAuto.ZERO);
    
    /** How large should the padding be on each side? */
    public TaffyRect<LengthPercentage> padding = TaffyRect.all(LengthPercentage.ZERO);
    
    /** How large should the border be on each side? */
    public TaffyRect<LengthPercentage> border = TaffyRect.all(LengthPercentage.ZERO);
    
    // === Alignment Properties ===
    
    /** How this node's children aligned in the cross/block axis? */
    public AlignItems alignItems = null;
    
    /** How this node should be aligned in the cross/block axis */
    public AlignItems alignSelf = null;
    
    /** How this node's children should be aligned in the inline axis */
    public AlignItems justifyItems = null;
    
    /** How this node should be aligned in the inline axis */
    public AlignItems justifySelf = null;
    
    /** How should content contained within this item be aligned in the cross/block axis */
    public AlignContent alignContent = null;
    
    /** How should content contained within this item be aligned in the main/inline axis */
    public AlignContent justifyContent = null;
    
    /** How large should the gaps between items in a grid or flex container be? */
    public TaffySize<LengthPercentage> gap = TaffySize.all(LengthPercentage.ZERO);
    
    // === Block Container Properties ===
    
    /** How items elements should aligned in the inline axis */
    public TextAlign textAlign = TextAlign.AUTO;

    // === Flexbox Container Properties ===
    
    /** Which direction does the main axis flow in? */
    public FlexDirection flexDirection = FlexDirection.ROW;
    
    /** Should elements wrap, or stay in a single line? */
    public FlexWrap flexWrap = FlexWrap.NO_WRAP;
    
    // === Flexbox Item Properties ===

    /** The relative rate at which this item grows when it is expanding to fill space */
    public float flexGrow = 0.0f;

    /** The relative rate at which this item shrinks when it is contracting to fit into space */
    public float flexShrink = 1.0f;

    /** Sets the initial main axis size of the item */
    public TaffyDimension flexBasis = TaffyDimension.AUTO;

    // === Flex Shorthand Methods ===
    
    /**
     * CSS flex shorthand: flex: none
     * Equivalent to flex: 0 0 auto
     * The item is sized according to its width/height properties, fully inflexible.
     */
    public TaffyStyle flexNone() {
        this.flexGrow = 0.0f;
        this.flexShrink = 0.0f;
        this.flexBasis = TaffyDimension.AUTO;
        return this;
    }
    
    /**
     * CSS flex shorthand: flex: auto
     * Equivalent to flex: 1 1 auto
     * The item is sized according to its width/height properties, but grows/shrinks to fill available space.
     */
    public TaffyStyle flexAuto() {
        this.flexGrow = 1.0f;
        this.flexShrink = 1.0f;
        this.flexBasis = TaffyDimension.AUTO;
        return this;
    }
    
    /**
     * CSS flex shorthand: flex: initial
     * Equivalent to flex: 0 1 auto (the default values)
     * The item is sized according to its width/height properties, can shrink but won't grow.
     */
    public TaffyStyle flexInitial() {
        this.flexGrow = 0.0f;
        this.flexShrink = 1.0f;
        this.flexBasis = TaffyDimension.AUTO;
        return this;
    }
    
    /**
     * CSS flex shorthand: flex: &lt;number&gt;
     * Equivalent to flex: &lt;number&gt; 1 0
     * The item receives the specified proportion of free space, with flex-basis of 0.
     * @param grow the flex-grow value
     */
    public TaffyStyle flex(float grow) {
        this.flexGrow = grow;
        this.flexShrink = 1.0f;
        this.flexBasis = TaffyDimension.ZERO;
        return this;
    }
    
    /**
     * CSS flex shorthand: flex: &lt;grow&gt; &lt;shrink&gt;
     * Equivalent to flex: &lt;grow&gt; &lt;shrink&gt; 0
     * @param grow the flex-grow value
     * @param shrink the flex-shrink value
     */
    public TaffyStyle flex(float grow, float shrink) {
        this.flexGrow = grow;
        this.flexShrink = shrink;
        this.flexBasis = TaffyDimension.ZERO;
        return this;
    }
    
    /**
     * CSS flex shorthand: flex: &lt;grow&gt; &lt;shrink&gt; &lt;basis&gt;
     * @param grow the flex-grow value
     * @param shrink the flex-shrink value
     * @param basis the flex-basis value
     */
    public TaffyStyle flex(float grow, float shrink, TaffyDimension basis) {
        this.flexGrow = grow;
        this.flexShrink = shrink;
        this.flexBasis = basis;
        return this;
    }
    
    /**
     * CSS flex shorthand: flex: &lt;grow&gt; &lt;shrink&gt; &lt;basis-length&gt;
     * @param grow the flex-grow value
     * @param shrink the flex-shrink value
     * @param basisLength the flex-basis as a length value in pixels
     */
    public TaffyStyle flex(float grow, float shrink, float basisLength) {
        this.flexGrow = grow;
        this.flexShrink = shrink;
        this.flexBasis = TaffyDimension.length(basisLength);
        return this;
    }
    
    // === Grid Container Properties ===
    
    /** Defines the track sizing functions (heights) of the grid rows */
    public List<TrackSizingFunction> gridTemplateRows = new ArrayList<>();
    
    /** Defines the track sizing functions (widths) of the grid columns */
    public List<TrackSizingFunction> gridTemplateColumns = new ArrayList<>();
    
    /** 
     * Defines the track template with support for repeat(auto-fill/auto-fit, ...).
     * If set, this takes precedence over gridTemplateRows for layout computation.
     */
    public List<GridTemplateComponent> gridTemplateRowsWithRepeat = null;
    
    /** 
     * Defines the track template with support for repeat(auto-fill/auto-fit, ...).
     * If set, this takes precedence over gridTemplateColumns for layout computation.
     */
    public List<GridTemplateComponent> gridTemplateColumnsWithRepeat = null;
    
    /**
     * Defines named grid areas in the grid template.
     * Each area specifies a name and the row/column lines that bound it.
     * <p>
     * Rust equivalent: grid_template_areas: Vec&lt;GridTemplateArea&gt;
     */
    public List<GridTemplateArea> gridTemplateAreas = new ArrayList<>();
    
    /**
     * Defines named lines for grid columns.
     * Each entry maps a line name to a line index.
     * Multiple entries with the same name create multiple named lines.
     * <p>
     * Rust equivalent: grid_template_column_names: Vec&lt;Vec&lt;String&gt;&gt;
     * (but flattened to List&lt;NamedGridLine&gt; for simplicity)
     */
    public List<NamedGridLine> gridTemplateColumnNames = new ArrayList<>();
    
    /**
     * Defines named lines for grid rows.
     * Each entry maps a line name to a line index.
     * Multiple entries with the same name create multiple named lines.
     * <p>
     * Rust equivalent: grid_template_row_names: Vec&lt;Vec&lt;String&gt;&gt;
     * (but flattened to List&lt;NamedGridLine&gt; for simplicity)
     */
    public List<NamedGridLine> gridTemplateRowNames = new ArrayList<>();
    
    /** Defines the size of implicitly created rows */
    public List<TrackSizingFunction> gridAutoRows = new ArrayList<>();
    
    /** Defines the size of implicitly created columns */
    public List<TrackSizingFunction> gridAutoColumns = new ArrayList<>();
    
    /** Controls how items get placed into the grid for auto-placed items */
    public GridAutoFlow gridAutoFlow = GridAutoFlow.ROW;
    
    // === Grid Child Properties ===
    
    /** Defines which row in the grid the item should start and end at */
    public TaffyLine<GridPlacement> gridRow = new TaffyLine<>(GridPlacement.AUTO_INSTANCE, GridPlacement.AUTO_INSTANCE);
    
    /** Defines which column in the grid the item should start and end at */
    public TaffyLine<GridPlacement> gridColumn = new TaffyLine<>(GridPlacement.AUTO_INSTANCE, GridPlacement.AUTO_INSTANCE);
    
    /**
     * Creates a new Style with default values
     */
    public TaffyStyle() {
    }
    
    /**
     * Creates a deep copy of this Style
     */
    public TaffyStyle copy() {
        TaffyStyle copy = new TaffyStyle();
        copy.display = this.display;
        copy.itemIsTable = this.itemIsTable;
        copy.itemIsReplaced = this.itemIsReplaced;
        copy.boxSizing = this.boxSizing;
        copy.overflow = this.overflow.copy();
        copy.scrollbarWidth = this.scrollbarWidth;
        copy.position = this.position;
        copy.inset = this.inset.copy();
        copy.size = this.size.copy();
        copy.minSize = this.minSize.copy();
        copy.maxSize = this.maxSize.copy();
        copy.aspectRatio = this.aspectRatio;
        copy.margin = this.margin.copy();
        copy.padding = this.padding.copy();
        copy.border = this.border.copy();
        copy.alignItems = this.alignItems;
        copy.alignSelf = this.alignSelf;
        copy.justifyItems = this.justifyItems;
        copy.justifySelf = this.justifySelf;
        copy.alignContent = this.alignContent;
        copy.justifyContent = this.justifyContent;
        copy.gap = this.gap.copy();
        copy.textAlign = this.textAlign;
        copy.direction = this.direction;
        copy.flexDirection = this.flexDirection;
        copy.flexWrap = this.flexWrap;
        copy.flexBasis = this.flexBasis;
        copy.flexGrow = this.flexGrow;
        copy.flexShrink = this.flexShrink;
        copy.gridTemplateRows = new ArrayList<>(this.gridTemplateRows);
        copy.gridTemplateColumns = new ArrayList<>(this.gridTemplateColumns);
        copy.gridTemplateRowsWithRepeat = this.gridTemplateRowsWithRepeat != null 
            ? new ArrayList<>(this.gridTemplateRowsWithRepeat) : null;
        copy.gridTemplateColumnsWithRepeat = this.gridTemplateColumnsWithRepeat != null
            ? new ArrayList<>(this.gridTemplateColumnsWithRepeat) : null;
        copy.gridAutoRows = new ArrayList<>(this.gridAutoRows);
        copy.gridAutoColumns = new ArrayList<>(this.gridAutoColumns);
        copy.gridAutoFlow = this.gridAutoFlow;
        copy.gridTemplateAreas = new ArrayList<>(this.gridTemplateAreas);
        copy.gridTemplateColumnNames = new ArrayList<>(this.gridTemplateColumnNames);
        copy.gridTemplateRowNames = new ArrayList<>(this.gridTemplateRowNames);
        copy.gridRow = this.gridRow.copy();
        copy.gridColumn = this.gridColumn.copy();
        return copy;
    }
    
    /**
     * Returns the BoxGenerationMode based on the display property
     */
    public BoxGenerationMode boxGenerationMode() {
        return display == TaffyDisplay.NONE ? BoxGenerationMode.NONE : BoxGenerationMode.NORMAL;
    }
    
    /**
     * Returns true if this is block layout
     */
    public boolean isBlock() {
        return display == TaffyDisplay.BLOCK;
    }
    
    // === Getter methods ===
    
    public TaffyDisplay getDisplay() { return display; }
    public TaffyDirection getDirection() { return direction; }
    public boolean getItemIsTable() { return itemIsTable; }
    public boolean getItemIsReplaced() { return itemIsReplaced; }
    public BoxSizing getBoxSizing() { return boxSizing; }
    public TaffyPoint<Overflow> getOverflow() { return overflow; }
    public float getScrollbarWidth() { return scrollbarWidth; }
    public TaffyPosition getPosition() { return position; }
    public TaffyRect<LengthPercentageAuto> getInset() { return inset; }
    public TaffySize<TaffyDimension> getSize() { return size; }
    public TaffySize<TaffyDimension> getMinSize() { return minSize; }
    public TaffySize<TaffyDimension> getMaxSize() { return maxSize; }
    public Float getAspectRatio() { return aspectRatio; }
    public TaffyRect<LengthPercentageAuto> getMargin() { return margin; }
    public TaffyRect<LengthPercentage> getPadding() { return padding; }
    public TaffyRect<LengthPercentage> getBorder() { return border; }
    public AlignItems getAlignItems() { return alignItems; }
    public AlignItems getAlignSelf() { return alignSelf; }
    public AlignContent getAlignContent() { return alignContent; }
    public JustifyContent getJustifyContent() {
        if (justifyContent == null) return JustifyContent.FLEX_START;
        return switch (justifyContent) {
            case FLEX_START -> JustifyContent.FLEX_START;
            case FLEX_END -> JustifyContent.FLEX_END;
            case CENTER -> JustifyContent.CENTER;
            case SPACE_BETWEEN -> JustifyContent.SPACE_BETWEEN;
            case SPACE_AROUND -> JustifyContent.SPACE_AROUND;
            case SPACE_EVENLY -> JustifyContent.SPACE_EVENLY;
            case START -> JustifyContent.START;
            case END -> JustifyContent.END;
            case STRETCH -> JustifyContent.STRETCH;
        };
    }
    public TaffySize<LengthPercentage> getGap() { return gap; }
    public TextAlign getTextAlign() { return textAlign; }
    public FlexDirection getFlexDirection() { return flexDirection; }
    public FlexWrap getFlexWrap() { return flexWrap; }
    public TaffyDimension getFlexBasis() { return flexBasis; }
    public float getFlexGrow() { return flexGrow; }
    public float getFlexShrink() { return flexShrink; }
    public List<TrackSizingFunction> getGridTemplateRows() { return gridTemplateRows; }
    public List<TrackSizingFunction> getGridTemplateColumns() { return gridTemplateColumns; }
    public List<TrackSizingFunction> getGridAutoRows() { return gridAutoRows; }
    public List<TrackSizingFunction> getGridAutoColumns() { return gridAutoColumns; }
    public GridAutoFlow getGridAutoFlow() { return gridAutoFlow; }
    public GridPlacement getGridRowStart() { return gridRow.start; }
    public GridPlacement getGridRowEnd() { return gridRow.end; }
    public GridPlacement getGridColumnStart() { return gridColumn.start; }
    public GridPlacement getGridColumnEnd() { return gridColumn.end; }
    public BoxGenerationMode getBoxGenerationMode() { return boxGenerationMode(); }
    
    @Override
    public String toString() {
        return "Style{display=" + display + ", position=" + position + 
               ", flexDirection=" + flexDirection + ", flexWrap=" + flexWrap + "}";
    }
}
