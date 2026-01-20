package dev.vfyjxf.taffy.gentest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Java test generator that mirrors the Rust taffy gentest tool.
 * Reads HTML fixture files, uses Chrome to compute layouts, and generates Java tests.
 */
public class TestGenerator {
    
    private static final Gson GSON = new Gson();
    private static final String TEST_PACKAGE = "dev.vfyjxf.taffy.generated";
    
    public static void main(String[] args) throws Exception {
        Path rootDir = Paths.get(System.getProperty("user.dir"));
        Path fixturesRoot = rootDir.resolve("taffy/test_fixtures");
        Path outputRoot = rootDir.resolve("taffy-java/src/test/java/dev/vfyjxf/taffy/generated");
        // Back-compat: if old generated tests exist under the previous base package, remove them
        // to avoid stale tests compiling against old package names.
        Path legacyOutputRoot = rootDir.resolve("taffy-java/src/test/java/com/nicecoder/taffy/generated");
        
        System.out.println("Root directory: " + rootDir);
        System.out.println("Fixtures root: " + fixturesRoot);
        System.out.println("Output root: " + outputRoot);
        
        if (!Files.exists(fixturesRoot)) {
            System.err.println("Fixtures directory not found: " + fixturesRoot);
            System.exit(1);
        }
        
        // Collect all HTML fixtures
        List<FixtureInfo> fixtures;
        try (Stream<Path> paths = Files.walk(fixturesRoot)) {
            fixtures = paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".html"))
                .filter(p -> !p.getFileName().toString().startsWith("x")) // Skip disabled tests
                .map(p -> new FixtureInfo(
                    p.getFileName().toString().replace(".html", ""),
                    p,
                    fixturesRoot.relativize(p.getParent()).toString().replace("\\", "/")
                ))
                .sorted(Comparator.comparing(f -> f.name))
                .collect(Collectors.toList());
        }
        
        System.out.println("Found " + fixtures.size() + " fixtures");
        
        // Setup WebDriver
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--no-sandbox");
        ChromeDriver driver = new ChromeDriver(options);
        
        try {
            // Verify scrollbar width is non-zero (required for accurate tests)
            driver.get("data:text/html;charset=utf-8,<html><body><div style='overflow:scroll' /></body></html>");
            Object scrollbarWidth = ((JavascriptExecutor) driver).executeScript(
                "return document.body.firstChild.clientWidth - document.body.firstChild.offsetWidth;"
            );
            System.out.println("Scrollbar width: " + scrollbarWidth);
            
            // Clean legacy output directory (if present)
            if (Files.exists(legacyOutputRoot) && !legacyOutputRoot.equals(outputRoot)) {
                deleteDirectory(legacyOutputRoot);
            }

            // Clean output directory
            if (Files.exists(outputRoot)) {
                deleteDirectory(outputRoot);
            }
            Files.createDirectories(outputRoot);
            
            // Group fixtures by category
            Map<String, List<FixtureInfo>> byCategory = fixtures.stream()
                .collect(Collectors.groupingBy(f -> f.category));
            
            // Process each category
            for (Map.Entry<String, List<FixtureInfo>> entry : byCategory.entrySet()) {
                String category = entry.getKey();
                List<FixtureInfo> categoryFixtures = entry.getValue();
                
                System.out.println("\nProcessing category: " + category + " (" + categoryFixtures.size() + " tests)");
                
                Path categoryDir = outputRoot.resolve(category);
                Files.createDirectories(categoryDir);
                
                StringBuilder classBuilder = new StringBuilder();
                classBuilder.append("package ").append(TEST_PACKAGE).append(".").append(category).append(";\n\n");
                classBuilder.append("import dev.vfyjxf.taffy.geometry.*;\n");
                classBuilder.append("import dev.vfyjxf.taffy.style.*;\n");
                classBuilder.append("import dev.vfyjxf.taffy.tree.*;\n");
                classBuilder.append("import dev.vfyjxf.taffy.util.MeasureFunc;\n");
                classBuilder.append("import org.junit.jupiter.api.Test;\n");
                classBuilder.append("import org.junit.jupiter.api.DisplayName;\n");
                classBuilder.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
                classBuilder.append("/**\n * Generated tests for ").append(category).append(" layout fixtures.\n */\n");
                classBuilder.append("public class ").append(toPascalCase(category)).append("Test {\n\n");
                classBuilder.append("    private static final float EPSILON = 0.1f;\n\n");

                // Shared Ahem text measurement helper (mirrors taffy_test_helpers::AhemTextMeasureData)
                classBuilder.append("    private static MeasureFunc ahemTextMeasure(String text, boolean vertical) {\n");
                classBuilder.append("        final String trimmed = text == null ? \"\" : text.trim();\n");
                classBuilder.append("        return (knownDimensions, availableSpace) -> {\n");
                classBuilder.append("            if (!Float.isNaN(knownDimensions.width) && !Float.isNaN(knownDimensions.height)) {\n");
                classBuilder.append("                return new FloatSize(knownDimensions.width, knownDimensions.height);\n");
                classBuilder.append("            }\n\n");
                classBuilder.append("            final char ZWS = '\\u200B';\n");
                classBuilder.append("            final float H_WIDTH = 10.0f;\n");
                classBuilder.append("            final float H_HEIGHT = 10.0f;\n\n");
                classBuilder.append("            String[] parts = trimmed.isEmpty() ? new String[0] : trimmed.split(String.valueOf(ZWS), -1);\n");
                classBuilder.append("            if (parts.length == 0) {\n");
                classBuilder.append("                float w = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : 0.0f;\n");
                classBuilder.append("                float h = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : 0.0f;\n");
                classBuilder.append("                return new FloatSize(w, h);\n");
                classBuilder.append("            }\n\n");
                classBuilder.append("            int minLineLength = 0;\n");
                classBuilder.append("            int maxLineLength = 0;\n");
                classBuilder.append("            for (String p : parts) {\n");
                classBuilder.append("                int len = p.length();\n");
                classBuilder.append("                if (len > minLineLength) minLineLength = len;\n");
                classBuilder.append("                maxLineLength += len;\n");
                classBuilder.append("            }\n\n");
                classBuilder.append("            float knownInline = vertical ? knownDimensions.height : knownDimensions.width;\n");
                classBuilder.append("            float knownBlock = vertical ? knownDimensions.width : knownDimensions.height;\n");
                classBuilder.append("            AvailableSpace availInline = vertical ? availableSpace.height : availableSpace.width;\n\n");
                classBuilder.append("            float inlineSize;\n");
                classBuilder.append("            if (!Float.isNaN(knownInline)) {\n");
                classBuilder.append("                inlineSize = knownInline;\n");
                classBuilder.append("            } else if (availInline != null && availInline.isMinContent()) {\n");
                classBuilder.append("                inlineSize = minLineLength * H_WIDTH;\n");
                classBuilder.append("            } else if (availInline != null && availInline.isMaxContent()) {\n");
                classBuilder.append("                inlineSize = maxLineLength * H_WIDTH;\n");
                classBuilder.append("            } else if (availInline != null && availInline.isDefinite()) {\n");
                classBuilder.append("                inlineSize = Math.min(availInline.getValue(), maxLineLength * H_WIDTH);\n");
                classBuilder.append("            } else {\n");
                classBuilder.append("                inlineSize = maxLineLength * H_WIDTH;\n");
                classBuilder.append("            }\n");
                classBuilder.append("            inlineSize = Math.max(inlineSize, minLineLength * H_WIDTH);\n\n");
                classBuilder.append("            float blockSize;\n");
                classBuilder.append("            if (!Float.isNaN(knownBlock)) {\n");
                classBuilder.append("                blockSize = knownBlock;\n");
                classBuilder.append("            } else {\n");
                classBuilder.append("                int inlineLineLength = (int) Math.floor(inlineSize / H_WIDTH);\n");
                classBuilder.append("                int lineCount = 1;\n");
                classBuilder.append("                int currentLineLength = 0;\n");
                classBuilder.append("                for (String p : parts) {\n");
                classBuilder.append("                    int len = p.length();\n");
                classBuilder.append("                    if (currentLineLength + len > inlineLineLength) {\n");
                classBuilder.append("                        if (currentLineLength > 0) {\n");
                classBuilder.append("                            lineCount += 1;\n");
                classBuilder.append("                        }\n");
                classBuilder.append("                        currentLineLength = len;\n");
                classBuilder.append("                    } else {\n");
                classBuilder.append("                        currentLineLength += len;\n");
                classBuilder.append("                    }\n");
                classBuilder.append("                }\n");
                classBuilder.append("                blockSize = lineCount * H_HEIGHT;\n");
                classBuilder.append("            }\n\n");
                classBuilder.append("            FloatSize computed = vertical\n");
                classBuilder.append("                ? new FloatSize(blockSize, inlineSize)\n");
                classBuilder.append("                : new FloatSize(inlineSize, blockSize);\n\n");
                classBuilder.append("            float outW = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : computed.width;\n");
                classBuilder.append("            float outH = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : computed.height;\n");
                classBuilder.append("            return new FloatSize(outW, outH);\n");
                classBuilder.append("        };\n");
                classBuilder.append("    }\n\n");
                
                int processedCount = 0;
                for (FixtureInfo fixture : categoryFixtures) {
                    try {
                        String testCode = processFixture(driver, fixture);
                        if (testCode != null) {
                            classBuilder.append(testCode);
                            processedCount++;
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing " + fixture.name + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                    
                    if (processedCount % 10 == 0) {
                        System.out.print(".");
                    }
                }
                
                classBuilder.append("}\n");
                
                Path classFile = categoryDir.resolve(toPascalCase(category) + "Test.java");
                Files.writeString(classFile, classBuilder.toString());
                System.out.println("\nWrote " + processedCount + " tests to " + classFile);
            }
            
        } finally {
            driver.quit();
        }
        
        System.out.println("\nDone!");
    }
    
    private static String processFixture(ChromeDriver driver, FixtureInfo fixture) throws Exception {
        String fileUrl = fixture.path.toUri().toString();
        driver.get(fileUrl);
        
        // Execute test helper to get test data
        Object result = ((JavascriptExecutor) driver).executeScript("return getTestData()");
        if (result == null) {
            System.err.println("No test data for: " + fixture.name);
            return null;
        }
        
        JsonObject testData = GSON.fromJson(result.toString(), JsonObject.class);
        JsonObject borderBoxData = testData.getAsJsonObject("borderBoxData");
        JsonObject contentBoxData = testData.getAsJsonObject("contentBoxData");
        
        StringBuilder sb = new StringBuilder();
        
        // Generate test for border-box mode (CSS default)
        sb.append(generateTest(fixture.name + "__border_box", borderBoxData));
        
        // Generate test for content-box mode
        if (contentBoxData != null) {
            sb.append(generateTest(fixture.name + "__content_box", contentBoxData));
        }
        
        return sb.toString();
    }
    
    private static String generateTest(String name, JsonObject description) {
        StringBuilder sb = new StringBuilder();
        
        boolean useRounding = description.has("useRounding") && description.get("useRounding").getAsBoolean();
        
        String methodName = toMethodName(name);
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"").append(name).append("\")\n");
        sb.append("    void ").append(methodName).append("() {\n");
        sb.append("        TaffyTree tree = new TaffyTree();\n");
        if (!useRounding) {
            sb.append("        tree.disableRounding();\n");
        }
        sb.append("\n");
        
        // Generate node creation code
        NodeContext ctx = new NodeContext();
        generateNodeCreation(sb, "node", description, ctx, 2);
        
        // Generate layout computation
        sb.append("\n");
        JsonObject viewport = description.getAsJsonObject("viewport");
        String availableSpace = generateAvailableSpace(viewport);
        sb.append("        tree.computeLayout(node, ").append(availableSpace).append(");\n\n");
        
        // Generate assertions
        generateAssertions(sb, "node", description, useRounding, 2);
        
        sb.append("    }\n\n");
        return sb.toString();
    }
    
    private static void generateNodeCreation(StringBuilder sb, String ident, JsonObject node, NodeContext ctx, int indent) {
        String ind = "        ".substring(0, indent * 4);
        JsonObject style = node.getAsJsonObject("style");
        JsonArray children = node.has("children") ? node.getAsJsonArray("children") : null;
        
        // First, generate all children
        List<String> childIdents = new ArrayList<>();
        if (children != null && children.size() > 0) {
            for (int i = 0; i < children.size(); i++) {
                String childIdent = ident + i;
                childIdents.add(childIdent);
                generateNodeCreation(sb, childIdent, children.get(i).getAsJsonObject(), ctx, indent);
            }
        }
        
        // Generate style
        sb.append(ind).append("TaffyStyle ").append(ident).append("Style = new TaffyStyle();\n");
        generateStyleSetters(sb, ident + "Style", style, ind);

        // Optional text content (used for intrinsic sizing via measure funcs)
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
        if (style.has("writingMode") && !style.get("writingMode").isJsonNull()) {
            String wm = style.get("writingMode").getAsString();
            if (wm != null && wm.startsWith("vertical")) {
                verticalWritingMode = true;
            }
        }
        
        // Create node
        if (childIdents.isEmpty()) {
            if (textContent != null) {
                sb.append(ind).append("MeasureFunc ").append(ident).append("Measure = ahemTextMeasure(\"")
                    .append(escapeJavaString(textContent))
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

    private static String escapeJavaString(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '\\':
                    out.append("\\\\");
                    break;
                case '"':
                    out.append("\\\"");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '\u200B':
                    out.append("\\u200B");
                    break;
                default:
                    out.append(c);
                    break;
            }
        }
        return out.toString();
    }
    
    private static void generateStyleSetters(StringBuilder sb, String styleVar, JsonObject style, String ind) {
        // Box sizing (default is BORDER_BOX, only emit if CONTENT_BOX)
        if (style.has("boxSizing") && !style.get("boxSizing").isJsonNull()) {
            String bs = style.get("boxSizing").getAsString();
            if ("content-box".equals(bs)) {
                sb.append(ind).append(styleVar).append(".boxSizing = BoxSizing.CONTENT_BOX;\n");
            }
        }

        // Writing direction (LTR/RTL). Default is LTR so only emit when explicitly set.
        if (style.has("direction") && !style.get("direction").isJsonNull()) {
            String dir = style.get("direction").getAsString();
            if ("rtl".equals(dir)) {
                sb.append(ind).append(styleVar).append(".direction = TaffyDirection.RTL;\n");
            } else if ("ltr".equals(dir)) {
                sb.append(ind).append(styleVar).append(".direction = TaffyDirection.LTR;\n");
            }
        }
        
        // Display
        if (style.has("display") && !style.get("display").isJsonNull()) {
            String display = style.get("display").getAsString();
            switch (display) {
                case "none" -> sb.append(ind).append(styleVar).append(".display = TaffyDisplay.NONE;\n");
                case "block" -> sb.append(ind).append(styleVar).append(".display = TaffyDisplay.BLOCK;\n");
                case "grid" -> sb.append(ind).append(styleVar).append(".display = TaffyDisplay.GRID;\n");
                case "flex" -> {} // Default
            }
        }
        
        // Position
        if (style.has("position") && !style.get("position").isJsonNull()) {
            String position = style.get("position").getAsString();
            if ("absolute".equals(position)) {
                sb.append(ind).append(styleVar).append(".position = TaffyPosition.ABSOLUTE;\n");
            }
        }
        
        // Flex direction
        if (style.has("flexDirection") && !style.get("flexDirection").isJsonNull()) {
            String fd = style.get("flexDirection").getAsString();
            switch (fd) {
                case "row-reverse" -> sb.append(ind).append(styleVar).append(".flexDirection = FlexDirection.ROW_REVERSE;\n");
                case "column" -> sb.append(ind).append(styleVar).append(".flexDirection = FlexDirection.COLUMN;\n");
                case "column-reverse" -> sb.append(ind).append(styleVar).append(".flexDirection = FlexDirection.COLUMN_REVERSE;\n");
            }
        }
        
        // Flex wrap
        if (style.has("flexWrap") && !style.get("flexWrap").isJsonNull()) {
            String fw = style.get("flexWrap").getAsString();
            switch (fw) {
                case "wrap" -> sb.append(ind).append(styleVar).append(".flexWrap = FlexWrap.WRAP;\n");
                case "wrap-reverse" -> sb.append(ind).append(styleVar).append(".flexWrap = FlexWrap.WRAP_REVERSE;\n");
            }
        }
        
        // Align items
        if (style.has("alignItems") && !style.get("alignItems").isJsonNull()) {
            String ai = style.get("alignItems").getAsString();
            String value = mapAlignItems(ai);
            if (value != null) {
                sb.append(ind).append(styleVar).append(".alignItems = ").append(value).append(";\n");
            }
        }
        
        // Align self
        if (style.has("alignSelf") && !style.get("alignSelf").isJsonNull()) {
            String as = style.get("alignSelf").getAsString();
            String value = mapAlignSelf(as);
            if (value != null) {
                sb.append(ind).append(styleVar).append(".alignSelf = ").append(value).append(";\n");
            }
        }
        
        // Align content
        if (style.has("alignContent") && !style.get("alignContent").isJsonNull()) {
            String ac = style.get("alignContent").getAsString();
            String value = mapAlignContent(ac);
            if (value != null) {
                sb.append(ind).append(styleVar).append(".alignContent = ").append(value).append(";\n");
            }
        }
        
        // Justify content
        if (style.has("justifyContent") && !style.get("justifyContent").isJsonNull()) {
            String jc = style.get("justifyContent").getAsString();
            String value = mapJustifyContent(jc);
            if (value != null) {
                sb.append(ind).append(styleVar).append(".justifyContent = ").append(value).append(";\n");
            }
        }
        
        // Text align
        if (style.has("textAlign") && !style.get("textAlign").isJsonNull()) {
            String ta = style.get("textAlign").getAsString();
            String value = mapTextAlign(ta);
            if (value != null) {
                sb.append(ind).append(styleVar).append(".textAlign = ").append(value).append(";\n");
            }
        }
        
        // Justify items
        if (style.has("justifyItems") && !style.get("justifyItems").isJsonNull()) {
            String ji = style.get("justifyItems").getAsString();
            String value = mapAlignItems(ji);  // Same mapping
            if (value != null) {
                sb.append(ind).append(styleVar).append(".justifyItems = ").append(value).append(";\n");
            }
        }
        
        // Justify self
        if (style.has("justifySelf") && !style.get("justifySelf").isJsonNull()) {
            String js = style.get("justifySelf").getAsString();
            String value = mapAlignSelf(js);  // Same mapping
            if (value != null) {
                sb.append(ind).append(styleVar).append(".justifySelf = ").append(value).append(";\n");
            }
        }
        
        // Flex properties - direct field assignment
        if (style.has("flexGrow") && !style.get("flexGrow").isJsonNull()) {
            float fg = style.get("flexGrow").getAsFloat();
            sb.append(ind).append(styleVar).append(".flexGrow = ").append(fg).append("f;\n");
        }
        if (style.has("flexShrink") && !style.get("flexShrink").isJsonNull()) {
            float fs = style.get("flexShrink").getAsFloat();
            sb.append(ind).append(styleVar).append(".flexShrink = ").append(fs).append("f;\n");
        }
        if (style.has("flexBasis") && !style.get("flexBasis").isJsonNull()) {
            String dim = generateDimension(style.getAsJsonObject("flexBasis"));
            if (dim != null) {
                sb.append(ind).append(styleVar).append(".flexBasis = ").append(dim).append(";\n");
            }
        }
        
        // Size
        if (style.has("size") && !style.get("size").isJsonNull()) {
            JsonObject size = style.getAsJsonObject("size");
            String width = size.has("width") && !size.get("width").isJsonNull() ? generateDimension(size.getAsJsonObject("width")) : null;
            String height = size.has("height") && !size.get("height").isJsonNull() ? generateDimension(size.getAsJsonObject("height")) : null;
            if (width != null || height != null) {
                sb.append(ind).append(styleVar).append(".size = new TaffySize<>(")
                    .append(width != null ? width : "TaffyDimension.AUTO").append(", ")
                    .append(height != null ? height : "TaffyDimension.AUTO").append(");\n");
            }
        }
        
        // Min size
        if (style.has("minSize") && !style.get("minSize").isJsonNull()) {
            JsonObject minSize = style.getAsJsonObject("minSize");
            String width = minSize.has("width") && !minSize.get("width").isJsonNull() ? generateDimension(minSize.getAsJsonObject("width")) : null;
            String height = minSize.has("height") && !minSize.get("height").isJsonNull() ? generateDimension(minSize.getAsJsonObject("height")) : null;
            if (width != null || height != null) {
                sb.append(ind).append(styleVar).append(".minSize = new TaffySize<>(")
                    .append(width != null ? width : "TaffyDimension.AUTO").append(", ")
                    .append(height != null ? height : "TaffyDimension.AUTO").append(");\n");
            }
        }
        
        // Max size
        if (style.has("maxSize") && !style.get("maxSize").isJsonNull()) {
            JsonObject maxSize = style.getAsJsonObject("maxSize");
            String width = maxSize.has("width") && !maxSize.get("width").isJsonNull() ? generateDimension(maxSize.getAsJsonObject("width")) : null;
            String height = maxSize.has("height") && !maxSize.get("height").isJsonNull() ? generateDimension(maxSize.getAsJsonObject("height")) : null;
            if (width != null || height != null) {
                sb.append(ind).append(styleVar).append(".maxSize = new TaffySize<>(")
                    .append(width != null ? width : "TaffyDimension.AUTO").append(", ")
                    .append(height != null ? height : "TaffyDimension.AUTO").append(");\n");
            }
        }
        
        // Aspect ratio
        if (style.has("aspectRatio") && !style.get("aspectRatio").isJsonNull()) {
            float ar = style.get("aspectRatio").getAsFloat();
            sb.append(ind).append(styleVar).append(".aspectRatio = ").append(ar).append("f;\n");
        }
        
        // Gap
        if (style.has("gap") && !style.get("gap").isJsonNull()) {
            JsonObject gap = style.getAsJsonObject("gap");
            String column = gap.has("column") && !gap.get("column").isJsonNull() ? generateLengthPercentage(gap.getAsJsonObject("column")) : null;
            String row = gap.has("row") && !gap.get("row").isJsonNull() ? generateLengthPercentage(gap.getAsJsonObject("row")) : null;
            if (column != null || row != null) {
                sb.append(ind).append(styleVar).append(".gap = new TaffySize<>(")
                    .append(column != null ? column : "LengthPercentage.ZERO").append(", ")
                    .append(row != null ? row : "LengthPercentage.ZERO").append(");\n");
            }
        }
        
        // Margin (default to ZERO, not AUTO)
        if (style.has("margin") && !style.get("margin").isJsonNull()) {
            JsonObject margin = style.getAsJsonObject("margin");
            String rect = generateEdges(margin, "LengthPercentageAuto", "ZERO");
            if (rect != null) {
                sb.append(ind).append(styleVar).append(".margin = ").append(rect).append(";\n");
            }
        }
        
        // Padding
        if (style.has("padding") && !style.get("padding").isJsonNull()) {
            JsonObject padding = style.getAsJsonObject("padding");
            String rect = generateEdges(padding, "LengthPercentage", "ZERO");
            if (rect != null) {
                sb.append(ind).append(styleVar).append(".padding = ").append(rect).append(";\n");
            }
        }
        
        // Border
        if (style.has("border") && !style.get("border").isJsonNull()) {
            JsonObject border = style.getAsJsonObject("border");
            String rect = generateEdges(border, "LengthPercentage", "ZERO");
            if (rect != null) {
                sb.append(ind).append(styleVar).append(".border = ").append(rect).append(";\n");
            }
        }
        
        // Inset (default to AUTO)
        if (style.has("inset") && !style.get("inset").isJsonNull()) {
            JsonObject inset = style.getAsJsonObject("inset");
            String rect = generateEdges(inset, "LengthPercentageAuto", "AUTO");
            if (rect != null) {
                sb.append(ind).append(styleVar).append(".inset = ").append(rect).append(";\n");
            }
        }
        
        // Grid template columns
        if (style.has("gridTemplateColumns") && !style.get("gridTemplateColumns").isJsonNull()) {
            JsonArray cols = style.getAsJsonArray("gridTemplateColumns");
            generateGridTemplate(sb, styleVar, "gridTemplateColumns", cols, ind);
        }
        
        // Grid template rows
        if (style.has("gridTemplateRows") && !style.get("gridTemplateRows").isJsonNull()) {
            JsonArray rows = style.getAsJsonArray("gridTemplateRows");
            generateGridTemplate(sb, styleVar, "gridTemplateRows", rows, ind);
        }
        
        // Grid auto columns
        if (style.has("gridAutoColumns") && !style.get("gridAutoColumns").isJsonNull()) {
            JsonArray cols = style.getAsJsonArray("gridAutoColumns");
            generateGridTemplate(sb, styleVar, "gridAutoColumns", cols, ind);
        }
        
        // Grid auto rows
        if (style.has("gridAutoRows") && !style.get("gridAutoRows").isJsonNull()) {
            JsonArray rows = style.getAsJsonArray("gridAutoRows");
            generateGridTemplate(sb, styleVar, "gridAutoRows", rows, ind);
        }
        
        // Grid auto flow
        if (style.has("gridAutoFlow") && !style.get("gridAutoFlow").isJsonNull()) {
            JsonObject gaf = style.getAsJsonObject("gridAutoFlow");
            String direction = gaf.get("direction").getAsString();
            String algorithm = gaf.get("algorithm").getAsString();
            String value = switch (direction + "_" + algorithm) {
                case "row_sparse" -> "GridAutoFlow.ROW";
                case "column_sparse" -> "GridAutoFlow.COLUMN";
                case "row_dense" -> "GridAutoFlow.ROW_DENSE";
                case "column_dense" -> "GridAutoFlow.COLUMN_DENSE";
                default -> null;
            };
            if (value != null) {
                sb.append(ind).append(styleVar).append(".gridAutoFlow = ").append(value).append(";\n");
            }
        }
        
        // Grid row
        if (style.has("gridRowStart") && !style.get("gridRowStart").isJsonNull()) {
            JsonObject grs = style.getAsJsonObject("gridRowStart");
            String placement = generateGridPlacement(grs);
            if (placement != null) {
                sb.append(ind).append(styleVar).append(".gridRow = new TaffyLine<>(").append(placement);
                if (style.has("gridRowEnd") && !style.get("gridRowEnd").isJsonNull()) {
                    sb.append(", ").append(generateGridPlacement(style.getAsJsonObject("gridRowEnd")));
                } else {
                    sb.append(", GridPlacement.auto()");
                }
                sb.append(");\n");
            }
        } else if (style.has("gridRowEnd") && !style.get("gridRowEnd").isJsonNull()) {
            String placement = generateGridPlacement(style.getAsJsonObject("gridRowEnd"));
            if (placement != null) {
                sb.append(ind).append(styleVar).append(".gridRow = new TaffyLine<>(GridPlacement.auto(), ").append(placement).append(");\n");
            }
        }
        
        // Grid column
        if (style.has("gridColumnStart") && !style.get("gridColumnStart").isJsonNull()) {
            JsonObject gcs = style.getAsJsonObject("gridColumnStart");
            String placement = generateGridPlacement(gcs);
            if (placement != null) {
                sb.append(ind).append(styleVar).append(".gridColumn = new TaffyLine<>(").append(placement);
                if (style.has("gridColumnEnd") && !style.get("gridColumnEnd").isJsonNull()) {
                    sb.append(", ").append(generateGridPlacement(style.getAsJsonObject("gridColumnEnd")));
                } else {
                    sb.append(", GridPlacement.auto()");
                }
                sb.append(");\n");
            }
        } else if (style.has("gridColumnEnd") && !style.get("gridColumnEnd").isJsonNull()) {
            String placement = generateGridPlacement(style.getAsJsonObject("gridColumnEnd"));
            if (placement != null) {
                sb.append(ind).append(styleVar).append(".gridColumn = new TaffyLine<>(GridPlacement.auto(), ").append(placement).append(");\n");
            }
        }
        
        // Overflow
        if ((style.has("overflowX") && !style.get("overflowX").isJsonNull()) ||
            (style.has("overflowY") && !style.get("overflowY").isJsonNull())) {
            String ox = style.has("overflowX") && !style.get("overflowX").isJsonNull() ? style.get("overflowX").getAsString() : "visible";
            String oy = style.has("overflowY") && !style.get("overflowY").isJsonNull() ? style.get("overflowY").getAsString() : "visible";
            String oxVal = mapOverflow(ox);
            String oyVal = mapOverflow(oy);
            if (!"Overflow.VISIBLE".equals(oxVal) || !"Overflow.VISIBLE".equals(oyVal)) {
                sb.append(ind).append(styleVar).append(".overflow = new TaffyPoint<>(").append(oxVal).append(", ").append(oyVal).append(");\n");
                // Set scrollbar width if overflow is scroll
                if ("scroll".equals(ox) || "scroll".equals(oy)) {
                    if (style.has("scrollbarWidth") && !style.get("scrollbarWidth").isJsonNull()) {
                        float scrollbarWidth = style.get("scrollbarWidth").getAsFloat();
                        sb.append(ind).append(styleVar).append(".scrollbarWidth = ").append(scrollbarWidth).append("f;\n");
                    }
                }
            }
        }
    }
    
    private static void generateGridTemplate(StringBuilder sb, String styleVar, String propName, JsonArray tracks, String ind) {
        if (tracks.isEmpty()) return;
        
        // Check if there's any auto-fill or auto-fit in the template
        boolean hasAutoRepetition = false;
        for (JsonElement elem : tracks) {
            JsonObject track = elem.getAsJsonObject();
            String kind = track.has("kind") ? track.get("kind").getAsString() : "scalar";
            if ("function".equals(kind) && "repeat".equals(track.get("name").getAsString())) {
                JsonArray args = track.getAsJsonArray("arguments");
                if (args.size() >= 2) {
                    JsonObject countObj = args.get(0).getAsJsonObject();
                    String unit = countObj.has("unit") ? countObj.get("unit").getAsString() : "";
                    if ("auto-fill".equals(unit) || "auto-fit".equals(unit)) {
                        hasAutoRepetition = true;
                        break;
                    }
                }
            }
        }
        
        if (hasAutoRepetition) {
            // Use the new gridTemplateColumnsWithRepeat / gridTemplateRowsWithRepeat
            String withRepeatProp = propName + "WithRepeat";
            sb.append(ind).append(styleVar).append(".").append(withRepeatProp).append(" = new java.util.ArrayList<>();\n");
            
            for (JsonElement elem : tracks) {
                JsonObject track = elem.getAsJsonObject();
                String kind = track.has("kind") ? track.get("kind").getAsString() : "scalar";
                
                if ("function".equals(kind) && "repeat".equals(track.get("name").getAsString())) {
                    JsonArray args = track.getAsJsonArray("arguments");
                    if (args.size() >= 2) {
                        JsonObject countObj = args.get(0).getAsJsonObject();
                        String unit = countObj.has("unit") ? countObj.get("unit").getAsString() : "";
                        
                        if ("auto-fill".equals(unit) || "auto-fit".equals(unit)) {
                            // Generate auto-fill/auto-fit repeat
                            StringBuilder tracksList = new StringBuilder();
                            for (int j = 1; j < args.size(); j++) {
                                if (j > 1) tracksList.append(", ");
                                tracksList.append(generateTrackSizingFunction(args.get(j).getAsJsonObject()));
                            }
                            String method = "auto-fill".equals(unit) ? "autoFill" : "autoFit";
                            sb.append(ind).append(styleVar).append(".").append(withRepeatProp)
                              .append(".add(GridTemplateComponent.").append(method).append("(")
                              .append(tracksList).append("));\n");
                        } else if ("integer".equals(unit)) {
                            // Integer count - expand to individual tracks
                            int count = countObj.get("value").getAsInt();
                            for (int i = 0; i < count; i++) {
                                for (int j = 1; j < args.size(); j++) {
                                    String tsf = generateTrackSizingFunction(args.get(j).getAsJsonObject());
                                    if (tsf != null) {
                                        sb.append(ind).append(styleVar).append(".").append(withRepeatProp)
                                          .append(".add(GridTemplateComponent.single(").append(tsf).append("));\n");
                                    }
                                }
                            }
                        }
                    }
                } else {
                    String tsf = generateTrackSizingFunction(track);
                    if (tsf != null) {
                        sb.append(ind).append(styleVar).append(".").append(withRepeatProp)
                          .append(".add(GridTemplateComponent.single(").append(tsf).append("));\n");
                    }
                }
            }
        } else {
            // No auto-repetition, use the simple list
            for (JsonElement elem : tracks) {
                JsonObject track = elem.getAsJsonObject();
                String kind = track.has("kind") ? track.get("kind").getAsString() : "scalar";
                
                // Handle repeat() - expand it to multiple tracks
                if ("function".equals(kind) && "repeat".equals(track.get("name").getAsString())) {
                    JsonArray args = track.getAsJsonArray("arguments");
                    if (args.size() >= 2) {
                        JsonObject countObj = args.get(0).getAsJsonObject();
                        if ("integer".equals(countObj.get("unit").getAsString())) {
                            int count = countObj.get("value").getAsInt();
                            for (int i = 0; i < count; i++) {
                                for (int j = 1; j < args.size(); j++) {
                                    String tsf = generateTrackSizingFunction(args.get(j).getAsJsonObject());
                                    if (tsf != null) {
                                        sb.append(ind).append(styleVar).append(".").append(propName).append(".add(").append(tsf).append(");\n");
                                    }
                                }
                            }
                        }
                    }
                } else {
                    String tsf = generateTrackSizingFunction(track);
                    if (tsf != null) {
                        sb.append(ind).append(styleVar).append(".").append(propName).append(".add(").append(tsf).append(");\n");
                    }
                }
            }
        }
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
                    if (args.size() >= 1) {
                        // fitContent takes LengthPercentage, extract from the track sizing function
                        JsonObject argObj = args.get(0).getAsJsonObject();
                        String unit = argObj.has("unit") ? argObj.get("unit").getAsString() : "";
                        String lp = switch (unit) {
                            case "px" -> "LengthPercentage.length(" + argObj.get("value").getAsFloat() + "f)";
                            case "percent" -> "LengthPercentage.percent(" + argObj.get("value").getAsFloat() + "f)";
                            default -> "LengthPercentage.ZERO";
                        };
                        yield "TrackSizingFunction.fitContent(" + lp + ")";
                    }
                    yield null;
                }
                case "repeat" -> {
                    // Simplified repeat handling
                    yield null; // Skip for now
                }
                default -> null;
            };
        }
        
        // Scalar value
        String unit = track.get("unit").getAsString();
        return switch (unit) {
            case "px" -> "TrackSizingFunction.fixed(LengthPercentage.length(" + track.get("value").getAsFloat() + "f))";
            case "percent" -> "TrackSizingFunction.fixed(LengthPercentage.percent(" + track.get("value").getAsFloat() + "f))";
            case "fraction" -> "TrackSizingFunction.fr(" + track.get("value").getAsFloat() + "f)";
            case "auto" -> "TrackSizingFunction.auto()";
            case "min-content" -> "TrackSizingFunction.minContent()";
            case "max-content" -> "TrackSizingFunction.maxContent()";
            default -> null;
        };
    }
    
    private static String generateGridPlacement(JsonObject placement) {
        String kind = placement.get("kind").getAsString();
        return switch (kind) {
            case "auto" -> "GridPlacement.auto()";
            case "line" -> "GridPlacement.line(" + placement.get("value").getAsInt() + ")";
            case "span" -> "GridPlacement.span(" + placement.get("value").getAsInt() + ")";
            default -> null;
        };
    }
    
    private static String mapOverflow(String value) {
        return switch (value) {
            case "hidden" -> "Overflow.HIDDEN";
            case "scroll" -> "Overflow.SCROLL";
            case "auto" -> "Overflow.AUTO";
            default -> "Overflow.VISIBLE";
        };
    }
    
    private static String mapTextAlign(String value) {
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
    
    private static String mapAlignItems(String value) {
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
    
    private static String mapAlignSelf(String value) {
        // Style.alignSelf and Style.justifySelf are AlignItems type, not AlignSelf
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
    
    private static String mapAlignContent(String value) {
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
    
    private static String mapJustifyContent(String value) {
        // justifyContent field is AlignContent type in Style class
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
    
    private static String generateDimension(JsonObject dim) {
        if (dim == null) return null;
        String unit = dim.get("unit").getAsString();
        return switch (unit) {
            case "auto" -> "TaffyDimension.AUTO";
            case "px" -> "TaffyDimension.length(" + dim.get("value").getAsFloat() + "f)";
            case "percent" -> "TaffyDimension.percent(" + dim.get("value").getAsFloat() + "f)";
            case "min-content" -> "TaffyDimension.minContent()";
            case "max-content" -> "TaffyDimension.maxContent()";
            case "fit-content" -> "TaffyDimension.fitContent()";
            case "stretch" -> "TaffyDimension.stretch()";
            default -> null;
        };
    }
    
    private static String generateLengthPercentage(JsonObject lp) {
        if (lp == null) return null;
        String unit = lp.get("unit").getAsString();
        return switch (unit) {
            case "px" -> "LengthPercentage.length(" + lp.get("value").getAsFloat() + "f)";
            case "percent" -> "LengthPercentage.percent(" + lp.get("value").getAsFloat() + "f)";
            default -> null;
        };
    }
    
    private static String generateLengthPercentageAuto(JsonObject lpa) {
        if (lpa == null) return null;
        String unit = lpa.get("unit").getAsString();
        return switch (unit) {
            case "auto" -> "LengthPercentageAuto.AUTO";
            case "px" -> "LengthPercentageAuto.length(" + lpa.get("value").getAsFloat() + "f)";
            case "percent" -> "LengthPercentageAuto.percent(" + lpa.get("value").getAsFloat() + "f)";
            case "min-content" -> "LengthPercentageAuto.minContent()";
            case "max-content" -> "LengthPercentageAuto.maxContent()";
            case "fit-content" -> "LengthPercentageAuto.fitContent()";
            case "stretch" -> "LengthPercentageAuto.stretch()";
            default -> null;
        };
    }
    
    private static String generateEdges(JsonObject edges, String type, String defaultSuffix) {
        String left = edges.has("left") && !edges.get("left").isJsonNull() 
            ? ("LengthPercentage".equals(type) ? generateLengthPercentage(edges.getAsJsonObject("left")) : generateLengthPercentageAuto(edges.getAsJsonObject("left")))
            : null;
        String right = edges.has("right") && !edges.get("right").isJsonNull()
            ? ("LengthPercentage".equals(type) ? generateLengthPercentage(edges.getAsJsonObject("right")) : generateLengthPercentageAuto(edges.getAsJsonObject("right")))
            : null;
        String top = edges.has("top") && !edges.get("top").isJsonNull()
            ? ("LengthPercentage".equals(type) ? generateLengthPercentage(edges.getAsJsonObject("top")) : generateLengthPercentageAuto(edges.getAsJsonObject("top")))
            : null;
        String bottom = edges.has("bottom") && !edges.get("bottom").isJsonNull()
            ? ("LengthPercentage".equals(type) ? generateLengthPercentage(edges.getAsJsonObject("bottom")) : generateLengthPercentageAuto(edges.getAsJsonObject("bottom")))
            : null;
        
        if (left == null && right == null && top == null && bottom == null) {
            return null;
        }
        
        // Use the provided defaultSuffix (e.g., "ZERO" or "AUTO")
        String defaultValue = type + "." + defaultSuffix;
        return "new TaffyRect<>(" + 
            (left != null ? left : defaultValue) + ", " +
            (right != null ? right : defaultValue) + ", " +
            (top != null ? top : defaultValue) + ", " +
            (bottom != null ? bottom : defaultValue) + ")";
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
            case "px" -> "AvailableSpace.definite(" + dim.get("value").getAsFloat() + "f)";
            case "max-content" -> "AvailableSpace.maxContent()";
            case "min-content" -> "AvailableSpace.minContent()";
            default -> "AvailableSpace.maxContent()";
        };
    }
    
    private static void generateAssertions(StringBuilder sb, String ident, JsonObject node, boolean useRounding, int indent) {
        String ind = "        ".substring(0, indent * 4);
        
        JsonObject layout = useRounding 
            ? node.getAsJsonObject("smartRoundedLayout")
            : node.getAsJsonObject("unroundedLayout");
        
        float width = layout.get("width").getAsFloat();
        float height = layout.get("height").getAsFloat();
        float x = layout.get("x").getAsFloat();
        float y = layout.get("y").getAsFloat();
        
        sb.append(ind).append("Layout ").append(ident).append("Layout = tree.getLayout(").append(ident).append(");\n");
        
        if (useRounding) {
            sb.append(ind).append("assertEquals(").append(width).append("f, ").append(ident).append("Layout.size().width, \"width of ").append(ident).append("\");\n");
            sb.append(ind).append("assertEquals(").append(height).append("f, ").append(ident).append("Layout.size().height, \"height of ").append(ident).append("\");\n");
            sb.append(ind).append("assertEquals(").append(x).append("f, ").append(ident).append("Layout.location().x, \"x of ").append(ident).append("\");\n");
            sb.append(ind).append("assertEquals(").append(y).append("f, ").append(ident).append("Layout.location().y, \"y of ").append(ident).append("\");\n");
        } else {
            sb.append(ind).append("assertEquals(").append(width).append("f, ").append(ident).append("Layout.size().width, EPSILON, \"width of ").append(ident).append("\");\n");
            sb.append(ind).append("assertEquals(").append(height).append("f, ").append(ident).append("Layout.size().height, EPSILON, \"height of ").append(ident).append("\");\n");
            sb.append(ind).append("assertEquals(").append(x).append("f, ").append(ident).append("Layout.location().x, EPSILON, \"x of ").append(ident).append("\");\n");
            sb.append(ind).append("assertEquals(").append(y).append("f, ").append(ident).append("Layout.location().y, EPSILON, \"y of ").append(ident).append("\");\n");
        }
        
        // Generate assertions for children
        if (node.has("children") && !node.get("children").isJsonNull()) {
            JsonArray children = node.getAsJsonArray("children");
            for (int i = 0; i < children.size(); i++) {
                generateAssertions(sb, ident + i, children.get(i).getAsJsonObject(), useRounding, indent);
            }
        }
    }
    
    private static String toPascalCase(String input) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : input.toCharArray()) {
            if (c == '_' || c == '-') {
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
    
    private static String toMethodName(String input) {
        // Convert test name to valid Java method name
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : input.toCharArray()) {
            if (c == '_' || c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        // Ensure first char is lowercase
        if (result.length() > 0) {
            result.setCharAt(0, Character.toLowerCase(result.charAt(0)));
        }
        return result.toString();
    }
    
    private static String escapeString(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
    
    private static void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (Stream<Path> paths = Files.walk(dir)) {
                paths.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            }
        }
    }
    
    private static class FixtureInfo {
        final String name;
        final Path path;
        final String category;
        
        FixtureInfo(String name, Path path, String category) {
            this.name = name;
            this.path = path;
            this.category = category.isEmpty() ? "root" : category;
        }
    }
    
    private static class NodeContext {
        int nodeCounter = 0;
    }
}
