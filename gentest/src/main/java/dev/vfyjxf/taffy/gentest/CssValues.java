package dev.vfyjxf.taffy.gentest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared CSS value parsing helpers.
 *
 * <p>These are used by the WPT source adapter and by the modern taffy helper
 * schema (which reports grid templates as CSS strings).</p>
 */
final class CssValues {

    static final int VIEWPORT_WIDTH = 800;
    static final int VIEWPORT_HEIGHT = 600;

    private CssValues() {}

    enum LengthType {
        PX,
        PERCENT
    }

    record LengthValue(LengthType type, float value) {
        private static final float BASE_FONT_SIZE = 16f;
        private static final float CH_WIDTH = 8f;

        static LengthValue parse(String raw) {
            if (raw == null) {
                return null;
            }
            String v = raw.trim().toLowerCase(Locale.ROOT);
            if (v.isEmpty()) {
                return null;
            }
            if ("0".equals(v)) {
                return new LengthValue(LengthType.PX, 0f);
            }
            if (v.startsWith("calc(") && v.endsWith(")")) {
                return parseCalc(v.substring(5, v.length() - 1));
            }
            if (v.endsWith("px")) {
                return new LengthValue(LengthType.PX, parseFloatSafe(v.substring(0, v.length() - 2)));
            }
            if (v.endsWith("%")) {
                return new LengthValue(LengthType.PERCENT, parseFloatSafe(v.substring(0, v.length() - 1)));
            }
            if (v.endsWith("em") && !v.endsWith("rem")) {
                return new LengthValue(LengthType.PX, parseFloatSafe(v.substring(0, v.length() - 2)) * BASE_FONT_SIZE);
            }
            if (v.endsWith("rem")) {
                return new LengthValue(LengthType.PX, parseFloatSafe(v.substring(0, v.length() - 3)) * BASE_FONT_SIZE);
            }
            if (v.endsWith("ch")) {
                return new LengthValue(LengthType.PX, parseFloatSafe(v.substring(0, v.length() - 2)) * CH_WIDTH);
            }
            if (v.endsWith("vw")) {
                return new LengthValue(LengthType.PX, parseFloatSafe(v.substring(0, v.length() - 2)) * VIEWPORT_WIDTH / 100f);
            }
            if (v.endsWith("vh")) {
                return new LengthValue(LengthType.PX, parseFloatSafe(v.substring(0, v.length() - 2)) * VIEWPORT_HEIGHT / 100f);
            }
            if (v.endsWith("vmin")) {
                return new LengthValue(LengthType.PX, parseFloatSafe(v.substring(0, v.length() - 4)) * Math.min(VIEWPORT_WIDTH, VIEWPORT_HEIGHT) / 100f);
            }
            if (v.endsWith("vmax")) {
                return new LengthValue(LengthType.PX, parseFloatSafe(v.substring(0, v.length() - 4)) * Math.max(VIEWPORT_WIDTH, VIEWPORT_HEIGHT) / 100f);
            }
            if (v.endsWith("pt")) {
                return new LengthValue(LengthType.PX, parseFloatSafe(v.substring(0, v.length() - 2)) * 1.333f);
            }
            return null;
        }

        private static LengthValue parseCalc(String expr) {
            expr = expr.trim();
            int opIdx = -1;
            char op = 0;
            for (int i = 1; i < expr.length(); i++) {
                char c = expr.charAt(i);
                if ((c == '+' || c == '-') && Character.isWhitespace(expr.charAt(i - 1))) {
                    opIdx = i;
                    op = c;
                    break;
                }
            }
            if (opIdx <= 0) {
                return parse(expr);
            }
            LengthValue lv = parse(expr.substring(0, opIdx).trim());
            LengthValue rv = parse(expr.substring(opIdx + 1).trim());
            if (lv == null || rv == null || lv.type != rv.type) {
                return null;
            }
            float result = op == '+' ? lv.value + rv.value : lv.value - rv.value;
            return new LengthValue(lv.type, result);
        }

        private static float parseFloatSafe(String s) {
            try {
                return Float.parseFloat(s.trim());
            } catch (NumberFormatException ex) {
                throw new UnsupportedStyleException("Unsupported numeric value: " + s);
            }
        }
    }

