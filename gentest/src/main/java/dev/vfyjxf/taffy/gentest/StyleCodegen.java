package dev.vfyjxf.taffy.gentest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared model and Java code emitter for {@code TaffyStyle} values.
 *
 * <p>The emission order and the literal formatting here are load-bearing: the
 * committed tests under {@code src/test/java/dev/vfyjxf/taffy/generated} were
 * produced by this order and {@code --check} compares byte for byte.</p>
 */
final class StyleCodegen {

    private StyleCodegen() {}

    /** A normalized style of one Taffy node. */
    static final class NodeStyle {
        String boxSizing;          // BORDER_BOX / CONTENT_BOX
        String direction;          // LTR / RTL
        String display;            // NONE / BLOCK / GRID (FLEX is the default, omit)
        String position;           // ABSOLUTE (static/relative are defaults, omit)
        String floatSide;          // TaffyFloat LEFT/RIGHT (none is the default, omit)
        String clear;              // Clear LEFT/RIGHT/BOTH (none is the default, omit)

        String flexDirection;
        String flexWrap;
        String alignItems;
        String alignSelf;
        String alignContent;
        String justifyContent;
        String textAlign;
        String justifyItems;
        String justifySelf;

        Float flexGrow;
        Float flexShrink;
        String flexBasis;

        Size size;
        Size minSize;
        Size maxSize;
        Float aspectRatio;
        /** Replaced element flag: consumed by block/flex/grid sizing in the engine. */
        boolean itemIsReplaced;
        /** Intrinsic size of a replaced leaf; emitted as an {@code itemIsReplaced} measure function. */
        Float intrinsicWidth;
        Float intrinsicHeight;
        Size gap;

        Edges margin;
        Edges padding;
        Edges border;
        Edges inset;

        GridTracks gridTemplateColumns;
        GridTracks gridTemplateRows;
        GridTracks gridAutoColumns;
        GridTracks gridAutoRows;
        String gridAutoFlow;
        GridLine gridRow;
        GridLine gridColumn;

        Overflow overflow;
    }

    /** width/height pair of TaffyDimension/LengthPercentage expressions. */
    record Size(String width, String height) {}

    record Edges(String left, String right, String top, String bottom, EdgeKind kind) {
        boolean isEmpty() {
            return left == null && right == null && top == null && bottom == null;
        }
    }

    enum EdgeKind {
        LENGTH_PERCENTAGE("LengthPercentage", "ZERO"),
        LENGTH_PERCENTAGE_AUTO("LengthPercentageAuto", "AUTO"),
        /** Margin: LengthPercentageAuto values but the historical generator defaults to ZERO. */
        LENGTH_PERCENTAGE_AUTO_ZERO("LengthPercentageAuto", "ZERO");

        final String type;
        final String defaultValue;

        EdgeKind(String type, String defaultValue) {
            this.type = type;
            this.defaultValue = defaultValue;
        }
    }

    /**
     * One grid track list. When {@code withRepeat} is true the entries are
     * {@code GridTemplateComponent} expressions emitted into the
     * {@code *WithRepeat} field; otherwise they are {@code TrackSizingFunction}
     * expressions emitted into the plain list field.
     */
    static final class GridTracks {
        final boolean withRepeat;
        final boolean initializeList;
        final List<String> entries;

        GridTracks(boolean withRepeat, boolean initializeList, List<String> entries) {
            this.withRepeat = withRepeat;
            this.initializeList = initializeList;
            this.entries = List.copyOf(entries);
        }

        static GridTracks plain(List<String> entries) {
            return new GridTracks(false, false, entries);
        }

        static GridTracks repeat(List<String> entries, boolean initializeList) {
            return new GridTracks(true, initializeList, entries);
        }

        boolean isEmpty() {
            return entries.isEmpty();
        }
    }

    record GridLine(String start, String end) {}

    record Overflow(String x, String y, Float scrollbarWidth) {
        boolean isVisible() {
            return "Overflow.VISIBLE".equals(x) && "Overflow.VISIBLE".equals(y);
        }

        boolean hasScroll() {
            return "Overflow.SCROLL".equals(x) || "Overflow.SCROLL".equals(y);
        }
    }

