package dev.vfyjxf.taffy.gentest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.vfyjxf.taffy.gentest.CssValues.CssStyleMap;
import dev.vfyjxf.taffy.gentest.FixtureDiscovery.Fixture;
import dev.vfyjxf.taffy.gentest.StyleCodegen.EdgeKind;
import dev.vfyjxf.taffy.gentest.StyleCodegen.GridLine;
import dev.vfyjxf.taffy.gentest.StyleCodegen.GridTracks;
import dev.vfyjxf.taffy.gentest.StyleCodegen.NodeStyle;
import dev.vfyjxf.taffy.gentest.StyleCodegen.Size;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Source adapter for the local web-platform-tests checkout.
 *
 * <p>Discovers allowlisted/discovered tests, loads them through a local HTTP
 * server, measures the DOM in headless Chrome, maps CSS onto a {@link NodeStyle},
 * and emits JUnit tests through the shared emitter.</p>
 */
final class WptSource {

    static final int MAX_TESTS_PER_CLASS = 20;
    private static final Gson GSON = new Gson();
    private static final int BATCH_SIZE = 15;

    private WptSource() {}

    record GeneratedFile(String suite, String className, Path file) {}

    static Path resolveCssRoot(GeneratorOptions options) {
        GeneratorOptions.requireDirectory(options.wptRoot, "WPT root",
                "Pass --wpt-root <checkout> (or -PwptRoot=...) pointing at a WPT checkout. " +
                        "A sparse checkout of css/ is enough.");
        Path cssRoot = options.wptCssRoot != null ? options.wptCssRoot : options.wptRoot;
        if (Files.isDirectory(cssRoot.resolve("css"))) {
            return cssRoot;
        }
        // Allow pointing directly at the WPT css/ directory (or at a checkout
        // whose css/ lives somewhere else). Discovery/URL paths are always
        // prefixed with "css/", so use the parent in that case.
        if (Files.isDirectory(cssRoot.resolve("css-grid")) || Files.isDirectory(cssRoot.resolve("css-flexbox"))
                || Files.isDirectory(cssRoot.resolve("css-sizing"))) {
            Path parent = cssRoot.getParent();
            if (parent != null) {
                return parent;
            }
        }
        throw new GeneratorOptions.UsageException(
                "css/ not found under WPT root: " + cssRoot +
                        "\nExpected <wpt-root>/css/... or pass --wpt-css-root pointing at the directory that contains css/.");
    }

    static List<Fixture> discover(GeneratorOptions options) throws IOException {
        Path cssRoot = resolveCssRoot(options);
        List<Fixture> fixtures = FixtureDiscovery.discoverWpt(cssRoot, options.wptSuites, options.maxFiles, options);
        List<Fixture> filtered = new ArrayList<>();
        for (Fixture fixture : fixtures) {
            String suite = fixture.category();
            if (suite == null) {
                System.err.println("[skip] not a supported suite path: " + fixture.relativePath());
                continue;
            }
            if (!options.categories.isEmpty() && !categoryMatches(options.categories, suite, fixture.relativePath())) {
                continue;
            }
            filtered.add(fixture);
        }
        if (filtered.isEmpty()) {
            throw new GeneratorOptions.UsageException(
                    "No WPT tests discovered under " + cssRoot + " (suites=" + options.wptSuites + ")" +
                            (options.categories.isEmpty() ? "" : " categories=" + options.categories));
        }
        return filtered;
    }