    static String dimensionExpr(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if (v.isEmpty()) {
            return null;
        }
        if ("auto".equals(v) || "content".equals(v)) {
            return "TaffyDimension.AUTO";
        }
        if ("min-content".equals(v)) {
            return "TaffyDimension.minContent()";
        }
        if ("max-content".equals(v)) {
            return "TaffyDimension.maxContent()";
        }
        if ("stretch".equals(v)) {
            return "TaffyDimension.stretch()";
        }
        if (v.startsWith("fit-content")) {
            if (v.equals("fit-content")) {
                return "TaffyDimension.fitContent()";
            }
            if (v.startsWith("fit-content(") && v.endsWith(")")) {
                LengthValue lv = LengthValue.parse(v.substring(12, v.length() - 1).trim());
                if (lv != null) {
                    return switch (lv.type) {
                        case PX -> "TaffyDimension.fitContent(" + lv.value + "f)";
                        case PERCENT -> "TaffyDimension.fitContentPercent(" + (lv.value / 100f) + "f)";
                    };
                }
            }
            return "TaffyDimension.fitContent()";
        }
        LengthValue lv = LengthValue.parse(v);
        if (lv == null) {
            return null;
        }
        return switch (lv.type) {
            case PX -> "TaffyDimension.length(" + lv.value + "f)";
            case PERCENT -> "TaffyDimension.percent(" + (lv.value / 100f) + "f)";
        };
    }

    static String lpExpr(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        LengthValue lv = LengthValue.parse(v);
        if (lv == null) {
            throw new UnsupportedStyleException("Unsupported length: " + v);
        }
        return switch (lv.type) {
            case PX -> "LengthPercentage.length(" + lv.value + "f)";
            case PERCENT -> "LengthPercentage.percent(" + (lv.value / 100f) + "f)";
        };
    }

    static String lpaExpr(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if ("auto".equals(v)) {
            return "LengthPercentageAuto.AUTO";
        }
        LengthValue lv = LengthValue.parse(v);
        if (lv == null) {
            throw new UnsupportedStyleException("Unsupported length: " + v);
        }
        return switch (lv.type) {
            case PX -> "LengthPercentageAuto.length(" + lv.value + "f)";
            case PERCENT -> "LengthPercentageAuto.percent(" + (lv.value / 100f) + "f)";
        };
    }