    /**
     * Emit every style assignment in the historical Taffy generator order.
     */
    static void emit(StringBuilder sb, String styleVar, NodeStyle style, String ind) {
        if (style.boxSizing != null) {
            sb.append(ind).append(styleVar).append(".boxSizing = BoxSizing.").append(style.boxSizing).append(";\n");
        }
        if (style.direction != null) {
            sb.append(ind).append(styleVar).append(".direction = TaffyDirection.").append(style.direction).append(";\n");
        }
        if (style.display != null) {
            sb.append(ind).append(styleVar).append(".display = TaffyDisplay.").append(style.display).append(";\n");
        }
        if (style.position != null) {
            sb.append(ind).append(styleVar).append(".position = TaffyPosition.").append(style.position).append(";\n");
        }
        if (style.floatSide != null) {
            sb.append(ind).append(styleVar).append(".float_ = TaffyFloat.").append(style.floatSide).append(";\n");
        }
        if (style.clear != null) {
            sb.append(ind).append(styleVar).append(".clear = Clear.").append(style.clear).append(";\n");
        }
        if (style.flexDirection != null) {
            sb.append(ind).append(styleVar).append(".flexDirection = FlexDirection.").append(style.flexDirection).append(";\n");
        }
        if (style.flexWrap != null) {
            sb.append(ind).append(styleVar).append(".flexWrap = FlexWrap.").append(style.flexWrap).append(";\n");
        }
        if (style.alignItems != null) {
            sb.append(ind).append(styleVar).append(".alignItems = ").append(style.alignItems).append(";\n");
        }
        if (style.alignSelf != null) {
            sb.append(ind).append(styleVar).append(".alignSelf = ").append(style.alignSelf).append(";\n");
        }
        if (style.alignContent != null) {
            sb.append(ind).append(styleVar).append(".alignContent = ").append(style.alignContent).append(";\n");
        }
        if (style.justifyContent != null) {
            sb.append(ind).append(styleVar).append(".justifyContent = ").append(style.justifyContent).append(";\n");
        }
        if (style.textAlign != null) {
            sb.append(ind).append(styleVar).append(".textAlign = ").append(style.textAlign).append(";\n");
        }
        if (style.justifyItems != null) {
            sb.append(ind).append(styleVar).append(".justifyItems = ").append(style.justifyItems).append(";\n");
        }
        if (style.justifySelf != null) {
            sb.append(ind).append(styleVar).append(".justifySelf = ").append(style.justifySelf).append(";\n");
        }
        if (style.flexGrow != null) {
            sb.append(ind).append(styleVar).append(".flexGrow = ").append(floatLiteral(style.flexGrow)).append(";\n");
        }
        if (style.flexShrink != null) {
            sb.append(ind).append(styleVar).append(".flexShrink = ").append(floatLiteral(style.flexShrink)).append(";\n");
        }
        if (style.flexBasis != null) {
            sb.append(ind).append(styleVar).append(".flexBasis = ").append(style.flexBasis).append(";\n");
        }
        emitSize(sb, styleVar, "size", style.size, ind);
        emitSize(sb, styleVar, "minSize", style.minSize, ind);
        emitSize(sb, styleVar, "maxSize", style.maxSize, ind);
        if (style.aspectRatio != null) {
            sb.append(ind).append(styleVar).append(".aspectRatio = ").append(floatLiteral(style.aspectRatio)).append(";\n");
        }
        if (style.itemIsReplaced) {
            sb.append(ind).append(styleVar).append(".itemIsReplaced = true;\n");
        }
        emitGap(sb, styleVar, style.gap, ind);
        emitEdges(sb, styleVar, "margin", style.margin, ind);
        emitEdges(sb, styleVar, "padding", style.padding, ind);
        emitEdges(sb, styleVar, "border", style.border, ind);
        emitEdges(sb, styleVar, "inset", style.inset, ind);
        emitGridTracks(sb, styleVar, "gridTemplateColumns", style.gridTemplateColumns, ind);
        emitGridTracks(sb, styleVar, "gridTemplateRows", style.gridTemplateRows, ind);
        emitGridTracks(sb, styleVar, "gridAutoColumns", style.gridAutoColumns, ind);
        emitGridTracks(sb, styleVar, "gridAutoRows", style.gridAutoRows, ind);
        if (style.gridAutoFlow != null) {
            sb.append(ind).append(styleVar).append(".gridAutoFlow = GridAutoFlow.").append(style.gridAutoFlow).append(";\n");
        }
        if (style.gridRow != null) {
            sb.append(ind).append(styleVar).append(".gridRow = new TaffyLine<>(")
                    .append(style.gridRow.start).append(", ").append(style.gridRow.end).append(");\n");
        }
        if (style.gridColumn != null) {
            sb.append(ind).append(styleVar).append(".gridColumn = new TaffyLine<>(")
                    .append(style.gridColumn.start).append(", ").append(style.gridColumn.end).append(");\n");
        }
        if (style.overflow != null && !style.overflow.isVisible()) {
            sb.append(ind).append(styleVar).append(".overflow = new TaffyPoint<>(")
                    .append(style.overflow.x).append(", ").append(style.overflow.y).append(");\n");
            if (style.overflow.hasScroll() && style.overflow.scrollbarWidth != null) {
                sb.append(ind).append(styleVar).append(".scrollbarWidth = ")
                        .append(floatLiteral(style.overflow.scrollbarWidth)).append(";\n");
            }
        }
    }

    private static void emitSize(StringBuilder sb, String styleVar, String field, Size size, String ind) {
        if (size == null || (size.width == null && size.height == null)) {
            return;
        }
        sb.append(ind).append(styleVar).append(".").append(field).append(" = new TaffySize<>(")
                .append(size.width != null ? size.width : "TaffyDimension.AUTO").append(", ")
                .append(size.height != null ? size.height : "TaffyDimension.AUTO").append(");\n");
    }