    private static boolean categoryMatches(Set<String> categories, String suite, String relativePath) {
        if (categories.contains(suite)) {
            return true;
        }
        for (String category : categories) {
            String normalized = category.replace('\\', '/');
            while (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            if (relativePath.equals(normalized) || relativePath.startsWith(normalized + "/")) {
                return true;
            }
        }
        return false;
    }

    static List<GeneratedFile> generate(GeneratorOptions options, Path packageRoot, ChromeOracle oracle) throws IOException {
        Path cssRoot = resolveCssRoot(options);
        List<Fixture> fixtures = discover(options);
        Path urlRoot = options.wptRoot;
        System.out.println("WPT root: " + options.wptRoot);
        System.out.println("CSS root: " + cssRoot);
        System.out.println("Tests to process: " + fixtures.size());

        Map<String, List<Fixture>> bySuite = new LinkedHashMap<>();
        for (Fixture fixture : fixtures) {
            bySuite.computeIfAbsent(fixture.category(), key -> new ArrayList<>()).add(fixture);
        }
        List<String> suites = new ArrayList<>(bySuite.keySet());
        suites.sort(String::compareTo);

        List<GeneratedFile> generated = new ArrayList<>();
        List<String> accepted = new ArrayList<>();

        LocalHttpServer server = LocalHttpServer.start(urlRoot, cssRoot);
        System.out.println("WPT HTTP base: " + server.baseUrl());
        WptSupport.SkipStats skipStats = new WptSupport.SkipStats();
        try {
            for (String suite : suites) {
                List<Fixture> suiteFixtures = bySuite.get(suite);
                suiteFixtures.sort((a, b) -> a.relativePath().compareTo(b.relativePath()));
                String pkg = options.wptPackage() + "." + suite;
                Path outDir = packageRoot.resolve(suite);
                String baseClass = JavaTestWriter.wptSuiteClass(suite);
                Set<Path> written = new HashSet<>();

                // Generate all accepted methods first: chunking by *accepted* count keeps
                // classes full (a fixture-level split would leave holes after skips).
                List<String> methods = new ArrayList<>();
                for (Fixture fixture : suiteFixtures) {
                    try {
                        String method = generateOne(oracle, server.baseUrl(), fixture, skipStats);
                        if (method != null) {
                            methods.add(method);
                            accepted.add(fixture.relativePath());
                        }
                    } catch (Exception ex) {
                        System.err.println("[error] " + fixture.relativePath() + ": " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }

                boolean chunked = methods.size() > MAX_TESTS_PER_CLASS;
                int classIndex = 0;
                for (int start = 0; start < methods.size(); start += MAX_TESTS_PER_CLASS) {
                    int end = Math.min(start + MAX_TESTS_PER_CLASS, methods.size());
                    String className = chunked ? baseClass + "_" + (classIndex + 1) : baseClass;
                    StringBuilder sb = new StringBuilder();
                    sb.append(JavaTestWriter.wptHeader(pkg, className));
                    for (String method : methods.subList(start, end)) {
                        sb.append(method);
                    }
                    sb.append("}\n");
                    Path file = outDir.resolve(className + ".java");
                    JavaTestWriter.write(file, sb.toString());
                    written.add(file.toAbsolutePath().normalize());
                    System.out.println("Wrote " + (end - start) + " tests to " + file);
                    classIndex++;
                }
                cleanupStale(outDir, baseClass, written);
                System.out.println("Suite '" + suite + "' generated tests: " + methods.size());
                for (Path file : written) {
                    generated.add(new GeneratedFile(suite, file.getFileName().toString().replace(".java", ""), file));
                }
            }
        } finally {
            server.close();
        }
        skipStats.printSummary();
        System.out.println("WPT accepted: " + accepted.size() + " test(s) from "
                + fixtures.size() + " fixture(s)");

        if (options.acceptedOut != null) {
            List<String> unique = accepted.stream().map(s -> s.replace('\\', '/')).distinct().toList();
            Path parent = options.acceptedOut.toAbsolutePath().normalize().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(options.acceptedOut, String.join("\n", unique) + "\n", StandardCharsets.UTF_8);
            System.out.println("Wrote accepted allowlist: " + options.acceptedOut + " (" + unique.size() + " tests)");
        }
        return generated;
    }

    private static void cleanupStale(Path outDir, String baseClass, Set<Path> written) throws IOException {
        if (!Files.isDirectory(outDir)) {
            return;
        }
        try (var stream = Files.list(outDir)) {
            for (Path file : stream.toList()) {
                String name = file.getFileName().toString();
                if (!name.equals(baseClass + ".java") && !name.matches(java.util.regex.Pattern.quote(baseClass) + "_\\d+\\.java")) {
                    continue;
                }
                if (!written.contains(file.toAbsolutePath().normalize())) {
                    Files.deleteIfExists(file);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Per-fixture generation
    // ------------------------------------------------------------------

    private static String generateOne(ChromeOracle oracle, String baseUrl, Fixture fixture,
                                       WptSupport.SkipStats skipStats) {
        String url = baseUrl + (fixture.relativePath().startsWith("/")
                ? fixture.relativePath().substring(1)
                : fixture.relativePath());
        oracle.loadUrl(url);
        Object result = oracle.executeScript(WptDomCollector.JS_COLLECT);
        if (result == null || result.toString().isBlank()) {
            return skip(fixture, skipStats, "collector returned no data");
        }
        JsonObject data = GSON.fromJson(result.toString(), JsonObject.class);
        if (data.has("skipReason") && !data.get("skipReason").isJsonNull()) {
            return skip(fixture, skipStats, data.get("skipReason").getAsString());
        }
        JsonObject viewport = data.getAsJsonObject("viewport");
        int vw = viewport.get("width").getAsInt();
        int vh = viewport.get("height").getAsInt();

        JsonArray nodeArray = data.getAsJsonArray("nodes");
        if (nodeArray == null || nodeArray.isEmpty()) {
            return skip(fixture, skipStats, "collector returned an empty node list");
        }
        List<NodeInfo> infos = new ArrayList<>(nodeArray.size());
        for (JsonElement element : nodeArray) {
            infos.add(NodeInfo.from(element.getAsJsonObject()));
        }
        if (infos.size() > 500) {
            return skip(fixture, skipStats, "too many nodes (" + infos.size() + " > 500)");
        }
        NodeInfo root = infos.get(0);
        if (!"body".equalsIgnoreCase(root.tag)) {
            return skip(fixture, skipStats, "unexpected root tag (expected body): " + root.tag);
        }
        int testRootIndex = -1;
        for (int i = 1; i < infos.size(); i++) {
            if (infos.get(i).parentIndex == 0) {
                testRootIndex = i;
                break;
            }
        }
        if (testRootIndex < 0) {
            return skip(fixture, skipStats, "no test container selected");
        }
        String unsupported = WptSupport.unsupportedReason(infos, testRootIndex);
        if (unsupported != null) {
            return skip(fixture, skipStats, unsupported);
        }

        List<WptStyle> styles;
        try {
            styles = new ArrayList<>(infos.size());
            for (NodeInfo info : infos) {
                styles.add(WptStyle.fromDom(info));
            }
        } catch (UnsupportedStyleException ex) {
            return skip(fixture, skipStats, "unsupported style: " + ex.getMessage());
        }

        int n = infos.size();
        Map<Integer, List<Integer>> children = new HashMap<>();
        for (int i = 0; i < n; i++) {
            children.put(i, new ArrayList<>());
        }
        for (int i = 1; i < n; i++) {
            int parent = infos.get(i).parentIndex;
            if (parent >= 0 && parent < n) {
                children.get(parent).add(i);
            }
        }
        List<Integer> order = topoOrderBottomUp(children);

        List<AssertionData> assertions = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            NodeInfo info = infos.get(i);
            float relX = info.rectX;
            float relY = info.rectY;
            if (info.parentIndex == 0 && i == testRootIndex) {
                NodeInfo body = infos.get(0);
                float bodyBorderLeft = parsePxValue(body.computed.get("border-left-width"));
                float bodyBorderTop = parsePxValue(body.computed.get("border-top-width"));
                float bodyPaddingLeft = parsePxValue(body.computed.get("padding-left"));
                float bodyPaddingTop = parsePxValue(body.computed.get("padding-top"));
                float marginLeft = parsePxValue(info.computed.get("margin-left"));
                float marginTop = parsePxValue(info.computed.get("margin-top"));
                relX = bodyPaddingLeft + bodyBorderLeft + marginLeft;
                relY = bodyPaddingTop + bodyBorderTop + marginTop;
            } else if (info.parentIndex >= 0 && info.parentIndex < n) {
                NodeInfo parent = infos.get(info.parentIndex);
                relX = info.rectX - parent.rectX;
                relY = info.rectY - parent.rectY;
            }
            String label = (info.id != null && !info.id.isBlank())
                    ? info.tag + "#" + info.id
                    : info.tag + "[" + i + "]";
            assertions.add(new AssertionData(i, label, info.rectW, info.rectH, relX, relY));
        }

        Map<Integer, float[]> browserSizes = new HashMap<>();
        for (int i = 0; i < n; i++) {
            NodeInfo info = infos.get(i);
            WptStyle style = styles.get(i);
            if (children.get(i).isEmpty() && style.needsMeasure && (info.rectW > 0 || info.rectH > 0)) {
                browserSizes.put(i, new float[]{info.rectW, info.rectH});
            }
        }

        // Build final node styles, including root/size overrides.
        List<NodeStyle> nodeStyles = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            NodeStyle nodeStyle = styles.get(i).node;
            if (i == 0) {
                nodeStyle.size = new Size(
                        "TaffyDimension.length(" + vw + "f)",
                        "TaffyDimension.length(" + vh + "f)");
            } else {
                float[] browser = browserSizes.get(i);
                if (browser != null) {
                    nodeStyle.size = new Size(
                            "TaffyDimension.length(" + JavaTestWriter.fmtFloat(browser[0]) + "f)",
                            "TaffyDimension.length(" + JavaTestWriter.fmtFloat(browser[1]) + "f)");
                }
            }
            nodeStyles.add(nodeStyle);
        }

        String methodName = JavaTestWriter.wptMethodName(fixture.name());
        boolean needsSplit = n > 25;
        StringBuilder sb = new StringBuilder();
        if (needsSplit) {
            appendSplitTest(sb, fixture, methodName, n, nodeStyles, order, children, assertions);
        } else {
            appendSimpleTest(sb, fixture, methodName, n, nodeStyles, order, children, assertions);
        }
        return sb.toString();
    }

    private static String skip(Fixture fixture, WptSupport.SkipStats skipStats, String reason) {
        System.err.println("[skip] " + fixture.relativePath() + ": " + reason);
        skipStats.record(reason);
        return null;
    }

    private static void appendSimpleTest(StringBuilder sb, Fixture fixture, String methodName, int n,
                                         List<NodeStyle> styles, List<Integer> order,
                                         Map<Integer, List<Integer>> children,
                                         List<AssertionData> assertions) {
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"").append(StyleCodegen.escapeJavaString(fixture.relativePath())).append("\")\n");
        sb.append("    void ").append(methodName).append("() {\n");
        sb.append("        TaffyTree tree = new TaffyTree();\n");
        sb.append("        tree.disableRounding();\n\n");

        for (int i = 0; i < n; i++) {
            sb.append("        TaffyStyle s").append(i).append(" = new TaffyStyle();\n");
            StyleCodegen.emit(sb, "s" + i, styles.get(i), "        ");
            sb.append("\n");
        }
        for (int i = 0; i < n; i++) {
            sb.append("        NodeId n").append(i).append(";\n");
        }
        sb.append("\n");
        for (int idx : order) {
            List<Integer> child = children.get(idx);
            if (child.isEmpty()) {
                appendLeaf(sb, "n" + idx, "s" + idx, styles.get(idx), "        ");
            } else {
                sb.append("        n").append(idx).append(" = tree.newWithChildren(s").append(idx).append(", ");
                for (int j = 0; j < child.size(); j++) {
                    if (j > 0) {
                        sb.append(", ");
                    }
                    sb.append("n").append(child.get(j));
                }
                sb.append(");\n");
            }
        }
        sb.append("\n");
        sb.append("        tree.computeLayout(n0, TaffySize.maxContent());\n\n");
        appendAssertionsInline(sb, assertions);
        sb.append("    }\n\n");
    }

    /**
     * Emits leaf creation. Replaced elements with a known intrinsic size are created
     * with the {@code replacedIntrinsicSize} measure callback; every other leaf keeps
     * the plain {@code newLeaf} form.
     */
    private static void appendLeaf(StringBuilder sb, String nodeVar, String styleVar,
                                   NodeStyle style, String ind) {
        if (style.intrinsicWidth != null && style.intrinsicHeight != null) {
            sb.append(ind).append(nodeVar).append(" = tree.newLeafWithMeasure(").append(styleVar)
                    .append(", replacedIntrinsicSize(")
                    .append(JavaTestWriter.fmtFloat(style.intrinsicWidth)).append("f, ")
                    .append(JavaTestWriter.fmtFloat(style.intrinsicHeight)).append("f));\n");
        } else {
            sb.append(ind).append(nodeVar).append(" = tree.newLeaf(").append(styleVar).append(");\n");
        }
    }

    private static void appendSplitTest(StringBuilder sb, Fixture fixture, String methodName, int n,
                                        List<NodeStyle> styles, List<Integer> order,
                                        Map<Integer, List<Integer>> children,
                                        List<AssertionData> assertions) {
        int styleMethodCount = (n + BATCH_SIZE - 1) / BATCH_SIZE;
        for (int batch = 0; batch < styleMethodCount; batch++) {
            int start = batch * BATCH_SIZE;
            int end = Math.min(start + BATCH_SIZE, n);
            sb.append("    private void ").append(methodName).append("_styles_").append(batch)
                    .append("(TaffyStyle[] styles) {\n");
            for (int i = start; i < end; i++) {
                sb.append("        styles[").append(i).append("] = new TaffyStyle();\n");
                StyleCodegen.emit(sb, "styles[" + i + "]", styles.get(i), "        ");
                sb.append("\n");
            }
            sb.append("    }\n\n");
        }

        int nodeMethodCount = (order.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        for (int batch = 0; batch < nodeMethodCount; batch++) {
            int start = batch * BATCH_SIZE;
            int end = Math.min(start + BATCH_SIZE, order.size());
            sb.append("    private void ").append(methodName).append("_nodes_").append(batch)
                    .append("(TaffyTree tree, TaffyStyle[] styles, NodeId[] nodes) {\n");
            for (int j = start; j < end; j++) {
                int idx = order.get(j);
                List<Integer> child = children.get(idx);
                if (child.isEmpty()) {
                    appendLeaf(sb, "nodes[" + idx + "]", "styles[" + idx + "]", styles.get(idx), "        ");
                } else {
                    sb.append("        nodes[").append(idx).append("] = tree.newWithChildren(styles[").append(idx).append("], ");
                    for (int k = 0; k < child.size(); k++) {
                        if (k > 0) {
                            sb.append(", ");
                        }
                        sb.append("nodes[").append(child.get(k)).append("]");
                    }
                    sb.append(");\n");
                }
            }
            sb.append("    }\n\n");
        }

        int assertMethodCount = (assertions.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        for (int batch = 0; batch < assertMethodCount; batch++) {
            int start = batch * BATCH_SIZE;
            int end = Math.min(start + BATCH_SIZE, assertions.size());
            sb.append("    private void ").append(methodName).append("_assert_").append(batch)
                    .append("(TaffyTree tree, NodeId[] nodes) {\n");
            for (int j = start; j < end; j++) {
                AssertionData assertion = assertions.get(j);
                sb.append("        // ").append(StyleCodegen.escapeJavaString(assertion.label)).append("\n");
                sb.append("        Layout l").append(assertion.index).append(" = tree.getLayout(nodes[")
                        .append(assertion.index).append("]);\n");
                appendAssertion(sb, assertion);
                sb.append("\n");
            }
            sb.append("    }\n\n");
        }

        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"").append(StyleCodegen.escapeJavaString(fixture.relativePath())).append("\")\n");
        sb.append("    void ").append(methodName).append("() {\n");
        sb.append("        TaffyTree tree = new TaffyTree();\n");
        sb.append("        tree.disableRounding();\n\n");
        sb.append("        TaffyStyle[] styles = new TaffyStyle[").append(n).append("];\n");
        for (int batch = 0; batch < styleMethodCount; batch++) {
            sb.append("        ").append(methodName).append("_styles_").append(batch).append("(styles);\n");
        }
        sb.append("\n");
        sb.append("        NodeId[] nodes = new NodeId[").append(n).append("];\n");
        for (int batch = 0; batch < nodeMethodCount; batch++) {
            sb.append("        ").append(methodName).append("_nodes_").append(batch).append("(tree, styles, nodes);\n");
        }
        sb.append("\n");
        sb.append("        tree.computeLayout(nodes[0], TaffySize.maxContent());\n\n");
        for (int batch = 0; batch < assertMethodCount; batch++) {
            sb.append("        ").append(methodName).append("_assert_").append(batch).append("(tree, nodes);\n");
        }
        sb.append("    }\n\n");
    }

    private static void appendAssertionsInline(StringBuilder sb, List<AssertionData> assertions) {
        for (AssertionData assertion : assertions) {
            sb.append("        // ").append(StyleCodegen.escapeJavaString(assertion.label)).append("\n");
            sb.append("        Layout l").append(assertion.index).append(" = tree.getLayout(n")
                    .append(assertion.index).append(");\n");
            appendAssertion(sb, assertion);
            sb.append("\n");
        }
    }

    private static void appendAssertion(StringBuilder sb, AssertionData assertion) {
        String value = JavaTestWriter.fmt3(assertion.width);
        sb.append("        assertEquals(").append(value).append("f, l").append(assertion.index)
                .append(".size().width, EPSILON, \"width\");\n");
        sb.append("        assertEquals(").append(JavaTestWriter.fmt3(assertion.height)).append("f, l")
                .append(assertion.index).append(".size().height, EPSILON, \"height\");\n");
        sb.append("        assertEquals(").append(JavaTestWriter.fmt3(assertion.x)).append("f, l")
                .append(assertion.index).append(".location().x, EPSILON, \"x\");\n");
        sb.append("        assertEquals(").append(JavaTestWriter.fmt3(assertion.y)).append("f, l")
                .append(assertion.index).append(".location().y, EPSILON, \"y\");\n");
    }

    private record AssertionData(int index, String label, float width, float height, float x, float y) {}

    private static List<Integer> topoOrderBottomUp(Map<Integer, List<Integer>> children) {
        List<Integer> out = new ArrayList<>();
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, 0});
        while (!stack.isEmpty()) {
            int[] frame = stack.peek();
            int node = frame[0];
            int idx = frame[1];
            List<Integer> child = children.getOrDefault(node, List.of());
            if (idx < child.size()) {
                int next = child.get(idx);
                frame[1] = idx + 1;
                stack.push(new int[]{next, 0});
            } else {
                stack.pop();
                out.add(node);
            }
        }
        return out;
    }

    private static float parsePxValue(String value) {
        if (value == null || value.isBlank()) {
            return 0f;
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        if ("0".equals(v)) {
            return 0f;
        }
        if (v.endsWith("px")) {
            try {
                return Float.parseFloat(v.substring(0, v.length() - 2).trim());
            } catch (NumberFormatException ex) {
                return 0f;
            }
        }
        return 0f;
    }

    // ------------------------------------------------------------------
    // DOM model
    // ------------------------------------------------------------------

    static final class NodeInfo {
        final int index;
        final int parentIndex;
        final String tag;
        final String id;
        final String inlineStyle;
        final Map<String, String> computed;
        final boolean hasText;
        final int naturalWidth;
        final int naturalHeight;
        final float rectX;
        final float rectY;
        final float rectW;
        final float rectH;

        private NodeInfo(int index, int parentIndex, String tag, String id, String inlineStyle,
                         Map<String, String> computed, boolean hasText,
                         int naturalWidth, int naturalHeight,
                         float rectX, float rectY, float rectW, float rectH) {
            this.index = index;
            this.parentIndex = parentIndex;
            this.tag = tag;
            this.id = id;
            this.inlineStyle = inlineStyle;
            this.computed = computed;
            this.hasText = hasText;
            this.naturalWidth = naturalWidth;
            this.naturalHeight = naturalHeight;
            this.rectX = rectX;
            this.rectY = rectY;
            this.rectW = rectW;
            this.rectH = rectH;
        }

        static NodeInfo from(JsonObject object) {
            int index = object.get("index").getAsInt();
            int parent = object.get("parent").getAsInt();
            String tag = object.get("tag").getAsString();
            String id = "";
            if (object.has("id")) {
                JsonElement element = object.get("id");
                if (element.isJsonPrimitive()) {
                    id = element.getAsString();
                }
            }
            String style = object.has("style") ? object.get("style").getAsString() : "";
            Map<String, String> computed = new HashMap<>();
            if (object.has("computed")) {
                for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("computed").entrySet()) {
                    computed.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            boolean hasText = object.has("hasText") && object.get("hasText").getAsBoolean();
            int naturalWidth = object.has("naturalWidth") ? object.get("naturalWidth").getAsInt() : 0;
            int naturalHeight = object.has("naturalHeight") ? object.get("naturalHeight").getAsInt() : 0;
            JsonObject rect = object.getAsJsonObject("rect");
            return new NodeInfo(index, parent, tag, id, style, computed, hasText,
                    naturalWidth, naturalHeight,
                    rect.get("x").getAsFloat(), rect.get("y").getAsFloat(),
                    rect.get("w").getAsFloat(), rect.get("h").getAsFloat());
        }
    }

    // ------------------------------------------------------------------
    // CSS -> NodeStyle mapping
    // ------------------------------------------------------------------

    static final class WptStyle {
        final NodeStyle node = new NodeStyle();
        boolean needsMeasure;

        static WptStyle fromDom(NodeInfo info) {
            CssStyleMap css = CssStyleMap.from(info.inlineStyle, info.computed);
            WptStyle out = new WptStyle();
            NodeStyle node = out.node;

            String display = lower(css.get("display"));
            node.display = switch (display == null ? "" : display) {
                case "flex", "inline-flex" -> "FLEX";
                case "grid", "inline-grid" -> "GRID";
                case "block", "inline-block" -> "BLOCK";
                case "flow-root" -> "FLOW_ROOT";
                case "none" -> "NONE";
                default -> null;
            };

            String position = lower(css.get("position"));
            node.position = switch (position == null ? "" : position) {
                case "absolute" -> "ABSOLUTE";
                case "relative" -> "RELATIVE";
                default -> null;
            };

            String boxSizing = lower(css.get("box-sizing"));
            node.boxSizing = switch (boxSizing == null ? "" : boxSizing) {
                case "border-box" -> "BORDER_BOX";
                case "content-box" -> "CONTENT_BOX";
                default -> null;
            };

            String direction = lower(css.get("direction"));
            node.direction = switch (direction == null ? "" : direction) {
                case "ltr" -> "LTR";
                case "rtl" -> "RTL";
                default -> null;
            };

            applyFloatAndClear(css, node);
            applyOverflow(css, node);

            String width = CssValues.dimensionExpr(css.get("width"));
            String height = CssValues.dimensionExpr(css.get("height"));
            if (width != null || height != null) {
                node.size = new Size(width, height);
            }
            String minWidth = CssValues.dimensionExpr(css.get("min-width"));
            String minHeight = CssValues.dimensionExpr(css.get("min-height"));
            if (minWidth != null || minHeight != null) {
                node.minSize = new Size(minWidth, minHeight);
            }
            String maxWidth = CssValues.dimensionExpr(css.get("max-width"));
            String maxHeight = CssValues.dimensionExpr(css.get("max-height"));
            if (maxWidth != null || maxHeight != null) {
                node.maxSize = new Size(maxWidth, maxHeight);
            }

            node.aspectRatio = parseAspectRatio(css.get("aspect-ratio"));
            applyReplacedData(info, css, node);

            node.margin = edgesFromBoxShorthand(css, "margin", true, EdgeKind.LENGTH_PERCENTAGE_AUTO_ZERO);
            node.padding = edgesFromBoxShorthand(css, "padding", false, EdgeKind.LENGTH_PERCENTAGE);
            node.border = edgesFromBorder(css);
            node.inset = edgesFromInset(css);

            String flexDirection = lower(css.get("flex-direction"));
            node.flexDirection = switch (flexDirection == null ? "" : flexDirection) {
                case "row" -> "ROW";
                case "row-reverse" -> "ROW_REVERSE";
                case "column" -> "COLUMN";
                case "column-reverse" -> "COLUMN_REVERSE";
                default -> null;
            };
            String flexWrap = lower(css.get("flex-wrap"));
            node.flexWrap = switch (flexWrap == null ? "" : flexWrap) {
                case "nowrap" -> "NO_WRAP";
                case "wrap" -> "WRAP";
                case "wrap-reverse" -> "WRAP_REVERSE";
                default -> null;
            };
            node.flexGrow = floatValue(css.get("flex-grow"));
            node.flexShrink = floatValue(css.get("flex-shrink"));
            node.flexBasis = CssValues.dimensionExpr(css.get("flex-basis"));

            node.alignItems = StyleCodegen.mapAlignItems(lower(css.get("align-items")));
            node.alignContent = StyleCodegen.mapAlignContent(lower(css.get("align-content")));
            node.justifyContent = StyleCodegen.mapJustifyContent(lower(css.get("justify-content")));
            node.alignSelf = wptAlignSelf(lower(css.get("align-self")));
            node.justifySelf = wptAlignSelf(lower(css.get("justify-self")));
            node.textAlign = StyleCodegen.mapTextAlign(lower(css.get("text-align")));
            node.justifyItems = StyleCodegen.mapAlignItems(lower(css.get("justify-items")));

            node.gap = gap(css);

            node.gridTemplateColumns = gridTemplate(css.get("grid-template-columns"));
            node.gridTemplateRows = gridTemplate(css.get("grid-template-rows"));
            node.gridAutoColumns = CssValues.gridAutoTracks(css.get("grid-auto-columns"));
            node.gridAutoRows = CssValues.gridAutoTracks(css.get("grid-auto-rows"));
            node.gridAutoFlow = CssValues.gridAutoFlow(css.get("grid-auto-flow"));

            String gridColumn = css.get("grid-column");
            if (gridColumn == null) {
                gridColumn = combineLine(css.get("grid-column-start"), css.get("grid-column-end"));
            }
            node.gridColumn = gridColumn == null ? null : CssValues.gridLine(gridColumn);

            String gridRow = css.get("grid-row");
            if (gridRow == null) {
                gridRow = combineLine(css.get("grid-row-start"), css.get("grid-row-end"));
            }
            node.gridRow = gridRow == null ? null : CssValues.gridLine(gridRow);

            String order = css.get("order");
            if (order != null && !order.isBlank()) {
                try {
                    if (Integer.parseInt(order.trim()) != 0) {
                        throw new UnsupportedStyleException("CSS order property not supported: " + order);
                    }
                } catch (NumberFormatException ignored) {
                    // Non-numeric order values are ignored.
                }
            }

            out.needsMeasure = needsMeasure(node);
            return out;
        }

        private static boolean needsMeasure(NodeStyle style) {
            if (style.size != null) {
                if (style.size.width() != null && !style.size.width().equals("TaffyDimension.AUTO")) {
                    return false;
                }
                if (style.size.height() != null && !style.size.height().equals("TaffyDimension.AUTO")) {
                    return false;
                }
            }
            if (style.display != null
                    && (style.display.equals("FLEX") || style.display.equals("GRID") || style.display.equals("BLOCK"))) {
                return false;
            }
            return true;
        }

        private static String wptAlignSelf(String value) {
            if (value == null) {
                return null;
            }
            return switch (value) {
                case "auto" -> null;
                case "stretch" -> "AlignItems.STRETCH";
                case "center" -> "AlignItems.CENTER";
                case "flex-start", "start" -> "AlignItems.FLEX_START";
                case "flex-end", "end" -> "AlignItems.FLEX_END";
                case "baseline" -> "AlignItems.BASELINE";
                default -> null;
            };
        }

        /**
         * Maps float/clear. Only the values taffy-java models are accepted;
         * {@code inline-start}/{@code inline-end} resolve through the inline
         * direction, everything else is reported as unsupported.
         */
        private static void applyFloatAndClear(CssStyleMap css, NodeStyle node) {
            boolean rtl = "rtl".equals(lower(css.get("direction")));
            String floatValue = lower(css.get("float"));
            node.floatSide = switch (floatValue == null ? "none" : floatValue) {
                case "none" -> null;
                case "left" -> "LEFT";
                case "right" -> "RIGHT";
                case "inline-start" -> rtl ? "RIGHT" : "LEFT";
                case "inline-end" -> rtl ? "LEFT" : "RIGHT";
                default -> throw new UnsupportedStyleException("CSS float value not supported: " + floatValue);
            };
            String clearValue = lower(css.get("clear"));
            node.clear = switch (clearValue == null ? "none" : clearValue) {
                case "none" -> null;
                case "left" -> "LEFT";
                case "right" -> "RIGHT";
                case "both" -> "BOTH";
                case "inline-start" -> rtl ? "RIGHT" : "LEFT";
                case "inline-end" -> rtl ? "LEFT" : "RIGHT";
                default -> throw new UnsupportedStyleException("CSS clear value not supported: " + clearValue);
            };
        }

        /** Maps the used overflow pair; {@code clip} maps onto {@code Overflow.CLIP}. */
        private static void applyOverflow(CssStyleMap css, NodeStyle node) {
            String x = lower(css.get("overflow-x"));
            String y = lower(css.get("overflow-y"));
            if (x == null && y == null) {
                String shorthand = lower(css.get("overflow"));
                if (shorthand == null || shorthand.isBlank()) {
                    return;
                }
                String[] parts = shorthand.trim().split("\\s+");
                x = parts[0];
                y = parts.length > 1 ? parts[1] : parts[0];
            }
            if (x == null) {
                x = "visible";
            }
            if (y == null) {
                y = "visible";
            }
            if ("visible".equals(x) && "visible".equals(y)) {
                return;
            }
            node.overflow = new StyleCodegen.Overflow(mapWptOverflow(x), mapWptOverflow(y), null);
        }

        /**
         * Maps one overflow axis. {@code auto} has no direct engine value: taffy's
         * {@code SCROLL} always reserves a scrollbar gutter, so {@code auto} maps to
         * {@code HIDDEN} (a scroll container for minimum-size purposes without a
         * reserved gutter). This is an approximation documented in gentest/README.md.
         */
        private static String mapWptOverflow(String value) {
            return switch (value) {
                case "clip" -> "Overflow.CLIP";
                case "hidden" -> "Overflow.HIDDEN";
                case "scroll" -> "Overflow.SCROLL";
                case "auto" -> "Overflow.HIDDEN";
                default -> "Overflow.VISIBLE";
            };
        }

        /**
         * Encodes replaced-element semantics: the engine consumes {@code itemIsReplaced}
         * (block/flex/grid stretch and minimum contributions) but has no intrinsic size
         * of its own, so the browser-reported natural size is turned into an intrinsic
         * aspect ratio, plus a measure callback when both axes are auto.
         */
        private static void applyReplacedData(NodeInfo info, CssStyleMap css, NodeStyle node) {
            if (!isReplacedTag(info.tag)) {
                return;
            }
            node.itemIsReplaced = true;
            float naturalRatio = info.naturalWidth > 0 && info.naturalHeight > 0
                    ? (float) info.naturalWidth / (float) info.naturalHeight
                    : Float.NaN;
            String rawRatio = css.get("aspect-ratio");
            boolean ratioIsAuto = rawRatio == null || rawRatio.isBlank()
                    || rawRatio.trim().toLowerCase(Locale.ROOT).startsWith("auto");
            if (!Float.isNaN(naturalRatio) && (ratioIsAuto || node.aspectRatio == null)) {
                node.aspectRatio = naturalRatio;
            }
            if (isAuto(css.get("width")) && isAuto(css.get("height"))
                    && info.naturalWidth > 0 && info.naturalHeight > 0) {
                node.intrinsicWidth = (float) info.naturalWidth;
                node.intrinsicHeight = (float) info.naturalHeight;
            }
        }

        private static boolean isReplacedTag(String tag) {
            if (tag == null) {
                return false;
            }
            return switch (tag.toLowerCase(Locale.ROOT)) {
                case "img", "canvas" -> true;
                default -> false;
            };
        }

        private static boolean isAuto(String value) {
            return value == null || value.isBlank() || "auto".equalsIgnoreCase(value.trim())
                    || "content".equalsIgnoreCase(value.trim());
        }

        /**
         * Parses {@code aspect-ratio}. The computed value of a replaced element is
         * {@code auto <ratio>} (natural ratio preferred, ratio as fallback), so the
         * leading {@code auto} keyword is stripped and the ratio part is used.
         */
        static Float parseAspectRatio(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            String v = value.trim().toLowerCase(Locale.ROOT);
            if ("auto".equals(v)) {
                return null;
            }
            if (v.startsWith("auto")) {
                v = v.substring(4).trim();
                if (v.isEmpty()) {
                    return null;
                }
            }
            try {
                if (v.contains("/")) {
                    String[] parts = v.split("\\s*/\\s*");
                    if (parts.length == 2) {
                        float width = Float.parseFloat(parts[0].trim());
                        float height = Float.parseFloat(parts[1].trim());
                        if (height != 0) {
                            return width / height;
                        }
                    }
                } else {
                    return Float.parseFloat(v);
                }
            } catch (NumberFormatException ignored) {
                // Unsupported aspect-ratio syntax.
            }
            return null;
        }

        private static Float floatValue(String value) {
            if (value == null) {
                return null;
            }
            String v = value.trim();
            if (v.isEmpty()) {
                return null;
            }
            try {
                return Float.parseFloat(v);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        private static String combineLine(String start, String end) {
            if (start == null && end == null) {
                return null;
            }
            return (start == null ? "auto" : start) + " / " + (end == null ? "auto" : end);
        }

        private static GridTracks gridTemplate(String raw) {
            if (raw == null || raw.isBlank() || "none".equalsIgnoreCase(raw.trim())) {
                return null;
            }
            if (raw.toLowerCase(Locale.ROOT).contains("repeat(")) {
                return CssValues.gridTemplateTracks(raw);
            }
            return GridTracks.plain(CssValues.TrackList.parse(raw).items);
        }

        private static Size gap(CssStyleMap css) {
            String gap = css.get("gap");
            String row = css.get("row-gap");
            String column = css.get("column-gap");
            if (gap != null && (row == null || column == null)) {
                String[] values = gap.trim().replaceAll("\\s+", " ").split(" ");
                if (values.length == 1) {
                    row = row != null ? row : values[0];
                    column = column != null ? column : values[0];
                } else if (values.length >= 2) {
                    row = row != null ? row : values[0];
                    column = column != null ? column : values[1];
                }
            }
            if (row == null && column == null) {
                return null;
            }
            return new Size(lpOrZero(column), lpOrZero(row));
        }

        private static String lpOrZero(String value) {
            if (value == null) {
                return "LengthPercentage.length(0f)";
            }
            String v = value.trim().toLowerCase(Locale.ROOT);
            if ("normal".equals(v)) {
                return "LengthPercentage.length(0f)";
            }
            return CssValues.lpExpr(v);
        }

        private static StyleCodegen.Edges edgesFromBoxShorthand(CssStyleMap css, String base,
                                                                boolean allowAuto, EdgeKind kind) {
            String shorthand = css.get(base);
            String left = css.get(base + "-left");
            String right = css.get(base + "-right");
            String top = css.get(base + "-top");
            String bottom = css.get(base + "-bottom");
            if (shorthand != null) {
                String[] values = CssValues.splitWs(shorthand);
                String t;
                String r;
                String b;
                String l;
                if (values.length == 1) {
                    t = r = b = l = values[0];
                } else if (values.length == 2) {
                    t = b = values[0];
                    r = l = values[1];
                } else if (values.length == 3) {
                    t = values[0];
                    r = l = values[1];
                    b = values[2];
                } else if (values.length >= 4) {
                    t = values[0];
                    r = values[1];
                    b = values[2];
                    l = values[3];
                } else {
                    return null;
                }
                top = top != null ? top : t;
                right = right != null ? right : r;
                bottom = bottom != null ? bottom : b;
                left = left != null ? left : l;
            }
            if (left == null && right == null && top == null && bottom == null) {
                return null;
            }
            left = left != null ? left : "0";
            right = right != null ? right : "0";
            top = top != null ? top : "0";
            bottom = bottom != null ? bottom : "0";
            if (!allowAuto) {
                for (String value : List.of(left, right, top, bottom)) {
                    if ("auto".equalsIgnoreCase(value)) {
                        throw new UnsupportedStyleException("auto not allowed for " + base);
                    }
                }
            }
            return new StyleCodegen.Edges(
                    edgeExpr(left, kind), edgeExpr(right, kind),
                    edgeExpr(top, kind), edgeExpr(bottom, kind), kind);
        }

        private static StyleCodegen.Edges edgesFromBorder(CssStyleMap css) {
            String left = css.get("border-left-width");
            String right = css.get("border-right-width");
            String top = css.get("border-top-width");
            String bottom = css.get("border-bottom-width");
            String border = css.get("border");
            if (border != null && (left == null || right == null || top == null || bottom == null)) {
                String[] parts = CssValues.splitWs(border);
                if (parts.length > 0) {
                    String maybe = parts[0];
                    if (maybe.endsWith("px") || "0".equals(maybe)) {
                        left = left != null ? left : maybe;
                        right = right != null ? right : maybe;
                        top = top != null ? top : maybe;
                        bottom = bottom != null ? bottom : maybe;
                    }
                }
            }
            if (left == null && right == null && top == null && bottom == null) {
                return null;
            }
            left = left != null ? left : "0";
            right = right != null ? right : "0";
            top = top != null ? top : "0";
            bottom = bottom != null ? bottom : "0";
            return new StyleCodegen.Edges(
                    CssValues.lpExpr(left), CssValues.lpExpr(right),
                    CssValues.lpExpr(top), CssValues.lpExpr(bottom), EdgeKind.LENGTH_PERCENTAGE);
        }

        private static StyleCodegen.Edges edgesFromInset(CssStyleMap css) {
            String left = css.get("left");
            String right = css.get("right");
            String top = css.get("top");
            String bottom = css.get("bottom");
            if (left == null && right == null && top == null && bottom == null) {
                return null;
            }
            left = left != null ? left : "auto";
            right = right != null ? right : "auto";
            top = top != null ? top : "auto";
            bottom = bottom != null ? bottom : "auto";
            return new StyleCodegen.Edges(
                    CssValues.lpaExpr(left), CssValues.lpaExpr(right),
                    CssValues.lpaExpr(top), CssValues.lpaExpr(bottom), EdgeKind.LENGTH_PERCENTAGE_AUTO);
        }

        private static String edgeExpr(String value, EdgeKind kind) {
            return kind == EdgeKind.LENGTH_PERCENTAGE
                    ? CssValues.lpExpr(value)
                    : CssValues.lpaExpr(value);
        }

        private static String lower(String value) {
            return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
        }
    }
}
