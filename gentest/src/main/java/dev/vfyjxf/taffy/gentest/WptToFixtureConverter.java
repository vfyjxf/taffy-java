package dev.vfyjxf.taffy.gentest;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;

/**
 * Converts WPT (Web Platform Tests) HTML files into taffy fixture HTML format.
 * <p>
 * Taffy fixtures include test_helper.js and test_base_style.css which provide
 * the JavaScript extraction logic and CSS defaults for the test generation pipeline.
 * <p>
 * Key differences handled:
 * - test_base_style.css sets display:flex on all divs; we reset to display:block
 * - WPT metadata (links, references, descriptions) is stripped
 * - Reference/visual-comparison elements are removed
 * - The outermost test div gets id="test-root"
 */
public class WptToFixtureConverter {

    // Elements that cannot be represented in taffy fixtures
    private static final Pattern UNSUPPORTED_ELEMENTS = Pattern.compile(
            "<(img|video|canvas|textarea|input|select|svg|table|fieldset)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Script tags (tests with JS logic cannot be converted)
    private static final Pattern SCRIPT_TAG = Pattern.compile(
            "<script\\b", Pattern.CASE_INSENSITIVE
    );

    // Metadata tags to strip (may span multiple lines)
    private static final Pattern METADATA_TAG = Pattern.compile(
            "<(link|meta)\\b[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // <title> element (has content between tags)
    private static final Pattern TITLE_ELEMENT = Pattern.compile(
            "<title\\b[^>]*>.*?</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Metadata attribute leftovers (e.g. title="..." on next line after a multi-line tag)
    private static final Pattern ORPHAN_ATTR = Pattern.compile(
            "^\\s*(title|content|href|rel)\\s*=\\s*\"[^\"]*\"\\s*>?\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );

    // <p> description lines
    private static final Pattern P_TAG = Pattern.compile(
            "<p\\b[^>]*>.*?</p>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Self-closing <p> (no content between tags on same line)
    private static final Pattern P_LINE = Pattern.compile(
            "^\\s*<p\\b.*$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );

    // <style> block extraction
    private static final Pattern STYLE_BLOCK = Pattern.compile(
            "<style[^>]*>(.*?)</style>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Reference divs (used only for visual comparison in WPT tests)
    private static final Pattern REFERENCE_DIV = Pattern.compile(
            "<div\\s+id\\s*=\\s*\"reference[^\"]*\"[^>]*>.*?</div>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Visual-only CSS properties (don't affect layout)
    private static final Pattern VISUAL_CSS = Pattern.compile(
            "\\b(background(-color)?|color|z-index)\\s*:\\s*[^;]+;?",
            Pattern.CASE_INSENSITIVE
    );

    // CSS rules targeting reference elements (for removal from <style> blocks)
    private static final Pattern REFERENCE_CSS_RULE = Pattern.compile(
            "#reference[^{]*\\{[^}]*\\}",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Body tag extraction
    private static final Pattern BODY_CONTENT = Pattern.compile(
            "<body[^>]*>(.*)</body>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Head tag content
    private static final Pattern HEAD_CONTENT = Pattern.compile(
            "<head[^>]*>(.*?)</head>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // HTML comments
    private static final Pattern HTML_COMMENT = Pattern.compile(
            "<!--.*?-->", Pattern.DOTALL
    );

    private static final String FIXTURE_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <script src="../../scripts/gentest/test_helper.js"></script>
              <link rel="stylesheet" type="text/css" href="../../scripts/gentest/test_base_style.css">
              <style>
                /* Reset base CSS defaults to match browser defaults for WPT test compatibility */
                div, span { display: block; }
              </style>
            %s  <title>
                %s
              </title>
            </head>
            <body>
            
            %s
            
            </body>
            </html>
            """;

    /**
     * Deprecated positional entry point kept for old build scripts.
     *
     * <p>Prefer: {@code Generator --source wpt --convert-to-fixtures
     * --convert-input <dir> --convert-output <dir>}.</p>
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: WptToFixtureConverter <wptDir> <outputDir> [--prefix <prefix>] [--pattern <glob>] [--recursive]");
            System.err.println("[deprecated] Prefer: Generator --source wpt --convert-to-fixtures " +
                    "--convert-input <dir> --convert-output <dir>");
            System.exit(2);
        }
        Path input = Paths.get(args[0]);
        Path output = Paths.get(args[1]);
        String prefix = "wpt_";
        String pattern = "flex-aspect-ratio-*.html";
        boolean recursive = false;
        for (int i = 2; i < args.length; i++) {
            switch (args[i]) {
                case "--prefix" -> prefix = args[++i];
                case "--pattern" -> pattern = args[++i];
                case "--recursive" -> recursive = true;
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    System.exit(2);
                }
            }
        }
        System.err.println("[deprecated] WptToFixtureConverter positional CLI; see gentest/README.md.");
        convert(input, output, prefix, pattern, recursive, false);
    }

    /**
     * Convert WPT HTML files into taffy fixtures.
     *
     * @return number of converted files
     */
    static int convert(Path input, Path output, String prefix, String pattern,
                       boolean recursive, boolean dryRun) throws IOException {
        if (!Files.isDirectory(input)) {
            throw new GeneratorOptions.UsageException("convert input is not a directory: " + input);
        }
        if (!dryRun) {
            Files.createDirectories(output);
        }

        List<Path> files = new ArrayList<>();
        java.nio.file.PathMatcher matcher = input.getFileSystem().getPathMatcher("glob:" + pattern);
        if (recursive) {
            try (Stream<Path> stream = Files.walk(input)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> matcher.matches(path.getFileName()))
                        .forEach(files::add);
            }
        } else {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(input, pattern)) {
                stream.forEach(files::add);
            }
        }
        files.sort(Comparator.comparing(path -> path.getFileName().toString()));

        int converted = 0;
        int skipped = 0;
        for (Path wptFile : files) {
            String content = Files.readString(wptFile).replace("\r\n", "\n").replace('\r', '\n');
            String fileName = wptFile.getFileName().toString();

            // WPT reference files are not tests; mirror the direct WPT discovery rules.
            String lowerName = fileName.toLowerCase(java.util.Locale.ROOT);
            if (lowerName.endsWith("-ref.html") || lowerName.endsWith("-notref.html")
                    || lowerName.contains("-manual")) {
                System.out.println("  SKIP (reference/manual): " + fileName);
                skipped++;
                continue;
            }

            if (UNSUPPORTED_ELEMENTS.matcher(content).find()) {
                System.out.println("  SKIP (unsupported element): " + fileName);
                skipped++;
                continue;
            }
            if (SCRIPT_TAG.matcher(content).find()) {
                System.out.println("  SKIP (has script): " + fileName);
                skipped++;
                continue;
            }

            String fixture = convertToFixture(content, fileName);
            if (fixture == null) {
                System.out.println("  SKIP (conversion failed): " + fileName);
                skipped++;
                continue;
            }

            String fixtureName = prefix + fileName.replace('-', '_');
            Path outputPath = output.resolve(fixtureName);
            if (dryRun) {
                System.out.println("  DRY-RUN would write: " + outputPath);
            } else {
                Files.writeString(outputPath, fixture.replace("\r\n", "\n").replace('\r', '\n'),
                        java.nio.charset.StandardCharsets.UTF_8);
                System.out.println("  OK: " + fileName + " -> " + fixtureName);
            }
            converted++;
        }
        System.out.println("\n" + (dryRun ? "Would convert" : "Converted") + ": " + converted + ", Skipped: " + skipped);
        return converted;
    }

    static String convertToFixture(String htmlContent, String originalName) {
        // Remove HTML comments (they may contain nested HTML that confuses parsing)
        String content = HTML_COMMENT.matcher(htmlContent).replaceAll("");

        // Extract CSS from <style> blocks for inlining into elements
        List<String> cssBlocks = new ArrayList<>();
        Matcher styleMatcher = STYLE_BLOCK.matcher(content);
        while (styleMatcher.find()) {
            String cssContent = styleMatcher.group(1).trim();
            // Remove visual-only CSS rules (background, color)
            cssContent = cleanCssBlock(cssContent);
            if (!cssContent.isBlank()) {
                cssBlocks.add(cssContent);
            }
        }
        // Remove <style> blocks from content
        content = STYLE_BLOCK.matcher(content).replaceAll("");

        // Extract body content (or use full content if no body tags)
        String bodyContent;
        Matcher bodyMatcher = BODY_CONTENT.matcher(content);
        if (bodyMatcher.find()) {
            bodyContent = bodyMatcher.group(1).trim();
        } else {
            // No explicit <body> - extract content after metadata
            bodyContent = content;
            // Remove doctype
            bodyContent = bodyContent.replaceFirst("(?i)<!DOCTYPE[^>]*>", "");
            // Remove <html> tags
            bodyContent = bodyContent.replaceFirst("(?i)<html[^>]*>", "");
            bodyContent = bodyContent.replaceFirst("(?i)</html>", "");
            // Remove head content
            bodyContent = HEAD_CONTENT.matcher(bodyContent).replaceAll("");
            // Remove standalone head/body tags
            bodyContent = bodyContent.replaceFirst("(?i)<head[^>]*>", "");
            bodyContent = bodyContent.replaceFirst("(?i)</head>", "");
            bodyContent = bodyContent.replaceFirst("(?i)<body[^>]*>", "");
            bodyContent = bodyContent.replaceFirst("(?i)</body>", "");
        }

        // Remove metadata tags (may span multiple lines)
        bodyContent = TITLE_ELEMENT.matcher(bodyContent).replaceAll("");
        bodyContent = METADATA_TAG.matcher(bodyContent).replaceAll("");
        // Remove orphaned attributes from multi-line metadata tags
        bodyContent = ORPHAN_ATTR.matcher(bodyContent).replaceAll("");

        // Remove <p> descriptions
        bodyContent = P_TAG.matcher(bodyContent).replaceAll("");
        bodyContent = P_LINE.matcher(bodyContent).replaceAll("");

        // Remove reference divs
        bodyContent = REFERENCE_DIV.matcher(bodyContent).replaceAll("");

        // Clean up extra blank lines
        bodyContent = bodyContent.replaceAll("(?m)^\\s*$\\n", "").trim();

        if (bodyContent.isBlank()) {
            return null;
        }

        // Inline CSS from <style> blocks into matching elements.
        // test_helper.js reads e.style.xxx (inline only), not getComputedStyle(),
        // so CSS from <style> blocks is invisible to the test generator.
        for (String css : cssBlocks) {
            bodyContent = inlineStyleBlockCss(bodyContent, css);
        }

        // Strip visual-only CSS from inline styles (background, color)
        bodyContent = cleanInlineStyles(bodyContent);

        // Add display:block to divs without explicit display in inline style.
        // test_helper.js reads e.style.display (inline only), not computedStyle.
        // Without this, child divs default to flex (from test_base_style.css)
        // but test_helper.js sees null → TestGenerator uses flex as default.
        bodyContent = addBlockDisplayToNonFlexDivs(bodyContent);

        // Add id="test-root" to the outermost div
        bodyContent = addTestRootId(bodyContent);

        return FIXTURE_TEMPLATE.formatted("", originalName, bodyContent);
    }

    /**
     * Remove visual-only CSS properties from a CSS block.
     */
    private static String cleanCssBlock(String css) {
        // Remove CSS rules targeting reference elements
        css = REFERENCE_CSS_RULE.matcher(css).replaceAll("");

        // Remove individual visual properties
        css = VISUAL_CSS.matcher(css).replaceAll("");

        // Remove empty rule blocks
        css = css.replaceAll("[^{}]+\\{\\s*\\}", "");

        return css.trim();
    }

    /**
     * Remove visual-only CSS from inline style attributes.
     */
    private static String cleanInlineStyles(String html) {
        // Find style="..." attributes and clean them
        Pattern styleAttr = Pattern.compile("style\\s*=\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
        Matcher m = styleAttr.matcher(html);
        StringBuilder result = new StringBuilder();
        while (m.find()) {
            String styleValue = m.group(1);
            String cleaned = VISUAL_CSS.matcher(styleValue).replaceAll("").trim();
            // Remove trailing/leading semicolons and extra spaces
            cleaned = cleaned.replaceAll(";\\s*;", ";").replaceAll("^;|;$", "").trim();
            if (cleaned.isEmpty()) {
                m.appendReplacement(result, "");
            } else {
                m.appendReplacement(result, "style=\"" + Matcher.quoteReplacement(cleaned) + "\"");
            }
        }
        m.appendTail(result);
        return result.toString();
    }

    /**
     * Add display:block to divs that don't already have display in their inline style.
     * test_helper.js reads e.style.display (inline styles only), so display from
     * CSS rules (like test_base_style.css's div{display:flex}) is invisible.
     * WPT tests assume browser default display:block for divs.
     */
    private static String addBlockDisplayToNonFlexDivs(String html) {
        // Match <div with or without style attribute
        Pattern divTag = Pattern.compile(
                "(<div)(\\b[^>]*)(>)", Pattern.CASE_INSENSITIVE);
        Matcher m = divTag.matcher(html);
        StringBuilder result = new StringBuilder();
        while (m.find()) {
            String prefix = m.group(1);    // "<div"
            String attrs = m.group(2);     // " style=\"...\"" or other attrs
            String close = m.group(3);     // ">"

            // Check if this div already has display in its inline style
            if (attrs.matches("(?i).*style\\s*=\\s*\"[^\"]*display\\s*:.*")) {
                // Already has display in inline style, leave it
                m.appendReplacement(result, Matcher.quoteReplacement(m.group(0)));
            } else if (attrs.matches("(?i).*style\\s*=\\s*\"[^\"]*\".*")) {
                // Has a style attribute but no display - add display:block
                String newAttrs = attrs.replaceFirst(
                        "(?i)(style\\s*=\\s*\")([^\"]*)(\")",
                        "$1display: block; $2$3"
                );
                m.appendReplacement(result, Matcher.quoteReplacement(prefix + newAttrs + close));
            } else {
                // No style attribute - add one with display:block
                m.appendReplacement(result,
                        Matcher.quoteReplacement(prefix + " style=\"display: block\"" + attrs + close));
            }
        }
        m.appendTail(result);
        return result.toString();
    }

    // CSS logical property → physical property mapping (horizontal writing mode)
    private static final Map<String, String> LOGICAL_TO_PHYSICAL = Map.ofEntries(
            Map.entry("inline-size", "width"),
            Map.entry("block-size", "height"),
            Map.entry("min-inline-size", "min-width"),
            Map.entry("min-block-size", "min-height"),
            Map.entry("max-inline-size", "max-width"),
            Map.entry("max-block-size", "max-height"),
            Map.entry("margin-inline-start", "margin-left"),
            Map.entry("margin-inline-end", "margin-right"),
            Map.entry("margin-block-start", "margin-top"),
            Map.entry("margin-block-end", "margin-bottom"),
            Map.entry("padding-inline-start", "padding-left"),
            Map.entry("padding-inline-end", "padding-right"),
            Map.entry("padding-block-start", "padding-top"),
            Map.entry("padding-block-end", "padding-bottom"),
            Map.entry("border-inline-start-width", "border-left-width"),
            Map.entry("border-inline-end-width", "border-right-width"),
            Map.entry("border-block-start-width", "border-top-width"),
            Map.entry("border-block-end-width", "border-bottom-width"),
            Map.entry("inset-inline-start", "left"),
            Map.entry("inset-inline-end", "right"),
            Map.entry("inset-block-start", "top"),
            Map.entry("inset-block-end", "bottom")
    );

    /**
     * Inline CSS rules from a style block into matching elements.
     * Supports #id, .class, and .class > element selectors.
     */
    private static String inlineStyleBlockCss(String html, String cssContent) {
        Pattern rulePattern = Pattern.compile("([^{}]+?)\\s*\\{([^}]+)\\}", Pattern.DOTALL);
        Matcher ruleMatcher = rulePattern.matcher(cssContent);

        while (ruleMatcher.find()) {
            String selector = ruleMatcher.group(1).trim();
            String declarations = ruleMatcher.group(2).trim();

            // Normalize declarations
            declarations = declarations.replaceAll("\\s+", " ").trim();
            if (!declarations.endsWith(";")) declarations += ";";

            // Convert logical properties to physical
            declarations = convertLogicalToPhysical(declarations);

            html = inlineRuleBySelector(html, selector, declarations);
        }

        return html;
    }

    private static String convertLogicalToPhysical(String declarations) {
        for (var entry : LOGICAL_TO_PHYSICAL.entrySet()) {
            declarations = declarations.replaceAll(
                    "\\b" + Pattern.quote(entry.getKey()) + "\\s*:",
                    entry.getValue() + ":"
            );
        }
        return declarations;
    }

    private static String inlineRuleBySelector(String html, String selector, String declarations) {
        if (selector.startsWith("#")) {
            String id = selector.substring(1);
            return mergeStyleIntoElementById(html, id, declarations);
        } else if (selector.matches("\\.[\\w-]+\\s*>\\s*\\w+")) {
            Matcher m = Pattern.compile("\\.(\\w[\\w-]*)\\s*>\\s*(\\w+)").matcher(selector);
            if (m.matches()) {
                return mergeStyleIntoChildOfClass(html, m.group(1), m.group(2), declarations);
            }
        } else if (selector.startsWith(".") && !selector.contains(" ") && !selector.contains(">")) {
            String className = selector.substring(1);
            return mergeStyleIntoElementsByClass(html, className, declarations);
        }
        System.out.println("  WARN: Cannot inline CSS selector: " + selector);
        return html;
    }

    private static String mergeStyleIntoElementById(String html, String id, String declarations) {
        Pattern p = Pattern.compile(
                "(<(?:div|span)\\b)([^>]*\\bid\\s*=\\s*\"" + Pattern.quote(id) + "\"[^>]*)(>)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = p.matcher(html);
        if (m.find()) {
            String merged = mergeDeclarationsIntoAttrs(m.group(2), declarations);
            return html.substring(0, m.start()) +
                    m.group(1) + merged + m.group(3) +
                    html.substring(m.end());
        }
        return html;
    }

    private static String mergeStyleIntoElementsByClass(String html, String className, String declarations) {
        Pattern p = Pattern.compile(
                "(<(?:div|span)\\b)([^>]*\\bclass\\s*=\\s*\"[^\"]*\\b" + Pattern.quote(className) + "\\b[^\"]*\"[^>]*)(>)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = p.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String merged = m.group(1) + mergeDeclarationsIntoAttrs(m.group(2), declarations) + m.group(3);
            m.appendReplacement(sb, Matcher.quoteReplacement(merged));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String mergeStyleIntoChildOfClass(String html, String parentClass, String childElement, String declarations) {
        // Match: <div class="...parentClass..." ...> whitespace <childElement ...>
        Pattern p = Pattern.compile(
                "(<(?:div|span)\\b[^>]*\\bclass\\s*=\\s*\"[^\"]*\\b" + Pattern.quote(parentClass) + "\\b[^\"]*\"[^>]*>)" +
                        "(\\s*)" +
                        "(<" + Pattern.quote(childElement) + ")(\\b[^>]*?)(>)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = p.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String merged = m.group(1) + m.group(2) + m.group(3) +
                    mergeDeclarationsIntoAttrs(m.group(4), declarations) + m.group(5);
            m.appendReplacement(sb, Matcher.quoteReplacement(merged));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Merge CSS declarations into an element's existing attributes string.
     * Existing inline properties are preserved (higher specificity than <style>).
     */
    private static String mergeDeclarationsIntoAttrs(String attrs, String declarations) {
        // Strip CSS comments from declarations
        declarations = declarations.replaceAll("/\\*.*?\\*/", "").trim();

        Pattern stylePattern = Pattern.compile("(style\\s*=\\s*\")([^\"]*)(\")", Pattern.CASE_INSENSITIVE);
        Matcher styleMatcher = stylePattern.matcher(attrs);
        if (styleMatcher.find()) {
            String existing = styleMatcher.group(2).trim();
            // Parse existing property names to avoid overwriting (inline specificity > <style>)
            Set<String> existingProps = new HashSet<>();
            for (String decl : existing.split(";")) {
                int colon = decl.indexOf(':');
                if (colon > 0) existingProps.add(decl.substring(0, colon).trim().toLowerCase());
            }
            // Only add declarations for properties not already in inline style
            StringBuilder newDecls = new StringBuilder();
            for (String decl : declarations.split(";")) {
                String trimmed = decl.trim();
                if (trimmed.isEmpty()) continue;
                int colon = trimmed.indexOf(':');
                if (colon <= 0) continue;
                String prop = trimmed.substring(0, colon).trim().toLowerCase();
                if (!existingProps.contains(prop)) {
                    newDecls.append(" ").append(trimmed).append(";");
                }
            }
            String newStyle = existing;
            if (!newStyle.isEmpty() && !newStyle.endsWith(";")) newStyle += ";";
            newStyle = (newStyle + newDecls).trim();
            return attrs.substring(0, styleMatcher.start()) +
                    "style=\"" + newStyle + "\"" +
                    attrs.substring(styleMatcher.end());
        } else {
            return attrs + " style=\"" + declarations + "\"";
        }
    }

    /**
     * Add id="test-root" to the first/outermost div element.
     * If it already has an id, replace it (CSS has been inlined already).
     */
    private static String addTestRootId(String html) {
        Pattern firstDiv = Pattern.compile("(<div)(\\b[^>]*)(>)", Pattern.CASE_INSENSITIVE);
        Matcher m = firstDiv.matcher(html);
        if (m.find()) {
            String tag = m.group(1);
            String attrs = m.group(2);
            String close = m.group(3);

            if (attrs.matches("(?i).*\\bid\\s*=\\s*\"[^\"]*\".*")) {
                // Replace existing id with test-root
                attrs = attrs.replaceFirst("(?i)\\bid\\s*=\\s*\"[^\"]*\"", "id=\"test-root\"");
            } else {
                // Add id="test-root"
                attrs = " id=\"test-root\"" + attrs;
            }
            return html.substring(0, m.start()) + tag + attrs + close + html.substring(m.end());
        }
        return html;
    }
}