    private static void emitGap(StringBuilder sb, String styleVar, Size gap, String ind) {
        if (gap == null || (gap.width == null && gap.height == null)) {
            return;
        }
        sb.append(ind).append(styleVar).append(".gap = new TaffySize<>(")
                .append(gap.width != null ? gap.width : "LengthPercentage.ZERO").append(", ")
                .append(gap.height != null ? gap.height : "LengthPercentage.ZERO").append(");\n");
    }

    private static void emitEdges(StringBuilder sb, String styleVar, String field, Edges edges, String ind) {
        if (edges == null || edges.isEmpty()) {
            return;
        }
        String defaultValue = edges.kind.type + "." + edges.kind.defaultValue;
        sb.append(ind).append(styleVar).append(".").append(field).append(" = new TaffyRect<>(")
                .append(edges.left != null ? edges.left : defaultValue).append(", ")
                .append(edges.right != null ? edges.right : defaultValue).append(", ")
                .append(edges.top != null ? edges.top : defaultValue).append(", ")
                .append(edges.bottom != null ? edges.bottom : defaultValue).append(");\n");
    }

    private static void emitGridTracks(StringBuilder sb, String styleVar, String field, GridTracks tracks, String ind) {
        if (tracks == null || tracks.isEmpty()) {
            return;
        }
        String target = field;
        if (tracks.withRepeat) {
            if (!field.equals("gridTemplateColumns") && !field.equals("gridTemplateRows")) {
                throw new UnsupportedStyleException(
                        "auto-repeat is not supported for " + field + " by TaffyStyle; " +
                                "expanded single tracks would be required");
            }
            target = field + "WithRepeat";
        }
        if (tracks.withRepeat && tracks.initializeList) {
            sb.append(ind).append(styleVar).append(".").append(target).append(" = new java.util.ArrayList<>();\n");
        }
        for (String entry : tracks.entries) {
            sb.append(ind).append(styleVar).append(".").append(target).append(".add(").append(entry).append(");\n");
        }
    }

    // ---------------------------------------------------------------------
    // CSS -> Java value mappings (shared by the taffy JSON adapter and WPT).
    // ---------------------------------------------------------------------

    static String mapOverflow(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "hidden" -> "Overflow.HIDDEN";
            case "clip" -> "Overflow.CLIP";
            case "scroll" -> "Overflow.SCROLL";
            case "auto" -> "Overflow.AUTO";
            default -> "Overflow.VISIBLE";
        };
    }

    static String mapTextAlign(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "left" -> "TextAlign.LEFT";
            case "right" -> "TextAlign.RIGHT";
            case "center" -> "TextAlign.CENTER";
            case "start" -> "TextAlign.START";
            case "end" -> "TextAlign.END";
            case "justify" -> "TextAlign.JUSTIFY";
            case "-webkit-left" -> "TextAlign.LEFT";
            case "-webkit-right" -> "TextAlign.RIGHT";
            case "-webkit-center" -> "TextAlign.CENTER";
            default -> null;
        };
    }

    static String mapAlignItems(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "start" -> "AlignItems.START";
            case "end" -> "AlignItems.END";
            case "flex-start" -> "AlignItems.FLEX_START";
            case "flex-end" -> "AlignItems.FLEX_END";
            case "center" -> "AlignItems.CENTER";
            case "baseline" -> "AlignItems.BASELINE";
            case "stretch" -> "AlignItems.STRETCH";
            default -> null;
        };
    }

    static String mapAlignSelf(String value) {
        // Style.alignSelf and Style.justifySelf are AlignItems in TaffyStyle.
        return mapAlignItems(value);
    }

    static String mapAlignContent(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "start" -> "AlignContent.START";
            case "end" -> "AlignContent.END";
            case "flex-start" -> "AlignContent.FLEX_START";
            case "flex-end" -> "AlignContent.FLEX_END";
            case "center" -> "AlignContent.CENTER";
            case "stretch" -> "AlignContent.STRETCH";
            case "space-between" -> "AlignContent.SPACE_BETWEEN";
            case "space-around" -> "AlignContent.SPACE_AROUND";
            case "space-evenly" -> "AlignContent.SPACE_EVENLY";
            default -> null;
        };
    }

    static String mapJustifyContent(String value) {
        // TaffyStyle.justifyContent is AlignContent.
        return mapAlignContent(value);
    }

    static String floatLiteral(float value) {
        return Float.toString(value) + "f";
    }

    static String toPascalCase(String input) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : input.toCharArray()) {
            if (c == '_' || c == '-' || c == '/' || c == '.') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    static String toMethodName(String input) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : input.toCharArray()) {
            if (c == '_' || c == '-' || c == '/' || c == '.') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        if (result.length() > 0) {
            result.setCharAt(0, Character.toLowerCase(result.charAt(0)));
        }
        return result.toString();
    }

    static String escapeJavaString(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\u200B' -> out.append("\\u200B");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    static String categoryToPackage(String category) {
        return category.replace('/', '.').replace('\\', '.');
    }

    static List<String> trimToEmpty(List<String> values) {
        List<String> out = new ArrayList<>(values.size());
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                out.add(value.trim());
            }
        }
        return out;
    }

    static String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
}
