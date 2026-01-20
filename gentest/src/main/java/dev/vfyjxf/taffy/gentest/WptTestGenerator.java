package dev.vfyjxf.taffy.gentest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Generate JUnit tests from a local web-platform-tests (WPT) checkout.
 *
 * Design goals:
 * - WPT tests live under a dedicated package/directory (do NOT mix with taffy fixtures).
 * - Only support the subset of CSS/layout that Taffy supports (grid/flex + box model + abspos).
 * - Prefer a small allowlisted subset; generation should be explicit and reproducible.
 *
 * The generator uses headless Chrome to compute DOM layout (as an oracle), then generates
 * equivalent Taffy trees + asserts that Taffy produces the same geometry.
 */
public final class WptTestGenerator {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String OUT_BASE_PACKAGE = "dev.vfyjxf.taffy.wpt";

    // Fixed viewport used when collecting browser geometry.
    private static final int VIEWPORT_WIDTH = 800;
    private static final int VIEWPORT_HEIGHT = 600;

    // Keep generated files reasonably small to avoid javac running out of heap on a single giant file.
    private static final int MAX_TESTS_PER_CLASS = 20;

    private WptTestGenerator() {}

    public static void main(String[] args) throws Exception {
        Args parsed = Args.parse(args);

        Path wptRoot = parsed.wptRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(wptRoot)) {
            Path repaired = tryRepairPossiblyEscapedWindowsPath(wptRoot);
            if (repaired != null) {
                wptRoot = repaired;
            } else {
                throw new IllegalArgumentException(
                    "wptRoot is not a directory: " + wptRoot + "\n" +
                    "If you configured this path in gradle.properties, Windows backslashes must be escaped (\\)\n" +
                    "or use forward slashes (recommended): D:/path/to/wpt-root"
                );
            }
        }

        // Support a workspace layout where the WPT "css/" subtree is placed at the workspace root
        // (e.g. repoRoot/css/...) while other WPT infrastructure might live elsewhere.
        // In that setup, allowlisted files under css/* should be read from cssRoot, and
        // browser requests to /css/* should also be served from cssRoot.
        Path repoRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path cssRoot = parsed.cssRoot != null ? parsed.cssRoot.toAbsolutePath().normalize() : wptRoot;
        if (parsed.cssRoot != null && !Files.isDirectory(cssRoot)) {
            Path repaired = tryRepairPossiblyEscapedWindowsPath(cssRoot);
            if (repaired != null) {
                cssRoot = repaired;
            } else {
                throw new IllegalArgumentException(
                    "cssRoot is not a directory: " + cssRoot + "\n" +
                    "If you configured this path in gradle.properties, escape backslashes (\\) or use forward slashes."
                );
            }
        }
        if (parsed.cssRoot == null) {
            if (!Files.isDirectory(wptRoot.resolve("css")) && Files.isDirectory(repoRoot.resolve("css"))) {
                cssRoot = repoRoot;
            }
        }

        List<String> allowlist;
        if (parsed.discover) {
            allowlist = discoverAllTests(cssRoot, parsed.suites, parsed.maxFiles);
            if (allowlist.isEmpty()) {
                throw new IllegalArgumentException("No tests discovered under suites: " + parsed.suites + " (cssRoot=" + cssRoot + ")");
            }
        } else {
            allowlist = readAllowlist(parsed.allowlistFile);
            if (allowlist.isEmpty()) {
                throw new IllegalArgumentException("Allowlist is empty: " + parsed.allowlistFile);
            }
        }

        Path outputRoot = parsed.outputRoot.toAbsolutePath().normalize();
        Files.createDirectories(outputRoot);

        System.out.println("WPT root: " + wptRoot);
        System.out.println("CSS root: " + cssRoot);
        if (parsed.discover) {
            System.out.println("Allowlist: (discover mode)");
        } else {
            System.out.println("Allowlist: " + parsed.allowlistFile);
        }
        System.out.println("Output root: " + outputRoot);
        System.out.println("Viewport: " + VIEWPORT_WIDTH + "x" + VIEWPORT_HEIGHT);
        System.out.println("Tests to process: " + allowlist.size());
        if (parsed.acceptedOut != null) {
            System.out.println("Accepted-out: " + parsed.acceptedOut);
        }