    static StyleCodegen.GridTracks gridTemplateTracks(String raw) {
        if (raw == null || raw.isBlank() || "none".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        String css = raw.trim();
        boolean withRepeat = css.toLowerCase(Locale.ROOT).contains("repeat(");
        List<String> items = withRepeat
                ? GridTemplateList.parse(css).items
                : TrackList.parse(css).items;
        return new StyleCodegen.GridTracks(withRepeat, false, items);
    }

    static StyleCodegen.GridTracks gridAutoTracks(String raw) {
        if (raw == null || raw.isBlank() || "auto".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        return StyleCodegen.GridTracks.plain(TrackList.parse(raw.trim()).items);
    }

    static String gridAutoFlow(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return switch (value) {
            case "row" -> "ROW";
            case "column" -> "COLUMN";
            case "row dense" -> "ROW_DENSE";
            case "column dense" -> "COLUMN_DENSE";
            default -> null;
        };
    }

    static StyleCodegen.GridLine gridLine(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim();
        if (v.isEmpty()) {
            return null;
        }
        String[] parts = v.split("/");
        String start = parts.length > 0 ? parts[0].trim() : "auto";
        String end = parts.length > 1 ? parts[1].trim() : "auto";
        GridPlacementParts s = GridPlacementParts.parse(start);
        GridPlacementParts e = GridPlacementParts.parse(end);
        if (s == null || e == null) {
            throw new UnsupportedStyleException("Unsupported grid line syntax: " + v);
        }
        return new StyleCodegen.GridLine(s.toJava(), e.toJava());
    }

    static StyleCodegen.GridLine gridLine(String start, String end) {
        if (start == null && end == null) {
            return null;
        }
        if (start == null) {
            start = "auto";
        }
        if (end == null) {
            end = "auto";
        }
        return gridLine(start + " / " + end);
    }

    static String[] splitWs(String s) {
        return s.trim().replaceAll("\\s+", " ").split(" ");
    }

    static List<String> splitTopLevel(String s) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            }
            if (c == ')') {
                depth = Math.max(0, depth - 1);
            }
            if (depth == 0 && Character.isWhitespace(c)) {
                if (!cur.isEmpty()) {
                    out.add(cur.toString());
                    cur.setLength(0);
                }
            } else {
                cur.append(c);
            }
        }
        if (!cur.isEmpty()) {
            out.add(cur.toString());
        }
        return out;
    }

    static List<String> splitTopLevelByComma(String s) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            }
            if (c == ')') {
                depth = Math.max(0, depth - 1);
            }
            if (depth == 0 && c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    static String insideParens(String token) {
        int a = token.indexOf('(');
        int b = token.lastIndexOf(')');
        if (a < 0 || b < 0 || b <= a) {
            throw new UnsupportedStyleException("Missing parens: " + token);
        }
        return token.substring(a + 1, b);
    }

    /** Parses a plain (non-repeat) track list. */
    static final class TrackList {
        final List<String> items;

        private TrackList(List<String> items) {
            this.items = items;
        }

        static TrackList parse(String raw) {
            String s = raw.trim();
            if (s.isEmpty()) {
                return new TrackList(List.of());
            }
            s = s.replaceAll("\\[[^\\]]*\\]", " ");
            s = s.replaceAll("\\s+", " ").trim();
            List<String> out = new ArrayList<>();
            for (String token : splitTopLevel(s)) {
                if (!token.isBlank()) {
                    out.add(trackExpr(token.trim()));
                }
            }
            return new TrackList(out);
        }

        static String trackExpr(String token) {
            String v = token.toLowerCase(Locale.ROOT);
            if ("auto".equals(v)) {
                return "TrackSizingFunction.auto()";
            }
            if ("min-content".equals(v)) {
                return "TrackSizingFunction.minContent()";
            }
            if ("max-content".equals(v)) {
                return "TrackSizingFunction.maxContent()";
            }
            if (v.endsWith("fr")) {
                float fr = Float.parseFloat(v.substring(0, v.length() - 2).trim());
                return "TrackSizingFunction.fr(" + fr + "f)";
            }
            if (v.startsWith("fit-content(")) {
                String inner = insideParens(token);
                return "TrackSizingFunction.fitContent(" + lpExpr(inner.trim().toLowerCase(Locale.ROOT)) + ")";
            }
            if (v.startsWith("minmax(")) {
                String inner = insideParens(token);
                List<String> parts = splitTopLevelByComma(inner);
                if (parts.size() != 2) {
                    throw new UnsupportedStyleException("Unsupported minmax(): " + token);
                }
                return "TrackSizingFunction.minmax("
                        + trackExpr(parts.get(0).trim()) + ", "
                        + trackExpr(parts.get(1).trim()) + ")";
            }
            LengthValue lv = LengthValue.parse(v);
            if (lv != null) {
                return switch (lv.type) {
                    case PX -> "TrackSizingFunction.fixed(" + lv.value + "f)";
                    case PERCENT -> "TrackSizingFunction.percent(" + (lv.value / 100f) + "f)";
                };
            }
            throw new UnsupportedStyleException("Unsupported track token: " + token);
        }
    }

    /** Parses grid-template-* values that may contain repeat(). */
    static final class GridTemplateList {
        final List<String> items;

        private GridTemplateList(List<String> items) {
            this.items = items;
        }

        static GridTemplateList parse(String raw) {
            String s = raw.trim();
            if (s.isEmpty()) {
                return new GridTemplateList(List.of());
            }
            s = s.replaceAll("\\[[^\\]]*\\]", " ");
            s = s.replaceAll("\\s+", " ").trim();
            List<String> out = new ArrayList<>();
            for (String token : splitTopLevel(s)) {
                if (token.isBlank()) {
                    continue;
                }
                String trimmed = token.trim();
                if (trimmed.toLowerCase(Locale.ROOT).startsWith("repeat(")) {
                    out.add(repeatExpr(trimmed));
                } else {
                    out.add("GridTemplateComponent.single(" + TrackList.trackExpr(trimmed) + ")");
                }
            }
            return new GridTemplateList(out);
        }

        private static String repeatExpr(String repeatToken) {
            String inner = insideParens(repeatToken);
            List<String> parts = splitTopLevelByComma(inner);
            if (parts.size() != 2) {
                throw new UnsupportedStyleException("Unsupported repeat(): " + repeatToken);
            }
            String countSpec = parts.get(0).trim().toLowerCase(Locale.ROOT);
            String trackListRaw = parts.get(1).trim();
            if (trackListRaw.toLowerCase(Locale.ROOT).contains("repeat(")) {
                throw new UnsupportedStyleException("Nested repeat() not supported: " + repeatToken);
            }
            List<String> tracks = TrackList.parse(trackListRaw).items;
            if (tracks.isEmpty()) {
                throw new UnsupportedStyleException("repeat() without tracks: " + repeatToken);
            }
            String args = String.join(", ", tracks);
            return switch (countSpec) {
                case "auto-fit" -> "GridTemplateComponent.autoFit(" + args + ")";
                case "auto-fill" -> "GridTemplateComponent.autoFill(" + args + ")";
                default -> {
                    int count;
                    try {
                        count = Integer.parseInt(countSpec);
                    } catch (NumberFormatException ex) {
                        throw new UnsupportedStyleException("Unsupported repeat count: " + repeatToken);
                    }
                    yield "GridTemplateComponent.repeatCount(" + count + ", " + args + ")";
                }
            };
        }
    }

    static final class GridPlacementParts {
        final String kind;
        final int value;

        private GridPlacementParts(String kind, int value) {
            this.kind = kind;
            this.value = value;
        }

        static GridPlacementParts parse(String raw) {
            String v = raw.trim().toLowerCase(Locale.ROOT);
            if (v.isEmpty() || "auto".equals(v)) {
                return new GridPlacementParts("auto", 0);
            }
            if (v.startsWith("span")) {
                String[] parts = v.replaceAll("\\s+", " ").split(" ");
                if (parts.length != 2) {
                    throw new UnsupportedStyleException("Unsupported span: " + raw);
                }
                try {
                    return new GridPlacementParts("span", Integer.parseInt(parts[1]));
                } catch (NumberFormatException ex) {
                    throw new UnsupportedStyleException("Unsupported span with named line: " + raw);
                }
            }
            if (!v.matches("-?\\d+")) {
                throw new UnsupportedStyleException("Unsupported grid line syntax: " + v);
            }
            return new GridPlacementParts("line", Integer.parseInt(v));
        }

        String toJava() {
            return switch (kind) {
                case "auto" -> "GridPlacement.auto()";
                case "span" -> "GridPlacement.span(" + value + ")";
                case "line" -> "GridPlacement.line(" + value + ")";
                default -> throw new UnsupportedStyleException("Unknown placement kind: " + kind);
            };
        }
    }

    /** Case-insensitive view over inline + computed CSS declarations. */
    static final class CssStyleMap {
        final Map<String, String> props;

        private CssStyleMap(Map<String, String> props) {
            this.props = props;
        }

        static CssStyleMap from(String inlineStyle, Map<String, String> computed) {
            Map<String, String> map = new HashMap<>();
            if (computed != null) {
                for (Map.Entry<String, String> entry : computed.entrySet()) {
                    map.put(normalizeKey(entry.getKey()), normalizeVal(entry.getValue()));
                }
            }
            if (inlineStyle != null && !inlineStyle.isBlank()) {
                for (String declaration : inlineStyle.split(";")) {
                    int idx = declaration.indexOf(':');
                    if (idx <= 0) {
                        continue;
                    }
                    String key = normalizeKey(declaration.substring(0, idx));
                    String value = normalizeVal(declaration.substring(idx + 1));
                    if (!key.isEmpty() && !value.isEmpty()) {
                        map.put(key, value);
                    }
                }
            }
            return new CssStyleMap(map);
        }

        String get(String key) {
            return props.get(normalizeKey(key));
        }

        private static String normalizeKey(String key) {
            return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        }

        private static String normalizeVal(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
