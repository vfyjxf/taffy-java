package dev.vfyjxf.taffy.gentest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.vfyjxf.taffy.gentest.FixtureDiscovery.Fixture;
import dev.vfyjxf.taffy.gentest.StyleCodegen.EdgeKind;
import dev.vfyjxf.taffy.gentest.StyleCodegen.GridLine;
import dev.vfyjxf.taffy.gentest.StyleCodegen.GridTracks;
import dev.vfyjxf.taffy.gentest.StyleCodegen.NodeStyle;
import dev.vfyjxf.taffy.gentest.StyleCodegen.Size;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Source adapter for Rust taffy HTML fixtures.
 *
 * <p>Discovers {@code <taffy-root>/test_fixtures/<category>/*.html}, drives
 * Chrome, and emits one JUnit class per category. The generated code shape is
 * kept byte-compatible with the historical {@code TestGenerator} (see
 * {@code --check}).</p>
 */
final class TaffySource {

    private static final Gson GSON = new Gson();

    private TaffySource() {}

    static List<Fixture> discover(GeneratorOptions options) throws IOException {
        Path fixturesRoot = resolveFixturesRoot(options);
        List<Fixture> fixtures = FixtureDiscovery.discoverTaffy(fixturesRoot, options);
        if (fixtures.isEmpty()) {
            throw new GeneratorOptions.UsageException(
                    "No taffy fixtures matched under " + fixturesRoot +
                            (options.categories.isEmpty() ? "" : " (categories=" + options.categories + ")"));
        }
        return fixtures;
    }

    static Path resolveFixturesRoot(GeneratorOptions options) {
        GeneratorOptions.requireDirectory(options.taffyRoot, "taffy root",
                "Pass --taffy-root <checkout> (or -PtaffyRoot=...) pointing at a Rust taffy checkout " +
                        "that contains test_fixtures/ and scripts/. The default is ../taffy when present.");
        Path fixtures = options.taffyRoot.resolve("test_fixtures");
        if (!Files.isDirectory(fixtures)) {
            throw new GeneratorOptions.UsageException(
                    "test_fixtures/ not found under taffy root: " + fixtures +
                            "\nExpected layout: <taffy-root>/test_fixtures/<category>/*.html and <taffy-root>/scripts/gentest/test_helper.js");
        }
        return fixtures;
    }

    /** Result of one category generation. */
    record GeneratedFile(String category, String className, Path file) {}

    static List<GeneratedFile> generate(GeneratorOptions options, Path packageRoot, ChromeOracle oracle) throws IOException {
        Path fixturesRoot = resolveFixturesRoot(options);
        List<Fixture> fixtures = discover(options);
        System.out.println("Taffy fixtures root: " + fixturesRoot);
        System.out.println("Found " + fixtures.size() + " taffy fixtures");

        Map<String, List<Fixture>> byCategory = fixtures.stream()
                .collect(Collectors.groupingBy(Fixture::category,
                        java.util.TreeMap::new, Collectors.toList()));

        List<GeneratedFile> generated = new ArrayList<>();
        for (Map.Entry<String, List<Fixture>> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            List<Fixture> categoryFixtures = entry.getValue();
            System.out.println("Processing category: " + category + " (" + categoryFixtures.size() + " tests)");

            String className = StyleCodegen.toPascalCase(category) + "Test";
            StringBuilder body = new StringBuilder();
            int processed = 0;
            for (Fixture fixture : categoryFixtures) {
                try {
                    String test = generateFixture(oracle, fixture);
                    if (test != null) {
                        body.append(test);
                        processed++;
                    }
                } catch (ScrollbarWidth.ContractViolation ex) {
                    // Platform-level contract (15px scrollbar): abort the whole
                    // run instead of writing an empty/incorrect suite.
                    throw ex;
                } catch (Exception ex) {
                    System.err.println("Error processing " + fixture.name() + ": " + ex.getMessage());
                    ex.printStackTrace();
                }
                if (processed > 0 && processed % 10 == 0) {
                    System.out.print(".");
                }
            }

            String source = JavaTestWriter.taffyHeader(options.taffyPackage(), category, className)
                    + body
                    + "}\n";
            Path file = packageRoot.resolve(category).resolve(className + ".java");
            JavaTestWriter.write(file, source);
            System.out.println("\nWrote " + processed + " tests to " + file);
            generated.add(new GeneratedFile(category, className, file));
        }
        return generated;
    }

    private static String generateFixture(ChromeOracle oracle, Fixture fixture) throws IOException {
        // loadFile() waits for the load event; the stylesheet probe then guards
        // against measuring a page whose body-class variants are not applied yet.
        oracle.loadFile(fixture.path());
        oracle.verifyBaseStylesheetApplied();
        float scrollbarWidth = oracle.measureScrollbarWidth();
        Object result = readTestData(oracle, fixture);
        if (result == null) {
            System.err.println("No test data for: " + fixture.name());
            return null;
        }
        JsonObject data = GSON.fromJson(result.toString(), JsonObject.class);

        String body = null;
        String content = null;
        if (data.has("borderBoxData") || data.has("contentBoxData")) {
            body = getObjectString(data, "borderBoxData");
            content = getObjectString(data, "contentBoxData");
        } else if (data.has("borderBoxLtrData") || data.has("contentBoxLtrData")) {
            // Modern taffy helper (post direction support). RTL variants are not
            // emitted yet; see README "Known limitations".
            System.err.println("[warn] " + fixture.relativePath() +
                    ": modern helper schema detected; generating LTR variants only");
            body = getObjectString(data, "borderBoxLtrData");
            content = getObjectString(data, "contentBoxLtrData");
        } else {
            throw new UnsupportedStyleException(
                    "unrecognized getTestData() schema (expected borderBoxData/contentBoxData or *LtrData); " +
                            "is the taffy checkout compatible with this generator?");
        }

        StringBuilder sb = new StringBuilder();
        if (body != null) {
            sb.append(generateTest(fixture.name() + "__border_box", body, scrollbarWidth));
        }
        if (content != null) {
            sb.append(generateTest(fixture.name() + "__content_box", content, scrollbarWidth));
        }
        return sb.toString();
    }

    /**
     * Read getTestData(), retrying transient JavaScript failures after
     * re-checking page readiness (upstream #1054 keeps the same backstop).
     * A null result is not retried: the fixture legitimately has no test data.
     */
    private static Object readTestData(ChromeOracle oracle, Fixture fixture) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= PageReadiness.GET_TEST_DATA_ATTEMPTS; attempt++) {
            try {
                return oracle.executeScript("return getTestData()");
            } catch (RuntimeException ex) {
                last = ex;
                if (attempt == PageReadiness.GET_TEST_DATA_ATTEMPTS) {
                    break;
                }
                System.err.println("[warn] getTestData() failed for " + fixture.relativePath()
                        + " (attempt " + attempt + "): " + ex.getMessage());
                sleep(PageReadiness.RETRY_DELAY_MS);
                oracle.awaitDocumentReady();
                oracle.verifyBaseStylesheetApplied();
            }
        }
        throw new IllegalStateException("getTestData() failed for " + fixture.relativePath()
                + " after " + PageReadiness.GET_TEST_DATA_ATTEMPTS + " attempts", last);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while retrying getTestData()", ex);
        }
    }

    private static String getObjectString(JsonObject data, String key) {
        if (!data.has(key) || data.get(key).isJsonNull()) {
            return null;
        }
        return data.get(key).toString();
    }

    private static String generateTest(String name, String descriptionJson, float scrollbarWidth) {
        JsonObject description = GSON.fromJson(descriptionJson, JsonObject.class);
        StringBuilder sb = new StringBuilder();

        boolean useRounding = description.has("useRounding") && description.get("useRounding").getAsBoolean();
        String methodName = StyleCodegen.toMethodName(name);
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"").append(StyleCodegen.escapeJavaString(name)).append("\")\n");
        sb.append("    void ").append(methodName).append("() {\n");
        sb.append("        TaffyTree tree = new TaffyTree();\n");
        if (!useRounding) {
            sb.append("        tree.disableRounding();\n");
        }
        sb.append("\n");

        generateNodeCreation(sb, "node", description, scrollbarWidth);

        sb.append("\n");
        String availableSpace = generateAvailableSpace(description.getAsJsonObject("viewport"));
        sb.append("        tree.computeLayout(node, ").append(availableSpace).append(");\n\n");
        generateAssertions(sb, "node", description, useRounding, 2);
        sb.append("    }\n\n");
        return sb.toString();
    }

    private static void generateNodeCreation(StringBuilder sb, String ident, JsonObject node, float scrollbarWidth) {
        String ind = "        ";
        JsonObject style = node.getAsJsonObject("style");
        JsonArray children = node.has("children") ? node.getAsJsonArray("children") : null;

        List<String> childIdents = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (int i = 0; i < children.size(); i++) {
                String childIdent = ident + i;
                childIdents.add(childIdent);
                generateNodeCreation(sb, childIdent, children.get(i).getAsJsonObject(), scrollbarWidth);
            }
        }

        sb.append(ind).append("TaffyStyle ").append(ident).append("Style = new TaffyStyle();\n");
        StyleCodegen.emit(sb, ident + "Style", toNodeStyle(style, scrollbarWidth), ind);

        String textContent = null;
        if (node.has("textContent") && !node.get("textContent").isJsonNull()) {
            textContent = node.get("textContent").getAsString();
            if (textContent != null) {
                textContent = textContent.trim();
                if (textContent.isEmpty()) {
                    textContent = null;
                }
            }
        }

        boolean verticalWritingMode = false;
        if (style != null && style.has("writingMode") && !style.get("writingMode").isJsonNull()) {
            String wm = style.get("writingMode").getAsString();
            if (wm != null && wm.startsWith("vertical")) {
                verticalWritingMode = true;
            }
        }

        if (childIdents.isEmpty()) {
            if (textContent != null) {
                sb.append(ind).append("MeasureFunc ").append(ident).append("Measure = ahemTextMeasure(\"")
                        .append(StyleCodegen.escapeJavaString(textContent))
                        .append("\", ").append(verticalWritingMode).append(");\n");
                sb.append(ind).append("NodeId ").append(ident).append(" = tree.newLeafWithMeasure(")
                        .append(ident).append("Style, ").append(ident).append("Measure);\n");
            } else {
                sb.append(ind).append("NodeId ").append(ident).append(" = tree.newLeaf(").append(ident).append("Style);\n");
            }
        } else {
            sb.append(ind).append("NodeId ").append(ident).append(" = tree.newWithChildren(").append(ident).append("Style");
            for (String child : childIdents) {
                sb.append(", ").append(child);
            }
            sb.append(");\n");
        }
        sb.append("\n");
    }

    private static NodeStyle toNodeStyle(JsonObject style, float scrollbarWidth) {
        NodeStyle out = new NodeStyle();
        if (style == null) {
            return out;
        }

        if (has(style, "boxSizing")) {
            String value = style.get("boxSizing").getAsString();
            if ("content-box".equals(value)) {
                out.boxSizing = "CONTENT_BOX";
            }
        }

        // Historical helper revisions did not report direction at all; the
        // committed tests encode the computed default LTR for every node.
        if (has(style, "direction")) {
            String value = style.get("direction").getAsString();
            if ("rtl".equals(value)) {
                out.direction = "RTL";
            } else if ("ltr".equals(value)) {
                out.direction = "LTR";
            }
        } else {
            out.direction = "LTR";
        }

        if (has(style, "display")) {
            String value = style.get("display").getAsString();
            switch (value) {
                case "none" -> out.display = "NONE";
                case "block" -> out.display = "BLOCK";
                case "grid" -> out.display = "GRID";
                default -> {
                    // flex is TaffyStyle's default and is intentionally omitted.
                }
            }
        }

        if (has(style, "position") && "absolute".equals(style.get("position").getAsString())) {
            out.position = "ABSOLUTE";
        }

        if (has(style, "flexDirection")) {
            String value = style.get("flexDirection").getAsString();
            switch (value) {
                case "row-reverse" -> out.flexDirection = "ROW_REVERSE";
                case "column" -> out.flexDirection = "COLUMN";
                case "column-reverse" -> out.flexDirection = "COLUMN_REVERSE";
                default -> {
                }
            }
        }
        if (has(style, "flexWrap")) {
            String value = style.get("flexWrap").getAsString();
            switch (value) {
                case "wrap" -> out.flexWrap = "WRAP";
                case "wrap-reverse" -> out.flexWrap = "WRAP_REVERSE";
                default -> {
                }
            }
        }

        out.alignItems = mapStyleValue(style, "alignItems", StyleCodegen::mapAlignItems);
        out.alignSelf = mapStyleValue(style, "alignSelf", StyleCodegen::mapAlignSelf);
        out.alignContent = mapStyleValue(style, "alignContent", StyleCodegen::mapAlignContent);
        out.justifyContent = mapStyleValue(style, "justifyContent", StyleCodegen::mapJustifyContent);
        out.textAlign = mapStyleValue(style, "textAlign", StyleCodegen::mapTextAlign);
        out.justifyItems = mapStyleValue(style, "justifyItems", StyleCodegen::mapAlignItems);
        out.justifySelf = mapStyleValue(style, "justifySelf", StyleCodegen::mapAlignSelf);

        if (has(style, "flexGrow")) {
            out.flexGrow = style.get("flexGrow").getAsFloat();
        }
        if (has(style, "flexShrink")) {
            out.flexShrink = style.get("flexShrink").getAsFloat();
        }
        if (has(style, "flexBasis")) {
            out.flexBasis = generateDimension(style.getAsJsonObject("flexBasis"));
        }

        out.size = generateSize(style, "size");
        out.minSize = generateSize(style, "minSize");
        out.maxSize = generateSize(style, "maxSize");
        if (has(style, "aspectRatio")) {
            out.aspectRatio = style.get("aspectRatio").getAsFloat();
        }
        if (has(style, "gap")) {
            JsonObject gap = style.getAsJsonObject("gap");
            String column = has(gap, "column") ? generateLengthPercentage(gap.getAsJsonObject("column")) : null;
            String row = has(gap, "row") ? generateLengthPercentage(gap.getAsJsonObject("row")) : null;
            if (column != null || row != null) {
                out.gap = new Size(column, row);
            }
        }

        if (has(style, "margin")) {
            out.margin = generateEdges(style.getAsJsonObject("margin"), EdgeKind.LENGTH_PERCENTAGE_AUTO_ZERO);
        }
        if (has(style, "padding")) {
            out.padding = generateEdges(style.getAsJsonObject("padding"), EdgeKind.LENGTH_PERCENTAGE);
        }
        if (has(style, "border")) {
            out.border = generateEdges(style.getAsJsonObject("border"), EdgeKind.LENGTH_PERCENTAGE);
        }
        if (has(style, "inset")) {
            out.inset = generateEdges(style.getAsJsonObject("inset"), EdgeKind.LENGTH_PERCENTAGE_AUTO);
        }

        out.gridTemplateColumns = readTracks(style, "gridTemplateColumns", true);
        out.gridTemplateRows = readTracks(style, "gridTemplateRows", true);
        out.gridAutoColumns = readTracks(style, "gridAutoColumns", false);
        out.gridAutoRows = readTracks(style, "gridAutoRows", false);

        if (has(style, "gridAutoFlow")) {
            JsonObject flow = style.getAsJsonObject("gridAutoFlow");
            String direction = flow.get("direction").getAsString();
            String algorithm = flow.get("algorithm").getAsString();
            out.gridAutoFlow = switch (direction + "_" + algorithm) {
                case "row_sparse" -> "ROW";
                case "column_sparse" -> "COLUMN";
                case "row_dense" -> "ROW_DENSE";
                case "column_dense" -> "COLUMN_DENSE";
                default -> null;
            };
        }

        out.gridRow = readGridLine(style, "gridRowStart", "gridRowEnd");
        out.gridColumn = readGridLine(style, "gridColumnStart", "gridColumnEnd");

        out.overflow = readOverflow(style, scrollbarWidth);
        return out;
    }

    private static String mapStyleValue(JsonObject style, String key,
                                         java.util.function.Function<String, String> mapper) {
        if (!has(style, key)) {
            return null;
        }
        return mapper.apply(style.get(key).getAsString());
    }

    private static boolean has(JsonObject object, String key) {
        return object != null && object.has(key) && !object.get(key).isJsonNull();
    }

    private static Size generateSize(JsonObject style, String key) {
        if (!has(style, key)) {
            return null;
        }
        JsonObject size = style.getAsJsonObject(key);
        String width = has(size, "width") ? generateDimension(size.getAsJsonObject("width")) : null;
        String height = has(size, "height") ? generateDimension(size.getAsJsonObject("height")) : null;
        return (width == null && height == null) ? null : new Size(width, height);
    }

    private static StyleCodegen.Edges generateEdges(JsonObject edges, EdgeKind kind) {
        if (edges == null) {
            return null;
        }
        String left = readEdge(edges, "left", kind);
        String right = readEdge(edges, "right", kind);
        String top = readEdge(edges, "top", kind);
        String bottom = readEdge(edges, "bottom", kind);
        if (left == null && right == null && top == null && bottom == null) {
            return null;
        }
        return new StyleCodegen.Edges(left, right, top, bottom, kind);
    }

    private static String readEdge(JsonObject edges, String key, EdgeKind kind) {
        if (!has(edges, key)) {
            return null;
        }
        JsonObject value = edges.getAsJsonObject(key);
        return kind == EdgeKind.LENGTH_PERCENTAGE
                ? generateLengthPercentage(value)
                : generateLengthPercentageAuto(value);
    }

    private static String generateDimension(JsonObject dim) {
        if (dim == null) {
            return null;
        }
        String unit = dim.get("unit").getAsString();
        return switch (unit) {
            case "auto" -> "TaffyDimension.AUTO";
            case "px" -> "TaffyDimension.length(" + StyleCodegen.floatLiteral(dim.get("value").getAsFloat()) + ")";
            case "percent" -> "TaffyDimension.percent(" + StyleCodegen.floatLiteral(dim.get("value").getAsFloat()) + ")";
            case "min-content" -> "TaffyDimension.minContent()";
            case "max-content" -> "TaffyDimension.maxContent()";
            case "fit-content" -> "TaffyDimension.fitContent()";
            case "stretch" -> "TaffyDimension.stretch()";
            case "content" -> "TaffyDimension.content()";
            default -> null;
        };
    }

    private static String generateLengthPercentage(JsonObject lp) {
        if (lp == null) {
            return null;
        }
        String unit = lp.get("unit").getAsString();
        return switch (unit) {
            case "px" -> "LengthPercentage.length(" + StyleCodegen.floatLiteral(lp.get("value").getAsFloat()) + ")";
            case "percent" -> "LengthPercentage.percent(" + StyleCodegen.floatLiteral(lp.get("value").getAsFloat()) + ")";
            default -> null;
        };
    }

    private static String generateLengthPercentageAuto(JsonObject lpa) {
        if (lpa == null) {
            return null;
        }
        String unit = lpa.get("unit").getAsString();
        return switch (unit) {
            case "auto" -> "LengthPercentageAuto.AUTO";
            case "px" -> "LengthPercentageAuto.length(" + StyleCodegen.floatLiteral(lpa.get("value").getAsFloat()) + ")";
            case "percent" -> "LengthPercentageAuto.percent(" + StyleCodegen.floatLiteral(lpa.get("value").getAsFloat()) + ")";
            case "min-content" -> "LengthPercentageAuto.minContent()";
            case "max-content" -> "LengthPercentageAuto.maxContent()";
            case "fit-content" -> "LengthPercentageAuto.fitContent()";
            case "stretch" -> "LengthPercentageAuto.stretch()";
            default -> null;
        };
    }

    /** Package-private so generator-contract tests can exercise both schema shapes. */
    static GridTracks readTracks(JsonObject style, String key, boolean template) {
        if (!has(style, key)) {
            return null;
        }
        JsonElement element = style.get(key);
        if (element.isJsonPrimitive()) {
            String css = element.getAsString();
            return template ? CssValues.gridTemplateTracks(css) : CssValues.gridAutoTracks(css);
        }
        if (!element.isJsonArray()) {
            return null;
        }
        return tracksFromJson(element.getAsJsonArray(), template);
    }

    private static GridTracks tracksFromJson(JsonArray tracks, boolean template) {
        if (tracks.isEmpty()) {
            return null;
        }
        boolean hasAutoRepetition = false;
        for (JsonElement element : tracks) {
            JsonObject track = element.getAsJsonObject();
            String kind = track.has("kind") ? track.get("kind").getAsString() : "scalar";
            if ("function".equals(kind) && "repeat".equals(track.get("name").getAsString())) {
                JsonArray args = track.getAsJsonArray("arguments");
                if (args.size() >= 2) {
                    JsonObject count = args.get(0).getAsJsonObject();
                    String unit = count.has("unit") ? count.get("unit").getAsString() : "";
                    if ("auto-fill".equals(unit) || "auto-fit".equals(unit)) {
                        hasAutoRepetition = true;
                        break;
                    }
                }
            }
        }

        List<String> entries = new ArrayList<>();
        if (template && hasAutoRepetition) {
            for (JsonElement element : tracks) {
                JsonObject track = element.getAsJsonObject();
                String kind = track.has("kind") ? track.get("kind").getAsString() : "scalar";
                if ("function".equals(kind) && "repeat".equals(track.get("name").getAsString())) {
                    JsonArray args = track.getAsJsonArray("arguments");
                    if (args.size() >= 2) {
                        JsonObject count = args.get(0).getAsJsonObject();
                        String unit = count.has("unit") ? count.get("unit").getAsString() : "";
                        if ("auto-fill".equals(unit) || "auto-fit".equals(unit)) {
                            List<String> inner = new ArrayList<>();
                            for (int j = 1; j < args.size(); j++) {
                                inner.add(generateTrackSizingFunction(args.get(j).getAsJsonObject()));
                            }
                            String method = "auto-fill".equals(unit) ? "autoFill" : "autoFit";
                            entries.add("GridTemplateComponent." + method + "(" + String.join(", ", inner) + ")");
                        } else if ("integer".equals(unit)) {
                            int countValue = count.get("value").getAsInt();
                            for (int i = 0; i < countValue; i++) {
                                for (int j = 1; j < args.size(); j++) {
                                    String tsf = generateTrackSizingFunction(args.get(j).getAsJsonObject());
                                    if (tsf != null) {
                                        entries.add("GridTemplateComponent.single(" + tsf + ")");
                                    }
                                }
                            }
                        }
                    }
                } else {
                    String tsf = generateTrackSizingFunction(track);
                    if (tsf != null) {
                        entries.add("GridTemplateComponent.single(" + tsf + ")");
                    }
                }
            }
            return entries.isEmpty() ? null : GridTracks.repeat(entries, true);
        }

        for (JsonElement element : tracks) {
            JsonObject track = element.getAsJsonObject();
            String kind = track.has("kind") ? track.get("kind").getAsString() : "scalar";
            if ("function".equals(kind) && "repeat".equals(track.get("name").getAsString())) {
                JsonArray args = track.getAsJsonArray("arguments");
                if (args.size() >= 2) {
                    JsonObject count = args.get(0).getAsJsonObject();
                    if ("integer".equals(count.get("unit").getAsString())) {
                        int countValue = count.get("value").getAsInt();
                        for (int i = 0; i < countValue; i++) {
                            for (int j = 1; j < args.size(); j++) {
                                String tsf = generateTrackSizingFunction(args.get(j).getAsJsonObject());
                                if (tsf != null) {
                                    entries.add(tsf);
                                }
                            }
                        }
                    }
                }
            } else {
                String tsf = generateTrackSizingFunction(track);
                if (tsf != null) {
                    entries.add(tsf);
                }
            }
        }
        return entries.isEmpty() ? null : GridTracks.plain(entries);
    }

    private static String generateTrackSizingFunction(JsonObject track) {
        String kind = track.has("kind") ? track.get("kind").getAsString() : "scalar";
        if ("function".equals(kind)) {
            String name = track.get("name").getAsString();
            JsonArray args = track.getAsJsonArray("arguments");
            return switch (name) {
                case "minmax" -> {
                    if (args.size() >= 2) {
                        String min = generateTrackSizingFunction(args.get(0).getAsJsonObject());
                        String max = generateTrackSizingFunction(args.get(1).getAsJsonObject());
                        yield "TrackSizingFunction.minmax(" + min + ", " + max + ")";
                    }
                    yield null;
                }
                case "fit-content" -> {
                    if (!args.isEmpty()) {
                        JsonObject arg = args.get(0).getAsJsonObject();
                        String unit = arg.has("unit") ? arg.get("unit").getAsString() : "";
                        String lp = switch (unit) {
                            case "px" -> "LengthPercentage.length(" + StyleCodegen.floatLiteral(arg.get("value").getAsFloat()) + ")";
                            case "percent" -> "LengthPercentage.percent(" + StyleCodegen.floatLiteral(arg.get("value").getAsFloat()) + ")";
                            default -> "LengthPercentage.ZERO";
                        };
                        yield "TrackSizingFunction.fitContent(" + lp + ")";
                    }
                    yield null;
                }
                default -> null;
            };
        }

        String unit = track.get("unit").getAsString();
        return switch (unit) {
            case "px" -> "TrackSizingFunction.fixed(LengthPercentage.length(" + StyleCodegen.floatLiteral(track.get("value").getAsFloat()) + "))";
            case "percent" -> "TrackSizingFunction.fixed(LengthPercentage.percent(" + StyleCodegen.floatLiteral(track.get("value").getAsFloat()) + "))";
            case "fraction" -> "TrackSizingFunction.fr(" + StyleCodegen.floatLiteral(track.get("value").getAsFloat()) + ")";
            case "auto" -> "TrackSizingFunction.auto()";
            case "min-content" -> "TrackSizingFunction.minContent()";
            case "max-content" -> "TrackSizingFunction.maxContent()";
            default -> null;
        };
    }

    private static String generateGridPlacement(JsonObject placement) {
        if (placement == null) {
            return null;
        }
        String kind = placement.get("kind").getAsString();
        return switch (kind) {
            case "auto" -> "GridPlacement.auto()";
            case "line" -> "GridPlacement.line(" + placement.get("value").getAsInt() + ")";
            case "span" -> "GridPlacement.span(" + placement.get("value").getAsInt() + ")";
            default -> null;
        };
    }

    private static GridLine readGridLine(JsonObject style, String startKey, String endKey) {
        if (has(style, startKey)) {
            String start = generateGridPlacement(style.getAsJsonObject(startKey));
            if (start != null) {
                String end = has(style, endKey)
                        ? generateGridPlacement(style.getAsJsonObject(endKey))
                        : "GridPlacement.auto()";
                return new GridLine(start, end != null ? end : "GridPlacement.auto()");
            }
        } else if (has(style, endKey)) {
            String end = generateGridPlacement(style.getAsJsonObject(endKey));
            if (end != null) {
                return new GridLine("GridPlacement.auto()", end);
            }
        }
        return null;
    }

    private static StyleCodegen.Overflow readOverflow(JsonObject style, float measuredScrollbarWidth) {
        boolean hasX = has(style, "overflowX");
        boolean hasY = has(style, "overflowY");
        if (!hasX && !hasY) {
            return null;
        }
        String x = hasX ? style.get("overflowX").getAsString() : "visible";
        String y = hasY ? style.get("overflowY").getAsString() : "visible";
        String mappedX = StyleCodegen.mapOverflow(x);
        String mappedY = StyleCodegen.mapOverflow(y);
        if ("Overflow.VISIBLE".equals(mappedX) && "Overflow.VISIBLE".equals(mappedY)) {
            return null;
        }
        Float scrollbarWidth = null;
        if ("scroll".equals(x) || "scroll".equals(y)) {
            if (has(style, "scrollbarWidth")) {
                float value = style.get("scrollbarWidth").getAsFloat();
                scrollbarWidth = value > 0 ? value : (measuredScrollbarWidth > 0 ? measuredScrollbarWidth : null);
            } else if (measuredScrollbarWidth > 0) {
                scrollbarWidth = measuredScrollbarWidth;
            }
        }
        return new StyleCodegen.Overflow(mappedX, mappedY, scrollbarWidth);
    }

    private static String generateAvailableSpace(JsonObject viewport) {
        if (viewport == null) {
            return "TaffySize.maxContent()";
        }
        JsonObject width = viewport.has("width") ? viewport.getAsJsonObject("width") : null;
        JsonObject height = viewport.has("height") ? viewport.getAsJsonObject("height") : null;
        boolean widthMaxContent = width == null || "max-content".equals(width.get("unit").getAsString());
        boolean heightMaxContent = height == null || "max-content".equals(height.get("unit").getAsString());
        if (widthMaxContent && heightMaxContent) {
            return "TaffySize.maxContent()";
        }
        String w = widthMaxContent ? "AvailableSpace.maxContent()" : generateAvailableSpaceDim(width);
        String h = heightMaxContent ? "AvailableSpace.maxContent()" : generateAvailableSpaceDim(height);
        return "new TaffySize<>(" + w + ", " + h + ")";
    }

    private static String generateAvailableSpaceDim(JsonObject dim) {
        String unit = dim.get("unit").getAsString();
        return switch (unit) {
            case "px" -> "AvailableSpace.definite(" + StyleCodegen.floatLiteral(dim.get("value").getAsFloat()) + ")";
            case "max-content" -> "AvailableSpace.maxContent()";
            case "min-content" -> "AvailableSpace.minContent()";
            default -> "AvailableSpace.maxContent()";
        };
    }

    private static void generateAssertions(StringBuilder sb, String ident, JsonObject node,
                                           boolean useRounding, int indent) {
        String ind = "        ";
        JsonObject layout = useRounding
                ? node.getAsJsonObject("smartRoundedLayout")
                : node.getAsJsonObject("unroundedLayout");
        float width = layout.get("width").getAsFloat();
        float height = layout.get("height").getAsFloat();
        float x = layout.get("x").getAsFloat();
        float y = layout.get("y").getAsFloat();

        sb.append(ind).append("Layout ").append(ident).append("Layout = tree.getLayout(").append(ident).append(");\n");
        if (useRounding) {
            sb.append(ind).append("assertEquals(").append(StyleCodegen.floatLiteral(width)).append(", ").append(ident).append("Layout.size().width, \"width of ").append(ident).append("\");\n");
            sb.append(ind).append("assertEquals(").append(StyleCodegen.floatLiteral(height)).append(", ").append(ident).append("Layout.size().height, \"height of ").append(ident).append("\");\n");
            sb.append(ind).append("assertEquals(").append(StyleCodegen.floatLiteral(x)).append(", ").append(ident).append("Layout.location().x, \"x of ").append(ident).append("\");\n");
            sb.append(ind).append("assertEquals(").append(StyleCodegen.floatLiteral(y)).append(", ").append(ident).append("Layout.location().y, \"y of ").append(ident).append("\");\n");
        } else {
            sb.append(ind).append("assertEquals(").append(StyleCodegen.floatLiteral(width)).append(", ").append(ident).append("Layout.size().width, EPSILON, \"width of ").append(ident).append("\");\n");
            sb.append(ind).append("assertEquals(").append(StyleCodegen.floatLiteral(height)).append(", ").append(ident).append("Layout.size().height, EPSILON, \"height of ").append(ident).append("\");\n");
            sb.append(ind).append("assertEquals(").append(StyleCodegen.floatLiteral(x)).append(", ").append(ident).append("Layout.location().x, EPSILON, \"x of ").append(ident).append("\");\n");
            sb.append(ind).append("assertEquals(").append(StyleCodegen.floatLiteral(y)).append(", ").append(ident).append("Layout.location().y, EPSILON, \"y of ").append(ident).append("\");\n");
        }

        appendScrollSizeAssertions(sb, ident, node, useRounding, ind);

        if (node.has("children") && !node.get("children").isJsonNull()) {
            JsonArray children = node.getAsJsonArray("children");
            for (int i = 0; i < children.size(); i++) {
                generateAssertions(sb, ident + i, children.get(i).getAsJsonObject(), useRounding, indent);
            }
        }
    }

    /**
     * Emits the {@code scrollWidth()}/{@code scrollHeight()} assertions for a node that is a
     * scroll container, reading the expected values from the fixture's Chrome measurement.
     *
     * <p>Upstream's XML runner has always recorded {@code scroll_width}/{@code scroll_height}
     * (upstream #1052, {@code tests/xml.rs}), but this port's generated tests only asserted
     * size/location. The fixture data already carries what is needed: {@code getTestData()}
     * returns Chrome's {@code scrollWidth}/{@code scrollHeight} in the layout objects and
     * {@code clientWidth}/{@code clientHeight} in the naively rounded layout. The expected
     * maximum scroll offset is therefore {@code max(0, scrollWidth - clientWidth)} for each axis
     * (upstream's XML fixture generator, #1052/#1364).</p>
     *
     * <p>Only nodes that establish a scroll container ({@code overflow-x}/{@code overflow-y} in
     * {@code hidden}, {@code scroll} or {@code auto}) get the assertions, exactly like the
     * upstream runner: for every other node the values are not meaningful and may be absent from
     * the fixture. A fixture that does not carry one of the fields (or the {@code style} object)
     * is skipped rather than failing, so older fixture data still generates a compilable test.</p>
     *
     * @param sb         the generated test body
     * @param ident      the node identifier used by the generated test
     * @param node       the fixture node description from {@code getTestData()}
     * @param useRounding whether the generated test uses smart rounding
     * @param ind        the indentation prefix for the emitted lines
     */
    static void appendScrollSizeAssertions(StringBuilder sb, String ident, JsonObject node,
                                           boolean useRounding, String ind) {
        if (!isScrollContainer(node)) {
            return;
        }
        Float scrollWidth = scrollSizeExpectation(node, useRounding, "scrollWidth", "clientWidth");
        if (scrollWidth != null) {
            sb.append(ind).append("assertEquals(").append(StyleCodegen.floatLiteral(scrollWidth))
                .append(", ").append(ident).append("Layout.scrollWidth(), EPSILON, \"scroll width of ")
                .append(ident).append("\");\n");
        }
        Float scrollHeight = scrollSizeExpectation(node, useRounding, "scrollHeight", "clientHeight");
        if (scrollHeight != null) {
            sb.append(ind).append("assertEquals(").append(StyleCodegen.floatLiteral(scrollHeight))
                .append(", ").append(ident).append("Layout.scrollHeight(), EPSILON, \"scroll height of ")
                .append(ident).append("\");\n");
        }
    }

    /**
     * The expected maximum scroll offset for one axis, or {@code null} when the fixture does not
     * record it.
     *
     * <p>This mirrors upstream's XML fixture generator: the scroll extent comes from the layout
     * object selected by the rounding mode, the client extent always comes from the naively
     * rounded layout (Chrome's {@code offsetWidth}/{@code clientWidth} family), and the
     * difference is floored at zero. The naive client size is used because it is the only place
     * the fixture records it, and it does not depend on the rounding strategy.</p>
     *
     * @param node         the fixture node description
     * @param useRounding  whether to read {@code smartRoundedLayout} instead of {@code unroundedLayout}
     * @param scrollKey    {@code scrollWidth} or {@code scrollHeight}
     * @param clientKey    {@code clientWidth} or {@code clientHeight}
     * @return the expected range, or {@code null} if any input field is missing
     */
    static Float scrollSizeExpectation(JsonObject node, boolean useRounding,
                                       String scrollKey, String clientKey) {
        JsonObject layout = objectField(node, useRounding ? "smartRoundedLayout" : "unroundedLayout");
        JsonObject naive = objectField(node, "naivelyRoundedLayout");
        if (layout == null || naive == null) {
            return null;
        }
        Float scrollExtent = floatField(layout, scrollKey);
        Float clientExtent = floatField(naive, clientKey);
        if (scrollExtent == null || clientExtent == null) {
            return null;
        }
        return Math.max(0f, scrollExtent - clientExtent);
    }

    /**
     * Whether the fixture node establishes a scroll container, i.e. either {@code overflow-x} or
     * {@code overflow-y} is {@code hidden}, {@code scroll} or {@code auto}.
     *
     * @param node the fixture node description
     * @return whether the node is a scroll container
     */
    static boolean isScrollContainer(JsonObject node) {
        JsonObject style = objectField(node, "style");
        if (style == null) {
            return false;
        }
        return isScrollableOverflow(style, "overflowX") || isScrollableOverflow(style, "overflowY");
    }

    private static boolean isScrollableOverflow(JsonObject style, String key) {
        JsonElement element = style.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return false;
        }
        String value = element.getAsString();
        return "hidden".equals(value) || "scroll".equals(value) || "auto".equals(value);
    }

    private static JsonObject objectField(JsonObject parent, String key) {
        JsonElement element = parent.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static Float floatField(JsonObject parent, String key) {
        JsonElement element = parent.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        return element.getAsFloat();
    }
}