        // Serve files over HTTP so absolute URLs like /resources/testharness.js resolve.
        LocalHttpServer server = LocalHttpServer.start(wptRoot, cssRoot);
        String baseUrl = server.baseUrl();
        System.out.println("WPT HTTP base: " + baseUrl);

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
            "--headless",
            "--disable-gpu",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--window-size=" + VIEWPORT_WIDTH + "," + VIEWPORT_HEIGHT
        );

        ChromeDriver driver = new ChromeDriver(options);
        try {
            Map<String, List<Fixture>> bySuite = new HashMap<>();
            List<String> accepted = parsed.acceptedOut != null ? new ArrayList<>() : null;

            for (String rel : allowlist) {
                Path testFile = resolveAllowlistedFile(wptRoot, cssRoot, rel);
                if (!Files.isRegularFile(testFile)) {
                    System.err.println("[skip] missing file: " + rel);
                    continue;
                }

                String suite = suiteFromPath(rel);
                if (suite == null) {
                    System.err.println("[skip] not a supported suite path: " + rel);
                    continue;
                }

                // Build unique method name by including ONLY the immediate parent subdirectory
                // to avoid conflicts while keeping the name short.
                // rel is like "css/css-grid/alignment/file.html" or "css/css-grid/grid-lanes/tentative/file.html"
                String relNorm = rel.replace('\\', '/');
                String prefix;
                if (relNorm.startsWith("css/css-grid/")) {
                    prefix = relNorm.substring("css/css-grid/".length());
                } else if (relNorm.startsWith("css/css-flexbox/")) {
                    prefix = relNorm.substring("css/css-flexbox/".length());
                } else {
                    prefix = relNorm;
                }
                // Remove file extension
                prefix = prefix.replaceAll("\\.(html|htm|xhtml|xht)$", "");
                
                // Only use immediate parent dir (if any) + filename, not the full path
                // e.g., "grid-lanes/tentative/grid-lanes-fragmentation-001" -> "tentative_grid_lanes_fragmentation_001"
                // e.g., "alignment/file-001" -> "alignment_file_001"
                String[] parts = prefix.split("/");
                String name;
                if (parts.length >= 2) {
                    // Use only immediate parent dir + filename
                    name = toSafeName(parts[parts.length - 2] + "_" + parts[parts.length - 1]);
                } else {
                    name = toSafeName(prefix);
                }
                bySuite.computeIfAbsent(suite, _k -> new ArrayList<>())
                    .add(new Fixture(rel, testFile, name));
            }

            for (Map.Entry<String, List<Fixture>> entry : bySuite.entrySet()) {
                String suite = entry.getKey();
                List<Fixture> fixtures = entry.getValue();

                String pkg = OUT_BASE_PACKAGE + "." + suite;
                String baseCls = switch (suite) {
                    case "grid" -> "WptGridTest";
                    case "flexbox" -> "WptFlexboxTest";
                    default -> "WptTest";
                };

                Path outDir = outputRoot.resolve(pkg.replace('.', '/'));
                Files.createDirectories(outDir);

                // If we are going to chunk, delete the legacy single-file output to prevent compilation of a huge stale file.
                if (fixtures.size() > MAX_TESTS_PER_CLASS) {
                    Files.deleteIfExists(outDir.resolve(baseCls + ".java"));
                }

                int totalOk = 0;
                int classIndex = 0;
                for (int start = 0; start < fixtures.size(); start += MAX_TESTS_PER_CLASS) {
                    int end = Math.min(start + MAX_TESTS_PER_CLASS, fixtures.size());
                    List<Fixture> chunk = fixtures.subList(start, end);

                    String cls = fixtures.size() <= MAX_TESTS_PER_CLASS
                        ? baseCls
                        : (baseCls + "_" + (classIndex + 1));

                    StringBuilder sb = new StringBuilder();
                    sb.append("package ").append(pkg).append(";\n\n");
                    sb.append("import dev.vfyjxf.taffy.geometry.*;\n");
                    sb.append("import dev.vfyjxf.taffy.style.*;\n");
                    sb.append("import dev.vfyjxf.taffy.tree.*;\n");
                    sb.append("import org.junit.jupiter.api.Test;\n");
                    sb.append("import org.junit.jupiter.api.DisplayName;\n");
                    sb.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
                    sb.append("/**\n");
                    sb.append(" * Generated from WPT.\n");
                    sb.append(" *\n");
                    sb.append(" * Source: https://github.com/web-platform-tests/wpt\n");
                    sb.append(" *\n");
                    sb.append(" * NOTE: These tests are generated and live under a dedicated package\n");
                    sb.append(" * (`").append(OUT_BASE_PACKAGE).append(".*`) and are intentionally\n");
                    sb.append(" * kept separate from taffy fixture-based tests.\n");
                    sb.append(" */\n");
                    sb.append("public class ").append(cls).append(" {\n\n");
                    sb.append("    private static final float EPSILON = 0.5f;\n\n");

                    int ok = 0;
                    for (Fixture f : chunk) {
                        try {
                            String method = generateOne(driver, wptRoot, baseUrl, f);
                            if (method != null) {
                                sb.append(method);
                                ok++;
                                if (accepted != null) {
                                    accepted.add(f.relPath);
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("[error] " + f.relPath + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    sb.append("}\n");

                    Path outFile = outDir.resolve(cls + ".java");
                    Files.writeString(outFile, sb.toString(), StandardCharsets.UTF_8);
                    System.out.println("Wrote " + ok + " tests to " + outFile);
                    totalOk += ok;
                    classIndex++;

                    // If we didn't chunk, we're done after one class.
                    if (fixtures.size() <= MAX_TESTS_PER_CLASS) {
                        break;
                    }
                }

                System.out.println("Suite '" + suite + "' generated tests: " + totalOk);
            }

            if (accepted != null) {
                // De-duplicate and keep stable order.
                List<String> unique = accepted.stream()
                    .map(s -> s.replace('\\', '/'))
                    .distinct()
                    .collect(Collectors.toList());
                Path parent = parsed.acceptedOut.toAbsolutePath().normalize().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(parsed.acceptedOut, String.join("\n", unique) + "\n", StandardCharsets.UTF_8);
                System.out.println("Wrote accepted allowlist: " + parsed.acceptedOut + " (" + unique.size() + " tests)");
            }

        } finally {
            driver.quit();
            server.close();
        }

        System.out.println("Done!");
    }

    private static String generateOne(ChromeDriver driver, Path wptRoot, String baseUrl, Fixture fixture) throws IOException {
        String url = baseUrl + (fixture.relPath.startsWith("/") ? fixture.relPath.substring(1) : fixture.relPath);
        driver.get(url);

        // Gather a minimal DOM tree with inline styles + a computed-style fallback + geometry.
        String json = Objects.toString(((JavascriptExecutor) driver).executeScript(JS_COLLECT), null);
        if (json == null || json.isBlank()) {
            System.err.println("[skip] no data returned: " + fixture.relPath);
            return null;
        }

        JsonObject data = GSON.fromJson(json, JsonObject.class);
        JsonObject viewport = data.getAsJsonObject("viewport");
        int vw = viewport.get("width").getAsInt();
        int vh = viewport.get("height").getAsInt();

        JsonArray nodes = data.getAsJsonArray("nodes");
        if (nodes == null || nodes.isEmpty()) {
            System.err.println("[skip] empty node list: " + fixture.relPath);
            return null;
        }

        // Build a lightweight tree model for generation.
        List<NodeInfo> infos = new ArrayList<>(nodes.size());
        for (JsonElement el : nodes) {
            JsonObject o = el.getAsJsonObject();
            NodeInfo ni = NodeInfo.from(o);
            infos.add(ni);
        }

        // Skip tests with too many nodes to avoid excessive memory/time.
        // 500 nodes is a reasonable limit for most tests.
        if (infos.size() > 500) {
            System.err.println("[skip] too many nodes (" + infos.size() + "): " + fixture.relPath);
            return null;
        }

        // Basic sanity: expect node[0] to be the <body> root.
        NodeInfo root = infos.getFirst();
        if (!"body".equalsIgnoreCase(root.tag)) {
            System.err.println("[skip] unexpected root tag (expected body): " + root.tag + " for " + fixture.relPath);
            return null;
        }

        // Parse styles; if we see unsupported syntax, skip this test for now.
        StyleModel styles;
        try {
            styles = StyleModel.fromDom(infos);
        } catch (UnsupportedStyleException ex) {
            System.err.println("[skip] unsupported style in " + fixture.relPath + ": " + ex.getMessage());
            return null;
        }

        String testName = fixture.name;
        String methodName = toMethodName(testName);
        int n = infos.size();

        // Children list per node.
        Map<Integer, List<Integer>> children = new HashMap<>();
        for (int i = 0; i < n; i++) {
            children.put(i, new ArrayList<>());
        }
        for (int i = 1; i < n; i++) {
            int p = infos.get(i).parentIndex;
            if (p >= 0 && p < n) {
                children.get(p).add(i);
            }
        }
        List<Integer> order = topoOrderBottomUp(children);

        // Find the "test root" - the first non-body node
        int testRootIndex = -1;
        for (int i = 1; i < n; i++) {
            NodeInfo ni = infos.get(i);
            if (ni.parentIndex == 0) {
                testRootIndex = i;
                break;
            }
        }

        // Pre-compute assertion data
        List<AssertionData> assertions = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            NodeInfo ni = infos.get(i);
            float relX = ni.rectX;
            float relY = ni.rectY;
            
            if (ni.parentIndex == 0 && i == testRootIndex) {
                NodeInfo body = infos.get(0);
                float bodyBorderLeft = parsePxValue(body.computed.get("border-left-width"));
                float bodyBorderTop = parsePxValue(body.computed.get("border-top-width"));
                float bodyPaddingLeft = parsePxValue(body.computed.get("padding-left"));
                float bodyPaddingTop = parsePxValue(body.computed.get("padding-top"));
                float marginLeft = parsePxValue(ni.computed.get("margin-left"));
                float marginTop = parsePxValue(ni.computed.get("margin-top"));
                relX = bodyPaddingLeft + bodyBorderLeft + marginLeft;
                relY = bodyPaddingTop + bodyBorderTop + marginTop;
            } else if (ni.parentIndex >= 0 && ni.parentIndex < n) {
                NodeInfo parent = infos.get(ni.parentIndex);
                relX = ni.rectX - parent.rectX;
                relY = ni.rectY - parent.rectY;
            }
            
            String nodeLabel = (ni.id != null && !ni.id.isBlank()) 
                ? ni.tag + "#" + ni.id 
                : ni.tag + "[" + i + "]";
            assertions.add(new AssertionData(i, nodeLabel, ni.rectW, ni.rectH, relX, relY));
        }

        // For small tests, use simple single-method generation
        // For larger tests, split into helper methods to avoid 65KB bytecode limit
        final int BATCH_SIZE = 15; // nodes per helper method
        boolean needsSplit = n > 25;
        
        // Determine which nodes need browser-computed size (leaf nodes with text content)
        Map<Integer, float[]> browserSizes = new HashMap<>();
        for (int i = 0; i < n; i++) {
            NodeInfo ni = infos.get(i);
            StyleSpec style = styles.styles.get(i);
            List<Integer> ch = children.get(i);
            if (ch.isEmpty() && style.needsMeasure() && (ni.rectW > 0 || ni.rectH > 0)) {
                browserSizes.put(i, new float[]{ni.rectW, ni.rectH});
            }
        }
        
        StringBuilder sb = new StringBuilder();
        
        if (needsSplit) {
            // Generate helper methods for styles
            int styleMethodCount = (n + BATCH_SIZE - 1) / BATCH_SIZE;
            for (int batch = 0; batch < styleMethodCount; batch++) {
                int start = batch * BATCH_SIZE;
                int end = Math.min(start + BATCH_SIZE, n);
                sb.append("    private void ").append(methodName).append("_styles_").append(batch);
                sb.append("(TaffyStyle[] styles, int vw, int vh, float[] browserW, float[] browserH) {\n");
                for (int i = start; i < end; i++) {
                    sb.append("        styles[").append(i).append("] = new TaffyStyle();\n");
                    float[] bs = browserSizes.get(i);
                    if (bs != null) {
                        sb.append("        // Leaf node with browser-computed size\n");
                        emitStyleAssignmentsWithBrowserSize(sb, "styles[" + i + "]", styles.styles.get(i), i == 0, i);
                    } else {
                        emitStyleAssignments(sb, "styles[" + i + "]", styles.styles.get(i), i == 0, vw, vh);
                    }
                    sb.append("\n");
                }
                sb.append("    }\n\n");
            }
            
            // Generate helper methods for node creation - now using simple newLeaf since sizes are explicit
            int nodeMethodCount = (order.size() + BATCH_SIZE - 1) / BATCH_SIZE;
            for (int batch = 0; batch < nodeMethodCount; batch++) {
                int start = batch * BATCH_SIZE;
                int end = Math.min(start + BATCH_SIZE, order.size());
                sb.append("    private void ").append(methodName).append("_nodes_").append(batch);
                sb.append("(TaffyTree tree, TaffyStyle[] styles, NodeId[] nodes) {\n");
                for (int j = start; j < end; j++) {
                    int idx = order.get(j);
                    List<Integer> ch = children.get(idx);
                    if (ch.isEmpty()) {
                        sb.append("        nodes[").append(idx).append("] = tree.newLeaf(styles[").append(idx).append("]);\n");
                    } else {
                        sb.append("        nodes[").append(idx).append("] = tree.newWithChildren(styles[").append(idx).append("], ");
                        for (int k = 0; k < ch.size(); k++) {
                            if (k > 0) sb.append(", ");
                            sb.append("nodes[").append(ch.get(k)).append("]");
                        }
                        sb.append(");\n");
                    }
                }
                sb.append("    }\n\n");
            }
            
            // Generate helper methods for assertions
            int assertMethodCount = (assertions.size() + BATCH_SIZE - 1) / BATCH_SIZE;
            for (int batch = 0; batch < assertMethodCount; batch++) {
                int start = batch * BATCH_SIZE;
                int end = Math.min(start + BATCH_SIZE, assertions.size());
                sb.append("    private void ").append(methodName).append("_assert_").append(batch);
                sb.append("(TaffyTree tree, NodeId[] nodes) {\n");
                for (int j = start; j < end; j++) {
                    AssertionData a = assertions.get(j);
                    sb.append("        // ").append(escapeJava(a.label)).append("\n");
                    sb.append("        Layout l").append(a.index).append(" = tree.getLayout(nodes[").append(a.index).append("]);\n");
                    sb.append("        assertEquals(").append(fmt(a.width)).append("f, l").append(a.index).append(".size().width, EPSILON, \"width\");\n");
                    sb.append("        assertEquals(").append(fmt(a.height)).append("f, l").append(a.index).append(".size().height, EPSILON, \"height\");\n");
                    sb.append("        assertEquals(").append(fmt(a.x)).append("f, l").append(a.index).append(".location().x, EPSILON, \"x\");\n");
                    sb.append("        assertEquals(").append(fmt(a.y)).append("f, l").append(a.index).append(".location().y, EPSILON, \"y\");\n\n");
                }
                sb.append("    }\n\n");
            }
            
            // Generate main test method that calls helpers
            sb.append("    @Test\n");
            sb.append("    @DisplayName(\"").append(escapeJava(fixture.relPath)).append("\")\n");
            sb.append("    void ").append(methodName).append("() {\n");
            sb.append("        TaffyTree tree = new TaffyTree();\n");
            sb.append("        tree.disableRounding();\n");
            sb.append("        int vw = ").append(vw).append(", vh = ").append(vh).append(";\n\n");
            
            // Prepare browser-computed size arrays for leaf nodes
            sb.append("        float[] browserW = new float[").append(n).append("];\n");
            sb.append("        float[] browserH = new float[").append(n).append("];\n");
            for (Map.Entry<Integer, float[]> e : browserSizes.entrySet()) {
                int idx = e.getKey();
                float[] bs = e.getValue();
                sb.append("        browserW[").append(idx).append("] = ").append(fmt(bs[0])).append("f; ");
                sb.append("browserH[").append(idx).append("] = ").append(fmt(bs[1])).append("f;\n");
            }
            sb.append("\n");
            
            sb.append("        TaffyStyle[] styles = new TaffyStyle[").append(n).append("];\n");
            for (int batch = 0; batch < styleMethodCount; batch++) {
                sb.append("        ").append(methodName).append("_styles_").append(batch).append("(styles, vw, vh, browserW, browserH);\n");
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
        } else {
            // Small test - use simple single method
            sb.append("    @Test\n");
            sb.append("    @DisplayName(\"").append(escapeJava(fixture.relPath)).append("\")\n");
            sb.append("    void ").append(methodName).append("() {\n");
            sb.append("        TaffyTree tree = new TaffyTree();\n");
            sb.append("        tree.disableRounding();\n\n");
            sb.append("        // Root viewport: ").append(vw).append("x").append(vh).append("\n");

            // Emit Style declarations in index order.
            for (int i = 0; i < n; i++) {
                sb.append("        TaffyStyle s").append(i).append(" = new TaffyStyle();\n");
                float[] bs = browserSizes.get(i);
                if (bs != null) {
                    emitStyleAssignments(sb, "s" + i, styles.styles.get(i), i == 0, vw, vh, bs[0], bs[1]);
                } else {
                    emitStyleAssignments(sb, "s" + i, styles.styles.get(i), i == 0, vw, vh);
                }
                sb.append("\n");
            }

            // Emit NodeId declarations.
            for (int i = 0; i < n; i++) {
                sb.append("        NodeId n").append(i).append(";\n");
            }
            sb.append("\n");

            // Create nodes bottom-up - use newLeaf for all since sizes are now explicit in style
            for (int idx : order) {
                List<Integer> ch = children.get(idx);
                if (ch.isEmpty()) {
                    sb.append("        n").append(idx).append(" = tree.newLeaf(s").append(idx).append(");\n");
                } else {
                    sb.append("        n").append(idx).append(" = tree.newWithChildren(s").append(idx).append(", ");
                    for (int j = 0; j < ch.size(); j++) {
                        if (j > 0) sb.append(", ");
                        sb.append("n").append(ch.get(j));
                    }
                    sb.append(");\n");
                }
            }

            sb.append("\n");
            sb.append("        tree.computeLayout(n0, TaffySize.maxContent());\n\n");

            // Emit assertions
            for (AssertionData a : assertions) {
                sb.append("        // ").append(escapeJava(a.label)).append("\n");
                sb.append("        Layout l").append(a.index).append(" = tree.getLayout(n").append(a.index).append(");\n");
                sb.append("        assertEquals(").append(fmt(a.width)).append("f, l").append(a.index).append(".size().width, EPSILON, \"width\");\n");
                sb.append("        assertEquals(").append(fmt(a.height)).append("f, l").append(a.index).append(".size().height, EPSILON, \"height\");\n");
                sb.append("        assertEquals(").append(fmt(a.x)).append("f, l").append(a.index).append(".location().x, EPSILON, \"x\");\n");
                sb.append("        assertEquals(").append(fmt(a.y)).append("f, l").append(a.index).append(".location().y, EPSILON, \"y\");\n\n");
            }

            sb.append("    }\n\n");
        }

        return sb.toString();
    }
    
    private record AssertionData(int index, String label, float width, float height, float x, float y) {}

    /**
     * Very small static HTTP server to serve files from a local WPT root.
     *
     * This is needed because many WPT tests reference resources via absolute URLs
     * (e.g. /resources/testharness.js) which do not work when opening a file:// URL.
     */
    private static final class LocalHttpServer implements AutoCloseable {
        private final HttpServer server;
        private final Path root;
        private final Path cssRoot;
        private final int port;

        private LocalHttpServer(HttpServer server, Path root, Path cssRoot, int port) {
            this.server = server;
            this.root = root;
            this.cssRoot = cssRoot;
            this.port = port;
        }

        static LocalHttpServer start(Path root, Path cssRoot) throws IOException {
            HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            LocalHttpServer wrapper = new LocalHttpServer(
                s,
                root.toAbsolutePath().normalize(),
                cssRoot != null ? cssRoot.toAbsolutePath().normalize() : null,
                s.getAddress().getPort()
            );
            s.createContext("/", wrapper::handle);
            s.start();
            return wrapper;
        }

        String baseUrl() {
            return "http://127.0.0.1:" + port + "/";
        }

        private void handle(HttpExchange ex) throws IOException {
            String rawPath = Objects.toString(ex.getRequestURI().getRawPath(), "/");
            String path = URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
            if (path.startsWith("/")) path = path.substring(1);
            if (path.isBlank()) {
                sendText(ex, 404, "Not Found");
                return;
            }

            Path file = resolveRequestPath(path);
            if (file == null || !Files.exists(file) || Files.isDirectory(file)) {
                sendText(ex, 404, "Not Found");
                return;
            }

            String contentType = contentTypeFor(file);
            Headers h = ex.getResponseHeaders();
            h.set("Content-Type", contentType);
            // Avoid caching surprises while iterating.
            h.set("Cache-Control", "no-store");

            byte[] bytes = Files.readAllBytes(file);
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }

        private Path resolveRequestPath(String path) {
            String p = path.replace('\\', '/');

            if (cssRoot != null && p.startsWith("css/")) {
                Path fromCssRoot = safeResolve(cssRoot, p);
                if (fromCssRoot != null && Files.exists(fromCssRoot)) {
                    return fromCssRoot;
                }
            }

            return safeResolve(root, p);
        }

        private static Path safeResolve(Path base, String path) {
            Path file = base.resolve(path).normalize();
            if (!file.startsWith(base)) {
                return null;
            }
            return file;
        }

        private static void sendText(HttpExchange ex, int code, String text) throws IOException {
            byte[] b = text.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            ex.sendResponseHeaders(code, b.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(b);
            }
        }

        private static String contentTypeFor(Path file) {
            String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.endsWith(".html") || name.endsWith(".htm") || name.endsWith(".xhtml") || name.endsWith(".xht")) {
                return "text/html; charset=utf-8";
            }
            if (name.endsWith(".css")) {
                return "text/css; charset=utf-8";
            }
            if (name.endsWith(".js")) {
                return "application/javascript; charset=utf-8";
            }
            if (name.endsWith(".json")) {
                return "application/json; charset=utf-8";
            }
            if (name.endsWith(".ttf")) {
                return "font/ttf";
            }
            if (name.endsWith(".woff")) {
                return "font/woff";
            }
            if (name.endsWith(".woff2")) {
                return "font/woff2";
            }
            if (name.endsWith(".png")) {
                return "image/png";
            }
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                return "image/jpeg";
            }
            if (name.endsWith(".gif")) {
                return "image/gif";
            }
            return "application/octet-stream";
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private static void emitStyleAssignments(StringBuilder sb, String var, StyleSpec style, boolean isRoot, int vw, int vh) {
        emitStyleAssignments(sb, var, style, isRoot, vw, vh, Float.NaN, Float.NaN);
    }

    /**
     * Emit style assignments for split tests where browser sizes come from arrays.
     * @param idx the node index for accessing browserW/browserH arrays
     */
    private static void emitStyleAssignmentsWithBrowserSize(StringBuilder sb, String var, StyleSpec style, boolean isRoot, int idx) {
        if (style.display != null) {
            sb.append("        ").append(var).append(".display = TaffyDisplay.").append(style.display).append(";\n");
        }
        if (style.position != null) {
            sb.append("        ").append(var).append(".position = TaffyPosition.").append(style.position).append(";\n");
        }
        if (style.boxSizing != null) {
            sb.append("        ").append(var).append(".boxSizing = BoxSizing.").append(style.boxSizing).append(";\n");
        }
        if (style.direction != null) {
            sb.append("        ").append(var).append(".direction = TaffyDirection.").append(style.direction).append(";\n");
        }
        
        // Use browser-computed size from arrays
        sb.append("        ").append(var).append(".size = new TaffySize<>(TaffyDimension.length(browserW[").append(idx).append("]), TaffyDimension.length(browserH[").append(idx).append("]));\n");
        
        if (style.minWidth != null || style.minHeight != null) {
            String w = style.minWidth != null ? style.minWidth : "TaffyDimension.AUTO";
            String h = style.minHeight != null ? style.minHeight : "TaffyDimension.AUTO";
            sb.append("        ").append(var).append(".minSize = new TaffySize<>(").append(w).append(", ").append(h).append(");\n");
        }
        if (style.maxWidth != null || style.maxHeight != null) {
            String w = style.maxWidth != null ? style.maxWidth : "TaffyDimension.AUTO";
            String h = style.maxHeight != null ? style.maxHeight : "TaffyDimension.AUTO";
            sb.append("        ").append(var).append(".maxSize = new TaffySize<>(").append(w).append(", ").append(h).append(");\n");
        }

        // Emit aspect-ratio if present
        if (style.aspectRatio != null) {
            sb.append("        ").append(var).append(".aspectRatio = ").append(style.aspectRatio).append("f;\n");
        }

        if (style.margin != null) {
            sb.append("        ").append(var).append(".margin = ").append(style.margin).append(";\n");
        }
        if (style.padding != null) {
            sb.append("        ").append(var).append(".padding = ").append(style.padding).append(";\n");
        }
        if (style.border != null) {
            sb.append("        ").append(var).append(".border = ").append(style.border).append(";\n");
        }
        if (style.inset != null) {
            sb.append("        ").append(var).append(".inset = ").append(style.inset).append(";\n");
        }
        if (style.flexDirection != null) {
            sb.append("        ").append(var).append(".flexDirection = FlexDirection.").append(style.flexDirection).append(";\n");
        }
        if (style.flexWrap != null) {
            sb.append("        ").append(var).append(".flexWrap = FlexWrap.").append(style.flexWrap).append(";\n");
        }
        // Flex properties - direct field assignment
        if (style.flexGrow != null) {
            sb.append("        ").append(var).append(".flexGrow = ").append(fmt(style.flexGrow)).append("f;\n");
        }
        if (style.flexShrink != null) {
            sb.append("        ").append(var).append(".flexShrink = ").append(fmt(style.flexShrink)).append("f;\n");
        }
        if (style.flexBasis != null) {
            sb.append("        ").append(var).append(".flexBasis = ").append(style.flexBasis).append(";\n");
        }
        if (style.gap != null) {
            sb.append("        ").append(var).append(".gap = ").append(style.gap).append(";\n");
        }
        if (style.alignItems != null) {
            sb.append("        ").append(var).append(".alignItems = AlignItems.").append(style.alignItems).append(";\n");
        }
        if (style.alignContent != null) {
            sb.append("        ").append(var).append(".alignContent = AlignContent.").append(style.alignContent).append(";\n");
        }
        if (style.justifyContent != null) {
            sb.append("        ").append(var).append(".justifyContent = JustifyContent.").append(style.justifyContent).append(";\n");
        }
        if (style.alignSelf != null) {
            sb.append("        ").append(var).append(".alignSelf = AlignSelf.").append(style.alignSelf).append(";\n");
        }
        if (style.justifySelf != null) {
            sb.append("        ").append(var).append(".justifySelf = JustifySelf.").append(style.justifySelf).append(";\n");
        }
        if (style.gridTemplateColumns != null || style.gridTemplateColumnsWithRepeat != null) {
            sb.append("        ").append(var).append(".gridTemplateColumns = ")
              .append(style.gridTemplateColumnsWithRepeat != null ? style.gridTemplateColumnsWithRepeat : style.gridTemplateColumns).append(";\n");
        }
        if (style.gridTemplateRows != null || style.gridTemplateRowsWithRepeat != null) {
            sb.append("        ").append(var).append(".gridTemplateRows = ")
              .append(style.gridTemplateRowsWithRepeat != null ? style.gridTemplateRowsWithRepeat : style.gridTemplateRows).append(";\n");
        }
        if (style.gridAutoColumns != null) {
            sb.append("        ").append(var).append(".gridAutoColumns = ").append(style.gridAutoColumns).append(";\n");
        }
        if (style.gridAutoRows != null) {
            sb.append("        ").append(var).append(".gridAutoRows = ").append(style.gridAutoRows).append(";\n");
        }
        if (style.gridAutoFlow != null) {
            sb.append("        ").append(var).append(".gridAutoFlow = GridAutoFlow.").append(style.gridAutoFlow).append(";\n");
        }
        if (style.gridColumn != null) {
            sb.append("        ").append(var).append(".gridColumn = ").append(style.gridColumn).append(";\n");
        }
        if (style.gridRow != null) {
            sb.append("        ").append(var).append(".gridRow = ").append(style.gridRow).append(";\n");
        }
    }

    /**
     * Emit style assignments, optionally overriding size with browser-computed values for leaf nodes.
     * @param browserW browser-computed width (NaN to use style's width)
     * @param browserH browser-computed height (NaN to use style's height)
     */
    private static void emitStyleAssignments(StringBuilder sb, String var, StyleSpec style, boolean isRoot, int vw, int vh, float browserW, float browserH) {
        // Root: force a definite size matching the viewport so abspos/percentages have a stable containing block.
        if (isRoot) {
            sb.append("        ").append(var).append(".size = new TaffySize<>(TaffyDimension.length(").append(vw).append("f), TaffyDimension.length(").append(vh).append("f));\n");
        }

        if (style.display != null) {
            sb.append("        ").append(var).append(".display = TaffyDisplay.").append(style.display).append(";\n");
        }
        if (style.position != null) {
            sb.append("        ").append(var).append(".position = TaffyPosition.").append(style.position).append(";\n");
        }
        if (style.boxSizing != null) {
            sb.append("        ").append(var).append(".boxSizing = BoxSizing.").append(style.boxSizing).append(";\n");
        }
        if (style.direction != null) {
            sb.append("        ").append(var).append(".direction = TaffyDirection.").append(style.direction).append(";\n");
        }

        // Use browser-computed size for leaf nodes that need intrinsic sizing
        boolean useBrowserSize = !Float.isNaN(browserW) || !Float.isNaN(browserH);
        if (useBrowserSize) {
            String w = !Float.isNaN(browserW) ? "TaffyDimension.length(" + fmt(browserW) + "f)" : (style.width != null ? style.width : "TaffyDimension.AUTO");
            String h = !Float.isNaN(browserH) ? "TaffyDimension.length(" + fmt(browserH) + "f)" : (style.height != null ? style.height : "TaffyDimension.AUTO");
            sb.append("        ").append(var).append(".size = new TaffySize<>(").append(w).append(", ").append(h).append(");\n");
        } else if (style.width != null || style.height != null) {
            String w = style.width != null ? style.width : "TaffyDimension.AUTO";
            String h = style.height != null ? style.height : "TaffyDimension.AUTO";
            sb.append("        ").append(var).append(".size = new TaffySize<>(").append(w).append(", ").append(h).append(");\n");
        }
        if (style.minWidth != null || style.minHeight != null) {
            String w = style.minWidth != null ? style.minWidth : "TaffyDimension.AUTO";
            String h = style.minHeight != null ? style.minHeight : "TaffyDimension.AUTO";
            sb.append("        ").append(var).append(".minSize = new TaffySize<>(").append(w).append(", ").append(h).append(");\n");
        }
        if (style.maxWidth != null || style.maxHeight != null) {
            String w = style.maxWidth != null ? style.maxWidth : "TaffyDimension.AUTO";
            String h = style.maxHeight != null ? style.maxHeight : "TaffyDimension.AUTO";
            sb.append("        ").append(var).append(".maxSize = new TaffySize<>(").append(w).append(", ").append(h).append(");\n");
        }

        // Emit aspect-ratio if present
        if (style.aspectRatio != null) {
            sb.append("        ").append(var).append(".aspectRatio = ").append(style.aspectRatio).append("f;\n");
        }

        if (style.margin != null) {
            sb.append("        ").append(var).append(".margin = ").append(style.margin).append(";\n");
        }
        if (style.padding != null) {
            sb.append("        ").append(var).append(".padding = ").append(style.padding).append(";\n");
        }
        if (style.border != null) {
            sb.append("        ").append(var).append(".border = ").append(style.border).append(";\n");
        }
        if (style.inset != null) {
            sb.append("        ").append(var).append(".inset = ").append(style.inset).append(";\n");
        }

        if (style.flexDirection != null) {
            sb.append("        ").append(var).append(".flexDirection = FlexDirection.").append(style.flexDirection).append(";\n");
        }
        if (style.flexWrap != null) {
            sb.append("        ").append(var).append(".flexWrap = FlexWrap.").append(style.flexWrap).append(";\n");
        }
        // Flex properties - direct field assignment
        if (style.flexGrow != null) {
            sb.append("        ").append(var).append(".flexGrow = ").append(style.flexGrow).append("f;\n");
        }
        if (style.flexShrink != null) {
            sb.append("        ").append(var).append(".flexShrink = ").append(style.flexShrink).append("f;\n");
        }
        if (style.flexBasis != null) {
            sb.append("        ").append(var).append(".flexBasis = ").append(style.flexBasis).append(";\n");
        }

        if (style.alignItems != null) {
            sb.append("        ").append(var).append(".alignItems = AlignItems.").append(style.alignItems).append(";\n");
        }
        if (style.alignContent != null) {
            sb.append("        ").append(var).append(".alignContent = AlignContent.").append(style.alignContent).append(";\n");
        }
        if (style.justifyContent != null) {
            sb.append("        ").append(var).append(".justifyContent = AlignContent.").append(style.justifyContent).append(";\n");
        }
        if (style.alignSelf != null) {
            sb.append("        ").append(var).append(".alignSelf = AlignItems.").append(style.alignSelf).append(";\n");
        }
        if (style.justifySelf != null) {
            sb.append("        ").append(var).append(".justifySelf = AlignItems.").append(style.justifySelf).append(";\n");
        }

        if (style.gap != null) {
            sb.append("        ").append(var).append(".gap = ").append(style.gap).append(";\n");
        }

        if (style.gridTemplateColumnsWithRepeat != null) {
            sb.append("        ").append(var).append(".gridTemplateColumnsWithRepeat = ").append(style.gridTemplateColumnsWithRepeat).append(";\n");
        }
        if (style.gridTemplateRowsWithRepeat != null) {
            sb.append("        ").append(var).append(".gridTemplateRowsWithRepeat = ").append(style.gridTemplateRowsWithRepeat).append(";\n");
        }
        if (style.gridTemplateColumns != null) {
            sb.append("        ").append(var).append(".gridTemplateColumns = ").append(style.gridTemplateColumns).append(";\n");
        }
        if (style.gridTemplateRows != null) {
            sb.append("        ").append(var).append(".gridTemplateRows = ").append(style.gridTemplateRows).append(";\n");
        }
        if (style.gridAutoColumns != null) {
            sb.append("        ").append(var).append(".gridAutoColumns = ").append(style.gridAutoColumns).append(";\n");
        }
        if (style.gridAutoRows != null) {
            sb.append("        ").append(var).append(".gridAutoRows = ").append(style.gridAutoRows).append(";\n");
        }
        if (style.gridAutoFlow != null) {
            sb.append("        ").append(var).append(".gridAutoFlow = GridAutoFlow.").append(style.gridAutoFlow).append(";\n");
        }
        if (style.gridColumn != null) {
            sb.append("        ").append(var).append(".gridColumn = ").append(style.gridColumn).append(";\n");
        }
        if (style.gridRow != null) {
            sb.append("        ").append(var).append(".gridRow = ").append(style.gridRow).append(";\n");
        }
    }

    private static List<Integer> topoOrderBottomUp(Map<Integer, List<Integer>> children) {
        // Post-order traversal starting from 0.
        List<Integer> out = new ArrayList<>();
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, 0});
        while (!stack.isEmpty()) {
            int[] frame = stack.peek();
            int node = frame[0];
            int idx = frame[1];
            List<Integer> ch = children.getOrDefault(node, List.of());
            if (idx < ch.size()) {
                int child = ch.get(idx);
                frame[1] = idx + 1;
                stack.push(new int[]{child, 0});
            } else {
                stack.pop();
                out.add(node);
            }
        }
        return out;
    }

    private static List<String> readAllowlist(Path allowlistFile) throws IOException {
        List<String> lines = Files.readAllLines(allowlistFile, StandardCharsets.UTF_8);
        return lines.stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .filter(s -> !s.startsWith("#"))
            .collect(Collectors.toList());
    }

    private static String suiteFromPath(String rel) {
        String p = rel.replace('\\', '/');
        if (p.startsWith("css/css-grid/")) return "grid";
        if (p.startsWith("css/css-flexbox/")) return "flexbox";
        return null;
    }

    private static Path resolveAllowlistedFile(Path wptRoot, Path cssRoot, String rel) {
        String p = rel.replace('\\', '/');
        if (p.startsWith("css/")) {
            return cssRoot.resolve(p).normalize();
        }
        return wptRoot.resolve(p).normalize();
    }

    /**
     * Heuristic repair for Windows paths that lost backslashes.
     *
     * A common way this happens is setting a path in gradle.properties:
     *   wptRoot=D:\\foo\\bar\\css
     * If you write single backslashes, Java properties treat them as escapes and may drop them,
     * producing something like D:\foo\barcss.
     */
    private static Path tryRepairPossiblyEscapedWindowsPath(Path p) {
        try {
            if (Files.isDirectory(p)) return p;
        } catch (Exception ignored) {
            // If Files throws due to malformed path, fall through to string-based repair.
        }

        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) {
            return null;
        }

        String raw = p.toString();
        // Only attempt repair if we see a suspicious concatenation like "...barcss" or "...barresources".
        String[] tokens = new String[]{"css", "resources"};
        for (String token : tokens) {
            int idx = raw.toLowerCase(Locale.ROOT).lastIndexOf(token);
            if (idx <= 0) continue;

            // If it's already separated, don't touch it.
            char prev = raw.charAt(idx - 1);
            if (prev == '\\' || prev == '/') continue;

            // Insert the current platform separator before the token.
            String candidate = raw.substring(0, idx) + java.io.File.separator + raw.substring(idx);
            try {
                Path repaired = Paths.get(candidate).toAbsolutePath().normalize();
                if (Files.isDirectory(repaired)) {
                    System.err.println("[warn] Repaired path: " + raw + " -> " + repaired);
                    return repaired;
                }
            } catch (Exception ignored) {
                // try next token
            }
        }

        return null;
    }

    private static List<String> discoverAllTests(Path cssRoot, List<String> suites, int maxFiles) throws IOException {
        List<String> out = new ArrayList<>();
        for (String suiteRel : suites) {
            String suitePath = suiteRel.replace('\\', '/');
            while (suitePath.startsWith("/")) suitePath = suitePath.substring(1);
            Path suiteDir = cssRoot.resolve(suitePath).normalize();
            if (!Files.isDirectory(suiteDir)) {
                System.err.println("[warn] suite dir not found: " + suiteDir);
                continue;
            }

            try (Stream<Path> stream = Files.walk(suiteDir)) {
                List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());

                for (Path p : files) {
                    if (maxFiles > 0 && out.size() >= maxFiles) break;

                    String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (!(name.endsWith(".html") || name.endsWith(".htm") || name.endsWith(".xhtml") || name.endsWith(".xht"))) {
                        continue;
                    }

                    // Keep this as non-conservative as possible while still avoiding obvious non-tests.
                    if (name.endsWith("-ref.html") || name.endsWith("-notref.html")) continue;
                    if (name.contains("-manual")) continue;

                    String rel = cssRoot.relativize(p.toAbsolutePath().normalize()).toString().replace('\\', '/');
                    // Skip support/reference/resources folders.
                    if (rel.contains("/support/") || rel.contains("/_support/") || rel.contains("/reference/") || rel.contains("/resources/")) {
                        continue;
                    }

                    out.add(rel);
                }
            }

            if (maxFiles > 0 && out.size() >= maxFiles) break;
        }

        // Stable order helps debugging & reproducibility.
        return out.stream().distinct().sorted().collect(Collectors.toList());
    }

    private static String toSafeName(String s) {
        return s.replaceAll("[^a-zA-Z0-9_]+", "_");
    }

    private static String toMethodName(String name) {
        String base = toSafeName(name);
        if (base.isEmpty()) base = "test";
        if (!Character.isJavaIdentifierStart(base.charAt(0))) {
            base = "t_" + base;
        }
        return base;
    }

    private static String escapeJava(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String fmt(float v) {
        // Keep numbers compact but stable.
        if (Float.isNaN(v)) return "Float.NaN";
        return String.format(Locale.ROOT, "%.3f", v);
    }

    /**
     * Parse a CSS pixel value like "10px" into a float. Returns 0 if the value is null/empty/unparseable.
     */
    private static float parsePxValue(String v) {
        if (v == null || v.isBlank()) return 0f;
        v = v.trim().toLowerCase(Locale.ROOT);
        if ("0".equals(v)) return 0f;
        if (v.endsWith("px")) {
            try {
                return Float.parseFloat(v.substring(0, v.length() - 2).trim());
            } catch (NumberFormatException e) {
                return 0f;
            }
        }
        return 0f;
    }

    private record Fixture(String relPath, Path path, String name) {}

    private record Args(Path wptRoot, Path allowlistFile, Path outputRoot, Path acceptedOut, Path cssRoot,
                        boolean discover, List<String> suites, int maxFiles) {
        static Args parse(String[] args) {
            Path wptRoot = null;
            Path allowlist = null;
            Path out = null;
            Path acceptedOut = null;
            Path cssRoot = null;
            boolean discover = false;
            List<String> suites = new ArrayList<>();
            int maxFiles = 0;

            for (int i = 0; i < args.length; i++) {
                String a = Objects.toString(args[i], "").trim();
                switch (a) {
                    case "--wptRoot" -> {
                        if (i + 1 >= args.length) throw new IllegalArgumentException("--wptRoot requires a value");
                        wptRoot = Paths.get(args[++i]);
                    }
                    case "--allowlist" -> {
                        if (i + 1 >= args.length) throw new IllegalArgumentException("--allowlist requires a value");
                        allowlist = Paths.get(args[++i]);
                    }
                    case "--outRoot" -> {
                        if (i + 1 >= args.length) throw new IllegalArgumentException("--outRoot requires a value");
                        out = Paths.get(args[++i]);
                    }
                    case "--acceptedOut" -> {
                        if (i + 1 >= args.length) throw new IllegalArgumentException("--acceptedOut requires a value");
                        acceptedOut = Paths.get(args[++i]);
                    }
                    case "--cssRoot" -> {
                        if (i + 1 >= args.length) throw new IllegalArgumentException("--cssRoot requires a value");
                        cssRoot = Paths.get(args[++i]);
                    }
                    case "--discover" -> discover = true;
                    case "--suites" -> {
                        if (i + 1 >= args.length) throw new IllegalArgumentException("--suites requires a value");
                        String v = Objects.toString(args[++i], "").trim();
                        for (String part : v.split(",")) {
                            String trimmed = part.trim();
                            if (!trimmed.isEmpty()) suites.add(trimmed);
                        }
                    }
                    case "--maxFiles" -> {
                        if (i + 1 >= args.length) throw new IllegalArgumentException("--maxFiles requires a value");
                        maxFiles = Integer.parseInt(args[++i]);
                    }
                    default -> {
                        // ignore
                    }
                }
            }

            if (wptRoot == null) throw new IllegalArgumentException("Missing required argument: --wptRoot <path>");
            if (!discover && allowlist == null) {
                throw new IllegalArgumentException("Missing required argument: --allowlist <file> (or pass --discover)");
            }
            if (out == null) {
                // Default to taffy-java/src/test/java/dev/vfyjxf/taffy/wpt (relative to workspace root)
                Path rootDir = Paths.get(System.getProperty("user.dir"));
                out = rootDir.resolve("taffy-java/src/test/java");
            }

            if (suites.isEmpty()) {
                suites.add("css/css-grid");
                suites.add("css/css-flexbox");
            }

            return new Args(wptRoot, allowlist, out, acceptedOut, cssRoot, discover, suites, maxFiles);
        }
    }

    // -------------------- DOM data model --------------------

    private static final class NodeInfo {
        final int index;
        final int parentIndex;
        final String tag;
        final String id;
        final String className;
        final String inlineStyle;
        final Map<String, String> computed;
        final float rectX;
        final float rectY;
        final float rectW;
        final float rectH;

        private NodeInfo(int index, int parentIndex, String tag, String id, String className, String inlineStyle,
                         Map<String, String> computed, float rectX, float rectY, float rectW, float rectH) {
            this.index = index;
            this.parentIndex = parentIndex;
            this.tag = tag;
            this.id = id;
            this.className = className;
            this.inlineStyle = inlineStyle;
            this.computed = computed;
            this.rectX = rectX;
            this.rectY = rectY;
            this.rectW = rectW;
            this.rectH = rectH;
        }

        static NodeInfo from(JsonObject o) {
            int index = o.get("index").getAsInt();
            int parent = o.get("parent").getAsInt();
            String tag = o.get("tag").getAsString();
            String id = "";
            if (o.has("id")) {
                JsonElement idElem = o.get("id");
                if (idElem.isJsonPrimitive()) {
                    id = idElem.getAsString();
                }
            }
            String cls = "";
            if (o.has("className")) {
                JsonElement clsElem = o.get("className");
                if (clsElem.isJsonPrimitive()) {
                    cls = clsElem.getAsString();
                }
            }
            String style = o.has("style") ? o.get("style").getAsString() : "";

            Map<String, String> computed = new HashMap<>();
            if (o.has("computed")) {
                JsonObject c = o.getAsJsonObject("computed");
                for (Map.Entry<String, JsonElement> e : c.entrySet()) {
                    computed.put(e.getKey(), e.getValue().getAsString());
                }
            }

            JsonObject r = o.getAsJsonObject("rect");
            float x = r.get("x").getAsFloat();
            float y = r.get("y").getAsFloat();
            float w = r.get("w").getAsFloat();
            float h = r.get("h").getAsFloat();

            return new NodeInfo(index, parent, tag, id, cls, style, computed, x, y, w, h);
        }
    }

    // -------------------- Style mapping --------------------

    private static final class StyleModel {
        final List<StyleSpec> styles;

        private StyleModel(List<StyleSpec> styles) {
            this.styles = styles;
        }

        static StyleModel fromDom(List<NodeInfo> nodes) {
            List<StyleSpec> out = new ArrayList<>(nodes.size());
            for (NodeInfo n : nodes) {
                CssStyleMap map = CssStyleMap.from(n.inlineStyle, n.computed);
                out.add(StyleSpec.fromCss(map));
            }
            return new StyleModel(out);
        }
    }

    private static final class StyleSpec {
        // Enum names as strings, emitted into generated Java.
        String display;
        String position;
        String boxSizing;
        String direction;

        String width;
        String height;
        String minWidth;
        String minHeight;
        String maxWidth;
        String maxHeight;
        String aspectRatio;  // e.g., "1.5" or null
        

        /**
         * Check if this style has no intrinsic sizing, meaning a leaf node with this style
         * needs a measure function to provide its natural size.
         */
        boolean needsMeasure() {
            // If explicit width or height is set (not AUTO), no measure needed
            if (width != null && !width.equals("TaffyDimension.AUTO")) return false;
            if (height != null && !height.equals("TaffyDimension.AUTO")) return false;
            // Containers (flex, grid, block) don't need measure - their size comes from children
            if (display != null && (display.equals("FLEX") || display.equals("GRID") || display.equals("BLOCK"))) {
                return false;
            }
            // Everything else (inline elements, text, etc.) with AUTO size needs measure
            return true;
        }

        String margin;
        String padding;
        String border;
        String inset;

        String flexDirection;
        String flexWrap;
        Float flexGrow;
        Float flexShrink;
        String flexBasis;

        String alignItems;
        String alignContent;
        String justifyContent;
        String alignSelf;
        String justifySelf;

        String gap;

        String gridTemplateColumns;
        String gridTemplateColumnsWithRepeat;
        String gridTemplateRows;
        String gridTemplateRowsWithRepeat;
        String gridAutoColumns;
        String gridAutoRows;
        String gridAutoFlow;

        String gridRow;
        String gridColumn;

        static StyleSpec fromCss(CssStyleMap css) {
            StyleSpec s = new StyleSpec();

            String display = css.get("display");
            if (display != null) {
                display = display.toLowerCase(Locale.ROOT);
                s.display = switch (display) {
                    case "flex" -> "FLEX";
                    case "grid" -> "GRID";
                    case "block", "flow-root" -> "BLOCK";
                    case "none" -> "NONE";
                    default -> null;
                };
            }

            String pos = css.get("position");
            if (pos != null) {
                pos = pos.toLowerCase(Locale.ROOT);
                s.position = switch (pos) {
                    case "absolute" -> "ABSOLUTE";
                    case "relative" -> "RELATIVE";
                    default -> null;
                };
            }

            String bs = css.get("box-sizing");
            if (bs != null) {
                bs = bs.toLowerCase(Locale.ROOT);
                s.boxSizing = switch (bs) {
                    case "border-box" -> "BORDER_BOX";
                    case "content-box" -> "CONTENT_BOX";
                    default -> null;
                };
            }

            String dir = css.get("direction");
            if (dir != null) {
                dir = dir.toLowerCase(Locale.ROOT);
                s.direction = switch (dir) {
                    case "ltr" -> "LTR";
                    case "rtl" -> "RTL";
                    default -> null;
                };
            }

            s.width = dimExpr(css.get("width"));
            s.height = dimExpr(css.get("height"));
            s.minWidth = dimExpr(css.get("min-width"));
            s.minHeight = dimExpr(css.get("min-height"));
            s.maxWidth = dimExpr(css.get("max-width"));
            s.maxHeight = dimExpr(css.get("max-height"));
            
            // Parse aspect-ratio (e.g., "1/1", "16/9", "1.5", "auto")
            String ar = css.get("aspect-ratio");
            if (ar != null && !ar.isEmpty() && !ar.equals("auto")) {
                ar = ar.trim();
                if (ar.contains("/")) {
                    // Format: "W / H" or "W/H"
                    String[] parts = ar.split("\\s*/\\s*");
                    if (parts.length == 2) {
                        try {
                            float w = Float.parseFloat(parts[0].trim());
                            float h = Float.parseFloat(parts[1].trim());
                            if (h != 0) {
                                s.aspectRatio = String.valueOf(w / h);
                            }
                        } catch (NumberFormatException e) {
                            // Skip unsupported aspect-ratio
                        }
                    }
                } else {
                    // Format: single number like "1.5"
                    try {
                        float ratio = Float.parseFloat(ar);
                        s.aspectRatio = String.valueOf(ratio);
                    } catch (NumberFormatException e) {
                        // Skip unsupported
                    }
                }
            }

            RectParts m = RectParts.fromBoxShorthand(css, "margin", true);
            if (m != null) s.margin = m.toJavaLPA();

            RectParts p = RectParts.fromBoxShorthand(css, "padding", false);
            if (p != null) s.padding = p.toJavaLP();

            RectParts b = RectParts.fromBorderWidth(css);
            if (b != null) s.border = b.toJavaLP();

            RectParts inset = RectParts.fromInset(css);
            if (inset != null) s.inset = inset.toJavaInset();

            String fd = css.get("flex-direction");
            if (fd != null) {
                fd = fd.toLowerCase(Locale.ROOT);
                s.flexDirection = switch (fd) {
                    case "row" -> "ROW";
                    case "row-reverse" -> "ROW_REVERSE";
                    case "column" -> "COLUMN";
                    case "column-reverse" -> "COLUMN_REVERSE";
                    default -> null;
                };
            }

            String fw = css.get("flex-wrap");
            if (fw != null) {
                fw = fw.toLowerCase(Locale.ROOT);
                s.flexWrap = switch (fw) {
                    case "nowrap" -> "NO_WRAP";
                    case "wrap" -> "WRAP";
                    case "wrap-reverse" -> "WRAP_REVERSE";
                    default -> null;
                };
            }

            s.flexGrow = floatVal(css.get("flex-grow"));
            s.flexShrink = floatVal(css.get("flex-shrink"));
            s.flexBasis = dimExpr(css.get("flex-basis"));

            s.alignItems = alignItems(css.get("align-items"));
            s.alignContent = alignContent(css.get("align-content"));
            s.justifyContent = alignContent(css.get("justify-content"));
            s.alignSelf = alignSelf(css.get("align-self"));
            s.justifySelf = alignSelf(css.get("justify-self"));

            String gap = css.get("gap");
            String rowGap = css.get("row-gap");
            String colGap = css.get("column-gap");
            GapParts gp = GapParts.from(gap, rowGap, colGap);
            if (gp != null) s.gap = gp.toJava();

            // Limit for generated grid template code length (to avoid 65KB bytecode limit)
            final int MAX_GRID_TEMPLATE_CODE_LENGTH = 10000;

            String gtc = css.get("grid-template-columns");
            if (gtc != null && !gtc.isBlank() && !"none".equalsIgnoreCase(gtc)) {
                String code;
                if (gtc.toLowerCase(Locale.ROOT).contains("repeat(")) {
                    code = GridTemplateList.parse(gtc).toJavaList();
                } else {
                    code = TrackList.parse(gtc).toJavaList();
                }
                if (code.length() > MAX_GRID_TEMPLATE_CODE_LENGTH) {
                    throw new UnsupportedStyleException("grid-template-columns code too long: " + code.length() + " chars");
                }
                if (gtc.toLowerCase(Locale.ROOT).contains("repeat(")) {
                    s.gridTemplateColumnsWithRepeat = code;
                } else {
                    s.gridTemplateColumns = code;
                }
            }
            String gtr = css.get("grid-template-rows");
            if (gtr != null && !gtr.isBlank() && !"none".equalsIgnoreCase(gtr)) {
                String code;
                if (gtr.toLowerCase(Locale.ROOT).contains("repeat(")) {
                    code = GridTemplateList.parse(gtr).toJavaList();
                } else {
                    code = TrackList.parse(gtr).toJavaList();
                }
                if (code.length() > MAX_GRID_TEMPLATE_CODE_LENGTH) {
                    throw new UnsupportedStyleException("grid-template-rows code too long: " + code.length() + " chars");
                }
                if (gtr.toLowerCase(Locale.ROOT).contains("repeat(")) {
                    s.gridTemplateRowsWithRepeat = code;
                } else {
                    s.gridTemplateRows = code;
                }
            }

            String gac = css.get("grid-auto-columns");
            if (gac != null && !gac.isBlank() && !"auto".equalsIgnoreCase(gac)) {
                s.gridAutoColumns = TrackList.parse(gac).toJavaList();
            }
            String gar = css.get("grid-auto-rows");
            if (gar != null && !gar.isBlank() && !"auto".equalsIgnoreCase(gar)) {
                s.gridAutoRows = TrackList.parse(gar).toJavaList();
            }

            String gaf = css.get("grid-auto-flow");
            if (gaf != null) {
                s.gridAutoFlow = gridAutoFlow(gaf);
            }

            String gridColumn = css.get("grid-column");
            if (gridColumn == null) {
                String start = css.get("grid-column-start");
                String end = css.get("grid-column-end");
                gridColumn = combineLine(start, end);
            }
            if (gridColumn != null) {
                s.gridColumn = lineToJava(gridColumn);
            }

            String gridRow = css.get("grid-row");
            if (gridRow == null) {
                String start = css.get("grid-row-start");
                String end = css.get("grid-row-end");
                gridRow = combineLine(start, end);
            }
            if (gridRow != null) {
                s.gridRow = lineToJava(gridRow);
            }

            // Check for unsupported CSS order property (Taffy doesn't support it)
            String order = css.get("order");
            if (order != null && !order.isBlank()) {
                try {
                    int orderVal = Integer.parseInt(order.trim());
                    if (orderVal != 0) {
                        throw new UnsupportedStyleException("CSS order property not supported: " + order);
                    }
                } catch (NumberFormatException e) {
                    // non-numeric order value, skip
                }
            }

            return s;
        }

        private static String combineLine(String start, String end) {
            if (start == null && end == null) return null;
            if (start == null) start = "auto";
            if (end == null) end = "auto";
            return start + " / " + end;
        }

        private static String alignItems(String v) {
            if (v == null) return null;
            v = v.trim().toLowerCase(Locale.ROOT);
            return switch (v) {
                case "stretch" -> "STRETCH";
                case "center" -> "CENTER";
                case "flex-start" -> "FLEX_START";
                case "flex-end" -> "FLEX_END";
                case "start" -> "START";
                case "end" -> "END";
                case "baseline" -> "BASELINE";
                default -> null;
            };
        }

        private static String alignContent(String v) {
            if (v == null) return null;
            v = v.trim().toLowerCase(Locale.ROOT);
            return switch (v) {
                case "stretch" -> "STRETCH";
                case "center" -> "CENTER";
                case "flex-start" -> "FLEX_START";
                case "flex-end" -> "FLEX_END";
                case "start" -> "START";
                case "end" -> "END";
                case "space-between" -> "SPACE_BETWEEN";
                case "space-around" -> "SPACE_AROUND";
                case "space-evenly" -> "SPACE_EVENLY";
                default -> null;
            };
        }

        private static String alignSelf(String v) {
            if (v == null) return null;
            v = v.trim().toLowerCase(Locale.ROOT);
            return switch (v) {
                case "auto" -> null; // auto means inherit from parent's alignItems
                case "stretch" -> "STRETCH";
                case "center" -> "CENTER";
                case "flex-start", "start" -> "FLEX_START";
                case "flex-end", "end" -> "FLEX_END";
                case "baseline" -> "BASELINE";
                default -> null;
            };
        }

        private static String gridAutoFlow(String v) {
            String s = v.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
            return switch (s) {
                case "row" -> "ROW";
                case "column" -> "COLUMN";
                case "row dense" -> "ROW_DENSE";
                case "column dense" -> "COLUMN_DENSE";
                default -> null;
            };
        }

        private static Float floatVal(String v) {
            if (v == null) return null;
            v = v.trim();
            if (v.isEmpty()) return null;
            try {
                return Float.parseFloat(v);
            } catch (NumberFormatException _ignored) {
                return null;
            }
        }

        private static String dimExpr(String v) {
            if (v == null) return null;
            v = v.trim().toLowerCase(Locale.ROOT);
            if (v.isEmpty()) return null;
            if ("auto".equals(v)) return "TaffyDimension.AUTO";
            if ("content".equals(v)) return "TaffyDimension.AUTO";
            // Intrinsic sizing keywords
            if ("min-content".equals(v)) return "TaffyDimension.minContent()";
            if ("max-content".equals(v)) return "TaffyDimension.maxContent()";
            if ("stretch".equals(v)) return "TaffyDimension.stretch()";
            if (v.startsWith("fit-content")) {
                // fit-content can have an optional argument: fit-content(200px)
                if (v.equals("fit-content")) {
                    return "TaffyDimension.fitContent()";
                }
                // fit-content(<length-percentage>)
                if (v.startsWith("fit-content(") && v.endsWith(")")) {
                    String arg = v.substring(12, v.length() - 1).trim();
                    LengthValue lv = LengthValue.parse(arg);
                    if (lv != null) {
                        return switch (lv.type) {
                            case PX -> "TaffyDimension.fitContent(" + lv.value + "f)";
                            case PERCENT -> "TaffyDimension.fitContentPercent(" + (lv.value / 100f) + "f)";
                        };
                    }
                }
                // Fallback to basic fit-content
                return "TaffyDimension.fitContent()";
            }
            LengthValue lv = LengthValue.parse(v);
            if (lv == null) return null;
            return switch (lv.type) {
                case PX -> "TaffyDimension.length(" + lv.value + "f)";
                case PERCENT -> "TaffyDimension.percent(" + (lv.value / 100f) + "f)";
            };
        }

        private static String lineToJava(String v) {
            v = v.trim();
            if (v.isEmpty()) return null;
            String[] parts = v.split("/");
            String start = parts.length > 0 ? parts[0].trim() : "auto";
            String end = parts.length > 1 ? parts[1].trim() : "auto";
            GridPlacementParts s = GridPlacementParts.parse(start);
            GridPlacementParts e = GridPlacementParts.parse(end);
            if (s == null || e == null) {
                throw new UnsupportedStyleException("Unsupported grid line syntax: " + v);
            }
            return "new TaffyLine<>(" + s.toJava() + ", " + e.toJava() + ")";
        }
    }

    private static final class CssStyleMap {
        final Map<String, String> props;

        private CssStyleMap(Map<String, String> props) {
            this.props = props;
        }

        static CssStyleMap from(String inlineStyle, Map<String, String> computed) {
            Map<String, String> map = new HashMap<>();

            // computed first
            if (computed != null) {
                for (Map.Entry<String, String> e : computed.entrySet()) {
                    map.put(normalizeKey(e.getKey()), normalizeVal(e.getValue()));
                }
            }

            // inline overrides
            if (inlineStyle != null && !inlineStyle.isBlank()) {
                String[] decls = inlineStyle.split(";");
                for (String d : decls) {
                    int idx = d.indexOf(':');
                    if (idx <= 0) continue;
                    String k = normalizeKey(d.substring(0, idx));
                    String v = normalizeVal(d.substring(idx + 1));
                    if (!k.isEmpty() && !v.isEmpty()) {
                        map.put(k, v);
                    }
                }
            }

            return new CssStyleMap(map);
        }

        String get(String key) {
            return props.get(normalizeKey(key));
        }

        private static String normalizeKey(String k) {
            return k == null ? "" : k.trim().toLowerCase(Locale.ROOT);
        }

        private static String normalizeVal(String v) {
            if (v == null) return "";
            return v.trim();
        }
    }

    private enum LengthType { PX, PERCENT }

    private record LengthValue(LengthType type, float value) {
        // Base font size for em/rem conversion (browser default is 16px)
        private static final float BASE_FONT_SIZE = 16f;
        // Average character width for ch unit (roughly 0.5em for most fonts)
        private static final float CH_WIDTH = 8f;

        static LengthValue parse(String raw) {
            if (raw == null) return null;
            String v = raw.trim().toLowerCase(Locale.ROOT);
            if (v.isEmpty()) return null;
            if ("0".equals(v)) return new LengthValue(LengthType.PX, 0f);
            
            // Handle calc() expressions - try to evaluate simple ones
            if (v.startsWith("calc(") && v.endsWith(")")) {
                return parseCalc(v.substring(5, v.length() - 1));
            }
            
            if (v.endsWith("px")) {
                return new LengthValue(LengthType.PX, parseFloatSafe(v.substring(0, v.length() - 2)));
            }
            if (v.endsWith("%")) {
                return new LengthValue(LengthType.PERCENT, parseFloatSafe(v.substring(0, v.length() - 1)));
            }
            // Support em unit (relative to font-size, use default 16px)
            if (v.endsWith("em") && !v.endsWith("rem")) {
                return new LengthValue(LengthType.PX, parseFloatSafe(v.substring(0, v.length() - 2)) * BASE_FONT_SIZE);
            }
            // Support rem unit (relative to root font-size)
            if (v.endsWith("rem")) {
                return new LengthValue(LengthType.PX, parseFloatSafe(v.substring(0, v.length() - 3)) * BASE_FONT_SIZE);
            }
            // Support ch unit (width of '0' character, roughly 0.5em)
            if (v.endsWith("ch")) {
                return new LengthValue(LengthType.PX, parseFloatSafe(v.substring(0, v.length() - 2)) * CH_WIDTH);
            }
            // Support vw/vh units (viewport width/height percentage)
            // Use our fixed viewport size: 800x600
            if (v.endsWith("vw")) {
                return new LengthValue(LengthType.PX, parseFloatSafe(v.substring(0, v.length() - 2)) * VIEWPORT_WIDTH / 100f);
            }
            if (v.endsWith("vh")) {
                return new LengthValue(LengthType.PX, parseFloatSafe(v.substring(0, v.length() - 2)) * VIEWPORT_HEIGHT / 100f);
            }
            // Support vmin/vmax
            if (v.endsWith("vmin")) {
                return new LengthValue(LengthType.PX, parseFloatSafe(v.substring(0, v.length() - 4)) * Math.min(VIEWPORT_WIDTH, VIEWPORT_HEIGHT) / 100f);
            }
            if (v.endsWith("vmax")) {
                return new LengthValue(LengthType.PX, parseFloatSafe(v.substring(0, v.length() - 4)) * Math.max(VIEWPORT_WIDTH, VIEWPORT_HEIGHT) / 100f);
            }
            // Support pt (points, 1pt = 1.333px)
            if (v.endsWith("pt")) {
                return new LengthValue(LengthType.PX, parseFloatSafe(v.substring(0, v.length() - 2)) * 1.333f);
            }
            return null;
        }
        
        // Parse simple calc() expressions like "calc(5% + 20px)" or "calc(10% - 8px)"
        private static LengthValue parseCalc(String expr) {
            expr = expr.trim();
            // Try to find + or - operator (not at the start, and not inside a number like -5px)
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
                // No operator found, try parsing as a single value
                return parse(expr);
            }
            
            String left = expr.substring(0, opIdx).trim();
            String right = expr.substring(opIdx + 1).trim();
            
            LengthValue lv = parse(left);
            LengthValue rv = parse(right);
            
            if (lv == null || rv == null) return null;
            
            // If both are pixels, we can compute the result
            if (lv.type == LengthType.PX && rv.type == LengthType.PX) {
                float result = op == '+' ? lv.value + rv.value : lv.value - rv.value;
                return new LengthValue(LengthType.PX, result);
            }
            // If both are percentages, we can compute the result
            if (lv.type == LengthType.PERCENT && rv.type == LengthType.PERCENT) {
                float result = op == '+' ? lv.value + rv.value : lv.value - rv.value;
                return new LengthValue(LengthType.PERCENT, result);
            }
            // Mixed units (% + px) - cannot be resolved at parse time
            // For test generation, we'll approximate by assuming a reasonable context
            // This is imperfect but allows more tests to be generated
            return null;
        }

        private static float parseFloatSafe(String s) {
            try {
                return Float.parseFloat(s.trim());
            } catch (NumberFormatException ex) {
                throw new UnsupportedStyleException("Unsupported numeric value: " + s);
            }
        }
    }

    private static final class RectParts {
        final String left;
        final String right;
        final String top;
        final String bottom;

        private RectParts(String left, String right, String top, String bottom) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }

        static RectParts fromBoxShorthand(CssStyleMap css, String base, boolean allowAuto) {
            String shorthand = css.get(base);
            String left = css.get(base + "-left");
            String right = css.get(base + "-right");
            String top = css.get(base + "-top");
            String bottom = css.get(base + "-bottom");

            if (shorthand != null) {
                String[] vals = splitWs(shorthand);
                String t, r, b, l;
                if (vals.length == 1) {
                    t = r = b = l = vals[0];
                } else if (vals.length == 2) {
                    t = b = vals[0];
                    r = l = vals[1];
                } else if (vals.length == 3) {
                    t = vals[0];
                    r = l = vals[1];
                    b = vals[2];
                } else if (vals.length >= 4) {
                    t = vals[0];
                    r = vals[1];
                    b = vals[2];
                    l = vals[3];
                } else {
                    return null;
                }
                top = top != null ? top : t;
                right = right != null ? right : r;
                bottom = bottom != null ? bottom : b;
                left = left != null ? left : l;
            }

            if (left == null && right == null && top == null && bottom == null) return null;
            left = left != null ? left : "0";
            right = right != null ? right : "0";
            top = top != null ? top : "0";
            bottom = bottom != null ? bottom : "0";

            if (!allowAuto) {
                if ("auto".equalsIgnoreCase(left) || "auto".equalsIgnoreCase(right) || "auto".equalsIgnoreCase(top) || "auto".equalsIgnoreCase(bottom)) {
                    throw new UnsupportedStyleException("auto not allowed for " + base);
                }
            }

            return new RectParts(left, right, top, bottom);
        }

        static RectParts fromBorderWidth(CssStyleMap css) {
            // Prefer explicit border-*-width; fallback to border shorthand.
            String left = css.get("border-left-width");
            String right = css.get("border-right-width");
            String top = css.get("border-top-width");
            String bottom = css.get("border-bottom-width");

            String border = css.get("border");
            if (border != null && (left == null || right == null || top == null || bottom == null)) {
                // Take the first token if it looks like a length.
                String[] parts = splitWs(border);
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

            if (left == null && right == null && top == null && bottom == null) return null;
            return new RectParts(
                left != null ? left : "0",
                right != null ? right : "0",
                top != null ? top : "0",
                bottom != null ? bottom : "0"
            );
        }

        static RectParts fromInset(CssStyleMap css) {
            String left = css.get("left");
            String right = css.get("right");
            String top = css.get("top");
            String bottom = css.get("bottom");
            if (left == null && right == null && top == null && bottom == null) return null;

            left = left != null ? left : "auto";
            right = right != null ? right : "auto";
            top = top != null ? top : "auto";
            bottom = bottom != null ? bottom : "auto";

            return new RectParts(left, right, top, bottom);
        }

        String toJavaLPA() {
            return "new TaffyRect<>(" + lpaExpr(left) + ", " + lpaExpr(right) + ", " + lpaExpr(top) + ", " + lpaExpr(bottom) + ")";
        }

        String toJavaLP() {
            return "new TaffyRect<>(" + lpExpr(left) + ", " + lpExpr(right) + ", " + lpExpr(top) + ", " + lpExpr(bottom) + ")";
        }

        String toJavaInset() {
            return toJavaLPA();
        }

        private static String lpaExpr(String v) {
            v = v.trim().toLowerCase(Locale.ROOT);
            if ("auto".equals(v)) return "LengthPercentageAuto.AUTO";
            LengthValue lv = LengthValue.parse(v);
            if (lv == null) throw new UnsupportedStyleException("Unsupported length: " + v);
            return switch (lv.type) {
                case PX -> "LengthPercentageAuto.length(" + lv.value + "f)";
                case PERCENT -> "LengthPercentageAuto.percent(" + (lv.value / 100f) + "f)";
            };
        }

        private static String lpExpr(String v) {
            v = v.trim().toLowerCase(Locale.ROOT);
            LengthValue lv = LengthValue.parse(v);
            if (lv == null) throw new UnsupportedStyleException("Unsupported length: " + v);
            return switch (lv.type) {
                case PX -> "LengthPercentage.length(" + lv.value + "f)";
                case PERCENT -> "LengthPercentage.percent(" + (lv.value / 100f) + "f)";
            };
        }

        private static String[] splitWs(String s) {
            return s.trim().replaceAll("\\s+", " ").split(" ");
        }
    }

    private static final class GapParts {
        final String row;
        final String col;

        private GapParts(String row, String col) {
            this.row = row;
            this.col = col;
        }

        static GapParts from(String gap, String rowGap, String colGap) {
            String row = rowGap;
            String col = colGap;

            if (gap != null && (row == null || col == null)) {
                String[] vals = gap.trim().replaceAll("\\s+", " ").split(" ");
                if (vals.length == 1) {
                    row = row != null ? row : vals[0];
                    col = col != null ? col : vals[0];
                } else if (vals.length >= 2) {
                    // CSS order: row-gap then column-gap
                    row = row != null ? row : vals[0];
                    col = col != null ? col : vals[1];
                }
            }

            if (row == null && col == null) return null;
            row = row != null ? row : "0";
            col = col != null ? col : "0";
            return new GapParts(row, col);
        }

        String toJava() {
            // Style.gap is Size<LengthPercentage> with width=column gap, height=row gap.
            return "new TaffySize<>(" + lp(col) + ", " + lp(row) + ")";
        }

        private static String lp(String v) {
            v = v.trim().toLowerCase(Locale.ROOT);
            // In CSS Box Alignment, `normal` computes to 0 for gap on grid/flex containers.
            // WPT uses this keyword frequently (sometimes implicitly via initial value).
            if ("normal".equals(v)) {
                return "LengthPercentage.length(0f)";
            }
            LengthValue lv = LengthValue.parse(v);
            if (lv == null) throw new UnsupportedStyleException("Unsupported gap length: " + v);
            return switch (lv.type) {
                case PX -> "LengthPercentage.length(" + lv.value + "f)";
                case PERCENT -> "LengthPercentage.percent(" + (lv.value / 100f) + "f)";
            };
        }
    }

    private static final class TrackList {
        final List<String> items;

        private TrackList(List<String> items) {
            this.items = items;
        }

        static TrackList parse(String raw) {
            String s = raw.trim();
            if (s.isEmpty()) return new TrackList(List.of());

            // Remove named grid lines: [foo bar]
            s = s.replaceAll("\\[[^\\]]*\\]", " ");
            s = s.replaceAll("\\s+", " ").trim();

            List<String> tokens = splitTopLevel(s);
            List<String> out = new ArrayList<>();
            for (String t : tokens) {
                if (t.isBlank()) continue;
                out.add(trackExpr(t.trim()));
            }
            return new TrackList(out);
        }

        String toJavaList() {
            if (items.isEmpty()) return "new java.util.ArrayList<>()";
            return "java.util.List.of(" + String.join(", ", items) + ")";
        }

        static String trackExpr(String t) {
            String v = t.toLowerCase(Locale.ROOT);
            if ("auto".equals(v)) return "TrackSizingFunction.auto()";
            if ("min-content".equals(v)) return "TrackSizingFunction.minContent()";
            if ("max-content".equals(v)) return "TrackSizingFunction.maxContent()";

            if (v.endsWith("fr")) {
                float fr = Float.parseFloat(v.substring(0, v.length() - 2).trim());
                return "TrackSizingFunction.fr(" + fr + "f)";
            }

            if (v.startsWith("fit-content(")) {
                String inner = insideParens(t);
                String lp = RectParts.lpExpr(inner.trim().toLowerCase(Locale.ROOT));
                return "TrackSizingFunction.fitContent(" + lp + ")";
            }

            if (v.startsWith("minmax(")) {
                String inner = insideParens(t);
                List<String> parts = splitTopLevelByComma(inner);
                if (parts.size() != 2) throw new UnsupportedStyleException("Unsupported minmax(): " + t);
                String a = trackExpr(parts.get(0).trim());
                String b = trackExpr(parts.get(1).trim());
                return "TrackSizingFunction.minmax(" + a + ", " + b + ")";
            }

            LengthValue lv = LengthValue.parse(v);
            if (lv != null) {
                return switch (lv.type) {
                    case PX -> "TrackSizingFunction.fixed(" + lv.value + "f)";
                    case PERCENT -> "TrackSizingFunction.percent(" + (lv.value / 100f) + "f)";
                };
            }

            throw new UnsupportedStyleException("Unsupported track token: " + t);
        }

        static List<String> splitTopLevel(String s) {
            List<String> out = new ArrayList<>();
            StringBuilder cur = new StringBuilder();
            int depth = 0;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '(') depth++;
                if (c == ')') depth = Math.max(0, depth - 1);

                if (depth == 0 && Character.isWhitespace(c)) {
                    if (!cur.isEmpty()) {
                        out.add(cur.toString());
                        cur.setLength(0);
                    }
                } else {
                    cur.append(c);
                }
            }
            if (!cur.isEmpty()) out.add(cur.toString());
            return out;
        }

        static List<String> splitTopLevelByComma(String s) {
            List<String> out = new ArrayList<>();
            StringBuilder cur = new StringBuilder();
            int depth = 0;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '(') depth++;
                if (c == ')') depth = Math.max(0, depth - 1);

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

        static String insideParens(String t) {
            int a = t.indexOf('(');
            int b = t.lastIndexOf(')');
            if (a < 0 || b < 0 || b <= a) throw new UnsupportedStyleException("Missing parens: " + t);
            return t.substring(a + 1, b);
        }
    }

    /**
     * Parses grid-template-rows/columns values that may contain repeat().
     *
     * We only support a subset sufficient for the WPT mini set:
     * - repeat(auto-fit, track-list)
     * - repeat(auto-fill, track-list)
     * - repeat(<int>, track-list)
     *
     * Named grid lines ([...]) are ignored.
     */
    private static final class GridTemplateList {
        final List<String> items;

        private GridTemplateList(List<String> items) {
            this.items = items;
        }

        static GridTemplateList parse(String raw) {
            String s = raw.trim();
            if (s.isEmpty()) return new GridTemplateList(List.of());

            // Remove named grid lines: [foo bar]
            s = s.replaceAll("\\[[^\\]]*\\]", " ");
            s = s.replaceAll("\\s+", " ").trim();

            List<String> tokens = TrackList.splitTopLevel(s);
            List<String> out = new ArrayList<>();
            for (String t : tokens) {
                if (t.isBlank()) continue;
                String token = t.trim();
                String lower = token.toLowerCase(Locale.ROOT);
                if (lower.startsWith("repeat(")) {
                    out.add(repeatExpr(token));
                } else {
                    String track = TrackList.trackExpr(token);
                    out.add("GridTemplateComponent.single(" + track + ")");
                }
            }
            return new GridTemplateList(out);
        }

        String toJavaList() {
            if (items.isEmpty()) return "new java.util.ArrayList<>()";
            return "java.util.List.of(" + String.join(", ", items) + ")";
        }

        private static String repeatExpr(String repeatToken) {
            String inner = TrackList.insideParens(repeatToken);
            List<String> parts = TrackList.splitTopLevelByComma(inner);
            if (parts.size() != 2) {
                throw new UnsupportedStyleException("Unsupported repeat(): " + repeatToken);
            }

            String countSpec = parts.get(0).trim().toLowerCase(Locale.ROOT);
            String trackListRaw = parts.get(1).trim();

            if (trackListRaw.toLowerCase(Locale.ROOT).contains("repeat(")) {
                throw new UnsupportedStyleException("Nested repeat() not supported: " + repeatToken);
            }

            TrackList tracks = TrackList.parse(trackListRaw);
            if (tracks.items.isEmpty()) {
                throw new UnsupportedStyleException("repeat() without tracks: " + repeatToken);
            }
            String args = String.join(", ", tracks.items);

            return switch (countSpec) {
                case "auto-fit" -> "GridTemplateComponent.autoFit(" + args + ")";
                case "auto-fill" -> "GridTemplateComponent.autoFill(" + args + ")";
                default -> {
                    int count;
                    try {
                        count = Integer.parseInt(countSpec);
                    } catch (NumberFormatException e) {
                        throw new UnsupportedStyleException("Unsupported repeat count: " + repeatToken);
                    }
                    yield "GridTemplateComponent.repeatCount(" + count + ", " + args + ")";
                }
            };
        }
    }

    private static final class GridPlacementParts {
        final String kind;
        final int value;

        private GridPlacementParts(String kind, int value) {
            this.kind = kind;
            this.value = value;
        }

        static GridPlacementParts parse(String raw) {
            String v = raw.trim().toLowerCase(Locale.ROOT);
            if (v.isEmpty() || "auto".equals(v)) return new GridPlacementParts("auto", 0);
            if (v.startsWith("span")) {
                String[] parts = v.replaceAll("\\s+", " ").split(" ");
                if (parts.length != 2) throw new UnsupportedStyleException("Unsupported span: " + raw);
                try {
                    return new GridPlacementParts("span", Integer.parseInt(parts[1]));
                } catch (NumberFormatException e) {
                    throw new UnsupportedStyleException("Unsupported span with named line: " + raw);
                }
            }

            // Named lines are not supported (taffy doesn't support them).
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

    private static final class UnsupportedStyleException extends RuntimeException {
        UnsupportedStyleException(String message) { super(message); }
    }

    // Collect DOM + geometry + computed-style fallback.
    //
    // IMPORTANT: WPT pages often include a large harness/log DOM under <body>.
    // Capturing the entire subtree explodes generated Java source size and can
    // make javac run out of heap. We keep <body> as the root for a stable
    // containing block, but only traverse a likely test-content subtree.
    private static final String JS_COLLECT = "return (function(){\n" +
        "  const SKIP = new Set(['SCRIPT','STYLE','LINK','META']);\n" +
        "  const SKIP_IDS = new Set(['log','metadata','description','testharness','testharness-log']);\n" +
        "  const css = document.createElement('style');\n" +
        "  css.textContent = 'html,body{margin:0;padding:0;border:0;}';\n" +
        "  document.head.appendChild(css);\n" +
        "\n" +
        "  const body = document.body;\n" +
        "  const bodyRect = body.getBoundingClientRect();\n" +
        "\n" +
        "  const props = [\n" +
        "    'display','position','box-sizing','direction',\n" +
        "    'width','height','min-width','min-height','max-width','max-height',\n" +
        "    'aspect-ratio',\n" +
        "    'margin','margin-left','margin-right','margin-top','margin-bottom',\n" +
        "    'padding','padding-left','padding-right','padding-top','padding-bottom',\n" +
        "    'border','border-left-width','border-right-width','border-top-width','border-bottom-width',\n" +
        "    'left','right','top','bottom',\n" +
        "    'flex-direction','flex-wrap','flex-grow','flex-shrink','flex-basis',\n" +
        "    'align-items','align-content','justify-content','align-self','justify-self',\n" +
        "    'gap','row-gap','column-gap',\n" +
        "    'grid-template-columns','grid-template-rows',\n" +
        "    'grid-auto-columns','grid-auto-rows','grid-auto-flow',\n" +
        "    'grid-column','grid-column-start','grid-column-end',\n" +
        "    'grid-row','grid-row-start','grid-row-end',\n" +
        "    'order'\n" +
        "  ];\n" +
        "\n" +
        "  // Get original (non-computed) CSS values from stylesheets and inline styles\n" +
        "  function getOriginalStyles(el) {\n" +
        "    const orig = {};\n" +
        "    // First, collect from matching stylesheet rules\n" +
        "    try {\n" +
        "      for (const sheet of document.styleSheets) {\n" +
        "        try {\n" +
        "          const rules = sheet.cssRules || sheet.rules;\n" +
        "          if (!rules) continue;\n" +
        "          for (const rule of rules) {\n" +
        "            if (rule.type !== 1) continue; // CSSStyleRule\n" +
        "            try {\n" +
        "              if (el.matches(rule.selectorText)) {\n" +
        "                const style = rule.style;\n" +
        "                for (const p of props) {\n" +
        "                  const v = style.getPropertyValue(p);\n" +
        "                  if (v) orig[p] = v;\n" +
        "                }\n" +
        "              }\n" +
        "            } catch (e) {}\n" +
        "          }\n" +
        "        } catch (e) {}\n" +
        "      }\n" +
        "    } catch (e) {}\n" +
        "    // Then, overlay inline styles (higher priority)\n" +
        "    if (el.style) {\n" +
        "      for (const p of props) {\n" +
        "        const v = el.style.getPropertyValue(p);\n" +
        "        if (v) orig[p] = v;\n" +
        "      }\n" +
        "    }\n" +
        "    return orig;\n" +
        "  }\n" +
        "\n" +
        "  const nodes = [];\n" +
        "\n" +
        "  function shouldSkip(el) {\n" +
        "    if (!el) return true;\n" +
        "    if (SKIP.has(el.tagName)) return true;\n" +
        "    const id = (el.id || '').toLowerCase();\n" +
        "    if (id && SKIP_IDS.has(id)) return true;\n" +
        "    const cls = (el.className || '').toString().toLowerCase();\n" +
        "    if (cls.includes('testharness') || cls.includes('wptreport')) return true;\n" +
        "    return false;\n" +
        "  }\n" +
        "\n" +
        "  function addNode(el, parentIndex) {\n" +
        "    if (shouldSkip(el)) return -1;\n" +
        "    const idx = nodes.length;\n" +
        "    const r = el.getBoundingClientRect();\n" +
        "    const cs = getComputedStyle(el);\n" +
        "    const computed = {};\n" +
        "    for (const p of props) { computed[p] = cs.getPropertyValue(p); }\n" +
        "    // Get original styles (preserves 'auto' values)\n" +
        "    const original = getOriginalStyles(el);\n" +
        "    // Properties that should default to 'auto' when not explicitly set\n" +
        "    const insetProps = new Set(['left','right','top','bottom']);\n" +
        "    // Merge: use original value if it contains 'auto', otherwise use computed\n" +
        "    // For inset properties, if not explicitly set in CSS, use 'auto'\n" +
        "    for (const p of props) {\n" +
        "      if (original[p] && original[p].includes('auto')) {\n" +
        "        computed[p] = original[p];\n" +
        "      } else if (insetProps.has(p) && !original[p]) {\n" +
        "        // Inset property not set in CSS - should be 'auto'\n" +
        "        computed[p] = 'auto';\n" +
        "      }\n" +
        "    }\n" +
        "    nodes.push({\n" +
        "      index: idx,\n" +
        "      parent: parentIndex,\n" +
        "      tag: el.tagName.toLowerCase(),\n" +
        "      id: el.id || '',\n" +
        "      className: el.className || '',\n" +
        "      style: el.getAttribute('style') || '',\n" +
        "      computed,\n" +
        "      rect: { x: (r.left - bodyRect.left), y: (r.top - bodyRect.top), w: r.width, h: r.height }\n" +
        "    });\n" +
        "    return idx;\n" +
        "  }\n" +
        "\n" +
        "  function walk(el, parentIndex) {\n" +
        "    const idx = addNode(el, parentIndex);\n" +
        "    if (idx < 0) return;\n" +
        "    for (const child of el.children) { walk(child, idx); }\n" +
        "  }\n" +
        "\n" +
        "  const bodyIdx = addNode(body, -1);\n" +
        "  let root = document.getElementById('container') || document.getElementById('test');\n" +
        "  if (!root) {\n" +
        "    // Priority 1: Find element with flex/grid display (actual test container)\n" +
        "    let best = null;\n" +
        "    let bestArea = 0;\n" +
        "    for (const child of body.children) {\n" +
        "      if (shouldSkip(child)) continue;\n" +
        "      const cs = getComputedStyle(child);\n" +
        "      const disp = cs.display;\n" +
        "      // Prioritize flex/grid containers as they are the actual test subjects\n" +
        "      if (disp === 'flex' || disp === 'inline-flex' || disp === 'grid' || disp === 'inline-grid') {\n" +
        "        const r = child.getBoundingClientRect();\n" +
        "        const area = r.width * r.height;\n" +
        "        if (area > bestArea) { bestArea = area; best = child; }\n" +
        "      }\n" +
        "    }\n" +
        "    // Priority 2: If no flex/grid found, use largest non-paragraph element\n" +
        "    if (!best) {\n" +
        "      bestArea = 0;\n" +
        "      for (const child of body.children) {\n" +
        "        if (shouldSkip(child)) continue;\n" +
        "        // Skip paragraphs (usually test descriptions like 'Test passes if...')\n" +
        "        if (child.tagName === 'P') continue;\n" +
        "        const r = child.getBoundingClientRect();\n" +
        "        const area = r.width * r.height;\n" +
        "        if (area > bestArea) { bestArea = area; best = child; }\n" +
        "      }\n" +
        "    }\n" +
        "    // Priority 3: Fallback to any largest element\n" +
        "    if (!best) {\n" +
        "      bestArea = 0;\n" +
        "      for (const child of body.children) {\n" +
        "        if (shouldSkip(child)) continue;\n" +
        "        const r = child.getBoundingClientRect();\n" +
        "        const area = r.width * r.height;\n" +
        "        if (area > bestArea) { bestArea = area; best = child; }\n" +
        "      }\n" +
        "    }\n" +
        "    root = best;\n" +
        "  }\n" +
        "  if (root) {\n" +
        "    walk(root, bodyIdx);\n" +
        "  }\n" +
        "\n" +
        "  const viewport = {\n" +
        "    width: document.documentElement.clientWidth,\n" +
        "    height: document.documentElement.clientHeight,\n" +
        "    dpr: window.devicePixelRatio\n" +
        "  };\n" +
        "  return JSON.stringify({ viewport, nodes });\n" +
        "})();";
}
